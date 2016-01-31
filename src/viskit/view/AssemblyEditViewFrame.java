/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
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

import viskit.jgraph.VgraphAssemblyComponentWrapper;
import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.LogUtils;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import viskit.control.AssemblyController;
import viskit.control.AssemblyControllerImpl;
import viskit.util.FileBasedAssemblyNode;
import viskit.Help;
import viskit.model.ModelEvent;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitProject;
import viskit.control.RecentProjectFileSetListener;
import viskit.doe.LocalBootLoader;
import viskit.images.AdapterIcon;
import viskit.images.PropertyChangeListenerImageIcon;
import viskit.images.PropertyChangeListenerIcon;
import viskit.images.SimEventListenerIcon;
import viskit.jgraph.vGraphAssemblyModel;
import viskit.model.*;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcController;
import viskit.mvc.mvcModelEvent;
import viskit.mvc.mvcRecentFileListener;
import viskit.util.AssemblyFileFilter;
import viskit.view.dialog.EventGraphNodeInspectorDialog;
import viskit.view.dialog.RecentFilesDialog;
import viskit.view.dialog.SimEventListenerConnectionInspectorDialog;
import viskit.view.dialog.UserPreferencesDialog;
import viskit.view.dialog.PropertyChangeListenerNodeInspectorDialog;
import viskit.view.dialog.AdapterConnectionInspectorDialog;
import viskit.view.dialog.PclEdgeInspectorDialog;

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
public class AssemblyEditViewFrame extends mvcAbstractJFrameView implements AssemblyView, DragStartListener {

    /** Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc. */
    public static final int SELECT_MODE = 0;
    public static final int ADAPTER_MODE = 1;
    public static final int SIMEVLIS_MODE = 2;
    public static final int PCL_MODE = 3;

    // The view needs access to this
    public JButton runButton;

    JMenu openRecentAssemblyMenu, openRecentProjectMenu;

    private final static String FRAME_DEFAULT_TITLE = "Assembly Editor";

    private final String FULLPATH = ViskitStatics.FULL_PATH;
    private final String CLEARPATHFLAG = ViskitStatics.CLEAR_PATH_FLAG;
    private final Color background = new Color(0xFB, 0xFB, 0xE5);

    /** Toolbar for dropping icons, connecting, etc. */
    private JTabbedPane tabbedPane;
    private JToolBar toolBar;
    private JToggleButton selectMode;
    private JToggleButton adapterMode,  simEventListenerMode,  propertyChangeListenerMode;
    private LegoTree legoTree,  propertyChangeListenerTree;
    private JMenuBar myMenuBar;
    private JMenuItem quitMenuItem;
    private RecentProjectFileSetListener recentProjectFileSetListener;

    private int untitledCount = 0;
	
    private JMenu fileMenu, editMenu, helpMenu;
	
	private AssemblyController assemblyController;
    private int menuShortcutKeyMask;

    public AssemblyEditViewFrame(mvcController controller) {
        super(FRAME_DEFAULT_TITLE);
        initializeMVC(controller);   // set up mvc linkages
        initializeUserInterface();   // build widgets
    }

    public JComponent getContent() {
        return (JComponent) getContentPane();
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return quitMenuItem;
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
        if (assemblyController == null)
		{
			assemblyController = (AssemblyController) getController();
			assemblyController.addRecentAssemblyFileSetListener(new RecentAssemblyFileListener());
			menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		}
		fileMenu = new JMenu("Assemblies");
        fileMenu.setMnemonic(KeyEvent.VK_A);
		editMenu = new JMenu(FRAME_DEFAULT_TITLE);
		helpMenu = new JMenu("Help");

        buildMenus();
        buildToolbar();
       
        buildTreePanels();  // Build here to prevent NPE from eventGraphController

        // Set up a assemblyEditorContent level pane that will be the content pane. This
        // has a border layout, and contains the toolbar on the assemblyEditorContent and
        // the main splitpane underneath.

        // assemblyEditorContent level panel
        getContent().setLayout(new BorderLayout());
        getContent().add(getToolBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new TabSelectionHandler());

        getContent().add(tabbedPane, BorderLayout.CENTER);
        getContent().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    public VgraphAssemblyComponentWrapper getCurrentVgraphAssemblyComponentWrapper() {
        JSplitPane jsplt = (JSplitPane) tabbedPane.getSelectedComponent();
        if (jsplt == null) {
            return null;
        }

        JScrollPane jSP = (JScrollPane) jsplt.getRightComponent();
        return (VgraphAssemblyComponentWrapper) jSP.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent() {
        VgraphAssemblyComponentWrapper vcw = getCurrentVgraphAssemblyComponentWrapper();
        if (vcw == null || vcw.drawingSplitPane == null) {return null;}
        return vcw.drawingSplitPane.getRightComponent();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JToolBar toolBar) {
        this.toolBar = toolBar;
    }
	
	public Component getSelectedAssembly () // TODO stricter typing
	{
		return tabbedPane.getSelectedComponent();
	}

    /**
     * @return the recentProjectFileSetListener
     */
    public RecentProjectFileSetListener getRecentProjectFileSetListener() {
        return recentProjectFileSetListener;
    }

	/**
	 * @return the fileMenu
	 */
	public JMenu getFileMenu() {
		return fileMenu;
	}

	/**
	 * @return the editMenu
	 */
	public JMenu getEditMenu() {
		return editMenu;
	}

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            VgraphAssemblyComponentWrapper myVgacw = getCurrentVgraphAssemblyComponentWrapper();

            if (myVgacw == null) {     // last tab has been closed
                setSelectedAssemblyName(null);
                return;
            }

            // Key to getting the LEGOs tree panel in each tab view
            myVgacw.drawingSplitPane.setLeftComponent(myVgacw.trees);

            setModel((AssemblyModelImpl) myVgacw.assemblyModel); // hold on locally
            getController().setModel(getModel()); // tell controller
            AssemblyModelImpl mod = (AssemblyModelImpl) getModel();

            if (mod.getLastFile() != null) {
                ((AssemblyControllerImpl) getController()).initOpenAssemblyWatch(mod.getLastFile(), mod.getJaxbRoot());
            }

            GraphMetadata gmd = mod.getMetaData();
            if (gmd != null) {
                setSelectedAssemblyName(gmd.name);
            } else if (viskit.ViskitStatics.debug) {
                System.err.println("error: AssemblyViewFrame gmd null..");
            }
        }
    }

    class RecentAssemblyFileListener implements mvcRecentFileListener {

        @Override
        public void listChanged() {
            AssemblyController assemblyController = (AssemblyController) getController();
            Set<File> fileSet = assemblyController.getRecentAssemblyFileSet();
            openRecentAssemblyMenu.removeAll();
            for (File fullPath : fileSet) {
                if (!fullPath.exists()) {
                    continue;
                }
                String nameOnly = fullPath.getName();
                Action menuItemAction = new ParameterizedAssemblyAction(nameOnly);
                menuItemAction.putValue(FULLPATH, fullPath);
                JMenuItem menuItem = new JMenuItem(menuItemAction);
                menuItem.setToolTipText(fullPath.getPath());
                openRecentAssemblyMenu.add(menuItem);
            }
            if (fileSet.size() > 0) {
                openRecentAssemblyMenu.add(new JSeparator());
                Action menuItemAction = new ParameterizedAssemblyAction("clear");
                menuItemAction.putValue(FULLPATH, CLEARPATHFLAG);  // flag
                JMenuItem menuItem = new JMenuItem(menuItemAction);
                menuItem.setToolTipText("Clear this list");
                openRecentAssemblyMenu.add(menuItem);
            }
        }
    }

    class ParameterizedAssemblyAction extends javax.swing.AbstractAction {

        ParameterizedAssemblyAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            AssemblyController assemblyController = (AssemblyController) getController();

            File fullPath;
            Object obj = getValue(ViskitStatics.FULL_PATH);
            if (obj instanceof String)
                fullPath = new File((String) obj);
            else
                fullPath = (File) obj;

            if (fullPath.getPath().equals(CLEARPATHFLAG)) {
                assemblyController.clearRecentAssemblyFileList();
            } else {
                assemblyController.openRecentAssembly(fullPath);
				ViskitGlobals.instance().getViskitApplicationFrame().selectAssemblyEditorTab();
            }
			buildMenus (); // reset
        }
    }

    public void buildMenus() 
	{
        // Set up file menu
        fileMenu.removeAll(); // reset
		
        fileMenu.add(buildMenuItem(assemblyController, "newAssembly", "New Assembly", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask)));

        fileMenu.add(buildMenuItem(assemblyController, "open", "Open Assembly", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask)));
        fileMenu.add(openRecentAssemblyMenu = buildMenu("Open Recent Assembly"));
		openRecentAssemblyMenu.setMnemonic(KeyEvent.VK_R);
		openRecentAssemblyMenu.setEnabled(assemblyController.getRecentAssemblyFileSet().size() > 0);

        // The EGViewFrame will get this listener for it's menu item of the same name TODO confirm
        recentProjectFileSetListener = new RecentProjectFileSetListener();
        getRecentProjectFileSetListener().addMenuItem(openRecentProjectMenu);
        assemblyController.addRecentProjectFileSetListener(getRecentProjectFileSetListener());
        fileMenu.addSeparator();

        fileMenu.add(buildMenuItem(assemblyController, "save", "Save Assembly", KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask)));
        fileMenu.add(buildMenuItem(assemblyController, "saveAs", "Save Assembly as...", KeyEvent.VK_A, null));
        fileMenu.add(buildMenuItem(assemblyController, "close", "Close Assembly", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutKeyMask)));
        fileMenu.add(buildMenuItem(assemblyController, "closeAll", "Close All Assemblies", null, null));

        // TODO: Unknown as to what this does exactly
        fileMenu.add(buildMenuItem(assemblyController, "export2grid", "Export to Cluster Format", KeyEvent.VK_C, null));
        ActionIntrospector.getAction(assemblyController, "export2grid").setEnabled(false);

        // Set up edit menu
        fileMenu.removeAll(); // reset
        editMenu.setMnemonic(KeyEvent.VK_A);
        editMenu.add(buildMenuItem(assemblyController, "undo", "Undo", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcutKeyMask)));
        editMenu.add(buildMenuItem(assemblyController, "redo", "Redo", KeyEvent.VK_Y,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutKeyMask)));

        ActionIntrospector.getAction(assemblyController, "undo").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "redo").setEnabled(false);

        editMenu.addSeparator();
        // the next four are disabled until something is selected
        editMenu.add(buildMenuItem(assemblyController, "cut", "Cut", KeyEvent.VK_X,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask)));
        editMenu.getItem(editMenu.getItemCount()-1).setToolTipText("Cut is not supported in Viskit.");
        editMenu.add(buildMenuItem(assemblyController, "copy", "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask)));
        editMenu.add(buildMenuItem(assemblyController, "paste", "Paste Events", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask)));
        editMenu.add(buildMenuItem(assemblyController, "remove", "Delete", KeyEvent.VK_DELETE,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, menuShortcutKeyMask)));

        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(assemblyController, "cut").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "remove").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "copy").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "paste").setEnabled(false);
        editMenu.addSeparator();

        editMenu.add(buildMenuItem(assemblyController, "newEventGraphNode", "Add Event Graph...", KeyEvent.VK_G, null));
        editMenu.add(buildMenuItem(assemblyController, "newPropChangeListenerNode", "Add Property Change Listener...", KeyEvent.VK_L, null));

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(assemblyController, "showXML", "View Saved XML", KeyEvent.VK_X, null));
        editMenu.add(buildMenuItem(assemblyController, "generateJavaSource", "Generate, Compile Java Source", KeyEvent.VK_J,
                KeyStroke.getKeyStroke(KeyEvent.VK_J, menuShortcutKeyMask)));
        editMenu.add(buildMenuItem(assemblyController, "windowImageCapture", "Save Assembly Diagram",
                KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, menuShortcutKeyMask)));

        editMenu.addSeparator();
        editMenu.add(buildMenuItem(assemblyController, "editGraphMetaData", "Edit Properties...", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, menuShortcutKeyMask)));
        editMenu.addSeparator();
        editMenu.add(buildMenuItem(assemblyController, "compileAssemblyAndPrepareSimulationRunner", "Initialize Assembly",
                KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK)));

        // Create a new menu bar and add the created menus
		if (myMenuBar == null)
		{
			Help help = ViskitGlobals.instance().getHelp();
			helpMenu.setMnemonic(KeyEvent.VK_H);

			helpMenu.add(buildMenuItem(help, "doContents", "Contents", KeyEvent.VK_C, null));
			helpMenu.add(buildMenuItem(help, "doSearch", "Search", KeyEvent.VK_S, null));
			helpMenu.addSeparator();

			helpMenu.add(buildMenuItem(help, "doTutorial", "Tutorial", KeyEvent.VK_T, null));
			helpMenu.add(buildMenuItem(help, "aboutAssemblyEditor", "About...", KeyEvent.VK_A, null));
		
			myMenuBar = new JMenuBar();
			myMenuBar.add(fileMenu);
			myMenuBar.add(editMenu);
			myMenuBar.add(helpMenu);
		}
    }

    private JMenu buildMenu(String name) {
        return new JMenu(name);
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
        if (propertyChangeListenerMode.isSelected()) {
            return PCL_MODE;
        }
        LogUtils.getLogger(AssemblyEditViewFrame.class).error("assert false : \"getCurrentMode()\"");
        return 0;
    }

    private void buildToolbar() {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        // Buttons for what mode we are in

        selectMode = makeToolbarButton(null, "viskit/images/selectNode.png",
                "Select items on the graph");
        Border border = selectMode.getBorder();
        selectMode.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createLineBorder(Color.lightGray, 2)));

        adapterMode = makeToolbarButton(null, new AdapterIcon(24, 24),
                "Connect assembly nodes (event graph instances) with adapter pattern");
        border = adapterMode.getBorder();
        adapterMode.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        simEventListenerMode = makeToolbarButton(null, new SimEventListenerIcon(24, 24),
                "Connect assembly nodes (event graph instances) with a SimEvent listener pattern");
        border = simEventListenerMode.getBorder();
        simEventListenerMode.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        propertyChangeListenerMode = makeToolbarButton(null, new PropertyChangeListenerIcon(24, 24),
                "Connect a property change listener to a SimEntity");
        border = propertyChangeListenerMode.getBorder();
        propertyChangeListenerMode.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 2)));

        JButton zoomIn = makeButton(null, "viskit/images/ZoomIn24.gif",
                "Zoom in on the graph");

        JButton zoomOut = makeButton(null, "viskit/images/ZoomOut24.gif",
                "Zoom out on the graph");

        JButton saveButton = makeButton(null, "viskit/images/save.png",
                "Save and compile assembly (Ctrl-S)");
		saveButton.setSize(new Dimension (24, 24));

        Action runAction = ActionIntrospector.getAction(getController(), "compileAssemblyAndPrepSimRunner");
        runButton = makeButton(runAction, "viskit/images/Play24.gif",
                "Compile and initialize the Assembly, prepare for Simulation Run");
        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(adapterMode);
        modeButtonGroup.add(simEventListenerMode);
        modeButtonGroup.add(propertyChangeListenerMode);

        // Make selection mode the default mode
        selectMode.setSelected(true);

        getToolBar().add(new JLabel("Mode"));
        getToolBar().addSeparator(new Dimension(5, 24));

        getToolBar().add(selectMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(adapterMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(simEventListenerMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(propertyChangeListenerMode);

        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(new JLabel("Zoom"));
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomIn);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOut);
        getToolBar().addSeparator(new Dimension(24, 24));

		// right aligned
		JLabel initializeLabel = new JLabel("<html><p align='right'>Initialize Assembly<br /> for Simulation Run </p></html>");
		initializeLabel.setHorizontalAlignment(JLabel.RIGHT);
        initializeLabel.setToolTipText("Prepare selected Assembly for Simulation Run");
              runButton.setToolTipText("Prepare selected Assembly for Simulation Run");
        getToolBar().add(initializeLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
		runButton.setHorizontalAlignment(JButton.RIGHT);
        getToolBar().add(runButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        
		JLabel saveLabel = new JLabel(EventGraphViewFrame.saveCompileLabelText);
		saveLabel.setToolTipText(     EventGraphViewFrame.saveCompileLabelTooltip);		
		saveButton.setToolTipText(    EventGraphViewFrame.saveCompileLabelTooltip);

		saveLabel.setHorizontalAlignment(JLabel.RIGHT);
        getToolBar().add(saveLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
		saveButton.setHorizontalAlignment(JButton.RIGHT);
        getToolBar().add(saveButton);
        getToolBar().addSeparator(new Dimension(5, 24));

        // Let the opening of Assembliess make this visible
        getToolBar().setVisible(false);

        zoomIn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgraphAssemblyComponentWrapper().setScale(getCurrentVgraphAssemblyComponentWrapper().getScale() + 0.1d);
            }
        });
        zoomOut.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgraphAssemblyComponentWrapper().setScale(Math.max(getCurrentVgraphAssemblyComponentWrapper().getScale() - 0.1d, 0.1d));
            }
        });
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
			   AssemblyController assemblyController = (AssemblyController) getController();
               assemblyController.save();
            }
        });

        // These buttons perform operations that are internal to our view class, and therefore their operations are
        // not under control of the application controller (Controller.java).  Small, simple anonymous inner classes
        // such as these have been certified by the Surgeon General to be only minimally detrimental to code health.

        class PortsVisibleListener implements ActionListener {

            private final boolean tOrF;

            PortsVisibleListener(boolean tOrF) {
                this.tOrF = tOrF;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                getCurrentVgraphAssemblyComponentWrapper().setPortsVisible(tOrF);
            }
        }

        PortsVisibleListener portsOn = new PortsVisibleListener(true);
        PortsVisibleListener portsOff = new PortsVisibleListener(false);
        selectMode.addActionListener(portsOff);
        adapterMode.addActionListener(portsOn);
        simEventListenerMode.addActionListener(portsOn);
        propertyChangeListenerMode.addActionListener(portsOn);
    }

    private JToggleButton makeToolbarButton(Action a, String icPath, String tt) {
        JToggleButton jtb;
        if (a != null) {
            jtb = new JToggleButton(a);
        } else {
            jtb = new JToggleButton();
        }
        return (JToggleButton) buttonCommon(jtb, icPath, tt);
    }

    private JToggleButton makeToolbarButton(Action a, Icon ic, String tt) {
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
        b.setIcon(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource(icPath)));
        return buttonCommon2(b, tt);
    }

    private AbstractButton buttonCommon2(AbstractButton b, String tt) {
        b.setToolTipText(tt);
        b.setBorder(BorderFactory.createEtchedBorder());
        b.setText(null);
        return b;
    }

    @Override
    public void addTab(AssemblyModel mod) {
        vGraphAssemblyModel vGAmod = new vGraphAssemblyModel();
        VgraphAssemblyComponentWrapper graphPane = new VgraphAssemblyComponentWrapper(vGAmod, this);
        vGAmod.setjGraph(graphPane);                               // todo fix this

        graphPane.assemblyModel = mod;
        graphPane.trees = treePanels;
        graphPane.trees.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        graphPane.trees.setMinimumSize(new Dimension(20, 20));
        graphPane.trees.setDividerLocation(250);

        // Split pane with the canvas on the right and a split pane with LEGO tree and PCLs on the left.
        JScrollPane jscrp = new JScrollPane(graphPane);

        graphPane.drawingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPane.trees, jscrp);

        // This is the key to getting the jgraph half to come up appropriately
        // wide by giving the right component (JGraph side) most of the usable
        // extra space in this SplitPlane -> 25%
        graphPane.drawingSplitPane.setResizeWeight(0.25);
        graphPane.drawingSplitPane.setOneTouchExpandable(true);

        try {
            graphPane.getDropTarget().addDropTargetListener(new vDropTargetAdapter());
        } catch (TooManyListenersException tmle) {
            LogUtils.getLogger(AssemblyEditViewFrame.class).error(tmle);
        }

        // the view holds only one assemblyModel, so it gets overwritten with each tab
        // but this call serves also to register the view with the passed assemblyModel
        // by virtue of calling stateChanged()
        tabbedPane.add("untitled" + untitledCount++, graphPane.drawingSplitPane);
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane); // bring to front

        // Now expose the Assembly toolbar
        Runnable r = new Runnable() {
            @Override
            public void run() {
                getToolBar().setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void deleteTab(AssemblyModel mod) {
        Component[] ca = tabbedPane.getComponents();

        for (int i = 0; i < ca.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getRightComponent();
            VgraphAssemblyComponentWrapper vgacw = (VgraphAssemblyComponentWrapper) jsp.getViewport().getComponent(0);
            if (vgacw.assemblyModel == mod) {
                tabbedPane.remove(i);
                vgacw.isActive = false;

                // Don't allow operation of tools with no Assembly tab in view (NPEs)
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

    public boolean hasOpenModels() {
		return (tabbedPane.getComponentCount() > 0);
    }

    @Override
    public AssemblyModel[] getOpenModels() {
        Component[] ca = tabbedPane.getComponents();
        AssemblyModel[] vm = new AssemblyModel[ca.length];
        for (int i = 0; i < vm.length; i++) {
            JSplitPane jsplt = (JSplitPane) ca[i];
            JScrollPane jsp = (JScrollPane) jsplt.getRightComponent();
            VgraphAssemblyComponentWrapper vgacw = (VgraphAssemblyComponentWrapper) jsp.getViewport().getComponent(0);
            vm[i] = vgacw.assemblyModel;
        }
        return vm;
    }

    /** Rebuilds the Listener Event Graph Object (LEGO) tree view */
    public void rebuildLEGOTreePanels() {
        legoTree.clear();
        JSplitPane treeSplit = buildTreePanels();
        getCurrentVgraphAssemblyComponentWrapper().drawingSplitPane.setTopComponent(treeSplit);
        treeSplit.setDividerLocation(250);
        legoTree.repaint();
    }
    private JSplitPane treePanels;

    private JSplitPane buildTreePanels() {

        legoTree = new LegoTree("simkit.BasicSimEntity", "viskit/images/assembly.png",
                this, "Drag an Event Graph onto the canvas to add it to the assembly");

        propertyChangeListenerTree = new LegoTree("java.beans.PropertyChangeListener", new PropertyChangeListenerImageIcon(20, 20),
                this, "Drag a PropertyChangeListener onto the canvas to add it to the assembly");

        String[] extraCP = UserPreferencesDialog.getExtraClassPath();

        if (extraCP != null) {
            File file;
            for (String path : extraCP) { // tbd same for pcls
                file = new File(path);
                if (!file.exists()) {

                    // Allow a relative path for Diskit-Test (Diskit)
                    if (path.contains("..")) {
                        file = new File(ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot().getParent() + "/" + path.replaceFirst("../", ""));
                    }
                }

                if (file.exists()) {
                    addEventGraphsToLegoTree(file, file.isDirectory());
                }
            }
        }

        // Now add our EventGraphs path for LEGO tree inclusion of our SimEntities
        ViskitGlobals viskitGlobalsInstance = ViskitGlobals.instance();
        ViskitProject viskitProject = viskitGlobalsInstance.getCurrentViskitProject();

        // A fresh (reset) LocalBootLoader will be instantiated
        // here when compiling EGs for the first time, or when the
        // SimkitXML2Java translator attempts to resolve a ParameterMap
        addEventGraphsToLegoTree(viskitProject.getEventGraphsDirectory(), true);

        // Now load the simkit.jar and diskit.jar from where ever they happen to
        // be located on the classpath if present
        String[] classPath = ((LocalBootLoader) viskitGlobalsInstance.getWorkClassLoader()).getClassPath();
        for (String path : classPath) {
            if (path.contains("simkit.jar") || (path.contains("diskit.jar"))) {
                addEventGraphsToLegoTree(new File(path), false);
                addPropertyChangeListenersToLegoTree(new File(path), false);
            }
        }

        LegosPanel lPan = new LegosPanel(legoTree);
        PropChangeListenersPanel pclPan = new PropChangeListenersPanel(propertyChangeListenerTree);

        legoTree.setBackground(background);
        propertyChangeListenerTree.setBackground(background);

        treePanels = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lPan, pclPan);
        treePanels.setBorder(null);
        treePanels.setOneTouchExpandable(true);

        pclPan.setMinimumSize(new Dimension(20, 80));
        lPan.setMinimumSize(new Dimension(20, 80));
        lPan.setPreferredSize(new Dimension(20, 240)); // give it some height for the initial split

        legoTree.setDragEnabled(true);
        propertyChangeListenerTree.setDragEnabled(true);

        return treePanels;
    }

    @Override
    public void genericReport(int type, String title, String msg)
    {
        JOptionPane.showMessageDialog(this, msg, title, type);
    }
    Transferable dragged;

    @Override
    public void startingDrag(Transferable trans) {
        dragged = trans;
    }

    /** Class to facilitate dragging new nodes onto the pallete */
    class vDropTargetAdapter extends DropTargetAdapter {

        @Override
        public void dragOver(DropTargetDragEvent e) {

            // NOTE: this action is very critical in getting JGraph 5.14 to
            // signal the drop method
            e.acceptDrag(e.getDropAction());
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            if (dragged != null) {
                try {
                    Point p = dtde.getLocation();

                    String s = dragged.getTransferData(DataFlavor.stringFlavor).toString();
                    String[] sa = s.split("\t");

                    // Check for XML-based node
                    FileBasedAssemblyNode xn = isFileBasedAssemblyNode(sa[1]);
                    if (xn != null) {
                        switch (sa[0]) {
                            case "simkit.BasicSimEntity":
                                ((AssemblyController) getController()).newFileBasedEventGraphNode(xn, p);
                                break;
                            case "java.beans.PropertyChangeListener":
                                ((AssemblyController) getController()).newFileBasedPropChangeListenerNode(xn, p);
                                break;
                        }
                    } else {
                        // Else class-based node
                        switch (sa[0]) {
                            case "simkit.BasicSimEntity":
                                ((AssemblyController) getController()).newEventGraphNode(sa[1], p);
                                break;
                            case "java.beans.PropertyChangeListener":
                                ((AssemblyController) getController()).newPropChangeListenerNode(sa[1], p);
                                break;
                        }
                    }

                    dragged = null;
                } catch (UnsupportedFlavorException | IOException e) {
                    LogUtils.getLogger(AssemblyEditViewFrame.class).error(e);
                }
            }
        }
    }

    private FileBasedAssemblyNode isFileBasedAssemblyNode(String s) {
        try {
            return FileBasedAssemblyNode.fromString(s);
        } catch (FileBasedAssemblyNode.exception e) {
            return null;
        }
    }

    @Override
    public void modelChanged(mvcModelEvent event) {
        switch (event.getID()) {
            default:
                getCurrentVgraphAssemblyComponentWrapper().viskitModelChanged((ModelEvent) event);
                break;
        }
    }

    @Override
    public boolean doEditEventGraphNode(EventGraphNode eventNode) {
        return EventGraphNodeInspectorDialog.showDialog(this, eventNode);
    }

    @Override
    public boolean doEditPropertyChangeListenerNode(PropertyChangeListenerNode propertyChangeListenerNode) {
        return PropertyChangeListenerNodeInspectorDialog.showDialog(this, propertyChangeListenerNode); // blocks
    }

    @Override
    public boolean doEditPropertyChangeListenerEdge(PropertyChangeListenerEdge propertyChangeListenerEdge) {
        return PclEdgeInspectorDialog.showDialog(this, propertyChangeListenerEdge);
    }

    @Override
    public boolean doEditAdapterEdge(AdapterEdge aEdge) {
        return AdapterConnectionInspectorDialog.showDialog(this, aEdge);
    }

    @Override
    public boolean doEditSimEventListEdge(SimEvListenerEdge seEdge) {
        return SimEventListenerConnectionInspectorDialog.showDialog(this, seEdge);
    }

    private Object getLeafUO(JTree tree) {
        TreePath[] oa = tree.getSelectionPaths();
        if (oa != null) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) oa[0].getLastPathComponent();
            return dmtn.getUserObject();
        }
        return null;
    }

    @Override
    public Object getSelectedEventGraph() {
        return getLeafUO(legoTree);
    }

    @Override
    public Object getSelectedPropertyChangeListener() {
        return getLeafUO(propertyChangeListenerTree);
    }

    @Override
    public void addEventGraphsToLegoTree(File f, boolean b) {
        if (f.exists()) {
            legoTree.addContentRoot(f, b);
        }
    }

    @Override
    public void addPropertyChangeListenersToLegoTree(File f, boolean b) {
        propertyChangeListenerTree.addContentRoot(f, b);
    }

    @Override
    public void removeEventGraphFromLEGOTree(File f) {
        legoTree.removeContentRoot(f);
    }

    // Not used
    @Override
    public void removePropertyChangeListenerFromLEGOTree(File f) {
        propertyChangeListenerTree.removeContentRoot(f);
    }

    @Override
    public int genericAsk(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    @Override
    public int genericAskYN(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
    }

    @Override
    public int genericAsk2Buttons(String title, String message, String button1, String button2) {
        return JOptionPane.showOptionDialog(this, message, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{button1, button2}, button1);
    }

    @Override
    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }    // ViskitView-required methods:

    private JFileChooser assemblyFileChooser;
    private JFileChooser buildOpenSaveChooser() {

        // Try to open in the current project directory for Assemblies
        if (ViskitGlobals.instance().getCurrentViskitProject() != null) {
            return new JFileChooser(ViskitGlobals.instance().getCurrentViskitProject().getAssembliesDirectory());
        } else {
            return new JFileChooser(new File(ViskitProject.MY_VISKIT_PROJECTS_DIR));
        }
    }

    @Override
    public File[] openFilesAsk() {
        assemblyFileChooser = buildOpenSaveChooser();
        assemblyFileChooser.setDialogTitle("Open Assembly File");

        // Look for assembly in the filename, Bug 1247 fix
        FileFilter filter = new AssemblyFileFilter("assembly");
        assemblyFileChooser.setFileFilter(filter);

        assemblyFileChooser.setMultiSelectionEnabled(true);

        int returnVal = assemblyFileChooser.showOpenDialog(this);
        return (returnVal == JFileChooser.APPROVE_OPTION) ? assemblyFileChooser.getSelectedFiles() : null;
    }

    @Override
    public File openRecentFilesAsk(Collection<String> lis) {
        String fn = RecentFilesDialog.showDialog(this, lis);
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
    public void setSelectedAssemblyName(String selectedAssemblyName) {
        boolean nullString = !(selectedAssemblyName != null && !selectedAssemblyName.isEmpty());
        if (!nullString) {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), selectedAssemblyName);
        }
    }

    @Override
    public void openProject() {
        AssemblyControllerImpl aController = ((AssemblyControllerImpl) getController());

        if (!aController.handleProjectClosing()) {
            return;
        }

        File file = ViskitProject.openProjectDirectory(this, ViskitProject.MY_VISKIT_PROJECTS_DIR);
        if (file != null) {
            aController.openProject(file);
        }

        showProjectName();
    }

    @Override
    public void showProjectName() {
        super.showProjectName();
        ViskitGlobals.instance().getEventGraphEditor().showProjectName();
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

    @Override
    public File saveFileAsk(String suggestedName, boolean showUniqueName) {
		 return saveFileAsk(suggestedName,  showUniqueName, "Save Assembly File");
    }

    @Override
    public File saveFileAsk(String suggestedName, boolean showUniqueName, String dialogTitle) {
        if (assemblyFileChooser == null) {
            assemblyFileChooser = buildOpenSaveChooser();
        }

        assemblyFileChooser.setDialogTitle(dialogTitle);
        File file = new File(ViskitGlobals.instance().getCurrentViskitProject().getAssembliesDirectory(), suggestedName);
        if (!file.getParentFile().isDirectory()) {
             file.getParentFile().mkdirs();
        }
        if (showUniqueName) {
             file = getUniqueName(suggestedName);
        }

        assemblyFileChooser.setSelectedFile(file);
        int retv = assemblyFileChooser.showSaveDialog(this);
        if (retv == JFileChooser.APPROVE_OPTION) {
            if (assemblyFileChooser.getSelectedFile().exists()) {
                if (JOptionPane.YES_OPTION != genericAskYN("File Exists",  "Overwrite? Confirm")) {
                    return null;
                }
            }
            return assemblyFileChooser.getSelectedFile();
        }

        // We canceled
        deleteCanceledSave(file.getParentFile());
        assemblyFileChooser = null;
        return null;
    }

    /** Handles a canceled new EG file creation
     *
     * @param file to candidate EG file
     */
    public void deleteCanceledSave(File file) {
        if (file.exists()) {
            if (file.delete()) {
                if (file.getParentFile().exists() && !file.getParentFile().equals(ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDirectory())) {
                    deleteCanceledSave(file.getParentFile());
                }
            }
        }
    }

    @Override
    public void showAndSaveSource(String className, String s, String fileName) {
        final JFrame f = new SourceWindow(this, className, s);
        f.setTitle("Generated source from " + fileName);

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
            xt = XmlTree.getTreeInPanel(f);
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
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
        JButton closeButton = new JButton("Close");
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(closeButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

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

        closeButton.addActionListener(new ActionListener() {

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

} // end class file AssemblyEditViewFrame.java

/** Utility class to handle EventNode DnD operations */
interface DragStartListener {
    void startingDrag(Transferable trans);
}
