package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.jar.JarFile;
import java.util.Iterator;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;
import java.io.IOException;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 10:10:45 AM
 */
public class PropChangeListenersList extends JList
{
  DefaultListModel model;// = new DefaultListModel();
  public PropChangeListenersList()
  {
    super();


    model = buildSampleData();

    setModel(model);
    setCellRenderer(new PCLRenderer());
    setVisibleRowCount(100);
  }

  private DefaultListModel buildSampleData()
  {
    DefaultListModel mod = new DefaultListModel();
    String jarFileName = "lib/simkit.jar";
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(jarFileName);
    }
    catch (IOException e) {
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
    ImageIcon ii;
    JLabel lab;
    JLabel txt;
    Font unsel, sel;
    private Color background = new Color(0xFB,0xFB,0xE5);

    public PCLRenderer()
    {
      ii = new ImageIcon(ClassLoader.getSystemResource("viskit/images/propchangelistener.png"));
      lab = new JLabel(ii);
      //lab.setBackground(PropChangeListenersList.this.getSelectionBackground());
      lab.setOpaque(false);

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
}
