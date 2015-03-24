package viskit.jgraph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.undo.UndoManager;
import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.*;
import viskit.view.AssemblyViewFrame;
import viskit.model.ModelEvent;
import viskit.control.AssemblyController;
import viskit.model.*;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects MOVES Institute. Naval
 * Postgraduate School, Monterey, CA
 *
 * @author Mike Bailey
 * @since Feb 19, 2004
 * @since 2:54:31 PM
 * @version $Id: vGraphAssemblyComponent.java 2323 2012-06-19 23:11:11Z tdnorbra$
 */
public class vGraphAssemblyComponent extends JGraph implements GraphModelListener {

    vGraphAssemblyModel vGAModel;
    AssemblyViewFrame parent;
    private UndoManager undoManager;

    public vGraphAssemblyComponent(vGraphAssemblyModel model, AssemblyViewFrame frame) {
        super(model);
        parent = frame;

        vGraphAssemblyComponent instance = this;
        ToolTipManager.sharedInstance().registerComponent(instance);
        this.vGAModel = model;
        this.setSizeable(false);
        this.setGridVisible(true);
        this.setGridMode(JGraph.LINE_GRID_MODE);
        this.setGridColor(new Color(0xcc, 0xcc, 0xff)); // default on Mac, makes Windows look better
        this.setGridEnabled(true); // means snap
        this.setGridSize(10);
        this.setMarqueeHandler(new vGraphMarqueeHandler(instance));
        this.setAntiAliased(true);
        this.setLockedHandleColor(Color.red);
        this.setHighlightColor(Color.red);

        // Set the Tolerance to 2 Pixel
        setTolerance(2);

        // Jump to default port on connect
        setJumpToDefaultPort(true);

         // Set up the cut/remove/paste/copy/undo/redo actions
        undoManager = new vGraphUndoManager(parent.getController());
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
                } else if (v instanceof AssemblyPropListCell) {
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
                    if (o instanceof PropChangeEdge) {
                        view = new vAssyPclEdgeView(e);
                    }
                    if (o instanceof AdapterEdge) {
                        view = new vAssyAdapterEdgeView(e);
                    }
                    if (o instanceof SimEvListenerEdge) {
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
        setUI(new vGraphAssemblyUI());    // we use our own for node/edge inspector editting
        //setUI(new BasicGraphUI());   // test
        invalidate();
    }

    private ModelEvent currentModelEvent = null;

    public void viskitModelChanged(ModelEvent ev) {
        currentModelEvent = ev;

        switch (ev.getID()) {
            case ModelEvent.NEWASSEMBLYMODEL:

                // Ensure we start fresh
                vGAModel.deleteAll();
                break;
            case ModelEvent.EVENTGRAPHADDED:

                // Reclaimed from the vGAModel to here
                insert((AssemblyNode) ev.getSource());
                break;
            case ModelEvent.EVENTGRAPHCHANGED:
                vGAModel.changeEGNode((AssemblyNode) ev.getSource());
                break;
            case ModelEvent.EVENTGRAPHDELETED:
                vGAModel.deleteEGNode((AssemblyNode) ev.getSource());
                break;

            case ModelEvent.PCLADDED:

                // Reclaimed from the vGAModel to here
                insert((AssemblyNode) ev.getSource());
                break;
            case ModelEvent.PCLCHANGED:
                vGAModel.changePCLNode((AssemblyNode) ev.getSource());
                break;
            case ModelEvent.PCLDELETED:
                vGAModel.deletePCLNode((AssemblyNode) ev.getSource());
                break;

            case ModelEvent.ADAPTEREDGEADDED:
                vGAModel.addAdapterEdge((AssemblyEdge) ev.getSource());
                break;
            case ModelEvent.ADAPTEREDGECHANGED:
                vGAModel.changeAdapterEdge((AssemblyEdge) ev.getSource());
                break;
            case ModelEvent.ADAPTEREDGEDELETED:
                vGAModel.deleteAdapterEdge((AssemblyEdge) ev.getSource());
                break;

            case ModelEvent.SIMEVLISTEDGEADDED:
                vGAModel.addSimEvListEdge((AssemblyEdge) ev.getSource());
                break;
            case ModelEvent.SIMEVLISTEDGECHANGED:
                vGAModel.changeSimEvListEdge((AssemblyEdge) ev.getSource());
                break;
            case ModelEvent.SIMEVLISTEDGEDELETED:
                vGAModel.deleteSimEvListEdge((AssemblyEdge) ev.getSource());
                break;

            case ModelEvent.PCLEDGEADDED:
                vGAModel.addPclEdge((AssemblyEdge) ev.getSource());
                break;
            case ModelEvent.PCLEDGEDELETED:
                vGAModel.deletePclEdge((AssemblyEdge) ev.getSource());
                break;
            case ModelEvent.PCLEDGECHANGED:
                vGAModel.changePclEdge((AssemblyEdge) ev.getSource());
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
    public void graphChanged(GraphModelEvent e) {
        if (currentModelEvent != null && currentModelEvent.getID() == ModelEvent.NEWASSEMBLYMODEL) // bail if this came from outside
        {
            return;
        } // this came in from outside, we don't have to inform anybody..prevent reentry

        // TODO: confirm any other events that should cause us to bail here
        GraphModelEvent.GraphModelChange c = e.getChange();
        Object[] ch = c.getChanged();
        if (ch != null) {
            for (Object cell : ch) {
                if (cell instanceof AssemblyCircleCell) {
                    AssemblyCircleCell cc = (AssemblyCircleCell) cell;
                    AttributeMap m = cc.getAttributes();
                    Rectangle2D.Double r = (Rectangle2D.Double) m.get("bounds");
                    if (r != null) {
                        EvGraphNode en = (EvGraphNode) cc.getUserObject();
                        en.setPosition(new Point2D.Double(r.x, r.y));
                        ((AssemblyModel) parent.getModel()).changeEvGraphNode(en);

                        // might have changed:
                        m.put("bounds", m.createRect(en.getPosition().getX(), en.getPosition().getY(), r.width, r.height));
                    }
                } else if (cell instanceof AssemblyPropListCell) {
                    AssemblyPropListCell plc = (AssemblyPropListCell) cell;

                    AttributeMap m = plc.getAttributes();
                    Rectangle2D.Double r = (Rectangle2D.Double) m.get("bounds");
                    if (r != null) {
                        PropChangeListenerNode pcln = (PropChangeListenerNode) plc.getUserObject();
                        pcln.setPosition(new Point2D.Double(r.x, r.y));
                        ((AssemblyModel) parent.getModel()).changePclNode(pcln);

                        // might have changed:
                        m.put("bounds", m.createRect(pcln.getPosition().getX(), pcln.getPosition().getY(), r.width, r.height));
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
    public String getToolTipText(MouseEvent event) {
        if (event != null) {
            Object c = this.getFirstCellForLocation(event.getX(), event.getY());
            if (c != null) {
                StringBuilder sb = new StringBuilder("<html>");
                if (c instanceof vAssemblyEdgeCell) {
                    vAssemblyEdgeCell vc = (vAssemblyEdgeCell) c;
                    AssemblyEdge se = (AssemblyEdge) vc.getUserObject();
                    Object to = se.getTo();
                    Object from = se.getFrom();

                    if (se instanceof AdapterEdge) {
                        Object toEv = ((AdapterEdge) se).getTargetEvent();
                        Object frEv = ((AdapterEdge) se).getSourceEvent();
                        sb.append("<center>Adapter<br><u>");// +
                        sb.append(from);
                        sb.append(".");
                        sb.append(frEv);
                        sb.append("</u> connected to <u>");
                        sb.append(to);
                        sb.append(".");
                        sb.append(toEv);
                    } else if (se instanceof SimEvListenerEdge) {
                        sb.append("<center>SimEvent Listener<br><u>");
                        sb.append(to);
                        sb.append("</u> listening to <u>");
                        sb.append(from);
                    } else {
                        String prop = ((PropChangeEdge) se).getProperty();
                        prop = (prop != null && prop.length() > 0) ? prop : "*all*";
                        sb.append("<center>Property Change Listener<br><u>");
                        sb.append(to);
                        sb.append("</u> listening to <u>");
                        sb.append(from);
                        sb.append(".");
                        sb.append(prop);
                    }
                    String desc = se.getDescriptionString();
                    if (desc != null) {
                        desc = desc.trim();
                        if (desc.length() > 0) {
                            sb.append("<br>");
                            sb.append("<u> description: </u>");
                            sb.append(wrapAtPos(escapeLTGT(desc), 60));
                        }
                    }
                    sb.append("</center>");
                    sb.append("</html>");
                    return sb.toString();
                } else if (c instanceof AssemblyCircleCell || c instanceof AssemblyPropListCell) {
                    String typ;
                    String name;
                    String desc;
                    if (c instanceof AssemblyCircleCell) {
                        AssemblyCircleCell cc = (AssemblyCircleCell) c;
                        EvGraphNode en = (EvGraphNode) cc.getUserObject();
                        typ = en.getType();
                        name = en.getName();
                        desc = en.getDescriptionString();
                    } else /*if (c instanceof AssemblyPropListCell)*/ {
                        AssemblyPropListCell cc = (AssemblyPropListCell) c;
                        PropChangeListenerNode pcln = (PropChangeListenerNode) cc.getUserObject();
                        typ = pcln.getType();
                        name = pcln.getName();
                        desc = pcln.getDescriptionString();
                    }

                    sb.append("<center><u>");
                    sb.append(typ);
                    sb.append("</u><br>");
                    sb.append(name);
                    if (desc != null) {
                        desc = desc.trim();
                        if (desc.length() > 0) {
                            sb.append("<br>");
                            sb.append("<u> description: </u>");
                            sb.append(wrapAtPos(escapeLTGT(desc), 60));
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

    private String wrapAtPos(String s, int len) {
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
    public String convertValueToString(Object value) {
        CellView view = (value instanceof CellView)
                ? (CellView) value
                : getGraphLayoutCache().getMapping(value, false);

        if (view instanceof AssemblyCircleView) {
            AssemblyCircleCell cc = (AssemblyCircleCell) view.getCell();
            Object en = cc.getUserObject();
            if (en instanceof EvGraphNode) {
                return ((EvGraphNode) en).getName();
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

        if (parent.getCurrentMode() == AssemblyViewFrame.ADAPTER_MODE) {
            controller.newAdapterArc(oa);
        } else if (parent.getCurrentMode() == AssemblyViewFrame.SIMEVLIS_MODE) {
            controller.newSimEvListArc(oa);
        } else if (parent.getCurrentMode() == AssemblyViewFrame.PCL_MODE) {
            controller.newPropChangeListArc(oa);
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
     * @param node the named AssemblyNode
     * @return a DefaultGraphCell with a given name
     */
    protected DefaultGraphCell createDefaultGraphCell(AssemblyNode node) {

        DefaultGraphCell cell;
        if (node instanceof EvGraphNode) {
            cell = new AssemblyCircleCell(node.getName());
        } else {
            cell = new AssemblyPropListCell(node.getName());
        }

        node.opaqueViewObject = cell;
        cell.setUserObject(node);

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
class AssemblyPropListCell extends DefaultGraphCell {

    AssemblyPropListCell() {
        this(null);
    }

    public AssemblyPropListCell(Object userObject) {
        super(userObject);
    }
}

/**
 * Sub class VertexView to install our own vapvr.
 */
class AssemblyPropListView extends VertexView {

    static vAssemblyPclVertexRenderer vapvr = new vAssemblyPclVertexRenderer();

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

    static vAssemblyEgVertexRenderer vaevr = new vAssemblyEgVertexRenderer();

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

    static vAssySelEdgeRenderer vaser = new vAssySelEdgeRenderer();

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

class vAssyAdapterEdgeRenderer extends vEdgeRenderer {

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

class vAssySelEdgeRenderer extends vEdgeRenderer {

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

class vAssyPclEdgeRenderer extends vEdgeRenderer {

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
