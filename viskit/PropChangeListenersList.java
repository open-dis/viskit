package viskit;

import javax.swing.*;
import java.awt.*;

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
  DefaultListModel model = new DefaultListModel();
  public PropChangeListenersList()
  {
    super(); //new Object[]{"ccccccccccc","Dddddd","e","ddddddddddddddddddddd","xx"});


    buildSampleData(model);
    //model.trimToSize();
    setModel(model);
    setVisibleRowCount(100);
    //Dimension d = this.getPreferredSize();
    //d.height = Integer.MAX_VALUE;


    //setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

  }

  private void buildSampleData(DefaultListModel model)
  {
    model.addElement("BlahPropertyListener.class");
    model.addElement("SomeOtherListenr.class");
    model.addElement("ABCDEFPropListener.class");
    model.addElement("BlahPropertyListener.class");
    model.addElement("SomeOtherListenr.class");
    model.addElement("BlahPropertyListener.class");
    model.addElement("SomeOtherListenr.class");
    model.addElement("BlahPropertyListener.class");
  }
}
