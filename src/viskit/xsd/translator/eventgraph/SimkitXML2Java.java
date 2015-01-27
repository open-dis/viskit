package viskit.xsd.translator.eventgraph;

import edu.nps.util.LogUtils;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;
import viskit.control.AssemblyControllerImpl;
import viskit.VGlobals;
import viskit.VStatics;
import viskit.xsd.bindings.eventgraph.*;

/** A generator of source code from Event Graph XML
 *
 * @author Rick Goldberg
 * @since March 23, 2004, 4:59 PM
 * @version $Id$
 */
public class SimkitXML2Java {

    static Logger log = LogUtils.getLogger(SimkitXML2Java.class);

    /* convenience Strings for formatting */
    public final static String SP = " ";
    public final static String SP_4 = SP + SP + SP + SP;
    public final static String SP_8 = SP_4 + SP_4;
    public final static String SP_12 = SP_8 + SP_4;
    public final static String OB = "{";
    public final static String CB = "}";
    public final static String SC = ";";
    public final static String CM = ",";
    public final static String LP = "(";
    public final static String RP = ")";
    public final static String EQ = "=";
    public final static String PD = ".";
    public final static String QU = "\"";
    public final static String LB = "[";
    public final static String RB = "]";
    public final static String JDO = "/**";
    public final static String JDC = "*/";
    public final static String PUBLIC = "public";
    public final static String PROTECTED = "protected";
    public final static String PRIVATE = "private";
    public final static String SIM_ENTITY_BASE = "SimEntityBase";

    private SimEntity root;
    InputStream fileInputStream;
    private String fileBaseName;
    JAXBContext jaxbCtx;
    private Unmarshaller unMarshaller;
    private Object unMarshalledObject;

    private String extendz = "";
    private String className = "";
    private String packageName = "";
    private File eventGraphFile;

    private List<Parameter> superParams;
    private List<Parameter> liParams;

    /** Default to initialize the JAXBContext only */
    private SimkitXML2Java() {
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.eventgraph");
        } catch (JAXBException ex) {
            log.error(ex);
            error(ex.getMessage());
        }
    }

    public SimkitXML2Java(InputStream stream) {
        this();
        fileInputStream = stream;
    }

    /**
     * Creates a new instance of SimkitXML2Java
     * when used from another class.  Instance this
     * with a String for the className of the xmlFile
     * @param xmlFile the file to generate code from
     */
    public SimkitXML2Java(String xmlFile) {
        this(VStatics.classForName(SimkitXML2Java.class.getName()).getClassLoader().getResourceAsStream(xmlFile));
        setFileBaseName(new File(baseNameOf(xmlFile)).getName());
        setEventGraphFile(new File(xmlFile));
    }

    public SimkitXML2Java(File f) throws FileNotFoundException {
        this(new FileInputStream(f));
        setFileBaseName(baseNameOf(f.getName()));
        setEventGraphFile(f);
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

    /** @return an unmarshalled JAXB Object */
    public Object getUnMarshalledObject() {
        return unMarshalledObject;
    }

    public void setUnMarshalledObject(Object unMarshalledObject) {
        this.unMarshalledObject = unMarshalledObject;
    }

    /** @return the XML to Java translated source as a string */
    public String translate() {

        StringBuilder source = new StringBuilder();
        StringWriter head = new StringWriter();
        StringWriter vars = new StringWriter();
        StringWriter accessorBlock = new StringWriter();
        StringWriter toStringBlock = new StringWriter();
        StringWriter parameterMapAndConstructor = new StringWriter();
        StringWriter runBlock = new StringWriter();
        StringWriter eventBlock = new StringWriter();
        StringWriter codeBlock = new StringWriter();

        buildHead(head);
        buildVars(vars, accessorBlock);
        buildToString(toStringBlock);
        buildParameterMapAndConstructor(parameterMapAndConstructor);
        buildEventBlock(runBlock, eventBlock);

        buildCodeBlock(codeBlock);

        buildSource(source, head, vars, parameterMapAndConstructor, runBlock, eventBlock, accessorBlock, toStringBlock, codeBlock);

        return source.toString();
    }

    /** @return the base name of this EG file */
    public String getFileBaseName() {
        return fileBaseName;
    }

    /**
     * Set the base name of this XML file
     * @param fileBaseName the base name of this XML file
     */
    public final void setFileBaseName(String fileBaseName) {
        this.fileBaseName = fileBaseName;
    }

    /** @return the XML root of this SimEntity */
    public SimEntity getRoot() {
        return root;
    }

    void buildHead(StringWriter head) {

        PrintWriter pw = new PrintWriter(head);
        className = this.root.getName();
        packageName = this.root.getPackage();
        extendz = this.root.getExtend();
        String implementz = this.root.getImplement();

        // TBD: should be checking the class definitions
        // of the Interfaces and create a code block
        // if none exists with template methods, and
        // Events for any "do" methods if none exists.
        if (implementz != null) {
            extendz += SP + "implements" + SP + implementz;
        }

        pw.println("package " + packageName + SC);
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
        pw.println("public class " + className + SP + "extends" + SP + extendz + SP + OB);
        pw.println();
    }

    void buildVars(StringWriter vars, StringWriter accessorBlock) {

        PrintWriter pw = new PrintWriter(vars);

        liParams = this.root.getParameter();
        superParams = resolveSuperParams(liParams);

        // Logger instantiation (for debugging only)
//        pw.println(sp4 + "static Logger LogUtils.getLogger() " + eq + " Logger" + pd +
//                "getLogger" + lp + className + pd + "class" + rp + sc);
//        pw.println();
        pw.println(SP_4 + "/* Simulation Parameters */");
        pw.println();
        for (Parameter p : liParams) {

            if (!superParams.contains(p)) {
                if (!p.getComment().isEmpty()) {
                    pw.print(SP_4 + JDO + SP);
                    for (String comment : p.getComment()) {
                        pw.print(comment);
                    }
                    pw.println(SP + JDC);
                }
                pw.println(SP_4 + PRIVATE + SP + p.getType() + SP + p.getName() + SC);
                pw.println();
            }

            if (extendz.contains(SIM_ENTITY_BASE)) {
                buildParameterAccessor(p, accessorBlock);
            } else if (!superParams.contains(p)) {
                buildParameterAccessor(p, accessorBlock);
            }
        }

        List<StateVariable> liStateV = this.root.getStateVariable();

        pw.println(SP_4 + "/* Simulation State Variables */");
        pw.println();
        for (StateVariable s : liStateV) {

            Class<?> c = null;

            // TODO: Determine if encountering generics that contain array types
            if (isGeneric(s.getType())) {
                if (!s.getComment().isEmpty()) {
                    pw.print(SP_4 + JDO + SP);
                    for (String comment : s.getComment()) {
                        pw.print(comment);
                    }
                    pw.println(SP + JDC);
                }
                pw.println(SP_4 + PROTECTED + SP + s.getType() + SP + s.getName() + SP + EQ + SP + "new" + SP + s.getType() + LP + RP + SC);
            } else {

                c = VStatics.classForName(s.getType());

                if (c == null || c.isPrimitive() || isArray(s.getType())) {
                    if (!s.getComment().isEmpty()) {
                        pw.print(SP_4 + JDO + SP);
                        for (String comment : s.getComment()) {
                            pw.print(comment);
                        }
                        pw.println(SP + JDC);
                    }
                    pw.println(SP_4 + PROTECTED + SP + stripLength(s.getType()) + SP + s.getName() + SC);
                } else {
                    pw.println(SP_4 + "/*" + SP + "inherited state variable" + SP + s.getType() + SP + s.getName() + SP + JDC);
                }
            }

            if (c != null && !c.isPrimitive() && !isArray(s.getType())) {
                Constructor<?> cst = null;

                try {
                    cst = c.getConstructor(new Class<?>[] {});
                } catch (NoSuchMethodException nsme) {
//                    log.error(nsme);
                }

                if (!s.getComment().isEmpty()) {
                    pw.print(SP_4 + JDO + SP);
                    for (String comment : s.getComment()) {
                        pw.print(comment);
                    }
                    pw.println(SP + JDC);
                }

                if (cst != null) {
                    pw.println(SP_4 + PROTECTED + SP + s.getType() + SP + s.getName() + SP + EQ + SP + "new" + SP + s.getType() + LP + RP + SC);
                } else { // really not a bad case, most likely will be set by the reset()
                    pw.println(SP_4 + PROTECTED + SP + s.getType() + SP + s.getName() + SP + EQ + SP + "null" + SC);
                }
            }
            buildStateVariableAccessor(s, accessorBlock);

            pw.println();
        }
        if (liStateV.isEmpty()) {
            pw.println(SP_4 + "/* None */");
            pw.println();
        }
    }

    // TODO: May have to check for generic containers of array types
    void buildParameterAccessor(Parameter p, StringWriter sw) {

        PrintWriter pw = new PrintWriter(sw);

        pw.print(SP_4 + "public void set" + capitalize(p.getName()) + LP);
        pw.println(p.getType() + SP + shortinate(p.getName()) + RP + SP + OB);
        pw.print(SP_8 + "this" + PD + p.getName() + SP + EQ + SP);

        if (isArray(p.getType()) || isGeneric(p.getType())) {

            pw.print(shortinate(p.getName()));
            pw.println(PD + "clone" + LP + RP + SC);
        } else {
            pw.println(shortinate(p.getName()) + SC);
        }
        pw.println(SP_4 + CB);
        pw.println();

        /* also provide indexed set/getters, may be multidimensional, however,
         * not expected to actually be multidimensional
         */
        if (isArray(p.getType())) {
            int d = dims(p.getType());

            pw.print(SP_4 + "public void set" + capitalize(p.getName()) + LP + indx(d));
            pw.println(baseOf(p.getType()) + SP + shortinate(p.getName()) + RP + SP + OB);
            pw.println(SP_8 + "this" + PD + p.getName() + indxbr(d) + SP + EQ + SP + shortinate(p.getName()) + SC);
            pw.println(SP_4 + CB);
            pw.println();

            pw.print(SP_4 + PUBLIC + SP + baseOf(p.getType()) + SP + "get");
            pw.print(capitalize(p.getName()) + LP + indxncm(d));
            pw.println(RP + SP + OB);
            pw.println(SP_8 + "return" + SP + p.getName() + indxbr(d) + SC);
            pw.println(SP_4 + CB);
            pw.println();
        }

        pw.print(SP_4 + "public " + p.getType() + SP + "get" + capitalize(p.getName()));
        pw.println(LP + RP + SP + OB);
        pw.println(SP_8 + "return" + SP + p.getName() + SC);
        pw.println(SP_4 + CB);
        pw.println();
    }

    private void buildToString(StringWriter toStringBlock) {

        // Assume this is a subclass of some SimEntityBase which should already
        // have a toString()
        if (!extendz.contains(SIM_ENTITY_BASE)) {return;}

        PrintWriter pw = new PrintWriter(toStringBlock);
        pw.println(SP_4 + "@Override");
        pw.print(SP_4 + "public String toString");
        pw.println(LP + RP + SP + OB);
        pw.println(SP_8 + "return" + SP + "getClass().getName()" + SC);
        pw.println(SP_4 + CB);
    }

    private int dims(String t) {
        int d = 0;
        int s;

        while ((s = t.indexOf("[")) > 0) {
            d++;
            t = t.substring(s + 1);
        }
        return d;
    }

    private String indx(int dims) {
        String inds = "";

        for (int k = 0; k < dims; k++) {
            inds += "int" + SP + "i" + k + CM + SP;
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
            inds += LB + "i" + k + RB;
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

            if (!isArray(s.getType()) || isGeneric(s.getType())) {
                tyStr = LP + stripLength(s.getType()) + RP;
            }

            // Supress warning call to unchecked cast since we return a clone
            // of Objects vice the desired type
            if (isGeneric(s.getType())) {
                pw.println(SP_4 + "@SuppressWarnings(\"unchecked\")");
            }
        }

        if (isArray(s.getType()) && !isGeneric(s.getType())) {
            int d = dims(s.getType());
            pw.print(SP_4 + PUBLIC + SP + baseOf(s.getType()) + SP + "get");
            pw.print(capitalize(s.getName()) + LP + indxncm(d));
            pw.println(RP + SP + OB);
            pw.println(SP_8 + "return" + SP + s.getName() + indxbr(d) + SC);
            pw.println(SP_4 + CB);
            pw.println();
        } else {
            pw.print(SP_4 + "public " + stripLength(s.getType()) + SP + "get" + capitalize(s.getName()));
            pw.println(LP + RP + SP + OB);
            pw.println(SP_8 + "return" + SP + (tyStr + SP + s.getName() + clStr).trim() + SC);
            pw.println(SP_4 + CB);
            pw.println();
        }
    }

    void buildParameterMapAndConstructor(StringWriter parameterMapAndConstructor) {

        PrintWriter pw = new PrintWriter(parameterMapAndConstructor);

        pw.println(SP_4 + "@viskit.ParameterMap" + SP + LP);
        pw.print(SP_8 + "names =" + SP + OB);
        for (Parameter pt : liParams) {
            pw.print(QU + pt.getName() + QU);
            if (liParams.indexOf(pt) < liParams.size() - 1) {
                pw.print(CM);
                pw.println();
                pw.print(SP_8 + SP_4);
            }
        }
        pw.println(CB + CM);
        pw.print(SP_8 + "types =" + SP + OB);
        for (Parameter pt : liParams) {
            pw.print(QU + pt.getType() + QU);
            if (liParams.indexOf(pt) < liParams.size() - 1) {
                pw.print(CM);
                pw.println();
                pw.print(SP_8 + SP_4);
            }
        }
        pw.println(CB);
        pw.println(SP_4 + RP);
        pw.println();
        pw.println(SP_4 + "/** Creates a new default instance of " + this.root.getName() + " */");

        // Generate a zero parameter (default) constructor in addition to a
        // parameterized constroctor
        if (!liParams.isEmpty()) {
            pw.println(SP_4 + "public " + this.root.getName() + LP + RP + SP + OB);
            pw.println(SP_4 + CB);
            pw.println();
        }

        // Now, generate the parameterized or zero parameter consructor
        pw.print(SP_4 + "public " + this.root.getName() + LP);
        for (Parameter pt : liParams) {

            pw.print(pt.getType() + SP + shortinate(pt.getName()));

            if (liParams.size() > 1) {
                if (liParams.indexOf(pt) < liParams.size() - 1) {
                    pw.print(CM);
                    pw.println();
                    pw.print(SP_8 + SP_4);
                }
            }
        }

        pw.println(RP + SP + OB);

        if (!extendz.contains(SIM_ENTITY_BASE)) {

            pw.print(SP_8 + "super" + LP);
            for (Parameter pt : superParams) {
                pw.print(shortinate(pt.getName()));
                if ((superParams.size() > 1) && (superParams.indexOf(pt) < superParams.size() - 1)) {
                    pw.print(CM);
                }
            }
            pw.println(RP + SC);
        }

        // skip over any sets that would get done in the superclass
        for (int l = superParams.size(); l < liParams.size(); l++) {

            Parameter pt = liParams.get(l);
            pw.println(SP_8 + "set" + capitalize(pt.getName()) + LP + shortinate(pt.getName()) + RP + SC);
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
                pw.println(SP_8 + st.getName() + SP + EQ + SP + "new" + SP + st.getType() + SC);
            }
        }

        pw.println(SP_4 + CB);
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
        List<LocalVariable> liLocalV = run.getLocalVariable();
        List<Object> liSchedCanc = run.getScheduleOrCancel();

        /* Handle the reset method */

        pw.println(SP_4 + "@Override");
        pw.println(SP_4 + "public void reset() {");
        pw.println(SP_8 + "super.reset()" + SC);

        pw.println();

        for (LocalVariable local : liLocalV) {
            pw.println(SP_8 + local.getType() + SP + local.getName() + SC);
        }

        if (!liLocalV.isEmpty()) {pw.println();}

        List<StateTransition> liStateT = run.getStateTransition();

        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();

            /* Prevent generic containers of arrays from getting initialized as
             * array types.
             */
            boolean isar = isArray(sv.getType()) && !isGeneric(sv.getType());
            String sps = isar ? SP_12 : SP_8;
            String in = indexFrom(st);

            if (isar) {
                pw.print(SP_8 + "for (" + in + SP + EQ + SP + "0; " + in + " < " + sv.getName() + PD + "length");
                pw.println(SC + SP + in + "++" + RP + SP + OB);
                pw.print(sps + sv.getName() + LB + in + RB);
            } else {
                pw.print(sps + sv.getName());
            }

            if (ops != null) {
                pw.println(PD + ops.getMethod() + SC);
            } else if (asg != null) {
                pw.println(SP + EQ + SP + asg.getValue() + SC);
            }

            if (isar) {
                pw.println(SP_8 + CB);
            }
        }

        pw.println(SP_4 + CB);
        pw.println();

        /* Handle the doRun method */

        Method doRun = null;

        // check if super has a doRun()
        if (!extendz.contains(SIM_ENTITY_BASE)) {

            try {
                Class<?> sup = resolveExtensionClass();
                doRun = sup.getDeclaredMethod("doRun", new Class<?>[] {});
            } catch (NoSuchMethodException cnfe) {
//                log.error(cnfe);
            }
        }

        if (doRun != null) {
            pw.println(SP_4 + "@Override");
            pw.println(SP_4 + "public void doRun" + LP + RP + SP + OB);
            pw.println(SP_8 + "super.doRun" + LP + RP + SC);
        } else {
            pw.println(SP_4 + JDO + SP + "Bootstraps the first simulation event" + SP + JDC);
            pw.println(SP_4 + "public void doRun" + LP + RP + SP + OB);
        }

        for (LocalVariable local : liLocalV) {
            pw.println(SP_8 + local.getType() + SP + local.getName() + (local.getValue() != null ? SP + EQ + SP + local.getValue() + SC : SC));
        }

        pw.println();

        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();

            boolean isar = isArray(sv.getType()) && !isGeneric(sv.getType());

            pw.print(SP_8 + "firePropertyChange" + LP + QU + sv.getName() + QU);

            // Give these FPC "getters" as arguments
            String stateVariableName = sv.getName().substring(0, 1).toUpperCase() + sv.getName().substring(1);
            String stateVariableGetter = "get" + stateVariableName + LP;

            if (isar) {
                if (ops != null) {
                    stateVariableGetter += RP + PD + ops.getMethod();
                } else if (asg != null) {
                    stateVariableGetter += asg.getValue() + RP;
                }
            } else {
                stateVariableGetter += RP;
            }

            pw.println(CM + SP + stateVariableGetter + RP + SC);
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
        pw.println(SP_8 + x);
        pw.println(SP_4 + CB);
        pw.println();
    }

    /** these Events should now be any other than the Run event
     * @param e the Event to process
     * @param eventBlock the StringWriter assigned to write the Event
     */
    void doEventBlock(Event e, StringWriter eventBlock) {
        log.debug("Event is: " + e.getName());
        PrintWriter pw = new PrintWriter(eventBlock);
        List<StateTransition> liStateT = e.getStateTransition();
        List<Argument> liArgs = e.getArgument();
        List<LocalVariable> liLocalV = e.getLocalVariable();
        List<Object> liSchedCanc = e.getScheduleOrCancel();

        Method superMethod = null;

        // check if super has a doEventName()
        if (!extendz.contains(SIM_ENTITY_BASE)) {
            try {
                Class<?> sup = resolveExtensionClass();
                superMethod = sup.getDeclaredMethod("do" + e.getName(), new Class<?>[]{});
            } catch (NoSuchMethodException cnfe) {
//            log.error(cnfe);
            }
        }

        if (superMethod != null) {
            pw.println(SP_4 + "@Override");
            pw.println(SP_4 + "public void do" + e.getName() + LP + RP + SP + OB);
            pw.println(SP_8 + "super.do" + e.getName() + LP + RP + SC);
            pw.println();
        } else {
            pw.print(SP_4 + "public void do" + e.getName() + LP);

            for (Argument a : liArgs) {
                pw.print(a.getType() + SP + a.getName());
                if (liArgs.size() > 1 && liArgs.indexOf(a) < liArgs.size() - 1) {
                    pw.print(CM + SP);
                }
            }

            // finish the method decl
            pw.println(RP + SP + OB);
        }

        // local variable decls
        for (LocalVariable local : liLocalV) {
            String[] lines = {" "};
            String value = local.getValue();
            if (!("".equals(value))) {
                lines = value.split("\\;");
            }
            pw.print(SP_8 + local.getType() + SP + local.getName() + SP + EQ);

            // reduce redundant casts
            pw.println(SP + lines[0].trim() + SC);
            for (int i = 1; i < lines.length; i++) {
                pw.println(SP_8 + lines[i].trim() + SC);
            }
        }

        if (liLocalV.size() > 0) {
            pw.println();
        }

        if (e.getCode() != null) {
            pw.println(SP_8 + "/* Code insertion for Event " + e.getName() + " */");
            String[] lines = e.getCode().split("\\n");
            for (String line : lines) {
                pw.println(SP_8 + line);
            }
            pw.println(SP_8 + "/* End Code insertion */");
            pw.println();
        }

        List<String> decls = new LinkedList<>();
        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();
            String change = "";
            String olds = ""; // old decl line Bar oldFoo ...
            String oldName = sv.getName(); // oldFoo
            if (ops != null) {
                change = PD + ops.getMethod() + SC;
            } else if (asg != null) {
                change = SP + EQ + SP + asg.getValue() + SC;
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
                olds += SP;
            }

            // by now, olds is "Bar" ( not Bar[] )
            // or nothing if alreadyDecld
            // now build up "Bar oldFoo = getFoo()"
            String getter = oldName + SP + EQ + SP + "get" + oldName.substring(5) + LP;
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
            olds += RP + SC;
            // now olds is Bar oldFoo = getFoo(<idxvar>?);
            // add this to the pre-formatted block
            olds += sv.getName() + ((isArray(sv.getType()) && !isGeneric(sv.getType())) ? LB + indexFrom(st) + RB : "") + change;
            String[] lines = olds.split("\\;");
            // format it
            for (int i = 0; i < lines.length; i++) {
                if (i == 0) {
                    pw.println(SP_8 + "/* StateTransition for " + sv.getName() + " */");
                } else if (i == 2) {
                    pw.println(SP_8 + "/* Code block for pre-transition */");
                }
                pw.println(SP_8 + lines[i] + SC);
            }
            if (isArray(sv.getType()) && !isGeneric(sv.getType())) {
                pw.print(SP_8 + "fireIndexedPropertyChange" + LP + indexFrom(st));
                pw.print(CM + SP + QU + sv.getName() + QU + CM);
                pw.println(oldName + CM + SP + "get" + oldName.substring(5) + LP + indexFrom(st) + RP + RP + SC);
            } else {
                pw.print(SP_8 + "firePropertyChange" + LP + QU + sv.getName() + QU + CM + SP);
                pw.println(oldName + CM + SP + "get" + oldName.substring(5) + LP + RP + RP + SC);
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
        pw.println(SP_4 + CB);
        pw.println();
    }

    void doSchedule(Schedule s, Event e, PrintWriter pw) {
        String condent = "";

        if (s.getCondition() != null) {
            condent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + s.getCondition() + RP + SP + OB);
        }

        pw.print(SP_8 + condent + "waitDelay" + LP + QU + ((Event) s.getEvent()).getName() + QU + CM + SP);

        // according to schema to meet Priority class definition, the following
        // tags should be permitted:
        // HIGHEST, HIGHER, HIGH, DEFAULT, LOW, LOWER, and LOWEST,
        // however, historically these could be numbers.

        // Bugfix 1400: These should now be eneumerations instead of FP values
        pw.print(s.getDelay() + CM + " Priority" + PD + s.getPriority());

        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        for (EdgeParameter ep : s.getEdgeParameter()) {
            pw.print(CM + " (Object) ");

            String epValue = ep.getValue();

            // Cover case where there is a "+ 1" increment, or "-1" decrement on a value
            if (epValue.contains("+") || epValue.contains("-")) {
                pw.print(LP + ep.getValue() + RP);
            } else {
                pw.print(ep.getValue());
            }
        }

        pw.println(RP + SC);

        if (s.getCondition() != null) {
            pw.println(SP_8 + CB);
        }
    }

    void doCancel(Cancel c, Event e, PrintWriter pw) {
        List<EdgeParameter> liEdgeP = c.getEdgeParameter();
        String condent = "";
        Event event = (Event) c.getEvent();

        if (c.getCondition() != null) {
            condent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + c.getCondition() + RP + SP + OB);
        }

        pw.print(SP_8 + condent + "interrupt" + LP + QU + event.getName() + QU);
        for (EdgeParameter ep : liEdgeP) {
            pw.print(CM + SP + "(Object)" + ep.getValue());
        }
        pw.print(RP + SC);
        pw.println();
        if (c.getCondition() != null) {
            pw.println(SP_8 + CB);
        }
    }

    void buildCodeBlock(StringWriter t) {
        PrintWriter pw = new PrintWriter(t);
        String code = root.getCode();
        if (code != null) {
            pw.println(SP_4 + "/* Inserted code for " + this.root.getName() + " */");
            String[] lines = code.split("\\n");
            for (String codeLines : lines) {
                pw.println(SP_4 + codeLines);
            }
            pw.println(SP_4 + "/* End inserted code */");
        }
        pw.println(CB);
    }

    void buildSource(StringBuilder source, StringWriter head, StringWriter vars,
            StringWriter parameterMapAndConstructor, StringWriter runBlock,
            StringWriter eventBlock, StringWriter accessorBlock, StringWriter toStringBlock, StringWriter codeBlock) {

        source.append(head.getBuffer()).append(vars.getBuffer());
        source.append(parameterMapAndConstructor.getBuffer());
        source.append(runBlock.getBuffer());
        source.append(eventBlock.getBuffer()).append(accessorBlock.getBuffer());
        source.append(toStringBlock);
        source.append(codeBlock.getBuffer());
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean isGeneric(String type) {
        return type.contains("<") && type.contains(">");
    }

    public File getEventGraphFile() {return eventGraphFile;}

    public final void setEventGraphFile(File f) {
        eventGraphFile = f;
    }

    private String stripLength(String s) {
        int left, right;
        if (!isArray(s)) {
            return s;
        }
        left = s.indexOf(LB);
        right = s.indexOf(RB);
        return s.substring(0, left + 1) + s.substring(right);
    }

    /** Resolves for either qualified, or unqualified extension name
     *
     * @return the resolved extension class type
     */
    private Class<?> resolveExtensionClass() {

        String unqualifiedExtends;
        Class<?> c;
        if (!extendz.contains(".")) {
            unqualifiedExtends = packageName + "." + extendz;
            c = VStatics.classForName(unqualifiedExtends.split("\\s")[0]);
        } else {
            c = VStatics.classForName(extendz.split("\\s")[0]);
        }
        return c;
    }

    // find the maximal set that the subclass parameters
    // can cover of the superclass's available constructors
    // note a subclass should have at least the superclass's
    // parameters and maybe some more
    private List<Parameter> resolveSuperParams(List<Parameter> params) {
        List<Parameter> localSuperParams = new ArrayList<>();
        if (extendz.contains(SIM_ENTITY_BASE) || extendz.contains("BasicSimEntity")) {
            return localSuperParams;
        }

        // the extendz field may also contain an implements
        // codeBlock.

        Class<?> c = resolveExtensionClass();

        if (c != null) {
            Constructor[] ca = c.getConstructors();
            int maxIndex = 0;
            int maxParamCount = 0;
            for (int i = 0; i < ca.length; i++) {

                // find largest fitting array of super parameters constructor
                int tmpCount = (ca[i].getParameterTypes()).length;
                if (tmpCount > maxParamCount && tmpCount <= params.size()) {
                    maxParamCount = tmpCount;
                    maxIndex = i;
                }
            }

            Parameter[] parray = new Parameter[maxParamCount];
            int pi = 0;
            Class<?>[] sparams = ca[maxIndex].getParameterTypes();

            outer:
            for (Parameter p : params) {
                for (int i = pi; i < sparams.length; i++) {
                    if (unqualifiedMatch(p.getType(), sparams[i].getName()) && pi < maxParamCount) {
                        parray[pi] = p;
                        ++pi;
                        break outer;
                    }
                }
            }

            localSuperParams = Arrays.asList(parray);
        } else {
            log.error(extendz + " was not found on the working classpath");
        }

        return localSuperParams;
    }

    /** Check equivalence of e.g. java.lang.Integer vs. Integer
     *
     * @param fromXml the subclass parameter to check
     * @param fromClazz the superclass parameter to check
     * @return indication of a match
     */
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

    // bug fix 1183
    private String indexFrom(StateTransition st) {
        return st.getIndex();
    }

    private String shortinate(String s) {
        return s.trim();
    }

    private String baseOf(String s) {
        return s.substring(0, s.indexOf(LB));
    }

    private String indexIn(String s) {
        return s.substring(s.indexOf(LB) + 1, s.indexOf(RB) - 1);
    }

    private String baseNameOf(String s) {
        return s.substring(0, s.indexOf(PD));
    }

    private boolean isCloneable(String c) {

        Class<?> aClass = null;

        try {
            aClass = VGlobals.instance().getWorkClassLoader().loadClass(c);
        } catch (ClassNotFoundException cnfe) {
//            log.error(cnfe);
        }

        if (aClass != null) {
            return Cloneable.class.isAssignableFrom(aClass);
        }
        return isArray(c) || isGeneric(c);
    }

    private boolean isArray(String a) {
        return (a.contains(RB));
    }

    /** Report and exit the JVM
     *
     * @param desc a description of the encountered error
     */
    private void error(String desc) {
        log.error(desc);
        System.exit(1);
    }

    /**
     * Follow this pattern to use this class from another,
     * otherwise this can be used stand alone from CLI
     *
     * @param args the command line arguments args[0] - XML file to translate
     */
    public static void main(String[] args) {

        String xmlFile = args[0].replaceAll("\\\\", "/");
        log.info("EventGraph (EG) file is: " + xmlFile);
        log.info("Generating Java Source...");

        InputStream is = null;
        try {
            is = new FileInputStream(xmlFile);
        } catch (FileNotFoundException fnfe) {log.error(fnfe);}

        SimkitXML2Java sx2j = new SimkitXML2Java(is);
        File baseName = new File(sx2j.baseNameOf(xmlFile));
        sx2j.setFileBaseName(baseName.getName());
        sx2j.setEventGraphFile(new File(xmlFile));
        sx2j.unmarshal();
        String dotJava = sx2j.translate();
        if (dotJava != null && !dotJava.isEmpty()) {
            log.info("Done.");
        } else {
            log.warn("Compile error on: " + xmlFile);
            return;
        }

        // also write out the .java to a file and compile it
        // to a .class
        log.info("Generating Java Bytecode...");
        try {
            if (AssemblyControllerImpl.compileJavaClassFromString(dotJava) != null) {
                log.info("Done.");
            }
        } catch (NullPointerException npe) {
            log.error(npe);
//            npe.printStackTrace();
        }
    }
}