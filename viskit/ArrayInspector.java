package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 16, 2004
 * Time: 3:27:42 PM
 */

public class ArrayInspector  extends JDialog
{
  public boolean modified = false;
  private JComponent locationComp;
  private JButton canButt,okButt;
  private JPanel buttPan,contentP;
  private JTextField typeTF, sizeTF;
  private JPanel upPan;
  private enableApplyButtonListener listnr;

  public ArrayInspector(JFrame parent, JComponent comp)
  {
    super(parent,"Array Inspector",true);
    locationComp = comp;
    contentP = new JPanel();
    contentP.setLayout(new BoxLayout(contentP,BoxLayout.Y_AXIS));
    setContentPane(contentP);

    upPan = new JPanel(new SpringLayout());
    JLabel typeLab = new JLabel("Array type",JLabel.TRAILING);
    typeTF = new JTextField();
    Vstatics.clampHeight(typeTF);
    typeLab.setLabelFor(typeTF);
    JLabel countLab = new JLabel("Array size (1-d)",JLabel.TRAILING);
    sizeTF = new JTextField();
    Vstatics.clampHeight(sizeTF);
    countLab.setLabelFor(sizeTF);

    upPan.add(typeLab);
    upPan.add(typeTF);
    upPan.add(countLab);
    upPan.add(sizeTF);

    SpringUtilities.makeCompactGrid(upPan,2,2,5,5,5,5);

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    buttPan.add(okButt);

    // attach listeners
    listnr = new enableApplyButtonListener();
    typeTF.addCaretListener(listnr);
    sizeTF.addCaretListener(listnr); // need to do something else here // todo
    sizeTF.addActionListener(new sizeListener());
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());
    okButt.setEnabled(false);
  }
  ObjListPanel olp;
  public void setData(List lis) // of instantiators
  {
    olp = new ObjListPanel(listnr);
    olp.setData(lis,false); // don't show the type

    contentP.removeAll();
    contentP.add(upPan);
    contentP.add(Box.createVerticalStrut(5));
    contentP.add(olp);
    contentP.add(Box.createVerticalStrut(5));
    contentP.add(buttPan);

    pack();
    this.setLocationRelativeTo(locationComp);
  }
  String myArrTyp;
  String myTyp;
  public void setType(String typ)
  {
    myArrTyp = typ;
    Class c = Vstatics.ClassForName(typ);
    myTyp = Vstatics.convertClassName(c.getComponentType().getName());
    typeTF.setText(typ);
  }

  public VInstantiator.Array getData()
  {
    return new VInstantiator.Array(myArrTyp,olp.getData());
  }
  class sizeListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      String s = sizeTF.getText().trim();
      int sz;
      try {
        sz = Integer.parseInt(s);
      }
      catch (NumberFormatException e1) {
        return;
      }
      if(sz <= 0)
        return;

      Vector v = new Vector(sz);
      for(int i=0;i<sz;i++)
        v.add(new VInstantiator.FreeF(myTyp,""));
      setData(v);
    }
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
      //getRootPane().setDefaultButton(okButt);
    }

    public void actionPerformed(ActionEvent event)
    {
      caretUpdate(null);
    }
  }


}
