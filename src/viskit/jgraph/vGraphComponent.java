package viskit.jgraph;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionEvent;
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
public class vGraphComponent extends JGraph implements GraphModelListener {

    vGraphModel vGModel;
    EventGraphViewFrame parent;
    protected Action removeAction;

    /** Sets up JGraph to render nodes and edges for DES
     *
     * @param model a model of the node with its specific edges
     * @param frame the main view frame canvas to render to
     */
    public vGraphComponent(vGraphModel model, EventGraphViewFrame frame) {
        super(model);
        parent = frame;

        vGraphComponent instance = this;
        ToolTipManager.sharedInstance().registerComponent(instance);
        this.vGModel = model;
        this.setSizeable(false);
        this.setGridVisible(true);
        this.setGridMode(JGraph.LINE_GRID_MODE);
        this.setGridColor(new Color(0xcc, 0xcc, 0xff)); // default on Mac, makes Windows look better
        this.setGridEnabled(true); // means snap
        this.setGridSize(10);
        this.setMarqueeHandler(new MyMarqueeHandler());
        this.setAntiAliased(true);

        // Set the Tolerance to 2 Pixel
        setTolerance(2);

        // Jump to default port on connect
        setJumpToDefaultPort(true);

        this.addGraphSelectionListener(new myGraphSelectionListener());
        model.addGraphModelListener(instance);

        setupCutCopyPaste();

        //this.setMarqueeColor(Color.red);
        this.setLockedHandleColor(Color.red);
        this.setHighlightColor(Color.red);
        //this.setHandleColor(Color.orange);

        // As of JGraph-5.2, custom cell rendering is
        // accomplished via this convention
        getGraphLayoutCache().setFactory(new DefaultCellViewFactory() {

            // To use circles, from the tutorial
            @Override
            protected VertexView createVertexView(Object v) {
                VertexView view;
                if (v instanceof CircleCell) {
                    view = new CircleView(v);
                } else {
                    view = super.createVertexView(v);
                }
                return view;
            }

            // To customize my edges
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

    private void setupCutCopyPaste() {

        // Handle keystrokes
        Action cutAction = new myCutKeyHandler();
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

        removeAction = new myRemoveKeyHandler();
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, accelMod),
                removeAction.getValue(Action.NAME));
        this.getActionMap().put(removeAction.getValue(Action.NAME), removeAction);
    }

    class myCopyKeyHandler extends AbstractAction {

        myCopyKeyHandler() {
            super("copy");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((EventGraphController) parent.getController()).copy();
        }
    }

    class myCutKeyHandler extends AbstractAction {

        myCutKeyHandler() {
            super("cut");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((EventGraphController) parent.getController()).cut();
        }
    }

    class myPasteKeyHandler extends AbstractAction {

        myPasteKeyHandler() {
            super("paste");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((EventGraphController) parent.getController()).paste();
        }
    }

    class myRemoveKeyHandler extends AbstractAction {

        myRemoveKeyHandler() {
            super("remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!vGraphComponent.this.isSelectionEmpty()) {
                Object[] cells = vGraphComponent.this.getSelectionCells();
                cells = vGraphComponent.this.getDescendants(cells);
                vGraphComponent.this.getModel().remove(cells);
            }
        }
    }

    @Override
    public void updateUI() {
        // Install a new UI
        setUI(new vGraphUI());    // we use our own for node/edge inspector editing
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
                vGModel.deleteAll();
                break;
            case ModelEvent.EVENTADDED:

                // Reclaimed from the vGModel to here
                insert((EventNode) ev.getSource());
                break;
            case ModelEvent.EVENTCHANGED:
                vGModel.changeEvent((EventNode) ev.getSource());
                break;
            case ModelEvent.EVENTDELETED:
                vGModel.deleteEventNode((EventNode) ev.getSource());
                break;
            case ModelEvent.EDGEADDED:
                vGModel.addEdge((Edge) ev.getSource());
                break;
            case ModelEvent.EDGECHANGED:
                vGModel.changeEdge((Edge) ev.getSource());
                break;
            case ModelEvent.EDGEDELETED:
                vGModel.deleteEdge((Edge) ev.getSource());
                break;
            case ModelEvent.CANCELINGEDGEADDED:
                vGModel.addCancelEdge((Edge) ev.getSource());
                break;
            case ModelEvent.CANCELINGEDGECHANGED:
                vGModel.changeCancelingEdge((Edge) ev.getSource());
                break;
            case ModelEvent.CANCELINGEDGEDELETED:
                vGModel.deleteCancelingEdge((Edge) ev.getSource());
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

                    AttributeMap m = cc.getAttributes();
                    Rectangle2D.Double r = (Rectangle2D.Double) m.get("bounds");
                    if (r != null) {
                        EventNode en = (EventNode) cc.getUserObject();
                        en.setPosition(new Point2D.Double(r.x, r.y));
                        ((Model) parent.getModel()).changeEvent(en);

                        // might have changed:
                        m.put("bounds", m.createRect(en.getPosition().getX(), en.getPosition().getY(), r.width, r.height));
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
                if (c instanceof vEdgeCell) {
                    vEdgeCell vc = (vEdgeCell) c;
                    Edge se = (Edge) vc.getUserObject();

                    if (se instanceof SchedulingEdge) {
                        sb.append("<center>Scheduling Edge</center>");
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
                        sb.append("<center>Canceling Edge</center>");
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

                        // TODO: Will need a schema change for this to show correctly
                        if (ep.getType() != null) {
                            epSt.append(" ");
                            epSt.append("(");
                            epSt.append(ep.getType());
                            epSt.append(")");
                        }
                        epSt.append("<br>");
                    }
                    if (epSt.length() > 0) {
                        sb.append("<u>edge parameters</u><br>");
                        sb.append(epSt.toString());
                    }
                    if (sb.substring(sb.length() - 4).equalsIgnoreCase("<br>")) {
                        sb.setLength(sb.length() - 4);
                    }
                    sb.append("</html>");
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
                    int n = 0;
                    for (ViskitElement ve : argLis) {
                        EventArgument arg = (EventArgument) ve;
                        String as = arg.getName() + " (" + arg.getType() + ")";
                        args.append("&nbsp;");
                        args.append(++n);
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
                        lvs.append(val.isEmpty() ? "<i><default></i>" : val);
                        lvs.append("<br>");
                    }
                    if (lvs.length() > 0) {
                        sb.append("<u>local variables</u><br>");
                        sb.append(lvs);
                    }

                    String codeBlock = en.getCodeBlock();
                    if (!codeBlock.isEmpty()) {
                        sb.append("<u>code block</u><br>");
                        sb.append("&nbsp;");
                        sb.append(" ");
                        sb.append(codeBlock);
                        sb.append("<br>");
                    }

                    List<ViskitElement> st = en.getTransitions();
                    StringBuffer sttrans = new StringBuffer();
                    for (ViskitElement ve : st) {
                        EventStateTransition est = (EventStateTransition) ve;
                        String[] sa = est.toString().split("\\n");
                        for (String s : sa) {
                            sttrans.append("&nbsp;");
                            sttrans.append(s);
                            sttrans.append("<br>");
                        }
                    }

                    if (sttrans.length() > 0) {
                        sb.append("<u>state transitions</u><br>");
                        sb.append(sttrans);
                    }

                    if (sb.substring(sb.length() - 4).equalsIgnoreCase("<br>")) {
                        sb.setLength(sb.length() - 4);
                    }
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
            } else if (e instanceof CancelingEdge) // should always be one of these 2 except for proto examples
            {
                return null;
            }
        }
        return null;
    }

    /**
     * This class informs the controller that the selected set has changed.  Since we're only using this
     * to (dis)able the cut and copy menu items, it could be argued that this functionality should be internal
     * to the View, and the controller needn't be involved.  Nevertheless, the round trip through the controller
     * remains in place.
     */
    class myGraphSelectionListener implements GraphSelectionListener {

        Vector<Object> selected = new Vector<>();

        @Override
        public void valueChanged(GraphSelectionEvent e) {
            boolean enabled = !vGraphComponent.this.isSelectionEmpty();
	    removeAction.setEnabled(enabled);

            Object[] oa = e.getCells();
            if (oa == null || oa.length <= 0) {
                return;
            }
            for (Object o : oa) {
                if (e.isAddedCell(o)) {
                    selected.add(((DefaultGraphCell) o).getUserObject());
                } else {
                    selected.remove(((DefaultGraphCell) o).getUserObject());
                }
            }
            ((EventGraphController) parent.getController()).selectNodeOrEdge(selected);
        }
    }

    /** MarqueeHandler that Connects Vertices and Displays PopupMenus */
    public class MyMarqueeHandler extends BasicMarqueeHandler {

        // Holds the Start and the Current Point
        protected Point2D start, current;

        // Holds the First and the Current Port
        protected PortView port, firstPort;

        /**
         * Component that is used for highlighting cells if the graph does not
         * allow XOR painting.
         */
        protected JComponent highlight;

        public MyMarqueeHandler() {
            // Configures the panel for highlighting ports
            highlight = createHighlight();
        }

        /**
         * Creates the component that is used for highlighting cells if the
         * graph does not allow XOR painting.
         * @return a component that is used for highlighting cells
         */
        private JComponent createHighlight() {
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            panel.setVisible(false);
            panel.setOpaque(false);

            return panel;
        }

        // Override to Gain Control (for PopupMenu and ConnectMode)
        @Override
        public boolean isForceMarqueeEvent(MouseEvent e) {
            if (e.isShiftDown()) {
                return false;
            }
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
                // Find Cell in Model Coordinates
//                Object cell = vGraphComponent.this.getFirstCellForLocation(e.getX(), e.getY());
                // Create PopupMenu for the Cell
//                JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
                // Display PopupMenu
//                menu.show(vGraphComponent.this, e.getX(), e.getY());
                // Else if in ConnectMode and Remembered Port is Valid
            } else if (port != null && vGraphComponent.this.isPortsVisible()) {
                // Remember Start Location
                start = vGraphComponent.this.toScreen(port.getLocation());
                // Remember First Port
                firstPort = port;
            } else // Call Superclass
            {
                super.mousePressed(e);
            }
        }

        // Find Port under Mouse and Repaint Connector
        @Override
        public void mouseDragged(MouseEvent e) {
            // If remembered Start Point is Valid
            if (start != null) {
                // Fetch Graphics from Graph
                Graphics g = vGraphComponent.this.getGraphics();
                // Reset Remembered Port
                PortView newPort = getTargetPortAt(e.getPoint());
                // Do not flicker (repaint only on real changes)
                if (newPort == null || newPort != port) {
                    // Xor-Paint the old Connector (Hide old Connector)
                    paintConnector(Color.black, vGraphComponent.this.getBackground(), g);
                    // If Port was found then Point to Port Location
                    port = newPort;
                    if (port != null) {
                        current = vGraphComponent.this.toScreen(port.getLocation());
                    } // Else If no Port was found then Point to Mouse Location
                    else {
                        current = vGraphComponent.this.snap(e.getPoint());
                    }
                    // Xor-Paint the new Connector
                    paintConnector(vGraphComponent.this.getBackground(), Color.black, g);
                }
            }
            // Call Superclass
            super.mouseDragged(e);
        }

        public PortView getSourcePortAt(Point2D point) {
            // Disable jumping
            vGraphComponent.this.setJumpToDefaultPort(false);
            PortView result;
            try {
                // Find a Port View in Model Coordinates and Remember
                result = vGraphComponent.this.getPortViewAt(point.getX(), point.getY());
            } finally {
                vGraphComponent.this.setJumpToDefaultPort(true);
            }
            return result;
        }

        // Find a Cell at point and Return its first Port as a PortView
        protected PortView getTargetPortAt(Point2D point) {
            // Find a Port View in Model Coordinates and Remember
            return vGraphComponent.this.getPortViewAt(point.getX(), point.getY());
        }

        // Connect the First Port and the Current Port in the Graph or Repaint
        @Override
        public void mouseReleased(MouseEvent e) {
            highlight(vGraphComponent.this, null);

            // If Valid Event, Current and First Port
            if (e != null && port != null && firstPort != null
                    && firstPort != port) {
                // Then Establish Connection
                connect((Port) firstPort.getCell(), (Port) port.getCell());
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
            if (e != null && getSourcePortAt(e.getPoint()) != null
                    && vGraphComponent.this.isPortsVisible()) {
                // Set Cusor on Graph (Automatically Reset)
                vGraphComponent.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
				// Consume Event
                // Note: This is to signal the BasicGraphUI's
                // MouseHandle to stop further event processing.
                e.consume();
            } else // Call Superclass
            {
                super.mouseMoved(e);
            }
        }

        // Use Xor-Mode on Graphics to Paint Connector
        protected void paintConnector(Color fg, Color bg, Graphics g) {
            if (vGraphComponent.this.isXorEnabled()) {
                // Set Foreground
                g.setColor(fg);
                // Set Xor-Mode Color
                g.setXORMode(bg);
                // Highlight the Current Port
                paintPort(vGraphComponent.this.getGraphics());

                drawConnectorLine(g);
            } else {
                Rectangle dirty = new Rectangle((int) start.getX(), (int) start.getY(), 1, 1);

                if (current != null) {
                    dirty.add(current);
                }

                dirty.grow(1, 1);

                vGraphComponent.this.repaint(dirty);
                highlight(vGraphComponent.this, port);
            }
        }

	// Overrides parent method to paint connector if
        // XOR painting is disabled in the graph
        @Override
        public void paint(JGraph graph, Graphics g) {
            super.paint(graph, g);

            if (!graph.isXorEnabled()) {
                g.setColor(Color.black);
                drawConnectorLine(g);
            }
        }

        protected void drawConnectorLine(Graphics g) {
            if (firstPort != null && start != null && current != null) {
                // Then Draw A Line From Start to Current Point
                g.drawLine((int) start.getX(), (int) start.getY(),
                        (int) current.getX(), (int) current.getY());
            }
        }

        // Use the Preview Flag to Draw a Highlighted Port
        protected void paintPort(Graphics g) {
            // If Current Port is Valid
            if (port != null) {
                // If Not Floating Port...
                boolean o = (GraphConstants.getOffset(port.getAllAttributes()) != null);
                // ...Then use Parent's Bounds
                Rectangle2D r = (o) ? port.getBounds() : port.getParentView()
                        .getBounds();
                // Scale from Model to Screen
                r = vGraphComponent.this.toScreen((Rectangle2D) r.clone());
                // Add Space For the Highlight Border
                r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
                        .getHeight() + 6);
                // Paint Port in Preview (=Highlight) Mode
                vGraphComponent.this.getUI().paintCell(g, port, r, true);
            }
        }

        /**
         * Highlights the given cell view or removes the highlight if no cell
         * view is specified.
         *
         * @param graph the JGraph panel
         * @param cellView the view to highlight
         */
        protected void highlight(final JGraph graph, final CellView cellView) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    if (cellView != null) {
                        highlight.setBounds(getHighlightBounds(graph, cellView));

                        if (highlight.getParent() == null) {
                            graph.add(highlight);
                            highlight.setVisible(true);
                        }
                    } else {
                        if (highlight.getParent() != null) {
                            highlight.setVisible(false);
                            highlight.getParent().remove(highlight);
                        }
                    }
                }
            };
            SwingUtilities.invokeLater(r);
        }

        /**
         * Returns the bounds to be used to highlight the given cell view.
         *
         * @param graph the JGraph panel
         * @param cellView the view to highlight
         * @return the bounds of the view to highlight
         */
        protected Rectangle getHighlightBounds(JGraph graph, CellView cellView) {
            boolean offset = (GraphConstants.getOffset(cellView.getAllAttributes()) != null);
            Rectangle2D r = (offset) ? cellView.getBounds() : cellView
                    .getParentView().getBounds();
            r = graph.toScreen((Rectangle2D) r.clone());
            int s = 3;

            return new Rectangle((int) (r.getX() - s), (int) (r.getY() - s),
                    (int) (r.getWidth() + 2 * s), (int) (r.getHeight() + 2 * s));
        }

    } // End of Editor.MyMarqueeHandler

    // NOTE: Not used
    // PopupMenu
    //
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
                    removeAction.actionPerformed(e);
                }
            });
        }
        menu.addSeparator();
        // Insert
        menu.add(new AbstractAction("Insert") {

            @Override
            public void actionPerformed(ActionEvent ev) {
//                insert(pt);
            }
        });
        return menu;
    }

    /** Inserts a new Edge between source and target nodes
     *
     * @param source the source node
     * @param target the target node
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

    /** Create the cells attributes before rendering on the graph
     *
     * @param point the 2D point at which to render the cell
     * @return the cells attributes before rendering on the graph
     */
    public Map createCellAttributes(Point2D point) {
        Map map = new Hashtable();

        // Snap the Point to the Grid
        if (this != null) {
            point = snap((Point2D) point.clone());
        } else {
            point = (Point2D) point.clone();
        }

        // Add a Bounds Attribute to the Map
        GraphConstants.setBounds(map, new Rectangle2D.Double(point.getX(),
                point.getY(), 54, 54));

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

        DefaultGraphCell cell = new CircleCell(node.getName());
        node.opaqueViewObject = cell;
        cell.setUserObject(node);

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
        vertex.getAttributes().applyMap(createCellAttributes(node.getPosition()));

        // Insert the Vertex (including child port and attributes)
        getGraphLayoutCache().insert(vertex);
    }
}

/**
 * ********************************************
 */
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

    static vEdgeRenderer localRenderer = new vEdgeRenderer();

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

    static vSelfEdgeRenderer localRenderer2 = new vSelfEdgeRenderer();

    public vSelfEdgeView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer2;
    }
}

/***********************************************/
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

    public CircleView(Object cell) {
        super(cell);
    }

    @Override
    public CellViewRenderer getRenderer() {
        return localRenderer;
    }
}

/**
 * Class to draw the self-referential edges as an arc attached to the node.
 */
class vSelfEdgeRenderer extends vEdgeRenderer {

    private double circleDiam = 30.0d;
    private Arc2D arc;

    @Override
    protected Shape createShape() {
        CircleView myCircle = (CircleView) view.getSource().getParentView();
        Rectangle2D circBnds = myCircle.getBounds();
        double circCenterX = circBnds.getCenterX();
        double circCenterY = circBnds.getCenterY();

        double topCenterX = circCenterX - circleDiam / 2;
        double topCenterY = circBnds.getY() + circBnds.getHeight() - 7;  // 7 pixels up

        AffineTransform rotater = new AffineTransform();
        rotater.setToRotation(getAngle(), circCenterX, circCenterY);

        if (view.sharedPath == null) {
            double ex = topCenterX;
            double ey = topCenterY;
            double ew = circleDiam;
            double eh = circleDiam;
            arc = new Arc2D.Double(ex, ey, ew, eh, 135.0d, 270.0d, Arc2D.OPEN); // angles: start, extent
            view.sharedPath = new GeneralPath(arc);
            view.sharedPath = new GeneralPath(view.sharedPath.createTransformedShape(rotater));
        } else {
            view.sharedPath.reset();
        }

        view.beginShape = view.lineShape = view.endShape = null;

        Point2D p2start = arc.getStartPoint();
        Point2D p2end = arc.getEndPoint();
        Point2D pstrt = new Point2D.Double(p2start.getX(), p2start.getY());
        Point2D pend = new Point2D.Double(p2end.getX(), p2end.getY());

        if (beginDeco != GraphConstants.ARROW_NONE) {
            view.beginShape = createLineEnd(beginSize, beginDeco, pstrt, new Point2D.Double(pstrt.getX() + 15, pstrt.getY() + 15));
            view.beginShape = rotater.createTransformedShape(view.beginShape);
        }
        if (endDeco != GraphConstants.ARROW_NONE) {
            view.endShape = createLineEnd(endSize, endDeco, new Point2D.Double(pend.getX() + 15, pend.getY() + 25), pend);
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
     * Defines how much we increment the angle calculated in getAngle() for each
     * self-referential edge discovered.  Since we want to advance 3/8 of
     * a circle for each edge, the value below should be Pi that increments by
     * Pi * 3/8.
     * But since the iterator in getAngle discovers each edge twice (since the
     * edge has a connection to both its head and tail, the math works out to
     * rotate only half that much.
     */
    private final static double ROT_INCR = Math.PI;

    /**
     * This class will determine if there are other self-referential edges
     * attached to this node and try to return a different angle for different
     * edges, so they will be rendered at different "clock" points around the
     * node circle.
     *
     * @return a different angle for each self-referential edge
     */
    private double getAngle() {
        vEdgeCell vec = (vEdgeCell) view.getCell();
        Edge edg = (Edge) vec.getUserObject();

        CircleCell vcc = (CircleCell) view.getSource().getParentView().getCell();
        EventNode en = (EventNode) vcc.getUserObject();
        double retd = ROT_INCR;
        for (ViskitElement ve : en.getConnections()) {
            Edge e = (Edge) ve;
            if (e.to == en && e.from == en) {
                retd += (ROT_INCR * 3/8);
                if (e == edg) {
                    return retd;
                 }
            }
        }
        return 0.0d;      // should always find one
    }
}
