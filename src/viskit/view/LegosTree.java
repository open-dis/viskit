package viskit.view;

import static edu.nps.util.GenericConversion.newListObjectTypeArray;
import edu.nps.util.LogUtils;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.jar.JarFile;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.*;
import org.apache.log4j.Logger;
import viskit.util.FileBasedAssyNode;
import viskit.control.FileBasedClassManager;
import viskit.util.FindClassesForInterface;
import viskit.ParameterMap;
import viskit.VGlobals;
import viskit.VStatics;
import viskit.control.AssemblyControllerImpl;
import viskit.xsd.bindings.eventgraph.ObjectFactory;
import viskit.xsd.bindings.eventgraph.Parameter;

/**<p>
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:44:31 AM
 * @version $Id$
 */
public class LegosTree extends JTree implements DragGestureListener, DragSourceListener {

    static Logger log = LogUtils.getLogger(LegosTree.class);

    private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
    private Class<?> targetClass;
    private String targetClassName;
    private Color background = new Color(0xFB, 0xFB, 0xE5);
    private ImageIcon myLeafIcon;
    private Icon standardNonLeafIcon;
    private Image myLeafIconImage;
    private DefaultTreeModel mod;
    private DragStartListener lis;
    private Vector<String> recurseNogoList;
    private String genericTableToolTip = "Drag onto canvas";

    String userDir = System.getProperty("user.dir");
    String userHome = System.getProperty("user.home");
    String name;

    /** Constructor for Listener Event Graph Object Tree
     *
     * @param className a class to evaluate as a LEGO
     * @param iconPath path to a LEGO icon
     * @param dslis a DragStartListener
     * @param tooltip description for this LEGO tree
     */
    LegosTree(String className, String iconPath, DragStartListener dslis, String tooltip) {
        this(className, new ImageIcon(VGlobals.instance().getWorkClassLoader().getResource(iconPath)), dslis, tooltip);
    }

    /** Constructor for Listener Event Graph Object Tree
     *
     * @param className a class to evaluate as a LEGO
     * @param icon a LEGO icon
     * @param dslis a DragStartListener
     * @param tooltip description for this LEGO tree
     */
    LegosTree(String className, ImageIcon icon, DragStartListener dslis, String tooltip) {
        super();
        setModel(mod = new DefaultTreeModel(rootNode));
        directoryRoots = new HashMap<>();

        lis = dslis;
        targetClassName = className;
        genericTableToolTip = tooltip;

        targetClass = VStatics.classForName(targetClassName);

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

        LegosTree instance = this;

        dragSource.createDefaultDragGestureRecognizer(instance, // component where drag originates
                DnDConstants.ACTION_COPY_OR_MOVE, instance);
    }

    // beginning of hack to hide the tree rootNode
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bugHack) {
            doBugHack();
        }
    }
    private boolean bugHack = true;

    private void doBugHack() {
        expandRow(0);
        setRootVisible(false);
        collapseRow(0);
        bugHack = false;
    }
    // end of hack to hide the tree rootNode

    /**
     * Override to provide a global tooltip for entire table..not just for nodes
     *
     * @param event mouse event
     * @return tooltip string
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        String s = super.getToolTipText(event);
        return s == null ? genericTableToolTip : s;
    }

    /** @return a class of type simkit.BasicSimEntity */
    public Class<?> getTargetClass() {
        return targetClass;
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

    public void removeContentRoot(File f) {
        //System.out.println("LegosTree.removeContentRoot: "+f.getAbsolutePath());
        if (_removeNode(rootNode, f) != null) {
            // Do nothing?
        } // System.out.println("...success");
        else {
             // Do nothing?
        }
    }

    private DefaultMutableTreeNode _removeNode(DefaultMutableTreeNode dmtn, File f) {
        for (int i = 0; i < dmtn.getChildCount(); i++) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) dmtn.getChildAt(i);
            if (n != null) {
                Object uo = n.getUserObject();
                if (!(uo instanceof FileBasedAssyNode)) {

                    // Keep looking for a FBAN in the root branches
                    _removeNode(n, f);
                } else {
                    FileBasedAssyNode fban = (FileBasedAssyNode) uo;
                    try {
                        if (fban.xmlSource.getCanonicalPath().equals(f.getCanonicalPath())) {
                            mod.removeNodeFromParent(n);
                            FileBasedClassManager.instance().unloadFile(fban);
                            return n;
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
        return null;
    }

    // 4 May 06 JMB The filter down below checks for empty dirs.
    /** If there is a directory, or a jarfile with xml in it, it will show in
     * the LEGO tree, but if its children have errors when marshaling they will
     * not appear.
     *
     * @param f the path to evaluate for SimEntities
     */
    public void addContentRoot(File f) {
        if (!f.getName().contains("svn")) {
            if (f.getName().toLowerCase().endsWith(".jar")) {
                addJarFile(f.getPath());
            } else if (!f.getName().endsWith(".java")) {
                addContentRoot(f, false);
            }
        }
    }

    /** Adds SimEntity icons to the Assembly Editor drag and drop tree
     *
     * @param f the directory to recurse to find SimEntitiy based EGs
     * @param recurse if true, recurse the directory
     */
    public void addContentRoot(File f, boolean recurse) {
        Vector<DefaultMutableTreeNode> v = new Vector<>();

        if (recurse) {
            recurseNogoList = new Vector<>();
        } else {
            recurseNogoList = null;
        }
        if (!f.getName().endsWith(".java")) {
            addContentRoot(f, recurse, v);
        }
    }

    // The two above are the public ones.
    private void addContentRoot(File f, boolean recurse, Vector<DefaultMutableTreeNode> rootVector) {
        DefaultMutableTreeNode myNode;

        // Prevent duplicates of the EG icons
        removeContentRoot(f);

        if (f.isDirectory()) {
            if (!recurse) {
                myNode = new DefaultMutableTreeNode(f.getPath());
                rootNode.add(myNode);
                rootVector.add(myNode); // for later pruning
                directoryRoots.put(f.getPath(), myNode);
                int idx = rootNode.getIndex(myNode);
                mod.nodesWereInserted(rootNode, new int[] {idx});

                File[] fa = f.listFiles(new MyClassTypeFilter(false));
                for (File file : fa) {
                    addContentRoot(file, recurse, rootVector);
                }
            } else { // recurse = true
                // Am I here?  If so, grab my treenode
                // Else is my parent here?  If so, hook me as child
                // If not, put me in under the rootNode
                myNode = directoryRoots.get(f.getPath());
                if (myNode == null) {
                    myNode = directoryRoots.get(f.getParent());
                    if (myNode != null) {
                        DefaultMutableTreeNode parent = myNode;
                        myNode = new DefaultMutableTreeNode(f.getName());
                        parent.add(myNode);
                        directoryRoots.put(f.getPath(), myNode);
                        int idx = parent.getIndex(myNode);
                        mod.nodesWereInserted(parent, new int[] {idx});
                    } else {

                        // Shorten long path names
                        // TODO: use VConfig to get the project path
                        if (f.getPath().contains(userDir)) {
                            name = f.getPath().substring(userDir.length() + 1, f.getPath().length());
                        } else if (f.getPath().contains(userHome)) {
                            name = f.getPath().substring(userHome.length() + 1, f.getPath().length());
                        } else {
                            name = f.getPath();
                        }

                        myNode = new DefaultMutableTreeNode(name);
                        rootNode.add(myNode);
                        rootVector.add(myNode); // for later pruning
                        directoryRoots.put(f.getPath(), myNode);
                        int idx = rootNode.getIndex(myNode);
                        mod.nodesWereInserted(rootNode, new int[] {idx});
                    }
                }
                File[] fa = f.listFiles(new MyClassTypeFilter(true));
                for (File file : fa) {
                    addContentRoot(file, recurse, rootVector);
                }
            }   // recurse = true
        } // is directory
        // We're NOT a directory...
        else {
            FileBasedAssyNode fban;
            try {

                // This call generates the source, compiles and validates EG XML files
                fban = FileBasedClassManager.instance().loadFile(f, getTargetClass());

                if (fban != null) {
                    myNode = new DefaultMutableTreeNode(fban);
                    DefaultMutableTreeNode par = directoryRoots.get(f.getParent());
                    if (par != null) {
                        par.add(myNode);
                        int idx = par.getIndex(myNode);
                        mod.nodesWereInserted(par, new int[] {idx});
                    } else {
                        rootNode.add(myNode);
                        int idx = rootNode.getIndex(myNode);
                        mod.nodesWereInserted(rootNode, new int[] {idx});
                    }
                } else {
                    log.warn("Compile problem encountered with generated source code for " + f.getName());
                    log.warn(f.getName() + " will not be listed in the Event Graphs node tree\n");
                }

                // Note:
                // On initial startup with valid XML, but bad compilation,
                // dirty won't get set b/c the graph model is null until the
                // model tab is created and the EG file is opened.  First pass
                // is only for inclusion on in the LEGOs tree
                if (VGlobals.instance().getActiveEventGraphModel() != null) {
                    VGlobals.instance().getActiveEventGraphModel().setDirty(fban == null);
                    VGlobals.instance().getEventGraphEditor().toggleEgStatusIndicators();
                }

            } catch (Throwable throwable) {

                // Uncomment to reveal common reason for Exceptions
                throwable.printStackTrace();
                log.error(throwable);
                if (recurseNogoList != null) {
                    recurseNogoList.add(f.getName());
                }
            }
        } // directory
    }
    Map<String, DefaultMutableTreeNode> directoryRoots;
    Map<String, DefaultMutableTreeNode> packagesHM = new HashMap<>();

    private void hookToParent(Class<?> c, DefaultMutableTreeNode myroot) {
        String pkg = c.getPackage().getName();
        DefaultMutableTreeNode dmtn = getParent(pkg, myroot);
        dmtn.add(new DefaultMutableTreeNode(c));
    }

    DefaultMutableTreeNode getParent(String pkg, DefaultMutableTreeNode lroot) {
        DefaultMutableTreeNode parent = packagesHM.get(pkg);

        if (parent == null) {
            if (!pkg.contains(".")) {
                // we're as far up as we can be
                parent = new DefaultMutableTreeNode(pkg);
                mod.insertNodeInto(parent, lroot, 0);
            } else {
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

    /** Adds SimEntity icons to the Assembly Editor drag and drop tree
     *
     * @param f the jar to evaluate for SimEntitiy based EGs
     */
    private void addJarFile(String jarFilePath) {
        JarFile jf;
        try {
            jf = new JarFile(jarFilePath);
        } catch (IOException e) {
            ((AssemblyControllerImpl)VGlobals.instance().getAssemblyController()).messageUser(
                    JOptionPane.ERROR_MESSAGE,
                    "I/O Error", "Error reading " + jarFilePath);
            return;
        }
        jarFileCommon(jf);
    }

    @SuppressWarnings("unchecked")
    private void jarFileCommon(JarFile jarFile) {

        // Prevent a case where we have simkit.jar in both the working classpath
        // and in a project's /lib directory.  We don't need to expose multiple
        // libs of the same name because they happen to be in two different
        // places
        Enumeration<DefaultMutableTreeNode> e = rootNode.children();
        String jarName = jarFile.getName().substring(jarFile.getName().lastIndexOf(File.separator) + 1);
        DefaultMutableTreeNode tn;
        while (e.hasMoreElements()) {
            tn = e.nextElement();
            if (tn.getUserObject().toString().contains(jarName)) {
                return;
            }
        }

        List<Class<?>> list = FindClassesForInterface.findClasses(jarFile, targetClass);
        for (Class<?> c : list) {
            Constructor<?>[] constr = c.getConstructors();
            List<Object>[] plist = newListObjectTypeArray(ArrayList.class, constr.length);
            ObjectFactory of = new ObjectFactory();
            Field f = null;
            try {
                f = c.getField("parameterMap");
            } catch (SecurityException ex) {
                log.error(ex);
//                ex.printStackTrace();
            } catch (NoSuchFieldException ex) {}

            if (viskit.VStatics.debug) {
                System.out.println("adding " + c.getName());
            }
            if (viskit.VStatics.debug) {
                System.out.println("\t # constructors: " + constr.length);
            }

            for (int i = 0; i < constr.length; i++) {
                Class<?>[] ptypes = constr[i].getParameterTypes();
                plist[i] = new ArrayList<>();
                if (viskit.VStatics.debug) {
                    System.out.println("\t # params " + ptypes.length + " in constructor " + i);
                }

                ParameterMap param = constr[i].getAnnotation(viskit.ParameterMap.class);
                // possible that a class inherited a parameterMap, check if annotated first
                if (param != null) {
                    String[] names = param.names();
                    String[] types = param.types();
                    if (names.length != types.length) {
                        throw new RuntimeException("ParameterMap names and types length mismatch");
                    }
                    for (int k = 0; k < names.length; k++) {
                        Parameter pt = of.createParameter();
                        pt.setName(names[k]);
                        pt.setType(types[k]);

                        plist[i].add(pt);
                    }

                } else if (f != null) {
                    if (viskit.VStatics.debug) {
                        System.out.println(f + " is a parameterMap");
                    }
                    try {
                        // parameters are in the following order
                        // {
                        //  { "type0","name0","type1","name1",... }
                        //  { "type0","name0", ... }
                        //  ...
                        // }
                        String[][] parameterMap = (String[][]) (f.get(new String[0][0]));
                        int numConstrs = parameterMap.length;

                        for (int n = 0; n < numConstrs; n++) { // tbd: check that numConstrs == constr.length
                            String[] params = parameterMap[n];
                            if (params != null) {
                                plist[n] = new ArrayList<>();
                                for (int k = 0; k < params.length; k += 2) {
                                    try {
                                        Parameter p = of.createParameter();
                                        String ptype = params[k];
                                        String pname = params[k + 1];

                                        p.setName(pname);
                                        p.setType(ptype);

                                        plist[n].add(p);
                                        if (viskit.VStatics.debug) {
                                            System.out.println("\tfrom compiled parameterMap" + p.getName() + p.getType());
                                        }
                                    } catch (Exception ex) {
                                        log.error(ex);
//                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                        break; // fix this up, should index along with i not n
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        log.error(ex);
//                        ex.printStackTrace();
                    }
                } else {// unknonws
                    for (int k = 0; k < ptypes.length; k++) {
                        try {
                            Parameter p = of.createParameter();
                            String ptname = VStatics.convertClassName(ptypes[k].getName());
                            if (ptname.indexOf(".class") > 0) { //??
                                ptname = ptname.split("\\.")[0];
                            }
                            p.setName("p[" + k + "] : ");
                            p.setType(ptname);

                            plist[i].add(p);
                            if (viskit.VStatics.debug) {
                                System.out.println("\t " + p.getName() + p.getType());
                            }
                        } catch (Exception ex) {
                            log.error(ex);
//                            ex.printStackTrace();
                        }
                    }
                }
            }
            VStatics.putParameterList(c.getName(), plist);
        }

        // Shorten long path names
        // TODO: use VConfig to get the project path
        if (jarFile.getName().contains(userDir)) {
            name = jarFile.getName().substring(userDir.length()+1, jarFile.getName().length());
        } else if (jarFile.getName().contains(userHome)) {
            name = jarFile.getName().substring(userHome.length()+1, jarFile.getName().length());
        } else {
            name = jarFile.getName();
        }

        if (list == null || list.isEmpty()) {
            log.warn("No classes of type " + targetClassName + " found in " + name);
            log.info(name + " will not be listed in the Assembly Editor's Event Graphs SimEntity node tree\n");
        } else {

            DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(name);
            mod.insertNodeInto(localRoot, rootNode, 0);

            for (Class<?> c : list) {
                hookToParent(c, localRoot);
            }
        }
    }

    class MyClassSorter implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            String s = ((Class) o1).getName();
            s = s.substring(s.lastIndexOf('.') + 1);
            String s2 = ((Class) o2).getName();
            s2 = s2.substring(s2.lastIndexOf('.') + 1);
            return (s.compareTo(s2));
        }
    }

    class MyRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object uo = ((DefaultMutableTreeNode) value).getUserObject();
            setLeafIcon(LegosTree.this.myLeafIcon); // default

            if (uo instanceof Class<?>) {
                Class<?> c = (Class<?>) uo;
                String nm = c.getName();

                setToolTipText(nm);
                nm = nm.substring(nm.lastIndexOf('.') + 1);
                //      if(sel)
                //        nm = "<html><b>"+nm+"</b></html>";   // sizes inst screwed up
                value = nm;
            } else if (uo instanceof FileBasedAssyNode) {
                FileBasedAssyNode xn = (FileBasedAssyNode) uo;
                String nm = xn.loadedClass;
                nm = nm.substring(nm.lastIndexOf('.') + 1);
                if (xn.isXML) {
                    nm += "(XML)";
                    setToolTipText(nm + " (loaded from XML)");
                } else {
                    nm += "(C)";
                    setToolTipText(nm + " (loaded from .class)");
                }
                value = nm;
            } else {
                if (leaf) // don't show a leaf icon for a directory in the filesys which doesn't happen to have contents
                {
                    setLeafIcon(LegosTree.this.standardNonLeafIcon);
                }
                setToolTipText(uo.toString());
                value = value.toString();
                sel = false;
            }
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        }
    }

    class MyClassTypeFilter implements java.io.FileFilter {

        boolean dirsToo;

        MyClassTypeFilter(boolean inclDirs) {
            dirsToo = inclDirs;
        }

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                if (!dirsToo) {
                    return false;
                }
                // TBD add an ignore in SettingsDialog, and in history file
                if (f.getName().contains("svn") || f.getName().contains("Assemblies") || f.getName().contains("Assembly") || f.getName().contains("Scenario") || f.getName().contains("Locations")) {
                    return false;
                }
                File[] fa = f.listFiles(new MyClassTypeFilter(true));
                return (fa != null || fa.length != 0);
            }

            return f.isFile() &&
                    (f.getName().endsWith(".class") || (f.getName().endsWith(".xml")));
        }
    }

    //** DragGestureListener **

    @Override
    public void dragGestureRecognized(DragGestureEvent e) {
        if (lis == null) {
            return;
        }

        Object o = getUO();
        if (o == null) {
            return;
        }

        Transferable xfer;
        StringSelection ss;

        if (o instanceof FileBasedAssyNode) {
            FileBasedAssyNode xn = (FileBasedAssyNode) o;
            ss = new StringSelection(targetClassName + "\t" + xn.toString());
        } else if (o instanceof Class<?>) {
            String s = getClassName(o);
            if (s == null) {
                return;
            }
            ss = new StringSelection(targetClassName + "\t" + s);
        } else {
            return;
        } // 24 Nov 04

        lis.startingDrag(ss);
        xfer = ss;
        try {
            e.startDrag(DragSource.DefaultCopyDrop, myLeafIconImage,
                    new Point(-myLeafIcon.getIconWidth() / 2, -myLeafIcon.getIconHeight() / 2), xfer, this);
        } catch (java.awt.dnd.InvalidDnDOperationException dnde) {
             // Do nothing?
            // nop, it works, makes some complaint, but works, why?
        }
    }

    // ** DragSourceListener **

    @Override
    public void dragDropEnd(DragSourceDropEvent e) {
    }

    @Override
    public void dragEnter(DragSourceDragEvent e) {
    }

    @Override
    public void dragExit(DragSourceEvent e) {
    }

    @Override
    public void dragOver(DragSourceDragEvent e) {
    }

    @Override
    public void dropActionChanged(DragSourceDragEvent e) {
    }

    public Object getUO() {
        TreePath path = getLeadSelectionPath();
        if (path == null) {
            return path;
        }
        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) path.getLastPathComponent();
        return dmtn.getUserObject();
    }

    public String getClassName(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Class<?>) {
            return ((Class<?>) o).getName();
        }
        if (o instanceof FileBasedAssyNode) {
            return ((FileBasedAssyNode) o).loadedClass;
        }
        return null;
    }

    /** Clear the queue of all SimEntities and Property Change Listeners */
    public void clear() {
        rootNode.removeAllChildren();
        if (directoryRoots != null) {
            directoryRoots.clear();
        }
    }

} // end class file LegosTree.java