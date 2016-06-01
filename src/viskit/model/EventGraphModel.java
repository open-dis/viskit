package viskit.model;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Vector;

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
public interface EventGraphModel 
{
    /**
     * Separate initialization from object construction.
     */
    void initialize();

    /**
     * Messaged by controller when a new EventGraphModel should be created, or an existing
 model is loading at startup.
     * @param f File representing persistent model representation.  If null, model resets itself to 0 nodes, 0 edges, etc.
     * @return for good open
     */
    boolean newModel(File f);

    /**
     * Save existing model to specified file.  If null, save to last file.  If no last file, error.
     *
     * @param f File to save to.
     * @return indication of success or failure
     */
    boolean saveModel(File f);

    /** @return a File object representing the last one passed to the two methods above */
    File getCurrentFile();

    /**
     * Reports saved state of model.  Becomes "clean" after a save.
     * @return state of model
     */
    boolean isDirty();

    /**
     * This is messaged by the controller, typically after a newModel(f) message.
     * It is used to inst a vector of all the nodes in the graph.  Since the
     * EventNode object has src and target members, it also serves to instantiate all
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
     * Messaged by controller to inst?? all defined simulation parameters
     *
     * @return Vector of ViskitParameter objects.
     */
    Vector<ViskitElement> getSimulationParameters();

    /**
     * Add a new event to the graph with the given label, at the given point
     * @param nodeName the name of the Event Node
     * @param p the (x, y) position of the Event Node
	 * @return new EventNode
     */
    EventNode newEventNode(String nodeName, Point2D p);

    /** Models a scheduling edge between two nodes
     *
     * @param sourceNode the source node
     * @param targetNode the target node
     */
    void newSchedulingEdge(EventNode sourceNode, EventNode targetNode);

    /** Models a cancelling edge between two nodes
     *
     * @param sourceNode the source node
     * @param targetNode the target node
     */
    void newCancellingEdge(EventNode sourceNode, EventNode targetNode);

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
    void deleteSchedulingEdge(SchedulingEdge edge);

    /** Deletes the given edge from the canvas.  Also handles and undo
     *
     * @param edge the edge to delete or undo
     */
    void deleteCancellingEdge(CancellingEdge edge);

    /** Changes the given edge on the canvas
     *
     * @param edge the edge to delete
     */
    void changeSchedulingEdge(SchedulingEdge edge);

    /** Changes the given edge on the canvas
     *
     * @param edge the edge to change
     */
    void changeCancellingEdge(CancellingEdge edge);

    /** Modifies the properties of this Event Graph model
     *
     * @param graphMetadata the meta data that contains changes to record
     */
    void setMetadata(GraphMetadata graphMetadata);

    /**
     * Notify of a change to an Event Node
     * @param eventNode the event node that changed
     * @return true if a change occurred
     */
    boolean changeEvent(EventNode eventNode);

    void newStateVariable(String name, String type, String initialValue, String description);

    void newSimParameter(String name, String type, String initialValue, String description);

    boolean changeStateVariable(ViskitStateVariable stateParameter);

    boolean changeSimParameter(ViskitParameter simParameter);
	
    String getCodeBlock();

    void changeCodeBlock(String newCodeBlock);

    void deleteStateVariable(ViskitStateVariable stateVariable);

    void deleteSimParameter(ViskitParameter simParameter);

    GraphMetadata getMetadata();

    /**
     * This is to allow the controller to stick in a Run event, but treat the graph as fresh.
     * @param dirty, if true force to save
     */
    void setDirty(boolean dirty);

    String generateLocalVariableName();

    String generateIndexVariableName();

    void resetLocalVariableNameGenerator();

    void resetIndexVariableNameGenerator();

    String generateStateVariableName();

    /** Supports redo of a cancelling edge
     * @param edge the CancellingEdge to reinsert into the model
     */
    void redoCancellingEdge(CancellingEdge edge);

    /**
     * Supports redo of a scheduling edge
     * @param ed the SchedulingEdge to reinsert into the model
     */
    void redoSchedulingEdge(SchedulingEdge ed);

    /**
     * Supports redo of an event node
     * @param eventNode the EventNode to reinsert into the model
     */
    void redoEvent(EventNode eventNode);
}
