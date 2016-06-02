package viskit.view;

import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.LogUtilities;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import viskit.Help;
import viskit.control.EventGraphController;
import viskit.ViskitConfiguration;
import viskit.model.ModelEvent;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitProject;
import viskit.control.AssemblyControllerImpl;
import viskit.control.EventGraphControllerImpl;
import viskit.images.CancellationArcIcon;
import viskit.images.EventNodeIcon;
import viskit.images.SchedulingArcIcon;
import viskit.jgraph.EventGraphComponentWrapper;
import viskit.jgraph.JGraphVisualModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcController;
import viskit.mvc.mvcModelEvent;
import viskit.mvc.mvcRecentFileListener;
import viskit.util.EventGraphFileFilter;
import viskit.view.dialog.ParameterDialog;
import viskit.view.dialog.EdgeInspectorDialog;
import viskit.view.dialog.StateVariableDialog;
import viskit.view.dialog.EventInspectorDialog;
import viskit.view.dialog.ProjectMetadataDialog;
import viskit.view.dialog.UserPreferencesDialog;

/**
 * Main "view" of the Viskit app. This class controls a 3-paneled JFrame showing
 a jgraph on the left and state variables and sim parameters panels on the
 right, with menus and a toolbar. To fully implement application-level MVC,
 events like the dragging and dropping of a node on the screen are first
 recognized in this class, but the GUI is not yet changed. Instead, this class
 (the View) messages the controller class (EventGraphControllerImpl -- by
 means of the EventGraphController i/f). The controller then informs the eventGraphModel
 (EventGraphModelImpl), which then updates itself and "broadcasts" that fact. This class
 is a eventGraphModel listener, so it gets the report, then updates the GUI. A round
 trip.

 20 SEP 2005: Updated to show multiple open event graphs. The controller is
 largely unchanged. To understand the flow, understand that 1) The tab
 "ChangeListener" plays a key role; 2) When the ChangeListener is hit, the
 controller.setModel() method installs the appropriate eventGraphModel for the
 newly-selectedTab event graph.

 OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects MOVES Institute
 Naval Postgraduate School, Monterey CA www.nps.edu
 *
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 12:52:59 PM
 * @version $Id$
 */
public class EventGraphViewFrame extends mvcAbstractJFrameView implements EventGraphView
{
    static final Logger LOG = LogUtilities.getLogger(EventGraphViewFrame.class);

    // Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc.
    public final static int SELECT_MODE          = 0;
    public final static int ADD_NODE_MODE        = 1;
    public final static int ARC_MODE             = 2;
    public final static int CANCEL_ARC_MODE      = 3;
    public final static int SELF_REF_MODE        = 4;
    public final static int SELF_REF_CANCEL_MODE = 5;

    private static final String FRAME_DEFAULT_TITLE = "Event Graph Editor";
    private static       String LOOK_AND_FEEL;

    /** Toolbar for dropping icons, connecting, etc. */
    private JToolBar      toolBar;    // Mode buttons on the toolbar
    private JLabel        addEvent;
    private JLabel        addSelfReferentialEdge;
    private JLabel        addSelfReferentialCancellingEdge;
    private JToggleButton selectMode;
    private JToggleButton arcMode;
    private JToggleButton cancelArcMode;
    private JTabbedPane   tabbedPane;
    private JMenuBar      myMenuBar;

    private JMenu openRecentEventGraphMenu, openRecentProjectsMenu;
	
	private JMenu projectsMenu    = new JMenu("Projects");
	private JMenu eventGraphsMenu = new JMenu("Event Graphs");
	private JMenu editMenu        = new JMenu(FRAME_DEFAULT_TITLE);
    private JMenu helpMenu        = new JMenu("Help");
	private viskit.Help help;
	private JMenuItem closeProjectMI; // expose to top-level menu via getter

    private final String  FULLPATH     = ViskitStatics.FULL_PATH;
    private final String CLEARPATHFLAG = ViskitStatics.CLEAR_PATH_FLAG;
	
	public static String saveCompileLabelText    = "<html><p align='right'>Save and<br /> Compile </p></html>";
	public static String saveCompileLabelTooltip = "Save file (Ctrl-S) then generate source code and compile Java (Ctrl-J)";
	
    private EventGraphControllerImpl eventGraphController;
	private boolean pathEditable = false;
	private boolean helpWindowBoundsInitialized = false;
	private double  currentZoomFactor = ViskitStatics.DEFAULT_ZOOM;
	
    private int     menuShortcutCtrlKeyMask;
	private int     menuShortcutAltKeyMask;
    private int     projectMenuShortcutKeyMask; // duplicated in AssemblyEditViewFrame
    private int     eventGraphMenuShortcutKeyMask;
    /**
     * Constructor; lays out initial GUI objects
     * @param controller the controller for this frame (MVF)
     */
    public EventGraphViewFrame(mvcController controller)
	{
        super(FRAME_DEFAULT_TITLE);
		LOOK_AND_FEEL = UserPreferencesDialog.getLookAndFeel(); // don't initialize until run time
        initializeMVC(controller); // set up mvc linkages
        initializeUserInterface(); // build widgets
    }

    /** @return the JPanel which is the content of this JFrame */
    public JComponent getContent() {
        return (JComponent) getContentPane();
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    /** @return the current mode--select, add, arc, cancelArc */
    public int getCurrentMode() {
        // Use the button's selected status to figure out what mode
        // we are in.

        if (selectMode.isSelected()) {
            return SELECT_MODE;
        }
        if (arcMode.isSelected()) {
            return ARC_MODE;
        }
        if (cancelArcMode.isSelected()) {
            return CANCEL_ARC_MODE;
        }

        LOG.error("assert false : \"getCurrentMode()\"");
        return 0;
    }

    /**
     * Initialize the MVC connections
     * @param ctrl the controller for this view
     */
    private void initializeMVC(mvcController ctrl) {
        setController(ctrl);
    }

    /**
     * Initialize the user interface
     */
    private void initializeUserInterface() 
	{
        if (eventGraphController == null)
		{
			eventGraphController = (EventGraphControllerImpl) getController();
			eventGraphController.addRecentEventGraphFileListener(new RecentEventGraphFileListener());
			
			menuShortcutCtrlKeyMask       = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			menuShortcutAltKeyMask        = InputEvent.ALT_MASK;
			projectMenuShortcutKeyMask    = menuShortcutCtrlKeyMask;                         //    Projects menu hotkey: <control>
			eventGraphMenuShortcutKeyMask = menuShortcutCtrlKeyMask | InputEvent.SHIFT_MASK; // Event Graph menu hotkey: <control-shift>
		}
		
		projectsMenu    = new JMenu("Projects");
		eventGraphsMenu = new JMenu("Event Graphs");
		editMenu        = new JMenu(FRAME_DEFAULT_TITLE );
		helpMenu        = new JMenu("Help");
		editMenu.setText(FRAME_DEFAULT_TITLE + "   "); // extra wide in order to align menus with tabs
		
        projectsMenu.setMnemonic(KeyEvent.VK_P);
     eventGraphsMenu.setMnemonic(KeyEvent.VK_E);
            editMenu.setMnemonic(KeyEvent.VK_E); // submenu
		    helpMenu.setMnemonic(KeyEvent.VK_H);

        // Layout menus
        buildMenus();

        // Layout of toolbar
        setupToolbar();

        // Set up a eventGraphViewerContent level pane that will be the content pane. This
        // has a border layout, and contains the toolbar on the eventGraphViewerContent and
        // the main splitpane underneath.

        getContent().setLayout(new BorderLayout());
        getContent().add(getToolBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new TabSelectionHandler());

        getContent().add(tabbedPane, BorderLayout.CENTER);
        getContent().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    public EventGraphComponentWrapper getCurrentJGraphEventGraphComponentWrapper()
	{
        JSplitPane splitPane = (JSplitPane) tabbedPane.getSelectedComponent();
        if (splitPane == null) {
            return null;
        }
        JScrollPane scrollPane = (JScrollPane) splitPane.getLeftComponent();
        return (EventGraphComponentWrapper) scrollPane.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent()
	{
        EventGraphComponentWrapper eventGraphComponentWrapper = getCurrentJGraphEventGraphComponentWrapper();
        if (eventGraphComponentWrapper == null || eventGraphComponentWrapper.drawingSplitPane == null) {return null;}
        return eventGraphComponentWrapper.drawingSplitPane.getLeftComponent();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JToolBar toolBar) {
        this.toolBar = toolBar;
    }

    /**
     * @return the openRecentProjectsMenu
     */
    public JMenu getOpenRecentProjectsMenu() {
        return openRecentProjectsMenu;
    }

	/**
	 * @return the eventGraphsMenu
	 */
	public JMenu getFileMenu() {
		return eventGraphsMenu;
	}

	/**
	 * @return the editMenu
	 */
	public JMenu getEditMenu() {
		return editMenu;
	}

	/**
	 * @return the projectsMenu
	 */
	public JMenu getProjectsMenu() {
		return projectsMenu;
	}

	/**
	 * @return the helpMenu
	 */
	public JMenu getHelpMenu() {
		return helpMenu;
	}

	/**
	 * @return the closeProjectMI
	 */
	public JMenuItem getCloseProjectMI() {
		return closeProjectMI;
	}

	/**
	 * @return the currentZoomFactor
	 */
	public double getCurrentZoomFactor() {
		return currentZoomFactor;
	}

	/**
	 * @param currentZoomFactor the currentZoomFactor to set
	 */
	public void setCurrentZoomFactor(double currentZoomFactor) {
		this.currentZoomFactor = currentZoomFactor;
	}

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) 
		{
            EventGraphComponentWrapper eventGraphComponentWrapper = getCurrentJGraphEventGraphComponentWrapper();

            if (eventGraphComponentWrapper == null) {     // last tab has been closed
                setSelectedEventGraphName(null);
                return;
            }

            // NOTE: Although a somewhat good idea, perhaps the user does not
            // wish to have work saved when merely switching between tabs on
            // the EventGraph palette.  However, when switching to the Assembly palette, we
            // will save all EventGraphs that have been modified
//            if (((EventGraphModel)getModel()).isDirty()) {
//                ((EventGraphController)getController()).save();
//            }

            setModel((EventGraphModelImpl) eventGraphComponentWrapper.eventGraphModel);    // hold on locally
            getController().setModel(getModel());  // tell controller

            adjustMenus((EventGraphModel) getModel()); // enable/disable menu items based on new EG

            GraphMetadata graphMetadata = ((EventGraphModel) getModel()).getMetadata();
			
            if (graphMetadata != null)
			{
				if ((graphMetadata.description == null) || graphMetadata.description.trim().isEmpty())
					 graphMetadata.description = ViskitStatics.DEFAULT_DESCRIPTION;
			
                setSelectedEventGraphName       (graphMetadata.name);
                setSelectedEventGraphDescription(graphMetadata.description);
            } 
			else if (viskit.ViskitStatics.debug) 
			{
                LOG.error("error: EventGraphViewFrame graphMetadata is null..");
            }
        }
    }

    private void buildStateParameterSplit(EventGraphComponentWrapper vgcw)
	{
        // State variables area:
        JPanel stateVariablesPanel = new JPanel();
        stateVariablesPanel.setLayout(new BoxLayout(stateVariablesPanel, BoxLayout.Y_AXIS));
        stateVariablesPanel.add(Box.createVerticalStrut(10));

        JPanel eventGraphParametersSubpanel = new JPanel();
        eventGraphParametersSubpanel.setLayout(new BoxLayout(eventGraphParametersSubpanel, BoxLayout.X_AXIS));
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());

        JLabel stateVariableLabel = new JLabel("State Variables");
        stateVariableLabel.setToolTipText("State variables can be modified during event processing");

        eventGraphParametersSubpanel.add(stateVariableLabel);
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        stateVariablesPanel.add(eventGraphParametersSubpanel);

        StateVariablesPanel stateVariablesPanel0 = new StateVariablesPanel(300, 5);
        stateVariablesPanel.add(stateVariablesPanel0);
        stateVariablesPanel.add(Box.createVerticalStrut(10));
        stateVariablesPanel.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for stateVariable adds, deletes and edits and tell it we'll be doing adds and deletes
        stateVariablesPanel0.doAddsAndDeletes(true);
        stateVariablesPanel0.addPlusListener(ActionIntrospector.getAction((EventGraphController) getController(), "newStateVariable"));

        // Introspector can't handle a param to the method....?
        stateVariablesPanel0.addMinusListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).deleteStateVariable((ViskitStateVariable) event.getSource());
            }
        });

        stateVariablesPanel0.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).stateVariableEdit((ViskitStateVariable) event.getSource());
            }
        });

        // Event jGraph parameters area
        JPanel parametersPanel = new JPanel();
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS)); //BorderLayout());
        parametersPanel.add(Box.createVerticalStrut(10));
		
		String usageHint = "To modify this field, use menu item \"Event Graph Editor > Edit Event Graph Properties...\" (Control-Shift-P)";
		
//        JLabel implementsLabel = new JLabel("Implements");
//        implementsLabel.setToolTipText("Event graph superclass (if any)");
//        implementsLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
//        implementsLabel.setToolTipText(usageHint);
//		implementsLabel.setEnabled(false); // TODO callback
//
//        parametersPanel.add(implementsLabel);
//        parametersPanel.add(Box.createVerticalStrut(10));

        JLabel descriptionLabel = new JLabel("Event Graph Description");
        descriptionLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        descriptionLabel.setToolTipText(usageHint);

        parametersPanel.add(descriptionLabel);
        parametersPanel.add(Box.createVerticalStrut(10));

        JTextArea descriptionTextArea = new JTextArea();
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        descriptionTextArea.setToolTipText(usageHint);
		descriptionTextArea.setEnabled(false); // TODO callback

        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        descriptionScrollPane.setMinimumSize(new Dimension(20, 20));

        // This works, you just have to have several lines of typed text to cause the etched scrollbar to appear
        parametersPanel.add(descriptionScrollPane);
        parametersPanel.add(Box.createVerticalStrut(10));
        parametersPanel.setMinimumSize(new Dimension(20, 20));

        eventGraphParametersSubpanel = new JPanel();
        eventGraphParametersSubpanel.setLayout(new BoxLayout(eventGraphParametersSubpanel, BoxLayout.X_AXIS));
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());

        JLabel titleLabel = new JLabel("Event Graph Initialization Parameters");
		titleLabel.setToolTipText("Event Graph instances get initialized when configured in an Assembly");

        eventGraphParametersSubpanel.add(titleLabel);
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        parametersPanel.add(eventGraphParametersSubpanel);

        ParametersPanel parametersPanel0 = new ParametersPanel(300, 5);
        parametersPanel0.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for parameter adds, deletes and edits and tell it we'll be doing adds and deletes
        parametersPanel0.doAddsAndDeletes(false);
        parametersPanel0.addPlusListener(ActionIntrospector.getAction((EventGraphController) getController(), "newSimParameter"));

        // Introspector can't handle a param to the method....?
        parametersPanel0.addMinusListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).deleteSimParameter((ViskitParameter) event.getSource());
            }
        });
        parametersPanel0.addDoubleClickedListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                ((EventGraphController) getController()).simParameterEdit((ViskitParameter) event.getSource());
            }
        });

        parametersPanel.add(parametersPanel0);
        parametersPanel.add(Box.createVerticalStrut(10));

        CodeBlockPanel codeblockPanel = buildCodeBlockPanel();

        JSplitPane stateCodeBlockSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(stateVariablesPanel),
                new JScrollPane(buildCodeBlockComponent(codeblockPanel)));
        stateCodeBlockSplit.setResizeWeight(0.75);
        stateCodeBlockSplit.setMinimumSize(new Dimension(20, 20));

        // Split pane that has description, parameters, state variables and code block.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                parametersPanel,
                stateCodeBlockSplit);
        splitPane.setResizeWeight(0.75);
        splitPane.setMinimumSize(new Dimension(20, 20));

        vgcw.stateParameterSplitPane = splitPane;
        vgcw.parametersPanel = parametersPanel0;
        vgcw.stateVariablesPanel = stateVariablesPanel0;
        vgcw.codeBlockPanel = codeblockPanel;
    }

    private CodeBlockPanel buildCodeBlockPanel()
	{
        CodeBlockPanel codeBlockPanel = new CodeBlockPanel(this, true, "Event Graph Code Block");
        codeBlockPanel.addUpdateListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) e.getSource();
                if (s != null) {
                    ((EventGraphController) getController()).codeBlockEdit(s);
                }
            }
        });
        return codeBlockPanel;
    }

    private JComponent buildCodeBlockComponent(CodeBlockPanel codeBlockPanel)
	{
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel codeBlockLabel = new JLabel("Event Graph Code Block");
        codeBlockLabel.setToolTipText("Code block source runs runs first in the top of the Event's \"do\" method");
        codeBlockLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(codeBlockLabel);
        codeBlockPanel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(codeBlockPanel);
        panel.setBorder(new EmptyBorder(5, 5, 5, 2));
        Dimension d = new Dimension(panel.getPreferredSize());
        d.width = Integer.MAX_VALUE;
        panel.setMaximumSize(d);

        return panel;
    }

    @Override
    public void setSelectedEventGraphName(String s) {
        boolean nullString = !(s != null && s.length() > 0);
        if (!nullString) {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), s);
        }
    }

    public void setSelectedImplementsName(String s) {
        boolean nullString = !(s != null && s.length() > 0);
        if (!nullString) {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), s);
        }
    }

    @Override
    public void setSelectedEventGraphDescription(String description) {
        JSplitPane splitPane = getCurrentJGraphEventGraphComponentWrapper().stateParameterSplitPane;
        JPanel panel = (JPanel) splitPane.getTopComponent();
        Component[] components = panel.getComponents();
        for (Component c : components) {
            if (c instanceof JScrollPane) {
                c = ((JScrollPane) c).getViewport().getComponent(0);
                ((JTextArea) c).setText(description);
            }
        }
    }

    int nextTabIndex = 0;

    @Override
    public void addTab(EventGraphModel eventGraphModel) // TODO set type EventGraphModel
	{
        JGraphVisualModel jGraphVisualModel = new JGraphVisualModel();
        EventGraphComponentWrapper graphPane = new EventGraphComponentWrapper(jGraphVisualModel, this);
        jGraphVisualModel.setjGraph(graphPane);
        graphPane.eventGraphModel = eventGraphModel;

        buildStateParameterSplit(graphPane);

        // Split pane with the canvas on the left and a split pane with state variables and parameters on the right.
        JScrollPane scrollPane = new JScrollPane(graphPane);

        graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, graphPane.stateParameterSplitPane);

        // This is the key to getting the jgraph half to come up appropriately
        // wide by giving the left component (JGraph side) most of the usable
        // extra space in this SplitPlane -> 75%
        graphPane.drawingSplitPane.setResizeWeight(0.75);
        graphPane.drawingSplitPane.setOneTouchExpandable(true); 

        graphPane.addMouseListener(new vCursorHandler());
        try {
            graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
        } catch (TooManyListenersException tmle) {
            LogUtilities.getLogger(EventGraphViewFrame.class).error(tmle);
        }
        graphPane.setToolTipText(eventGraphModel.getMetadata().description);

        // the view holds only one eventGraphModel, so it gets overwritten with each tab
        // but this call serves also to register the view with the passed eventGraphModel
        // by virtue of calling stateChanged()
        tabbedPane.add("EventGraph" + nextTabIndex, graphPane.drawingSplitPane);
        tabbedPane.setToolTipTextAt(tabbedPane.getTabCount()-1, ViskitStatics.TOOLTIP_EVENTGRAPH_ASSEMBLY_EDITOR_TAB_COLORS); // TODO eventGraphModel.getMetadata().description);

        // Bring the JGraph component to front. Also, allows models their own
        // canvas to draw to prevent a NPE
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane); // bring to front
		nextTabIndex++; // increment in preparation for next tab

        // Now expose the EventGraph toolbar
        Runnable r = new Runnable() {
            @Override
            public void run() {
                getToolBar().setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);
    }
	
	public JSplitPane getSelectedPane ()
	{
		return ((JSplitPane) tabbedPane.getSelectedComponent());
	}

    @Override
    public void deleteTab(EventGraphModel mod)
	{
        for (Component c : tabbedPane.getComponents()) {
            JSplitPane jsplt = (JSplitPane) c;
            JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
            EventGraphComponentWrapper vgcw = (EventGraphComponentWrapper) jsp.getViewport().getComponent(0);
            if (vgcw.eventGraphModel == mod) {
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
		nextTabIndex--; // decrement in preparation for next tab
		if (nextTabIndex < 0)
			nextTabIndex = 0;
    }

    public boolean hasOpenEventGraphs() {
		return (tabbedPane != null) && (tabbedPane.getTabCount() > 0) && (nextTabIndex > 0);
    }

    public boolean hasActiveEventGraph() {
		return (ViskitGlobals.instance() != null) &&
			   (ViskitGlobals.instance().getActiveEventGraphModel()!= null);
    }

    public boolean hasOpenAssemblies() {
		return (ViskitGlobals.instance() != null) &&
			   (ViskitGlobals.instance().getAssemblyEditViewFrame() != null) &&
			   (ViskitGlobals.instance().getAssemblyEditViewFrame().hasOpenAssemblies());
    }

    public boolean hasActiveAssembly() {
		return (ViskitGlobals.instance() != null) &&
			   (ViskitGlobals.instance().getActiveEventGraphModel()!= null);
    }

    @Override
    public EventGraphModel[] getOpenModels() {
        Component[] ca = tabbedPane.getComponents();
        EventGraphModel[] vm = new EventGraphModel[ca.length];
        for (int i = 0; i < vm.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
            EventGraphComponentWrapper vgcw = (EventGraphComponentWrapper) jsp.getViewport().getComponent(0);
            vm[i] = vgcw.eventGraphModel;
        }
        return vm;
    }

    @Override
    public String addParameterDialog() 
	{

        if (ParameterDialog.showDialog(ViskitGlobals.instance().getViskitApplicationFrame(), null)) 
		{
            ((EventGraphController) getController()).buildNewSimParameter( // blocks here
				   ParameterDialog.newName,     
                   ParameterDialog.newType,
                   "TODO new value here",
                   ParameterDialog.newDescription);
            return ParameterDialog.newName;
        }
        return null;
    }

    @Override
    public String addStateVariableDialog() 
	{
        if (StateVariableDialog.showDialog(ViskitGlobals.instance().getViskitApplicationFrame(), null))  // blocks here
		{
            ((EventGraphController) getController()).buildNewStateVariable (
					StateVariableDialog.newName,
                    StateVariableDialog.newType,
					StateVariableDialog.newImplicit,
					StateVariableDialog.newValue,
                    StateVariableDialog.newDescription);
            return StateVariableDialog.newName;
        }
        return null;
    }

    /**
     * Do menu layout work here
     * @param eventGraphModel the current eventGraphModel of our EG view
     */
    private void adjustMenus(EventGraphModel eventGraphModel)
	{
		// TODO select eventGraphModel tab
        buildMenus();
    }

    class RecentEventGraphFileListener implements mvcRecentFileListener
	{
        @Override
        public void recentFileListChanged()
		{
            EventGraphControllerImpl eventGraphController = (EventGraphControllerImpl) getController();
			if ((eventGraphController == null) || (openRecentEventGraphMenu == null))
				return;
            List<File>  openEventGraphFileList = eventGraphController.getOpenEventGraphFileList();
            Set<File>  recentEventGraphFileSet = eventGraphController.getRecentEventGraphFileSet();
            openRecentEventGraphMenu.removeAll();
            for (File eventGraphFile : recentEventGraphFileSet)
			{
                if (!eventGraphFile.exists() || openEventGraphFileList.contains(eventGraphFile))
				{
                    continue; // skip this entry, don't add recent menu item if file is open
                }
                String fileNameOnly = eventGraphFile.getName();
                Action menuItemAction = new ParameterizedEventGraphAction(fileNameOnly);
                menuItemAction.putValue(FULLPATH, eventGraphFile);
                JMenuItem menuItem = new JMenuItem(menuItemAction);
                menuItem.setToolTipText(eventGraphFile.getPath());
                openRecentEventGraphMenu.add(menuItem);
            }
            if (openRecentEventGraphMenu.getItemCount() > 0) // fileSet.size()
			{
                openRecentEventGraphMenu.add(new JSeparator());
                Action clearMenuItemAction = new ParameterizedEventGraphAction("clear"); // TODO
                clearMenuItemAction.putValue(FULLPATH, CLEARPATHFLAG);  // flag
                JMenuItem clearRecentEventGraphMenuItem = new JMenuItem(clearMenuItemAction);
                clearRecentEventGraphMenuItem.setToolTipText("Clear this list");
                openRecentEventGraphMenu.add(clearRecentEventGraphMenuItem);
            }
        }
    }

    class ParameterizedEventGraphAction extends javax.swing.AbstractAction
	{
		private String action;

        ParameterizedEventGraphAction(String s) {
            super(s);
			action = s;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            EventGraphController eventGraphController = (EventGraphController) getController();

            File eventGraphFile;
            Object obj = getValue(ViskitStatics.FULL_PATH);
            if (obj instanceof String)
                eventGraphFile = new File((String) obj);
            else
                eventGraphFile = (File) obj;

            if (eventGraphFile.getPath().equals(CLEARPATHFLAG) || action.equals("clear"))
			{
                eventGraphController.clearRecentEventGraphFileSet();
            }
			else 
			{
                eventGraphController.openRecentEventGraph(eventGraphFile);
				ViskitGlobals.instance().getViskitApplicationFrame().displayEventGraphEditorTab();
				
				// now select tab with recent event graph, if appropriate
				if (action.endsWith(".xml"))
				{
					boolean found = false;
					int selectedTab = tabbedPane.getSelectedIndex(); // save current selection
		
					for (Component c : tabbedPane.getComponents())
					{
						// This will fire a call to stateChanged() which also sets the current eventGraphModel
						tabbedPane.setSelectedComponent(c);
						if (((EventGraphModel) getModel()).getMetadata().name.equals(action.substring(0,action.indexOf(".xml"))))
						{
							found = true; // stay on this tab
							break;
						}
					}
					// TODO check if this tab isn't found even if it was selected via recent list? is this code block all out of order, checking before it gets loaded?
					// Restore active tab and eventGraphModel by virtue of firing a call to stateChanged()
					if (!found)
						tabbedPane.setSelectedIndex(selectedTab);
					// TODO similar functionality for assembly tabs, once they are working
				}
            }
			buildMenus (); // reset, may have switched panes
        }
    }

    public void buildMenus() 
	{
		// ===================================================
        // Initialize
        projectsMenu.removeAll(); // reset
		if (openRecentEventGraphMenu == null)
		{
			openRecentEventGraphMenu = buildMenu("Recent Event Graph"); // don't wipe it out if already there!
			openRecentEventGraphMenu.setToolTipText("open Recent Event Graph");
			openRecentEventGraphMenu.setMnemonic(KeyEvent.VK_R);
			// no accelerator hotkey for JMenu
		}
		eventGraphController.updateEventGraphFileLists();
		
		// ===================================================
        // Set up Projects menu

		boolean isProjectOpen = false;
		if (ViskitGlobals.instance().getCurrentViskitProject() != null) // viskit may be starting with no project open
			isProjectOpen = ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen();
		if (ViskitGlobals.instance().getViskitApplicationFrame() != null)
			ViskitGlobals.instance().getViskitApplicationFrame().refreshCloseProjectMI (isProjectOpen);
		
        projectsMenu.add(buildMenuItem(eventGraphController, AssemblyControllerImpl.NEWPROJECT_METHOD, "New Project", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, projectMenuShortcutKeyMask), true));
		projectsMenu.add(buildMenuItem(this, OPENPROJECT_METHOD, "Open Project", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, projectMenuShortcutKeyMask), true));
		if (openRecentProjectsMenu == null) // don't wipe it out if already there!
		{
			openRecentProjectsMenu = buildMenu ("Recent Project");
			openRecentProjectsMenu.setToolTipText("Open Recent Project");
			openRecentProjectsMenu.setMnemonic(KeyEvent.VK_R);
			// no accelerator hotkey for JMenu
		}
		AssemblyControllerImpl assemblyController = ViskitGlobals.instance().getAssemblyController();
		if (assemblyController != null)
			assemblyController.updateProjectFileLists();
		// if active, don't list current project on Recent Projects list
		if (isProjectOpen && (openRecentProjectsMenu != null))
		{
			for (int index = 0; index < openRecentProjectsMenu.getItemCount(); index++)
			{
				if (openRecentProjectsMenu.getItem(index).getText().equals(ViskitGlobals.instance().getCurrentViskitProject().getProjectName()))
				{
					openRecentProjectsMenu.remove(index);
					break;
				}
			}
		}
		openRecentProjectsMenu.setEnabled(openRecentProjectsMenu.getItemCount() > 0);
		projectsMenu.add(openRecentProjectsMenu);

		boolean eventGraphVisible = hasOpenEventGraphs() && ViskitGlobals.instance().getViskitApplicationFrame().isEventGraphEditorTabSelected();
		boolean   assemblyVisible = hasOpenAssemblies()  && ViskitGlobals.instance().getViskitApplicationFrame().isAssemblyEditorTabSelected();

        JMenuItem copyProjectMI               = buildMenuItem(this, COPY_PROJECT_METHOD,                "Copy Project",                   KeyEvent.VK_C, null /* dangerous operation, no hotkey */, isProjectOpen);
		copyProjectMI.setEnabled (isProjectOpen);
		copyProjectMI.setToolTipText("Copy project to new project directory, re-open project");
		projectsMenu.add(copyProjectMI);
		
        projectsMenu.addSeparator();
		
		// ensure selected before allowing deletion
        JMenuItem deleteProjectMI               = buildMenuItem(this, DELETE_PROJECT_METHOD,                "Delete Project",   KeyEvent.VK_D, null /* dangerous operation, no hotkey */, isProjectOpen);
		deleteProjectMI.setEnabled (isProjectOpen && false); // TODO
		deleteProjectMI.setToolTipText("TODO future capability");
		projectsMenu.add(deleteProjectMI);

		// ensure selected before allowing deletion
        JMenuItem deleteEventGraphFromProjectMI = buildMenuItem(this, DELETE_EVENT_GRAPH_FROM_PROJECT_METHOD, "Delete Event Graph from Project",   KeyEvent.VK_D, null /* dangerous operation, no hotkey */, (eventGraphVisible && hasActiveEventGraph()));
		deleteEventGraphFromProjectMI.setEnabled (isProjectOpen && eventGraphVisible && false); // TODO
		deleteEventGraphFromProjectMI.setToolTipText("TODO future capability");
		projectsMenu.add(deleteEventGraphFromProjectMI);

		// ensure selected before allowing deletion
        JMenuItem deleteAssemblyFromProjectMI   = buildMenuItem(this, DELETE_ASSEMBLY_FROM_PROJECT_METHOD,   "Delete Assembly from Project",      KeyEvent.VK_D, null /* dangerous operation, no hotkey */, (assemblyVisible && hasActiveAssembly()));
		deleteAssemblyFromProjectMI.setEnabled (isProjectOpen && assemblyVisible && false); // TODO
		deleteAssemblyFromProjectMI.setToolTipText("TODO future capability");
		projectsMenu.add(deleteAssemblyFromProjectMI);
		
        projectsMenu.addSeparator();

        JMenuItem projectSettingsMI = buildMenuItem(this, EDIT_PROJECT_PROPERTIES_METHOD, "Edit Project Properties",      KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_P, projectMenuShortcutKeyMask), isProjectOpen);
		projectSettingsMI.setEnabled (isProjectOpen);
		projectsMenu.add(projectSettingsMI);
		
		JMenuItem mailZippedProjectFilesMI = buildMenuItem(eventGraphController, AssemblyControllerImpl.MAIL_ZIPPED_PROJECT_FILES_METHOD, "Mail Zipped Project Files", KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_M, projectMenuShortcutKeyMask), true);
        mailZippedProjectFilesMI.setEnabled (isProjectOpen);
		projectsMenu.add(mailZippedProjectFilesMI);
        
		closeProjectMI = buildMenuItem(this, AssemblyEditViewFrame.CLOSE_PROJECT_METHOD, "Close Project", KeyEvent.VK_W, KeyStroke.getKeyStroke(KeyEvent.VK_W, projectMenuShortcutKeyMask), isProjectOpen);
		closeProjectMI.setEnabled (isProjectOpen);
		projectsMenu.add(closeProjectMI);
		
		// ===================================================
		// Set up Event Graphs menu
		eventGraphsMenu.setEnabled(true); // always on
        eventGraphsMenu.removeAll();      // reset
		
        eventGraphsMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.NEWEVENTGRAPH_METHOD, "New Event Graph",  KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, eventGraphMenuShortcutKeyMask), isProjectOpen));
        eventGraphsMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.OPEN_METHOD,          "Open Event Graph", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, eventGraphMenuShortcutKeyMask), isProjectOpen));
		openRecentEventGraphMenu.setEnabled(isProjectOpen && eventGraphController.getRecentEventGraphFileSet().size() > 0);
		eventGraphsMenu.add(openRecentEventGraphMenu);
        eventGraphsMenu.addSeparator();
		
        JMenuItem saveEventGraphMI        = buildMenuItem(eventGraphController, EventGraphControllerImpl.SAVE_METHOD,         "Save Event Graph",         KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, eventGraphMenuShortcutKeyMask), eventGraphVisible);
        eventGraphsMenu.add(saveEventGraphMI);
        JMenuItem saveAsEventGraphMI      = buildMenuItem(eventGraphController, EventGraphControllerImpl.SAVEAS_METHOD,       "Save Event Graph as...",   KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_S, eventGraphMenuShortcutKeyMask), eventGraphVisible);
        eventGraphsMenu.add(saveAsEventGraphMI);
        JMenuItem saveEventGraphDiagramMI = buildMenuItem(eventGraphController, EventGraphControllerImpl.IMAGECAPTURE_METHOD, "Save Event Graph Diagram", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, eventGraphMenuShortcutKeyMask), eventGraphVisible);
		eventGraphsMenu.add(saveEventGraphDiagramMI);
		JMenuItem closeEventGraphMI       = buildMenuItem(eventGraphController, EventGraphControllerImpl.CLOSE_METHOD,        "Close Event Graph",        KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_W, eventGraphMenuShortcutKeyMask), eventGraphVisible);
        eventGraphsMenu.add(closeEventGraphMI);
		JMenuItem closeAllEventGraphsMI   = buildMenuItem(eventGraphController, EventGraphControllerImpl.CLOSEALL_METHOD,     "Close All Event Graphs",   KeyEvent.VK_L, null, eventGraphVisible);
        eventGraphsMenu.add(closeAllEventGraphsMI);

		// ===================================================
        // Set up edit menu
		// only turn top edit menu on if Event Graphs are present and Event Graph Editor tab is selected
		editMenu.setEnabled(eventGraphVisible && ViskitGlobals.instance().getViskitApplicationFrame().isEventGraphEditorTabSelected());
        editMenu.removeAll(); // reset
		
          editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.UNDO_METHOD, "Undo Edit", KeyEvent.VK_Z, KeyStroke.getKeyStroke(KeyEvent.VK_Z, eventGraphMenuShortcutKeyMask), eventGraphVisible));
          editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.REDO_METHOD, "Redo Edit", KeyEvent.VK_Y, KeyStroke.getKeyStroke(KeyEvent.VK_Y, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.UNDO_METHOD).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.REDO_METHOD).setEnabled(false);

        editMenu.addSeparator();
        // the next four are disabled until something is selected
        editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.CUT_METHOD,    "Cut Events",       KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_X, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.getItem(editMenu.getItemCount()-1).setToolTipText(EventGraphControllerImpl.CUT_METHOD + " is not supported in Viskit.");
        editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.COPY_METHOD,   "Copy Events",      KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.PASTE_METHOD,  "Paste Events",     KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_V, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.REMOVE_METHOD, "Delete Selection", KeyEvent.VK_DELETE, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, eventGraphMenuShortcutKeyMask), eventGraphVisible));

        // Initialization: these start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.CUT_METHOD).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.COPY_METHOD).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.PASTE_METHOD).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.REMOVE_METHOD).setEnabled(false);

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.NEWEVENTNODE_METHOD,          "Add Event Node",              KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.NEWSIMPARAMETER_METHOD,       "Add Simulation Parameter...", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.NEWSTATEVARIABLE_METHOD,      "Add State Variable...",       KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_V, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.NEWSELFSCHEDULINGEDGE_METHOD, "Add Self-Referential Scheduling Edge...", null, null, eventGraphVisible));
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.NEWSELFCANCELLINGEDGE_METHOD, "Add Self-Referential Cancelling Edge...",  null, null, eventGraphVisible));

        // Thess start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.NEWSELFSCHEDULINGEDGE_METHOD).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, EventGraphControllerImpl.NEWSELFCANCELLINGEDGE_METHOD).setEnabled(false);

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.SHOWXML_METHOD,           "View Saved XML",                KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_X, eventGraphMenuShortcutKeyMask), eventGraphVisible));
        editMenu.add(buildMenuItem(  eventGraphController, EventGraphControllerImpl.JAVASOURCE_METHOD,        "Generate, Compile Java Source", KeyEvent.VK_J, KeyStroke.getKeyStroke(KeyEvent.VK_J, eventGraphMenuShortcutKeyMask), eventGraphVisible));
//        JMenuItem saveEventGraphDiagramMI2 = 
//				     buildMenuItem(  eventGraphController, EventGraphControllerImpl.IMAGECAPTURE_METHOD,      "Save Event Graph Diagram",      KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, eventGraphMenuShortcutKeyMask), eventGraphVisible);
		editMenu.add(saveEventGraphDiagramMI); // shown in two places

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(eventGraphController, EventGraphControllerImpl.EDIT_EVENT_GRAPH_METADATA_METHOD, "Edit Event Graph Properties...",KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_P, eventGraphMenuShortcutKeyMask), eventGraphVisible));

		// ===================================================
        // Create a new menu bar and add the created menus
		
		if (myMenuBar == null)
		{
			myMenuBar = new JMenuBar();
			myMenuBar.add(eventGraphsMenu);
			myMenuBar.add(editMenu);
			
			try  // previously this block also existed in AssemblyViewFrame, refactored.  TODO still seeing 2 icon tabs within Help display.
			{
				help = new viskit.Help(this);
				checkHelpWindowBounds (); // likely too early during initialization sequence to take effect, parent windows not yet available
				ViskitGlobals.instance().setHelp(help);

				helpMenu.add(buildMenuItem(help, Help.SHOW_HELP_CONTENTS_METHOD,       "Help Contents", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0 /* no mask */), true));
				
				helpMenu.add(buildMenuItem(help, Help.SHOW_HELP_SEARCH_METHOD,         "Search Help",   KeyEvent.VK_S, null, true));

//				helpMenu.add(buildMenuItem(help, Help.SHOW_HELP_TUTORIAL_METHOD,       "Tutorial", KeyEvent.VK_T, null, true)); // TODO
				
				helpMenu.add(buildMenuItem(help, Help.SHOW_HELP_ABOUT_ASSEMBLY_METHOD, "About...", KeyEvent.VK_A, null, true));
				
				myMenuBar.add(helpMenu);
			}
			catch (Exception e)
			{
				LogUtilities.getLogger(EventGraphViewFrame.class).error("Error creating EventGraphViewFrame help menu: ", e);
			}
		}
		else if (!helpWindowBoundsInitialized)
		         checkHelpWindowBounds ();
    }
	
	/** Set proper initial Help window size and location */
	@SuppressWarnings("UnnecessaryReturnStatement")
	public void checkHelpWindowBounds ()
	{
		if  (helpWindowBoundsInitialized)
		{
			return; // no action required
		}
		else if ((this.getBounds().height > 0)  && (this.getBounds().getBounds().width > 0))
		{
			help.mainFrameLocated(this.getBounds());
			helpWindowBoundsInitialized = true;
		}
		else if (!helpWindowBoundsInitialized && (ViskitGlobals.instance().getViskitApplicationFrame() != null))
		{
			Rectangle applicationFrameBounds = ViskitGlobals.instance().getViskitApplicationFrame().getBounds();
			if ((applicationFrameBounds.height > 0)  && (applicationFrameBounds.getBounds().width > 0))
			{
				help.mainFrameLocated(applicationFrameBounds.getBounds());
				helpWindowBoundsInitialized = true;
			}
		}
	}

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel, boolean enabled)
	{
        Action action = ActionIntrospector.getAction(source, method);
		if (action == null)
		{
			LOG.error(source.toString() + " buildMenuItem() unable to find method \"" + method + "()\" for menu item \"" + name + "\"");
		}
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
            ActionUtilities.decorateAction(action, map);
        }
		JMenuItem menuItem = ActionUtilities.createMenuItem(action);
		menuItem.setEnabled (enabled);
        return menuItem;
    }

    private JMenu buildMenu(String name) {
        return new JMenu(name);
    }

    private JToggleButton makeJToggleButton(Action a, String icPath, String tt) {
        JToggleButton jtb;
        if (a != null) {
            jtb = new JToggleButton(a);
        } else {
            jtb = new JToggleButton();
        }
        return (JToggleButton) buttonCommon(jtb, icPath, tt);
    }

    private JButton makeButton(Action action, String iconPath, String toolTip)
	{
        JButton button;
        if (action != null) {
            button = new JButton(action);
        } else {
            button = new JButton();
        }
        return (JButton) buttonCommon(button, iconPath, toolTip);
    }

    private AbstractButton buttonCommon(AbstractButton button, String iconPath, String toolTip)
	{
		ImageIcon imageIcon = new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource(iconPath));
		if (imageIcon.getImage() != null)
		{
			button.setIcon(imageIcon);
		}
		else
		{
			LOG.error ("Illegal icon path, no image found: " + iconPath);
		}
        button.setToolTipText(toolTip);
        button.setBorder(BorderFactory.createEtchedBorder());
        button.setText(null);
        return button;
    }

    private JLabel makeJLabel(String imageIconPath, String tooltip) 
	{
        JLabel newLabel = new JLabel(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource(imageIconPath)));
        newLabel.setToolTipText(tooltip);
        return newLabel;
    }

    private void setupToolbar()
	{
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        // Buttons for what mode we are in

        addEvent = makeJLabel("viskit/images/eventNode.png",
                "Drag a new SimEntity onto canvas to add new events to the event graph");
        addEvent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        addEvent.setIcon(new EventNodeIcon());

        addSelfReferentialEdge = makeJLabel("viskit/images/selfReferentialArc.png",
                "Drag onto an existing SimEntity node to add a self-referential scheduling edge");
        addSelfReferentialEdge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        addSelfReferentialCancellingEdge = makeJLabel("viskit/images/selfCancelArc.png",
                "Drag onto an existing SimEntity node to add a self-referential cancelling edge");
        addSelfReferentialCancellingEdge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        selectMode = makeJToggleButton(null, "viskit/images/selectNode.png",
                "Select a SimEntity node or scheduling/cancelling arc on the graph");

        arcMode = makeJToggleButton(null, "viskit/images/schedulingArc.png",
                "Scheduling edge connection between SimEntity nodes (click center box on source node, drag arrow to target node)");
        arcMode.setIcon(new SchedulingArcIcon());

        cancelArcMode = makeJToggleButton(null, "viskit/images/cancellationArc.png",
                "Cancelling edge connection between SimEntity nodes (click center box on source node, drag arrow to target node)");
        cancelArcMode.setIcon(new CancellationArcIcon());

        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(arcMode);
        modeButtonGroup.add(cancelArcMode);

        JButton zoomInButton = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in on the assembly");

        JButton zoomOutButton = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out on the assembly");

        JButton zoomResetButton = makeButton(null, "viskit/images/Stop24.gif",
                "Zoom reset to default");

        JButton saveButton = makeButton(null, "viskit/images/save.png",
                "Save and compile event graph (Ctrl-S)");
		saveButton.setSize(new Dimension (24, 24));

        // Make selection mode the default mode
        selectMode.setSelected(true);

        getToolBar().add(new JLabel("Add"));
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addEvent);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addSelfReferentialEdge);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addSelfReferentialCancellingEdge);

        getToolBar().addSeparator(new Dimension(24, 24));

        getToolBar().add(new JLabel("Mode"));
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(selectMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(arcMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(cancelArcMode);

        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(new JLabel("Zoom"));
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomInButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOutButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomResetButton);
        getToolBar().addSeparator(new Dimension(24, 24));

		// right aligned
		JLabel saveLabel = new JLabel(saveCompileLabelText);
		saveLabel.setToolTipText(     saveCompileLabelTooltip);
		saveLabel.setHorizontalAlignment(JButton.RIGHT);
        getToolBar().add(saveLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
		saveButton.setHorizontalAlignment(JButton.RIGHT);
        getToolBar().add(saveButton);
        getToolBar().addSeparator(new Dimension(5, 24));

        // Let the opening of Event Graphs make this visible
        getToolBar().setVisible(false);

        zoomInButton.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent e)
			{
				currentZoomFactor = getCurrentJGraphEventGraphComponentWrapper().getScale() + ViskitStatics.DEFAULT_ZOOM_INCREMENT;
                getCurrentJGraphEventGraphComponentWrapper().setScale(currentZoomFactor);
            }
        });
        zoomOutButton.addActionListener(new ActionListener()
		{
            @Override
            public void actionPerformed(ActionEvent e)
			{
				currentZoomFactor = Math.max(getCurrentJGraphEventGraphComponentWrapper().getScale() - ViskitStatics.DEFAULT_ZOOM_INCREMENT, ViskitStatics.DEFAULT_ZOOM_INCREMENT); // no smaller than increment value, avoid zero/negative scaling
                getCurrentJGraphEventGraphComponentWrapper().setScale(currentZoomFactor);
            }
        });
        zoomResetButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) 
			{
				currentZoomFactor = ViskitStatics.DEFAULT_ZOOM;
                getCurrentJGraphEventGraphComponentWrapper().setScale(ViskitStatics.DEFAULT_ZOOM);
            }
        });
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
			   
				EventGraphController eventGraphController = (EventGraphController) getController();
				eventGraphController.save();
            }
        });

        TransferHandler th = new TransferHandler("text");
        DragMouseAdapter dma = new DragMouseAdapter();
        addEvent.setTransferHandler(th);
        addEvent.addMouseListener(dma);
        addSelfReferentialEdge.setTransferHandler(th);
        addSelfReferentialEdge.addMouseListener(dma);
        addSelfReferentialCancellingEdge.setTransferHandler(th);
        addSelfReferentialCancellingEdge.addMouseListener(dma);

        // These buttons perform operations that are internal to our view class, and therefore their operations are
        // not under control of the application controller (EventGraphControllerImpl.java).  Small, simple anonymous inner classes
        // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        selectMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentJGraphEventGraphComponentWrapper().setPortsVisible(false);
            }
        });
        arcMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentJGraphEventGraphComponentWrapper().setPortsVisible(true);
            }
        });
        cancelArcMode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentJGraphEventGraphComponentWrapper().setPortsVisible(true);
            }
        });

    }

    /** Changes the background color of Event Graph tabs depending on eventGraphModel.isDirty()
	 * status to give the user an indication of a good/bad save &amp; compile
     * operation.  Of note is that the default L&amp;F on must be selected for
     * Windows machines, else no color will be visible.  On Macs, the platform
     * L&amp;F works best.
     */
    public void toggleEventGraphStatusIndicators()
	{
        int selectedTab = tabbedPane.getSelectedIndex(); // save current selection

        for (Component c : tabbedPane.getComponents())
		{
            // This will fire a call to stateChanged() which also sets the current eventGraphModel
            tabbedPane.setSelectedComponent(c);

            if (((EventGraphModel) getModel()).isDirty())
			{
                tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.RED.brighter());

                if (LOOK_AND_FEEL != null && !LOOK_AND_FEEL.isEmpty() && LOOK_AND_FEEL.toLowerCase().equals("default"))
                    tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.RED.darker());
				
				// TODO save button
            } 
			else
			{
                tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.GREEN.brighter());

                if (LOOK_AND_FEEL != null && !LOOK_AND_FEEL.isEmpty() && LOOK_AND_FEEL.toLowerCase().equals("default"))
                    tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.GREEN.darker());
				
				// TODO save button
            }
        }
        // Restore active tab and eventGraphModel by virtue of firing a call to stateChanged()
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
            arc = new Cursor(Cursor.CROSSHAIR_CURSOR);
            Image img = new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/canArcCursor.png")).getImage();

            // Check if we should size the cursor
            Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(0, 0);
            if (d.width != 0 && d.height != 0 && ViskitStatics.OPERATING_SYSTEM.contains("Windows")) {

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

        // TODO: The cursors don't always set unless it's been moved up to the
        // frame's title bar.  Mac OS X 10.10.3 & JDK 1.8.0_45
        @Override
        public void mouseEntered(MouseEvent e) {
            switch (getCurrentMode()) {
                case ARC_MODE:
                    getCurrentJGraphEventGraphComponentWrapper().setCursor(arc);
                    break;
                case CANCEL_ARC_MODE:
                    getCurrentJGraphEventGraphComponentWrapper().setCursor(cancel);
                    break;
                default:
                    getCurrentJGraphEventGraphComponentWrapper().setCursor(select);
            }
        }
    }

    final static int NODE_DRAG = 0;
    final static int SELF_REFERENTIAL_EDGE_DRAG = 1;
    final static int SELF_REFERENTIAL_CANCELLING_EDGE_DRAG = 2;
    private int dragger;

    /** Class to support dragging and dropping on the jGraph pallette */
    class DragMouseAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent mouseEvent)
		{
            JComponent component = (JComponent) mouseEvent.getSource();
            if (component == EventGraphViewFrame.this.addSelfReferentialEdge)
			{
                dragger = SELF_REFERENTIAL_EDGE_DRAG;
            } 
			else if (component == EventGraphViewFrame.this.addSelfReferentialCancellingEdge)
			{
                dragger = SELF_REFERENTIAL_CANCELLING_EDGE_DRAG;
            } 
			else
			{
                dragger = NODE_DRAG;
            }
            TransferHandler handler = component.getTransferHandler();
            handler.exportAsDrag(component, mouseEvent, TransferHandler.COPY);
        }
    }

    /** Class to facilitate dragging new nodes, or self-referential edges onto nodes on the jGraph pallette */
    class vDropTargetAdapter extends DropTargetAdapter 
	{
        @Override
        public void dragOver(DropTargetDragEvent e) {

            // NOTE: this action is very critical in getting JGraph 5.14 to signal the drop method
            e.acceptDrag(e.getDropAction());
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            Point p = e.getLocation();  // subtract the size of the label

            // get the node in question from the jGraph
            Object o = getCurrentJGraphEventGraphComponentWrapper().getViskitElementAt(p);

            if (dragger == NODE_DRAG) {
                Point pp = new Point(
                        p.x - addEvent.getWidth(),
                        p.y - addEvent.getHeight());
                ((EventGraphController) getController()).buildNewEventNode(pp);
            } 
			else if (dragger == SELF_REFERENTIAL_CANCELLING_EDGE_DRAG) 
			{
                if (o != null && o instanceof EventNode) {
                    EventNode en = (EventNode) o;
                    // We're making a self-referential arc
                    ((EventGraphController) getController()).buildNewCancellingArc(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                }
            } 
			else 
			{
                if (o != null && o instanceof EventNode)
				{
                    EventNode en = (EventNode) o;
                    // We're making a self-referential arc
                    ((EventGraphController) getController()).buildNewSchedulingArc(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                }
            }
        }
    }
    private JFileChooser eventGraphFileChooser;

    private void buildOpenSaveEventGraphsChooser()
	{
		if ((eventGraphFileChooser == null) || // remember previous directory (likely package directory), if used already
			!(eventGraphFileChooser.getCurrentDirectory().getPath().contains(ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDirectory().getPath()))) // project may have changed
		{
			eventGraphFileChooser = new JFileChooser();
			
			// Try to open in the current project directory for EventGraphs
			if (ViskitGlobals.instance().getCurrentViskitProject() != null) 
			{
				eventGraphFileChooser.setCurrentDirectory (ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDirectory());
			} 
			else 
			{
				eventGraphFileChooser.setCurrentDirectory (new File(ViskitProject.MY_VISKIT_PROJECTS_DIR + File.separator + ViskitProject.EVENTGRAPHS_DIRECTORY_NAME));
			}
			eventGraphFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			eventGraphFileChooser.setDialogTitle("Open Event Graph File(s)");

			eventGraphFileChooser.addChoosableFileFilter(new EventGraphFileFilter(
					new String[] {"xml", "png"}));

			// Bug fix: 1249
			eventGraphFileChooser.setMultiSelectionEnabled(true);
			eventGraphFileChooser.setFileHidingEnabled(true);
			eventGraphFileChooser.setAcceptAllFileFilterUsed(false);
		}
    }

    @Override
    public File[] openFilesAsk()
	{
		buildOpenSaveEventGraphsChooser();

        int returnValue = eventGraphFileChooser.showOpenDialog(this);
        return (returnValue == JFileChooser.APPROVE_OPTION) ? eventGraphFileChooser.getSelectedFiles() : null;
    }

    public final static String OPENPROJECT_METHOD = "openProject"; // must match following method name.  Not possible to accomplish this programmatically.
    /** Open an already existing Viskit Project.  Called via reflection from the Actions library. */
    public void openProject () // method name must exactly match preceding string value
	{
        ViskitGlobals.instance().getAssemblyEditViewFrame().openProject();
    }

    /** Close the current project. */
    public void closeProject ()
	{
        ViskitGlobals.instance().getAssemblyEditViewFrame().closeProject();
    }
	
    /** Edit the current project properties. */
    public void editProjectProperties (boolean pathEditable)
	{
		this.pathEditable = pathEditable;
		editProjectProperties ();
	}
	
    public final static String EDIT_PROJECT_PROPERTIES_METHOD = "editProjectProperties"; // must match following method name.  Not possible to accomplish this programmatically.
    /** Edit current project settings. */
    public void editProjectProperties () // method name must exactly match preceding string value
	{
        ViskitConfiguration viskitConfiguration = ViskitConfiguration.instance();
		
		String projectFullPath;
		if ((ViskitGlobals.instance().getCurrentViskitProject() != null) &&
			(ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory() != null))
			 projectFullPath = ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getPath();
		else projectFullPath = viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PATH_KEY); // starting point;
		
        GraphMetadata graphMetadata = new GraphMetadata (                               // must match order of constructor
				viskitConfiguration.getValue(ViskitConfiguration.PROJECT_NAME_KEY),     // name
				"", // packageName not used by project, TODO remember latest value?
				viskitConfiguration.getValue(ViskitConfiguration.PROJECT_AUTHOR_KEY),   // author
				viskitConfiguration.getValue(ViskitConfiguration.PROJECT_CREATED_KEY),  // created
				viskitConfiguration.getValue(ViskitConfiguration.PROJECT_REVISION_KEY), // revision
				"", // extendsPackageName 
				"", // implementsPackageName
				projectFullPath, // project path, including project name
				viskitConfiguration.getValue(ViskitConfiguration.PROJECT_DESCRIPTION_KEY), // description
				true); // isProject
		
		graphMetadata.pathEditable = this.pathEditable;
		
        boolean modified = ProjectMetadataDialog.showDialog(this, graphMetadata); // display user panel, report if values were modified
        if (modified)
		{
			graphMetadata = ProjectMetadataDialog.getGraphMetadata(); // get updated metadata and save it to project
			
            viskitConfiguration.setValue(ViskitConfiguration.PROJECT_AUTHOR_KEY,      graphMetadata.author);
            viskitConfiguration.setValue(ViskitConfiguration.PROJECT_CREATED_KEY,     graphMetadata.created);
            viskitConfiguration.setValue(ViskitConfiguration.PROJECT_REVISION_KEY,    graphMetadata.revision);
            viskitConfiguration.setValue(ViskitConfiguration.PROJECT_DESCRIPTION_KEY, graphMetadata.description);
			
			if (pathEditable) // project path is only saved if creating a new project
			{
				viskitConfiguration.setValue(ViskitConfiguration.PROJECT_NAME_KEY,    graphMetadata.name);
				if (graphMetadata.path.endsWith(graphMetadata.name))
				{
					viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY,graphMetadata.path);
				}
				else
				{
					viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY,graphMetadata.path + ViskitStatics.getFileSeparator() + graphMetadata.name);
				}
			}
        }
		this.pathEditable = false; // reset value for next time through
		viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PROPERTIES_EDIT_COMPLETED_KEY, Boolean.toString(modified));
    }
	
    public final static String COPY_PROJECT_METHOD = "copyProject"; // must match following method name.  Not possible to accomplish this programmatically.
    /** Rename the current Project. */
    public void copyProject () // method name must exactly match preceding string value
	{
        String title = "Project copy failed";
		String message;
		ViskitConfiguration viskitConfiguration = ViskitConfiguration.instance();
		EventGraphController eventGraphController = (EventGraphController)ViskitGlobals.instance().getEventGraphController();
		
		String originalProjectName = viskitConfiguration.getValue(ViskitConfiguration.PROJECT_NAME_KEY);
		String originalProjectPath = viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PATH_KEY); // parent directory
		String  currentProjectPath = originalProjectPath;
		
		if (!originalProjectPath.endsWith(originalProjectName))
			  currentProjectPath = currentProjectPath + '/' + originalProjectName;
		
		editProjectProperties (true); // pathEditable=true.  must be careful, this widget can change project properties.
		
		// check if user cancelled, if so then skip and return
		if (viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PROPERTIES_EDIT_COMPLETED_KEY).equalsIgnoreCase("true")) 
		{
			String  changedProjectName = viskitConfiguration.getValue(ViskitConfiguration.PROJECT_NAME_KEY);
			String  changedProjectPath = viskitConfiguration.getValue(ViskitConfiguration.PROJECT_PATH_KEY); // parent directory
			
			// restore name and path changes
			viskitConfiguration.setValue(ViskitConfiguration.PROJECT_NAME_KEY, originalProjectName); // restore
			viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY, originalProjectPath); // restore
			ViskitGlobals.instance().getCurrentViskitProject().setProjectName (originalProjectName); // restore
			
			if (changedProjectPath.endsWith(originalProjectName))
			{
				changedProjectPath = changedProjectPath.substring(0,changedProjectPath.lastIndexOf(originalProjectName)) + changedProjectName;
				viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY, changedProjectPath);
			}
			
			if (originalProjectName.equalsIgnoreCase(changedProjectName)) // same name won't work
			{
				message = "<html><p align='center'>Project name <i>" + originalProjectName + "</i> unchanged, no action taken to copy project directory " + ViskitStatics.RECENTER_SPACING + "</p>";

				eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE, title, message);
				LOG.error (title + ".  " + message);
				return;
			}
			File originalProjectDirectory = new File(currentProjectPath);
			File  changedProjectDirectory = new File(changedProjectPath);

			if (changedProjectDirectory.exists() && (changedProjectDirectory.list().length > 0)) // don't clobber an existing directory that has files
			{
				message = "<html><p align='center'>Output directory already exists and contains files, so it cannot be a new project directory." + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p>&nbsp;" + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p align='center'>Project name <i>" + originalProjectName + "</i> unchanged, no action taken to copy project directory " + ViskitStatics.RECENTER_SPACING + "</p>";

				eventGraphController.messageToUser(JOptionPane.ERROR_MESSAGE, title, message);
				LOG.error (title + ".  " + message);
				return;
			}
			
			LOG.info ("Ready to copy... first closing current project " + originalProjectName);
			closeProject();
			
			if (ViskitGlobals.isProjectOpen()) // check if user cancelled closing project, if so then restore and return
			{				
				message = "<html><p align='center'>Project name <i>" + originalProjectName + "</i> unchanged, no action taken to copy project directory " + ViskitStatics.RECENTER_SPACING + "</p>";

				eventGraphController.messageToUser(JOptionPane.INFORMATION_MESSAGE, title, message);
				return;
			}

			// continuing...
			LOG.info ("Copying project " + originalProjectPath + " to " 
										 +  changedProjectPath);
				
			viskitConfiguration.setValue(ViskitConfiguration.PROJECT_NAME_KEY,  changedProjectName); // reset
			viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY,  changedProjectPath); // reset
			ViskitGlobals.instance().getCurrentViskitProject().setProjectName  (changedProjectName); // reset
			
			boolean fileCopySucceeded = false;
			Exception moveException   = null;
			try {
				FileUtils.copyDirectory(originalProjectDirectory, changedProjectDirectory); // apache commons.io
				
				fileCopySucceeded = true;
			} 
			catch (IOException ex) 
			{
				// Check results (sometimes Files.move works even though throwing an exception, perhaps File.copy also)
				if (changedProjectDirectory.exists() && (changedProjectDirectory.list().length > 0))
				{
					fileCopySucceeded = true;
					moveException = ex;
				}
				else
				{
					moveException = ex;
				}
			}
			if (fileCopySucceeded)
			{
				  title = "Project copy succeeded";
				message = "<html><p align='center'>Project <i>" + originalProjectName + "</i> copied to " + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p>&nbsp;" + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p align='center'><i>" + changedProjectDirectory + "</i>" + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p>&nbsp;" + ViskitStatics.RECENTER_SPACING + "</p>";
				eventGraphController.messageToUser(JOptionPane.INFORMATION_MESSAGE, title, message);
			}
			else // failed
			{
				title = "Project copy failed";
				message = "<html><p align='center'>A file access problem occurred.</p>"
						+ "<p>&nbsp;" + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p align='center'>Project <i>" + originalProjectName + "</i> was not copied to " + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p>&nbsp;" + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p align='center'><i>" + changedProjectDirectory + "</i>" + ViskitStatics.RECENTER_SPACING + "</p>"
						+ "<p>&nbsp;" + ViskitStatics.RECENTER_SPACING + "</p>";
				eventGraphController.messageToUser(JOptionPane.INFORMATION_MESSAGE, title, message);
				String moveExceptionTitle = new String();
				if (moveException != null)
					moveExceptionTitle = moveException.toString();
				LOG.error(title + ". " + moveExceptionTitle, moveException);
				LOG.error(title + ". " + message);
				
				// restore any name or path changes
				viskitConfiguration.setValue(ViskitConfiguration.PROJECT_NAME_KEY, originalProjectName); // restore
				viskitConfiguration.setValue(ViskitConfiguration.PROJECT_PATH_KEY, originalProjectPath); // restore
				ViskitGlobals.instance().getCurrentViskitProject().setProjectName (originalProjectName); // restore
				return;
			}

			LOG.info ("Opening copied project " + changedProjectPath + File.separator + changedProjectName);
			openProject(); // uses PROJECT_PATH_KEY
		}
		// log final results
		LOG.info ("Project status: " + ViskitGlobals.instance().getCurrentViskitProject().getProjectName() + " open=" 
								     + ViskitGlobals.isProjectOpen());
    }
	
    public final static String DELETE_PROJECT_METHOD = "deleteProject"; // must match following method name.  Not possible to accomplish this programmatically.
    /** Remove the current Project. */
    public void deleteProject () // method name must exactly match preceding string value
	{
		// TODO
    }
	
    public final static String DELETE_EVENT_GRAPH_FROM_PROJECT_METHOD = "deleteEventGraphFromProject"; // must match following method name.  Not possible to accomplish this programmatically.
    /** Remove an Event Graph from current Project. */
    public void deleteEventGraphFromProject () // method name must exactly match preceding string value
	{
		// TODO
    }
	
    public final static String DELETE_ASSEMBLY_FROM_PROJECT_METHOD = "deleteAssemblyFromProject"; // must match following method name.  Not possible to accomplish this programmatically.
    /** Remove an Assembly from current Project. */
    public void deleteAssemblyFromProject () // method name must exactly match preceding string value
	{
		// TODO
    }
	
    /**
     * Ensures a unique file name by appending a count integer to the filename
     * until the system can detect that it is indeed unique
     *
     * @param suggName the base file name to start with
     * @param parent the parent directory of the suggested file to be named
     * @return a unique filename based on the suggName with an integer appended
     * to ensure uniqueness
     */
    public File getUniqueName(String suggName, File parent) {
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
            fil = new File(parent, suggName + appnd + suffix);
            appnd = "" + ++count;
        } while (fil.exists());

        return fil;
    }

    @Override
    public File saveEventGraphFileAsk(String suggestedName, boolean showUniqueName)
	{
		 return saveEventGraphFileAsk(suggestedName,  showUniqueName, "Save Event Graph File");
    }

    @Override
    public File saveEventGraphFileAsk(String suggestedName, boolean showUniqueName, String dialogTitle)
	{
        buildOpenSaveEventGraphsChooser();
        eventGraphFileChooser.setDialogTitle(dialogTitle);

        File file = new File(eventGraphFileChooser.getCurrentDirectory(), suggestedName);
        if (showUniqueName)
		{
            file = getUniqueName(suggestedName, file.getParentFile());
        }

        eventGraphFileChooser.setSelectedFile(file);
        int returnValue = eventGraphFileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION)
		{
            if (eventGraphFileChooser.getSelectedFile().exists())
			{
                if (JOptionPane.YES_OPTION != genericAskYN("File Exists",  "Overwrite? Confirm"))
				{
                    return null; // user cancelled
                }
            }
            return eventGraphFileChooser.getSelectedFile();
        }
        deleteCanceledSave(file.getParentFile()); // user canceled
        eventGraphFileChooser = null;
        return null;
    }

    /** Handles a canceled new Event Graph file creation
     *
     * @param file to candidate EG file
     */
    private void deleteCanceledSave(File file) 
	{
        ViskitGlobals.instance().getAssemblyEditViewFrame().deleteCanceledSave(file);
    }

    @Override
    public File openRecentFilesAsk(Collection<String> lis) 
	{
        return ViskitGlobals.instance().getAssemblyEditViewFrame().openRecentFilesAsk(lis);
    }

    @Override
    public boolean doEditNode(EventNode node) 
	{
        selectMode.doClick();     // always go back into select mode
        return EventInspectorDialog.showDialog(ViskitGlobals.instance().getViskitApplicationFrame(), node); // blocks
    }

    @Override
    public boolean doEditEdge(Edge edge) 
	{
        selectMode.doClick();     // always go back into select mode
        return EdgeInspectorDialog.showDialog(ViskitGlobals.instance().getViskitApplicationFrame(), edge); // blocks
    }

    @Override
    public boolean doEditCancellingEdge(Edge edge) 
	{
        return doEditEdge(edge); // blocks
    }

    @Override
    public boolean doEditParameter(ViskitParameter param) 
	{
        return ParameterDialog.showDialog(ViskitGlobals.instance().getViskitApplicationFrame(), param);    // blocks
    }

    @Override
    public boolean doEditStateVariable(ViskitStateVariable var) 
	{
        return StateVariableDialog.showDialog(ViskitGlobals.instance().getViskitApplicationFrame(), var); // blocks
    }

    @Override
    public int genericAsk(String title, String msg) {
        return ViskitGlobals.instance().getAssemblyEditViewFrame().genericAsk(title, msg);
    }

    @Override
    public int genericAskYN(String title, String msg) {
        return ViskitGlobals.instance().getAssemblyEditViewFrame().genericAskYN(title, msg);
    }

    @Override
    public void genericReport(int type, String title, String msg) {
        ViskitGlobals.instance().getAssemblyEditViewFrame().genericReport(type, title, msg);
    }

    @Override
    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }
	public void refreshCodeBlock (String newSource)
	{
        EventGraphComponentWrapper eventGraphComponentWrapper = getCurrentJGraphEventGraphComponentWrapper();
		eventGraphComponentWrapper.codeBlockPanel.setData(newSource);
	}

    @Override
    public void modelChanged(mvcModelEvent event)
	{
        EventGraphComponentWrapper eventGraphComponentWrapper = getCurrentJGraphEventGraphComponentWrapper();
        ParametersPanel     parametersPanel     = eventGraphComponentWrapper.parametersPanel;
        StateVariablesPanel stateVariablesPanel = eventGraphComponentWrapper.stateVariablesPanel;
        switch (event.getID())
		{
            // Changes the two side panels need to know about
            case ModelEvent.SIMPARAMETER_ADDED:
                parametersPanel.addRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.SIMPARAMETER_DELETED:
                parametersPanel.removeRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.SIMPARAMETER_CHANGED:
                parametersPanel.updateRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATEVARIABLE_ADDED:
                stateVariablesPanel.addRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATEVARIABLE_DELETED:
                stateVariablesPanel.removeRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATEVARIABLE_CHANGED:
                stateVariablesPanel.updateRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.CODEBLOCK_CHANGED:
                eventGraphComponentWrapper.codeBlockPanel.setData((String) event.getSource());
                break;
            case ModelEvent.NEWMODEL:
                stateVariablesPanel.setData(null);
                parametersPanel.setData(null);

                // Deliberate matching to default: option here; avoids warning
                eventGraphComponentWrapper.viskitModelChanged((ModelEvent) event);
                break;

            // Changes the jGraph needs to know about
            default:
                eventGraphComponentWrapper.viskitModelChanged((ModelEvent) event);
                break;
        }

        // Let eventGraphModel.isDirty() determine status color
        toggleEventGraphStatusIndicators();
    }

} // end class file EventGraphViewFrame.jav