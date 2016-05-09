package viskit.view;

import edu.nps.util.LogUtilities;
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
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.*;
import org.apache.log4j.Logger;
import viskit.util.FileBasedAssemblyNode;
import viskit.control.FileBasedClassManager;
import viskit.util.FindClassesForInterface;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;

/**
 * Class to support creating a Listener Event Graph Object (LEGO) tree on the
 * Assembly Editor. Used for dragging and dropping EG and PCL nodes to the pallete
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
    static final Logger LOG = LogUtilities.getLogger(LegoTree.class);

    private DefaultMutableTreeNode rootTreeNode;

    private DefaultTreeModel treeModel;

    private Class<?> targetClass;

    private String targetClassName;

    private Color backgroundColor;

    private ImageIcon myLeafIcon;

    private Icon standardNonLeafIcon;

    private Image myLeafIconImage;

    private DragStartListener dragStartListener;

    private String genericTableToolTip = "Drag onto canvas";

    String userDirectory  = System.getProperty("user.dir");

    String userHome       = System.getProperty("user.home");

    String projectPath;

    String name;

    /**
     * Constructor for Listener Event Graph Object Tree
     *
     * @param className a class to evaluate as a LEGO
     * @param iconPath path to a LEGO icon
     * @param dragStartListener a DragStartListener
     * @param tooltip description for this LEGO tree
     */
    LegoTree(String className, String iconPath, DragStartListener dragStartListener, String tooltip)
	{
        this(className, new ImageIcon(ViskitGlobals.instance().getWorkClassLoader().getResource(iconPath)), dragStartListener, tooltip);
    }

    /**
     * Constructor for Listener Event Graph Object Tree
     *
     * @param className a class to evaluate as a LEGO
     * @param icon a LEGO icon
     * @param dragStartListener a DragStartListener
     * @param tooltip description for this LEGO tree
     */
    LegoTree(String className, ImageIcon icon, DragStartListener dragStartListener, String tooltip)
	{
        super();
        myLeafIcon             = icon;
        backgroundColor        = new Color(0xFB, 0xFB, 0xE5);

        this.dragStartListener = dragStartListener;
        targetClassName        = className;
        genericTableToolTip    = tooltip;

        targetClass = ViskitStatics.classForName(targetClassName);
		
		initialize ();
    }
	
	private void initialize ()
	{
        rootTreeNode       = new DefaultMutableTreeNode("root");
        treeModel          = new DefaultTreeModel(rootTreeNode);
        setModel(treeModel);
        directoryRoots     = new HashMap<>();
		
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); // selection can only contain one path at a time

        MyTreeCellRenderer myCellRenderer = new MyTreeCellRenderer();
        setCellRenderer(myCellRenderer);

        setToolTipText("");   // needs to be done first to enable tt below
        setRootVisible(true); // we want this to be false, but there is some sort of JTree bug...see paintComponent override below
        setShowsRootHandles(true);
        setVisibleRowCount(100);    // means always fill a normal size panel
        myCellRenderer.setBackgroundNonSelectionColor(backgroundColor);

        myLeafIconImage = myLeafIcon.getImage();
        standardNonLeafIcon = myCellRenderer.getOpenIcon();

        myCellRenderer.setLeafIcon(myLeafIcon);
        DragSource dragSource = DragSource.getDefaultDragSource();

        LegoTree instance = this;

        dragSource.createDefaultDragGestureRecognizer(instance, // component where drag originates
                DnDConstants.ACTION_COPY_OR_MOVE, instance);

        projectPath = ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getPath();
	}

    // beginning of hack to hide the tree rootNode
    @Override
    protected void paintComponent(Graphics g)
	{
        super.paintComponent(g);
        if (bugHack) {
            doBugHack();
        }
    }

    private boolean bugHack = true;

    private void doBugHack()
	{
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
    public String getToolTipText(MouseEvent event)
	{
        String s = super.getToolTipText(event);
        return s == null ? genericTableToolTip : s;
    }

    /**
     * @return a class of type simkit.BasicSimEntity
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    public void removeSelected()
	{
        TreePath[] selections;
        while ((selections = getSelectionPaths()) != null)
		{
            TreePath currentSelection = selections[0];
            if (currentSelection != null)
			{
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
                MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
                if (parent != null)
				{
                    treeModel.removeNodeFromParent(currentNode);
                }
            }
        }
    }

    /**
     * Used to help prevent duplicate EG or PCL nodes from appearing in the LEGO
     * tree on the Assembly Editor in addition to simply supporting the user by
     * removing a node
     *
     * @param f the file to remove from the LEGO tree
     */
    public void removeContentRoot(File f)
	{
        //System.out.println("LegoTree.removeContentRoot: "+f.getAbsolutePath());
        _removeNode(rootTreeNode, f);
    }

    private DefaultMutableTreeNode _removeNode(DefaultMutableTreeNode dmtn, File f)
	{
        for (int i = 0; i < dmtn.getChildCount(); i++)
		{
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) dmtn.getChildAt(i);
            if (n != null)
			{
                Object uo = n.getUserObject();
                if (!(uo instanceof FileBasedAssemblyNode)) 
				{
                    // Keep looking for a FBAN in the root branches
                    _removeNode(n, f);
                } 
				else 
				{
                    FileBasedAssemblyNode fban = (FileBasedAssemblyNode) uo;

                    try {
                        if (fban.isXML && fban.xmlSource.getCanonicalPath().equals(f.getCanonicalPath()))
						{
                            treeModel.removeNodeFromParent(n);
                            FileBasedClassManager.instance().unloadFile(fban);
                            return n;
                        }
                    } 
					catch (IOException e)
					{
                        LOG.error(e);
                    }
                }
            }
        }
        return null;
    }

    // 4 May 06 JMB The filter down below checks for empty dirs.
    /**
     * Adds SimEntity icons to the Assembly Editor drag and drop tree. If there
     * is a directory, or a jarfile with xml in it, it will show in the LEGO
     * tree, but if its children have errors when marshaling they will not
     * appear.
     *
     * @param f the directory to recurse to find SimEntity-based Event Graphs
     * @param recurse if true, recurse the directory
     */
    public void addContentRoot(File f, boolean recurse)
	{
        if (!f.getPath().contains(".svn") && !f.getPath().contains(".cvs") && !f.getPath().contains(".git")) // exclude directories
		{
            if (f.getName().toLowerCase().endsWith(".jar"))
			{
                addJarFile(f.getPath());
            } 
			else if (!f.getName().endsWith(".java"))
			{
                _addContentRoot(f, recurse);
            }
        }
    }

    // Does the real work
    private void _addContentRoot(File f, boolean recurse)
	{
        DefaultMutableTreeNode myMutableTreeNode;

        removeContentRoot(f); // Prevent duplicates of the event graph icons

        if (f.isDirectory())
		{
            if (!recurse)
			{
                myMutableTreeNode = new DefaultMutableTreeNode(f.getPath());
                rootTreeNode.add(myMutableTreeNode);
                directoryRoots.put(f.getPath(), myMutableTreeNode);
                int index = rootTreeNode.getIndex(myMutableTreeNode);
                treeModel.nodesWereInserted(rootTreeNode, new int[]{index});
                File[] containedFileList = f.listFiles(new MyClassTypeFilter(false));
                for (File file : containedFileList)
				{
					// TODO why are we recursing here if recurse=false ?
                    _addContentRoot(file, file.isDirectory()); // recurse on subdirectories
                }
            } 
			else // recurse = true, directory
			{
                // Am I here?  If so, grab my treenode
                // Else is my parent here?  If so, hook me as child
                // If not, put me in under the rootNode
                myMutableTreeNode = directoryRoots.get(f.getPath());
                if (myMutableTreeNode == null)
				{
                    myMutableTreeNode = directoryRoots.get(f.getParent());
                    if (myMutableTreeNode != null)
					{
                        DefaultMutableTreeNode parent = myMutableTreeNode;
                        myMutableTreeNode = new DefaultMutableTreeNode(f.getName());
                        parent.add(myMutableTreeNode);
                        directoryRoots.put(f.getPath(), myMutableTreeNode);
                        int index = parent.getIndex(myMutableTreeNode);
                        treeModel.nodesWereInserted(parent, new int[]{index});
                    } 
					else
					{
                        // Shorten long path names
                        if (f.getPath().contains(userDirectory)) 
						{
                            name = f.getPath().substring(userDirectory.length() + 1, f.getPath().length());
                        } 
						else if (f.getPath().contains(userHome)) 
						{
                            name = f.getPath().substring(userHome.length() + 1, f.getPath().length());
                        } 
						else if (f.getPath().contains(projectPath)) 
						{
                            name = f.getPath().substring(projectPath.length() + 1, f.getPath().length());
                        } 
						else 
						{
                            name = f.getPath();
                        }

                        myMutableTreeNode = new DefaultMutableTreeNode(name);
                        rootTreeNode.add(myMutableTreeNode);
                        directoryRoots.put(f.getPath(), myMutableTreeNode);
                        int index = rootTreeNode.getIndex(myMutableTreeNode);
                        treeModel.nodesWereInserted(rootTreeNode, new int[]{index});
                    }
                }
                File[] containedFileList = f.listFiles(new MyClassTypeFilter(true));
                for (File file : containedFileList) 
				{
                    _addContentRoot(file, file.isDirectory()); // recurse on subdirectories
                }
            }   // recurse = true
        } // is directory
		
        else // We're NOT a directory...
		{
            FileBasedAssemblyNode fban;
            try {
                // This call generates the source, compiles and validates EG XML files
                // Also checks for extensions of SimEntityBase in .class files
                fban = FileBasedClassManager.instance().loadFile(f, getTargetClass());

                if (fban != null) 
				{
                    myMutableTreeNode = new DefaultMutableTreeNode(fban);
                    int index;
                    DefaultMutableTreeNode parentNode = directoryRoots.get(f.getParent());
                    if (parentNode != null) 
					{
                        parentNode.add(myMutableTreeNode);
                        index = parentNode.getIndex(myMutableTreeNode);
                        treeModel.nodesWereInserted(parentNode, new int[] {index});
                    } 
					else 
					{
                        rootTreeNode.add(myMutableTreeNode);
                        index = rootTreeNode.getIndex(myMutableTreeNode);
                        treeModel.nodesWereInserted(rootTreeNode, new int[] {index});
                    }
                } 
				else 
				{

					LOG.info(f.getName() + " not be listed in the LEGOs tree because compiled class not found\n");
					
					ViskitGlobals.instance().getAssemblyController().messageToUser(
							JOptionPane.ERROR_MESSAGE,
							"Compilation Error", "<html>" +
							"<p align='center'>Error compiling <i>" + f.getName() + "</i>" + ViskitStatics.RECENTER_SPACING + "</p>" +
						    "<p>&nbsp;</p>" +
							"<p align='center'><i>" + f.getName() + ".class</i> file cannot be added to Event Graph Selection tree for Assembly use" + ViskitStatics.RECENTER_SPACING + "</p>");
                }

                // Note:
                // On initial startup with valid XML, but bad compilation,
                // dirty won't get set b/c the graph model is null until the
                // model tab is created and the EG file is opened.  First pass
                // is only for inclusion in the LEGOs tree
                if (ViskitGlobals.instance().getActiveEventGraphModel() != null) {
                    ViskitGlobals.instance().getActiveEventGraphModel().setDirty(fban == null);
                    ViskitGlobals.instance().getEventGraphViewFrame().toggleEventGraphStatusIndicators();
                }
						
//						// TODO move this code to event dispatch thread
//						// http://stackoverflow.com/questions/11256159/expanding-specific-jtree-path
//						if (true) // f.getPath().contains("EventGraphs")) // TODO string constant
//						{
//							this.expandPath(new TreePath(myMutableTreeNode)); // TODO not working
//						}
            } 
			catch (Throwable t)
			{
                // Uncomment to reveal common reason for Exceptions
//                t.printStackTrace();
                LOG.error(t);
            }
        } // directory
    }

    Map<String, DefaultMutableTreeNode> directoryRoots;

    Map<String, DefaultMutableTreeNode> packagesHM = new HashMap<>();

    DefaultMutableTreeNode getParent(String packageName, DefaultMutableTreeNode lroot) {
        DefaultMutableTreeNode parent = packagesHM.get(packageName);

        if (parent == null) {
            if (!packageName.contains(".")) {
                // we're as far up as we can be
                parent = new DefaultMutableTreeNode(packageName);
                treeModel.insertNodeInto(parent, lroot, 0);
            } else {
                // go further
                String ppkg = packageName.substring(0, packageName.lastIndexOf('.'));
                DefaultMutableTreeNode granddaddy = getParent(ppkg, lroot);
                parent = new DefaultMutableTreeNode(packageName.substring(packageName.lastIndexOf('.') + 1));
                treeModel.insertNodeInto(parent, granddaddy, 0);
            }
            packagesHM.put(packageName, parent);
        }

        return parent;
    }

    /**
     * Adds SimEntity icons to the Assembly Editor drag and drop tree
     *
     * @param f the jar to evaluate for SimEntitiy based EGs
     */
    private void addJarFile(String jarFilePath) {
        JarFile jarFile;
        try {
            jarFile = new JarFile(jarFilePath);
        } 
		catch (IOException e)
		{
            ViskitGlobals.instance().getAssemblyController().messageToUser(
                    JOptionPane.ERROR_MESSAGE,
                    "Input/Output (I/O) Error", 
					"Error reading <i>" + jarFilePath + "</i>" + ViskitStatics.RECENTER_SPACING);
            return;
        }
        jarFileCommon(jarFile);
    }

    @SuppressWarnings("unchecked")
    private void jarFileCommon(JarFile jarFile) {

        // Prevent a case where we have simkit.jar in both the working classpath
        // and in a project's /lib directory.  We don't need to expose multiple
        // libs of the same name because they happen to be in two different
        // places
        Enumeration<DefaultMutableTreeNode> e = rootTreeNode.children();
        String jarName = jarFile.getName().substring(jarFile.getName().lastIndexOf(File.separator) + 1);
        DefaultMutableTreeNode tn;
        while (e.hasMoreElements()) {
            tn = e.nextElement();
            if (tn.getUserObject().toString().contains(jarName)) {
                return;
            }
        }

        List<Class<?>> interfacesClassList = FindClassesForInterface.findClasses(jarFile, targetClass);
        for (Class<?> interfaceClass : interfacesClassList) {
            ViskitStatics.resolveParameters(interfaceClass);
        }

        // Shorten long path names
        if (jarFile.getName().contains(userDirectory)) {
            name = jarFile.getName().substring(userDirectory.length() + 1, jarFile.getName().length());
        } else if (jarFile.getName().contains(userHome)) {
            name = jarFile.getName().substring(userHome.length() + 1, jarFile.getName().length());
        } else if (jarFile.getName().contains(projectPath)) {
            name = jarFile.getName().substring(projectPath.length() + 1, jarFile.getName().length());
        } else {
            name = jarFile.getName();
        }

        if (interfacesClassList == null || interfacesClassList.isEmpty()) {
            LOG.warn("No classes of type " + targetClassName + " found in " + name);
            LOG.info(name + " will not be listed in the Assembly Editor's Event Graphs SimEntity node tree\n");
        } else {

            DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(name);
            treeModel.insertNodeInto(localRoot, rootTreeNode, 0);

            for (Class<?> c : interfacesClassList) {
                hookToParent(c, localRoot);
            }
        }
    }

    private void hookToParent(Class<?> c, DefaultMutableTreeNode myroot)
	{
        String pkg = c.getPackage().getName();
        DefaultMutableTreeNode dmtn = getParent(pkg, myroot);
        dmtn.add(new DefaultMutableTreeNode(c));
    }

    class MyTreeCellRenderer extends DefaultTreeCellRenderer
	{
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean hasFocus) 
		{
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            setLeafIcon(LegoTree.this.myLeafIcon); // default

            if (userObject instanceof Class<?>) // precompiled .class file
			{
                Class<?> classType = (Class<?>) userObject;
                String   className = classType.getName();

                className = className.substring(className.lastIndexOf('.') + 1); // short form
                setToolTipText(className + " (separately compiled)");
//              className += " (externally compiled)";
                value = className;
            }
			else if (userObject instanceof FileBasedAssemblyNode)
			{
                FileBasedAssemblyNode xmlFileBasedAssemblyNode = (FileBasedAssemblyNode) userObject;
                String className = xmlFileBasedAssemblyNode.loadedClass;
                className = className.substring(className.lastIndexOf('.') + 1); // short form
                if (xmlFileBasedAssemblyNode.isXML)
				{
                    setToolTipText(className + " (loaded from Viskit XML)");
                    className += " (XML)";
                } 
				else
				{
                    setToolTipText(className + " (loaded from Viskit-compiled .class)");
                    className += " (compiled)";
                }
                value = className;
            } 
			else 
			{
                if (isLeaf) // don't show a leaf icon for a directory in the file system which doesn't happen to have contents
                {
                    setLeafIcon(LegoTree.this.standardNonLeafIcon);
                }
				String toolTipText = userObject.toString();
				if (toolTipText.equals("EventGraphs")) // TODO String constant
				{
					toolTipText += " project subdirectory containing your models";
					selected = true; 
					expanded = true; // TODO must be moved, put code on event dispatch thread?
				}
				else 
				{
					selected = false;
				}
                setToolTipText(toolTipText);
                value = value.toString();
            }
            return super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, hasFocus);
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

            return f.isFile()
                    && (f.getName().endsWith(".class") || (f.getName().endsWith(".xml")));
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
        rootTreeNode.removeAllChildren();
        if (directoryRoots != null) {
            directoryRoots.clear();
        }
    }

} // end class file LegoTree.java
