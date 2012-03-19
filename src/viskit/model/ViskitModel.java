package viskit.model;

import java.awt.Point;
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
public interface ViskitModel {

    /**
     * Separate initialization from object construction.
     */
    void init();

    /**
     * Messaged by controller when a new Model should be loaded.
     * @param f File representing persistent model representation.  If null, model resets itself to 0 nodes, 0 edges, etc.
     * @return for good open
     */
    boolean newModel(File f);

    /**
     * Save existing model to specified file.  If null, save to last file.  If no last file, error.
     * @param f File to save to.
     */
    void saveModel(File f);

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
    Vector<? extends ViskitElement> getAllNodes();

    /**
     * Messaged by controller to inst all defined StateVariables.
     * @return Vector of StateVariables.
     */
    Vector<? extends ViskitElement> getStateVariables();

    /**
     * Messaged by controller to inst all defined simulation parameters.  Order (may be) important (?), ergo ArrayList container.
     * @return Vector of vParameter objects.
     */
    Vector<? extends ViskitElement> getSimParameters();

    /**
     * Add a new event to the graph with the given label, at the given point
     * @param nodeName the name of the Event Node
     * @param p the (x, y) position of the Event Node
     */
    void newEvent(String nodeName, Point p);

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

    void changeMetaData(GraphMetaData gmd);

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
     * @param dirt
     */
    void setDirty(boolean dirty);

    String generateLocalVariableName();

    String generateIndexVariableName();

    void resetLVNameGenerator();

    void resetIdxNameGenerator();

    String generateStateVariableName();
}
