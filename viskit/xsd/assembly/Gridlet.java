/*
 * Gridlet.java
 *
 * Created on January 28, 2006, 1:30 PM
 *
 * Process that actually runs on a Grid node.
 *
 * Assumes environment properties set:
 * USID : Unique Session ID
 * FILENAME : Name of experiement file.
 * PORT: Port number to report back to.
 * SGE : any SGE environment
 * TBD - XmlRpcClient may need to be either clear or 
 * ssl. 
 *
 * Third party jars can be added to the runtime classpath
 * prior to reconstituting an Assembly and running it. See
 * GridRunner for XML-RPC details. To do this though, Gridlets
 * should be launched from viskit.xsd.cli.Launcher to
 * have access to the Boot ClassLoader.
 *
 */

package viskit.xsd.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
import viskit.doe.DoeException;
import viskit.doe.LocalBootLoader;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.cli.Boot;
import javax.xml.bind.Element;
import simkit.stat.SampleStatistics;
import com.sun.tools.javac.Main;


/**
 *
 * @author Rick Goldberg
 *
 * Gridlet indexes itself to a DesignPoint within the Experiment file,
 * creates an Assembly from that DesignPoint, compiles and runs it.
 *
 * This is the compiled version of the Gridlet, formerly interpereted,
 * which is now in BshGridlet.
 *
 * Main difference aside from using javac to compile, is attention
 * to separation of class paths by various Gridlets which may be 
 * running on the same host. Each Gridlet runs a particular DesignPoint,
 * which in java is represented by a single .class for the ViskitAssembly.
 * Also generated are any event-graph .classes, which also go there.
 *
 * If a Gridlet was spawned by a LocalBootLoader, the EventGraphs should
 * already be in the classpath, ie, currentContextClassLoader's.
 *
 * Get 'em while they're hot, these Gridlets should outperform the interpereted
 * mode, but most importantly enable entities with a large number of
 * parameters.
 *
 */

public class Gridlet extends Thread {
    SimkitAssemblyXML2Java sax2j;
    XmlRpcClientLite xmlrpc;
    // The gridRunner really is a GridRunner, however this instance came from 
    // a LocalBootLoader, or possibly Boot on grid in which case not related, 
    // either way, the gridRunner will not be recognized as a GridRunner because
    // it comes from a different loader. To communicate back, use introspection.
    Object gridRunner;
    int taskID;
    int numTasks;
    int jobID;
    int port;
    String frontHost;
    String usid;
    String filename;
    String pwd;
    File expFile;
    
    
    public Gridlet(int taskID, int jobID, int numTasks, String frontHost, int port, String usid, URL expFile ) {
        try {
            xmlrpc = new XmlRpcClientLite(frontHost, port);
        } catch (java.net.MalformedURLException murle) {
            murle.printStackTrace();
        }
    }
    // not used
    public Gridlet(int taskID, int jobID, int numTasks, GridRunner gridRunner, File expFile) throws DoeException {
        this.taskID = taskID;
        this.jobID = jobID;
        this.numTasks = numTasks;
        this.gridRunner = gridRunner;
        this.expFile = expFile;
        try {
            this.sax2j = new SimkitAssemblyXML2Java(expFile.toURL().openStream());
        } catch (Exception ex) {
            throw new DoeException(ex.getMessage());
        }
    }
    
    public Gridlet() {
       
        try {
           
            Process pr = Runtime.getRuntime().exec("env");
            InputStream is = pr.getInputStream();
            Properties p = System.getProperties();
            p.load(is);
            
            if (p.getProperty("SGE_TASK_ID")!=null) {
                
                taskID=Integer.parseInt(p.getProperty("SGE_TASK_ID"));
                jobID=Integer.parseInt(p.getProperty("JOB_ID"));
                numTasks=Integer.parseInt(p.getProperty("SGE_TASK_LAST"));
                frontHost = p.getProperty("SGE_O_HOST");
                usid = p.getProperty("USID");
                filename = p.getProperty("FILENAME");
                port = Integer.parseInt(p.getProperty("PORT"));
                pwd = p.getProperty("PWD");
                sax2j = new SimkitAssemblyXML2Java(new URL("file:"+pwd+"/"+filename).openStream());
                System.out.println(taskID+ " "+ jobID+" "+usid+" "+filename+" "+pwd); 
                //FIXME: should also check if SSL
                xmlrpc = new XmlRpcClientLite(frontHost,port);
                
                // not as needed as before
                // still handy but possible
                // 1 never starts or returns
                // after 2 or any of the 
                // concurrently running jobs
                // see removeTask which
                // handles the main case 
                // where you'd need jobID
                // better
                if (taskID == 1) {
                    Vector v = new Vector();
                    v.add(usid);
                    v.add(new Integer(jobID));
                    xmlrpc.execute("gridkit.setJobID", v);
                }
                
                // get any thirdPartyJars and install them now
                Vector v = new Vector();
                v.add(usid);
                v = (Vector)xmlrpc.execute("gridkit.getJars",v);
                Enumeration e = v.elements();
                ClassLoader boot = Thread.currentThread().getContextClassLoader();
                if (boot instanceof Boot) while ( e.hasMoreElements() ) {
                    ((Boot)boot).addJar(new URL((String)e.nextElement()));
                } else {
                    if (!v.isEmpty())
                        throw 
                            new RuntimeException("You should really be using viskit.xsd.cli.Boot loader to launch Gridlets!");
                }
                
            } else {
                // check if LocalBootLoader mode, otherwise throw exception
                Object loaderO = Thread.currentThread().getContextClassLoader();
                Class loaderz = loaderO.getClass();
                if ( !( loaderz.getName().equals("viskit.doe.LocalBootLoader") ) )
                    throw new RuntimeException("Not running as SGE job or local mode?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // GridRunner may have to be handled introspecively!
    // if setting this with the parent's loader causes cast
    // exception here then we know
    public void setGridRunner(Object gridRunner) {
        this.gridRunner = gridRunner;
    }
    
    public void setExperimentFile(File experimentFile) {
        this.expFile = experimentFile;
        try {
            //See comment in LocalTaskQueue, try commenting out the line, and uncommenting the printlns to see it up close
            //System.out.println("Gridlet.setExperimentFile, "+Thread.currentThread()+"'s loader is "+ Thread.currentThread().getContextClassLoader());
            //System.out.println("Gridlet.setExperimentFile, "+this+"'s loader is "+ Thread.currentThread().getContextClassLoader());
            sax2j = new SimkitAssemblyXML2Java(experimentFile.toURL().openStream());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }
    
    public void setJobID(int jobID) {
        this.jobID = jobID;
    }
    
    public void setTotalTasks(int totalTasks) {
        this.numTasks = totalTasks;
    }
    /*
     task.setExperimentFile(experimentFile);
     task.setTaskID(i+1);
     task.setJobID(0); // tbd, enable multiple jobs
     task.setTotalTasks(totalTasks);
    */
    
    public static void main(String[] args) {
        Gridlet gridlet = new Gridlet();
        gridlet.start();
    }
    
    public void run() {
        
        SimkitAssemblyType root;
        sax2j.unmarshal();
        root = sax2j.getRoot();
        
        ExperimentType exp = root.getExperiment();
        int replicationsPerDesignPoint = Integer.parseInt(exp.getReplicationsPerDesignPoint());
        List samples = exp.getSample();
        
        List designParams = root.getDesignParameters();
        int sampleIndex = (taskID-1) / designParams.size();
        int designPtIndex = (taskID-1) % designParams.size();
        
        Sample sample = (Sample) samples.get(sampleIndex);
        List designPoints = sample.getDesignPoint();
        
        DesignPointType designPoint = (DesignPointType)(designPoints.get(designPtIndex));
        List designArgs = designPoint.getTerminalParameter();
        Iterator itd = designParams.iterator();
        Iterator itp = designArgs.iterator();
        
        
        boolean debug_io = Boolean.valueOf(exp.getDebug()).booleanValue();
        debug_io = true;
        if(debug_io)System.out.println(filename+" Grid Task ID "+taskID+" of "+numTasks+" tasks in jobID "+jobID+" which is DesignPoint "+designPtIndex+" of Sample "+ sampleIndex);
        
        //pass design args into design params
        while ( itd.hasNext() && itp.hasNext() ) {
            TerminalParameterType arg = (TerminalParameterType)(itp.next());
            TerminalParameterType designParam = (TerminalParameterType)(itd.next());
            designParam.setValue(arg.getValue());
        }
        
        try {
            
            //processed into results tag, sent back to
            //SGE_O_HOST at socket in raw XML
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream log = new PrintStream(baos);
            java.io.OutputStream oldOut = System.out;
            
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            PrintStream err = new PrintStream(baos2);
            java.io.OutputStream oldErr = System.err;
            
            // disconnect io
            
            if(!debug_io) {
                System.setErr(err);
                System.setOut(log);
            }
            
            File workDir = new File(System.getProperty("user.dir"));
            File tempDir = TempDir.createGeneratedName("gridkit",workDir);
            // setting a classpath this way, we don't need to keep track of the
            // actual files, ie "javac -d tempDir" will create subdirs 
            String systemClassPath = System.getProperty("java.class.path");
            System.setProperty("java.class.path", systemClassPath+File.pathSeparator+tempDir.getCanonicalPath());
            
            List depends = root.getEventGraph();
            Iterator di = depends.iterator();
            ArrayList eventGraphJavaFiles = new ArrayList();
            
            // submit all EventGraphs to the beanshell context
            while ( di.hasNext() ) {
                
                EventGraphType d = (EventGraphType)(di.next());
                ByteArrayInputStream bais;
                StringBuffer s = new StringBuffer();
                List content = d.getContent();
                Iterator it = content.iterator();
                
                while ( it.hasNext() ) {
                    String str = (String)(it.next());
                    s.append(str);
                }
                
                bais = new ByteArrayInputStream(s.toString().getBytes());
                // generate java for the eventGraph and evaluate a loaded
                // class
                viskit.xsd.translator.SimkitXML2Java sx2j = 
                        new viskit.xsd.translator.SimkitXML2Java(bais);
                // first convert XML to java source
                sx2j.unmarshal();
                
                if (debug_io) {
                    System.out.println("Evaluating generated java Event Graph:");
                    System.out.println(sx2j.translate());
                }
                // pass the source for this SimEntity in for compile
                String eventGraphJava = sx2j.translate();
                File eventGraphJavaFile = new File(tempDir,sx2j.getRoot().getName()+".java");
                FileWriter writer = new FileWriter(eventGraphJavaFile);
                writer.write(eventGraphJava);
                writer.flush();
                writer.close();
                // since there may be some kind of event-graph interdependency, compile
                // all .java's "at once"; javac should be able to resolve these if given
                // on the command line all at once.
                eventGraphJavaFiles.add(eventGraphJavaFile);
                
            }
            
            // compile eventGraphJavaFiles and deposit the .classes in the appropriate
            // direcory under tempDir
            
            String[] cmd;
            Iterator it = eventGraphJavaFiles.iterator();
            ArrayList cmdLine = new ArrayList();
            cmdLine.add("-verbose");
            cmdLine.add("-classpath");
            cmdLine.add(System.getProperty("java.class.path"));
            cmdLine.add("-d");
            cmdLine.add(tempDir.getCanonicalPath());
            
            // allow javac to resolve interdependencies by
            // providing all .java's at once
            while (it.hasNext()) {
                File java = (File)(it.next());
                cmdLine.add(java.getCanonicalPath());
            }
            
            // Now do the Assembly
            
            String assemblyJava = sax2j.translate();
            File assemblyJavaFile = new File(tempDir,sax2j.getRoot().getName()+".java");
            FileWriter writer = new FileWriter(assemblyJavaFile);
            writer.write(assemblyJava);
            writer.flush();
            writer.close();
            cmdLine.add(assemblyJavaFile.getCanonicalPath());
            
            cmd = (String[])cmdLine.toArray(new String[]{});
            
            int reti =  com.sun.tools.javac.Main.compile(cmd);
            
            if (debug_io) {
                System.out.println("Evaluating generated java Simulation "+ root.getName() + ":");
                System.out.println(sax2j.translate());
                if (reti != 0) {
                    //System.out.println("\tCompile Failed");
                }
            }
             
            //ClassLoader cloader = Thread.currentThread().getContextClassLoader(); // or not getContextClassLoader()?
            ClassLoader cloader = getContextClassLoader();
            System.out.println(cloader+" Adding file:"+File.separator+tempDir.getCanonicalPath()+File.separator);
            
            if(cloader instanceof Boot) {
                ((Boot)cloader).addURL(new URL("file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator));
            } else if (cloader.getClass().getName().equals("viskit.doe.LocalBootLoader")) {
                System.out.println("doAddURL "+"file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator);
                Method doAddURL = cloader.getClass().getMethod("doAddURL",java.net.URL.class);
                doAddURL.invoke(cloader,new URL("file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator));
                //((LocalBootLoader)cloader).doAddURL(new URL("file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator));
            }
          
            Class asmz = cloader.loadClass(sax2j.root.getPackage()+"."+sax2j.root.getName());
            Constructor asmc = asmz.getConstructors()[0];
            ViskitAssembly sim = (ViskitAssembly)(asmc.newInstance(new Object[] {} ));
            Thread runner = new Thread(sim);
           
            // trumpets...
            
            runner.start();
            try {
                runner.join();
            } catch (InterruptedException ie) {;} // done
            
            // finished running, collect some statistics
            
            simkit.stat.SampleStatistics[] designPointStats = sim.getDesignPointStats();
            simkit.stat.SampleStatistics replicationStat;
            
            // go through and copy in the statistics
            
            viskit.xsd.bindings.assembly.ObjectFactory of = 
                    new viskit.xsd.bindings.assembly.ObjectFactory();
            String statXml;
            
            // first get designPoint stats
            if (designPointStats != null ) try {
                
                for ( int i = 0; i < designPointStats.length; i++) {
                    
                    if (designPointStats[i] instanceof simkit.stat.IndexedSampleStatistics ) {
                        
                        viskit.xsd.bindings.assembly.IndexedSampleStatistics iss = 
                                of.createIndexedSampleStatistics();
                        iss.setName(designPointStats[i].getName());
                        List args = iss.getSampleStatistics();
                        simkit.stat.SampleStatistics[] allStat = 
                                ((simkit.stat.IndexedSampleStatistics)designPointStats[i]).getAllSampleStat();
                        
                        for ( int j = 0; j < allStat.length; j++) {
                            
                            args.add(statForStat(allStat[j]));

                        }
                        statXml = sax2j.marshalToString(iss);
                    } else {
                        statXml = sax2j.marshalToString(statForStat(designPointStats[i]));
                        
                    }
                    
                    if (debug_io)
                        System.out.println(statXml);
                    
                    if (gridRunner != null) { // local gridRunner
                        Class gridRunnerz = gridRunner.getClass();
                        Method mthd = gridRunnerz.getMethod("addDesignPointStat",int.class,int.class,int.class,String.class);
                        mthd.invoke(gridRunner,sampleIndex,designPtIndex,designPointStats.length,statXml);
                        //gridRunner.addDesignPointStat(sampleIndex,designPtIndex,designPointStats.length,statXml);
                    } else {
                        
                        Vector args = new Vector();
                        args.add(usid);
                        args.add(new Integer(sampleIndex));
                        args.add(new Integer(designPtIndex));
                        args.add(new Integer(designPointStats.length));
                        args.add(statXml);
                        if (debug_io) {
                            System.out.println("sending DesignPointStat "+sampleIndex+" "+designPtIndex);
                            System.out.println(statXml);
                        }
                        xmlrpc.execute("gridkit.addDesignPointStat", args);
                    }
                    // replication stats similarly
                    
                    String repName = designPointStats[i].getName();
                    repName = repName.substring(0, repName.length()-5);  // strip off ".mean"
                    
                    for ( int j = 0 ; j < replicationsPerDesignPoint ; j++ ) {
                        replicationStat = sim.getReplicationStat(repName,j);
                        if (replicationStat != null) {
                            try {
                                if (replicationStat instanceof simkit.stat.IndexedSampleStatistics ) {
                                    viskit.xsd.bindings.assembly.IndexedSampleStatistics iss = 
                                            of.createIndexedSampleStatistics();
                                    iss.setName(replicationStat.getName());
                                    List arg = iss.getSampleStatistics();
                                    simkit.stat.SampleStatistics[] allStat =
                                            ((simkit.stat.IndexedSampleStatistics)replicationStat).getAllSampleStat();
                                    for ( int k = 0; k < allStat.length; k++) {
                                        
                                        arg.add(statForStat(allStat[j]));
                                        
                                    }
                                    statXml = sax2j.marshalToString(iss);
                                } else {
                                    statXml = sax2j.marshalToString(statForStat(replicationStat));
                                    
                                }
                                if (debug_io)
                                    System.out.println(statXml);
                                if (gridRunner != null) { // local is a local gridRunner
                                    Class gridRunnerz = gridRunner.getClass();
                                    Method mthd = gridRunnerz.getMethod("addReplicationStat",int.class,int.class,int.class,String.class);
                                    mthd.invoke(gridRunner,sampleIndex,designPtIndex,designPointStats.length,statXml);
                                    //gridRunner.addReplicationStat(sampleIndex,designPtIndex,j,statXml);
                                } else {// use rpc to runner on grid
                                    Vector args = new Vector();
                                    args.add(usid);
                                    args.add(new Integer(sampleIndex));
                                    args.add(new Integer(designPtIndex));
                                    args.add(new Integer(j));
                                    args.add(statXml);
                                    if (debug_io) {
                                        System.out.println("sending ReplicationStat"+sampleIndex+" "+designPtIndex+" "+j);
                                        System.out.println(statXml);
                                    }
                                    xmlrpc.execute("gridkit.addReplicationStat", args);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                    }
                }
                
            } catch (Exception e) { e.printStackTrace(); }
            else System.out.println("No DesignPointStats");
            
            
            // reconnect io
            
            if(!debug_io) {
                System.setOut(new PrintStream(oldOut));
                System.setErr(new PrintStream(oldErr));
            }
            
            // skim through console chatter and organize
            // into log, error, and if non stats property
            // change messages which are wrapped in xml,
            // then so sent as a Results tag. Results 
            // should probably not be named Results as
            // it is really LogMessages, results themselves
            // end up as SampleStatistics as above.
            
            java.io.StringReader sr = new java.io.StringReader(baos.toString());
            java.io.BufferedReader br = new java.io.BufferedReader(sr);
            
            java.io.StringReader esr = new java.io.StringReader(baos.toString());
            java.io.BufferedReader ebr = new java.io.BufferedReader(sr);
            
            try {

                PrintWriter out;
                StringWriter sw;
                String line;
                ArrayList logs = new ArrayList();
                ArrayList propertyChanges = new ArrayList();
                ArrayList errs = new ArrayList();
                
                sw = new StringWriter();
                out = new PrintWriter(sw);
                String qu  = "\"";
                out.println("<Results index="+qu+(taskID-1)+qu+" job="+qu+jobID+qu+" designPoint="+qu+designPtIndex+qu+" sample="+qu+sampleIndex+qu+">");
                while( (line = br.readLine()) != null ) {
                    if (line.indexOf("<PropertyChange") < 0) {
                        logs.add(line);
                    } else {
                        propertyChanges.add(line);
                        // all one line? already added, else :
                        while (line.indexOf("</PropertyChange>") < 0 ) {
                            if ( ( line = br.readLine() ) != null ) {
                                propertyChanges.add(line);
                            }
                        }
                        
                    }
                }
                while( (line = ebr.readLine()) != null ) {
                    errs.add(line);
                }
                out.println("<Log>");
                out.println("<![CDATA[");
                it = logs.iterator();
                while (it.hasNext()) {
                    out.println((String)(it.next()));
                }
                out.println("]]>");
                out.println("</Log>");
                it = propertyChanges.iterator();
                while (it.hasNext()) {
                    out.println((String)(it.next()));
                }
                
                out.println("<Errors>");
                out.println("<![CDATA[");
                it = errs.iterator();
                while (it.hasNext()) {
                    
                    out.println((String)(it.next()));
                    
                }
                out.println("]]>");
                out.println("</Errors>");
                out.println("</Results>");
                out.println();
                
                
                if ( gridRunner != null ) {
                    Class gridRunnerz = gridRunner.getClass();
                    
                    //gridRunner.addResult(sw.toString());
                    Method mthd = gridRunnerz.getMethod("addResult",String.class);
                    mthd.invoke(gridRunner,sw.toString());
                    
                    //gridRunner.removeTask(jobID,taskID);
                    mthd = gridRunnerz.getMethod("removeTask",int.class,int.class);
                    mthd.invoke(gridRunner,jobID,taskID);
                } else {
                    //send results back to front end
                    Vector parms = new Vector();
                    parms.add(usid);
                    parms.add(new String(sw.toString()));
                    
                    if (debug_io) {
                        System.out.println("sending Result ");
                        System.out.println(sw.toString());
                    }
                    xmlrpc.execute("gridkit.addResult", parms);
                    
                    // this could be a new feature of SGE 6.0
                    parms.clear();
                    parms.add(usid);
                    parms.add(new Integer(jobID));
                    parms.add(new Integer(taskID));
                    xmlrpc.execute("gridkit.removeTask", parms);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    private viskit.xsd.bindings.assembly.SampleStatistics statForStat(simkit.stat.SampleStatistics stat) throws Exception {
        viskit.xsd.bindings.assembly.ObjectFactory of = new viskit.xsd.bindings.assembly.ObjectFactory();
        viskit.xsd.bindings.assembly.SampleStatistics sampleStat = of.createSampleStatistics();
        sampleStat.setCount(""+stat.getCount());
        sampleStat.setMaxObs(""+stat.getMaxObs());
        sampleStat.setMean(""+stat.getMean());
        sampleStat.setMinObs(""+stat.getMinObs());
        sampleStat.setName(stat.getName());
        sampleStat.setSamplingType(stat.getSamplingType().toString());
        sampleStat.setStandardDeviation(""+stat.getStandardDeviation());
        sampleStat.setVariance(""+stat.getVariance());
        return sampleStat;
    }
    


}
