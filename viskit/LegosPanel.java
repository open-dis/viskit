package viskit;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:44:55 AM
 */

public class LegosPanel extends JPanel
{
  JButton plus,minus;
  LegosTree tree ;
  public LegosPanel(LegosTree ltree)
  {
    this.tree = ltree;
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    JLabel lab = new JLabel("Event Graphs");
    lab.setAlignmentX(Box.CENTER_ALIGNMENT);
    add(lab);
    JScrollPane jsp = new JScrollPane(tree);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    add(jsp);
      JPanel buttPan = new JPanel();
      buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
      buttPan.add(Box.createHorizontalGlue());
        plus = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/plus.png")));
        plus.setBorder(null);
        plus.setText(null);
        plus.setToolTipText("Add event graph class file or directory root to this list");
        Dimension dd = plus.getPreferredSize();
        plus.setMinimumSize(dd);
        plus.setMaximumSize(dd);
      buttPan.add(plus);
      buttPan.add(Box.createHorizontalStrut(10));

        minus = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minus.png")));
        minus.setDisabledIcon(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minusGrey.png")));
        minus.setBorder(null);
        minus.setText(null);
        minus.setToolTipText("Remove event graph class file or directory from this list");
        dd = minus.getPreferredSize();
        minus.setMinimumSize(dd);
        minus.setMaximumSize(dd);
        minus.setActionCommand("m");
        //minus.setEnabled(false);
      buttPan.add(minus);
      buttPan.add(Box.createHorizontalGlue());
    add(buttPan);
    minus.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        tree.removeSelected();
      }
    });

    plus.addActionListener( new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
          JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
          jfc.setFileFilter(new ClassTypeFilter());
          jfc.setAcceptAllFileFilterUsed(false);
          jfc.setMultiSelectionEnabled(true);
          jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

          int retv = jfc.showOpenDialog(LegosPanel.this);
          if (retv == JFileChooser.APPROVE_OPTION) {
            File[] fa = jfc.getSelectedFiles();
            for(int i=0;i<fa.length;i++) {
            Boolean recurse = null;
            if(fa[i].isDirectory()) {
              if(recurse == null) {
                int retrn = JOptionPane.showConfirmDialog(LegosPanel.this,"Recurse directories?","Question",JOptionPane.YES_OPTION,JOptionPane.QUESTION_MESSAGE);
                recurse = new Boolean(retrn==JOptionPane.YES_OPTION);
              }
              tree.addContentRoot(fa[i],recurse.booleanValue());
            }
            else
              tree.addContentRoot(fa[i]);
            }
          }
        }
    });
  }
  static class ClassTypeFilter extends FileFilter
  {
    public boolean accept(File f)
    {
      if(f.isFile() && !f.getName().endsWith(".class") && !f.getName().endsWith(".jar"))
        return false;
      return true;
    }

    public String getDescription()
    {
      return "Java class files, jar files or directories";
    }
  }

}
