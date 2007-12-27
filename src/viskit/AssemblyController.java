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
import edu.nps.util.FileIO;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgraph.graph.DefaultGraphCell;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;
import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.translator.SimkitXML2Java;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:26:02 AM
 * @version $Id: AssemblyController.java 1669 2007-12-19 20:27:14Z tdnorbra $
 */
public class AssemblyController extends mvcAbstractController implements ViskitAssemblyController, OpenAssembly.AssyChangeListener {
    
    public static AssemblyController inst;
    static Logger log = Logger.getLogger(AssemblyController.class);
    Class<?> simEvSrcClass, simEvLisClass, propChgSrcClass, propChgLisClass;
    private String initialFile;

    public AssemblyController() {
        initConfig();
        //initFileWatch();
        inst = this;
    }

    public static AssemblyController instance() {
        return inst;
    }

    public void setInitialFile(String fil) {
        if (viskit.Vstatics.debug) {
            System.out.println("Initial file set: " + fil);
        }
        initialFile = fil;
    }

    public void runAssembly(String initialFile) {
        if (viskit.Vstatics.debug) {
            System.out.println("Running assembly: " + initialFile);
        }
        File f = new File(initialFile); 
        _doOpen(f);
        runAssembly();
    }
    
    public void begin() {
        
        File f;
        if (initialFile != null) {
            if (viskit.Vstatics.debug) {
                System.out.println("Loading initial file: " + initialFile);
            }
            f = new File(initialFile);
            _doOpen(f);
            runAssembly();
        } else {
            ArrayList<String> lis = getOpenFileList(false);
            log.debug("Inside begin() and lis.size() is: " + lis.size());
            
            if (lis.size() > 0) {                
                
                // TODO: should only be one, so why is the open file list of size 15?
                f = new File(lis.get(0));
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

    public void runEventGraphEditor() {
        if (VGlobals.instance().getEventGraphEditor() == null) {
            VGlobals.instance().buildEventGraphViewFrame();
        }
        VGlobals.instance().runEventGraphView();
    }

    public boolean preQuit() {
        if (lastFile != null) {
            markConfigOpen(lastFile);
        }

        //    if (((AssemblyModel)getModel()).isDirty())
//      return askToSaveAndContinue();
//    return true;
        return checkSaveIfDirty();
    }

    public void postQuit() {
        VGlobals.instance().quitAssemblyEditor();
    }

    public void quit() //----------------
    {
        if (preQuit()) {
            postQuit();
        }
    }

    /** Information required by the EventGraphController to see if an Assembly
     * file is already open.  Also checked internally by this class.
     * @param refresh flag to refresh the list from .viskit_history.xml
     * @return a final (unmodifiable) reference to the current Assembly open list
     */
    public final ArrayList<String> getOpenFileList(boolean refresh) {
        if (refresh || openV == null) {
            _setFileLists();
        }
        return openV;
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

            //int ynx = (((ViskitAssemblyView) getView()).genericAskYN("Question", sb.toString()));
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

        if (((AssemblyModel) getModel()).isDirty()) {
            return askToSaveAndContinue();
        }
        return true;  // proceed
    }

    public void settings() {
        // placeholder for combo gui
    }
    
    public void open() {
        
        if (!checkSaveIfDirty()) {
            return;
        }

        File file = ((ViskitAssemblyView) getView()).openFileAsk();
        if (file != null) {
            _doOpen(file);
        }
    }

    File lastFile;
    private void _doOpen(File f) {
        if (!f.exists()) {
            return;
        }

        lastFile = f;

        ViskitAssemblyModel mod = (ViskitAssemblyModel) getModel();
        boolean goodOpen = mod.newModel(lastFile);
        if (goodOpen) {
            mod = (ViskitAssemblyModel) getModel();
            GraphMetaData gmd = mod.getMetaData();
            ((ViskitAssemblyView) getView()).fileName(gmd.name); //lastFile.getName());
            adjustRecentList(lastFile);

            // replaced by below fileWatchOpen(lastFile);
            initOpenAssyWatch(lastFile, mod.getJaxbRoot());
        }
        
        openEventGraphs(lastFile);
    }

    private void initOpenAssyWatch(File f, SimkitAssembly jaxbroot) {
        try {
            OpenAssembly.inst().setFile(f, jaxbroot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    public String getHandle() {
        return assyChgListener.getHandle();
    }

    public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param) {
        assyChgListener.assyChanged(action, source, param);
    }
    /////////////////////////////////////////////////////////////////////////////////////
  // Methods to implement a scheme where other modules will be informed of file changes //
  // (Would Java Beans do this with more or less effort?

    private File watchDir;
    private void initFileWatch() {
        try {
            watchDir = File.createTempFile("assy", "current");   // actually creates
            String p = watchDir.getAbsolutePath();   // just want the name part of it
            watchDir.delete();        // Don't want the file to be made yet
            watchDir = new File(p);
            watchDir.mkdir();
            watchDir.deleteOnExit();

            DirectoryWatch dirWatch = new DirectoryWatch(watchDir);
            dirWatch.setLoopSleepTime(1 * 1000); // 1 secs
            dirWatch.startWatcher();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fileWatchSave(File f) {
        fileWatchOpen(f);
    }

    private void fileWatchOpen(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        try {
            FileIO.copyFile(f, ofile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fileWatchClose(File f) {
        String nm = f.getName();
        File ofile = new File(watchDir, nm);
        ofile.delete();
    }

    public void addAssemblyFileListener(OpenAssembly.AssyChangeListener lis) //DirectoryWatch.DirectoryChangeListener lis)
    {
        //dirWatch.addListener(lis);
        OpenAssembly.inst().addListener(lis);
    }

    public void removeAssemblyFileListener(OpenAssembly.AssyChangeListener lis) //DirectoryWatch.DirectoryChangeListener lis)
    {
        OpenAssembly.inst().removeListener(lis);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /** Here we are informed of open Event Graphs */
    DirectoryWatch.DirectoryChangeListener egListener = new DirectoryWatch.DirectoryChangeListener() {

        public void fileChanged(File file, int action, DirectoryWatch source) {
            ;
        }
    };

    public DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener() {
        return egListener;
    }

    public void save() //----------------
    {
        if (lastFile == null) {
            saveAs();
        } else {
            updateGMD();
            ((ViskitAssemblyModel) getModel()).saveModel(lastFile);
            fileWatchSave(lastFile);
        }
    }

    public void saveAs() {
        ViskitAssemblyModel model = (ViskitAssemblyModel) getModel();
        ViskitAssemblyView view = (ViskitAssemblyView) getView();
        GraphMetaData gmd = model.getMetaData();

        File saveFile = view.saveFileAsk(gmd.name + ".xml", false);

        if (saveFile != null) {
            if (lastFile != null) {
                fileWatchClose(lastFile);
            }
            lastFile = saveFile;

            String n = lastFile.getName();
            if (n.endsWith(".xml") || n.endsWith(".XML")) {
                n = n.substring(0, n.length() - 4);
            }
            gmd.name = n;

            updateGMD();
            model.saveModel(lastFile);
            view.fileName(lastFile.getName());

            fileWatchOpen(lastFile);
            adjustRecentList(saveFile);
        }
    }

    private void updateGMD() {
        GraphMetaData gmd = ((ViskitAssemblyModel) getModel()).getMetaData();
        //gmd.stopTime = ((ViskitAssemblyView)getView()).getStopTime();
    //gmd.verbose = ((ViskitAssemblyView)getView()).getVerbose();
        ((ViskitAssemblyModel) getModel()).changeMetaData(gmd);

    }

    public void newAssembly() {
        if (!checkSaveIfDirty()) {
            return;
        }

        if (lastFile != null) {
            fileWatchClose(lastFile);
            lastFile = null;
        }  
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        vmod.newModel(null);
        editGraphMetaData();
        ((ViskitAssemblyView) getView()).fileName(vmod.getMetaData().name);
    }
    
    public void close() {
        if (preClose()) {postClose();}
    }

    public boolean preClose() {
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();
        if (vmod.isDirty()) {
            if (!askToSaveAndContinue()) {return false;}
        }
        return true;
    }

    public void postClose() {        
        ViskitAssemblyModel vmod = (ViskitAssemblyModel) getModel();        
        vmod.newModel(null);
        if (lastFile != null) {
            fileWatchClose(lastFile);
            lastFile = null;
        }
        
        // Close any currently open EGs
        VGlobals.instance().getEventGraphEditor().controller.closeAll();
        ((ViskitAssemblyView) getView()).fileName("");
    }
    
    private int egNodeCount = 0;

    private String shortEgName(String typeName) {
        String shortname = "evgr_";
        if (typeName.lastIndexOf('.') != -1) {
            shortname = typeName.substring(typeName.lastIndexOf('.') + 1) + "_";
        }
        return shortname + egNodeCount++;
    }
    private Point nextPoint = new Point(25, 25);

    private Point getNextPoint() {
        nextPoint.x = nextPoint.x >= 200 ? 25 : nextPoint.x + 25;
        nextPoint.y = nextPoint.y >= 200 ? 25 : nextPoint.y + 25;
        return nextPoint;
    }

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

    public void newEventGraphNode(String typeName, Point p) {
        String shName = shortEgName(typeName);
        ((viskit.model.AssemblyModel) getModel()).newEventGraph(shName, typeName, p);
    }

    public void newFileBasedEventGraphNode(FileBasedAssyNode xnode, Point p) {
        String shName = shortEgName(xnode.loadedClass);
        ((viskit.model.ViskitAssemblyModel) getModel()).newEventGraphFromXML(shName, xnode, p);
    }

    private String shortPCLName(String typeName) {
        String shortname = "lstnr_";
        if (typeName.lastIndexOf('.') != -1) {
            shortname = typeName.substring(typeName.lastIndexOf('.') + 1) + "_";
        }

        return shortname + egNodeCount++; // use same counter
    }

    private String shortAdapterName(String typeName) {
        String shortname = "adptr_";
        if (typeName.lastIndexOf('.') != -1) {
            shortname = typeName.substring(typeName.lastIndexOf('.') + 1) + "_";
        }
        return shortname + egNodeCount++; // use same counter
    }

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

    public void newPropChangeListenerNode(String name, Point p) {
        String shName = shortPCLName(name);
        ((viskit.model.AssemblyModel) getModel()).newPropChangeListener(shName, name, p);
    }

    public void newFileBasedPropChangeListenerNode(FileBasedAssyNode xnode, Point p) {
        String shName = shortPCLName(xnode.loadedClass);
        ((viskit.model.AssemblyModel) getModel()).newPropChangeListenerFromXML(shName, xnode, p);

    }

    /**
     *
     * @return true = continue, false = don't (i.e., we cancelled)
     */
    private boolean askToSaveAndContinue() {
        int yn = (((ViskitAssemblyView) getView()).genericAsk("Question", "Save modified assembly?"));

        switch (yn) {
            case JOptionPane.YES_OPTION:
                save();
                if (((AssemblyModel) getModel()).isDirty()) {
                    return false;
                } // we cancelled
         // else
                return true;
            //break;
            case JOptionPane.NO_OPTION:
                return true;
            //break;
            case JOptionPane.CANCEL_OPTION:
            default:
                return false;
        }
    }

    public void editGraphMetaData() //--------------------------
    {
        GraphMetaData gmd = ((ViskitAssemblyModel) getModel()).getMetaData();
        boolean modified = AssemblyMetaDataDialog.showDialog(VGlobals.instance().getMainAppWindow(),
                VGlobals.instance().getMainAppWindow(), gmd);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeMetaData(gmd);
        }
    }

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

    /*
    if(!(oA instanceof EvGraphNode) || !(oB instanceof EvGraphNode))
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection", "Both nodes must be instances of Event Graphs.");
    else {
      AdapterEdge ae = ((ViskitAssemblyModel)getModel()).newAdapterEdge((EvGraphNode)oA,(EvGraphNode)oB);
      // edit right away
      if(ae != null)     // shouldn't happen
        adapterEdgeEdit(ae);
    }
*/
    }

    public void newSimEvListArc(Object[] nodes) {
        AssemblyNode oA = (AssemblyNode) ((DefaultGraphCell) nodes[0]).getUserObject();
        AssemblyNode oB = (AssemblyNode) ((DefaultGraphCell) nodes[1]).getUserObject();

        AssemblyNode[] oArr = checkLegalForSEListenerArc(oA, oB);

        if (oArr == null) {
            ((ViskitAssemblyView) getView()).genericErrorReport("Incompatible connection", "The nodes must be a SimEventListener and SimEventSource combination.");
            return;
        }
        ((ViskitAssemblyModel) getModel()).newSimEvLisEdge(oArr[0], oArr[1]);

    /*
    if(!(oA instanceof EvGraphNode) || !(oB instanceof EvGraphNode))
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection", "Both nodes must be instances of Event Graphs.");
    else
    ((ViskitAssemblyModel)getModel()).newSimEvLisEdge((EvGraphNode)oA,(EvGraphNode)oB);
*/
    }

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

    /*
    PropChangeEdge pce = null;
    if(oA instanceof PropChangeListenerNode && !(oB instanceof PropChangeListenerNode)) {
      pce = ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oB,(PropChangeListenerNode)oA);
    }
    else if(oB instanceof PropChangeListenerNode && !(oA instanceof PropChangeListenerNode)) {
      pce = ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oA,(PropChangeListenerNode)oB);
    }
    else
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","One of the two nodes must be an instance of a PropertyChangeListener.");
    // edit right away
    if(pce != null)
      pcListenerEdgeEdit(pce);
*/
    }

    AssemblyNode[] checkLegalForSEListenerArc(AssemblyNode a, AssemblyNode b) {
        Class ca = findClass(a);
        Class cb = findClass(b);
        return orderSELSrcAndLis(a, b, ca, cb);
    }

    AssemblyNode[] checkLegalForPropChangeArc(AssemblyNode a, AssemblyNode b) {
        Class ca = findClass(a);
        Class cb = findClass(b);
        return orderPCLSrcAndLis(a, b, ca, cb);
    }

    Class findClass(AssemblyNode o) {
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

    public void pcListenerEdit(PropChangeListenerNode pclNode) //---------------------------------------
    {
        boolean done;
        do {
            done = true;
            boolean modified = ((ViskitAssemblyView) getView()).doEditPclNode(pclNode);
            if (modified) {
                done = ((ViskitAssemblyModel) getModel()).changePclNode(pclNode);
            }
        } while (!done);
    }

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

    public void pcListenerEdgeEdit(PropChangeEdge pclEdge) {
        boolean modified = ((ViskitAssemblyView) getView()).doEditPclEdge(pclEdge);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changePclEdge(pclEdge);
        }
    }

    public void adapterEdgeEdit(AdapterEdge aEdge) {
        boolean modified = ((ViskitAssemblyView) getView()).doEditAdapterEdge(aEdge);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeAdapterEdge(aEdge);
        }
    }

    public void simEvListenerEdgeEdit(SimEvListenerEdge seEdge) {
        boolean modified = ((ViskitAssemblyView) getView()).doEditSimEvListEdge(seEdge);
        if (modified) {
            ((ViskitAssemblyModel) getModel()).changeSimEvEdge(seEdge);
        }
    }
    private Vector selectionVector = new Vector();

    public void selectNodeOrEdge(Vector v) //------------------------------------
    {
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
                f = FileBasedClassManager.inst().getFile(className);
            } catch (Exception e) {
                if (viskit.Vstatics.debug) {
                    e.printStackTrace();
                }
            }
            if (f == null) {
                JOptionPane.showMessageDialog(null, "Please select an XML Event Graph to load to EG Editor tab");
                return;
            }
            // _doOpen checks if a tab is already opened
            VGlobals.instance().getEventGraphEditor().controller._doOpen(f);
        }
    }

    public void copy() //----------------
    {
        if (selectionVector.size() <= 0) {
            return;
        }
        copyVector = (Vector) selectionVector.clone();
        ActionIntrospector.getAction(this, "paste").setEnabled(true);
    }
    int copyCount = 0;

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

    public void cut() //---------------
    {
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
            String specialNodeMsg = (nodeCount > 0 ? "\n(All unselected but attached edges will also be deleted.)" : "");
            if (((ViskitAssemblyView) getView()).genericAsk("Delete element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg) == JOptionPane.YES_OPTION) {
                // do edges first?
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
        if (!checkSaveForSourceCompile() || lastFile == null) {
            return;
        }

        ((ViskitAssemblyView) getView()).displayXML(lastFile);
    }

    private boolean checkSaveForSourceCompile() {
        if (((ViskitAssemblyModel) getModel()).isDirty() || lastFile == null) {
            int ret = JOptionPane.showConfirmDialog(null, "The model will be saved.\nContinue?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ret != JOptionPane.YES_OPTION) {
                return false;
            }
            this.saveAs();
        }
        return true;
    }

    public void generateJavaSource() {
        String source = produceJavaClass();
        if (source != null && source.length() > 0) {
            String className = ((ViskitAssemblyModel) getModel()).getMetaData().packageName + "." + ((ViskitAssemblyModel) getModel()).getMetaData().name;
            ((ViskitAssemblyView) getView()).showAndSaveSource(className, source);
        }
    }

    private String produceJavaClass() {
        if (!checkSaveForSourceCompile() || lastFile == null) {
            return null;
        }

        return buildJavaAssemblySource(((ViskitAssemblyModel) getModel()).getFile());
    }

    // above are routines to operate on current assembly

    public static String buildJavaAssemblySource(File f) {
        try {
            SimkitAssemblyXML2Java x2j = new SimkitAssemblyXML2Java(f);
            x2j.unmarshal();
            return x2j.translate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String buildJavaEventGraphSource(File f) {
        try {
            SimkitXML2Java x2j = new SimkitXML2Java(f);
            x2j.unmarshal();
            return x2j.translate();
        } catch (Exception e) {
            if (viskit.Vstatics.debug) {
                e.printStackTrace();
            }
            System.err.println("Error building Java from " + f.getName() + ": " + e.getMessage() + ", erroneous event-graph xml found");
        }
        return null;
    }

    public static File compileJavaClassFromStringAndHandleDependencies(String src) {
        handleFileBasedClasses();
        return compileJavaClassFromString(src);
    }

    public static int compileJavaFromStringAndHandleDependencies(String src) {
        handleFileBasedClasses();
        return compileJavaFromString(src);
    }

    public static File compileJavaClassFromString(String src) {
        return compileJavaClassFromString(src, false);
    }

    public static int compileJavaFromString(String src) {
        File f = makeFile(src);
        if (f == null) {
            return -1;
        }
        String cp = getCustomClassPath();
        String canPath;
        try {
            canPath = f.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        //    return com.sun.tools.javac.Main.compile(new String[]{"-Xlint:unchecked", "-Xlint:deprecation", "-verbose", "-classpath",cp,"-d", f.getParent(), canPath});
        return com.sun.tools.javac.Main.compile(new String[]{"-Xlint:unchecked", "-Xlint:deprecation", "-classpath", cp, "-d", f.getParent(), canPath});
    }

    private static File makeFile(String src) {
        String baseName = null;

        // Find the package subdirectory
        Pattern pat = Pattern.compile("package.+;");
        Matcher mat = pat.matcher(src);
        boolean fnd = mat.find();

        if (fnd) {
            int st = mat.start();
            int end = mat.end();
            String s = src.substring(st, end);
            s = s.replace(';', '/');
            String[] sa = s.split("\\s");
            sa[1] = sa[1].replace('.', '/');
        }
        // done finding the package subdir (just to mark the file as "deleteOnExit")

        pat = Pattern.compile("public\\s+class\\s+");
        mat = pat.matcher(src);
        fnd = mat.find();
        // if(fnd) {
        int end = mat.end();
        String s = src.substring(end, end + 128).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];
        // }
        try {
            File f = VGlobals.instance().getWorkDirectory();
            f = new File(f, baseName + ".java");
            f.createNewFile();
            f.deleteOnExit();

            FileWriter fw = new FileWriter(f);
            fw.write(src);
            fw.flush();
            fw.close();
            return f;
        } catch (IOException e) {
            return null;
        }
    }

    public static File compileJavaClassFromString(String src, boolean completeOnBadCompile) {
        String baseName = null;

        // Find the package subdirectory
        Pattern pat = Pattern.compile("package.+;");
        Matcher mat = pat.matcher(src);
        boolean fnd = mat.find();

        String packagePath = "";
        if (fnd) {
            int st = mat.start();
            int end = mat.end();
            String s = src.substring(st, end);
            s = s.replace(';', '/');
            String[] sa = s.split("\\s");
            sa[1] = sa[1].replace('.', '/');
            packagePath = sa[1].trim();
        }
        // done finding the package subdir (just to mark the file as "deleteOnExit")

        pat = Pattern.compile("public\\s+class\\s+");
        mat = pat.matcher(src);
        fnd = mat.find();
        // if(fnd) {
        int end = mat.end();
        String s = src.substring(end, end + 128).trim();
        String[] sa = s.split("\\s+");

        baseName = sa[0];
        // }
        try {
            File f = VGlobals.instance().getWorkDirectory();
            f = new File(f, baseName + ".java");
            f.createNewFile();
            f.deleteOnExit();

            FileWriter fw = new FileWriter(f);
            fw.write(src);
            fw.flush();
            fw.close();

            String cp = getCustomClassPath();

            //int reti =  com.sun.tools.javac.Main.compile(new String[]{"-Xlint:unchecked", "-Xlint:deprecation", "-verbose", "-classpath",cp,"-d", f.getParent(), f.getCanonicalPath()});
            System.out.println("Compiling " + f.getCanonicalPath());
            int reti = com.sun.tools.javac.Main.compile(new String[]{"-Xlint:unchecked", "-Xlint:deprecation", "-classpath", cp, "-d", f.getParent(), f.getCanonicalPath()});

            if (reti == 0 || completeOnBadCompile) {
                return new File(f.getParentFile().getAbsoluteFile(), packagePath + baseName + ".class");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static PkgAndFile createTemporaryEventGraphClass(File xmlFile) {
        PkgAndFile paf;
        try {
            String src = buildJavaEventGraphSource(xmlFile);
            
            // If using plain Vanilla Viskit, don't compile diskit extended EGs
            // as diskit.jar won't be available
            if (src.contains("diskit") && !new File("lib/ext/diskit.jar").exists()) {
                FileBasedClassManager.inst().addCacheMiss(xmlFile);
                return null;
            }
            paf = compileJavaClassAndSetPackage(src);
            FileBasedClassManager.inst().addCache(xmlFile, paf.f);
            return paf;
        } catch (Exception e) {
            log.error("Error creating Java class file from " + xmlFile + ": " + e.getMessage());
            FileBasedClassManager.inst().addCacheMiss(xmlFile);
        }
        return null;
    }

    /** Not currently used */
    PkgAndFile createEventGraphClass(File xmlFile) {
        try {
            String src = buildJavaEventGraphSource(xmlFile);
            PkgAndFile paf = compileJavaClassAndSetPackage(src);
            FileBasedClassManager.inst().addCache(xmlFile, paf.f);
            return paf;
        } catch (Exception e) {
            log.error("Error creating Java class file from " + xmlFile + ": " + e.getMessage());
            FileBasedClassManager.inst().addCacheMiss(xmlFile);
        }
        return null;
    }

    static PkgAndFile compileJavaClassAndSetPackage(String source) {
        return compileJavaClassAndSetPackage(source, false);
    }

    static PkgAndFile compileJavaClassAndSetPackage(String source, boolean continueOnBadCompile) {
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
            File f = compileJavaClassFromString(source, continueOnBadCompile);
            if (f != null) {
                //f.deleteOnExit(); // these get cached now for startup
                return new PkgAndFile(pkg, f);
            }
        }
        return null;
    }


    // From menu

    public void export2grid() {
        ViskitAssemblyModel model = (ViskitAssemblyModel) getModel();
        File tFile = null;
        try {
            tFile = File.createTempFile("ViskitAssy", ".xml");
        } catch (IOException e) {
            ((ViskitAssemblyView) getView()).genericErrorReport("File System Error",
                    "Error creating temporary file.");
            return;
        }
        model.saveModel(tFile);
    //todo switch to DOE
    }
    private String[] execStrings;

    public void initAssemblyRun() {
        String src = produceJavaClass();                   // asks to save
        PkgAndFile paf = compileJavaClassAndSetPackage(src, true);
        if (paf != null) {
            File f = paf.f;
            String clNam = f.getName().substring(0, f.getName().indexOf('.'));
            clNam = paf.pkg + "." + clNam;

            String classPath = getCustomClassPath();

            execStrings = buildExecStrings(clNam, classPath);
        } else {
            execStrings = null;
        }
    }

    public void runAssembly() {
        initAssemblyRun();
        if (execStrings == null) {
            JOptionPane.showMessageDialog(null, "Error on compile");         //todo, more information
        } else {
            runner.exec(execStrings);
        }
    }

    static String getCustomClassPath() {
        VGlobals.instance().resetWorkClassLoader();
        return Vstatics.getCustomClassPath();
    }

    private static void handleFileBasedClasses() {
        Collection fileClasses = FileBasedClassManager.inst().getFileLoadedClasses();
        for (Iterator itr = fileClasses.iterator(); itr.hasNext();) {
            FileBasedAssyNode fbn = (FileBasedAssyNode) itr.next();
            if (fbn.isXML) {
                createTemporaryEventGraphClass(fbn.xmlSource);
            } else {
                moveClassFileIntoPlace(fbn);
            }
        }
    }

    private static void moveClassFileIntoPlace(FileBasedAssyNode fbn) {
        File f = new File(VGlobals.instance().getWorkDirectory(),
                fbn.pkg.replace('.', Vstatics.getFileSeparator().charAt(0)));
        f.mkdir();

        File target = new File(f, fbn.classFile.getName());
        try {
            target.createNewFile();

            BufferedInputStream is = new BufferedInputStream(new FileInputStream(fbn.classFile));
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(target));
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static final int EXEC_JAVACMD = 0;
    public static final int EXEC_VMARG0 = 1;
    public static final int EXEC_VMARG1 = 2;
    public static final int EXEC_VMARG3 = 3;
    public static final int EXEC_DASH_CP = 4;
    public static final int EXEC_CLASSPATH = 5;
    public static final int EXEC_RUNNER_CLASS_NAME = 6;
    public static final int EXEC_TARGET_CLASS_NAME = 7;
    public static final int EXEC_VERBOSE_SWITCH = 8;
    public static final int EXEC_STOPTIME_SWITCH = 9;
    public static final int EXEC_FIRST_ENTITY_NAME = 10;

    // The following four match the previous four, but represent indices as seen by the main()
  // method in the launched class:

    public static final int APP_TARGET_CLASS_NAME = 0;
    public static final int APP_VERBOSE_SWITCH = 1;
    public static final int APP_STOPTIME_SWITCH = 2;
    public static final int APP_FIRST_ENTITY_NAME = 3;

    /**
   * Maintain the above statics to match the order below.
   * @param className
   * @param classPath
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
        v.add("viskit.InternalAssemblyRunner$ExternalSimRunner");  // 6
        v.add(className);            // 7

        v.add("" + ((ViskitAssemblyModel) getModel()).getMetaData().verbose);    // 8
        v.add(((ViskitAssemblyModel) getModel()).getMetaData().stopTime);      // 9

        Vector<String> vec = ((ViskitAssemblyModel) getModel()).getVerboseEntityNames();
        for (String s : vec) {
            v.add(s);                                                  // 10+
        }

        String[] ra = new String[v.size()];
        return (String[]) v.toArray(ra);
    }
    private String imgSaveCount = "";
    private int imgSaveInt = -1;

    public void captureWindow() //-------------------------
    {
        String fileName = "AssemblyScreenCapture";
        if (lastFile != null) {
            fileName = lastFile.getName();
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

    class timerCallback implements ActionListener {

        File fil;
        boolean display;

        timerCallback(File f, boolean b) {
            fil = f;
            display = b;
        }

        public void actionPerformed(ActionEvent ev) {
            // create and save the image
          //Component component = (Component) getView();

            // Similarly to Controller.java (EG editor controller), putting the views into tabs requires the following two
          // to replace the one above.
            AssemblyViewFrame avf = (AssemblyViewFrame) getView();
            //Component component = avf.getContent();

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

            /*
      Point p = new Point(0, 0);
      SwingUtilities.convertPointToScreen(p, component);
      Rectangle region = component.getBounds();
      region.x = p.x;
      region.y = p.y;
      BufferedImage image = null;
      try {
        image = new Robot().createScreenCapture(region);
        ImageIO.write(image, "png", fil);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
 */

            // display a scaled version
            if (display) {
                JFrame frame = new JFrame("Saved as " + fil.getName());
                //ImageIcon ii = new ImageIcon(image.getScaledInstance(image.getWidth() * 50 / 100, image.getHeight() * 50 / 100, Image.SCALE_FAST));
              // Nah...
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
    
    /** Opens each EG associated with this Assembly
     * @param f the Assembly File to open EventGraphs for
     */
    private void openEventGraphs(File f) {
        
        try {

            // Close any currently open EGs
            VGlobals.instance().getEventGraphEditor().controller.closeAll();

            EventGraphCache.makeEntityTable(f.getAbsolutePath());
            for (String filePath : EventGraphCache.getEventGraphFiles()) {
                // _doOpen checks if a tab is already opened
                VGlobals.instance().getEventGraphEditor().controller._doOpen(new File(filePath));
            }
        } catch (Exception ex) {
            log.info(ex);
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
    }
    
    private ArrayList<String> openV;
    private void _setFileLists() {
        openV = new ArrayList<String>(4);
        String[] valueAr = historyConfig.getStringArray(assyHistoryKey + "[@value]");
        log.debug("_setFileLists() valueAr size is: " + valueAr.length);
        for (int i = 0; i < valueAr.length; i++) {
            recentFileList.add(valueAr[i]);
            String op = historyConfig.getString(assyHistoryKey + "(" + i + ")[@open]");

            if (op != null && (op.toLowerCase().equals("true") || op.toLowerCase().equals("yes"))) {
                openV.add(valueAr[i]);
            }
        }
    }

    private void saveHistoryXML(ArrayList recentFiles) {
        historyConfig.clearTree(assyHistoryClearKey);

        for (int i = 0; i < recentFiles.size(); i++) {
            String value = (String) recentFiles.get(i);
            historyConfig.setProperty(assyHistoryKey + "(" + i + ")[@value]", value);
        }
        historyConfig.getDocument().normalize();
    }

    private void setRecentFileList(ArrayList lis) {
        saveHistoryXML(lis);
    }

    private void markConfigOpen(File f) {
        int idx = recentFileList.indexOf(f.getAbsolutePath());
        if (idx != -1) {
            historyConfig.setProperty(assyHistoryKey + "(" + idx + ")[@open]", "true");
        }

    // The open attribute is zeroed out for all recent files the first time a file is opened
    }

    private ArrayList<String> getRecentFileList(boolean refresh) {
        if (refresh || recentFileList == null) {
            _setFileLists();
        }
        return recentFileList;
    }

    XMLConfiguration historyConfig;
    String assyHistoryKey = "history.AssemblyEditor.Recent.AssemblyFile";
    String assyHistoryClearKey = "history.AssemblyEditor.Recent";
    private void initConfig() {
        try {
            historyConfig = VGlobals.instance().getHistoryConfig();
        } catch (Exception e) {
            System.out.println("Error loading history file: " + e.getMessage());
            System.out.println("Recent file saving disabled");
            historyConfig = null;
        }
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