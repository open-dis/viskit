package viskit.xsd.translator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

// Application specific imports
import viskit.xsd.bindings.eventgraph.*;
import org.apache.log4j.Logger;

/**
 * @author Rick Goldberg
 * @since March 23, 2004, 4:59 PM
 * @version $Id: SimkitXML2Java.java 1669 2007-12-19 20:27:14Z tdnorbra $
 */
public class SimkitXML2Java {

    static Logger log = Logger.getLogger(SimkitXML2Java.class);
    private SimEntity root;
    InputStream fileInputStream;
    private String fileBaseName;
    JAXBContext jaxbCtx;
    private Unmarshaller unMarshaller;
    private Object unMarshalledObject;

    /* convenience Strings for formatting */
    private final String sp = " ";
    private final String sp4 = sp + sp + sp + sp;
    private final String sp8 = sp4 + sp4;
    private final String sp12 = sp8 + sp4;
    private final String ob = "{";
    private final String cb = "}";
    private final String sc = ";";
    private final String cm = ",";
    private final String lp = "(";
    private final String rp = ")";
    private final String eq = "=";
    private final String pd = ".";
    private final String qu = "\"";
    private final String lb = "[";
    private final String rb = "]";
    
    private String extend = "";
    private String className = "";

    /**
     * Creates a new instance of SimkitXML2Java
     * when used from another class, instance this
     * with a String for the className of the xmlFile
     * @param xmlFile 
     */
    public SimkitXML2Java(String xmlFile) {
        try {
            fileBaseName = baseNameOf(xmlFile);
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
            fileInputStream = Class.forName("viskit.xsd.translator.SimkitXML2Java").getClassLoader().getResourceAsStream(xmlFile);
        } catch (ClassNotFoundException cnfe) {
            log.error(cnfe);
        } catch (JAXBException ex) {
            log.error(ex);
        }
    }

    public SimkitXML2Java(InputStream stream) {
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
            fileInputStream = stream;
        } catch (JAXBException ex) {
            log.error(ex);
        }
    }

    public SimkitXML2Java(File f) throws Exception {
        fileBaseName = baseNameOf(f.getName());
        jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
        fileInputStream = new FileInputStream(f);
    }

    public void unmarshal() {        
        try {
            setUnMarshaller(jaxbCtx.createUnmarshaller());
            setUnMarshalledObject(getUnMarshaller().unmarshal(fileInputStream));
            this.root = (SimEntity) getUnMarshalledObject();
        } catch (JAXBException ex) {
            
            // Silence attempting to unmarshal an Assembly here
            log.debug("Error occuring in SimkitXML2Java.unmarshal(): " + ex);
        }
    }
    
    public Unmarshaller getUnMarshaller() {
        return unMarshaller;
    }

    public void setUnMarshaller(Unmarshaller unMarshaller) {
        this.unMarshaller = unMarshaller;
    }

    /**
     * 
     * @return
     */
    public Object getUnMarshalledObject() {
        return unMarshalledObject;
    }

    public void setUnMarshalledObject(Object unMarshalledObject) {
        this.unMarshalledObject = unMarshalledObject;
    }
    
    public String translate() {
        
        StringBuffer source = new StringBuffer();
        StringWriter head = new StringWriter();
        StringWriter vars = new StringWriter();
        StringWriter runBlock = new StringWriter();
        StringWriter eventBlock = new StringWriter();
        StringWriter accessorBlock = new StringWriter();
        StringWriter tail = new StringWriter();

        buildHead(head);
        buildVars(vars, accessorBlock);
        buildEventBlock(runBlock, eventBlock);
        buildTail(tail);

        buildSource(source, head, vars, runBlock, eventBlock, accessorBlock, tail);

        return source.toString();
    }
    
    /**
     * 
     * @return
     */
    public String getFileBaseName() {
        return fileBaseName;
    }

    /**
     * 
     * @param fileBaseName
     */
    public void setFileBaseName(String fileBaseName) {
        this.fileBaseName = fileBaseName;
    }
    
    /**
     * 
     * @param data
     * @param out
     */
    public void writeOut(String data, PrintStream out) {
        out.println(data);
    }
    
    /**
     * 
     * @return
     */
    public SimEntity getRoot() {
        return root;
    }

    void buildHead(StringWriter head) {

        PrintWriter pw = new PrintWriter(head);
        className = this.root.getName();
        String pkg = this.root.getPackage();
        extend = this.root.getExtend();
        String implement = this.root.getImplement();
        // TBD: should be checking the class definitions
        // of the Interfaces and create a code block
        // if none exists with template methods, and
        // Events for any "do" methods if none exists.
        if (implement != null) {
            extend += sp + "implements" + sp + implement;
        }

        pw.println("package " + pkg + sc);
        pw.println();
        pw.println("// Standard library imports");
        pw.println("import java.util.*;");
        pw.println();
        pw.println("// Application specific imports");
        pw.println("import org.apache.log4j.Logger;");
        pw.println("import simkit.*;");
        pw.println("import simkit.random.*;");
        pw.println();
        pw.println("public class " + className + sp + "extends" + sp + extend + sp + ob);
        pw.println();
    }

    void buildVars(StringWriter vars, StringWriter accessorBlock) {

        PrintWriter pw = new PrintWriter(vars);

        List<Parameter> liParam = this.root.getParameter();
        List<Parameter> superParams = resolveSuperParams(this.root.getParameter());

        // Logger instantiation
        pw.println(sp4 + "static Logger log " + eq + " Logger" + pd + 
                "getLogger" + lp + className + pd + "class" + rp + sc);
        pw.println();
        pw.println(sp4 + "/* Simulation Parameters */");
        for (Parameter p : liParam) {

            if (!superParams.contains(p)) {
                pw.println(sp4 + "private" + sp + p.getType() + sp + p.getName() + sc);
            } else {
                pw.println(sp4 + "/* inherited parameter " + p.getType() + sp + p.getName() + " */");
            }

            if (!(extend.indexOf("SimEntityBase") < 0)) {
                buildParameterAccessor(p, accessorBlock);
            } else if (!superParams.contains(p)) {
                buildParameterAccessor(p, accessorBlock);
            }
        }

        pw.println();

        List<StateVariable> liStateV = this.root.getStateVariable();

        pw.println(sp4 + "/* Simulation State Variables */");
        for (StateVariable s : liStateV) {
            
            Class<?> c = null;
            
            // TODO: use better checking for primitive types i.e. Class.isPrimitive()
            
            // TODO: Determine if encountering generics that contain array types
            if (isGeneric(s.getType())) {
                pw.println(sp4 + "protected" + sp + s.getType() + sp + s.getName() + sp + eq + sp + "new" + sp + s.getType() + lp + rp + sc);
            } else {
                try {
                    c = Class.forName(s.getType());
                } catch (ClassNotFoundException cnfe) {
//                log.error(cnfe);
                    pw.println(sp4 + "protected" + sp + stripLength(s.getType()) + sp + s.getName() + sc);
                }
            }

            if (c != null) {
                Constructor cst = null;

                try {
                    cst = c.getConstructor(new Class<?>[] {});
                } catch (NoSuchMethodException nsme) {
//                    log.error(nsme);
                }

                if (cst != null) {
                    pw.println(sp4 + "protected" + sp + s.getType() + sp + s.getName() + sp + eq + sp + "new" + sp + s.getType() + lp + rp + sc);
                } else { // really not a bad case, most likely will be set by the reset()
                    pw.println(sp4 + "protected" + sp + s.getType() + sp + s.getName() + sp + eq + sp + "null" + sc);
                }
            }
            buildStateVariableAccessor(s, accessorBlock);
        }
    }

    // TODO: May have to check for generic containers of array types
    void buildParameterAccessor(Parameter p, StringWriter sw) {

        PrintWriter pw = new PrintWriter(sw);

        pw.print(sp4 + "public void set" + capitalize(p.getName()) + lp);
        pw.println(p.getType() + sp + shortinate(p.getName()) + rp + sp + ob);
        pw.print(sp8 + "this" + pd + p.getName() + sp + eq + sp);

        if (isArray(p.getType()) || isGeneric(p.getType())) {
            pw.print(lp + p.getType() + rp + sp + shortinate(p.getName()));
            pw.println(pd + "clone" + lp + rp + sc);
        } else {
            pw.println(shortinate(p.getName()) + sc);
        }
        pw.println(sp4 + cb);
        pw.println();

        /* also provide indexed set/getters, may be multidimensional, however,
         * not expected to actually be multidimensional
         */
        if (isArray(p.getType())) {
            int d = dims(p.getType());

            pw.print(sp4 + "public void set" + capitalize(p.getName()) + lp + indx(d));
            pw.println(baseOf(p.getType()) + sp + shortinate(p.getName()) + rp + sp + ob);
            pw.println(sp8 + "this" + pd + p.getName() + indxbr(d) + sp + eq + sp + shortinate(p.getName()) + sc);
            pw.println(sp4 + cb);
            pw.println();


            pw.print(sp4 + "public" + sp + baseOf(p.getType()) + sp + "get");
            pw.print(capitalize(p.getName()) + lp + indxncm(d));
            pw.println(rp + sp + ob);
            pw.println(sp8 + "return" + sp + p.getName() + indxbr(d) + sc);
            pw.println(sp4 + cb);
            pw.println();
        }

        pw.print(sp4 + "public " + p.getType() + sp + "get" + capitalize(p.getName()));
        pw.println(lp + rp + sp + ob);
        pw.println(sp8 + "return" + sp + p.getName() + sc);
        pw.println(sp4 + cb);
        pw.println();
    }

    private int dims(String t) {
        int d = 0;
        int s = 0;

        while ((s = t.indexOf("[")) > 0) {
            d++;
            t = t.substring(s + 1);
        }
        return d;
    }

    private String indx(int dims) {
        String inds = "";

        for (int k = 0; k < dims; k++) {
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

        for (int k = 0; k < dims; k++) {
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
         
            // Supress warning call to unchecked cast
            if (isGeneric(s.getType())) {
                pw.println(sp4 + "@SuppressWarnings(\"unchecked\")");
            }
        }

        pw.print(sp4 + "public " + stripLength(s.getType()) + sp + "get" + capitalize(s.getName()));
        pw.println(lp + rp + sp + ob);
        pw.println(sp8 + "return" + sp + tyStr + sp + s.getName() + clStr + sc);
        pw.println(sp4 + cb);
        pw.println();

        // Prevent creating accessor for generic containers of array types
        if (isArray(s.getType()) && !isGeneric(s.getType())) {
            int d = dims(s.getType());
            pw.print(sp4 + "public" + sp + baseOf(s.getType()) + sp + "get");
            pw.print(capitalize(s.getName()) + lp + indxncm(d));
            pw.println(rp + sp + ob);
            pw.println(sp8 + "return" + sp + s.getName() + indxbr(d) + sc);
            pw.println(sp4 + cb);
            pw.println();
        }
    }

    void buildEventBlock(StringWriter runBlock, StringWriter eventBlock) {
       
        List<Event> events = this.root.getEvent();
        boolean didRun = false;

        for (Event e : events) {
            if (e.getName().equals("Run")) {
                doRunBlock(e, runBlock);
                didRun = true;
            } else {
                doEventBlock(e, eventBlock);
            }
        }

        if (!didRun) {
            Event r = (new ObjectFactory()).createEvent();
            r.setName("Run");
            doRunBlock(r, runBlock);
        }
    }

    void doRunBlock(Event run, StringWriter runBlock) {
        
        PrintWriter pw = new PrintWriter(runBlock);
        List<Object> liSchedCanc = run.getScheduleOrCancel();
        List<Parameter> superPList = new ArrayList<Parameter>();
        List<Parameter> pList = this.root.getParameter();

        pw.println();
        pw.println(sp4 + "@viskit.ParameterMap" + sp + lp);
        pw.print(sp8 + "names =" + sp + ob);
        for (Parameter pt : pList) {
            pw.print(qu + pt.getName() + qu);
            if (pList.indexOf(pt) < pList.size() - 1) {
                pw.print(cm);
                pw.println();
                pw.print(sp8 + sp4);
            }
        }
        pw.println(cb + cm);
        pw.print(sp8 + "types =" + sp + ob);
        for (Parameter pt : pList) {
            pw.print(qu + pt.getType() + qu);
            if (pList.indexOf(pt) < pList.size() - 1) {
                pw.print(cm);
                pw.println();
                pw.print(sp8 + sp4);
            }
        }
        pw.println(cb);
        pw.println(sp4 + rp);
        pw.println();
        pw.println(sp4 + "/** Creates a new instance of " + this.root.getName() + " */");
        pw.print(sp4 + "public " + this.root.getName() + lp);

        for (Parameter pt : pList) {

            pw.print(pt.getType() + sp + shortinate(pt.getName()));

            if (pList.size() > 1) {
                if (pList.indexOf(pt) < pList.size() - 1) {
                    pw.print(cm);
                    pw.println();
                    pw.print(sp8 + sp4);
                }
            }
        }

        pw.println(rp + sp + ob);

        if (extend.indexOf("SimEntityBase") < 0) {

            pList = this.root.getParameter();
            superPList = resolveSuperParams(pList);
            pw.print(sp8 + "super" + lp);
            for (Parameter pt : superPList) {
                pw.print(shortinate(pt.getName()));
                if ((superPList.size() > 1) && (superPList.indexOf(pt) < superPList.size() - 1)) {
                    pw.print(cm);
                }
            }
            pw.println(rp + sc);
        }

        // skip over any sets that would get done in the superclass
        for (int l = superPList.size(); l < this.root.getParameter().size(); l++) {

            Parameter pt = this.root.getParameter().get(l);
            pw.println(sp8 + "set" + capitalize(pt.getName()) + lp + shortinate(pt.getName()) + rp + sc);
        }

        // create new arrays, if any
        // note: have to assume that the length of parameter arrays
        // is consistent
        List<StateVariable> liStateV = this.root.getStateVariable();

        /* Prevent generic containers of arrays from getting instantiated twice.
         * Already done in the state variable declarations
         */
        for (StateVariable st : liStateV) {
            if (isArray(st.getType()) && !isGeneric(st.getType())) {
                pw.println(sp8 + st.getName() + sp + eq + sp + "new" + sp + st.getType() + sc);
            }
        }

        pw.println(sp4 + cb);
        pw.println();
        pw.println(sp4 + "/** Set initial values of all state variables */");
        pw.println(sp4 + "@Override");
        pw.println(sp4 + "public void reset() {");

        List<LocalVariable> liLocalV = run.getLocalVariable();

        for (LocalVariable local : liLocalV) {
            pw.println(sp8 + local.getType() + sp + local.getName() + sc);
        }

        pw.println(sp8 + "super.reset()" + sc);
        pw.println();
        pw.println(sp8 + "/* StateTransitions for the Run Event */");

        List<StateTransition> liStateT = run.getStateTransition();

        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();
            
            /* Prevent generic containers of arrays from getting initialized as
             * array types.
             */
            boolean isar = isArray(sv.getType()) && !isGeneric(sv.getType());
            String spn = isar ? sp12 : sp8;
            String in = indexFrom(st);

            if (isar) {
                pw.print(sp8 + "for (" + in + " = 0; " + in + " < " + sv.getName() + pd + "length");
                pw.println(sc + sp + in + "++" + rp + sp + ob);
                pw.print(spn + sv.getName() + lb + in + rb);
            } else {
                pw.print(spn + sv.getName());
            }

            if (asg == null) {
                pw.println(pd + ops.getMethod() + sc);
            } else {
                pw.println(sp + eq + sp + asg.getValue() + sc);
            }

            if (isar) {
                pw.println(sp8 + cb);
            }
        }

        pw.println(sp4 + cb);
        pw.println();        

        // check if super has a doRun()
        if (extend.indexOf("SimEntityBase") < 0) {
            
            Method doRun = null;
            try {
                Class<?> sup = Class.forName(extend);
                doRun = sup.getDeclaredMethod("doRun", new Class<?>[] {});
            } catch (ClassNotFoundException cnfe) {                
                
                // If using plain Vanilla Viskit, don't report on diskit extended EGs
                if (!cnfe.getMessage().contains("diskit")) {
//                    log.error(cnfe);
                }
            } catch (NoSuchMethodException cnfe) {
//                log.error(cnfe);
            }
            if (doRun != null) {
                pw.println(sp4 + "@Override");
                pw.println(sp4 + "public void doRun" + lp + rp + sp + ob);
                pw.println(sp8 + "super.doRun" + lp + rp + sc);
            } else {
                pw.println(sp4 + "public void doRun" + lp + rp + sp + ob);
            }
        } else {
            pw.println(sp4 + "public void doRun" + lp + rp + sp + ob);
        }

        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            pw.print(sp8 + "firePropertyChange" + lp + qu + sv.getName() + qu);
            
            // Give these FPC "getters" as argements
            String stateVariableName = sv.getName().substring(0, 1).toUpperCase() + sv.getName().substring(1);
            String stateVariableGetter = "get" + stateVariableName + lp + rp;
            pw.println(cm + sp + stateVariableGetter + rp + sc);
        }

        for (Object o : liSchedCanc) {
            if (o instanceof Schedule) {
                doSchedule((Schedule) o, run, pw);
            } else {
                doCancel((Cancel) o, run, pw);
            }
        }

        String x = "";
        if (run.getCode() != null) {
            x = run.getCode();
        }
        pw.println(sp8 + x);
        pw.println(sp4 + cb);
        pw.println();
    }

    /** these Events should now not be any Run event
     * @param e
     * @param eventBlock 
     */
    void doEventBlock(Event e, StringWriter eventBlock) {
        PrintWriter pw = new PrintWriter(eventBlock);
        List<StateTransition> liStateT = e.getStateTransition();
        List<Argument> liArgs = e.getArgument();
        List<LocalVariable> liLocalV = e.getLocalVariable();
        List<Object> liSchedCanc = e.getScheduleOrCancel();
                
        pw.print(sp4 + "public void do" + e.getName() + lp);

        for (Argument a : liArgs) {
            pw.print(a.getType() + sp + a.getName());
            if (liArgs.size() > 1 && liArgs.indexOf(a) < liArgs.size() - 1) {
                pw.print(cm + sp);
            }
        }

        // finish the method decl
        pw.println(rp + sp + ob);

        // local variable decls
        for (LocalVariable local : liLocalV) {
            String[] lines = {" "};
            String value = local.getValue();
            if (!("".equals(value))) {
                lines = value.split("\\;");
            }
            pw.print(sp8 + local.getType() + sp + local.getName() + sp + eq);
            pw.println(sp + lp + local.getType() + rp + sp + lines[0].trim() + sc);
            for (int i = 1; i < lines.length; i++) {
                pw.println(sp8 + lines[i].trim() + sc);
            }
        }

        if (liLocalV.size() > 0) {
            pw.println();
        }

        if (e.getCode() != null) {
            pw.println(sp8 + "/* Code insertion for Event " + e.getName() + " */");
            String[] lines = e.getCode().split("\\n");
            for (int i = 0; i < lines.length; i++) {
                pw.println(sp8 + lines[i]);
            }
            pw.println(sp8 + "/* End Code insertion */");
            pw.println();
        }

        LinkedList<String> decls = new LinkedList<String>();
        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();
            String change = "";
            String olds = ""; // old decl line Bar oldFoo ...
            String oldName = sv.getName(); // oldFoo
            if (ops != null) {
                change = pd + ops.getMethod() + sc;
            } else if (asg != null) {
                change = sp + eq + sp + asg.getValue() + sc;
            }
            oldName = "_old_" + oldName.substring(0, 1).toUpperCase() + oldName.substring(1);
            if (!decls.contains(oldName)) {
                olds = sv.getType();
                decls.add(oldName);
                
                // Prevent calling a generic container of arrays with an index
                if (isArray(olds) && !isGeneric(olds)) {
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
            if ("".equals(olds)) {
                olds = getter;
            } else {
                olds += getter;
            }
            
            // Prevent accessing an array index for generic containers of array types
            // check need _idxvar_from(st)
            if (isArray(sv.getType()) && !isGeneric(olds)) {
                olds += indexFrom(st);
            }
            olds += rp + sc;
            // now olds is Bar oldFoo = getFoo(<idxvar>?);
            // add this to the pre-formatted block
            olds += sv.getName() + ((isArray(sv.getType()) && !isGeneric(sv.getType())) ? lb + indexFrom(st) + rb : "") + change;
            String[] lines = olds.split("\\;");
            // format it
            for (int i = 0; i < lines.length; i++) {
                if (i == 0) {
                    pw.println(sp8 + "/* StateTransition for " + sv.getName() + " */");
                } else if (i == 2) {
                    pw.println(sp8 + "/* Code block for pre-transition */");
                }
                pw.println(sp8 + lines[i] + sc);
            }
            if (isArray(sv.getType()) && !isGeneric(sv.getType())) {
                pw.print(sp8 + "fireIndexedPropertyChange" + lp + indexFrom(st));
                pw.print(cm + sp + qu + sv.getName() + qu + cm);
                pw.println(oldName + cm + sp + "get" + oldName.substring(5) + lp + indexFrom(st) + rp + rp + sc);
            } else {
                pw.print(sp8 + "firePropertyChange" + lp + qu + sv.getName() + qu + cm + sp);
                pw.println(oldName + cm + sp + "get" + oldName.substring(5) + lp + rp + rp + sc);
            }
            pw.println();
        }
        
        // waitDelay/interrupt
        for (Object o : liSchedCanc) {
            if (o instanceof Schedule) {
                doSchedule((Schedule) o, e, pw);
            } else {
                doCancel((Cancel) o, e, pw);
            }
        }
        pw.println(sp4 + cb);
        pw.println();
    }

    void doSchedule(Schedule s, Event e, PrintWriter pw) {
        String condent = "";

        if (s.getCondition() != null) {
            condent = sp4;
            pw.println(sp8 + "if" + sp + lp + s.getCondition() + rp + sp + ob);
        }

        pw.print(sp8 + condent + "waitDelay" + lp + qu + ((Event) s.getEvent()).getName() + qu + cm);

        // according to schema to meet Priority class definition, the following tags should be permitted:
        // HIGHEST, HIGHER, HIGH, DEFAULT, LOW, LOWER, and LOWEST,
        // however, historically these could be numbers.
        // check if Number, assign TAG; tbd how these are scaled?

        int prioIndex = 0;
        try {
            prioIndex = Integer.parseInt(s.getPriority());
        } catch (NumberFormatException nfe1) {
            try {
                prioIndex = (int) Double.parseDouble(s.getPriority());
            } catch (NumberFormatException nfe2) {
//                log.error(nfe2);
            }
        }

        // numerical priority values from -3 to 3 
        // this range may need to be scaled or shifted
        // or not, see simkit

        if (prioIndex > 3) {
            prioIndex = 3;
        }
        if (prioIndex < -3) {
            prioIndex = -3;
        }

        String[] priorities = {"LOWEST", "LOWER", "LOW", "DEFAULT", "HIGH", "HIGHER", "HIGHEST"};
        String prio = priorities[prioIndex + 3];

        pw.print(s.getDelay() + cm + "Priority" + pd + prio);

        // varargs can throw a mostly harmless compiler warning if there is only one arg here
        // "warning: non-varargs call of varargs method with inexact argument type for last parameter""
        // If there are more than one or none there is no ambiguity.
        // If the one arg case is cast as Object then the warning is suppressed

        if (s.getEdgeParameter().size() == 1) {

            EdgeParameter ep = s.getEdgeParameter().get(0);

            pw.print(cm + "(Object)");
            pw.print(lp + ep.getValue() + rp);

        } else if (s.getEdgeParameter().size() > 1) {
            pw.print(cm);

            // prevent a comma after the last parameter
            for (ListIterator<EdgeParameter> edgeParamIterator = s.getEdgeParameter().listIterator(); edgeParamIterator.hasNext();) {
                EdgeParameter param = edgeParamIterator.next();
                pw.print(param.getValue());
                if (edgeParamIterator.hasNext()) {
                    pw.print(cm);
                }
            }
        }

        pw.println(rp + sc);

        if (s.getCondition() != null) {
            pw.println(sp8 + cb);
        }
    }

    void doCancel(Cancel c, Event e, PrintWriter pw) {
        List<EdgeParameter> liEdgeP = c.getEdgeParameter();
        Class<?> cl = null;
        String condent = "";
        Event event = (Event) c.getEvent();
        List<Argument> eventArgs = event.getArgument();
        ListIterator<Argument> eventArgsi = eventArgs.listIterator();

        if (c.getCondition() != null) {
            condent = sp4;
            pw.println(sp8 + "if" + sp + lp + c.getCondition() + rp + sp + ob);
        }

        pw.print(sp8 + condent + "interrupt" + lp + qu + event.getName() + qu + cm);
        pw.print("new Object[]" + ob);

        for (EdgeParameter ep : liEdgeP) {
            Argument arg = eventArgsi.next();
            try {
                cl = Class.forName(arg.getType());
            } catch (ClassNotFoundException cnfe) {
                // most likely a primitive type
                String type = arg.getType();
                String constructor = "new" + sp;
                if (type.equals("int")) {
                    constructor += "Integer";
                } else if (type.equals("float")) {
                    constructor += "Float";
                } else if (type.equals("double")) {
                    constructor += "Double";
                } else if (type.equals("long")) {
                    constructor += "Long";
                } else if (type.equals("boolean")) {
                    constructor += "Boolean";
                } else if (ep.getValue().equals("this")) {
                    constructor = "";
                }
                pw.print(constructor + lp + ep.getValue() + rp);
            }
            if (cl != null) {
                pw.print(ep.getValue());
            }

            if (liEdgeP.size() > 1 && liEdgeP.indexOf(ep) < liEdgeP.size() - 1) {
                pw.print(cm);
            }
        }
        pw.println(cb + rp + sc);

        if (c.getCondition() != null) {
            pw.println(sp8 + cb);
        }
    }

    void buildTail(StringWriter t) {
        PrintWriter pw = new PrintWriter(t);
        String code = root.getCode();
        if (code != null) {
            pw.println(sp4 + "/* Inserted code for " + this.root.getName() + " */");
            String[] lines = code.split("\\n");
            for (String codeLines : lines) {
                pw.println(sp4 + codeLines);
            }
            pw.println(sp4 + "/* End inserted code */");
        }
        pw.println(cb);
    }

    void buildSource(StringBuffer source, StringWriter head, StringWriter vars, StringWriter runBlock,
            StringWriter eventBlock, StringWriter accessorBlock, StringWriter tail) {

        source.append(head.getBuffer()).append(vars.getBuffer()).append(runBlock.getBuffer());
        source.append(eventBlock.getBuffer()).append(accessorBlock.getBuffer()).append(tail.getBuffer());
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean isGeneric(String type) {
        return (type.contains("<") && type.contains(">"));
    }

    private String stripLength(String s) {
        int left, right;
        if (!isArray(s)) {
            return s;
        }
        left = s.indexOf(lb);
        right = s.indexOf(rb);
        return s.substring(0, left + 1) + s.substring(right);
    }

    // find the maximal set that the subclass parameters
    // can cover of the superclass's available constructors
    // note a subclass should have at least the superclass's
    // parameters and maybe some more
    private List<Parameter> resolveSuperParams(List<Parameter> params) {
        List<Parameter> superParams = new ArrayList<Parameter>();
        if (extend.equals("simkit.SimEntityBase") || extend.equals("simkit.BasicSimEntity")) {
            return superParams;
        }

        try {
            // the extend field may also contain an implemnts
            // tail.

            Class<?> c = Class.forName(extend.split("\\s")[0]);
            Constructor[] ca = c.getConstructors();
            int maxIndex = 0;
            int maxParamCount = 0;
            for (int i = 0; i < ca.length; i++) {
                //find largest fitting array of super parameters constructor
                int tmpCount = (ca[i].getParameterTypes()).length;
                if (tmpCount > maxParamCount && tmpCount <= params.size()) {
                    maxParamCount = tmpCount;
                    maxIndex = i;
                }
            }

            Parameter[] parray = new Parameter[maxParamCount];
            int pi = 0;
            for (Parameter p : params) {
                Class<?>[] sparams = ca[maxIndex].getParameterTypes();
                for (int i = pi; i < sparams.length; i++) {
                    if (unqualifiedMatch(p.getType(), sparams[i].getName()) && pi < maxParamCount) {
                        parray[pi] = p;
                        ++pi;
                        break;
                    }
                }
            }

            superParams = Arrays.asList(parray);

        } catch (java.lang.ClassNotFoundException cnfe) {
            if (extend.equals("simkit.SimEntityBase")) {
                log.error(extend + " not in classpath ");
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
    private List<Parameter> paramsInSuper(Constructor c, List<Parameter> params) {
        Class<?>[] cTypes = c.getParameterTypes();
        Vector<String> pTypes = new Vector<String>();
        Vector<String> superPTypes = new Vector<String>();
        Vector<Parameter> subset = new Vector<Parameter>();
        for (Parameter p : params) {
            pTypes.addElement(p.getType());
        }
        for (int i = 0; i < cTypes.length; i++) {
            superPTypes.addElement(cTypes[i].getName());
        }
        if (pTypes.containsAll(superPTypes)) {
            for (Parameter p : params) {
                if (superPTypes.contains(p.getType())) {
                    subset.addElement(p);
                }
            }
        }
        return subset;
    }

    boolean compileCode(String fileName) {
        String fName = this.root.getName();
        if (!fName.equals(fileName)) {
            System.out.println("Using " + fName);
            fileName = fName + ".java";
        }
        String path = this.root.getPackage();
        File fDest;
        char[] pchars;
        // this doesn't work! : path.replaceAll(pd,File.separator);
        pchars = path.toCharArray();
        for (char character : pchars) {
            if (character == '.') {
                character = File.separatorChar;
            }
        }
        path = new String(pchars);
        try {
            File f = new File(pd + File.separator + path);
            f.mkdirs();
            fDest = new File(path + File.separator + fileName);
            f = new File(fileName);
            f.renameTo(fDest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (com.sun.tools.javac.Main.compile(new String[] {"-Xlint:unchecked", 
            "-Xlint:deprecation", "-verbose", "-sourcepath", path, "-d", pd, 
            path + File.separator + fileName}) == 0);
    }

    // bug fix 1183
    private String indexFrom(StateTransition st) {
        return st.getIndex();
    }

    private String shortinate(String s) {
        return s.trim();
    }

    private String baseOf(String s) {
        return s.substring(0, s.indexOf(lb));
    }

    private String indexIn(String s) {
        return s.substring(s.indexOf(lb) + 1, s.indexOf(rb) - 1);
    }

    private String baseNameOf(String s) {
        return s.substring(0, s.indexOf(pd));
    }

    private boolean isCloneable(String c) {

        Class<?> aClass = null;

        try {
            aClass = Thread.currentThread().getContextClassLoader().loadClass(c);
        } catch (ClassNotFoundException cnfe) {
//            log.error(cnfe);
        }

        if (aClass != null) {
            return java.lang.Cloneable.class.isAssignableFrom(aClass);
        }
        return isArray(c) || isGeneric(c);
    }

    private boolean isArray(String c) {
        return (c.indexOf(rb) > 0);
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

        log.info("XML file is: " + args[0]);
        log.info("Generating Java Source...");
        
        InputStream is = null;
        try {
            is = new FileInputStream(args[0]);
        } catch (FileNotFoundException fnfe) {log.error(fnfe);}

//        SimkitXML2Java sx2j = new SimkitXML2Java(args[0]);
        SimkitXML2Java sx2j = new SimkitXML2Java(is);
        File baseName = new File(sx2j.baseNameOf(args[0]));
        log.info("baseName: " + baseName.getAbsolutePath());
        sx2j.setFileBaseName(baseName.getName());
        sx2j.unmarshal();

        String dotJava = sx2j.translate();
        
        // Print to console if given args < 1
        if (args.length > 1) {
            sx2j.writeOut(dotJava, System.out);
        }

        log.info("Done.");

        // also write out the .java to a file and compile it
        // to a .class
        log.info("Generating Java Bytecode...");
        try {
            File fileName = new File(sx2j.getRoot().getPackage() + "/" + sx2j.getFileBaseName() + ".java");
            fileName.getParentFile().mkdir();
            FileOutputStream fout = new FileOutputStream(fileName);
            PrintStream ps = new PrintStream(fout, true);
            sx2j.writeOut(dotJava, ps);
            if (!sx2j.compileCode(fileName.getAbsolutePath())) {
                sx2j.error("Compile error " + fileName);
            } else {
                log.info("Done.");
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
    }   
}