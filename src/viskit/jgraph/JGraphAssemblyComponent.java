package viskit.jgraph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.*;
import javax.swing.undo.UndoManager;
import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.*;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.view.AssemblyEditViewFrame;
import viskit.model.ModelEvent;
import viskit.control.AssemblyController;
import viskit.model.*;
import viskit.view.dialog.PclEdgeInspectorDialog;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects MOVES Institute. Naval
 * Postgraduate School, Monterey, CA
 *
 * @author Mike Bailey
 * @since Feb 19, 2004
 * @since 2:54:31 PM
 * @version $Id: JGraphAssemblyComponent.java 2323 2012-06-19 23:11:11Z tdnorbra$
 */
public class JGraphAssemblyComponent extends JGraph implements GraphModelListener {

    JGraphAssemblyModel vGAModel;
    AssemblyEditViewFrame parent;
    private UndoManager undoManager;

    public JGraphAssemblyComponent(JGraphAssemblyModel model, AssemblyEditViewFrame frame) {
        super(model);
        parent = frame;

        JGraphAssemblyComponent instance = this;
        ToolTipManager.sharedInstance().registerComponent(instance);
		// jGraph initializations
        this.vGAModel = model;
        this.setSizeable(false);
        this.setGridVisible(true);
        this.setGridMode(JGraph.LINE_GRID_MODE);
        this.setGridColor(new Color(0xcc, 0xcc, 0xff)); // default on Mac, makes Windows look better
        this.setGridEnabled(true); // means snap - TODO expose interface
        this.setGridSize(ViskitStatics.DEFAULT_GRID_SIZE);
        this.setMarqueeHandler(new JGraphMarqueeHandler(instance, "Assembly"));
        this.setAntiAliased(true);
        this.setLockedHandleColor(Color.red);
        this.setHighlightColor(Color.red);
		
//		double defaultScale = this.getScale(); // debug
//		getTolerance();                        // debug
		this.setScale(ViskitStatics.DEFAULT_ZOOM); // initialization
		this.setMinimumMove (ViskitStatics.DEFAULT_GRID_SIZE);
        setTolerance(ViskitStatics.DEFAULT_SELECT_TOLERANCE);

        // Jump to default port on connect
        setJumpToDefaultPort(true);

         // Set up the cut/remove/paste/copy/undo/redo actions
        undoManager = new JGraphGraphUndoManager(parent.getController());
        addGraphSelectionListener((GraphSelectionListener) undoManager);
        model.addUndoableEditListener(undoManager);
        model.addGraphModelListener(instance);

        // As of JGraph-5.2, custom cell rendering is
        // accomplished via this convention
        getGraphLayoutCache().setFactory(new DefaultCellViewFactory() {

            // To use circles, from the tutorial
            @Override
            protected VertexView createVertexView(Object v) {
                VertexView view;
                if (v instanceof AssemblyCircleCell) {
                    view = new AssemblyCircleView(v);
                } else if (v instanceof AssemblyPropertyListCell) {
                    view = new AssemblyPropListView(v);
                } else {
                    view = super.createVertexView(v);
                }
                return view;
            }

            // To customize my edges
            @Override
            protected EdgeView createEdgeView(Object e) {
                EdgeView view = null;
                if (e instanceof vAssemblyEdgeCell) {
                    Object o = ((vAssemblyEdgeCell) e).getUserObject();
                    if (o instanceof PropertyChangeListenerEdge) {
                        view = new vAssyPclEdgeView(e);
                    }
                    if (o instanceof AdapterEdge) {
                        view = new vAssyAdapterEdgeView(e);
                    }
                    if (o instanceof SimEventListenerEdge) {
                        view = new vAssySelEdgeView(e);
                    }
                } else {
                    view = super.createEdgeView(e);
                }
                return view;
            }

            @Override
            protected PortView createPortView(Object p) {
                PortView view;
                if (p instanceof vAssemblyPortCell) {
                    view = new vAssemblyPortView(p);
                } else {
                    view = super.createPortView(p);
                }
                return view;
            }
        });
    }

    @Override
    public void updateUI() {
        // Install a new UI
        setUI(new JGraphAssemblyUI());    // we use our own for node/edge inspector editting
        //setUI(new BasicGraphUI());   // test
        invalidate();
    }

    private ModelEvent currentModelEvent = null;

    public void viskitModelChanged(ModelEvent modelEvent) {
        currentModelEvent = modelEvent;

        switch (modelEvent.getID())
		{
            case ModelEvent.NEWASSEMBLYMODEL:

                // Ensure we start fresh
                vGAModel.deleteAll();
                break;
            case ModelEvent.EVENTGRAPH_ADDED:

                // Reclaimed from the vGAModel to here
                insert((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.EVENTGRAPH_CHANGED:
                vGAModel.changeEGNode((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.EVENTGRAPH_DELETED:
                vGAModel.deleteEGNode((AssemblyNode) modelEvent.getSource());
                break;

            case ModelEvent.PCL_ADDED:

                // Reclaimed from the vGAModel to here
                insert((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.PCL_CHANGED:
                vGAModel.changePCLNode((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.PCL_DELETED:
                vGAModel.deletePCLNode((AssemblyNode) modelEvent.getSource());
                break;

            case ModelEvent.ADAPTEREDGE_ADDED:
                vGAModel.addAdapterEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.ADAPTEREDGE_CHANGED:
                vGAModel.changeAdapterEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.ADAPTEREDGE_DELETED:
                vGAModel.deleteAdapterEdge((AssemblyEdge) modelEvent.getSource());
                break;

            case ModelEvent.SIMEVENTLISTEDGE_ADDED:
                vGAModel.addSimEvListEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.SIMEVENTLISTEDGE_CHANGED:
                vGAModel.changeSimEvListEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.SIMEVENTLISTEDGE_DELETED:
                vGAModel.deleteSimEvListEdge((AssemblyEdge) modelEvent.getSource());
                break;

            case ModelEvent.PCLEDGE_ADDED:
                vGAModel.addPclEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.PCLEDGE_DELETED:
                vGAModel.deletePclEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.PCLEDGE_CHANGED:
                vGAModel.changePclEdge((AssemblyEdge) modelEvent.getSource());
                break;

            // Deliberate fall-through for these b/c the JGraph internal model
            // keeps track
            case ModelEvent.UNDO_EVENT_GRAPH:
            case ModelEvent.REDO_EVENT_GRAPH:
            case ModelEvent.UNDO_PCL:
            case ModelEvent.REDO_PCL:
            case ModelEvent.UNDO_ADAPTER_EDGE:
            case ModelEvent.REDO_ADAPTER_EDGE:
            case ModelEvent.UNDO_SIMEVENT_LISTENER_EDGE:
            case ModelEvent.REDO_SIMEVENT_LISTENER_EDGE:
            case ModelEvent.UNDO_PCL_EDGE:
            case ModelEvent.REDO_PCL_EDGE:
                vGAModel.reDrawNodes();
                break;
            default:
            //System.out.println("duh");
        }
        currentModelEvent = null;
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    @Override
    public void graphChanged(GraphModelEvent graphModelEvent)
	{
        if (currentModelEvent != null && currentModelEvent.getID() == ModelEvent.NEWASSEMBLYMODEL) // bail if this came from outside
        {
            return; // this came in from outside, we don't have to inform anybody.. prevent reentry
        }

        // TODO: confirm any other events that should cause us to bail here
        GraphModelEvent.GraphModelChange graphModelChange = graphModelEvent.getChange();
        Object[] graphModelChangeArray = graphModelChange.getChanged();

        // bounds (position) might have changed:
        if (graphModelChangeArray != null)
		{
			double assemblyZoomFactor = ViskitGlobals.instance().getAssemblyEditViewFrame().getCurrentZoomFactor();
            for (Object cell : graphModelChangeArray)
			{
                if (cell instanceof AssemblyCircleCell)
				{
                    AssemblyCircleCell assemblyCircleCell = (AssemblyCircleCell) cell;
                    AttributeMap attributeMap = assemblyCircleCell.getAttributes();
                    Rectangle2D.Double rectangle2D = (Rectangle2D.Double) attributeMap.get("bounds");
                    if (rectangle2D != null)
					{
                        EventGraphNode eventGraphNode = (EventGraphNode) assemblyCircleCell.getUserObject();
                        eventGraphNode.setPosition(ViskitGlobals.snapToGrid(snap(new Point2D.Double(rectangle2D.x, rectangle2D.y)),assemblyZoomFactor));
                        ((AssemblyModel) parent.getModel()).changeEventGraphNode(eventGraphNode);
                        attributeMap.put("bounds", attributeMap.createRect(eventGraphNode.getPosition().getX(), eventGraphNode.getPosition().getY(), rectangle2D.width, rectangle2D.height));
                    }
                } 
				else if (cell instanceof AssemblyPropertyListCell)
				{
                    AssemblyPropertyListCell assemblyPropertyListCell = (AssemblyPropertyListCell) cell;

                    AttributeMap attributeMap = assemblyPropertyListCell.getAttributes();
                    Rectangle2D.Double rectangle2D = (Rectangle2D.Double) attributeMap.get("bounds");
                    if (rectangle2D != null)
					{
                        PropertyChangeListenerNode propertyChangeListenerNode = (PropertyChangeListenerNode) assemblyPropertyListCell.getUserObject();
                        propertyChangeListenerNode.setPosition(ViskitGlobals.snapToGrid(snap(new Point2D.Double(rectangle2D.x, rectangle2D.y)),assemblyZoomFactor));
                        ((AssemblyModel) parent. getModel()).changePclNode(propertyChangeListenerNode);
                        attributeMap.put("bounds", attributeMap.createRect(propertyChangeListenerNode.getPosition().getX(), propertyChangeListenerNode.getPosition().getY(), rectangle2D.width, rectangle2D.height));
                    }
                }
            }
        }
    }

    private String escapeLTGT(String s) {
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    @Override
    public String getToolTipText(MouseEvent event)
	{
        if (event != null)
		{
            Object c = this.getFirstCellForLocation(event.getX(), event.getY());
            if (c != null)
			{
                StringBuilder sb = new StringBuilder("<html>");
                if (c instanceof vAssemblyEdgeCell)
				{
                    vAssemblyEdgeCell vc = (vAssemblyEdgeCell) c;
                    AssemblyEdge assemblyEdge = (AssemblyEdge) vc.getUserObject();
					// events flow out of fromObject into toObject
                    Object   toObject = assemblyEdge.getTo();
                    Object fromObject = assemblyEdge.getFrom();

                    if (assemblyEdge instanceof AdapterEdge)
					{
                        Object   toEvent = ((AdapterEdge) assemblyEdge).getTargetEvent();
                        Object fromEvent = ((AdapterEdge) assemblyEdge).getSourceEvent();
                        sb.append("<center>Adapter<br><u>");// +
                        sb.append(fromObject);
                        sb.append(".");
                        sb.append(fromEvent);
                        sb.append("</u> connected to <u>");
                        sb.append(toObject);
                        sb.append(".");
                        sb.append(toEvent);
                    }
					else if (assemblyEdge instanceof SimEventListenerEdge) {
                        sb.append("<center>SimEvent Listener<br><u>");
                        sb.append(toObject);
                        sb.append("</u> listening to <u>");
                        sb.append(fromObject);
                    }
					else
					{
                        String property = ((PropertyChangeListenerEdge) assemblyEdge).getProperty();
                        property = (property != null && property.length() > 0) ? property : PclEdgeInspectorDialog.ALL_STATE_VARIABLES_NAME;
                        sb.append("<center>Property Change Listener<br><u>");
						String    className = ((PropertyChangeListenerNode) toObject).getType();
						if (className.contains("."))
							className = className.substring(className.lastIndexOf(".")+1);
						String instanceName = ((PropertyChangeListenerNode) toObject).getName();
						if (!instanceName.toLowerCase().contains(className.toLowerCase()))
						{
							sb.append(className);
							sb.append(" ");
						}
                        sb.append(instanceName);
                        sb.append("</u> listening to <u>");
                        sb.append(((EventGraphNode)fromObject).getName()); // class name
						// also need instance name? probably only if more than one instance of this class exists,
						//      and perhaps not even then since this is a tooltip
                        sb.append(".");
                        sb.append(property);
                    }
                    String description = assemblyEdge.getDescription();
                    if (description != null)
					{
                        description = description.trim();
                        if (description.length() > 0) {
                            sb.append("<br>");
                            sb.append("<i>description:</i> ");
                            sb.append(wrapAtPosition(description, 60));
                        }
                    }
                    sb.append("</center>");
                    sb.append("</html>");
                    return sb.toString();
                } 
				else if (c instanceof AssemblyCircleCell || c instanceof AssemblyPropertyListCell)
				{
                    String type;
                    String name;
                    String description;
                    if (c instanceof AssemblyCircleCell) {
                        AssemblyCircleCell cc = (AssemblyCircleCell) c;
                        EventGraphNode en = (EventGraphNode) cc.getUserObject();
                        type = en.getType();
                        name = en.getName();
                        description = en.getDescription();
                    } else /*if (c instanceof AssemblyPropertyListCell)*/ {
                        AssemblyPropertyListCell cc = (AssemblyPropertyListCell) c;
                        PropertyChangeListenerNode pcln = (PropertyChangeListenerNode) cc.getUserObject();
                        type = pcln.getType();
                        name = pcln.getName();
                        description = pcln.getDescription();
                    }

                    sb.append("<center><u>");
                    sb.append(type);
                    sb.append("</u> Assembly <br>");
                    sb.append(name);
                    if (description != null) {
                        description = description.trim();
                        if (description.length() > 0) {
                            sb.append("<br>");
                            sb.append("<i>description:</i> ");
                            sb.append(wrapAtPosition(escapeLTGT(description), 60));
                        }
                    }
                    sb.append("</center>");
                    sb.append("</html>");
                    return sb.toString();
                }
            }
        }
        return null;
    }

    private String wrapAtPosition(String s, int len) {
        String[] sa = s.split(" ");
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        do {
            int ll = 0;
            sb.append("&nbsp;");
            do {
                ll += sa[idx].length() + 1;
                sb.append(sa[idx++]);
                sb.append(" ");
            } while (idx < sa.length && ll < len);
            sb.append("<br>");
        } while (idx < sa.length);

        String st = sb.toString();
        if (st.endsWith("<br>")) {
            st = st.substring(0, st.length() - 4);
        }
        return st.trim();
    }

    @Override
    public String convertValueToString(Object value)
	{
        CellView view = (value instanceof CellView)
                ? (CellView) value
                : getGraphLayoutCache().getMapping(value, false);

        if (view instanceof AssemblyCircleView) {
            AssemblyCircleCell cc = (AssemblyCircleCell) view.getCell();
            Object en = cc.getUserObject();
            if (en instanceof EventGraphNode) {
                return ((EventGraphNode) en).getName();
            }    // label name is actually gotten in paintComponent
        }
        return null;
    }

    /**
     * @return the undoManager
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /** Insert a new Edge between source and target
     * @param source the "from" of the connection
     * @param target the "to" of the connection
     */
    public void connect(Port source, Port target) {
        DefaultGraphCell src = (DefaultGraphCell) getModel().getParent(source);
        DefaultGraphCell tar = (DefaultGraphCell) getModel().getParent(target);
        Object[] oa = new Object[]{src, tar};
        AssemblyController controller = (AssemblyController) parent.getController();

        switch (parent.getCurrentMode()) {
            case AssemblyEditViewFrame.ADAPTER_MODE:
                controller.newAdapterArc(oa);
                break;
            case AssemblyEditViewFrame.SIM_EVENT_LISTENER_MODE:
                controller.newSimEvListArc(oa);
                break;
            case AssemblyEditViewFrame.PROPERTY_CHANGE_LISTENER_MODE:
                controller.newPropertyChangeListArc(oa);
                break;
            default:
                break;
        }
    }

    final static double DEFAULT_CELL_SIZE = 54.0d;

    /** Create the cells attributes before rendering on the graph.  The
 edge attributes are set in the JGraphAssemblyModel
     *
     * @param node the named AssemblyNode to create attributes for
     * @return the cells attributes before rendering on the graph
     */
    public Map createCellAttributes(AssemblyNode node)
	{
        Map map = new Hashtable();
        Point2D point = node.getPosition();
		double assemblyZoomFactor = ViskitGlobals.instance().getAssemblyEditViewFrame().getCurrentZoomFactor();

        // Snap the Point to the Grid
        if (this != null) {
            point = snap((Point2D) point.clone());
        } else {
            point = (Point2D) point.clone();
        }
		point = ViskitGlobals.snapToGrid (point, assemblyZoomFactor); // utilize Viskit grid size rather that faulty jGraph snap

        // Add a Bounds Attribute to the Map
        GraphConstants.setBounds(map, new Rectangle2D.Double(
                point.getX(),
                point.getY(),
                DEFAULT_CELL_SIZE,
                DEFAULT_CELL_SIZE));

        GraphConstants.setBorder(map, BorderFactory.createRaisedBevelBorder());

        // Make sure the cell is resized on insert (doen't work)
//        GraphConstants.setResize(map, true);

        GraphConstants.setBackground(map, Color.black.darker());
        GraphConstants.setForeground(map, Color.white);
        GraphConstants.setFont(map, GraphConstants.DEFAULTFONT.deriveFont(Font.BOLD, 12));

        // Add a nice looking gradient background
//        GraphConstants.setGradientColor(map, Color.blue);
        // Add a Border Color Attribute to the Map
//        GraphConstants.setBorderColor(map, Color.black);
        // Add a White Background
//        GraphConstants.setBackground(map, Color.white);

        // Make Vertex Opaque
        GraphConstants.setOpaque(map, true);
        return map;
    }

    /**
     * Creates a DefaultGraphCell with a given name
     * @param node the named AssemblyNode
     * @return a DefaultGraphCell with a given name
     */
    protected DefaultGraphCell createDefaultGraphCell(AssemblyNode node) {

        DefaultGraphCell cell;
        if (node instanceof EventGraphNode) {
            cell = new AssemblyCircleCell(node);
        } else {
            cell = new AssemblyPropertyListCell(node);
        }

        node.opaqueViewObject = cell;

        // Add one Floating Port
        cell.add(new vAssemblyPortCell(node.getName() + "/Center"));
        return cell;
    }

    /** Insert a new Vertex at point
     *
     * @param node the AssemblyNode to insert
     */
    public void insert(AssemblyNode node) {
        DefaultGraphCell vertex = createDefaultGraphCell(node);

        // Create a Map that holds the attributes for the Vertex
        vertex.getAttributes().applyMap(createCellAttributes(node));

        // Insert the Vertex (including child port and attributes)
        getGraphLayoutCache().insert(vertex);

        vGAModel.reDrawNodes();
    }
}

/**        Extended JGraph Classes
 * ********************************************
 */

/**
 * To mark our edges.
 */
class vAssemblyEdgeCell extends DefaultEdge {

    public vAssemblyEdgeCell() {
        this(null);
    }

    public vAssemblyEdgeCell(Object userObject) {
        super(userObject);
    }
}

class vAssemblyPortCell extends DefaultPort {

    public vAssemblyPortCell() {
        this(null);
    }

    public vAssemblyPortCell(Object o) {
        this(o, null);
    }

    public vAssemblyPortCell(Object o, Port port) {
        super(o, port);
    }
}

class vAssemblyPortView extends PortView {

    static int mysize = 10;   // smaller than the circle

    public vAssemblyPortView(Object o) {
        super(o);
        setPortSize(mysize);
    }
}

/***********************************************/

/**
 * To mark our nodes.
 */
class AssemblyPropertyListCell extends DefaultGraphCell {

    AssemblyPropertyListCell() {
        this(null);
    }

    public AssemblyPropertyListCell(Object userObject) {
        super(userObject);
    }
}

/**
 * Sub class VertexView to install our own vapvr.
 */
class AssemblyPropListView extends VertexView {

    static JGraphAssemblyPclVertexRenderer vapvr = new JGraphAssemblyPclVertexRenderer();

    public AssemblyPropListView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vapvr;
    }
}

class AssemblyCircleCell extends DefaultGraphCell {

    AssemblyCircleCell() {
        this(null);
    }

    public AssemblyCircleCell(Object userObject) {
        super(userObject);
    }
}

/**
 * Sub class VertexView to install our own vapvr.
 */
class AssemblyCircleView extends VertexView {

    static JGraphAssemblyEgVertexRenderer vaevr = new JGraphAssemblyEgVertexRenderer();

    public AssemblyCircleView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vaevr;
    }
}

// Begin support for custom line ends and double line (adapter) on assembly edges
class vAssyAdapterEdgeView extends vEdgeView {

    static vAssyAdapterEdgeRenderer vaaer = new vAssyAdapterEdgeRenderer();

    public vAssyAdapterEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vaaer;
    }
}

class vAssySelEdgeView extends vEdgeView {

    static JGraphAssemblySelectedEdgeRenderer vaser = new JGraphAssemblySelectedEdgeRenderer();

    public vAssySelEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vaser;
    }
}

class vAssyPclEdgeView extends vEdgeView {

    static vAssyPclEdgeRenderer vaper = new vAssyPclEdgeRenderer();

    public vAssyPclEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vaper;
    }
}

class vAssyAdapterEdgeRenderer extends JGraphEdgeRenderer {

    /**
     * Paint the vaaer. Overridden to do a double line and paint over the end
     * shape
     */
    @Override
    public void paint(Graphics g) {
        Shape edgeShape = view.getShape();
        // Sideeffect: beginShape, lineShape, endShape
        if (edgeShape != null) {
            Graphics2D g2 = (Graphics2D) g;
            int c = BasicStroke.CAP_BUTT;
            int j = BasicStroke.JOIN_MITER;

            BasicStroke lineStroke = new BasicStroke(lineWidth, c, j);
            BasicStroke whiteStripeStroke = new BasicStroke(lineWidth / 3, c, j);
            BasicStroke onePixStroke = new BasicStroke(1, c, j);

            g2.setStroke(onePixStroke);

            translateGraphics(g);
            g.setColor(getForeground());
            if (view.beginShape != null) {
                if (beginFill) {
                    g2.fill(view.beginShape);
                }
                g2.draw(view.beginShape);
            }
            if (view.endShape != null) {
                if (endFill) {
                    g2.fill(view.endShape);
                }
                g2.draw(view.endShape);
            }
            g2.setStroke(lineStroke);
            if (lineDash != null) {// Dash For Line Only
                g2.setStroke(new BasicStroke(lineWidth, c, j, 10.0f, lineDash, 0.0f));
                whiteStripeStroke = new BasicStroke(lineWidth / 3, c, j, 10.0f, lineDash, 0.0f);
            }
            if (view.lineShape != null) {
                g2.draw(view.lineShape);

                g2.setColor(Color.white);
                g2.setStroke(whiteStripeStroke);
                g2.draw(view.lineShape);
                g2.setColor(getForeground());
            }
            if (selected) { // Paint Selected
                g2.setStroke(GraphConstants.SELECTION_STROKE);
                g2.setColor(((JGraph) graph.get()).getHighlightColor());
                if (view.beginShape != null) {
                    g2.draw(view.beginShape);
                }
                if (view.lineShape != null) {
                    g2.draw(view.lineShape);
                }
                if (view.endShape != null) {
                    g2.draw(view.endShape);
                }
            }
            if (((JGraph) graph.get()).getEditingCell() != view.getCell()) {
                Object label = ((JGraph) graph.get()).convertValueToString(view);
                if (label != null) {
                    g2.setStroke(new BasicStroke(1));
                    g.setFont(getFont());

                    // TODO: verify label rendering here
                    paintLabel(g, label.toString(), ((JGraph) graph.get()).getCenterPoint(), true);
                }
            }
        }
    }

    @Override
    protected Shape createLineEnd(int size, int style, Point2D src, Point2D dst)
	{
        double d = Math.max(1, dst.distance(src));
        double ax = -(size * (dst.getX() - src.getX()) / d);
        double ay = -(size * (dst.getY() - src.getY()) / d);
        GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
        path.moveTo(dst.getX() - ay / 3, dst.getY() + ax / 3);
        path.lineTo(dst.getX() + ax / 2, dst.getY() + ay / 2);
        path.lineTo(dst.getX() + ay / 3, dst.getY() - ax / 3);

        return path;
    }
}

class JGraphAssemblySelectedEdgeRenderer extends JGraphEdgeRenderer {

    @Override
    protected Shape createLineEnd(int size, int style, Point2D src, Point2D dst) {
        // Same as above
        double d = Math.max(1, dst.distance(src));
        double ax = -(size * (dst.getX() - src.getX()) / d);
        double ay = -(size * (dst.getY() - src.getY()) / d);
        GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
        path.moveTo(dst.getX() - ay / 3, dst.getY() + ax / 3);
        path.lineTo(dst.getX() + ax / 2, dst.getY() + ay / 2);
        path.lineTo(dst.getX() + ay / 3, dst.getY() - ax / 3);

        return path;
    }
}

class vAssyPclEdgeRenderer extends JGraphEdgeRenderer {

    @Override
    protected Shape createLineEnd(int size, int style, Point2D src, Point2D dst) {
        double d = Math.max(1, dst.distance(src));
        double ax = -(size * (dst.getX() - src.getX()) / d);
        double ay = -(size * (dst.getY() - src.getY()) / d);
        GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
        path.moveTo(dst.getX() - ay / 3, dst.getY() + ax / 3);
        path.lineTo(dst.getX() + ax / 2 - ay / 3, dst.getY() + ay / 2 + ax / 3);
        path.lineTo(dst.getX() + ax / 2 + ay / 3, dst.getY() + ay / 2 - ax / 3);
        path.lineTo(dst.getX() + ay / 3, dst.getY() - ax / 3);

        return path;
    }
}
// End support for custom line ends and double adapter line on assembly edges
// end class file vgraphAssemblyComponent.java
