package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

public class ObjListPanel extends JPanel implements ActionListener
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
       field[i] = new JTextField(inst.toString());
       Vstatics.clampHeight(field[i]);

       Class c = Vstatics.ClassForName(inst.getType());
       if(c == null)
         System.err.println("what to do here... "+inst.getType());

       if (c != null) {
         if (!c.isPrimitive() || c.isArray()) {
           JPanel tinyP = new JPanel();
           tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
           tinyP.add(field[i]);
           JButton b = new JButton("...");
           b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
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
      ArrayInspector ai = new ArrayInspector(null, this);
      ai.setType(vinst.getType());
      ai.setData(((VInstantiator.Array) vinst).getInstantiators());

      ai.setVisible(true); // blocks
      if (ai.modified)
        shadow[idx] = ai.getData();
    }
    else {
      ObjectInspector oi = new ObjectInspector(null, this);
      oi.setType(vinst.getType());
      //VConstructor vcon = new VConstructor();
      //con.setType(vinst.getType());
      //vcon.getInstantiators().add(vinst);
      //oi.setData(vcon);
      oi.setData(vinst);
      oi.setVisible(true); // blocks
      if (oi.modified) {
        //shadow[idx] = (VInstantiator) oi.getData().getInstantiators().get(0);   // put the instantiator on the list
        shadow[idx] = oi.getData();
        field[idx].setText(oi.getData().toString());
        if (changeListener != null)
          changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
      }
    }
  }
}
