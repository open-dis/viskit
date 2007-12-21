package viskit.jgraph;

import org.jgraph.graph.EdgeRenderer;
import org.jgraph.graph.EdgeView;

import java.awt.*;
import java.awt.geom.PathIterator;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * User: mike
 * Date: Feb 24, 2004
 * Time: 2:32:30 PM
 */

/**
 * The guy that actually paints edges.
 */
public class vEdgeRenderer extends EdgeRenderer
{

  /**
   * Override only to use a different way of positioning the label on the edge.  The default method
   * doesn't do such a good job on quadradic edges.
   * @param view EdgeView for this edge
   * @return Center point of label
   */

  public Point getLabelPosition(EdgeView view)
  {
    super.getLabelPosition(view);  // invcase of side effects, but forget the return
    //view.sharedPath.

    Shape s = view.sharedPath;
    Point src = null;
    Point aim;

    for(PathIterator pi = s.getPathIterator(null); !pi.isDone();)
    {
      int ret = pi.currentSegment(coo);
      if(ret == PathIterator.SEG_MOVETO) {
        src = new Point((int)coo[0],(int)coo[1]);
      }

      if(ret == PathIterator.SEG_CUBICTO) {
        aim = new Point((int)coo[4],(int)coo[5]);

        double theta = Math.atan2(aim.getY()-src.getY(),aim.getX()-src.getX());
        int newX = src.x + (int)(Math.cos(theta)*25);
        int newY = src.y + (int)(Math.sin(theta)*25);
        return new Point(newX,newY);


        // make it the midpoint of the straight line between the control points
/*
        int dx = (int)coo[2] - (int) coo[0];
        int dy = (int)coo[3] - (int) coo[1];

        return new Point((int)coo[0] + dx/2,(int)coo[1] + dy/2);
*/
      }

      pi.next();
    }

    Rectangle tr = getPaintBounds(view);
    return new Point(tr.x+tr.width/2, tr.y+tr.height/2); // just use the center of the clip
  }
  double[] coo = new double[6];

}
