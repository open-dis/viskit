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

import edu.nps.util.LogUtils;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.control.AnalystReportController;
import viskit.control.AssemblyControllerImpl;
import viskit.control.AssemblyController;
import viskit.control.EventGraphController;
import viskit.control.InternalAssemblySimulationRunner;
import viskit.control.RecentProjectFileSetListener;
import viskit.doe.DoeMain;
import viskit.doe.DoeMainFrame;
import viskit.doe.JobLauncherTab2;
import viskit.model.Model;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.util.OpenAssembly;
import viskit.view.dialog.SettingsDialog;
import viskit.mvc.MvcModel;
import edu.nps.util.SystemExitHandler;
import viskit.assembly.AssemblySimulationRunPlug;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 22, 2005
 * @since 3:25:11 PM
 * @version $Id$
 */
public class MainFrame extends JFrame
{
    public final String VISKIT_APPLICATION_TITLE = "Viskit Discrete Event Simulation"; // using Simkit
    
    MvcAbstractViewFrame eventGraphFrame;
    MvcAbstractViewFrame assemblyFrame;
    MvcAbstractViewFrame reportPanel;
    InternalAssemblySimulationRunner internalAssemblySimulationRunner;
    JobLauncherTab2 runGridComponent;

    private Action myQuitAction;
    private JTabbedPane topTabbedAssemblyPane;
    private JTabbedPane simulationRunTabbedPane;
    private DoeMain doeMain;

    /** The initial assembly to load. */
    private final String initialAssemblyFile;
    private final int TAB0_EVENTGRAPH_EDITOR_INDEX = 0;
    private final int TAB0_ASSEMBLY_EDITOR_INDEX = 1;
    private final int TAB0_ASSEMBLYRUN_SUBTABS_INDEX = 2;
    private final int TAB0_ANALYST_REPORT_INDEX = 3;
    private final int[] tabIndices = {
        TAB0_EVENTGRAPH_EDITOR_INDEX,
        TAB0_ASSEMBLY_EDITOR_INDEX,
        TAB0_ASSEMBLYRUN_SUBTABS_INDEX,
        TAB0_ANALYST_REPORT_INDEX,
    };
    private final int TAB1_LOCALRUN_INDEX = 0;
    private final int TAB1_DESIGN_OF_EXPERIMENTS_INDEX = 1;
    private final int TAB1_CLUSTERUN_INDEX = 2;

    private AssemblyController     assemblyController;
    private EventGraphController eventGraphController;

    public MainFrame(String initialAssemblyFile) 
    {
        super(ViskitConfiguration.VISKIT_FULL_APPLICATION_NAME);
        
        this.initialAssemblyFile = initialAssemblyFile;
        ViskitGlobals.instance().setMainFrameWindow(MainFrame.this);

        initializeMainFrame();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Integer.parseInt(ViskitConfiguration.instance().getVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfiguration.instance().getVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]"));
        MainFrame.this.setLocation((d.width - w) / 2, (d.height - h) / 2);
        MainFrame.this.setSize(w, h);

        // Let the quit handler take care of an exit initiation
        MainFrame.this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        MainFrame.this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                myQuitAction.actionPerformed(null); // perform cleanups
            }
        });
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/ViskitSplash2.png"));
        MainFrame.this.setIconImage(icon.getImage());
    }

    /** @return the quit action class for Viskit */
    public Action getMyQuitAction() {
        return myQuitAction;
    }

    java.util.List<JMenuBar> menus = new ArrayList<>();

    private void initializeMainFrame() 
    {
        updateApplicationTitle();
        
//        ViskitGlobals.instance().setAssemblyQuitHandler(null);
//        ViskitGlobals.instance().setEventGraphQuitHandler(null); <- TODO: investigate why these are here
        JMenuBar menuBar;
        int assemblyPaneIndex;

        ChangeListener tabChangeListener = new myTabChangeListener();
        myQuitAction = new ExitAction("Exit");

        topTabbedAssemblyPane = new JTabbedPane();

        // Swing:
        getContentPane().add(topTabbedAssemblyPane);
        topTabbedAssemblyPane.setFont(topTabbedAssemblyPane.getFont().deriveFont(Font.BOLD));

        // Tabbed event graph editor
        eventGraphFrame = ViskitGlobals.instance().buildEventGraphViewFrame();
        if (SettingsDialog.isEventGraphEditorVisible()) {
            topTabbedAssemblyPane.add(((EventGraphViewFrame) eventGraphFrame).getContent());
            assemblyPaneIndex = topTabbedAssemblyPane.indexOfComponent(((EventGraphViewFrame) eventGraphFrame).getContent());
            topTabbedAssemblyPane.setTitleAt(assemblyPaneIndex, "Event Graph Editor");
            topTabbedAssemblyPane.setToolTipTextAt(assemblyPaneIndex, "Visual editor for simulation entity definitions");
            menuBar = ((EventGraphViewFrame) eventGraphFrame).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            // TODO no longer needed?
            // eventGraphFrame.setTitleListener(myTitleListener, assemblyPaneIndex);
            setJMenuBar(menuBar);
            jamQuitHandler(((EventGraphViewFrame) eventGraphFrame).getQuitMenuItem(), myQuitAction, menuBar);
            tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX] = assemblyPaneIndex;
        } else {
            tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX] = -1;
        }

        // Ensures Event Graph editor is the selected tab for menu function
        topTabbedAssemblyPane.addChangeListener(tabChangeListener);

        // Assembly editor
        assemblyFrame = ViskitGlobals.instance().buildAssemblyViewFrame();
        if (SettingsDialog.isAssemblyEditorVisible())
        {
            topTabbedAssemblyPane.add(((AssemblyViewFrame) assemblyFrame).getContent());
            assemblyPaneIndex = topTabbedAssemblyPane.indexOfComponent(((AssemblyViewFrame) assemblyFrame).getContent());
            topTabbedAssemblyPane.setTitleAt(assemblyPaneIndex, "Assembly Editor");
            topTabbedAssemblyPane.setToolTipTextAt(assemblyPaneIndex, "Visual editor for simulation defined by assembly");
            menuBar = ((AssemblyViewFrame) assemblyFrame).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            // TODO is this needed?
            // assemblyFrame.setTitleListener(myTitleListener, assemblyPaneIndex);
            setJMenuBar(menuBar);
            jamQuitHandler(((AssemblyViewFrame) assemblyFrame).getQuitMenuItem(), myQuitAction, menuBar);
            tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] = assemblyPaneIndex;
        } 
        else 
        {
            tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] = -1;
        }

        assemblyController = (AssemblyController) assemblyFrame.getController();
        assemblyController.setMainTabbedPane(topTabbedAssemblyPane, TAB0_ASSEMBLY_EDITOR_INDEX);
        eventGraphController = (EventGraphController) eventGraphFrame.getController();

        // Now set the recent open project's file listener for the eventGraphFrame now
        // that we have an assemblyFrame reference
        RecentProjectFileSetListener recentProjectFileSetListener = ((AssemblyViewFrame) assemblyFrame).getRecentProjectFileSetListener();
        recentProjectFileSetListener.addMenuItem(((EventGraphViewFrame) eventGraphFrame).getOpenRecentProjectMenu());

        // Now setup the assembly and event graph file change listener(s)
        assemblyController.addAssemblyFileListener(assemblyController.getAssemblyChangeListener());
        eventGraphController.addEventGraphFileListener(assemblyController.getOpenEventGraphListener()); // A live listener, but currently doing nothing (tdn) 9/13/24

        // Assembly Simulation Run
        simulationRunTabbedPane = new JTabbedPane();
        simulationRunTabbedPane.addChangeListener(tabChangeListener);
        JPanel runTabbedPanePanel = new JPanel(new BorderLayout());
        runTabbedPanePanel.setBackground(new Color(206, 206, 255)); // light blue
        runTabbedPanePanel.add(simulationRunTabbedPane, BorderLayout.CENTER);

        // Always selected as visible
        if (SettingsDialog.isAssemblySimulationRunVisible()) {
            topTabbedAssemblyPane.add(runTabbedPanePanel);
            assemblyPaneIndex = topTabbedAssemblyPane.indexOfComponent(runTabbedPanePanel);
            topTabbedAssemblyPane.setTitleAt(assemblyPaneIndex, "Assembly Simulation Run");
            topTabbedAssemblyPane.setToolTipTextAt(assemblyPaneIndex, "First initialize Assembly Simulation Run from Assembly tab");
            menus.add(null); // placeholder
            tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX] = assemblyPaneIndex;
//          tabbedPane.setEnabledAt(idx, false); // TODO do not disable?
        } 
        else 
        {
            tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX] = -1;
        }

        // Analyst report
        boolean analystReportPanelVisible = SettingsDialog.isAnalystReportVisible();
        if (analystReportPanelVisible)
        {
            reportPanel = ViskitGlobals.instance().buildAnalystReportFrame();
            topTabbedAssemblyPane.add(reportPanel.getContentPane());
            assemblyPaneIndex = topTabbedAssemblyPane.indexOfComponent(reportPanel.getContentPane());
            topTabbedAssemblyPane.setTitleAt(assemblyPaneIndex, "Analyst Report");
            topTabbedAssemblyPane.setToolTipTextAt(assemblyPaneIndex, "Supports analyst assessment and report generation");
            menuBar = ((AnalystReportViewFrame) reportPanel).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(menuBar);
            }
            // TODO is this needed?
            // reportPanel.setTitleListener(myTitleListener, assemblyPaneIndex);
            jamQuitHandler(null, myQuitAction, menuBar);
            tabIndices[TAB0_ANALYST_REPORT_INDEX] = assemblyPaneIndex;
            AnalystReportController analystReportController = (AnalystReportController) reportPanel.getController();
            analystReportController.setMainTabbedPane(topTabbedAssemblyPane, assemblyPaneIndex);
            assemblyController.addAssemblyFileListener((OpenAssembly.AssembyChangeListener) reportPanel);
        } 
        else
        {
            tabIndices[TAB0_ANALYST_REPORT_INDEX] = -1;
        }

        // Assembly Simulation Run
        internalAssemblySimulationRunner = new InternalAssemblySimulationRunner(analystReportPanelVisible);
        ViskitGlobals.instance().setInternalAssemblySimulationRunner(internalAssemblySimulationRunner);
        
        simulationRunTabbedPane.add(ViskitGlobals.instance().getAssemblySimulationRunPanel(), TAB1_LOCALRUN_INDEX);
        simulationRunTabbedPane.setTitleAt(TAB1_LOCALRUN_INDEX, "Local Run");
        simulationRunTabbedPane.setToolTipTextAt(TAB1_LOCALRUN_INDEX, "Run replications on local host");
        menuBar = internalAssemblySimulationRunner.getMenus();
        menus.add(menuBar);
        doCommonHelp(menuBar);
        jamSettingsHandler(menuBar);
        // TODO is this needed?
        // internalAssemblySimulationRunner.setTitleListener(myTitleListener, topTabbedAssemblyPane.getTabCount() + TAB1_LOCALRUN_INDEX);
        jamQuitHandler(internalAssemblySimulationRunner.getQuitMenuItem(), myQuitAction, menuBar);
        
// TODO unhide, unify?
        AssemblyControllerImpl assemblyControllerImpl = ((AssemblyControllerImpl) assemblyFrame.getController());
        assemblyControllerImpl.setInitialAssemblyFile(initialAssemblyFile);
        assemblyControllerImpl.setAssemblyRunner(new ThisAssemblyRunnerPlug());
        
        /* DIFF between OA3302 branch and trunk */

        // Design of experiments
        DoeMainFrame doeFrame = null;
        boolean isDOEVisible = SettingsDialog.isDesignOfExperimentsVisible();
        if (isDOEVisible) {
            doeMain = DoeMain.main2();
            doeFrame = doeMain.getMainFrame();
            simulationRunTabbedPane.add(doeFrame.getContent(), TAB1_DESIGN_OF_EXPERIMENTS_INDEX);
            simulationRunTabbedPane.setTitleAt(TAB1_DESIGN_OF_EXPERIMENTS_INDEX, "Design of Experiments");
            simulationRunTabbedPane.setIconAt(TAB1_DESIGN_OF_EXPERIMENTS_INDEX, new ImageIcon(getClass().getClassLoader().getResource("viskit/images/grid.png")));
            menuBar = doeMain.getMenus();
            if (menuBar == null) {
                menuBar = new JMenuBar();
                menuBar.add(new JMenu("File"));
            }
            menus.add(menuBar);
            doCommonHelp(menuBar);
            // TODO is this needed?
            // doeFrame.setTitleListener(myTitleListener, topTabbedAssemblyPane.getTabCount() + TAB1_DESIGN_OF_EXPERIMENTS_INDEX);
            jamQuitHandler(doeMain.getQuitMenuItem(), myQuitAction, menuBar);
            assemblyControllerImpl.addAssemblyFileListener(doeFrame.getController().getOpenAssemblyListener());
            eventGraphController.addEventGraphFileListener(doeFrame.getController().getOpenEventGraphListener());
        }

        // Grid run panel
        if (SettingsDialog.isCloudSimulationRunVisible() && isDOEVisible) {
            runGridComponent = new JobLauncherTab2(doeMain.getController(), null, null, this);
            if (doeFrame != null)
                doeFrame.getController().setJobLauncher(runGridComponent);
            simulationRunTabbedPane.add(runGridComponent.getContent(), TAB1_CLUSTERUN_INDEX);
            simulationRunTabbedPane.setTitleAt(TAB1_CLUSTERUN_INDEX, "LaunchClusterJob");
            simulationRunTabbedPane.setIconAt(TAB1_CLUSTERUN_INDEX, new ImageIcon(getClass().getClassLoader().getResource("viskit/images/grid.png")));
            menuBar = new JMenuBar();
            menuBar.add(new JMenu("File"));
            jamQuitHandler(null, myQuitAction, menuBar);
            menus.add(menuBar);
            doCommonHelp(menuBar);
            // TODO is this needed?
            // runGridComponent.setTitleListener(myTitleListener, topTabbedAssemblyPane.getTabCount() + TAB1_CLUSTERUN_INDEX);
            assemblyControllerImpl.addAssemblyFileListener(runGridComponent);
        }
        /* End DIFF between OA3302 branch and trunk */

        // let the event graph controller establish Viskit's classpath and open
        // EventGraphs first if an assembly file has not been submitted at startup
        if (initialAssemblyFile == null || initialAssemblyFile.isBlank() || initialAssemblyFile.contains("$")) {
            runLater(0L, () -> {
                eventGraphController.begin();
            });
        }

        runLater(500L, () -> {
            assemblyControllerImpl.begin();
        });
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

            Model[] mods = ViskitGlobals.instance().getEventGraphEditor().getOpenModels();
            Model dirtyMod = null;

            // Make sure we save modified Event Graphs if we wander off to the Assembly tab
            for (Model mod : mods) {
                if (mod.isDirty()) {
                    dirtyMod = mod;
                    ViskitGlobals.instance().getEventGraphController().setModel((MvcModel) mod);
                    ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).save();
                }
            }

            if (dirtyMod != null && dirtyMod.isDirty()) {

                // This will fire another call to stateChanged()
                topTabbedAssemblyPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX]);
                return;
            }

            int i = topTabbedAssemblyPane.getSelectedIndex();

            // If we compiled and prepped an Assembly to run, but want to go
            // back and change something, then handle that here
            if (i == tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX]) 
            {
                i = topTabbedAssemblyPane.getTabCount() + simulationRunTabbedPane.getSelectedIndex();
                topTabbedAssemblyPane.setToolTipTextAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX], "Run simulation defined by assembly");
            }
            else
            {
                topTabbedAssemblyPane.setToolTipTextAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX], "First initialize Assembly Simulation Run from Assembly tab");
            }

            getJMenuBar().remove(helpMenu);
            JMenuBar newMB = menus.get(i);
            newMB.add(helpMenu);
            setJMenuBar(newMB);
            // TODO is this needed?
            // myTitleListener.setTitle(titles[i], i);
        }
    }

    private JMenu helpMenu;

    /**
     * Stick the first Help menu we see into all the following ones.
     * @param mb
     */
    private void doCommonHelp(JMenuBar mb) {
        JMenu men;
        for (int i = 0; i < mb.getMenuCount(); i++) {
            men = mb.getMenu(i);
            if (men.getText().equalsIgnoreCase("Help")) {
                if (helpMenu == null) {
                    helpMenu = men;
                } else {
                    mb.remove(i);
                }
                return;
            }
        }
    }

    private void jamSettingsHandler(JMenuBar mb) {
        JMenu menu;
        Component c;
        JMenuItem menuItem;
        for (int i = 0; i < mb.getMenuCount(); i++) {
            menu = mb.getMenu(i);
            if (menu.getText().equalsIgnoreCase("File")) {
                for (int j = 0; j < menu.getMenuComponentCount(); j++) {
                    c = menu.getMenuComponent(j);
                    if (c instanceof JMenuItem) {
                        menuItem = (JMenuItem) c;
                        if (menuItem.getText().toLowerCase().contains("settings")) {
                            menuItem.addActionListener(mySettingsHandler);
                            return;
                        }
                    }
                }
            }
        }
    }

    ActionListener mySettingsHandler = (ActionEvent e) -> {
        SettingsDialog.showDialog(MainFrame.this);
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
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
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
        public void actionPerformed(ActionEvent e) 
        {
            SystemExitHandler defaultHandler = ViskitGlobals.instance().getSystemExitHandler();
            ViskitGlobals.instance().setSystemExitHandler(nullSystemExitHandler);

            // Tell Visit to not recompile open Event Graphs from any remaining open
            // Assemblies when we perform a Viskit exit
            ((AssemblyControllerImpl) ViskitGlobals.instance().getAssemblyController()).setCloseAll(true);

            outer: // Java block label
            // https://stackoverflow.com/questions/14147821/labeled-statement-block-in-java
            // https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.15
            // https://docs.oracle.com/javase/specs/jls/se6/html/statements.html
            {
                if (tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX] != -1) {
                    topTabbedAssemblyPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX]);
                    if (!((EventGraphController) eventGraphFrame.getController()).preQuit()) {
                        break outer;
                    }
                }
                if (tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] != -1) {
                    topTabbedAssemblyPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX]);
                    if (!((AssemblyController) assemblyFrame.getController()).preQuit()) {
                        break outer;
                    }
                }

                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX] != -1) {
                    topTabbedAssemblyPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX]);
                    if (doeMain != null) {
                        if (!doeMain.getController().preQuit()) {
                            break outer;
                        }
                    }
                }
                /* End DIFF between OA3302 branch and trunk */

                // TODO: other preQuits here if needed
                ViskitGlobals.instance().setSystemExitHandler(defaultHandler);    // reset default handler

                if (tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX] != -1) {
                    eventGraphController.removeEventGraphFileListener(assemblyController.getOpenEventGraphListener());
                    eventGraphController.removeRecentEventGraphFileListener(ViskitGlobals.instance().getEventGraphEditor().getRecentEventGraphFileListener());

                    // TODO: Need doe listener removal (tdn) 9/13/24

                    ((EventGraphController) eventGraphFrame.getController()).postQuit();
                }
                if (tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] != -1) {
                    assemblyController.removeAssemblyFileListener(assemblyController.getAssemblyChangeListener());
                    assemblyController.removeAssemblyFileListener((OpenAssembly.AssembyChangeListener) reportPanel);
                    assemblyController.removeRecentAssemblyFileSetListener(ViskitGlobals.instance().getAssemblyEditor().getRecentAssemblyFileListener());
                    assemblyController.removeRecentProjectFileSetListener(ViskitGlobals.instance().getAssemblyEditor().getRecentProjectFileSetListener());

                    // TODO: Need grid and doe listener removal (tdn) 9/13/24

                    ((AssemblyController) assemblyFrame.getController()).postQuit();
                }

                /* DIFF between OA3302 branch and trunk */
                if (doeMain != null) {
                    doeMain.getController().postQuit();
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
                ViskitConfiguration.instance().setVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]", "" + bounds.width);
                ViskitConfiguration.instance().setVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]", "" + bounds.height);

                // Pretty-fy all xml docs used for configuration
                ViskitConfiguration.instance().cleanup();

                // All other "Frames" were setVisible(false) above
                setVisible(false);

                // This will dispose all "Frames" and interrupt any non-daemon threads
                ViskitGlobals.instance().systemExit(0);  // quit application
            } //outer

            // Here if somebody cancelled.
            ViskitGlobals.instance().setSystemExitHandler(defaultHandler);
        }
    }

    final SystemExitHandler nullSystemExitHandler = (int status) -> {
        // do nothing
    };

    /** Prepares the Assembly with a fresh class loader free of static artifacts for
     * a completely independent run
     */
    class ThisAssemblyRunnerPlug implements AssemblySimulationRunPlug {

        @Override
        public void exec(String[] execStrings) {
            if (tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX] != -1) {

                topTabbedAssemblyPane.setEnabledAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX], true);

                // toggles a tab change listener
                topTabbedAssemblyPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX]);
                simulationRunTabbedPane.setSelectedIndex(TAB1_LOCALRUN_INDEX);

                // initializes a fresh class loader
                internalAssemblySimulationRunner.preInitRun(execStrings);
            }
        }

        @Override
        public void resetAssemblySimulationRunPanel()
        {
            AssemblySimulationRunPanel runnerPanel = ViskitGlobals.instance().getAssemblySimulationRunPanel();
            runnerPanel.outputStreamTA.setText(null);
            runnerPanel.outputStreamTA.setText(
                    "Assembly output stream:" + runnerPanel.lineEnd +
                    "-----------------------" + runnerPanel.lineEnd);
            runnerPanel.vcrStartTimeTF.setText("");
            runnerPanel.vcrStopTimeTF.setText("");
            runnerPanel.numberReplicationsTF.setText(Integer.toString(AssemblySimulationRunPanel.DEFAULT_NUMBER_OF_REPLICATIONS)); // initialized in XML and panel
            runnerPanel.vcrVerboseCB.setSelected(false);
            runnerPanel.printReplicationReportsCB.setSelected(false);
            runnerPanel.printSummaryReportsCB.setSelected(false);

            internalAssemblySimulationRunner.vcrButtonPressDisplayUpdate(InternalAssemblySimulationRunner.Event.OFF); // initialize
            internalAssemblySimulationRunner.doTitle(null);
            
            runnerPanel.nowRunningLabel.setText("");
        }
    }

    String[] titles = new String[]{"", "", "", "", "", "", "", "", "", ""};
    TitleListener myTitleListener = new MyTitleListener();

    class MyTitleListener implements TitleListener  // TODO are tab-specific titles needed?
    {
        @Override
        public void setTitle(String title, int key) {
            titles[key] = title;
            int assemblyTabIndex = topTabbedAssemblyPane.getSelectedIndex();
            if (assemblyTabIndex == tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_INDEX]) {
                assemblyTabIndex = topTabbedAssemblyPane.getTabCount() + simulationRunTabbedPane.getSelectedIndex();
                simulationRunTabbedPane.setTitleAt(TAB1_LOCALRUN_INDEX, title.substring(title.indexOf(":") + 1));
            }
            // TODO shift top-most title control to be separate
//            if (assemblyTabIndex == key) {
//                MainFrame.this.setTitle(title);
//            }
        }
    }
    
    /**
     * Application title, include project name in the frame title bar
     */
    public void updateApplicationTitle()
    {
        String newTitle = VISKIT_APPLICATION_TITLE;
        if      ( ViskitConfiguration.PROJECT_TITLE_NAME.toLowerCase().contains("project"))
             newTitle +=         ": " +    ViskitConfiguration.instance().getVal(ViskitConfiguration.PROJECT_TITLE_NAME);
        else if (!ViskitConfiguration.PROJECT_TITLE_NAME.isBlank())
             newTitle += " Project: " + ViskitConfiguration.instance().getVal(ViskitConfiguration.PROJECT_TITLE_NAME);
        // otherwise value is unchanged;
        setTitle(newTitle);
    }
}
