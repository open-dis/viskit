package viskit.view;


import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.jar.JarFile;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.*;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import viskit.util.FileBasedAssemblyNode;
import viskit.control.FileBasedClassManager;
import viskit.util.FindClassesForInterface;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import static viskit.ViskitUserConfiguration.SYSTEM_USER_DIR;
import static viskit.ViskitUserConfiguration.SYSTEM_USER_HOME;

/**
 * Class to support creating a Listener Event Graph Object (LEGO) tree on the
 * Assembly Editor. Used for dragging and dropping Event Graph and PCL nodes to the pallette
 * for creating Assembly files.
 *
 * <pre>
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * </pre>
 *
 * @author Mike Bailey
 * @since May 14, 2004
 * @since 9:44:31 AM
 * @version $Id$
 */
public class LegoTree extends JTree implements DragGestureListener, DragSourceListener 
{
    static final Logger LOG = LogManager.getLogger();

    private DefaultMutableTreeNode rootNode;

    private Class<?> targetClass;

    private String targetClassName;

    private Color background;

    private ImageIcon myLeafIcon;

    private Icon standardNonLeafIcon;

    private Image myLeafIconImage;

    private DefaultTreeModel defaultTreeModel;

    private DragStartListener dragStartListener;

    private String genericTableToolTip = "Drag onto canvas";

    String userDir  = SYSTEM_USER_DIR;

    String userHome = SYSTEM_USER_HOME;

    String projectPath = new String();

    String name;

    /**
     * Constructor for Listener Event Graph Object Tree
     *
     * @param className a class to evaluate as a LEGO
     * @param iconPath path to a LEGO icon
     * @param dragStartListener a DragStartListener
     * @param tooltip description for this LEGO tree
     */
    LegoTree(String className, String iconPath, DragStartListener dragStartListener, String tooltip) {
        this(className, new ImageIcon(dragStartListener.getClass().getClassLoader().getResource(iconPath)), dragStartListener, tooltip);
    }

    /**
     * Constructor for Listener Event Graph Object Tree
     *
     * @param className a class to evaluate as a LEGO
     * @param icon a LEGO icon
     * @param dragStartListener a DragStartListener
     * @param tooltip description for this LEGO tree
     */
    LegoTree(String className, ImageIcon icon, DragStartListener dragStartListener, String tooltip) {
        super();
        rootNode = new DefaultMutableTreeNode("root");
        background = new Color(0xFB, 0xFB, 0xE5);
        setModel(defaultTreeModel = new DefaultTreeModel(rootNode));
        directoryRootsHashMap = new HashMap<>();

        dragStartListener = dragStartListener;
        targetClassName = className;
        genericTableToolTip = tooltip;

        targetClass = ViskitStatics.classForName(targetClassName);

        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        MyRenderer myRenderer = new MyRenderer();
        setCellRenderer(myRenderer);

        setToolTipText("");  // needs to be done first to enable tt below
        setRootVisible(true); // we want this to be false, but there is some sort of JTree bug...see paintComponent override below
        setShowsRootHandles(true);
        setVisibleRowCount(100);    // means always fill a normal size panel
        myRenderer.setBackgroundNonSelectionColor(background);

        myLeafIcon = icon;
        myLeafIconImage = myLeafIcon.getImage();
        standardNonLeafIcon = myRenderer.getOpenIcon();

        myRenderer.setLeafIcon(myLeafIcon);
        DragSource dragSource = DragSource.getDefaultDragSource();

        LegoTree instance = this;

        dragSource.createDefaultDragGestureRecognizer(instance, // component where drag originates
                DnDConstants.ACTION_COPY_OR_MOVE, instance);

        if ((ViskitGlobals.instance().getViskitProject()                  != null) &&
            (ViskitGlobals.instance().getViskitProject().getProjectDirectory() != null))
            projectPath = ViskitGlobals.instance().getViskitProject().getProjectDirectory().getPath();
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

    /**
     * @return a class of type simkit.BasicSimEntity
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    public void removeSelected() {
        TreePath[] selections;
        TreePath currentSelection;
        DefaultMutableTreeNode currentNode;
        MutableTreeNode parent;
        while ((selections = getSelectionPaths()) != null) {
            currentSelection = selections[0];
            if (currentSelection != null) {
                currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
                parent = (MutableTreeNode) (currentNode.getParent());
                if (parent != null)
                    defaultTreeModel.removeNodeFromParent(currentNode);
            }
        }
    }

    /**
     * Used to help prevent duplicate Event Graph or PCL nodes from appearing in the LEGO
     * tree on the Assembly Editor in addition to simply supporting the user by
     * removing a node
     *
     * @param fileToRemove the file to remove from the LEGO tree
     */
    public void removeContentRoot(File fileToRemove) {
        _removeNode(rootNode, fileToRemove);
    }

    private DefaultMutableTreeNode _removeNode(DefaultMutableTreeNode defaultMutableTreeNode, File file) 
    {
        DefaultMutableTreeNode n;
        Object uo;
        FileBasedAssemblyNode fileBasedAssemblyNode;
        for (int i = 0; i < defaultMutableTreeNode.getChildCount(); i++) 
        {
            n = (DefaultMutableTreeNode) defaultMutableTreeNode.getChildAt(i);
            if (n != null) 
            {
                uo = n.getUserObject();
                if (!(uo instanceof FileBasedAssemblyNode))
                {
                    // Keep looking for a FBAN in the root branches
                    _removeNode(n, file);
                } 
                else 
                {
                    fileBasedAssemblyNode = (FileBasedAssemblyNode) uo;

                    try {
                        if (fileBasedAssemblyNode.isXML && fileBasedAssemblyNode.xmlSource.getCanonicalPath().equals(file.getCanonicalPath())) {
                            defaultTreeModel.removeNodeFromParent(n);
                            FileBasedClassManager.instance().unloadFile(fileBasedAssemblyNode);
                            return n;
                        }
                    } 
                    catch (IOException e) {
                        LOG.error(e);
                    }
                }
            }
        }
        return null; // non SimEntity
    }

    // 4 May 06 JMB The filter down below checks for empty dirs.
    /**
     * Adds SimEntity icons to the Assembly Editor drag and drop tree. If there
     * is a directory, or a jar, containing .xml or .class SimEntitiy files,
     * they will show in the LEGO tree, but if the directory's children have
     * errors when marshaling XML they will not appear. Non SimEntity files will
     * also not appear
     *
     * @param file the directory to recurse to find SimEntity based Event Graphs
     * @param recurse if true, recurse the directory
     */
    public void addContentRoot(File file, boolean recurse) 
    {
        if (file.getName().toLowerCase().endsWith(".jar")) {
            addJarFile(file.getPath());
        } 
        else if (!file.getName().toLowerCase().endsWith(".java")) {
            _addContentRoot(file, recurse); // directories, .xml, .class
        }
    }

    /** Does the real work of adding content to LegoTree
     * 
     * @param rootFile file or directory
     * @param recurse whether to recurse into directories for contained files
     */
    private void _addContentRoot(File rootFile, boolean recurse) 
    {
        DefaultMutableTreeNode myNode;

        // Prevent duplicates of the Event Graph icons
        removeContentRoot(rootFile);

        if (rootFile.isDirectory()) 
        {
            if (!recurse) // do not recurse into directory
            {
                myNode = new DefaultMutableTreeNode(rootFile.getPath());
                rootNode.add(myNode);
                directoryRootsHashMap.put(rootFile.getPath(), myNode);
                int index = rootNode.getIndex(myNode);
                defaultTreeModel.nodesWereInserted(rootNode, new int[]{index});

                File[] fileArray = rootFile.listFiles(new MyClassTypeFilter(false));
                for (File nextFile : fileArray)
                    _addContentRoot(nextFile, nextFile.isDirectory());

            } 
            else  // recurse into directory
            {
                // Am I here?  If so, grab my treenode
                // Else is my parent here?  If so, hook me as child
                // If not, put me in under the rootNode
                myNode = directoryRootsHashMap.get(rootFile.getPath());
                if (myNode == null)
                {
                    myNode = directoryRootsHashMap.get(rootFile.getParent());
                    if (myNode != null) // found it
                    {
                        DefaultMutableTreeNode parent = myNode;
                        myNode = new DefaultMutableTreeNode(rootFile.getName());
                        parent.add(myNode);
                        directoryRootsHashMap.put(rootFile.getPath(), myNode);
                        int index = parent.getIndex(myNode);
                        defaultTreeModel.nodesWereInserted(parent, new int[]{index});
                    } 
                    else // keep looking
                    {
                        // Shorten long path names
                        if (rootFile.getPath().contains(userDir)) {
                            name = rootFile.getPath().substring(userDir.length() + 1, rootFile.getPath().length());
                        } 
                        else if (rootFile.getPath().contains(userHome)) {
                            name = rootFile.getPath().substring(userHome.length() + 1, rootFile.getPath().length());
                        } 
                        else if (rootFile.getPath().contains(projectPath)) {
                            name = rootFile.getPath().substring(projectPath.length() + 1, rootFile.getPath().length());
                        } 
                        else {
                            name = rootFile.getPath();
                        }
                        myNode = new DefaultMutableTreeNode(name);
                        rootNode.add(myNode);
                        directoryRootsHashMap.put(rootFile.getPath(), myNode);
                        int index = rootNode.getIndex(myNode);
                        defaultTreeModel.nodesWereInserted(rootNode, new int[]{index});
                    }
                }
                File[] fileArray = rootFile.listFiles(new MyClassTypeFilter(true));
                for (File nextFile : fileArray) 
                {
                    _addContentRoot(nextFile, nextFile.isDirectory());
                }
            }   // recurse = true
        } // is directory
        else  // We're NOT a directory...
        {
            FileBasedAssemblyNode fileBasedAssemblyNode;
            try 
            {
                // This call generates the source, compiles and validates Event Graph XML files
                // Also checks for extensions of SimEntityBase in .class files
                fileBasedAssemblyNode = FileBasedClassManager.instance().loadFile(rootFile, getTargetClass());

                if (fileBasedAssemblyNode != null)
                {
                    myNode = new DefaultMutableTreeNode(fileBasedAssemblyNode);
                    int index;
                    DefaultMutableTreeNode parentPath = directoryRootsHashMap.get(rootFile.getParent());
                    if (parentPath != null) {
                        parentPath.add(myNode);
                        index = parentPath.getIndex(myNode);
                        defaultTreeModel.nodesWereInserted(parentPath, new int[] {index});
                    } 
                    else
                    {
                        rootNode.add(myNode);
                        index = rootNode.getIndex(myNode);
                        defaultTreeModel.nodesWereInserted(rootNode, new int[] {index});
                    }
                } 
                else
                {
                    // NOTE: .class files come here if a directory is listed on
                    // the additional classpath element
                    LOG.warn("(fileBasedAssemblyNode == null)");
                    LOG.info("{} will not be listed in the Assembly Editor's SimEntity tree\n", rootFile.getName());
                }
                // Note
                // On initial startup with valid XML, but bad compilation,
                // dirty won't get set b/c the graph model is null until the
                // model tab is created and the Event Graph file is opened. First pass
                // is only for inclusion in the LEGOs tree
                if (ViskitGlobals.instance().getActiveEventGraphModel() != null) 
                {
                    ViskitGlobals.instance().getActiveEventGraphModel().setModelDirty(fileBasedAssemblyNode == null);
                    ViskitGlobals.instance().getEventGraphViewFrame().toggleEventGraphStatusIndicators();
                    ViskitGlobals.instance().getEventGraphViewFrame().enableEventGraphMenuItems();
                }
            } catch (Throwable t)
            {
                // Uncomment to reveal common reason for Exceptions
                t.printStackTrace(System.err);
                LOG.error(t);
            }
        } // directory
    } // end _addContentRoot

    Map<String, DefaultMutableTreeNode> directoryRootsHashMap;
    Map<String, DefaultMutableTreeNode>       packagesHashMap = new HashMap<>();

    DefaultMutableTreeNode getParent(String parentPackage, DefaultMutableTreeNode lroot) 
    {
        DefaultMutableTreeNode parentDefaultMutableTreeNode = packagesHashMap.get(parentPackage);

        if (parentDefaultMutableTreeNode == null) 
        {
            if (!parentPackage.contains(".")) {
                // we're as far up as we can be
                parentDefaultMutableTreeNode = new DefaultMutableTreeNode(parentPackage);
                defaultTreeModel.insertNodeInto(parentDefaultMutableTreeNode, lroot, 0);
            } 
            else 
            {
                // go further
                String parentPackageName = parentPackage.substring(0, parentPackage.lastIndexOf('.'));
                DefaultMutableTreeNode grandParentDefaultMutableTreeNode = getParent(parentPackageName, lroot);
                parentDefaultMutableTreeNode = new DefaultMutableTreeNode(parentPackage.substring(parentPackage.lastIndexOf('.') + 1));
                defaultTreeModel.insertNodeInto(parentDefaultMutableTreeNode, grandParentDefaultMutableTreeNode, 0);
            }
            packagesHashMap.put(parentPackage, parentDefaultMutableTreeNode);
        }
        return parentDefaultMutableTreeNode;
    }

    /**
     * Adds SimEntity icons to the Assembly Editor drag and drop tree
     *
     * @param jarFilePath the jar to evaluate for SimEntitiy based Event Graphs
     */
    private void addJarFile(String jarFilePath) 
    {
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarFilePath);
        } 
        catch (IOException e) {
            LOG.error(e);
            return;
        }
        jarFileCommon(jarFile);
    }

    @SuppressWarnings("unchecked")
    private void jarFileCommon(JarFile jarFile) 
    {
        // Prevent a case where we have simkit.jar in both the working classpath
        // and in a project's /lib directory.  We don't need to expose multiple
        // libs of the same name because they happen to be in two different
        // places
        Enumeration<TreeNode> rootNodeChildrenTreeNodeEnumeration = rootNode.children();
        String jarName = jarFile.getName().substring(jarFile.getName().lastIndexOf(File.separator) + 1);
        DefaultMutableTreeNode nextTreeNode;
        while (rootNodeChildrenTreeNodeEnumeration.hasMoreElements()) 
        {
            nextTreeNode = (DefaultMutableTreeNode) rootNodeChildrenTreeNodeEnumeration.nextElement();
            if (nextTreeNode.getUserObject().toString().contains(jarName)) {
                return; // already present
            }
        }
        List<Class<?>> classList = FindClassesForInterface.findClasses(jarFile, targetClass);
        classList.forEach(nextClass -> {
            ViskitStatics.resolveParameters(nextClass);
        });

        // Shorten long path names
        if (jarFile.getName().contains(userDir)) {
            name = jarFile.getName().substring(userDir.length() + 1, jarFile.getName().length());
        } else if (jarFile.getName().contains(userHome)) {
            name = jarFile.getName().substring(userHome.length() + 1, jarFile.getName().length());
        } else if (jarFile.getName().contains(projectPath)) {
            name = jarFile.getName().substring(projectPath.length() + 1, jarFile.getName().length());
        } else {
            name = jarFile.getName();
        }

        if (classList.isEmpty()) {
            LOG.warn("No classes of type {} found in {}", targetClassName, name);
            LOG.info("{} will not be listed in the Assembly Editor's SimEntity tree\n", name);
        } else {

            DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(name);
            defaultTreeModel.insertNodeInto(localRoot, rootNode, 0);

            classList.forEach(c -> {
                hookToParent(c, localRoot);
            });
        }
    }

    private void hookToParent(Class<?> c, DefaultMutableTreeNode myroot) {
        String pkg = c.getPackage().getName();
        DefaultMutableTreeNode dmtn = getParent(pkg, myroot);
        dmtn.add(new DefaultMutableTreeNode(c));
    }

    class MyRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object uo = ((DefaultMutableTreeNode) value).getUserObject();
            setLeafIcon(LegoTree.this.myLeafIcon); // default

            if (uo instanceof Class<?>) {
                Class<?> c = (Class<?>) uo;
                String nm = c.getName();

                setToolTipText(nm);
                nm = nm.substring(nm.lastIndexOf('.') + 1);
                //      if(sel)
                //        nm = "<html><b>"+nm+"</b></html>";   // sizes inst screwed up
                value = nm;
            } else if (uo instanceof FileBasedAssemblyNode) {
                FileBasedAssemblyNode xn = (FileBasedAssemblyNode) uo;
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
                    setLeafIcon(LegoTree.this.standardNonLeafIcon);
                }

                if (uo != null)
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
                if (!dirsToo)
                    return false;

                // TBD add an ignore in SettingsDialog, and in history file
                if (f.getName().contains("Assembl") || f.getName().contains("Scenario") || f.getName().contains("Locations"))
                    return false;

                File[] fa = f.listFiles(new MyClassTypeFilter(true));
                return (fa != null || fa != null && fa.length != 0);
            }

            return f.isFile() &&
                  (f.getName().toLowerCase().endsWith(".class") || 
                   f.getName().toLowerCase().endsWith(".xml"));
        }
    }

    //** DragGestureListener **
    @Override
    public void dragGestureRecognized(DragGestureEvent e) {
        if (dragStartListener == null) {
            return;
        }

        Object o = getUO();
        if (o == null) {
            return;
        }

        Transferable xfer;
        StringSelection ss;

        if (o instanceof FileBasedAssemblyNode) {
            FileBasedAssemblyNode xn = (FileBasedAssemblyNode) o;
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

        dragStartListener.startingDrag(ss);
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
        if (o instanceof FileBasedAssemblyNode) {
            return ((FileBasedAssemblyNode) o).loadedClass;
        }
        return null;
    }

    /**
     * Clear the queue of all SimEntities and Property Change Listeners
     */
    public void clear() {
        rootNode.removeAllChildren();
        if (directoryRootsHashMap != null) {
            directoryRootsHashMap.clear();
        }
    }

} // end class file LegoTree.java
