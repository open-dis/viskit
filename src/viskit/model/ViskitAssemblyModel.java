package viskit.model;

import viskit.FileBasedAssyNode;
import viskit.xsd.bindings.assembly.SimkitAssembly;

import java.awt.*;
import java.io.File;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 17, 2004
 * Time: 9:16:26 AM
 */

public interface ViskitAssemblyModel
{
  public void newEventGraph(String widgetName, String className, Point p);
  public void newEventGraphFromXML(String widgetName, FileBasedAssyNode node, Point p);

  public void newPropChangeListener(String widgetName, String className, Point p);
  public void newPropChangeListenerFromXML(String widgetName, FileBasedAssyNode node, Point p);
  /**
    *  Reports saved state of model.  Becomes "clean" after a save.
    */
  public boolean isDirty       ();
  public void    setDirty      (boolean tf);  // to force save
  /**
   * Messaged by controller when a new Model should be loaded.
   * @param f File representing persistent model representation.  If null, model resets itself to 0 nodes, 0 edges, etc.
   */
  public boolean newModel      (File f);
  public void    saveModel     (File f);
  public File    getFile       ();

  public GraphMetaData getMetaData();
  public void changeMetaData    (GraphMetaData gmd);

  public SimkitAssembly getJaxbRoot();
  public AdapterEdge    newAdapterEdge    (String name, AssemblyNode src, AssemblyNode target); //EvGraphNode src, EvGraphNode target);
  public PropChangeEdge newPclEdge        (AssemblyNode src, AssemblyNode target); //EvGraphNode src, PropChangeListenerNode target);
  public void           newSimEvLisEdge   (AssemblyNode src, AssemblyNode target); //EvGraphNode src, EvGraphNode target);

  public boolean changePclNode     (PropChangeListenerNode pclNode);
  public boolean changeEvGraphNode (EvGraphNode evNode);
  public void changePclEdge     (PropChangeEdge pclEdge);
  public void changeAdapterEdge (AdapterEdge aEdge);
  public void changeSimEvEdge   (SimEvListenerEdge seEdge);

  public void deleteEvGraphNode   (EvGraphNode evNode);
  public void deletePCLNode       (PropChangeListenerNode pclNode);
  public void deleteAdapterEdge   (AdapterEdge ae);
  public void deletePropChangeEdge(PropChangeEdge pce);
  public void deleteSimEvLisEdge  (SimEvListenerEdge sele);

  //public String buildJavaAssemblySource();
  //public File   compileJavaClass(String src);

  public Vector getVerboseEntityNames();
  public void externalClassesChanged(Vector v);
}
