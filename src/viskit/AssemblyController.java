package viskit;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import actions.ActionIntrospector;
import edu.nps.util.DirectoryWatch;
import edu.nps.util.TempFileManager;
import java.lang.reflect.Field;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.mvc.mvcModel;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.translator.SimkitXML2Java;
import viskit.util.Compiler;
import viskit.util.XMLValidationTool;


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
public class AssemblyController extends mvcAbstractController implements ViskitAssemblyController, OpenAssembly.AssyChangeListener {

    /** Static instance of this Controller */
    public static AssemblyController inst;
    static Logger log = Logger.getLogger(AssemblyController.class);
    Class<?> simEvSrcClass, simEvLisClass, propChgSrcClass, propChgLisClass;
    private String initialFile;
    private JTabbedPane runTabbedPane;
    private int runTabbedPaneIdx;

    /** Creates a new instance of AssemblyController */
    public AssemblyController() {
        initConfig();
        inst = this;
    }

    /**
     * 
     * @param fil
     */
    public void setInitialFile(String fil) {
        if (viskit.Vstatics.debug) {
            System.out.println("Initial file set: " + fil);
        }
        initialFile = fil;
    }

    /** Begin Viskit's initial state upon startup */
    public void begin() {

        File f;
        if (initialFile != null) {
            log.debug("Loading initial file: " + initialFile);
            f = new File(initialFile);

            _doOpen(f);
            compileAssemblyAndPrepSimRunner();
        } else {
            ArrayList<String> lis = getOpenFileList(false);
            log.debug("Inside begin() and lis.size() is: " + lis.size());

            for (String assyFile : lis) {

                f = new File(assyFile);
                _doOpen(f);
            }
        }

        // The following comments were an attempt to solve classloader issues that needed to be solved
        // a different way
        try {
            simEvSrcClass = Vstatics.classForName("simkit.SimEventSource");
            simEvLisClass = Vstatics.classForName("simkit.SimEventListener");
            propChgSrcClass = Vstatics.classForName("simkit.PropertyChangeSource");
            propChgLisClass = Vstatics.classForName("java.beans.PropertyChangeListener");
            if (simEvSrcClass == null || simEvLisClass == null || propChgSrcClass == null || propChgLisClass == null) {
                throw new ClassNotFoundException();
            }
        } catch (ClassNotFoundException e) {
            ((ViskitAssemblyView) getView()).genericErrorReport("Internal error", "simkit.jar not in classpath");
        }
    }

    /**
     * 
     */
    public void runEventGraphEditor() {
        if (VGlobals.instance().getEventGraphEditor() == null) {
            VGlobals.instance().buildEventGraphViewFrame();
        }
        VGlobals.instance().runEventGraphView();
    }

    /**
     * Perform Assembly Editor shutdown duties
     * @return true if Assembly was saved if dirty (modified)
     */
    public boolean preQuit() {
        ViskitAssemblyModel[] modAr = ((ViskitAssemblyView) getView()).getOpenModels();
        for (ViskitAssemblyModel mod : modAr) {
            setModel((mvcModel) mod);
            File f = mod.getLastFile();
            if (f != null) {
                markConfigOpen(f);
            }
            if (preClose()) {
                postClose();
            } else {
                return false;
            } // cancelled
        }
        return checkSaveIfDirty();
    }

    /**
     * 
     */
    public void postQuit() {
        ((ViskitAssemblyView) getView()).prepareToQuit();
        VGlobals.instance().quitAssemblyEditor();
    }

    /**
     * 
     */
    public void quit() {
        if (preQuit()) {
            postQuit();
        }
    }

    /** Information required by the EventGraphController to see if an Assembly
     * file is already open.  Also checked internally by this class.
     * @param refresh flag to refresh the list from viskitConfig.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    public final ArrayList<String> getOpenFileList(boolean refresh) {
        if (refresh || openV == null) {
            _setFileLists();
        }
        return openV;
    }

    /**
     * @param runTabbedPane a hook to the Assembly Run panel
     * @param idx the index of the Assembly Run Tab
     */
    public void setRunTabbedPane(JComponent runTabbedPane, int idx) {
        this.runTabbedPane = (JTabbedPane) runTabbedPane;
        this.runTabbedPaneIdx = idx;
        this.runTabbedPane.setEnabledAt(this.runTabbedPaneIdx, false);
    }

    private boolean checkSaveIfDirty() {
        if (localDirty) {
            StringBuffer sb = new StringBuffer("<html><center>Execution parameters have been modified.<br>(");

            for (Iterator itr = isLocalDirty.iterator(); itr.hasNext();) {
                sb.append(((OpenAssembly.AssyChangeListener) itr.next()).getHandle());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // last comma-space
            sb.append(")<br>Choose yes if you want to stop this operation, then manually select<br>the indicated tab(s) to ");
            sb.append("save the execution parameters.");

            int yn = (((ViskitAssemblyView) getView()).genericAsk2Butts("Question", sb.toString(), "Stop and let me save",
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
        AssemblyModel mod = (AssemblyModel) getModel();
        if (mod != null) {
            if (((AssemblyModel) getModel()).isDirty()) {
                return askToSaveAndContinue();
            }
        }
        return ret;  // proceed
    }

    /**
     * 
     */
    public void settings() {
        // placeholder for combo gui
    }

    /** Opens a Viskit Project Assembly File */
    public void open() {

        if (!checkSaveIfDirty()) {
            return;
        }

        File[] files = ((ViskitAssemblyView) getView()).openFilesAsk();
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

        ViskitAssemblyView vaw = (ViskitAssemblyView) getView();
        AssemblyModel mod = new AssemblyModel(this);
        mod.init();
        VGlobals.instance().amod = mod;
        
        vaw.addTab(mod);

        // these may init to null on startup, check
        // before doing any openAlready lookups
        ViskitAssemblyModel[] openAlready = null;
        if (vaw != null) {
            openAlready = vaw.getOpenModels();
        }
        boolean isOpenAlready = false;
        if (openAlready != null) {
            for (ViskitAssemblyModel model : openAlready) {
                if (model.getLastFile() != null) {
                    String path = model.getLastFile().getAbsolutePath();
                    if (path.equals(file.getAbsolutePath())) {
                        isOpenAlready = true;
                    }
                }
            }
        }

        if (mod.newModel(file) && !isOpenAlready) {
            ((ViskitAssemblyView) getView()).setSelectedAssemblyName(mod.getMetaData().name);
            adjustRecentList(file);

            // replaces old fileWatchOpen(file);
            initOpenAssyWatch(file, mod.getJaxbRoot());
            openEventGraphs(file);
            if (runTabbedPane.isEnabledAt(this.runTabbedPaneIdx)) {
                runTabbedPane.setEnabledAt(this.runTabbedPaneIdx, false);
            }
        } else {
            vaw.delTab(mod);
        }
    }

    protected void initOpenAssyWatch(File f, SimkitAssembly jaxbroot) {
        try {
            OpenAssembly.inst().setFile(f, jaxbroot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @return
     */
    public OpenAssembly.AssyChangeListener getAssemblyChangeListener() {
        return assyChgListener;
    }
    private boolean localDirty = false;
    private HashSet<OpenAssembly.AssyChangeListener> isLocalDirty = new HashSet<OpenAssembly.AssyChangeListener>();
    OpenAssembly.AssyChangeListener assyChgListener = new OpenAssembly.AssyChangeListener() {

        public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param) {
            switch (action) {
                case JAXB_CHANGED:
                    isLocalDirty.remove(source);
                    if (isLocalDirty.isEmpty()) {
                        localDirty = false;
                    }

                    ((ViskitAssemblyModel) getModel()).setDirty(true);
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
                    break;

                default:
                    System.err.println("Program error AssemblyController.assyChanged");
            }
        }

        public String getHandle() {
            return "Assembly Controller";
        }
    };

    /**
     * 
     * @return
     */
    public String getHandle() {
        return assyChgListener.getHandle();
    }

    /**
     * 
     * @param action
     * @param source
     * @param param
     */
    public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param) {
        assyChgListener.assyChanged(action, source, param);
    }
    /////////////////////////////////////////////////////////////////////////////////////
    // Methods to implement a scheme where other modules will be informed of file changes //
    // (Would Java Beans do this with more or less effort?
    private File watchDir;

    private void initFileWatch() {
        try {
            watchDir = TempFileManager.createTempFile("assy", "current");   // actually creates
            String p = watchDir.getAbsolutePath();   // just want the name part of it
            watchDir.delete();        // Don't want the file to be made yet
            watchDir = new File(p);
            watchDir.mkdir();

            DirectoryWatch dirWatch = new DirectoryWatch(watchDir);
            dirWatch.setLoopSleepTime(1 * 1000); // 1 secs
            dirWatch.startWatcher();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fileWatchClose(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        ofile.delete();
    }

    /**
     * 
     * @param lis
     */
    public void addAssemblyFileListener(OpenAssembly.AssyChangeListener lis) {
        OpenAssembly.inst().addListener(lis);
    }

    /**
     * 
     * @param lis
     */
    public void removeAssemblyFileListener(OpenAssembly.AssyChangeListener lis) //DirectoryWatch.DirectoryChangeListener lis)
    {
        OpenAssembly.inst().removeListener(lis);
    }
    HashSet<RecentFileListener> recentListeners = new HashSet<RecentFileListener>();

    public void addRecentFileListListener(RecentFileListener lis) {
        recentListeners.add(lis);
    }

    public void removeRecentFileListListener(RecentFileListener lis) {
        recentListeners.remove(lis);
    }

    private void notifyRecentFileListeners() {
        for (RecentFileListener lis : recentListeners) {
            lis.listChanged();
        }
    }    /////////////////////////////////////////////////////////////////////////////////////
    /** Here we are informed of open Event Graphs */
    DirectoryWatch.DirectoryChangeListener egListener = new DirectoryWatch.DirectoryChangeListener() {

        public void fileChanged(File file, int action, DirectoryWatch source) {
            // Do nothing?
        }
    };

    /** @return a DirectoryChangeListener */
    public DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener() {
        return egListener;
    }

    /** Save the current Assembly File */
    public void save() {
        ViskitAssemblyModel mod = (ViskitAssemblyModel) getModel();
        if (mod.getLastFile() == null) {
            saveAs();
        } else {
            mod.saveModel(mod.getLastFile());
        }
    }

    /** Save the current Assembly File "as" desired by user */
    public void saveAs() {
        ViskitAssemblyModel model = (ViskitAssemblyModel) getModel();
        ViskitAssemblyView view = (ViskitAssemblyView) getView();
        GraphMetaData gmd = model.getMetaData();
        File saveFile = view.saveFileAsk(gmd.packageName + Vstatics.getFileSeparator() + gmd.name + ".xml", false);

        if (saveFile != null) {
            if (model.getLastFile() != null) {
                fileWatchClose(model.getLastFile());
            }

            String n = saveFile.getName();
            if (n.toLowerCase().endsWith(".xml")) {
                n = n.substring(0, n.length() - 4);
            }
            gmd.name = n;
            model.changeMetaData(gmd); // might have renamed
            
            model.saveModel(saveFile);
            view.setSelectedAssemblyName(gmd.name);
            adjustRecentList(saveFile);
        }
    }

    /** Edit the properties (metadata) of the Assembly */
    public void editGraphMetaData() {
        GraphMetaData gmd = ((ViskitAssemblyModel) getModel()).getMetaData();
        boolean modified = 
                AssemblyMetaDataDialog.showDialog(VGlobals.instance().getAssemblyEditor(), gmd);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeMetaData(gmd);

            // update title bar
            ((ViskitAssemblyView) getView()).setSelectedAssemblyName(gmd.name);
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
            egCountField = AssemblyController.class.getDeclaredField("egNodeCount");
            adptrCountField = AssemblyController.class.getDeclaredField("adptrNodeCount");
            pclCountField = AssemblyController.class.getDeclaredField("pclNodeCount");
        } catch (Exception ex) {
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
            ViskitAssemblyModel model = (ViskitAssemblyModel) getModel();
            do {
                retn = shortname + count++;
            } while (model.nameExists(retn));   // don't force the model to mangle the name
            intField.setInt(this, count);
        } catch (Exception ex) {
            System.err.println("Program error in AssemblyController.shortName" + ex.getLocalizedMessage());
        }
        return retn;
    }

    /** Creates a new Viskit Project */
    public void newProject() {
        String msg = "Are you sure you want to close your current Viskit Project?";
        String title = "Close Current Project";

        int ret = ((ViskitAssemblyView) getView()).genericAskYN(title, msg);
        if (ret == JOptionPane.YES_OPTION) {
            closeAll();
            ViskitConfig.instance().clearViskitConfig();
            VGlobals.instance().initProjectHome();
            VGlobals.instance().createWorkDirectory();
        }
    }

    /** Opens an already existing Viskit Project
     * @param jfc the JFileChooser from the EventGraphViewFrame to select 
     * the project directory
     * @param avf the AssemblyViewFrame for the JFileChooser's orientation
     */
    public void openProject(JFileChooser jfc, AssemblyViewFrame avf) {
        String msg = "Are you sure you want to close your current Viskit Project?";
        String title = "Close Current Project";

        int ret = ((ViskitAssemblyView) getView()).genericAskYN(title, msg);
        if (ret == JOptionPane.YES_OPTION) {
            int retv = jfc.showOpenDialog(avf);
            if (retv == JFileChooser.APPROVE_OPTION) {
                closeAll();
                ViskitConfig.instance().clearViskitConfig();
                ViskitProject.MY_VISKIT_PROJECTS_DIR = jfc.getSelectedFile().getParent();
                ViskitConfig.instance().setVal(ViskitConfig.PROJECT_HOME_KEY, ViskitProject.MY_VISKIT_PROJECTS_DIR);
                ViskitProject.DEFAULT_PROJECT = jfc.getSelectedFile().getName();
                VGlobals.instance().createWorkDirectory();
            }
        }
    }

    /** Create a new blank assembly graph model */
    public void newAssembly() {
        if (!checkSaveIfDirty()) {
            return;
        }
        GraphMetaData oldGmd = null;        
        ViskitAssemblyModel viskitAssemblyModel = (ViskitAssemblyModel) getModel();
        if (viskitAssemblyModel != null) {
            oldGmd = viskitAssemblyModel.getMetaData();
        }

        AssemblyModel mod = new AssemblyModel(this);
        mod.init();
        mod.newModel(null);
        VGlobals.instance().amod = mod;
        
        // No model set in controller yet...it gets set
        // when TabbedPane changelistener detects a tab change.
        ((ViskitAssemblyView) getView()).addTab(mod);

        GraphMetaData gmd = new GraphMetaData(mod);   // build a new one, specific to Assy
        if (oldGmd != null) {
            gmd.packageName = oldGmd.packageName;
        }

        boolean modified = 
                AssemblyMetaDataDialog.showDialog(VGlobals.instance().getAssemblyEditor(), gmd);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeMetaData(gmd);
            ((ViskitAssemblyView) getView()).setSelectedAssemblyName(gmd.name);  // into title bar
        } else {
            ((ViskitAssemblyView) getView()).delTab(mod);
        }
    }

    /** Calls both pre and post closing actions */
    public void close() {
        if (preClose()) {
            postClose();
        }
    }

    /** @return in most cases, a true */
    public boolean preClose() {
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        boolean ret = true;
        if (vmod != null) {
            if (vmod.isDirty()) {
                return askToSaveAndContinue();
            }
        }
        return ret;
    }

    /** Clean up for closing Assembly models */
    public void postClose() {
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();        
        ViskitAssemblyView view = (ViskitAssemblyView) getView();        
        ViskitAssemblyModel[] modAr = view.getOpenModels();        
        
        // Close any currently open EGs because we don't yet know which ones
        // to keep open until iterating through each remaining model
        VGlobals.instance().getEventGraphEditor().controller.closeAll();
        
        if (modAr.length == 0) {
            return;
        }

        view.delTab(vmod);
               
        if (vmod.getLastFile() != null) {
            fileWatchClose(vmod.getLastFile());
        }
        
        // Keep the other Assembly's EGs open 
        if (!isCloseAll()) {
            for (ViskitAssemblyModel mod : modAr) {
                if (!mod.equals(vmod)) {
                    openEventGraphs(mod.getLastFile());
                }
            }
        }

        runTabbedPane.setEnabledAt(this.runTabbedPaneIdx, false);
    }
    
    private boolean closeAll = false;
    public void closeAll() {
        
        // Close any currently open EGs because we don't yet know which ones
        // to keep open until iterating through each remaining model
        VGlobals.instance().getEventGraphEditor().controller.closeAll();
        
        ViskitAssemblyModel[] modAr = ((ViskitAssemblyView) getView()).getOpenModels();
        for (ViskitAssemblyModel mod : modAr) {
            setModel((mvcModel) mod);                        
            setCloseAll(true);
            close();
        }
        
        setCloseAll(false);
    }
    
    private Point nextPoint = new Point(25, 25);
    private Point getNextPoint() {
        nextPoint.x = nextPoint.x >= 200 ? 25 : nextPoint.x + 25;
        nextPoint.y = nextPoint.y >= 200 ? 25 : nextPoint.y + 25;
        return nextPoint;
    }

    /**
     * 
     */
    public void newEventGraphNode() // menu click
    {
        Object o = ((ViskitAssemblyView) getView()).getSelectedEventGraph();

        if (o != null) {
            if (o instanceof Class) {
                newEventGraphNode(((Class) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssyNode) {
                newFileBasedEventGraphNode((FileBasedAssyNode) o, getNextPoint());
                return;
            }
        }
        // Nothing selected or non-leaf
        ((ViskitAssemblyView) getView()).genericErrorReport("Can't create", "You must first select an Event Graph from the panel on the left.");
    }

    /**
     * 
     * @param typeName
     * @param p
     */
    public void newEventGraphNode(String typeName, Point p) {
        String shName = shortEgName(typeName);
        ((viskit.model.AssemblyModel) getModel()).newEventGraph(shName, typeName, p);
    }

    /**
     * 
     * @param xnode
     * @param p
     */
    public void newFileBasedEventGraphNode(FileBasedAssyNode xnode, Point p) {
        String shName = shortEgName(xnode.loadedClass);
        ((viskit.model.ViskitAssemblyModel) getModel()).newEventGraphFromXML(shName, xnode, p);
    }

    /**
     * 
     */
    public void newPropChangeListenerNode() // menu click
    {
        Object o = ((ViskitAssemblyView) getView()).getSelectedPropChangeListener();

        if (o != null) {
            if (o instanceof Class) {
                newPropChangeListenerNode(((Class) o).getName(), getNextPoint());
                return;
            } else if (o instanceof FileBasedAssyNode) {
                newFileBasedPropChangeListenerNode((FileBasedAssyNode) o, getNextPoint());
                return;
            }
        }
        // If nothing selected or a non-leaf
        ((ViskitAssemblyView) getView()).genericErrorReport("Can't create", "You must first select a Property Change Listener from the panel on the left.");
    }

    /**
     * 
     * @param name
     * @param p
     */
    public void newPropChangeListenerNode(String name, Point p) {
        String shName = shortPCLName(name);
        ((viskit.model.AssemblyModel) getModel()).newPropChangeListener(shName, name, p);
    }

    /**
     * 
     * @param xnode
     * @param p
     */
    public void newFileBasedPropChangeListenerNode(FileBasedAssyNode xnode, Point p) {
        String shName = shortPCLName(xnode.loadedClass);
        ((viskit.model.AssemblyModel) getModel()).newPropChangeListenerFromXML(shName, xnode, p);

    }

    /**
     *
     * @return true = continue, false = don't (i.e., we cancelled)
     */
    private boolean askToSaveAndContinue() {
        int yn = (((ViskitAssemblyView) getView()).genericAskYN("Question", "Save modified assembly?"));

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();
                if (((AssemblyModel) getModel()).isDirty()) {
                    return false;
                } // we cancelled
                return true;
            case JOptionPane.NO_OPTION:
                return true;

            // Something funny if we're here
            default:
                return false;
        }
    }

    /**
     * 
     * @param nodes
     */
    public void newAdapterArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultGraphCell) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultGraphCell) nodes[1]).getUserObject();

        AssemblyNode[] oArr = null;
        try {
            oArr = checkLegalForSEListenerArc(oA, oB);
        } catch (Exception e) {
            ((ViskitAssemblyView) getView()).genericErrorReport("Connection error.", "Possible class not found.  All referenced entities must be in a list at left.");
            return;
        }
        if (oArr == null) {
            ((ViskitAssemblyView) getView()).genericErrorReport("Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        adapterEdgeEdit(((ViskitAssemblyModel) getModel()).newAdapterEdge(shortAdapterName(""), oArr[0], oArr[1]));
    }

    /**
     * 
     * @param nodes
     */
    public void newSimEvListArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultGraphCell) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultGraphCell) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForSEListenerArc(oA, oB);

        if (oArr == null) {
            ((ViskitAssemblyView) getView()).genericErrorReport("Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        ((ViskitAssemblyModel) getModel()).newSimEvLisEdge(oArr[0], oArr[1]);
    }

    /**
     * 
     * @param nodes
     */
    public void newPropChangeListArc(Object[] nodes) {
        // One and only one has to be a prop change listener
        AssemblyNode oA = (AssemblyNode) ((DefaultGraphCell) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultGraphCell) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForPropChangeArc(oA, oB);

        if (oArr == null) {
            ((ViskitAssemblyView) getView()).genericErrorReport("Incompatible connection", "The nodes must be a PropertyChangeListener and PropertyChangeSource combination.");
            return;
        }
        pcListenerEdgeEdit(((ViskitAssemblyModel) getModel()).newPclEdge(oArr[0], oArr[1]));
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
        return Vstatics.classForName(o.getType());
    }

    AssemblyNode[] orderPCLSrcAndLis(AssemblyNode a, AssemblyNode b, Class<?> ca, Class<?> cb) {
        AssemblyNode[] obArr = new AssemblyNode[2];
        // tbd, reloading these classes is needed right now as
        // we don't know if the workClassLoader is the same instance
        // as it used to be when these were originally loaded
        // the tbd here is to see if there can be a shared root loader
        simEvSrcClass = Vstatics.classForName("simkit.SimEventSource");
        simEvLisClass = Vstatics.classForName("simkit.SimEventListener");
        propChgSrcClass = Vstatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = Vstatics.classForName("java.beans.PropertyChangeListener");
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
        simEvSrcClass = Vstatics.classForName("simkit.SimEventSource");
        simEvLisClass = Vstatics.classForName("simkit.SimEventListener");
        propChgSrcClass = Vstatics.classForName("simkit.PropertyChangeSource");
        propChgLisClass = Vstatics.classForName("java.beans.PropertyChangeListener");
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
    public void pcListenerEdit(PropChangeListenerNode pclNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((ViskitAssemblyView) getView()).doEditPclNode(pclNode);
            if (modified) {
                done = ((ViskitAssemblyModel) getModel()).changePclNode(pclNode);
            }
        } while (!done);
    }

    /**
     * 
     * @param evNode
     */
    public void evGraphEdit(EvGraphNode evNode) {
        boolean done;
        do {
            done = true;
            boolean modified = ((ViskitAssemblyView) getView()).doEditEvGraphNode(evNode);
            if (modified) {
                done = ((ViskitAssemblyModel) getModel()).changeEvGraphNode(evNode);
            }
        } while (!done);

    }

    /**
     * 
     * @param pclEdge
     */
    public void pcListenerEdgeEdit(PropChangeEdge pclEdge) {
        boolean modified = ((ViskitAssemblyView) getView()).doEditPclEdge(pclEdge);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changePclEdge(pclEdge);
        }
    }

    /**
     * 
     * @param aEdge
     */
    public void adapterEdgeEdit(AdapterEdge aEdge) {
        boolean modified = ((ViskitAssemblyView) getView()).doEditAdapterEdge(aEdge);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeAdapterEdge(aEdge);
        }
    }

    /**
     * 
     * @param seEdge
     */
    public void simEvListenerEdgeEdit(SimEvListenerEdge seEdge) {
        boolean modified = ((ViskitAssemblyView) getView()).doEditSimEvListEdge(seEdge);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeSimEvEdge(seEdge);
        }
    }
    private Vector<Object> selectionVector = new Vector<Object>();

    /**
     * 
     * @param v
     */
    public void selectNodeOrEdge(Vector<Object> v) {
        selectionVector = v;
        boolean ccbool = (selectionVector.size() > 0);
        ActionIntrospector.getAction(this, "copy").setEnabled(ccbool);
        ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
        int egCount = 0;
        for (Object o : selectionVector) {
            if (o instanceof EvGraphNode) {
                ActionIntrospector.getAction(this, "edit").setEnabled(true);
                egCount++;
            }
        }
        if (egCount == 0) {
            ActionIntrospector.getAction(this, "edit").setEnabled(false);
        }
    }
    private Vector copyVector = new Vector();

    /**
     * 
     */
    public void edit() {
        if (selectionVector.size() <= 0) {
            return;
        }
        copyVector = (Vector) selectionVector.clone();
        for (Object o : copyVector) {
            if (!(o instanceof EvGraphNode)) {
                JOptionPane.showMessageDialog(null, "Please select an Event Graph");
                return;
            }
            String className = ((EvGraphNode) o).getType();
            File f = null;
            try {
                f = FileBasedClassManager.instance().getFile(className);
            } catch (Exception e) {
                if (viskit.Vstatics.debug) {
                    e.printStackTrace();
                }
            }
            if (f == null) {
                JOptionPane.showMessageDialog(null,
                        "Please select an XML Event Graph to load to EG Editor tab");
                return;
            }
            // _doOpen checks if a tab is already opened
            VGlobals.instance().getEventGraphEditor().controller._doOpen(f);
        }
    }

    /**
     * 
     */
    public void copy() {
        if (selectionVector.size() <= 0) {
            return;
        }
        copyVector = (Vector) selectionVector.clone();
        ActionIntrospector.getAction(this, "paste").setEnabled(true);
    }
    int copyCount = 0;

    /**
     * 
     */
    public void paste() //-----------------
    {
        if (copyVector.size() <= 0) {
            return;
        }
        int x = 100,
                y = 100;
        int n = 0;
        // We only paste un-attached nodes (at first)
        for (Iterator itr = copyVector.iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (o instanceof AssemblyEdge) {
                continue;
            }
            if (o instanceof EvGraphNode) {
                String nm = ((EvGraphNode) o).getName();
                String typ = ((EvGraphNode) o).getType();
                ((ViskitAssemblyModel) getModel()).newEventGraph(nm + "-copy" + copyCount++, typ, new Point(x + (20 * n), y + (20 * n)));
            } else if (o instanceof PropChangeListenerNode) {
                String nm = ((PropChangeListenerNode) o).getName();
                String typ = ((PropChangeListenerNode) o).getType();
                ((ViskitAssemblyModel) getModel()).newPropChangeListener(nm + "-copy" + copyCount++, typ, new Point(x + (20 * n), y + (20 * n)));
            }
            n++;
        }
    }

    public void cut() {
        if (selectionVector != null && selectionVector.size() > 0) {
            // first ask:
            String msg = "";
            int nodeCount = 0;  // different msg for edge delete
            for (Object o : selectionVector) {
                if (o instanceof EvGraphNode || o instanceof PropChangeListenerNode) {
                    nodeCount++;
                }
                String s = o.toString();
                s = s.replace('\n', ' ');
                msg += ", \n" + s;
            }
            String specialNodeMsg = (nodeCount > 0) ? "\n(All unselected but attached edges will also be deleted.)" : "";
            if (((ViskitAssemblyView) getView()).genericAsk("Delete element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg) == JOptionPane.YES_OPTION) {
                // do edges first?
                delete();
            }
        }
    }

    public void delete() {
        Vector localV = (Vector) selectionVector.clone();   // avoid concurrent update
        for (Object elem : localV) {
            if (elem instanceof AssemblyEdge) {
                killEdge((AssemblyEdge) elem);
            } else if (elem instanceof EvGraphNode) {
                EvGraphNode en = (EvGraphNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    killEdge(ed);
                }
                ((ViskitAssemblyModel) getModel()).deleteEvGraphNode(en);
            } else if (elem instanceof PropChangeListenerNode) {
                PropChangeListenerNode en = (PropChangeListenerNode) elem;
                for (AssemblyEdge ed : en.getConnections()) {
                    killEdge(ed);
                }
                ((ViskitAssemblyModel) getModel()).deletePCLNode(en);
            }
        }
    }

    private void killEdge(AssemblyEdge e) {
        if (e instanceof AdapterEdge) {
            ((ViskitAssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
        } else if (e instanceof PropChangeEdge) {
            ((ViskitAssemblyModel) getModel()).deletePropChangeEdge((PropChangeEdge) e);
        } else if (e instanceof SimEvListenerEdge) {
            ((ViskitAssemblyModel) getModel()).deleteSimEvLisEdge((SimEvListenerEdge) e);
        }
    }

    /* a component, e.g., model, wants to say something. */
    /**
     * 
     * @param typ
     * @param msg
     */
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
        ((ViskitAssemblyView) getView()).genericErrorReport(title, msg);
    }

    /********************************/
    /* from menu:*/
    public void showXML() {
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || vmod.getLastFile() == null) {
            return;
        }

        ((ViskitAssemblyView) getView()).displayXML(vmod.getLastFile());
    }

    private boolean checkSaveForSourceCompile() {        
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        if (vmod.isDirty() || vmod.getLastFile() == null) {
            int ret = ((ViskitAssemblyView) getView()).genericAskYN("Confirm", "The model will be saved.\nContinue?");
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            this.saveAs();
        }
        return true;
    }

    public void generateJavaSource() {
        String source = produceJavaAssemblyClass();
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        if (source != null && source.length() > 0) {
            String className = vmod.getMetaData().packageName + "." + vmod.getMetaData().name;
            ((ViskitAssemblyView) getView()).showAndSaveSource(className, source);
        }
    }

    private String produceJavaAssemblyClass() {
        
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        if (!checkSaveForSourceCompile() || vmod.getLastFile() == null) {
            return null;
        }
        return buildJavaAssemblySource(vmod.getLastFile());
    }
 
    /**
     * Build the actual source code from the Assembly XML
     * @param f the Assembly file to produce source from
     * @return a string of Assembly source code
     */
    public String buildJavaAssemblySource(File f) {
        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xvt = new XMLValidationTool(f, new File(XMLValidationTool.LOCAL_ASSEMBLY_SCHEMA));
        
        if ((xvt == null) || !xvt.isValidXML()) {

            // TODO: implement a Dialog pointing to the validationErrors.log
            return null;
        } else {
            log.info(f + " is valid XML\n");
        }

        SimkitAssemblyXML2Java x2j = null;
        try {
            x2j = new SimkitAssemblyXML2Java(f);
            x2j.unmarshal();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return x2j.translate();
    }

    // NOTE: above are routines to operate on current assembly
   
    /**
     * Build the actual source code from the Event Graph XML
     * @param x2j the Event Graph initialized translator to produce source with
     * @return a string of Event Graph source code
     */
    public String buildJavaEventGraphSource(SimkitXML2Java x2j) {
        String eventGraphSource = null;
        
        // Must validate XML first and handle any errors before compiling
        XMLValidationTool xvt = new XMLValidationTool(x2j.getEventGraphFile(), 
                new File(XMLValidationTool.LOCAL_EVENT_GRAPH_SCHEMA));

        if ((xvt == null) || !xvt.isValidXML()) {

            // TODO: implement a Dialog pointing to the validationErrors.log
            return null;
        } else {
            log.info(x2j.getEventGraphFile() + " is valid XML\n");
        }

        try {
            eventGraphSource = x2j.translate();
        } catch (Exception e) {
            log.error("Error building Java from " + x2j.getFileBaseName() + 
                    ": " + e.getMessage() + ", erroneous event-graph xml found");
        }
        return eventGraphSource;
    }   
    
    /** Create and test compile our EventGraphs and Assemblies from XML
     * Know complilation path
     * @param src the translated source either from SimkitXML2Java, or SimkitAssemblyXML2Java
     * @return a reference to *.class files of our compiled sources
     */
    public File compileJavaClassFromString(String src) {
        String baseName = null;

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
        fnd = mat.find();

        int end = mat.end();
        String s = src.substring(end, src.length()).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];

        try {

            // Should always have a live ViskitProject
            ViskitProject viskitProj = VGlobals.instance().getCurrentViskitProject();
            File workingDir = VGlobals.instance().getWorkDirectory().getParentFile();

            // Create, or find the project's java source
            File srcPkg = new File(viskitProj.getSrcDir(), pkg);
            if (!srcPkg.isDirectory()) {
                srcPkg.mkdirs();
            }
            File javaFile = new File(srcPkg, baseName + ".java");
            javaFile.createNewFile();

            FileWriter fw = new FileWriter(javaFile);
            fw.write(src);
            fw.flush();
            fw.close();

            // Now create the build/classes directory to place bytecode
            File classesDir = (viskitProj != null) ? viskitProj.getClassDir() : new File(workingDir, ViskitProject.CLASSES_DIRECTORY_NAME);
            if (!classesDir.isDirectory()) {
                classesDir.mkdirs();
            }

            log.info("Test compiling " + javaFile.getCanonicalPath());

            String diagnostic = Compiler.invoke(pkg, baseName, src);
            if (diagnostic != null) {
                if (diagnostic.isEmpty()) {
                    log.info("No compile errors\n");
                    return new File(classesDir, packagePath + baseName + ".class");
                } else {
                    log.info(diagnostic);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Known path for EventGraph compilation
     * @param xmlFile the EventGraph to package up
     * @return a package and file pair
     */
    public PkgAndFile createTemporaryEventGraphClass(File xmlFile) {
        try {
            SimkitXML2Java x2j = new SimkitXML2Java(xmlFile);
            x2j.unmarshal();

            boolean isEventGraph = x2j.getUnMarshalledObject() instanceof viskit.xsd.bindings.eventgraph.SimEntity;
            if (!isEventGraph) {
                log.debug("Is an Assembly: " + !isEventGraph);
                return null;
            }

            String src = buildJavaEventGraphSource(x2j);

            // If using plain Vanilla Viskit, don't compile diskit extended EGs
            // as diskit.jar won't be available
            if (src.contains("diskit") && !new File("lib/ext/diskit.jar").exists()) {
                FileBasedClassManager.instance().addCacheMiss(xmlFile);
                return null;
            }
            PkgAndFile paf = compileJavaClassAndSetPackage(src);
            FileBasedClassManager.instance().addCache(xmlFile, paf.f);
            return paf;
        } catch (Exception e) {
            log.error("Error creating Java class file from " + xmlFile + ": " + e.getMessage() + "\n");
            FileBasedClassManager.instance().addCacheMiss(xmlFile);
        }
        return null;
    }

    /** Path for EG and Assy compilation
     * @param source the raw source to write to file
     * @return a package and file pair
     */
    public PkgAndFile compileJavaClassAndSetPackage(String source) {
        String pkg = null;
        if (source != null && source.length() > 0) {
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
    /**
     * 
     */
    public void export2grid() {
        ViskitAssemblyModel model = (ViskitAssemblyModel) getModel();
        File tFile = null;
        try {
            tFile = TempFileManager.createTempFile("ViskitAssy", ".xml");
        } catch (IOException e) {
            ((ViskitAssemblyView) getView()).genericErrorReport("File System Error",
                    "Error creating temporary file.");
            return;
        }
        model.saveModel(tFile);
    //todo switch to DOE
    }
    private String[] execStrings;

    /** Known path for Assy compilation */
    public void initAssemblyRun() {
        String src = produceJavaAssemblyClass(); // asks to save

        PkgAndFile paf = compileJavaClassAndSetPackage(src);
        if (paf != null) {
            File f = paf.f;
            String clNam = f.getName().substring(0, f.getName().indexOf('.'));
            clNam = paf.pkg + "." + clNam;

            String classPath = getClassPathString();

            execStrings = buildExecStrings(clNam, classPath);            
        } else {
            execStrings = null;
        }
    }

    static String getClassPathString() {
        VGlobals.instance().resetWorkClassLoader();
        return Vstatics.getClassPathAsString();
    }

    /** Compile the Assembly and prepare the Simulation Runner for external JVM */
    public void compileAssemblyAndPrepSimRunner() {        
        initAssemblyRun();
        if (execStrings == null) {
            JOptionPane.showMessageDialog(null, "Compile not attempted, check log for details",
                    "Assembly Source Generation Error",
                    JOptionPane.WARNING_MESSAGE); //todo, more information
        } else {

            // Ensure changes to the Assembly Properties dialog get saved
            save();
        
            // Ensure a cleared Assembly Run panel upon every Assembly compile
            RunnerPanel2 rp2 = VGlobals.instance().getRunPanel();
            rp2.soutTA.setText(null);
            rp2.soutTA.setText("Assembly output stream:" + rp2.lineEnd +
                    "----------------------" + rp2.lineEnd);
        
            runner.exec(execStrings);
            runTabbedPane.setEnabledAt(this.runTabbedPaneIdx, true);
        }
    }

    /**
     * 
     */
    public static final int EXEC_JAVACMD = 0;
    /**
     * 
     */
    public static final int EXEC_VMARG0 = 1;
    /**
     * 
     */
    public static final int EXEC_VMARG1 = 2;
    /**
     * 
     */
    public static final int EXEC_VMARG3 = 3;
    /**
     * 
     */
    public static final int EXEC_DASH_CP = 4;
    /**
     * 
     */
    public static final int EXEC_CLASSPATH = 5;
    /**
     * 
     */
    public static final int EXEC_TARGET_CLASS_NAME = 6;
    /**
     * 
     */
    public static final int EXEC_VERBOSE_SWITCH = 7;
    /**
     * 
     */
    public static final int EXEC_STOPTIME_SWITCH = 8;
    /**
     * 
     */
    public static final int EXEC_FIRST_ENTITY_NAME = 9;    
    
    /** Prepare for the compilation of the loaded assembly file from java source
     * Maintain the above statics to match the order below.
     * @param className the name of the Assembly file to compile
     * @param classPath the current ClassLoader context
     * @return os exec array
     */
    private String[] buildExecStrings(String className, String classPath) {
        Vector<String> v = new Vector<String>();
        String fsep = Vstatics.getFileSeparator();

        StringBuffer sb = new StringBuffer();
        sb.append(System.getProperty("java.home"));
        sb.append(fsep);
        sb.append("bin");
        sb.append(fsep);
        sb.append("java");
        v.add(sb.toString());        // 0

        v.add("-Xss5M");             // 1
        v.add("-Xincgc");            // 2
        v.add("-Xmx512M");           // 3
        v.add("-cp");                // 4
        v.add(classPath);            // 5
        v.add(className);            // 6

        v.add("" + ((ViskitAssemblyModel) getModel()).getMetaData().verbose); // 7
        v.add(((ViskitAssemblyModel) getModel()).getMetaData().stopTime);     // 8

        Vector<String> vec = ((ViskitAssemblyModel) getModel()).getDetailedOutputEntityNames();
        for (String s : vec) {
            v.add(s);                                                         // 9+
        }

        String[] ra = new String[v.size()];
        return v.toArray(ra);
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    /** Screen capture a snapshot of the Assembly View Frame */
    public void captureWindow() {
        
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        String fileName = "AssemblyScreenCapture";
        if (vmod.getLastFile() != null) {
            fileName = vmod.getLastFile().getName();
        }

        File fil = ((ViskitAssemblyView) getView()).saveFileAsk(fileName + imgSaveCount + ".png", true);
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
     * @param assyImagePath an image path to write the .png
     */
    public void captureAssemblyImage(String assyImagePath) {
        final Timer tim = new Timer(100, new timerCallback(new File(assyImagePath), false));
        tim.setRepeats(false);
        tim.start();
    }

    public

    boolean isCloseAll() {
        return closeAll;
    }

    public void setCloseAll(boolean closeAll) {
        this.closeAll = closeAll;
    }

    class timerCallback implements ActionListener {

        File fil;
        boolean display;

        timerCallback(File f, boolean b) {
            fil = f;
            display = b;
        }

        public void actionPerformed(ActionEvent ev) {

            // create and save the image
            AssemblyViewFrame avf = (AssemblyViewFrame) getView();

            // Get only the jgraph part
            Component component = avf.getCurrentJgraphComponent();
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
                System.out.println("AssemblyController Exception in capturing screen: " + e.getMessage());
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
    
    /** The default version of this.  Run assembly in external VM. */
    AssemblyRunnerPlug runner = new AssemblyRunnerPlug() {

        public void exec(String[] execStrings) {
            try {
                Runtime.getRuntime().exec(execStrings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    void setAssemblyRunner(AssemblyRunnerPlug plug) {
        runner = plug;
    }    
    
    // Recent open file support:
    private static final int RECENTLISTSIZE = 15;
    private ArrayList<String> recentFileList = new ArrayList<String>(RECENTLISTSIZE + 1);

    /** TODO: This may be deprecated now due to below new method */
    public void openRecent() {

        ArrayList<String> v = getRecentFileList(true); // have a settings panel now...false);
        if (v.size() <= 0) {
            open();
        } else {
            File file = ((ViskitAssemblyView) getView()).openRecentFilesAsk(v);
            if (file != null) {
                _doOpen(file);
            }
        }

        // v might have been changed
        setRecentFileList(v);
    }

    public void openRecentAssembly(String path) {
        _doOpen(new File(path));
    }

    /** Opens each EG associated with this Assembly
     * @param f the Assembly File to open EventGraphs for
     */
    private void openEventGraphs(File f) {

        try {

            EventGraphCache.instance().makeEntityTable(f.getAbsolutePath());
            for (File filePath : EventGraphCache.instance().getEventGraphFiles()) {

                // _doOpen checks if a tab is already opened
                VGlobals.instance().getEventGraphEditor().controller._doOpen(filePath);
            }
        } catch (Exception ex) {
            log.error(ex);
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
        openV = new ArrayList<String>(4);
        if (getHistoryConfig() == null) {
            return;
        }
        String[] valueAr = getHistoryConfig().getStringArray(ViskitConfig.ASSY_HISTORY_KEY + "[@value]");
        log.debug("_setFileLists() valueAr size is: " + valueAr.length);
        for (int i = 0; i < valueAr.length; i++) {

            // Attempt to prevent dupicate entries
            if (recentFileList.contains(valueAr[i])) {
                continue;
            }
            recentFileList.add(valueAr[i]);
            String op = getHistoryConfig().getString(ViskitConfig.ASSY_HISTORY_KEY + "(" + i + ")[@open]");

            if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes"))) {
                openV.add(valueAr[i]);
            }
        }
        notifyRecentFileListeners();
    }

    private void saveHistoryXML(ArrayList<String> recentFiles) {
        getHistoryConfig().clearTree(ViskitConfig.RECENT_ASSY_CLEAR_KEY);

        for (int i = 0; i < recentFiles.size(); i++) {
            String value = recentFiles.get(i);
            getHistoryConfig().setProperty(ViskitConfig.ASSY_HISTORY_KEY + "(" + i + ")[@value]", value);
        }
        getHistoryConfig().getDocument().normalize();
    }

    private void setRecentFileList(ArrayList<String> lis) {
        saveHistoryXML(lis);
    }

    private void markConfigAllClosed() {
        for (int i = 0; i < recentFileList.size(); i++) {
            historyConfig.setProperty(ViskitConfig.ASSY_HISTORY_KEY + "(" + i + ")[@open]", "false");
        }
    }
    
    private void markConfigOpen(File f) {
        int idx = recentFileList.indexOf(f.getAbsolutePath());
        if (idx != -1) {
            getHistoryConfig().setProperty(ViskitConfig.ASSY_HISTORY_KEY + "(" + idx + ")[@open]", "true");
        }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    }

    public void clearRecentFileList() {
        recentFileList.clear();
        saveHistoryXML(recentFileList);
        notifyRecentFileListeners();
    }

    public java.util.List<String> getRecentFileList() // implement interface
    {
        return getRecentFileList(false);
    }

    private ArrayList<String> getRecentFileList(boolean refresh) {
        if (refresh || recentFileList == null) {
            _setFileLists();
        }
        return recentFileList;
    }
    XMLConfiguration historyConfig;

    private void initConfig() {
        try {
            historyConfig = ViskitConfig.instance().getViskitConfig();
        } catch (Exception e) {
            System.out.println("Error loading history file: " + e.getMessage());
            System.out.println("Recent file saving disabled");
            historyConfig = null;
        }
    }

    private XMLConfiguration getHistoryConfig() {
        return historyConfig;
    }
}

class PkgAndFile {

    String pkg;
    File f;

    PkgAndFile(String pkg, File f) {
        this.pkg = pkg;
        this.f = f;
    }
}

interface AssemblyRunnerPlug {

    public void exec(String[] execStrings);
}
