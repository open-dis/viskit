/*
 * SimkitXML2Java.java
 *
 * Created on March 23, 2004, 4:59 PM
 */

package viskit.xsd.translator;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.ListIterator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import viskit.xsd.bindings.*;

/**
 *
 * @author  Rick Goldberg
 *
 */

public class SimkitXML2Java {

    private SimEntity root;

    InputStream fileInputStream;
    String fileBaseName;
    JAXBContext jaxbCtx;

    /* convenience Strings for formatting */

    private final String sp  = " ";
    private final String sp4 = sp+sp+sp+sp;
    private final String sp8 = sp4+sp4;
    private final String sp12= sp8+sp4;
    private final String ob  = "{";
    private final String cb  = "}";
    private final String sc  = ";";
    private final String cm  = ",";
    private final String lp  = "(";
    private final String rp  = ")";
    private final String eq  = "=";
    private final String pd  = ".";
    private final String qu  = "\"";
    private final String lb  = "[";
    private final String rb  = "]";

    
    /** 
     * Creates a new instance of SimkitXML2Java 
     * when used from another class, instance this
     * with a String for the name of the xmlFile
     */

    public SimkitXML2Java(String xmlFile) {
	fileBaseName = baseNameOf(xmlFile);
	try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings");
	    fileInputStream = Class.forName("viskit.xsd.translator.SimkitXML2Java").getClassLoader().getResourceAsStream(xmlFile);
	} catch ( Exception e ) {
	    e.printStackTrace();
	} 
	
    }

    public SimkitXML2Java(File f) throws Exception {
	fileBaseName = baseNameOf(f.getName());
	jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings");
	fileInputStream = new FileInputStream(f);
    }
    
    public void unmarshal() {
	Unmarshaller u;
	try {
	    u = jaxbCtx.createUnmarshaller();
	    this.root = (SimEntity) u.unmarshal(fileInputStream);
	} catch (Exception e) { e.printStackTrace(); }
    }  

    public String translate() {

	StringBuffer source = new StringBuffer();
	StringWriter head = new StringWriter();
	StringWriter tail = new StringWriter();
	StringWriter vars = new StringWriter();
	StringWriter runBlock = new StringWriter();
	StringWriter eventBlock = new StringWriter();
	StringWriter accessorBlock = new StringWriter();

	buildHead(head);
	buildVars(vars, accessorBlock);
	buildEventBlock(runBlock,eventBlock);
	buildTail(tail);

	buildSource(source, head, vars, runBlock, eventBlock, accessorBlock, tail);

	return source.toString();
    }

    void buildHead(StringWriter head) {

	PrintWriter pw = new PrintWriter(head);
	String name = this.root.getName();
        String pkg  = this.root.getPackage();
	
	pw.println("package " + pkg + sc);
	pw.println();
	pw.println("import simkit.*;");
	pw.println("import simkit.random.*;");
	pw.println("import java.util.*;");
	pw.println();
	pw.println("public class " + name + sp + "extends SimEntityBase" + sp + ob);
	pw.println();
    }

    void buildVars(StringWriter vars, StringWriter accessorBlock) {

	PrintWriter pw = new PrintWriter(vars);

	ListIterator li = this.root.getParameter().listIterator();

	while ( li.hasNext() ) {

	    Parameter p = (Parameter) li.next();
	    
	    pw.println(sp4 + "private" + sp + p.getType() + sp + p.getName() + sc);

	    buildParameterAccessor(p,accessorBlock);

	} 
	
	pw.println();

	li = this.root.getStateVariable().listIterator();

	while ( li.hasNext() ) {
	    Class c = null;
		
	    StateVariable s = (StateVariable) li.next();

	    try {
		c = Class.forName(s.getType());
	    } catch ( ClassNotFoundException cnfe ) {
	        pw.println(sp4 + "protected" + sp + s.getType() 
			+ sp + s.getName() + sc);
	    }

	    if ( c != null ) {
		pw.println(sp4 + "protected" + sp + s.getType() + sp 
			+ s.getName() + sp + eq + sp + "new" + sp 
			+ s.getType() + lp + rp + sc ) ;	
	    }

	    buildStateVariableAccessor(s,accessorBlock);
	}

    }

    void buildParameterAccessor(Parameter p, StringWriter sw) {

	PrintWriter pw = new PrintWriter(sw);
	
	pw.print(sp4 + "public void set" + capitalize(p.getName()) + lp);	
	pw.println(p.getType() + sp + shortinate(p.getName()) + rp + sp + ob);
	pw.print(sp8 + p.getName() + sp + eq + sp);
	
	if ( isArray(p.getType()) ) {
	    pw.print(lp + p.getType() + rp + sp + shortinate(p.getName()));
	    pw.println(pd + "clone" + lp + rp + sc);
	} else {
	    pw.println(shortinate(p.getName()) + sc);
	}
	pw.println(sp4 + cb);
	pw.println();	

	// also provide indexed set/getters, may be multidimensional however not expected
	// to actually be multidimensional
	if ( isArray(p.getType()) ) {
	    int d = dims(p.getType());

	    pw.print(sp4 + "public void set" + capitalize(p.getName()) + lp + indx(d));
	    pw.println(baseOf(p.getType()) + sp + shortinate(p.getName()) + rp + sp + ob);
	    pw.println(sp8 + p.getName() + indxbr(d) + sp + eq + sp + shortinate(p.getName()) + sc);
	    pw.println(sp4 + cb);
	    pw.println();


	    pw.print(sp4 + "public" + sp + baseOf(p.getType()) + sp + "get" );
	    pw.print(capitalize(p.getName()) + lp + indxncm(d));
	    pw.println(rp + sp + ob);
	    pw.println(sp8 + "return" + sp + p.getName() + indxbr(d) + sc);
	    pw.println(sp4 + cb);
	    pw.println();
	}
	
	pw.print(sp4 + "public " + p.getType() + sp + "get" + capitalize(p.getName()) );
	pw.println(lp + rp + sp + ob);
	pw.println(sp8 + "return" + sp + p.getName() + sc);
	pw.println(sp4 + cb);
	pw.println();
    }

 
    private int dims(String t) {
	int d = 0;
	int s = 0;

	while ( (s = t.indexOf("[")) > 0 ) {
	    d++;
	    t = t.substring(s + 1);
	}

	return d;
    }

    private String indx(int dims) {
	String inds = "";

	for (int k = 0; k < dims; k++ ) {
	    inds += "int" + sp + "i" + k + cm + sp;
	}

	return inds;
    }

    // trim off trailing comma space
    private String indxncm(int dims) {
	String ind = indx(dims);
	return ind.substring(0, ind.length() - 2);
    }
	    
    // creates [i0][i1]..[ik]
    private String indxbr(int dims) {
	String inds = "";

	for (int k = 0; k < dims; k++ ) {
	    inds += lb + "i" + k + rb;
	}

	return inds;
    }


    void buildStateVariableAccessor(StateVariable s, StringWriter sw) {

        PrintWriter pw = new PrintWriter(sw);
	String clStr = "";
	String tyStr = "";

	// check for cloneable 

	if (isCloneable(s.getType())) {
	    clStr = ".clone()";
	    tyStr = lp + s.getType() + rp;
	}

        pw.print(sp4 + "public " + s.getType() + sp + "get" + capitalize(s.getName()) );
        pw.println(lp + rp + sp + ob);
	pw.println(sp8 + "return" + sp + tyStr + sp + s.getName() + clStr + sc);
	pw.println(sp4 + cb);
	pw.println();

	if ( isArray(s.getType()) ) {
	    int d = dims(s.getType());
	    pw.print(sp4 + "public" + sp + baseOf(s.getType()) + sp + "get" );
	    pw.print(capitalize(s.getName()) + lp + indxncm(d));
	    pw.println(rp + sp + ob);
	    pw.println(sp8 + "return" + sp + s.getName() + indxbr(d) + sc);
	    pw.println(sp4 + cb);
	    pw.println();
	}

        pw.println();
    }

    void buildEventBlock(StringWriter runBlock,StringWriter eventBlock) {
	
	List events = this.root.getEvent();
	ListIterator li = events.listIterator();
	Event e = null;

	while ( li.hasNext() ) {

	    e = (Event) li.next();
	
	    if ( e.getName().equals("Run") ) {
		doRunBlock(e,runBlock);
	    } else {
		doEventBlock(e,eventBlock);
	    }

	}
    }
	
    void doRunBlock(Event run, StringWriter runBlock) {
	
	PrintWriter pw = new PrintWriter(runBlock);
	ListIterator li;
	List sched = run.getScheduleOrCancel();
	ListIterator schi = sched.listIterator();
	
	// w/out a tag, can only assume arrays in parameters
	// are all the same size...
	String firstArray=null;

	pw.println();
	pw.println(sp4 + "/** Creates a new instance of " + this.root.getName() + " */");
	pw.println();
	pw.print(sp4 + "public " + this.root.getName() + lp);

	List pList = this.root.getParameter();
	li = this.root.getParameter().listIterator();

	while ( li.hasNext() ) {

	    Parameter pt = (Parameter) li.next();
		
	    pw.print(pt.getType() + sp + shortinate(pt.getName()));
	
	    if ( pList.size() > 1 ) {
 	        if ( pList.indexOf(pt) <  pList.size() - 1 ) {
	            pw.print(cm);
		    pw.println();
		    pw.print(sp8 + sp4);
	        }
	    }
	}

	pw.println(rp + sp + ob);
	pw.println();

	li = this.root.getParameter().listIterator();

	while ( li.hasNext() ) {
	    Parameter pt = (Parameter) li.next();
	    pw.println(sp8 + "set" + capitalize(pt.getName()) + 
		lp + shortinate(pt.getName()) + rp + sc);
	    if( firstArray == null ) {
		firstArray = (isArray(pt.getType()))?pt.getName():null;
	    }
	}

	// create new arrays, if any
	// note: have to assume that the length of parameter arrays
	// is consistent 

	li = this.root.getStateVariable().listIterator();

	while ( li.hasNext() ) {
	    StateVariable st = (StateVariable) li.next();
	    if (isArray(st.getType())) {
		pw.print(sp8 + st.getName() + sp + eq + sp + "new" + sp + baseOf(st.getType()));
		pw.println(lb + shortinate(firstArray) + pd + "length" + rb + sc);
	    }
	}	
	

	pw.println(sp4 + cb);
	pw.println();
	pw.println(sp4 + "/** Set initial values of all state variables */");
	pw.println(sp4 + "public void reset() {");

	li = run.getLocalVariable().listIterator();

	while ( li.hasNext() ) {
	    LocalVariable local = (LocalVariable) li.next();
	    pw.println(sp8 + local.getType() + sp + local.getName() + sc);
	}
	
	pw.println();
	pw.println(sp8 + "super.reset()" + sc);
	pw.println();
	pw.println(sp8 + "/** StateTransitions for the Run Event */");
	pw.println();

	li = run.getStateTransition().listIterator();
	
	while ( li.hasNext() ) {
	    StateTransition st = (StateTransition) li.next();
	    StateVariable sv = (StateVariable) st.getState();
	    AssignmentType asg = st.getAssignment();
	    OperationType ops = st.getOperation();
	    boolean isar = isArray(sv.getType());
	    String spn = isar ? sp12:sp8;
	    String in = indexFrom(st);
	
	    if ( isar ) {
		pw.print(sp8 + "for (" + in + " = 0; " + in + " < " + sv.getName() + pd + "length");
		pw.println(sc + sp + in + "++" + rp + sp + ob);
	        pw.print(spn + sv.getName() + lb + in + rb);
	    } else {
		pw.print(spn + sv.getName());
	    }

	    if ( asg == null ) { 
		pw.println(pd + ops.getMethod() + sc);
	    } else {
		pw.println(sp + eq + sp + asg.getValue() + sc);
	    }

	    if ( isar ) {
		pw.println(sp8 + cb);
	    }
	}

	pw.println(sp4 + cb);
	pw.println();
	
	pw.println(sp4 + "public void doRun() {");
	
	li = run.getStateTransition().listIterator();

	while( li.hasNext() ) {
	    StateTransition st = (StateTransition) li.next();
	    StateVariable sv = (StateVariable) st.getState();
	    pw.print(sp8 + "firePropertyChange(" + qu + sv.getName() + qu); 
	    pw.println(cm + sv.getName() + rp + sc);
	}

	while ( schi.hasNext() ) {
	    Object o = schi.next();
	    if ( o instanceof ScheduleType ) {
		doSchedule((ScheduleType)o,run,pw);
	    } else {
		doCancel((CancelType)o,run,pw);
	    }
	}
	
	pw.println(sp4 + cb);
	pw.println();
    }

    /** these Events should now not be any Run event */

    void doEventBlock(Event e, StringWriter eventBlock) {
	PrintWriter pw = new PrintWriter(eventBlock);
	ListIterator sli = e.getStateTransition().listIterator();
	List args = e.getArgument();
	ListIterator ai = args.listIterator();
	List locs = e.getLocalVariable();
	ListIterator lci = locs.listIterator();
	List sched = e.getScheduleOrCancel();
	ListIterator schi = sched.listIterator();

	pw.print(sp4 + "public void do" + e.getName() + lp); 

	while ( ai.hasNext() ) {
	    Argument a = (Argument) ai.next();
	    pw.print(a.getType() + sp + a.getName());
	    if ( args.size() > 1 && args.indexOf(a) < args.size() - 1 ) {
		pw.print(cm + sp);
	    }
 	}

	// finish the method decl
	pw.println(rp + sp + ob);

	// local variable decls
	while ( lci.hasNext() ) {
	    LocalVariableType local = (LocalVariableType) lci.next();
	    pw.print(sp8 + local.getType() + sp + local.getName() + sp + eq);
	    pw.print(sp + lp + local.getType() + rp);
	    pw.println(sp + local.getValue() + sc);
	}
	
	if ( locs.size() > 0 ) pw.println();

	while ( sli.hasNext() ) {
   	    StateTransition st = (StateTransition) sli.next();
	    StateVariable sv = (StateVariable) st.getState();
	    AssignmentType asg = st.getAssignment();
	    OperationType ops = st.getOperation(); 

	    pw.print(sp8 + sv.getName());

	    if (st.getIndex() != null) pw.print(lb + indexFrom(st) + rb);

	    if (ops != null) {
		pw.println(pd + ops.getMethod() + sc);
	    } else if (asg != null) {
		pw.println(sp + eq + sp + asg.getValue() + sc);
	    }
	
	    if ( isArray(sv.getType()) ) {
		pw.print(sp8 + "fireIndexedPropertyChange" + lp + indexFrom(st)); 
		pw.println(cm + sp + qu + sv.getName() + qu + cm + sp + sv.getName() + rp + sc);
	    } else {
	        pw.print(sp8 + "firePropertyChange" + lp + qu + sv.getName() + qu + cm + sp);
	        pw.println(sv.getName() + rp + sc);
	    }
	    pw.println();

	}

	// waitDelay/interrupt
	while ( schi.hasNext() ) {
	    Object o = schi.next();
	    if ( o instanceof ScheduleType ) {
		doSchedule((ScheduleType)o,e,pw);
	    } else {
		doCancel((CancelType)o,e,pw);
	    }
	}

	pw.println(sp4 + cb);
	pw.println();
	
    }

    void doSchedule(ScheduleType s, Event e, PrintWriter pw) {
	List edges = s.getEdgeParameter();
	ListIterator ei = edges.listIterator();
	Class c = null;
	String condent = "";
        EventType event = (EventType)s.getEvent();
        List eventArgs = event.getArgument();
        ListIterator eventArgsi = eventArgs.listIterator();
	
	if ( s.getCondition() != null ) {
	    condent = sp4;
	    pw.println(sp8 + "if" + sp + lp + s.getCondition() + rp + sp + ob);
	}	

	pw.print(sp8 + condent + "waitDelay" + lp + qu + ((EventType)s.getEvent()).getName() + qu + cm);
	pw.print(s.getDelay() + cm + "new Object[]" + ob);
	
	while ( ei.hasNext() ) {
	    EdgeParameterType ep = (EdgeParameterType) ei.next();
            ArgumentType arg = (ArgumentType) eventArgsi.next();
	    try {
	        c = Class.forName(arg.getType()); 
	    } catch ( ClassNotFoundException cnfe ) {
		// most likely a primitive type
		String type = arg.getType();
		String constructor = "new" + sp;
		if (type.equals("int")) {
		    constructor+="Integer";
		} else if (type.equals("float")) {
		    constructor+="Float";
		} else if (type.equals("double")) {
		    constructor+="Double";
		} else if (type.equals("long")) {
		    constructor+="Long";
		} else if (type.equals("boolean")) {
		    constructor+="Boolean";
		}
		pw.print(constructor + lp + ep.getValue() + rp);
	    }
	    if (c != null) {
		pw.print(ep.getValue());
	    }
	    
	    if ( edges.size() > 1 && edges.indexOf(ep) < edges.size() - 1 ) {
	        pw.print(cm);
	    }

	}
	pw.println(cb + cm + s.getPriority() + rp + sc);
	
	if ( s.getCondition() != null ) {
	    pw.println(sp8 + cb);
	}
    }

    void doCancel(CancelType c, Event e, PrintWriter pw) {
	List edges = c.getEdgeParameter();
	ListIterator ei = edges.listIterator();
	Class cl = null;
	String condent = "";        
        EventType event = (EventType)c.getEvent();
	List eventArgs = event.getArgument();
        ListIterator eventArgsi = eventArgs.listIterator();

	if ( c.getCondition() != null ) {
	    condent = sp4;
	    pw.println(sp8 + "if" + sp + lp + c.getCondition() + rp + sp + ob);
	}	

	pw.print(sp8 + condent + "interrupt" + lp + qu + event.getName() + qu + cm);
	pw.print("new Object[]" + ob);

	while ( ei.hasNext() ) {
	    EdgeParameterType ep = (EdgeParameterType) ei.next();            
            ArgumentType arg = (ArgumentType) eventArgsi.next();
	    try {
	        cl = Class.forName(arg.getType()); 
	    } catch ( ClassNotFoundException cnfe ) {
		// most likely a primitive type
		String type = arg.getType();
		String constructor = "new" + sp;
		if (type.equals("int")) {
		    constructor+="Integer";
		} else if (type.equals("float")) {
		    constructor+="Float";
		} else if (type.equals("double")) {
		    constructor+="Double";
		} else if (type.equals("long")) {
		    constructor+="Long";
		} else if (type.equals("boolean")) {
		    constructor+="Boolean";
		}
		pw.print(constructor + lp + ep.getValue() + rp);
	    }
	    if (cl != null) {
		pw.print(ep.getValue());
	    }
	    
	    if ( edges.size() > 1 && edges.indexOf(ep) < edges.size() - 1 ) {
	        pw.print(cm);
	    }

	}
	pw.println(cb + rp + sc);
	
	if ( c.getCondition() != null ) {
	    pw.println(sp8 + cb);
	}
	
    }

    void buildTail(StringWriter t) {
	PrintWriter pw = new PrintWriter(t);

	pw.println();
	pw.println(cb);
    }

    void buildSource(StringBuffer source, StringWriter head, StringWriter vars, StringWriter runBlock, 
	StringWriter eventBlock, StringWriter accessorBlock, StringWriter tail ) {
   	 
	source.append(head.getBuffer()).append(vars.getBuffer()).append(runBlock.getBuffer());
	source.append(eventBlock.getBuffer()).append(accessorBlock.getBuffer()).append(tail.getBuffer());
    }

    public void writeOut(String data, java.io.PrintStream out) {
	out.println(data);	
    }

    private String capitalize( String s ) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    boolean compileCode (String fileName) {
	return ( 
	    com.sun.tools.javac.Main.compile(
                 new String[] {fileName}
	    ) == 0
	);
    }

    private String indexFrom(StateTransitionType st) {
	String index = "i"; // fallback guess

	if (st.getIndex() instanceof LocalVariableType)
	    index = ((LocalVariableType)st.getIndex()).getName();
	else if (st.getIndex() instanceof ArgumentType)
	    index = ((Argument)st.getIndex()).getName();
	else if (st.getIndex() instanceof ParameterType)
	    index = ((ParameterType)st.getIndex()).getName();

	return index;
    }

    private String shortinate( String s ) {

	String result = s.substring(0,1);
	char[] ca = s.toCharArray();
	char[] tmp = new char[1];
	
	for ( int i = 1; i < ca.length; i++ ) {
	    if ( Character.isUpperCase(ca[i]) ) {
		tmp[0] = ca[i];
		result += new String(tmp);
	    }
	}

	return result.toLowerCase();
    }

    private String baseOf( String s ) {
	return s.substring(0,s.indexOf(lb));
    }

    private String indexIn( String s ) {
	return s.substring(s.indexOf(lb)+1, s.indexOf(rb)-1);
    }

    private String baseNameOf( String s ) {
	return s.substring(0,s.indexOf(pd));
    }

    private boolean isCloneable( String c ) {

	Class aClass = null;

	try {
	    aClass = Thread.currentThread().getContextClassLoader().loadClass(c);
	} catch ( ClassNotFoundException cnfe ) {;}
	
	if (aClass != null) {
	    return java.lang.Cloneable.class.isAssignableFrom(aClass);
	} else if (isArray(c)) {
	    return true;
	} else
	    return false;
    }

    private boolean isArray( String c ) {
	if ( c.endsWith(rb) ) return true;
	else return false;
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
     * args[0] - XML file to translate
     * follow this pattern to use this class from another,
     * otherwise this can be used stand alone from CLI
     */

    public static void main(String[] args) {

	System.out.println("Generating Java Source...");

	SimkitXML2Java sx2j = new SimkitXML2Java(args[0]);
	sx2j.unmarshal();

	String dotJava = sx2j.translate();
	if (args.length > 1) sx2j.writeOut(dotJava,System.out);

	System.out.println("Done.");
	
	// also write out the .java to a file and compile it
	// to a .class
	System.out.println("Generating Java Bytecode...");
	try {
	    String fileName = sx2j.fileBaseName + ".java";
	    FileOutputStream fout = 
		new FileOutputStream(fileName);
	    PrintStream ps = new PrintStream(fout,true);
	    sx2j.writeOut(dotJava,ps);
	    if ( !sx2j.compileCode(fileName) ) 
		sx2j.error("Compile error " + fileName);
	    else 
		System.out.println("Done.");
	} catch (FileNotFoundException fnfe) { 
		sx2j.error("Bad filename " + sx2j.fileBaseName); 
	}

	
    }
    
}
