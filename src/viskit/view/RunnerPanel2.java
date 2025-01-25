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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import viskit.ViskitConfiguration;
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
public class RunnerPanel2 extends JPanel {

    public boolean dump = true;
    public boolean search;
    public String lineEnd = System.getProperty("line.separator");
    public JScrollPane jsp;
    public JTextArea soutTA;
    public JSplitPane xsplPn;
    public JButton vcrStop, vcrPlay, vcrRewind, vcrStep, closeButton;
    public JCheckBox vcrVerbose;
    public JTextField vcrSimTime, vcrStopTime;
    public JCheckBox saveRepDataCB;
    public JCheckBox printReplicationReportsCB;
    public JCheckBox searchCB;
    public JDialog searchPopup;
    public JCheckBox printSummaryReportsCB;
    public JCheckBox resetSeedStateCB;
    public JCheckBox analystReportCB;
    public JTextField numberReplicationsTF;
    public JScrollBar bar;
    public JTextField verboseReplicationNumberTF;
    public JLabel npsLabel;

    private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
    private JLabel titl;
    private final boolean aRPanelVisible;

    /**
     * Create an Assembly Runner panel
     * @param title the title of this panel
     * @param skipCloseButt if true, don't supply rewind or pause buttons on VCR,
     * not hooked up, or working right.  A false will enable all VCR buttons.
     * Currently, only start and stop work
     * @param analystReportPanelVisible if true, will enable the analyst report check box
     */
    public RunnerPanel2(String title, boolean skipCloseButt, boolean analystReportPanelVisible) {
        this.aRPanelVisible = analystReportPanelVisible;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        if (title != null) {
            titl = new JLabel(title);
            titl.setHorizontalAlignment(JLabel.CENTER);
            add(titl, BorderLayout.NORTH);
        }
        JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane leftSplit;

        soutTA = new JTextArea("Assembly output stream:" + lineEnd +
                "----------------------" + lineEnd);
        soutTA.setEditable(true); // to allow for additional manual input for saving out
        soutTA.setToolTipText("This text area space is editable");
        soutTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        soutTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        // don't force an initial scroller soutTA.setRows(100);
        jsp = new JScrollPane(soutTA);
        bar = jsp.getVerticalScrollBar();
        bar.setUnitIncrement(STEPSIZE);

        JComponent vcrPanel = makeVCRPanel(skipCloseButt);

        Icon npsIcon = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"));
        String npsString = "";

        npsLabel = new JLabel(npsString, npsIcon, JLabel.CENTER);
        npsLabel.setVerticalTextPosition(JLabel.TOP);
        npsLabel.setHorizontalTextPosition(JLabel.CENTER);
        npsLabel.setIconTextGap(50);

        int w = Integer.parseInt(ViskitConfiguration.instance().getVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfiguration.instance().getVal(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]"));

        leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, new JScrollPane(vcrPanel), npsLabel);
        leftSplit.setDividerLocation((h/2) - 50); // TODO check with Ben, else -1

        leftRightSplit.setLeftComponent(leftSplit);
        leftRightSplit.setRightComponent(jsp);
        leftRightSplit.setDividerLocation((w/2) - (w/4));

        add(leftRightSplit, BorderLayout.CENTER);
    }

    private final String VERBOSE_REPLICATION_DEFAULT_LABEL = "[replication #]";
    
    private JPanel makeVCRPanel(boolean skipIncompleteButtons) {
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel vcrSimTimeLabel = new JLabel("Sim start time: ");
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        // TODO:  can a user use this to advance to a certain time in the sim?
        vcrSimTime = new JTextField(10);
        vcrSimTime.setEditable(false);
        ViskitStatics.clampSize(vcrSimTime, vcrSimTime, vcrSimTime);
        JPanel vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(vcrSimTimeLabel);
        vcrSimTimePanel.add(vcrSimTime);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        flowPanel.add(vcrSimTimePanel);

        JLabel vcrStopTimeLabel = new JLabel("Sim stop time: ");
        vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        vcrStopTime = new JTextField(10);
        ViskitStatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);
        vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(vcrStopTimeLabel);
        vcrSimTimePanel.add(vcrStopTime);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        flowPanel.add(vcrSimTimePanel);

        numberReplicationsTF = new JTextField(10);
        numberReplicationsTF.addActionListener((ActionEvent e) -> {
            int numReps = Integer.parseInt(numberReplicationsTF.getText().trim());
            if (numReps < 1) {
                numberReplicationsTF.setText("1");
            }
        });
        ViskitStatics.clampSize(numberReplicationsTF, numberReplicationsTF, numberReplicationsTF);
        JLabel numReplicationsLabel = new JLabel("# replications: ");
        vcrSimTimePanel = new JPanel();
        vcrSimTimePanel.setLayout(new BoxLayout(vcrSimTimePanel, BoxLayout.X_AXIS));
        vcrSimTimePanel.add(numReplicationsLabel);
        vcrSimTimePanel.add(numberReplicationsTF);
        vcrSimTimePanel.add(Box.createHorizontalStrut(10));
        flowPanel.add(vcrSimTimePanel);


        vcrVerbose = new JCheckBox("Verbose output", false);
        vcrVerbose.addActionListener(new vcrVerboseCBListener());
        vcrVerbose.setToolTipText("Enables verbose output for all runs");
        flowPanel.add(vcrVerbose);

        verboseReplicationNumberTF = new JTextField(7);
        VerboseReplicationNumberTFListener listener = new VerboseReplicationNumberTFListener();
        verboseReplicationNumberTF.addActionListener(listener);
        verboseReplicationNumberTF.addCaretListener(listener);
        ViskitStatics.clampSize(verboseReplicationNumberTF);
        verboseReplicationNumberTF.setToolTipText("Input a single replication run (1..n) to be verbose");
        verboseReplicationNumberTF.setText(VERBOSE_REPLICATION_DEFAULT_LABEL);
        flowPanel.add(verboseReplicationNumberTF);

        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        if (!skipIncompleteButtons) {
            flowPanel.add(closeButton);
        }

        printReplicationReportsCB = new JCheckBox("Print replication report(s)");
        printReplicationReportsCB.setToolTipText("Print Output Report for Replication(s) to console");
        flowPanel.add(printReplicationReportsCB);
        printSummaryReportsCB = new JCheckBox("Print summary report");
        printSummaryReportsCB.setToolTipText("Print out the Summary Output Report to console");
        flowPanel.add(printSummaryReportsCB);

        /* DIFF between OA3302 branch and trunk */
        saveRepDataCB = new JCheckBox("Save replication data to XML");
        saveRepDataCB.setToolTipText("Use in conjuction with Enable Analyst Reports to save replication data to XML");
        saveRepDataCB.setSelected(aRPanelVisible);
        saveRepDataCB.setEnabled(aRPanelVisible);
        flowPanel.add(saveRepDataCB);
        analystReportCB = new JCheckBox("Enable Analyst Reports");
        analystReportCB.setToolTipText("When enabled, replication data saved to XML will be used to generate HTML reports");
        analystReportCB.setSelected(aRPanelVisible);
        analystReportCB.setEnabled(aRPanelVisible);
        flowPanel.add(analystReportCB);

        // Initially, unselected
        resetSeedStateCB = new JCheckBox("Reset seed state each rerun");

        // TODO: Expose at a later time when we have use for this
        resetSeedStateCB.setEnabled(false);
//      flowPanel.add(resetSeedStateCB);
        
        JPanel runLabelPanel = new JPanel();
        JLabel runLabel = new JLabel("Run assembly: ");
        runLabelPanel.setToolTipText("Start the simulation replications");
        runLabelPanel.add(runLabel);
        flowPanel.add(runLabelPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        vcrRewind = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewind.setToolTipText("Reset the simulation run");
        vcrRewind.setEnabled(false);
        vcrRewind.setBorder(BorderFactory.createEtchedBorder());
        vcrRewind.setText(null);
        if (!skipIncompleteButtons) {
            buttonPanel.add(vcrRewind);
        }

        vcrPlay = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Play24.gif")));
        vcrPlay.setToolTipText("Start or resume the simulation run");
        vcrPlay.setBorder(BorderFactory.createEtchedBorder());
        vcrPlay.setText(null);
        buttonPanel.add(vcrPlay);

        vcrStep = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrStep.setToolTipText("Step the simulation");
        vcrStep.setBorder(BorderFactory.createEtchedBorder());
        vcrStep.setText(null);
        vcrStep.setToolTipText("Single step");
        vcrStep.setEnabled(false); // TODO
        buttonPanel.add(vcrStep);

        vcrStop = new JButton(new ImageIcon(getClass().getClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStop.setToolTipText("Stop the simulation run");
        vcrStop.setEnabled(false);
        vcrStop.setBorder(BorderFactory.createEtchedBorder());
        vcrStop.setText(null);
        buttonPanel.add(vcrStop);

        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        flowPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        flowPanel.setPreferredSize(new Dimension(vcrPlay.getPreferredSize()));

        flowPanel.add(buttonPanel);
        return flowPanel;
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
                // vcrVerbose.setSelected(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }
}
