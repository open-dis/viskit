package viskit;

import viskit.mvc.mvcAbstractJFrameView;
import viskit.model.Model;
import viskit.model.AssemblyModel;
import viskit.model.ViskitAssemblyModel;
import viskit.jgraph.vGraphAssemblyModel;
import viskit.jgraph.vGraphAssemblyComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 10, 2004
 * Time: 2:07:37 PM
 */

public class AssemblyViewFrame extends mvcAbstractJFrameView implements ViskitAssemblyView
{
  private ViskitAssemblyModel model;
  private ViskitAssemblyController controller;
  private JSplitPane jsp;
  private Color background = new Color(0xFB,0xFB,0xE5);


  /**
   * Toolbar for dropping icons, connecting, etc.
   */
  private JToolBar toolBar;

  /**
   * Button group that holds the mode buttons.
   */
  private ButtonGroup modeButtonGroup;

  private JToggleButton selectMode;
  private JToggleButton arcMode;
  private JButton zoomIn, zoomOut;




  public AssemblyViewFrame(AssemblyModel model, AssemblyController controller)
  {
    super("Viskit -- Simkit Assembly Editor");
    initMVC(model,controller);   // set up mvc linkages
    initUI();            // build widgets

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(((d.width - 800) / 2)+30, ((d.height - 600) / 2)+30);
    setSize(800, 600);

/*   enable when finished with widget creation
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        ((AssemblyController)getController()).quit();
      }
    });
*/
  }

  /**
   * Initialize the MCV connections
   */
  private void initMVC(AssemblyModel mod, AssemblyController ctrl)
  {
    setModel(mod);
    setController(ctrl);
  }

  /**
   * Initialize the user interface
   */
  private void initUI()
  {
    Container cont = getContentPane();

    buildMenus();
    buildToolbar();

    // Set up a top level pane that will be the content pane. This
    // has a border layout, and contains the toolbar on the top and
    // the main splitpane underneath.

    // top level panel
    JPanel top = new JPanel();
    top.setLayout(new BorderLayout());
    top.add(toolBar, BorderLayout.NORTH);

    JComponent canvas = buildCanvas();
    JComponent trees = buildTreePanels();
    trees.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    JScrollPane leftsp = new JScrollPane(trees);
    leftsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    //leftsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,trees,new JScrollPane(canvas));
    jsp.setOneTouchExpandable(true);
    trees.setMinimumSize(new Dimension(20,20));
    canvas.setMinimumSize(new Dimension(20,20));
    //jsp.setDividerLocation(0.5d);
    top.add(jsp,BorderLayout.CENTER);
    getContentPane().add(top);
  }



  private void buildMenus()
  {

  }

  private void buildToolbar()
  {
    modeButtonGroup = new ButtonGroup();
    toolBar = new JToolBar();

    // Buttons for what mode we are in

    selectMode    = makeJTButton(null, "viskit/images/selectNode.png",
                                       "Select items on the graph");
    arcMode       = makeJTButton(null, "viskit/images/schedArc.png",
                                       "Connect nodes with scheduling arcs");

    zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                                        "Zoom in on the graph");

    zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                                        "Zoom out on the graph");

    modeButtonGroup.add(selectMode);
    modeButtonGroup.add(arcMode);

    // Make selection mode the default mode
    selectMode.setSelected(true);

    toolBar.add(new JLabel("Mode: "));
    toolBar.add(selectMode);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(arcMode);
    toolBar.addSeparator(new Dimension(5,24));

    toolBar.addSeparator(new Dimension(24,24));
    toolBar.add(new JLabel("Zoom: "));
    toolBar.add(zoomIn);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(zoomOut);

    zoomIn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        graphPane.setScale(graphPane.getScale() + 0.1d);
      }
    });
    zoomOut.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        graphPane.setScale(Math.max(graphPane.getScale() - 0.1d, 0.1d));
      }
    });

    // These buttons perform operations that are internal to our view class, and therefore their operations are
    // not under control of the application controller (Controller.java).  Small, simple anonymous inner classes
    // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

    selectMode.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        graphPane.setPortsVisible(false);
      }
    });
    arcMode.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        graphPane.setPortsVisible(true);
      }
    });


  }

  private JToggleButton makeJTButton(Action a, String icPath, String tt)
  {
    JToggleButton jtb;
    if(a != null)jtb = new JToggleButton(a);
    else jtb = new JToggleButton();
    return (JToggleButton)buttonCommon(jtb,icPath,tt);
  }
  private JButton makeButton(Action a, String icPath, String tt)
  {
    JButton b;
    if(a != null) b = new JButton(a);
    else b = new JButton();
    return (JButton)buttonCommon(b,icPath,tt);
  }
  private AbstractButton buttonCommon(AbstractButton b, String icPath, String tt)
  {
    b.setIcon(new ImageIcon(ClassLoader.getSystemResource(icPath)));
    b.setToolTipText(tt);
    b.setBorder(BorderFactory.createEtchedBorder());
    b.setText(null);
    return b;
  }




  private vGraphAssemblyComponent graphPane;
  private JComponent buildCanvas()
  {
    // Set up the basic panes for the layouts
    vGraphAssemblyModel mod = new vGraphAssemblyModel();
    graphPane = new vGraphAssemblyComponent(mod,this);
    mod.graph = graphPane;                               // todo fix this

    graphPane.addMouseListener(new vCursorHandler());
    try{
      graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
    }
    catch(Exception e) {
      //assert false : "Drop target init. error";
      System.err.println("assert false : \"Drop target init. error\"");
    }

    return graphPane;
  }
  private JSplitPane panJsp;

  private JComponent buildTreePanels()
  {
    LegosTree lTree= new LegosTree();
    LegosPanel lPan = new LegosPanel(lTree);

    PropChangeListenersList pcList = new PropChangeListenersList();
    PropChangeListenersPanel pcPan = new PropChangeListenersPanel(pcList);

    lTree.setBackground(background);
    pcList.setBackground(background);

      panJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,lPan,pcPan);
/*
  JScrollPane tsp = new JScrollPane(lPan,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  JScrollPane psp = new JScrollPane(pcPan,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  panJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,tsp,psp);
  tsp.setMinimumSize(new Dimension(20,20));
  psp.setMinimumSize(new Dimension(20,20));
*/

      panJsp.setBorder(null);
      panJsp.setOneTouchExpandable(true);
      pcPan.setMinimumSize(new Dimension(20,80));
      lPan.setMinimumSize(new Dimension(20,80));

    return panJsp;
  }
  private void buildToolBar()
  {

  }
  private boolean firstShown = false;

  public void setVisible(boolean b)
  {
    super.setVisible(b);
    if(firstShown == false) {
      firstShown = true;
      jsp.setDividerLocation(225);
      panJsp.setDividerLocation(0.5d);
    }
  }

  // Some private classes to implement dnd and dynamic cursor update
  class vCursorHandler extends MouseAdapter
  {
    Cursor select;
    Cursor arc;
    Cursor cancel;

    vCursorHandler()
    {
      super();
      select    = Cursor.getDefaultCursor();
      //select    = new Cursor(Cursor.MOVE_CURSOR);
      arc       = new Cursor(Cursor.CROSSHAIR_CURSOR);

      Image img = new ImageIcon(ClassLoader.getSystemResource("viskit/images/canArcCursor.png")).getImage();
      cancel    = Toolkit.getDefaultToolkit().createCustomCursor(img,new Point(0,0),"CancelArcCursor");
    }

    public void mouseEntered(MouseEvent e)
    {
/*
      switch(getCurrentMode()) {
        case SELECT_MODE:
        case SELF_REF_MODE:
          graphPane.setCursor(select);
          break;
        case ARC_MODE:
          graphPane.setCursor(arc);
          break;
        case CANCEL_ARC_MODE:
          graphPane.setCursor(cancel);
          break;
        default:
          //assert false : "vCursorHandler";
          System.err.println("assert false : \"vCursorHandler\"");
      }
*/
    }
  }


  final static int NODE_DRAG = 0;
  final static int SELF_REF_DRAG = 1;
  private int dragger;
  // Two classes to support dragging and dropping on the graph
  class DragMouseAdapter extends MouseAdapter
  {
    public void mousePressed(MouseEvent e)
    {
      JComponent c = (JComponent) e.getSource();
      //todo
/*
      if(c == EventGraphViewFrame.this.addSelfRef)
        dragger = SELF_REF_DRAG;
      else
        dragger = NODE_DRAG;
*/

      TransferHandler handler = c.getTransferHandler();
      handler.exportAsDrag(c, e, TransferHandler.COPY);
    }
  }
  class vDropTargetAdapter extends DropTargetAdapter
  {
    public void drop(DropTargetDropEvent dtde)
    {
/*
      Point p = dtde.getLocation();  // subtract the size of the label
      if(dragger == NODE_DRAG) {
        Point pp = new Point(
          // todo addEvent is the drug-from JLabel
          p.x - addEvent.getWidth(),
          p.y - addEvent.getHeight());
        ((ViskitController)getController()).newNode(pp);
      }
      else {
        // get the node in question from the graph
        Object o = graphPane.getViskitElementAt(p);
        if(o != null && o instanceof EventNode) {
          EventNode en = (EventNode)o;
          // We're making a self-referential arc
          ((ViskitController)getController()).newArc(new Object[]{en.opaqueViewObject,en.opaqueViewObject});
        }
      }
*/
    }
  }

}
