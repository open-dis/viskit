package viskit.control;

import edu.nps.util.DirectoryWatch;
import java.awt.Point;
import java.io.File;
import java.util.Set;
import java.util.Vector;
import javax.swing.JComponent;
import viskit.util.FileBasedAssyNode;
import viskit.util.OpenAssembly;
import viskit.model.*;
import viskit.mvc.mvcRecentFileListener;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:27:13 AM
 * @version $Id$
 */
public interface AssemblyController {

    /** Initialize this controller upon startup */
    void begin();

    /** User has clicked a menu item */
    void newEventGraphNode();

    void newPropChangeListenerNode();

    /** User has established some parameter, model can create object
     * @param name the name of the node
     * @param p the (x, y) point it will appear
     */
    void newEventGraphNode(String name, Point p);

    void newFileBasedEventGraphNode(FileBasedAssyNode xnode, Point p);

    void newFileBasedPropChangeListenerNode(FileBasedAssyNode xnode, Point p);

    void newPropChangeListenerNode(String name, Point p);

    /**
     * Edit the properties (metadata) of the Assembly
     */
    void editGraphMetaData();

    /**
     * Create a new blank assembly graph model
     */
    void newAssembly();

    /**
     * Creates a new Viskit Project
     */
    void newProject();

    void setRunTabbedPane(JComponent runTabbedPane, int idx);

    void runEventGraphEditor();

    void showXML();

    /** A component, e.g., vAMod, wants to say something.
     *
     * @param typ the type of message, i.e. WARN, ERROR, INFO, QUESTION
     * @param title the title of the message
     * @param msg the message to transmit
     */
    void messageUser(int typ, String title, String msg);    // typ is one of JOptionPane types

    void selectNodeOrEdge(Vector<Object> v);

    /**
     * Creates an adapter arc between two assembly nodes
     *
     * @param nodes and array of Nodes to connect with an adapter
     */
    void newAdapterArc(Object[] nodes);

    void newSimEvListArc(Object[] nodes);

    void newPropChangeListArc(Object[] nodes);

    void pcListenerEdit(PropChangeListenerNode pclNode);

    void evGraphEdit(EvGraphNode evNode);

    void pcListenerEdgeEdit(PropChangeEdge pclEdge);

    void adapterEdgeEdit(AdapterEdge edgeObj);

    void simEvListenerEdgeEdit(SimEvListenerEdge edgeObj);

    /**
     * menu selections
     */
    void copy();

    /**
     * Ultimately performs a delete function for selected edges and nodes
     */
    void cut();        // to remove nodes and edges

    /**
     * Performs a delete function for selected edges and nodes
     */
    void delete();

    /**
     * Opens a Viskit Project Assembly File
     */
    void open();

    /**
     * Performs project clean up tasks before closing out the project
     */
    void doProjectCleanup();

    /**
     * Opens an already existing Viskit Project
     *
     * @param file the project root file for an existing Viskit project
     */
    void openProject(File file);

    void openRecent(String fullPath);

    void paste();

    /**
     * Perform shutdown operations
     */
    void quit();

    /**
     * Save the current Assy file as is
     */
    void save();

    /**
     * Save the current Assembly File "as" desired by user
     */
    void saveAs();

    // Bug fix: 1195
    /**
     * Calls both pre and post closing actions
     */
    void close();

    void closeAll();

    /**
     * @return indication of completion
     */
    boolean preClose();

    /**
     * Clean up for closing Assembly models
     */
    void postClose();

    void settings();

    /**
     * Perform Assembly Editor shutdown duties
     *
     * @return true if Assembly was dirty (modified)
     */
    boolean preQuit();

    void postQuit();

    /**
     * @param lis the AssyChangeListener to add as a listener
     */
    void addAssemblyFileListener(OpenAssembly.AssyChangeListener lis);

    /**
     * @param lis the AssyChangeListener to remove as a listener
     */
    void removeAssemblyFileListener(OpenAssembly.AssyChangeListener lis);

    OpenAssembly.AssyChangeListener getAssemblyChangeListener();

    DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener();

    /**
     * Generates Java source code from an Assembly file and displays it from
     * a source window for inspection.
     */
    void generateJavaSource();

    /** Compile the Assembly and prepare the Simulation Runner for simulation
     * run.  This is called from the AssemblyView via reflection when the
     * Initialize assembly run button is selected from the Assembly Editor panel.
     */
    void compileAssemblyAndPrepSimRunner();

    /** Saving, generating java source and compilation are taken care of here */
    void initAssemblyRun();

    void export2grid();

    /** Screen capture a snapshot of the Assembly View Frame */
    void captureWindow();

    void addRecentAssyFileSetListener(mvcRecentFileListener lis);

    void removeRecentAssyFileSetListener(mvcRecentFileListener lis);

    Set<String> getRecentAssyFileSet();

    void clearRecentAssyFileList();

    void addRecentProjFileSetListener(mvcRecentFileListener lis);

    void removeRecentProjFileSetListener(mvcRecentFileListener lis);

    Set<String> getRecentProjFileSet();

    void clearRecentProjFileSet();
}
