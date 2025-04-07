package viskit.control;

import actions.ActionIntrospector;

import edu.nps.util.DirectoryWatch;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jgraph.graph.DefaultGraphCell;

import viskit.ViskitGlobals;
import viskit.ViskitUserConfiguration;
import viskit.ViskitStatics;
import static viskit.ViskitStatics.DESCRIPTION_HINT;
import static viskit.control.AssemblyControllerImpl.LOG;
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
public class EventGraphControllerImpl extends MvcAbstractController implements EventGraphController 
{
    static final Logger LOG = LogManager.getLogger();

    public EventGraphControllerImpl() {
        initConfig();
        initOpenEventGraphWatch();
    }

    @Override
    public void begin() {
        List<String> files = getOpenFileSetList(false);

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
            File[] eventGraphFiles = ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory().listFiles();
            if (eventGraphFiles.length == 0) {
                newEventGraph();
            }
        }
    }

    @Override
    public void showViskitUserPreferences() 
    {
        ViskitGlobals.instance().getAssemblyController().showViskitUserPreferences();
    }

    @Override
    public void newProject() {
        ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).newProject();
    }

    @Override
    public void zipAndMailProject() {
        ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).zipProject();
    }

    @Override
    public void newEventGraph()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();

        // Don't allow a new event graph to be created if a current project is
        // not open
        if (!ViskitGlobals.instance().getViskitProject().isProjectOpen()) {return;}

        GraphMetadata oldGraphMetadata = null;
        Model viskitModel = (Model) getModel();
        if (viskitModel != null) {
            oldGraphMetadata = viskitModel.getMetadata();
        }
        ModelImpl newModelImpl = new ModelImpl(this);
        newModelImpl.initialize();
        newModelImpl.newModel(null);

        // No model set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((EventGraphView) getView()).addTab(newModelImpl);

        // If we have models already opened, then use their package names for
        // this new Event Graph
        GraphMetadata graphMetadata = newModelImpl.getMetadata();
        if (oldGraphMetadata != null) {
            graphMetadata.packageName = oldGraphMetadata.packageName;
        }

        boolean modified =
                EventGraphMetadataDialog.showDialog((JFrame) getView(), graphMetadata);
        if (modified) {

            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(graphMetadata.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(ViskitStatics.emptyIfNull(graphMetadata.description));

            // Bugfix 1398
            String message =
                    "<html><body><p align='center'>Do you wish to start with a <b>\"Run\"</b> Event?</p></body></html>";
            String title = "Confirm Run Event";

            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            boolean dirty = false;
            if (returnValue == JOptionPane.YES_OPTION) {
                buildNewNode(new Point(30, 60), "Run");
                dirty = true;
            }
            ((Model) getModel()).setDirty(dirty);
        } else {
           ((EventGraphView) getView()).deleteTab(newModelImpl);
        }
    }

    /**
     * Dialog operation
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        int yn = (ViskitGlobals.instance().getMainFrame().genericAsk("Question", "Save modified graph?"));

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
    public void open()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        // Bug fix: 1249
        File[] files = ViskitGlobals.instance().getEventGraphViewFrame().openFilesAsk();
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
    void _doOpenEventGraph(File file)
    {
        EventGraphView eventGraphView = (EventGraphView) getView();
        ModelImpl modelImplementation = new ModelImpl(this);
        modelImplementation.initialize();
        eventGraphView.addTab(modelImplementation);

        Model[] openAlreadyModelArray = eventGraphView.getOpenModels();
        boolean isOpenAlready = false;
        String path;
        if (openAlreadyModelArray != null) {
            for (Model model : openAlreadyModelArray) {
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

            eventGraphView.setSelectedEventGraphName(modelImplementation.getMetadata().name);
            modelImplementation.getMetadata().description = ViskitStatics.emptyIfNull(modelImplementation.getMetadata().description);
            if  (modelImplementation.getMetadata().description.isBlank())
                 eventGraphView.setSelectedEventGraphDescription(DESCRIPTION_HINT);
            else eventGraphView.setSelectedEventGraphDescription(modelImplementation.getMetadata().description);
            adjustRecentEventGraphFileSet(file);
            markEventGraphFilesAsOpened();

            // Check for good compilation. TODO: Possibly grossly unnecessary since all classpaths and initial Event Graph parsing areadly took place in the project space during startup (tdn) 9/14/24
//            handleCompileAndSave(mod, file); <- possible source of Viskit barfing when opening a large set of Event Graphs
        } 
        else {
            eventGraphView.deleteTab(modelImplementation); // Not a good open, tell view
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
    private DirectoryWatch directoryWatch;
    private File watchDirectoryFile;

    private void initOpenEventGraphWatch()
    {
        try { // TBD this may be obsolete
            watchDirectoryFile = TempFileManager.createTempFile("eventGraphs", "current");   // actually creates
            watchDirectoryFile = TempFileManager.createTempDir(watchDirectoryFile);

            directoryWatch = new DirectoryWatch(watchDirectoryFile);
            directoryWatch.setLoopSleepTime(1_000); // 1 sec
            directoryWatch.startWatcher();
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
        File ofile = new File(watchDirectoryFile, nm);
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
        File ofile = new File(watchDirectoryFile, nm);
        ofile.delete();
        AssemblyView view = (AssemblyView) ViskitGlobals.instance().getAssemblyController().getView();
        view.removeEventGraphFromLEGOTree(f);
    }

    @Override
    public void addEventGraphFileListener(DirectoryWatch.DirectoryChangeListener lis) {
        directoryWatch.addListener(lis);
    }

    @Override
    public void removeEventGraphFileListener(DirectoryWatch.DirectoryChangeListener lis) {
        directoryWatch.removeListener(lis);
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

    private List<String> openEventGraphsList;

    @SuppressWarnings("unchecked")
    private void recordEventGraphFiles() {
        if (historyXMLConfiguration == null) {initConfig();}
        openEventGraphsList = new ArrayList<>(4);
        List<Object> valueAr = historyXMLConfiguration.getList(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "[@value]");
        int idx = 0;
        String op;
        String eventGraphFile;
        for (Object s : valueAr) {
            eventGraphFile = (String) s;
            if (recentEventGraphFileSet.add(eventGraphFile)) {
                op = historyXMLConfiguration.getString(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true")))
                    openEventGraphsList.add(eventGraphFile);

                notifyRecentFileListeners();
            }
            idx++;
        }
    }

    private void saveEventGraphHistoryXML(Set<String> recentFilesSet) {
        historyXMLConfiguration.clearTree(ViskitUserConfiguration.RECENT_EVENTGRAPH_CLEAR_KEY);
        int index = 0;

        // The value's path is already delimited with "/"
        for (String nextRecentFile : recentFilesSet) {
            historyXMLConfiguration.setProperty(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + index + ")[@value]", nextRecentFile); // set relative path if available
            index++;
        }
        historyXMLConfiguration.getDocument().normalize();
    }

    @Override
    public void clearRecentEventGraphFileSet()
    {
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

    private List<String> getOpenFileSetList(boolean refresh) {
        if (refresh || openEventGraphsList == null)
            recordEventGraphFiles();

        return openEventGraphsList;
    }

    @Override
    public void messageUser(int messageType, String title, String message) // typ is one of JOptionPane types
    {
        ViskitGlobals.instance().getMainFrame().genericReport(messageType, title, message);
    }

    @Override
    public void quit()
    {
        if (preQuit()) {
            postQuit();
        }
    }

    @Override
    public boolean preQuit() {

        // Check for dirty models before exiting
        Model[] modelArray = ((EventGraphView) getView()).getOpenModels();
        for (Model nextModel : modelArray) {
            setModel((MvcModel) nextModel);

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
        directoryWatch.stopWatcher();
    }

    @Override
    public void closeAll() {

        Model[] models = ((EventGraphView) getView()).getOpenModels();
        for (Model model : models) {
            setModel((MvcModel) model);
            close();
        }
    }

    @Override
    public void close()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
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
                historyXMLConfiguration.setProperty(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]", "false");

            idx++;
        }
    }

    // NOTE: The open attribute is zeroed out for all recent files the first
    // time a file is opened
    private void markConfigOpen(File f) {
        int idx = 0;
        for (String key : recentEventGraphFileSet) {
            if (key.contains(f.getName()))
                historyXMLConfiguration.setProperty(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]", "true");

            idx++;
        }
    }

    @Override
    public void save()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        Model eventGraphModel = (Model) getModel();
        if (eventGraphModel == null)
            eventGraphModel = ViskitGlobals.instance().getActiveEventGraphModel();
        if (eventGraphModel == null)
        {
            LOG.error("unable to save() null eventGraphModel");
            return;
        }
        File localLastFile = eventGraphModel.getLastFile();
        if (localLastFile == null) {
            saveAs();
        } 
        else {
            handleCompileAndSave(eventGraphModel, localLastFile);
        }
    }

    @Override
    public void saveAs()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        Model model = (Model) getModel();
        EventGraphView eventGraphView = (EventGraphView) getView();
        GraphMetadata graphMetadata = model.getMetadata();
        if ((graphMetadata.description == null) || graphMetadata.description.equals(DESCRIPTION_HINT))
            graphMetadata.description = "";

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = eventGraphView.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false,
                                         "Save Event Graph as...");

        if (saveFile != null) {
            File localLastFile = model.getLastFile();
            if (localLastFile != null) {
                fileWatchClose(localLastFile);
            }

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            graphMetadata.name = n;
            eventGraphView.setSelectedEventGraphName(graphMetadata.name);
            model.changeMetadata(graphMetadata); // might have renamed

            handleCompileAndSave(model, saveFile);
            adjustRecentEventGraphFileSet(saveFile);
            markEventGraphFilesAsOpened();
        }
    }

    /**
     * Handles whether an XML Event Graph file gets its java source compiled and watchedmodel@param m the model of the XML Event Graph
     * @param file the XML file name to save to
     */
    private void handleCompileAndSave(Model model, File file)
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        if (model.saveModel(file))
        {
            // We don't need to recurse since we know this is a file, but make sure
            // it is re-compiled and re-validated. model.isDirty will be set from this call.
            ViskitGlobals.instance().getAssemblyViewFrame().addEventGraphsToLegoTree(file, false);
        }
        // Don't watch an XML file whose source couldn't be compiled correctly
        if (!model.isDirty()) {
            fileWatchSave(file);
        }
    }

    @Override
    public void newSimulationParameter() //------------------------
    {
        ((EventGraphView) getView()).addParameterDialog();
    }

    @Override
    public void buildNewSimulationParameter(String name, String type, String initVal, String description) 
    {
        ((Model) getModel()).newSimulationParameter(name, type, initVal, description);
    }

    @Override
    public void simParameterEdit(ViskitParameter param) {
        boolean modified = ((EventGraphView) getView()).doEditParameter(param);
        if (modified) {
            ((viskit.model.Model) getModel()).changeSimParameter(param);
        }
    }

    @Override
    public void codeBlockEdit(String codeBlockString) {
        ((viskit.model.Model) getModel()).changeCodeBlock(codeBlockString);
    }

    @Override
    public void stateVariableEdit(ViskitStateVariable stateVariable) {
        boolean modified = ((EventGraphView) getView()).doEditStateVariable(stateVariable);
        if (modified) {
            ((viskit.model.Model) getModel()).changeStateVariable(stateVariable);
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
    public void selectNodeOrEdge(Vector<Object> objectVector) //------------------------------------
    {
        selectionVector = objectVector;
        boolean selected = nodeOrEdgeSelected(objectVector);

        // Cut not supported
        ActionIntrospector.getAction(this, "cut").setEnabled(false);
        ActionIntrospector.getAction(this, "remove").setEnabled(selected);
        ActionIntrospector.getAction(this, "copy").setEnabled(nodeSelected());
        ActionIntrospector.getAction(this, "newSelfReferentialSchedulingEdge").setEnabled(selected);
        ActionIntrospector.getAction(this, "newSelfReferentialCancelingEdge").setEnabled(selected);
    }

    private boolean nodeCopied() {
        return isNodeInVector(copyVector);
    }

    private boolean nodeSelected() {
        return isNodeInVector(selectionVector);
    }

    private boolean nodeOrEdgeSelected(Vector vectorOfInterest) {
        if (isNodeInVector(vectorOfInterest)) {
            return true;
        }
        for (Object nextObject : vectorOfInterest) {
            if (nextObject instanceof Edge) {
                return true;
            }
        }
        return false;
    }

    private boolean isNodeInVector(Vector vectorOfInterest) {
        for (Object nextObject : vectorOfInterest) {
            if (nextObject instanceof EventNode) {
                return true;
            }
        }
        return false;
    }

    private boolean doRemove = false;

    @Override
    public void remove() 
    {
        if (!selectionVector.isEmpty()) {
            // first ask:
            String foundObjectName, message = "";
            int localNodeCount = 0;  // different msg for edge delete
            for (Object nextObject : selectionVector) {
                if (nextObject != null && nextObject instanceof EventNode) {
                    localNodeCount++;
                    foundObjectName = nextObject.toString();
                    foundObjectName = foundObjectName.replace('\n', ' ');
                    message += ", \n" + foundObjectName;
                }
            }
            if (message.length() > 3) {
                message = message.substring(3);
            } // remove leading stuff

            String specialNodeMessage = (localNodeCount > 0 ? "\n(All unselected, but attached edges are permanently removed.)" : "");
            doRemove = ViskitGlobals.instance().getMainFrame().genericAskYesNo("Remove element(s)?", "Confirm remove " + message + "?" + specialNodeMessage) == JOptionPane.YES_OPTION;
            if (doRemove) {
                // TODO do edges first?
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
                    "Edges cannot be copied, only nodes.");
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
        String name;
        // We only paste un-attached nodes (at first)
        for (Object nextObject : copyVector) {
            if (nextObject instanceof Edge) {
                continue;
            }

            if (nextObject != null) {
                name = ((ViskitElement) nextObject).getName();
                buildNewNode(new Point(x + (offset * bias), y + (offset * bias)), name + "-copy");
                bias++;
            }
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void delete() 
    {
        EventNode eventNode;

        // Prevent concurrent modification
        Vector<Object> selectionObjectVector = (Vector<Object>) selectionVector.clone();
        for (Object nextObject : selectionObjectVector) {
            if (nextObject instanceof Edge) {
                removeEdge((Edge) nextObject);
            } 
            else if (nextObject instanceof EventNode) {
                eventNode = (EventNode) nextObject;
                for (ViskitElement nextEdge : eventNode.getConnections()) {
                    removeEdge((Edge) nextEdge);
                }
                ((Model) getModel()).deleteEvent(eventNode);
            }
        }

        // Clear the cache after a delete to prevent unnecessary buildup
        if (!selectionVector.isEmpty())
            selectionVector.clear();
    }

    /** Removes the JAXB (XML) binding from the model for this edge
     *
     * @param edge the edge to remove
     */
    private void removeEdge(Edge edge) {
        if (edge instanceof SchedulingEdge) {
            ((Model) getModel()).deleteSchedulingEdge(edge);
        } else {
            ((Model) getModel()).deleteCancelingEdge(edge);
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
    public void undo()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        
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

        ViskitGraphUndoManager graphUndoManager = (ViskitGraphUndoManager) view.getCurrentVgraphComponentWrapper().getUndoManager();
        try {

            // This will clear the selectionVector via callbacks
            graphUndoManager.undo(view.getCurrentVgraphComponentWrapper().getGraphLayoutCache());
        } 
        catch (CannotUndoException ex) {
            LOG.error("Unable to undo: {}", ex);
        } 
        finally {
            updateUndoRedoStatus();
        }
    }

    /**
     * Replaces the last selected node or edge from the JGraph model
     */
    @Override
    public void redo()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();

        // Recreate the JAXB (XML) bindings since the paste function only does
        // nodes and not edges
        if (redoGraphCell instanceof org.jgraph.graph.Edge) {

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof SchedulingEdge) 
            {
                SchedulingEdge schedulingEdge = (SchedulingEdge) redoGraphCell.getUserObject();
                ((Model) getModel()).redoSchedulingEdge(schedulingEdge);
            } 
            else if (redoGraphCell.getUserObject() instanceof CancelingEdge) {
                CancelingEdge cancelingEdge = (CancelingEdge) redoGraphCell.getUserObject();
                ((Model) getModel()).redoCancelingEdge(cancelingEdge);
            }
        } 
        else {
            EventNode eventNode = (EventNode) redoGraphCell.getUserObject();
            ((Model) getModel()).redoEvent(eventNode);
        }
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();
        ViskitGraphUndoManager graphUndoManager = (ViskitGraphUndoManager) eventGraphViewFrame.getCurrentVgraphComponentWrapper().getUndoManager();
        
        try {
            graphUndoManager.redo(eventGraphViewFrame.getCurrentVgraphComponentWrapper().getGraphLayoutCache());
        } 
        catch (CannotRedoException ex) {
            LOG.error("Unable to redo: {}", ex);
        } 
        finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();
        ViskitGraphUndoManager graphUndoManager = (ViskitGraphUndoManager) eventGraphViewFrame.getCurrentVgraphComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, "undo").setEnabled(graphUndoManager.canUndo(eventGraphViewFrame.getCurrentVgraphComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, "redo").setEnabled(graphUndoManager.canRedo(eventGraphViewFrame.getCurrentVgraphComponentWrapper().getGraphLayoutCache()));

        isUndo = false;
    }

    @Override
    public void deleteSimParameter(ViskitParameter simParameter) {
        ((Model) getModel()).deleteSimParameter(simParameter);
    }

    @Override
    public void deleteStateVariable(ViskitStateVariable stateVariable) {
        ((Model) getModel()).deleteStateVariable(stateVariable);
    }

    private boolean checkSave() 
    {
        Model model = (Model) getModel();
        if (model == null) {return false;}
        if (model.isDirty() || model.getLastFile() == null) {
            String message = "The model will be saved.\nContinue?";
            String title = "Confirm";
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            if (returnValue != JOptionPane.YES_OPTION) {
                return false;
            }
            save();
        }
        return true;
    }

    @Override
    public void generateJavaSource()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        Model model = (Model) getModel();
        if (model == null) {return;}
        File localLastFile = model.getLastFile();
        if (!checkSave() || localLastFile == null) {
            return;
        }

        SimkitXML2Java xml2Java = null;
        try {
            xml2Java = new SimkitXML2Java(localLastFile);
            xml2Java.unmarshal();
        } catch (FileNotFoundException fnfe) {
            LOG.error(fnfe);
        }

        String sourceString = ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).buildJavaEventGraphSource(xml2Java);
        LOG.debug(sourceString);
        if (sourceString != null && sourceString.length() > 0) {
            String className = model.getMetadata().packageName + "." + model.getMetadata().name;
            ViskitGlobals.instance().getAssemblyViewFrame().showAndSaveSource(className, sourceString, localLastFile.getName());
        }
    }

    @Override
    public void viewXML()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        if (!checkSave() || ((Model) getModel()).getLastFile() == null) {
            return;
        }
        ViskitGlobals.instance().getAssemblyViewFrame().displayXML(((Model) getModel()).getLastFile());
    }

    @Override
    public void eventList() {
        // not used
        if (viskit.ViskitStatics.debug) {
            LOG.info("EventListAction in " + this);
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
        buildNewNode(p, "event_" + nodeCount++);
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
    public void newSelfReferentialSchedulingEdge() //--------------------------
    {
        if (selectionVector != null) {
            for (Object nextObject : selectionVector) {
                if (nextObject instanceof EventNode) {
                    ((Model) getModel()).newSchedulingEdge((EventNode) nextObject, (EventNode) nextObject);
                }
            }
        }
    }

    /** Handles the menu selection for a new self-referential canceling edge */
    public void newSelfReferentialCancelingEdge() {  //--------------------------
        if (selectionVector != null) {
            for (Object nextObject : selectionVector) 
            {
                if (nextObject instanceof EventNode) {
                    ((Model) getModel()).newCancelingEdge((EventNode) nextObject, (EventNode) nextObject);
                }
            }
        }
    }

    @Override
    public void editGraphMetadata() 
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        Model eventGraphModel = (Model) getModel();
        if (eventGraphModel == null) 
        {
            eventGraphModel = ViskitGlobals.instance().getActiveEventGraphModel();
        }
        if (eventGraphModel == null) 
        {
            LOG.error("editGraphMetadata() unable to find eventGraphModel");
            return; // not expected
        }
        GraphMetadata graphMetadata = eventGraphModel.getMetadata();
        boolean modified =
                EventGraphMetadataDialog.showDialog((JFrame) getView(), graphMetadata);
        if (modified) {
            ((Model) getModel()).changeMetadata(graphMetadata);

            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(graphMetadata.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(ViskitStatics.emptyIfNull(graphMetadata.description));
        }
    }

    @Override
    public void nodeEdit(EventNode eventNode) // shouldn't be required?
    //----------------------------------
    {
        boolean done;
        boolean modified;
        do {
            done = true;
            modified = ((EventGraphView) getView()).doEditNode(eventNode);
            if (modified) {
                done = ((viskit.model.Model) getModel()).changeEventNode(eventNode);
            }
        } while (!done);
    }

    @Override
    public void schedulingArcEdit(Edge edge) {
        boolean modified = ((EventGraphView) getView()).doEditEdge(edge);
        if (modified) {
            ((viskit.model.Model) getModel()).changeSchedulingEdge(edge);
        }
    }

    @Override
    public void cancellingArcEdit(Edge edge) {
        boolean modified = ((EventGraphView) getView()).doEditCancelEdge(edge);
        if (modified) {
            ((viskit.model.Model) getModel()).changeCancelingEdge(edge);
        }
    }
    private String imageSaveCountString = "";
    private int    imageSaveInt = -1;

    @Override
    public void captureWindow()
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        String fileName = "EventGraphScreenCapture";

        // create and save the image
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();

        // Get only the jgraph part
        Component component = eventGraphViewFrame.getCurrentJgraphComponent();
        if (component == null) {return;}
//        File localLastFile = ((Model) getModel()).getLastFile();
//        if (localLastFile != null) {
//            fileName = localLastFile.getName();
//        }

        String imageFileName = ViskitGlobals.instance().getActiveEventGraphModel().getMetadata().name + imageSaveCountString + ".xml.png"; // fileName +
        File eventGraphScreenCaptureFile = ((EventGraphView) getView()).saveFileAsk(imageFileName, false,
                "Save Image, Event Graph Diagram...");

        if (eventGraphScreenCaptureFile == null) 
        {
            LOG.error("captureWindow() eventGraphScreenCaptureFile is null");
            return;
        }

        final Timer timer = new Timer(100, new TimerCallback(eventGraphScreenCaptureFile, true, component));
        timer.setRepeats(false);
        timer.start();

        imageSaveCountString = "" + (++imageSaveInt);
    }

    @Override
    public void captureEventGraphImages(List<File> eventGraphs, List<File> eventGraphImages) 
    {
        Iterator<File> fileIterator = eventGraphImages.listIterator(0);

        File imageFile;
        TimerCallback timerCallback;

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
        for (File eventGraph : eventGraphs) 
        {
            _doOpenEventGraph(eventGraph);
            LOG.debug("eventGraph: " + eventGraph);

            // Now capture and store the Event Graph images
            if (fileIterator.hasNext()) 
            {
                imageFile = fileIterator.next();
                LOG.info("captureEventGraphImages() image " + imageFile.getName() + "\n      {}", imageFile);

                // Don't display an extra frame while taking snapshots
                timerCallback = new TimerCallback(imageFile, false, eventGraphViewFrame.getCurrentJgraphComponent());

                // Make sure we have a directory ready to receive these images
                if (!imageFile.getParentFile().isDirectory()) {
                     imageFile.getParentFile().mkdirs();
                }

                // Fire this quickly as another Event Graph will immediately load
                final Timer timer = new Timer(0, timerCallback);
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    class TimerCallback implements ActionListener {

        File file;
        boolean display;
        JFrame frame;
        Component component;

        TimerCallback(File file, boolean whetherToDisplay, Component component) {
            this.file = file;
            display = whetherToDisplay;
            this.component = component;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
                LOG.debug("CurrentJgraphComponent is a JScrollPane: " + component);
            }
            Rectangle rectangle = component.getBounds();
            Image image = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_4BYTE_ABGR_PRE);

            // Tell the jgraph component to draw into memory
            try {
                component.paint(image.getGraphics());
            } catch (NullPointerException e) {
                LOG.error(e);
            }
            // NPEs are happening in JGraph

            try {
                ImageIO.write((RenderedImage)image, "png", file);
            } catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) {
                frame = new JFrame("Saved as " + file.getName());
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
    XMLConfiguration historyXMLConfiguration;

    /** This is the very first caller for getViskitAppConfiguration() upon Viskit startup */
    private void initConfig() {
        try {
            historyXMLConfiguration = ViskitUserConfiguration.instance().getViskitAppConfiguration();
        } 
        catch (Exception e) {
            LOG.error("Error loading history file: {}", e);
            LOG.warn("Recent file saving disabled");
            historyXMLConfiguration = null;
        }
    }
}
