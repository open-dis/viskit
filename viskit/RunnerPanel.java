package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

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

  public RunnerPanel(boolean verbose)
  {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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

    splPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, jsp, jspErr);

    add(splPn, BorderLayout.CENTER);
    add(makeVCRPanel(), BorderLayout.SOUTH);
    vcrVerbose.setSelected(verbose);

    setupPipes();
  }

  JPanel makeVCRPanel()
  {
    JPanel vcrToolBar = new JPanel();
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

    JLabel vcrSimTimeLab = new JLabel("Sim. time:");
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

    vcrVerbose = new JCheckBox("verbose output", false);
    vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
    vcrToolBar.add(vcrVerbose);

    closeButt = new JButton("Close");
    closeButt.setToolTipText("Close this window");
    vcrToolBar.add(closeButt);

    vcrToolBar.add(Box.createHorizontalGlue());
    vcrToolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    return vcrToolBar;
  }

  PipedInputStream piOut;
  PipedInputStream piErr;
  PipedOutputStream poOut;
  PipedOutputStream poErr;

  private void setupPipes()
  {
    try {
      piOut = new PipedInputStream();
      poOut = new PipedOutputStream(piOut);
      System.setOut(new PrintStream(poOut, true));
      piErr = new PipedInputStream();
      poErr = new PipedOutputStream(piErr);
      System.setErr(new PrintStream(poErr, true));
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    new ReaderThread(piOut, soutTA).start();
    new ReaderThread(piErr, serrTA).start();
  }

  class ReaderThread extends Thread
  {
    PipedInputStream pi;
    JTextArea myTa;

    ReaderThread(PipedInputStream pi, JTextArea ta)
    {
      this.pi = pi;
      this.myTa = ta;
    }

    public void run()
    {
      final byte[] buf = new byte[1024];

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

          setupPipes();
          return;
        }
        if (len == -1) {
          setupPipes();
          return;
        }

        // Write to the swing widget
        new inSwingThread(myTa, new String(buf, 0, len));
      }
    }
  }

  /**
   * Class to encapsulate a packet containing a JTextArea reference and a string, and append the
   * string to the TextArea in the GUI thread.
   */
  class inSwingThread implements Runnable
  {
    JTextArea ta;
    String s;

    inSwingThread(JTextArea ta, String s)
    {
      this.ta = ta;
      this.s = s;

      SwingUtilities.invokeLater(this);
    }

    public void run()
    {
      ta.append(s);
      ta.setCaretPosition(ta.getText().length());
    }
  }
}
