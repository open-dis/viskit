package viskit;

import java.awt.*;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:27:13 AM
 */
public interface ViskitAssemblyController
{
  public void newEventGraphNode(String name, Point p);
  public void newPropChangeListenerNode(String name, Point p);  
  public void newAssembly();
  public void editGraphMetaData  ();
  
  public void selectNodeOrEdge(Vector v);

}
