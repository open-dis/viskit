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
        initializeViskitConfiguration();
        initializeOpenEventGraphWatch();
    }

    @Override
    public void begin()
	{
        List<File> fileList = EventGraphControllerImpl.this.getOpenEventGraphFileList(false);

        if (!fileList.isEmpty()) {

            // Open whatever Event Graphs were marked open on last closing
            for (File f : fileList) {
                _doOpen(f);
            }

        } 
		else if (ViskitGlobals.instance().getCurrentViskitProject() != null) // project might not be open
		{
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
    public void newProject()
	{
        ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).newProject();
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus();   // reset menus
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus(); // reset menus
    }

    @Override
    public void zipAndMailProject() {
        ((AssemblyController)ViskitGlobals.instance().getAssemblyController()).zipAndMailProject();
    }

    public final static String NEWEVENTGRAPH_METHOD = "newEventGraph"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newEventGraph () // method name must exactly match preceding string value
	{
        // Don't allow a new event graph to be created if a current project is not open
        if ((ViskitGlobals.instance() == null) || 
		    (ViskitGlobals.instance().getCurrentViskitProject() == null) || 
		    !ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen()) 
		{
			messageToUser (JOptionPane.WARNING_MESSAGE, "No project directory found", 
					"<html><p align='center'>New event graphs are only created within an open project.</p>" +
					"<p align='center'>Please open or create a project first.</p>");
			return;
		}
		ViskitGlobals.instance().getViskitApplicationFrame().displayEventGraphEditorTab(); // prerequisite to possible file menu dialog

        GraphMetadata priorEventGraphMetadata = null;
        EventGraphModelImpl priorEventGraphModel = (EventGraphModelImpl) getModel();
        if (priorEventGraphModel != null) {
            priorEventGraphMetadata = priorEventGraphModel.getMetadata();
        }

        EventGraphModel eventGraphModel = new EventGraphModelImpl(this);
        eventGraphModel.initialize();
        eventGraphModel.newModel(null); // no file

        // No model set in controller yet... it gets set
        // when TabbedPane changelistener detects a tab change.
        ((EventGraphView) getView()).addTab(eventGraphModel);

        // If we have models already opened, then use most recent package name as default for this new EventGraph
        GraphMetadata newEventGraphMetadata = eventGraphModel.getMetadata();
        if (priorEventGraphMetadata != null) {
            newEventGraphMetadata.packageName = priorEventGraphMetadata.packageName;
        }
		newEventGraphMetadata.description = "TODO: enter a description for this new event graph";

        boolean modified = EventGraphMetadataDialog.showDialog((JFrame) getView(), newEventGraphMetadata);
		
        if (modified)
		{
            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(newEventGraphMetadata.name);
			
            ((EventGraphView) getView()).setSelectedEventGraphDescription(newEventGraphMetadata.description);

            // Bugfix 1398
            String message =
                    "<html><body><p align='center'>Do you want " + newEventGraphMetadata.name + 
					" execution to start with a <b>\"Run\"</b> Event?</p></body></html>";
            String title = "Confirm Run Event";

            int returnValue = ((EventGraphView) getView()).genericAskYN(title, message);
            boolean dirty = false;
            if (returnValue == JOptionPane.YES_OPTION) {
                buildNewNode(new Point(30, 60), "Run");
                dirty = true;
            }
            ((EventGraphModel) getModel()).setDirty(dirty);
        } 
		else
		{
           ((EventGraphView) getView()).deleteTab(eventGraphModel);
        }
		ViskitGlobals.instance().getViskitApplicationFrame().displayEventGraphEditorTab(); // prerequisite to buildMenus
		ViskitGlobals.instance().getViskitApplicationFrame().buildMenus(); // reset
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

    public final static String OPEN_METHOD = "open"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void open () // method name must exactly match preceding string value
	{
        // Bug fix: 1249
        File[] files = ((EventGraphView) getView()).openFilesAsk();
        if (files == null) {
            return;
        }
        for (File file : files)
		{
            if (file != null)
			{
				if (file.getName().startsWith("."))
				{
					break; // skip hidden files
				}
				else if (file.getParentFile().getAbsolutePath().startsWith(ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getAbsolutePath()))
				{
					_doOpen(file);
					ViskitGlobals.instance().getViskitApplicationFrame().displayEventGraphEditorTab();
				}
				else 
				{
					messageToUser (JOptionPane.WARNING_MESSAGE, "Illegal directory for current project", 
							"<html><p>Event graphs must be within the currently open project.</p>" +
							"<p>&nbsp</p>" +
							"<p>Current project name: <b>" + ViskitGlobals.instance().getCurrentViskitProject().getProjectName() + "</b></p>" +
							"<p>Current project path: "    + ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getAbsolutePath() + "</p>" +
							"<p>&nbsp</p>" +
							"<p>Please choose an event graph in current project, or else open a different project.</p>");
					// TODO offer to copy?
					break;
				}
			}
        }
		updateEventGraphFileLists ();
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
    }

    @Override
    public void openRecentEventGraph(File path) {
        _doOpen(path);
    }

    // Package protected for the AssemblyControllerImpl's access to open EventGraphs
    void _doOpen(File file)
	{
        EventGraphView eventGraphView = (EventGraphView) getView();
        EventGraphModelImpl model = new EventGraphModelImpl(this);
        model.initialize();
        eventGraphView.addTab(model);
        ViskitGlobals.instance().getEventGraphViewFrame().getSelectedPane().setToolTipText(model.getMetadata().description);

        EventGraphModel[] openAlready = eventGraphView.getOpenModels();
        boolean isOpenAlready = false;
        if (openAlready != null) {
            for (EventGraphModel eventGraphModel : openAlready) {
                if (eventGraphModel.getCurrentFile() != null) {
                    String path = eventGraphModel.getCurrentFile().getAbsolutePath();
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
        ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // refresh
    }

    /** Start w/ undo/redo disabled in the Edit Menu after opening a file */
    private void resetRedoUndoStatus() {

        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();

        if (eventGraphViewFrame.getCurrentEventGraphComponentWrapper() != null) {
            vGraphUndoManager undoMgr = (vGraphUndoManager) eventGraphViewFrame.getCurrentEventGraphComponentWrapper().getUndoManager();
            undoMgr.discardAllEdits();
            updateUndoRedoStatus();
        }
    }

    /** Mark every EG file opened as "open" in the app config file */
    private void markEventGraphFilesAsOpened() {

        EventGraphModel[] openAlready = ((EventGraphView) getView()).getOpenModels();
        for (EventGraphModel vMod : openAlready) {
            if (vMod.getCurrentFile() != null) {
                String modelPath = vMod.getCurrentFile().getAbsolutePath().replaceAll("\\\\", "/");
                markXMLConfigurationOpen(modelPath);
            }
        }
    }

    // Support for informing listeners about open eventgraphs
    // Methods to implement a scheme where other modules will be informed of file changes
    // (Would Java Beans do this with more or less effort?
    private DirectoryWatch dirWatch;
    private File watchDir;

    private void initializeOpenEventGraphWatch() {
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
    public void addEventGraphFileListener(DirectoryWatch.DirectoryChangeListener listener) {
        dirWatch.addListener(listener);
    }

    @Override
    public void removeEventGraphFileListener(DirectoryWatch.DirectoryChangeListener listener) {
        dirWatch.removeListener(listener);
    }

    Set<mvcRecentFileListener> recentListeners = new HashSet<>();

    @Override
    public void addRecentEventGraphFileListener(mvcRecentFileListener listener)
    {
      recentListeners.add(listener);
    }

    @Override
    public void removeRecentEventGraphFileListener(mvcRecentFileListener listener)
    {
      recentListeners.remove(listener);
    }

    private void notifyRecentFileListeners()
    {
      for (mvcRecentFileListener listener : recentListeners)
	  {
            listener.listChanged();
      }
    }

    private static final int RECENTLISTSIZE = 15;
    private Set<File> recentEventGraphFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);;

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an event graph file to add to the list
     */
    private void adjustRecentEventGraphFileSet(File file) 
	{
        for (Iterator<File> itr = recentEventGraphFileSet.iterator(); itr.hasNext();)
		{
            File f = itr.next();
            if (file.getPath().equals(f.getPath())) {
                itr.remove();
                break;
            }
        }
        recentEventGraphFileSet.add(file); // to the top
        notifyRecentFileListeners();
        saveEventGraphHistoryXML(recentEventGraphFileSet);
    }

    private List<File> openEventGraphsFileList = new ArrayList<>(4);

    @SuppressWarnings("unchecked")
    public void updateEventGraphFileLists ()
	{
        if (historyXMLConfiguration == null) 
		{
			initializeViskitConfiguration();
		}
        List<String> eventGraphFilePathList = historyXMLConfiguration.getList(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "[@value]");
        int index = 0;
        for (String eventGraphFilePath : eventGraphFilePathList) {
            if (recentEventGraphFileSet.add(new File(eventGraphFilePath))) // returns true if file added
			{
                String openValue = historyXMLConfiguration.getString(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + index + ")[@open]");

                if (openValue != null && (openValue.toLowerCase().equals("true") || openValue.toLowerCase().equals("yes"))) {
                    openEventGraphsFileList.add(new File(eventGraphFilePath));
                }
                notifyRecentFileListeners();
            }
            index++;
        }
        saveEventGraphHistoryXML(recentEventGraphFileSet);
    }

    private void saveEventGraphHistoryXML(Set<File> recentFiles)
	{
        historyXMLConfiguration.clearTree(ViskitConfiguration.RECENT_EVENT_GRAPH_CLEAR_KEY);
        int index = 0;

        // The value's modelPath is already delimited with "/"
        for (File recentFile : recentFiles) {
            historyXMLConfiguration.setProperty(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + index + ")[@value]", recentFile.getPath());
            index++;
        }
        historyXMLConfiguration.getDocument().normalize();
    }

    @Override
    public void clearRecentEventGraphFileSet()
	{
        recentEventGraphFileSet.clear();
        notifyRecentFileListeners();
        saveEventGraphHistoryXML(recentEventGraphFileSet);
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
    }

    @Override
    public Set<File> getRecentEventGraphFileSet()
	{
        return getRecentEventGraphFileSet(true); // typically must refresh to see changes
    }

    private Set<File> getRecentEventGraphFileSet(boolean refresh) {
        if (refresh || recentEventGraphFileSet == null) {
            updateEventGraphFileLists();
        }
        return recentEventGraphFileSet;
    }

    public List<File> getOpenEventGraphFileList()
	{
        return getOpenEventGraphFileList(true); // typically must refresh to see changes
    }

    private List<File> getOpenEventGraphFileList(boolean refresh) {
        if (refresh || openEventGraphsFileList == null) {
            updateEventGraphFileLists();
        }
        return openEventGraphsFileList;
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

    public final static String CLOSEALL_METHOD = "closeAll"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void closeAll () // method name must exactly match preceding string value
	{
        EventGraphModel[] openModels = ((EventGraphView) getView()).getOpenModels();
        for (EventGraphModel eventGraphModel : openModels) {
            setModel((mvcModel) eventGraphModel);
            close();
        }
		// included in close(): ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
    }

    public final static String CLOSE_METHOD = "close"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void close () // method name must exactly match preceding string value
	{
        if (preClose()) {
            postClose();
        }
//		updateEventGraphFileLists (); // save for next time
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // reset
    }

    @Override
    public boolean preClose() {
        EventGraphModelImpl eventGraphModel = (EventGraphModelImpl) getModel();
        if (eventGraphModel == null) {
            return false;
        }
        if (eventGraphModel.isDirty()) {
            return askToSaveAndContinue();
        }
        return true;
    }

    @Override
    public void postClose() {

        EventGraphModelImpl eventGraphModel = (EventGraphModelImpl) getModel();
        if (eventGraphModel.getCurrentFile() != null)
		{
			openEventGraphsFileList.remove(eventGraphModel.getCurrentFile());
            fileWatchClose(eventGraphModel.getCurrentFile());
            markXMLConfigurationClosed(eventGraphModel.getCurrentFile());
        }
        ((EventGraphView) getView()).deleteTab(eventGraphModel);
    }

    private void markXMLConfigurationClosed(File f) {

        // Someone may try to close a file that hasn't been saved
        if (f == null) {return;}

        int index = 0;
        for (File key : recentEventGraphFileSet) {
            if (key.getPath().contains(f.getName())) {
                historyXMLConfiguration.setProperty(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + index + ")[@open]", "false");
            }
            index++;
        }
    }

    // NOTE: The open attribute is zeroed out for all recent files the first
    // time a file is opened
    private void markXMLConfigurationOpen(String path) {
        int idx = 0;
        for (File key : recentEventGraphFileSet) {
            if (key.getPath().contains(path)) {
                historyXMLConfiguration.setProperty(ViskitConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]", "true");
            }
            idx++;
        }
    }

    public final static String SAVE_METHOD = "save"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void save () // method name must exactly match preceding string value
	{
        EventGraphModel mod = (EventGraphModel) getModel();
        File localLastFile = mod.getCurrentFile();
        if (localLastFile == null) {
            saveAs();
        } else {
            handleCompileAndSave(mod, localLastFile);
        }
    }

    public final static String SAVEAS_METHOD = "saveAs"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void saveAs () // method name must exactly match preceding string value
	{
        EventGraphModel eventGraphModel = (EventGraphModel) getModel();
        EventGraphView  eventGraphView  = (EventGraphView) getView();
        GraphMetadata   graphMetadata   =  eventGraphModel.getMetadata();

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = eventGraphView.saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false, "Save Event Graph File As");

        if (saveFile != null) {
            File localLastFile = eventGraphModel.getCurrentFile();
            if (localLastFile != null) {
                fileWatchClose(localLastFile);
            }

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            graphMetadata.name = n;
            eventGraphView.setSelectedEventGraphName(graphMetadata.name);
            eventGraphModel.setMetadata(graphMetadata); // might have renamed

            handleCompileAndSave(eventGraphModel, saveFile);
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
            ViskitGlobals.instance().getAssemblyEditViewFrame().addEventGraphsToLegoTree(f, false);
        }

        // Don't watch a an XML file whose source couldn't be compiled correctly
        if (!m.isDirty()) {
            fileWatchSave(f);
        }
    }

    public final static String NEWSIMPARAMETER_METHOD = "newSimParameter"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newSimParameter () // method name must exactly match preceding string value
    {
        ((EventGraphView) getView()).addParameterDialog();
    }

    @Override
    public void buildNewSimParameter(String name, String type, String initialValue, String description) {
        ((EventGraphModel) getModel()).newSimParameter(name, type, initialValue, description);
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

    public final static String NEWSTATEVARIABLE_METHOD = "newStateVariable"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newStateVariable() // method name must exactly match preceding string value
	{
        ((EventGraphView) getView()).addStateVariableDialog();
    }

    // Comes in from view
    @Override
    public void buildNewStateVariable(String name, String type, String initialValue, String description) //----------------------------
    {
        ((viskit.model.EventGraphModel) getModel()).newStateVariable(name, type, initialValue, description);
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
        ActionIntrospector.getAction(this, "newSelfSchedulingEdge").setEnabled(selected);
        ActionIntrospector.getAction(this, "newSelfCancellingEdge").setEnabled(selected);
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

    public final static String REMOVE_METHOD = "remove"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void remove () // method name must exactly match preceding string value
	{
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
	
    public final static String CUT_METHOD = "cut"; // must match following method name.  not possible to accomplish this programmatically.
	/**
	 * Not supported
	 */
    @Override
    public void cut() // method name must exactly match preceding string value
    {
        // Not supported
    }

	
    public final static String COPY_METHOD = "copy"; // must match following method name.  not possible to accomplish this programmatically.
	
    @Override
    @SuppressWarnings("unchecked")
    public void copy() // method name must exactly match preceding string value
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

    public final static String PASTE_METHOD = "paste"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void paste() // method name must exactly match preceding string value
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

    public final static String DELETE_METHOD = "delete"; // must match following method name.  Not possible to accomplish this programmatically.
    /** 
	 * Permanently delete, or undo selected nodes and attached edges from the cache
	 */
    @SuppressWarnings("unchecked")
    private void delete() // method name must exactly match preceding string value
	{
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
     * @param edge the edge to remove
     */
    private void removeEdge(Edge edge) {
        if (edge instanceof SchedulingEdge) {
            ((EventGraphModel) getModel()).deleteSchedulingEdge((SchedulingEdge) edge);
        } else {
            ((EventGraphModel) getModel()).deleteCancellingEdge((CancellingEdge) edge);
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
	
    public final static String UNDO_METHOD = "undo"; // must match following method name.  Not possible to accomplish this programmatically.
    /**
     * Removes the last selected node or edge from the JGraph model
     */
    @Override
    public void undo() // method name must exactly match preceding string value
	{

        isUndo = true;

        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentEventGraphComponentWrapper().getUndoManager();

        Object[] roots = view.getCurrentEventGraphComponentWrapper().getRoots();
        redoGraphCell = (DefaultGraphCell) roots[roots.length - 1];

        // Prevent dups
        if (!selectionVector.contains(redoGraphCell.getUserObject()))
            selectionVector.add(redoGraphCell.getUserObject());

        remove();

        if (!doRemove) {return;}

        try {

            // This will clear the selectionVector via callbacks
            undoMgr.undo(view.getCurrentEventGraphComponentWrapper().getGraphLayoutCache());
        } catch (CannotUndoException ex) {
            LOG.error("Unable to undo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    public final static String REDO_METHOD = "redo"; // must match following method name.  Not possible to accomplish this programmatically.
    /**
     * Replaces the last selected node or edge from the JGraph model
     */
    @Override
    public void redo() // method name must exactly match preceding string value
	{
        // Recreate the JAXB (XML) bindings since the paste function only does nodes and not edges
        if (redoGraphCell instanceof org.jgraph.graph.Edge) { // TODO fix

            // Handles both arcs and self-referential arcs
            if (redoGraphCell.getUserObject() instanceof SchedulingEdge) {
                SchedulingEdge ed = (SchedulingEdge) redoGraphCell.getUserObject();
                ((EventGraphModel) getModel()).redoSchedulingEdge(ed);
            } else {
                CancellingEdge ed = (CancellingEdge) redoGraphCell.getUserObject();
                ((EventGraphModel) getModel()).redoCancellingEdge(ed);
            }
        } else {
            EventNode node = (EventNode) redoGraphCell.getUserObject();
            ((EventGraphModel) getModel()).redoEvent(node);
        }

        EventGraphViewFrame view = (EventGraphViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) view.getCurrentEventGraphComponentWrapper().getUndoManager();
        try {
            undoMgr.redo(view.getCurrentEventGraphComponentWrapper().getGraphLayoutCache());
        } catch (CannotRedoException ex) {
            LOG.error("Unable to redo: " + ex);
        } finally {
            updateUndoRedoStatus();
        }
    }

    /** Toggles the undo/redo Edit menu items on/off */
    public void updateUndoRedoStatus() {
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();
        vGraphUndoManager undoMgr = (vGraphUndoManager) eventGraphViewFrame.getCurrentEventGraphComponentWrapper().getUndoManager();

        ActionIntrospector.getAction(this, UNDO_METHOD).setEnabled(undoMgr.canUndo(eventGraphViewFrame.getCurrentEventGraphComponentWrapper().getGraphLayoutCache()));
        ActionIntrospector.getAction(this, REDO_METHOD).setEnabled(undoMgr.canRedo(eventGraphViewFrame.getCurrentEventGraphComponentWrapper().getGraphLayoutCache()));

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
        EventGraphModel eventGraphModel = (EventGraphModel) getModel();
        if (eventGraphModel == null) {return false;}
        if (eventGraphModel.isDirty() || eventGraphModel.getCurrentFile() == null) {
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

    public final static String JAVASOURCE_METHOD = "generateJavaSource"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void generateJavaSource () // method name must exactly match preceding string value
	{
        EventGraphModel eventGraphModel = (EventGraphModel) getModel();
        if (eventGraphModel == null) {return;}
        File localLastFile = eventGraphModel.getCurrentFile();
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

        String source = (ViskitGlobals.instance().getAssemblyController()).buildJavaEventGraphSource(simkitXML2Java);
        LOG.debug(source);
        if (source != null && source.length() > 0) {
            String className = eventGraphModel.getMetadata().packageName + "." +
                    eventGraphModel.getMetadata().name;
            ViskitGlobals.instance().getAssemblyEditViewFrame().showAndSaveSource(className, source, localLastFile.getName());
        }
    }

    public final static String SHOWXML_METHOD = "showXML"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void showXML () // method name must exactly match preceding string value
	{
        if (!checkSave() || ((EventGraphModel) getModel()).getCurrentFile() == null) {
            return;
        }

        ViskitGlobals.instance().getAssemblyEditViewFrame().displayXML(((EventGraphModel) getModel()).getCurrentFile());
    }

    @Override
    public void eventList() {
        // not used
        if (viskit.ViskitStatics.debug) {
            System.out.println("EventListAction in " + this);
        }
    }
    private int nodeCount = 0;

    public final static String NEWEVENTNODE_METHOD = "newNode"; // must match following method name.  Not possible to accomplish this programmatically.
    @Override
    public void newNode () // method name must exactly match preceding string value
    {
        buildNewNode(new Point(100, 100)); // TODO avoid collision
    }

    @Override
    public void buildNewNode(Point p) //--------------------------
    {
        buildNewNode(p, "evnt_" + nodeCount++);
    }

    @Override
    public void buildNewNode(Point point, String nodeName) //------------------------------------
    {
        ((viskit.model.EventGraphModel) getModel()).newEvent(nodeName, point);
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
    public void buildNewCancellingArc(Object[] nodes) //--------------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((EventGraphModel) getModel()).newCancellingEdge(src, tar);
    }

    public final static String NEWSELFSCHEDULINGEDGE_METHOD = "newSelfSchedulingEdge"; // must match following method name.  not possible to accomplish this programmatically.
    /** Handles the menu selection for a new self-referential scheduling edge */
    public void newSelfSchedulingEdge() // method name must exactly match preceding string value
    {
        if (selectionVector != null) {
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    ((EventGraphModel) getModel()).newSchedulingEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    public final static String NEWSELFCANCELLINGEDGE_METHOD = "newSelfCancellingEdge"; // must match following method name.  not possible to accomplish this programmatically.
    /** Handles the menu selection for a new self-referential cancelling edge */
    public void newSelfCancellingEdge() // method name must exactly match preceding string value
	{
        if (selectionVector != null) {
            for (Object o : selectionVector) {
                if (o instanceof EventNode) {
                    ((EventGraphModel) getModel()).newCancellingEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    public final static String EDIT_EVENT_GRAPH_METADATA_METHOD = "editEventGraphMetadata"; // must match following method name.  not possible to accomplish this programmatically.
    @Override
    public void editEventGraphMetadata () // method name must exactly match preceding string value
	{
        EventGraphModel eventGraphModel = (EventGraphModel) getModel();
        if (eventGraphModel == null) {return;}
        GraphMetadata graphMetadata = eventGraphModel.getMetadata();
        boolean modified = EventGraphMetadataDialog.showDialog((JFrame) getView(), graphMetadata);
        if (modified) {
            ((EventGraphModel) getModel()).setMetadata(graphMetadata);

            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(graphMetadata.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(graphMetadata.description);
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
    public void schedulingArcEdit(SchedulingEdge edge) {
        boolean modified = ((EventGraphView) getView()).doEditEdge(edge);
        if (modified) {
            ((viskit.model.EventGraphModel) getModel()).changeSchedulingEdge(edge);
        }
    }

    @Override
    public void cancellingArcEdit(CancellingEdge edge) {
        boolean modified = ((EventGraphView) getView()).doEditCancellingEdge( edge);
        if (modified) {
            ((viskit.model.EventGraphModel) getModel()).changeCancellingEdge(edge);
        }
    }
    private String imageSaveCount = "";
    private int    imageSaveInt = -1;

    public final static String IMAGECAPTURE_METHOD = "windowImageCapture"; // must match following method name.  not possible to accomplish this programmatically.
    @Override
    public void windowImageCapture () // method name must exactly match preceding string value
	{
        String fileName = "EventGraphDiagram"; // default, replaced by filename

        // create and save the image
        EventGraphViewFrame egvf = (EventGraphViewFrame) getView();

        // Get only the jgraph part
        Component component = egvf.getCurrentJgraphComponent();
        if (component == null) {return;}
        File localLastFile = ((EventGraphModel) getModel()).getCurrentFile();
        if (localLastFile != null) {
            fileName = localLastFile.getName();
			if (fileName.endsWith(".xml"))
				fileName = fileName.substring (0, fileName.indexOf(".xml"));
        }
        File imageFile = ((EventGraphView) getView()).saveFileAsk(fileName + imageSaveCount + ".png", false, "Save Event Graph Diagram Image");

        if (imageFile == null) {
            return;
        }
        final Timer timer = new Timer(100, new TimerCallback(imageFile, true, component));
        timer.setRepeats(false);
        timer.start();

        imageSaveCount = "" + (++imageSaveInt);
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
    XMLConfiguration historyXMLConfiguration;

    /** This is the very first caller for getViskitApplicationXMLConfiguration() upon Viskit startup */
    private void initializeViskitConfiguration() {
        try {
            historyXMLConfiguration = ViskitConfiguration.instance().getViskitApplicationXMLConfiguration();
        } catch (Exception e) {
            LOG.error("Error loading history file: " + e.getMessage());
            LOG.warn ("Recent file saving disabled");
            historyXMLConfiguration = null;
        }
    }
}
