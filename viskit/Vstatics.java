package viskit;

import edu.nps.util.SimpleDirectoriesAndJarsClassLoader;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import viskit.xsd.bindings.eventgraph.ObjectFactory;
import viskit.xsd.bindings.eventgraph.ParameterType;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 17, 2004
 * Time: 8:27:07 AM
 */

public class Vstatics
{
  /**
   * Convert a class name to human readable form.  (See Class.getName());
   * @param s from Class.getName()
   * @return readable version
   */
  public static String convertClassName(String s)
  {
    if(s.charAt(0) != '[')
      return s;

    int dim = 0;
    StringBuffer sb = new StringBuffer();
    for(int i=0;i<s.length();i++) {
      if(s.charAt(i) == '['){
        dim ++;
        sb.append("[]");
      }
      else
        break;
    }

    String brackets = sb.toString();

    char ty = s.charAt(dim);
    s = s.substring(dim+1);
    switch(ty)
    {
      case 'Z': return "boolean" + brackets;
      case 'B': return "byte" + brackets;
      case 'C': return "char" + brackets;
      case 'L': return s.substring(0,s.length()-1) + brackets;  // lose the ;
      case 'D': return "double" + brackets;
      case 'F': return "float" + brackets;
      case 'I': return "int" + brackets;
      case 'J': return "long" + brackets;
      case 'S': return "short" + brackets;
      default:
        return "bad parse";
    }
  }
  /**
   * Clamp the size of c to the preferred height of h and the preferred width of w
   * @param c
   * @param h
   * @param w
   */
  public static void clampSize(JComponent c, JComponent h, JComponent w)
  {
    Dimension d = new Dimension(h.getPreferredSize().width,w.getPreferredSize().height);
    c.setMaximumSize(d);
    c.setMinimumSize(d);
  }

  /**
   * Set the size(s) of c to be exactly those of src
   * @param c
   * @param src
   */
  public static void cloneSize(JComponent c, JComponent src)
  {
    Dimension d = new Dimension(src.getPreferredSize());
    c.setMaximumSize(d);
    c.setMinimumSize(d);
    c.setPreferredSize(d);
  }
  /**
   * Clamp the height of comp to it's preferred height
   * @param comp
   */
  public static void clampHeight(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
    comp.setMinimumSize(new Dimension(Integer.MIN_VALUE,d.height));
  }
  /**
   * Clamp the height of a component to another's height
   * @param c
   * @param h
   */
  public static void clampHeight(JComponent c, JComponent h)
  {
    int height =  h.getPreferredSize().height;
    Dimension dmx = c.getMaximumSize();
    Dimension dmn = c.getMinimumSize();
    //c.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));
    //c.setMinimumSize(new Dimension(Integer.MIN_VALUE,height));
    c.setMaximumSize(new Dimension(dmx.width,height));
    c.setMinimumSize(new Dimension(dmn.width,height));
  }

  static String getCustomClassPath()
  {
    // The order of the class path is 1) work dir, 2) extra paths, 3) existing classpath
    String sep = Vstatics.getPathSeparator();
    StringBuffer cPath = new StringBuffer();

    String appclassPath = System.getProperty("java.class.path");
    File workDir = VGlobals.instance().getWorkDirectory();
    // if the workDir is not already in the list, add it
    if(appclassPath.indexOf(workDir.getAbsolutePath()) == -1){
      cPath.append(workDir.getAbsolutePath());
      cPath.append(sep);
    }
    cPath.append(getExtraClassPaths());
    cPath.append(appclassPath);
    return cPath.toString();
  }

  static String getExtraClassPaths()
  {
    String sep = Vstatics.getPathSeparator();
    StringBuffer cPath = new StringBuffer();

    String[] extraPaths = SettingsDialog.getExtraClassPath();
    if(extraPaths != null && extraPaths.length>0) {
      for(int i=0;i<extraPaths.length;i++) {
        cPath.append(extraPaths[i]);
        cPath.append(sep);
      }
    }
    return cPath.toString();
  }
  static String[] getExtraClassPathArray()
  {
    return SettingsDialog.getExtraClassPath();
  }

  /**
   * Call this method to inst a class representation of an entity.  We'll try first
   * the "standard" classpath-classloader, then try to inst any that were loaded by file.
   * @param s
   * @return class
   */
  public static Class classForName(String s)
  {
    Class c = cForName(s,Vstatics.class.getClassLoader());
    if(c == null)
      c = FileBasedClassManager.inst().getFileClass(s);
    if(c == null)
      c = cForName(s,VGlobals.instance().getWorkClassLoader());
    if(c == null)
      c = cForName(s,new SimpleDirectoriesAndJarsClassLoader(Vstatics.class.getClassLoader(),getExtraClassPathArray()));
    return c;
  }

  static Class cForName(String s, ClassLoader clsLoader)
  {
    Class c = null;
    try {
      c = Class.forName(s,false,clsLoader); //true,clsLoader);
      return c;
    }
    catch (ClassNotFoundException e) {
      c = tryPrimsAndArrays(s,clsLoader);
      if(c == null) {
          c = tryCommonClasses(s,clsLoader);
          if(c == null) {
              try {
                  c = Thread.currentThread().getContextClassLoader().loadClass(s);
              } catch (ClassNotFoundException cnfe ) {
                  ; //System.err.println("Vstatics what to do here... "+s);
              }
          }
      }
      return c;
    }
  }
  static class retrnChar
  {
    char c;
  }
  static Class tryCommonClasses(String s, ClassLoader cLdr) {
    String conv = commonExpansions(s);
    if(conv == null)
      return null;
    try {
      return Class.forName(conv,false,cLdr); // test 26JUL04 true,cLdr);
    }
    catch(Exception e) {
      return null;
    }
  }
  static String commonExpansions(String s)
  {
    if(s.equals("String"))
      return "java.lang.String";
    if(s.equals("Object"))
      return "java.lang.Object";
    if(s.equals("Queue"))
      return "java.util.Queue";
    return null;
  }
  static Class tryPrimitive(String s) {
    return tryPrimitive(s,new retrnChar());
  }

  static Class tryPrimitive(String s, retrnChar rc) {
    if(s.equals("long")){
      rc.c = 'J';
      return long.class;
    }
    else if(s.equals("float")) {
      rc.c = 'F';
      return float.class;
    }
    else if(s.equals("char")) {
      rc.c = 'C';
      return char.class;
    }
    else if(s.equals("int")) {
      rc.c = 'I';
      return int.class;
    }
    else if(s.equals("short")) {
      rc.c = 'S';
      return short.class;
    }
    else if(s.equals("double")) {
      rc.c = 'D';
      return double.class;
    }
    else if(s.equals("byte")) {
      rc.c = 'B';
      return byte.class;
    }
    else if(s.equals("boolean")) {
      rc.c = 'Z';
      return boolean.class;
    }
    else
      return null;
  }
  static Class tryPrimsAndArrays(String s, ClassLoader cLdr) {
    String[] spl = s.split("\\[");
    boolean isArray = spl.length > 1;
    char prefix = ' ';
    String name = "";
    char suffix = ' ';
    retrnChar rc = new retrnChar();
    Class c = tryPrimitive(spl[0],rc);

    if(c != null) {   // primitive
      if(isArray)
        prefix = rc.c;
      else
        return c;
    }
    else {        // object
      name = spl[0];
      if(isArray) {
        prefix = 'L';
        suffix = ';';
      }

    }
    StringBuffer sb = new StringBuffer();
    if(isArray)
      for(int i=0;i<spl.length-1;i++)
        sb.append('[');

    sb.append(prefix);
    sb.append(name);
    sb.append(suffix);
    String ns = sb.toString().trim();

    try {
      c = Class.forName(ns,false,cLdr); //Vstatics.class.getClassLoader());
      return c;
    }
    catch (ClassNotFoundException e) {
      // one last check
      if(commonExpansions(name) != null)
      {
        return tryPrimsAndArrays(s.replaceFirst(name,commonExpansions(name)),cLdr);
      }
      return null;
    }

  }
  static public String getPathSeparator()
  {
    return System.getProperty("path.separator");
  }
  static public String getFileSeparator()
  {
    return System.getProperty("file.separator");
  }
  
  static HashMap parameterMap = new HashMap();
  static void putParameterList(String type, List[] p) {
      System.out.println("Vstatics putting "+type+" "+p);
      parameterMap.remove(type);
      parameterMap.put(type,p);
  }
  
  
  static public List[] resolveParameters(String type) {
      List[] resolved = (List[])(parameterMap.get(type));
      if (resolved == null) { // taken from LegosTree addJarCommon(), tbd refactor it
          Class c = classForName(type);
          System.out.println("adding "+c.getName());
          ObjectFactory of = new ObjectFactory();
          Constructor[] constr = c.getConstructors();
          ArrayList[] plist = new ArrayList[constr.length];
          
          // at this point, there should be loaded classes
          // from LegosTree, however, addJarFileCommon is only
          // looking for SimEntity types (TBD could refactor this better
          // as this code block is directly from LegosTree.addJarFileCommon()
          Field f = null;
          try {
              f = c.getField("parameterMap");
          } catch (SecurityException ex) {
              ex.printStackTrace();
          } catch (NoSuchFieldException ex) {
              ;
          }
          if (f != null) { // these would be base classes not arrays
              System.out.println(f+" is a parameterMap");
              try {
                  // parameters are in the following order
                  // {
                  //  { "type0","name0","type1","name1",... }
                  //  { "type0","name0", ... }
                  //  ...
                  // }
                  String[][] parameterMap = (String[][])(f.get(new String[0][0]));
                  int numConstrs = parameterMap.length;
                  
                  for (int n = 0; n < numConstrs; n++) {
                      String[] params = parameterMap[n];
                      plist[n] = new ArrayList();
                      for (int k = 0; k < params.length; k+=2) {
                          try {
                              ParameterType p = of.createParameter();
                              String ptype = params[k];
                              String pname = params[k+1];
                              
                              p.setName(pname);
                              p.setType(ptype);
                              plist[n].add(p);
                              System.out.println("\tfrom compiled parameterMap" + p.getName() + p.getType());
                          } catch (Exception e) {
                              e.printStackTrace();
                          }
                      }
                  }
              } catch (IllegalArgumentException ex) {
                  ex.printStackTrace();
              } catch (IllegalAccessException ex) {
                  ex.printStackTrace();
              }
          } else {
              System.out.println("\t # constructors: "+constr.length);
              for ( int i = 0; i < constr.length; i ++ ) {
                  Class[] ptypes = constr[i].getParameterTypes();
                  plist[i] = new ArrayList();
                  System.out.println("\t # params "+ptypes.length+" in constructor "+i);
                  for ( int k = 0; k < ptypes.length; k++ ) {
                      try {
                          ParameterType p = of.createParameter();
                          String ptname = ptypes[k].getName();
                          if ( ptname.indexOf(".class")>0 ) { //??
                              ptname = ptname.split("\\.")[0];
                          }
                          // could be from class loader, which would
                          // prepend [L, etc. to an array, fix it up here
                          if ( ptname.startsWith("[")) {
                              System.out.println("[] an array "+ptname);
                              // java has it if array of some type then [Lclassname, long
                              // pointer? so for all cases of prims [x except [L
                              // then just convert to full name, otherwise if begins with [L
                              // check if length of string is > 2, then it is [classname
                              // also, note name is followed by ;
                              if (ptname.length() == 2) { // must be a prim type
                                  if (ptname.equals("[B")) {
                                      ptname = "byte[]";
                                  } else if (ptname.equals("[C")) {
                                      ptname = "char[]";
                                  } else if (ptname.equals("[D")) {
                                      ptname = "double[]";
                                  } else if (ptname.equals("[F")) {
                                      ptname = "float[]";
                                  } else if (ptname.equals("[I")) {
                                      ptname = "int[]";
                                  } else if (ptname.equals("[J")) {
                                      ptname = "long[]";
                                  } else if (ptname.equals("[S")) {
                                      ptname = "short[]";
                                  } else if (ptname.equals("[Z")) {
                                      ptname = "boolean[]";
                                  }
                              } else {
                                  ptname = convertClassName(ptname);
                              }
                          }
                          p.setName("p["+k+"] : ");
                          p.setType(ptname);
                          plist[i].add(p);
                          System.out.println("\t "+p.getName()+p.getType());
                      } catch (Exception e) {
                          e.printStackTrace();
                      }
                  }
              }
          }
          //putParameterList(c.getName(),plist);
          putParameterList(type,plist);
          resolved = plist;
      }
      return resolved;
      
  }
  
  // check if primitive type in Viskit format ie not Clazz format
  public static boolean isPrimitive(String type) {
      return type.equals("byte") | type.equals("boolean") 
        | type.equals("char") | type.equals("double") 
        | type.equals("float") | type.equals("int") 
        | type.equals("short"); 
  }
  
  // returns number of constructors, checks is [] type
  public static int numConstructors(String type) {
      if (type.endsWith("]")) {
          return 1;
      } else {
          Class clz = classForName(type);
          Constructor[] constrs = clz.getConstructors();
          return constrs.length;
      }
  }
  
}
