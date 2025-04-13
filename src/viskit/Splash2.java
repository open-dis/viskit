package viskit;

import edu.nps.util.Log4jUtilities;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 13, 2004
 * @since 9:19:25 AM
 *
 * Based on code posted by Stanislav Lapitsky, ghost_s@mail.ru, posted on the Sun developer forum.  Feb 9, 2004.
 * @deprecated invoke with {@link viskit.EventGraphAssemblyComboMain} directly
 */
@Deprecated(since = "1.0.16", forRemoval = true) // TODO huh? seems to still be in use...
public class Splash2 extends JFrame
{
    static final Logger LOG = LogManager.getLogger();

    Robot robot;
    BufferedImage screenImg;
    Rectangle screenRect;
    MyPanel contentPanel = new MyPanel();
    private static JProgressBar progressBar;
    boolean userActivate = false;

    public Splash2() {
        super();
        setUndecorated(true);

        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("viskit/images/ViskitSplash2.png"));
        JLabel label = new JLabel(icon);
        label.setOpaque(false);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("Loading...");
        contentPanel.add(label, BorderLayout.CENTER);

        Container contentPane = getContentPane();
        createScreenImage();
        contentPane.add(contentPanel, BorderLayout.CENTER);
        contentPane.add(progressBar, BorderLayout.SOUTH);

        this.pack();

        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(ComponentEvent e) {
                resetUnderImg();
                repaint();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                resetUnderImg();
                repaint();
            }
        });

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                if (userActivate) {
                    userActivate = false;
                    Splash2.this.setVisible(false);
                    createScreenImage();
                    resetUnderImg();
                    Splash2.this.setVisible(true);
                } else {
                    userActivate = true;
                }
            }
        });
    }

    private void createScreenImage() {
        try {
            if (robot == null) {
                robot = new Robot();
            }
        } catch (AWTException ex) {
            LOG.error(ex);
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenRect = new Rectangle(0, 0, screenSize.width, screenSize.height);
        screenImg = robot.createScreenCapture(screenRect);
    }

    private void resetUnderImg() {
        if (robot != null && screenImg != null) {
            Rectangle frameRect = getBounds();
            int x = frameRect.x; // + 4;
            contentPanel.paintX = 0;
            contentPanel.paintY = 0;
            if (x < 0) {
                contentPanel.paintX = -x;
                x = 0;
            }
            int y = frameRect.y; // + 23;
            if (y < 0) {
                contentPanel.paintY = -y;
                y = 0;
            }
            int w = frameRect.width; // - 10;
            if (x + w > screenImg.getWidth()) {
                w = screenImg.getWidth() - x;
            }
            int h = frameRect.height; // - 23 - 5;
            if (y + h > screenImg.getHeight()) {
                h = screenImg.getHeight() - y;
            }

            contentPanel.underFrameImg = screenImg.getSubimage(x, y, w, h);
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_main = "main";

    public static void main(String[] args)
    {
        if (viskit.ViskitStatics.debug) {
            LOG.info("java.class.path=" + System.getProperty("java.class.path"));
        }

        final Splash2 splash2 = new Splash2();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        splash2.setLocation((d.width - splash2.getWidth()) / 2, (d.height - splash2.getHeight()) / 2);

        SwingUtilities.invokeLater(() -> {
            splash2.setVisible(true);
        });

        // This is for the launch4j executable for Win & executable jar for Unix
        if (args.length == 0) {
            args = new String[] {"viskit.EventGraphAssemblyComboMain"};
        }

        // First argument is main class
        String target = args[0];
        int newLen = args.length - 1;
        String[] newArgs = new String[newLen];
        for (int i = 0; i < newLen; i++) {
            newArgs[i] = args[i + 1];
        }

        // this is used to give us some min splash viewing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        progressBar.setString("Starting Viskit...");
        try {

            // Call the main() method of the application using reflection
            Object[] arguments = new Object[] {newArgs};
            Class<?>[] parameterTypes = new Class[] {newArgs.getClass()};

            Class<?> mainClass = ViskitStatics.classForName(target);

            Method mainMethod = mainClass.getMethod(METHOD_main, parameterTypes);
            mainMethod.invoke(null, arguments);
        } 
        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.error(ex);
        }
        progressBar.setString("Complete");

        SwingUtilities.invokeLater(() -> {
            splash2.dispose();
        });
    }

    /** Invoked on the command line by viskit.Splash2$DefaultEntry **/
    public static class DefaultEntry {
        public static void main(String[] args) {
            Splash2.main(new String[] {"viskit.EventGraphAssemblyComboMain"});
        }
    }

}

class MyPanel extends JPanel {

    BufferedImage underFrameImg;
    int paintX = 0;
    int paintY = 0;

    public MyPanel() {
        super();
        setOpaque(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(underFrameImg, paintX, paintY, null);
    }
}