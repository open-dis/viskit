package viskit.jgraph;

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute.
 * Naval Postgraduate School, Monterey, CA
 * User: mike
 * Date: Feb 19, 2004
 * Time: 2:54:31 PM
 */
public class vGraphAssemblyComponent extends JGraph implements GraphModelListener
    /**********************************************/
{
  vGraphAssemblyModel model;
  AssemblyViewFrame parent;

  public vGraphAssemblyComponent(vGraphAssemblyModel model, AssemblyViewFrame frame)
  {
    super(model);
    parent = frame;
    ToolTipManager.sharedInstance().registerComponent(this);
    //super.setDoubleBuffered(false); // test for mac
    this.model = model;
    this.setModel(model);
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
    model.addGraphModelListener(this);

    setupCutCopyPaste();
    
    //this.setMarqueeColor(Color.red);
    this.setLockedHandleColor(Color.red);
    this.setHighlightColor(Color.red);
    //this.setHandleColor(Color.orange);
    this.setDropEnabled(true);
  }
  private void setupCutCopyPaste()
  {
    // Handle keystrokes
    AbstractAction cutAction = new myCutKeyHandler();
    Action copyAction = new myCopyKeyHandler();
    Action pasteAction = new myPasteKeyHandler();

    int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X,accelMod),
                  cutAction.getValue(Action.NAME));
    this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C,accelMod),
                  copyAction.getValue(Action.NAME));
    this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,accelMod),
                  pasteAction.getValue(Action.NAME));
    this.getActionMap().put(cutAction.getValue(Action.NAME),cutAction);
    this.getActionMap().put(copyAction.getValue(Action.NAME),copyAction);
    this.getActionMap().put(pasteAction.getValue(Action.NAME),pasteAction);
  }

  class myCopyKeyHandler extends AbstractAction
  {
    myCopyKeyHandler()
    {
      super("copy");
    }
    public void actionPerformed(ActionEvent e)
    {
      ((ViskitAssemblyController) parent.getController()).copy();
    }
  }
  class myCutKeyHandler extends AbstractAction
  {
    myCutKeyHandler()
    {
      super("cut");
    }
    public void actionPerformed(ActionEvent e)
    {
      ((ViskitAssemblyController) parent.getController()).cut();
    }
  }
  class myPasteKeyHandler extends AbstractAction
  {
    myPasteKeyHandler()
    {
      super("paste");
    }
    public void actionPerformed(ActionEvent e)
    {
      ((ViskitAssemblyController) parent.getController()).paste();
    }
  }

  public void updateUI()
  {
    // Install a new UI
    setUI(new vGraphAssemblyUI(this));    // we use our own for node/edge inspector editting
    //setUI(new BasicGraphUI());   // test
    invalidate();
  }

  public ViskitElement getViskitElementAt(Point p)
  {
    Object cell = vGraphAssemblyComponent.this.getFirstCellForLocation(p.x, p.y);
    if(cell != null && (cell instanceof AssemblyCircleCell || cell instanceof AssemblyPropListCell))
      return (ViskitElement)((DefaultGraphCell)cell).getUserObject();
    return null;
  }

  private ModelEvent currentModelEvent = null;

  public void viskitModelChanged(ModelEvent ev)
  {
    currentModelEvent = ev;

    switch (ev.getID()) {
      case ModelEvent.NEWASSEMBLYMODEL:
        model.deleteAll();
        break;
      case ModelEvent.EVENTGRAPHADDED:
        model.addEGNode((EvGraphNode)ev.getSource());
        break;
      case ModelEvent.EVENTGRAPHDELETED:
        model.deleteEGNode((EvGraphNode)ev.getSource());
        break;
      case ModelEvent.EVENTGRAPHCHANGED:
        model.changeEGNode((EvGraphNode)ev.getSource());
        break;

      case ModelEvent.PCLADDED:
        model.addPCLNode((PropChangeListenerNode)ev.getSource());
        break;
      case ModelEvent.PCLDELETED:
        model.deletePCLNode((PropChangeListenerNode)ev.getSource());
        break;
      case ModelEvent.PCLCHANGED:
        model.changePCLNode((PropChangeListenerNode)ev.getSource());
        break;

      case ModelEvent.ADAPTEREDGEADDED:
        model.addAdapterEdge((AdapterEdge)ev.getSource());
        break;
      case ModelEvent.ADAPTEREDGEDELETED:
        model.deleteAdapterEdge((AdapterEdge)ev.getSource());
        break;
      case ModelEvent.ADAPTEREDGECHANGED:
        model.changeAdapterEdge((AdapterEdge)ev.getSource());
        break;

      case ModelEvent.SIMEVLISTEDGEADDED:
        model.addSimEvListEdge((SimEvListenerEdge)ev.getSource());
        break;
      case ModelEvent.SIMEVLISTEDGEDELETED:
        model.deleteSimEvListEdge((SimEvListenerEdge)ev.getSource());
        break;
      case ModelEvent.SIMEVLISTEDGECHANGED:
        model.changeSimEvListEdge((SimEvListenerEdge)ev.getSource());
        break;

      case ModelEvent.PCLEDGEADDED:
        model.addPclEdge((PropChangeEdge)ev.getSource());
        break;
      case ModelEvent.PCLEDGEDELETED:
        model.deletePclEdge((PropChangeEdge)ev.getSource());
        break;
      case ModelEvent.PCLEDGECHANGED:
        model.changePclEdge((PropChangeEdge)ev.getSource());
        break;

      default:
        //System.out.println("duh");
    }
    currentModelEvent = null;
  }

  /**
   * GraphModelListener entry.  We use this to listen for position changes so
   * we can stuff them into EventNode
   *
   * @param e
   */
  public void graphChanged(GraphModelEvent e)
  {
 //   if(currentModelEvent!= null && currentModelEvent.getID() == ModelEvent.NEWMODEL)
    if(currentModelEvent != null && currentModelEvent.getSource() != this.model)
      // bail if this came from outside
      return;  // this came in from outside, we don't have to inform anybody..prevent reentry
    //todo confirm any other events that should cause us to bail here
    GraphModelEvent.GraphModelChange c = e.getChange();
    Object[] ch = c.getChanged();
    if (ch != null) {
      for (int i = 0; i < ch.length; i++) {
        if (ch[i] instanceof AssemblyCircleCell) {
          AssemblyCircleCell cc = (AssemblyCircleCell) ch[i];
          Map m = cc.getAttributes();
          Rectangle r = (Rectangle) m.get("bounds");
          if (r != null) {
            EvGraphNode en = (EvGraphNode) cc.getUserObject();
            en.setPosition(new Point(r.x, r.y));
            ((ViskitAssemblyModel) parent.getModel()).changeEvGraphNode(en);
            m.put("bounds",new Rectangle(en.getPosition().x, en.getPosition().y,r.width,r.height));
          }
        }
        else if (ch[i] instanceof AssemblyPropListCell) {
          AssemblyPropListCell plc = (AssemblyPropListCell) ch[i];
          Map m = plc.getAttributes();
          Rectangle r = (Rectangle) m.get("bounds");
          if (r != null) {
            PropChangeListenerNode pcln = (PropChangeListenerNode) plc.getUserObject();
            pcln.setPosition(new Point(r.x, r.y));
            ((ViskitAssemblyModel) parent.getModel()).changePclNode(pcln);
            // might have changed:
            m.put("bounds",new Rectangle(pcln.getPosition().x, pcln.getPosition().y,r.width,r.height));
          }
        }
      }
    }
  }

  public String getToolTipText(MouseEvent event)
  {
    if (event != null) {
      Object c = this.getFirstCellForLocation(event.getX(), event.getY());
      if (c != null) {
        StringBuffer sb = new StringBuffer("<html>");
        if (c instanceof vAssemblyEdgeCell) {
          vAssemblyEdgeCell vc = (vAssemblyEdgeCell) c;
          AssemblyEdge se = (AssemblyEdge) vc.getUserObject();
          Object to = se.getTo();
          Object from = se.getFrom();

          if(se instanceof AdapterEdge) {
            Object toEv = ((AdapterEdge)se).getTargetEvent();
            Object frEv = ((AdapterEdge)se).getSourceEvent();
            sb.append("<center>Adapter<br><u>");// +
            sb.append(from);
            sb.append(".");
            sb.append(frEv);
            sb.append("</u> connected to <u>");
            sb.append(to);
            sb.append(".");
            sb.append(toEv);
            sb.append("</center>");
          }
          else if(se instanceof SimEvListenerEdge) {
            sb.append("<center>SimEvent Listener<br><u>");
            sb.append(to);
            sb.append("</u> listening to <u>");
            sb.append(from);
            sb.append("</center>");
          }
          else /*if(vc.getUserObject() instanceof PropChangeEdge)*/ {
            String prop = ((PropChangeEdge)se).getProperty();
            prop = (prop != null &&prop.length()>0) ? prop : "*all*";
            sb.append("<center>Property Change Listener<br><u>");
            sb.append(from);
            sb.append("</u> listening to <u>");
            sb.append(to);
            sb.append(".");
            sb.append(prop);
            sb.append("</center>");
          }
          if(se.getDescription() != null) {
            String desc = se.getDescription().trim();
            sb.append("<u>description</u><br>");
            sb.append(wrapAtPos(escapeLTGT(desc),60));
          }
          sb.append("</html>");
          return sb.toString();
        }
        else if (c instanceof AssemblyCircleCell) {
          AssemblyCircleCell cc = (AssemblyCircleCell) c;
          EvGraphNode en = (EvGraphNode) cc.getUserObject();
          sb.append("<center><u>");
          sb.append(en.getType());
          sb.append("</u><br>");
          sb.append(en.getName());
          sb.append("</center>");
          if(en.getDescription() != null) {
            String desc = en.getDescription().trim();
            sb.append("<u>description</u><br>");
            sb.append(wrapAtPos(escapeLTGT(desc),60));
          }
          sb.append("</HTML>");
          return sb.toString();
        }
        else if (c instanceof AssemblyPropListCell) {
          AssemblyPropListCell cc = (AssemblyPropListCell) c;
          PropChangeListenerNode en = (PropChangeListenerNode) cc.getUserObject();
          sb.append("<center><u>");
          sb.append(en.getType());
          sb.append("</u><br>");
          sb.append(en.getName());
          sb.append("</center>");
          if(en.getDescription() != null) {
            String desc = en.getDescription().trim();
            sb.append("<u>description</u><br>");
            sb.append(wrapAtPos(escapeLTGT(desc),60));
          }
          sb.append("</HTML>");
          return sb.toString();
        }
      }
    }
    return null;
  }

  private String wrapAtPos(String s, int len)
  {
    String[] sa = s.split(" ");
    StringBuffer sb = new StringBuffer();
    int idx=0;
    do {
      int ll=0;
      sb.append("&nbsp;");
      do {
        ll+= sa[idx].length() + 1;
        sb.append(sa[idx++]);
        sb.append(" ");
      }
      while(idx < sa.length && ll < len);
      sb.append("<br>");
    }
    while(idx < sa.length);

    String st = sb.toString();
    if(st.endsWith("<br>"))
      st = st.substring(0,st.length()-4);
    return st.trim();
  }

  private String escapeLTGT(String s)
  {
    s = s.replaceAll("<","&lt;");
    s = s.replaceAll(">","&gt;");
    return s;
  }

  public String convertValueToString(Object value)
  {
    CellView view = (value instanceof CellView)
        ? (CellView) value
        : getGraphLayoutCache().getMapping(value, false);

    if (view instanceof AssemblyCircleView) {
      AssemblyCircleCell cc = (AssemblyCircleCell) view.getCell();
      Object en = cc.getUserObject();
      if (en instanceof EvGraphNode)
        return ((EvGraphNode) en).getName();    // label name is actually gotten in paintComponent
    }
/*
    else if (view instanceof vAssemblyEdgeView) {
      vAssemblyEdgeCell aec = (vAssemblyEdgeCell)view.getCell();
      Object e = aec.getUserObject();
      if( e instanceof PropChangeEdge) {
        return "PropChangeEdge"; // temp
      }
      else if ( e instanceof SimEvListenerEdge ) {
        return "SimEvListenerEdge"; // temp
      }
      else if ( e instanceof AdapterEdge ) {
        return " AdapterEdge";
      }
    }
*/
/*         old
    else if (view instanceof vEdgeView) {
      vEdgeCell cc = (vEdgeCell) view.getCell();
      Object e = cc.getUserObject();
      if (e instanceof SchedulingEdge) {
        SchedulingEdge se = (SchedulingEdge) e;
        if (se.conditional == null || se.conditional.length() <= 0)   // put S only for conditional edges
          return null;
        return "S";
      }
      else if (e instanceof CancellingEdge) // should always be one of these 2 except for proto examples
        return null;
    }
*/
    return null;
  }


  // To use circles, from the tutorial
  protected VertexView createVertexView(Object v, CellMapper cm)
  {
    if (v instanceof AssemblyCircleCell)
      return new AssemblyCircleView(v, this, cm);
    else if (v instanceof AssemblyPropListCell)
      return new AssemblyPropListView(v, this, cm);
    // else
    return super.createVertexView(v, cm);
  }

  // To customize my edges
  protected EdgeView createEdgeView(Object e, CellMapper cm)
  {
    if (e instanceof vAssemblyEdgeCell) {
      Object o = ((vAssemblyEdgeCell)e).getUserObject();
      if(o instanceof PropChangeEdge)
        return new vAssyPclEdgeView(e, this, cm);
      if(o instanceof AdapterEdge)
        return new vAssyAdapterEdgeView(e, this, cm);
      if(o instanceof SimEvListenerEdge)
        return new vAssySelEdgeView(e, this, cm);
    }
    return super.createEdgeView(e, cm);
  }

  protected PortView createPortView(Object p, CellMapper cm)
  {
    if (p instanceof vAssemblyPortCell)
      return new vAssemblyPortView(p,this,cm);
    return super.createPortView(p, cm);
  }

  /**
   * This class informs the controller that the selected set has changed.  Since we're only useing this
   * to (dis)able the cut and copy menu items, it could be argued that this functionality should be internal
   * to the View, and the controller needn't be involved.  Nevertheless, the round trip through the controller
   * remains in place.
   */
  class myGraphSelectionListener implements GraphSelectionListener
  {
    Vector selected = new Vector();

    public void valueChanged(GraphSelectionEvent e)
    {
      Object[] oa = e.getCells();
      if (oa == null || oa.length <= 0)
        return;
      for (int i = 0; i < oa.length; i++)
        if (e.isAddedCell(i))
          selected.add(((DefaultGraphCell) oa[i]).getUserObject());
        else
          selected.remove(((DefaultGraphCell) oa[i]).getUserObject());
      ((ViskitAssemblyController) parent.getController()).selectNodeOrEdge(selected);
    }
  }

// MarqueeHandler that Connects Vertices and Displays PopupMenus
  public class MyMarqueeHandler extends BasicMarqueeHandler
  {

    // Holds the Start and the Current Point
    protected Point start, current;

    // Holds the First and the Current Port
    protected PortView port, firstPort;

    // Override to Gain Control (for PopupMenu and ConnectMode)
    public boolean isForceMarqueeEvent(MouseEvent e)
    {
      // If Right Mouse Button we want to Display the PopupMenu
      if (SwingUtilities.isRightMouseButton(e))
      // Return Immediately
        return true;
      // Find and Remember Port
      port = getSourcePortAt(e.getPoint());
      // If Port Found and in ConnectMode (=Ports Visible)
      if (port != null && vGraphAssemblyComponent.this.isPortsVisible())
        return true;
      // Else Call Superclass
      return super.isForceMarqueeEvent(e);
    }

    // Display PopupMenu or Remember Start Location and First Port
    public void mousePressed(final MouseEvent e)
    {
      // If Right Mouse Button
      if (SwingUtilities.isRightMouseButton(e)) {
        // Scale From Screen to Model
        // Point loc = vGraphAssemblyComponent.this.fromScreen(e.getPoint());
        // Find Cell in Model Coordinates
        // Object cell = vGraphAssemblyComponent.this.getFirstCellForLocation(loc.x, loc.y);
        // Create PopupMenu for the Cell
        // JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
        // Display PopupMenu

        // jmb...not today
        // menu.show(vGraphComponent.this, e.getX(), e.getY());

        // Else if in ConnectMode and Remembered Port is Valid
      }
      else if (port != null && !e.isConsumed() && vGraphAssemblyComponent.this.isPortsVisible()) {
        // Remember Start Location
        start = vGraphAssemblyComponent.this.toScreen(port.getLocation(null));
        // Remember First Port
        firstPort = port;
        // Consume Event
        e.consume();
      }
      else
      // Call Superclass
        super.mousePressed(e);
    }

    // Find Port under Mouse and Repaint Connector
    public void mouseDragged(MouseEvent e)
    {
      // If remembered Start Point is Valid
      if (start != null && !e.isConsumed()) {
        // Fetch Graphics from Graph
        Graphics g = vGraphAssemblyComponent.this.getGraphics();
        // Xor-Paint the old Connector (Hide old Connector)
        paintConnector(Color.black, vGraphAssemblyComponent.this.getBackground(), g);
        // Reset Remembered Port
        port = getTargetPortAt(e.getPoint());
        // If Port was found then Point to Port Location
        if (port != null)
          current = vGraphAssemblyComponent.this.toScreen(port.getLocation(null));
        // Else If no Port was found then Point to Mouse Location
        else
          current = vGraphAssemblyComponent.this.snap(e.getPoint());
        // Xor-Paint the new Connector
        paintConnector(vGraphAssemblyComponent.this.getBackground(), Color.black, g);
        // Consume Event
        e.consume();
      }
      // Call Superclass
      super.mouseDragged(e);
    }

    public PortView getSourcePortAt(Point point)
    {
      // Scale from Screen to Model
      Point tmp = vGraphAssemblyComponent.this.fromScreen(new Point(point));
      // Find a Port View in Model Coordinates and Remember
      return vGraphAssemblyComponent.this.getPortViewAt(tmp.x, tmp.y);
    }

    // Find a Cell at point and Return its first Port as a PortView
    protected PortView getTargetPortAt(Point point)
    {
      // Find Cell at point (No scaling needed here)
      Object cell = vGraphAssemblyComponent.this.getFirstCellForLocation(point.x, point.y);
      // Loop Children to find PortView
      for (int i = 0; i < vGraphAssemblyComponent.this.getModel().getChildCount(cell); i++) {
        // Get Child from Model
        Object tmp = vGraphAssemblyComponent.this.getModel().getChild(cell, i);
        // Get View for Child using the Graph's View as a Cell Mapper
        //jmb fix  tmp = graphPane.getView().getMapping(tmp, false);
        // If Child View is a Port View and not equal to First Port
        if (tmp instanceof PortView && tmp != firstPort)
        // Return as PortView
          return (PortView) tmp;
      }
      // No Port View found
      return getSourcePortAt(point);
    }

    // Connect the First Port and the Current Port in the Graph or Repaint
    public void mouseReleased(MouseEvent e)
    {
      // If Valid Event, Current and First Port
      if (e != null && !e.isConsumed() && port != null && firstPort != null &&
          firstPort != port) {
        // Then Establish Connection
        connect((Port) firstPort.getCell(), (Port) port.getCell());
        // Consume Event
        e.consume();
        // Else Repaint the Graph
      }
      else
        vGraphAssemblyComponent.this.repaint();
      // Reset Global Vars
      firstPort = port = null;
      start = current = null;
      // Call Superclass
      super.mouseReleased(e);
    }

    // Show Special Cursor if Over Port
    public void mouseMoved(MouseEvent e)
    {
      // Check Mode and Find Port
      if (e != null && getSourcePortAt(e.getPoint()) != null &&
          !e.isConsumed() && vGraphAssemblyComponent.this.isPortsVisible()) {
        // Set Cusor on Graph (Automatically Reset)
        vGraphAssemblyComponent.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Consume Event
        e.consume();
      }
      // Call Superclass
      super.mouseMoved(e); //this was super.mouseReleased(e);  apparently was not screwing things up
    }

    // Use Xor-Mode on Graphics to Paint Connector
    protected void paintConnector(Color fg, Color bg, Graphics g)
    {
      // Set Foreground
      g.setColor(fg);
      // Set Xor-Mode Color
      g.setXORMode(bg);
      // Highlight the Current Port
      paintPort(vGraphAssemblyComponent.this.getGraphics());
      // If Valid First Port, Start and Current Point
      if (firstPort != null && start != null && current != null)
      // Then Draw A Line From Start to Current Point
        g.drawLine(start.x, start.y, current.x, current.y);
    }

    // Use the Preview Flag to Draw a Highlighted Port
    protected void paintPort(Graphics g)
    {
      // If Current Port is Valid
      if (port != null) {
        // If Not Floating Port...
        boolean o = (GraphConstants.getOffset(port.getAttributes()) != null);
        // ...Then use Parent's Bounds
        Rectangle r = (o) ? port.getBounds() : port.getParentView().getBounds();
        // Scale from Model to Screen
        r = vGraphAssemblyComponent.this.toScreen(new Rectangle(r));
        // Add Space For the Highlight Border
        //r.setBounds(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
        r.setBounds(r.x - 5, r.y - 5, r.width + 10, r.height + 10);
        // Paint Port in Preview (=Highlight) Mode
        vGraphAssemblyComponent.this.getUI().paintCell(g, port, r, true);
      }
    }

    // Insert a new Vertex at point
    public void insert(Point point)
    {
      // Construct Vertex with no Label
      DefaultGraphCell vertex = new DefaultGraphCell();
      // Add one Floating Port
      vertex.add(new DefaultPort());
      // Snap the Point to the Grid
      point = vGraphAssemblyComponent.this.snap(new Point(point));
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
      //Hashtable attributes = new Hashtable();
      // Associate the Vertex with its Attributes
      //attributes.put(vertex, map);

      // Insert the Vertex and its Attributes
      //   graphPane.getModel().insert(new Object[]{vertex}, null, null, attributes);
    }

    // Insert a new Edge between source and target
    public void connect(Port source, Port target)
    {
      DefaultGraphCell src = (DefaultGraphCell) vGraphAssemblyComponent.this.getModel().getParent(source);
      DefaultGraphCell tar = (DefaultGraphCell) vGraphAssemblyComponent.this.getModel().getParent(target);
      Object[] oa = new Object[]{src, tar};
      ViskitAssemblyController controller = (ViskitAssemblyController) parent.getController();

      if(parent.getCurrentMode() == AssemblyViewFrame.ADAPTER_MODE)
        controller.newAdapterArc(oa);
      else if(parent.getCurrentMode() == AssemblyViewFrame.SIMEVLIS_MODE)
        controller.newSimEvListArc(oa);
      else if(parent.getCurrentMode() == AssemblyViewFrame.PCL_MODE)
        controller.newPropChangeListArc(oa);
    }

    public JPopupMenu createPopupMenu(final Point pt, final Object cell)
    {
      JPopupMenu menu = new JPopupMenu();
      if (cell != null) {
        // Edit
        menu.add(new AbstractAction("Edit")
        {
          public void actionPerformed(ActionEvent e)
          {
            vGraphAssemblyComponent.this.startEditingAtCell(cell);
          }
        });
      }
      // Remove
      if (!vGraphAssemblyComponent.this.isSelectionEmpty()) {
        menu.addSeparator();
        menu.add(new AbstractAction("Remove")
        {
          public void actionPerformed(ActionEvent e)
          {
            // jmb fix remove.actionPerformed(e);
            // remove is an Action
          }
        });
      }
      menu.addSeparator();
      // Insert
      menu.add(new AbstractAction("Insert")
      {
        public void actionPerformed(ActionEvent ev)
        {
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
class vAssemblyEdgeCell extends DefaultEdge
{
  public vAssemblyEdgeCell()
  {
    this(null);
  }

  public vAssemblyEdgeCell(Object userObject)
  {
    super(userObject);
  }
}

/*
class vSelfEdgeCell extends vEdgeCell
{
  public vSelfEdgeCell()
  {
    this(null);
  }

  public vSelfEdgeCell(Object userObject)
  {
    super(userObject);
  }
}
*/

class vAssemblyPortCell extends DefaultPort
{
  public vAssemblyPortCell()
  {
    this(null);
  }

  public vAssemblyPortCell(Object o)
  {
    this(o,null);
  }

  public vAssemblyPortCell(Object o, Port port)
  {
    super(o, port);
  }
}

class vAssemblyPortView extends PortView
{
  static int mysize = 54;   // same as the circle

  public vAssemblyPortView(Object o, JGraph jGraph, CellMapper cellMapper)
  {
    super(o, jGraph, cellMapper);
  }

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
 * To mark our nodes.
 */
class AssemblyPropListCell extends DefaultGraphCell
{
  AssemblyPropListCell()
  {
    this(null);
  }

  public AssemblyPropListCell(Object userObject)
  {
    super(userObject);
  }

}
/**
 * Sub class VertexView to install our own renderer.
 */
class AssemblyPropListView extends VertexView
{
  static vAssemblyPclVertexRenderer renderer = new vAssemblyPclVertexRenderer();

  public AssemblyPropListView(Object cell, JGraph gr, CellMapper cm)
  {
    super(cell, gr, cm);
  }

  public CellViewRenderer getRenderer()
  {
    return renderer;
  }

}
class AssemblyCircleCell extends DefaultGraphCell
{

  AssemblyCircleCell()
  {
    this(null);
  }

  public AssemblyCircleCell(Object userObject)
  {
    super(userObject);
  }
}

/**
 * Sub class VertexView to install our own renderer.
 */
class AssemblyCircleView extends VertexView
{
  static vAssemblyEgVertexRenderer renderer = new vAssemblyEgVertexRenderer();

  public AssemblyCircleView(Object cell, JGraph gr, CellMapper cm)
  {
    super(cell, gr, cm);
  }

  public CellViewRenderer getRenderer()
  {
    return renderer;
  }

}

// Begin support for custom line ends and double line (adapter) on assembly edges
class vAssyAdapterEdgeView extends vEdgeView
{
  public static vAssyAdapterEdgeRenderer renderer = new vAssyAdapterEdgeRenderer();

  public vAssyAdapterEdgeView(Object cell, JGraph gr, CellMapper cm)
  {
    super(cell, gr, cm);
  }

  public CellViewRenderer getRenderer()
  {
    return renderer;
  }
}

class vAssySelEdgeView extends vEdgeView
{
  public static vAssySelEdgeRenderer renderer = new vAssySelEdgeRenderer();

  public vAssySelEdgeView(Object cell, JGraph gr, CellMapper cm)
  {
    super(cell, gr, cm);
  }

  public CellViewRenderer getRenderer()
  {
    return renderer;
  }
}

class vAssyPclEdgeView extends vEdgeView
{
  public static vAssyPclEdgeRenderer renderer = new vAssyPclEdgeRenderer();

  public vAssyPclEdgeView(Object cell, JGraph gr, CellMapper cm)
  {
    super(cell, gr, cm);
  }

  public CellViewRenderer getRenderer()
  {
    return renderer;
  }
}

class vAssyAdapterEdgeRenderer extends vEdgeRenderer
{
  /**
    * Paint the renderer.  Overridden to do a double line and paint over the end shap
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
       BasicStroke whiteStripeStroke = new BasicStroke(lineWidth/3, c, j);
       BasicStroke onePixStroke = new BasicStroke(1,c,j);

       g2.setStroke(onePixStroke);

       translateGraphics(g);
       g.setColor(getForeground());
       if (view.beginShape != null) {
         if (beginFill)
           g2.fill(view.beginShape);
         g2.draw(view.beginShape);
       }
       if (view.endShape != null) {
         if (endFill)
           g2.fill(view.endShape);
         g2.draw(view.endShape);
       }
       g2.setStroke(lineStroke);
       if (lineDash != null) {// Dash For Line Only
         g2.setStroke(new BasicStroke(lineWidth, c, j, 10.0f, lineDash, 0.0f));
         whiteStripeStroke = new BasicStroke(lineWidth/3, c, j, 10.0f, lineDash, 0.0f);
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
         g2.setColor(graph.getHighlightColor());
         if (view.beginShape != null)
           g2.draw(view.beginShape);
         if (view.lineShape != null)
           g2.draw(view.lineShape);
         if (view.endShape != null)
           g2.draw(view.endShape);
       }
       if (graph.getEditingCell() != view.getCell()) {
         Object label = graph.convertValueToString(view);
         if (label != null) {
           g2.setStroke(new BasicStroke(1));
           g.setFont(getFont());
           paintLabel(g, label.toString());
         }
       }
     }
  }

  @Override
  protected Shape createLineEnd(int size, int style, Point src, Point dst)
  {
    int d = (int) Math.max(1, dst.distance(src));
    int ax = - (size * (dst.x - src.x) / d);
    int ay = - (size * (dst.y - src.y) / d);
    GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO,4);
    path.moveTo(dst.x        - ay/3, dst.y        + ax/3);
    path.lineTo(dst.x + ax/2       , dst.y + ay/2       );
    path.lineTo(dst.x        + ay/3, dst.y        - ax/3);

    return path;
  }

}

class vAssySelEdgeRenderer extends vEdgeRenderer
{
  @Override
  protected Shape createLineEnd(int size, int style, Point src, Point dst)
  {
    // Same as above
    int d = (int) Math.max(1, dst.distance(src));
    int ax = - (size * (dst.x - src.x) / d);
    int ay = - (size * (dst.y - src.y) / d);
    GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO,4);
    path.moveTo(dst.x        - ay/3, dst.y        + ax/3);
    path.lineTo(dst.x + ax/2       , dst.y + ay/2       );
    path.lineTo(dst.x        + ay/3, dst.y        - ax/3);

    return path;
  }
}

class vAssyPclEdgeRenderer extends vEdgeRenderer
{
  @Override
  protected Shape createLineEnd(int size, int style, Point src, Point dst)
  {
    int d = (int) Math.max(1, dst.distance(src));
    int ax = - (size * (dst.x - src.x) / d);
    int ay = - (size * (dst.y - src.y) / d);
    GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO,4);
    path.moveTo(dst.x        - ay/3, dst.y        + ax/3);
    path.lineTo(dst.x + ax/2 - ay/3, dst.y + ay/2 + ax/3);
    path.lineTo(dst.x + ax/2 + ay/3, dst.y + ay/2 - ax/3);
    path.lineTo(dst.x        + ay/3, dst.y        - ax/3);

    return path;
  }
}
// End support for custom line ends and double adapter line on assembly edges

/**
 * A replacement class to tweek the routing slightly so that the edges come into the node from other directions than
 * NSE and W.  Also, support offsetting edges between the same two nodes.
 */
class ViskitAssemblyRouting implements org.jgraph.graph.Edge.Routing
{
  public void route(EdgeView edge, java.util.List points)
  {
    int n = points.size();
    Object fromKey = null, toKey = null;

    Point from = edge.getPoint(0);

    if (edge.getSource() instanceof PortView) {
      from = ((PortView) edge.getSource()).getLocation(null);
      fromKey = getKey((PortView) edge.getSource());
    }
    else if (edge.getSource() != null)
      from = edge.getSource().getBounds().getLocation();

    Point to = edge.getPoint(n - 1);

    if (edge.getTarget() instanceof PortView) {
      to = ((PortView) edge.getTarget()).getLocation(null);
      toKey = getKey((PortView) edge.getTarget());
    }
    else if (edge.getTarget() != null)
      to = edge.getTarget().getBounds().getLocation();

    int adjustFactor = 0;
    if (toKey != null && fromKey != null)
      adjustFactor = getFactor(toKey, fromKey, edge);

    int sig = adjustFactor % 2;
    adjustFactor++;
    adjustFactor /= 2;
    if (sig == 0)
      adjustFactor *= -1;

    int adjustment = /*0  + */ 35 * adjustFactor;       // little bias

    int dx = Math.abs(from.x - to.x);
    int dy = Math.abs(from.y - to.y);
    int x2 = from.x + ((to.x - from.x) / 2);
    int y2 = from.y + ((to.y - from.y) / 2);
    Point[] routed = new Point[2];
    if (dx > dy) {
      routed[0] = new Point(x2, from.y+adjustment);
      routed[1] = new Point(x2, to.y-adjustment);
    }
    else {
      routed[0] = new Point(from.x-adjustment, y2);
      routed[1] = new Point(to.x+adjustment, y2);
    }


    // Set/Add Points
    for (int i = 0; i < routed.length; i++)
      if (points.size() > i + 2)
        points.set(i + 1, routed[i]);
      else
        points.add(i + 1, routed[i]);
    // Remove spare points
    while (points.size() > routed.length + 2) {
      points.remove(points.size() - 2);
    }

  }

  private Object getKey(PortView pv)
  {
    Object o = pv.getParentView();
    if(o instanceof AssemblyCircleView) {
      AssemblyCircleView cv = (AssemblyCircleView) o;
      AssemblyCircleCell cc = (AssemblyCircleCell) cv.getCell();
      EvGraphNode egn = (EvGraphNode) cc.getUserObject();
      return egn.getModelKey();
    }
    else if (o instanceof AssemblyPropListView) {
      AssemblyPropListView apv = (AssemblyPropListView) o;
      AssemblyPropListCell apc = (AssemblyPropListCell) apv.getCell();
      PropChangeListenerNode pn = (PropChangeListenerNode) apc.getUserObject();
      return pn.getModelKey();
    }
    else {
      System.err.println("bad state vGraphAssemblyComponent.getKey()");
      return null;
    }
  }

  static HashMap nodePairs = new HashMap();

  private int getFactor(Object toKey, Object fromKey, EdgeView ev)
  {
    String toStr = toKey.toString();
    String fromStr = fromKey.toString();
    String masterKey = null;
    if (toStr.compareTo(fromStr) > 0)
      masterKey = fromStr + "-" + toStr;
    else
      masterKey = toStr + "-" + fromStr;
    vAssemblyEdgeCell vec = (vAssemblyEdgeCell) ev.getCell();
    AssemblyEdge edg = (AssemblyEdge) vec.getUserObject();
    Object edgeKey = edg.getModelKey();

    Vector lis = (Vector) nodePairs.get(masterKey);
    if (lis == null) {
      // never had an edge between these 2 before
      Vector v = new Vector();
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
