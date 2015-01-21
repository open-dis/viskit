package viskit.view;

import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.LogUtils;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import viskit.control.EventGraphController;
import viskit.Help;
import viskit.model.ModelEvent;
import viskit.util.TitleListener;
import viskit.VGlobals;
import viskit.ViskitConfig;
import viskit.ViskitProject;
import viskit.images.CanArcIcon;
import viskit.images.EventNodeIcon;
import viskit.images.SchedArcIcon;
import viskit.jgraph.VgraphComponentWrapper;
import viskit.jgraph.vGraphModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcController;
import viskit.mvc.mvcModelEvent;
import viskit.mvc.mvcRecentFileListener;
import viskit.util.EventGraphFileFilter;
import viskit.view.dialog.ParameterDialog;
import viskit.view.dialog.EdgeInspectorDialog;
import viskit.view.dialog.RecentFilesDialog;
import viskit.view.dialog.StateVariableDialog;
import viskit.view.dialog.EventInspectorDialog;

/**
 * Main "view" of the Viskit app. This class controls a 3-paneled JFrame showing
 a jgraph on the left and state variables and sim parameters panels on the
 right, with menus and a toolbar. To fully implement application-level MVC,
 events like the dragging and dropping of a node on the screen are first
 recognized in this class, but the GUI is not yet changed. Instead, this class
 (the View) messages the controller class (EventGraphControllerImpl -- by
 means of the EventGraphController i/f). The controller then informs the model
 (ModelImpl), which then updates itself and "broadcasts" that fact. This class
 is a model listener, so it gets the report, then updates the GUI. A round
 trip.

 20 SEP 2005: Updated to show multiple open eventgraphs. The controller is
 largely unchanged. To understand the flow, understand that 1) The tab
 "ChangeListener" plays a key role; 2) When the ChangeListener is hit, the
 controller.setModel() method installs the appropriate model for the
 newly-selectedTab eventgraph.

 OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects MOVES Institute
 Naval Postgraduate School, Monterey CA www.nps.edu
 *
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 12:52:59 PM
 * @version $Id$
 */
public class EventGraphViewFrame extends mvcAbstractJFrameView implements EventGraphView {
    // Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc.
    public final static int SELECT_MODE = 0;
    public final static int ADD_NODE_MODE = 1;
    public final static int ARC_MODE = 2;
    public final static int CANCEL_ARC_MODE = 3;
    public final static int SELF_REF_MODE = 4;

    /** Toolbar for dropping icons, connecting, etc. */
    private JToolBar toolBar;    // Mode buttons on the toolbar
    private JLabel addEvent;
    private JLabel addSelfRef;
    private JToggleButton selectMode;
    private JToggleButton arcMode;
    private JToggleButton cancelArcMode;
    public Help help;
    private JTabbedPane tabbedPane;
    private JPanel eventGraphViewerContent;
    private JMenuBar myMenuBar;
    private JMenuItem quitMenuItem;
    private TitleListener titlList;
    private int titlKey;
    private final static String FRAME_DEFAULT_TITLE = " Viskit Event Graph Editor";
    private final static boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("windows");

    /**
     * Constructor; lays out initial GUI objects
     * @param ctrl the controller for this frame (MVF)
     */
    public EventGraphViewFrame(mvcController ctrl) {
        super(FRAME_DEFAULT_TITLE);
        initMVC(ctrl);   // set up mvc linkages
        initUI();    // build widgets
    }

    /** @return the JPanel which is the content of this JFrame */
    public JComponent getContent() {
        return eventGraphViewerContent;
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return quitMenuItem;
    }

    /** @return the current mode--select, add, arc, cancelArc */
    public int getCurrentMode() {
        // Use the button's selected status to figure out what mode
        // we are in.

        if (selectMode.isSelected()) {
            return SELECT_MODE;
        }
        //if (addEvent.isSelected() == true) return ADD_NODE_MODE;
        if (arcMode.isSelected()) {
            return ARC_MODE;
        }
        if (cancelArcMode.isSelected()) {
            return CANCEL_ARC_MODE;
        }
        //if (selfRefMode.isSelected() == true) return SELF_REF_MODE;
        // If none of them are selected we're in serious trouble.
        //assert false : "getCurrentMode()";
        System.err.println("assert false : \"getCurrentMode()\"");
        return 0;
    }

    /**
     * Initialize the MVC connections
     * @param ctrl the controller for this view
     */
    private void initMVC(mvcController ctrl) {
        setController(ctrl);
    }

    /**
     * Initialize the user interface
     */
    private void initUI() {

        // Layout menus
        buildMenus();

        // Layout of toolbar
        setupToolbar();

        // Set up a eventGraphViewerContent level pane that will be the content pane. This
        // has a border layout, and contains the toolbar on the eventGraphViewerContent and
        // the main splitpane underneath.

        eventGraphViewerContent = new JPanel();
        eventGraphViewerContent.setLayout(new BorderLayout());
        eventGraphViewerContent.add(getToolBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new TabSelectionHandler());

        eventGraphViewerContent.add(tabbedPane, BorderLayout.CENTER);
        eventGraphViewerContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    private VgraphComponentWrapper getCurrentVgcw() {
        JSplitPane jsplt = (JSplitPane) tabbedPane.getSelectedComponent();
        if (jsplt == null) {
            return null;
        }

        JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
        return (VgraphComponentWrapper) jsp.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent() {
        VgraphComponentWrapper vcw = getCurrentVgcw();
        return vcw.drawingSplitPane.getLeftComponent();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JToolBar toolBar) {
        this.toolBar = toolBar;
    }

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {

            VgraphComponentWrapper myVgcw = getCurrentVgcw();

            if (myVgcw == null) {     // last tab has been closed
                setSelectedEventGraphName(null);
                return;
            }

            // NOTE: Although a somewhat good idea, perhaps the user does not
            // wish to have work saved when merely switching between tabs on
            // the EG pallete.  However, when switching to the Assy pallete, we
            // will save all EGs that have been modified
//            if (((Model)getModel()).isDirty()) {
//                ((EventGraphController)getController()).save();
//            }

            setModel((ModelImpl) myVgcw.model);    // hold on locally
            getController().setModel(getModel());  // tell controller

            adjustMenus((Model) getModel()); // enable/disable menu items based on new EG

            GraphMetaData gmd = ((Model) getModel()).getMetaData();
            if (gmd != null) {
                setSelectedEventGraphName(gmd.name);
                setSelectedEventGraphDescription(gmd.description);
            } else if (viskit.VStatics.debug) {
                System.out.println("error: EventGraphViewFrame gmd null..");
            }
        }
    }

    private void buildStateParamSplit(VgraphComponentWrapper vgcw) {
        // State variables area:
        JPanel stateVariablesPanel = new JPanel();
        stateVariablesPanel.setLayout(new BoxLayout(stateVariablesPanel, BoxLayout.Y_AXIS));
        stateVariablesPanel.add(Box.createVerticalStrut(5));

        JPanel eventGraphParametersSubpanel = new JPanel();
        eventGraphParametersSubpanel.setLayout(new BoxLayout(eventGraphParametersSubpanel, BoxLayout.X_AXIS));
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        JLabel stateVariableLabel = new JLabel("State Variables");
        stateVariableLabel.setToolTipText("State variables can be modified during event processing");
        eventGraphParametersSubpanel.add(stateVariableLabel);
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        stateVariablesPanel.add(eventGraphParametersSubpanel);

        VariablesPanel vp = new VariablesPanel(300, 5);
        stateVariablesPanel.add(vp);
        stateVariablesPanel.add(Box.createVerticalStrut(5));
        stateVariablesPanel.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for stateVariable adds, deletes and edits and tell it we'll be doing adds and deletes
        vp.doAddsAndDeletes(false);
        vp.addPlusListener(ActionIntrospector.getAction((EventGraphController) getController(), "newStateVariable"));

        // Introspector can't handle a param to the method....?
        vp.addMinusListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).deleteStateVariable((vStateVariable) event.getSource());
            }
        });

        vp.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).stateVariableEdit((vStateVariable) event.getSource());
            }
        });

        // Event graph parameters area
        JPanel parametersPanel = new JPanel();
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS)); //BorderLayout());
        parametersPanel.add(Box.createVerticalStrut(5));

        JLabel descriptionLabel = new JLabel("Description");
        descriptionLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        descriptionLabel.setToolTipText("Use \"Edit > Edit Properties\" panel (Ctrl-E) to modify description");

        JTextArea descriptionTextArea = new JTextArea();
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        parametersPanel.add(descriptionLabel);
        parametersPanel.add(Box.createVerticalStrut(5));

        // This works, you just have to have several lines of typed text to cause
        // the etched scrollbar to appear
        parametersPanel.add(descriptionScrollPane);
        parametersPanel.add(Box.createVerticalStrut(5));

        parametersPanel.setMinimumSize(new Dimension(20, 20));

        eventGraphParametersSubpanel = new JPanel();
        eventGraphParametersSubpanel.setLayout(new BoxLayout(eventGraphParametersSubpanel, BoxLayout.X_AXIS));
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        JLabel titleLabel = new JLabel("Event Graph Parameters");
        titleLabel.setToolTipText("Event graph parameters are initialized upon starting each simulation replication");
        eventGraphParametersSubpanel.add(titleLabel);
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        parametersPanel.add(eventGraphParametersSubpanel);

        ParametersPanel pp = new ParametersPanel(300, 5);
        parametersPanel.add(pp);
        parametersPanel.add(Box.createVerticalStrut(5));
        pp.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for parameter adds, deletes and edits and tell it we'll be doing adds and deletes
        pp.doAddsAndDeletes(false);
        pp.addPlusListener(ActionIntrospector.getAction((EventGraphController) getController(), "newSimParameter"));

        // Introspector can't handle a param to the method....?
        pp.addMinusListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).deleteSimParameter((vParameter) event.getSource());
            }
        });
        pp.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).simParameterEdit((vParameter) event.getSource());
            }
        });

        CodeBlockPanel codeblockPan = buildCodeBlockPanel();


        JSplitPane stateCblockSplt = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(stateVariablesPanel),
                new JScrollPane(buildCodeBlockComponent(codeblockPan)));
        stateCblockSplt.setResizeWeight(0.75);

        // Split pane that has description, parameters, state variables and code block.
        JSplitPane spltPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                parametersPanel,
                stateCblockSplt);
        spltPn.setResizeWeight(0.75);

        spltPn.setMinimumSize(new Dimension(20, 20));

        vgcw.stateParamSplitPane = spltPn;
        vgcw.paramPan = pp;
        vgcw.varPan = vp;
        vgcw.codeBlockPan = codeblockPan;
    }

    private CodeBlockPanel buildCodeBlockPanel() {
        CodeBlockPanel cbp = new CodeBlockPanel(this, true, "Event Graph Code Block");
        cbp.addUpdateListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) e.getSource();
                if (s != null) {
                    ((EventGraphController) getController()).codeBlockEdit((String) e.getSource());
                }
            }
        });
        return cbp;
    }

    private JComponent buildCodeBlockComponent(CodeBlockPanel cbp) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lab = new JLabel("Code Block");
        lab.setToolTipText("Use of the code block will cause code to run first" +
                " in the top of the Event's \"do\" method");
        lab.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        p.add(lab);
        cbp.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        p.add(cbp);
        p.setBorder(new EmptyBorder(5, 5, 5, 2));
        Dimension d = new Dimension(p.getPreferredSize());
        d.width = Integer.MAX_VALUE;
        p.setMaximumSize(d);

        return p;
    }

    @Override
    public void setSelectedEventGraphDescription(String description) {
        JSplitPane jsp = getCurrentVgcw().stateParamSplitPane;
        JPanel jp = (JPanel) jsp.getTopComponent();
        Component[] components = jp.getComponents();
        for (Component c : components) {
            if (c instanceof JScrollPane) {
                c = ((JScrollPane) c).getViewport().getComponent(0);
                ((JTextArea) c).setText(description);
            }
        }
    }

    int untitledCount = 0;

    @Override
    public void addTab(Model mod) {
        vGraphModel vmod = new vGraphModel();
        VgraphComponentWrapper graphPane = new VgraphComponentWrapper(vmod, this);
        vmod.graph = graphPane;                               // todo fix this
        graphPane.model = mod;

        buildStateParamSplit(graphPane);

        // Split pane with the canvas on the left and a split pane with state variables and parameters on the right.
        JScrollPane jsp = new JScrollPane(graphPane);

        graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp, graphPane.stateParamSplitPane);

        // This is the key to getting the jgraph half to come up appropriately
        // wide by giving the left component (JGraph side) most of the usable
        // extra space in this SplitPlane -> 75%
        graphPane.drawingSplitPane.setResizeWeight(0.75);
        graphPane.drawingSplitPane.setOneTouchExpandable(true);

        graphPane.addMouseListener(new vCursorHandler());
        try {
            graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
        } catch (TooManyListenersException tmle) {
            LogUtils.getLogger(EventGraphViewFrame.class).error(tmle);
        }

        // the view holds only one model, so it gets overwritten with each tab
        // but this call serves also to register the view with the passed model
        // by virtue of calling stateChanged()
        tabbedPane.add("" + untitledCount++, graphPane.drawingSplitPane);

        // Bring the JGraph component to front. Also, allows models their own
        // canvas to draw prevent a NPE
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane);

        // Now expose the EventGraph toolbar
        Runnable r = new Runnable() {
            @Override
            public void run() {
                getToolBar().setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void delTab(Model mod) {
        for (Component c : tabbedPane.getComponents()) {
            JSplitPane jsplt = (JSplitPane) c;
            JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
            VgraphComponentWrapper vgcw = (VgraphComponentWrapper) jsp.getViewport().getComponent(0);
            if (vgcw.model == mod) {
                tabbedPane.remove(c);
                vgcw.isActive = false;

                // Don't allow operation of tools with no Event Graph tab in view (NPEs)
                if (tabbedPane.getTabCount() == 0) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            getToolBar().setVisible(false);
                        }
                    };
                    SwingUtilities.invokeLater(r);
                }
                return;
            }
        }
    }

    @Override
    public Model[] getOpenModels() {
        Component[] ca = tabbedPane.getComponents();
        Model[] vm = new Model[ca.length];
        for (int i = 0; i < vm.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
            VgraphComponentWrapper vgcw = (VgraphComponentWrapper) jsp.getViewport().getComponent(0);
            vm[i] = vgcw.model;
        }
        return vm;
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);

        // tell the help screen where we are so he can center himself
        help.mainFrameLocated(this.getBounds());
    }

    /**
     * run the add parameter dialog
     * @return the String representation of this parameter
     */
    @Override
    public String addParameterDialog() {

        if (ParameterDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, null)) {      // blocks here
            ((EventGraphController) getController()).buildNewSimParameter(ParameterDialog.newName,
                    ParameterDialog.newType,
                    "new value here",
                    ParameterDialog.newComment);
            return ParameterDialog.newName;
        }
        return null;
    }

    @Override
    public String addStateVariableDialog() {
        if (StateVariableDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, null)) {      // blocks here
            ((EventGraphController) getController()).buildNewStateVariable(StateVariableDialog.newName,
                    StateVariableDialog.newType,
                    "new value here",
                    StateVariableDialog.newComment);
            return StateVariableDialog.newName;
        }
        return null;
    }

    /**
     * Do menu layout work here
     * @param mod the current model of our EG view
     */
    private void adjustMenus(Model mod) {
        //todo
    }
    private String FULLPATH = "FULLPATH";
    private String CLEARPATHFLAG = "<<clearPath>>";

    class RecentEgFileListener implements mvcRecentFileListener {

        @Override
        public void listChanged() {
            EventGraphController vcontroller = (EventGraphController) getController();
            Set<String> lis = vcontroller.getRecentFileSet();
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

        @Override
        public void actionPerformed(ActionEvent ev) {
            EventGraphController vcontroller = (EventGraphController) getController();
            String fullPath = (String) getValue(FULLPATH);
            if (fullPath.equals(CLEARPATHFLAG)) {
                vcontroller.clearRecentFileSet();
            } else {
                vcontroller.openRecentEventGraph(fullPath);
            }
        }
    }
    private JMenu openRecentMenu;
    private RecentEgFileListener myEgFileListener;

    private void buildMenus() {
        EventGraphController vcontroller = (EventGraphController) getController();

        myEgFileListener = new RecentEgFileListener();
        vcontroller.addRecentEgFileListener(myEgFileListener);

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // Set up file menu
        JMenu fileMenu = new JMenu("File");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(buildMenuItem(vcontroller, "newProject", "New Viskit Project", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "newEventGraph", "New Event Graph", KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, accelMod)));
        fileMenu.addSeparator();

        fileMenu.add(buildMenuItem(vcontroller, "open", "Open", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, accelMod)));
        fileMenu.add(openRecentMenu = buildMenu("Open Recent Event Graph"));
        fileMenu.add(buildMenuItem(this, "openProject", "Open Project", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_P, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "close", "Close", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "closeAll", "Close All", null, null));
        fileMenu.add(buildMenuItem(vcontroller, "save", "Save", KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "saveAs", "Save as...", KeyEvent.VK_A, null));
        fileMenu.addSeparator();
        fileMenu.add(buildMenuItem(vcontroller, "showXML", "View Saved XML", KeyEvent.VK_X, null));
        fileMenu.add(buildMenuItem(vcontroller, "generateJavaSource", "Generate Java Source", KeyEvent.VK_J, null));
        fileMenu.add(buildMenuItem(vcontroller, "captureWindow", "Save Screen Image", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod)));
        fileMenu.addSeparator();
        fileMenu.add(buildMenuItem(vcontroller, "settings", "Settings", null, null));
        fileMenu.addSeparator();
        fileMenu.add(quitMenuItem = buildMenuItem(vcontroller, "quit", "Exit",
                KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK)));

        // Set up edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        // the next three are disabled until something is selected
        editMenu.add(buildMenuItem(vcontroller, "cut", "Cut", KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));
        editMenu.add(buildMenuItem(vcontroller, "copy", "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));
        editMenu.add(buildMenuItem(vcontroller, "paste", "Paste Events", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));

        // These 3 start off being disabled, until something is selected
        ActionIntrospector.getAction(vcontroller, "cut").setEnabled(false);
        ActionIntrospector.getAction(vcontroller, "copy").setEnabled(false);
        ActionIntrospector.getAction(vcontroller, "paste").setEnabled(false);

        editMenu.addSeparator();

        editMenu.add(buildMenuItem(vcontroller, "newNode", "Add Event Node", KeyEvent.VK_N, null));
        editMenu.add(buildMenuItem(vcontroller, "newSimParameter", "Add Simulation Parameter...", KeyEvent.VK_S, null));
        editMenu.add(buildMenuItem(vcontroller, "newStateVariable", "Add State Variable...", KeyEvent.VK_V, null));
        editMenu.add(buildMenuItem(vcontroller, "newSelfRefEdge", "Add Self-Referential Edge...", KeyEvent.VK_R, null));

        // This starts off being disabled, until something is selectedTab
        ActionIntrospector.getAction(vcontroller, "newSelfRefEdge").setEnabled(false);

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(vcontroller, "editGraphMetaData", "Edit Properties...", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));

        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(fileMenu);
        myMenuBar.add(editMenu);

        help = new Help(this);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        helpMenu.add(buildMenuItem(help, "doContents", "Contents", KeyEvent.VK_C, null));
        helpMenu.add(buildMenuItem(help, "doSearch", "Search", KeyEvent.VK_S, null));
        helpMenu.addSeparator();
        helpMenu.add(buildMenuItem(help, "doTutorial", "Tutorial", KeyEvent.VK_T, null));
        helpMenu.add(buildMenuItem(help, "aboutEventGraphEditor", "About...", KeyEvent.VK_A, null));

        myMenuBar.add(helpMenu);
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel) {
        Action a = ActionIntrospector.getAction(source, method);
        Map<String, Object> map = new HashMap<>();
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

    private JMenu buildMenu(String name) {
        return new JMenu(name);
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
        b.setToolTipText(tt);
        b.setBorder(BorderFactory.createEtchedBorder());
        b.setText(null);
        return b;
    }

    private JLabel makeJLabel(String icPath, String tt) {
        JLabel jlab = new JLabel(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icPath)));
        jlab.setToolTipText(tt);
        return jlab;
    }

    private void setupToolbar() {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        // Buttons for what mode we are in

        addEvent = makeJLabel("viskit/images/eventNode.png",
                "Drag onto canvas to add new events to the event graph");
        addEvent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        addEvent.setIcon(new EventNodeIcon());
        addSelfRef = makeJLabel("viskit/images/selfArc.png",
                "Drag onto an existing event node to add a self-referential scheduling edge");

        addSelfRef.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        selectMode = makeJTButton(null, "viskit/images/selectNode.png",
                "Select items on the graph");
        arcMode = makeJTButton(null, "viskit/images/schedArc.png",
                "Connect nodes with a scheduling edge");
        // TODO:  self-referential canceling edge?

        arcMode.setIcon(new SchedArcIcon());
        cancelArcMode = makeJTButton(null, "viskit/images/canArc.png",
                "Connect nodes with a cancelling edge");
        cancelArcMode.setIcon(new CanArcIcon());

        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(arcMode);
        modeButtonGroup.add(cancelArcMode);

        JButton zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in on the graph");

        JButton zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out on the graph");

        // Make selection mode the default mode
        selectMode.setSelected(true);

        getToolBar().add(new JLabel("Add: "));
        getToolBar().add(addEvent);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addSelfRef);

        getToolBar().addSeparator(new Dimension(24, 24));

        getToolBar().add(new JLabel("Mode: "));
        getToolBar().add(selectMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(arcMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(cancelArcMode);

        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(new JLabel("Zoom: "));
        getToolBar().add(zoomIn);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOut);

        // Let the opening of EGs make this visible
        getToolBar().setVisible(false);

        zoomIn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setScale(getCurrentVgcw().getScale() + 0.1d);
            }
        });
        zoomOut.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setScale(Math.max(getCurrentVgcw().getScale() - 0.1d, 0.1d));
            }
        });

        addEvent.setTransferHandler(new TransferHandler("text"));
        addEvent.addMouseListener(new DragMouseAdapter());
        addSelfRef.setTransferHandler(new TransferHandler("text"));
        addSelfRef.addMouseListener(new DragMouseAdapter());

        // These buttons perform operations that are internal to our view class, and therefore their operations are
        // not under control of the application controller (EventGraphControllerImpl.java).  Small, simple anonymous inner classes
        // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        selectMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setPortsVisible(false);
            }
        });
        arcMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setPortsVisible(true);
            }
        });
        cancelArcMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setPortsVisible(true);
            }
        });

    }

    /** Changes the background color of EG tabs depending on model.isDirty()
     * status to give the user an indication of a good/bad save & compile
     * operation
     */
    public void toggleEgStatusIndicators() {

        int selectedTab = tabbedPane.getSelectedIndex();

        for (Component c : tabbedPane.getComponents()) {

            // This will fire a call to stateChanged() which also sets the
            // current model
            tabbedPane.setSelectedComponent(c);

            if (((Model) getModel()).isDirty()) {

                if (IS_WIN)
                    tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.RED.brighter());
                else
                   tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.RED.brighter());
            } else {

                if (IS_WIN)
                    tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.GREEN.darker());
                else
                    tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.GREEN.brighter());
            }
        }

        // Restore active tab and model by virtue of firing a call to stateChanged()
        tabbedPane.setSelectedIndex(selectedTab);
    }

    /** Some private classes to implement Drag and Drop (DnD) and dynamic cursor update */
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

            // Check if we should size the cursor
            Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
            if (d.width != 0 && d.height != 0 && System.getProperty("os.name").contains("Windows")) {

                // Only works on windoze
                buildCancelCursor(img);
            } else {
                cancel = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "CancelArcCursor");
            }
        }

        /**
         * This is a lot of work to build a cursor.
         *
         * @param img the cursor image to build
         */
        private void buildCancelCursor(Image img) {
            new Thread(new cursorBuilder(img)).start();
        }

        class cursorBuilder implements Runnable, ImageObserver {

            Image img;

            cursorBuilder(Image img) {
                this.img = img;
            }
            int infoflags;

            @Override
            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                this.infoflags = infoflags;
                return (infoflags & ImageObserver.ALLBITS) == 0;
            }

            @Override
            public void run() {
                infoflags = 0;
                int w = img.getWidth(this);
                int h = img.getHeight(this);    // set image observer
                if (w == -1 || h == -1) {
                    waitForIt();
                }

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gs = ge.getDefaultScreenDevice();
                GraphicsConfiguration gc = gs.getDefaultConfiguration();
                Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
                BufferedImage bi = gc.createCompatibleImage(d.width, d.height, Transparency.BITMASK);
                infoflags = 0;
                w = bi.getWidth(this);
                h = bi.getHeight(this);
                if (w == -1 || h == -1) {
                    waitForIt();
                }

                Graphics g = bi.createGraphics();
                infoflags = 0;
                if (!g.drawImage(img, 0, 0, this)) {
                    waitForIt();
                }

                cancel = Toolkit.getDefaultToolkit().createCustomCursor(bi, new Point(0, 0), "CancelArcCursor");
                g.dispose();
            }

            private void waitForIt() {
                while ((infoflags & ImageObserver.ALLBITS) == 0) {
                    Thread.yield();
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
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
    class DragMouseAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            JComponent c = (JComponent) e.getSource();
            if (c == EventGraphViewFrame.this.addSelfRef) {
                dragger = SELF_REF_DRAG;
            } else {
                dragger = NODE_DRAG;
            }

            TransferHandler handler = c.getTransferHandler();
            handler.exportAsDrag(c, e, TransferHandler.COPY);
        }
    }

    class vDropTargetAdapter extends DropTargetAdapter {

        @Override
        public void dragOver(DropTargetDragEvent e) {

            // NOTE: this action is very critical in getting JGraph 5.14 to
            // signal the drop method
            e.acceptDrag(e.getDropAction());
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            Point p = e.getLocation();  // subtract the size of the label
            if (dragger == NODE_DRAG) {
                Point pp = new Point(
                        p.x - addEvent.getWidth(),
                        p.y - addEvent.getHeight());
                ((EventGraphController) getController()).buildNewNode(pp);
            } else {
                // get the node in question from the graph
                Object o = getCurrentVgcw().getViskitElementAt(p);
                if (o != null && o instanceof EventNode) {
                    EventNode en = (EventNode) o;
                    // We're making a self-referential arc
                    ((EventGraphController) getController()).buildNewArc(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                }
            }
        }
    }
    private JFileChooser jfc;

    private JFileChooser buildOpenSaveChooser() {

        // Try to open in the current project directory for EventGraphs
        if (VGlobals.instance().getCurrentViskitProject() != null) {
            return new JFileChooser(VGlobals.instance().getCurrentViskitProject().getEventGraphsDir());
        } else {
            return new JFileChooser(new File(ViskitProject.MY_VISKIT_PROJECTS_DIR));
        }
    }

    @Override
    public File[] openFilesAsk() {
        jfc = buildOpenSaveChooser();
        jfc.setDialogTitle("Open Event Graph Files");

        // Bug fix: 1246
        jfc.addChoosableFileFilter(new EventGraphFileFilter(
                new String[] {"assembly", "smal", "x3d", "x3dv", "java", "class"}));

        // Bug fix: 1249
        jfc.setMultiSelectionEnabled(true);

        int retv = jfc.showOpenDialog(this);
        return (retv == JFileChooser.APPROVE_OPTION) ? jfc.getSelectedFiles() : null;
    }

    /** Open an already existing Viskit Project */
    public void openProject() {
        ((AssemblyViewFrame) VGlobals.instance().getAssemblyController().getView()).openProject();

        showProjectName();
    }

    @Override
    public void showProjectName() {

        // Set project title in Frame title bar
        String ttl = " Project: " + ViskitConfig.instance().getVal(ViskitConfig.PROJECT_TITLE_NAME);
        setTitle(ttl);
        if (this.titlList != null) {
            titlList.setTitle(ttl, titlKey);
        }
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

    /** Saves the current Event Graph "as" desired by the user
     *
     * @param suggName the package and file name of the EG
     * @param showUniqueName show EG name only
     * @return a File object of the saved EG
     */
    @Override
    public File saveFileAsk(String suggName, boolean showUniqueName) {
        if (jfc == null) {
            jfc = buildOpenSaveChooser();
        }

        File fil = new File(VGlobals.instance().getCurrentViskitProject().getEventGraphsDir(), suggName);
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
                        genericAskYN("File Exists",  "Overwrite? Confirm")) {
                    return null;
                }
            }
            return jfc.getSelectedFile();
        }
        return null;
    }

    @Override
    public File openRecentFilesAsk(Collection<String> lis) {
        String fn = RecentFilesDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, lis);
        if (fn != null) {
            File f = new File(fn);
            if (f.exists()) {
                return f;
            } else {
                genericReport(JOptionPane.ERROR_MESSAGE, "File not found.", f + " does not exist");
            }
        }
        return null;
    }

    @Override
    public void setSelectedEventGraphName(String s) {
        boolean nullString = !(s != null && s.length() > 0);
        if (!nullString) {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), s);
        }
    }

    @Override
    public boolean doEditNode(EventNode node) {
        selectMode.doClick();     // always go back into select mode
        return EventInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), node); // blocks
    }

    @Override
    public boolean doEditEdge(SchedulingEdge edge) {
        selectMode.doClick();     // always go back into select mode
        return EdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), edge); // blocks
    }

    @Override
    public boolean doEditCancelEdge(CancellingEdge edge) {
        selectMode.doClick();     // always go back into select mode
        return EdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), edge); // blocks
    }

    @Override
    public boolean doEditParameter(vParameter param) {
        return ParameterDialog.showDialog(VGlobals.instance().getMainAppWindow(), getCurrentVgcw(), param);    // blocks
    }

    @Override
    public boolean doEditStateVariable(vStateVariable var) {
        return StateVariableDialog.showDialog(VGlobals.instance().getMainAppWindow(), getCurrentVgcw(), var);
    }

    @Override
    public int genericAsk(String title, String msg) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    @Override
    public int genericAskYN(String title, String msg) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION);
    }

    @Override
    public void genericReport(int type, String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title, type);
    }

    @Override
    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }

    @Override
    public void modelChanged(mvcModelEvent event) {
        VgraphComponentWrapper vgcw = getCurrentVgcw();
        ParametersPanel pp = vgcw.paramPan;
        VariablesPanel vp = vgcw.varPan;
        switch (event.getID()) {
            // Changes the two side panels need to know about
            case ModelEvent.SIMPARAMETERADDED:
                pp.addRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.SIMPARAMETERDELETED:
                pp.removeRow(event.getSource());
                break;
            case ModelEvent.SIMPARAMETERCHANGED:
                pp.updateRow(event.getSource());
                break;
            case ModelEvent.STATEVARIABLEADDED:
                vp.addRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATEVARIABLEDELETED:
                vp.removeRow(event.getSource());
                break;
            case ModelEvent.STATEVARIABLECHANGED:
                vp.updateRow(event.getSource());
                break;
            case ModelEvent.CODEBLOCKCHANGED:
                vgcw.codeBlockPan.setData((String) event.getSource());
                break;
            case ModelEvent.NEWMODEL:
                vp.setData(null);
                pp.setData(null);

            // Changes the graph needs to know about
            default:
                vgcw.viskitModelChanged((ModelEvent) event);
                toggleEgStatusIndicators();
                break;
        }
    }

    @Override
    public void showAndSaveSource(String className, String s, String filename) {
        final JFrame f = new SourceWindow(this, className, s);
        f.setTitle("Generated source from " + filename);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                f.setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void displayXML(File f) {
        JComponent xt;
        try {
            xt = XTree.getTreeInPanel(f);
        } catch (Exception e) {
            genericReport(JOptionPane.ERROR_MESSAGE, "XML Display Error", e.getMessage());
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

        Runnable r = new Runnable() {

            @Override
            public void run() {
                jf.setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);

        closeButt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        jf.dispose();
                    }
                };
                SwingUtilities.invokeLater(r);
            }
        });
    }

    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlKey = key;

        showProjectName();
    }
}