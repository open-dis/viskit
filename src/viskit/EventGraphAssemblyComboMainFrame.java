/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

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

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 22, 2005
 * @since 3:25:11 PM
 * @version $Id$
 */
package viskit;

import edu.nps.util.SysExitHandler;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import viskit.doe.DoeMain;

/* DIFF between OA3302 branch and trunk */
import viskit.doe.DoeMainFrame;
/* End DIFF between OA3302 branch and trunk */

import viskit.doe.FileHandler;
import viskit.doe.JobLauncherTab2;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.util.ArrayList;

public class EventGraphAssemblyComboMainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private JTabbedPane runTabbedPane;
    EventGraphViewFrame egFrame;
    AssemblyViewFrame asyFrame;
    InternalAssemblyRunner asyRunComponent;
    JobLauncherTab2 runGridComponent;
    AnalystReportPanel reportPanel;
    Action myQuitAction;
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

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((d.width - 800) / 2, (d.height - 600) / 2);
        //this.setSize(800, 600);
        this.setSize(930, 680);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                myQuitAction.actionPerformed(null);
            }
        });
        ImageIcon icon = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/ViskitSplash2.png"));
        this.setIconImage(icon.getImage());
    }
    
    ArrayList<JMenuBar> menus = new ArrayList<JMenuBar>();

    private void initUI() {
        VGlobals.instance().setAssemblyQuitHandler(null);
        VGlobals.instance().setEventGraphQuitHandler(null);
        JMenuBar menuBar;

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD));
        ChangeListener tabChangeListener = new myTabChangeListener();

        myQuitAction = new ExitAction("Exit");

        // Tabbed event graph editor
        egFrame = VGlobals.instance().initEventGraphViewFrame(true);
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
            jamQuitHandler(egFrame.getQuitMenuItem(), myQuitAction, egFrame.getMenus());
            tabIndices[TAB0_EGEDITOR_IDX] = idx;
        } else {
            tabIndices[TAB0_EGEDITOR_IDX] = -1;
        }

        // Assembly editor
        asyFrame = VGlobals.instance().initAssemblyViewFrame(true);
        if (SettingsDialog.isAssemblyEditorVisible()) {
            tabbedPane.add(asyFrame.getContent());
            int idx = tabbedPane.indexOfComponent(asyFrame.getContent());
            tabbedPane.setTitleAt(idx, "Assembly Editor");
            tabbedPane.setToolTipTextAt(idx, "Visual editor for simulation defined by assembly");

            menuBar = asyFrame.getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(menuBar);
            }
            asyFrame.setTitleListener(myTitleListener, idx);
            jamQuitHandler(asyFrame.getQuitMenuItem(), myQuitAction, asyFrame.getMenus());
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
        if (SettingsDialog.isAnalystReportVisible()) {
            tabbedPane.add(reportPanel = new AnalystReportPanel());
            int idx = tabbedPane.indexOfComponent(reportPanel);
            tabbedPane.setTitleAt(idx, "Analyst Report");
            tabbedPane.setToolTipTextAt(idx, "Support analyst assessment and produce report");
            menuBar = reportPanel.getMenus();
            menus.add(menuBar);
            doCommonHelp(menuBar);
            jamSettingsHandler(menuBar);
            if (getJMenuBar() == null) {
                setJMenuBar(menuBar);
            }
            reportPanel.setTitleListener(myTitleListener, idx);
            jamQuitHandler(null, myQuitAction, reportPanel.getMenus());
            tabIndices[TAB0_ANAL_REPORT_IDX] = idx;
        } else {
            tabIndices[TAB0_ANAL_REPORT_IDX] = -1;
        }

        // Assembly runner
        asyRunComponent = new InternalAssemblyRunner();
        runTabbedPane.add(asyRunComponent.getRunnerPanel(), TAB1_LOCALRUN_IDX);
        runTabbedPane.setTitleAt(TAB1_LOCALRUN_IDX, "Local Run");
        runTabbedPane.setToolTipTextAt(TAB1_LOCALRUN_IDX, "Run replications on local host");
        menuBar = asyRunComponent.getMenus();
        menus.add(menuBar);
        doCommonHelp(menuBar);
        jamSettingsHandler(menuBar);
        asyRunComponent.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_LOCALRUN_IDX);
        jamQuitHandler(asyRunComponent.getQuitMenuItem(), myQuitAction, asyRunComponent.getMenus());
        AssemblyController controller = ((AssemblyController) asyFrame.getController());
        controller.setInitialFile(initialFile);
        controller.setAssemblyRunner(new ThisAssemblyRunnerPlug());
        asyRunComponent.setAnalystReportGUI(reportPanel);
        
        /* DIFF between OA3302 branch and trunk */
        
        // Design of experiments    
        doeMain = DoeMain.main2();
        DoeMainFrame doeFrame = doeMain.getMainFrame();
        runTabbedPane.add(doeFrame.getContent(), TAB1_DOE_IDX);
        runTabbedPane.setTitleAt(TAB1_DOE_IDX, "Design of Experiments");
        runTabbedPane.setIconAt(TAB1_DOE_IDX, new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/grid.png")));
        menuBar = doeMain.getMenus();
        if (menuBar == null) {
            menuBar = new JMenuBar();
            menuBar.add(new JMenu("File"));
        }
        menus.add(menuBar);
        doCommonHelp(menuBar);
        doeFrame.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_DOE_IDX);
        jamQuitHandler(doeMain.getQuitMenuItem(), myQuitAction, menuBar);

        // Grid run panel
        runGridComponent = new JobLauncherTab2(doeMain.getController(), null, null, this);
        doeFrame.getController().setJobLauncher(runGridComponent);
        runTabbedPane.add(runGridComponent.getContent(), TAB1_CLUSTERUN_IDX);
        runTabbedPane.setTitleAt(TAB1_CLUSTERUN_IDX, "LaunchClusterJob");
        runTabbedPane.setIconAt(TAB1_CLUSTERUN_IDX, new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/grid.png")));
        menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        jamQuitHandler(null, myQuitAction, menuBar);
        menus.add(menuBar);
        doCommonHelp(menuBar);
        runGridComponent.setTitleListener(myTitleListener, tabbedPane.getTabCount() + TAB1_CLUSTERUN_IDX);

        /* End DIFF between OA3302 branch and trunk */
        
        // Now setup the assembly file change listener(s)
        final ViskitAssemblyController assyCntlr = (ViskitAssemblyController) asyFrame.getController();        
        assyCntlr.setRunTabbedPane(tabbedPane, tabbedPaneIdx);
        assyCntlr.addAssemblyFileListener(assyCntlr.getAssemblyChangeListener());
        assyCntlr.addAssemblyFileListener(asyRunComponent);
        
        /* DIFF between OA3302 branch and trunk */
        assyCntlr.addAssemblyFileListener(doeFrame.getController().getOpenAssemblyListener());
        /* End DIFF between OA3302 branch and trunk */
        
        assyCntlr.addAssemblyFileListener(runGridComponent);
        if (SettingsDialog.isAnalystReportVisible()) {
            assyCntlr.addAssemblyFileListener(reportPanel);
        }
        
        // Now setup the open-event graph listener(s)
        final ViskitController egCntlr = (ViskitController) egFrame.getController();
        egCntlr.addOpenEventGraphListener(assyCntlr.getOpenEventGraphListener());
        
        /* DIFF between OA3302 branch and trunk */
        egCntlr.addOpenEventGraphListener(doeFrame.getController().getOpenEventGraphListener());
        /* End DIFF between OA3302 branch and trunk */
        
        // Start the controllers
        runLater(0, new Runnable() {
            public void run() {
                egCntlr.begin();
            }
        });

        runLater(3000, new Runnable() {
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
        if (viskit.Vstatics.debug) {
            System.out.println("Run later: " + runr);
        }
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(ms);
                } catch (InterruptedException e) {}
                SwingUtilities.invokeLater(runr);
            }
        }, "runLater");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }

    class myTabChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            int i = tabbedPane.getSelectedIndex();
            if (i == tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]) {
                i = tabbedPane.getTabCount() + runTabbedPane.getSelectedIndex();
            }

            getJMenuBar().remove(hmen);
            JMenuBar newMB = menus.get(i);
            newMB.add(hmen);
            setJMenuBar(newMB);
            setTitle(titles[i]);
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
        for (int i = 0; i < al.length; i++) {
            mi.removeActionListener(al[i]);
        }

        mi.setAction(qa);
    }

    class ExitAction extends AbstractAction {

        public ExitAction(String s) {
            super(s);
        }

        public void actionPerformed(ActionEvent e) {
            SysExitHandler defaultHandler = VGlobals.instance().getSysExitHandler();
            VGlobals.instance().setSysExitHandler(nullSysExitHandler);

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
                    if (!((ViskitAssemblyController) asyFrame.getController()).preQuit()) {
                        break outer;
                    }
                }
                
                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB0_ASSYRUN_SUBTABS_IDX] != -1) {
                    tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]);
                    runTabbedPane.setSelectedIndex(TAB1_DOE_IDX);
                    if (!doeMain.getController().preQuit()) {
                        break outer;
                    }
                }
                /* End DIFF between OA3302 branch and trunk */

                //todo other preQuits here if needed
                VGlobals.instance().setSysExitHandler(defaultHandler);    // reset default handler

                if (tabIndices[TAB0_EGEDITOR_IDX] != -1) {
                    ((ViskitController) egFrame.getController()).postQuit();
                }
                if (tabIndices[TAB0_ASSYEDITOR_IDX] != -1) {
                    ((AssemblyController) asyFrame.getController()).postQuit();
                }
                
                /* DIFF between OA3302 branch and trunk */
                if (tabIndices[TAB0_ASSYRUN_SUBTABS_IDX] != -1) {
                    doeMain.getController().postQuit();
                }
                /* End DIFF between OA3302 branch and trunk */
                
                //todo other postQuits here if needed

                thisClassCleanup();
                
                // TODO: What is setting this true when it's false?
                // The Viskit Setting Dialog, third tab
                if (viskit.Vstatics.debug) {
                    System.out.println("in actionPerformed of exit");
                }
                VGlobals.instance().sysExit(0);  // quit application
            } //outer

            // Here if somebody cancelled.
            VGlobals.instance().setSysExitHandler(defaultHandler);
        }
    }

    private void thisClassCleanup() {
        // Lot of hoops to pretty-fy config xml files
        Document doc;
        Format form = Format.getPrettyFormat();
        XMLOutputter xout = new XMLOutputter(form);
        try {
            
            // For c_app.xml
            doc = FileHandler.unmarshallJdom(ViskitConfig.C_APP_FILE);
            xout.output(doc, new FileWriter(ViskitConfig.C_APP_FILE));            
        } catch (Exception e) {
            Vstatics.log.error("Bad jdom op: " + e.getMessage());
        }
    }
    
    private SysExitHandler nullSysExitHandler = new SysExitHandler() {

        public void doSysExit(int status) {
            // do nothing
        }
    };

    class ThisAssemblyRunnerPlug implements AssemblyRunnerPlug {

        /**
         * 
         * @param execStrings
         */
        public void exec(String[] execStrings) {
    
            /* The default version of this does a RuntimeExec("java"....) to 
             * spawn a new VM.  We want to run the assembly in a new VM, but not
             * the GUI.
             */
            if (tabIndices[TAB0_ASSYRUN_SUBTABS_IDX] != 0) {
                tabbedPane.setSelectedIndex(tabIndices[TAB0_ASSYRUN_SUBTABS_IDX]);
                runTabbedPane.setSelectedIndex(TAB1_LOCALRUN_IDX);
                asyRunComponent.initParams(execStrings);
            }
        }
    }
    
    String[] titles = new String[]{"", "", "", "", "", "", "", "", "", ""};
    TitleListener myTitleListener = new myTitleListener();

    class myTitleListener implements TitleListener {

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