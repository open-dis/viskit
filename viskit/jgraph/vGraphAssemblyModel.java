package viskit.jgraph;

import org.jgraph.JGraph;
import org.jgraph.graph.*;
import viskit.model.CancellingEdge;
import viskit.model.Edge;
import viskit.model.EventNode;
import viskit.model.SchedulingEdge;

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

  Map viskitEdgeStyle, viskitCancelEdgeStyle;
  Map viskitSelfRefEdge, viskitSelfRefCancel;

  public void initViskitStyle() {

    viskitEdgeStyle = GraphConstants.createMap();
    GraphConstants.setDisconnectable(viskitEdgeStyle,false);
    GraphConstants.setLineEnd  (viskitEdgeStyle,GraphConstants.ARROW_TECHNICAL);
    GraphConstants.setEndFill  (viskitEdgeStyle, true);
    GraphConstants.setEndSize  (viskitEdgeStyle, 10);
    GraphConstants.setFont       (viskitEdgeStyle, GraphConstants.defaultFont.deriveFont(10));
    GraphConstants.setBendable   (viskitEdgeStyle, true);
    GraphConstants.setLineStyle  (viskitEdgeStyle, GraphConstants.STYLE_BEZIER);
    GraphConstants.setLineWidth  (viskitEdgeStyle, 1);
    GraphConstants.setOpaque     (viskitEdgeStyle, true);
    GraphConstants.setBackground (viskitEdgeStyle, new Color(255,255,255,180));
    // comment out for no border GraphConstants.setBorderColor(viskitEdgeStyle, Color.gray);
    GraphConstants.setForeground (viskitEdgeStyle, Color.black);
    GraphConstants.setRouting    (viskitEdgeStyle, new ViskitRouting());

    viskitCancelEdgeStyle = GraphConstants.createMap();
    GraphConstants.setDisconnectable(viskitCancelEdgeStyle,false);
    GraphConstants.setLineEnd  (viskitCancelEdgeStyle,GraphConstants.ARROW_TECHNICAL);
    GraphConstants.setEndFill  (viskitCancelEdgeStyle, true);
    GraphConstants.setEndSize  (viskitCancelEdgeStyle, 10);
    GraphConstants.setFont       (viskitCancelEdgeStyle, GraphConstants.defaultFont.deriveFont(10));
    GraphConstants.setBendable   (viskitCancelEdgeStyle, true);
    GraphConstants.setLineStyle  (viskitCancelEdgeStyle, GraphConstants.STYLE_BEZIER);
    GraphConstants.setLineWidth  (viskitCancelEdgeStyle, 1);
    GraphConstants.setOpaque     (viskitCancelEdgeStyle, true);
    GraphConstants.setBackground (viskitCancelEdgeStyle, new Color(255,255,255,180));
    // comment out for no border GraphConstants.setBorderColor(viskitCancelEdgeStyle, Color.gray);
    GraphConstants.setForeground (viskitCancelEdgeStyle, Color.black);
    GraphConstants.setDashPattern(viskitCancelEdgeStyle, new float[] { 3, 3 });
    GraphConstants.setRouting    (viskitCancelEdgeStyle, new ViskitRouting());

    viskitSelfRefEdge = GraphConstants.createMap();
    viskitSelfRefEdge.putAll    (viskitEdgeStyle);
    GraphConstants.setLineStyle (viskitSelfRefEdge, GraphConstants.STYLE_ORTHOGONAL);
    //GraphConstants.setLineEnd   (viskitSelfRefEdge, GraphConstants.ARROW_SIMPLE);
    viskitSelfRefEdge.remove    (GraphConstants.ROUTING);

    viskitSelfRefCancel = GraphConstants.createMap();
    viskitSelfRefCancel.putAll  (viskitCancelEdgeStyle);
    GraphConstants.setLineStyle (viskitSelfRefCancel, GraphConstants.STYLE_ORTHOGONAL);
    //GraphConstants.setLineEnd   (viskitSelfRefCancel, GraphConstants.ARROW_CIRCLE);
    viskitSelfRefCancel.remove  (GraphConstants.ROUTING);
  }

  public void changeEvent(EventNode en)
  {
    CircleCell c = (CircleCell)en.opaqueViewObject;
    c.setUserObject(en);

    graph.getUI().stopEditing(graph);
    graph.graphDidChange(); // jmb try...yes, I thought the stopEditing would do the same thing
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
  public void deleteEventNode(EventNode en)
  {
    DefaultGraphCell c = (DefaultGraphCell)en.opaqueViewObject;
    c.removeAllChildren();
    this.remove(new Object[]{c});
  }
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
  public void addEdge(SchedulingEdge se)
  {
    _addEdgeCommon(se,viskitEdgeStyle);
  }
  public void addCancelEdge(CancellingEdge ce)
  {
    _addEdgeCommon(ce,viskitCancelEdgeStyle);
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
        attributes.put(edge,this.viskitSelfRefEdge);
      else
        attributes.put(edge,this.viskitSelfRefCancel);
    }

    this.insert(new Object[]{edge},attributes,cs,null,null);

  }
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
