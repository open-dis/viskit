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
import java.awt.event.ActionListener;

public class DoeMenuBar extends JMenuBar implements DoeEvents
{
  private DoeController controller;
  private JMenu file, run;

  public DoeMenuBar(DoeController controller, boolean isSubComponent)
  {
    this.controller = controller;

    // Top-level menus
    file = new fileMenu(isSubComponent);
    add(file);

    if (!isSubComponent) {
      run = new runMenu();
      add(run);
    }
  }

  JMenuItem buildMI(String label, ActionListener lis, char cmd)
  {
    return buildMI(new JMenuItem(label), lis, cmd);
  }

  JMenuItem buildMI(JMenuItem mi, ActionListener lis, char cmd)
  {
    mi.setActionCommand(new String(new char[]{cmd}));
    mi.addActionListener(lis);
    return mi;
  }

  class fileMenu extends JMenu
  {
    fileMenu(boolean isSubComponent)
    {
      super("File");
      JMenuItem mi;

      if (!isSubComponent) {
        mi = buildMI("Open file", controller, OPEN_FILE_CHOOSE);
        add(mi);

        addSeparator();

        mi = buildMI("Save DOE file", controller, SAVE_FILE);
        add(mi);
        mi = buildMI("Save DOE file as ...", controller, SAVE_FILE_AS);
        add(mi);

        addSeparator();
      }
      mi = buildMI("Quit", controller, EXIT_APP);
      add(mi);
    }
  }

  class runMenu extends JMenu
  {
    runMenu()
    {
      super("Run");
      JMenuItem mi;

      mi = buildMI("Launch Doe job", controller, RUN_JOB);
      add(mi);
    }
  }
}