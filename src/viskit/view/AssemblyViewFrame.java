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
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static viskit.Help.METHOD_aboutViskit;
import static viskit.Help.METHOD_doContents;
import static viskit.Help.METHOD_doSearch;
import static viskit.Help.METHOD_doTutorial;
import static viskit.Help.METHOD_launchGithubSimkit;
import static viskit.Help.METHOD_launchGithubViskit;
import static viskit.Help.METHOD_launchMV3302SimkitJavaProgrammingVideos;
import static viskit.Help.METHOD_launchSimkitDesModelingManual;

import viskit.control.AssemblyControllerImpl;
import viskit.util.FileBasedAssemblyNode;
import viskit.model.ModelEvent;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitProject;
import static viskit.ViskitStatics.isFileReady;
import static viskit.control.AssemblyControllerImpl.METHOD_captureWindow;
import static viskit.control.AssemblyControllerImpl.METHOD_close;
import static viskit.control.AssemblyControllerImpl.METHOD_closeAll;
import static viskit.control.AssemblyControllerImpl.METHOD_closeProject;
import static viskit.control.AssemblyControllerImpl.METHOD_copy;
import static viskit.control.AssemblyControllerImpl.METHOD_cut;
import static viskit.control.AssemblyControllerImpl.METHOD_editGraphMetadata;
import static viskit.control.AssemblyControllerImpl.METHOD_newAssembly;
import static viskit.control.AssemblyControllerImpl.METHOD_newEventGraphNode;
import static viskit.control.AssemblyControllerImpl.METHOD_newProject;
import static viskit.control.AssemblyControllerImpl.METHOD_newPropertyChangeListenerNode;
import static viskit.control.AssemblyControllerImpl.METHOD_open;
import static viskit.control.AssemblyControllerImpl.METHOD_openProject;
import static viskit.control.AssemblyControllerImpl.METHOD_paste;
import static viskit.control.AssemblyControllerImpl.METHOD_prepareSimulationRunner;
import static viskit.control.AssemblyControllerImpl.METHOD_quit;
import static viskit.control.AssemblyControllerImpl.METHOD_redo;
import static viskit.control.AssemblyControllerImpl.METHOD_remove;
import static viskit.control.AssemblyControllerImpl.METHOD_save;
import static viskit.control.AssemblyControllerImpl.METHOD_saveAs;
import static viskit.control.AssemblyControllerImpl.METHOD_showViskitUserPreferences;
import static viskit.control.AssemblyControllerImpl.METHOD_undo;
import static viskit.control.AssemblyControllerImpl.METHOD_viewXML;
import static viskit.control.AssemblyControllerImpl.METHOD_zipProject;
import static viskit.control.EventGraphControllerImpl.METHOD_zipAndMailProject;
import viskit.control.RecentProjectFileSetListener;
import viskit.doe.LocalBootLoader;
import viskit.images.AdapterIcon;
import viskit.images.PropertyChangeListenerImageIcon;
import viskit.images.PropertyChangeListenerIcon;
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
import viskit.view.dialog.ViskitUserPreferencesDialog;
import viskit.view.dialog.PclNodeInspectorDialog;
import viskit.view.dialog.AdapterConnectionInspectorDialog;
import viskit.view.dialog.PclEdgeInspectorDialog;
import viskit.mvc.MvcController;
import viskit.mvc.MvcModel;
import viskit.mvc.MvcRecentFileListener;
import static viskit.view.SimulationRunPanel.INITIAL_SIMULATION_RUN_HINT;
import static viskit.control.AssemblyControllerImpl.METHOD_generateJavaCode;
import static viskit.control.AssemblyControllerImpl.METHOD_saveAll;

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
    static final Logger LOG = LogManager.getLogger();
    
    private String title = new String();
    
    /** Modes we can be in--selecting items, adding nodes to canvas, drawing arcs, etc. */
    public static final int                   SELECT_MODE = 0;
    public static final int                  ADAPTER_MODE = 1;
    public static final int        SIMEVENT_LISTENER_MODE = 2;
    public static final int PROPERTY_CHANGE_LISTENER_MODE = 3;

    // The controller needs access to this
    public JButton prepareAssemblyForSimulationRunButton;

    JMenu openRecentAssemblyMenu, openRecentProjectMenu;

    private final static String FRAME_DEFAULT_TITLE = "Assembly Editor";
    private static final String LOOK_AND_FEEL = ViskitUserPreferencesDialog.getLookAndFeel();

    private final String CLEARPATHFLAG = ViskitStatics.CLEAR_PATH_FLAG;
    private final Color background = new Color(0xFB, 0xFB, 0xE5);

    /** Toolbar for dropping icons, connecting, etc. */
    private JTabbedPane tabbedPane;
    private JToolBar toolBar;
    private JToggleButton selectModeToggleButton;
    private JToggleButton adapterModeToggleButton,  simEventListenerModeToggleButton,  propertyChangeListenerModeToggleButton;
    private LegoTree legoEventGraphsTree, propertyChangeListenerTree;
    private JMenuBar            myMenuBar;
    private JMenuItem         quitMenuItem;
    private JMenuItem   newProjectMenuItem;
    private JMenuItem  openProjectMenuItem;
    private JMenuItem closeProjectMenuItem;
    private JMenuItem   zipProjectMenuItem;
    private JMenuItem      saveAllMenuItem;
    private JMenuItem userPreferencesMenuItem;
    private RecentProjectFileSetListener recentProjectFileSetListener;
    private RecentAssemblyFileListener   recentAssemblyFileListener; // TODO public?
    private AssemblyControllerImpl assemblyController;
    private JMenu projectMenu;
    private JMenu assemblyMenu;
    private JMenu editMenu;
    private JMenu helpMenu;
    
    private JMenu helpResourcesMenu;
    private JMenuItem simkitDesModelingManualMenuItem;
    private JMenuItem simkitJavaProgrammingVideosMenuItem;
    private JMenuItem simkitGithubMenuItem;
    private JMenuItem viskitGithubMenuItem;
    
    private JLabel metadataLabel;
    private JLabel modeLabel;
    private JLabel zoomLabel;

    private int untitledCount = 0;
    
    private JMenu     editAssemblySubMenu;
    private JMenuItem editMetadataPropertiesMenuItem;
    private JMenuItem closeAssemblyMenuItem;
    private JMenuItem closeAllAssembliesMenuItem;
    private JMenuItem saveAssemblyMenuItem;
    private JMenuItem saveAsAssemblyMenuItem;
    private JMenuItem assemblyGraphImageSave;
    private JMenuItem assemblyGenerateJavaSourceMenuItem;
    private JMenuItem assemblyXmlViewMenuItem;

    public AssemblyViewFrame(MvcController mvcController)
    {
        super(FRAME_DEFAULT_TITLE);
        initializeMVC(mvcController);   // set up mvc linkages
        initializeUserInterface(); // build widgets
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
     * @param mvcController the controller for this view
     */
    private void initializeMVC(MvcController mvcController)
    {
        setController(mvcController);
    }

    /**
     * Initialize the user interface
     */
    private void initializeUserInterface()
    {
        if (assemblyController == null)
            assemblyController = new AssemblyControllerImpl();
        
        buildAssemblyEditMenu(); // must be first
        buildAssemblyMenuItems();
        buildToolbar();
        buildProjectMenu();
        buildHelpMenu();

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

    public ViskitGraphAssemblyComponentWrapper getCurrentViskitGraphAssemblyComponentWrapper() 
    {
        JSplitPane splitPane = (JSplitPane) tabbedPane.getSelectedComponent();
        if (splitPane == null) {return null;}

        JScrollPane scrollPane = (JScrollPane) splitPane.getRightComponent();
        return (ViskitGraphAssemblyComponentWrapper) scrollPane.getViewport().getComponent(0);
    }

    /** get JGraph from window system display.TODO: how to crop image?
     * @return the component holding the JGraph image */
    public Component getCurrentJgraphComponent() 
    {
        ViskitGraphAssemblyComponentWrapper assemblyComponentWrapper = getCurrentViskitGraphAssemblyComponentWrapper();
        if (assemblyComponentWrapper == null || assemblyComponentWrapper.drawingSplitPane == null) 
        {
            return null;
        }
        return assemblyComponentWrapper.drawingSplitPane.getRightComponent();
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
    public RecentProjectFileSetListener getRecentProjectFileSetListener()
    {
        if (recentProjectFileSetListener == null)
            recentProjectFileSetListener = new RecentProjectFileSetListener();
        return recentProjectFileSetListener;
    }

    /** Tab switch: this will come in with the newly selected tab in place */
    class TabSelectionHandler implements ChangeListener 
    {
        @Override
        public void stateChanged(ChangeEvent changeEvent)
        {
            ViskitGraphAssemblyComponentWrapper viskitGraphAssemblyComponentWrapper = getCurrentViskitGraphAssemblyComponentWrapper();

            if (viskitGraphAssemblyComponentWrapper == null) {     // last tab has been closed
                setSelectedAssemblyName(null);
                return;
            }

            // Key to getting the LEGOs tree panel in each tab view
            viskitGraphAssemblyComponentWrapper.drawingSplitPane.setLeftComponent(viskitGraphAssemblyComponentWrapper.trees);

            setModel((MvcModel) viskitGraphAssemblyComponentWrapper.assemblyModel); // hold on locally
            getController().setModel(getModel()); // tell controller
            AssemblyModelImpl assemblyModel = (AssemblyModelImpl) getModel();
            
            // TODO alternative failing attempts
////            ViskitGlobals.instance().setActiveAssemblyModel(ViskitGlobals.instance().getActiveAssemblyModel());
//                               setModel((MvcModel) viskitGraphAssemblyComponentWrapper.assemblyModel); // hold on locally
//            assemblyController.setModel((MvcModel) viskitGraphAssemblyComponentWrapper.assemblyModel); // tell controller
//            AssemblyModelImpl assemblyModel = (AssemblyModelImpl) getModel(); // TODO not found in corresponding EventGraph method

            if (assemblyModel.getCurrentFile() != null)
                ((AssemblyControllerImpl) getController()).initializeOpenAssemblyWatch(assemblyModel.getCurrentFile(), assemblyModel.getJaxbRoot());

            GraphMetadata graphMetadata = assemblyModel.getMetadata();
            if ((graphMetadata != null) && (graphMetadata.name != null))
                setSelectedAssemblyName(graphMetadata.name.trim());
            else if (viskit.ViskitStatics.debug)
                LOG.error("error: AssemblyViewFrame graphMetadata null...");
        }
    }

    class RecentAssemblyFileListener implements MvcRecentFileListener
    {
        @Override
        public void listenerChanged()
        {
            String fileName;
            Action currentAction;
            JMenuItem menuItem;
            assemblyController = ViskitGlobals.instance().getActiveAssemblyController(); // TODO repetitive
            Set<String> filePathSet = assemblyController.getRecentAssemblyFileSet();
            openRecentAssemblyMenu.removeAll(); // clear prior to rebuilding menu
            openRecentAssemblyMenu.setEnabled(false); // disable unless file is found
            File file;
            for (String fullPath : filePathSet) 
            {
                file = new File(fullPath);
                if (!file.exists())
                {
                    // file not found as expected, something happened externally and so report it
                    LOG.error("*** [AssemblyViewFrame listChanged] Event graph file not found: " + file.getAbsolutePath());
                    continue; // actual file not found, skip to next file in files loop
                }
                fileName = file.getName();
                currentAction = new ParameterizedAssemblyAction(fileName);
                currentAction.putValue(ViskitStatics.FULL_PATH, fullPath);
                menuItem = new JMenuItem(currentAction);
                menuItem.setToolTipText(file.getPath());
                openRecentAssemblyMenu.add(menuItem);
                openRecentAssemblyMenu.setEnabled(true); // at least one is found
            }
            if (!filePathSet.isEmpty()) 
            {
                openRecentAssemblyMenu.add(new JSeparator());
                currentAction = new ParameterizedAssemblyAction("clear history");
                currentAction.putValue(ViskitStatics.FULL_PATH, ViskitStatics.CLEAR_PATH_FLAG);  // flag
                menuItem = new JMenuItem(currentAction);
                menuItem.setToolTipText("Clear this list");
                openRecentAssemblyMenu.add(menuItem);
            }
            // TODO note that some items might remain loaded after "clear menu" and so wondering if that is ambiguous
            
            // prior implementation is different pattern and so not continued
//            AssemblyController assemblyCcontroller = (AssemblyControllerImpl) getController();
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

    class ParameterizedAssemblyAction extends javax.swing.AbstractAction 
    {
        ParameterizedAssemblyAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            File fullPathFile;
            Object obj = getValue(ViskitStatics.FULL_PATH);
            if (obj instanceof String)
                fullPathFile = new File((String) obj);
            else
                fullPathFile = (File) obj;

            if (fullPathFile != null)
            {
                if (fullPathFile.getPath().equals(CLEARPATHFLAG))
                    assemblyController.clearRecentAssemblyFileList();
                else
                    assemblyController.openRecentAssembly(fullPathFile);
            }
        }
    }

    private void buildAssemblyEditMenu()
    {
        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        ActionIntrospector.getAction(assemblyController, METHOD_undo).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_redo).setEnabled(false);
        
        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(assemblyController, METHOD_cut).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_remove).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_copy).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_paste).setEnabled(false);

        // Set up edit menu
        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        
//        JMenu whichMenu;
//        if  (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
//             whichMenu = editMenu;
//        else whichMenu = assemblyMenu;

        editMenu.add(buildMenuItem(assemblyController, METHOD_undo, "Undo", KeyEvent.VK_U,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK)));
        editMenu.add(buildMenuItem(assemblyController, METHOD_redo, "Redo", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.ALT_DOWN_MASK)));
        editMenu.addSeparator();

        // the next four are disabled until something is selected
        editMenu.add(buildMenuItem(assemblyController, METHOD_cut, "Cut", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK)));
        editMenu.getItem(editMenu.getItemCount()-1).setToolTipText("Cut is not supported in Viskit.");
        editMenu.add(buildMenuItem(assemblyController, METHOD_copy, "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK)));
        editMenu.add(buildMenuItem(assemblyController, METHOD_paste, "Paste Event Node", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK)));
        editMenu.add(buildMenuItem(assemblyController, METHOD_remove, "Delete", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK)));
        editMenu.addSeparator();

        editMenu.add(buildMenuItem(assemblyController, METHOD_newEventGraphNode, "Add a new Event Graph", KeyEvent.VK_A, null));
        editMenu.add(buildMenuItem(assemblyController, METHOD_newPropertyChangeListenerNode, "Add a new Property Change Listener", KeyEvent.VK_A, null));
        editMenu.addSeparator();

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        editMenu.add(buildMenuItem(assemblyController, METHOD_editGraphMetadata, "Edit selected Assembly Metadata Properties", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK)));
        }
    }
    
    public void enableAssemblyMenuItems()
    {
        boolean isAssemblyLoaded = (ViskitGlobals.instance().hasViskitProject() && ViskitGlobals.instance().isProjectOpen() &&
                                    hasAssembliesLoaded());
        editAssemblySubMenu.setEnabled(isAssemblyLoaded);
        editMetadataPropertiesMenuItem.setEnabled(isAssemblyLoaded);
        closeAssemblyMenuItem.setEnabled(isAssemblyLoaded);
        closeAllAssembliesMenuItem.setEnabled(isAssemblyLoaded);
        saveAssemblyMenuItem.setEnabled(isAssemblyLoaded);
        saveAsAssemblyMenuItem.setEnabled(isAssemblyLoaded);
        assemblyGraphImageSave.setEnabled(isAssemblyLoaded);
        assemblyGenerateJavaSourceMenuItem.setEnabled(isAssemblyLoaded);
        assemblyXmlViewMenuItem.setEnabled(isAssemblyLoaded);
    }

    private void buildAssemblyMenuItems()
    {
        recentAssemblyFileListener = new RecentAssemblyFileListener();
        assemblyController.addRecentAssemblyFileSetListener(getRecentAssemblyFileListener());

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Set up file menu
        assemblyMenu = new JMenu("Assembly"); // Editor
        assemblyMenu.setMnemonic(KeyEvent.VK_A);
        assemblyMenu.addActionListener((ActionEvent e) -> {
            enableAssemblyMenuItems();
        });

        // Set up edit submenu
        editAssemblySubMenu = new JMenu("Edit Assembly...");
        editAssemblySubMenu.setToolTipText("Edit functions for selected Asssembly");
        editAssemblySubMenu.setMnemonic(KeyEvent.VK_E);
        if (!ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus()) // combined menu integration
        {
        ActionIntrospector.getAction(assemblyController, METHOD_undo).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_redo).setEnabled(false);
        
        // These start off being disabled, until something is selected
        ActionIntrospector.getAction(assemblyController, METHOD_cut).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_remove).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_copy).setEnabled(false);
        ActionIntrospector.getAction(assemblyController, METHOD_paste).setEnabled(false);

        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_undo, "Undo", KeyEvent.VK_Z,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_redo, "Redo", KeyEvent.VK_Y,
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        editAssemblySubMenu.addSeparator();

        // the next four are disabled until something is selected
        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_cut, "Cut", KeyEvent.VK_X,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        editAssemblySubMenu.getItem(editAssemblySubMenu.getItemCount()-1).setToolTipText("Cut is not supported in Viskit.");
        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_copy, "Copy", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_paste, "Paste Event Node", KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_remove, "Delete", KeyEvent.VK_DELETE,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        editAssemblySubMenu.addSeparator();

        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_newEventGraphNode, "Add a new Event Graph", KeyEvent.VK_G, null));
        editAssemblySubMenu.add(buildMenuItem(assemblyController, METHOD_newPropertyChangeListenerNode, "Add a new Property Change Listener", KeyEvent.VK_L, null));
        
        assemblyMenu.add(editAssemblySubMenu);
        editMetadataPropertiesMenuItem = buildMenuItem(assemblyController, METHOD_editGraphMetadata, "Edit Assembly Metadata", KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        editMetadataPropertiesMenuItem.setToolTipText("Edit selected Assembly Metadata Properties");
        assemblyMenu.add(editMetadataPropertiesMenuItem);
        assemblyMenu.addSeparator();
        }

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        assemblyMenu.add(buildMenuItem(assemblyController, METHOD_newProject, "New Viskit Project", KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK)));
        
        assemblyMenu.add(buildMenuItem(this, METHOD_openProject, "Open Project", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK)));
        assemblyMenu.add(openRecentProjectMenu = buildMenu("Open Recent Project"));
        openRecentProjectMenu.setMnemonic('O');
        openRecentProjectMenu.setEnabled(false); // inactive until needed, reset by listener
        
        assemblyMenu.add(buildMenuItem(this, METHOD_closeProject, "Close Project", KeyEvent.VK_C,
                null));
        
        assemblyMenu.add(buildMenuItem(assemblyController, METHOD_zipAndMailProject, "Zip/Email Viskit Project", KeyEvent.VK_Z, null));
        assemblyMenu.addSeparator();
        } // end hasOriginalModalMenus
        
        // The AssemblyViewFrame will get this listener for its menu item of the same name
        recentProjectFileSetListener = new RecentProjectFileSetListener();
        recentProjectFileSetListener.addMenuItem(openRecentProjectMenu);
        assemblyController.addRecentProjectFileSetListener(getRecentProjectFileSetListener());
        
        assemblyMenu.add(buildMenuItem(assemblyController, METHOD_newAssembly, "New Assembly", KeyEvent.VK_N,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));

        assemblyMenu.add(buildMenuItem(assemblyController, METHOD_open, "Open Assembly", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        assemblyMenu.add(openRecentAssemblyMenu = buildMenu("Open Recent Assembly"));
        openRecentAssemblyMenu.setMnemonic('O');
        openRecentAssemblyMenu.setEnabled(false); // inactive until needed, reset by listener

        assemblyMenu.addSeparator();
        
        closeAssemblyMenuItem = buildMenuItem(assemblyController, METHOD_close, "Close Assembly", KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        assemblyMenu.add(closeAssemblyMenuItem);

        closeAllAssembliesMenuItem = buildMenuItem(assemblyController, METHOD_closeAll, "Close All Assemblies", KeyEvent.VK_C, null);
        assemblyMenu.add(closeAllAssembliesMenuItem);

        saveAssemblyMenuItem = buildMenuItem(assemblyController, METHOD_save, "Save Assembly", KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        assemblyMenu.add(saveAssemblyMenuItem);

        saveAsAssemblyMenuItem = buildMenuItem(assemblyController, METHOD_saveAs, "Save Assembly as...", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        assemblyMenu.add(saveAsAssemblyMenuItem);

        assemblyMenu.addSeparator();

        assemblyGraphImageSave = buildMenuItem(assemblyController, METHOD_captureWindow, "Image Save", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        assemblyGraphImageSave.setToolTipText("Image Save for Assembly Diagram");
        assemblyMenu.add(assemblyGraphImageSave);

        assemblyGenerateJavaSourceMenuItem = buildMenuItem(assemblyController, METHOD_generateJavaCode, "Java Source Generation", KeyEvent.VK_J, // TODO confirm "saved"
                KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        assemblyGenerateJavaSourceMenuItem.setToolTipText("Java Source Generation and Compilation for Saved Assembly");
        assemblyMenu.add(assemblyGenerateJavaSourceMenuItem);

        assemblyXmlViewMenuItem = buildMenuItem(assemblyController, METHOD_viewXML, "XML View", KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        assemblyXmlViewMenuItem.setToolTipText("XML View of Saved Assembly");
        assemblyMenu.add(assemblyXmlViewMenuItem);
        
        
/* TODO fix functionality before exposing
        assemblyMenu.addSeparator();
        // TODO add icon?
        assemblyMenu.add(buildMenuItem(assemblyController, METHOD_prepareSimulationRunner, "Initialize Assembly for Simulation Run", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
*/

        /*
        // TODO: Unknown as to what this does exactly
        assemblyMenu.add(buildMenuItem(assemblyController, "export2grid", "Export to Cluster Format", KeyEvent.VK_C, null));
        ActionIntrospector.getAction(assemblyController, "export2grid").setEnabled(false);
        */

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        assemblyMenu.addSeparator();
        assemblyMenu.add(buildMenuItem(assemblyController, METHOD_showViskitUserPreferences, "Viskit User Preferences", null, null));
        assemblyMenu.addSeparator();

        assemblyMenu.add(quitMenuItem = buildMenuItem(assemblyController, METHOD_quit, "Quit", KeyEvent.VK_Q,
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, accelMod)));
        } // end hasOriginalModalMenus
        
        // Create a new menu bar and add the menus we created above to it
        myMenuBar = new JMenuBar();
        myMenuBar.add(assemblyMenu);
        myMenuBar.add(editMenu);
        
        enableAssemblyMenuItems();

        // Help editor created by the EGVF for all of Viskit's UIs
    }

    public void enableProjectMenuItems()
    {
        // TODO fix these flags
        boolean isProjectLoaded = (ViskitGlobals.instance().hasViskitProject() || ViskitGlobals.instance().isProjectOpen());
        closeProjectMenuItem.setEnabled(isProjectLoaded);
          zipProjectMenuItem.setEnabled(isProjectLoaded);
             saveAllMenuItem.setEnabled(isProjectLoaded && 
                     (ViskitGlobals.instance().hasDirtyAssembly() ||
                      ViskitGlobals.instance().hasDirtyEventGraph()));
          
        if  (ViskitGlobals.instance().isProjectOpen())
             openProjectMenuItem.setToolTipText("Open an existing Viskit project, closing current project");
        else openProjectMenuItem.setToolTipText("Open an existing Viskit project");
        
        if  (ViskitGlobals.instance().isProjectOpen())
             openRecentProjectMenu.setToolTipText("Select and open Recent Project, closing current project");
        else openRecentProjectMenu.setToolTipText("Select and open Recent Project");
    }

    private void buildProjectMenu()
    {
        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // TODO repetitive
        
        projectMenu = new JMenu("Project");
        projectMenu.setMnemonic(KeyEvent.VK_P);
        projectMenu.setToolTipText("Viskit Project operations");
        projectMenu.addActionListener((ActionEvent e) -> {
            enableProjectMenuItems();
        });

        newProjectMenuItem = buildMenuItem(assemblyController, METHOD_newProject, "New Project", KeyEvent.VK_N,
                null);
        newProjectMenuItem.setToolTipText("Create a new Viskit project");
        projectMenu.add(newProjectMenuItem);
        
        openProjectMenuItem = buildMenuItem(this, METHOD_openProject, "Open Project", KeyEvent.VK_O,
                null);
        if  (ViskitGlobals.instance().isProjectOpen())
             openProjectMenuItem.setToolTipText("Open an existing Viskit project, closing current project");
        else openProjectMenuItem.setToolTipText("Open an existing Viskit project");
        projectMenu.add(openProjectMenuItem);
        
        openRecentProjectMenu = buildMenu("Open Recent Project");
        if  (ViskitGlobals.instance().isProjectOpen())
             openRecentProjectMenu.setToolTipText("Select and open Recent Project, closing current project");
        else openRecentProjectMenu.setToolTipText("Select and open Recent Project");
        projectMenu.add(openRecentProjectMenu);
        openRecentProjectMenu.setMnemonic('O');
        openRecentProjectMenu.setEnabled(false); // inactive until needed, reset by project listener

        // The AssemblyViewFrame will get this listener for its menu item of the same name
        recentProjectFileSetListener = new RecentProjectFileSetListener();
        getRecentProjectFileSetListener().addMenuItem(openRecentProjectMenu);
        assemblyController.addRecentProjectFileSetListener(getRecentProjectFileSetListener());
        
        closeProjectMenuItem =     buildMenuItem(this, METHOD_closeProject,                            "Close Project", KeyEvent.VK_C,
                null);
        closeProjectMenuItem.setToolTipText("Close the current Viskit project");
        projectMenu.add(closeProjectMenuItem);
        
        saveAllMenuItem = buildMenuItem(assemblyController, METHOD_saveAll,                "Save All Modified Models", KeyEvent.VK_Z,
                null);
        saveAllMenuItem.setToolTipText("Save all modified Assembly and Event Graph models in this project");
        projectMenu.add(saveAllMenuItem);
        
        zipProjectMenuItem = buildMenuItem(assemblyController, METHOD_zipProject,                "Zip Project", KeyEvent.VK_Z,
                null);
        zipProjectMenuItem.setToolTipText("Zip the current Viskit project");
        projectMenu.add(zipProjectMenuItem);

        projectMenu.addSeparator();
        
        userPreferencesMenuItem = buildMenuItem(assemblyController, METHOD_showViskitUserPreferences, "Viskit User Preferences", KeyEvent.VK_V, null);
        userPreferencesMenuItem.setToolTipText("Edit and save Viskit User Preference");
        projectMenu.add(userPreferencesMenuItem);

        quitMenuItem = buildMenuItem(assemblyController, METHOD_quit,                      "Quit", KeyEvent.VK_Q,
                null);
        quitMenuItem.setToolTipText("Exit and close Viskit");
        projectMenu.add(quitMenuItem);
    }
    
    public void buildHelpMenu()
    {
        viskit.Help help = new viskit.Help(ViskitGlobals.instance().getMainFrame());
//        help.mainFrameLocated(ViskitGlobals.instance().getMainFrame().getBounds()); // this fails, apparently puts center at (0, 0)
        
        ViskitGlobals.instance().setHelp(help); // single instance for all viskit frames

        helpMenu = new JMenu("Help");
        getHelpMenu().setMnemonic(KeyEvent.VK_H);

        // https://stackoverflow.com/questions//15266351/make-f1-shortcut-key-in-swing
        getHelpMenu().add(buildMenuItem(help, METHOD_doContents,  "Contents",     KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_F1,0)));
        getHelpMenu().add(buildMenuItem(help, METHOD_doSearch,    "Search",       KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.SHIFT_DOWN_MASK)));
        getHelpMenu().add(buildMenuItem(help, METHOD_doTutorial,  "Tutorial",     KeyEvent.VK_T, null));
        
        getHelpMenu().addSeparator();
        helpResourcesMenu = new JMenu("Resources");
        
        simkitDesModelingManualMenuItem = buildMenuItem(help, METHOD_launchSimkitDesModelingManual, "Simkit DES Modeling Manual", KeyEvent.VK_M, null);
        simkitJavaProgrammingVideosMenuItem = buildMenuItem(help, METHOD_launchMV3302SimkitJavaProgrammingVideos, "Simkit Java Programming Videos", KeyEvent.VK_V, null);
        simkitGithubMenuItem = buildMenuItem(help, METHOD_launchGithubSimkit, "Github Simkit Repository", KeyEvent.VK_G, null);
        viskitGithubMenuItem = buildMenuItem(help, METHOD_launchGithubViskit, "Github Viskit Repository", KeyEvent.VK_G, null);
                
        helpResourcesMenu.add(simkitDesModelingManualMenuItem);
        helpResourcesMenu.add(simkitJavaProgrammingVideosMenuItem);
        helpResourcesMenu.add(simkitGithubMenuItem);
        helpResourcesMenu.add(viskitGithubMenuItem);
        helpMenu.add(helpResourcesMenu);
        
        getHelpMenu().add(buildMenuItem(help, METHOD_aboutViskit, "About Viskit", KeyEvent.VK_A, null));
    }

    private JMenu buildMenu(String name) 
    {
        return new JMenu(name);
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String methodName, String menuItemName, Integer mnemonicKeyEvent, KeyStroke accelleratorKeyStroke) 
    {
        Action action = ActionIntrospector.getAction(source, methodName);
        if (action == null)
        {
            LOG.error("buildMenuItem reflection failed for name=" + menuItemName + " method=" + methodName + " in " + source.toString());
            return new JMenuItem(menuItemName + "(not working, reflection failed) ");
        }
        Map<String, Object> map = new HashMap<>();
        if (mnemonicKeyEvent != null)
            map.put(Action.MNEMONIC_KEY, mnemonicKeyEvent);
        if (accelleratorKeyStroke != null)
            map.put(Action.ACCELERATOR_KEY, accelleratorKeyStroke);
        if (menuItemName != null) {
            map.put(Action.NAME, menuItemName);
        }
        if (!map.isEmpty())
            ActionUtilities.decorateAction(action, map);

        return ActionUtilities.createMenuItem(action);
    }

    /**
     * @return the current mode--select, add, schedulingEdge, cancelingEdge
     */
    public int getCurrentMode() 
    {
        // Use the button's selected status to figure out what mode
        // we are in.

        if (selectModeToggleButton.isSelected())
            return SELECT_MODE;
        if (adapterModeToggleButton.isSelected())
            return ADAPTER_MODE;
        if (simEventListenerModeToggleButton.isSelected())
            return SIMEVENT_LISTENER_MODE;
        if (propertyChangeListenerModeToggleButton.isSelected())
            return PROPERTY_CHANGE_LISTENER_MODE;
        LOG.error("assert false : \"getCurrentMode()\""); // TODO ???
        return 0;
    }

    private void buildToolbar() 
    {
        ButtonGroup modeButtonGroup = new ButtonGroup();
        setToolBar(new JToolBar());

        metadataLabel = new JLabel("Metadata: ");
        metadataLabel.setToolTipText("Edit Assembly Metadata");
        
        JButton metadataButton = makeButton(null, "viskit/images/Information24.gif",
                "Edit Assembly Metadata");
        metadataButton.addActionListener((ActionEvent e) -> {
            ((AssemblyControllerImpl) getController()).editGraphMetadata();
        });

        // Buttons for what mode we are in

        selectModeToggleButton = makeJToggleButton(
                null,
                "viskit/images/selectNode.png",
                "Select an item on the graph"
        );
        Border defBor = selectModeToggleButton.getBorder();
        selectModeToggleButton.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(Color.lightGray, 2)));

        adapterModeToggleButton = makeJToggleButton(
                null,
                new AdapterIcon(24, 24),
                "Connect SimEntities with adapter pattern"
        );
        defBor = adapterModeToggleButton.getBorder();
        adapterModeToggleButton.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        simEventListenerModeToggleButton = makeJToggleButton(
                null,
                new SimEventListenerIcon(24, 24),
                "Connect SimEntities through a SimEvent listener pattern"
        );
        defBor = simEventListenerModeToggleButton.getBorder();
        simEventListenerModeToggleButton.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xce, 0xce, 0xff), 2)));

        propertyChangeListenerModeToggleButton = makeJToggleButton(null,
                new PropertyChangeListenerIcon(24, 24),
                "Connect a property change listener to a SimEntity"
        );
        defBor = propertyChangeListenerModeToggleButton.getBorder();
        propertyChangeListenerModeToggleButton.setBorder(BorderFactory.createCompoundBorder(defBor, BorderFactory.createLineBorder(new Color(0xff, 0xc8, 0xc8), 2)));

        JButton zoomInButton = makeButton(
                null,
                "viskit/images/ZoomIn24.gif",
                "Zoom in towards the graph"
        );

        JButton zoomOutButton = makeButton(
                null,
                "viskit/images/ZoomOut24.gif",
                "Zoom out from the graph"
        );

        Action prepareSimulationRunnerAction = ActionIntrospector.getAction(getController(), METHOD_prepareSimulationRunner);
        prepareAssemblyForSimulationRunButton = makeButton(prepareSimulationRunnerAction, 
             "viskit/images/Play24.gif", // large
                "Compile and initialize the assembly, prepare for Simulation Run");
        modeButtonGroup.add(selectModeToggleButton);
        modeButtonGroup.add(adapterModeToggleButton);
        modeButtonGroup.add(simEventListenerModeToggleButton);
        modeButtonGroup.add(propertyChangeListenerModeToggleButton);

        // Make selection mode the default mode
        selectModeToggleButton.setSelected(true);
        
        getToolBar().add(metadataLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(metadataButton);
        getToolBar().addSeparator(new Dimension(24, 24));

        modeLabel = new JLabel("Mode: ");
        modeLabel.setToolTipText("Select editing mode");
        getToolBar().add(modeLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
        
        getToolBar().addSeparator(new Dimension(24, 24));

        getToolBar().add(selectModeToggleButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(adapterModeToggleButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(simEventListenerModeToggleButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().addSeparator(new Dimension(24, 24));
        getToolBar().add(propertyChangeListenerModeToggleButton);

        getToolBar().add(new JLabel("Zoom: "));
        zoomLabel = new JLabel("Zoom: ");
        zoomLabel.setToolTipText("Zoom in or out");
        getToolBar().add(zoomLabel);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomInButton);
        getToolBar().addSeparator(new Dimension(5, 24));
        getToolBar().add(zoomOutButton);
        getToolBar().addSeparator(new Dimension(24, 24));

        getToolBar().add(prepareAssemblyForSimulationRunButton);
        JLabel prepareAssemblyForSimulationRunLabel = new JLabel ("  Initialize Assembly for Simulation Run");
        prepareAssemblyForSimulationRunLabel.setToolTipText(INITIAL_SIMULATION_RUN_HINT);
        getToolBar().add(prepareAssemblyForSimulationRunLabel);

        // Let the opening of Assembliess make this visible
        getToolBar().setVisible(false);

        zoomInButton.addActionListener((ActionEvent e) -> {
            getCurrentViskitGraphAssemblyComponentWrapper().setScale(getCurrentViskitGraphAssemblyComponentWrapper().getScale() + 0.1d);
        });
        zoomOutButton.addActionListener((ActionEvent e) -> {
            getCurrentViskitGraphAssemblyComponentWrapper().setScale(Math.max(getCurrentViskitGraphAssemblyComponentWrapper().getScale() - 0.1d, 0.1d));
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
                getCurrentViskitGraphAssemblyComponentWrapper().setPortsVisible(tOrF);
            }
        }

        PortsVisibleListener portsOn  = new PortsVisibleListener(true);
        PortsVisibleListener portsOff = new PortsVisibleListener(false);
                    selectModeToggleButton.addActionListener(portsOff);
                   adapterModeToggleButton.addActionListener(portsOn);
          simEventListenerModeToggleButton.addActionListener(portsOn);
        propertyChangeListenerModeToggleButton.addActionListener(portsOn);
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

    private JButton makeButton(Action action, String iconPath, String tooltip) {
        JButton button;
        if (action != null)
            button = new JButton(action);
        else
            button = new JButton();
        return (JButton) buttonCommon(button, iconPath, tooltip);
    }

    private AbstractButton buttonCommon(AbstractButton button, String iconPath, String tooltip) 
    {
        button.setIcon(new ImageIcon(getClass().getClassLoader().getResource(iconPath)));
        return buttonCommon2(button, tooltip);
    }

    private AbstractButton buttonCommon2(AbstractButton button, String tooltip) 
    {
        button.setToolTipText(tooltip);
        button.setBorder(BorderFactory.createEtchedBorder());
        button.setText(null);
        return button;
    }

    @Override
    public void addTab(AssemblyModel assemblyModel)
    {
        ViskitGraphAssemblyModel vGAmod = new ViskitGraphAssemblyModel();
        ViskitGraphAssemblyComponentWrapper graphPane = new ViskitGraphAssemblyComponentWrapper(vGAmod, this);
        vGAmod.setjGraph(graphPane); // TODO fix this

        graphPane.assemblyModel = assemblyModel;
        graphPane.trees = treePanelsSplitPane;
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
            graphPane.getDropTarget().addDropTargetListener(new ViskitDropTargetAdapter());
        } catch (TooManyListenersException tmle) {
            LOG.error(tmle);
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
//            if (assemblyController == null)
//                assemblyController = ViskitGlobals.instance().getActiveAssemblyController(); // unexpected
//            assemblyController.makeTopPaneAssemblyTabActive();
            ViskitGlobals.instance().selectAssemblyEditorTab();
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void deleteTab(AssemblyModel mod) 
    {
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
    public AssemblyModel[] getOpenAssemblyModels() 
    {
        Component[] ca = tabbedPane.getComponents();
        AssemblyModel[] assemblyModelArray = new AssemblyModel[ca.length];
        JSplitPane splitPane;
        JScrollPane scrollPane;
        ViskitGraphAssemblyComponentWrapper graphAssemblyComponentWrapper;
        for (int i = 0; i < assemblyModelArray.length; i++) {
            splitPane = (JSplitPane) ca[i];
            scrollPane = (JScrollPane) splitPane.getRightComponent();
            graphAssemblyComponentWrapper = (ViskitGraphAssemblyComponentWrapper) scrollPane.getViewport().getComponent(0);
            assemblyModelArray[i] = graphAssemblyComponentWrapper.assemblyModel;
        }
        return assemblyModelArray;
    }

    /** Rebuilds the Listener Event Graph Object (LEGO) tree view */
    public void rebuildLEGOTreePanels() 
    {
        if (getCurrentViskitGraphAssemblyComponentWrapper() == null)
            return; // no LEGO panel yet

        legoEventGraphsTree.clear();
        propertyChangeListenerTree.clear();
        JSplitPane treeSplit = buildTreePanels();
        getCurrentViskitGraphAssemblyComponentWrapper().drawingSplitPane.setTopComponent(treeSplit);
        treeSplit.setDividerLocation(250);
        legoEventGraphsTree.repaint();
        propertyChangeListenerTree.repaint();
    }
    private JSplitPane treePanelsSplitPane;

    private JSplitPane buildTreePanels() 
    {
        legoEventGraphsTree = new LegoTree("simkit.BasicSimEntity", "viskit/images/assembly.png",
                this, "Drag an Event Graph onto the canvas to add it to the assembly");

        propertyChangeListenerTree = new LegoTree("java.beans.PropertyChangeListener", new PropertyChangeListenerImageIcon(20, 20),
                this, "Drag a PropertyChangeListener onto the canvas to add it to the assembly");

        // Parse any extra/additional classpath for any required dependencies
        File file;
        try {
            URL[] extraClassPathUrlArray = ViskitUserPreferencesDialog.getExtraClassPathArraytoURLArray();
            for (URL path : extraClassPathUrlArray) { // tbd same for pcls
                if (path == null)
                    continue; // can happen if extraClassPaths.path[@value] is null or erroneous
                file = new File(path.toURI());
                if (file.exists())
                    addEventGraphsToLegoTree(file, file.isDirectory()); // recurse directories
            }
        } 
        catch (URISyntaxException uriSyntaxException) {
            LOG.error("buildTreePanels() {}", uriSyntaxException);
        }

        // Now add our EventGraphs path for LEGO tree inclusion of our SimEntities
        ViskitProject viskitProject = ViskitGlobals.instance().getViskitProject();

        // A RunSimulation (reset) LocalBootLoader will be instantiated
        // here when compiling EGs for the first time, or when the
        // SimkitXML2Java translator attempts to resolve a ParameterMap
        if (viskitProject != null)
        {
            if ((legoEventGraphsTree != null) && (legoEventGraphsTree.getRowCount() > 0))
            {
                LOG.info("buildTreePanels() check previous event graphs compiled...");
                addEventGraphsToLegoTree(viskitProject.getEventGraphsDirectory(), true);
                LOG.info("buildTreePanels() check previous event graphs complete");
            }            
        }

        // Now load the simkit.jar and diskit.jar from where ever they happen to
        // be located on the classpath if present
        String[] classPath = ((LocalBootLoader) ViskitGlobals.instance().getViskitApplicationClassLoader()).getClassPath();
        for (String path : classPath)
        {
            if (path.contains("simkit.jar") || (path.contains("diskit.jar"))) 
            {
                            addEventGraphsToLegoTree(new File(path), false);
                addPropertyChangeListenersToLegoTree(new File(path), false);
            }
        }
               LegosEventGraphsPanel       legosEventGraphsPanel = new LegosEventGraphsPanel(legoEventGraphsTree);
        PropertyChangeListenersPanel propertyChangeListenerPanel = new PropertyChangeListenersPanel(propertyChangeListenerTree);

               legoEventGraphsTree.setBackground(background);
        propertyChangeListenerTree.setBackground(background);

        treePanelsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, legosEventGraphsPanel, propertyChangeListenerPanel);
        treePanelsSplitPane.setBorder(new EmptyBorder(10, 0, 10, 0));
        treePanelsSplitPane.setOneTouchExpandable(true);

        propertyChangeListenerPanel.setMinimumSize(new Dimension(20, 80));
              legosEventGraphsPanel.setMinimumSize(new Dimension(20, 80));
              legosEventGraphsPanel.setPreferredSize(new Dimension(20, 240)); // give it some height for the initial split

        legoEventGraphsTree.setDragEnabled(true);
        propertyChangeListenerTree.setDragEnabled(true);

        return treePanelsSplitPane;
    }
    Transferable draggedTransferable;

    @Override
    public void startingDrag(Transferable transferableInput) {
        draggedTransferable = transferableInput;
    }

    /** Class to facilitate dragging new nodes onto the palette */
    class ViskitDropTargetAdapter extends DropTargetAdapter 
    {
        @Override
        public void dragOver(DropTargetDragEvent dropTargetDragEvent) 
        {
            // NOTE: this action is very critical in getting JGraph 5.14 to signal the drop method
            dropTargetDragEvent.acceptDrag(dropTargetDragEvent.getDropAction());
        }

        @Override
        public void drop(DropTargetDropEvent dropTargetDropEvent) 
        {
            if (draggedTransferable != null) 
            {
                try {
                    Point p = dropTargetDropEvent.getLocation();

                    String s = draggedTransferable.getTransferData(DataFlavor.stringFlavor).toString();
                    String[] sa = s.split("\t");

                    // Check for XML-based node
                    FileBasedAssemblyNode xmlFileBasedAssemblyNode = isFileBasedAssemblyNode(sa[1]);
                    if (xmlFileBasedAssemblyNode != null) 
                    {
                        switch (sa[0]) {
                            case "simkit.BasicSimEntity":
                                ((AssemblyControllerImpl) getController()).newFileBasedEventGraphNode(xmlFileBasedAssemblyNode, p);
                                break;
                            case "java.beans.PropertyChangeListener":
                                ((AssemblyControllerImpl) getController()).newFileBasedPropertyChangeListenerNode(xmlFileBasedAssemblyNode, p);
                                break;
                        }
                    } 
                    else {
                        // Else class-based node
                        switch (sa[0]) {
                            case "simkit.BasicSimEntity":
                                ((AssemblyControllerImpl) getController()).newEventGraphNode(sa[1], p);
                                break;
                            case "java.beans.PropertyChangeListener":
                                ((AssemblyControllerImpl) getController()).newPropertyChangeListenerNode(sa[1], p);
                                break;
                        }
                    }
                    draggedTransferable = null;
                } 
                catch (UnsupportedFlavorException | IOException e) {
                    LOG.error(e);
                }
            }
        }
    }

    private FileBasedAssemblyNode isFileBasedAssemblyNode(String nodeName) 
    {
        try {
            return FileBasedAssemblyNode.fromString(nodeName);
        } 
        catch (FileBasedAssemblyNode.exception e) {
            return null;
        }
    }

    @Override
    public void modelChanged(MvcModelEvent modelEvent) 
    {
        switch (modelEvent.getID()) {
            default:
                getCurrentViskitGraphAssemblyComponentWrapper().viskitModelChanged((ModelEvent) modelEvent);
                break;
        }
        // Let model.isDirty() determine status color
        toggleAssemblyStatusIndicators();
        enableAssemblyMenuItems();
    }

    /** Changes the background/foreground color of Assembly tabs depending on
     * model.isDirty() status giving the user an indication of a good/bad
     * save and compile operation. Of note is that the default Look+Feel must be
     * selected for WIN machines, else no colors will be visible. On Macs, the
     * platform Look+Feel works best.
     */
    public void toggleAssemblyStatusIndicators()
    {
        int originalSelectedTabIndex = tabbedPane.getSelectedIndex();
        for (Component currentSwingComponent : tabbedPane.getComponents()) 
        {
            // This will fire a call to stateChanged() which also sets the current model
            tabbedPane.setSelectedComponent(currentSwingComponent);
            // TODO tooltip text hints not appearing

            if (((AssemblyModelImpl) getModel()).isModelDirty())
            {
                // background color changes look excessive
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
        if (tabbedPane.getSelectedIndex() != originalSelectedTabIndex)
            tabbedPane.setSelectedIndex(originalSelectedTabIndex);
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
        return getLeafUO(legoEventGraphsTree);
    }

    @Override
    public Object getSelectedPropertyChangeListener() {
        return getLeafUO(propertyChangeListenerTree);
    }

    @Override
    public void addEventGraphsToLegoTree(File file, boolean recurse) 
    {
        if ((file != null) && file.exists())
            legoEventGraphsTree.addContentRoot(file, recurse);
    }

    @Override
    public void addPropertyChangeListenersToLegoTree(File f, boolean b) {
        propertyChangeListenerTree.addContentRoot(f, b);
    }

    @Override
    public void removeEventGraphFromLEGOTree(File f) {
        legoEventGraphsTree.removeContentRoot(f);
    }

    // Not used
    @Override
    public void removePropertyChangeListenerFromLEGOTree(File f) {
        propertyChangeListenerTree.removeContentRoot(f);
    }

    // ViskitView-required methods:
    private JFileChooser openSaveChooser;
    
    private JFileChooser buildOpenSaveChooser() 
    {
        // Try to open in the current project directory for Assemblies
        if (ViskitGlobals.instance().getViskitProject() != null)
            return new JFileChooser(ViskitGlobals.instance().getViskitProject().getAssembliesDirectory());
        else
            return new JFileChooser(new File(ViskitProject.VISKIT_PROJECTS_DIRECTORY));
    }

    @Override
    public File[] openFilesAsk() {
        openSaveChooser = buildOpenSaveChooser();
        openSaveChooser.setDialogTitle("Open Assembly Files");

        // Look for assembly in the filename, Bug 1247 fix
        FileFilter filter = new AssemblyFileFilter("assembly"); // TODO too restrictive
        openSaveChooser.setFileFilter(filter);
        openSaveChooser.setMultiSelectionEnabled(true);

        int returnValue = openSaveChooser.showOpenDialog(this);
        return (returnValue == JFileChooser.APPROVE_OPTION) ? openSaveChooser.getSelectedFiles() : null;
    }

    @Override
    public File openRecentFilesAsk(Collection<String> fileCollection) {
        String fn = RecentFilesDialog.showDialog(this, fileCollection);
        if (fn != null) {
            File file = new File(fn);
            if (file.exists())
                return file;
            else
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, "File not found.", file + " does not exist");
        }
        return null;
    }

    @Override
    public void setSelectedAssemblyName(String newName) 
    {
        boolean nullString = !(newName != null && !newName.isEmpty()); // TODO ! isBlank()
        if  ((!nullString) && (tabbedPane != null))
             tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), newName);
    }

    public void closeProject()
    {
        assemblyController = ViskitGlobals.instance().getActiveAssemblyController();
        
        assemblyController.closeProject();

//        ViskitProjectSelectionPanel viskitProjectSelectionPanel = new ViskitProjectSelectionPanel();
//        viskitProjectSelectionPanel.showDialog();
    }

    @Override
    public void openProject()
    {
        assemblyController = ViskitGlobals.instance().getActiveAssemblyController();

        if (!assemblyController.closeProject())
            return;

        File file = ViskitProject.openProjectDirectory(this.getContent(), ViskitProject.VISKIT_PROJECTS_DIRECTORY);
        if (file != null)
        {
            assemblyController.openProject(file); // calls EGVF setTitleProjectName
            String projectName = new String();
            if (file.exists())
                projectName = file.getName(); // TODO someday, also handle using project metadata <Project name="whassup"/>
            ViskitGlobals.instance().setTitleProjectName(projectName);
            MainFrame.displayWelcomeGuidance(); // if no event graph or assembly is open
        }
    }

    private File getUniqueName(String suggestedName) 
    {
        String appendCount = "";
        String suffix      = "";

        int lastDot = suggestedName.lastIndexOf('.');
        if (lastDot != -1) {
            suffix = suggestedName.substring(lastDot);
            suggestedName = suggestedName.substring(0, lastDot);
        }
        int count = -1;
        File uniqueFile = null;
        do {
            uniqueFile  = new File(suggestedName + appendCount + suffix);
            appendCount = "" + ++count;
        } while (uniqueFile.exists());

        return uniqueFile;
    }

    @Override
    public File saveFileAsk(String suggestedPath, boolean showUniqueName, String title)
    {
        if (openSaveChooser == null)
            openSaveChooser = buildOpenSaveChooser();
        openSaveChooser.setDialogTitle(title);
        
        File suggestedFile = new File(ViskitGlobals.instance().getViskitProject().getAssembliesDirectory(), suggestedPath);
        if (!suggestedFile.getParentFile().isDirectory())
             suggestedFile.getParentFile().mkdirs();
        if (showUniqueName)
             suggestedFile = getUniqueName(suggestedPath);

        openSaveChooser.setSelectedFile(suggestedFile);
        int returnValue = openSaveChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) 
        {
            String message = "Confirm: overwrite " + openSaveChooser.getSelectedFile().getName() + "?";
            if (openSaveChooser.getSelectedFile().exists()) {
                if (JOptionPane.YES_OPTION != ViskitGlobals.instance().getMainFrame().genericAskYesNo("File Exists",  message))
                    return null;
            }
            try {
                LOG.info("Saved file as\n      {}", openSaveChooser.getSelectedFile().getCanonicalPath());
            }
            catch (IOException ioe)
            {
                // logic error, chooser indicated OK?
                LOG.error("Saved file suggested path chooser problem\n      {}", suggestedPath);
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
    public void deleteCanceledSave(File file) 
    {
        if ((file != null) && file.exists() && !file.isDirectory())
        {
            if (file.delete())
            {
                if (file.getParentFile().exists() && 
                   !file.getParentFile().equals(ViskitGlobals.instance().getViskitProject().getEventGraphsDirectory()))
                    deleteCanceledSave(file.getParentFile());
            }
        }
    }

    @Override
    public void showAndSaveSource(String className, String sourceText, String fileName) 
    {
        final JFrame newFrame = new SourceWindow(this, className, sourceText);
        newFrame.setTitle(fileName + " generated Java source");

        Runnable r = () -> {
            newFrame.setVisible(true);
        };
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void displayXML(File xmlFile)
    {
        if (!isFileReady(xmlFile))
        {
            LOG.error("displayXML(xmlFile) unable to continue");
            return;
        }
        JComponent xmlTreeComponent;
        try {
            xmlTreeComponent = XMLTreeComponent.getTreeInPanel(xmlFile);
        } 
        catch (Exception e) {
            LOG.error("displayXML(" + xmlFile.getAbsolutePath() + ") exception " + e.getMessage());
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, "XML Display Error", e.getMessage());
            return;
        }
        //xt.setVisibleRowCount(25);
        xmlTreeComponent.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final JFrame xmlDisplayFrame = new JFrame(xmlFile.getName());

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
    @SuppressWarnings("NonPublicExported")
    public RecentAssemblyFileListener getRecentAssemblyFileListener() {
        return recentAssemblyFileListener;
    }

    /**
     * @return the title
     */
    @Override
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
        if  (tabbedPane == null)
             return 0;
        else return tabbedPane.getTabCount();
    }
    /** has one or more Assemblies loaded
     * @return whether one or more Assemblies are loaded */
    public boolean hasAssembliesLoaded()
    {
        return (getNumberAssembliesLoaded() > 0);
    }

    /**
     * @return the assemblyMenu
     */
    public JMenu getAssemblyMenu()
    {
//        if  (!ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
//        {
//            // add to combined menu
//            assemblyMenu.add(editMenu); // TODO fix
//        }
        return assemblyMenu;
    }

    /**
     * @return the projectMenu
     */
    public JMenu getProjectMenu()
    {
        return projectMenu;
    }

    /**
     * @return the helpMenu
     */
    public JMenu getHelpMenu() {
        return helpMenu;
    }

} // end class file AssemblyViewFrame.java

/** Utility class to handle EventNode DnD operations */
interface DragStartListener {
    void startingDrag(Transferable trans);
}
