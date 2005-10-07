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
 * @since Jul 20, 2005
 * @since 10:36:33 AM
 */

package viskit.doe;

import edu.nps.util.DirectoryWatch;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;

public class DoeController implements DoeEvents, ActionListener, DirectoryWatch.DirectoryChangeListener
{
  private JFileChooser openSaveFileChooser;
  private DoeMainFrame mainFrame;

  public void setMainFrame(DoeMainFrame frame)
  {
    mainFrame = frame;
  }
  public DoeController()
  {
    openSaveFileChooser = initFileChooser();
  }
  // Event handling code;
  public void actionPerformed(char c)
  {
    actionPerformed(c, new Object());    // use dummy
  }

  public void actionPerformed(char c, Object src)
  {
    actionPerformed(new ActionEvent(src, 0, new String(new char[]{c})));
  }

  public void actionPerformed(ActionEvent e)
  {
    char c = e.getActionCommand().charAt(0);

    DoeFileModel dfm;
    switch (c) {
      case OPEN_FILE:
        checkDirty();
        doOpen(new File(((String)e.getSource())));
        break;

      case OPEN_FILE_CHOOSE:
        checkDirty();
        openSaveFileChooser.setDialogTitle("Open Assembly or DOE File");
        int retv = openSaveFileChooser.showOpenDialog(mainFrame);
        if (retv != JFileChooser.APPROVE_OPTION)
          return;

        File f = openSaveFileChooser.getSelectedFile();
        doOpen(f);
        break;

      case SAVE_FILE:
        dfm = mainFrame.getModel();
        if(dfm == null)
          return;

        if(dfm.userFile.getName().endsWith(".grd"))
          doSave(dfm);
        else
          doSaveAs(dfm);
        clearDirty();
        break;

      case SAVE_FILE_AS:
        dfm = mainFrame.getModel();
        if(dfm == null)
          return;
        doSaveAs(dfm);
        clearDirty();
        break;

      case EXIT_APP:
        if(checkDirty() != JOptionPane.CANCEL_OPTION)
          System.exit(0);
       break;

      case RUN_JOB:
        doRun();
        break;
    }
  }

  private int checkDirty()
  {
    DoeFileModel dfm = mainFrame.getModel();
    int reti = JOptionPane.YES_OPTION;
    if(dfm != null) {
      if(((ParamTableModel)dfm.paramTable.getModel()).dirty == true) {
        reti = JOptionPane.showConfirmDialog(mainFrame,"Save changes?");
        if(reti == JOptionPane.YES_OPTION)
          doSave(dfm);
      }
    }
    return reti;
  }
  private void clearDirty()
  {
    DoeFileModel dfm = mainFrame.getModel();
    if(dfm != null)
      ((ParamTableModel)dfm.paramTable.getModel()).dirty = false;
  }

  private void doSaveAs(DoeFileModel dfm)
  {
    String nm = dfm.userFile.getName();
    if(!nm.endsWith(".grd")) {
      int idx = nm.lastIndexOf('.');
      nm = nm.substring(0,idx);
      nm = nm+".grd";
    }

    openSaveFileChooser.setSelectedFile(new File(nm));
    int ret = openSaveFileChooser.showSaveDialog(mainFrame);
    if(ret != JFileChooser.APPROVE_OPTION)
      return;

    File f = openSaveFileChooser.getSelectedFile();
    try {
      dfm.marshall(f);
    }
    catch (Exception e) {
      JOptionPane.showMessageDialog(mainFrame,"Error on file save-as: "+e.getMessage(),"File save error",JOptionPane.OK_OPTION);
    }
    dfm.userFile = f;
    mainFrame.setTitle(mainFrame.titleString+" -- "+dfm.userFile.getName());
  }

  private void doSave(DoeFileModel dfm)
  {
    try {
      dfm.marshall(dfm.userFile);
    }
    catch (Exception e) {
      JOptionPane.showMessageDialog(mainFrame,"Error on file save: "+e.getMessage(),"File save error",JOptionPane.OK_OPTION);
    }
  }

  private void doOpen(File f)
  {
    try {
      DoeFileModel dfm = FileHandler.openFile(f);
      mainFrame.setModel(dfm);
      mainFrame.installContent();
      mainFrame.setTitle(mainFrame.titleString+" -- "+dfm.userFile.getName());
    }
    catch (Exception e) {
      System.out.println("bad file open: "+e.getMessage());
    }
  }
  private void doRun()
  {
    DoeFileModel dfm = mainFrame.getModel();

    // check for anything checked
    check:{
      int n = dfm.paramTable.getModel().getRowCount();

      for(int r=0;r<n;r++) {
        if(((Boolean)dfm.paramTable.getModel().getValueAt(r,ParamTableModel.FACTOR_COL)).booleanValue() == true){
          break check;
        }
      }
      JOptionPane.showMessageDialog(mainFrame,"No independent variables (factors) selected.",
          "Sorry",JOptionPane.ERROR_MESSAGE);
      return;
    }

    if(dfm != null) {
      File fil=null;
      try {
        fil = dfm.marshall();
      }
      catch (Exception e) {
        e.printStackTrace();
        return;
      }
      FileHandler.runFile(fil,dfm.userFile.getName()+" "+new Date().toString(),mainFrame);
    }
    else
      System.out.println("no model");
  }

  private JFileChooser initFileChooser()
  {
    JFileChooser chooser = new JFileChooser(); //System.getProperty("user.home")+"/Desktop"); //dir")); //"Scripts");
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
    FileHandler.FileFilterEx[] filter = {
      new FileHandler.FileFilterEx(".grd", "Doe files (*.grd)", true),
      new FileHandler.FileFilterEx(".xml", "Assembly files (*.xml)", true)
    };
    for (int i = 0; i < filter.length; i++)
      chooser.addChoosableFileFilter(filter[i]);

    return chooser;
  }

  /* Here's where we are informed of changed in the assembly file */
  public void fileChanged(File file, int action, DirectoryWatch source)
  {
    // temp:
    System.out.println("DoeController got assembly change message: "+action+
                                  " " + file.getAbsolutePath());
  }
}