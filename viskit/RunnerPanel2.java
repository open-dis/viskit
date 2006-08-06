/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.MovesInstitute.org)
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
 * @since Jul 17, 2006
 * @since 3:17:07 PM
 */

package viskit;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.io.*;


/**
 * A VCR-controls and TextArea panel.  Hijacks System.out and System.err and displays them on its TextAreas.
 */
public class RunnerPanel2 extends JPanel
{
  String lineEnd = System.getProperty("line.separator");

  public JTextArea soutTA, serrTA;
  public JSplitPane xsplPn;

  public JButton vcrStop, vcrPlay, vcrRewind, vcrStep, closeButt;
  //public JButton saveParms;
  public JCheckBox vcrVerbose;

  public JTextField vcrSimTime, vcrStopTime;

  private InputStream outInpStr, errInpStr;
  public JCheckBox saveRepDataCB;
  public JCheckBox printRepReportsCB;
  public JCheckBox printSummReportsCB;
  public JTextField numRepsTF;

  public RunnerPanel2(boolean verbose, boolean skipCloseButt)
  {
    this(null,skipCloseButt);
    vcrVerbose.setSelected(verbose);
    piping=true;
    setupPipes();
  }

  public RunnerPanel2(String title, boolean skipCloseButt)
  {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    if(title != null) {
      JLabel titl = new JLabel(title);
      titl.setHorizontalAlignment(JLabel.CENTER);
      add(titl,BorderLayout.NORTH);
    }
    JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    JSplitPane leftSplit;
    JSplitPane rightSplit;

    soutTA = new JTextArea("Assembly output stream:" + lineEnd +
        "----------------------" + lineEnd);
    soutTA.setEditable(true); //false);
    soutTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
    soutTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
    JScrollPane jsp = new JScrollPane(soutTA);

    serrTA = new JTextArea("Assembly error stream:" + lineEnd +
        "---------------------" + lineEnd);
    serrTA.setForeground(Color.red);
    serrTA.setEditable(true); //false);
    serrTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
    serrTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
    JScrollPane jspErr = new JScrollPane(serrTA);

    rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, jsp,jspErr);

    JComponent vcrPanel = makeVCRPanel(skipCloseButt);

    leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,false,new JScrollPane(vcrPanel),
        new JLabel(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/NPS-3clr-PMS-vrt-type.png"))));

    leftRightSplit.setLeftComponent(leftSplit);
    leftRightSplit.setRightComponent(rightSplit);
    leftRightSplit.setDividerLocation(240);
    leftSplit.setDividerLocation(200);
    rightSplit.setDividerLocation(350);

    add(leftRightSplit,BorderLayout.CENTER);
  }

  public void setStreams(InputStream out, InputStream err)
  {
    outInpStr = out;
    errInpStr = err;
    setupPipes();
  }

  private JPanel makeVCRPanel(boolean skipCloseButt)
  {
    JPanel flowPan = new JPanel(new FlowLayout(FlowLayout.LEFT));

    JLabel vcrSimTimeLab = new JLabel("Sim start time:");
    // TODO:  is this start time or current time of sim?
    // TODO:  is this used elsewhere, or else can it simply be removed?
    vcrSimTime = new JTextField(10);
    vcrSimTime.setEditable(false);
    Vstatics.clampSize(vcrSimTime, vcrSimTime, vcrSimTime);
    JPanel labTF = new JPanel();
    labTF.setLayout(new BoxLayout(labTF,BoxLayout.X_AXIS));
    labTF.add(vcrSimTimeLab);
    labTF.add(vcrSimTime);
    labTF.add(Box.createHorizontalStrut(10));
    flowPan.add(labTF);

    JLabel vcrStopTimeLabel = new JLabel("Sim stop time:");
    vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
    vcrStopTime = new JTextField(10);
    Vstatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);
    labTF = new JPanel();
    labTF.setLayout(new BoxLayout(labTF,BoxLayout.X_AXIS));
    labTF.add(vcrStopTimeLabel);
    labTF.add(vcrStopTime);
    labTF.add(Box.createHorizontalStrut(10));
    flowPan.add(labTF);

    numRepsTF = new JTextField(10);
    Vstatics.clampSize(numRepsTF,numRepsTF,numRepsTF);
    JLabel numRepsLab = new JLabel("# replications:");
    labTF = new JPanel();
    labTF.setLayout(new BoxLayout(labTF,BoxLayout.X_AXIS));
    labTF.add(numRepsLab);
    labTF.add(numRepsTF);
    labTF.add(Box.createHorizontalStrut(10));
    flowPan.add(labTF);

    vcrVerbose = new JCheckBox("Verbose output", false);
    vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
    flowPan.add(vcrVerbose);

    closeButt = new JButton("Close");
    closeButt.setToolTipText("Close this window");
    if(!skipCloseButt) {
      flowPan.add(closeButt);
    }


    saveRepDataCB = new JCheckBox("Save replication data");
    flowPan.add(saveRepDataCB);
    printRepReportsCB = new JCheckBox("Print replication reports");
    flowPan.add(printRepReportsCB);
    printSummReportsCB = new JCheckBox("Print summary reports       ");
    flowPan.add(printSummReportsCB);

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));

    vcrStop = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Stop24.gif")));
    vcrStop.setToolTipText("Stop the simulation run");
    vcrStop.setEnabled(false);
    vcrStop.setBorder(BorderFactory.createEtchedBorder());
    vcrStop.setText(null);
    buttPan.add(vcrStop);

    vcrRewind = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Rewind24.gif")));
    vcrRewind.setToolTipText("Reset the simulation run");
    vcrRewind.setEnabled(false);
    vcrRewind.setBorder(BorderFactory.createEtchedBorder());
    vcrRewind.setText(null);
    if(!skipCloseButt)
      buttPan.add(vcrRewind);

    vcrPlay = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Play24.gif")));
    vcrPlay.setToolTipText("Begin or resume the simulation run");
    if(skipCloseButt)
      vcrPlay.setToolTipText("Begin the simulation run");
    vcrPlay.setBorder(BorderFactory.createEtchedBorder());
    vcrPlay.setText(null);
    buttPan.add(vcrPlay);

    vcrStep = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/StepForward24.gif")));
    vcrStep.setToolTipText("Step the simulation");
    vcrStep.setBorder(BorderFactory.createEtchedBorder());
    vcrStep.setText(null);
    if(!skipCloseButt)
      buttPan.add(vcrStep);

    buttPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    flowPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    flowPan.setPreferredSize(new Dimension(vcrPlay.getPreferredSize()));

    flowPan.add(buttPan);
    return flowPan;
  }

  private boolean piping=false;
  private void setupPipes()
  {
    if ((outInpStr != null) && (errInpStr != null)) {
      new ReaderThread(outInpStr, soutTA, true).start();
      new ReaderThread(errInpStr, serrTA, false).start();
    }
    else {
      try {
        PipedInputStream piOut = new PipedInputStream();
        PipedOutputStream poOut = new PipedOutputStream(piOut);
        System.setOut(new PrintStream(poOut, true));
        PipedInputStream piErr = new PipedInputStream();
        PipedOutputStream poErr = new PipedOutputStream(piErr);
        System.setErr(new PrintStream(poErr, true));

        new ReaderThread(piOut, soutTA, true).start();
        new ReaderThread(piErr, serrTA, false).start();
      }
      catch (IOException e) {
        JOptionPane.showMessageDialog(null, "IOException in setupPipes() " + e.getMessage());
      }
    }
  }

  class ReaderThread extends Thread
  {
    InputStream pi;
    JTextArea myTa;
    boolean clampSize;

    ReaderThread(InputStream pi, JTextArea ta , boolean clampSize)
    {
      super("rdrThr");
      this.pi = pi;
      this.myTa = ta;
      this.clampSize = clampSize;
    }

    public void run()
    {
      final byte[] buf = new byte[20*1024];
      PipedInputStream pis = new PipedInputStream();
      PipedOutputStream pos = null;
      try {
        pos = new PipedOutputStream(pis);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      byte[] ba = new byte[1];

      while (true) {
        int len = -1;
        try {
          len = pi.read(buf);
        }
        catch (IOException e) {
          // this happens when the thread that was the writer is dead.  Seems screwy when you
          // compare it to System.out, which doesn't care if anybody lives or dies.  The thread in
          // ExternalAssemblyRunner dies each time we exit Schedule, whether stepping or otherwise.
          // It's fine to just set up the I/O again and wait for the next time.

          if(piping)
            setupPipes();
          return;
        }
        if (len == -1) {
          if(piping)
            setupPipes();
          return;
        }

        // Write to the swing widget
        new inSwingThread(myTa, new String(buf, 0, len), pos);
        try {
          pis.read(ba);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

  StringBuffer stsb = new StringBuffer(); // we're single threaded
  /**
   * Class to encapsulate a packet containing a JTextArea reference and a string, and append the
   * string to the TextArea in the GUI thread.
   */
  class inSwingThread implements Runnable
  {
    JTextArea ta;
    String s;
    Document doc;
    PipedOutputStream pos;

    inSwingThread(JTextArea ta, String s, PipedOutputStream pos)
    {
      this.ta = ta;
      this.s = s;
      this.pos = pos;
      doc = ta.getDocument();
      SwingUtilities.invokeLater(this);
    }

    public void run()
    {
      if(clampSize) {
        stsb.append(s);
        if(stsb.length() > 0x100000) {
          stsb.delete(0,stsb.length()-0x100000-1);
        }
        ta.setText(stsb.toString());
      }
      else
        ta.append(s);

      ta.setCaretPosition(doc.getLength());

      if(pos != null)
        try {
          pos.write(1);
          pos.flush();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
    }
  }
  }

}
