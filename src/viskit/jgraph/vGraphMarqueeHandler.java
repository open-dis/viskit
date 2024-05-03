/*
 Copyright (c) 1995-2015 held by the author(s).  All rights reserved.

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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import org.jgraph.JGraph;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.Port;
import org.jgraph.graph.PortView;

/**
 * Custom MarqueeHandler that Connects Vertices
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.jgraph.vGraphMarqueeHandler">Terry Norbraten, NPS MOVES</a>
 */
public class vGraphMarqueeHandler extends BasicMarqueeHandler {

    /** Holds the Start and the Current Point */
    protected Point2D start, current;

    /** Holds the First and the Current Port */
    protected PortView port, firstPort;

    /**
     * Component that is used for highlighting cells if the graph does not allow
     * XOR painting.
     */
    protected JComponent highlight;

    private JGraph graph;

    /** Default constructor to provide custom vertex connections
     * 
     * @param graph JGraph component (our canvas)
     */
    public vGraphMarqueeHandler(JGraph graph) {

        this.graph = graph;

        // Configures the panel for highlighting ports
        highlight = createHighlight();
    }

    /**
     * Creates the component that is used for highlighting cells if the graph
     * does not allow XOR painting.
     *
     * @return a component that is used for highlighting cells
     */
    private JComponent createHighlight() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        panel.setVisible(false);
        panel.setOpaque(false);

        return panel;
    }

    // Override to Gain Control (for PopupMenu and ConnectMode)
    @Override
    public boolean isForceMarqueeEvent(MouseEvent e) {
        
        // Not working for macOS
        if (e.isShiftDown()) {
            return false;
        }
        
        // If Right Mouse Button we want to Display the PopupMenu
        if (SwingUtilities.isRightMouseButton(e)) // Return Immediately
        {
            return true;
        }
        
        // Find and Remember Port
        port = getSourcePortAt(e.getPoint());
        // If Port Found and in ConnectMode (=Ports Visible)
        if (port != null && graph.isPortsVisible() /*&& e.getClickCount() != 2*/) //jmb  added to edit when in edge mode
        {
            return true;
        }
        
        // Else Call Superclass
        return super.isForceMarqueeEvent(e);
    }

    // Display PopupMenu or Remember Start Location and First Port
    @Override
    public void mousePressed(MouseEvent e) {
        if (port != null && graph.isPortsVisible()) {
            // Remember Start Location
            start = graph.toScreen(port.getLocation());
            // Remember First Port
            firstPort = port;
        } else // Call Superclass
        {
            super.mousePressed(e);
        }
    }

    // Find Port under Mouse and Repaint Connector
    @Override
    public void mouseDragged(MouseEvent e) {
        // If remembered Start Point is Valid
        if (start != null) {
            // Fetch Graphics from Graph
            Graphics g = graph.getGraphics();
            // Reset Remembered Port
            PortView newPort = getTargetPortAt(e.getPoint());
            // Do not flicker (repaint only on real changes)
            if (newPort == null || newPort != port) {
                // Xor-Paint the old Connector (Hide old Connector)
                paintConnector(Color.black, graph.getBackground(), g);
                // If Port was found then Point to Port Location
                port = newPort;
                if (port != null) {
                    current = graph.toScreen(port.getLocation());
                } // Else If no Port was found then Point to Mouse Location
                else {
                    current = graph.snap(e.getPoint());
                }
                // Xor-Paint the new Connector
                paintConnector(graph.getBackground(), Color.black, g);
            }
        }
        // Call Superclass
        super.mouseDragged(e);
    }

    public PortView getSourcePortAt(Point2D point) {
        // Disable jumping
        graph.setJumpToDefaultPort(false);
        PortView result;
        try {
            // Find a Port View in Model Coordinates and Remember
            result = graph.getPortViewAt(point.getX(), point.getY());
        } finally {
            graph.setJumpToDefaultPort(true);
        }
        return result;
    }

    // Find a Cell at point and Return its first Port as a PortView
    protected PortView getTargetPortAt(Point2D point) {
        // Find a Port View in Model Coordinates and Remember
        return graph.getPortViewAt(point.getX(), point.getY());
    }

    // Connect the First Port and the Current Port in the Graph or Repaint
    @Override
    public void mouseReleased(MouseEvent e) {
        highlight(graph, null);

        // If Valid Event, Current and First Port
        if (e != null && port != null && firstPort != null
                && firstPort != port) {
            // Then Establish Connection
            if (graph instanceof vGraphComponent) {
                ((vGraphComponent)graph).connect((Port) firstPort.getCell(), (Port) port.getCell());
            } else {
                ((vGraphAssemblyComponent)graph).connect((Port) firstPort.getCell(), (Port) port.getCell());
            }
            e.consume();
            
        // Else Repaint the Graph
        } else {
            GraphModel mod = graph.getModel();
            if (mod instanceof vGraphModel) {
                ((vGraphModel)mod).reDrawNodes();
            } else if (mod instanceof vGraphAssemblyModel) {
                ((vGraphAssemblyModel)mod).reDrawNodes();
            }
        }
        
        // Reset Global Vars
        firstPort = port = null;
        start = current = null;
        
        // Pass off to Superclass
        super.mouseReleased(e);
    }

    // Show Special Cursor if Over Port
    @Override
    public void mouseMoved(MouseEvent e) {
        // Check Mode and Find Port
        if (e != null && getSourcePortAt(e.getPoint()) != null
                && graph.isPortsVisible()) {
            // Set Cusor on Graph (Automatically Reset)
            graph.setCursor(new Cursor(Cursor.HAND_CURSOR));
		// Consume Event
            // Note: This is to signal the BasicGraphUI's
            // MouseHandle to stop further event processing.
            e.consume();
        } else // Pass off to Superclass
        {
            super.mouseMoved(e); // does nothing in super
        }
    }

    // Use Xor-Mode on Graphics to Paint Connector
    protected void paintConnector(Color fg, Color bg, Graphics g) {
        if (graph.isXorEnabled()) {
            // Set Foreground
            g.setColor(fg);
            // Set Xor-Mode Color
            g.setXORMode(bg);
            // Highlight the Current Port
            paintPort(graph.getGraphics());

            drawConnectorLine(g);
        } else {
            Rectangle dirty = new Rectangle((int) start.getX(), (int) start.getY(), 1, 1);

            if (current != null) {
                dirty.add(current);
            }

            dirty.grow(1, 1);

            graph.repaint(dirty);
            highlight(graph, port);
        }
    }

    // Overrides parent method to paint connector if
    // XOR painting is disabled in the graph
    @Override
    public void paint(JGraph graph, Graphics g) {
        super.paint(graph, g);

        if (!graph.isXorEnabled()) {
            g.setColor(Color.black);
            drawConnectorLine(g);
        }
    }

    protected void drawConnectorLine(Graphics g) {
        if (firstPort != null && start != null && current != null) {
            // Then Draw A Line From Start to Current Point
            g.drawLine((int) start.getX(), (int) start.getY(),
                    (int) current.getX(), (int) current.getY());
        }
    }

    // Use the Preview Flag to Draw a Highlighted Port
    protected void paintPort(Graphics g) {
        // If Current Port is Valid
        if (port != null) {
            // If Not Floating Port...
            boolean o = (GraphConstants.getOffset(port.getAllAttributes()) != null);
            // ...Then use Parent's Bounds
            Rectangle2D r = (o) ? port.getBounds() : port.getParentView().getBounds();
            // Scale from Model to Screen
            r = graph.toScreen((Rectangle2D) r.clone());
            // Add Space For the Highlight Border
            r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
                    .getHeight() + 6);
            // Paint Port in Preview (=Highlight) Mode
            graph.getUI().paintCell(g, port, r, true);
        }
    }

    /**
     * Highlights the given cell view or removes the highlight if no cell view
     * is specified.
     *
     * @param graph the JGraph panel
     * @param cellView the view to highlight
     */
    protected void highlight(JGraph graph, CellView cellView) {
        if (cellView != null) {
            highlight.setBounds(getHighlightBounds(graph, cellView));

            if (highlight.getParent() == null) {
                graph.add(highlight);
                highlight.setVisible(true);
            }
        } else {
            if (highlight.getParent() != null) {
                highlight.setVisible(false);
                highlight.getParent().remove(highlight);
            }
        }
    }

    /**
     * Returns the bounds to be used to highlight the given cell view.
     *
     * @param graph the JGraph panel
     * @param cellView the view to highlight
     * @return the bounds of the view to highlight
     */
    protected Rectangle getHighlightBounds(JGraph graph, CellView cellView) {
        boolean offset = (GraphConstants.getOffset(cellView.getAllAttributes()) != null);
        Rectangle2D r = (offset) ? cellView.getBounds() : cellView
                .getParentView().getBounds();
        r = graph.toScreen((Rectangle2D) r.clone());
        int s = 3;

        return new Rectangle((int) (r.getX() - s), (int) (r.getY() - s),
                (int) (r.getWidth() + 2 * s), (int) (r.getHeight() + 2 * s));
    }

} // end class file vGraphMarqueeHandler.java
