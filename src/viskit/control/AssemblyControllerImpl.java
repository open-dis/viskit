package viskit.control;

import actions.ActionIntrospector;
import edu.nps.util.DirectoryWatch;
import edu.nps.util.FileIO;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import edu.nps.util.ZipUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
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
import viskit.util.FileBasedAssyNode;
import viskit.util.OpenAssembly;
import viskit.VGlobals;
import viskit.ViskitConfig;
import viskit.ViskitProject;
import viskit.VStatics;
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
import viskit.view.RunnerPanel2;
import viskit.view.AssemblyViewFrame;
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
public class AssemblyControllerImpl extends mvcAbstractController implements AssemblyController, OpenAssembly.AssyChangeListener {

    static final Logger LOG = LogUtils.getLogger(AssemblyControllerImpl.class);
    private static int mutex = 0;
    Class<?> simEvSrcClass, simEvLisClass, propChgSrcClass, propChgLisClass;
    private String initialFile;
    private JTabbedPane runTabbedPane;
    private int runTabbedPaneIdx;

    /** The handler to run an assembly */
    private AssemblyRunnerPlug runner;

    /** Creates a new instance of AssemblyController */
    public AssemblyControllerImpl() {
        initConfig();
    }

    /**
     * Sets an initial assy file to open upon Viskit startup supplied by the
     * command line
     * @param fil the assy file to initially open upon startup
     */
    public void setInitialFile(String fil) {
        if (viskit.VStatics.debug) {
            System.out.println("Initial file set: " + fil);
        }
        initialFile = fil;
    }

    /** This method is for introducing Assemblies to compile from outside of
     * Viskit.  This method is not used from Viskit and must is required for
     * third party access.
     *
     * @param assyPath an assembly file to compile
     */
    public void compileAssembly(String assyPath) {
        LOG.debug("Compiling assembly: " + assyPath);
        File f = new File(assyPath);
        initialFile = assyPath;
        _doOpen(f);
        compileAssemblyAndPrepSimRunner();
    }

    @Override
    public void begin() {

        // The initialFile is set if we have stated a file "arg" upon startup
        // from the command line
        if (initialFile != null) {
            LOG.debug("Loading initial file: " + initialFile);
            compileAssembly(initialFile);
        } else {
            List<File> lis = getOpenAssyFileList(false);
            LOG.debug("Inside begin() and lis.size() is: " + lis.size());

            for (File f : lis) {
                _doOpen(f);
            }
        }

        recordProjFiles();
    }

    /** Information required by the EventGraphControllerImpl to see if an Assembly
     * file is already open.  Also checked internally by this class.
     * @param refresh flag to refresh the list from viskitConfig.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    public final List<File> getOpenAssyFileList(boolean refresh) {
        if (refresh || openAssemblies == null) {
            recordAssyFiles();
        }
        return openAssemblies;
    }

    @Override
    public void setAssemblyRunPane(JComponent runTabbedPane, int idx) {
        this.runTabbedPane = (JTabbedPane) runTabbedPane;
        runTabbedPaneIdx = idx;
        this.runTabbedPane.setEnabledAt(runTabbedPaneIdx, false);
    }

    private boolean checkSaveIfDirty() {
        if (localDirty) {
            StringBuilder sb = new StringBuilder("<html><center>Execution parameters have been modified.<br>(");

            for (Iterator<OpenAssembly.AssyChangeListener> itr = isLocalDirty.iterator(); itr.hasNext();) {
                sb.append(itr.next().getHandle());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // last comma-space
            sb.append(")<br>Choose yes if you want to stop this operation, then manually select<br>the indicated tab(s) to ");
            sb.append("save the execution parameters.");

            int yn = (((AssemblyView) getView()).genericAsk2Butts("Question", sb.toString(), "Stop and let me save",
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
                _doOpen(file);
            }
        }
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

            adjustRecentAssySet(file);
            markAssyFilesOpened();

            // replaces old fileWatchOpen(file);
            initOpenAssyWatch(file, mod.getJaxbRoot());
            openEventGraphs(file);

        } else {
            vaw.delTab(mod);
        }

        resetRedoUndoStatus();
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {

        AssemblyViewFrame view = (AssemblyViewFrame) getView();

        if (view.getCurrentVgacw() != null) {
            vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgacw().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Assy file opened as "open" in the app config file */
    private void markAssyFilesOpened() {

        // Mark every vAMod opened as "open"
        AssemblyModel[] openAlready = ((AssemblyView) getView()).getOpenModels();
        for (AssemblyModel vAMod : openAlready) {
            if (vAMod.getLastFile() != null) {
                String modelPath = vAMod.getLastFile().getAbsolutePath().replaceAll("\\\\", "/");
                markAssyConfigOpen(modelPath);
            }
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
    public void initOpenAssyWatch(File f, SimkitAssembly jaxbroot) {
        OpenAssembly.inst().setFile(f, jaxbroot);
    }

    /** @return the listener for this AssemblyControllerImpl */
    @Override
    public OpenAssembly.AssyChangeListener getAssemblyChangeListener() {
        return assyChgListener;
    }
    private boolean localDirty = false;
    private Set<OpenAssembly.AssyChangeListener> isLocalDirty = new HashSet<>();
    OpenAssembly.AssyChangeListener assyChgListener = new OpenAssembly.AssyChangeListener() {

        @Override
        public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param) {
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

                    ((EventGraphController) VGlobals.instance().getEventGraphController()).closeAll();

                    AssemblyModel vAMod = (AssemblyModel) getModel();
                    markAssyConfigClosed(vAMod.getLastFile());

                    AssemblyView view = (AssemblyView) getView();
                    view.delTab(vAMod);

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
                    LOG.warn("Program error AssemblyController.assyChanged");
            }
        }

        @Override
        public String getHandle() {
            return "Assembly Controller";
        }
    };

    @Override
    public String getHandle() {
        return assyChgListener.getHandle();
    }

    @Override
    public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param) {
        assyChgListener.assyChanged(action, source, param);
    }

    @Override
    public void addAssemblyFileListener(OpenAssembly.AssyChangeListener lis) {
        OpenAssembly.inst().addListener(lis);
    }

    @Override
    public void removeAssemblyFileListener(OpenAssembly.AssyChangeListener lis) {
        OpenAssembly.inst().removeListener(lis);
    }

    Set<mvcRecentFileListener> recentAssyListeners = new HashSet<>();

    @Override
    public void addRecentAssyFileSetListener(mvcRecentFileListener lis) {
        recentAssyListeners.add(lis);
    }

    @Override
    public void removeRecentAssyFileSetListener(mvcRecentFileListener lis) {
        recentAssyListeners.remove(lis);
    }

    /** Here we are informed of open Event Graphs */

    private void notifyRecentAssyFileListeners() {
        for (mvcRecentFileListener lis : recentAssyListeners) {
            lis.listChanged();
        }
    }

    Set<mvcRecentFileListener> recentProjListeners = new HashSet<>();

    @Override
    public void addRecentProjFileSetListener(mvcRecentFileListener lis) {
        recentProjListeners.add(lis);
    }

    @Override
    public void removeRecentProjFileSetListener(mvcRecentFileListener lis) {
        recentProjListeners.remove(lis);
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
            // Do nothing?
        }
    };

    /** @return a DirectoryChangeListener */
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
        GraphMetaData gmd = model.getMetaData();

        // Allow the user to type specific package names
        String packageName = gmd.packageName.replace(".", VStatics.getFileSeparator());
        File saveFile = view.saveFileAsk(packageName + VStatics.getFileSeparator() + gmd.name + ".xml", false);

        if (saveFile != null) {

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            gmd.name = n;
            model.changeMetaData(gmd); // might have renamed

            model.saveModel(saveFile);
            view.setSelectedAssemblyName(gmd.name);
            adjustRecentAssySet(saveFile);
            markAssyFilesOpened();
        }
    }

    @Override
    public void editGraphMetaData() {
        AssemblyModel mod = (AssemblyModel) getModel();
        if (mod == null) {return;}
        GraphMetaData gmd = mod.getMetaData();
        boolean modified =
                AssemblyMetaDataDialog.showDialog(VGlobals.instance().getAssemblyEditor(), gmd);
        if (modified) {
            ((AssemblyModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(gmd.name);
        }
    }
    private int egNodeCount = 0;
    private int adptrNodeCount = 0;
    private int pclNodeCount = 0;    // A little experiment in class introspection
    private static Field egCountField;
    private static Field adptrCountField;
    private static Field pclCountField;

    static { // do at class init time
        try {
            egCountField = AssemblyControllerImpl.class.getDeclaredField("egNodeCount");
            adptrCountField = AssemblyControllerImpl.class.getDeclaredField("adptrNodeCount");
            pclCountField = AssemblyControllerImpl.class.getDeclaredField("pclNodeCount");
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String shortEgName(String typeName) {
        return shortName(typeName, "evgr_", egCountField);
    }

    private String shortPCLName(String typeName) {
        return shortName(typeName, "lstnr_", pclCountField); // use same counter
    }

    private String shortAdapterName(String typeName) {
        return shortName(typeName, "adptr_", adptrCountField); // use same counter
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
            VGlobals.instance().initProjectHome();
            VGlobals.instance().createWorkDirectory();

            // For a brand new empty project open a default EG
            File[] egFiles = VGlobals.instance().getCurrentViskitProject().getEventGraphsDir().listFiles();
            if (egFiles.length == 0) {
                ((EventGraphController)VGlobals.instance().getEventGraphController()).newEventGraph();
            }
        }
    }

    @Override
    public void zipAndMailProject() {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            File projDir;
            File projZip;

            @Override
            public Void doInBackground() {

                projDir = VGlobals.instance().getCurrentViskitProject().getProjectRoot();
                projZip = new File(System.getProperty("user.dir"), projDir.getName() + ".zip");

                try {

                    // First, copy the debug.log to the project dir
                    FileIO.copyFile(ViskitConfig.V_DEBUG_LOG, new File(projDir, "debug.log"), true);
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
                    try {
                        url = new URL("mailto:" + VStatics.VISKIT_MAILING_LIST
                                + "?subject=Viskit%20Project%20Submission%20Name="
                                + projDir.getName() + "&body=see%20attachment");
                    } catch (MalformedURLException e) {
                        LOG.error(e);
                    }

                    String msg = "Please "
                            + "navigate to " + projZip.getParent() + " and "
                            + "email the " + projZip.getName() + " zip file to "
                            + "<b><a href=\"" + url.toString() + "\">" + VStatics.VISKIT_MAILING_LIST + "</a></b>"
                            + "<br/><br/>Click the link to open up an email form, then attach the zip file";

                    try {
                        Desktop.getDesktop().open(projZip.getParentFile());
                    } catch (IOException e) {
                        LOG.error(e);
                    }

                    LogUtils.showHyperlinkedDialog((Component) getView(), "Viskit Project: " + projDir.getName(), url, msg);

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
        if (VGlobals.instance().getCurrentViskitProject().isProjectOpen()) {
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
        ((EventGraphController) VGlobals.instance().getEventGraphController()).closeAll();
        ViskitConfig.instance().clearViskitConfig();
        clearRecentAssyFileList();
        ((EventGraphController) VGlobals.instance().getEventGraphController()).clearRecentEGFileSet();
        VGlobals.instance().getCurrentViskitProject().closeProject();
    }

    @Override
    public void openProject(File file) {
        VStatics.setViskitProjectFile(file);
        VGlobals.instance().createWorkDirectory();

        // Add our currently opened project to the recently opened projects list
        adjustRecentProjSet(VGlobals.instance().getCurrentViskitProject().getProjectRoot());
    }

    @Override
    public void newAssembly() {

        // Don't allow a new event graph to be created is a current project is
        // not open
        if (!VGlobals.instance().getCurrentViskitProject().isProjectOpen()) {return;}

        GraphMetaData oldGmd = null;
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

        GraphMetaData gmd = new GraphMetaData(mod);   // build a new one, specific to Assy
        if (oldGmd != null) {
            gmd.packageName = oldGmd.packageName;
        }

        boolean modified =
                AssemblyMetaDataDialog.showDialog(VGlobals.instance().getAssemblyEditor(), gmd);
        if (modified) {
            ((AssemblyModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((AssemblyView) getView()).setSelectedAssemblyName(gmd.name);

            // TODO: Implement this
//            ((AssemblyView)  getView()).setSelectedEventGraphDescription(gmd.description);
        } else {
            ((AssemblyView) getView()).delTab(mod);
        }
    }

    @Override
    public void messageUser(int typ, String title, String msg) // typ is one of JOptionPane types
    {   AssemblyView view = (AssemblyView) getView();
        if (view != null)
            ((AssemblyView) getView()).genericReport(typ, title, msg);
        else {
            JOptionPane.showMessageDialog(null, msg, title, typ);
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
        VGlobals.instance().quitAssemblyEditor();
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
        OpenAssembly.inst().doSendCloseAssy();
    }

    private void markAssyConfigClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int idx = 0;
        for (File key : recentAssyFileSet) {
            if (key.getPath().contains(f.getName())) {
                historyConfig.setProperty(ViskitConfig.ASSY_HISTORY_KEY + "(" + idx + ")[@open]", "false");
            }
            idx++;
        }
    }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    private void markAssyConfigOpen(String path) {

        int idx = 0;
        for (File tempPath : recentAssyFileSet) {

            if (tempPath.getPath().equals(path)) {
                historyConfig.setProperty(ViskitConfig.ASSY_HISTORY_KEY + "(" + idx + ")[@open]", "true");
            }
            idx++;
        }
    }

    private Point nextPoint = new Point(25, 25);
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
            } else if (o instanceof FileBasedAssyNode) {
                newFileBasedEventGraphNode((FileBasedAssyNode) o, getNextPoint());
                return;
            }
        }
        // Nothing selected or non-leaf
        messageUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select an Event Graph from the panel on the left.");
    }

    @Override
    public void newEventGraphNode(String typeName, Point p) {
        String shName = shortEgName(typeName);
        ((AssemblyModel) getModel()).newEventGraph(shName, typeName, p);
    }

    @Override
    public void newFileBasedEventGraphNode(FileBasedAssyNode xnode, Point p) {
        String shName = shortEgName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newEventGraphFromXML(shName, xnode, p);
    }

    @Override
    public void newPropChangeListenerNode() // menu click
    {
        Object o = ((AssemblyView) getView()).getSelectedPropChangeListener();

        if (o != null) {
            if (o instanceof Class<?>) {
                newPropChangeListenerNode(((Class<?>) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssyNode) {
                newFileBasedPropChangeListenerNode((FileBasedAssyNode) o, getNextPoint());
                return;
            }
        }
        // If nothing selected or a non-leaf
        messageUser(JOptionPane.ERROR_MESSAGE, "Can't create", "You must first select a Property Change Listener from the panel on the left.");
    }

    @Override
    public void newPropChangeListenerNode(String name, Point p) {
        String shName = shortPCLName(name);
        ((AssemblyModel) getModel()).newPropChangeListener(shName, name, p);
    }

    @Override
    public void newFileBasedPropChangeListenerNode(FileBasedAssyNode xnode, Point p) {
        String shName = shortPCLName(xnode.loadedClass);
        ((AssemblyModel) getModel()).newPropChangeListenerFromXML(shName, xnode, p);
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
    public void newSimEvListArc(Object[] nodes) {
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
    public void newPropChangeListArc(Object[] nodes) {
        // One and only one has to be a prop change listener
        AssemblyNode oA = (AssemblyNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForPropChangeArc(oA, oB);

        if (oArr == null) {
            messageUser(JOptionPane.ERROR_MESSAGE, "Incompatible connection", "The nodes must be a PropertyChangeListener and PropertyChangeSource combination.");
            return;
        }
        pcListenerEdgeEdit(((AssemblyModel) getModel()).newPropChangeEdge(oArr[0], oArr[1]));
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
        return VStatics.classForName(o.getType());
    }

    AssemblyNode[] orderPCLSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        // tbd, reloading these classes is needed right now as
        // we don't know if the workClassLoader is the same instance
        // as it used to be when these were originally loaded
        // the tbd here is to see if there can be a shared root loader
        simEvSrcClass = VStatics.classForName("simkit.SimEventSource");
        simEvLisClass = VStatics.classForName("simkit.SimEventListener");
        propChgSrcClass = VStatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = VStatics.classForName("java.beans.PropertyChangeListener");
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
        simEvSrcClass = VStatics.classForName("simkit.SimEventSource");
        simEvLisClass = VStatics.classForName("simkit.SimEventListener");
        propChgSrcClass = VStatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = VStatics.classForName("java.beans.PropertyChangeListener");
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
     * @param pclNode
     */
    @Override
    public void pcListenerEdit(PropChangeListenerNode pclNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((AssemblyView) getView()).doEditPclNode(pclNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changePclNode(pclNode);
            }
        } while (!done);
    }

    @Override
    public void evGraphEdit(EvGraphNode evNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((AssemblyView) getView()).doEditEvGraphNode(evNode);
            if (modified) {
                done = ((AssemblyModel) getModel()).changeEvGraphNode(evNode);
            }
        } while (!done);
    }

    @Override
    public void pcListenerEdgeEdit(PropChangeEdge pclEdge) {
        boolean modified = ((AssemblyView) getView()).doEditPclEdge(pclEdge);
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
    public void simEvListenerEdgeEdit(SimEvListenerEdge seEdge) {
        boolean modified = ((AssemblyView) getView()).doEditSimEvListEdge(seEdge);
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
        int x = 100, y = 100;
        int offset = 20;
        for (Object o : copyVector) {
            if (o instanceof AssemblyEdge) {
                continue;
            }

            Point2D p = new Point(x + (offset * copyCount), y + (offset * copyCount));
            if (o instanceof EvGraphNode) {
                String nm = ((ViskitElement) o).getName();
                String typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newEventGraph(nm + "-copy" + copyCount, typ, p);
            } else if (o instanceof PropChangeListenerNode) {
                String nm = ((ViskitElement) o).getName();
                String typ = ((ViskitElement) o).getType();
                ((AssemblyModel) getModel()).newPropChangeListener(nm + "-copy" + copyCount, typ, p);
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
            } else if (elem instanceof EvGraphNode) {
                EvGraphNode en = (EvGraphNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deleteEvGraphNode(en);
            } else if (elem instanceof PropChangeListenerNode) {
                PropChangeListenerNode en = (PropChangeListenerNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    removeEdge(ed);
                }
                ((AssemblyModel) getModel()).deletePropChangeListener(en);
            }
        }

        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    private void removeEdge(AssemblyEdge e) {
        if (e instanceof AdapterEdge) {
            ((AssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
        } else if (e instanceof PropChangeEdge) {
            ((AssemblyModel) getModel()).deletePropChangeEdge((PropChangeEdge) e);
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

        AssemblyViewFrame view = (AssemblyViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgacw().getUndoManager();

        Object[] roots = view.getCurrentVgacw().getRoots();
        redoGraphCell = (DefaultGraphCell) roots[roots.length - 1];

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        remove();

        if (!doRemove) {return;}

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentVgacw().getGraphLayoutCache());
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
        if (redoGraphCell instanceof org.jgraph.graph.Edge) {

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof AdapterEdge) {
                AdapterEdge ed = (AdapterEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoAdapterEdge(ed);
            } else if (redoGraphCell.getUserObject() instanceof PropChangeEdge) {
                PropChangeEdge ed = (PropChangeEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropChangeEdge(ed);
            } else {
                SimEvListenerEdge ed = (SimEvListenerEdge) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoSimEvLisEdge(ed);
            }
        } else {

            if (redoGraphCell.getUserObject() instanceof PropChangeListenerNode) {
                PropChangeListenerNode node = (PropChangeListenerNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoPropChangeListener(node);
            } else {
                EvGraphNode node = (EvGraphNode) redoGraphCell.getUserObject();
                ((AssemblyModel) getModel()).redoEventGraph(node);
            }
        }

        AssemblyViewFrame view = (AssemblyViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgacw().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentVgacw().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        AssemblyViewFrame view = (AssemblyViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgacw().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(view.getCurrentVgacw().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(view.getCurrentVgacw().getGraphLayoutCache()));

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
        if (source != null && source.length() > 0) {
            String className = vmod.getMetaData().packageName + "." + vmod.getMetaData().name;
            ((AssemblyView) getView()).showAndSaveSource(className, source);
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
        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xvt = new XMLValidationTool(f, new File(XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA));

        if ((xvt == null) || !xvt.isValidXML()) {

            // TODO: implement a Dialog pointing to the validationErrors.LOG
            return null;
        } else {
            LOG.info(f + " is valid XML\n");
        }

        SimkitAssemblyXML2Java x2j = null;
        try {
            x2j = new SimkitAssemblyXML2Java(f);
            x2j.unmarshal();
        } catch (FileNotFoundException e) {
            LOG.error(e);
        }
        return x2j.translate();
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
        XMLValidationTool xvt = new XMLValidationTool(x2j.getEventGraphFile(),
                new File(XMLValidationTool.LOCAL_EVENT_GRAPH_SCHEMA));

        if ((xvt == null) || !xvt.isValidXML()) {

            // TODO: implement a Dialog pointing to the validationErrors.LOG
            return null;
        } else {
            LOG.info(x2j.getEventGraphFile() + " is valid XML\n");
        }

        try {
            eventGraphSource = x2j.translate();
        } catch (Exception e) {
            LOG.error("Error building Java from " + x2j.getFileBaseName() +
                    ": " + e.getMessage() + ", erroneous event-graph xml found");
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
            ViskitProject viskitProj = VGlobals.instance().getCurrentViskitProject();

            // Create, or find the project's java source and package
            File srcPkg = new File(viskitProj.getSrcDir(), packagePath);
            if (!srcPkg.isDirectory()) {
                srcPkg.mkdirs();
            }
            File javaFile = new File(srcPkg, baseName + ".java");
            javaFile.createNewFile();

            fw = new FileWriter(javaFile);
            fw.write(src);

            // An error stream to write additional error info out to
            ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
            Compiler.setOutPutStream(baosOut);

            File classesDir = viskitProj.getClassesDir();

            LOG.info("Test compiling " + javaFile.getCanonicalPath());

            // This will create a class/package to place the .class file
            String diagnostic = Compiler.invoke(pkg, baseName, src);
            compileSuccess = diagnostic.equals(Compiler.COMPILE_SUCCESS_MESSAGE);
            if (compileSuccess) {

                LOG.info(diagnostic + "\n");
                return new File(classesDir, packagePath + baseName + ".class");
            } else {

                LOG.error(diagnostic + "\n");
                if (!baosOut.toString().isEmpty()) {
                    LOG.error(baosOut.toString() + "\n");
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
     * loads for the first time, or is saved during an edit
     *
     * @param xmlFile the EventGraph to package up
     * @return a package and file pair
     */
    public PkgAndFile createTemporaryEventGraphClass(File xmlFile) {
        PkgAndFile paf = null;
        try {
            SimkitXML2Java x2j = new SimkitXML2Java(xmlFile);
            x2j.unmarshal();

            boolean isEventGraph = x2j.getUnMarshalledObject() instanceof viskit.xsd.bindings.eventgraph.SimEntity;
            if (!isEventGraph) {
                LOG.debug("Is an Assembly: " + !isEventGraph);
                return null;
            }

            String src = buildJavaEventGraphSource(x2j);

            /* We may have forgotten a parameter required for a super class */
            if (src == null) {
                String msg = xmlFile + " did not compile.\n" +
                        "Please check that you have provided the correct parameters for any super classes";
                LOG.error(xmlFile + " " + msg);
                messageUser(JOptionPane.ERROR_MESSAGE, "Source code compilation error", msg);
                return null;
            }

            // If using plain Vanilla Viskit, don't compile diskit extended EGs
            // as diskit.jar won't be available
            String[] classPath = ((LocalBootLoader) VGlobals.instance().getWorkClassLoader()).getClassPath();
            boolean foundDiskit = false;
            for (String path : classPath) {
                if (path.contains("diskit.jar")) {
                    foundDiskit = !foundDiskit;
                    break;
                }
            }

            if (src.contains("diskit") && !foundDiskit) {
                FileBasedClassManager.instance().addCacheMiss(xmlFile);

                // TODO: Need to announce/recommend to the user to place
                // diskit.jar in the classpath, then restart Viskit
            } else {
                paf = compileJavaClassAndSetPackage(src);
            }
        } catch (FileNotFoundException e) {
            LOG.error("Error creating Java class file from " + xmlFile + ": " + e.getMessage() + "\n");
            FileBasedClassManager.instance().addCacheMiss(xmlFile);
        }
        return paf;
    }

    /** Path for EG and Assy compilation
     *
     * @param source the raw source to write to file
     * @return a package and file pair
     */
    private PkgAndFile compileJavaClassAndSetPackage(String source) {
        String pkg = null;
        if (source != null && !source.isEmpty()) {
            Pattern p = Pattern.compile("package.*;");
            Matcher m = p.matcher(source);
            if (m.find()) {
                String nuts = m.group();
                if (nuts.endsWith(";")) {
                    nuts = nuts.substring(0, nuts.length() - 1);
                }

                String[] sa = nuts.split("\\s");
                pkg = sa[1];
            }
            File f = compileJavaClassFromString(source);
            if (f != null) {
                return new PkgAndFile(pkg, f);
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
            messageUser(JOptionPane.ERROR_MESSAGE, "File System Error", e.getMessage());
            return;
        }
        model.saveModel(tFile);
    //todo switch to DOE
    }
    private String[] execStrings;

    // Known modelPath for Assy compilation
    @Override
    public void initAssemblyRun() {
        String src = produceJavaAssemblyClass(); // asks to save

        PkgAndFile paf = compileJavaClassAndSetPackage(src);
        if (paf != null) {
            File f = paf.f;
            String clNam = f.getName().substring(0, f.getName().indexOf('.'));
            clNam = paf.pkg + "." + clNam;

            // no longer necessary since we don't invoke
            // Runtime.exec to compile anymore
            String classPath = "";

            execStrings = buildExecStrings(clNam, classPath);
        } else {
            execStrings = null;
        }
    }

    @Override
    public void compileAssemblyAndPrepSimRunner() {

        // Prevent multiple pushes of the initialize sim run button
        mutex++;
        if (mutex > 1) {
            return;
        }

        // Prevent double clicking which will cause potential ClassLoader issues
        Runnable r = new Runnable() {

            @Override
            public void run() {
                ((AssemblyViewFrame) getView()).runButt.setEnabled(false);
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

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            public Void doInBackground() {

                // Compile and prep the execStrings
                initAssemblyRun();

                if (execStrings == null) {

                    if (VGlobals.instance().getActiveAssemblyModel() == null) {
                        messageUser(JOptionPane.WARNING_MESSAGE,
                            "Assembly File Not Opened",
                            "Please open an Assembly file");
                    } else {

                        URL url = null;
                        try {
                            url = new URL("mailto:" + VStatics.VISKIT_MAILING_LIST
                                    + "?subject=Assembly%20Source%20Generation%20Error&body=log%20output:");
                        } catch (MalformedURLException ex) {
                            LOG.error(ex);
                        }

                        String msg = "Assembly source generation/compilation failure.  <br/>Please "
                                + "navigate to " + ViskitConfig.V_DEBUG_LOG.getPath() + " and "
                                + "email the log to "
                                + "<b><a href=\"" + url.toString() + "\">" + VStatics.VISKIT_MAILING_LIST + "</a></b>"
                                + "<br/><br/>Click the link to open up an email form, then copy and paste the log's contents";

                        LogUtils.showHyperlinkedDialog((Component) getView(), "Assembly Error", url, msg);
                    }
                } else {

                    // Ensure a cleared Assembly Run panel upon every Assembly compile
                    RunnerPanel2 rp2 = VGlobals.instance().getRunPanel();
                    rp2.soutTA.setText(null);
                    rp2.soutTA.setText("Assembly output stream:" + rp2.lineEnd
                            + "----------------------" + rp2.lineEnd);

                    // Ensure changes to the Assembly Properties dialog get saved
                    save();

                    // Initializes a fresh class loader
                    runner.exec(execStrings);
                    runTabbedPane.setEnabledAt(runTabbedPaneIdx, true);

                    // reset
                    execStrings = null;
                }

                return null;
            }

            @Override
            protected void done() {
                try {

                    // Wait for the compile, save and Assy preps to finish
                    get();

                } catch (InterruptedException | ExecutionException e) {
                    LOG.error(e);
//                    e.printStackTrace();
                } finally {
                    ((AssemblyViewFrame) getView()).runButt.setEnabled(true);
                    mutex--;
                }
            }
        };
        worker.execute();
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
     * Maintain the above statics to match the order below.
     * @param className the name of the Assembly file to compile
     * @param classPath the current ClassLoader context
     * @return os exec array
     */
    private String[] buildExecStrings(String className, String classPath) {
        Vector<String> v = new Vector<>();
        String fsep = VStatics.getFileSeparator();

        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("java.home"));
        sb.append(fsep);
        sb.append("bin");
        sb.append(fsep);
        sb.append("java");
        v.add(sb.toString());        // 0

        v.add("-Xss2m");             // 1
        v.add("-Xincgc");            // 2
        v.add("-Xmx512m");           // 3
        v.add("-cp");                // 4
        v.add(classPath);            // 5
        v.add(className);            // 6

        v.add("" + ((AssemblyModel) getModel()).getMetaData().verbose); // 7
        v.add(((AssemblyModel) getModel()).getMetaData().stopTime);     // 8

        Vector<String> vec = ((AssemblyModel) getModel()).getDetailedOutputEntityNames();
        for (String s : vec) {
            v.add(s);                                                   // 9+
        }

        String[] ra = new String[v.size()];
        return v.toArray(ra);
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    @Override
    public void captureWindow() {

        AssemblyModel vmod = (AssemblyModel) getModel();
        String fileName = "AssemblyScreenCapture";
        if (vmod.getLastFile() != null) {
            fileName = vmod.getLastFile().getName();
        }

        File fil = ((AssemblyView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", true);
        if (fil == null) {
            return;
        }

        final Timer tim = new Timer(100, new timerCallback(fil, true));
        tim.setRepeats(false);
        tim.start();

        imgSaveCount = "" + (++imgSaveInt);
    }

    /** Provides an automatic capture of the currently loaded Assembly and stores
     * it to a specified location for inclusion in the generated Analyst Report
     *
     * @param assyImagePath an image modelPath to write the .png
     */
    public void captureAssemblyImage(String assyImagePath) {
        final Timer tim = new Timer(100, new timerCallback(new File(assyImagePath), false));
        tim.setRepeats(false);
        tim.start();
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
            Rectangle reg = component.getBounds();
            BufferedImage image = new BufferedImage(reg.width, reg.height, BufferedImage.TYPE_3BYTE_BGR);

            // Tell the jgraph component to draw into our memory
            component.paint(image.getGraphics());
            try {
                ImageIO.write(image, "png", fil);
            } catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) {
                final JFrame frame = new JFrame("Saved as " + fil.getName());
                ImageIcon ii = new ImageIcon(image);
                JLabel lab = new JLabel(ii);
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
            List<File> eGFiles = EventGraphCache.instance().getEventGraphFilesList();
            for (File file : eGFiles) {

                tempFile = file;

                // _doOpen checks if a tab is already opened
                ((EventGraphControllerImpl) VGlobals.instance().getEventGraphController())._doOpen(file);
            }
        } catch (Exception ex) {
            LOG.error("Opening EventGraph file: " + tempFile + " caused error: " + ex);
            messageUser(JOptionPane.WARNING_MESSAGE,
                    "EventGraph Opening Error",
                    "EventGraph file: " + tempFile + "\nencountered error: " + ex + " while loading."
                    );
            closeAll();
        }
    }

    /** Recent open file support */
    private static final int RECENTLISTSIZE = 15;
    private Set<File> recentAssyFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);
    private Set<File> recentProjFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an assembly file to add to the list
     */
    private void adjustRecentAssySet(File file) {
        for (Iterator<File> itr = recentAssyFileSet.iterator(); itr.hasNext();) {

            File f = itr.next();
            if (file.getPath().equals(f.getPath())) {
                itr.remove();
                break;
            }
        }

        recentAssyFileSet.add(file); // to the top
        saveAssyHistoryXML(recentAssyFileSet);
        notifyRecentAssyFileListeners();
    }

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file a project file to add to the list
     */
    public void adjustRecentProjSet(File file) {
        for (Iterator<File> itr = recentProjFileSet.iterator(); itr.hasNext();) {

            File f = itr.next();
            if (file.getPath().equals(f.getPath())) {
                itr.remove();
                break;
            }
        }

        recentProjFileSet.add(file); // to the top
        saveProjHistoryXML(recentProjFileSet);
        notifyRecentProjFileListeners();
    }

    private List<File> openAssemblies;

    @SuppressWarnings("unchecked")
    private void recordAssyFiles() {
        if (historyConfig == null) {initConfig();}
        openAssemblies = new ArrayList<>(4);
        List<String> valueAr = historyConfig.getList(ViskitConfig.ASSY_HISTORY_KEY + "[@value]");
        LOG.debug("_setAssyFileLists() valueAr size is: " + valueAr.size());
        int idx = 0;
        for (String s : valueAr) {
            if (recentAssyFileSet.add(new File(s))) {
                String op = historyConfig.getString(ViskitConfig.ASSY_HISTORY_KEY + "(" + idx + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes"))) {
                    openAssemblies.add(new File(s));
                }

                notifyRecentAssyFileListeners();
            }
            idx++;
        }
    }

    @SuppressWarnings("unchecked")
    private void recordProjFiles() {
        if (historyConfig == null) {initConfig();}
        List<String> valueAr = historyConfig.getList(ViskitConfig.PROJ_HISTORY_KEY + "[@value]");
        LOG.debug("recordProjFile valueAr size is: " + valueAr.size());
        for (String value : valueAr) {
            adjustRecentProjSet(new File(value));
        }
    }

    private void saveAssyHistoryXML(Set<File> recentFiles) {
        historyConfig.clearTree(ViskitConfig.RECENT_ASSY_CLEAR_KEY);
        int idx = 0;

        // The value's modelPath is already delimited with "/"
        for (File value : recentFiles) {
            historyConfig.setProperty(ViskitConfig.ASSY_HISTORY_KEY + "(" + idx + ")[@value]", value.getPath());
            idx++;
        }
        historyConfig.getDocument().normalize();
    }

    /** Always keep our project Hx until a user clears it manually
     *
     * @param recentFiles a Set of recently opened projects
     */
    private void saveProjHistoryXML(Set<File> recentFiles) {
        int ix = 0;
        for (File value : recentFiles) {
            historyConfig.setProperty(ViskitConfig.PROJ_HISTORY_KEY + "(" + ix + ")[@value]", value.getPath());
            ix++;
        }
        historyConfig.getDocument().normalize();
    }

    @Override
    public void clearRecentAssyFileList() {
        recentAssyFileSet.clear();
        saveAssyHistoryXML(recentAssyFileSet);
        notifyRecentAssyFileListeners();
    }

    @Override
    public Set<File> getRecentAssyFileSet() {
        return getRecentAssyFileSet(false);
    }

    private Set<File> getRecentAssyFileSet(boolean refresh) {
        if (refresh || recentAssyFileSet == null) {
            recordAssyFiles();
        }
        return recentAssyFileSet;
    }

    @Override
    public void clearRecentProjFileSet() {
        recentProjFileSet.clear();
        saveProjHistoryXML(recentProjFileSet);
        notifyRecentProjFileListeners();
    }

    @Override
    public Set<File> getRecentProjFileSet() {
        return getRecentProjFileSet(false);
    }

    private Set<File> getRecentProjFileSet(boolean refresh) {
        if (refresh || recentProjFileSet == null) {
            recordProjFiles();
        }
        return recentProjFileSet;
    }

    XMLConfiguration historyConfig;

    private void initConfig() {
        try {
            historyConfig = ViskitConfig.instance().getViskitAppConfig();
        } catch (Exception e) {
            LOG.error("Error loading history file: " + e.getMessage());
            LOG.warn("Recent file saving disabled");
            historyConfig = null;
        }
    }
}