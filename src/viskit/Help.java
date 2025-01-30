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

import edu.nps.util.LogUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.*;
import javax.help.HelpBroker;
import javax.help.CSH;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.SwingHelpUtilities;

import viskit.util.BrowserLauncher;
import viskit.util.Version;

/**
 * @version $Id$
 * @author  ahbuss
 */
public class Help
{
    public static final Version VERSION = new Version("version.txt");
    public static final String VERSION_STRING = VERSION.getVersionString();
    public static final String CR = "<br>";
    public static final String ABOUT_EVENTGRAPH_STRING =
            "Viskit Event Graph and Assembly Editor" + CR + "   version " + VERSION_STRING +
            ", last modified: " + VERSION.getLastModified() + CR + CR;
    public static final String ABOUT_ASSEMBLY_STRING =
            "Viskit Assembly Editor" + CR + "   version " + VERSION_STRING + CR
            + "last modified: " + VERSION.getLastModified() + CR + CR;
    public static final String SIMKIT_URL   = "https://github.com/ahbuss/Simkit";
    public static final String VISKIT_URL   = "https://gitlab.nps.edu/Savage/viskit";
    public static final String ISSUES_URL   = "https://gitlab.nps.edu/Savage/viskit/issues";
    public static final String BUGZILLA_URL = "https://github.com/terry-norbraten/viskit/issues";
    public static final String DEVELOPERS =
            "Copyright &copy; 2004-2025 under the Lesser GNU Public License (LGPL)" + CR + CR
            + "<b>Developers:</b>" + CR
            + "&nbsp;&nbsp;&nbsp;Terry Norbraten" + CR
            + "&nbsp;&nbsp;&nbsp;Don Brutzman" + CR
            + "with" + CR
            + "&nbsp;&nbsp;&nbsp;Mike Bailey" + CR
            + "&nbsp;&nbsp;&nbsp;Arnold Buss" + CR
            + "&nbsp;&nbsp;&nbsp;Rick Goldberg" + CR
            + "&nbsp;&nbsp;&nbsp;Don McGregor" + CR
            + "&nbsp;&nbsp;&nbsp;Patrick Sullivan";
    public static final String SIMKIT_PAGE =
            CR
            + "Visit the Simkit home page at" + CR
            + LinkURLString(SIMKIT_URL) + CR;
    public static final String VISKIT_PAGE = CR
            + "Visit the Viskit home page at" + CR
            + LinkURLString(VISKIT_URL);
    public static final String VERSIONS =
            "<hr>Simkit Version: "
            + simkit.Version.getVersion()
            + CR + "Java version: "
            + System.getProperty("java.version");
    public static final String VISKIT_ISSUES_PAGE = CR
            + "Viskit Issue tracker:" + CR
            + LinkURLString(ISSUES_URL);

    private HelpBroker hb;

    // A strange couple of things to support JavaHelp's rather strange design for CSH use:
    private final Component TUTORIAL_COMPONENT;
    private final ActionListener TUTORIAL_LISTENER_LAUNCHER;

    private Component parent;
    private final Icon icon;
    private final JEditorPane aboutViskitEditorPane;

    /** Creates a new instance of Help
     * @param parent main frame to center on
     */
    public Help(Component parent) {
        this.parent = parent;

        ClassLoader cl = viskit.Help.class.getClassLoader();
        URL helpSetURL = HelpSet.findHelpSet(cl, "viskit/javahelp/vHelpSet.hs");
        try {
            hb = new HelpSet(null, helpSetURL).createHelpBroker();
        } catch (HelpSetException e) {
//        e.printStackTrace();
            LogUtils.getLogger(Help.class).error(e);
        }

        // Here we're setting up the action event peripherals for the tutorial menu selection
        TUTORIAL_LISTENER_LAUNCHER = new CSH.DisplayHelpFromSource(hb);
        TUTORIAL_COMPONENT = new Button();

        CSH.setHelpIDString(TUTORIAL_COMPONENT, "hTutorial");

        icon = new ImageIcon(
                getClass().getClassLoader().getResource(
                "viskit/images/ViskitLogo.png")
        );

        BrowserLauncher bl = new BrowserLauncher(null);
        SwingHelpUtilities.setContentViewerUI("viskit.util.BrowserLauncher");

        aboutViskitEditorPane = new JEditorPane();
        aboutViskitEditorPane.addHyperlinkListener(bl);
        aboutViskitEditorPane.setContentType("text/html");
        aboutViskitEditorPane.setEditable(false);
        aboutViskitEditorPane.setText(ABOUT_EVENTGRAPH_STRING
                + DEVELOPERS + CR + VISKIT_PAGE //+ VISKIT_ISSUES_PAGE
                + SIMKIT_PAGE + VERSIONS);
    }

    public void aboutViskit() {
        JOptionPane.showMessageDialog(parent,
                aboutViskitEditorPane,
                "About Viskit",
                JOptionPane.OK_OPTION,
                icon
        );
    }

    public void doContents() {
        hb.setDisplayed(true);
        hb.setCurrentView("TOC");
    }

    public void doSearch() {
        hb.setDisplayed(true);
        hb.setCurrentView("Search");
    }

    public void doTutorial() {
        ActionEvent ae = new ActionEvent(TUTORIAL_COMPONENT, 0, "tutorial");
        TUTORIAL_LISTENER_LAUNCHER.actionPerformed(ae);
    }

    public void mainFrameLocated(Rectangle bounds) {
        Point p = new Point(bounds.x, bounds.y);
        Dimension d = new Dimension(bounds.width, bounds.height);
        Dimension hd = new Dimension(1200, 700);
        hb.setSize(hd);
        p.x = p.x + d.width / 2 - hd.width / 2;
        p.y = p.y + d.height / 2 - hd.height / 2;
        hb.setLocation(p);
    }

    public static String LinkURLString(String urlString) {
        String linkString = "";
        try {
            URL url = new URI(urlString).toURL();
            linkString = "<a href = " + url + ">" + url + "</a>";
        } catch (MalformedURLException | URISyntaxException ex) {}
        return linkString;

    }

    public static void main(String[] args) {
        System.out.println("Viskit DES interface: " + VERSION);
    }
}
