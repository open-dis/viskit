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
import java.util.regex.Pattern;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Apr 5, 2004
 * Time: 3:20:33 PM
 */

public class VGlobals
{
  private static VGlobals me;
  private Interpreter interpreter;
  private DefaultComboBoxModel cbMod;
  private JPopupMenu popup;
  private myTypeListener myListener;

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

    cbMod = new DefaultComboBoxModel(new Vector(Arrays.asList(defaultTypeStrings)));
    myListener = new myTypeListener();
    buildTypePopup();
    setupWorkDirectory();
  }

  /* routines to manage the singleton-aspect of the views. */

  AssemblyViewFrame avf;
  AssemblyController acont;
  AssemblyModel amod;
  boolean assyFirstRun = false;
  /**
   * Get a reference to the assembly editor view.
   * @return a reference to the assembly editor view or null if yet unbuilt.
   */
  public AssemblyViewFrame getAssemblyEditor()
  {
    return avf;
  }
  public AssemblyViewFrame buildAssemblyViewFrame()
  {
    return buildAssemblyViewFrame(new AssemblyController(), new AssemblyModel());
  }
  public AssemblyViewFrame buildAssemblyViewFrame(AssemblyController cont, AssemblyModel mod)
  {
    avf = new AssemblyViewFrame(mod,cont);
    acont = cont;
    amod = mod;
    cont.setModel(mod);   // registers cntl as model listener
    cont.setView(avf);

    mod.init();
    cont.begin();

    return avf;
  }
  public AssemblyModel getAssemblyModel()
  {
    return amod;
  }
  public AssemblyController getAssemblyController()
  {
    return acont;
  }

  public void runAssemblyView()
  {
    if (avf == null)
      buildAssemblyViewFrame();

    avf.setVisible(true);
    avf.toFront();

    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        if (assyFirstRun)
          return;

        assyFirstRun = true;
        acont.newAssembly();
      }
    });

  }

  public void quitAssemblyEditor()
  {
    if(avf != null) {
      avf.setVisible(false);
    }

    if(egvf != null && egvf.isVisible())
      return;
    System.exit(0);
  }
  EventGraphViewFrame egvf;
  public EventGraphViewFrame getEventGraphEditor()
  {
    return egvf;
  }
  public EventGraphViewFrame buildEventGraphViewFrame()
  {
    return buildEventGraphViewFrame(new Controller(), new Model());
  }
  public EventGraphViewFrame buildEventGraphViewFrame(Controller cont, Model mod)
  {
    egvf = new EventGraphViewFrame(mod,cont);
    cont.setModel(mod);   // registers cntl as model listener
    cont.setView(egvf);

    mod.init();
    cont.begin();

    return egvf;
  }
  public void quitEventGraphEditor()
  {
    if(egvf != null) {
      egvf.setVisible(false);
    }
    if(avf != null && avf.isVisible())
      return;
    System.exit(0);
  }
  public void runEventGraphView()
  {
    if (egvf == null)
      buildEventGraphViewFrame();

    egvf.setVisible(true);
    egvf.toFront();
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
          Object o = instantiateType(typ);
          //interpreter.set(name,o);      // the 2nd param will be null if nogo and cause exc
          interpreter.eval(typ+" "+ name +" = "+o);
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
      Class c = Vstatics.classForName(typ);
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
    return VsimkitObjects.getInstance(typ);
  }

  private boolean handlePrimitive(String name, String typ)
  {
    try {
      if(typ.equals("int")) {
        interpreter.eval("int " + name + " = 0");
        //interpreter.set(name,(int)0);
        return true;
      }
      if(typ.equals("int[]")) {
        interpreter.eval("int[] " + name + " = new int[0]");
        //interpreter.set(name,new int[0]);
        return true;
      }
      if(typ.equals("boolean")) {
        interpreter.eval("boolean " + name + " = false");  // 17Aug04, should have always defaulted to false
        //interpreter.set(name,true);
        return true;
      }
      if(typ.equals("boolean[]")) {
        interpreter.eval("boolean[] " + name + " = new boolean[0]");
        //interpreter.set(name,new boolean[0]);
        return true;
      }
      if(typ.equals("double")) {
        interpreter.eval("double " + name + " = 0.0d");
        //interpreter.set(name,0.0d);
        return true;
      }
      if(typ.equals("double[]")) {
        interpreter.eval("double[] " + name + " = new double[0]");
        //interpreter.set(name,new double[0]);
        return true;
      }
      if(typ.equals("float")) {
        interpreter.eval("float " + name + " = 0.0f");
        //interpreter.set(name,0.0f);
        return true;
      }
      if(typ.equals("float[]")) {
        interpreter.eval("float[] " + name + " = new float[0]");
        //interpreter.set(name,new float[0]);
        return true;
      }
      if(typ.equals("byte")) {
        interpreter.eval("byte " + name + " = 0");
        //interpreter.set(name,(byte)0);
        return true;
       }
      if(typ.equals("byte[]")) {
        interpreter.eval("byte[] " + name + " = new byte[0]");
        //interpreter.set(name,new byte[0]);
        return true;
       }
      if(typ.equals("char")) {
        interpreter.eval("char " + name + " = 0");
        //interpreter.set(name,(char)0);
        return true;
      }
      if(typ.equals("char[]")) {
        interpreter.eval("char[] " + name + " = new char[0]");
        //interpreter.set(name,new char[0]);
        return true;
      }
      if(typ.equals("short")) {
        interpreter.eval("short " + name + " = 0");
        //interpreter.set(name,(short)0);
        return true;
      }
      if(typ.equals("short[]")) {
        interpreter.eval("short[] " + name + " = new short[0]");
        //interpreter.set(name,new short[0]);
        return true;
      }
      if(typ.equals("long")) {
        interpreter.eval("long " + name + " = 0");
        //interpreter.set(name,(long)0);
        return true;
      }
      if(typ.equals("long[]")) {
        interpreter.eval("long[] "+ name + " = new long[0]");
        //interpreter.set(name,new long[0]);
        return true;
      }
    }
    catch (EvalError evalError) {
      //assert false:"BeanShell eval error ";
      System.err.println(bshErr);
      evalError.printStackTrace();
      return false;
    }
    return false;
  }

  /*
    Dynamic variable type list processing.  Build Type combo boxes and manage user-typed object types.
  */
  private String   moreTypesString = "more...";
  private String[] defaultTypeStrings = {
    "int",
    "double",
    "Integer",
    "Double",
    "String",
    moreTypesString
  };

  private String[] morePackages = {"primitives","java.lang","java.util","simkit.random","cancel"};
  private String[][] moreClasses =
  {
    {"boolean","char","byte","short","int","long","float","double"},
    {"Boolean","Character","Byte","Short","Integer","Long","Float","Double","String","StringBuffer"},
    {"HashMap","HashSet","LinkedList","Properties","Random","TreeMap","TreeSet","Vector"},
    {"RandomNumber","RandomVariate"},
    {}
  };

  /**
   * This is messaged by dialogs and others when a user has selected a type for a new variable.  We look
   * around to see if we've already got it covered.  If not, we add it to the end of the list.
   * @param ty
   */
  public String typeChosen(String ty)
  {
    ty = ty.replaceAll("\\s","");              // every whitespace removed
    for(int i=0;i<cbMod.getSize();i++) {
      if(cbMod.getElementAt(i).toString().equals(ty))
        return ty;
    }
    // else, put it at the end, but before the "more"
    cbMod.insertElementAt(ty,cbMod.getSize()-1);
    return ty;
  }

  public JComboBox getTypeCB()
  {
    JComboBox cb = new JComboBox(cbMod);
    cb.addActionListener(myListener);
    cb.addItemListener(myListener);
    cb.setRenderer(new myTypeListRenderer());
    cb.setEditable(true);
    return cb;
  }

  private void buildTypePopup()
  {

    popup = new JPopupMenu();
    JMenu m;
    JMenuItem mi;
/*
    for(Iterator itr = VsimkitObjects.hashmap.keySet().iterator(); itr.hasNext();) {
      String s = (String)itr.next();
      mi = new JMenuItem(s);
      mi.addActionListener(myListener);
      popup.add(mi);
    }
*/
   for(int i=0;i<morePackages.length;i++) {
     if(moreClasses[i].length <= 0) {           // if no classes, make the "package selectable
       mi = new MyJMenuItem(morePackages[i],null);
       mi.addActionListener(myListener);
       popup.add(mi);
     }
     else {
       m = new JMenu(morePackages[i]);
       for(int j=0;j<moreClasses[i].length;j++) {
         mi = new MyJMenuItem(moreClasses[i][j],morePackages[i]+"."+moreClasses[i][j]);
         mi.addActionListener(myListener);
         m.add(mi);
       }
       popup.add(m);
     }
   }
  }
  JComboBox pending;
  Object lastSelected = "void";

  class myTypeListener implements ActionListener, ItemListener
  {
    public void itemStateChanged(ItemEvent e)
    {
      if(e.getStateChange() == ItemEvent.DESELECTED)
        lastSelected = e.getItem();
    }

    public void actionPerformed(ActionEvent e)
    {
      Object o = e.getSource();
      if(o instanceof JComboBox) {
        JComboBox cb = (JComboBox)o;
        pending = cb;
        if(cb.getSelectedItem().toString().equals(moreTypesString))
           popup.show(cb,0,0);
      }
      else {
        MyJMenuItem mi = (MyJMenuItem)o;
        if(!mi.getText().equals("cancel"))
          pending.setSelectedItem(mi.getFullName()); //mi.getText());
        else
          pending.setSelectedItem(lastSelected);
      }
    }
  }
  class myTypeListRenderer extends JLabel implements ListCellRenderer
  {
    //Font specialFont = getFont().deriveFont(Font.ITALIC);
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
      JLabel lab = new JLabel(value.toString());
      if(value.toString().equals(moreTypesString))
        lab.setBorder(BorderFactory.createRaisedBevelBorder()); //createEtchedBorder());
        //lab.setFont(specialFont);
      return lab;
    }
  }

  Vector existingNames = new Vector();
  Vector existingAssemblyNames = new Vector();
  /**
   * Returns true if the data is valid, eg we have a valid parameter name
   * and a valid type.
   */

  private boolean checkLegalJavaName(String nm)
  {

    String javaVariableNameRegExp;

    // Do a REGEXP to confirm that the variable name fits the criteria for
    // a Java variable. We don't want to allow something like "2f", which
    // Java will misinterpret as a number literal rather than a variable. This regexp
    // is slightly more restrictive in that it demands that the variable name
    // start with a lower case letter (which is not demanded by Java but is
    // a strong convention) and disallows the underscore. "^" means it
    // has to start with a lower case letter in the leftmost position.

    javaVariableNameRegExp = "^[a-z][a-zA-Z0-9]*$";
    if(!Pattern.matches(javaVariableNameRegExp, nm))
    {
      JOptionPane.showMessageDialog(null,
                                    "variable names must start with a lower case letter and conform to the Java variable naming conventions",
                                    "alert",
                                    JOptionPane.ERROR_MESSAGE);
      return false;
    }

    // Check to make sure the name the user specified isn't already used by a state variable
    // or parameter.

    for(int idx = 0; idx < existingNames.size(); idx++)
    {
      if(nm.equals(existingNames.get(idx)))
      {
        JOptionPane.showMessageDialog(null,
                                    "variable names must be unique and not match any existing parameter or state variable name",
                                    "alert",
                                    JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }

/*
    // Check to make sure the class or type exists
    if(!ClassUtility.classExists(typeLabel.getSelectedItem().toString()))
    {
      JOptionPane.showMessageDialog(null,
                                    "The class name " + typeLabel.getSelectedItem().toString() + "  does not exist on the classpath",
                                    "alert",
                                    JOptionPane.ERROR_MESSAGE);
      return false;
    }
*/
    existingNames.add(nm);
    return true;
  }

  public void reset()
  {
    existingNames.clear();
    stateVars.clear();
    simParms.clear();
  }
  public void assemblyReset()
  {
    existingAssemblyNames.clear();
  }
  /**
   * Small class to hold on to the fully-qualified class name, while displaying only the
   * un-qualified name;
   */
  class MyJMenuItem extends JMenuItem
  {
    private String fullName;
    MyJMenuItem(String nm, String fullName)
    {
      super(nm);
      this.fullName = fullName;
    }
    public String getFullName()
    {
      return fullName;
    }
  }

  public File getWorkDirectory()
  {
    return workDirectory;
  }

  private File workDirectory;

  private void setupWorkDirectory()
  {
    try {
      workDirectory = File.createTempFile("viskit","work");   // actually creates
      String p = workDirectory.getAbsolutePath();   // just want the name part of it
      workDirectory.delete();        // Don't want the file to be made yet
      workDirectory = new File(p);
      workDirectory.mkdir();
      workDirectory.deleteOnExit();
      
      File nf = new File(workDirectory,"simkit");     // most go here
      nf.mkdir();
      nf.deleteOnExit();
      nf = new File(nf,"examples");
      nf.mkdir();
      nf.deleteOnExit();
      return;
    }
    catch (IOException e) {}

    if(workDirectory.mkdir()==false)
        JOptionPane.showMessageDialog(null,"The directory "+ workDirectory.getPath()+
                    " could not be created.  Correct permissions before proceeding.",
                                      "Error",JOptionPane.ERROR_MESSAGE);
  }
}
