/*
Copyright (c) 1995-2009 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.movesinstitute.org)
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

import javax.swing.*;

import com.jgoodies.looks.Options;
import com.jgoodies.looks.common.ShadowPopupFactory;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;

import edu.nps.util.LogUtils;
import java.awt.EventQueue;

/**
 * MOVES Institute</p>
 * Naval Postgraduate School, Monterey, CA</p>
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Sep 22, 2005 : 3:23:52 PM
 * @version $Id$
 */
public class EventGraphAssemblyComboMain {

    /**
     * Viskit entry point from the command line, or introspection
     * @param args command line arguments if any
     */
    public static void main(final String[] args) {

        // Launch all GUI stuff on, or within the EDT
        try {
            if (!EventQueue.isDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        createGUI(args);
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        createGUI(args);
                    }
                });
            }

        } catch (Exception e) {
            LogUtils.getLogger(EventGraphAssemblyComboMain.class).error(e);
        }
    }

    private static void createGUI(String[] args) {
        String initialFile = null;

        if (args.length > 0) {
            initialFile = args[0];
        }

        if (viskit.Vstatics.debug) {
            System.out.println("***Inside EventGraphAssembly main: " + args.length);
        }
        setLandFandFonts();

        // Leave tooltips on the screen until mouse movement causes removal
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setDismissDelay(Integer.MAX_VALUE);  // never remove automatically

        JFrame mainFrame = new EventGraphAssemblyComboMainFrame(initialFile);
        VGlobals.instance().setMainAppWindow(mainFrame);
        mainFrame.setVisible(true);
    }

    public static void setLandFandFonts() {
        ViskitConfig cfg = ViskitConfig.instance();
        String s = cfg.getVal(ViskitConfig.LOOK_AND_FEEL_KEY);
        try {
            if (s == null || s.isEmpty() || s.equalsIgnoreCase("default")) {
                setJGoodies();
            } else if (s.equalsIgnoreCase("platform")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(s);
            }
        } catch (Exception ex) {
            System.err.println("Error setting Look and Feel to " + s);
        }
    }

    public static void setJGoodies() throws Exception{
        LookAndFeel laf = new PlasticLookAndFeel();
        Options.setUseNarrowButtons(true);
        PlasticLookAndFeel.setMyCurrentTheme(new com.jgoodies.looks.plastic.theme.DesertBluer());
        PlasticLookAndFeel.setHighContrastFocusColorsEnabled(true);

        UIManager.setLookAndFeel(laf);
        ShadowPopupFactory.uninstall();
    }

} // end class file EventGraphAssemblyComboMain.java