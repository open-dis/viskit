/*
 * GridRunner.java
 *
 * Created on January 26, 2006, 3:14 PM
 *
 */
package viskit.gridlet;

import static edu.nps.util.LogUtils.getLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import static java.io.File.createTempFile;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.valueOf;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.gc;
import static java.lang.System.getProperty;
import static java.lang.System.runFinalization;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.synchronizedList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import static javax.xml.bind.JAXBContext.newInstance;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.Logger;

import simkit.random.MersenneTwister;
import viskit.doe.DoeException;
import viskit.doe.LocalBootLoader;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.*;

/**
 * The GridRunner launches a number of Gridlets to
 * run the actual replications. Gridlets communicate
 * back to the AssemblyServer their Reports via the
 * XML-RPC port, which in turn via usid is updated
 * in the associated GridRunner; this reduces the
 * number of ports required from up to infinity to
 * just 1.
 *
 * GridRunner implements the back end of each XML-RPC
 * call related to running an experiment, it prepares
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
 * @version $Id$
 */
public class GridRunner /* compliments DoeRunDriver*/ {
    String usid;
    Integer jobID;
    int port;
    static Logger log = getLogger(GridRunner.class);
    Vector<String> eventGraphs;
    Map<String, Object> thirdPartyJars;
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
    // list of Booleans in order
    // indicating if a taskID is in the queue
    // True: in queue
    // False: Complete or DOA, check Results to determine.
    // gets updated by removeTask or removeIndexedTask
    // can be used by client by synchronized checking
    List<Object> queue;
    boolean queueClean = false; // dirty means unclaimed info
    // locking semaphores between set and get threads
    // for designPts, results
    List<Boolean> designPointStatsNotifiers;
    List<Boolean> replicationStatsNotifiers;
    List<Boolean> resultsNotifiers;
    List<String> status;

    public GridRunner() {
        this.eventGraphs = new Vector<>();
        this.thirdPartyJars = new Hashtable<>();
        try {
            assemblyFactory = new viskit.xsd.bindings.assembly.ObjectFactory();
        } catch (Exception e) {
            log.error(e);
        }

        this.usid = "LOCAL-RUN";
        this.port = 0;

    }

    /** Creates a new instance of GridRunner
     * @param usid
     * @param port
     */
    public GridRunner(String usid, int port) {
        this();
        this.usid = usid;
        this.port = port;
    }

    public GridRunner(LocalBootLoader loader) {
        this("LOCAL-RUN", 0);
    }

    /**
     * hook for gridkit.setAssembly XML-RPC call, used to initialize
     * From DOE panel. Accepts raw XML String of Assembly.
     * @param assembly the Assembly to set
     * @return an indication of success
     */
    public Boolean setAssembly(String assembly) {
        Unmarshaller u;
        InputStream inputStream;

        log.debug("Setting assembly");
        log.debug(assembly);

        inputStream = new ByteArrayInputStream(assembly.getBytes());
        try {
            JAXBContext jaxbCtx = newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            u = jaxbCtx.createUnmarshaller();
            this.root = (SimkitAssembly) u.unmarshal(inputStream);
        } catch (JAXBException e) {
            log.error(e);
            return Boolean.FALSE;
        }

        // clear results count
        this.tasksCompleted = 0;
        // set number replications per DesignPoint
        this.replicationsPerDesignPoint = parseInt(root.getExperiment().getReplicationsPerDesignPoint());
        // set totalSamples
        this.totalSamples = parseInt(root.getExperiment().getTotalSamples());
        // totalSample * designPointCount = queue size
        // also handy, sampleIndex = taskID / designPointCount;
        // and         designPtIndex = taskID % designPointCount;
        this.designPointCount = root.getDesignParameters().size();
        // timeout for synchronized calls as set by Experiment tag, or not means indefinite wait
        this.queue = new ArrayList<>();
        this.designPointStatsNotifiers = new ArrayList<>();
        this.replicationStatsNotifiers = new ArrayList<>();
        this.resultsNotifiers = new ArrayList<>();
        this.status = new ArrayList<>();
        for ( int i = 0; i < totalSamples * designPointCount; i ++) {
            queue.add(Boolean.TRUE);
            designPointStatsNotifiers.add(false);
            for (int j = 0; j < this.replicationsPerDesignPoint; j++) {
                replicationStatsNotifiers.add(false);
            }
            resultsNotifiers.add(false);
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
                log.debug("Accepting jar transfer: " + filename + " of " + sequence);
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
                    File jarFile = createTempFile(filename,".jar");
                    try (FileOutputStream fos = new FileOutputStream(jarFile)) {
                        fos.write(jarData.toByteArray());
                        fos.flush();
                    }
                    URL u = jarFile.toURI().toURL();

                    // replace buffer with URL, any further attempt to
                    // send this file during this session results
                    // in no transfer. URL will be retrieved by Gridlets
                    // later.
                    thirdPartyJars.put(filename, u);
                    log.debug("Cached jar " + u);
                }
            }
            return valueOf(""+data.length);
        } catch (IOException | NumberFormatException e) {
            log.error(e);
            return valueOf("-1");
        }
    }

    // called by Gridlet to install 3rd pty jars into
    // its own Boot class loader
    public Vector<String> getJars() {
        Vector<String> ret = new Vector<>();
        Enumeration<Object> e = ((Hashtable<String, Object>)thirdPartyJars).elements();
        while ( e.hasMoreElements() ) {
            ret.add(e.nextElement().toString());
        }
        return ret;
    }

    /**
     * hook for gridkit.addResult XML-RPC call, used to report
     * back results from grid node run. Accepts raw XML String of Report.
     * @param report the report to add to these results
     * @return an indication of success
     */
    public Boolean addResult(String report) {
        boolean error = false;

        StreamSource strsrc =
                new javax.xml.transform.stream.StreamSource(new ByteArrayInputStream(report.getBytes()));

        try {

            JAXBContext jc = newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            Unmarshaller u = jc.createUnmarshaller();
            Results r = (Results) ( u.unmarshal(strsrc) );
            int sample = parseInt(r.getSample());
            int designPt = parseInt(r.getDesignPoint());
            // note replication is not indexed, yet.
            // after sim has finished run(), then is queried
            // directly for replicationStats and designPointStats
            // which is the real "result" and is handled
            // differently. this result is mainly to dump
            // a lot of logs.

            List<Sample> samples = root.getExperiment().getSample();
            //
            List<DesignPoint> designPoints = samples.get(sample).getDesignPoint();
            int index = sample*designPointCount + designPt;
            Boolean notifier = resultsNotifiers.get(index);
            synchronized(notifier) {
                DesignPoint designPoint = designPoints.get(designPt);
                designPoint.setResults(r);
                // notice these get swapped, the Boolean
                // being waited on is no longer the one in
                // the Vector, however it only waits if FALSE
                resultsNotifiers.set(index,true);
                notifier.notifyAll();

            }

        } catch (NumberFormatException | JAXBException e) {
            error = true;
            log.error(e);
        }

        return error;
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
     * @param sample a index for a sample
     * @param designPt and index for a design point
     * @return a String representation for the result
     */
    public synchronized String getResult(int sample, int designPt) {
        try {
            Sample s = root.getExperiment().getSample().get(sample);
            DesignPoint designPoint = s.getDesignPoint().get(designPt);
            Results r = designPoint.getResults();
            int index = sample * designPointCount + designPt;
            Boolean notifier = resultsNotifiers.get(index);
            if ( ! notifier ) {
                try {
                    notifier.wait();
                } catch (InterruptedException ie) {
                    // do nothing
                }
                r = designPoint.getResults();
            }

            if ( r == null ) {
                try {
                    r = assemblyFactory.createResults();
                    r.setDesignPoint(""+designPt);
                    r.setSample(""+sample);

                } catch (Exception e) {
                    log.error(e);
                }
            }
            return (new SimkitAssemblyXML2Java()).marshalFragmentToString(r);
        } catch (Exception e) {
            log.error(e); // do nothing, the request came before design was in
        }

        return "WAIT";
    }

    // Hashtable returned is name keyed to String of xml
    public synchronized Map<String, String> getDesignPointStats(int sampleIndex, int designPtIndex) {
        Sample s = root.getExperiment().getSample().get(sampleIndex);
        DesignPoint dp = s.getDesignPoint().get(designPtIndex);
        Map<String, String> ret = new Hashtable<>();
        List<JAXBElement<?>> stats = dp.getStatistics();
        int index = sampleIndex*designPointCount + designPtIndex;
        Boolean notifier = designPointStatsNotifiers.get(index);
        if(!notifier) {
            int sz = stats.size();
            while ( sz < numberOfStats ) { // tbd use boolean in notifier
                try {
                    notifier.wait();
                } catch (InterruptedException ie) {
                    //System.out.println("getDesignPointStats has size  "+stats.size());
                }
            }
        }
        Iterator<JAXBElement<?>> it = stats.iterator();
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
            if ( name != null && xml != null ) {
                ret.put(name, xml);
            }
        }

        return ret;
    }

    // Hashtable returned is name keyed to String of xml
    public synchronized Map<String, String> getReplicationStats(int sampleIndex, int designPtIndex, int replicationIndex) {
        Sample s = root.getExperiment().getSample().get(sampleIndex);
        DesignPoint dp = s.getDesignPoint().get(designPtIndex);
        Replication rp = dp.getReplication().get(replicationIndex);
        Map<String, String> ret = new Hashtable<>();
        List<JAXBElement<?>> stats = rp.getStatistics();
        int index = ((sampleIndex*designPointCount + designPtIndex) * replicationsPerDesignPoint) + replicationIndex;
        Boolean notifier = replicationStatsNotifiers.get(index);

        if ( !notifier ) {
            try {
                notifier.wait();
            } catch (InterruptedException ex) {
                // do nothing
            }
        }

        Iterator<JAXBElement<?>> it = stats.iterator();
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
            if ( name != null && xml != null ) {
                ret.put(name, xml);
            }
        }

        return ret;
    }

    public Boolean addDesignPointStat(int sampleIndex, int designPtIndex, int numberOfStats, String stat) {
        try {
            this.numberOfStats = numberOfStats; // this really only needs to be set the first time
            JAXBContext jc = newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            Unmarshaller u = jc.createUnmarshaller();
            Sample sample = root.getExperiment().getSample().get(sampleIndex);
            DesignPoint designPoint = sample.getDesignPoint().get(designPtIndex);
            JAXBElement<?> stats = (JAXBElement<?>) u.unmarshal(new ByteArrayInputStream(stat.getBytes()));
            int index = (sampleIndex*designPointCount) + designPtIndex;
            Boolean notifier = designPointStatsNotifiers.get(index);

            synchronized(notifier) {
                designPoint.getStatistics().add(stats);
                designPointStatsNotifiers.set(index,true);
                notifier.notify();

                //System.out.println("addDesignPointStat "+stat);
            }

        } catch (JAXBException e) {
            log.error(e);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public Boolean addReplicationStat(int sampleIndex, int designPtIndex, int replicationIndex, String stat) {
        try {
            JAXBContext jc = newInstance(SimkitAssemblyXML2Java.ASSEMBLY_BINDINGS);
            Unmarshaller u = jc.createUnmarshaller();
            Sample sample = root.getExperiment().getSample().get(sampleIndex);
            DesignPoint designPoint = sample.getDesignPoint().get(designPtIndex);
            Replication rep = designPoint.getReplication().get(replicationIndex);
            JAXBElement<?> stats = (JAXBElement<?>) u.unmarshal(new ByteArrayInputStream(stat.getBytes()));
            int index = ((sampleIndex*designPointCount + designPtIndex) * replicationsPerDesignPoint) + replicationIndex;
            Boolean notifier = replicationStatsNotifiers.get(index);

            synchronized(notifier) {
                rep.getStatistics().add(stats);
                replicationStatsNotifiers.set(index,true);
                notifier.notify();

                System.out.println("addReplicationStat "+stat);
            }
        } catch (JAXBException e) {
            log.error(e);
            return Boolean.FALSE;
        }

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

        removeTask(jobID,taskID);

        // TBD check if result first then make an empty result if needed
        try {
            Results r = assemblyFactory.createResults();
            r.setDesignPoint(""+designPtIndex);
            r.setSample(""+sampleIndex);
            // release Results lock on thread
            addResult((new SimkitAssemblyXML2Java()).marshalFragmentToString(r));
            System.out.println("addResult for "+(new SimkitAssemblyXML2Java()).marshalFragmentToString(r));
        } catch (Exception e) {
            log.error(e);
        }
        return taskID;
    }

    // called by Gridlet to remove itself after
    // completion

    public synchronized Integer removeTask(int jobID, int taskID) {
        try {
            log.debug("qdel: " + jobID + "." + taskID);
            if (!usid.equals("LOCAL-RUN")) {
                getRuntime().exec( new String[] {"qdel",""+jobID+"."+taskID} ) ;
            } else {
                status.set(taskID-1,"Complete");
            }

            List<Object> sQueue = synchronizedList(queue);

            synchronized(sQueue) {
                ListIterator<Object> li = sQueue.listIterator(taskID-1);

                li.next();

                // TODO: fix generics
                li.set(Boolean.FALSE);

                queueClean = false;

                // last case first
                // if results not ready, wait on resultsNotifier
                Boolean notifier = resultsNotifiers.get(taskID-1);
                if ( ! notifier  ) {

                    try {
                        notifier.wait();
                    } catch (InterruptedException ex) {
                        // do nothing
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
                            // do nothing
                        }

                    }
                }
                // if designPointStats not ready, wait on designPointStatsNotifier
                notifier = designPointStatsNotifiers.get(taskID-1);
                if ( ! notifier ) {

                    try {
                        notifier.wait();
                    } catch (InterruptedException ex) {
                        // do nothing
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
                File dump = createTempFile(root.getName(),"Results.xml",experimentFile.getParentFile());

                new SimkitAssemblyXML2Java().marshal(root, new FileOutputStream(dump));

            }
        } catch (IOException e) {
            log.error(e);
        }
        runFinalization();
        gc();
        return taskID;
    }

    /**
     * XML-RPC handler for clearing the grid queue,
     * @return number of remaining tasks still in the queue
     * that will be terminated.
     */

    public Integer flushQueue() {
        Integer remainingTasks = ( designPointCount * totalSamples ) - tasksCompleted;
        try {
            getRuntime().exec( new String[] {"qdel",jobID.toString()} ) ;
        } catch (IOException e) {
            log.debug(e);
        }
        if (root != null) {
            try {
                new SimkitAssemblyXML2Java().marshal(root, new FileOutputStream(new File(root.getName() + "Exp.xml")));
            } catch (FileNotFoundException e) {
                log.error(e);
            }
        }


        return remainingTasks;
    }

    /**
     * XML-RPC handler for returning number of remaining tasks in queue.
     * @return number of remaining tasks in the queue still running.
     */

    public Integer getRemainingTasks() {
        return ( designPointCount * totalSamples ) - tasksCompleted;
    }

    // idea is to reduce waiting threads to avoid hitting the xml-rpc
    // server thread limit of 100 requests. This call will block until
    // there has been some change in the queue, and returns a Vector
    // of Booleans which can be compared to the previous return Vector
    // to see which was updated. It won't block the first time called.
    public synchronized List<Object> getTaskQueue() {

        if (queueClean) {
            synchronized(queue) {
                try {
                    //if (timeout == 0)
                        queue.wait(); // wait for dirtyness
                    //else
                        //queue.wait(timeout);
                } catch (InterruptedException ie) {
                    // do nothing
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
     * @return an indication of success
     */
    public Boolean clear() {
        flushQueue();
        this.root = null;
        gc();
        return true;
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
                Runtime r = getRuntime();
                Process p = r.exec(new String[]{"qstat","-f"});
                java.io.InputStream i = p.getInputStream();
                java.io.StringWriter sw = new java.io.StringWriter();
                int c;
                while ( (c=i.read()) > 0 ) {
                    sw.write(c);
                }
                return sw.toString();
            } catch (IOException e) {
                log.error(e);
                return "QSTAT-ERROR";
            }
        }
    }

    public Integer getDesignPointCount() {
        return designPointCount;
    }

    public String qstatXML() {
        try {
            Runtime r = getRuntime();
            Process p = r.exec(new String[]{"qstat","-xml"});
            java.io.InputStream i = p.getInputStream();
            java.io.StringWriter sw = new java.io.StringWriter();
            int c;
            while ( (c=i.read()) > 0 ) {
                sw.write(c);
            }
            return sw.toString();
        } catch (IOException e) {
            log.error(e);
            return "QSTAT-ERROR";
        }
    }

    public Boolean run() {

        File userDir;
        try { // install the eventgraphs in the assembly
            List<EventGraph> eventGraphList = root.getEventGraph();
            Enumeration<String> e = eventGraphs.elements();
            while ( e.hasMoreElements() ) {
                String eg = e.nextElement();

                // TODO: update with generic JWSDP
                EventGraph egt = assemblyFactory.createEventGraph();
                egt.setContent(eg);
                eventGraphList.add(egt);
            }

            if (!usid.equals("LOCAL-RUN")) {
                // Grid runs put these in the gridkit daemon's user dir
                userDir = new File(getProperty("user.dir"));
                // create experimentFile, an assembly tree with decorations
                // give it unique name
                experimentFile = createTempFile(root.getName()+"Exp",".xml",userDir);
            } else {
                experimentFile = createTempFile(root.getName()+"Exp",".xml");
            }
        } catch (IOException e) {
            log.error(e);
            return Boolean.FALSE;
        }

        // calculate DesignPoints

        if (!calculateDesignPoints()) {
            return Boolean.FALSE;
        }

        // deposit the Experiment file

        try {
            new SimkitAssemblyXML2Java().marshal(root, new FileOutputStream(experimentFile));
        } catch (FileNotFoundException e) {
            log.error(e);
            return Boolean.FALSE;
        }

        // spawn Gridlets
        int totalTasks = designPointCount*totalSamples;
        log.info("usid is: " + usid);
        try {
            if (!usid.equals("LOCAL-RUN")) {
                getRuntime().exec( new String[] {"qsub","-cwd","-v","FILENAME="+experimentFile.getName(),"-v","PORT="+port,"-v","USID="+usid,"-t","1-"+totalTasks,"-S","/bin/bash","./gridrun.sh"});
            } else {
                localRun(experimentFile,totalTasks);
            }
        } catch (IOException e) {
            log.error(e);
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
        List<Object> lastQueue;
        try {

            // TODO: fix generics
            queue = new LocalTaskQueue(this,experimentFile,totalTasks);
        } catch (DoeException e) {
            log.error(e);
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
                    if (!lastQueue.get(i).equals(nextQueue.get(i))) {
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
        ArrayList<Object> q = new ArrayList<>();
        for (Object localQ1 : localQ) {
            q.add(localQ1);
        }
        //}
        return q;
    }

    public Boolean calculateDesignPoints() {
        boolean batch;

        List<TerminalParameter> params = root.getDesignParameters();
        batch = !params.isEmpty();

        if ( batch ) {

            if (root.getExperiment() == null) { // in test mode, never happens

                Experiment e = assemblyFactory.createExperiment();
                Sample s = assemblyFactory.createSample();
                s.setIndex("" + 0);

                e.getSample().add(s);
                e.setType("full-factorial");
                root.setExperiment(e);
                doFullFactorial();

            } else { // take a Script or use built ins
                Experiment e = root.getExperiment();
                String expType = e.getType();
                //bsh.Interpreter bsh = new bsh.Interpreter();
                try {
                    switch (expType) {
                        case "full-factorial":
                            doFullFactorial();
                            break;
                        case "latin-hypercube":
                            doLatinHypercube();
                            break;
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

        List<TerminalParameter> params = root.getDesignParameters();
        Map<TerminalParameter, Object> values = new HashMap<>();
        for (TerminalParameter t : params) {
            log.debug("Batch Mode " + t);
            JAXBElement<ValueRange> range = t.getValueRange();
            Object returns;
            if (range.getName().toString().contains("DoubleRange")) {
                returns = new Double[] {
                    Double.valueOf(range.getValue().getLowValue()),
                    Double.valueOf(range.getValue().getHighValue())
                };
            } else {
                returns = new Integer[] {
                    Integer.valueOf(range.getValue().getLowValue()),
                    Integer.valueOf(range.getValue().getHighValue())
                };
            }

            values.put(t,returns);
        }
        if (!values.isEmpty()) {
            iterate(values,values.size()-1);
        }
    }

    void iterate(Map<TerminalParameter, Object> values, int depth) {

        TerminalParameter[] terms = (TerminalParameter[]) values.keySet().toArray();
        Object params = values.get(terms[depth]);
        Object[] paramValues = (Object[])params;
        for (Object paramValue : paramValues) {
            TerminalParameter tp = terms[depth];
            tp.setValue(paramValue.toString());
            if (depth > 0) {
                iterate(values, depth - 1);
            } else {

                Experiment experiment = root.getExperiment();
                Sample sample = experiment.getSample().get(0);

                // TODO: Update with generic JWSDP
                List<DesignPoint> designPoints = sample.getDesignPoint();
                DesignPoint designPoint = assemblyFactory.createDesignPoint();

                // TODO: Update with generic JWSDP
                List<TerminalParameter> terminalParams = designPoint.getTerminalParameter();
                for (Object term : terms) {
                    TerminalParameter termCopy = assemblyFactory.createTerminalParameter();
                    termCopy.setValue(((TerminalParameter) term).getValue());
                    termCopy.setType(((TerminalParameter) term).getType());
                    terminalParams.add(termCopy);
                }

                designPoints.add(designPoint);
            }
        }
    }

    /**
     * Here we can use a Script to optionally set values before each set of Runs.
     * eg.
     * &lt;Script&gt; server.getServiceTime().getRandomNumber().resetSeed(); &lt;/Script&gt;
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
     * be run per Experiment. If a script for the Runs as described above is
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
     * @throws java.lang.Exception
     */
    // totalSamples is number of unique squares
    // size of designParams list is dimension of square
    // and number of designPoints to generate
    // each taskID is multi-replicationed
    //
    // taskID is sampleNumber
    public void doLatinHypercube() throws Exception {
        //int runs = replicationsPerDesignPoint;
        //String initScript = experiment.getScript();
        //bsh.Interpreter bsh = new bsh.Interpreter();

        int size = designPointCount;
        LatinPermutator latinSquares = new LatinPermutator(size);
        List<TerminalParameter> designParams = root.getDesignParameters();
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
                Iterator<TerminalParameter> it = designParams.iterator();
                row = latinSquare[j];
                int ct = 0;

                while (it.hasNext()) {

                    TerminalParameter tp = assemblyFactory.createTerminalParameter();
                    TerminalParameter dp = it.next();
                    Object[] range;

                    if (dp.getType().equals("double")) {

                        range = new Double[]{
                            Double.valueOf(dp.getValueRange().getValue().getLowValue()),
                            Double.valueOf(dp.getValueRange().getValue().getHighValue())
                        };

                    } else {

                        range = new Integer[]{
                            Integer.valueOf(dp.getValueRange().getValue().getLowValue()),
                            Integer.valueOf(dp.getValueRange().getValue().getHighValue())
                        };

                    }

                    // create sample "stratified" from n=size equal probability bins
                    // over range; this will "jitter" the sample if used repeatedly,
                    // while maintaining proximity to the same hypersurface anchor points
                    boolean enableJitter = true; // make a property of Experiment type tbd
                    if (range[0] instanceof Double) { // right now accept Double[2], spline TBD
                        double h = ((Double) (range[0]));
                        double l = ((Double) (range[1]));
                        if (h < l) {
                            double tmp = l;
                            l = h;
                            h = tmp;
                        }
                        double dt = h - l;
                        double ddt = dt / size;
                        double sampleJitter = ddt * (rnd.draw() - .5); // fits in bin +/- .5ddt
                        double value = l + ddt * row[ct] + (enableJitter ? sampleJitter : 0.0);

                        tp.setValue("" + value);
                        tp.setType(dp.getType());

                    } else if (range[0] instanceof Integer) { // or accept Integer[size] (not tested)

                        tp.setValue(range[row[ct]].toString());
                        tp.setType(dp.getType());

                    }

                    designPt.getTerminalParameter().add(tp);
                    ct++; //
                }

                List<Replication> runList = designPt.getReplication();
                for (int ri = 0; ri < replicationsPerDesignPoint; ri++) {
                    Replication r = assemblyFactory.createReplication();
                    r.setIndex("" + ri);
                    runList.add(r);
                }

                designPt.setIndex("" + j);
                designPoints.add(designPt);
            }
        }
    }
}
