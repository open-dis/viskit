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
package viskit.gridlet;

import edu.nps.util.Log4jUtilities;
import edu.nps.util.TempFileManager;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.doe.DoeException;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.assembly.ViskitAssembly;
import viskit.xsd.bindings.assembly.DesignPoint;
import viskit.xsd.bindings.assembly.EventGraph;
import viskit.xsd.bindings.assembly.Experiment;
import viskit.xsd.bindings.assembly.IndexedSampleStatistics;
import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.Sample;
import viskit.xsd.bindings.assembly.SampleStatistics;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.bindings.assembly.TerminalParameter;
import viskit.xsd.cli.Boot;
import viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java;

/**
 *
 * @author Rick Goldberg
 *
 * Gridlet indexes itself to a DesignPoint within the Experiment file,
 * creates an Assembly from that DesignPoint, compiles and runs it.
 *
 * This is the compiled version of the Gridlet, formerly interpreted,
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
 * Get 'em while they're hot, these Gridlets should outperform the interpreted
 * mode, but most importantly enable entities with a large number of
 * parameters.
 *
 * @version $Id$
 */
public class Gridlet extends Thread
{
    static final Logger LOG = LogManager.getLogger();

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
            this.sax2j = new SimkitAssemblyXML2Java(expFile.toURI().toURL().openStream());
        } catch (IOException ex) {
            throw new DoeException(ex.getMessage());
        }
    }

    // TODO: XML RPC version does not yet support generics
    @SuppressWarnings("unchecked")
    public Gridlet() {

        try {
            Map<String, String> props = System.getenv();

            if (props.get("SGE_TASK_ID")!=null) {
                taskID=Integer.parseInt(props.get("SGE_TASK_ID"));
                jobID=Integer.parseInt(props.get("JOB_ID"));
                numTasks=Integer.parseInt(props.get("SGE_TASK_LAST"));
                frontHost = props.get("SGE_O_HOST");
                usid = props.get("USID");
                filename = props.get("FILENAME");
                port = Integer.parseInt(props.get("PORT"));
                pwd = props.get("PWD");
                sax2j = new SimkitAssemblyXML2Java(new URI("file:"+pwd+"/"+filename).toURL().openStream());
                LOG.info(taskID+ " "+ jobID+" "+usid+" "+filename+" "+pwd);
                //TBD: should also check if SSL
                xmlrpc = new XmlRpcClientLite(frontHost,port);

                // still needed?
                if (taskID == 1) {

                    Vector<Object> v = new Vector<>();
                    v.add(usid);
                    v.add(jobID);
                    xmlrpc.execute("gridkit.setJobID", v);
                }

                // get any thirdPartyJars and install them now
                Vector<Object> v = new Vector<>();
                v.add(usid);

                // TODO: Fix generics
                v = (Vector) xmlrpc.execute("gridkit.getJars",v);
                Enumeration e = v.elements();
                ClassLoader boot = ViskitGlobals.instance().getViskitApplicationClassLoader();
                if (boot instanceof Boot) {
                    while (e.hasMoreElements()) {
                        ((Boot) boot).addJar(new URI((String) e.nextElement()).toURL());
                    }
                } else {
                    if (!v.isEmpty()) {
                        throw new RuntimeException("You should really be using viskit.xsd.cli.Boot loader to launch Gridlets!");
                    }
                }

            } else {
                // check if LocalBootLoader mode, otherwise throw exception
                Object loaderO = ViskitGlobals.instance().getViskitApplicationClassLoader();
                Class<?> loaderz = loaderO.getClass();
                if ( !( loaderz.getName().equals(ViskitStatics.LOCAL_BOOT_LOADER) ) ) {
                    throw new RuntimeException("Not running as SGE job or local mode?");
                }
                usid = "LOCAL-RUN";
            }
        } catch (IOException | XmlRpcException | RuntimeException | URISyntaxException e) {
            e.printStackTrace(System.err);
        }
    }

    public void setGridRunner(Object gridRunner) {
        this.gridRunner = gridRunner;
    }

    public void setExperimentFile(File experimentFile) {
        this.expFile = experimentFile;
        this.filename = expFile.getName();
        try {
            //See comment in LocalTaskQueue, try commenting out the line, and uncommenting the printlns to see it up close
            //LOG.info("Gridlet.setExperimentFile, "+Thread.currentThread()+"'s loader is "+ ViskitGlobals.instance().getViskitApplicationClassLoader());
            //LOG.info("Gridlet.setExperimentFile, "+this+"'s loader is "+ getContextClassLoader());
            sax2j = new SimkitAssemblyXML2Java(experimentFile.toURI().toURL().openStream());
        } catch (IOException ex) {
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

    public static void main(String[] args) {
        Gridlet gridlet = new Gridlet();
        gridlet.start();
    }

    @Override
    public void run() {

        SimkitAssembly root;
        sax2j.unmarshal();
        root = sax2j.getAssemblyRoot();

        Experiment exp = root.getExperiment();
        int replicationsPerDesignPoint = Integer.parseInt(exp.getReplicationsPerDesignPoint());
        List<Sample> samples = exp.getSample();

        List<TerminalParameter> designParams = root.getDesignParameters();
        int sampleIndex = (taskID-1) / designParams.size();
        int designPtIndex = (taskID-1) % designParams.size();

        Sample sample = samples.get(sampleIndex);
        List<DesignPoint> designPoints = sample.getDesignPoint();

        DesignPoint designPoint = designPoints.get(designPtIndex);
        List<TerminalParameter> designArgs = designPoint.getTerminalParameter();
        Iterator<TerminalParameter> itd = designParams.iterator();
        Iterator<TerminalParameter> itp = designArgs.iterator();

        boolean debug_io = Boolean.parseBoolean(exp.getDebug());
        debug_io = false;
        //if(debug_io)LOG.info(filename+" Grid Task ID "+taskID+" of "+numTasks+" tasks in jobID "+jobID+" which is DesignPoint "+designPtIndex+" of Sample "+ sampleIndex);

        //pass design args into design params
        while ( itd.hasNext() && itp.hasNext() ) {
            TerminalParameter arg = itp.next();
            TerminalParameter designParam = itd.next();
            designParam.setValue(arg.getValue());
        }

        try {

            //processed into results tag, sent back to
            //SGE_O_HOST at socket in raw XML
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream log = new PrintStream(baos);
            OutputStream oldOut = System.out;

            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            PrintStream err = new PrintStream(baos2);
            OutputStream oldErr = System.err;

            // disconnect io

            if(!debug_io) {
                System.setErr(err);
                System.setOut(log);
            }

            File tempDir;
            String classPath=""; // for javac cmd line
            if (!usid.equals("LOCAL-RUN")) {

                tempDir = TempFileManager.createTempFile("gridkit","doe");
                tempDir = TempFileManager.createTempDir(tempDir);
                // setting a classpath this way, we don't need to keep track of the
                // actual files, ie "javac -d tempDir" will create subdirs
                classPath = System.getProperty("java.class.path");
                System.setProperty("java.class.path", classPath+File.pathSeparator+tempDir.getCanonicalPath());
                classPath = System.getProperty("java.class.path");
            } else {

                tempDir = TempFileManager.createTempFile("viskit","doe");
                tempDir = TempFileManager.createTempDir(tempDir);

                Object loader = getContextClassLoader();
                Class<?> loaderz = loader.getClass();
                String[] classPaths = (String[]) (loaderz.getMethod("getClassPath",new Class<?>[] {})).invoke(loader, new Object[] {});
                for (String path : classPaths) {
                    classPath += path + File.pathSeparator;
                }
            }

            List<EventGraph> depends = root.getEventGraph();
            List<File> javaFiles = new ArrayList<>();

            // submit all EventGraphs
            for (EventGraph d : depends) {
                ByteArrayInputStream bais;
                String content = d.getContent();

                bais = new ByteArrayInputStream(content.getBytes());

                // generate java for the eventGraph and evaluate a loaded
                // class
                SimkitEventGraphXML2Java sx2j = new SimkitEventGraphXML2Java(bais);
                // first convert XML to java source
                sx2j.unmarshal();

                if (debug_io) {
                    LOG.info("Evaluating generated java Event Graph:");
                    LOG.info(sx2j.translate());
                }
                // pass the source for this SimEntity in for compile
                String eventGraphJava = sx2j.translate();
                File eventGraphJavaFile = new File(tempDir,sx2j.getRoot().getName()+".java");
                try (FileWriter writer = new FileWriter(eventGraphJavaFile)) {
                    writer.write(eventGraphJava);
                }
                // since there may be some kind of event-graph interdependency, compile
                // all .java's "at once"; javac should be able to resolve these if given
                // on the command line all at once.
                javaFiles.add(eventGraphJavaFile);
            }

            // compile eventGraphJavaFiles and deposit the .classes in the appropriate
            // direcory under tempDir

            List<String> cmdLine = new ArrayList<>();

            cmdLine.add("-Xlint:unchecked");
            cmdLine.add("-Xlint:deprecation");
            cmdLine.add("-verbose");
            cmdLine.add("-cp");
            cmdLine.add(classPath);
            cmdLine.add("-d");
            cmdLine.add(tempDir.getCanonicalPath());

            // Now add the Assembly

            String assemblyJava = sax2j.translateIntoJavaSource();
            File assemblyJavaFile = new File(tempDir, root.getName()+".java");
            try (FileWriter writer = new FileWriter(assemblyJavaFile)) {
                writer.write(assemblyJava);
            }
            javaFiles.add(assemblyJavaFile);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics,null,null);
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles);
            compiler.getTask(null, fileManager, null, cmdLine, null, compilationUnits).call();

            if (/*debug_io*/true) {
                LOG.info("Evaluating generated java Simulation "+ root.getName() + ":");
                LOG.info(sax2j.translateIntoJavaSource());
                URI cwd = tempDir.toURI();
                for (Diagnostic<? extends JavaFileObject> diagnostic :
                    diagnostics.getDiagnostics()) {
                    String kind = diagnostic.getKind().toString().toLowerCase();
                    JavaFileObject source = diagnostic.getSource();
                    if (source != null) {
                        long line = diagnostic.getLineNumber();
                        URI name = cwd.relativize(source.toUri());
                        if (line != Diagnostic.NOPOS) {
                            System.out.format("%s:%s: %s: %s%n", name, line, kind, diagnostic.getMessage(null));
                        }
                        else {
                            System.out.format("%s:1: %s: %s%n", name, kind, diagnostic.getMessage(null));
                        }
                    } else {
                        System.out.format("%s: %s%n", kind,
                                diagnostic.getMessage(null));
                    }
                }
            }

            ClassLoader cloader = getContextClassLoader();
            LOG.info(cloader+" Adding file:"+File.separator+tempDir.getCanonicalPath()+File.separator);

            if(cloader instanceof Boot) {
                ((Boot) cloader).addURL(new URI("file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator).toURL());
            } else if (cloader.getClass().getName().equals(ViskitStatics.LOCAL_BOOT_LOADER)) {
                LOG.info("doAddURL "+"file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator);
                Method doAddURL = cloader.getClass().getMethod("doAddURL",java.net.URL.class);
                doAddURL.invoke(cloader, new URI("file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator).toURL());
                //((LocalBootLoader)cloader).doAddURL(new URL("file:"+File.separator+File.separator+tempDir.getCanonicalPath()+File.separator));
            }

            Class<?> asmz = cloader.loadClass(sax2j.getAssemblyRoot().getPackage()+"."+sax2j.getAssemblyRoot().getName());
            Constructor<?> asmc = asmz.getConstructors()[0];
            ViskitAssembly sim = (ViskitAssembly) (asmc.newInstance(new Object[] {} ));
            sim.setEnableAnalystReports(false); // not needed in cluster mode, and threadly
            Thread runner = new Thread(sim);

            // trumpets...

            runner.start();
            try {
                runner.join();
            } catch (InterruptedException ie) {} // done

            // finished running, collect some statistics

            simkit.stat.SampleStatistics[] designPointStats = sim.getDesignPointSampleStatistics();
            simkit.stat.SampleStatistics replicationStat;

            // go through and copy in the statistics

            ObjectFactory of = new ObjectFactory();
            String statXml;

            // first get designPoint stats
            if (designPointStats != null ) {
                try {
                    for (simkit.stat.SampleStatistics designPointStat : designPointStats) {
                        if (designPointStat instanceof simkit.stat.IndexedSampleStatistics) {
                            // tbd handle this for local case too
                            IndexedSampleStatistics iss = of.createIndexedSampleStatistics();
                            iss.setName(designPointStat.getName());
                            List<SampleStatistics> args = iss.getSampleStatistics();
                            simkit.stat.SampleStatistics[] allStat = ((simkit.stat.IndexedSampleStatistics) designPointStat).getAllSampleStat();
                            for (simkit.stat.SampleStatistics allStat1 : allStat) {
                                args.add(statForStat(allStat1));
                            }
                            statXml = sax2j.marshalFragmentToString(iss);
                        } else {
                            statXml = sax2j.marshalFragmentToString(statForStat(designPointStat));
                        }
                        if (debug_io) {
                            LOG.info(statXml);
                        }
                        if (gridRunner != null) {
                            // local gridRunner
                            Class<?> gridRunnerz = gridRunner.getClass();
                            Method mthd = gridRunnerz.getMethod("addDesignPointStat", int.class, int.class, int.class, String.class);
                            mthd.invoke(gridRunner, sampleIndex, designPtIndex, designPointStats.length, statXml);
                        } else {

                            Vector<Object> args = new Vector<>();
                            args.add(usid);
                            args.add(sampleIndex);
                            args.add(designPtIndex);
                            args.add(designPointStats.length);
                            args.add(statXml);
                            if (debug_io) {
                                LOG.info("sending DesignPointStat " + sampleIndex + " " + designPtIndex);
                                LOG.info(statXml);
                            }
                            xmlrpc.execute("gridkit.addDesignPointStat", args);
                        }
                        // replication stats similarly
                        String repName = designPointStat.getName();
                        repName = repName.substring(0, repName.length() - 6); // strip off ".count"
                        for (int j = 0; j < replicationsPerDesignPoint; j++) {
                            replicationStat = sim.getReplicationSampleStatistics(repName, j);
                            if (replicationStat != null) {
                                try {
                                    if (replicationStat instanceof simkit.stat.IndexedSampleStatistics) {
                                        IndexedSampleStatistics iss = of.createIndexedSampleStatistics();
                                        iss.setName(replicationStat.getName());

                                        List<SampleStatistics> arg = iss.getSampleStatistics();
                                        simkit.stat.SampleStatistics[] allStat = ((simkit.stat.IndexedSampleStatistics) replicationStat).getAllSampleStat();
                                        for (simkit.stat.SampleStatistics allStat1 : allStat) {
                                            arg.add(statForStat(allStat[j]));
                                        }
                                        statXml = sax2j.marshalToString(iss);
                                    } else {
                                        statXml = sax2j.marshalToString(statForStat(replicationStat));
                                    }
                                    if (debug_io) {
                                        LOG.info(statXml);
                                    }
                                    if (gridRunner != null) {
                                        // local is a local gridRunner
                                        Class<?> gridRunnerz = gridRunner.getClass();
                                        Method mthd = gridRunnerz.getMethod("addReplicationStat", int.class, int.class, int.class, String.class);
                                        mthd.invoke(gridRunner, sampleIndex, designPtIndex, j, statXml);
                                        //gridRunner.addReplicationStat(sampleIndex,designPtIndex,j,statXml);
                                    } else {
                                        // use rpc to runner on grid
                                        Vector<Object> args = new Vector<>();
                                        args.add(usid);
                                        args.add(sampleIndex);
                                        args.add(designPtIndex);
                                        args.add(j);
                                        args.add(statXml);
                                        if (debug_io) {
                                            LOG.info("sending ReplicationStat" + sampleIndex + " " + designPtIndex + " " + j);
                                            LOG.info(statXml);
                                        }
                                        xmlrpc.execute("gridkit.addReplicationStat", args);
                                    }
                                } 
                                catch (Exception e) {
                                    LOG.error("run() exception: " + e.getMessage());
                                }
                            }
                        }
                    }
                } 
                catch (Exception e) {
                    LOG.error("run() exception: " + e.getMessage());
                }
            }
            else {
                LOG.info("run() problem: No DesignPointStats");


                // reconnect io
            }


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

            StringReader sr = new java.io.StringReader(baos.toString());
            BufferedReader br = new java.io.BufferedReader(sr);

            StringReader esr = new java.io.StringReader(baos.toString());
            BufferedReader ebr = new java.io.BufferedReader(esr);

            // TBD: The following only works correctly in grid mode, due to multithreaded mode's shared
            // access to System.out. Either each thread synchronizes on System.out at the
            // start of the run method, or expect verbose messages to appear in the wrong
            // index. There doesn't appear to be a way to set Simkit's output stream for
            // verbose true. Synchronizing on System.out is probably not the best thing, as
            // it defeats the whole purpose of multithreading if there is more than one CPU.
            // In this case it is probably best to turn verbose false, and emulate the built-in
            // Simkit messages via SimEventListener.
            try {

                PrintWriter out;
                StringWriter sw;
                String line;
                List<String> logs = new ArrayList<>();
                List<String> propertyChanges = new ArrayList<>();
                List<String> errs = new ArrayList<>();

                sw = new StringWriter();
                out = new PrintWriter(sw);
                String qu  = "\"";
                out.println("<Results index="+qu+(taskID-1)+qu+" job="+qu+jobID+qu+" designPoint="+qu+designPtIndex+qu+" sample="+qu+sampleIndex+qu+">");
                while( (line = br.readLine()) != null ) {
                    if (!line.contains("<PropertyChange")) {
                        logs.add(line);
                    } else {
                        propertyChanges.add(line);
                        // all one line? already added, else :
                        while (!line.contains("</PropertyChange>") ) {
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
                for (String lg : logs) {
                    out.println(lg);
                }
                out.println("]]>");
                out.println("</Log>");
                for (String pCh : propertyChanges) {
                    out.println(pCh);
                }

                out.println("<Errors>");
                out.println("<![CDATA[");
                for (String error : errs) {
                    out.println(error);
                }
                out.println("]]>");
                out.println("</Errors>");
                out.println("</Results>");
                out.println();

                if ( gridRunner != null ) {
                    Class<?> gridRunnerz = gridRunner.getClass();

                    //gridRunner.addResult(sw.toString());
                    Method mthd = gridRunnerz.getMethod("addResult",String.class);
                    mthd.invoke(gridRunner,sw.toString());

                    //gridRunner.removeTask(jobID,taskID);
                    mthd = gridRunnerz.getMethod("removeTask",int.class,int.class);
                    mthd.invoke(gridRunner,jobID,taskID);
                } else {
                    //send results back to front end
                    Vector<Object> parms = new Vector<>();
                    parms.add(usid);
                    parms.add(sw.toString());

                    if (debug_io) {
                        LOG.info("sending Result ");
                        LOG.info(sw.toString());
                    }
                    xmlrpc.execute("gridkit.addResult", parms);

                    // this could be a new feature of SGE 6.0
                    parms.clear();

                    parms.add(usid);
                    parms.add(jobID);
                    parms.add(taskID);
                    xmlrpc.execute("gridkit.removeTask", parms);
                }
            } catch (IOException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | XmlRpcException e) {
                e.printStackTrace(System.err);
            }

        } catch (IOException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | InstantiationException | URISyntaxException e) {
            e.printStackTrace(System.err);
        }
    }

    private SampleStatistics statForStat(simkit.stat.SampleStatistics stat) throws Exception {
        ObjectFactory of = new ObjectFactory();
        SampleStatistics sampleStat = of.createSampleStatistics();
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
