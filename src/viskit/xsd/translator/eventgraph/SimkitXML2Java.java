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
    public final static String RA = ">";
    public final static String LA = "<";
    public final static String JDO = "/**";
    public final static String JDC = "*/";
    public final static String PUBLIC = "public";
    public final static String PROTECTED = "protected";
    public final static String PRIVATE = "private";
    public final static String SIM_ENTITY_BASE = "SimEntityBase";
    public final static String EVENT_GRAPH_BINDINGS = "viskit.xsd.bindings.eventgraph";

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
    private List<StateVariable> liStateV;

    /** Default to initialize the JAXBContext only */
    private SimkitXML2Java() {
        try {
            jaxbCtx = JAXBContext.newInstance(EVENT_GRAPH_BINDINGS);
        } catch (JAXBException ex) {
            log.error(ex);
            error(ex.getMessage());
        }
    }

    /** Instance that facilitates code generation via the given input stream
     *
     * @param stream the file stream to generate code from
     */
    public SimkitXML2Java(InputStream stream) {
        this();
        fileInputStream = stream;
    }

    /**
     * Creates a new instance of SimkitXML2Java
     * when used from another class.  Instance this
     * with a String for the className of the xmlFile
     *
     * @param xmlFile the file to generate code from
     */
    public SimkitXML2Java(String xmlFile) {
        this(VStatics.classForName(
                SimkitXML2Java.class.getName()).getClassLoader().getResourceAsStream(xmlFile));
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
        StringWriter parameters = new StringWriter();
        StringWriter stateVars = new StringWriter();
        StringWriter accessorBlock = new StringWriter();
        StringWriter parameterMap = new StringWriter();
        StringWriter constructors = new StringWriter();
        StringWriter runBlock = new StringWriter();
        StringWriter eventBlock = new StringWriter();
        StringWriter toStringBlock = new StringWriter();
        StringWriter codeBlock = new StringWriter();

        buildHead(head);
        buildParameters(parameters, accessorBlock);
        buildStateVariables(stateVars, accessorBlock);
        buildParameterMap(parameterMap);
        buildConstructors(constructors);
        buildEventBlock(runBlock, eventBlock);
        buildToString(toStringBlock);
        buildCodeBlock(codeBlock);

        buildSource(source, head, parameters, stateVars, parameterMap,
                constructors, runBlock, eventBlock, accessorBlock,
                toStringBlock, codeBlock);

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

    public File getEventGraphFile() {
        return eventGraphFile;
    }

    public final void setEventGraphFile(File f) {
        eventGraphFile = f;
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

    void buildParameters(StringWriter vars, StringWriter accessorBlock) {

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
            } else {
                pw.println(SP_4 + "/* inherited parameter " + p.getType() + SP + p.getName() + " */");
            }
            pw.println();

            if (extendz.contains(SIM_ENTITY_BASE)) {
                buildParameterModifierAndAccessor(p, accessorBlock);
            } else if (!superParams.contains(p)) {
                buildParameterModifierAndAccessor(p, accessorBlock);
            }
        }
        if (liParams.isEmpty()) {
            pw.println(SP_4 + "/* None */");
            pw.println();
        }
    }

    void buildStateVariables(StringWriter vars, StringWriter accessorBlock) {

        PrintWriter pw = new PrintWriter(vars);

        liStateV = this.root.getStateVariable();

        pw.println(SP_4 + "/* Simulation State Variables */");
        pw.println();

        Class<?> c;
        Constructor<?> cst;
        for (StateVariable s : liStateV) {

            // Non array type generics
            if (isGeneric(s.getType())) {
                if (!s.getComment().isEmpty()) {
                    pw.print(SP_4 + JDO + SP);
                    for (String comment : s.getComment()) {
                        pw.print(comment);
                    }
                    pw.println(SP + JDC);
                }
                if (!isArray(s.getType()))
                    pw.println(SP_4 + PROTECTED + SP + s.getType() + SP + s.getName() + SP + EQ + SP + "new" + SP + stripType(s.getType()) + LP + RP + SC);
                else
                    pw.println(SP_4 + PROTECTED + SP + stripLength(s.getType()) + SP + s.getName() + SC);
            } else {

                c = VStatics.classForName(s.getType());

                // Non-super type, primitive, primitive[] or another type array
                if (c == null || VGlobals.instance().isPrimitiveOrPrimitiveArray(s.getType())) {

                    if (!s.getComment().isEmpty()) {
                        pw.print(SP_4 + JDO + SP);
                        for (String comment : s.getComment()) {
                            pw.print(comment);
                        }
                        pw.println(SP + JDC);
                    }

                    pw.println(SP_4 + PROTECTED + SP + stripLength(s.getType()) + SP + s.getName() + SC);

                } else if (!isArray(s.getType())) {

                    if (!s.getComment().isEmpty()) {
                        pw.print(SP_4 + JDO + SP);
                        for (String comment : s.getComment()) {
                            pw.print(comment);
                        }
                        pw.println(SP + JDC);
                    }

                    // NOTE: not the best way to do this, but functions for now
                    try {
                        cst = c.getConstructor(new Class<?>[]{});
                    } catch (NoSuchMethodException nsme) {
//                    log.error(nsme);

                        // reset
                        cst = null;
                    }

                    if (cst != null) {
                        pw.println(SP_4 + PROTECTED + SP + s.getType() + SP + s.getName() + SP + EQ + SP + "new" + SP + s.getType() + LP + RP + SC);
                    } else { // really not a bad case, most likely will be set by the reset()
                        pw.println(SP_4 + PROTECTED + SP + s.getType() + SP + s.getName() + SP + EQ + SP + "null" + SC);
                    }
                } else
                    pw.println(SP_4 + PROTECTED + SP + stripLength(s.getType()) + SP + s.getName() + SC);
            }

            buildStateVariableAccessor(s, accessorBlock);
            pw.println();
        }
        if (liStateV.isEmpty()) {
            pw.println(SP_4 + "/* None */");
            pw.println();
        }
    }

    /** Convenience method for stripping the type from between generic angle brackets
     *
     * @param s the generic type to strip
     * @return a stripped type from between generic angle brackets
     */
    private String stripType(String s) {
        int left, right;
        if (!isGeneric(s)) {
            return s;
        }
        left = s.indexOf(LA);
        right = s.indexOf(RA);
        return s.substring(0, left + 1) + s.substring(right);
    }

    void buildParameterModifierAndAccessor(Parameter p, StringWriter sw) {

        // Don't dup any super setters
        if (!extendz.contains(SIM_ENTITY_BASE)) {

            Class<?> sup = resolveExtensionClass();
            Method[] methods = sup.getMethods();

            for (Method m : methods) {
                if (("set" + capitalize(p.getName())).equals(m.getName())) {
                    return;
                }
            }
        }

        PrintWriter pw = new PrintWriter(sw);

        pw.print(SP_4 + "public final void set" + capitalize(p.getName()) + LP);
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

        /* also provide indexed getters, may be multidimensional, however,
         * not expected to actually be multidimensional
         */
        if (isArray(p.getType())) {
            int d = dims(p.getType());

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

        if (isArray(s.getType())) {
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

    void buildParameterMap(StringWriter parameterMap) {
        PrintWriter pw = new PrintWriter(parameterMap);

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
    }

    void buildConstructors(StringWriter constructors) {

        PrintWriter pw = new PrintWriter(constructors);

        // Generate a zero parameter (default) constructor in addition to a
        // parameterized constructor if we are not an extension
        if (superParams.isEmpty()) {
            pw.println(SP_4 + "/** Creates a new default instance of " + this.root.getName() + " */");
            pw.println(SP_4 + "public " + this.root.getName() + LP + RP + SP + OB);
            pw.println(SP_4 + CB);
            pw.println();
        }

        if (!liParams.isEmpty()) {
            for (StateVariable st : liStateV) {

                // Suppress warning call to unchecked cast since we return a clone
                // of Objects vice the desired type
                if (isGeneric(st.getType()) && isArray(st.getType())) {
                    pw.println(SP_4 + "@SuppressWarnings(\"unchecked\")");
                    break;
                }
            }

            // Now, generate the parameterized consructor
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

            Method[] methods = null;

            // check for any super params for this constructor
            if (!extendz.contains(SIM_ENTITY_BASE)) {

                Class<?> sup = resolveExtensionClass();
                methods = sup.getMethods();

                pw.print(SP_8 + "super" + LP);
                for (Parameter pt : superParams) {
                    pw.print(shortinate(pt.getName()));
                    if ((superParams.size() > 1) && (superParams.indexOf(pt) < superParams.size() - 1)) {
                        pw.print(CM + SP);
                    }
                }
                pw.println(RP + SC);
            }

            String superParam = null;

            // skip over any sets that would get done in the superclass, or
            // call super.set*()
            for (int l = superParams.size(); l < liParams.size(); l++) {

                Parameter pt = liParams.get(l);
                if (methods != null) {
                    for (Method m : methods) {
                        if (("set" + capitalize(pt.getName())).equals(m.getName())) {
                            superParam = m.getName();
                            break;
                        }
                    }
                }

                if (superParam != null && !superParam.isEmpty())
                    pw.println(SP_8 + "super.set" + capitalize(pt.getName()) + LP + shortinate(pt.getName()) + RP + SC);
                else
                    pw.println(SP_8 + "set" + capitalize(pt.getName()) + LP + shortinate(pt.getName()) + RP + SC);

                // reset
                superParam = null;
            }

            for (StateVariable st : liStateV) {
                if (isArray(st.getType())) {
                    pw.println(SP_8 + st.getName() + SP + EQ + SP + "new" + SP + stripGenerics(st.getType()) + SC);
                }
            }

            pw.println(SP_4 + CB);
            pw.println();
        }
    }

    /** Convenience method for stripping the angle brackets and type from a
     * generic array declaration
     *
     * @param type the generic type to strip
     * @return a stripped generic type, i.e. remove &lt;type&gt;
     */
    private String stripGenerics(String type) {
        int left, right;
        if (!isGeneric(type)) {
            return type;
        }
        left = type.indexOf(LA);
        right = type.indexOf(RA);
        return type.substring(0, left) + type.substring(right + 1);
    }

    void buildEventBlock(StringWriter runBlock, StringWriter eventBlock) {

        List<Event> events = this.root.getEvent();

        // Bugfix 1398
        for (Event e : events) {
            if (e.getName().equals("Run")) {
                doResetBlock(e, runBlock);
                doRunBlock(e, runBlock);
            } else {
                doEventBlock(e, eventBlock);
            }
        }
    }

    void doResetBlock(Event run, StringWriter runBlock) {

        PrintWriter pw = new PrintWriter(runBlock);
        List<LocalVariable> liLocalV = run.getLocalVariable();
        List<StateTransition> liStateT = run.getStateTransition();

        pw.println(SP_4 + "@Override");
        pw.println(SP_4 + "public void reset() " + OB);
        pw.println(SP_8 + "super.reset()" + SC);

        if (!liLocalV.isEmpty()) {
            pw.println();
            pw.println(SP_8 + "/* local variable decarlations */");
        }

        for (LocalVariable local : liLocalV) {
            pw.println(SP_8 + local.getType() + SP + local.getName() + SC);
        }

        if (!liLocalV.isEmpty()) {pw.println();}

        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();

            boolean isar = isArray(sv.getType());
            String sps = isar ? SP_12 : SP_8;
            String in = indexFrom(st);

            if (isar) {
                pw.println(SP_8 + "for " + LP + in + SP + EQ + SP + "0; " + in + " < " + sv.getName() + PD + "length"+ SC + SP + in + "++" + RP + SP + OB);
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
    }

    void doRunBlock(Event run, StringWriter runBlock) {

        PrintWriter pw = new PrintWriter(runBlock);
        List<LocalVariable> liLocalV = run.getLocalVariable();
        List<Object> liSchedCanc = run.getScheduleOrCancel();

        String doRun = null;

        // check if any super has a doRun()
        if (!extendz.contains(SIM_ENTITY_BASE)) {

            Class<?> sup = resolveExtensionClass();
            Method[] methods = sup.getMethods();
            for (Method m : methods) {
                if ("doRun".equals(m.getName()) && m.getParameterCount() == 0) {
                    doRun = m.getName();
                    break;
                }
            }
        }

        if (doRun != null) {
            pw.println(SP_4 + "@Override");
            pw.println(SP_4 + "public void " + doRun + LP + RP + SP + OB);
            pw.println(SP_8 + "super." + doRun + LP + RP + SC);
        } else {
            pw.println(SP_4 + JDO + SP + "Bootstraps the first simulation event" + SP + JDC);
            pw.println(SP_4 + "public void doRun" + LP + RP + SP + OB);
        }

        pw.println();

        if (!liLocalV.isEmpty()) {
            pw.println(SP_8 + "/* local variable decarlations */");
        }
        for (LocalVariable local : liLocalV) {
            pw.println(SP_8 + local.getType() + SP + local.getName() + SC);
        }

        if (!liLocalV.isEmpty()) {pw.println();}

        if (run.getCode() != null && !run.getCode().isEmpty()) {
            pw.println(SP_8 + "/* Code insertion for Event " + run.getName() + " */");
            String[] lines = run.getCode().split("\\n");
            for (String line : lines) {
                pw.println(SP_8 + line);
            }
            pw.println(SP_8 + "/* End Code insertion */");
            pw.println();
        }

        List<StateTransition> liStateT = run.getStateTransition();

        for (StateTransition st : liStateT) {
            StateVariable sv = (StateVariable) st.getState();
            Assignment asg = st.getAssignment();
            Operation ops = st.getOperation();

            boolean isar = isArray(sv.getType());
            String sps = isar ? SP_12 : SP_8;
            String in = indexFrom(st);

            if (isar) {
                pw.println(SP_8 + "for " + LP + in + SP + EQ + SP + "0; " + in + " < " + sv.getName() + PD + "length"+ SC + SP + in + "++" + RP + SP + OB);
                pw.print(sps + "fireIndexedPropertyChange" + LP + in + CM + SP + QU + sv.getName() + QU);
            } else {
                pw.print(SP_8 + "firePropertyChange" + LP + QU + sv.getName() + QU);
            }

            // Give these FPCs "getters" as arguments
            String stateVariableName = capitalize(sv.getName());
            String stateVariableGetter = "get" + stateVariableName + LP;

            if (isar) {
                if (ops != null) {
                    stateVariableGetter += RP + PD + ops.getMethod();
                } else if (asg != null) {
                    stateVariableGetter += in + RP;
                }
            } else {
                stateVariableGetter += RP;
            }

            pw.println(CM + SP + stateVariableGetter + RP + SC);

            if (isar) {
                pw.println(SP_8 + CB);
            }
        }

        if(!liStateT.isEmpty()) {pw.println();}

        for (Object o : liSchedCanc) {
            if (o instanceof Schedule) {
                doSchedule((Schedule) o, run, pw);
            } else {
                doCancel((Cancel) o, run, pw);
            }
        }

        pw.println(SP_4 + CB);
        pw.println();
    }

    /** These Events should now be any other than the Run, or Reset events
     *
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

        String doEvent = null;

        // check if any super has a doEventName()
        if (!extendz.contains(SIM_ENTITY_BASE)) {

            Class<?> sup = resolveExtensionClass();
            Method[] methods = sup.getMethods();
            for (Method m : methods) {
                if (("do"+e.getName()).equals(m.getName()) && m.getParameterCount() == liArgs.size()) {
                    doEvent = m.getName();
                    break;
                }
            }
        }

        if (doEvent != null) {
            pw.println(SP_4 + "@Override");
        }

        pw.print(SP_4 + "public void do" + e.getName() + LP);

        for (Argument a : liArgs) {
            pw.print(a.getType() + SP + a.getName());
            if (liArgs.size() > 1 && liArgs.indexOf(a) < liArgs.size() - 1) {
                pw.print(CM + SP);
            }
        }

        // finish the method decl
        pw.println(RP + SP + OB);

        if (doEvent != null) {
            pw.print(SP_8 + "super." + doEvent + LP);
            for (Argument a : liArgs) {
                pw.print(a.getName());
                if (liArgs.size() > 1 && liArgs.indexOf(a) < liArgs.size() - 1) {
                    pw.print(CM + SP);
                }
            }

            // finish the super decl
            pw.println(RP + SC);
        }

        pw.println();

        if (!liLocalV.isEmpty()) {
            pw.println(SP_8 + "/* local variable decarlations */");
        }
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

        if (e.getCode() != null && !e.getCode().isEmpty()) {
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
            LocalVariableAssignment lva = st.getLocalVariableAssignment();
            LocalVariableInvocation lvi = st.getLocalVariableInvocation();
            String change = "";
            String olds = ""; // old decl line Bar oldFoo ...
            String oldName = sv.getName(); // oldFoo
            if (ops != null) {
                change = PD + ops.getMethod() + SC;
            } else if (asg != null) {
                change = SP + EQ + SP + asg.getValue() + SC;
            }
            oldName = "_old_" + capitalize(oldName);
            if (!decls.contains(oldName)) {
                olds = sv.getType();
                decls.add(oldName);

                if (isArray(olds)) {
                    String[] baseName;
                    baseName = olds.split("\\[");
                    olds = baseName[0];
                }
                olds += SP;
            }

            // by now, olds is "Bar" ( not Bar[] )
            // or nothing if already Decld
            // now build up "Bar oldFoo = getFoo()"
            String getter = oldName + SP + EQ + SP + "get" + oldName.substring(5) + LP;
            if ("".equals(olds)) {
                olds = getter;
            } else {
                olds += getter;
            }

            if (isArray(sv.getType())) {
                olds += indexFrom(st);
            }
            olds += RP + SC;

            // now olds is Bar oldFoo = getFoo(<idxvar>?);
            // add this to the pre-formatted block
            olds += sv.getName() + (isArray(sv.getType()) ? LB + indexFrom(st) + RB : "") + change;
            String[] lines = olds.split("\\;");

            // format it
            for (int i = 0; i < lines.length; i++) {

                if (i == 0) {
                    pw.println(SP_8 + "/* StateTransition for " + sv.getName() + " */");
                    pw.println(SP_8 + lines[i] + SC);
                } else {

                    // Account for local assignment to accomodate state transition
                    if (lva != null && !lva.getValue().isEmpty())
                        pw.println(SP_8 + lva.getValue() + SP + EQ + SP + lines[i] + SC);
                    else
                        pw.println(SP_8 + lines[i] + SC);

                }
            }

            if (isArray(sv.getType())) {
                pw.print(SP_8 + "fireIndexedPropertyChange" + LP + indexFrom(st));
                pw.print(CM + SP + QU + sv.getName() + QU + CM + SP);
                pw.println(oldName + CM + SP + "get" + oldName.substring(5) + LP + indexFrom(st) + RP + RP + SC);
            } else {
                pw.print(SP_8 + "firePropertyChange" + LP + QU + sv.getName() + QU + CM + SP);
                pw.println(oldName + CM + SP + "get" + oldName.substring(5) + LP + RP + RP + SC);
            }

            // Now, print out any any void return type, zero parameter methods
            // as part of this state transition
            if (lvi != null) {
                String invoke = lvi.getMethod();
                if (invoke != null && !invoke.isEmpty()) {
                    pw.println(SP_8 + invoke + SC);
                }
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

        if (s.getCondition() != null && !s.getCondition().equals("true")) {
            condent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + s.getCondition() + RP + SP + OB);
        }

        pw.print(SP_8 + condent + "waitDelay" + LP + QU + ((Event) s.getEvent()).getName() + QU + CM + SP);

        // according to schema, to meet Priority class definition, the following
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

        if (s.getCondition() != null && !s.getCondition().equals("true")) {
            pw.println(SP_8 + CB);
        }
    }

    void doCancel(Cancel c, Event e, PrintWriter pw) {
        List<EdgeParameter> liEdgeP = c.getEdgeParameter();
        String condent = "";
        Event event = (Event) c.getEvent();

        if (c.getCondition() != null && !c.getCondition().equals("true")) {
            condent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + c.getCondition() + RP + SP + OB);
        }

        pw.print(SP_8 + condent + "interrupt" + LP + QU + event.getName() + QU);

        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        for (EdgeParameter ep : liEdgeP) {
            pw.print(CM + SP + "(Object) " + ep.getValue());
        }

        pw.println(RP + SC);

        if (c.getCondition() != null && !c.getCondition().equals("true")) {
            pw.println(SP_8 + CB);
        }
    }

    void buildToString(StringWriter toStringBlock) {

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

    void buildSource(StringBuilder source, StringWriter head,
            StringWriter parameters, StringWriter stateVars,
            StringWriter parameterMap, StringWriter constructors,
            StringWriter runBlock, StringWriter eventBlock,
            StringWriter accessorBlock, StringWriter toStringBlock,
            StringWriter codeBlock) {

        source.append(head.getBuffer());
        source.append(parameters.getBuffer());
        source.append(stateVars.getBuffer());
        source.append(parameterMap.getBuffer());
        source.append(constructors.getBuffer());
        source.append(runBlock.getBuffer());
        source.append(eventBlock.getBuffer());
        source.append(accessorBlock.getBuffer());
        source.append(toStringBlock);
        source.append(codeBlock.getBuffer());
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean isGeneric(String type) {
        return VGlobals.instance().isGeneric(type);
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
    // can cover of the super class's available constructors
    // note a subclass should have at least the super class's
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

            for (Parameter p : params) {
                for (int i = pi; i < sparams.length; i++) {
                    if (unqualifiedMatch(p.getType(), sparams[i].getName()) && pi < maxParamCount) {
                        parray[pi] = p;
                        ++pi;
                        break;
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
        fromClazz = VStatics.convertClassName(fromClazz);
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
        return VGlobals.instance().isArray(a);
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