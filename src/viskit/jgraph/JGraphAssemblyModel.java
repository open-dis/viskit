package viskit.jgraph;

import edu.nps.util.LogUtilities;
import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;
import org.apache.log4j.Logger;
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
public class JGraphAssemblyModel extends DefaultGraphModel 
{
    static final Logger LOG = LogUtilities.getLogger(JGraphAssemblyModel.class);

    Map viskitAssemblyAdapterEdgeStyle;
    Map viskitAssemblyPclEdgeStyle;
    Map viskitAssemblySimEventListenerEdgeStyle;
    private JGraph jGraph;

    public JGraphAssemblyModel() {
        initializeViskitStyle();
    }

    @SuppressWarnings("unchecked") // JGraph not genericized
    private void initializeViskitStyle()
	{
        viskitAssemblyAdapterEdgeStyle = new AttributeMap();

        // common to 3 types
        GraphConstants.setDisconnectable(viskitAssemblyAdapterEdgeStyle, false);
        GraphConstants.setLineBegin(viskitAssemblyAdapterEdgeStyle, GraphConstants.ARROW_TECHNICAL);  // arrow not drawn
        GraphConstants.setBeginFill(viskitAssemblyAdapterEdgeStyle, false);
        GraphConstants.setBeginSize(viskitAssemblyAdapterEdgeStyle, 16);

        // This setting critical to getting the start and end points offset from
        // the center of the node
        GraphConstants.setLineStyle(viskitAssemblyAdapterEdgeStyle, GraphConstants.STYLE_ORTHOGONAL);
        GraphConstants.setOpaque(viskitAssemblyAdapterEdgeStyle, true);
        GraphConstants.setForeground(viskitAssemblyAdapterEdgeStyle, Color.black);
        GraphConstants.setRouting(viskitAssemblyAdapterEdgeStyle, new JGraphRouting());

        // duplicate for pcl
        viskitAssemblyPclEdgeStyle = new AttributeMap();
        viskitAssemblyPclEdgeStyle.putAll(viskitAssemblyAdapterEdgeStyle);

        // duplicate for sel
        viskitAssemblySimEventListenerEdgeStyle = new AttributeMap();
        viskitAssemblySimEventListenerEdgeStyle.putAll(viskitAssemblyAdapterEdgeStyle);

        // Customize adapter
        GraphConstants.setLineWidth(viskitAssemblyAdapterEdgeStyle, 3.0f); // wide line because we're doubling
        GraphConstants.setLineColor(viskitAssemblyAdapterEdgeStyle, Color.black);

        // Customize pcl
        GraphConstants.setLineWidth(viskitAssemblyPclEdgeStyle, 1.5f);
        GraphConstants.setLineColor(viskitAssemblyPclEdgeStyle, new Color(134, 87, 87)); // sort of blood color

        // Customize sel
        GraphConstants.setLineWidth(viskitAssemblySimEventListenerEdgeStyle, 1.0f);
        GraphConstants.setLineColor(viskitAssemblySimEventListenerEdgeStyle, Color.black);
    }

    public void changeEvent(AssemblyNode simEntityNode) 
	{
        DefaultGraphCell c = (DefaultGraphCell) simEntityNode.opaqueViewObject;
        c.setUserObject(simEntityNode);

        redrawJGraphNodes();
    }

    public void redrawJGraphNodes() 
	{
        jGraph.getUI().stopEditing(jGraph);
        jGraph.refresh();
    }

    public void changeEventGraphNode(AssemblyNode eventGraphNode)
	{
        DefaultGraphCell graphCell = (DefaultGraphCell) eventGraphNode.opaqueViewObject;
        graphCell.setUserObject(eventGraphNode);

        redrawJGraphNodes();
    }

    public void changePCLNode(AssemblyNode propertyChangeListenerNode) 
	{
        changeEventGraphNode(propertyChangeListenerNode);
    }

    /** Ensures a clean JGraph tab for a new model */
    public void deleteAllJGraphNodes() 
	{
        Object[] localRoots = getRoots(this);
        for (Object localRoot : localRoots) 
		{
            if (localRoot instanceof AssemblyCircleCell || localRoot instanceof AssemblyPropertyChangeListenerCell) 
			{
                Object[] child = new Object[1];
                child[0] = ((DefaultGraphCell) localRoot).getFirstChild();
                jGraph.getGraphLayoutCache().remove(child);
            }
        }
        jGraph.getGraphLayoutCache().remove(localRoots);

        redrawJGraphNodes();
    }

    public void deleteEventGraphNode(AssemblyNode eventGraphNode) 
	{
        DefaultGraphCell graphCell = (DefaultGraphCell) eventGraphNode.opaqueViewObject;
        graphCell.removeAllChildren();
        jGraph.getGraphLayoutCache().remove(new Object[]{graphCell});

        redrawJGraphNodes();
    }

    public void deletePCLNode(AssemblyNode propertyChangeListenerNode)
	{
        deleteEventGraphNode(propertyChangeListenerNode);
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    public void addAdapterEdge(AssemblyEdge adapterEdge) 
	{
        Object fromObject = adapterEdge.getFromObject();
        Object   toObject = adapterEdge.getToObject();
        DefaultGraphCell from, to;
        from = (DefaultGraphCell) ((AssemblyNode) fromObject).opaqueViewObject;
        to   = (DefaultGraphCell) ((AssemblyNode) toObject).opaqueViewObject;

        JGraphAssemblyEdgeCell edge = new JGraphAssemblyEdgeCell();
        adapterEdge.opaqueViewObject = edge;
        edge.setUserObject(adapterEdge);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getFirstChild(), to.getFirstChild());

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssemblyAdapterEdgeStyle);

        jGraph.getGraphLayoutCache().insert(new Object[]{edge}, atts, cs, null, null);

        redrawJGraphNodes();
    }

    public void changeAdapterEdge(AssemblyEdge ae) {
        changeAnyEdge(ae);
    }

    public void deleteAdapterEdge(AssemblyEdge ae) {
        DefaultEdge c = (DefaultEdge) ae.opaqueViewObject;
        jGraph.getGraphLayoutCache().remove(new Object[]{c});

        redrawJGraphNodes();
    }

    public void changeAnyEdge(AssemblyEdge asEd) {
        DefaultGraphCell c = (DefaultGraphCell) asEd.opaqueViewObject;
        c.setUserObject(asEd);

        redrawJGraphNodes();
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
        Object frO = sele.getFromObject();
        Object toO = sele.getToObject();
        DefaultGraphCell from, to;
        from = (DefaultGraphCell) ((AssemblyNode) frO).opaqueViewObject;
        to = (DefaultGraphCell) ((AssemblyNode) toO).opaqueViewObject;

        JGraphAssemblyEdgeCell edge = new JGraphAssemblyEdgeCell();
        sele.opaqueViewObject = edge;
        edge.setUserObject(sele);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getFirstChild(), to.getFirstChild());

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssemblySimEventListenerEdgeStyle);

        jGraph.getGraphLayoutCache().insert(new Object[]{edge}, atts, cs, null, null);

        redrawJGraphNodes();
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
        AssemblyNode egn = (AssemblyNode) pce.getFromObject();
        //PropertyChangeListenerNode pcln = (PropertyChangeListenerNode)pce.getToObject();         //todo uncomment after xml fixed
        AssemblyNode pcln = (AssemblyNode) pce.getToObject();
        DefaultGraphCell from = (DefaultGraphCell) egn.opaqueViewObject;
        DefaultGraphCell to = (DefaultGraphCell) pcln.opaqueViewObject;
        JGraphAssemblyEdgeCell edge = new JGraphAssemblyEdgeCell();
        pce.opaqueViewObject = edge;
        edge.setUserObject(pce);

        ConnectionSet cs = new ConnectionSet();
        cs.connect(edge, from.getChildAt(0), to.getChildAt(0));

        Map atts = new Hashtable();
        atts.put(edge, this.viskitAssemblyPclEdgeStyle);

        jGraph.getGraphLayoutCache().insert(new Object[] {edge}, atts, cs, null, null);

        redrawJGraphNodes();
    }

    /**
     * @param jGraph the jGraph to set
     */
    public void setjGraph(JGraph jGraph) {
        this.jGraph = jGraph;
    }
}
