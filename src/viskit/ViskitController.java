package viskit;

import edu.nps.util.DirectoryWatch;
import viskit.model.*;

import java.awt.*;
import java.util.Vector;

/**
 * @author Mike Bailey
 * @since Mar 19, 2004
 * @since 9:00:57 AM
 * @version $Id$
 */
public interface ViskitController {
  /* start app */
  void begin();

  /* user has clicked a button or menu item: */
  void newNode();
  void newSimParameter();
  void newStateVariable();

  /* user has established some entity parameters, model can create objects */
  void buildNewNode         (Point p);
  void buildNewNode         (Point p, String name);
  void buildNewSimParameter (String name, String type, String initVal, String comment);
  void buildNewStateVariable(String name, String type, String initVal, String comment);
  void buildNewArc          (Object[] nodes);
  void buildNewCancelArc    (Object[] nodes);

  void editGraphMetaData  ();

  void newEventGraph();

  void runAssemblyEditor();
  void showXML();

  /* a component, e.g., model, wants to say something. */
  void messageUser(int typ, String msg);    // typ is one of JOptionPane types

  /* requests to the controller to perform editing operations on existing entities */
  void nodeEdit         (EventNode node);
  void arcEdit          (SchedulingEdge ed);
  void canArcEdit       (CancellingEdge ed);
  void simParameterEdit (vParameter param);
  void stateVariableEdit(vStateVariable var);
  void codeBlockEdit    (String s);
  
  /* menu selections */
  void copy();
  void cut();        // to remove nodes and edges
  
  /** Opens selected files from a FileChooser */
  void open();
  void openRecent();
  void openRecentEventGraph(String path);
  void close();
  void closeAll();
  void paste();
  void quit();
  void save();
  void saveAs();
  void selectNodeOrEdge(Vector v);

  void settings();

  boolean preClose();
  boolean preQuit();
  void    postClose();
  void    postQuit();

  void deleteSimParameter(vParameter p);
  void deleteStateVariable(vStateVariable var);

  void eventList();
  void generateJavaClass();  
  
  /** Provides a single screenshot capture capability */
  void captureWindow();
  
  void addOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis);
  void removeOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis) ;
  
  void addRecentFileListListener(RecentFileListener lis);
  void removeRecentFileListListener(RecentFileListener lis);
  java.util.List<String> getRecentFileList();
  
  public static interface RecentFileListener
  {
    public void listChanged();
  }
}
