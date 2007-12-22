/*
 * GridRunner.java
 *
 * Created on January 26, 2006, 3:14 PM
 *
 */
package viskit.xsd.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import simkit.random.MersenneTwister;
import viskit.doe.DoeException;
import viskit.doe.LocalBootLoader;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.bindings.eventgraph.*; 

/**
 * The GridRunner launches a number of Gridlets to
 * run the actual replications. Gridlets communicate
 * back to the AssemblyServer their Reports via the 
 * XML-RPC port, which in turn via usid is updated
 * in the associated GridRunner; this reduces the 
 * number of ports required from upto infinity to 
 * just 1.
 *
 * GridRunner implements the back end of each XML-RPC
 * call related to running an experiment, It prepares
 * the DesignPoints, saves the experiment file, and
 * qsubs the Gridlets. 
 * 
 * GridRunner can also run in single-host mode, 
 * spawning locally run Gridlets as Threads.
 * 
 * First Gridlet should
 * report back the jobID via AssemblyServer chain,
 * for further administration.
 * 
 * Interface for AssemblyHandler and GridRunner
 * could/should be "factorizable".
 *
 * Needs to decide if using SecureAssemblyServer or not
 * as that port is unitary. For now, assume SecureGridRunner
 * TBD and will refactor the handler-handler later.
 *
 * @author Rick Goldberg
 * @version $Id: GridRunner.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class GridRunner /* compliments DoeRunDriver*/ {
    String usid;
    Integer jobID;
    int port;
    static final boolean debug = true;
    Vector<String> eventGraphs;
    Hashtable<String, Object> thirdPartyJars;
    File experimentFile;
    // This SimkitAssembly is used to set up 
    // the experiment. Read in by the setAssembly()
    // method, subsequently augmented by addEventGraph().
    // Upon, run(), written out to shared storage
    // for the Gridlets to then reconsitute with appropriate
    // parameters and execute. TBD it is possible to GC 
    // after run() as long as Results are handled the 
    // same synchronized way.
    SimkitAssembly root;
    viskit.xsd.bindings.assembly.ObjectFactory assemblyFactory;
    viskit.xsd.bindings.eventgraph.ObjectFactory eventGraphFactory; //?
    // running total number of tasks done
    int tasksCompleted;
    // count of DesignPoints per Sample
    int designPointCount;
    // replications per DesignPoint
    int replicationsPerDesignPoint;
    // total sample sets
    int totalSamples;
    // numberOfStats used to synchronize access to getDesignPointStats
    int numberOfStats = Integer.MAX_VALUE;
    // timeout in ms for any synchronized requests
    long timeout;
    // list of Booleans in order
    // indicating if a taskID is in the queue
    // True: in queue
    // False: Complete or DOA, check Results to determine.
    // gets updated by removeTask or removeIndexedTask
    // can be used by client by synchronized checking
    ArrayList<Object> queue;
    boolean queueClean = false; // dirty means unclaimed info
    // locking semaphores between set and get threads
    // for designPts, results
    List<Boolean> designPointStatsNotifiers;
    List<Boolean> replicationStatsNotifiers;
    List<Boolean> resultsNotifiers;
    List<String> status;
    
    LocalBootLoader loader = null;
    ClassLoader initLoader = null;
    
    public GridRunner() {
        this.eventGraphs = new Vector<String>();        
        this.thirdPartyJars = new Hashtable<String, Object>();
        try {
            assemblyFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
            eventGraphFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory(); //?
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        this.usid = "LOCAL-RUN";
        this.port = 0;

    }
    
    /** Creates a new instance of GridRunner */
    public GridRunner(String usid, int port) {
        this();
        this.usid = usid;
        this.port = port;
    }
     
    public GridRunner(LocalBootLoader loader) {
        this("LOCAL-RUN",0);
        this.loader = loader;
        //Thread.currentThread().setContextClassLoader(loader);
    }
    
    /**
     * hook for gridkit.setAssembly XML-RPC call, used to initialize
     * From DOE panel. Accepts raw XML String of Assembly.
     */
    
    public Boolean setAssembly(String assembly) {
        Unmarshaller u;
        InputStream inputStream;
 
        if (debug) {
            System.out.println("Setting assembly");
            System.out.println(assembly);
        }
        inputStream = new ByteArrayInputStream(assembly.getBytes());
        try {
            JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly", this.getClass().getClassLoader() );
            u = jaxbCtx.createUnmarshaller();
            this.root = (SimkitAssembly) u.unmarshal(inputStream);
        } catch (Exception e) { e.printStackTrace(); return Boolean.FALSE; }
    
        // clear results count
        this.tasksCompleted = 0;
        // set number replications per DesignPoint
        this.replicationsPerDesignPoint = Integer.parseInt(root.getExperiment().getReplicationsPerDesignPoint());
        // set totalSamples
        this.totalSamples = Integer.parseInt(root.getExperiment().getTotalSamples());
        // totalSample * designPointCount = queue size
        // also handy, sampleIndex = taskID / designPointCount; 
        // and         designPtIndex = taskID % designPointCount; 
        this.designPointCount = root.getDesignParameters().size();
        // timeout for synchronized calls as set by Experiment tag, or not means indefinite wait
        String to = root.getExperiment().getTimeout();
        this.timeout = Long.parseLong(to==null?"0":to);
        this.queue = new ArrayList<Object>();
        this.designPointStatsNotifiers = new ArrayList<Boolean>();
        this.replicationStatsNotifiers = new ArrayList<Boolean>();
        this.resultsNotifiers = new ArrayList<Boolean>();
        this.status = new ArrayList<String>();
        for ( int i = 0; i < totalSamples * designPointCount; i ++) {
            queue.add(Boolean.TRUE);
            designPointStatsNotifiers.add(new Boolean(false));
            for (int j = 0; j < this.replicationsPerDesignPoint; j++) {
                replicationStatsNotifiers.add(new Boolean(false));
            }
            resultsNotifiers.add(new Boolean(false));
            status.add("Pending");
        }
        
        return Boolean.TRUE;
    }
    
    // can be called either before or after setAssembly()
    // won't be processed until a run()
    public Boolean addEventGraph(String eventGraph) {
        eventGraphs.add(eventGraph);
        return Boolean.TRUE;
    }
    
    // unknown what the max buffer size is, 
    // but won't assume any particular length here
    // sequence starts at
    // file.size() / buffer.length + file.size() % buffer.length > 0 ? 0 : -1
    // and decrements to 0, which is the id of the last chunk
    int lastSequence = 0;
    public Integer transferJar(String filename, byte[] data, int sequence) {
        ByteArrayOutputStream jarData;
        try {
            if ( !thirdPartyJars.containsKey(filename) ) {
                if (debug) System.out.println("Accepting jar transfer: "+filename+" of "+sequence);
                lastSequence = sequence;
                jarData = new ByteArrayOutputStream();
                jarData.write(data);
                
                thirdPartyJars.put(filename, jarData);
            } else if (lastSequence - 1 == sequence) {
                jarData = (ByteArrayOutputStream) thirdPartyJars.get(filename);
                jarData.write(data);
                lastSequence = sequence;
                if (sequence == 0) { // sender counts down to 0
                    if(filename.endsWith(".jar")) {
                        filename = filename.substring(0,filename.length()-4);
                    }
                    File jarFile = File.createTempFile(filename,".jar");
                    FileOutputStream fos = new FileOutputStream(jarFile);
                    fos.write(jarData.toByteArray());
                    fos.flush();
                    fos.close();
                    URL u = jarFile.toURI().toURL();
                    
                    // replace buffer with URL, any further attempt to
                    // send this file during this session results
                    // in no transfer. URL will be retrieved by Gridlets
                    // later.
                    thirdPartyJars.put(filename, u);
                    if (debug) System.out.println("Cached jar "+u);                    
                }
            }
            return Integer.valueOf(""+data.length);
        } catch (Exception e) {
            return Integer.valueOf("-1");
        }
    }

    // called by Gridlet to install 3rd pty jars into
    // its own Boot class loader
    public Vector getJars() {
        Vector<String> ret = new Vector<String>();
        Enumeration e = thirdPartyJars.elements();
        while ( e.hasMoreElements() ) {
            ret.add(((URL)e.nextElement()).toString());
        }
        return ret;
    }
    
    /**
     * hook for gridkit.addResult XML-RPC call, used to report
     * back results from grid node run. Accepts raw XML String of Report.
     */    
    public Boolean addResult(String report) {
        boolean error = false;
        
        StreamSource strsrc =
                new javax.xml.transform.stream.StreamSource(new ByteArrayInputStream(report.getBytes()));
        
        try {
            
            JAXBContext jc = JAXBContext.newInstance( "viskit.xsd.bindings.assembly" , root.getClass().getClassLoader());
            Unmarshaller u = jc.createUnmarshaller();
            Results r = (Results) ( u.unmarshal(strsrc) );
            int sample = Integer.parseInt(r.getSample());
            int designPt = Integer.parseInt(r.getDesignPoint());
            // note replication is not indexed, yet.
            // after sim has finished run(), then is queried
            // directly for replicationStats and designPointStats
            // which is the real "result" and is handled 
            // differently. this result is mainly to dump
            // a lot of logs.
            
            List samples = root.getExperiment().getSample();
            //
            List designPoints = ((Sample)samples.get(sample)).getDesignPoint();
            int index = sample*designPointCount + designPt;
            Boolean notifier = resultsNotifiers.get(index);
            synchronized(notifier) {
                DesignPoint designPoint = (DesignPoint) designPoints.get(designPt);
                designPoint.setResults(r);
                // notice these get swapped, the Boolean
                // being waited on is no longer the one in 
                // the Vector, however it only waits if FALSE
                resultsNotifiers.set(index,new Boolean(true)); 
                notifier.notifyAll();
                
            }
            
        } catch (Exception e) { error = true; e.printStackTrace(); }
        
        return new Boolean(error);
    }
    
    public synchronized String getResultByTaskID(int taskID) {
        taskID --;
        int sampleIndex = taskID / designPointCount;
        int designPtIndex = taskID % designPointCount;
        return getResult(sampleIndex,designPtIndex);
        
    }
    
    /**
     * XML-RPC hook to retrieve results from an experimental run.
     * The call is synchronized, the calling client thread
     * which invokes this method on the server thread blocks
     * until a node run invokes the addResult() on a separate
     * server thread for the particular run requested.
     *
     * Any of Async XML-RPC with client callbacks, single threaded
     * in order, or multithreaded any order clients can be used.
     * This server has a maximum of 100 server threads default,
     * so don't send more than that many multithreaded requests
     * unless using Async mode with callbacks.
     *
     * Note: this method times out if a timeout value is set as
     * an attribute of the Experiment. This makes it tunable depending
     * on the expected run time of the bench test by the user. This
     * comes in handy if the client was single threaded in sequence,
     * see TestReader.java in gridkit.tests. If no value for timeout
     * is supplied in the XML-GRD, then it waits indefinitely.
     */    
    public synchronized String getResult(int sample, int designPt) {
        try {
            Sample s = (Sample)(root.getExperiment().getSample().get(sample)); 
            DesignPoint designPoint = (DesignPoint)(s.getDesignPoint().get(designPt));
            Results r = designPoint.getResults();
            int index = sample * designPointCount + designPt;
            Boolean notifier = resultsNotifiers.get(index);
            if ( ! notifier ) {
                try {
                    notifier.wait();
                } catch (InterruptedException ie) {
                    ;
                }
                r = designPoint.getResults();
            }
            
            if ( r == null ) {
                try {
                    r = (Results)(assemblyFactory.createResults());
                    r.setDesignPoint(""+designPt);
                    r.setSample(""+sample);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return (new SimkitAssemblyXML2Java()).marshalFragmentToString(r);
        } catch (Exception npe) {
            npe.printStackTrace(); // do nothing, the request came before design was in
        }
        
        return "WAIT";
    }
    
    // Hashtable returned is name keyed to String of xml
    public synchronized Hashtable getDesignPointStats(int sampleIndex, int designPtIndex) {
        Sample s = (Sample)(root.getExperiment().getSample().get(sampleIndex));
        DesignPoint dp = (DesignPoint) s.getDesignPoint().get(designPtIndex);
        Hashtable<String, String> ret = new Hashtable<String, String>();
        List stats = dp.getStatistics(); 
        int index = sampleIndex*designPointCount + designPtIndex;
        Boolean notifier = designPointStatsNotifiers.get(index);
        if(!notifier) {
            int sz = stats.size();
            while ( sz < numberOfStats ) { // tbd use boolean in notifier
                try {
                    notifier.wait();
                } catch (InterruptedException ie) {
                    ;//System.out.println("getDesignPointStats has size  "+stats.size());
                }
            }
        }
        Iterator it = stats.iterator();
        while (it.hasNext()) {
            String name = null;
            String xml = null;
            Object st = it.next();
            if ( st instanceof SampleStatistics ) {
                name = ((SampleStatistics)st).getName();
                xml = (new SimkitAssemblyXML2Java()).marshalFragmentToString(st);
            } else if ( st instanceof IndexedSampleStatistics ) {
                name = ((IndexedSampleStatistics)st).getName();
                xml = (new SimkitAssemblyXML2Java()).marshalFragmentToString(st);
            }
            if ( name != null && xml != null ) ret.put(name,xml);
        }
        
        return ret;
    }
    
    // Hashtable returned is name keyed to String of xml
    public synchronized Hashtable getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) {
        Sample s = (Sample)(root.getExperiment().getSample().get(sampleIndex));
        DesignPoint dp = (DesignPoint) s.getDesignPoint().get(designPtIndex);
        Replication rp = (Replication) dp.getReplication().get(replicationIndex);
        Hashtable<String, String> ret = new Hashtable<String, String>();
        List stats = rp.getStatistics();
        int index = ((sampleIndex*designPointCount + designPtIndex) * replicationsPerDesignPoint) + replicationIndex;
        Boolean notifier = replicationStatsNotifiers.get(index);
        
        if ( !notifier ) {
            try {
                notifier.wait();
            } catch (InterruptedException ex) {
                ;
            }
        }
        
        Iterator it = stats.iterator();
        while (it.hasNext()) {
            String name = null;
            String xml = null;
            Object st = it.next();
            if ( st instanceof SampleStatistics ) {
                name = ((SampleStatistics)st).getName();
                xml = (new SimkitAssemblyXML2Java()).marshalToString(st);
            } else if ( st instanceof IndexedSampleStatistics ) {
                name = ((IndexedSampleStatistics)st).getName();
                xml = (new SimkitAssemblyXML2Java()).marshalToString(st);
            }
            if ( name != null && xml != null ) ret.put(name,xml);
        }
        
        return ret;
    }
    
    public Boolean addDesignPointStat(int sampleIndex, int designPtIndex, int numberOfStats, String stat) {
        try {
            this.numberOfStats = numberOfStats; // this really only needs to be set the first time
            JAXBContext jc = JAXBContext.newInstance("viskit.xsd.bindings.assembly", root.getClass().getClassLoader());
            Unmarshaller u = jc.createUnmarshaller();
            Sample sample = (Sample) root.getExperiment().getSample().get(sampleIndex);
            DesignPoint designPoint = (DesignPoint) sample.getDesignPoint().get(designPtIndex);
            JAXBElement<?> stats = (JAXBElement<?>) u.unmarshal(new ByteArrayInputStream(stat.getBytes()));
            int index = (sampleIndex*designPointCount) + designPtIndex;
            Boolean notifier = designPointStatsNotifiers.get(index);
                        
            synchronized(notifier) {
                designPoint.getStatistics().add(stats);
                designPointStatsNotifiers.set(index,new Boolean(true));
                notifier.notify();
                    
                //System.out.println("addDesignPointStat "+stat);
            }
            
        } catch (Exception e) { e.printStackTrace(); return Boolean.FALSE; }
        
        return Boolean.TRUE;
    }

    public Boolean addReplicationStat(int sampleIndex, int designPtIndex, int replicationIndex, String stat) {
        try {
            JAXBContext jc = JAXBContext.newInstance( "viskit.xsd.bindings.assembly", this.getClass().getClassLoader()  );
            Unmarshaller u = jc.createUnmarshaller();
            Sample sample = (Sample) root.getExperiment().getSample().get(sampleIndex);
            DesignPoint designPoint = (DesignPoint) sample.getDesignPoint().get(designPtIndex);
            Replication rep = (Replication) designPoint.getReplication().get(replicationIndex);
            JAXBElement<?> stats = (JAXBElement<?>) u.unmarshal(new ByteArrayInputStream(stat.getBytes()));            
            int index = ((sampleIndex*designPointCount + designPtIndex) * replicationsPerDesignPoint) + replicationIndex;
            Boolean notifier = replicationStatsNotifiers.get(index);
            
            synchronized(notifier) {
                rep.getStatistics().add(stats);
                replicationStatsNotifiers.set(index,new Boolean(true));
                notifier.notify();
                
                System.out.println("addReplicationStat "+stat);
            }
        } catch (Exception e) { e.printStackTrace(); return Boolean.FALSE; }
       
        return Boolean.TRUE;
    }
    
    // qdel format is "jobID.taskID" in array jobs, so only way to
    // get jobID to parent process was by tagging the experiment
    // with the usid, so that the first Gridlet would report its
    // SGE_JOB_ID ( subsequently every other Gridlet's in the array ).
    
    // called by DOE or anybody that indexes by sample and designPt
    public synchronized Integer removeIndexedTask(int sampleIndex, int designPtIndex) {
        int taskID = sampleIndex * designPointCount;
        taskID += designPtIndex;
        taskID += 1;
       
        removeTask(jobID.intValue(),taskID);
        
        // TBD check if result first then make an empty result if needed
        try {
            Results r = assemblyFactory.createResults();
            r.setDesignPoint(""+designPtIndex);
            r.setSample(""+sampleIndex);
            // release Results lock on thread
            addResult((new SimkitAssemblyXML2Java()).marshalFragmentToString(r));
            System.out.println("addResult for "+(new SimkitAssemblyXML2Java()).marshalFragmentToString(r));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Integer(taskID);
    }
    
    // called by Gridlet to remove itself after
    // completion
    
    public synchronized Integer removeTask(int jobID, int taskID) {
        try {
            if (debug) System.out.println("qdel: "+jobID+"."+taskID); 
            if (!usid.equals("LOCAL-RUN")) {
                Runtime.getRuntime().exec( new String[] {"qdel",""+jobID+"."+taskID} ) ;
            } else {
                status.set(taskID-1,"Complete");
            }            
            
            List<Object> sQueue = Collections.synchronizedList(queue);
            
            synchronized(sQueue) {
                ListIterator<Object> li = sQueue.listIterator(taskID-1);
                
                li.next();
                
                // TODO: fix generics
                li.set(new Boolean(Boolean.FALSE));
                
                queueClean = false;
                
                // last case first
                // if results not ready, wait on resultsNotifier
                Boolean notifier = resultsNotifiers.get(taskID-1);
                if ( ! notifier  ) {
                    
                    try {
                        notifier.wait();
                    } catch (InterruptedException ex) {
                        ;
                    }
                    
                }
                // if replication stats not ready, wait on replicationStatsNotifier
                // replications are always added in order by one thread,
                // don't expect any a < b to be locked if b isn't
                for (int i = 0; i < replicationsPerDesignPoint; i++) {
                    int index = ((taskID-1)*replicationsPerDesignPoint) + i;
                    notifier = replicationStatsNotifiers.get(index);
                    if ( ! notifier ) {
                        
                        try {
                            notifier.wait();
                        } catch (InterruptedException ex) {
                            ;
                        }
                        
                    }
                }
                // if designPointStats not ready, wait on designPointStatsNotifier
                notifier = designPointStatsNotifiers.get(taskID-1);
                if ( ! notifier ) {
                    
                    try {
                        notifier.wait();
                    } catch (InterruptedException ex) {
                        ;
                    }
                    
                }
                tasksCompleted++;
                //queue.notify();
            }
            
            // if all results in, done! write out all results to storage
            // TBD, filename should include some session info since same
            // Assembly may be experimented on repeatedly. Really TBD,
            // SessionManager should keep a persistent cache of active
            // sessions in case of server shutdown, and that would be
            // a good place to also store this "core dump" filename for the
            // session.
            if ( tasksCompleted == designPointCount * totalSamples) {
                File dump = File.createTempFile(root.getName(),"Results.xml",experimentFile.getParentFile());
                
                (new SimkitAssemblyXML2Java())
                .marshal((javax.xml.bind.Element)root,
                        (OutputStream)new FileOutputStream(dump));
                
            }
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
        System.runFinalization();
        System.gc();
        return new Integer(taskID);
        
    }
    
    /**
     * XML-RPC handler for clearing the grid queue,
     * @return number of remaining tasks still in the queue
     * that will be terminated.
     */
  
    public Integer flushQueue() {
        Integer remainingTasks = new Integer(( designPointCount * totalSamples ) - tasksCompleted );
        try {
            Runtime.getRuntime().exec( new String[] {"qdel",jobID.toString()} ) ;
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
        if (root != null) {
            try {
                (new SimkitAssemblyXML2Java()).marshal((javax.xml.bind.Element)root,
                        (OutputStream)new FileOutputStream(new File(root.getName()+"Exp.xml")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        
        return remainingTasks;
    }
    
    /**
     * XML-RPC handler for returning number of remaining tasks in queue.
     * @return number of remaining tasks in the queue still running.
     */
    
    public Integer getRemainingTasks() {
        return new Integer(( designPointCount * totalSamples ) - tasksCompleted );
    }
    
    // idea is to reduce waiting threads to avoid hitting the xml-rpc
    // server thread limit of 100 requests. This call will block until
    // there has been some change in the queue, and returns a Vector
    // of Booleans which can be compared to the previous return Vector
    // to see which was updated. It won't block the first time called.
    public synchronized ArrayList getTaskQueue() {
        
        if (queueClean) {
            synchronized(queue) {
                try {
                    //if (timeout == 0)
                        queue.wait(); // wait for dirtyness
                    //else
                        //queue.wait(timeout);
                } catch (InterruptedException ie) {
                    ;
                } 
                queueClean = true;
            } 
            return queue;
        }        
        
        return queue;
    }
    
    public void setJobID(Integer jobID) {
        this.jobID = jobID;
    }
    
    /**
     * XML-RPC handler for clearing the experiment from memory,
     * could be used in cases where you want to flush the queue
     * and also the accumulated state so far.
     */
    public Boolean clear() {
        flushQueue();
        this.root = null;
        System.gc();
        return new Boolean(true);
    }
    
    public String qstat() {
        
        if (usid.equals("LOCAL-RUN")) {
            if (queue != null) {
                String qstat = "Local Job Queue:\nTaskID:\tStatus:\n";
                // taskIDs are evolved from the SGE notation
                // which starts at 1
                int taskID = 1; 
                for ( String s : status ) {
                    qstat += ""+(taskID++) +"\t"+s+"\n";
                }
                return qstat;
                //return ((LocalTaskQueue)queue).toString();
            } else {
                return "Waiting to run";
            }
        } else {
            try {
                Runtime r = Runtime.getRuntime();
                Process p = r.exec(new String[]{"qstat","-f"});
                java.io.InputStream i = p.getInputStream();
                java.io.StringWriter sw = new java.io.StringWriter();
                int c;
                while ( (c=i.read()) > 0 ) {
                    sw.write(c);
                }
                return sw.toString();
            } catch (IOException ex) {
                ex.printStackTrace();
                return "QSTAT-ERROR";
            }
        }
    }
    
    public Integer getDesignPointCount() {
        return new Integer(designPointCount);
    }
    
    public String qstatXML() {
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(new String[]{"qstat","-xml"});
            java.io.InputStream i = p.getInputStream();
            java.io.StringWriter sw = new java.io.StringWriter();
            int c;
            while ( (c=i.read()) > 0 ) {
                sw.write(c);
            }
            return sw.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return "QSTAT-ERROR";
        }
    }
    
    public Boolean run() {
        File userDir;
        try { // install the eventgraphs in the assembly
            List<EventGraph> eventGraphList = root.getEventGraph();
            Enumeration e = eventGraphs.elements();
            while ( e.hasMoreElements() ) {
                String eg = (String) e.nextElement();
                
                // TODO: update with generic JWSDP
                EventGraph egt = assemblyFactory.createEventGraph();
                egt.setContent(eg);
                eventGraphList.add(egt);
            }
            
            if (!usid.equals("LOCAL-RUN")) {
                // Grid runs put these in the gridkit daemon's user dir
                userDir = new File(System.getProperty("user.dir"));
                // create experimentFile, an assembly tree with decorations
                // give it unique name
                experimentFile = File.createTempFile(root.getName()+"Exp",".xml",userDir);
            } else {
                File tempDir = File.createTempFile("viskit","exp");
                tempDir.delete();
                tempDir.mkdir();
                tempDir.deleteOnExit();
                experimentFile = File.createTempFile(root.getName()+"Exp",".xml",tempDir);
            }
            
        } catch (Exception e) {
            return Boolean.FALSE;
        }
        
        // calculate DesignPoints
        
        if ( calculateDesignPoints() == Boolean.FALSE ) return Boolean.FALSE;
        
        
        // deposit the Experiment file
       
        try {
            (new SimkitAssemblyXML2Java()).marshal((javax.xml.bind.Element)root,
                    (OutputStream)new FileOutputStream(experimentFile));
        } catch (Exception e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
        
        // spawn Gridlets
        int totalTasks = designPointCount*totalSamples;
        try {
            if (!usid.equals("LOCAL-RUN")) {
                Runtime.getRuntime().exec( new String[] {"qsub","-cwd","-v","FILENAME="+experimentFile.getName(),"-v","PORT="+port,"-v","USID="+usid,"-t","1-"+totalTasks,"-S","/bin/bash","./gridrun.sh"});
            } else {
                localRun(experimentFile,totalTasks);
            }
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            return Boolean.FALSE;
        }
        
        return Boolean.TRUE;
    }
    
    // spawn Gridlets, but not on the "Grid", ie Locally
    // in Local mode, the number of concurrent threads
    // should be limited to number of cpu's and cores on
    // the local host machine. There really isn't a way
    // I know of to get that number in a platform independent
    // way. so default to 4 and be selectable, one thread
    // at a time per core, recent JVM's can do this pretty
    // well for most OS's hopefully, some quite well.
    //
    // In Grid mode, the TaskQueue is a synchronized
    // array of Boolean's, to be pluggable, the Local
    // queue is a list of Threads, whose get method
    // returns the isAlive() state of the thread, only
    // current threads in the pool get a start().
    public static final int MAX_THREADS = 4;
    void localRun(File experimentFile, int totalTasks) {
        ArrayList<Object> lastQueue;
        try {
            
            // TODO: fix generics
            queue = new LocalTaskQueue(this,experimentFile,totalTasks);  
        } catch (DoeException e) {
            e.printStackTrace();
        }
        
        // this shouldn't block on the very first call
        int tasksRemaining = getRemainingTasks(); // should be totalTasks
        
        lastQueue = cloneFromLocalTaskQueue((LocalTaskQueue) queue);
        queueClean = false; // redirty it
        // launch N starters of totalTasks tasks here
        // active tasks are going to be hot so put them in the pool
        // needed: a way to select the pool size
        // here just test with N=4 for starters. GUI adjust tbd
        int starters = MAX_THREADS>totalTasks?totalTasks:MAX_THREADS;
        for (int task = 0; task<starters; task++,tasksRemaining--) {
            status.set(task,"Running");
           ((LocalTaskQueue)queue).activate(task);
        }
        // if starters tasks complete in this loop, activate upto starters more until no tasks remain
        // this second time through the getTaskQueue should block until results in
        // from a Gridlet
        while (tasksRemaining > 0) {
            // this should block until a task or a number of tasks ends
            LocalTaskQueue nextQueue = (LocalTaskQueue) getTaskQueue();
            synchronized(nextQueue) {
                for (int i = 0; i < nextQueue.size(); i ++) {
                    // any change between queries indicates a transition at
                    // taskID = i0, i1,..., indicating results for these tasks
                    // since it's possible more than one comes in at once
                    if (!((Boolean) lastQueue.get(i)).equals(((Boolean) nextQueue.get(i)))) {
                        // i changed due to end of task
                        //
                        // find next available task from nextQueue
                        int j;
                        for (j = i+1; j<nextQueue.size(); j++) {
                            if (nextQueue.activate(j)) {
                                status.set(j,"Running");
                                break;
                            }
                        }
                        --tasksRemaining;
                        
                        nextQueue.set(i, Boolean.FALSE);
                        nextQueue.notify();                        
                        
                    }
                    // TODO: fix generics
                    lastQueue.set(i,nextQueue.get(i));
                } 
                //lastQueue = cloneFromLocalTaskQueue(nextQueue);
            }
            
        }
    }
    
    private synchronized ArrayList<Object> cloneFromLocalTaskQueue(LocalTaskQueue localQ) {
        ArrayList<Object> q = new ArrayList<Object>();
        //synchronized (localQ) {
            for ( int i = 0; i < localQ.size(); i++ ) {
                q.add(new Boolean((Boolean)localQ.get(i)));
            }
        //}
        return q;
    }
    
    public Boolean calculateDesignPoints() {
        boolean batch;
        
        java.util.List params = root.getDesignParameters();
        batch = !params.isEmpty();
        
        if ( batch ) {
            
            if (root.getExperiment() == null) { // in test mode, never happens
             
                Experiment e = assemblyFactory.createExperiment();
                Sample s = assemblyFactory.createSample();
                s.setIndex("" + 0);

                // TODO: fix generics
                e.getSample().add(s);
                e.setType("full-factorial");
                root.setExperiment(e);
                doFullFactorial();
                
            } else { // take a Script or use built ins
                Experiment e = root.getExperiment();
                String expType = e.getType();
                //bsh.Interpreter bsh = new bsh.Interpreter();
                try {
                    if (expType.equals("full-factorial")) {
                        doFullFactorial();
                    } else if (expType.equals("latin-hypercube")) {
                        doLatinHypercube();
                    }
                    //could script via jaxb and beanshell
                    //bsh.eval(root.getExperiment().getScript());
                } catch (Exception ex) { return Boolean.FALSE; }
            }
   
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
        
    }
    
    
    public void doFullFactorial() {
        
        List params = root.getDesignParameters();
        HashMap<TerminalParameter, Object> values = new HashMap<TerminalParameter, Object>();
        Iterator it = params.iterator();
        
        while (it.hasNext()) {
            
            TerminalParameter t = (TerminalParameter) (it.next());
            if (debug) System.out.println("Batch Mode "+t);
            JAXBElement<ValueRange> range = t.getValueRange();
            Object returns;
            if (range.getName().toString().contains("DoubleRange")) {
                returns = new Double[] { 
                    new Double(range.getValue().getLowValue()), 
                    new Double(range.getValue().getHighValue()) 
                };
            } else {
                returns = new Integer[] {
                    new Integer(range.getValue().getLowValue()),
                    new Integer(range.getValue().getHighValue())
                };
            }            
            
            values.put(t,returns);                    
            
        }
        if (values.size() > 0) {
            iterate(values,values.size()-1);
        }
    }
    
   
    
    void iterate(HashMap values, int depth) {
        
        Object[] terms = ((values.keySet()).toArray());
        Object params = (values.get((TerminalParameter)(terms[depth])));
        Object[] paramValues = (Object[])params;
        
        for ( int i = 0; i < paramValues.length; i++ ) {
            
            TerminalParameter tp = (TerminalParameter)(terms[depth]);
            tp.setValue(paramValues[i].toString());
            
            if ( depth > 0) {
                
                iterate(values, depth - 1);
                
            } else {
                
                Experiment experiment = root.getExperiment();
                Sample sample = (Sample) experiment.getSample().get(0);

                // TODO: Update with generic JWSDP
                List<DesignPoint> designPoints = sample.getDesignPoint();
                DesignPoint designPoint = assemblyFactory.createDesignPoint();

                // TODO: Update with generic JWSDP
                List<TerminalParameter> terminalParams = designPoint.getTerminalParameter();

                for (int j = 0; j < terms.length; j++) {
                    TerminalParameter termCopy = assemblyFactory.createTerminalParameter();
                    termCopy.setValue(((TerminalParameter) terms[j]).getValue());
                    termCopy.setType(((TerminalParameter) terms[j]).getType());
                    terminalParams.add(termCopy);
                }

                // TODO: fix generics
                designPoints.add(designPoint);
            }
        }
        
        
        
    }
    /**
     * Here we can use a Script to optionally set values before each set of Runs.
     * eg.
     * <Script> server.getServiceTime().getRandomNumber().resetSeed(); </Script>
     * so, the script should get copied into each DesignPoint instance (?).
     *
     * The DesignParameters return a range of values as per the FullFactorial
     * version.
     *
     * Each range is divided into bins of equal probability. Each bin is
     * numbered from 0 to number of independent variables - 1.
     * A numIndptVars x numIndptVars index matrix is created in the form of of a
     * Random Latin Square. A Random Latin Square is one whose first row and
     * column contain a random permutation of {sequence 0...runs-1} and each sub
     * matrix is created by selecting values that are not in the row or column
     * of the super matrix. To randomize within the same jvm session, rather
     * than take the value of the range at the bin number in the stratification,
     * a uniformly chosen sample is taken from the bin for each design point,
     * which stochastically jitters the sample points. Then even if the single
     * pass node runs all are from the same seed, they came from a slightly
     * different sample point. To create more runs per sample that have any
     * meaning then, it is required to use a different seed each Run via Script,
     * since each run starts from a "fresh" jvm.
     *
     * The Latin part is that the index matrix is Latin, which represent
     * probability bins to select from, not interpolated values of the ranges.
     * For small number of variates, more samples, as controlled from the
     * Experiment tag's totalsSamples attribute, from each Latin square, should
     * be run per per Experiment. If a script for the Runs as described above is
     * used then several different results can occur for designs where a
     * RandomVariate is seeded, otherwise they are the same.
     *
     * Each Latin square may generate an "infinite" number of similarly jittered
     * DesignPoints, but there are countably finite Latin square combinations.
     *
     * In general, the number of Latin square combinations is far
     * less than the number of FullFactorial combinations and converges as fast,
     * or better; there can be a large number of Latin squares, so in the case of
     * a large number of variates, it may not be essential to select more than
     * one sample set from each Latin square.
     *
     */
    
    
    // totalSamples is number of unique squares
    // size of designParams list is dimension of square
    // and number of designPoints to generate
    // each taskID is multi-replicationed
    // 
    // taskID is sampleNumber * 
    public void doLatinHypercube() throws Exception {
        //int runs = replicationsPerDesignPoint; 
        //String initScript = experiment.getScript();
        //bsh.Interpreter bsh = new bsh.Interpreter();
        
        int size = designPointCount;
        LatinPermutator latinSquares = new LatinPermutator(size);
        List<TerminalParameter> designParams = root.getDesignParameters();
        
        // TODO: fix generics                    
        List<Sample> samples = root.getExperiment().getSample();
        List<DesignPoint> designPoints;// = root.getExperiment().getDesignPoint();
    
        MersenneTwister rnd = new MersenneTwister();        
        
        for ( int i = 0; i < totalSamples; i++ ) {
            Sample sample = assemblyFactory.createSample();
            sample.setIndex(""+i);
            samples.add(sample);
            designPoints = sample.getDesignPoint();
            int[][] latinSquare = latinSquares.getRandomLatinSquare();
            int[] row;
            
            for ( int j = 0 ; j < latinSquare.length; j++) { // .length == size
                DesignPoint designPt = assemblyFactory.createDesignPoint();
                Iterator it = designParams.iterator();
                row = latinSquare[j];
                int ct = 0;

                while (it.hasNext()) {

                    TerminalParameter tp = assemblyFactory.createTerminalParameter();
                    TerminalParameter dp = (TerminalParameter) it.next();
                    Object[] range;

                    if (dp.getType().equals("double")) {

                        range = new Double[]{
                            new Double(dp.getValueRange().getValue().getLowValue()),
                            new Double(dp.getValueRange().getValue().getHighValue())
                        };

                    } else {

                        range = new Integer[]{
                            new Integer(dp.getValueRange().getValue().getLowValue()),
                            new Integer(dp.getValueRange().getValue().getHighValue())
                        };

                    }

                    // create sample "stratified" from n=size equal probability bins
                    // over range; this will "jitter" the sample if used repeatedly,
                    // while maintaining proximity to the same hypersurface anchor points
                    boolean enableJitter = true; // make a property of Experiment type tbd
                    if (range[0] instanceof Double) { // right now accept Double[2], spline TBD
                        double h = ((Double) (range[0])).doubleValue();
                        double l = ((Double) (range[1])).doubleValue();
                        if (h < l) {
                            double tmp = l;
                            l = h;
                            h = tmp;
                        }
                        double dt = h - l;
                        double ddt = dt / (double) size;
                        double sampleJitter = ddt * (rnd.draw() - .5); // fits in bin +/- .5ddt
                        double value = l + ddt * ((double) row[ct]) + (enableJitter ? sampleJitter : 0.0);

                        tp.setValue("" + value);
                        tp.setType(dp.getType());


                    } else if (range[0] instanceof Integer) { // or accept Integer[size] (not tested)

                        tp.setValue(range[row[ct]].toString());
                        tp.setType(dp.getType());

                    }

                    // TODO: fix generics
                    designPt.getTerminalParameter().add(tp);
                    ct++; //
                }

                // TODO: fix generics
                List<Replication> runList = designPt.getReplication();
                for (int ri = 0; ri < replicationsPerDesignPoint; ri++) {
                    Replication r = assemblyFactory.createReplication();
                    r.setIndex("" + ri);
                    runList.add(r);
                }

                designPt.setIndex("" + j);

                // TODO: fix generics
                designPoints.add(designPt);
                    
            }
        }
        
    }
    
}
