package viskit;

import actions.ActionIntrospector;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 13, 2004
 * Time: 10:51:00 AM
 */

public class RunnerPanel extends JPanel
{
  String lineEnd = System.getProperty("line.separator");

  public JTextArea soutTA,serrTA;
  public JSplitPane splPn;

  public JButton vcrStop,vcrPlay,vcrRewind,vcrStep,closeButt;
  public JCheckBox vcrVerbose;

  public JTextField vcrSimTime,vcrStopTime;

  public RunnerPanel(boolean verbose)
  {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    soutTA = new JTextArea("Assembly output stream:"+lineEnd+
                           "----------------------"+lineEnd);
    soutTA.setEditable(false);
    soutTA.setFont(new Font("Monospaced",Font.PLAIN,12));
    soutTA.setBackground(new Color(0xFB,0xFB,0xE5));
    JScrollPane jsp = new JScrollPane(soutTA);

    serrTA = new JTextArea("Assembly error stream:"+lineEnd+
                           "---------------------"+lineEnd);
    serrTA.setForeground(Color.red);
    serrTA.setEditable(false);
    serrTA.setFont(new Font("Monospaced",Font.PLAIN,12));
    serrTA.setBackground(new Color(0xFB,0xFB,0xE5));
    JScrollPane jspErr = new JScrollPane(serrTA);

    splPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true,jsp,jspErr);

    add(splPn,BorderLayout.CENTER);
    add( makeVCRPanel(),BorderLayout.SOUTH);
    vcrVerbose.setSelected(verbose);

    setupPipes();
  }
  
  JPanel makeVCRPanel()
  {
    JPanel vcrToolBar = new JPanel();
    vcrToolBar.setLayout(new BoxLayout(vcrToolBar,BoxLayout.X_AXIS));
    vcrToolBar.add(Box.createHorizontalGlue());

    vcrStop = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/Stop24.gif")));
    vcrStop.setToolTipText("Stop the simulation run");
    vcrStop.setEnabled(false);
    vcrStop.setBorder(BorderFactory.createEtchedBorder());
    vcrStop.setText(null);
    vcrToolBar.add(vcrStop);

    vcrRewind = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/Rewind24.gif")));
    vcrRewind.setToolTipText("Rewind the simulation run");
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
    Vstatics.clampSize(vcrSimTime,vcrSimTime,vcrSimTime);

    vcrToolBar.add(vcrSimTimeLab);
    vcrToolBar.add(vcrSimTime);
    vcrToolBar.add(Box.createHorizontalStrut(10));

    JLabel vcrStopTimeLabel = new JLabel("Stop time:");
    vcrStopTime = new JTextField(10);
    Vstatics.clampSize(vcrStopTime,vcrStopTime,vcrStopTime);

    vcrToolBar.add(vcrStopTimeLabel);
    vcrToolBar.add(vcrStopTime);
    vcrToolBar.add(Box.createHorizontalStrut(10));

    vcrVerbose = new JCheckBox("verbose output",false);
    vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
    vcrToolBar.add(vcrVerbose);

    closeButt = new JButton("Close");
    closeButt.setToolTipText("Close this window");
    vcrToolBar.add(closeButt);

    vcrToolBar.add(Box.createHorizontalGlue());
    vcrToolBar.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    return vcrToolBar;
  }

  private void setupPipes()
  {
    System.setOut(new PrintStream(new BufferedOutputStream(new myPrintStr(soutTA))));
    System.setErr(new PrintStream(new BufferedOutputStream(new myPrintStr(serrTA))));
  }
  class myPrintStr extends OutputStream
  {
    JTextArea myTA;
    myPrintStr(JTextArea ta)
    {
      myTA = ta;
    }
    char ca[] = new char[1];
    public void write(int b) throws IOException
    {
      ca[0] = (char)b;
      new inSwingThread(myTA,new String(ca));
    }
  }
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
