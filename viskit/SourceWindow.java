package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

  public SourceWindow(JFrame main, String source)
  {
    this.main = main;
    this.src = source;

    Container con = getContentPane();

    con.setLayout(new BoxLayout(con,BoxLayout.Y_AXIS));

    JTextArea jta = new JTextArea(src);
    jta.setFont(new Font("Monospaced",Font.PLAIN,12));
    JScrollPane jsp = new JScrollPane(jta);
    con.add(jsp);

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
    buttPan.add(Box.createHorizontalGlue());

    JButton saveButt = new JButton("Save source and close");
    buttPan.add(saveButt);

    JButton closeButt = new JButton("Close");
    buttPan.add(closeButt);

    buttPan.add(Box.createHorizontalStrut(40));
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
