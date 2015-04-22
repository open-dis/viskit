package viskit.control;

import edu.nps.util.DirectoryWatch;
import java.awt.Point;
import java.io.File;
import java.util.Set;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
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

    /** Creates a zip of the current project directory and initiates an email
     * client form to open for mailing to the viskit mailing list
     */
    void zipAndMailProject();

    /**
     * Sets the Assembly Run panel
     * @param runPane the parent of the Assembly Run panel
     * @param idx the index to retrieve the Run Pane
     */
    void setAssemblyRunPane(JComponent runPane, int idx);

    /** Retrieves the parent of the Assembly Run Panel
     *
     * @return the parent of the Assembly Run Panel
     */
    JTabbedPane getRunTabbedPane();

    /** Retrieves the index of the Assembly Run Panel within the RunTabbedPane
     * @return the index of the Assembly Run Panel within the RunTabbedPane
     */
    int getRunTabbledPanelIdx();

    void showXML();

    /** A component, e.g., vAMod, wants to say something.
     *
     * @param typ the type of message, i.e. WARN, ERROR, INFO, QUESTION, etc.
     * @param title the title of the message in the dialog frame
     * @param msg the message to transmit
     */
    void messageUser(int typ, String title, String msg);    // typ is one of JOptionPane types

    /** Handles UI selection of nodes and edges
     *
     * @param v a Vector of nodes and edges
     */
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

    /** Handles editing of Event Graph nodes
     *
     * @param evNode the node to edit
     */
    void evGraphEdit(EvGraphNode evNode);

    /** Edits the PropertyChangeListner edge
     *
     * @param pclEdge the PCL edite to edit
     */
    void pcListenerEdgeEdit(PropChangeEdge pclEdge);

    /** Edits the Adapter edge
     *
     * @param aEdge the Adapter edge to edit
     */
    void adapterEdgeEdit(AdapterEdge aEdge);

    /** Edits the selected SimEvent listener edge
     *
     * @param seEdge the SimEvent edge to edit
     */
    void simEvListenerEdgeEdit(SimEvListenerEdge seEdge);

    /** CMD-Z or CNTL-Z */
    void undo();

    /** CMD-Y or CNTL-Y */
    void redo();

    /** Perform a full delete */
    void remove();

    /**
     * Not supported in Viskit
     */
    void cut();

    /**
     * CMD-C or CNTL-C
     */
    void copy();

    /** Performs the paste operation CNTL-V or CMD-V */
    void paste();

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

    void openRecentAssembly(File fullPath);

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

    /** Closes all open Assy files and their corresponding EG files */
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

    /** Generating java source and compilation are taken care of here */
    void initAssemblyRun();

    void export2grid();

    /** Screen capture a snapshot of the Assembly View Frame */
    void captureWindow();

    void addRecentAssyFileSetListener(mvcRecentFileListener lis);

    void removeRecentAssyFileSetListener(mvcRecentFileListener lis);

    Set<File> getRecentAssyFileSet();

    void clearRecentAssyFileList();

    void addRecentProjFileSetListener(mvcRecentFileListener lis);

    void removeRecentProjFileSetListener(mvcRecentFileListener lis);

    Set<File> getRecentProjFileSet();

    void clearRecentProjFileSet();
}
