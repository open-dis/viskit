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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import viskit.ViskitUserConfiguration;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import static viskit.control.InternalAssemblyRunner.SimulationState.READY;

/**
 * A VCR-controls and TextArea panel.  Sends Simkit output to TextArea
 *
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @author Rick Goldberg
 * @since Jul 17, 2006
 * @since 3:17:07 PM
 */
public class SimulationRunPanel extends JPanel
{
    public final static int DEFAULT_NUMBER_OF_REPLICATIONS = 30; // also defined twice in viskit.xsd schema
    
    public final static String INITIAL_SIMULATION_RUN_HINT = "First initialize an Assembly before Simulation Run..."; // Local Run Simulation

    public static final String lineEnd = System.getProperty("line.separator");
    
    public final static String INITIAL_SIMULATION_RUN_HEADER = "Assembly output stream:" + lineEnd +
                                                               "-----------------------" + lineEnd;
    public static final String SIMULATION_RUN_PANEL_TITLE = "Simulation Run Console";
    
    public boolean dump = true;
    public boolean search;
    public JScrollPane scrollPane;
    public JTextArea outputStreamTA;
//    public JSplitPane xsplitPane;
    public JButton   vcrStopButton, vcrRunResumeButton, vcrRewindButton, vcrPauseStepButton, closeButton, vcrClearConsoleButton;
    public JCheckBox vcrVerboseCB;
    public JTextField vcrStartTimeTF, vcrStopTimeTF;
    public JCheckBox saveReplicationDataCB;
    public JCheckBox printReplicationReportsCB;
    public JCheckBox searchCB;
    public JDialog   searchPopupDialog;
    public JCheckBox printSummaryReportsCB;
    public JCheckBox resetSeedStateCB;
    public JCheckBox analystReportCB;
    public JTextField numberReplicationsTF;
    public JScrollBar scrollBar;
    public JTextField verboseReplicationNumberTF;
    public static final String VERBOSE_REPLICATION_NUMBER_DEFAULT_HINT = "[replication #]";
    public JLabel  vcrButtonStatusLabel, nowRunningLabel;
    public JLabel  viskitRunnerBannerLabel;
    private String viskitRunnerBannerString;
    public JLabel iconLabel;
    private String title;
    private boolean hasLoadedAssembly = false;

    private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
    private JLabel titleLabel;
    private final boolean analystReportPanelVisible;


    /**
     * Create an Assembly Runner panel
     * @param newTitle the title of this panel
     * @param showExtraButtons if true, supply rewind or pause buttons on VCR,
     * not hooked up, or working right.  A true will enable all VCR buttons.
     * Currently, only start and stop work
     * @param analystReportPanelVisible if true, will enable the Analyst Report check box
     */
    public SimulationRunPanel(String newTitle, boolean showExtraButtons, boolean analystReportPanelVisible)
    {
        this.analystReportPanelVisible = analystReportPanelVisible; // TODO why?
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//        JPanel titlePanel = new JPanel();
        if (newTitle != null)
        {
            titleLabel = new JLabel(newTitle);
            titleLabel.setToolTipText("Console output is editable to support copying and analysis");
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
            titleLabel.setBorder(new EmptyBorder(0,0,0,0));
            add(titleLabel, BorderLayout.NORTH);
        }
//        titlePanel.add(titleLabel, BorderLayout.NORTH);
//        titlePanel.add(clearConsoleButton, BorderLayout.EAST);
//        add(titlePanel);
        
        JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane leftSideHorizontalSplit;

        outputStreamTA = new JTextArea(INITIAL_SIMULATION_RUN_HINT + lineEnd);
        outputStreamTA.setEditable(true); // to allow for additional manual input prior to saving out
        outputStreamTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputStreamTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        // don't force an initial scroller outputStreamTA.setRows(100);
        scrollPane = new JScrollPane(outputStreamTA);
        scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setUnitIncrement(STEPSIZE);

        JComponent vcrPanel = makeReplicationSettingsVCRPanel(showExtraButtons);

        viskitRunnerBannerString = lineEnd; // provide spacing, presumably
        viskitRunnerBannerLabel = new JLabel(viskitRunnerBannerString, JLabel.CENTER);
        viskitRunnerBannerLabel.setVerticalTextPosition(JLabel.TOP);
        
        // https://stackoverflow.com/questions/6714045/how-to-resize-jlabel-imageicon
        Icon    npsIcon = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"));
        Icon viskitIcon = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/ViskitSplash2.png"));
        String iconString = "";        
        iconLabel = new JLabel(iconString, viskitIcon, JLabel.CENTER); // npsIcon, 
        iconLabel.setVerticalTextPosition(JLabel.TOP);
        iconLabel.setHorizontalTextPosition(JLabel.CENTER);
        iconLabel.setIconTextGap(20);

        int w = Integer.parseInt(ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitUserConfiguration.instance().getValue(ViskitUserConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]"));

        leftSideHorizontalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, new JScrollPane(vcrPanel), iconLabel);
        
        // reduce magnitude of offset value (add positive increment) to provide more room in upper-left vcrPanel 
        leftSideHorizontalSplit.setDividerLocation((h/2) + 10);

        leftRightSplit.setLeftComponent(leftSideHorizontalSplit);
        leftRightSplit.setRightComponent(scrollPane);
        leftRightSplit.setDividerLocation((w/2) - (w/4) + 10);

        add(leftRightSplit, BorderLayout.CENTER);
    }
    
    private JPanel makeReplicationSettingsVCRPanel()
    {
        return makeReplicationSettingsVCRPanel (false);
    }
    
    private JPanel makeReplicationSettingsVCRPanel(boolean showIncompleteButtons)
    {
        JPanel upperLeftFlowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JPanel runLabelPanel = new JPanel();
        JLabel runLabel = new JLabel("Run assembly simulation");
        runLabel.setToolTipText("Run controls for assembly simulation");
        runLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        runLabel.setBorder(new EmptyBorder(0,0,0,10));
        runLabelPanel.setToolTipText("Run controls for assembly simulation");
        runLabelPanel.add(runLabel);
        upperLeftFlowPanel.add(runLabelPanel);
        upperLeftFlowPanel.add(Box.createVerticalStrut(4));
//        upperLeftFlowPanel.add(Box.createHorizontalStrut(40)); // indent next button panel

        JPanel vcrButtonsPanel = new JPanel();
        vcrButtonsPanel.setLayout(new BoxLayout(vcrButtonsPanel, BoxLayout.X_AXIS));
//        runButtonPanel.setBorder(new EmptyBorder(0,10,0,20));

        vcrRewindButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewindButton.setBorder(BorderFactory.createEtchedBorder());
        vcrRewindButton.setText(null);
        vcrRewindButton.setToolTipText("Reset the simulation run");
        vcrRewindButton.setEnabled(true); // false true
        if (showIncompleteButtons) 
        {
            vcrButtonsPanel.add(vcrRewindButton);
        }

        vcrRunResumeButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Play24.gif")));
        vcrRunResumeButton.setBorder(BorderFactory.createEtchedBorder());
        vcrRunResumeButton.setText(null);
        vcrRunResumeButton.setToolTipText("Run or resume the simulation");
        vcrRunResumeButton.setEnabled(true);
        vcrButtonsPanel.add(vcrRunResumeButton);

        vcrPauseStepButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrPauseStepButton.setBorder(BorderFactory.createEtchedBorder());
        vcrPauseStepButton.setText(null);
        vcrPauseStepButton.setToolTipText("Single step the simulation");
        vcrPauseStepButton.setEnabled(true); // false true
        vcrButtonsPanel.add(vcrPauseStepButton); // i.e. vcrPause

        vcrStopButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStopButton.setBorder(BorderFactory.createEtchedBorder());
        vcrStopButton.setText(null);
        vcrStopButton.setToolTipText("Stop the simulation run");
        vcrStopButton.setEnabled(true); // false true
        vcrButtonsPanel.add(vcrStopButton);
        
        vcrClearConsoleButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Delete24.gif"))); // Clear
        
        ViskitStatics.clampComponentSize(vcrClearConsoleButton, vcrStopButton, vcrStopButton);
        // https://stackoverflow.com/questions/1954674/can-i-make-swing-jbuttons-have-smaller-margins
        vcrClearConsoleButton.setMargin(new Insets(3, 4, 3, 4));
        vcrClearConsoleButton.setToolTipText("Clear all console text");
        // https://stackoverflow.com/questions/9569700/java-call-method-via-jbutton
        vcrClearConsoleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputStreamTA.selectAll();
                outputStreamTA.replaceSelection(""); // cleears
            }
        });
        vcrButtonsPanel.add(vcrClearConsoleButton);
        
        vcrButtonStatusLabel = new JLabel(" " + READY);
        vcrButtonStatusLabel.setToolTipText("Status of simulation run state machine");
        vcrButtonsPanel.add(vcrButtonStatusLabel);

        upperLeftFlowPanel.add(Box.createVerticalStrut(4));
        upperLeftFlowPanel.add(vcrButtonsPanel);
        upperLeftFlowPanel.add(Box.createVerticalStrut(4));
        
        nowRunningLabel = new JLabel(new String(), JLabel.CENTER);
        nowRunningLabel.setBorder(new EmptyBorder(0,1,0,10));
        nowRunningLabel.setText(lineEnd);
        // text value is set by propertyChange listener
        upperLeftFlowPanel.add(Box.createVerticalBox()); // TODO which?
        upperLeftFlowPanel.add(Box.createVerticalStrut(4));
        upperLeftFlowPanel.add(nowRunningLabel);
        upperLeftFlowPanel.add(Box.createVerticalStrut(4));

        vcrButtonsPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        JLabel replicationSettingsLabel = new JLabel("Replication Settings", JLabel.CENTER);
        replicationSettingsLabel.setToolTipText("These settings control simulation replications");
        upperLeftFlowPanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(replicationSettingsLabel);

        JLabel vcrStartTimeLabel = new JLabel("Sim start time: ");
        vcrStartTimeLabel.setToolTipText("Initial simulation start time");
//      ViskitStatics.clampComponentSize(vcrStartTimeLabel); // TODO unexpected reduction
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        vcrStartTimeLabel.setBorder(new EmptyBorder(0,2,0,10));
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        // TODO:  can a user use this to advance to a certain time in the sim?
        vcrStartTimeTF = new JTextField(10);
        vcrStartTimeTF.setEditable(false);
        ViskitStatics.clampComponentSize(vcrStartTimeTF);
        JPanel vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(vcrStartTimeLabel);
        vcrSimTimePanel.add(vcrStartTimeTF);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(vcrSimTimePanel);

        JLabel vcrStopTimeLabel = new JLabel("Sim stop time: ");
        vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        ViskitStatics.clampComponentSize(vcrStopTimeLabel, vcrStartTimeLabel, vcrStartTimeLabel);
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        vcrStopTimeLabel.setBorder(new EmptyBorder(0,2,0,10));
        vcrStopTimeTF = new JTextField(10);
        ViskitStatics.clampComponentSize(vcrStopTimeTF, vcrStartTimeTF, vcrStartTimeTF);
        vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(vcrStopTimeLabel);
        vcrSimTimePanel.add(vcrStopTimeTF);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(vcrSimTimePanel);

        JLabel numberReplicationsLabel = new JLabel("# replications: ");
        numberReplicationsLabel.setToolTipText("How many replications (simulation executions) to run");
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        numberReplicationsLabel.setBorder(new EmptyBorder(0,2,0,10));
        numberReplicationsTF = new JTextField(10);
        numberReplicationsTF.setToolTipText("How many simulation repetitions are run to get statistics");
        numberReplicationsTF.setText(Integer.toString(DEFAULT_NUMBER_OF_REPLICATIONS));
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        numberReplicationsTF.setBorder(new EmptyBorder(0,2,0,0));
        numberReplicationsTF.addActionListener((ActionEvent e) -> {
            int numberReplications = Integer.parseInt(numberReplicationsTF.getText().trim());
            if (numberReplications < 1) 
            {
                numberReplicationsTF.setText(Integer.toString(DEFAULT_NUMBER_OF_REPLICATIONS));
            }
        });
        ViskitStatics.clampComponentSize(numberReplicationsTF, vcrStartTimeTF, vcrStartTimeTF);
        
        String[] exampleReplicationCounts =
        {
               "1", // 0
               "2", // 1
              "10", // 2
              "30", // 3
             "100", // 4
            "1000", // 5
        };
        JComboBox numberReplicationsComboBox = new JComboBox<>(exampleReplicationCounts);
        numberReplicationsComboBox.setSelectedIndex(3); // 30, law of large numbers (LLN)
        numberReplicationsComboBox.setEditable(true);
        numberReplicationsComboBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String count = (String) cb.getSelectedItem();
                numberReplicationsTF.setText(count);
                // TODO any further updates?
            }
        });
        ViskitStatics.clampComponentSize(numberReplicationsComboBox, vcrStartTimeTF, vcrStartTimeTF);
        
        vcrSimTimePanel = new JPanel();
//      vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(numberReplicationsLabel);
//      vcrSimTimePanel.add(numberReplicationsTF);
        vcrSimTimePanel.add(numberReplicationsComboBox);
        upperLeftFlowPanel.add(vcrSimTimePanel);

        vcrVerboseCB = new JCheckBox("Verbose output", false);
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        vcrVerboseCB.setBorder(new EmptyBorder(0,2,0,10));
        vcrVerboseCB.addActionListener(new VcrVerboseCBListener());
        vcrVerboseCB.setToolTipText("Enables verbose output for all runs (or one run)");
        upperLeftFlowPanel.add(vcrVerboseCB);

        verboseReplicationNumberTF = new JTextField(7);
        VerboseReplicationNumberTFListener verboseReplicationNumberTFListener = new VerboseReplicationNumberTFListener();
        verboseReplicationNumberTF.addActionListener(verboseReplicationNumberTFListener);
        verboseReplicationNumberTF.addCaretListener(verboseReplicationNumberTFListener);
        ViskitStatics.clampComponentSize(verboseReplicationNumberTF);
        verboseReplicationNumberTF.setText(VERBOSE_REPLICATION_NUMBER_DEFAULT_HINT);
        verboseReplicationNumberTF.setToolTipText("Which replication run (1..n) will be verbose?");
        upperLeftFlowPanel.add(verboseReplicationNumberTF);

        // what is this??
//        closeButton = new JButton( "Close");
//        closeButton.setToolTipText("Close this window");
//        if (showIncompleteButtons) {
//            upperLeftFlowPanel.add(closeButton);
//        }

        printReplicationReportsCB = new JCheckBox("Log replication reports");
        printReplicationReportsCB.setToolTipText("Print Output Report for Replications to console");
        upperLeftFlowPanel.add(printReplicationReportsCB);
        printSummaryReportsCB = new JCheckBox("Log summary report");
        printSummaryReportsCB.setToolTipText("Log the Summary Output Report for Replication(s) to console");
        upperLeftFlowPanel.add(printSummaryReportsCB);

        saveReplicationDataCB = new JCheckBox("Save replication data to XML file");
        saveReplicationDataCB.setToolTipText("Save replication data to XML file, for use as Analyst Report");
        saveReplicationDataCB.setSelected(analystReportPanelVisible);
        saveReplicationDataCB.setEnabled(analystReportPanelVisible);
        upperLeftFlowPanel.add(saveReplicationDataCB);
        analystReportCB = new JCheckBox("Enable Analyst Reports");
        analystReportCB.setToolTipText("Replication data saved to XML is used to generate HTML reports");
        analystReportCB.setSelected(analystReportPanelVisible);
        analystReportCB.setEnabled(analystReportPanelVisible);
        upperLeftFlowPanel.add(analystReportCB);
        
        upperLeftFlowPanel.add(Box.createVerticalStrut(4));
        
        // Initially, unselected
        resetSeedStateCB = new JCheckBox("Reset seed state each rerun");

        // TODO: Expose at a later time when we have use for this
        resetSeedStateCB.setEnabled(false);
//      flowPanel.add(resetSeedStateCB);

        upperLeftFlowPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        upperLeftFlowPanel.setPreferredSize(new Dimension(vcrRunResumeButton.getPreferredSize()));
        
        return upperLeftFlowPanel;
    }

    class VcrVerboseCBListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent event) 
        {
            if (verboseReplicationNumberTF.getText().isBlank())
            {
                verboseReplicationNumberTF.setText(VERBOSE_REPLICATION_NUMBER_DEFAULT_HINT);
            }
        }
    }

    class VerboseReplicationNumberTFListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            if (!verboseReplicationNumberTF.getText().isEmpty() || 
                 verboseReplicationNumberTF.getText().equals(VERBOSE_REPLICATION_NUMBER_DEFAULT_HINT)) 
            {
                // probably no response needed, keep user in control of panel settings
                // vcrVerboseCB.setSelected(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }
    // utility methods
    
    public int getTotalReplications()
    {
        if (!numberReplicationsTF.getText().isBlank() && ViskitGlobals.isNumeric(numberReplicationsTF.getText().trim()))
        {
            return Integer.parseInt(numberReplicationsTF.getText().trim());
        }
        else return 0;
    }
    
    private int numberOfReplications;
    
    public int getNumberOfReplications()
    {
        return numberOfReplications;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setNumberOfReplications = "setNumberOfReplications";
    
    public void setNumberOfReplications(int value)
    {
        numberOfReplications = value;
    }

    /**
     * @return the viskitRunnerNoteString
     */
    public String getViskitRunnerBannerString() 
    {
        return viskitRunnerBannerString;
    }

    /**
     * @param newViskitRunnerFeedbackString the viskitRunnerNoteString to set
     */
    public void setViskitRunnerFeedbackString(String newViskitRunnerFeedbackString) 
    {
        this.viskitRunnerBannerString = newViskitRunnerFeedbackString;
        viskitRunnerBannerLabel.setText(newViskitRunnerFeedbackString);
    }

    /**
     * @return the title
     */
    public String getTitle() 
    {
        return title;
    }

    /**
     * @param newTitle the title to set
     */
    public void setTitle(String newTitle) 
    {
        title = newTitle;
        titleLabel.setText(newTitle);
        revalidate();
        repaint();
    }

    /**
     * @return whether Assembly has been loaded in SimulationRunPanel
     */
    public boolean hasLoadedAssembly() {
        return hasLoadedAssembly;
    }

    /**
     * @param hasLoadedAssembly confirm whether Assembly has been loaded in SimulationRunPanel
     */
    public void setHasLoadedAssembly(boolean hasLoadedAssembly) {
        this.hasLoadedAssembly = hasLoadedAssembly;
    }
}
