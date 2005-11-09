package viskit;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.io.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 13, 2004
 * Time: 10:51:00 AM
 */

/**
 * A VCR-controls and TextArea panel.  Hijacks System.out and System.err and displays them on its TextAreas.
 */
public class RunnerPanel extends JPanel
{
  String lineEnd = System.getProperty("line.separator");

  public JTextArea soutTA, serrTA;
  public JSplitPane splPn;

  public JButton vcrStop, vcrPlay, vcrRewind, vcrStep, closeButt;
  public JCheckBox vcrVerbose;

  public JTextField vcrSimTime, vcrStopTime;

  private InputStream outInpStr, errInpStr;
  JPanel vcrToolBar;

  public RunnerPanel(boolean verbose, boolean skipCloseButt)
  {
    this(null,skipCloseButt);
    vcrVerbose.setSelected(verbose);
    piping=true;
    setupPipes();
  }

  public RunnerPanel(String title, boolean skipCloseButt)
  {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    if(title != null) {
      JLabel titl = new JLabel(title);
      titl.setHorizontalAlignment(JLabel.CENTER);
      add(titl,BorderLayout.NORTH);
    }
    soutTA = new JTextArea("Assembly output stream:" + lineEnd +
        "----------------------" + lineEnd);
    soutTA.setEditable(true); //false);
    soutTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
    soutTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
    JScrollPane jsp = new JScrollPane(soutTA);
    jsp.setPreferredSize(new Dimension(10,350));   // give it some height for the initial split

    serrTA = new JTextArea("Assembly error stream:" + lineEnd +
        "---------------------" + lineEnd);
    serrTA.setForeground(Color.red);
    serrTA.setEditable(true); //false);
    serrTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
    serrTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
    JScrollPane jspErr = new JScrollPane(serrTA);

    splPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false,jsp,jspErr);
    add(splPn, BorderLayout.CENTER);
    add(makeVCRPanel(skipCloseButt), BorderLayout.SOUTH);
  }

  public void setStreams(InputStream out, InputStream err)
  {
    outInpStr = out;
    errInpStr = err;
    setupPipes();
  }

  JPanel makeVCRPanel(boolean skipCloseButt)
  {
    vcrToolBar = new JPanel();
    vcrToolBar.setLayout(new BoxLayout(vcrToolBar, BoxLayout.X_AXIS));
    vcrToolBar.add(Box.createHorizontalGlue());

    vcrStop = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/Stop24.gif")));
    vcrStop.setToolTipText("Stop the simulation run");
    vcrStop.setEnabled(false);
    vcrStop.setBorder(BorderFactory.createEtchedBorder());
    vcrStop.setText(null);
    vcrToolBar.add(vcrStop);

    vcrRewind = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/Rewind24.gif")));
    vcrRewind.setToolTipText("Reset the simulation run");
    vcrRewind.setEnabled(false);
    vcrRewind.setBorder(BorderFactory.createEtchedBorder());
    vcrRewind.setText(null);
    vcrToolBar.add(vcrRewind);

    vcrPlay = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/Play24.gif")));
    vcrPlay.setToolTipText("Begin or resume the simulation run");
    vcrPlay.setBorder(BorderFactory.createEtchedBorder());
    vcrPlay.setText(null);
    vcrToolBar.add(vcrPlay);

    vcrStep = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/StepForward24.gif")));
    vcrStep.setToolTipText("Step the simulation");
    vcrStep.setBorder(BorderFactory.createEtchedBorder());
    vcrStep.setText(null);
    vcrToolBar.add(vcrStep);

    vcrToolBar.add(Box.createHorizontalStrut(20));

    JLabel vcrSimTimeLab = new JLabel("Simulation time:");
    vcrSimTime = new JTextField(10);
    vcrSimTime.setEditable(false);
    Vstatics.clampSize(vcrSimTime, vcrSimTime, vcrSimTime);

    vcrToolBar.add(vcrSimTimeLab);
    vcrToolBar.add(vcrSimTime);
    vcrToolBar.add(Box.createHorizontalStrut(10));

    JLabel vcrStopTimeLabel = new JLabel("Stop time:");
    vcrStopTime = new JTextField(10);
    Vstatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);

    vcrToolBar.add(vcrStopTimeLabel);
    vcrToolBar.add(vcrStopTime);
    vcrToolBar.add(Box.createHorizontalStrut(10));

    vcrVerbose = new JCheckBox("Verbose output", false);
    vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
    vcrToolBar.add(vcrVerbose);
    vcrToolBar.add(Box.createHorizontalStrut(5));

    closeButt = new JButton("Close");
    closeButt.setToolTipText("Close this window");
    if(!skipCloseButt) {
      vcrToolBar.add(closeButt);
    }
    vcrToolBar.add(Box.createHorizontalGlue());
    vcrToolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    return vcrToolBar;
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
        JOptionPane.showMessageDialog(null, "exep in setupPipes " + e.getMessage());
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
        if(stsb.length() > 0x8000) {
          stsb.delete(0,stsb.length()-0x8000-1);
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
