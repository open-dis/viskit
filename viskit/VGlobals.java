package viskit;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import viskit.model.*;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.lang.reflect.Array;

/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Apr 5, 2004
 * Time: 3:20:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class VGlobals
{
  private static VGlobals me;
  private Interpreter interpreter;
  private JPopupMenu moreTypesMenu;
  private ComboBoxModel cbMod;


  public static synchronized VGlobals instance()
  {
    if (me == null) {
      me = new VGlobals();
    }
    return me;
  }

  private VGlobals()
  {
    initBeanShell();
    moreTypesMenu = new JPopupMenu("more...");
    moreTypesMenu.add("java.lang.String");
    typesVector = new Vector(Arrays.asList(defaultTypes));
    //typesVector.add(moreTypesMenu);
    cbMod = new DefaultComboBoxModel(typesVector);
  }

/*
  public String[] getTypes()
  {
    return defaultTypes;
  }
*/

  private String[] defaultTypes = {
    "char",
    "boolean",
    "byte",
    "double",
    "float",
    "int",
    "short",
    "long",
    "java.lang.String",
  };

  private Vector typesVector;

  public void addType(String ty)
  {
    if (Arrays.binarySearch(defaultTypes, ty) < 0) {
// todo      moreTypesMenu.add(new JMenuItem(ty));

      String[] newArr = new String[defaultTypes.length + 1];
      System.arraycopy(defaultTypes, 0, newArr, 0, defaultTypes.length);
      newArr[newArr.length - 1] = ty;
      defaultTypes = newArr;
      Arrays.sort(defaultTypes);
      cbMod = new DefaultComboBoxModel(defaultTypes);
    }
  }

  public ComboBoxModel getTypeCBModel()
  {
    return cbMod;
  }

  public void setStateVarsList(Collection svs)
  {
    stateVars = svs;
  }
  public void setSimParmsList(Collection parms)
  {
    simParms = parms;
  }
  private Collection stateVars = new Vector();
  private Collection simParms = new Vector();

  public ComboBoxModel getStateVarsCBModel()
  {
    return new DefaultComboBoxModel(new Vector(stateVars));
  }

  /******/
  /* Beanshell code */
  /******/
  private void initBeanShell()
  {
    interpreter = new Interpreter();
    interpreter.setStrictJava(true);       // no loose typeing
    NameSpace ns = interpreter.getNameSpace();
    ns.importPackage("simkit.*");
    ns.importPackage("simkit.examples.*");
    ns.importPackage("simkit.random.*");
    ns.importPackage("simkit.smdx.*");
    ns.importPackage("simkit.stat.*");
    ns.importPackage("simkit.util.*");
  }
  String bshErr = "BeanShell eval error";
  private Vector nsSets = new Vector();

  public String parseCode(EventNode node, String s)
  {
    // Load the interpreter with the state variables and the sim parameters
    // Load up the local variables and event parameters for this particular node
    // Then do the parse.

    // Lose the new lines
    s = s.replace('\n',' ');

    // state variables
    for (Iterator itr = stateVars.iterator(); itr.hasNext();) {
      vStateVariable sv = (vStateVariable) itr.next();
      String result = handleNameType(sv.getName(),sv.getType());
      if(result != null) {
        clearNamespace();
        return bshErr +"\n" + result;
      }
      nsSets.add(sv.getName());
    }
    // Sim parameters
    for (Iterator itr = simParms.iterator(); itr.hasNext();) {
      vParameter par = (vParameter) itr.next();
      String result = handleNameType(par.getName(),par.getType());
      if(result != null) {
        clearNamespace();
        return bshErr +"\n" + result;
      }
      nsSets.add(par.getName());
    }
    // Event local variables
    if(node != null) {
      for(Iterator itr = node.getLocalVariables().iterator(); itr.hasNext(); ) {
        EventLocalVariable elv = (EventLocalVariable)itr.next();
        String result = handleNameType(elv.getName(),elv.getType());
        if(result != null) {
          clearNamespace();
          return bshErr +"\n" + result;
        }
        nsSets.add(elv.getName());
      }
      // Event arguments
      for(Iterator itr = node.getArguments().iterator(); itr.hasNext(); ) {
        EventArgument ea = (EventArgument)itr.next();
        String result = handleNameType(ea.getName(),ea.getType());
        if(result != null) {
          clearNamespace();
          return bshErr +"\n" + result;
        }
      }
    }

    // see if we can parse it.  We've initted all arrays to size = 1, so ignore
    // outofbounds exceptions
    try {
      String noCRs = s.replace('\n',' ');
      Object o = interpreter.eval(noCRs);
    }
    catch (EvalError evalError) {
      if(evalError.getMessage().indexOf("java.lang.ArrayIndexOutOfBoundsException") == -1) {
        clearNamespace();
        return bshErr + "\n" + evalError.getMessage();
      } // else fall through the catch
    }

    clearNamespace();
    return null;    // null means good parse!
  }

  private void clearNamespace()
  {
    for(Iterator itr = nsSets.iterator(); itr.hasNext(); ) {
      try {
        interpreter.unset((String)itr.next());
      }
      catch (EvalError evalError)
      {
        //System.out.println(evalError.getMessage());
      }
    }
    nsSets.clear();
  }
  private String handleNameType(String name, String typ)
  {
    if (!handlePrimitive(name, typ)) {
        try {
          interpreter.set(name,instantiateType(typ));      // the 2nd param will be null if nogo and cause exc
        }
        catch (Exception ex) {
          clearNamespace();
          return bshErr + "\n" + ex.getMessage();
        }
      //}
    }
    return null; // this is good
  }


  private Object instantiateType(String typ)
  {
    Object o = null;
    boolean isArr = false;
    if(typ.indexOf('[') != -1) {
      typ = typ.substring(0,typ.length()-2);
      isArr = true;
    }
    try {
      Class c = Class.forName(typ);
      if(isArr)
        o = Array.newInstance(c,1);
      else
        o = c.newInstance();
    }
    catch (Exception e) {
      o = null;
    }
    if(o != null)
      return o;

    // OK. See if we've got a dummy one in our HashMap
    return VsimkitObjects.hashmap.get(typ);
  }

  private boolean handlePrimitive(String name, String typ)
  {
    try {
      if(typ.equals("int")) {
        interpreter.set(name,(int)0);
        return true;
      }
      if(typ.equals("int[]")) {
        interpreter.set(name,new int[0]);
        return true;
      }
      if(typ.equals("boolean")) {
        interpreter.set(name,true);
        return true;
      }
      if(typ.equals("boolean[]")) {
        interpreter.set(name,new boolean[0]);
        return true;
      }
      if(typ.equals("double")) {
        interpreter.set(name,0.0d);
        return true;
      }
      if(typ.equals("double[]")) {
        interpreter.set(name,new double[0]);
        return true;
      }
      if(typ.equals("float")) {
        interpreter.set(name,0.0f);
        return true;
      }
      if(typ.equals("float[]")) {
        interpreter.set(name,new float[0]);
        return true;
      }
      if(typ.equals("byte")) {
        interpreter.set(name,(byte)0);
        return true;
       }
      if(typ.equals("byte[]")) {
        interpreter.set(name,new byte[0]);
        return true;
       }
      if(typ.equals("char")) {
        interpreter.set(name,(char)0);
        return true;
      }
      if(typ.equals("char[]")) {
        interpreter.set(name,new char[0]);
        return true;
      }
      if(typ.equals("short")) {
        interpreter.set(name,(short)0);
        return true;
      }
      if(typ.equals("short[]")) {
        interpreter.set(name,new short[0]);
        return true;
      }
      if(typ.equals("long")) {
        interpreter.set(name,(long)0);
        return true;
      }
      if(typ.equals("long[]")) {
        interpreter.set(name,new long[0]);
        return true;
      }
    }
    catch (EvalError evalError) {
      //assert false:"BeanShell eval error ";
      System.err.println("BeanShell evel error");
      evalError.printStackTrace();
      return false;
    }
    return false;
  }
}
