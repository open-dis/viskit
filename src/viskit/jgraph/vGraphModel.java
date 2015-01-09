package viskit.jgraph;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
import viskit.model.CancellingEdge;
import viskit.model.Edge;
import viskit.model.EventNode;
import viskit.model.SchedulingEdge;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey
 * @since Feb 23, 2004
 * @since 1:21:52 PM
 * @version $Id$
 */
public class vGraphModel extends DefaultGraphModel {

    public JGraph graph; // fix this

    public vGraphModel() {
        initViskitStyle();
    }
    Map viskitEdgeStyle, viskitCancelEdgeStyle;
    Map viskitSelfRefEdge, viskitSelfRefCancel;

    @SuppressWarnings("unchecked") // JGraph not genericized
    private void initViskitStyle() {

        viskitEdgeStyle = new AttributeMap();

        // common to 4 types
        GraphConstants.setDisconnectable(viskitEdgeStyle, false);
        GraphConstants.setLineEnd(viskitEdgeStyle, GraphConstants.ARROW_TECHNICAL);
        GraphConstants.setEndFill(viskitEdgeStyle, true);
        GraphConstants.setEndSize(viskitEdgeStyle, 10);
        GraphConstants.setFont(viskitEdgeStyle, GraphConstants.DEFAULTFONT.deriveFont(10));
        GraphConstants.setBendable(viskitEdgeStyle, true);
        GraphConstants.setLineStyle(viskitEdgeStyle, GraphConstants.STYLE_ORTHOGONAL);
        GraphConstants.setLineWidth(viskitEdgeStyle, 1);
        GraphConstants.setOpaque(viskitEdgeStyle, true);
        GraphConstants.setBackground(viskitEdgeStyle, new Color(255, 255, 255, 180));
        GraphConstants.setForeground(viskitEdgeStyle, Color.black);
        GraphConstants.setRouting(viskitEdgeStyle, new vRouting());

        // dup for cancel
        viskitCancelEdgeStyle = new AttributeMap();
        viskitCancelEdgeStyle.putAll(viskitEdgeStyle);
        GraphConstants.setDashPattern(viskitCancelEdgeStyle, new float[]{3, 3});

        // dup for self edge
        viskitSelfRefEdge = new AttributeMap();
        viskitSelfRefEdge.putAll(viskitEdgeStyle);
        viskitSelfRefEdge.remove(GraphConstants.ROUTING);

        // dup for cancel self edge
        viskitSelfRefCancel = new AttributeMap();
        viskitSelfRefCancel.putAll(viskitCancelEdgeStyle);
        viskitSelfRefCancel.remove(GraphConstants.ROUTING);
    }

    public void changeEvent(EventNode en) {
        CircleCell c = (CircleCell) en.opaqueViewObject;
        c.setUserObject(en);

        reDrawNodes(); // jmb try...yes, I thought the stopEditing would do the same thing
    }

    public void changeEdge(SchedulingEdge ed) {
        changeEitherEdge(ed);
    }

    public void changeCancellingEdge(CancellingEdge ed) {
        changeEitherEdge(ed);
    }

    // I don't think this is required anymore.  We don't change src/targets...we rebuild the edge
    private void changeEitherEdge(Edge ed) {
        CircleCell newFromCC = (CircleCell) ed.from.opaqueViewObject;
        vEdgeCell edgeC = (vEdgeCell) ed.opaqueViewObject;

        DefaultPort dpFrom = (DefaultPort) edgeC.getSource();
        Object dpCC = dpFrom.getParent();
        if (dpCC == newFromCC) {
            return;
        } // no change
        edgeC.setSource(edgeC.getTarget());
        edgeC.setTarget(dpFrom);

        reDrawNodes(); // this does it, but label is screwed
    }

    private void reDrawNodes() {
        graph.getUI().stopEditing(graph);
        graph.refresh();
    }

    public void deleteAll() {
        //remove(getRoots(this));
        Object[] localRoots = getRoots(this);
        for (Object localRoot : localRoots) {
            if (localRoot instanceof CircleCell) {
                Object[] child = new Object[1];
                child[0] = ((CircleCell) localRoot).getFirstChild();
                remove(child);
            }
        }
        remove(localRoots);
    }

    public void deleteEdge(SchedulingEdge edge) {
        DefaultEdge e = (DefaultEdge) edge.opaqueViewObject;
        remove(new Object[]{e});
    }

    public void deleteCancellingEdge(CancellingEdge edge) {
        DefaultEdge e = (DefaultEdge) edge.opaqueViewObject;
        remove(new Object[]{e});
    }

    public void deleteEventNode(EventNode en) {
        DefaultGraphCell c = (DefaultGraphCell) en.opaqueViewObject;
        c.removeAllChildren();
        remove(new Object[]{c});
    }

    public void addEdge(SchedulingEdge se) {
        _addEdgeCommon(se, viskitEdgeStyle);
    }

    public void addCancelEdge(CancellingEdge ce) {
        _addEdgeCommon(ce, viskitCancelEdgeStyle);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    private void _addEdgeCommon(viskit.model.Edge ed, Map edgeStyle) {
        EventNode enfrom = ed.from;
        EventNode ento = ed.to;
        DefaultGraphCell source = (DefaultGraphCell) enfrom.opaqueViewObject;
        DefaultGraphCell target = (DefaultGraphCell) ento.opaqueViewObject;

        DefaultEdge edge;
        if (enfrom == ento) {
            edge = new vSelfEdgeCell(null);
        } else {
            edge = new vEdgeCell(null);
        }

        ed.opaqueViewObject = edge;
        edge.setUserObject(ed);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, source.getFirstChild(), target.getFirstChild());

        Map atts = new Hashtable();

        if (enfrom == ento) {// self referential overwrite
            if (ed instanceof SchedulingEdge) {
                atts.put(edge, viskitSelfRefEdge);
            } else {
                atts.put(edge, viskitSelfRefCancel);
            }
        } else {
            atts.put(edge, edgeStyle);
        }

        insert(new Object[]{edge}, atts, cs, null, null);
    }
}
