package viskit.model;

import viskit.ViskitAssemblyView;

import java.awt.Point;
import java.io.File;
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
  public void newPropChangeListener(String widgetName, String className, Point p);
  /**
    *  Reports saved state of model.  Becomes "clean" after a save.
    */
   public boolean isDirty       ();
  /**
   * Messaged by controller when a new Model should be loaded.
   * @param f File representing persistent model representation.  If null, model resets itself to 0 nodes, 0 edges, etc.
   */
  public void newModel      (File f);
  public void saveModel     (File f);
  
  public GraphMetaData getMetaData();
  public void changeMetaData    (GraphMetaData gmd);

  public AdapterEdge    newAdapterEdge    (EvGraphNode src, EvGraphNode target);
  public PropChangeEdge newPclEdge        (EvGraphNode src, PropChangeListenerNode target);
  public void           newSimEvLisEdge   (EvGraphNode src, EvGraphNode target);

  public void changePclNode     (PropChangeListenerNode pclNode);
  public void changeEvGraphNode (EvGraphNode evNode);
  public void changePclEdge     (PropChangeEdge pclEdge);
  public void changeAdapterEdge (AdapterEdge aEdge);
  public void changeSimEvEdge   (SimEvListenerEdge seEdge);

  public void deleteEvGraphNode   (EvGraphNode evNode);
  public void deletePCLNode       (PropChangeListenerNode pclNode);
  public void deleteAdapterEdge   (AdapterEdge ae);
  public void deletePropChangeEdge(PropChangeEdge pce);
  public void deleteSimEvLisEdge  (SimEvListenerEdge sele);

  public String buildJavaSource();

}
