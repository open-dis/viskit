/*
 * SimkitXML2Java.java
 *
 * Created on March 23, 2004, 4:59 PM
 */

package viskit.xsd.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import viskit.xsd.bindings.eventgraph.*;

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
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
	    fileInputStream = Class.forName("viskit.xsd.translator.SimkitXML2Java").getClassLoader().getResourceAsStream(xmlFile);
	} catch ( Exception e ) {
	    e.printStackTrace();
	}

    }

    public SimkitXML2Java(InputStream stream) {
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
            fileInputStream = stream;
        } catch ( Exception e ) {
	    e.printStackTrace();
	}
    }

    public SimkitXML2Java(File f) throws Exception {
	fileBaseName = baseNameOf(f.getName());
	jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
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
        String extend = this.root.getExtend();
        String implement = this.root.getImplement();

        if ( implement != null ) {
            extend += sp + implement;
        }

	pw.println("package " + pkg + sc);
	pw.println();
	pw.println("import simkit.*;");
	pw.println("import simkit.random.*;");
	pw.println("import java.util.*;");
	pw.println();
	pw.println("public class " + name + sp + "extends" + sp + extend + sp + ob);
	pw.println();
    }

    void buildVars(StringWriter vars, StringWriter accessorBlock) {

	PrintWriter pw = new PrintWriter(vars);

	ListIterator li = this.root.getParameter().listIterator();
	List superParams = resolveSuperParams(this.root.getParameter());
	boolean extend = (this.root.getExtend().indexOf("SimEntityBase") < 0);

	while ( li.hasNext() ) {

	    Parameter p = (Parameter) li.next();

            if ( !superParams.contains(p) ) {
                pw.println(sp4 + "private" + sp + p.getType() + sp + p.getName() + sc);
            } else {
                pw.println(sp4 + "/* inherited parameter " + p.getType() + sp + p.getName() + " */");
            }

	    if ( !extend )
		buildParameterAccessor(p,accessorBlock);
	    else if ( !superParams.contains(p) )
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
	        pw.println(sp4 + "protected" + sp + stripLength(s.getType())
			+ sp + s.getName() + sc);
	    }

	    if ( c != null ) {
                java.lang.reflect.Constructor cst = null;

                try {
                    cst = c.getDeclaredConstructor(new Class[] {});
                } catch (Exception e) { // no null constructors
                    ;
                }

		if ( cst != null ) pw.println(sp4 + "protected" + sp + s.getType() + sp
			+ s.getName() + sp + eq + sp + "new" + sp
			+ s.getType() + lp + rp + sc ) ;
                else { // really not a bad case, most likely will be set by the reset()
                    pw.println(sp4 + "protected" + sp + s.getType() + sp
                        + s.getName() + sp + eq + sp + "null" + sc
                    );
                }
	    }

	    buildStateVariableAccessor(s,accessorBlock);
	}

    }

    void buildParameterAccessor(Parameter p, StringWriter sw) {

	PrintWriter pw = new PrintWriter(sw);

	pw.print(sp4 + "public void set" + capitalize(p.getName()) + lp);
	pw.println(p.getType() + sp + shortinate(p.getName()) + rp + sp + ob);
	pw.print(sp8 + "this" + pd + p.getName() + sp + eq + sp);

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
	    pw.println(sp8 + "this" + pd + p.getName() + indxbr(d) + sp + eq + sp + shortinate(p.getName()) + sc);
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
	    tyStr = lp + stripLength(s.getType()) + rp;
	}

        pw.print(sp4 + "public " + stripLength(s.getType()) + sp + "get" + capitalize(s.getName()) );
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
        boolean didRun = false;

	while ( li.hasNext() ) {

	    e = (Event) li.next();

	    if ( e.getName().equals("Run") ) {
		doRunBlock(e,runBlock);
                didRun = true;
	    } else {
		doEventBlock(e,eventBlock);
	    }

	}

        if (!didRun) {
            try {
                Event r = (new ObjectFactory()).createEvent();
                r.setName("Run");
                doRunBlock(r,runBlock);
            } catch (javax.xml.bind.JAXBException ex) { ex.printStackTrace(); }
        }

    }

    void doRunBlock(Event run, StringWriter runBlock) {

	PrintWriter pw = new PrintWriter(runBlock);
	ListIterator li;
	List sched = run.getScheduleOrCancel();
	ListIterator schi = sched.listIterator();
	List superPList = new ArrayList();


	pw.println();
	pw.println(sp4 + "/** Creates a new instance of " + this.root.getName() + " */");
	pw.println();
	pw.print(sp4 + "public " + this.root.getName() + lp);

	List pList = this.root.getParameter();
	li = pList.listIterator();

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

        if (this.root.getExtend().indexOf("SimEntityBase") < 0) {
            pList = this.root.getParameter();
	    superPList = resolveSuperParams(pList);
            li = superPList.listIterator();
            pw.print(sp8 + "super" + lp);
            while ( li.hasNext() ) {
                ParameterType pt = (ParameterType) li.next();
                pw.print(shortinate(pt.getName()));
                if ((superPList.size() > 1) && (superPList.indexOf(pt) < superPList.size() - 1)) {
                    pw.print(cm);
                }

            }
            pw.println(rp + sc);

        }
	
        // skip over any sets that would get done in the superclass
        for ( int l = superPList.size(); l < this.root.getParameter().size(); l ++) {
            
	    ParameterType pt = (ParameterType) this.root.getParameter().get(l);
	    pw.println(sp8 + "set" + capitalize(pt.getName()) +
		lp + shortinate(pt.getName()) + rp + sc);

	}

	// create new arrays, if any
	// note: have to assume that the length of parameter arrays
	// is consistent

	li = this.root.getStateVariable().listIterator();

	while ( li.hasNext() ) {
	    StateVariable st = (StateVariable) li.next();
	    if (isArray(st.getType())) {
		pw.println(sp8 + st.getName() + sp + eq + sp + "new" + sp + st.getType() + sc);
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

        if (this.root.getExtend().indexOf("SimEntityBase") < 0) {
            // check if super has a doRun()
            Method doRun = null;
            try {
                Class sup = Class.forName(this.root.getExtend());
                doRun = sup.getDeclaredMethod("doRun", new Class[] { });
            } catch (Exception e) {;}
            if (doRun != null) pw.println(sp8 + "super.doRun();");
        }

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

        String x = "";
        if ( run.getCode() != null ) {
            x = run.getCode();
        }
        pw.println(sp4 + x);
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
            String[] lines = {" "};
            String value = local.getValue();
            if ( !( "".equals(value)) ) {
                lines = value.split("\\;");
            }
	    pw.print(sp8 + local.getType() + sp + local.getName() + sp + eq);
	    pw.println(sp + lp + local.getType() + rp + lines[0].trim() + sc);
            for ( int i = 1; i < lines.length; i++ ) {
                pw.println(sp8 + lines[i].trim() + sc);
            }
	}

	if ( locs.size() > 0 ) pw.println();

        if ( e.getCode() != null ) {
            pw.println(sp8 + "/* Code insertion for Event " + e.getName() + " */");
            String[] lines = e.getCode().split("\\n");
            for ( int i = 0; i < lines.length ; i++ ) {
                pw.println(sp8 + lines[i]);
            }
            pw.println(sp8 + "/* End Code insertion */");
        }

        LinkedList decls = new LinkedList();
	while ( sli.hasNext() ) {
   	    StateTransition st = (StateTransition) sli.next();
	    StateVariable sv = (StateVariable) st.getState();
	    AssignmentType asg = st.getAssignment();
	    OperationType ops = st.getOperation();
            String change = "";
            String olds = ""; // old decl line Bar oldFoo ...
            String oldName = sv.getName(); // oldFoo
            if (ops != null) {
                change = pd + ops.getMethod() + sc;
            } else if ( asg != null ) {
                change = sp + eq + sp + asg.getValue() + sc;
            }
            oldName = "_old_" + oldName.substring(0,1).toUpperCase() + oldName.substring(1);
            if ( !decls.contains(oldName) ) {
                olds = sv.getType();
                decls.add(oldName);
                if ( isArray(olds) ) {
                    String[] baseName;
                    baseName = olds.split("\\[");
                    olds = baseName[0];
                }
                olds += sp;
            }
            // by now, olds is "Bar" ( not Bar[] )
            // or nothing if alreadyDecld
            // now build up "Bar oldFoo = getFoo("
            String getter = oldName + sp + eq + sp + "get" + oldName.substring(5) + lp;
            if ( "".equals(olds) ) {
                olds = getter;
            } else {
                olds += getter;
            }
            // check need _idxvar_from(st)
            if ( isArray(sv.getType())) {
                olds += indexFrom(st);
            }
            olds += rp + sc;
            // now olds is Bar oldFoo = getFoo(<idxvar>?);
            // add this to the pre-formatted block
            olds += sv.getName() + ( isArray(sv.getType()) ? lb + indexFrom(st)+ rb : "" ) + change;
            String[] lines = olds.split("\\;");
            // format it
            for (int i = 0; i < lines.length; i++) {
                if ( i == 0 ) {
                    pw.println(sp8 + "/* StateTransition for " + sv.getName() + " */");
                } else if ( i == 2 ) {
                    pw.println(sp8 + "/* Code block for pre-transition */");
                }
                pw.println(sp8 + lines[i] + sc);
            }
            if ( isArray(sv.getType()) ) {
                pw.print(sp8 + "fireIndexedPropertyChange" + lp + indexFrom(st));
                pw.print(cm + sp + qu + sv.getName() + qu + cm );
                pw.println( oldName + cm + sp + "get" + oldName.substring(5) + lp + indexFrom(st) + rp + rp + sc);
            } else {
                pw.print(sp8 + "firePropertyChange" + lp + qu + sv.getName() + qu + cm + sp);
                pw.println(oldName + cm + sp + "get" + oldName.substring(5) + lp + rp + rp + sc);
            }
            pw.println();
        }
        pw.println();
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
		} else if (type.equals("char")) {
		    constructor+="Character";
		} else if (type.equals("short")) {
		    constructor+="Short";
		} else { // see #93
                    constructor = "";
                    pw.print(ep.getValue());
                } if ( !constructor.equals("") )
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
		} else if (ep.getValue().equals("this")) {
                    constructor = "";
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
        String code = root.getCode();
        if ( code != null ) {
            pw.println(sp4 + "/* Inserted code for " + this.root.getName() + " */");
            String[] lines = code.split("\\n");
            for ( int i = 0 ; i < lines.length ; i ++ ) {
                pw.println(sp4 + lines[i]);
            }
            pw.println(sp4 + "/* End inserted code */");
        }
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

    private String stripLength( String s ) {
        int left, right;
        if ( !isArray(s) ) return s;
        left = s.indexOf(lb);
        right = s.indexOf(rb);
        return s.substring(0,left + 1) + s.substring(right);

    }

    // find the maximal set that the subclass parameters
    // can cover of the superclass's available constructors
    // note a subclass should have at least the superclass's
    // parameters and maybe some more
    private List resolveSuperParams(List params) {
	List superParams = new ArrayList();
        if (this.root.getExtend().equals("simkit.SimEntityBase") ||
            this.root.getExtend().equals("simkit.BasicSimEntity"))
            return superParams;

	try {
	    Class c = Class.forName(this.root.getExtend());
	    Constructor[] ca = c.getConstructors();
	    int maxIndex = 0;
	    int maxParamCount = 0;
	    for ( int i = 0; i < ca.length; i++ ) {
                //find largest fitting array of super parameters constructor
                int tmpCount = (ca[i].getParameterTypes()).length;
 	        if ( tmpCount > maxParamCount && tmpCount <= params.size() ) {
		    maxParamCount = tmpCount;
		    maxIndex = i;
	        }
	    }

	    ListIterator it = params.listIterator();
            ParameterType[] parray = new ParameterType[maxParamCount];
            int pi = 0;
            while (it.hasNext()) {
                Object po = it.next();
                ParameterType p = (ParameterType)po;
                Class[] sparams = ca[maxIndex].getParameterTypes();
                for ( int i = pi; i < sparams.length ; i ++) {
                    if ( unqualifiedMatch( p.getType(), sparams[i].getName()) && pi < maxParamCount ) {
                        parray[pi] = p;
                        ++pi;
                        break;
                    } 
                }

            }

            superParams = java.util.Arrays.asList(parray);

	} catch ( java.lang.ClassNotFoundException cnfe ) {
	    String extend = this.root.getExtend();
	    if (extend.equals("simkit.SimEntityBase")) {
	        System.out.println(extend + " not in classpath ");
	    }
	}

	return superParams;

    }
    
    // check equivalence of eg. java.lang.Integer vs. Integer
    private boolean unqualifiedMatch(String fromXml, String fromClazz) {
        if (fromXml.equals(fromClazz)) {
            return true;
        }
        String nm[] = fromClazz.split("\\.");
        if (nm != null) {
            if (fromXml.equals(nm[nm.length - 1])) {
                return true;
            }
        }
        return false;
        
    }

    // returns List of params from super that match
    // should be the number of params for the constructor,
    // or 0 for an oddball constructor. typically constructor
    // list size should be at minimum the same set as the super.
    // unused?
    private List paramsInSuper(Constructor c, List params) {
	Class[] cTypes = c.getParameterTypes();
	java.util.Vector pTypes = new java.util.Vector();
	java.util.Vector superPTypes = new java.util.Vector();
	java.util.Vector subset = new java.util.Vector();
	ListIterator li = params.listIterator();
	while ( li.hasNext() ) {
	    Parameter p = (Parameter) li.next();
	    pTypes.addElement(p.getType());
	}
	for ( int i = 0; i < cTypes.length; i++ ) {
	    superPTypes.addElement(cTypes[i].getName());
	}
	if ( pTypes.containsAll(superPTypes) ) {
	    li = params.listIterator();
	    while ( li.hasNext() ) {
		Parameter p = (Parameter) li.next();
		if ( superPTypes.contains(p.getType()) ) {
		    subset.addElement(p);
		}
	    }
	}

	return subset;
    }

    boolean compileCode (String fileName) {
        String fName = this.root.getName();
        if ( !fName.equals(fileName) ) {
            System.out.println("Using " + fName);
            fileName = fName + ".java";
        }
	String path = this.root.getPackage();
	File fDest;
	char[] pchars;
	int j;
	// this doesn't work! : path.replaceAll(pd,File.separator);
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

        return s.trim();

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
	if ( c.indexOf(rb) > 0 ) return true;
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
