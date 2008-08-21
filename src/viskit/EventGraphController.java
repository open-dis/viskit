package viskit;

import actions.ActionIntrospector;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.FileIO;
import edu.nps.util.TempFileManager;
import java.util.HashSet;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;

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

    static File lastFile;
    static Logger log = Logger.getLogger(EventGraphController.class);

    public EventGraphController() {
        initConfig();
        initFileWatch();
        this._setFileLists();
    }

    public static File getLastFile() {
        return lastFile;
    }

    public void begin() {
        ArrayList<String> lis = getOpenFileList(false);

        // don't default to new event graph
        if (lis.size() <= 0) {
            log.debug("In begin() (if) of EventGraphController");
            return;
        } else {

            // If EventGraphs were already open without a corresponding Assembly
            // file open, then open them upon Viskit starting, else, let the
            // Assembly file tell which EGs to open
            if (VGlobals.instance().getAssemblyController() != null) {
                ArrayList<String> al = VGlobals.instance().getAssemblyController().getOpenFileList(false);
                if (al.size() == 0) {
                    log.debug("In begin() (else) of EventGraphController");
                    for (String s : lis) {
                        File f = new File(s);
                        if (f.exists()) {
                            _doOpen(f);
                        }
                    }
                }
            } else {return;}
        }
    }

    public void settings() {
        // placeholder for multi-tabbed combo app.
    }

    public void quit() {
        if (preQuit()) {
            postQuit();
        }
    }

    public boolean preQuit() //----------------
    {
        markConfigAllClosed();
        ViskitModel[] modAr = ((ViskitView) getView()).getOpenModels();
        for (int i = 0; i < modAr.length; i++) {
            setModel((mvcModel) modAr[i]);
            File f = modAr[i].getLastFile();
            if (f != null) {
                markConfigOpen(f);
            }
            if (preClose()) {
                postClose();
            } else {
                return false;
            } // cancelled
        }
        return true;
    }

    public void postQuit() {
        ((ViskitView) getView()).prepareToQuit();
        VGlobals.instance().quitEventGraphEditor();
    }

    /** Creates a new Viskit Project */
    public void newProject() {
        String msg = "Are you sure you want to close your current Viskit Project?";
        String title = "Close Current Project";
                
        int ret = ((ViskitView) getView()).genericAskYN(title, msg);
        if (ret == JOptionPane.YES_OPTION) {
            VGlobals.instance().getAssemblyController().close();
            ViskitConfig.instance().clearViskitConfig();
            VGlobals.instance().initProjectHome();
            VGlobals.instance().createWorkDirectory();            
        }
    }
    
    /** Opens an already existing Viskit Project
     * @param jfc the JFileChooser from the EventGraphViewFrame to select 
     * the project directory
     * @param egvf the EventGraphViewFrame for the JFileChooser's orientation
     */
    public void openProject(JFileChooser jfc, EventGraphViewFrame egvf) {
        String msg = "Are you sure you want to close your current Viskit Project?";
        String title = "Close Current Project";
                
        int ret = ((ViskitView) getView()).genericAskYN(title, msg);
        if (ret == JOptionPane.YES_OPTION) {
            int retv = jfc.showOpenDialog(egvf);
            if (retv == JFileChooser.APPROVE_OPTION) {
                VGlobals.instance().getAssemblyController().close();
                ViskitConfig.instance().clearViskitConfig();
                ViskitProject.MY_VISKIT_PROJECTS_DIR = jfc.getSelectedFile().getParent();
                ViskitConfig.instance().setVal(ViskitConfig.PROJECT_HOME_KEY, ViskitProject.MY_VISKIT_PROJECTS_DIR);
                ViskitProject.DEFAULT_PROJECT = jfc.getSelectedFile().getName();
                VGlobals.instance().createWorkDirectory();
            }
        }
    }
    
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
        ((ViskitView) getView()).addTab(mod, true);
//        editGraphMetaData();

        GraphMetaData gmd = new GraphMetaData(mod);
        if (oldGmd != null) {
            gmd.packageName = oldGmd.packageName;
        }

        boolean modified = EventGraphMetaDataDialog.showDialog(VGlobals.instance().getMainAppWindow(),
                VGlobals.instance().getMainAppWindow(), gmd);
        if (modified) {
            ((ViskitModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((ViskitView) getView()).setSelectedEventGraphName(gmd.name);
        }

        if (EventGraphMetaDataDialog.modified) {
            
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
     * @return true = continue, false = don't (i.e., we cancelled)
     */
    private boolean askToSaveAndContinue() {
        int yn = (((ViskitView) getView()).genericAskYN("Question", "Save modified graph?"));

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();
                if (((Model) getModel()).isDirty()) {
                    return false;
                } // we cancelled
                return true;
            //break;
            case JOptionPane.NO_OPTION:
                return true;
                
            // Something funny if we're here
            default:
                return false;
        }
    }

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

    public void _doOpen(File file) {
        ViskitView viskitView = (ViskitView) getView();
        Model mod = new Model(this);
        mod.init();

        // these may init to null on startup, check
        // before doing any openAlready lookups
        viskitView.addTab(mod, false);
        ViskitModel[] openAlready = null;
        if (viskitView != null) {
            openAlready = viskitView.getOpenModels();
        }
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
        if (!mod.newModel(file) || isOpenAlready) {
            viskitView.delTab(mod);   // Not a good open, tell view
            return;
        }
        viskitView.setSelectedEventGraphName(mod.getMetaData().name);
        adjustRecentList(file);

        fileWatchOpen(file);
    }
    private static final int RECENTLISTSIZE = 15;
    private ArrayList<String> recentFileList;

    public void openRecent() {
        ArrayList<String> v = getRecentFileList(true); // have a settings panel now ... false);
        if (v.size() <= 0) {
            open();
        } else {
            File file = ((ViskitView) getView()).openRecentFilesAsk(v);
            if (file != null) {
                _doOpen(file);
            }

            // v might have been changed
            setRecentFileList(v);
        }
    }

    public void openRecentEventGraph(String path) {
      _doOpen(new File(path));
    }
    
    // Support for informing listeners about open eventgraphs
    // Methods to implement a scheme where other modules will be informed of file changes //
    // (Would Java Beans do this with more or less effort?
    private DirectoryWatch dirWatch;
    private File watchDir;

    private void initFileWatch() {
        try { // TBD this may be obsolete
            watchDir = TempFileManager.createTempFile("egs", "current");   // actually creates
            String p = watchDir.getAbsolutePath();   // just want the name part of it
            watchDir.delete();        // Don't want the file to be made yet
            watchDir = new File(p);
            watchDir.mkdir();

            dirWatch = new DirectoryWatch(watchDir);
            dirWatch.setLoopSleepTime(1 * 1000); // 1 secs
            dirWatch.startWatcher();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fileWatchSave(File f) {
        fileWatchOpen(f);
    }

    /** Create a temporary location to store copies of EventGraphs in XML form.
     * This is to compare against any changes to and whether to re-cache the
     * MD5 hash generated elsewhere for this EG.
     * This is a known EventGraph compilation path
     * @param f the EventGraph file to generate MD5 hash for
     */
    private void fileWatchOpen(File f) {
        String nm = f.getName();
        lastFile = f;
        File ofile = new File(watchDir, nm);
        log.debug("f is: " + f + " and ofile is: " + ofile);
        try {
            FileIO.copyFile(f, ofile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ViskitAssemblyView view = (ViskitAssemblyView) AssemblyController.inst.getView();

        // not temporary, but use static method anyway
        PkgAndFile paf = AssemblyController.createTemporaryEventGraphClass(f);
        if (paf != null) {
            view.addToEventGraphPallette(f);
        } else {
            view.removeFromEventGraphPallette(f);
        }
    }

    private void fileWatchClose(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        ofile.delete();
        ViskitAssemblyView view = (ViskitAssemblyView) (AssemblyController.inst.getView());
        view.removeFromEventGraphPallette(f);
    }

    public void addOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis) {
        dirWatch.addListener(lis);
    }

    public void removeOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis) {
        dirWatch.removeListener(lis);
    }

    HashSet<RecentFileListener> recentListeners = new HashSet<RecentFileListener>();
    public void addRecentFileListListener(RecentFileListener lis)
    {
      recentListeners.add(lis);
    }
    
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
    
    /**
     * If passed file is in the list, move it to the top.  Else insert it;
     * Trim to RECENTLISTSIZE
     * @param file
     */
    private void adjustRecentList(File file) {
        String s = file.getAbsolutePath();
        int idx;
        if ((idx = recentFileList.indexOf(s)) != -1) {
            recentFileList.remove(idx);
        }
        recentFileList.add(0, s);      // to the top

        while (recentFileList.size() > RECENTLISTSIZE) {
            recentFileList.remove(recentFileList.size() - 1);
        }
        saveHistoryXML(recentFileList);
        notifyRecentFileListeners();
    }

    private ArrayList<String> openV;
    private void _setFileLists() {
        recentFileList = new ArrayList<String>(RECENTLISTSIZE + 1);
        openV = new ArrayList<String>(4);
        if (historyConfig == null) {return;}
        String[] valueAr = historyConfig.getStringArray(ViskitConfig.EG_HISTORY_KEY + "[@value]");
        int i = 0;
        for (String s : valueAr) {

            // Attempt to prevent dupicate entries
            if (recentFileList.contains(s)) {continue;}
            recentFileList.add(s);
            String op = historyConfig.getString(ViskitConfig.EG_HISTORY_KEY + "(" + i + ")[@open]");
            if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes"))) {
                openV.add(s);
            }
            i++;
        }
        notifyRecentFileListeners();
    }

    private void saveHistoryXML(ArrayList<String> recentFiles) {
        historyConfig.clearTree(ViskitConfig.RECENT_EG_CLEAR_KEY);

        for (int i = 0; i < recentFiles.size(); i++) {
            String value = recentFiles.get(i);
            historyConfig.setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + i + ")[@value]", value);
        }
        historyConfig.getDocument().normalize();
    }

    private void setRecentFileList(ArrayList<String> lis) {
        saveHistoryXML(lis);
    }

    private void markConfigAllClosed() {
        for (int i = 0; i < recentFileList.size(); i++) {
            historyConfig.setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + i + ")[@open]", "false");
        }
    }

    private void markConfigOpen(File f) {
        int idx = recentFileList.indexOf(f.getAbsolutePath());
        if (idx != -1) {
            historyConfig.setProperty(ViskitConfig.EG_HISTORY_KEY + "(" + idx + ")[@open]", "true");
        }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    }
    
    public void clearRecentFileList()
    {
        recentFileList.clear();
        saveHistoryXML(recentFileList);
        notifyRecentFileListeners();
    }

    public java.util.List<String> getRecentFileList()  // implement interface
    {
        return getRecentFileList(false);
    }
    
    private ArrayList<String> getRecentFileList(boolean refresh) {
        if (refresh || recentFileList == null) {
            _setFileLists();
        }
        return recentFileList;
    }

    private ArrayList<String> getOpenFileList(boolean refresh) {
        if (refresh || openV == null) {
            _setFileLists();
        }
        return openV;
    }

    /* a component, e.g., model, wants to say something. */
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

    public void close() {
        if (preClose()) {
            postClose();
        }
    }

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

    public void postClose() {
        Model mod = (Model) getModel();

        ((ViskitView) getView()).delTab(mod);

        if (mod.getLastFile() != null) {
            fileWatchClose(mod.getLastFile());
        }
    }

    public void closeAll() {
        ViskitModel[] modAr = ((ViskitView) getView()).getOpenModels();
        for (int i = 0; i < modAr.length; i++) {
            setModel((mvcModel) modAr[i]);
            close();
        }
    }

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

    public void saveAs() {
        ViskitModel mod = (ViskitModel) getModel();
        ViskitView view = (ViskitView) getView();
        GraphMetaData gmd = mod.getMetaData();

        File saveFile = view.saveFileAsk(gmd.name + ".xml", false);
        if (saveFile != null) {
            File localLastFile = mod.getLastFile();
            if (localLastFile != null) {
                fileWatchClose(localLastFile);
            }

            String n = saveFile.getName();
            if (n.endsWith(".xml") || n.endsWith(".XML")) {
                n = n.substring(0, n.length() - 4);
            }
            gmd.name = n;
            mod.changeMetaData(gmd); // might have renamed

            mod.saveModel(saveFile);
            view.setSelectedEventGraphName(n);

            fileWatchOpen(saveFile);
            adjustRecentList(saveFile);
        }
    }

    public void newSimParameter() //------------------------
    {
        ((ViskitView) getView()).addParameterDialog();

    }

    public void buildNewSimParameter(String name, String type, String initVal, String comment) {
        ((ViskitModel) getModel()).newSimParameter(name, type, initVal, comment);
    }

    public void simParameterEdit(vParameter param) {
        boolean modified = ((ViskitView) getView()).doEditParameter(param);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeSimParameter(param);
        }
    }

    public void codeBlockEdit(String s) {
        ((viskit.model.ViskitModel) getModel()).changeCodeBlock(s);
    }

    public void stateVariableEdit(vStateVariable var) {
        boolean modified = ((ViskitView) getView()).doEditStateVariable(var);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeStateVariable(var);
        }
    }

    // Comes in from plus button
    public void newStateVariable() {
        ((ViskitView) getView()).addStateVariableDialog();
    }

    // Comes in from view
    public void buildNewStateVariable(String name, String type, String initVal, String comment) //----------------------------
    {
        ((viskit.model.ViskitModel) getModel()).newStateVariable(name, type, initVal, comment);
    }
    private Vector selectionVector = new Vector();

    public void selectNodeOrEdge(Vector v) //------------------------------------
    {
        selectionVector = v;
        boolean ccbool = (selectionVector.size() > 0 ? true : false);
        ActionIntrospector.getAction(this, "copy").setEnabled(nodeSelected());
        ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
        ActionIntrospector.getAction(this, "newSelfRefEdge").setEnabled(ccbool);
    }
    private Vector copyVector = new Vector();

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
            String nm = ((viskit.model.EventNode) o).getName();
            ((viskit.model.ViskitModel) getModel()).newEvent(nm + "-copy", new Point(x + (20 * n), y + (20 * n)));
            n++;
        }
    }

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

    public void deleteSimParameter(vParameter p) {
        ((ViskitModel) getModel()).deleteSimParameter(p);
    }

    public void deleteStateVariable(vStateVariable var) {
        ((ViskitModel) getModel()).deleteStateVariable(var);
    }

    // TODO: This will throw a null pointer if no Event Graph is loaded
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

    public void generateJavaClass() {
        File localLastFile = ((ViskitModel) getModel()).getLastFile();
        if (!checkSave() || localLastFile == null) {
            return;
        }
        String source = ((ViskitModel) getModel()).buildJavaSource();
        log.debug(source);
        if (source != null && source.length() > 0) {
            String className = ((ViskitModel) getModel()).getMetaData().packageName + "." + ((ViskitModel) getModel()).getMetaData().name;
            ((ViskitView) getView()).showAndSaveSource(className, source, localLastFile.getName());
        }
    }

    public void showXML() {
        if (checkSave() == false || ((ViskitModel) getModel()).getLastFile() == null) {
            return;
        }

        ((ViskitView) getView()).displayXML(((ViskitModel) getModel()).getLastFile());
    }

    public void runAssemblyEditor() {
        if (VGlobals.instance().getAssemblyEditor() == null) {
            VGlobals.instance().buildAssemblyViewFrame(false);
        }
        VGlobals.instance().runAssemblyView();
    }

    public void eventList() {
        // not used
        if (viskit.Vstatics.debug) {
            System.out.println("EventListAction in " + this);
        }
    }
    private int nodeCount = 0;

    public void newNode() //-------------------
    {
        buildNewNode(new Point(100, 100));
    }

    public void buildNewNode(Point p) //--------------------------
    {
        buildNewNode(p, "evnt_" + nodeCount++);
    }

    public void buildNewNode(Point p, String nm) //------------------------------------
    {
        ((viskit.model.ViskitModel) getModel()).newEvent(nm, p);
    }

    public void buildNewArc(Object[] nodes) //--------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultGraphCell) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultGraphCell) nodes[1]).getUserObject();
        ((ViskitModel) getModel()).newEdge(src, tar);
    }

    public void buildNewCancelArc(Object[] nodes) //--------------------------------------
    {
        // My node view objects hold node model objects and vice versa
        EventNode src = (EventNode) ((DefaultGraphCell) nodes[0]).getUserObject();
        EventNode tar = (EventNode) ((DefaultGraphCell) nodes[1]).getUserObject();
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

    public void editGraphMetaData() {
        GraphMetaData gmd = ((ViskitModel) getModel()).getMetaData();
        boolean modified = EventGraphMetaDataDialog.showDialog(VGlobals.instance().getMainAppWindow(),
                VGlobals.instance().getMainAppWindow(), gmd);
        if (modified) {
            ((ViskitModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((ViskitView) getView()).setSelectedEventGraphName(gmd.name);
        }
    }

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

    public void arcEdit(viskit.model.SchedulingEdge ed) //------------------------------------
    {
        boolean modified = ((ViskitView) getView()).doEditEdge(ed);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeEdge(ed);
        }
    }

    public void canArcEdit(viskit.model.CancellingEdge ed) //---------------------------------------
    {
        boolean modified = ((ViskitView) getView()).doEditCancelEdge(ed);
        if (modified) {
            ((viskit.model.ViskitModel) getModel()).changeCancelEdge(ed);
        }
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    public void captureWindow() //-------------------------
    {
        String fileName = "ViskitScreenCapture";
        File localLastFile = ((ViskitModel) getModel()).getLastFile();
        if (localLastFile != null) {
            fileName = localLastFile.getName();
        }

        File fil = ((ViskitView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", false);

        if (fil == null) {
            return;
        }

        final Timer tim = new Timer(100, new TimerCallback(fil, true));
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
    public void captureEventGraphImages(LinkedList<File> eventGraphs, LinkedList<String> eventGraphImages) {
        ListIterator<String> itr = eventGraphImages.listIterator(0);
        
        String eventGraphImage;
        File eventGraphImageFile;
        TimerCallback tcb;

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
            log.debug("eventGraph: " + eventGraph);

            // Now capture and store the Event Graph images
            if (itr.hasNext()) {
                eventGraphImage = itr.next();
                eventGraphImageFile = new File(eventGraphImage);
                log.debug("eventGraphImage is: " + eventGraphImage);
                tcb = new TimerCallback(eventGraphImageFile, false);

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

        TimerCallback(File f, boolean b) {
            fil = f;
            display = b;
        }

        public void actionPerformed(ActionEvent ev) {

            // create and save the image
            EventGraphViewFrame egvf = (EventGraphViewFrame) getView();

            // Get only the jgraph part
            Component component = egvf.getCurrentJgraphComponent();

            if (component instanceof JScrollPane) {
                component = ((JScrollPane) component).getViewport().getView();
                log.debug("CurrentJgraphComponent is a JScrollPane: " + component);
            }
            Rectangle reg = component.getBounds();
            BufferedImage image = new BufferedImage(reg.width, reg.height, BufferedImage.TYPE_3BYTE_BGR);

            // Tell the jgraph component to draw into our memory
            component.paint(image.getGraphics());
            try {
                ImageIO.write(image, "png", fil);
            } catch (IOException e) {
                System.out.println("Controller Exception in capturing screen: " + e.getMessage());
                return;
            }

            // display a scaled version
            if (display) {
                JFrame frame = new JFrame("Saved as " + fil.getName());
                ImageIcon ii = new ImageIcon(image);
                JLabel lab = new JLabel(ii);
                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(lab, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo((Component) getView());
                frame.setVisible(true);
            }
        }
    }
    XMLConfiguration historyConfig;

    /** This is the very first caller for getViskitConfig() upon Viskit startup */
    private void initConfig() {
        try {
            historyConfig = ViskitConfig.instance().getViskitConfig();
        } catch (Exception e) {
            System.out.println("Error loading history file: " + e.getMessage());
            System.out.println("Recent file saving disabled");
            historyConfig = null;
        }
    }
}
