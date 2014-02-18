/*
 Copyright (c) 1995-2014 held by the author(s).  All rights reserved.

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
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
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

import edu.nps.util.LogUtils;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.PortView;
import viskit.model.AssemblyEdge;
import viskit.model.EvGraphNode;
import viskit.model.EventNode;
import viskit.model.PropChangeListenerNode;
import viskit.model.ViskitElement;

/**
 * A replacement class to tweak the routing slightly so that the edges come into
 * the node from other directions than NSE and W. Also, support offsetting edges
 * between the same two nodes.
 * @author Mike Bailey
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.jgraph.ViskitRouting">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class vRouting implements org.jgraph.graph.DefaultEdge.Routing {

    Map<String, Vector<Object>> nodePairs = new HashMap<String, Vector<Object>>();

    @Override
    @SuppressWarnings("unchecked") // JGraph not genericized
    public List route(GraphLayoutCache glc, EdgeView edge) {

        List points = edge.getPoints();
        Object fromKey = null, toKey = null;

        Point2D from;

        if (edge.getSource() instanceof PortView) {
            from = ((PortView) edge.getSource()).getLocation();
            fromKey = getKey((PortView) edge.getSource());
        } else if (edge.getSource() != null) {
            Rectangle2D b = edge.getSource().getBounds();
            from = edge.getAttributes().createPoint(b.getCenterX(),
                    b.getCenterY());
        } else {
            from = (Point2D) points.get(0);
        }

        Point2D to;

        if (edge.getTarget() instanceof PortView) {
            to = ((PortView) edge.getTarget()).getLocation();
            toKey = getKey((PortView) edge.getTarget());
        } else if (edge.getTarget() != null) {
            Rectangle2D b = edge.getTarget().getBounds();
            to = edge.getAttributes().createPoint(b.getCenterX(),
                    b.getCenterY());
        } else {
            to = (Point2D) points.get(points.size() - 1);
        }

        int adjustFactor = 0;
        if (toKey != null && fromKey != null) {
            adjustFactor = getFactor(toKey, fromKey, edge);
        }

        int sig = adjustFactor % 2;
        adjustFactor++;
        adjustFactor /= 2;
        if (sig == 0) {
            adjustFactor *= -1;
        }

        int adjustment = 35 * adjustFactor;       // little bias

        double dx = Math.abs(from.getX() - to.getX());
        double dy = Math.abs(from.getY() - to.getY());
        double x2 = from.getX() + ((to.getX() - from.getX()) / 2);
        double y2 = from.getY() + ((to.getY() - from.getY()) / 2);
        Point2D[] routed = new Point2D[2];
        if (dx > dy) {
            routed[0] = edge.getAllAttributes().createPoint(x2, from.getY() + adjustment);
            routed[1] = edge.getAllAttributes().createPoint(x2, to.getY() - adjustment);
        } else {
            routed[0] = edge.getAllAttributes().createPoint(from.getX() - adjustment, y2);
            routed[1] = edge.getAllAttributes().createPoint(to.getX() + adjustment, y2);
        }

        // Set/Add Points
        for (int i = 0; i < routed.length; i++) {
            if (points.size() > i + 2) {
                points.set(i + 1, routed[i]);
            } else {
                points.add(i + 1, routed[i]);
            }
        }

        // Remove spare points
        while (points.size() > routed.length + 2) {
            points.remove(points.size() - 2);
        }

        return points;
    }

    private Object getKey(PortView pv) {
        Object o = pv.getParentView();

        if (o instanceof CircleView) {
            CircleView cv = (CircleView) pv.getParentView();
            CircleCell cc = (CircleCell) cv.getCell();
            EventNode en = (EventNode) cc.getUserObject();
            return en.getModelKey();
        } else if (o instanceof AssemblyCircleView) {
            AssemblyCircleView cv = (AssemblyCircleView) o;
            AssemblyCircleCell cc = (AssemblyCircleCell) cv.getCell();
            EvGraphNode egn = (EvGraphNode) cc.getUserObject();
            return egn.getModelKey();
        } else if (o instanceof AssemblyPropListView) {
            AssemblyPropListView apv = (AssemblyPropListView) o;
            AssemblyPropListCell apc = (AssemblyPropListCell) apv.getCell();
            PropChangeListenerNode pn = (PropChangeListenerNode) apc.getUserObject();
            return pn.getModelKey();
        } else {
            LogUtils.getLogger(getClass()).warn("ParentView of " + pv + " is " + o);
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

        DefaultEdge vec;
        ViskitElement edg = null;
        if (ev.getCell() instanceof vEdgeCell) {
            vec = (vEdgeCell) ev.getCell();
            edg = (viskit.model.Edge) vec.getUserObject();
        } else if (ev.getCell() instanceof vAssemblyEdgeCell) {
            vec = (vAssemblyEdgeCell) ev.getCell();
            edg = (AssemblyEdge) vec.getUserObject();
        }

        Object edgeKey = edg.getModelKey();

        Vector<Object> lis = nodePairs.get(masterKey);
        if (lis == null) {
            // never had an edge between these 2 before
            Vector<Object> v = new Vector<Object>();
            v.add(edgeKey);
            //System.out.println("adding edgekey in "+masterKey + " "+ edgeKey);
            nodePairs.put(masterKey, v);
            return 0;
        }
        // Here if there has been a previous edge between the 2, maybe just this one
        if (!lis.contains(edgeKey)) {
            lis.add(edgeKey);
            //System.out.println("adding edgekey in "+masterKey + " "+ edgeKey);
        }
        return lis.indexOf(edgeKey);
    }

    @Override
    public int getPreferredLineStyle(EdgeView ev) {
        return GraphConstants.STYLE_SPLINE;
    }

} // end class file vRouting.java
