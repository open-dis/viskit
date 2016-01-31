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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import viskit.ViskitGlobals;
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
public class SimulationRunPanel extends JPanel {

    public boolean dump = true;
    public boolean search;
    public String lineEnd = System.getProperty("line.separator");
    public JScrollPane jsp;
    public JTextArea simulationOutputTA;
    public JSplitPane xsplPn;
    public JButton vcrStop,  vcrPlay,  vcrRewind,  vcrStep,  closeButt;
    public JCheckBox vcrVerbose;
    public JTextField vcrSimulationTime,  vcrStopTime;
    public JCheckBox saveRepDataCB;
    public JCheckBox printRepReportsCB;
    public JCheckBox searchCB;
    public JDialog searchPopup;
    public JCheckBox printSummReportsCB;
    public JCheckBox resetSeedStateCB;
    public JCheckBox analystReportCB;
    public JTextField numberOfReplicationsTF;
    public JScrollBar bar;
    public JTextField verboseRepNumberTF;
    public JLabel npsLabel;

    private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
	private final JLabel titleLabel = new JLabel();
    private final boolean assemblyRunPanelVisible;

    /**
     * Create an Simulation Run panel
     * @param title the title of this panel
     * @param skipCloseButton if ture, don't supply rewind or pause buttons on VCR,
     * not hooked up, or working right.  A false will enable all VCR buttons.
     * Currently, only start and stop work
     * @param assemblyRunPanelPanelVisible if true, will enable the analyst report check box
     */
    public SimulationRunPanel(String title, boolean skipCloseButton, boolean assemblyRunPanelPanelVisible) {
        this.assemblyRunPanelVisible = assemblyRunPanelPanelVisible;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.NORTH);
        if (title != null)
		{
			setTitle (title);
        }
        JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane leftSplit;
		
        simulationOutputTA = new JTextArea();
		initializeSimulationOutput ();
        simulationOutputTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        simulationOutputTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        // don't force an initial scroller such as simulationOutputTA.setRows(100);
        jsp = new JScrollPane(simulationOutputTA);
        bar = jsp.getVerticalScrollBar();
        bar.setUnitIncrement(STEPSIZE);

        JComponent vcrPanel = makeVCRPanel(skipCloseButton);

        Icon npsIcon = new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"));
        String npsString = "";

        npsLabel = new JLabel(npsString, npsIcon, JLabel.CENTER);
        npsLabel.setVerticalTextPosition(JLabel.TOP);
        npsLabel.setHorizontalTextPosition(JLabel.CENTER);
        npsLabel.setIconTextGap(50);

        int w = Integer.parseInt(ViskitConfiguration.instance().getValue(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfiguration.instance().getValue(ViskitConfiguration.APP_MAIN_BOUNDS_KEY + "[@h]"));

        leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, new JScrollPane(vcrPanel), npsLabel);
        leftSplit.setDividerLocation((h/2) - 50); // TODO check with Ben, else -1

        leftRightSplit.setLeftComponent(leftSplit);
        leftRightSplit.setRightComponent(jsp);
        leftRightSplit.setDividerLocation((w/2) - (w/4));

        add(leftRightSplit, BorderLayout.CENTER);

        // Provide access to Enable Analyst Report checkbox
        ViskitGlobals.instance().setRunPanel(SimulationRunPanel.this);
    }

    private JPanel makeVCRPanel(boolean skipCloseButton) {
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel vcrSimulationStartTimeLabel = new JLabel("Simulation start time: ");
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        // TODO:  can a user use this to advance to a certain time in the sim?
        vcrSimulationTime = new JTextField(10);
        vcrSimulationTime.setEditable(false);
        ViskitStatics.clampSize(vcrSimulationTime, vcrSimulationTime, vcrSimulationTime);
        JPanel vcrPanel = new JPanel();
        vcrPanel.setLayout(new BoxLayout(vcrPanel, BoxLayout.X_AXIS));
        vcrPanel.add(vcrSimulationStartTimeLabel);
        vcrPanel.add(vcrSimulationTime);
        vcrPanel.add(Box.createHorizontalStrut(10));
        flowPanel.add(vcrPanel);

        JLabel vcrSimulationStopTimeLabel = new JLabel("Simulation stop time: ");
        vcrSimulationStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        vcrStopTime = new JTextField(10);
        ViskitStatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);
        vcrPanel = new JPanel();
        vcrPanel.setLayout(new BoxLayout(vcrPanel, BoxLayout.X_AXIS));
        vcrPanel.add(vcrSimulationStopTimeLabel);
        vcrPanel.add(vcrStopTime);
        vcrPanel.add(Box.createHorizontalStrut(10));
        flowPanel.add(vcrPanel);

        numberOfReplicationsTF = new JTextField(10);
        numberOfReplicationsTF.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    int numReps = Integer.parseInt(numberOfReplicationsTF.getText().trim());
                    if (numReps < 1) {
                        numberOfReplicationsTF.setText("1");
                    }
                }
            });
        ViskitStatics.clampSize(numberOfReplicationsTF, numberOfReplicationsTF, numberOfReplicationsTF);
        JLabel numRepsLab = new JLabel("# replications: ");
        vcrPanel = new JPanel();
        vcrPanel.setLayout(new BoxLayout(vcrPanel, BoxLayout.X_AXIS));
        vcrPanel.add(numRepsLab);
        vcrPanel.add(numberOfReplicationsTF);
        vcrPanel.add(Box.createHorizontalStrut(10));
        flowPanel.add(vcrPanel);


        vcrVerbose = new JCheckBox("Verbose output", false);
        vcrVerbose.addActionListener(new vcrVerboseCBListener());
        vcrVerbose.setToolTipText("Enables verbose output for all runs");
        flowPanel.add(vcrVerbose);

        verboseRepNumberTF = new JTextField(7);
        VerboseReplicationNumberTFListener lis = new VerboseReplicationNumberTFListener();
        verboseRepNumberTF.addActionListener(lis);
        verboseRepNumberTF.addCaretListener(lis);
        ViskitStatics.clampSize(verboseRepNumberTF);
        verboseRepNumberTF.setToolTipText("Input a single replication run (1...n) to be verbose");
        flowPanel.add(verboseRepNumberTF);

        closeButt = new JButton("Close");
        closeButt.setToolTipText("Close this window");
        if (!skipCloseButton) {
            flowPanel.add(closeButt);
        }

        saveRepDataCB = new JCheckBox("Save replication data");
        saveRepDataCB.setToolTipText("If using only a SimplePropertyDumper, no need to check this");
        flowPanel.add(saveRepDataCB);
        printRepReportsCB = new JCheckBox("Print replication reports");
        flowPanel.add(printRepReportsCB);
        printSummReportsCB = new JCheckBox("Print summary reports");
        flowPanel.add(printSummReportsCB);

        /* DIFF between OA3302 branch and trunk */
        analystReportCB = new JCheckBox("Enable Analyst Reports");
        analystReportCB.setSelected(true);
        analystReportCB.setEnabled(assemblyRunPanelVisible);
        flowPanel.add(analystReportCB);

        // Initially, unselected
        resetSeedStateCB = new JCheckBox("Reset seed state each rerun");

        // TODO: Expose at a later time when we have usefullness for this
        resetSeedStateCB.setEnabled(false);
//        flowPan.add(resetSeedStateCB);
        /* End DIFF between OA3302 branch and trunk */

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        vcrStop = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStop.setToolTipText("Stop the simulation run");
        vcrStop.setEnabled(false);
        vcrStop.setBorder(BorderFactory.createEtchedBorder());
        vcrStop.setText(null);
        buttonPanel.add(vcrStop);

        vcrRewind = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewind.setToolTipText("Reset the simulation run");
        vcrRewind.setEnabled(false);
        vcrRewind.setBorder(BorderFactory.createEtchedBorder());
        vcrRewind.setText(null);
        if (!skipCloseButton) {
            buttonPanel.add(vcrRewind);
        }

        vcrPlay = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/Play24.gif")));
        vcrPlay.setToolTipText("Begin or resume the simulation run");
        if (skipCloseButton) {
            vcrPlay.setToolTipText("Begin the simulation run");
        }
        vcrPlay.setBorder(BorderFactory.createEtchedBorder());
        vcrPlay.setText(null);
        buttonPanel.add(vcrPlay);

        vcrStep = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrStep.setToolTipText("Step the simulation");
        vcrStep.setBorder(BorderFactory.createEtchedBorder());
        vcrStep.setText(null);
        if (!skipCloseButton) {
            buttonPanel.add(vcrStep);
        }

        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
          flowPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        flowPanel.setPreferredSize(new Dimension(vcrPlay.getPreferredSize()));

        flowPanel.add(buttonPanel);
        return flowPanel;
    }

    class vcrVerboseCBListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (vcrVerbose.isSelected()) {
                verboseRepNumberTF.setText("");
            }
        }
    }

    class VerboseReplicationNumberTFListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            if (!verboseRepNumberTF.getText().isEmpty()) {
                vcrVerbose.setSelected(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }
	public void setTitle (String newTitle)
	{
		String fullTitle = newTitle.trim();
		
		if      ( fullTitle.contains("Assembly") && !fullTitle.contains("Run"))
		 	 titleLabel.setText(newTitle + " Runner");
		else if (!fullTitle.contains("Assembly") &&  fullTitle.contains("Run"))
			 titleLabel.setText(newTitle + " Assembly");
		else if ( fullTitle.contains("Assembly") &&  fullTitle.contains("Run"))
			 titleLabel.setText(newTitle);
		else titleLabel.setText(newTitle + " Assembly Runner"); // whew
	}
	public final void initializeSimulationOutput ()
	{
		String initializationMessage;
		if (ViskitGlobals.instance().getAssemblyEditor().hasOpenModels())
		{
			initializationMessage = "Simulation output stream:" + lineEnd +
                                    "-------------------------" + lineEnd;
			simulationOutputTA.setEditable(true);
		}
		else if (ViskitGlobals.instance().getAssemblyEditor().hasOpenModels() && !ViskitGlobals.instance().getAssemblyController().isAssemblyReady())
		{
			initializationMessage = "Please initialize the selected Assembly before using the Simulation Run panel." + lineEnd +
                                    "------------------------------------------------------------------------------" + lineEnd;
			simulationOutputTA.setEditable(true);
		}
		else 
		{
			initializationMessage = "Please open/create and initialize an Assembly before using the Simulation Run panel." + lineEnd +
                                    "------------------------------------------------------------------------------------" + lineEnd;
			simulationOutputTA.setEditable(false);
		}
		simulationOutputTA.setText(initializationMessage);
	}
}
