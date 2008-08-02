package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 26, 2004 : 
 * @since 8:43:39 AM
 * @version $Id$
 */
public class PropChangListIcon extends ImageIcon {

    int w, h;

    PropChangListIcon(int width, int height) {
        w = width;
        h = height;

        // This is so the Image obj can be extracted from this obj for dragging purposes
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        paintIcon(null, g, 0, 0);
        setImage(bi);
    }

    @Override
    public int getIconHeight() {
        return h;
    }

    @Override
    public int getIconWidth() {
        return w;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        Insets ins = new Insets(0, 0, 0, 0);
        if (c instanceof JComponent) {
            ins = ((JComponent) c).getInsets();
        }
        g2.translate(ins.left + x, ins.top + y);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        if (c != null) {
            g2.setColor(new Color(0xff, 0xc8, 0xc8)); //c.getBackground());
        } //c.getBackground());
        else {
            g2.setColor(new Color(0xff, 0xc8, 0xc8));
        }
        g2.fill3DRect(0, 0, w, h, true);
        g2.setColor(Color.black);
        g2.drawLine(1, 2, 9, 2);
        g2.drawLine(1, 9, 18, 9);
        g2.drawLine(1, 17, 9, 17);
        g2.drawLine(9, 2, 9, 17);
    }
}
