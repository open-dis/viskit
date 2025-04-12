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
import viskit.Help;
import viskit.model.ModelEvent;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitProject;
import static viskit.ViskitStatics.DESCRIPTION_HINT;
import static viskit.control.AssemblyControllerImpl.METHOD_captureWindow;
import static viskit.control.EventGraphControllerImpl.METHOD_close;
import static viskit.control.EventGraphControllerImpl.METHOD_closeAll;
import static viskit.control.EventGraphControllerImpl.METHOD_copy;
import static viskit.control.EventGraphControllerImpl.METHOD_cut;
import static viskit.control.EventGraphControllerImpl.METHOD_editGraphMetadata;
import static viskit.control.EventGraphControllerImpl.METHOD_generateJavaSource;
import static viskit.control.EventGraphControllerImpl.METHOD_newEventGraph;
import static viskit.control.EventGraphControllerImpl.METHOD_open;
import static viskit.control.EventGraphControllerImpl.METHOD_paste;
import static viskit.control.EventGraphControllerImpl.METHOD_redo;
import static viskit.control.EventGraphControllerImpl.METHOD_remove;
import static viskit.control.EventGraphControllerImpl.METHOD_save;
import static viskit.control.EventGraphControllerImpl.METHOD_saveAs;
import static viskit.control.EventGraphControllerImpl.METHOD_undo;
import static viskit.control.EventGraphControllerImpl.METHOD_viewXML;
import static viskit.control.EventGraphControllerImpl.METHOD_zipAndMailProject;
import viskit.images.CanArcIcon;
import viskit.images.EventNodeIcon;
import viskit.images.SchedArcIcon;
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
    public final static int ARC_MODE = 2;
    public final static int CANCEL_ARC_MODE = 3;
    public final static int SELF_REF_MODE = 4;
    public final static int SELF_REF_CANCEL_MODE = 5;

    private static final String FRAME_DEFAULT_TITLE = " Viskit Event Graph Editor";
    private static final String LOOK_AND_FEEL = ViskitUserPreferencesDialog.getLookAndFeel();

    /** Toolbar for dropping icons, connecting, etc. */
    private JToolBar toolBar;    // Mode buttons on the toolbar
    private JLabel addEvent;
    private JLabel addSelfRef;
    private JLabel addSelfCancelRef;
    private JToggleButton selectMode;
    private JToggleButton arcMode;
    private JToggleButton cancelArcMode;
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
        initMVC(mvcController);       // set up mvc linkages
        initializeUserInterface();    // build widgets
    }

    /** @return the JPanel which is the content of this JFrame */
    public JComponent getContent() {
        return (JComponent) getContentPane();
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
     * @param mvcController the controller for this view
     */
    private void initMVC(MvcController mvcController) {
        setController(mvcController);
    }

    /**
     * Initialize the user interface
     */
    private void initializeUserInterface() {

        // Layout menus
        buildEditMenu(); // must be first
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

    public ViskitGraphComponentWrapper getCurrentVgraphComponentWrapper() {
        JSplitPane jsplt = (JSplitPane) tabbedPane.getSelectedComponent();
        if (jsplt == null) {
            return null;
        }

        JScrollPane jsp = (JScrollPane) jsplt.getLeftComponent();
        return (ViskitGraphComponentWrapper) jsp.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent() {
        ViskitGraphComponentWrapper vcw = getCurrentVgraphComponentWrapper();
        if (vcw == null || vcw.drawingSplitPane == null) {return null;}
        return vcw.drawingSplitPane.getLeftComponent();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JToolBar toolBar) {
        this.toolBar = toolBar;
    }

    /**
     * @return the openRecentProjMenu
     */
    public JMenu getOpenRecentProjectMenu() 
    {
        return openRecentProjectMenu;
    }

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) 
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

    private void buildStateParamSplit(ViskitGraphComponentWrapper vgcw) {

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

        StateVariablesPanel vp = new StateVariablesPanel(300, 5);
        stateVariablesPanel.add(vp);
        stateVariablesPanel.add(Box.createVerticalStrut(5));
        stateVariablesPanel.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for stateVariable adds, deletes and edits and tell it we'll be doing adds and deletes
        vp.setEnableAddsAndDeletes(false);
        vp.addPlusListener(ActionIntrospector.getAction(getController(), "newStateVariable"));

        // Introspector can't handle a param to the method....?
        vp.addMinusListener((ActionEvent event) -> {
            ((EventGraphController) getController()).deleteStateVariable((ViskitStateVariable) event.getSource());
        });

        vp.addDoubleClickedListener((ActionEvent event) -> {
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
        
        JLabel instructions = new JLabel("Edit Properties or Ctrl-E to Edit");
        instructions.setToolTipText(DESCRIPTION_HINT);
        instructions.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        int bigSz = descriptionLabel.getFont().getSize();
        instructions.setFont(descriptionLabel.getFont().deriveFont(Font.ITALIC, (float) (bigSz - 2)));
        parametersPanel.add(instructions);
//        parametersPanel.add(Box.createVerticalStrut(5));

        JTextArea descriptionTA = new JTextArea();
        descriptionTA.setToolTipText(DESCRIPTION_HINT);
        descriptionTA.setEditable(false);
        descriptionTA.setText(DESCRIPTION_HINT); // initial value, hopefully overwritten by loaded value
        descriptionTA.setWrapStyleWord(true);
        descriptionTA.setLineWrap(true);
        descriptionTA.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JScrollPane descriptionSP = new JScrollPane(descriptionTA);
        descriptionSP.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        descriptionSP.setMinimumSize(new Dimension(20, 20));

        // This works, you just have to have several lines of typed text to cause
        // the etched scrollbar to appear
        parametersPanel.add(descriptionSP);
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

        ParametersPanel pp = new ParametersPanel(300, 5);
        pp.setMinimumSize(new Dimension(20, 20));

        // Wire handlers for parameter adds, deletes and edits and tell it we'll be doing adds and deletes
        pp.setEnableAddsAndDeletes(false);
        pp.addPlusListener(ActionIntrospector.getAction(getController(), "newSimulationParameter"));

        // Introspector can't handle a param to the method....?
        pp.addMinusListener((ActionEvent event) -> {
            ((EventGraphController) getController()).deleteSimParameter((ViskitParameter) event.getSource());
        });
        pp.addDoubleClickedListener((ActionEvent event) -> {
            ((EventGraphController) getController()).simParameterEdit((ViskitParameter) event.getSource());
        });

        parametersPanel.add(pp);
        parametersPanel.add(Box.createVerticalStrut(5));

        CodeBlockPanel codeblockPan = buildCodeBlockPanel();

        JSplitPane stateCblockSplt = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(stateVariablesPanel),
                new JScrollPane(buildCodeBlockComponent(codeblockPan)));
        stateCblockSplt.setResizeWeight(0.75);
        stateCblockSplt.setMinimumSize(new Dimension(20, 20));

        // Split pane that has description, parameters, state variables and code block.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                parametersPanel,
                stateCblockSplt);
        splitPane.setResizeWeight(0.75);
        splitPane.setMinimumSize(new Dimension(20, 20));

        vgcw.stateParameterSplitPane = splitPane;
        vgcw.paramPan = pp;
        vgcw.varPan = vp;
        vgcw.codeBlockPan = codeblockPan;
    }

    private CodeBlockPanel buildCodeBlockPanel() {
        CodeBlockPanel cbp = new CodeBlockPanel(this, true, "Event Graph Code Block");
        cbp.addUpdateListener((ActionEvent e) -> {
            String s = (String) e.getSource();
            if (s != null) {
                ((EventGraphController) getController()).codeBlockEdit(s);
            }
        });
        return cbp;
    }

    private JComponent buildCodeBlockComponent(CodeBlockPanel codeBlockPanel) {
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
        boolean nullString = !(newName != null && newName.length() > 0); // TODO ! isBlank()
        if  ((!nullString) && (tabbedPane != null))
             tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), newName);
    }

    @Override
    public void setSelectedEventGraphDescription(String description) {
        JSplitPane jsp = getCurrentVgraphComponentWrapper().stateParameterSplitPane;
        JPanel jp = (JPanel) jsp.getTopComponent();
        Component[] components = jp.getComponents();
        for (Component c : components) {
            if (c instanceof JScrollPane) {
                c = ((JScrollPane) c).getViewport().getComponent(0);
                ((JTextComponent) c).setText(description);
            }
        }
    }

    int untitledCount = 0;

    @Override
    public void addTab(Model mod) {
        ViskitGraphModel vmod = new ViskitGraphModel();
        ViskitGraphComponentWrapper graphPane = new ViskitGraphComponentWrapper(vmod, this);
        vmod.setjGraph(graphPane);
        graphPane.model = mod;

        buildStateParamSplit(graphPane);

        // Split pane with the canvas on the left and a split pane with state variables and parameters on the right.
        JScrollPane jsp = new JScrollPane(graphPane);

        graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp, graphPane.stateParameterSplitPane);

        // This is the key to getting the jgraph half to come up appropriately
        // wide by giving the left component (JGraph side) most of the usable
        // extra space in this SplitPlane -> 75%
        graphPane.drawingSplitPane.setResizeWeight(0.75);
        graphPane.drawingSplitPane.setOneTouchExpandable(true);

        graphPane.addMouseListener(new vCursorHandler());
        try {
            graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
        } catch (TooManyListenersException tmle) {
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
    public void deleteTab(Model mod) {
        JSplitPane jsplt;
        JScrollPane jsp;
        ViskitGraphComponentWrapper viskitGraphComponentWrapper;
        Runnable r;
        for (Component c : tabbedPane.getComponents()) {
            jsplt = (JSplitPane) c;
            jsp = (JScrollPane) jsplt.getLeftComponent();
            viskitGraphComponentWrapper = (ViskitGraphComponentWrapper) jsp.getViewport().getComponent(0);
            if (viskitGraphComponentWrapper.model == mod) {
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
    public Model[] getOpenModels() {
        JSplitPane jsplt;
        JScrollPane jsp;
        ViskitGraphComponentWrapper vgcw;
        Component[] ca = tabbedPane.getComponents();
        Model[] vm = new Model[ca.length];
        for (int i = 0; i < vm.length; i++) {
            jsplt = (JSplitPane) ca[i];
            jsp = (JScrollPane) jsplt.getLeftComponent();
            vgcw = (ViskitGraphComponentWrapper) jsp.getViewport().getComponent(0);
            vm[i] = vgcw.model;
        }
        return vm;
    }

    @Override
    public String addParameterDialog() {

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
    public String addStateVariableDialog() {
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
        public void listChanged() 
        {
            String nameOnly;
            Action currentAction;
            JMenuItem mi;
            EventGraphController eventGraphController = (EventGraphController) getController();
            Set<String> files = eventGraphController.getRecentEventGraphFileSet();
            openRecentEventGraphMenu.removeAll(); // clear prior to rebuilding menu
            openRecentEventGraphMenu.setEnabled(false); // disable unless file is found
            File file;
            for (String fullPath : files) 
            {
                file = new File(fullPath);
                if (!file.exists())
                {
                    // file not found as expected, something happened externally and so report it
                    LOG.error("*** [EventGraphViewFrame listChanged] Event graph file not found: " + file.getPath());
                    continue; // actual file not found, skip to next file in files loop
                }
                nameOnly = file.getName();
                currentAction = new ParameterizedAction(nameOnly);
                currentAction.putValue(ViskitStatics.FULL_PATH, fullPath);
                mi = new JMenuItem(currentAction);
                mi.setToolTipText(file.getPath());
                openRecentEventGraphMenu.add(mi);
                openRecentEventGraphMenu.setEnabled(true); // at least one is found
            }
            if (!files.isEmpty()) 
            {
                openRecentEventGraphMenu.add(new JSeparator());
                currentAction = new ParameterizedAction("clear history");
                currentAction.putValue(ViskitStatics.FULL_PATH, ViskitStatics.CLEAR_PATH_FLAG);  // flag
                mi = new JMenuItem(currentAction);
                mi.setToolTipText("Clear this list");
                openRecentEventGraphMenu.add(mi);
            }
            // TODO note that some items might remain loaded after "clear menu" and so wondering if that is ambiguous
        }
    }

    class ParameterizedAction extends javax.swing.AbstractAction {

        ParameterizedAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) 
        {
            EventGraphController eventGraphController = (EventGraphController) getController();

            File fullPath;
            Object obj = getValue(ViskitStatics.FULL_PATH);
            if (obj instanceof String)
                fullPath = new File((String) obj);
            else
                fullPath = (File) obj;

            if (fullPath != null && fullPath.getPath().equals(ViskitStatics.CLEAR_PATH_FLAG)) {
                eventGraphController.clearRecentEventGraphFileSet();
            } else {
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
        ActionIntrospector.getAction(eventGraphController, METHOD_cut).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_copy).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_paste).setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, METHOD_remove).setEnabled(false);
        editMenu.addSeparator();

        editMenu.add(buildMenuItem(eventGraphController, "newNode", "Add a new Event Node", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(eventGraphController, "newSimulationParameter", "Add a new Simulation Parameter", KeyEvent.VK_S, null));
        editMenu.add(buildMenuItem(eventGraphController, "newStateVariable", "Add a new State Variable", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(eventGraphController, "newSelfReferentialSchedulingEdge", "Add Self-Referential Scheduling Edge", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(eventGraphController, "newSelfReferentialCancelingEdge", "Add Self-Refenential Canceling Edge", KeyEvent.VK_A, null));

        // Thess start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, "newSelfReferentialSchedulingEdge").setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, "newSelfReferentialCancelingEdge").setEnabled(false);
        editMenu.addSeparator();

        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
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

        editEventGraphSubMenu = new JMenu("Edit selected Event Graph..."); // submenu
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

        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, "newNode", "Add a new Event Node", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, "newSimulationParameter", "Add a new Simulation Parameter", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, "newStateVariable", "Add a new State Variable", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, "newSelfReferentialSchedulingEdge", "Add Self-Referential Scheduling Edge", KeyEvent.VK_A, null));
        editEventGraphSubMenu.add(buildMenuItem(eventGraphController, "newSelfReferentialCancelingEdge", "Add Self-Refenential Canceling Edge", KeyEvent.VK_A, null));

        // Thess start off being disabled, until something is selected
        ActionIntrospector.getAction(eventGraphController, "newSelfReferentialSchedulingEdge").setEnabled(false);
        ActionIntrospector.getAction(eventGraphController, "newSelfReferentialCancelingEdge").setEnabled(false);
        
        // TODO "disable" both of these if no Event Graph is active
        eventGraphMenu.add(editEventGraphSubMenu);
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_editGraphMetadata, "Edit selected Event Graph Metadata Properties...", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));
        eventGraphMenu.addSeparator();

        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
        eventGraphMenu.add(buildMenuItem(eventGraphController, "newProject", "New Viskit Project", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK)));
        
        eventGraphMenu.add(buildMenuItem(this, "openProject", "Open Project", KeyEvent.VK_P,
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

        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_captureWindow, "Image Save for Event Graph Diagram", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod)));
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_generateJavaSource, "Java Source Generation for saved Event Graph", KeyEvent.VK_J,
                KeyStroke.getKeyStroke(KeyEvent.VK_J, accelMod)));
        eventGraphMenu.add(buildMenuItem(eventGraphController, METHOD_viewXML, "XML View of Saved Event Graph", KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));

        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
        eventGraphMenu.addSeparator();
        eventGraphMenu.add(buildMenuItem(eventGraphController, "showViskitUserPreferences", "Viskit User Preferences", KeyEvent.VK_V, null));
        
        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
        eventGraphMenu.addSeparator();

        eventGraphMenu.add(quitMenuItem = buildMenuItem(eventGraphController, "quit", "Quit", KeyEvent.VK_Q,
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, accelMod)));
        }
        }

        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(eventGraphMenu);
        myMenuBar.add(editMenu);
        
        Help help = new Help(ViskitGlobals.instance().getMainFrame());
        help.mainFrameLocated(ViskitGlobals.instance().getMainFrame().getBounds());
        ViskitGlobals.instance().setHelp(help); // single instance for all viskit frames

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        
        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
        helpMenu.add(buildMenuItem(help, "doContents", "Contents", KeyEvent.VK_C, null));
        helpMenu.add(buildMenuItem(help, "doSearch", "Search", KeyEvent.VK_S, null));
        helpMenu.addSeparator();

        helpMenu.add(buildMenuItem(help, "doTutorial", "Tutorial", KeyEvent.VK_T, null));
        helpMenu.add(buildMenuItem(help, "aboutViskit", "About Viskit", KeyEvent.VK_A, null));

        myMenuBar.add(helpMenu);
        }
    }
    
    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mnemonic, KeyStroke accelerator)
    {
        Action action = ActionIntrospector.getAction(source, method);
        if (action == null)
        {
            LOG.error("buildMenuItem reflection failed for name=" + name + " method=" + method + " in " + source.toString());
            return new JMenuItem(name + "(not working, reflection failed) ");
        }
        Map<String, Object> map = new HashMap<>();
        if (mnemonic != null) {
            map.put(Action.MNEMONIC_KEY, mnemonic);
        }
        if (accelerator != null) {
            map.put(Action.ACCELERATOR_KEY, accelerator);
        }
        if (name != null) {
            map.put(Action.NAME, name);
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

    private JToggleButton makeJToggleButton(Action currentAction, String iconPath, String tooltipText) {
        JToggleButton jToggleButton;
        if (currentAction != null) {
            jToggleButton = new JToggleButton(currentAction);
        } else {
            jToggleButton = new JToggleButton();
        }
        return (JToggleButton) buttonCommon(jToggleButton, iconPath, tooltipText);
    }

    private JButton makeButton(Action a, String iconPath, String tooltipText) {
        JButton b;
        if (a != null) {
            b = new JButton(a);
        } else {
            b = new JButton();
        }
        return (JButton) buttonCommon(b, iconPath, tooltipText);
    }

    private AbstractButton buttonCommon(AbstractButton button, String iconPath, String tooltipText) {
        button.setIcon(new ImageIcon(getClass().getClassLoader().getResource(iconPath)));
        button.setToolTipText(tooltipText);
        button.setBorder(BorderFactory.createEtchedBorder());
        button.setText(null);
        return button;
    }

    private JLabel makeJLabel(String iconPath, String tooltipText) {
        JLabel jlab = new JLabel(new ImageIcon(getClass().getClassLoader().getResource(iconPath)));
        jlab.setToolTipText(tooltipText);
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

        addSelfCancelRef = makeJLabel("viskit/images/selfCancelArc.png",
                "Drag onto an existing event node to add a self-referential canceling edge");
        addSelfCancelRef.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        selectMode = makeJToggleButton(null, "viskit/images/selectNode.png",
                "Select an item on the graph");

        arcMode = makeJToggleButton(null, "viskit/images/schedArc.png",
                "Connect nodes with a scheduling edge");
        arcMode.setIcon(new SchedArcIcon());

        cancelArcMode = makeJToggleButton(null, "viskit/images/canArc.png",
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
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(addSelfCancelRef);

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

        // Let the opening of Event Graphs make this visible
        getToolBar().setVisible(false);

        zoomIn.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setScale(getCurrentVgraphComponentWrapper().getScale() + 0.1d);
        });
        zoomOut.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setScale(Math.max(getCurrentVgraphComponentWrapper().getScale() - 0.1d, 0.1d));
        });

        TransferHandler th = new TransferHandler("text");
        DragMouseAdapter dma = new DragMouseAdapter();
        addEvent.setTransferHandler(th);
        addEvent.addMouseListener(dma);
        addSelfRef.setTransferHandler(th);
        addSelfRef.addMouseListener(dma);
        addSelfCancelRef.setTransferHandler(th);
        addSelfCancelRef.addMouseListener(dma);

        // These buttons perform operations that are internal to our view class, and therefore their operations are
        // not under control of the application controller (EventGraphControllerImpl.java).  Small, simple anonymous inner classes
        // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        selectMode.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setPortsVisible(false);
        });
        arcMode.addActionListener((ActionEvent e) -> {
            getCurrentVgraphComponentWrapper().setPortsVisible(true);
        });
        cancelArcMode.addActionListener((ActionEvent e) -> {
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
        int selectedTab = tabbedPane.getSelectedIndex();
        for (Component currentSwingComponent : tabbedPane.getComponents()) 
        {
            // This will fire a call to stateChanged() which also sets the current model
            tabbedPane.setSelectedComponent(currentSwingComponent);
            // TODO tooltip text hints not appearing
            if (((Model) getModel()).isDirty())
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
            Image img = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/canArcCursor.png")).getImage();

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

        @Override
        public void mouseEntered(MouseEvent e) {
            switch (getCurrentMode()) {
                case SELECT_MODE:
                    getCurrentVgraphComponentWrapper().setCursor(select);
                    break;
                case ARC_MODE:
                    getCurrentVgraphComponentWrapper().setCursor(arc);
                    break;
                case CANCEL_ARC_MODE:
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
            if (c == EventGraphViewFrame.this.addSelfRef) {
                dragger = SELF_REF_DRAG;
            } else if (c == EventGraphViewFrame.this.addSelfCancelRef) {
                dragger = SELF_REF_CANCEL_DRAG;
            } else {
                dragger = NODE_DRAG;
            }

            TransferHandler handler = c.getTransferHandler();
            handler.exportAsDrag(c, e, TransferHandler.COPY);
        }
    }

    /** Class to facilitate dragging new nodes, or self-referential edges onto nodes on the jGraph pallette */
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

            // get the node in question from the jGraph
            Object o = getCurrentVgraphComponentWrapper().getViskitElementAt(p);

            switch (dragger) {
                case NODE_DRAG:
                    Point pp = new Point(
                            p.x - addEvent.getWidth(),
                            p.y - addEvent.getHeight());
                    ((EventGraphController) getController()).buildNewNode(pp);
                    break;
                case SELF_REF_CANCEL_DRAG:
                    if (o != null && o instanceof EventNode) {
                        EventNode en = (EventNode) o;
                        // We're making a self-referential arc
                        ((EventGraphController) getController()).buildNewCancelingArc(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
                    }
                    break;
                default:
                    if (o != null && o instanceof EventNode) {
                        EventNode en = (EventNode) o;
                        // We're making a self-referential arc
                        ((EventGraphController) getController()).buildNewSchedulingArc(new Object[]{en.opaqueViewObject, en.opaqueViewObject});
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
        ViskitGlobals.instance().getAssemblyViewFrame().openProject();
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
        if (retv == JFileChooser.APPROVE_OPTION) {
            if (openSaveChooser.getSelectedFile().exists()) {
                if (JOptionPane.YES_OPTION != ViskitGlobals.instance().getMainFrame().genericAskYesNo("File Exists",  "Overwrite? Confirm")) {
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
        ViskitGlobals.instance().getAssemblyViewFrame().deleteCanceledSave(file);
    }

    @Override
    public File openRecentFilesAsk(Collection<String> lis) {
        return ViskitGlobals.instance().getAssemblyViewFrame().openRecentFilesAsk(lis);
    }

    @Override
    public boolean doEditNode(EventNode node) {
        selectMode.doClick();     // always go back into select mode
        return EventNodeInspectorDialog.showDialog(ViskitGlobals.instance().getMainFrame(), node); // blocks
    }

    @Override
    public boolean doEditEdge(Edge edge) {
        selectMode.doClick();     // always go back into select mode
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
    public void modelChanged(MvcModelEvent event) {
        ViskitGraphComponentWrapper vgcw = getCurrentVgraphComponentWrapper();
        ParametersPanel pp = vgcw.paramPan;
        StateVariablesPanel vp = vgcw.varPan;
        switch (event.getID()) {
            // Changes the two side panels need to know about
            case ModelEvent.SIM_PARAMETER_ADDED:
                pp.addRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.SIM_PARAMETER_DELETED:
                pp.removeRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.SIM_PARAMETER_CHANGED:
                pp.updateRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATE_VARIABLE_ADDED:
                vp.addRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATE_VARIABLE_DELETED:
                vp.removeRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.STATE_VARIABLE_CHANGED:
                vp.updateRow((ViskitElement) event.getSource());
                break;
            case ModelEvent.CODEBLOCK_CHANGED:
                vgcw.codeBlockPan.setData((String) event.getSource());
                break;
            case ModelEvent.NEW_MODEL:
                vp.setData(null);
                pp.setData(null);

                // Deliberate fallthrough here. See default note

            // Changes that jGraph needs to know about
            default:
                vgcw.viskitModelChanged((ModelEvent) event);
                break;
        }

        // Let model.isDirty() determine status color
        toggleEventGraphStatusIndicators();
    }

    /**
     * @return the recentEventGraphFileListener
     */
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
