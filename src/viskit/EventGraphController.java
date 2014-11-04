package viskit;

import actions.ActionIntrospector;
import edu.nps.util.DirectoryWatch;
import edu.nps.util.FileIO;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;
import viskit.xsd.translator.SimkitXML2Java;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 12:52:59 PM
 * @version $Id$
 *
 * This is the MVC controller for the Viskit app.  All user inputs come here, and this
 * code decides what to do about it.  To add new events:
 * 1 add a new public Action BLAH field
 * 2 instantiate it in the constructor, mapping it to a handler (name)
 * 3 write the handler
 */
public class EventGraphController extends mvcAbstractController implements ViskitController {

    static final Logger LOGGER = LogUtils.getLogger(EventGraphController.class);

    public EventGraphController() {
        initConfig();
        initFileWatch();
        this._setFileSet();
    }

    @Override
    public void begin() {
        java.util.List<String> lis = getOpenFileSet(false);

        // don't default to new event graph
        if (lis.isEmpty()) {
            LOGGER.debug("In begin() (if) of EventGraphController");
        } else {

            // If EventGraphs were already open without a corresponding Assembly
            // file open, then open them upon Viskit starting, else, let the
            // Assembly file tell which EGs to open
            if (VGlobals.instance().getAssemblyController() != null) {
                java.util.List<String> al = VGlobals.instance().getAssemblyController().getOpenAssyFileList(false);
                if (al.isEmpty()) {
                    LOGGER.debug("In begin() (else) of EventGraphController");
                    for (String s : lis) {
                        File f = new File(s);
                        if (f.exists()) {
                            _doOpen(f);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void settings() {
        // placeholder for multi-tabbed combo app.
    }

    /** Creates a new Viskit Project */
    @Override
    public void newProject() {
        VGlobals.instance().getAssemblyController().newProject();
    }

    @Override
    public void newEventGraph() {
        GraphMetaData oldGmd = null;
        ViskitModel viskitModel = (ViskitModel) getModel();
        if (viskitModel != null) {
            oldGmd = viskitModel.getMetaData();
        }

        Model mod = new Model(this);
        mod.init();
        mod.newModel(null);

        // No model set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((ViskitView) getView()).addTab(mod);

        GraphMetaData gmd = new GraphMetaData(mod);
        if (oldGmd != null) {
            gmd.packageName = oldGmd.packageName;
        }

        boolean modified =
                EventGraphMetaDataDialog.showDialog(VGlobals.instance().getEventGraphEditor(), gmd);
        if (modified) {
            ((ViskitModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((ViskitView) getView()).setSelectedEventGraphName(gmd.name);

            // Bugfix 1398
            String msg =
                    "<html><body><p align='center'>Do you wish to start with a <b>\"Run\"</b> Event?</p></body></html>";
            String title = "Confirm Run Event";

            int ret = ((ViskitView) getView()).genericAskYN(title, msg);
            boolean dirty = false;
            if (ret == JOptionPane.YES_OPTION) {
                buildNewNode(new Point(30, 30), "Run");
                dirty = true;
            }
            ((ViskitModel) getModel()).setDirty(dirty);
        } else {
           ((ViskitView) getView()).delTab(mod);
        }
    }

    /**
     *
     * @return true = continue, false = don't (i.e., we canceled)
     */
    private boolean askToSaveAndContinue() {
        int yn = (((ViskitView) getView()).genericAsk("Question", "Save modified graph?"));

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();
                if (((ViskitModel) getModel()).isDirty()) {
                    return false;
                } // we cancelled
                return true;
            case JOptionPane.NO_OPTION:
                return true;
            case JOptionPane.CANCEL_OPTION:
                return false;

            // Something funny if we're here
            default:
                return false;
        }
    }

    @Override
    public void open() {
        // Bug fix: 1249
        File[] files = ((ViskitView) getView()).openFilesAsk();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file != null) {
                _doOpen(file);
            }
        }
    }

    @Override
    public void openRecentEventGraph(String path) {
        _doOpen(new File(path));
    }

    // Protected for the AssemblyController's access to open EventGraphs
    protected void _doOpen(File file) {
        ViskitView viskitView = (ViskitView) getView();
        Model mod = new Model(this);
        mod.init();
        viskitView.addTab(mod);

        ViskitModel[] openAlready = viskitView.getOpenModels();
        boolean isOpenAlready = false;
        if (openAlready != null) {
            for (ViskitModel model : openAlready) {
                if (model.getLastFile() != null) {
                    String path = model.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) {
                        isOpenAlready = true;
                    }
                }
            }
        }
        if (mod.newModel(file) && !isOpenAlready) {

            // We may find one or more simkit.Priority(s) with numeric values vice
            // eneumerations in the EG XML.  Modify and save the EG XML silently
            if (mod.isNumericPriority()) {
                save();
                mod.setNumericPriority(false);
            }

            viskitView.setSelectedEventGraphName(mod.getMetaData().name);
            viskitView.setSelectedEventGraphDescription(mod.getMetaData().description);
            adjustRecentList(file);

            // Mark every vAMod opened as "open"
            openAlready = viskitView.getOpenModels();
            for (ViskitModel vMod : openAlready) {
                if (vMod.getLastFile() != null) {
                    String modelPath = vMod.getLastFile().getAbsolutePath().replaceAll("\\\\", "/");
                    markConfigOpen(modelPath);
                }
            }
            fileWatchOpen(file);
        } else {
            viskitView.delTab(mod);   // Not a good open, tell view
        }
    }

    // Support for informing listeners about open eventgraphs
    // Methods to implement a scheme where other modules will be informed of file changes
    // (Would Java Beans do this with more or less effort?
    private DirectoryWatch dirWatch;
    private File watchDir;

    private void initFileWatch() {
        try { // TBD this may be obsolete
            watchDir = TempFileManager.createTempFile("egs", "current");   // actually creates
            watchDir = TempFileManager.createTempDir(watchDir);

            dirWatch = new DirectoryWatch(watchDir);
            dirWatch.setLoopSleepTime(1_000); // 1 sec
            dirWatch.startWatcher();
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

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
        LOGGER.debug("f is: " + f + " and ofile is: " + ofile);
        try {
            FileIO.copyFile(f, ofile, true);
        } catch (IOException e) {
            LOGGER.error(e);
//            e.printStackTrace();
        }
    }

    private void fileWatchClose(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        ofile.delete();
        ViskitAssemblyView view = (ViskitAssemblyView) VGlobals.instance().getAssemblyController().getView();
        view.removeFromEventGraphPallette(f);
    }

    @Override
    public void addOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis) {
        dirWatch.addListener(lis);
    }

    @Override
    public void removeOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis) {
        dirWatch.removeListener(lis);
    }

    Set<RecentFileListener> recentListeners = new HashSet<>();

    @Override
    public void addRecentFileListListener(RecentFileListener lis)
    {
      recentListeners.add(lis);
    }

    @Override
    public void removeRecentFileListListener(RecentFileListener lis)
    {
      recentListeners.remove(lis);
    }

    private void notifyRecentFileListeners()
    {
      for(RecentFileListener lis : recentListeners) {
            lis.listChanged();
        }
    }

    private static final int RECENTLISTSIZE = 15;
    private Set<String> recentFileSet = new LinkedHashSet<>(RECENTLISTSIZE + 1);;

    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file
     */
    private void adjustRecentList(File file) {
        String s = file.getAbsolutePath().replaceAll("\\\\", "/");
        recentFileSet.remove(s);
        recentFileSet.add(s);      // to the top

        saveHistoryXML(recentFileSet);
        notifyRecentFileListeners();
    }

    private java.util.List<String> openEventGraphs;
    private void _setFileSet() {
        openEventGraphs = new ArrayList<>(4);
        if (historyConfig == null) {return;}
        String[] valueAr = getHistoryConfig().getStringArray(ViskitConfig.EG_HISTORY_KEY + "[@value]");
        int i = 0;
        for (String s : valueAr) {
            if (recentFileSet.add(s)) {
                String op = getHistoryConfig().getString(ViskitConfig.EG_HISTORY_KEY + "(" + i + ")[@open]");

                if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes"))) {
                    openEventGraphs.add(s);
                }

                notifyRecentFileListeners();
            }
            i++;
        }
    }

    private void saveHistoryXML(Set<String> recentFiles) {
        getHistoryConfig().clearTree(ViskitConfig.RECENT_EG_CLEAR_KEY);
        int ix = 0;

        // The value's modelPath is already delimited with "/"
        for (String value : recentFiles) {
            getHistoryConfig().setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + ix + ")[@value]", value);
            ix++;
        }
        getHistoryConfig().getDocument().normalize();
    }

    @Override
    public void clearRecentFileSet() {
        recentFileSet.clear();
        saveHistoryXML(recentFileSet);
        notifyRecentFileListeners();
    }

    @Override
    public Set<String> getRecentFileSet() {
        return getRecentFileSet(false);
    }

    private Set<String> getRecentFileSet(boolean refresh) {
        if (refresh || recentFileSet == null) {
            _setFileSet();
        }
        return recentFileSet;
    }

    private List<String> getOpenFileSet(boolean refresh) {
        if (refresh || openEventGraphs == null) {
            _setFileSet();
        }
        return openEventGraphs;
    }

    /* a component, e.g., model, wants to say something. */
    @Override
    public void messageUser(int typ, String msg) // typ is one of JOptionPane types
    {
        String title;
        switch (typ) {
            case JOptionPane.WARNING_MESSAGE:
                title = "Warning";
                break;
            case JOptionPane.ERROR_MESSAGE:
                title = "Error";
                break;
            case JOptionPane.INFORMATION_MESSAGE:
                title = "Information";
                break;
            case JOptionPane.PLAIN_MESSAGE:
            case JOptionPane.QUESTION_MESSAGE:
            default:
                title = "";
                break;
        }
        ((ViskitView) getView()).genericErrorReport(title, msg);
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
        ViskitModel[] mods = ((ViskitView) getView()).getOpenModels();
        for (ViskitModel mod : mods) {
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
        ((ViskitView) getView()).prepareToQuit();
        VGlobals.instance().quitEventGraphEditor();
    }

    @Override
    public void closeAll() {

        ViskitModel[] mods = ((ViskitView) getView()).getOpenModels();
        for (ViskitModel mod : mods) {
            setModel((mvcModel) mod);
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
        Model mod = (Model) getModel();
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

        Model mod = (Model) getModel();
        if (mod.getLastFile() != null) {
            fileWatchClose(mod.getLastFile());
            markConfigClosed(mod.getLastFile());
        }

        ((ViskitView) getView()).delTab(mod);
    }

    private void markConfigClosed(File f) {
        int idx = 0;
        for (String key : recentFileSet) {
            if (key.contains(f.getName())) {
                getHistoryConfig().setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + idx + ")[@open]", "false");
            }
            idx++;
        }
    }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    private void markConfigOpen(String path) {
        int idx = 0;
        for (String key : recentFileSet) {
            if (key.contains(path)) {
                getHistoryConfig().setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + idx + ")[@open]", "true");
                getHistoryConfig().setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + idx + ")[@value]", path);
            }
            idx++;
        }
    }

    @Override
    public void save() {
        ViskitModel mod = (ViskitModel) getModel();
        File localLastFile = mod.getLastFile();
        if (localLastFile == null) {
            saveAs();
        } else {
            ((ViskitModel) getModel()).saveModel(localLastFile);
            fileWatchSave(localLastFile);
        }
    }

    @Override
    public void saveAs() {
        ViskitModel mod = (ViskitModel) getModel();
        ViskitView view = (ViskitView) getView();
        GraphMetaData gmd = mod.getMetaData();
        File saveFile = view.saveFileAsk(gmd.packageName + Vstatics.getFileSeparator() + gmd.name + ".xml", false);

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
            mod.changeMetaData(gmd); // might have renamed

            mod.saveModel(saveFile);
            view.setSelectedEventGraphName(gmd.name);

            fileWatchSave(saveFile);
            adjustRecentList(saveFile);
        }
    }

    @Override
    public void newSimParameter() //------------------------
    {
        ((ViskitView) getView()).addParameterDialog();

    }

    @Override
    public void buildNewSimParameter(String name, String type, String initVal, String comment) {
        ((ViskitModel) getModel()).newSimParameter(name, type, initVal, comment);
    }

    @Override
    public void simParameterEdit(vParameter param) {
        boolean modified = ((ViskitView) getView()).doEditParameter(param);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeSimParameter(param);
        }
    }

    @Override
    public void codeBlockEdit(String s) {
        ((viskit.model.ViskitModel) getModel()).changeCodeBlock(s);
    }

    @Override
    public void stateVariableEdit(vStateVariable var) {
        boolean modified = ((ViskitView) getView()).doEditStateVariable(var);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeStateVariable(var);
        }
    }

    // Comes in from plus button
    @Override
    public void newStateVariable() {
        ((ViskitView) getView()).addStateVariableDialog();
    }

    // Comes in from view
    @Override
    public void buildNewStateVariable(String name, String type, String initVal, String comment) //----------------------------
    {
        ((viskit.model.ViskitModel) getModel()).newStateVariable(name, type, initVal, comment);
    }
    private Vector selectionVector = new Vector();

    @Override
    public void selectNodeOrEdge(Vector v) //------------------------------------
    {
        selectionVector = v;
        boolean ccbool = (selectionVector.size() > 0);
        ActionIntrospector.getAction(this, "copy").setEnabled(nodeSelected());
        ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
        ActionIntrospector.getAction(this, "newSelfRefEdge").setEnabled(ccbool);
    }
    private Vector copyVector = new Vector();

    @Override
    public void copy() //----------------
    {
        if (!nodeSelected()) {
            ((ViskitView) getView()).genericErrorReport("Unsupported Action", "Edges cannot be copied.");
            return;
        }
        copyVector = (Vector) selectionVector.clone();

        // Paste only works for node, check to enable/disable paste menu item
        handlePasteMenuItem();
    }

    private void handlePasteMenuItem() {
        ActionIntrospector.getAction(this, "paste").setEnabled(nodeCopied());
    }

    private boolean nodeCopied() {
        return nodeInVector(copyVector);
    }

    private boolean nodeSelected() {
        return nodeInVector(selectionVector);
    }

    private boolean nodeInVector(Vector v) {
        for (Iterator itr = v.iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (o instanceof EventNode) {
                return true;
            }
        }
        return false;

    }

    @Override
    public void paste() //-----------------
    {
        if (copyVector.size() <= 0) {
            return;
        }
        int x = 100, y = 100;
        int n = 0;
        // We only paste un-attached nodes (at first)
        for (Object o : copyVector) {
            if (o instanceof Edge) {
                continue;
            }
            String nm = ((ViskitElement) o).getName();
            ((viskit.model.ViskitModel) getModel()).newEvent(nm + "-copy", new Point(x + (20 * n), y + (20 * n)));
            n++;
        }
    }

    @Override
    public void cut() //---------------
    {
        if (selectionVector != null && selectionVector.size() > 0) {
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
            }  // remove leading stuff

            String specialNodeMsg = (localNodeCount > 0 ? "\n(Events remain in paste buffer, but attached edges are permanently deleted.)" : "");
            if (((ViskitView) getView()).genericAskYN("Remove element(s)?", "Confirm remove " + msg + "?" + specialNodeMsg) == JOptionPane.YES_OPTION) {
                // do edges first?
                copyVector = (Vector) selectionVector.clone();
                for (Object elem : copyVector) {
                    if (elem instanceof Edge) {
                        killEdge((Edge) elem);
                    } else if (elem instanceof EventNode) {
                        EventNode en = (EventNode) elem;
                        for (ViskitElement ed : en.getConnections()) {
                            killEdge((Edge) ed);
                        }
                        ((ViskitModel) getModel()).deleteEvent(en);
                    }
                }
            }
            handlePasteMenuItem();
        }
    }

    private void killEdge(Edge e) {
        if (e instanceof SchedulingEdge) {
            ((ViskitModel) getModel()).deleteEdge((SchedulingEdge) e);
        } else {
            ((ViskitModel) getModel()).deleteCancelEdge((CancellingEdge) e);
        }
    }

    @Override
    public void deleteSimParameter(vParameter p) {
        ((ViskitModel) getModel()).deleteSimParameter(p);
    }

    @Override
    public void deleteStateVariable(vStateVariable var) {
        ((ViskitModel) getModel()).deleteStateVariable(var);
    }

    private boolean checkSave() {
        if (((ViskitModel) getModel()).isDirty() || ((ViskitModel) getModel()).getLastFile() == null) {
            String msg = "The model will be saved.\nContinue?";
            String title = "Confirm";
            int ret = ((ViskitView) getView()).genericAskYN(title, msg);
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            save();
        }
        return true;
    }

    @Override
    public void generateJavaSource() {
        ViskitModel mod = (ViskitModel) getModel();
        File localLastFile = mod.getLastFile();
        if (!checkSave() || localLastFile == null) {
            return;
        }

        SimkitXML2Java x2j = null;
        try {
            x2j = new SimkitXML2Java(localLastFile);
            x2j.unmarshal();
        } catch (FileNotFoundException fnfe) {
            LOGGER.error(fnfe);
        }

        String source = VGlobals.instance().getAssemblyController().buildJavaEventGraphSource(x2j);
        LOGGER.debug(source);
        if (source != null && source.length() > 0) {
            String className = mod.getMetaData().packageName + "." +
                    mod.getMetaData().name;
            ((ViskitView) getView()).showAndSaveSource(className, source, localLastFile.getName());
        }
    }

    @Override
    public void showXML() {
        if (!checkSave() || ((ViskitModel) getModel()).getLastFile() == null) {
            return;
        }

        ((ViskitView) getView()).displayXML(((ViskitModel) getModel()).getLastFile());
    }

    @Override
    public void runAssemblyEditor() {
        if (VGlobals.instance().getAssemblyEditor() == null) {
            VGlobals.instance().buildAssemblyViewFrame(false);
        }
        VGlobals.instance().runAssemblyView();
    }

    @Override
    public void eventList() {
        // not used
        if (viskit.Vstatics.debug) {
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
        ((viskit.model.ViskitModel) getModel()).newEvent(nm, p);
    }

    @Override
    public void buildNewArc(Object[] nodes) //--------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((ViskitModel) getModel()).newEdge(src, tar);
    }

    @Override
    public void buildNewCancelArc(Object[] nodes) //--------------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultMutableTreeNode) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultMutableTreeNode) nodes[1]).getUserObject();
        ((ViskitModel) getModel()).newCancelEdge(src, tar);
    }

    public void newSelfRefEdge() //--------------------------
    {
        if (selectionVector != null && selectionVector.size() > 0) {
            for (Iterator itr = selectionVector.iterator(); itr.hasNext();) {
                Object o = itr.next();
                if (o instanceof EventNode) {
                    ((ViskitModel) getModel()).newEdge((EventNode) o, (EventNode) o);
                }
            }
        }
    }

    @Override
    public void editGraphMetaData() {
        ViskitModel mod = (ViskitModel) getModel();
        if (mod == null) {return;}
        GraphMetaData gmd = mod.getMetaData();
        boolean modified =
                EventGraphMetaDataDialog.showDialog(VGlobals.instance().getEventGraphEditor(), gmd);
        if (modified) {
            ((ViskitModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((ViskitView) getView()).setSelectedEventGraphName(gmd.name);
        }
    }

    @Override
    public void nodeEdit(viskit.model.EventNode node) // shouldn't be required
    //----------------------------------
    {
        boolean done;
        do {
            done = true;
            boolean modified = ((ViskitView) getView()).doEditNode(node);
            if (modified) {
                done = ((viskit.model.ViskitModel) getModel()).changeEvent(node);
            }
        } while (!done);
    }

    @Override
    public void arcEdit(viskit.model.SchedulingEdge ed) {
        boolean modified = ((ViskitView) getView()).doEditEdge(ed);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeEdge(ed);
        }
    }

    @Override
    public void canArcEdit(viskit.model.CancellingEdge ed) {
        boolean modified = ((ViskitView) getView()).doEditCancelEdge(ed);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeCancelEdge(ed);
        }
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    @Override
    public void captureWindow() {
        String fileName = "ViskitScreenCapture";

        // create and save the image
        EventGraphViewFrame egvf = (EventGraphViewFrame) getView();

        // Get only the jgraph part
        Component component = egvf.getCurrentJgraphComponent();
        File localLastFile = ((ViskitModel) getModel()).getLastFile();
        if (localLastFile != null) {
            fileName = localLastFile.getName();
        }

        File fil = ((ViskitView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", false);

        if (fil == null) {
            return;
        }

        final Timer tim = new Timer(100, new TimerCallback(fil, true, egvf, component));
        tim.setRepeats(false);
        tim.start();

        imgSaveCount = "" + (++imgSaveInt);
    }

    /** Provides an automatic capture of all Event Graphs images used in an
     * Assembly and stores them to a specified location for inclusion in the
     * generated Analyst Report
     *
     * @param eventGraphs a list of Event Graph paths to image capture
     * @param eventGraphImages a list of Event Graph image paths to write .png files
     */
    public void captureEventGraphImages(java.util.List<File> eventGraphs, java.util.List<String> eventGraphImages) {
        ListIterator<String> itr = eventGraphImages.listIterator(0);

        String eventGraphImage;
        File eventGraphImageFile;
        TimerCallback tcb;

        // create and save the image
        EventGraphViewFrame egvf = (EventGraphViewFrame) getView();

        // Get only the jgraph part
        Component component = egvf.getCurrentJgraphComponent();

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
            LOGGER.debug("eventGraph: " + eventGraph);

            // Now capture and store the Event Graph images
            if (itr.hasNext()) {
                eventGraphImage = itr.next();
                eventGraphImageFile = new File(eventGraphImage);
                LOGGER.debug("eventGraphImage is: " + eventGraphImage);
                tcb = new TimerCallback(eventGraphImageFile, false, egvf, component);

                // Make sure we have a directory ready to receive these images
                if (!eventGraphImageFile.getParentFile().isDirectory()) {
                    eventGraphImageFile.getParentFile().mkdirs();
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

        TimerCallback(File f, boolean b, JFrame frame, Component component) {
            fil = f;
            display = b;
            this.frame = frame;
            this.component = component;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {

            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
                LOGGER.debug("CurrentJgraphComponent is a JScrollPane: " + component);
            }
            Rectangle reg = component.getBounds();
            BufferedImage image = new BufferedImage(reg.width, reg.height, BufferedImage.TYPE_3BYTE_BGR);

            // Tell the jgraph component to draw into our memory
            component.paint(image.getGraphics());
            try {
                ImageIO.write(image, "png", fil);
            } catch (IOException e) {
                LOGGER.error(e);
            }

            // display a scaled version
            if (display) {
                JFrame localFrame = new JFrame("Saved as " + fil.getName());
                ImageIcon ii = new ImageIcon(image);
                JLabel lab = new JLabel(ii);
                localFrame.getContentPane().setLayout(new BorderLayout());
                localFrame.getContentPane().add(lab, BorderLayout.CENTER);
                localFrame.pack();
                localFrame.setLocationRelativeTo((Component) getView());
                localFrame.setVisible(true);
            }
        }
    }
    XMLConfiguration historyConfig;

    /** This is the very first caller for getViskitAppConfig() upon Viskit startup */
    private void initConfig() {
        try {
            historyConfig = ViskitConfig.instance().getViskitAppConfig();
        } catch (Exception e) {
            LOGGER.error("Error loading history file: " + e.getMessage());
            LOGGER.warn("Recent file saving disabled");
            historyConfig = null;
        }
    }

    private XMLConfiguration getHistoryConfig() {
        return historyConfig;
    }
}
