package viskit;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.jar.JarFile;

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
  private DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
  private Class targetClass;
  private String targetClassName;
  private Color background = new Color(0xFB, 0xFB, 0xE5);
  //private Icon standardClosedIcon;
  private ImageIcon myLeafIcon;
  private Icon standardNonLeafIcon;
  private Image myLeafIconImage;
  DefaultTreeModel mod;
  private DragStartListener lis;
  private AssemblyController controller;
  private Vector recurseNogoList;
  private String genericTableToolTip = "Drag onto canvas";

  public LegosTree(String className, String iconPath, DragStartListener dslis, AssemblyController controller, String tooltip)
  {
    this(className, new ImageIcon(ClassLoader.getSystemResource(iconPath)), dslis, controller, tooltip);
  }

  public LegosTree(String className, ImageIcon icon, DragStartListener dslis, AssemblyController controller, String tooltip)
  {
    super();
    lis = dslis;
    targetClassName = className;
    this.controller = controller;
    genericTableToolTip = tooltip;

    mod = new DefaultTreeModel(root);

    try {
      targetClass = Class.forName(targetClassName,false,this.getClass().getClassLoader()); //"simkit.BasicSimEntity");
    }
    catch (ClassNotFoundException e) {
      System.out.println("Error:  Need simkit classes in classpath");
      e.printStackTrace();
      return;
    }

    addJarFile("simkit.SimEntity", "lib/simkit.jar");
    addJarFile("diskit.DISEntity", "lib/ext/diskit.jar");
    
    setModel(mod);
    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    //todo test  this.expandRow(1);
    // expandAll(this, true);

    MyRenderer rendr = new MyRenderer();
    setCellRenderer(rendr);

    //   collapseRow(1);

    setToolTipText("mama");  // needs to be done first to enable tt below
    setRootVisible(false);
    setShowsRootHandles(true);
    setVisibleRowCount(100);    // means always fill a normal size panel
    rendr.setBackgroundNonSelectionColor(background);

    myLeafIcon = icon;
    myLeafIconImage = myLeafIcon.getImage();
    standardNonLeafIcon = rendr.getOpenIcon();

    rendr.setLeafIcon(myLeafIcon);
    //standardClosedIcon = rendr.getClosedIcon();
    DragSource dragSource = DragSource.getDefaultDragSource();

    dragSource.createDefaultDragGestureRecognizer(this, // component where drag originates
        DnDConstants.ACTION_COPY_OR_MOVE, // actions
        this); // drag gesture recognizer

  }

  /**
   * Override to provide a global tooltip for entire table..not just for nodes
   * @param event
   * @return
   */
  public String getToolTipText(MouseEvent event)
  {
    String s = super.getToolTipText(event);
    return s == null ? genericTableToolTip : s;
  }

  public Class getTargetClass()
  {
    return targetClass;
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


  // todo if no valid leaves are found, remove the directory...at it is, it is represented in
  // the tree with the node icon.

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

    if(recurse == true)
      recurseNogoList = new Vector();
    else
      recurseNogoList = null;

    addContentRoot(f, recurse, v);

    if(recurseNogoList != null && recurseNogoList.size()>0) {
      JOptionPane.showMessageDialog(this,recurseNogoList.toArray(new String[0]),"Classes or files not added:",JOptionPane.INFORMATION_MESSAGE);
    }
    if (classNodeCount != 0)
      return;

    // Here if we maybe added a bunch of directories, but twernt no leaves in our tree.
/*
    int ret = JOptionPane.showConfirmDialog(LegosTree.this, "No classes of type " + targetClass.getName() + "\nfound " +
        "in " + f.getName() +
        ", or duplicate class type encountered. \nInsert in list anyway?", "Error",
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (ret == JOptionPane.YES_OPTION)
      return;

    for (Iterator iterator = v.iterator(); iterator.hasNext();) {
      DefaultMutableTreeNode n = (DefaultMutableTreeNode) iterator.next();
      mod.removeNodeFromParent(n);
    }
*/
    JOptionPane.showMessageDialog(LegosTree.this,"No classes of type " + targetClass.getName() +
                                                 " found in "+f.getName() + ",\n" +
                        "or only duplicate class type(s) encountered.","Error",JOptionPane.WARNING_MESSAGE);
  }

  // The two above are the public ones.
  private void addContentRoot(File f, boolean recurse, Vector rootVector)
  {
    DefaultMutableTreeNode myNode = null;
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
/*
      if (f.getName().toLowerCase().endsWith(".xml")) {
        myNode = handleEGxml(f);
        if(myNode == null)
          return;
      }
      else {
        Class c = _getClass(f);
        if (c == null)
          return;
        myNode = buildClassNode(c);
      }

*/

      FileBasedAssyNode fban = null;
      try {
        fban = FileBasedClassManager.inst().loadFile(f);
        // Check here for duplicates of the classes which have been loaded on the classpath (simkit.jar);
        // No dups accepted
        try {
          Class.forName(fban.loadedClass);
          return;  // don't proceed
        }
        catch (ClassNotFoundException e) {}
      }
      catch (Throwable throwable) {
        System.err.println("Couldn't handle "+f.getName()+". "+throwable.getMessage());
        if(recurseNogoList != null)
          recurseNogoList.add(f.getName());
        return;
      }
      myNode = new DefaultMutableTreeNode(fban);

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
      classNodeCount++;
    }  // directory
  }

  /**
   * Try to build a tree node from the XML file

   */
/*
  private DefaultMutableTreeNode handleEGxml(File fxml)
  {
    File fc = AssemblyController.createTemporaryEventGraphClass(fxml);

    Class fclass = null;
    try {
      fclass = FindClassesForInterface.classFromFile(fc);
    }
    catch (Throwable throwable) {
      throwable.printStackTrace();
    }
    if(!FindClassesForInterface.matchClass(fclass,targetClass))
      return null;

    FileBasedAssyNode xn = new FileBasedAssyNode(fc,fclass.getName(),fxml);
    classNodeCount++;

    return new DefaultMutableTreeNode(xn);
  }
*/

  private int classNodeCount;

/*
  private DefaultMutableTreeNode buildClassNode(Class c)
  {
    classNodeCount++;
    return new DefaultMutableTreeNode(c);
  }
*/
  HashMap directoryRoots;
  DefaultMutableTreeNode rootNode;
/*
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
*/

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

  private void addJarFile(String jarFilePath)
  {
    JarFile jf = null;
    try {
      jf = new JarFile(jarFilePath);
    }
    catch (IOException e) {
      JOptionPane.showMessageDialog(LegosTree.this, "Error reading " + jarFilePath, "I/O Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    jarFileCommon(jf);
  }

  /**
   *
   * @param classInJarFile
   * @param jarFileName
   */
  private void addJarFile(String classInJarFile, String jarFileName)
  {
    JarFile jarFile = null;
    try {
      // This way doesn't need jar file dir on classpath
      Class c = Class.forName(classInJarFile,false,this.getClass().getClassLoader());

      String clsName = "/" + c.getName().replace('.', '/') + ".class";
      URL classU = c.getResource(clsName);
      String jp = classU.getPath();
      int bang = jp.indexOf('!');
      if (bang >= 0)
        jp = jp.substring(0, bang);
      URI jui = new URI(jp.toString());
      jarFile = new JarFile(jui.getPath());

      // This way needs jar file directory on classpath
      //URL jurl = ClassLoader.getSystemResource(jarFileName);
      //URI juri = new URI(jurl.toString());
      //jarFile = new JarFile(juri.getPath()); //jarFileName);
    }
    catch (Exception e) {
      JOptionPane.showMessageDialog(LegosTree.this, "Error reading " + jarFileName, "I/O Error", JOptionPane.ERROR_MESSAGE);
      return;

    }
    jarFileCommon(jarFile);
  }

  private void jarFileCommon(JarFile jarFile)
  {
    java.util.List list = FindClassesForInterface.findClasses(jarFile, targetClass);
    if (list == null || list.size() <= 0) {
      JOptionPane.showMessageDialog(LegosTree.this, "No classes of type " + targetClassName + " found\n" +
          "in " + jarFile.getName(), "Not found", JOptionPane.WARNING_MESSAGE);
      return;
    }

    DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(jarFile.getName());
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
      setLeafIcon(LegosTree.this.myLeafIcon); // default

      if (uo instanceof Class) {
        Class c = (Class) uo;
        String nm = c.getName();

        setToolTipText(nm);
        nm = nm.substring(nm.lastIndexOf('.') + 1);
//      if(sel)
//        nm = "<html><b>"+nm+"</b></html>";   // sizes inst screwed up
        value = nm;
      }
      else if (uo instanceof FileBasedAssyNode) {
        FileBasedAssyNode xn = (FileBasedAssyNode)uo;
        String nm = xn.loadedClass;
        nm = nm.substring(nm.lastIndexOf('.') + 1);
        if(xn.isXML) {
          nm = nm + "(XML)";
          setToolTipText(nm + " (loaded from XML)");
        }
        else {
          nm = nm + "(C)";
          setToolTipText(nm + " (loaded from .class)");
        }
        value = nm;
      }
      else {
        if(leaf)        // don't show a leaf icon for a directory in the filesys which doesn't happen to have contents
          setLeafIcon(LegosTree.this.standardNonLeafIcon);
        setToolTipText(uo.toString());
        value = value.toString();
        sel = false;
      }
      return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
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
      if (f.isDirectory()) {
        if( dirsToo == false)
          return false;
        else
          return true;
      }

      if (f.isFile()) {
        if(f.getName().endsWith(".class"))
          return true;
        if(f.getName().endsWith(".xml"))
          return true;
        else
          return false;
      }

      return false;
    }
  }

  // Drag stuff
  public void dragGestureRecognized(DragGestureEvent e)
  {
    if(lis == null)
      return;
    Object o = getUO();
    if(o == null)
      return;
    Transferable xfer = null;

    if (o instanceof FileBasedAssyNode) {
      FileBasedAssyNode xn = (FileBasedAssyNode)o;

      StringSelection ss = new StringSelection(targetClassName + "\t" + xn.toString());
      lis.startingDrag(ss);
      xfer = ss;
    }
    else if (o instanceof Class) {
      String s = getClassName(o);
      if (s == null)
        return;
      StringSelection ss = new StringSelection(targetClassName + "\t" + s);
      lis.startingDrag(ss);
      xfer = ss;
    }
    else
      return; // 24 Nov 04
    
    e.startDrag(DragSource.DefaultCopyDrop, myLeafIconImage,
      new Point(-myLeafIcon.getIconWidth() / 2, -myLeafIcon.getIconHeight() / 2), xfer, this);
  }

  public void dragDropEnd(DragSourceDropEvent e){}
  public void dragEnter(DragSourceDragEvent e){}
  public void dragExit(DragSourceEvent e){}
  public void dragOver(DragSourceDragEvent e){}
  public void dropActionChanged(DragSourceDragEvent e){}

  public Object getUO()
  {
    DefaultMutableTreeNode dmtn = null;
    TreePath path = getLeadSelectionPath();
    if(path == null)
      return path;
    dmtn = (DefaultMutableTreeNode) path.getLastPathComponent();
    return dmtn.getUserObject();
  }

  public String getClassName(Object o)
  {
    if (o == null)
      return null;
    if (o instanceof Class)
      return ((Class) o).getName();
    if (o instanceof FileBasedAssyNode)
      return ((FileBasedAssyNode) o).loadedClass;
    return
        null;
  }
}


