/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.jgraph;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jgraph.graph.GraphConstants;
import viskit.ViskitStatics;

import viskit.model.Edge;
import viskit.model.EventNode;
import viskit.model.ViskitElement;

/**
 * Class to draw the self-referential edges as an arc attached to the node.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.jgraph.ViskitGraphSelfEdgeRenderer">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
class ViskitGraphSelfEdgeRenderer extends ViskitGraphEdgeRenderer {

    private final double circleDiam = 30.0d;
    private Arc2D arc;

    @Override
    @SuppressWarnings("unchecked") // JGraph not genericized
    protected Shape createShape() {
        VertexCircleView myCircle = (VertexCircleView) view.getSource().getParentView();
        Rectangle2D circBnds = myCircle.getBounds();
        double circCenterX = circBnds.getCenterX();
        double circCenterY = circBnds.getCenterY();
        double topCenterX = circCenterX - circleDiam / 2;
        double topCenterY = circBnds.getY() + circBnds.getHeight() - 7;  // 7 pixels up

        AffineTransform rotater = new AffineTransform();
        rotater.setToRotation(getAngle(), circCenterX, circCenterY);

        if (view.sharedPath == null) {
            double ex = topCenterX;
            double ey = topCenterY;
            double ew = circleDiam;
            double eh = circleDiam;
            arc = new Arc2D.Double(ex, ey, ew, eh, 135.0d, 270.0d, Arc2D.OPEN); // angles: start, extent
            view.sharedPath = new GeneralPath(arc);
            view.sharedPath = new GeneralPath(view.sharedPath.createTransformedShape(rotater));
        } else {
            view.sharedPath.reset();
        }

        view.beginShape = view.lineShape = view.endShape = null;

        Point2D p2start = arc.getStartPoint();
        Point2D p2end = arc.getEndPoint();
        Point2D pstart = new Point2D.Double(p2start.getX(), p2start.getY());
        Point2D pend = new Point2D.Double(p2end.getX(), p2end.getY());

        if (beginDeco == GraphConstants.ARROW_NONE) {
            view.beginShape = createLineEnd(beginSize, beginDeco, pstart, new Point2D.Double(pstart.getX() + 15, pstart.getY() + 15));
            view.beginShape = rotater.createTransformedShape(view.beginShape);
        }
        if (endDeco == GraphConstants.ARROW_TECHNICAL) {
            view.endShape = createLineEnd(endSize, endDeco, new Point2D.Double(pend.getX() + 15, pend.getY() + 25), pend);
            view.endShape = rotater.createTransformedShape(view.endShape);
        }

        if (view.endShape == null && view.beginShape == null) {

            // With no end decorations the line shape is the same as the
            // shared path and memory
            view.lineShape = view.sharedPath;
        } else {

            view.lineShape = (Shape) view.sharedPath.clone();

            if (view.endShape != null) 
                view.sharedPath.append(view.endShape, true);
            
            if (view.beginShape != null)
                view.sharedPath.append(view.beginShape, true);
        }
        
        // Now shift the orig. control points to rotate with the offset shape
        List cntlpts = view.getPoints();
        Shape s = view.sharedPath;
        double[] coords = new double[6];
        Point2D src, aim, cp1, cp2, cp3;
        int ret, ix = 0;
        if (cntlpts.size() == 5) {
            for (PathIterator pi = s.getPathIterator(null); !pi.isDone();) {
                ret = pi.currentSegment(coords);
                switch (ret) {
                    case PathIterator.SEG_MOVETO:
                        src = new Point2D.Double(coords[0], coords[1]);
                        cntlpts.set(0, src);
                        LOG.debug("SEG_MOVETO  coords: {}", coords);
                        break;
                    case PathIterator.SEG_LINETO:
                        LOG.debug("SEG_LINETO  coords: {}", coords);
                        break;
                    case PathIterator.SEG_QUADTO:
                        LOG.debug("SEG_QUADTO coords: {}", coords);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        if (ix == 2) {
                            cp1 = new Point2D.Double(coords[0], coords[1]);
                            cntlpts.set(1, cp1);
                            cp2 = new Point2D.Double(coords[2], coords[3]);
                            cntlpts.set(2, cp2);
                            cp3 = new Point2D.Double(coords[4], coords[5]);
                            cntlpts.set(3, cp3);
                            LOG.debug("SEG_CUBICTO coords: {}", coords);
                        }
                        break;
                    case PathIterator.SEG_CLOSE:
                        aim = new Point2D.Double(coords[4], coords[5]);
                        cntlpts.set(4, aim);
                        LOG.debug("SEG_CLOSE   coords: {}", coords);
                        break;
                    default:
                        LOG.debug("default coords: {}", coords);
                        break;
                }
                pi.next();
                ix++;
            }
            if (ViskitStatics.debug)
                System.out.println();
        }

        return view.sharedPath;
    }

    /**
     * Defines how much we increment the angle calculated in getAngle() for each
     * self-referential edge discovered.  Since we want to advance 3/8 of
     * a circle for each edge, the value below should be Pi that increments by
     * Pi * 3/8.
     * But since the iterator in getAngle discovers each edge twice (since the
     * edge has a connection to both its head and tail, the math works out to
     * rotate only half that much.
     */
    private final static double ROT_INCR = Math.PI;

    /**
     * This method will determine if there are other self-referential edges
     * attached to this node and try to return a different angle for different
     * edges, so they will be rendered at different "clock" points around the
     * node circle.
     *
     * @return a different angle for each self-referential edge
     */
    private double getAngle() {
        vEdgeCell vec = (vEdgeCell) view.getCell();
        Edge edg = (Edge) vec.getUserObject();

        vCircleCell vcc = (vCircleCell) view.getSource().getParentView().getCell();
        EventNode en = (EventNode) vcc.getUserObject();
        double retd = ROT_INCR;
        Edge e;
        for (ViskitElement ve : en.getConnections()) {
            e = (Edge) ve;
            if (e.to == en && e.from == en) {
                retd += (ROT_INCR * 3/8);
                if (e == edg) {
                    return retd;
                }
            }
        }
        return 0.0d;      // should always find one
    }

} // end class ViskitGraphSelfEdgeRenderer
