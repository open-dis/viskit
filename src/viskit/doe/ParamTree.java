/*
Copyright (c) 1995-2007 held by the author(s).  All rights reserved.

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

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 1:25:58 PM
 * @verion $Id:$
 */
package viskit.doe;

import org.jdom.Element;
import org.jdom.Attribute;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;
import java.util.Iterator;
import java.awt.*;

public class ParamTree extends JTree {

    public ParamTree(List list) {
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        setCellRenderer(new pRenderer());
        if (list != null) {
            setParams(list);
        }
    }

    public void setParams(List lis) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter tree");

        for (Iterator itr = lis.iterator(); itr.hasNext();) {
            Element elm = (Element) itr.next();
            // assert elm.getName().equalsIgnoreCase("SimEntity");
            if (!elm.getName().equalsIgnoreCase("SimEntity")) {
                System.err.println("ParamTree.setParams, unknown element type: " + elm.getName());
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(elm);
            root.add(node);
            addChildren(elm, node);
        }
        setModel(new DefaultTreeModel(root));
    }

    private void addChildren(Element elm, DefaultMutableTreeNode node) {
        List lis = elm.getChildren();
        for (Iterator itr = lis.iterator(); itr.hasNext();) {
            Element e = (Element) itr.next();
            DefaultMutableTreeNode n = new DefaultMutableTreeNode(e);
            String nm = e.getName();
            if (nm.equalsIgnoreCase("MultiParameter")) {
                addChildren(e, n); // recurse
            } // recurse
            else {
                // assert nm.equalsIgnoreCase("TerminalParameter");
                if (!nm.equalsIgnoreCase("TerminalParameter")) {
                    System.err.println("ParamTree.addChildren, unknown element type: " + nm);
                }

            }
            node.add(n);
        }
    }

    class pRenderer extends JPanel implements TreeCellRenderer {

        JCheckBox cb = new JCheckBox("Name: ");
        JTextField nmFld = new JTextField();
        JLabel tLab = new JLabel("  type: ");
        JLabel tyField = new JLabel();

        public pRenderer() {
            setOpaque(false);
            cb.setOpaque(false);
            nmFld.setBackground(Color.lightGray);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(cb);
            add(nmFld);
            add(tLab);
            add(tyField);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (viskit.Vstatics.debug) {
                System.out.println("gettcr selected=" + selected);
            }
            Object o = ((DefaultMutableTreeNode) value).getUserObject();
            if (o instanceof String) {
                return new JLabel((String) o);
            }
            pRenderer pr = new pRenderer();
            Element el = (Element) o;
            pr.cb.setEnabled(el.getName().equalsIgnoreCase("TerminalParameter"));
      Attribute at = el.getAttribute("nameRef");
      pr.nmFld.setText(at != null? at.getValue():"   ");
      at = el.getAttribute("type");
      pr.tyField.setText(at != null? at.getValue():"");
      pr.cb.setSelected(selected);
      //pr.invalidate();
      return pr;
    }
  }
}