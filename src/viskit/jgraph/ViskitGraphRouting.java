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

import edu.nps.util.Log4jUtilities;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.PortView;

import viskit.model.EventGraphNode;
import viskit.model.EventNode;
import viskit.model.PropertyChangeListenerNode;
import viskit.model.ViskitElement;

/**
 * A replacement class to tweak the routing slightly so that the edges come into
 * the node from directions other than NSE and W if there are more than one edge
 * to/from a node.This class will also provide a unique control point to give
 a distinctive parabolic shape to the edge when there are more than one edge
 to/from a node.
 *
 * @author Mike Bailey
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.jgraph.ViskitGraphRouting">Terry Norbraten, NPS MOVES</a>
 * @version $Id$
 */
public class ViskitGraphRouting implements org.jgraph.graph.DefaultEdge.Routing 
{
    static final Logger LOG = LogManager.getLogger();

    Map<String, Vector<Object>> nodePairs = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked") // JGraph not genericized
    public List route(GraphLayoutCache glc, EdgeView edge) {

        List points = edge.getPoints();
        Object fromKey = null, toKey = null;

        Point2D from;

        if (edge.getSource() instanceof PortView) {
            from = ((PortView) edge.getSource()).getLocation();
            fromKey = getKey((PortView) edge.getSource());
        }/* else if (edge.getSource() != null) {
            Rectangle2D b = edge.getSource().getBounds();
            from = edge.getAttributes().createPoint(b.getCenterX(),
                    b.getCenterY());
        }*/ else {
            from = (Point2D) points.get(0);
        }

        Point2D to;

        if (edge.getTarget() instanceof PortView) {
            to = ((PortView) edge.getTarget()).getLocation();
            toKey = getKey((PortView) edge.getTarget());
        }/* else if (edge.getTarget() != null) {
            Rectangle2D b = edge.getTarget().getBounds();
            to = edge.getAttributes().createPoint(b.getCenterX(),
                    b.getCenterY());
        }*/ else {
            to = (Point2D) points.get(points.size() - 1);
        }

        int adjustFactor = 0;
        if (toKey != null && fromKey != null) {
            adjustFactor = getFactor(toKey, fromKey, edge);
        }

        // Not sure what this block does is relevant, but it's not hurting anything
        int sig = adjustFactor % 2;
        adjustFactor++;
        adjustFactor /= 2;
        if (sig == 0) {
            adjustFactor *= -1;
        }

        double dx = Math.abs(from.getX() - to.getX());
        double dy = Math.abs(from.getY() - to.getY());

        // Offset adjustments so that we and edge does not start from the center of a node
        double x1 = from.getX() + ((to.getX() - from.getX()) / 2);
        double y1 = from.getY() + ((to.getY() - from.getY()) / 2);
        double x2 = to.getX() + ((from.getX() - to.getX()) / 2);
        double y2 = to.getY() + ((from.getY() - to.getY()) / 2);

        // Handle beginning Edge point placement (not worried about AssemblyEdge)
        Object ed = edge.getSource();
        vPortCell vCell = null;
        if (ed instanceof PortView) {
            if (((CellView) ed).getCell() instanceof vPortCell) {
                vCell = (vPortCell) ((CellView) ed).getCell();

                // If we have more than two edges to/from this node, then bias
                // the parabolic control points
                if (vCell.getEdges().size() > 2) {
                    if (adjustFactor == 0) {
                        adjustFactor -= 1;
                    }
                }
            }
        }

        // bias control for parabola effect
        int adjustment = 55 * adjustFactor;

        Point2D[] routed = new Point2D[2];

        // determine orthoganality between nodes
        boolean adjust = (dx == 0 || dy == 0);

        if (adjust) {
            if (dx > dy) {
                routed[0] = edge.getAllAttributes().createPoint(x1, from.getY() + adjustment);
                routed[1] = edge.getAllAttributes().createPoint(x1, to.getY() + adjustment);
            } else {
                routed[0] = edge.getAllAttributes().createPoint(from.getX() + adjustment, y1);
                routed[1] = edge.getAllAttributes().createPoint(to.getX() + adjustment, y1);
            }
        } else {
            routed[0] = edge.getAllAttributes().createPoint(x1, y1);
            routed[1] = edge.getAllAttributes().createPoint(x2, y2);
        }

        // Set/Add unique control points
        for (int i = 0; i < routed.length; i++) {

            // This call gets SEs & CEs to draw in a parabola
            if (vCell != null && points.contains(routed[i])) {continue;}

            if (points.size() > i + 2) {
                points.set(i + 1, routed[i]);
            } else {
                points.add(i + 1, routed[i]);
            }
        }

        return points;
    }

    private Object getKey(PortView pv) {
        Object o = pv.getParentView();

        if (o instanceof VertexCircleView) {
            VertexCircleView cv = (VertexCircleView) pv.getParentView();
            vCircleCell cc = (vCircleCell) cv.getCell();
            EventNode en = (EventNode) cc.getUserObject();
            return en.getModelKey();
        } else if (o instanceof ViskitAssemblyCircleView) {
            ViskitAssemblyCircleView cv = (ViskitAssemblyCircleView) o;
            ViskitAssemblyCircleCell cc = (ViskitAssemblyCircleCell) cv.getCell();
            EventGraphNode egn = (EventGraphNode) cc.getUserObject();
            return egn.getModelKey();
        } else if (o instanceof ViskitAssemblyPropListView) {
            ViskitAssemblyPropListView apv = (ViskitAssemblyPropListView) o;
            vAssemblyPropertyListCell apc = (vAssemblyPropertyListCell) apv.getCell();
            PropertyChangeListenerNode pn = (PropertyChangeListenerNode) apc.getUserObject();
            return pn.getModelKey();
        } else {
            LOG.warn("ParentView of " + pv + " is " + o);
            return null;
        }
    }

    private int getFactor(Object toKey, Object fromKey, EdgeView ev) {
        String toStr = toKey.toString();
        String fromStr = fromKey.toString();
        String masterKey;
        if (toStr.compareTo(fromStr) > 0) {
            masterKey = fromStr + "-" + toStr;
        } else {
            masterKey = toStr + "-" + fromStr;
        }

        Object edgeKey = getElement(ev).getModelKey();

        Vector<Object> lis = nodePairs.get(masterKey);
        if (lis == null) {
            // never had an edge between these 2 before
            Vector<Object> v = new Vector<>();
            v.add(edgeKey);
            //LOG.info("adding edgekey in "+masterKey + " "+ edgeKey);
            nodePairs.put(masterKey, v);
                return 0;
        }

        // Here if there has been a previous edge between the 2, maybe just this one
        if (!lis.contains(edgeKey)) {
            lis.add(edgeKey);
            //LOG.info("adding edgekey in "+masterKey + " "+ edgeKey);
        }
        return lis.indexOf(edgeKey);
    }

    private ViskitElement getElement(EdgeView ev) {
        DefaultEdge vec;
        ViskitElement edg = null;
        if (ev.getCell() instanceof vEdgeCell) {
            vec = (DefaultEdge) ev.getCell();
            edg = (ViskitElement) vec.getUserObject();
        } else if (ev.getCell() instanceof ViskitAssemblyEdgeCell) {
            vec = (DefaultEdge) ev.getCell();
            edg = (ViskitElement) vec.getUserObject();
        }
        return edg;
    }

    // NOTE: This preference ensures symmetric, bendable edges
    @Override
    public int getPreferredLineStyle(EdgeView ev) {
        return GraphConstants.STYLE_BEZIER;
    }

} // end class file ViskitGraphRouting.java
