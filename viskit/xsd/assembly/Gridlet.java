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
 */

package viskit.xsd.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcClientLite;
import bsh.Interpreter;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.bindings.eventgraph.*;
import javax.xml.bind.Element;
import simkit.stat.SampleStatistics;


/**
 *
 * @author Rick Goldberg
 */

public class Gridlet extends Thread {
    SimkitAssemblyXML2Java sax2j;
    int taskID;
    int numTasks;
    int jobID;
    int port;
    String frontHost;
    String usid;
    String filename;
    
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
                sax2j = new SimkitAssemblyXML2Java(filename);
            } else {
                throw new RuntimeException("Not running as SGE job?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) {
        
    }
    
    public void run() {
        SimkitAssemblyType root;
        
        
        sax2j.unmarshal();
        root = sax2j.getRoot();
        
        ExperimentType exp = root.getExperiment();
        int runsPerDesignPt = Integer.parseInt(exp.getReplicationsPerDesignPoint());
        List designPoints = exp.getDesignPoint();
        int designPtsSize = designPoints.size(); // aka getCount() on local side
        int designPtIndex = (taskID-1)/runsPerDesignPt;
        int runIndex = (taskID-1)%runsPerDesignPt;
        
        DesignPointType designPoint = (DesignPointType)(designPoints.get(designPtIndex));
        List designParams = designPoint.getTerminalParameter();
        List params = root.getDesignParameters();
        Iterator itd = designParams.iterator();
        Iterator itp = params.iterator();
        boolean debug_io = Boolean.valueOf(exp.getDebug()).booleanValue();
        
        if(sax2j.debug)System.out.println(filename+" Grid Task ID "+taskID+" of "+numTasks+" tasks in jobID "+jobID+" which is Run "+ runIndex + " in DesignPoint "+designPtIndex);
        //exp.setBatchID(fileBaseName+" Grid Task ID "+taskID+" of "+lastTask+" tasks in jobID "+jobID+" which is Run "+ runIndex + " in DesignPoint "+designPtIndex);
        
        while ( itd.hasNext() && itp.hasNext() ) {
            TerminalParameterType param = (TerminalParameterType)(itp.next());
            TerminalParameterType designParam = (TerminalParameterType)(itd.next());
            param.setValue(designParam.getValue());
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
            
            if(!debug_io) {
                System.setErr(err);
                System.setOut(log);
            }
            
            bsh.Interpreter bsh = new bsh.Interpreter();
            //bsh.setClassLoader(SimkitAssemblyXML2Java.class.getClassLoader());
            
            List depends = root.getEventGraph();
            Iterator di = depends.iterator();
            
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
                
                viskit.xsd.translator.SimkitXML2Java sx2j = new viskit.xsd.translator.SimkitXML2Java(bais);
                sx2j.unmarshal();
                
                System.out.println("Evaluating generated java Event Graph:");
                System.out.println(sx2j.translate());
                
                bsh.eval(sx2j.translate());
                
            }
            
            if (sax2j.debug) {
                System.out.println("Evaluating generated java Simulation "+ root.getName() + ":");
                System.out.println(sax2j.translate());
            }
            bsh.eval(sax2j.translate());
            bsh.eval("sim = new "+ root.getName() +"();");
            bsh.eval("sim.main(new String[0])"); // or sim.start();
            ViskitAssembly sim = (ViskitAssembly) bsh.get("sim");
            SampleStatistics[] designPointStats = sim.getDesignPointStats();
            // really want rep stats?
            //SampleStatistics[] replicationStats = sim.getReplicationStats(i);
            
            if(!debug_io) {
                System.setOut(new PrintStream(oldOut));
                System.setErr(new PrintStream(oldErr));
            }
            
            java.io.StringReader sr = new java.io.StringReader(baos.toString());
            java.io.BufferedReader br = new java.io.BufferedReader(sr);
            
            java.io.StringReader esr = new java.io.StringReader(baos.toString());
            java.io.BufferedReader ebr = new java.io.BufferedReader(sr);
            
            try {
                XmlRpcClientLite xmlrpc = new XmlRpcClientLite(frontHost,port);
                PrintWriter out;
                StringWriter sw;
                String line;
                ArrayList logs = new ArrayList();
                ArrayList propertyChanges = new ArrayList();
                ArrayList errs = new ArrayList();
                
                sw = new StringWriter();
                out = new PrintWriter(sw);
                String qu  = "\"";
                out.println("<Results index="+qu+(taskID-1)+qu+" job="+qu+jobID+qu+" design="+qu+designPtIndex+qu+" run="+qu+runIndex+qu+">");
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
                
                //debug
                System.out.println("sending Result ");
                System.out.println(sw.toString());
                
                xmlrpc.execute("gridkit.addResult", parms);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        } catch (bsh.EvalError ee) {
            ee.printStackTrace();
        }
    }
    
}
