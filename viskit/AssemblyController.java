package viskit;

import viskit.mvc.mvcAbstractController;
import viskit.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import java.io.File;

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

  /* menu selections */
  public void copy()
  {
  }

  public void generateJavaClass()
  {
    if(((ViskitAssemblyModel)getModel()).isDirty()) {
      int ret = JOptionPane.showConfirmDialog(null,"The model will be saved.\nContinue?","Confirm",JOptionPane.YES_NO_OPTION);
      if(ret != JOptionPane.YES_OPTION)
        return;
      this.saveAs();
    }

    String source = ((ViskitAssemblyModel)getModel()).buildJavaSource();
    if(source != null && source.length() > 0)
      ((ViskitAssemblyView)getView()).showAndSaveSource(source);

  }

  public void runEventGraphEditor()
  {
    if (VGlobals.instance().getEventGraphEditor() == null)
      VGlobals.instance().buildEventGraphViewFrame();
    VGlobals.instance().runEventGraphView();
  }

  public void saveAs()
  {
    lastFile = ((ViskitAssemblyView)getView()).saveFileAsk(((ViskitAssemblyModel)getModel()).getMetaData().name);
    if(lastFile != null) {
      ((ViskitAssemblyModel)getModel()).saveModel(lastFile);
      ((ViskitAssemblyView)getView()).fileName(lastFile.getName());
    }

  }

  public void quit()
  //----------------
  {
    
  }
  public void cut()
  {

  }
  public void paste()
  {

  }
  File lastFile;
  public void open()
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
    String fauxName = "evgr_" + egNodeCount++;
    ((viskit.model.AssemblyModel) getModel()).newEventGraph(fauxName, typeName, p);
  }

  public void newPropChangeListenerNode(String name, Point p)
  {
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
  public void pcListenerEdit(PropChangeListenerNode pclNode)
  //---------------------------------------
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditPclNode(pclNode);
    if (modified) {
      ((ViskitAssemblyModel) getModel()).changePclNode(pclNode);
    }
  }
  public void pcListenerEdgeEdit(PropChangeEdge pclEdge)
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditPclEdge(pclEdge);
    if (modified) {
      ((ViskitAssemblyModel) getModel()).changePclEdge(pclEdge);
    }
  }

  public void adapterEdgeEdit(AdapterEdge aEdge)
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditAdapterEdge(aEdge);
    if (modified) {
      ((ViskitAssemblyModel)getModel()).changeAdapterEdge(aEdge);
    }
  }

  public void simEvListenerEdgeEdit(SimEvListenerEdge seEdge)
  {
    boolean modified = ((ViskitAssemblyView)getView()).doEditSimEvListEdge(seEdge);
    if (modified) {
      ((ViskitAssemblyModel)getModel()).changeSimEvEdge(seEdge);
    }
  }
}
