package viskit;

import edu.nps.util.LogUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;
import javax.help.HelpBroker;
import javax.help.CSH;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.SwingHelpUtilities;
import org.apache.log4j.Logger;
import viskit.util.BrowserLauncher;
import viskit.util.Version;

/**
 * @version $Id$
 * @author  ahbuss
 */
public class Help 
{
    static final Logger LOG = LogUtilities.getLogger(Help.class);

    public static final Version VERSION = new Version("viskit/version.txt");
    public static final String  VERSION_STRING = VERSION.getVersionString();
    public static final String  CR = "<br>";
    public static final String  ABOUT_EVENT_GRAPH =
            "Visual Simkit (Viskit) Event Graph Editor" + CR + "   version " + VERSION_STRING + CR
            + VERSION.getLastModified() + CR + CR;
    public static final String  ABOUT_ASSEMBLY =
            "Assembly Editor for Visual Simkit (Viskit)" + CR + "   version " + VERSION_STRING + CR
            + VERSION.getLastModified() + CR + CR;
    public static final String   SIMKIT_URL = "http://eos.nps.edu/Simkit";
    public static final String   VISKIT_URL = "http://eos.nps.edu/Viskit";
    public static final String BUGZILLA_URL = "https://eos.nps.edu/bugzilla";
    public static final String   DEVELOPERS =
            "Copyright &copy; 2004-2016 under the Lesser GNU Public License (LGPL)" + CR + CR
            + "<b>Developers:</b>" + CR
            + "&nbsp;&nbsp;&nbsp;" + "Arnold Buss"     + CR
            + "&nbsp;&nbsp;&nbsp;" + "Terry Norbraten" + CR
            + "&nbsp;&nbsp;&nbsp;" + "Mike Bailey"     + CR
            + "&nbsp;&nbsp;&nbsp;" + "Don Brutzman"    + CR
            + "&nbsp;&nbsp;&nbsp;" + "Rick Goldberg"   + CR
            + "&nbsp;&nbsp;&nbsp;" + "Don McGregor"    + CR
            + "&nbsp;&nbsp;&nbsp;" + "Patrick Sullivan";
	
    public static final String SIMKIT_WEBSITE =
            CR
            + "Simkit home page: " + CR
            + LinkURLString(SIMKIT_URL) + CR;
	
    public static final String VISKIT_WEBSITE = CR
            + "Visual Simkit (Viskit) home page: " + CR
            + LinkURLString(VISKIT_URL);
    public static final String VERSIONS_INFORMATION =
            "<hr>Simkit Version: "
            + simkit.Version.getVersion()
            + CR + "Java version: "
            + System.getProperty("java.version");
	
    public static final String BUGZILLA_PAGE = CR // TODO currently inactive
            + "Please register for the Visual Simkit (Viskit) Issue Tracker:" + CR
            + LinkURLString(BUGZILLA_URL);

    private HelpBroker helpBroker;

    // A strange couple of things to support JavaHelp's rather strange design for CSH use:
    private final Component      TUTORIAL_COMPONENT;
	private final String         TUTORIAL_COMPONENT_ID = "helpTutorial";
    private final ActionListener TUTORIAL_LISTENER_LAUNCHER;

    private       Component   parent;
    private final Icon        viskitLogoIcon;
    private final JEditorPane aboutEventGraphEditorPane;
    private final JEditorPane aboutAssemblyEditorPane;

    /** Creates a new instance of Help
     * @param parent main frame to center on
     */
    public Help(Component parent)
	{
        this.parent = parent;

        ClassLoader classLoader = viskit.Help.class.getClassLoader();
        URL helpSetURL = HelpSet.findHelpSet(classLoader, "viskit/javahelp/vHelpSet.hs");
        try {
            helpBroker = new HelpSet(null, helpSetURL).createHelpBroker();
			helpBroker.setCurrentView("Introduction"); // initial page to view
        } 
		catch (HelpSetException e)
		{
//        e.printStackTrace();
            LOG.error ("Unable to load help set", e);
        }

        // Here we're setting up the action event peripherals for the tutorial menu selection
        TUTORIAL_LISTENER_LAUNCHER = new CSH.DisplayHelpFromSource(helpBroker);
        TUTORIAL_COMPONENT         = new Button();

        CSH.setHelpIDString(TUTORIAL_COMPONENT, TUTORIAL_COMPONENT_ID);

        viskitLogoIcon = new ImageIcon(
                ViskitGlobals.instance().getWorkClassLoader().getResource(
                "viskit/images/ViskitLogo.png"));

        BrowserLauncher browserLauncher = new BrowserLauncher(null);
        SwingHelpUtilities.setContentViewerUI("viskit.util.BrowserLauncher");

        aboutEventGraphEditorPane = new JEditorPane();
        aboutEventGraphEditorPane.addHyperlinkListener(browserLauncher);
        aboutEventGraphEditorPane.setContentType("text/html");
        aboutEventGraphEditorPane.setEditable(false);
        aboutEventGraphEditorPane.setText(ABOUT_EVENT_GRAPH
                + DEVELOPERS + CR + VISKIT_WEBSITE 
			 // + BUGZILLA_PAGE
                + SIMKIT_WEBSITE + VERSIONS_INFORMATION);

        aboutAssemblyEditorPane = new JEditorPane();
        aboutAssemblyEditorPane.addHyperlinkListener(browserLauncher);
        aboutAssemblyEditorPane.setContentType("text/html");
        aboutAssemblyEditorPane.setEditable(false);
        aboutAssemblyEditorPane.setText(ABOUT_ASSEMBLY
                + DEVELOPERS + CR + VISKIT_WEBSITE
			 // + BUGZILLA_PAGE
                + SIMKIT_WEBSITE);
    }

    public void aboutEventGraphEditor() 
	{
        JOptionPane.showMessageDialog(parent, aboutEventGraphEditorPane,
                "About Visual Simkit (Viskit) Event Graph Editor...",
                JOptionPane.OK_OPTION, viskitLogoIcon);
    }

    public final static String SHOW_HELP_ABOUT_ASSEMBLY_METHOD = "aboutAssemblyEditor"; // must match following method name.  not possible to accomplish this programmatically.
    public void aboutAssemblyEditor() // method name must exactly match preceding string value
	{
        JOptionPane.showMessageDialog(parent, aboutAssemblyEditorPane,
                "About Visual Simkit (Viskit) Assembly Editor...",
                JOptionPane.OK_OPTION, viskitLogoIcon);
    }

    public final static String SHOW_HELP_CONTENTS_METHOD = "showHelpContents"; // must match following method name.  not possible to accomplish this programmatically.
    public void showHelpContents() // method name must exactly match preceding string value
	{
        helpBroker.setDisplayed(true);
        helpBroker.setCurrentView("Introduction"); // initial page to view
    }

    public final static String SHOW_HELP_SEARCH_METHOD = "showHelpSearch"; // must match following method name.  not possible to accomplish this programmatically.
    public void showHelpSearch() // method name must exactly match preceding string value
	{
        helpBroker.setDisplayed(true);
        helpBroker.setCurrentView("Search");
    }

    public final static String SHOW_HELP_TUTORIAL_METHOD = "showHelpTutorial"; // must match following method name.  not possible to accomplish this programmatically.
    public void showHelpTutorial() // method name must exactly match preceding string value
	{
        ActionEvent actionEvent = new ActionEvent(TUTORIAL_COMPONENT, 0, "tutorial");
        TUTORIAL_LISTENER_LAUNCHER.actionPerformed(actionEvent);
    }

    public void mainFrameLocated(Rectangle bounds)
	{
        Point panelLocation = new Point(bounds.x, bounds.y);
        Dimension  d = new Dimension(bounds.width, bounds.height);
        Dimension hd = new Dimension(1200, 700);
        helpBroker.setSize(hd);
        panelLocation.x = Math.max(panelLocation.x + d.width  / 2 - hd.width  / 2, 0); // non-negative
        panelLocation.y = Math.max(panelLocation.y + d.height / 2 - hd.height / 2, 0);
        helpBroker.setLocation(panelLocation);
    }

    public static String LinkURLString(String urlString) 
	{
        String linkString = "";
        try {
            URL url = new URL(urlString);
            linkString = "<a href = " + url + ">" + url + "</a>";
        } 
		catch (MalformedURLException ex) {}
        return linkString;

    }

    public static void main(String[] args)
	{
        System.out.println("Visual Simkit (Viskit) Discrete Event Simulation (DES) interface: " + VERSION);
    }
}
