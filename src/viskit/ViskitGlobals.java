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

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;

import edu.nps.util.GenericConversion;
import edu.nps.util.LogUtils;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

import org.apache.logging.log4j.Logger;

import viskit.control.AnalystReportController;
import viskit.control.AssemblyControllerImpl;
import viskit.control.EventGraphControllerImpl;
import viskit.doe.LocalBootLoader;
import viskit.model.AssemblyModel;
import viskit.model.AnalystReportModel;
import viskit.model.EventNode;
import viskit.model.Model;
import viskit.model.ViskitElement;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.view.AnalystReportViewFrame;
import viskit.view.AssemblyViewFrame;
import viskit.view.EventGraphViewFrame;
import viskit.view.SimulationRunPanel;
import viskit.view.ViskitStartupProjectInitializationPanel;
import viskit.view.dialog.SettingsDialog;
import viskit.mvc.MvcController;
import edu.nps.util.SystemExitHandler;
import viskit.control.TextAreaOutputStream;
import viskit.view.MainFrame;

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
public class ViskitGlobals {

    private static final String BEAN_SHELL_ERROR = "BeanShell eval error";
    static final Logger LOG = LogUtils.getLogger(ViskitGlobals.class);
    private static ViskitGlobals viskitGlobals;
    private Interpreter interpreter;
    private final DefaultComboBoxModel<String> defaultComboBoxModel;
    private JPopupMenu popupMenu;
    private final MyTypeListener myTypeListener;
    private MainFrame mainFrame;

    private ViskitProject ViskitProject;

    /** Need hold of the Enable Analyst Reports checkbox and number of replications, likely others too */
    private SimulationRunPanel simulationRunPanel;
    
    private TextAreaOutputStream internalSimulationRunner;

    /** Flag to denote called systemExit only once */
    private boolean systemExitCalled = false;

    /** The current project working directory */
    private File workingDirectory;

    /** The current project base directory */
    private File projectsBaseDirectory;

    /** The main app JavaHelp set */
    private Help help;

    public static synchronized ViskitGlobals instance() {
        if (viskitGlobals == null) {
            viskitGlobals = new ViskitGlobals();
        }
        return viskitGlobals;
    }

    private ViskitGlobals() {
        defaultComboBoxModel = new DefaultComboBoxModel<>(new Vector<>(Arrays.asList(defaultTypeStrings)));
        myTypeListener = new MyTypeListener();
        buildTypePopup();
        inititalizeProjectHome();
        createWorkingDirectory();
    }

    /* routines to manage the singleton-aspect of the views. */
    MvcAbstractViewFrame assemblyViewFrame;
    MvcController assemblyController;

    /**
     * Get a reference to the assembly editor view.
     * @return a reference to the assembly editor view or null if yet unbuilt.
     */
    public AssemblyViewFrame getAssemblyViewFrame() {
        return (AssemblyViewFrame) assemblyViewFrame;
    }

    /** Called from the EventGraphAssemblyComboMainFrame to initialize at UI startup
     *
     * @return the component AssemblyViewFrame
     */
    public MvcAbstractViewFrame buildAssemblyViewFrame() {
        assemblyController = new AssemblyControllerImpl();
        assemblyViewFrame = new AssemblyViewFrame(assemblyController);
        assemblyController.setView(assemblyViewFrame);
        return assemblyViewFrame;
    }

    /** Rebuilds the Listener Event Graph Object (LEGO) panels on the Assembly Editor */
    public void rebuildLEGOTreePanels() {
        ((AssemblyViewFrame) assemblyViewFrame).rebuildLEGOTreePanels();
    }

    public AssemblyModel getActiveAssemblyModel() {
        return (AssemblyModel) assemblyController.getModel();
    }

    public MvcController getAssemblyController() {
        return assemblyController;
    }

    ActionListener defaultAssemblyQuitHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (assemblyViewFrame != null) {
                assemblyViewFrame.setVisible(false);
            }
        }
    };
    ActionListener assemblyQuitHandler = defaultAssemblyQuitHandler;

    public void quitAssemblyEditor() {
        if (assemblyQuitHandler != null) {
            assemblyQuitHandler.actionPerformed(new ActionEvent(this, 0, "quit Assembly editor"));
        }
    }

    public void setAssemblyQuitHandler(ActionListener lis) {
        assemblyQuitHandler = lis;
    }

    /* EventGraphViewFrame / EventGraphControllerImpl */

    MvcAbstractViewFrame eventGraphViewFrame;
    MvcController eContl;

    public EventGraphViewFrame getEventGraphViewFrame() {
        return (EventGraphViewFrame) eventGraphViewFrame;
    }

    /** This method starts the chain of various Viskit startup steps.  By
     * calling for a new EventGraphControllerImpl(), in its constructor is a call
     * to initConfig() which is the first time that the vConfig.xml is
     * looked for, or if one is not there, to create one from the template. The
     * vConfig.xml is an important file that holds information on recent
     * assembly and event graph openings, and caching of compiled source from
     * EventGraphs.
     *
     * @return an instance of the EventGraphViewFrame
     */
    public MvcAbstractViewFrame buildEventGraphViewFrame() {
        eContl = new EventGraphControllerImpl();
        eventGraphViewFrame = new EventGraphViewFrame(eContl);
        eContl.setView(eventGraphViewFrame);
        return eventGraphViewFrame;
    }

    public MvcController getEventGraphController() {
        return eContl;
    }

    public Model getActiveEventGraphModel() {
        return (Model) eContl.getModel();
    }

    ActionListener defaultEventGraphQuitHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (eventGraphViewFrame != null) {
                eventGraphViewFrame.setVisible(false);
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

    public Vector<ViskitElement> getStateVariablesList() {
        return getActiveEventGraphModel().getStateVariables();
    }

    public ComboBoxModel<ViskitElement> getStateVariablesCBModel() {
        return new DefaultComboBoxModel<>(getStateVariablesList());
    }

    public Vector<ViskitElement> getSimParametersList() {
        return getActiveEventGraphModel().getSimParameters();
    }

    public ComboBoxModel<ViskitElement> getSimParamsCBModel() {
        return new DefaultComboBoxModel<>(getSimParametersList());
    }

    /* AnalystReport model / view / controller */

    /* routines to manage the singleton-aspect of the view */
    MvcAbstractViewFrame aRf;
    MvcController aRcont;

    /**
     * Get a reference to the analyst report view.
     * @return a reference to the analyst report view or null if yet unbuilt.
     */
    public AnalystReportViewFrame getAnalystReportEditor() {
        return (AnalystReportViewFrame) aRf;
    }

    /** Called from the EventGraphAssemblyComboMainFrame to initialize at UI startup
     *
     * @return the component AnalystReportViewFrame
     */
    public MvcAbstractViewFrame buildAnalystReportFrame() {
        aRcont = new AnalystReportController();
        aRf = new AnalystReportViewFrame(aRcont);
        aRcont.setView(aRf);
        return aRf;
    }

    /** @return the analyst report builder (model) */
    public AnalystReportModel getAnalystReportModel() {
        return (AnalystReportModel) aRcont.getModel();
    }

    /** @return the analyst report controller */
    public MvcController getAnalystReportController() {
        return aRcont;
    }

    /******
     * Beanshell code
     ******/
    private void initBeanShell() {

        if (interpreter == null) {
            interpreter = new Interpreter();
            interpreter.setStrictJava(true);       // no loose typing
        }

        String[] workCP = SettingsDialog.getExtraClassPath();
        if (workCP != null && workCP.length > 0) {
            for (String path : workCP) {
                try {
                    interpreter.getClassManager().addClassPath(new URI("file", "localhost", path).toURL());
                } catch (IOException | URISyntaxException e) {
                    LOG.error("Working classpath component: " + path);
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
//        String noCRs = interpretString.replace('\n', ' ');

        String name;
        String type;

        if (node != null) {

            // Event local variables
            for (ViskitElement eventLocalVariable : node.getLocalVariables()) {
                String result;
                type = eventLocalVariable.getType();
                name = eventLocalVariable.getName();
                if (isArray(type)) {
                    result = handleNameType(name, stripArraySize(type));
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
        for (ViskitElement stateVariable : getStateVariablesList()) {
            String result;
            type = stateVariable.getType();
            name = stateVariable.getName();
            if (isArray(type)) {
                result = handleNameType(name, stripArraySize(type));
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
        for (ViskitElement par : getSimParametersList()) {
            String result;
            type = par.getType();
            name = par.getName();
            if (isArray(type)) {
                result = handleNameType(name, stripArraySize(type));
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
//                LOG.debug("Interpreter evaluation result: " + o);
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

    /** Checks for an array type
     *
     * @param type the type to check
     * @return true if an array type
     */
    public boolean isArray(String type) {
        return type.endsWith("]");
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
            LOG.error(returnString);
        }

        // good if remains null
        return returnString;
    }

    /** Checks for a generic type
     *
     * @param type the type to check
     * @return true if a generic type
     */
    public boolean isGeneric(String type) {
        return type.contains(">");
    }

    /** The entry point for Viskit startup. This method will either identify a
     * recorded project space, or launch a dialog asking the user to either
     * create a new project space, or open another existing one, or exit Viskit
     */
    public final void inititalizeProjectHome() {
        ViskitConfiguration vConfig = ViskitConfiguration.instance();
        String projectHome = vConfig.getVal(ViskitConfiguration.PROJECT_PATH_KEY);
        LOG.debug(projectHome);
        if (projectHome.isEmpty() || !(new File(projectHome).exists())) {
            ViskitStartupProjectInitializationPanel.showDialog(); // hopefully, void this
        } else {
            ViskitProject.VISKIT_PROJECTS_DIRECTORY = projectHome;
        }
    }

    private Object instantiateType(String type) throws Exception {
        Object o = null;
        boolean isArr = false;

        if (isArray(type)) {
            type = type.substring(0, type.length() - "[]".length());
            isArr = true;
        }
        try {
            Class<?> c = ViskitStatics.classForName(type);
            if (c != null) {

                Constructor<?>[] constructors = c.getConstructors();

                // The first constructor should be the default, no argument one
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterTypes().length == 0) {
                        if (isArr) {
                            o = Array.newInstance(c, 1);
                        } else {
                            o = c.getDeclaredConstructor().newInstance();
                        }
                    }
                }
            }
        } catch (SecurityException | NegativeArraySizeException | InstantiationException | IllegalAccessException e) {
            LOG.error(e);
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
            LOG.error(evalError);
//            evalError.printStackTrace();
        }
        return false;
    }

    /* Dynamic variable type list processing.  Build Type combo boxes and manage
     * user-typed object types.
     */
    private final String moreTypesString = "more...";
    private final String[] defaultTypeStrings = {
            "int",
            "double",
            "Integer",
            "Double",
            "String",
            moreTypesString};
    private final String[] morePackages = {"primitives", "java.lang", "java.util", "simkit.random", "cancel"};

    // these are for the moreClasses array
    private final int PRIMITIVES_INDEX = 0;

    private final String[][] moreClasses =
            {{"boolean", "byte", "char", "double", "float", "int", "long", "short"},
            {"Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Short", "String", "StringBuilder"},
            {"HashMap<K,V>", "HashSet<E>", "LinkedList<E>", "Properties", "Random", "TreeMap<K,V>", "TreeSet<E>", "Vector<E>"},
            {"RandomNumber", "RandomVariate"},
            {}
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
        if (v.isEmpty()) {
            return null;
        }
        return GenericConversion.toArray(v, new String[0]);
    }

    /**
     * This is messaged by dialogs and others when a user has selected a type
     * for a new variable.  We look around to see if we've already got it
     * covered.  If not, we add it to the end of the list.
     *
     * @param ty the type to evaluate
     * @return the String representation of this type if found
     */
    public String typeChosen(String ty) {
        ty = ty.replaceAll("\\s", "");              // every whitespace removed
        for (int i = 0; i < defaultComboBoxModel.getSize(); i++) {
            if (defaultComboBoxModel.getElementAt(i).equals(ty)) {
                return ty;
            }
        }
        // else, put it at the end, but before the "more"
        defaultComboBoxModel.insertElementAt(ty, defaultComboBoxModel.getSize() - 1);
        return ty;
    }

    public JComboBox<String> getTypeCB() {
        JComboBox<String> cb = new JComboBox<>(defaultComboBoxModel);
        cb.addActionListener(myTypeListener);
        cb.addItemListener(myTypeListener);
        cb.setRenderer(new myTypeListRenderer());
        cb.setEditable(true);
        return cb;
    }

    private void buildTypePopup() {

        popupMenu = new JPopupMenu();
        JMenu m;
        JMenuItem mi;

        for (int i = 0; i < morePackages.length; i++) {
            if (moreClasses[i].length <= 0) {           // if no classes, make the "package" selectable
                mi = new MyJMenuItem(morePackages[i], null);
                mi.addActionListener(myTypeListener);
                popupMenu.add(mi);
            } else {
                m = new JMenu(morePackages[i]);
                for (String item : moreClasses[i]) {
                    if (i == PRIMITIVES_INDEX) {
                        mi = new MyJMenuItem(item, item);
                    } // no package
                    else {
                        mi = new MyJMenuItem(item, morePackages[i] + "." + item);
                    }
                    mi.addActionListener(myTypeListener);
                    m.add(mi);
                }
                popupMenu.add(m);
            }
        }
    }
    JComboBox pending;
    Object lastSelected = "void";

    public SimulationRunPanel getSimulationRunPanel()
    {
        return simulationRunPanel;
    }

    public void setSimulationRunPanel(SimulationRunPanel simulationRunPanel) 
    {
        this.simulationRunPanel = simulationRunPanel;
    }

    /**
     * @return current ViskitProject */
    public ViskitProject getViskitProject() {
        return ViskitProject;
    }

    /**
     * @return whether a ViskitProject is currently loaded */
    public boolean hasViskitProject() {
        return (ViskitProject != null);
    }

    /**
     * @return a project's working directory which is typically the
     * build/classes directory
     */
    public File getWorkDirectory() {
        return workingDirectory;
    }

    /**
     * Not the best Java Bean convention, but performs as a no argument setter
     * for an open project's working directory (build/classes). Establishes the
     * class loader, project space, extra classpaths and identifies the path for
     * .class files of the projects Event Graphs
     */
    public final void createWorkingDirectory()
    {
        ViskitConfiguration vConfig = ViskitConfiguration.instance();
        if (vConfig.getViskitAppConfiguration() == null)
            return;

        String projectName = vConfig.getVal(ViskitConfiguration.PROJECT_NAME_KEY);
        if ((projectName != null) && (!projectName.isEmpty())) {
            ViskitProject.DEFAULT_PROJECT_NAME = projectName;
        }
        projectsBaseDirectory = new File(ViskitProject.VISKIT_PROJECTS_DIRECTORY);

        if (ViskitProject == null)
            ViskitProject = new ViskitProject(new File(projectsBaseDirectory, ViskitProject.DEFAULT_PROJECT_NAME));
        else
            ViskitProject.setProjectRoot(new File(projectsBaseDirectory, ViskitProject.DEFAULT_PROJECT_NAME));

        if (ViskitProject.initProject()) {
            SettingsDialog.saveExtraClasspathEntries(ViskitProject.getProjectAdditionalClasspaths()); // necessary to find and record extra classpaths
        } else {
            LOG.warn("Unable to create project directory, returning");
            return;
        }
        workingDirectory = ViskitProject.getClassesDirectory();
    }

    private ClassLoader workLoader, freshLoader;

    /**
     * Retrieve Viskit's working ClassLoader. It may be reset from time to
     * time if extra classpaths are loaded
     *
     * @return Viskit's working ClassLoader
     */
    public ClassLoader getWorkClassLoader() {
        if (workLoader == null) {
            URL[] urls = SettingsDialog.getExtraClassPathArraytoURLArray();
            LocalBootLoader loader = new LocalBootLoader(urls,
                    Thread.currentThread().getContextClassLoader(),
                    getWorkDirectory());

            // Allow Assembly files in the ClassLoader
            workLoader = loader.initialize(true);

            Thread.currentThread().setContextClassLoader(workLoader);
        }
        return workLoader;
    }

    public void resetWorkClassLoader() {
        workLoader = null;
    }

    /** This class loader is specific to Assembly running in that it is
     * pristine from the working class loader in use for normal Viskit operations
     *
     * @return a pristine class loader for Assembly runs
     */
    public ClassLoader getFreshClassLoader() {
        if (freshLoader == null) {

            // Forcing exposure of extra classpaths here.  Bugfix 1237
            URL[] urlArray = SettingsDialog.getExtraClassPathArraytoURLArray();

            /* Not sure if this breaks the "fresh" classloader for assembly
               running, but in post JDK8 land, the Java Platform Module System
               (JPMS) rules and as such we need to retain certain modules, i.e.
               java.sql. With retaining the boot class loader, not sure if that
               causes sibling classloader static variables to be retained. In
               any case we must retain the original bootloader as it has
               references to the loaded module system.
            */
            LocalBootLoader loader = new LocalBootLoader(urlArray,
                ClassLoader.getPlatformClassLoader(), // <- the parent of the platform loader should be the internal boot loader
                getWorkDirectory());

            // Allow Assembly files in the ClassLoader
            freshLoader = loader.initialize(true);

            // Set a fresh ClassLoader for this thread to be free of any static
            // state set from the Viskit working ClassLoader
            Thread.currentThread().setContextClassLoader(freshLoader);
        }
        return freshLoader;
    }

    public void resetFreshClassLoader() {
        freshLoader = null;
    }

    /** @return a model to print a stack trace of calling classes and their methods */
    @SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown", "ThrowableResultIgnored"})
    public String printCallerLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("Calling class: ").append(new Throwable().fillInStackTrace().getStackTrace()[4].getClassName());
        sb.append("\nCalling method: ").append(new Throwable().fillInStackTrace().getStackTrace()[4].getMethodName());
        return sb.toString();
    }

    /** All of this to avoid a SystemExit call explicitly */
    private SystemExitHandler systemExitHandler = (int status) -> {
        LOG.debug("Viskit is exiting with status {}: ", status);

        /* If an application launched a JVM, and is still running, this will
        * only make Viskit disappear. If Viskit is running standalone,
        * then then all JFrames created by Viskit will dispose, and the JVM
        * will then cease.
        * @see http://java.sun.com/docs/books/jvms/second_edition/html/Concepts.doc.html#19152
        * @see http://72.5.124.55/javase/6/docs/api/java/awt/doc-files/AWTThreadIssues.html
        */
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
            LOG.debug("Frame is {}: ", f);

            /* Prevent non-viskit created components from disposing if
             * launched from another application. SwingUtilities is a
             * little "ify" though as it's not Viskit specific. Viskit,
             * however, spawns a lot of anonymous Runnables with
             * SwingUtilities
             */
            if (f.toString().toLowerCase().contains("viskit")) {
                f.dispose();
            }
            if (f.toString().contains("SwingUtilities")) {
                f.dispose();
            }

            // Case for XMLTree JFrames
            if (f.getTitle().contains("xml")) {
                f.dispose();
            }
        }

        /* The SwingWorker Thread is active when the assembly runner is
         * running and will subsequently block a JVM exit due to its "wait"
         * state. Must interrupt it in order to cause the JVM to exit
         * @see docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
         */
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads) {
            LOG.debug("Thread before exit {}: ", t);

            // Attempt to release the URLClassLoader's file lock on open JARs
            t.setContextClassLoader(ClassLoader.getSystemClassLoader());
//            if (t.getName().contains("SwingWorker"))
//                t.interrupt(); // not working as expected
//            if (t.getName().contains("Image Fetcher"))
//                t.interrupt();
        }
    };

    public void setSystemExitHandler(SystemExitHandler handler) {
        systemExitHandler = handler;
    }

    public SystemExitHandler getSystemExitHandler() {
        return systemExitHandler;
    }

    /** Called to perform proper thread shutdown without calling System.exit(0)
     *
     * @param status the status of JVM shutdown
     */
    public void systemExit(int status) {
        if (!isSystemExitCalled()) {
            systemExitHandler.doSystemExit(status);
            setSystemExitCalled(true);
        }
    }

    public MainFrame getMainFrame() 
    {
        return mainFrame;
    }

    public void setMainFrame(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;
    }

    public Help getHelp() {
        return help;
    }

    /** The EventGraph Editor is the first to start so it will set the instance
     * of Help for Viskit
     * @param help the JavaHelp instance to set for Viskit
     */
    public void setHelp(Help help) {
        this.help = help;
    }

    public boolean isSystemExitCalled() {
        return systemExitCalled;
    }

    public void setSystemExitCalled(boolean systemExitCalled) {
        this.systemExitCalled = systemExitCalled;
    }

    /**
     * Small class to hold on to the fully-qualified class name, while displaying only the
     * un-qualified name;
     */
    @SuppressWarnings("serial")
    class MyJMenuItem extends JMenuItem {

        private final String fullName;

        MyJMenuItem(String nm, String fullName) {
            super(nm);
            this.fullName = fullName;
        }

        public String getFullName() {
            return fullName;
        }
    }

    class MyTypeListener implements ActionListener, ItemListener {

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
                final JComboBox cb = (JComboBox) o;
                pending = cb;
                if (cb.getSelectedItem().toString().equals(moreTypesString)) {

                    // NOTE: was getting an IllegalComponentStateException for component not showing
                    Runnable r = () -> {
                        if (cb.isShowing())
                            popupMenu.show(cb, 0, 0);
                    };

                    try {
                        SwingUtilities.invokeLater(r);
                    } catch (Exception ex) {
                        LOG.error(ex);
                    }
                }
            } else {
                MyJMenuItem mi = (MyJMenuItem) o;
                if (mi != null && !mi.getText().equals("cancel")) {
                    pending.setSelectedItem(mi.getFullName());
                }
                else {
                    pending.setSelectedItem(lastSelected);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    class myTypeListRenderer extends JLabel implements ListCellRenderer<String> {

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel lab = new JLabel(value);
            if (value.equals(moreTypesString)) {
                lab.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            return lab;
        }
    }
    
    /** test if string is numeric.  sheesh, why isn't this in Java already?
     * https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
     * @param string
     * @return whether numeric
     */
    public static boolean isNumeric(String string) {
        try {
            Double.valueOf(string);
            return true;
        } 
        catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @return the internalSimulationRunner
     */
    public TextAreaOutputStream getInternalSimulationRunner() {
        return internalSimulationRunner;
    }

    /**
     * @param internalSimulationRunner the internalSimulationRunner to set
     */
    public void setInternalSimulationRunner(TextAreaOutputStream internalSimulationRunner) {
        this.internalSimulationRunner = internalSimulationRunner;
    }

} // end class file ViskitGlobals.java
