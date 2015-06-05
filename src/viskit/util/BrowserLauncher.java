package viskit.util;

import edu.nps.util.LogUtils;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.help.JHelpContentViewer;
import javax.help.plaf.basic.BasicContentViewerUI;
import javax.swing.JComponent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.plaf.ComponentUI;
import org.apache.log4j.Logger;

/**
 * @version $Id: BrowserLauncher.java 1860 2008-06-17 23:48:42Z ahbuss $
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.util.BrowserLauncher">Terry Norbraten, NPS MOVES</a>
 */
public class BrowserLauncher extends BasicContentViewerUI {

    static final Logger LOG = LogUtils.getLogger(BrowserLauncher.class);

    public static ComponentUI createUI(JComponent x) {
        return new BrowserLauncher((JHelpContentViewer) x);
    }

    public BrowserLauncher(JHelpContentViewer b) {
        super(b);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == EventType.ACTIVATED) {
            URL url = e.getURL();
            try {
                Desktop.getDesktop().browse(url.toURI());
            } catch (IOException | URISyntaxException ex) {
                LOG.error(ex);
            }
        }
    }
}
