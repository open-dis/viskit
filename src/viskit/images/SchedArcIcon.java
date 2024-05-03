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
 * Time: 9:03:56 PM
 */

public class SchedArcIcon implements Icon
{
  @Override
  public int getIconHeight()
  {
    return 24;
  }

  @Override
  public int getIconWidth()
  {
    return 24;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y)
  {
    Graphics2D g2d = (Graphics2D)g;
    Insets ins = new Insets(0,0,0,0);
    if(c instanceof JComponent)
      ins = ((Container)c).getInsets();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.translate(ins.left,ins.top);

    g2d.setColor(Color.black);
    g2d.drawOval(0,15,8,8);
    g2d.drawOval(15,0,8,8);
    g2d.drawLine(12,8,16,8);
    g2d.drawLine(16,8,16,12);

    g2d.drawLine(7,17,17,7);
  }
}
