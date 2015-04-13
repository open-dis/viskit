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
package viskit.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.*;
import viskit.VGlobals;
import viskit.ViskitConfig;
import viskit.VStatics;

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
    public JButton vcrStop,  vcrPlay,  vcrRewind,  vcrStep,  closeButt;
    public JCheckBox vcrVerbose;
    public JTextField vcrSimTime,  vcrStopTime;
    public JCheckBox saveRepDataCB;
    public JCheckBox printRepReportsCB;
    public JCheckBox searchCB;
    public JDialog searchPopup;
    public JCheckBox printSummReportsCB;
    public JCheckBox resetSeedCB;
    public JCheckBox analystReportCB;
    public JTextField numRepsTF;
    public JScrollBar bar;
    public JTextField verboseRepNumberTF;
    public JLabel npsLabel;

    private final int STEPSIZE = 100; // adjusts speed of top/bottom scroll arrows
    private JLabel titl;
    private boolean aRPanelVisible;

    /**
     * Create an Assembly Runner panel
     * @param title the title of this panel
     * @param skipCloseButt if ture, don't supply rewind or pause buttons on VCR,
     * not hooked up, or working right.  A false will enable all VCR buttons.
     * Currently, only start and stop work
     * @param aRPanelVisible if true, will enable the analyst report check box
     */
    public RunnerPanel2(String title, boolean skipCloseButt, boolean aRPanelVisible) {
        this.aRPanelVisible = aRPanelVisible;
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
        soutTA.setEditable(true); //false);
        soutTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        soutTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        // don't force an initial scroller soutTA.setRows(100);
        jsp = new JScrollPane(soutTA);
        bar = jsp.getVerticalScrollBar();
        bar.setUnitIncrement(STEPSIZE);

        JComponent vcrPanel = makeVCRPanel(skipCloseButt);

        Icon npsIcon = new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"));
        String npsString = "";

        npsLabel = new JLabel(npsString, npsIcon, JLabel.CENTER);
        npsLabel.setVerticalTextPosition(JLabel.TOP);
        npsLabel.setHorizontalTextPosition(JLabel.CENTER);
        npsLabel.setIconTextGap(50);

        int w = Integer.parseInt(ViskitConfig.instance().getVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@w]"));
        int h = Integer.parseInt(ViskitConfig.instance().getVal(ViskitConfig.APP_MAIN_BOUNDS_KEY + "[@h]"));

        leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, new JScrollPane(vcrPanel), npsLabel);
        leftSplit.setDividerLocation((h/2) - 25);

        leftRightSplit.setLeftComponent(leftSplit);
        leftRightSplit.setRightComponent(jsp);
        leftRightSplit.setDividerLocation((w/2) - (w/4));

        add(leftRightSplit, BorderLayout.CENTER);

        // Provide access to Enable Analyst Report checkbox
        VGlobals.instance().setRunPanel(RunnerPanel2.this);
    }

    private JPanel makeVCRPanel(boolean skipCloseButt) {
        JPanel flowPan = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel vcrSimTimeLab = new JLabel("Sim start time: ");
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        // TODO:  can a user use this to advance to a certain time in the sim?
        vcrSimTime = new JTextField(10);
        vcrSimTime.setEditable(false);
        VStatics.clampSize(vcrSimTime, vcrSimTime, vcrSimTime);
        JPanel labTF = new JPanel();
        labTF.setLayout(new BoxLayout(labTF, BoxLayout.X_AXIS));
        labTF.add(vcrSimTimeLab);
        labTF.add(vcrSimTime);
        labTF.add(Box.createHorizontalStrut(10));
        flowPan.add(labTF);

        JLabel vcrStopTimeLabel = new JLabel("Sim stop time: ");
        vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        vcrStopTime = new JTextField(10);
        VStatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);
        labTF = new JPanel();
        labTF.setLayout(new BoxLayout(labTF, BoxLayout.X_AXIS));
        labTF.add(vcrStopTimeLabel);
        labTF.add(vcrStopTime);
        labTF.add(Box.createHorizontalStrut(10));
        flowPan.add(labTF);

        numRepsTF = new JTextField(10);
        numRepsTF.addActionListener(
            new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    int numReps = Integer.parseInt(numRepsTF.getText().trim());
                    if (numReps < 1) {
                        numRepsTF.setText("1");
                    }
                }
            });
        VStatics.clampSize(numRepsTF, numRepsTF, numRepsTF);
        JLabel numRepsLab = new JLabel("# replications: ");
        labTF = new JPanel();
        labTF.setLayout(new BoxLayout(labTF, BoxLayout.X_AXIS));
        labTF.add(numRepsLab);
        labTF.add(numRepsTF);
        labTF.add(Box.createHorizontalStrut(10));
        flowPan.add(labTF);

        vcrVerbose = new JCheckBox("Verbose output", false);
        vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
        flowPan.add(vcrVerbose);

        verboseRepNumberTF = new JTextField(7);
        VStatics.clampSize(verboseRepNumberTF);
        verboseRepNumberTF.setToolTipText("Input a single replication number (1...n) to be verbose");
        flowPan.add(verboseRepNumberTF);

        closeButt = new JButton("Close");
        closeButt.setToolTipText("Close this window");
        if (!skipCloseButt) {
            flowPan.add(closeButt);
        }

        saveRepDataCB = new JCheckBox("Save replication data");
        flowPan.add(saveRepDataCB);
        printRepReportsCB = new JCheckBox("Print replication reports");
        flowPan.add(printRepReportsCB);
        printSummReportsCB = new JCheckBox("Print summary reports");
        flowPan.add(printSummReportsCB);

        /* DIFF between OA3302 branch and trunk */
        analystReportCB = new JCheckBox("Enable Analyst Reports");
        analystReportCB.setEnabled(aRPanelVisible);
        flowPan.add(analystReportCB);

//        resetSeedCB = new JCheckBox("Reset seed each rerun");
//        flowPan.add(resetSeedCB);
        /* End DIFF between OA3302 branch and trunk */

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));

        vcrStop = new JButton(new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStop.setToolTipText("Stop the simulation run");
        vcrStop.setEnabled(false);
        vcrStop.setBorder(BorderFactory.createEtchedBorder());
        vcrStop.setText(null);
        buttPan.add(vcrStop);

        vcrRewind = new JButton(new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewind.setToolTipText("Reset the simulation run");
        vcrRewind.setEnabled(false);
        vcrRewind.setBorder(BorderFactory.createEtchedBorder());
        vcrRewind.setText(null);
        if (!skipCloseButt) {
            buttPan.add(vcrRewind);
        }

        vcrPlay = new JButton(new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/Play24.gif")));
        vcrPlay.setToolTipText("Begin or resume the simulation run");
        if (skipCloseButt) {
            vcrPlay.setToolTipText("Begin the simulation run");
        }
        vcrPlay.setBorder(BorderFactory.createEtchedBorder());
        vcrPlay.setText(null);
        buttPan.add(vcrPlay);

        vcrStep = new JButton(new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrStep.setToolTipText("Step the simulation");
        vcrStep.setBorder(BorderFactory.createEtchedBorder());
        vcrStep.setText(null);
        if (!skipCloseButt) {
            buttPan.add(vcrStep);
        }

        buttPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        flowPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        flowPan.setPreferredSize(new Dimension(vcrPlay.getPreferredSize()));

        flowPan.add(buttPan);
        return flowPan;
    }
}
