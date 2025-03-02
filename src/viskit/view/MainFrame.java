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

import edu.nps.util.Log4jUtilities;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitConfigurationStore;
import viskit.control.AssemblyControllerImpl;
import viskit.control.EventGraphController;
import viskit.control.TextAreaOutputStream;
import viskit.doe.DoeMain;
import viskit.doe.JobLauncherTab2;
import viskit.model.Model;
import viskit.util.OpenAssembly;
import viskit.mvc.MvcModel;
import edu.nps.util.SystemExitHandler;
import java.util.TimerTask;
import org.apache.logging.log4j.Logger;
import viskit.assembly.SimulationRunInterface;
import viskit.control.AnalystReportController;
import viskit.control.EventGraphControllerImpl;
import viskit.control.RecentProjectFileSetListener;
import viskit.doe.DoeMainFrame;
import static viskit.view.MainFrame.TAB0_ASSEMBLY_EDITOR_INDEX;
import static viskit.view.MainFrame.TAB0_EVENTGRAPH_EDITOR_INDEX;
import static viskit.view.MainFrame.TAB1_LOCALRUN_INDEX;
import static viskit.view.MainFrame.tabIndices;
import viskit.view.dialog.ViskitUserPreferences;
import static viskit.view.MainFrame.TAB0_SIMULATIONRUN_INDEX;

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

    /**
     * @return the eventGraphController
     */
    public EventGraphControllerImpl getEventGraphController() {
        return eventGraphController;
    }
    public final String VISKIT_APPLICATION_TITLE = ViskitConfigurationStore.VISKIT_FULL_APPLICATION_NAME;

    /** modalMenus: true means choice from traditional modalMenuBarList, false means new combinedMenuBar */
    private boolean modalMenus = false; 
    JMenuBar mainFrameMenuBar;
    JMenuBar combinedMenuBar = new JMenuBar();
    java.util.List<JMenuBar> modalMenuBarList = new ArrayList<>();
    
    EventGraphViewFrame  eventGraphFrame;
    AssemblyViewFrame    assemblyFrame;
    AnalystReportViewFrame analystReportPanel;
    TextAreaOutputStream internalSimulationRunner;
    JobLauncherTab2 runGridComponent;

    private Action myQuitAction;
    private JTabbedPane topTabbedPane;
    private JTabbedPane simulationRunTabbedPane;
    private DoeMain doeMain;

    /** The initial assembly to load. */
    private       final String initialAssemblyFile;
    public static final int TAB0_EVENTGRAPH_EDITOR_INDEX = 0;
    public static final int TAB0_ASSEMBLY_EDITOR_INDEX   = 1;
    public static final int TAB0_SIMULATIONRUN_INDEX     = 2;
    public static final int TAB0_ANALYSTREPORT_INDEX    = 3;
    public static final int[] tabIndices = {
        TAB0_EVENTGRAPH_EDITOR_INDEX,
        TAB0_ASSEMBLY_EDITOR_INDEX,
        TAB0_SIMULATIONRUN_INDEX,
        TAB0_ANALYSTREPORT_INDEX,
    };
    public static final int TAB1_LOCALRUN_INDEX = 0;
    public static final int TAB1_DESIGN_OF_EXPERIMENTS_INDEX = 1;
    public static final int TAB1_CLUSTERUN_INDEX = 2;

    private AssemblyControllerImpl     assemblyController;
    private EventGraphControllerImpl eventGraphController;
    
    static final Logger LOG = Log4jUtilities.getLogger(MainFrame.class);

    public MainFrame(String initialAssemblyFile) 
    {
        super(ViskitConfigurationStore.VISKIT_FULL_APPLICATION_NAME);
        
        ViskitGlobals.instance().setMainFrame(MainFrame.this);
        
        this.initialAssemblyFile = initialAssemblyFile;

        initializeMainFrame();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Integer.parseInt(ViskitConfigurationStore.instance().getValue(ViskitConfigurationStore.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfigurationStore.instance().getValue(ViskitConfigurationStore.APP_MAIN_BOUNDS_KEY + "[@h]"));
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

    private void initializeMainFrame() 
    {
//        ViskitGlobals.instance().setAssemblyQuitActionListener(null);
//        ViskitGlobals.instance().setEventGraphQuitHandler(null); <- TODO: investigate why these are here
        int assemblyPaneIndex;

        ChangeListener tabChangeListener = new myTabChangeListener();
        myQuitAction = new QuitAction("Quit");

        topTabbedPane = new JTabbedPane();

        // Swing:
        getContentPane().add(topTabbedPane);
        topTabbedPane.setFont(topTabbedPane.getFont().deriveFont(Font.BOLD));

        // Tabbed event graph editor
        eventGraphFrame = ViskitGlobals.instance().buildEventGraphViewFrame();
        if (ViskitUserPreferences.isEventGraphEditorVisible()) 
        {
            topTabbedPane.add(((EventGraphViewFrame) eventGraphFrame).getContent());
            assemblyPaneIndex = topTabbedPane.indexOfComponent(((EventGraphViewFrame) eventGraphFrame).getContent());
            topTabbedPane.setTitleAt(assemblyPaneIndex, "Event Graph Editor");
            topTabbedPane.setToolTipTextAt(assemblyPaneIndex, "Visual editor for Event Graph definitions");
            mainFrameMenuBar = ((EventGraphViewFrame) eventGraphFrame).getMenus();
            modalMenuBarList.add(mainFrameMenuBar);
            doCommonHelp(mainFrameMenuBar);
            jamSettingsHandler(mainFrameMenuBar);
            // TODO no longer needed?
            // eventGraphFrame.setTitleListener(myTitleListener, assemblyPaneIndex);
            setJMenuBar(mainFrameMenuBar);
            
        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
            jamQuitHandler(((EventGraphViewFrame) eventGraphFrame).getQuitMenuItem(), myQuitAction, mainFrameMenuBar);
        }
            tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX] = assemblyPaneIndex;
        } 
        else {
            tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX] = -1;
        }

        // Ensures Event Graph editor is the selected tab for menu function
        topTabbedPane.addChangeListener(tabChangeListener);

        // Assembly editor
        assemblyFrame = ViskitGlobals.instance().buildAssemblyViewFrame();
        if (ViskitUserPreferences.isAssemblyEditorVisible())
        {
            topTabbedPane.add(((AssemblyViewFrame) assemblyFrame).getContent());
            assemblyPaneIndex = topTabbedPane.indexOfComponent(((AssemblyViewFrame) assemblyFrame).getContent());
            topTabbedPane.setTitleAt(assemblyPaneIndex, "Assembly Editor");
            topTabbedPane.setToolTipTextAt(assemblyPaneIndex, "Visual editor for assembly definitions");
            mainFrameMenuBar = ((AssemblyViewFrame) assemblyFrame).getMenus();
            modalMenuBarList.add(mainFrameMenuBar);
            doCommonHelp(mainFrameMenuBar);
            jamSettingsHandler(mainFrameMenuBar);
            // TODO is this needed?
            // assemblyFrame.setTitleListener(myTitleListener, assemblyPaneIndex);
            setJMenuBar(mainFrameMenuBar);
        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
            jamQuitHandler(((AssemblyViewFrame) assemblyFrame).getQuitMenuItem(), myQuitAction, mainFrameMenuBar);
        }
            tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] = assemblyPaneIndex;
        } 
        else 
        {
            tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] = -1;
        }

        assemblyController = ViskitGlobals.instance().getAssemblyController();
        assemblyController.setMainTabbedPane(topTabbedPane, TAB0_ASSEMBLY_EDITOR_INDEX);
        eventGraphController = ViskitGlobals.instance().getEventGraphController();

        // Now set the recent open project's file listener for the eventGraphFrame now
        // that we have an assemblyFrame reference
        RecentProjectFileSetListener recentProjectFileSetListener = ((AssemblyViewFrame) assemblyFrame).getRecentProjectFileSetListener();
        recentProjectFileSetListener.addMenuItem(((EventGraphViewFrame) eventGraphFrame).getOpenRecentProjectMenu());

        // Now setup the assembly and event graph file change listener(s)
        getAssemblyController().addAssemblyFileListener(getAssemblyController().getAssemblyChangeListener());
        getEventGraphController().addEventGraphFileListener(getAssemblyController().getOpenEventGraphListener()); // A live listener, but currently doing nothing (tdn) 9/13/24

        // Assembly Simulation Run
        simulationRunTabbedPane = new JTabbedPane();
        getSimulationRunTabbedPane().addChangeListener(tabChangeListener);
        JPanel runTabbedPanePanel = new JPanel(new BorderLayout());
        runTabbedPanePanel.setBackground(new Color(206, 206, 255)); // light blue
        runTabbedPanePanel.add(getSimulationRunTabbedPane(), BorderLayout.CENTER);

        // Always selected as visible
        if (ViskitUserPreferences.isAssemblySimulationRunVisible()) {
            topTabbedPane.add(runTabbedPanePanel);
            assemblyPaneIndex = topTabbedPane.indexOfComponent(runTabbedPanePanel);
            topTabbedPane.setTitleAt(assemblyPaneIndex, "Simulation Run");
            topTabbedPane.setToolTipTextAt(assemblyPaneIndex, "First initialize Assembly for Simulation Run from Assembly tab");
            modalMenuBarList.add(null); // placeholder
            tabIndices[TAB0_SIMULATIONRUN_INDEX] = assemblyPaneIndex;
//          tabbedPane.setEnabledAt(idx, false); // TODO do not disable?
        } 
        else 
        {
            tabIndices[TAB0_SIMULATIONRUN_INDEX] = -1;
        }

        // Analyst report
        boolean analystReportPanelVisible = ViskitUserPreferences.isAnalystReportVisible();
        if (analystReportPanelVisible)
        {
            analystReportPanel = (AnalystReportViewFrame)ViskitGlobals.instance().buildAnalystReportFrame();
            topTabbedPane.add(analystReportPanel.getContentPane());
            assemblyPaneIndex = topTabbedPane.indexOfComponent(analystReportPanel.getContentPane());
            topTabbedPane.setTitleAt(assemblyPaneIndex, "Analyst Report");
            topTabbedPane.setToolTipTextAt(assemblyPaneIndex, "Editor for analyst assessment and report generation");
            mainFrameMenuBar = ((AnalystReportViewFrame) analystReportPanel).getMenus();
            modalMenuBarList.add(mainFrameMenuBar);
            doCommonHelp(mainFrameMenuBar);
            jamSettingsHandler(mainFrameMenuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(mainFrameMenuBar);
            }
            // TODO is this needed?
            // analystReportPanel.setTitleListener(myTitleListener, assemblyPaneIndex);
        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
            jamQuitHandler(null, myQuitAction, mainFrameMenuBar);
        }
            tabIndices[TAB0_ANALYSTREPORT_INDEX] = assemblyPaneIndex;
            AnalystReportController analystReportController = (AnalystReportController) analystReportPanel.getController();
            analystReportController.setMainTabbedPane(topTabbedPane, assemblyPaneIndex);
            getAssemblyController().addAssemblyFileListener((OpenAssembly.AssembyChangeListener) analystReportPanel);
        } 
        else
        {
            tabIndices[TAB0_ANALYSTREPORT_INDEX] = -1;
        }

        // Assembly Simulation Run
        internalSimulationRunner = new TextAreaOutputStream(analystReportPanelVisible);
        ViskitGlobals.instance().setInternalSimulationRunner(internalSimulationRunner);
        
        getSimulationRunTabbedPane().add(ViskitGlobals.instance().getSimulationRunPanel(), TAB1_LOCALRUN_INDEX);
        getSimulationRunTabbedPane().setTitleAt(TAB1_LOCALRUN_INDEX, SimulationRunPanel.INITIAL_SIMULATIONRUN_HINT);
        getSimulationRunTabbedPane().setToolTipTextAt(TAB1_LOCALRUN_INDEX, "Run replications of assembly simulation on local host");
        mainFrameMenuBar = internalSimulationRunner.getMenus();
        modalMenuBarList.add(mainFrameMenuBar);
        doCommonHelp(mainFrameMenuBar);
        jamSettingsHandler(mainFrameMenuBar);
        
        // TODO is this needed?
        // internalSimulationRunner.setTitleListener(myTitleListener, topTabbedAssemblyPane.getTabCount() + TAB1_LOCALRUN_INDEX);
        
        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
        jamQuitHandler(internalSimulationRunner.getQuitMenuItem(), myQuitAction, mainFrameMenuBar);
        }
        
// TODO unhide, unify?
        AssemblyControllerImpl assemblyControllerImpl = ((AssemblyControllerImpl) assemblyFrame.getController());
        assemblyControllerImpl.setInitialAssemblyFile(initialAssemblyFile);
        assemblyControllerImpl.setAssemblyRunner(new SimulationRunPlug());
        
        /* DIFF between OA3302 branch and trunk */

        // Design of experiments
        DoeMainFrame doeFrame = null;
        boolean isDOEVisible = ViskitUserPreferences.isDesignOfExperimentsVisible();
        if (isDOEVisible) {
            doeMain = DoeMain.main2();
            doeFrame = doeMain.getMainFrame();
            getSimulationRunTabbedPane().add(doeFrame.getContent(), TAB1_DESIGN_OF_EXPERIMENTS_INDEX);
            getSimulationRunTabbedPane().setTitleAt(TAB1_DESIGN_OF_EXPERIMENTS_INDEX, "Design of Experiments");
            getSimulationRunTabbedPane().setIconAt(TAB1_DESIGN_OF_EXPERIMENTS_INDEX, new ImageIcon(getClass().getClassLoader().getResource("viskit/images/grid.png")));
            mainFrameMenuBar = doeMain.getMenus();
            if (mainFrameMenuBar == null) {
                mainFrameMenuBar = new JMenuBar();
                mainFrameMenuBar.add(new JMenu("File"));
            }
            modalMenuBarList.add(mainFrameMenuBar);
            doCommonHelp(mainFrameMenuBar);
            // TODO is this needed?
            // doeFrame.setTitleListener(myTitleListener, topTabbedAssemblyPane.getTabCount() + TAB1_DESIGN_OF_EXPERIMENTS_INDEX);
            jamQuitHandler(doeMain.getQuitMenuItem(), myQuitAction, mainFrameMenuBar);
            assemblyControllerImpl.addAssemblyFileListener(doeFrame.getController().getOpenAssemblyListener());
            getEventGraphController().addEventGraphFileListener(doeFrame.getController().getOpenEventGraphListener());
        }

        // Grid run panel
        if (ViskitUserPreferences.isCloudSimulationRunVisible() && isDOEVisible) {
            runGridComponent = new JobLauncherTab2(doeMain.getController(), null, null, this);
            if (doeFrame != null)
                doeFrame.getController().setJobLauncher(runGridComponent);
            getSimulationRunTabbedPane().add(runGridComponent.getContent(), TAB1_CLUSTERUN_INDEX);
            getSimulationRunTabbedPane().setTitleAt(TAB1_CLUSTERUN_INDEX, "LaunchClusterJob");
            getSimulationRunTabbedPane().setIconAt(TAB1_CLUSTERUN_INDEX, new ImageIcon(getClass().getClassLoader().getResource("viskit/images/grid.png")));
            mainFrameMenuBar = new JMenuBar();
            mainFrameMenuBar.add(new JMenu("File"));
            jamQuitHandler(null, myQuitAction, mainFrameMenuBar);
            modalMenuBarList.add(mainFrameMenuBar);
            doCommonHelp(mainFrameMenuBar);
            // TODO is this needed?
            // runGridComponent.setTitleListener(myTitleListener, topTabbedAssemblyPane.getTabCount() + TAB1_CLUSTERUN_INDEX);
            assemblyControllerImpl.addAssemblyFileListener(runGridComponent);
        }

        // let the event graph controller establish Viskit's classpath and open
        // EventGraphs first if an assembly file has not been submitted at startup
        if (initialAssemblyFile == null || initialAssemblyFile.isBlank() || initialAssemblyFile.contains("$")) {
            runLater(100L, () -> {
                getEventGraphController().begin();
            });
        }

        runLater(500L, () -> {
            assemblyControllerImpl.begin();
        });
    }

    public static void runLater(final long ms, final Runnable runnable) {

        java.util.Timer timer = new java.util.Timer("DelayedRunner", true);

        TimerTask delayedThreadStartTask = new TimerTask() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(runnable);
            }
        };

        timer.schedule(delayedThreadStartTask, ms);
    }

    /** Utility class to handle tab selections on the main frame */
    class myTabChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e)
        {
            if (topTabbedPane == null)
                return; // nothing to do, yet

            Model[] modelArray = ViskitGlobals.instance().getEventGraphViewFrame().getOpenModels();
            Model dirtyModel = null;

            // Make sure we save modified Event Graphs if we wander off to the Assembly tab
            for (Model nextModel : modelArray) {
                if (nextModel.isDirty()) {
                    dirtyModel = nextModel;
                    ViskitGlobals.instance().getEventGraphController().setModel((MvcModel) nextModel);
                    ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).save();
                }
            }

            if (dirtyModel != null && dirtyModel.isDirty()) {

                // This will fire another call to stateChanged()
                topTabbedPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX]);
                return;
            }

            int i = topTabbedPane.getSelectedIndex();

            // If we compiled and prepped an Assembly to run, but want to go
            // back and change something, then handle that here
            if (i == tabIndices[TAB0_SIMULATIONRUN_INDEX]) 
            {
                i = topTabbedPane.getTabCount() + getSimulationRunTabbedPane().getSelectedIndex();
                topTabbedPane.setToolTipTextAt(tabIndices[TAB0_SIMULATIONRUN_INDEX], "Run replications of simulation defined by selected Assembly");
            }
            else if ((topTabbedPane != null) && (topTabbedPane.getTabCount() >= TAB0_SIMULATIONRUN_INDEX)) // TODO why is this needed??
            {
                topTabbedPane.setToolTipTextAt(tabIndices[TAB0_SIMULATIONRUN_INDEX], "First initialize Assembly for Simulation Run from Assembly tab");
            }

            JMenuBar selectedMenuBar = new JMenuBar();
            if (helpMenu != null)
            {
                getJMenuBar().remove(helpMenu);
//                selectedMenuBar = modalMenuBarList.get(i);
                selectedMenuBar.add(helpMenu);
            }
            
            // TODO rename menu methods consistently
            combinedMenuBar.add(ViskitGlobals.instance().getAssemblyViewFrame().getProjectMenu());
            combinedMenuBar.add(ViskitGlobals.instance().getEventGraphViewFrame().getEventGraphMenu());
            combinedMenuBar.add(ViskitGlobals.instance().getAssemblyViewFrame().getAssemblyMenu());
            combinedMenuBar.add(ViskitGlobals.instance().getInternalSimulationRunner().getMenus());
            combinedMenuBar.add(ViskitGlobals.instance().getAnalystReportViewFrame().getMenus());
            combinedMenuBar.add(ViskitGlobals.instance().getAssemblyViewFrame().getHelpMenu());
            
            if  (hasModalMenus())
                 setJMenuBar(selectedMenuBar);
            else setJMenuBar(combinedMenuBar);
            
            // TODO is this needed?
            // myTitleListener.setTitle(titles[i], i);
        }
    }

    private JMenu helpMenu;

    /**
     * Stick the first Help menu we see into all the following ones.
     * @param menuBar
     */
    private void doCommonHelp(JMenuBar menuBar) 
    {
        JMenu menu;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            menu = menuBar.getMenu(i);
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
        ViskitUserPreferences.showDialog(MainFrame.this);
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

    class QuitAction extends AbstractAction {

        public QuitAction(String s) {
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
                    topTabbedPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX]);
                    if (!((EventGraphController) eventGraphFrame.getController()).preQuit()) {
                        break outer;
                    }
                }
                if (tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] != -1) {
                    topTabbedPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX]);
                    if (!((AssemblyControllerImpl) assemblyFrame.getController()).preQuit()) {
                        break outer;
                    }
                }

                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB0_SIMULATIONRUN_INDEX] != -1) {
                    topTabbedPane.setSelectedIndex(tabIndices[TAB0_SIMULATIONRUN_INDEX]);
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
                    getEventGraphController().removeEventGraphFileListener(getAssemblyController().getOpenEventGraphListener());
                    getEventGraphController().removeRecentEventGraphFileListener(ViskitGlobals.instance().getEventGraphViewFrame().getRecentEventGraphFileListener());

                    // TODO: Need doe listener removal (tdn) 9/13/24

                    ((EventGraphController) eventGraphFrame.getController()).postQuit();
                }
                if (tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX] != -1) {
                    getAssemblyController().removeAssemblyFileListener(getAssemblyController().getAssemblyChangeListener());
                    getAssemblyController().removeAssemblyFileListener((OpenAssembly.AssembyChangeListener) analystReportPanel);
                    getAssemblyController().removeRecentAssemblyFileSetListener(ViskitGlobals.instance().getAssemblyViewFrame().getRecentAssemblyFileListener());
                    getAssemblyController().removeRecentProjectFileSetListener(ViskitGlobals.instance().getAssemblyViewFrame().getRecentProjectFileSetListener());

                    // TODO: Need grid and doe listener removal (tdn) 9/13/24

                    ((AssemblyControllerImpl) assemblyFrame.getController()).postQuit();
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
                    LOG.info("in actionPerformed");
                }

                // Remember the size of this main frame set by the user
                Rectangle bounds = getBounds();
                ViskitConfigurationStore.instance().setValue(ViskitConfigurationStore.APP_MAIN_BOUNDS_KEY + "[@w]", "" + bounds.width);
                ViskitConfigurationStore.instance().setValue(ViskitConfigurationStore.APP_MAIN_BOUNDS_KEY + "[@h]", "" + bounds.height);

                // Pretty-fy all xml docs used for configuration
                ViskitConfigurationStore.instance().cleanup();

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

    /** Prepares the Assembly with a RunSimulation class loader free of static artifacts for
     * a completely independent run
     */
    class SimulationRunPlug implements SimulationRunInterface {

        @Override
        public void exec(String[] execStrings) {
            if (tabIndices[TAB0_SIMULATIONRUN_INDEX] != -1) {

                topTabbedPane.setEnabledAt(tabIndices[TAB0_SIMULATIONRUN_INDEX], true);

                // toggles a tab change listener
                topTabbedPane.setSelectedIndex(tabIndices[TAB0_SIMULATIONRUN_INDEX]);
                getSimulationRunTabbedPane().setSelectedIndex(TAB1_LOCALRUN_INDEX);

                // initializes a RunSimulation class loader
                internalSimulationRunner.preInitRun(execStrings);
            }
        }

        @Override
        public void resetSimulationRunPanel()
        {
            SimulationRunPanel simulationRunPanel = ViskitGlobals.instance().getSimulationRunPanel();
            simulationRunPanel.outputStreamTA.setText(null);
            simulationRunPanel.outputStreamTA.setText(
                    "Assembly output stream:" + simulationRunPanel.lineEnd +
                    "-----------------------" + simulationRunPanel.lineEnd);
            simulationRunPanel.vcrStartTimeTF.setText("");
            simulationRunPanel.vcrStopTimeTF.setText("");
            simulationRunPanel.numberReplicationsTF.setText(Integer.toString(SimulationRunPanel.DEFAULT_NUMBER_OF_REPLICATIONS)); // initialized in XML and panel
            simulationRunPanel.vcrVerboseCB.setSelected(false);
            simulationRunPanel.printReplicationReportsCB.setSelected(false);
            simulationRunPanel.printSummaryReportsCB.setSelected(false);

            internalSimulationRunner.vcrButtonPressDisplayUpdate(TextAreaOutputStream.Event.OFF); // initialize
            internalSimulationRunner.doTitle(null);
            
            simulationRunPanel.nowRunningLabel.setText("");
        }
    }

    String[] titles = new String[]{"", "", "", "", "", "", "", "", "", ""};
    TitleListener myTitleListener = new MyTitleListener();

    class MyTitleListener implements TitleListener  // TODO are tab-specific titles needed?
    {
        @Override
        public void setTitle(String title, int key) {
            titles[key] = title;
            int assemblyTabIndex = topTabbedPane.getSelectedIndex();
            if (assemblyTabIndex == tabIndices[TAB0_SIMULATIONRUN_INDEX]) {
                assemblyTabIndex = topTabbedPane.getTabCount() + getSimulationRunTabbedPane().getSelectedIndex();
                getSimulationRunTabbedPane().setTitleAt(TAB1_LOCALRUN_INDEX, title.substring(title.indexOf(":") + 1));
            }
            // TODO shift top-most title control to be separate
//            if (assemblyTabIndex == key) {
//                MainFrame.this.setTitle(title);
//            }
        }
    }
    
    /**
     * @return the simulationRunTabbedPane
     */
    public JTabbedPane getSimulationRunTabbedPane() {
        return simulationRunTabbedPane;
    }

    /**
     * Interface setting modalMenus: true means choice from traditional modalMenuBarList, false means new combinedMenuBar
     * @return the modalMenus
     */
    public boolean hasModalMenus() {
        return modalMenus;
    }

    /**
     * Interface setting modalMenus: true means choice from traditional modalMenuBarList, false means new combinedMenuBar
     * @param modalMenus the modalMenus to set
     */
    public void setModalMenus(boolean modalMenus) {
        this.modalMenus = modalMenus;
    }
    
    /** All done, any additional final cleanups can be performed here if needed. */
    public void quit()
    {
        System.exit(0);
    }

    /**
     * @return the assemblyController
     */
    public AssemblyControllerImpl getAssemblyController() {
        return assemblyController;
    }
    /**
     * Shows the project name in the frame title bar
     * @param newProjectName the new projectName
     */
    public void setTitleProjectName(String newProjectName)
    {
        String prefix = VISKIT_APPLICATION_TITLE;
        String newTitle;
        
        if (newProjectName == null)
        {
            LOG.error ("MainFrame.setTitleApplicationProjectName() received a null String, ignored");
            newProjectName = new String();
        }
        else if (newProjectName.isBlank())
        {
            LOG.error ("MainFrame.setTitleApplicationProjectName() received a blank String, ignored");
        }
        if  (newProjectName.isBlank())
             newTitle = prefix;
        else if (!newProjectName.toLowerCase().contains("project"))
             newTitle = prefix + ": Project " + newProjectName;
        else newTitle = prefix + ": "         + newProjectName;
        setTitle(newTitle);
        super.setTitle(newTitle);
    }
    public void selectEventGraphTab ()
    {
        topTabbedPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_INDEX]);
    }
    public void selectAssemblyTab ()
    {
        topTabbedPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLY_EDITOR_INDEX]);
    }
    public void selectSimulationRunTab ()
    {
        topTabbedPane.setSelectedIndex(tabIndices[TAB0_SIMULATIONRUN_INDEX]);
    }
    public void selectAnalystReportTab ()
    {
        topTabbedPane.setSelectedIndex(tabIndices[TAB0_ANALYSTREPORT_INDEX]);
    }

    public static void displayWelcomeGuidance()
    {
        // TODO check if author has entered profile information, offer to help if still needed

        if ((ViskitGlobals.instance().hasViskitProject()) &&
            (ViskitGlobals.instance().getEventGraphViewFrame().getNumberEventGraphsLoaded() == 0) &&
            (ViskitGlobals.instance().getAssemblyViewFrame().getNumberAssembliesLoaded()    == 0))
        {
            // provide initial guidance to new user who is facing an empty editor
            String message = "<html><body><p align='center'>Welcome to Viskit !</p><br />";
            if (ViskitGlobals.instance().isProjectOpen())
            {
                message +=   "<p align='center'>";
                if (!ViskitGlobals.instance().getProjectName().toLowerCase().contains("project"))
                     message +=   "Project ";
                message +=   "<i>" + ViskitGlobals.instance().getProjectName() + "</i> is the open project</p><br />";    
            }
            message +=       "<p align='center'>To get started, open or create an</p><br />";
            message +=       "<p align='center'><i>Event Graph</i> &nbsp;or <i>Assembly</i></p><br />";
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Add Event Graph or Assembly", message);
        }
    }
    
    // user-interaction methods moved from AssemblyViewFrame to MainFrame, up higher in hierarchy

    public String promptForStringOrCancel(String title, String message, String initialValue) {
        return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE, null, null, initialValue);
    }

    public int genericAskYesNo(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
    }

    /** 
     * Handle asking a question with 2 responses offered
     * @param title shown at top of panel
     * @param message body in display
     * @param buttonLabel1 for first choice
     * @param buttonLabel2 for second choice
     * @return returns -1 if closed, 0 for first button, 1 for second button */
    public int genericAsk2Buttons(String title, String message, String buttonLabel1, String buttonLabel2) {
        return JOptionPane.showOptionDialog(this, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{buttonLabel1, buttonLabel2}, buttonLabel1);
    }

    public void genericReport(int messageType, String title, String message) {
        if (messageType == JOptionPane.ERROR_MESSAGE) {
            AssemblyViewFrame.LOG.error(message);
            LOG.error("***" + message);
        }
        JOptionPane.showMessageDialog(ViskitGlobals.instance().getMainFrame(), message, title, messageType);
    }

    public int genericAsk(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
    }
}
