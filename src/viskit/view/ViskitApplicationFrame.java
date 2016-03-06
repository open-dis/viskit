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

import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.LogUtils;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.nps.util.SystemExitHandler;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import viskit.EventGraphAssemblyComboMain;
import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.assembly.AssemblyRunnerPlug;
import viskit.control.AnalystReportController;
import viskit.control.AssemblyControllerImpl;
import viskit.control.AssemblyController;
import viskit.control.EventGraphController;
import viskit.control.InternalAssemblyRunner;
import viskit.control.RecentProjectFileSetListener;
import viskit.doe.DoeMain;
import viskit.doe.DoeMainFrame;
import viskit.doe.JobLauncherTab2;
import viskit.model.EventGraphModel;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModel;
import viskit.view.dialog.UserPreferencesDialog;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 22, 2005
 * @since 3:25:11 PM
 * @version $Id$
 */
public class ViskitApplicationFrame extends JFrame {

    private JTabbedPane    mainTabbedPane;
    private JTabbedPane runTabbedPane;
    EventGraphViewFrame eventGraphViewFrame;
    AssemblyEditViewFrame assemblyEditViewFrame;
    InternalAssemblyRunner assemblyRunComponent;
    JobLauncherTab2 runGridComponent;
    mvcAbstractJFrameView analystReportFrame;
    public Action myExitAction;
    private DoeMain designOfExperimentsMain;
    private JMenuItem quitMenuItem;
    protected TitleListener titleListener;
    protected int titleKey;
	
    private final String initialFile;
    private final int TAB_EVENTGRAPH_EDITOR     = 0;
    private final int TAB_ASSEMBLY_EDITOR       = 1;
    private final int TAB_SIMULATION_RUN        = 2;
    private final int TAB_ANALYST_REPORT        = 3;
    private final int TAB_DESIGN_OF_EXPERIMENTS = 4;
    private final int TAB_GRID_CLUSTER_JOBS     = 5; // TODO naming
    private final int[] tabIndices = {
        TAB_EVENTGRAPH_EDITOR, 
        TAB_ASSEMBLY_EDITOR,
        TAB_SIMULATION_RUN,
        TAB_ANALYST_REPORT,
//		TAB_DESIGN_OF_EXPERIMENTS
	};
    private final int TAB1_LOCALRUN_IDX = 0;
    private final int TAB1_DOE_IDX = 1;
    private final int TAB1_CLUSTERUN_IDX = 2;
	
	private JMenuBar mainMenuBar;
    private final int menuShortcutKeyMask;
	
	private JMenu    fileMenu, projectsMenu, eventGraphFileMenu, eventGraphEditMenu, assemblyFileMenu, assemblyEditMenu, assemblyRunMenu, analystReportMenu, helpMenu;

    public ViskitApplicationFrame(String initialFile)
	{
        super(ViskitConfiguration.VISKIT_FULL_APPLICATION_NAME); // default title

        this.initialFile = initialFile;
		
		menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        initializeUserInterface();

        int w = Integer.parseInt(ViskitConfiguration.instance().getValue(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfiguration.instance().getValue(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]"));

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((d.width - w) / 2, (d.height - h) / 2);
        this.setSize(w, h);

        // Let the quit handler take care of an exit initiation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                myExitAction.actionPerformed(null);
            }
        });
        ImageIcon icon = new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/ViskitSplash2.png"));
        this.setIconImage(icon.getImage());
		
		showProjectName();
    }

    /** @return the quit action class for Viskit */
    public Action getMyQuitAction() {
        return myExitAction;
    }

    java.util.List<JMenuBar> menus = new ArrayList<>();

    public final void initializeUserInterface()
	{
        ViskitGlobals.instance().setAssemblyQuitHandler(null);
        ViskitGlobals.instance().setEventGraphQuitHandler(null);
        JMenuBar fileMenuBar, eventGraphMenuBar, assemblyEditMenuBar, assemblyRunMenuBar, analystReportMenuBar, 
				 designOfExperimentsMenuBar, clusterGridMenuBar; // TODO delete
        
        mainMenuBar = new JMenuBar();
        setJMenuBar(mainMenuBar);
		
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
		mainMenuBar.add(fileMenu); // initialize
		
        myExitAction = new ExitAction("Exit");

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(mainTabbedPane.getFont().deriveFont(Font.BOLD));

		// =============================================================================================
        // Event graph editor
		
        eventGraphViewFrame = (EventGraphViewFrame) ViskitGlobals.instance().buildEventGraphViewFrame();
        if (UserPreferencesDialog.isEventGraphEditorVisible()) {
            mainTabbedPane.add(eventGraphViewFrame.getContent());
            int idx = mainTabbedPane.indexOfComponent(eventGraphViewFrame.getContent());
            mainTabbedPane.setTitleAt(idx, "Event Graph Editor");
            mainTabbedPane.setToolTipTextAt(idx, "Visual editor for object class definitions");

			      projectsMenu = eventGraphViewFrame.getProjectsMenu(); // TODO move into this class, cleanup
			eventGraphFileMenu = eventGraphViewFrame.getFileMenu();
			eventGraphEditMenu = eventGraphViewFrame.getEditMenu();
			eventGraphEditMenu.setEnabled(false); // activated when corresponding tabbed pane selected and event graph present
		   	   fileMenu.add(projectsMenu);        // submenu
               fileMenu.add(eventGraphFileMenu);  // submenu
            mainMenuBar.add(eventGraphEditMenu);  // top level
			
//            eventGraphMenuBar = eventGraphViewFrame.getMenus();
//            menus.add(eventGraphMenuBar);
//            doCommonHelp(mainMenuBar);
//            jamSettingsHandler(mainMenuBar); // TODO investigate, apparently necessary for exit
//            eventGraphViewFrame.setTitleListener(myTitleListener, idx);
//            setJMenuBar(mainMenuBar);
            tabIndices[TAB_EVENTGRAPH_EDITOR] = idx;
        } else {
            tabIndices[TAB_EVENTGRAPH_EDITOR] = -1;
        }

		// =============================================================================================
        // Assembly editor
        assemblyEditViewFrame = (AssemblyEditViewFrame) ViskitGlobals.instance().buildAssemblyViewFrame();
        if (UserPreferencesDialog.isAssemblyEditorVisible()) {
            mainTabbedPane.add(assemblyEditViewFrame.getContent());
            int idx = mainTabbedPane.indexOfComponent(assemblyEditViewFrame.getContent());
            mainTabbedPane.setTitleAt(idx, "Assembly Editor");
            mainTabbedPane.setToolTipTextAt(idx, "Visual editor for simulation program defined by Assembly");
			
			assemblyFileMenu = assemblyEditViewFrame.getFileMenu();
            fileMenu.add(assemblyFileMenu);    // submenu
			assemblyEditMenu = assemblyEditViewFrame.getEditMenu();
			assemblyEditMenu.setEnabled(false); // activated when corresponding tabbed pane selected and assembly present
            mainMenuBar.add(assemblyEditMenu); // top level
			
//            assemblyEditMenuBar = assemblyEditViewFrame.getMenus();
//            menus.add(assemblyEditMenuBar);
//            doCommonHelp(mainMenuBar);
//            jamSettingsHandler(mainMenuBar);
//            if (getJMenuBar() == null) {
//                setJMenuBar(mainMenuBar);
//            }
//            assemblyEditViewFrame.setTitleListener(myTitleListener, idx);
//            jamQuitHandler(assemblyViewFrame.getQuitMenuItem(), myExitAction, mainMenuBar);
            tabIndices[TAB_ASSEMBLY_EDITOR] = idx;
        } else {
            tabIndices[TAB_ASSEMBLY_EDITOR] = -1;
        }

		// =============================================================================================
        // Simulation Run
        runTabbedPane = new JTabbedPane();
        JPanel runTabbedPanePanel = new JPanel(new BorderLayout());
        runTabbedPanePanel.setBackground(new Color(206, 206, 255)); // light blue
        runTabbedPanePanel.add(runTabbedPane, BorderLayout.CENTER);

        // Always selected as visible
        if (UserPreferencesDialog.isSimulationRunVisible()) {
            mainTabbedPane.add(runTabbedPanePanel);
            int idx = mainTabbedPane.indexOfComponent(runTabbedPanePanel);
            mainTabbedPane.setTitleAt(idx, "Simulation Run");
            mainTabbedPane.setToolTipTextAt(idx, "First select Assembly Initialization button from Assembly tab");
            menus.add(null); // placeholder TODO?
            tabIndices[TAB_SIMULATION_RUN] = idx;
//          tabbedPane.setEnabledAt(idx, false); // TODO do not disable?
        } else {
            tabIndices[TAB_SIMULATION_RUN] = -1;
        }

		// =============================================================================================
        // Simulation Run
		
        boolean analystReportPanelVisible = UserPreferencesDialog.isAnalystReportVisible();
        assemblyRunComponent = new InternalAssemblyRunner(analystReportPanelVisible);
		
        runTabbedPane.add(assemblyRunComponent.getRunnerPanel(), TAB1_LOCALRUN_IDX);
        runTabbedPane.setTitleAt(TAB1_LOCALRUN_IDX, "Assembly Initialization Needed");
        runTabbedPane.setToolTipTextAt(TAB1_LOCALRUN_IDX, "Run simulation replications for current Assembly on local system");
		
		assemblyRunMenu = assemblyRunComponent.getSimulationRunMenu();
		assemblyRunMenu.setEnabled(true); // activated when corresponding tabbed pane selected
        fileMenu.add(assemblyRunMenu);
		
//        assemblyRunMenuBar = assemblyRunComponent.getMenus();
//        menus.add(assemblyRunMenuBar);
//        doCommonHelp(mainMenuBar);
//        jamSettingsHandler(mainMenuBar);
        assemblyRunComponent.setTitleListener(myTitleListener, mainTabbedPane.getTabCount() + TAB1_LOCALRUN_IDX);
//        jamQuitHandler(assemblyRunComponent.getQuitMenuItem(), myExitAction, mainMenuBar);
        AssemblyControllerImpl controller = ((AssemblyControllerImpl) assemblyEditViewFrame.getController());
        controller.setInitialFile(initialFile);
        controller.setAssemblyRunner(new ThisAssemblyRunnerPlug());

        final EventGraphController eventGraphController = (EventGraphController)   eventGraphViewFrame.getController();
        final   AssemblyController   assemblyController =   (AssemblyController) assemblyEditViewFrame.getController();

		// =============================================================================================
        // Analyst report
        if (analystReportPanelVisible)
		{
            analystReportFrame = ViskitGlobals.instance().buildAnalystReportFrame();
            mainTabbedPane.add(analystReportFrame.getContentPane());
            int idx = mainTabbedPane.indexOfComponent(analystReportFrame.getContentPane());
            mainTabbedPane.setTitleAt(idx, "Analyst Report");
            mainTabbedPane.setToolTipTextAt(idx, "Supports analyst assessment and report generation");
		
			analystReportMenu = ((AnalystReportFrame)analystReportFrame).getFileMenu();
            fileMenu.add(analystReportMenu);
			
//            analystReportMenuBar = ((AnalystReportFrame)analystReportFrame).getMenus();
//            menus.add(analystReportMenuBar);
//            doCommonHelp(mainMenuBar);
//            jamSettingsHandler(mainMenuBar);
//            if (getJMenuBar() == null) {
//                setJMenuBar(mainMenuBar);
//            }
//            ((AnalystReportFrame)analystReportFrame).setTitleListener(myTitleListener, idx);
//            jamQuitHandler(null, myExitAction, mainMenuBar);
            tabIndices[TAB_ANALYST_REPORT] = idx;
            AnalystReportController analystReportController = (AnalystReportController) analystReportFrame.getController();
            analystReportController.setMainTabbedPane(mainTabbedPane, idx);
            assemblyController.addAssemblyFileListener((AnalystReportFrame) analystReportFrame);
        } else {
            tabIndices[TAB_ANALYST_REPORT] = -1;
        }
		// =============================================================================================
        // File menu continued

        fileMenu.addSeparator();
		JMenuItem userPreferencesMenuItem = new JMenuItem ("User Preferences"); // buildMenuItem(eventGraphController, "settings", "User Preferences", null, null);
		userPreferencesMenuItem.setMnemonic(KeyEvent.VK_U);
        fileMenu.add(userPreferencesMenuItem);
        userPreferencesMenuItem.addActionListener(myUserPreferencesHandler);
//        jamSettingsHandler(fileMenu);
		
		quitMenuItem = buildMenuItem(eventGraphController, "quit", "Exit", KeyEvent.VK_F4, KeyStroke.getKeyStroke(KeyEvent.VK_F4, menuShortcutKeyMask)); // do not change "quit"
        fileMenu.add(quitMenuItem); // TODO omit hotkey
        jamQuitHandler(getQuitMenuItem(), myExitAction, mainMenuBar); // TODO investigate, apparently necessary for exit

        // Now that we have an assemblyFrame reference, set the recent open project's file listener for the eventGraphFrame
        RecentProjectFileSetListener listener = assemblyEditViewFrame.getRecentProjectFileSetListener();
        listener.addMenuItem(eventGraphViewFrame.getOpenRecentProjectsMenu());

		
        // Now setup the assembly and event graph file change listener(s)
          assemblyController.addAssemblyFileListener  (assemblyController.getAssemblyChangeListener());
        eventGraphController.addEventGraphFileListener(assemblyController.getOpenEventGraphListener());

		// =============================================================================================
        // Design of experiments
        DoeMainFrame designOfExperimentsFrame = null;
        boolean isDOEVisible = UserPreferencesDialog.isDOEVisible();
        if (isDOEVisible) {
            designOfExperimentsMain = DoeMain.main2();
            designOfExperimentsFrame = designOfExperimentsMain.getMainFrame();
            runTabbedPane.add(designOfExperimentsFrame.getContent(), TAB1_DOE_IDX);
            runTabbedPane.setTitleAt(TAB1_DOE_IDX, "Design of Experiments");
            runTabbedPane.setIconAt(TAB1_DOE_IDX, new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/grid.png")));
            designOfExperimentsMenuBar = designOfExperimentsMain.getMenus();
//            if (mainMenuBar == null) {
//                mainMenuBar = new JMenuBar();
//                mainMenuBar.add(new JMenu("File"));
//            }
			for (int i = 0; i < designOfExperimentsMenuBar.getMenuCount(); i++) // TODO upgrade
			{
				mainMenuBar.add(designOfExperimentsMenuBar.getMenu(i));                      
			}
            menus.add(designOfExperimentsMenuBar);
//            doCommonHelp(mainMenuBar);
            designOfExperimentsFrame.setTitleListener(myTitleListener, mainTabbedPane.getTabCount() + TAB1_DOE_IDX);
//            jamQuitHandler(designOfExperimentsMain.getQuitMenuItem(), myExitAction, mainMenuBar);
            assemblyController.addAssemblyFileListener(designOfExperimentsFrame.getController().getOpenAssemblyListener());
            eventGraphController.addEventGraphFileListener(designOfExperimentsFrame.getController().getOpenEventGraphListener());
        }

		// =============================================================================================
        // Grid run panel
        if (UserPreferencesDialog.isClusterRunVisible()) {
            runGridComponent = new JobLauncherTab2(designOfExperimentsMain.getController(), null, null, this);
			if ((designOfExperimentsFrame != null) && (designOfExperimentsFrame.getController() != null))
			     designOfExperimentsFrame.getController().setJobLauncher(runGridComponent);
            runTabbedPane.add(runGridComponent.getContent(), TAB1_CLUSTERUN_IDX);
            runTabbedPane.setTitleAt(TAB1_CLUSTERUN_IDX, "LaunchClusterJob");
            runTabbedPane.setIconAt(TAB1_CLUSTERUN_IDX, new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/grid.png")));
			
			// TODO clusterGridMenuBar
			
//            mainMenuBar = new JMenuBar();
//            mainMenuBar.add(new JMenu("File"));
//            jamQuitHandler(null, myExitAction, mainMenuBar);
//            menus.add(mainMenuBar);
//            doCommonHelp(mainMenuBar);
            runGridComponent.setTitleListener(myTitleListener, mainTabbedPane.getTabCount() + TAB1_CLUSTERUN_IDX);
            assemblyController.addAssemblyFileListener(runGridComponent);
        }
		// =============================================================================================
//		doCommonHelp(mainMenuBar);
        mainMenuBar.add(eventGraphViewFrame.getHelpMenu()); // TODO move that method here
		
		// do not showProjectName(); yet because hashmap isn't ready and project is not loaded 

        // let the event graph controller establish the Viskit classpath and open
        // EventGraphs first
        runLater(0L, new Runnable() {
            @Override
            public void run() {
                eventGraphController.begin();
            }
        });

        runLater(500L, new Runnable() {
            @Override
            public void run() {
                assemblyController.begin();
            }
        });

        // Swing:
        getContentPane().add(mainTabbedPane);

        ChangeListener tabChangeListener = new myTabChangeListener();
           mainTabbedPane.addChangeListener(tabChangeListener);
        runTabbedPane.addChangeListener(tabChangeListener);
    }

    private void runLater(final long ms, final Runnable runr) {

        java.util.Timer timer = new java.util.Timer("DelayedRunner", true);

        TimerTask delayedThreadStartTask = new TimerTask() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(runr);
            }
        };

        timer.schedule(delayedThreadStartTask, ms);
    }

    /** Utility class to handle tab selections on the main frame */
    class myTabChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {

            EventGraphModel[] eventGraphModels = ViskitGlobals.instance().getEventGraphEditor().getOpenModels();
            EventGraphModel dirtyEventGraphModel = null;

            // Make sure we save modified EGs if we wander off to the Assembly tab
            for (EventGraphModel eventGraphModel : eventGraphModels) {

                if (eventGraphModel.isDirty()) {
                    dirtyEventGraphModel = eventGraphModel;
                    ViskitGlobals.instance().getEventGraphController().setModel((mvcModel) eventGraphModel);
                    ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).save();
                }
            }

            if (dirtyEventGraphModel != null && dirtyEventGraphModel.isDirty())
			{
                // This will fire another call to stateChanged()
                mainTabbedPane.setSelectedIndex(tabIndices[TAB_EVENTGRAPH_EDITOR]);
				eventGraphEditMenu.setEnabled(true);
			     ((AnalystReportFrame)analystReportFrame).getFileMenu().setEnabled(false);
            }
            
			int i = mainTabbedPane.getSelectedIndex();
			buildMenus();
			
			if (i == tabIndices[TAB_EVENTGRAPH_EDITOR])
			{
				boolean showingEventGraph = ViskitGlobals.instance().getEventGraphEditor().hasActiveEventGraph();
			
//				eventGraphViewFrame = (EventGraphViewFrame) ViskitGlobals.instance().buildEventGraphViewFrame();
				eventGraphEditMenu.setEnabled(showingEventGraph);
			   	  assemblyEditMenu.setEnabled(false);
            }
            // If we compiled and prepped an Assembly to run, but want to go
            // back and change something, then handle that here
			else if (i == tabIndices[TAB_SIMULATION_RUN])
			{
                i = mainTabbedPane.getTabCount() + runTabbedPane.getSelectedIndex(); // TODO subtabs
                mainTabbedPane.setToolTipTextAt(TAB_SIMULATION_RUN, "Simulation Run is defined by active Assembly"); // tabIndices[TAB_SIMULATION_RUN]
				eventGraphEditMenu.setEnabled(false);
			   	  assemblyEditMenu.setEnabled(false);

                // Resets the Viskit ClassLoader
//                assyRunComponent.getAssemblyRunStopListener().actionPerformed(null);
            } 
			else if (i == tabIndices[TAB_ANALYST_REPORT])
			{
				eventGraphEditMenu.setEnabled(false);
			   	  assemblyEditMenu.setEnabled(false);
            }
			else // Assembly Edit
			{
				boolean showingAssembly = ViskitGlobals.instance().getAssemblyEditor().hasActiveAssembly();
			
//				assemblyEditViewFrame = (AssemblyEditViewFrame) ViskitGlobals.instance().buildAssemblyViewFrame();
                mainTabbedPane.setToolTipTextAt(tabIndices[TAB_SIMULATION_RUN], "First select Assembly Initialization button from Assembly tab"); // TODO fix
//                tabbedPane.setEnabledAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX], false);
				eventGraphEditMenu.setEnabled(false);
			   	  assemblyEditMenu.setEnabled(showingAssembly);
            }

//            getJMenuBar().remove(hmen);
//            JMenuBar newMB = menus.get(i);
//            newMB.add(hmen);
//            setJMenuBar(newMB);
			if (titles[i].isEmpty())
			{
				System.out.println ("No title for tab " + i); // TODO fix improper setup
			}
			else myTitleListener.setTitle(titles[i], i);
        }
    }
	public void buildMenus ()
	{
		ViskitGlobals.instance().getEventGraphEditor().buildMenus(); // refresh to keep current
		ViskitGlobals.instance().getAssemblyEditor().buildMenus();
	}

    /**
     * Stick the first Help menu we see into all the following ones.
     * @param menuBar
     */
	@Deprecated // TODO remove
    private void doCommonHelp(JMenuBar menuBar) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu.getText().equalsIgnoreCase("Help")) {
                if (helpMenu == null) {
                    helpMenu = menu;
                } else {
                    menuBar.remove(i);
                }
                return;
            }
        }
    }

//    private void jamSettingsHandler(JMenuBar menuBar) {
//        for (int i = 0; i < menuBar.getMenuCount(); i++) {
//            JMenu nextMenu = menuBar.getMenu(i);
//            if (nextMenu.getText().equalsIgnoreCase("Project")) {
//                for (int j = 0; j < nextMenu.getMenuComponentCount(); j++) {
//                    Component c = nextMenu.getMenuComponent(j);
//                    if (c instanceof JMenuItem) {
//                        JMenuItem menuItem = (JMenuItem) c;
//                        if (menuItem.getText().equalsIgnoreCase("Settings")) {
//                            menuItem.addActionListener(myUserPreferencesHandler);
//                            return;
//                        }
//                    }
//                }
//            }
//        }
//    }

    ActionListener myUserPreferencesHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            UserPreferencesDialog.showDialog(ViskitApplicationFrame.this);
        }
    };

    private void jamQuitHandler(JMenuItem mi, Action qa, JMenuBar mb) {
        if (mi == null) {
            JMenu m = mb.getMenu(0); // first menu
            if (m == null) {
                m = new JMenu("File");
                mb.add(m);
            }
            m.addSeparator();
            mi = new JMenuItem("Exit");
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
            m.add(mi);
        }

        ActionListener[] al = mi.getActionListeners();
        for (ActionListener al1 : al) {
            mi.removeActionListener(al1);
        }

        mi.setAction(qa);
    }

    class ExitAction extends AbstractAction {

        public ExitAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SystemExitHandler defaultHandler = ViskitGlobals.instance().getSystemExitHandler();
            ViskitGlobals.instance().setSystemExitHandler(nullSystemExitHandler);

            // Tell Visit to not recompile open Event Graphss from any remaining open Assemblies
			// when we perform a Viskit exit
            ViskitGlobals.instance().getAssemblyController().setCloseAll(true);

            outer:
            {
                if (tabIndices[TAB_EVENTGRAPH_EDITOR] != -1) {
                    mainTabbedPane.setSelectedIndex(tabIndices[TAB_EVENTGRAPH_EDITOR]);
                    if (!((EventGraphController) eventGraphViewFrame.getController()).preQuit()) {
                        break outer;
                    }
                }
                if (tabIndices[TAB_ASSEMBLY_EDITOR] != -1) {
                    mainTabbedPane.setSelectedIndex(tabIndices[TAB_ASSEMBLY_EDITOR]);
                    if (!((AssemblyController) assemblyEditViewFrame.getController()).preQuit()) {
                        break outer;
                    }
                }

                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB_SIMULATION_RUN] != -1) {
                    mainTabbedPane.setSelectedIndex(tabIndices[TAB_SIMULATION_RUN]);
                    if (designOfExperimentsMain != null) {
                        if (!designOfExperimentsMain.getController().preQuit()) {
                            break outer;
                        }
                    }
                }
                /* End DIFF between OA3302 branch and trunk */

                // TODO: other preQuits here if needed
                ViskitGlobals.instance().setSystemExitHandler(defaultHandler);    // reset default handler

                if (tabIndices[TAB_EVENTGRAPH_EDITOR] != -1) {
                    ((EventGraphController) eventGraphViewFrame.getController()).postQuit();
                }
                if (tabIndices[TAB_ASSEMBLY_EDITOR] != -1) {
                    ((AssemblyController) assemblyEditViewFrame.getController()).postQuit();
                }

                /* DIFF between OA3302 branch and trunk */
                if (designOfExperimentsMain != null) {
                    designOfExperimentsMain.getController().postQuit();
                }
                /* End DIFF between OA3302 branch and trunk */

                // TODO: other postQuits here if needed

                // Q: What is setting this true when it's false?
                // A: The Viskit Setting Dialog, third tab
                if (viskit.ViskitStatics.debug) {
                    LogUtils.getLogger(ExitAction.class).info("in actionPerformed");
                }

                // Remember the size of this main frame set by the user
                Rectangle bounds = getBounds();
                ViskitConfiguration.instance().setValue(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]", "" + bounds.width);
                ViskitConfiguration.instance().setValue(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]", "" + bounds.height);

                // Pretty-fy all xml docs used for configuration
                ViskitConfiguration.instance().cleanup();

                ViskitGlobals.instance().systemExit(0);  // quit application
            } //outer

            // Here if somebody cancelled.
            ViskitGlobals.instance().setSystemExitHandler(defaultHandler);
        }
    }

    private final SystemExitHandler nullSystemExitHandler = new SystemExitHandler() {

        @Override
        public void doSystemExit(int status) {
            // do nothing
        }
    };

    /** Prepares the Assembly with a fresh class loader free of static artifacts for
     * a completely independent run
     */
    class ThisAssemblyRunnerPlug implements AssemblyRunnerPlug {

        @Override
        public void exec(String[] execStrings) {
            if (tabIndices[TAB_SIMULATION_RUN] != -1) {

                mainTabbedPane.setEnabledAt(tabIndices[TAB_SIMULATION_RUN], true);

                // toggles a tab change listener
                mainTabbedPane.setSelectedIndex(tabIndices[TAB_SIMULATION_RUN]);
                runTabbedPane.setSelectedIndex(TAB1_LOCALRUN_IDX);


                // initializes a fresh class loader
                assemblyRunComponent.preInitializeRun(execStrings);
            }
        }
    }

    String[] titles = new String[]{"", "", "", "", "", "", "", "", "", ""};
    TitleListener myTitleListener = new myTitleListener();

    class myTitleListener implements TitleListener {

        @Override
        public void setTitle(String title, int key) {
            titles[key] = title;
            int tabIndex = mainTabbedPane.getSelectedIndex();
            if (tabIndex == tabIndices[TAB_SIMULATION_RUN]) {
                tabIndex = mainTabbedPane.getTabCount() + runTabbedPane.getSelectedIndex();
            }
			
			if ((title != null) && title.isEmpty())
			{
				System.out.println ("Blank title set of tab " + key);
			}
			else if (tabIndex == key) {
                ViskitApplicationFrame.this.setTitle(title);
            }
        }
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
		JMenuItem menuItem = ActionUtilities.createMenuItem(a);
		// TODO get accelerator key to show :(
		// menuItem.setAccelerator(KeyStroke.getKeyStroke(mn, accel));
		

        return menuItem;
    }

    public JMenuItem getQuitMenuItem() {
        return quitMenuItem;
    }
	
	public boolean isEventGraphEditorTabSelected ()
	{
		return (mainTabbedPane.getSelectedIndex() == TAB_EVENTGRAPH_EDITOR);
    }
	
	public boolean isAssemblyEditorTabSelected ()
	{
		return (mainTabbedPane.getSelectedIndex() == TAB_ASSEMBLY_EDITOR);
    }
	
	public boolean isSimulationRunTabSelected ()
	{
		return (mainTabbedPane.getSelectedIndex() == TAB_SIMULATION_RUN);
    }
	
	public boolean isAnalystReportTabSelected ()
	{
		return (mainTabbedPane.getSelectedIndex() == TAB_ANALYST_REPORT);
    }
	
	public void displayEventGraphEditorTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_EVENTGRAPH_EDITOR);
		buildMenus();
	}
	public void displaySimulationRunTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_SIMULATION_RUN);
		buildMenus();
		
		String  assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getMetadata().name;
		int selectedRunIndex = getRunTabbedPane().getSelectedIndex();
		ViskitGlobals.instance().getSimulationRunPanel().setTitle(assemblyName);
		getRunTabbedPane().setTitleAt(selectedRunIndex, assemblyName);
	}
	public void displayAssemblyEditorTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_ASSEMBLY_EDITOR);
		buildMenus();
	}
	public void displayAnalystReportTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_ANALYST_REPORT);
		buildMenus();
	}
	public void displayDesignOfExperimentsTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_DESIGN_OF_EXPERIMENTS);
		buildMenus();
	}
	public void displayGridClusterJobTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_GRID_CLUSTER_JOBS);
		buildMenus();
	}
	public JTabbedPane getRunTabbedPane ()
	{
		return runTabbedPane;
	}
	public JPanel getSelectedSimulationRunTab ()
	{
		return (JPanel) runTabbedPane.getSelectedComponent();
	}
	public int getSelectedSimulationRunIndex ()
	{
		return runTabbedPane.getSelectedIndex();
	}

    /** Draconian process for restoring from a possibly corrupt, or out if synch
     * .viskit config directory in the user's profile space.
	 * TODO expose this functionality on User Preferences
     */
    public static void nukeDotViskit() {
        File dotViskit = ViskitConfiguration.VISKIT_CONFIG_DIR;
        if (dotViskit.exists()) {

            // Can't delete .viskit dir unless it's empty
            File[] files = dotViskit.listFiles();
            for (File file : files) {
                file.delete();
            }
            if (dotViskit.delete())
                LogUtils.getLogger(EventGraphAssemblyComboMain.class).info(dotViskit.getName() + " was found and deleted from your system.");

            LogUtils.getLogger(EventGraphAssemblyComboMain.class).info("Please restart Viskit");
        }
    }

    /** Sets the frame title listener and key for this frame
     *
     * @param listener the title listener to set
     * @param key the key for this frame's title
     */
    public void setTitleListener(TitleListener listener, int key) {
        titleListener = listener;
        titleKey      = key;

        showProjectName();
    }

    /**
     * Shows the project name in the frame title bar
     */
    public final void showProjectName()
	{
        String title = " " + ViskitConfiguration.VISKIT_SHORT_APPLICATION_NAME + ": ";
		if ((ViskitGlobals.instance().getCurrentViskitProject() == null) ||
			 ViskitGlobals.instance().getCurrentViskitProject().getProjectName().isEmpty())
			 title += ViskitConfiguration.VISKIT_FULL_APPLICATION_NAME;
		else 
		{
			title += ViskitGlobals.instance().getCurrentViskitProject().getProjectName();
			// ViskitConfiguration.instance().getVal(ViskitConfiguration.PROJECT_TITLE_NAME);
			if (!title.contains("Project"))
				 title += " Project";
		}
	    setTitle(title);
        if (this.titleListener != null) {
            titleListener.setTitle(title, titleKey);
        }
    }
}
