package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.util.jar.JarFile;
import java.util.Iterator;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;
import java.io.IOException;
import java.net.URL;
import java.net.URI;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 10:10:45 AM
 */
public class PropChangeListenersList extends JList implements DragGestureListener, DragSourceListener
{
  DefaultListModel model;// = new DefaultListModel();
  ImageIcon icon;
  Image     myIconImage;

  DragStartListener lis;

  public PropChangeListenersList(DragStartListener lis)
  {
    super();

    this.lis = lis;
    model = buildSampleData();

    setModel(model);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setCellRenderer(new PCLRenderer());
    setVisibleRowCount(100);
    DragSource dragSource = DragSource.getDefaultDragSource();

    dragSource.createDefaultDragGestureRecognizer(this, // component where drag originates
        DnDConstants.ACTION_COPY_OR_MOVE, // actions
        this); // drag gesture recognizer

  }

  private DefaultListModel buildSampleData()
  {
    DefaultListModel mod = new DefaultListModel();
    String jarFileName = "lib/simkit.jar";
    JarFile jarFile = null;
    try {
      URL jurl = ClassLoader.getSystemResource(jarFileName);
      URI juri = new URI(jurl.toString());
      jarFile = new JarFile(juri.getPath()); //jarFileName);
    }
    catch (Exception e) {
      e.printStackTrace();
      return mod;
    }
    java.util.List list = FindClassesForInterface.findClasses(jarFile, java.beans.PropertyChangeListener.class);
    Vector v = new Vector();
    for(Iterator itr = list.iterator(); itr.hasNext();) {
      Class c = (Class)itr.next();
      v.add(c);
    }
    Class[] ca = new Class[v.size()];
    ca = (Class[])v.toArray(ca);
    Arrays.sort(ca,new MyClassSorter());
    for(int i=0;i<ca.length;i++)
      mod.addElement(ca[i]);
   //   String nm = c.getName();
   //   v.add(nm.substring(nm.lastIndexOf('.')+1));
 //mod.addElement(c);
 //   }

/*
    String[] sa = new String[v.size()];
    sa = (String[])v.toArray(sa);
    Arrays.sort(sa);
    for(int i=0;i<sa.length;i++)
      mod.addElement(sa[i]);
*/

    return mod;
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
  /**
   * This "strange" renderer idea...you build one instance, but its method gets called repeatedly
   * to return modified versions of itself.
   */

  class PCLRenderer extends JPanel implements ListCellRenderer
  {
    //ImageIcon ii;
    JLabel lab;
    JLabel txt;
    Font unsel, sel;
    private Color background = new Color(0xFB,0xFB,0xE5);

    public PCLRenderer()
    {
      //icon = new ImageIcon(ClassLoader.getSystemResource("viskit/images/propchangelistener.png"));
      icon = new PropChangListIcon(20,20);
      myIconImage = icon.getImage();
      lab = new JLabel(icon);
      lab.setBackground(background);
      lab.setOpaque(true);

      txt = new JLabel("junk");
      unsel = PropChangeListenersList.this.getFont();
      sel = unsel.deriveFont(Font.BOLD);

      txt.setFont(unsel);

      txt.setBackground(PropChangeListenersList.this.getSelectionBackground());
      txt.setOpaque(false);

      setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
      add(lab);
      add(txt);
      setOpaque(true);
      setBackground(background);
    }
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
      String nm = ((Class)value).getName();
      txt.setText(nm.substring(nm.lastIndexOf('.')+1));
      if(isSelected) {
        txt.setOpaque(true);
        txt.setFont(sel);
      }
      else {
        txt.setOpaque(false);
        txt.setFont(unsel);
      }
      return this;
    }
  }

  public String getToolTipText(MouseEvent e)
  {
    int index = locationToIndex(e.getPoint());
    if (index > -1) {
      Class c = (Class) model.getElementAt(index);
      return c.getName(); //getPackage().getName();
    }
    else
      return null;
  }

  // Drag stuff
  public void dragGestureRecognized(DragGestureEvent e)
  {
    String s = getClassName();
    if(s != null) {
      //e.startDrag(DragSource.DefaultCopyDrop,new StringSelection(s), this);
      try{
      if(lis != null)
        lis.startingDrag(new StringSelection(s));
      e.startDrag(DragSource.DefaultCopyDrop,
          myIconImage,new Point(-icon.getIconWidth()/2,
              -icon.getIconHeight()/2),new StringSelection(s), this);
      }
      catch(InvalidDnDOperationException ex) {
        ex.printStackTrace();
        System.out.println(ex);
      }
    }
  }

  public void dragDropEnd(DragSourceDropEvent e){}
  public void dragEnter(DragSourceDragEvent e){}
  public void dragExit(DragSourceEvent e){}
  public void dragOver(DragSourceDragEvent e){}
  public void dropActionChanged(DragSourceDragEvent e){}

  public String getClassName()
  {
   Object o = PropChangeListenersList.this.getSelectedValue();
/*
    TreePath path = getLeadSelectionPath();
    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) path.getLastPathComponent();
    Object o = dmtn.getUserObject();
*/
    if (o != null && o instanceof Class)
      return ((Class) o).getName();
    return
        null;
  }

}

