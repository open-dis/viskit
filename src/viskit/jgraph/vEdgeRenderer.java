package viskit.jgraph;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jgraph.graph.CellView;
import org.jgraph.graph.EdgeRenderer;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.util.Bezier;
import org.jgraph.util.Spline2D;

/**
 * The guy that actually paints edges.
 *
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * @author Mike Bailey
 * @since Feb 24, 2004
 * @since 2:32:30 PM
 * @version $Id$
 */
public class vEdgeRenderer extends EdgeRenderer {

    double[] coo = new double[6];

    /**
     * Override only to use a different way of positioning the label on the edge.
     * The default method doesn't do such a good job on quadratic edges.
     * @param view EdgeView for this edge
     * @return center point of label
     */
    @Override
    public Point2D getLabelPosition(EdgeView view) {

        setView(view);

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

    void setView(CellView value) {
        if (value instanceof EdgeView) {
            view = (EdgeView) value;
            installAttributes(view);
        } else {
            view = null;
        }
    }

    /**
     * Returns the shape that represents the current edge in the context of the
     * current graph. This method sets the global beginShape, lineShape and
     * endShape variables as a side-effect.
     * @return shape object for this edge
     */
    @Override
    protected Shape createShape() {
        int n = view.getPointCount();
        if (n > 1) {
			// Following block may modify static vars as side effect (Flyweight
            // Design)
            EdgeView tmp = view;
            Point2D[] p = new Point2D[n];
            for (int i = 0; i < n; i++) {
                Point2D pt = tmp.getPoint(i);
                if (pt == null) {
                    return null; // exit
                }
                p[i] = new Point2D.Double(pt.getX(), pt.getY());
            }

	    // End of Side-Effect Block
            // Undo Possible MT-Side Effects
            if (view != tmp) {
                view = tmp;
                installAttributes(view);
            }
            // End of Undo
            if (view.sharedPath == null) {
                view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
            } else {
                view.sharedPath.reset();
            }
            view.beginShape = view.lineShape = view.endShape = null;
            Point2D p0 = p[0];
            Point2D pe = p[n - 1];
            Point2D p1 = p[1];
            Point2D p2 = p[n - 2];

            if (lineStyle == GraphConstants.STYLE_BEZIER && n > 2) {
                bezier = new Bezier(p);
                p2 = bezier.getPoint(bezier.getPointCount() - 1);
            } else if (lineStyle == GraphConstants.STYLE_SPLINE && n > 2) {
                spline = new Spline2D(p);
                double[] point = spline.getPoint(0.9875);
				// Extrapolate p2 away from the end point, pe, to avoid integer
                // rounding errors becoming too large when creating the line end
                double scaledX = pe.getX() - ((pe.getX() - point[0]) * 128);
                double scaledY = pe.getY() - ((pe.getY() - point[1]) * 128);
                p2.setLocation(scaledX, scaledY);
            }

            if (beginDeco != GraphConstants.ARROW_NONE) {
                view.beginShape = createLineEnd(beginSize, beginDeco, p1, p0);
            }
            if (endDeco != GraphConstants.ARROW_NONE) {
                view.endShape = createLineEnd(endSize, endDeco, p2, pe);
            }

            // Had to regenerate the sharedPath as it was going null here (TDN)
            if (view.sharedPath == null)
                view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);

            view.sharedPath.moveTo((float) p0.getX(), (float) p0.getY());
            /* THIS CODE WAS ADDED BY MARTIN KRUEGER 10/20/2003 */
            if (lineStyle == GraphConstants.STYLE_BEZIER && n > 2) {
                Point2D[] b = bezier.getPoints();
                view.sharedPath.quadTo((float) b[0].getX(),
                        (float) b[0].getY(), (float) p1.getX(), (float) p1
                        .getY());
                for (int i = 2; i < n - 1; i++) {
                    Point2D b0 = b[2 * i - 3];
                    Point2D b1 = b[2 * i - 2];
                    view.sharedPath.curveTo((float) b0.getX(), (float) b0
                            .getY(), (float) b1.getX(), (float) b1.getY(),
                            (float) p[i].getX(), (float) p[i].getY());
                }
                view.sharedPath.quadTo((float) b[b.length - 1].getX(),
                        (float) b[b.length - 1].getY(),
                        (float) p[n - 1].getX(), (float) p[n - 1].getY());
            } else if (lineStyle == GraphConstants.STYLE_SPLINE && n > 2) {
                for (double t = 0; t <= 1; t += 0.0125) {
                    double[] xy = spline.getPoint(t);
                    view.sharedPath.lineTo((float) xy[0], (float) xy[1]);
                }
            } /* END */
            else {
                for (int i = 1; i < n - 1; i++) {
                    view.sharedPath.lineTo((float) p[i].getX(), (float) p[i]
                            .getY());
                }

                // Had to regenerate the sharedPath as it was going null here (TDN)
                if (view.sharedPath == null)
                    view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);

                view.sharedPath.lineTo((float) pe.getX(), (float) pe.getY());
            }
            view.sharedPath.moveTo((float) pe.getX(), (float) pe.getY());
            if (view.endShape == null && view.beginShape == null) {

                // With no end decorations the line shape is the same as the
                // shared path and memory
                view.lineShape = view.sharedPath;
            } else {
                view.lineShape = (GeneralPath) view.sharedPath.clone();
                if (view.endShape != null) {
                    view.sharedPath.append(view.endShape, true);
                }
                if (view.beginShape != null) {
                    view.sharedPath.append(view.beginShape, true);
                }
            }
            return view.sharedPath;
        }
        return null;
    }

} // end class file vEdgeRenderer.java
