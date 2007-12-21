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

public class AdapterIcon implements Icon
{
  int width,height;
  public AdapterIcon(int w, int h)
  {
    width = w;
    height = h;
  }
  public int getIconHeight()
  {
    return height;
  }

  public int getIconWidth()
  {
    return width;
  }

  public void paintIcon(Component c, Graphics g, int x, int y)
  {
    Graphics2D g2d = (Graphics2D)g;
    Insets ins = new Insets(0,0,0,0);
    if(c instanceof JComponent)
      ins = ((JComponent)c).getInsets();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.translate(ins.left,ins.top);

    g2d.setColor(Color.black);
    g2d.drawLine(0,5,8,10);
    g2d.drawLine(0,19,8,14);
    g2d.drawLine(0,10,23,10);
    g2d.drawLine(0,14,23,14);
  }
}
