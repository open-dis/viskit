/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

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
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
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
package viskit.doe;

import edu.nps.util.Log4jUtilities;
import javax.swing.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitStatics;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 10:36:33 AM
 * @version $Id$
 */
public class DoeMain implements DoeEvents
{
    static final Logger LOG = LogManager.getLogger();

    private DoeController controller;
    private DoeMainFrame mainFrame;
    private DoeMenuBar menuBar;

    public DoeMain(boolean contentOnly, String[] args) {
        if (!contentOnly) {
            DoeMain.setLookAndFeel();
        }

        doConfiguration();
        buildController();
        buildMainFrame(contentOnly);
        buildMenuBar(contentOnly);

        if (!contentOnly) {
            displayFrame();
        }

        if (args != null) {
            for (String arg : args) {
                controller.actionPerformed(OPEN_FILE, arg);
            }
        }
    }

    private void doConfiguration() {

    }

    private void buildMenuBar(boolean contentOnly) {
        menuBar = new DoeMenuBar(controller, contentOnly);
        if (!contentOnly) {
            mainFrame.setJMenuBar(menuBar);
        }
    }

    private void buildMainFrame(boolean contentOnly) {
        mainFrame = new DoeMainFrame(contentOnly, controller);
        controller.setDoeFrame(mainFrame);
    }

    private void buildController() {
        controller = new DoeController();
    }

    public DoeMainFrame getMainFrame() {
        return mainFrame;
    }

    public JMenuBar getMenus() {
        return menuBar;
    }

    public DoeController getController() {
        return controller;
    }

    private void displayFrame() {
        mainFrame.installContent();
        mainFrame.setJMenuBar(menuBar);
        mainFrame.setBounds(100, 100, 800, 600);

        // Make frame visible in GUI thread to be strictly legal
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
        });
    }

    /* Use JGoodies L&F */
    public static void setLookAndFeel() {
        String laf;

        String os = ViskitStatics.OPERATING_SYSTEM.toLowerCase();
        if (os.contains("windows")) {
            laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        } else {
            laf = "javax.swing.plaf.metal.MetalLookAndFeel";
        }

        try {
            UIManager.setLookAndFeel(laf);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            LOG.error("can't change l&f");
        }
    }

    public JMenuItem getQuitMenuItem() {
        if (menuBar != null) {
            JMenu fileM = menuBar.getMenu(0);
            for (int i = 0; i < fileM.getMenuComponentCount(); i++) {
                JMenuItem m = fileM.getItem(i);
                if (m != null && m.getText().toLowerCase().startsWith("quit")) {
                    return m;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        new DoeMain(false, args);
    }

    public static DoeMain main2() {
        return new DoeMain(true, null);
    }
}
