package viskit;

import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModelEvent;
import viskit.model.*;
import viskit.jgraph.vGraphAssemblyModel;
import viskit.jgraph.vGraphAssemblyComponent;
import viskit.images.AdapterIcon;
import viskit.images.SimEventListenerIcon;
import viskit.images.PropChangeListenerIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

import actions.ActionIntrospector;
import actions.ActionUtilities;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 10, 2004
 * Time: 2:07:37 PM
 */

public class AssemblyViewFrame extends mvcAbstractJFrameView implements ViskitAssemblyView, DragStartListener
{
  private ViskitAssemblyModel model;
  private ViskitAssemblyController controller;
  private JSplitPane jsp;
  private Color background = new Color(0xFB,0xFB,0xE5);

  // Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc.
  public static final int SELECT_MODE = 0;
  public static final int ADAPTER_MODE = 1;
  public static final int SIMEVLIS_MODE = 2;
  public static final int PCL_MODE = 3;

   private JMenuBar menuBar;
   private JMenu fileMenu, editMenu;

  private String filename;

  /**
   * Toolbar for dropping icons, connecting, etc.
   */
  private JToolBar toolBar;

  /**
   * Button group that holds the mode buttons.
   */
  private ButtonGroup modeButtonGroup;

  private JToggleButton selectMode;
  private JToggleButton adapterMode, simEventListenerMode, propChangeListenerMode;
  private JButton zoomIn, zoomOut;

  private JPanel canvasPanel;
  private LegosTree lTree, pclTree;
  
  private Help help;

  //private JTextField vcrStopTime;

  public AssemblyViewFrame(AssemblyModel model, AssemblyController controller)
  {
    super("Viskit -- Simkit Assembly Editor");
    initMVC(model,controller);   // set up mvc linkages
    initUI();            // build widgets

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(((d.width - 800) / 2)+30, ((d.height - 600) / 2)+30);
    setSize(800, 600);

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        ((AssemblyController)getController()).quit();
      }
    });
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
    //buildVCRToolbar();

    // Set up a top level pane that will be the content pane. This
    // has a border layout, and contains the toolbar on the top and
    // the main splitpane underneath.

    // top level panel
    JPanel top = new JPanel();
    top.setLayout(new BorderLayout());
    //top.add(toolBar, BorderLayout.NORTH);

    JComponent canvas = buildCanvas();
    JComponent trees = buildTreePanels();
    trees.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    JScrollPane leftsp = new JScrollPane(trees);
    leftsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    //leftsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,trees,new JScrollPane(canvasPanel)); //canvas));
    jsp.setOneTouchExpandable(true);
    trees.setMinimumSize(new Dimension(20,20));
    canvas.setMinimumSize(new Dimension(20,20));
    //jsp.setDividerLocation(0.5d);
    top.add(jsp,BorderLayout.CENTER);
    // uncomment following to put the vcr toolbar back in place.
    // It's now in ExternalAssemblyRunner
    //top.add(vcrToolBar,BorderLayout.SOUTH);
    top.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    getContentPane().add(top);
  }



  private void buildMenus()
  {
    ViskitAssemblyController controller = (ViskitAssemblyController)getController();
    int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    // Set up file menu
    fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    fileMenu.add(buildMenuItem(controller,"newAssembly",    "New Assembly", new Integer(KeyEvent.VK_N),
                                                               KeyStroke.getKeyStroke(KeyEvent.VK_N,accelMod)));
    fileMenu.add(buildMenuItem(controller,"open",             "Open", new Integer(KeyEvent.VK_O),
                                                               KeyStroke.getKeyStroke(KeyEvent.VK_O,accelMod)));
    fileMenu.add(buildMenuItem(controller,"save",             "Save", new Integer(KeyEvent.VK_S),
                                                               KeyStroke.getKeyStroke(KeyEvent.VK_S,accelMod)));
    fileMenu.add(buildMenuItem(controller,"saveAs",           "Save as...", new Integer(KeyEvent.VK_A),null));
    fileMenu.addSeparator();
    fileMenu.add(buildMenuItem(controller,"generateJavaSource","Generate Java Source",new Integer(KeyEvent.VK_G),null));
    fileMenu.add(buildMenuItem(controller,"runAssembly","Run Assembly",new Integer(KeyEvent.VK_R),null));
    //fileMenu.add(buildMenuItem(controller,"compileJavaClass","Compile Java Class",new Integer(KeyEvent.VK_M),null));
    fileMenu.add(buildMenuItem(controller,"runEventGraphEditor", "Event Graph Editor", null,null));
    fileMenu.addSeparator();
    fileMenu.add(buildMenuItem(controller,"quit",             "Exit",new Integer(KeyEvent.VK_X),null));

    // Set up edit menu
    editMenu = new JMenu("Edit");
    editMenu.setMnemonic(KeyEvent.VK_E);
    // the next three are disabled until something is selected
    editMenu.add(buildMenuItem(controller,"cut",  "Cut",  new Integer(KeyEvent.VK_T),
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_X,accelMod)));
    editMenu.add(buildMenuItem(controller,"copy", "Copy", new Integer(KeyEvent.VK_C),
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_C,accelMod)));
    editMenu.add(buildMenuItem(controller,"paste","Paste",new Integer(KeyEvent.VK_P),
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_V,accelMod)));

    // These 3 start off being disabled, until something is selected
    ActionIntrospector.getAction(controller,"cut").setEnabled(false);
    ActionIntrospector.getAction(controller,"copy").setEnabled(false);
    ActionIntrospector.getAction(controller,"paste").setEnabled(false);

    editMenu.addSeparator();
    editMenu.add(buildMenuItem(controller,"editGraphMetaData","Edit Assembly Properties...",null,null));

    // Create a new menu bar and add the menus we created above to it
    menuBar = new JMenuBar();
    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    //menuBar.add(simulationMenu);
    
    help = new Help(this);
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    
    helpMenu.add( buildMenuItem(help, "aboutAssemblyEditor", "About...", null, null ) );
    helpMenu.add( buildMenuItem(help, "help", "Help...", null, null ) );
    menuBar.add(helpMenu);
    
    this.setJMenuBar(menuBar);
  }

  // Use the actions package
  private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel)
  {
    Action a = ActionIntrospector.getAction(source,method);
    Map map = new HashMap();
    if(mn != null)
      map.put(Action.MNEMONIC_KEY,mn);
    if(accel != null)
      map.put(Action.ACCELERATOR_KEY,accel);
    if(name != null)
      map.put(Action.NAME,name);
    if(!map.isEmpty())
      ActionUtilities.decorateAction(a,map);

    return ActionUtilities.createMenuItem(a);
  }

  /**
   * Returns the current mode--select, add, arc, cancelArc
   */
  public int getCurrentMode()
  {
    // Use the button's selected status to figure out what mode
    // we are in.

    if (selectMode.isSelected() == true) return SELECT_MODE;
    if (adapterMode.isSelected() == true) return ADAPTER_MODE;
    if (simEventListenerMode.isSelected() == true) return SIMEVLIS_MODE;
    if (propChangeListenerMode.isSelected() == true) return PCL_MODE;
    //assert false : "getCurrentMode()";
    System.err.println("assert false : \"getCurrentMode()\"");
    return 0;
  }


/*
  private JPanel vcrToolBar;
  private JButton vcrStop, vcrPause, vcrPlay, vcrStep;
  private JCheckBox  vcrVerbose;
  private void buildVCRToolbar()
  {
    vcrToolBar = new JPanel();
    vcrToolBar.setLayout(new BoxLayout(vcrToolBar,BoxLayout.X_AXIS));
    vcrToolBar.add(Box.createHorizontalGlue());

    vcrStop = makeButton(null, "viskit/images/Stop24.gif", "Stop the simulation run");
    vcrStop.setEnabled(false);
    vcrToolBar.add(vcrStop);
    vcrPlay = makeButton(null, "viskit/images/Play24.gif", "Begin or resume the simulation run");
    vcrToolBar.add(vcrPlay);
    vcrPause = makeButton(null, "viskit/images/Pause24.gif", "Pause the simulation run");
    vcrToolBar.add(vcrPause);
    vcrPause.setEnabled(false);
    vcrStep = makeButton(null, "viskit/images/StepForward24.gif", "Step the simulation");
    vcrToolBar.add(vcrStep);

    vcrToolBar.add(Box.createHorizontalStrut(20));

    JLabel vcrSimTimeLab = new JLabel("Sim. time:");
    JTextField vcrSimTime = new JTextField(10);
    vcrSimTime.setEditable(false);
    clampSize(vcrSimTime);

    vcrToolBar.add(vcrSimTimeLab);
    vcrToolBar.add(vcrSimTime);
    vcrToolBar.add(Box.createHorizontalStrut(10));

    JLabel vcrStopTimeLabel = new JLabel("Stop time:");
    vcrStopTime = new JTextField(10);
    clampSize(vcrStopTime);

    vcrToolBar.add(vcrStopTimeLabel);
    vcrToolBar.add(vcrStopTime);
    vcrToolBar.add(Box.createHorizontalStrut(10));

    vcrVerbose = new JCheckBox("verbose output",false);
    vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
    vcrToolBar.add(vcrVerbose);

    vcrToolBar.add(Box.createHorizontalGlue());
    vcrToolBar.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    
    ViskitAssemblyController controller = (ViskitAssemblyController)getController();

    vcrStop.addActionListener(ActionIntrospector.getAction(controller,"vcrStop"));
    vcrPause.addActionListener(ActionIntrospector.getAction(controller,"vcrRewind"));
    vcrPlay.addActionListener(ActionIntrospector.getAction(controller,"vcrPlay"));
    vcrStep.addActionListener(ActionIntrospector.getAction(controller,"vcrStep"));
  }
*/
  private void buildToolbar()
  {
    modeButtonGroup = new ButtonGroup();
    toolBar = new JToolBar();

    // Buttons for what mode we are in

    selectMode             = makeJTButton(null, "viskit/images/selectNode.png",
                                       "Select items on the graph");
    Border defBor = selectMode.getBorder();
    selectMode.setBorder(BorderFactory.createCompoundBorder(
        defBor,BorderFactory.createLineBorder(Color.lightGray,2)));

    //adapterMode          = makeJTButton(null, "viskit/images/adapter.png",
    adapterMode            = makeJTButton(null,new AdapterIcon(24,24),
                                       "Connect assemblies with adapter pattern");
    defBor = adapterMode.getBorder();
    adapterMode.setBorder(BorderFactory.createCompoundBorder(
        defBor,BorderFactory.createLineBorder(new Color(0xce,0xce,0xff),2)));

    //simEventListenerMode = makeJTButton(null, "viskit/images/bridge.png",
    simEventListenerMode   = makeJTButton(null,new SimEventListenerIcon(24,24),
                                       "Connect assemblies through a SimEvent listener pattern");
    defBor = simEventListenerMode.getBorder();
    simEventListenerMode.setBorder(BorderFactory.createCompoundBorder(
        defBor,BorderFactory.createLineBorder(new Color(0xce,0xce,0xff),2)));
    
    //propChangeListenerMode = makeJTButton(null, "viskit/images/bridge.png",
    propChangeListenerMode = makeJTButton(null,new PropChangeListenerIcon(24,24),
                                       "Connect a property change listener to a SimEntity");
    defBor = propChangeListenerMode.getBorder();
    propChangeListenerMode.setBorder(BorderFactory.createCompoundBorder(
        defBor,BorderFactory.createLineBorder(new Color(0xff,0xc8,0xc8),2)));

    zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                                        "Zoom in on the graph");

    zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                                        "Zoom out on the graph");

    modeButtonGroup.add(selectMode);
    modeButtonGroup.add(adapterMode);
    modeButtonGroup.add(simEventListenerMode);
    modeButtonGroup.add(propChangeListenerMode);

    // Make selection mode the default mode
    selectMode.setSelected(true);

    toolBar.add(new JLabel("Mode: "));

    toolBar.add(selectMode);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(adapterMode);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(simEventListenerMode);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(propChangeListenerMode);

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

    class PortsVisibleListener implements ActionListener
    {
      private boolean tOrF;
      PortsVisibleListener(boolean tOrF)
      {
        this.tOrF = tOrF;
      }
      public void actionPerformed(ActionEvent e)
      {
        graphPane.setPortsVisible(tOrF);
      }

    };
    PortsVisibleListener portsOn = new PortsVisibleListener(true);
    PortsVisibleListener portsOff = new PortsVisibleListener(false);
    selectMode.addActionListener(portsOff);
    adapterMode.addActionListener(portsOn);
    simEventListenerMode.addActionListener(portsOn);
    propChangeListenerMode.addActionListener(portsOn);
  }

  private JToggleButton makeJTButton(Action a, String icPath, String tt)
  {
    JToggleButton jtb;
    if(a != null)jtb = new JToggleButton(a);
    else jtb = new JToggleButton();
    return (JToggleButton)buttonCommon(jtb,icPath,tt);
  }
  private JToggleButton makeJTButton(Action a, Icon ic, String tt)
  {
    JToggleButton jtb;
    if(a != null)jtb = new JToggleButton(a);
    else jtb = new JToggleButton();
    jtb.setIcon(ic);
    return (JToggleButton)buttonCommon2(jtb,tt);
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
    return buttonCommon2(b,tt);
  }
  private AbstractButton buttonCommon2(AbstractButton b, String tt)
  {
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
      //DropTarget dt = graphPane.getDropTarget();
      //System.out.println("blub");
      graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
    }
    catch(Exception e) {
      //assert false : "Drop target init. error";
      System.err.println("assert false : \"Drop target init. error\"");
    }

    canvasPanel = new JPanel();
    canvasPanel.setLayout(new BorderLayout());
    canvasPanel.add(graphPane,BorderLayout.CENTER);
    canvasPanel.add(toolBar,BorderLayout.NORTH);
    return graphPane;
  }
  private JSplitPane panJsp;

  private JComponent buildTreePanels()
  {
    lTree = new LegosTree("simkit.BasicSimEntity", "viskit/images/assembly.png", this, (AssemblyController)getController());
    LegosPanel lPan = new LegosPanel(lTree);

    //PropChangeListenersList pcList = new PropChangeListenersList(this);
    pclTree = new LegosTree("java.beans.PropertyChangeListener", new PropChangListIcon(20, 20), this, (AssemblyController)getController());
    PropChangeListenersPanel pcPan = new PropChangeListenersPanel(pclTree); //pcList);

    lTree.setBackground(background);
    pclTree.setBackground(background);

    panJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lPan, pcPan);

    panJsp.setBorder(null);
    panJsp.setOneTouchExpandable(true);
    pcPan.setMinimumSize(new Dimension(20, 80));
    lPan.setMinimumSize(new Dimension(20, 80));

    lTree.setDragEnabled(true);
    pclTree.setDragEnabled(true);
    //  lTree.setTransferHandler(new JTreeToJgraphTransferHandler());
    // lTree.addMouseListener( new DragMouseAdapter());
    return panJsp;
  }
  
  public void genericErrorReport(String title, String msg)
  //-----------------------------------------------------
  {
    JOptionPane.showMessageDialog(this,msg,title,JOptionPane.ERROR_MESSAGE);
  }

  Transferable dragged;
  public void startingDrag(Transferable trans)
  {
    dragged = trans;
  }

  class vDropTargetAdapter extends DropTargetAdapter
  {
/*
    public void dragEnter(DropTargetDragEvent dtde)
    {
    }

    public void dragExit(DropTargetEvent dte)
    {
    }

    public void dragOver(DropTargetDragEvent dtde)
    {
    }

    public void dropActionChanged(DropTargetDragEvent dtde)
    {
    }
*/

    public void drop(DropTargetDropEvent dtde)
    {
      if(dragged != null) {
        try {
          Point p = dtde.getLocation();

          String s = dragged.getTransferData(DataFlavor.stringFlavor).toString();
          String[] sa = s.split("\t");

          // Check for XML-based node

          try {
            FileBasedAssyNode xn = FileBasedAssyNode.fromString(sa[1]);
            if(sa[0].equals("simkit.BasicSimEntity"))
              ((ViskitAssemblyController)getController()).newFileBasedEventGraphNode(xn,p);
            else if(sa[0].equals("java.beans.PropertyChangeListener"))
              ((ViskitAssemblyController)getController()).newFileBasedPropChangeListenerNode(xn,p);
          }
          catch (FileBasedAssyNode.exception exception) {
            // Else class-based node
            if(sa[0].equals("simkit.BasicSimEntity")) {
              ((ViskitAssemblyController)getController()).newEventGraphNode(sa[1],p);
            }
            else if(sa[0].equals("java.beans.PropertyChangeListener")) {
              ((ViskitAssemblyController)getController()).newPropChangeListenerNode(sa[1],p);
            }
          }

          dragged = null;
          return;
        }
        catch (UnsupportedFlavorException e) {
          e.printStackTrace();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
        catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }
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

  /**
   * This is where the "master" model (simkit.viskit.model.Model) updates the view.
   * Not so much to do in this editor as in EventGraphViewFrame
   * @param event
   */
  public void modelChanged(mvcModelEvent event)
  //-------------------------------------------
  {

    switch(event.getID())
    {
      default:
        this.graphPane.viskitModelChanged((ModelEvent)event);
    }
  }

  // permit user to edit existing entities
  public boolean doEditEvGraphNode(EvGraphNode evNode)
  {
    return EvGraphNodeInspectorDialog.showDialog(this,this,evNode);
  }

  public boolean doEditPclNode(PropChangeListenerNode pclNode)
  {
    return PclNodeInspectorDialog.showDialog(this,this,pclNode); // blocks
  }

  public boolean doEditPclEdge(PropChangeEdge pclEdge)
  {
    return PclEdgeInspectorDialog.showDialog(this,this,pclEdge);
  }

  public boolean doEditAdapterEdge(AdapterEdge aEdge)
  {
    return AdapterConnectionInspectorDialog.showDialog(this,this,aEdge);
  }

  public boolean doEditSimEvListEdge(SimEvListenerEdge seEdge)
  {
    return SimEventListenerConnectionInspectorDialog.showDialog(this,this,seEdge);
  }

  public void fileName(String s)    // informative, tells view what we're working on
  {
    this.filename = s;
    this.setTitle("Viskit Assembly: "+s);
  }

/*
  public void setStopTime(String s)
  {
    vcrStopTime.setText(s);
  }

  public void setVerbose(boolean v)
  {
    vcrVerbose.setSelected(v);
  }

  public String getStopTime()
  {
    return vcrStopTime.getText().trim();
  }

  public boolean getVerbose()
  {
    return vcrVerbose.isSelected();
  }
*/

  public int genericAsk(String title, String msg)
  //---------------------------------------------
  {
    return JOptionPane.showConfirmDialog(this,msg,title,JOptionPane.YES_NO_CANCEL_OPTION);
  }

  public String promptForStringOrCancel(String title, String message, String initval)
  //---------------------------------------------------------------------------------
  {
    return (String)JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                                               null, null, initval);
  }

  // ViskitView-required methods:
  private JFileChooser jfc;
  public File openFileAsk()
  //-----------------------
  {
    if (jfc == null)
      jfc = new JFileChooser(System.getProperty("user.dir"));

    int retv = jfc.showOpenDialog(this);
    if (retv == JFileChooser.APPROVE_OPTION)
      return jfc.getSelectedFile();
    return null;
  }

  public File saveFileAsk(String suggNameNoType)
  //-----------------------
  {
    if(jfc == null)
      jfc = new JFileChooser(System.getProperty("user.dir"));
    jfc.setSelectedFile(new File(suggNameNoType+".xml"));
    int retv = jfc.showSaveDialog(this);
    if(retv == JFileChooser.APPROVE_OPTION)
      return jfc.getSelectedFile();
    return null;
  }

    /**
   * Called by the controller after source has been generated.  Show to the user and provide him with the option
   * to save.
   * @param s Java source
   */
  public void showAndSaveSource(String s)
  {
    JFrame f = new SourceWindow(this,s);
    f.setTitle("Generated source from "+filename);
    f.setVisible(true);
  }

  void clampHeight(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
    comp.setMinimumSize(new Dimension(Integer.MAX_VALUE,d.height));
  }
  void clampSize(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(d);
    comp.setMinimumSize(d);
  }

}

interface DragStartListener
{
  public void startingDrag(Transferable trans);
}