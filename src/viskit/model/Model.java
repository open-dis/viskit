package viskit.model;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 1:43:07 PM
 * @version $Id$
 */
public interface Model
{
    static final Logger LOG = LogManager.getLogger();

    /**
     * Separate initialization from object construction.
     */
    void initialize();

    /**
     * Messaged by controller when a new Model should be created, or an existing
     * model is loading at startup.
     * @param f File representing persistent model representation. If null, model resets itself to 0 nodes, 0 edges, etc.
     * @return for good open
     */
    boolean newModel(File f);

    /**
     * Save existing model.
     * @return indication of success or failure
     */
    boolean save();

    /**
     * Save existing model to specified file.  If null, save to last file.  If no last file, error.
     *
     * @param f File to save to.
     * @return indication of success or failure
     */
    boolean saveModel(File f);

    /** @return a File object representing the last one passed to the two methods above */
    File getLastFile();

    /**
     * Reports saved state of model.  Becomes "clean" after a save.
     * @return state of model
     */
    boolean isModelModified();

    /**
     * This is messaged by the controller, typically after a newModel(f) message.
     * It is used to inst a vector of all the nodes in the graph.  Since the
     * EventNode object has src and target members, it also serves to inst all
     * the edges.
     *
     * @return Vector of EventNodes.
     */
    Vector<ViskitElement> getAllNodes();

    /**
     * Messaged by controller to retrieve all defined StateVariables
     *
     * @return Vector of StateVariables
     */
    Vector<ViskitElement> getStateVariables();

    /**
     * Messaged by controller to inst all defined simulation parameters
     *
     * @return Vector of ViskitParameter objects.
     */
    Vector<ViskitElement> getSimulationParameters();

    /**
     * Add a new event to the graph with the given label, at the given point
     * @param nodeName the name of the Event Node
     * @param p the (x, y) position of the Event Node
     */
    void newEventNode(String nodeName, Point2D p);

    /** Models a scheduling edge between two nodes
     *
     * @param src the source node
     * @param target the target node
     */
    void newSchedulingEdge(EventNode src, EventNode target);

    /** Models a canceling edge between two nodes
     *
     * @param src the source node
     * @param target the target node
     */
    void newCancelingEdge(EventNode src, EventNode target);

    /**
     * Delete the referenced event, also deleting attached edges.  Also handles
     * an undo
     *
     * @param node the node to delete or undo
     */
    void deleteEvent(EventNode node);

    /** Deletes the given edge from the canvas.  Also handles an undo
     *
     * @param edge the edge to delete or undo
     */
    void deleteSchedulingEdge(Edge edge);

    /** Deletes the given edge from the canvas.  Also handles and undo
     *
     * @param edge the edge to delete or undo
     */
    void deleteCancelingEdge(Edge edge);

    /** Changes the given edge on the canvas
     *
     * @param e the edge to delete
     */
    void changeSchedulingEdge(Edge e);

    /** Changes the given edge on the canvas
     *
     * @param e the edge to delete
     */
    void changeCancelingEdge(Edge e);

    /** Modifies the properties of this Event Graph model
     *
     * @param graphMetadata the meta data that contains changes to record
     */
    void changeMetadata(GraphMetadata graphMetadata);

    /**
     * Notify of a change to an Event Node
     * @param en the event node that changed
     * @return true if a change occurred
     */
    boolean changeEventNode(EventNode en);

    void newStateVariable(String name, String type, String initVal, String comment);

    void newSimulationParameter(String name, String type, String initVal, String description);

    boolean changeStateVariable(ViskitStateVariable st);

    boolean changeSimParameter(ViskitParameter p);

    void changeSourceCodeBlock(String s);

    void deleteStateVariable(ViskitStateVariable sv);

    void deleteSimParameter(ViskitParameter p);

    GraphMetadata getMetadata();

    /**
     * This is to allow the controller to stick in a Run event, but treat the graph as fresh.
     * @param newModelModifiedStatus, if true then prompt user to save
     */
    void setModelModified(boolean newModelModifiedStatus);

    String generateLocalVariableName();

    String generateIndexVariableName();

    void resetLocalVariableNameGenerator();

    void resetIndexNameGenerator();

    String generateStateVariableName();

    /** Supports redo of a canceling edge
     * @param ed the CancelingEdge to reinsert into the model
     */
    void redoCancelingEdge(Edge ed);

    /**
     * Supports redo of a scheduling edge
     * @param ed the SchedulingEdge to reinsert into the model
     */
    void redoSchedulingEdge(Edge ed);

    /**
     * Supports redo of an event node
     * @param node the event to reinsert into the model
     */
    void redoEvent(EventNode node);
}
