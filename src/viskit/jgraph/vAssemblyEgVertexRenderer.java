package viskit.jgraph;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
import viskit.model.AssemblyNode;

/**
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * User: mike
 * Date: Feb 23, 2004
 * Time: 3:40:51 PM
 */

/*
 * @(#)VertexRenderer.java	1.0 1/1/02
 *
 * Copyright (c) 2001-2004, Gaudenz Alder
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of JGraph nor the names of its contributors may be used
 *   to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
/**
 * This renderer displays entries that implement the CellView interface
 * and supports the following attributes. If the cell view is not a leaf,
 * this object is only visible if it is selected.
 * <pre>
 * GraphConstants.BOUNDS
 * GraphConstants.ICON
 * GraphConstants.FONT
 * GraphConstants.OPAQUE
 * GraphConstants.BORDER
 * GraphConstants.BORDERCOLOR
 * GraphConstants.LINEWIDTH
 * GraphConstants.FOREGROUND
 * GraphConstants.BACKGROUND
 * GraphConstants.VERTICAL_ALIGNMENT
 * GraphConstants.HORIZONTAL_ALIGNMENT
 * GraphConstants.VERTICAL_TEXT_POSITION
 * GraphConstants.HORIZONTAL_TEXT_POSITION
 * </pre>
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */
public class vAssemblyEgVertexRenderer
        extends JComponent // JLabel jmb
        implements CellViewRenderer, Serializable {

    /** Use this flag to control if groups should appear transparent. */
    protected boolean hideGroups = true;

    /** Cache the current graph for drawing. */
    transient protected JGraph graph;

    /** Cache the current shape for drawing. */
    transient protected VertexView view;

    /** Cached hasFocus and selected value. */
    transient protected boolean hasFocus,  selected,  preview,  opaque,  childrenSelected;

    /** Cached default foreground and default background. */
    transient protected Color defaultForeground,  defaultBackground,  bordercolor;

    /** Cached borderwidth. */
    transient protected int borderWidth;

    /** Cached value of the double buffered state */
    transient boolean isDoubleBuffered = false;

    /**
     * Constructs a renderer that may be used to render vertices.
     */
    public vAssemblyEgVertexRenderer() {
        defaultForeground = UIManager.getColor("Tree.textForeground");
        defaultBackground = UIManager.getColor("Tree.textBackground");
    }
    private float[] dash = {5f, 5f};
    private BasicStroke mySelectionStroke =
            new BasicStroke(
            2, // change from default of 1
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f,
            dash,
            0.0f);

    /**
     * Constructs a renderer that may be used to render vertices.
     * @param hideGroups
     */
    public vAssemblyEgVertexRenderer(boolean hideGroups) {
        this();
        this.hideGroups = hideGroups;
    }

    /**
     * Configure and return the renderer based on the passed in
     * components. The value is typically set from messaging the
     * graph with <code>convertValueToString</code>.
     * We recommend you check the value's class and throw an
     * illegal argument exception if it's not correct.
     *
     * @param   graph the graph that that defines the rendering context.
     * @param   view the object that should be rendered.
     * @param   sel whether the object is selected.
     * @param   focus whether the object has the focus.
     * @param   preview whether we are drawing a preview.
     * @return	the component used to render the value.
     */
    @Override
    public Component getRendererComponent(
            JGraph graph,
            CellView view,
            boolean sel,
            boolean focus,
            boolean preview) {
        this.graph = graph;
        isDoubleBuffered = graph.isDoubleBuffered();
        if (view instanceof VertexView) {
            this.view = (VertexView) view;
            setComponentOrientation(graph.getComponentOrientation());

            this.graph = graph;
            this.hasFocus = focus;
            this.childrenSelected =
                    graph.getSelectionModel().isChildrenSelected(view.getCell());
            this.selected = sel;
            this.preview = preview;
            if (this.view.isLeaf() || !hideGroups) {
                installAttributes(view);
            } else {
                // jmb setText(null);
                setBorder(null);
                setOpaque(false);
            // jmb setIcon(null);
            }
            return this;
        }
        return null;
    }

    /**
     * Install the attributes of specified cell in this
     * renderer instance. This means, retrieve every published
     * key from the cells hashtable and set global variables
     * or superclass properties accordingly.
     *
     * @param view cell to retrieve the attribute values from.
     */
    protected void installAttributes(CellView view) {
        Map map = view.getAllAttributes();
        // jmb	setIcon(GraphConstants.getIcon(map));
        setOpaque(GraphConstants.isOpaque(map));
        setBorder(GraphConstants.getBorder(map));
        // jmb	setVerticalAlignment(GraphConstants.getVerticalAlignment(map));
        // jmb	setHorizontalAlignment(GraphConstants.getHorizontalAlignment(map));
        // jmb	setVerticalTextPosition(GraphConstants.getVerticalTextPosition(map));
        // jmb	setHorizontalTextPosition(GraphConstants.getHorizontalTextPosition(map));
        bordercolor = GraphConstants.getBorderColor(map);
        borderWidth = Math.max(1, Math.round(GraphConstants.getLineWidth(map)));
        if (getBorder() == null && bordercolor != null) {
            setBorder(BorderFactory.createLineBorder(bordercolor, borderWidth));
        }
        Color foreground = GraphConstants.getForeground(map);
        setForeground((foreground != null) ? foreground : defaultForeground);
        Color background = GraphConstants.getBackground(map);
        setBackground((background != null) ? background : defaultBackground);
        setFont(GraphConstants.getFont(map));
    }

    /**
     * Paint the renderer. Overrides superclass paint
     * to add specific painting.
     */
    @Override
    public void paint(Graphics g) {
        try {
            //if (preview && !isDoubleBuffered)
            //	setOpaque(false);
            super.paint(g);   // jmb this will come down to paintComponent
            paintSelectionBorder(g);
        } catch (IllegalArgumentException e) {
        // JDK Bug: Zero length string passed to TextLayout constructor
        }
    }
    Color circColor = new Color(0xCE, 0xCE, 0xFF); // pale blue 255,255,204); // pale yellow
    Font myfont = new Font("Verdana", Font.PLAIN, 10);

    // jmb
    @Override
    protected void paintComponent(Graphics g) {
        Rectangle2D r = view.getBounds();
        Graphics2D g2 = (Graphics2D) g;
        // jmb test  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(circColor);
        int myoff = 2;
        g2.fillRoundRect(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff, 20, 20);
        //g2.fillOval(myoff,myoff,r.width-2*myoff,r.height-2*myoff); // size of rect is 54,54
        g2.setColor(Color.darkGray);
        //g2.drawOval(myoff,myoff,r.width-2*myoff,r.height-2*myoff);
        g2.drawRoundRect(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff, 20, 20);
        // Draw the text in the circle
        g2.setFont(myfont);         // uses component's font if not specified
        DefaultGraphCell cell = (DefaultGraphCell) view.getCell();

        // Use the getName method instead of toString
        String nm = ((AssemblyNode) cell.getUserObject()).getName();
        FontMetrics metrics = g2.getFontMetrics();
        nm = breakName(nm, 50, metrics);
        String[] lns = nm.split("\n");       // handle multi-line titles

        int hgt = metrics.getHeight();  // height of a line of text
        int ytop = 54 / 2 - (hgt * (lns.length - 1) / 2) + hgt / 4;    // start y coord

        for (int i = 0; i < lns.length; i++) {
            int xp = metrics.stringWidth(lns[i]); // length of string fragment
            int y = ytop + (hgt * i);
            g2.drawString(lns[i], (54 - xp) / 2, y);
        }
    }

    private String breakName(String name, int maxW, FontMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        String[] n = name.split("\n");
        for (String n1 : n) {
            String[] nn = splitIfNeeded(n1, maxW, metrics);
            for (String nn1 : nn) {
                sb.append(nn1);
                sb.append("\n");
            }
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /*
    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                JLabel j = new JLabel("yup");
                JFrame jf = new JFrame();
                jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jf.getContentPane().add(j);
                jf.pack();
                jf.setVisible(true);
                Image ii = j.createImage(500, 500); //"viskit/images/eventNode.png");
                Graphics gr = ii.getGraphics();
                FontMetrics fm = gr.getFontMetrics();
                new vAssemblyEgVertexRenderer().splitIfNeeded("MyNameIsMikeBaileyAndILiveInCalifornia", 50, fm);
                new vAssemblyEgVertexRenderer().splitIfNeeded("MYNAMEISMIKEBAILEYANDILIVEINCALIFORNIA", 50, fm);
                new vAssemblyEgVertexRenderer().splitIfNeeded("MyNameIsMikeBaileyandiliveincalifornia", 50, fm);
                new vAssemblyEgVertexRenderer().splitIfNeeded("mynameismikebaileyandiliveincalifornia", 50, fm);
            }
        });
    }
     */

    private String[] splitIfNeeded(String s, int maxW, FontMetrics metrics) {
        String[] nuts = new String[2];
        nuts[1] = s;
        Vector<String> v = new Vector<>();
        do {
            nuts = splitOnce(nuts[1], maxW, metrics);
            v.add(nuts[0]);
        } while (nuts[1] != null);
        String[] ra = new String[v.size()];
        ra = v.toArray(ra);
        return ra;
    }

    private String[] splitOnce(String s, int maxW, FontMetrics metrics) {
        String[] ra = new String[2];
        ra[0] = s;

        int w = metrics.stringWidth(s);
        if (w < maxW) {
            return ra;
        }

        String ws = s;
        int fw;
        int i;
        for (i = s.length() - 1; i > 0; i--) {
            ws = s.substring(0, i);
            fw = metrics.stringWidth(ws);
            if (fw <= maxW) {
                break;
            }
        }
        if (i <= 0) {
            return ra;
        }    // couldn't get small enough...?

        // ws is now a small piece of string less than our max

        int j;
        for (j = ws.length() - 1; j > 0; j--) {

            if (Character.isUpperCase(s.charAt(j + 1))) {
                break;
            }
        }
        if (j <= 0) {
            return ra;
        } // couldn't find a break

        ra[0] = ws.substring(0, j + 1);
        ra[1] = ws.substring(j + 1) + s.substring(i);
        return ra;
    }

    @Override
    protected void paintBorder(Graphics g) {
    // jmb lose the rectangle super.paintBorder(g);

    // To put a red border around "incompletely-specified" nodes, establish
    // some connection to the user object, and do a conditional here
    /*
    if(userobject shows incomplete) {
    Graphics2D g2 = (Graphics2D)g;
    Rectangle r = view.getBounds();
    g2.setColor(Color.red);
    g2.drawRoundRect(2,2,r.width-4,r.height-4,20,20);
    }
    else
    ;  // Do nothing
     */
    }

    /**
     * Provided for subclass users to paint a selection border.
     * @param g
     */
    protected void paintSelectionBorder(Graphics g) {
        //((Graphics2D) g).setStroke(GraphConstants.SELECTION_STROKE);
        ((Graphics2D) g).setStroke(this.mySelectionStroke);
        if (childrenSelected) {
            g.setColor(graph.getGridColor());
        } else if (hasFocus && selected) {
            g.setColor(graph.getLockedHandleColor());
        } else if (selected) {
            g.setColor(graph.getHighlightColor());
        }
        if (childrenSelected || selected) {
            Dimension d = getSize();
            g.drawRect(0, 0, d.width - 1, d.height - 1);
        }
    }

    /**
     * TODO: Not currently used.
     * Returns the intersection of the bounding rectangle and the
     * straight line between the source and the specified point p.
     * The specified point is expected not to intersect the bounds.
     *
     * @param view
     * @param source
     * @param p
     * @return
     */
    public Point2D getPerimeterPoint(VertexView view, Point source, Point p) {
        Rectangle2D bounds = view.getBounds();
        double x = bounds.getX();
        double y = bounds.getY();
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        double xCenter = bounds.getCenterX();
        double yCenter = bounds.getCenterY();
        double dx = p.x - xCenter; // Compute Angle
        double dy = p.y - yCenter;
        double alpha = Math.atan2(dy, dx);
        double xout, yout;
        double pi = Math.PI;
        double pi2 = Math.PI / 2.0;
        double beta = pi2 - alpha;
        double t = Math.atan2(height, width);
        if (alpha < -pi + t || alpha > pi - t) { // Left edge
            xout = x;
            yout = yCenter - (int) (width * Math.tan(alpha) / 2);
        } else if (alpha < -t) { // Top Edge
            yout = y;
            xout = xCenter - (int) (height * Math.tan(beta) / 2);
        } else if (alpha < t) { // Right Edge
            xout = x + width;
            yout = yCenter + (int) (width * Math.tan(alpha) / 2);
        } else { // Bottom Edge
            yout = y + height;
            xout = xCenter + (int) (height * Math.tan(beta) / 2);
        }
        return new Point2D.Double(xout, yout);
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void validate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void revalidate() {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void repaint(Rectangle r) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    protected void firePropertyChange(
            String propertyName,
            Object oldValue,
            Object newValue) {
        // Strings get interned...
        if (propertyName.equals("text")) {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            byte oldValue,
            byte newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            char oldValue,
            char newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            short oldValue,
            short newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            int oldValue,
            int newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            long oldValue,
            long newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            float oldValue,
            float newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            double oldValue,
            double newValue) {
    }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    @Override
    public void firePropertyChange(
            String propertyName,
            boolean oldValue,
            boolean newValue) {
    }

    /**
     * Returns the hideGroups.
     * @return boolean
     */
    public boolean isHideGroups() {
        return hideGroups;
    }

    /**
     * Sets the hideGroups.
     * @param hideGroups The hideGroups to set
     */
    public void setHideGroups(boolean hideGroups) {
        this.hideGroups = hideGroups;
    }
}
