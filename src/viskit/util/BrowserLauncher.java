package viskit.util;

import edu.nps.util.LogUtils;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import javax.swing.event.HyperlinkEvent;
import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;
import javax.swing.event.HyperlinkListener;
import org.apache.log4j.Logger;
import viskit.VGlobals;

/**
 * @version $Id: BrowserLauncher.java 1860 2008-06-17 23:48:42Z ahbuss $
 * @author abuss
 */
public class BrowserLauncher implements HyperlinkListener {

    public static final String WINDOWS = "Windows";
    public static final String MAC = "Mac OS X";
    static Logger log = LogUtils.getLogger(BrowserLauncher.class);

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == ACTIVATED) {
            URL url = e.getURL();
            try {
                String osName = System.getProperty("os.name");
                if (osName.startsWith(WINDOWS)) {
                    Runtime.getRuntime().exec(
                            "rundll32 url.dll,FileProtocolHandler " +
                            url);
                } else if (osName.equals(MAC)) {
                    Class<?> clazz = VGlobals.instance().getWorkClassLoader().loadClass(
                            "com.apple.eio.FileManager");
                    Method method = clazz.getMethod("openURL", String.class);
                    method.invoke(null, url.toString());
                }
            }
            catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                log.error(ex);
            }
        }
    }
}
