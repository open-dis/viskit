package viskit.jgraph;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
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

    Map viskitAssyAdapterEdgeStyle;
    Map viskitAssyPclEdgeStyle;
    Map viskitAssySimEvLisEdgeStyle;
    private JGraph jGraph;

    public vGraphAssemblyModel() {
        initViskitStyle();
    }

    @SuppressWarnings("unchecked") // JGraph not genericized
    private void initViskitStyle() {

        viskitAssyAdapterEdgeStyle = new AttributeMap();

        // common to 3 types
        GraphConstants.setDisconnectable(viskitAssyAdapterEdgeStyle, false);
        GraphConstants.setLineBegin(viskitAssyAdapterEdgeStyle, GraphConstants.ARROW_TECHNICAL);  // arrow not drawn
        GraphConstants.setBeginFill(viskitAssyAdapterEdgeStyle, false);
        GraphConstants.setBeginSize(viskitAssyAdapterEdgeStyle, 16);

        // This setting critical to getting the start and end points offset from
        // the center of the node
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

    public void changeEvent(AssemblyNode en) {
        DefaultGraphCell c = (DefaultGraphCell) en.opaqueViewObject;
        c.setUserObject(en);

        reDrawNodes();
    }

    public void reDrawNodes() {
        jGraph.getUI().stopEditing(jGraph);
        jGraph.refresh();
    }

    public void changeEGNode(AssemblyNode egn) {
        DefaultGraphCell c = (DefaultGraphCell) egn.opaqueViewObject;
        c.setUserObject(egn);

        reDrawNodes();
    }

    public void changePCLNode(AssemblyNode pcln) {
        changeEGNode(pcln);
    }

    /** Ensures a clean JGraph tab for a new model */
    public void deleteAll() {
        Object[] localRoots = getRoots(this);
        for (Object localRoot : localRoots) {
            if (localRoot instanceof AssemblyCircleCell || localRoot instanceof AssemblyPropListCell) {
                Object[] child = new Object[1];
                child[0] = ((DefaultMutableTreeNode) localRoot).getFirstChild();
                jGraph.getGraphLayoutCache().remove(child);
            }
        }
        jGraph.getGraphLayoutCache().remove(localRoots);

        reDrawNodes();
    }

    public void deleteEGNode(AssemblyNode egn) {
        DefaultGraphCell c = (DefaultGraphCell) egn.opaqueViewObject;
        c.removeAllChildren();
        jGraph.getGraphLayoutCache().remove(new Object[]{c});

        reDrawNodes();
    }

    public void deletePCLNode(AssemblyNode pcln) {
        deleteEGNode(pcln);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addAdapterEdge(AssemblyEdge ae) {
        Object frO = ae.getFrom();
        Object toO = ae.getTo();
        DefaultGraphCell from, to;
        from = (DefaultGraphCell) ((ViskitElement) frO).opaqueViewObject;
        to = (DefaultGraphCell) ((ViskitElement) toO).opaqueViewObject;

        vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
        ae.opaqueViewObject = edge;
        edge.setUserObject(ae);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getFirstChild(), to.getFirstChild());

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssyAdapterEdgeStyle);

        jGraph.getGraphLayoutCache().insert(new Object[]{edge}, atts, cs, null, null);

        reDrawNodes();
    }

    public void changeAdapterEdge(AssemblyEdge ae) {
        changeAnyEdge(ae);
    }

    public void deleteAdapterEdge(AssemblyEdge ae) {
        DefaultEdge c = (DefaultEdge) ae.opaqueViewObject;
        jGraph.getGraphLayoutCache().remove(new Object[]{c});

        reDrawNodes();
    }

    public void changeAnyEdge(AssemblyEdge asEd) {
        DefaultGraphCell c = (DefaultGraphCell) asEd.opaqueViewObject;
        c.setUserObject(asEd);

        reDrawNodes();
    }

    public void deleteSimEvListEdge(AssemblyEdge sele) {
        deleteAdapterEdge(sele);
    }

    public void changeSimEvListEdge(AssemblyEdge sele) {
        changeAnyEdge(sele);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addSimEvListEdge(AssemblyEdge sele) {
        Object frO = sele.getFrom();
        Object toO = sele.getTo();
        DefaultGraphCell from, to;
        from = (DefaultGraphCell) ((ViskitElement) frO).opaqueViewObject;
        to = (DefaultGraphCell) ((ViskitElement) toO).opaqueViewObject;

        vAssemblyEdgeCell edge = new vAssemblyEdgeCell();
        sele.opaqueViewObject = edge;
        edge.setUserObject(sele);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getFirstChild(), to.getFirstChild());

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssySimEvLisEdgeStyle);

        jGraph.getGraphLayoutCache().insert(new Object[]{edge}, atts, cs, null, null);

        reDrawNodes();
    }

    public void deletePclEdge(AssemblyEdge pce) {
        deleteAdapterEdge(pce);
    }

    public void changePclEdge(AssemblyEdge pce) {
        changeAnyEdge(pce);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addPclEdge(AssemblyEdge pce) {
        AssemblyNode egn = (AssemblyNode) pce.getFrom();
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

        jGraph.getGraphLayoutCache().insert(new Object[] {edge}, atts, cs, null, null);

        reDrawNodes();
    }

    /**
     * @param jGraph the jGraph to set
     */
    public void setjGraph(JGraph jGraph) {
        this.jGraph = jGraph;
    }
}
