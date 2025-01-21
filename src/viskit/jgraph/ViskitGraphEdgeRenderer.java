package viskit.jgraph;

import edu.nps.util.LogUtils;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.apache.logging.log4j.Logger;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.EdgeRenderer;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;

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
public class ViskitGraphEdgeRenderer extends EdgeRenderer {
    
    protected static final Logger LOG = LogUtils.getLogger(ViskitGraphEdgeRenderer.class);

    double[] coo = new double[6];

    // Override only to use a different way of positioning the label on the edge
    @Override
    public Point2D getLabelPosition(EdgeView view) {
        Point2D src = null, aim, labelPosn = null;
        int ret;
        double theta, newX, newY;

        setView(view);
        
        Shape s = view.sharedPath;
        if (s == null) {
            labelPosn = super.getLabelPosition(view);
        } else {
            for (PathIterator pi = s.getPathIterator(null); !pi.isDone();) {
                ret = pi.currentSegment(coo);
                if (ret == PathIterator.SEG_MOVETO)
                    src = new Point2D.Double(coo[0], coo[1]);

                if (ret == PathIterator.SEG_CUBICTO) {
                    aim = new Point2D.Double(coo[4], coo[5]);

                    if (src != null) {
                        theta = Math.atan2(aim.getY() - src.getY(), aim.getX() - src.getX());
                        newX = src.getX() + (Math.cos(theta) * 25);
                        newY = src.getY() + (Math.sin(theta) * 25);
                        labelPosn = new Point2D.Double(newX, newY);
                        break;
                    }
                }

                pi.next();
            }
        }

        if (labelPosn == null) {
            Rectangle2D tr = getPaintBounds(view);
            labelPosn = new Point2D.Double(tr.getCenterX(), tr.getCenterY()); // just use the center of the clip
        }
        
        GraphConstants.setLabelPosition(view.getAttributes(), labelPosn);
        
        return labelPosn;
    }

    /**
     * Sets view to work with, caching necessary values until the next call of
     * this method or until some other methods with explicitly specified
     * different view.
     * 
     * @param value the CellView the working view
     */
    void setView(CellView value) { // super.setView is not public, have to implement here
        if (value instanceof EdgeView) {
            view = (EdgeView) value;
            installAttributes(view);
        } else {
            view = null;
        }
    }
    
    @Override
    public boolean intersects(JGraph graph, CellView value, Rectangle rect) {
        if (value instanceof EdgeView && graph != null) {
            setView(value);

            // If we have three control points, we can get rid of hit
            // detection and do an intersection test on the two diagonals
            // of rect and the line between the two end points
            Graphics2D g2 = (Graphics2D) graph.getGraphics();
            if (g2 == null || view.getPointCount() == 2) {
                Point2D p0 = view.getPoint(0);
                Point2D p1 = view.getPoint(1);
                if (rect.intersectsLine(p0.getX(), p0.getY(), p1.getX(), p1.getY()))
                    return true;
                
            } else if (view.getShape().intersects(rect)) // <- This finally fixes the edge/listener selection on MBP displays 8/8/24 tdn
                return true;                               // Fixes Issue #1
            
            Rectangle2D r = getLabelBounds(graph, view);
            if (r != null && r.intersects(rect) && g2 != null) {
                boolean hits = true;

                // Performs exact hit detection on rotated labels
                if (EdgeRenderer.HIT_LABEL_EXACT) {
                    AffineTransform tx = g2.getTransform();

                    try {
                        String lab = graph.convertValueToString(view);
                        Point2D tmpPt = getLabelPosition(view);
                        Dimension size = getLabelSize(view, lab);
                        Rectangle2D tmp = new Rectangle((int) tmpPt.getX(), (int) tmpPt.getY(), size.width, size.height);

                        double cx = tmp.getCenterX();
                        double cy = tmp.getCenterY();

                        g2.translate(-size.width / 2, -size.height * 0.75
                                - metrics.getDescent());

                        boolean applyTransform = isLabelTransform(lab);
                        double angle;

                        if (applyTransform) {
                            angle = getLabelAngle(lab);
                            g2.rotate(angle, cx, cy);
                        }

                        hits = g2.hit(rect, tmp, false);
                    } finally {
                        g2.setTransform(tx);
                    }
                }

                if (hits)
                    return true;
                
            }
            Object[] labels = GraphConstants.getExtraLabels(view.getAllAttributes());
            if (labels != null) {
                for (int i = 0; i < labels.length; i++) {
                    r = getExtraLabelBounds(graph, view, i);
                    if (r != null && r.intersects(rect))
                        return true;                
                }
            }
        }
        return false;
    }
        
    /**
     * Estimates whether the transform for label should be applied. With the
     * transform, the label will be painted along the edge. To apply transform,
     * rotate graphics by the angle returned from {@link #getLabelAngle}
     *
     * @return true, if transform can be applied, false otherwise
     */
    private boolean isLabelTransform(String label) {
        if (!isLabelTransformEnabled()) {
            return false;
        }
        Point2D p = getLabelPosition(view);
        if (p != null && label != null && label.length() > 0) {
            int sw = metrics.stringWidth(label);
            Point2D p1 = view.getPoint(0);
            Point2D p2 = view.getPoint(view.getPointCount() - 1);
            double length = Math.sqrt((p2.getX() - p1.getX())
                    * (p2.getX() - p1.getX()) + (p2.getY() - p1.getY())
                    * (p2.getY() - p1.getY()));
            if (!(length <= Double.NaN || length < sw)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLabelTransformEnabled() {
        return labelTransformEnabled;
    }
    
    /**
     * Calculates the angle at which graphics should be rotated to paint label
     * along the edge. Before calling this method always check that transform
     * should be applied using {
     *
     * @linkisLabelTransform}
     *
     * @return the value of the angle, 0 if the angle is zero or can't be
     * calculated
     */
    private double getLabelAngle(String label) {
        Point2D p = getLabelPosition(view);
        double angle = 0;
        if (p != null && label != null && label.length() > 0) {
            int sw = metrics.stringWidth(label);
            // Note: For control points you may want to choose other
            // points depending on the segment the label is in.
            Point2D p1 = view.getPoint(0);
            Point2D p2 = view.getPoint(view.getPointCount() - 1);
            // Length of the edge
            double length = Math.sqrt((p2.getX() - p1.getX())
                    * (p2.getX() - p1.getX()) + (p2.getY() - p1.getY())
                    * (p2.getY() - p1.getY()));
            if (!(length <= Double.NaN || length < sw)) { // Label fits into
                // edge's length

                // To calculate projections of edge
                double cos = (p2.getX() - p1.getX()) / length;
                double sin = (p2.getY() - p1.getY()) / length;

                // Determine angle
                angle = Math.acos(cos);
                if (sin < 0) { // Second half
                    angle = 2 * Math.PI - angle;
                }
            }
            if (angle > Math.PI / 2 && angle <= Math.PI * 3 / 2) {
                angle -= Math.PI;
            }
        }
        return angle;
    }

} // end class ViskitGraphEdgeRenderer
