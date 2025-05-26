package viskit.xsd.translator.eventgraph;


import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import viskit.control.AssemblyControllerImpl;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.xsd.bindings.eventgraph.*;

/** A generator of source code from Event Graph XML
 *
 * @author Rick Goldberg
 * @since March 23, 2004, 4:59 PM
 * @version $Id$
 */
public class SimkitEventGraphXML2Java
{
    static final Logger LOG = LogManager.getLogger();

    /* convenience Strings for formatting */
    /** space character */
    public final static String SP = " ";
    public final static String SP_4 = SP + SP + SP + SP;
    public final static String SP_8 = SP_4 + SP_4;
    public final static String SP_12 = SP_8 + SP_4;
    /** open brace */
    public final static String OB = "{";
    /** close brace */
    public final static String CB = "}";
    /** semicolon */
    public final static String SC = ";";
    /** comma */
    public final static String CM = ",";
    /** left parenthesis */
    public final static String LP = "(";
    /** right parenthesis */
    public final static String RP = ")";
    /** equals sign */
    public final static String EQ = "=";
    /** period */
    public final static String PD = ".";
    /** plus sign */
    public final static String PL = "+";
    /** quotation mark */
    public final static String QU = "\"";
    /** left bracket */
    public final static String LB = "[";
    /** right bracket */
    public final static String RB = "]";
    /** right angle-bracket &gt; */
    public final static String RA = ">";
    /** left angle-bracket &lt; */
    public final static String LA = "<";
    /** Javadoc opening comment */
    public final static String JDO = "/**";
    /**  Javadoc closing comment */
    public final static String JDC = "*/";
    public final static String PUBLIC = "public";
    public final static String PROTECTED = "protected";
    public final static String PRIVATE = "private";
    public final static String SIM_ENTITY_BASE = "SimEntityBase";
    public final static String EVENT_GRAPH_BINDINGS = "viskit.xsd.bindings.eventgraph";

    private SimEntity root;
    InputStream fileInputStream;
    private String fileBaseName;
    private static JAXBContext jaxbContext;
    private Unmarshaller unMarshaller;
    private Object unMarshalledObject;

    private String extendz = "";
    private String className = "";
    private String packageName = "";
    private File eventGraphFile;

    private List<Parameter> superParameterList;
    private List<Parameter> rootParameterList;
    private List<StateVariable> stateVariableList;

    /** 
     * A generator to convert Event Graph XML model into Java source 
     */
    private SimkitEventGraphXML2Java()
    {
        try {
            if (jaxbContext == null) // avoid JAXBException (perhaps due to concurrency)
                jaxbContext = JAXBContext.newInstance(EVENT_GRAPH_BINDINGS);
        } 
        catch (JAXBException ex) {
            LOG.error(ex);
            error(ex.getMessage());
        }
    }

    /** Instance that facilitates code generation via the given input stream
     *
     * @param stream the file stream to generate code from
     */
    public SimkitEventGraphXML2Java(InputStream stream) {
        this();
        fileInputStream = stream;
    }

    /**
     * Creates a new instance of SimkitXML2Java
     * when used from another class.  Instance this
     * with a String for the className of the xmlFile
     *
     * @param xmlFilePath the file to generate code from
     */
    public SimkitEventGraphXML2Java(String xmlFilePath) {
        this(ViskitStatics.classForName(SimkitEventGraphXML2Java.class.getName()).getClassLoader().getResourceAsStream(xmlFilePath));
        setFileBaseName(new File(baseNameOf(xmlFilePath)).getName());
        setEventGraphFile(new File(xmlFilePath));
    }

    public SimkitEventGraphXML2Java(File f) throws FileNotFoundException {
        this(new FileInputStream(f));
        setFileBaseName(baseNameOf(f.getName()));
        setEventGraphFile(f);
    }

    public void unmarshal() {
        try {
            setUnMarshaller(jaxbContext.createUnmarshaller());
            setUnMarshalledObject(getUnMarshaller().unmarshal(fileInputStream));
            root = (SimEntity) getUnMarshalledObject();
        } 
        catch (JAXBException ex)
        {
            // Silence attempting to unmarshal an Assembly here
            LOG.debug("Error occuring in SimkitXML2Java.unmarshal(): " + ex);
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
    public String translate() 
    {
        StringBuilder source = new StringBuilder();
        StringWriter head = new StringWriter();
        StringWriter parameters = new StringWriter();
        StringWriter stateVariables = new StringWriter();
        StringWriter accessorBlock = new StringWriter();
        StringWriter parameterMap = new StringWriter();
        StringWriter constructors = new StringWriter();
        StringWriter runBlock = new StringWriter();
        StringWriter eventBlock = new StringWriter();
        StringWriter toStringBlock = new StringWriter();
        StringWriter codeBlock = new StringWriter();

        buildHead(head);
        buildParameters(parameters, accessorBlock);
        buildStateVariables(stateVariables, accessorBlock);
        buildParameterMap(parameterMap);
        buildConstructors(constructors);
        buildEventBlock(runBlock, eventBlock);
        buildToString(toStringBlock);
        buildCodeBlock(codeBlock);

        buildSource(source, head, parameters, stateVariables, parameterMap,
                constructors, runBlock, eventBlock, accessorBlock,
                toStringBlock, codeBlock);

        return source.toString();
    }

    /** @return the base name of this Event Graph file */
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

    public final void setEventGraphFile(File xmlFile) {
        eventGraphFile = xmlFile;
    }

    void buildHead(StringWriter head)
    {
        PrintWriter pw = new PrintWriter(head);

        className         = root.getName();
        packageName       = root.getPackage();
        extendz           = root.getExtend();
        String implementz = root.getImplement();

        // TBD: should be checking the class definitions
        // of the Interfaces and create a code block
        // if none exists with template methods, and
        // Events for any "do" methods if none exists.
        if ((implementz != null) && !implementz.isBlank())
        {
            extendz += SP + "implements" + SP + implementz;
        }

        pw.println("// created using SimkitEventGraphXML2Java from " + packageName + "/" + className + ".xml");
        pw.println();
        
//        pw.println("   *** Intentional source error for testing ***"); // debug
//        pw.println();

        pw.println("package " + packageName + SC);
        pw.println();
        pw.println("// Standard library imports");
        pw.println("import java.util.*;");
        // For debugging only
//      pw.println("import org.apache.logging.log4j.LogManager;"); // TODO this needs to be supported in classpath
        pw.println();
        pw.println("// Application specific imports");

        pw.println("import simkit.*;");
        pw.println("import simkit.random.*;");
        pw.println();
        pw.println("public class " + className + SP + "extends" + SP + extendz + SP + OB);
        pw.println();
//        pw.println("static final Logger LOG = LogManager.getLogger();" );
//        pw.println();
    }

    void buildParameters(StringWriter vars, StringWriter accessorBlock) {

        PrintWriter printWriter = new PrintWriter(vars);

        rootParameterList = root.getParameter();
        superParameterList = resolveSuperParams(rootParameterList);

        // Logger instantiation (for debugging only)
//        pw.println(sp4 + "static Logger Log4jUtilities.getLogger() " + eq + " Logger" + pd +
//                "getLogger" + lp + className + pd + "class" + rp + sc);
//        pw.println();
        printWriter.println(SP_4 + "/* Simulation Parameters */");
        printWriter.println();
        for (Parameter nextParameter : rootParameterList) 
        {
            if (!superParameterList.contains(nextParameter)) 
            {
                String nextDescription = ViskitStatics.emptyIfNull(nextParameter.getDescription());
                if (!nextDescription.isEmpty()) {
                    printWriter.print(SP_4 + JDO + SP);
//                    for (String comment : p.getComment()) // obsolete
//                        printWriter.print(comment);
                    printWriter.print(nextDescription);
                    printWriter.println(SP + JDC);
                }
                printWriter.println(SP_4 + PRIVATE + SP + nextParameter.getType() + SP + nextParameter.getName() + SC);
            } else {
                printWriter.println(SP_4 + "/* inherited parameter " + nextParameter.getType() + SP + nextParameter.getName() + " */");
            }
            printWriter.println();

            if (extendz.contains(SIM_ENTITY_BASE))
                buildParameterModifierAndAccessor(nextParameter, accessorBlock);
            else if (!superParameterList.contains(nextParameter))
                buildParameterModifierAndAccessor(nextParameter, accessorBlock);
        }
        if (rootParameterList.isEmpty()) {
            printWriter.println(SP_4 + "/* None */");
            printWriter.println();
        }
    }

    void buildStateVariables(StringWriter vars, StringWriter accessorBlock) {

        PrintWriter pw = new PrintWriter(vars);

        stateVariableList = root.getStateVariable();

        pw.println(SP_4 + "/* Simulation State Variables */");
        pw.println();

        Class<?> newClass;
        Constructor<?> constructor;
        for (StateVariable nextStateVariable : stateVariableList) {

            // Non array type generics
            if (isGeneric(nextStateVariable.getType())) 
            {
                String nextDescription = ViskitStatics.emptyIfNull(nextStateVariable.getDescription());
                if (!nextDescription.isEmpty()) {
                    pw.print(SP_4 + JDO + SP);
//                    for (String comment : stateVariable.getComment()) {
//                        pw.print(comment);
//                    }
                    pw.print(nextDescription);
                    pw.println(SP + JDC);
                }
                if (!isArray(nextStateVariable.getType()))
                    pw.println(SP_4 + PROTECTED + SP + nextStateVariable.getType() + SP + nextStateVariable.getName() + SP + EQ + SP + "new" + SP + stripType(nextStateVariable.getType()) + LP + RP + SC);
                else
                    pw.println(SP_4 + PROTECTED + SP + stripLength(nextStateVariable.getType()) + SP + nextStateVariable.getName() + SC);
            } else {

                newClass = ViskitStatics.classForName(nextStateVariable.getType());

                // Non-super type, primitive, primitive[] or another type array
                if (newClass == null || ViskitGlobals.instance().isPrimitiveOrPrimitiveArray(nextStateVariable.getType())) 
                {
                    String nextDescription = ViskitStatics.emptyIfNull(nextStateVariable.getDescription());
                    if (!nextDescription.isEmpty()) {
                        pw.print(SP_4 + JDO + SP);
//                        for (String comment : stateVariable.getComment()) {
//                            pw.print(comment);
//                        }
                        pw.print(nextDescription);
                        pw.println(SP + JDC);
                    }

                    pw.println(SP_4 + PROTECTED + SP + stripLength(nextStateVariable.getType()) + SP + nextStateVariable.getName() + SC);

                } else if (!isArray(nextStateVariable.getType())) 
                {
                    String nextDescription = ViskitStatics.emptyIfNull(nextStateVariable.getDescription());
                    if (!nextDescription.isEmpty()) {
                        pw.print(SP_4 + JDO + SP);
//                        for (String comment : stateVariable.getComment()) {
//                            pw.print(comment);
//                        }
                        pw.print(nextDescription);
                        pw.println(SP + JDC);
                    }

                    // NOTE: not the best way to do this, but functions for now
                    try {
                        constructor = newClass.getConstructor(new Class<?>[]{});
                    } catch (NoSuchMethodException nsme) {
//                    LOG.error(nsme);

                        // reset
                        constructor = null;
                    }

                    if (constructor != null) {
                        pw.println(SP_4 + PROTECTED + SP + nextStateVariable.getType() + SP + nextStateVariable.getName() + SP + EQ + SP + "new" + SP + nextStateVariable.getType() + LP + RP + SC);
                    } else { // really not a bad case, most likely will be set by the reset()
                        pw.println(SP_4 + PROTECTED + SP + nextStateVariable.getType() + SP + nextStateVariable.getName() + SP + EQ + SP + "null" + SC);
                    }
                } else
                    pw.println(SP_4 + PROTECTED + SP + stripLength(nextStateVariable.getType()) + SP + nextStateVariable.getName() + SC);
            }

            buildStateVariableAccessor(nextStateVariable, accessorBlock);
            pw.println();
        }
        if (stateVariableList.isEmpty()) {
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
        for (Parameter pt : rootParameterList) {
            pw.print(QU + pt.getName() + QU);
            if (rootParameterList.indexOf(pt) < rootParameterList.size() - 1) {
                pw.print(CM);
                pw.println();
                pw.print(SP_8 + SP_4);
            }
        }
        pw.println(CB + CM);
        pw.print(SP_8 + "types =" + SP + OB);
        for (Parameter pt : rootParameterList) {
            pw.print(QU + pt.getType() + QU);
            if (rootParameterList.indexOf(pt) < rootParameterList.size() - 1) {
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
        if (superParameterList.isEmpty()) {
            pw.println(SP_4 + "/** Creates a new default instance of " + root.getName() + " */");
            pw.println(SP_4 + "public " + root.getName() + LP + RP + SP + OB);
            pw.println(SP_4 + CB);
            pw.println();
        }

        if (!rootParameterList.isEmpty()) {
            for (StateVariable st : stateVariableList) {

                // Suppress warning call to unchecked cast since we return a clone
                // of Objects vice the desired type
                if (isGeneric(st.getType()) && isArray(st.getType())) {
                    pw.println(SP_4 + "@SuppressWarnings(\"unchecked\")");
                    break;
                }
            }

            // Now, generate the parameterized consructor
            pw.print(SP_4 + "public " + root.getName() + LP);
            for (Parameter pt : rootParameterList) {

                pw.print(pt.getType() + SP + shortinate(pt.getName()));

                if (rootParameterList.size() > 1) {
                    if (rootParameterList.indexOf(pt) < rootParameterList.size() - 1) {
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
                for (Parameter pt : superParameterList) {
                    pw.print(shortinate(pt.getName()));
                    if ((superParameterList.size() > 1) && (superParameterList.indexOf(pt) < superParameterList.size() - 1)) {
                        pw.print(CM + SP);
                    }
                }
                pw.println(RP + SC);
            }

            String superParam = null;

            // skip over any sets that would get done in the superclass, or
            // call super.set*()
            Parameter pt;
            for (int l = superParameterList.size(); l < rootParameterList.size(); l++) {

                pt = rootParameterList.get(l);
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

            for (StateVariable st : stateVariableList)
                if (isArray(st.getType()))
                    pw.println(SP_8 + st.getName() + SP + EQ + SP + "new" + SP + stripGenerics(st.getType()) + SC);

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

        List<Event> events = root.getEvent();

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

        for (LocalVariable local : liLocalV)
            pw.println(SP_8 + local.getType() + SP + local.getName() + SC);

        if (!liLocalV.isEmpty()) {pw.println();}

        StateVariable sv;
        Assignment asg;
        Operation ops;
        boolean isar;
        String sps, in;
        for (StateTransition st : liStateT) {
            sv = (StateVariable) st.getState();
            asg = st.getAssignment();
            ops = st.getOperation();

            isar = isArray(sv.getType());
            sps = isar ? SP_12 : SP_8;
            in = indexFrom(st);

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

        if (!liLocalV.isEmpty())
            pw.println(SP_8 + "/* local variable decarlations */");
        for (LocalVariable local : liLocalV)
            pw.println(SP_8 + local.getType() + SP + local.getName() + SC);

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

        StateVariable sv;
        Assignment asg;
        Operation ops;
        boolean isar;
        String sps, in, stateVariableName, stateVariableGetter;
        for (StateTransition st : liStateT) {
            sv = (StateVariable) st.getState();
            asg = st.getAssignment();
            ops = st.getOperation();

            isar = isArray(sv.getType());
            sps = isar ? SP_12 : SP_8;
            in = indexFrom(st);

            if (isar) {
                pw.println(SP_8 + "for " + LP + in + SP + EQ + SP + "0; " + in + " < " + sv.getName() + PD + "length"+ SC + SP + in + "++" + RP + SP + OB);
                pw.print(sps + "fireIndexedPropertyChange" + LP + in + CM + SP + QU + sv.getName() + QU);
            } else {
                pw.print(SP_8 + "firePropertyChange" + LP + QU + sv.getName() + QU);
            }

            // Give these FPCs "getters" as arguments
            stateVariableName = capitalize(sv.getName());
            stateVariableGetter = "get" + stateVariableName + LP;

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
        LOG.debug("Event is: " + e.getName());
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

        if (doEvent != null)
            pw.println(SP_4 + "@Override");

        // Strip out name mangling artifacts imposed by the EventGraph Model.
        // This is done to keep XML happy with no identical IDREFs, but let's
        // Simkit work its magic with reflection
        String eventName = e.getName().replaceAll("_\\w+_*", "");

        pw.print(SP_4 + "public void do" + eventName + LP);

        for (Argument a : liArgs) {
            pw.print(a.getType() + SP + a.getName());
            if (liArgs.size() > 1 && liArgs.indexOf(a) < liArgs.size() - 1)
                pw.print(CM + SP);
        }

        // finish the method decl
        pw.println(RP + SP + OB);

        if (doEvent != null) {
            pw.print(SP_8 + "super." + doEvent + LP);
            for (Argument a : liArgs) {
                pw.print(a.getName());
                if (liArgs.size() > 1 && liArgs.indexOf(a) < liArgs.size() - 1)
                    pw.print(CM + SP);
            }

            // finish the super decl
            pw.println(RP + SC);
        }

        pw.println();

        if (!liLocalV.isEmpty())
            pw.println(SP_8 + "/* local variable decarlations */");

        String[] lines;
        String value;
        for (LocalVariable local : liLocalV) {
            lines = new String[] {" "};
            value = local.getValue();
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

        if (!liLocalV.isEmpty()) {
            pw.println();
        }

        if (e.getCode() != null && !e.getCode().isEmpty()) {
            pw.println(SP_8 + "/* Code insertion for Event " + eventName + " */");
            lines = e.getCode().split("\\n");
            for (String line : lines)
                pw.println(SP_8 + line);

            pw.println(SP_8 + "/* End Code insertion */");
            pw.println();
        }

        List<String> decls = new LinkedList<>();
        StateVariable sv;
        Assignment asg;
        Operation ops;
        LocalVariableAssignment lva;
        LocalVariableInvocation lvi;
        String change, olds, oldName, getter, invoke;
        for (StateTransition st : liStateT) {
            sv = (StateVariable) st.getState();
            asg = st.getAssignment();
            ops = st.getOperation();
            lva = st.getLocalVariableAssignment();
            lvi = st.getLocalVariableInvocation();
            change = "";
            olds = ""; // old decl line Bar oldFoo ...
            oldName = sv.getName(); // oldFoo

            if (ops != null)
                change = PD + ops.getMethod() + SC;
            else if (asg != null)
                change = SP + EQ + SP + asg.getValue() + SC;

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
            getter = oldName + SP + EQ + SP + "get" + oldName.substring(5) + LP;
            if ("".equals(olds))
                olds = getter;
            else
                olds += getter;

            if (isArray(sv.getType())) {
                olds += indexFrom(st);
            }
            olds += RP + SC;

            // now olds is Bar oldFoo = getFoo(<idxvar>?);
            // add this to the pre-formatted block
            olds += sv.getName() + (isArray(sv.getType()) ? LB + indexFrom(st) + RB : "") + change;
            lines = olds.split("\\;");

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
                invoke = lvi.getMethod();
                if (invoke != null && !invoke.isEmpty())
                    pw.println(SP_8 + invoke + SC);
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
        Event event = (Event) s.getEvent();

        if (s.getCondition() != null && !s.getCondition().equals("true")) {
            condent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + s.getCondition() + RP + SP + OB);
        }

        // Strip out name mangling artifacts imposed by the EventGraph Model.
        // This is done to keep XML happy with no identical IDREFs, but lets
        // Simkit work its magic with reflection
        String eventName = event.getName().replaceAll("_\\w+_*", "");

        pw.print(SP_8 + condent + "waitDelay" + LP + QU + eventName + QU + CM + SP);

        // according to schema, to meet Priority class definition, the following
        // tags should be permitted:
        // HIGHEST, HIGHER, HIGH, DEFAULT, LOW, LOWER, and LOWEST,
        // however, historically these could be numbers.

        // Bugfix 1400: These should now be eneumerations instead of FP values
        pw.print(s.getDelay() + CM + " Priority" + PD + s.getPriority());

        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        String epValue;
        for (EdgeParameter ep : s.getEdgeParameter()) {
            pw.print(CM + " (Object) ");

            epValue = ep.getValue();

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

        // Strip out name mangling artifacts imposed by the EventGraph Model.
        // This is done to keep XML happy with no identical IDREFs, but let's
        // Simkit work its magic with reflection
        String eventName = event.getName().replaceAll("_\\w+_*", "");

        pw.print(SP_8 + condent + "interrupt" + LP + QU + eventName + QU);

        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        for (EdgeParameter ep : liEdgeP)
            pw.print(CM + SP + "(Object) " + ep.getValue());

        pw.println(RP + SC);

        if (c.getCondition() != null && !c.getCondition().equals("true"))
            pw.println(SP_8 + CB);
    }

    void buildToString(StringWriter toStringBlock) {

        // Assume this is a subclass of some SimEntityBase which should already
        // have a toString()
        if (!extendz.contains(SIM_ENTITY_BASE)) {return;}

        PrintWriter pw = new PrintWriter(toStringBlock);
        pw.println(SP_4 + "@Override");
        pw.print(SP_4 + "public String toString");
        pw.println(LP + RP + SP + OB);
        pw.println(SP_8 + "return" + SP + "getName()" + SC); // <- TODO: What more info can we provide?
        pw.println(SP_4 + CB);
    }

    void buildCodeBlock(StringWriter t) {
        PrintWriter pw = new PrintWriter(t);
        String code = root.getCode();
        String[] lines;
        if (code != null) {
            pw.println(SP_4 + "/* Inserted code for " + root.getName() + " */");
            lines = code.split("\\n");
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
        return ViskitGlobals.instance().isGenericType(type);
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
            c = ViskitStatics.classForName(unqualifiedExtends.split("\\s")[0]);
        } else {
            c = ViskitStatics.classForName(extendz.split("\\s")[0]);
        }
        return c;
    }

    // find the maximal set that the subclass parameters
    // can cover of the super class's available constructors
    // note a subclass should have at least the super class's
    // parameters and maybe some more
    private List<Parameter> resolveSuperParams(List<Parameter> params) {
        List<Parameter> localSuperParams = new ArrayList<>();
        if (extendz.contains(SIM_ENTITY_BASE) || extendz.contains("BasicSimEntity"))
            return localSuperParams;

        // the extendz field may also contain an implements
        // codeBlock.

        Class<?> c = resolveExtensionClass();

        if (c != null) {
            Constructor[] ca = c.getConstructors();
            int maxIndex = 0;
            int maxParamCount = 0;
            int tmpCount;
            for (int i = 0; i < ca.length; i++) {

                // find largest fitting array of super parameters constructor
                tmpCount = (ca[i].getParameterTypes()).length;
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
            LOG.error("{} was not found on the working classpath", extendz);
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
        fromClazz = ViskitStatics.convertClassName(fromClazz);
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
            aClass = ViskitGlobals.instance().getViskitApplicationClassLoader().loadClass(c);
        } catch (ClassNotFoundException cnfe) {
//            LOG.error(cnfe);
        }

        if (aClass != null) {
            return Cloneable.class.isAssignableFrom(aClass);
        }
        return isArray(c) || isGeneric(c);
    }

    private boolean isArray(String a) {
        return ViskitGlobals.instance().isArray(a);
    }

    /** Report and exit the JVM
     *
     * @param errorDescription a description of the encountered error
     */
    private void error(String errorDescription) {
        LOG.error(errorDescription);
        System.exit(1);
    }

    /**
     * Follow this pattern to use this class from another,
     * otherwise this can be used stand alone from CLI
     *
     * @param args the command line arguments args[0] - XML file to translate
     */
    public static void main(String[] args) 
    {
        String xmlFile = args[0].replaceAll("\\\\", "/");
        LOG.info("EventGraph file is: {}", xmlFile);
        LOG.info("Generating Java Source...");

        try (InputStream is = new FileInputStream(xmlFile)) {

            SimkitEventGraphXML2Java sx2j = new SimkitEventGraphXML2Java(is);
            File baseName = new File(sx2j.baseNameOf(xmlFile));
            sx2j.setFileBaseName(baseName.getName());
            sx2j.setEventGraphFile(new File(xmlFile));
            sx2j.unmarshal();
            String dotJava = sx2j.translate();
            if (dotJava != null && !dotJava.isEmpty()) {
                LOG.info("Done.");
            } else {
                LOG.warn("Compile error on: {}", xmlFile);
                return;
            }

            // also write out the .java to a file and compile it
            // to a .class
            LOG.info("Generating Java Bytecode...");
            try {
                if (AssemblyControllerImpl.compileJavaClassFromString(dotJava) != null)
                    LOG.info("Done.");
                
            } catch (NullPointerException npe) {
                LOG.error(npe);
//            npe.printStackTrace();
            }
        } catch (IOException fnfe) {
            LOG.error(fnfe);
        }
    }
    
} // end class file SimkitEventGraphXML2Java.java