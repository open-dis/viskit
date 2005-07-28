/*
 * SimkitAssemblyXML2Java.java
 *
 * Created on April 1, 2004, 10:09 AM
 */

package viskit.xsd.assembly;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import javax.xml.bind.JAXBContext; 
import javax.xml.bind.JAXBException; 
import javax.xml.bind.Unmarshaller; 
import javax.xml.bind.Marshaller; 
import javax.xml.transform.stream.StreamSource;
import viskit.xsd.bindings.assembly.*;
import simkit.random.RandomNumber;
import simkit.random.MersenneTwister;
import org.apache.xmlrpc.*;

/**
 *
 * @author  Rick Goldberg
 *
 */

public class SimkitAssemblyXML2Java implements XmlRpcHandler {

    SimkitAssemblyType root;
    InputStream fileInputStream;
    String fileBaseName;
    File inputFile;
    JAXBContext jaxbCtx;
    GridTaskGetter tasker;
    AssemblyServer assemblyServer;
    int port;
    int count; // of design points
    int totalResults; // of total runs
    boolean busy = false;

    /* convenience Strings for formatting */

    final private String sp  = " ";
    final private String sp4 = sp+sp+sp+sp;
    final private String sp8 = sp4+sp4;
    final private String sp12 = sp8+sp4;
    final private String sp16 = sp8+sp8;
    final private String ob  = "{";
    final private String cb  = "}";
    final private String sc  = ";";
    final private String cm  = ",";
    final private String lp  = "(";
    final private String rp  = ")";
    final private String eq  = "=";
    final private String pd  = ".";
    final private String qu  = "\"";
    final private String nw = "new";

    
    /** 
     * Creates a new instance of SimkitAssemblyXML2Java 
     * when used from another class, instance this
     * with a String for the name of the xmlFile.
     *
     * If the xmlFile contains DesignParameters, then it must be a grid
     * experiment, in which case, calculates the design points and runs
     * grid tasks.
     *
     * Otherwise, it runs the file as a plain assembly, no grid.
     *
     */

    public SimkitAssemblyXML2Java(String xmlFile) {
	this.fileBaseName = baseNameOf(xmlFile);
	try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
            this.fileInputStream = Class.forName("viskit.xsd.assembly.SimkitAssemblyXML2Java").getClassLoader().getResourceAsStream(xmlFile);
	} catch ( Exception e ) {
	    e.printStackTrace();
	} 
        
        tasker = new GridTaskGetter(this);
	
    }
    
    
    /** Started from qsub grid script on grid nodes. */
    public SimkitAssemblyXML2Java(int port, String fileName) {
        this(fileName);
        this.port = port;
    }
    
    
    /** 
     * Starts the server in "local" mode. This is the front end service.
     * Sets up an XML-RPC webserver to read in an assembly from DOE panel.
     * Once read, handler is added for reports, then DesignPoints calculated 
     * and each run on grid nodes. 
     */
    
    public SimkitAssemblyXML2Java(int port) {
        //ServerSocket servs = null;
        this.port = port;
        try {
            this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
            assemblyServer = new AssemblyServer(this,port);
            assemblyServer.start();
        } catch ( Exception e) {
            e.printStackTrace();
        }
        
        
    }
    
    
    /** Used by Viskit */
    public SimkitAssemblyXML2Java(File f) throws Exception {
        this.fileBaseName = baseNameOf(f.getName());
	this.inputFile = f;
        this.jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
        this.fileInputStream = new FileInputStream(f);
    }

    public void unmarshal() {
	Unmarshaller u;
	try {
	    u = jaxbCtx.createUnmarshaller();
	    this.root = (SimkitAssemblyType) u.unmarshal(fileInputStream);
	} catch (Exception e) { e.printStackTrace(); }
        marshal();
    }  

    public javax.xml.bind.Element unmarshalAny(String bindings) {
        JAXBContext oldCtx = jaxbCtx;
        Unmarshaller u;
        try {
            jaxbCtx = JAXBContext.newInstance(bindings);
            u = jaxbCtx.createUnmarshaller();
            jaxbCtx = oldCtx;
            return (javax.xml.bind.Element) u.unmarshal(fileInputStream);
        } catch (Exception e) {
            jaxbCtx = oldCtx;
            return (javax.xml.bind.Element) null;
        }
        
    }
    
    public void marshal() {
        Marshaller m;
        try {
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                new Boolean(true));
            m.marshal(this.root,System.out);

        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public String marshalToString(Object jaxb) {
        Marshaller m;
        String s;
        if ( jaxb == null ) {
            return "<Empty/>";
        }
        if (jaxb instanceof ResultsType) {
            s = "<Result/>";
        } else {
            s = "<Errors/>";
        }
        try {
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, 
                    new Boolean(true));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            m.marshal(jaxb,pw);
            s = sw.toString();
        } catch (Exception e) { e.printStackTrace(); }
        return s;
    }
    
    public void marshal(File f) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);
            marshal((javax.xml.bind.Element)root, (OutputStream)fos);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void marshal(javax.xml.bind.Element node, java.io.OutputStream o) {
        Marshaller m;
        try {
            m = jaxbCtx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m.marshal(node,o);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    
    public String translate() {

	StringBuffer source = new StringBuffer();
	StringWriter head = new StringWriter();
	StringWriter tail = new StringWriter();
	StringWriter entities = new StringWriter();
	StringWriter listeners = new StringWriter();
	StringWriter connectors = new StringWriter();
	StringWriter output = new StringWriter();

	buildHead(head);
	buildEntities(entities);
	buildListeners(listeners);
	buildConnectors(connectors);
	buildOutput(output);
	buildTail(tail);

	buildSource(source, head, entities, listeners, connectors, output, tail);

	return source.toString();
    }

    void buildHead(StringWriter head) {

	PrintWriter pw = new PrintWriter(head);
	String name = this.root.getName();
	String pkg  = this.root.getPackage();
        String extend = this.root.getExtend();
	
	pw.println("package " + pkg + sc);
	pw.println();
	pw.println("import simkit.*;");
	pw.println("import simkit.random.*;");
	pw.println("import simkit.stat.*;");
	pw.println("import simkit.util.*;");
	pw.println("import java.text.*;");
	pw.println();
        if ( extend.equals("java.lang.Object") ) {
            extend = "";
        } else {
            extend = "extends" + sp + extend + sp;
        }
	pw.println("public class " + name + sp + extend + ob);
	pw.println();
	pw.println();

    }

    void buildEntities(StringWriter entities) {
	PrintWriter pw = new PrintWriter(entities);
	ListIterator seli = this.root.getSimEntity().listIterator();

	while ( seli.hasNext() ) {

	    SimEntity se = (SimEntity) seli.next();
	    List pl = se.getParameters();
	    ListIterator pli = pl.listIterator();
	    
	    pw.print(sp4 + "public" + sp + se.getType() + sp + se.getName() + sp + eq);
	    pw.print(sp + nw + sp + se.getType() + lp);

	    if ( pli.hasNext() ) {
		pw.println();
	        while ( pli.hasNext() ) {
		    doParameter(pl, pli.next(), sp12, pw);
	        }
		pw.println();
	        pw.println(sp8 + rp + sc);
	    } else pw.println(rp + sc);

	    pw.println();

	} 

	pw.println();

    }

     /* Build up a parameter up to but not including a trailing comma. 
      * _callers_ should check the size of the list to determine if a 
      * comma is needed. This may include a closing paren or brace
      * and any nesting. Note a a doParameter may also be a caller
      * of a doParameter, so the comma placement is tricky.
      */
 
    void doParameter(List plist, Object param, String indent, PrintWriter pw) {

	if ( param instanceof MultiParameterType ) {
	    doMultiParameter((MultiParameterType)param, indent, pw);
	} else if ( param instanceof FactoryParameterType ) {
	    doFactoryParameter((FactoryParameterType)param, indent, pw);
	} else { 
	    doTerminalParameter((TerminalParameterType)param, indent, pw);
	}

	maybeComma(plist, param, pw);
    }

    void doFactoryParameter(FactoryParameterType fact, String indent, PrintWriter pw) {
	String factory = fact.getFactory();
        String method = fact.getMethod();
	List facts = fact.getParameters();
	ListIterator facti = facts.listIterator();
	pw.println(indent + sp4 + factory + pd + method + lp);
	while ( facti.hasNext() ) {
	    doParameter(facts, facti.next(), indent + sp8, pw);
	}
	pw.print(indent + sp4 + rp);
    }

    void doTerminalParameter(TerminalParameterType term, String indent, PrintWriter pw) {

	String type = term.getType();
	String value = term.getValue();
        if ( term.getNameRef() != null ) {
            value=((TerminalParameterType)(term.getNameRef())).getValue();
        }
        if ( isPrimitive(type) ) {
	    pw.print(indent + sp4 + value); 
	} else if ( isString(type) ) {
	    pw.print(indent + sp4 + qu + value + qu);
	} else { // some Expression
	    //pw.print(indent + sp4 + nw + sp + type + lp);
	    //pw.print(value + rp);
            pw.print(indent + value);
	}

    }

    void doSimpleStringParameter(TerminalParameterType term, PrintWriter pw) {
	
	String type = term.getType();
	String value = term.getValue();

	if ( isString(type) ) {
	    pw.print(qu + value + qu);
	} else {
	    error("Should only have a single String parameter for this PropertyChangeListener");
	}

    }

    boolean isPrimitive(String type) {
	if (
	    type.equals("boolean") |
	    type.equals("char") |
	    type.equals("double") |
	    type.equals("float") |
	    type.equals("int") |
	    type.equals("long") |
	    type.equals("short") 
	) return true;
	else return false;
    }

    boolean isString(String type) {
	if ( 
	    type.equals("String") | 
	    type.equals("java.lang.String") 
	) return true;
	else return false;
    }

    boolean isArray(String type) {
	if ( 
	    type.endsWith("]")
	) return true;
	else return false;
    }

    void doMultiParameter(MultiParameterType p, String indent, PrintWriter pw) {

	List params = p.getParameters();
	ListIterator paramsi = params.listIterator();
        String ptype = p.getType();

	if ( isArray(ptype) ) {
	    pw.println(indent + sp4 + nw + sp + ptype + ob);
	    while ( paramsi.hasNext() ) {
		doParameter(params, paramsi.next(), indent + sp4, pw);
	    }
	    pw.print(indent + sp4 + cb);
	} else { // some multi param object
	    pw.println(indent + sp4 + nw + sp + ptype + lp);
	    while ( paramsi.hasNext() ) {
		doParameter(params, paramsi.next(), indent + sp4, pw);
	    }
	    pw.print(indent + sp4 + rp);
	}

    }

    void maybeComma(List params, Object param, PrintWriter pw) {
	if ( params.size() > 1 && params.indexOf(param) < params.size() - 1 ) {
	    pw.println(cm);
	} else pw.println();
    }

    void buildListeners(StringWriter listeners) {
	
	PrintWriter pw = new PrintWriter(listeners);
        
        
        
	ListIterator li = this.root.getPropertyChangeListener().listIterator();

	while ( li.hasNext() ) {
	    PropertyChangeListenerType pcl = (PropertyChangeListenerType)li.next();
	    List tparam = pcl.getParameters(); 
	    ListIterator tparami = tparam.listIterator();
            
            
	    pw.println(sp4 + "java.beans.PropertyChangeListener" + sp + pcl.getName() + sp + eq + sp);
	    pw.print(sp8 + nw + sp + pcl.getType() + lp);
	    if ( tparami.hasNext() ) {
	        while ( tparami.hasNext() ) {
		    // note, check if PropertyChangeListeners can have more than one
		    // String param, if so, do a maybeComma() 
		    // see bug #36, this should now check all other parameter types
		    doSimpleStringParameter((TerminalParameter)tparami.next(), pw);
	        }
	    } 
	    pw.println(rp + sc);
	    pw.println();
	} 

	li = this.root.getAdapter().listIterator();
        
        while ( li.hasNext() ) {
            AdapterType a = (AdapterType)li.next();
            String n = a.getName();
            pw.print(sp4 + "simkit.Bridge" + sp + n + sp + eq + sp + nw + sp + "simkit.Bridge");
            pw.println(lp + qu + a.getEventHeard() + qu + cm + sp + qu + a.getEventSent() + qu + rp + sc);
            pw.println();
        }
        
    }

    void buildConnectors(StringWriter connectors) {
	
	PrintWriter pw = new PrintWriter(connectors);
        
        // listeners get attached upon instantiation
        
        pw.println(sp4 + "public" + sp + this.root.getName() + lp + rp + sp + ob);
        
	ListIterator connects = this.root.getSimEventListenerConnection().listIterator();
	
	while ( connects.hasNext() ) {
	    SimEventListenerConnectionType simcon = (SimEventListenerConnectionType)connects.next();
	    pw.print(sp8 + ((SimEntityType)simcon.getSource()).getName() + pd + "addSimEventListener" );
	    pw.println(lp + ((SimEntityType)simcon.getListener()).getName() + rp + sc); 
	}
        
	pw.println();

	connects = this.root.getPropertyChangeListenerConnection().listIterator();

	while ( connects.hasNext() ) {
	    PropertyChangeListenerConnectionType pccon = (PropertyChangeListenerConnectionType)connects.next();
	    pw.print(sp8 + ((SimEntityType)pccon.getSource()).getName());
	    pw.print(pd + "addPropertyChangeListener" + lp);
	    if ( pccon.getProperty() != null ) {
		pw.print(qu + pccon.getProperty() + qu + cm);
	    } 
	    Object listener = pccon.getListener();
	    if ( listener instanceof SimEntity ) {
	        pw.println(((SimEntityType)(pccon.getListener())).getName() + rp + sc);
	    } else {
	        pw.println(sp + ((PropertyChangeListenerType)(pccon.getListener())).getName() + rp + sc);
	    }
	}            
        pw.println();
        connects = this.root.getAdapter().listIterator();
        while (connects.hasNext()) {
            AdapterType a = (AdapterType) connects.next();
            pw.print(sp8 + ((SimEntityType)a.getFrom()).getName() + pd + "addSimEventListener" + lp);
            pw.println(a.getName() + rp + sc);
            pw.print(sp8 + a.getName() + pd + "addSimEventListener" + lp);
            pw.println(((SimEntityType)a.getTo()).getName() + rp + sc);
            pw.println();    
        }

        pw.println(sp4 + cb);
	pw.println();
    }

    void buildOutput(StringWriter out) {
	PrintWriter pw = new PrintWriter(out);	
        
        pw.println(sp4 + "public static void main(String[] args) {");
        pw.print(sp8 + this.root.getName() + sp + "asm" + sp);
        pw.println(eq + sp + nw + sp + this.root.getName() + lp + rp + sc);

	ListIterator outputs = this.root.getOutput().listIterator();
	while ( outputs.hasNext() ) {
            Object elem = ((OutputType)outputs.next()).getEntity();
            String name = "<FIX: Output not of SimEntity or PropertyChangeListener>";
            
            if ( elem instanceof SimEntityType ) {
                name = ((SimEntityType)elem).getName();
            } else if ( elem instanceof PropertyChangeListenerType ) {
                name = ((PropertyChangeListenerType)elem).getName();
            }
	    pw.println(sp8 + "System.out.println" + lp + "asm" + pd + name + rp + sc);
	}
    }

    void buildTail(StringWriter t) {

	PrintWriter pw = new PrintWriter(t);
	ScheduleType schedule;

	if ( (schedule = this.root.getSchedule()) != null ) {
	    pw.print(sp8 + "Schedule" + pd + "stopAtTime");
	    pw.println(lp + schedule.getStopTime() + rp + sc);
	
	    pw.print(sp8 + "Schedule" + pd + "setVerbose");
	    pw.println(lp + schedule.getVerbose() + rp + sc);

	    pw.println(sp8 + "Schedule" + pd + "reset" + lp + rp + sc);
	    pw.println(sp8 + "Schedule" + pd + "startSimulation" + lp + rp + sc);

	}
        
        List pclList = this.root.getPropertyChangeListener();
        Iterator it = pclList.iterator();
            
        while ( it.hasNext() ) {
            String name;
            PropertyChangeListenerType pcl = (PropertyChangeListenerType)(it.next());
            pw.println(sp8 + "System.out.println"+lp+"asm"+pd+pcl.getName()+pd+"toString"+lp+rp+rp+sc);
        }
        
	pw.println();
	pw.println(sp4 + cb);
	pw.println(cb);
    }

    void buildSource(StringBuffer source, StringWriter head, StringWriter entities, 
		StringWriter listeners, StringWriter connectors, StringWriter output, 
		StringWriter tail ) {
   	 
	source.append(head.getBuffer()).append(entities.getBuffer()).append(listeners.getBuffer());
	source.append(connectors.getBuffer()).append(output.getBuffer()).append(tail.getBuffer());
    }

    public void writeOut(String data, java.io.PrintStream out) {
	out.println(data);	
    }

    private String baseNameOf( String s ) {
        return s.substring(0,s.indexOf(pd));
    }

    boolean compileCode(String fileName) {
	String path = this.root.getPackage();
	File fDest;
	char[] pchars;
	int j;
	pchars = path.toCharArray();
	for (j = 0; j<pchars.length; j++) {
	    if ( pchars[j] == '.' ) pchars[j] = File.separatorChar;
	}
	path = new String(pchars);
	try {
	    File f = new File(pd + File.separator + path);
	    f.mkdirs();
	    fDest = new File(path + File.separator + fileName);
	    f = new File(fileName);
	    f.renameTo(fDest);
	} catch (Exception e) { e.printStackTrace(); }	
        return (
            com.sun.tools.javac.Main.compile(
                 new String[] {"-verbose","-sourcepath",path,"-d",pd,path+File.separator+fileName}
            ) == 0
        );
    }

    void runIt() {
	try {
	    File f = new File(pd);
	    ClassLoader cl = new URLClassLoader(new URL[] {f.toURL()});
	    Class assembly = cl.loadClass(this.root.getPackage()+pd+fileBaseName);
	    Object params[] = { new String[]{} };
            Class classParams[] = { params[0].getClass() };
            Method mainMethod = assembly.getDeclaredMethod("main", classParams);
            mainMethod.invoke(null, params);
	} catch (Exception e) { error(e.toString()); }
    }

    void error(String desc) {

	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);

	pw.println("error :");
	pw.println(desc);

	System.err.println(sw.toString());
	System.exit(1);
    }

    /**
     * @param args the command line arguments
     * args[0] - -f | --file | -p | --port
     * args[1] - filename | port
     * args[2] - -p | --port | -f | --file
     * args[3] - port | filename
     */

    public static void main(String[] arg) {
        int port = 0;
        String fileName = null;
        SimkitAssemblyXML2Java sax2j = null;
        List args = java.util.Arrays.asList(arg);
        Iterator it = args.iterator();
        
        while (it.hasNext()) {
            String a = (String)(it.next());
            
            if (a.equals("-p") || a.equals("--port")) {
                if ( it.hasNext() ) {
                    a = (String)(it.next());
                    port = Integer.parseInt(a);
                } else {
                    usage();
                }
            } else if (a.equals("-f") || a.equals("--file")) {
                if ( it.hasNext() ) {
                    fileName = (String)(it.next());
                } else {
                    usage();
                }
            }
        }
        
        if ( port == 0 ) {
            if ( fileName == null ) {
                usage();
            } else {
                sax2j = new SimkitAssemblyXML2Java(fileName); // regular style
                sax2j.unmarshal();
                sax2j.tasker.start();
            }
            
        } else {
            if ( fileName == null ) {
                sax2j = new SimkitAssemblyXML2Java(port); // start server
            } else {
                sax2j = new SimkitAssemblyXML2Java(port,fileName); // in grid mode
                sax2j.tasker.start();
            }
        }
        
    }
    
    static void usage() {
        System.out.println("Check args, you need at least a port or a file in grid mode");
        System.out.println("usage: Assembly [-p port | --port port | -f file | --file file]");
        System.exit(1);
    }
    
    public void doGridTask(String frontHost, int taskID, int lastTask, int jobID) {
        
        unmarshal();
        
        ExperimentType exp = root.getExperiment();
        int runsPerDesignPt = getRunsPerDesignPoint();
        List designPoints = exp.getDesignPoint();
        int designPtsSize = designPoints.size(); // aka getCount() on local side
        int designPtIndex = (taskID-1)/runsPerDesignPt;
        int runIndex = (taskID-1)%runsPerDesignPt;
        
        DesignPointType designPoint = (DesignPointType)(designPoints.get(designPtIndex));
        List designParams = designPoint.getTerminalParameter();
        List params = root.getDesignParameters();
        Iterator itd = designParams.iterator();
        Iterator itp = params.iterator();
        
        System.out.println(fileBaseName+" Grid Task ID "+taskID+" of "+lastTask+" tasks in jobID "+jobID+" which is Run "+ runIndex + " in DesignPoint "+designPtIndex);
        exp.setBatchID(fileBaseName+" Grid Task ID "+taskID+" of "+lastTask+" tasks in jobID "+jobID+" which is Run "+ runIndex + " in DesignPoint "+designPtIndex);
        
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
            
            System.setErr(err);
            System.setOut(log);
            
            bsh.Interpreter bsh = new bsh.Interpreter();
            bsh.setClassLoader(SimkitAssemblyXML2Java.class.getClassLoader());
            
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

	    System.out.println("Evaluating generated java Simulation "+ root.getName() + ":");
	    System.out.println(translate());
            bsh.eval(translate());
            //bsh.eval("sim = new "+ root.getName() +"();");
            //bsh.eval("sim.main(new String[0])");
            
            bsh.eval(root.getName()+".main(new String[0]);");
            
            System.setOut(new PrintStream(oldOut));
            System.setErr(new PrintStream(oldErr));
            
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
                
                out.println("<Results index="+qu+(taskID-1)+qu+" job="+qu+jobID+qu+" design="+qu+designPtIndex+qu+" run="+qu+runIndex+qu+">");
                while( (line = br.readLine()) != null ) {
                    if (line.indexOf("<PropertyChange") !=0) {
                        logs.add(line);
                        
                    } else {
                        propertyChanges.add(line);
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
                parms.add(new String(sw.toString()));
                xmlrpc.execute("experiment.addResult", parms);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        } catch (bsh.EvalError ee) {
            ee.printStackTrace();
        } 
    }
    
    public void doLocalTask() {
        boolean batch;
        
        java.util.List params = root.getDesignParameters();
        batch = !params.isEmpty();
        
        if ( batch ) {
            
            if (root.getExperiment() == null) { // in test mode
                
                try {
                    ObjectFactory of = new ObjectFactory();
                    ExperimentType e = of.createExperiment();
                    e.setBatchID(fileBaseName+": "+(new java.util.Date()).toString());
                    e.setType("full-factorial");
                    root.setExperiment(e);
                    doFullFactorial();
                } catch (javax.xml.bind.JAXBException jaxe) { jaxe.printStackTrace(); }

            } else { // take a Script or use built ins
                ExperimentType e = root.getExperiment();
                e.setBatchID(fileBaseName+": "+(new java.util.Date()).toString());
                String expType = e.getType();
                bsh.Interpreter bsh = new bsh.Interpreter();
                
                if (expType.equals("full-factorial")) {
                    doFullFactorial();
                } else if (expType.equals("latin-hypercube")) {
                    doLatinHypercube();
                }

                //bsh.eval(root.getExperiment().getScript());
                
            }
            
            String experimentsFileName = fileBaseName + ".exp";
            System.out.println("Creating experiments: "+experimentsFileName);
            marshal(new File(experimentsFileName));
            
            int totalRuns = getCount()*getRunsPerDesignPoint();
            try {
                Runtime.getRuntime().exec( new String[] {"qsub","-t","1-"+totalRuns,"-S","/bin/bash","./gridrun.sh",experimentsFileName});
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }

        } else {
            System.out.println("Generating Java Source...");

            marshal();
            String dotJava = translate();
        
            writeOut(dotJava,System.out);

            System.out.println("Done.");
            System.out.println("Generating Java Bytecode...");

            try {
                String fileName = fileBaseName + ".java";
                FileOutputStream fout =
                    new FileOutputStream(fileName);
                PrintStream ps = new PrintStream(fout,true);
                writeOut(dotJava,ps);
                if ( !compileCode(fileName) )
                    error("Compile error " + fileName);
                else {
                    System.out.println("Done.");

                    System.out.println("Running Assembly " + fileBaseName + "...");
                    System.out.println();

                    runIt();
                }
            } catch (FileNotFoundException fnfe) {
                error("Bad filename " + fileBaseName);
            }
        }

    }
    
    
    public void doFullFactorial() {
        try { 
            List params = root.getDesignParameters();
            ObjectFactory of = new ObjectFactory();
            root.setExperiment(of.createExperiment());
            root.getExperiment().setBatchID(fileBaseName+": "+(new java.util.Date()).toString());
            java.util.HashMap values = new java.util.HashMap();
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
        } catch (javax.xml.bind.JAXBException jaxe) { jaxe.printStackTrace(); }

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
                
                ObjectFactory of = new ObjectFactory();
                try {
                    ExperimentType experiment = root.getExperiment();
                    List designPoints = experiment.getDesignPoint();
                    DesignPointType designPoint = of.createDesignPoint();
                    List terminalParams = designPoint.getTerminalParameter();
                    
                    for (int j = 0; j<terms.length; j++) {
                        TerminalParameterType termCopy = of.createTerminalParameter();
                        termCopy.setValue(((TerminalParameterType)terms[j]).getValue());
                        termCopy.setType(((TerminalParameterType)terms[j]).getType());
                        terminalParams.add(termCopy);
                    }
                    
                    designPoints.add(designPoint);
                    incrementCount();
                
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
     * even so, there can be a large number of Latin squares, so in the case of
     * a large number of variates, it may not be essential to select more than
     * one sample set from each Latin square.
     *
     */
    
    public void doLatinHypercube() {
        ExperimentType experiment = root.getExperiment();
        int runs = getRunsPerDesignPoint();
        String initScript = experiment.getScript();
        bsh.Interpreter bsh = new bsh.Interpreter();
        
        int totalSamples = Integer.parseInt(root.getExperiment().getTotalSamples());
        int size = root.getDesignParameters().size();
        int runsPerDesignPt = getRunsPerDesignPoint();
        LatinPermutator latinSquares = new LatinPermutator(size);
        List designParams = root.getDesignParameters();
        List designPoints = root.getExperiment().getDesignPoint();
        ObjectFactory of = new ObjectFactory();
        HashMap values = new java.util.HashMap();
        MersenneTwister rnd = new MersenneTwister();
        count = 0;
        
        for ( int i = 0; i < totalSamples; i++ ) {
            
            int[][] latinSquare = latinSquares.getRandomLatinSquare();
            int[] row;
            
            for ( int j = 0 ; j < latinSquare.length; j++) { // .length == size
                try {
                    DesignPointType designPt = of.createDesignPoint();
                    Iterator it = designParams.iterator();
                    row = latinSquare[j];
                    int ct = 0;
                    
                    while ( it.hasNext() ) {
                        
                        TerminalParameterType tp = of.createTerminalParameter();
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
                            
                            
                        } else if (range[0] instanceof Integer) { // or accept Integer[size]
                            
                            tp.setValue(range[row[ct]].toString());
                            tp.setType(dp.getType());
                            
                        }
                        
                        designPt.getTerminalParameter().add(tp);
                        ct++; //
                    }
                    
                    List runList = designPt.getRun();
                    for ( int ri = 0; ri < runsPerDesignPt ; ri++ ) {
                        RunType r = of.createRun();
                        r.setIndex(""+ri);
                        runList.add(r);
                    }
                    
                    designPt.setIndex(""+getCount());
                    designPoints.add(designPt);
                    incrementCount();
                    
                } catch (javax.xml.bind.JAXBException jaxbe) { jaxbe.printStackTrace(); }
            }
        }
        
    }
    
    /* count of design points */
    private void incrementCount() {
        ++this.count;
    }
    
    private int getCount() {
        return count;
    }
    
    private int getRunsPerDesignPoint() {
        return Integer.parseInt(root.getExperiment().getRunsPerDesignPoint());
    }
    
    /* results from each run */
    /* note this would only be called when getCount() has already been tallied */
    private void incrementTotalResults() {
        ++this.totalResults;
        System.out.println(totalResults+" of "+getCount() * getRunsPerDesignPoint());
    }
    
    private int getTotalResults() {
        return totalResults;
    }
    
    // LatinPermutator -
    // any swap of two rows or two columns in a LHS is a LHS
    // start with a base LHS, where
    // A(i,j) = [ i + (N-j) % N ] % N
    //
    // which is also a table of addition, eg,
    // ( i + j ) = A(i,j) % N
    
    // can be shown that any permution of 1..N-1 used as
    // i' = I(p(i)) and j' = J(q(j)) in A is Latin.
    
    // hence we can iterate through all I and J permutations rather quickly
    // and generate all Latins, or any random one almost instantly.
    // no memory matrix needs to be created, just indexes into virtual
    // rows and cols.
    
    class LatinPermutator {
        MersenneTwister rnd;
        ArrayList set;
        int size;
        int[] row;
        int[] col;
        int rc,cc;
        int ct;
        
        //for testing stand-alone
        //public static void main(String[] args) {
            //LatinPermutator lp = new LatinPermutator(Integer.parseInt(args[0]));
            //output size number of randoms
            //System.out.println("Output "+lp.size+" random LHS");
            //for ( int j = 0; j < 10*lp.size; j++ ) {
                //java.util.Date d = new java.util.Date();
                //long time = d.getTime();
                //lp.randomSquare();
                //d = new java.util.Date();
                //time -= d.getTime();
                //System.out.println("Random Square:");
                //lp.output();
                //System.out.println("milliseconds : "+-1*time);
                //System.out.println("---------------------------------------------");
            //}
            
            //output series starting at base
            //System.out.println("---------------------------------------------");
            //System.out.println("Output bubbled LHS");
            //lp.ct=0;
            //bubbles not perfect, hits some squares more than once, not all squares
            //possible with only single base
            //lp.bubbles();
            
        //}
        
        public LatinPermutator(int size) {
            rnd = new MersenneTwister();
            this.size=size;
            row = new int[size];
            col = new int[size];
            rc = cc = size-1;
            ct=0;
        }
        
        int getAsubIJ(int i, int j) {
            return
                    (i + ((size - j)%size))%size;
        }
        
        /*
        // not really used except for test as per main()
        void bubbles() {
            int i;
            for ( i = 0; i < size; i++ ) {
                row[i] = col[i] = i;
            }
            output();
            i = size;
            while ( i-- > 0 )
                while (bubbleRow()) {
                output();
                while (bubbleCol()) {
                    output();
                }
                }
            
        }
        
        // not really used except for test as per bubbles() in main()
        boolean bubbleRow() {
            int t;
            if ( rc < 1 ) {
                rc = size-1;
                return false;
            }
            t = row[rc];
            row[rc] = row[rc-1];
            row[rc-1] = t;
            rc--;
            return true;
        }
        
        // not really used except for test as per bubbles() in main()
        boolean bubbleCol() {
            int t;
            if ( cc < 1 ) {
                cc = size-1;
                return false;
            }
            t = col[cc];
            col[cc] = col[cc-1];
            col[cc-1] = t;
            cc--;
            return true;
        }
        */
        void output() {
            //System.out.println("Row index: ");
            ////for ( int i = 0;  i < size; i++ ) {
            //System.out.print(row[i]+" ");
            //}
            //System.out.println();
            //System.out.println();
            //System.out.println("Col index: ");
            //for ( int i = 0;  i < size; i++ ) {
            //System.out.print(col[i]+" ");
            //}
            //System.out.println();
            //System.out.println();
            System.out.println();
            System.out.println("Square "+(ct++)+": ");
            for ( int i = 0;  i < size; i++ ) {
                System.out.println();
                for ( int j = 0; j < size; j++ ) {
                    System.out.print(getAsubIJ(row[i],col[j])+" ");
                }
            }
            System.out.println();
        }
        
        void randomSquare() {
            int totSize = size;
            ArrayList r = new ArrayList();
            ArrayList c = new ArrayList();
            
            for ( int i = 0; i < size; i ++) {
                r.add(new Integer(i));
                c.add(new Integer(i));
            }
            
            for ( int i = 0; i < size; i ++) {
                row[i] = ((Integer)(r.remove((int)((double)r.size()*rnd.draw())))).intValue();
                col[i] = ((Integer)(c.remove((int)((double)c.size()*rnd.draw())))).intValue();
            }

        }
        
        int[][] getRandomLatinSquare() {
            int[][] square = new int[size][size];
            randomSquare();
            for ( int i = 0;  i < size; i++ ) {
                for ( int j = 0; j < size; j++ ) {
                    square[i][j]=getAsubIJ(row[i],col[j]);
                }
            }
            
            output();
            return square;
        }
        
        void setSeed(long seed) {
            rnd.setSeed(seed);
        }
        
    }
    
    /**
     * XML-RPC service for Gridkit
     */
    
    class AssemblyServer extends WebServer {
        
        SimkitAssemblyXML2Java inst;
        
        AssemblyServer(SimkitAssemblyXML2Java inst, int port) {
            super(port);
            this.inst=inst;
            addHandler("experiment",inst);
            
        }
    }
    
    /**
     * Implement the XmlRpcHandler interface directly to manually specify available
     * methods.
     */
    
    public Object execute(String methodName, Vector parameters) throws java.lang.Exception {
        Object ret;
        String call = new String(methodName);
        String xmlData = new String("empty");
        System.out.println("Execute for "+call+sp+getTotalResults());
        
        if (call.equals("experiment.setAssembly")) {
            xmlData=(String) parameters.elementAt(0);
            ret = setAssembly(xmlData);
        } else if (call.equals("experiment.addResult") ||
                call.equals("experiment.addReport")) {
            xmlData= (String) parameters.elementAt(0);
            ret = addReport(xmlData);
        } else if (call.equals("experiment.getResult")) {
            Integer designPt = (Integer) parameters.elementAt(0);
            Integer run = (Integer) parameters.elementAt(1);
            ret = getResult(designPt.intValue(),run.intValue());
        } else if (call.equals("experiment.flushQueue")) {
            ret = flushQueue();
        } else if (call.equals("experiment.getRemainingJobs")) {
            ret = getRemainingJobs();
        } else if (call.equals("experiment.clear")) {
            ret = clear();
        } else if (call.equals("experiment.removeTask")) {
            Integer designPt = (Integer) parameters.elementAt(0);
            Integer run = (Integer) parameters.elementAt(1);
            ret = removeTask(designPt.intValue(),run.intValue());
        } else { 
            throw new Exception("No such method \""+methodName+"\"! ");
        }
        return ret;
    }
    
    /**
     * hook for experiment.setAssembly XML-RPC call, used to initialize
     * From DOE panel. Accepts raw XML String of Assembly.
     */
    
    public Boolean setAssembly(String assembly) {
        Unmarshaller u;
        
        System.out.println("Setting assembly");
        if (busy) {
            flushQueue();
        }
        
        System.out.println(assembly);
        busy = true; // needed now with all the other methods??
        fileInputStream = new ByteArrayInputStream(assembly.getBytes());
        try {
            u = jaxbCtx.createUnmarshaller();
            this.root = (SimkitAssemblyType) u.unmarshal(fileInputStream);
        } catch (Exception e) { e.printStackTrace(); busy=false; }
        
        fileBaseName = root.getName();
        System.out.println("got fileBaseName of "+fileBaseName);
        tasker = new GridTaskGetter(this);
        tasker.start();
        
        
        // clear results count
        totalResults = 0;
        
        return new Boolean(busy);
    }
    
    /**
     * hook for experiment.addReport XML-RPC call, used to report
     * back results from grid node run. Accepts raw XML String of Report.
     */
    
    public Boolean addReport(String report) {
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
                List runList = designPoint.getRun();
                RunType run = (RunType)runList.get(Integer.parseInt(r.getRun()));
                run.setResults(r);
                synchronized(run) {
                    run.notify();
                }
            }

            incrementTotalResults();
            
            //check if done, then write out complete file.
            //unlock the setAssembly method to accept further
            //experiments.
            
            if ( getTotalResults() == getCount() * getRunsPerDesignPoint()) {
                marshal(new File(root.getName()+".exp"));
                busy = false;
            }
            
        } catch (Exception e) { error = true; e.printStackTrace(); }
        
        return new Boolean(busy);
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
        RunType runner = (RunType)(((DesignPointType)(root.getExperiment().getDesignPoint().get(designPt))).getRun().get(run));
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
                ObjectFactory of = new ObjectFactory();
                r = (ResultsType)(of.createResults());
                r.setDesign(""+designPt);
                r.setRun(""+run);
               
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return marshalToString(r);
    }
    
    public Integer removeTask(int designPt, int run) {
        int taskID = designPt * Integer.parseInt(root.getExperiment().getRunsPerDesignPoint());
        taskID += run;
        taskID += 1;
        try {
            Runtime.getRuntime().exec( new String[] {"qdel",""+taskID} ) ;
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
        
        try {
            ObjectFactory of = new ObjectFactory();
            ResultsType r = (ResultsType)(of.createResults());
            r.setDesign(""+designPt);
            r.setRun(""+run);
            // release Results lock on thread
            addReport(marshalToString(r)); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Integer(taskID);
    }

    /** 
     * XML-RPC handler for clearing the grid queue, 
     * @returns number of remaining jobs still in the queue
     * that will be terminated.
     */
    public Integer flushQueue() {
        Integer remainingJobs = new Integer(( getCount() * getRunsPerDesignPoint() ) - getTotalResults());
        try {
            Runtime.getRuntime().exec( new String[] {"qdel","all"} ) ;
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
        if (root != null) {
            marshal(new File(root.getName()+".exp"));
        }
        busy = false;
        
        return remainingJobs;
    }
    
    /** 
     * XML-RPC handler for returning number of remaining jobs in queue,
     * could be used to estimate when a set of jobs becomes stuck. 
     * @returns number of remaining jobs in the queue still running.
     */
    
    public Integer getRemainingJobs() {
        return new Integer(( getCount() * getRunsPerDesignPoint() ) - getTotalResults());
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
    
    class GridTaskGetter extends Thread implements Runnable {
        InputStream is;
        java.util.Properties p = System.getProperties();
        SimkitAssemblyXML2Java inst;
        boolean isTask;
        int task;
        int numTasks;
        int jobID;
        int port;
        String frontHost;
        
        public GridTaskGetter(SimkitAssemblyXML2Java instance) {
            inst = instance;
            try {
                Process pr = Runtime.getRuntime().exec("env"); 
                is = pr.getInputStream();
                
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
        }
        
        public void run() {
            try {
                p.load(is);
                
                // note: will probably want to define platform
                // independent properties that map to the platform
                // dependent properties.
                
                if (isTask=p.getProperty("SGE_TASK_ID")!=null) {
                    task=Integer.parseInt(p.getProperty("SGE_TASK_ID"));
                    jobID=Integer.parseInt(p.getProperty("JOB_ID"));
                    numTasks=Integer.parseInt(p.getProperty("SGE_TASK_LAST"));
                    frontHost = p.getProperty("SGE_O_HOST");
                }
                
            } catch (IOException ioe) {
                    ioe.printStackTrace();
            }
            
            if (isTask) {
                inst.doGridTask(frontHost,task,numTasks,jobID);
            } else {
                inst.doLocalTask();
            }
        }

    }
}
