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

import edu.nps.util.LogUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitStatics;
import viskit.control.InternalAssemblyRunner;

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
    static final Logger LOG = LogUtilities.getLogger(SimulationRunPanel.class);

    public boolean dump = true;
    public boolean search;
    public String lineEnd = System.getProperty("line.separator");
    public JScrollPane jsp;
    public JTextArea simulationOutputTA;
    public JSplitPane xsplPn;
    public JButton vcrStop,  vcrPlay,  vcrRewind,  vcrStep,  closeButton;
    public JCheckBox vcrVerboseCB;
    public JTextField vcrSimulationStartTimeTF,  vcrSimulationStopTimeTF;
    public JCheckBox saveReplicationDataCB;
    public JCheckBox printReplicationReportsCB;
    public JCheckBox searchCB;
    public JDialog searchPopup;
    public JCheckBox printSummaryReportsCB;
    public JCheckBox resetSeedStateCB;
    public JCheckBox analystReportCB;
    public JTextField numberOfReplicationsTF;
    public JScrollBar bar;
    public JTextField verboseReplicationNumberTF;
    public JLabel npsLabel;

    private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
	private final JLabel titleLabel = new JLabel();
    private final boolean simulationRunPanelVisible;
	private int numberOfReplications = 1;
	private String fullTitle = new String();

    /**
     * Create an Simulation Run panel
     * @param title the title of this panel
     * @param skipCloseButton if ture, don't supply rewind or pause buttons on VCR,
     * not hooked up, or working right.  A false will enable all VCR buttons.
     * Currently, only start and stop work
     * @param simulationRunPanelPanelVisible if true, will enable the analyst report check box
     */
    public SimulationRunPanel(String title, boolean skipCloseButton, boolean simulationRunPanelPanelVisible) {
        this.simulationRunPanelVisible = simulationRunPanelPanelVisible;
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
        simulationOutputTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        simulationOutputTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        // don't force an initial scroller such as simulationOutputTA.setRows(100);
        jsp = new JScrollPane(simulationOutputTA);
        bar = jsp.getVerticalScrollBar();
        bar.setUnitIncrement(STEPSIZE);

        JComponent vcrPanel = buildSimulationRunControlPanel(skipCloseButton);

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
        leftRightSplit.setDividerLocation(275); // (w/2) - (w/4));

        add(leftRightSplit, BorderLayout.CENTER);
		
		initializeRunWidgetsSimulationOutputTA ();

        // Provide access to Enable Analyst Report checkbox etc.
        ViskitGlobals.instance().setSimulationRunPanel(SimulationRunPanel.this);
    }

    private JPanel buildSimulationRunControlPanel(boolean skipCloseButton)
	{
        JPanel simulationRunControlPanel = new JPanel();
		simulationRunControlPanel.setLayout(new BoxLayout(simulationRunControlPanel, BoxLayout.Y_AXIS));
		simulationRunControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		simulationRunControlPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel vcrPanel = new JPanel();
        vcrPanel.setLayout(new BoxLayout(vcrPanel, BoxLayout.X_AXIS));
		vcrPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		vcrPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		
        JLabel vcrSimulationStartTimeLabel = new JLabel("Simulation start time: ");
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        // TODO:  can a user use this to advance to a certain time in the sim?
        vcrSimulationStartTimeTF = new JTextField(10);
        vcrSimulationStartTimeTF.setEditable(false);
        ViskitStatics.clampSize(vcrSimulationStartTimeTF);
        vcrPanel.add(vcrSimulationStartTimeLabel);
        vcrPanel.add(Box.createHorizontalStrut(5));
        vcrPanel.add(vcrSimulationStartTimeTF);
        simulationRunControlPanel.add(vcrPanel);
		simulationRunControlPanel.add(Box.createVerticalStrut(5));

        JLabel vcrSimulationStopTimeLabel = new JLabel("Simulation stop time: ");
        vcrSimulationStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        vcrSimulationStopTimeTF = new JTextField(10);
        ViskitStatics.clampSize(vcrSimulationStopTimeTF);
        vcrPanel = new JPanel(); // reset
		vcrPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        vcrPanel.setLayout(new BoxLayout(vcrPanel, BoxLayout.X_AXIS));
        vcrPanel.add(vcrSimulationStopTimeLabel);
        vcrPanel.add(Box.createHorizontalStrut(5));
        vcrPanel.add(vcrSimulationStopTimeTF);
        simulationRunControlPanel.add(vcrPanel); // updated
		simulationRunControlPanel.add(Box.createVerticalStrut(5));

        numberOfReplicationsTF = new JTextField(10);
        numberOfReplicationsTF.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setNumberOfReplications(Integer.parseInt(numberOfReplicationsTF.getText().trim()));
                    if (getNumberOfReplications() < 1)
					{
						setNumberOfReplications(1);
                        numberOfReplicationsTF.setText("1");
                    }
                }
            });
        ViskitStatics.clampSize(numberOfReplicationsTF);
        JLabel numberOfReplicationsLabel = new JLabel("   Total # replications: ");
        vcrPanel = new JPanel(); // reset
        vcrPanel.setLayout(new BoxLayout(vcrPanel, BoxLayout.X_AXIS));
		vcrPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        vcrPanel.add(numberOfReplicationsLabel);
        vcrPanel.add(Box.createHorizontalStrut(5));
        vcrPanel.add(numberOfReplicationsTF);
        simulationRunControlPanel.add(vcrPanel); // updated
		simulationRunControlPanel.add(Box.createVerticalStrut(5));
		
        vcrVerboseCB = new JCheckBox("Verbose output", true); // TODO user preference
        vcrVerboseCB.addActionListener(new VcrVerboseCBListener());
        vcrVerboseCB.setToolTipText("Enables verbose output for all runs");
        simulationRunControlPanel.add(vcrVerboseCB); // updated
		simulationRunControlPanel.add(Box.createVerticalStrut(5));
		
		JPanel singleVerboseReplicationPanel = new JPanel();
//		singleVerboseReplicationPanel.setLayout(new FlowLayout());
        singleVerboseReplicationPanel.setLayout(new BoxLayout(singleVerboseReplicationPanel, BoxLayout.X_AXIS));
		singleVerboseReplicationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		singleVerboseReplicationPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        JLabel singleVerboseReplicationLabel = new JLabel("     Single verbose replication #: ");
        verboseReplicationNumberTF           = new JTextField(5);
        ViskitStatics.clampSize(verboseReplicationNumberTF);
		singleVerboseReplicationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		verboseReplicationNumberTF.setAlignmentX(Component.LEFT_ALIGNMENT);
        VerboseReplicationNumberTFListener listener = new VerboseReplicationNumberTFListener();
        verboseReplicationNumberTF.addActionListener(listener);
        verboseReplicationNumberTF.addCaretListener (listener);
		String verboseReplicationHint = "Input a single replication run (1...n) that will have verbose output";
        verboseReplicationNumberTF.setToolTipText(verboseReplicationHint);
        verboseReplicationNumberTF.setToolTipText(verboseReplicationHint);
        singleVerboseReplicationPanel.add(singleVerboseReplicationLabel);
        singleVerboseReplicationPanel.add(verboseReplicationNumberTF);
		simulationRunControlPanel.add(singleVerboseReplicationPanel);
		simulationRunControlPanel.add(Box.createVerticalStrut(5));

        closeButton = new JButton("Close");
        closeButton.setToolTipText("Close this window");
        if (!skipCloseButton) {
            simulationRunControlPanel.add(closeButton);
			simulationRunControlPanel.add(Box.createVerticalStrut(5));
        }

        saveReplicationDataCB = new JCheckBox("Save replication data");
        saveReplicationDataCB.setToolTipText("If using only a SimplePropertyDumper, no need to check this");
        simulationRunControlPanel.add(saveReplicationDataCB);
		simulationRunControlPanel.add(Box.createVerticalStrut(5));
        printReplicationReportsCB = new JCheckBox("Print replication reports");
        simulationRunControlPanel.add(printReplicationReportsCB);
		simulationRunControlPanel.add(Box.createVerticalStrut(5));
        printSummaryReportsCB = new JCheckBox("Print summary reports");
        simulationRunControlPanel.add(printSummaryReportsCB);
		simulationRunControlPanel.add(Box.createVerticalStrut(5));

        /* DIFF between OA3302 branch and trunk */
        analystReportCB = new JCheckBox("Enable analyst report");
        analystReportCB.setSelected(true);
        analystReportCB.setEnabled(simulationRunPanelVisible);
        simulationRunControlPanel.add(analystReportCB);
        simulationRunControlPanel.add(Box.createVerticalStrut(5));
		
        resetSeedStateCB = new JCheckBox("Reset seed state each rerun");
        resetSeedStateCB.setEnabled(false); // Initially, unselected
//      flowPan.add(resetSeedStateCB);// TODO: Expose at a later time when we have ususability for this
//		flowPanel.add(Box.createVerticalStrut(5));
        /* End DIFF between OA3302 branch and trunk */

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        vcrStop = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStop.setToolTipText("Stop the simulation run");
        vcrStop.setEnabled(false);
        vcrStop.setBorder(BorderFactory.createEtchedBorder());
//        vcrStop.setText("Stop");
        buttonPanel.add(vcrStop);

        vcrRewind = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewind.setToolTipText("Reset the simulation run");
        vcrRewind.setEnabled(false);
        vcrRewind.setBorder(BorderFactory.createEtchedBorder());
//        vcrRewind.setText("Rewind");
//        buttonPanel.add(vcrRewind);

        vcrPlay = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/Play24.gif")));
        vcrPlay.setToolTipText("Begin or resume the simulation run");
        if (skipCloseButton) {
            vcrPlay.setToolTipText("Begin the simulation run");
        }
        vcrPlay.setBorder(BorderFactory.createEtchedBorder());
//        vcrPlay.setText("Play");
        buttonPanel.add(vcrPlay);

        vcrStep = new JButton(new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrStep.setToolTipText("Step the simulation");
        vcrStep.setBorder(BorderFactory.createEtchedBorder());
//        vcrStep.setEnabled (!skipCloseButton);
//		buttonPanel.add(vcrStep);
        simulationRunControlPanel.add(buttonPanel);
		
        return simulationRunControlPanel;
    }

	/**
	 * @return the numberOfReplications
	 */
	public int getNumberOfReplications()
	{
		numberOfReplications = Integer.parseInt(numberOfReplicationsTF.getText().trim()); // ensure value is captured from TextField
		return numberOfReplications;
	}

	/**
	 * @param numberOfReplications the numberOfReplications to set
	 */
	public void setNumberOfReplications(int numberOfReplications) 
	{
		numberOfReplicationsTF.setText(Integer.toString(numberOfReplications));
		this.numberOfReplications = numberOfReplications;
	}

    class VcrVerboseCBListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (vcrVerboseCB.isSelected())
			{
				// verbose and single-verbose-replication are mutually exclusive
                verboseReplicationNumberTF.setText(""); //TODO consider layout refactor, radio buttons
            }
        }
    }

    class VerboseReplicationNumberTFListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            if (!verboseReplicationNumberTF.getText().isEmpty()) {
                vcrVerboseCB.setSelected(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }
	public final void setTitle (String newTitle)
	{
		fullTitle = newTitle.trim();
		// adjust title for readability
		if      ( fullTitle.contains("Simulation") && !fullTitle.contains("Run"))
		 	 fullTitle += " Runner";
		else if (!fullTitle.contains("Simulation") &&  fullTitle.contains("Run"))
			 fullTitle += " Assembly";
		else if ( fullTitle.contains("Simulation") &&  fullTitle.contains("Run"))
		{
			// OK as is
		}
		else fullTitle += " Simulation Run"; // whew
		
		titleLabel.setText(fullTitle);
	}
	public final void initializeRunWidgetsSimulationOutputTA ()
	{
		boolean ready = false;
		String initializationMessage;
		if      (ViskitGlobals.instance().getAssemblyEditViewFrame().hasActiveAssembly() && !ViskitGlobals.instance().getAssemblyController().isAssemblyReady())
		{
			initializationMessage = "**********************************************************************************" + lineEnd +
                                    "* Not ready! Initialize the selected Assembly before using Simulation Run panel. *" + lineEnd +
                                    "**********************************************************************************" + lineEnd;
		}
		else if (ViskitGlobals.instance().getAssemblyEditViewFrame().hasActiveAssembly())
		{
			initializationMessage = "Simulation output stream";
			if ((fullTitle != null) && !fullTitle.isEmpty())
			{
				initializationMessage += " for " + fullTitle; // TODO fix initialization sequence
			}
			initializationMessage += lineEnd +
                                     InternalAssemblyRunner.horizontalRuleEqualSigns + lineEnd +
                                     InternalAssemblyRunner.horizontalRuleEqualSigns + lineEnd;
			ready = true;
		}
		else 
		{
			initializationMessage = "************************************************************************************" + lineEnd +
                                    "* Not ready! Open/create/initialize an Assembly before using Simulation Run panel. *" + lineEnd +
                                    "************************************************************************************" + lineEnd;
		}
		// adjust panel widgets
				simulationOutputTA.setText(initializationMessage);
				simulationOutputTA.setEditable(ready);
			numberOfReplicationsTF.setEnabled(ready);
		verboseReplicationNumberTF.setEnabled(ready);
		  vcrSimulationStartTimeTF.setEnabled(ready);
	       vcrSimulationStopTimeTF.setEnabled(ready);
	}
}
