package viskit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;
import java.awt.*;
import java.util.jar.JarFile;
import java.util.*;
import java.io.*;

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
  private static DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
  private Class targetClass;
  private Color background = new Color(0xFB,0xFB,0xE5);
  private Icon standardClosedIcon;
  private Icon myLeafIcon;
  DefaultTreeModel mod;

  public LegosTree()
  {
    super(root);
    mod = new DefaultTreeModel(root);
    addJarFile("src/lib/simkit.jar");

    setModel(mod);
    try {
      targetClass = Class.forName("simkit.BasicSimEntity");
    }
    catch (ClassNotFoundException e) {
      System.out.println("Error:  Need simkit classes in classpath");
      e.printStackTrace();
      return;
    }
    MyRenderer rendr = new MyRenderer();
    setCellRenderer(rendr);

    expandRow(1);
    this.setToolTipText("mama");  // needs to be done first to enable tt below
    setRootVisible(false);
    setShowsRootHandles(true);
    setVisibleRowCount(100);    // means always fill a normal size panel
    rendr.setBackgroundNonSelectionColor(background);
    myLeafIcon = new ImageIcon(ClassLoader.getSystemResource("viskit/images/assembly.png"));
    rendr.setLeafIcon(myLeafIcon);
    standardClosedIcon = rendr.getClosedIcon();
  }

  public void addContentRoot(File f)
  {
    if(f.getName().toLowerCase().endsWith(".jar"))
      addJarFile(f.getPath());
    addContentRoot(f,false);
  }

  public void addContentRoot(File f, boolean recurse)
  {
    DefaultMutableTreeNode myNode;
    if(f.isDirectory() && recurse == true) {
      // Am I here?  If so, grab my treenode
      // Else is my parent here?  If so, hook me as child
      // If not, put me in under the root
      myNode = (DefaultMutableTreeNode)directoryRoots.get(f.getPath());
      if(myNode == null) {
        myNode = (DefaultMutableTreeNode)directoryRoots.get(f.getParent());
        if(myNode != null) {
          DefaultMutableTreeNode parent = myNode;
          myNode = new DefaultMutableTreeNode(f.getPath());
          parent.add(myNode);
          directoryRoots.put(f.getPath(),myNode);
          int idx = parent.getIndex(myNode);
          mod.nodesWereInserted(parent,new int[]{idx});
        }
        else {
          myNode = new DefaultMutableTreeNode(f.getPath());
          root.add(myNode);
          directoryRoots.put(f.getPath(),myNode);
          int idx = root.getIndex(myNode);
          mod.nodesWereInserted(root,new int[]{idx});
        }
      }
      File[] fa = f.listFiles(new MyClassTypeFilter());
      for(int i=0;i<fa.length;i++)
        addContentRoot(fa[i],recurse);
    }

    // We're NOT a directory...
    else {
      Class c = _getClass(f);
      if(c != null) {
        myNode = new DefaultMutableTreeNode(c);
        DefaultMutableTreeNode par = (DefaultMutableTreeNode)directoryRoots.get(f.getParent());
        if(par != null) {
          par.add(myNode);
          int idx = par.getIndex(myNode);
          mod.nodesWereInserted(par,new int[]{idx});
        }
        else {
          root.add(myNode);
          int idx = root.getIndex(myNode);
          mod.nodesWereInserted(root,new int[]{idx});
        }
      }
    }

  }
  HashMap directoryRoots = new HashMap();
  DefaultMutableTreeNode rootNode;
  private Class _getClass(File f)
  {
    Class c = null;
    try {
      c = FindClassesForInterface.classFromFile(f,targetClass);
    }
    catch (Throwable e) {
      System.out.println(e);
      return null;
    }

    return c;
  }

  private void addJarFile(String jarFileName)
  {
    DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(jarFileName);
    mod.insertNodeInto(localRoot,root,0);
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(jarFileName);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    java.util.List list = FindClassesForInterface.findClasses(jarFile, simkit.BasicSimEntity.class);
    Vector v = new Vector();
    for(Iterator itr = list.iterator(); itr.hasNext();) {
      Class c = (Class)itr.next();
      v.add(c);
    }
    Class[] ca = new Class[v.size()];
    ca = (Class[])v.toArray(ca);
    Arrays.sort(ca,new MyClassSorter());
    int idx = 0;
    for(int i=0;i<ca.length;i++) {
      DefaultMutableTreeNode node = new DefaultMutableTreeNode(ca[i]);
      mod.insertNodeInto(node,localRoot,idx++);
    }
  }

  class MyClassSorter implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      String s = ((Class)o1).getName();
      s = s.substring(s.lastIndexOf('.')+1);
      String s2 = ((Class)o2).getName();
      s2 = s2.substring(s2.lastIndexOf('.')+1);
      return (s.compareTo(s2));
    }
  }

  class MyRenderer extends DefaultTreeCellRenderer
  {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
      Object uo = ((DefaultMutableTreeNode)value).getUserObject();
      if(uo instanceof Class) {
        Class c = (Class)((DefaultMutableTreeNode)value).getUserObject();
        String nm = c.getName();
        setText(nm.substring(nm.lastIndexOf('.')+1));
        setToolTipText(nm);
        setLeafIcon(myLeafIcon);
        // We changed the text, so the following is required
        setPreferredSize(getPreferredSize());
      }
      else {
        setToolTipText((String)uo);
        setLeafIcon(standardClosedIcon);
      }
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      return this;
    }
  }

  class MyClassTypeFilter implements java.io.FileFilter
  {
    public boolean accept(File f)
    {
      if(f.isFile() && !f.getName().endsWith(".class") )
        return false;
      return true;

    }
  }
}
