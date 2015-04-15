package viskit.view;

import edu.nps.util.SpringUtilities;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import viskit.VGlobals;
import viskit.model.VInstantiator;
import viskit.view.dialog.ArrayInspector;
import viskit.view.dialog.ObjectInspector;
import viskit.VStatics;
import viskit.control.AssemblyController;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 16, 2004
 * @since 3:03:09 PM
 * @version $Id$
 */
public class ObjListPanel extends JPanel implements ActionListener, CaretListener {

    private JDialog parent;
    private JLabel typeLab[];
    private JTextField entryTF[];
    private VInstantiator shadow[];
    private ActionListener changeListener;

    public ObjListPanel(ActionListener changeListener) {
        setLayout(new SpringLayout());
        this.changeListener = changeListener;
    }

    public void setDialogInfo(JDialog parent) {
        this.parent = parent;
    }

    public void setData(List<Object> lis, boolean showLabels) // of Vinstantiators
    {
        int sz = lis.size();
        typeLab = new JLabel[sz];
        JLabel[] nameLab = (sz <= 0 ? null : new JLabel[sz]);
        entryTF = new JTextField[sz];
        shadow = new VInstantiator[sz];
        JComponent[] contentObj = new JComponent[sz];

        if (viskit.VStatics.debug) {
            System.out.println("really has " + sz + "parameters");
        }
        int i = 0;
        String jTFText;
        for (Iterator<Object> itr = lis.iterator(); itr.hasNext(); i++) {
            VInstantiator inst = (VInstantiator) itr.next();
            shadow[i] = inst;
            typeLab[i] = new JLabel(/*"<html>(<i>"+*/inst.getType()/*+")"*/, JLabel.TRAILING);     // html screws up table sizing below
            String s = inst.getName();
            nameLab[i] = new JLabel(s);
            nameLab[i].setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(0, 2, 0, 2))); // some space at sides
            nameLab[i].setOpaque(true);
            nameLab[i].setBackground(new Color(255, 255, 255, 64));
            if (viskit.VStatics.debug) {
                System.out.println("really set label " + s);
            }

            s = inst.getDescription();
            if (s != null && !s.isEmpty()) {
                nameLab[i].setToolTipText(s);
            }

            entryTF[i] = new JTextField(8);
            entryTF[i].setToolTipText("Manually enter/override method "
                    + "arguments here");
            VStatics.clampHeight(entryTF[i]);

            // A little more Object... (vararg) support
            if (inst instanceof VInstantiator.Array) {
                VInstantiator.Array via = (VInstantiator.Array) inst;
                if (!via.getInstantiators().isEmpty() && via.getInstantiators().get(0) instanceof VInstantiator.FreeF) {
                    VInstantiator.FreeF vif = (VInstantiator.FreeF) via.getInstantiators().get(0);
                    jTFText = vif.getValue();
                } else
                    jTFText = inst.toString();
            } else
                jTFText = inst.toString();

            entryTF[i].setText(jTFText);
            entryTF[i].addCaretListener(this);

            Class<?> c = VStatics.getClassForInstantiatorType(inst.getType());

            if (c == null) {
                System.err.println("what to do here for " + inst.getType());
                return;
            }

            if (c != null) {
                if (!c.isPrimitive() || c.isArray()) {
                    JPanel tinyP = new JPanel();
                    tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
                    tinyP.add(entryTF[i]);
                    JButton b = new JButton("...");
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(),
                            BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    VStatics.clampSize(b, entryTF[i], b);

                    tinyP.add(b);
                    if (showLabels) {
                        typeLab[i].setLabelFor(tinyP);
                    }
                    contentObj[i] = tinyP;
                    b.setToolTipText("Edit with Instantiation Wizard");
                    b.addActionListener(this);
                    b.setActionCommand("" + i);
                } else {
                    if (showLabels) {
                        typeLab[i].setLabelFor(entryTF[i]);
                    }
                    contentObj[i] = entryTF[i];
                }
            }
        }
        if (showLabels) {
            for (int x = 0; x < typeLab.length; x++) {

//                if (nameLab != null) {
//                    if (nameLab[x].getText().length() <= 0) {
//                        nameLab[x].setText(":");
//                        nameLab[x].setBorder(new LineBorder(Color.cyan));
//                    }
//                    add(nameLab[x]);
//                }
                add(typeLab[x]);
                add(contentObj[x]);
            }

//            if (nameLab != null) {
//                SpringUtilities.makeCompactGrid(this, typeLab.length, 3, 5, 5, 5, 5);
//            } else {
                SpringUtilities.makeCompactGrid(this, typeLab.length, 2, 5, 5, 5, 5);
//            }
        } else {
            for (int x = 0; x < typeLab.length; x++) {
                add(contentObj[x]);
            }
            SpringUtilities.makeCompactGrid(this, entryTF.length, 1, 5, 5, 5, 5);
        }
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        if (changeListener != null) {
            changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
        }
    }

    /** The base of embedded parameters to finalize EG constructor instantiation.
     * Provides support for Object... (varargs)
     *
     * @return a list of free form instantiators
     */
    public List<Object> getData() {
        Vector<Object> v = new Vector<>();
        for (int i = 0; i < typeLab.length; i++) {
            if (shadow[i] instanceof VInstantiator.FreeF) {
                ((VInstantiator.FreeF) shadow[i]).setValue(entryTF[i].getText().trim());
            } else if (shadow[i] instanceof VInstantiator.Array) {
                VInstantiator.Array via = (VInstantiator.Array) shadow[i];
                List<Object> inst = via.getInstantiators();
                inst.add(new VInstantiator.FreeF(via.getType(), entryTF[i].getText().trim()));
            }
            v.add(shadow[i]);
        }
        setData(v, true);
        return v;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int idx = Integer.parseInt(e.getActionCommand());

        VInstantiator inst = shadow[idx];

        Class<?> c = VStatics.getClassForInstantiatorType(inst.getType());
        if (c == null) {
            System.err.println("what to do here for " + inst.getType());
            return;
        }
        if (c.isArray()) {
            ArrayInspector ai = new ArrayInspector(parent);   // "this" could be locComp
            ai.setType(inst.getType());

            // Special case for Object... (varargs)
            if (inst instanceof VInstantiator.FreeF) {
                List<Object> l = new ArrayList<>();
                l.add((VInstantiator.FreeF) inst);
                ai.setData(l);
            } else {
                ai.setData(((VInstantiator.Array) inst).getInstantiators());
            }

            ai.setVisible(true); // blocks
            if (ai.modified) {
                shadow[idx] = ai.getData();
                entryTF[idx].setText(shadow[idx].toString());
                if (changeListener != null) {
                    changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
                }
            }
        } else {
            ObjectInspector oi = new ObjectInspector(parent);
            oi.setType(inst.getType());

            try {
                oi.setData(inst);
            } catch (ClassNotFoundException e1) {
                String msg = "An object type specified in this element (probably " + inst.getType() + ") was not found.\n" +
                        "Add the XML or class file defining the element to the proper list at left.";
                ((AssemblyController)VGlobals.instance().getAssemblyEditor().getController()).messageUser(
                        JOptionPane.ERROR_MESSAGE,
                        e1.getMessage(),
                        msg);
                return;
            }
            oi.setVisible(true); // blocks
            if (oi.modified) {

                VInstantiator vi = oi.getData();
                if (vi == null) {return;}

                shadow[idx] = vi;
                entryTF[idx].setText(vi.toString());
                if (changeListener != null) {
                    changeListener.actionPerformed(new ActionEvent(this, 0, "Obj changed"));
                }
            }
        }
    }
}
