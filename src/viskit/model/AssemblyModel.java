package viskit.model;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Vector;
import viskit.util.FileBasedAssemblyNode;
import viskit.xsd.bindings.assembly.SimkitAssembly;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 17, 2004
 * Time: 9:16:26 AM
 */
 public interface AssemblyModel {

    /** Places an event graph node on the assembly palette when the user drags
     * an icon from the LEGO tree
     *
     * @param widgetName the name of the Event Graph
     * @param className the Event Graph class from parsing third party libs of the current project
     * @param p the point on the palette to place the node icon
     */
     void newEventGraph(String widgetName, String className, Point2D p);

     /**
      * Support redo for the event graph node
      * @param node the node to redo
      */
     void redoEventGraph(EventGraphNode node);

    /** Places an event graph node on the assembly palette when the user drags
     * an icon from the LEGO tree
     *
     * @param widgetName the name of the Event Graph
     * @param node the cached node from parsing the Event Graph directory of the current project
     * @param p the point on the palette to place the node icon
     */
    void newEventGraphFromXML(String widgetName, FileBasedAssemblyNode node, Point2D p);

    void newPropertyChangeListener(String widgetName, String className, Point2D p);

    /**
     * Supports redo of a PropertyChangeListenerNode
     * @param node the node to redo
     */
    void redoPropertyChangeListener(PropertyChangeListenerNode node);

    void newPropertyChangeListenerFromXML(String widgetName, FileBasedAssemblyNode node, Point2D p);

    /**
     * Boolean to signify whether the model has been changed since last disk
     * save.
     *
     * @return true means changes have been made and it needs to be flushed.
     */
    boolean isModelModified();

    void setModelModified(boolean newValue);  // to force save

    /**
     * Messaged by controller when a new Model should be loaded.
     *
     * @param f File representing persistent model representation. If null,
     * model resets itself to 0 nodes, 0 edges, etc.
     * @return indication of success
     */
    boolean newAssemblyModel(File f);

    /** Saves the current Assembly file out to XML
     *
     * @param f the Assembly file to save
     */
    void saveModel(File f);

    /**
     * @return a File object representing the last one passed to the two methods
     * above
     */
    File getCurrentFile();

    /** Retrieve the meta data for this Assembly
     *
     * @return the meta data for this Assembly
     */
    GraphMetadata getMetadata();

    void changeMetadata(GraphMetadata newGraphMetadata);

    SimkitAssembly getJaxbRoot();

    AdapterEdge newAdapterEdge(String name, AssemblyNode src, AssemblyNode target); //EvGraphNode src, EventGraphNode target);

    PropertyChangeEdge newPropChangeEdge(AssemblyNode src, AssemblyNode target); //EvGraphNode src, PropertyChangeListenerNode target);

    void newSimEventListenerEdge(AssemblyNode src, AssemblyNode target); //EvGraphNode src, EventGraphNode target);

    boolean changePclNode(PropertyChangeListenerNode pclNode);

    boolean changeEventGraphNode(EventGraphNode evNode);

    void changePclEdge(PropertyChangeEdge pclEdge);

    void changeAdapterEdge(AdapterEdge aEdge);

    void changeSimEventListenerEdge(SimEventListenerEdge seEdge);

    void deleteEventGraphNode(EventGraphNode evNode);

    void deletePropertyChangeListener(PropertyChangeListenerNode pclNode);

    void deleteAdapterEdge(AdapterEdge ae);

    /**
     * Support redo of an AdapterEdge
     * @param ae the edge to redo
     */
    void redoAdapterEdge(AdapterEdge ae);

    /**
     * Assembly nodes don't hold onto edges.
     * @param pce the edge to delete
     */
    void deleteProperyChangeEdge(PropertyChangeEdge pce);

    /**
     * Supports redo of a PropertyChangeEdge
     * @param pce the edge to redo
     */
    void redoPropertyChangeEdge(PropertyChangeEdge pce);

    void deleteSimEventListenerEdge(SimEventListenerEdge sele);

    /**
     * Supports redo of a SimEvLisEdge
     * @param sele the edge to redo
     */
    void redoSimEventListenerEdge(SimEventListenerEdge sele);

    /** Retrieve a list of detailed output entity names
     *
     * @return a list of detailed output entity names
     */
    Vector<String> getDetailedOutputEntityNames();

    /** Retrieve a list of verbose output entity names
     *
     * @return a list of verbose output entity names
     */
    Vector<String> getVerboseOutputEntityNames();

    /** NOTE: Not currently used
     * Notify of a change in the external classpath
     * @param v a Vector of external class names cached
     */
    void externalClassesChanged(Vector<String> v);

    /** Check for the existence of a SimEntity name
     *
     * @param name the name to check
     * @return true if this name already exists
     */
    boolean nameExists(String name);
}
