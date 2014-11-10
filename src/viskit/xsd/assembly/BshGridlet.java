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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import static java.lang.Boolean.valueOf;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperties;
import static java.lang.System.setErr;
import static java.lang.System.setOut;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.cli.Boot;


/**
 * @author Rick Goldberg
 * @version $Id$
 */
public class BshGridlet extends Thread {
    SimkitAssemblyXML2Java sax2j;
    XmlRpcClientLite xmlrpc;
    int taskID;
    int numTasks;
    int jobID;
    int port;
    String frontHost;
    String usid;
    String filename;
    String pwd;

    // TODO: XML RPC version does not yet support generics
    @SuppressWarnings("unchecked")
    public BshGridlet() {

        try {

            Process pr = getRuntime().exec("env");
            InputStream is = pr.getInputStream();
            Properties p = getProperties();
            p.load(is);

            if (p.getProperty("SGE_TASK_ID")!=null) {

                taskID=parseInt(p.getProperty("SGE_TASK_ID"));
                jobID=parseInt(p.getProperty("JOB_ID"));
                numTasks=parseInt(p.getProperty("SGE_TASK_LAST"));
                frontHost = p.getProperty("SGE_O_HOST");
                usid = p.getProperty("USID");
                filename = p.getProperty("FILENAME");
                port = parseInt(p.getProperty("PORT"));
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
                    Vector<Object> v = new Vector<>();
                    v.add(usid);
                    v.add(jobID);
                    xmlrpc.execute("gridkit.setJobID", v);
                }

                // get any thirdPartyJars and install them now
                Vector<String> v = new Vector<>();
                v.add(usid);

                // TODO: fix generics
                v = (Vector) xmlrpc.execute("gridkit.getJars", v);
                Enumeration e = v.elements();
                ClassLoader boot = currentThread().getContextClassLoader();
                if (boot instanceof Boot) {
                    while (e.hasMoreElements()) {
                        ((Boot) boot).addJar(new URL((String) e.nextElement()));
                    }
                } else {
                    if (!v.isEmpty()) {
                        throw new RuntimeException("You should really be using viskit.xsd.cli.Boot loader to launch Gridlets!");
                    }
                }

            } else {
                throw new RuntimeException("Not running as SGE job?");
            }
        } catch (IOException | RuntimeException | XmlRpcException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Gridlet gridlet = new Gridlet();
        gridlet.start();
    }

    @Override
    public void run() {

        SimkitAssembly root;
        sax2j.unmarshal();
        root = sax2j.getRoot();

        Experiment exp = root.getExperiment();
        int replicationsPerDesignPoint = parseInt(exp.getReplicationsPerDesignPoint());
        List<Sample> samples = exp.getSample();

        List<TerminalParameter> designParams = root.getDesignParameters();
        int sampleIndex = (taskID-1) / designParams.size();
        int designPtIndex = (taskID-1) % designParams.size();

        Sample sample = samples.get(sampleIndex);
        List<DesignPoint> designPoints = sample.getDesignPoint();

        DesignPoint designPoint = designPoints.get(designPtIndex);
        List<TerminalParameter> designArgs = designPoint.getTerminalParameter();
        Iterator itd = designParams.iterator();
        Iterator itp = designArgs.iterator();

        boolean debug_io = valueOf(exp.getDebug());

        if(debug_io) {
            System.out.println(filename + " Grid Task ID " + taskID + " of " + numTasks + " tasks in jobID " + jobID + " which is DesignPoint " + designPtIndex + " of Sample " + sampleIndex);
        }

        //pass design args into design params
        while ( itd.hasNext() && itp.hasNext() ) {
            TerminalParameter arg = (TerminalParameter)(itp.next());
            TerminalParameter designParam = (TerminalParameter)(itd.next());
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
                setErr(err);
                setOut(log);
            }

            bsh.Interpreter bsh = new bsh.Interpreter();

            List<EventGraph> depends = root.getEventGraph();
            Iterator di = depends.iterator();

            // submit all EventGraphs to the beanshell context
            while ( di.hasNext() ) {

                EventGraph d = (EventGraph) (di.next());
                ByteArrayInputStream bais;
                String content = d.getContent();

                bais = new ByteArrayInputStream(content.getBytes());

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
                // pass the source for this SimEntity in for "compile"
                bsh.eval(sx2j.translate());
            }

            if (debug_io) {
                System.out.println("Evaluating generated java Simulation "+ root.getName() + ":");
                System.out.println(sax2j.translate());
            }
            //
            // Now do the Assembly
            //
            // first beanshell "compile" and instance
            //
            bsh.eval(sax2j.translate());
            bsh.eval("sim = new "+ root.getName() +"();");
            ViskitAssembly sim = (ViskitAssembly) bsh.get("sim");
            //
            // thread obtained ViskitAssembly and run it
            //
            Thread runner = new Thread(sim);
            runner.start();
            try {
                runner.join();
            } catch (InterruptedException ie) {} // done

            // finished running, collect some statistics
            // from the beanshell context to java

            simkit.stat.SampleStatistics[] designPointStats = sim.getDesignPointStats();
            simkit.stat.SampleStatistics replicationStat;

            // go through and copy in the statistics

            viskit.xsd.bindings.assembly.ObjectFactory of =
                    new viskit.xsd.bindings.assembly.ObjectFactory();
            String statXml;

            // first get designPoint stats
            if (designPointStats != null ) {
                try {

                    for (simkit.stat.SampleStatistics designPointStat : designPointStats) {
                        if (designPointStat instanceof simkit.stat.IndexedSampleStatistics) {
                            viskit.xsd.bindings.assembly.IndexedSampleStatistics iss = of.createIndexedSampleStatistics();
                            iss.setName(designPointStat.getName());
                            List<SampleStatistics> args = iss.getSampleStatistics();
                            simkit.stat.SampleStatistics[] allStat = ((simkit.stat.IndexedSampleStatistics) designPointStat).getAllSampleStat();
                            for (simkit.stat.SampleStatistics allStat1 : allStat) {
                                // TODO: fix generics
                                args.add(statForStat(allStat1));
                            }
                            statXml = sax2j.marshalToString(iss);
                        } else {
                            statXml = sax2j.marshalToString(statForStat(designPointStat));
                        }
                        if (debug_io) {
                            System.out.println(statXml);
                        }
                        Vector<Object> args = new Vector<>();
                        args.add(usid);
                        args.add(sampleIndex);
                        args.add(designPtIndex);
                        args.add(designPointStats.length);
                        args.add(statXml);
                        if (debug_io) {
                            System.out.println("sending DesignPointStat " + sampleIndex + " " + designPtIndex);
                            System.out.println(statXml);
                        }
                        xmlrpc.execute("gridkit.addDesignPointStat", args);
                        // replication stats similarly
                        String repName = designPointStat.getName();
                        repName = repName.substring(0, repName.length() - 6); // strip off ".count"
                        for (int j = 0; j < replicationsPerDesignPoint; j++) {
                            replicationStat = sim.getReplicationStat(repName, j);
                            if (replicationStat != null) {
                                try {
                                    if (replicationStat instanceof simkit.stat.IndexedSampleStatistics) {
                                        viskit.xsd.bindings.assembly.IndexedSampleStatistics iss = of.createIndexedSampleStatistics();
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
                                        System.out.println(statXml);
                                    }
                                    args.clear();
                                    args.add(usid);
                                    args.add(sampleIndex);
                                    args.add(designPtIndex);
                                    args.add(j);
                                    args.add(statXml);
                                    if (debug_io) {
                                        System.out.println("sending ReplicationStat" + sampleIndex + " " + designPtIndex + " " + j);
                                        System.out.println(statXml);
                                    }
                                    xmlrpc.execute("gridkit.addReplicationStat", args);
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                System.out.println("No DesignPointStats");
                // reconnect io
            }

            // reconnect io

            if(!debug_io) {
                setOut(new PrintStream(oldOut));
                setErr(new PrintStream(oldErr));
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
            java.io.BufferedReader ebr = new java.io.BufferedReader(sr);

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
                Iterator it = logs.iterator();
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

                //send results back to front end
                Vector<Object> parms = new Vector<>();
                parms.add(usid);
                parms.add(sw.toString());

                if (debug_io) {
                    System.out.println("sending Result ");
                    System.out.println(sw.toString());
                }
                xmlrpc.execute("gridkit.addResult", parms);

                // this could be a new feature of SGE 6.0
                parms.clear();

                parms.add(usid);
                parms.add(jobID);
                parms.add(taskID);
                xmlrpc.execute("gridkit.removeTask", parms);

            } catch (IOException | XmlRpcException e) {
                e.printStackTrace();
            }

        } catch (bsh.EvalError ee) {
            ee.printStackTrace();
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
