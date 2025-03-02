
/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

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

package viskit.view;

import edu.nps.util.Log4jUtilities;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.*;

import viskit.ViskitGlobals;
import static viskit.ViskitGlobals.isFileReady;
import viskit.ViskitStatics;
import viskit.doe.FileHandler;

/**
 * A class to present an XML file in a JTree widget
 * @author Mike Bailey
 * @since 27 Aug 2004
 * @version $Id$
 */
public class XMLTreeComponent extends JTree 
{   
    static final Logger LOG = Log4jUtilities.getLogger(XMLTreeComponent.class);

    DefaultTreeModel mod;

    static XmlTreePanel getTreeInPanel(File xmlFile) throws Exception {
        return new XmlTreePanel(xmlFile);
    }

    public XMLTreeComponent(File xmlFile) throws Exception 
    {
        super();
        setFile(xmlFile);
    }
    XMLOutputter xmlOutputter;
    Document document = null;

    public final void setFile(File xmlFile) {
        Format format;
        try {
            document = FileHandler.unmarshallJdom(xmlFile);
            xmlOutputter = new XMLOutputter();
            format = Format.getPrettyFormat();
            xmlOutputter.setFormat(format);
        }
        catch (JDOMException | IOException e) {
            document = null;
            xmlOutputter = null;
            LOG.error("Error parsing or finding file {}", xmlFile.getAbsolutePath());
            return;
        }

        // throw existing away here?
        // this.removeAll();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        root.setUserObject(new nElement(0, document.getRootElement()));
        mod = new DefaultTreeModel(root);
        //addChildren(root);
        addRoot(root);
        setModel(mod);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        MyRenderer rendr = new MyRenderer();
        setCellRenderer(rendr);

        //   collapseRow(1);

        setToolTipText("XML Tree View");  // needs to be done first to enable tt below
        setRootVisible(true);
        setShowsRootHandles(true);
        setVisibleRowCount(100);    // means always fill a normal size panel
        revalidate();
        this.expandAll(this, true);
    }

    public String getXML() {
        if (xmlOutputter != null) {
            return xmlOutputter.outputString(document);
        }
        return "";
    }

    private void addAttributes(DefaultMutableTreeNode node) {
        Element elm = ((nElement) node.getUserObject()).elem;
        java.util.List lis = elm.getAttributes();
        if (lis.isEmpty()) {
            return;
        }
        for (Iterator itr = lis.iterator(); itr.hasNext();) {
            Attribute att = (Attribute) itr.next();
            String attrs = "<font color='black'>" + att.getName() + " =</font> " + att.getValue();
            node.add(new DefaultMutableTreeNode(attrs));
        }
    }

    private void addRoot(DefaultMutableTreeNode node) {
        addAttributes(node);         // root attributes
        addContent(node);
    }

    private void addContent(DefaultMutableTreeNode node) {
        Element elm = ((nElement) node.getUserObject()).elem;
        int level = ((nElement) node.getUserObject()).n;

        java.util.List lis = elm.getContent();
        if (lis.isEmpty()) {
            return;
        }
        for (Iterator itr = lis.iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (o instanceof Element) {
                DefaultMutableTreeNode dmt = new DefaultMutableTreeNode(new nElement(level + 1, (Element) o));
                node.add(dmt);
                addAttributes(dmt);
                addContent(dmt);
            } else if (o instanceof Text) {
                String s = ((Text) o).getTextTrim();
                if (s.length() > 0) {
                    DefaultMutableTreeNode dmt = new DefaultMutableTreeNode(s);
                    node.add(dmt);
                }
            } else {
                
                if (o != null) {
                    DefaultMutableTreeNode dmt = new DefaultMutableTreeNode(o.toString());
                    node.add(dmt);
                }
            }
        }

    }

    public void removeSelected() {
        TreePath[] selections;
        while ((selections = getSelectionPaths()) != null) {
            TreePath currentSelection = selections[0];
            if (currentSelection != null) {
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
                MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
                if (parent != null) {
                    mod.removeNodeFromParent(currentNode);
                }
            }
        }
    }

    // If expand is true, expands all nodes in the tree.
    // Otherwise, collapses all nodes in the tree.
    private void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();

        // Traverse tree from root
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }
    Color[] colors;

    class MyRenderer extends DefaultTreeCellRenderer {

        Icon[] icons = new XTreeIcon[8];
        float startR = 1.f;
        float startG = 51.f / 255.f;
        float startB = 51.f / 255.f;
        float endR = 51.f / 255.f;
        float endG = 1.f;
        float endB = 215.f / 255.f;

        MyRenderer() {
            colors = new Color[icons.length];
            float rDelta = (endR - startR) / (icons.length - 1);
            float gDelta = (endG - startG) / (icons.length - 1);
            float bDelta = (endB - startB) / (icons.length - 1);
            for (int i = 0; i < icons.length; i++) {
                colors[i] = new Color(startR + rDelta * i, startG + gDelta * i, startB + bDelta * i);
                icons[i] = new XTreeIcon(colors[i]);
            }
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object o = ((DefaultMutableTreeNode) value).getUserObject();

            if (o instanceof nElement) {
                int idx = ((nElement) o).n;
                Element el = ((nElement) o).elem;
                value = "<html>" + wrap(el.getName());
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                String tt = el.toString();
                if (tt.length() < 100) {
                    setToolTipText(tt);
                }
                if (idx == 0) {
                    setToolTipText(((XMLTreeComponent) tree).document.toString());
                }
                setIcon(icons[idx % icons.length]);
            } else {
                value = "<html><font color='maroon' style='bold'>" + wrap(value.toString());
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                setIcon(null);
                setToolTipText(value.toString());
            }
            return this;
        //return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
//    static int wrapSiz = 50;//100;
//    static String nl = System.getProperty("line.separator");
    static boolean isWindows = ViskitStatics.OPERATING_SYSTEM.toLowerCase().contains("windows");

    static private String wrap(String s) {
        if (isWindows) {
            return s;
        }     // can't get it to work
        StringBuilder sb = new StringBuilder();
        String[] sa = new String[]{"", ""};
        sa[1] = s;
        do {
            sa = _wrap(sa[1]);
            sb.append(sa[0]);
            sb.append("<br>");
        } while (sa[1].length() > 0);
        sb.setLength(sb.length() - 4);  //lose last <br>
        return sb.toString().trim();

    // return s;
    }

    static private String[] _wrap(String s) {
        String[] sa = {"", ""};
        if (s.length() < 100) {
            sa[0] = s;
        } else {
            int idx = s.lastIndexOf(' ', 100);
            if (idx != -1) {
                sa[0] = s.substring(0, idx);
                sa[1] = s.substring(idx + 1);
            }
        }
        return sa;
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> {
            
            JFrame f = new JFrame("XML Tree Widget Test");
            
            JFileChooser jfc = new JFileChooser();
            jfc.showOpenDialog(f);
            File fil = jfc.getSelectedFile();
            if (fil == null) {
                ViskitGlobals.instance().systemExit(0);
            }
            
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Container c = f.getContentPane();
            c.setLayout(new BorderLayout());
            
            //XTree xt = new XMLTreeComponent(fil);
            //c.add(new JScrollPane(xt), BorderLayout.CENTER);
            XmlTreePanel p = null;
            try {
                p = XMLTreeComponent.getTreeInPanel(fil);
            } 
            catch (Exception e) 
            {
                LOG.error("main(" + Arrays.toString(args) + ") exception: " + e.getMessage());
            }
            
            if (p != null)
                LOG.info(p.xmlTreeComponent.getXML());
            
            c.add(p, BorderLayout.CENTER);
            f.setSize(500, 400);
            f.setLocation(300, 300);
            f.setVisible(true);
            
            // xt.setFile(fil);
        });
    }

    class nElement {

        public int n;
        public Element elem;

        nElement(int n, Element e) {
            this.n = n;
            this.elem = e;
        }
    }
}

class XmlTreePanel extends JPanel
{
    static final Logger LOG = Log4jUtilities.getLogger(XmlTreePanel.class);
    
    public XMLTreeComponent xmlTreeComponent;
    public JTextArea sourceXmlTextArea;

    XmlTreePanel(File xmlFile) throws Exception 
    {

        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        if (!isFileReady(xmlFile))
            return;

        try {
            xmlTreeComponent = new XMLTreeComponent(xmlFile);
        } 
        catch (Exception e) {
                LOG.error("XmlTreePanel(" + xmlFile.getAbsolutePath()+ ") exception: " + e.getMessage());
            xmlTreeComponent = null;
            throw (e);
        }

        sourceXmlTextArea = new JTextArea("raw XML here");
        sourceXmlTextArea.setWrapStyleWord(true);
        sourceXmlTextArea.setEditable(false);
        sourceXmlTextArea.setLineWrap(true);
        sourceXmlTextArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        Font oldF = sourceXmlTextArea.getFont();
        sourceXmlTextArea.setFont(new Font("Monospaced", oldF.getStyle(), oldF.getSize()));
        sourceXmlTextArea.setText(getElementText((DefaultMutableTreeNode) xmlTreeComponent.mod.getRoot()));
        sourceXmlTextArea.setCaretPosition(0);

        JScrollPane xmlTreeScrollPane = new JScrollPane(xmlTreeComponent);
        JScrollPane xmlTextScrollPane = new JScrollPane(sourceXmlTextArea);
        xmlTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // because we wrap

        JSplitPane xmlViewSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, xmlTreeScrollPane, xmlTextScrollPane);
        xmlViewSplitPane.setOneTouchExpandable(false);
        xmlViewSplitPane.setResizeWeight(0.75);

        Dimension d1 = xmlTreeComponent.getPreferredSize();
        Dimension d2 = sourceXmlTextArea.getPreferredSize();
        xmlViewSplitPane.setPreferredSize(new Dimension(d1.width, d1.height + d2.height));
        add(xmlViewSplitPane);
        add(Box.createVerticalGlue());

        xmlTreeComponent.getSelectionModel().addTreeSelectionListener((TreeSelectionEvent e) -> {
            DefaultMutableTreeNode dmt = (DefaultMutableTreeNode) xmlTreeComponent.getLastSelectedPathComponent();
            if (dmt == null) {
                return;
            }
            sourceXmlTextArea.setText(getElementText(dmt));
            sourceXmlTextArea.revalidate();
            sourceXmlTextArea.setCaretPosition(0);
        });
    }

    final String getElementText(DefaultMutableTreeNode dmt) {
        Object o = dmt.getUserObject();
        if (o instanceof XMLTreeComponent.nElement) {
            Element elm = ((XMLTreeComponent.nElement) o).elem;
            return xmlTreeComponent.xmlOutputter.outputString(elm);
        } else {
            return "";
        }
    }
}

class XTreeIcon implements Icon {

    Color myColor;

    XTreeIcon(Color c) {
        super();

        myColor = c;
    }

    @Override
    public int getIconHeight() {
        return 12;
    }

    @Override
    public int getIconWidth() {
        return 12;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;
        Insets ins = new Insets(0, 0, 0, 0);
        if (c instanceof JComponent) {
            ins = ((Container) c).getInsets();
        }
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(ins.left, ins.top);

        g2d.setColor(myColor);
        g2d.fillOval(1, 1, 10, 10);
        g2d.setColor(Color.black);
        g2d.drawOval(1, 1, 10, 10);
    }
}