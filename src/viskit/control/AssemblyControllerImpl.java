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
import viskit.xsd.translator.eventgraph.SimkitXML2Java;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;
import static viskit.view.MainFrame.TAB1_LOCALRUN_INDEX;
import viskit.assembly.SimulationRunInterface;
import viskit.view.SimulationRunPanel;

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

    /** Creates a new instance of AssemblyController */
    public AssemblyControllerImpl() {
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
    private void compileAssembly(String assemblyPath) {
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

        if (initialAssemblyFilePath != null && !initialAssemblyFilePath.isBlank() && !initialAssemblyFilePath.contains("$")) { // Check for $ makes sure that a property key isn't being used
            LOG.debug("Loading initial file: {}", initialAssemblyFilePath);                             // pointing to a assembly path isn't commented
                                                                                                         // out
            // Switch to the project that this Assembly file is located in if paths do not coincide
            if (!initialAssemblyFilePath.contains(projectDirectory.getPath())) 
            {
                doProjectCleanup();
                projectDirectory = new File(initialAssemblyFilePath).getParentFile().getParentFile().getParentFile();
                openProject(projectDirectory); // calls EventGraphViewFrame setTitleProjectName

                // Add a new project EventGraphs for LEGO tree inclusion of our SimEntities
                ViskitUserPreferencesDialog.RebuildLEGOTreePanelTask t = new ViskitUserPreferencesDialog.RebuildLEGOTreePanelTask();
                t.execute();
            }
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
//      (ViskitGlobals.instance().getAssemblyViewFrame()).setTitleProjectName(); // unneeded
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

    private boolean checkSaveIfDirty() 
    {
        if (localDirty) 
        {
            StringBuilder sb = new StringBuilder("<html><p align='center'>Execution parameters have been modified.<br>(");

            for (Iterator<OpenAssembly.AssemblyChangeListener> itr = isLocalDirty.iterator(); itr.hasNext();) {
                sb.append(itr.next().getHandle());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // last comma-space
            sb.append(")<br>Choose yes if you want to stop this operation, then manually select<br>the indicated tab(s) to ");
            sb.append("save the execution parameters.");

            int yn = (ViskitGlobals.instance().getMainFrame().genericAsk2Buttons("Question", sb.toString(), "Stop and let me save",
                    "Ignore my execution parameter changes"));
            // n == -1 if dialog was just closed
            //   ==  0 for first option
            //   ==  1 for second option

            // be conservative, stop for first 2 choices
            if (yn != 1) {
                return false;
            }
        }
        boolean returnValue = true;
        AssemblyModelImpl assemblyModel = (AssemblyModelImpl) getModel();
        if (assemblyModel != null) {
            if (assemblyModel.isDirty()) {
                return askToSaveAndContinue(); // blocks
            }
        }
        return returnValue;  // proceed
    }

    @Override
    public void showViskitUserPreferences()
    {
        boolean modified; // TODO can we do anything with this notification?
        modified = ViskitUserPreferencesDialog.showDialog(ViskitGlobals.instance().getMainFrame());
    }

    @Override
    public void open()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        File[] filesArray = ViskitGlobals.instance().getAssemblyViewFrame().openFilesAsk();
        if (filesArray == null)
            return;

        for (File file : filesArray) {
            if (file != null)
                _doOpen(file);
        }
    }

    private void _doOpen(File file) {
        if (!file.exists())
            return;

        AssemblyViewFrame assemblyViewFrame = ViskitGlobals.instance().getAssemblyViewFrame();
        AssemblyModelImpl assemblyModel = new AssemblyModelImpl(this);
        assemblyModel.initialize();
        assemblyViewFrame.addTab(assemblyModel);

        // these may initialize to null on startup, check
        // before doing any openAlready lookups
        AssemblyModel[] assemblyModelOpenAlreadyArray = null;
        if (assemblyViewFrame != null)
            assemblyModelOpenAlreadyArray = assemblyViewFrame.getOpenModels();

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
            initOpenAssemblyWatch(file, assemblyModel.getJaxbRoot());
            openEventGraphs(file);
        } 
        else 
        {
            assemblyViewFrame.deleteTab(assemblyModel);
        }

        resetRedoUndoStatus();
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {
        AssemblyViewFrame assemblyViewFrame = ViskitGlobals.instance().getAssemblyViewFrame();

        if (assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper() != null) {
            ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) assemblyViewFrame.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Assembly file opened as "open" in the app config file */
    private void markAssemblyFilesOpened() {

        // Mark every vAMod opened as "open"
        AssemblyModel[] openAlready = ViskitGlobals.instance().getAssemblyViewFrame().getOpenModels();
        for (AssemblyModel assemblyModel : openAlready) {
            if (assemblyModel.getCurrentFile() != null)
                markAssemblyConfigurationOpen(assemblyModel.getCurrentFile());
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
    public void initOpenAssemblyWatch(File f, SimkitAssembly jaxbroot) {
        OpenAssembly.inst().setFile(f, jaxbroot);
    }

    /** @return the listener for this AssemblyControllerImpl */
    @Override
    public OpenAssembly.AssemblyChangeListener getAssemblyChangeListener() {
        return assemblyChangeListener;
    }
    private boolean localDirty = false;
    private Set<OpenAssembly.AssemblyChangeListener> isLocalDirty = new HashSet<>();
    OpenAssembly.AssemblyChangeListener assemblyChangeListener = new OpenAssembly.AssemblyChangeListener() {

        @Override
        public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
            switch (action) {
                case JAXB_CHANGED:
                    isLocalDirty.remove(source);
                    if (isLocalDirty.isEmpty())
                        localDirty = false;

                    ((AssemblyModel) getModel()).setDirty(true);
                    break;

                case NEW_ASSEMBLY:
                    isLocalDirty.clear();
                    localDirty = false;
                    break;

                case PARAM_LOCALLY_EDITED:
                    // This gets hit when you type something in the last three tabs
                    isLocalDirty.add(source);
                    localDirty = true;
                    break;

                case CLOSE_ASSEMBLY:

                    // Close any currently open Event Graphs because we don't yet know which ones
                    // to keep open until iterating through each remaining vAMod

                    ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();

                    AssemblyModel assemblyModel = (AssemblyModel) getModel();
                    markAssemblyConfigurationClosed(assemblyModel.getCurrentFile());

                    AssemblyView view = (AssemblyView) getView();
                    view.deleteTab(assemblyModel);

                    // NOTE: This doesn't work quite right. If no Assembly is open,
                    // then any non-associated Event Graphs that were also open will
                    // annoyingly close from the closeAll call above. We are
                    // using an open Event Graph cache system that relies on parsing an
                    // Assembly file to find its associated Event Graphs to open
                    if (!isCloseAll()) {
                        AssemblyModel[] modAr = view.getOpenModels();
                        for (AssemblyModel mod : modAr) {
                            if (!mod.equals(assemblyModel))
                                openEventGraphs(mod.getCurrentFile());
                        }
                    }

                    break;

                default:
                    LOG.warn("Program error AssemblyController.assemblyChanged");
            }
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
    public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
        assemblyChangeListener.assemblyChanged(action, source, param);
    }

    @Override
    public void addAssemblyFileListener(OpenAssembly.AssemblyChangeListener lis) {
        OpenAssembly.inst().addListener(lis);
    }

    @Override
    public void removeAssemblyFileListener(OpenAssembly.AssemblyChangeListener lis) {
        OpenAssembly.inst().removeListener(lis);
    }

    Set<MvcRecentFileListener> recentAssemblyListeners = new HashSet<>();

    @Override
    public void addRecentAssemblyFileSetListener(MvcRecentFileListener lis) {
        recentAssemblyListeners.add(lis);
    }

    @Override
    public void removeRecentAssemblyFileSetListener(MvcRecentFileListener lis) {
        recentAssemblyListeners.remove(lis);
    }

    /** Here we are informed of open Event Graphs */

    private void notifyRecentAssemblyFileListeners() {
        for (MvcRecentFileListener lis : recentAssemblyListeners)
            lis.listChanged();
    }

    Set<MvcRecentFileListener> recentProjectListeners = new HashSet<>();

    @Override
    public void addRecentProjectFileSetListener(MvcRecentFileListener lis) {
        recentProjectListeners.add(lis);
    }

    @Override
    public void removeRecentProjectFileSetListener(MvcRecentFileListener lis) {
        recentProjectListeners.remove(lis);
    }

    private void notifyRecentProjectFileListeners() {
        for (MvcRecentFileListener lis : recentProjectListeners) {
            lis.listChanged();
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

    @Override
    public void save()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (assemblyModel.getCurrentFile() == null) {
            saveAs();
        } 
        else {
            assemblyModel.saveModel(assemblyModel.getCurrentFile());
        }
    }

    @Override
    public void saveAs()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        AssemblyModel model = (AssemblyModel) getModel();
        AssemblyView assemblyView = (AssemblyView) getView();
        GraphMetadata graphMetadata = model.getMetadata();

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = assemblyView.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false);

        if (saveFile != null) {

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            graphMetadata.name = n;
            model.changeMetadata(graphMetadata); // might have renamed

            model.saveModel(saveFile);
            assemblyView.setSelectedAssemblyName(graphMetadata.name);
            adjustRecentAssemblySet(saveFile);
            markAssemblyFilesOpened();
        }
    }

    @Override
    public void editGraphMetadata() 
    {
//        AssemblyModel assemblyModel = (AssemblyModel) getModel();
//        if (assemblyModel == null) 
//        {
//            return;
//        }
//        GraphMetadata graphMetadata = assemblyModel.getMetadata();
        
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        
        AssemblyModelImpl assemblyModel = (AssemblyModelImpl) getModel();
//        Model model = (Model) getModel();
        if (assemblyModel == null) 
        {
            LOG.error("editGraphMetadata() failed, (assemblyModel == null)");
            return; // not expected
        }
        GraphMetadata graphMetadata = assemblyModel.getMetadata();
        
        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyViewFrame(), graphMetadata);
        if (modified) {
            ((AssemblyModel) getModel()).changeMetadata(graphMetadata);

            // update title bar for frame
            ViskitGlobals.instance().getAssemblyViewFrame().setSelectedAssemblyName(graphMetadata.name);
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

    private String shortName(String typeName, String prefix, Field intField) {
        String shortname = prefix;
        if (typeName.lastIndexOf('.') != -1) {
            shortname = typeName.substring(typeName.lastIndexOf('.') + 1) + "_";
        }

        // Don't capitalize the first letter
        char[] ca = shortname.toCharArray();
        ca[0] = Character.toLowerCase(ca[0]);
        shortname = new String(ca);

        String retn = shortname;
        try {
            int count = intField.getInt(this);
            // Find a unique name
            AssemblyModel model = (AssemblyModel) getModel();
            do {
                retn = shortname + count++;
            } while (model.nameExists(retn));   // don't force the vAMod to mangle the name
            intField.setInt(this, count);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.error(ex);
        }
        return retn;
    }

    @Override
    public void newProject() 
    {
        if (closeProject()) 
        {
            ViskitGlobals.instance().createProjectWorkingDirectory();

            // For a brand new empty project open a default Event Graph
            File[] eventGraphFiles = ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory().listFiles();
            if (eventGraphFiles.length == 0) {
                ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).newEventGraph();
            }
        }
    }

    @Override
    public void zipProject() {

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            File projectDirectory;
            File projectZipFile;
            File projectZipLogFile;

            @Override
            public Void doInBackground() 
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
                String dateOutput = formatter.format(new Date()); // today, now
                
                projectDirectory  = ViskitGlobals.instance().getViskitProject().getProjectDirectory();
                projectZipFile    = new File(projectDirectory.getParentFile(), 
                                            projectDirectory.getName() + "_" + dateOutput + ".zip");
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
            public void done() {
                try {
                    // Waits for the zip process to finish
                    get();
                    
                    String message = "<html><p align='center'>Viskit project zip file is now ready:</p><br />" + 
                                           "<p align='center'>" + projectZipFile.getAbsolutePath()+ "</p></html>";
                    ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                        "Project zip complete", message);

                    Desktop.getDesktop().open(projectZipFile.getParentFile());
                    
                    // possibly useful for students in class
//                    try {
//                        URL mailtoURL = new URI("mailto:" + ViskitStatics.VISKIT_MAILING_LIST
//                                + "?subject=Viskit%20Project%20Submission%20for%20"
//                                + projectDirectory.getName() + "&body=see%20attachment").toURL();
//                        
//                        String message = "Please navigate to<br/>"
//                                + projectZipFile.getParent()
//                                + "<br/>and email the " + projectZipFile.getName()
//                                + " file to "
//                                + "<b><a href=\"" + mailtoURL.toString() + "\">"
//                                + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
//                                + "<br/><br/>Click the link to open up an email "
//                                + "form, then attach the zip file";
//                        ViskitStatics.showHyperlinkedDialog((Component) getView(), "Viskit Project: " + projectDirectory.getName(), mailtoURL, message, false);
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

    /** 
     * Common method between the AssemblyView and this AssemblyController
     * @return indication of continue or cancel
     */
    public boolean closeProject() 
    {
        boolean projectClosed;
        String title, message;
        if (ViskitGlobals.instance().getViskitProject().isProjectOpen())
        {
            title = "Close Current Project?";
            message = "<html><p align='center'>Are you sure that you want to first close</p><br/>";
            String projectName = ViskitGlobals.instance().getProjectWorkingDirectory().getAbsolutePath();
            if  (projectName.toLowerCase().contains("project"))
                 message += "<p align='center'><i>"         + projectName + "</i></p><br/><p align='center'> as the currently active project?</p><br/>";
            else message += "<p align='center'>Project <i>" + projectName + "</i></p><br/><p align='center'> as the currently active project?</p><br/>";
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            if (returnValue == JOptionPane.YES_OPTION)
            {
                doProjectCleanup();
                projectClosed = true;
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

    @Override
    public void openProject(File projectDirectory)
    {
        ViskitGlobals.instance().setProjectFile(projectDirectory);
        ViskitGlobals.instance().setProjectName(projectDirectory.getName());
        ViskitGlobals.instance().createProjectWorkingDirectory();

        // Add our currently opened project to the recently opened projects list
        adjustRecentProjectSet(ViskitGlobals.instance().getViskitProject().getProjectDirectory());
        
        ViskitGlobals.instance().setTitleProjectName(ViskitGlobals.instance().getProjectName());
        
        runnerSimulationRunInterface.resetSimulationRunPanel();
    }

    @Override
    public void newAssembly()
    {
        // Don't allow a new Assembly to be created if a current project is not open
        if (!ViskitGlobals.instance().getViskitProject().isProjectOpen()) 
        {
            LOG.error("newAssembly() unable to create new assembly if project is not open");
            return;
        }
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();

        GraphMetadata oldGraphMetadata = null;
        AssemblyModel viskitAssemblyModel = (AssemblyModel) getModel();
        if (viskitAssemblyModel != null) {
            oldGraphMetadata = viskitAssemblyModel.getMetadata();
        }

        AssemblyModelImpl assemblyModel = new AssemblyModelImpl(this);
        assemblyModel.initialize();
        assemblyModel.newAssemblyModel(null); // should create new assembly file

        // No vAMod set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ViskitGlobals.instance().getAssemblyViewFrame().addTab(assemblyModel);

        GraphMetadata graphMetadata = new GraphMetadata(assemblyModel);   // build a new one, specific to Assembly
        if (oldGraphMetadata != null) {
            graphMetadata.packageName = oldGraphMetadata.packageName;
        }

        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyViewFrame(), graphMetadata);
        if (modified) 
        {
            ((AssemblyModel) getModel()).changeMetadata(graphMetadata);

            // update title bar
            ViskitGlobals.instance().getAssemblyViewFrame().setSelectedAssemblyName(graphMetadata.name);

            // TODO: Implement this
//            ((AssemblyView)  getView()).setSelectedEventGraphDescription(graphMetadata.description);
        } 
        else 
        {
            ViskitGlobals.instance().getAssemblyViewFrame().deleteTab(assemblyModel);
        }
    }

    /** 
     * A component, e.g., vAMod, wants to say something.
     * @param messageType the type of message, i.e. WARN, ERROR, INFO, QUESTION, etc.
     * @param messageTitle the title of the message in the dialog frame
     * @param messageBody the message to transmit
     */
    @Override
    public void messageUser(int messageType, String messageTitle, String messageBody) // messageType is one of JOptionPane types
    {
        if (ViskitGlobals.instance().getAssemblyViewFrame() != null)
            ViskitGlobals.instance().getMainFrame().genericReport(messageType, messageTitle, messageBody);
    }

    @Override
    public void quit() 
    {
        if (preQuit()) {
            postQuit();
        }
        ViskitGlobals.instance().getMainFrame().quit();
    }

    @Override
    public boolean preQuit() {

        // Check for dirty models before exiting
        AssemblyModel[] assemblyModelArray = ViskitGlobals.instance().getAssemblyViewFrame().getOpenModels();
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
    public void postQuit() {
        ViskitGlobals.instance().quitAssemblyEditor();
    }

    private boolean closeAll = false;

    @Override
    public void closeAll() 
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        AssemblyModel[] assemblyModelArray = ViskitGlobals.instance().getAssemblyViewFrame().getOpenModels();
        for (AssemblyModel assemblyModel : assemblyModelArray) 
        {
            setModel((MvcModel) assemblyModel);
            setCloseAll(true);
            close();
        }
        setCloseAll(false);
    }

    @Override
    public void close()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        if (preClose()) {
            postClose();
        }
    }

    @Override
    public boolean preClose() 
    {
        AssemblyModelImpl assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel(); // (AssemblyModel) getModel();
        if (assemblyModel == null) {
            return false;
        }
        LOG.debug("preClose() close assembly {}", assemblyModel.getCurrentAssemblyModelName());
        if (assemblyModel.isDirty()) {
            return checkSaveIfDirty();
        }
        return true;
    }

    @Override
    public void postClose() 
    {
        LOG.debug("postClose() close assembly {}", OpenAssembly.inst().getName());
        OpenAssembly.inst().doFireActionCloseAssembly();
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
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

    @Override
    public void newEventGraphNode() // menu click
    {
        Object o = ViskitGlobals.instance().getAssemblyViewFrame().getSelectedEventGraph();

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
        messageUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select an Event Graph from the panel on the left.");
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
        Object o = ViskitGlobals.instance().getAssemblyViewFrame().getSelectedPropertyChangeListener();

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
        messageUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select a Property Change Listener from the panel on the left.");
    }

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
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        String message = "Save modified ";
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if ((assemblyModel != null) && (assemblyModel.getMetadata() != null))
             message += assemblyModel.getMetadata().name + ".xml"; 
        if (!message.toLowerCase().contains("assembly"))
             message += " assembly";
        message += "?";
        int yn = (ViskitGlobals.instance().getMainFrame().genericAsk("Save assembly?", message));

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();

                // TODO: Can't remember why this is here after a save?
                if (((AssemblyModel) getModel()).isDirty()) {
                    return false;
                } // we cancelled
                return true;
            case JOptionPane.NO_OPTION:

                // No need to recompile
                if (((AssemblyModel) getModel()).isDirty()) {
                    ((AssemblyModel) getModel()).setDirty(false);
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
    public void newAdapterArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr;
        try {
            oArr = checkLegalForSEListenerArc(oA, oB);
        } 
        catch (Exception e) {
            messageUser(JOptionPane.ERROR_MESSAGE, "Connection error.", "Possible class not found.  All referenced entities must be in a list at left.");
            return;
        }
        if (oArr == null) {
            messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        adapterEdgeEdit(((AssemblyModel) getModel()).newAdapterEdge(shortAdapterName(""), oArr[0], oArr[1]));
    }

    @Override
    public void newSimEventListenerArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForSEListenerArc(oA, oB);

        if (oArr == null) {
            messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        ((AssemblyModel) getModel()).newSimEventListenerEdge(oArr[0], oArr[1]);
    }

    @Override
    public void newPropertyChangeListenerArc(Object[] nodes) {
        // One and only one has to be a prop change listener
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForPropChangeArc(oA, oB);

        if (oArr == null) {
            messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a PropertyChangeListener and PropertyChangeSource combination.");
            return;
        }
        propertyChangeListenerEdgeEdit(((AssemblyModel) getModel()).newPropChangeEdge(oArr[0], oArr[1]));
    }

    AssemblyNode[] checkLegalForSEListenerArc(AssemblyNode a, AssemblyNode b) {
        Class<?> ca = findClass(a);
        Class<?> cb = findClass(b);
        return orderSELSrcAndLis(a, b, ca, cb);
    }

    AssemblyNode[] checkLegalForPropChangeArc(AssemblyNode a, AssemblyNode b) {
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
            modified = ViskitGlobals.instance().getAssemblyViewFrame().doEditPropertyChangeListenerNode(pclNode);
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
            modified = ViskitGlobals.instance().getAssemblyViewFrame().doEditEventGraphNode(evNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changeEvGraphNode(evNode);
            }
        } while (!done);
    }

    @Override
    public void propertyChangeListenerEdgeEdit(PropertyChangeEdge pclEdge) {
        boolean modified = ViskitGlobals.instance().getAssemblyViewFrame().doEditPropertyChangeListenerEdge(pclEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changePclEdge(pclEdge);
        }
    }

    @Override
    public void adapterEdgeEdit(AdapterEdge aEdge) {
        boolean modified = ViskitGlobals.instance().getAssemblyViewFrame().doEditAdapterEdge(aEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeAdapterEdge(aEdge);
        }
    }

    @Override
    public void simEventListenerEdgeEdit(SimEventListenerEdge seEdge) {
        boolean modified = ViskitGlobals.instance().getAssemblyViewFrame().doEditSimEventListenerEdge(seEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeSimEvEdge(seEdge);
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

    @Override
    public void remove()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        if (!selectionVector.isEmpty()) {
            // first ask:
            String s, message = "";
            int nodeCount = 0;  // different message for edge delete
            for (Object o : selectionVector) {
                if (o instanceof AssemblyNode) {
                    nodeCount++;
                    s = o.toString();
                    s = s.replace('\n', ' ');
                    message += ", \n" + s;
                }
            }
            String specialNodeMessage = (nodeCount > 0) ? "\n(All unselected but attached edges will also be removed.)" : "";
            doRemove = ViskitGlobals.instance().getMainFrame().genericAsk("Remove element(s)?", "Confirm remove" + message + "?" + specialNodeMessage) == JOptionPane.YES_OPTION;
            if (doRemove) {
                // do edges first?
                delete();
            }
        }
    }

    @Override
    public void cut() //---------------
    {
        // Not supported
    }

    @Override
    @SuppressWarnings("unchecked")
    public void copy() {
        if (selectionVector.isEmpty()) {
            messageUser(JOptionPane.WARNING_MESSAGE,
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
    private void delete() {
        Vector<Object> v = (Vector<Object>) selectionVector.clone();   // avoid concurrent update
        AssemblyNode en;
        for (Object elem : v) {
            if (elem instanceof AssemblyEdge) {
                removeEdge((AssemblyEdge) elem);
            } else if (elem instanceof EventGraphNode) {
                en = (EventGraphNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deleteEventGraphNode((EventGraphNode) en);
            } else if (elem instanceof PropertyChangeListenerNode) {
                en = (PropertyChangeListenerNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deletePropertyChangeListener((PropertyChangeListenerNode) en);
            }
        }

        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    private void removeEdge(AssemblyEdge e) {
        if (e instanceof AdapterEdge) {
            ((AssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
        } else if (e instanceof PropertyChangeEdge) {
            ((AssemblyModel) getModel()).deletePropChangeEdge((PropertyChangeEdge) e);
        } else if (e instanceof SimEventListenerEdge) {
            ((AssemblyModel) getModel()).deleteSimEventListenerEdge((SimEventListenerEdge) e);
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

    /**
     * Removes the last selected node or edge from the JGraph model
     */
    @Override
    public void undo()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        if (selectionVector.isEmpty())
            return;

        isUndo = true;

        AssemblyViewFrame view = ViskitGlobals.instance().getAssemblyViewFrame();
        Object[] roots = view.getCurrentViskitGraphAssemblyComponentWrapper().getRoots();
        for (Object root : roots) {
            if (root instanceof DefaultGraphCell)
                redoGraphCell = ((DefaultGraphCell) root);
            if (selectionVector.firstElement().equals(redoGraphCell.getUserObject()))
                break;
        }
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache());
        } catch (CannotUndoException ex) {
            LOG.error("Unable to undo: {}", ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /**
     * Replaces the last selected node or edge from the JGraph model
     */
    @Override
    public void redo()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();

        // Recreate the JAXB (XML) bindings since the paste function only does
        // nodes and not edges
        if (redoGraphCell instanceof org.jgraph.graph.Edge) {

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof AdapterEdge) {
                AdapterEdge ed = (AdapterEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoAdapterEdge(ed);
            } else if (redoGraphCell.getUserObject() instanceof PropertyChangeEdge) {
                PropertyChangeEdge ed = (PropertyChangeEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropertyChangeEdge(ed);
            } else {
                SimEventListenerEdge ed = (SimEventListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoSimEvLisEdge(ed);
            }
        } else {

            if (redoGraphCell.getUserObject() instanceof PropertyChangeListenerNode) {
                PropertyChangeListenerNode node = (PropertyChangeListenerNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropertyChangeListener(node);
            } else {
                EventGraphNode node = (EventGraphNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoEventGraph(node);
            }
        }

        AssemblyViewFrame view = ViskitGlobals.instance().getAssemblyViewFrame();
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: {}", ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        AssemblyViewFrame view = ViskitGlobals.instance().getAssemblyViewFrame();
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentViskitGraphAssemblyComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(view.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(view.getCurrentViskitGraphAssemblyComponentWrapper().getGraphLayoutCache()));

        isUndo = false;
    }

    /********************************/
    /* from menu:*/

    @Override
    public void viewXML()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || (assemblyModel == null) || (assemblyModel.getCurrentFile() == null)) 
        {
            LOG.error("viewXML() unable to retrieve assemblyModel");
                    return;
        }
        ViskitGlobals.instance().getAssemblyViewFrame().displayXML(assemblyModel.getCurrentFile());
    }

    private boolean checkSaveForSourceCompile()
    {
        AssemblyModel assemblyModel = (AssemblyModel) getModel();

        // Perhaps a cached file is no longer present in the path
        if (assemblyModel == null) {return false;}
        if (assemblyModel.isDirty() || assemblyModel.getCurrentFile() == null) {
            int ret = ViskitGlobals.instance().getMainFrame().genericAskYesNo("Confirm", "The model will be saved.\nContinue?");
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            this.saveAs();
        }
        return true;
    }

    @Override
    public void generateJavaSource()
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        String source = produceJavaAssemblyClass();
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (source != null && !source.isEmpty()) {
            String className = assemblyModel.getMetadata().packageName + "." + assemblyModel.getMetadata().name;
            ViskitGlobals.instance().getAssemblyViewFrame().showAndSaveSource(className, source, assemblyModel.getCurrentFile().getName());
        }
    }

    private String produceJavaAssemblyClass() 
    {
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || assemblyModel.getCurrentFile() == null) {
            return null;
        }
        if (assemblyModel.getCurrentFile().exists())
        {
            if  (assemblyModel.getMetadata() != null)
                 LOG.info("Preparing assembly " + assemblyModel.getMetadata().name + "using Metadata file");
            else LOG.error("produceJavaAssemblyClass() assemblyModel modelMetadata is null");
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
     * @param f the Assembly file to produce source from
     * @return a string of Assembly source code
     */
    public String buildJavaAssemblySource(File f) 
    {
        String assemblySource = null;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = new XMLValidationTool(f.getPath(), XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA);

        if (!xmlValidationTool.isValidXML()) 
        {
            // TODO: implement a Dialog pointing to the validationErrors.LOG
            LOG.error("buildJavaAssemblySource{} found invalid XML!\n      " + f.getAbsolutePath());
            return assemblySource;
        } 
        else {
            LOG.info("buildJavaAssemblySource{} found valid XML\n      " + f.getAbsolutePath());
        }

        SimkitAssemblyXML2Java x2j;
        try {
            x2j = new SimkitAssemblyXML2Java(f);
            x2j.unmarshal();
            assemblySource = x2j.translate();
        } 
        catch (FileNotFoundException e) {
            LOG.error(e);
            assemblySource = "";
        }
        return assemblySource;
    }

    // NOTE: above are routines to operate on current assembly

    /**
     * Build the actual source code from the Event Graph XML after a successful
     * XML validation
     *
     * @param simkitXML2Java the Event Graph initialized translator to produce source with
     * @return a string of Event Graph source code
     */
    public String buildJavaEventGraphSource(SimkitXML2Java simkitXML2Java)
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
            LOG.error("buildJavaEventGraphSource() error building Java from {}: {}, erroneous event-graph xml found", simkitXML2Java.getFileBaseName(), e.getMessage());
        }
        return eventGraphSource;
    }

    /** Create and test compile our EventGraphs and Assemblies from XML
     *
     * @param sourceCode the translated source either from SimkitXML2Java, or SimkitAssemblyXML2Java
     * @return a reference to a successfully compiled *.class file or null if
     * a compile failure occurred
     */
    public static File compileJavaClassFromString(String sourceCode) 
    {
        String baseName;

        // Find the package subdirectory
        Pattern patttern = Pattern.compile("package.+;");
        Matcher matcher = patttern.matcher(sourceCode);
        boolean fnd = matcher.find();

        String packagePath = "";
        String packageName = "";
        if (fnd) {
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
            File sourcePackagePath = new File(viskitProject.getSrcDirectory(), packagePath);
            if (!sourcePackagePath.isDirectory())
                 sourcePackagePath.mkdirs(); // include intermediate subdirectories
            File javaSourceFile = new File(sourcePackagePath, baseName + ".java");
            javaSourceFile.createNewFile();

            sourceCodeFileWriter = new FileWriter(javaSourceFile);
            sourceCodeFileWriter.write(sourceCode);

            // An error stream to write additional error info out to
            OutputStream baosOutputStream = new ByteArrayOutputStream();
            Compiler.setOutPutStream(baosOutputStream);

            File classesDirectory = viskitProject.getClassesDirectory();
            
            String relativePathToCompiledClass = packagePath + baseName + ".class";
            File compiledClassFile = new File(classesDirectory.getAbsolutePath() + "/" + relativePathToCompiledClass);

            LOG.info("compileJavaClassFromString() compiling file:\n      " + javaSourceFile.getAbsolutePath() + " to\n      " +
                         compiledClassFile.getAbsolutePath());

            // This will create a class/package to place the .class file
            String diagnostic = Compiler.invoke(packageName, baseName, sourceCode);
            compileSuccess = diagnostic.equals(Compiler.COMPILE_SUCCESS_MESSAGE);            

            LOG.debug("compileJavaClassFromString() compiledClassFile=\n      " + compiledClassFile.getAbsolutePath());
            if (compileSuccess) 
            {
                LOG.info("compileJavaClassFromString() " + diagnostic + "\n");
                return compiledClassFile;
            } 
            else
            {
                LOG.error("compileJavaClassFromString() " + diagnostic);
                if (!baosOutputStream.toString().isEmpty())
                    LOG.error("compileJavaClassFromString() " + baosOutputStream.toString());
            }

        } 
        catch (IOException ioe) {
            LOG.error("compileJavaClassFromString() exception" + ioe);
        } 
        finally {
            try {
                if (sourceCodeFileWriter != null)
                    sourceCodeFileWriter.close();
            } 
            catch (IOException e) {}
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
    public PackageAndFile createTemporaryEventGraphClass(File xmlFile) {
        PackageAndFile paf = null;
        try {
            SimkitXML2Java x2j = new SimkitXML2Java(xmlFile);
            x2j.unmarshal();

            boolean isEventGraph = x2j.getUnMarshalledObject() instanceof viskit.xsd.bindings.eventgraph.SimEntity;
            if (!isEventGraph) {
                LOG.debug("Is an Assembly: {}", !isEventGraph);
                return paf;
            }

            String sourceString = buildJavaEventGraphSource(x2j);

            /* We may have forgotten a parameter required for a super class */
            if (sourceString == null) {
                String message = xmlFile + " did not translate to source code.\n" +
                        "Manually compile to determine cause";
                LOG.error(message);
                messageUser(JOptionPane.ERROR_MESSAGE, "Source code translation error", message);
                return null;
            }

            paf = compileJavaClassAndSetPackage(sourceString);

        } catch (FileNotFoundException e) {
            LOG.error("Error creating Java class file from {}: {}\n", xmlFile , e.getMessage());
            FileBasedClassManager.instance().addCacheMiss(xmlFile);
        }
        return paf;
    }

    /** Path for Event Graph and Assembly compilation
     *
     * @param source the raw source to write to file
     * @return a package and file pair
     */
    private PackageAndFile compileJavaClassAndSetPackage(String source) {
        String pkg = null;
        if (source != null && !source.isEmpty()) {
            Pattern p = Pattern.compile("package.*;");
            Matcher m = p.matcher(source);
            if (m.find()) {
                String nuts = m.group();
                if (nuts.endsWith(";"))
                    nuts = nuts.substring(0, nuts.length() - 1);

                String[] sa = nuts.split("\\s");
                pkg = sa[1];
            }
            File f = compileJavaClassFromString(source);
            if (f != null)
                return new PackageAndFile(pkg, f);
        }
        return null;
    }

    // From menu
    @Override
    public void export2grid() {
        AssemblyModel model = (AssemblyModel) getModel();
        File tempFile;
        try {
            tempFile = TempFileManager.createTempFile("ViskitAssembly", ".xml");
        } catch (IOException e) {
            messageUser(JOptionPane.ERROR_MESSAGE, "File System Error", e.getMessage());
            return;
        }
        model.saveModel(tempFile);
    //todo switch to DOE
    }
    private String[] execStringArray;

    // Known modelPath for Assembly compilation
    @Override
    public void prepareAssemblySimulationRun() 
    {
        String src = produceJavaAssemblyClass(); // asks to save

        PackageAndFile paf = compileJavaClassAndSetPackage(src);
        if (paf != null) {
            File f = paf.file;
            String className = f.getName().substring(0, f.getName().indexOf('.'));
            className = paf.packageName + "." + className;

            execStringArray = buildExecStringArray(className);
        } 
        else {
            execStringArray = new String[0];
        }
    }

    @Override
    public void prepareSimulationRunner()
    {
        // Prevent multiple pushes of the initialize simulation run button
        mutex++;
        if (mutex > 1) {
            return;
        }

        // Prevent double clicking which will cause potential ClassLoader issues
        Runnable r = () -> {
            (ViskitGlobals.instance().getAssemblyViewFrame()).prepareAssemblyForSimulationRunButton.setEnabled(false);
        };

        try {
            SwingUtilities.invokeLater(r);
        } 
        catch (Exception e) {
            LOG.error("prepareSimulationRunner() SwingUtilities.invokeLater(" + r.toString() + ") exception: " + e.getMessage());
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            public Void doInBackground() // prepareSimulationRunner()
            {
                // Compile and prep the execStrings
                prepareAssemblySimulationRun();

                if (execStringArray == null)
                {
//                  if (ViskitGlobals.instance().getActiveAssemblyModel() == null)
                    if (!ViskitGlobals.instance().getAssemblyViewFrame().hasAssembliesLoaded())
                    {
                        messageUser(JOptionPane.WARNING_MESSAGE,
                            "Assembly File Not Loaded",
                            "Please load an Assembly file");
                        LOG.error(" doInBackground() " +  "Assembly File Not Loaded, please load an Assembly file");
                    } 
                    else
                    {
                        String message1 = "Please inspect autogenerated source to debug the source error,";
                        String message2 = "then correct the assembly XML for correct proper compilation";
                        messageUser(JOptionPane.WARNING_MESSAGE, "Assembly source generation/compilation error",
                                "<html><p align-'center'>" + message1 + "</p> <p align-'center'>" + message2 + "</p>");
                        LOG.error("doInBackground() " + message1 + "\n      " + message2);
                    }
                }
                else
                {
                    // Ensure a cleared Simulation panel upon every Assembly compile
                    runnerSimulationRunInterface.resetSimulationRunPanel();

                    // Ensure any changes to the Assembly Properties dialog get saved
                    save();
        
                    String assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getMetadata().name;
                    String  consoleName = SimulationRunPanel.SIMULATION_RUN_PANEL_TITLE;
                    if (!assemblyName.isBlank() && assemblyName.toLowerCase().contains("assembly"))
                         consoleName += " for " + assemblyName;
                    else if (!assemblyName.isBlank())
                         consoleName += " for Assembly " + assemblyName;
                    (ViskitGlobals.instance().getAssemblyViewFrame()).setTitle(assemblyName);
                    ViskitGlobals.instance().getSimulationRunPanel().setTitle(consoleName);
                    ViskitGlobals.instance().getMainFrame().getSimulationRunTabbedPane().setTitleAt(TAB1_LOCALRUN_INDEX, 
                            assemblyName); // "Simulation Run for " + 
                    
                    // Initializes a fresh class loader
                    runnerSimulationRunInterface.exec(execStringArray);

                    // reset
                    execStringArray = null;
                    
                    // provide user guidance on first initialization
                    if (!ViskitGlobals.instance().getSimulationRunPanel().hasLoadedAssembly())
                    {
                        announceReadyToCommenceSimulationRun(assemblyName); // initialization complete
                    }
                    ViskitGlobals.instance().getSimulationRunPanel().setHasLoadedAssembly(true);
                }
                
                // TODO what else?
                return null; // doInBackground complete
            }

            @Override
            protected void done() 
            {
                try {
                    // Wait for the compile, save and Assembly preparations to finish
                    get();
                } 
                catch (InterruptedException | ExecutionException e) {
                    LOG.error(e);
//                    e.printStackTrace();
                }
                finally {
                    (ViskitGlobals.instance().getAssemblyViewFrame()).prepareAssemblyForSimulationRunButton.setEnabled(true);
                    mutex--;
                }
            }
        };
        worker.execute();
    }

    private void announceReadyToCommenceSimulationRun(String newAssemblyName)
    {
        String message =
                "<html><body>" +
                "<p align='center'>" + newAssemblyName;
        if (!newAssemblyName.toLowerCase().contains("simulation"))
            message += " simulation";
        message +=
                " is ready to run!</p><br />" +
                "<p align='center'>Check the Replication Settings at left, then press Run button to begin.</p><br />" +
                "<p align='center'>Multiple simulation replications provide data for a draft Analyst Report.</p><br /></body></html>";
                
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
        v.add("" + ((AssemblyModel) getModel()).getMetadata().verbose); // 1
        v.add(  ((AssemblyModel) getModel()).getMetadata().stopTime); // 2

        String[] ra = new String[v.size()];
        return v.toArray(ra);
    }
    private String imageSaveCountString = "";
    private int    imageSaveCountInt    = -1;

    @Override
    public void captureWindow() 
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (assemblyModel == null)
            assemblyModel = ViskitGlobals.instance().getActiveAssemblyModel();
        String fileName = "AssemblyScreenCapture";
        if ((assemblyModel != null) && (assemblyModel.getCurrentFile() != null))
        {
            fileName = assemblyModel.getCurrentFile().getName();
        }
        else
        {
            LOG.error("captureWindow() failed to find assemblyModel");
            return;
        }
        fileName = fileName + imageSaveCountString + ".png";
        File assemblyScreenCaptureFile = ViskitGlobals.instance().getAssemblyViewFrame().saveFileAsk(fileName, true);
        if (assemblyScreenCaptureFile == null) {
            LOG.error("captureWindow() assemblyScreenCaptureFile is null");
            return;
        }

        final Timer captureWindowTimer = new Timer(100, new ImageCaptureTimerCallback(assemblyScreenCaptureFile, true));
        captureWindowTimer.setRepeats(false);
        captureWindowTimer.start();

        imageSaveCountString = "" + (++imageSaveCountInt);
    }

    /** 
     * Provides an automatic capture of the currently loaded Assembly and stores
     * it to a specified location for inclusion in the generated Analyst Report
     *@param assemblyImageFile assemblyImage an image file to write the .png
     */
    public void captureAssemblyImage(File assemblyImageFile)
    {
        ViskitGlobals.instance().getMainFrame().selectAssemblyTab();
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
            // create and save the image
            AssemblyViewFrame assemblyViewFrame = ViskitGlobals.instance().getAssemblyViewFrame();

            // Get only the jgraph part
            Component component = assemblyViewFrame.getCurrentJgraphComponent();
            if (component == null) {return;}
            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
            }
            Rectangle rec = component.getBounds();
            Image image = new BufferedImage(rec.width, rec.height, BufferedImage.TYPE_3BYTE_BGR);
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
                final JFrame frame = new JFrame("Saved as " + imageFile.getName());
                Icon imageIcon = new ImageIcon(image);
                JLabel jLabel = new JLabel(imageIcon);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(jLabel, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo((Component) getView());

                Runnable runnableImageDisplay = () -> {
                    frame.setVisible(true);
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
    private void openEventGraphs(File f) {
        File tempFile = f;
        try {
            List<File> eventGraphFiles = EventGraphCache.instance().getEventGraphFilesList();
            for (File file : eventGraphFiles) {
                tempFile = file;

                // _doOpenEventGraph checks if a tab is already opened
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController())._doOpenEventGraph(file);
            }
        } 
        catch (Exception ex) {
            LOG.error("Opening EventGraph file: {} caused error: {}", tempFile, ex);
            messageUser(JOptionPane.WARNING_MESSAGE,
                    "EventGraph Opening Error",
                    "EventGraph file: " + tempFile + "\nencountered error: " + ex + " while loading."
                    );
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
    private void recordAssemblyFiles() {
        openAssembliesList = new ArrayList<>(4);
        List<Object> valueList = historyConfiguration.getList(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "[@value]");
        LOG.debug("recordAssemblyFiles() valueAr size is: {}", valueList.size());
        int idx = 0;
        String op;
        String assemblyFile;
        for (Object s : valueList) {
            assemblyFile = (String) s;
            if (recentAssemblyFileSet.add(assemblyFile)) {
                op = historyConfiguration.getString(ViskitUserConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true")))
                    openAssembliesList.add(assemblyFile);

                notifyRecentAssemblyFileListeners();
            }
            idx++;
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