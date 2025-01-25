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

import viskit.ViskitConfiguration;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;

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
public class AssemblySimulationRunPanel extends JPanel
{
    public boolean dump = true;
    public boolean search;
    public String lineEnd = System.getProperty("line.separator");
    public JScrollPane scrollPane;
    public JTextArea outputStreamTA;
    public JSplitPane xsplitPane;
    public JButton vcrStopButton, vcrPlayButton, vcrRewindButton, vcrStepButton, closeButton;
    public JCheckBox vcrVerboseCB;
    public JTextField vcrSimTimeTF, vcrStopTimeTF;
    public JCheckBox saveReplicationDataCB;
    public JCheckBox printReplicationReportsCB;
    public JCheckBox searchCB;
    public JDialog searchPopupDialog;
    public JCheckBox printSummaryReportsCB;
    public JCheckBox resetSeedStateCB;
    public JCheckBox analystReportCB;
    public JTextField numberReplicationsTF;
    public JScrollBar scrollBar;
    public JTextField verboseReplicationNumberTF;
    public JLabel nowRunningLabel;
    public JLabel  viskitRunnerBannerLabel;
    private String viskitRunnerBannerString;
    public JLabel npsLabel;

    private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
    private JLabel title;
    private final boolean aRPanelVisible;

    /**
     * Create an Assembly Runner panel
     * @param newTitle the title of this panel
     * @param showExtraButtons if true, supply rewind or pause buttons on VCR,
     * not hooked up, or working right.  A true will enable all VCR buttons.
     * Currently, only start and stop work
     * @param analystReportPanelVisible if true, will enable the analyst report check box
     */
    public AssemblySimulationRunPanel(String newTitle, boolean showExtraButtons, boolean analystReportPanelVisible) {
        this.aRPanelVisible = analystReportPanelVisible;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        if (newTitle != null)
        {
            title = new JLabel(newTitle);
            title.setHorizontalAlignment(JLabel.CENTER);
            add(title, BorderLayout.NORTH);
        }
        JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane leftSideHorizontalSplit;

        outputStreamTA = new JTextArea("Assembly output stream:" + lineEnd +
                "----------------------" + lineEnd);
        outputStreamTA.setEditable(true); // to allow for additional manual input for saving out
        outputStreamTA.setToolTipText("This text area space is editable");
        outputStreamTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputStreamTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        // don't force an initial scroller outputStreamTA.setRows(100);
        scrollPane = new JScrollPane(outputStreamTA);
        scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.setUnitIncrement(STEPSIZE);

        JComponent vcrPanel = makeVCRPanel(showExtraButtons);

        viskitRunnerBannerString = lineEnd; // provide spacing, presumably
        viskitRunnerBannerLabel = new JLabel(viskitRunnerBannerString, JLabel.CENTER);
        viskitRunnerBannerLabel.setVerticalTextPosition(JLabel.TOP);
        
        Icon npsIcon = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"));
        String npsString = "";        
        npsLabel = new JLabel(npsString, npsIcon, JLabel.CENTER);
        npsLabel.setVerticalTextPosition(JLabel.TOP);
        npsLabel.setHorizontalTextPosition(JLabel.CENTER);
        npsLabel.setIconTextGap(50);

        int w = Integer.parseInt(ViskitConfiguration.instance().getVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfiguration.instance().getVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]"));

        leftSideHorizontalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, new JScrollPane(vcrPanel), viskitRunnerBannerLabel);
        leftSideHorizontalSplit.setDividerLocation((h/2) - 50); // TODO check with Ben, else -1

        leftRightSplit.setLeftComponent(leftSideHorizontalSplit);
        leftRightSplit.setRightComponent(scrollPane);
        leftRightSplit.setDividerLocation((w/2) - (w/4));

        add(leftRightSplit, BorderLayout.CENTER);
    }

    private final String VERBOSE_REPLICATION_DEFAULT_LABEL = "[replication #]";
    
    private JPanel makeVCRPanel()
    {
        return makeVCRPanel (false);
    }
    
    private JPanel makeVCRPanel(boolean showIncompleteButtons)
    {
        JPanel upperLeftFlowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel vcrSimTimeLabel = new JLabel("Sim start time: ");
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        // TODO:  can a user use this to advance to a certain time in the sim?
        vcrSimTimeTF = new JTextField(10);
        vcrSimTimeTF.setEditable(false);
        ViskitStatics.clampSize(vcrSimTimeTF, vcrSimTimeTF, vcrSimTimeTF);
        JPanel vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(vcrSimTimeLabel);
        vcrSimTimePanel.add(vcrSimTimeTF);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(vcrSimTimePanel);

        JLabel vcrStopTimeLabel = new JLabel("Sim stop time: ");
        vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        vcrStopTimeTF = new JTextField(10);
        ViskitStatics.clampSize(vcrStopTimeTF, vcrStopTimeTF, vcrStopTimeTF);
        vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(vcrStopTimeLabel);
        vcrSimTimePanel.add(vcrStopTimeTF);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        upperLeftFlowPanel.add(vcrSimTimePanel);

        numberReplicationsTF = new JTextField(10);
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        numberReplicationsTF.setBorder(new EmptyBorder(0,0,0,0));
        numberReplicationsTF.addActionListener((ActionEvent e) -> {
            int numReplications = Integer.parseInt(numberReplicationsTF.getText().trim());
            if (numReplications < 1) 
            {
                numberReplicationsTF.setText("1");
            }
        });
        ViskitStatics.clampSize(numberReplicationsTF, numberReplicationsTF, numberReplicationsTF);
        JLabel numberReplicationsLabel = new JLabel("# replications: ");
        vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(numberReplicationsLabel);
        vcrSimTimePanel.add(numberReplicationsTF);
        upperLeftFlowPanel.add(vcrSimTimePanel);

        vcrVerboseCB = new JCheckBox("Verbose output", false);
        vcrVerboseCB.addActionListener(new vcrVerboseCBListener());
        vcrVerboseCB.setToolTipText("Enables verbose output for all runs");
        upperLeftFlowPanel.add(vcrVerboseCB);

        verboseReplicationNumberTF = new JTextField(7);
        VerboseReplicationNumberTFListener listener = new VerboseReplicationNumberTFListener();
        verboseReplicationNumberTF.addActionListener(listener);
        verboseReplicationNumberTF.addCaretListener(listener);
        ViskitStatics.clampSize(verboseReplicationNumberTF);
        verboseReplicationNumberTF.setToolTipText("Input a single replication run (1..n) to be verbose");
        verboseReplicationNumberTF.setText(VERBOSE_REPLICATION_DEFAULT_LABEL);
        upperLeftFlowPanel.add(verboseReplicationNumberTF);

        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        if (showIncompleteButtons) {
            upperLeftFlowPanel.add(closeButton);
        }

        printReplicationReportsCB = new JCheckBox("Print replication report(s)");
        printReplicationReportsCB.setToolTipText("Print Output Report for Replication(s) to console");
        upperLeftFlowPanel.add(printReplicationReportsCB);
        printSummaryReportsCB = new JCheckBox("Print summary report");
        printSummaryReportsCB.setToolTipText("Print out the Summary Output Report to console");
        upperLeftFlowPanel.add(printSummaryReportsCB);

        /* DIFF between OA3302 branch and trunk */
        saveReplicationDataCB = new JCheckBox("Save replication data to XML");
        saveReplicationDataCB.setToolTipText("Use in conjuction with Enable Analyst Reports to save replication data to XML");
        saveReplicationDataCB.setSelected(aRPanelVisible);
        saveReplicationDataCB.setEnabled(aRPanelVisible);
        upperLeftFlowPanel.add(saveReplicationDataCB);
        analystReportCB = new JCheckBox("Enable Analyst Reports");
        analystReportCB.setToolTipText("When enabled, replication data saved to XML will be used to generate HTML reports");
        analystReportCB.setSelected(aRPanelVisible);
        analystReportCB.setEnabled(aRPanelVisible);
        upperLeftFlowPanel.add(analystReportCB);

        // Initially, unselected
        resetSeedStateCB = new JCheckBox("Reset seed state each rerun");

        // TODO: Expose at a later time when we have use for this
        resetSeedStateCB.setEnabled(false);
//      flowPanel.add(resetSeedStateCB);
        
        JPanel runLabelPanel = new JPanel();
        JLabel runLabel = new JLabel("Run assembly: ");
        // https://stackoverflow.com/questions/33172555/how-to-set-padding-at-jlabel
        runLabel.setBorder(new EmptyBorder(0,0,0,0));
        runLabelPanel.setToolTipText("Simulation replications control");
        runLabelPanel.add(runLabel);
        upperLeftFlowPanel.add(runLabelPanel);

        JPanel runButtonPanel = new JPanel();
        runButtonPanel.setLayout(new BoxLayout(runButtonPanel, BoxLayout.X_AXIS));
//        runButtonPanel.setBorder(new EmptyBorder(0,10,0,20));

        vcrRewindButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewindButton.setToolTipText("Reset the simulation run");
        vcrRewindButton.setEnabled(false);
        vcrRewindButton.setBorder(BorderFactory.createEtchedBorder());
        vcrRewindButton.setText(null);
        if (showIncompleteButtons) {
            runButtonPanel.add(vcrRewindButton);
        }

        vcrPlayButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Play24.gif")));
        vcrPlayButton.setToolTipText("Start or resume the simulation run");
        vcrPlayButton.setBorder(BorderFactory.createEtchedBorder());
        vcrPlayButton.setText(null);
        runButtonPanel.add(vcrPlayButton);

        vcrStepButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrStepButton.setToolTipText("Step the simulation");
        vcrStepButton.setBorder(BorderFactory.createEtchedBorder());
        vcrStepButton.setText(null);
        vcrStepButton.setToolTipText("Single step");
        vcrStepButton.setEnabled(false); // TODO
        runButtonPanel.add(vcrStepButton);

        vcrStopButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStopButton.setToolTipText("Stop the simulation run");
        vcrStopButton.setEnabled(false);
        vcrStopButton.setBorder(BorderFactory.createEtchedBorder());
        vcrStopButton.setText(null);
        runButtonPanel.add(vcrStopButton);

        upperLeftFlowPanel.add(runButtonPanel);
        
        nowRunningLabel = new JLabel(new String(), JLabel.CENTER);
        nowRunningLabel.setBorder(new EmptyBorder(0,5,0,10));
        nowRunningLabel.setText(lineEnd);
        // text value is set by propertyChange listener
        upperLeftFlowPanel.add(Box.createVerticalBox());
        upperLeftFlowPanel.add(nowRunningLabel);
//        upperLeftFlowPanel.add(Box.createHorizontalStrut(10));

        runButtonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        upperLeftFlowPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        upperLeftFlowPanel.setPreferredSize(new Dimension(vcrPlayButton.getPreferredSize()));
        
        return upperLeftFlowPanel;
    }

    class vcrVerboseCBListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) 
        {
            if (verboseReplicationNumberTF.getText().isBlank())
            {
                verboseReplicationNumberTF.setText(VERBOSE_REPLICATION_DEFAULT_LABEL);
            }
        }
    }

    class VerboseReplicationNumberTFListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            if (!verboseReplicationNumberTF.getText().isEmpty() || 
                 verboseReplicationNumberTF.getText().equals(VERBOSE_REPLICATION_DEFAULT_LABEL)) 
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
    
    public void setNumberOfReplications(int value)
    {
        numberOfReplications = value;
    }

    /**
     * @return the viskitRunnerNoteString
     */
    public String getViskitRunnerBannerString() {
        return viskitRunnerBannerString;
    }

    /**
     * @param newViskitRunnerFeedbackString the viskitRunnerNoteString to set
     */
    public void setViskitRunnerFeedbackString(String newViskitRunnerFeedbackString) {
        this.viskitRunnerBannerString = newViskitRunnerFeedbackString;
        viskitRunnerBannerLabel.setText(newViskitRunnerFeedbackString);
    }
}
