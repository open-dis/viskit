package viskit;

import viskit.mvc.mvcAbstractController;
import viskit.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.Reader;
import java.io.IOException;

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

  public void runEventGraphEditor()
  {
    if (VGlobals.instance().getEventGraphEditor() == null)
      VGlobals.instance().buildEventGraphViewFrame();
    VGlobals.instance().runEventGraphView();
  }


  public void quit()
  //----------------
  {
    if (((AssemblyModel)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;
    VGlobals.instance().quitAssemblyEditor();

  }

  File lastFile;
  public void open()
  {
    if (((AssemblyModel)getModel()).isDirty())
      if(askToSaveAndContinue() == false)
        return;

    lastFile = ((ViskitAssemblyView) getView()).openFileAsk();
    if (lastFile != null) {
      ((ViskitAssemblyModel) getModel()).newModel(lastFile);
      ((ViskitAssemblyView) getView()).fileName(lastFile.getName());
      GraphMetaData gmd = ((ViskitAssemblyModel) getModel()).getMetaData();
      ((ViskitAssemblyView) getView()).setStopTime(gmd.stopTime);
      ((ViskitAssemblyView) getView()).setVerbose(gmd.verbose);
    }

  }
  public void save()
  //----------------
  {
    if(lastFile == null)
      saveAs();
    else {
      updateGMD();
      ((ViskitAssemblyModel)getModel()).saveModel(lastFile);
    }
  }

  public void saveAs()
  {
    lastFile = ((ViskitAssemblyView)getView()).saveFileAsk(((ViskitAssemblyModel)getModel()).getMetaData().name);
    if(lastFile != null) {
      updateGMD();
      ((ViskitAssemblyModel)getModel()).saveModel(lastFile);
      ((ViskitAssemblyView)getView()).fileName(lastFile.getName());
    }

  }
  private void updateGMD()
  {
    GraphMetaData gmd = ((ViskitAssemblyModel)getModel()).getMetaData();
    gmd.stopTime = ((ViskitAssemblyView)getView()).getStopTime();
    gmd.verbose = ((ViskitAssemblyView)getView()).getVerbose();
    ((ViskitAssemblyModel)getModel()).changeMetaData(gmd);

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
    String shortname = "evgr_";
    if(typeName.lastIndexOf('.') != -1)
      shortname = typeName.substring(typeName.lastIndexOf('.')+1) + "_";
    shortname = shortname + egNodeCount++;
    ((viskit.model.AssemblyModel) getModel()).newEventGraph(shortname, typeName, p);
  }

  public void newPropChangeListenerNode(String name, Point p)
  {
    String shortname = "lstnr_";
    if(name.lastIndexOf('.') != -1)
      shortname = name.substring(name.lastIndexOf('.')+1) + "_";

    shortname = shortname + egNodeCount++; // use same counter
    ((viskit.model.AssemblyModel)getModel()).newPropChangeListener(shortname,name,p);
  }
  /**
   *
   * @return true = continue, false = don't (i.e., we cancelled)
   */
    private boolean askToSaveAndContinue()
    {
      int yn = (((ViskitAssemblyView) getView()).genericAsk("Question", "Save current assembly?"));

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

  public void newAdapterArc(Object[]nodes)
  {
    Object oA = ((DefaultGraphCell)nodes[0]).getUserObject();
    Object oB = ((DefaultGraphCell)nodes[1]).getUserObject();
    if(!(oA instanceof EvGraphNode) || !(oB instanceof EvGraphNode))
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection", "Both nodes must be instances of Event Graphs.");
    else {
      AdapterEdge ae = ((ViskitAssemblyModel)getModel()).newAdapterEdge((EvGraphNode)oA,(EvGraphNode)oB);
      // edit right away
      if(ae != null)     // shouldn't happen
        adapterEdgeEdit(ae);
    }
  }
  public void newSimEvListArc(Object[]nodes)
  {
    Object oA = ((DefaultGraphCell)nodes[0]).getUserObject();
    Object oB = ((DefaultGraphCell)nodes[1]).getUserObject();
    if(!(oA instanceof EvGraphNode) || !(oB instanceof EvGraphNode))
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection", "Both nodes must be instances of Event Graphs.");
    else
    ((ViskitAssemblyModel)getModel()).newSimEvLisEdge((EvGraphNode)oA,(EvGraphNode)oB);
  }
  public void newPropChangeListArc(Object[]nodes)
  {
    // One and only one has to be a prop change listener
    Object oA = ((DefaultGraphCell)nodes[0]).getUserObject();
    Object oB = ((DefaultGraphCell)nodes[1]).getUserObject();

    PropChangeEdge pce = null;
    if(oA instanceof PropChangeListenerNode && !(oB instanceof PropChangeListenerNode)) {
      pce = ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oB,(PropChangeListenerNode)oA);
    }
    else if(oB instanceof PropChangeListenerNode && !(oA instanceof PropChangeListenerNode)) {
      pce = ((ViskitAssemblyModel)getModel()).newPclEdge((EvGraphNode)oA,(PropChangeListenerNode)oB);
    }
    else
      ((ViskitAssemblyView)getView()).genericErrorReport("Incompatible connection","One of the two nodes must be an instance of a PropertyChangeListener.");
    // edit right away
    if(pce != null)
      pcListenerEdgeEdit(pce);
  }

  public void pcListenerEdit(PropChangeListenerNode pclNode)
  //---------------------------------------
  {
    boolean modified = ((ViskitAssemblyView) getView()).doEditPclNode(pclNode);
    if (modified) {
      ((ViskitAssemblyModel) getModel()).changePclNode(pclNode);
    }
  }

  public void evGraphEdit(EvGraphNode evNode)
  {
    boolean modified = ((ViskitAssemblyView)getView()).doEditEvGraphNode(evNode);
    if(modified) {
      ((ViskitAssemblyModel)getModel()).changeEvGraphNode(evNode);
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
  private Vector selectionVector = new Vector();

  public void selectNodeOrEdge(Vector v)
  //------------------------------------
  {
    selectionVector = v;
    boolean ccbool = (selectionVector.size() > 0 ? true : false);
    ActionIntrospector.getAction(this, "copy").setEnabled(ccbool);
    ActionIntrospector.getAction(this, "cut").setEnabled(ccbool);
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
  int copyCount=0;
  public void paste()
  //-----------------
  {
    if (copyVector.size() <= 0)
      return;
    int x=100,y=100; int n=0;
    // We only paste un-attached nodes (at first)
    for(Iterator itr = copyVector.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof AssemblyEdge)
        continue;
      if(o instanceof EvGraphNode) {
        String nm = ((EvGraphNode)o).getName();
        String typ = ((EvGraphNode)o).getType();
        ((ViskitAssemblyModel)getModel()).newEventGraph(nm+"-copy"+copyCount++,typ,new Point(x+(20*n),y+(20*n)));
      }
      else if (o instanceof PropChangeListenerNode) {
        String nm = ((PropChangeListenerNode)o).getName();
        String typ = ((PropChangeListenerNode)o).getType();
        ((ViskitAssemblyModel)getModel()).newPropChangeListener(nm+"-copy"+copyCount++,typ,new Point(x+(20*n),y+(20*n)));
      }
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
        if(o instanceof EvGraphNode || o instanceof PropChangeListenerNode)
          nodeCount++;
        String s = o.toString();
        s = s.replace('\n', ' ');
        msg += ", \n" + s;
      }
      String specialNodeMsg = (nodeCount > 0 ? "\n(All unselected but attached edges will also be deleted.)" : "");
      if (((ViskitAssemblyView) getView()).genericAsk("Delete element(s)?", "Confirm remove" + msg + "?" + specialNodeMsg)
       == JOptionPane.YES_OPTION) {
        // do edges first?
        Vector localV = (Vector) selectionVector.clone();   // avoid concurrent update
        for (Iterator itr = localV.iterator(); itr.hasNext();) {
          Object elem = itr.next();
          if(elem instanceof AssemblyEdge) {
            killEdge((AssemblyEdge)elem);
          }
          else if(elem instanceof EvGraphNode) {
            EvGraphNode en = (EvGraphNode)elem;
            for (Iterator it2 = en.getConnections().iterator(); it2.hasNext();) {
              AssemblyEdge ed = (AssemblyEdge) it2.next();
              killEdge(ed);
            }
            ((ViskitAssemblyModel) getModel()).deleteEvGraphNode(en);
          }
          else if(elem instanceof PropChangeListenerNode) {
            PropChangeListenerNode en = (PropChangeListenerNode)elem;
            for (Iterator it2 = en.getConnections().iterator(); it2.hasNext();) {
              AssemblyEdge ed = (AssemblyEdge) it2.next();
              killEdge(ed);
            }
            ((ViskitAssemblyModel) getModel()).deletePCLNode(en);
          }
        }
      }
    }
  }

  private void killEdge(AssemblyEdge e)
  {
    if (e instanceof AdapterEdge)
      ((ViskitAssemblyModel) getModel()).deleteAdapterEdge((AdapterEdge) e);
    else if (e instanceof PropChangeEdge)
      ((ViskitAssemblyModel) getModel()).deletePropChangeEdge((PropChangeEdge) e);
    else if (e instanceof SimEvListenerEdge)
      ((ViskitAssemblyModel) getModel()).deleteSimEvLisEdge((SimEvListenerEdge) e);
  }

  public void generateJavaClass()
  {
    String source = produceJavaClass();
    if(source != null && source.length() > 0)
      ((ViskitAssemblyView)getView()).showAndSaveSource(source);
  }

  private String produceJavaClass()
  {
    if(((ViskitAssemblyModel)getModel()).isDirty()) {
      int ret = JOptionPane.showConfirmDialog(null,"The model will be saved.\nContinue?","Confirm",JOptionPane.YES_NO_OPTION);
      if(ret != JOptionPane.YES_OPTION)
        return null;

      this.saveAs();
    }

    return ((ViskitAssemblyModel)getModel()).buildJavaSource();
  }
  String packageFromLastCompile;

  public File compileJavaClass()
  {
    String source = produceJavaClass();
    if(source != null && source.length() > 0) {
       Pattern p = Pattern.compile("package.*;");
      Matcher m = p.matcher(source);
      if(m.find()){
        String nuts = m.group();
        if(nuts.endsWith(";"))
          nuts=nuts.substring(0,nuts.length()-1);

        String[] sa = nuts.split("\\s");
        packageFromLastCompile = sa[1];
      }
      return ((ViskitAssemblyModel)getModel()).compileJavaClass(source);
    }
    return null;
  }
  public void runAssembly()
  {
    File f = compileJavaClass();
    if(f != null) {
      String clNam = f.getName().substring(0,f.getName().indexOf('.'));
      clNam = packageFromLastCompile + "." + clNam;
      String classPath = System.getProperty("java.class.path");
      String sourceDir = f.getParent();
      if(classPath.indexOf(sourceDir) == -1)
        classPath = sourceDir + Vstatics.getPathSeparator() + classPath;

      String[] execStrings = buildExecStrings(clNam,classPath);
      Process proc = null;
      try {
        proc = Runtime.getRuntime().exec(execStrings);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    else
      JOptionPane.showMessageDialog(null,"Error on compile");
  }
  private String[] buildExecStrings(String className, String classPath)
  {
    Vector v = new Vector();
    String fsep = Vstatics.getFileSeparator();

     StringBuffer sb = new StringBuffer();
     sb.append(System.getProperty("java.home"));
     sb.append(fsep+"bin"+fsep+"java");
    v.add(sb.toString());

    v.add("-cp");
    v.add(classPath);
    v.add("viskit.ExternalAssemblyRunner");
    v.add(className);

    //todo get global defaultVerbose boolean from graphmetadata
    v.add("true");
    //todo get stoptime from graphmetadata
    v.add("22.2");
    // todo add names of entities which should be defaultVerbose
    //v.add("arrival");
    // etc
    String[] ra = new String[v.size()];
    return (String[])v.toArray(ra);
  }

  public void xvcrPlay()
  {
    File f = compileJavaClass();
    if(f != null) {
      String clNam = f.getName().substring(0,f.getName().indexOf('.'));
      clNam = packageFromLastCompile + "." + clNam;
      //((ViskitAssemblyModel) getModel()).startAssemblyRun(clNam,f.getParent());
      AssemblyRunner runner = new AssemblyRunner(clNam);
      String cp = runner.getClasspath();
      String pathSep = System.getProperty("path.separator");
      runner.setClasspath(f.getParent()+pathSep+cp);

      new RunWindow((JFrame)getView(),runner).setVisible(true);   // blocks
    }
    else
      JOptionPane.showMessageDialog(null,"Error on compile");
  }
  public void xvcrPause()
  {
    System.out.println("vcrRewind");
  }
  public void xvcrStop()
  {
    System.out.println("vcrStop");
  }
  public void xvcrStep()
  {
    System.out.println("vcrStep");
  }
}
