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
import edu.nps.util.LogUtilities;
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
    private final int TAB_INFO                  = 0;
    private final int TAB_EVENTGRAPH_EDITOR     = 1;
    private final int TAB_ASSEMBLY_EDITOR       = 2;
    private final int TAB_SIMULATION_RUN        = 3;
    private final int TAB_ANALYST_REPORT        = 4;
    private final int TAB_DESIGN_OF_EXPERIMENTS = 5;
    private final int TAB_GRID_CLUSTER_JOBS     = 6; // TODO naming
    private final int[] tabIndices = {
        TAB_INFO, 
        TAB_EVENTGRAPH_EDITOR, 
        TAB_ASSEMBLY_EDITOR,
        TAB_SIMULATION_RUN,
        TAB_ANALYST_REPORT,
//		TAB_DESIGN_OF_EXPERIMENTS
	};
    private final int SUBTAB_LOCALRUN_INDEX  = 0;
    private final int SUBTAB_DOE_INDEX       = 1;
    private final int SUBTAB_CLUSTERUN_INDEX = 2;
	
	private JMenuBar mainMenuBar;
    private final int menuShortcutKeyMask;
	
	private JMenu    fileMenu, projectsMenu, eventGraphFileMenu, eventGraphEditMenu, assemblyFileMenu, assemblyEditMenu, assemblyRunMenu, analystReportMenu, helpMenu;

	private RecentProjectFileSetListener recentProjectFileSetListener;
	
	private String title = new String();
	
	private JMenuItem closeProjectMI = new JMenuItem();
	
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
        ViskitGlobals.instance().setAssemblyQuitHandler(null);   // TODO
        ViskitGlobals.instance().setEventGraphQuitHandler(null); // TODO
        JMenuBar fileMenuBar, eventGraphMenuBar, assemblyEditMenuBar, assemblyRunMenuBar, analystReportMenuBar, 
				 designOfExperimentsMenuBar, clusterGridMenuBar; // TODO delete
        
        mainMenuBar = new JMenuBar();
        setJMenuBar(mainMenuBar);
		
        fileMenu = new JMenu("File     "); // extra wide in order to align menus with tabs
        fileMenu.setMnemonic(KeyEvent.VK_F);
		mainMenuBar.add(fileMenu); // initialize
		
        myExitAction = new ExitAction("Exit"); // TODO

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(mainTabbedPane.getFont().deriveFont(Font.BOLD));

		// =============================================================================================
        // Info frame
		
		JPanel infoPanel = new JPanel ();
		infoPanel.setEnabled(false);
        mainTabbedPane.add(infoPanel);
		
		int newTabIndex = mainTabbedPane.indexOfComponent(infoPanel);
		mainTabbedPane.setTitleAt(newTabIndex, "Info");
		mainTabbedPane.setToolTipTextAt(newTabIndex, "TODO");
		mainTabbedPane.setEnabledAt(newTabIndex, false); // TODO remove when tab becomes useful

		// =============================================================================================
        // Event graph editor
		
        eventGraphViewFrame = (EventGraphViewFrame) ViskitGlobals.instance().buildEventGraphViewFrame();
        if (UserPreferencesDialog.isEventGraphEditorVisible())
		{
            mainTabbedPane.add(eventGraphViewFrame.getContent());
            newTabIndex = mainTabbedPane.indexOfComponent(eventGraphViewFrame.getContent());
            mainTabbedPane.setTitleAt(newTabIndex, "Event Graph Editor");
            mainTabbedPane.setToolTipTextAt(newTabIndex, "Visual editor for object class definitions");
			

			      projectsMenu = eventGraphViewFrame.getProjectsMenu(); // TODO move into this class, cleanup
			eventGraphFileMenu = eventGraphViewFrame.getFileMenu();
			eventGraphEditMenu = eventGraphViewFrame.getEditMenu();
			eventGraphEditMenu.setEnabled(false); // activated when corresponding tabbed pane selected and event graph present
		   	   fileMenu.add(projectsMenu);        // submenu
               fileMenu.add(eventGraphFileMenu);  // submenu
            mainMenuBar.add(eventGraphEditMenu);  // top level
			
            tabIndices[TAB_EVENTGRAPH_EDITOR] = newTabIndex;
        } else {
            tabIndices[TAB_EVENTGRAPH_EDITOR] = -1;
        }

		// =============================================================================================
        // Assembly editor
		
        assemblyEditViewFrame = (AssemblyEditViewFrame) ViskitGlobals.instance().buildAssemblyViewFrame();
        if (UserPreferencesDialog.isAssemblyEditorVisible())
		{
            mainTabbedPane.add(assemblyEditViewFrame.getContent());
            newTabIndex = mainTabbedPane.indexOfComponent(assemblyEditViewFrame.getContent());
            mainTabbedPane.setTitleAt(newTabIndex, "Assembly Editor");
            mainTabbedPane.setToolTipTextAt(newTabIndex, "Visual editor for simulation program defined by Assembly");
			
			assemblyFileMenu = assemblyEditViewFrame.getFileMenu();
            fileMenu.add(assemblyFileMenu);    // submenu
			assemblyEditMenu = assemblyEditViewFrame.getEditMenu();
			assemblyEditMenu.setEnabled(false); // activated when corresponding tabbed pane selected and assembly present
            mainMenuBar.add(assemblyEditMenu); // top level
			
            tabIndices[TAB_ASSEMBLY_EDITOR] = newTabIndex;
        }
		else 
		{
            tabIndices[TAB_ASSEMBLY_EDITOR] = -1;
        }

		// =============================================================================================
        // Simulation Run
		
        runTabbedPane = new JTabbedPane();
        JPanel simulationRunTabbedPanePanel = new JPanel(new BorderLayout());
        simulationRunTabbedPanePanel.setBackground(new Color(206, 206, 255)); // light blue
        simulationRunTabbedPanePanel.add(runTabbedPane, BorderLayout.CENTER);

        // Always selected as visible
        if (UserPreferencesDialog.isSimulationRunVisible()) 
		{
            mainTabbedPane.add(simulationRunTabbedPanePanel);
            newTabIndex = mainTabbedPane.indexOfComponent(simulationRunTabbedPanePanel);
            mainTabbedPane.setTitleAt(newTabIndex, "Simulation Run");
            mainTabbedPane.setToolTipTextAt(newTabIndex, "First select Assembly Initialization button from Assembly tab");
            menus.add(null); // placeholder TODO?
            tabIndices[TAB_SIMULATION_RUN] = newTabIndex;
        } 
		else
		{
            tabIndices[TAB_SIMULATION_RUN] = -1;
        }

		// =============================================================================================
        // Simulation Run
		
        boolean analystReportPanelVisible = UserPreferencesDialog.isAnalystReportVisible();
        assemblyRunComponent = new InternalAssemblyRunner(analystReportPanelVisible);
		
        runTabbedPane.add(assemblyRunComponent.getRunnerPanel(), SUBTAB_LOCALRUN_INDEX);
        runTabbedPane.setTitleAt(SUBTAB_LOCALRUN_INDEX, "Assembly Initialization Needed");
        runTabbedPane.setToolTipTextAt(SUBTAB_LOCALRUN_INDEX, "Run simulation replications for current Assembly on local system");
		
		assemblyRunMenu = assemblyRunComponent.getSimulationRunMenu();
		assemblyRunMenu.setEnabled(true); // activated when corresponding tabbed pane selected
        fileMenu.add(assemblyRunMenu);
		
        assemblyRunComponent.setTitleListener(myTitleListener, mainTabbedPane.getTabCount() + SUBTAB_LOCALRUN_INDEX);
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
            newTabIndex = mainTabbedPane.indexOfComponent(analystReportFrame.getContentPane());
            mainTabbedPane.setTitleAt(newTabIndex, "Analyst Report");
            mainTabbedPane.setToolTipTextAt(newTabIndex, "Supports analyst assessment and report generation");
		
			analystReportMenu = ((AnalystReportFrame)analystReportFrame).getFileMenu();
            fileMenu.add(analystReportMenu);
			
            tabIndices[TAB_ANALYST_REPORT] = newTabIndex;
            AnalystReportController analystReportController = (AnalystReportController) analystReportFrame.getController();
            analystReportController.setMainTabbedPane(mainTabbedPane, newTabIndex);
            assemblyController.addAssemblyFileListener((AnalystReportFrame) analystReportFrame);
        } 
		else 
		{
            tabIndices[TAB_ANALYST_REPORT] = -1;
        }
		// =============================================================================================
        // File menu continued

        fileMenu.addSeparator();
		JMenuItem userPreferencesMenuItem = new JMenuItem ("User Preferences"); // buildMenuItem(eventGraphController, "settings", "User Preferences", null, null);
		userPreferencesMenuItem.setMnemonic(KeyEvent.VK_U);
        userPreferencesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK));
        fileMenu.add(userPreferencesMenuItem);
        userPreferencesMenuItem.addActionListener(myUserPreferencesHandler);
		
		closeProjectMI = eventGraphViewFrame.getCloseProjectMI();
		// duplicate entry, also on Projects submenu
//		fileMenu.add(closeProjectMI); 
		
		final String QUIT_METHOD = "quit"; // must match following method name.  Not possible to accomplish this programmatically.
		quitMenuItem = buildMenuItem(eventGraphController, QUIT_METHOD, "Exit", KeyEvent.VK_F4, KeyStroke.getKeyStroke(KeyEvent.VK_F4, menuShortcutKeyMask)); // do not change "quit"
        fileMenu.add(quitMenuItem); // TODO omit hotkey
        jamQuitHandler(getQuitMenuItem(), myExitAction, mainMenuBar); // TODO investigate, apparently necessary for exit

        // Now that we have an assemblyEditViewFrame reference, set the recent open project's file listener for the eventGraphFrame
        if (recentProjectFileSetListener == null) // remember rather than re-instantiate
		{
			recentProjectFileSetListener = assemblyEditViewFrame.getRecentProjectFileSetListener();
			recentProjectFileSetListener.addMenuItem(eventGraphViewFrame.getOpenRecentProjectsMenu());
		}
		
        // Now setup the assembly and event graph file change listener(s)
          assemblyController.addAssemblyFileListener  (assemblyController.getAssemblyChangeListener());
        eventGraphController.addEventGraphFileListener(assemblyController.getOpenEventGraphListener());

		// =============================================================================================
        // Design of experiments
        DoeMainFrame designOfExperimentsFrame = null;
        boolean isDOEVisible = UserPreferencesDialog.isDOEVisible();
        if (isDOEVisible)
		{
            designOfExperimentsMain = DoeMain.main2();
            designOfExperimentsFrame = designOfExperimentsMain.getMainFrame();
            runTabbedPane.add(designOfExperimentsFrame.getContent(), SUBTAB_DOE_INDEX);
            runTabbedPane.setTitleAt(SUBTAB_DOE_INDEX, "Design of Experiments");
            runTabbedPane.setIconAt(SUBTAB_DOE_INDEX, new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/grid.png")));
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
            designOfExperimentsFrame.setTitleListener(myTitleListener, mainTabbedPane.getTabCount() + SUBTAB_DOE_INDEX);
//            jamQuitHandler(designOfExperimentsMain.getQuitMenuItem(), myExitAction, mainMenuBar);
            assemblyController.addAssemblyFileListener(designOfExperimentsFrame.getController().getOpenAssemblyListener());
            eventGraphController.addEventGraphFileListener(designOfExperimentsFrame.getController().getOpenEventGraphListener());
        }

		// =============================================================================================
        // Grid run panel
        if (UserPreferencesDialog.isClusterRunVisible())
		{
            runGridComponent = new JobLauncherTab2(designOfExperimentsMain.getController(), null, null, this);
			if ((designOfExperimentsFrame != null) && (designOfExperimentsFrame.getController() != null))
			     designOfExperimentsFrame.getController().setJobLauncher(runGridComponent);
            runTabbedPane.add(runGridComponent.getContent(), SUBTAB_CLUSTERUN_INDEX);
            runTabbedPane.setTitleAt(SUBTAB_CLUSTERUN_INDEX, "LaunchClusterJob");
            runTabbedPane.setIconAt(SUBTAB_CLUSTERUN_INDEX, new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/grid.png")));
			
			// TODO clusterGridMenuBar
			
//            mainMenuBar = new JMenuBar();
//            mainMenuBar.add(new JMenu("File"));
//            jamQuitHandler(null, myExitAction, mainMenuBar);
//            menus.add(mainMenuBar);
//            doCommonHelp(mainMenuBar);
            runGridComponent.setTitleListener(myTitleListener, mainTabbedPane.getTabCount() + SUBTAB_CLUSTERUN_INDEX);
            assemblyController.addAssemblyFileListener(runGridComponent);
        }
		// =============================================================================================
//		doCommonHelp(mainMenuBar);
        mainMenuBar.add(eventGraphViewFrame.getHelpMenu()); // TODO move that method here
		
		mainTabbedPane.setSelectedIndex(TAB_EVENTGRAPH_EDITOR);

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

            EventGraphModel[] eventGraphModels = ViskitGlobals.instance().getEventGraphViewFrame().getOpenModels();
            EventGraphModel dirtyEventGraphModel = null;

            // Make sure we save modified EGs if we wander off to the Assembly tab
            for (EventGraphModel eventGraphModel : eventGraphModels)
			{
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
				boolean showingEventGraph = ViskitGlobals.instance().getEventGraphViewFrame().hasActiveEventGraph();
			
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
				boolean showingAssembly = ViskitGlobals.instance().getAssemblyEditViewFrame().hasActiveAssembly();
			
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
	
	public void refreshCloseProjectMI (boolean status)
	{
		closeProjectMI.setEnabled(status);
	}
	
	public void buildMenus ()
	{
		if (ViskitGlobals.instance().getCurrentViskitProject() != null)
			 refreshCloseProjectMI (ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen());
		else refreshCloseProjectMI (false);
		ViskitGlobals.instance().getEventGraphViewFrame().buildMenus(); // refresh to keep current
		ViskitGlobals.instance().getAssemblyEditViewFrame().buildMenus();
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

    private void jamQuitHandler(JMenuItem mi, Action quitAction, JMenuBar menuBar) {
        if (mi == null) {
            JMenu menu = menuBar.getMenu(0); // first menu
            if (menu == null)
			{
                menu = new JMenu("File");
                menuBar.add(menu);
            }
            menu.addSeparator();
            mi = new JMenuItem("Exit");
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
            menu.add(mi);
        }

        ActionListener[] actionListeners = mi.getActionListeners();
        for (ActionListener actionListener : actionListeners)
		{
            mi.removeActionListener(actionListener);
        }
        mi.setAction(quitAction);
    }

    class ExitAction extends AbstractAction {

        public ExitAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent e)
		{
            SystemExitHandler defaultHandler = ViskitGlobals.instance().getSystemExitHandler();
            ViskitGlobals.instance().setSystemExitHandler(nullSystemExitHandler);

            // Tell Viskit to not recompile open Event Graphs from any remaining open Assemblies when we perform a Viskit exit
            ViskitGlobals.instance().getAssemblyController().setCloseAll(true);

            outer:
            {
                if (tabIndices[TAB_EVENTGRAPH_EDITOR] != -1)
				{
                    mainTabbedPane.setSelectedIndex(tabIndices[TAB_EVENTGRAPH_EDITOR]);
					// check all open event graphs, ask user to save as appropriate
                    if (!((EventGraphController) eventGraphViewFrame.getController()).preQuit())
					{
                        break outer; // user cancel
                    }
                }
                if (tabIndices[TAB_ASSEMBLY_EDITOR] != -1)
				{
                    mainTabbedPane.setSelectedIndex(tabIndices[TAB_ASSEMBLY_EDITOR]);					
					// check all open assemblies, ask user to save as appropriate
                    if (!((AssemblyController) assemblyEditViewFrame.getController()).preQuit())
					{
                        break outer; // user cancel
                    }
                }

                if (tabIndices[TAB_SIMULATION_RUN] != -1) {
                    mainTabbedPane.setSelectedIndex(tabIndices[TAB_SIMULATION_RUN]);
                    if (designOfExperimentsMain != null) {
                        if (!designOfExperimentsMain.getController().preQuit())
						{
                            break outer; // user cancel
                        }
                    }
                }
                // TODO: other preQuits here if needed
				
                ViskitGlobals.instance().setSystemExitHandler(defaultHandler);    // reset default handler

                if (tabIndices[TAB_EVENTGRAPH_EDITOR] != -1)
				{
                    ((EventGraphController) eventGraphViewFrame.getController()).postQuit();
                }
                if (tabIndices[TAB_ASSEMBLY_EDITOR] != -1)
				{
                    ((AssemblyController) assemblyEditViewFrame.getController()).postQuit();
                }

                /* DIFF between OA3302 branch and trunk */
                if (designOfExperimentsMain != null)
				{
                    designOfExperimentsMain.getController().postQuit();
                }
                // TODO: other postQuits here if needed

                // Q: What is setting this true when it's false?
                // A: The Viskit Setting Dialog, third tab
                if (viskit.ViskitStatics.debug)
				{
                    LogUtilities.getLogger(ExitAction.class).info("in actionPerformed");
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
                runTabbedPane.setSelectedIndex(SUBTAB_LOCALRUN_INDEX);


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
	
	public boolean isInfoTabSelected ()
	{
		return (mainTabbedPane.getSelectedIndex() == TAB_INFO);
    }
	
	public void displayInfoTab ()
	{
		mainTabbedPane.setSelectedIndex(TAB_INFO);
		buildMenus();
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
        File dotViskit = ViskitConfiguration.VISKIT_CONFIGURATION_DIR;
        if (dotViskit.exists()) {

            // Can't delete .viskit dir unless it's empty
            File[] files = dotViskit.listFiles();
            for (File file : files) {
                file.delete();
            }
            if (dotViskit.delete())
                LogUtilities.getLogger(EventGraphAssemblyComboMain.class).info(dotViskit.getName() + " was found and deleted from your system.");

            LogUtilities.getLogger(EventGraphAssemblyComboMain.class).info("Please restart Viskit");
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
		String newTitle = getProjectTitle ();
	    setTitle(newTitle);
        if (this.titleListener != null) {
            titleListener.setTitle(newTitle, titleKey);
        }
    }
	public final String getProjectTitle ()
	{
        String title = " ";
		
		if ((ViskitGlobals.instance().getCurrentViskitProject() == null) ||
			 ViskitGlobals.instance().getCurrentViskitProject().getProjectName().trim().isEmpty() ||
			!ViskitGlobals.instance().getCurrentViskitProject().isProjectOpen())
		{
			title = ViskitConfiguration.VISKIT_FULL_APPLICATION_NAME; // no project is open
		}
		else // project is open
		{ 
			title = ViskitConfiguration.VISKIT_SHORT_APPLICATION_NAME;
			if (!ViskitGlobals.instance().getCurrentViskitProject().getProjectName().contains("Project"))
				 title += " Project";
			title += ": ";
			title += ViskitGlobals.instance().getCurrentViskitProject().getProjectName();
			// ViskitConfiguration.instance().getVal(ViskitConfiguration.PROJECT_TITLE_NAME);
		}
		return title;
	}
}
