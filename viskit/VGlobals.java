package viskit;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import edu.nps.util.FileIO;
import edu.nps.util.SimpleDirectoryClassLoader;
import org.apache.commons.configuration.XMLConfiguration;
import viskit.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public AssemblyViewFrame buildAssemblyViewFrame(boolean contentOnly)
  {
    AssemblyController cont = new AssemblyController();
    return buildAssemblyViewFrame(contentOnly, cont, new AssemblyModel(cont));
  }

  public AssemblyViewFrame buildAssemblyViewFrame(boolean contentOnly, AssemblyController cont, AssemblyModel mod)
  {
    initAssemblyViewFrame(contentOnly,cont,mod);
    cont.begin();

    return avf;
  }

  public AssemblyViewFrame initAssemblyViewFrame(boolean contentOnly)
  {
    AssemblyController cont = new AssemblyController();
    return initAssemblyViewFrame(contentOnly,cont, new AssemblyModel(cont));
  }

  public AssemblyViewFrame initAssemblyViewFrame(boolean contentOnly, AssemblyController cont, AssemblyModel mod)
  {
    avf = new AssemblyViewFrame(contentOnly, mod,cont);
    acont = cont;
    amod = mod;
    cont.setModel(mod);   // registers cntl as model listener
    cont.setView(avf);

    mod.init();
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
      buildAssemblyViewFrame(false);

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

  ActionListener defaultAssyQuitHandler = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      if(avf != null) {
        avf.setVisible(false);
      }
      if(egvf != null && egvf.isVisible())
        return;
      System.exit(0);
    }
  };
  ActionListener assyQuitHandler=defaultAssyQuitHandler;

  public void quitAssemblyEditor()
  {
    if(assyQuitHandler!= null)
      assyQuitHandler.actionPerformed(new ActionEvent(this,0,"quit assy editor"));
  }

  public void setAssemblyQuitHandler(ActionListener  lis)
  {
    assyQuitHandler = lis;
  }
  EventGraphViewFrame egvf;
  public EventGraphViewFrame getEventGraphEditor()
  {
    return egvf;
  }

  public EventGraphViewFrame buildEventGraphViewFrame()
  {
    return buildEventGraphViewFrame(false,new Controller());
  }

  public EventGraphViewFrame buildEventGraphViewFrame(boolean contentOnly, Controller cont)
  {
    initEventGraphViewFrame(contentOnly,cont);
    cont.begin();
    return egvf;
  }
  public EventGraphViewFrame initEventGraphViewFrame(boolean contentOnly)
  {
    return initEventGraphViewFrame(contentOnly,new Controller());
  }

  public EventGraphViewFrame initEventGraphViewFrame(boolean contentOnly, Controller cont)
  {
    egvf = new EventGraphViewFrame(contentOnly,cont);
    cont.setView(egvf);
    return egvf;
  }
  
  public ViskitModel getActiveEventGraphModel()
  {
    return  (ViskitModel)egvf.getModel();
  }

  ActionListener defaultEventGraphQuitHandler = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      if(egvf != null) {
        egvf.setVisible(false);
      }
      if(avf != null && avf.isVisible())
        return;
      System.exit(0);
    }
  };

  ActionListener eventGraphQuitHandler = defaultEventGraphQuitHandler;

  public void quitEventGraphEditor()
  {
    if(eventGraphQuitHandler != null)
      eventGraphQuitHandler.actionPerformed(new ActionEvent(this,0,"quit event graph editor"));
  }
  public void setEventGraphQuitHandler(ActionListener lis)
  {
    eventGraphQuitHandler = lis;
  }

  public void runEventGraphView()
  {
    if (egvf == null)
      buildEventGraphViewFrame();

    egvf.setVisible(true);
    egvf.toFront();
  }

  public void installEventGraphView()
  {
    if (egvf == null)
      buildEventGraphViewFrame();
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
    ns.importPackage("diskit.*");         // 17 Nov 2004
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
      String result;
      if(sv.getType().indexOf('[') != -1)
        result = handleNameType(sv.getName(),sv.getArrayType(),false);
      else
        result = handleNameType(sv.getName(),sv.getType(),false);
      if(result != null) {
        clearNamespace();
        return bshErr +"\n" + result;
      }
      nsSets.add(sv.getName());
    }
    // Sim parameters
    for (Iterator itr = simParms.iterator(); itr.hasNext();) {
      vParameter par = (vParameter) itr.next();
      String result;
      if(par.getType().indexOf('[') != -1)
        result = handleNameType(par.getName(),par.getArrayType(),false);
      else
        result = handleNameType(par.getName(),par.getType(),false);
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
        String result;
        if(elv.getType().indexOf('[') != -1)
          result = handleNameType(elv.getName(),elv.getArrayType(),false);
        else
          result = handleNameType(elv.getName(),elv.getType(),false);
        if(result != null) {
          clearNamespace();
          return bshErr +"\n" + result;
        }
        nsSets.add(elv.getName());
      }
      // Event arguments
      for(Iterator itr = node.getArguments().iterator(); itr.hasNext(); ) {
        EventArgument ea = (EventArgument)itr.next();
        String result = handleNameType(ea.getName(),ea.getType(),false);
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
  private String handleNameType(String name, String typ, boolean doInstantiate)      // I don't think the instant part
  {                                                                                  // is or should be used here
    if (!handlePrimitive(name, typ)) {
      if (!doInstantiate)
        return (findType(name,typ));

      try {
        Object o = instantiateType(typ);
        if (o == null)
          throw new Exception("Class not found: " + typ);
        //interpreter.set(name,o);      // the 2nd param will be null if nogo and cause exc
        interpreter.eval(typ + " " + name + " = " + o);
      }
      catch (Exception ex) {
        clearNamespace();
        return ex.getMessage();
      }
    }
    return null; // this is good
  }

  private String findType(String name, String typ) {
    try {
      Class c = Vstatics.classForName(typ);
      interpreter.eval(typ + " " + name + ";");
    }
    catch (Exception e) {
      clearNamespace();
      return e.getMessage();
    }
    return null;
  }
  private Object instantiateType(String typ) throws Exception
  {
    Object o = null;
    boolean isArr = false;
    if(typ.indexOf('[') != -1) {
      typ = typ.substring(0,typ.length()-2);
      isArr = true;
    }
    try {
      Class c = Vstatics.classForName(typ);
      if(c == null)
        throw new Exception("Class not found: "+typ);
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
    try {
      o = VsimkitObjects.getInstance(typ);
    }
    catch (Exception e) {
      throw new Exception(e);
    }
    return o;
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
  private final int primitivesIndex = 0; // for above array
  private final int packagesStart = 1;
  private final int packagesEnd = 3;

  private String[][] moreClasses =
  {
    {"boolean","char","byte","short","int","long","float","double"},
    {"Boolean","Character","Byte","Short","Integer","Long","Float","Double","String","StringBuffer"},
    {"HashMap","HashSet","LinkedList","Properties","Random","TreeMap","TreeSet","Vector"},
    {"RandomNumber","RandomVariate"},
    {}
  };
  public boolean isPrimitive(String ty)
  {
    for(int i=0;i<moreClasses[primitivesIndex].length;i++) {
      if(ty.equals(moreClasses[primitivesIndex][i]))
        return true;
    }
    return false;
  }
  public boolean isPrimitiveOrPrimitiveArray(String ty)
  {
    int idx;
    if((idx = ty.indexOf('[')) != -1)
      ty = ty.substring(0,idx);
    return isPrimitive(ty);
  }

  Pattern bracketsPattern = Pattern.compile("\\[.*?\\]");
  Pattern spacesPattern = Pattern.compile("\\s");

  public String stripArraySize(String typ)
  {
    Matcher m = bracketsPattern.matcher(typ);
    String r = m.replaceAll("[]");            // [blah] with[]
    m = spacesPattern.matcher(r);
    return m.replaceAll("");
  }

  public String[] getArraySize(String typ)
  {
    Vector v = new Vector();
    Matcher m = bracketsPattern.matcher(typ);

    while(m.find())
    {
      String g = m.group();
      v.add(g.substring(1,g.length()-1).trim());
    }
    if(v.size() <= 0)
      return null;
    return (String[])v.toArray(new String[0]);
  }

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
         if(i == primitivesIndex)
           mi = new MyJMenuItem(moreClasses[i][j],moreClasses[i][j]); // no package
         else
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

  private XMLConfiguration hConfig;
  private String userConfigPath = System.getProperty("user.home")+
                                  System.getProperty("file.separator") +
                                  ".viskit_history.xml";

  private ClassLoader workLoader;
  public ClassLoader getWorkClassLoader()
  {
    if(workLoader == null)
      workLoader = new SimpleDirectoryClassLoader(workDirectory);
    return workLoader;
  }

  public XMLConfiguration getHistoryConfig()
  {
    if (hConfig == null) {
      try {
        File hf = new File(userConfigPath);
        if (!hf.exists()) {
          File src = new File("c_history_template.xml");
          hf.createNewFile();
          FileIO.copyFile(src, hf, true);
        }
        hConfig = ViskitConfig.instance().getIndividualXMLConfig(hf.getAbsolutePath());
      }
      catch (Exception e) {
        System.out.println("Error loading history file: " + e.getMessage());
        System.out.println("Recent file saving disabled");
        hConfig = null;
      }
    }
    return hConfig;
  }
}
