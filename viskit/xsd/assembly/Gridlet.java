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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
import bsh.Interpreter;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.bindings.eventgraph.*;
import viskit.xsd.cli.Boot;
import javax.xml.bind.Element;
import simkit.stat.SampleStatistics;


/**
 *
 * @author Rick Goldberg
 */

public class Gridlet extends Thread {
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
                throw new RuntimeException("Not running as SGE job?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
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
            
            bsh.Interpreter bsh = new bsh.Interpreter();
            
            List depends = root.getEventGraph();
            Iterator di = depends.iterator();
            
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
            } catch (InterruptedException ie) {;} // done
            
            // finished running, collect some statistics
            // from the beanshell context to java
            
            simkit.stat.SampleStatistics[] designPointStats = sim.getDesignPointStats();
            simkit.stat.SampleStatistics replicationStat;
            
            // go through and copy in the statistics
            
            viskit.xsd.bindings.assembly.ObjectFactory of = 
                    new viskit.xsd.bindings.assembly.ObjectFactory();
            String statXml;
            
            // first designPoint stats
            // TBD synchronize designPointStats to notify front end
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
                                args.clear();
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
                
            } catch (Exception e) {
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
