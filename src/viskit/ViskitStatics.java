/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit;

import edu.nps.util.FindFile;
import edu.nps.util.GenericConversion;
import edu.nps.util.LogUtilities;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import viskit.control.EventGraphController;
import viskit.control.FileBasedClassManager;
import viskit.doe.LocalBootLoader;
import viskit.xsd.bindings.eventgraph.ObjectFactory;
import viskit.xsd.bindings.eventgraph.Parameter;

/** <pre>
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * </pre>
 * @author Mike Bailey
 * @since Jun 17, 2004
 * @since 8:27:07 AM
 * @version $Id$
 */
public class ViskitStatics
{
    static final Logger LOG = LogUtilities.getLogger(ViskitStatics.class);

    public static boolean debug = true; // TODO expose?

    /* Commonly used class names */
    public static final String RANDOM_NUMBER_CLASS_NAME  = "simkit.random.RandomNumber";
    public static final String RANDOM_VARIATE_CLASS_NAME = "simkit.random.RandomVariate";
    public static final String RANDOM_VARIATE_FACTORY_CLASS = RANDOM_VARIATE_CLASS_NAME + "Factory";
    public static final String RANDOM_VARIATE_FACTORY_DEFAULT_METHOD = "getInstance";
    public static final String SIMPLE_PROPERTY_DUMPER = "simkit.util.SimplePropertyDumper";
    public static final String LOCAL_BOOT_LOADER = "viskit.doe.LocalBootLoader";
    public static final String JAVA_LANG_STRING  = "java.lang.String";
    public static final String JAVA_LANG_OBJECT  = "java.lang.Object";
	
    public static final String TOOLTIP_EVENTGRAPH_ASSEMBLY_EDITOR_TAB_COLORS = "green = saved with compile success, red = modifications need saving or compile failure";
    public static final String DEFAULT_DESCRIPTION       = "TODO add description"; // better to nag than ignore
    public static final String TOOLTIP_EVENT_ARGUMENTS   = "Define initialization arguments for this Event, passed by a Schedulng Edge";
    public static final String TOOLTIP_LOCAL_VARIABLES   = "Define local variables for this Event, persistent and usable in State Transition computations and code block";
    public static final String TOOLTIP_CODE_BLOCK        = "Define a Java source-code block for this Event (advanced feature, not recommended)";
    public static final String TOOLTIP_STATE_TRANSITIONS = "Define state transitions for this Event";

    public static final String FULL_PATH = "FULLPATH";
    public static final String CLEAR_PATH_FLAG = "<<clearPath>>";

    public static final String OPERATING_SYSTEM = System.getProperty("os.name");
	
	public static final double DEFAULT_ZOOM             = 1.50d;
	public static final double DEFAULT_ZOOM_INCREMENT   = 0.25d;
	public static final int    DEFAULT_SELECT_TOLERANCE =  4; // pixels, used for seleting an object, jGraph default is 4
	
    // apparently jGraph has a factor-of-2 error when applying grid size; see ViskitGlobals.snapToGrid() method
	public static final int    DEFAULT_GRID_SIZE        = 10; // pixels, also applied to minimumMove
	public static final int    DEFAULT_GRID_SNAP        = 10; // pixels, also used for minimumMove
	
	public final static String         VISKIT_READY_MESSAGE = "Visual Simkit is ready to go!";
	public final static String PROJECT_OPEN_SUCCESS_MESSAGE = "Project opened successfully!";
	public final static String PROJECT_OPEN_FAILURE_MESSAGE = "Project failed to open...";
	public final static String             RECENTER_SPACING = " &nbsp &nbsp &nbsp &nbsp ";

    /** Utility method to configure a Viskit project
     *
     * @param projectDirectory the base directory of a Viskit project
     */
    @SuppressWarnings("unchecked")
    public static void setViskitProjectDirectory (File projectDirectory)
	{
		// must be parent directory!! combination of values results in actual directorn 
        ViskitProject.MY_VISKIT_PROJECTS_DIR = projectDirectory.getParent().replaceAll("\\\\", "/"); // normalize
		// ViskitConfiguration.instance() creates configuration (if needed) before saving values
        ViskitConfiguration.instance().setValue(ViskitConfiguration.PROJECT_PATH_KEY, ViskitProject.MY_VISKIT_PROJECTS_DIR);
        ViskitProject.DEFAULT_PROJECT_NAME = projectDirectory.getName();
        ViskitConfiguration.instance().setValue(ViskitConfiguration.PROJECT_NAME_KEY, ViskitProject.DEFAULT_PROJECT_NAME);

        XMLConfiguration projectHistoryConfiguration = ViskitConfiguration.instance().getViskitApplicationXMLConfiguration();
        List<String> projectHistoryList = projectHistoryConfiguration.getList(ViskitConfiguration.PROJECT_HISTORY_KEY + "[@value]");
		if (projectHistoryList == null)
			projectHistoryList = new ArrayList<>();
        boolean match = false;
        for (String priorProjectDirectory : projectHistoryList) 
		{
            if (priorProjectDirectory.equals(projectDirectory.getPath()))
			{
                match = true;
                break;
            }
        }
        if (!match) 
		{
            projectHistoryConfiguration.setProperty(ViskitConfiguration.PROJECT_HISTORY_KEY + "(" + projectHistoryList.size() + ")[@value]", projectDirectory.getPath());
            projectHistoryConfiguration.getDocument().normalize();
        }
    }

    /**
     * Convert a class name array type to human readable form.
     * (See Class.getName());
     *
     * @param s from Class.getName()
     * @return readable version of array type
     */
    public static String convertClassName(String s) {
        if (s.charAt(0) != '[') {
            return s;
        }

        int dim = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '[') {
                dim++;
                sb.append("[]");
            } else {
                break;
            }
        }

        String brackets = sb.toString();

        char ty = s.charAt(dim);
        s = s.substring(dim + 1);
        switch (ty) {
            case 'Z':
                return "boolean" + brackets;
            case 'B':
                return "byte" + brackets;
            case 'C':
                return "char" + brackets;
            case 'L':
                return s.substring(0, s.length() - 1) + brackets;  // lose the ;
            case 'D':
                return "double" + brackets;
            case 'F':
                return "float" + brackets;
            case 'I':
                return "int" + brackets;
            case 'J':
                return "long" + brackets;
            case 'S':
                return "short" + brackets;
            default:
                return "bad parse";
        }
    }

    /**
     * Clamp the size of c to the preferred height of h and the preferred width of w
     * @param c the component to size clamp
     * @param h component height
     * @param w component width
     */
    public static void clampSize(JComponent c, JComponent h, JComponent w) {
        Dimension d = new Dimension(h.getPreferredSize().width, w.getPreferredSize().height);
        c.setMaximumSize(d);
        c.setMinimumSize(d);
    }

    public static void clampMaxSize(JComponent c) {
        Dimension d = new Dimension(c.getPreferredSize());
        c.setMaximumSize(d);
    }

    public static void clampSize(JComponent c) {
        clampSize(c, c, c);
    }

    /**
     * Set the size(s) of c to be exactly those of src
     * @param c the component who's size is to be clamped
     * @param src the source component to clamp size to
     */
    public static void cloneSize(JComponent c, JComponent src) {
        Dimension d = new Dimension(src.getPreferredSize());
        c.setMaximumSize(d);
        c.setMinimumSize(d);
        c.setPreferredSize(d);
    }

    /**
     * Clamp the height of a component to it's preferred height
     * @param comp the component who's height is to be clamped
     */
    public static void clampHeight(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        comp.setMinimumSize(new Dimension(Integer.MIN_VALUE, d.height));
    }

    /**
     * Clamp the height of a component to another's height
     * @param c the component who's height is to be clamped
     * @param h the height to clamp to
     */
    public static void clampHeight(JComponent c, JComponent h) {
        int height = h.getPreferredSize().height;
        Dimension dmx = c.getMaximumSize();
        Dimension dmn = c.getMinimumSize();
        //c.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));
        //c.setMinimumSize(new Dimension(Integer.MIN_VALUE,height));
        c.setMaximumSize(new Dimension(dmx.width, height));
        c.setMinimumSize(new Dimension(dmn.width, height));
    }

    /**
     * Exec a file.
     * @param path fully qualified path and filename
     * @return null if exec was ok, else error message
     */
    public static String runOSFile(String path) {
        Runtime run = Runtime.getRuntime();
        try {
            if (OPERATING_SYSTEM.contains("Mac")) {
                run.exec(new String[]{"open", path});
            } else if (OPERATING_SYSTEM.contains("Win")) {
                run.exec(new String[]{"start", "iexplore", path});
            } else {
                run.exec(new String[]{path});
            }
        } catch (IOException e) {
            return e.getMessage();
        }
        return null;
    }

    /**
     * Call this method to instantiate a class representation of an entity.  We'll try first
     * the "standard" classpath-classloader, then try to instantiate any that were loaded by file.
     * @param className the name of the class to instantiate
     * @return an instantiated class given by s if available from the loader
     */
    public static Class<?> ClassForName(String className)
	{
        Class<?> classRetrieved = ClassFromName(className, ViskitGlobals.instance().getWorkClassLoader());

        if (classRetrieved == null) // check if found, retry if needed
		{
            classRetrieved = tryUnqualifiedName(className);
        }
        if (classRetrieved == null)  // check if found, retry if needed
		{
            classRetrieved = FileBasedClassManager.instance().getFileClass(className);
        }
        if (classRetrieved == null)  // check if found, report if not
		{
            LOG.error("ClassForName(" + className + ") unsuccessful");
        }
        return classRetrieved;
    }

    /** Convenience method in a series of chains for resolving a class that is
     * hopefully on the classpath
     *
     * @param className the name of the class to search for
     * @param classLoader the class loader to search
     * @return an instantiated class object from the given name
     */
    static Class<?> ClassFromName(String className, ClassLoader classLoader) 
	{
        Class<?> classRetrieved = null;
        try {
            classRetrieved = Class.forName(className, false, classLoader);
        } 
		catch (ClassNotFoundException e) 
		{
            classRetrieved = tryPrimsAndArrays(className, classLoader);
            if (classRetrieved == null) 
			{
                classRetrieved = tryCommonClasses(className, classLoader);
                if (classRetrieved == null) 
				{
                    try {
                        classRetrieved = ViskitGlobals.instance().getWorkClassLoader().loadClass(className);
                    } 
					catch (ClassNotFoundException cnfe) 
					{
                        // sometimes happens, ignore
						LOG.error ("ClassNotFoundException", cnfe);
                    }
                }
            }
        } 
		catch (NoClassDefFoundError e) 
		{
			String title   = "Missing: " + e.getMessage();
			String message = "Please make sure that the library for: " + className
                            + "\nis in the project classpath, then restart Viskit";
			
            ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).messageToUser(
                    JOptionPane.ERROR_MESSAGE,
                    title, message);
			LOG.error (title);
			LOG.error (message);
        }
        return classRetrieved;
    }

    static class ReturnChar {
        char c;
    }

    static Class<?> tryCommonClasses(String s, ClassLoader classLoader) 
	{
        String conv = commonExpansions(s);
        if (conv == null) {
            return null;
        }
        try {
            return Class.forName(conv, false, classLoader); // test 26JUL04 true,cLdr);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /** Convenience method for expanding unqualified (common) types used in Viskit
     *
     * @param s the string of the unqualified type
     * @return the qualified type
     */
    static String commonExpansions(String s)
	{
        String returnValue;

        switch (s) 
		{
            case "String":
                returnValue = JAVA_LANG_STRING;
                break;

            case "Object":
                returnValue = JAVA_LANG_OBJECT;
                break;

            case "Queue":
                returnValue = "java.util.Queue";
                break;

            case "TreeSet":
                returnValue = "java.util.TreeSet";
                break;

            case "RandomNumber":
                returnValue = RANDOM_NUMBER_CLASS_NAME;
                break;

            case "RandomVariate":
                returnValue = RANDOM_VARIATE_CLASS_NAME;
                break;

            default:
                returnValue = null;
        }
        return returnValue;
    }

    static Class<?> tryPrimitive(String s) {
        return tryPrimitive(s, new ReturnChar());
    }

    static Class<?> tryPrimitive(String s, ReturnChar rc)
	{
        switch (s) 
		{
            case "long":
                rc.c = 'J';
                return long.class;
            case "float":
                rc.c = 'F';
                return float.class;
            case "char":
                rc.c = 'C';
                return char.class;
            case "int":
                rc.c = 'I';
                return int.class;
            case "short":
                rc.c = 'S';
                return short.class;
            case "double":
                rc.c = 'D';
                return double.class;
            case "byte":
                rc.c = 'B';
                return byte.class;
            case "boolean":
                rc.c = 'Z';
                return boolean.class;
            default:
                return null;
        }
    }

    static Class<?> tryPrimsAndArrays(String s, ClassLoader classLoader) 
	{
        String[] splittee = s.split("\\[");
        boolean isArray = splittee.length > 1;
        char prefix = ' ';
        String name = "";
        char suffix = ' ';
        ReturnChar returnChar = new ReturnChar();
        Class<?> c = tryPrimitive(splittee[0], returnChar);

        if (c != null) {   // primitive
            if (isArray) {
                prefix = returnChar.c;
            } else {
                return c;
            }
        } else {        // object
            name = splittee[0];
            if (isArray) {
                prefix = 'L';
                suffix = ';';
            }

        }
        StringBuilder sb = new StringBuilder();
        if (isArray) {
            for (int i = 0; i < (splittee.length - 1); i++) {
                sb.append('[');
            }
        }

        sb.append(prefix);
        sb.append(name);
        sb.append(suffix);
        String ns = sb.toString().trim();

        try {
            c = Class.forName(ns, false, classLoader);
            return c;
        } 
		catch (ClassNotFoundException e) 
		{
            // one last check
            if (commonExpansions(name) != null) 
			{
                return tryPrimsAndArrays(s.replaceFirst(name, commonExpansions(name)), classLoader);
            }
            return null;
        }
    }

    /** Attempt to resolve an unqualified to a qualified class name.  This only
     * works for classes that are on the classpath that are not contained in a
     * jar file
     *
     * @param name the unqualified class name to resolve
     * @return a fully resolved class on the classpath
     */
    static Class<?> tryUnqualifiedName(String name) 
	{
        String userDir  = System.getProperty("user.dir");
        String userHome = System.getProperty("user.home");
        String workDir  = ViskitGlobals.instance().getWorkDirectory().getPath();

        FindFile finder;
        Path startingDirectory;
        String pattern = name + "\\.class";
        Class<?> c = null;
        LocalBootLoader loader = (LocalBootLoader)ViskitGlobals.instance().getWorkClassLoader();
        String[] classpaths = loader.getClassPath();

        for (String classpath : classpaths) 
		{
            // We can deal with jars w/the SimpleDirectoriesAndJarsClassLoader
            if (classpath.contains(".jar")) {continue;}

            startingDirectory = Paths.get(classpath);
            finder = new FindFile(pattern);

            try {
                Files.walkFileTree(startingDirectory, finder);
            } 
			catch (IOException e) 
			{
                LOG.error("Files.walkFileTree(startingDirectory, finder)", e);
            }

            try {
                if (finder.getPath() != null) 
				{
                    String clazz = finder.getPath().toString();

                    // Strip out unwanted prepaths
                    if (clazz.contains(userHome)) {
                        clazz = clazz.substring(userHome.length() + 1, clazz.length());
                    } else if (clazz.contains(userDir)) {
                        clazz = clazz.substring(userDir.length() + 1, clazz.length());
                    } else if (clazz.contains(workDir)) {
                        clazz = clazz.substring(workDir.length() + 1, clazz.length());
                    }

                    // Strip off .class and replace File.separatorChar w/ a "."
                    clazz = clazz.substring(0, clazz.lastIndexOf(".class"));
                    clazz = clazz.replace(File.separatorChar, '.');

                    c = Class.forName(clazz, false, loader);
                    break;
                }
            } catch (ClassNotFoundException e) {}

        }

        return c;
    }

    static public String getPathSeparator() 
	{
        return System.getProperty("path.separator");
    }

    static public String getFileSeparator() 
	{
        return System.getProperty("file.separator");
    }

    static Map<String, List<Object>[]> parameterHashMap = new HashMap<>();

    /**
     * For the given class type Event Graph, record its specific ParameterMap
     * @param typeName the Event Graph class name
     * @param parameterMapObjectArray a List of parameter map object arrays
     */
    static public void putParameterListInHashMap(String typeName, List<Object>[] parameterMapObjectArray)
	{
        if (debug) 
		{
            LOG.info("ViskitStatics putting " + typeName + " " + Arrays.toString(parameterMapObjectArray));
        }
        parameterHashMap.remove(typeName); // ensure correct
        parameterHashMap.put(typeName, parameterMapObjectArray);
    }

    /** Checks for and return a varargs type as an array, or the original type
     *
     * @param type the Class type to check
     * @return return a varargs type as an array, or the orig. type
     */
    static public Class<?> getClassForInstantiatorType(String type) {
        Class<?> c;
        if (type.contains("Object...")) {
            c = ClassForName("java.lang.Object[]");
        } else {
            c = ClassForName(type);
        }
        return c;
    }

    /**
     * For the given Event Graph class type, return its specific ParameterMap contents using reflection techniques.
     *
     * @param eventGraphType the Event Graph class type to resolve
     * @return a List of parameter map object arrays
     */
    static public List<Object>[] resolveParametersUsingReflection(Class<?> eventGraphType)
	{
		if (eventGraphType == null)
			return null;
		
        List<Object>[] resolvedParameterList = null;
		
		// test
		if ((parameterHashMap != null) && (eventGraphType.getName() != null) && (parameterHashMap.get(eventGraphType.getName()) != null))
		{
			resolvedParameterList = parameterHashMap.get(eventGraphType.getName());
			LOG.info("parameters already resolved"); // TODO confirm OK, are we all done?
		}
		
        if (resolvedParameterList == null)
		{
            Constructor<?>[] reflectionConstructors  = eventGraphType.getConstructors(); // reflection
            List<Object>[]   reflectionParameterList = GenericConversion.newListObjectTypeArray(List.class, reflectionConstructors.length);
            Annotation[]     reflectionParameterAnnotations;
			
            ObjectFactory jaxbObjectFactory = new ObjectFactory();
            Field jaxbField = null;

            try {
                jaxbField = eventGraphType.getField("parameterMap"); // TODO confirm correct name, are we finding it?
            } 
			catch (SecurityException ex)
			{
                LOG.error("SecurityException when accessing parameterMap: " + ex);
            }
			catch (NoSuchFieldException ex)
			{
				// null result for creating field; keep looknig
			}

            if (viskit.ViskitStatics.debug)
			{
                LOG.info("adding " + eventGraphType.getName());
                LOG.info("\t # constructors: " + reflectionConstructors.length);
            }

            for (int i = 0; i < reflectionConstructors.length; i++)
			{
                Class<?>[] reflectionParameterClassTypes  = reflectionConstructors[i].getParameterTypes();
                           reflectionParameterAnnotations = reflectionConstructors[i].getDeclaredAnnotations();
                reflectionParameterList[i] = new ArrayList<>();
                if (viskit.ViskitStatics.debug) 
				{
                    LOG.info("\tparameter count=" + reflectionParameterClassTypes.length + " in constructor " + i);
                }

                // possible that a class inherited an annotated parameterHashMap, check that first
                if (    reflectionParameterAnnotations != null &&     reflectionParameterAnnotations.length > 0)
				{
                    if (    reflectionParameterAnnotations.length > 1)
					{
						String message = "Only one Annotation allowed per constructor, found " +     reflectionParameterAnnotations.length;
						LOG.error (message);
                        throw new RuntimeException(message);
                    }

                    ParameterMap annotationParameterMap = reflectionConstructors[i].getAnnotation(viskit.ParameterMap.class);
                    if (annotationParameterMap != null)
					{
                        String[] names        = annotationParameterMap.names();
                        String[] types        = annotationParameterMap.types();
                        String[] descriptions = annotationParameterMap.descriptions();
                        if (names.length != types.length)
						{
                            throw new RuntimeException("ParameterMap names and types length mismatch");
                        }
                        for (int k = 0; k < names.length; k++)
						{
                            Parameter parameter = jaxbObjectFactory.createParameter();
                            parameter.setName(names[k]);
                            parameter.setType(types[k]);
                            parameter.setDescription(descriptions[k]);

                            reflectionParameterList[i].add(parameter);
                        }
                    }

                } 
				else if (jaxbField != null) // known field
				{
                    if (viskit.ViskitStatics.debug)
					{
                        LOG.info(jaxbField + " is a parameterMap");
                    }
                    try {
                        // parameters are in the following order
                        // {
                        //  { "type0","name0","type1","name1",... }
                        //  { "type0","name0", ... }
                        //  ...
                        // }
                        String[][] parameterMap = (String[][]) (jaxbField.get(new String[0][0]));
                        int constructorCount = parameterMap.length;
						// TODO check that constructorCount == constr.length

                        for (int n = 0; n < constructorCount; n++) 
						{
                            String[] parameterValueArray = parameterMap[n];
                            if (parameterValueArray != null) {
                                reflectionParameterList[n] = new ArrayList<>();
                                for (int k = 0; k < parameterValueArray.length; k += 2)
								{
                                    try {
                                        Parameter parameter = jaxbObjectFactory.createParameter();
                                        String parameterTypeName    = parameterValueArray[k];
                                        String parameterName        = parameterValueArray[k + 1];
                                        String parameterDescription = DEFAULT_DESCRIPTION; // TODO fix

                                        parameter.setName       (parameterName);
                                        parameter.setType       (parameterTypeName);
                                        parameter.setDescription(parameterDescription);

                                        reflectionParameterList[n].add(parameter);
                                        if (viskit.ViskitStatics.debug)
										{
                                            LOG.info("\tfrom compiled parameterMap: " + parameter.getName() + " " + parameter.getType());
                                        }
                                    } catch (Exception ex) {
                                        LOG.error(ex);
//                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
//                        break; // fix this up, should index along with i not n
                    } 
					catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOG.error(ex);
                    }
                } 
				else // unknown field
				{
                    int k = 0;
                    for (Class<?> reflectionParameterClassType : reflectionParameterClassTypes)
					{
                        try {
                            Parameter newParameter = jaxbObjectFactory.createParameter();
                            String newParameterTypeName = ViskitStatics.convertClassName(reflectionParameterClassType.getName());
                            if (newParameterTypeName.indexOf(".class") > 0)
							{
                                newParameterTypeName = newParameterTypeName.substring(0,newParameterTypeName.indexOf(".class")); // omit .class from type name
                            }
							// TODO apparently no way to get Javadoc from reflection for this field parameter
							String newParameterDescription = DEFAULT_DESCRIPTION; // better to nag than ignore

                            // Not sure what use a name like this is for PCLs
                            newParameter.setName("parameter[" + k++ + "]"); // TODO can't reflection get method name?
                            newParameter.setType(newParameterTypeName);
                            newParameter.setDescription(newParameterDescription);
							
                            reflectionParameterList[i].add(newParameter);
                            if (viskit.ViskitStatics.debug)
							{
                                LOG.info("\t " + newParameter.getType() + " " + newParameter.getName());
                            }
                        } 
						catch (Exception ex)
						{
                            LOG.error(ex);
                        }
                    }
                }
            }
            putParameterListInHashMap(eventGraphType.getName(), reflectionParameterList);
            resolvedParameterList = reflectionParameterList;
        }
        return resolvedParameterList;
    }

    /**
     * Strips out the qualified header, java.lang
     * @param s the string to strip
     * @return a stripped string
     */
    public static String stripOutJavaDotLang(String s) {
        if (s.contains("java.lang.")) {
            s = s.replace("java.lang.", "");
        }
        return s;
    }

    /**
     * Strips out the array brackets [] and replaces with vararg ellipsis ...
     * @param s the string to make into varargs form, if applicable
     * @return original or modified string
     */
    public static String applyVarArgSymbol(String s) 
	{
        // Show varargs ellipsis symbol vice []
        if (s.contains("[]")) 
		{
            s = s.replaceAll("\\[\\]", "...");
        }
        return s;
    }

    /** Checks if primitive type in Viskit format, i.e. not Class format
     * @param type the type to evaluate and determine if a primitive
     * @return true if the given string represents a primitive type
     */
    public static boolean isPrimitive(String type) 
	{
        return  type.equals("byte")    |
                type.equals("boolean") |
                type.equals("char")    |
                type.equals("double")  |
                type.equals("float")   |
                type.equals("int")     |
                type.equals("short")   |
                type.equals("long");
    }

    /**
	 * Uses reflection to count the number of constructors for the given class type
     * @param type the type class for searching constructors
     * @return number of constructors, checks is [] type
     */
    public static int numberOfConstructors(String type) 
	{
        //
        if (debug) 
		{
            LOG.info("number of constructors for " + type + ":");
        }
        if (ViskitGlobals.instance().isArray(type)) 
		{
            if (debug) 
			{
                LOG.info("1");
            }
            return 1;
        } 
		else 
		{
            Class<?> clazz = ClassForName(type);
            if (clazz != null)
			{
                Constructor[] reflectionConstructors = clazz.getConstructors();
                if (reflectionConstructors == null) 
				{
                    return 0;
                } 
				else
				{
                    if (debug) 
					{
                        LOG.info(reflectionConstructors.length);
                    }
                    return reflectionConstructors.length;
                }
            } 
			else 
			{
				if (debug) 
				{
					LOG.info("0");
				}
                return 0;
            }
        }
    }

    /** Utility method to bring up a hyperlinked message to allow the user to
 email the debug.LOG to the Viskit mailing list if the user's machine has
 an installed email client
     *
     * @param parent the parent component to center the JOptionPane panel
     * @param title dialog title, such as a trouble cause or throwable instance name to reference
     * @param url a URL used to populate an email form
     * @param message the message to inform the user with
     * @param showLog a flag to denote showing the debug.LOG in an output text editor
     */
    public static void showHyperlinkedDialog(Component parent, String title, final URL url, String message, final boolean showLog)
	{
        // for copying style
        JLabel label = new JLabel();
        Font font = label.getFont();

        // create some css from the label's font
        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
        style.append("font-size:").append(font.getSize()).append("pt;");

        // html content
        JEditorPane editorPane = new JEditorPane("text/html",
                "<html><body style=\"" + style + "\">"
                + message + "</body></html>");

        // handle link events to bring up mail client and debug.LOG
        editorPane.addHyperlinkListener(new HyperlinkListener()
		{
            @Override
            public void hyperlinkUpdate(HyperlinkEvent event)
			{
                try {
                    if (    event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED) // never reached if tracing this line in debug mode, ENTERED occurs first
//						|| (event.getEventType().equals(HyperlinkEvent.EventType.ENTERED) && ViskitStatics.debug)
					   )
					{
                        if (showLog)
                            Desktop.getDesktop().browse(ViskitConfiguration.VISKIT_DEBUG_LOG.toURI());

                        Desktop.getDesktop().mail(url.toURI()); // mail invoked second, thus goes on top
                    }
                } 
				catch (IOException | URISyntaxException ex) 
				{
                    LOG.error(ex);
                }
            }
        });
        editorPane.setEditable(false);
        editorPane.setBackground(label.getBackground());

        JOptionPane.showMessageDialog(parent, editorPane, title, JOptionPane.INFORMATION_MESSAGE);
    }
	
	public static void sendErrorReport (Exception e)
	{
		sendErrorReport ("Viskit has experienced a significant execution problem.", e);
    }
	
	public static void sendErrorReport (String preamble, Exception exception)
	{
			if (exception != null) // save exception stack trace to log as error
			{
				// http://stackoverflow.com/questions/1149703/how-can-i-convert-a-stack-trace-to-a-string
				StringWriter sw = new StringWriter();
				 PrintWriter pw = new PrintWriter(sw);
				exception.printStackTrace(pw);
				LOG.error (exception.getMessage());
				LOG.error (sw.toString()); // stack trace as a string
			}
			
            URL mailtoUrl = null;
			String mailtoString = new String();
            try {
				// http://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
//				byte[] encoded = Files.readAllBytes(Paths.get(ViskitConfiguration.VISKIT_DEBUG_LOG.getAbsolutePath()));
				byte[] encoded = ViskitConfiguration.VISKIT_DEBUG_LOG.getAbsolutePath().getBytes();
				String logText = new String (encoded);
                mailtoUrl = new URL("mailto:" + ViskitConfiguration.VISKIT_MAILING_LIST +
                        "?subject=Visual%20Simkit%20(viskit)%20execution%20trouble%20report&body=Please%20describe%20what%20happened%20while%20using%20Visual%20Simkit:%0D%0A%0D%0A%0D%0A%0D%0A"
						+ "To%20help%20debug,%20also%20please%20copy%20and%20paste%20the%20debug.log%20output:%0D%0A%0D%0A%0D%0A%0D%0A"
						+ "Thanks%20for%20using%20Visual%20Simkit!%20%20" 
						// http://stackoverflow.com/questions/724043/http-url-address-encoding-in-java
						+ java.net.URLEncoder.encode(ViskitConfiguration.VISKIT_WEBSITE_URL, "ISO-8859-1") // escape url for email
//						+ logText // not working, probably clobbers mailer
				);
				mailtoString = "<a href='" + mailtoUrl.toString()+ "'>" + ViskitConfiguration.VISKIT_MAILING_LIST + "</a>";
            } 
			catch (Exception ex) 
			{
                LOG.error(ex);
			}
            String message = "<html>"
					+ "<p align='center'>" + preamble + ViskitStatics.RECENTER_SPACING + "</p>"
//                  + "<p align='center'>Execution LOG details are available at " + ViskitConfiguration.VISKIT_DEBUG_LOG.getPath() + ViskitStatics.RECENTER_SPACING + "</p>"
                    + "<p align='center'>Please view and email the session log to " 
					  + "<i>" + mailtoString  + "</i>" + ViskitStatics.RECENTER_SPACING + "</p>"
					+ "<p align='center'>Click the link above to draft an email, then copy &amp; paste the log's contents." + ViskitStatics.RECENTER_SPACING + "</p>"
					+ "<p align='center'>Thanks!" + ViskitStatics.RECENTER_SPACING + ViskitStatics.RECENTER_SPACING + "</p>";

			String title = ViskitConfiguration.VISKIT_FULL_APPLICATION_NAME;
			if (exception != null)
				   title = exception.toString();
            ViskitStatics.showHyperlinkedDialog(null, title, mailtoUrl, message, true); // need to debug, often caused by a method-naming problem		
	}
}
