package viskit;

import edu.nps.util.DirectoryWatch;
import viskit.model.*;

import java.awt.*;
import java.util.Vector;

/**
 * User: mike
 * Date: Mar 19, 2004
 * Time: 9:00:57 AM
 */

public interface ViskitController
{
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

  /* requests to the controller to perform editing operations on existing entities */
  void nodeEdit         (EventNode node);
  void arcEdit          (SchedulingEdge ed);
  void canArcEdit       (CancellingEdge ed);
  void simParameterEdit (vParameter param);
  void stateVariableEdit(vStateVariable var);

  /* menu selections */
  void copy();
  void cut();        // to remove nodes and edges
  void open();
  void openRecent();
  void close();
  void closeAll();
  void paste();
  void quit();
  void save();
  void saveAs();
  void selectNodeOrEdge(Vector v);

  void deleteSimParameter(vParameter p);
  void deleteStateVariable(vStateVariable var);

  void eventList();
  void generateJavaClass();
  void captureWindow();

  void addOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis);
  void removeOpenEventGraphListener(DirectoryWatch.DirectoryChangeListener lis) ;
}
