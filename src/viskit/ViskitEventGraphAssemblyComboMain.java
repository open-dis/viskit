/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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

import edu.nps.util.LogUtilities;
import java.awt.EventQueue;
import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.swing.*;
import viskit.control.EventGraphControllerImpl;
import viskit.view.ViskitApplicationFrame;
import viskit.view.dialog.UserPreferencesDialog;

/**
 * <p>MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Sep 22, 2005 : 3:23:52 PM
 * @version $Id$
 */
public class ViskitEventGraphAssemblyComboMain
{
	private static org.apache.log4j.Logger LOG; // create user's log directory before instantiating LOG below

    private static ImageIcon aboutIcon = null;

    /**
     * Viskit entry point from the command line, or introspection
     * @param args command line arguments if any
     */
    public static void main(final String[] args)
	{
		ViskitConfiguration initializeViskitConfigurationSingleton = ViskitConfiguration.instance (); // Create user's log directory first
		LOG = LogUtilities.getLogger(EventGraphControllerImpl.class);
		
        // Launch all GUI stuff on, or within the EventDispatchThread
        try 
		{
            if (!EventQueue.isDispatchThread())
			{
                SwingUtilities.invokeAndWait(new Runnable()
				{
                    @Override
                    public void run()
					{
                        createGUI(args);
                    }
                });
            } 
			else 
			{
                SwingUtilities.invokeLater(new Runnable()
				{
                    @Override
                    public void run()
					{
                        createGUI(args);
                    }
                });
            }
        } 
		catch (Exception e) // catch all
		{
            LogUtilities.getLogger(ViskitEventGraphAssemblyComboMain.class).error(e);

			// A corrupted viskitProject can cause an InvocationTargetException.
			// Check the log output.  Common fix: clean all and rebuild.
			
			// The Apache Commons configuration/*.xml files have behaved rather well
			// and don't need to be nuked as of late: 03 DEC 2014.
            // User saveConfigurationFiles method nukeDotViskit(); moved to user preferences
			
			ViskitStatics.sendErrorReport ("Visual Simkit (Viskit) has experienced a significant startup problem.", e);
        }
    }

    private static void createGUI(String[] args) {

        boolean isMac = ViskitStatics.OPERATING_SYSTEM.toLowerCase().startsWith("mac os x");
        String initialFile = null;

        if (args.length > 0) {
            initialFile = args[0];
        }

        if (viskit.ViskitStatics.debug)
		{
//            LOG.debug("Inside ViskitEventGraphAssemblyComboMain with " + args.length + " arguments");
        }
        setLookAndFeelAndFonts();

        // Leave tooltips on the screen until mouse movement causes removal
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.setDismissDelay(Integer.MAX_VALUE);  // never remove automatically

        ViskitApplicationFrame viskitMainFrame = new ViskitApplicationFrame(initialFile);
        ViskitGlobals.instance().setMainAppWindow(viskitMainFrame);

        if (isMac) {
            aboutIcon = new ImageIcon(ViskitEventGraphAssemblyComboMain.class.getResource("/viskit/images/ViskitLogo.gif"));
            setupMacGUI();
        }
        viskitMainFrame.setVisible(true); // display application and commence
    }

    private static void setLookAndFeelAndFonts()
	{
        String LOOK_AND_FEEL = UserPreferencesDialog.getLookAndFeel();
        try {
            if (LOOK_AND_FEEL == null || LOOK_AND_FEEL.isEmpty() || LOOK_AND_FEEL.equalsIgnoreCase("default")) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } else if (LOOK_AND_FEEL.equalsIgnoreCase("platform")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(LOOK_AND_FEEL);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOG.error("Error setting Look and Feel to " + LOOK_AND_FEEL);
        }
    }

    private static void setupMacGUI() {
        try {
            Class<?> applicationListener = ViskitStatics.ClassForName("com.apple.eawt.ApplicationListener");
            Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] { applicationListener }, new InvocationHandler() 
			{
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    switch (method.getName()) 
					{
                        case "handleQuit":
                            ViskitGlobals.instance().getViskitApplicationFrame().myExitAction.actionPerformed(null);
                            break;
                        case "handleAbout":
                            try {
                                Help help = ViskitGlobals.instance().getHelp();
                                help.aboutEventGraphEditor();
                                Class<?> applicationEventClass = ViskitStatics.ClassForName("com.apple.eawt.ApplicationEvent");
                                Method setHandled = applicationEventClass.getMethod("setHandled", boolean.class);
                                setHandled.invoke(args[0], true);
                            } 
							catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                LOG.error("Error showing About display: " + ex);
                            }   break;
                    }
                    return null;
                }
            });

            Class<?> applicationClass = ViskitStatics.ClassForName("com.apple.eawt.Application");
            Object applicationInstance = applicationClass.newInstance();

            Method m = applicationClass.getMethod("addApplicationListener", applicationListener);
            m.invoke(applicationInstance, proxy);

            if (aboutIcon != null) {
                try {
                    m = applicationClass.getMethod("setDockIconImage", Image.class);
                    m.invoke(applicationInstance, aboutIcon.getImage());
                } catch (NoSuchMethodException ex){
                    LOG.error("Error showing aboutIcon in dock " + ex);
                }
            }
        } catch (IllegalArgumentException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            LOG.error("Error defining Apple Quit & About handlers: " + ex);
        }
    }

} // end class file ViskitEventGraphAssemblyComboMain.java