package viskit.jgraph;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jgraph.graph.EdgeRenderer;
import org.jgraph.graph.EdgeView;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey
 * @since Feb 24, 2004
 * @since 2:32:30 PM
 * @version $Id$
 *
 * The guy that actually paints edges.
 */
public class vEdgeRenderer extends EdgeRenderer {
        
    double[] coo = new double[6];

    /**
     * Override only to use a different way of positioning the label on the edge.  The default method
     * doesn't do such a good job on quadratic edges.
     * @param view EdgeView for this edge
     * @return Center point of label
     */
    @Override
    public Point2D getLabelPosition(EdgeView view) {
        super.getLabelPosition(view);  // incase of side effects, but forget the return
        //view.sharedPath.

        Shape s = view.sharedPath;
        Point2D src = null;
        Point2D aim;

        for (PathIterator pi = s.getPathIterator(null); !pi.isDone();) {
            int ret = pi.currentSegment(coo);
            if (ret == PathIterator.SEG_MOVETO) {
                src = new Point2D.Double(coo[0], coo[1]);
            }

            if (ret == PathIterator.SEG_CUBICTO) {
                aim = new Point2D.Double(coo[4], coo[5]);

                double theta = Math.atan2(aim.getY() - src.getY(), aim.getX() - src.getX());
                double newX = src.getX() + (Math.cos(theta) * 25);
                double newY = src.getY() + (Math.sin(theta) * 25);
                return new Point2D.Double(newX, newY);


            // make it the midpoint of the straight line between the control points
            /*
            int dx = (int)coo[2] - (int) coo[0];
            int dy = (int)coo[3] - (int) coo[1];
            return new Point((int)coo[0] + dx/2,(int)coo[1] + dy/2);
            */
            }

            pi.next();
        }

        Rectangle2D tr = getPaintBounds(view);
        return new Point2D.Double(tr.getCenterX(), tr.getCenterY()); // just use the center of the clip
    }
}
