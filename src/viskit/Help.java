package viskit;

import edu.nps.util.LogUtils;
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
import viskit.util.BrowserLauncher;
import viskit.util.Version;

/**
 * @version $Id$
 * @author  ahbuss
 */
public class Help {

    public static final Version VERSION = new Version("viskit/version.txt");
    public static final String VERSION_STRING = VERSION.getVersionString();
    private Component parent;
    private Icon icon;
    private JEditorPane aboutEGEditorPane;
    private JEditorPane aboutAssemblyEditorPane;
    public static final String CR = "<br>";
    public static final String ABOUT_EG_STRING =
            "Viskit Event Graph Editor" + CR + "   version " + VERSION_STRING + CR
            + VERSION.getLastModified() + CR + CR;
    public static final String ABOUT_ASSEMBLY_STRING =
            "Viskit Assembly Editor" + CR + "   version " + VERSION_STRING + CR
            + VERSION.getLastModified() + CR + CR;
    public static final String SIMKIT_URL = "http://diana.nps.edu/Simkit/";
    public static final String VISKIT_URL = "http://diana.nps.edu/Viskit/";
    public static final String BUGZILLA_URL = "https://diana.nps.edu/bugzilla/";
    public static final String DEVELOPERS =
            "Copyright &copy; 2004-2010 under the Lesser GNU License" + CR + CR
            + "<b>Developers:</b>" + CR
            + "&nbsp;&nbsp;&nbsp;Arnold Buss" + CR
            + "&nbsp;&nbsp;&nbsp;Mike Bailey" + CR
            + "&nbsp;&nbsp;&nbsp;Rick Goldberg" + CR
            + "&nbsp;&nbsp;&nbsp;Don McGregor" + CR
            + "&nbsp;&nbsp;&nbsp;Don Brutzman" + CR
            + "&nbsp;&nbsp;&nbsp;Patrick Sullivan" + CR
            + "&nbsp;&nbsp;&nbsp;Terry Norbraten";
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
    public static final String BUGZILLA_PAGE = CR
            + "Please register for the Viskit Issue tracker:" + CR
            + LinkURLString(BUGZILLA_URL);
    private static HelpBroker hb;
    // A strange couple of things to support JavaHelp's rather strange design for CSH use:
    private static final Component TUTORIAL_COMPONENT;
    private static final ActionListener TUTORIAL_LISTENER_LAUNCHER;

    static {
        ClassLoader cl = viskit.Help.class.getClassLoader();
        URL helpSetURL = HelpSet.findHelpSet(cl, "viskit/javahelp/vHelpSet.hs");
        HelpSet hs = null;
        try {
            hs = new HelpSet(null, helpSetURL);
            hb = hs.createHelpBroker();
        } catch (HelpSetException e) {
//        e.printStackTrace();
            LogUtils.getLogger(Help.class).error(e);
        }

        // Here we're setting up the action event peripherals for the tutorial menu selection
        TUTORIAL_COMPONENT = new Button();
        TUTORIAL_LISTENER_LAUNCHER = new CSH.DisplayHelpFromSource(hb);
        CSH.setHelpIDString(TUTORIAL_COMPONENT, "hTutorial");
    }

    /** Creates a new instance of Help */
    public Help(Component parent) {
        this.parent = parent;
        icon = new ImageIcon(
                Thread.currentThread().getContextClassLoader().getResource(
                "viskit/images/ViskitLogo.png"));
        aboutEGEditorPane = new JEditorPane();

        aboutEGEditorPane.addHyperlinkListener(
                new BrowserLauncher());
        aboutEGEditorPane.setContentType("text/html");
        aboutEGEditorPane.setEditable(false);
        aboutEGEditorPane.setText(ABOUT_EG_STRING
                + DEVELOPERS + CR + VISKIT_PAGE + BUGZILLA_PAGE
                + SIMKIT_PAGE + VERSIONS);

        aboutAssemblyEditorPane = new JEditorPane();

        aboutAssemblyEditorPane.addHyperlinkListener(
                new BrowserLauncher());
        aboutAssemblyEditorPane.setContentType("text/html");
        aboutAssemblyEditorPane.setEditable(false);
        aboutAssemblyEditorPane.setText(ABOUT_ASSEMBLY_STRING
                + DEVELOPERS + CR + VISKIT_PAGE + BUGZILLA_PAGE
                + SIMKIT_PAGE);
    }

    public void aboutEventGraphEditor() {
        JOptionPane.showMessageDialog(parent, aboutEGEditorPane,
                "About Viskit Event Graph Editor...",
                JOptionPane.OK_OPTION, icon);
    }

    public void aboutAssemblyEditor() {
        JOptionPane.showMessageDialog(parent, aboutAssemblyEditorPane,
                "About Viskit Event Graph Editor...",
                JOptionPane.OK_OPTION, icon);
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
    /*
    public void help() {
    JOptionPane.showMessageDialog(parent, "Viskit help not yet implemented",
    "Viskit Help", JOptionPane.OK_OPTION, icon);
    }
     */
    // message when the main frame has been positioned on the screen

    public void mainFrameLocated(Rectangle bounds) {
        Point p = new Point(bounds.x, bounds.y);
        Dimension d = new Dimension(bounds.width, bounds.height);
        //Dimension hd = hb.getSize();
        //hd.width+=100;
        Dimension hd = new Dimension(1200, 700);
        hb.setSize(hd);
        p.x = p.x + d.width / 2 - hd.width / 2;
        p.y = p.y + d.height / 2 - hd.height / 2;
        hb.setLocation(p);
    }

    public static String LinkURLString(String urlString) {
        String linkString = "";
        try {
            URL url = new URL(urlString);
            linkString = "<a href = " + url
                    + ">" + url + "<a>";

        } catch (MalformedURLException ex) {
        }
        return linkString;

    }

    public static void main(String[] args) {
        System.out.println("Viskit DES interface: " + VERSION);
    }
}
