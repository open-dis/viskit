package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 16, 2004
 * Time: 3:03:09 PM
 */

public class ObjListPanel extends JPanel implements ActionListener, CaretListener
{
  private JLabel label[];
  private JTextField field[];
  private VInstantiator shadow[];
  private ActionListener changeListener;

  public ObjListPanel(ActionListener changeListener)
  {
    setLayout(new SpringLayout());
    this.changeListener = changeListener;
  }
  public void setDialogInfo(JDialog parent, Component locComp)
  {
    this.parent = parent;
    this.locComp = locComp;
  }
  private JDialog parent;
  private Component locComp;

  public void setData(List lis, boolean showLabels)  // of Vinstantiators
  {
    label = new JLabel[lis.size()];
    field = new JTextField[label.length];
    shadow = new VInstantiator[label.length];

     int i = 0;
     for (Iterator itr = lis.iterator(); itr.hasNext();i++) {
       VInstantiator inst = (VInstantiator) itr.next();
       shadow[i] = inst;

       label[i] = new JLabel(inst.getType(), JLabel.TRAILING);
       if(showLabels)
         add(label[i]);
       field[i] = new JTextField();
       Vstatics.clampHeight(field[i]);
       field[i].setText(inst.toString());
       field[i].addCaretListener(this);

       Class c = Vstatics.ClassForName(inst.getType());
       if(c == null)
         System.err.println("what to do here... "+inst.getType());

       if (c != null) {
         if (!c.isPrimitive() || c.isArray()) {
           JPanel tinyP = new JPanel();
           tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
           tinyP.add(field[i]);
           JButton b = new JButton("...");
           b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
           Vstatics.clampSize(b, field[i], b);

           tinyP.add(b);
           if(showLabels)
             label[i].setLabelFor(tinyP);
           add(tinyP);
           b.setToolTipText("Edit with Instantiation Wizard");
           b.addActionListener(this);
           b.setActionCommand(""+i);
         }
         else {
           if(showLabels)
             label[i].setLabelFor(field[i]);
           add(field[i]);
         }
       }
     }
     if(showLabels)
       SpringUtilities.makeCompactGrid(this, label.length, 2, 5, 5, 5, 5);
     else
       SpringUtilities.makeCompactGrid(this, field.length, 1, 5, 5, 5, 5);
   }

  public void caretUpdate(CaretEvent e)
  {
    if (changeListener != null)
      changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
  }

  /* returns a list of instantiators */
   public List getData()
   {
     Vector v = new Vector();
     for(int i=0;i<label.length;i++) {
       if(shadow[i] instanceof VInstantiator.FreeF)
         ((VInstantiator.FreeF)shadow[i]).setValue(field[i].getText().trim());
       v.add(shadow[i]);
     }
     return v;
   }

  public void actionPerformed(ActionEvent e)
  {
    int idx = Integer.parseInt(e.getActionCommand());

    VInstantiator vinst = shadow[idx];
    Class c = Vstatics.ClassForName(vinst.getType());
    if (c.isArray()) {
      ArrayInspector ai = new ArrayInspector(parent, this);   // "this" could be locComp
      ai.setType(vinst.getType());
      ai.setData(((VInstantiator.Array) vinst).getInstantiators());

      ai.setVisible(true); // blocks
      if (ai.modified) {
        shadow[idx] = ai.getData();
        field[idx].setText(shadow[idx].toString());
        if (changeListener != null)
          changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
      }
    }
    else {
      ObjectInspector oi = new ObjectInspector(parent, this);     // "this" could be locComp
      oi.setType(vinst.getType());
      oi.setData(vinst);
      oi.setVisible(true); // blocks
      if (oi.modified) {
        shadow[idx] = oi.getData();
        field[idx].setText(oi.getData().toString());
        if (changeListener != null)
          changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
      }
    }

  }
}
