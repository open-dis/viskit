/*
 * GridRunner.java
 *
 * Created on January 26, 2006, 3:14 PM
 *
 */

package viskit.xsd.assembly;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
    String jobID;
    int port;
    static final boolean debug = true;
    Vector eventGraphs;
    
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
    // total number of Results received
    int resultsReceived;
    // count of DesignPoints
    int designPointCount;
    // replications per DesignPoint
    int replicationsPerDesignPoint;
    
    /** Creates a new instance of GridRunner */
    public GridRunner(String usid, int port) {
        this.usid = usid;
        this.port = port;
        this.eventGraphs = new Vector();
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
        this.resultsReceived = 0;
        // set number replications per DesignPoint
        this.replicationsPerDesignPoint = Integer.parseInt(root.getExperiment().getReplicationsPerDesignPoint());
        
        return Boolean.TRUE;
    }
    
    // can be called either before or after setAssembly()
    // won't be processed until a run()
    Boolean addEventGraph(String eventGraph) {
        eventGraphs.add(eventGraph);
        return Boolean.TRUE;
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
            ResultsType r = (ResultsType) ( u.unmarshal(strsrc) );
            
            int index = Integer.parseInt(r.getDesign());
            
            
            List designPoints = Collections.synchronizedList(root.getExperiment().getDesignPoint());
            
            synchronized(designPoints) {
                
                DesignPointType designPoint = (DesignPointType) designPoints.get(index);
                List runList = designPoint.getReplication();
                ReplicationType run = (ReplicationType)runList.get(Integer.parseInt(r.getReplication()));
                run.setResults(r);
                synchronized(run) {
                    run.notify();
                }
            }
            
            this.resultsReceived++;
            
            // if all results in, done! write out all results to storage
            if ( resultsReceived == designPointCount * replicationsPerDesignPoint) {
                (new SimkitAssemblyXML2Java())
                    .marshal((javax.xml.bind.Element)root, 
                        (OutputStream)new FileOutputStream(new File(root.getName()+"Exp.xml")));
            }
            
        } catch (Exception e) { error = true; e.printStackTrace(); }
        
        return new Boolean(error);
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
    
    public synchronized String getResult(int designPt, int run) {
        ReplicationType runner = (ReplicationType)(((DesignPointType)(root.getExperiment().getDesignPoint().get(designPt))).getReplication().get(run));
        ResultsType r = runner.getResults();
        int timeout = Integer.parseInt(root.getExperiment().getTimeout());
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
                r.setDesign(""+designPt);
                r.setReplication(""+run);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (new SimkitAssemblyXML2Java()).marshalToString(r);
    }

    // qdel format is "jobID.taskID" in array jobs, so only way to
    // get jobID to parent process was by tagging the experiment
    // with the usid, so that the first Gridlet would report its
    // SGE_JOB_ID ( subsequently every other Gridlet's in the array ).
    public Integer removeTask(int designPt, int run) {
        int taskID = designPt * Integer.parseInt(root.getExperiment().getReplicationsPerDesignPoint());
        taskID += run;
        taskID += 1;
        try {
            Runtime.getRuntime().exec( new String[] {"qdel",""+jobID+"."+taskID} ) ;
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
        
        try {
            ResultsType r = (ResultsType)(assemblyFactory.createResults());
            r.setDesign(""+designPt);
            r.setReplication(""+run);
            // release Results lock on thread
            addResult((new SimkitAssemblyXML2Java()).marshalToString(r));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Integer(taskID);
    }
    
    /**
     * XML-RPC handler for clearing the grid queue,
     * @return number of remaining jobs still in the queue
     * that will be terminated.
     */
    
  
    public Integer flushQueue() {
        Integer remainingJobs = new Integer(( designPointCount * replicationsPerDesignPoint ) - resultsReceived );
        try {
            Runtime.getRuntime().exec( new String[] {"qdel",jobID} ) ;
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

        
        return remainingJobs;
    }
    
    /**
     * XML-RPC handler for returning number of remaining jobs in queue,
     * could be used to estimate when a set of jobs becomes stuck.
     * @return number of remaining jobs in the queue still running.
     */
    
    public Integer getRemainingTasks() {
        return new Integer(( designPointCount * replicationsPerDesignPoint ) - resultsReceived );
    }
    
    public void setJobID(String jobID) {
        jobID = new String(jobID);
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
        int totalRuns = designPointCount*replicationsPerDesignPoint;
        try {
            Runtime.getRuntime().exec( new String[] {"qsub","-v","FILENAME="+experimentFileName,"-v","PORT="+port,"-v","USID="+usid,"-t","1-"+totalRuns,"-S","/bin/bash","./gridrun.sh"});
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
                    e.setType("full-factorial");
                    root.setExperiment(e);
                    doFullFactorial();
                } catch (javax.xml.bind.JAXBException jaxe) { jaxe.printStackTrace(); }
                
            } else { // take a Script or use built ins
                ExperimentType e = root.getExperiment();
                String expType = e.getType();
                //bsh.Interpreter bsh = new bsh.Interpreter();
                
                if (expType.equals("full-factorial")) {
                    doFullFactorial();
                } else if (expType.equals("latin-hypercube")) {
                    doLatinHypercube();
                }
                //could script via jaxb and beanshell
                //bsh.eval(root.getExperiment().getScript());
                
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
            List exprList = t.getContent();
            Iterator itex = exprList.iterator();
            Object returns = null;
            while (itex.hasNext()) {
                String expr = (String)itex.next();
                bsh.Interpreter bsh = new bsh.Interpreter();
                try {
                    bsh.eval(expr);
                    returns = bsh.eval(t.getName()+"();");
                    System.out.println(expr+" returns "+returns);
                    values.put(t,returns);
                    
                } catch (bsh.EvalError ee) {
                    ee.printStackTrace();
                }
            }
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
                    List designPoints = experiment.getDesignPoint();
                    DesignPointType designPoint = assemblyFactory.createDesignPoint();
                    List terminalParams = designPoint.getTerminalParameter();
                    
                    for (int j = 0; j<terms.length; j++) {
                        TerminalParameterType termCopy = assemblyFactory.createTerminalParameter();
                        termCopy.setValue(((TerminalParameterType)terms[j]).getValue());
                        termCopy.setType(((TerminalParameterType)terms[j]).getType());
                        terminalParams.add(termCopy);
                    }
                    
                    designPoints.add(designPoint);
                    designPointCount++;
                    
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
    
    public void doLatinHypercube() {
        ExperimentType experiment = root.getExperiment();
        int runs = replicationsPerDesignPoint; // fewer keystrokes
        String initScript = experiment.getScript();
        bsh.Interpreter bsh = new bsh.Interpreter();
        
        int totalSamples = Integer.parseInt(root.getExperiment().getTotalSamples());
        int size = root.getDesignParameters().size();
        int runsPerDesignPt = replicationsPerDesignPoint;  // check ?
        LatinPermutator latinSquares = new LatinPermutator(size);
        List designParams = root.getDesignParameters();
        List designPoints = root.getExperiment().getDesignPoint();
    
        HashMap values = new java.util.HashMap();
        MersenneTwister rnd = new MersenneTwister();
        designPointCount = 0;
        
        for ( int i = 0; i < totalSamples; i++ ) {
            
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
                        List exprList = dp.getContent();
                        Iterator itex = exprList.iterator();
                        Object returns = null;
                        
                        // evaluate each TerminalParameter script within the
                        // DesignParameter group
                        while (itex.hasNext()) {
                            String expr = (String)itex.next();
                            bsh = new bsh.Interpreter();
                            try {
                                bsh.eval(expr);
                                returns = bsh.eval(dp.getName()+"();");
                                
                                values.put(dp,returns);
                                
                            } catch (bsh.EvalError ee) {
                                ee.printStackTrace();
                            }
                        }
                        
                        Object[] range = (Object[]) values.get(dp);
                        
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
                            double sample = l + ddt*((double)row[ct]) + (enableJitter?sampleJitter:0.0);
                            
                            tp.setValue(""+sample);
                            tp.setType(dp.getType());
                            
                            
                        } else if (range[0] instanceof Integer) { // or accept Integer[size] (not tested)
                            
                            tp.setValue(range[row[ct]].toString());
                            tp.setType(dp.getType());
                            
                        }
                        
                        designPt.getTerminalParameter().add(tp);
                        ct++; //
                    }
                    
                    List runList = designPt.getReplication();
                    for ( int ri = 0; ri < runsPerDesignPt ; ri++ ) {
                        ReplicationType r = assemblyFactory.createReplication();
                        r.setIndex(""+ri);
                        runList.add(r);
                    }
                    
                    designPt.setIndex(""+designPointCount);
                    designPoints.add(designPt);
                    designPointCount++;
                    
                } catch (javax.xml.bind.JAXBException jaxbe) { jaxbe.printStackTrace(); }
            }
        }
        
    }
    
}
