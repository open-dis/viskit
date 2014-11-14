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
 * Time: 9:18:11 PM
 */

public class PropChangeListenerIcon implements Icon
{
  int width,height;
  public PropChangeListenerIcon(int w, int h)
  {
    width = w;
    height = h;
  }
  @Override
  public int getIconHeight()
  {
    return height;
  }

  @Override
  public int getIconWidth()
  {
    return width;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y)
  {
    Graphics2D g2d = (Graphics2D)g;
    Insets ins = new Insets(0,0,0,0);
    if(c instanceof JComponent)
      ins = ((JComponent)c).getInsets();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.translate(ins.left,ins.top);

    g2d.setColor(Color.black);
    g2d.drawLine(0,5,9,5);
    g2d.drawLine(0,19,9,19);
    g2d.drawLine(9,5,9,19);
    g2d.drawLine(0,12,23,12);
  }
}
