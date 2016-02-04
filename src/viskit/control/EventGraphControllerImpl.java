package viskit.control;

import actions.ActionIntrospector;
import edu.nps.util.DirectoryWatch;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitStatics;
import viskit.jgraph.vGraphUndoManager;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;
import viskit.mvc.mvcRecentFileListener;
import viskit.view.AssemblyView;
import viskit.view.EventGraphViewFrame;
import viskit.view.EventGraphView;
import viskit.view.dialog.EventGraphMetadataDialog;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 12:52:59 PM
 * @version $Id$

 This is the MVC controller for the Viskit app.  All user inputs come here, and this
 code decides what to do about it.  To add new events:
 1 add a new public Action BLAH field
 2 instantiate it in the constructor, mapping it to a handler (name)
 3 write the handler
 */
public class EventGraphControllerImpl extends mvcAbstractController implements EventGraphController {

    static final Logger LOG = LogUtils.getLogger(EventGraphControllerImpl.class);

    public EventGraphControllerImpl() {
        initConfig();
        initOpenEventGraphWatch();
    }

    @Override
    public void begin() {
        List<File> lis = getOpenFileSet(false);

        if (!lis.isEmpty()) {

            // Open whatever Event Graphs were marked open on last closing
            for (File f : lis) {
                _doOpen(f);
            }

        } else {

            // For a brand new empty project open a default Event Graph
            File[] eventGraphFiles = ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDirectory().listFiles();
            if (eventGraphFiles.length == 0) {
                newEventGraph();
            }
        }
    }

//    TODO delete, reflection method no longer needed
//	  @Override
//    public void settings() {
//        // placeholder for multi-tabbed combo app.
//    }

    @Override
    public void newProject() {
        ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).newProject();
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
		ViskitGlobals.instance().getAssemblyEditor().buildMenus(); // reset
    }

    @Override
    public void zipAndMailProject() {
        ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).zipAndMailProject();
    }

    @Override
    public void newEventGraph() {

        // Don't allow a new event graph to be created if a current project is not open
        if (!ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) 
		{
			messageToUser (JOptionPane.WARNING_MESSAGE, "No project directory", 
					"<html><p>New event graphs are only created within an open project.</p>" +
					"<p>Please open or create a project first.</p>");
			return;
		}

        GraphMetadata priorEventGraphMetadata = null;
        ModelImpl viskitModel = (ModelImpl) getModel();
        if (viskitModel != null) {
            priorEventGraphMetadata = viskitModel.getMetadata();
        }

        EventGraphModel eventGraphModel = new ModelImpl(this);
        eventGraphModel.init();
        eventGraphModel.newModel(null);

        // No model set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((EventGraphView) getView()).addTab(eventGraphModel);

        // If we have models already opened, then use their package names for his new EventGraph
        GraphMetadata newEventGraphMetadata = eventGraphModel.getMetadata();
        if (priorEventGraphMetadata != null) {
            newEventGraphMetadata.packageName = priorEventGraphMetadata.packageName;
        }

        boolean modified = EventGraphMetadataDialog.showDialog((JFrame) getView(), newEventGraphMetadata);
		
        if (modified) {

            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(newEventGraphMetadata.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(newEventGraphMetadata.description);

            // Bugfix 1398
            String msg =
                    "<html><body><p align='center'>Do you want " + newEventGraphMetadata.name + 
					" Event Graph execution to start with a <b>\"Run\"</b> Event?</p></body></html>";
            String title = "Confirm Run Event";

            int ret = ((EventGraphView) getView()).genericAskYN(title, msg);
            boolean dirty = false;
            if (ret == JOptionPane.YES_OPTION) {
                buildNewNode(new Point(30, 60), "Run");
                dirty = true;
            }
            ((EventGraphModel) getModel()).setDirty(dirty);
        } else {
           ((EventGraphView) getView()).deleteTab(eventGraphModel);
        }
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
    }

    /**
     * Dialog operation
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue() {
        int yesNo = (((EventGraphView) getView()).genericAsk("Question", "Save modified event graph?"));

        boolean returnValue;

        switch (yesNo) {
            case JOptionPane.YES_OPTION:
                save();
                returnValue = true;
                break;
            case JOptionPane.NO_OPTION:

                // No need to recompile
                if (((EventGraphModel) getModel()).isDirty()) {
                    ((EventGraphModel) getModel()).setDirty(false);
                }
                returnValue = true;
                break;
            case JOptionPane.CANCEL_OPTION:
                returnValue = false;
                break;

            // Something funny if we're here
            default:
                returnValue = false;
                break;
        }
        return returnValue;
    }

    @Override
    public void open() {
        // Bug fix: 1249
        File[] files = ((EventGraphView) getView()).openFilesAsk();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file != null) {
				if (file.getParentFile().getAbsolutePath().startsWith(ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot().getAbsolutePath()))
				{
					_doOpen(file);
					ViskitGlobals.instance().getViskitApplicationFrame().selectEventGraphEditorTab();
				}
				else 
				{
					messageToUser (JOptionPane.WARNING_MESSAGE, "Illegal directory for current project", 
							"<html><p>Event graphs must be within the currently open project.</p>" +
							"<p>&nbsp</p>" +
							"<p>Current project name: <b>" + ViskitGlobals.instance().getCurrentViskitProject().getProjectName() + "</b></p>" +
							"<p>Current project path: "    + ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot().getAbsolutePath() + "</p>" +
							"<p>&nbsp</p>" +
							"<p>Please choose an event graph in current project, or else open a different project.</p>");
					// TODO offer to copy?
					break;
				}
			}
        }
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
    }

    @Override
    public void openRecentEventGraph(File path) {
        _doOpen(path);
    }

    // Package protected for the AssemblyControllerImpl's access to open EventGraphs
    void _doOpen(File file) {

        EventGraphView eventGraphView = (EventGraphView) getView();
        ModelImpl model = new ModelImpl(this);
        model.init();
        eventGraphView.addTab(model);
        ViskitGlobals.instance().getEventGraphEditor().getSelectedPane().setToolTipText(model.getMetadata().description);

        EventGraphModel[] openAlready = eventGraphView.getOpenModels();
        boolean isOpenAlready = false;
        if (openAlready != null) {
            for (EventGraphModel eventGraphModel : openAlready) {
                if (eventGraphModel.getLastFile() != null) {
                    String path = eventGraphModel.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) {
                        isOpenAlready = true;
                    }
                }
            }
        }
        if (model.newModel(file) && !isOpenAlready) {

            // We may find one or more simkit.Priority(s) with numeric values vice
            // eneumerations in the EG XML.  Modify and save the EG XML silently
            if (model.isNumericPriority()) {
                save();
                model.setNumericPriority(false);
            }

            eventGraphView.setSelectedEventGraphName(model.getMetadata().name);
            eventGraphView.setSelectedEventGraphDescription(model.getMetadata().description);
            adjustRecentEventGraphFileSet(file);
            markEventGraphFilesAsOpened();

            // Check for good compilation
            handleCompileAndSave(model, file);
        } else {
            eventGraphView.deleteTab(model);   // Not a good open, tell view
        }

        resetRedoUndoStatus();
        ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // refresh
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {

        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();

        if (eventGraphViewFrame.getCurrentVgcw() != null) {
            vGraphUndoManager undoMgr = (vGraphUndoManager) eventGraphViewFrame.getCurrentVgcw().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every EG file opened as "open" in the app config file */
    private void markEventGraphFilesAsOpened() {

        EventGraphModel[] openAlready = ((EventGraphView) getView()).getOpenModels();
        for (EventGraphModel vMod : openAlready) {
            if (vMod.getLastFile() != null) {
                String modelPath = vMod.getLastFile().getAbsolutePath().replaceAll("\\\\", "/");
                markConfigOpen(modelPath);
            }
        }
    }

    // Support for informing listeners about open eventgraphs
    // Methods to implement a scheme where other modules will be informed of file changes
    // (Would Java Beans do this with more or less effort?
    private DirectoryWatch dirWatch;
    private File watchDir;

    private void initOpenEventGraphWatch() {
        try { // TBD this may be obsolete
            watchDir = TempFileManager.createTempFile("egs", "current");   // actually creates
            watchDir = TempFileManager.createTempDir(watchDir);

            dirWatch = new DirectoryWatch(watchDir);
            dirWatch.setLoopSleepTime(1_000); // 1 sec
            dirWatch.startWatcher();
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    /**
     * Creates a new temporary EG as a scratch pad
     * @param f the EG to watch
     */
    private void fileWatchSave(File f) {
        fileWatchOpen(f);
    }

    /** A temporary location to store copies of EventGraphs in XML form.
     * This is to compare against any changes to and whether to re-cache the
     * MD5 hash generated for this EG.
     * @param f the EventGraph file to generate MD5 hash for
     */
    private void fileWatchOpen(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        LOG.debug("f is: " + f + " and ofile is: " + ofile);
        try {
            Files.copy(f.toPath(), ofile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error(e);
//            e.printStackTrace();
        }
    }

    private void fileWatchClose(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        ofile.delete();
        AssemblyView view = (AssemblyView) ViskitGlobals.instance().getAssemblyController().getView();
        view.removeEventGraphFromLEGOTree(f);
    }

    @Override
    public void addEventGraphFileListener(DirectoryWatch.DirectoryChangeListener lis) {
        dirWatch.addListener(lis);
    }

    @Override
    public void removeEventGraphFileListener(DirectoryWatch.DirectoryChangeListener lis) {
        dirWatch.removeListener(lis);
    }

    Set<mvcRecentFileListener> recentListeners = new HashSet<>();

    @Override
    public void addRecentEventGraphFileListener(mvcRecentFileListener lis)
    {
      recentListeners.add(lis);
    }

    @Override
    public void removeRecentEventGraphFileListener(mvcRecentFileListener lis)
    {
      recentListeners.remove(lis);
    }

    private void notifyRecentFileListeners()
    {
      for(mvcRecentFileListener lis : recentListeners) {
            lis.listChanged();
        }
    }

    private static final int RECENTLISTSIZE = 15;
    private Set<File> recentEventGraphFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);;

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an event graph file to add to the list
     */
    private void adjustRecentEventGraphFileSet(File file) {
        for (Iterator<File> itr = recentEventGraphFileSet.iterator(); itr.hasNext();) {

            File f = itr.next();
            if (file.getPath().equals(f.getPath())) {
                itr.remove();
                break;
            }
        }

        recentEventGraphFileSet.add(file); // to the top
        saveEventGraphHistoryXML(recentEventGraphFileSet);
        notifyRecentFileListeners();
    }

    private List<File> openEventGraphs;

    @SuppressWarnings("unchecked")
    private void recordEgFiles() {
        if (historyConfig == null) {initConfig();}
        openEventGraphs = new ArrayList<>(4);
        List<String> valueAr = historyConfig.getList(ViskitConfiguration.EG_HISTORY_KEY + "[@value]");
        int i = 0;
        for (String s : valueAr) {
            if (recentEventGraphFileSet.add(new File(s))) {
                String op = historyConfig.getString(ViskitConfiguration.EG_HISTORY_KEY + "(" + i + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes"))) {
                    openEventGraphs.add(new File(s));
                }

                notifyRecentFileListeners();
            }
            i++;
        }
    }

    private void saveEventGraphHistoryXML(Set<File> recentFiles) {
        historyConfig.clearTree(ViskitConfiguration.RECENT_EVENT_GRAPH_CLEAR_KEY);
        int ix = 0;

        // The value's modelPath is already delimited with "/"
        for (File value : recentFiles) {
            historyConfig.setProperty(ViskitConfiguration.EG_HISTORY_KEY + "(" + ix + ")[@value]", value.getPath());
            ix++;
        }
        historyConfig.getDocument().normalize();
    }

    @Override
    public void clearRecentEventGraphFileSet() {
        recentEventGraphFileSet.clear();
        saveEventGraphHistoryXML(recentEventGraphFileSet);
        notifyRecentFileListeners();
    }

    @Override
    public Set<File> getRecentEventGraphFileSet() {
        return getRecentEGFileSet(false);
    }

    private Set<File> getRecentEGFileSet(boolean refresh) {
        if (refresh || recentEventGraphFileSet == null) {
            recordEgFiles();
        }
        return recentEventGraphFileSet;
    }

    private List<File> getOpenFileSet(boolean refresh) {
        if (refresh || openEventGraphs == null) {
            recordEgFiles();
        }
        return openEventGraphs;
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
    {
        ((EventGraphView) getView()).genericReport(dialogType, title, message);
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
        EventGraphModel[] mods = ((EventGraphView) getView()).getOpenModels();
        for (EventGraphModel mod : mods) {
            setModel((mvcModel) mod);

            // Check for a canceled exit
            if (!preClose()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void postQuit() {
        ViskitGlobals.instance().quitEventGraphEditor();
    }

    @Override
    public void closeAll() {

        EventGraphModel[] mods = ((EventGraphView) getView()).getOpenModels();
        for (EventGraphModel mod : mods) {
            setModel((mvcModel) mod);
            close();
        }
		// included in close(): ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // reset
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
        ModelImpl mod = (ModelImpl) getModel();
        if (mod == null) {
            return false;
        }

        if (mod.isDirty()) {
            return askToSaveAndContinue();
        }

        return true;
    }

    @Override
    public void postClose() {

        ModelImpl mod = (ModelImpl) getModel();
        if (mod.getLastFile() != null) {
            fileWatchClose(mod.getLastFile());
            markConfigClosed(mod.getLastFile());
        }

        ((EventGraphView) getView()).deleteTab(mod);
    }

    private void markConfigClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int idx = 0;
        for (File key : recentEventGraphFileSet) {
            if (key.getPath().contains(f.getName())) {
                historyConfig.setProperty(ViskitConfiguration.EG_HISTORY_KEY + "(" + idx + ")[@open]", "false");
            }
            idx++;
        }
    }

    // NOTE: The open attribute is zeroed out for all recent files the first
    // time a file is opened
    private void markConfigOpen(String path) {
        int idx = 0;
        for (File key : recentEventGraphFileSet) {
            if (key.getPath().contains(path)) {
                historyConfig.setProperty(ViskitConfiguration.EG_HISTORY_KEY + "(" + idx + ")[@open]", "true");
            }
            idx++;
        }
    }

    @Override
    public void save() {
        EventGraphModel mod = (EventGraphModel) getModel();
        File localLastFile = mod.getLastFile();
        if (localLastFile == null) {
            saveAs();
        } else {
            handleCompileAndSave(mod, localLastFile);
        }
    }

    @Override
    public void saveAs() {
        EventGraphModel mod = (EventGraphModel) getModel();
        EventGraphView view = (EventGraphView) getView();
        GraphMetadata gmd = mod.getMetadata();

        // Allow the user to type specific package names
        String packageName = gmd.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = view.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + gmd.name + ".xml", false, "Save Event Graph File As");

        if (saveFile != null) {
            File localLastFile = mod.getLastFile();
            if (localLastFile != null) {
                fileWatchClose(localLastFile);
            }

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            gmd.name = n;
            view.setSelectedEventGraphName(gmd.name);
            mod.changeMetadata(gmd); // might have renamed

            handleCompileAndSave(mod, saveFile);
            adjustRecentEventGraphFileSet(saveFile);
            markEventGraphFilesAsOpened();
        }
    }

    /**
     * Handles whether an XML EG file gets its java source compiled and watched
     *
     * @param m the model of the XML EG
     * @param f the XML file name to save to
     */
    private void handleCompileAndSave(EventGraphModel m, File f) {

        if (m.saveModel(f)) {

            // We don't need to recurse since we know this is a file, but make sure
            // it's re-compiled and re-validated.  model.isDirty will be set from
            // this call.
            ViskitGlobals.instance().getAssemblyEditor().addEventGraphsToLegoTree(f, false);
        }

        // Don't watch a an XML file whose source couldn't be compiled correctly
        if (!m.isDirty()) {
            fileWatchSave(f);
        }
    }

    @Override
    public void newSimParameter() //------------------------
    {
        ((EventGraphView) getView()).addParameterDialog();
    }

    @Override
    public void buildNewSimParameter(String name, String type, String initVal, String comment) {
        ((EventGraphModel) getModel()).newSimParameter(name, type, initVal, comment);
    }

    @Override
    public void simParameterEdit(vParameter param) {
        boolean modified = ((EventGraphView) getView()).doEditParameter(param);
        if (modified) {
            ((viskit.model.EventGraphModel) getModel()).changeSimParameter(param);
        }
    }

    @Override
    public void codeBlockEdit(String s) {
        ((viskit.model.EventGraphModel) getModel()).changeCodeBlock(s);
    }

    @Override
    public void stateVariableEdit(vStateVariable var) {
        boolean modified = ((EventGraphView) getView()).doEditStateVariable(var);
        if (modified) {
            ((viskit.model.EventGraphModel) getModel()).changeStateVariable(var);
        }
    }

    @Override
    public void newStateVariable() {
        ((EventGraphView) getView()).addStateVariableDialog();
    }

    // Comes in from view
    @Override
    public void buildNewStateVariable(String name, String type, String initVal, String comment) //----------------------------
    {
        ((viskit.model.EventGraphModel) getModel()).newStateVariable(name, type, initVal, comment);
    }

    private Vector<Object> selectionVector = new Vector<>();
    private Vector<Object> copyVector = new Vector<>();

    @Override
    public void selectNodeOrEdge(Vector<Object> v) //------------------------------------
    {
        selectionVector = v;
        boolean selected = nodeOrEdgeSelected(v);

        // Cut not supported
        ActionIntrospector.getAction(this, "cut").setEnabled(false);
        ActionIntrospector.getAction(this, "remove").setEnabled(selected);
        ActionIntrospector.getAction(this, "copy").setEnabled(nodeSelected());
        ActionIntrospector.getAction(this, "newSelfRefSchedulingEdge").setEnabled(selected);
        ActionIntrospector.getAction(this, "newSelfRefCancelingEdge").setEnabled(selected);
    }

    private boolean nodeCopied() {
        return nodeInVector(copyVector);
    }

    private boolean nodeSelected() {
        return nodeInVector(selectionVector);
    }

    private boolean nodeOrEdgeSelected(Vector v) {
        if (nodeInVector(v)) {
            return true;
        }
        for (Object o : v) {
            if (o instanceof Edge) {
                return true;
            }
        }
        return false;
    }

    private boolean nodeInVector(Vector v) {
        for (Object o : v) {
            if (o instanceof EventNode) {
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
            int localNodeCount = 0;  // different msg for edge delete
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    localNodeCount++;
                }
                String s = o.toString();
                s = s.replace('\n', ' ');
                msg += ", \n" + s;
            }
            if (msg.length() > 3) {
                msg = msg.substring(3);
            } // remove leading stuff

            String specialNodeMsg = (localNodeCount > 0 ? "\n(All unselected, but attached edges are permanently removed.)" : "");
            doRemove = ((EventGraphView) getView()).genericAskYN("Remove element(s)?", "Confirm remove " + msg + "?" + specialNodeMsg) == JOptionPane.YES_OPTION;
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
    public void copy() //----------------
    {
        if (!nodeSelected()) {
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

    private int bias = 0;

    @Override
    public void paste() //-----------------
    {
        if (copyVector.isEmpty()) {
            return;
        }
        int x = 100, y = 100;
        int offset = 20;
        // We only paste un-attached nodes (at first)
        for (Object o : copyVector) {
            if (o instanceof Edge) {
                continue;
            }
            String nm = ((ViskitElement) o).getName();
            buildNewNode(new Point(x + (offset * bias), y + (offset * bias)), nm + "-copy");
            bias++;
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void delete() {

        // Prevent concurrent modification
        Vector<Object> v = (Vector<Object>) selectionVector.clone();
        for (Object elem : v) {
            if (elem instanceof Edge) {
                removeEdge((Edge) elem);
            } else if (elem instanceof EventNode) {
                EventNode en = (EventNode) elem;
                for (ViskitElement ed : en.getConnections()) {
                    removeEdge((Edge) ed);
                }
                ((EventGraphModel) getModel()).deleteEvent(en);
            }
        }

        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    /** Removes the JAXB (XML) binding from the model for this edge
     *
     * @param e the edge to remove
     */
    private void removeEdge(Edge e) {
        if (e instanceof SchedulingEdge) {
            ((EventGraphModel) getModel()).deleteSchedulingEdge(e);
        } else {
            ((EventGraphModel) getModel()).deleteCancelingEdge(e);
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

        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgcw().getUndoManager();

        Object[] roots = view.getCurrentVgcw().getRoots();
        redoGraphCell = (DefaultGraphCell) roots[roots.length - 1];

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        remove();

        if (!doRemove) {return;}

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentVgcw().getGraphLayoutCache());
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
            if (redoGraphCell.getUserObject() instanceof SchedulingEdge) {
                SchedulingEdge ed = (SchedulingEdge) redoGraphCell.getUserObject();
                ((EventGraphModel) getModel()).redoSchedulingEdge(ed);
            } else {
                CancelingEdge ed = (CancelingEdge) redoGraphCell.getUserObject();
                ((EventGraphModel) getModel()).redoCancelingEdge(ed);
            }
        } else {
            EventNode node = (EventNode) redoGraphCell.getUserObject();
            ((EventGraphModel) getModel()).redoEvent(node);
        }

        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgcw().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentVgcw().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentVgcw().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(view.getCurrentVgcw().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(view.getCurrentVgcw().getGraphLayoutCache()));

        isUndo = false;
    }

    @Override
    public void deleteSimParameter(vParameter p) {
        ((EventGraphModel) getModel()).deleteSimParameter(p);
    }

    @Override
    public void deleteStateVariable(vStateVariable var) {
        ((EventGraphModel) getModel()).deleteStateVariable(var);
    }

    private boolean checkSave() {
        EventGraphModel mod = (EventGraphModel) getModel();
        if (mod == null) {return false;}
        if (mod.isDirty() || mod.getLastFile() == null) {
            String msg = "The model will be saved.\nContinue?";
            String title = "Confirm";
            int ret = ((EventGraphView) getView()).genericAskYN(title, msg);
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            save();
        }
        return true;
    }

    @Override
    public void generateJavaSource() {
        EventGraphModel mod = (EventGraphModel) getModel();
        if (mod == null) {return;}
        File localLastFile = mod.getLastFile();
        if (!checkSave() || localLastFile == null) {
            return;
        }

        SimkitXML2Java simkitXML2Java = null;
        try {
            simkitXML2Java = new SimkitXML2Java(localLastFile);
            simkitXML2Java.unmarshal();
        } catch (FileNotFoundException fnfe)
		{	
			String message = localLastFile.getName() + " updated Event Graph Java file not found when unmarshalling";
			messageToUser(JOptionPane.ERROR_MESSAGE, "Event Graph Java file not found", message);
            LOG.error(fnfe);
        }

        String source = ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).buildJavaEventGraphSource(simkitXML2Java);
        LOG.debug(source);
        if (source != null && source.length() > 0) {
            String className = mod.getMetadata().packageName + "." +
                    mod.getMetadata().name;
            ViskitGlobals.instance().getAssemblyEditor().showAndSaveSource(className, source, localLastFile.getName());
        }
    }

    @Override
    public void showXML() {
        if (!checkSave() || ((EventGraphModel) getModel()).getLastFile() == null) {
            return;
        }

        ViskitGlobals.instance().getAssemblyEditor().displayXML(((EventGraphModel) getModel()).getLastFile());
    }

    @Override
    public void eventList() {
        // not used
        if (viskit.ViskitStatics.debug) {
            System.out.println("EventListAction in " + this);
        }
    }
    private int nodeCount = 0;

    @Override
    public void newNode() //-------------------
    {
        buildNewNode(new Point(100, 100));
    }

    @Override
    public void buildNewNode(Point p) //--------------------------
    {
        buildNewNode(p, "evnt_" + nodeCount++);
    }

    @Override
    public void buildNewNode(Point p, String nm) //------------------------------------
    {
        ((viskit.model.EventGraphModel) getModel()).newEvent(nm, p);
    }

    @Override
    public void buildNewSchedulingArc(Object[] nodes) //--------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((EventGraphModel) getModel()).newSchedulingEdge(src, tar);
    }

    @Override
    public void buildNewCancelingArc(Object[] nodes) //--------------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((EventGraphModel) getModel()).newCancelingEdge(src, tar);
    }

    /** Handles the menu selection for a new self-referential scheduling edge */
    public void newSelfRefSchedulingEdge() //--------------------------
    {
        if (selectionVector != null) {
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    ((EventGraphModel) getModel()).newSchedulingEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    /** Handles the menu selection for a new self-referential canceling edge */
    public void newSelfRefCancelingEdge() {  //--------------------------
        if (selectionVector != null) {
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    ((EventGraphModel) getModel()).newCancelingEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    @Override
    public void editGraphMetadata() {
        EventGraphModel mod = (EventGraphModel) getModel();
        if (mod == null) {return;}
        GraphMetadata gmd = mod.getMetadata();
        boolean modified =
                EventGraphMetadataDialog.showDialog((JFrame) getView(), gmd);
        if (modified) {
            ((EventGraphModel) getModel()).changeMetadata(gmd);

            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(gmd.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(gmd.description);
        }
    }

    @Override
    public void nodeEdit(EventNode node) // shouldn't be required
    //----------------------------------
    {
        boolean done;
        boolean modified;
        do {
            done = true;
            modified = ((EventGraphView) getView()).doEditNode(node);
            if (modified) {
                done = ((viskit.model.EventGraphModel) getModel()).changeEvent(node);
            }
        } while (!done);
    }

    @Override
    public void schedulingArcEdit(Edge ed) {
        boolean modified = ((EventGraphView) getView()).doEditEdge(ed);
        if (modified) {
            ((viskit.model.EventGraphModel) getModel()).changeSchedulingEdge(ed);
        }
    }

    @Override
    public void cancellingArcEdit(Edge ed) {
        boolean modified = ((EventGraphView) getView()).doEditCancelEdge(ed);
        if (modified) {
            ((viskit.model.EventGraphModel) getModel()).changeCancelingEdge(ed);
        }
    }
    private String imgSaveCount = "";
    private int    imgSaveInt = -1;

    @Override
    public void windowImageCapture() {
        String fileName = "EventGraphDiagram";

        // create and save the image
        EventGraphViewFrame egvf = (EventGraphViewFrame) getView();

        // Get only the jgraph part
        Component component = egvf.getCurrentJgraphComponent();
        if (component == null) {return;}
        File localLastFile = ((EventGraphModel) getModel()).getLastFile();
        if (localLastFile != null) {
            fileName = localLastFile.getName();
        }

        File fil = ((EventGraphView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", false, "Save Event Graph Diagram Image");

        if (fil == null) {
            return;
        }

        final Timer tim = new Timer(100, new TimerCallback(fil, true, component));
        tim.setRepeats(false);
        tim.start();

        imgSaveCount = "" + (++imgSaveInt);
    }

    @Override
    public void captureEventGraphImages(List<File> eventGraphs, List<File> eventGraphImages) {
        Iterator<File> itr = eventGraphImages.listIterator(0);

        File imageFile;
        TimerCallback tcb;

        // create and save the image
        EventGraphViewFrame egvf = (EventGraphViewFrame) getView();

        /* If another run is to be performed with the intention of generating
         * an Analyst Report, prevent the last Event Graph open (from prior group
         * if any open) from being the dominant (only) screen shot taken.  In
         * other words, if the prior group of Event Graphs were open on the same
         * Assembly, then all of the screen shots would be of the last Event Graph
         * that was opened either manually, or automatically by the below process.
         */
        closeAll();

        // Each Event Graph needs to be opened first
        for (File eventGraph : eventGraphs) {
            _doOpen(eventGraph);
            LOG.debug("eventGraph: " + eventGraph);

            // Now capture and store the Event Graph images
            if (itr.hasNext()) {
                imageFile = itr.next();
                LOG.debug("eventGraphImage is: " + imageFile);

                // Don't display an extra frame while taking snapshots
                tcb = new TimerCallback(imageFile, false, egvf.getCurrentJgraphComponent());

                // Make sure we have a directory ready to receive these images
                if (!imageFile.getParentFile().isDirectory()) {
                    imageFile.getParentFile().mkdirs();
                }

                // Fire this quickly as another Event Graph will immediately load
                final Timer tim = new Timer(0, tcb);
                tim.setRepeats(false);
                tim.start();
            }
        }
    }

    class TimerCallback implements ActionListener {

        File fil;
        boolean display;
        JFrame frame;
        Component component;

        TimerCallback(File f, boolean b, Component component) {
            fil = f;
            display = b;
            this.component = component;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {

            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
                LOG.debug("CurrentJgraphComponent is a JScrollPane: " + component);
            }
            Rectangle rec = component.getBounds();
            Image image = new BufferedImage(rec.width, rec.height, BufferedImage.TYPE_4BYTE_ABGR_PRE);

            // Tell the jgraph component to draw into memory
            try {
                component.paint(image.getGraphics());
            } catch (NullPointerException e) {
                LOG.error(e);
            }
            // NPEs are happening in JGraph

            try {
                ImageIO.write((RenderedImage)image, "png", fil);
            } catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) {
                frame = new JFrame("Saved as " + fil.getName());
                Icon ii = new ImageIcon(image);
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
    XMLConfiguration historyConfig;

    /** This is the very first caller for getViskitApplicationXMLConfiguration() upon Viskit startup */
    private void initConfig() {
        try {
            historyConfig = ViskitConfiguration.instance().getViskitApplicationXMLConfiguration();
        } catch (Exception e) {
            LOG.error("Error loading history file: " + e.getMessage());
            LOG.warn("Recent file saving disabled");
            historyConfig = null;
        }
    }
}
