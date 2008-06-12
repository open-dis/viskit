package viskit;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import edu.nps.util.FileIO;
import edu.nps.util.SysExitHandler;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import static edu.nps.util.GenericConversion.toArray;
import viskit.doe.LocalBootLoader;
import viskit.model.AssemblyModel;
import viskit.model.EventNode;
import viskit.model.ViskitElement;
import viskit.model.ViskitModel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 5, 2004
 * @since 3:20:33 PM
 * @version $Id: VGlobals.java 1667 2007-12-17 20:24:55Z tdnorbra $
 */
public class VGlobals {

    public static Logger log = Logger.getLogger(VGlobals.class);
    private static VGlobals me;
    private Interpreter interpreter;
    private DefaultComboBoxModel cbMod;
    private JPopupMenu popup;
    private myTypeListener myListener;
    private JFrame mainAppWindow;
    
    /** Need hold of the Enable Analyst Reports checkbox */
    private RunnerPanel2 runPanel;
    
    /** Flag to denote called sysExit only once */
    private boolean sysExitCalled = false;

    public static synchronized VGlobals instance() {
        if (me == null) {
            me = new VGlobals();
        }
        return me;
    }

    private VGlobals() {
        //initBeanShell();

        cbMod = new DefaultComboBoxModel(new Vector<String>(Arrays.asList(defaultTypeStrings)));
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
    public AssemblyViewFrame getAssemblyEditor() {
        return avf;
    }

    public AssemblyViewFrame buildAssemblyViewFrame(boolean contentOnly) {
        AssemblyController cont = new AssemblyController();
        return buildAssemblyViewFrame(contentOnly, cont, new AssemblyModel(cont));
    }

    public AssemblyViewFrame buildAssemblyViewFrame(boolean contentOnly, AssemblyController cont, AssemblyModel mod) {
        initAssemblyViewFrame(contentOnly, cont, mod);
        cont.begin();

        return avf;
    }

    public AssemblyViewFrame initAssemblyViewFrame(boolean contentOnly) {
        AssemblyController cont = new AssemblyController();
        return initAssemblyViewFrame(contentOnly, cont, new AssemblyModel(cont));
    }

    public AssemblyViewFrame initAssemblyViewFrame(boolean contentOnly, AssemblyController cont, AssemblyModel mod) {
        avf = new AssemblyViewFrame(contentOnly, mod, cont);
        acont = cont;
        amod = mod;
        cont.setModel(mod);   // registers cntl as model listener
        cont.setView(avf);

        mod.init();
        return avf;
    }

    public void rebuildTreePanels() {
        avf.rebuildTreePanels();
    }

    public AssemblyModel getAssemblyModel() {
        return amod;
    }

    public AssemblyController getAssemblyController() {
        return acont;
    }

    public void runAssemblyView() {
        if (avf == null) {
            buildAssemblyViewFrame(false);
        }

        avf.setVisible(true);
        avf.toFront();

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (assyFirstRun) {
                    return;
                }

                assyFirstRun = true;
                acont.newAssembly();
            }
        });

    }
    ActionListener defaultAssyQuitHandler = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            if (avf != null) {
                avf.setVisible(false);
            }
            if (egvf != null && egvf.isVisible()) {
                return;
            }
            sysExit(0);
        }
     };
    ActionListener assyQuitHandler = defaultAssyQuitHandler;

    public void quitAssemblyEditor() {
        if (assyQuitHandler != null) {
            assyQuitHandler.actionPerformed(new ActionEvent(this, 0, "quit assy editor"));
        }
    }

    public void setAssemblyQuitHandler(ActionListener lis) {
        assyQuitHandler = lis;
    }
    EventGraphViewFrame egvf;

    public EventGraphViewFrame getEventGraphEditor() {
        return egvf;
    }

    public EventGraphViewFrame buildEventGraphViewFrame() {
        return buildEventGraphViewFrame(false, new EventGraphController());
    }

    public EventGraphViewFrame buildEventGraphViewFrame(boolean contentOnly, EventGraphController cont) {
        initEventGraphViewFrame(contentOnly, cont);
        cont.begin();
        return egvf;
    }

    public EventGraphViewFrame initEventGraphViewFrame(boolean contentOnly) {
        return initEventGraphViewFrame(contentOnly, new EventGraphController());
    }

    public EventGraphViewFrame initEventGraphViewFrame(boolean contentOnly, EventGraphController cont) {
        egvf = new EventGraphViewFrame(contentOnly, cont);
        cont.setView(egvf);
        return egvf;
    }

    public ViskitModel getActiveEventGraphModel() {
        return (ViskitModel) egvf.getModel();
    }
    
    ActionListener defaultEventGraphQuitHandler = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            if (egvf != null) {
                egvf.setVisible(false);
            }
            if (avf != null && avf.isVisible()) {
                return;
            }
            sysExit(0);
        }
    };
    ActionListener eventGraphQuitHandler = defaultEventGraphQuitHandler;

    public void quitEventGraphEditor() {
        if (eventGraphQuitHandler != null) {
            eventGraphQuitHandler.actionPerformed(new ActionEvent(this, 0, "quit event graph editor"));
        }
    }

    public void setEventGraphQuitHandler(ActionListener lis) {
        eventGraphQuitHandler = lis;
    }

    public void runEventGraphView() {
        if (egvf == null) {
            buildEventGraphViewFrame();
        }

        egvf.setVisible(true);
        egvf.toFront();
    }

    public void installEventGraphView() {
        if (egvf == null) {
            buildEventGraphViewFrame();
        }
    }

    private Vector<? extends ViskitElement> getStateVarsList() {
        return getActiveEventGraphModel().getStateVariables();
    }

    private Vector<? extends ViskitElement> getSimParmsList() {
        return getActiveEventGraphModel().getSimParameters();
    }

    public ComboBoxModel getStateVarsCBModel() {
        return new DefaultComboBoxModel(getStateVarsList());
    }

    /******/
    /* Beanshell code */
    /******/
    private void initBeanShell() {
        interpreter = new Interpreter();
        interpreter.setStrictJava(true);       // no loose typeing

        String[] extraCP = Vstatics.getExtraClassPathArray();
        if (extraCP != null && extraCP.length > 0) {
            for (String path : extraCP) {
                try {
                    interpreter.getClassManager().addClassPath(new URL("file", "localhost", path));
                } catch (IOException e) {
                    log.error("bad extra classpath: " + path);
                }
            }
        }

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
    private Vector<String> nsSets = new Vector<String>();

    public String parseCode(EventNode node, String s) {
        initBeanShell();
        // Load the interpreter with the state variables and the sim parameters
        // Load up any local variables and event parameters for this particular node
        // Then, parse.

        // Lose the new lines
        String noCRs = s.replace('\n', ' ');
        
        String name = null;
        String type = null;

        if (node != null) {            
            
            // Event local variables
            for (ViskitElement eventLocalVariable : node.getLocalVariables()) {
                String result;
                type = eventLocalVariable.getType();
                name = eventLocalVariable.getName();
                if (isArray(type)) {
                    result = handleNameType(name, eventLocalVariable.getArrayType());
                } else {
                    result = handleNameType(name, type);
                }
                if (result != null) {
                    clearNamespace();
                    return bshErr + "\n" + result;
                }
                nsSets.add(name);
            }
            
            // Event arguments
            for (ViskitElement ea : node.getArguments()) {
                type = ea.getType();
                name = ea.getName();
                String result = handleNameType(name, type);
                if (result != null) {
                    clearNamespace();
                    return bshErr + "\n" + result;
                }
                nsSets.add(name);
            }
        }
        
        // state variables
        for (ViskitElement stateVariable : getStateVarsList()) {
            String result;
            type = stateVariable.getType();
            name = stateVariable.getName();
            if (isArray(type)) {
                result = handleNameType(name, stateVariable.getArrayType());
            } else {
                result = handleNameType(name, type);
            }

            // The news is bad....
            if (result != null) {
                clearNamespace();
                return bshErr + "\n" + result;
            }
            nsSets.add(name);
        }
        
        // Sim parameters
        for (ViskitElement par : getSimParmsList()) {
            String result;
            type = par.getType();
            name = par.getName();
            if (isArray(type)) {
                result = handleNameType(name, par.getArrayType());
            } else {
                result = handleNameType(name, type);
            }
            if (result != null) {
                clearNamespace();
                return bshErr + "\n" + result;
            }
            nsSets.add(name);
        }
        
        /* see if we can parse it.  We've initted all arrays to size = 1, so 
         * ignore outofbounds exceptions, bugfix 1183
         */
        try {
            /* Ignore anything that is assigned from a "getter" as we are not
             * giving beanShell the whole EG picture.
             */
            if(!noCRs.contains("get")) {
                Object o = interpreter.eval(noCRs);
                log.debug("Interpreter evaluation result: " + o);
            }            
        } catch (EvalError evalError) {                    
            if (!evalError.toString().contains("java.lang.ArrayIndexOutOfBoundsException")) {
                clearNamespace();
                return bshErr + "\n" + evalError.getMessage();
            } // else fall through the catch
        }
        clearNamespace();
        return null;    // null means good parse!
    }
    
    public boolean isArray(String ty) {
        return ty.contains("[") && ty.contains("]");
    }
    
    // TODO: Fix the logic here, it doesn't seem to get used correctly
    private void clearNamespace() {
        for (String ns : nsSets) {
            try {
                interpreter.unset(ns);
            } catch (EvalError evalError) {
                log.error(evalError);
            }
        }
        nsSets.clear();
    }

    private String handleNameType(String name, String typ) {
        String returnString = null;
        if (!handlePrimitive(name, typ)) {
            returnString = (findType(name, typ));
        }
        
        // good if remains null
        return returnString;
    }

    @SuppressWarnings("unchecked")
    private String findType(String name, String type) {        
        String returnString = null;
        try {
            if (isGeneric(type)) {
                type = type.substring(0, type.indexOf("<"));
            }
            Object o = instantiateType(type);
            
            // At this time, only default, no argument contructors can be set
            if (o != null) {
                if (o instanceof Collection) {
                    ((Collection) o).add("E");
                }
                if (o instanceof Map) {
                    ((Map) o).put("K", "V");
                }
                
                interpreter.set(name, o);
            } /*else {
                returnString = "no error, but not null";
            }*/
            
            /* TODO: the above else is a placeholder for when we implement full
             * beahshell checking
             */
              
        } catch (Exception ex) {
            clearNamespace();
            returnString =  ex.getMessage();
        }
        
        // good if remains null
        return returnString;
    }
    
    public boolean isGeneric(String type) {
        return (type.contains("<") && type.contains(">"));
    }

    private Object instantiateType(String type) throws Exception {
        Object o = null;
        boolean isArr = false;
        
        // TODO: Have to get viskit.VsimkitObjects to work first
        if (isSimkitDotRandom(type)) {return o;}
        
        if (type.contains("[")) {
            type = type.substring(0, type.length() - "[]".length());
            isArr = true;
        }
        try {
            Class<?> c = Vstatics.classForName(type);           
            if (c == null) {throw new Exception("Class not found: " + type);}
            
            Constructor<?>[] constructors = c.getConstructors();
            
            // The first constructor should be the default, no argument one
            for (Constructor constructor : constructors) {
                if (constructor.getParameterTypes().length == 0) {
                    if (isArr) {
                        o = Array.newInstance(c, 1);
                    } else {
                        o = c.newInstance();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        
        // TODO: Fix the call to VsimkitObjects someday
//        if (o == null) {
//            try {
//                o = VsimkitObjects.getInstance(type);
//            } catch (Exception e) {
//                throw new Exception(e);
//            }
//        }

        return o;
    }

    private boolean handlePrimitive(String name, String typ) {
        try {
            if (typ.equals("int")) {
                interpreter.eval("int " + name + " = 0");
                return true;
            }
            if (typ.equals("int[]")) {
                interpreter.eval("int[] " + name + " = new int[1]");
                return true;
            }
            if (typ.equals("boolean")) {
                interpreter.eval("boolean " + name + " = false");  // 17Aug04, should have always defaulted to false
                return true;
            }
            if (typ.equals("boolean[]")) {
                interpreter.eval("boolean[] " + name + " = new boolean[1]");
                return true;
            }
            if (typ.equals("double")) {
                interpreter.eval("double " + name + " = 0.0d");
                return true;
            }
            if (typ.equals("double[]")) {
                interpreter.eval("double[] " + name + " = new double[1]");
                return true;
            }
            if (typ.equals("float")) {
                interpreter.eval("float " + name + " = 0.0f");
                return true;
            }
            if (typ.equals("float[]")) {
                interpreter.eval("float[] " + name + " = new float[1]");
                return true;
            }
            if (typ.equals("byte")) {
                interpreter.eval("byte " + name + " = 0");
                return true;
            }
            if (typ.equals("byte[]")) {
                interpreter.eval("byte[] " + name + " = new byte[1]");
                return true;
            }
            if (typ.equals("char")) {
                interpreter.eval("char " + name + " = '0'");
                return true;
            }
            if (typ.equals("char[]")) {
                interpreter.eval("char[] " + name + " = new char[1]");
                return true;
            }
            if (typ.equals("short")) {
                interpreter.eval("short " + name + " = 0");
                return true;
            }
            if (typ.equals("short[]")) {
                interpreter.eval("short[] " + name + " = new short[1]");
                return true;
            }
            if (typ.equals("long")) {
                interpreter.eval("long " + name + " = 0");
                return true;
            }
            if (typ.equals("long[]")) {
                interpreter.eval("long[] " + name + " = new long[1]");
                return true;
            }
        } catch (EvalError evalError) {
            log.error(bshErr);
            evalError.printStackTrace();
        }
        return false;
    }

    /* Dynamic variable type list processing.  Build Type combo boxes and manage
     * user-typed object types.
     */
    private String moreTypesString = "more...";
    private String[] defaultTypeStrings = {
            "int",
            "double",
            "Integer",
            "Double",
            "String",
            moreTypesString};
    private String[] morePackages = {"primitives", "java.lang", "java.util", "simkit.random", "cancel"};
    private final int PRIMITIVES_INDEX = 0; // for moreClasses array
    private final int JAVA_LANG_INDEX = 1; // for moreClasses array
    private final int JAVA_UTIL_INDEX = 2; // for moreClasses array
    private final int SIMKIT_RANDOM_INDEX = 3; // for moreClasses array

    private String[][] moreClasses =
            {{"boolean", "byte", "char", "double", "float", "int", "long", "short"},
            {"Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Short", "String", "StringBuffer"},
            {"HashMap<K,V>", "HashSet<E>", "LinkedList<E>", "Properties", "Random", "TreeMap<K,V>", "TreeSet<E>", "Vector<E>"},
            {"RandomNumber", "RandomVariate"}, {}
    };
    
    /** @param ty the type to check if primitive or array
     * @return true if primitive or array
     */
    public boolean isPrimitiveOrPrimitiveArray(String ty) {
        int idx;
        if ((idx = ty.indexOf('[')) != -1) {
            ty = ty.substring(0, idx);
        }
        return isPrimitive(ty);
    }
    
    /** @param ty the type to check if primitive type
     * @return true if primitive type
     */
    public boolean isPrimitive(String ty) {
        for (String s : moreClasses[PRIMITIVES_INDEX]) {
            if (ty.equals(s)) {
                return true;
            }
        }
        return false;
    }
    
    /** @param ty the type to check if member of java.lang.*
     * @return true if member of java.lang.*
     */
    public boolean isJavaDotLang(String ty) {
        for (String s : moreClasses[JAVA_LANG_INDEX]) {
            if (s.contains(ty)) {
                return true;
            }
        }
        return false;
    }
    
    /** The simple (basic) class name is required for this
     * @param ty the type to check if member of java.util.*
     * @return true if member of java.util.*
     */
    public boolean isJavaDotUtil(String ty) {
        for (String s : moreClasses[JAVA_UTIL_INDEX]) {
            if (s.contains(ty)) {
                return true;
            }
        }
        return false;
    }    
    
    /**@param ty the type to check if member of simkit.random.*
     * @return true if member of simkit.random.*
     */
    public boolean isSimkitDotRandom(String ty) {
        for (String s : moreClasses[SIMKIT_RANDOM_INDEX]) {
            if (s.contains(ty)) {
                return true;
            }
        }
        return false;
    }
    
    Pattern bracketsPattern = Pattern.compile("\\[.*?\\]");
    Pattern spacesPattern = Pattern.compile("\\s");

    public String stripArraySize(String typ) {
        Matcher m = bracketsPattern.matcher(typ);
        String r = m.replaceAll("[]");            // [blah] with[]
        m = spacesPattern.matcher(r);
        return m.replaceAll("");
    }

    public String[] getArraySize(String typ) {
        Vector<String> v = new Vector<String>();
        Matcher m = bracketsPattern.matcher(typ);

        while (m.find()) {
            String g = m.group();
            v.add(g.substring(1, g.length() - 1).trim());
        }
        if (v.size() <= 0) {
            return null;
        }
        return toArray(v, new String[0]);
    }

    /**
     * This is messaged by dialogs and others when a user has selected a type for a new variable.  We look
     * around to see if we've already got it covered.  If not, we add it to the end of the list.
     * @param ty
     * @return 
     */
    public String typeChosen(String ty) {
        ty = ty.replaceAll("\\s", "");              // every whitespace removed
        for (int i = 0; i < cbMod.getSize(); i++) {
            if (cbMod.getElementAt(i).toString().equals(ty)) {
                return ty;
            }
        }
        // else, put it at the end, but before the "more"
        cbMod.insertElementAt(ty, cbMod.getSize() - 1);
        return ty;
    }

    public JComboBox getTypeCB() {
        JComboBox cb = new JComboBox(cbMod);
        cb.addActionListener(myListener);
        cb.addItemListener(myListener);
        cb.setRenderer(new myTypeListRenderer());
        cb.setEditable(true);
        return cb;
    }

    private void buildTypePopup() {

        popup = new JPopupMenu();
        JMenu m;
        JMenuItem mi;
       
        for (int i = 0; i < morePackages.length; i++) {
            if (moreClasses[i].length <= 0) {           // if no classes, make the "package selectable
                mi = new MyJMenuItem(morePackages[i], null);
                mi.addActionListener(myListener);
                popup.add(mi);
            } else {
                m = new JMenu(morePackages[i]);
                for (int j = 0; j < moreClasses[i].length; j++) {
                    if (i == PRIMITIVES_INDEX) {
                        mi = new MyJMenuItem(moreClasses[i][j], moreClasses[i][j]);
                    } // no package
                    else {
                        mi = new MyJMenuItem(moreClasses[i][j], morePackages[i] + "." + moreClasses[i][j]);
                    }
                    mi.addActionListener(myListener);
                    m.add(mi);
                }
                popup.add(m);
            }
        }
    }
    JComboBox pending;
    Object lastSelected = "void";

    public RunnerPanel2 getRunPanel() {
        return runPanel;
    }

    public void setRunPanel(RunnerPanel2 runPanel) {
        this.runPanel = runPanel;
    }

    class myTypeListener implements ActionListener, ItemListener {

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                lastSelected = e.getItem();
            }
        }

        public void actionPerformed(ActionEvent e) {
            Object o = e.getSource();
            if (o instanceof JComboBox) {
                JComboBox cb = (JComboBox) o;
                pending = cb;
                if (cb.getSelectedItem().toString().equals(moreTypesString)) {
                    popup.show(cb, 0, 0);
                }
            } else {
                MyJMenuItem mi = (MyJMenuItem) o;
                if (!mi.getText().equals("cancel")) {
                    pending.setSelectedItem(mi.getFullName());
                } //mi.getText());
                else {
                    pending.setSelectedItem(lastSelected);
                }
            }
        }
    }

    class myTypeListRenderer extends JLabel implements ListCellRenderer {
        //Font specialFont = getFont().deriveFont(Font.ITALIC);

        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lab = new JLabel(value.toString());
            if (value.toString().equals(moreTypesString)) {
                lab.setBorder(BorderFactory.createRaisedBevelBorder());
            } //createEtchedBorder());
        //lab.setFont(specialFont);
            return lab;
        }
    }
    Vector<String> existingNames = new Vector<String>();
    Vector existingAssemblyNames = new Vector();
    
    /**
     * @param nm 
     * @return true if the data is valid, eg we have a valid parameter name
     * and a valid type.
     */
    private boolean checkLegalJavaName(String nm) {

        String javaVariableNameRegExp;

        // Do a REGEXP to confirm that the variable name fits the criteria for
        // a Java variable. We don't want to allow something like "2f", which
        // Java will misinterpret as a number literal rather than a variable. This regexp
        // is slightly more restrictive in that it demands that the variable name
        // start with a lower case letter (which is not demanded by Java but is
        // a strong convention) and disallows the underscore. "^" means it
        // has to start with a lower case letter in the leftmost position.

        javaVariableNameRegExp = "^[a-z][a-zA-Z0-9]*$";
        if (!Pattern.matches(javaVariableNameRegExp, nm)) {
            JOptionPane.showMessageDialog(null,
                    "variable names must start with a lower case letter and conform to the Java variable naming conventions",
                    "alert",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Check to make sure the name the user specified isn't already used by a state variable
        // or parameter.

        for (String existingName : existingNames) {
            if (nm.equals(existingName)) {
                JOptionPane.showMessageDialog(null,
                        "variable names must be unique and not match any existing parameter or state variable name",
                        "alert",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        existingNames.add(nm);
        return true;
    }

    public void reset() {
        existingNames.clear();
    }

    public void assemblyReset() {
        existingAssemblyNames.clear();
    }
    
    /**
     * Small class to hold on to the fully-qualified class name, while displaying only the
     * un-qualified name;
     */
    class MyJMenuItem extends JMenuItem {

        private String fullName;

        MyJMenuItem(String nm, String fullName) {
            super(nm);
            this.fullName = fullName;
        }

        public String getFullName() {
            return fullName;
        }
    }

    public File getWorkDirectory() {
        if ( !workDirectory.exists() ) {
            workDirectory.mkdir();
        } 
        return workDirectory;
    }
    private File workDirectory;

    private void setupWorkDirectory() {

        List<String> cache = Arrays.asList(getHistoryConfig().getStringArray("Cached.EventGraphs" + "[@xml]"));

        if (cache.isEmpty()) {
            newWorkDir();
        } else {
            workDirectory = new File(getHistoryConfig().getString("Cached[@workDir]"));
            if (workDirectory == null) {
                newWorkDir();
            } else if (workDirectory.listFiles().length == 0) {
                newWorkDir();
            }
        }
    }

    private void newWorkDir() {
        try {
            workDirectory = File.createTempFile("viskit", "work");   // actually creates
            String p = workDirectory.getAbsolutePath();   // just want the name part of it
            workDirectory.delete();        // Don't want the file to be made yet
            workDirectory = new File(p);
            workDirectory.mkdir();
            workDirectory.deleteOnExit();

            /* TODO: nothing really seems to ever go in here, but it deletes on
             * JVM exit anyway
             */
            File nf = new File(workDirectory, "simkit");     // most go here
            nf.mkdir();
            nf.deleteOnExit();
            nf = new File(nf, "examples");
            nf.mkdir();
            nf.deleteOnExit();
            return;
        } catch (IOException e) {
            log.error(e.toString());
        }

        if (workDirectory.mkdir() == false) {
            JOptionPane.showMessageDialog(null, "The directory " + workDirectory.getPath() +
                    " could not be created.  Correct permissions before proceeding.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private XMLConfiguration hConfig;
    private String userConfigPath = System.getProperty("user.home") +
            System.getProperty("file.separator") + ".viskit_history.xml";
    private ClassLoader workLoader;

    public ClassLoader getWorkClassLoader() {
        if (workLoader == null) {
            LocalBootLoader loader = new LocalBootLoader(
                    SettingsDialog.getExtraClassPathArraytoURLArray(), 
                    Thread.currentThread().getContextClassLoader(), 
                    getWorkDirectory());
            workLoader = loader.init(true);
        }
        return workLoader;
    }

    public void resetWorkClassLoader() {
        workLoader = null;
    }

    public ClassLoader getWorkClassLoader(boolean reboot) {
        resetWorkClassLoader();
        return getWorkClassLoader();
    }

    public String getUserConfigFile() {
        return userConfigPath;
    }

    public XMLConfiguration getHistoryConfig() {
        if (hConfig == null) {
            try {
                File hf = new File(userConfigPath);
                System.out.println("Loading history configuration from " + hf);
                if (!hf.exists() || hf.length() < 3) {
                    File src = new File("c_history_template.xml");
                    hf.createNewFile();
                    FileIO.copyFile(src, hf, true);
                }
                hConfig = ViskitConfig.instance().getIndividualXMLConfig(hf.getAbsolutePath());
            } catch (Exception e) {
                log.error("Error loading history file: " + e.getMessage());
                log.error("Recent file saving disabled");
                hConfig = null;
            }
        }
        return hConfig;
    }
    
    private SysExitHandler sysexithandler = new SysExitHandler() {

        public void doSysExit(int status) {

            log.debug("Viskit is exiting with status: " + status);

            /* If an application launched a JVM, and is still running, these
             * will only make Viskit disappear. If Viskit is running standalone, 
             * then when all JFrames created by Viskit will dispose, and the JVM
             * will then cease.
             * @see http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#19152
             * @see http://72.5.124.55/javase/6/docs/api/java/awt/doc-files/AWTThreadIssues.html
             */
            Frame[] frames = Frame.getFrames();
            int count = 0;
            for (Frame f : frames) {
                log.debug("Frame count in Viskit: " + (++count));
                log.debug("Frame is: " + f);

                /* Prevent non-viskit components from disposing if launched from
                 * another application.  SwingUtilities is a little "ify" though
                 * as it's not Viskit specific.  Viskit, however, spawns a lot
                 * of anonymous Runnables with SwingUtilities
                 */
                if (f.toString().contains("viskit") || f.toString().contains("SwingUtilities")) {
                    f.dispose();
                }
                // Case for XMLTree JFrames
                if (f.getTitle().contains("xml")) {
                    f.dispose();
                }
            }
            
            /* The SwingWorker Thread is active when the assembly runner is
             * running and will subsequently block a JVM exit due to its "wait"
             * state.  Must interrupt it in order to cause the JVM to exit
             * @see docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
             */
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            for (Thread t : threads) {
                log.debug("Thread is: " + t);
                if (t.getName().contains("SwingWorker")) {
                    t.interrupt();
                }
            }
        }
    };

    public void setSysExitHandler(SysExitHandler handler) {
        sysexithandler = handler;
    }

    public SysExitHandler getSysExitHandler() {
        return sysexithandler;
    }

    /** Called to perform proper thread shutdown without calling System.exit(0)
     * 
     * @param status the status of JVM shutdown
     */
    public void sysExit(int status) {
        if (!sysExitCalled) {
            sysexithandler.doSysExit(status);
            sysExitCalled = true;
        }
    }

    public JFrame getMainAppWindow() {
        return mainAppWindow;
    }

    public void setMainAppWindow(JFrame mainAppWindow) {
        this.mainAppWindow = mainAppWindow;
    }
}