package viskit;

import viskit.mvc.mvcAbstractController;
import viskit.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

import actions.ActionIntrospector;
import org.jgraph.graph.DefaultGraphCell;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:26:02 AM
 */

public class AssemblyController extends mvcAbstractController implements ViskitAssemblyController
{
  public void begin()
  //-----------------
  {
    //newEventGraph();
  }

  public void quit()
  //----------------
  {
    
  }

  public void save()
  //----------------
  {
    // todo implement
  }
  public void newAssembly()
  {
    if (((AssemblyModel)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;

    ((ViskitAssemblyModel) getModel()).newModel(null);
    editGraphMetaData();

  }

  private int egNodeCount=0;
  public void newEventGraphNode(String typeName, Point p)
  {
    System.out.println("new eg node "+typeName+" at "+p.x+" "+p.y);
    String fauxName = "evgr_" + egNodeCount++;
    ((viskit.model.AssemblyModel) getModel()).newEventGraph(fauxName, typeName, p);

  }

  public void newPropChangeListenerNode(String name, Point p)
  {
    System.out.println("new prop change node "+name+" at "+p.x+" "+p.y);
    String fauxName = "lstnr_"+egNodeCount++; // use same counter
    ((viskit.model.AssemblyModel)getModel()).newPropChangeListener(fauxName,name,p);

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
          if(((AssemblyModel)getModel()).isDirty())
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

    public void editGraphMetaData()
    //--------------------------
    {
      GraphMetaData gmd = ((ViskitAssemblyModel)getModel()).getMetaData();
      boolean modified = AssemblyMetaDataDialog.showDialog((AssemblyViewFrame)getView(),(AssemblyViewFrame)getView(),gmd);
      if(modified)
        ((ViskitAssemblyModel)getModel()).changeMetaData(gmd);
    }
  Vector selectionVector;
  public void selectNodeOrEdge(Vector v)
  //------------------------------------
  {
    selectionVector = v;
    boolean ccbool = (selectionVector.size() > 0 ? true : false);
// todo   ActionIntrospector.getAction(this, "copy").setEnabled(ccbool);
//    ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
//    ActionIntrospector.getAction(this, "newSelfRefEdge").setEnabled(ccbool);
  }
  public void newAdapterArc(Object[]nodes)
  {
    ((ViskitAssemblyModel)getModel()).newAdapterEdge(((DefaultGraphCell)nodes[0]).getUserObject(),
                                                     ((DefaultGraphCell)nodes[1]).getUserObject());
  }
  public void newSimEvListArc(Object[]nodes)
  {
    ((ViskitAssemblyModel)getModel()).newSimEvLisEdge(((DefaultGraphCell)nodes[0]).getUserObject(),
                                                      ((DefaultGraphCell)nodes[1]).getUserObject()); 
  }
  public void newPropChangeListArc(Object[]nodes)
  {
    // One has to be a prop change listener
    Object oA = ((DefaultGraphCell)nodes[0]).getUserObject();
    Object oB = ((DefaultGraphCell)nodes[1]).getUserObject();
    if(oA instanceof PropChangeListenerNode && !(oB instanceof PropChangeListenerNode)) {
      ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oB,(PropChangeListenerNode)oA);
      return;
    }
    if(oB instanceof PropChangeListenerNode && !(oA instanceof PropChangeListenerNode)) {
      ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oA,(PropChangeListenerNode)oB);
      return;
    }

    ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","One of the two nodes must be an instance of a PropertyChangeListener.");
  }
/*
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
*/

}
