package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Apr 23, 2004
 * Time: 3:17:23 PM
 */
public class RunWindow extends JDialog
{
  JFrame main;
  AssemblyRunner runner;

  JTextArea soutTA,serrTA;
  JSplitPane splPn;
  String lineEnd = System.getProperty("line.separator");

  public RunWindow(JFrame main, AssemblyRunner runner)
  {
    this.main = main;
    this.runner = runner;
    setTitle("Executing "+runner.getClassName());
    setModal(true);

    JPanel pan = new JPanel(new BorderLayout());
    pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    setContentPane(pan);
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

    pan.add(splPn,BorderLayout.CENTER);

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
    buttPan.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
    buttPan.add(Box.createHorizontalGlue());

    JButton closeButt = new JButton("Close");
    buttPan.add(closeButt);

    buttPan.add(Box.createHorizontalStrut(40));
    pan.add(buttPan,BorderLayout.SOUTH);

    this.setSize(main.getWidth()-200,main.getHeight()-100);
    this.setLocationRelativeTo(main);

    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    closeButt.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // Done by dispose RunWindow.this.setVisible(false);
        RunWindow.this.dispose();
      }
    });
    
  }
  serrWriter serrW;
  soutWriter soutW;
  public void setVisible(boolean b)
  {
    if(b) {
      serrW = new serrWriter(runner,serrTA);
      soutW = new soutWriter(runner,soutTA);
      serrW.go();
      soutW.go();

      runner.runAssembly();
    }
    else {
      serrW.kill();
      soutW.kill();
    }
    SwingUtilities.invokeLater(new Runnable(){public void run() {
      splPn.setDividerLocation(0.85d); }});         // do it this way to make it have an effect

    super.setVisible(b);       // blocks here
  }

  class windowWriter implements Runnable
  {
    BufferedReader br=null;
    JTextArea ta;
    windowWriter(JTextArea outTA, Reader rdr)
    {
      ta = outTA;
      br = new BufferedReader(rdr);
    }
    Thread me;
    public void go()
    {
      me = new Thread(this);
      me.start();
    }
    public void kill()
    {
      if(me == null)
        return;

      // else....what to do here
    }
    public void run()
    {
      //System.out.println("into windowWriter");
      try {
        while(true) {
          String s = br.readLine();
          new inSwingThread(s);
        }
      }
      catch (IOException e) {}
      //System.out.println("out of windowWriter");
      new inSwingThread(/*lineEnd + */"End of Run." + lineEnd);
      me = null;
    }
    class inSwingThread implements Runnable
    {
      String s = null;
      inSwingThread(String s)
      {
        this.s = s;
        SwingUtilities.invokeLater(this);
      }
      public void run()
      {
        ta.append(s+"\n");
        ta.setCaretPosition(ta.getText().length());
      }
    }
  }
  class soutWriter extends windowWriter
  {
    soutWriter(AssemblyRunner ar, JTextArea ta)
    {
      super(ta,ar.getSysOutReader());
    }
  }
  class serrWriter extends windowWriter
  {
    serrWriter(AssemblyRunner ar, JTextArea ta)
    {
      super(ta,ar.getSysErrReader());
    }
  }

}
