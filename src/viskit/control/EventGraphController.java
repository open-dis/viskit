package viskit.control;

import edu.nps.util.DirectoryWatch;

import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import viskit.model.*;
import viskit.mvc.MvcRecentFileListener;

/**
 * Abstract interface for EventGraphControllerImpl
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
    void buildNewEventNode();

    void newSimulationParameter();

    /** Comes in from plus button on State Variables panel */
    void newStateVariable();

    /**
     * User has established some entity parameters, model can create objects
     *
     * @param point the graphical point of new node
     */
    void buildNewEventNode(Point point);

    void buildNewEventNode(Point point, String name);

    void buildNewSimulationParameter(String name, String type, String initialValue, String description);

    void buildNewStateVariable(String name, String type, String initialValue, String description);

    /** Connect a scheduling edge between two nodes
     *
     * @param nodes an array of source and target nodes
     */
    void buildNewSchedulingEdge(Object[] nodes);

    /** Connect a canceling edge between two nodes
     *
     * @param nodes an array of source and target nodes
     */
    void buildNewCancelingEdge(Object[] nodes);

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

    void editGraphMetadata();

    /**
     * Creates a new blank EventGraph model
     */
    void newEventGraph();

    /**
     * Creates a new Viskit Project
     */
    void newProject();

    /** 
     * Creates a zip of the current project directory and initiates an email
     * client form to open for mailing to the viskit mailing list
     */
    void zipProject();

    /** Show the XML form of an event graph */
    void viewXML();

    /** 
     * Requests to the controller to perform editing operations on existing entities
     * @param eventNode the node to edit
     */
    void nodeEdit(EventNode eventNode);

    /**
     * Edit a scheduling edge
     * @param schedulingEdge the edge to edit
     */
    void schedulingEdgeEdit(Edge schedulingEdge);

    /**
     * Edit a canceling edge
     * @param cancelingEdge the edge to edit
     */
    void cancelingEdgeEdit(Edge cancelingEdge);

    void simulationParameterEdit(ViskitParameter simParameter);

    void stateVariableEdit(ViskitStateVariable stateVariable);

    void codeBlockEdit(String s);

    /**
     * Opens selected files from a FileChooser
     */
    void open();

    /**
     * Opens one of the recently opened Event Graphs
     * @param recentEventGraphPath to the recent Event Graph
     */
    void openRecentEventGraph(File recentEventGraphPath);

    /** Closes an open Event Graph */
    void close();

    /**
     * Closes all open Event Graphs open from a project
     */
    void closeAll();

    /**
     * Undo the last selected node or edge modified using CMD-Z or CNTL-Z 
     */
    void undo();

    /** Redo the last selected node or edge modified using CMD-Y or CNTL-Y */
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

    void showViskitUserPreferences();

    boolean preClose();

    boolean preQuit();

    void postClose();

    void postQuit();

    void deleteSimParameter(ViskitParameter simParameter);

    void deleteStateVariable(ViskitStateVariable stateVariable);

    void eventList();

    /**
     * Generates Java source code from an Event Graph file
     */
    void generateJavaCode();

    /**
     * Provides a single screenshot capture capability
     */
    void captureWindow();

    void addEventGraphFileListener(DirectoryWatch.DirectoryChangeListener eventGraphFileListener);

    void removeEventGraphFileListener(DirectoryWatch.DirectoryChangeListener eventGraphFileListener);

    void addRecentEventGraphFileListener(MvcRecentFileListener recentEventGraphFileListener);
    
    void removeRecentEventGraphFileListener(MvcRecentFileListener recentEventGraphFileListener);

    Set<String> getRecentEventGraphFileSet();

    /** Clears the recent Event Graph file list thus far generated */
    void clearRecentEventGraphFileSet();
}
