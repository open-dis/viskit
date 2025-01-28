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

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.logging.log4j.Logger;

import org.jgraph.graph.DefaultGraphCell;

import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitStatics;
import viskit.jgraph.ViskitGraphUndoManager;
import viskit.model.*;
import viskit.mvc.MvcAbstractController;
import viskit.view.dialog.EventGraphMetadataDialog;
import viskit.view.AssemblyView;
import viskit.view.EventGraphViewFrame;
import viskit.view.EventGraphView;
import viskit.xsd.translator.eventgraph.SimkitXML2Java;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;

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
public class EventGraphControllerImpl extends MvcAbstractController implements EventGraphController {

    static final Logger LOG = LogUtils.getLogger(EventGraphControllerImpl.class);

    public EventGraphControllerImpl() {
        initConfig();
        initOpenEventGraphWatch();
    }

    @Override
    public void begin() {
        List<String> files = getOpenFileSet(false);

        if (!files.isEmpty()) {
            File file;

            // Open whatever Event Graphs were marked open on last closing
            for (String f : files) {
                file = new File(f);
                // Prevent project mismatch
                if (file.exists())
                    _doOpenEventGraph(file);
            }

        } else {

            // For a brand new empty project open a default Event Graph
            File[] eventGraphFiles = ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDir().listFiles();
            if (eventGraphFiles.length == 0) {
                newEventGraph();
            }
        }
    }

    @Override
    public void settings() {
        // placeholder for multi-tabbed combo app.
    }

    @Override
    public void newProject() {
        ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).newProject();
    }

    @Override
    public void zipAndMailProject() {
        ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).zipAndMailProject();
    }

    @Override
    public void newEventGraph() {

        // Don't allow a new event graph to be created if a current project is
        // not open
        if (!ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) {return;}

        GraphMetadata oldGmd = null;
        Model viskitModel = (Model) getModel();
        if (viskitModel != null) {
            oldGmd = viskitModel.getMetadata();
        }

        ModelImpl mod = new ModelImpl(this);
        mod.init();
        mod.newModel(null);

        // No model set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((EventGraphView) getView()).addTab(mod);

        // If we have models already opened, then use their package names for
        // this new Event Graph
        GraphMetadata gmd = mod.getMetadata();
        if (oldGmd != null) {
            gmd.packageName = oldGmd.packageName;
        }

        boolean modified =
                EventGraphMetadataDialog.showDialog((JFrame) getView(), gmd);
        if (modified) {

            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(gmd.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(gmd.description);

            // Bugfix 1398
            String msg =
                    "<html><body><p align='center'>Do you wish to start with a <b>\"Run\"</b> Event?</p></body></html>";
            String title = "Confirm Run Event";

            int ret = ((EventGraphView) getView()).genericAskYN(title, msg);
            boolean dirty = false;
            if (ret == JOptionPane.YES_OPTION) {
                buildNewNode(new Point(30, 60), "Run");
                dirty = true;
            }
            ((Model) getModel()).setDirty(dirty);
        } else {
           ((EventGraphView) getView()).deleteTab(mod);
        }
    }

    /**
     * Dialog operation
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue() {
        int yn = (((EventGraphView) getView()).genericAsk("Question", "Save modified graph?"));

        boolean retVal;

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();
                retVal = true;
                break;
            case JOptionPane.NO_OPTION:

                // No need to recompile
                if (((Model) getModel()).isDirty()) {
                    ((Model) getModel()).setDirty(false);
                }
                retVal = true;
                break;
            case JOptionPane.CANCEL_OPTION:
                retVal = false;
                break;

            // Something funny if we're here
            default:
                retVal = false;
                break;
        }
        return retVal;
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
                _doOpenEventGraph(file);
            }
        }
    }

    @Override
    public void openRecentEventGraph(File path) {
        _doOpenEventGraph(path);
    }

    // Package protected for the AssemblyControllerImpl's access to open EventGraphs
    void _doOpenEventGraph(File file) {
        EventGraphView viskitView = (EventGraphView) getView();
        ModelImpl modelImplementation = new ModelImpl(this);
        modelImplementation.init();
        viskitView.addTab(modelImplementation);

        Model[] openAlready = viskitView.getOpenModels();
        boolean isOpenAlready = false;
        String path;
        if (openAlready != null) {
            for (Model model : openAlready) {
                if (model.getLastFile() != null) {
                    path = model.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) {
                        isOpenAlready = true;
                        break;
                    }
                }
            }
        }
        if (modelImplementation.newModel(file) && !isOpenAlready) {

            // We may find one or more simkit.Priority(s) with numeric values vice
            // eneumerations in the Event Graph XML. Modify and save the Event Graph XML silently
            if (modelImplementation.isNumericPriority()) {
                save();
                modelImplementation.setNumericPriority(false);
            }

            viskitView.setSelectedEventGraphName(modelImplementation.getMetadata().name);
            if  (modelImplementation.getMetadata().description.isBlank())
                 viskitView.setSelectedEventGraphDescription(EventGraphViewFrame.DESCRIPTION_HINT);
            else viskitView.setSelectedEventGraphDescription(modelImplementation.getMetadata().description);
            adjustRecentEventGraphFileSet(file);
            markEventGraphFilesAsOpened();

            // Check for good compilation. TODO: Possibly grossly unnecessary since all classpaths and initial Event Graph parsing areadly took place in the project space during startup (tdn) 9/14/24
//            handleCompileAndSave(mod, file); <- possible source of Viskit barfing when opening a large set of Event Graphs
        } else {
            viskitView.deleteTab(modelImplementation); // Not a good open, tell view
        }

        resetRedoUndoStatus();
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {
        EventGraphViewFrame view = (EventGraphViewFrame) getView();

        if (view.getCurrentVgraphComponentWrapper() != null) {
            ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgraphComponentWrapper().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every Event Graph file opened as "open" in the app config file */
    private void markEventGraphFilesAsOpened() {
        Model[] openAlready = ((EventGraphView) getView()).getOpenModels();
        for (Model vMod : openAlready) {
            if (vMod.getLastFile() != null)
                markConfigOpen(vMod.getLastFile());
        }
    }

    // Support for informing listeners about open eventgraphs
    // Methods to implement a scheme where other modules will be informed of file changes
    // Would Java Beans do this with more or less effort?
    private DirectoryWatch dirWatch;
    private File watchDir;

    private void initOpenEventGraphWatch() {
        try { // TBD this may be obsolete
            watchDir = TempFileManager.createTempFile("eventGraphs", "current");   // actually creates
            watchDir = TempFileManager.createTempDir(watchDir);

            dirWatch = new DirectoryWatch(watchDir);
            dirWatch.setLoopSleepTime(1_000); // 1 sec
            dirWatch.startWatcher();
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    /**
     * Creates a new temporary Event Graph as a scratch pad
     * @param f the Event Graph to watch
     */
    private void fileWatchSave(File f) {
        fileWatchOpen(f);
    }

    /** A temporary location to store copies of EventGraphs in XML form.
     * This is to compare against any changes to and whether to re-cache the
     * MD5 hash generated for this Event Graph.
     * @param f the EventGraph file to generate MD5 hash for
     */
    private void fileWatchOpen(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        LOG.debug("f is: {} and ofile is: {}", f, ofile);
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

    Set<MvcRecentFileListener> recentListeners = new HashSet<>();

    @Override
    public void addRecentEventGraphFileListener(MvcRecentFileListener lis)
    {
      recentListeners.add(lis);
    }

    @Override
    public void removeRecentEventGraphFileListener(MvcRecentFileListener lis)
    {
      recentListeners.remove(lis);
    }

    private void notifyRecentFileListeners()
    {
      for(MvcRecentFileListener lis : recentListeners) {
            lis.listChanged();
        }
    }

    private static final int RECENTLISTSIZE = 15;
    private final Set<String> recentEventGraphFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);;

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an event graph file to add to the list
     */
    private void adjustRecentEventGraphFileSet(File file) {
        String f;
        for (Iterator<String> itr = recentEventGraphFileSet.iterator(); itr.hasNext();) {
            f = itr.next();
            if (file.getPath().equals(f)) {
                itr.remove();
                break;
            }
        }

        if (!recentEventGraphFileSet.contains(file.getPath()))
            recentEventGraphFileSet.add(file.getPath()); // to the top

        saveEventGraphHistoryXML(recentEventGraphFileSet);
        notifyRecentFileListeners();
    }

    private List<String> openEventGraphs;

    @SuppressWarnings("unchecked")
    private void recordEventGraphFiles() {
        if (historyConfig == null) {initConfig();}
        openEventGraphs = new ArrayList<>(4);
        List<Object> valueAr = historyConfig.getList(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "[@value]");
        int idx = 0;
        String op;
        String eventGraphFile;
        for (Object s : valueAr) {
            eventGraphFile = (String) s;
            if (recentEventGraphFileSet.add(eventGraphFile)) {
                op = historyConfig.getString(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true")))
                    openEventGraphs.add(eventGraphFile);

                notifyRecentFileListeners();
            }
            idx++;
        }
    }

    private void saveEventGraphHistoryXML(Set<String> recentFiles) {
        historyConfig.clearTree(ViskitConfiguration.RECENT_EVENTGRAPH_CLEAR_KEY);
        int idx = 0;

        // The value's path is already delimited with "/"
        for (String value : recentFiles) {
            historyConfig.setProperty(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@value]", value); // set relative path if available
            idx++;
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
    public Set<String> getRecentEventGraphFileSet() {
        return getRecentEventGraphFileSet(true);
    }

    private Set<String> getRecentEventGraphFileSet(boolean refresh) {
        if (refresh || recentEventGraphFileSet == null)
            recordEventGraphFiles();

        return recentEventGraphFileSet;
    }

    private List<String> getOpenFileSet(boolean refresh) {
        if (refresh || openEventGraphs == null)
            recordEventGraphFiles();

        return openEventGraphs;
    }

    @Override
    public void messageUser(int typ, String title, String msg) // typ is one of JOptionPane types
    {
        ((EventGraphView) getView()).genericReport(typ, title, msg);
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
        Model[] mods = ((EventGraphView) getView()).getOpenModels();
        for (Model mod : mods) {
            setModel((MvcModel) mod);

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
        dirWatch.stopWatcher();
    }

    @Override
    public void closeAll() {

        Model[] mods = ((EventGraphView) getView()).getOpenModels();
        for (Model mod : mods) {
            setModel((MvcModel) mod);
            close();
        }
    }

    @Override
    public void close() {
        if (preClose()) {
            postClose();
        }
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
        for (String key : recentEventGraphFileSet) {
            if (key.contains(f.getName()))
                historyConfig.setProperty(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]", "false");

            idx++;
        }
    }

    // NOTE: The open attribute is zeroed out for all recent files the first
    // time a file is opened
    private void markConfigOpen(File f) {
        int idx = 0;
        for (String key : recentEventGraphFileSet) {
            if (key.contains(f.getName()))
                historyConfig.setProperty(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]", "true");

            idx++;
        }
    }

    @Override
    public void save() {
        Model mod = (Model) getModel();
        File localLastFile = mod.getLastFile();
        if (localLastFile == null) {
            saveAs();
        } else {
            handleCompileAndSave(mod, localLastFile);
        }
    }

    @Override
    public void saveAs() {
        Model mod = (Model) getModel();
        EventGraphView view = (EventGraphView) getView();
        GraphMetadata graphMetadata = mod.getMetadata();
        if (graphMetadata.description.equals(EventGraphViewFrame.DESCRIPTION_HINT))
            graphMetadata.description = "";

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = view.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false);

        if (saveFile != null) {
            File localLastFile = mod.getLastFile();
            if (localLastFile != null) {
                fileWatchClose(localLastFile);
            }

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            graphMetadata.name = n;
            view.setSelectedEventGraphName(graphMetadata.name);
            mod.changeMetadata(graphMetadata); // might have renamed

            handleCompileAndSave(mod, saveFile);
            adjustRecentEventGraphFileSet(saveFile);
            markEventGraphFilesAsOpened();
        }
    }

    /**
     * Handles whether an XML Event Graph file gets its java source compiled and watched
     *
     * @param m the model of the XML Event Graph
     * @param f the XML file name to save to
     */
    private void handleCompileAndSave(Model m, File f) {
        if (m.saveModel(f)) {

            // We don't need to recurse since we know this is a file, but make sure
            // it's re-compiled and re-validated. model.isDirty will be set from
            // this call.
            ViskitGlobals.instance().getAssemblyEditor().addEventGraphsToLegoTree(f, false);
        }

        // Don't watch an XML file whose source couldn't be compiled correctly
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
        ((Model) getModel()).newSimParameter(name, type, initVal, comment);
    }

    @Override
    public void simParameterEdit(ViskitParameter param) {
        boolean modified = ((EventGraphView) getView()).doEditParameter(param);
        if (modified) {
            ((viskit.model.Model) getModel()).changeSimParameter(param);
        }
    }

    @Override
    public void codeBlockEdit(String s) {
        ((viskit.model.Model) getModel()).changeCodeBlock(s);
    }

    @Override
    public void stateVariableEdit(ViskitStateVariable var) {
        boolean modified = ((EventGraphView) getView()).doEditStateVariable(var);
        if (modified) {
            ((viskit.model.Model) getModel()).changeStateVariable(var);
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
        ((viskit.model.Model) getModel()).newStateVariable(name, type, initVal, comment);
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
            String s, msg = "";
            int localNodeCount = 0;  // different msg for edge delete
            for (Object o : selectionVector) {
                if (o != null && o instanceof EventNode) {
                    localNodeCount++;
                    s = o.toString();
                    s = s.replace('\n', ' ');
                    msg += ", \n" + s;
                }
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

    private int bias = 0;

    @Override
    public void paste() //-----------------
    {
        if (copyVector.isEmpty()) {
            return;
        }
        int x = 100, y = 100;
        int offset = 20;
        String nm;
        // We only paste un-attached nodes (at first)
        for (Object o : copyVector) {
            if (o instanceof Edge) {
                continue;
            }

            if (o != null) {
                nm = ((ViskitElement) o).getName();
                buildNewNode(new Point(x + (offset * bias), y + (offset * bias)), nm + "-copy");
                bias++;
            }
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void delete() {
        EventNode en;

        // Prevent concurrent modification
        Vector<Object> v = (Vector<Object>) selectionVector.clone();
        for (Object elem : v) {
            if (elem instanceof Edge) {
                removeEdge((Edge) elem);
            } else if (elem instanceof EventNode) {
                en = (EventNode) elem;
                for (ViskitElement ed : en.getConnections()) {
                    removeEdge((Edge) ed);
                }
                ((Model) getModel()).deleteEvent(en);
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
            ((Model) getModel()).deleteSchedulingEdge(e);
        } else {
            ((Model) getModel()).deleteCancelingEdge(e);
        }
    }

    /** Match the selected element as our candidate for a redo */
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

    @Override
    public void undo() {
        if (selectionVector.isEmpty())
            return;

        isUndo = true;

        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        Object[] roots = view.getCurrentVgraphComponentWrapper().getRoots();
        for (Object root : roots) {
            if (root instanceof DefaultGraphCell)
                redoGraphCell = ((DefaultGraphCell) root);
            if (selectionVector.firstElement().equals(redoGraphCell.getUserObject()))
                break;
        }

        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgraphComponentWrapper().getUndoManager();
        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentVgraphComponentWrapper().getGraphLayoutCache());
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
            if (redoGraphCell.getUserObject() instanceof SchedulingEdge) {
                SchedulingEdge ed = (SchedulingEdge) redoGraphCell.getUserObject();
                ((Model) getModel()).redoSchedulingEdge(ed);
            } else if (redoGraphCell.getUserObject() instanceof CancelingEdge) {
                CancelingEdge ed = (CancelingEdge) redoGraphCell.getUserObject();
                ((Model) getModel()).redoCancelingEdge(ed);
            }
        } else {
            EventNode node = (EventNode) redoGraphCell.getUserObject();
            ((Model) getModel()).redoEvent(node);
        }

        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgraphComponentWrapper().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentVgraphComponentWrapper().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: {}", ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        ViskitGraphUndoManager undoMgr = (ViskitGraphUndoManager) view.getCurrentVgraphComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(undoMgr.canUndo(view.getCurrentVgraphComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(undoMgr.canRedo(view.getCurrentVgraphComponentWrapper().getGraphLayoutCache()));

        isUndo = false;
    }

    @Override
    public void deleteSimParameter(ViskitParameter p) {
        ((Model) getModel()).deleteSimParameter(p);
    }

    @Override
    public void deleteStateVariable(ViskitStateVariable var) {
        ((Model) getModel()).deleteStateVariable(var);
    }

    private boolean checkSave() {
        Model mod = (Model) getModel();
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
        Model mod = (Model) getModel();
        if (mod == null) {return;}
        File localLastFile = mod.getLastFile();
        if (!checkSave() || localLastFile == null) {
            return;
        }

        SimkitXML2Java x2j = null;
        try {
            x2j = new SimkitXML2Java(localLastFile);
            x2j.unmarshal();
        } catch (FileNotFoundException fnfe) {
            LOG.error(fnfe);
        }

        String source = ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).buildJavaEventGraphSource(x2j);
        LOG.debug(source);
        if (source != null && source.length() > 0) {
            String className = mod.getMetadata().packageName + "." +
                    mod.getMetadata().name;
            ViskitGlobals.instance().getAssemblyEditor().showAndSaveSource(className, source, localLastFile.getName());
        }
    }

    @Override
    public void showXML() {
        if (!checkSave() || ((Model) getModel()).getLastFile() == null) {
            return;
        }

        ViskitGlobals.instance().getAssemblyEditor().displayXML(((Model) getModel()).getLastFile());
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
        ((viskit.model.Model) getModel()).newEvent(nm, p);
    }

    @Override
    public void buildNewSchedulingArc(Object[] nodes) //--------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((Model) getModel()).newSchedulingEdge(src, tar);
    }

    @Override
    public void buildNewCancelingArc(Object[] nodes) //--------------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((Model) getModel()).newCancelingEdge(src, tar);
    }

    /** Handles the menu selection for a new self-referential scheduling edge */
    public void newSelfRefSchedulingEdge() //--------------------------
    {
        if (selectionVector != null) {
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    ((Model) getModel()).newSchedulingEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    /** Handles the menu selection for a new self-referential canceling edge */
    public void newSelfRefCancelingEdge() {  //--------------------------
        if (selectionVector != null) {
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    ((Model) getModel()).newCancelingEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    @Override
    public void editGraphMetaData() {
        Model mod = (Model) getModel();
        if (mod == null) {return;}
        GraphMetadata gmd = mod.getMetadata();
        boolean modified =
                EventGraphMetadataDialog.showDialog((JFrame) getView(), gmd);
        if (modified) {
            ((Model) getModel()).changeMetadata(gmd);

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
                done = ((viskit.model.Model) getModel()).changeEvent(node);
            }
        } while (!done);
    }

    @Override
    public void schedulingArcEdit(Edge ed) {
        boolean modified = ((EventGraphView) getView()).doEditEdge(ed);
        if (modified) {
            ((viskit.model.Model) getModel()).changeSchedulingEdge(ed);
        }
    }

    @Override
    public void cancellingArcEdit(Edge ed) {
        boolean modified = ((EventGraphView) getView()).doEditCancelEdge(ed);
        if (modified) {
            ((viskit.model.Model) getModel()).changeCancelingEdge(ed);
        }
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    @Override
    public void captureWindow() {
        String fileName = "ViskitScreenCapture";

        // create and save the image
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();

        // Get only the jgraph part
        Component component = eventGraphViewFrame.getCurrentJgraphComponent();
        if (component == null) {return;}
        File localLastFile = ((Model) getModel()).getLastFile();
        if (localLastFile != null) {
            fileName = localLastFile.getName();
        }

        File fil = ((EventGraphView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", false);

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
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();

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
            _doOpenEventGraph(eventGraph);
            LOG.debug("eventGraph: " + eventGraph);

            // Now capture and store the Event Graph images
            if (itr.hasNext()) {
                imageFile = itr.next();
                LOG.debug("eventGraphImage is: " + imageFile);

                // Don't display an extra frame while taking snapshots
                tcb = new TimerCallback(imageFile, false, eventGraphViewFrame.getCurrentJgraphComponent());

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

                Runnable r = () -> {
                    frame.setVisible(true);
                };
                SwingUtilities.invokeLater(r);
            }
        }
    }
    XMLConfiguration historyConfig;

    /** This is the very first caller for getViskitAppConfiguration() upon Viskit startup */
    private void initConfig() {
        try {
            historyConfig = ViskitConfiguration.instance().getViskitAppConfiguration();
        } catch (Exception e) {
            LOG.error("Error loading history file: {}", e);
            LOG.warn("Recent file saving disabled");
            historyConfig = null;
        }
    }
}
