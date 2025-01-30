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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.logging.log4j.Logger;

import viskit.control.AssemblyController;
import viskit.control.AssemblyControllerImpl;
import viskit.util.FileBasedAssemblyNode;
import viskit.model.ModelEvent;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitProject;
import viskit.assembly.BasicAssembly;
import viskit.control.RecentProjectFileSetListener;
import viskit.doe.LocalBootLoader;
import viskit.images.AdapterIcon;
import viskit.images.PropChangListenerImageIcon;
import viskit.images.PropChangeListenerIcon;
import viskit.images.SimEventListenerIcon;
import viskit.jgraph.ViskitGraphAssemblyComponentWrapper;
import viskit.jgraph.ViskitGraphAssemblyModel;
import viskit.model.*;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.mvc.MvcModelEvent;
import viskit.util.AssemblyFileFilter;
import viskit.view.dialog.EventGraphNodeInspectorDialog;
import viskit.view.dialog.RecentFilesDialog;
import viskit.view.dialog.SimEventListenerConnectionInspectorDialog;
import viskit.view.dialog.SettingsDialog;
import viskit.view.dialog.PclNodeInspectorDialog;
import viskit.view.dialog.AdapterConnectionInspectorDialog;
import viskit.view.dialog.PclEdgeInspectorDialog;
import viskit.mvc.MvcController;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 10, 2004, 2:07:37 PM
 *
 * <pre>
 * The following is a list of startup events that allow for population of the
 * LEGO node tree (SimEntity and Properety Change Listener (PCL) drag and drop
 * pallettes)
 *
 * - Parse any extra classpaths recorded in the app history and all jars in the
 *   project's lib directory
 * - Parse the project's Event Graph directory
 *
 *   All SimEntities and PCLs found in either of the parsing events above, the
 *   following takes place.
 *
 * - Translate any XML found to java source -> build/src/${pkg}/*.java
 * - Validate XML
 * - Compile java source to -> build/classes/${pkg}/*.class
 * - Unmarshall XML
 * - Load all SimEntity classes via working ClassLoader to check readiness for
 *   running at sim time
 * - Cache both .class and .xml w/ MD5 hash to denote ready for sim running. If
 *   any files found not ready (won't validate, compile, etc.), place on the
 *   cache missed list. If previously on the cached missed list, but now ready
 *   for validation &and; compilation, remove from cached missed list and place
 *   on cached ready list
 * - If any files were previously cached, then make sure all .class files still
 *   load correctly and XML still validates &and; compiles
 * - All cached ready files are placed on file watch for any changes to ready
 *  status
 *
 * - Load the above representations into thier respective LEGO trees denoted
 *   with file type and a blue icon (SimEntity) or pink icon (PCL) showing ready
 *   to be used to configure an assembly file for sim running
 * </pre>
 */
public class AssemblyViewFrame extends MvcAbstractViewFrame implements AssemblyView, DragStartListener
{
    static final Logger LOG = LogUtils.getLogger(AssemblyViewFrame.class);
    
    private String title = new String();
    
    /** Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc. */
    public static final int SELECT_MODE = 0;
    public static final int ADAPTER_MODE = 1;
    public static final int SIMEVLIS_MODE = 2;
    public static final int PCL_MODE = 3;

    // The controller needs access to this
    public JButton initializeAssemblySimulationRunButton;

    JMenu openRecentAssemblyMenu, openRecentProjectMenu;

    private final static String FRAME_DEFAULT_TITLE = " Viskit Assembly Editor";

    private final String FULLPATH = ViskitStatics.FULL_PATH;
    private final String CLEARPATHFLAG = ViskitStatics.CLEAR_PATH_FLAG;
    private final Color background = new Color(0xFB, 0xFB, 0xE5);

    /** Toolbar for dropping icons, connecting, etc. */
    private JTabbedPane tabbedPane;
    private JToolBar toolBar;
    private JToggleButton selectMode;
    private JToggleButton adapterMode,  simEventListenerMode,  propertyChangeListenerMode;
    private LegoTree lTree, pclTree;
    private JMenuBar myMenuBar;
    private JMenuItem quitMenuItem;
    private RecentProjectFileSetListener recentProjectFileSetListener;
    private RecentAssemblyFileListener recentAssemblyFileListener;

    private int untitledCount = 0;

    public AssemblyViewFrame(MvcController controller) {
        super(FRAME_DEFAULT_TITLE);
        initMVC(controller);   // set up mvc linkages
        initUI();   // build widgets
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
    private void initMVC(MvcController ctrl) {
        setController(ctrl);
    }

    /**
     * Initialize the user interface
     */
    private void initUI() {
        buildMenus();
        buildToolbar();

        // Build here to prevent NPE from EventGraphController
        buildTreePanels();

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

    public ViskitGraphAssemblyComponentWrapper getCurrentVgacw() {
        JSplitPane jsplt = (JSplitPane) tabbedPane.getSelectedComponent();
        if (jsplt == null) {return null;}

        JScrollPane jSP = (JScrollPane) jsplt.getRightComponent();
        return (ViskitGraphAssemblyComponentWrapper) jSP.getViewport().getComponent(0);
    }

    public Component getCurrentJgraphComponent() {
        ViskitGraphAssemblyComponentWrapper vcw = getCurrentVgacw();
        if (vcw == null || vcw.drawingSplitPane == null) {return null;}
        return vcw.drawingSplitPane.getRightComponent();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JToolBar toolBar) {
        this.toolBar = toolBar;
    }

    /**
     * @return the recentProjectFileSetListener
     */
    public RecentProjectFileSetListener getRecentProjectFileSetListener() {
        return recentProjectFileSetListener;
    }

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            ViskitGraphAssemblyComponentWrapper myVgacw = getCurrentVgacw();

            if (myVgacw == null) {     // last tab has been closed
                setSelectedAssemblyName(null);
                return;
            }

            // Key to getting the LEGOs tree panel in each tab view
            myVgacw.drawingSplitPane.setLeftComponent(myVgacw.trees);

            setModel((MvcModel) myVgacw.assemblyModel); // hold on locally
            getController().setModel(getModel()); // tell controller
            AssemblyModelImpl mod = (AssemblyModelImpl) getModel();

            if (mod.getLastFile() != null)
                ((AssemblyControllerImpl) getController()).initOpenAssemblyWatch(mod.getLastFile(), mod.getJaxbRoot());

            GraphMetadata gmd = mod.getMetadata();
            if (gmd != null)
                setSelectedAssemblyName(gmd.name);
            else if (viskit.ViskitStatics.debug)
                System.err.println("error: AssemblyViewFrame gmd null..");
        }
    }

    class RecentAssemblyFileListener implements MvcRecentFileListener {

        @Override
        public void listChanged()
        {
            String nameOnly;
            Action currentAction;
            JMenuItem mi;
            AssemblyController assemblyController = (AssemblyController) getController();
            Set<String> files = assemblyController.getRecentAssemblyFileSet();
            openRecentAssemblyMenu.removeAll(); // clear prior to rebuilding menu
            openRecentAssemblyMenu.setEnabled(false); // disable unless file is found
            File file;
            for (String fullPath : files) 
            {
                file = new File(fullPath);
                if (!file.exists())
                {
                    // file not found as expected, something happened externally and so report it
                    System.err.println("*** [AssemblyViewFrame listChanged] Event graph file not found: " + file.getPath());
                    continue; // actual file not found, skip to next file in files loop
                }
                nameOnly = file.getName();
                currentAction = new ParameterizedAssemblyAction(nameOnly);
                currentAction.putValue(ViskitStatics.FULL_PATH, fullPath);
                mi = new JMenuItem(currentAction);
                mi.setToolTipText(file.getPath());
                openRecentAssemblyMenu.add(mi);
                openRecentAssemblyMenu.setEnabled(true); // at least one is found
            }
            if (!files.isEmpty()) 
            {
                openRecentAssemblyMenu.add(new JSeparator());
                currentAction = new ParameterizedAssemblyAction("clear history");
                currentAction.putValue(ViskitStatics.FULL_PATH, ViskitStatics.CLEAR_PATH_FLAG);  // flag
                mi = new JMenuItem(currentAction);
                mi.setToolTipText("Clear this list");
                openRecentAssemblyMenu.add(mi);
            }
            // TODO note that some items might remain loaded after "clear menu" and so wondering if that is ambiguous
            
            // prior implementation is different pattern and so not continued
//            AssemblyController assemblyCcontroller = (AssemblyController) getController();
//            Set<String> files = assemblyCcontroller.getRecentAssemblyFileSet();
//            openRecentAssemblyMenu.removeAll();
//            files.stream().filter(fullPath -> new File(fullPath).exists()).map(fullPath -> {
//                String nameOnly = new File(fullPath).getName();
//                Action act = new ParameterizedAssemblyAction(nameOnly);
//                act.putValue(FULLPATH, fullPath);
//                JMenuItem mi = new JMenuItem(act);
//                mi.setToolTipText(fullPath);
//                return mi;
//            }).forEachOrdered(mi -> {
//                openRecentAssemblyMenu.add(mi);
//            });
//            if (!files.isEmpty()) {
//                openRecentAssemblyMenu.add(new JSeparator());
//                Action act = new ParameterizedAssemblyAction("clear history");
//                act.putValue(FULLPATH, CLEARPATHFLAG);  // flag
//                JMenuItem mi = new JMenuItem(act);
//                mi.setToolTipText("Clear this list");
//                openRecentAssemblyMenu.add(mi);
//            }
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

            if (fullPath != null)
            {
                if (fullPath.getPath().equals(CLEARPATHFLAG))
                    assemblyController.clearRecentAssemblyFileList();
                else
                    assemblyController.openRecentAssembly(fullPath);
            }
        }
    }

    private void buildMenus()
    {
        AssemblyController assemblyController = (AssemblyController) getController();
        recentAssemblyFileListener = new RecentAssemblyFileListener();
        assemblyController.addRecentAssemblyFileSetListener(getRecentAssemblyFileListener());

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Set up file menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        fileMenu.add(buildMenuItem(assemblyController, "newProject", "New Viskit Project", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK)));
        
        fileMenu.add(buildMenuItem(this, "openProject", "Open Project", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK)));
        fileMenu.add(openRecentProjectMenu = buildMenu("Open Recent Project"));

        // The AssemblyViewFrame will get this listener for its menu item of the same name
        recentProjectFileSetListener = new RecentProjectFileSetListener();
        getRecentProjectFileSetListener().addMenuItem(openRecentProjectMenu);
        assemblyController.addRecentProjectFileSetListener(getRecentProjectFileSetListener());
        
        fileMenu.add(buildMenuItem(assemblyController, "zipAndMailProject", "Zip/Email Viskit Project", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK)));
        fileMenu.addSeparator();
        
        fileMenu.add(buildMenuItem(assemblyController, "newAssembly", "New Assembly", KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, accelMod)));

        fileMenu.add(buildMenuItem(assemblyController, "open", "Open Assembly", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, accelMod)));
        fileMenu.add(openRecentAssemblyMenu = buildMenu("Open Recent Assembly"));
        openRecentAssemblyMenu.setEnabled(false); // inactive until needed, reset by listener

        // Bug fix: 1195
        fileMenu.add(buildMenuItem(assemblyController, "close", "Close Assembly", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, accelMod)));
        fileMenu.add(buildMenuItem(assemblyController, "closeAll", "Close All Assemblies", null, null));
        fileMenu.add(buildMenuItem(assemblyController, "save", "Save Assembly", KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, accelMod)));
        fileMenu.add(buildMenuItem(assemblyController, "saveAs", "Save Assembly as...", KeyEvent.VK_A, null));
        fileMenu.addSeparator();

        fileMenu.add(buildMenuItem(assemblyController, "generateJavaSource", "Generate Java Source of saved Assembly", KeyEvent.VK_J, // TODO confirm "saved"
                KeyStroke.getKeyStroke(KeyEvent.VK_J, accelMod)));
        fileMenu.add(buildMenuItem(assemblyController, "captureWindow", "Save Assembly Screen Image", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, accelMod)));
        fileMenu.add(buildMenuItem(assemblyController, "prepareSimulationRunner", "Initialize Assembly Simulation Run", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK)));
        fileMenu.add(buildMenuItem(assemblyController, "viewXML", "XML View of Saved Assembly", KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));

        // TODO: Unknown as to what this does exactly
        fileMenu.add(buildMenuItem(assemblyController, "export2grid", "Export to Cluster Format", KeyEvent.VK_C, null));
        ActionIntrospector.getAction(assemblyController, "export2grid").setEnabled(false);
        fileMenu.addSeparator();

        fileMenu.add(buildMenuItem(assemblyController, "settings", "Viskit Settings", null, null));
        fileMenu.addSeparator();

        fileMenu.add(quitMenuItem = buildMenuItem(assemblyController, "quit", "Exit", KeyEvent.VK_Q,
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, accelMod)));

        // Set up edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(buildMenuItem(assemblyController, "undo", "Undo", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, accelMod)));
        editMenu.add(buildMenuItem(assemblyController, "redo", "Redo", KeyEvent.VK_Y,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, accelMod)));

        ActionIntrospector.getAction(assemblyController, "undo").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "redo").setEnabled(false);
        editMenu.addSeparator();

        // the next four are disabled until something is selected
        editMenu.add(buildMenuItem(assemblyController, "cut", "Cut", KeyEvent.VK_X,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, accelMod)));
        editMenu.getItem(editMenu.getItemCount()-1).setToolTipText("Cut is not supported in Viskit.");
        editMenu.add(buildMenuItem(assemblyController, "copy", "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, accelMod)));
        editMenu.add(buildMenuItem(assemblyController, "paste", "Paste Events", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, accelMod)));
        editMenu.add(buildMenuItem(assemblyController, "remove", "Delete", KeyEvent.VK_DELETE,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, accelMod)));

        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(assemblyController, "cut").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "remove").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "copy").setEnabled(false);
        ActionIntrospector.getAction(assemblyController, "paste").setEnabled(false);
        editMenu.addSeparator();

        editMenu.add(buildMenuItem(assemblyController, "newEventGraphNode", "Add Event Graph...", KeyEvent.VK_G, null));
        editMenu.add(buildMenuItem(assemblyController, "newPropertyChangeListenerNode", "Add Property Change Listener...", KeyEvent.VK_L, null));
        editMenu.addSeparator();

        editMenu.add(buildMenuItem(assemblyController, "editGraphMetaData", "Edit Properties...", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, accelMod)));

        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(fileMenu);
        myMenuBar.add(editMenu);

        // Help editor created by the EGVF for all of Viskit's UIs
    }

    private JMenu buildMenu(String name) 
    {
        return new JMenu(name);
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mnemonicKeyEvent, KeyStroke accelleratorKeyStroke) {
        Action a = ActionIntrospector.getAction(source, method);

        Map<String, Object> map = new HashMap<>();
        if (mnemonicKeyEvent != null)
            map.put(Action.MNEMONIC_KEY, mnemonicKeyEvent);
        if (accelleratorKeyStroke != null)
            map.put(Action.ACCELERATOR_KEY, accelleratorKeyStroke);
        if (name != null)
            map.put(Action.NAME, name);
        if (!map.isEmpty())
            ActionUtilities.decorateAction(a, map);

        return ActionUtilities.createMenuItem(a);
    }

    /**
     * @return the current mode--select, add, arc, cancelArc
     */
    public int getCurrentMode() {
        // Use the button's selected status to figure out what mode
        // we are in.

        if (selectMode.isSelected())
            return SELECT_MODE;
        if (adapterMode.isSelected())
            return ADAPTER_MODE;
        if (simEventListenerMode.isSelected())
            return SIMEVLIS_MODE;
        if (propertyChangeListenerMode.isSelected())
            return PCL_MODE;
        LogUtils.getLogger(AssemblyViewFrame.class).error("assert false : \"getCurrentMode()\"");
        return 0;
    }

    private void buildToolbar() {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        // Buttons for what mode we are in

        selectMode = makeJToggleButton(
                null,
                "viskit/images/selectNode.png",
                "Select items on the graph"
        );
        Border defBor = selectMode.getBorder();
        selectMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(Color.lightGray, 2)));

        adapterMode = makeJToggleButton(
                null,
                new AdapterIcon(24, 24),
                "Connect SimEntities with adapter pattern"
        );
        defBor = adapterMode.getBorder();
        adapterMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        simEventListenerMode = makeJToggleButton(
                null,
                new SimEventListenerIcon(24, 24),
                "Connect SimEntities through a SimEvent listener pattern"
        );
        defBor = simEventListenerMode.getBorder();
        simEventListenerMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        propertyChangeListenerMode = makeJToggleButton(
                null,
                new PropChangeListenerIcon(24, 24),
                "Connect a property change listener to a SimEntity"
        );
        defBor = propertyChangeListenerMode.getBorder();
        propertyChangeListenerMode.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 2)));

        JButton zoomIn = makeButton(
                null,
                "viskit/images/ZoomIn24.gif",
                "Zoom in on the graph"
        );

        JButton zoomOut = makeButton(
                null,
                "viskit/images/ZoomOut24.gif",
                "Zoom out on the graph"
        );

        Action prepareSimulationRunnerAction = ActionIntrospector.getAction(getController(), "prepareSimulationRunner");
        initializeAssemblySimulationRunButton = makeButton(prepareSimulationRunnerAction, "viskit/images/Play24.gif",
                "Compile, initialize the assembly and prepare the Simulation Runner");
        modeButtonGroup.add(selectMode);
        modeButtonGroup.add(adapterMode);
        modeButtonGroup.add(simEventListenerMode);
        modeButtonGroup.add(propertyChangeListenerMode);

        // Make selection mode the default mode
        selectMode.setSelected(true);

        getToolBar().add(new JLabel("Mode: "));

        getToolBar().add(selectMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(adapterMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(simEventListenerMode);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(propertyChangeListenerMode);

        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(new JLabel("Zoom: "));
        getToolBar().add(zoomIn);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOut);
        getToolBar().addSeparator(new Dimension(24, 24));

        JLabel initializeAssemblyRunLabel = new JLabel ("  Initialize Assembly Simulation Run: ");
        initializeAssemblyRunLabel.setToolTipText("First initialize Assembly Simulation Run from Assembly tab");
        getToolBar().add(initializeAssemblyRunLabel);
        getToolBar().add(initializeAssemblySimulationRunButton);

        // Let the opening of Assembliess make this visible
        getToolBar().setVisible(false);

        zoomIn.addActionListener((ActionEvent e) -> {
            getCurrentVgacw().setScale(getCurrentVgacw().getScale() + 0.1d);
        });
        zoomOut.addActionListener((ActionEvent e) -> {
            getCurrentVgacw().setScale(Math.max(getCurrentVgacw().getScale() - 0.1d, 0.1d));
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
                getCurrentVgacw().setPortsVisible(tOrF);
            }
        }

        PortsVisibleListener portsOn  = new PortsVisibleListener(true);
        PortsVisibleListener portsOff = new PortsVisibleListener(false);
                    selectMode.addActionListener(portsOff);
                   adapterMode.addActionListener(portsOn);
          simEventListenerMode.addActionListener(portsOn);
        propertyChangeListenerMode.addActionListener(portsOn);
    }

    private JToggleButton makeJToggleButton(Action newAction, String iconPath, String tooltipText) {
        JToggleButton jToggleButton;

        if (newAction != null)
            jToggleButton = new JToggleButton(newAction);
        else
            jToggleButton = new JToggleButton();

        return (JToggleButton) buttonCommon(jToggleButton, iconPath, tooltipText);
    }

    private JToggleButton makeJToggleButton(Action newAction, Icon iconInstance, String tooltipText) {
        JToggleButton jToggleButton;
        if (newAction != null)
            jToggleButton = new JToggleButton(newAction);
        else
            jToggleButton = new JToggleButton();
        jToggleButton.setIcon(iconInstance);
        return (JToggleButton) buttonCommon2(jToggleButton, tooltipText);
    }

    private JButton makeButton(Action a, String icPath, String tt) {
        JButton b;
        if (a != null)
            b = new JButton(a);
        else
            b = new JButton();
        return (JButton) buttonCommon(b, icPath, tt);
    }

    private AbstractButton buttonCommon(AbstractButton b, String icPath, String tt) {
        b.setIcon(new ImageIcon(getClass().getClassLoader().getResource(icPath)));
        return buttonCommon2(b, tt);
    }

    private AbstractButton buttonCommon2(AbstractButton b, String tt) {
        b.setToolTipText(tt);
        b.setBorder(BorderFactory.createEtchedBorder());
        b.setText(null);
        return b;
    }

    @Override
    public void addTab(AssemblyModel assemblyModel)
    {
        ViskitGraphAssemblyModel vGAmod = new ViskitGraphAssemblyModel();
        ViskitGraphAssemblyComponentWrapper graphPane = new ViskitGraphAssemblyComponentWrapper(vGAmod, this);
        vGAmod.setjGraph(graphPane); // TODO fix this

        graphPane.assemblyModel = assemblyModel;
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
            LogUtils.getLogger(AssemblyViewFrame.class).error(tmle);
        }

        // the view holds only one assemblyModel, so it gets overwritten with each tab
        // but this call serves also to register the view with the passed assemblyModel
        // by virtue of calling stateChanged()
        tabbedPane.add("untitled" + untitledCount++, graphPane.drawingSplitPane);
        tabbedPane.setSelectedComponent(graphPane.drawingSplitPane); // bring to front

        // Now expose the Assembly toolbar
        Runnable r = () -> {
            getToolBar().setVisible(true);
            // If an assembly is loaded, make assembly frame active 
            AssemblyControllerImpl assemblyController = (AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController();
            assemblyController.makeTopPaneAssemblyTabActive();
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void deleteTab(AssemblyModel mod) {
        Component[] ca = tabbedPane.getComponents();

        JSplitPane jsplt;
        JScrollPane jsp;
        ViskitGraphAssemblyComponentWrapper vgacw;
        for (int i = 0; i < ca.length; i++) {
            jsplt = (JSplitPane) ca[i];
            jsp = (JScrollPane) jsplt.getRightComponent();
            vgacw = (ViskitGraphAssemblyComponentWrapper) jsp.getViewport().getComponent(0);
            if (vgacw.assemblyModel == mod) {
                tabbedPane.remove(i);
                vgacw.isActive = false;

                // Don't allow operation of tools with no Assembly tab in view (NPEs)
                if (tabbedPane.getTabCount() == 0) {
                    Runnable r = () -> {
                        getToolBar().setVisible(false);
                    };
                    SwingUtilities.invokeLater(r);
                }
                return;
            }
        }
    }

    @Override
    public AssemblyModel[] getOpenModels() {
        Component[] ca = tabbedPane.getComponents();
        AssemblyModel[] vm = new AssemblyModel[ca.length];
        JSplitPane jsplt;
        JScrollPane jsp;
        ViskitGraphAssemblyComponentWrapper vgacw;
        for (int i = 0; i < vm.length; i++) {
            jsplt = (JSplitPane) ca[i];
            jsp = (JScrollPane) jsplt.getRightComponent();
            vgacw = (ViskitGraphAssemblyComponentWrapper) jsp.getViewport().getComponent(0);
            vm[i] = vgacw.assemblyModel;
        }
        return vm;
    }

    /** Rebuilds the Listener Event Graph Object (LEGO) tree view */
    public void rebuildLEGOTreePanels() {
        if (getCurrentVgacw() == null)
            return; // no LEGO panel yet

        lTree.clear();
        pclTree.clear();
        JSplitPane treeSplit = buildTreePanels();
        getCurrentVgacw().drawingSplitPane.setTopComponent(treeSplit);
        treeSplit.setDividerLocation(250);
        lTree.repaint();
        pclTree.repaint();
    }
    private JSplitPane treePanels;

    private JSplitPane buildTreePanels() {
        lTree = new LegoTree("simkit.BasicSimEntity", "viskit/images/assembly.png",
                this, "Drag an Event Graph onto the canvas to add it to the assembly");

        pclTree = new LegoTree("java.beans.PropertyChangeListener", new PropChangListenerImageIcon(20, 20),
                this, "Drag a PropertyChangeListener onto the canvas to add it to the assembly");

        // Parse any extra/additional classpath for any required dependencies
        File file;
        try {
            URL[] extraCP = SettingsDialog.getExtraClassPathArraytoURLArray();
            for (URL path : extraCP) { // tbd same for pcls
                if (path == null)
                    continue; // can happen if extraClassPaths.path[@value] is null or erroneous
                file = new File(path.toURI());
                if (file.exists())
                    addEventGraphsToLegoTree(file, file.isDirectory()); // recurse directories
            }
        } catch (URISyntaxException ex) {
            LogUtils.getLogger(getClass()).error(ex);
        }

        // Now add our EventGraphs path for LEGO tree inclusion of our SimEntities
        ViskitGlobals vGlobals = ViskitGlobals.instance();
        ViskitProject vkp = vGlobals.getCurrentViskitProject();

        // A fresh (reset) LocalBootLoader will be instantiated
        // here when compiling EGs for the first time, or when the
        // SimkitXML2Java translator attempts to resolve a ParameterMap
        addEventGraphsToLegoTree(vkp.getEventGraphsDir(), true);

        // Now load the simkit.jar and diskit.jar from where ever they happen to
        // be located on the classpath if present
        String[] classPath = ((LocalBootLoader) vGlobals.getWorkClassLoader()).getClassPath();
        for (String path : classPath) {
            if (path.contains("simkit.jar") || (path.contains("diskit.jar"))) {
                addEventGraphsToLegoTree(new File(path), false);
                addPropertyChangeListenersToLegoTree(new File(path), false);
            }
        }

        LegosPanel lPan = new LegosPanel(lTree);
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
    Transferable dragged;

    @Override
    public void startingDrag(Transferable trans) {
        dragged = trans;
    }

    /** Class to facilitate dragging new nodes onto the pallette */
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
                                ((AssemblyController) getController()).newFileBasedPropertyChangeListenerNode(xn, p);
                                break;
                        }
                    } else {
                        // Else class-based node
                        switch (sa[0]) {
                            case "simkit.BasicSimEntity":
                                ((AssemblyController) getController()).newEventGraphNode(sa[1], p);
                                break;
                            case "java.beans.PropertyChangeListener":
                                ((AssemblyController) getController()).newPropertyChangeListenerNode(sa[1], p);
                                break;
                        }
                    }

                    dragged = null;
                } catch (UnsupportedFlavorException | IOException e) {
                    LogUtils.getLogger(AssemblyViewFrame.class).error(e);
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
    public void modelChanged(MvcModelEvent event) {
        switch (event.getID()) {
            default:
                getCurrentVgacw().viskitModelChanged((ModelEvent) event);
                break;
        }
    }

    @Override
    public boolean doEditEventGraphNode(EventGraphNode evNode) {
        return EventGraphNodeInspectorDialog.showDialog(this, evNode);
    }

    @Override
    public boolean doEditPropertyChangeListenerNode(PropertyChangeListenerNode pclNode) {
        return PclNodeInspectorDialog.showDialog(this, pclNode); // blocks
    }

    @Override
    public boolean doEditPropertyChangeListenerEdge(PropertyChangeEdge pclEdge) {
        return PclEdgeInspectorDialog.showDialog(this, pclEdge);
    }

    @Override
    public boolean doEditAdapterEdge(AdapterEdge aEdge) {
        return AdapterConnectionInspectorDialog.showDialog(this, aEdge);
    }

    @Override
    public boolean doEditSimEventListenerEdge(SimEventListenerEdge seEdge) {
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
        return getLeafUO(lTree);
    }

    @Override
    public Object getSelectedPropertyChangeListener() {
        return getLeafUO(pclTree);
    }

    @Override
    public void addEventGraphsToLegoTree(File f, boolean b) {
        if (f.exists())
            lTree.addContentRoot(f, b);
    }

    @Override
    public void addPropertyChangeListenersToLegoTree(File f, boolean b) {
        pclTree.addContentRoot(f, b);
    }

    @Override
    public void removeEventGraphFromLEGOTree(File f) {
        lTree.removeContentRoot(f);
    }

    // Not used
    @Override
    public void removePropertyChangeListenerFromLEGOTree(File f) {
        pclTree.removeContentRoot(f);
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
    public int genericAsk2Buttons(String title, String msg, String butt1, String butt2) {
        return JOptionPane.showOptionDialog(this, msg, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{butt1, butt2}, butt1);
    }

    @Override
    public void genericReport(int type, String title, String message) {
        if (type == JOptionPane.ERROR_MESSAGE)
        {
            LOG.error(message);
            System.err.println("***" + message);
        }
        JOptionPane.showMessageDialog(ViskitGlobals.instance().getMainFrameWindow(), message, title, type);
    }

    @Override
    public String promptForStringOrCancel(String title, String message, String initval) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initval);
    }    // ViskitView-required methods:

    private JFileChooser jfc;
    private JFileChooser buildOpenSaveChooser() {

        // Try to open in the current project directory for Assemblies
        if (ViskitGlobals.instance().getCurrentViskitProject() != null)
            return new JFileChooser(ViskitGlobals.instance().getCurrentViskitProject().getAssembliesDir());
        else
            return new JFileChooser(new File(ViskitProject.VISKIT_PROJECTS_DIRECTORY));
    }

    @Override
    public File[] openFilesAsk() {
        jfc = buildOpenSaveChooser();
        jfc.setDialogTitle("Open Assembly Files");

        // Look for assembly in the filename, Bug 1247 fix
        FileFilter filter = new AssemblyFileFilter("assembly");
        jfc.setFileFilter(filter);
        jfc.setMultiSelectionEnabled(true);

        int returnVal = jfc.showOpenDialog(this);
        return (returnVal == JFileChooser.APPROVE_OPTION) ? jfc.getSelectedFiles() : null;
    }

    @Override
    public File openRecentFilesAsk(Collection<String> lis) {
        String fn = RecentFilesDialog.showDialog(this, lis);
        if (fn != null) {
            File f = new File(fn);
            if (f.exists())
                return f;
            else
                genericReport(JOptionPane.ERROR_MESSAGE, "File not found.", f + " does not exist");
        }
        return null;
    }

    @Override
    public void setSelectedAssemblyName(String s) {
        boolean nullString = !(s != null && !s.isEmpty());
        if (!nullString)
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), s);
    }

    @Override
    public void openProject() {
        AssemblyControllerImpl aController = ((AssemblyControllerImpl) getController());

        if (!aController.handleProjectClosing())
            return;

        File file = ViskitProject.openProjectDirectory(this.getContent(), ViskitProject.VISKIT_PROJECTS_DIRECTORY);
        if (file != null)
            aController.openProject(file); // calls EGVF setTitleApplicationProjectName

        setTitleApplicationProjectName();
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
    public File saveFileAsk(String suggName, boolean showUniqueName) {
        if (jfc == null)
            jfc = buildOpenSaveChooser();

        jfc.setDialogTitle("Save Assembly File");
        File fil = new File(ViskitGlobals.instance().getCurrentViskitProject().getAssembliesDir(), suggName);
        if (!fil.getParentFile().isDirectory())
            fil.getParentFile().mkdirs();
        if (showUniqueName)
            fil = getUniqueName(suggName);

        jfc.setSelectedFile(fil);
        int retv = jfc.showSaveDialog(this);
        if (retv == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile().exists()) {
                if (JOptionPane.YES_OPTION != genericAskYN("File Exists",  "Overwrite? Confirm"))
                    return null;
            }
            return jfc.getSelectedFile();
        }

        // We canceled
        deleteCanceledSave(fil.getParentFile());
        jfc = null;
        return null;
    }

    /** Handles a canceled new Event Graph file creation
     *
     * @param file to candidate Event Graph file
     */
    public void deleteCanceledSave(File file) {
        if (file.exists()) {
            if (file.delete()) {
                if (file.getParentFile().exists() && !file.getParentFile().equals(ViskitGlobals.instance().getCurrentViskitProject().getEventGraphsDir()))
                    deleteCanceledSave(file.getParentFile());
            }
        }
    }

    @Override
    public void showAndSaveSource(String className, String s, String fileName) {
        final JFrame f = new SourceWindow(this, className, s);
        f.setTitle("Generated Java source from: " + fileName);

        Runnable r = () -> {
            f.setVisible(true);
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void displayXML(File file)
    {
        JComponent xmlTreeComponent;
        try {
            xmlTreeComponent = XMLTreeComponent.getTreeInPanel(file);
        } 
        catch (Exception e) {
            genericReport(JOptionPane.ERROR_MESSAGE, "XML Display Error", e.getMessage());
            return;
        }
        //xt.setVisibleRowCount(25);
        xmlTreeComponent.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final JFrame xmlDisplayFrame = new JFrame(file.getName());

        JPanel contentPanel = new JPanel();
        xmlDisplayFrame.setContentPane(contentPanel);

        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel.add(xmlTreeComponent, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
        JButton closeButton = new JButton("Close");
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(closeButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        //jf.pack();
        xmlDisplayFrame.setSize(475, 500);
        xmlDisplayFrame.setLocationRelativeTo(this);

        Runnable r = () -> {
            xmlDisplayFrame.setVisible(true);
        };
        SwingUtilities.invokeLater(r);

        closeButton.addActionListener((ActionEvent e) -> {
            Runnable r1 = () -> {
                xmlDisplayFrame.dispose();
            };
            SwingUtilities.invokeLater(r1);
        });
    }

//    void clampHeight(JComponent comp) {
//        Dimension d = comp.getPreferredSize();
//        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
//        comp.setMinimumSize(new Dimension(Integer.MAX_VALUE, d.height));
//    }
//
//    void clampSize(JComponent comp) {
//        Dimension d = comp.getPreferredSize();
//        comp.setMaximumSize(d);
//        comp.setMinimumSize(d);
//    }

    /**
     * @return the recentAsyFileListener
     */
    public RecentAssemblyFileListener getRecentAssemblyFileListener() {
        return recentAssemblyFileListener;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param newTitle the title to set
     */
    @Override
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }
    public int getNumberAssembliesLoaded()
    {
        return tabbedPane.getTabCount();
    }

} // end class file AssemblyViewFrame.java

/** Utility class to handle EventNode DnD operations */
interface DragStartListener {
    void startingDrag(Transferable trans);
}
