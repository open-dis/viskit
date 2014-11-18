package viskit.jgraph;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import javax.swing.SwingUtilities;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import viskit.VGlobals;
import viskit.model.*;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey
 * @since Feb 23, 2004
 * @since 1:21:52 PM
 * @version $Id$
 */
public class vGraphAssemblyModel extends DefaultGraphModel {

    public JGraph graph; // fix this

    public vGraphAssemblyModel() {
        initViskitStyle();
    }
    Map viskitAssyAdapterEdgeStyle;
    Map viskitAssyPclEdgeStyle;
    Map viskitAssySimEvLisEdgeStyle;

    @SuppressWarnings("unchecked") // JGraph not genericized
    private void initViskitStyle() {

        viskitAssyAdapterEdgeStyle = new AttributeMap();

        // common to 3 types
        GraphConstants.setDisconnectable(viskitAssyAdapterEdgeStyle, false);
        GraphConstants.setLineBegin(viskitAssyAdapterEdgeStyle, GraphConstants.ARROW_TECHNICAL);  // arrow not drawn
        GraphConstants.setBeginFill(viskitAssyAdapterEdgeStyle, false);
        GraphConstants.setBeginSize(viskitAssyAdapterEdgeStyle, 16);
        GraphConstants.setBendable(viskitAssyAdapterEdgeStyle, true);
        GraphConstants.setLineStyle(viskitAssyAdapterEdgeStyle, GraphConstants.STYLE_ORTHOGONAL);
        GraphConstants.setOpaque(viskitAssyAdapterEdgeStyle, true);
        GraphConstants.setForeground(viskitAssyAdapterEdgeStyle, Color.black);
        GraphConstants.setRouting(viskitAssyAdapterEdgeStyle, new vRouting());

        // duplicate for pcl
        viskitAssyPclEdgeStyle = new AttributeMap();
        viskitAssyPclEdgeStyle.putAll(viskitAssyAdapterEdgeStyle);

        // duplicate for sel
        viskitAssySimEvLisEdgeStyle = new AttributeMap();
        viskitAssySimEvLisEdgeStyle.putAll(viskitAssyAdapterEdgeStyle);

        // Customize adapter
        GraphConstants.setLineWidth(viskitAssyAdapterEdgeStyle, 3.0f); // wide line because we're doubling
        GraphConstants.setLineColor(viskitAssyAdapterEdgeStyle, Color.black);

        // Customize pcl
        GraphConstants.setLineWidth(viskitAssyPclEdgeStyle, 1.5f);
        GraphConstants.setLineColor(viskitAssyPclEdgeStyle, new Color(134, 87, 87)); // sort of blood color

        // Customize sel
        GraphConstants.setLineWidth(viskitAssySimEvLisEdgeStyle, 1.0f);
        GraphConstants.setLineColor(viskitAssySimEvLisEdgeStyle, Color.black);
    }

    private void reDrawNodes() {
        graph.getUI().stopEditing(graph);
        graph.refresh();
    }

    public void changeEvent(AssemblyNode en) {
        DefaultGraphCell c = (DefaultGraphCell) en.opaqueViewObject;
        c.setUserObject(en);
        reDrawNodes();
    }

    public void deleteAll() {
        //remove(getRoots(this));
        Object[] localRoots = getRoots(this);
        for (Object localRoot : localRoots) {
            if (localRoot instanceof AssemblyCircleCell || localRoot instanceof AssemblyPropListCell) {
                Object[] child = new Object[1];
                child[0] = ((DefaultGraphCell) localRoot).getFirstChild();
                remove(child);
            }
        }
        remove(localRoots);
    }

    public void deleteEGNode(EvGraphNode egn) {
        DefaultGraphCell c = (DefaultGraphCell) egn.opaqueViewObject;
        c.removeAllChildren();
        this.remove(new Object[]{c});
    }

    public void changeEGNode(EvGraphNode egn) {
        AssemblyCircleCell c = (AssemblyCircleCell) egn.opaqueViewObject;
        c.setUserObject(egn);
        reDrawNodes();
    }

    public void deletePCLNode(PropChangeListenerNode pcln) {
        DefaultGraphCell c = (DefaultGraphCell) pcln.opaqueViewObject;
        c.removeAllChildren();
        this.remove(new Object[]{c});
    }

    public void changePCLNode(PropChangeListenerNode pcln) {
        AssemblyPropListCell c = (AssemblyPropListCell) pcln.opaqueViewObject;
        c.setUserObject(pcln);
        reDrawNodes();
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addAdapterEdge(AdapterEdge ae) {
        Object frO = ae.getFrom();
        Object toO = ae.getTo();
        DefaultGraphCell from, to;
        if (frO instanceof EvGraphNode) {
            from = (DefaultGraphCell) ((EvGraphNode) frO).opaqueViewObject;
        } else {
            from = (DefaultGraphCell) ((PropChangeListenerNode) frO).opaqueViewObject;
        }
        if (toO instanceof EvGraphNode) {
            to = (DefaultGraphCell) ((EvGraphNode) toO).opaqueViewObject;
        } else {
            to = (DefaultGraphCell) ((PropChangeListenerNode) toO).opaqueViewObject;
        }

        vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
        ae.opaqueViewObject = edge;
        edge.setUserObject(ae);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getFirstChild(), to.getFirstChild());

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssyAdapterEdgeStyle);

        insert(new Object[]{edge}, atts, cs, null, null);
        toBack(new Object[]{edge});

        // If a user cancels this edge, it needs to be counted as selected
        // so that the AssemblyControllerImpl can find it
        Vector<Object> ev = new Vector<>();
        ev.add(ae);
        VGlobals.instance().getAssemblyController().selectNodeOrEdge(ev);
    }

    public void deleteAdapterEdge(AdapterEdge ae) {
        this.remove(new Object[]{ae.opaqueViewObject});
    }

    public void changeAnyEdge(AssemblyEdge asEd) {
        DefaultGraphCell c = (DefaultGraphCell) asEd.opaqueViewObject;
        c.setUserObject(asEd);
        reDrawNodes();
    }

    public void changeAdapterEdge(AdapterEdge ae) {
        changeAnyEdge(ae);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addSimEvListEdge(SimEvListenerEdge sele) {
        Object frO = sele.getFrom();
        Object toO = sele.getTo();
        DefaultGraphCell from, to;
        if (frO instanceof EvGraphNode) {
            from = (DefaultGraphCell) ((EvGraphNode) frO).opaqueViewObject;
        } else {
            from = (DefaultGraphCell) ((PropChangeListenerNode) frO).opaqueViewObject;
        }
        if (toO instanceof EvGraphNode) {
            to = (DefaultGraphCell) ((EvGraphNode) toO).opaqueViewObject;
        } else {
            to = (DefaultGraphCell) ((PropChangeListenerNode) toO).opaqueViewObject;
        }

        vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
        sele.opaqueViewObject = edge;
        edge.setUserObject(sele);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getFirstChild(), to.getFirstChild());

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssySimEvLisEdgeStyle);

        insert(new Object[]{edge}, atts, cs, null, null);
        toBack(new Object[]{edge});
    }

    public void deleteSimEvListEdge(SimEvListenerEdge sele) {
        this.remove(new Object[]{sele.opaqueViewObject});
    }

    public void changeSimEvListEdge(SimEvListenerEdge sele) {
        changeAnyEdge(sele);
    }

    public void deletePclEdge(PropChangeEdge pce) {
        this.remove(new Object[]{pce.opaqueViewObject});
    }

    public void changePclEdge(PropChangeEdge pce) {
        changeAnyEdge(pce);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addPclEdge(PropChangeEdge pce) {
        EvGraphNode egn = (EvGraphNode) pce.getFrom();
        //PropChangeListenerNode pcln = (PropChangeListenerNode)pce.getTo();         //todo uncomment after xml fixed
        AssemblyNode pcln = (AssemblyNode) pce.getTo();
        DefaultGraphCell from = (DefaultGraphCell) egn.opaqueViewObject;
        DefaultGraphCell to = (DefaultGraphCell) pcln.opaqueViewObject;
        vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
        pce.opaqueViewObject = edge;
        edge.setUserObject(pce);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getChildAt(0), to.getChildAt(0));

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssyPclEdgeStyle);

        insert(new Object[] {edge}, atts, cs, null, null);
        toBack(new Object[] {edge});

        // If a user cancels this edge, it needs to be counted as selected
        // so that the AssemblyControllerImpl can find it
        Vector<Object> ev = new Vector<>();
        ev.add(pce);
        VGlobals.instance().getAssemblyController().selectNodeOrEdge(ev);
    }
}
