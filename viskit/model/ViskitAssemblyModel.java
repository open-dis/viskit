package viskit.model;

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
  public GraphMetaData getMetaData();
  public void changeMetaData   (GraphMetaData gmd);
  public void changeEvGNode      (EvGraphNode ev);
  
}
