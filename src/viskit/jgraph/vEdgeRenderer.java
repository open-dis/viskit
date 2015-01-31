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
     * Override only to use a different way of positioning the label on the edge.
     * The default method doesn't do such a good job on quadratic edges.
     * @param view EdgeView for this edge
     * @return Center point of label
     */
    @Override
    public Point2D getLabelPosition(EdgeView view) {

        Shape s = view.sharedPath;
        Point2D src = null;
        Point2D aim;

        if (s == null) {
            return super.getLabelPosition(view);
        }

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
            }

            pi.next();
        }

        Rectangle2D tr = getPaintBounds(view);
        return new Point2D.Double(tr.getCenterX(), tr.getCenterY()); // just use the center of the clip
    }
}
