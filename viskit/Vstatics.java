package viskit;

import javax.swing.*;
import java.awt.*;

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
    Dimension d = new Dimension(h.getPreferredSize().height,w.getPreferredSize().width);
    c.setMaximumSize(d);
    c.setMinimumSize(d);
  }

  /**
   * Clamp the height of comp to it's preferred height
   * @param comp
   */
  public static void clampHeight(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
    comp.setMinimumSize(new Dimension(Integer.MAX_VALUE,d.height));
  }

  public static Class ClassForName(String s)
  {
    Class c = null;
    try {
      c = Class.forName(s);
      return c;
    }
    catch (ClassNotFoundException e) {
      c = tryPrimsAndArrays(s);
      if(c == null) {
        c = tryCommonClasses(s);
        if(c == null)
          System.err.println("what to do here... "+s);
      }
      return c;
  }

  }
  static class retrnChar
  {
    char c;
  }
  static Class tryCommonClasses(String s) {
    String conv = commonExpansions(s);
    if(conv == null)
      return null;
    try {
      return Class.forName(conv);
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
  static Class tryPrimsAndArrays(String s) {
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
      c = Class.forName(ns,false,Vstatics.class.getClassLoader());
      return c;
    }
    catch (ClassNotFoundException e) {
      // one last check
      if(commonExpansions(name) != null)
      {
        return tryPrimsAndArrays(s.replaceFirst(name,commonExpansions(name)));
      }
      return null;
    }

  }

}
