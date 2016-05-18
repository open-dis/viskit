package viskit.control;

import edu.nps.util.DirectoryWatch;
import java.awt.Point;
import java.io.File;
import java.util.Set;
import java.util.Vector;
import viskit.util.FileBasedAssemblyNode;
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
public interface AssemblyController 
{
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
	
    void newEventGraphNode(String name, Point p, String description);

    void newFileBasedEventGraphNode(FileBasedAssemblyNode xnode, Point p);

    void newFileBasedEventGraphNode(FileBasedAssemblyNode xnode, Point p, String description);

    void newFileBasedPropertyChangeListenerNode(FileBasedAssemblyNode xnode, Point p);

    void newFileBasedPropertyChangeListenerNode(FileBasedAssemblyNode xnode, Point p, String description);

    void newPropertyChangeListenerNode(String name, Point p);

    void newPropertyChangeListenerNode(String name, Point p, String description);

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
    void mailZippedProjectFiles();

    void showXML();

    /** A component, e.g., vAMod, wants to say something.
     *
     * @param dialogType the type of dialog panel, i.e. WARN, ERROR, INFO, QUESTION, etc.
     * @param title the title of the message in the dialog frame
     * @param message the message to transmit
     */
    void messageToUser(int dialogType, String title, String message);    // typ is one of JOptionPane types

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

    void newPropertyChangeListArc(Object[] nodes);

    void propertyChangeListenerEdit(PropertyChangeListenerNode pclNode);

    /** Handles editing of Event Graph nodes
     *
     * @param eventNode the node to edit
     */
    void eventGraphEdit(EventGraphNode eventNode);

    /** Edits the PropertyChangeListner edge
     *
     * @param pclEdge the PCL edite to edit
     */
    void propertyChangeListenerEdgeEdit(PropertyChangeListenerEdge pclEdge);

    /** Edits the Adapter edge
     *
     * @param aEdge the Adapter edge to edit
     */
    void adapterEdgeEdit(AdapterEdge aEdge);

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
    void openAssembly();

    /**
     * Performs project clean up tasks before closing out the project
     */
    void doProjectCleanup();

    /**
     * Opens an already existing Viskit Project
     *
     * @param file the project root file for an existing Viskit project
     */
    void openProjectDirectory(File file);

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

    /** Closes all open Assembly files and their corresponding EG files */
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

    /** Compile the Assembly and prepare the Simulation Runner for simulation
     * run.  This is called from the AssemblyView via reflection when the
     * Assembly Initialization button is selected from the Assembly Editor panel.
     */
    void compileAssemblyAndPrepareSimulationRunner();

    /** Generating java source and compilation are taken care of here */
    void initializeAssemblyRun();
	
    /** Whether active Assembly is ready for Simulation Run 
	 * @return Whether initializeAssemblyRun was successful */
	public boolean isAssemblyReady ();

    void export2grid();

    /** Screen capture an image snapshot of the Assembly Editor frame */
    void windowImageCapture();

    void      addRecentAssemblyFileSetListener(mvcRecentFileListener listener);

    void   removeRecentAssemblyFileSetListener(mvcRecentFileListener listener);

    Set<File> getRecentAssemblyFileSet();

    void    clearRecentAssemblyFileList();

    void      addRecentProjectListener(mvcRecentFileListener listener);

    void   removeRecentProjectListener(mvcRecentFileListener listener);

    Set<File> getRecentProjectFileSet();

    void    clearRecentProjectFileSet();
}
