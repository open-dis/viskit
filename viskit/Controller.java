package viskit;

import actions.ActionIntrospector;
import org.jgraph.graph.DefaultGraphCell;
import viskit.model.*;
import viskit.mvc.mvcAbstractController;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 12:52:59 PM
 */

/**
 * This is the MVC controller for the Viskit app.  All user inputs come here, and this
 * code decides what to do about it.  To add new events:
 * 1 add a new public Action BLAH field
 * 2 instantiate it in the constructor, mapping it to a handler (name)
 * 3 write the handler
 */

public class Controller extends mvcAbstractController implements ViskitController
/*******************************************************************************/
{
  public Controller()
  //=================
  {
  }
  public void begin()
  //-----------------
  {
    // wait for Main to do this after the first window is put up
    // newEventGraph();
  }

  public void quit()
  //----------------
  {
    if (((Model)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;
    VGlobals.instance().quitEventGraphEditor();
  }

  public void newEventGraph()
  //-------------------------
  {
    if (((Model)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;

    lastFile = null;
    ((ViskitModel) getModel()).newModel(null);

    editGraphMetaData();
  }

/**
 *
 * @return true = continue, false = don't (i.e., we cancelled)
 */
  private boolean askToSaveAndContinue()
  {
    int yn = (((ViskitView) getView()).genericAsk("Question", "Save current graph?"));

    switch (yn) {
      case JOptionPane.YES_OPTION:
        save();
        if(((Model)getModel()).isDirty())
          return false; // we cancelled
       // else
        return true;
        //break;
      case JOptionPane.NO_OPTION:
        return true;
        //break;
      case JOptionPane.CANCEL_OPTION:
      default:
        return false;
    }
  }

  File lastFile;
  public void open()
  //----------------
  {
    if (((Model)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;

    lastFile = ((ViskitView) getView()).openFileAsk();
    if (lastFile != null) {
      ((ViskitModel) getModel()).newModel(lastFile);
      ((ViskitView) getView()).fileName(lastFile.getName());
    }
  }

  public void save()
  //----------------
  {
    if(lastFile == null)
      saveAs();
    else
      ((ViskitModel)getModel()).saveModel(lastFile);
  }

  public void saveAs()
  //------------------
  {
    lastFile = ((ViskitView)getView()).saveFileAsk(((ViskitModel)getModel()).getMetaData().name);
    if(lastFile != null) {
      ((ViskitModel)getModel()).saveModel(lastFile);
      ((ViskitView)getView()).fileName(lastFile.getName());
    }
  }

  public void newSimParameter()
  //------------------------
  {
    ((ViskitView) getView()).addParameterDialog();

  }
  public void newSimParameter(String name, String type, String initVal, String comment)
  {
    ((ViskitModel)getModel()).newSimParameter(name, type, initVal, comment);
  }

  public void simParameterEdit(vParameter param)
  {
    boolean modified = ((ViskitView) getView()).doEditParameter(param);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeSimParameter(param);
    }
  }

  public void stateVariableEdit(vStateVariable var)
  {
    boolean modified = ((ViskitView) getView()).doEditStateVariable(var);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeStateVariable(var);
    }
  }
  
  public void newStateVariable()
  {
    ((ViskitView) getView()).addStateVariableDialog();
  }
  public void newStateVariable(String name, String type, String initVal, String comment)
  //----------------------------
  {
    ((viskit.model.ViskitModel)getModel()).newStateVariable(name,type,initVal,comment);
  }

  private Vector selectionVector = new Vector();

  public void selectNodeOrEdge(Vector v)
  //------------------------------------
  {
    selectionVector = v;
    boolean ccbool = (selectionVector.size() > 0 ? true : false);
    ActionIntrospector.getAction(this, "copy").setEnabled(ccbool);
    ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
    ActionIntrospector.getAction(this, "newSelfRefEdge").setEnabled(ccbool);
  }

  private Vector copyVector = new Vector();

  public void copy()
  //----------------
  {
    if (selectionVector.size() <= 0)
      return;
    copyVector = (Vector) selectionVector.clone();
    ActionIntrospector.getAction(this,"paste").setEnabled(true);
  }

  public void paste()
  //-----------------
  {
    if (copyVector.size() <= 0)
      return;
    int x=100,y=100; int n=0;
    // We only paste un-attached nodes (at first)
    for(Iterator itr = copyVector.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof Edge)
        continue;
      String nm = ((viskit.model.EventNode)o).getName();
      ((viskit.model.ViskitModel) getModel()).newEvent(nm+"-copy", new Point(x+(20*n),y+(20*n)));
      n++;
    }
  }

  public void cut()
  //---------------
  {
    if (selectionVector != null && selectionVector.size() > 0) {
      // first ask:
      String msg = "";
      int nodeCount = 0;  // different msg for edge delete
      for (Iterator itr = selectionVector.iterator(); itr.hasNext();) {
        Object o = itr.next();
        if(o instanceof EventNode)
          nodeCount++;
        String s = o.toString();
        s = s.replace('\n', ' ');
        msg += ", \n" + s;
      }
      String specialNodeMsg = (nodeCount > 0 ? "\n(All unselected but attached edges will also be deleted.)" : "");
      if (((ViskitView) getView()).genericAsk("Delete element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg)
       == JOptionPane.YES_OPTION) {
        // do edges first?
        Vector localV = (Vector) selectionVector.clone();   // avoid concurrent update
        for (Iterator itr = localV.iterator(); itr.hasNext();) {
          Object elem = itr.next();
          if(elem instanceof Edge) {
            killEdge((Edge)elem);
          }
          else if(elem instanceof EventNode) {
            EventNode en = (EventNode)elem;
            for (Iterator it2 = en.getConnections().iterator(); it2.hasNext();) {
              Edge ed = (Edge) it2.next();
              killEdge(ed);
            }
            ((ViskitModel) getModel()).deleteEvent(en);
          }
        }
      }
    }
  }

  private void killEdge(Edge e)
  {
    if (e instanceof SchedulingEdge)
      ((ViskitModel) getModel()).deleteEdge((SchedulingEdge) e);
    else
      ((ViskitModel) getModel()).deleteCancelEdge((CancellingEdge) e);
  }

  public void deleteSimParameter(vParameter p)
  {
    ((ViskitModel) getModel()).deleteSimParameter(p);    
  }

  public void deleteStateVariable(vStateVariable var)
  {
    ((ViskitModel)getModel()).deleteStateVariable(var);
  }

  private boolean checkSave()
  {
    if(((ViskitModel)getModel()).isDirty() || lastFile == null) {
      int ret = JOptionPane.showConfirmDialog(null,"The model will be saved.\nContinue?","Confirm",JOptionPane.YES_NO_OPTION);
      if(ret != JOptionPane.YES_OPTION)
        return false;
      this.saveAs();
    }
    return true;
  }
  public void generateJavaClass()
  {
    if(checkSave() == false || lastFile == null)
      return;
    String source = ((ViskitModel)getModel()).buildJavaSource();
    if(source != null && source.length() > 0)
      ((ViskitView)getView()).showAndSaveSource(source);
  }

  public void showXML()
  {
    if(checkSave() == false || lastFile == null)
      return;

    ((ViskitView)getView()).displayXML(lastFile); 
  }

  public void runAssemblyEditor()
  {
    if (VGlobals.instance().getAssemblyEditor() == null)
      VGlobals.instance().buildAssemblyViewFrame();
    VGlobals.instance().runAssemblyView();
  }

  public void eventList()
  {
    // todo implement
    System.out.println("EventListAction in " + this);
  }

  private int nodeCount = 0;
  public void newNode()
  {
    newNode(new Point(100,100));
  }
  public void newNode(Point p)
  //--------------------------
  {
    String fauxName = "evnt_" + nodeCount++;
    ((viskit.model.ViskitModel) getModel()).newEvent(fauxName, p);
  }

  public void newArc(Object[] nodes)
  //--------------------------------
  {
    // My node view objects hold node model objects and vice versa
    EventNode src = (EventNode) ((DefaultGraphCell) nodes[0]).getUserObject();
    EventNode tar = (EventNode) ((DefaultGraphCell) nodes[1]).getUserObject();
    ((ViskitModel) getModel()).newEdge(src, tar);
  }

  public void newCancelArc(Object[] nodes)
  //--------------------------------------
  {
    // My node view objects hold node model objects and vice versa
    EventNode src = (EventNode) ((DefaultGraphCell) nodes[0]).getUserObject();
    EventNode tar = (EventNode) ((DefaultGraphCell) nodes[1]).getUserObject();
    ((ViskitModel) getModel()).newCancelEdge(src, tar);
  }

  public void newSelfRefEdge()
  //--------------------------
  {
    if (selectionVector != null && selectionVector.size() > 0) {
      for (Iterator itr = selectionVector.iterator(); itr.hasNext();) {
        Object o = itr.next();
        if(o instanceof EventNode) {
          ((ViskitModel) getModel()).newEdge((EventNode)o,(EventNode)o);
        }
      }
    }
  }

  public void editGraphMetaData()
  //--------------------------
  {
    GraphMetaData gmd = ((ViskitModel)getModel()).getMetaData();
    boolean modified = EvGraphMetaDataDialog.showDialog((EventGraphViewFrame)getView(),(EventGraphViewFrame)getView(),gmd);
    if(modified)
      ((ViskitModel)getModel()).changeMetaData(gmd);

    // update title bar
    ((ViskitView)getView()).fileName(gmd.name);    
  }
  
  public void nodeEdit(viskit.model.EventNode node)      // shouldn't be required
  //----------------------------------
  {
    boolean modified = ((ViskitView) getView()).doEditNode(node);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeEvent(node);
    }
  }

  public void arcEdit(viskit.model.SchedulingEdge ed)
  //------------------------------------
  {
    boolean modified = ((ViskitView) getView()).doEditEdge(ed);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeEdge(ed);
    }
  }

  public void canArcEdit(viskit.model.CancellingEdge ed)
  //---------------------------------------
  {
    boolean modified = ((ViskitView) getView()).doEditCancelEdge(ed);
    if (modified) {
      ((viskit.model.ViskitModel) getModel()).changeCancelEdge(ed);
    }
  }

  public void captureWindow()
      //-------------------------
  {
    String fileName = "ViskitScreenCapture";
    if (lastFile != null)
      fileName = lastFile.getName();

    // get a unique filename
    File fil;
    String appnd = "";
    int count = -1;
    do {
      fil = new File(fileName + appnd + ".png");
      appnd = "" + ++count;
    }
    while (fil.exists());

    final Timer tim = new Timer(100,new timerCallback(fil));
    tim.setRepeats(false);
    tim.start();

  }
  class timerCallback implements ActionListener
  {
    File fil;
    timerCallback(File f)
    {
      fil = f;
    }
    public void actionPerformed(ActionEvent ev)
    {
      // create and save the image
      Component component = (Component) getView();
      Point p = new Point(0, 0);
      SwingUtilities.convertPointToScreen(p, component);
      Rectangle region = component.getBounds();
      region.x = p.x;
      region.y = p.y;
      BufferedImage image = null;
      try {
        image = new Robot().createScreenCapture(region);
        ImageIO.write(image, "png", fil);
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      // display a scaled version
      JFrame frame = new JFrame("Saved as " + fil.getName());
      ImageIcon ii = new ImageIcon(image.getScaledInstance(image.getWidth() * 50 / 100, image.getHeight() * 50 / 100, Image.SCALE_FAST));
      JLabel lab = new JLabel(ii);
      frame.getContentPane().setLayout(new BorderLayout());
      frame.getContentPane().add(lab, BorderLayout.CENTER);
      frame.pack();
      frame.setLocationRelativeTo((Component) getView());
      frame.setVisible(true);
    }
  }
}
