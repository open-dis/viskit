package viskit.jgraph;

import org.jgraph.JGraph;
import org.jgraph.graph.*;
import viskit.model.*;
import viskit.model.Edge;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Map;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * User: mike
 * Date: Feb 23, 2004
 * Time: 1:21:52 PM
 */
public class vGraphAssemblyModel extends DefaultGraphModel
{
  public JGraph graph; // fix this
  public vGraphAssemblyModel()
  {
    initViskitStyle();
  }

  Map viskitAssyAdapterEdgeStyle;
  Map viskitAssyPclEdgeStyle;
  Map viskitAssySimEvLisEdgeStyle;

  public void initViskitStyle() {

    viskitAssyAdapterEdgeStyle = GraphConstants.createMap();

    GraphConstants.setDisconnectable(viskitAssyAdapterEdgeStyle,false);
    GraphConstants.setLineEnd  (viskitAssyAdapterEdgeStyle,GraphConstants.ARROW_TECHNICAL);
    GraphConstants.setEndFill  (viskitAssyAdapterEdgeStyle, true);
    GraphConstants.setEndSize  (viskitAssyAdapterEdgeStyle, 10);
    GraphConstants.setFont       (viskitAssyAdapterEdgeStyle, GraphConstants.defaultFont.deriveFont(10));
    GraphConstants.setBendable   (viskitAssyAdapterEdgeStyle, true);
    GraphConstants.setLineStyle  (viskitAssyAdapterEdgeStyle, GraphConstants.STYLE_BEZIER);
    GraphConstants.setLineWidth  (viskitAssyAdapterEdgeStyle, 2.0f);
    GraphConstants.setOpaque     (viskitAssyAdapterEdgeStyle, true);
    GraphConstants.setBackground (viskitAssyAdapterEdgeStyle, new Color(255,255,255,180));
    //GraphConstants.setBorderColor(viskitAssyAdapterEdgeStyle, Color.red);
    GraphConstants.setForeground (viskitAssyAdapterEdgeStyle, Color.black);
    GraphConstants.setRouting    (viskitAssyAdapterEdgeStyle, new ViskitAssemblyRouting());
    GraphConstants.setLineColor  (viskitAssyAdapterEdgeStyle, Color.lightGray); //new Color(0xA2,0xD9,0x9F));              // green

    viskitAssyPclEdgeStyle = GraphConstants.createMap();
    viskitAssyPclEdgeStyle.putAll(viskitAssyAdapterEdgeStyle);

    //GraphConstants.setDashPattern(viskitAssyPclEdgeStyle, new float[] { 3, 3 });
    GraphConstants.setLineColor(viskitAssyPclEdgeStyle,new Color(0xff,0xc8,0xc8));
    GraphConstants.setLineWidth(viskitAssyPclEdgeStyle,1.5f);


    viskitAssySimEvLisEdgeStyle = GraphConstants.createMap();
    viskitAssySimEvLisEdgeStyle.putAll(viskitAssyAdapterEdgeStyle);

    GraphConstants.setLineColor(viskitAssySimEvLisEdgeStyle,Color.black);
    GraphConstants.setLineWidth(viskitAssySimEvLisEdgeStyle, 1.0f);
  }

  public void changeEvent(EventNode en)
  {
    CircleCell c = (CircleCell)en.opaqueViewObject;
    c.setUserObject(en);

    graph.getUI().stopEditing(graph);
    graph.graphDidChange(); // jmb try...yes, I thought the stopEditing would do the same thing
  }


  public void deleteAll()
  {
    //remove(getRoots(this));
    Object[] roots = getRoots(this);
    for(int i=0;i<roots.length;i++) {
      if( roots[i] instanceof CircleCell) {
        Object[] child = new Object[1];
        child[0] = ((CircleCell)roots[i]).getFirstChild();
        remove(child);
      }
    }
    remove(roots);
  }

/*
  public void deleteEventNode(EventNode en)
  {
    DefaultGraphCell c = (DefaultGraphCell)en.opaqueViewObject;
    c.removeAllChildren();
    this.remove(new Object[]{c});
  }
*/
  public void deleteEGNode(EvGraphNode egn)
  {
    DefaultGraphCell c = (DefaultGraphCell)egn.opaqueViewObject;
    c.removeAllChildren();
    this.remove(new Object[]{c});
  }
  public void addEGNode(EvGraphNode egn)
  {
    DefaultGraphCell c = new AssemblyCircleCell(egn.getName());
    egn.opaqueViewObject = c;
    c.setUserObject(egn);
    Map attributes = new Hashtable();
    attributes.put(c,createBounds(egn.getPosition().x,egn.getPosition().y,Color.black));
    c.add(new vAssemblyPortCell(egn.getName()+"/Center"));
    this.insert(new Object[]{c},attributes,null,null,null);
  }
  
  public void changeEGNode(EvGraphNode egn)
  {
    AssemblyCircleCell c = (AssemblyCircleCell)egn.opaqueViewObject;
    c.setUserObject(egn);

    graph.getUI().stopEditing(graph);
    graph.graphDidChange(); // jmb try...yes, I thought the stopEditing would do the same thing

  }

  public void addPCLNode(PropChangeListenerNode pcln)
  {
    DefaultGraphCell c = new AssemblyPropListCell(pcln.getName());
    pcln.opaqueViewObject = c;
    c.setUserObject(pcln);
    Map attributes = new Hashtable();
    attributes.put(c,createBounds(pcln.getPosition().x,pcln.getPosition().y,Color.black));
    c.add(new vAssemblyPortCell(pcln.getName()+"/Center"));
    this.insert(new Object[]{c},attributes,null,null,null);

  }
  public void deletePCLNode(PropChangeListenerNode pcln)
  {
    DefaultGraphCell c = (DefaultGraphCell)pcln.opaqueViewObject;
    c.removeAllChildren();
    this.remove(new Object[]{c});

  }
  public void changePCLNode(PropChangeListenerNode pcln)
  {
    AssemblyPropListCell c = (AssemblyPropListCell)pcln.opaqueViewObject;
    c.setUserObject(pcln);

    graph.getUI().stopEditing(graph);
    graph.graphDidChange(); // jmb try...yes, I thought the stopEditing would do the same thing


  }
  public void addAdapterEdge(AdapterEdge ae)
  {
    Object frO = ae.from;
    Object toO = ae.to;
    DefaultGraphCell from,to;
    if(frO instanceof EvGraphNode) {
      from = (DefaultGraphCell)((EvGraphNode)frO).opaqueViewObject;
    }
    else {
      from = (DefaultGraphCell)((PropChangeListenerNode)frO).opaqueViewObject;
    }
    if(toO instanceof EvGraphNode) {
      to = (DefaultGraphCell)((EvGraphNode)toO).opaqueViewObject;
    }
    else {
      to = (DefaultGraphCell)((PropChangeListenerNode)toO).opaqueViewObject;
    }

    vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
    ae.opaqueViewObject = edge;
    edge.setUserObject(ae);
    ConnectionSet cs = new ConnectionSet();
    cs.connect(edge,from.getChildAt(0),to.getChildAt(0));
    Map attributes = new Hashtable();
    attributes.put(edge,this.viskitAssyAdapterEdgeStyle);

    this.insert(new Object[]{edge},attributes,cs,null,null);

  }
  public void deleteAdapterEdge(AdapterEdge ae)
  {

  }
  public void changeAdapterEdge(AdapterEdge ae)
  {

  }
  public void addSimEvListEdge(SimEvListenerEdge sele)
  {
    Object frO = sele.from;
    Object toO = sele.to;
    DefaultGraphCell from,to;
    if(frO instanceof EvGraphNode) {
      from = (DefaultGraphCell)((EvGraphNode)frO).opaqueViewObject;
    }
    else {
      from = (DefaultGraphCell)((PropChangeListenerNode)frO).opaqueViewObject;
    }
    if(toO instanceof EvGraphNode) {
      to = (DefaultGraphCell)((EvGraphNode)toO).opaqueViewObject;
    }
    else {
      to = (DefaultGraphCell)((PropChangeListenerNode)toO).opaqueViewObject;
    }

    vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
    sele.opaqueViewObject = edge;
    edge.setUserObject(sele);
    ConnectionSet cs = new ConnectionSet();
    cs.connect(edge,from.getChildAt(0),to.getChildAt(0));
    Map attributes = new Hashtable();
    attributes.put(edge,this.viskitAssySimEvLisEdgeStyle);

    this.insert(new Object[]{edge},attributes,cs,null,null);
  }
  public void deleteSimEvListEdge(SimEvListenerEdge sele)
  {

  }
  public void changeSimEvListEdge(SimEvListenerEdge sele)
  {

  }
  public void deletePclEdge(PropChangeEdge pce)
  {

  }
  public void changePclEdge(PropChangeEdge pce)
  {

  }
/*
  public void addEventNode(EventNode en)
  {
    DefaultGraphCell c = new CircleCell(en.getName());
    en.opaqueViewObject = c;
    c.setUserObject(en);

    Map attributes = new Hashtable();
    attributes.put(c,createBounds(en.getPosition().x, en.getPosition().y,Color.black));
    //attributes.put(c,createBounds(p.x,p.y,Color.black)); // color a nop?
    
    //c.add(new DefaultPort(en.getName()+"/Center"));
    c.add(new vPortCell(en.getName()+"/Center"));
    this.insert(new Object[]{c},attributes,null,null,null);
  }
*/
/*
  public void deleteEdge(SchedulingEdge edge)
  {
    DefaultEdge e = (DefaultEdge)edge.opaqueViewObject;
    this.remove(new Object[]{e});
  }

  public void deleteCancellingEdge(CancellingEdge edge)
  {
    DefaultEdge e = (DefaultEdge)edge.opaqueViewObject;
    this.remove(new Object[]{e});
  }
  public void changeEdge(SchedulingEdge ed)
  {
    changeEitherEdge(ed);
  }
  public void changeCancellingEdge(CancellingEdge ed)
  {
    changeEitherEdge(ed);
  }

  // I don't think this is required anymore.  We don't change src/targets...we rebuild the edge
  private void changeEitherEdge(Edge ed)
  {
    CircleCell newFromCC = (CircleCell)ed.from.opaqueViewObject;
    vEdgeCell  edgeC = (vEdgeCell)ed.opaqueViewObject;

    DefaultPort dpFrom = (DefaultPort)edgeC.getSource();
    Object dpCC = dpFrom.getParent();
    if(dpCC == newFromCC)
      return; // no change
    edgeC.setSource(edgeC.getTarget());
    edgeC.setTarget(dpFrom);

    graph.getUI().stopEditing(graph);    // this does it, but label is screwed
    graph.graphDidChange(); // needed for redraw

  }
  */
  public void addPclEdge(PropChangeEdge pce)
  {
    EvGraphNode egn = (EvGraphNode)pce.from;
    PropChangeListenerNode pcln = (PropChangeListenerNode)pce.to;
    DefaultGraphCell from = (DefaultGraphCell)egn.opaqueViewObject;
    DefaultGraphCell to = (DefaultGraphCell)pcln.opaqueViewObject;
    vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
    pce.opaqueViewObject = edge;
    edge.setUserObject(pce);
    ConnectionSet cs = new ConnectionSet();
    cs.connect(edge,from.getChildAt(0),to.getChildAt(0));
    Map attributes = new Hashtable();
    attributes.put(edge,this.viskitAssyPclEdgeStyle);

    this.insert(new Object[]{edge},attributes,cs,null,null);
  }
  /*
  public void addEdge(SchedulingEdge se)
  {
    _addEdgeCommon(se,viskitAssyAdapterEdgeStyle);
  }
  public void addCancelEdge(CancellingEdge ce)
  {
    _addEdgeCommon(ce,viskitAssyPclEdgeStyle);
  }
  private void _addEdgeCommon(Edge ed, Map edgeStyle)
  {
    EventNode enfrom = ed.from;
    EventNode ento   = ed.to;
    DefaultGraphCell from = (DefaultGraphCell)enfrom.opaqueViewObject;
    DefaultGraphCell to   = (DefaultGraphCell)ento.opaqueViewObject;

    DefaultEdge edge = null;
    if(enfrom == ento)
      edge = new vSelfEdgeCell(null);
    else
      edge = new vEdgeCell(null);

    ed.opaqueViewObject = edge;
    edge.setUserObject(ed);
    ConnectionSet cs = new ConnectionSet();
    cs.connect(edge, from.getChildAt(0), to.getChildAt(0));

    Map attributes = new Hashtable();

    attributes.put(edge, edgeStyle);

    if(enfrom == ento) {// self referential overwrite
      if(ed instanceof SchedulingEdge)
        attributes.put(edge,this.viskitAssySimEvLisEdgeStyle);
      else
        attributes.put(edge,this.viskitSelfRefCancel);
    }

    this.insert(new Object[]{edge},attributes,cs,null,null);

  }
*/
  public Map createBounds(int x, int y, Color c) {
    Map map = GraphConstants.createMap();
    GraphConstants.setBounds(map, new Rectangle(x, y, 54, 54)); //90, 30));
    GraphConstants.setBorder(map, BorderFactory.createRaisedBevelBorder());
    GraphConstants.setBackground(map, c.darker());
    GraphConstants.setForeground(map, Color.white);
    GraphConstants.setFont(map, GraphConstants.defaultFont.deriveFont(Font.BOLD, 12));
    GraphConstants.setOpaque(map, true);
    return map;
  }
}
