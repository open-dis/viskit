package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
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

public class ObjListPanel extends JPanel implements ActionListener, CaretListener {
    private JLabel typeLab[];
    private JTextField entryTF[];
    private VInstantiator shadow[];
    private ActionListener changeListener;
    
    public ObjListPanel(ActionListener changeListener) {
        setLayout(new SpringLayout());
        this.changeListener = changeListener;
    }
    public void setDialogInfo(JDialog parent, Component locComp) {
        this.parent = parent;
    }
    private JDialog parent;
    
    public void setData(List lis, boolean showLabels)  // of Vinstantiators
    {
        int sz = lis.size();
        typeLab = new JLabel[sz];
        JLabel[] nameLab = (sz<=0?null:new JLabel[sz]);
        entryTF = new JTextField[sz];
        shadow = new VInstantiator[sz];
        JComponent[] contentObj = new JComponent[sz];
        
        if (viskit.Vstatics.debug) System.out.println("really has "+sz+"parameters");
        int i = 0;
        for (Iterator itr = lis.iterator(); itr.hasNext();i++) {
            VInstantiator inst = (VInstantiator) itr.next();
            shadow[i] = inst.vcopy();
            //shadow[i] = inst; //?
            typeLab[i] = new JLabel(/*"<html>(<i>"+*/inst.getType()/*+")"*/, JLabel.TRAILING);     // html screws up table sizing below
            String s = inst.getName();
            ///if(s != null && s.length()>0 && nameLab != null) {
            nameLab[i] = new JLabel(s);
            nameLab[i].setBorder(new CompoundBorder(new LineBorder(Color.black),new EmptyBorder(0,2,0,2))); // some space at sides
            nameLab[i].setOpaque(true);
            nameLab[i].setBackground(new Color(255,255,255,64));
            if (viskit.Vstatics.debug) System.out.println("really set label "+s);
            //}
            //else
            //nameLab = null; // if one is bad, disable all

            s = inst.getDescription();
            if(s != null && s.length()>0)
              nameLab[i].setToolTipText(s);

            entryTF[i] = new JTextField(8);
            Vstatics.clampHeight(entryTF[i]);
            entryTF[i].setText(inst.toString());
            entryTF[i].addCaretListener(this);
            
            Class c = Vstatics.classForName(inst.getType());
            if(c == null)
                System.err.println("what to do here... "+inst.getType());
            
            if (c != null) {
                if (!c.isPrimitive() || c.isArray()) {
                    JPanel tinyP = new JPanel();
                    tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
                    tinyP.add(entryTF[i]);
                    JButton b = new JButton("...");
                    b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    Vstatics.clampSize(b, entryTF[i], b);
                    
                    tinyP.add(b);
                    if(showLabels)
                        typeLab[i].setLabelFor(tinyP);
                    //add(tinyP);
                    contentObj[i] = tinyP;
                    b.setToolTipText("Edit with Instantiation Wizard");
                    b.addActionListener(this);
                    b.setActionCommand(""+i);
                } else {
                    if(showLabels)
                        typeLab[i].setLabelFor(entryTF[i]);
                    //add(entryTF[i]);
                    contentObj[i] = entryTF[i];
                }
            }
        }
        if (showLabels) {
            for (int x = 0; x < typeLab.length; x++) {
                if (nameLab != null){
                    if(nameLab[x].getText().length() <= 0) {
                        nameLab[x].setText(":"); nameLab[x].setBorder(new LineBorder(Color.cyan));}
                    add(nameLab[x]);
                }
                add(typeLab[x]);
                add(contentObj[x]);
            }
            
            if (nameLab != null)
                SpringUtilities.makeCompactGrid(this, typeLab.length, 3, 5, 5, 5, 5);
            else
                SpringUtilities.makeCompactGrid(this, typeLab.length, 2, 5, 5, 5, 5);
        } else {
            for (int x = 0; x < typeLab.length; x++)
                add(contentObj[x]);
            SpringUtilities.makeCompactGrid(this, entryTF.length, 1, 5, 5, 5, 5);
        }
    }
    
    public void caretUpdate(CaretEvent e) {
        if (changeListener != null)
            changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
    }
    
    /* returns a list of instantiators */
    public List getData() {
        Vector v = new Vector();
        for(int i=0;i<typeLab.length;i++) {
            if(shadow[i] instanceof VInstantiator.FreeF)
                ((VInstantiator.FreeF)shadow[i]).setValue(entryTF[i].getText().trim());
            //v.add(shadow[i].vcopy());
            v.add(shadow[i]);
        }
        return v;
    }
    
    public void actionPerformed(ActionEvent e) {
        int idx = Integer.parseInt(e.getActionCommand());
        
        VInstantiator vinst = shadow[idx].vcopy();
        //VInstantiator vinst = shadow[idx];
        //List[] params = Vstatics.resolveParameters(vinst.getType()); //
        //vinst = new VInstantiator.Constr(params[0],vinst.getType()); // ??? 
        Class c = Vstatics.classForName(vinst.getType());
        if (c.isArray()) {
            ArrayInspector ai = new ArrayInspector(parent, this);   // "this" could be locComp
            ai.setType(vinst.getType());
            ai.setData(((VInstantiator.Array) vinst).getInstantiators());
            
            ai.setVisible(true); // blocks
            if (ai.modified) {
                shadow[idx] = ai.getData();
                entryTF[idx].setText(shadow[idx].toString());
                if (changeListener != null)
                    changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
            }
        } else {
            ObjectInspector oi = new ObjectInspector(parent, this);     // "this" could be locComp
            oi.setType(vinst.getType());
            // use default constructor if exists
            Class clazz = Vstatics.classForName(vinst.getType());
            if(clazz != null) {
                List args;
                java.lang.reflect.Constructor[] construct = clazz.getConstructors();
                if(construct != null && construct.length > 0) {
                    //args = VInstantiator.buildDummyInstantiators(construct[0]);
                    //if ( vinst instanceof VInstantiator.Constr) ((VInstantiator.Constr)vinst).setArgs(args);
                    
                }
            }
            
            try {
                oi.setData(vinst);
            } catch (ClassNotFoundException e1) {
                String msg = "An object type specified in this element (probably "+vinst.getType()+") was not found.\n" +
                        "Add the XML or class file defining the element to the proper list at left.";
                JOptionPane.showMessageDialog(parent,msg,"Class Definition Not Found",JOptionPane.ERROR_MESSAGE);
                return;
            }
            oi.setVisible(true); // blocks
            if (oi.modified) {
                shadow[idx] = oi.getData();
                entryTF[idx].setText(oi.getData().toString());
                if (changeListener != null)
                    changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
            }
        }
        
    }
}
