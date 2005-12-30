package viskit;

import actions.ActionIntrospector;
import actions.ActionUtilities;
import viskit.images.CanArcIcon;
import viskit.images.EventNodeIcon;
import viskit.images.SchedArcIcon;
import viskit.jgraph.vGraphModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModel;
import viskit.mvc.mvcModelEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 12:52:59 PM
 */

/**
 * Main "view" of the Viskit app.  This class controls a 3-paneled JFrame showing a jgraph on the left and state
 * variables and sim parameters panels on the right, with menus and a toolbar.  To fully implement application-level MVC,
 * events like the
 * dragging and dropping of a node on the screen are first recognized in this class, but the GUI is not yet changed.
 * Instead, this class (the View) messages the controller class (Controller -- by means of the ViskitController i/f).
 * The controller then informs the model (Model), which then updates itself and "broadcasts" that fact.  This class is a model
 * listener, so it gets the report, then updates the GUI.  A round trip.
 *
 * 20 SEP 2005: Updated to show multiple open eventgraphs.  The controller is largely unchanged.  To understand the
 * flow, understand that 1) The tab "ChangeListener" plays a key role; 2) When the ChangeListener is hit, the controller.setModel()
 * method installs the appropriate model for the newly-selected eventgraph.
 */

public class EventGraphViewFrame extends mvcAbstractJFrameView implements ViskitView
{
  // Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc.
  public final static int SELECT_MODE = 0;
  public final static int ADD_NODE_MODE = 1;
  public final static int ARC_MODE = 2;
  public final static int CANCEL_ARC_MODE = 3;
  public final static int SELF_REF_MODE = 4;

  /**
   * Toolbar for dropping icons, connecting, etc.
   */
  private JToolBar toolBar;

  // Mode buttons on the toolbar
  private JLabel addEvent;
  private JLabel addSelfRef;

  private JToggleButton selectMode;
  private JToggleButton arcMode;
  private JToggleButton cancelArcMode;

  private Help help;
  private JTabbedPane tabbedPane;
  private JPanel eventGraphViewerContent;
  private JMenuBar myMenuBar;
  private JMenuItem quitMenuItem;
  private TitleListener titlList;
  private int titlKey;

  private final static String frameDefaultTitle = "Viskit Event Graph Editor";

  /**
   * Constructor; lays out initial GUI objects
   */
  public EventGraphViewFrame(boolean contentOnly, Controller ctrl)
  //====================================================
  {
    super("Viskit -- Simkit Event Graph Editor");
    initMVC(ctrl);   // set up mvc linkages
    initUI(contentOnly);    // build widgets

    if (!contentOnly) {
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation((d.width - 800) / 2, (d.height - 600) / 2);
      setSize(800, 600);

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter()
      {
        public void windowClosing(WindowEvent e)
        {
          ((Controller) getController()).quit();
          // if this simply returns, nothing happens
          // else, the controller will Sys.exit()
        }
      });
      ImageIcon icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitSplash2.png"));
      setIconImage(icon.getImage());
    }
  }

  public EventGraphViewFrame(Controller ctrl)
  {
    this(false, ctrl);
  }

  public JComponent getContent()
  {
    return eventGraphViewerContent;
  }

  public JMenuBar getMenus()
  {
    return myMenuBar;
  }
  
  public JMenuItem getQuitMenuItem()
  {
    return quitMenuItem;
  }
  /**
   * Returns the current mode--select, add, arc, cancelArc
   */
  public int getCurrentMode()
  {
    // Use the button's selected status to figure out what mode
    // we are in.

    if (selectMode.isSelected()) return SELECT_MODE;
    //if (addEvent.isSelected() == true) return ADD_NODE_MODE;
    if (arcMode.isSelected()) return ARC_MODE;
    if (cancelArcMode.isSelected()) return CANCEL_ARC_MODE;
    //if (selfRefMode.isSelected() == true) return SELF_REF_MODE;
    // If none of them are selected we're in serious trouble.
    //assert false : "getCurrentMode()";
    System.err.println("assert false : \"getCurrentMode()\"");
    return 0;
  }

  /**
   * Initialize the MCV connections
   */
  private void initMVC(Controller ctrl)
  {
    //setModel(mod);
    setController(ctrl);
  }

  /**
   * Initialize the user interface
   */
  private void initUI(boolean contentOnly)
  {
    // Layout menus
    setupMenus(contentOnly);

    // Layout of toolbar
    setupToolbar();

    // Set up a eventGraphViewerContent level pane that will be the content pane. This
    // has a border layout, and contains the toolbar on the eventGraphViewerContent and
    // the main splitpane underneath.

    eventGraphViewerContent = new JPanel();
    eventGraphViewerContent.setLayout(new BorderLayout());
    eventGraphViewerContent.add(toolBar, BorderLayout.NORTH);

    tabbedPane = new JTabbedPane();
    tabbedPane.addChangeListener(new TabSelectionHandler());

    eventGraphViewerContent.add(tabbedPane,BorderLayout.CENTER);
    eventGraphViewerContent.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    if(!contentOnly)                // Can't add it here if we're going to put it somewhere  else
      getContentPane().add(eventGraphViewerContent);
  }

  private VgraphComponentWrapper getCurrentVgcw()
  {
    JSplitPane jsplt = (JSplitPane)tabbedPane.getSelectedComponent();
    if(jsplt == null)
      return null;

    JScrollPane jsp = (JScrollPane)jsplt.getLeftComponent();
    return(VgraphComponentWrapper)jsp.getViewport().getComponent(0);
  }

  class TabSelectionHandler implements ChangeListener
  {
   /*
    * Tab switch: this will come in with the newly selected tab in place.
    */
    public void stateChanged(ChangeEvent e)
    {
      VgraphComponentWrapper myVgcw = getCurrentVgcw();

      if(myVgcw == null) {     // last tab has been closed
        setSelectedEventGraphName(null);
        return;
      }
      setModel((Model)myVgcw.model);                  // hold on locally
      getController().setModel((Model)myVgcw.model);  // tell controller
      adjustMenus(myVgcw.model);                      // enable/disable menu items based on new EG

      GraphMetaData gmd = myVgcw.model.getMetaData();
      if(gmd != null)
        setSelectedEventGraphName(gmd.name);
    }
  }

  private void buildStateParamSplit(VgraphComponentWrapper vgcw)
  {
    // State variables area:
    JPanel stateVariablesPanel = new JPanel();
    stateVariablesPanel.setLayout(new BoxLayout(stateVariablesPanel,BoxLayout.Y_AXIS));
    stateVariablesPanel.add(Box.createVerticalStrut(5));

     JPanel p = new JPanel();
     p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
     p.add(Box.createHorizontalGlue());
      p.add(new JLabel("State Variables"));
     p.add(Box.createHorizontalGlue());
    stateVariablesPanel.add(p);

    VariablesPanel vp = new VariablesPanel(300,5);
    stateVariablesPanel.add(vp);
    stateVariablesPanel.add(Box.createVerticalStrut(5));
    stateVariablesPanel.setMinimumSize(new Dimension(20,20));

    // Wire handlers for stateVariable adds, deletes and edits and tell it we'll be doing adds and deletes
    vp.doAddsAndDeletes(false);
    vp.addPlusListener (ActionIntrospector.getAction((ViskitController)getController(),"newStateVariable"));

    // Introspector can't handle a param to the method....?
    vp.addMinusListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        ((ViskitController)getController()).deleteStateVariable((vStateVariable)event.getSource());
      }
    });

    vp.addDoubleClickedListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        ((ViskitController)getController()).stateVariableEdit((vStateVariable)event.getSource());
      }
    });

    // Event graph parameters area
    JPanel parametersPanel = new JPanel();
    parametersPanel.setLayout(new BoxLayout(parametersPanel,BoxLayout.Y_AXIS)); //BorderLayout());
    parametersPanel.add(Box.createVerticalStrut(5));
     p = new JPanel();
     p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
     p.add(Box.createHorizontalGlue());
     p.add(new JLabel("Event graph parameters"));
     p.add(Box.createHorizontalGlue());
    parametersPanel.add(p);

    ParametersPanel pp = new ParametersPanel(300,5);
    parametersPanel.add(pp);
    parametersPanel.add(Box.createVerticalStrut(5));
    pp.setMinimumSize(new Dimension(20,20));

    // Wire handlers for parameter adds, deletes and edits and tell it we'll be doing adds and deletes
    pp.doAddsAndDeletes(false);
    pp.addPlusListener (ActionIntrospector.getAction((ViskitController)getController(),"newSimParameter"));

    // Introspector can't handle a param to the method....?
    pp.addMinusListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        ((ViskitController)getController()).deleteSimParameter((vParameter)event.getSource());
      }
    });
    pp.addDoubleClickedListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        ((ViskitController)getController()).simParameterEdit((vParameter)event.getSource());
      }
    });

    CodeBlockPanel codeblockPan = buildCodeBlockPanel();

    JSplitPane stateCblockSplt = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                 new JScrollPane(stateVariablesPanel),
                                                 buildCodeBlockComponent(codeblockPan));
    // Split pane that has parameters, state variables and code block.
    JSplitPane spltPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                    new JScrollPane(parametersPanel),
                                                    stateCblockSplt);
    spltPn.setMinimumSize(new Dimension(20,20));

    vgcw.stateParamSplitPane = spltPn;
    vgcw.paramPan = pp;
    vgcw.varPan = vp;
    vgcw.codeBlockPan = codeblockPan;
  }
  private CodeBlockPanel buildCodeBlockPanel()
  {
    CodeBlockPanel cbp = new CodeBlockPanel(this,true,"Event Graph Code Block");
    cbp.addUpdateListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        String s = (String)e.getSource();
        if(s != null)
          ((ViskitController)getController()).codeBlockEdit((String)e.getSource());
      }
    });
    return cbp;
  }

  private JComponent buildCodeBlockComponent(CodeBlockPanel cbp)
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    JLabel lab = new JLabel("Code Block");
    lab.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    p.add(lab);
    cbp.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    p.add(cbp);
    p.setBorder(new EmptyBorder(5,5,5,2));
    Dimension d = new Dimension(p.getPreferredSize());
    d.width = Integer.MAX_VALUE;
    p.setMaximumSize(d);

    return p;
  }

  int untitledCount=0;
  public void addTab(ViskitModel mod, boolean isNewEG) // When a tab is added
  {
    vGraphModel vmod = new vGraphModel();
    VgraphComponentWrapper graphPane = new VgraphComponentWrapper(vmod,this); //vGraphComponent(mod,this);
    vmod.graph = graphPane;                               // todo fix this
    graphPane.model = mod;

    buildStateParamSplit(graphPane);
    // Split pane with the canvas on the left and a split pane with state variables and parameters on the right.
    graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                       new JLabel(), //new JScrollPane(graphPane),
                                       new JLabel()); //dummy stateParameterSplit);


    // Save the existing as the first setting for the new one.
    //graphPane.drawingSplitSetting = drawingSplitPane.getDividerLocation();

    graphPane.addMouseListener(new vCursorHandler());
    try{
      graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
    }
    catch(Exception e) {
      //assert false : "Drop target init. error";
      System.err.println("assert false : \"Drop target init. error\"");
    }
    JScrollPane jsp = new JScrollPane(graphPane);
    graphPane.drawingSplitPane.setLeftComponent(jsp);
    graphPane.drawingSplitPane.setRightComponent(graphPane.stateParamSplitPane);

    tabbedPane.add("untitled"+untitledCount++,graphPane.drawingSplitPane);
    tabbedPane.setSelectedComponent(graphPane.drawingSplitPane); // bring to front

    // If a new one, the splitpane is off
    if(isNewEG)
      graphPane.drawingSplitPane.setDividerLocation(250);

    setModel((mvcModel)mod); // the view holds only one model, so it gets overwritten with each tab
    // but this call serves also to register the view with the passed model
  }

  public void delTab(ViskitModel mod) // When a tab is removed
  {
    Component[] ca = tabbedPane.getComponents();

    for(int i=0;i<ca.length;i++) {
      JSplitPane jsplt = (JSplitPane)ca[i];
      JScrollPane jsp = (JScrollPane)jsplt.getLeftComponent();
      VgraphComponentWrapper vgcw = (VgraphComponentWrapper) jsp.getViewport().getComponent(0);
      if(vgcw.model==mod){
        tabbedPane.remove(i);
        vgcw.isActive = false;
        return;
      }
    }
    System.out.println("Deleting a tab that wasn't there!");
  }

  public ViskitModel[] getOpenModels()
  {
    Component[] ca = tabbedPane.getComponents();
    ViskitModel[] vm = new ViskitModel[ca.length];
    for (int i = 0; i < vm.length; i++) {
      JSplitPane jsplt = (JSplitPane)ca[i];
      JScrollPane jsp = (JScrollPane)jsplt.getLeftComponent();
      VgraphComponentWrapper vgcw = (VgraphComponentWrapper) jsp.getViewport().getComponent(0);
      vm[i] = vgcw.model;
    }
    return vm;
  }

  public void setVisible(boolean b)
  {
    super.setVisible(b);
    // tell the help screen where we are so he can center himself
    help.mainFrameLocated(this.getBounds());
  }

  public void prepareToQuit()
  {
    String boundsKey = "app.EventGraphEditor.FrameBounds";
    Rectangle bounds = getBounds();
    ViskitConfig.instance().setVal(boundsKey+"[@h]",""+bounds.height);
    ViskitConfig.instance().setVal(boundsKey+"[@w]",""+bounds.width);
    ViskitConfig.instance().setVal(boundsKey+"[@x]",""+bounds.x);
    ViskitConfig.instance().setVal(boundsKey+"[@y]",""+bounds.y);
  }

  /**
   * run the add parameter dialog
   */
  public String addParameterDialog()
  {

    if( ParameterDialog.showDialog(VGlobals.instance().getMainAppWindow(),this,null)) {      // blocks here
      ((ViskitController)getController()).buildNewSimParameter(ParameterDialog.newName,
                                                          ParameterDialog.newType,
                                                          "new value here",
                                                          ParameterDialog.newComment);
      return ParameterDialog.newName;
    }
    return null;
  }

  public String addStateVariableDialog()
  {
    if( StateVariableDialog.showDialog(VGlobals.instance().getMainAppWindow(),this,null)) {      // blocks here
      ((ViskitController)getController()).buildNewStateVariable(StateVariableDialog.newName,
                                                          StateVariableDialog.newType,
                                                          "new value here",
                                                          StateVariableDialog.newComment);
      return StateVariableDialog.newName;
    }
    return null;
  }
  /**
   * Do menu layout work here.  These menus, and the toggle buttons which follow, make use of the "actions"
   * package, which
   */

  private void adjustMenus(ViskitModel mod)
  {
    //todo
  }

  private void setupMenus(boolean contentOnly)
  {
    ViskitController controller = (ViskitController)getController();
    int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    // Set up file menu
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    fileMenu.add(buildMenuItem(controller,"newEventGraph",    "New Event Graph", new Integer(KeyEvent.VK_N),
                                                               KeyStroke.getKeyStroke(KeyEvent.VK_N,accelMod)));
    fileMenu.add(buildMenuItem(controller,"open",             "Open", new Integer(KeyEvent.VK_O),
                                                               KeyStroke.getKeyStroke(KeyEvent.VK_O,accelMod)));
    fileMenu.add(buildMenuItem(controller,"openRecent",       "Open Recent", null, null));
    fileMenu.add(buildMenuItem(controller,"close",            "Close",null,null));
    fileMenu.add(buildMenuItem(controller,"closeAll",         "Close All",null,null));
    fileMenu.add(buildMenuItem(controller,"save",             "Save", new Integer(KeyEvent.VK_S),
                                                               KeyStroke.getKeyStroke(KeyEvent.VK_S,accelMod)));
    fileMenu.add(buildMenuItem(controller,"saveAs",           "Save as...", new Integer(KeyEvent.VK_A),null));
    fileMenu.addSeparator();
    fileMenu.add(buildMenuItem(controller,"showXML",          "View Saved XML", null,null));
    fileMenu.add(buildMenuItem(controller,"generateJavaClass","Generate Java",new Integer(KeyEvent.VK_G),null));
    fileMenu.add(buildMenuItem(controller,"captureWindow",    "Save screen image",null,null));
    if(!contentOnly) {
      fileMenu.addSeparator();
      fileMenu.add(buildMenuItem(controller,"runAssemblyEditor", "Assembly Editor", null,null));
    }
    if(contentOnly) {
      fileMenu.addSeparator();
      fileMenu.add(buildMenuItem(controller,"settings",       "Settings", null,null));
    }
    fileMenu.addSeparator();
    fileMenu.add(quitMenuItem = buildMenuItem(controller,"quit",             "Exit",new Integer(KeyEvent.VK_X),null));

    // Set up edit menu
    JMenu editMenu = new JMenu("Edit");
    editMenu.setMnemonic(KeyEvent.VK_E);
    // the next three are disabled until something is selected
    editMenu.add(buildMenuItem(controller,"cut",  "Cut",  new Integer(KeyEvent.VK_T),
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_X,accelMod)));
    editMenu.add(buildMenuItem(controller,"copy", "Copy", new Integer(KeyEvent.VK_C),
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_C,accelMod)));
    editMenu.add(buildMenuItem(controller,"paste","Paste Events",new Integer(KeyEvent.VK_P),
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_V,accelMod)));

    // These 3 start off being disabled, until something is selected
    ActionIntrospector.getAction(controller,"cut").setEnabled(false);
    ActionIntrospector.getAction(controller,"copy").setEnabled(false);
    ActionIntrospector.getAction(controller,"paste").setEnabled(false);

    editMenu.addSeparator();

    editMenu.add(buildMenuItem(controller,"newNode",         "Add Event",                   new Integer(KeyEvent.VK_E),null));
    editMenu.add(buildMenuItem(controller,"newStateVariable","Add State Variable...",       new Integer(KeyEvent.VK_S),null));
    editMenu.add(buildMenuItem(controller,"newSimParameter", "Add Simulation Parameter...", new Integer(KeyEvent.VK_M),null));
    editMenu.add(buildMenuItem(controller,"newSelfRefEdge",  "Add Self-Referential Edge...",null,null));

    // This starts off being disabled, until something is selected
    ActionIntrospector.getAction(controller,"newSelfRefEdge").setEnabled(false);

    editMenu.addSeparator();
    editMenu.add(buildMenuItem(controller,"editGraphMetaData","Edit Graph Properties...",null,null));

    // Create a new menu bar and add the menus we created above to it
    myMenuBar = new JMenuBar();
    myMenuBar.add(fileMenu);
    myMenuBar.add(editMenu);
    //menuBar.add(simulationMenu);
    
    help = new Help(this);
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);

    helpMenu.add( buildMenuItem(help,"doContents","Contents",null,null));
    helpMenu.add( buildMenuItem(help,"doSearch","Search",null,null));
    helpMenu.addSeparator();
    helpMenu.add( buildMenuItem(help,"doTutorial","Tutorial",null,null));
    helpMenu.add( buildMenuItem(help, "aboutEventGraphEditor", "About...", null, null ) );
    //helpMenu.add( buildMenuItem(help, "help", "Help...", null, null ) );
    myMenuBar.add(helpMenu);
    
    if(!contentOnly)
      setJMenuBar(myMenuBar);
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
    b.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icPath)));
    b.setToolTipText(tt);
    b.setBorder(BorderFactory.createEtchedBorder());
    b.setText(null);
    return b;
  }
  private JLabel makeJLabel(String icPath, String tt)
  {
    JLabel jlab = new JLabel(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icPath)));
    jlab.setToolTipText(tt);
    return jlab;
  }

  private void setupToolbar()
  {
    ButtonGroup modeButtonGroup = new ButtonGroup();
    toolBar = new JToolBar();

    // Buttons for what mode we are in

    addEvent = makeJLabel("viskit/images/eventNode.png",
                         "Drag onto canvas to add new events to the event graph");
    addEvent.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(),
                        BorderFactory.createEmptyBorder(4,4,4,4)));
addEvent.setIcon(new EventNodeIcon());
    addSelfRef = makeJLabel("viskit/images/selfArc.png",
                            "Drag onto an existing event node to add a self-referential edge");

    addSelfRef.setBorder(BorderFactory.createCompoundBorder(
                         BorderFactory.createEtchedBorder(),
                         BorderFactory.createEmptyBorder(4,4,4,4)));
    selectMode    = makeJTButton(null, "viskit/images/selectNode.png",
                                       "Select items on the graph");
    arcMode       = makeJTButton(null, "viskit/images/schedArc.png",
                                       "Connect nodes with scheduling arcs");
arcMode.setIcon(new SchedArcIcon());
    cancelArcMode = makeJTButton(null, "viskit/images/canArc.png",
                                       "Connect nodes with a canceling arc");
cancelArcMode.setIcon(new CanArcIcon());
//    selfRefMode   = makeJTButton(null, "viskit/images/selfArc.png",
//                                       "Add a self-referential edge to a node");

    JButton zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                                        "Zoom in on the graph");

    JButton zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                                        "Zoom out on the graph");

    modeButtonGroup.add(selectMode);
    modeButtonGroup.add(arcMode);
    modeButtonGroup.add(cancelArcMode);
    //modeButtonGroup.add(selfRefMode);

    // Make selection mode the default mode
    selectMode.setSelected(true);

    toolBar.add(new JLabel("Add: "));
    toolBar.add(addEvent);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(addSelfRef);

    toolBar.addSeparator(new Dimension(24,24));

    toolBar.add(new JLabel("Mode: "));
    toolBar.add(selectMode);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(arcMode);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(cancelArcMode);
    //toolBar.addSeparator(new Dimension(5,24));
    //toolBar.add(selfRefMode);

    toolBar.addSeparator(new Dimension(24,24));
    toolBar.add(new JLabel("Zoom: "));
    toolBar.add(zoomIn);
    toolBar.addSeparator(new Dimension(5,24));
    toolBar.add(zoomOut);

    zoomIn.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        getCurrentVgcw().setScale(getCurrentVgcw().getScale() + 0.1d);
      }
    });
    zoomOut.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        getCurrentVgcw().setScale(Math.max(getCurrentVgcw().getScale() - 0.1d, 0.1d));
      }
    });

    addEvent.setTransferHandler(new TransferHandler("text"));
    addEvent.addMouseListener(new DragMouseAdapter());
    addSelfRef.setTransferHandler(new TransferHandler("text"));
    addSelfRef.addMouseListener(new DragMouseAdapter());

    // These buttons perform operations that are internal to our view class, and therefore their operations are
    // not under control of the application controller (Controller.java).  Small, simple anonymous inner classes
    // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

    selectMode.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        getCurrentVgcw().setPortsVisible(false);
      }
    });
    arcMode.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        getCurrentVgcw().setPortsVisible(true);
      }
    });
    cancelArcMode.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        getCurrentVgcw().setPortsVisible(true);
      }
    });

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
      select = Cursor.getDefaultCursor();
      //select    = new Cursor(Cursor.MOVE_CURSOR);
      arc = new Cursor(Cursor.CROSSHAIR_CURSOR);
      Image img = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/canArcCursor.png")).getImage();

      // Check if we should size the cursor
      Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
      if (d.width != 0 && d.height != 0) {
        buildCancelCursor(img);
      }
      else
        cancel = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "CancelArcCursor");
    }

    /**
     * This is a lot of work to build a cursor.
     *
     * @param img
     */
    private void buildCancelCursor(Image img)
    {
      new Thread(new cursorBuilder(img)).start();
    }

    class cursorBuilder implements Runnable, ImageObserver
    {
      Image img;

      cursorBuilder(Image img)
      {
        this.img = img;
      }

      int infoflags;

      public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
      {
        this.infoflags = infoflags;
        return (infoflags & ImageObserver.ALLBITS) == 0;
      }

      public void run()
      {
        infoflags = 0;
        int w = img.getWidth(this);
        int h = img.getHeight(this);    // set image observer
        if (w == -1 || h == -1)
          waitForIt();
/*
        int newwid = (int) (1.3 * img.getWidth(null));
        int newhei = (int) (1.3 * img.getHeight(null));
        infoflags = 0;
        img = img.getScaledInstance(newwid, newhei, Image.SCALE_DEFAULT);
        w = img.getWidth(this);
        h = img.getHeight(this);
        if(w == -1 || h == -1)
          waitForIt();
*/
        BufferedImage bi = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();
        Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
        bi = gc.createCompatibleImage(d.width, d.height, Transparency.BITMASK);
        infoflags = 0;
        w = bi.getWidth(this);
        h = bi.getHeight(this);
        if (w == -1 || h == -1)
          waitForIt();

        Graphics g = bi.createGraphics();
        infoflags = 0;
        if (!g.drawImage(img, 0, 0, this))
          waitForIt();

        cancel = Toolkit.getDefaultToolkit().createCustomCursor(bi, new Point(0, 0), "CancelArcCursor");
        g.dispose();
      }

      private void waitForIt()
      {
        while ((infoflags & ImageObserver.ALLBITS) == 0) {
          Thread.yield();
        }
      }
    }

    public void mouseEntered(MouseEvent e)
    {
      switch (getCurrentMode()) {
        case SELECT_MODE:
        case SELF_REF_MODE:
          getCurrentVgcw().setCursor(select);
          break;
        case ARC_MODE:
          getCurrentVgcw().setCursor(arc);
          break;
        case CANCEL_ARC_MODE:
          getCurrentVgcw().setCursor(cancel);
          break;
        default:
          //assert false : "vCursorHandler";
          System.err.println("assert false : \"vCursorHandler\"");
      }
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
      if(c == EventGraphViewFrame.this.addSelfRef)
        dragger = SELF_REF_DRAG;
      else
        dragger = NODE_DRAG;

      TransferHandler handler = c.getTransferHandler();
      handler.exportAsDrag(c, e, TransferHandler.COPY);
    }
  }
  
  class vDropTargetAdapter extends DropTargetAdapter
  {
    public void drop(DropTargetDropEvent dtde)
    {
      Point p = dtde.getLocation();  // subtract the size of the label
      if(dragger == NODE_DRAG) {
        Point pp = new Point(
          p.x - addEvent.getWidth(),
          p.y - addEvent.getHeight());
        ((ViskitController)getController()).buildNewNode(pp);
      }
      else {
        // get the node in question from the graph
        Object o = getCurrentVgcw().getViskitElementAt(p);
        if(o != null && o instanceof EventNode) {
          EventNode en = (EventNode)o;
          // We're making a self-referential arc
          ((ViskitController)getController()).buildNewArc(new Object[]{en.opaqueViewObject,en.opaqueViewObject});
        }
      }
    }
  }

  // ViskitView-required methods:
  private JFileChooser jfc;
  public File openFileAsk()
  //-----------------------
  {
    if (jfc == null) {
      jfc = new JFileChooser(System.getProperty("user.dir")+
                             System.getProperty("file.separator")+"examples");
      jfc.setDialogTitle("Open Event Graph File");
    }
    int retv = jfc.showOpenDialog(this);
    if (retv == JFileChooser.APPROVE_OPTION)
      return jfc.getSelectedFile();
    return null;
  }

  private File getUniqueName(String suggName)
  {
    String appnd = "";
    String suffix = "";

    int lastDot = suggName.lastIndexOf('.');
    if(lastDot != -1) {
      suffix = suggName.substring(lastDot);
      suggName = suggName.substring(0,lastDot);
    }
    int count = -1;
    File fil = null;
    do {
      fil = new File(suggName + appnd + suffix);
      appnd = "" + ++count;
    }
    while (fil.exists());

    return fil;
  }

  public File saveFileAsk(String suggName, boolean showUniqueName)
  //-----------------------
  {
    if(jfc == null)
      jfc = new JFileChooser(System.getProperty("user.dir"));

    File fil = new File(suggName);
    if(showUniqueName)
      fil = getUniqueName(suggName);

    jfc.setSelectedFile(fil);
    int retv = jfc.showSaveDialog(this);
    if(retv == JFileChooser.APPROVE_OPTION) {
      if(jfc.getSelectedFile().exists()) {
        if (JOptionPane.YES_OPTION !=
            JOptionPane.showConfirmDialog(this, "File exists.  Overwrite?","Confirm",
                                                  JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE))
          return null;
      }
      return jfc.getSelectedFile();
    }
    return null;
  }

  public File openRecentFilesAsk(Collection lis)
  {
    String fn = RecentFilesDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, lis);
    if (fn != null) {
      File f = new File(fn);
      if (f.exists())
        return f;
      else
        JOptionPane.showMessageDialog(this, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    return null;
  }

  public void setSelectedEventGraphName(String s)
  //----------------------------
  {
    boolean nullString = !(s != null && s.length()>0);
    String ttl = nullString ? frameDefaultTitle :"Viskit Event Graph: "+s;
    setTitle(ttl);
    if(!nullString) {
      tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(),s);
    }
    if(this.titlList != null)
      titlList.setTitle(ttl,titlKey);
  }

  public boolean doEditNode(EventNode node)
  //---------------------------------------
  {
    selectMode.doClick();     // always go back into select mode
    return EventInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(),VGlobals.instance().getMainAppWindow(),node); // blocks
  }

  public boolean doEditEdge(SchedulingEdge edge)
  //--------------------------------------------
  {
    selectMode.doClick();     // always go back into select mode
    return EdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(),VGlobals.instance().getMainAppWindow(),edge); // blocks
  }

  public boolean doEditCancelEdge(CancellingEdge edge)
  //--------------------------------------------------
  {
    selectMode.doClick();     // always go back into select mode
    return EdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(),VGlobals.instance().getMainAppWindow(),edge); // blocks
  }

  public boolean doEditParameter(vParameter param)
  {
    return ParameterDialog.showDialog(VGlobals.instance().getMainAppWindow(),getCurrentVgcw(),param);    // blocks
  }
  public boolean doEditStateVariable(vStateVariable var)
  {
    return StateVariableDialog.showDialog(VGlobals.instance().getMainAppWindow(),getCurrentVgcw(),var);
  }

  public boolean doMetaGraphEdit(GraphMetaData gmd)
  //-----------------------------------------------
  {
    return EvGraphMetaDataDialog.showDialog(VGlobals.instance().getMainAppWindow(),getCurrentVgcw(),gmd);
  }

  public int genericAsk(String title, String msg)
  //---------------------------------------------
  {
    return JOptionPane.showConfirmDialog(VGlobals.instance().getMainAppWindow(),msg,title,JOptionPane.YES_NO_CANCEL_OPTION);
  }

  public void genericErrorReport(String title, String msg)
  //-----------------------------------------------------
  {
    JOptionPane.showMessageDialog(VGlobals.instance().getMainAppWindow(),msg,title,JOptionPane.ERROR_MESSAGE);
  }

  public String promptForStringOrCancel(String title, String message, String initval)
  //---------------------------------------------------------------------------------
  {
    return (String)JOptionPane.showInputDialog(VGlobals.instance().getMainAppWindow(), message, title, JOptionPane.PLAIN_MESSAGE,
                                               null, null, initval);
  }

  /**
   * This is where the "master" model (simkit.viskit.model.Model) updates the view.
   * @param event
   */
  public void modelChanged(mvcModelEvent event)
  //-------------------------------------------
  {
    VgraphComponentWrapper vgcw = getCurrentVgcw();
    ParametersPanel pp = vgcw.paramPan;
    VariablesPanel  vp = vgcw.varPan;
    switch(event.getID())
    {
      // Changes the two side panels need to know about
      case ModelEvent.SIMPARAMETERADDED:
        pp.addRow(event.getSource());
        //VGlobals.instance().setSimParmsList(((ViskitModel)this.getModel()).getSimParameters());
        break;
      case ModelEvent.SIMPARAMETERDELETED:
        pp.removeRow(event.getSource());
        //VGlobals.instance().setSimParmsList(((ViskitModel)this.getModel()).getSimParameters());
        break;
      case ModelEvent.SIMPARAMETERCHANGED:
        pp.updateRow(event.getSource());
        //VGlobals.instance().setSimParmsList(((ViskitModel)this.getModel()).getSimParameters());
        break;

      case ModelEvent.STATEVARIABLEADDED:
        vp.addRow(event.getSource());
        //VGlobals.instance().setStateVarsList(((ViskitModel)this.getModel()).getStateVariables());
        break;
      case ModelEvent.STATEVARIABLEDELETED:
        vp.removeRow(event.getSource());
        //VGlobals.instance().setStateVarsList(((ViskitModel)this.getModel()).getStateVariables());
        break;
      case ModelEvent.STATEVARIABLECHANGED:
        vp.updateRow(event.getSource());
        //VGlobals.instance().setStateVarsList(((ViskitModel)this.getModel()).getStateVariables());
        break;

      case ModelEvent.CODEBLOCKCHANGED:
        vgcw.codeBlockPan.setData((String)event.getSource());
        break;

      case ModelEvent.NEWMODEL:
        vp.setData(null);
        pp.setData(null);
        //VGlobals.instance().setStateVarsList(((ViskitModel)this.getModel()).getStateVariables());
        //VGlobals.instance().setSimParmsList(((ViskitModel)this.getModel()).getSimParameters());
        // fall through

      // Changes the graph needs to know about
      default:
        getCurrentVgcw().viskitModelChanged((ModelEvent)event);

    }
  }

  /**
   * Called by the controller after source has been generated.  Show to the user and provide him with the option
   * to save.
   * @param s Java source
   */
  public void showAndSaveSource(String s, String filename)
  {
    JFrame f = new SourceWindow(this,s);
    f.setTitle("Generated source from "+filename);
    f.setVisible(true);
  }

  public void displayXML(File f)
  {
    JComponent xt = null;
    try {
      xt = XTree.getTreeInPanel(f);
    }
    catch (Exception e) {
      JOptionPane.showMessageDialog(this,e.getMessage());
      return;
    }
    //xt.setVisibleRowCount(25);
    xt.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

    final JFrame jf = new JFrame(f.getName());

    JPanel content = new JPanel();
    jf.setContentPane(content);

    content.setLayout(new BorderLayout());
    content.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
    content.add(xt,BorderLayout.CENTER);
      JPanel buttPan = new JPanel();
      buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
      buttPan.setBorder(BorderFactory.createEmptyBorder(0,4,4,4));
      JButton closeButt = new JButton("Close");
      buttPan.add(Box.createHorizontalGlue());
      buttPan.add(closeButt);
    content.add(buttPan,BorderLayout.SOUTH);

    //jf.pack();
    jf.setSize(475,500);
    jf.setLocationRelativeTo(this);
    jf.setVisible(true);

    closeButt.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        jf.setVisible(false);
      }
    });
  }

  public void setTitleListener(TitleListener lis, int key)
  {
    titlList = lis;
    titlKey = key;

    // default
    if(titlList != null)
      titlList.setTitle(frameDefaultTitle,titlKey);
  }
}



