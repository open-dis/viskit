package viskit;

import bsh.Interpreter;
import bsh.EvalError;

import javax.swing.*;
import java.util.*;

import viskit.model.vParameter;
import viskit.model.EventNode;
import viskit.model.vStateVariable;

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
  }

  public String[] getTypes()
  {
    return defaultTypes;
  }

  private String[] defaultTypes = {
    "double",
    "float",
    "int",
    "java.lang.String",
    "short",
  };

  public void addType(String ty)
  {
    if (Arrays.binarySearch(defaultTypes, ty) < 0) {
      String[] newArr = new String[defaultTypes.length + 1];
      System.arraycopy(defaultTypes, 0, newArr, 0, defaultTypes.length);
      newArr[newArr.length - 1] = ty;
      defaultTypes = newArr;
      Arrays.sort(defaultTypes);
      cbMod = new DefaultComboBoxModel(defaultTypes);
    }
  }

  ComboBoxModel cbMod = new DefaultComboBoxModel(defaultTypes);

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
  }
  String bshErr = "BeanShell eval error";
  private Vector nsSets = new Vector();

  public String parseCode(EventNode node, String s)
  {
    // Load the interpreter with the state variables and the sim parameters
    // Load up the local variables and event parameters for this particular node
    // Then do the parse.

    for (Iterator itr = stateVars.iterator(); itr.hasNext();) {
      vStateVariable sv = (vStateVariable) itr.next();
      if (!handlePrimitive(sv.getName(), sv.getType())) {
        if (!handleJavaDotLang(sv.getName(), sv.getType())) {
          try {
            interpreter.set(sv.getName(), Class.forName(sv.getType()).newInstance());
          }
          catch (Exception ex) {
            clearNamespace();
            return bshErr + "\n" + ex.getMessage();
          }
        }
      }
      nsSets.add(sv.getName());
    }
    for (Iterator itr = simParms.iterator(); itr.hasNext();) {
      vParameter par = (vParameter) itr.next();
      if (!handlePrimitive(par.getName(), par.getType())) {
        if (!handleJavaDotLang(par.getName(), par.getType())) {
          try {
            interpreter.set(par.getName(), Class.forName(par.getType()).newInstance());
          }
          catch (Exception ex) {
            clearNamespace();
            return bshErr + "\n" + ex.getMessage();
          }
        }
      }
      nsSets.add(par.getName());
    }
    try {
      String noCRs = s.replace('\n',' ');
      Object o = interpreter.eval(noCRs);
    }
    catch (EvalError evalError) {
      clearNamespace();
      return bshErr + "\n" + evalError.getMessage();
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

  private boolean handlePrimitive(String name, String typ)
  {
    try {
      if(typ.equals("int")) {
        interpreter.set(name,(int)0);
        return true;
      }
      if(typ.equals("boolean")) {
        interpreter.set(name,true);
        return true;
      }
      if(typ.equals("double")) {
        interpreter.set(name,0.0d);
        return true;
      }
      if(typ.equals("float")) {
        interpreter.set(name,0.0f);
        return true;
      }
      if(typ.equals("byte")) {
        interpreter.set(name,(byte)0);
        return true;
       }
      if(typ.equals("char")) {
        interpreter.set(name,(char)0);
        return true;
      }
      if(typ.equals("short")) {
        interpreter.set(name,(short)0);
        return true;
      }
      if(typ.equals("long")) {
        interpreter.set(name,(long)0);
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
  private boolean handleJavaDotLang(String nm, String typ)
  {
    if(nm.indexOf('.') != -1)
      return false;

    try {
      interpreter.set(nm,Class.forName("java.lang."+typ).newInstance());
    }
    catch (Exception e) {
      return false;
    }
    return true;
  }
}
