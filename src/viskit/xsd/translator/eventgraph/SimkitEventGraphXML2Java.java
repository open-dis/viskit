package viskit.xsd.translator.eventgraph;

import edu.nps.util.LogUtilities;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import org.apache.log4j.Logger;
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
    static final Logger LOG = LogUtilities.getLogger(SimkitEventGraphXML2Java.class);

    /* convenience Strings for formatting */
	/** space character */
    public final static String SP = " ";
	/** 4 space characters */
    public final static String SP_4 = SP + SP + SP + SP;
	/** 8 space characters */
    public final static String SP_8 = SP_4 + SP_4;
	/** 12 space characters */
    public final static String SP_12 = SP_8 + SP_4;
	/** open squiggly bracket */
    public final static String OB = "{";
	/** close squiggly bracket */
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
	/** period (decimal point, dot) */
    public final static String PD = ".";
	/** quote mark */
    public final static String QU = "\"";
	/** left square bracket */
    public final static String LB = "[";
	/** right square bracket */
    public final static String RB = "]";
	/** right angle bracket (greater-than sign) */
    public final static String RA = ">";
	/**left angle bracket (less-than sign) */
    public final static String LA = "<";
	/** javadoc open */
    public final static String JDO = "/**";
	/** javadoc close */
    public final static String JDC = "*/";
	/** java comment open */
    public final static String JCO = "/*";
	/** java comment close */
    public final static String JCC = "*/";
	
    public final static String PUBLIC = "public";
    public final static String PROTECTED = "protected";
    public final static String PRIVATE = "private";
    public final static String SIM_ENTITY_BASE = "SimEntityBase";
    public final static String EVENT_GRAPH_BINDINGS = "viskit.xsd.bindings.eventgraph";
	public final static String SPACER_BAR = "======";

    private SimEntity root;
    InputStream fileInputStream;
    private String fileBaseName;
    JAXBContext jaxbContext;
    private Unmarshaller unMarshaller;
    private Object unMarshalledObject;

    private String extendz = "";
    private String className = "";
    private String packageName = "";
    private File eventGraphFile;

    private List<Parameter>   superParametersList;
    private List<Parameter>        parametersList;
    private List<StateVariable> stateVariableList;
	private boolean hasImplicitStateVariable;

    /** Default to initialize the JAXBContext only */
    private SimkitEventGraphXML2Java()
	{
        try {
            jaxbContext = JAXBContext.newInstance(EVENT_GRAPH_BINDINGS);
        } 
		catch (JAXBException ex) 
		{
            LOG.error("error initializing jaxbContext", ex);
            error(ex.getMessage());
        }
    }

    /** Instance that facilitates source code generation via the given input stream
     *
     * @param stream the file stream to generate source code from
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
     * @param xmlFile the file to generate source code from
     */
    public SimkitEventGraphXML2Java(String xmlFile) {
        this(ViskitStatics.ClassForName(SimkitEventGraphXML2Java.class.getName()).getClassLoader().getResourceAsStream(xmlFile));
        setFileBaseName(new File(baseNameOf(xmlFile)).getName());
        setEventGraphFile(new File(xmlFile));
    }

    public SimkitEventGraphXML2Java(File f) throws FileNotFoundException {
        this(new FileInputStream(f));
        setFileBaseName(baseNameOf(f.getName()));
        setEventGraphFile(f);
    }

    public void unmarshal()
	{
        try 
		{
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            setUnMarshaller(jaxbUnmarshaller);
			
			// https://dersteps.wordpress.com/2012/08/22/enable-jaxb-event-handling-to-catch-errors-as-they-happen
			jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() 
			{
				@Override
				public boolean handleEvent(ValidationEvent validationEvent) 
				{
					System.out.println("Unmarshaller event handler says: " + validationEvent.getMessage() + 
									   " (Exception: " + validationEvent.getLinkedException() + ")");
					return false;
				}
			});
            setUnMarshalledObject(getUnMarshaller().unmarshal(fileInputStream));
            root = (SimEntity) getUnMarshalledObject();
        } catch (JAXBException ex) 
		{
            LOG.debug("Error occurring during Event Graph unmarshal()", ex);
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
        StringBuilder sourceStringBuilder = new StringBuilder();
		
        StringWriter           headStringWriter = new StringWriter();
        StringWriter     parametersStringWriter = new StringWriter();
        StringWriter stateVariablesStringWriter = new StringWriter();
        StringWriter   parameterMapStringWriter = new StringWriter();
        StringWriter  accessorBlockStringWriter = new StringWriter();
        StringWriter   constructorsStringWriter = new StringWriter();
        StringWriter       runBlockStringWriter = new StringWriter();
        StringWriter     eventBlockStringWriter = new StringWriter();
        StringWriter  toStringBlockStringWriter = new StringWriter();
        StringWriter      codeBlockStringWriter = new StringWriter();

        buildHead          (headStringWriter);
        buildStateVariables(stateVariablesStringWriter, accessorBlockStringWriter);
        buildParameters    (parametersStringWriter,     accessorBlockStringWriter);
        buildParameterMap  (parameterMapStringWriter); // immediately following parameter definitions
        buildConstructors  (constructorsStringWriter);
        buildEventBlock    (runBlockStringWriter, eventBlockStringWriter);
        buildToString      (toStringBlockStringWriter);
        buildCodeBlock     (codeBlockStringWriter);

        buildSource(sourceStringBuilder, 
				    headStringWriter, 
					stateVariablesStringWriter,
					parametersStringWriter,
					parameterMapStringWriter,
					constructorsStringWriter, 
					runBlockStringWriter, 
					eventBlockStringWriter, 
					accessorBlockStringWriter,
					toStringBlockStringWriter, 
					codeBlockStringWriter);

        return sourceStringBuilder.toString();
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

    void buildHead(StringWriter head) 
	{
        PrintWriter pw = new PrintWriter(head);

		// TODO license, other metadata

          className = root.getName();
        packageName = root.getPackage();
            extendz = root.getExtend();
        String implementz = root.getImplement();

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
		String description = root.getDescription().trim();
		if (!description.isEmpty())
		{
			pw.println(JDO + SP + description);
			pw.println(SP + SP + JDC);
		}
        pw.println(PUBLIC + " class " + className + SP + "extends" + SP + extendz);
        pw.println(OB);
    }

    void buildParameters(StringWriter parametersStringWriterHolder, StringWriter accessorBlockStringWriterHolder)
	{
        PrintWriter pw = new PrintWriter(parametersStringWriterHolder);

             parametersList = root.getParameter();
        superParametersList = resolveSuperParams(parametersList);

        // Logger instantiation (for debugging only)
//        pw.println(sp4 + "static Logger LogUtils.getLogger() " + eq + " Logger" + pd +
//                "getLogger" + lp + className + pd + "class" + rp + sc);
//        pw.println();

        pw.print(SP_4 + "/* " + SPACER_BAR + " Event Graph Initialization Parameters");
        if (parametersList.isEmpty()) 
		{
            pw.println(": none " + SPACER_BAR + " */");
        }
		else
		{
			pw.println(" " + SPACER_BAR + " */");
        }
        pw.println();
		
		// this output carefully directed to precede accessor block for initialization parameters
		PrintWriter abpw = new PrintWriter (accessorBlockStringWriterHolder);
		abpw.print(SP_4 + JCO + SP + SPACER_BAR + SP + "Initialization Parameter accessors: ");
		if (superParametersList.isEmpty() && parametersList.isEmpty()) 
		{
			abpw.println("none" + SP + SPACER_BAR + SP + JCC);
		}
		else
		{
			abpw.println("read and write methods to set values" + SP + SPACER_BAR + SP + JCC);
		}
		abpw.println();
		
        for (Parameter p : parametersList) 
		{
            if (!superParametersList.contains(p)) 
			{
				String description = p.getDescription().trim();
				if (!description.isEmpty()) 
				{
					pw.println(SP_4 + JDO + SP + description + SP + JDC);
				}
                pw.println(SP_4 + PRIVATE + SP + p.getType() + SP + p.getName() + SC);
            } 
			else 
			{
                pw.println(SP_4 + "/* inherited parameter " + p.getType() + SP + p.getName() + " */");
            }
            pw.println();

            if (extendz.contains(SIM_ENTITY_BASE)) 
			{
                buildParameterModifierAndAccessor(p, accessorBlockStringWriterHolder);
            } 
			else if (!superParametersList.contains(p)) 
			{
                buildParameterModifierAndAccessor(p, accessorBlockStringWriterHolder);
            }
        }
    }

    void buildStateVariables(StringWriter stateVariableWriterHolder, StringWriter accessorBlockWriterHolder)
	{
        PrintWriter pw = new PrintWriter(stateVariableWriterHolder);

        stateVariableList = root.getStateVariable();

        pw.print(SP_4 + JCO + SP + SPACER_BAR + SP + "Event Graph State Variables");
        if (stateVariableList.isEmpty())
		{
            pw.println(": none " + SPACER_BAR + SP + JDC);
        }
		else
		{
            pw.println(" " + SPACER_BAR + SP + JDC);
        }
        pw.println();

        Class<?> c;
        Constructor<?> cst;
        for (StateVariable stateVariable : stateVariableList)
		{
			String implicitStatus = "";
			String description = stateVariable.getDescription().trim();
			
			if (stateVariable.isImplicit())
			{
				hasImplicitStateVariable = true;
				implicitStatus = stateVariable.getName() + " is an implicit state variable whose value is computed from other state variables"  + "\n" +
								 SP_8 + "  *  Be sure to provide a corresponding method in the event graph code block: compute_" + stateVariable.getName() + "()";
			}
			if (!description.isEmpty() || stateVariable.isImplicit())
			{
				String lineBreak = "";
				if (!description.isEmpty() && stateVariable.isImplicit())
					   lineBreak = "\n" + SP_8 + "  *  ";
				pw.println(SP_4 + JDO + SP + description + lineBreak + implicitStatus);
				pw.println(SP_4 + JDC);
			}
				
			String implicitInlineComment = "";
			if (stateVariable.isImplicit())
				   implicitInlineComment = " // implicit state variable";
				
            // Non array type generics
            if (isGeneric(stateVariable.getType()))
			{
				
                if (!isArray(stateVariable.getType()))
                    pw.println(SP_4 + PROTECTED + SP + stateVariable.getType() + SP + stateVariable.getName() + SP + EQ + SP + "new" + SP + stripType(stateVariable.getType()) + LP + RP + SC + implicitInlineComment);
                else
                    pw.println(SP_4 + PROTECTED + SP + stripLength(stateVariable.getType()) + SP + stateVariable.getName() + SC + implicitInlineComment);
            } 
			else 
			{
				pw.println(SP_4 + PROTECTED + SP + stripLength(stateVariable.getType()) + SP + stateVariable.getName() + SC + implicitInlineComment);
					
                c = ViskitStatics.ClassForName(stateVariable.getType());

                // Non-super type, primitive, primitive[] or another type array
                if (c == null || ViskitGlobals.instance().isPrimitiveOrPrimitiveArray(stateVariable.getType())) 
				{
					// TODO previously javadoc here; anything else still needed?
                } 
				else if (!isArray(stateVariable.getType())) 
				{
                    // NOTE: not the best way to do this, but functions for now
                    try {
                        cst = c.getConstructor(new Class<?>[]{});
                    } catch (NoSuchMethodException nsme) {
//                    LOG.error(nsme);

                        // reset
                        cst = null;
                    }

                    if (cst != null) {
                        pw.println(SP_4 + PROTECTED + SP + stateVariable.getType() + SP + stateVariable.getName() + SP + EQ + SP + "new" + SP + stateVariable.getType() + LP + RP + SC);
                    } else { // really not a bad case, most likely will be set by the reset()
                        pw.println(SP_4 + PROTECTED + SP + stateVariable.getType() + SP + stateVariable.getName() + SP + EQ + SP + "null" + SC);
                    }
                } else
                    pw.println(SP_4 + PROTECTED + SP + stripLength(stateVariable.getType()) + SP + stateVariable.getName() + SC);
            }
			if (stateVariableList.indexOf(stateVariable) == 0)
			{
				// this output carefully directed to precede accessor block for state variables
				PrintWriter abpw = new PrintWriter(accessorBlockWriterHolder);
				abpw.print(SP_4 + JCO + SP + SPACER_BAR + SP + "Event Graph State Variables");
				if (stateVariableList.isEmpty())
				{
					abpw.println(" accessors: none" + SP + SPACER_BAR + SP + JCC);
				}
				else
				{
					abpw.println(": only read accessor methods provided" + SP + SPACER_BAR + SP + JCC);
					abpw.println(SP_4 + JCO + SP + SPACER_BAR + SP  + SP + "(only Event State Transitions can change State Variable values)" + SP + SPACER_BAR + SP + JCC);
					abpw.println();
				}
			}
            buildStateVariableAccessor(stateVariable, accessorBlockWriterHolder);
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

    void buildParameterModifierAndAccessor(Parameter p, StringWriter sw)
	{
        // Don't duplicate any super setters
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

        pw.print  (SP_4 + PUBLIC + " final void set" + capitalize(p.getName()) + SP + LP);
        pw.println(p.getType() + SP + "new" + capitalize(p.getName()) + RP);
        pw.println(SP_4 + OB);
        pw.print(SP_8 + "this" + PD + p.getName() + SP + EQ + SP);

        if (isArray(p.getType()) || isGeneric(p.getType())) 
		{
            pw.print  ("new" + capitalize(p.getName()));
            pw.println(PD + "clone" + LP + RP + SC);
        } 
		else 
		{
            pw.println("new" + capitalize(p.getName()) + SC);
        }
        pw.println(SP_4 + CB);
        pw.println();

        /* TODO also provide indexed getters, may be multidimensional, however,
         * not expected to actually be multidimensional
         */
        if (isArray(p.getType())) 
		{
            int d = dims(p.getType());

            pw.println(SP_4 + PUBLIC + SP + baseOf(p.getType()) + SP + "get" + capitalize(p.getName()) + SP + LP + indxncm(d) + RP);
            pw.println(SP_4 + OB);
            pw.println(SP_8 + "return" + SP + p.getName() + indxbr(d) + SC);
            pw.println(SP_4 + CB);
            pw.println();
        }

        pw.println(SP_4 + PUBLIC + SP + p.getType() + SP + "get" + capitalize(p.getName()) + SP + LP + RP);
        pw.println(SP_4 + OB);
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

    void buildStateVariableAccessor(StateVariable stateVariable, StringWriter sw)
	{
        PrintWriter pw = new PrintWriter(sw);
		
        String cloneString = "";
        String  typeString = "";

        // check for cloneable
        if (isCloneable(stateVariable.getType())) 
		{
            cloneString = ".clone()";

            if (!isArray(stateVariable.getType()) || isGeneric(stateVariable.getType())) 
			{
                typeString = LP + stripLength(stateVariable.getType()) + RP;
            }

            // Supress warning call to unchecked cast since we return a clone
            // of Objects vice the desired type
            if (isGeneric(stateVariable.getType())) 
			{
                pw.println(SP_4 + "@SuppressWarnings(\"unchecked\")");
            }
        }

        if (isArray(stateVariable.getType()))
		{
            int d = dims(stateVariable.getType());
            pw.print  (SP_4 + PUBLIC + SP + baseOf(stateVariable.getType()) + SP + "get" + capitalize(stateVariable.getName()));
            pw.println(SP + LP + indxncm(d) + RP);
            pw.println(SP_4 + OB);
			if (stateVariable.isImplicit())
			{
				pw.println(SP_8 + "// implicit state variables are computed from other state variables");
				pw.println(SP_8 + "compute_" + stateVariable.getName() + SP + "();");
				pw.println(); 
			}
            pw.println(SP_8 + "return" + SP + stateVariable.getName() + indxbr(d) + SC);
            pw.println(SP_4 + CB);
            pw.println();
        } 
		else 
		{
            pw.print  (SP_4 + PUBLIC + SP + stripLength(stateVariable.getType()) + SP + "get" + capitalize(stateVariable.getName()));
            pw.println(SP + LP + RP);
            pw.println(SP_4 + OB);
			if (stateVariable.isImplicit())
			{
				pw.println(SP_8 + "// implicit state variables are computed from other state variables");
				pw.println(SP_8 + "compute_" + stateVariable.getName() + SP + "();");
				pw.println();
			}
            pw.println(SP_8 + "return" + SP + (typeString + SP + stateVariable.getName() + cloneString).trim() + SC);
            pw.println(SP_4 + CB);
            pw.println();
        }
    }

    void buildParameterMap(StringWriter parameterMap) {
        PrintWriter pw = new PrintWriter(parameterMap);

        pw.println(SP_4 + "@viskit.ParameterMap" + SP + LP);
        pw.print(SP_8 + "names =" + SP + OB);
        for (Parameter parameter : parametersList)
		{
			pw.println();
			pw.print(SP_8 + SP_4);
            pw.print(QU + parameter.getName() + QU);
            if (parametersList.indexOf(parameter) < parametersList.size() - 1) 
			{
				pw.print(CM);
            }
        }
        pw.println(CB + CM);
        pw.print(SP_8 + "types =" + SP + OB);
        for (Parameter parameter : parametersList) 
		{
			pw.println();
			pw.print(SP_8 + SP_4);
            pw.print(QU + parameter.getType() + QU);
            if (parametersList.indexOf(parameter) < parametersList.size() - 1) 
			{
                pw.print(CM);
            }
        }
        pw.println(CB + CM);
        pw.print(SP_8 + "descriptions =" + SP + OB);
        for (Parameter parameter : parametersList) 
		{
			pw.println();
			pw.print(SP_8 + SP_4);
            pw.print(QU + parameter.getDescription() + QU);
            if (parametersList.indexOf(parameter) < parametersList.size() - 1) 
			{
                pw.print(CM);
            }
        }
        pw.println(CB);
        pw.println(SP_4 + RP);
        pw.println();
    }

    void buildConstructors(StringWriter constructors) {

        PrintWriter pw = new PrintWriter(constructors);

		pw.println(SP_4 + JDO + SP + "No-parameter constructor creates a new default instance of " + root.getName() + " event graph.");
        if (!parametersList.isEmpty() || !superParametersList.isEmpty()) 
		{
			pw.println(SP_4 + SP_4 + "When used, typically followed by individual setting of parameter value initializations.");
		}
		pw.println(SP_4 + SP  + SP + JDC);

        // Generate a zero parameter (default) constructor in addition to a
        // parameterized constructor if we are not an extension
        if (superParametersList.isEmpty()) 
		{
            pw.println(SP_4 + PUBLIC + SP + root.getName() + LP + RP + SP + OB);
            pw.println(SP_4 + CB);
            pw.println();
        }

        if (!parametersList.isEmpty()) 
		{
            for (StateVariable stateVariable : stateVariableList)
			{
                // Suppress warning call to unchecked cast since we return a clone
                // of Objects vice the desired type
                if (isGeneric(stateVariable.getType()) && isArray(stateVariable.getType())) 
				{
                    pw.println(SP_4 + "@SuppressWarnings(\"unchecked\")");
                    break;
                }
            }
            
			// Event graph constructor javadoc
			pw.println(SP_4 + JDO + SP + "All-parameter constructor creates a new instance of " + root.getName() + " event graph.");
            pw.println(SP_4 + " * Warning: if more than one parameters have compatible types, be sure to invoke them in the correct order!");
			for (Parameter parameter : parametersList)
			{
				pw.print(SP_4 + " * @param " + parameter.getName() + SP + parameter.getDescription());
				pw.println();
			}
			pw.println(SP_4 + SP + JDC);

            // Now, generate the parameterized consructor
            pw.print(SP_4 + PUBLIC + SP + root.getName() + SP + LP);
            for (Parameter parameter : parametersList)
			{
				pw.println();
				pw.print(SP_8);
                pw.print(parameter.getType() + SP + shortinate(parameter.getName()));

                if ((parametersList.size() > 1) && (parametersList.indexOf(parameter) < parametersList.size() - 1)) 
				{
					pw.print(CM);
				}
            }
            pw.println(RP);
			pw.println(SP_4 + OB);

            Method[] methods = null;

            // check for any super params for this constructor
            if (!extendz.contains(SIM_ENTITY_BASE)) {

                Class<?> sup = resolveExtensionClass();
                methods = sup.getMethods();

                pw.print(SP_8 + "super" + LP);
                for (Parameter pt : superParametersList) {
                    pw.print(shortinate(pt.getName()));
                    if ((superParametersList.size() > 1) && (superParametersList.indexOf(pt) < superParametersList.size() - 1)) {
                        pw.print(CM + SP);
                    }
                }
                pw.println(RP + SC);
            }

            String superParam = null;

            // skip over any sets that would get done in the superclass, or
            // call super.set*()
            for (int l = superParametersList.size(); l < parametersList.size(); l++) {

                Parameter pt = parametersList.get(l);
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

            for (StateVariable stateVariable : stateVariableList) 
			{
                if (isArray(stateVariable.getType())) 
				{
                    pw.println(SP_8 + stateVariable.getName() + SP + EQ + SP + "new" + SP + stripGenerics(stateVariable.getType()) + SC);
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
    private String stripGenerics(String type) 
	{
        int left, right;
        if (!isGeneric(type)) 
		{
            return type;
        }
        left = type.indexOf(LA);
        right = type.indexOf(RA);
        return type.substring(0, left) + type.substring(right + 1);
    }

    void buildEventBlock(StringWriter runBlockStringWriter, StringWriter eventBlockStringWriter) {

        List<Event> events = root.getEvent();

        for (Event event : events)
		{
            if (event.getName().equals("Run")) // reserved event name
			{
                doResetBlock(event, runBlockStringWriter);
                  doRunBlock(event, runBlockStringWriter);
            } 
			else
			{
                doEventBlock(event, eventBlockStringWriter);
            }
        }
    }

    void doResetBlock(Event run, StringWriter runBlock) 
	{
        PrintWriter pw = new PrintWriter(runBlock);
        List<LocalVariable>     localVariableList = run.getLocalVariable();
        List<StateTransition> stateTransitionList = run.getStateTransition(); // list does not include implicit state variables

		pw.println(SP_4 + JDO + SP + "Reset state variables back to initial values for " + root.getName() + " event graph.");
		pw.println(SP_4 + SP + JDC);
        pw.println(SP_4 + "@Override");
        pw.println(SP_4 + PUBLIC + " void reset ()");
        pw.println(SP_4 + OB);
        pw.println(SP_8 + "super.reset()" + SC);

        if (!localVariableList.isEmpty()) 
		{
            pw.println();
            pw.println(SP_8 + "/* local variable declarations */");
        }

        for (LocalVariable localVariable : localVariableList) 
		{
            pw.println(SP_8 + localVariable.getType() + SP + localVariable.getName() + SC);
        }

        if (!localVariableList.isEmpty()) 
		{
			pw.println();
		}

        for (StateTransition stateTransition : stateTransitionList)
		{
            StateVariable stateVariable = (StateVariable) stateTransition.getState();
            Assignment    assignment    = stateTransition.getAssignment();
            Operation     operation     = stateTransition.getOperation();
 
            boolean isArray = isArray(stateVariable.getType());
            String spaces = isArray ? SP_12 : SP_8;
            String in = indexFrom(stateTransition);

            if (stateVariable.isImplicit()) 
			{
                pw.println(SP_8 + "// " + stateVariable.getName() + " is implicit and thus gets computed, not initialized");
                continue;
            } 

            if (isArray) 
			{
                pw.println(SP_8 + "for " + LP + in + SP + EQ + SP + "0; " + in + " < " + stateVariable.getName() + PD + "length"+ SC + SP + in + "++" + RP + SP + OB);
                pw.print(spaces + stateVariable.getName() + LB + in + RB);
            } 
			else 
			{
                pw.print(spaces + stateVariable.getName());
            }

            if (operation != null) 
			{
                pw.println(PD + operation.getMethod() + SC);
            } 
			else if (assignment != null) 
			{
                pw.println(SP + EQ + SP + assignment.getValue() + SC);
            }

            if (isArray) 
			{
                pw.println(SP_8 + CB);
            }
        }
        pw.println(SP_4 + CB);
        pw.println();
    }

    void doRunBlock(Event run, StringWriter runBlock) 
	{
        PrintWriter pw = new PrintWriter(runBlock);
        List<LocalVariable>    localVariableList = run.getLocalVariable();
        List<Object> scheduleOrCancelingEdgeList = run.getScheduleOrCancel();
		
		pw.println(SP_4 + JDO + SP + "The Run event bootstraps the first simulation event in the event graph.");
		pw.println(SP_4 + SP + JDC);

        // check if any super has a doRun()
        String doRun = null;
        if (!extendz.contains(SIM_ENTITY_BASE))
		{
            Class<?> superClass = resolveExtensionClass();
            Method[] superClassMethods = superClass.getMethods();
            for (Method superClassMethod : superClassMethods) 
			{
                if ("doRun".equals(superClassMethod.getName()) && superClassMethod.getParameterCount() == 0) 
				{
                     doRun = superClassMethod.getName(); // found doRun method, save that same name here
                     break;
                }
            }
        }
        if (doRun != null)
		{
            pw.println(SP_4 + "@Override");
            pw.println(SP_4 + PUBLIC + " void " + doRun + LP + RP + SP + OB);
            pw.println(SP_8 + "super." + doRun + LP + RP + SC);
        }
		else 
		{
            pw.println(SP_4 + PUBLIC + " void doRun" + LP + RP);
			pw.println(SP_4 + OB);
        }
        if (!localVariableList.isEmpty())
		{
            pw.println(SP_8 + "/* local variable declarations (for this event only) */");
			
			for (LocalVariable local : localVariableList) 
			{
				pw.println(SP_8 + local.getType() + SP + local.getName() + SC);
			}
        }

		// cannot rename jaxb method name for run.getCode() without modifying Code element name in simkit.xsd assembly.xsd schemas
        if (run.getCode() != null && !run.getCode().isEmpty()) 
		{
			pw.println();
            pw.println(SP_8 + "/* ====== Code Block insertion for " + run.getName() + " event graph ====== */");
            String[] lines = run.getCode().split("\\n");
            for (String line : lines)
			{
                pw.println(SP_8 + line);
            }
			for (StateVariable stateVariable : stateVariableList)
			{
				if (stateVariable.isImplicit() && !run.getCode().contains("compute_" + stateVariable.getName()))
				{
					// similar code block found in StateVariableDialog.unloadWidgets()
					pw.println();
					pw.println(SP_8 + JDO + SP + "Implicit state variable computation" + SP + JDC);
					pw.println(SP_8 + "private void compute_" + stateVariable.getName() + "()");
					pw.println(SP_8 + OB);
					pw.println(SP_8 + SP_4 + "// define a code block to insert computation code here");
					pw.println(SP_8 + SP_4 + stateVariable.getName() + SP + EQ + SP + "__TODO__" + "; // override using code block");
					pw.println(SP_8 + CB);
					pw.println();
				}
			}
            pw.println(SP_8 + "/* Code Block insertion complete */");
            pw.println();
        }

        List<StateTransition> stateTransitionList = run.getStateTransition();
		
		if (stateTransitionList.isEmpty())
		{
			pw.println(SP_8 + JCO + "no state transition for this event" + SP + JCC);
		}
        for (StateTransition stateTransition : stateTransitionList)
		{
            StateVariable stateVariable = (StateVariable) stateTransition.getState();
            Assignment    assignment    = stateTransition.getAssignment();
            Operation     operation     = stateTransition.getOperation();

            boolean isArray = isArray(stateVariable.getType());
            String spacing  = isArray ? SP_12 : SP_8;
            String in = indexFrom(stateTransition);

            if (isArray)
			{
                pw.println(SP_8 + "for " + LP + in + SP + EQ + SP + "0; " + in + " < " + stateVariable.getName() + PD + "length"+ SC + SP + in + "++" + RP + SP + OB);
                pw.print(spacing + "fireIndexedPropertyChange" + LP + in + CM + SP + QU + stateVariable.getName() + QU);
            } 
			else 
			{
                pw.print(SP_8 + "firePropertyChange" + LP + QU + stateVariable.getName() + QU);
            }
            // Provide these FPCs "getters" as arguments
            String stateVariableName = capitalize(stateVariable.getName());
            String stateVariableGetter = "get" + stateVariableName + LP;

            if (isArray)
			{
                if (operation != null)
				{
                    stateVariableGetter += RP + PD + operation.getMethod();
                } 
				else if (assignment != null) 
				{
                    stateVariableGetter += in + RP;
                }
            } 
			else 
			{
                stateVariableGetter += RP;
            }
            pw.println(CM + SP + stateVariableGetter + RP + SC + SP + "// report change to listeners");

            if (isArray) 
			{
                pw.println(SP_8 + CB);
            }
			if (!stateTransitionList.isEmpty() && 
				(stateTransitionList.indexOf(stateTransition) < stateTransitionList.size() - 1)) 
			{
				pw.println();
			}
        }
		
		if (scheduleOrCancelingEdgeList.isEmpty())
		{
			if (!stateTransitionList.isEmpty())
				pw.println();
			pw.println(SP_8 + JCO + SP + "no edges are attached to this event for event scheduling or event cancellation" + SP + JCC);
		}
        for (Object scheduleOrCancelingEdge : scheduleOrCancelingEdgeList)
		{
            if (scheduleOrCancelingEdge instanceof Schedule)
			{
                doSchedule((Schedule) scheduleOrCancelingEdge, run, pw);
            } 
			else 
			{
                doCancel((Cancel) scheduleOrCancelingEdge, run, pw);
            }
			if (scheduleOrCancelingEdgeList.indexOf(scheduleOrCancelingEdge) < scheduleOrCancelingEdgeList.size() - 1)
				pw.println();
        }
        pw.println(SP_4 + CB);
        pw.println();
    }

    /** These Events should now be any other than the Run, or Reset events
     *
     * @param event the Event to process
     * @param eventBlockStringWriter the StringWriter assigned to write the Event
     */
    void doEventBlock(Event event, StringWriter eventBlockStringWriter)
	{
        LOG.debug("Event is: " + event.getName());
        PrintWriter pw = new PrintWriter(eventBlockStringWriter);
        List<StateTransition> stateTransitionList = event.getStateTransition();
        List<Argument>               argumentList = event.getArgument();
        List<LocalVariable>     localVariableList = event.getLocalVariable();
        List<Object>         scheduleOrCancelList = event.getScheduleOrCancel();

        String doEvent = null;

        // check if any super has a doEventName()
        if (!extendz.contains(SIM_ENTITY_BASE)) {

            Class<?> sup = resolveExtensionClass();
            Method[] methods = sup.getMethods();
            for (Method m : methods) {
                if (("do"+event.getName()).equals(m.getName()) && m.getParameterCount() == argumentList.size()) {
                    doEvent = m.getName();
                    break;
                }
            }
        }

        if (doEvent != null) 
		{
            pw.println(SP_4 + "@Override");
        }

        // Strip out name mangling artifacts imposed by the EventGraph Model.
        // This is done to keep XML happy with no identical IDREFs, but let's
        // Simkit work its magic with reflection
        String eventName = event.getName().replaceAll("_\\w+_*", "");

		// Produce the event javadoc
		pw.print  (SP_4 + JDO + SP + "Perform the " + eventName + " event. ");
		String description = event.getDescription().trim();
		if (!description.isEmpty())
		{
			pw.print(description);
			if (!description.endsWith("."))
				pw.print(".");
		}
		pw.println();
        for (Argument argument : argumentList) 
		{
            pw.println(SP_4 + " * param " + argument.getName() + SP + argument.getDescription().trim());
		}
		pw.println(SP_4 + SP + JDC);
		
		// Produce the event source
        pw.print  (SP_4 + PUBLIC + " void do" + eventName + LP);

        for (Argument argument : argumentList) 
		{
            pw.print(argument.getType() + SP + argument.getName());
            if (argumentList.size() > 1 && argumentList.indexOf(argument) < argumentList.size() - 1)
			{
                pw.print(CM + SP);
            }
        }
        // finish the method declaration
        pw.println(RP);
        pw.println(SP_4 + OB);

        if (doEvent != null) 
		{
            pw.print(SP_8 + "super." + doEvent + LP);
            for (Argument a : argumentList) {
                pw.print(a.getName());
                if (argumentList.size() > 1 && argumentList.indexOf(a) < argumentList.size() - 1) {
                    pw.print(CM + SP);
                }
            }
            // finish the super declaration
            pw.println(RP + SC);
        }

        if (!localVariableList.isEmpty()) 
		{
            pw.println(SP_8 + "/* local variable declarations */");
			
			for (LocalVariable local : localVariableList) 
			{
				String[] lines = {" "};
				String value = local.getValue();
				if (!("".equals(value))) 
				{
					lines = value.split("\\;");
				}
				pw.print(SP_8 + local.getType() + SP + local.getName() + SP + EQ);

				// reduce redundant casts
				pw.println(SP + lines[0].trim() + SC);
				for (int i = 1; i < lines.length; i++) 
				{
					pw.println(SP_8 + lines[i].trim() + SC);
				}
			}
			if (localVariableList.size() > 0) 
			{
				pw.println();
			}
        }

        if (event.getCode() != null && !event.getCode().isEmpty()) // cannot rename jaxb method name without modifying simkit.xsd assembly.xsd schemas
		{
            pw.println(SP_8 + "/* Code Block insertion for Event " + eventName + " */");
            String[] lines = event.getCode().split("\\n"); // cannot rename jaxb method name without modifying simkit.xsd assembly.xsd schemas
            for (String line : lines) {
                pw.println(SP_8 + line);
            }
            pw.println(SP_8 + "/* Code Block insertion complete */");
            pw.println();
        }

        List<String> decls = new LinkedList<>();
        for (StateTransition stateTransition : stateTransitionList)
		{
            StateVariable stateVariable = (StateVariable) stateTransition.getState();
            Assignment    assignment    = stateTransition.getAssignment();
            Operation     operation     = stateTransition.getOperation();
            LocalVariableAssignment lva = stateTransition.getLocalVariableAssignment();
            LocalVariableInvocation lvi = stateTransition.getLocalVariableInvocation();
            String change  = "";
            String olds    = ""; // old decl line Bar oldFoo ...
            String oldName = stateVariable.getName(); // oldFoo
			
            if (operation != null) 
			{
                change = PD + operation.getMethod() + SC;
            } 
			else if (assignment != null)  
			{
                change = SP + EQ + SP + assignment.getValue() + SC;
            }
			final String PRIOR = "_prior_";
            oldName = PRIOR + capitalize(oldName);
            if (!decls.contains(oldName)) 
			{
                olds = stateVariable.getType();
                decls.add(oldName);

                if (isArray(olds)) 
				{
                    String[] baseName;
                    baseName = olds.split("\\[");
                    olds = baseName[0];
                }
                olds += SP;
            }

            // by now, olds is "Bar" ( not Bar[] )
            // or nothing if already Decld
            // now build up "Bar oldFoo = getFoo()"
            String getter = oldName + SP + EQ + SP + "get" + oldName.substring(PRIOR.length()) + LP; // PRIOR.length() restores original name
            if ("".equals(olds))
			{
                olds = getter;
            } 
			else 
			{
                olds += getter;
            }

            if (isArray(stateVariable.getType())) 
			{
                olds += indexFrom(stateTransition);
            }
            olds += RP + SC;

            // now olds is Bar oldFoo = getFoo(<idxvar>?);
            // add this to the pre-formatted block
            olds += stateVariable.getName() + (isArray(stateVariable.getType()) ? LB + indexFrom(stateTransition) + RB : "") + change;
            String[] lines = olds.split("\\;");

            // format it
            for (int i = 0; i < lines.length; i++)
			{
                if (i == 0) 
				{
                    pw.println(SP_8 + JCO + "StateTransition for state variable " + stateVariable.getName() + SP + JCC);
                    pw.println(SP_8 + lines[i] + SC);
                }
				else 
				{
                    // Account for local assignment to accomodate state transition
                    if (lva != null && !lva.getValue().isEmpty())
                        pw.println(SP_8 + lva.getValue() + SP + EQ + SP + lines[i] + SC);
                    else
                        pw.println(SP_8 + lines[i] + SC);
                }
            }
            if (isArray(stateVariable.getType())) 
			{
                pw.print(SP_8 + "fireIndexedPropertyChange" + LP + indexFrom(stateTransition));
                pw.print(CM + SP + QU + stateVariable.getName() + QU + CM + SP);
                pw.println(oldName + CM + SP + "get" + oldName.substring(PRIOR.length()) + LP + indexFrom(stateTransition) + RP + RP + SC + SP + "// report change to listeners");
            }
			else 
			{
                pw.print(SP_8 + "firePropertyChange" + LP + QU + stateVariable.getName() + QU + CM + SP);
                pw.println(oldName + CM + SP + "get" + oldName.substring(PRIOR.length()) + LP + RP + RP + SC + SP + "// report change to listeners");
            }

            // Now, print out any any void return type, zero parameter methods
            // as part of this state transition
            if (lvi != null) 
			{
                String invoke = lvi.getMethod();
                if (invoke != null && !invoke.isEmpty()) 
				{
                    pw.println(SP_8 + invoke + SC);
                }
            }
            pw.println();
        }

        // schedule (waitDelay) and cancel (interrupt) invocations
        for (Object scheduleOrCancelObject : scheduleOrCancelList)
		{
            if (scheduleOrCancelObject instanceof Schedule)
			{
                doSchedule((Schedule) scheduleOrCancelObject, event, pw);
            } 
			else
			{
                doCancel((Cancel) scheduleOrCancelObject, event, pw);
            }
			if ((scheduleOrCancelList.size() > 1) && scheduleOrCancelList.indexOf(scheduleOrCancelObject) < scheduleOrCancelList.size() - 1)
			{
				pw.println();
			}
        }
		pw.println(SP_4 + CB);
		pw.println();
    }

    void doSchedule(Schedule jaxbSchedule, Event e, PrintWriter pw)
	{
        String conditionalIndent = "";
        Event event = (Event) jaxbSchedule.getEvent();

        // Strip out name mangling artifacts imposed by the EventGraph Model.
        // This is done to keep XML happy with no identical IDREFs, but lets
        // Simkit work its magic with reflection
        String eventName = event.getName().replaceAll("_\\w+_*", "");

        if (jaxbSchedule.getCondition() != null && !jaxbSchedule.getCondition().equals("true"))
		{
            conditionalIndent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + jaxbSchedule.getCondition() + RP + SP + "// conditional expression");
            pw.println(SP_8 + OB);
        }
		
		pw.println(SP_8 + conditionalIndent + JCO + SP + "Schedule " + eventName + " event " + JCC);

        pw.print  (SP_8 + conditionalIndent + "waitDelay" + LP + QU + eventName + QU + CM + SP);

        // according to schema, to meet Priority class definition, the following tags should be permitted:
        // HIGHEST, HIGHER, HIGH, DEFAULT, LOW, LOWER, and LOWEST

        // use enumerations instead of numeric values
        pw.print(jaxbSchedule.getDelay() + CM + " Priority" + PD + jaxbSchedule.getPriority());

        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        for (EdgeParameter edgeParameter : jaxbSchedule.getEdgeParameter()) 
		{
            pw.print(CM + " (Object) ");

            String edgeParameterValue = edgeParameter.getValue();

            // Cover case where there is a "+ 1" increment, or "-1" decrement on a value
            if (edgeParameterValue.contains("+") || edgeParameterValue.contains("-")) 
			{
                pw.print(LP + edgeParameter.getValue() + RP);
            } 
			else 
			{
                pw.print(edgeParameter.getValue());
            }
        }
        pw.println(RP + SC);

        if (jaxbSchedule.getCondition() != null && !jaxbSchedule.getCondition().equals("true")) 
		{
            pw.println(SP_8 + CB);
        }
    }

    void doCancel(Cancel cancel, Event e, PrintWriter pw)
	{
        List<EdgeParameter> edgeParameterList = cancel.getEdgeParameter();
        String conditionalIndent = "";
        Event jaxbEvent = (Event) cancel.getEvent();

        // Strip out name mangling artifacts imposed by the EventGraph Model.
        // This is done to keep XML happy with no identical IDREFs, but let's
        // Simkit work its magic with reflection
        String eventName = jaxbEvent.getName().replaceAll("_\\w+_*", "");

        if (cancel.getCondition() != null && !cancel.getCondition().equals("true")) {
            conditionalIndent = SP_4;
            pw.println(SP_8 + "if" + SP + LP + cancel.getCondition() + RP + SP + "// conditional expression");
            pw.println(SP_8 + OB);
        }
		
		pw.println(SP_8 + conditionalIndent + JCO + SP + "Cancel " + eventName + " event " + JCC);

        pw.print  (SP_8 + conditionalIndent + "interrupt" + LP + QU + eventName + QU);

        // Note: The following loop covers all possibilities with the
        // interim "fix" that all parameters are cast to (Object) whether
        // they need to be or not.
        for (EdgeParameter edgeParameter : edgeParameterList) {
            pw.print(CM + SP + "(Object) " + edgeParameter.getValue());
        }

        pw.println(RP + SC);

        if (cancel.getCondition() != null && !cancel.getCondition().equals("true")) {
            pw.println(SP_8 + CB);
        }
    }

    void buildToString(StringWriter toStringBlock)
	{
        // Assume this is a subclass of some SimEntityBase which should already
        // have a toString()
        if (!extendz.contains(SIM_ENTITY_BASE)) {return;}

        PrintWriter pw = new PrintWriter(toStringBlock);
        pw.println(SP_4 + "@Override");
        pw.println(SP_4 + PUBLIC + " String toString ()");
        pw.println(OB);
        pw.println(SP_8 + "return" + SP + "getClass().getName()" + SC);
        pw.println(SP_4 + CB);
    }

    void buildCodeBlock(StringWriter t)
	{
        PrintWriter pw = new PrintWriter(t);
        String codeBlock = root.getCode(); // cannot rename jaxb method name without modifying simkit.xsd assembly.xsd schemas
        if ((codeBlock != null) && !codeBlock.trim().isEmpty())
		{
            pw.println(SP_4 + "/* Code Block for " + root.getName() + " */");
            pw.println();
            String[] lines = codeBlock.split("\\n");
            for (String codeLines : lines) 
			{
                pw.println(SP_4 + codeLines);
            }
            pw.println(SP_4 + "/* Code Block insertion complete */");
        }
        pw.println(CB);
    }

    void buildSource( // TODO make these class variables to avoid possibility of misordering
			StringBuilder source, 
			StringWriter head,
            StringWriter parameters, 
			StringWriter stateVariables,
            StringWriter parameterMap, 
			StringWriter constructors,
            StringWriter runBlock, 
			StringWriter eventBlock,
            StringWriter accessorBlock, 
			StringWriter toStringBlock,
            StringWriter codeBlock)
	{
        source.append(head.getBuffer());
        source.append(parameters.getBuffer());
        source.append(stateVariables.getBuffer());
        source.append(parameterMap.getBuffer());
        source.append(constructors.getBuffer());
        source.append(runBlock.getBuffer());
        source.append(eventBlock.getBuffer());
        source.append(accessorBlock.getBuffer());
        source.append(toStringBlock);
        source.append(codeBlock.getBuffer());
    }

    private String capitalize(String s)
	{
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private boolean isGeneric(String typeName)
	{
        return ViskitGlobals.instance().isGeneric(typeName);
    }

    private String stripLength(String s)
	{
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
            c = ViskitStatics.ClassForName(unqualifiedExtends.split("\\s")[0]);
        } else {
            c = ViskitStatics.ClassForName(extendz.split("\\s")[0]);
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

        // the extendz field may also contain an implements codeBlock. TODO confirm OK

        Class<?> c = resolveExtensionClass();

        if (c != null) 
		{
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

            for (Parameter p : params) 
			{
                for (int i = pi; i < sparams.length; i++) 
				{
                    if (unqualifiedMatch(p.getType(), sparams[i].getName()) && pi < maxParamCount) 
					{
                        parray[pi] = p;
                        ++pi;
                        break;
                    }
                }
            }

            localSuperParams = Arrays.asList(parray);
        }
		else 
		{
            LOG.error(extendz + " was not found on the working classpath");
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
            aClass = ViskitGlobals.instance().getWorkClassLoader().loadClass(c);
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
     * @param desc a description of the encountered error
     */
    private void error(String desc) {
        LOG.error(desc);
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
        LOG.info("EventGraph (EG) file is: " + xmlFile);
        LOG.info("Generating Java Source...");

        InputStream is = null;
        try {
            is = new FileInputStream(xmlFile);
        } catch (FileNotFoundException fnfe) {LOG.error(fnfe);}

        SimkitEventGraphXML2Java sx2j = new SimkitEventGraphXML2Java(is);
        File baseName = new File(sx2j.baseNameOf(xmlFile));
        sx2j.setFileBaseName(baseName.getName());
        sx2j.setEventGraphFile(new File(xmlFile));
        sx2j.unmarshal();
        String dotJava = sx2j.translate();
        if (dotJava != null && !dotJava.isEmpty()) {
            LOG.info("Done.");
        } else {
            LOG.warn("Compile error on: " + xmlFile);
            return;
        }

        // also write out the .java to a file and compile it
        // to a .class
        LOG.info("Generating Java Bytecode...");
        try {
            if (AssemblyControllerImpl.compileJavaClassFromString(dotJava) != null) {
                LOG.info("Done.");
            }
        } catch (NullPointerException npe) {
            LOG.error(npe);
//            npe.printStackTrace();
        }
    }
}