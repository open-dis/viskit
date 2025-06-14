package viskit.control;

import actions.ActionIntrospector;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.TempFileManager;
import edu.nps.util.ZipUtilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jgraph.graph.DefaultGraphCell;

import viskit.util.EventGraphCache;
import viskit.util.FileBasedAssemblyNode;
import viskit.util.OpenAssembly;
import viskit.ViskitGlobals;
import viskit.ViskitUserConfiguration;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import static viskit.ViskitUserConfiguration.VISKIT_ERROR_LOG;
import viskit.jgraph.ViskitGraphUndoManager;
import viskit.model.*;
import viskit.mvc.MvcAbstractController;
import viskit.util.Compiler;
import viskit.util.XMLValidationTool;
import viskit.view.dialog.AssemblyMetadataDialog;
import viskit.view.AssemblyViewFrame;
import viskit.view.AssemblyView;
import viskit.view.dialog.ViskitUserPreferencesDialog;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;
import static viskit.view.MainFrame.TAB1_LOCALRUN_INDEX;
import viskit.assembly.SimulationRunInterface;
import viskit.control.InternalAssemblyRunner.SimulationState;
import viskit.view.MainFrame;
import viskit.view.SimulationRunPanel;
import static viskit.ViskitStatics.NO_DESCRIPTION_PROVIDED_HTML;

/**
 * AssemblyController full implementation.
 * 
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:26:02 AM
 * @version $Id: AssemblyControllerImpl.java 2884 2015-09-02 17:21:52Z tdnorbra $
 */
public class AssemblyControllerImpl extends MvcAbstractController implements AssemblyController, OpenAssembly.AssemblyChangeListener 
{
    static final Logger LOG = LogManager.getLogger();
    
    private static int mutex = 0;
    Class<?> simEventSourceClass, simEventListenerClass, propertyChangeSourceClass, propertyChangeListenerClass;

    /** The path to an assembly file if given from the command line */
    private String initialAssemblyFilePath;
    
    private static ViskitProject viskitProject;

    /** The handler to run an assembly */
    private SimulationRunInterface runnerSimulationRunInterface;
    
    JTabbedPane mainTabbedPane;
    int mainTabbedPaneIndex;
    
    private boolean localModified = false;
    
    private AssemblyViewFrame assemblyViewFrame;
    
    private AssemblyModelImpl assemblyModel;

    /** Constructor that creates a new instance of AssemblyController */
    public AssemblyControllerImpl() 
    {
        initializeConfiguration();
    }

    /**
     * Sets an initial assembly file to open upon Viskit startup supplied by the
     * command line
     * @param assemblyFilePath the assembly file to initially open upon startup
     */
    public void setInitialAssemblyFile(String assemblyFilePath) {
        if (viskit.ViskitStatics.debug) {
            LOG.info("setInitialAssemblyFile: {}", assemblyFilePath);
        }
        initialAssemblyFilePath = assemblyFilePath;
    }

    /** 
     *This method is for Assembly compilation
     * @param assemblyPath an assembly file to compile
     */
    private void compileAssembly(String assemblyPath) 
    {
        LOG.debug("Compiling assembly: {}", assemblyPath);
        File assemblyFile = new File(assemblyPath);
        _doOpen(assemblyFile);
        prepareSimulationRunner();
    }

    @Override
    public void begin() 
    {
        if (viskitProject == null)
            viskitProject = ViskitGlobals.instance().getViskitProject();
        File projectDirectory =  viskitProject.getProjectDirectory();

        // The initialAssemblyFilePath is set if we have stated a file "arg" upon startup from the command line

        if (initialAssemblyFilePath != null && !initialAssemblyFilePath.isBlank() && 
           !initialAssemblyFilePath.contains("$")) // Check for $ makes sure that a property key isn't being used
        {
            // Switch to the project that this Assembly file is located in if paths do not coincide
            if (!initialAssemblyFilePath.contains(projectDirectory.getPath())) 
            {
                LOG.info("Switch to the project that this Assembly file is located");
                doProjectCleanup();
                projectDirectory = new File(initialAssemblyFilePath).getParentFile().getParentFile().getParentFile();
                openProject(projectDirectory); // calls EventGraphViewFrame setTitleProjectName

                // Add a new project EventGraphs for LEGO tree inclusion of our SimEntities
                ViskitUserPreferencesDialog.RebuildLEGOTreePanelTask rebuildLEGOTreePanelTask = new ViskitUserPreferencesDialog.RebuildLEGOTreePanelTask();
                rebuildLEGOTreePanelTask.execute();
            }
            LOG.info("\n=============================");
            LOG.info("Loading initial assembly: {}", initialAssemblyFilePath);
            
            compileAssembly(initialAssemblyFilePath);
        } 
        else 
        {
            openProject(projectDirectory); // calls AssemblyControllerImpl setTitleProjectName
            List<String> openAssemblyFileList = getOpenAssemblyFileList(false);
            LOG.debug("Inside begin() and openAssemblyFileList.size() is: {}", openAssemblyFileList.size());
            File openAssemblyFile;
            for (String openAssemblyFilePath : openAssemblyFileList) 
            {
                openAssemblyFile = new File(openAssemblyFilePath);
                // Prevent project mismatch
                if (openAssemblyFile.exists())
                    _doOpen(openAssemblyFile);
            }
        }
//      (ViskitGlobals.instance().getAssemblyEditorViewFrame()).setTitleProjectName(); // unneeded
        recordProjectFiles();
    }

    /** Information required by the EventGraphControllerImpl to see if an Assembly
     * file is already open. Also checked internally by this class.
     * @param refresh flag to refresh the list from viskitConfig.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    public final List<String> getOpenAssemblyFileList(boolean refresh) {
        if (refresh || openAssembliesList == null)
            recordAssemblyFiles();

        return openAssembliesList;
    }

    /** Whether one or more assemblies are open
     * @return whether any assemblies are open
     */
    public final boolean isAssemblyOpen()
    {  
//      return (!getOpenAssemblyFileList(true).isEmpty()); // TODO not working
        return ViskitGlobals.instance().isAssemblyOpen();
    }

    private boolean checkSaveIfModified()
    {
        if (assemblyModel == null)
            assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        if (assemblyModel.isModelModified()) 
        {
            StringBuilder sb = new StringBuilder("<html><p align='center'>Execution parameters have been modified.<br>(");

            for (Iterator<OpenAssembly.AssemblyChangeListener> openAssemblyChangeListenerIterator = isLocalModifiedSet.iterator(); openAssemblyChangeListenerIterator.hasNext();) {
                sb.append(openAssemblyChangeListenerIterator.next().getHandle());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // last comma-space
            sb.append(")<br>Choose yes if you want to stop this operation, then manually select<br>the indicated tab(s) to ");
            sb.append("save the execution parameters.");

            int userResponse = (ViskitGlobals.instance().getMainFrame().genericAsk2Buttons("Question", sb.toString(),
                    "Stop and let me save",
                    "Ignore execution parameter changes"));
            // n == -1 if dialog was just closed
            //   ==  0 for first option
            //   ==  1 for second option

            // be conservative, stop for first 2 choices
            if (userResponse != 1) {
                return false;
            }
        }
        boolean returnValue = true;
        
        if (assemblyModel.isModelModified())
        {
            return askToSaveAndContinue(); // blocks
        }
        else return returnValue;  // proceed
    }
    
    /** method name for reflection use */
    public static final String METHOD_showViskitUserPreferences = "showViskitUserPreferences";

    @Override
    public void showViskitUserPreferences()
    {
        boolean modified; // TODO can we do anything with this notification?
        modified = ViskitUserPreferencesDialog.showDialog(ViskitGlobals.instance().getMainFrame());
    }
    
    /** method name for reflection use */
    public static final String METHOD_open = "open";

    @Override
    public void open()
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        
        // TODO fix: avoid problems when multiple assemblies are open
        if (isAssemblyOpen())
        {
            String title = "Close Current Assembly?";
            String assemblyName = ViskitGlobals.instance().getActiveAssemblyName();
            String message = "<html><p align='center'>Are you sure that you want to first close</p><br/>";
            message += "<p align='center'><i>"         + assemblyName + "</i></p><br/><p align='center'> as the currently active Assembly?</p><br/>";
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            if (returnValue == JOptionPane.NO_OPTION)
            {
                return;
            }
            else closeAll();
        }
        
        File[] filesArray = ViskitGlobals.instance().getAssemblyEditorViewFrame().openFilesAsk();
        if (filesArray == null)
            return; // no matching event graphs for this assembly to open

        for (File file : filesArray)
        {
            if (file != null)
                _doOpen(file);
        }
    }

    private void _doOpen(File file) 
    {
        if (!file.exists())
            return;
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        assemblyModel = new AssemblyModelImpl(this);
        assemblyModel.initialize();
        assemblyViewFrame.addTab(assemblyModel);

        // these may initialize to null on startup, check
        // before doing any openAlready lookups
        AssemblyModel[] assemblyModelOpenAlreadyArray = null;
        if (assemblyViewFrame != null)
            assemblyModelOpenAlreadyArray = assemblyViewFrame.getOpenAssemblyModels();

        boolean isOpenAlready = false;
        String path;
        if (assemblyModelOpenAlreadyArray != null) 
        {
            for (AssemblyModel assemblyModleOpenAlready : assemblyModelOpenAlreadyArray) {
                if (assemblyModleOpenAlready.getCurrentFile() != null) {
                    path = assemblyModleOpenAlready.getCurrentFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())){
                        isOpenAlready = true;
                        break;
                    }
                }
            }
        }
        if (assemblyModel.newAssemblyModel(file) && !isOpenAlready) 
        {
            assemblyViewFrame.setSelectedAssemblyName(assemblyModel.getMetadata().name);
            // TODO: Implement an Assembly description block set here

            adjustRecentAssemblySet(file);
            markAssemblyFilesOpened();

            // replaces old fileWatchOpen(file);
            initializeOpenAssemblyWatch(file, assemblyModel.getJaxbRoot());
            openEventGraphs(file);
        } 
        else 
        {
            assemblyViewFrame.deleteTab(assemblyModel);
        }
        assemblyViewFrame.toggleAssemblyStatusIndicators();
        resetRedoUndoStatus();
        assemblyViewFrame.enableAssemblyMenuItems();
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() 
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();

        if (assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper() != null) {
            ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Assembly file opened as "open" in the app config file */
    private void markAssemblyFilesOpened() 
    {
        // Mark every vAMod opened as "open"
        AssemblyModel[] openAlready = ViskitGlobals.instance().getAssemblyEditorViewFrame().getOpenAssemblyModels();
        for (AssemblyModel nextAssemblyModel : openAlready) {
            if (nextAssemblyModel.getCurrentFile() != null)
                markAssemblyConfigurationOpen(nextAssemblyModel.getCurrentFile());
        }
    }

    @Override
    public void openRecentAssembly(File path) {
        _doOpen(path);
    }

    /** Tell the Assembly File listener our new name
     *
     * @param f the XML Assembly file
     * @param jaxbroot the JAXB root of this XML file
     */
    public void initializeOpenAssemblyWatch(File f, SimkitAssembly jaxbroot) {
        OpenAssembly.instance().setFile(f, jaxbroot);
    }

    /** @return the listener for this AssemblyControllerImpl */
    @Override
    public OpenAssembly.AssemblyChangeListener getAssemblyChangeListener() {
        return assemblyChangeListener;
    }
    
    private Set<OpenAssembly.AssemblyChangeListener> isLocalModifiedSet = new HashSet<>();
    
    OpenAssembly.AssemblyChangeListener assemblyChangeListener = new OpenAssembly.AssemblyChangeListener() 
    {
        @Override
        public void assemblyChanged(int assemblyChangeListenerAction, OpenAssembly.AssemblyChangeListener sourceAssemblyChangeListener, Object parameterObject) 
        {
            if (assemblyViewFrame == null)
                assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
            if (assemblyModel == null)
                assemblyModel = (AssemblyModelImpl) getModel();
        
            switch (assemblyChangeListenerAction) 
            {    
                case JAXB_CHANGED:
                    isLocalModifiedSet.remove(sourceAssemblyChangeListener);
                    if (isLocalModifiedSet.isEmpty())
                    {
                        localModified = false; // not expecting modified if loaded from JAXB
                    }
                    assemblyModel.setModelModified(false);
                    break;

                case NEW_ASSEMBLY:
                    isLocalModifiedSet.clear();
                    localModified = false;
                    ((AssemblyModel) getModel()).setModelModified(false); // TODO watchout might cause infinite loop
                    break;

                case PARAMETER_LOCALLY_EDITED:
                    // This gets hit when you type something in the last three tabs
                    isLocalModifiedSet.add(sourceAssemblyChangeListener);
                    localModified = true;
                    assemblyModel.setModelModified(true);
                    break;

                case CLOSE_ASSEMBLY:
                    // Close any currently open Event Graphs because we don't yet know which ones
                    // to keep open until iterating through each remaining vAMod
                    ViskitGlobals.instance().getEventGraphController().closeAll();

                    markAssemblyConfigurationClosed(assemblyModel.getCurrentFile());

                    AssemblyView assemblyView = (AssemblyView) getView();
                    assemblyView.deleteTab(assemblyModel);
                    assemblyModel = null;

                    // NOTE: This doesn't work quite right. If no Assembly is open,
                    // then any non-associated Event Graphs that were also open will
                    // annoyingly close from the closeAll call above. We are
                    // using an open Event Graph cache system that relies on parsing an
                    // Assembly file to find its associated Event Graphs to open
                    if (!isCloseAll())
                    {
                        AssemblyModel[] assemblyModelArray = assemblyView.getOpenAssemblyModels();
                        for (AssemblyModel nextAssemblyModel : assemblyModelArray) {
                            if (!nextAssemblyModel.equals(assemblyModel))
                                openEventGraphs(nextAssemblyModel.getCurrentFile());
                        }
                    }
                    break;

                default:
                    LOG.warn("Program error AssemblyController.assemblyChanged");
            }
            assemblyViewFrame.toggleAssemblyStatusIndicators();
            assemblyViewFrame.enableAssemblyMenuItems();
        }

        @Override
        public String getHandle() {
            return "Assembly Controller";
        }
    };

    @Override
    public String getHandle() {
        return assemblyChangeListener.getHandle();
    }

    @Override
    public void assemblyChanged(int assemblyChangeListenerAction, OpenAssembly.AssemblyChangeListener source, Object param) {
        assemblyChangeListener.assemblyChanged(assemblyChangeListenerAction, source, param);
    }

    @Override
    public void addAssemblyFileListener(OpenAssembly.AssemblyChangeListener assemblyChangeListener) {
        OpenAssembly.instance().addListener(assemblyChangeListener);
    }

    @Override
    public void removeAssemblyFileListener(OpenAssembly.AssemblyChangeListener assemblyChangeListener) {
        OpenAssembly.instance().removeListener(assemblyChangeListener);
    }

    Set<MvcRecentFileListener> recentAssemblyListenerSet = new HashSet<>();

    @Override
    public void addRecentAssemblyFileSetListener(MvcRecentFileListener mvcRecentFileListener) {
        recentAssemblyListenerSet.add(mvcRecentFileListener);
    }

    @Override
    public void removeRecentAssemblyFileSetListener(MvcRecentFileListener mvcRecentFileListener) {
        recentAssemblyListenerSet.remove(mvcRecentFileListener);
    }

    /** Here we are informed of open Event Graphs */

    private void notifyRecentAssemblyFileListeners() {
        for (MvcRecentFileListener mvcRecentFileListener : recentAssemblyListenerSet)
            mvcRecentFileListener.listenerChanged();
    }

    Set<MvcRecentFileListener> recentProjectListenerSet = new HashSet<>();

    @Override
    public void addRecentProjectFileSetListener(MvcRecentFileListener mvcRecentFileListener) {
        recentProjectListenerSet.add(mvcRecentFileListener);
    }

    @Override
    public void removeRecentProjectFileSetListener(MvcRecentFileListener mvcRecentFileListener) {
        recentProjectListenerSet.remove(mvcRecentFileListener);
    }

    private void notifyRecentProjectFileListeners() {
        for (MvcRecentFileListener mvcRecentFileListener : recentProjectListenerSet) {
            mvcRecentFileListener.listenerChanged();
        }
    }

    /** Here we are informed of open Event Graphs */
    DirectoryWatch.DirectoryChangeListener eventGraphListener = (File file, int action, DirectoryWatch source) -> {
        // Do nothing.  The DirectoryWatch still tracks temp Event Graphs, but we
        // don't need anything special from this method, yet....
    };

    @Override
    public DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener() {
        return eventGraphListener; // A live listener, but currently doing nothing (tdn) 9/13/24
    }
    
    /** method name for reflection use */
    public static final String METHOD_save = "save";

    @Override
    public void save()
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
            
        if (!ViskitGlobals.instance().isSelectedAssemblyEditorTab())
        {
            ViskitGlobals.instance().selectAssemblyEditorTab();
            if ((ViskitGlobals.instance().getAssemblyEditorViewFrame().getNumberAssembliesLoaded() > 1) &&
                 ViskitGlobals.instance().hasModifiedAssembly())
            {
                ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "Select Assembly", "First select an Assembly in Assembly Editor before saving");
                return;
            }
        }
        ViskitGlobals.instance().selectAssemblyEditorTab(); // ensure proper focus
        if (assemblyModel == null)
            assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        if (assemblyModel == null)
        {
            LOG.error("unable to save() null assemblyModel");
            return;
        }
        if (assemblyModel.getCurrentFile() == null) 
        {
            saveAs();
        } 
        else 
        {
            assemblyModel.saveModel(assemblyModel.getCurrentFile());
        }
        assemblyViewFrame.toggleAssemblyStatusIndicators();
        assemblyViewFrame.enableAssemblyMenuItems();
    }
    
    /** method name for reflection use */
    public static final String METHOD_saveAll= "saveAll";

    /** Save all modified Assembly and Event Graph models */
    public void saveAll()
    {
        if (ViskitGlobals.instance().hasModifiedAssembly())
        {
            int numberOfAssemblies = ViskitGlobals.instance().getAssemblyEditorViewFrame().getNumberAssembliesLoaded();
            if (ViskitGlobals.instance().hasModifiedAssembly())
            {                
                for (int index = 0; index < numberOfAssemblies; index++)
                {
                    AssemblyModel nextAssemblyModel = ViskitGlobals.instance().getAssemblyEditorViewFrame().getOpenAssemblyModels()[index];
                    if (nextAssemblyModel.isModelModified())
                    {
                        ((AssemblyModelImpl) nextAssemblyModel).saveModel(nextAssemblyModel.getCurrentFile());
                    }
                }
            }
        }
        if (ViskitGlobals.instance().hasModifiedEventGraph())
        {
            ViskitGlobals.instance().getEventGraphController().saveAll();
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_saveAs = "saveAs";

    @Override
    public void saveAs()
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        
        if (!ViskitGlobals.instance().isSelectedAssemblyEditorTab())
        {
            ViskitGlobals.instance().selectAssemblyEditorTab();
            if (ViskitGlobals.instance().getAssemblyEditorViewFrame().getNumberAssembliesLoaded() > 1)
            {
                ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "Select Assembly", "First select an Assembly before saving");
                return;
            }
        }
        assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        if (assemblyModel == null) 
        {
            LOG.error(METHOD_saveAs + "() failed, (assemblyModel == null)");
            return; // not expected
        }
        GraphMetadata graphMetadata = assemblyModel.getMetadata();

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = assemblyViewFrame.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false, "Save Assembly File As");

        if (saveFile != null) 
        {
            String saveFileName = saveFile.getName();
            if (saveFileName.toLowerCase().endsWith(".xml")) {
                saveFileName = saveFileName.substring(0, saveFileName.length() - 4);
            }
            graphMetadata.name = saveFileName;
            assemblyModel.changeMetadata(graphMetadata); // might have renamed

            assemblyModel.saveModel(saveFile);
            assemblyViewFrame.setSelectedAssemblyName(graphMetadata.name);
            adjustRecentAssemblySet(saveFile);
            markAssemblyFilesOpened();
            assemblyViewFrame.toggleAssemblyStatusIndicators();
            assemblyViewFrame.enableAssemblyMenuItems();
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_editGraphMetadata = "editGraphMetadata";

    @Override
    public void editGraphMetadata() 
    {
//        AssemblyModel assemblyModel = (AssemblyModel) getModel();
//        if (assemblyModel == null) 
//        {
//            return;
//        }
//        GraphMetadata graphMetadata = assemblyModel.getMetadata();
        
        ViskitGlobals.instance().selectAssemblyEditorTab();
        
        assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
//        Model model = (Model) getModel();
        if (assemblyModel == null) 
        {
            LOG.error(METHOD_editGraphMetadata + "() failed, (assemblyModel == null)");
            return; // not expected
        }
        GraphMetadata graphMetadata = assemblyModel.getMetadata();
        
        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyEditorViewFrame(), graphMetadata);
        if (modified) 
        {
            assemblyModel.changeMetadata(graphMetadata);
            ViskitGlobals.instance().getActiveAssemblyModel().setModelModified(true);

            // update title bar for frame
            ViskitGlobals.instance().getAssemblyEditorViewFrame().setSelectedAssemblyName(graphMetadata.name);
        }
    }

    // These can not be final, else reflection with fail
    private int eventGraphNodeCount = 0;
    private int adapterNodeCount = 0;
    private int propertyChangeListenerNodeCount = 0;    // A little experiment in class introspection

    private static Field eventGraphCountField;
    private static Field adapterNodeCountField;
    private static Field propertyChangeListenerNodeCountField;

    static { // do at class initialize time
        try {
            eventGraphCountField = AssemblyControllerImpl.class.getDeclaredField("eventGraphNodeCount");
           adapterNodeCountField = AssemblyControllerImpl.class.getDeclaredField("adapterNodeCount");
            propertyChangeListenerNodeCountField = AssemblyControllerImpl.class.getDeclaredField("propertyChangeListenerNodeCount");
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String shortEventGraphName(String typeName) {
        return shortName(typeName, "eventGraph_", eventGraphCountField); // use same counter
    }

    private String shortPropertyChangeListenerName(String typeName) {
        return shortName(typeName, "listener_", propertyChangeListenerNodeCountField); // use same counter
    }

    private String shortAdapterName(String typeName) {
        return shortName(typeName, "adapter_", adapterNodeCountField); // use same counter
    }

    private String shortName(String typeName, String prefix, Field intField) 
    {
        String shortname = prefix;
        if (typeName.lastIndexOf('.') != -1) {
            shortname = typeName.substring(typeName.lastIndexOf('.') + 1) + "_";
        }

        // Don't capitalize the first letter
        char[] ca = shortname.toCharArray();
        ca[0] = Character.toLowerCase(ca[0]);
        shortname = new String(ca);

        String returnValue = shortname;
        try {
            int count = intField.getInt(this);
            // Find a unique name
            AssemblyModel model = (AssemblyModel) getModel();
            do {
                returnValue = shortname + count++;
            } 
            while (model.nameExists(returnValue));   // don't force the vAMod to mangle the name
            
            intField.setInt(this, count);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.error(ex);
        }
        return returnValue;
    }
    
    /** method name for reflection use */
    public static final String METHOD_newProject = "newProject";

    @Override
    public void newProject() 
    {
        if (ViskitGlobals.instance().isProjectOpen()) 
        {
            closeProject();
        }
        // TODO ask for project name?
//        ViskitGlobals.instance().createProjectWorkingDirectory();
        
        ViskitGlobals.instance().initializeProjectHome();

        // For a brand new empty project open a default Event Graph
        File[] eventGraphFiles = new File[0];
        if (ViskitGlobals.instance().hasViskitProject())
            eventGraphFiles = ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory().listFiles();
        if (eventGraphFiles.length == 0) {
            ViskitGlobals.instance().getEventGraphController().newEventGraph();
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_zipProject = "zipProject";

    @Override
    public void zipProject()
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() 
        {
            String projectName;
            File projectDirectory;
            File projectZipFile;
            File projectZipLogFile;

            @Override
            public Void doInBackground() // zipProject
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
                String dateOutput = formatter.format(new Date()); // today, now
                
                projectDirectory  = ViskitGlobals.instance().getViskitProject().getProjectDirectory();
                projectName       = projectDirectory.getName();
                projectZipFile    = new File(projectDirectory.getParentFile(), projectName + "_" + dateOutput + ".zip");
                projectZipLogFile = new File(projectDirectory, "projectZipDebug.log");

                if (projectZipFile.exists())
                    projectZipFile.delete();

                if (projectZipLogFile.exists())
                    projectZipLogFile.delete();

                try {

                    // First, copy the error.log to the project dir
                    if ((VISKIT_ERROR_LOG != null) && VISKIT_ERROR_LOG.exists())
                    {
                        // TODO look at including other log files
                        Files.copy(VISKIT_ERROR_LOG.toPath(), projectZipLogFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    ZipUtilities.initializeCounts();
                    ZipUtilities.zipFolder(projectDirectory, projectZipFile);
                    LOG.info("ZipUtils processFolder() found " + ZipUtilities.getDirectoryCount() + " directories and " + 
                                                        ZipUtilities.getFileCount()      + " files");
                    LOG.info("zipProject() projectZipFile\n      {}", projectZipFile);   
                } 
                catch (IOException e) {
                    LOG.error(e);
                }
                return null;
            }

            @Override
            public void done()  // zipProject
            {
                try {
                    // Waits for the zip process to finish
                    get();
                    
                    String projectSuffix = new String();
                    if (!projectName.toLowerCase().contains("project"))
                         projectSuffix = " project";
                    String message = "<html><p align='center'>Viskit project zip file is now ready for " + projectName + projectSuffix + ".</p><br />" + 
                                           "<p align='center'>Now showing</p><br />" + 
                                           "<p align='center'>" + projectZipFile.getAbsolutePath()+ "</p></html>";
                    ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                        "Project zip complete", message);

                    Desktop.getDesktop().open(projectZipFile.getParentFile());
                    
                    // possibly useful for students in class
//                    try {
//                        URL mailtoURL = new URI("mailto:" + ViskitStatics.VISKIT_MAILING_LIST
//                                + "?subject=Viskit%20Project%20Submission%20for%20"
//                                + projectDirectory.getAssemblyName() + "&body=see%20attachment").toURL();
//                        
//                        String message = "Please navigate to<br/>"
//                                + projectZipFile.getParent()
//                                + "<br/>and email the " + projectZipFile.getAssemblyName()
//                                + " file to "
//                                + "<b><a href=\"" + mailtoURL.toString() + "\">"
//                                + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
//                                + "<br/><br/>Click the link to open up an email "
//                                + "form, then attach the zip file";
//                        ViskitStatics.showHyperlinkedDialog((Component) getView(), "Viskit Project: " + projectDirectory.getAssemblyName(), mailtoURL, message, false);
//                    } catch (MalformedURLException | URISyntaxException e) {
//                        LOG.error(e);
//                    }

                } 
                catch (InterruptedException | ExecutionException | IOException e) {
                    LOG.error(e);
                }
            }
        };
        worker.execute();
    }
    
    /** method name for reflection use */
    public static final String METHOD_closeProject = "closeProject";

    /** 
     * Common method between the AssemblyView and this AssemblyController
     * @return indication of continue or cancel
     */
    public boolean closeProject() 
    {
        boolean projectClosed;
        String title, message;
        
        if (ViskitGlobals.instance().getViskitProject() == null)
            return true; // no project to close
        
        if (ViskitGlobals.instance().getViskitProject().isProjectOpen()) // TODO 
        {
            title = "Close Current Project?";
            message = "<html><p align='center'>Are you sure that you want to close</p><br/>";
            String projectName = ViskitGlobals.instance().getProjectWorkingDirectory().getAbsolutePath();
            if  (projectName.toLowerCase().contains("project"))
                 message += "<p align='center'><i>"         + projectName + "</i></p><br/><p align='center'> as the currently active project?</p><br/>";
            else message += "<p align='center'>Project <i>" + projectName + "</i></p><br/><p align='center'> as the currently active project?</p><br/>";
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            if (returnValue == JOptionPane.YES_OPTION)
            {
                doProjectCleanup();
                projectClosed = true;
                // TODO duplicative
                viskitProject = null;
                ViskitGlobals.instance().setViskitProject(null);
            } 
            else {
                projectClosed = false;
            }
        }
        else // hopefully not reachable if other logic works OK
        {
//            title = "No project is open";
//            message  = "<html><p align='center'>Unable to close project, since no project is open.</p><br />";
//            message +=       "<p align='center'>Use menu <i>Project &gt; Open Project</i> &nbsp;to continue.</p><br />";
//            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, title, message);
            projectClosed = true;
        }
        ViskitGlobals.instance().getAssemblyEditorViewFrame().enableProjectMenuItems();
        
        MainFrame.displayWelcomeGuidance(); // modal dialog; advise user to open or create a project
        
        // not working
//        runLater(500L, () -> {
//            MainFrame.displayWelcomeGuidance(); // modal dialog; advise user to open or create a project
//        });
        return projectClosed;
    }

    @Override
    public void doProjectCleanup()
    {
        closeAll();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();
        ViskitUserConfiguration.instance().clearViskitConfiguration();
        clearRecentAssemblyFileList();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).clearRecentEventGraphFileSet();
        ViskitGlobals.instance().getViskitProject().closeProject();
    }
    
    /** method name for reflection use */
    public static final String METHOD_openProject = "openProject";

    @Override
    public void openProject(File projectDirectory)
    {
        ViskitGlobals.instance().setProjectFile(projectDirectory);
        ViskitGlobals.instance().setProjectName(projectDirectory.getName());
        ViskitGlobals.instance().createProjectWorkingDirectory();

        // Add our currently opened project to the recently opened projects list
        adjustRecentProjectSet(ViskitGlobals.instance().getViskitProject().getProjectDirectory());
        
        ViskitGlobals.instance().setTitleProjectName(ViskitGlobals.instance().getProjectName());
        
        ViskitGlobals.instance().getAssemblyEditorViewFrame().enableProjectMenuItems();
        
        runnerSimulationRunInterface.resetSimulationRunPanel();
    }
    
    /** method name for reflection use */
    public static final String METHOD_newAssembly = "newAssembly";

    @Override
    public void newAssembly()
    {
        // Don't allow a new Assembly to be created if a current project is not open
        if (!ViskitGlobals.instance().getViskitProject().isProjectOpen()) 
        {
            LOG.error("newAssembly() unable to create new assembly if project is not open");
            return;
        }
        ViskitGlobals.instance().selectAssemblyEditorTab();

        GraphMetadata oldGraphMetadata = null;
        AssemblyModel viskitAssemblyModel = (AssemblyModel) getModel();
        if (viskitAssemblyModel != null) {
            oldGraphMetadata = viskitAssemblyModel.getMetadata();
        }

        AssemblyModelImpl newAssemblyModel = new AssemblyModelImpl(this);
        newAssemblyModel.initialize();
        newAssemblyModel.newAssemblyModel(null); // should create new assembly file

        // No vAMod set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ViskitGlobals.instance().getAssemblyEditorViewFrame().addTab(newAssemblyModel);

        GraphMetadata graphMetadata = new GraphMetadata(newAssemblyModel);   // build a new one, specific to Assembly
        if (oldGraphMetadata != null) {
            graphMetadata.packageName = oldGraphMetadata.packageName;
        }

        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyEditorViewFrame(), graphMetadata);
        if (modified) 
        {
            // TODO NPE
            ((AssemblyModel) getModel()).changeMetadata(graphMetadata);

            // update title bar
            ViskitGlobals.instance().getAssemblyEditorViewFrame().setSelectedAssemblyName(graphMetadata.name);

            // TODO: Implement this
//            ((AssemblyView)  getView()).setSelectedEventGraphDescription(graphMetadata.description);
        } 
        else 
        {
            ViskitGlobals.instance().getAssemblyEditorViewFrame().deleteTab(newAssemblyModel);
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_quit  = "quit";

    @Override
    public void quit() 
    {
        String title, message;
        title = "Quit Viskit?";
        message = "<html><p align='center'>Are you sure that you want to quit Viskit?</p><br/>";
        int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
        if (returnValue == JOptionPane.YES_OPTION)
        {
            if (preQuit()) {
                postQuit();
            }
            ViskitGlobals.instance().getMainFrame().quit();
        }
        // otherwise return
    }

    @Override
    public boolean preQuit()
    {
        // Check for modified models before exiting
        AssemblyModel[] assemblyModelArray = ViskitGlobals.instance().getAssemblyEditorViewFrame().getOpenAssemblyModels();
        for (AssemblyModel vmod : assemblyModelArray) {
            setModel((MvcModel) vmod);

            // Check for a canceled exit
            if (!preClose()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void postQuit() 
    {
        ViskitGlobals.instance().quitAssemblyEditor();
    }

    private boolean closeAll = false;
    
    /** method name for reflection use */
    public static final String METHOD_closeAll = "closeAll";

    @Override
    public void closeAll()
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        
        int numberOfAssemblies = ViskitGlobals.instance().getAssemblyEditorViewFrame().getNumberAssembliesLoaded();
        
        if ( ViskitGlobals.instance().hasModifiedAssembly() && 
            !ViskitGlobals.instance().isSelectedAssemblyEditorTab() && (numberOfAssemblies != 0))
        {
            ViskitGlobals.instance().selectAssemblyEditorTab(); // making sure
            String title = new String(), message = new String();
            if  (numberOfAssemblies == 1)
            {
                title   = "Inspect assembly before closing";
                message = "First inspect open Assembly in Assembly Editor before closing";
            }
            else if (numberOfAssemblies > 1) // more that one
            {
                title   = "Inspect assemblies before closing";
                message = "First inspect open Assemblies in Assembly Editor before closing them";
            }
            ViskitGlobals.instance().selectAssemblyEditorTab();
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, 
                    title, message);
                return;
        }
        ViskitGlobals.instance().selectAssemblyEditorTab(); // making sure
        
        int numberOfEventGraphs = ViskitGlobals.instance().getEventGraphEditorViewFrame().getNumberEventGraphsLoaded();
        if (numberOfEventGraphs > 1)
            LOG.info("Closing all assemblies also closes corresponding event graphs");
        
        AssemblyModel[] assemblyModelArray = ViskitGlobals.instance().getAssemblyEditorViewFrame().getOpenAssemblyModels();
        for (AssemblyModel nextAssemblyModel : assemblyModelArray) 
        {
            setModel((MvcModel) nextAssemblyModel);
            setCloseAll(true);
            close(); // each one
        }
        setCloseAll(false);
        assemblyViewFrame.enableAssemblyMenuItems();
    }
    
    /** method name for reflection use */
    public static final String METHOD_close = "close";

    @Override
    public void close()
    {
        int numberOfAssemblies = ViskitGlobals.instance().getAssemblyEditorViewFrame().getNumberAssembliesLoaded();
        if (!ViskitGlobals.instance().isSelectedAssemblyEditorTab() && (numberOfAssemblies > 1))
        {
            ViskitGlobals.instance().selectAssemblyEditorTab();
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, 
                    "Confirm Assembly choice", 
                    "First select one of the open Assemblies before closing");
            return;
        }
        if (!ViskitGlobals.instance().isSelectedAssemblyEditorTab() && (numberOfAssemblies == 0))
        {
            ViskitGlobals.instance().selectAssemblyEditorTab();
            return;
        }
        if (!ViskitGlobals.instance().isSelectedAssemblyEditorTab()) // only one open Assembly, but it wasn't visible
        {
            ViskitGlobals.instance().selectAssemblyEditorTab();
            String title   = "Please confirm...";
            String message = "Close " + ViskitGlobals.instance().getActiveAssemblyName() + "?";
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            if (returnValue == JOptionPane.NO_OPTION)
                return;
        }
        ViskitGlobals.instance().selectAssemblyEditorTab(); // making sure
        
//        boolean hasModifiedEventGraph = false; // TODO if needed
//        
//        Model[] eventGraphModels = ((EventGraphView) getView()).getOpenEventGraphModels();
//        for (Model model : eventGraphModels) 
//        {
//            if (model.isModified())
//            {
//                hasModifiedEventGraph = true;
//                break;
//            }
//        }
//        if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() && hasModifiedEventGraph)
//        {
//            ViskitGlobals.instance().selectEventGraphEditorTab();
//            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "View Event Graph Editor", "First review Event Graphs before closing");
//            return;
//        }
        if (preClose()) {
            postClose();
        }
        assemblyViewFrame.enableAssemblyMenuItems();
    }

    @Override
    public boolean preClose() 
    {
        assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel(); // (AssemblyModel) getModel();
        if (assemblyModel == null) {
            return false;
        }
        LOG.debug("preClose() close assembly {}", assemblyModel.getCurrentAssemblyModelName());
        if (assemblyModel.isModelModified()) 
        {
            return checkSaveIfModified();
        }
        return true;
    }

    @Override
    public void postClose() 
    {
        LOG.debug("postClose() close assembly {}", OpenAssembly.getName());
        OpenAssembly.instance().doFireActionCloseAssembly();
        ViskitGlobals.instance().selectAssemblyEditorTab();
    }

    private void markAssemblyConfigurationClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int idx = 0;
        for (String key : recentAssemblyFileSet) {
            if (key.contains(f.getName()))
                historyConfiguration.setProperty(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "false");

            idx++;
        }
    }

    // The open attribute is zeroed out for all recent filesArray the first time a file is opened
    private void markAssemblyConfigurationOpen(File f) {

        int idx = 0;
        for (String key : recentAssemblyFileSet) {
            if (key.contains(f.getName()))
                historyConfiguration.setProperty(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "true");

            idx++;
        }
    }

    private final Point nextPoint = new Point(25, 25);
    private Point getNextPoint() {
        nextPoint.x = nextPoint.x >= 200 ? 25 : nextPoint.x + 25;
        nextPoint.y = nextPoint.y >= 200 ? 25 : nextPoint.y + 25;
        return nextPoint;
    }
    
    /** method name for reflection use */
    public static final String METHOD_newEventGraphNode = "newEventGraphNode";

    @Override
    public void newEventGraphNode() // menu click
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        Object o = ViskitGlobals.instance().getAssemblyEditorViewFrame().getSelectedEventGraph();

        if (o != null) {
            if (o instanceof Class<?>) {
                newEventGraphNode(((Class<?>) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssemblyNode) {
                newFileBasedEventGraphNode((FileBasedAssemblyNode) o, getNextPoint());
                return;
            }
        }
        // Nothing selected or non-leaf
        ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select an Event Graph from the panel on the left.");
    }

    @Override
    public void newEventGraphNode(String typeName, Point p) {
        String shName = shortEventGraphName(typeName);
        ((AssemblyModel) getModel()).newEventGraph(shName, typeName, p);
    }

    @Override
    public void newFileBasedEventGraphNode(FileBasedAssemblyNode xnode, Point p) {
        String shName = shortEventGraphName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newEventGraphFromXML(shName, xnode, p);
    }

    @Override
    public void newPropertyChangeListenerNode() // menu click
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        Object o = ViskitGlobals.instance().getAssemblyEditorViewFrame().getSelectedPropertyChangeListener();

        if (o != null) {
            if (o instanceof Class<?>) {
                newPropertyChangeListenerNode(((Class<?>) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssemblyNode) {
                newFileBasedPropertyChangeListenerNode((FileBasedAssemblyNode) o, getNextPoint());
                return;
            }
        }
        // If nothing selected or a non-leaf
        ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select a Property Change Listener from the panel on the left.");
    }
    
    /** method name for reflection use */
    public static final String METHOD_newPropertyChangeListenerNode = "newPropertyChangeListenerNode";

    @Override
    public void newPropertyChangeListenerNode(String name, Point p) {
        String shName = shortPropertyChangeListenerName(name);
        ((AssemblyModel) getModel()).newPropertyChangeListener(shName, name, p);
    }

    @Override
    public void newFileBasedPropertyChangeListenerNode(FileBasedAssemblyNode xnode, Point p) {
        String shName = shortPropertyChangeListenerName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newPropertyChangeListenerFromXML(shName, xnode, p);
    }

    /**
     *
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue()
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        String message = "Save modified ";
        assemblyModel = (AssemblyModelImpl) getModel();
        if ((assemblyModel != null) && (assemblyModel.getMetadata() != null))
             message += assemblyModel.getMetadata().name + ".xml"; 
        if (!message.toLowerCase().contains("assembly"))
             message += " assembly";
        message += "?";
        int userResponse = (ViskitGlobals.instance().getMainFrame().genericAsk("Save assembly?", message));

        switch (userResponse) 
        {
            case JOptionPane.YES_OPTION:
                save();
                // TODO: Can't remember why this is here after a save?
                if (((AssemblyModel) getModel()).isModelModified()) {
                    return false;
                } // we cancelled
                return true;
                
            case JOptionPane.NO_OPTION:

                // No need to recompile
                if (((AssemblyModel) getModel()).isModelModified()) {
                    ((AssemblyModel) getModel()).setModelModified(false);
                }
                return true;
                
            case JOptionPane.CANCEL_OPTION:
                return false;

            // Something funny if we're here
            default:
                return false;
        }
    }

    @Override
    public void newAdapterEdge(Object[] nodes) 
    {
        AssemblyNode assemblyNodeA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode assemblyNodeB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] assemblyNodeArray;
        try {
            assemblyNodeArray = checkLegalForSEListenerEdge(assemblyNodeA, assemblyNodeB);
        } 
        catch (Exception e) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Connection error.", "Possible class not found.  All referenced entities must be in a list at left.");
            return;
        }
        if (assemblyNodeArray == null) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        adapterEdgeEdit(((AssemblyModel) getModel()).newAdapterEdge(shortAdapterName(""), assemblyNodeArray[0], assemblyNodeArray[1]));
    }

    @Override
    public void newSimEventListenerEdge(Object[] nodes) {
        AssemblyNode assemblyNodeA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode assemblyNodeB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] assemblyNodeArray = checkLegalForSEListenerEdge(assemblyNodeA, assemblyNodeB);

        if (assemblyNodeArray == null) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        ((AssemblyModel) getModel()).newSimEventListenerEdge(assemblyNodeArray[0], assemblyNodeArray[1]);
    }

    @Override
    public void newPropertyChangeListenerEdge(Object[] nodes) {
        // One and only one has to be a prop change listener
        AssemblyNode assemblyNodeA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode assemblyNodeB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] assemblyNodeArray = checkLegalForPropertyChangeEdge(assemblyNodeA, assemblyNodeB);

        if (assemblyNodeArray == null) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a PropertyChangeListener and PropertyChangeSource combination.");
            return;
        }
        propertyChangeListenerEdgeEdit(((AssemblyModel) getModel()).newPropChangeEdge(assemblyNodeArray[0], assemblyNodeArray[1]));
    }

    AssemblyNode[] checkLegalForSEListenerEdge(AssemblyNode a, AssemblyNode b) {
        Class<?> ca = findClass(a);
        Class<?> cb = findClass(b);
        return orderSELSrcAndLis(a, b, ca, cb);
    }

    AssemblyNode[] checkLegalForPropertyChangeEdge(AssemblyNode a, AssemblyNode b) {
        Class<?> ca = findClass(a);
        Class<?> cb = findClass(b);
        return orderPCLSrcAndLis(a, b, ca, cb);
    }

    Class<?> findClass(AssemblyNode o) {
        return ViskitStatics.classForName(o.getType());
    }

    AssemblyNode[] orderPCLSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        // tbd, reloading these classes is needed right now as
        // we don't know if the workClassLoader is the same instance
        // as it used to be when these were originally loaded
        // the tbd here is to see if there can be a shared root loader
        simEventSourceClass = ViskitStatics.classForName("simkit.SimEventSource");
        simEventListenerClass = ViskitStatics.classForName("simkit.SimEventListener");
        propertyChangeSourceClass = ViskitStatics.classForName("simkit.PropertyChangeSource");
        propertyChangeListenerClass = ViskitStatics.classForName("java.beans.PropertyChangeListener");
        if (propertyChangeSourceClass.isAssignableFrom(ca)) {
            obArr[0] = a;
        } else if (propertyChangeSourceClass.isAssignableFrom(cb)) {
            obArr[0] = b;
        }
        if (propertyChangeListenerClass.isAssignableFrom(cb)) {
            obArr[1] = b;
        } else if (propertyChangeListenerClass.isAssignableFrom(ca)) {
            obArr[1] = a;
        }

        if (obArr[0] == null || obArr[1] == null || obArr[0] == obArr[1]) {
            return null;
        }
        return obArr;
    }

    AssemblyNode[] orderSELSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        simEventSourceClass = ViskitStatics.classForName("simkit.SimEventSource");
        simEventListenerClass = ViskitStatics.classForName("simkit.SimEventListener");
        propertyChangeSourceClass = ViskitStatics.classForName("simkit.PropertyChangeSource");
        propertyChangeListenerClass = ViskitStatics.classForName("java.beans.PropertyChangeListener");
        if (simEventSourceClass.isAssignableFrom(ca)) {
            obArr[0] = a;
        } else if (simEventSourceClass.isAssignableFrom(cb)) {
            obArr[0] = b;
        }
        if (simEventListenerClass.isAssignableFrom(cb)) {
            obArr[1] = b;
        } else if (simEventListenerClass.isAssignableFrom(ca)) {
            obArr[1] = a;
        }

        if (obArr[0] == null || obArr[1] == null || obArr[0] == obArr[1]) {
            return null;
        }
        return obArr;
    }

    /**
     *
     * @param pclNode Property Change Listener Node
     */
    @Override
    public void propertyChangeListenerEdit(PropertyChangeListenerNode pclNode) {
        boolean done, modified;
        do {
            done = true;
            modified = ViskitGlobals.instance().getAssemblyEditorViewFrame().doEditPropertyChangeListenerNode(pclNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changePclNode(pclNode);
            }
        } while (!done);
    }

    @Override
    public void eventGraphEdit(EventGraphNode evNode) {
        boolean done, modified;
        do {
            done = true;
            modified = ViskitGlobals.instance().getAssemblyEditorViewFrame().doEditEventGraphNode(evNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changeEventGraphNode(evNode);
            }
        } while (!done);
    }

    @Override
    public void propertyChangeListenerEdgeEdit(PropertyChangeEdge pclEdge) {
        boolean modified = ViskitGlobals.instance().getAssemblyEditorViewFrame().doEditPropertyChangeListenerEdge(pclEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changePclEdge(pclEdge);
        }
    }

    @Override
    public void adapterEdgeEdit(AdapterEdge aEdge) {
        boolean modified = ViskitGlobals.instance().getAssemblyEditorViewFrame().doEditAdapterEdge(aEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeAdapterEdge(aEdge);
        }
    }

    @Override
    public void simEventListenerEdgeEdit(SimEventListenerEdge seEdge) {
        boolean modified = ViskitGlobals.instance().getAssemblyEditorViewFrame().doEditSimEventListenerEdge(seEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeSimEventListenerEdge(seEdge);
        }
    }

    private Vector<Object> selectionVector = new Vector<>();
    private Vector<Object> copyVector = new Vector<>();

    @Override
    public void selectNodeOrEdge(Vector<Object> v) {
        selectionVector = v;
        boolean selected = nodeOrEdgeSelected();

        // Cut not supported
        ActionIntrospector.getAction(this, "cut").setEnabled(false);
        ActionIntrospector.getAction(this, "remove").setEnabled(selected);
        ActionIntrospector.getAction(this, "copy").setEnabled(selected);
    }

    private boolean nodeCopied() {
        return nodeOrEdgeInVector(copyVector);
    }

    private boolean nodeOrEdgeSelected() {
        return nodeOrEdgeInVector(selectionVector);
    }

    private boolean nodeOrEdgeInVector(Vector<Object> v) {
        for (Object o : v) {
            if (o instanceof AssemblyNode) {
                return true;
            }
            if (o instanceof AssemblyEdge) {
                return true;
            }
        }
        return false;
    }

    private boolean doRemove = false;
    
    /** method name for reflection use */
    public static final String METHOD_remove = "remove";

    @Override
    @SuppressWarnings("unchecked")
    public void remove()
    {
        String foundObjectName = "", description = "", message, specialNodeMessage;
        ViskitGlobals.instance().selectAssemblyEditorTab();
        
        // Prevent concurrent update while looping over selectionVector
        Vector<Object> selectionVectorClone = (Vector<Object>) selectionVector.clone(); // TODO unchecked cast warning
        if (!selectionVectorClone.isEmpty()) 
        {
            // first ask:
            int nodeCount = 0;  // different message for edge delete
            for (Object nextSelectionObject : selectionVectorClone) 
            {
                if (nextSelectionObject == null)
                {
                    LOG.error("remove() selection vector included null object, ignored"); // unexpected
                }
                else if (nextSelectionObject instanceof AssemblyNode) 
                {
                    nodeCount++;
                    foundObjectName = ((AssemblyNode)nextSelectionObject).getName();
                    description     = ((AssemblyNode)nextSelectionObject).getDescription();
                    if (description.length() > 40)
                        description = description.substring(0,40) + "... ";
                    foundObjectName = foundObjectName.replace('\n', ' ').trim();
                }
                else
                {
                    foundObjectName = nextSelectionObject.getClass().getName();
                    LOG.error("remove() found unexpected deletion type: {}", foundObjectName);
                }
                if ((description == null) || description.isBlank())
                    description = NO_DESCRIPTION_PROVIDED_HTML;
                specialNodeMessage = (nodeCount > 0) ? 
                        "<p align='center'>Note that all unselected but attached edges are also removed.</p>" : "";
                message = "<html><body><center>" +
                          "<p align='center'>Confirm removal of " + foundObjectName + "?" + "</p>" + 
                          "<p align='center'>(description: " + description + ")" + "</p><br />" + 
                          specialNodeMessage +
                          "</center></body></html>";
                doRemove = ViskitGlobals.instance().getMainFrame().genericAsk(
                        "Remove element from Assembly?", message) == JOptionPane.YES_OPTION;
               if (doRemove)
                {
                    // LOG.info messages are found in removeSelectedGraphObjects()
                    LOG.debug("remove() {} element from Assembly approved by auther", foundObjectName);
                }
                else // deselect
                {
                    selectionVector.removeElement(nextSelectionObject); // change master while continuing to loop over clone
                }
            }
            if (!selectionVector.isEmpty())
                removeSelectedGraphObjects();
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_cut = "cut";

    @Override
    public void cut() //---------------
    {
        // Not supported
    }
    
    /** method name for reflection use */
    public static final String METHOD_copy = "copy";

    @Override
    @SuppressWarnings("unchecked")
    public void copy() {
        if (selectionVector.isEmpty()) {
            ViskitGlobals.instance().messageUser(JOptionPane.WARNING_MESSAGE,
                    "Unsupported Action",
                    "Edges cannot be copied.");
            return;
        }
        copyVector = (Vector<Object>) selectionVector.clone();

        // Paste only works for a node, check to enable/disable paste menu item
        handlePasteMenuItem();
    }

    private void handlePasteMenuItem() {
        ActionIntrospector.getAction(this, "paste").setEnabled(nodeCopied());
    }

    /** Also acts as a bias for point offset */
    private int copyCount = 0;
    
    /** method name for reflection use */
    public static final String METHOD_paste = "paste";

    @Override
    public void paste() //-----------------
    {
        if (copyVector.isEmpty()) {
            return;
        }
        int x = 100, y = 100, offset = 20;
        Point2D p;
        String nm, typ;
        for (Object o : copyVector) {
            if (o instanceof AssemblyEdge) {
                continue;
            }

            p = new Point(x + (offset * copyCount), y + (offset * copyCount));
            if (o instanceof EventGraphNode) {
                nm = ((ViskitElement) o).getName();
                typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newEventGraph(nm + "-copy" + copyCount, typ, p);
            } else if (o instanceof PropertyChangeListenerNode) {
                nm = ((ViskitElement) o).getName();
                typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newPropertyChangeListener(nm + "-copy" + copyCount, typ, p);
            }
            copyCount++;
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void removeSelectedGraphObjects() 
    {
        AssemblyNode assemblyNode;
        
        // Prevent concurrent update while looping over selectionVector
        Vector<Object> selectionVectorClone = (Vector<Object>) selectionVector.clone(); // TODO unchecked cast warning
        
        for (Object nextNode : selectionVectorClone) 
        {
            if (nextNode instanceof AssemblyEdge) 
            {
                LOG.info("removeSelectedGraphObjects() AssemblyEdge {} element from Assembly", ((AssemblyEdge) nextNode).getName());
                removeEdge((AssemblyEdge) nextNode);
            } 
            else if (nextNode instanceof EventGraphNode) 
            {
                assemblyNode = (EventGraphNode) nextNode;
                for (AssemblyEdge nextAssemblyEdge : assemblyNode.getConnections()) 
                {
                    LOG.info("removeSelectedGraphObjects() AssemblyEdge {} element from Assembly", nextAssemblyEdge.getName());
                    removeEdge(nextAssemblyEdge);
                }
                ((AssemblyModel) getModel()).deleteEventGraphNode((EventGraphNode) assemblyNode);
            } 
            else if (nextNode instanceof PropertyChangeListenerNode)
            {
                assemblyNode = (PropertyChangeListenerNode) nextNode;
                for (AssemblyEdge nextAssemblyEdge : assemblyNode.getConnections()) 
                {
                    LOG.info("removeSelectedGraphObjects() AssemblyEdge {} element from Assembly", nextAssemblyEdge.getName());
                    removeEdge(nextAssemblyEdge);
                }
                LOG.info("removeSelectedGraphObjects() PropertyChangeListenerNode {} element from Assembly", ((PropertyChangeListenerNode) assemblyNode).getName());
                ((AssemblyModel) getModel()).deletePropertyChangeListener((PropertyChangeListenerNode) assemblyNode);
            }
        }
        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    private void removeEdge(AssemblyEdge nextAssemblyEdge) 
    {
        if (nextAssemblyEdge instanceof AdapterEdge) 
        {
            ((AssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) nextAssemblyEdge);
        } 
        else if (nextAssemblyEdge instanceof PropertyChangeEdge) 
        {
            ((AssemblyModel) getModel()).deleteProperyChangeEdge((PropertyChangeEdge) nextAssemblyEdge);
        } 
        else if (nextAssemblyEdge instanceof SimEventListenerEdge) 
        {
            ((AssemblyModel) getModel()).deleteSimEventListenerEdge((SimEventListenerEdge) nextAssemblyEdge);
        }
    }

    /** Assume the last selected element is our candidate for a redo */
    private DefaultGraphCell redoGraphCell;

    /** Inform the model that this in an undo, not full delete */
    private boolean isUndo = false;

    /**
     * Informs the model that this is an undo, not a full delete
     * @return true if an undo, not a full delete
     */
    public boolean isUndo() {
        return isUndo;
    }
    
    /** method name for reflection use */
    public static final String METHOD_undo = "undo";

    /**
     * Removes the last selected node or edge from the JGraph model
     */
    @Override
    public void undo()
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        
        ViskitGlobals.instance().selectAssemblyEditorTab();
        if (selectionVector.isEmpty())
            return;

        isUndo = true;

        Object[] roots = assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getRoots();
        for (Object root : roots) {
            if (root instanceof DefaultGraphCell)
                redoGraphCell = ((DefaultGraphCell) root);
            if (selectionVector.firstElement().equals(redoGraphCell.getUserObject()))
                break;
        }
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache());
        } 
        catch (CannotUndoException ex) {
            LOG.error(METHOD_undo + "() unable to undo: {}", ex);
        } 
        finally {
            updateUndoRedoStatus();
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_redo = "redo";

    /**
     * Replaces the last selected node or edge from the JGraph model
     */
    @Override
    public void redo()
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        
        ViskitGlobals.instance().selectAssemblyEditorTab();

        // Recreate the JAXB (XML) bindings since the paste function only does
        // nodes and not edges
        if (redoGraphCell instanceof org.jgraph.graph.Edge) 
        {
            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof AdapterEdge) 
            {
                AdapterEdge nextAdapterEdge = (AdapterEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoAdapterEdge(nextAdapterEdge);
            } 
            else if (redoGraphCell.getUserObject() instanceof PropertyChangeEdge) 
            {
                PropertyChangeEdge nextPropertyChangeEdge = (PropertyChangeEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropertyChangeEdge(nextPropertyChangeEdge);
            } 
            else {
                SimEventListenerEdge nextSimEventListenerEdge = (SimEventListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoSimEventListenerEdge(nextSimEventListenerEdge);
            }
        } 
        else 
        {
            if (redoGraphCell.getUserObject() instanceof PropertyChangeListenerNode) 
            {
                PropertyChangeListenerNode pclNode = (PropertyChangeListenerNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropertyChangeListener(pclNode);
            } 
            else {
                EventGraphNode nextEventGraphNode = (EventGraphNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoEventGraph(nextEventGraphNode);
            }
        }

        ViskitGraphUndoManager undoManager = (ViskitGraphUndoManager) assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();
        try {
            undoManager.redo(assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache());
        } 
        catch (CannotRedoException ex) {
            LOG.error(METHOD_redo + "() unable to redo: {}", ex);
        } 
        finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() 
    {
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache()));

        isUndo = false;
    }

    /********************************/
    /* from menu:*/
    
    /** method name for reflection use */
    public static final String METHOD_viewXML = "viewXML";

    @Override
    public void viewXML()
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        if (assemblyViewFrame.getNumberAssembliesLoaded() == 0)
        {
            String message = "First load an Assembly before viewing XML source";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "No Assembly is loaded", message);
            return;
        }
        assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        if (!checkSaveForSourceCompile() || (assemblyModel == null) || (assemblyModel.getCurrentFile() == null)) 
        {
            LOG.error("viewXML() unable to retrieve assemblyModel");
                    return;
        }
        ViskitGlobals.instance().getAssemblyEditorViewFrame().displayXML(assemblyModel.getCurrentFile());
    }

    private boolean checkSaveForSourceCompile()
    {
        assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();

        // Perhaps a cached file is no longer present in the path
        if (assemblyModel == null) {return false;}
        if (assemblyModel.isModelModified() || assemblyModel.getCurrentFile() == null) {
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo("Confirm", "The model will be saved.\nContinue?");
            if (returnValue != JOptionPane.YES_OPTION) {
                return false;
            }
            this.saveAs();
        }
        return true;
    }
    
    /** method name for reflection use */
    public static final String METHOD_generateJavaCode = "generateJavaCode";

    @Override
    public void generateJavaCode()
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        if (assemblyViewFrame.getNumberAssembliesLoaded() == 0)
        {
            String message = "First load an Assembly before generating Java code";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "No Assembly is loaded", message);
            return;
        }
        
        String source = produceJavaAssemblyClass();
        if (assemblyModel == null)
            assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        if (source != null && !source.isEmpty()) 
        {
            String className = assemblyModel.getMetadata().packageName + "." + assemblyModel.getMetadata().name;
            ViskitGlobals.instance().getAssemblyEditorViewFrame().showAndSaveSource(className, source, assemblyModel.getCurrentFile().getName());
        }
    }

    private String produceJavaAssemblyClass() 
    {
        if (assemblyModel == null)
            assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        if (!checkSaveForSourceCompile() || assemblyModel.getCurrentFile() == null) {
            return null;
        }
        if (assemblyModel.getCurrentFile().exists())
        {
            if  (assemblyModel.getMetadata() != null)
                 LOG.info("Preparing assembly " + assemblyModel.getMetadata().name + " using contained Metadata");
            else LOG.error("produceJavaAssemblyClass() assemblyModel model Metadata is null");
            return buildJavaAssemblySource(assemblyModel.getCurrentFile());
        }
        else
        {
             LOG.error("produceJavaAssemblyClass() assemblyModel file does not exist]\n      {}", assemblyModel.getCurrentFile());
             return "";
        }
    }

    /**
     * Builds the actual source code from the Assembly XML after a successful
     * XML validation
     *
     * @param xmlSourceFile the Assembly file to produce source from
     * @return a string of Assembly source code
     */
    public String buildJavaAssemblySource(File xmlSourceFile) 
    {
        String assemblySourceText = null;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = new XMLValidationTool(xmlSourceFile.getPath(), XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA);

        if (!xmlValidationTool.isValidXML()) 
        {
            // TODO: implement a Dialog pointing to the validationErrors.LOG
            LOG.error("buildJavaAssemblySource{} found invalid XML!\n      " + xmlSourceFile.getAbsolutePath());
            return assemblySourceText;
        } 
        else {
            LOG.info("buildJavaAssemblySource{} found valid XML\n      " + xmlSourceFile.getAbsolutePath());
        }

        SimkitAssemblyXML2Java xml2Java;
        try {
            xml2Java = new SimkitAssemblyXML2Java(xmlSourceFile);
            xml2Java.unmarshal();
            assemblySourceText = xml2Java.translateIntoJavaSource();
        } 
        catch (FileNotFoundException e) {
            LOG.error(e);
            assemblySourceText = "";
        }
        return assemblySourceText;
    }

    // NOTE: above are routines to operate on current assembly

    /**
     * Build the actual source code from the Event Graph XML after a successful
     * XML validation
     *
     * @param simkitXML2Java the Event Graph initialized translator to produce source with
     * @return a string of Event Graph source code
     */
    public String buildJavaEventGraphSource(SimkitEventGraphXML2Java simkitXML2Java)
    {
        String eventGraphSource = null;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = new XMLValidationTool(simkitXML2Java.getEventGraphFile().getPath(),
                XMLValidationTool.LOCAL_EVENT_GRAPH_SCHEMA);

        if (!xmlValidationTool.isValidXML())
        {
            // TODO: implement a Dialog pointing to the validationErrors.LOG
            LOG.error("{} is not valid XML!\n", simkitXML2Java.getEventGraphFile().getAbsolutePath());
            return eventGraphSource;
        } 
        else 
        {
            LOG.info("buildJavaEventGraphSource() found valid XML:\n      " + simkitXML2Java.getEventGraphFile().getAbsolutePath());
        }

        try {
            eventGraphSource = simkitXML2Java.translate();
        } 
        catch (Exception e) {
            LOG.error("buildJavaEventGraphSource() error building Java from\n      {}\n      Exception: {}; erroneous event-graph xml or conversion found", simkitXML2Java.getFileBaseName(), e.getMessage());
            e.printStackTrace(); // trace is usually needed here
        }
        return eventGraphSource;
    }

    /** Create and test compile our EventGraphs and Assemblies from XML
     *
     * @param sourceCode the translated source either from SimkitEventGraphXML2Java, or SimkitAssemblyXML2Java
     * @return a reference to a successfully compiled *.class file or null if
     * a compile failure occurred
     */
    public static File compileJavaClassFromString(String sourceCode) 
    {
        String baseName;

        // Find the package subdirectory
        Pattern patttern = Pattern.compile("package.+;");
        Matcher matcher  = patttern.matcher(sourceCode);
        boolean found    = matcher.find();

        String packagePath = "";
        String packageName = "";
        if (found) 
        {
            int startIndex = matcher.start();
            int   endIndex = matcher.end();
            String s = sourceCode.substring(startIndex, endIndex);
            packageName = sourceCode.substring(startIndex, endIndex - 1);
            packageName = packageName.substring("package".length(), packageName.length()).trim();
            s = s.replace(';', File.separatorChar);
            String[] sa = s.split("\\s");
            sa[1] = sa[1].replace('.', File.separatorChar);
            packagePath = sa[1].trim();
        }

        patttern = Pattern.compile("public\\s+class\\s+");
        matcher = patttern.matcher(sourceCode);
        matcher.find();

        int end = matcher.end();
        String s = sourceCode.substring(end, sourceCode.length()).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];

        FileWriter sourceCodeFileWriter = null;
        boolean compileSuccess;
        try {
            // expecting to always have a active ViskitProject
            if (viskitProject == null)
                viskitProject = ViskitGlobals.instance().getViskitProject(); // TODO breaks singleton pattern?

            // Create, or find the project's java source and package
            File sourcePackageDirectory = new File(viskitProject.getSrcDirectory(), packagePath);
            if (!sourcePackageDirectory.isDirectory())
                 sourcePackageDirectory.mkdirs(); // include intermediate subdirectories
            File javaSourceFile = new File(sourcePackageDirectory, baseName + ".java");
            javaSourceFile.createNewFile();

            sourceCodeFileWriter = new FileWriter(javaSourceFile);
            sourceCodeFileWriter.write(sourceCode);

            // An error stream to write additional error info out to
            OutputStream errorsByteArrayOutputStream = new ByteArrayOutputStream();
            Compiler.setOutPutStream(errorsByteArrayOutputStream);

            File classesDirectory = viskitProject.getClassesDirectory();
            if (!classesDirectory.isDirectory())
                 classesDirectory.mkdirs(); // include intermediate subdirectories
            
            String relativePathToCompiledClass = packagePath + baseName + ".class";
            File compiledClassFile = new File(classesDirectory.getAbsolutePath() + "/" + relativePathToCompiledClass);
            if (!compiledClassFile.getParentFile().isDirectory())
                 compiledClassFile.getParentFile().mkdirs(); // include intermediate subdirectories
            

            // This will create a class/package to place the .class file
            String diagnostic = Compiler.invoke(packageName, baseName, sourceCode); // diagnostics only
            compileSuccess = diagnostic.equals(Compiler.COMPILE_SUCCESS_MESSAGE);
            
            // https://docs.oracle.com/javase/tutorial/essential/io/move.html
            // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#move-java.nio.file.Path-java.nio.file.Path-java.nio.file.CopyOption...-
            // move compiler results
            File compiledResultFile = new File(ViskitGlobals.instance().getProjectWorkingDirectory() + "/" + packageName, baseName + ".class");
//          compiledResultFile.getAbsolutePath(); // debug
            if  (compiledResultFile.exists())
            {
                // TODO moving files eludes the class loader later
//                Files.move(compiledResultFile.toPath(), 
//                        compiledClassFile.getParentFile().toPath().resolve(compiledResultFile.toPath().getFileName()), 
//                        REPLACE_EXISTING);
//                Files.delete(compiledResultFile.getParentFile().toPath()); // remove package directory
            }
            
            LOG.info("compileJavaClassFromString() compiling file:\n      " + javaSourceFile.getAbsolutePath() + " to\n      " +
                         compiledResultFile.getAbsolutePath());
//                       compiledClassFile.getAbsolutePath());

            LOG.debug("compileJavaClassFromString() compiledClassFile=\n      " + compiledClassFile.getAbsolutePath() +
                      "  exists=" + compiledClassFile.exists() + " length=" + compiledClassFile.length());
            if (compileSuccess) 
            {
                LOG.debug("compileJavaClassFromString(): " + 
                         "autogenerated Java compilation result\n" +
                         "=====================================\n" + 
                         diagnostic + "\n" +
                         "=====================================\n");
                return compiledClassFile;
            } 
            else
            {
                String errorResults = 
                         "autogenerated Java compilation error\n" +
                         "====================================\n" + 
                         diagnostic + "\n" +
                         "====================================\n";
                if (!errorsByteArrayOutputStream.toString().isBlank())
                    errorResults +=
                         errorsByteArrayOutputStream.toString() +
                         "====================================\n";
                LOG.error("compileJavaClassFromString(): "  + errorResults);
                // send error to simulation console for easiest user viewing
                ViskitGlobals.instance().selectSimulationRunTab();
                ViskitGlobals.instance().getSimulationRunPanel().outputStreamTA.append("\n\n" + errorResults);
                return null;
            }
        } 
        catch (IOException ioe) 
        {
            LOG.error("compileJavaClassFromString() exception" + ioe);
            ioe.printStackTrace();
        } 
        finally 
        {
            try {
                if (sourceCodeFileWriter != null)
                    sourceCodeFileWriter.close();
            } 
            catch (IOException ioe) {
                LOG.error("compileJavaClassFromString() sourceCodeFileWriter.close() exception: {}", ioe);
            }
        }
        return null;
    }

    /**
     * Known modelPath for EventGraph compilation. Called whenever an XML file
     * loads for the first time, or is saved during an edit
     *
     * @param xmlFile the EventGraph to package up
     * @return a package and file pair
     */
    public PackageAndFile createTemporaryEventGraphClass(File xmlFile) 
    {
        PackageAndFile packageAndFile = null;
        try {
            SimkitEventGraphXML2Java simkitEventGraphXML2Java = new SimkitEventGraphXML2Java(xmlFile);
            simkitEventGraphXML2Java.unmarshal();

            // SimEntity is a synonym for event graph, when running as part of an asssembly;
            // be careful to use correct package tree
//          Object unMarshalledObject = simkitEventGraphXML2Java.getUnMarshalledObject(); // debug
            boolean isEventGraph = simkitEventGraphXML2Java.getUnMarshalledObject() instanceof viskit.xsd.bindings.eventgraph.SimEntity;
            if (!isEventGraph) {
                LOG.error("Event graph {} is an Assembly: {}", xmlFile.getName(), !isEventGraph);
                return packageAndFile;
            }
            String sourceString = buildJavaEventGraphSource(simkitEventGraphXML2Java);

            /* We may have forgotten a parameter required for a super class */
            if (sourceString == null) 
            {
                String message = xmlFile + "\n" + "did not translate correctly into source code.\n" +
                        "Check console, inspect source to determine cause";
                LOG.error(message);
                ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Source code translation error", message);
                return null;
            }
            packageAndFile = compileJavaClassAndSetPackage(sourceString);
        }
        catch (FileNotFoundException e)
        {
            String message = xmlFile + "\n" + "did not translate correctly into source code.\n" +
                    "Check console, inspect source to determine cause";
            LOG.error("Event graph compilation error creating Java class file from {}: {}\n", xmlFile , e.getMessage());
            FileBasedClassManager.instance().addCacheMiss(xmlFile);
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, "Event graph compilation error", message);
        }
        return packageAndFile;
    }

    /** Path for Event Graph and Assembly compilation
     *
     * @param source the raw source to write to file
     * @return a package and file pair
     */
    private PackageAndFile compileJavaClassAndSetPackage(String source) 
    {
        String newPackage = null;
        if (source != null && !source.isEmpty()) 
        {
            Pattern pattern = Pattern.compile("package.*;");
            Matcher matcher = pattern.matcher(source);
            if (matcher.find()) 
            {
                String matchedString = matcher.group();
                if (matchedString.endsWith(";"))
                    matchedString = matchedString.substring(0, matchedString.length() - 1);

                String[] stringArray = matchedString.split("\\s");
                newPackage = stringArray[1];
            }
            File newFile = compileJavaClassFromString(source);
            // TODO check downstream, ensure file goes into correct subdirectory
            if (newFile != null)
                return new PackageAndFile(newPackage, newFile);
        }
        return null;
    }

    // From menu
    @Override
    public void export2grid() 
    {
        AssemblyModel copiedAssemblyModel = (AssemblyModel) getModel();
        File tempFile;
        try {
            tempFile = TempFileManager.createTempFile("ViskitAssembly", ".xml");
        } 
        catch (IOException e) {
            ViskitGlobals.instance().messageUser(JOptionPane.ERROR_MESSAGE, 
                                       "File system error creating tempfile", e.getMessage());
            return;
        }
        copiedAssemblyModel.saveModel(tempFile);
        // todo switch to DOE
    }
    private String[] execStringArray;

    // Known modelPath for Assembly compilation
    @Override
    public void prepareAssemblySimulationSource() 
    {
        String sourceString = produceJavaAssemblyClass(); // asks to save

        PackageAndFile packageAndFile = compileJavaClassAndSetPackage(sourceString);
        if (packageAndFile != null) 
        {
            File file = packageAndFile.file;
            String className = file.getName().substring(0, file.getName().lastIndexOf('.'));
            className = packageAndFile.packageName + "." + className;

            execStringArray = buildExecStringArray(className);
        } 
        else
        {
            execStringArray = new String[0];
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_prepareSimulationRunner = "prepareSimulationRunner";

    @Override
    public void prepareSimulationRunner()
    {
        // Prevent multiple pushes of the initialize simulation run button
        mutex++;
        if (mutex > 1) {
            return;
        }

        // Disable button to prevent double clicking which can cause potential ClassLoader issues
        Runnable r = () -> {
            (ViskitGlobals.instance().getAssemblyEditorViewFrame()).prepareAssemblyForSimulationRunButton.setEnabled(false);
        };
        try {
            SwingUtilities.invokeLater(r);
        } 
        catch (Exception e) {
            LOG.error(METHOD_prepareSimulationRunner + "() SwingUtilities.invokeLater(" + r.toString() + ") exception: " + e.getMessage());
        }

        SwingWorker<Void, Void> prepareSimulationRunnerSwingWorker = new SwingWorker<Void, Void>()
        {
            @Override
            public Void doInBackground() // prepareSimulationRunner() activity
            {
                // Compile and prepare the execStrings
                prepareAssemblySimulationSource();

                if (execStringArray == null)
                {
//                  if (ViskitGlobals.instance().getActiveAssemblyModel() == null)
                    if (!ViskitGlobals.instance().getAssemblyEditorViewFrame().hasAssembliesLoaded())
                    {
                        ViskitGlobals.instance().messageUser(JOptionPane.WARNING_MESSAGE,
                            "Assembly File Not Loaded",
                            "Please load an Assembly file");
                        LOG.error(" doInBackground() " +  "Assembly File Not Loaded, please load an Assembly file");
                    } 
                    else
                    {
                        String message1 = "Please inspect autogenerated source to debug the source error,";
                        String message2 = "then correct the assembly XML for correct proper compilation";
                        ViskitGlobals.instance().messageUser(JOptionPane.WARNING_MESSAGE, "Assembly source generation/compilation error",
                                "<html><p align-'center'>" + message1 + "</p> <p align-'center'>" + message2 + "</p>");
                        LOG.error("doInBackground() " + message1 + "\n      " + message2);
                    }
                }
                else
                {
                    String assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getMetadata().name;
                    
                    LOG.info("\n" + "      =================================================================================================" + 
                             "\n" + "      prepareSimulationRunner() {}",assemblyName);
                    
                    // TODO reset SimulationRunPanel simulation start and stop time to match graph metadata if new Assembly found
                    
                    // in case of rewind/rerun, get SimulationRunPanel startTime, stopTime, number of replications
                    double  priorStartTime                = ViskitGlobals.instance().getSimulationRunPanel().getStartTime();
                    double  priorStopTime                 = ViskitGlobals.instance().getSimulationRunPanel().getStopTime();
                    int     priorNumberReplications       = ViskitGlobals.instance().getSimulationRunPanel().getNumberReplications();
                    int     priorVerboseReplicationNumber = ViskitGlobals.instance().getSimulationRunPanel().getVerboseReplicationNumber();
                    boolean priorVerboseOutput            = ViskitGlobals.instance().getSimulationRunPanel().getVerboseOutput();
                    
                    // Ensure a cleared Simulation panel upon every Assembly compile
                    runnerSimulationRunInterface.resetSimulationRunPanel();
                    
                    if (priorStartTime != -1.0)
                        ViskitGlobals.instance().getSimulationRunPanel().setStartTime(priorStartTime);
                    if (priorStopTime != -1.0)
                        ViskitGlobals.instance().getSimulationRunPanel().setStopTime(priorStopTime);
                    if (priorNumberReplications != 0)
                    {
                        ViskitGlobals.instance().getSimulationRunPanel().setNumberReplications(priorNumberReplications);
                    }
                    if (priorVerboseReplicationNumber != -1)
                        ViskitGlobals.instance().getSimulationRunPanel().setVerboseReplicationNumber(priorVerboseReplicationNumber);
                    ViskitGlobals.instance().getSimulationRunPanel().setVerboseOutput(priorVerboseOutput);

                    // Ensure any changes to the Assembly Properties dialog get saved
                    save();
        
                    String  consoleName = SimulationRunPanel.SIMULATION_RUN_PANEL_TITLE;
                    if (!assemblyName.isBlank() && assemblyName.toLowerCase().contains("assembly"))
                         consoleName += " for " + assemblyName;
                    else if (!assemblyName.isBlank())
                         consoleName += " for Assembly " + assemblyName;
                    (ViskitGlobals.instance().getAssemblyEditorViewFrame()).setTitle(assemblyName);
                    ViskitGlobals.instance().getSimulationRunPanel().setTitle(consoleName);
                    ViskitGlobals.instance().getMainFrame().getSimulationRunTabbedPane().setTitleAt(TAB1_LOCALRUN_INDEX, 
                            assemblyName); // "Simulation Run for " + 
                    
                    // Initializes a fresh class loader
                    runnerSimulationRunInterface.exec(execStringArray);

                    // reset
                    execStringArray = null;

                    ViskitGlobals.instance().getSimulationRunPanel().setHasLoadedAssembly(true);
                    // pretty awkward, need cleaner invocation
                    ViskitGlobals.instance().getMainFrame().internalAssemblyRunner.vcrButtonPressSimulationStateDisplayUpdate(SimulationState.READY); // initialize
                    announceReadyToCommenceSimulationRun(assemblyName); // initialization complete
                }
                
                // TODO what else?
                return null; // doInBackground complete
            }

            @Override
            protected void done() // prepareSimulationRunner() SwingWorker
            {
                try {
                    // Wait for the compile, save and Assembly preparations to finish
                    get();
                } 
                catch (InterruptedException | ExecutionException e) 
                {
                    // look for compilation errors  produced from compileJavaClassFromString()
                    LOG.error("Compilation error in autogenerated source: {}", e);
                    e.printStackTrace();
                }
                finally {
                    (ViskitGlobals.instance().getAssemblyEditorViewFrame()).prepareAssemblyForSimulationRunButton.setEnabled(true);
                    if (mutex > 0)
                        mutex--;
                }
            }
        };
        prepareSimulationRunnerSwingWorker.execute();
    }

    private void announceReadyToCommenceSimulationRun(String newAssemblyName)
    {
        ViskitGlobals.instance().getMainFrame().initializeSimulationRunPanelOutputStreamTA();
        
        LOG.info("Ready to Commence Simulation Run.\n      planned numberOfReplications={}", 
                ViskitGlobals.instance().getSimulationRunPanel().getNumberReplications());
        
        String message =
                "<html><body>" +
                "<p align='center'>" + newAssemblyName;
        if (!newAssemblyName.toLowerCase().contains("simulation"))
            message += " simulation";
        message +=
                " is ready to run!</p><br />" +
                "<p align='center'>Check replication settings at left, then press <b>Run button</b> to begin.</p><br />" +
                "<p align='center'>Multiple simulation replications provide data for your draft Analyst Report.</p><br /></body></html>";
                
        ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Ready to Commence Simulation Run", message);
    }

    public static final int EXEC_TARGET_CLASS_NAME = 0;
    public static final int EXEC_VERBOSE_SWITCH    = 1;
    public static final int EXEC_STOPTIME_SWITCH   = 2;;

    /** Prepare for running the loaded assembly file from java source.
     * Maintain the above statics indices in order to match the order used.
     * @param className the name of the Assembly file to compile
     * @return a parameter array
     */
    private String[] buildExecStringArray(String className)
    {
        if (((AssemblyModel) getModel()) == null)
            LOG.error("buildExecStringArray(" + className + ") (AssemblyModel) getModel() is missing");
        
        List<String> v = new ArrayList<>();

        v.add(className);                                             // 0
        v.add(String.valueOf(((AssemblyModel) getModel()).getMetadata().verbose)); // 1
        v.add(                 ((AssemblyModel) getModel()).getMetadata().stopTime); // 2

        String[] ra = new String[v.size()];
        return v.toArray(ra);
    }
    private String imageSaveCountString = "";
    private int    imageSaveCountInt    = -1;
    
    /** method name for reflection use */
    public static final String METHOD_captureWindow = "captureWindow";

    @Override
    public void captureWindow()
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        
        if (assemblyViewFrame == null)
            assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();
        if (assemblyViewFrame.getNumberAssembliesLoaded() == 0)
        {
            String message = "First load an Assembly before capturing an image";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "No Assembly is loaded", message);
            return;
        }
        if (assemblyModel == null)
            assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        String imageFileName = "AssemblyScreenCapture";
        if ((assemblyModel != null) && (assemblyModel.getCurrentFile() != null))
        {
            imageFileName = assemblyModel.getCurrentFile().getName();
        }
        else
        {
            LOG.error("captureWindow() failed to find assemblyModel");
            return;
        }
        imageFileName = imageFileName + imageSaveCountString + ".png";
        File assemblyScreenCaptureFile = ViskitGlobals.instance().getAssemblyEditorViewFrame().saveFileAsk(imageFileName, true, 
                "Save Image As");
        if (assemblyScreenCaptureFile == null) {
            LOG.error("captureWindow() assemblyScreenCaptureFile is null");
            return;
        }

        final Timer captureWindowTimer = new Timer(100, new ImageCaptureTimerCallback(assemblyScreenCaptureFile, true));
        captureWindowTimer.setRepeats(false);
        captureWindowTimer.start();

        imageSaveCountString = "" + (++imageSaveCountInt);
        
        try
        {
            if (assemblyScreenCaptureFile.exists() && !assemblyScreenCaptureFile.isDirectory())
            {
                Desktop.getDesktop().open(assemblyScreenCaptureFile); // display image
                // also open directory to facilitate copying, editing
                Desktop.getDesktop().open(assemblyScreenCaptureFile.getParentFile());
            }
        }
        catch (IOException e)
        {
            LOG.error("captureWindow() unable to display ()", assemblyScreenCaptureFile);
        }
    }

    /** 
     * Provides an automatic capture of the currently loaded Assembly and stores
     * it to a specified location for inclusion in the generated Analyst Report
     *@param assemblyImageFile assemblyImage an image file to write the .png
     */
    public void captureAssemblyImage(File assemblyImageFile)
    {
        ViskitGlobals.instance().selectAssemblyEditorTab();
        // Don't displayCapture an extra frame while taking snapshots
        final Timer captureAssemblyImageTimer = new Timer(100, new ImageCaptureTimerCallback(assemblyImageFile, false));
        captureAssemblyImageTimer.setRepeats(false);
        captureAssemblyImageTimer.start();
    }

    public boolean isCloseAll() {
        return closeAll;
    }

    public void setCloseAll(boolean closeAll) {
        this.closeAll = closeAll;
    }

    class ImageCaptureTimerCallback implements ActionListener 
    {
        File    imageFile;
        boolean displayCapture;

        /**
         * Constructor for this timerCallBack
         * @param imageFile the file to write an image to
         * @param displayCapture if true, displayCapture the image
         */
        ImageCaptureTimerCallback(File imageFile, boolean displayCapture)
        {
            if (imageFile == null)
            {
                LOG.error("ImageCaptureTimerCallback constructor: (imageFile is null, no capture performed");
                return;
            }
            this.imageFile      = imageFile;
            this.displayCapture = displayCapture;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) 
        {
            if (assemblyViewFrame == null)
                assemblyViewFrame = ViskitGlobals.instance().getAssemblyEditorViewFrame();

            // Get only the jgraph part
            Component component = assemblyViewFrame.getCurrentJgraphComponent();
            if (component == null) {return;}
            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
            }
            Rectangle rectangle = component.getBounds();
            Image image = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_3BYTE_BGR);
            // TODO how to crop empty space?  perhaps alternative is to save image when constructed?

            // Tell the jgraph component to draw into memory
            component.paint(image.getGraphics());

            try {
                ImageIO.write((RenderedImage)image, "png", imageFile);
            } 
            catch (IOException e) {
                LOG.error(e);
            }

            // displayCapture a scaled version
            if (displayCapture)
            {
                final JFrame imageCaptureFrame = new JFrame("Saved as " + imageFile.getName());
                Icon imageIcon = new ImageIcon(image);
                JLabel jLabel = new JLabel(imageIcon);
                imageCaptureFrame.getContentPane().setLayout(new BorderLayout());
                imageCaptureFrame.getContentPane().add(jLabel, BorderLayout.CENTER);
                imageCaptureFrame.pack();
                imageCaptureFrame.setLocationRelativeTo((Component) getView());

                Runnable runnableImageDisplay = () -> {
                    imageCaptureFrame.setVisible(true);
                };
                SwingUtilities.invokeLater(runnableImageDisplay);
            }
        }
    }

    /** Override the default SimulationRunInterface
     *
     * @param plug the SimulationRunInterface to set
     */
    public void setAssemblyRunner(SimulationRunInterface plug) {
        runnerSimulationRunInterface = plug;
    }

    /** Opens each Event Graph associated with this Assembly
     *
     * @param f the Assembly File to open EventGraphs for (not used)
     */
    private void openEventGraphs(File f) 
    {
        File tempFile = f;
        try {
            List<File> eventGraphFiles = EventGraphCache.instance().getEventGraphFilesList();
            for (File file : eventGraphFiles) 
            {
                tempFile = file;

                // _doOpenEventGraph checks if a tab is already opened
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController())._doOpenEventGraph(file);
            }
        } 
        catch (Exception ex) 
        {
            LOG.error("Opening EventGraph file: {} caused error: {}", tempFile, ex);
            ViskitGlobals.instance().messageUser(JOptionPane.WARNING_MESSAGE,
                    "EventGraph Opening Error",
                    "EventGraph file: " + tempFile + "\nencountered error: " + ex + " while loading."
                    );
            ex.printStackTrace(); // debug usually needed
            closeAll();
        }
    }

    /** Recent open file support */
    private static final int RECENTLISTSIZE = 15;
    private final Set<String> recentAssemblyFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);
    private final Set<String> recentProjectFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);

    /**
     * If passed file is in the list, move it to the top. Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an assembly file to add to the list
     */
    private void adjustRecentAssemblySet(File file) {
        String f;
        for (Iterator<String> itr = recentAssemblyFileSet.iterator(); itr.hasNext();) {
            f = itr.next();
            if (file.getPath().equals(f)) {
                itr.remove();
                break;
            }
        }

        if (!recentAssemblyFileSet.contains(file.getPath()))
            recentAssemblyFileSet.add(file.getPath()); // to the top

        saveAssemblyHistoryXML(recentAssemblyFileSet);
        notifyRecentAssemblyFileListeners();
    }

    /**
     * If passed file is in the list, move it to the top. Else insert it;
     * Trim to RECENTLISTSIZE
     * @param currentProjectDirectory a project to add to the list
     */
    private void adjustRecentProjectSet(File currentProjectDirectory) 
    {
        String listedProjectFile;
        // First remove the project name if it s there...
        for (Iterator<String> itr = recentProjectFileSet.iterator(); itr.hasNext();) {
            listedProjectFile = itr.next();
            if (currentProjectDirectory.getPath().equals(listedProjectFile)) {
                itr.remove();
                break;
            }
        }
        // thenadd it back to the top
        if (!recentProjectFileSet.contains(currentProjectDirectory.getPath()))
             recentProjectFileSet.add(currentProjectDirectory.getPath()); 

        saveProjectHistoryXML(recentProjectFileSet);
        notifyRecentProjectFileListeners();
    }

    private List<String> openAssembliesList;

    @SuppressWarnings("unchecked")
    private void recordAssemblyFiles() // TODO check
    {
        openAssembliesList = new ArrayList<>(4);
        List<Object> valueList = historyConfiguration.getList(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "[@value]");
        LOG.debug("recordAssemblyFiles() valueAr size is: {}", valueList.size());
        int count = 0;
        String op;
        String assemblyFileName;
        for (Object nextValue : valueList) 
        {
            assemblyFileName = (String) nextValue;
            if (recentAssemblyFileSet.add(assemblyFileName)) 
            {
                op = historyConfiguration.getString(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "(" + count + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true")))
                    openAssembliesList.add(assemblyFileName);

                notifyRecentAssemblyFileListeners();
            }
            count++;
        }
    }

    @SuppressWarnings("unchecked")
    private void recordProjectFiles() {
        List<Object> valueAr = historyConfiguration.getList(ViskitUserConfiguration.PROJECT_HISTORY_KEY + "[@value]");
        LOG.debug("recordProjectFiles valueAr size is: {}", valueAr.size());
        for (Object value : valueAr)
            adjustRecentProjectSet(new File((String) value));
    }

    private void saveAssemblyHistoryXML(Set<String> recentFiles) {
        historyConfiguration.clearTree(ViskitUserConfiguration.RECENT_ASSEMBLY_CLEAR_KEY);
        int idx = 0;

        // The value's path is already delimited with "/"
        for (String value : recentFiles) {
            historyConfiguration.setProperty(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@value]", value); // set relative path if available
            idx++;
        }
        historyConfiguration.getDocument().normalize();
    }

    /** Always keep our project history until a user clears it manually
     *
     * @param recentFiles a Set of recently opened projects
     */
    private void saveProjectHistoryXML(Set<String> recentFiles) {
        int idx = 0;
        for (String value : recentFiles) {
            historyConfiguration.setProperty(ViskitUserConfiguration.PROJECT_HISTORY_KEY + "(" + idx + ")[@value]", value); // set relative path if available
            idx++;
        }
        historyConfiguration.getDocument().normalize();
    }

    @Override
    public void clearRecentAssemblyFileList() {
        recentAssemblyFileSet.clear();
        saveAssemblyHistoryXML(recentAssemblyFileSet);
        notifyRecentAssemblyFileListeners();
    }

    @Override
    public Set<String> getRecentAssemblyFileSet() {
        return getRecentAssemblyFileSet(false);
    }

    private Set<String> getRecentAssemblyFileSet(boolean refresh) {
        if (refresh || recentAssemblyFileSet == null)
            recordAssemblyFiles();

        return recentAssemblyFileSet;
    }

    @Override
    public void clearRecentProjectFileSet() {
        recentProjectFileSet.clear();
        saveProjectHistoryXML(recentProjectFileSet);
        notifyRecentProjectFileListeners();
    }

    @Override
    public Set<String> getRecentProjectFileSet() {
        return getRecentProjectFileSet(false);
    }

    private Set<String> getRecentProjectFileSet(boolean refresh) {
        if (refresh || recentProjectFileSet == null)
            recordProjectFiles();

        return recentProjectFileSet;
    }

    XMLConfiguration historyConfiguration;

    private void initializeConfiguration() {
        try {
            historyConfiguration = ViskitUserConfiguration.instance().getViskitAppConfiguration();
        } 
        catch (Exception e) {
            LOG.error("Error loading history file: {}", e.getMessage());
            LOG.warn("Recent file saving disabled");
            historyConfiguration = null;
        }
    }

    /**
     * Sets the Analyst report panel
     * @param tabbedPane our Analyst report panel parent
     * @param index the index to retrieve the Analyst report panel
     */
    @Override
    public void setMainTabbedPane(JComponent tabbedPane, int index) {
        this.mainTabbedPane = (JTabbedPane) tabbedPane;
        mainTabbedPaneIndex = index;
    }
    public void makeTopPaneAssemblyTabActive() // TODO no longer needed?
    {
        mainTabbedPane.setSelectedIndex(mainTabbedPaneIndex);
    }
}