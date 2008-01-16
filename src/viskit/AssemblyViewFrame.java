package viskit;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

// Application specific imports
import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.AssemblyFileFilter;
import org.apache.log4j.Logger;
import viskit.images.AdapterIcon;
import viskit.images.PropChangeListenerIcon;
import viskit.images.SimEventListenerIcon;
import viskit.jgraph.vGraphAssemblyComponent;
import viskit.jgraph.vGraphAssemblyModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModelEvent;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 10, 2004
 * @since 2:07:37 PM
 * @version $Id: AssemblyViewFrame.java 1666 2007-12-17 05:24:41Z tdnorbra $
 */
public class AssemblyViewFrame extends mvcAbstractJFrameView implements ViskitAssemblyView, DragStartListener {
    
    /** log4j logger instance */
    static Logger log = Logger.getLogger(AssemblyViewFrame.class);
    
    /** Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc. */
    public static final int SELECT_MODE = 0;
    public static final int ADAPTER_MODE = 1;
    public static final int SIMEVLIS_MODE = 2;
    public static final int PCL_MODE = 3;
    private JSplitPane jsp;
    private Color background = new Color(0xFB, 0xFB, 0xE5);
    private String filename;

    /** Toolbar for dropping icons, connecting, etc. */
    private JToolBar toolBar;
    private JToggleButton selectMode;
    private JToggleButton adapterMode,  simEventListenerMode,  propChangeListenerMode;
    private JPanel canvasPanel;
    private LegosTree lTree,  pclTree;
    private JPanel assemblyEditorContent;
    private JMenuBar myMenuBar;
    private JMenuItem quitMenuItem;
    private JButton runButt;

    public AssemblyViewFrame(AssemblyModel model, AssemblyController controller) {
        this(false, model, controller);
    }

    public AssemblyViewFrame(boolean contentOnly, AssemblyModel model, AssemblyController controller) {
        super("Viskit -- Simkit Assembly Editor");
        initMVC(model, controller);   // set up mvc linkages
        initUI(contentOnly);            // build widgets

        if (!contentOnly) {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(((d.width - 800) / 2) + 30, ((d.height - 600) / 2) + 30);
            setSize(800, 600);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            this.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    ((AssemblyController) getController()).quit();
                }
            });

            ImageIcon icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitSplash2.png"));
            setIconImage(icon.getImage());
        }
    }

    public JComponent getContent() {
        return assemblyEditorContent;
    }

    public Component getCurrentJgraphComponent() {
        return graphPane;

    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return quitMenuItem;
    }

    /**
   * Initialize the MCV connections
   */
    private void initMVC(AssemblyModel mod, AssemblyController ctrl) {
        setModel(mod);
        setController(ctrl);
    }

    /**
   * Initialize the user interface
   */
    private void initUI(boolean contentOnly) {
        buildMenus(contentOnly);
        buildToolbar(contentOnly);
        //buildVCRToolbar();

        // Set up a assemblyEditorContent level pane that will be the content pane. This
    // has a border layout, and contains the toolbar on the assemblyEditorContent and
    // the main splitpane underneath.

        // assemblyEditorContent level panel
        assemblyEditorContent = new JPanel();
        assemblyEditorContent.setLayout(new BorderLayout());
        //assemblyEditorContent.add(toolBar, BorderLayout.NORTH);

        JComponent canvas = buildCanvas();
        JSplitPane trees = buildTreePanels();
        trees.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane leftsp = new JScrollPane(trees);
        leftsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        //leftsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, trees, new JScrollPane(canvasPanel)); //canvas));
        jsp.setOneTouchExpandable(true);
        trees.setMinimumSize(new Dimension(20, 20));
        canvas.setMinimumSize(new Dimension(20, 20));
        //jsp.setDividerLocation(0.5d);
        assemblyEditorContent.add(jsp, BorderLayout.CENTER);
        // uncomment following to put the vcr toolbar back in place.
    // It's now in ExternalAssemblyRunner
    //assemblyEditorContent.add(vcrToolBar,BorderLayout.SOUTH);
        assemblyEditorContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (!contentOnly) // Can't add it here if we're going to put it somewhere else
        {
            getContentPane().add(assemblyEditorContent);
        }

        trees.setDividerLocation(250);
    }

    private void buildMenus(boolean contentOnly) {
        ViskitAssemblyController controller = (ViskitAssemblyController) getController();
        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // Set up file menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(buildMenuItem(controller, "newAssembly", "New Assembly", new Integer(KeyEvent.VK_N),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, accelMod)));
        fileMenu.add(buildMenuItem(controller, "open", "Open", new Integer(KeyEvent.VK_O),
                KeyStroke.getKeyStroke(KeyEvent.VK_O, accelMod)));
        fileMenu.add(buildMenuItem(controller, "openRecent", "Open Recent", new Integer(KeyEvent.VK_P), null));
        
        // Bug fix: 1195
        fileMenu.add(buildMenuItem(controller, "close", "Close", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, accelMod)));
        fileMenu.add(buildMenuItem(controller, "save", "Save", new Integer(KeyEvent.VK_S),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, accelMod)));
        fileMenu.add(buildMenuItem(controller, "saveAs", "Save as...", new Integer(KeyEvent.VK_A), null));
        
        fileMenu.addSeparator();
        fileMenu.add(buildMenuItem(controller, "showXML", "View Saved XML", new Integer(KeyEvent.VK_X), null));
        fileMenu.add(buildMenuItem(controller, "generateJavaSource", "Generate Java Source", new Integer(KeyEvent.VK_J), null));
        fileMenu.add(buildMenuItem(controller, "captureWindow", "Save Screen Image", new Integer(KeyEvent.VK_I),
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod)));
        fileMenu.add(buildMenuItem(controller, "runAssembly", "Run Assembly", new Integer(KeyEvent.VK_R),
                KeyStroke.getKeyStroke(KeyEvent.VK_R, accelMod)));

        if (contentOnly) {
            fileMenu.add(buildMenuItem(controller, "export2grid", "Export to Cluster Format", new Integer(KeyEvent.VK_C), null));
        }
        if (!contentOnly) {
            fileMenu.addSeparator();
            fileMenu.add(buildMenuItem(controller, "runEventGraphEditor", "Event Graph Editor", null, null));
        }
        if (contentOnly) {
            fileMenu.addSeparator();
            fileMenu.add(buildMenuItem(controller, "settings", "Settings", null, null));
        }
        fileMenu.addSeparator();
        fileMenu.add(quitMenuItem = buildMenuItem(controller, "quit", "Exit", new Integer(KeyEvent.VK_X), KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK)));

        // Set up edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        // the next three are disabled until something is selected
        editMenu.add(buildMenuItem(controller, "cut", "Cut", new Integer(KeyEvent.VK_T),
                KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));
        editMenu.add(buildMenuItem(controller, "copy", "Copy", new Integer(KeyEvent.VK_C),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));
        editMenu.add(buildMenuItem(controller, "paste", "Paste", new Integer(KeyEvent.VK_P),
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        editMenu.add(buildMenuItem(controller, "edit", "Load an EG Editor Tab", new Integer(KeyEvent.VK_E),
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));

        // These 4 start off being disabled, until something is selected
        ActionIntrospector.getAction(controller, "cut").setEnabled(false);
        ActionIntrospector.getAction(controller, "copy").setEnabled(false);
        ActionIntrospector.getAction(controller, "paste").setEnabled(false);
        ActionIntrospector.getAction(controller, "edit").setEnabled(false);

        editMenu.addSeparator();

        editMenu.add(buildMenuItem(controller, "newEventGraphNode", "Add Event Graph...",
                new Integer(KeyEvent.VK_G), null));
        editMenu.add(buildMenuItem(controller, "newPropChangeListenerNode", "Add Property Change Listener...",
                new Integer(KeyEvent.VK_L), null));

        editMenu.addSeparator();

        editMenu.add(buildMenuItem(controller, "editGraphMetaData", "Edit Properties...", new Integer(KeyEvent.VK_E),
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));

        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(fileMenu);
        myMenuBar.add(editMenu);
        //menuBar.add(simulationMenu);

        Help help = new Help(this);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        helpMenu.add(buildMenuItem(help, "doContents", "Contents", new Integer(KeyEvent.VK_C), null));
        helpMenu.add(buildMenuItem(help, "doSearch", "Search", new Integer(KeyEvent.VK_S), null));
        helpMenu.addSeparator();
        helpMenu.add(buildMenuItem(help, "doTutorial", "Tutorial", new Integer(KeyEvent.VK_T), null));
        helpMenu.add(buildMenuItem(help, "aboutEventGraphEditor", "About...", new Integer(KeyEvent.VK_A), null));
        //helpMenu.add( buildMenuItem(help, "help", "Help...", null, null ) );
        myMenuBar.add(helpMenu);

        if (!contentOnly) {
            setJMenuBar(myMenuBar);
        }
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel) {
        Action a = ActionIntrospector.getAction(source, method);
        
        Map<String, Object> map = new HashMap<String, Object>();
        if (mn != null) {
            map.put(Action.MNEMONIC_KEY, mn);
        }
        if (accel != null) {
            map.put(Action.ACCELERATOR_KEY, accel);
        }
        if (name != null) {
            map.put(Action.NAME, name);
        }
        if (!map.isEmpty()) {
            ActionUtilities.decorateAction(a, map);
        }

        return ActionUtilities.createMenuItem(a);
    }

    /**
     * Returns the current mode--select, add, arc, cancelArc
     */
    public int getCurrentMode() {
        // Use the button's selected status to figure out what mode
        // we are in.

        if (selectMode.isSelected()) {
            return SELECT_MODE;
        }
        if (adapterMode.isSelected()) {
            return ADAPTER_MODE;
        }
        if (simEventListenerMode.isSelected()) {
            return SIMEVLIS_MODE;
        }
        if (propChangeListenerMode.isSelected()) {
            return PCL_MODE;
        }
        //assert false : "getCurrentMode()";
        System.err.println("assert false : \"getCurrentMode()\"");
        return 0;
    }

    private void buildToolbar(boolean contentOnly) {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        toolBar = new JToolBar();

        // Buttons for what mode we are in

        selectMode = makeJTButton(null, "viskit/images/selectNode.png",
                "Select items on the graph");
        Border defBor = selectMode.getBorder();
        selectMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(Color.lightGray, 2)));

        //adapterMode          = makeJTButton(null, "viskit/images/adapter.png",
        adapterMode = makeJTButton(null, new AdapterIcon(24, 24),
                "Connect assemblies with adapter pattern");
        defBor = adapterMode.getBorder();
        adapterMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        //simEventListenerMode = makeJTButton(null, "viskit/images/bridge.png",
        simEventListenerMode = makeJTButton(null, new SimEventListenerIcon(24, 24),
                "Connect assemblies through a SimEvent listener pattern");
        defBor = simEventListenerMode.getBorder();
        simEventListenerMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        //propChangeListenerMode = makeJTButton(null, "viskit/images/bridge.png",
        propChangeListenerMode = makeJTButton(null, new PropChangeListenerIcon(24, 24),
                "Connect a property change listener to a SimEntity");
        defBor = propChangeListenerMode.getBorder();
        propChangeListenerMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 2)));

        JButton zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in on the graph");

        JButton zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out on the graph");

        Action runAction = ActionIntrospector.getAction(getController(), "runAssembly");
        runButt = makeButton(runAction, "viskit/images/Play24.gif",
                "Run the assembly");
        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(adapterMode);
        modeButtonGroup.add(simEventListenerMode);
        modeButtonGroup.add(propChangeListenerMode);

        // Make selection mode the default mode
        selectMode.setSelected(true);

        toolBar.add(new JLabel("Mode: "));

        toolBar.add(selectMode);
        toolBar.addSeparator(new Dimension(5, 24));
        toolBar.add(adapterMode);
        toolBar.addSeparator(new Dimension(5, 24));
        toolBar.add(simEventListenerMode);
        toolBar.addSeparator(new Dimension(5, 24));
        toolBar.add(propChangeListenerMode);

        toolBar.addSeparator(new Dimension(24, 24));
        toolBar.add(new JLabel("Zoom: "));
        toolBar.add(zoomIn);
        toolBar.addSeparator(new Dimension(5, 24));
        toolBar.add(zoomOut);
        // if(!contentOnly) {
        toolBar.addSeparator(new Dimension(24, 24));
        toolBar.add(new JLabel("  Initialize assembly runner: "));
        toolBar.add(runButt);
        //  }
        zoomIn.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        graphPane.setScale(graphPane.getScale() + 0.1d);
                    }
                });
        zoomOut.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        graphPane.setScale(Math.max(graphPane.getScale() - 0.1d, 0.1d));
                    }
                });

        // These buttons perform operations that are internal to our view class, and therefore their operations are
    // not under control of the application controller (Controller.java).  Small, simple anonymous inner classes
    // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        class PortsVisibleListener implements ActionListener {

            private boolean tOrF;

            PortsVisibleListener(boolean tOrF) {
                this.tOrF = tOrF;
            }

            public void actionPerformed(ActionEvent e) {
                graphPane.setPortsVisible(tOrF);
            }
        }

        PortsVisibleListener portsOn = new PortsVisibleListener(true);
        PortsVisibleListener portsOff = new PortsVisibleListener(false);
        selectMode.addActionListener(portsOff);
        adapterMode.addActionListener(portsOn);
        simEventListenerMode.addActionListener(portsOn);
        propChangeListenerMode.addActionListener(portsOn);
    }

    private JToggleButton makeJTButton(Action a, String icPath, String tt) {
        JToggleButton jtb;
        if (a != null) {
            jtb = new JToggleButton(a);
        } else {
            jtb = new JToggleButton();
        }
        return (JToggleButton) buttonCommon(jtb, icPath, tt);
    }

    private JToggleButton makeJTButton(Action a, Icon ic, String tt) {
        JToggleButton jtb;
        if (a != null) {
            jtb = new JToggleButton(a);
        } else {
            jtb = new JToggleButton();
        }
        jtb.setIcon(ic);
        return (JToggleButton) buttonCommon2(jtb, tt);
    }

    private JButton makeButton(Action a, String icPath, String tt) {
        JButton b;
        if (a != null) {
            b = new JButton(a);
        } else {
            b = new JButton();
        }
        return (JButton) buttonCommon(b, icPath, tt);
    }

    private AbstractButton buttonCommon(AbstractButton b, String icPath, String tt) {
        b.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icPath)));
        return buttonCommon2(b, tt);
    }

    private AbstractButton buttonCommon2(AbstractButton b, String tt) {
        b.setToolTipText(tt);
        b.setBorder(BorderFactory.createEtchedBorder());
        b.setText(null);
        return b;
    }
    private vGraphAssemblyComponent graphPane;

    private JComponent buildCanvas() {
        // Set up the basic panes for the layouts
        vGraphAssemblyModel mod = new vGraphAssemblyModel();
        graphPane = new vGraphAssemblyComponent(mod, this);
        mod.graph = graphPane;                               // todo fix this

        graphPane.addMouseListener(new vCursorHandler());
        try {
            //DropTarget dt = graphPane.getDropTarget();
      //System.out.println("blub");
            graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
        } catch (Exception e) {
            //assert false : "Drop target init. error";
            System.err.println("assert false : \"Drop target init. error\"");
        }

        canvasPanel = new JPanel();
        canvasPanel.setLayout(new BorderLayout());
        canvasPanel.add(graphPane, BorderLayout.CENTER);
        canvasPanel.add(toolBar, BorderLayout.NORTH);
        return graphPane;
    }
    private JSplitPane panJsp;

    void rebuildTreePanels() {
        lTree.clear();
        JSplitPane treeSplit = buildTreePanels();
        jsp.setTopComponent(treeSplit);
        treeSplit.setDividerLocation(250);
        lTree.repaint();
    }

    private JSplitPane buildTreePanels() {

        lTree = new LegosTree("simkit.BasicSimEntity", "viskit/images/assembly.png",
                this, "Drag an Event Graph onto the canvas to add it to the assembly");

        // Decouple diskit from vanilla Viskit operation
        File diskitJar = new File("lib/ext/diskit.jar");
        for (String path : SettingsDialog.getExtraClassPath()) { // tbd same for pcls
            if (path.endsWith(".jar")) {
                if (diskitJar.getName().contains(new File(path).getName())) {
                    continue;
                } else {
                    lTree.addContentRoot(new File(path));
                }
            } else {
                lTree.addContentRoot(new File(path), true);
            }
        }

        LegosPanel lPan = new LegosPanel(lTree);

        pclTree = new LegosTree("java.beans.PropertyChangeListener", new PropChangListIcon(20, 20),
                this, "Drag a PropertyChangeListener onto the canvas to add it to the assembly");

        // todo get from project
        pclTree.addContentRoot(new File("lib/simkit.jar"));
        
        // If we built diskit.jar, then include it
        if (diskitJar.exists()) {
            pclTree.addContentRoot(diskitJar);
        }
        diskitJar = null;

        PropChangeListenersPanel pcPan = new PropChangeListenersPanel(pclTree);

        lTree.setBackground(background);
        pclTree.setBackground(background);

        panJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lPan, pcPan);
        panJsp.setBorder(null);
        panJsp.setOneTouchExpandable(true);

        pcPan.setMinimumSize(new Dimension(20, 80));
        lPan.setMinimumSize(new Dimension(20, 80));
        lPan.setPreferredSize(new Dimension(20, 240)); // give it some height for the initial split

        lTree.setDragEnabled(true);
        pclTree.setDragEnabled(true);
        return panJsp;
    }

    public void genericErrorReport(String title, String msg) //-----------------------------------------------------
    {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }
    Transferable dragged;

    public void startingDrag(Transferable trans) {
        dragged = trans;
    }

    class vDropTargetAdapter extends DropTargetAdapter {

        public void drop(DropTargetDropEvent dtde) {
            if (dragged != null) {
                try {
                    Point p = dtde.getLocation();

                    String s = dragged.getTransferData(DataFlavor.stringFlavor).toString();
                    String[] sa = s.split("\t");

                    // Check for XML-based node

                    FileBasedAssyNode xn = isFileBasedAssyNode(sa[1]);
                    if (xn != null) {
                        if (sa[0].equals("simkit.BasicSimEntity")) {
                            ((ViskitAssemblyController) getController()).newFileBasedEventGraphNode(xn, p);
                        } else if (sa[0].equals("java.beans.PropertyChangeListener")) {
                            ((ViskitAssemblyController) getController()).newFileBasedPropChangeListenerNode(xn, p);
                        }
                    } else {
                        // Else class-based node
                        if (sa[0].equals("simkit.BasicSimEntity")) {
                            ((ViskitAssemblyController) getController()).newEventGraphNode(sa[1], p);
                        } else if (sa[0].equals("java.beans.PropertyChangeListener")) {
                            ((ViskitAssemblyController) getController()).newPropChangeListenerNode(sa[1], p);
                        }
                    }

                    dragged = null;
                    return;
                } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private FileBasedAssyNode isFileBasedAssyNode(String s) {
        try {
            return FileBasedAssyNode.fromString(s);
        } catch (FileBasedAssyNode.exception exception) {
            return null;
        }
    }
    private boolean firstShown = false;

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (!firstShown) {
            firstShown = true;
            jsp.setDividerLocation(225);
            panJsp.setDividerLocation(0.5d);
        }
    }

    // Some private classes to implement dnd and dynamic cursor update

    class vCursorHandler extends MouseAdapter {

        Cursor select;
        Cursor arc;
        Cursor cancel;

        vCursorHandler() {
            super();
            select = Cursor.getDefaultCursor();
            //select    = new Cursor(Cursor.MOVE_CURSOR);
            arc = new Cursor(Cursor.CROSSHAIR_CURSOR);

            Image img = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/canArcCursor.png")).getImage();
            cancel = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "CancelArcCursor");
        }

        @Override
        public void mouseEntered(MouseEvent e) {
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
     *
     * @param event
     */
    @Override
    public void modelChanged(mvcModelEvent event) //-------------------------------------------
    {
        switch (event.getID()) {
            default:
                this.graphPane.viskitModelChanged((ModelEvent) event);
        }
    }

    /** permits user to edit existing entities */
    public boolean doEditEvGraphNode(EvGraphNode evNode) {
        return EventGraphNodeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), evNode);
    }

    public boolean doEditPclNode(PropChangeListenerNode pclNode) {
        return PclNodeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), pclNode); // blocks
    }

    public boolean doEditPclEdge(PropChangeEdge pclEdge) {
        return PclEdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), pclEdge);
    }

    public boolean doEditAdapterEdge(AdapterEdge aEdge) {
        return AdapterConnectionInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), aEdge);
    }

    public boolean doEditSimEvListEdge(SimEvListenerEdge seEdge) {
        return SimEventListenerConnectionInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), seEdge);
    }

    public void fileName(String s) // informative, tells view what we're working on
    {
        this.filename = s;
        String ttl = "Viskit Assembly Editor: " + s;
        setTitle(ttl);
        if (titlList != null) {
            titlList.setTitle(ttl, titlkey);
        }
    }

    private Object getLeafUO(JTree tree) {
        TreePath[] oa = tree.getSelectionPaths();
        if (oa != null) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) oa[0].getLastPathComponent();
            return dmtn.getUserObject();
        }
        return null;

    }

    public Object getSelectedEventGraph() {
        return getLeafUO(lTree);
    }

    public Object getSelectedPropChangeListener() {
        return getLeafUO(pclTree);
    }

    // For auto load of open eventgraphs

    public void addToEventGraphPallette(File f) {
        lTree.addContentRoot(f);
    }

    public void addToPropChangePallette(File f) {
        pclTree.addContentRoot(f);
    }

    public void removeFromEventGraphPallette(File f) {
        lTree.removeContentRoot(f);
    }

    public void removeFromPropChangePallette(File f) {
        pclTree.removeContentRoot(f);
    }

    public int genericAsk(String title, String msg) //---------------------------------------------
    {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public int genericAskYN(String title, String msg) //-----------------------------------------------
    {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION);
    }

    public int genericAsk2Butts(String title, String msg, String butt1, String butt2) {
        return JOptionPane.showOptionDialog(this, msg, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{butt1, butt2}, butt1);
    }

    public String promptForStringOrCancel(String title, String message, String initval) //---------------------------------------------------------------------------------
    {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }

    // ViskitView-required methods:

    private JFileChooser jfc = new JFileChooser(new File("."));

    /** Display a file chooser filtered by Assembly XML files only */
    public File openFileAsk() {

        jfc.setDialogTitle("Open Assembly File");
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // Look for assembly in the filename, Bug 1247 fix
        FileFilter filter = new AssemblyFileFilter("assembly");
        jfc.setFileFilter(filter);
        int returnVal = jfc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            log.info("You chose to open: " + jfc.getSelectedFile().getName());
            return jfc.getSelectedFile();
        }
        return null;
    }

    public File openRecentFilesAsk(Collection<String> lis) {
        String fn = RecentFilesDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), lis);
        if (fn != null) {
            File f = new File(fn);
            if (f.exists()) {
                return f;
            } else {
                JOptionPane.showMessageDialog(this, "File not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private File getUniqueName(String suggName) {
        String appnd = "";
        String suffix = "";

        int lastDot = suggName.lastIndexOf('.');
        if (lastDot != -1) {
            suffix = suggName.substring(lastDot);
            suggName = suggName.substring(0, lastDot);
        }
        int count = -1;
        File fil = null;
        do {
            fil = new File(suggName + appnd + suffix);
            appnd = "" + ++count;
        } while (fil.exists());

        return fil;
    }

    public File saveFileAsk(String suggName, boolean showUniqueName) {

        jfc.setDialogTitle("Save Assembly File");
        File fil = new File(suggName);
        if (showUniqueName) {
            fil = getUniqueName(suggName);
        }

        jfc.setSelectedFile(fil);
        int retv = jfc.showSaveDialog(this);
        if (retv == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile().exists()) {
                if (JOptionPane.YES_OPTION !=
                        JOptionPane.showConfirmDialog(this, "File exists.  Overwrite?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
                    return null;
                }
            }
            return jfc.getSelectedFile();
        }
        return null;
    }

    /**
     * Called by the controller after source has been generated.  Show to the user and provide him with the option
     * to save.
     *
     * @param s Java source
     */
    public void showAndSaveSource(String className, String s) {
        JFrame f = new SourceWindow(this, className, s);
        f.setTitle("Generated source from " + filename);
        f.setVisible(true);
    }

    public void displayXML(File f) {
        JComponent xt = null;
        try {
            xt = XTree.getTreeInPanel(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }
        //xt.setVisibleRowCount(25);
        xt.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final JFrame jf = new JFrame(f.getName());

        JPanel content = new JPanel();
        jf.setContentPane(content);

        content.setLayout(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(xt, BorderLayout.CENTER);
        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        buttPan.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
        JButton closeButt = new JButton("Close");
        buttPan.add(Box.createHorizontalGlue());
        buttPan.add(closeButt);
        content.add(buttPan, BorderLayout.SOUTH);

        //jf.pack();
        jf.setSize(475, 500);
        jf.setLocationRelativeTo(this);
        jf.setVisible(true);

        closeButt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jf.setVisible(false);
            }
        });
    }

    void clampHeight(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        comp.setMinimumSize(new Dimension(Integer.MAX_VALUE, d.height));
    }

    void clampSize(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        comp.setMaximumSize(d);
        comp.setMinimumSize(d);
    }
    private TitleListener titlList;
    private int titlkey;

    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlkey = key;

        // default
        if (titlList != null) {
            titlList.setTitle("Viskit Assembly Editor", titlkey);
        }
    }
}

interface DragStartListener {

    public void startingDrag(Transferable trans);
}