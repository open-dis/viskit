package viskit;

import viskit.model.*;

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

  public void newAdapterArc(Object[]nodes);
  public void newSimEvListArc(Object[]nodes);
  public void newPropChangeListArc(Object[]nodes);

  public void pcListenerEdit(PropChangeListenerNode pclNode);
  public void evGraphEdit(EvGraphNode evNode);
  public void pcListenerEdgeEdit(PropChangeEdge pclEdge);
  public void adapterEdgeEdit(AdapterEdge edgeObj);
  public void simEvListenerEdgeEdit(SimEvListenerEdge edgeObj);

  /* menu selections */
  void copy();
  void cut();        // to remove nodes and edges
  void open();
  void paste();
  void quit();
  void save();
  void saveAs();

  void generateJavaClass();
  void runEventGraphEditor();
}
