/*
Copyright (c) 1995-2009 held by the author(s).  All rights reserved.

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

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.nps.util.SysExitHandler;
import viskit.util.TitleListener;
import viskit.VGlobals;
import viskit.ViskitConfig;
import viskit.assembly.AssemblyRunnerPlug;
import viskit.control.AssemblyControllerImpl;
import viskit.control.AssemblyController;
import viskit.control.EventGraphController;
import viskit.control.InternalAssemblyRunner;
import viskit.doe.DoeMain;
import viskit.doe.DoeMainFrame;
import viskit.doe.JobLauncherTab2;
import viskit.model.Model;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcModel;
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
public class EventGraphAssemblyComboMainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private JTabbedPane runTabbedPane;
    EventGraphViewFrame egFrame;
    AssemblyViewFrame assyFrame;
    InternalAssemblyRunner assyRunComponent;
    JobLauncherTab2 runGridComponent;
    mvcAbstractJFrameView reportPanel;
    public Action myQuitAction;
    private DoeMain doeMain;

    /** The initial assembly to load. */
    private String initialFile;
    private int TAB0_EGEDITOR_IDX = 0;
    private int TAB0_ASSYEDITOR_IDX = 1;
    private int TAB0_ASSYRUN_SUBTABS_IDX = 2;
    private int TAB0_ANAL_REPORT_IDX = 3;
    private int[] tabIndices = {TAB0_EGEDITOR_IDX, TAB0_ASSYEDITOR_IDX,
            TAB0_ASSYRUN_SUBTABS_IDX, TAB0_ANAL_REPORT_IDX};
    private final int TAB1_LOCALRUN_IDX = 0;
    private final int TAB1_DOE_IDX = 1;
    private final int TAB1_CLUSTERUN_IDX = 2;

    public EventGraphAssemblyComboMainFrame(String initialFile) {
        super("Viskit");

        this.initialFile = initialFile;

        initUI();

        int w = Integer.parseInt(ViskitConfig.instance().getVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfig.instance().getVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@h]"));

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((d.width - w) / 2, (d.height - h) / 2);
        this.setSize(w, h);

        // Let the quit handler take care of an exit initiation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                myQuitAction.actionPerformed(null);
            }
        });
        ImageIcon icon = new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/ViskitSplash2.png"));
        this.setIconImage(icon.getImage());
    }

    /** @return the quit action class for Viskit */
    public Action getMyQuitAction() {
        return myQuitAction;
    }

    java.util.List<JMenuBar> menus = new ArrayList<>();

    private void initUI() {
        VGlobals.instance().setAssemblyQuitHandler(null);
        VGlobals.instance().setEventGraphQuitHandler(null);
        JMenuBar menuBar;

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD));
        ChangeListener tabChangeListener = new myTabChangeListener();

        myQuitAction = new ExitAction("Exit");

        // Tabbed event graph editor
        egFrame = (EventGraphViewFrame) VGlobals.instance().buildEventGraphViewFrame();
        if (SettingsDialog.isEventGraphEditorVisible()) {
            tabbedPane.add(egFrame.getContent());
            int idx = tabbedPane.indexOfComponent(egFrame.getContent());
            tabbedPane.setTitleAt(idx, "Event Graph Editor");
            tabbedPane.setToolTipTextAt(idx, "Visual editor for object class definitions");
            menuBar = egFrame.getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            egFrame.setTitleListener(myTitleListener, idx);
            setJMenuBar(menuBar);
            jamQuitHandler(egFrame.getQuitMenuItem(), myQuitAction, menuBar);
            tabIndices[TAB0_EGEDITOR_IDX] = idx;
        } else {
            tabIndices[TAB0_EGEDITOR_IDX] = -1;
        }

        // Assembly editor
        assyFrame = (AssemblyViewFrame) VGlobals.instance().buildAssemblyViewFrame();
        if (SettingsDialog.isAssemblyEditorVisible()) {
            tabbedPane.add(assyFrame.getContent());
            int idx = tabbedPane.indexOfComponent(assyFrame.getContent());
            tabbedPane.setTitleAt(idx, "Assembly Editor");
            tabbedPane.setToolTipTextAt(idx, "Visual editor for simulation defined by assembly");

            menuBar = assyFrame.getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(menuBar);
            }
            assyFrame.setTitleListener(myTitleListener, idx);
            jamQuitHandler(assyFrame.getQuitMenuItem(), myQuitAction, menuBar);
            tabIndices[TAB0_ASSYEDITOR_IDX] = idx;
        } else {
            tabIndices[TAB0_ASSYEDITOR_IDX] = -1;
        }

        // Assembly Run
        runTabbedPane = new JTabbedPane();
        JPanel runTabbedPanePanel = new JPanel(new BorderLayout());
        runTabbedPanePanel.setBackground(new Color(206, 206, 255)); // light blue
        runTabbedPanePanel.add(runTabbedPane, BorderLayout.CENTER);
        int tabbedPaneIdx = -1;
        if (SettingsDialog.isAssemblyRunVisible()) {
            tabbedPane.add(runTabbedPanePanel);
            tabbedPaneIdx = tabbedPane.indexOfComponent(runTabbedPanePanel);
            tabbedPane.setTitleAt(tabbedPaneIdx, "Assembly Run");
            tabbedPane.setToolTipTextAt(tabbedPaneIdx, "Run simulation defined by assembly");
            menus.add(null); // placeholder
        }
        tabIndices[TAB0_ASSYRUN_SUBTABS_IDX] = tabbedPaneIdx;

        // Analyst report
        boolean aRPanelVisible = SettingsDialog.isAnalystReportVisible();
        if (aRPanelVisible) {
            reportPanel = VGlobals.instance().buildAnalystReportFrame();
            tabbedPane.add(reportPanel.getContentPane());
            int idx = tabbedPane.indexOfComponent(reportPanel.getContentPane());
            tabbedPane.setTitleAt(idx, "Analyst Report");
            tabbedPane.setToolTipTextAt(idx, "Support analyst assessment and produce report");
            menuBar = ((AnalystReportFrame)reportPanel).getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(menuBar);
            }
            ((AnalystReportFrame)reportPanel).setTitleListener(myTitleListener, idx);
            jamQuitHandler(null, myQuitAction, menuBar);
            tabIndices[TAB0_ANAL_REPORT_IDX] = idx;
        } else {
            tabIndices[TAB0_ANAL_REPORT_IDX] = -1;
        }

        // Assembly runner
        assyRunComponent = new InternalAssemblyRunner(aRPanelVisible);
        runTabbedPane.add(assyRunComponent.getRunnerPanel(), TAB1_LOCALRUN_IDX);
        runTabbedPane.setTitleAt(TAB1_LOCALRUN_IDX, "Local Run");
        runTabbedPane.setToolTipTextAt(TAB1_LOCALRUN_IDX, "Run replications on local host");
        menuBar = assyRunComponent.getMenus();
        menus.add(menuBar);
        doCommonHelp(menuBar);
        jamSettingsHandler(menuBar);
        assyRunComponent.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_LOCALRUN_IDX);
        jamQuitHandler(assyRunComponent.getQuitMenuItem(), myQuitAction, menuBar);
        AssemblyControllerImpl controller = ((AssemblyControllerImpl) assyFrame.getController());
        controller.setInitialFile(initialFile);
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
            runTabbedPane.setIconAt(TAB1_DOE_IDX, new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/grid.png")));
            menuBar = doeMain.getMenus();
            if (menuBar == null) {
                menuBar = new JMenuBar();
                menuBar.add(new JMenu("File"));
            }
            menus.add(menuBar);
            doCommonHelp(menuBar);
            doeFrame.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_DOE_IDX);
            jamQuitHandler(doeMain.getQuitMenuItem(), myQuitAction, menuBar);
        }

        // Grid run panel
        if (SettingsDialog.isClusterRunVisible()) {
            runGridComponent = new JobLauncherTab2(doeMain.getController(), null, null, this);
            doeFrame.getController().setJobLauncher(runGridComponent);
            runTabbedPane.add(runGridComponent.getContent(), TAB1_CLUSTERUN_IDX);
            runTabbedPane.setTitleAt(TAB1_CLUSTERUN_IDX, "LaunchClusterJob");
            runTabbedPane.setIconAt(TAB1_CLUSTERUN_IDX, new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/grid.png")));
            menuBar = new JMenuBar();
            menuBar.add(new JMenu("File"));
            jamQuitHandler(null, myQuitAction, menuBar);
            menus.add(menuBar);
            doCommonHelp(menuBar);
            runGridComponent.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_CLUSTERUN_IDX);
        }

        /* End DIFF between OA3302 branch and trunk */

        // Now setup the assembly file change listener(s)
        final AssemblyController assyCntlr = (AssemblyController) assyFrame.getController();
        assyCntlr.setAssemblyRunPane(tabbedPane, tabbedPaneIdx);
        assyCntlr.addAssemblyFileListener(assyCntlr.getAssemblyChangeListener());

        /* DIFF between OA3302 branch and trunk */
        if (isDOEVisible)
            assyCntlr.addAssemblyFileListener(doeFrame.getController().getOpenAssemblyListener());
        /* End DIFF between OA3302 branch and trunk */

        assyCntlr.addAssemblyFileListener(runGridComponent);
        if (aRPanelVisible) {
            assyCntlr.addAssemblyFileListener((AnalystReportFrame) reportPanel);
        }

        // Now setup the open-event graph listener(s)
        final EventGraphController egCntlr = (EventGraphController) egFrame.getController();
        egCntlr.addOpenEventGraphListener(assyCntlr.getOpenEventGraphListener());

        /* DIFF between OA3302 branch and trunk */
        if (isDOEVisible)
            egCntlr.addOpenEventGraphListener(doeFrame.getController().getOpenEventGraphListener());
        /* End DIFF between OA3302 branch and trunk */

        // let the event graph controller establish the Viskit classpath and open
        // EventGraphs first
        runLater(0L, new Runnable() {
            @Override
            public void run() {
                egCntlr.begin();
            }
        });

        runLater(500L, new Runnable() {
            @Override
            public void run() {
                assyCntlr.begin();
            }
        });

        // Swing:
        getContentPane().add(tabbedPane);

        tabbedPane.addChangeListener(tabChangeListener);
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

    class myTabChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {

            Model[] mods = VGlobals.instance().getEventGraphEditor().getOpenModels();
            Model dirtyMod = null;

            // Make sure we save modified EGs if we wander off to the Assy tab
            for (Model mod : mods) {

                if (mod.isDirty()) {
                    dirtyMod = mod;
                    VGlobals.instance().getEventGraphController().setModel((mvcModel) mod);
                    ((EventGraphController)VGlobals.instance().getEventGraphController()).save();
                }
            }

            if (dirtyMod != null && dirtyMod.isDirty()) {

                // This will fire another call stateChanged()
                tabbedPane.setSelectedIndex(tabIndices[TAB0_EGEDITOR_IDX]);
                return;
            }

            int i = tabbedPane.getSelectedIndex();
            if (i == tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]) {
                i = tabbedPane.getTabCount() + runTabbedPane.getSelectedIndex();
            }

            getJMenuBar().remove(hmen);
            JMenuBar newMB = menus.get(i);
            newMB.add(hmen);
            setJMenuBar(newMB);
            setTitle(titles[i]);

            // If we compiled and prepped an Assembly to run, but want to go
            // back and change something, then handle that here
            AssemblyController assyCont = (AssemblyController) assyFrame.getController();
            if (assyCont.getRunTabbedPane().isEnabledAt(assyCont.getRunTabbledPanelIdx())) {
                assyCont.getRunTabbedPane().setEnabledAt(assyCont.getRunTabbledPanelIdx(), false);

                // Resets the Viskit ClassLoader
                assyRunComponent.getAssemblyRunStopListener().actionPerformed(null);
            }
        }
    }

    private JMenu hmen;

    /**
     * Stick the first Help menu we see into all the following ones.
     * @param mb
     */
    private void doCommonHelp(JMenuBar mb) {
        for (int i = 0; i < mb.getMenuCount(); i++) {
            JMenu men = mb.getMenu(i);
            if (men.getText().equalsIgnoreCase("Help")) {
                if (hmen == null) {
                    hmen = men;
                } else {
                    mb.remove(i);
                }
                return;
            }
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        // tell the help screen where we are so he can center himself
        egFrame.help.mainFrameLocated(this.getBounds());
    }

    private void jamSettingsHandler(JMenuBar mb) {
        for (int i = 0; i < mb.getMenuCount(); i++) {
            JMenu men = mb.getMenu(i);
            if (men.getText().equalsIgnoreCase("File")) {
                for (int j = 0; j < men.getMenuComponentCount(); j++) {
                    Component c = men.getMenuComponent(j);
                    if (c instanceof JMenuItem) {
                        JMenuItem jmi = (JMenuItem) c;
                        if (jmi.getText().equalsIgnoreCase("settings")) {
                            jmi.addActionListener(mySettingsHandler);
                            return;
                        }
                    }
                }
            }
        }
    }

    ActionListener mySettingsHandler = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            SettingsDialog.showDialog(EventGraphAssemblyComboMainFrame.this);
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
            SysExitHandler defaultHandler = VGlobals.instance().getSysExitHandler();
            VGlobals.instance().setSysExitHandler(nullSysExitHandler);

            // Tell Visit to not recompile open EGs from any remaining open
            // Assemblies when we perform a Viskit exit
            ((AssemblyControllerImpl)VGlobals.instance().getAssemblyController()).setCloseAll(true);

            outer:
            {
                if (tabIndices[TAB0_EGEDITOR_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_EGEDITOR_IDX]);
                    if (!((EventGraphController) egFrame.getController()).preQuit()) {
                        break outer;
                    }
                }
                if (tabIndices[TAB0_ASSYEDITOR_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSYEDITOR_IDX]);
                    if (!((AssemblyController) assyFrame.getController()).preQuit()) {
                        break outer;
                    }
                }

                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB0_ASSYRUN_SUBTABS_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]);
                    if (doeMain != null) {
                        if (!doeMain.getController().preQuit()) {
                            break outer;
                        }
                    }
                }
                /* End DIFF between OA3302 branch and trunk */

                // TODO: other preQuits here if needed
                VGlobals.instance().setSysExitHandler(defaultHandler);    // reset default handler

                if (tabIndices[TAB0_EGEDITOR_IDX] != -1) {
                    ((EventGraphController) egFrame.getController()).postQuit();
                }
                if (tabIndices[TAB0_ASSYEDITOR_IDX] != -1) {
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
                    System.out.println("in actionPerformed of exit");
                }

                // Remember the size of this main frame set by the user
                Rectangle bounds = getBounds();
                ViskitConfig.instance().setVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@w]", "" + bounds.width);
                ViskitConfig.instance().setVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@h]", "" + bounds.height);

                // Pretty-fy all xml docs used for configuration
                ViskitConfig.instance().cleanup();

                VGlobals.instance().sysExit(0);  // quit application
            } //outer

            // Here if somebody cancelled.
            VGlobals.instance().setSysExitHandler(defaultHandler);
        }
    }

    private SysExitHandler nullSysExitHandler = new SysExitHandler() {

        @Override
        public void doSysExit(int status) {
            // do nothing
        }
    };

    /** Runs the Assy with a fresh class loader free of artifacts for a
     * completely independent run
     */
    class ThisAssemblyRunnerPlug implements AssemblyRunnerPlug {

        @Override
        public void exec(String[] execStrings) {
            if (tabIndices[TAB0_ASSYRUN_SUBTABS_IDX] != 0) {

                // toggles a tab change listener
                tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]);
                runTabbedPane.setSelectedIndex(TAB1_LOCALRUN_IDX);

                // initializes a fresh class loader
                assyRunComponent.preInitRun(execStrings);
            }
        }
    }

    String[] titles = new String[]{"", "", "", "", "", "", "", "", "", ""};
    TitleListener myTitleListener = new myTitleListener();

    class myTitleListener implements TitleListener {

        @Override
        public void setTitle(String title, int key) {
            titles[key] = title;
            int tabIdx = tabbedPane.getSelectedIndex();
            if (tabIdx == tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]) {
                tabIdx = tabbedPane.getTabCount() + runTabbedPane.getSelectedIndex();
            }

            if (tabIdx == key) {
                EventGraphAssemblyComboMainFrame.this.setTitle(title);
            }
        }
    }
}
