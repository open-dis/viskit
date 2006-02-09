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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import simkit.random.MersenneTwister;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.bindings.eventgraph.*; //?

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
 * NB:
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
 */

public class GridRunner {
    String usid;
    Integer jobID;
    int port;
    static final boolean debug = true;
    Vector eventGraphs;
    Hashtable thirdPartyJars;
    // This SimkitAssemblyType is used to set up 
    // the experiment. Read in by the setAssembly()
    // method, subsequently augmented by addEventGraph().
    // Upon, run(), written out to shared storage
    // for the Gridlets to then reconsitute with appropriate
    // parameters and execute. TBD it is possible to GC 
    // after run() as long as Results are handled the 
    // same synchronized way.
    SimkitAssemblyType root;
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
    Vector queue;
    boolean queueClean = false; // dirty means unclaimed info
    /** Creates a new instance of GridRunner */
    public GridRunner(String usid, int port) {
        this.usid = usid;
        this.port = port;
        this.eventGraphs = new Vector();
        this.thirdPartyJars = new Hashtable();
        try {
            assemblyFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
            eventGraphFactory = new viskit.xsd.bindings.eventgraph.ObjectFactory(); //?
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
            u = jaxbCtx.createUnmarshaller();
            this.root = (SimkitAssemblyType) u.unmarshal(inputStream);
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
        this.queue = new Vector();
        for ( int i = 0; i < totalSamples * designPointCount; i ++) {
            queue.add(Boolean.TRUE);
        }
        return Boolean.TRUE;
    }
    
    // can be called either before or after setAssembly()
    // won't be processed until a run()
    Boolean addEventGraph(String eventGraph) {
        eventGraphs.add(eventGraph);
        return Boolean.TRUE;
    }
    
    // unknown what the max buffer size is, 
    // but won't assume any particular length here
    // sequence starts at
    // file.size() / buffer.length + file.size() % buffer.length > 0 ? 0 : -1
    // and decrements to 0, which is the id of the last chunk
    int lastSequence = 0;
    Integer transferJar(String filename, byte[] data, int sequence) {
        ByteArrayOutputStream jarData;
        try {
            if ( !thirdPartyJars.containsKey(filename) ) {
                System.out.println("Accepting jar transfer: "+filename+" of "+sequence);
                lastSequence = sequence;
                jarData = new ByteArrayOutputStream();
                jarData.write(data);
                thirdPartyJars.put(filename,jarData);
            } else if (lastSequence - 1 == sequence) {
                jarData = (ByteArrayOutputStream)thirdPartyJars.get(filename);
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
                    URL u = jarFile.toURL();
                    // replace buffer with URL, any further attempt to
                    // send this file during this session results
                    // in no transfer. URL will be retrieved by Gridlets
                    // later.
                    thirdPartyJars.put(filename,u);
                    System.out.println("Cached jar "+u);
                    
                }
            }
            return Integer.valueOf(data.length);
        } catch (Exception e) {
            return Integer.valueOf(-1);
        }
    }

    // called by Gridlet to install 3rd pty jars into
    // its own Boot class loader
    public Vector getJars() {
        Vector ret = new Vector();
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
            JAXBContext jc = JAXBContext.newInstance( "viskit.xsd.bindings.assembly" );
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
            List designPoints = Collections.synchronizedList(((Sample)(samples.get(sample))).getDesignPoint());
            
            synchronized(designPoints) {
                
                DesignPointType designPoint = (DesignPointType) designPoints.get(designPt);
                designPoint.setResults(r);
                synchronized(designPoint) {
                    designPoint.notify();
                }
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
            DesignPointType runner = (DesignPointType)(s.getDesignPoint().get(designPt));
            ResultsType r = runner.getResults();
            if ( r == null ) { // not while
                synchronized(runner) {
                    try {
                        if (timeout == 0)
                            runner.wait();
                        else
                            runner.wait(timeout);
                    } catch (InterruptedException ie) {
                        ;
                    }
                }
                r = runner.getResults();
                
            }
            
            if ( r == null ) {
                try {
                    r = (ResultsType)(assemblyFactory.createResults());
                    r.setDesignPoint(""+designPt);
                    r.setSample(""+sample);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return (new SimkitAssemblyXML2Java()).marshalToString(r);
        } catch (NullPointerException npe) {
            ; // do nothing, the request came before design was in
        }
        
        return "WAIT";
    }
    
    // Hashtable returned is name keyed to String of xml
    public synchronized Hashtable getDesignPointStats(int sampleIndex, int designPtIndex) {
        Sample s = (Sample)(root.getExperiment().getSample().get(sampleIndex));
        DesignPointType dp = (DesignPointType) s.getDesignPoint().get(designPtIndex);
        Hashtable ret = new Hashtable();
        List stats = dp.getStatistics(); 
        synchronized(stats) {
            while ( stats.size() < numberOfStats ) {
                try {
                    stats.wait();
                } catch (InterruptedException ie) {
                    ;
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
                xml = (new SimkitAssemblyXML2Java()).marshalToString(st);
            } else if ( st instanceof IndexedSampleStatistics ) {
                name = ((IndexedSampleStatistics)st).getName();
                xml = (new SimkitAssemblyXML2Java()).marshalToString(st);
            }
            if ( name != null && xml != null ) ret.put(name,xml);
        }
        
        return ret;
    }
    
    // Hashtable returned is name keyed to String of xml
    public synchronized Hashtable getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) {
        Sample s = (Sample)(root.getExperiment().getSample().get(sampleIndex));
        DesignPointType dp = (DesignPointType) s.getDesignPoint().get(designPtIndex);
        ReplicationType rp = (ReplicationType) dp.getReplication().get(replicationIndex);
        Hashtable ret = new Hashtable();
        List stats = rp.getStatistics();
        synchronized(stats) {
            while ( stats.size() < numberOfStats ) {
                try {
                    stats.wait();
                } catch (InterruptedException ie) {
                    ;
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
            JAXBContext jc = JAXBContext.newInstance( "viskit.xsd.bindings.assembly" );
            Unmarshaller u = jc.createUnmarshaller();
            SampleType sample = (SampleType) root.getExperiment().getSample().get(sampleIndex);
            DesignPoint designPoint = (DesignPoint) sample.getDesignPoint().get(designPtIndex);
            Object stats = u.unmarshal(new ByteArrayInputStream(stat.getBytes()));
            List statses = designPoint.getStatistics();
            this.numberOfStats = numberOfStats; // this really only needs to be set the first time
            synchronized(statses) {
                statses.add(stats);
                statses.notifyAll();
            }
        } catch (Exception e) { return Boolean.FALSE; }
        
        return Boolean.TRUE;
    }

    public Boolean addReplicationStat(int sampleIndex, int designPtIndex, int replicationIndex, String stat) {
        try {
            JAXBContext jc = JAXBContext.newInstance( "viskit.xsd.bindings.assembly" );
            Unmarshaller u = jc.createUnmarshaller();
            SampleType sample = (SampleType) root.getExperiment().getSample().get(sampleIndex);
            DesignPoint designPoint = (DesignPoint) sample.getDesignPoint().get(designPtIndex);
            ReplicationType rep = (ReplicationType) designPoint.getReplication().get(replicationIndex);
            Object stats = u.unmarshal(new ByteArrayInputStream(stat.getBytes()));
            List statses = rep.getStatistics();
            synchronized(statses) {
                statses.add(stats);
                statses.notifyAll();
            }
        } catch (Exception e) { return Boolean.FALSE; }
       
        return Boolean.TRUE;
    }
    
    // qdel format is "jobID.taskID" in array jobs, so only way to
    // get jobID to parent process was by tagging the experiment
    // with the usid, so that the first Gridlet would report its
    // SGE_JOB_ID ( subsequently every other Gridlet's in the array ).
    
    // called by DOE or anybody that indexes by sample and designPt
    public Integer removeIndexedTask(int sampleIndex, int designPtIndex) {
        int taskID = sampleIndex * designPointCount;
        taskID += designPtIndex;
        taskID += 1;
       
        removeTask(jobID.intValue(),taskID);
        
        // TBD check if result first then make an empty result if needed
        try {
            ResultsType r = (ResultsType)(assemblyFactory.createResults());
            r.setDesignPoint(""+designPtIndex);
            r.setSample(""+sampleIndex);
            // release Results lock on thread
            addResult((new SimkitAssemblyXML2Java()).marshalToString(r));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Integer(taskID);
    }
    
    // called by Gridlet to remove itself after
    // completion
    
    public Integer removeTask(int jobID, int taskID) {
        try {
            if (debug) System.out.println("qdel: "+jobID+"."+taskID);        
            Runtime.getRuntime().exec( new String[] {"qdel",""+jobID+"."+taskID} ) ;
            
            
            tasksCompleted++;
            synchronized(queue) {
                queue.set(taskID-1,Boolean.FALSE);
                queueClean = false;
                queue.notify();
            }
            // if all results in, done! write out all results to storage
            // TBD, filename should include some session info since same
            // Assembly may be experimented on repeatedly. Really TBD,
            // SessionManager should keep a persistent cache of active
            // sessions in case of server shutdown, and that would be 
            // a good place to also store this "core dump" filename for the 
            // session.
            if ( tasksCompleted == designPointCount * totalSamples) {
                (new SimkitAssemblyXML2Java())
                .marshal((javax.xml.bind.Element)root,
                        (OutputStream)new FileOutputStream(new File(root.getName()+"Exp.xml")));
            }
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }

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
     * XML-RPC handler for returning number of remaining jobs in queue,
     * could be used to estimate when a set of jobs becomes stuck.
     * @return number of remaining jobs in the queue still running.
     */
    
    public Integer getRemainingTasks() {
        return new Integer(( designPointCount * totalSamples ) - tasksCompleted );
    }
    
    // idea is to reduce waiting threads to avoid hitting the xml-rpc
    // server thread limit of 100 requests. This call will block until
    // there has been some change in the queue, and returns a Vector
    // of Booleans which can be compared to the previous return Vector
    // to see which was updated.
    public synchronized Vector getTaskQueue() {
        
        if (queueClean) {
            synchronized(queue) {
                try {
                    if (timeout == 0)
                        queue.wait(); // wait for dirtyness
                    else
                        queue.wait(timeout);
                } catch (InterruptedException ie) {
                    ;
                }
            } return queue;
        }
        
        queueClean = true;
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
    
    Boolean run() {
        try { // install the eventgraphs in the assembly
            List eventGraphList = root.getEventGraph();
            Enumeration e = eventGraphs.elements();
            while ( e.hasMoreElements() ) {
                String eg = (String) e.nextElement();
                EventGraphType egt = assemblyFactory.createEventGraphType();
                egt.getContent().add(eg);
                eventGraphList.add(egt);
            }
        } catch (Exception e) {
            return Boolean.FALSE;
        }
        
        // calculate DesignPoints
        
        if ( calculateDesignPoints() == Boolean.FALSE ) return Boolean.FALSE;
        
        // deposit the Experiment file
        String experimentFileName = root.getName() + "Exp.xml";
        try {
            (new SimkitAssemblyXML2Java()).marshal((javax.xml.bind.Element)root,
                    (OutputStream)new FileOutputStream(new File(experimentFileName)));
        } catch (Exception e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
        
        // spawn Gridlets
        int totalTasks = designPointCount*totalSamples;
        try {
            Runtime.getRuntime().exec( new String[] {"qsub","-cwd","-v","FILENAME="+experimentFileName,"-v","PORT="+port,"-v","USID="+usid,"-t","1-"+totalTasks,"-S","/bin/bash","./gridrun.sh"});
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            return Boolean.FALSE;
        }
        
        return Boolean.TRUE;
    }
    
    public Boolean calculateDesignPoints() {
        boolean batch;
        
        java.util.List params = root.getDesignParameters();
        batch = !params.isEmpty();
        
        if ( batch ) {
            
            if (root.getExperiment() == null) { // in test mode, never happens
                
                try {
                    ExperimentType e = assemblyFactory.createExperiment();
                    SampleType s = assemblyFactory.createSample();
                    s.setIndex(""+0);
                    e.getSample().add(s);
                    e.setType("full-factorial");
                    root.setExperiment(e);
                    doFullFactorial();
                } catch (javax.xml.bind.JAXBException jaxe) { jaxe.printStackTrace(); }
                
            } else { // take a Script or use built ins
                ExperimentType e = root.getExperiment();
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
        HashMap values = new HashMap();
        Iterator it = params.iterator();
        
        while (it.hasNext()) {
            
            TerminalParameterType t = (TerminalParameterType) (it.next());
            System.out.println("Batch Mode "+t);
            ValueRangeType range = t.getValueRange();
            Object returns;
            if ( range instanceof DoubleRange ) {
                returns = new Double[] { 
                    new Double(((DoubleRange)range).getLowValue()), 
                    new Double(((DoubleRange)range).getHighValue()) 
                };
            } else {
                returns = new Integer[] {
                    new Integer(((IntegerRange)range).getLowValue()),
                    new Integer(((IntegerRange)range).getHighValue())
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
                
                try {
                    ExperimentType experiment = root.getExperiment();
                    Sample sample = (Sample) experiment.getSample().get(0);
                    List designPoints = sample.getDesignPoint();
                    DesignPointType designPoint = assemblyFactory.createDesignPoint();
                    List terminalParams = designPoint.getTerminalParameter();
                    
                    for (int j = 0; j<terms.length; j++) {
                        TerminalParameterType termCopy = assemblyFactory.createTerminalParameter();
                        termCopy.setValue(((TerminalParameterType)terms[j]).getValue());
                        termCopy.setType(((TerminalParameterType)terms[j]).getType());
                        terminalParams.add(termCopy);
                    }
                    
                    designPoints.add(designPoint);
                    
                } catch (javax.xml.bind.JAXBException jaxe) {jaxe.printStackTrace();}
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
        ExperimentType experiment = root.getExperiment();
        //int runs = replicationsPerDesignPoint; 
        String initScript = experiment.getScript();
        bsh.Interpreter bsh = new bsh.Interpreter();
        
        int size = designPointCount;
        LatinPermutator latinSquares = new LatinPermutator(size);
        List designParams = root.getDesignParameters();
        List samples = root.getExperiment().getSample();
        List designPoints;// = root.getExperiment().getDesignPoint();
    
        HashMap values = new java.util.HashMap();
        MersenneTwister rnd = new MersenneTwister();
        
        
        for ( int i = 0; i < totalSamples; i++ ) {
            SampleType sample = assemblyFactory.createSample();
            sample.setIndex(""+i);
            samples.add(sample);
            designPoints = sample.getDesignPoint();
            int[][] latinSquare = latinSquares.getRandomLatinSquare();
            int[] row;
            
            for ( int j = 0 ; j < latinSquare.length; j++) { // .length == size
                try {
                    DesignPointType designPt = assemblyFactory.createDesignPoint();
                    Iterator it = designParams.iterator();
                    row = latinSquare[j];
                    int ct = 0;
                    
                    while ( it.hasNext() ) {
                        
                        TerminalParameterType tp = assemblyFactory.createTerminalParameter();
                        TerminalParameterType dp = (TerminalParameterType)it.next();
                        Object[] range;
                        
                        if (dp.getType().equals("double")) {
                            
                            range = new Double[]{
                                new Double(dp.getValueRange().getLowValue()),
                                new Double(dp.getValueRange().getHighValue())
                            };
                            
                        } else {
                            
                            range = new Integer[]{
                                new Integer(dp.getValueRange().getLowValue()),
                                new Integer(dp.getValueRange().getHighValue())
                            };
                            
                        }
        
                        // create sample "stratified" from n=size equal probability bins
                        // over range; this will "jitter" the sample if used repeatedly,
                        // while maintaining proximity to the same hypersurface anchor points
                        boolean enableJitter = true; // make a property of Experiment type tbd
                        if (range[0] instanceof Double) { // right now accept Double[2], spline TBD
                            double h = ((Double)(range[0])).doubleValue();
                            double l = ((Double)(range[1])).doubleValue();
                            if ( h < l ) {
                                double tmp = l;
                                l = h;
                                h = tmp;
                            }
                            double dt = h - l;
                            double ddt = dt/(double)size;
                            double sampleJitter = ddt*(rnd.draw()-.5); // fits in bin +/- .5ddt
                            double value = l + ddt*((double)row[ct]) + (enableJitter?sampleJitter:0.0);
                            
                            tp.setValue(""+value);
                            tp.setType(dp.getType());
                            
                            
                        } else if (range[0] instanceof Integer) { // or accept Integer[size] (not tested)
                            
                            tp.setValue(range[row[ct]].toString());
                            tp.setType(dp.getType());
                            
                        }
                        
                        designPt.getTerminalParameter().add(tp);
                        ct++; //
                    }
                    
                    List runList = designPt.getReplication();
                    for ( int ri = 0; ri < replicationsPerDesignPoint ; ri++ ) {
                        ReplicationType r = assemblyFactory.createReplication();
                        r.setIndex(""+ri);
                        runList.add(r);
                    }
                    
                    designPt.setIndex(""+j);
                    designPoints.add(designPt);
                    
                } catch (javax.xml.bind.JAXBException jaxbe) { jaxbe.printStackTrace(); }
            }
        }
        
    }
    
}
