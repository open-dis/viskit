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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.log4j.Logger;
import viskit.images.AdapterIcon;
import viskit.images.PropChangeListenerIcon;
import viskit.images.SimEventListenerIcon;
import viskit.jgraph.vGraphAssemblyModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModel;
import viskit.mvc.mvcModelEvent;


/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 10, 2004
 * @since 2:07:37 PM
 * @version $Id$
 */
public class AssemblyViewFrame extends mvcAbstractJFrameView implements ViskitAssemblyView, DragStartListener {

    /** log4j log instance */
    static Logger log = Logger.getLogger(AssemblyViewFrame.class);
    /** Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc. */
    public static final int SELECT_MODE = 0;
    public static final int ADAPTER_MODE = 1;
    public static final int SIMEVLIS_MODE = 2;
    public static final int PCL_MODE = 3;
    private final static String FRAME_DEFAULT_TITLE = "Viskit Assembly Editor";
    private Color background = new Color(0xFB, 0xFB, 0xE5);
    private String filename;
    /** Toolbar for dropping icons, connecting, etc. */
    private JTabbedPane tabbedPane;
    private JToolBar toolBar;
    private JToggleButton selectMode;
    private JToggleButton adapterMode,  simEventListenerMode,  propChangeListenerMode;
    private LegosTree lTree,  pclTree;
    private JPanel assemblyEditorContent;
    private JMenuBar myMenuBar;
    private JMenuItem quitMenuItem;
    private JButton runButt;
    private int untitledCount = 0;

    public AssemblyViewFrame(AssemblyController controller) {
        this(false, controller);
    }

    public AssemblyViewFrame(boolean contentOnly, AssemblyController controller) {
        super(FRAME_DEFAULT_TITLE);
        initMVC(controller);   // set up mvc linkages
        initUI(contentOnly);          // build widgets

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

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return quitMenuItem;
    }

    /**
     * Initialize the MCV connections
     * @param ctrl
     */
    private void initMVC(AssemblyController ctrl) {
        setController(ctrl);
    }

    /**
     * Initialize the user interface
     * @param contentOnly
     */
    private void initUI(boolean contentOnly) {
        buildMenus(contentOnly);
        buildToolbar(contentOnly);

        // Build here to prevent NPE from EGContrlr
        buildTreePanels();

        // Set up a assemblyEditorContent level pane that will be the content pane. This
        // has a border layout, and contains the toolbar on the assemblyEditorContent and
        // the main splitpane underneath.

        // assemblyEditorContent level panel
        assemblyEditorContent = new JPanel();
        assemblyEditorContent.setLayout(new BorderLayout());
        assemblyEditorContent.add(getToolBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new TabSelectionHandler());
        assemblyEditorContent.add(tabbedPane, BorderLayout.CENTER);
        assemblyEditorContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (!contentOnly) // Can't add it here if we're going to put it somewhere else
        {
            getContentPane().add(assemblyEditorContent);
        }
    }
    private String FULLPATH = "FULLPATH";
    private String CLEARPATHFLAG = "<<clearPath>>";
    JMenu openRecentMenu;
    private _RecentFileListener myFileListener;

    private vGraphAssemblyComponentWrapper getCurrentVgacw() {
        JSplitPane jsplt = (JSplitPane) tabbedPane.getSelectedComponent();
        if (jsplt == null) {
            return null;
        }

        JScrollPane jSP = (JScrollPane) jsplt.getRightComponent();
        return (vGraphAssemblyComponentWrapper) jSP.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent() {
        vGraphAssemblyComponentWrapper vcw = getCurrentVgacw();
        return vcw.drawingSplitPane.getRightComponent();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JToolBar toolBar) {
        this.toolBar = toolBar;
    }

    class TabSelectionHandler implements ChangeListener {

        /** Tab switch: this will come in with the newly selected tab in place */
        public void stateChanged(ChangeEvent e) {
            vGraphAssemblyComponentWrapper myVgacw = getCurrentVgacw();

            if (myVgacw == null) {     // last tab has been closed
                fileName(null);
                return;
            }

            // Key to getting the LEGOs tree panel in each tab view
            myVgacw.drawingSplitPane.setLeftComponent(myVgacw.trees);

            setModel((AssemblyModel) myVgacw.model);          // hold on locally
            getController().setModel((AssemblyModel) myVgacw.model);  // tell controller
            AssemblyModel mod = (AssemblyModel) getModel();

            if (mod.getLastFile() != null) {

                // Also, tell the Design of Experiments Panel to update
                VGlobals.instance().amod = mod;
                ((AssemblyController) getController()).initOpenAssyWatch(mod.getLastFile(), mod.getJaxbRoot());
            }

            GraphMetaData gmd = myVgacw.model.getMetaData();
            if (gmd != null) {
                fileName(gmd.name);
            } else if (viskit.Vstatics.debug) {
                System.out.println("error: AssemblyViewFrame gmd null..");
            }
        }
    }

    class _RecentFileListener implements ViskitAssemblyController.RecentFileListener {

        public void listChanged() {
            ViskitAssemblyController acontroller = (ViskitAssemblyController) getController();
            java.util.List<String> lis = acontroller.getRecentFileList();
            openRecentMenu.removeAll();
            for (String fullPath : lis) {
                File f = new File(fullPath);
                if (!f.exists()) {
                    continue;
                }
                String nameOnly = f.getName();
                Action act = new ParameterizedAction(nameOnly);
                act.putValue(FULLPATH, fullPath);
                JMenuItem mi = new JMenuItem(act);
                mi.setToolTipText(fullPath);
                openRecentMenu.add(mi);
            }
            if (lis.size() > 0) {
                openRecentMenu.add(new JSeparator());
                Action act = new ParameterizedAction("clear");
                act.putValue(FULLPATH, CLEARPATHFLAG);  // flag
                JMenuItem mi = new JMenuItem(act);
                mi.setToolTipText("Clear this list");
                openRecentMenu.add(mi);
            }
        }
    }

    class ParameterizedAction extends javax.swing.AbstractAction {

        ParameterizedAction(String s) {
            super(s);
        }

        public void actionPerformed(ActionEvent ev) {
            ViskitAssemblyController acontroller = (ViskitAssemblyController) getController();
            String fullPath = (String) getValue(FULLPATH);
            if (fullPath.equals(CLEARPATHFLAG)) {
                acontroller.clearRecentFileList();
            } else {
                acontroller.openRecentAssembly(fullPath);
            }
        }
    }

    private void buildMenus(boolean contentOnly) {
        ViskitAssemblyController controller = (ViskitAssemblyController) getController();

        myFileListener = new _RecentFileListener();
        controller.addRecentFileListListener(myFileListener);

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // Set up file menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(buildMenuItem(controller, "newProject", "New Viskit Project", new Integer(KeyEvent.VK_V),
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        fileMenu.add(buildMenuItem(controller, "newAssembly", "New Assembly", new Integer(KeyEvent.VK_N),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, accelMod)));
        fileMenu.addSeparator();

        fileMenu.add(buildMenuItem(controller, "open", "Open", new Integer(KeyEvent.VK_O),
                KeyStroke.getKeyStroke(KeyEvent.VK_O, accelMod)));
        fileMenu.add(openRecentMenu = buildMenu("Open Recent Assembly"));
        fileMenu.add(buildMenuItem(this, "openProject", "Open Project", new Integer(KeyEvent.VK_P),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, accelMod)));

        // Bug fix: 1195
        fileMenu.add(buildMenuItem(controller, "close", "Close", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, accelMod)));
        fileMenu.add(buildMenuItem(controller, "closeAll", "Close All", null, null));
        fileMenu.add(buildMenuItem(controller, "save", "Save", new Integer(KeyEvent.VK_S),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, accelMod)));
        fileMenu.add(buildMenuItem(controller, "saveAs", "Save as...", new Integer(KeyEvent.VK_A), null));

        fileMenu.addSeparator();
        fileMenu.add(buildMenuItem(controller, "showXML", "View Saved XML", new Integer(KeyEvent.VK_X), null));
        fileMenu.add(buildMenuItem(controller, "generateJavaSource", "Generate Java Source", new Integer(KeyEvent.VK_J), null));
        fileMenu.add(buildMenuItem(controller, "captureWindow", "Save Screen Image", new Integer(KeyEvent.VK_I),
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod)));
        fileMenu.add(buildMenuItem(controller, "compileAssemblyAndPrepSimRunner", "Compile Assembly", new Integer(KeyEvent.VK_C),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));

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

        Help help = new Help(this);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        helpMenu.add(buildMenuItem(help, "doContents", "Contents", new Integer(KeyEvent.VK_C), null));
        helpMenu.add(buildMenuItem(help, "doSearch", "Search", new Integer(KeyEvent.VK_S), null));
        helpMenu.addSeparator();
        helpMenu.add(buildMenuItem(help, "doTutorial", "Tutorial", new Integer(KeyEvent.VK_T), null));
        helpMenu.add(buildMenuItem(help, "aboutEventGraphEditor", "About...", new Integer(KeyEvent.VK_A), null));
        myMenuBar.add(helpMenu);

        if (!contentOnly) {
            setJMenuBar(myMenuBar);
        }
    }

    private JMenu buildMenu(String name) {
        return new JMenu(name);
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
     * @return the current mode--select, add, arc, cancelArc
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
        log.error("assert false : \"getCurrentMode()\"");
        return 0;
    }

    private void buildToolbar(boolean contentOnly) {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        // Buttons for what mode we are in

        selectMode = makeJTButton(null, "viskit/images/selectNode.png",
                "Select items on the graph");
        Border defBor = selectMode.getBorder();
        selectMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(Color.lightGray, 2)));

        adapterMode = makeJTButton(null, new AdapterIcon(24, 24),
                "Connect assemblies with adapter pattern");
        defBor = adapterMode.getBorder();
        adapterMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        simEventListenerMode = makeJTButton(null, new SimEventListenerIcon(24, 24),
                "Connect assemblies through a SimEvent listener pattern");
        defBor = simEventListenerMode.getBorder();
        simEventListenerMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        propChangeListenerMode = makeJTButton(null, new PropChangeListenerIcon(24, 24),
                "Connect a property change listener to a SimEntity");
        defBor = propChangeListenerMode.getBorder();
        propChangeListenerMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 2)));

        JButton zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in on the graph");

        JButton zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out on the graph");

        Action runAction = ActionIntrospector.getAction(getController(), "compileAssemblyAndPrepSimRunner");
        runButt = makeButton(runAction, "viskit/images/Play24.gif",
                "Compile, initialize the assembly and prepare the Simulation Runner");
        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(adapterMode);
        modeButtonGroup.add(simEventListenerMode);
        modeButtonGroup.add(propChangeListenerMode);

        // Make selection mode the default mode
        selectMode.setSelected(true);

        getToolBar().add(new JLabel("Mode: "));

        getToolBar().add(selectMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(adapterMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(simEventListenerMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(propChangeListenerMode);

        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(new JLabel("Zoom: "));
        getToolBar().add(zoomIn);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOut);
        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(new JLabel("  Compile/initialize assembly runner: "));
        getToolBar().add(runButt);

        // Let the opening of Assembliess make this visible
        getToolBar().setVisible(false);

        zoomIn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getCurrentVgacw().setScale(getCurrentVgacw().getScale() + 0.1d);
            }
        });
        zoomOut.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getCurrentVgacw().setScale(Math.max(getCurrentVgacw().getScale() - 0.1d, 0.1d));
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
                getCurrentVgacw().setPortsVisible(tOrF);
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

    /** Enable adding of one or more Assembly tabs
     * @param mod the ViskitAssemblyModel to view on this tab
     */
    public void addTab(ViskitAssemblyModel mod) {
        vGraphAssemblyModel vGAmod = new vGraphAssemblyModel();
        vGraphAssemblyComponentWrapper graphPane = new vGraphAssemblyComponentWrapper(vGAmod, this);
        vGAmod.graph = graphPane;                               // todo fix this
        graphPane.model = mod;

        graphPane.trees = treePanels;
        graphPane.trees.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        graphPane.trees.setMinimumSize(new Dimension(20, 20));
        graphPane.trees.setDividerLocation(250);

        // Split pane with the canvas on the right and a split pane with LEGO tree and PCLs on the left.
        JScrollPane jscrp = new JScrollPane(graphPane);
        jscrp.setPreferredSize(new Dimension(500, 500));  // experiment to see if splitpane
        graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPane.trees, jscrp);
        graphPane.drawingSplitPane.setOneTouchExpandable(true);

        graphPane.addMouseListener(new vCursorHandler());
        try {
            graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
        } catch (Exception e) {
            log.error("assert false : \"Drop target init. error\"");
        }

        tabbedPane.add("untitled" + untitledCount++, graphPane.drawingSplitPane);
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane); // bring to front

        setModel((mvcModel) mod); // the view holds only one model, so it gets overwritten with each tab
    // but this call serves also to register the view with the passed model
    }

    public void delTab(ViskitAssemblyModel mod) {
        Component[] ca = tabbedPane.getComponents();

        for (int i = 0; i < ca.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getRightComponent();
            vGraphAssemblyComponentWrapper vgacw = (vGraphAssemblyComponentWrapper) jsp.getViewport().getComponent(0);
            if (vgacw.model == mod) {
                tabbedPane.remove(i);
                vgacw.isActive = false;

                // Don't allow operation of tools with no Assembly tab in view (NPEs)
                if (tabbedPane.getTabCount() == 0) {
                    getToolBar().setVisible(false);
                }
                return;
            }
        }
    }

    public ViskitAssemblyModel[] getOpenModels() {
        Component[] ca = tabbedPane.getComponents();
        ViskitAssemblyModel[] vm = new ViskitAssemblyModel[ca.length];
        for (int i = 0; i < vm.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getRightComponent();
            vGraphAssemblyComponentWrapper vgacw = (vGraphAssemblyComponentWrapper) jsp.getViewport().getComponent(0);
            vm[i] = vgacw.model;
        }
        return vm;
    }

    void rebuildTreePanels() {
        lTree.clear();
        JSplitPane treeSplit = buildTreePanels();
        getCurrentVgacw().drawingSplitPane.setTopComponent(treeSplit);
        treeSplit.setDividerLocation(250);
        lTree.repaint();
    }
    private JSplitPane treePanels;

    private JSplitPane buildTreePanels() {

        lTree = new LegosTree("simkit.BasicSimEntity", "viskit/images/assembly.png",
                this, "Drag an Event Graph onto the canvas to add it to the assembly");

        // Decouple diskit from vanilla Viskit operation
        File diskitJar = new File("lib/ext/diskit.jar");
        String[] extraCP = SettingsDialog.getExtraClassPath();
        if (extraCP != null) {
            for (String path : extraCP) { // tbd same for pcls
                if (new File(path).exists()) {
                    if (path.endsWith(".jar")) {
                        if (diskitJar.getName().contains(new File(path).getName())) {
                            continue;
                        } else {
                            lTree.addContentRoot(new File(path));
                        }

                    // A new project may contain an empty EventGraphs directory
                    } else if (new File(path).listFiles().length == 0) {
                        continue;
                    } else {
                        lTree.addContentRoot(new File(path), true);
                    }
                }
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

        PropChangeListenersPanel pclPan = new PropChangeListenersPanel(pclTree);

        lTree.setBackground(background);
        pclTree.setBackground(background);

        treePanels = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lPan, pclPan);
        treePanels.setBorder(null);
        treePanels.setOneTouchExpandable(true);

        pclPan.setMinimumSize(new Dimension(20, 80));
        lPan.setMinimumSize(new Dimension(20, 80));
        lPan.setPreferredSize(new Dimension(20, 240)); // give it some height for the initial split

        lTree.setDragEnabled(true);
        pclTree.setDragEnabled(true);

        return treePanels;
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

    // TODO: This saves the size/shape of the EG Editor frame, but doesn't
    // load this config on next startup
    public void prepareToQuit() {
        Rectangle bounds = getBounds();
        ViskitConfig.instance().setVal(ViskitConfig.ASSY_EDITOR_FRAME_BOUNDS_KEY + "[@h]", "" + bounds.height);
        ViskitConfig.instance().setVal(ViskitConfig.ASSY_EDITOR_FRAME_BOUNDS_KEY + "[@w]", "" + bounds.width);
        ViskitConfig.instance().setVal(ViskitConfig.ASSY_EDITOR_FRAME_BOUNDS_KEY + "[@x]", "" + bounds.x);
        ViskitConfig.instance().setVal(ViskitConfig.ASSY_EDITOR_FRAME_BOUNDS_KEY + "[@y]", "" + bounds.y);
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
        }
    }

    /**
     * This is where the "master" model (simkit.viskit.model.Model) updates the view.
     * Not so much to do in this editor as in EventGraphViewFrame
     *
     * @param event
     */
    @Override
    public void modelChanged(mvcModelEvent event) {
        switch (event.getID()) {
            default:
                getCurrentVgacw().viskitModelChanged((ModelEvent) event);
        }
    }

    /** permits user to edit existing entities
     * @param evNode
     * @return
     */
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

    public int genericAsk(String title, String msg) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public int genericAskYN(String title, String msg) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION);
    }

    public int genericAsk2Butts(String title, String msg, String butt1, String butt2) {
        return JOptionPane.showOptionDialog(this, msg, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{butt1, butt2}, butt1);
    }

    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }    // ViskitView-required methods:
    private JFileChooser jfc;

    private JFileChooser buildOpenSaveChooser() {

        // Try to open in the current project directory for Assemblies
        if (VGlobals.instance().getCurrentViskitProject() != null) {
            return new JFileChooser(VGlobals.instance().getCurrentViskitProject().getAssemblyDir());
        } else {
            return new JFileChooser(new File(ViskitProject.MY_VISKIT_PROJECTS_DIR));
        }
    }

    /** Display a file chooser filtered by Assembly XML files only
     * @return the chosen XML Assembly file
     */
    public File openFileAsk() {
        if (jfc == null) {
            jfc = buildOpenSaveChooser();
        }

        jfc.setDialogTitle("Open Assembly File");
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Look for assembly in the filename, Bug 1247 fix
        FileFilter filter = new AssemblyFileFilter("assembly");
        jfc.setFileFilter(filter);
        int returnVal = jfc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            jfc.setFileFilter(null);  // TODO: For JFC Swing issue with Win32ShellFolder2? // what's this? (mike asks)
            log.info("You chose to open: " + f.getName());
            return f;
        }
        jfc = null;
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

    public void fileName(String s) {
        boolean nullString = !(s != null && s.length() > 0);
        String ttl = nullString ? FRAME_DEFAULT_TITLE : "Viskit Assembly: " + s;
        setTitle(ttl);
        if (!nullString) {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), s);
        }
        if (this.titlList != null) {
            titlList.setTitle(ttl, titlkey);
        }
    }

    /** Open an already existing Viskit Project */
    public void openProject() {
        jfc = new JFileChooser(new File(ViskitProject.MY_VISKIT_PROJECTS_DIR));
        jfc.setDialogTitle("Open an Existing Viskit Project");
        jfc.setDialogType(JFileChooser.OPEN_DIALOG);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ((AssemblyController) getController()).openProject(jfc, this);
        jfc = null;
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

    /** Saves the current Assembly "as" desired by the user
     *
     * @param suggName the package and file name of the Assembly
     * @param showUniqueName show Assembly name only
     * @return a File object of the saved Assembly
     */
    public File saveFileAsk(String suggName, boolean showUniqueName) {
        if (jfc == null) {
            jfc = buildOpenSaveChooser();
        }

        jfc.setDialogTitle("Save Assembly File");
        File fil = new File(VGlobals.instance().getCurrentViskitProject().getAssemblyDir(), suggName);
        if (!fil.getParentFile().isDirectory()) {
            fil.getParentFile().mkdirs();
        }
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
        jfc = null;
        return null;
    }

    /**
     * Called by the controller after source has been generated.  Show to the
     * user and provide the option to save.
     *
     * @param className
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
                jf.dispose();
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
            titlList.setTitle(FRAME_DEFAULT_TITLE, titlkey);
        }
    }
}

interface DragStartListener {

    public void startingDrag(Transferable trans);
}