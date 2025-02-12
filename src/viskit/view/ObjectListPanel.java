package viskit.view;

import edu.nps.util.Log4jUtilities;
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
import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.model.ViskitModelInstantiator;
import viskit.view.dialog.ArrayInspector;
import viskit.view.dialog.ObjectInspector;
import viskit.ViskitStatics;
import viskit.control.AssemblyControllerImpl;

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
public class ObjectListPanel extends JPanel implements ActionListener, CaretListener
{
    static final Logger LOG = Log4jUtilities.getLogger(ObjectListPanel.class);

    private JDialog parent;
    private JLabel typeLabelArray[];
    private JTextField entryTF[];
    private ViskitModelInstantiator shadow[];
    private final ActionListener changeListener;

    public ObjectListPanel(ActionListener changeListener) {
        setLayout(new SpringLayout());
        this.changeListener = changeListener;
    }

    public void setDialogInfo(JDialog parent) {
        this.parent = parent;
    }

    public void setData(List<Object> objectList, boolean showLabels) // of Vinstantiators
    {
        int objectListSize = objectList.size();
        typeLabelArray = new JLabel[objectListSize];
        JLabel[] nameLabel = (objectListSize <= 0 ? null : new JLabel[objectListSize]);
        entryTF = new JTextField[objectListSize];
        shadow = new ViskitModelInstantiator[objectListSize];
        JComponent[] contentObj = new JComponent[objectListSize];

        if (viskit.ViskitStatics.debug) {
            System.out.println("really has " + objectListSize + "parameters");
        }
        int i = 0;
        String jTFText = "", s;
        ViskitModelInstantiator nextInstance;
        ViskitModelInstantiator.Factory vif;
        ViskitModelInstantiator.FreeF viff;
        JPanel tinyP;
        JButton b;
        for (Iterator<Object> itr = objectList.iterator(); itr.hasNext(); i++) {
            nextInstance = (ViskitModelInstantiator) itr.next();
            shadow[i] = nextInstance;
            typeLabelArray[i] = new JLabel("(" + nextInstance.getType() + ")" + " " + nextInstance.getName(), JLabel.TRAILING); // html screws up table sizing below
            s = nextInstance.getName();
            nameLabel[i] = new JLabel(s);
            nameLabel[i].setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(0, 2, 0, 2))); // some space at sides
            nameLabel[i].setOpaque(true);
            nameLabel[i].setBackground(new Color(255, 255, 255, 64));
            if (viskit.ViskitStatics.debug) {
                System.out.println("really set label " + s);
            }

            s = nextInstance.getDescription();
            if (s != null && !s.isEmpty()) {
                nameLabel[i].setToolTipText(s);
            }

            entryTF[i] = new JTextField(8);
            entryTF[i].setToolTipText("Manually enter/override arguments "
                    + "here. Seperate vararg entries with commas");
            ViskitStatics.clampHeight(entryTF[i]);

            // If we have a factory, then reflect the Object... input to the
            // getInstance() method of RVF
            if (nextInstance instanceof ViskitModelInstantiator.Factory) {
                vif = (ViskitModelInstantiator.Factory) nextInstance;
                if (!vif.getParams().isEmpty() && vif.getParams().get(0) instanceof ViskitModelInstantiator.FreeF) {
                    viff = (ViskitModelInstantiator.FreeF) vif.getParams().get(0);
                    jTFText = viff.getValue();
                }
            } else {
                // Show the formal parameter type in the TF
                jTFText = nextInstance.toString();
            }

            entryTF[i].setText(jTFText);
            entryTF[i].addCaretListener(this);

            Class<?> c = ViskitStatics.getClassForInstantiatorType(nextInstance.getType());

            if (c == null) {
                LOG.error("what to do here for " + nextInstance.getType());
                return;
            }

            if (!c.isPrimitive() || c.isArray()) {
                tinyP = new JPanel();
                tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
                tinyP.add(entryTF[i]);
                b = new JButton("...");
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(),
                        BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                ViskitStatics.clampComponentSize(b, entryTF[i], b);

                tinyP.add(b);
                if (showLabels) {
                    typeLabelArray[i].setLabelFor(tinyP);
                }
                contentObj[i] = tinyP;
                b.setToolTipText("Edit with Instantiation Wizard");
                b.addActionListener(this);
                b.setActionCommand("" + i);
            } else {
                if (showLabels) {
                    typeLabelArray[i].setLabelFor(entryTF[i]);
                }
                contentObj[i] = entryTF[i];
            }
        }
        if (showLabels) {
            for (int x = 0; x < typeLabelArray.length; x++) {
                add(typeLabelArray[x]);
                add(contentObj[x]);
            }

            SpringUtilities.makeCompactGrid(this, typeLabelArray.length, 2, 5, 5, 5, 5);
        } else {
            for (int x = 0; x < typeLabelArray.length; x++) {
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

    /** The base of embedded parameters to finalize Event Graph constructor instantiation.
     * Provides support for Object... (varargs)
     *
     * @return a list of free form instantiators
     */
    public List<Object> getData() {
        ViskitModelInstantiator.Array via;
        ViskitModelInstantiator.Factory vif;
        List<Object> insts, params;
        Vector<Object> v = new Vector<>();
        for (int i = 0; i < typeLabelArray.length; i++) {
            if (shadow[i] instanceof ViskitModelInstantiator.FreeF) {
                ((ViskitModelInstantiator.FreeF) shadow[i]).setValue(entryTF[i].getText().trim());
            } else if (shadow[i] instanceof ViskitModelInstantiator.Array) {
                via = (ViskitModelInstantiator.Array) shadow[i];
                insts = via.getInstantiators();

                // TODO: Limit one instantiator per Array?
                if (insts.isEmpty())
                    insts.add(new ViskitModelInstantiator.FreeF(via.getType(), entryTF[i].getText().trim()));
            } else if (shadow[i] instanceof ViskitModelInstantiator.Factory) {
                vif = (ViskitModelInstantiator.Factory) shadow[i];
                params = vif.getParams();

                // TODO: Limit one parameter per Factory?
                if (params.isEmpty())
                    params.add(new ViskitModelInstantiator.FreeF(vif.getType(), entryTF[i].getText().trim()));
            }
            v.add(shadow[i]);
        }
        setData(v, true);
        return v;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int idx = Integer.parseInt(e.getActionCommand());

        ViskitModelInstantiator inst = shadow[idx];

        Class<?> c = ViskitStatics.getClassForInstantiatorType(inst.getType());
        if (c == null) {
            LOG.error("what to do here for " + inst.getType());
            return;
        }
        if (c.isArray()) {
            ArrayInspector ai = new ArrayInspector(parent);   // "this" could be locComp
            ai.setType(inst.getType());

            // Special case for Object... (varargs)
            if (inst instanceof ViskitModelInstantiator.FreeF) {
                List<Object> l = new ArrayList<>();
                l.add(inst);
                ai.setData(l);
            } else {
                ai.setData(((ViskitModelInstantiator.Array) inst).getInstantiators());
            }

            ai.setVisible(true); // blocks
            if (ai.modified) {
                shadow[idx] = ai.getData();
                entryTF[idx].setText(shadow[idx].toString());
                caretUpdate(null);
            }
        } else {
            ObjectInspector oi = new ObjectInspector(parent);
            oi.setType(inst.getType());

            try {
                oi.setData(inst);
            } catch (ClassNotFoundException e1) {
                String msg = "An object type specified in this element (probably " + inst.getType() + ") was not found.\n" +
                        "Add the XML or class file defining the element to the proper list at left.";
                ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyViewFrame().getController()).messageUser(
                        JOptionPane.ERROR_MESSAGE,
                        e1.getMessage(),
                        msg);
                return;
            }
            oi.setVisible(true); // blocks
            if (oi.modified) {

                ViskitModelInstantiator vi = oi.getData();
                if (vi == null) {return;}

                // Prevent something like RVF.getInstance(RandomVariate) from
                // being entered in the text field
                ViskitModelInstantiator.Factory fac = null;
                if (vi instanceof ViskitModelInstantiator.Factory)
                    fac = (ViskitModelInstantiator.Factory) vi;

                shadow[idx] = vi;
                if (fac != null)
                    entryTF[idx].setText(fac.getParams().getFirst().toString());
                else
                    entryTF[idx].setText(vi.toString());
                caretUpdate(null);
            }
        }
    }
}
