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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class DoeController implements DoeEvents, ActionListener
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

    switch (c) {
      case OPEN_FILE:
        System.out.println("got OPEN_FILE event");
        doOpen(new File(((String)e.getSource())));
        break;

      case OPEN_FILE_CHOOSE:
        System.out.println("got OPEN_FILE_CHOOSE event");
        openSaveFileChooser.setDialogTitle("Open Doe File");
        int retv = openSaveFileChooser.showOpenDialog(mainFrame);
        if (retv != JFileChooser.APPROVE_OPTION)
          return;

        File f = openSaveFileChooser.getSelectedFile();
        doOpen(f);
        break;
      case IMPORT_ASSEMBLY:
        System.out.println("got IMPORT_ASSEMBLY event");
        break;

      case SAVE_FILE:
        System.out.println("got SAVE_FILE event");
        break;
      case SAVE_FILE_AS:
        System.out.println("got SAVE_FILE_AS event");
        break;

      case EXIT_APP:
        System.out.println("got EXIT_APP event");
        //if(!dirty)
          System.exit(0);
        break;
      case RUN_JOB:
        System.out.println("got RUN_JOB event");
        //todo use ParmamTable and FileHandler to write out a temp file
        // use it here
        String file = "/Users/mike/Desktop/bremerton.xml";
        new JobLauncher(file,mainFrame);
        break;
    }
  }
  private void doOpen(File f)
  {
    try {
      DoeFileModel dfm = FileHandler.openFile(f);
      mainFrame.setModel(dfm);
    }
    catch (Exception e) {
      System.out.println("bad file open: "+e.getMessage());
    }
  }
  private JFileChooser initFileChooser()
  {
    JFileChooser chooser = new JFileChooser(System.getProperty("user.home")+"/Desktop"); //dir")); //"Scripts");
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileHandler.FileFilterEx[] filter = {
      new FileHandler.FileFilterEx(".xml", "Doe files (*.xml)", true)
    };
    for (int i = 0; i < filter.length; i++)
      chooser.addChoosableFileFilter(filter[i]);

    //chooser.setFileView(new FileHandler.IconFileView(".xml", new ImageIcon("build/image/xmlicon.png")));
    return chooser;
  }

  public void setMenu(JMenuBar mb)
  {

  }
}