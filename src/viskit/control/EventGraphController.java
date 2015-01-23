package viskit.control;

import edu.nps.util.DirectoryWatch;
import java.awt.Point;
import java.io.File;
import java.util.Set;
import java.util.Vector;
import viskit.model.*;
import viskit.mvc.mvcRecentFileListener;

/**
 * @author Mike Bailey
 * @since Mar 19, 2004
 * @since 9:00:57 AM
 * @version $Id$
 */
public interface EventGraphController {

    /**
     * Start app
     */
    void begin();

    /**
     * User has clicked a button or menu item
     */
    void newNode();

    void newSimParameter();

    /** Comes in from plus button on State Variables panel */
    void newStateVariable();

    /**
     * User has established some entity parameters, model can create objects
     *
     * @param p the graphical point of new node
     */
    void buildNewNode(Point p);

    void buildNewNode(Point p, String name);

    void buildNewSimParameter(String name, String type, String initVal, String comment);

    void buildNewStateVariable(String name, String type, String initVal, String comment);

    void buildNewArc(Object[] nodes);

    void buildNewCancelArc(Object[] nodes);

    /**
     * Provides an automatic capture of all Event Graphs images used in an
     * Assembly and stores them to a specified location for inclusion in the
     * generated Analyst Report
     *
     * @param eventGraphs a list of Event Graph paths to image capture
     * @param eventGraphImages a list of Event Graph image paths to write .png
     * files
     */
    void captureEventGraphImages(java.util.List<File> eventGraphs, java.util.List<String> eventGraphImages);

    void editGraphMetaData();

    /**
     * Create a new blank EventGraph model
     */
    void newEventGraph();

    /**
     * Creates a new Viskit Project
     */
    void newProject();

    void runAssemblyEditor();

    /** Show the XML form of an event graph */
    void showXML();

    /** A component, e.g., vMod, wants to say something.
     *
     * @param typ the type of message, i.e. ERROR, WARN, INFO, QUESTION, etc.
     * @param title the title of the message in the dialog frame
     * @param msg the message to transmit
     */
    void messageUser(int typ, String title, String msg);    // typ is one of JOptionPane types

    /** Requests to the controller to perform editing operations on existing entities
     * @param node the node to edit
     */
    void nodeEdit(EventNode node);

    void arcEdit(SchedulingEdge ed);

    void canArcEdit(CancellingEdge ed);

    void simParameterEdit(vParameter param);

    void stateVariableEdit(vStateVariable var);

    void codeBlockEdit(String s);

    /* menu selections */
    void copy();

    /**
     * Ultimately performs a delete function for selected edges and nodes
     */
    void cut();        // to remove nodes and edges

    /**
     * Opens selected files from a FileChooser
     */
    void open();

    void openRecentEventGraph(String path);

    void close();

    /**
     * Closes all open EGs open from a project
     */
    void closeAll();

    /** Performs the paste operation CNTL-V or CMD-V */
    void paste();

    /**
     * Perform shutdown operations
     */
    void quit();

    void save();

    /**
     * Save the current EventGraph "as" desired by user
     */
    void saveAs();

    void selectNodeOrEdge(Vector<Object> v);

    void settings();

    boolean preClose();

    boolean preQuit();

    void postClose();

    void postQuit();

    void deleteSimParameter(vParameter p);

    void deleteStateVariable(vStateVariable var);

    void eventList();

    /**
     * Generates Java source code from an Event Graph file
     */
    void generateJavaSource();

    /**
     * Provides a single screenshot capture capability
     */
    void captureWindow();

    void addOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis);

    void removeOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis);

    void addRecentEgFileListener(mvcRecentFileListener lis);

    void removeRecentEgFileListener(mvcRecentFileListener lis);

    Set<String> getRecentFileSet();

    /** Clears the recent EG file list thus far generated */
    void clearRecentFileSet();
}
