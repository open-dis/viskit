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

import com.jgoodies.looks.Options;
import com.jgoodies.looks.common.ShadowPopupFactory;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import edu.nps.util.LogUtils;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.swing.*;

/**
 * <p>MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Sep 22, 2005 : 3:23:52 PM
 * @version $Id$
 */
public class EventGraphAssemblyComboMain {

    private static ImageIcon aboutIcon = null;

    /**
     * Viskit entry point from the command line, or introspection
     * @param args command line arguments if any
     */
    public static void main(final String[] args) {

        // Launch all GUI stuff on, or within the EDT
        try {
            if (!EventQueue.isDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        createGUI(args);
                    }
                });
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        createGUI(args);
                    }
                });
            }

        } catch (InterruptedException e) {
            LogUtils.getLogger(EventGraphAssemblyComboMain.class).error(e);

            // if we got here, then we need to nuke the ${user.home}/.viskit dir
            // it will be recreated on next start up
            nukeHomeDirectory();
        } catch (InvocationTargetException e) {
            LogUtils.getLogger(EventGraphAssemblyComboMain.class).error(e);

            // If we encounter this case, then uncomment printStackTrace() to
            // drill down on the cause.  Easier than setting a breakpoint and
            // debugging!
//            e.printStackTrace();

            // if we got here, then we need to nuke the ${user.home}/.viskit dir
            // it will be recreated on next start up
            nukeHomeDirectory();
        }
    }

    private static void nukeHomeDirectory() {
        java.io.File viskitDir = new java.io.File(System.getProperty("user.home") + "/.viskit");
        if (viskitDir.exists()) {

            // Can't delete .viskit dir unless it's empty
            java.io.File[] files = viskitDir.listFiles();
            for (java.io.File file : files) {
                file.delete();
            }
            boolean success = viskitDir.delete();
            LogUtils.getLogger(EventGraphAssemblyComboMain.class).warn("The contents of your " + viskitDir.getPath() + " directory was found to be corrupted and will be deleted");
            if (success)
                LogUtils.getLogger(EventGraphAssemblyComboMain.class).info(viskitDir.getName() + " was found and deleted.");
            LogUtils.getLogger(EventGraphAssemblyComboMain.class).info("Please restart Viskit");
        }
    }

    private static void createGUI(String[] args) {

        boolean onMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
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

        if (onMac) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Viskit");
            aboutIcon = new ImageIcon(EventGraphAssemblyComboMain.class.getResource("/viskit/images/ViskitLogo.gif"));
            setupMacGUI();
        }

        mainFrame.setVisible(true);
    }

    private static void setLandFandFonts() {
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

    private static void setJGoodies() throws Exception{
        LookAndFeel laf = new PlasticLookAndFeel();
        Options.setUseNarrowButtons(true);
        PlasticLookAndFeel.setMyCurrentTheme(new com.jgoodies.looks.plastic.theme.DesertBluer());
        PlasticLookAndFeel.setHighContrastFocusColorsEnabled(true);

        UIManager.setLookAndFeel(laf);
        ShadowPopupFactory.uninstall();
    }

    private static void setupMacGUI() {
        try {
            Class<?> applicationListener = Class.forName("com.apple.eawt.ApplicationListener");
            Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] { applicationListener }, new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    switch (method.getName()) {
                        case "handleQuit":
                            ((EventGraphAssemblyComboMainFrame)VGlobals.instance().getMainAppWindow()).myQuitAction.actionPerformed(null);
                            break;
                        case "handleAbout":
                            try {
                                Help help = new Help(VGlobals.instance().getMainAppWindow());
                                help.aboutEventGraphEditor();
                                Class<?> applicationEventClass = Class.forName("com.apple.eawt.ApplicationEvent");
                                Method setHandled = applicationEventClass.getMethod("setHandled", boolean.class);
                                setHandled.invoke(args[0], true);
                            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                System.err.println("Error showing About Box: " + ex);
                            }   break;
                    }
                    return null;
                }
            });

            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Object applicationInstance = applicationClass.newInstance();

            Method m = applicationClass.getMethod("addApplicationListener", applicationListener);
            m.invoke(applicationInstance, proxy);

            if (aboutIcon != null) {
                try {
                    m = applicationClass.getMethod("setDockIconImage", Image.class);
                    m.invoke(applicationInstance, aboutIcon.getImage());
                } catch (NoSuchMethodException ex){
                    System.err.println("Error showing aboutIcon in dock " + ex);
                }
            }
        } catch (ClassNotFoundException | IllegalArgumentException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            System.err.println("Error defining Apple Quit & About handlers: " + ex);
        }
    }

} // end class file EventGraphAssemblyComboMain.java