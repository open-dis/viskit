package viskit.jgraph;

import org.jgraph.JGraph;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import viskit.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Map;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey
 * @since Feb 23, 2004
 * @since 1:21:52 PM
 * @version $Id: vGraphAssemblyModel.java 1662 2007-12-16 19:44:04Z tdnorbra $
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

  // TODO: JGraph v3.1 not generic
  @SuppressWarnings("unchecked")
  public void initViskitStyle() {

    viskitAssyAdapterEdgeStyle = GraphConstants.createMap();

    // common to 3 types
    GraphConstants.setDisconnectable(viskitAssyAdapterEdgeStyle, false);
    GraphConstants.setLineBegin     (viskitAssyAdapterEdgeStyle, GraphConstants.ARROW_TECHNICAL);  // arrow not drawn
    GraphConstants.setBeginFill     (viskitAssyAdapterEdgeStyle, false);
    GraphConstants.setBeginSize     (viskitAssyAdapterEdgeStyle, 16);
    GraphConstants.setBendable      (viskitAssyAdapterEdgeStyle, true);
    GraphConstants.setLineStyle     (viskitAssyAdapterEdgeStyle, GraphConstants.STYLE_BEZIER);
    GraphConstants.setOpaque        (viskitAssyAdapterEdgeStyle, true);
    GraphConstants.setForeground    (viskitAssyAdapterEdgeStyle, Color.black);
    GraphConstants.setRouting       (viskitAssyAdapterEdgeStyle, new ViskitAssemblyRouting());

    // duplicate for pcl
    viskitAssyPclEdgeStyle = GraphConstants.createMap();
    
    // TODO: Fix generics
    viskitAssyPclEdgeStyle.putAll(viskitAssyAdapterEdgeStyle);

    // duplicate for sel
    viskitAssySimEvLisEdgeStyle = GraphConstants.createMap();
        
    // TODO: Fix generics
    viskitAssySimEvLisEdgeStyle.putAll(viskitAssyAdapterEdgeStyle);

    // Customize adapter
    GraphConstants.setLineWidth(viskitAssyAdapterEdgeStyle, 3.0f); // wide line because we're doubling
    GraphConstants.setLineColor(viskitAssyAdapterEdgeStyle, Color.black);

    // Customize pcl
    GraphConstants.setLineWidth(viskitAssyPclEdgeStyle, 1.5f);
    GraphConstants.setLineColor(viskitAssyPclEdgeStyle, new Color(134,87,87)); // sort of blood color

    // Customize sel
    GraphConstants.setLineWidth(viskitAssySimEvLisEdgeStyle, 1.0f);
    GraphConstants.setLineColor(viskitAssySimEvLisEdgeStyle, Color.black);
  }

  public void changeEvent(AssemblyNode en)
  {
    DefaultGraphCell c = (DefaultGraphCell)en.opaqueViewObject;
    c.setUserObject(en);

    graph.getUI().stopEditing(graph);
    graph.graphDidChange(); // jmb try...yes, I thought the stopEditing would do the same thing
  }


  public void deleteAll()
  {
    //remove(getRoots(this));
    Object[] roots = getRoots(this);
    for(int i=0;i<roots.length;i++) {
      if( roots[i] instanceof AssemblyCircleCell || roots[i] instanceof AssemblyPropListCell) {
        Object[] child = new Object[1];
        child[0] = ((DefaultGraphCell)roots[i]).getFirstChild();
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
    
    Map<DefaultGraphCell, Map> attributes = new Hashtable<DefaultGraphCell, Map>();
    attributes.put(c,createBounds(egn.getPosition().x,egn.getPosition().y,Color.black));
    c.add(new vAssemblyPortCell(egn.getName()+"/Center"));
    this.insert(new Object[]{c},attributes,null,null,null);
    this.toFront(new Object[]{c});
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
    
    Map<DefaultGraphCell, Map> attributes = new Hashtable<DefaultGraphCell, Map>();
    attributes.put(c,createBounds(pcln.getPosition().x,pcln.getPosition().y,Color.black));
    c.add(new vAssemblyPortCell(pcln.getName()+"/Center"));
    this.insert(new Object[]{c},attributes,null,null,null);
    this.toFront(new Object[]{c});
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
    Object frO = ae.getFrom();
    Object toO = ae.getTo();
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
    
    Map<vAssemblyEdgeCell, Map> attributes = new Hashtable<vAssemblyEdgeCell, Map>();
    attributes.put(edge,this.viskitAssyAdapterEdgeStyle);

    this.insert(new Object[]{edge},attributes,cs,null,null);
    this.toBack(new Object[]{edge});
  }
  public void deleteAdapterEdge(AdapterEdge ae)
  {
    this.remove(new Object[]{ae.opaqueViewObject});
  }
  public void changeAnyEdge(AssemblyEdge asEd)
  {
    DefaultGraphCell c = (DefaultGraphCell)asEd.opaqueViewObject;
    c.setUserObject(asEd);

    graph.getUI().stopEditing(graph);
    graph.graphDidChange(); // jmb try...yes, I thought the stopEditing would do the same thing

  }
  public void changeAdapterEdge(AdapterEdge ae)
  {
    changeAnyEdge(ae);
  }
  public void addSimEvListEdge(SimEvListenerEdge sele)
  {
    Object frO = sele.getFrom();
    Object toO = sele.getTo();
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
    
    Map<vAssemblyEdgeCell, Map> attributes = new Hashtable<vAssemblyEdgeCell, Map>();
    attributes.put(edge,this.viskitAssySimEvLisEdgeStyle);

    this.insert(new Object[]{edge},attributes,cs,null,null);
    this.toBack(new Object[]{edge});
  }
  public void deleteSimEvListEdge(SimEvListenerEdge sele)
  {
    this.remove(new Object[]{sele.opaqueViewObject});
  }

  public void changeSimEvListEdge(SimEvListenerEdge sele)
  {
    changeAnyEdge(sele);
  }
  public void deletePclEdge(PropChangeEdge pce)
  {
    this.remove(new Object[]{pce.opaqueViewObject});

  }
  public void changePclEdge(PropChangeEdge pce)
  {
    changeAnyEdge(pce);
  }
  public void addPclEdge(PropChangeEdge pce)
  {
    EvGraphNode egn = (EvGraphNode)pce.getFrom();
    //PropChangeListenerNode pcln = (PropChangeListenerNode)pce.getTo();         //todo uncomment after xml fixed
    AssemblyNode pcln = (AssemblyNode)pce.getTo();
    DefaultGraphCell from = (DefaultGraphCell)egn.opaqueViewObject;
    DefaultGraphCell to = (DefaultGraphCell)pcln.opaqueViewObject;
    vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
    pce.opaqueViewObject = edge;
    edge.setUserObject(pce);
    ConnectionSet cs = new ConnectionSet();
    cs.connect(edge,from.getChildAt(0),to.getChildAt(0));
    
    Map<vAssemblyEdgeCell, Map> attributes = new Hashtable<vAssemblyEdgeCell, Map>();
    attributes.put(edge,this.viskitAssyPclEdgeStyle);

    this.insert(new Object[]{edge},attributes,cs,null,null);
    this.toBack(new Object[]{edge});
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
