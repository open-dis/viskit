package viskit;
import java.awt.*;
import javax.swing.*;
/**
 *
 * @author  ahbuss
 */
public class Help {
    
    private Component parent;
    private Icon icon;
    
    private static final String CR = System.getProperty("line.separator");
    
    public static final String ABOUT_STRING =
        "Viskit Event Graph Editor" + CR + CR +
        "(c) 2004 under the Lesser GNU License" +CR +CR +
        "Developers:" + CR +
        "  Arnold Buss" + CR +
        "  Mike Bailey" + CR +
        "  Rick Goldberg";
    
    /** Creates a new instance of Help */
    public Help(Component parent) {
        this.parent = parent;
        icon = new ImageIcon(
            Thread.currentThread().getContextClassLoader().getResource(
                "viskit/images/ViskitLogo.png"
            )
        );
    }
    
    public void about() {
        JOptionPane.showMessageDialog(parent, ABOUT_STRING, "About Viskit...",
            JOptionPane.OK_OPTION, icon);
    }
    
    public void help() {
        JOptionPane.showMessageDialog(parent, "Viskit help not yet implemented",
            "Viskit Help", JOptionPane.OK_OPTION, icon);
    }
    
}
