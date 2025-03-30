package viskit.jgraph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
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

import viskit.view.EventGraphViewFrame;
import viskit.model.ModelEvent;
import viskit.control.EventGraphController;
import viskit.model.*;
import viskit.model.Edge;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute.
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey
 * @since Feb 19, 2004
 * @since 2:54:31 PM
 * @version $Id$
 */
public class ViskitGraphComponent extends JGraph implements GraphModelListener
{
    static final Logger LOG = LogManager.getLogger();
    
    ViskitGraphModel vGModel; // local copy for convenience
    EventGraphViewFrame parent;

    private UndoManager undoManager;

    /** Sets up JGraph to render nodes and edges for DES
     *
     * @param model a model of the node with its specific edges
     * @param frame the main view frame canvas to render to
     */
    public ViskitGraphComponent(ViskitGraphModel model, EventGraphViewFrame frame) {
        super(model);
        parent = frame;

        ViskitGraphComponent instance = this;
        ToolTipManager.sharedInstance().registerComponent(instance);
        this.vGModel = model;
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
        undoManager = new ViskitGraphUndoManager(parent.getController());
        this.addGraphSelectionListener((GraphSelectionListener) undoManager);
        model.addUndoableEditListener(undoManager);
        model.addGraphModelListener(instance);

        // As of JGraph-5.2, custom cell rendering is
        // accomplished via this convention
        this.getGraphLayoutCache().setFactory(new DefaultCellViewFactory() {

            // To use circles, from the tutorial
            @Override
            protected VertexView createVertexView(Object v) {
                VertexView view;
                if (v instanceof vCircleCell) {
                    view = new VertexCircleView(v);
                } else {
                    view = super.createVertexView(v);
                }
                return view;
            }

            // To customize edges
            @Override
            protected EdgeView createEdgeView(Object e) {
                EdgeView view;
                if (e instanceof vSelfEdgeCell) // order important... 1st is sub of 2nd
                {
                    view = new vSelfEdgeView(e);
                } else if (e instanceof vEdgeCell) {
                    view = new vEdgeView(e);
                } else {
                    view = super.createEdgeView(e);
                }
                return view;
            }

            @Override
            protected PortView createPortView(Object p) {
                PortView view;
                if (p instanceof vPortCell) {
                    view = new vPortView(p);
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
        setUI(new ViskitGraphUI());    // we use our own for node/edge inspector editing
        invalidate();
    }

    @Override // Prevents the NPE on macOS
    public AccessibleContext getAccessibleContext() {
        return parent.getCurrentJgraphComponent().getAccessibleContext();
    }

    /**
     * Returns the Viskit element at the given point
     * @param p the point of the Viskit element
     * @return the Viskit element at the given point
     */
    public ViskitElement getViskitElementAt(Point p) {
        Object cell = getFirstCellForLocation(p.x, p.y);
        if (cell != null && cell instanceof vCircleCell) {
            return (ViskitElement) ((DefaultMutableTreeNode) cell).getUserObject();
        }
        return null;
    }
    private ModelEvent currentModelEvent = null;

    public void viskitModelChanged(ModelEvent ev) {
        currentModelEvent = ev;

        switch (ev.getID()) {
            case ModelEvent.NEW_MODEL:

                // Ensure we start fresh
                vGModel.deleteAll();
                break;
            case ModelEvent.EVENT_ADDED:

                // Reclaimed from the vGModel to here
                insert((EventNode) ev.getSource());
                break;
            case ModelEvent.EVENT_CHANGED:
                vGModel.changeEvent((EventNode) ev.getSource());
                break;
            case ModelEvent.EVENT_DELETED:
                vGModel.deleteEventNode((EventNode) ev.getSource());
                break;
            case ModelEvent.EDGE_ADDED:
                vGModel.addEdge((Edge) ev.getSource());
                break;
            case ModelEvent.EDGE_CHANGED:
                vGModel.changeEdge((Edge) ev.getSource());
                break;
            case ModelEvent.EDGE_DELETED:
                vGModel.deleteEdge((Edge) ev.getSource());
                break;
            case ModelEvent.CANCELING_EDGE_ADDED:
                vGModel.addCancelEdge((Edge) ev.getSource());
                break;
            case ModelEvent.CANCELING_EDGE_CHANGED:
                vGModel.changeCancelingEdge((Edge) ev.getSource());
                break;
            case ModelEvent.CANCELING_EDGE_DELETED:
                vGModel.deleteCancelingEdge((Edge) ev.getSource());
                break;

            // Deliberate fall-through for these b/c the JGraph internal model
            // keeps track
            case ModelEvent.REDO_CANCELING_EDGE:
            case ModelEvent.REDO_SCHEDULING_EDGE:
            case ModelEvent.REDO_EVENT_NODE:
            case ModelEvent.UNDO_CANCELING_EDGE:
            case ModelEvent.UNDO_SCHEDULING_EDGE:
            case ModelEvent.UNDO_EVENT_NODE:
                vGModel.reDrawNodes();
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
        if (currentModelEvent != null && currentModelEvent.getID() == ModelEvent.NEW_MODEL) {
            return;
        } // this came in from outside, we don't have to inform anybody..prevent reentry

        // TODO: confirm any other events that should cause us to bail here
        GraphModelEvent.GraphModelChange c = graphModelEvent.getChange();
        Object[] ch = c.getChanged();

        // bounds (position) might have changed:
        if (ch != null) {
            vCircleCell cc;
            AttributeMap m;
            Rectangle2D.Double r;
            EventNode en;
            for (Object cell : ch) {
                if (cell instanceof vCircleCell) {
                    cc = (vCircleCell) cell;
                    m = cc.getAttributes();
                    r = (Rectangle2D.Double) m.get("bounds");
                    if (r != null) {
                        en = (EventNode) cc.getUserObject();
                        en.setPosition(new Point2D.Double(r.x, r.y));
                        ((Model) parent.getModel()).changeEventNode(en);
                        m.put("bounds", m.createRect(en.getPosition().getX(), en.getPosition().getY(), r.width, r.height));
                    }
                }
            }
        }
    }
    /** escape less-than and greater-than HTML characters */
    private String escapeLTGT(String htmlString) 
    {
        htmlString = htmlString.replaceAll("<", "&lt;");
        htmlString = htmlString.replaceAll(">", "&gt;");
        return htmlString;
    }

    @Override
    public String getToolTipText(MouseEvent mouseEvent) 
    {
        if (mouseEvent != null) {
            Object firstCell = this.getFirstCellForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (firstCell != null) 
            {
                StringBuilder htmlBuilder = new StringBuilder("<html>");
                if (firstCell instanceof vEdgeCell) 
                {
                    vEdgeCell edgeCell = (vEdgeCell) firstCell;
                    Edge edge = (Edge) edgeCell.getUserObject();

                    if (edge instanceof SchedulingEdge) {

                        if  (edgeCell instanceof vSelfEdgeCell)
                             htmlBuilder.append("<p align='center'>Self Scheduling Edge</p>");
                        else htmlBuilder.append("<p align='center'>Scheduling Edge</p>");

                        if (!edge.getDescription().isBlank() || (edge.conditionalDescription != null))
                        {
                            String newDescription = edge.getDescription().trim();
                            if (edge.conditionalDescription != null)
                            {
                                if (!newDescription.isBlank() && !newDescription.endsWith(".") && 
                                    !edge.conditionalDescription.isBlank())
                                    newDescription += ".";
                                newDescription += " " + edge.conditionalDescription.trim();                                
                            }
                            if (newDescription.length() > 0) 
                            {
                                htmlBuilder.append("<u>description</u><br>");
                                htmlBuilder.append(wrapStringAtPosition(escapeLTGT(newDescription), 60));
                                htmlBuilder.append("<br>");
                            }
                        }

                        double priority;
                        String s;

                        // Assume numeric comes in, avoid NumberFormatException via Regex check
                        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, ((SchedulingEdge) edge).priority))
                        {
                            priority = Double.parseDouble(((SchedulingEdge) edge).priority);
                            NumberFormat decimalFormat = DecimalFormat.getNumberInstance();
                            decimalFormat.setMaximumFractionDigits(3);
                            decimalFormat.setMaximumIntegerDigits(3);
                            if (Double.compare(priority, Double.MAX_VALUE) >= 0) {
                                s = "MAX";
                            } else if (Double.compare(priority, -Double.MAX_VALUE) <= 0) {
                                s = "MIN";
                            } else {
                                s = decimalFormat.format(priority);
                            }
                        } else {
                            s = ((SchedulingEdge) edge).priority;
                        }

                        htmlBuilder.append("<u>priority</u><br>&nbsp;");
                        htmlBuilder.append(s);
                        htmlBuilder.append("<br>");

                        if (edge.delay != null) {
                            String dly = edge.delay.trim();
                            if (dly.length() > 0) {
                                htmlBuilder.append("<u>delay</u><br>&nbsp;");
                                htmlBuilder.append(dly);
                                htmlBuilder.append("<br>");
                            }
                        }

                        int idx = 1;
                        if (!edge.parameters.isEmpty()) {

                            htmlBuilder.append("<u>edge parameters</u><br>");
                            ViskitEdgeParameter ep;
                            for (ViskitElement e : edge.parameters) {
                                ep = (ViskitEdgeParameter) e;
                                htmlBuilder.append("&nbsp;");
                                htmlBuilder.append(idx++);
                                htmlBuilder.append(" ");
                                htmlBuilder.append(ep.getValue());

                                if (ep.getType() != null && !ep.getType().isEmpty()) {
                                    htmlBuilder.append(" ");
                                    htmlBuilder.append("(");
                                    htmlBuilder.append(ep.getType());
                                    htmlBuilder.append(")");
                                }
                                htmlBuilder.append("<br>");
                            }
                        }

                    }
                    else 
                    {
                        if  (edgeCell instanceof vSelfEdgeCell)
                             htmlBuilder.append("<p align='center'>Self-Canceling Edge</p>");
                        else htmlBuilder.append("<p align='center'>Canceling Edge</p>");
                    }

                    if (edge != null && edge.conditionalDescription != null) {
                        String newDescription = edge.conditionalDescription.trim();
                        if (newDescription.length() > 0) 
                        {
                            htmlBuilder.append("<u>description</u><br>");
                            htmlBuilder.append(wrapStringAtPosition(escapeLTGT(newDescription), 60));
                            htmlBuilder.append("<br>");
                        }
                    }

                    if (edge != null && edge.conditional != null) {
                        String conditional = edge.conditional.trim();
                        if (conditional.length() > 0) {
                            htmlBuilder.append("<u>condition</u><br>&nbsp;if ( <b>");
                            htmlBuilder.append(escapeLTGT(conditional));
                            htmlBuilder.append("</b> )<br>");
                        }
                    }

                    // Strip out the last <br>
                    if (htmlBuilder.substring(htmlBuilder.length() - 4).equalsIgnoreCase("<br>")) {
                        htmlBuilder.setLength(htmlBuilder.length() - 4);
                    }
                    htmlBuilder.append("</html>");
                    return htmlBuilder.toString();
                }
                else if (firstCell instanceof vCircleCell) 
                {
                    vCircleCell circleCell = (vCircleCell) firstCell;
                    EventNode eventNode = (EventNode) circleCell.getUserObject();
                    htmlBuilder.append("<p align='center'>");

                    // Show event node names w/ corresponding parameters if any
                    String nodeName = eventNode.getName();
                    String[] arr = nodeName.split("_");

                    if (arr.length > 1) {
                        htmlBuilder.append(arr[0]);
                        htmlBuilder.append("<br>");
                        htmlBuilder.append("(");
                        htmlBuilder.append(arr[1]);
                        htmlBuilder.append(")");
                        htmlBuilder.append("<br>");
                    } 
                    else {
                        htmlBuilder.append(nodeName);
                        htmlBuilder.append(" Event Node");
                    }
                    htmlBuilder.append("</p>");

//                    if (!eventNode.getComments().isEmpty()) {
//                        String stripBrackets = eventNode.getComments().get(0).trim();
//                        if (stripBrackets.length() > 0) {
//                            htmlBuilder.append("<u>description</u><br>");
//                            htmlBuilder.append(wrapStringAtPosition(escapeLTGT(stripBrackets), 60));
//                            htmlBuilder.append("<br>");
//                        }
//                    }
                    if (!eventNode.getDescription().isEmpty()) {
                        String stripBrackets = eventNode.getDescription().trim();
                        if (stripBrackets.length() > 0) {
                            htmlBuilder.append("<u>description</u><br>");
                            htmlBuilder.append(wrapStringAtPosition(escapeLTGT(stripBrackets), 60));
                            htmlBuilder.append("<br>");
                        }
                    }

                    List<ViskitElement> argumentsList = eventNode.getArguments();
                    if (!argumentsList.isEmpty()) {

                        htmlBuilder.append("<u>arguments</u><br>");
                        int n = 0;
                        EventArgument eventArgument;
                        String value;
                        for (ViskitElement viskitElement : argumentsList) {
                            eventArgument = (EventArgument) viskitElement;
                            value = eventArgument.getName() + " (" + eventArgument.getType() + ")";
                            htmlBuilder.append("&nbsp;");
                            htmlBuilder.append(++n);
                            htmlBuilder.append(" ");
                            htmlBuilder.append(value);
                            htmlBuilder.append("<br>");
                        }
                    }

                    List<ViskitElement> localVariableList = eventNode.getLocalVariables();
                    if (!localVariableList.isEmpty()) {

                        htmlBuilder.append("<u>Local variables</u><br>");
                        EventLocalVariable eventLocalVariable;
                        String value;
                        for (ViskitElement nextLocalVariable : localVariableList) {
                            eventLocalVariable = (EventLocalVariable) nextLocalVariable;
                            htmlBuilder.append("&nbsp;");
                            htmlBuilder.append(eventLocalVariable.getName());
                            htmlBuilder.append(" (");
                            htmlBuilder.append(eventLocalVariable.getType());
                            htmlBuilder.append(") = ");
                            value = eventLocalVariable.getValue();
                            htmlBuilder.append(value.isEmpty() ? "<i><default></i>" : value);
                            htmlBuilder.append("<br>");
                        }
                    }

                    String codeBlockString = eventNode.getCodeBlockString();
                    if (codeBlockString != null && !codeBlockString.isEmpty()) {
                        htmlBuilder.append("<u>code block</u><br>");

                        String[] sa = codeBlockString.split("\\n");
                        for (String s : sa) {
                            htmlBuilder.append("&nbsp;");
                            htmlBuilder.append(s);
                            htmlBuilder.append("<br>");
                        }
                    }

                    List<ViskitElement> stateTransitionsList = eventNode.getStateTransitions();
                    if (!stateTransitionsList.isEmpty()) {

                        htmlBuilder.append("<u>state transitions</u><br>");
                        EventStateTransition est;
                        String[] sa;
                        for (ViskitElement ve : stateTransitionsList) {
                            est = (EventStateTransition) ve;
                            sa = est.toString().split("\\n");
                            for (String s : sa) {
                                htmlBuilder.append("&nbsp;");
                                htmlBuilder.append(s);
                                htmlBuilder.append("<br>");
                            }
                        }
                    }

                    // Strip out the last <br>
                    if (htmlBuilder.substring(htmlBuilder.length() - 4).equalsIgnoreCase("<br>")) {
                        htmlBuilder.setLength(htmlBuilder.length() - 4);
                    }
                    htmlBuilder.append("</html>");
                    return htmlBuilder.toString();
                }
            }
        }
        return null;
    }

    private String wrapStringAtPosition(String s, int length) {
        String[] sa = s.split(" ");
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        int ll;
        do {
            ll = 0;
            sb.append("&nbsp;");
            do {
                ll += sa[idx].length() + 1;
                sb.append(sa[idx++]);
                sb.append(" ");
            } while (idx < sa.length && ll < length);
            sb.append("<br>");
        } while (idx < sa.length);

        String st = sb.toString();
        if (st.endsWith("<br>")) {
            st = st.substring(0, st.length() - 4);
        }
        return st.trim();
    }

    @Override // Don't return null, fix for Issue #1
    public String convertValueToString(Object value) {
        CellView view = (value instanceof CellView)
                ? (CellView) value
                : getGraphLayoutCache().getMapping(value, false);

        String retVal = null;
        if (view instanceof VertexCircleView) {
            vCircleCell cc = (vCircleCell) view.getCell();
            Object en = cc.getUserObject();

            if (en instanceof EventNode) // should always be, except for our prototype examples
                retVal = ((ViskitElement) en).getName();

        } else if (view instanceof vEdgeView) {
            vEdgeCell cc = (vEdgeCell) view.getCell();
            Object e = cc.getUserObject();

            if (e instanceof SchedulingEdge) {
                SchedulingEdge se = (SchedulingEdge) e;

                if (se.conditional == null || se.conditional.isEmpty())
                    retVal = "";
                else
                    retVal = "\u01A7"; // https://www.compart.com/en/unicode/U+01A7
            } else if (e instanceof CancelingEdge) // should always be one of these 2 except for proto examples
                retVal = "";
        }
        return retVal;
    }

    /**
     * @return the undoManager
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /** Inserts a new Edge between source and target nodes
     *
     * @param source the source node to connect
     * @param target the target node to connect
     */
    public void connect(Port source, Port target) {

        DefaultGraphCell src = (DefaultGraphCell) getModel().getParent(source);
        DefaultGraphCell tar = (DefaultGraphCell) getModel().getParent(target);
        Object[] oa = new Object[]{src, tar};
        EventGraphController controller = (EventGraphController) parent.getController();
        if (parent.getCurrentMode() == EventGraphViewFrame.CANCEL_ARC_MODE) {
            controller.buildNewCancelingArc(oa);
        } else {
            controller.buildNewSchedulingArc(oa);
        }
    }

    final static double DEFAULT_CELL_SIZE = 54.0d;

    /** Create the cell's final attributes before rendering on the graph. The
     * edge attributes are set in the vGraphModel
     *
     * @param node the named EventNode to create attributes for
     * @return the cells attributes before rendering on the graph
     */
    public Map createCellAttributes(EventNode node) {
        Map map = new Hashtable();
        Point2D point = node.getPosition();

        // Snap the Point to the Grid
        if (this != null) {
            point = snap((Point2D) point.clone());
        } else {
            point = (Point2D) point.clone();
        }

        // Add a Bounds Attribute to the Map.  NOTE: using the length of the
        // node name to help size the cell does not bode well with the
        // customized edge router, so, leave it at DEFAULT_CELL_SIZE
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
     * @param node the named EventNode
     * @return a DefaultGraphCell with a given name
     */
    protected DefaultGraphCell createDefaultGraphCell(EventNode node) {

        DefaultGraphCell cell = new vCircleCell(node);
        node.opaqueViewObject = cell;

        // Add one Floating Port
        cell.add(new vPortCell(node.getName() + "/Center"));
        return cell;
    }

    /** Insert a new Vertex at point
     * @param node the EventNode to insert
     */
    public void insert(EventNode node) {
        DefaultGraphCell vertex = createDefaultGraphCell(node);

        // Create a Map that holds the attributes for the Vertex
        vertex.getAttributes().applyMap(createCellAttributes(node));

        // Insert the Vertex (including child port and attributes)
        getGraphLayoutCache().insert(vertex);

        vGModel.reDrawNodes();
    }
}

/**         Extended JGraph Classes
 * ********************************************
 */

/**
 * To mark our edges.
 */
class vEdgeCell extends DefaultEdge {

    public vEdgeCell(Object userObject) {
        super(userObject);
    }

    @Override
    public String toString() {
        return "";
    }
}

class vSelfEdgeCell extends vEdgeCell {

    public vSelfEdgeCell(Object userObject) {
        super(userObject);
    }
}

class vPortCell extends DefaultPort {

    public vPortCell(Object o) {
        this(o, null);
    }

    public vPortCell(Object o, Port port) {
        super(o, port);
    }
}

class vPortView extends PortView {

    static int mysize = 10;   // smaller than the circle

    public vPortView(Object o) {
        super(o);
        setPortSize(mysize);
    }
}

/**
 * Sub class EdgeView to install our own localRenderer.
 */
class vEdgeView extends EdgeView {

    static ViskitGraphEdgeRenderer localRenderer = new ViskitGraphEdgeRenderer();

    public vEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer;
    }
}

/**
 * Sub class EdgeView to support self-referring edges
 */
class vSelfEdgeView extends vEdgeView {

    static ViskitGraphSelfEdgeRenderer localRenderer2 = new ViskitGraphSelfEdgeRenderer();

    public vSelfEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer2;
    }
}

/**
 * To mark our nodes.
 */
class vCircleCell extends DefaultGraphCell {

    public vCircleCell(Object userObject) {
        super(userObject);
    }
}

/**
 * Sub class VertexView to install our own localRenderer.
 */
class VertexCircleView extends VertexView {

    static ViskitGraphVertexRenderer localRenderer = new ViskitGraphVertexRenderer();

    public VertexCircleView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer;
    }
}
