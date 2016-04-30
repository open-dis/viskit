package viskit.model;

import edu.nps.util.LogUtilities;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.xsd.bindings.assembly.FactoryParameter;
import viskit.xsd.bindings.assembly.MultiParameter;
import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.TerminalParameter;
import viskit.xsd.bindings.eventgraph.Parameter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 15, 2004
 * @since 9:43:42 AM
 * @version $Id$
 */
public abstract class VInstantiator {

    static final Logger LOG = LogUtilities.getLogger(VInstantiator.class);
    private String type;
    private String name = "";
    private String description = "";

    public VInstantiator(String definedType) {
        type = definedType;
    }

    public String getTypeName() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription()
	{
		if (description == null)
			description = "";
        return description;
    }

    abstract public VInstantiator vcopy();

    abstract public boolean isValid();

    public static Vector<Object> buildDummyInstantiators(Executable exe) {

        Vector<Object> v = new Vector<>();
        Class<?>[] cs = exe.getParameterTypes();
        String args;
        for (Class<?> c : cs) {
            args = ViskitStatics.convertClassName(c.getName());

            // Strip out java.lang
            args = ViskitStatics.stripOutJavaDotLang(args);

            // Show varargs symbol vice []
            args = ViskitStatics.makeVarArgs(args);

            if (c.isArray())
                v.add(new VInstantiator.Array(args, new ArrayList<>()));
            else
                v.add(new VInstantiator.FreeForm(args, ""));
        }
        return v;
    }

    /***********************************************************************/
    public static class FreeForm extends VInstantiator {

        private String value;

        public FreeForm(String type, String value) {
            super(type);
            setValue(value);
        }

        public String getValue() {
            return value;
        }

        public final void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public VInstantiator vcopy()
		{
            VInstantiator resultVInstantiator = new VInstantiator.FreeForm(getTypeName(), getValue());
            resultVInstantiator.setName(getName());
            resultVInstantiator.setDescription(getDescription());
            return resultVInstantiator;
        }

        @Override
        public boolean isValid() {
            String typeName = getTypeName();
            String value = getValue();
            return typeName != null & value != null & !typeName.isEmpty() & !value.isEmpty();
        }
    }

    /***********************************************************************/
    public static class Construct extends VInstantiator {

        private List<Object> args;

        /** Takes a List of Assembly parameters and arguments for type
         *
         * @param assemblyParametersList a list of Assembly parameters
         * @param typeName a parameter type
         */
        public Construct(List<Object> assemblyParametersList, String typeName) {
            super(typeName);

            if (viskit.ViskitStatics.debug) {
                LOG.info("Building Constructor for " + typeName);
            }
            if (viskit.ViskitStatics.debug) {
                LOG.info("Required Parameters:");

                for (Object o : assemblyParametersList) {

                    String s1 = "null";
                    if (o instanceof TerminalParameter) { // check if caller is sending assembly param types
                        s1 = ((TerminalParameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tAssembly TerminalParameter");
                        }
                    } else if (o instanceof MultiParameter) {
                        s1 = ((MultiParameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tAssembly MultiParameter");
                        }
                    } else if (o instanceof FactoryParameter) {
                        s1 = ((FactoryParameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tAssembly FactoryParameter");
                        }
                    } else if (o instanceof Parameter) { // from InstantiationPanel, this could also be an eventgraph param type?
                        s1 = ((Parameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tEventGraph Parameter");
                        }
                    }
                    LOG.info(" " + s1);
                }
            }

            // gets lists of EventGraph parameters for type if top-level
            // or null if type is a basic class i.e., java.lang.Double
            List<Object>[] eventGraphParameters = ViskitStatics.resolveParameters(ViskitStatics.classForName(typeName));
            int index = 0;

            args = buildInstantiators(assemblyParametersList);
            // pick the EventGraph list that matches the Assembly arguments
            if (eventGraphParameters != null)
			{
                while (index < (eventGraphParameters.length - 1))
				{
                    if (parametersMatch(assemblyParametersList, eventGraphParameters[index]))
					{
                        break; // found correct parameter
                    }
					else
					{
                        index++; // continue
                    }
					if (index == (eventGraphParameters.length - 1))
					{
						// TODO if not found prior to end of loop, warn of model inconsistency, probably should not continue
						LOG.error("Model inconsistency: no matching eventGraphParameter found to match assemblyPrameter[" + index + "]"); // TODO
					}
                }
                if (viskit.ViskitStatics.debug) {
                    LOG.info(typeName + " VInstantiator using constructor #" + index);
                }
                // bug: weird case where params came in 0 length but no 0 length constuctors
                // happens if external class used as parameter?
                if (assemblyParametersList.size() != eventGraphParameters[index].size())
				{
                    args = buildInstantiators(eventGraphParameters[index]);
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("Warning: VInstantiator.Constructor assembly had different expected parameters length than event graph");
                    }
                }
                if (eventGraphParameters[index] != null)
				{
                    // now that the values, types, etc. are set, grab names from Event Graph parameters
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("args came back from buildInstantiators as: ");
                        for (Object arg : args) {
                            LOG.info(arg);
                        }
                    }
                    if (args != null)
					{
                        for (int j = 0; j < eventGraphParameters[index].size(); j++)
						{
                            if (viskit.ViskitStatics.debug)
							{
                                LOG.info("setting name " + ((Parameter)eventGraphParameters[index].get(j)).getName());
                            }
                            ((VInstantiator) args.get(j)).setName(((Parameter)eventGraphParameters[index].get(j)).getName());
							
							String parameterDescription = ((Parameter)eventGraphParameters[index].get(j)).getDescription();
							if (parameterDescription == null)
							{
								parameterDescription = listToString(((Parameter)eventGraphParameters[index].get(j)).getComment());
											
								if (parameterDescription == null) // if still not there, punt (safely)
								{
									parameterDescription = ""; // TODO ensure this value got set when initially loaded
								}
							}
                            ((VInstantiator) args.get(j)).setDescription(parameterDescription);
                        }
                    }
                }
            }
        }

        public Construct(String typeName, List<Object> args) {
            super(typeName);
            setArgs(args);
            findArgNames(typeName, args);
        }

        public Construct(String type, List<Object> args, List<String> names) {
            this(type, args);
            for (int i = 0; i < args.size(); i++) {
                ((VInstantiator) args.get(i)).setName(names.get(i));
            }
        }

        private String listToString(List<String> stringList) {
            StringBuilder sb = new StringBuilder("");
            for (String s : stringList) {
                sb.append(s);
            }
            return sb.toString();
        }

        /**
         * @param assemblyParameters used to build the instantiators
         * @return a List of VInstantiators given a List of Assembly Parameters
         */
        final List<Object> buildInstantiators(List<Object> assemblyParameters) {

            List<Object> instantiatorList = new ArrayList<>();
            for (Object o : assemblyParameters) {
                if (o instanceof TerminalParameter) {
                    instantiatorList.add(buildTerminalParameter((TerminalParameter) o));
                } else if (o instanceof MultiParameter) {
                    instantiatorList.add(buildMultiParameter((MultiParameter) o));
                } else if (o instanceof FactoryParameter) {
                    instantiatorList.add(buildFactoryParameter((FactoryParameter) o));
                } else if (o instanceof Parameter) { // from InstantiationPanel Const getter
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("Conversion from " + ((Parameter) o).getType());
                    }

                    String name         = ((Parameter) o).getName();
                    String typeName     = ((Parameter) o).getType();
                    String description  = ((Parameter) o).getDescription();
                    ObjectFactory objectFactory = new ObjectFactory();

                    // TerminalParameter w/ special case for Object... (varargs)
                    if (ViskitStatics.isPrimitive(typeName) || typeName.contains("String") || typeName.contains("Object...")) 
					{
                        TerminalParameter terminalParameter = objectFactory.createTerminalParameter();
                        terminalParameter.setName(name);
                        terminalParameter.setType(typeName);
                        terminalParameter.setDescription(description);
                        terminalParameter.setValue("");

                        instantiatorList.add(buildTerminalParameter(terminalParameter));
                    } 
					else if (ViskitStatics.numConstructors(typeName) > 0) // MultiParameter
					{

                        MultiParameter multiParameter = objectFactory.createMultiParameter();
                        multiParameter.setName(name);
                        multiParameter.setType(typeName);
                        multiParameter.setDescription(description);

                        instantiatorList.add(buildMultiParameter(multiParameter));
                    } 
					else 
					{ // no constructors, should be a FactoryParameter or array of them

                        if (ViskitGlobals.instance().isArray(typeName))
						{
                            MultiParameter multiParameter = objectFactory.createMultiParameter();
                            multiParameter.setName(name);
                            multiParameter.setType(typeName);
							multiParameter.setDescription(description);
                            instantiatorList.add(buildMultiParameter(multiParameter));
                        } 
						else
						{
                            FactoryParameter factoryParameter = objectFactory.createFactoryParameter();
                            factoryParameter.setName(name);
                            factoryParameter.setType(typeName); // this is the type returned by method
							factoryParameter.setDescription(description);
                            factoryParameter.setFactory(ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS);
                            factoryParameter.setMethod(ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD);

                            instantiatorList.add(buildFactoryParameter(factoryParameter));
                        }
                    }
                }
            }
            return instantiatorList;
        }

        VInstantiator.FreeForm buildTerminalParameter(TerminalParameter terminalParameter) {
            return new VInstantiator.FreeForm(terminalParameter.getType(), terminalParameter.getValue());
        }

        VInstantiator.Array buildMultiParameter(MultiParameter multiParameter, boolean dummy) {
            List<Object> instantiatorList = multiParameter.getParameters();
            return new VInstantiator.Array(multiParameter.getType(), buildInstantiators(instantiatorList));
        }

        VInstantiator buildMultiParameter(MultiParameter multiParameter) {
            VInstantiator vInstantiatorAorC;

            // Check for special case of varargs
            if (ViskitGlobals.instance().isArray(multiParameter.getType()) || multiParameter.getType().contains("...")) {
                vInstantiatorAorC = buildMultiParameter(multiParameter, true);
            } else {
                if (ViskitStatics.debug) {
                    LOG.info("Trying to buildMultiParameter " + multiParameter.getType());
                }

                List<Object> parameterList = multiParameter.getParameters();

                if (parameterList.isEmpty()) {

                    // Likely, Diskit, or another library is not on the classpath
                    if (ViskitStatics.resolveParameters(ViskitStatics.classForName(multiParameter.getType())) == null) {
                        return null;
                    } else {
                        parameterList = ViskitStatics.resolveParameters(ViskitStatics.classForName(multiParameter.getType()))[0];
                    }
                }
                Iterator<Object> parameterListIterator = parameterList.iterator();
                if (ViskitStatics.debug) {
                    while (parameterListIterator.hasNext()) {
                        LOG.info(parameterListIterator.next());
                    }
                }

                vInstantiatorAorC = new VInstantiator.Construct(parameterList, multiParameter.getType());
            }
            return vInstantiatorAorC;
        }

        VInstantiator.Factory buildFactoryParameter(FactoryParameter factoryParameter) {
            List<Object> objectList = factoryParameter.getParameters();
            return new VInstantiator.Factory(
                    factoryParameter.getType(), factoryParameter.getFactory(), factoryParameter.getMethod(),
                    buildInstantiators(objectList));
        }

        final boolean parametersMatch(List<Object> assemblyParameters, List<Object> eventGraphParameters) {
            if (assemblyParameters.size() != eventGraphParameters.size()) {
                if (viskit.ViskitStatics.debug) {
                    LOG.info("No match.");
                }
                return false;
            }

            for (int i = 0; i < assemblyParameters.size(); i++) {
                Object o = assemblyParameters.get(i);
                String eventGraphTypeName = ((Parameter)eventGraphParameters.get(i)).getType();
                String assemblyTypeName;
                if (o instanceof TerminalParameter) // check if caller is sending assembly parameter types
				{
                    assemblyTypeName = ((TerminalParameter) o).getType();
                } 
				else if (o instanceof MultiParameter) 
				{
                    assemblyTypeName = ((MultiParameter) o).getType();
                } 
				else if (o instanceof FactoryParameter) 
				{
                    assemblyTypeName = ((FactoryParameter) o).getType();
                } 
				else if (o instanceof Parameter) // from InstantiationPanel, this could also be an eventgraph parameter type
				{
                    assemblyTypeName = ((Parameter) o).getType();
                } 
				else 
				{
                    return false;
                }
                if (viskit.ViskitStatics.debug)
				{
                    System.out.print("Type match " + assemblyTypeName + " to " + eventGraphTypeName);
                }

                // check if vType was assignable from pType.

                Class<?> eventGraphClass = ViskitStatics.classForName(eventGraphTypeName);
                Class<?>   assemblyClass = ViskitStatics.classForName(assemblyTypeName);
                Class<?>[]   vInterfaces = assemblyClass.getInterfaces();
                boolean interfaceMatch = false;
                for (Class<?> vInterface : vInterfaces) {
                    //interfz |= vInterfz[k].isAssignableFrom(eClazz);
                    interfaceMatch |= eventGraphClass.isAssignableFrom(vInterface);
                }
                boolean match = (eventGraphClass.isAssignableFrom( assemblyClass) | interfaceMatch);
                if (!match)
				{
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("No match.");
                    }
                    return false;
                }
            }
            if (viskit.ViskitStatics.debug) {
                LOG.info("Match.");
            }
            return true; // no problems found
        }

        /**
         * Find the names of the arguments
         * @param type
         * @param args List of VInstantiators
         * @return true if arg names have been found
         */
        private boolean findArgNames(String type, List<Object> args) {
            if (args == null) {
                setArgs(getDefaultArgs(type));
                args = getArgs();
            }
            return (indexOfArgNames(type, args) < 0);
        }

        /** Find a typeName match in the ClassLoader of the given EventGraph's parameters
         *
         * @param typeName the EventGraph parameter to check
         * @param args a list of EG parameters
         * @return the index into the found matching constructor
         */
        public int indexOfArgNames(String typeName, List<Object> args) {
            List<Object>[] parameters = ViskitStatics.resolveParameters(ViskitStatics.classForName(typeName));
            int constructorIndex = -1;

            if (parameters == null) {
                return constructorIndex;
            }
            int index = 0;

            if (viskit.ViskitStatics.debug) {
                LOG.info("args length " + args.size());
                LOG.info("indexOfArgNames " + typeName + " parameters list length is " + parameters.length);
            }
            for (List<Object> parameter : parameters) {
                if (viskit.ViskitStatics.debug) {
                    LOG.info("parameter.size() " + parameter.size());
                }
                if (parameter.size() == args.size())
				{
                    boolean match = true;
                    for (int j = 0; j < args.size(); j++) {

                        if (viskit.ViskitStatics.debug) {
                            LOG.info("touching " +
                                    ViskitStatics.convertClassName(
                                            ((Parameter)parameter.get(j)).getType())
                                    + " "
                                    + ((VInstantiator) args.get(j)).getTypeName());
                        }
                        String parameterTypeName     = ViskitStatics.convertClassName(((Parameter)parameter.get(j)).getType());
                        String vInstantiatorTypeName = ((VInstantiator) args.get(j)).getTypeName();

                        // check if vType was assignable from pType.

                        Class<?> parameterClassName = ViskitStatics.classForName(parameterTypeName);

                        if (parameterClassName == null) {
                            JOptionPane.showMessageDialog(null, "<html><body><p align='center'>" +
                                    "Please check Event Graph <b>" + typeName + "</b> parameter(s) for compliance using" +
                                    " fully qualified Java class names.  " + parameterTypeName + " should be a " +
                                    vInstantiatorTypeName + ".</p></body></html>",
                                    "Basic Java Class Name Found",
                                    JOptionPane.ERROR_MESSAGE);
                            match = false;
                        } 
						else 
						{
                            Class<?> vInstantiatorClass = ViskitStatics.classForName(vInstantiatorTypeName);
                            Class<?>[] vInstantiatorInterfaces = vInstantiatorClass.getInterfaces();
                            boolean interfaceMatch = false;
                            for (Class<?> vInstantiatorInterfaceClass : vInstantiatorInterfaces) {
                                //interfz |= vInterfz[k].isAssignableFrom(pClazz);
                                interfaceMatch |= parameterClassName.isAssignableFrom(vInstantiatorInterfaceClass);
                            }

                            match &= (parameterClassName.isAssignableFrom(vInstantiatorClass) | interfaceMatch);

                            // set the names, the final iteration of while cleans up
                            if (!((VInstantiator) (args.get(j))).getName().equals(((Parameter)parameter.get(j)).getName()))
                                 ((VInstantiator) (args.get(j))).setName(((Parameter)parameter.get(j)).getName());
                            if (viskit.ViskitStatics.debug) {
                                LOG.info(" to " + ((Parameter)parameter.get(j)).getName());
                            }
                        }
                    }
                    if (match) {
                        constructorIndex = index;
                        break;
                    }
                }
                index++;
            }
            if (viskit.ViskitStatics.debug) {
                LOG.info("Resolving " + typeName + " " + parameters[constructorIndex] + " at index " + constructorIndex);
            }
            // the class manager caches Parameter List jaxb from the SimEntity.
            // If it didn't come from XML, then a null is returned.

            return constructorIndex;
        }

        private List<Object> getDefaultArgs(String typeName) {
            Class<?> inputClass = ViskitStatics.classForName(typeName);
            if (inputClass != null) {
                Constructor[] constructors = inputClass.getConstructors();
                if (constructors != null && constructors.length > 0)
				{
                    // TODO: May need to revisit why we are just concerned with
                    // the default zero param constructor
                    return VInstantiator.buildDummyInstantiators(constructors[0]);
                }
            }
            return new Vector<>(); // null
        }

        public List<Object> getArgs() {
            return args;
        }

        public final void setArgs(List<Object> args) {
            this.args = args;
        }

        @Override
        public String toString() {
            String result = "new " + getTypeName() + "(";
            result = result + (args.size() > 0 ? ((VInstantiator) args.get(0)).getTypeName() + ",..." : "");
            return result + ")";
        }

        @Override
        public VInstantiator vcopy() {
            Vector<Object> objectVector = new Vector<>();
            for (Object o : args) {
                VInstantiator vInstantiator = (VInstantiator) o;
                objectVector.add(vInstantiator.vcopy());
            }
            VInstantiator resultVInstantiator = new VInstantiator.Construct(getTypeName(), objectVector);
            resultVInstantiator.setName(this.getName());
            resultVInstantiator.setDescription(this.getDescription());
            return resultVInstantiator;
        }

        @Override
        public boolean isValid() {
            if (getTypeName() == null || getTypeName().isEmpty()) {
                return false;
            }
            for (Object o : args) {
                VInstantiator vInstantiator = (VInstantiator) o;
                if (!vInstantiator.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /***********************************************************************/
    public static class Array extends VInstantiator {

        private List<Object> instantiators; // array dimension == size()

        public Array(String typeName, List<Object> instantiatorsList) {
            super(typeName);
            setInstantiators(instantiatorsList);
        }

        @Override
        public VInstantiator vcopy() {
            Vector<Object> objectVector = new Vector<>();
            for (Object vi : instantiators) {
                objectVector.add(((VInstantiator) vi).vcopy());
            }
            VInstantiator rv = new VInstantiator.Array(getTypeName(), objectVector);
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        public List<Object> getInstantiators() {
            return instantiators;
        }

        public final void setInstantiators(List<Object> instantiators) {
            this.instantiators = instantiators;
        }

        @Override
        public String toString() {
            if (instantiators != null) {
                if (getTypeName().contains("Object...")) {
                    return getTypeName();
                } else {
                    String t = getTypeName().substring(0, getTypeName().indexOf('['));
                    return "new " + t + "[" + instantiators.size() + "]";
                }
            } else {
                return "";
            }
        }

        @Override
        public boolean isValid() {
            if (getTypeName() == null || getTypeName().isEmpty()) {
                return false;
            }
            for (Object vi : instantiators) {
                if (!((VInstantiator) vi).isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /***********************************************************************/
    public static class Factory extends VInstantiator {

        private String factoryClassName;
        private String methodName;
        private List<Object> parametersList;

        /** A factory for the VInstantiator which carries information on what
         * type of variable we need to provide for a SimEntity constructor.
         *
         * @param typeName Object type required by a SimEntity constructor
         * @param factoryClassName the class that will return this type
         * @param methodName the method of the factoryClass that will return our desired type
         * @param parametersList the parameters required to return the desired type
         */
        public Factory(String typeName, String factoryClassName, String methodName, List<Object> parametersList) {
            super(typeName);
            setFactoryClassName(factoryClassName);
            setMethodName(methodName);
            setParametersList(parametersList);
        }

        public String getFactoryClass() {
            return factoryClassName;
        }

        public String getMethodName() {
            return methodName;
        }

        public List<Object> getParametersList() {
            return parametersList;
        }

        public final void setFactoryClassName(String factoryClassName) {
            this.factoryClassName = factoryClassName;
        }

        public final void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public final void setParametersList(List<Object> parametersList) {
            this.parametersList = parametersList;
        }

        @Override
        public String toString() {
            if (parametersList.isEmpty()) {
                return "";
            }

            StringBuilder buffer = new StringBuilder();
            buffer.append(factoryClassName);
            buffer.append(".");
            buffer.append(methodName);
            buffer.append("(");
            String args = null;
            for (Object o : parametersList) {

                if (o instanceof VInstantiator) {
                    args = ((VInstantiator)o).type;
                } else if (o instanceof String) {
                    args = (String) o;
                }

                // Strip out java.lang
                args = ViskitStatics.stripOutJavaDotLang(args);

                // Show varargs symbol vice []
                if (ViskitGlobals.instance().isArray(args)) {
                    args = ViskitStatics.makeVarArgs(args);
                    buffer.append(args);
                } else {
                    buffer.append(args);
                }
                buffer.append(", ");
            }
            buffer = buffer.delete(buffer.lastIndexOf(", "), buffer.length());
            buffer.append(")");

            return buffer.toString(); // TODO show examples
        }

        @Override
        public VInstantiator vcopy() {
            Vector<Object> objectList = new Vector<>();
            VInstantiator vi;
            for (Object o : parametersList) {

                if (o instanceof VInstantiator) {
                    vi = (VInstantiator) o;
                    objectList.add(vi.vcopy());
                } else if (o instanceof String) {
                    objectList.add((String) o);
                }
            }
            VInstantiator resultVInstantiator = new VInstantiator.Factory(getTypeName(), getFactoryClass(), getMethodName(), objectList);
            resultVInstantiator.setName(getName());
            resultVInstantiator.setDescription(getDescription());
            return resultVInstantiator;
        }

        @Override
        public boolean isValid() {
            String typeName = getTypeName(), factoryClassName = getFactoryClass(), methodName = getMethodName();
            if (typeName == null   || factoryClassName == null   || methodName == null ||
                typeName.isEmpty() || factoryClassName.isEmpty() || methodName.isEmpty())
			{
                return false;
            }
            for (Object o : parametersList) {

                if (o instanceof VInstantiator) {
                    VInstantiator v = (VInstantiator) o;
                    if (!v.isValid()) {
                        return false;
                    }
                } else if (o instanceof String) {
                    if (((String) o).isEmpty()) {
                        return false;
                    }
                }
            }
            return true; // no problems found
        }
    }
}
