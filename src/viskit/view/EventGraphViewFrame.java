/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.view;

import actions.ActionIntrospector;
import actions.ActionUtilities;


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
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import viskit.control.EventGraphController;
import viskit.model.ModelEvent;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitProject;
import static viskit.ViskitStatics.DESCRIPTION_HINT;
import static viskit.control.AssemblyControllerImpl.METHOD_captureWindow;
import static viskit.control.AssemblyControllerImpl.METHOD_newProject;
import static viskit.control.AssemblyControllerImpl.METHOD_openProject;
import static viskit.control.AssemblyControllerImpl.METHOD_quit;
import static viskit.control.AssemblyControllerImpl.METHOD_showViskitUserPreferences;
import static viskit.control.EventGraphControllerImpl.METHOD_buildNewEventNode;
import static viskit.control.EventGraphControllerImpl.METHOD_close;
import static viskit.control.EventGraphControllerImpl.METHOD_closeAll;
import static viskit.control.EventGraphControllerImpl.METHOD_copy;
import static viskit.control.EventGraphControllerImpl.METHOD_cut;
import static viskit.control.EventGraphControllerImpl.METHOD_editGraphMetadata;
import static viskit.control.EventGraphControllerImpl.METHOD_newEventGraph;
import static viskit.control.EventGraphControllerImpl.METHOD_newSelfReferentialCancelingEdge;
import static viskit.control.EventGraphControllerImpl.METHOD_newSelfReferentialSchedulingEdge;
import static viskit.control.EventGraphControllerImpl.METHOD_newSimulationParameter;
import static viskit.control.EventGraphControllerImpl.METHOD_newStateVariable;
import static viskit.control.EventGraphControllerImpl.METHOD_open;
import static viskit.control.EventGraphControllerImpl.METHOD_paste;
import static viskit.control.EventGraphControllerImpl.METHOD_redo;
import static viskit.control.EventGraphControllerImpl.METHOD_remove;
import static viskit.control.EventGraphControllerImpl.METHOD_save;
import static viskit.control.EventGraphControllerImpl.METHOD_saveAs;
import static viskit.control.EventGraphControllerImpl.METHOD_undo;
import static viskit.control.EventGraphControllerImpl.METHOD_viewXML;
import static viskit.control.EventGraphControllerImpl.METHOD_zipAndMailProject;
import viskit.images.CancellingEdgeIcon;
import viskit.images.EventNodeIcon;
import viskit.images.SchedulingEdgeIcon;
import viskit.jgraph.ViskitGraphComponentWrapper;
import viskit.jgraph.ViskitGraphModel;
import viskit.model.*;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.mvc.MvcModelEvent;
import viskit.util.EventGraphFileFilter;
import viskit.view.dialog.ParameterDialog;
import viskit.view.dialog.EdgeInspectorDialog;
import viskit.view.dialog.StateVariableDialog;
import viskit.view.dialog.EventNodeInspectorDialog;
import viskit.view.dialog.ViskitUserPreferencesDialog;
import viskit.mvc.MvcController;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;
import static viskit.control.EventGraphControllerImpl.METHOD_generateJavaCode;

/**
 Main "view" of the Viskit app. This class controls a 3-paneled JFrame showing
 a jgraph on the left and state variables and sim parameters panels on the
 right, with menus and a toolbar. To fully implement application-level MVC,
 events like the dragging and dropping of a node on the screen are first
 recognized in this class, but the GUI is not yet changed. Instead, this class
 (the View) messages the controller class (EventGraphControllerImpl -- by
 means of the EventGraphController i/f). The controller then informs the model
 (ModelImpl), which then updates itself and "broadcasts" that fact. This class
 is a model listener, so it gets the report, then updates the GUI. A round
 trip.

 20 SEP 2005: Updated to show multiple open event graphs. The controller is
 largely unchanged. To understand the flow, understand that 1) The tab
 "ChangeListener" plays a key role; 2) When the ChangeListener is hit, the
 controller.setModel() method installs the appropriate model for the
 newly-selectedTab event graph.

 OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects MOVES Institute
 Naval Postgraduate School, Monterey CA www.nps.edu

  @author Mike Bailey
  @since Mar 2, 2004
  @since 12:52:59 PM
  @version $Id$
 */
public class EventGraphViewFrame extends MvcAbstractViewFrame implements EventGraphView
{
    static final Logger LOG = LogManager.getLogger();

    // Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc.
    public final static int SELECT_MODE = 0;
    public final static int ADD_NODE_MODE = 1;
    public final static int SCHEDULING_EDGE_MODE = 2;
    public final static int CANCELLING_EDGE_MODE = 3;
    public final static int SELF_REF_MODE = 4;
    public final static int SELF_REF_CANCEL_MODE = 5;

    private static final String FRAME_DEFAULT_TITLE = " Viskit Event Graph Editor";
    private static final String LOOK_AND_FEEL = ViskitUserPreferencesDialog.getLookAndFeel();

    /** Toolbar for dropping icons, connecting, etc. */
    private JToolBar eventGraphEditorToolbar;    // Mode buttons on the toolbar
    private JLabel metadataLabel;
    private JLabel addLabel;
    private JLabel addEventLabel;
    private JLabel addSelfReferentialEdgeLabel;
    private JLabel addSelfCancellingEdgeLabel;
    
    private JLabel modeLabel;
    private JToggleButton selectModeButton;
    private JToggleButton schedulingEdgeModeButton;
    private JToggleButton cancellingEdgeModeButton;
    private JLabel zoomLabel;
    
    private JTabbedPane tabbedPane;
    private JMenuBar myMenuBar;
    private JMenu editMenu;
    private JMenu eventGraphMenu;
    private JMenu editEventGraphSubMenu;
    private JMenuItem quitMenuItem;
    private RecentEventGraphFileListener recentEventGraphFileListener;

    private JMenu openRecentEventGraphMenu, openRecentProjectMenu;

    /**
     * Constructor; lays out initial GUI objects
     * @param mvcController the controller for this frame (MVF)
     */
    public EventGraphViewFrame(MvcController mvcController) {
        super(FRAME_DEFAULT_TITLE);
        initializeMVC(mvcController);       // set up mvc linkages
        initializeUserInterface();    // build widgets
    }

    /** @return the JPanel which is the content of this JFrame */
    public JComponent getContentPaneComponent() {
        return (JComponent) getContentPane();
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return quitMenuItem;
    }

    /** @return the current mode--select, add, arc, cancelArc */
    public int getCurrentMode() 
    {
        // Use the button's selected status to figure out what mode we are in.

        if (selectModeButton.isSelected()) {
            return SELECT_MODE;
        }
        if (schedulingEdgeModeButton.isSelected()) {
            return SCHEDULING_EDGE_MODE;
        }
        if (cancellingEdgeModeButton.isSelected()) {
            return CANCELLING_EDGE_MODE;
        }

        LOG.error("assert false : \"getCurrentMode()\""); // TODO ??
        return 0;
    }

    /**
     * Initialize the MVC connections
     * @param mvcController the controller for this view
     */
    private void initializeMVC(MvcController mvcController) {
        setController(mvcController);
    }

    /**
     * Initialize the user interface
     */
    private void initializeUserInterface() 
    {
        // Layout menus
        buildEditMenu(); // must be first
        buildMenus();

        // Layout of toolbar
        setupToolbar();

        // Set up a eventGraphViewerContent level pane that will be the content pane. This
        // has a border layout, and contains the toolbar on the eventGraphViewerContent and
        // the main splitpane underneath.

        getContentPaneComponent().setLayout(new BorderLayout());
        getContentPaneComponent().add(getToolBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new TabSelectionHandler());

        getContentPaneComponent().add(tabbedPane, BorderLayout.CENTER);
        getContentPaneComponent().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    public int getNumberEventGraphsLoaded()
    {
        return tabbedPane.getTabCount();
    }
    
    /** has one or more Event Graphs loaded
     * @return whether one or more Event Graphs are loaded */
    public boolean hasEventGraphsLoaded()
    {
        return (getNumberEventGraphsLoaded() > 0);
    }

    public ViskitGraphComponentWrapper getCurrentVgraphComponentWrapper() 
    {
        JSplitPane splitPane = (JSplitPane) tabbedPane.getSelectedComponent();
        if (splitPane == null) {
            return null;
        }

        JScrollPane scrollPane = (JScrollPane) splitPane.getLeftComponent();
        return (ViskitGraphComponentWrapper) scrollPane.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent() 
    {
        ViskitGraphComponentWrapper graphComponentWrapper = getCurrentVgraphComponentWrapper();
        if (graphComponentWrapper == null || graphComponentWrapper.drawingSplitPane == null) 
        {
             return null;
        }
        else return graphComponentWrapper.drawingSplitPane.getLeftComponent();
    }

    public JToolBar getToolBar() {
        return eventGraphEditorToolbar;
    }

    public void setToolBar(JToolBar newToolbar) {
        this.eventGraphEditorToolbar = newToolbar;
    }

    /**
     * @return the openRecentProjMenu
     */
    public JMenu getOpenRecentProjectMenu() 
    {
        return openRecentProjectMenu;
    }

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener 
    {
        @Override
        public void stateChanged(ChangeEvent changeEvent) 
        {
            ViskitGraphComponentWrapper viskitGraphComponentWrapper = getCurrentVgraphComponentWrapper();

            if (viskitGraphComponentWrapper == null) {     // last tab has been closed
                setSelectedEventGraphName(null);
                return;
            }

            // NOTE: Although a somewhat good idea, perhaps the user does not
            // wish to have work saved when merely switching between tabs on
            // the Event Graph palette. However, when switching to the Assembly palette, we
            // will save all Event Graphs that have been modified
//            if (((Model)getModel()).isDirty()) {
//                ((EventGraphController)getController()).save();
//            }

            setModel((MvcModel) viskitGraphComponentWrapper.model);    // hold on locally
            getController().setModel(getModel());  // tell controller

//            adjustMenus((Model) getModel()); // enable/disable menu items based on new Event Graph

            GraphMetadata graphMetadata = ((Model) getModel()).getMetadata();
            if (graphMetadata != null) {
                setSelectedEventGraphName(graphMetadata.name);
                setSelectedEventGraphDescription(ViskitStatics.emptyIfNull(graphMetadata.description));
            } 
            else if (viskit.ViskitStatics.debug) {
                LOG.error("error: EventGraphViewFrame metadata is null");
            }
        }
    }

    private void buildStateParamSplit(ViskitGraphComponentWrapper graphComponentWrapper) 
    {
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

        StateVariablesPanel nextStateVariablesPanel = new StateVariablesPanel(300, 5);
        stateVariablesPanel.add(nextStateVariablesPanel);
        stateVariablesPanel.add(Box.createVerticalStrut(5));
        stateVariablesPanel.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for stateVariable adds, deletes and edits and tell it we'll be doing adds and deletes
        nextStateVariablesPanel.setEnableAddsAndDeletes(false);
        nextStateVariablesPanel.addPlusListener(ActionIntrospector.getAction(getController(), METHOD_newStateVariable));

        // Introspector can't handle a param to the method....?
        nextStateVariablesPanel.addMinusListener((ActionEvent event) -> {
            ((EventGraphController) getController()).deleteStateVariable((ViskitStateVariable) event.getSource());
        });

        nextStateVariablesPanel.addDoubleClickedListener((ActionEvent event) -> {
            ((EventGraphController) getController()).stateVariableEdit((ViskitStateVariable) event.getSource());
        });

        // Event jGraph parameters area
        JPanel parametersPanel = new JPanel();
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS)); //BorderLayout());
        parametersPanel.add(Box.createVerticalStrut(5));

        JLabel descriptionLabel = new JLabel("Event Graph Description"); // TODO fix this functionality
        descriptionLabel.setToolTipText("(" + DESCRIPTION_HINT + ")");
        descriptionLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        parametersPanel.add(descriptionLabel);
//        parametersPanel.add(Box.createVerticalStrut(5));
        
        JLabel instructionsLabel = new JLabel("Edit Properties or Ctrl-E to Edit");
        instructionsLabel.setToolTipText(DESCRIPTION_HINT);
        instructionsLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        int bigSz = descriptionLabel.getFont().getSize();
        instructionsLabel.setFont(descriptionLabel.getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        parametersPanel.add(instructionsLabel);
//        parametersPanel.add(Box.createVerticalStrut(5));

        JTextArea descriptionTA = new JTextArea();
        descriptionTA.setToolTipText(DESCRIPTION_HINT);
        descriptionTA.setEditable(false);
        descriptionTA.setText(DESCRIPTION_HINT); // initial value, hopefully overwritten by loaded value
        descriptionTA.setWrapStyleWord(true);
        descriptionTA.setLineWrap(true);
        descriptionTA.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTA);
        descriptionScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        descriptionScrollPane.setMinimumSize(new Dimension(20, 20));

        // This works, you just have to have several lines of typed text to cause
        // the etched scrollbar to appear
        parametersPanel.add(descriptionScrollPane);
        parametersPanel.add(Box.createVerticalStrut(5));
        parametersPanel.setMinimumSize(new Dimension(20, 20));

        eventGraphParametersSubpanel = new JPanel();
        eventGraphParametersSubpanel.setLayout(new BoxLayout(eventGraphParametersSubpanel, BoxLayout.X_AXIS));
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());

        JLabel titleLabel = new JLabel("Simulation Parameters");
        titleLabel.setToolTipText("Event graph simulation parameters are initialized upon starting each simulation replication");

        eventGraphParametersSubpanel.add(titleLabel);
        eventGraphParametersSubpanel.add(Box.createHorizontalGlue());
        parametersPanel.add(eventGraphParametersSubpanel);

        ParametersPanel nextParametersPanel = new ParametersPanel(300, 5);
        nextParametersPanel.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for parameter adds, deletes and edits and tell it we'll be doing adds and deletes
        nextParametersPanel.setEnableAddsAndDeletes(false);
        nextParametersPanel.addPlusListener(ActionIntrospector.getAction(getController(), METHOD_newSimulationParameter));

        // Introspector can't handle a param to the method....?
        nextParametersPanel.addMinusListener((ActionEvent event) -> {
            ((EventGraphController) getController()).deleteSimParameter((ViskitParameter) event.getSource());
        });
        nextParametersPanel.addDoubleClickedListener((ActionEvent event) -> {
            ((EventGraphController) getController()).simParameterEdit((ViskitParameter) event.getSource());
        });

        parametersPanel.add(nextParametersPanel);
        parametersPanel.add(Box.createVerticalStrut(5));

        CodeBlockPanel codeblockPanel = buildCodeBlockPanel();

        JSplitPane stateCodeBlockSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(stateVariablesPanel),
                new JScrollPane(buildCodeBlockComponent(codeblockPanel)));
        stateCodeBlockSplitPane.setResizeWeight(0.75);
        stateCodeBlockSplitPane.setMinimumSize(new Dimension(20, 20));

        // Split pane that has description, parameters, state variables and code block.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                parametersPanel,
                stateCodeBlockSplitPane);
        splitPane.setResizeWeight(0.75);
        splitPane.setMinimumSize(new Dimension(20, 20));

        graphComponentWrapper.stateParameterSplitPane = splitPane;
        graphComponentWrapper.parametersPanel = nextParametersPanel;
        graphComponentWrapper.stateVariablesPanel = nextStateVariablesPanel;
        graphComponentWrapper.codeBlockPanel = codeblockPanel;
    }

    private CodeBlockPanel buildCodeBlockPanel() 
    {
        CodeBlockPanel odeBlockPanel = new CodeBlockPanel(this, true, "Event Graph Code Block");
        odeBlockPanel.addUpdateListener((ActionEvent e) -> {
            String s = (String) e.getSource();
            if (s != null) {
                ((EventGraphController) getController()).codeBlockEdit(s);
            }
        });
        return odeBlockPanel;
    }

    private JComponent buildCodeBlockComponent(CodeBlockPanel codeBlockPanel) 
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel globalCodeBlockLabel = new JLabel("Global Code Block");
        globalCodeBlockLabel.setToolTipText("Code block source code can declare global variables, static variables, static methods, etc.");
        globalCodeBlockLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(globalCodeBlockLabel);
        codeBlockPanel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(codeBlockPanel);
        panel.setBorder(new EmptyBorder(5, 5, 5, 2));
        Dimension d = new Dimension(panel.getPreferredSize());
        d.width = Integer.MAX_VALUE;
        panel.setMaximumSize(d);

        return panel;
    }

    @Override
    public void setSelectedEventGraphName(String newName) 
    {
        boolean nullString = !(newName != null && newName.trim().length() > 0); // TODO ! isBlank()
        if  ((!nullString) && (newName != null) && (tabbedPane != null))
             tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), newName.trim());
    }

    @Override
    public void setSelectedEventGraphDescription(String description) 
    {
        JSplitPane splitPane = getCurrentVgraphComponentWrapper().stateParameterSplitPane;
        JPanel panel = (JPanel) splitPane.getTopComponent();
        Component[] componentArray = panel.getComponents();
        for (Component component : componentArray) 
        {
            if (component instanceof JScrollPane) 
            {
                component = ((JScrollPane) component).getViewport().getComponent(0);
                ((JTextComponent) component).setText(description);
            }
        }
    }

    int untitledCount = 0;

    @Override
    public void addTab(Model model) 
    {
        ViskitGraphModel graphModel = new ViskitGraphModel();
        ViskitGraphComponentWrapper graphPane = new ViskitGraphComponentWrapper(graphModel, this);
        graphModel.setjGraph(graphPane);
        graphPane.model = model;

        buildStateParamSplit(graphPane);

        // Split pane with the canvas on the left and a split pane with state variables and parameters on the right.
        JScrollPane scrollPane = new JScrollPane(graphPane);

        graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, graphPane.stateParameterSplitPane);

        // This is the key to getting the jgraph half to come up appropriately
        // wide by giving the left component (JGraph side) most of the usable
        // extra space in this SplitPlane -> 75%
        graphPane.drawingSplitPane.setResizeWeight(0.75);
        graphPane.drawingSplitPane.setOneTouchExpandable(true);

        graphPane.addMouseListener(new CursorHandler());
        try {
            graphPane.getDropTarget().addDropTargetListener(new EventGraphDropTargetAdapter());
        } 
        catch (TooManyListenersException tmle) {
            LOG.error(tmle);
        }

        // the view holds only one model, so it gets overwritten with each tab
        // but this call serves also to register the view with the passed model
        // by virtue of calling stateChanged()
        tabbedPane.add("" + untitledCount++, graphPane.drawingSplitPane);

        // Bring the JGraph component to front. Also, allows models their own
        // canvas to draw to prevent a NPE
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane);

        // Now expose the EventGraph toolbar
        Runnable r = () -> {
            getToolBar().setVisible(true);
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void deleteTab(Model model)
    {
        JSplitPane splitPane;
        JScrollPane scrollPane;
        ViskitGraphComponentWrapper viskitGraphComponentWrapper;
        Runnable r;
        for (Component c : tabbedPane.getComponents()) {
            splitPane = (JSplitPane) c;
            scrollPane = (JScrollPane) splitPane.getLeftComponent();
            viskitGraphComponentWrapper = (ViskitGraphComponentWrapper) scrollPane.getViewport().getComponent(0);
            if (viskitGraphComponentWrapper.model == model) {
                tabbedPane.remove(c);
                viskitGraphComponentWrapper.isActive = false;

                // Don't allow operation of tools with no Event Graph tab in view (NPEs)
                if (tabbedPane.getTabCount() == 0) {
                    r = () -> {
                        getToolBar().setVisible(false);
                    };
                    SwingUtilities.invokeLater(r);
                }
                return;
            }
        }
    }

    @Override
    public Model[] getOpenModels() 
    {
        JSplitPane splitPane;
        JScrollPane scrollPane;
        ViskitGraphComponentWrapper graphComponentWrapper;
        Component[] componentArray = tabbedPane.getComponents();
        Model[] modelArray = new Model[componentArray.length];
        
        for (int i = 0; i < modelArray.length; i++) 
        {
            splitPane = (JSplitPane) componentArray[i];
            scrollPane = (JScrollPane) splitPane.getLeftComponent();
            graphComponentWrapper = (ViskitGraphComponentWrapper) scrollPane.getViewport().getComponent(0);
            modelArray[i] = graphComponentWrapper.model;
        }
        return modelArray;
    }

    @Override
    public String addParameterDialog()
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();

        if (ParameterDialog.showDialog(ViskitGlobals.instance().getMainFrame(), null)) {      // blocks here
            ((EventGraphController) getController()).buildNewSimulationParameter(ParameterDialog.newName,
                    ParameterDialog.newType,
                    "new value here",
                    ParameterDialog.newComment);
            return ParameterDialog.newName;
        }
        return null;
    }

    @Override
    public String addStateVariableDialog() 
    {
        ViskitGlobals.instance().selectEventGraphEditorTab();
        
        if (StateVariableDialog.showDialog(ViskitGlobals.instance().getMainFrame(), null)) {      // blocks here
            ((EventGraphController) getController()).buildNewStateVariable(StateVariableDialog.newName,
                    StateVariableDialog.newType,
                    "new value here",
                    StateVariableDialog.newComment);
            return StateVariableDialog.newName;
        }
        return null;
    }

    class RecentEventGraphFileListener implements MvcRecentFileListener 
    {
        @Override
        public void listenerChanged() 
        {
            String fileName;
            Action currentAction;
            JMenuItem menuItem;
            EventGraphController eventGraphController = (EventGraphController) getController();
            Set<String> filePathSet = eventGraphController.getRecentEventGraphFileSet();
            openRecentEventGraphMenu.removeAll(); // clear prior to rebuilding menu
            openRecentEventGraphMenu.setEnabled(false); // disable unless file is found
            File file;
            for (String fullPath : filePathSet) 
            {
                file = new File(fullPath);
                if (!file.exists())
                {
                    // file not found as expected, something happened externally and so report it
                    LOG.error("*** [EventGraphViewFrame listChanged] Event graph file not found: " + file.getPath());
                    continue; // actual file not found, skip to next file in files loop
                }
                fileName = file.getName();
                currentAction = new ParameterizedAction(fileName);
                currentAction.putValue(ViskitStatics.FULL_PATH, fullPath);
                menuItem = new JMenuItem(currentAction);
                menuItem.setToolTipText(file.getPath());
                openRecentEventGraphMenu.add(menuItem);
                openRecentEventGraphMenu.setEnabled(true); // at least one is found
            }
            if (!filePathSet.isEmpty()) 
            {
                openRecentEventGraphMenu.add(new JSeparator());
                currentAction = new ParameterizedAction("clear history");
                currentAction.putValue(ViskitStatics.FULL_PATH, ViskitStatics.CLEAR_PATH_FLAG);  // flag
                menuItem = new JMenuItem(currentAction);
                menuItem.setToolTipText("Clear this list");
                openRecentEventGraphMenu.add(menuItem);
            }
            // TODO note that some items might remain loaded after "clear menu" and so wondering if that is ambiguous
        }
    }

    class ParameterizedAction extends javax.swing.AbstractAction 
    {
        ParameterizedAction(String s) 
        {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) 
        {
            ViskitGlobals.instance().selectEventGraphEditorTab();
            EventGraphController eventGraphController = (EventGraphController) getController();

            File fullPath;
            Object obj = getValue(ViskitStatics.FULL_PATH);
            if (obj instanceof String)
                fullPath = new File((String) obj);
            else
                fullPath = (File) obj;

            if (fullPath != null && fullPath.getPath().equals(ViskitStatics.CLEAR_PATH_FLAG)) 
            {
                eventGraphController.clearRecentEventGraphFileSet();
            } 
            else {
                eventGraphController.openRecentEventGraph(fullPath);
            }
        }
    }

    private void buildEditMenu()
    {
        EventGraphController eventGraphController = (EventGraphController) getController(); // TODO repetitive
        
        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        
        // Set up edit menu
        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(buildMenuItem(eventGraphController, METHOD_undo, "Undo", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, accelMod)));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_redo, "Redo", KeyEvent.VK_Y,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, accelMod)));

        ActionIntrospector.getAction(eventGraphController, METHOD_undo).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_redo).setEnabled(false);
        editMenu.addSeparator();

        // the next four are disabled until something is selected
        editMenu.add(buildMenuItem(eventGraphController, METHOD_cut, "Cut", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));
        editMenu.getItem(editMenu.getItemCount()-1).setToolTipText("Cut is not supported in Viskit.");
        editMenu.add(buildMenuItem(eventGraphController, METHOD_copy, "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_paste, "Paste Event Node", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_remove, "Delete", KeyEvent.VK_D,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, accelMod)));

        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, METHOD_cut   ).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_copy  ).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_paste ).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_remove).setEnabled(false);
        editMenu.addSeparator();

        editMenu.add(buildMenuItem(eventGraphController, METHOD_buildNewEventNode, "Add a new Event Node", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_newSimulationParameter, "Add a new Simulation Parameter", KeyEvent.VK_S, null));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_newStateVariable, "Add a new State Variable", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_newSelfReferentialSchedulingEdge, "Add Self-Referential Scheduling Edge", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(eventGraphController, METHOD_newSelfReferentialCancelingEdge, "Add Self-Refenential Canceling Edge", KeyEvent.VK_A, null));

        // Thess start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, METHOD_newSelfReferentialSchedulingEdge).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_newSelfReferentialCancelingEdge ).setEnabled(false);
        editMenu.addSeparator();

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        editMenu.add(buildMenuItem(eventGraphController, METHOD_editGraphMetadata, "Edit selected Event Graph Metadata Properties...", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));
        }
    }

    private void buildMenus()
    {
        EventGraphController eventGraphController = (EventGraphController) getController();
        recentEventGraphFileListener = new RecentEventGraphFileListener();
        eventGraphController.addRecentEventGraphFileListener(getRecentEventGraphFileListener());

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Set up file menu
//      JMenu fileMenu = new JMenu("File");
        eventGraphMenu = new JMenu("Event Graph"); // Editor
        eventGraphMenu.setMnemonic(KeyEvent.VK_E);

        editEventGraphSubMenu = new JMenu("Edit Event Graph..."); // submenu
        editEventGraphSubMenu.setToolTipText("Edit functions for selected Event Graph");
        editEventGraphSubMenu.setMnemonic(KeyEvent.VK_E);
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_undo, "Undo", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, accelMod)));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_redo, "Redo", KeyEvent.VK_Y,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, accelMod)));

        ActionIntrospector.getAction(eventGraphController, METHOD_undo).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_redo).setEnabled(false);
        editEventGraphSubMenu.addSeparator();

        // the next four are disabled until something is selected
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_cut, "Cut", KeyEvent.VK_X,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));
        editEventGraphSubMenu.getItem(editEventGraphSubMenu.getItemCount()-1).setToolTipText("Cut is not supported in Viskit.");
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_copy, "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_paste, "Paste Event Node", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_remove, "Delete", KeyEvent.VK_DELETE,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, accelMod)));

        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, METHOD_cut).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_copy).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_paste).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_remove).setEnabled(false);
        editEventGraphSubMenu.addSeparator();

        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_buildNewEventNode, "Add a new Event Node", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_newSimulationParameter, "Add a new Simulation Parameter", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_newStateVariable, "Add a new State Variable", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_newSelfReferentialSchedulingEdge, "Add Self-Referential Scheduling Edge", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, METHOD_newSelfReferentialCancelingEdge, "Add Self-Refenential Canceling Edge", KeyEvent.VK_A, null));

        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, METHOD_newSelfReferentialSchedulingEdge).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_newSelfReferentialCancelingEdge).setEnabled(false);
        
        // TODO "disable" both of these if no Event Graph is active
        eventGraphMenu.add(editEventGraphSubMenu);
        JMenuItem editEventGraphMetadataMenuItem = buildMenuItem(eventGraphController, METHOD_editGraphMetadata, "Edit Event Graph Metadata", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod));
        editEventGraphMetadataMenuItem.setToolTipText("Edit selected Event Graph Metadata Properties");
        eventGraphMenu.add(editEventGraphMetadataMenuItem);
        eventGraphMenu.addSeparator();

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_newProject, "New Viskit Project", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK)));
        
        eventGraphMenu.add(buildMenuItem(this,                 METHOD_openProject, "Open Project", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK)));
        eventGraphMenu.add(openRecentProjectMenu = buildMenu("Open Recent Project"));
        // The recently opened project file listener will be set with the
        // openRecentProjMenu in the MainFrame after the AssemblyView is instantiated

        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_zipAndMailProject, "Zip/Email Viskit Project", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK)));
        eventGraphMenu.addSeparator();
        }
        
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_newEventGraph, "New Event Graph", KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, accelMod)));

        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_open, "Open Event Graph", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, accelMod)));
        eventGraphMenu.add(openRecentEventGraphMenu = buildMenu("Open Recent Event Graph"));
        openRecentEventGraphMenu.setMnemonic('O');
        openRecentEventGraphMenu.setEnabled(false); // inactive until needed, reset by listener
        eventGraphMenu.addSeparator();

        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_close, "Close Event Graph", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, accelMod)));
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_closeAll, "Close All Event Graphs", KeyEvent.VK_C, null));
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_save, "Save Event Graph", KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, accelMod)));
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_saveAs, "Save Event Graph as...", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_A, accelMod)));
        eventGraphMenu.addSeparator();

        JMenuItem eventGraphGraphImageSave = buildMenuItem(eventGraphController, METHOD_captureWindow, "Image Save", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod));
        eventGraphGraphImageSave.setToolTipText("Image Save for Event Graph Diagram");
        eventGraphMenu.add(eventGraphGraphImageSave);
        JMenuItem eventGraphGenerateJavaSourceMenuItem = buildMenuItem(eventGraphController, METHOD_generateJavaCode, "Java Code Generation", KeyEvent.VK_J,
                KeyStroke.getKeyStroke(KeyEvent.VK_J, accelMod));
        eventGraphGenerateJavaSourceMenuItem.setToolTipText("Java Code Generation and Compilation for saved Event Graph");
        eventGraphMenu.add(eventGraphGenerateJavaSourceMenuItem);
        JMenuItem eventGraphXmlViewMenuItem = buildMenuItem(eventGraphController, METHOD_viewXML, "XML Source View", KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod));
        eventGraphXmlViewMenuItem.setToolTipText("XML Source View of Saved Event Graph");
        eventGraphMenu.add(eventGraphXmlViewMenuItem);

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        eventGraphMenu.addSeparator();
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_showViskitUserPreferences, "Viskit User Preferences", KeyEvent.VK_V, null));
        
        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        eventGraphMenu.addSeparator();

        eventGraphMenu.add(quitMenuItem = buildMenuItem(eventGraphController, METHOD_quit, "Quit", KeyEvent.VK_Q,
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, accelMod)));
        }
        }

        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(eventGraphMenu);
        myMenuBar.add(editMenu);
        
// see AssemblyViewFrame
//        Help help = new Help(ViskitGlobals.instance().getMainFrame());
//        help.mainFrameLocated(ViskitGlobals.instance().getMainFrame().getBounds());
//        ViskitGlobals.instance().setHelp(help); // single instance for all viskit frames
//
//        JMenu helpMenu = new JMenu("Help");
//        helpMenu.setMnemonic(KeyEvent.VK_H);
//        
//        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
//        {
//        helpMenu.add(buildMenuItem(help, METHOD_doContents, "Contents", KeyEvent.VK_C, null));
//        helpMenu.add(buildMenuItem(help, METHOD_doSearch, "Search", KeyEvent.VK_S, null));
//        helpMenu.addSeparator();
//
//        helpMenu.add(buildMenuItem(help, METHOD_doTutorial, "Tutorial", KeyEvent.VK_T, null));
//        helpMenu.add(buildMenuItem(help, METHOD_aboutViskit, "About Viskit", KeyEvent.VK_A, null));
//
//        myMenuBar.add(helpMenu);
//        }
    }
    
    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String methodName, String menuItemName, Integer mnemonic, KeyStroke accelerator)
    {
        Action action = ActionIntrospector.getAction(source, methodName);
        if (action == null)
        {
            LOG.error("buildMenuItem() reflection failed for name=" + menuItemName + " method=" + methodName + " in " + source.toString());
            return new JMenuItem(menuItemName + "(not working, buildMenuItem() reflection failed)");
        }
        Map<String, Object> map = new HashMap<>();
        if (mnemonic != null) {
            map.put(Action.MNEMONIC_KEY, mnemonic);
        }
        if (accelerator != null) {
            map.put(Action.ACCELERATOR_KEY, accelerator);
        }
        if (menuItemName != null) {
            map.put(Action.NAME, menuItemName);
        }
        if (!map.isEmpty()) {
            ActionUtilities.decorateAction(action, map);
        }

        return ActionUtilities.createMenuItem(action);
    }

    private JMenu buildMenu(String name)
    {
        return new JMenu(name);
    }

    private JToggleButton makeJToggleButton(Action currentAction, String iconPath, String tooltipText)
    {
        JToggleButton jToggleButton;
        if (currentAction != null) {
            jToggleButton = new JToggleButton(currentAction);
        } 
        else {
            jToggleButton = new JToggleButton();
        }
        return (JToggleButton) buttonCommon(jToggleButton, iconPath, tooltipText);
    }

    private JButton makeButton(Action action, String iconPath, String tooltipText) 
    {
        JButton button;
        if (action != null) {
            button = new JButton(action);
        } 
        else {
            button = new JButton();
        }
        return (JButton) buttonCommon(button, iconPath, tooltipText);
    }

    private AbstractButton buttonCommon(AbstractButton button, String iconPath, String tooltipText) 
    {
        button.setIcon(new ImageIcon(getClass().getClassLoader().getResource(iconPath)));
        button.setToolTipText(tooltipText);
        button.setBorder(BorderFactory.createEtchedBorder());
        button.setText(null);
        return button;
    }

    private JLabel makeJLabel(String iconPath, String tooltipText) 
    {
        JLabel label = new JLabel(new ImageIcon(getClass().getClassLoader().getResource(iconPath)));
        label.setToolTipText(tooltipText);
        return label;
    }

    private void setupToolbar() 
    {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        metadataLabel = new JLabel("Metadata: ");
        metadataLabel.setToolTipText("Edit Event Graph Metadata");
        
        JButton metadataButton = makeButton(null, "viskit/images/Information24.gif",
                "Edit Event Graph Metadata");
        metadataButton.addActionListener((ActionEvent e) -> {
            ((EventGraphController) getController()).editGraphMetadata();
        });

        // Buttons for what mode we are in

        addEventLabel = makeJLabel("viskit/images/eventNode.png",
                "Drag onto canvas to add new events to the event graph");
        addEventLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        addEventLabel.setIcon(new EventNodeIcon());

        addSelfReferentialEdgeLabel = makeJLabel("viskit/images/selfConnectingEdge.png",
                "Drag onto an existing event node to add a self-referential scheduling edge");
        addSelfReferentialEdgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        addSelfCancellingEdgeLabel = makeJLabel("viskit/images/selfCancellingEdge.png",
                "Drag onto an existing event node to add a self-referential canceling edge");
        addSelfCancellingEdgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        selectModeButton = makeJToggleButton(null, "viskit/images/selectNode.png",
                "Select an item on the graph");

        schedulingEdgeModeButton = makeJToggleButton(null, "viskit/images/schedulingEdge.png",
                "Connect centers of two nodes with a scheduling edge");
        schedulingEdgeModeButton.setIcon(new SchedulingEdgeIcon());

        cancellingEdgeModeButton = makeJToggleButton(null, "viskit/images/cancellingEdge.png",
                "Connect centers of two nodes nodes with a cancelling edge");
        cancellingEdgeModeButton.setIcon(new CancellingEdgeIcon());

        modeButtonGroup.add(selectModeButton);
        modeButtonGroup.add(schedulingEdgeModeButton);
        modeButtonGroup.add(cancellingEdgeModeButton);

        JButton zoomInButton = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in towards the graph");

        JButton zoomOutButton = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out from the graph");

        // Make selection mode the default mode
        selectModeButton.setSelected(true);

        metadataLabel = new JLabel("Metadata: ");
        metadataLabel.setToolTipText("Edit event graph metadata");
        getToolBar().add(metadataLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(metadataButton);
        getToolBar().addSeparator(new Dimension(24, 24));

        addLabel = new JLabel("Add: ");
        addLabel.setToolTipText("Add an item");
        getToolBar().add(addLabel);
        getToolBar().add(addEventLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addSelfReferentialEdgeLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addSelfCancellingEdgeLabel);

        getToolBar().addSeparator(new Dimension(24, 24));

        modeLabel = new JLabel("Mode: ");
        modeLabel.setToolTipText("Select editing mode");
        getToolBar().add(modeLabel);
        getToolBar().add(selectModeButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(schedulingEdgeModeButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(cancellingEdgeModeButton);
        getToolBar().addSeparator(new Dimension(24, 24));

        zoomLabel = new JLabel("Zoom: ");
        zoomLabel.setToolTipText("Zoom in or out");
        getToolBar().add(zoomLabel);
        getToolBar().add(zoomInButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOutButton);

        // Let the opening of Event Graphs make this visible
        getToolBar().setVisible(false);

        zoomInButton.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setScale(getCurrentVgraphComponentWrapper().getScale() + 0.1d);
        });
        zoomOutButton.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setScale(Math.max(getCurrentVgraphComponentWrapper().getScale() - 0.1d, 0.1d));
        });

        TransferHandler th = new TransferHandler("text");
        DragMouseAdapter dma = new DragMouseAdapter();
        addEventLabel.setTransferHandler(th);
        addEventLabel.addMouseListener(dma);
        addSelfReferentialEdgeLabel.setTransferHandler(th);
        addSelfReferentialEdgeLabel.addMouseListener(dma);
        addSelfCancellingEdgeLabel.setTransferHandler(th);
        addSelfCancellingEdgeLabel.addMouseListener(dma);

        // These buttons perform operations that are internal to our view class, and therefore their operations are
        // not under control of the application controller (EventGraphControllerImpl.java).  Small, simple anonymous inner classes
        // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        selectModeButton.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setPortsVisible(false);
        });
        schedulingEdgeModeButton.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setPortsVisible(true);
        });
        cancellingEdgeModeButton.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setPortsVisible(true);
        });
    }

    /** Changes the background/foreground color of Event Graph tabs depending on
     * model.isDirty() status giving the user an indication of a good/bad
     * save and compile operation. Of note is that the default Look+Feel must be
     * selected for WIN machines, else no colors will be visible. On Macs, the
     * platform Look+Feel works best.
     */
    public void toggleEventGraphStatusIndicators()
    {
        int originalSelectedTabIndex = tabbedPane.getSelectedIndex();
        for (Component currentSwingComponent : tabbedPane.getComponents()) 
        {
            // This will fire a call to stateChanged() which also sets the current model
            tabbedPane.setSelectedComponent(currentSwingComponent);
            // TODO tooltip text hints not appearing
            if (((Model) getModel()).isModelDirty())
            {
                // background changes seem excessive
//                tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.RED.brighter());

                if (LOOK_AND_FEEL != null && !LOOK_AND_FEEL.isEmpty() && LOOK_AND_FEEL.toLowerCase().equals("default"))
                    tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.RED.darker());
                tabbedPane.setToolTipText("Problem with event graph model validation and compilation");
            } 
            else 
            {
                // background changes seem excessive
//                tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), Color.GREEN.brighter());

                if (LOOK_AND_FEEL != null && !LOOK_AND_FEEL.isEmpty() && LOOK_AND_FEEL.toLowerCase().equals("default"))
                    tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), Color.GREEN.darker());
                tabbedPane.setToolTipText("Successful event graph model validation and compilation");
            }
        }
        // Restore active tab and model by virtue of firing a call to stateChanged()
        tabbedPane.setSelectedIndex(originalSelectedTabIndex);
    }

    /** Some private classes to implement Drag and Drop (DnD) and dynamic cursor update */
    class CursorHandler extends MouseAdapter {

        Cursor select;
        Cursor arc;
        Cursor cancel;

        CursorHandler() {
            super();
            select = Cursor.getDefaultCursor();
            arc = new Cursor(Cursor.CROSSHAIR_CURSOR);
            Image img = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/cancellingEdgeCursor.png")).getImage();

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
            new Thread(new CursorBuilder(img)).start();
        }

        class CursorBuilder implements Runnable, ImageObserver {

            Image img;

            CursorBuilder(Image img) {
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
                    getCurrentVgraphComponentWrapper().setCursor(select);
                    break;
                case SCHEDULING_EDGE_MODE:
                    getCurrentVgraphComponentWrapper().setCursor(arc);
                    break;
                case CANCELLING_EDGE_MODE:
                    getCurrentVgraphComponentWrapper().setCursor(cancel);
                    break;
                default:
                    getCurrentVgraphComponentWrapper().setCursor(select);
                    break;
            }
        }
    }

    final static int NODE_DRAG = 0;
    final static int SELF_REF_DRAG = 1;
    final static int SELF_REF_CANCEL_DRAG = 2;
    private int dragger;

    /** Class to support dragging and dropping on the jGraph pallette */
    class DragMouseAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            JComponent c = (JComponent) e.getSource();
            if (c == EventGraphViewFrame.this.addSelfReferentialEdgeLabel) {
                dragger = SELF_REF_DRAG;
            } else if (c == EventGraphViewFrame.this.addSelfCancellingEdgeLabel) {
                dragger = SELF_REF_CANCEL_DRAG;
            } else {
                dragger = NODE_DRAG;
            }

            TransferHandler handler = c.getTransferHandler();
            handler.exportAsDrag(c, e, TransferHandler.COPY);
        }
    }

    /** Class to facilitate dragging new nodes, or self-referential edges onto nodes on the jGraph pallette */
    class EventGraphDropTargetAdapter extends DropTargetAdapter {

        @Override
        public void dragOver(DropTargetDragEvent e) {

            // NOTE: this action is very critical in getting JGraph 5.14 to
            // signal the drop method
            e.acceptDrag(e.getDropAction());
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            Point p = e.getLocation();  // subtract the size of the label

            // get the node in question from the jGraph
            Object o = getCurrentVgraphComponentWrapper().getViskitElementAt(p);

            switch (dragger) {
                case NODE_DRAG:
                    Point pp = new Point(
                            p.x - addEventLabel.getWidth(),
                            p.y - addEventLabel.getHeight());
                    ((EventGraphController) getController()).buildNewEventNode(pp);
                    break;
                case SELF_REF_CANCEL_DRAG:
                    if (o != null && o instanceof EventNode) {
                        EventNode en = (EventNode) o;
                        // We're making a self-referential arc
                        ((EventGraphController) getController()).buildNewCancelingEdge(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                    }
                    break;
                default:
                    if (o != null && o instanceof EventNode) {
                        EventNode en = (EventNode) o;
                        // We're making a self-referential arc
                        ((EventGraphController) getController()).buildNewSchedulingEdge(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                    }
                    break;
            }
        }
    }
    private JFileChooser openSaveChooser;

    private JFileChooser buildOpenSaveChooser() {

        // Try to open in the current project directory for EventGraphs
        if (ViskitGlobals.instance().getViskitProject() != null) {
            return new JFileChooser(ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory());
        } else {
            return new JFileChooser(new File(ViskitProject.VISKIT_PROJECTS_DIRECTORY));
        }
    }

    @Override
    public File[] openFilesAsk()
    {
        openSaveChooser = buildOpenSaveChooser();
        openSaveChooser.setDialogTitle("Open Event Graph Files");

        // Bug fix: 1246
        openSaveChooser.addChoosableFileFilter(new EventGraphFileFilter(
                new String[] {"assembly", "smal", "xml", "x3d", "x3dv", "java", "class"}));

        // Bug fix: 1249
        openSaveChooser.setMultiSelectionEnabled(true);

        int retv = openSaveChooser.showOpenDialog(this);
        return (retv == JFileChooser.APPROVE_OPTION) ? openSaveChooser.getSelectedFiles() : null;
    }

    /** Open an already existing Viskit Project.  Called via reflection from
     * the Actions library.
     */
    public void openProject() {
        ViskitGlobals.instance().getAssemblyEditorViewFrame().openProject();
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
    public File saveFileAsk(String suggestedName, boolean showUniqueName, String title)
    {
        if (openSaveChooser == null) {
            openSaveChooser = buildOpenSaveChooser();
        }
        openSaveChooser.setDialogTitle(title);

        File suggestedFile = new File(ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory(), suggestedName);
        if (!suggestedFile.getParentFile().isDirectory()) {
            suggestedFile.getParentFile().mkdirs();
        }
        if (showUniqueName) {
            suggestedFile = getUniqueName(suggestedName, suggestedFile.getParentFile());
        }
        openSaveChooser.setSelectedFile(suggestedFile);
        int retv = openSaveChooser.showSaveDialog(this);
        if (retv == JFileChooser.APPROVE_OPTION) 
        {
            if (openSaveChooser.getSelectedFile().exists())
            {
                String message = "Confirm: overwrite " + openSaveChooser.getSelectedFile().getName() + "?";
                if (JOptionPane.YES_OPTION != ViskitGlobals.instance().getMainFrame().genericAskYesNo("File Exists",  message)) {
                    return null;
                }
            }
            try {
                LOG.info("Saved file as\n      {}", openSaveChooser.getSelectedFile().getCanonicalPath());
            }
            catch (IOException ioe)
            {
                // logic error, chooser indicated OK?
                LOG.error("Saved file suggested path chooser problem for {}", suggestedName);
            }
            return openSaveChooser.getSelectedFile();
        }

        // We canceled
        deleteCanceledSave(suggestedFile.getParentFile());
        openSaveChooser = null;
        return null;
    }

    /** Handles a canceled new Event Graph file creation
     *
     * @param file to candidate Event Graph file
     */
    private void deleteCanceledSave(File file) {
        ViskitGlobals.instance().getAssemblyEditorViewFrame().deleteCanceledSave(file);
    }

    @Override
    public File openRecentFilesAsk(Collection<String> lis) {
        return ViskitGlobals.instance().getAssemblyEditorViewFrame().openRecentFilesAsk(lis);
    }

    @Override
    public boolean doEditNode(EventNode node) {
        selectModeButton.doClick();     // always go back into select mode
        return EventNodeInspectorDialog.showDialog(ViskitGlobals.instance().getMainFrame(), node); // blocks
    }

    @Override
    public boolean doEditEdge(Edge edge) {
        selectModeButton.doClick();     // always go back into select mode
        return EdgeInspectorDialog.showDialog(ViskitGlobals.instance().getMainFrame(), edge); // blocks
    }

    @Override
    public boolean doEditCancelEdge(Edge edge) {
        return doEditEdge(edge); // blocks
    }

    @Override
    public boolean doEditParameter(ViskitParameter param) {
        return ParameterDialog.showDialog(ViskitGlobals.instance().getMainFrame(), param);    // blocks
    }

    @Override
    public boolean doEditStateVariable(ViskitStateVariable var) {
        return StateVariableDialog.showDialog(ViskitGlobals.instance().getMainFrame(), var);
    }

    // TODO duplicate:
    @Override
    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }

    @Override
    public void modelChanged(MvcModelEvent modelEvent) 
    {
        ViskitGraphComponentWrapper vgcw = getCurrentVgraphComponentWrapper();
        ParametersPanel parametersPanel = vgcw.parametersPanel;
        StateVariablesPanel stateVariablesPanel = vgcw.stateVariablesPanel;
        switch (modelEvent.getID()) 
        {
            // Changes the two side panels need to know about
            case ModelEvent.SIM_PARAMETER_ADDED:
                parametersPanel.addRow((ViskitElement) modelEvent.getSource());
                break;
            case ModelEvent.SIM_PARAMETER_DELETED:
                parametersPanel.removeRow((ViskitElement) modelEvent.getSource());
                break;
            case ModelEvent.SIM_PARAMETER_CHANGED:
                parametersPanel.updateRow((ViskitElement) modelEvent.getSource());
                break;
            case ModelEvent.STATE_VARIABLE_ADDED:
                stateVariablesPanel.addRow((ViskitElement) modelEvent.getSource());
                break;
            case ModelEvent.STATE_VARIABLE_DELETED:
                stateVariablesPanel.removeRow((ViskitElement) modelEvent.getSource());
                break;
            case ModelEvent.STATE_VARIABLE_CHANGED:
                stateVariablesPanel.updateRow((ViskitElement) modelEvent.getSource());
                break;
            case ModelEvent.CODEBLOCK_CHANGED:
                vgcw.codeBlockPanel.setData((String) modelEvent.getSource());
                break;
            case ModelEvent.NEW_MODEL:
                stateVariablesPanel.setData(null);
                parametersPanel.setData(null);

                // Deliberate fallthrough here. See default note

            // Changes that jGraph needs to know about
            default:
                vgcw.viskitModelChanged((ModelEvent) modelEvent);
                break;
        }

        // Let model.isDirty() determine status color
        toggleEventGraphStatusIndicators();
    }

    /**
     * @return the recentEventGraphFileListener
     */
    @SuppressWarnings("NonPublicExported")
    public RecentEventGraphFileListener getRecentEventGraphFileListener() {
        return recentEventGraphFileListener;
    }

    /**
     * @return the eventGraphMenu
     */
    public JMenu getEventGraphMenu()
    {
        return eventGraphMenu;
    }

} // end class file EventGraphViewFrame.java
