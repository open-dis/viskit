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

import edu.nps.util.GenericConversion;
import edu.nps.util.LogUtils;
import edu.nps.util.SimpleDirectoriesAndJarsClassLoader;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import viskit.control.EventGraphController;
import viskit.control.FileBasedClassManager;
import viskit.view.dialog.SettingsDialog;
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

    /** Utility method to initialize and configure Viskit for a specific project
     * space
     *
     * @param projFile the base directory of a Viskit project
     */
    public static void initProjectDirectories(File projFile) {
        ViskitProject.MY_VISKIT_PROJECTS_DIR = projFile.getParent().replaceAll("\\\\", "/");
        ViskitConfig.instance().setVal(ViskitConfig.PROJECT_PATH_KEY, ViskitProject.MY_VISKIT_PROJECTS_DIR);
        ViskitProject.DEFAULT_PROJECT_NAME = projFile.getName();
        ViskitConfig.instance().setVal(ViskitConfig.PROJECT_NAME_KEY, ViskitProject.DEFAULT_PROJECT_NAME);
    }

    /**
     * Convert a class name to human readable form.  (See Class.getName());
     * @param s from Class.getName()
     * @return readable version
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
     * @param path
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

    static String[] getExtraClassPathArray() {
        return SettingsDialog.getExtraClassPath();
    }

    /**
     * Call this method to instantiate a class representation of an entity.  We'll try first
     * the "standard" classpath-classloader, then try to instantiate any that were loaded by file.
     * @param s the name of the class to instantiate
     * @return an instantiated class given by s if available from the loader
     */
    public static Class<?> classForName(String s) {
        Class<?> c;

        c = cForName(s, VGlobals.instance().getWorkClassLoader());
        if (c == null) {
            c = cForName(s, VStatics.class.getClassLoader());
        }
        if (c == null) {
            c = FileBasedClassManager.instance().getFileClass(s);
        }
        if (c == null) {
            c = cForName(s, new SimpleDirectoriesAndJarsClassLoader(VStatics.class.getClassLoader(), getExtraClassPathArray()));
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
                retVal = "java.lang.String";
                break;

            case "Object":
                retVal = "java.lang.Object";
                break;

            case "Queue":
                retVal = "java.util.Queue";
                break;

            case "RandomNumber":
                retVal = "simkit.random.RandomNumber";
                break;

            case "RandomVariate":
                retVal = "simkit.random.RandomVariate";
                break;

            case "Entity":
                retVal = "simkit.Entity";
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

    /**
     * For the given class type EG, return its specific ParameterMap
     * @param type the EG class name
     * @return a List of parameter map object arrays
     */
    static public List<Object>[] resolveParameters(String type) {
        List<Object>[] resolved = parameterMap.get(type);
        if (debug) {
            if (resolved != null) {
                System.out.println("parameters already resolved");
            }
        }
        if (resolved == null) { // taken from LegosTree addJarCommon(), tbd refactor it
            Class<?> c = classForName(type);
            if (c == null) {
                LogUtils.getLogger(VStatics.class).warn("Class file for: " + type + " was not found on the classpath");
                return null;
            }
            if (debug) {
                System.out.println("adding " + c.getName());
            }
            ObjectFactory of = new ObjectFactory();
            Constructor[] constr = c.getConstructors();
            List<Object>[] plist = GenericConversion.newListObjectTypeArray(ArrayList.class, constr.length);

            // at this point, there should be loaded classes
            // from LegosTree, however, addJarFileCommon is only
            // looking for SimEntity types (TBD could refactor this better
            // as this code block is directly from LegosTree.addJarFileCommon())
            Field f = null;
            try {
                f = c.getField("parameterMap");
            } catch (SecurityException ex) {
                LogUtils.getLogger(VStatics.class).error(ex);
            } catch (NoSuchFieldException ex) {}
            if (f != null) { // these would be base classes not arrays
                if (debug) {
                    System.out.println(f + " is a parameterMap");
                }
                try {
                    // parameters are in the following order
                    // {
                    //  { "type0","name0","type1","name1",... }
                    //  { "type0","name0", ... }
                    //  ...
                    // }
                    String[][] paramMap = (String[][]) f.get(new String[0][0]);
                    int numConstrs = paramMap.length;

                    for (int n = 0; n < numConstrs; n++) {
                        String[] params = paramMap[n];
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
                                    if (debug) {
                                        System.out.println("\tfrom compiled parameterMap" + p.getName() + p.getType());
                                    }
                                } catch (Exception e) {
                                    LogUtils.getLogger(VStatics.class).error(e);
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    LogUtils.getLogger(VStatics.class).error(ex);
                }
            } else {
                if (debug) {
                    System.out.println("\t # constructors: " + constr.length);
                }
                for (int i = 0; i < constr.length; i++) {
                    Class<?>[] ptypes = constr[i].getParameterTypes();
                    plist[i] = new ArrayList<>();
                    if (debug) {
                        System.out.println("\t # params " + ptypes.length + " in constructor " + i);
                    }
                    for (int k = 0; k < ptypes.length; k++) {
                        try {
                            Parameter p = of.createParameter();
                            String ptname = ptypes[k].getName();
                            if (ptname.indexOf(".class") > 0) { //??
                                ptname = ptname.split("\\.")[0];
                            }
                            // could be from class loader, which would
                            // prepend [L, etc. to an array, fix it up here
                            if (ptname.startsWith("[")) {
                                if (debug) {
                                    System.out.println("[] an array " + ptname);
                                }
                                // java has it if array of some type then [Lclassname, long
                                // pointer? so for all cases of prims [x except [L
                                // then just convert to full name, otherwise if begins with [L
                                // check if length of string is > 2, then it is [classname
                                // also, note name is followed by ;
                                if (ptname.length() == 2) { // must be a prim type
                                    switch (ptname) {
                                        case "[B":
                                            ptname = "byte[]";
                                            break;
                                        case "[C":
                                            ptname = "char[]";
                                            break;
                                        case "[D":
                                            ptname = "double[]";
                                            break;
                                        case "[F":
                                            ptname = "float[]";
                                            break;
                                        case "[I":
                                            ptname = "int[]";
                                            break;
                                        case "[J":
                                            ptname = "long[]";
                                            break;
                                        case "[S":
                                            ptname = "short[]";
                                            break;
                                        case "[Z":
                                            ptname = "boolean[]";
                                            break;
                                    }
                                } else {
                                    ptname = convertClassName(ptname);
                                }
                            }

                            p.setName("p[" + k + "] : ");
                            p.setType(ptname);
                            plist[i].add(p);
                            if (debug) {
                                System.out.println("\t " + p.getName() + p.getType());
                            }
                        } catch (Exception e) {
                            LogUtils.getLogger(VStatics.class).error(e);
                        }
                    }
                }
            }
            putParameterList(type, plist);
            resolved = plist;
        }
        return resolved;
    }

    /** Checks if primitive type in Viskit format, i.e. not Clazz format
     * @param type the type to evaluate and determine if a primitive
     * @return an indication of success
     */
    public static boolean isPrimitive(String type) {
        return type.equals("byte") | type.equals("boolean") | type.equals("char") | type.equals("double") | type.equals("float") | type.equals("int") | type.equals("short") | type.equals("long");
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
        if (type.endsWith("]")) {
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
