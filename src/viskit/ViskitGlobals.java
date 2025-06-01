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
import viskit.control.InternalAssemblyRunner;
import viskit.doe.LocalBootLoader;
import viskit.model.AnalystReportModel;
import viskit.model.AssemblyModelImpl;
import viskit.model.EventNode;
import viskit.model.Model;
import viskit.model.ViskitElement;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.view.AnalystReportViewFrame;
import viskit.view.AssemblyViewFrame;
import viskit.view.EventGraphViewFrame;
import viskit.view.MainFrame;
import viskit.view.SimulationRunPanel;
import viskit.view.ViskitProjectSelectionPanel;
import viskit.view.dialog.ViskitProjectGenerationDialog3;
import viskit.view.dialog.ViskitUserPreferencesDialog;
import edu.nps.util.SystemExitHandler;
import org.apache.logging.log4j.LogManager;
import static viskit.ViskitProject.DEFAULT_PROJECT_NAME;

/**
 * ViskitGlobals contains global methods and variables.
 * @warning Not thread safe!
 * 
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 5, 2004
 * @since 3:20:33 PM
 * @version $Id$
 */
public class ViskitGlobals
{
    static final Logger LOG = LogManager.getLogger();
    
    /** Singleton pattern, with final version applied by NetBeans
     * @see https://stackoverflow.com/questions/2832297/java-singleton-pattern
     * @see https://stackoverflow.com/questions/70689/what-is-an-efficient-way-to-implement-a-singleton-pattern-in-java
     */
    // lazy loading, not immediate loading which can fail
    private static volatile ViskitGlobals INSTANCE = null;
   
    /** Private constructor prevents instantiation from other classes.
     *  Other initialization checks moved out of this constructor to avoid breaking singleton pattern.
     */
    private ViskitGlobals() 
    {
        // This should only occur once
        // Other initialization checks moved out of this constructor to avoid breaking singleton pattern
        // TODO does LOG interfere with singleton pattern?
        LOG.info("created ViskitGlobals singleton (if this message occurs again, it is a problem)"); // TODO threading issue?
    }
    
    public static ViskitGlobals instance()
    {
        ViskitGlobals INSTANCE = ViskitGlobals.INSTANCE;
        if (INSTANCE == null) { // Check 1
            synchronized (ViskitGlobals.class) {
                INSTANCE = ViskitGlobals.INSTANCE;
                if (INSTANCE == null) { // Check 2
                    ViskitGlobals.INSTANCE = INSTANCE = new ViskitGlobals();
                }
            }
        }
        if (INSTANCE == null)
            LOG.warn ("initial instance creation failed! check logs afterwards for synchronized singleton safety check");
        return INSTANCE;
        
//        if (INSTANCE == null) { // Check 1
//            synchronized (ViskitGlobals.class) {
//                if (INSTANCE == null) { // Check 2
//                    INSTANCE = new ViskitGlobals();
//                }
//            }
//        }
//        return INSTANCE;
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
    
    /* Topmost frame for the application */
    private MainFrame mainFrame;
    
    /** Top-level Viskit application running main() method */
    private ViskitApplication viskitApplication;

    /** Current Viskit project, only one can be active at a time */
    private ViskitProject viskitProject;

    /** Need hold of the Enable Analyst Reports checkbox and number of replications, likely others too */
    private SimulationRunPanel simulationRunPanel;
    
    private InternalAssemblyRunner internalAssemblyRunner;

    /** Flag to denote called systemExit only once */
    private boolean systemExitCalled = false;

    /** The current project name */
    private String projectName;

    /** The current project File */
    private File projectFile;

    /** The base directory for all projects */
    private File allProjectsBaseDirectory;

    /** The current project working directory */
    private File projectWorkingDirectory;
    
    /** The current project working directory for build classes */
    private File projectClassesDirectory;

    /** The main app JavaHelp set */
    private Help help;

    /* EventGraphViewFrame / EventGraphControllerImpl */

    EventGraphViewFrame      eventGraphViewFrame;
    EventGraphControllerImpl eventGraphController;
    
    /* Configuration constants and methods */

    /* routines to manage the singleton-aspect of the views. */
    private AssemblyViewFrame      assemblyViewFrame;
    private AssemblyControllerImpl assemblyController;

    private static final String BEAN_SHELL_ERROR = "BeanShell eval error";
    private Interpreter interpreter;
    private final DefaultComboBoxModel<String> defaultComboBoxModel = 
                                new DefaultComboBoxModel<>(new Vector<>(Arrays.asList(defaultTypeStrings)));
    private JPopupMenu           viskitTypePopupMenu;
    private MyViskitTypeListener myViskitTypeListener;

    /* routines to manage the singleton-aspect of the view */
    private AnalystReportViewFrame  analystReportViewFrame;
    private AnalystReportController analystReportController;
    
    /** this loader seems to provoke repeated ViskitGlobals singleton creation, and so 
     * putting it ViskitGlobals in attempt to avoid side effects */
//    private static ClassLoader localWorkingClassLoader; // TODO confirm deconfliction

    /**
     * Get a reference to the assembly editor view.
     * @return a reference to the assembly editor view or null if yet unbuilt.
     */
    public AssemblyViewFrame getAssemblyEditorViewFrame() {
        return (AssemblyViewFrame) assemblyViewFrame;
    }

    /**
     * Initialize GUI at startup
     * @return the component AssemblyViewFrame
     */
    public AssemblyViewFrame buildAssemblyViewFrame() 
    {
        assemblyController = new AssemblyControllerImpl();
        assemblyViewFrame  = new AssemblyViewFrame(assemblyController);
        assemblyController.setView(assemblyViewFrame);
        return assemblyViewFrame;
    }

    /** Rebuilds the Listener Event Graph Object (LEGO) panels on the Assembly Editor */
    public void rebuildLEGOTreePanels() {
        ((AssemblyViewFrame) assemblyViewFrame).rebuildLEGOTreePanels();
    }

    public AssemblyModelImpl getActiveAssemblyModel() 
    {
        if (assemblyController == null)
        {
            LOG.error("getActiveAssemblyModel() retrieval of assemblyController returned null object");
             return null;
        } 
        // getModel() is typlically checked by invoking code block
//        else if (assemblyController.getModel() == null)
//        {
//             LOG.error("getActiveAssemblyModel() assemblyController.getModel() returned null object");
//             return null;
//        } 
        else return (AssemblyModelImpl) assemblyController.getModel();
    }

    public String getActiveAssemblyName() 
    {
        if  (getActiveAssemblyModel() == null)
             return "";
        else return getActiveAssemblyModel().getName();
    }

    public AssemblyControllerImpl getActiveAssemblyController() {
        return assemblyController;
    }

    ActionListener defaultAssemblyQuitActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (assemblyViewFrame != null) {
                assemblyViewFrame.setVisible(false);
            }
        }
    };
    ActionListener assemblyQuitActionListener = defaultAssemblyQuitActionListener;

    public void quitAssemblyEditor() 
    {
        if (assemblyQuitActionListener != null) 
        {
            assemblyQuitActionListener.actionPerformed(new ActionEvent(this, 0, "quit Assembly Editor"));
        }
    }

    public void setAssemblyQuitActionListener(ActionListener newActionListener) 
    {
        assemblyQuitActionListener = newActionListener;
    }

    public EventGraphViewFrame getEventGraphViewFrame()
    {
        return (EventGraphViewFrame) eventGraphViewFrame;
    }

    /** This method starts the chain of various Viskit startup steps. 
     * By calling for a new EventGraphControllerImpl(), in its constructor is a
     * call to initConfig() which is the first time that the vConfig.xml is
     * looked for, or if one is not there, to create one from the template. The
     * vConfig.xml is an important file that holds information on recent
     * assembly and event graph openings, and caching of compiled source from
     * EventGraphs.
     *
     * @return an instance of the EventGraphViewFrame
     */
    public EventGraphViewFrame buildEventGraphViewFrame() 
    {
        eventGraphController = new EventGraphControllerImpl();
        eventGraphViewFrame  = new EventGraphViewFrame(eventGraphController);
        eventGraphController.setView(eventGraphViewFrame);
        return eventGraphViewFrame;
    }

    public EventGraphControllerImpl getEventGraphController() {
        return eventGraphController;
    }

    public Model getActiveEventGraphModel() {
        return (Model) eventGraphController.getModel();
    }
    
    ActionListener defaultEventGraphQuitActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (eventGraphViewFrame != null) {
                eventGraphViewFrame.setVisible(false);
            }
        }
    };
    ActionListener eventGraphQuitHandler = defaultEventGraphQuitActionListener;

    public void quitEventGraphEditor() {
        if (eventGraphQuitHandler != null) {
            eventGraphQuitHandler.actionPerformed(new ActionEvent(this, 0, "quit event graph editor"));
        }
    }

    public void setEventGraphQuitHandler(ActionListener actionListener) {
        eventGraphQuitHandler = actionListener;
    }

    public Vector<ViskitElement> getStateVariablesList() {
        return getActiveEventGraphModel().getStateVariables();
    }

    public ComboBoxModel<ViskitElement> getStateVariablesComboBoxModel() {
        return new DefaultComboBoxModel<>(getStateVariablesList());
    }

    public Vector<ViskitElement> getSimulationParametersList() {
        return getActiveEventGraphModel().getSimulationParameters();
    }

    public ComboBoxModel<ViskitElement> getSimulationParametersComboBoxModel() {
        return new DefaultComboBoxModel<>(getSimulationParametersList());
    }

    /* AnalystReport model / view / controller, now shifted to stronger class typing */

    /**
     * Get a reference to the analyst report view.
     * @return a reference to the analyst report view or null if yet unbuilt.
     */
    public AnalystReportViewFrame getAnalystReportViewFrame() {
        if    (analystReportViewFrame == null)
        {
               analystReportViewFrame = new AnalystReportViewFrame();
        }
        return analystReportViewFrame;
    }

    /**
     * Get a reference to the analyst report view.
     * @return a reference to the analyst report view or null if yet unbuilt.
     */
    public AnalystReportController getAnalystReportController() {
        if    (analystReportController == null)
        {
               analystReportController = new AnalystReportController();
        }
        return analystReportController;
    }

    /** Called from the EventGraphAssemblyComboMainFrame to initialize at UI startup
     *
     * @return the component AnalystReportViewFrame
     */
    public MvcAbstractViewFrame buildAnalystReportFrame()
    {
        analystReportController = new AnalystReportController();
        setAnalystReportViewFrame(new AnalystReportViewFrame());
//        getAnalystReportViewFrame().setAnalystReportController(analystReportController);
//        analystReportController.setView(getAnalystReportViewFrame());
        return getAnalystReportViewFrame();
    }

    /** @return the analyst report builder (model) */
    public AnalystReportModel getAnalystReportModel() {
        return (AnalystReportModel) analystReportController.getModel();
    }

//    /** @return the analyst report controller */
//    public MvcController getAnalystReportController() {
//        return analystReportController;
//    }

    /******
     * Beanshell code
     ******/
    private void initializeBeanShell()
    {
        if (interpreter == null) {
            interpreter = new Interpreter();
            interpreter.setStrictJava(true);       // no loose typing
        }

        String[] workCP = ViskitUserPreferencesDialog.getExtraClassPathArray();
        if (workCP != null && workCP.length > 0) {
            for (String path : workCP) {
                try {
                    interpreter.getClassManager().addClassPath(new URI("file", "localhost", path).toURL());
                } catch (IOException | URISyntaxException e) {
                    LOG.error("Working classpath component: " + path);
                }
            }
        }

        NameSpace nameSpace = interpreter.getNameSpace();
        nameSpace.importPackage("simkit.*");
        nameSpace.importPackage("simkit.random.*");
        nameSpace.importPackage("simkit.smdx.*");
        nameSpace.importPackage("simkit.stat.*");
        nameSpace.importPackage("simkit.util.*");
        nameSpace.importPackage("diskit.*");         // 17 Nov 2004
    }

    /** Use BeanShell for code parsing to detect potential errors
     *
     * @param eventNode the SimEntity node being evaluated
     * @param interpretString the code block to check
     * @return any indication of a parsing error.  A null means all is good.
     */
    public String parseCode(EventNode eventNode, String interpretString) {
        initializeBeanShell();
        // Load the interpreter with the state variables and the sim parameters
        // Load up any local variables and event parameters for this particular node
        // Then, parse.

        // Lose the new lines
//        String noCRs = interpretString.replace('\n', ' ');

        String name;
        String type;

        if (eventNode != null) {

            // Event local variables
            for (ViskitElement eventLocalVariable : eventNode.getLocalVariables()) 
            {
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
            for (ViskitElement viskitElement : eventNode.getArguments()) {
                type = viskitElement.getType();
                name = viskitElement.getName();
                String result = handleNameType(name, type);
                if (result != null) {
                    clearNamespace();
                    clearClassPath();
                    return BEAN_SHELL_ERROR + "\n" + result;
                }
            }
        }

        // state variables
        for (ViskitElement stateVariable : getStateVariablesList()) 
        {
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
        for (ViskitElement simParameter : getSimulationParametersList())
        {
            String result;
            type = simParameter.getType();
            name = simParameter.getName();
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
        if (!handlePrimitiveType(name, typ)) {
            returnString = findType(name, typ);
        }

        // good if remains null
        return returnString;
    }

    private String findType(String name, String type) {
        String returnString = null;
        try {
            if (isGenericType(type)) {
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

        } 
        catch (Exception ex) {
            LOG.error("findType(" + name + ", " + type + "} exception: " + ex.getMessage());
        }

        // good if remains null
        return returnString;
    }

    /** Checks for a generic type
     *
     * @param type the type to check
     * @return true if a generic type
     */
    public boolean isGenericType(String type) {
        return type.contains(">");
    }
    
    static ViskitProjectSelectionPanel viskitProjectSelectionPanel; // TODO replace

    /** The entry point for Viskit startup. This method will either identify a
     * recorded project space, or launch a dialog asking the user to either
     * create a new project space, or open another existing one, or exit Viskit
     */
    public final void initializeProjectHome() 
    {
        File newProjectFile;
        String pathPrefix  = ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.PROJECT_PATH_KEY);
        if (pathPrefix.startsWith("./"))
            pathPrefix = System.getProperty("user.dir").replaceAll("\\\\", "/") + pathPrefix.substring(1);
        String projectHome = pathPrefix + "/" +
                             ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.PROJECT_NAME_KEY); 
//      String projectHome = getProjectWorkingDirectory().getName();

//////        viskitProject.setProjectRootDirectory( new File(projectHome));

        if (  projectHome.isEmpty() || 
              projectHome.equals("/") || projectHome.equals("///") || projectHome.equals("\\\\") ||
            !(new File(projectHome).exists()) ||
             (hasViskitProject() && !isProjectOpen()))
        {
            if (!projectHome.isEmpty())
                 LOG.info("initializeProjectHome() projectHome=\n      " + projectHome);
            LOG.info("initializeProjectHome() did not find a previously existing project");
            
            if (ViskitGlobals.instance().hasMainFrameInitialized())
            {
                String popupTitle = "New Project or Open Project?";
                String message =
                        "<html><body>" +
                        "<p align='center'>Create a new Viskit project, or</p><br />" +
                        "<p align='center'>Open an existing Viskit project?</p><br />";
                
//                int returnValue = getMainFrame().genericAsk2Buttons(popupTitle, message, 
//                        "New Project", "Open Project");
                
                String buttonLabel1 = "New Project";
                String buttonLabel2 = "Open Project";
                int returnValue = JOptionPane.showOptionDialog(new JFrame(), message, popupTitle, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{buttonLabel1, buttonLabel2}, buttonLabel1);
        
                if  (returnValue == 0) // new project
                {    

                // What we wish to do here is force the user to create a new project space
                // before letting them move on, or, open and existing project, or the only
                // other option is to exit
                do {
                    ViskitProjectGenerationDialog3.showDialog();
                    if (ViskitProjectGenerationDialog3.cancelled)
                        return;

                    String newProjectPath = ViskitProjectGenerationDialog3.projectPath;
                    newProjectFile = new File(newProjectPath);
                    if (newProjectFile.exists() && (newProjectFile.isFile() || newProjectFile.list().length > 0))
                        JOptionPane.showMessageDialog(getEventGraphViewFrame().getRootPane(), 
                                "Chosen project name already exists, please create a new project name.");
                    else
                        break; // out of do-while

                } while (true); // loop until break

//                File projectDirectory = ViskitProject.openProjectDirectory(ViskitGlobals.instance().getEventGraphViewFrame().getRootPane(),".");
                }
                else // open project
                {
                    // might occur during initial startup before appliaction frame is initialized
                    JComponent floatingComponent;
                    if (getEventGraphViewFrame() != null)
                         floatingComponent = getEventGraphViewFrame().getRootPane(); //
                    else floatingComponent = new JPanel();
                    File projectDirectory = ViskitProject.openProjectDirectory(floatingComponent,".");
                    // TODO untested
                    newProjectFile = new File (projectDirectory, ViskitProject.PROJECT_FILE_NAME);
                    setProjectFile(newProjectFile);
                }
            
////            // no project open, popup special dialog
////            if (viskitProjectSelectionPanel == null)
////            {
////                viskitProjectSelectionPanel = new ViskitProjectSelectionPanel(); // should only occur once
////            }
////            viskitProjectSelectionPanel.showDialog(); // blocks

///////            }
////            else
////            {
////                ViskitProject.VISKIT_PROJECTS_DIRECTORY = projectHome;
////                // TODO untested
////////                newProjectFile = new File (projectHome + "/" + ViskitProject.PROJECT_FILE_NAME);
////            setProjectFile(newProjectFile);
////            LOG.info("initializeProjectHome() newProjectFile=\n      " + newProjectFile.getAbsolutePath());
            }
        }
        else 
        {
            LOG.info("initializeProjectHome() found a previously existing project:\n      {}", projectHome); // Now createProjectWorkingDirectory()..."); // debug
            createProjectWorkingDirectory(); // TODO needed? maybe yes for first time, but not if repeating...
        }
    }

    private Object instantiateType(String type) throws Exception {
        Object o = null;
        boolean isArray = false;

        if (isArray(type)) {
            type = type.substring(0, type.length() - "[]".length());
            isArray = true;
        }
        try {
            Class<?> c = ViskitStatics.classForName(type);
            if (c != null) {

                Constructor<?>[] constructors = c.getConstructors();

                // The first constructor should be the default, no argument one
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterTypes().length == 0) {
                        if (isArray) {
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

    private boolean handlePrimitiveType(String name, String type) {
        try {
            if (type.equals("int")) {
                interpreter.eval("int " + name + " = 0");
                return true;
            }
            if (type.equals("int[]")) {
                interpreter.eval("int[] " + name + " = new int[1]");
                return true;
            }
            if (type.equals("boolean")) {
                interpreter.eval("boolean " + name + " = false");  // 17Aug04, should have always defaulted to false
                return true;
            }
            if (type.equals("boolean[]")) {
                interpreter.eval("boolean[] " + name + " = new boolean[1]");
                return true;
            }
            if (type.equals("double")) {
                interpreter.eval("double " + name + " = 0.0d");
                return true;
            }
            if (type.equals("double[]")) {
                interpreter.eval("double[] " + name + " = new double[1]");
                return true;
            }
            if (type.equals("float")) {
                interpreter.eval("float " + name + " = 0.0f");
                return true;
            }
            if (type.equals("float[]")) {
                interpreter.eval("float[] " + name + " = new float[1]");
                return true;
            }
            if (type.equals("byte")) {
                interpreter.eval("byte " + name + " = 0");
                return true;
            }
            if (type.equals("byte[]")) {
                interpreter.eval("byte[] " + name + " = new byte[1]");
                return true;
            }
            if (type.equals("char")) {
                interpreter.eval("char " + name + " = '0'");
                return true;
            }
            if (type.equals("char[]")) {
                interpreter.eval("char[] " + name + " = new char[1]");
                return true;
            }
            if (type.equals("short")) {
                interpreter.eval("short " + name + " = 0");
                return true;
            }
            if (type.equals("short[]")) {
                interpreter.eval("short[] " + name + " = new short[1]");
                return true;
            }
            if (type.equals("long")) {
                interpreter.eval("long " + name + " = 0");
                return true;
            }
            if (type.equals("long[]")) {
                interpreter.eval("long[] " + name + " = new long[1]");
                return true;
            }
        } catch (EvalError evalError) {
            LOG.error(evalError);
//            evalError.printStackTrace();
        }
        return false;
    }

    /** @param viskitType the type to check if primitive or array
     * @return true if primitive or array
     */
    public boolean isPrimitiveOrPrimitiveArray(String viskitType) {
        int idx;
        if ((idx = viskitType.indexOf('[')) != -1) {
            viskitType = viskitType.substring(0, idx);
        }
        return isPrimitive(viskitType);
    }

    /** @param viskitType the type to check if primitive type
     * @return true if primitive type
     */
    public boolean isPrimitive(String viskitType) 
    {
        for (String s : moreClasses[PRIMITIVES_INDEX]) {
            if (viskitType.equals(s)) {
                return true;
            }
        }
        return false;
    }

    Pattern bracketsPattern = Pattern.compile("\\[.*?\\]");
    Pattern   spacesPattern = Pattern.compile("\\s");

    public String stripArraySize(String viskitType) {
        Matcher m = bracketsPattern.matcher(viskitType);
        String r = m.replaceAll("[]");            // [blah] with[]
        m = spacesPattern.matcher(r);
        return m.replaceAll("");
    }

    public String[] getArraySize(String viskitType) {
        Vector<String> v = new Vector<>();
        Matcher m = bracketsPattern.matcher(viskitType);

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
     * @param viskitType the type to evaluate
     * @return the String representation of this type if found
     */
    public String typeChosen(String viskitType) {
        viskitType = viskitType.replaceAll("\\s", "");              // every whitespace removed
        for (int i = 0; i < defaultComboBoxModel.getSize(); i++) 
        {
            if (defaultComboBoxModel.getElementAt(i).equals(viskitType)) {
                return viskitType;
            }
        }
        // else, put it at the end, but before the "more"
        defaultComboBoxModel.insertElementAt(viskitType, defaultComboBoxModel.getSize() - 1);
        return viskitType;
    }

    public JComboBox<String> getViskitTypeComboBox() 
    {
        if (myViskitTypeListener == null)  
            myViskitTypeListener = new MyViskitTypeListener();
        
        JComboBox<String> comboBox = new JComboBox<>(defaultComboBoxModel);
        comboBox.addActionListener(myViskitTypeListener);
        comboBox.addItemListener(myViskitTypeListener);
        comboBox.setRenderer(new myViskitTypeListRenderer());
        comboBox.setEditable(true);
        return comboBox;
    }

    private void buildViskitTypePopupMenu()
    {
        viskitTypePopupMenu = new JPopupMenu();
        JMenu menu;
        JMenuItem menuItem;

        for (int i = 0; i < morePackages.length; i++) 
        {
            if (moreClasses[i].length <= 0) {           // if no classes, make the "package" selectable
                menuItem = new MyJMenuItem(morePackages[i], null);
                menuItem.addActionListener(myViskitTypeListener);
                viskitTypePopupMenu.add(menuItem);
            } 
            else {
                menu = new JMenu(morePackages[i]);
                for (String item : moreClasses[i]) {
                    if (i == PRIMITIVES_INDEX) {
                        menuItem = new MyJMenuItem(item, item);
                    } // no package
                    else {
                        menuItem = new MyJMenuItem(item, morePackages[i] + "." + item);
                    }
                    menuItem.addActionListener(myViskitTypeListener);
                    menu.add(menuItem);
                }
                viskitTypePopupMenu.add(menu);
            }
        }
    }
    JComboBox pendingComboBox;
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
     * @return current viskitProject */
    public ViskitProject getViskitProject() {
        return viskitProject;
    }
    
    /**
     * @return current viskitProject root directory path */
    public String getProjectRootDirectoryPath()
    {
        return getViskitProject().getProjectDirectoryPath();
    }

    // duplicative but tricky, watch out!
    /**
     * @return whether a viskitProject is currently loaded */
    public boolean hasViskitProject() {
        // TODO further checks?
        return (getViskitProject() != null);
    }

    /**
     * @return a project's working directory which is typically the
     * build/classes directory (NOT the project directory)
     */
    public File getProjectWorkingDirectory() {
        return projectWorkingDirectory;
    }

    /**
     * Establishes the class loader, project space, extra classpaths
     * and identifies the path for class files of the project's Event Graphs
     * and Assemblies.  Not the best Java Bean convention, but performs as a 
     * no-argument setter for an open project's working directory (build/classes).
     */
    public final void createProjectWorkingDirectory()
    {
        if (ViskitUserConfiguration.instance().getViskitAppConfiguration() == null)
            return; // unexpected condition

        if ((projectName == null) || projectName.isBlank())
        {
            if  (!ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.PROJECT_NAME_KEY).isBlank())
                 projectName = ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.PROJECT_NAME_KEY);
            else projectName = DEFAULT_PROJECT_NAME;
        }
        setProjectName(projectName);
        
// TODO not clear that the next lines are correct.  
// why isn't this returning if already complete?
// why is this invoked after a simulation run??
        if (allProjectsBaseDirectory == null)
        {
            // TODO offer selection as user preference??
            // preferred: use this line if default initializes to provided default project
            allProjectsBaseDirectory = new File(System.getProperty("user.dir")); // full path is preferred to "."
            
            // use this line if default initializes to user store
            // allProjectsBaseDirectory = ViskitUserConfiguration.VISKIT_CONFIGURATION_DIR;
            // allProjectsBaseDirectory.mkdir();
            
            // trust but verify
            if (!allProjectsBaseDirectory.isDirectory())
            {
                 LOG.error("createProjectWorkingDirectory() check: allProjectsBaseDirectory is not a directory\n      " + allProjectsBaseDirectory.getAbsolutePath());
            }
            else LOG.info("createProjectWorkingDirectory() check: allProjectsBaseDirectory=\n      " + allProjectsBaseDirectory.getAbsolutePath());
        }
        
        if (projectWorkingDirectory == null)
        {
            // two-step process, create each directory individually
            projectWorkingDirectory = new File(allProjectsBaseDirectory,
                                                ViskitProject.DEFAULT_VISKIT_PROJECTS_DIRECTORY_NAME );
            projectWorkingDirectory.mkdir();
            projectWorkingDirectory = new File(projectWorkingDirectory, 
                                                projectName);
            projectWorkingDirectory.mkdir();
            // trust but verify
            if (!projectWorkingDirectory.isDirectory())
            {
                LOG.error("createProjectWorkingDirectory() projectWorkingDirectory is not a directory");
            }
            else LOG.info("createProjectWorkingDirectory() projectWorkingDirectory=\n      " + projectWorkingDirectory.getAbsolutePath());
        }

        if (getViskitProject() == null)
            setViskitProject(new ViskitProject(projectWorkingDirectory));
        else
        {
//            // (TODO, questionable) unexpected error condition; looping?
//            LOG.error("TODO questionable invocation follows...");
//            viskitProject.setProjectDirectory(new File(allProjectsBaseDirectory, ViskitProject.DEFAULT_PROJECT_NAME));
        }

        if (getViskitProject().initializeProject()) 
        {
            ViskitUserPreferencesDialog.saveExtraClasspathEntries(getViskitProject().getProjectAdditionalClasspaths()); // necessary to find and record extra classpaths
        } 
        else {
            LOG.error("Unable to create project directory for " + allProjectsBaseDirectory);
            return;
        }
        setProjectClassesDirectory(getViskitProject().getClassesDirectory());
    }

    private ClassLoader viskitApplicationClassLoader;

    /**
     * Retrieve the primary Viskit workingClassLoader, which may be reset from time to
     * time if extra classpaths are loaded.  
     * This is used prior to running an assembly in a simulation run, and again 
     * becomes active after all replications are complete when progressing to the Analyst Report.
     * @see BasicAssembly.getRunSimulationClassLoader()
     * @return Viskit's working ClassLoader
     */
    public ClassLoader getViskitApplicationClassLoader()
    {
            if (viskitApplicationClassLoader == null) // workingClassLoader should only get created once
            {
                URL[] urls = ViskitUserPreferencesDialog.getExtraClassPathArraytoURLArray();
                viskit.doe.LocalBootLoader localBootLoader = new LocalBootLoader(
                        urls,
                        Thread.currentThread().getContextClassLoader(),
                        getProjectWorkingDirectory(),
                        "ViskitApplicationBootLoader");

                // Allow Assembly files in the ClassLoader
                viskitApplicationClassLoader = localBootLoader.initialize(true);

                // TODO experimenting with context
                Thread.currentThread().setContextClassLoader(viskitApplicationClassLoader);
                LOG.debug("getViskitApplicationClassLoader() currentThread\n      contextClassLoader='" + viskitApplicationClassLoader.getName() + "'" + "\n");
            }
        if (viskitApplicationClassLoader == null)
        {
            LOG.error("getWorkingClassLoader() ran without exception but returned null");
        }
        return viskitApplicationClassLoader;
    }

    public void resetWorkingClassLoader() {
        viskitApplicationClassLoader = null;
        LOG.debug("resetWorkingClassLoader() complete"); // TODO threading issue?
    }


    /** @return a model to print a stack trace of calling classes and their methods */
    @SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown", "ThrowableResultIgnored"})
    public String printCallerLog() 
    {
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
        Thread[] runningThreadArray = new Thread[Thread.activeCount()];
        int runningThreadCount = runningThreadArray.length;
        Thread.enumerate(runningThreadArray);
        for (Thread thread : runningThreadArray) {
            LOG.debug("Thread before exit {}: ", thread);

            // Attempt to release the URLClassLoader's file lock on open JARs
            thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
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
     * @param status the status code for application shutdown
     */
    public void systemExit(int status) 
    {
        if (!isSystemExitCalled()) 
        {
            systemExitHandler.doSystemExit(status);
            setSystemExitCalled(true); // avoid repeated calls
        }
    }

    public boolean hasMainFrameInitialized()
    {
        return (mainFrame != null);
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
    class MyJMenuItem extends JMenuItem
    {
        private final String fullName;

        MyJMenuItem(String shortName, String fullName) {
            super(shortName);
            this.fullName = fullName;
        }
        public String getFullName() {
            return fullName;
        }
    }

    class MyViskitTypeListener implements ActionListener, ItemListener
    {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                lastSelected = e.getItem();
            }
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Object o = actionEvent.getSource();
        
            if (viskitTypePopupMenu == null)  
                buildViskitTypePopupMenu();
            if (o instanceof JComboBox) {
                final JComboBox comboBox = (JComboBox) o;
                pendingComboBox = comboBox;
                if (comboBox.getSelectedItem().toString().equals(moreTypesString)) {

                    // NOTE: was getting an IllegalComponentStateException for component not showing
                    Runnable r = () -> {
                        if (comboBox.isShowing())
                            viskitTypePopupMenu.show(comboBox, 0, 0);
                    };

                    try {
                        SwingUtilities.invokeLater(r);
                    } 
                    catch (Exception ex) {
                        LOG.error("actionPerformed(" + actionEvent.toString() + ") exception: " + ex.getMessage());
                    }
                }
            } 
            else {
                MyJMenuItem mi = (MyJMenuItem) o;
                if (mi != null && !mi.getText().equals("cancel")) {
                    pendingComboBox.setSelectedItem(mi.getFullName());
                }
                else {
                    pendingComboBox.setSelectedItem(lastSelected);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    class myViskitTypeListRenderer extends JLabel implements ListCellRenderer<String> 
    {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel valueLabel = new JLabel(value);
            if (value.equals(moreTypesString)) {
                valueLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }
            return valueLabel;
        }
    }
    
    /** test if string is numeric.  sheesh, why isn't this in Java already?
     * https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
     * @param string
     * @return whether numeric
     */
    public static boolean isNumeric(String string)
    {
        try {
            Double.valueOf(string);
            return true;
        } 
        catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @return the simulationTextAreaOutputStream
     */
    public InternalAssemblyRunner getInternalSimulationRunner() {
        return internalAssemblyRunner;
    }

    /**
     * @param newInternalAssemblyRunner the simulationTextAreaOutputStream to set
     */
    public void setInternalSimulationRunner(InternalAssemblyRunner newInternalAssemblyRunner) {
        this.internalAssemblyRunner = newInternalAssemblyRunner;
    }

    /** 
     * Utility method to configure a Viskit project.
     * (moved here from ViskitStatics).
     * @param newProjectFile the base directory of a Viskit project
     */
//  @SuppressWarnings("unchecked")
    public void setProjectFile(File newProjectFile) // TODO rename File -> Directory
    {
        if (newProjectFile == null)
        {
            LOG.error("setProjectFile() received a null file, ignored...");
            return;
        }
        // newProjectName must be a directory
        String newProjectName = newProjectFile.getName();
        if (newProjectFile.isDirectory())
            setProjectName(newProjectName);
        else
        {
            newProjectName = newProjectFile.getParentFile().getName();
            setProjectName(newProjectName);
        }
        ViskitProject.VISKIT_PROJECTS_DIRECTORY = newProjectFile.getParentFile().getAbsolutePath().replaceAll("\\\\", "/"); // de-windows
        ViskitUserConfiguration.instance().setValue(ViskitUserConfiguration.PROJECT_PATH_KEY, ViskitProject.VISKIT_PROJECTS_DIRECTORY);
        ViskitUserConfiguration.instance().setValue(ViskitUserConfiguration.PROJECT_NAME_KEY, newProjectName);
    }

    /**
     * @return current projectFile
     */
    public File getProjectFile() 
    {
        return projectFile;
    }

    /**
     * @return current projectName
     */
    public String getProjectName() {
        return projectName;
    }
    
    /** Utility method calling MainFrame
     * @param newProjectName */
    public void setTitleProjectName (String newProjectName)
    {
        mainFrame.setTitleProjectName(newProjectName);
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        if (projectName.endsWith(ViskitProject.PROJECT_FILE_NAME))
            LOG.error("setProjectName(" + projectName + ") must be a directory name, not a file name");
    }

    /**
     * @param analystReportController the analystReportController to set
     */
    public void setAnalystReportController(AnalystReportController analystReportController) {
        this.analystReportController = analystReportController;
    }

    /**
     * @param analystReportViewFrame the analystReportViewFrame to set
     */
    public void setAnalystReportViewFrame(AnalystReportViewFrame analystReportViewFrame) {
        this.analystReportViewFrame = analystReportViewFrame;
    }
    public void selectEventGraphEditorTab()
    {
        mainFrame.selectEventGraphEditorTab();
    }
    public void selectAssemblyEditorTab()
    {
        mainFrame.selectAssemblyEditorTab();
    }
    public void selectSimulationRunTab()
    {
        mainFrame.selectSimulationRunTab();
    }
    public void selectAnalystReportTab()
    {
        mainFrame.selectAnalystReportTab();
    }
    public boolean isSelectedEventGraphEditorTab()
    {
        return (ViskitGlobals.instance().getMainFrame().getTopTabbedPaneSelectedIndex() == MainFrame.TAB_INDEX_EVENTGRAPH_EDITOR);
    }
    public boolean isSelectedAssemblyEditorTab()
    {
        return (ViskitGlobals.instance().getMainFrame().getTopTabbedPaneSelectedIndex() == MainFrame.TAB_INDEX_ASSEMBLY_EDITOR);
    }
    public boolean isSelecteSimulationRunTab()
    {
        return (ViskitGlobals.instance().getMainFrame().getTopTabbedPaneSelectedIndex() == MainFrame.TAB_INDEX_SIMULATION_RUN);
    }
    public boolean isSelectedAnalystReportTab()
    {
        return (ViskitGlobals.instance().getMainFrame().getTopTabbedPaneSelectedIndex() == MainFrame.TAB_INDEX_ANALYST_REPORT);
    }
    
    public boolean isProjectOpen()
    {
        if (getViskitProject() == null)
             return false;
        else return getViskitProject().isProjectOpen();
    }

    /**
     * @param viskitApplication the viskitApplication to set
     */
    public void setViskitApplication(ViskitApplication viskitApplication) {
        this.viskitApplication = viskitApplication;
    }

    /**
     * @return the viskitApplication
     */
    public ViskitApplication getViskitApplication() {
        return viskitApplication;
    }

//////    /**
//////     * @return the localWorkingClassLoader
//////     */
//////    public static ClassLoader getLocalWorkingClassLoader() {
//////        return localWorkingClassLoader;
//////    }
//////
//////    /**
//////     * @param newLocalWorkingClassLoader the localWorkingClassLoader to set
//////     */
//////    public static void setLocalWorkingClassLoader(ClassLoader newLocalWorkingClassLoader) {
//////        localWorkingClassLoader = newLocalWorkingClassLoader;
//////    }
    
// move to ViskitStatics
//////    /** Check whether file and contents exist, ready for further work
//////     * @param file to check
//////     * @return whether ready
//////     */
//////    public static boolean isFileReady (File file)
//////    {
//////        if (file == null)
//////        {
//////            LOG.error("isFileReady() file reference is null");
//////            return false;
//////        }
//////        else if (!file.exists())
//////        {
//////            LOG.error("isFileReady() file does not exist:\n      " + file.getAbsolutePath());
//////            return false;
//////        }
//////        else if (file.length() == 0)
//////        {
//////            LOG.error("isFileReady() file is empty:\n      " + file.getAbsolutePath());
//////            return false;
//////        }
//////        return true;
//////    }

    /**
     * @return the projectClassesDirectory
     */
    public File getProjectClassesDirectory() {
        return projectClassesDirectory;
    }

    /**
     * @param projectClassesDirectory the projectClassesDirectory to set
     */
    public void setProjectClassesDirectory(File projectClassesDirectory) {
        this.projectClassesDirectory = projectClassesDirectory;
    }

    /** 
     * A component, e.g., vAMod, wants to say something.
     * @param messageType the type of message, i.e. WARN, ERROR, INFO, QUESTION, etc.
     * @param messageTitle the title of the message in the dialog frame
     * @param messageBody the message to transmit
     */
    public void messageUser(int messageType, String messageTitle, String messageBody) // messageType is one of JOptionPane types
    {
        if (ViskitGlobals.instance().getAssemblyEditorViewFrame() != null)
            ViskitGlobals.instance().getMainFrame().genericReport(messageType, messageTitle, messageBody);
    }

    /**
     * @param viskitProject the viskitProject to set
     */
    public void setViskitProject(ViskitProject viskitProject) {
        this.viskitProject = viskitProject;
    }

} // end class file ViskitGlobals.java
