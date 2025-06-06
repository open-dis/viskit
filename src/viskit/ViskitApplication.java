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
import java.util.Arrays;

import javax.swing.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.view.MainFrame;
import viskit.view.dialog.ViskitUserPreferencesDialog;

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
    static final Logger LOG = LogManager.getLogger();
    
    public static final String VISKIT_SHORT_APPLICATION_NAME = "Viskit";
    public static final String VISKIT_WELCOME_MESSAGE        = "Welcome to " + VISKIT_SHORT_APPLICATION_NAME;
    public static final String VISKIT_FULL_APPLICATION_NAME  = "Visual Simkit (Viskit) Modeling Tool for Discrete Event Simulation (DES) Analysis";
    
    /**
     * Viskit entry point from the command line, or introspection
     * @param args command line arguments if any
     */
    public static void main(final String[] args)
    {
        LOG.info(VISKIT_WELCOME_MESSAGE);
        ViskitUserConfiguration.logDotViskitConfigurationDirectoryStatus();
        
        // Launch all GUI stuff on, or within the EDT
        try {
            SwingUtilities.invokeLater(() -> {
                createGUI(args);
            });

        } 
        catch (Exception e) {
            LOG.error("main(" + Arrays.toString(args) + ") exception: " + e.getMessage());

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

                String msg = "Viskit has experienced a startup glitch.  <br/>"
                        + "Please navigate to " + ViskitUserConfiguration.VISKIT_ERROR_LOG.getAbsolutePath() + "<br/>"
                        + "and email the log to "
                        + "<b><a href=\"" + url.toString() + "\">" + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
                        + "<br/><br/>Click the link to open up an email form, then copy and paste the log's contents";

                ViskitStatics.showHyperlinkedDialog(null, e.toString(), url, msg, true);
            } 
            catch (MalformedURLException | URISyntaxException ex) {
                LOG.fatal(ex);
            }
        }
    }

    /** Draconian process for restoring from a possibly corrupt, or out if synch
     * .viskit config directory in the user's profile space
     */
    public static void nukeDotViskit() {
        File dotViskit = ViskitUserConfiguration.VISKIT_CONFIGURATION_DIR;
        if (dotViskit.exists()) {

            // Can't delete .viskit dir unless it's empty
            File[] files = dotViskit.listFiles();
            for (File file : files) {
                file.delete();
            }
            if (dotViskit.delete())
                LOG.info("{} was found and deleted from your system.", dotViskit.getName());
            ViskitUserConfiguration.logDotViskitConfigurationDirectoryStatus();
            LOG.info("Please restart Viskit");
        }
    }

    /** Static initializer for graphical user interface.
     * Beware of ExceptionInInitializerError when running this code or initializing a static variable.
     * @see https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/ExceptionInInitializerError.html
     * @param args arguments
     */
    private static void createGUI(String[] args)
    {
        try 
        {
            boolean isMac = ViskitStatics.OPERATING_SYSTEM.contains("Mac");
            String initialAssemblyFile = null;

            if (args.length > 0)
                initialAssemblyFile = args[0];

            LOG.debug("***Inside ViskitApplication main, createGUI{}: ", args.length);
            if (viskit.ViskitStatics.debug) {
                LOG.debug("***viskit.ViskitStatics.debug=" + viskit.ViskitStatics.debug);
            }

            setLookAndFeel();

            // Leave tooltips on the screen until mouse movement causes removal
            ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
            toolTipManager.setDismissDelay(Integer.MAX_VALUE); // never remove automatically

            // these ViskitGlobal calls need to occur afterMainFrame initialization so that diagnostic popups are possible,
            // but are needed beforehand for MainFrame to find globals, ouch... 
            // resolution: use ViskitGlobals.instance().hasMainFrameInitialized() to avoid exceptions
            ViskitGlobals.instance().initializeProjectHome();  // needed for first time, or if no project found

            MainFrame mainFrame = new MainFrame(initialAssemblyFile);
            if (isMac) {
                setupMacUI(); // special handling
            }
            mainFrame.setVisible(true);

            MainFrame.runLater(500L, () -> {
                // wait a second, give file loading a chance to finish before checking no models loaded...
                LOG.info("ViskitApplication launched and displayed successfully"); // updating log first facilitates debugging
                MainFrame.displayWelcomeGuidance(); // if no event graph or assembly is open; blocks and awaits user acknowledgement
            });
        }
        catch (ExceptionInInitializerError exception)
        {
            LOG.error ("createGUI(" + String.join(",", args) + ") " + exception);
        }
    }

    private static void setLookAndFeel()
    {
        String userPreferencesLookAndFeel = ViskitUserPreferencesDialog.getLookAndFeel();
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
        Desktop.getDesktop().setQuitHandler((QuitEvent quitEvent, QuitResponse quitResponse) -> 
        {
            mainFrame.getMyQuitAction().actionPerformed(null); // perform cleanups
            quitResponse.performQuit();
        });

        ImageIcon aboutIcon = new ImageIcon(ViskitApplication.class.getResource("/viskit/images/ViskitLogo.gif"));
        Taskbar.getTaskbar().setIconImage(aboutIcon.getImage());
    }

} // end class file ViskitApplication.java