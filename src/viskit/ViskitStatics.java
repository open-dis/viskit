/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

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
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
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
import edu.nps.util.Log4jUtilities;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
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

import org.apache.logging.log4j.Logger;

import viskit.control.FileBasedClassManager;
import viskit.doe.LocalBootLoader;
import viskit.xsd.bindings.eventgraph.ObjectFactory;
import viskit.xsd.bindings.eventgraph.Parameter;

/** 
 * Viskit classes and methods, provided as static references and links for internal visibility to all other code in Viskit.
 * <pre>
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
    /** Inaccessible private constructor to prevent external instantiation, 
     * replacing default Java constructor which is otherwise created automatically.
     *@see https://stackoverflow.com/questions/13773710/can-a-class-have-no-constructor
     */
    private ViskitStatics()
    {
        // unreachable externally and not used inside this class
        // LOG message notifies if this occurs more than once
        LOG.error("created private unreachable constructor with static initialization block");
    }
    static
    {
        // ourobouros issue: having an external checker does not work, because 
        // ViskitStatics would also need to be a singleton as well.
        // ---------------------------------------------------------------------
        // precaution: deliberately initialize singleton classes during startup, 
        // avoiding potential interference by other threads, and  initializing
        // counts for singleton safety checks below
        // ViskitGlobals            temp1 = ViskitGlobals.instance();
        // ViskitConfigurationStore temp2 = ViskitConfigurationStore.instance();
    }
        // private static int            viskitGlobalsCreationCount = 0; // singleton safety check
        // private static int viskitConfigurationStoreCreationCount = 0; // singleton safety check
    
    /* Commonly used class names */
    public static final String RANDOM_NUMBER_CLASS                   = "simkit.random.RandomNumber";
    public static final String RANDOM_VARIATE_CLASS                  = "simkit.random.RandomVariate";
    public static final String RANDOM_VARIATE_FACTORY_CLASS          = RANDOM_VARIATE_CLASS + "Factory";
    public static final String RANDOM_VARIATE_FACTORY_DEFAULT_METHOD = "getInstance";
    public static final String SIMPLE_PROPERTY_DUMPER                = "simkit.util.SimplePropertyDumper";
    public static final String LOCAL_BOOT_LOADER                     = "viskit.doe.LocalBootLoader";
    public static final String JAVA_LANG_STRING                      = "java.lang.String";
    public static final String JAVA_LANG_OBJECT                      = "java.lang.Object";
    public static final String VISKIT_MAILING_LIST                   = "brutzman@nps.edu;terry.norbraten@gmail.com";

    public static final String FULL_PATH       = "FULLPATH";
    public static final String CLEAR_PATH_FLAG = "<<clearPath>>";

    public static final String OPERATING_SYSTEM = System.getProperty("os.name");

    static final Logger        LOG   = Log4jUtilities.getLogger(ViskitStatics.class);

    public static boolean      debug = false;

    public static String DESCRIPTION_HINT = "Good descriptions reveal model meaning and author intent";

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
     * Clamp the size of component c to the preferred height of component h and the preferred width of component w
     * @param targetComponent the component to size clamp
     * @param heightComponent use height of this component
     * @param widthComponent  use width  of this component
     */
    public static void clampComponentSize(JComponent targetComponent, JComponent heightComponent, JComponent widthComponent) {
        Dimension dimension = new Dimension(widthComponent.getPreferredSize().width, 
                                          heightComponent.getPreferredSize().height);
        targetComponent.setMaximumSize(dimension);
        targetComponent.setMinimumSize(dimension);
    }

    public static void clampMaxSize(JComponent c) {
        Dimension d = new Dimension(c.getPreferredSize());
        c.setMaximumSize(d);
    }

    public static void clampComponentSize(JComponent component) {
        ViskitStatics.clampComponentSize(component, component, component);
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
     * Call this method to instantiate a class representation of an entity. We'll try first
     * the "standard" classpath-classloader, then try to instantiate any that were loaded by file.
     * @param s the name of the class to instantiate
     * @return an instantiated class given by s if available from the loader
     */
    public static Class<?> classForName(String s) {
        Class<?> c = cForName(s, ViskitGlobals.instance().getViskitApplicationClassLoader());

        if (c == null) {
            c = tryUnqualifiedName(s);
        }

        if (c == null) {
            c = FileBasedClassManager.instance().getFileClass(s);
        }

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
            c = tryPrimitivesAndArrays(s, clsLoader);
            if (c == null) {
                c = tryCommonClasses(s, clsLoader);
                if (c == null) {
                    try {
                        c = clsLoader.loadClass(s);
                    } catch (ClassNotFoundException cnfe) {
                        // sometimes happens, ignore
                    }
                }
            }
        } 
        catch (NoClassDefFoundError e)
        {
            ViskitGlobals.instance().getMainFrame().genericReport(
                    JOptionPane.ERROR_MESSAGE,
                    "Missng: " + e.getMessage(),
                    "Please make sure that the library for: " + s
                            + "\nis in the project classpath, then restart Viskit");
        }
        return c;
    }

    static class ReturnChar {
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

            case "TreeSet":
                retVal = "java.util.TreeSet";
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

//    static Class<?> tryPrimitive(String s) {
//        return tryPrimitive(s, new ReturnChar());
//    }

    static Class<?> tryPrimitive(String s, ReturnChar rc) {
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

    static Class<?> tryPrimitivesAndArrays(String s, ClassLoader cLdr) {
        String[] spl = s.split("\\[");
        boolean isArray = spl.length > 1;
        char prefix = ' ';
        String name = "";
        char suffix = ' ';
        ReturnChar rc = new ReturnChar();
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
                return tryPrimitivesAndArrays(s.replaceFirst(name, commonExpansions(name)), cLdr);
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

        String systemUserDir  = System.getProperty("user.dir");
        String systemUserHome = System.getProperty("user.home");
        String workDir = ViskitGlobals.instance().getProjectWorkingDirectory().getPath();

        FindFile finder;
        Path startingDir;
        String pattern = name + "\\.class";
        Class<?> c = null;
        LocalBootLoader loader = (LocalBootLoader) ViskitGlobals.instance().getViskitApplicationClassLoader();
        String[] classpaths = loader.getClassPath();
        String clazz;

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
                    clazz = finder.getPath().toString();

                    // Strip out unwanted prepaths
                    if (clazz.contains(systemUserHome))
                        clazz = clazz.substring(systemUserHome.length() + 1, clazz.length());
                    else if (clazz.contains(systemUserDir))
                        clazz = clazz.substring(systemUserDir.length() + 1, clazz.length());
                    else if (clazz.contains(workDir))
                        clazz = clazz.substring(workDir.length() + 1, clazz.length());

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

    public static String getPathSeparator() {
        return System.getProperty("path.separator");
    }

    public static String getFileSeparator() {
        return System.getProperty("file.separator");
    }

    static Map<String, List<Object>[]> parameterMap = new HashMap<>();

    /**
     * For the given class type Event Graph, record its specific ParameterMap
     * @param type the Event Graph class name
     * @param p a List of parameter map object arrays
     */
    public static void putParameterList(String type, List<Object>[] p) {
        if (debug) {
            LOG.info("ViskitStatics putting " + type + " " + Arrays.toString(p));
        }
        parameterMap.remove(type);
        parameterMap.put(type, p);
    }

    /** Checks for and return a varargs type as an array, or the orig type
     *
     * @param type the Class type to check
     * @return return a varargs type as an array, or the orig. type
     */
    public static Class<?> getClassForInstantiatorType(String type) {
        Class<?> c;
        if (type.contains("Object...")) {
            c = classForName("java.lang.Object[]");
        } else {
            c = classForName(type);
        }
        return c;
    }

    /**
     * For the given Event Graph class type, return its specific ParameterMap contents.
     * A LOG error will report if something is missing on the classpath
     *
     * @param type the Event Graph class type to resolve
     * @return a List of parameter map object arrays
     */
    @SuppressWarnings("unchecked")
    public static List<Object>[] resolveParameters(Class<?> type) {

        // nulls can occur when staring Viskit pointing to an assembly in another
        // project space. In this case we can be silent
        if (type == null)
            return null;

        // Ben Cheng NPE fix
        Object testResult = parameterMap.get(type.getName());
        List<Object>[] resolved = null;
        if (testResult != null) {
            resolved = (List<Object>[]) testResult;
            LOG.debug("parameter {} already resolved", type);
        }
        if (resolved == null) {
            Constructor<?>[] constr;
            try {
                constr = type.getConstructors();
            } catch (NoClassDefFoundError er) {
                LOG.error(er);
                return null;
            }
            List<Object>[] plist = GenericConversion.newListObjectTypeArray(List.class, constr.length);
            ObjectFactory of = new ObjectFactory();
            Field f = null;

            try {
                f = type.getField("parameterMap");
            } catch (SecurityException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            } catch (NoSuchFieldException ex) {}

            if (viskit.ViskitStatics.debug) {
                LOG.info("adding " + type.getName());
                LOG.info("\t # constructors: " + constr.length);
            }

            Class<?>[] ptypes;
            ParameterMap param;
            String[] names, types;
            Parameter pt, p;
            String[] params;
            String[][] pMap;
            int numConstrs;
            Annotation[] paramAnnots;
            String ptype, pname, ptType;
            for (int i = 0; i < constr.length; i++) {
                ptypes = constr[i].getParameterTypes();
                paramAnnots = constr[i].getDeclaredAnnotations();
                plist[i] = new ArrayList<>();
                if (viskit.ViskitStatics.debug)
                    LOG.debug("\t # params {} in constructor {}", ptypes.length, i);

                // possible that a class inherited a parameterMap, check if annotated first
                if (paramAnnots != null && paramAnnots.length > 0) {
                    if (paramAnnots.length > 1)
                        throw new RuntimeException("Only one Annotation per constructor"); // TODO: harsh

                    param = constr[i].getAnnotation(viskit.ParameterMap.class);
                    if (param != null) {
                        names = param.names();
                        types = param.types();
                        if (names.length != types.length)
                            throw new RuntimeException("ParameterMap names and types length mismatch"); // TODO: harsh

                        for (int k = 0; k < names.length; k++) {
                            pt = of.createParameter();
                            pt.setName(names[k]);
                            pt.setType(types[k]);

                            plist[i].add(pt);
                        }
                    }

                } else if (f != null) {
                    if (viskit.ViskitStatics.debug)
                        LOG.debug("{} is a parameterMap", f);

                    try {
                        // parameters are in the following order
                        // {
                        //  { "type0","name0","type1","name1",... },
                        //  { "type0","name0", ... },
                        //  ...
                        // }
                        pMap = (String[][]) (f.get(new String[0][0]));
                        numConstrs = pMap.length;

                        for (int n = 0; n < numConstrs; n++) { // tbd: check that numConstrs == constr.length
                            params = pMap[n];
                            if (params != null) {
                                plist[n] = new ArrayList<>();
                                for (int k = 0; k < params.length; k += 2) {
                                    try {
                                        p = of.createParameter();
                                        ptype = params[k];
                                        pname = params[k + 1];

                                        p.setName(pname);
                                        p.setType(ptype);

                                        plist[n].add(p);
                                        if (viskit.ViskitStatics.debug)
                                            LOG.debug("\tfrom compiled parameterMap {}", p.getName() + p.getType());

                                    }
                                    catch (Exception ex) {
                                        LOG.error("resolveParameters(" + type + ") initial-loop exception: " + ex.getMessage());
                                    }
                                }
                            }
                        }
//                        break; // fix this up, should index along with i not n
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOG.error(ex);
//                        ex.printStackTrace();
                    }
                } else { // unknown
                    int k = 0;
                    for (Class<?> ptyp : ptypes) {
                        try {
                            p = of.createParameter();
                            ptType = ViskitStatics.convertClassName(ptyp.getName());
                            if (ptType.indexOf(".class") > 0) { //??
                                ptType = ptType.split("\\.")[0];
                            }

                            // Not sure what use a name like this is for PCLs
                            p.setName("p[" + k++ + "] : ");
                            p.setType(ptType);
                            plist[i].add(p);
                            if (viskit.ViskitStatics.debug)
                                LOG.info("\t {}{}", p.getName(), p.getType());

                        }
                        catch (Exception ex) {
                            LOG.error("resolveParameters(" + type + ") secondary-loop exception: " + ex.getMessage());
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
        if (s.contains("[]"))
            s = s.replaceAll("\\[\\]", "...");
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
        if (ViskitGlobals.instance().isArray(type)) {
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
                        LOG.info(constrs.length);
                    }
                    return constrs.length;
                }
            } else {
                return 0;
            }
        }
    }

    /** Utility method to bring up a hyperlinked message to allow the user to
     * email the error.log to the Viskit mailing list if the user's machine has
     * an installed email client
     *
     * @param parent the parent component to center the JOptionPane panel
     * @param cause a throwable instance name to reference
     * @param url a URL used to populate an email form
     * @param msg the message to inform the user with
     * @param showLog a flag to denote showing the debug.log in an output text editor
     */
    public static void showHyperlinkedDialog(Component parent, String cause, final URL url, String msg, final boolean showLog) 
    {
        // TODO move this together with other notifiers
        // Bugfix 1377

        // for copying style
        JLabel label = new JLabel();
        Font font = label.getFont();

        // create some css from the label's font
        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
        style.append("font-size:").append(font.getSize()).append("pt;");

        // html content
        JEditorPane ep = new JEditorPane("text/html",
                "<html><body style=\"" + style + "\">"
                + msg + "</body></html>");

        // handle link events to bring up mail client and debug.log
        ep.addHyperlinkListener((HyperlinkEvent e) -> {
            try {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    Desktop.getDesktop().mail(url.toURI());

                    if (showLog)
                        Desktop.getDesktop().browse(ViskitConfigurationStore.VISKIT_ERROR_LOG.toURI());
                }
            } catch (IOException | URISyntaxException ex) {
                LOG.error(ex);
            }
        });
        ep.setEditable(false);
        ep.setBackground(label.getBackground());

        JOptionPane.showMessageDialog(parent, ep, cause, JOptionPane.ERROR_MESSAGE);
    }

//    /**
//     * @return the viskitGlobalsCreationCount singleton safety check
//     */
//    public static synchronized int getViskitGlobalsCreationCount() {
//        return viskitGlobalsCreationCount;
//    }
//
//    /**
//     * Trust but verify: increment the singleton safety check value with each 
//     * instantiation, warn if more than one occurs
//     */
//    public static synchronized void incrementViskitGlobalsCreationCount() {
//        viskitGlobalsCreationCount++;
//        if (viskitGlobalsCreationCount > 1) // unexpected failure
//        {
//            LOG.error("Singleton safety check failed, viskitGlobalsCreationCount=" + 
//                                                      viskitGlobalsCreationCount);
//            String message = "<html><body><p align='center'>ViskitGlobals singleton safety check failure!</p><br />";
//            message +=       "<p align='center'>viskitGlobalsCreationCount=" + viskitGlobalsCreationCount + "</p><br />";
//            // watch out for dueling creation problems
////            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
////                "Singleton safety check error", message);
//        }
//    }
//
//    /**
//     * @return the viskitConfigurationStorCreationCount singleton safety check
//     */
//    public static synchronized int getViskitConfigurationStoreCreationCount() {
//        return viskitConfigurationStoreCreationCount;
//    }
//
//    /**
//     * Trust but verify: increment the singleton safety check value with each
//     * instantiation, warn if more than one occurs
//     */
//    public static synchronized void incrementViskitConfigurationStoreCreationCount() {
//        viskitConfigurationStoreCreationCount++;
//        if (viskitConfigurationStoreCreationCount > 1) // unexpected failure
//        {
//            LOG.error("Singleton safety check failed, viskitConfigurationStoreCreationCount=" + 
//                                                      viskitConfigurationStoreCreationCount);
//            String message = "<html><body><p align='center'>ViskitConfigurationStore singleton safety check failure!</p><br />";
//            message +=       "<p align='center'>viskitConfigurationStoreCreationCount=" + viskitConfigurationStoreCreationCount + "</p><br />";
//            // watch out for dueling creation problems
////            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
////                "Singleton safety check error", message);
//        }
//    }

} // end class ViskitStatics
