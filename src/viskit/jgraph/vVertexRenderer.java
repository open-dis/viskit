/*
 * @(#)VertexRenderer.java	1.0 03-JUL-04
 * 
 * Copyright (c) 2001-2004 Gaudenz Alder
 *  
 */
package viskit.jgraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
//import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;

import org.jgraph.JGraph;
import org.jgraph.graph.*;

/*
 * This overridden JGraph class's main purpose is to render customized text in
 * each of the Cell instances that represent nodes
 *
 * <p>
 * OPNAV N81-NPS World-Class-Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * </p>
 * @author Mike Bailey
 * @since Feb 23, 2004
 * @since 3:40:51 PM
 */

/**
 * This renderer displays entries that implement the CellView interface and
 * supports the following attributes. If the cell view is not a leaf, this
 * object is only visible if it is selected.
 * <li>GraphConstants.BOUNDS GraphConstants.ICON GraphConstants.FONT
 * GraphConstants.OPAQUE GraphConstants.BORDER GraphConstants.BORDERCOLOR
 * GraphConstants.LINEWIDTH GraphConstants.FOREGROUND GraphConstants.BACKGROUND
 * GraphConstants.VERTICAL_ALIGNMENT GraphConstants.HORIZONTAL_ALIGNMENT
 * GraphConstants.VERTICAL_TEXT_POSITION GraphConstants.HORIZONTAL_TEXT_POSITION
 * </li>
 *
 * @version 1.0 1/1/02
 * @author Gaudenz Alder
 */
public class vVertexRenderer
        extends JComponent // JLabel jmb
        implements CellViewRenderer, Serializable {

    /** Cache the current graph for drawing. */
    transient protected JGraph graph;

    /**
     * Cache the current shape for drawing.
     */
    transient protected VertexView view;

    /**
     * Cached hasFocus and selected value.
     */
    transient protected boolean hasFocus, selected, preview, childrenSelected;

    /**
     * Cached default foreground and default background.
     */
    transient protected Color defaultForeground, defaultBackground,
            bordercolor;

    /**
     * Cached borderwidth.
     */
    transient protected int borderWidth;

    /**
     * Cached value of the double buffered state
     */
    transient protected boolean isDoubleBuffered = false;
    
    /**
     * Cached value of whether the label is to be displayed
     */
    transient protected boolean labelEnabled;
    
    /**
     * Caches values of the colors to be used for painting the cell. The values
     * for gridColor, highlightColor and lockedHandleColor are updated with the
     * respective values from JGraph in getRendererComponent each time a vertex
     * is rendered. To render the selection border, the highlightColor or the
     * lockedHandleColor are used depending on the focused state of the vertex.
     * The gridColor is used to draw the selection border if any child cells are
     * selected. To change these color values, please use the respective setters
     * in JGraph.
     */
    transient protected Color gradientColor = null, gridColor = Color.black,
            highlightColor = Color.black, lockedHandleColor = Color.black;
    
    private final float[] dash = {5f, 5f};
    private final Stroke mySelectionStroke =
            new BasicStroke(
            2, // change from default of 1
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f,
            dash,
            0.0f);

    /**
     * Constructs a renderer that may be used to render vertices.
     */
    public vVertexRenderer() {
        defaultForeground = UIManager.getColor("Tree.textForeground");
        defaultBackground = UIManager.getColor("Tree.textBackground");
    }

    /**
     * Configure and return the renderer component based on the passed in cell.
     * The value is typically set from messaging the graph with
     * <code>convertValueToString</code>. We recommend you check the value's
     * class and throw an illegal argument exception if it's not correct.
     *
     * @param graph the graph that that defines the rendering context.
     * @param view the cell view that should be rendered.
     * @param sel whether the object is selected.
     * @param focus whether the object has the focus.
     * @param preview whether we are drawing a preview.
     * @return the component used to render the value.
     */
    @Override
    public Component getRendererComponent(JGraph graph, CellView view,
            boolean sel, boolean focus, boolean preview) {
        this.graph = graph;       
        gridColor = graph.getGridColor();
        highlightColor = graph.getHighlightColor();
        lockedHandleColor = graph.getLockedHandleColor();
        isDoubleBuffered = graph.isDoubleBuffered();
        if (view instanceof VertexView) {
            this.view = (VertexView) view;
            setComponentOrientation(graph.getComponentOrientation());
            if (graph.getEditingCell() != view.getCell()) {
                Object label = graph.convertValueToString(view);
                if (label != null) {
//                    setText(label.toString());
                } else {
//                    setText(null);
                }
            } else {
//                setText(null);
            }
            this.hasFocus = focus;
            this.childrenSelected = graph.getSelectionModel()
                    .isChildrenSelected(view.getCell());
            this.selected = sel;
            this.preview = preview;
            if (this.view.isLeaf()
                    || GraphConstants.isGroupOpaque(view.getAllAttributes())) {
                installAttributes(view);
            } else {
                resetAttributes();
            }
            return this;
        }
        return null;
    }
    
    /**
     * Hook for subclassers that is invoked when the installAttributes is not
     * called to reset all attributes to the defaults. <br>
     * Subclassers must invoke the superclass implementation.
     * 
     */
    protected void resetAttributes() {
//        setText(null);
        setBorder(null);
        setOpaque(false);
//        setGradientColor(null);
//        setIcon(null);
    }

    /**
     * Install the attributes of specified cell in this renderer instance. This
     * means, retrieve every published key from the cells hashtable and set
     * global variables or superclass properties accordingly.
     *
     * @param view the cell view to retrieve the attribute values from.
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
        gradientColor = GraphConstants.getGradientColor(map);
//        setGradientColor(gradientColor);
        Color background = GraphConstants.getBackground(map);
        setBackground((background != null) ? background : defaultBackground);
        setFont(GraphConstants.getFont(map));
        labelEnabled = GraphConstants.isLabelEnabled(map);
    }

    /**
     * Paint the renderer. Overrides superclass paint to add specific painting.
     */
    @Override
    public void paint(Graphics g) {
        try {
//            if (gradientColor != null && !preview && isOpaque()) {
//                setOpaque(false);
//                Graphics2D g2d = (Graphics2D) g;
//                g2d.setPaint(new GradientPaint(0, 0, getBackground(),
//                        getWidth(), getHeight(), gradientColor, true));
//                g2d.fillRect(0, 0, getWidth(), getHeight());
//            }
            super.paint(g);   // jmb this will come down to paintComponent
            paintSelectionBorder(g);
        } catch (IllegalArgumentException e) {
            // JDK Bug: Zero length string passed to TextLayout constructor
        }
    }
    Color egColor = new Color(0xCE, 0xCE, 0xFF); // pale blue
    Color pclColor = new Color(0xFF, 0xC8, 0xC8); // pale pink
    Color enColor = new Color(255, 255, 204); // pale yellow
    Color circColor;
    Font myfont = new Font("Verdana", Font.PLAIN, 10);

    // jmb
    @Override
    protected void paintComponent(Graphics g) {
        Rectangle2D r = view.getBounds();
        Graphics2D g2 = (Graphics2D) g;
        
        if (view instanceof vAssyCircleView) // EGN
            circColor = egColor;
        if (view instanceof vAssyPropListView) // PCL
            circColor = pclColor;
        if (view instanceof vCircleView) // EN
            circColor = enColor;
        
        g2.setColor(circColor);
        int myoff = 2;
        
        if (view instanceof vAssyCircleView) // EGN
            g2.fillRoundRect(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff, 20, 20);
        if (view instanceof vAssyPropListView) // PCL
            g2.fillRect(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff);
        if (view instanceof vCircleView) // EN
            g2.fillOval(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff); // size of rect is 54,54
            
        g2.setColor(Color.darkGray);
        
        if (view instanceof vAssyCircleView) // EGN
            g2.drawRoundRect(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff, 20, 20);
        if (view instanceof vAssyPropListView) { // PCL
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, new float[]{2.0f, 2.0f}, 0.0f));
            g2.drawRect(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff);
        }
        if (view instanceof vCircleView) // EN
            g2.drawOval(myoff, myoff, r.getBounds().width - 2 * myoff, r.getBounds().height - 2 * myoff);

        // Draw the text in the circle
        g2.setFont(myfont);         // uses component's font if not specified
        DefaultGraphCell cell = (DefaultGraphCell) view.getCell();
        String nm = cell.getUserObject().toString();
        FontMetrics metrics = g2.getFontMetrics();
        nm = breakName(nm, 50, metrics);

        if (view instanceof vAssyCircleView) { // EGN
        
            // Show event node names w/ corresponding parameters if any
            String[] arr = nm.split("_");

            if (arr.length > 1)
                nm = arr[0] + "\n(" + arr[1] + ")";
        }

        String[] lns = nm.split("\n"); // handle multi-line titles

        int hgt = metrics.getHeight();  // height of a line of text
        int ytop = 54 / 2 - (hgt * (lns.length - 1) / 2) + hgt / 4;    // start y coord

        int xp, y;
        for (int i = 0; i < lns.length; i++) {
            xp = metrics.stringWidth(lns[i]); // length of string fragment
            y = ytop + (hgt * i);
            g2.drawString(lns[i], (54 - xp) / 2, y);
        }
    }

    private String breakName(String name, int maxW, FontMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        String[] n = name.split("\n");
        String[] nn;
        for (String n1 : n) {
            nn = splitIfNeeded(n1, maxW, metrics);
            for (String nn1 : nn) {
                sb.append(nn1);
                sb.append("\n");
            }
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

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
    }

    /**
     * Provided for subclassers to paint a selection border.
     * @param g
     */
    protected void paintSelectionBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
//        Stroke previousStroke = g2.getStroke();
//        g2.setStroke(GraphConstants.SELECTION_STROKE);  
        g2.setStroke(this.mySelectionStroke);
        if (childrenSelected || selected) {
            if (childrenSelected) {
                g.setColor(gridColor);
            } else if (hasFocus && selected) {
                g.setColor(lockedHandleColor);
            } else if (selected) {
                g.setColor(highlightColor);
            }
            Dimension d = getSize();
            g.drawRect(0, 0, d.width - 1, d.height - 1);
        }
//        g2.setStroke(previousStroke);
    }

    /**
     * Returns the intersection of the bounding rectangle and the straight line
     * between the source and the specified point p. The specified point is
     * expected not to intersect the bounds.
     * @param view
     * @param source
     * @param p
     * @return 
     */
    public Point2D getPerimeterPoint(VertexView view, Point2D source, Point2D p) {
        Rectangle2D bounds = view.getBounds();
        double x = bounds.getX();
        double y = bounds.getY();
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        double xCenter = x + width / 2;
        double yCenter = y + height / 2;
        double dx = p.getX() - xCenter; // Compute Angle
        double dy = p.getY() - yCenter;
        double alpha = Math.atan2(dy, dx);
        double xout, yout;
        double pi = Math.PI;
        double pi2 = Math.PI / 2.0;
        double beta = pi2 - alpha;
        double t = Math.atan2(height, width);
        if (alpha < -pi + t || alpha > pi - t) { // Left edge
            xout = x;
            yout = yCenter - width * Math.tan(alpha) / 2;
        } else if (alpha < -t) { // Top Edge
            yout = y;
            xout = xCenter - height * Math.tan(beta) / 2;
        } else if (alpha < t) { // Right Edge
            xout = x + width;
            yout = yCenter + width * Math.tan(alpha) / 2;
        } else { // Bottom Edge
            yout = y + height;
            xout = xCenter + height * Math.tan(beta) / 2;
        }
        return new Point2D.Double(xout, yout);
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void validate() {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void revalidate() {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void repaint(Rectangle r) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    protected void firePropertyChange(String propertyName, Object oldValue,
            Object newValue) {
        // Strings get interned...
        if ("text".equals(propertyName)) {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, byte oldValue,
            byte newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    @Override
    public void firePropertyChange(String propertyName, char oldValue,
            char newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, short oldValue,
            short newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, int oldValue,
            int newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, long oldValue,
            long newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, float oldValue,
            float newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, double oldValue,
            double newValue) {
    }

    /**
     * Overridden for performance reasons. See the <a
     * href="#override">Implementation Note </a> for more information.
     */
    @Override
    public void firePropertyChange(String propertyName, boolean oldValue,
            boolean newValue) {
    }
    
    /**
     * @return Returns the gradientColor.
     */
    public Color getGradientColor() {
        return gradientColor;
    }

    /**
     * @param gradientColor The gradientColor to set.
     */
    public void setGradientColor(Color gradientColor) {
        this.gradientColor = gradientColor;
    }
}
