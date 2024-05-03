package viskit.jgraph;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
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

    Map viskitEdgeStyle, viskitCancelEdgeStyle;
    Map viskitSelfRefEdge, viskitSelfRefCancel;
    private JGraph jGraph;

    public vGraphModel() {
        initViskitStyle();
    }

    @SuppressWarnings("unchecked") // JGraph not genericized
    private void initViskitStyle() {

        viskitEdgeStyle = new AttributeMap();

        // common to 4 types
        GraphConstants.setDisconnectable(viskitEdgeStyle, false);
        GraphConstants.setLineEnd(viskitEdgeStyle, GraphConstants.ARROW_TECHNICAL);
        GraphConstants.setEndFill(viskitEdgeStyle, true);
        GraphConstants.setEndSize(viskitEdgeStyle, 10);
        GraphConstants.setFont(viskitEdgeStyle, GraphConstants.DEFAULTFONT.deriveFont(10));

        // This setting critical to getting the start and end points offset from
        // the center of the node
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

        // JGraph loop routing is assigned by default
        viskitSelfRefEdge.remove(GraphConstants.ROUTING);

        // dup for cancel self edge
        viskitSelfRefCancel = new AttributeMap();
        viskitSelfRefCancel.putAll(viskitCancelEdgeStyle);

        // JGraph loop routing is assigned by default
        viskitSelfRefCancel.remove(GraphConstants.ROUTING);
    }

    public void changeEvent(EventNode en) {
        CircleCell c = (CircleCell) en.opaqueViewObject;
        c.setUserObject(en);

        reDrawNodes(); // jmb try...yes, I thought the stopEditing would do the same thing
    }

    /** Critical in toggling the status of a model, whether it can compile, or not */
    public void reDrawNodes() {
        jGraph.getUI().stopEditing(jGraph);
        jGraph.refresh();
    }

    public void changeEdge(Edge ed) {
        changeEitherEdge(ed);
    }

    public void changeCancelingEdge(Edge ed) {
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

    /** Ensures a clean JGraph tab for a new model */
    public void deleteAll() {
        Object[] localRoots = getRoots(this);
        for (Object localRoot : localRoots) {
            if (localRoot instanceof CircleCell) {
                Object[] child = new Object[1];
                child[0] = ((DefaultMutableTreeNode) localRoot).getFirstChild();
                jGraph.getGraphLayoutCache().remove(child);
            }
        }
        jGraph.getGraphLayoutCache().remove(localRoots);

        reDrawNodes();
    }

    public void deleteEventNode(EventNode en) {

        DefaultGraphCell c = (DefaultGraphCell) en.opaqueViewObject;
        c.removeAllChildren();
        jGraph.getGraphLayoutCache().remove(new Object[]{c});

        reDrawNodes();
    }

    public void deleteEdge(Edge edge) {

        DefaultEdge e = (DefaultEdge) edge.opaqueViewObject;
        jGraph.getGraphLayoutCache().remove(new Object[]{e});

        reDrawNodes();
    }

    public void deleteCancelingEdge(Edge edge) {
        deleteEdge(edge);
    }

    public void addEdge(Edge e) {
        _addEdgeCommon(e, viskitEdgeStyle);
    }

    public void addCancelEdge(Edge e) {
        _addEdgeCommon(e, viskitCancelEdgeStyle);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    private void _addEdgeCommon(Edge ed, Map edgeStyle) {

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

        jGraph.getGraphLayoutCache().insert(new Object[]{edge}, atts, cs, null, null);

        reDrawNodes();
    }

    /**
     * @param jGraph the jGraph to set
     */
    public void setjGraph(JGraph jGraph) {
        this.jGraph = jGraph;
    }
}
