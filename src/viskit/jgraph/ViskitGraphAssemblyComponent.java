package viskit.jgraph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.undo.UndoManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.*;

import viskit.view.AssemblyViewFrame;
import viskit.model.ModelEvent;
import viskit.control.AssemblyController;
import viskit.control.AssemblyControllerImpl;
import viskit.model.*;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects MOVES Institute.Naval
 Postgraduate School, Monterey, CA
 *
 * @author Mike Bailey
 * @since Feb 19, 2004
 * @since 2:54:31 PM
 * @version $Id: ViskitGraphAssemblyComponent.java 2323 2012-06-19 23:11:11Z tdnorbra$
 */
public class ViskitGraphAssemblyComponent extends JGraph implements GraphModelListener 
{
    static final Logger LOG = LogManager.getLogger();

    ViskitGraphAssemblyModel viskitGraphAssemblyModel;
    AssemblyViewFrame        parentFrame;
    private UndoManager      undoManager;

    public ViskitGraphAssemblyComponent(ViskitGraphAssemblyModel graphAssemblyModel, AssemblyViewFrame assemblyViewFrame) 
    {
        super(graphAssemblyModel);
        this.parentFrame = assemblyViewFrame;

        ViskitGraphAssemblyComponent instance = this;
        ToolTipManager.sharedInstance().registerComponent(instance);
        this.viskitGraphAssemblyModel = graphAssemblyModel;
        this.setSizeable(false);
        this.setGridVisible(true);
        this.setGridMode(JGraph.LINE_GRID_MODE);
        this.setGridColor(new Color(0xcc, 0xcc, 0xff)); // default on Mac, makes Windows look better
        this.setGridEnabled(true); // means snap
        this.setGridSize(10);
        this.setMarqueeHandler(new ViskitGraphMarqueeHandler(instance));
        this.setAntiAliased(true);
        this.setLockedHandleColor(Color.red);
        this.setHighlightColor(Color.red);

        // Set the Tolerance to 2 Pixels
        this.setTolerance(2);

        // Jump to default port on connect
        this.setJumpToDefaultPort(true);

         // Set up the cut/remove/paste/copy/undo/redo actions
        undoManager = new ViskitGraphUndoManager(parentFrame.getController());
        this.addGraphSelectionListener((GraphSelectionListener) undoManager);
        graphAssemblyModel.addUndoableEditListener(undoManager);
        graphAssemblyModel.addGraphModelListener(instance);

        // As of JGraph-5.2, custom cell rendering is
        // accomplished via this convention
        this.getGraphLayoutCache().setFactory(new DefaultCellViewFactory() {

            // To use circles, from the tutorial
            @Override
            protected VertexView createVertexView(Object viskitObject) 
            {
                VertexView vertexView;
                if (viskitObject instanceof ViskitAssemblyCircleCell) {
                    vertexView = new ViskitAssemblyCircleView(viskitObject);
                } else if (viskitObject instanceof vAssemblyPropertyListCell) {
                    vertexView = new ViskitAssemblyPropListView(viskitObject);
                } else {
                    vertexView = super.createVertexView(viskitObject);
                }
                return vertexView;
            }

            // To customize my edges
            @Override
            protected EdgeView createEdgeView(Object e)
            {
                EdgeView edgeView = null;
                if (e instanceof ViskitAssemblyEdgeCell) {
                    Object o = ((DefaultMutableTreeNode) e).getUserObject();
                    if (o instanceof PropertyChangeEdge) {
                        edgeView = new ViskitAssemblyPropertyChangeListenerEdgeView(e);
                    }
                    if (o instanceof AdapterEdge) {
                        edgeView = new ViskitAssemblyAdapterEdgeView(e);
                    }
                    if (o instanceof SimEventListenerEdge) {
                        edgeView = new ViskitAssemblySelEdgeView(e);
                    }
                } else {
                    edgeView = super.createEdgeView(e);
                }
                return edgeView;
            }

            @Override
            protected PortView createPortView(Object p)
            {
                PortView portView;
                if (p instanceof ViskitAssemblyPortCell) {
                    portView = new ViskitAssemblyPortView(p);
                } else {
                    portView = super.createPortView(p);
                }
                return portView;
            }
        });
    }

    @Override
    public void updateUI() {
        // Install a new UI
        setUI(new ViskitGraphAssemblyUI());    // we use our own for node/edge inspector editting
        invalidate();
    }
    
    @Override // Prevents the NPE on macOS
    public AccessibleContext getAccessibleContext() {
        return parentFrame.getCurrentJgraphComponent().getAccessibleContext();
    }

    private ModelEvent currentModelEvent = null;

    public void viskitModelChanged(ModelEvent modelEvent) {
        currentModelEvent = modelEvent;

        switch (modelEvent.getID()) 
        {
            case ModelEvent.NEW_ASSEMBLY_MODEL:

                // Ensure we start fresh
                viskitGraphAssemblyModel.deleteAll();
                break;
            case ModelEvent.EVENT_GRAPH_ADDED:

                // Reclaimed from the viskitGraphAssemblyModel to here
                insert((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.EVENT_GRAPH_CHANGED:
                viskitGraphAssemblyModel.changeEventGraphNode((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.EVENT_GRAPH_DELETED:
                viskitGraphAssemblyModel.deleteEventGraphNode((AssemblyNode) modelEvent.getSource());
                break;

            case ModelEvent.PCL_ADDED:

                // Reclaimed from the viskitGraphAssemblyModel to here
                insert((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.PCL_CHANGED:
                viskitGraphAssemblyModel.changePCLNode((AssemblyNode) modelEvent.getSource());
                break;
            case ModelEvent.PCL_DELETED:
                viskitGraphAssemblyModel.deletePCLNode((AssemblyNode) modelEvent.getSource());
                break;

            case ModelEvent.ADAPTER_EDGE_ADDED:
                viskitGraphAssemblyModel.addAdapterEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.ADAPTER_EDGE_CHANGED:
                viskitGraphAssemblyModel.changeAdapterEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.ADAPTER_EDGE_DELETED:
                viskitGraphAssemblyModel.deleteAdapterEdge((AssemblyEdge) modelEvent.getSource());
                break;

            case ModelEvent.SIM_EVENT_LISTENER_EDGE_ADDED:
                viskitGraphAssemblyModel.addSimEvListEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.SIM_EVENT_LISTENER_EDGE_CHANGED:
                viskitGraphAssemblyModel.changeSimEvListEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.SIM_EVENT_LISTENER_EDGE_DELETED:
                viskitGraphAssemblyModel.deleteSimEvListEdge((AssemblyEdge) modelEvent.getSource());
                break;

            case ModelEvent.PCL_EDGE_ADDED:
                viskitGraphAssemblyModel.addPclEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.PCL_EDGE_DELETED:
                viskitGraphAssemblyModel.deletePclEdge((AssemblyEdge) modelEvent.getSource());
                break;
            case ModelEvent.PCL_EDGE_CHANGED:
                viskitGraphAssemblyModel.changePclEdge((AssemblyEdge) modelEvent.getSource());
                break;

            // Deliberate fall-through for these b/c the JGraph internal model
            // keeps track
            case ModelEvent.UNDO_EVENT_GRAPH:
            case ModelEvent.REDO_EVENT_GRAPH:
            case ModelEvent.UNDO_PCL:
            case ModelEvent.REDO_PCL:;
            case ModelEvent.UNDO_ADAPTER_EDGE:
            case ModelEvent.REDO_ADAPTER_EDGE:
            case ModelEvent.UNDO_SIM_EVENT_LISTENER_EDGE:
            case ModelEvent.REDO_SIM_EVENT_LISTENER_EDGE:
            case ModelEvent.UNDO_PCL_EDGE:
            case ModelEvent.REDO_PCL_EDGE:
                viskitGraphAssemblyModel.reDrawNodes();
                break;
            default:
            //LOG.info("duh");
        }
        currentModelEvent = null;
    }

    // TODO: This version JGraph does not support generics
    @SuppressWarnings("unchecked")
    @Override
    public void graphChanged(GraphModelEvent graphModelEvent) 
    {
        if (currentModelEvent != null && currentModelEvent.getID() == ModelEvent.NEW_ASSEMBLY_MODEL) // bail if this came from outside
        {
            return;
        } // this came in from outside, we don't have to inform anybody..prevent reentry

        // TODO: confirm any other events that should cause us to bail here
        GraphModelEvent.GraphModelChange graphModelChange = graphModelEvent.getChange();
        Object[] changeArray = graphModelChange.getChanged();

        // bounds (position) might have changed:
        if (changeArray != null) {
            ViskitAssemblyCircleCell assemblyCircleCell;
            AttributeMap attributeMap;
            Rectangle2D.Double r;
            EventGraphNode eventGraphNode;
            PropertyChangeListenerNode propertyChangeListenerNode;
            for (Object cellObject : changeArray) {
                if (cellObject instanceof ViskitAssemblyCircleCell) {
                    assemblyCircleCell = (ViskitAssemblyCircleCell) cellObject;
                    attributeMap = assemblyCircleCell.getAttributes();
                    r = (Rectangle2D.Double) attributeMap.get("bounds");
                    if (r != null) {
                        eventGraphNode = (EventGraphNode) assemblyCircleCell.getUserObject();
                        eventGraphNode.setPosition(new Point2D.Double(r.x, r.y));
                        ((AssemblyModel) parentFrame.getModel()).changeEventGraphNode(eventGraphNode);
                        attributeMap.put("bounds", attributeMap.createRect(eventGraphNode.getPosition().getX(), eventGraphNode.getPosition().getY(), r.width, r.height));
                    }
                } else if (cellObject instanceof vAssemblyPropertyListCell) {
                    vAssemblyPropertyListCell assemblyPropertyListCell = (vAssemblyPropertyListCell) cellObject;

                    attributeMap = assemblyPropertyListCell.getAttributes();
                    r = (Rectangle2D.Double) attributeMap.get("bounds");
                    if (r != null) {
                        propertyChangeListenerNode = (PropertyChangeListenerNode) assemblyPropertyListCell.getUserObject();
                        propertyChangeListenerNode.setPosition(new Point2D.Double(r.x, r.y));
                        ((AssemblyModel) parentFrame. getModel()).changePclNode(propertyChangeListenerNode);
                        attributeMap.put("bounds", attributeMap.createRect(propertyChangeListenerNode.getPosition().getX(), propertyChangeListenerNode.getPosition().getY(), r.width, r.height));
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
    public String getToolTipText(MouseEvent mouseEvent) 
    {
        if (mouseEvent != null) {
            Object cellObject = this.getFirstCellForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (cellObject != null) {
                StringBuilder sb = new StringBuilder("<html>");
                if (cellObject instanceof ViskitAssemblyEdgeCell) {
                    ViskitAssemblyEdgeCell assemblyEdgeCell = (ViskitAssemblyEdgeCell) cellObject;
                    AssemblyEdge  schedulingEdge = (AssemblyEdge) assemblyEdgeCell.getUserObject();
                    Object to   = schedulingEdge.getTo();
                    Object from = schedulingEdge.getFrom();

                    if (schedulingEdge instanceof AdapterEdge) {
                        Object   toTargetEvent = ((AdapterEdge) schedulingEdge).getTargetEvent();
                        Object fromTargetEvent = ((AdapterEdge) schedulingEdge).getSourceEvent();
                        sb.append("<center>Adapter<br><u>");// +
                        sb.append(from);
                        sb.append(".");
                        sb.append(fromTargetEvent);
                        sb.append("</u> connected to <u>");
                        sb.append(to);
                        sb.append(".");
                        sb.append(toTargetEvent);
                    } else if (schedulingEdge instanceof SimEventListenerEdge) {
                        sb.append("<center>SimEvent Listener<br><u>");
                        sb.append(to);
                        sb.append("</u> listening to <u>");
                        sb.append(from);
                    } else {
                        String propertyString = ((PropertyChangeEdge) schedulingEdge).getProperty();
                        propertyString = (propertyString != null && propertyString.length() > 0) ? propertyString : "*all*";
                        sb.append("<center>Property Change Listener<br><u>");
                        sb.append(to);
                        sb.append("</u> listening to <u>");
                        sb.append(from);
                        sb.append(".");
                        sb.append(propertyString);
                    }
                    String description = schedulingEdge.getDescription();
                    if (description != null) {
                        description = description.trim();
                        if (description.length() > 0) {
                            sb.append("<br>");
                            sb.append("<u> description: </u>");
                            sb.append(wrapAtPosition(escapeLTGT(description), 60));
                        }
                    }
                    sb.append("</center>");
                    sb.append("</html>");
                    return sb.toString();
                } 
                else if (cellObject instanceof ViskitAssemblyCircleCell || cellObject instanceof vAssemblyPropertyListCell) {
                    String type;
                    String name;
                    String description;
                    if (cellObject instanceof ViskitAssemblyCircleCell) {
                        ViskitAssemblyCircleCell cc = (ViskitAssemblyCircleCell) cellObject;
                        EventGraphNode eventGraphNode = (EventGraphNode) cc.getUserObject();
                        type = eventGraphNode.getType();
                        name = eventGraphNode.getName();
                        description = eventGraphNode.getDescription();
                    } else /*if (c instanceof vAssemblyPropertyListCell)*/ {
                        vAssemblyPropertyListCell assemblyPropertyListCell = (vAssemblyPropertyListCell) cellObject;
                        PropertyChangeListenerNode propertyChangeListenerNode = (PropertyChangeListenerNode) assemblyPropertyListCell.getUserObject();
                        type = propertyChangeListenerNode.getType();
                        name = propertyChangeListenerNode.getName();
                        description = propertyChangeListenerNode.getDescription();
                    }

                    sb.append("<center><u>");
                    sb.append(type);
                    sb.append("</u><br>");
                    sb.append(name);
                    if (description != null) {
                        description = description.trim();
                        if (description.length() > 0) {
                            sb.append("<br>");
                            sb.append("<u> description: </u>");
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

    private String wrapAtPosition(String s, int position) {
        String[] sa = s.split(" ");
        StringBuilder sb = new StringBuilder();
        int index = 0;
        do {
            int ll = 0;
            sb.append("&nbsp;");
            do {
                ll += sa[index].length() + 1;
                sb.append(sa[index++]);
                sb.append(" ");
            } while (index < sa.length && ll < position);
            sb.append("<br>");
        } while (index < sa.length);

        String st = sb.toString();
        if (st.endsWith("<br>")) {
            st = st.substring(0, st.length() - 4);
        }
        return st.trim();
    }

    @Override
    public String convertValueToString(Object value) {
        CellView cellView = (value instanceof CellView)
                ? (CellView) value
                : getGraphLayoutCache().getMapping(value, false);

        if (cellView instanceof ViskitAssemblyCircleView) {
            ViskitAssemblyCircleCell assemblyCircleCell = (ViskitAssemblyCircleCell) cellView.getCell();
            Object potentialEventGraphNode = assemblyCircleCell.getUserObject();
            if (potentialEventGraphNode instanceof EventGraphNode) {
                return ((ViskitElement) potentialEventGraphNode).getName();
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
     * @param targetPort the "to" of the connection
     */
    public void connect(Port sourcePort, Port targetPort) 
    {
        DefaultGraphCell sourceGraphCell = (DefaultGraphCell) getModel().getParent(sourcePort);
        DefaultGraphCell targetGraphCell = (DefaultGraphCell) getModel().getParent(targetPort);
        Object[] objectArray = new Object[]{sourceGraphCell, targetGraphCell};
        AssemblyController assemblyController = (AssemblyControllerImpl) parentFrame.getController();

        switch (parentFrame.getCurrentMode())
        {
            case AssemblyViewFrame.ADAPTER_MODE:
                assemblyController.newAdapterEdge(objectArray);
                break;
            case AssemblyViewFrame.SIMEVENT_LISTENER_MODE:
                assemblyController.newSimEventListenerEdge(objectArray);
                break;
            case AssemblyViewFrame.PROPERTY_CHANGE_LISTENER_MODE:
                assemblyController.newPropertyChangeListenerEdge(objectArray);
                break;
            default:
                break;
        }
    }

    final static double DEFAULT_CELL_SIZE = 54.0d;

    /** Create the cells attributes before rendering on the graph.  The
     * edge attributes are set in the vGraphAssemblyModel
     *
     * @param node the named AssemblyNode to create attributes for
     * @return the cells attributes before rendering on the graph
     */
    public Map createCellAttributes(AssemblyNode node) {
        Map map = new Hashtable();
        Point2D point = node.getPosition();

        // Snap the Point to the Grid
        if (this != null) {
            point = snap((Point2D) point.clone());
        } else {
            point = (Point2D) point.clone();
        }

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
     * @param assemblyNode the named AssemblyNode
     * @return a DefaultGraphCell with a given name
     */
    protected DefaultGraphCell createDefaultGraphCell(AssemblyNode assemblyNode) 
    {
        DefaultGraphCell defaultGraphCell = null;
        if (assemblyNode != null) {
            if (assemblyNode instanceof EventGraphNode) {
                defaultGraphCell = new ViskitAssemblyCircleCell(assemblyNode);
            } else {
                defaultGraphCell = new vAssemblyPropertyListCell(assemblyNode);
            }

            assemblyNode.opaqueViewObject = defaultGraphCell;

            // Add one Floating Port
            defaultGraphCell.add(new ViskitAssemblyPortCell(assemblyNode.getName() + "/Center"));
        }
        return defaultGraphCell;
    }

    /** Insert a new Vertex at point
     *
     * @param assemblyNode the AssemblyNode to insert
     */
    public void insert(AssemblyNode assemblyNode) 
    {
        DefaultGraphCell vertexCell = createDefaultGraphCell(assemblyNode);

        // Create a Map that holds the attributes for the Vertex
        vertexCell.getAttributes().applyMap(createCellAttributes(assemblyNode));

        // Insert the Vertex (including child port and attributes)
        getGraphLayoutCache().insert(vertexCell);

        viskitGraphAssemblyModel.reDrawNodes();
    }
    
} // end class ViskitGraphAssemblyComponent

/**        Extended JGraph Classes
 * ********************************************
 */

/**
 * To mark our edges.
 */
class ViskitAssemblyEdgeCell extends DefaultEdge {

    public ViskitAssemblyEdgeCell() {
        this(null);
    }

    public ViskitAssemblyEdgeCell(Object userObject) {
        super(userObject);
    }
}

class ViskitAssemblyPortCell extends DefaultPort {

    public ViskitAssemblyPortCell(Object o) {
        this(o, null);
    }

    public ViskitAssemblyPortCell(Object o, Port port) {
        super(o, port);
    }
}

class ViskitAssemblyPortView extends PortView {

    static int mysize = 10;   // smaller than the circle

    public ViskitAssemblyPortView(Object o) {
        super(o);
        setPortSize(mysize);
    }
}

/***********************************************/

/**
 * To mark our nodes.
 */
class vAssemblyPropertyListCell extends DefaultGraphCell {

    public vAssemblyPropertyListCell(Object userObject) {
        super(userObject);
    }
}

/**
 * Sub class VertexView to install our own vvr.
 */
class ViskitAssemblyPropListView extends VertexView {

    // shared with ViskitAssemblyCircleView
    static ViskitGraphVertexRenderer vvr = new ViskitGraphVertexRenderer();

    public ViskitAssemblyPropListView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vvr;
    }
}

class ViskitAssemblyCircleCell extends DefaultGraphCell {

    public ViskitAssemblyCircleCell(Object userObject) {
        super(userObject);
    }
}

class ViskitAssemblyCircleView extends VertexView {

    public ViskitAssemblyCircleView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return ViskitAssemblyPropListView.vvr;
    }
}

// Begin support for custom line ends and double line (adapter) on assembly edges
class ViskitAssemblyAdapterEdgeView extends vEdgeView {

    static ViskitAssemblyAdapterEdgeRenderer vaaer = new ViskitAssemblyAdapterEdgeRenderer();

    public ViskitAssemblyAdapterEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vaaer;
    }
}

/**
 * Sub class vEdgeView to install our own vaer.
 */
class ViskitAssemblySelEdgeView extends vEdgeView {

    // shared with ViskitAssemblyPropertyChangeListenerEdgeView
    static ViskitAssemblyEdgeRenderer vaer = new ViskitAssemblyEdgeRenderer();

    public ViskitAssemblySelEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return vaer;
    }
}

class ViskitAssemblyPropertyChangeListenerEdgeView extends vEdgeView {

    public ViskitAssemblyPropertyChangeListenerEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return ViskitAssemblySelEdgeView.vaer;
    }
}

class ViskitAssemblyAdapterEdgeRenderer extends ViskitGraphEdgeRenderer {

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

    // Shared with ViskitAssemblyEdgeRenderer.createLineEnd
    @Override
    protected Shape createLineEnd(int size, int style, Point2D src, Point2D dst) {
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

class ViskitAssemblyEdgeRenderer extends ViskitGraphEdgeRenderer {

    @Override
    protected Shape createLineEnd(int size, int style, Point2D src, Point2D dst) {
        return ViskitAssemblyAdapterEdgeView.vaaer.createLineEnd(size, style, src, dst);
    }
}
// End support for custom line ends and double adapter line on assembly edges
// end class file ViskitGraphAssemblyComponent.java
