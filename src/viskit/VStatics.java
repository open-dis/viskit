/*
Copyright (c) 1995-2015 held by the author(s).  All rights reserved.

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
import edu.nps.util.LogUtils;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
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
public class VStatics {

    public static boolean debug = false;

    /* Commonly used class names */
    public static final String RANDOM_NUMBER_CLASS = "simkit.random.RandomNumber";
    public static final String RANDOM_VARIATE_CLASS = "simkit.random.RandomVariate";
    public static final String RANDOM_VARIATE_FACTORY_CLASS = RANDOM_VARIATE_CLASS + "Factory";
    public static final String RANDOM_VARIATE_FACTORY_DEFAULT_METHOD = "getInstance";
    public static final String SIMPLE_PROPERTY_DUMPER = "simkit.util.SimplePropertyDumper";
    public static final String LOCAL_BOOT_LOADER = "viskit.doe.LocalBootLoader";
    public static final String JAVA_LANG_STRING = "java.lang.String";
    public static final String JAVA_LANG_OBJECT = "java.lang.Object";
    public static final String VISKIT_MAILING_LIST = "viskit@www.movesinstitute.org";

    public static final String FULL_PATH = "FULLPATH";
    public static final String CLEAR_PATH_FLAG = "<<clearPath>>";

    static final Logger LOG = LogUtils.getLogger(VStatics.class);

    /** Utility method to configure a Viskit project
     *
     * @param projFile the base directory of a Viskit project
     */
    @SuppressWarnings("unchecked")
    public static void setViskitProjectFile(File projFile) {
        ViskitProject.MY_VISKIT_PROJECTS_DIR = projFile.getParent().replaceAll("\\\\", "/");
        ViskitConfig.instance().setVal(ViskitConfig.PROJECT_PATH_KEY, ViskitProject.MY_VISKIT_PROJECTS_DIR);
        ViskitProject.DEFAULT_PROJECT_NAME = projFile.getName();
        ViskitConfig.instance().setVal(ViskitConfig.PROJECT_NAME_KEY, ViskitProject.DEFAULT_PROJECT_NAME);

        XMLConfiguration historyConfig = ViskitConfig.instance().getViskitAppConfig();
        List<String> valueAr = historyConfig.getList(ViskitConfig.PROJ_HISTORY_KEY + "[@value]");
        boolean match = false;
        for (String s : valueAr) {
            if (s.equals(projFile.getPath())) {
                match = true;
                break;
            }
        }
        if (!match) {
            historyConfig.setProperty(ViskitConfig.PROJ_HISTORY_KEY + "(" + valueAr.size() + ")[@value]", projFile.getPath());
            historyConfig.getDocument().normalize();
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
        String os = System.getProperty("os.name");
        try {
            if (os.contains("Mac")) {
                run.exec(new String[]{"open", path});
            } else if (os.contains("Win")) {
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
     * @param s the name of the class to instantiate
     * @return an instantiated class given by s if available from the loader
     */
    public static Class<?> classForName(String s) {

        Class<?> c = cForName(s, VGlobals.instance().getWorkClassLoader());

        if (c == null) {
            c = tryUnqualifiedName(s);
        }

//        if (c == null) {
//            c = cForName(s, VStatics.class.getClassLoader());
//        }

        if (c == null) {
            c = FileBasedClassManager.instance().getFileClass(s);
        }

//        if (c == null) {
//            c = cForName(s, new SimpleDirectoriesAndJarsClassLoader(VStatics.class.getClassLoader(), getExtraClassPathArray()));
//        }

        return c;
    }

    /** Convenience method in a series of chains for resolving a class that is
     * hopefully on the classpath
     *
     * @param s the name of the class to search for
     * @param clsLoader the class loader to search
     * @return an instantiated class object from the given name
     */
    static Class<?> cForName(String s, ClassLoader clsLoader) {
        Class<?> c = null;
        try {
            c = Class.forName(s, false, clsLoader);
        } catch (ClassNotFoundException e) {
            c = tryPrimsAndArrays(s, clsLoader);
            if (c == null) {
                c = tryCommonClasses(s, clsLoader);
                if (c == null) {
                    try {
                        c = VGlobals.instance().getWorkClassLoader().loadClass(s);
                    } catch (ClassNotFoundException cnfe) {
                        // sometimes happens, ignore
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            ((EventGraphController)VGlobals.instance().getEventGraphController()).messageUser(
                    JOptionPane.ERROR_MESSAGE,
                    "Missng: " + e.getMessage(),
                    "Please make sure that the library for: " + s
                            + "\nis in the project classpath, then restart Viskit");
        }
        return c;
    }

    static class retrnChar {
        char c;
    }

    static Class<?> tryCommonClasses(String s, ClassLoader cLdr) {
        String conv = commonExpansions(s);
        if (conv == null) {
            return null;
        }
        try {
            return Class.forName(conv, false, cLdr); // test 26JUL04 true,cLdr);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /** Convenience method for expanding unqualified (common) types used in Viskit
     *
     * @param s the string of the unqualified type
     * @return the qualified type
     */
    static String commonExpansions(String s) {
        String retVal;

        switch (s) {

            case "String":
                retVal = JAVA_LANG_STRING;
                break;

            case "Object":
                retVal = JAVA_LANG_OBJECT;
                break;

            case "Queue":
                retVal = "java.util.Queue";
                break;

            case "RandomNumber":
                retVal = RANDOM_NUMBER_CLASS;
                break;

            case "RandomVariate":
                retVal = RANDOM_VARIATE_CLASS;
                break;

            default:
                retVal = null;
        }

        return retVal;
    }

    static Class<?> tryPrimitive(String s) {
        return tryPrimitive(s, new retrnChar());
    }

    static Class<?> tryPrimitive(String s, retrnChar rc) {
        switch (s) {
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

    static Class<?> tryPrimsAndArrays(String s, ClassLoader cLdr) {
        String[] spl = s.split("\\[");
        boolean isArray = spl.length > 1;
        char prefix = ' ';
        String name = "";
        char suffix = ' ';
        retrnChar rc = new retrnChar();
        Class<?> c = tryPrimitive(spl[0], rc);

        if (c != null) {   // primitive
            if (isArray) {
                prefix = rc.c;
            } else {
                return c;
            }
        } else {        // object
            name = spl[0];
            if (isArray) {
                prefix = 'L';
                suffix = ';';
            }

        }
        StringBuilder sb = new StringBuilder();
        if (isArray) {
            for (int i = 0; i < (spl.length - 1); i++) {
                sb.append('[');
            }
        }

        sb.append(prefix);
        sb.append(name);
        sb.append(suffix);
        String ns = sb.toString().trim();

        try {
            c = Class.forName(ns, false, cLdr);
            return c;
        } catch (ClassNotFoundException e) {
            // one last check
            if (commonExpansions(name) != null) {
                return tryPrimsAndArrays(s.replaceFirst(name, commonExpansions(name)), cLdr);
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
    static Class<?> tryUnqualifiedName(String name) {

        String userDir = System.getProperty("user.dir");
        String userHome = System.getProperty("user.home");
        String workDir = VGlobals.instance().getWorkDirectory().getPath();

        FindFile finder;
        Path startingDir;
        String pattern = name + "\\.class";
        Class<?> c = null;
        LocalBootLoader loader = (LocalBootLoader)VGlobals.instance().getWorkClassLoader();
        String[] classpaths = loader.getClassPath();

        for (String cpath : classpaths) {

            // We can deal with jars w/the SimpleDirectoriesAndJarsClassLoader
            if (cpath.contains(".jar")) {continue;}

            startingDir = Paths.get(cpath);
            finder = new FindFile(pattern);

            try {
                Files.walkFileTree(startingDir, finder);
            } catch (IOException e) {
                LOG.error(e);
            }

            try {
                if (finder.getPath() != null) {
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

    static public String getPathSeparator() {
        return System.getProperty("path.separator");
    }

    static public String getFileSeparator() {
        return System.getProperty("file.separator");
    }

    static Map<String, List<Object>[]> parameterMap = new HashMap<>();

    /**
     * For the given class type EG, record its specific ParameterMap
     * @param type the EG class name
     * @param p a List of parameter map object arrays
     */
    static public void putParameterList(String type, List<Object>[] p) {
        if (debug) {
            System.out.println("Vstatics putting " + type + " " + Arrays.toString(p));
        }
        parameterMap.remove(type);
        parameterMap.put(type, p);
    }

    /** Checks for and return a varargs type as an array, or the orig type
     *
     * @param type the Class type to check
     * @return return a varargs type as an array, or the orig. type
     */
    static public Class<?> getClassForInstantiatorType(String type) {
        Class<?> c;
        if (type.contains("Object...")) {
            c = classForName("java.lang.Object[]");
        } else {
            c = classForName(type);
        }
        return c;
    }

    /**
     * For the given EG class type, return its specific ParameterMap contents
     *
     * @param type the EG class type to resolve
     * @return a List of parameter map object arrays
     */
    static public List<Object>[] resolveParameters(Class<?> type) {
        List<Object>[] resolved = parameterMap.get(type.getName());
        if (debug) {
            if (resolved != null) {
                System.out.println("parameters already resolved");
            }
        }
        if (resolved == null) {

            Constructor<?>[] constr = type.getConstructors();
            Annotation[] paramAnnots;
            List<Object>[] plist = GenericConversion.newListObjectTypeArray(List.class, constr.length);
            ObjectFactory of = new ObjectFactory();
            Field f = null;

            try {
                f = type.getField("parameterMap");
            } catch (SecurityException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            } catch (NoSuchFieldException ex) {}

            if (viskit.VStatics.debug) {
                System.out.println("adding " + type.getName());
                System.out.println("\t # constructors: " + constr.length);
            }

            for (int i = 0; i < constr.length; i++) {
                Class<?>[] ptypes = constr[i].getParameterTypes();
                paramAnnots = constr[i].getDeclaredAnnotations();
                plist[i] = new ArrayList<>();
                if (viskit.VStatics.debug) {
                    System.out.println("\t # params " + ptypes.length + " in constructor " + i);
                }

                // possible that a class inherited a parameterMap, check if annotated first
                if (paramAnnots != null && paramAnnots.length > 0) {
                    if (paramAnnots.length > 1) {
                        throw new RuntimeException("Only one Annotation per constructor");
                    }

                    ParameterMap param = constr[i].getAnnotation(viskit.ParameterMap.class);
                    if (param != null) {
                        String[] names = param.names();
                        String[] types = param.types();
                        if (names.length != types.length) {
                            throw new RuntimeException("ParameterMap names and types length mismatch");
                        }
                        for (int k = 0; k < names.length; k++) {
                            Parameter pt = of.createParameter();
                            pt.setName(names[k]);
                            pt.setType(types[k]);

                            plist[i].add(pt);
                        }
                    }

                } else if (f != null) {
                    if (viskit.VStatics.debug) {
                        System.out.println(f + " is a parameterMap");
                    }
                    try {
                        // parameters are in the following order
                        // {
                        //  { "type0","name0","type1","name1",... }
                        //  { "type0","name0", ... }
                        //  ...
                        // }
                        String[][] pMap = (String[][]) (f.get(new String[0][0]));
                        int numConstrs = pMap.length;

                        for (int n = 0; n < numConstrs; n++) { // tbd: check that numConstrs == constr.length
                            String[] params = pMap[n];
                            if (params != null) {
                                plist[n] = new ArrayList<>();
                                for (int k = 0; k < params.length; k += 2) {
                                    try {
                                        Parameter p = of.createParameter();
                                        String ptype = params[k];
                                        String pname = params[k + 1];

                                        p.setName(pname);
                                        p.setType(ptype);

                                        plist[n].add(p);
                                        if (viskit.VStatics.debug) {
                                            System.out.println("\tfrom compiled parameterMap" + p.getName() + p.getType());
                                        }
                                    } catch (Exception ex) {
                                        LOG.error(ex);
//                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
//                        break; // fix this up, should index along with i not n
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOG.error(ex);
//                        ex.printStackTrace();
                    }
                } else { // unknonws
                    int k = 0;
                    for (Class<?> ptype : ptypes) {
                        try {
                            Parameter p = of.createParameter();
                            String ptType = VStatics.convertClassName(ptype.getName());
                            if (ptType.indexOf(".class") > 0) { //??
                                ptType = ptType.split("\\.")[0];
                            }

                            // Not sure what use a name like this is for PCLs
                            p.setName("p[" + k++ + "] : ");
                            p.setType(ptType);
                            plist[i].add(p);
                            if (viskit.VStatics.debug) {
                                System.out.println("\t " + p.getName() + p.getType());
                            }
                        } catch (Exception ex) {
                            LOG.error(ex);
//                            ex.printStackTrace();
                        }
                    }
                }
            }
            putParameterList(type.getName(), plist);
            resolved = plist;
        }
        return resolved;
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
     * Strips out the array brackets and replaces with ...
     * @param s the string to make varargs
     * @return a varargs type
     */
    public static String makeVarArgs(String s) {

        // Show varargs symbol vice []
        if (s.contains("[]")) {
            s = s.replaceAll("\\[\\]", "...");
        }
        return s;
    }

    /** Checks if primitive type in Viskit format, i.e. not Clazz format
     * @param type the type to evaluate and determine if a primitive
     * @return true if the given string represents a primitive type
     */
    public static boolean isPrimitive(String type) {
        return type.equals("byte") |
                type.equals("boolean") |
                type.equals("char") |
                type.equals("double") |
                type.equals("float") |
                type.equals("int") |
                type.equals("short") |
                type.equals("long");
    }

    /**
     * @param type the type class for searching constructors
     * @return number of constructors, checks is [] type
     */
    public static int numConstructors(String type) {
        //
        if (debug) {
            System.out.print("number of constructors for " + type + ":");
        }
        if (VGlobals.instance().isArray(type)) {
            if (debug) {
                System.out.print("1");
            }
            return 1;
        } else {
            Class<?> clz = classForName(type);
            if (clz != null) {
                Constructor[] constrs = clz.getConstructors();
                if (constrs == null) {
                    return 0;
                } else {
                    if (debug) {
                        System.out.println(constrs.length);
                    }
                    return constrs.length;
                }
            } else {
                return 0;
            }
        }
    }
}
