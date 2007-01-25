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
 * @since Nov 2, 2005
 * @since 11:24:06 AM
 */

package viskit;

import org.apache.commons.configuration.XMLConfiguration;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import viskit.doe.LocalBootLoader;

public class SettingsDialog extends JDialog
{
  private static SettingsDialog dialog;
  private static boolean modified = false;
  private JFrame mother;
  private JButton canButt;
  private JButton okButt;
  private JTabbedPane tabbedPane;
  private JList classPathJlist;

  public static boolean showDialog(JFrame mother)
  {
    if (dialog == null)
      dialog = new SettingsDialog(mother);
    else
      dialog.setParams();
    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  private SettingsDialog(JFrame mother)
  {
    super(mother, "Viskit Application Settings", true);
    this.mother = mother;

    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());
    initConfig();

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    content.setBorder(new EmptyBorder(10,10,10,10));
    setContentPane(content);

    tabbedPane = new JTabbedPane();
    buildWidgets();

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Close");
    buttPan.add(Box.createHorizontalGlue());
    //buttPan.add(canButt);
    buttPan.add(okButt);
    //buttPan.add(Box.createHorizontalGlue());

    content.add(tabbedPane);
    content.add(Box.createVerticalStrut(5));
    content.add(buttPan);

    fillWidgets();     // put the data into the widgets

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    Dimension d = getSize();
    d.width = Math.max(d.width,600);
    setSize(d);
    this.setLocationRelativeTo(mother);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());
  }

  private void setParams()
  {
    fillWidgets();

    modified = false;

    pack();
    Dimension d = getSize();
    d.width = Math.max(d.width,600);
    setSize(d);
    this.setLocationRelativeTo(mother);
  }

  private void buildWidgets()
  {
    JPanel classpathP = new JPanel();
    classpathP.setLayout(new BoxLayout(classpathP,BoxLayout.Y_AXIS));
    classPathJlist = new JList(new DefaultListModel());
    classpathP.add(new JScrollPane(classPathJlist));
    JPanel bPan = new JPanel();
    bPan.setLayout(new BoxLayout(bPan,BoxLayout.X_AXIS));
    bPan.add(Box.createHorizontalGlue());
    JButton upCPButt = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/upArrow.png")));
    upCPButt.setBorder(null);
    upCPButt.addActionListener(new upCPhandler());
    JButton addCPButt = new JButton("add");
    addCPButt.addActionListener(new addCPhandler());
    JButton removeCPButt = new JButton("remove");
    removeCPButt.addActionListener(new delCPhandler());
    JButton dnCPButt = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/downArrow.png")));
    dnCPButt.setBorder(null);
    dnCPButt.addActionListener(new downCPhandler());
    bPan.add(upCPButt);
    bPan.add(addCPButt);
    bPan.add(removeCPButt);
    bPan.add(dnCPButt);
    bPan.add(Box.createHorizontalGlue());
    classpathP.add(bPan);

    tabbedPane.addTab("Additional classpath entries",classpathP);

    JPanel recentP = new JPanel();
    recentP.setLayout(new BoxLayout(recentP,BoxLayout.Y_AXIS));

    JButton clearEGRecent = new JButton("Clear recent event graphs list");
    clearEGRecent.addActionListener(new clearEGHandler());
    clearEGRecent.setAlignmentX(Box.CENTER_ALIGNMENT);
    JButton clearAssRecent = new JButton("Clear recent assemblies list");
    clearAssRecent.addActionListener(new clearAssHandler());
    clearAssRecent.setAlignmentX(Box.CENTER_ALIGNMENT);
    recentP.add(Box.createVerticalGlue());
    recentP.add(clearEGRecent);
    recentP.add(clearAssRecent);
    recentP.add(Box.createVerticalGlue());

    tabbedPane.addTab("Recent files lists",recentP);

  }

  private static XMLConfiguration vConfig;
  private static String xClassPathKey = "extraClassPath.path";
  private static String xClassPathClearKey = "extraClassPath";
  private static String recentEGClearKey = "history.EventGraphEditor.Recent";
  private static String recentAssyClearKey = "history.AssemblyEditor.Recent";
  
  private static void initConfig()
  {
    try {
      vConfig = VGlobals.instance().getHistoryConfig();
    }
    catch (Exception e) {
      System.out.println("Error loading config file: "+e.getMessage());
      vConfig = null;
    }
  }

  class clearEGHandler implements ActionListener
  {
    public void actionPerformed(ActionEvent actionEvent)
    {
      vConfig.clearTree(recentEGClearKey);
    }
  }
  class clearAssHandler implements ActionListener
  {
    public void actionPerformed(ActionEvent actionEvent)
    {
      vConfig.clearTree(recentAssyClearKey);
    }
  }

  private void clearClassPathEntries()
  {
    vConfig.clearTree(xClassPathClearKey);
  }

  private void saveClassPathEntries(String[] lis)
  {
    clearClassPathEntries();

    for(int i=0;i<lis.length;i++) {
      vConfig.setProperty(xClassPathKey +"("+i+")[@value]",lis[i]);
    }
  }

  private void fillWidgets()
  {
    //classPathJlist.removeAll();
    DefaultListModel mod = new DefaultListModel();
    String[] sa = getExtraClassPath();
    for(int i=0;i<sa.length;i++)
      mod.addElement(sa[i]);
    classPathJlist.setModel(mod);
  }

  private void unloadWidgets()
  {

  }

  class cancelButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      modified = false;    // for the caller
      setVisible(false);
    }
  }

  class applyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      if (modified)
        unloadWidgets();
      setVisible(false);
    }
  }

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if (modified == true) {
        int ret = JOptionPane.showConfirmDialog(SettingsDialog.this, "Apply changes?",
            "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION)
          okButt.doClick();
        else
          canButt.doClick();
      }
      else
        canButt.doClick();
    }
  }

  JFileChooser addChooser;

  class addCPhandler implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if (addChooser == null) {
        addChooser = new JFileChooser(System.getProperty("user.dir"));
        addChooser.setMultiSelectionEnabled(false);
        addChooser.setAcceptAllFileFilterUsed(false);
        addChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        addChooser.setFileFilter(new FileFilter()
        {
          public boolean accept(File f)
          {
            if (f.isDirectory()) {
              return true;
            }
            String nm = f.getName();
            int idx = nm.lastIndexOf('.');
            if (idx != -1) {
              String extension = nm.substring(idx).toLowerCase();
              if (extension != null && (
                  extension.equals(".jar") ||
                  extension.equals(".zip"))) {
                return true;
              }
            }
            return false;
          }

          public String getDescription()
          {
            return "Directories, jars and zips";
          }
        });
      }

      int retv = addChooser.showOpenDialog(SettingsDialog.this);
      if (retv == JFileChooser.APPROVE_OPTION) {
        File selFile = addChooser.getSelectedFile();
        String absPath = selFile.getAbsolutePath();
        String sep = System.getProperty("file.separator");
        if(selFile.isDirectory() && !absPath.endsWith(sep))
          absPath = absPath + sep;
        ((DefaultListModel)classPathJlist.getModel()).addElement(absPath);
        installClassPathIntoConfig();
      }
    }
  }

  private void installClassPathIntoConfig()
  {
    Object m = classPathJlist.getModel();
    Object[] oa = ((DefaultListModel)classPathJlist.getModel()).toArray();
    String[] sa = new String[oa.length];
    for(int j=0;j<oa.length;j++)
      sa[j]=(String)oa[j];

    saveClassPathEntries(sa);
  }

  class delCPhandler implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      int[] selected = classPathJlist.getSelectedIndices();
      if(selected == null || selected.length <= 0)
        return;
      for(int i=selected.length-1;i>=0;i--) {
        ((DefaultListModel)classPathJlist.getModel()).removeElementAt(selected[i]);
      }
      installClassPathIntoConfig();
    }
  }

  class upCPhandler implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      int[] selected = classPathJlist.getSelectedIndices();
      if(selected == null || selected.length <= 0 || selected[0]<=0)
         return;
      moveLine(selected[0],-1);
    }
  }
  private void moveLine(int idx, int polarity)
  {
    classPathJlist.clearSelection();
    DefaultListModel mod = (DefaultListModel)classPathJlist.getModel();
    Object o = mod.getElementAt(idx);
    mod.removeElementAt(idx);
    mod.insertElementAt(o,idx + polarity);
    classPathJlist.setSelectedIndex(idx + polarity);

  }
  class downCPhandler implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      int[] selected = classPathJlist.getSelectedIndices();
      int listLen = classPathJlist.getModel().getSize();

      if(selected == null || selected.length <= 0 || selected[0]>=(listLen-1))
         return;
      moveLine(selected[0],+1);
    }
  }

  public static String[] getExtraClassPath()
  {
    if(vConfig==null)
      initConfig();
    VGlobals.instance().resetWorkClassLoader();
    return vConfig.getStringArray(xClassPathKey +"[@value]");
  }

}
