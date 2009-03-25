package viskit.xsd.translator;

import edu.nps.util.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

// Application specific imports
import viskit.xsd.bindings.eventgraph.*;
import viskit.VGlobals;

/**
 * @author Rick Goldberg
 * @since March 23, 2004, 4:59 PM
 * @version $Id$
 */
public class SimkitXML2Java {

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
    
    private String extendz = "";
    private String className = "";
    private File eventGraphFile;

    /** Default to initialize the JAXBContext only */
    private SimkitXML2Java() {
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
        } catch (JAXBException ex) {
            LogUtils.getLogger().error(ex);
        }       
    }
    
    /**
     * Creates a new instance of SimkitXML2Java
     * when used from another class, instance this
     * with a String for the className of the xmlFile
     * @param xmlFile 
     */
    public SimkitXML2Java(String xmlFile) {
        this();
        try {
            fileBaseName = baseNameOf(xmlFile);
            fileInputStream = Class.forName("viskit.xsd.translator.SimkitXML2Java").getClassLoader().getResourceAsStream(xmlFile);
        } catch (ClassNotFoundException cnfe) {
            LogUtils.getLogger().error(cnfe);
        }
    }

    public SimkitXML2Java(InputStream stream) {
        this();
        fileInputStream = stream;        
    }

    public SimkitXML2Java(File f) throws FileNotFoundException {
        this(new FileInputStream(f));
        fileBaseName = baseNameOf(f.getName());
        setEventGraphFile(f);
    }

    public void unmarshal() {        
        try {
            setUnMarshaller(jaxbCtx.createUnmarshaller());
            setUnMarshalledObject(getUnMarshaller().unmarshal(fileInputStream));
            this.root = (SimEntity) getUnMarshalledObject();
        } catch (JAXBException ex) {
            
            // Silence attempting to unmarshal an Assembly here
            LogUtils.getLogger().debug("Error occuring in SimkitXML2Java.unmarshal(): " + ex);
        }
    }
    
    public Unmarshaller getUnMarshaller() {
        return unMarshaller;
    }

    public void setUnMarshaller(Unmarshaller unMarshaller) {
        this.unMarshaller = unMarshaller;
    }

    /** @return an unmarshalled JAXB Ojbect */
    public Object getUnMarshalledObject() {
        return unMarshalledObject;
    }

    public void setUnMarshalledObject(Object unMarshalledObject) {
        this.unMarshalledObject = unMarshalledObject;
    }
    
    /** @return the XML to Java translated source as a string */
    public String translate() {
        
        StringBuffer source = new StringBuffer();
        StringWriter head = new StringWriter();
        StringWriter vars = new StringWriter();
        StringWriter accessorBlock = new StringWriter();
        StringWriter toStringBlock = new StringWriter();
        StringWriter parameterMapAndConstructor = new StringWriter();
        StringWriter runBlock = new StringWriter();
        StringWriter eventBlock = new StringWriter();
        StringWriter tail = new StringWriter();

        buildHead(head);
        buildVars(vars, accessorBlock);
        buildToString(toStringBlock);
        buildParameterMapAndConstructor(parameterMapAndConstructor);
        buildEventBlock(runBlock, eventBlock);
        
        // TODO: Rename?  This is actually the code block builder
        buildTail(tail);

        buildSource(source, head, vars, parameterMapAndConstructor, runBlock, eventBlock, accessorBlock, toStringBlock, tail);

        return source.toString();
    }
    
    /** @return the base name of this EG file */
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
    
    /** @return the XML root of this SimEntity */
    public SimEntity getRoot() {
        return root;
    }

    void buildHead(StringWriter head) {

        PrintWriter pw = new PrintWriter(head);
        className = this.root.getName();
        String pkg = this.root.getPackage();
        extendz = this.root.getExtend();
        String implementz = this.root.getImplement();
        // TBD: should be checking the class definitions
        // of the Interfaces and create a code block
        // if none exists with template methods, and
        // Events for any "do" methods if none exists.
        if (implementz != null) {
            extendz += sp + "implements" + sp + implementz;
        }

        pw.println("package " + pkg + sc);
        pw.println();
        pw.println("// Standard library imports");
        pw.println("import java.util.*;");
        pw.println();
        pw.println("// Application specific imports");
        
        // For debugging only
//        pw.println("import org.apache.log4j.Logger;");
        pw.println("import simkit.*;");
        pw.println("import simkit.random.*;");
        pw.println();
        pw.println("public class " + className + sp + "extends" + sp + extendz + sp + ob);
        pw.println();
    }

    void buildVars(StringWriter vars, StringWriter accessorBlock) {

        PrintWriter pw = new PrintWriter(vars);

        List<Parameter> liParam = this.root.getParameter();
        List<Parameter> superParams = resolveSuperParams(this.root.getParameter());

        // Logger instantiation
//        pw.println(sp4 + "static Logger LogUtils.getLogger() " + eq + " Logger" + pd +
//                "getLogger" + lp + className + pd + "class" + rp + sc);
//        pw.println();
        pw.println(sp4 + "/* Simulation Parameters */");
        for (Parameter p : liParam) {

            if (!superParams.contains(p)) {
                pw.println(sp4 + "private" + sp + p.getType() + sp + p.getName() + sc);
            } else {
                pw.println(sp4 + "/* inherited parameter " + p.getType() + sp + p.getName() + " */");
            }

            if (!(extendz.indexOf("SimEntityBase") < 0)) {
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
//                LogUtils.getLogger().error(cnfe);
                    pw.println(sp4 + "protected" + sp + stripLength(s.getType()) + sp + s.getName() + sc);
                }
            }

            if (c != null) {
                Constructor cst = null;

                try {
                    cst = c.getConstructor(new Class<?>[] {});
                } catch (NoSuchMethodException nsme) {
//                    LogUtils.getLogger().error(nsme);
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
            
            pw.print(shortinate(p.getName()));
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

    private void buildToString(StringWriter toStringBlock) {
        PrintWriter pw = new PrintWriter(toStringBlock);
        pw.println(sp4 + "/** Override the toString() method of java.lang.Object */");
        pw.println(sp4 + "@Override");
        pw.print(sp4 + "public String toString");
        pw.println(lp + rp + sp + ob);
//        pw.println(sp8 + "super.toString()" + sc);
        pw.println(sp8 + "return" + sp + "this.getClass().getName()" + sc);
//        pw.println(sp8 + "return" + sp + "super.toString()" + sp + "+" + sp + 
//                qu + " :: " + qu + sp + "+" + sp + "this.getClass().getName()" + sc);
        pw.println(sp4 + cb);
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
            
            if (!isArray(s.getType())) {
                tyStr = lp + stripLength(s.getType()) + rp;
            }
         
            // Supress warning call to unchecked cast
            if (isGeneric(s.getType())) {
                pw.println(sp4 + "@SuppressWarnings(\"unchecked\")");
            }
        }

        pw.print(sp4 + "public " + stripLength(s.getType()) + sp + "get" + capitalize(s.getName()));
        pw.println(lp + rp + sp + ob);
        pw.println(sp8 + "return" + sp + (tyStr + sp + s.getName() + clStr).trim() + sc);
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

    void buildParameterMapAndConstructor(StringWriter parameterMapAndConstructor) {
        
        PrintWriter pw = new PrintWriter(parameterMapAndConstructor);
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

        if (extendz.indexOf("SimEntityBase") < 0) {

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
    }

   void buildEventBlock(StringWriter runBlock, StringWriter eventBlock) {
       
        List<Event> events = this.root.getEvent();

        // Bugfix 1398
        for (Event e : events) {
            if (e.getName().equals("Run")) {
                doRunBlock(e, runBlock);
            } else {
                doEventBlock(e, eventBlock);
            }
        }
    }
    
    void doRunBlock(Event run, StringWriter runBlock) {
        
        PrintWriter pw = new PrintWriter(runBlock);
        List<Object> liSchedCanc = run.getScheduleOrCancel();
        
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
        if (extendz.indexOf("SimEntityBase") < 0) {
            
            Method doRun = null;
            try {
                Class<?> sup = Class.forName(extendz);
                doRun = sup.getDeclaredMethod("doRun", new Class<?>[] {});
            } catch (ClassNotFoundException cnfe) {                
                
                // If using plain Vanilla Viskit, don't report on diskit extended EGs
                if (!cnfe.getMessage().contains("diskit")) {
//                    LogUtils.getLogger().error(cnfe);
                }
            } catch (NoSuchMethodException cnfe) {
//                LogUtils.getLogger().error(cnfe);
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
            
            // Give these FPC "getters" as arguments
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

    /** these Events should now be any other than the Run event
     * @param e the Event to process
     * @param eventBlock the StringWriter assigned to write the Event
     */
    void doEventBlock(Event e, StringWriter eventBlock) {
        LogUtils.getLogger().debug("Event is: " + e.getName());
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
                        
            // reduce redundant casts
            pw.println(sp + lines[0].trim() + sc);
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
            for (String line : lines) {
                pw.println(sp8 + line);
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

        // according to schema to meet Priority class definition, the following 
        // tags should be permitted:
        // HIGHEST, HIGHER, HIGH, DEFAULT, LOW, LOWER, and LOWEST,
        // however, historically these could be numbers.
        
        // Bugfix 1400: These should now be eneumerations instead of FP values
        pw.print(s.getDelay() + cm + " Priority" + pd + s.getPriority());
        
        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        for (EdgeParameter ep : s.getEdgeParameter()) {
            pw.print(cm + " (Object) ");
            
            String epValue = ep.getValue();
            
            // Cover case where there is a "+ 1" increment, or "-1" decrement on a value
            if (epValue.contains("+") || epValue.contains("-")) {
                pw.print(lp + ep.getValue() + rp);
            } else {
                pw.print(ep.getValue());
            }            
        }

        pw.println(rp + sc);

        if (s.getCondition() != null) {
            pw.println(sp8 + cb);
        }
    }

    void doCancel(Cancel c, Event e, PrintWriter pw) {
        List<EdgeParameter> liEdgeP = c.getEdgeParameter();
        String condent = "";
        Event event = (Event) c.getEvent();

        if (c.getCondition() != null) {
            condent = sp4;
            pw.println(sp8 + "if" + sp + lp + c.getCondition() + rp + sp + ob);
        }

        pw.print(sp8 + condent + "interrupt" + lp + qu + event.getName() + qu);
        for (EdgeParameter ep : liEdgeP) {
            pw.print(cm + sp + "(Object)" + ep.getValue());
        }
        pw.print(rp + sc);
        pw.println();
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

    void buildSource(StringBuffer source, StringWriter head, StringWriter vars, 
            StringWriter parameterMapAndConstructor, StringWriter runBlock,
            StringWriter eventBlock, StringWriter accessorBlock, StringWriter toStringBlock, StringWriter tail) {

        source.append(head.getBuffer()).append(vars.getBuffer());
        source.append(parameterMapAndConstructor.getBuffer());
        source.append(runBlock.getBuffer());
        source.append(eventBlock.getBuffer()).append(accessorBlock.getBuffer());
        source.append(toStringBlock);
        source.append(tail.getBuffer());
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean isGeneric(String type) {
        return (type.contains("<") && type.contains(">"));
    }

    public File getEventGraphFile() {return eventGraphFile;}
    
    public void setEventGraphFile(File f) {
        eventGraphFile = f;
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
        if (extendz.equals("simkit.SimEntityBase") || extendz.equals("simkit.BasicSimEntity")) {
            return superParams;
        }

        try {
            // the extendz field may also contain an implemnts
            // tail.

            Class<?> c = Class.forName(extendz.split("\\s")[0]);
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
            if (extendz.equals("simkit.SimEntityBase")) {
                LogUtils.getLogger().error(extendz + " not in classpath ");
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
//            LogUtils.getLogger().error(cnfe);
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

        String xmlFile = args[0].replaceAll("\\\\", "/");
        LogUtils.getLogger().info("Event Graph (EG) file is: " + xmlFile);
        LogUtils.getLogger().info("Generating Java Source...");
        
        InputStream is = null;
        try {
            is = new FileInputStream(xmlFile);
        } catch (FileNotFoundException fnfe) {LogUtils.getLogger().error(fnfe);}

        SimkitXML2Java sx2j = new SimkitXML2Java(is);
        File baseName = new File(sx2j.baseNameOf(xmlFile));
        sx2j.setFileBaseName(baseName.getName());
        sx2j.unmarshal();
        String dotJava = sx2j.translate();
        LogUtils.getLogger().info("Done.");

        // also write out the .java to a file and compile it
        // to a .class
        LogUtils.getLogger().info("Generating Java Bytecode...");
        if (VGlobals.instance().getAssemblyController().compileJavaClassFromString(dotJava) != null) {
           LogUtils.getLogger().info("Done.");
        }
    }
}