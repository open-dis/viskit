package viskit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:44:31 AM
 */
public class LegosTree extends JTree
{
  private static DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");;
  private Color background = new Color(0xFB,0xFB,0xE5);
  public LegosTree()
  {
    super(root);

    buildSampleData(root);

    expandRow(0);
    setRootVisible(false);
    setShowsRootHandles(true);
    setVisibleRowCount(100);    // means always fill a normal size panel
    ((DefaultTreeCellRenderer)getCellRenderer()).setBackgroundNonSelectionColor(background);
  }

  private void buildSampleData(DefaultMutableTreeNode root)
  {
    DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[10];
    for(int i=0;i<nodes.length;i++) {
      nodes[i] = new DefaultMutableTreeNode("LegoName"+i+".class");
    }
    for(int i=0;i<nodes.length;i++) {
      if(nodes[i] != null) {
        switch(i) {
          case 3:
            nodes[3].setUserObject("directory blah");
            nodes[3].add(nodes[4]);
            nodes[4] = null;
            break;
          case 8:
            nodes[8].setUserObject("another blah");
            nodes[8].add(nodes[9]);
            nodes[9] = null;
          default:
            ;
        }
        root.add(nodes[i]);
      }
    }
  }
}
