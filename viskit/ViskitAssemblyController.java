package viskit;

import edu.nps.util.DirectoryWatch;
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
  // user has clicked a menu item
  public void newEventGraphNode();
  public void newPropChangeListenerNode();

  // user has established some parameter, model can create object
  public void newEventGraphNode                  (String name, Point p);
  public void newFileBasedEventGraphNode         (FileBasedAssyNode xnode, Point p);
  public void newFileBasedPropChangeListenerNode (FileBasedAssyNode xnode, Point p);
  public void newPropChangeListenerNode          (String name, Point p);

  public void editGraphMetaData  ();

  public void newAssembly();

         void runEventGraphEditor();
         void showXML();

  /* a component, e.g., model, wants to say something. */
  public void messageUser(int typ, String msg);    // typ is one of JOptionPane types

  public void selectNodeOrEdge      (Vector v);

  public void newAdapterArc         (Object[]nodes);
  public void newSimEvListArc       (Object[]nodes);
  public void newPropChangeListArc  (Object[]nodes);

  public void pcListenerEdit        (PropChangeListenerNode pclNode);
  public void evGraphEdit           (EvGraphNode evNode);
  public void pcListenerEdgeEdit    (PropChangeEdge pclEdge);
  public void adapterEdgeEdit       (AdapterEdge edgeObj);
  public void simEvListenerEdgeEdit (SimEvListenerEdge edgeObj);

  /* menu selections */
  void copy();
  void cut();        // to remove nodes and edges
  void open();
  void openRecent();
  void paste();
  void quit();
  void save();
  void saveAs();
  
  // Bug fix: 1195
  void close();
  boolean preClose();
  void postClose();     

  void settings();

  boolean preQuit();
  void    postQuit();

  void addAssemblyFileListener   (OpenAssembly.AssyChangeListener lis);
  void removeAssemblyFileListener(OpenAssembly.AssyChangeListener lis);

  OpenAssembly.AssyChangeListener        getAssemblyChangeListener();
  DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener ();

  void generateJavaSource();
  void runAssembly();
  void initAssemblyRun();
  void export2grid();
  void captureWindow();
}
