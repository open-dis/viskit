package viskit.control;

import edu.nps.util.DirectoryWatch;
import java.awt.Point;
import java.io.File;
import java.util.List;
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

    /** Connect a scheduling edge between two nodes
     *
     * @param nodes an array of source and target nodes
     */
    void buildNewSchedulingArc(Object[] nodes);

    /** Connect a canceling edge between two nodes
     *
     * @param nodes an array of source and target nodes
     */
    void buildNewCancelingArc(Object[] nodes);

    /**
     * Provides an automatic capture of all Event Graphs images used in an
     * Assembly and stores them to a specified location for inclusion in the
     * generated Analyst Report
     *
     * @param eventGraphs a list of Event Graph paths to image capture
     * @param eventGraphImages a list of Event Graph image paths to write .png
     * files
     */
    void captureEventGraphImages(List<File> eventGraphs, List<File> eventGraphImages);

    void editGraphMetaData();

    /**
     * Creates a new blank EventGraph model
     */
    void newEventGraph();

    /**
     * Creates a new Viskit Project
     */
    void newProject();

    /** Creates a zip of the current project directory and initiates an email
     * client form to open for mailing to the viskit mailing list
     */
    void zipAndMailProject();

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
     *
     * @param node the node to edit
     */
    void nodeEdit(EventNode node);

    /**
     * Edit a scheduling edge
     *
     * @param ed the edge to edit
     */
    void schedulingArcEdit(Edge ed);

    /**
     * Edit a canceling edge
     *
     * @param ed the edge to edit
     */
    void cancellingArcEdit(Edge ed);

    void simParameterEdit(vParameter param);

    void stateVariableEdit(vStateVariable var);

    void codeBlockEdit(String s);

    /**
     * Opens selected files from a FileChooser
     */
    void open();

    void openRecentEventGraph(File path);

    void close();

    /**
     * Closes all open EGs open from a project
     */
    void closeAll();

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
     * Perform shutdown operations
     */
    void quit();

    /**
     * Save the current EventGraph model to file
     */
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

    void addEventGraphFileListener(DirectoryWatch.DirectoryChangeListener lis);

    void removeEventGraphFileListener(DirectoryWatch.DirectoryChangeListener lis);

    void addRecentEgFileListener(mvcRecentFileListener lis);

    void removeRecentEgFileListener(mvcRecentFileListener lis);

    Set<File> getRecentEGFileSet();

    /** Clears the recent EG file list thus far generated */
    void clearRecentEGFileSet();
}
