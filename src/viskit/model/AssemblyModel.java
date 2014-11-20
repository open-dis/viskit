package viskit.model;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Vector;
import viskit.util.FileBasedAssyNode;
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

    void newEventGraph(String widgetName, String className, Point2D p);

    void newEventGraphFromXML(String widgetName, FileBasedAssyNode node, Point2D p);

    void newPropChangeListener(String widgetName, String className, Point2D p);

    void newPropChangeListenerFromXML(String widgetName, FileBasedAssyNode node, Point2D p);

    /**
     * Boolean to signify whether the model has been changed since last disk
     * save.
     *
     * @return true means changes have been made and it needs to be flushed.
     */
    boolean isDirty();

    void setDirty(boolean tf);  // to force save

    /**
     * Messaged by controller when a new Model should be loaded.
     *
     * @param f File representing persistent model representation. If null,
     * model resets itself to 0 nodes, 0 edges, etc.
     * @return indication of success
     */
    boolean newModel(File f);

    void saveModel(File f);

    /**
     * @return a File object representing the last one passed to the two methods
     * above
     */
    File getLastFile();

    GraphMetaData getMetaData();

    void changeMetaData(GraphMetaData gmd);

    SimkitAssembly getJaxbRoot();

    AdapterEdge newAdapterEdge(String name, AssemblyNode src, AssemblyNode target); //EvGraphNode src, EvGraphNode target);

    PropChangeEdge newPclEdge(AssemblyNode src, AssemblyNode target); //EvGraphNode src, PropChangeListenerNode target);

    void newSimEvLisEdge(AssemblyNode src, AssemblyNode target); //EvGraphNode src, EvGraphNode target);

    boolean changePclNode(PropChangeListenerNode pclNode);

    boolean changeEvGraphNode(EvGraphNode evNode);

    void changePclEdge(PropChangeEdge pclEdge);

    void changeAdapterEdge(AdapterEdge aEdge);

    void changeSimEvEdge(SimEvListenerEdge seEdge);

    void deleteEvGraphNode(EvGraphNode evNode);

    void deletePCLNode(PropChangeListenerNode pclNode);

    void deleteAdapterEdge(AdapterEdge ae);

    /**
     * Assembly nodes don't hold onto edges.
     * @param pce
     */
    void deletePropChangeEdge(PropChangeEdge pce);

    void deleteSimEvLisEdge(SimEvListenerEdge sele);

    Vector<String> getDetailedOutputEntityNames();

    Vector<String> getVerboseOutputEntityNames();

    /** NOTE: Not currently used
     * Notify of a change in the external classpath
     * @param v a Vector of external class names cached
     */
    void externalClassesChanged(Vector<String> v);

    boolean nameExists(String name);
}
