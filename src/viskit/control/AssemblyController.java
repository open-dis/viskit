package viskit.control;

import edu.nps.util.DirectoryWatch;
import java.awt.Point;
import java.io.File;
import java.util.Set;
import java.util.Vector;
import javax.swing.JComponent;
import viskit.util.FileBasedAssemblyNode;
import viskit.util.OpenAssembly;
import viskit.model.*;
import viskit.mvc.MvcRecentFileListener;

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

    void newPropertyChangeListenerNode();

    /** User has established some parameter, model can create object
     * @param name the name of the node
     * @param p the (x, y) point it will appear
     */
    void newEventGraphNode(String name, Point p);

    void newFileBasedEventGraphNode(FileBasedAssemblyNode xnode, Point p);

    void newFileBasedPropertyChangeListenerNode(FileBasedAssemblyNode xnode, Point p);

    void newPropertyChangeListenerNode(String name, Point p);

    /**
     * Edit the properties (metadata) of the Assembly
     */
    void editGraphMetadata();

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
    void zipProject();

    void viewXML();

    /** Handles UI selection of nodes and edges
     *
     * @param v a Vector of nodes and edges
     */
    void selectNodeOrEdge(Vector<Object> v);

    /**
     * Creates an adapter edge between two assembly nodes
     *
     * @param nodeArray and array of Nodes to connect with an adapter
     */
    void newAdapterEdge(Object[] nodeArray);

    void newSimEventListenerEdge(Object[] nodeArray);

    void newPropertyChangeListenerEdge(Object[] rnodeArray);

    void propertyChangeListenerEdit(PropertyChangeListenerNode pclNode);

    /** Handles editing of EventGraph nodes
     * @param eventGraphNode the node to edit
     */
    void eventGraphEdit(EventGraphNode eventGraphNode);

    /** Edits the PropertyChangeListener edge
     *
     * @param pclEdge the PCL edge to edit
     */
    void propertyChangeListenerEdgeEdit(PropertyChangeEdge pclEdge);

    /** Edits the Adapter edge
     *
     * @param adapterEdge the Adapter edge to edit
     */
    void adapterEdgeEdit(AdapterEdge adapterEdge);

    /** Edits the selected SimEvent listener edge
     *
     * @param seEdge the SimEvent edge to edit
     */
    void simEventListenerEdgeEdit(SimEventListenerEdge seEdge);

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
     * Save the current Assembly file as is
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

    /** Closes all open Assembly files and their corresponding Event Graph files */
    void closeAll();

    /**
     * @return indication of completion
     */
    boolean preClose();

    /**
     * Clean up for closing Assembly models
     */
    void postClose();

    void showViskitUserPreferences();

    /**
     * Perform Assembly Editor shutdown duties
     *
     * @return true if Assembly was dirty (modified)
     */
    boolean preQuit();

    void   postQuit();

    /**
     * @param lis the AssemblyChangeListener to add as a listener
     */
    void addAssemblyFileListener(OpenAssembly.AssemblyChangeListener lis);

    /**
     * @param lis the AssemblyChangeListener to remove as a listener
     */
    void removeAssemblyFileListener(OpenAssembly.AssemblyChangeListener lis);

    OpenAssembly.AssemblyChangeListener getAssemblyChangeListener();

    /** @return a DirectoryChangeListener */
    DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener();

    /**
     * Generates Java source code from an Assembly file and displays it from
     * a source window for inspection.
     */
    void generateJavaSource();

    /** Prepare the Assembly for simulation run. This is called from 
     * the AssemblyView via reflection when the Prepare Assembly Run button 
     * is selected from the Assembly Editor panel.
     */
    void prepareSimulationRunner();

    /** Generating java source and compilation are taken care of here */
    void prepareAssemblySimulationRun();

    void export2grid();

    /** Screen capture a snapshot of the Assembly View Frame */
    void captureWindow();

    void addRecentAssemblyFileSetListener(MvcRecentFileListener lis);

    void removeRecentAssemblyFileSetListener(MvcRecentFileListener lis);

    Set<String> getRecentAssemblyFileSet();

    void clearRecentAssemblyFileList();

    void addRecentProjectFileSetListener(MvcRecentFileListener lis);

    void removeRecentProjectFileSetListener(MvcRecentFileListener lis);

    Set<String> getRecentProjectFileSet();

    void clearRecentProjectFileSet();
    
    public void setMainTabbedPane(JComponent tabbedPane, int idx);
}
