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
public abstract class ViskitInstantiator
{
    static final Logger LOG = LogUtilities.getLogger(ViskitInstantiator.class);
	
    private String name = "";
    private String type;
    private String description = "";

    public ViskitInstantiator(String definedType) 
	{
        type = definedType;
    }

    public String getTypeName() 
	{
        return type;
    }

    public void setName(String name) 
	{
        this.name = name;
    }

    public String getName() 
	{
        return name;
    }

    public void setDescription(String description) 
	{
        this.description = description;
    }

    public String getDescription()
	{
		if (description == null)
			description = "";
        return description;
    }
	
	public ViskitInstantiator vcopy() // need full copy for each implementing class
	{
		if (this instanceof ViskitInstantiator.Array)
		{
			return ((ViskitInstantiator.Array)this).vcopy();
		}
		else if (this instanceof ViskitInstantiator.Construct)
		{
			return ((ViskitInstantiator.Construct)this).vcopy();
		}
		else if (this instanceof ViskitInstantiator.Factory)
		{
			return ((ViskitInstantiator.Factory)this).vcopy();
		}
		else if  (this instanceof ViskitInstantiator.FreeForm)
		{
			return ((ViskitInstantiator.FreeForm)this).vcopy();
		}
		else
		{
			LOG.error ("Erroneous ViskitInstantiator vcopy invocation"); // do not invoke as super.vcopy() from implementing class!
			return null;
		}
	}

    abstract public boolean isValid();

    public static Vector<Object> buildInstantiatorsFromReflection(Executable[] reflectionExecutables) 
	{
        Vector<Object> viskitInstantiatorVector = new Vector<>();
		
		for (Executable reflectionExecutable : reflectionExecutables)
		{
			Class<?>[] reflectionParameterClasses = reflectionExecutable.getParameterTypes();
			String className, parameterName;
			for (Class<?> reflectionParameterClass : reflectionParameterClasses) 
			{
				className = ViskitStatics.convertClassName(reflectionParameterClass.getName());
				className = ViskitStatics.stripOutJavaDotLang(className); // Strip out java.lang
				className = ViskitStatics.applyVarArgSymbol(className);   // Show varargs ellipsis symbol ... vice []
				parameterName = "parameterNameTODO";
				if (reflectionParameterClass.isArray())
				{
					viskitInstantiatorVector.add(new ViskitInstantiator.Array(className, new ArrayList<>()));	  // TODO how to add name?
				}
////				else if (true) // reflectionParameterClass.???) // TODO trying to construct factory type...
////				{
////					viskitInstantiatorVector.add(new ViskitInstantiator.Construct(className, new ArrayList<>())); // TODO how to add name?
////				}
				else 	
				{
					viskitInstantiatorVector.add(new ViskitInstantiator.FreeForm(className, parameterName)); // TODO fix name
				}
			}
			if (viskitInstantiatorVector.size() > 0) // reflection found parameters during preceding iteration
			{
				return viskitInstantiatorVector;
			}
		}
		return viskitInstantiatorVector; // return empty
    }

    /***********************************************************************/
    public static class FreeForm extends ViskitInstantiator 
	{
        private String value;

        public FreeForm(String type, String name) 
		{
            super(type);
            super.name = name;
        }

        public String getValue() 
		{
            return value;
        }

        public final void setValue(String value) 
		{
            this.value = value;
        }

        @Override
        public String toString()
		{
            return value;
        }

		@Override
        public ViskitInstantiator.FreeForm vcopy()
		{
            ViskitInstantiator.FreeForm resultViskitInstantiator = new ViskitInstantiator.FreeForm(getTypeName(), getName());
            resultViskitInstantiator.setValue(getValue());
            resultViskitInstantiator.setDescription(getDescription());
            return resultViskitInstantiator;
        }

        @Override
        public boolean isValid() 
		{
            String  name     = getName();
            String  typeName = getTypeName();
            String  value    = getValue();
			boolean valid    = (name != null) & (typeName != null) & (value != null) & !name.isEmpty() & !typeName.isEmpty() & !value.isEmpty();
            return  valid;
        }
    }

    /***********************************************************************/
    public static class Construct extends ViskitInstantiator 
	{
        private List<Object> viskitParametersFactoryList;

        /** Takes a List of Assembly parameters and arguments for typeName, then constructs a ViskitInstantiator
         *
         * @param assemblyParametersList a list of Assembly parameters
         * @param typeName a parameter type
         */
        public Construct(List<Object> assemblyParametersList, String typeName)
		{
            super(typeName);

            if (viskit.ViskitStatics.debug) 
			{
                LOG.info("Constructor for " + typeName + ". Required Parameters:");
            }

			int index = 0;
			for (Object assemblyParameter : assemblyParametersList) // inspect for debugging purposes
			{
				String assemblyParameterName        = "notFound";
				String assemblyParameterType        = "null";
				String assemblyParameterValue       = "";
				String assemblyParameterDescription = "";
				String assemblyFactoryParameterFactoryName = "";
	 TerminalParameter assemblyFactoryParameterTerminalParameter = null;
						
				if (assemblyParameter instanceof TerminalParameter) // simple type
				{ 
					assemblyParameterName        = ((TerminalParameter) assemblyParameter).getName();
					assemblyParameterType        = ((TerminalParameter) assemblyParameter).getType();
					assemblyParameterValue       = ((TerminalParameter) assemblyParameter).getValue();
					assemblyParameterDescription = ((TerminalParameter) assemblyParameter).getDescription();
					if (viskit.ViskitStatics.debug) 
					{
						LOG.info("Assembly TerminalParameter " + assemblyParameterType + " " + assemblyParameterName + " " + assemblyParameterValue);
					}
				} 
				else if (assemblyParameter instanceof MultiParameter) // TODO object array type?
				{
					assemblyParameterName        = ((MultiParameter) assemblyParameter).getName();
					assemblyParameterType        = ((MultiParameter) assemblyParameter).getType();
					assemblyParameterValue       = ""; // no value attribute for Parameter element
					assemblyParameterDescription = ((MultiParameter) assemblyParameter).getDescription();
					if (viskit.ViskitStatics.debug) 
					{
						LOG.info("Assembly MultiParameter " + assemblyParameterType + " " + assemblyParameterName);
					}
				} 
				else if (assemblyParameter instanceof FactoryParameter) // which contains a TerminalParameter
				{
					assemblyFactoryParameterFactoryName = ((FactoryParameter) assemblyParameter).getFactory();
					if (((((FactoryParameter) assemblyParameter).getParameters()) != null) && 
					    ((((FactoryParameter) assemblyParameter).getParameters()).get(0) != null) && 
					    ((((FactoryParameter) assemblyParameter).getParameters()).get(0) instanceof TerminalParameter))
					{
						assemblyFactoryParameterTerminalParameter = ((TerminalParameter)(((FactoryParameter) assemblyParameter).getParameters()).get(0));
						assemblyParameterName        = assemblyFactoryParameterTerminalParameter.getName();
						assemblyParameterType        = assemblyFactoryParameterTerminalParameter.getType(); // object type
						assemblyParameterValue       = "";// no value attribute for Parameter element
						assemblyParameterDescription = assemblyFactoryParameterTerminalParameter.getDescription();
						
						if (viskit.ViskitStatics.debug) 
						{
							LOG.info("Assembly FactoryParameter " + assemblyFactoryParameterFactoryName + " " + assemblyParameterType + " " + assemblyParameterName);
						}
					}
					else
					{
						LOG.error("Assembly error, FactoryParameter factory='" + assemblyFactoryParameterFactoryName + "' contains no TerminalParameter");
					}
				} 
				else if (assemblyParameter instanceof Parameter) // from InstantiationPanel, this could also be an eventgraph param type?
				{
					assemblyParameterName        = ((Parameter) assemblyParameter).getName();
					assemblyParameterType        = ((Parameter) assemblyParameter).getType();
					assemblyParameterValue       = ""; // no value attribute for Parameter element
					assemblyParameterDescription = ((Parameter) assemblyParameter).getDescription();
					if (viskit.ViskitStatics.debug) 
					{
						LOG.info("EventGraph Parameter " + assemblyParameterType + " " + assemblyParameterName);
					}
				}
				else
				{
					LOG.error ("Assembly parameter has unexpected type");
					break;
				}
//				((ViskitInstantiator) viskitParametersFactoryList.get(index)).setName       (assemblyParameterName);
//				((ViskitInstantiator) viskitParametersFactoryList.get(index)).setDescription(assemblyParameterDescription);
				index++;
			}
			// preceding was mostly informational, the next call does the work
            viskitParametersFactoryList = buildInstantiators(assemblyParametersList);
			
			if (!assemblyParametersList.isEmpty() && !viskitParametersFactoryList.isEmpty())
				return; // constructor complete
			
			// When XML-based instantiation is unsuccessful, then we are apparently working with compiled .class code
			
            // Gets lists of EventGraph parameters for type, if top-level,
            // otherwise null if type is a basic class i.e., java.lang.Double
            List<Object>[] eventGraphParametersList = ViskitStatics.resolveParametersUsingReflection(ViskitStatics.classForName(typeName));
			
            // pick the EventGraph list that matches the Assembly arguments
            if (eventGraphParametersList != null)
			{
				index = 0;
                while (index < eventGraphParametersList.length)
				{
                    if (parametersListsMatch(assemblyParametersList, eventGraphParametersList[index]))
					{
                        break; // found correct parameter list, they match
                    }
					else
					{
                        index++; // continue looping
                    }
					if (index == eventGraphParametersList.length) // end of loop
					{
						// TODO if not found prior to end of loop, warn of model inconsistency, probably should not continue
						LOG.error("Model inconsistency: no matching eventGraphParameter found to match assemblyParameter[" + index + "]"); // TODO
						return; // constructor complete
					}
                }
                if (viskit.ViskitStatics.debug)
				{
                    LOG.info(typeName + " VInstantiator using constructor #" + index);
                }
                // bug: weird case where params came in 0 length but no 0 length constuctors
                // happens if external class used as parameter?
                if (assemblyParametersList.size() != eventGraphParametersList[index].size())
				{
                    viskitParametersFactoryList = buildInstantiators(eventGraphParametersList[index]);
                    if (viskit.ViskitStatics.debug)
					{
                        LOG.info("Warning: VInstantiator.Constructor assembly had different expected parameters length than event graph");
                    }
                }
                if (eventGraphParametersList[index] != null)
				{
                    // now that the values, types, etc. are set, grab names from Event Graph parameters
                    if (viskit.ViskitStatics.debug)
					{
                        LOG.info("args came back from buildInstantiators as: ");
                        for (Object initializationArgument : viskitParametersFactoryList)
						{
                            LOG.info(initializationArgument);
                        }
                    }
                    if (viskitParametersFactoryList != null)
					{
                        for (int j = 0; j < eventGraphParametersList[index].size(); j++)
						{
                            if (viskit.ViskitStatics.debug)
							{
                                LOG.info("setting name " + ((Parameter)eventGraphParametersList[index].get(j)).getName());
                            }
                            ((ViskitInstantiator) viskitParametersFactoryList.get(j)).setName(((Parameter)eventGraphParametersList[index].get(j)).getName());
							
							String parameterDescription = ((Parameter)eventGraphParametersList[index].get(j)).getDescription();
							if (parameterDescription == null)
							{
								parameterDescription = listToString(((Parameter)eventGraphParametersList[index].get(j)).getComment());
							}
							if (parameterDescription == null)
							{
								parameterDescription = ((TerminalParameter)assemblyParametersList.get(j)).getDescription();
							}		
							if (parameterDescription == null) // if still not there, punt (safely)
							{
								parameterDescription = ViskitStatics.DEFAULT_DESCRIPTION; // TODO ensure this value got set when initially loaded
							}
                            ((ViskitInstantiator) viskitParametersFactoryList.get(j)).setDescription(parameterDescription);
                        }
                    }
                }
            }
        }

        public Construct(String typeName, List<Object> args)
		{
            super(typeName);
            setParametersFactoryList(args);
            findArgumentNames(typeName, args);
        }

        public Construct(String type, List<Object> args, List<String> names)
		{
            this(type, args);
            for (int i = 0; i < args.size(); i++) {
                ((ViskitInstantiator) args.get(i)).setName(names.get(i));
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
         * @param jaxbAssemblyParameters used to build the instantiators
         * @return a List of ViskitInstantiators given a List of Assembly Parameters
         */
        final List<Object> buildInstantiators(List<Object> jaxbAssemblyParameters) // TODO type Object to ViskitInstantiator
		{
            List<Object> instantiatorList = new ArrayList<>();
            for (Object jaxbAssemblyParameter : jaxbAssemblyParameters)
			{
                if (jaxbAssemblyParameter instanceof TerminalParameter)
				{
                    instantiatorList.add(buildTerminalParameter((TerminalParameter) jaxbAssemblyParameter));
                } 
				else if (jaxbAssemblyParameter instanceof MultiParameter) 
				{
                    instantiatorList.add(buildMultiParameter((MultiParameter) jaxbAssemblyParameter));
                } 
				else if (jaxbAssemblyParameter instanceof FactoryParameter)  // which contains a TerminalParameter
				{
                    instantiatorList.add(buildFactoryParameter((FactoryParameter) jaxbAssemblyParameter));
                } 
				else if (jaxbAssemblyParameter instanceof Parameter) 
				{ // from InstantiationPanel Const getter
                    if (viskit.ViskitStatics.debug) 
					{
                        LOG.info("Conversion from " + ((Parameter) jaxbAssemblyParameter).getType());
                    }
                    String name         = ((Parameter) jaxbAssemblyParameter).getName();
                    String typeName     = ((Parameter) jaxbAssemblyParameter).getType();
                    String description  = ((Parameter) jaxbAssemblyParameter).getDescription();
                    ObjectFactory objectFactory = new ObjectFactory();

                    // TerminalParameter w/ special case for Object... (varargs)
                    if (ViskitStatics.isPrimitive(typeName) || typeName.contains("String") || typeName.contains("Object...")) 
					{
                        TerminalParameter terminalParameter = objectFactory.createTerminalParameter();
                        terminalParameter.setName(name);
                        terminalParameter.setType(typeName);
                        terminalParameter.setDescription(description);
						if (jaxbAssemblyParameter instanceof TerminalParameter)
                            terminalParameter.setValue(((TerminalParameter) jaxbAssemblyParameter).getValue());

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

        ViskitInstantiator.FreeForm buildTerminalParameter(TerminalParameter terminalParameter)
		{
			ViskitInstantiator.FreeForm viskitInstantiator =  new ViskitInstantiator.FreeForm(terminalParameter.getType(), terminalParameter.getName());
			viskitInstantiator.setValue      (terminalParameter.getValue());
			viskitInstantiator.setDescription(terminalParameter.getDescription());
			if ((viskitInstantiator.getDescription() == null) || viskitInstantiator.getDescription().isEmpty())
				viskitInstantiator.setDescription(ViskitStatics.DEFAULT_DESCRIPTION);
            return viskitInstantiator;
        }

        ViskitInstantiator.Array buildMultiParameter(MultiParameter multiParameter, boolean dummy)
		{
            List<Object> instantiatorList = multiParameter.getParameters();
            return new ViskitInstantiator.Array(multiParameter.getType(), buildInstantiators(instantiatorList));
        }

        ViskitInstantiator buildMultiParameter(MultiParameter multiParameter)
		{
            ViskitInstantiator vInstantiatorAorC;

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
                    if (ViskitStatics.resolveParametersUsingReflection(ViskitStatics.classForName(multiParameter.getType())) == null) {
                        return null;
                    } else {
                        parameterList = ViskitStatics.resolveParametersUsingReflection(ViskitStatics.classForName(multiParameter.getType()))[0];
                    }
                }
                Iterator<Object> parameterListIterator = parameterList.iterator();
                if (ViskitStatics.debug) {
                    while (parameterListIterator.hasNext()) {
                        LOG.info(parameterListIterator.next());
                    }
                }

                vInstantiatorAorC = new ViskitInstantiator.Construct(parameterList, multiParameter.getType());
            }
            return vInstantiatorAorC;
        }

        ViskitInstantiator.Factory buildFactoryParameter(FactoryParameter factoryParameter) 
		{
			String methodName = "";
            List<Object> objectList = factoryParameter.getParameters();
			
			if ((objectList != null) && (objectList.size() > 0) && (objectList.get(0) != null))
			{
				Object jaxbObject = objectList.get(0);
                if (jaxbObject instanceof TerminalParameter)
				{
					methodName = ((TerminalParameter)jaxbObject).getName();
                } 
				else if (jaxbObject instanceof MultiParameter) 
				{
					methodName = ((MultiParameter)jaxbObject).getName();
                } 
				else if (jaxbObject instanceof FactoryParameter) // which contains a TerminalParameter
				{
					String assemblyFactoryParameterFactoryName = "";
					
					String assemblyParameterName        = "notFound";
					String assemblyParameterType        = "null";
					String assemblyParameterValue       = "";
					String assemblyParameterDescription = "";
		 TerminalParameter assemblyFactoryParameterTerminalParameter = null;
		 
					assemblyFactoryParameterTerminalParameter = ((TerminalParameter)(((FactoryParameter) jaxbObject).getParameters()).get(0));
					assemblyParameterName        = assemblyFactoryParameterTerminalParameter.getName();
					assemblyParameterType        = assemblyFactoryParameterTerminalParameter.getType(); // object type
					assemblyParameterValue       = // no value attribute for Parameter element
					assemblyParameterDescription = assemblyFactoryParameterTerminalParameter.getDescription();
					
					return new ViskitInstantiator.Factory(
							assemblyParameterType,      // typeName
							((FactoryParameter)jaxbObject).getName(),   // factoryClassName
							assemblyParameterName,                      // methodName
							buildInstantiators(((FactoryParameter) jaxbObject).getParameters())); // parametersList						
                } 
				else if (jaxbObject instanceof Parameter)  // from InstantiationPanel Const getter
				{
					methodName = ((Parameter)jaxbObject).getName();
				}
				if ((methodName == null) || methodName.isEmpty())
				{
					methodName = "notFound";
				}
			}
            return new ViskitInstantiator.Factory(
                    factoryParameter.getType(),      // typeName
					factoryParameter.getFactory(),   // factoryClassName
					methodName,                      // methodName
                    buildInstantiators(objectList)); // parametersList, recevies a TerminalParameter
        }

        final boolean parametersListsMatch(List<Object> assemblyParameters, List<Object> eventGraphParameters)
		{
            if (assemblyParameters.isEmpty() && eventGraphParameters.isEmpty())
			{
                if (viskit.ViskitStatics.debug)
				{
                    LOG.info("Match: zero parameters.");
                }
                return true;
            }
            if (assemblyParameters.size() != eventGraphParameters.size())
			{
                if (viskit.ViskitStatics.debug)
				{
                    LOG.info("No match.");
                }
                return false;
            }
			
            for (int i = 0; i < assemblyParameters.size(); i++) // check each pair for type match
			{
                String eventGraphTypeName = ((Parameter)eventGraphParameters.get(i)).getType();
				
                Object jaxbObject = assemblyParameters.get(i);
                String assemblyTypeName;
                if (jaxbObject instanceof TerminalParameter) // check if caller is sending assembly parameter types
				{
                    assemblyTypeName = ((TerminalParameter) jaxbObject).getType();
                } 
				else if (jaxbObject instanceof MultiParameter) 
				{
                    assemblyTypeName = ((MultiParameter) jaxbObject).getType();
                } 
				else if (jaxbObject instanceof FactoryParameter)  // which contains a TerminalParameter
				{
                    assemblyTypeName = ((FactoryParameter) jaxbObject).getType();
                } 
				else if (jaxbObject instanceof Parameter) // from InstantiationPanel, this could also be an eventgraph parameter type
				{
                    assemblyTypeName = ((Parameter) jaxbObject).getType();
                } 
				else 
				{
                    return false; // invalid type doesn't match
                }
                if (viskit.ViskitStatics.debug)
				{
                    LOG.info("Single type match, parameter " + i + ": " + assemblyTypeName + " to " + eventGraphTypeName);
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
                boolean match = (eventGraphClass.isAssignableFrom(assemblyClass) | interfaceMatch);
                if (!match)
				{
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("No match.");
                    }
                    return false;
                }
            }
            if (viskit.ViskitStatics.debug)
			{
                LOG.info("Overall match.");
            }
            return true; // no problems found, each list of parameters matches the other
        }

        /**
         * Find the names of the arguments
         * @param typeName
         * @param arguments List of ViskitInstantiators
         * @return true if argument names have been found
         */
        private boolean findArgumentNames(String typeName, List<Object> arguments)
		{
            if (arguments == null)
			{
                setParametersFactoryList(getDefaultParameters(typeName));
                arguments = getParametersFactoryList();
            }
            return (indexOfArgumentNames(typeName, arguments) >= 0); // true if index found, apparently simple type if -1
        }

        /** Find a typeName match in the ClassLoader of the given EventGraph's parameters
         *
         * @param typeName the EventGraph parameter to check
         * @param arguments a list of EG parameters
         * @return the index into the found matching constructor
         */
        public int indexOfArgumentNames(String typeName, List<Object> arguments)
		{
            int constructorIndex = -1;
            List<Object>[] parameterArrayList = ViskitStatics.resolveParametersUsingReflection(ViskitStatics.classForName(typeName));

            if (parameterArrayList == null)
			{
                return constructorIndex;
            }
            int index = 0;

            if (viskit.ViskitStatics.debug)
			{
                LOG.info("arguments length " + arguments.size());
                LOG.info("indexOfArgumentNames " + typeName + " parameterArrayList.length is " + parameterArrayList.length);
            }
            for (List<Object> parameter : parameterArrayList)
			{
                if (viskit.ViskitStatics.debug)
				{
                    LOG.info("parameter.size() " + parameter.size());
                }
                if (parameter.size() == arguments.size())
				{
                    boolean match = true;
                    for (int j = 0; j < arguments.size(); j++) {

                        if (viskit.ViskitStatics.debug) {
                            LOG.info("touching " +
                                    ViskitStatics.convertClassName(
                                            ((Parameter)parameter.get(j)).getType())
                                    + " "
                                    + ((ViskitInstantiator) arguments.get(j)).getTypeName());
                        }
                        String parameterTypeName     = ViskitStatics.convertClassName(((Parameter)parameter.get(j)).getType());
                        String vInstantiatorTypeName = ((ViskitInstantiator) arguments.get(j)).getTypeName();

                        // check if vType was assignable from pType.

                        Class<?> parameterClass = ViskitStatics.classForName(parameterTypeName);

                        if (parameterClass == null) {
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
                            for (Class<?> vInstantiatorInterfaceClass : vInstantiatorInterfaces) 
							{
                                //interfz |= vInterfz[k].isAssignableFrom(pClazz);
                                interfaceMatch |= parameterClass.isAssignableFrom(vInstantiatorInterfaceClass);
                            }

                            match &= (parameterClass.isAssignableFrom(vInstantiatorClass) | interfaceMatch);

                            // set the names, the final iteration of while cleans up
                            if (!((ViskitInstantiator) (arguments.get(j))).getName().equals(((Parameter)parameter.get(j)).getName()))
							{
								((ViskitInstantiator) (arguments.get(j))).setName(((Parameter)parameter.get(j)).getName());
								if (viskit.ViskitStatics.debug)
								{
									LOG.info(" to " + ((Parameter)parameter.get(j)).getName());
								}
							}
                        }
                    }
                    if (match)
					{
                        constructorIndex = index;
                        break;
                    }
                }
                index++;
            }
            if ((viskit.ViskitStatics.debug) && (constructorIndex >= 0))
			{
                LOG.info("Resolving " + typeName + " " + parameterArrayList[constructorIndex] + " at index " + constructorIndex);
            }
            // the class manager caches Parameter List jaxb from the SimEntity.
            // If it didn't come from XML, then a null is returned.

            return constructorIndex;
        }

        private List<Object> getDefaultParameters(String typeName)
		{
            Class<?> inputClass = ViskitStatics.classForName(typeName);
            if (inputClass != null)
			{
                Constructor[] reflectionConstructors = inputClass.getConstructors();
                if (reflectionConstructors != null && reflectionConstructors.length > 0)
				{
                    // TODO: May need to revisit why we are just concerned with 
                    // the default zero-parameter constructor
                    return ViskitInstantiator.buildInstantiatorsFromReflection(reflectionConstructors);
                }
            }
            return new Vector<>(); // null
        }

        public List<Object> getParametersFactoryList()
		{
            return viskitParametersFactoryList;
        }

        public final void setParametersFactoryList(List<Object> parametersFactoryList) 
		{
            this.viskitParametersFactoryList = parametersFactoryList;
        }

        @Override
        public String toString() 
		{
            String result = "new " + getTypeName() + "(";
            result += (viskitParametersFactoryList.size() > 0 ? ((ViskitInstantiator) viskitParametersFactoryList.get(0)).getTypeName() + ",..." : "");
            return result + ")";
        }

		@Override
        public ViskitInstantiator.Construct vcopy()
		{
            Vector<Object> objectVector = new Vector<>();
            for (Object viskitObject : viskitParametersFactoryList)
			{
				if (viskitObject instanceof ViskitInstantiator)
				{
					objectVector.add(((ViskitInstantiator)viskitObject).vcopy()); // type-aware copy
				} 
				else
					LOG.error ("Unknown type during ViskitInstantiator.Construct.vcopy of viskitParametersFactoryList");
            }
            ViskitInstantiator.Construct resultViskitInstantiator = new ViskitInstantiator.Construct(getTypeName(), objectVector);
            resultViskitInstantiator.setName(this.getName()); // TODO not finding anything - never originally set?
            resultViskitInstantiator.setDescription(this.getDescription());
            return resultViskitInstantiator;
        }
		
        @Override
        public boolean isValid()
		{
            if (getTypeName() == null || getTypeName().isEmpty())
			{
                return false;
            }
            for (Object viskitObject : viskitParametersFactoryList)
			{
                ViskitInstantiator viskitInstantiator = (ViskitInstantiator) viskitObject;
                if (!viskitInstantiator.isValid())
				{
                    return false;
                }
            }
            return true;
        }
    }

    /***********************************************************************/
    public static class Array extends ViskitInstantiator {

        private List<Object> instantiators; // array dimension == size()

        public Array(String typeName, List<Object> instantiatorsList) {
            super(typeName);
            setInstantiators(instantiatorsList);
        }

        public ViskitInstantiator.Array vcopy()
		{
            Vector<Object> objectVector = new Vector<>();
			
            for (Object instantiator : instantiators) // instantiator likely of a different sutype than Array
			{
				if (instantiator instanceof ViskitInstantiator)
					objectVector.add(((ViskitInstantiator) instantiator).vcopy());
				else
					LOG.error ("Unknown type during ViskitInstantiator.Array.vcopy of instantiators");
            }
            ViskitInstantiator.Array resultViskitInstantiator = new ViskitInstantiator.Array(getTypeName(), objectVector);
            resultViskitInstantiator.setName(getName());
            resultViskitInstantiator.setDescription(getDescription());
            return resultViskitInstantiator;
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
        public boolean isValid()
		{
            if (getTypeName() == null || getTypeName().isEmpty())
			{
                return false;
            }
            for (Object viskitInstantiator : instantiators)
			{
                if (!((ViskitInstantiator) viskitInstantiator).isValid())
				{
                    return false;
                }
            }
            return true;
        }
    }

    /***********************************************************************/
    public static class Factory extends ViskitInstantiator
	{
        private String factoryClassName;
        private String methodName;
        private List<Object> parametersList;

        /** A factory for the ViskitInstantiator which carries information on what
         *  type of variable we need to provide for a SimEntity constructor.
         *
         * @param typeName Object type required by a SimEntity constructor
         * @param factoryClassName the class that will return this type
         * @param methodName the method of the factoryClass that will return our desired type
         * @param parametersList the parameters required to return the desired type
         */
        public Factory(String typeName, String factoryClassName, String methodName, List<Object> parametersList) 
		{
            super(typeName);
            setFactoryClassName(factoryClassName);
            setMethodName(methodName);
            setParametersList(parametersList);
        }

        public String getFactoryClass() 
		{
            return factoryClassName;
        }

        public String getMethodName() 
		{
            return methodName;
        }

        public List<Object> getParametersList() 
		{
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
        public String toString()
		{
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

                if (o instanceof ViskitInstantiator) {
                    args = ((ViskitInstantiator)o).type;
                } else if (o instanceof String) {
                    args = (String) o;
                }

                // Strip out java.lang
                args = ViskitStatics.stripOutJavaDotLang(args);

                // Show varargs symbol vice []
                if (ViskitGlobals.instance().isArray(args)) {
                    args = ViskitStatics.applyVarArgSymbol(args);
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
        public ViskitInstantiator.Factory vcopy()
		{
            Vector<Object> objectList = new Vector<>();
            ViskitInstantiator vi;
            for (Object parameter : parametersList) 
			{
                if (parameter instanceof ViskitInstantiator) // parameter may be another ViskitInstantiatortype
				{
                    vi = (ViskitInstantiator) parameter;
                    objectList.add(vi.vcopy());
                } 
				else if (parameter instanceof String) 
				{
                    objectList.add((String) parameter);
                }
				else
					LOG.error ("Unknown type during ViskitInstantiator.Factory.vcopy of parametersList");
            }
            ViskitInstantiator.Factory resultVInstantiator = new ViskitInstantiator.Factory(getTypeName(), getFactoryClass(), getMethodName(), objectList);
            resultVInstantiator.setName(getName());
            resultVInstantiator.setDescription(getDescription());
            return resultVInstantiator;
        }

        @Override
        public boolean isValid()
		{
            String typeName = getTypeName(), factoryClassName = getFactoryClass(), methodName = getMethodName();
            if (typeName == null   || factoryClassName == null   || methodName == null ||
                typeName.isEmpty() || factoryClassName.isEmpty() || methodName.isEmpty())
			{
                return false;
            }
            for (Object o : parametersList) {

                if (o instanceof ViskitInstantiator) 
				{
                    ViskitInstantiator v = (ViskitInstantiator) o;
                    if (!v.isValid())
					{
                        return false;
                    }
                } 
				else if (o instanceof String)
				{
                    if (((String) o).isEmpty())
					{
                        return false;
                    }
                }
            }
            return true; // no problems found
        }
    }
}
