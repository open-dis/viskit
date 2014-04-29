package viskit.jgraph;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.*;
import viskit.AssemblyViewFrame;
import viskit.ModelEvent;
import viskit.ViskitAssemblyController;
import viskit.model.*;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects MOVES Institute. Naval
 * Postgraduate School, Monterey, CA
 *
 * @author Mike Bailey
 * @since Feb 19, 2004
 * @since 2:54:31 PM
 * @version $Id: vGraphAssemblyComponent.java 2323 2012-06-19 23:11:11Z tdnorbra
 * $
 */
public class vGraphAssemblyComponent extends JGraph implements GraphModelListener {

    vGraphAssemblyModel model;
    AssemblyViewFrame parent;
    protected Action removeAction;

    public vGraphAssemblyComponent(vGraphAssemblyModel model, AssemblyViewFrame frame) {
        super(model);
        parent = frame;

        vGraphAssemblyComponent instance = this;
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
            ((ViskitAssemblyController) parent.getController()).copy();
        }
    }

    class myCutKeyHandler extends AbstractAction {

        myCutKeyHandler() {
            super("cut");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((ViskitAssemblyController) parent.getController()).cut();
        }
    }

    class myPasteKeyHandler extends AbstractAction {

        myPasteKeyHandler() {
            super("paste");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((ViskitAssemblyController) parent.getController()).paste();
        }
    }

    class myRemoveKeyHandler extends AbstractAction {

        myRemoveKeyHandler() {
            super("remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!vGraphAssemblyComponent.this.isSelectionEmpty()) {
                Object[] cells = vGraphAssemblyComponent.this.getSelectionCells();
                cells = vGraphAssemblyComponent.this.getDescendants(cells);
                vGraphAssemblyComponent.this.getModel().remove(cells);
            }
        }
    }

    @Override
    public void updateUI() {
        // Install a new UI
        setUI(new vGraphAssemblyUI(this));    // we use our own for node/edge inspector editting
        //setUI(new BasicGraphUI());   // test
        invalidate();
    }

    private ModelEvent currentModelEvent = null;

    public void viskitModelChanged(ModelEvent ev) {
        currentModelEvent = ev;

        switch (ev.getID()) {
            case ModelEvent.NEWASSEMBLYMODEL:
                model.deleteAll();
                break;
            case ModelEvent.EVENTGRAPHADDED:

                // Reclaimed from the model to here
                insert((EvGraphNode) ev.getSource());
                break;
            case ModelEvent.EVENTGRAPHDELETED:
                model.deleteEGNode((EvGraphNode) ev.getSource());
                break;
            case ModelEvent.EVENTGRAPHCHANGED:
                model.changeEGNode((EvGraphNode) ev.getSource());
                break;

            case ModelEvent.PCLADDED:

                // Reclaimed from the model to here
                insert((PropChangeListenerNode) ev.getSource());
                break;
            case ModelEvent.PCLDELETED:
                model.deletePCLNode((PropChangeListenerNode) ev.getSource());
                break;
            case ModelEvent.PCLCHANGED:
                model.changePCLNode((PropChangeListenerNode) ev.getSource());
                break;

            case ModelEvent.ADAPTEREDGEADDED:
                model.addAdapterEdge((AdapterEdge) ev.getSource());
                break;
            case ModelEvent.ADAPTEREDGEDELETED:
                model.deleteAdapterEdge((AdapterEdge) ev.getSource());
                break;
            case ModelEvent.ADAPTEREDGECHANGED:
                model.changeAdapterEdge((AdapterEdge) ev.getSource());
                break;

            case ModelEvent.SIMEVLISTEDGEADDED:
                model.addSimEvListEdge((SimEvListenerEdge) ev.getSource());
                break;
            case ModelEvent.SIMEVLISTEDGEDELETED:
                model.deleteSimEvListEdge((SimEvListenerEdge) ev.getSource());
                break;
            case ModelEvent.SIMEVLISTEDGECHANGED:
                model.changeSimEvListEdge((SimEvListenerEdge) ev.getSource());
                break;

            case ModelEvent.PCLEDGEADDED:
                model.addPclEdge((PropChangeEdge) ev.getSource());
                break;
            case ModelEvent.PCLEDGEDELETED:
                model.deletePclEdge((PropChangeEdge) ev.getSource());
                break;
            case ModelEvent.PCLEDGECHANGED:
                model.changePclEdge((PropChangeEdge) ev.getSource());
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
        if (currentModelEvent != null && currentModelEvent.getSource() != this.model) // bail if this came from outside
        {
            return;
        }  // this came in from outside, we don't have to inform anybody..prevent reentry
        //todo confirm any other events that should cause us to bail here
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
                        ((ViskitAssemblyModel) parent.getModel()).changeEvGraphNode(en);

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
                        ((ViskitAssemblyModel) parent.getModel()).changePclNode(pcln);

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
                        sb.append("</center>");
                    } else if (se instanceof SimEvListenerEdge) {
                        sb.append("<center>SimEvent Listener<br><u>");
                        sb.append(to);
                        sb.append("</u> listening to <u>");
                        sb.append(from);
                        sb.append("</center>");
                    } else {
                        String prop = ((PropChangeEdge) se).getProperty();
                        prop = (prop != null && prop.length() > 0) ? prop : "*all*";
                        sb.append("<center>Property Change Listener<br><u>");
                        sb.append(to);
                        sb.append("</u> listening to <u>");
                        sb.append(from);
                        sb.append(".");
                        sb.append(prop);
                        sb.append("</center>");
                    }
                    String desc = se.getDescriptionString();
                    if (desc != null) {
                        desc = desc.trim();
                        if (desc.length() > 0) {
                            sb.append("<u>description</u><br>");
                            sb.append(wrapAtPos(escapeLTGT(desc), 60));
                        }
                    }
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
                    sb.append("</center>");
                    if (desc != null) {
                        desc = desc.trim();
                        if (desc.length() > 0) {
                            sb.append("<u>description</u><br>");
                            sb.append(wrapAtPos(escapeLTGT(desc), 60));
                        }
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
     * This class informs the controller that the selected set has changed.
     * Since we're only using this to (dis)able the cut and copy menu items, it
     * could be argued that this functionality should be internal to the View,
     * and the controller needn't be involved. Nevertheless, the round trip
     * through the controller remains in place.
     */
    class myGraphSelectionListener implements GraphSelectionListener {

        Vector<Object> selected = new Vector<Object>();

        @Override
        public void valueChanged(GraphSelectionEvent e) {
            boolean enabled = !vGraphAssemblyComponent.this.isSelectionEmpty();
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
            ((ViskitAssemblyController) parent.getController()).selectNodeOrEdge(selected);
        }
    }

// MarqueeHandler that Connects Vertices and Displays PopupMenus
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
         *
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
            if (port != null && vGraphAssemblyComponent.this.isPortsVisible()) {
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
//                Object cell = vGraphAssemblyComponent.this.getFirstCellForLocation(e.getX(), e.getY());
                // Create PopupMenu for the Cell
//                JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
                // Display PopupMenu
//                menu.show(vGraphAssemblyComponent.this, e.getX(), e.getY());
                // Else if in ConnectMode and Remembered Port is Valid
            } else if (port != null && vGraphAssemblyComponent.this.isPortsVisible()) {
                // Remember Start Location
                start = vGraphAssemblyComponent.this.toScreen(port.getLocation());
                // Remember First Port
                firstPort = port;
            } else {
                // Call Superclass
                super.mousePressed(e);
            }
        }

        // Find Port under Mouse and Repaint Connector
        @Override
        public void mouseDragged(MouseEvent e) {
            // If remembered Start Point is Valid
            if (start != null) {
                // Fetch Graphics from Graph
                Graphics g = vGraphAssemblyComponent.this.getGraphics();
                // Reset Remembered Port
                PortView newPort = getTargetPortAt(e.getPoint());
                // Do not flicker (repaint only on real changes)
                if (newPort == null || newPort != port) {
                    // Xor-Paint the old Connector (Hide old Connector)
                    paintConnector(Color.black, vGraphAssemblyComponent.this.getBackground(), g);
                    // If Port was found then Point to Port Location
                    port = newPort;
                    if (port != null) {
                        current = vGraphAssemblyComponent.this.toScreen(port.getLocation());
                    } // Else If no Port was found then Point to Mouse Location
                    else {
                        current = vGraphAssemblyComponent.this.snap(e.getPoint());
                    }
                    // Xor-Paint the new Connector
                    paintConnector(vGraphAssemblyComponent.this.getBackground(), Color.black, g);
                }
            }
            // Call Superclass
            super.mouseDragged(e);
        }

        public PortView getSourcePortAt(Point2D point) {
            // Disable jumping
            vGraphAssemblyComponent.this.setJumpToDefaultPort(false);
            PortView result;
            try {
                // Find a Port View in Model Coordinates and Remember
                result = vGraphAssemblyComponent.this.getPortViewAt(point.getX(), point.getY());
            } finally {
                vGraphAssemblyComponent.this.setJumpToDefaultPort(true);
            }
            return result;
        }

        // Find a Cell at point and Return its first Port as a PortView
        protected PortView getTargetPortAt(Point2D point) {
            // Find a Port View in Model Coordinates and Remember
            return vGraphAssemblyComponent.this.getPortViewAt(point.getX(), point.getY());
        }

        // Connect the First Port and the Current Port in the Graph or Repaint
        @Override
        public void mouseReleased(MouseEvent e) {
            highlight(vGraphAssemblyComponent.this, null);

            // If Valid Event, Current and First Port
            if (e != null && port != null && firstPort != null
                    && firstPort != port) {
                // Then Establish Connection
                connect((Port) firstPort.getCell(), (Port) port.getCell());
                e.consume();
                // Else Repaint the Graph
            } else {
                vGraphAssemblyComponent.this.repaint();
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
                    && vGraphAssemblyComponent.this.isPortsVisible()) {
                // Set Cusor on Graph (Automatically Reset)
                vGraphAssemblyComponent.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            if (vGraphAssemblyComponent.this.isXorEnabled()) {
                // Set Foreground
                g.setColor(fg);
                // Set Xor-Mode Color
                g.setXORMode(bg);
                // Highlight the Current Port
                paintPort(vGraphAssemblyComponent.this.getGraphics());

                drawConnectorLine(g);
            } else {
                Rectangle dirty = new Rectangle((int) start.getX(), (int) start.getY(), 1, 1);

                if (current != null) {
                    dirty.add(current);
                }

                dirty.grow(1, 1);

                vGraphAssemblyComponent.this.repaint(dirty);
                highlight(vGraphAssemblyComponent.this, port);
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
                r = vGraphAssemblyComponent.this.toScreen((Rectangle2D) r.clone());
                // Add Space For the Highlight Border
                r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
                        .getHeight() + 6);
                // Paint Port in Preview (=Highlight) Mode
                vGraphAssemblyComponent.this.getUI().paintCell(g, port, r, true);
            }
        }

        /**
         * Highlights the given cell view or removes the highlight if no cell
         * view is specified.
         *
         * @param graph
         * @param cellView
         */
        protected void highlight(JGraph graph, CellView cellView) {
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

        /**
         * Returns the bounds to be used to highlight the given cell view.
         *
         * @param graph
         * @param cellView
         * @return
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

    // NOTE: Not currently used
    // PopupMenu
    //
    public JPopupMenu createPopupMenu(final Point pt, final Object cell) {
        JPopupMenu menu = new JPopupMenu();
        if (cell != null) {
            // Edit
            menu.add(new AbstractAction("Edit") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    vGraphAssemblyComponent.this.startEditingAtCell(cell);
                }
            });
        }
        // Remove
        if (!vGraphAssemblyComponent.this.isSelectionEmpty()) {
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

    // Insert a new Edge between source and target
    public void connect(Port source, Port target) {
        DefaultGraphCell src = (DefaultGraphCell) vGraphAssemblyComponent.this.getModel().getParent(source);
        DefaultGraphCell tar = (DefaultGraphCell) vGraphAssemblyComponent.this.getModel().getParent(target);
        Object[] oa = new Object[]{src, tar};
        ViskitAssemblyController controller = (ViskitAssemblyController) parent.getController();

        if (parent.getCurrentMode() == AssemblyViewFrame.ADAPTER_MODE) {
            controller.newAdapterArc(oa);
        } else if (parent.getCurrentMode() == AssemblyViewFrame.SIMEVLIS_MODE) {
            controller.newSimEvListArc(oa);
        } else if (parent.getCurrentMode() == AssemblyViewFrame.PCL_MODE) {
            controller.newPropChangeListArc(oa);
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
        vertex.getAttributes().applyMap(createCellAttributes(node.getPosition()));

        // Insert the Vertex (including child port and attributes)
        getGraphLayoutCache().insert(vertex);
        getGraphLayoutCache().toFront(new Object[]{vertex});
    }
}

/**
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
     * Paint the vapvr. Overridden to do a double line and paint over the end
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
