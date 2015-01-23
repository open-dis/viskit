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
public interface Model {

    /**
     * Separate initialization from object construction.
     */
    void init();

    /**
     * Messaged by controller when a new Model should be created, or an existing
     * model is loading at startup.
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
    File getLastFile();

    /**
     * Reports saved state of model.  Becomes "clean" after a save.
     * @return state of model
     */
    boolean isDirty();

    /**
     * This is messaged by the controller, typically after a newModel(f) message.  It is used to inst a vector of all the
     * nodes in the graph.  Since the EventNode object has src and target members, it also serves to inst all the edges.
     * @return Vector of EventNodes.
     */
    Vector<ViskitElement> getAllNodes();

    /**
     * Messaged by controller to inst all defined StateVariables.
     * @return Vector of StateVariables.
     */
    Vector<ViskitElement> getStateVariables();

    /**
     * Messaged by controller to inst all defined simulation parameters.  Order (may be) important (?), ergo ArrayList container.
     * @return Vector of vParameter objects.
     */
    Vector<ViskitElement> getSimParameters();

    /**
     * Add a new event to the graph with the given label, at the given point
     * @param nodeName the name of the Event Node
     * @param p the (x, y) position of the Event Node
     */
    void newEvent(String nodeName, Point2D p);

    void newEdge(EventNode src, EventNode target);

    void newCancelEdge(EventNode src, EventNode target);

    /**
     * Delete the referenced event, also deleting attached edges.
     *
     * @param node the node to delete
     */
    void deleteEvent(EventNode node);

    void deleteEdge(SchedulingEdge edge);

    void deleteCancelEdge(CancellingEdge edge);

    void changeEdge(SchedulingEdge e);

    void changeCancelEdge(CancellingEdge e);

    /** Modifies the properties of this EG model
     *
     * @param gmd the meta data that contains changes to record
     */
    void changeMetaData(GraphMetaData gmd);

    /**
     * Notify of a change to an Event Node
     * @param en the event node that changed
     * @return true if a change occurred
     */
    boolean changeEvent(EventNode en);

    void newStateVariable(String name, String type, String initVal, String comment);

    void newSimParameter(String name, String type, String initVal, String comment);

    boolean changeStateVariable(vStateVariable st);

    boolean changeSimParameter(vParameter p);

    void changeCodeBlock(String s);

    void deleteStateVariable(vStateVariable sv);

    void deleteSimParameter(vParameter p);

    GraphMetaData getMetaData();

    /**
     * This is to allow the controller to stick in a Run event, but treat the graph as fresh.
     * @param dirty, if true force to save
     */
    void setDirty(boolean dirty);

    String generateLocalVariableName();

    String generateIndexVariableName();

    void resetLVNameGenerator();

    void resetIdxNameGenerator();

    String generateStateVariableName();
}
