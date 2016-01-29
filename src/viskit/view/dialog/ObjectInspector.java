package viskit.view.dialog;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import viskit.model.VInstantiator;
import viskit.view.InstantiationPanel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 16, 2004
 * Time: 3:27:42 PM
 *
 * @version $Id$
 */
public class ObjectInspector extends JDialog
{
  public boolean modified = false;
  private JButton cancelButton,okButton;
  private JPanel buttonPanel,contentP;
  InstantiationPanel ip;
  enableApplyButtonListener lis;

  public ObjectInspector(JDialog parent)
  {
    super(parent,"Object Inspector",true);
    contentP = new JPanel();
    contentP.setLayout(new BoxLayout(contentP,BoxLayout.Y_AXIS));
    contentP.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    setContentPane(contentP);

    buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    cancelButton = new JButton("Cancel");
    okButton = new JButton("Apply changes");
    okButton.setEnabled(false);
    buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);

    // attach listeners
    lis = new enableApplyButtonListener();
    cancelButton.addActionListener(new cancelButtonListener());
    okButton.addActionListener(new applyButtonListener());
  }

  public void setType(String typ)
  {
    contentP.removeAll();

    ip = new InstantiationPanel(this,lis,false,true);  // allow type editing
    ip.setBorder(null);

    contentP.add(ip);
    //contentP.add(Box.createVerticalGlue());
    contentP.add(Box.createVerticalStrut(5));
    contentP.add(buttonPanel);

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
      okButton.setEnabled(true);
      getRootPane().setDefaultButton(okButton);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      ObjectInspector.this.pack();             // fix for buttons disappearing on bottom
      caretUpdate(null);
    }
  }

}
