package viskit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.*;
import javax.help.HelpBroker;
import javax.help.CSH;
import javax.help.HelpSet;
import javax.help.HelpSetException;
/**
 * @version $Id$
 * @author  ahbuss
 */
public class Help {
    
    public static final String VERSION = "0.2.2";
    
    private Component parent;
    private Icon icon;
    
    private static final String CR = System.getProperty("line.separator");
    
    public static final String ABOUT_EG_STRING =
        "Viskit Event Graph Editor" + CR + "   version " + VERSION + CR + CR;
    
    public static final String ABOUT_ASSEMBLY_STRING =
        "Viskit Assembly Editor" + CR + "   version " + VERSION + CR + CR ;

    
    public static final String DEVELOPERS =         
        "(c) 2004-2005 under the Lesser GNU License" +CR +CR +
        "Developers:" + CR +
        "  Arnold Buss" + CR +
        "  Mike Bailey" + CR +
        "  Rick Goldberg" + CR +
        "  Don McGregor" + CR +
        "  Don Brutzman";
    

    private static HelpBroker hb;
    // A strange couple of things to support JavaHelp's rather strange design for CSH use:
    private static Component      tutorialComponent;
    private static ActionListener tutListenerLauncher;

    static
    {
      ClassLoader cl = viskit.Help.class.getClassLoader();
      URL helpSetURL = HelpSet.findHelpSet(cl, "viskit/javahelp/vHelpSet.hs");
      HelpSet hs = null;
      try {
        hs = new HelpSet(null, helpSetURL);
        hb = hs.createHelpBroker();
      }
      catch (HelpSetException e) {
        e.printStackTrace();
      }
      // Here we're setting up the action event peripherals for the tutorial menu selection
      tutorialComponent = new Button();
      tutListenerLauncher = new CSH.DisplayHelpFromSource(hb);
      CSH.setHelpIDString(tutorialComponent, "tutorial");
    }
  
    /** Creates a new instance of Help */
    public Help(Component parent) {
        this.parent = parent;
        icon = new ImageIcon(
            Thread.currentThread().getContextClassLoader().getResource(
                "viskit/images/ViskitLogo.png"
            )
        );
    }
    
    public void aboutEventGraphEditor() {
        JOptionPane.showMessageDialog(parent, ABOUT_EG_STRING +
            DEVELOPERS, "About Viskit Event Graph Editor...",
            JOptionPane.OK_OPTION, icon);
    }
    
    public void aboutAssemblyEditor() {
        JOptionPane.showMessageDialog(parent, ABOUT_ASSEMBLY_STRING +
            DEVELOPERS, "About Viskit Event Graph Editor...",
            JOptionPane.OK_OPTION, icon);
    }

    public void doContents()
    {
      hb.setDisplayed(true);
      hb.setCurrentView("TOC");
    }
    public void doSearch()
    {
      hb.setDisplayed(true);
      hb.setCurrentView("Search");
    }
    public void doTutorial()
    {
      ActionEvent ae = new ActionEvent(tutorialComponent,0,"tutorial");
      tutListenerLauncher.actionPerformed(ae);
    }
/*
    public void help() {
        JOptionPane.showMessageDialog(parent, "Viskit help not yet implemented",
            "Viskit Help", JOptionPane.OK_OPTION, icon);
    }
*/
  // message when the main frame has been positioned on the screen
  public void mainFrameLocated(Rectangle bounds)
  {
    Point p = new Point(bounds.x, bounds.y);
    Dimension d = new Dimension(bounds.width,bounds.height);
    Dimension hd = hb.getSize();
    hd.width+=100;
    hb.setSize(hd);
    p.x = p.x + d.width / 2 - hd.width / 2;
    p.y = p.y + d.height / 2 - hd.height / 2;
    hb.setLocation(p);
  }

}
