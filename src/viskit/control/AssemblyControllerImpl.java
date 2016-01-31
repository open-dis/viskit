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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
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
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import viskit.util.EventGraphCache;
import viskit.util.FileBasedAssemblyNode;
import viskit.util.OpenAssembly;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import viskit.assembly.AssemblyRunnerPlug;
import viskit.doe.LocalBootLoader;
import viskit.jgraph.vGraphUndoManager;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;
import viskit.mvc.mvcRecentFileListener;
import viskit.util.Compiler;
import viskit.util.XMLValidationTool;
import viskit.view.dialog.AssemblyMetaDataDialog;
import viskit.view.SimulationRunPanel;
import viskit.view.AssemblyEditViewFrame;
import viskit.view.AssemblyView;
import viskit.xsd.translator.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:26:02 AM
 * @version $Id$
 */
public class AssemblyControllerImpl extends mvcAbstractController implements AssemblyController, OpenAssembly.AssemblyChangeListener {

    static final Logger LOG = LogUtils.getLogger(AssemblyControllerImpl.class);
    private static int mutex = 0;
    Class<?> simEvSrcClass, simEvLisClass, propChgSrcClass, propChgLisClass;
    private String initialFilePath;

    /** The handler to run an assembly */
    private AssemblyRunnerPlug runner;

    /** Creates a new instance of AssemblyController */
    public AssemblyControllerImpl() {
        initializeHistoryXMLConfiguration();
    }

    /**
     * Sets an initial assembly file to open upon Viskit startup supplied by the
     * command line
     * @param assemblyPath the assembly file to immediately open upon startup
     */
    public void setInitialFile(String assemblyPath) {
        if (viskit.ViskitStatics.debug) {
            System.out.println("Initial file set: " + assemblyPath);
        }
        initialFilePath = assemblyPath;
    }

    /** This method is for introducing Assemblies to compile from outside of
     * Viskit.  This method is not used from Viskit and is required for third-party access.
     *
     * @param assemblyPath an assembly file to compile
     */
    public void compileAssembly(String assemblyPath) {
        LOG.debug("Compiling assembly: " + assemblyPath);
        File f = new File(assemblyPath);
        initialFilePath = assemblyPath;
        _doOpen(f);
        compileAssemblyAndPrepareSimulationRunner();
    }

    @Override
    public void begin() {

        // The initialFilePath is set if we have stated a file "arg" upon startup
        // from the command line
        if (initialFilePath != null) {
            LOG.debug("Loading initial file: " + initialFilePath);
            compileAssembly(initialFilePath);
        } else {
            List<File> lis = getOpenAssemblyFileList(false);
            LOG.debug("Inside begin() and lis.size() is: " + lis.size());

            for (File f : lis) {
                _doOpen(f);
            }
        }
        recordProjectFiles();
    }

    /** Information required by the EventGraphControllerImpl to see if an Assembly
     * file is already open.  Also checked internally by this class.
     * @param refresh flag to refresh the list from viskitConfig.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    public final List<File> getOpenAssemblyFileList(boolean refresh) {
        if (refresh || openAssembliesFileList == null) {
            recordAssemblyFiles();
        }
        return openAssembliesFileList;
    }

    private boolean checkSaveIfDirty() {
        if (localDirty) {
            StringBuilder sb = new StringBuilder("<html><center>Execution parameters have been modified.<br>(");

            for (Iterator<OpenAssembly.AssemblyChangeListener> itr = isLocalDirty.iterator(); itr.hasNext();) {
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
    public void settings() {
        // placeholder for combo gui
    }

    @Override
    public void open() {

        File[] files = ((AssemblyView) getView()).openFilesAsk();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file != null) {
				if (file.getParentFile().getAbsolutePath().startsWith(ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot().getAbsolutePath()))
				{
					_doOpen(file);
					ViskitGlobals.instance().getViskitApplicationFrame().selectAssemblyEditorTab();
				}
				else 
				{
					messageToUser (JOptionPane.WARNING_MESSAGE, "Illegal directory for current project", 
							"<html><p>Open assemblies must be within the currently open project.</p>" +
							"<p>&nbsp</p>" +
							"<p>Current project name: <b>" + ViskitGlobals.instance().getCurrentViskitProject().getProjectName() + "</b></p>" +
							"<p>Current project path: "    + ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot().getAbsolutePath() + "</p>" +
							"<p>&nbsp</p>" +
							"<p>Please choose another assembly or else open a different project.</p>");
					// TODO offer to copy?
					break;
				}
			}
        }
		ViskitGlobals.instance().getAssemblyEditor().buildMenus(); // reset
    }

    private void _doOpen(File file) {
        if (!file.exists()) {
            return;
        }

        AssemblyView vaw = (AssemblyView) getView();
        AssemblyModelImpl mod = new AssemblyModelImpl(this);
        mod.init();
        vaw.addTab(mod);

        // these may init to null on startup, check
        // before doing any openAlready lookups
        AssemblyModel[] openAlready = null;
        if (vaw != null) {
            openAlready = vaw.getOpenModels();
        }
        boolean isOpenAlready = false;
        if (openAlready != null) {
            for (AssemblyModel model : openAlready) {
                if (model.getLastFile() != null) {
                    String path = model.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) {
                        isOpenAlready = true;
                    }
                }
            }
        }

        if (mod.newModel(file) && !isOpenAlready) {

            vaw.setSelectedAssemblyName(mod.getMetaData().name);
            // TODO: Implement an Assembly description block set here

            adjustRecentAssemblySet(file);
            markAssemblyFilesOpened();

            // replaces old fileWatchOpen(file);
            initOpenAssemblyWatch(file, mod.getJaxbRoot());
            openEventGraphs(file);

        } else {
            vaw.deleteTab(mod);
        }

        resetRedoUndoStatus();
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {

        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();

        if (view.getCurrentVgraphAssemblyComponentWrapper() != null) {
            vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Assembly file opened as "open" in the app config file */
    private void markAssemblyFilesOpened() {

        // Mark every vAMod opened as "open"
        AssemblyModel[] openAlready = ((AssemblyView) getView()).getOpenModels();
        for (AssemblyModel vAMod : openAlready) {
            if (vAMod.getLastFile() != null) {
                String modelPath = vAMod.getLastFile().getAbsolutePath().replaceAll("\\\\", "/");
                markAssemblyConfigurationOpen(modelPath);
            }
        }
    }

    @Override
    public void openRecentAssembly(File path) {
        _doOpen(path);
		ViskitGlobals.instance().getAssemblyEditor().buildMenus(); // reset
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
                    if (isLocalDirty.isEmpty()) {
                        localDirty = false;
                    }

                    ((AssemblyModel) getModel()).setDirty(true);
                    break;

                case NEW_ASSY:
                    isLocalDirty.clear();
                    localDirty = false;
                    break;

                case PARAM_LOCALLY_EDITTED:
                    // This gets hit when you type something in the last three tabs
                    isLocalDirty.add(source);
                    localDirty = true;
                    break;

                case CLOSE_ASSY:

                    // Close any currently open EGs because we don't yet know which ones
                    // to keep open until iterating through each remaining vAMod

                    ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();

                    AssemblyModel vAMod = (AssemblyModel) getModel();
                    markAssemblyConfigurationClosed(vAMod.getLastFile());

                    AssemblyView view = (AssemblyView) getView();
                    view.deleteTab(vAMod);

                    // NOTE: This doesn't work quite right.  If no Assy is open,
                    // then any non-associated EGs that were also open will
                    // annoyingly close from the closeAll call above.  We are
                    // using an open EG cache system that relies on parsing an
                    // Assy file to find its associated EGs to open
                    if (!isCloseAll()) {

                        AssemblyModel[] modAr = view.getOpenModels();
                        for (AssemblyModel mod : modAr) {
                            if (!mod.equals(vAMod)) {
                                openEventGraphs(mod.getLastFile());
                            }
                        }
                    }

                    break;

                default:
                    LOG.warn("Program error AssemblyControllerImpl.assemblyChanged");
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
    public void addAssemblyFileListener(OpenAssembly.AssemblyChangeListener listener) {
        OpenAssembly.inst().addListener(listener);
    }

    @Override
    public void removeAssemblyFileListener(OpenAssembly.AssemblyChangeListener listener) {
        OpenAssembly.inst().removeListener(listener);
    }

    Set<mvcRecentFileListener> recentAssyListeners = new HashSet<>();

    @Override
    public void addRecentAssemblyFileSetListener(mvcRecentFileListener listener) {
        recentAssyListeners.add(listener);
    }

    @Override
    public void removeRecentAssemblyFileSetListener(mvcRecentFileListener listener) {
        recentAssyListeners.remove(listener);
    }

    /** Here we are informed of open Event Graphs */

    private void notifyRecentAssemblyFileListeners() {
        for (mvcRecentFileListener lis : recentAssyListeners) {
            lis.listChanged();
        }
    }

    Set<mvcRecentFileListener> recentProjListeners = new HashSet<>();

    @Override
    public void addRecentProjectFileSetListener(mvcRecentFileListener listener) {
        recentProjListeners.add(listener);
    }

    @Override
    public void removeRecentProjectFileSetListener(mvcRecentFileListener listener) {
        recentProjListeners.remove(listener);
    }

    private void notifyRecentProjFileListeners() {
        for (mvcRecentFileListener lis : recentProjListeners) {
            lis.listChanged();
        }
    }

    /** Here we are informed of open Event Graphs */
    DirectoryWatch.DirectoryChangeListener egListener = new DirectoryWatch.DirectoryChangeListener() {

        @Override
        public void fileChanged(File file, int action, DirectoryWatch source) {
            // Do nothing.  The DirectoryWatch still tracks temp EGs, but we
            // don't need anything special from this method, yet....
        }
    };

    @Override
    public DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener() {
        return egListener;
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
        AssemblyView view = (AssemblyView) getView();
        GraphMetadata gmd = model.getMetaData();

        // Allow the user to type specific package names
        String packageName = gmd.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = view.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + gmd.name + ".xml", false, "Save Assembly File As");

        if (saveFile != null) {

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            gmd.name = n;
            model.changeMetaData(gmd); // might have renamed

            model.saveModel(saveFile);
            view.setSelectedAssemblyName(gmd.name);
            adjustRecentAssemblySet(saveFile);
            markAssemblyFilesOpened();
        }
    }

    @Override
    public void editGraphMetaData() {
        AssemblyModel mod = (AssemblyModel) getModel();
        if (mod == null) {return;}
        GraphMetadata gmd = mod.getMetaData();
        boolean modified =
                AssemblyMetaDataDialog.showDialog(ViskitGlobals.instance().getAssemblyEditor(), gmd);
        if (modified) {
            ((AssemblyModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(gmd.name);
        }
    }
    private int eventGraphNodeCount = 0;
    private int adapterNodeCount = 0;
    private int propertyChangeListenerNodeCount = 0;    // A little experiment in class introspection
    private static Field eventGraphCountField;
    private static Field adapterCountField;
    private static Field propertyChangeListenerCountField;

    static { // do at class initialization time
        try {
                        eventGraphCountField = AssemblyControllerImpl.class.getDeclaredField("eventGraphNodeCount");
                           adapterCountField = AssemblyControllerImpl.class.getDeclaredField("adapterNodeCount");
            propertyChangeListenerCountField = AssemblyControllerImpl.class.getDeclaredField("propertyChangeListenerNodeCount");
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String shortEventGraphName(String typeName) {
        return shortName(typeName, "evgr_", eventGraphCountField);
    }

    private String shortPropertyChangeListenerName(String typeName) {
        return shortName(typeName, "lstnr_", propertyChangeListenerCountField); // use same counter
    }

    private String shortAdapterName(String typeName) {
        return shortName(typeName, "adptr_", adapterCountField); // use same counter
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
    public void newProject() {
        if (handleProjectClosing()) {
            ViskitGlobals.instance().initializeProjectHomeDirectory();
            ViskitGlobals.instance().createWorkDirectory();

            // For a brand new empty project open a default EG
            File[] egFiles = ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDirectory().listFiles();
            if (egFiles.length == 0) {
                ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).newEventGraph();
            }
        }
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
		ViskitGlobals.instance().getAssemblyEditor().buildMenus(); // reset
    }

    @Override
    public void zipAndMailProject() {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            File projDir;
            File projZip;
            File logFile;

            @Override
            public Void doInBackground() {

                projDir = ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot();
                projZip = new File(projDir.getParentFile(), projDir.getName() + ".zip");
                logFile = new File(projDir, "debug.log");

                if (projZip.exists())
                    projZip.delete();

                if (logFile.exists())
                    logFile.delete();

                try {

                    // First, copy the debug.log to the project dir
                    Files.copy(ViskitConfiguration.V_DEBUG_LOG.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    ZipUtils.zipFolder(projDir, projZip);
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

                    URL url = null;
					String urlString = "[not found]";
                    try {
                        url = new URL("mailto:" + ViskitStatics.VISKIT_MAILING_LIST
                                + "?subject=Viskit%20Project%20Submission%20for%20"
                                + projDir.getName() + "&body=see%20attachment");
						urlString =  url.toString();
                    } catch (MalformedURLException e) {
                        LOG.error(e);
                    }

                    String msg = "Please navigate to<br/>"
                            + projZip.getParent()
                            + "<br/>and email the " + projZip.getName()
                            + " file to "
                            + "<b><a href=\"" + urlString + "\">"
                            + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
                            + "<br/><br/>Click the link to open up an email "
                            + "form, then attach the zip file";

                    try {
                        Desktop.getDesktop().open(projZip.getParentFile());
                    } catch (IOException e) {
                        LOG.error(e);
                    }

                    ViskitStatics.showHyperlinkedDialog((Component) getView(), "Viskit Project: " + projDir.getName(), url, msg, false);

                } catch (InterruptedException | ExecutionException e) {
                    LOG.error(e);
                }
            }
        };
        worker.execute();
    }

    /** Common method between the AssyView and this AssyController
     *
     * @return indication of continue or cancel
     */
    public boolean handleProjectClosing() {
        boolean retVal = true;
        if (ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) {
            String msg = "Are you sure you want to close your current Viskit Project?";
            String title = "Close Current Project";

            int ret = ((AssemblyView) getView()).genericAskYN(title, msg);
            if (ret == JOptionPane.YES_OPTION) {
                doProjectCleanup();
            } else {
                retVal = false;
            }
        }
        return retVal;
    }

    @Override
    public void doProjectCleanup() {
        closeAll();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).closeAll();
        ViskitConfiguration.instance().clearViskitConfiguration();
        clearRecentAssemblyFileList();
        ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).clearRecentEventGraphFileSet();
        ViskitGlobals.instance().getCurrentViskitProject().closeProject();
    }

    @Override
    public void openProject(File file) {
        ViskitStatics.setViskitProjectFile(file);
        ViskitGlobals.instance().createWorkDirectory();

        // Add our currently opened project to the recently opened projects list
        adjustRecentProjectSet(ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot());
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
		ViskitGlobals.instance().getAssemblyEditor().buildMenus();   // reset
    }

    @Override
    public void newAssembly() {

        // Don't allow a new assembly to be created if a current project is  not open
        if (!ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) 
		{
			messageToUser (JOptionPane.WARNING_MESSAGE, "No project directory", 
					"<html><p>New assemblies are only created within an open project.</p>" +
					"<p>Please open or create a project first.</p>");
			return;
		}

        GraphMetadata oldGmd = null;
        AssemblyModel viskitAssemblyModel = (AssemblyModel) getModel();
        if (viskitAssemblyModel != null) {
            oldGmd = viskitAssemblyModel.getMetaData();
        }

        AssemblyModelImpl mod = new AssemblyModelImpl(this);
        mod.init();
        mod.newModel(null);

        // No vAMod set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((AssemblyView) getView()).addTab(mod);

        GraphMetadata gmd = new GraphMetadata(mod);   // build a new one, specific to Assy
        if (oldGmd != null) {
            gmd.packageName = oldGmd.packageName;
        }

        boolean modified =
                AssemblyMetaDataDialog.showDialog(ViskitGlobals.instance().getAssemblyEditor(), gmd);
        if (modified) {
            ((AssemblyModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(gmd.name);

            // TODO: Implement this
//            ((AssemblyView)  getView()).setSelectedEventGraphDescription(gmd.description);
        } else {
            ((AssemblyView) getView()).deleteTab(mod);
        }
		ViskitGlobals.instance().getAssemblyEditor().buildMenus(); // reset
    }

    /**
     * A component wants to say something.
     *
     * @param dialogType the type of dialog popup, i.e. WARN, ERROR, INFO, QUESTION
     * @param title the title of the dialog frame
     * @param message the information to present
     */
    @Override
    public void messageToUser(int dialogType, String title, String message) // dialogType is one of JOptionPane types
    {   AssemblyView view = (AssemblyView) getView();
        if (view != null)
            ((AssemblyView) getView()).genericReport(dialogType, title, message);
        else {
            JOptionPane.showMessageDialog(null, message, title, dialogType);
        }
    }

    @Override
    public void quit() {
        if (preQuit()) {
            postQuit();
        }
    }

    @Override
    public boolean preQuit() {

        // Check for dirty models before exiting
        AssemblyModel[] modAr = ((AssemblyView) getView()).getOpenModels();
        for (AssemblyModel vmod : modAr) {
            setModel((mvcModel) vmod);

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
            setModel((mvcModel) vmod);
            setCloseAll(true);
            close();
        }
		// included in close(): ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
        setCloseAll(false);
    }

    @Override
    public void close() {
        if (preClose()) {
            postClose();
        }
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
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
        OpenAssembly.inst().doSendCloseAssy();
    }

    private void markAssemblyConfigurationClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int idx = 0;
        for (File key : recentAssemblyFileSet) {
            if (key.getPath().contains(f.getName())) {
                historyXMLConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "false");
            }
            idx++;
        }
    }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    private void markAssemblyConfigurationOpen(String path) {

        int idx = 0;
        for (File tempPath : recentAssemblyFileSet) {

            if (tempPath.getPath().equals(path)) {
                historyXMLConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@open]", "true");
            }
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
        messageToUser(JOptionPane.ERROR_MESSAGE, "Can't create a new node", "You must first open an Event Graph before adding a new node.");
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
        messageToUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select a Property Change Listener from the panel on the left.");
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
    private boolean askToSaveAndContinue() {
        int yn = (((AssemblyView) getView()).genericAsk("Question", "Save modified assembly?"));

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
            messageToUser(JOptionPane.ERROR_MESSAGE, "Connection error.", "Possible class not found.  All referenced entities must be in a list at left.");
            return;
        }
        if (oArr == null) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        adapterEdgeEdit(((AssemblyModel) getModel()).newAdapterEdge(shortAdapterName(""), oArr[0], oArr[1]));
    }

    @Override
    public void newSimEvListArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForSEListenerArc(oA, oB);

        if (oArr == null) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        ((AssemblyModel) getModel()).newSimEvLisEdge(oArr[0], oArr[1]);
    }

    @Override
    public void newPropertyChangeListArc(Object[] nodes) {
        // One and only one has to be a prop change listener
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForPropChangeArc(oA, oB);

        if (oArr == null) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a PropertyChangeListener and PropertyChangeSource combination.");
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
        boolean done;
        do {
            done = true;
            boolean modified = ((AssemblyView) getView()).doEditPropertyChangeListenerNode(pclNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changePclNode(pclNode);
            }
        } while (!done);
    }

    @Override
    public void eventGraphEdit(EventGraphNode eventNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((AssemblyView) getView()).doEditEventGraphNode(eventNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changeEvGraphNode(eventNode);
            }
        } while (!done);
    }

    @Override
    public void propertyChangeListenerEdgeEdit(PropertyChangeListenerEdge pclEdge) {
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
    public void simEventListenerEdgeEdit(SimEvListenerEdge seEdge) {
        boolean modified = ((AssemblyView) getView()).doEditSimEventListEdge(seEdge);
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

    private boolean nodeOrEdgeInVector(Vector v) {
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
            String msg = "";
            int nodeCount = 0;  // different msg for edge delete
            for (Object o : selectionVector) {
                if (o instanceof AssemblyNode) {
                    nodeCount++;
                }
                String s = o.toString();
                s = s.replace('\n', ' ');
                msg += ", \n" + s;
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
            messageToUser(JOptionPane.WARNING_MESSAGE,
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
        int x = 100, y = 100;
        int offset = 20;
        for (Object o : copyVector) {
            if (o instanceof AssemblyEdge) {
                continue;
            }

            Point2D p = new Point(x + (offset * copyCount), y + (offset * copyCount));
            if (o instanceof EventGraphNode) {
                String nm = ((ViskitElement) o).getName();
                String typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newEventGraph(nm + "-copy" + copyCount, typ, p);
            } else if (o instanceof PropertyChangeListenerNode) {
                String nm = ((ViskitElement) o).getName();
                String typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newPropertyChangeListener(nm + "-copy" + copyCount, typ, p);
            }
            copyCount++;
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void delete() {
        Vector<Object> v = (Vector<Object>) selectionVector.clone();   // avoid concurrent update
        for (Object elem : v) {
            if (elem instanceof AssemblyEdge) {
                removeEdge((AssemblyEdge) elem);
            } else if (elem instanceof EventGraphNode) {
                EventGraphNode en = (EventGraphNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deleteEvGraphNode(en);
            } else if (elem instanceof PropertyChangeListenerNode) {
                PropertyChangeListenerNode en = (PropertyChangeListenerNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deletePropertyChangeListener(en);
            }
        }

        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    private void removeEdge(AssemblyEdge e) {
        if (e instanceof AdapterEdge) {
            ((AssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
        } else if (e instanceof PropertyChangeListenerEdge) {
            ((AssemblyModel) getModel()).deletePropChangeEdge((PropertyChangeListenerEdge) e);
        } else if (e instanceof SimEvListenerEdge) {
            ((AssemblyModel) getModel()).deleteSimEvLisEdge((SimEvListenerEdge) e);
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

        isUndo = true;

        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();

        Object[] roots = view.getCurrentVgraphAssemblyComponentWrapper().getRoots();
        redoGraphCell = (DefaultGraphCell) roots[roots.length - 1];

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        remove();

        if (!doRemove) {return;}

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache());
        } catch (CannotUndoException ex) {
            LOG.error("Unable to undo: " + ex);
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
        if (redoGraphCell instanceof org.jgraph.graph.Edge) { // TODO fix

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof AdapterEdge) {
                AdapterEdge ed = (AdapterEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoAdapterEdge(ed);
            } else if (redoGraphCell.getUserObject() instanceof PropertyChangeListenerEdge) {
                PropertyChangeListenerEdge ed = (PropertyChangeListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropChangeEdge(ed);
            } else {
                SimEvListenerEdge ed = (SimEvListenerEdge) redoGraphCell.getUserObject();
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

        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        AssemblyEditViewFrame view = (AssemblyEditViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgraphAssemblyComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(view.getCurrentVgraphAssemblyComponentWrapper().getGraphLayoutCache()));

        isUndo = false;
    }

    /********************************/
    /* from menu:*/

    @Override
    public void showXML() {
        AssemblyModel vmod = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || vmod.getLastFile() == null) {
            return;
        }

        ((AssemblyView) getView()).displayXML(vmod.getLastFile());
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
        AssemblyModel vmod = (AssemblyModel) getModel();
        if (source != null && !source.isEmpty()) {
            String className = vmod.getMetaData().packageName + "." + vmod.getMetaData().name;
            ((AssemblyView) getView()).showAndSaveSource(className, source, vmod.getLastFile().getName());
        }
    }

    private String produceJavaAssemblyClass()
	{
        AssemblyModel assemblyModel = (AssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || assemblyModel.getLastFile() == null) {
			// TODO error notification needed, or is it already getting handled?
            return null;
        }
		String sourceCode = buildJavaAssemblySource(assemblyModel.getLastFile());
        return sourceCode;
    }

    /**
     * Builds the actual source code from the Assembly XML after a successful
     * XML validation.  These routines to operate on the current assembly.
     *
     * @param f the Assembly file to produce source from
     * @return a string of Assembly source code
     */
    public String buildJavaAssemblySource(File f) {
        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = new XMLValidationTool(f, new File(XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA));

        if (!xmlValidationTool.isValidXML()) {
			String message = f.getName() + " is not valid XML";
			messageToUser(JOptionPane.ERROR_MESSAGE, "Invalid Assembly XML", message);
            LOG.error(message + "\n");
            // TODO: implement a Dialog pointing to the validationErrors.LOG
            return null;
        } else {
            LOG.info(f.getName() + " is valid XML\n");
        }

        SimkitAssemblyXML2Java simkitAssemblyXML2Java = null;
        try {
            simkitAssemblyXML2Java = new SimkitAssemblyXML2Java(f);
            simkitAssemblyXML2Java.unmarshal();
        } catch (FileNotFoundException e) {
			String message = f.getName() + " updated Assembly XML file not found when unmarshalling";
			messageToUser(JOptionPane.ERROR_MESSAGE, "XML file not found", message);
            LOG.error(e);
        }
		if  (simkitAssemblyXML2Java != null)
		  	 return simkitAssemblyXML2Java.translate();
		else 
		{
			String message = f.getName() + " autogenenerated Java file not found";
			messageToUser(JOptionPane.ERROR_MESSAGE, "File not found", message);
			return "Error, no Java source produced";
		}
    }

    /**
     * Build the actual source code from the Event Graph XML after a successful
     * XML validation
     *
     * @param simkitXML2Java the Event Graph initialized translator to produce source with
     * @return a string of Event Graph source code
     */
    public String buildJavaEventGraphSource(SimkitXML2Java simkitXML2Java) {
        String eventGraphSource;

        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xmlValidationTool = 
				      new XMLValidationTool(simkitXML2Java.getEventGraphFile(),
                                            new File(XMLValidationTool.LOCAL_EVENT_GRAPH_SCHEMA));

        if (!xmlValidationTool.isValidXML())
		{
			String message = simkitXML2Java.getEventGraphFile().getName() + " is not valid XML";
			messageToUser(JOptionPane.ERROR_MESSAGE, " Invalid Event Graph XML", message);
			LOG.error(message + "\n");
            return null;
        } else {
            LOG.info(simkitXML2Java.getEventGraphFile() + " is valid XML\n");
        }

        try {
            eventGraphSource = simkitXML2Java.translate();
        } 
		catch (Exception e)
		{
			String message = "Error building Java from " + simkitXML2Java.getFileBaseName() +
                             ":\n" + e.getMessage() + ", erroneous event-graph xml found";
			messageToUser(JOptionPane.ERROR_MESSAGE, "Failed Event Graph conversion to Java", message);
			LOG.error(message + "\n");
			eventGraphSource = "Error, no Java source produced";
        }
        return eventGraphSource;
    }

    /** Create and test compile our EventGraphs and Assemblies from XML
     *
     * @param sourceCode the translated source either from SimkitXML2Java, or SimkitAssemblyXML2Java
     * @return a reference to a successfully compiled *.class file or null if
     * a compile failure occurred
     */
    public static File compileJavaClassFromString(String sourceCode) {
        String baseName;

        // Find the package subdirectory
        Pattern pattern = Pattern.compile("package.+;");
        Matcher matcher = pattern.matcher(sourceCode);
        boolean findResult = matcher.find();

        String packagePath = "";
        String packageName = "";
        if (findResult) {
            int st = matcher.start();
            int end = matcher.end();
            String s = sourceCode.substring(st, end);
            packageName = sourceCode.substring(st, end - 1);
            packageName = packageName.substring("package".length(), packageName.length()).trim();
            s = s.replace(';', File.separatorChar);
            String[] sa = s.split("\\s");
            sa[1] = sa[1].replace('.', File.separatorChar);
            packagePath = sa[1].trim();
        }

        pattern = Pattern.compile("public\\s+class\\s+");
        matcher = pattern.matcher(sourceCode);
        matcher.find();

        int end = matcher.end();
        String s = sourceCode.substring(end, sourceCode.length()).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];

        FileWriter fw = null;
        boolean compileSuccess;
        try {

            // Should always have a live ViskitProject
            ViskitProject viskitProject = ViskitGlobals.instance().getCurrentViskitProject();

            // Create, or find the project's java source and package
            File sourcePackage = new File(viskitProject.getSrcDirectory(), packagePath);
            if (!sourcePackage.isDirectory()) {
                 sourcePackage.mkdirs();
            }
            File javaFile = new File(sourcePackage, baseName + ".java");
            javaFile.createNewFile();

            fw = new FileWriter(javaFile);
            fw.write(sourceCode);

            // An error stream to write additional error info out to
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Compiler.setOutputStream(byteArrayOutputStream);

            File classesDirectory = viskitProject.getClassesDirectory();

            LOG.info("Test compiling " + javaFile.getCanonicalPath());

            // This will create a class/package to place the .class file
            String diagnostic = Compiler.invoke(packageName, baseName, sourceCode);
            compileSuccess = diagnostic.equals(Compiler.COMPILE_SUCCESS_MESSAGE);
            if (compileSuccess)
			{
                LOG.info(diagnostic + "\n");
                return new File(classesDirectory, packagePath + baseName + ".class");
            } 
			else
			{
				String message = "Error compiling " + javaFile.getCanonicalPath();
                LOG.error(message + "\n");
                LOG.error(diagnostic + "\n");
                if (!byteArrayOutputStream.toString().isEmpty()) {
                    LOG.error(byteArrayOutputStream.toString() + "\n");
                }
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
     * Known modelPath for EventGraph compilation.  Called whenever an XML file
     * loads for the first time, or is saved during an analyst editing session.
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
                LOG.debug("Is an Assembly: " + !isEventGraph);
                return null;
            }
            String sourceCode = buildJavaEventGraphSource(x2j);

            /* Warn that we may have forgotten a parameter required for a super class */
            if (sourceCode == null) {
                String message = xmlFile + " did not compile.\n" +
                        "Please check that you have provided initialization parameters in the \n" +
                        "identical order declared by any super classes";
                LOG.error(xmlFile + " " + message);
                LOG.error(sourceCode);
                messageToUser(JOptionPane.ERROR_MESSAGE, "Source code compilation error", message);
                return null;
            }

            // If using plain Vanilla Viskit, don't compile diskit extended EGs
            // as diskit.jar won't be available
            String[] classPath = ((LocalBootLoader) ViskitGlobals.instance().getWorkClassLoader()).getClassPath();
            boolean foundDiskit = false;
            for (String path : classPath) {
                if (path.contains("diskit.jar")) {
                    foundDiskit = !foundDiskit;
                    break;
                }
            }

            if (sourceCode.contains("diskit") && !foundDiskit) {
                FileBasedClassManager.instance().addCacheMiss(xmlFile);

                // TODO: Need to announce/recommend to the user to place
                // diskit.jar in the classpath, then restart Viskit
            } else {
                paf = compileJavaClassAndSetPackage(sourceCode);
            }
        } catch (FileNotFoundException e) {
            LOG.error("Error creating Java class file from " + xmlFile + ": " + e.getMessage() + "\n");
            FileBasedClassManager.instance().addCacheMiss(xmlFile);
        }
        return paf;
    }

    /** Path for EventGraph and Assembly compilation
     *
     * @param sourceCode the raw source to write to file
     * @return a package and file pair
     */
    private PackageAndFile compileJavaClassAndSetPackage(String sourceCode) {
        String packageName = new String();
        if (sourceCode != null && !sourceCode.isEmpty()) {
            Pattern p = Pattern.compile("package.*;");
            Matcher m = p.matcher(sourceCode);
            if (m.find()) {
                String nuts = m.group();
                if (nuts.endsWith(";")) {
                    nuts = nuts.substring(0, nuts.length() - 1);
                }

                String[] sa = nuts.split("\\s");
                packageName = sa[1];
            }
            File f = compileJavaClassFromString(sourceCode);
            if (f != null) {
                return new PackageAndFile(packageName, f);
            }
        }
        return null;
    }

    // From menu
    @Override
    public void export2grid() {
        AssemblyModel model = (AssemblyModel) getModel();
        File tFile;
        try {
            tFile = TempFileManager.createTempFile("ViskitAssy", ".xml");
        } catch (IOException e) {
            messageToUser(JOptionPane.ERROR_MESSAGE, "File System Error", e.getMessage());
            return;
        }
        model.saveModel(tFile);
    //todo switch to DOE
    }
    private String[] executionParameters;

    // Known modelPath for Assembly compilation
    @Override
    public void initializeAssemblyRun()
	{
        String sourceCode = produceJavaAssemblyClass(); // asks to save

        PackageAndFile packageAndFile = compileJavaClassAndSetPackage(sourceCode);
        if (packageAndFile != null) {
            File f = packageAndFile.file;
            String className = f.getName().substring(0, f.getName().indexOf('.'));
            className = packageAndFile.pkg + "." + className;
			
            String classPath = ""; // no longer necessary since we don't invoke Runtime.exec to compile anymore

            executionParameters = buildExecutionParameterArray(className, classPath);
        } else {
            executionParameters = null;
        }
    }
	
	@Override
	public boolean isAssemblyReady ()
	{
		return (executionParameters != null); // if parameters are ready, then Assembly passed all tests
	}

    @Override
    public void compileAssemblyAndPrepareSimulationRunner() {

        // Prevent multiple pushes of the initialize Simulation Run button
        mutex++;
        if (mutex > 1) {
            return;
        }

        // Prevent double clicking which will cause potential ClassLoader issues
        Runnable r = new Runnable() {

            @Override
            public void run() {
                ((AssemblyEditViewFrame) getView()).runButton.setEnabled(false);
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeLater(r);
        else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InvocationTargetException | InterruptedException e) {
                LOG.error(e);
            }
        }

        SwingWorker worker = new SwingWorker<Void, Void>()
		{
            @Override
            public Void doInBackground() // TODO why Void arther than void?  SwingWorker convention of some sort
			{
                initializeAssemblyRun(); // generate, compile sourceCode and prepare the executionParameters

                if (executionParameters == null) 
				{
                    if (ViskitGlobals.instance().getActiveAssemblyModel() == null) {
                        messageToUser(JOptionPane.WARNING_MESSAGE,
                            "No Assembly file is active",
                            "Please open an Assembly file before running a simulation");
                    } else {
                        String message = "<html><p>Runtime error: no executionParameters found.</p><br />" + 
								         "<p>Please locate and correct the source of the error in assembly XML for proper compilation.</p>";
                        messageToUser(JOptionPane.WARNING_MESSAGE, "Failed Assembly source generation/compilation", message);
                    }
                } 
				else 
				{
                    // Ensure a cleared Simulation Run panel upon every Assembly compile
                    SimulationRunPanel simulationRunPanel = ViskitGlobals.instance().getSimulationRunPanel();
					simulationRunPanel.initializeSimulationOutput ();

                    save(); // Ensure changes to the Assembly Properties dialog get saved
					
                    runner.exec(executionParameters); // Initializes a fresh class loader
                    
                    executionParameters = null; // reset
                }
                return null;
            }

            @Override
            protected void done() {
                try
				{
                   get(); // Wait for the compile, save and Assembly preparations to finish
                } 
				catch (InterruptedException | ExecutionException e) 
				{
                    LOG.error(e);
//                    e.printStackTrace();
                } 
				finally 
				{
                    ((AssemblyEditViewFrame) getView()).runButton.setEnabled(true);
                    mutex--; // clear lock
					ViskitGlobals.instance().getViskitApplicationFrame().selectSimulationRunTab(); // send analyst to Simulation Run tab
                }
            }
        };
        worker.execute(); // do it to it
    }

    // No longer invoking a Runtime.Exec for compilation
//    public static final int EXEC_JAVACMD = 0;
//    public static final int EXEC_VMARG0 = 1;
//    public static final int EXEC_VMARG1 = 2;
//    public static final int EXEC_VMARG3 = 3;
//    public static final int EXEC_DASH_CP = 4;
//    public static final int EXEC_CLASSPATH = 5;
    public static final int EXEC_TARGET_CLASS_NAME = 6;
    public static final int EXEC_VERBOSE_SWITCH = 7;
    public static final int EXEC_STOPTIME_SWITCH = 8;
//    public static final int EXEC_FIRST_ENTITY_NAME = 9;

    /** Prepare for the compilation of the loaded assembly file from java source.
     * Maintain the above static enumerations to match the order below.
     * @param className the name of the Assembly file to compile
     * @param classPath the current ClassLoader context
     * @return operation-system specific array of execution parameters as String array
     */
    private String[] buildExecutionParameterArray(String className, String classPath)
	{
        Vector<String> v = new Vector<>();
        String fileSeparator = ViskitStatics.getFileSeparator();

        StringBuilder invocation = new StringBuilder();
        invocation.append(System.getProperty("java.home"));
        invocation.append(fileSeparator);
        invocation.append("bin");
        invocation.append(fileSeparator);
        invocation.append("java");
        v.add(invocation.toString());// 0
		// execution parameters, TODO consider including these properties as user preferences
        v.add("-Xss2m");                                                // 1
        v.add("-Xincgc");                                               // 2
        v.add("-Xmx512m");                                              // 3
        v.add("-cp");                                                   // 4
        v.add(classPath);                                               // 5
        v.add(className);                                               // 6

        v.add("" + ((AssemblyModel) getModel()).getMetaData().verbose); // 7
        v.add(((AssemblyModel) getModel()).getMetaData().stopTime);     // 8

        Vector<String> detailedOutputEntityNames = ((AssemblyModel) getModel()).getDetailedOutputEntityNames();
        for (String entityName : detailedOutputEntityNames) {
            v.add(entityName);                                          // 9+
        }
        String[] returnStringArray = new String[v.size()];
        return v.toArray(returnStringArray);
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    @Override
    public void windowImageCapture() {

        AssemblyModel vmod = (AssemblyModel) getModel();
        String fileName = "AssemblyDiagram";
        if (vmod.getLastFile() != null) {
            fileName = vmod.getLastFile().getName();
        }

        File imageFile = ((AssemblyView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", true, "Save Assembly Diagram Image");
        if (imageFile == null) {
            return;
        }

        final Timer timer = new Timer(100, new timerCallback(imageFile, true));
        timer.setRepeats(false);
        timer.start();

        imgSaveCount = "" + (++imgSaveInt);
    }

    /** Provides an automatic capture of the currently loaded Assembly and stores
     * it to a specified location for inclusion in the generated Analyst Report
     *
     * @param assemblyImage an image file to write the .png
     */
    public void captureAssemblyImage(File assemblyImage) {

        // Don't display an extra frame while taking snapshots
        final Timer timer = new Timer(100, new timerCallback(assemblyImage, false));
        timer.setRepeats(false);
        timer.start();
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
        public void actionPerformed(ActionEvent ev)
		{
            // create and save the image
            AssemblyEditViewFrame assemblyEditViewFrame = (AssemblyEditViewFrame) getView();

            // Get only the jgraph part
            Component component = assemblyEditViewFrame.getCurrentJgraphComponent();
            if (component == null) {return;}
            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
            }
            Rectangle rectangle = component.getBounds();
            Image image = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_3BYTE_BGR);

            // Tell the jgraph component to draw into memory
            component.paint(image.getGraphics());

            try {
                ImageIO.write((RenderedImage)image, "png", fil); // write to file
            } catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) {
                final JFrame frame = new JFrame("Saved as " + fil.getName());
                Icon imageIcon = new ImageIcon(image);
                JLabel lab = new JLabel(imageIcon);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(lab, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo((Component) getView());

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        frame.setVisible(true);
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        }
    }

    /** Override the default AssemblyRunnerPlug
     *
     * @param plug the AssemblyRunnerPlug to set
     */
    public void setAssemblyRunner(AssemblyRunnerPlug plug) {
        runner = plug;
    }

    /** Opens each EG associated with this Assembly
     * @param f the Assembly File to open EventGraphs for (not used)
     */
    private void openEventGraphs(File f) {
        File tempFile = null;
        try {
            List<File> eventGraphFilesList = EventGraphCache.instance().getEventGraphFilesList();
            for (File file : eventGraphFilesList)
			{
                tempFile = file;

                // _doOpen checks if a tab is already opened
                ((EventGraphControllerImpl) ViskitGlobals.instance().getEventGraphController())._doOpen(file);
            }
			ViskitGlobals.instance().getViskitApplicationFrame().selectAnalystReportTab(); // ensure correct context when building menus
			ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
        } catch (Exception ex) {
            LOG.error("Opening EventGraph file: " + tempFile + " caused error: " + ex);
            messageToUser(JOptionPane.WARNING_MESSAGE,
                    "EventGraph Opening Error",
                    "EventGraph file: " + tempFile + "\nencountered error: " + ex + " while loading."
                    );
            closeAll();
        }
    }

    /** Recent open file support */
    private static final int RECENTLISTSIZE = 15;
    private final Set<File> recentAssemblyFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);
    private final Set<File> recentProjectFileSet  = new LinkedHashSet<>(RECENTLISTSIZE + 1);

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an assembly file to add to the list
     */
    private void adjustRecentAssemblySet(File file) {
        for (Iterator<File> itr = recentAssemblyFileSet.iterator(); itr.hasNext();) {

            File f = itr.next();
            if (file.getPath().equals(f.getPath())) {
                itr.remove();
                break;
            }
        }

        recentAssemblyFileSet.add(file); // to the top
        saveAssemblyHistoryXML(recentAssemblyFileSet);
        notifyRecentAssemblyFileListeners();
    }

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file a project file to add to the list
     */
    public void adjustRecentProjectSet(File file) {
        for (Iterator<File> itr = recentProjectFileSet.iterator(); itr.hasNext();) {

            File f = itr.next();
            if (file.getPath().equals(f.getPath())) {
                itr.remove();
                break;
            }
        }
        recentProjectFileSet.add(file); // to the top
        saveProjectHistoryXML(recentProjectFileSet);
        notifyRecentProjFileListeners();
    }

    private List<File> openAssembliesFileList;

    @SuppressWarnings("unchecked")
    private void recordAssemblyFiles() {
        if (historyXMLConfiguration == null) {initializeHistoryXMLConfiguration();}
        openAssembliesFileList = new ArrayList<>(4);
        List<String> valueList = historyXMLConfiguration.getList(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "[@value]");
        LOG.debug("recordAssemblyFiles() valueList size is: " + valueList.size());
        int index = 0;
        for (String s : valueList) {
            if (recentAssemblyFileSet.add(new File(s))) {
                String open = historyXMLConfiguration.getString(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + index + ")[@open]");

                if (open != null && (open.toLowerCase().equals("true") || open.toLowerCase().equals("yes"))) {
                    openAssembliesFileList.add(new File(s));
                }
                notifyRecentAssemblyFileListeners();
            }
            index++;
        }
    }

    @SuppressWarnings("unchecked")
    private void recordProjectFiles() {
        if (historyXMLConfiguration == null) {initializeHistoryXMLConfiguration();}
        List<String> valueAr = historyXMLConfiguration.getList(ViskitConfiguration.PROJECT_HISTORY_KEY + "[@value]");
        LOG.debug("recordProjFile valueAr size is: " + valueAr.size());
        for (String value : valueAr) {
            adjustRecentProjectSet(new File(value));
        }
    }

    private void saveAssemblyHistoryXML(Set<File> recentFiles) {
        historyXMLConfiguration.clearTree(ViskitConfiguration.RECENT_ASSEMBLY_CLEAR_KEY);
        int idx = 0;

        // The value's modelPath is already delimited with "/"
        for (File value : recentFiles) {
            historyXMLConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_HISTORY_KEY + "(" + idx + ")[@value]", value.getPath());
            idx++;
        }
        historyXMLConfiguration.getDocument().normalize();
    }

    /** Always keep our project Hx until a user clears it manually
     *
     * @param recentFiles a Set of recently opened projects
     */
    private void saveProjectHistoryXML(Set<File> recentFiles) {
        int ix = 0;
        for (File value : recentFiles) {
            historyXMLConfiguration.setProperty(ViskitConfiguration.PROJECT_HISTORY_KEY + "(" + ix + ")[@value]", value.getPath());
            ix++;
        }
        historyXMLConfiguration.getDocument().normalize();
    }

    @Override
    public void clearRecentAssemblyFileList() {
        recentAssemblyFileSet.clear();
        saveAssemblyHistoryXML(recentAssemblyFileSet);
        notifyRecentAssemblyFileListeners();
    }

    @Override
    public Set<File> getRecentAssemblyFileSet() {
        return getRecentAssyFileSet(false);
    }

    private Set<File> getRecentAssyFileSet(boolean refresh) {
        if (refresh || recentAssemblyFileSet == null) {
            recordAssemblyFiles();
        }
        return recentAssemblyFileSet;
    }

    @Override
    public void clearRecentProjectFileSet() {
        recentProjectFileSet.clear();
        saveProjectHistoryXML(recentProjectFileSet);
        notifyRecentProjFileListeners();
    }

    @Override
    public Set<File> getRecentProjectFileSet() {
        return getRecentProjFileSet(false);
    }

    private Set<File> getRecentProjFileSet(boolean refresh) {
        if (refresh || recentProjectFileSet == null) {
            recordProjectFiles();
        }
        return recentProjectFileSet;
    }

    XMLConfiguration historyXMLConfiguration;

    private void initializeHistoryXMLConfiguration() {
        try {
            historyXMLConfiguration = ViskitConfiguration.instance().getViskitApplicationXMLConfiguration();
        } catch (Exception e) {
            LOG.error("Error loading recent history file: " + e.getMessage());
            LOG.warn ("Error, recent history file disabled");
            historyXMLConfiguration = null;
        }
    }
}