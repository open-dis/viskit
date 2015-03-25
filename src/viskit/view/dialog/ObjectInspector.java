package viskit.view.dialog;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import viskit.view.InstantiationPanel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 16, 2004
 * Time: 3:27:42 PM
 */
public class ObjectInspector extends JDialog
{
  public boolean modified = false;
  private JButton canButt,okButt;
  private JPanel buttPan,contentP;
  InstantiationPanel ip;
  enableApplyButtonListener lis;

  public ObjectInspector(JDialog parent)
  {
    super(parent,"Object Inspector",true);
    contentP = new JPanel();
    contentP.setLayout(new BoxLayout(contentP,BoxLayout.Y_AXIS));
    contentP.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    setContentPane(contentP);

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    okButt.setEnabled(false);
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    buttPan.add(okButt);

    // attach listeners
    lis = new enableApplyButtonListener();
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());
  }

  public void setType(String typ)
  {
    contentP.removeAll();

    ip = new InstantiationPanel(this,lis,false,true);  // allow type editing
    ip.setBorder(null);

    contentP.add(ip);
    //contentP.add(Box.createVerticalGlue());
    contentP.add(Box.createVerticalStrut(5));
    contentP.add(buttPan);

    pack();     // do this prior to next
    setLocationRelativeTo(getParent());
  }

  public void setData(VInstantiator vi) throws ClassNotFoundException
  {
    ip.setData(vi);
    pack();
  }

  public VInstantiator getData()
  {
    return ip.getData();
  }

  class cancelButtonListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent event)
    {
      modified = false;    // for the caller
      dispose();
    }
  }

  class applyButtonListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent event)
    {
      dispose();
    }
  }

  class enableApplyButtonListener implements CaretListener, ActionListener
  {
    @Override
    public void caretUpdate(CaretEvent event)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      ObjectInspector.this.pack();             // fix for buttons disappearing on bottom
      caretUpdate(null);
    }
  }

}
