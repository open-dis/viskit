package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class SourceWindow extends JFrame
{
  JFrame main;
  final String src;

  Thread sysOutThread;
  JTextArea jta;
  public SourceWindow(JFrame main, String source)
  {
    this.main = main;
    this.src = source;

    JPanel outerPan = new JPanel(new BorderLayout());
    setContentPane(outerPan);
    JPanel con = new JPanel();
    outerPan.add(con,BorderLayout.CENTER);

    con.setLayout(new BoxLayout(con,BoxLayout.Y_AXIS));
    con.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    JToolBar tb = new JToolBar();
    JButton fontPlus = new JButton("Larger");
    JButton fontMinus=new JButton("Smaller");
    JButton printB = new JButton("Print");
    tb.add(new JLabel("Font:"));
    tb.add(fontPlus);
    tb.add(fontMinus);
    //tb.add(new JLabel("     "));
    tb.add(printB);
    fontPlus.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        jta.setFont(jta.getFont().deriveFont(jta.getFont().getSize2D()+1.0f));
      }
    });
    fontMinus.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        jta.setFont(jta.getFont().deriveFont(Math.max(jta.getFont().getSize2D()-1.0f,1.0f)));
      }
    });

    outerPan.add(tb,BorderLayout.NORTH);

    /*JTextArea*/ jta = new JTextArea(src);
    jta.setEditable(false);
    jta.setFont(new Font("Monospaced",Font.PLAIN,12));
    JScrollPane jsp = new JScrollPane(jta);
    con.add(jsp);

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
    buttPan.add(Box.createHorizontalGlue());

    JButton compileButt = new JButton("Compile test");
    buttPan.add(compileButt);

    JButton saveButt = new JButton("Save source and close");
    buttPan.add(saveButt);

    JButton closeButt = new JButton("Close");
    buttPan.add(closeButt);

    //buttPan.add(Box.createHorizontalStrut(40));
    con.add(buttPan);

    this.setSize(main.getWidth()-200,main.getHeight()-100);
    this.setLocationRelativeTo(main);

    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    closeButt.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        SourceWindow.this.dispose();
      }
    });

    compileButt.addActionListener( new ActionListener()
    {
      StringBuffer sb = new StringBuffer();
      BufferedReader br;
      String nl = System.getProperty("line.separator");
      public void actionPerformed(ActionEvent e)
      {
        PrintStream origSysOut = System.out;
         PipedOutputStream pos = new PipedOutputStream();
         PrintStream newSysOut = new PrintStream(pos);
         PipedInputStream pis = new PipedInputStream();
         try {pos.connect(pis);}catch (IOException e1) {JOptionPane.showMessageDialog(null,"bad pos.connect!");}
         br = new BufferedReader(new InputStreamReader(pis));

         sysOutThread = new Thread(new Runnable() {
           public void run()
           {
             try {
               String ln;
               while((ln = br.readLine()) != null)
               {
                 sb.append(ln);
                 sb.append(nl);
               }
             }
             catch (IOException e1) {
              // normal termination
             }
             try {br.close();}catch (IOException e1) {}
             sysOutThread = null;
          }
        });

        sysOutThread.start();

        System.setOut(newSysOut);
        System.setErr(newSysOut);

        AssemblyController.compileJavaClassFromStringAndHandleDependencies(src);
        
        newSysOut.flush();
        System.setOut(origSysOut);
        newSysOut.close();

        // We're on the Swing event thread here so this is slightly lousy:
        while(sysOutThread != null)
          Thread.yield();

        // Display the commpile results:
        sysOutDialog.showDialog(SourceWindow.this,SourceWindow.this,sb.toString(),getFileName());
      }
    });

    saveButt.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        JFileChooser jfc = new JFileChooser();
        String fn = getFileName();
        jfc.setSelectedFile(new File(fn));
        jfc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int ret =  jfc.showSaveDialog(SourceWindow.this);
        if(ret != JFileChooser.APPROVE_OPTION)
          return;
        File f = jfc.getSelectedFile();
        try {
          FileWriter fw = new FileWriter(f);
          fw.write(src);
          fw.close();
          SourceWindow.this.dispose();
        }
        catch (IOException ex) {
          JOptionPane.showMessageDialog(null,"Exception on source file write" +
                                     "\n"+ f.getName() +
                                     "\n"+ ex.getMessage(),
                                     "File I/O Error",JOptionPane.ERROR_MESSAGE);
        }

      }
    });
  }

  /**
   * Get the file name from the class statement
   * @return classname+".java"
   */
  private String getFileName()
  {
    String[] nm = src.split("\\bclass\\b"); // find the class, won't work if there is the word 'class' in top comments
    if(nm.length >=2) {
      nm = nm[1].split("\\b");            // find the space after the class
      int idx=0;
      while(idx < nm.length) {
        if(nm[idx] != null && nm[idx].trim().length()>0)
          return nm[idx].trim()+".java";
        idx++;
      }
    }
    return "unnamed.java";
  }
}

class sysOutDialog extends JDialog implements ActionListener
{
  private static sysOutDialog dialog;
  private static String value = "";
  private JList list;
  private JTextArea jta;
  private JScrollPane jsp;

  /**
   * Set up and show the dialog.  The first Component argument
   * determines which frame the dialog depends on; it should be
   * a component in the dialog's controlling frame. The second
   * Component argument should be null if you want the dialog
   * to come up with its left corner in the center of the screen;
   * otherwise, it should be the component on top of which the
   * dialog should appear.
   */
  public static String showDialog(Component frameComp,
                                  Component locationComp,
                                  String labelText,
                                  String title)
  {
    Frame frame = JOptionPane.getFrameForComponent(frameComp);
    dialog = new sysOutDialog(frame,
        locationComp,
        labelText,
        title);
    dialog.setVisible(true);
    return value;
  }

  private sysOutDialog(Frame frame,
                       Component locationComp,
                       String text,
                       String title)
  {
    super(frame, title, true);

    //Create and initialize the buttons.
    JButton cancelButton = new JButton("OK");
    cancelButton.addActionListener(this);
    getRootPane().setDefaultButton(cancelButton);

    //main part of the dialog
    jta = new JTextArea(text);
    jsp = new JScrollPane(jta);
    jsp.setPreferredSize(new Dimension(frame.getWidth()-50,frame.getHeight()-50));
    jsp.setAlignmentX(LEFT_ALIGNMENT);
    jsp.setBorder(BorderFactory.createEtchedBorder());
    //Create a container so that we can add a title around
    //the scroll pane.  Can't add a title directly to the
    //scroll pane because its background would be white.
    //Lay out the label and scroll pane from top to bottom.
    JPanel listPane = new JPanel();
    listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
    JLabel label = new JLabel("Compiler results");
    label.setLabelFor(list);
    listPane.add(label);
    listPane.add(Box.createRigidArea(new Dimension(0, 5)));
    listPane.add(jsp); //listScroller);
    listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    //Lay out the buttons from left to right.
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(cancelButton);

    //Put everything together, using the content pane's BorderLayout.
    Container contentPane = getContentPane();
    contentPane.add(listPane, BorderLayout.CENTER);
    contentPane.add(buttonPane, BorderLayout.PAGE_END);
    pack();
    setLocationRelativeTo(locationComp);
  }

  //Handle clicks on the Set and Cancel buttons.
  public void actionPerformed(ActionEvent e)
  {
    sysOutDialog.dialog.setVisible(false);
  }
}
