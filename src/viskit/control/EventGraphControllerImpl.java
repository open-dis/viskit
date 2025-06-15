package viskit.control;

import actions.ActionIntrospector;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.TempFileManager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
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
import viskit.jgraph.ViskitGraphUndoManager;
import viskit.model.*;
import viskit.mvc.MvcAbstractController;
import viskit.view.dialog.EventGraphMetadataDialog;
import viskit.view.AssemblyView;
import viskit.view.EventGraphViewFrame;
import viskit.view.EventGraphView;
import viskit.xsd.translator.eventgraph.SimkitEventGraphXML2Java;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;
import static viskit.ViskitStatics.NO_DESCRIPTION_PROVIDED_HTML;

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
    
    private GraphMetadata graphMetadata;

    public EventGraphControllerImpl() {
        initConfig();
        initOpenEventGraphWatch();
    }

    @Override
    public void begin() {
        List<String> files = getOpenFileSetList(false);

        if (!files.isEmpty())
        {
            File file;

            // Open whatever Event Graphs were marked open on last closing
            for (String f : files) {
                file = new File(f);
                // Prevent project mismatch
                if (file.exists())
                    _doOpenEventGraph(file);
            }
        } 
        else
        {
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
        ViskitGlobals.instance().getActiveAssemblyController().showViskitUserPreferences();
    }

    @Override
    public void newProject() {
        ((AssemblyControllerImpl)ViskitGlobals.instance().getActiveAssemblyController()).newProject();
    }
    
    /** method name for reflection use */
    public static final String METHOD_zipAndMailProject = "zipAndMailProject";

    @Override
    public void zipProject() {
        ((AssemblyControllerImpl)ViskitGlobals.instance().getActiveAssemblyController()).zipProject();
    }
    
    /** method name for reflection use */
    public static final String METHOD_newEventGraph = "newEventGraph";

    @Override
    public void newEventGraph()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();

        // Don't allow a new event graph to be created if a current project is not open
        if (!ViskitGlobals.instance().hasViskitProject() ||
            !ViskitGlobals.instance().getViskitProject().isProjectOpen()) 
        {
            LOG.error("newEventGraph() cannot create new Event Graph if no project is open");
            return;
        }

        GraphMetadata oldGraphMetadata = null;
        Model viskitModel = (Model) getModel();
        if (viskitModel != null) {
            oldGraphMetadata = viskitModel.getMetadata();
        }
        EventGraphModelImpl newModelImpl = new EventGraphModelImpl(this);
        newModelImpl.initialize();
        newModelImpl.newModel(null);
        // TODO check for duplicate name

        // No model set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((EventGraphView) getView()).addTab(newModelImpl);

        // If we have models already opened, then use their package names for
        // this new Event Graph
        GraphMetadata newGraphMetadata = newModelImpl.getMetadata();
        if (oldGraphMetadata != null) {
            newGraphMetadata.packageName = oldGraphMetadata.packageName;
        }

        boolean modified =
                EventGraphMetadataDialog.showDialog((JFrame) getView(), newGraphMetadata);
        if (modified) 
        {
            // update title bar
            ((EventGraphView) getView()).setSelectedEventGraphName(newGraphMetadata.name);
            ((EventGraphView) getView()).setSelectedEventGraphDescription(ViskitStatics.emptyIfNull(newGraphMetadata.description));

            String message =
                    "<html><body><p align='center'>Do you want to add a <b>\"Run\"</b> Event?</p></body></html>";
            String title = "Confirm Run Event";

            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            boolean modelModified = false;
            if (returnValue == JOptionPane.YES_OPTION) {
                EventGraphControllerImpl.this.buildNewEventNode(new Point(30, 60), "Run");
                modelModified = true;
            }
            ((Model) getModel()).setModelModified(modelModified);
        } 
        else 
        {
           ((EventGraphView) getView()).deleteTab(newModelImpl);
        }
        graphMetadata = newGraphMetadata;
    }

    /**
     * Dialog operation
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        int userResponse = (ViskitGlobals.instance().getMainFrame().genericAsk("Save event graph?", "Save modified event graph?"));

        boolean returnValue;

        switch (userResponse) 
        {
            case JOptionPane.YES_OPTION:
                save();
                returnValue = true;
                break;
                
            case JOptionPane.NO_OPTION:
                // No need to recompile
                if (((Model) getModel()).isModelModified()) {
                    ((Model) getModel()).setModelModified(false);
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
    
    /** method name for reflection use */
    public static final String METHOD_open = "open";


    @Override
    public void open()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        // Bug fix: 1249
        File[] files = ViskitGlobals.instance().getEventGraphEditorViewFrame().openFilesAsk();
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
        EventGraphModelImpl eventGraphModelImplementation = new EventGraphModelImpl(this);
        eventGraphModelImplementation.initialize();
        eventGraphView.addTab(eventGraphModelImplementation);

        Model[] openAlreadyModelArray = eventGraphView.getOpenEventGraphModels();
        boolean isOpenAlready = false;
        String path;
        if (openAlreadyModelArray != null) 
        {
            for (Model model : openAlreadyModelArray) 
            {
                if (model.getLastFile() != null) 
                {
                    path = model.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) 
                    {
                        isOpenAlready = true;
                        break;
                    }
                }
            }
        }
        if (eventGraphModelImplementation.newModel(file) && !isOpenAlready) {

            // We may find one or more simkit.Priority(s) with numeric values vice
            // eneumerations in the Event Graph XML. Modify and save the Event Graph XML silently
            if (eventGraphModelImplementation.isNumericPriority()) {
                save();
                eventGraphModelImplementation.setNumericPriority(false);
            }

            eventGraphView.setSelectedEventGraphName(eventGraphModelImplementation.getMetadata().name);
            eventGraphModelImplementation.getMetadata().description = ViskitStatics.emptyIfNull(eventGraphModelImplementation.getMetadata().description);
            if  (eventGraphModelImplementation.getMetadata().description.isBlank())
                 eventGraphView.setSelectedEventGraphDescription(DESCRIPTION_HINT);
            else eventGraphView.setSelectedEventGraphDescription(eventGraphModelImplementation.getMetadata().description);
            adjustRecentEventGraphFileSet(file);
            markEventGraphFilesAsOpened();

            // Check for good compilation. TODO: Possibly grossly unnecessary since all classpaths and initial Event Graph parsing areadly took place in the project space during startup (tdn) 9/14/24
//            handleCompileAndSave(mod, file); <- possible source of Viskit barfing when opening a large set of Event Graphs
        } 
        else {
            eventGraphView.deleteTab(eventGraphModelImplementation); // Not a good open, tell view
        }
        resetRedoUndoStatus();
        ViskitGlobals.instance().getEventGraphEditorViewFrame().enableEventGraphMenuItems();
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
        Model[] openAlready = ((EventGraphView) getView()).getOpenEventGraphModels();
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
     * @param file the EventGraph file to generate MD5 hash for
     */
    private void fileWatchOpen(File file) 
    {
        String fileName = file.getName();
        File ofile = new File(watchDirectoryFile, fileName);
        LOG.debug("f is: {} and ofile is: {}", file, ofile);
        try {
            Files.copy(file.toPath(), ofile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } 
        catch (IOException e) {
            LOG.error(e);
//            e.printStackTrace();
        }
    }

    private void fileWatchClose(File file)
    {
        String fileName = file.getName();
        File ofile = new File(watchDirectoryFile, fileName);
        ofile.delete();
        AssemblyView assemblyView = (AssemblyView) ViskitGlobals.instance().getActiveAssemblyController().getView();
        assemblyView.removeEventGraphFromLEGOTree(file);
    }

    @Override
    public void addEventGraphFileListener(DirectoryWatch.DirectoryChangeListener directoryChangeListener) {
        directoryWatch.addListener(directoryChangeListener);
    }

    @Override
    public void removeEventGraphFileListener(DirectoryWatch.DirectoryChangeListener directoryChangeListener) {
        directoryWatch.removeListener(directoryChangeListener);
    }

    Set<MvcRecentFileListener> recentFileListenerSet = new HashSet<>();

    @Override
    public void addRecentEventGraphFileListener(MvcRecentFileListener mvcRecentFileListener)
    {
      recentFileListenerSet.add(mvcRecentFileListener);
    }

    @Override
    public void removeRecentEventGraphFileListener(MvcRecentFileListener recentFileListener)
    {
      recentFileListenerSet.remove(recentFileListener);
    }

    private void notifyRecentFileListeners()
    {
      for(MvcRecentFileListener recentFileListener : recentFileListenerSet) {
            recentFileListener.listenerChanged();
        }
    }

    private static final int RECENTLISTSIZE = 15;
    private final Set<String> recentEventGraphFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);;

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file an event graph file to add to the list
     */
    private void adjustRecentEventGraphFileSet(File file)
    {
        String eventGraphName;
        for (Iterator<String> iterator = recentEventGraphFileSet.iterator(); iterator.hasNext();) 
        {
            eventGraphName = iterator.next();
            if (file.getPath().equals(eventGraphName)) {
                iterator.remove();
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
    private void recordEventGraphFiles() 
    {
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
    
    /** method name for reflection use */
    public static final String METHOD_quit = "quit";

    @Override
    public void quit()
    {
        if (preQuit()) {
            postQuit();
        }
    }

    @Override
    public boolean preQuit() {

        // Check for modified models before exiting
        Model[] modelArray = ((EventGraphView) getView()).getOpenEventGraphModels();
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
    
    /** method name for reflection use */
    public static final String METHOD_closeAll = "closeAll";

    @Override
    public void closeAll()
    {
        int numberOfEventGraphs = ViskitGlobals.instance().getEventGraphEditorViewFrame().getNumberEventGraphsLoaded();
        
        if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() && // closing without checking first
ViskitGlobals.instance().hasModifiedEventGraph() &&
            !ViskitGlobals.instance().isSelectedAssemblyEditorTab())     // closing automatically when closing parent Assembly
        {
            ViskitGlobals.instance().selectEventGraphEditorTab(); // making sure
            String title = new String(), message = new String();
            if  (numberOfEventGraphs == 1)
            {
                title   = "Inspect Event Graph before closing";
                message = "First inspect open Event Graph in Event Graph Editor before closing";
            }
            else if (numberOfEventGraphs > 1) // more than one
            {
                title   = "Inspect Event Graphs before closing";
                message = "First inspect open Event Graphs in Event Graph Editor before closing them";
            }
            ViskitGlobals.instance().selectEventGraphEditorTab();
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, 
                    title, message);
                return;
        }
        ViskitGlobals.instance().selectEventGraphEditorTab(); // making sure
        
        boolean hasModifiedEventGraph = false; // TODO if needed
        
        Model[] eventGraphModels = ((EventGraphView) getView()).getOpenEventGraphModels();
        for (Model model : eventGraphModels) 
        {
            if (model.isModelModified())
            {
                hasModifiedEventGraph = true;
                break;
            }
        }
        if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() && hasModifiedEventGraph)
        {
            ViskitGlobals.instance().selectEventGraphEditorTab();
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "View Event Graph Editor", "First review Event Graph models before closing");
            return;
        }
        for (Model model : eventGraphModels) 
        {
            setModel((MvcModel) model); // TODO this is a little sloppy since an event graph might also be used by another open assembly
            close();
        }
        ViskitGlobals.instance().getEventGraphEditorViewFrame().enableEventGraphMenuItems();
    }
    
    /** method name for reflection use */
    public static final String METHOD_close = "close";

    @Override
    public void close()
    {
        if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() && // closing without checking first
            !ViskitGlobals.instance().isSelectedAssemblyEditorTab())     // closing automatically when closing parent Assembly
        {
            // OK for closing Assembly to close all corresponding Event Graphs, otherwise check
            
            int numberOfEventGraphs = ViskitGlobals.instance().getEventGraphEditorViewFrame().getNumberEventGraphsLoaded();
            if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() && (numberOfEventGraphs > 1))
            {
                ViskitGlobals.instance().selectEventGraphEditorTab();
                ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, 
                        "Confirm Event Graph choice", 
                        "First select one of the open Event Graphs before closing");
                return;
            }
            if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() && (numberOfEventGraphs == 0))
            {
                ViskitGlobals.instance().selectEventGraphEditorTab();
                return;
            }
            if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab()) // only one open Event Graph, but it wasn't visible
            {
                ViskitGlobals.instance().selectEventGraphEditorTab();
                String title   = "Please confirm...";
                String message;
                graphMetadata = ViskitGlobals.instance().getActiveEventGraphModel().getMetadata();
                if (graphMetadata != null)
                     message= "Close " + graphMetadata.name + "?";
                else message= "Close Event Graph?";
                int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
                if (returnValue == JOptionPane.NO_OPTION)
                    return;
            }
        }
        ViskitGlobals.instance().selectEventGraphEditorTab();
        if (preClose()) {
            postClose();
        }
        ViskitGlobals.instance().getEventGraphEditorViewFrame().enableEventGraphMenuItems();
    }

    @Override
    public boolean preClose() 
    {
        EventGraphModelImpl eventGraphModel = (EventGraphModelImpl) getModel();
        if (eventGraphModel == null) 
        {
            return false;
        }
        if (eventGraphModel.isModelModified())
        {
            return askToSaveAndContinue();
        }
        return true;
    }

    @Override
    public void postClose()
    {
        EventGraphModelImpl eventGraphModel = (EventGraphModelImpl) getModel();
        if (eventGraphModel.getLastFile() != null) 
        {
            fileWatchClose(eventGraphModel.getLastFile());
            markConfigClosed(eventGraphModel.getLastFile());
        }
        ((EventGraphView) getView()).deleteTab(eventGraphModel);
    }

    private void markConfigClosed(File file) {

        // Someone may try to close a file that hasn't been saved
        if (file == null) {return;}

        int idx = 0;
        for (String key : recentEventGraphFileSet) {
            if (key.contains(file.getName()))
                historyXMLConfiguration.setProperty(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + idx + ")[@open]", "false");

            idx++;
        }
    }

    // NOTE: The open attribute is zeroed out for all recent files the first
    // time a file is opened
    private void markConfigOpen(File file) 
    {
        int index = 0;
        for (String key : recentEventGraphFileSet) 
        {
            if (key.contains(file.getName()))
                historyXMLConfiguration.setProperty(ViskitUserConfiguration.EVENTGRAPH_HISTORY_KEY + "(" + index + ")[@open]", "true");

            index++;
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_save = "save";

    @Override
    public void save()
    {
        if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab() &&
             ViskitGlobals.instance().hasModifiedEventGraph())
        {
            ViskitGlobals.instance().selectEventGraphEditorTab();
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "Select Event Graph", "First select an Event Graph in Event Graph Editor before saving");
            return;
        }
        ViskitGlobals.instance().selectEventGraphEditorTab(); // ensure correct focus
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
    
    /** method name for reflection use */
    public static final String METHOD_saveAs = "saveAs";

    @Override
    public void saveAs()
    {
        if (!ViskitGlobals.instance().isSelectedEventGraphEditorTab())
        {
            ViskitGlobals.instance().selectEventGraphEditorTab();
            if ((ViskitGlobals.instance().getEventGraphEditorViewFrame().getNumberEventGraphsLoaded() > 1) && 
                !ViskitGlobals.instance().getActiveEventGraphModel().isModelModified())
            {
                ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "Select Event Graph", "First select an Event Graph before saving");
                return;
            }
        }
        ViskitGlobals.instance().selectEventGraphEditorTab();
        Model model = (Model) getModel();
        EventGraphView eventGraphView = (EventGraphView) getView();
        graphMetadata = model.getMetadata();
        if ((graphMetadata.description == null) || graphMetadata.description.equals(DESCRIPTION_HINT))
            graphMetadata.description = "";

        // Allow the user to type specific package names
        String packageName = graphMetadata.packageName.replace(".", ViskitStatics.getFileSeparator());
        File saveFile = ViskitGlobals.instance().getEventGraphEditorViewFrame().saveFileAsk(packageName + ViskitStatics.getFileSeparator() + graphMetadata.name + ".xml", false,
                                         "Save Event Graph as...");

        if (saveFile != null) 
        {
            File localLastFile = model.getLastFile();
            if (localLastFile != null) {
                fileWatchClose(localLastFile);
            }

            String saveFileName = saveFile.getName();
            if (saveFileName.toLowerCase().endsWith(".xml")) {
                saveFileName = saveFileName.substring(0, saveFileName.length() - 4);
            }
            graphMetadata.name = saveFileName;
            eventGraphView.setSelectedEventGraphName(graphMetadata.name);
            model.changeMetadata(graphMetadata); // might have renamed

            handleCompileAndSave(model, saveFile);
            adjustRecentEventGraphFileSet(saveFile);
            markEventGraphFilesAsOpened();
        }
    }
    /**
     * Saves all modified event graphs
     */
    public void saveAll()
    {
        int numberOfEventGraphs = ViskitGlobals.instance().getEventGraphEditorViewFrame().getNumberEventGraphsLoaded();
        for (int index = 0; index < numberOfEventGraphs; index++)
        {
            Model nextEventGraphModel = ViskitGlobals.instance().getEventGraphEditorViewFrame().getOpenEventGraphModels()[index];
            if (nextEventGraphModel.isModelModified())
                nextEventGraphModel.save();
        }
    }
    
    /**
     * Handles whether an XML Event Graph file gets its java source compiled and watchedmodel@param m the model of the XML Event Graph
     * @param file the XML file name to save to
     */
    private void handleCompileAndSave(Model model, File file)
    {
        ViskitGlobals.instance().selectEventGraphEditorTab(); // making sure
        if (model.saveModel(file))
        {
            // We don't need to recurse since we know this is a file, but make sure
            // it is re-compiled and re-validated later. model.isModelModified will be set from this call.
            ViskitGlobals.instance().getAssemblyEditorViewFrame().addEventGraphsToLegoTree(file, false);
        }
        // Don't watch an XML file whose source couldn't be compiled correctly
        if (!model.isModelModified()) {
            fileWatchSave(file);
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_newSimulationParameter = "newSimulationParameter";

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
    public void simulationParameterEdit(ViskitParameter newSimulationParameter) {
        boolean modified = ((EventGraphView) getView()).doEditParameter(newSimulationParameter);
        if (modified) {
            ((viskit.model.Model) getModel()).changeSimParameter(newSimulationParameter);
        }
    }

    @Override
    public void codeBlockEdit(String codeBlockString) {
        ((viskit.model.Model) getModel()).changeSourceCodeBlock(codeBlockString);
    }

    @Override
    public void stateVariableEdit(ViskitStateVariable stateVariable) {
        boolean modified = ((EventGraphView) getView()).doEditStateVariable(stateVariable);
        if (modified) {
            ((viskit.model.Model) getModel()).changeStateVariable(stateVariable);
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_newStateVariable = "newStateVariable";

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
    
    /** method name for reflection use */
    public static final String METHOD_remove = "remove";

    @Override
    @SuppressWarnings("unchecked")
    public void remove()
    {
        String foundObjectName = "", description = "", message, specialNodeMessage;
        ViskitGlobals.instance().selectEventGraphEditorTab();
        
        // Prevent concurrent update while looping over selectionVector
        Vector<Object> selectionVectorClone = (Vector<Object>) selectionVector.clone(); // TODO unchecked cast warning
        if (!selectionVectorClone.isEmpty()) 
        {
            // first ask:
            int localNodeCount = 0;  // different msg for edge delete
            for (Object nextSelectionObject : selectionVectorClone) 
            {
                if (nextSelectionObject == null)
                {
                    LOG.error("remove() selection vector included null object, ignored"); // unexpected
                }
                else if (nextSelectionObject instanceof EventNode)
                {
                    localNodeCount++;
                    foundObjectName =  "EventNode " + ((EventNode)nextSelectionObject).getName();
                    description     =                 ((EventNode)nextSelectionObject).getDescription();
                    if (description.length() > 40)
                        description = description.substring(0,40) + "... ";
                    foundObjectName = foundObjectName.replace('\n', ' ').trim();
                }
                else if (nextSelectionObject instanceof SchedulingEdge)
                {
                    localNodeCount++;
                    foundObjectName = "SchedulingEdge " + ((SchedulingEdge)nextSelectionObject).getName();
                    description     =                     ((SchedulingEdge)nextSelectionObject).getDescription();
                    if (description.length() > 40)
                        description = description.substring(0,40) + "... ";
                    foundObjectName = foundObjectName.replace('\n', ' ').trim();
                }
                else if (nextSelectionObject instanceof CancelingEdge) 
                {
                    localNodeCount++;
                    foundObjectName =  "CancelingEdge " + ((CancelingEdge)nextSelectionObject).getName();
                    description     =                     ((CancelingEdge)nextSelectionObject).getDescription();
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
                specialNodeMessage = ((localNodeCount > 0) && (nextSelectionObject instanceof EventNode)) ? 
                       "<p align='center'>Note that all unselected but attached edges are also removed.</p>" : "";
                message = "<html><body><center>" +
                          "<p align='center'>Confirm removal of " + foundObjectName + "?" + "</p>" + 
                          "<br /><p align='center'>(description: " + description + ")" + "</p><br />" + 
                          specialNodeMessage +
                          "</center></body></html>";
                doRemove = ViskitGlobals.instance().getMainFrame().genericAskYesNo(
                        "Remove element from Event Graph?", message) == JOptionPane.YES_OPTION;
                if (doRemove)
                {
                    // LOG.info messages are found in removeSelectedGraphObjects()
                    LOG.debug("remove() {} element from Event Graph approved by auther", foundObjectName);
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
    public void copy() //----------------
    {
        if (!nodeSelected()) {
            ViskitGlobals.instance().messageUser(JOptionPane.WARNING_MESSAGE,
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
    
    /** method name for reflection use */
    public static final String METHOD_paste = "paste";

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
                EventGraphControllerImpl.this.buildNewEventNode(new Point(x + (offset * bias), y + (offset * bias)), name + "-copy");
                bias++;
            }
        }
    }

    /** Permanently delete, or undo selected nodes and attached edges from the cache */
    @SuppressWarnings("unchecked")
    private void removeSelectedGraphObjects() 
    {
        EventNode eventNode;

        // Prevent concurrent modification while looping over selectionObjectVector
        Vector<Object> selectionObjectVector = (Vector<Object>) selectionVector.clone(); // TODO unchecked cast warning
        
        for (Object nextObject : selectionObjectVector) 
        {
            if (nextObject instanceof Edge) 
            {
                LOG.info("removeSelectedGraphObjects() Edge {} element from Event Graph", ((Edge) nextObject).getName());
                removeEdge((Edge) nextObject);
            } 
            else if (nextObject instanceof EventNode) 
            {
                eventNode = (EventNode) nextObject;
                for (ViskitElement nextEdge : eventNode.getConnections()) 
                {
                    LOG.info("removeSelectedGraphObjects() Edge {} element from Event Graph", ((Edge) nextEdge).getName());
                    removeEdge((Edge) nextEdge);
                }
                LOG.info("removeSelectedGraphObjects() EventNode {} element from Event Graph", eventNode.getName());
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
    
    /** method name for reflection use */
    public static final String METHOD_undo = "undo";

    @Override
    public void undo()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        
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
        ViskitGlobals.instance().selectEventGraphEditorTab();

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
    public void updateUndoRedoStatus() 
    {
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
        if (model.isModelModified() || model.getLastFile() == null) {
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
    
    /** method name for reflection use */
    public static final String METHOD_generateJavaCode = "generateJavaCode";

    @Override
    public void generateJavaCode()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();
        if (eventGraphViewFrame.getNumberEventGraphsLoaded()== 0)
        {
            String message = "First load an Event Graph before generating Java code";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "No Event Graph is loaded", message);
            return;
        }
        
        Model model = (Model) getModel();
        if (model == null) {return;}
        File localLastFile = model.getLastFile();
        if (!checkSave() || localLastFile == null) {
            return;
        }

        SimkitEventGraphXML2Java xml2Java = null;
        try {
            xml2Java = new SimkitEventGraphXML2Java(localLastFile);
            xml2Java.unmarshal();
        } catch (FileNotFoundException fnfe) {
            LOG.error(fnfe);
        }

        String sourceString = ((AssemblyControllerImpl)ViskitGlobals.instance().getActiveAssemblyController()).buildJavaEventGraphSource(xml2Java);
        LOG.debug(sourceString);
        if (sourceString != null && sourceString.length() > 0) {
            String className = model.getMetadata().packageName + "." + model.getMetadata().name;
            ViskitGlobals.instance().getAssemblyEditorViewFrame().showAndSaveSource(className, sourceString, localLastFile.getName());
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_viewXML = "viewXML";

    @Override
    public void viewXML()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();
        if (eventGraphViewFrame.getNumberEventGraphsLoaded()== 0)
        {
            String message = "First load an Event Graph before viewing XML source";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "No Event Graph is loaded", message);
            return;
        }
        
        if (!checkSave() || ((Model) getModel()).getLastFile() == null) {
            return;
        }
        ViskitGlobals.instance().getAssemblyEditorViewFrame().displayXML(((Model) getModel()).getLastFile());
    }

    @Override
    public void eventList() {
        // not used
        if (viskit.ViskitStatics.debug) {
            LOG.info("EventListAction in " + this);
        }
    }
    private int nodeCount = 0;

    /** method name for reflection use */
    public static final String METHOD_buildNewEventNode = "buildNewEventNode";

    @Override
    public void buildNewEventNode() //-------------------
    {
        EventGraphControllerImpl.this.buildNewEventNode(new Point(100, 100));
    }

    @Override
    public void buildNewEventNode(Point p) //--------------------------
    {
        EventGraphControllerImpl.this.buildNewEventNode(p, "newEvent_" + nodeCount++);
    }

    @Override
    public void buildNewEventNode(Point p, String newName) //------------------------------------
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        ((viskit.model.Model) getModel()).newEventNode(newName, p);
    }

    @Override
    public void buildNewSchedulingEdge(Object[] nodes) //--------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((Model) getModel()).newSchedulingEdge(src, tar);
    }

    @Override
    public void buildNewCancelingEdge(Object[] nodes) //--------------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((Model) getModel()).newCancelingEdge(src, tar);
    }
    
    /** method name for reflection use */
    public static final String METHOD_newSelfReferentialSchedulingEdge = "newSelfReferentialSchedulingEdge";

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
    
    /** method name for reflection use */
    public static final String METHOD_newSelfReferentialCancelingEdge = "newSelfReferentialCancelingEdge";

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
    
    /** method name for reflection use */
    public static final String METHOD_editGraphMetadata = "editGraphMetadata";

    @Override
    public void editGraphMetadata() 
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        Model eventGraphModel = (Model) getModel();
        if (eventGraphModel == null) 
        {
            eventGraphModel = ViskitGlobals.instance().getActiveEventGraphModel();
        }
        if (eventGraphModel == null) 
        {
            LOG.error(METHOD_editGraphMetadata + "() unable to find eventGraphModel");
            return; // not expected
        }
        graphMetadata = eventGraphModel.getMetadata();
        boolean modified = EventGraphMetadataDialog.showDialog((JFrame) getView(), graphMetadata);
        if (modified)
        {
            ((Model) getModel()).changeMetadata(graphMetadata);
            ViskitGlobals.instance().getActiveEventGraphModel().setModelModified(true); // TODO move into dialog panel

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
    public void schedulingEdgeEdit(Edge edge) {
        boolean modified = ((EventGraphView) getView()).doEditEdge(edge);
        if (modified) {
            ((viskit.model.Model) getModel()).changeSchedulingEdge(edge);
        }
    }

    @Override
    public void cancelingEdgeEdit(Edge edge) {
        boolean modified = ((EventGraphView) getView()).doEditCancelEdge(edge);
        if (modified) {
            ((viskit.model.Model) getModel()).changeCancelingEdge(edge);
        }
    }
    private String imageSaveCountString = "";
    private int    imageSaveInt = -1;
    
    /** method name for reflection use */
    public static final String METHOD_captureWindow = "captureWindow";

    @Override
    public void captureWindow()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        
        EventGraphViewFrame eventGraphViewFrame = (EventGraphViewFrame) getView();
        if (eventGraphViewFrame.getNumberEventGraphsLoaded()== 0)
        {
            String message = "First load an Event Graph before capturing an image";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.WARNING_MESSAGE,
                "No Event Graph is loaded", message);
            return;
        }
        
        String fileName = "EventGraphScreenCapture";

        // create and save the image
        
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
        
        try
        {
            if (eventGraphScreenCaptureFile.exists() && !eventGraphScreenCaptureFile.isDirectory())
            {
                Desktop.getDesktop().open(eventGraphScreenCaptureFile); // display image
                // also open directory to facilitate copying, editing
                Desktop.getDesktop().open(eventGraphScreenCaptureFile.getParentFile());
            }
        }
        catch (IOException e)
        {
            LOG.error("captureWindow() unable to display ()", eventGraphScreenCaptureFile);
        }
    }

    /** Capture images using already-completed JGraph images.  
     * TODO crop excess for smaller margin, probably by keeping track of dimensions when creating. */
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
        closeAll(); // TODO seems wrong but likely needed for off-screen rendering to file

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

                // Don't display an extra timerCallbackFrame while taking snapshots from each display frame
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

    class TimerCallback implements ActionListener 
    {
        File file;
        boolean display;
        JFrame timerCallbackFrame;
        Component component;

        TimerCallback(File file, boolean whetherToDisplay, Component component) {
            this.file = file;
            display = whetherToDisplay;
            this.component = component;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            if (component instanceof JScrollPane) 
            {
                component = ((JScrollPane) component).getViewport().getView();
                LOG.debug("CurrentJgraphComponent is a JScrollPane: " + component);
            }
            Rectangle rectangle = component.getBounds();
            Image image = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_4BYTE_ABGR_PRE);

            // Tell the jgraph component to draw into memory
            try {
                component.paint(image.getGraphics());
            } 
            catch (NullPointerException e) {
                LOG.error(e);
            }
            // NPEs are happening in JGraph

            try {
                ImageIO.write((RenderedImage)image, "png", file);
            } 
            catch (IOException e) {
                LOG.error(e);
            }

            // display a scaled version
            if (display) 
            {
                timerCallbackFrame = new JFrame("Saved as " + file.getName());
                Icon ii = new ImageIcon(image);
                JLabel lab = new JLabel(ii);
                timerCallbackFrame.getContentPane().setLayout(new BorderLayout());
                timerCallbackFrame.getContentPane().add(lab, BorderLayout.CENTER);
                timerCallbackFrame.pack();
                timerCallbackFrame.setLocationRelativeTo((Component) getView());

                Runnable r = () -> {
                    timerCallbackFrame.setVisible(true);
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
        catch (Exception e) 
        {
            LOG.error("Error loading history file: {}", e);
            LOG.warn("Recent file saving disabled");
            historyXMLConfiguration = null;
        }
    }
}
