/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

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
package viskit;

import edu.nps.util.Log4jUtilities;

import java.awt.Desktop;
import java.awt.Taskbar;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitResponse;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.*;
import org.apache.logging.log4j.Logger;

import viskit.view.MainFrame;
import viskit.view.dialog.ViskitUserPreferences;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 22, 2005 : 3:23:52 PM
 * @version $Id$
 */
public class ViskitApplication
{
    static final Logger LOG = Log4jUtilities.getLogger(ViskitApplication.class);
    
    /**
     * Viskit entry point from the command line, or introspection
     * @param args command line arguments if any
     */
    public static void main(final String[] args)
    {
        
        // Launch all GUI stuff on, or within the EDT
        try {
            SwingUtilities.invokeLater(() -> {
                createGUI(args);
            });

        } 
        catch (Exception e) {
            LOG.error(e);

            if (e instanceof InvocationTargetException) {

                // not convinced we need to do this anymore.  A corrupted
                // viskitProject can cause an InvocationTargetException.  The
                // Apache Commons config files have behaved rather well and don't
                // need to be nuked as of late: 03 DEC 2014.
//                nukeDotViskit();
                e.printStackTrace(System.err);
            }

            try {
                URL url = new URI("mailto:" + ViskitStatics.VISKIT_MAILING_LIST +
                        "?subject=Viskit%20startup%20error&body=log%20output:").toURL();

                String msg = "Viskit has experienced a startup glitch.  <br/>Please "
                        + "navigate to " + ViskitConfigurationStore.VISKIT_ERROR_LOG.getPath() + " and "
                        + "email the log to "
                        + "<b><a href=\"" + url.toString() + "\">" + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
                        + "<br/><br/>Click the link to open up an email form, then copy and paste the log's contents";

                ViskitStatics.showHyperlinkedDialog(null, e.toString(), url, msg, true);
            } catch (MalformedURLException | URISyntaxException ex) {
                LOG.fatal(ex);
            }
        }
    }

    /** Draconian process for restoring from a possibly corrupt, or out if synch
     * .viskit config directory in the user's profile space
     */
    public static void nukeDotViskit() {
        File dotViskit = ViskitConfigurationStore.VISKIT_CONFIGURATION_DIR;
        if (dotViskit.exists()) {

            // Can't delete .viskit dir unless it's empty
            File[] files = dotViskit.listFiles();
            for (File file : files) {
                file.delete();
            }
            if (dotViskit.delete())
                LOG.info("{} was found and deleted from your system.", dotViskit.getName());

            LOG.info("Please restart Viskit");
        }
    }

    private static void createGUI(String[] args)
    {
        boolean isMac = ViskitStatics.OPERATING_SYSTEM.contains("Mac");
        String initialAssemblyFile = null;

        if (args.length > 0)
            initialAssemblyFile = args[0];

        if (viskit.ViskitStatics.debug) {
            LOG.debug("***Inside ViskitApplication main, createGUI{}: ", args.length);
        }
        
        setLookAndFeel();

        // Leave tooltips on the screen until mouse movement causes removal
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.setDismissDelay(Integer.MAX_VALUE); // never remove automatically

        MainFrame mainFrame = new MainFrame(initialAssemblyFile);
        if (isMac) {
            setupMacUI();
        }
        mainFrame.setVisible(true);
        
        MainFrame.runLater(1000L, () -> {
            // give file loading a chance to finish before checking no models loaded...
            MainFrame.displayWelcomeGuidance() ;
        });
    }

    private static void setLookAndFeel()
    {
        String userPreferencesLookAndFeel = ViskitUserPreferences.getLookAndFeel();
        try {
            if (userPreferencesLookAndFeel == null || userPreferencesLookAndFeel.isEmpty() || userPreferencesLookAndFeel.equalsIgnoreCase("default")) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } else if (userPreferencesLookAndFeel.equalsIgnoreCase("platform")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(userPreferencesLookAndFeel);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOG.error("Error setting {} Look and Feel", userPreferencesLookAndFeel);
        }
    }

    private static void setupMacUI() {
        Desktop.getDesktop().setAboutHandler(e -> {
            Help help = ViskitGlobals.instance().getHelp();
            help.aboutViskit();
        });

        final MainFrame mainFrame = (MainFrame) ViskitGlobals.instance().getMainFrame();

        // CMD Q for macOS
        Desktop.getDesktop().setQuitHandler((QuitEvent e, QuitResponse response) -> {
            mainFrame.getMyQuitAction().actionPerformed(null); // perform cleanups
            response.performQuit();
        });

        ImageIcon aboutIcon = new ImageIcon(ViskitApplication.class.getResource("/viskit/images/ViskitLogo.gif"));
        Taskbar.getTaskbar().setIconImage(aboutIcon.getImage());
    }

} // end class file ViskitApplication.java