/*
Copyright (c) 1995-2009 held by the author(s).  All rights reserved.

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

import viskit.control.EventGraphControllerImpl;
import viskit.control.AssemblyControllerImpl;
import viskit.view.ViskitProjectButtonPanel;
import viskit.view.RunnerPanel2;
import viskit.view.dialog.SettingsDialog;
import viskit.view.AssemblyViewFrame;
import viskit.view.EventGraphViewFrame;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import static edu.nps.util.GenericConversion.toArray;
import edu.nps.util.LogUtils;
import edu.nps.util.SysExitHandler;
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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import org.apache.log4j.Logger;
import viskit.control.AssemblyController;
import viskit.doe.LocalBootLoader;
import viskit.model.AssemblyModel;
import viskit.model.EventNode;
import viskit.model.ViskitElement;
import viskit.model.Model;
import viskit.mvc.mvcController;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 5, 2004
 * @since 3:20:33 PM
 * @version $Id$
 */
public class VGlobals {

    private static final String BEAN_SHELL_ERROR = "BeanShell eval error";
    static Logger log = LogUtils.getLogger(VGlobals.class);
    private static VGlobals me;
    private Interpreter interpreter;
    private DefaultComboBoxModel<String> cbMod;
    private JPopupMenu popup;
    private myTypeListener myListener;
    private JFrame mainAppWindow;

    private ViskitProject currentViskitProject;

    /** Need hold of the Enable Analyst Reports checkbox */
    private RunnerPanel2 runPanel;

    /** Flag to denote called sysExit only once */
    private boolean sysExitCalled = false;

    /** The current project working directory */
    private File workDirectory;

    /** The current project base directory */
    private File projectsBaseDir;

    public static synchronized VGlobals instance() {
        if (me == null) {
            me = new VGlobals();
        }
        return me;
    }

    private VGlobals() {
        cbMod = new DefaultComboBoxModel<>(new Vector<>(Arrays.asList(defaultTypeStrings)));
        myListener = new myTypeListener();
        buildTypePopup();
        initProjectHome();
        createWorkDirectory();
    }

    /* routines to manage the singleton-aspect of the views. */
    AssemblyViewFrame avf;
    mvcController acont;
    public AssemblyModel amod;
    boolean assyFirstRun = false;

    /**
     * Get a reference to the assembly editor view.
     * @return a reference to the assembly editor view or null if yet unbuilt.
     */
    public AssemblyViewFrame getAssemblyEditor() {
        return avf;
    }

    /** Used only if a call to {@link #getAssemblyEditor()} ever returns null
     *
     * @return the component AssemblyViewFrame
     */
    public AssemblyViewFrame buildAssemblyViewFrame() {
        AssemblyControllerImpl cont = new AssemblyControllerImpl();
        return buildAssemblyViewFrame(cont);
    }

    /** Called from the EventGraphAssemblyComboMainFrame to initialize UI startup
     *
     * @return the component AssemblyViewFrame
     */
    public AssemblyViewFrame initAssemblyViewFrame() {
        AssemblyControllerImpl cont = new AssemblyControllerImpl();
        return initAssemblyViewFrame(cont);
    }

    private AssemblyViewFrame buildAssemblyViewFrame(AssemblyController cont) {
        initAssemblyViewFrame(cont);
        cont.begin();
        return avf;
    }

    private AssemblyViewFrame initAssemblyViewFrame(AssemblyController cont) {
        acont = (mvcController) cont;
        avf = new AssemblyViewFrame((mvcController) cont);
        ((AssemblyControllerImpl)cont).setView(avf);
        return avf;
    }

    public void rebuildTreePanels() {
        avf.rebuildTreePanels();
    }

    public AssemblyModel getActiveAssemblyModel() {
        return amod;
    }

    public mvcController getAssemblyController() {
        return acont;
    }

    public void runAssemblyView() {
        if (avf == null) {
            buildAssemblyViewFrame();
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                avf.setVisible(true);
                avf.toFront();

                if (!assyFirstRun) {
                    assyFirstRun = true;
                    ((AssemblyController)acont).newAssembly();
                }
            }
        });
    }
    ActionListener defaultAssyQuitHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (avf != null) {
                        avf.setVisible(false);
                    }
                    if (egvf != null && egvf.isVisible()) {
                        // do nothing at the moment
                    }
                }
            });
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

    /* EventGraphViewFrame / EventGraphControllerImpl */

    EventGraphViewFrame egvf;
    mvcController eContl;

    public EventGraphViewFrame getEventGraphEditor() {
        return egvf;
    }

    public EventGraphViewFrame buildEventGraphViewFrame() {
        return buildEventGraphViewFrame(new EventGraphControllerImpl());
    }

    public EventGraphViewFrame buildEventGraphViewFrame(EventGraphControllerImpl cont) {
        initEventGraphViewFrame(cont);
        cont.begin();
        return egvf;
    }

    /** This method starts the chain of various Viskit startup steps.  By
     * calling for a new EventGraphControllerImpl(), in its constructor is a call
     * to initConfig() which is the first time that the viskitConfig.xml is
     * looked for, or if one is not there, to create one from the template.  The
     * viskitConfig.xml is an important file that holds information on recent
     * assembly and event graph openings, and caching of compiled source from
     * EventGraphs.
     *
     * @return an instance of the EventGraphViewFrame
     */
    public EventGraphViewFrame initEventGraphViewFrame() {
        return initEventGraphViewFrame(new EventGraphControllerImpl());
    }

    public EventGraphViewFrame initEventGraphViewFrame(EventGraphControllerImpl cont) {
        egvf = new EventGraphViewFrame(cont);
        cont.setView(egvf);
        eContl = cont;
        return egvf;
    }

    public mvcController getEventGraphController() {
        return eContl;
    }

    public Model getActiveEventGraphModel() {
        return (Model) eContl.getModel();
    }

    ActionListener defaultEventGraphQuitHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (egvf != null) {
                egvf.setVisible(false);
            }
            if (avf != null && avf.isVisible()) {
                // do nothing at the moment
            }
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

    private Vector<ViskitElement> getStateVarsList() {
        return getActiveEventGraphModel().getStateVariables();
    }

    private Vector<? extends ViskitElement> getSimParmsList() {
        return getActiveEventGraphModel().getSimParameters();
    }

    public ComboBoxModel<ViskitElement> getStateVarsCBModel() {
        return new DefaultComboBoxModel<>(getStateVarsList());
    }

    /******/
    /* Beanshell code */
    /******/
    private void initBeanShell() {

        if (interpreter == null) {
            interpreter = new Interpreter();
            interpreter.setStrictJava(true);       // no loose typing
        }

        String[] workCP = VStatics.getExtraClassPathArray();
        if (workCP != null && workCP.length > 0) {
            for (String path : workCP) {
                try {
                    interpreter.getClassManager().addClassPath(new URL("file", "localhost", path));
                } catch (IOException e) {
                    log.error("Working classpath component: " + path);
                }
            }
        }

        NameSpace ns = interpreter.getNameSpace();
        ns.importPackage("simkit.*");
        ns.importPackage("simkit.random.*");
        ns.importPackage("simkit.smdx.*");
        ns.importPackage("simkit.stat.*");
        ns.importPackage("simkit.util.*");
        ns.importPackage("diskit.*");         // 17 Nov 2004
    }

    /** Use BeanShell for code parsing to detect potential errors
     *
     * @param node the SimEntity node being evaluated
     * @param interpretString the code block to check
     * @return any indication of a parsing error.  A null means all is good.
     */
    public String parseCode(EventNode node, String interpretString) {
        initBeanShell();
        // Load the interpreter with the state variables and the sim parameters
        // Load up any local variables and event parameters for this particular node
        // Then, parse.

        // Lose the new lines
        String noCRs = interpretString.replace('\n', ' ');

        String name;
        String type;

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
                    clearClassPath();
                    return BEAN_SHELL_ERROR + "\n" + result;
                }
            }

            // Event arguments
            for (ViskitElement ea : node.getArguments()) {
                type = ea.getType();
                name = ea.getName();
                String result = handleNameType(name, type);
                if (result != null) {
                    clearNamespace();
                    clearClassPath();
                    return BEAN_SHELL_ERROR + "\n" + result;
                }
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
                clearClassPath();
                return BEAN_SHELL_ERROR + "\n" + result;
            }
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
                clearClassPath();
                return BEAN_SHELL_ERROR + "\n" + result;
            }
        }

        // Unfortunately, since we are not giving BeanShell the full access to
        // source code, we can not check things like adding and removing from
        // Lists as the variable name for the list is unknown just from the code
        // snippet.  Therefore, we comment this sectout out.
//        /* see if we can parse it.  We've initted all arrays to size = 1, so
//         * ignore outofbounds exceptions, bugfix 1183
//         */
//        try {
//            /* Ignore anything that is assigned from "getter" and "setter" as we
//             * are not giving beanShell the whole EG picture.
//             */
//            if(!noCRs.contains("get") && !noCRs.contains("set")) {
//                Object o = interpreter.eval(noCRs);
//                log.debug("Interpreter evaluation result: " + o);
//            }
//        } catch (EvalError evalError) {
//            if (!evalError.toString().contains("java.lang.ArrayIndexOutOfBoundsException")) {
//                clearNamespace();
//                clearClassPath();
//                return BEAN_SHELL_ERROR + "\n" + evalError.getMessage();
//            } // else fall through the catch
//        }
        clearNamespace();
        clearClassPath();
        return null;    // null means good parse!
    }

    public boolean isArray(String ty) {
        return ty.contains("[") && ty.contains("]");
    }

    // TODO: Fix the logic here, it doesn't seem to get used correctly
    private void clearNamespace() {
        interpreter.getNameSpace().clear();
    }

    private void clearClassPath() {
        interpreter.getClassManager().reset();
    }

    private String handleNameType(String name, String typ) {
        String returnString = null;
        if (!handlePrimitive(name, typ)) {
            returnString = findType(name, typ);
        }

        // good if remains null
        return returnString;
    }

    private String findType(String name, String type) {
        String returnString = null;
        try {
            if (isGeneric(type)) {
                type = type.substring(0, type.indexOf("<"));
            }
            Object o = instantiateType(type);

            // At this time, only default no argument contructors can be set
            if (o != null) {
                interpreter.set(name, o);
            } /*else {
                returnString = "no error, but not null";
            }*/

            /* TODO: the above else is a placeholder for when we implement full
             * beahshell checking
             */

        } catch (Exception ex) {
            returnString =  ex.getMessage();
            log.error(returnString);
        }

        // good if remains null
        return returnString;
    }

    public boolean isGeneric(String type) {
        return (type.contains("<") && type.contains(">"));
    }

    /** The entry point for Viskit startup.  This method will either identify a
     * recorded project space, or launch a dialog asking the user to either
     * create a new project space, or open another existing one, or exit Viskit
     */
    public final void initProjectHome() {

        ViskitConfig vConfig = ViskitConfig.instance();
        String projectHome = vConfig.getVal(ViskitConfig.PROJECT_PATH_KEY);
        log.debug(projectHome);
        if (projectHome.isEmpty() || !(new File(projectHome).exists())) {
            ViskitProjectButtonPanel.showDialog();
        } else {
            ViskitProject.MY_VISKIT_PROJECTS_DIR = projectHome;
        }
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
            Class<?> c = VStatics.classForName(type);
            if (c == null) {throw new Exception("Class not found: " + type);}

            Constructor<?>[] constructors = c.getConstructors();

            // The first constructor should be the default, no argument one
            for (Constructor<?> constructor : constructors) {
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
            log.error(evalError);
//            evalError.printStackTrace();
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
        Vector<String> v = new Vector<>();
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
     * @param ty the type to evaluate
     * @return the String representation of this type if found
     */
    public String typeChosen(String ty) {
        ty = ty.replaceAll("\\s", "");              // every whitespace removed
        for (int i = 0; i < cbMod.getSize(); i++) {
            if (cbMod.getElementAt(i).equals(ty)) {
                return ty;
            }
        }
        // else, put it at the end, but before the "more"
        cbMod.insertElementAt(ty, cbMod.getSize() - 1);
        return ty;
    }

    public JComboBox<String> getTypeCB() {
        JComboBox<String> cb = new JComboBox<>(cbMod);
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
                for (String item : moreClasses[i]) {
                    if (i == PRIMITIVES_INDEX) {
                        mi = new MyJMenuItem(item, item);
                    } // no package
                    else {
                        mi = new MyJMenuItem(item, morePackages[i] + "." + item);
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

    public ViskitProject getCurrentViskitProject() {
        return currentViskitProject;
    }

    /**
     * @return a working directory which is typically an open project's build/
     * classes directory
     */
    public File getWorkDirectory() {
        return workDirectory;
    }

    /**
     * Not the best Java Bean convention, but performs as a no argument setter
     * for the an open project's working directory (build/classes)
     */
    public final void createWorkDirectory() {
        ViskitConfig vConfig = ViskitConfig.instance();
        if (vConfig.getViskitAppConfig() == null) {
            return;
        }

        String projectName = vConfig.getVal(ViskitConfig.PROJECT_NAME_KEY);
        if ((projectName != null) && (!projectName.isEmpty())) {
            ViskitProject.DEFAULT_PROJECT_NAME = projectName;
        }
        projectsBaseDir = new File(ViskitProject.MY_VISKIT_PROJECTS_DIR);
        currentViskitProject = new ViskitProject(new File(projectsBaseDir, ViskitProject.DEFAULT_PROJECT_NAME));

        if (currentViskitProject.initProject()) {
            SettingsDialog.saveExtraClassPathEntries(currentViskitProject.getProjectContents());
        } else {
            throw new RuntimeException("Unable to create project directory");
        }
        workDirectory = currentViskitProject.getClassesDir();
    }

    private ClassLoader workLoader;

    /**
     * Retrieve Viskit's working ClassLoader.  It may be reset from time to
     * time if extra classpaths are loaded
     * @return Viskit's working ClassLoader
     */
    public ClassLoader getWorkClassLoader() {
        if (workLoader == null) {
            URL[] urlArray = SettingsDialog.getExtraClassPathArraytoURLArray();

            LocalBootLoader loader = new LocalBootLoader(urlArray,
                    Thread.currentThread().getContextClassLoader(),
                    getWorkDirectory());

            // Allow Assembly files in the ClassLoader
            workLoader = loader.init(true);
        }
        return workLoader;
    }

    public void resetWorkClassLoader() {
        workLoader = null;
    }

    /** @return a model to print a stack trace of calling classes and their methods */
    @SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown", "ThrowableResultIgnored"})
    public String printCallerLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("Calling class: ").append(new Throwable().fillInStackTrace().getStackTrace()[4].getClassName());
        sb.append("\nCalling method: ").append(new Throwable().fillInStackTrace().getStackTrace()[4].getMethodName());
        return sb.toString();
    }

    private SysExitHandler sysexithandler = new SysExitHandler() {

        @Override
        public void doSysExit(int status) {

            log.debug("Viskit is exiting with status: " + status);

            /* If an application launched a JVM, and is still running, this will
             * only make Viskit disappear.  If Viskit is running standalone,
             * then then all JFrames created by Viskit will dispose, and the JVM
             * will then cease.
             * @see http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#19152
             * @see http://72.5.124.55/javase/6/docs/api/java/awt/doc-files/AWTThreadIssues.html
             */
            Frame[] frames = Frame.getFrames();
            int count = 0;
            for (Frame f : frames) {
                log.debug("Frame count in Viskit: " + (++count));

                /* Prevent non-viskit created components from disposing if
                 * launched from another application.  SwingUtilities is a
                 * little "ify" though as it's not Viskit specific.  Viskit,
                 * however, spawns a lot of anonymous Runnables with
                 * SwingUtilities
                 */
                if (f.toString().toLowerCase().contains("viskit")) {
                    log.debug("Frame is: " + f);
                    f.dispose();
                }
                if (f.toString().contains("SwingUtilities")) {
                    log.debug("Frame is: " + f);
                    f.dispose();
                }

                // Case for XMLTree JFrames
                if (f.getTitle().contains("xml")) {
                    log.debug("Frame is: " + f);
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
                // Now attempt to release the URLClassLoader's file lock on open JARs
                t.setContextClassLoader(ClassLoader.getSystemClassLoader());
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
        if (!isSysExitCalled()) {
            sysexithandler.doSysExit(status);
            setSysExitCalled(true);
        }
    }

    public JFrame getMainAppWindow() {
        return mainAppWindow;
    }

    public void setMainAppWindow(JFrame mainAppWindow) {
        this.mainAppWindow = mainAppWindow;
    }

    public boolean isSysExitCalled() {
        return sysExitCalled;
    }

    public void setSysExitCalled(boolean sysExitCalled) {
        this.sysExitCalled = sysExitCalled;
    }

    /**
     * Small class to hold on to the fully-qualified class name, while displaying only the
     * un-qualified name;
     */
    @SuppressWarnings("serial")
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

    class myTypeListener implements ActionListener, ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                lastSelected = e.getItem();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object o = e.getSource();
            if (o instanceof JComboBox) {
                JComboBox cb = (JComboBox) o;
                pending = cb;
                if (cb.getSelectedItem().toString().equals(moreTypesString)) {

                    // NOTE: was getting an IllegalComponentStateException for component not showing
                    if (popup.isShowing())
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

    @SuppressWarnings("serial")
    class myTypeListRenderer extends JLabel implements ListCellRenderer<String> {
        //Font specialFont = getFont().deriveFont(Font.ITALIC);

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel lab = new JLabel(value);
            if (value.equals(moreTypesString)) {
                lab.setBorder(BorderFactory.createRaisedBevelBorder());
            } //createEtchedBorder());
            //lab.setFont(specialFont);
            return lab;
        }
    }

}