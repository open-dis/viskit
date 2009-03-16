package viskit;

import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.EventGraphFileFilter;
import org.apache.log4j.Logger;
import viskit.images.CanArcIcon;
import viskit.images.EventNodeIcon;
import viskit.images.SchedArcIcon;
import viskit.jgraph.vGraphModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModel;
import viskit.mvc.mvcModelEvent;

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
import java.util.Set;
import java.util.TooManyListenersException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Main "view" of the Viskit app.  This class controls a 3-paneled JFrame
 * showing a jgraph on the left and state variables and sim parameters panels on
 * the right, with menus and a toolbar.  To fully implement application-level
 * MVC, events like the dragging and dropping of a node on the screen are first
 * recognized in this class, but the GUI is not yet changed.  Instead, this
 * class (the View) messages the controller class (EventGraphController -- by
 * means of the ViskitController i/f).  The controller then informs the model
 * (Model), which then updates itself and "broadcasts" that fact.  This class is
 * a model listener, so it gets the report, then updates the GUI.  A round trip.
 *
 * 20 SEP 2005: Updated to show multiple open eventgraphs.  The controller is
 * largely unchanged.  To understand the flow, understand that
 * 1) The tab "ChangeListener" plays a key role;
 * 2) When the ChangeListener is hit, the controller.setModel() method installs
 * the appropriate model for the newly-selected eventgraph.
 *
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects</p>
 * MOVES Institute</p>
 * Naval Postgraduate School, Monterey CA</p>
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 12:52:59 PM
 * @version $Id$
 */
public class EventGraphViewFrame extends mvcAbstractJFrameView implements ViskitView {
    // Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc.
    public final static int SELECT_MODE = 0;
    public final static int ADD_NODE_MODE = 1;
    public final static int ARC_MODE = 2;
    public final static int CANCEL_ARC_MODE = 3;
    public final static int SELF_REF_MODE = 4;
    static Logger log = Logger.getLogger(EventGraphViewFrame.class);
    /** Toolbar for dropping icons, connecting, etc. */
    private JToolBar toolBar;    // Mode buttons on the toolbar
    private JLabel addEvent;
    private JLabel addSelfRef;
    private JToggleButton selectMode;
    private JToggleButton arcMode;
    private JToggleButton cancelArcMode;
    Help help;
    private JTabbedPane tabbedPane;
    private JPanel eventGraphViewerContent;
    private JMenuBar myMenuBar;
    private JMenuItem quitMenuItem;
    private TitleListener titlList;
    private int titlKey;
    public EventGraphController controller;
    private final static String FRAME_DEFAULT_TITLE = " Viskit Event Graph Editor";

    public EventGraphViewFrame(EventGraphController ctrl) {
        this(false, ctrl);
    }

    /**
     * Constructor; lays out initial GUI objects
     * @param contentOnly
     * @param ctrl
     */
    public EventGraphViewFrame(boolean contentOnly, EventGraphController ctrl) {
        super(FRAME_DEFAULT_TITLE);
        this.controller = ctrl;
        initMVC(ctrl);   // set up mvc linkages
        initUI(contentOnly);    // build widgets

        if (!contentOnly) {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((d.width - 800) / 2, (d.height - 600) / 2);
            setSize(800, 600);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    controller.quit();
                // if this simply returns, nothing happens
                // else, the controller will Sys.exit()
                }
            });
            ImageIcon icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitSplash2.png"));
            setIconImage(icon.getImage());
        }
    }

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
     * Initialize the MCV connections
     * @param ctrl the EventGraphController
     */
    private void initMVC(EventGraphController ctrl) {
        setController(ctrl);
    }

    /**
     * Initialize the user interface
     * @param contentOnly
     */
    private void initUI(boolean contentOnly) {
        // Layout menus
        buildMenus(contentOnly);

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
        if (!contentOnly) // Can't add it here if we're going to put it somewhere  else
        {
            getContentPane().add(eventGraphViewerContent);
        }
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

    class TabSelectionHandler implements ChangeListener {

        /** Tab switch: this will come in with the newly selected tab in place */
        public void stateChanged(ChangeEvent e) {
            VgraphComponentWrapper myVgcw = getCurrentVgcw();

            if (myVgcw == null) {     // last tab has been closed
                setSelectedEventGraphName(null);
                return;
            }
            
            setModel((Model) myVgcw.model);        // hold on locally
            getController().setModel(getModel());  // tell controller
            adjustMenus((ViskitModel) getModel()); // enable/disable menu items based on new EG

            GraphMetaData gmd = ((ViskitModel) getModel()).getMetaData();
            if (gmd != null) {
                setSelectedEventGraphName(gmd.name);
                setSelectedEventGraphDescription(gmd.description);
            } else if (viskit.Vstatics.debug) {
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
        vp.addPlusListener(ActionIntrospector.getAction((ViskitController) getController(), "newStateVariable"));

        // Introspector can't handle a param to the method....?
        vp.addMinusListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ((ViskitController) getController()).deleteStateVariable((vStateVariable) event.getSource());
            }
        });

        vp.addDoubleClickedListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ((ViskitController) getController()).stateVariableEdit((vStateVariable) event.getSource());
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
        pp.addPlusListener(ActionIntrospector.getAction((ViskitController) getController(), "newSimParameter"));

        // Introspector can't handle a param to the method....?
        pp.addMinusListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ((ViskitController) getController()).deleteSimParameter((vParameter) event.getSource());
            }
        });
        pp.addDoubleClickedListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                ((ViskitController) getController()).simParameterEdit((vParameter) event.getSource());
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

            public void actionPerformed(ActionEvent e) {
                String s = (String) e.getSource();
                if (s != null) {
                    ((ViskitController) getController()).codeBlockEdit((String) e.getSource());
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

    public void addTab(ViskitModel mod) {
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
            log.error("Drop target init. error");
            log.error(tmle);
        }

        // the view holds only one model, so it gets overwritten with each tab
        // but this call serves also to register the view with the passed model
        setModel((mvcModel) mod);
        
        tabbedPane.add("" + untitledCount++, graphPane.drawingSplitPane);        
        
        // Bring the JGraph component to front  
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane);

        // Now expose the EventGraph toolbar
        getToolBar().setVisible(true);
    }

    public void delTab(ViskitModel mod) {
        Component[] ca = tabbedPane.getComponents();

        for (int i = 0; i < ca.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
            VgraphComponentWrapper vgcw = (VgraphComponentWrapper) jsp.getViewport().getComponent(0);
            if (vgcw.model == mod) {
                tabbedPane.remove(i);
                vgcw.isActive = false;

                // Don't allow operation of tools with no Event Graph tab in view (NPEs)
                if (tabbedPane.getTabCount() == 0) {
                    getToolBar().setVisible(false);
                }
                return;
            }
        }
    }

    public ViskitModel[] getOpenModels() {
        Component[] ca = tabbedPane.getComponents();
        ViskitModel[] vm = new ViskitModel[ca.length];
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

    public void prepareToQuit() {
        Rectangle bounds = getBounds();
        XMLConfiguration appConfig = ViskitConfig.instance().getViskitAppConfig();
        appConfig.setProperty(ViskitConfig.EG_EDITOR_FRAME_BOUNDS_KEY + "[@h]", "" + bounds.height);
        appConfig.setProperty(ViskitConfig.EG_EDITOR_FRAME_BOUNDS_KEY + "[@w]", "" + bounds.width);
        appConfig.setProperty(ViskitConfig.EG_EDITOR_FRAME_BOUNDS_KEY + "[@x]", "" + bounds.x);
        appConfig.setProperty(ViskitConfig.EG_EDITOR_FRAME_BOUNDS_KEY + "[@y]", "" + bounds.y);
    }

    /**
     * run the add parameter dialog
     * @return the String representation of this parameter
     */
    public String addParameterDialog() {

        if (ParameterDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, null)) {      // blocks here
            ((ViskitController) getController()).buildNewSimParameter(ParameterDialog.newName,
                    ParameterDialog.newType,
                    "new value here",
                    ParameterDialog.newComment);
            return ParameterDialog.newName;
        }
        return null;
    }

    public String addStateVariableDialog() {
        if (StateVariableDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, null)) {      // blocks here
            ((ViskitController) getController()).buildNewStateVariable(StateVariableDialog.newName,
                    StateVariableDialog.newType,
                    "new value here",
                    StateVariableDialog.newComment);
            return StateVariableDialog.newName;
        }
        return null;
    }

    /**
     * Do menu layout work here.  These menus, and the toggle buttons which
     * follow, make use of the "actions" package, which
     * @param mod
     */
    private void adjustMenus(ViskitModel mod) {
        //todo
    }
    private String FULLPATH = "FULLPATH";
    private String CLEARPATHFLAG = "<<clearPath>>";

    class _RecentFileListener implements ViskitController.RecentFileListener {

        public void listChanged() {
            ViskitController vcontroller = (ViskitController) getController();
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

        public void actionPerformed(ActionEvent ev) {
            ViskitController vcontroller = (ViskitController) getController();
            String fullPath = (String) getValue(FULLPATH);
            if (fullPath.equals(CLEARPATHFLAG)) {
                vcontroller.clearRecentFileSet();
            } else {
                vcontroller.openRecentEventGraph(fullPath);
            }
        }
    }
    private JMenu openRecentMenu;
    private _RecentFileListener myFileListener;

    private void buildMenus(boolean contentOnly) {
        ViskitController vcontroller = (ViskitController) getController();

        myFileListener = new _RecentFileListener();
        vcontroller.addRecentFileListListener(myFileListener);

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // Set up file menu
        JMenu fileMenu = new JMenu("File");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(buildMenuItem(vcontroller, "newProject", "New Viskit Project", new Integer(KeyEvent.VK_V),
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "newEventGraph", "New Event Graph", new Integer(KeyEvent.VK_N),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, accelMod)));
        fileMenu.addSeparator();

        fileMenu.add(buildMenuItem(vcontroller, "open", "Open", new Integer(KeyEvent.VK_O),
                KeyStroke.getKeyStroke(KeyEvent.VK_O, accelMod)));
        fileMenu.add(openRecentMenu = buildMenu("Open Recent Event Graph"));
        fileMenu.add(buildMenuItem(this, "openProject", "Open Project", new Integer(KeyEvent.VK_P),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "close", "Close", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "closeAll", "Close All", null, null));
        fileMenu.add(buildMenuItem(vcontroller, "save", "Save", new Integer(KeyEvent.VK_S),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, accelMod)));
        fileMenu.add(buildMenuItem(vcontroller, "saveAs", "Save as...", new Integer(KeyEvent.VK_A), null));
        fileMenu.addSeparator();
        fileMenu.add(buildMenuItem(vcontroller, "showXML", "View Saved XML", new Integer(KeyEvent.VK_X), null));
        fileMenu.add(buildMenuItem(vcontroller, "generateJavaSource", "Generate Java Source", new Integer(KeyEvent.VK_J), null));
        fileMenu.add(buildMenuItem(vcontroller, "captureWindow", "Save Screen Image", new Integer(KeyEvent.VK_I),
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod)));
        if (!contentOnly) {
            fileMenu.addSeparator();
            fileMenu.add(buildMenuItem(vcontroller, "runAssemblyEditor", "Assembly Editor", null, null));
        }
        if (contentOnly) {
            fileMenu.addSeparator();
            fileMenu.add(buildMenuItem(vcontroller, "settings", "Settings", null, null));
        }
        fileMenu.addSeparator();
        fileMenu.add(quitMenuItem = buildMenuItem(vcontroller, "quit", "Exit", new Integer(KeyEvent.VK_X), KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK)));

        // Set up edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        // the next three are disabled until something is selected
        editMenu.add(buildMenuItem(vcontroller, "cut", "Cut", new Integer(KeyEvent.VK_T),
                KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));
        editMenu.add(buildMenuItem(vcontroller, "copy", "Copy", new Integer(KeyEvent.VK_C),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));
        editMenu.add(buildMenuItem(vcontroller, "paste", "Paste Events", new Integer(KeyEvent.VK_P),
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));

        // These 3 start off being disabled, until something is selected
        ActionIntrospector.getAction(vcontroller, "cut").setEnabled(false);
        ActionIntrospector.getAction(vcontroller, "copy").setEnabled(false);
        ActionIntrospector.getAction(vcontroller, "paste").setEnabled(false);

        editMenu.addSeparator();

        editMenu.add(buildMenuItem(vcontroller, "newNode", "Add Event Node", new Integer(KeyEvent.VK_N), null));
        editMenu.add(buildMenuItem(vcontroller, "newSimParameter", "Add Simulation Parameter...", new Integer(KeyEvent.VK_S), null));
        editMenu.add(buildMenuItem(vcontroller, "newStateVariable", "Add State Variable...", new Integer(KeyEvent.VK_V), null));
        editMenu.add(buildMenuItem(vcontroller, "newSelfRefEdge", "Add Self-Referential Edge...", new Integer(KeyEvent.VK_R), null));

        // This starts off being disabled, until something is selected
        ActionIntrospector.getAction(vcontroller, "newSelfRefEdge").setEnabled(false);

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(vcontroller, "editGraphMetaData", "Edit Properties...", new Integer(KeyEvent.VK_E),
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));

        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(fileMenu);
        myMenuBar.add(editMenu);

        help = new Help(this);
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

        JButton zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in on the graph");

        JButton zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out on the graph");

        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(arcMode);
        modeButtonGroup.add(cancelArcMode);

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

            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setScale(getCurrentVgcw().getScale() + 0.1d);
            }
        });
        zoomOut.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setScale(Math.max(getCurrentVgcw().getScale() - 0.1d, 0.1d));
            }
        });

        addEvent.setTransferHandler(new TransferHandler("text"));
        addEvent.addMouseListener(new DragMouseAdapter());
        addSelfRef.setTransferHandler(new TransferHandler("text"));
        addSelfRef.addMouseListener(new DragMouseAdapter());

        // These buttons perform operations that are internal to our view class, and therefore their operations are
        // not under control of the application controller (EventGraphController.java).  Small, simple anonymous inner classes
        // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        selectMode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setPortsVisible(false);
            }
        });
        arcMode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setPortsVisible(true);
            }
        });
        cancelArcMode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getCurrentVgcw().setPortsVisible(true);
            }
        });

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

            // Check if we should size the cursor
            Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
            if (d.width != 0 && d.height != 0) {
                buildCancelCursor(img);
            } else {
                cancel = Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(0, 0), "CancelArcCursor");
            }
        }

        /**
         * This is a lot of work to build a cursor.
         *
         * @param img
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

            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                this.infoflags = infoflags;
                return (infoflags & ImageObserver.ALLBITS) == 0;
            }

            public void run() {
                infoflags = 0;
                int w = img.getWidth(this);
                int h = img.getHeight(this);    // set image observer
                if (w == -1 || h == -1) {
                    waitForIt();
                }
                BufferedImage bi = null;
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gs = ge.getDefaultScreenDevice();
                GraphicsConfiguration gc = gs.getDefaultConfiguration();
                Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
                bi = gc.createCompatibleImage(d.width, d.height, Transparency.BITMASK);
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

        public void drop(DropTargetDropEvent dtde) {
            Point p = dtde.getLocation();  // subtract the size of the label
            if (dragger == NODE_DRAG) {
                Point pp = new Point(
                        p.x - addEvent.getWidth(),
                        p.y - addEvent.getHeight());
                ((ViskitController) getController()).buildNewNode(pp);
            } else {
                // get the node in question from the graph
                Object o = getCurrentVgcw().getViskitElementAt(p);
                if (o != null && o instanceof EventNode) {
                    EventNode en = (EventNode) o;
                    // We're making a self-referential arc
                    ((ViskitController) getController()).buildNewArc(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                }
            }
        }
    }
    private JFileChooser jfc;

    private JFileChooser buildOpenSaveChooser() {

        // Try to open in the current project directory for EventGraphs
        if (VGlobals.instance().getCurrentViskitProject() != null) {
            return new JFileChooser(VGlobals.instance().getCurrentViskitProject().getEventGraphDir());
        } else {
            return new JFileChooser(new File(ViskitProject.MY_VISKIT_PROJECTS_DIR));
        }
    }

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
    public File saveFileAsk(String suggName, boolean showUniqueName) {
        if (jfc == null) {
            jfc = buildOpenSaveChooser();
        }

        File fil = new File(VGlobals.instance().getCurrentViskitProject().getEventGraphDir(), suggName);
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
        return null;
    }

    public File openRecentFilesAsk(Collection<String> lis) {
        String fn = RecentFilesDialog.showDialog(VGlobals.instance().getMainAppWindow(), this, lis);
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

    public void setSelectedEventGraphName(String s) {
        boolean nullString = !(s != null && s.length() > 0);
        String ttl =
                nullString ? FRAME_DEFAULT_TITLE :
                    " Project: " + ViskitConfig.instance().getVal(ViskitConfig.PROJECT_TITLE_NAME);
        setTitle(ttl);
        if (!nullString) {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), s);
        }
        if (this.titlList != null) {
            titlList.setTitle(ttl, titlKey);
        }
    }

    public boolean doEditNode(EventNode node) {
        selectMode.doClick();     // always go back into select mode
        return EventInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), node); // blocks
    }

    public boolean doEditEdge(SchedulingEdge edge) {
        selectMode.doClick();     // always go back into select mode
        return EdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), edge); // blocks
    }

    public boolean doEditCancelEdge(CancellingEdge edge) {
        selectMode.doClick();     // always go back into select mode
        return EdgeInspectorDialog.showDialog(VGlobals.instance().getMainAppWindow(), VGlobals.instance().getMainAppWindow(), edge); // blocks
    }

    public boolean doEditParameter(vParameter param) {
        return ParameterDialog.showDialog(VGlobals.instance().getMainAppWindow(), getCurrentVgcw(), param);    // blocks
    }

    public boolean doEditStateVariable(vStateVariable var) {
        return StateVariableDialog.showDialog(VGlobals.instance().getMainAppWindow(), getCurrentVgcw(), var);
    }

    public int genericAsk(String title, String msg) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public int genericAskYN(String title, String msg) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION);
    }

    public void genericErrorReport(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }

    /**
     * This is where the "master" model (viskit.model.Model) updates the view.
     * @param event
     */
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
        }
    }

    /**
     * Called by the controller after source has been generated.
     * Show to the user and provide him with the option to save.
     * @param className
     * @param s Java source
     * @param filename
     */
    public void showAndSaveSource(String className, String s, String filename) {
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

    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlKey = key;

        // default
        if (titlList != null) {
            titlList.setTitle(FRAME_DEFAULT_TITLE, titlKey);
        }
    }
}