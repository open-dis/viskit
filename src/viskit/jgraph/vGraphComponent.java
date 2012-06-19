package viskit.jgraph;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;
import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.*;
import viskit.EventGraphViewFrame;
import viskit.ModelEvent;
import viskit.ViskitController;
import viskit.model.Edge;
import viskit.model.*;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute.
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey 
 * @since Feb 19, 2004
 * @since 2:54:31 PM
 * @version $Id$
 */
public class vGraphComponent extends JGraph implements GraphModelListener {

    vGraphModel model;
    EventGraphViewFrame parent;

    public vGraphComponent(vGraphModel model, EventGraphViewFrame frame) {
        super(model);
        parent = frame;
        
        vGraphComponent instance = this;
        ToolTipManager.sharedInstance().registerComponent(instance);
        //super.setDoubleBuffered(false); // test for mac
        this.model = model;
        this.setBendable(true);
        this.setSizeable(false);
        this.setGridVisible(true);
        //this.setGridMode(JGraph.CROSS_GRID_MODE);
        //this.setGridMode(JGraph.DOT_GRID_MODE);
        this.setGridMode(JGraph.LINE_GRID_MODE);
        this.setGridColor(new Color(0xcc, 0xcc, 0xff)); // default on Mac, makes Windows look better
        this.setGridEnabled(true); // means snap
        this.setGridSize(10);
        this.setMarqueeHandler(new MyMarqueeHandler());
        this.setAntiAliased(true);
        this.addGraphSelectionListener(new myGraphSelectionListener());
        model.addGraphModelListener(instance);

        setupCutCopyPaste();

        //this.setMarqueeColor(Color.red);
        this.setLockedHandleColor(Color.red);
        this.setHighlightColor(Color.red);
    //this.setHandleColor(Color.orange);

    }

    private void setupCutCopyPaste() {
        // Handle keystrokes
        AbstractAction cutAction = new myCutKeyHandler();
        Action copyAction = new myCopyKeyHandler();
        Action pasteAction = new myPasteKeyHandler();

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod),
                cutAction.getValue(Action.NAME));
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod),
                copyAction.getValue(Action.NAME));
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod),
                pasteAction.getValue(Action.NAME));
        this.getActionMap().put(cutAction.getValue(Action.NAME), cutAction);
        this.getActionMap().put(copyAction.getValue(Action.NAME), copyAction);
        this.getActionMap().put(pasteAction.getValue(Action.NAME), pasteAction);
    }

    class myCopyKeyHandler extends AbstractAction {

        myCopyKeyHandler() {
            super("copy");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((ViskitController) parent.getController()).copy();
        }
    }

    class myCutKeyHandler extends AbstractAction {

        myCutKeyHandler() {
            super("cut");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((ViskitController) parent.getController()).cut();
        }
    }

    class myPasteKeyHandler extends AbstractAction {

        myPasteKeyHandler() {
            super("paste");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((ViskitController) parent.getController()).paste();
        }
    }

    @Override
    public void updateUI() {
        // Install a new UI
        setUI(new vGraphUI(this));    // we use our own for node/edge inspector editing
        //setUI(new BasicGraphUI());  // test
        invalidate();
    }

    public ViskitElement getViskitElementAt(Point p) {
        Object cell = vGraphComponent.this.getFirstCellForLocation(p.x, p.y);
        if (cell != null && cell instanceof CircleCell) {
            return (ViskitElement) ((CircleCell) cell).getUserObject();
        }
        return null;
    }
    private ModelEvent currentModelEvent = null;

    public void viskitModelChanged(ModelEvent ev) {
        currentModelEvent = ev;

        switch (ev.getID()) {
            case ModelEvent.NEWMODEL:
                model.deleteAll();
                break;
            case ModelEvent.EVENTADDED:
                model.addEventNode((EventNode) ev.getSource());
                break;
            case ModelEvent.EDGEADDED:
                model.addEdge((SchedulingEdge) ev.getSource());
                break;
            case ModelEvent.CANCELLINGEDGEADDED:
                model.addCancelEdge((CancellingEdge) ev.getSource());
                break;
            case ModelEvent.EVENTCHANGED:
                model.changeEvent((EventNode) ev.getSource());
                break;
            case ModelEvent.EVENTDELETED:
                model.deleteEventNode((EventNode) ev.getSource());
                break;
            case ModelEvent.EDGECHANGED:
                model.changeEdge((SchedulingEdge) ev.getSource());
                break;
            case ModelEvent.EDGEDELETED:
                model.deleteEdge((SchedulingEdge) ev.getSource());
                break;
            case ModelEvent.CANCELLINGEDGECHANGED:
                model.changeCancellingEdge((CancellingEdge) ev.getSource());
                break;
            case ModelEvent.CANCELLINGEDGEDELETED:
                model.deleteCancellingEdge((CancellingEdge) ev.getSource());
                break;
            default:
                //System.out.println("duh");
        }
        currentModelEvent = null;
    }

    @Override
    public void graphChanged(GraphModelEvent e) {
        if (currentModelEvent != null && currentModelEvent.getID() == ModelEvent.NEWMODEL) {
            return;
        }  // this came in from outside, we don't have to inform anybody..prevent reentry
        //todo confirm any other events that should cause us to bail here
        GraphModelEvent.GraphModelChange c = e.getChange();
        Object[] ch = c.getChanged();
        if (ch != null) {
            for (Object cell : ch) {
                if (cell instanceof CircleCell) {
                    CircleCell cc = (CircleCell) cell;
                    
                    @SuppressWarnings("unchecked") // JGraph not genericized
                    Map<String, Rectangle> m = cc.getAttributes();
                    Rectangle r = m.get("bounds");
                    if (r != null) {
                        EventNode en = (EventNode) cc.getUserObject();
                        en.setPosition(new Point(r.x, r.y));
                        ((ViskitModel) parent.getModel()).changeEvent(en);
                        m.put("bounds", new Rectangle(en.getPosition().x, en.getPosition().y, r.width, r.height));
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
                StringBuilder sb = new StringBuilder("<HTML>");
                if (c instanceof vEdgeCell) {
                    vEdgeCell vc = (vEdgeCell) c;
                    Edge se = (Edge) vc.getUserObject();

                    if (se instanceof SchedulingEdge) {
                        sb.append("<center>Schedule</center>");
                        if (se.conditionalDescription != null) {
                            String cmt = se.conditionalDescription.trim();
                            if (cmt.length() > 0) {
                                sb.append("<br><u>description</u><br>");
                                sb.append(wrapAtPos(escapeLTGT(cmt), 60));
                                sb.append("<br>");
                            }
                        }

                        double priority;
                        String s;
                        
                        // Assume numeric comes in, avoid NumberFormatException via Regex check
                        if (Pattern.matches(SchedulingEdge.FLOATING_POINT_REGEX, ((SchedulingEdge) se).priority)) {
                            priority = Double.parseDouble(((SchedulingEdge) se).priority);
                            NumberFormat df = DecimalFormat.getNumberInstance();
                            df.setMaximumFractionDigits(3);
                            df.setMaximumIntegerDigits(3);
                            if (Double.compare(priority, Double.MAX_VALUE) >= 0) {
                                s = "MAX";
                            } else if (Double.compare(priority, -Double.MAX_VALUE) <= 0) {
                                s = "MIN";
                            } else {
                                s = df.format(priority);
                            }
                        } else {
                            s = ((SchedulingEdge) se).priority;
                        }

                        sb.append("<u>priority</u><br>&nbsp;");
                        sb.append(s);
                        sb.append("<br>");

                        if (se.delay != null) {
                            String dly = se.delay.trim();
                            if (dly.length() > 0) {
                                sb.append("<u>delay</u><br>&nbsp;");
                                sb.append(dly);
                                sb.append("<br>");
                            }
                        }
                    } else {
                        sb.append("<center>Cancel</center>");
                        if (se.conditionalDescription != null) {
                            String cmt = se.conditionalDescription.trim();
                            if (cmt.length() > 0) {
                                sb.append("<br><u>description</u><br>");
                            }
                            sb.append(wrapAtPos(escapeLTGT(cmt), 60));
                            sb.append("<br>");
                        }
                    }

                    if (se.conditional != null) {
                        String cond = se.conditional.trim();
                        if (cond.length() > 0) {
                            sb.append("<u>condition</u><br>&nbsp;if( ");
                            sb.append(escapeLTGT(cond));
                            sb.append(" )<br>");
                        }
                    }
                    StringBuilder epSt = new StringBuilder();
                    int idx = 1;
                    for (Iterator itr = se.parameters.iterator(); itr.hasNext();) {
                        vEdgeParameter ep = (vEdgeParameter) itr.next();
                        epSt.append("&nbsp;");
                        epSt.append(idx++);
                        epSt.append(" ");
                        epSt.append(ep.getValue());
                        epSt.append("<br>");
                    }
                    if (epSt.length() > 0) {
                        sb.append("<u>edge parameters</u><br>");
                        sb.append(epSt.toString());
                    }
                    if (sb.substring(sb.length() - 4).equalsIgnoreCase("<br>")) {
                        sb.setLength(sb.length() - 4);
                    }
                    sb.append("</HTML>");
                    return sb.toString();

                } else if (c instanceof CircleCell) {
                    CircleCell cc = (CircleCell) c;
                    EventNode en = (EventNode) cc.getUserObject();
                    sb.append("<center>");
                    sb.append(en.getName());
                    sb.append("</center>");

                    if (!en.getComments().isEmpty()) {
                        String stripBrackets = en.getComments().get(0).trim();
                        if (stripBrackets.length() > 0) {
                            sb.append("<u>description</u><br>");
                            sb.append(wrapAtPos(escapeLTGT(stripBrackets), 60));
                            sb.append("<br>");
                        }
                    }
                    
                    List<ViskitElement> argLis = en.getArguments();
                    StringBuffer args = new StringBuffer();
                    int n = 1;
                    for (ViskitElement ve : argLis) {
                        EventArgument arg = (EventArgument) ve;
                        String as = arg.getName() + " (" + arg.getType() + ")";
                        args.append("&nbsp;");
                        args.append(n);
                        args.append(" ");
                        args.append(as);
                        args.append("<br>");
                    }
                    if (args.length() > 0) {
                        sb.append("<u>arguments</u><br>");
                        sb.append(args);
                    }

                    Vector<ViskitElement> locVarLis = en.getLocalVariables();
                    StringBuffer lvs = new StringBuffer();
                    for (ViskitElement ve : locVarLis) {
                        EventLocalVariable lv = (EventLocalVariable) ve;
                        lvs.append("&nbsp;");
                        lvs.append(lv.getName());
                        lvs.append(" (");
                        lvs.append(lv.getType());
                        lvs.append(") = ");
                        String val = lv.getValue();
                        lvs.append(val.length() <= 0 ? "<i><default></i>" : val);
                        lvs.append("<br>");
                    }
                    if (lvs.length() > 0) {
                        sb.append("<u>local variables</u><br>");
                        sb.append(lvs);
                    }
                    List<ViskitElement> st = en.getTransitions();
                    StringBuffer sttrans = new StringBuffer();
                    for (ViskitElement ve : st) {
                        EventStateTransition est = (EventStateTransition) ve;
                        sttrans.append("&nbsp;");
                        sttrans.append(est.getStateVarName());
                        sttrans.append(!est.isOperation() ? "=" : ".");
                        sttrans.append(escapeLTGT(est.getOperationOrAssignment()));
                        sttrans.append("<br>");
                    }
                    
                    if (sttrans.length() > 0) {
                        sb.append("<u>state transitions</u><br>");
                        sb.append(sttrans);
                    }

                    if (sb.substring(sb.length() - 4).equalsIgnoreCase("<br>")) {
                        sb.setLength(sb.length() - 4);
                    }
                    sb.append("</HTML>");
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

        if (view instanceof CircleView) {
            CircleCell cc = (CircleCell) view.getCell();
            Object en = cc.getUserObject();
            if (en instanceof EventNode) // should always be, except for our prototype examples
            {
                return ((EventNode) en).getName();
            }
        } else if (view instanceof vEdgeView) {
            vEdgeCell cc = (vEdgeCell) view.getCell();
            Object e = cc.getUserObject();
            if (e instanceof SchedulingEdge) {
                SchedulingEdge se = (SchedulingEdge) e;
                if (se.conditional == null || se.conditional.isEmpty()) // put S only for conditional edges
                {
                    return null;
                }
                return null;  // bug 675 "S";
            } else if (e instanceof CancellingEdge) // should always be one of these 2 except for proto examples
            {
                return null;
            }
        }
        return null;
    }

    // To use circles, from the tutorial
    @Override
    protected VertexView createVertexView(Object v, CellMapper cm) {
        if (v instanceof CircleCell) {
            return new CircleView(v, this, cm);
        }
        // else
        return super.createVertexView(v, cm);
    }

    // To customize my edges
    @Override
    protected EdgeView createEdgeView(Object e, CellMapper cm) {
        if (e instanceof vSelfEdgeCell) // order important... 1st is sub of 2nd
        {
            return new vSelfEdgeView(e, this, cm);
        } else if (e instanceof vEdgeCell) {
            return new vEdgeView(e, this, cm);
        }
        // else
        return super.createEdgeView(e, cm);
    }

    @Override
    protected PortView createPortView(Object p, CellMapper cm) {
        if (p instanceof vPortCell) {
            return new vPortView(p, this, cm);
        }
        return super.createPortView(p, cm);
    }

    /**
     * This class informs the controller that the selected set has changed.  Since we're only using this
     * to (dis)able the cut and copy menu items, it could be argued that this functionality should be internal
     * to the View, and the controller needn't be involved.  Nevertheless, the round trip through the controller
     * remains in place.
     */
    class myGraphSelectionListener implements GraphSelectionListener {

        Vector<Object> selected = new Vector<Object>();

        @Override
        public void valueChanged(GraphSelectionEvent e) {
            Object[] oa = e.getCells();
            if (oa == null || oa.length <= 0) {
                return;
            }
            for (int i = 0; i < oa.length; i++) {
                if (e.isAddedCell(i)) {
                    selected.add(((DefaultGraphCell) oa[i]).getUserObject());
                } else {
                    selected.remove(((DefaultGraphCell) oa[i]).getUserObject());
                }
            }
            ((ViskitController) parent.getController()).selectNodeOrEdge(selected);
        }
    }

    // MarqueeHandler that Connects Vertices and Displays PopupMenus
    public class MyMarqueeHandler extends BasicMarqueeHandler {

        // Holds the Start and the Current Point
        protected Point start,  current;

        // Holds the First and the Current Port
        protected PortView port,  firstPort;

        // Override to Gain Control (for PopupMenu and ConnectMode)
        @Override
        public boolean isForceMarqueeEvent(MouseEvent e) {
            // If Right Mouse Button we want to Display the PopupMenu
            if (SwingUtilities.isRightMouseButton(e)) // Return Immediately
            {
                return true;
            }
            // Find and Remember Port
            port = getSourcePortAt(e.getPoint());
            // If Port Found and in ConnectMode (=Ports Visible)
            if (port != null && vGraphComponent.this.isPortsVisible() && e.getClickCount() != 2) //jmb  added to edit when in edge mode
            {
                return true;
            }
            // Else Call Superclass
            return super.isForceMarqueeEvent(e);
        }

        // Display PopupMenu or Remember Start Location and First Port
        @Override
        public void mousePressed(final MouseEvent e) {
            // If Right Mouse Button
            if (SwingUtilities.isRightMouseButton(e)) {
            // Scale From Screen to Model
            //Point loc = vGraphComponent.this.fromScreen(e.getPoint());
            // Find Cell in Model Coordinates
            //Object cell = vGraphComponent.this.getFirstCellForLocation(loc.x, loc.y);
            // Create PopupMenu for the Cell
            //JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
            // Display PopupMenu

            // jmb...not today
            //menu.show(vGraphComponent.this, e.getX(), e.getY());

            // Else if in ConnectMode and Remembered Port is Valid
            } else if (port != null && !e.isConsumed() && vGraphComponent.this.isPortsVisible()) {
                // Remember Start Location
                start = vGraphComponent.this.toScreen(port.getLocation(null));
                // Remember First Port
                firstPort = port;
                // Consume Event
                e.consume();
            } else // Call Superclass
            {
                super.mousePressed(e);
            }
        }

        // Find Port under Mouse and Repaint Connector
        @Override
        public void mouseDragged(MouseEvent e) {
            // If remembered Start Point is Valid
            if (start != null && !e.isConsumed()) {
                // Fetch Graphics from Graph
                Graphics g = vGraphComponent.this.getGraphics();
                // Xor-Paint the old Connector (Hide old Connector)
                paintConnector(Color.black, vGraphComponent.this.getBackground(), g);
                // Reset Remembered Port
                port = getTargetPortAt(e.getPoint());
                // If Port was found then Point to Port Location
                if (port != null) {
                    current = vGraphComponent.this.toScreen(port.getLocation(null));
                } // Else If no Port was found then Point to Mouse Location
                else {
                    current = vGraphComponent.this.snap(e.getPoint());
                }
                // Xor-Paint the new Connector
                paintConnector(vGraphComponent.this.getBackground(), Color.black, g);
                // Consume Event
                e.consume();
            }
            // Call Superclass
            super.mouseDragged(e);
        }

        public PortView getSourcePortAt(Point point) {
            // Scale from Screen to Model
            Point tmp = vGraphComponent.this.fromScreen(new Point(point));
            // Find a Port View in Model Coordinates and Remember
            return vGraphComponent.this.getPortViewAt(tmp.x, tmp.y);
        }

        // Find a Cell at point and Return its first Port as a PortView
        protected PortView getTargetPortAt(Point point) {
            // Find Cell at point (No scaling needed here)
            Object cell = vGraphComponent.this.getFirstCellForLocation(point.x, point.y);
            // Loop Children to find PortView
            for (int i = 0; i < vGraphComponent.this.getModel().getChildCount(cell); i++) {
                // Get Child from Model
                Object tmp = vGraphComponent.this.getModel().getChild(cell, i);
                // Get View for Child using the Graph's View as a Cell Mapper
                //jmb fix  tmp = graphPane.getView().getMapping(tmp, false);
                // If Child View is a Port View and not equal to First Port
                if (tmp instanceof PortView && tmp != firstPort) // Return as PortView
                {
                    return (PortView) tmp;
                }
            }
            // No Port View found
            return getSourcePortAt(point);
        }

        // Connect the First Port and the Current Port in the Graph or Repaint
        @Override
        public void mouseReleased(MouseEvent e) {
            // If Valid Event, Current and First Port
            if (e != null && !e.isConsumed() && port != null && firstPort != null &&
                    firstPort != port) {
                // Then Establish Connection
                connect((Port) firstPort.getCell(), (Port) port.getCell());
                // Consume Event
                e.consume();
            // Else Repaint the Graph
            } else {
                vGraphComponent.this.repaint();
            }
            // Reset Global Vars
            firstPort = port = null;
            start = current = null;
            // Call Superclass
            super.mouseReleased(e);
        }

        // Show Special Cursor if Over Port
        @Override
        public void mouseMoved(MouseEvent e) {
            // Check Mode and Find Port
            if (e != null && getSourcePortAt(e.getPoint()) != null &&
                    !e.isConsumed() && vGraphComponent.this.isPortsVisible()) {
                // Set Cusor on Graph (Automatically Reset)
                vGraphComponent.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
                // Consume Event
                e.consume();
            }
            // Call Superclass
            super.mouseMoved(e); //this was super.mouseReleased() but apparently was not causing probs.
        }

        // Use Xor-Mode on Graphics to Paint Connector
        protected void paintConnector(Color fg, Color bg, Graphics g) {
            // Set Foreground
            g.setColor(fg);
            // Set Xor-Mode Color
            g.setXORMode(bg);
            // Highlight the Current Port
            paintPort(vGraphComponent.this.getGraphics());
            // If Valid First Port, Start and Current Point
            if (firstPort != null && start != null && current != null) // Then Draw A Line From Start to Current Point
            {
                g.drawLine(start.x, start.y, current.x, current.y);
            }
        }

        // Use the Preview Flag to Draw a Highlighted Port
        protected void paintPort(Graphics g) {
            // If Current Port is Valid
            if (port != null) {
                // If Not Floating Port...
                boolean o = (GraphConstants.getOffset(port.getAttributes()) != null);
                // ...Then use Parent's Bounds
                Rectangle r = (o) ? port.getBounds() : port.getParentView().getBounds();
                // Scale from Model to Screen
                r = vGraphComponent.this.toScreen(new Rectangle(r));
                // Add Space For the Highlight Border
                //r.setBounds(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
                r.setBounds(r.x - 5, r.y - 5, r.width + 10, r.height + 10);
                // Paint Port in Preview (=Highlight) Mode
                vGraphComponent.this.getUI().paintCell(g, port, r, true);
            }
        }

        // Insert a new Vertex at point
        private void insert(Point point) {
            
            // Construct Vertex with no Label
            DefaultGraphCell vertex = new DefaultGraphCell();
            
            // Add one Floating Port
            vertex.add(new DefaultPort());
            // Snap the Point to the Grid
            point = vGraphComponent.this.snap(new Point(point));
            // Default Size for the new Vertex
            Dimension size = new Dimension(25, 25);
            // Create a Map that holds the attributes for the Vertex
            Map map = GraphConstants.createMap();
            // Add a Bounds Attribute to the Map
            GraphConstants.setBounds(map, new Rectangle(point, size));
            // Add a Border Color Attribute to the Map
            GraphConstants.setBorderColor(map, Color.black);
            // Add a White Background
            GraphConstants.setBackground(map, Color.white);
            // Make Vertex Opaque
            GraphConstants.setOpaque(map, true);
        // Construct a Map from cells to Maps (for insert)
        // Hashtable attributes = new Hashtable();
        // Associate the Vertex with its Attributes
        // attributes.put(vertex, map);

        // Insert the Vertex and its Attributes
        //   graphPane.getModel().insert(new Object[]{vertex}, null, null, attributes);
        }

        // Insert a new Edge between source and target
        public void connect(Port source, Port target) {
            DefaultGraphCell src = (DefaultGraphCell) vGraphComponent.this.getModel().getParent(source);
            DefaultGraphCell tar = (DefaultGraphCell) vGraphComponent.this.getModel().getParent(target);
            Object[] oa = new Object[]{src, tar};
            ViskitController controller = (ViskitController) parent.getController();
            if (parent.getCurrentMode() == EventGraphViewFrame.CANCEL_ARC_MODE) {
                controller.buildNewCancelArc(oa);
            } else {
                controller.buildNewArc(oa);
            }
        }

        public JPopupMenu createPopupMenu(final Point pt, final Object cell) {
            JPopupMenu menu = new JPopupMenu();
            if (cell != null) {
                // Edit
                menu.add(new AbstractAction("Edit") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        vGraphComponent.this.startEditingAtCell(cell);
                    }
                });
            }
            // Remove
            if (!vGraphComponent.this.isSelectionEmpty()) {
                menu.addSeparator();
                menu.add(new AbstractAction("Remove") {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                    // jmb fix remove.actionPerformed(e);
                    // remove is an Action
                    }
                });
            }
            menu.addSeparator();
            // Insert
            menu.add(new AbstractAction("Insert") {

                @Override
                public void actionPerformed(ActionEvent ev) {
                    insert(pt);
                }
            });
            return menu;
        }
    } // End of Editor.MyMarqueeHandler
}

/***********************************************/
/**
 * To mark our edges.
 */
class vEdgeCell extends DefaultEdge {

    public vEdgeCell() {
        this(null);
    }

    public vEdgeCell(Object userObject) {
        super(userObject);
    }
}

class vSelfEdgeCell extends vEdgeCell {

    public vSelfEdgeCell() {
        this(null);
    }

    public vSelfEdgeCell(Object userObject) {
        super(userObject);
    }
}

class vPortCell extends DefaultPort {

    public vPortCell() {
        this(null);
    }

    public vPortCell(Object o) {
        this(o, null);
    }

    public vPortCell(Object o, Port port) {
        super(o, port);
    }
}

class vPortView extends PortView {

    static int mysize = 54;   // same as the circle

    public vPortView(Object o, JGraph jGraph, CellMapper cellMapper) {
        super(o, jGraph, cellMapper);
    }

    @Override
    public Rectangle getBounds() {
        Rectangle bounds = new Rectangle(getLocation(null));
        bounds.x = bounds.x - mysize / 2;
        bounds.y = bounds.y - mysize / 2;
        bounds.width = bounds.width + mysize;
        bounds.height = bounds.height + mysize;
        return bounds;
    }
}

/**
 * Sub class EdgeView to install our own localRenderer.
 */
class vEdgeView extends EdgeView {

    public static vEdgeRenderer localRenderer = new vEdgeRenderer();

    public vEdgeView(Object cell, JGraph gr, CellMapper cm) {
        super(cell, gr, cm);
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

    public static vSelfEdgeRenderer localRenderer2 = new vSelfEdgeRenderer();

    public vSelfEdgeView(Object cell, JGraph gr, CellMapper cm) {
        super(cell, gr, cm);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer2;
    }
}

/**
 * To mark our nodes.
 */
class CircleCell extends DefaultGraphCell {

    CircleCell() {
        this(null);
    }

    public CircleCell(Object userObject) {
        super(userObject);
    }
}

/**
 * Sub class VertexView to install our own localRenderer.
 */
class CircleView extends VertexView {

    static vVertexRenderer localRenderer = new vVertexRenderer();

    public CircleView(Object cell, JGraph gr, CellMapper cm) {
        super(cell, gr, cm);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer;
    }
}

/**
 * A replacement class to tweak the routing slightly so that the edges come into
 * the node from other directions than NSE and W.  Also, support offsetting 
 * edges between the same two nodes.
 */
class ViskitRouting implements org.jgraph.graph.Edge.Routing {

    @Override
    @SuppressWarnings("unchecked") // JGraph not genericized
    public void route(EdgeView edge, List points) {
        int n = points.size();
        Object fromKey = null, toKey = null;

        Point from = edge.getPoint(0);

        if (edge.getSource() instanceof PortView) {
            from = ((PortView) edge.getSource()).getLocation(null);
            fromKey = getKey((PortView) edge.getSource());
        } else if (edge.getSource() != null) {
            from = edge.getSource().getBounds().getLocation();
        }

        Point to = edge.getPoint(n - 1);

        if (edge.getTarget() instanceof PortView) {
            to = ((PortView) edge.getTarget()).getLocation(null);
            toKey = getKey((PortView) edge.getTarget());
        } else if (edge.getTarget() != null) {
            to = edge.getTarget().getBounds().getLocation();
        }

        int adjustFactor = 0;
        if (toKey != null && fromKey != null) {
            adjustFactor = getFactor(toKey, fromKey, edge);
        }

        int sig = adjustFactor % 2;
        adjustFactor++;
        adjustFactor /= 2;
        if (sig == 0) {
            adjustFactor *= -1;
        }

        int adjustment = 0 + 35 * adjustFactor;       // little bias

        int dx = Math.abs(from.x - to.x);
        int dy = Math.abs(from.y - to.y);
        int x2 = from.x + ((to.x - from.x) / 2);
        int y2 = from.y + ((to.y - from.y) / 2);
        Point[] routed = new Point[2];
        if (dx > dy) {
            routed[0] = new Point(x2, from.y + adjustment);
            routed[1] = new Point(x2, to.y - adjustment);
        } else {
            routed[0] = new Point(from.x - adjustment, y2);
            routed[1] = new Point(to.x + adjustment, y2);
        }

        // Set/Add Points
        for (int i = 0; i < routed.length; i++) {
            if (points.size() > i + 2) {
                points.set(i + 1, routed[i]);
            } else {
                points.add(i + 1, routed[i]);
            }
        }
        
        // Remove spare points
        while (points.size() > routed.length + 2) {
            points.remove(points.size() - 2);
        }

    }

    private Object getKey(PortView pv) {
        CircleView cv = (CircleView) pv.getParentView();
        CircleCell cc = (CircleCell) cv.getCell();
        EventNode en = (EventNode) cc.getUserObject();
        return en.getModelKey();
    }
    static Map<Object, Vector<Object>> nodePairs = new HashMap<Object, Vector<Object>>();

    private int getFactor(Object toKey, Object fromKey, EdgeView ev) {
        String toStr = toKey.toString();
        String fromStr = fromKey.toString();
        String masterKey;
        if (toStr.compareTo(fromStr) > 0) {
            masterKey = fromStr + "-" + toStr;
        } else {
            masterKey = toStr + "-" + fromStr;
        }

        vEdgeCell vec = (vEdgeCell) ev.getCell();
        viskit.model.Edge edg = (viskit.model.Edge) vec.getUserObject();
        Object edgeKey = edg.getModelKey();

        Vector<Object> lis = nodePairs.get(masterKey);
        if (lis == null) {
            // never had an edge between these 2 before
            Vector<Object> v = new Vector<Object>();
            v.add(edgeKey);
            //System.out.println("adding edgekey in "+masterKey + " "+ edgeKey);
            nodePairs.put(masterKey, v);
            return 0;
        }
        // Here if there has been a previous edge between the 2, maybe just this one
        if (!lis.contains(edgeKey)) {
            lis.add(edgeKey);
        //System.out.println("adding edgekey in "+masterKey + " "+ edgeKey);
        }
        return lis.indexOf(edgeKey);
    }
}

/**
 * Class to draw the self-referential edges as an arc attached to the node.
 */
class vSelfEdgeRenderer extends vEdgeRenderer {

    private int circleDiam = 30;
    private Arc2D arc;

    /**
     * Returns the shape that represents the current edge
     * in the context of the current graph.
     * This method sets the global beginShape, lineShape
     * and endShape variables as a side-effect.
     * 
     * @return the shape that represents the current edge
     * in the context of the current graph
     */
    @Override
    protected Shape createShape() {
        CircleView myCircle = (CircleView) view.getSource().getParentView();
        Rectangle circBnds = myCircle.getBounds();
        int circCenterX = circBnds.x + circBnds.width / 2;
        int circCenterY = circBnds.y + circBnds.height / 2;

        int topCenterX = circBnds.x + circBnds.width / 2 - circleDiam / 2;
        int topCenterY = circBnds.y + circBnds.height - 7;  // 7 pixels up

        AffineTransform rotater = new AffineTransform();
        rotater.setToRotation(getAngle(), (double) circCenterX, (double) circCenterY);

        if (view.sharedPath == null) {
            double ex = 0.0d + topCenterX;
            double ey = 0.0d + topCenterY;
            double ew = 0.0d + circleDiam;
            double eh = 0.0d + circleDiam;
            arc = new Arc2D.Double(ex, ey, ew, eh, 135.0d, 270.0d, Arc2D.OPEN); // angles: start , extent
            view.sharedPath = new GeneralPath(arc);
            view.sharedPath = new GeneralPath(view.sharedPath.createTransformedShape(rotater));
        } else {
            view.sharedPath.reset();
        }

        view.beginShape = view.lineShape = view.endShape = null;

        Point2D p2start = arc.getStartPoint();
        Point2D p2end = arc.getEndPoint();
        Point pstrt = new Point((int) p2start.getX(), (int) p2start.getY());
        Point pend = new Point((int) p2end.getX(), (int) p2end.getY());

        if (beginDeco != GraphConstants.ARROW_NONE) {
            view.beginShape = createLineEnd(beginSize, beginDeco, pstrt, new Point(pstrt.x + 15, pstrt.y + 15));
            view.beginShape = rotater.createTransformedShape(view.beginShape);
        }
        if (endDeco != GraphConstants.ARROW_NONE) {
            view.endShape = createLineEnd(endSize, endDeco, new Point(pend.x + 15, pend.y + 25), pend);
            view.endShape = rotater.createTransformedShape(view.endShape);
        }

        view.lineShape = (GeneralPath) view.sharedPath.clone();

        if (view.endShape != null) {
            view.sharedPath.append(view.endShape, true);
        }
        if (view.beginShape != null) {
            view.sharedPath.append(view.beginShape, true);
        }

        return view.sharedPath;
    }
    
    /**
     * Defines how much we increment the angle calculated in getAngle() for each self-referential edge discovered.
     * Since we want to advance 3/8 of a circle for each edge, the value below should be 2Pi * 3/8.
     * But since the iterator in getAngle discovers each edge twice (since the node has a connection to
     * both its head and tail, the math works out to rotate only half that much.
     */
    private static double rotIncr = Math.PI * 3.d / 8.d;

    /**
     * This class will determine if there are other self-referential edges attached to this
     * node, and try to return a different angle for different edges, so they will be rendered
     * at different "clock" points around the node circle.
     * @return a different angle for different edges
     */
    private double getAngle() {
        vEdgeCell vec = (vEdgeCell) view.getCell();
        Edge edg = (Edge) vec.getUserObject();

        CircleCell vcc = (CircleCell) view.getSource().getParentView().getCell();
        EventNode en = (EventNode) vcc.getUserObject();
        double retd = -rotIncr;
        for (ViskitElement ve : en.getConnections()) {
            Edge e = (Edge) ve;
            if (e.to == en && e.from == en) {
                retd += rotIncr;
                if (e == edg) {
                    return retd;
                }
            }
        }
        return 0.0d;      // should always find one
    }
}
