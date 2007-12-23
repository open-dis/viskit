package viskit;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.JarFile;

import static edu.nps.util.GenericConversion.newListObjectTypeArray;
import viskit.xsd.bindings.eventgraph.ObjectFactory;
import viskit.xsd.bindings.eventgraph.Parameter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:44:31 AM
 * @version $Id: LegosTree.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class LegosTree extends JTree implements DragGestureListener, DragSourceListener
{
  private DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
  private Class targetClass;
  private String targetClassName;
  private Color background = new Color(0xFB, 0xFB, 0xE5);
  private ImageIcon myLeafIcon;
  private Icon standardNonLeafIcon;
  private Image myLeafIconImage;
  DefaultTreeModel mod;
  private DragStartListener lis;
  private Vector<String> recurseNogoList;
  private String genericTableToolTip = "Drag onto canvas";
  
  LegosTree(String className, String iconPath, DragStartListener dslis, String tooltip)
  {
    this(className, new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(iconPath)), dslis, tooltip);
  }

  LegosTree(String className, ImageIcon icon, DragStartListener dslis, String tooltip)
  {
    super();
    setModel(mod = new DefaultTreeModel(root));

    lis = dslis;
    targetClassName = className;
    genericTableToolTip = tooltip;

    try {
      targetClass = Class.forName(targetClassName, false, this.getClass().getClassLoader()); //"simkit.BasicSimEntity");
    }
    catch (ClassNotFoundException e) {
      System.out.println("Error:  Need simkit classes in classpath");
      e.printStackTrace();
      return;
    }

    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    MyRenderer rendr = new MyRenderer();
    setCellRenderer(rendr);

    setToolTipText("");  // needs to be done first to enable tt below
    setRootVisible(true); // we want this to be false, but there is some sort of JTree bug...see paintComponent override below
    setShowsRootHandles(true);
    setVisibleRowCount(100);    // means always fill a normal size panel
    rendr.setBackgroundNonSelectionColor(background);

    myLeafIcon = icon;
    myLeafIconImage = myLeafIcon.getImage();
    standardNonLeafIcon = rendr.getOpenIcon();

    rendr.setLeafIcon(myLeafIcon);
    DragSource dragSource = DragSource.getDefaultDragSource();

    dragSource.createDefaultDragGestureRecognizer(this, // component where drag originates
        DnDConstants.ACTION_COPY_OR_MOVE, this);
  }

  // beginning of hack to hide the tree root
  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if(bugHack)
      doBugHack();
  }

  private boolean bugHack = true;
  private void doBugHack()
  {
    expandRow(0);
    setRootVisible(false);
    collapseRow(0);
    bugHack = false;
  }

  // end of hack to hide the tree root

  /**
   * Override to provide a global tooltip for entire table..not just for nodes
   *
   * @param event mouse event
   * @return tooltip string
   */
  @Override
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

  public void removeContentRoot(File f)
  {
    //System.out.println("LegosTree.removeContentRoot: "+f.getAbsolutePath());
    if (_removeNode(root, f) != null)
      ; // System.out.println("...success");
    else
      ; // System.out.println("...failure");
  }

  private DefaultMutableTreeNode _removeNode(DefaultMutableTreeNode dmtn, File f)
  {
    if (dmtn.getChildCount() > 0) {
      for (int i = 0; i < dmtn.getChildCount(); i++) {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) dmtn.getChildAt(i);
        if (n != null) {
          Object uo = n.getUserObject();
          if (! (uo instanceof FileBasedAssyNode))
            continue;
          FileBasedAssyNode fban = (FileBasedAssyNode) uo;
          try {
            if (fban.xmlSource.getCanonicalPath().equals(f.getCanonicalPath())) {
              mod.removeNodeFromParent(n);
              FileBasedClassManager.inst().unloadFile(fban);
              return n;
            }
          }
          catch (IOException e) {
            System.out.println("getCanonicalPath in LegosTree : " + e.getMessage());
          }
        }
      }
    }
    return null;
  }

  // 2do if no valid leaves are found, remove the directory...as it is, it is represented in
  // the tree with the node icon.

  // 4 May 06 JMB The filter down below checks for empty dirs.  If there is a directory
  // with xml in it, it will show, but if it's children have errors when marshalling,
  // they will not appear.

  public void addContentRoot(File f)
  {
    if (!f.getName().equals("CVS")) {
      if (f.getName().toLowerCase().endsWith(".jar"))
        addJarFile(f.getPath());
      else if ( !f.getName().endsWith(".java") )
        addContentRoot(f, false);
    }
  }

  public void addContentRoot(File f, boolean recurse)
  {
    Vector<DefaultMutableTreeNode> v = new Vector<DefaultMutableTreeNode>();
    directoryRoots = new HashMap<String, DefaultMutableTreeNode>();
    classNodeCount = 0;

    if (recurse)
      recurseNogoList = new Vector<String>();
    else
      recurseNogoList = null;
    if ( !f.getName().endsWith(".java") )
        addContentRoot(f, recurse, v);
    /* Skip the bad news reporting
    if(recurseNogoList != null && recurseNogoList.size()>0) {
    JOptionPane.showMessageDialog(this,recurseNogoList.toArray(new String[0]),"Classes or files not added:",JOptionPane.INFORMATION_MESSAGE);
    }
    */

    if (classNodeCount != 0)
      return;

    JOptionPane.showMessageDialog(LegosTree.this, "Compile error in " + f.getName() + ",\n" +
        "or no classes of type " + targetClass.getName() +
        " found,\n" +
        "or only duplicate class type(s) encountered.",
        "Error", JOptionPane.WARNING_MESSAGE);
  }

  // The two above are the public ones.
  private void addContentRoot(File f, boolean recurse, Vector<DefaultMutableTreeNode> rootVector)
  {
    DefaultMutableTreeNode myNode;
    removeContentRoot(f);
    if (f.isDirectory()) {
      if (!recurse) {
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
            myNode = new DefaultMutableTreeNode(f.getName());
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
        for (File file : fa) {
            addContentRoot(file, recurse, rootVector);
        }
      }   // recurse = true
    }     // is directory

    // We're NOT a directory...
    else {
      FileBasedAssyNode fban;
      try {
        fban = FileBasedClassManager.inst().loadFile(f);
        if (fban == null) return;
        // Check here for duplicates of the classes which have been loaded on the classpath (simkit.jar);
        // No dups accepted, should throw exception upon success
        try {
          Class.forName(fban.loadedClass);
          return;  // don't proceed
        } catch (Exception e) { // expectecd
            ;//e.printStackTrace();
        }
        
      } catch (Throwable throwable) {
        if ( viskit.Vstatics.debug ) throwable.printStackTrace();
        System.err.println("Couldn't handle " + f + ". " + throwable.getMessage());
        if (recurseNogoList != null)
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

  private int classNodeCount;
  HashMap<String, DefaultMutableTreeNode> directoryRoots;
  DefaultMutableTreeNode rootNode;

  HashMap<String, DefaultMutableTreeNode> packagesHM = new HashMap<String, DefaultMutableTreeNode>();

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
    JarFile jf;
    try {
      jf = new JarFile(jarFilePath);
    }
    catch (IOException e) {
      JOptionPane.showMessageDialog(LegosTree.this, "Error reading " + jarFilePath, "I/O Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    jarFileCommon(jf);
  }

  private void jarFileCommon(JarFile jarFile) {
      List<Class<?>> list = FindClassesForInterface.findClasses(jarFile, targetClass);
      for (Class<?> c : list) {
          Constructor[] constr = c.getConstructors();
          List<Object>[] plist = newListObjectTypeArray(ArrayList.class, constr.length);
          ObjectFactory of = new ObjectFactory();
          Field f = null;
          try {
              f = c.getField("parameterMap");
          } catch (SecurityException ex) {
              ex.printStackTrace();
          } catch (NoSuchFieldException ex) {
          }
          if (viskit.Vstatics.debug) System.out.println("adding " + c.getName());          
          
          if (viskit.Vstatics.debug) System.out.println("\t # constructors: " + constr.length);
          for (int i = 0; i < constr.length; i ++) {
              Class[] ptypes = constr[i].getParameterTypes();
              plist[i] = new ArrayList<Object>();
              if (viskit.Vstatics.debug) System.out.println("\t # params " + ptypes.length + " in constructor " + i);
              
              ParameterMap param = constr[i].getAnnotation(viskit.ParameterMap.class);
              // possible that a class inherited a parameterMap, check if annotated first
              if ( param != null ) { 
                  String[] names = ((ParameterMap)param).names();
                  String[] types = ((ParameterMap)param).types();
                  if (names.length != types.length) throw new RuntimeException("ParameterMap names and types length mismatch");
                  for ( int k = 0; k < names.length; k ++) {
                      Parameter pt;
                      pt = of.createParameter();
                      pt.setName(names[k]);
                      pt.setType(types[k]);

                      plist[i].add(pt);
                  }
                  
              } else if ( f != null ) {
                  if (viskit.Vstatics.debug) System.out.println(f+" is a parameterMap");
                  try {
                      // parameters are in the following order
                      // {
                      //  { "type0","name0","type1","name1",... }
                      //  { "type0","name0", ... }
                      //  ...
                      // }
                      String[][] parameterMap = (String[][])(f.get(new String[0][0]));
                      int numConstrs = parameterMap.length;
                      
                      for (int n = 0; n < numConstrs; n++) { // tbd: check that numConstrs = constr.length
                          String[] params = parameterMap[n];
                          if (params != null) {
                              plist[n] = new ArrayList<Object>();
                              for (int k = 0; k < params.length; k+=2) {
                                  try {
                                      Parameter p = of.createParameter();
                                      String ptype = params[k];
                                      String pname = params[k+1];
                                      
                                      p.setName(pname);
                                      p.setType(ptype);
                                      
                                      plist[n].add(p);
                                      if (viskit.Vstatics.debug) System.out.println("\tfrom compiled parameterMap" + p.getName() + p.getType());
                                  } catch (Exception e) {
                                      e.printStackTrace();
                                  }
                              }
                          }
                      } break; // fix this up, should index along with i not n
                  } catch (IllegalArgumentException ex) {
                      ex.printStackTrace();
                  } catch (IllegalAccessException ex) {
                      ex.printStackTrace();
                  }
              } else {// unknonws
                  for (int k = 0; k < ptypes.length; k++) {
                      try {
                          Parameter p = of.createParameter();
                          String ptname = Vstatics.convertClassName(ptypes[k].getName());
                          if (ptname.indexOf(".class") > 0) { //??
                              ptname = ptname.split("\\.")[0];
                          }
                          p.setName("p[" + k + "] : ");
                          p.setType(ptname);
                          
                          plist[i].add(p);
                          if (viskit.Vstatics.debug) System.out.println("\t " + p.getName() + p.getType());
                      } catch (Exception e) {
                          e.printStackTrace();
                      }
                      
                  }
              }
          } // for
          Vstatics.putParameterList(c.getName(), plist);
      }
      
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
    @Override
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
        FileBasedAssyNode xn = (FileBasedAssyNode) uo;
        String nm = xn.loadedClass;
        nm = nm.substring(nm.lastIndexOf('.') + 1);
        if (xn.isXML) {
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
        if (leaf)        // don't show a leaf icon for a directory in the filesys which doesn't happen to have contents
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
        if (!dirsToo)
          return false;
        // TBD add an ignore in SettingsDialog, and in history file
        if (       
                   f.getName().equals("CVS") 
                || f.getName().indexOf("Assemblies") > -1 
                || f.getName().indexOf("Assembly") > -1 
                || f.getName().indexOf("Scenario") > -1 
                || f.getName().indexOf("Locations") > -1 
                
           )
          return false;
        File[] fa = f.listFiles(new MyClassTypeFilter(true));
        if(fa == null || fa.length <= 0)
          return false;   // don't include empty dirs.

        else
          return true;
      }

      return f.isFile() &&
            (f.getName().endsWith(".class") || (f.getName().endsWith(".xml")));
    }
  }

  // Drag stuff
  public void dragGestureRecognized(DragGestureEvent e)
  {
    if (lis == null)
      return;
    Object o = getUO();
    if (o == null)
      return;
    Transferable xfer;

    if (o instanceof FileBasedAssyNode) {
      FileBasedAssyNode xn = (FileBasedAssyNode) o;

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
    try {
        e.startDrag(DragSource.DefaultCopyDrop, myLeafIconImage,
            new Point(-myLeafIcon.getIconWidth() / 2, -myLeafIcon.getIconHeight() / 2), xfer, this);
    } catch ( java.awt.dnd.InvalidDnDOperationException dnde ) {
        ;// nop, it works, makes some complaint, but works, why? 
    }
  }

  public void dragDropEnd(DragSourceDropEvent e)
  {
  }

  public void dragEnter(DragSourceDragEvent e)
  {
  }

  public void dragExit(DragSourceEvent e)
  {
  }

  public void dragOver(DragSourceDragEvent e)
  {
  }

  public void dropActionChanged(DragSourceDragEvent e)
  {
  }

  public Object getUO()
  {
    TreePath path = getLeadSelectionPath();
    if (path == null)
      return path;
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) path.getLastPathComponent();
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
  
  public void clear() {
      root.removeAllChildren();
      if (directoryRoots != null) directoryRoots.clear();
  }
}


