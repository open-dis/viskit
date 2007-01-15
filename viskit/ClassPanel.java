package viskit;

import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:44:55 AM
 */

public class ClassPanel extends JPanel
{
  JButton plus, minus;
  LegosTree tree;
  JFileChooser jfc;

  public ClassPanel(LegosTree ltree, String title, String plusTT, String minusTT)
  {
    this.tree = ltree;
    jfc = new JFileChooser(System.getProperty("user.dir") + "/BehaviorLibraries");

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    JLabel lab = new JLabel(title); //"Event Graphs");
    lab.setAlignmentX(Box.CENTER_ALIGNMENT);
    add(lab);
    JScrollPane jsp = new JScrollPane(tree);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    add(jsp);
    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    buttPan.add(Box.createHorizontalGlue());
    plus = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/plus.png")));
    plus.setBorder(null);
    plus.setText(null);
    plus.setToolTipText(plusTT); //"Add event graph class file or directory root to this list");
    Dimension dd = plus.getPreferredSize();
    plus.setMinimumSize(dd);
    plus.setMaximumSize(dd);
    buttPan.add(plus);
    buttPan.add(Box.createHorizontalStrut(10));

    minus = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/minus.png")));
    minus.setDisabledIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/minusGrey.png")));
    minus.setBorder(null);
    minus.setText(null);
    minus.setToolTipText(minusTT); //"Remove event graph class file or directory from this list");
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

    plus.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        jfc.setFileFilter(new ClassTypeFilter(tree.getTargetClass()));
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.setMultiSelectionEnabled(true);
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        int retv = jfc.showOpenDialog(ClassPanel.this);
        if (retv == JFileChooser.APPROVE_OPTION) {
          File[] fa = jfc.getSelectedFiles();
          for (int i = 0; i < fa.length; i++) {
            Boolean recurse = null;
            if (fa[i].isDirectory()) {
              if (recurse == null) {
                int retrn = JOptionPane.showConfirmDialog(ClassPanel.this, "Recurse directories?", "Question", JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE);
                recurse = new Boolean(retrn == JOptionPane.YES_OPTION);
              }
              tree.addContentRoot(fa[i], recurse.booleanValue());
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
    private Class targetClass;     // looking for classes of this kind (or jars or directories)

    ClassTypeFilter(Class c)
    {
      this.targetClass = c;
    }

    public boolean accept(File f)
    {
      if (f.isFile()) {
        String lcnam = f.getName().toLowerCase();
        if (lcnam.endsWith(".jar"))
          return true;
        if (lcnam.endsWith(".xml"))
          return true;
        if (lcnam.endsWith(".class")) {
          Class fileClass = getClass(f);
          if (fileClass == null)
            return false;
          if (targetClass.isAssignableFrom(fileClass))
            return true;
        }
        return false;
      }
      return true;
    }

    Class getClass(File f)
    {
      Class c = null;
      try {
        c = FindClassesForInterface.classFromFile(f, c);
          
      }
      catch (Throwable e) {}

      // Here we don't show any classes that are reachable through the viskit classpath..i.e., simkit.jar
      // We want no dups there.

      if(c != null) {
        try {
          Class.forName(c.getName(),false,null);
          return null;         // this is the negative case
        }
        catch (ClassNotFoundException e) {}        // positive case
      }

      return c;
    }

    public String getDescription()
    {
      return "Java class files, xml files, jar files or directories";
    }
  }

}
