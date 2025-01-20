/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

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
import edu.nps.util.SysExitHandler;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitConfig;
import viskit.assembly.AssemblyRunnerPlug;
import viskit.control.AnalystReportController;
import viskit.control.AssemblyControllerImpl;
import viskit.control.AssemblyController;
import viskit.control.EventGraphController;
import viskit.control.InternalAssemblyRunner;
import viskit.control.RecentProjFileSetListener;
import viskit.doe.DoeMain;
import viskit.doe.DoeMainFrame;
import viskit.doe.JobLauncherTab2;
import viskit.model.Model;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModel;
import viskit.util.OpenAssembly;
import viskit.view.dialog.SettingsDialog;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 22, 2005
 * @since 3:25:11 PM
 * @version $Id$
 */
public class MainFrame extends JFrame {

    mvcAbstractJFrameView egFrame;
    mvcAbstractJFrameView assyFrame;
    mvcAbstractJFrameView reportPanel;
    InternalAssemblyRunner assyRunComponent;
    JobLauncherTab2 runGridComponent;

    private Action myQuitAction;
    private JTabbedPane tabbedPane;
    private JTabbedPane runTabbedPane;
    private DoeMain doeMain;

    /** The initial assembly to load. */
    private final String initialAssyFile;
    private final int TAB0_EVENTGRAPH_EDITOR_IDX = 0;
    private final int TAB0_ASSEMBLY_EDITOR_IDX = 1;
    private final int TAB0_ASSEMBLYRUN_SUBTABS_IDX = 2;
    private final int TAB0_ANALYST_REPORT_IDX = 3;
    private final int[] tabIndices = {
        TAB0_EVENTGRAPH_EDITOR_IDX,
        TAB0_ASSEMBLY_EDITOR_IDX,
        TAB0_ASSEMBLYRUN_SUBTABS_IDX,
        TAB0_ANALYST_REPORT_IDX
    };
    private final int TAB1_LOCALRUN_IDX = 0;
    private final int TAB1_DOE_IDX = 1;
    private final int TAB1_CLUSTERUN_IDX = 2;

    private AssemblyController assyCntlr;
    private EventGraphController egCntlr;

    public MainFrame(String initialAssyFile) {
        super(ViskitConfig.VISKIT_FULL_APPLICATION_NAME);
        this.initialAssyFile = initialAssyFile;
        ViskitGlobals.instance().setMainAppWindow(MainFrame.this);

        initUI();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Integer.parseInt(ViskitConfig.instance().getVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfig.instance().getVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@h]"));
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

    private void initUI() {
//        ViskitGlobals.instance().setAssemblyQuitHandler(null);
//        ViskitGlobals.instance().setEventGraphQuitHandler(null); <- TODO: investigate why these are here
        JMenuBar menuBar;
        int idx;

        ChangeListener tabChangeListener = new myTabChangeListener();
        myQuitAction = new ExitAction("Exit");

        tabbedPane = new JTabbedPane();

        // Swing:
        getContentPane().add(tabbedPane);
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD));

        // Tabbed event graph editor
        egFrame = ViskitGlobals.instance().buildEventGraphViewFrame();
        if (SettingsDialog.isEventGraphEditorVisible()) {
            tabbedPane.add(((EventGraphViewFrame) egFrame).getContent());
            idx = tabbedPane.indexOfComponent(((EventGraphViewFrame) egFrame).getContent());
            tabbedPane.setTitleAt(idx, "Event Graph Editor");
            tabbedPane.setToolTipTextAt(idx, "Visual editor for simulation entity definitions");
            menuBar = ((EventGraphViewFrame) egFrame).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            egFrame.setTitleListener(myTitleListener, idx);
            setJMenuBar(menuBar);
            jamQuitHandler(((EventGraphViewFrame) egFrame).getQuitMenuItem(), myQuitAction, menuBar);
            tabIndices[TAB0_EVENTGRAPH_EDITOR_IDX] = idx;
        } else {
            tabIndices[TAB0_EVENTGRAPH_EDITOR_IDX] = -1;
        }

        // Ensures EG editor is the selected tab for menu function
        tabbedPane.addChangeListener(tabChangeListener);

        // Assembly editor
        assyFrame = ViskitGlobals.instance().buildAssemblyViewFrame();
        if (SettingsDialog.isAssemblyEditorVisible()) {
            tabbedPane.add(((AssemblyViewFrame) assyFrame).getContent());
            idx = tabbedPane.indexOfComponent(((AssemblyViewFrame) assyFrame).getContent());
            tabbedPane.setTitleAt(idx, "Assembly Editor");
            tabbedPane.setToolTipTextAt(idx, "Visual editor for simulation defined by assembly");
            menuBar = ((AssemblyViewFrame) assyFrame).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            assyFrame.setTitleListener(myTitleListener, idx);
            setJMenuBar(menuBar);
            jamQuitHandler(((AssemblyViewFrame) assyFrame).getQuitMenuItem(), myQuitAction, menuBar);
            tabIndices[TAB0_ASSEMBLY_EDITOR_IDX] = idx;
        } else {
            tabIndices[TAB0_ASSEMBLY_EDITOR_IDX] = -1;
        }

        assyCntlr = (AssemblyController) assyFrame.getController();
        egCntlr = (EventGraphController) egFrame.getController();

        // Now set the recent open project's file listener for the egFrame now
        // that we have an assyFrame reference
        RecentProjFileSetListener listener = ((AssemblyViewFrame) assyFrame).getRecentProjFileSetListener();
        listener.addMenuItem(((EventGraphViewFrame) egFrame).getOpenRecentProjMenu());

        // Now setup the assembly and event graph file change listener(s)
        assyCntlr.addAssemblyFileListener(assyCntlr.getAssemblyChangeListener());
        egCntlr.addEventGraphFileListener(assyCntlr.getOpenEventGraphListener()); // A live listener, but currently doing nothing (tdn) 9/13/24

        // Assembly Run
        runTabbedPane = new JTabbedPane();
        runTabbedPane.addChangeListener(tabChangeListener);
        JPanel runTabbedPanePanel = new JPanel(new BorderLayout());
        runTabbedPanePanel.setBackground(new Color(206, 206, 255)); // light blue
        runTabbedPanePanel.add(runTabbedPane, BorderLayout.CENTER);

        // Always selected as visible
        if (SettingsDialog.isAssemblyRunVisible()) {
            tabbedPane.add(runTabbedPanePanel);
            idx = tabbedPane.indexOfComponent(runTabbedPanePanel);
            tabbedPane.setTitleAt(idx, "Assembly Run");
            tabbedPane.setToolTipTextAt(idx, "First initialize assembly runner from Assembly tab");
            menus.add(null); // placeholder
            tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX] = idx;
//          tabbedPane.setEnabledAt(idx, false); // TODO do not disable?
        } else {
            tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX] = -1;
        }

        // Analyst report
        boolean analystReportPanelVisible = SettingsDialog.isAnalystReportVisible();
        if (analystReportPanelVisible) {
            reportPanel = ViskitGlobals.instance().buildAnalystReportFrame();
            tabbedPane.add(reportPanel.getContentPane());
            idx = tabbedPane.indexOfComponent(reportPanel.getContentPane());
            tabbedPane.setTitleAt(idx, "Analyst Report");
            tabbedPane.setToolTipTextAt(idx, "Supports analyst assessment and report generation");
            menuBar = ((AnalystReportFrame) reportPanel).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(menuBar);
            }
            reportPanel.setTitleListener(myTitleListener, idx);
            jamQuitHandler(null, myQuitAction, menuBar);
            tabIndices[TAB0_ANALYST_REPORT_IDX] = idx;
            AnalystReportController cntlr = (AnalystReportController) reportPanel.getController();
            cntlr.setMainTabbedPane(tabbedPane, idx);
            assyCntlr.addAssemblyFileListener((OpenAssembly.AssyChangeListener) reportPanel);
        } else {
            tabIndices[TAB0_ANALYST_REPORT_IDX] = -1;
        }

        // Assembly runner
        assyRunComponent = new InternalAssemblyRunner(analystReportPanelVisible);
        runTabbedPane.add(ViskitGlobals.instance().getSimRunnerPanel(), TAB1_LOCALRUN_IDX);
        runTabbedPane.setTitleAt(TAB1_LOCALRUN_IDX, "Local Run");
        runTabbedPane.setToolTipTextAt(TAB1_LOCALRUN_IDX, "Run replications on local host");
        menuBar = assyRunComponent.getMenus();
        menus.add(menuBar);
        doCommonHelp(menuBar);
        jamSettingsHandler(menuBar);
        assyRunComponent.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_LOCALRUN_IDX);
        jamQuitHandler(assyRunComponent.getQuitMenuItem(), myQuitAction, menuBar);
        AssemblyControllerImpl controller = ((AssemblyControllerImpl) assyFrame.getController());
        controller.setInitialAssyFile(initialAssyFile);
        controller.setAssemblyRunner(new ThisAssemblyRunnerPlug());

        /* DIFF between OA3302 branch and trunk */

        // Design of experiments
        DoeMainFrame doeFrame = null;
        boolean isDOEVisible = SettingsDialog.isDOEVisible();
        if (isDOEVisible) {
            doeMain = DoeMain.main2();
            doeFrame = doeMain.getMainFrame();
            runTabbedPane.add(doeFrame.getContent(), TAB1_DOE_IDX);
            runTabbedPane.setTitleAt(TAB1_DOE_IDX, "Design of Experiments");
            runTabbedPane.setIconAt(TAB1_DOE_IDX, new ImageIcon(getClass().getClassLoader().getResource("viskit/images/grid.png")));
            menuBar = doeMain.getMenus();
            if (menuBar == null) {
                menuBar = new JMenuBar();
                menuBar.add(new JMenu("File"));
            }
            menus.add(menuBar);
            doCommonHelp(menuBar);
            doeFrame.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_DOE_IDX);
            jamQuitHandler(doeMain.getQuitMenuItem(), myQuitAction, menuBar);
            assyCntlr.addAssemblyFileListener(doeFrame.getController().getOpenAssemblyListener());
            egCntlr.addEventGraphFileListener(doeFrame.getController().getOpenEventGraphListener());
        }

        // Grid run panel
        if (SettingsDialog.isClusterRunVisible() && isDOEVisible) {
            runGridComponent = new JobLauncherTab2(doeMain.getController(), null, null, this);
            if (doeFrame != null)
                doeFrame.getController().setJobLauncher(runGridComponent);
            runTabbedPane.add(runGridComponent.getContent(), TAB1_CLUSTERUN_IDX);
            runTabbedPane.setTitleAt(TAB1_CLUSTERUN_IDX, "LaunchClusterJob");
            runTabbedPane.setIconAt(TAB1_CLUSTERUN_IDX, new ImageIcon(getClass().getClassLoader().getResource("viskit/images/grid.png")));
            menuBar = new JMenuBar();
            menuBar.add(new JMenu("File"));
            jamQuitHandler(null, myQuitAction, menuBar);
            menus.add(menuBar);
            doCommonHelp(menuBar);
            runGridComponent.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_CLUSTERUN_IDX);
            assyCntlr.addAssemblyFileListener(runGridComponent);
        }
        /* End DIFF between OA3302 branch and trunk */

        // let the event graph controller establish Viskit's classpath and open
        // EventGraphs first if an assy file has not been submitted at startup
        if (initialAssyFile == null || initialAssyFile.isBlank() || initialAssyFile.contains("$")) {
            runLater(0L, () -> {
                egCntlr.begin();
            });
        }

        runLater(500L, () -> {
            assyCntlr.begin();
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

            // Make sure we save modified EGs if we wander off to the Assy tab
            for (Model mod : mods) {
                if (mod.isDirty()) {
                    dirtyMod = mod;
                    ViskitGlobals.instance().getEventGraphController().setModel((mvcModel) mod);
                    ((EventGraphController) ViskitGlobals.instance().getEventGraphController()).save();
                }
            }

            if (dirtyMod != null && dirtyMod.isDirty()) {

                // This will fire another call to stateChanged()
                tabbedPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_IDX]);
                return;
            }

            int i = tabbedPane.getSelectedIndex();

            // If we compiled and prepped an Assembly to run, but want to go
            // back and change something, then handle that here
            if (i == tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX]) {
                i = tabbedPane.getTabCount() + runTabbedPane.getSelectedIndex();
                tabbedPane.setToolTipTextAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX], "Run simulation defined by assembly");
            } else {
                tabbedPane.setToolTipTextAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX], "First initialize assembly runner from Assembly tab");
            }

            getJMenuBar().remove(helpMenu);
            JMenuBar newMB = menus.get(i);
            newMB.add(helpMenu);
            setJMenuBar(newMB);
            myTitleListener.setTitle(titles[i], i);
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
        JMenu men;
        Component c;
        JMenuItem jmi;
        for (int i = 0; i < mb.getMenuCount(); i++) {
            men = mb.getMenu(i);
            if (men.getText().equalsIgnoreCase("File")) {
                for (int j = 0; j < men.getMenuComponentCount(); j++) {
                    c = men.getMenuComponent(j);
                    if (c instanceof JMenuItem) {
                        jmi = (JMenuItem) c;
                        if (jmi.getText().toLowerCase().contains("settings")) {
                            jmi.addActionListener(mySettingsHandler);
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
        public void actionPerformed(ActionEvent e) {
            SysExitHandler defaultHandler = ViskitGlobals.instance().getSysExitHandler();
            ViskitGlobals.instance().setSysExitHandler(nullSysExitHandler);

            // Tell Visit to not recompile open EGs from any remaining open
            // Assemblies when we perform a Viskit exit
            ((AssemblyControllerImpl) ViskitGlobals.instance().getAssemblyController()).setCloseAll(true);

            outer:
            {
                if (tabIndices[TAB0_EVENTGRAPH_EDITOR_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_EVENTGRAPH_EDITOR_IDX]);
                    if (!((EventGraphController) egFrame.getController()).preQuit()) {
                        break outer;
                    }
                }
                if (tabIndices[TAB0_ASSEMBLY_EDITOR_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLY_EDITOR_IDX]);
                    if (!((AssemblyController) assyFrame.getController()).preQuit()) {
                        break outer;
                    }
                }

                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX]);
                    if (doeMain != null) {
                        if (!doeMain.getController().preQuit()) {
                            break outer;
                        }
                    }
                }
                /* End DIFF between OA3302 branch and trunk */

                // TODO: other preQuits here if needed
                ViskitGlobals.instance().setSysExitHandler(defaultHandler);    // reset default handler

                if (tabIndices[TAB0_EVENTGRAPH_EDITOR_IDX] != -1) {
                    egCntlr.removeEventGraphFileListener(assyCntlr.getOpenEventGraphListener());
                    egCntlr.removeRecentEgFileListener(ViskitGlobals.instance().getEventGraphEditor().getRecentEgFileListener());

                    // TODO: Need doe listener removal (tdn) 9/13/24

                    ((EventGraphController) egFrame.getController()).postQuit();
                }
                if (tabIndices[TAB0_ASSEMBLY_EDITOR_IDX] != -1) {
                    assyCntlr.removeAssemblyFileListener(assyCntlr.getAssemblyChangeListener());
                    assyCntlr.removeAssemblyFileListener((OpenAssembly.AssyChangeListener) reportPanel);
                    assyCntlr.removeRecentAssyFileSetListener(ViskitGlobals.instance().getAssemblyEditor().getRecentAssyFileListener());
                    assyCntlr.removeRecentProjFileSetListener(ViskitGlobals.instance().getAssemblyEditor().getRecentProjFileSetListener());

                    // TODO: Need grid and doe listener removal (tdn) 9/13/24

                    ((AssemblyController) assyFrame.getController()).postQuit();
                }

                /* DIFF between OA3302 branch and trunk */
                if (doeMain != null) {
                    doeMain.getController().postQuit();
                }
                /* End DIFF between OA3302 branch and trunk */

                // TODO: other postQuits here if needed

                // Q: What is setting this true when it's false?
                // A: The Viskit Setting Dialog, third tab
                if (viskit.VStatics.debug) {
                    LogUtils.getLogger(ExitAction.class).info("in actionPerformed");
                }

                // Remember the size of this main frame set by the user
                Rectangle bounds = getBounds();
                ViskitConfig.instance().setVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@w]", "" + bounds.width);
                ViskitConfig.instance().setVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@h]", "" + bounds.height);

                // Pretty-fy all xml docs used for configuration
                ViskitConfig.instance().cleanup();

                // All other "Frames" were setVisible(false) above
                setVisible(false);

                // This will dispose all "Frames" and interrupt any non-daemon threads
                ViskitGlobals.instance().sysExit(0);  // quit application
            } //outer

            // Here if somebody cancelled.
            ViskitGlobals.instance().setSysExitHandler(defaultHandler);
        }
    }

    final SysExitHandler nullSysExitHandler = (int status) -> {
        // do nothing
    };

    /** Prepares the Assy with a fresh class loader free of static artifacts for
     * a completely independent run
     */
    class ThisAssemblyRunnerPlug implements AssemblyRunnerPlug {

        @Override
        public void exec(String[] execStrings) {
            if (tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX] != -1) {

                tabbedPane.setEnabledAt(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX], true);

                // toggles a tab change listener
                tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX]);
                runTabbedPane.setSelectedIndex(TAB1_LOCALRUN_IDX);

                // initializes a fresh class loader
                assyRunComponent.preInitRun(execStrings);
            }
        }

        @Override
        public void resetRunner() {
            RunnerPanel2 rp2 = ViskitGlobals.instance().getSimRunnerPanel();
            rp2.soutTA.setText(null);
            rp2.soutTA.setText("Assembly output stream:" + rp2.lineEnd
                    + "----------------------" + rp2.lineEnd);
            rp2.vcrSimTime.setText("");
            rp2.vcrStopTime.setText("");
            rp2.numRepsTF.setText("");
            rp2.vcrVerbose.setSelected(false);
            rp2.printRepReportsCB.setSelected(false);
            rp2.printSummReportsCB.setSelected(false);

            assyRunComponent.twiddleButtons(InternalAssemblyRunner.Event.OFF);
            assyRunComponent.doTitle(null);
        }
    }

    String[] titles = new String[]{"", "", "", "", "", "", "", "", "", ""};
    TitleListener myTitleListener = new myTitleListener();

    class myTitleListener implements TitleListener {

        @Override
        public void setTitle(String title, int key) {
            titles[key] = title;
            int tabIdx = tabbedPane.getSelectedIndex();
            if (tabIdx == tabIndices[TAB0_ASSEMBLYRUN_SUBTABS_IDX]) {
                tabIdx = tabbedPane.getTabCount() + runTabbedPane.getSelectedIndex();
            }

            if (tabIdx == key) {
                MainFrame.this.setTitle(title);
            }
        }
    }
}
