package viskit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
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
public class LegosTree extends JTree implements DragGestureListener, DragSourceListener
{
  private static DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
  private Class targetClass;
  private Color background = new Color(0xFB, 0xFB, 0xE5);
  private Icon standardClosedIcon;
  private ImageIcon myLeafIcon;
  private Image myLeafIconImage;
  DefaultTreeModel mod;
  private DragStartListener lis;
  public LegosTree(DragStartListener dslis)
  {
    super(root);
    lis = dslis;
    mod = new DefaultTreeModel(root);

    try {
      targetClass = Class.forName("simkit.BasicSimEntity");
    }
    catch (ClassNotFoundException e) {
      System.out.println("Error:  Need simkit classes in classpath");
      e.printStackTrace();
      return;
    }

    addJarFile("lib/simkit.jar");
    setModel(mod);
    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    expandAll(this, true);

    MyRenderer rendr = new MyRenderer();
    setCellRenderer(rendr);

 //   collapseRow(1);

    setToolTipText("mama");  // needs to be done first to enable tt below
    setRootVisible(false);
    setShowsRootHandles(true);
    setVisibleRowCount(100);    // means always fill a normal size panel
    rendr.setBackgroundNonSelectionColor(background);

    myLeafIcon = new ImageIcon(ClassLoader.getSystemResource("viskit/images/assembly.png"));
    myLeafIconImage = myLeafIcon.getImage();

    rendr.setLeafIcon(myLeafIcon);
    standardClosedIcon = rendr.getClosedIcon();
    DragSource dragSource = DragSource.getDefaultDragSource();

    dragSource.createDefaultDragGestureRecognizer(this, // component where drag originates
        DnDConstants.ACTION_COPY_OR_MOVE, // actions
        this); // drag gesture recognizer

  }

  public void removeSelected()
  {
    TreePath[] selections;
    while ((selections = getSelectionPaths()) != null) {
      TreePath currentSelection = selections[0];
      if (currentSelection != null) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)
            (currentSelection.getLastPathComponent());
        MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
        if (parent != null) {
          mod.removeNodeFromParent(currentNode);
        }
      }
    }
  }

  public void addContentRoot(File f)
  {
    if (f.getName().toLowerCase().endsWith(".jar"))
      addJarFile(f.getPath());
    addContentRoot(f, false);
  }

  public void addContentRoot(File f, boolean recurse)
  {
    Vector v = new Vector();
    directoryRoots = new HashMap();
    classNodeCount = 0;

    addContentRoot(f, recurse, v);

    if (classNodeCount != 0)
      return;

    // Here if we maybe added a bunch of directories, but twernt no leaves in our tree.
    int ret = JOptionPane.showConfirmDialog(LegosTree.this, "No classes of type " + targetClass.getName() + "\nfound " +
        "in " + f.getName() +
        ".\nInsert in list anyway?", "Not found",
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (ret == JOptionPane.YES_OPTION)
      return;

    for (Iterator iterator = v.iterator(); iterator.hasNext();) {
      DefaultMutableTreeNode n = (DefaultMutableTreeNode) iterator.next();
      mod.removeNodeFromParent(n);
    }

  }

  private void addContentRoot(File f, boolean recurse, Vector rootVector)
  {
    DefaultMutableTreeNode myNode;
    if (f.isDirectory()) {
      if (recurse == false) {
        myNode = new DefaultMutableTreeNode(f.getPath());
        root.add(myNode);
        rootVector.add(myNode); // for later pruning
        directoryRoots.put(f.getPath(), myNode);
        int idx = root.getIndex(myNode);
        mod.nodesWereInserted(root, new int[]{idx});

        File[] fa = f.listFiles(new MyClassTypeFilter(false));
        for (int i = 0; i < fa.length; i++)
          addContentRoot(fa[i], recurse, rootVector);
      }
      else { // recurse = true
        // Am I here?  If so, grab my treenode
        // Else is my parent here?  If so, hook me as child
        // If not, put me in under the root
        myNode = (DefaultMutableTreeNode) directoryRoots.get(f.getPath());
        if (myNode == null) {
          myNode = (DefaultMutableTreeNode) directoryRoots.get(f.getParent());
          if (myNode != null) {
            DefaultMutableTreeNode parent = myNode;
            myNode = new DefaultMutableTreeNode(f.getPath());
            parent.add(myNode);
            directoryRoots.put(f.getPath(), myNode);
            int idx = parent.getIndex(myNode);
            mod.nodesWereInserted(parent, new int[]{idx});
          }
          else {
            myNode = new DefaultMutableTreeNode(f.getPath());
            root.add(myNode);
            rootVector.add(myNode); // for later pruning
            directoryRoots.put(f.getPath(), myNode);
            int idx = root.getIndex(myNode);
            mod.nodesWereInserted(root, new int[]{idx});
          }
        }
        File[] fa = f.listFiles(new MyClassTypeFilter(true));
        for (int i = 0; i < fa.length; i++)
          addContentRoot(fa[i], recurse, rootVector);
      }   // recurse = true
    }     // is directory

    // We're NOT a directory...
    else {
      Class c = _getClass(f);
      if (c != null) {
        myNode = buildClassNode(c);
        DefaultMutableTreeNode par = (DefaultMutableTreeNode) directoryRoots.get(f.getParent());
        if (par != null) {
          par.add(myNode);
          int idx = par.getIndex(myNode);
          mod.nodesWereInserted(par, new int[]{idx});
        }
        else {
          root.add(myNode);
          int idx = root.getIndex(myNode);
          mod.nodesWereInserted(root, new int[]{idx});
        }
      }
    }
  }

  private int classNodeCount;

  private DefaultMutableTreeNode buildClassNode(Class c)
  {
    classNodeCount++;
    return new DefaultMutableTreeNode(c);
  }

  HashMap directoryRoots;
  DefaultMutableTreeNode rootNode;

  private Class _getClass(File f)
  {
    Class c = null;
    try {
      c = FindClassesForInterface.classFromFile(f, targetClass);
    }
    catch (Throwable e) {
      System.out.println(e);
      return null;
    }
    return c;
  }

  HashMap packagesHM = new HashMap();

  private void hookToParent(Class c, DefaultMutableTreeNode myroot)
  {
    String pkg = c.getPackage().getName();
    DefaultMutableTreeNode dmtn = getParent(pkg, myroot);
    dmtn.add(new DefaultMutableTreeNode(c));
  }

  DefaultMutableTreeNode getParent(String pkg, DefaultMutableTreeNode lroot)
  {
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) packagesHM.get(pkg);

    if (parent == null) {
      if (pkg.indexOf('.') == -1) {
        // we're as far up as we can be
        parent = new DefaultMutableTreeNode(pkg);
        mod.insertNodeInto(parent, lroot, 0);
      }
      else {
        // go further
        String ppkg = pkg.substring(0, pkg.lastIndexOf('.'));
        DefaultMutableTreeNode granddaddy = getParent(ppkg, lroot);
        parent = new DefaultMutableTreeNode(pkg.substring(pkg.lastIndexOf('.') + 1));
        mod.insertNodeInto(parent, granddaddy, 0);
      }
      packagesHM.put(pkg, parent);
    }

    return parent;
  }

  private void addJarFile(String jarFileName)
  {
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(jarFileName);
    }
    catch (IOException e) {
      JOptionPane.showMessageDialog(LegosTree.this, "Error reading " + jarFileName, "I/O Error", JOptionPane.ERROR_MESSAGE);
      return;

    }
    Class c = simkit.BasicSimEntity.class;

    java.util.List list = FindClassesForInterface.findClasses(jarFile, c);
    if (list == null || list.size() <= 0) {
      JOptionPane.showMessageDialog(LegosTree.this, "No classes of type " + c.getName() + " found\n" +
          "in " + jarFileName, "Not found", JOptionPane.WARNING_MESSAGE);
      return;
    }

    DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(jarFileName);
    mod.insertNodeInto(localRoot, root, 0);

    for (Iterator itr = list.iterator(); itr.hasNext();) {
      hookToParent((Class) itr.next(), localRoot);
    }
  }

  // If expand is true, expands all nodes in the tree.
  // Otherwise, collapses all nodes in the tree.
  private void expandAll(JTree tree, boolean expand)
  {
    TreeNode root = (TreeNode) tree.getModel().getRoot();

    // Traverse tree from root
    expandAll(tree, new TreePath(root), expand);
  }

  private void expandAll(JTree tree, TreePath parent, boolean expand)
  {
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
    }
    else {
      tree.collapsePath(parent);
    }
  }


  class MyClassSorter implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      String s = ((Class) o1).getName();
      s = s.substring(s.lastIndexOf('.') + 1);
      String s2 = ((Class) o2).getName();
      s2 = s2.substring(s2.lastIndexOf('.') + 1);
      return (s.compareTo(s2));
    }
  }

  class MyRenderer extends DefaultTreeCellRenderer
  {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
      Object uo = ((DefaultMutableTreeNode) value).getUserObject();

      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

      if (uo instanceof Class) {
        Class c = (Class) uo;
        String nm = c.getName();
        setToolTipText(nm);
        nm = nm.substring(nm.lastIndexOf('.') + 1);
        if(sel)
          nm = "<html><b>"+nm+"</b></html>";
        setText(nm);
        // We changed the text, so the following is required
        setPreferredSize(getPreferredSize());
      }
      else {
        setToolTipText((String) uo);
      }
      return this;
    }
  }

  class MyClassTypeFilter implements java.io.FileFilter
  {
    boolean dirsToo;

    MyClassTypeFilter(boolean inclDirs)
    {
      dirsToo = inclDirs;
    }

    public boolean accept(File f)
    {
      if (f.isDirectory() && dirsToo == false)
        return false;
      if (f.isFile() && !f.getName().endsWith(".class"))
        return false;
      return true;

    }
  }

  // Drag stuff
  public void dragGestureRecognized(DragGestureEvent e)
  {
    String s = getClassName();
    if(s != null) {
      StringSelection ss = new StringSelection(s);
      if(lis != null)
        lis.startingDrag(ss);
      e.startDrag(DragSource.DefaultCopyDrop,
          myLeafIconImage,new Point(-myLeafIcon.getIconWidth()/2,
              -myLeafIcon.getIconHeight()/2),ss, this);
    }
  }

  public void dragDropEnd(DragSourceDropEvent e){}
  public void dragEnter(DragSourceDragEvent e){}
  public void dragExit(DragSourceEvent e){}
  public void dragOver(DragSourceDragEvent e){}
  public void dropActionChanged(DragSourceDragEvent e){}

  public String getClassName()
  {
    TreePath path = getLeadSelectionPath();
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) path.getLastPathComponent();
    Object o = dmtn.getUserObject();
    if (o != null && o instanceof Class)
      return ((Class) o).getName();
    return
        null;
  }
}


