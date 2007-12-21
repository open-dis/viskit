package viskit.images;

import javax.swing.*;
import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.edu
 * By:   Mike Bailey
 * Date: May 25, 2004
 * Time: 8:03:58 PM
 */

public class EventNodeIcon implements Icon
{
  public int getIconHeight()
  {
    return 24;
  }

  public int getIconWidth()
  {
    return 24;
  }

  public void paintIcon(Component c, Graphics g, int x, int y)
  {
    Graphics2D g2d = (Graphics2D)g;
    Insets ins = new Insets(0,0,0,0);
    if(c instanceof JComponent)
      ins = ((JComponent)c).getInsets();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.translate(ins.left,ins.top);
    
    g2d.setColor(new Color(0xff,0xff,0xCC));
    g2d.fillOval(1,1,22,22);
    g2d.setColor(Color.black);
    g2d.drawOval(1,1,22,22);
    g2d.setStroke(new BasicStroke(2.0f));
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
    g2d.drawLine(19,16,19,22);
    g2d.drawLine(16,19,22,19);
  }
}
