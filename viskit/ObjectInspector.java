package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 16, 2004
 * Time: 3:27:42 PM
 */

public class ObjectInspector  extends JDialog implements ActionListener
{
  public boolean modified = false;
  private JComponent locationComp;
  private JButton canButt,okButt;
  private JPanel buttPan,contentP;
  InstantiationPanel ip;
  enableApplyButtonListener lis;

  public ObjectInspector(JFrame parent, JComponent comp)
  {
    super(parent,"Object Inspector",true);
    locationComp = comp;
    contentP = new JPanel();
    contentP.setLayout(new BoxLayout(contentP,BoxLayout.Y_AXIS));
    contentP.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    setContentPane(contentP);

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    buttPan.add(okButt);

    // attach listeners
    lis = new enableApplyButtonListener();
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());
    okButt.setEnabled(false);
  }

  public void setType(String typ)
  {
    contentP.removeAll();

    ip = new InstantiationPanel(this,lis);
    ip.setBorder(null);

    contentP.add(ip);
    //contentP.add(Box.createVerticalGlue());
    contentP.add(Box.createVerticalStrut(5));
    contentP.add(buttPan);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);
  }

  public void actionPerformed(ActionEvent e)
  {
    // enable ok butt
  }

  public void setData(VInstantiator vi)
  {
//    myVcon = vi.vcopy();
//    ip.setData(myVcon);
    ip.setData(vi);
    pack();
  }

  public VInstantiator getData()
  {
    return ip.getData();
  }

  class cancelButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      //if(checkBlankFields())
      //  return;
      modified = false;    // for the caller
      setVisible(false);
    }
  }

  class applyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      //if(checkBlankFields())
       // return;
      //if (modified)
        //unloadWidgets();
      setVisible(false);
    }
  }

  class enableApplyButtonListener implements CaretListener, ActionListener
  {
    public void caretUpdate(CaretEvent event)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }

    public void actionPerformed(ActionEvent event)
    {
      caretUpdate(null);
    }
  }

}
