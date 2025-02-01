package viskit.control;

import actions.ActionIntrospector;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import edu.nps.util.ZipUtils;

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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import org.apache.logging.log4j.Logger;

import org.jgraph.graph.DefaultGraphCell;

import viskit.util.EventGraphCache;
import viskit.util.FileBasedAssemblyNode;
import viskit.util.OpenAssembly;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import viskit.jgraph.ViskitGraphUndoManager;
import viskit.model.*;
import viskit.mvc.MvcAbstractController;
import viskit.util.Compiler;
import viskit.util.XMLValidationTool;
import viskit.view.dialog.AssemblyMetadataDialog;
import viskit.view.AssemblyViewFrame;
import viskit.view.AssemblyView;
import viskit.view.dialog.SettingsDialog;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;
import static viskit.view.MainFrame.TAB1_LOCALRUN_INDEX;
import viskit.assembly.SimulationRunInterface;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:26:02 AM
 * @version $Id: AssemblyControllerImpl.java 2884 2015-09-02 17:21:52Z tdnorbra $
 */
public class AssemblyControllerImpl extends MvcAbstractController implements AssemblyController, OpenAssembly.AssembyChangeListener {

    static final Logger LOG = LogUtils.getLogger(AssemblyControllerImpl.class);
    private static int mutex = 0;
    Class<?> simEvSrcClass, simEvLisClass, propChgSrcClass, propChgLisClass;

    /** The path to an assembly file if given from the command line */
    private String initialAssemblyFile;

    /** The handler to run an assembly */
    private SimulationRunInterface runner;

    /** Creates a new instance of AssemblyController */
    public AssemblyControllerImpl() {
        initializeConfiguration();
    }

    /**
     * Sets an initial assembly file to open upon Viskit startup supplied by the
     * command line
     * @param f the assembly file to initially open upon startup
     */
    public void setInitialAssemblyFile(String f) {
        if (viskit.ViskitStatics.debug) {
            LOG.info("Initial file set: {}", f);
        }
        initialAssemblyFile = f;
    }

    /** 
     *This method is for Assembly compilation
     * @param assemblyPath an assembly file to compile
     */
    private void compileAssembly(String assemblyPath) {
        LOG.debug("Compiling assembly: {}", assemblyPath);
        File f = new File(assemblyPath);
        _doOpen(f);
        prepareSimulationRunner();
    }

    @Override
    public void begin() {
        File projectPath = ViskitGlobals.instance().getViskitProject().getProjectRoot();

        // The initialAssemblyFile is set if we have stated a file "arg" upon startup
        // from the command line
        if (initialAssemblyFile != null && !initialAssemblyFile.isBlank() && !initialAssemblyFile.contains("$")) { // Check for $ makes sure that a property
            LOG.debug("Loading initial file: {}", initialAssemblyFile);                             // pointing to a assembly path isn't commented
                                                                                                         // out
            // Switch to the project that this Assembly file is located in if paths do not coincide
            if (!initialAssemblyFile.contains(projectPath.getPath())) {
                doProjectCleanup();
                projectPath = new File(initialAssemblyFile).getParentFile().getParentFile().getParentFile();
                openProject(projectPath); // calls EventGraphViewFrame setTitleApplicationProjectName

                // Add new project EventGraphs for LEGO tree inclusion of our SimEntities
                SettingsDialog.RebuildLEGOTreePanelTask t = new SettingsDialog.RebuildLEGOTreePanelTask();
                t.execute();
            }
            compileAssembly(initialAssemblyFile);
        } else {
            openProject(projectPath); // calls EventGraphViewFrame setTitleApplicationProjectName
            List<String> files = getOpenAssemblyFileList(false);
            LOG.debug("Inside begin() and lis.size() is: {}", files.size());
            File file;
            for (String f : files) {
                file = new File(f);
                // Prevent project mismatch
                if (file.exists())
                    _doOpen(file);
            }
        }

        ((AssemblyViewFrame) getView()).setTitleApplicationProjectName();
        recordProjectFiles();
    }

    /** Information required by the EventGraphControllerImpl to see if an Assembly
     * file is already open. Also checked internally by this class.
     * @param refresh flag to refresh the list from viskitConfig.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    public final List<String> getOpenAssemblyFileList(boolean refresh) {
        if (refresh || openAssemblies == null)
            recordAssemblyFiles();

        return openAssemblies;
    }

    private boolean checkSaveIfDirty() {
        if (localDirty) {
            StringBuilder sb = new StringBuilder("<html><center>Execution parameters have been modified.<br>(");

            for (Iterator<OpenAssembly.AssembyChangeListener> itr = isLocalDirty.iterator(); itr.hasNext();) {
                sb.append(itr.next().getHandle());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // last comma-space
            sb.append(")<br>Choose yes if you want to stop this operation, then manually select<br>the indicated tab(s) to ");
            sb.append("save the execution parameters.");

            int yn = (((AssemblyView) getView()).genericAsk2Buttons("Question", sb.toString(), "Stop and let me save",
                    "Ignore my execution parameter changes"));
            // n == -1 if dialog was just closed
            //   ==  0 for first option
            //   ==  1 for second option

            // be conservative, stop for first 2 choices
            if (yn != 1) {
                return false;
            }
        }
        boolean ret = true;
        AssemblyModelImpl mod = (AssemblyModelImpl) getModel();
        if (mod != null) {
            if (((AssemblyModel) getModel()).isDirty()) {
                return askToSaveAndContinue();
            }
        }
        return ret;  // proceed
    }

    @Override
    public void settings()
    {
        boolean modified; // TODO can we do anything with this notification?
        modified = SettingsDialog.showDialog(ViskitGlobals.instance().getMainFrame());
    }

    @Override
    public void open() {
        File[] files = ((AssemblyView) getView()).openFilesAsk();
        if (files == null)
            return;

        for (File file : files) {
            if (file != null)
                _doOpen(file);
        }
    }

    private void _doOpen(File file) {
        if (!file.exists())
            return;

        AssemblyView assemblyView = (AssemblyView) getView();
        AssemblyModelImpl assemblyModelImpl = new AssemblyModelImpl(this);
        assemblyModelImpl.initialize();
        assemblyView.addTab(assemblyModelImpl);

        // these may initialize to null on startup, check
        // before doing any openAlready lookups
        AssemblyModel[] assemblyModelOpenAlreadyArray = null;
        if (assemblyView != null)
            assemblyModelOpenAlreadyArray = assemblyView.getOpenModels();

        boolean isOpenAlready = false;
        String path;
        if (assemblyModelOpenAlreadyArray != null) 
        {
            for (AssemblyModel assemblyModleOpenAlready : assemblyModelOpenAlreadyArray) {
                if (assemblyModleOpenAlready.getLastFile() != null) {
                    path = assemblyModleOpenAlready.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())){
                        isOpenAlready = true;
                        break;
                    }
                }
            }
        }
        if (assemblyModelImpl.newModel(file) && !isOpenAlready) 
        {
            assemblyView.setSelectedAssemblyName(assemblyModelImpl.getMetadata().name);
            // TODO: Implement an Assembly description block set here

            adjustRecentAssemblySet(file);
            markAssemblyFilesOpened();

            // replaces old fileWatchOpen(file);
            initOpenAssemblyWatch(file, assemblyModelImpl.getJaxbRoot());
            openEventGraphs(file);
        } 
        else 
        {
            assemblyView.deleteTab(assemblyModelImpl);
        }

        resetRedoUndoStatus();
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {
        AssemblyViewFrame view = (AssemblyViewFrame) getView();

        if (view.getCurrentVgacw() != null) {
            ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgacw().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Assembly file opened as "open" in the app config file */
    private void markAssemblyFilesOpened() {

        // Mark every vAMod opened as "open"
        AssemblyModel[] openAlready = ((AssemblyView) getView()).getOpenModels();
        for (AssemblyModel assemblyModel : openAlready) {
            if (assemblyModel.getLastFile() != null)
                markAssemblyConfigurationOpen(assemblyModel.getLastFile());
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
    public OpenAssembly.AssembyChangeListener getAssemblyChangeListener() {
        return assemblyChangeListener;
    }
    private boolean localDirty = false;
    private Set<OpenAssembly.AssembyChangeListener> isLocalDirty = new HashSet<>();
    OpenAssembly.AssembyChangeListener assemblyChangeListener = new OpenAssembly.AssembyChangeListener() {

        @Override
        public void assemblyChanged(int action, OpenAssembly.AssembyChangeListener source, Object param) {
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
                    markAssemblyConfigurationClosed(assemblyModel.getLastFile());

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
                                openEventGraphs(mod.getLastFile());
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
    public void assemblyChanged(int action, OpenAssembly.AssembyChangeListener source, Object param) {
        assemblyChangeListener.assemblyChanged(action, source, param);
    }

    @Override
    public void addAssemblyFileListener(OpenAssembly.AssembyChangeListener lis) {
        OpenAssembly.inst().addListener(lis);
    }

    @Override
    public void removeAssemblyFileListener(OpenAssembly.AssembyChangeListener lis) {
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
    public void save() {
        AssemblyModel mod = (AssemblyModel) getModel();
        if (mod.getLastFile() == null) {
            saveAs();
        } else {
            mod.saveModel(mod.getLastFile());
        }
    }

    @Override
    public void saveAs() {
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
            model.changeMetaData(graphMetadata); // might have renamed

            model.saveModel(saveFile);
            assemblyView.setSelectedAssemblyName(graphMetadata.name);
            adjustRecentAssemblySet(saveFile);
            markAssemblyFilesOpened();
        }
    }

    @Override
    public void editGraphMetadata() {
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (assemblyModel == null) {return;}
        GraphMetadata graphMetadata = assemblyModel.getMetadata();
        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyViewFrame(), graphMetadata);
        if (modified) {
            ((AssemblyModel) getModel()).changeMetaData(graphMetadata);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(graphMetadata.name);
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
        if (handleProjectClosing()) 
        {
            ViskitGlobals.instance().inititalizeProjectHome();
            ViskitGlobals.instance().createWorkingDirectory();

            // For a brand new empty project open a default Event Graph
            File[] eventGraphFiles = ViskitGlobals.instance().getViskitProject().getEventGraphsDir().listFiles();
            if (eventGraphFiles.length == 0) {
                ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).newEventGraph();
            }
        }
    }

    @Override
    public void zipAndMailProject() {

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            File projectDirectory;
            File projectZipFile;
            File logFile;

            @Override
            public Void doInBackground() {

                projectDirectory = ViskitGlobals.instance().getViskitProject().getProjectRoot();
                projectZipFile = new File(projectDirectory.getParentFile(), projectDirectory.getName() + ".zip");
                logFile = new File(projectDirectory, "debug.log");

                if (projectZipFile.exists())
                    projectZipFile.delete();

                if (logFile.exists())
                    logFile.delete();

                try {

                    // First, copy the error.log to the project dir
                    Files.copy(ViskitConfiguration.VISKIT_ERROR_LOG.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    ZipUtils.zipFolder(projectDirectory, projectZipFile);
                } catch (IOException e) {
                    LOG.error(e);
                }

                return null;
            }

            @Override
            public void done() {
                try {

                    // Waits for the zip process to finish
                    get();

                    try {
                        URL url = new URI("mailto:" + ViskitStatics.VISKIT_MAILING_LIST
                                + "?subject=Viskit%20Project%20Submission%20for%20"
                                + projectDirectory.getName() + "&body=see%20attachment").toURL();
                        String msg = "Please navigate to<br/>"
                                + projectZipFile.getParent()
                                + "<br/>and email the " + projectZipFile.getName()
                                + " file to "
                                + "<b><a href=\"" + url.toString() + "\">"
                                + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
                                + "<br/><br/>Click the link to open up an email "
                                + "form, then attach the zip file";
                        ViskitStatics.showHyperlinkedDialog((Component) getView(), "Viskit Project: " + projectDirectory.getName(), url, msg, false);
                    } catch (MalformedURLException | URISyntaxException e) {
                        LOG.error(e);
                    }

                    Desktop.getDesktop().open(projectZipFile.getParentFile());

                } catch (InterruptedException | ExecutionException | IOException e) {
                    LOG.error(e);
                }
            }
        };
        worker.execute();
    }

    /** Common method between the AssemblyView and this AssemblyController
     *
     * @return indication of continue or cancel
     */
    public boolean handleProjectClosing() {
        boolean projectClosed = true;
        if (ViskitGlobals.instance().getViskitProject().isProjectOpen())
        {
            String msg = "Are you sure you want to close your current Viskit Project?";
            String title = "Close Current Project?";

            int returnValue = ((AssemblyView) getView()).genericAskYN(title, msg);
            if (returnValue == JOptionPane.YES_OPTION)
            {
                doProjectCleanup();
            } 
            else {
                projectClosed = false;
            }
        }
        
        return projectClosed;
    }

    @Override
    public void doProjectCleanup()
    {
        closeAll();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();
        ViskitConfiguration.instance().clearViskitConfig();
        clearRecentAssemblyFileList();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).clearRecentEventGraphFileSet();
        ViskitGlobals.instance().getViskitProject().closeProject();
        ViskitGlobals.instance().getMainFrame().updateApplicationTitle();
    }

    @Override
    public void openProject(File file) {
        ViskitStatics.setViskitProjectFile(file);
        ViskitGlobals.instance().createWorkingDirectory();

        // Add our currently opened project to the recently opened projects list
        adjustRecentProjectSet(ViskitGlobals.instance().getViskitProject().getProjectRoot());
        ViskitGlobals.instance().getEventGraphViewFrame().setTitleApplicationProjectName();
        runner.resetSimulationRunPanel();
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

        GraphMetadata oldGraphMetadata = null;
        AssemblyModel viskitAssemblyModel = (AssemblyModel) getModel();
        if (viskitAssemblyModel != null) {
            oldGraphMetadata = viskitAssemblyModel.getMetadata();
        }

        AssemblyModelImpl assemblyModel = new AssemblyModelImpl(this);
        assemblyModel.initialize();
        assemblyModel.newModel(null); // should create new assembly file

        // No vAMod set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((AssemblyView) getView()).addTab(assemblyModel);

        GraphMetadata graphMetadata = new GraphMetadata(assemblyModel);   // build a new one, specific to Assembly
        if (oldGraphMetadata != null) {
            graphMetadata.packageName = oldGraphMetadata.packageName;
        }

        boolean modified =
                AssemblyMetadataDialog.showDialog(ViskitGlobals.instance().getAssemblyViewFrame(), graphMetadata);
        if (modified) 
        {
            ((AssemblyModel) getModel()).changeMetaData(graphMetadata);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(graphMetadata.name);

            // TODO: Implement this
//            ((AssemblyView)  getView()).setSelectedEventGraphDescription(gmd.description);
        } 
        else 
        {
            ((AssemblyView) getView()).deleteTab(assemblyModel);
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
        if (((AssemblyView) getView()) != null)
            ((AssemblyView) getView()).genericReport(messageType, messageTitle, messageBody);
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
        AssemblyModel[] modAr = ((AssemblyView) getView()).getOpenModels();
        for (AssemblyModel vmod : modAr) {
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
    public void closeAll() {
        AssemblyModel[] modAr = ((AssemblyView) getView()).getOpenModels();
        for (AssemblyModel vmod : modAr) {
            setModel((MvcModel) vmod);
            setCloseAll(true);
            close();
        }
        setCloseAll(false);
    }

    @Override
    public void close() {
        if (preClose()) {
            postClose();
        }
    }

    @Override
    public boolean preClose() {
        AssemblyModel vmod = (AssemblyModel) getModel();
        if (vmod == null) {
            return false;
        }

        if (vmod.isDirty()) {
            return checkSaveIfDirty();
        }

        return true;
    }

    @Override
    public void postClose() {
        OpenAssembly.inst().doSendCloseAssembly();
    }

    private void markAssemblyConfigurationClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int idx = 0;
        for (String key : recentAssemblyFileSet) {
            if (key.contains(f.getName()))
                historyConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "false");

            idx++;
        }
    }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    private void markAssemblyConfigurationOpen(File f) {

        int idx = 0;
        for (String key : recentAssemblyFileSet) {
            if (key.contains(f.getName()))
                historyConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "true");

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
        Object o = ((AssemblyView) getView()).getSelectedEventGraph();

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
        Object o = ((AssemblyView) getView()).getSelectedPropertyChangeListener();

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
        ((AssemblyModel) getModel()).newPropChangeListener(shName, name, p);
    }

    @Override
    public void newFileBasedPropertyChangeListenerNode(FileBasedAssemblyNode xnode, Point p) {
        String shName = shortPropertyChangeListenerName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newPropChangeListenerFromXML(shName, xnode, p);
    }

    /**
     *
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue()
    {
        String message = "Save modified ";
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if ((assemblyModel != null) && (assemblyModel.getMetadata() != null))
             message += assemblyModel.getMetadata().name + ".xml"; 
        if (!message.toLowerCase().contains("assembly"))
             message += " assembly";
        message += "?";
        int yn = (((AssemblyView) getView()).genericAsk("Save assembly?", message));

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
        } catch (Exception e) {
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
        ((AssemblyModel) getModel()).newSimEvLisEdge(oArr[0], oArr[1]);
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
        simEvSrcClass = ViskitStatics.classForName("simkit.SimEventSource");
        simEvLisClass = ViskitStatics.classForName("simkit.SimEventListener");
        propChgSrcClass = ViskitStatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = ViskitStatics.classForName("java.beans.PropertyChangeListener");
        if (propChgSrcClass.isAssignableFrom(ca)) {
            obArr[0] = a;
        } else if (propChgSrcClass.isAssignableFrom(cb)) {
            obArr[0] = b;
        }
        if (propChgLisClass.isAssignableFrom(cb)) {
            obArr[1] = b;
        } else if (propChgLisClass.isAssignableFrom(ca)) {
            obArr[1] = a;
        }

        if (obArr[0] == null || obArr[1] == null || obArr[0] == obArr[1]) {
            return null;
        }
        return obArr;
    }

    AssemblyNode[] orderSELSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        simEvSrcClass = ViskitStatics.classForName("simkit.SimEventSource");
        simEvLisClass = ViskitStatics.classForName("simkit.SimEventListener");
        propChgSrcClass = ViskitStatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = ViskitStatics.classForName("java.beans.PropertyChangeListener");
        if (simEvSrcClass.isAssignableFrom(ca)) {
            obArr[0] = a;
        } else if (simEvSrcClass.isAssignableFrom(cb)) {
            obArr[0] = b;
        }
        if (simEvLisClass.isAssignableFrom(cb)) {
            obArr[1] = b;
        } else if (simEvLisClass.isAssignableFrom(ca)) {
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
            modified = ((AssemblyView) getView()).doEditPropertyChangeListenerNode(pclNode);
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
            modified = ((AssemblyView) getView()).doEditEventGraphNode(evNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changeEvGraphNode(evNode);
            }
        } while (!done);
    }

    @Override
    public void propertyChangeListenerEdgeEdit(PropertyChangeEdge pclEdge) {
        boolean modified = ((AssemblyView) getView()).doEditPropertyChangeListenerEdge(pclEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changePclEdge(pclEdge);
        }
    }

    @Override
    public void adapterEdgeEdit(AdapterEdge aEdge) {
        boolean modified = ((AssemblyView) getView()).doEditAdapterEdge(aEdge);
        if (modified) {
            ((AssemblyModel) getModel()).changeAdapterEdge(aEdge);
        }
    }

    @Override
    public void simEventListenerEdgeEdit(SimEventListenerEdge seEdge) {
        boolean modified = ((AssemblyView) getView()).doEditSimEventListenerEdge(seEdge);
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
    public void remove() {
        if (!selectionVector.isEmpty()) {
            // first ask:
            String s, msg = "";
            int nodeCount = 0;  // different msg for edge delete
            for (Object o : selectionVector) {
                if (o instanceof AssemblyNode) {
                    nodeCount++;
                    s = o.toString();
                    s = s.replace('\n', ' ');
                    msg += ", \n" + s;
                }
            }
            String specialNodeMsg = (nodeCount > 0) ? "\n(All unselected but attached edges will also be removed.)" : "";
            doRemove = ((AssemblyView) getView()).genericAsk("Remove element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg) == JOptionPane.YES_OPTION;
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
                ((AssemblyModel) getModel()).newPropChangeListener(nm + "-copy" + copyCount, typ, p);
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
                ((AssemblyModel) getModel()).deleteEvGraphNode((EventGraphNode) en);
            } else if (elem instanceof PropertyChangeListenerNode) {
                en = (PropertyChangeListenerNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deletePropChangeListener((PropertyChangeListenerNode) en);
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
            ((AssemblyModel) getModel()).deleteSimEvLisEdge((SimEventListenerEdge) e);
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
    public void undo() {
        if (selectionVector.isEmpty())
            return;

        isUndo = true;

        AssemblyViewFrame view = (AssemblyViewFrame) getView();
        Object[] roots = view.getCurrentVgacw().getRoots();
        for (Object root : roots) {
            if (root instanceof DefaultGraphCell)
                redoGraphCell = ((DefaultGraphCell) root);
            if (selectionVector.firstElement().equals(redoGraphCell.getUserObject()))
                break;
        }
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgacw().getUndoManager();

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentVgacw().getGraphLayoutCache());
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
    public void redo() {

        // Recreate the JAXB (XML) bindings since the paste function only does
        // nodes and not edges
        if (redoGraphCell instanceof org.jgraph.graph.Edge) {

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof AdapterEdge) {
                AdapterEdge ed = (AdapterEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoAdapterEdge(ed);
            } else if (redoGraphCell.getUserObject() instanceof PropertyChangeEdge) {
                PropertyChangeEdge ed = (PropertyChangeEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropChangeEdge(ed);
            } else {
                SimEventListenerEdge ed = (SimEventListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoSimEvLisEdge(ed);
            }
        } else {

            if (redoGraphCell.getUserObject() instanceof PropertyChangeListenerNode) {
                PropertyChangeListenerNode node = (PropertyChangeListenerNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropChangeListener(node);
            } else {
                EventGraphNode node = (EventGraphNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoEventGraph(node);
            }
        }

        AssemblyViewFrame view = (AssemblyViewFrame) getView();
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgacw().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentVgacw().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: {}", ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        AssemblyViewFrame view = (AssemblyViewFrame) getView();
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgacw().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(view.getCurrentVgacw().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(view.getCurrentVgacw().getGraphLayoutCache()));

        isUndo = false;
    }

    /********************************/
    /* from menu:*/

    @Override
    public void viewXML() {
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || assemblyModel.getLastFile() == null) {
            return;
        }

        ((AssemblyView) getView()).displayXML(assemblyModel.getLastFile());
    }

    private boolean checkSaveForSourceCompile() {
        AssemblyModel vmod = (AssemblyModel) getModel();

        // Perhaps a cached file is no longer present in the path
        if (vmod == null) {return false;}
        if (vmod.isDirty() || vmod.getLastFile() == null) {
            int ret = ((AssemblyView) getView()).genericAskYN("Confirm", "The model will be saved.\nContinue?");
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            this.saveAs();
        }
        return true;
    }

    @Override
    public void generateJavaSource() {
        String source = produceJavaAssemblyClass();
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (source != null && !source.isEmpty()) {
            String className = assemblyModel.getMetadata().packageName + "." + assemblyModel.getMetadata().name;
            ((AssemblyView) getView()).showAndSaveSource(className, source, assemblyModel.getLastFile().getName());
        }
    }

    private String produceJavaAssemblyClass() {
        AssemblyModel vmod = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || vmod.getLastFile() == null) {
            return null;
        }
        return buildJavaAssemblySource(vmod.getLastFile());
    }

    /**
     * Builds the actual source code from the Assembly XML after a successful
     * XML validation
     *
     * @param f the Assembly file to produce source from
     * @return a string of Assembly source code
     */
    public String buildJavaAssemblySource(File f) {
        String assemblySource = null;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xvt = new XMLValidationTool(f.getPath(), XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA);

        if (!xvt.isValidXML()) {

            // TODO: implement a Dialog pointing to the validationErrors.LOG
            return assemblySource;
        } else {
            LOG.info("{} is valid XML\n", f);
        }

        SimkitAssemblyXML2Java x2j;
        try {
            x2j = new SimkitAssemblyXML2Java(f);
            x2j.unmarshal();
            assemblySource = x2j.translate();
        } catch (FileNotFoundException e) {
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
     * @param x2j the Event Graph initialized translator to produce source with
     * @return a string of Event Graph source code
     */
    public String buildJavaEventGraphSource(SimkitXML2Java x2j) {
        String eventGraphSource = null;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xvt = new XMLValidationTool(x2j.getEventGraphFile().getPath(),
                XMLValidationTool.LOCAL_EVENT_GRAPH_SCHEMA);

        if (!xvt.isValidXML()) {

            // TODO: implement a Dialog pointing to the validationErrors.LOG
            return eventGraphSource;
        } 
        else 
        {
            LOG.info("{} is valid XML", x2j.getEventGraphFile());
        }

        try {
            eventGraphSource = x2j.translate();
        } catch (Exception e) {
            LOG.error("Error building Java from {}: {}, erroneous event-graph xml found", x2j.getFileBaseName(), e.getMessage());
        }
        return eventGraphSource;
    }

    /** Create and test compile our EventGraphs and Assemblies from XML
     *
     * @param src the translated source either from SimkitXML2Java, or SimkitAssemblyXML2Java
     * @return a reference to a successfully compiled *.class file or null if
     * a compile failure occurred
     */
    public static File compileJavaClassFromString(String src) {
        String baseName;

        // Find the package subdirectory
        Pattern pat = Pattern.compile("package.+;");
        Matcher mat = pat.matcher(src);
        boolean fnd = mat.find();

        String packagePath = "";
        String pkg = "";
        if (fnd) {
            int st = mat.start();
            int end = mat.end();
            String s = src.substring(st, end);
            pkg = src.substring(st, end - 1);
            pkg = pkg.substring("package".length(), pkg.length()).trim();
            s = s.replace(';', File.separatorChar);
            String[] sa = s.split("\\s");
            sa[1] = sa[1].replace('.', File.separatorChar);
            packagePath = sa[1].trim();
        }

        pat = Pattern.compile("public\\s+class\\s+");
        mat = pat.matcher(src);
        mat.find();

        int end = mat.end();
        String s = src.substring(end, src.length()).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];

        FileWriter fw = null;
        boolean compileSuccess;
        try {

            // Should always have a live ViskitProject
            ViskitProject viskitProject = ViskitGlobals.instance().getViskitProject();

            // Create, or find the project's java source and package
            File srcPkg = new File(viskitProject.getSrcDir(), packagePath);
            if (!srcPkg.isDirectory())
                srcPkg.mkdirs();
            File javaFile = new File(srcPkg, baseName + ".java");
            javaFile.createNewFile();

            fw = new FileWriter(javaFile);
            fw.write(src);

            // An error stream to write additional error info out to
            OutputStream baosOut = new ByteArrayOutputStream();
            Compiler.setOutPutStream(baosOut);

            File classesDir = viskitProject.getClassesDirectory();

            LOG.info("Test compiling " + javaFile.getCanonicalPath());

            // This will create a class/package to place the .class file
            String diagnostic = Compiler.invoke(pkg, baseName, src);
            compileSuccess = diagnostic.equals(Compiler.COMPILE_SUCCESS_MESSAGE);
            if (compileSuccess) {
                LOG.info(diagnostic + "\n");
                return new File(classesDir, packagePath + baseName + ".class");
            } else {
                LOG.error(diagnostic + "\n");
                if (!baosOut.toString().isEmpty())
                    LOG.error(baosOut.toString() + "\n");
            }

        } catch (IOException ioe) {
            LOG.error(ioe);
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (IOException e) {}
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

            String src = buildJavaEventGraphSource(x2j);

            /* We may have forgotten a parameter required for a super class */
            if (src == null) {
                String msg = xmlFile + " did not translate to source code.\n" +
                        "Manually compile to determine cause";
                LOG.error(msg);
                messageUser(JOptionPane.ERROR_MESSAGE, "Source code translation error", msg);
                return null;
            }

            paf = compileJavaClassAndSetPackage(src);

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
        File tFile;
        try {
            tFile = TempFileManager.createTempFile("ViskitAssembly", ".xml");
        } catch (IOException e) {
            messageUser(JOptionPane.ERROR_MESSAGE, "File System Error", e.getMessage());
            return;
        }
        model.saveModel(tFile);
    //todo switch to DOE
    }
    private String[] execStrings;

    // Known modelPath for Assembly compilation
    @Override
    public void initializeAssemblySimulationRun() 
    {
        String src = produceJavaAssemblyClass(); // asks to save

        PackageAndFile paf = compileJavaClassAndSetPackage(src);
        if (paf != null) {
            File f = paf.file;
            String clNam = f.getName().substring(0, f.getName().indexOf('.'));
            clNam = paf.packageName + "." + clNam;

            execStrings = buildExecStrings(clNam);
        } 
        else {
            execStrings = null;
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
            ((AssemblyViewFrame) getView()).initializeAssemblySimulationRunButton.setEnabled(false);
        };

        try {
            SwingUtilities.invokeLater(r);
        } catch (Exception e) {
            LOG.error(e);
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            public Void doInBackground() {

                // Compile and prep the execStrings
                initializeAssemblySimulationRun();

                if (execStrings == null) {

                    if (ViskitGlobals.instance().getActiveAssemblyModel() == null)
                    {
                        messageUser(JOptionPane.WARNING_MESSAGE,
                            "Assembly File Not Opened",
                            "Please open an Assembly file");
                    } 
                    else
                    {
                        String msg = "Please locate and correct the source of the error in the assembly XML for proper compilation";
                        messageUser(JOptionPane.WARNING_MESSAGE, "Assembly source generation/compilation error", msg);
                    }
                }
                else
                {
                    // Ensure a cleared Simulation panel upon every Assembly compile
                    runner.resetSimulationRunPanel();

                    // Ensure any changes to the Assembly Properties dialog get saved
                    save();
        
                    String assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getMetadata().name;
                    String  consoleName = TextAreaOutputStream.ASSEMBLY_SIMULATION_RUN_PANEL_TITLE;
                    if (!assemblyName.isBlank() && assemblyName.toLowerCase().contains("assembly"))
                         consoleName += " for " + assemblyName;
                    else if (!assemblyName.isBlank())
                         consoleName += " for Assembly " + assemblyName;
                    ((AssemblyViewFrame) getView()).setTitle(assemblyName);
                    ViskitGlobals.instance().getSimulationRunPanel().setTitle(consoleName);
                    ViskitGlobals.instance().getMainFrame().getSimulationRunTabbedPane().setTitleAt(TAB1_LOCALRUN_INDEX,  assemblyName);
                    // Initializes a fresh class loader
                    runner.exec(execStrings);

                    // reset
                    execStrings = null;
                }

                return null;
            }

            @Override
            protected void done() {
                try {

                    // Wait for the compile, save and Assembly preparations to finish
                    get();

                } 
                catch (InterruptedException | ExecutionException e) {
                    LOG.error(e);
//                    e.printStackTrace();
                } finally {
                    ((AssemblyViewFrame) getView()).initializeAssemblySimulationRunButton.setEnabled(true);
                    mutex--;
                }
            }
        };
        worker.execute();
    }

    public static final int EXEC_TARGET_CLASS_NAME = 0;
    public static final int EXEC_VERBOSE_SWITCH = 1;
    public static final int EXEC_STOPTIME_SWITCH = 2;;

    /** Prepare for running the loaded assembly file from java source.
     * Maintain the above statics indices to match the order used.
     * @param className the name of the Assembly file to compile
     * @return a parameter array
     */
    private String[] buildExecStrings(String className) {
        List<String> v = new ArrayList<>();

        v.add(className);                                               // 0
        v.add("" + ((AssemblyModel) getModel()).getMetadata().verbose); // 1
        v.add(((AssemblyModel) getModel()).getMetadata().stopTime);     // 2

        String[] ra = new String[v.size()];
        return v.toArray(ra);
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    @Override
    public void captureWindow() {

        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        String fileName = "AssemblyScreenCapture";
        if (assemblyModel.getLastFile() != null) {
            fileName = assemblyModel.getLastFile().getName();
        }

        File assemblyScreenCaptureFile = ((AssemblyView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", true);
        if (assemblyScreenCaptureFile == null) {
            return;
        }

        final Timer captureWindowTimer = new Timer(100, new timerCallback(assemblyScreenCaptureFile, true));
        captureWindowTimer.setRepeats(false);
        captureWindowTimer.start();

        imgSaveCount = "" + (++imgSaveInt);
    }

    /** Provides an automatic capture of the currently loaded Assembly and stores
     * it to a specified location for inclusion in the generated Analyst Report
     *
     *@param assemblyImageFile assemblyImage an image file to write the .png
     */
    public void captureAssemblyImage(File assemblyImageFile) {

        // Don't display an extra frame while taking snapshots
        final Timer captureAssemblyImageTimer = new Timer(100, new timerCallback(assemblyImageFile, false));
        captureAssemblyImageTimer.setRepeats(false);
        captureAssemblyImageTimer.start();
    }

    public boolean isCloseAll() {
        return closeAll;
    }

    public void setCloseAll(boolean closeAll) {
        this.closeAll = closeAll;
    }

    class timerCallback implements ActionListener {

        File fil;
        boolean display;

        /**
         * Constructor for this timerCallBack
         * @param f the file to write an image to
         * @param b if true, display the image
         */
        timerCallback(File f, boolean b) {
            fil = f;
            display = b;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {

            // create and save the image
            AssemblyViewFrame avf = (AssemblyViewFrame) getView();

            // Get only the jgraph part
            Component component = avf.getCurrentJgraphComponent();
            if (component == null) {return;}
            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
            }
            Rectangle rec = component.getBounds();
            Image image = new BufferedImage(rec.width, rec.height, BufferedImage.TYPE_3BYTE_BGR);

            // Tell the jgraph component to draw into memory
            component.paint(image.getGraphics());

            try {
                ImageIO.write((RenderedImage)image, "png", fil);
            } catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) {
                final JFrame frame = new JFrame("Saved as " + fil.getName());
                Icon ii = new ImageIcon(image);
                JLabel lab = new JLabel(ii);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(lab, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo((Component) getView());

                Runnable r = () -> {
                    frame.setVisible(true);
                };
                SwingUtilities.invokeLater(r);
            }
        }
    }

    /** Override the default SimulationRunInterface
     *
     * @param plug the SimulationRunInterface to set
     */
    public void setAssemblyRunner(SimulationRunInterface plug) {
        runner = plug;
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
        } catch (Exception ex) {
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
     * @param file a project file to add to the list
     */
    private void adjustRecentProjectSet(File file) {
        String f;
        for (Iterator<String> itr = recentProjectFileSet.iterator(); itr.hasNext();) {
            f = itr.next();
            if (file.getPath().equals(f)) {
                itr.remove();
                break;
            }
        }

        if (!recentProjectFileSet.contains(file.getPath()))
             recentProjectFileSet.add(file.getPath()); // to the top

        saveProjectHistoryXML(recentProjectFileSet);
        notifyRecentProjectFileListeners();
    }

    private List<String> openAssemblies;

    @SuppressWarnings("unchecked")
    private void recordAssemblyFiles() {
        openAssemblies = new ArrayList<>(4);
        List<Object> valueList = historyConfiguration.getList(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "[@value]");
        LOG.debug("recordAssemblyFiles() valueAr size is: {}", valueList.size());
        int idx = 0;
        String op;
        String assemblyFile;
        for (Object s : valueList) {
            assemblyFile = (String) s;
            if (recentAssemblyFileSet.add(assemblyFile)) {
                op = historyConfiguration.getString(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true")))
                    openAssemblies.add(assemblyFile);

                notifyRecentAssemblyFileListeners();
            }
            idx++;
        }
    }

    @SuppressWarnings("unchecked")
    private void recordProjectFiles() {
        List<Object> valueAr = historyConfiguration.getList(ViskitConfiguration.PROJECT_HISTORY_KEY + "[@value]");
        LOG.debug("recordProjectFiles valueAr size is: {}", valueAr.size());
        for (Object value : valueAr)
            adjustRecentProjectSet(new File((String) value));
    }

    private void saveAssemblyHistoryXML(Set<String> recentFiles) {
        historyConfiguration.clearTree(ViskitConfiguration.RECENT_ASSEMBLY_CLEAR_KEY);
        int idx = 0;

        // The value's path is already delimited with "/"
        for (String value : recentFiles) {
            historyConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@value]", value); // set relative path if available
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
            historyConfiguration.setProperty(ViskitConfiguration.PROJECT_HISTORY_KEY + "(" + idx + ")[@value]", value); // set relative path if available
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
            historyConfiguration = ViskitConfiguration.instance().getViskitAppConfiguration();
        } catch (Exception e) {
            LOG.error("Error loading history file: {}", e.getMessage());
            LOG.warn("Recent file saving disabled");
            historyConfiguration = null;
        }
    }
    JTabbedPane mainTabbedPane;
    int mainTabbedPaneIdx;

    /**
     * Sets the Analyst report panel
     * @param tabbedPane our Analyst report panel parent
     * @param idx the index to retrieve the Analyst report panel
     */
    @Override
    public void setMainTabbedPane(JComponent tabbedPane, int idx) {
        this.mainTabbedPane = (JTabbedPane) tabbedPane;
        mainTabbedPaneIdx = idx;
    }
    public void makeTopPaneAssemblyTabActive()
    {
        mainTabbedPane.setSelectedIndex(mainTabbedPaneIdx);
    }
}