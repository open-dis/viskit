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
import viskit.ViskitGlobals;
import viskit.model.ViskitInstantiator;
import viskit.view.dialog.ArrayInspector;
import viskit.view.dialog.ObjectInspector;
import viskit.ViskitStatics;
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
public class ObjectListPanel extends JPanel implements ActionListener, CaretListener {

    private JDialog parent;
	private JLabel         nameLabelArray[];
    private JLabel         typeLabelArray[];
    private JTextField     entryTFArray  [];
	private JComponent     panelArray[];
    private ViskitInstantiator  shadowInstantiatorArray[];
    private ActionListener changeListener;

    public ObjectListPanel(ActionListener changeListener) {
        setLayout(new SpringLayout());
        this.changeListener = changeListener;
    }

    public void setDialogInfo(JDialog parent) {
        this.parent = parent;
    }

    public void setData(List<Object> parameterObjectList, // of Vinstantiators 
			            boolean showLabels)
    {
        int numberOfParameters = parameterObjectList.size();
        typeLabelArray = new JLabel[numberOfParameters];
        nameLabelArray = (numberOfParameters <= 0 ? null : new JLabel[numberOfParameters]);
          entryTFArray = new JTextField[numberOfParameters]; // user enters initial value here
            panelArray = new JComponent[numberOfParameters];
        shadowInstantiatorArray = new ViskitInstantiator[numberOfParameters];

        if (viskit.ViskitStatics.debug) {
            System.out.println("really has " + numberOfParameters + "parameters");
        }
        int i = 0;
        String parameterTypeName;
        for (Iterator<Object> iterator = parameterObjectList.iterator(); iterator.hasNext(); i++)
		{
            ViskitInstantiator instantiator = (ViskitInstantiator) iterator.next();
            shadowInstantiatorArray[i] = instantiator;
            typeLabelArray[i] = new JLabel("(" + instantiator.getTypeName() + ")" + " " + instantiator.getName(), JLabel.TRAILING); // html screws up table sizing below
            String parameterName        = instantiator.getName();
            nameLabelArray[i] = new JLabel(parameterName);
            nameLabelArray[i].setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(0, 2, 0, 2))); // some space at sides
            nameLabelArray[i].setOpaque(true);
            nameLabelArray[i].setBackground(new Color(255, 255, 255, 64));

            String parameterDescription = instantiator.getDescription();
            if (parameterDescription != null && !parameterDescription.isEmpty()) {
                typeLabelArray[i].setToolTipText(parameterDescription);
                nameLabelArray[i].setToolTipText(parameterDescription);
            }

            entryTFArray[i] = new JTextField(8);
            entryTFArray[i].setToolTipText("Manually enter/override method "
                    + "arguments here, use proper Java syntax (quote strings, comma separated, etc.)");
            ViskitStatics.clampHeight(entryTFArray[i]);

            parameterTypeName = instantiator.getTypeName();

            // If we have a factory, then reflect the Object... input to the getInstance() method of RandomVariateFactory (RVF)
			String parameterInitializationValue = new String();
			String parameterInitializationHint  = new String();
            if (instantiator instanceof ViskitInstantiator.Factory) {
                ViskitInstantiator.Factory vif = (ViskitInstantiator.Factory) instantiator;
                if (!vif.getParametersList().isEmpty() && vif.getParametersList().get(0) instanceof ViskitInstantiator.FreeForm)
				{
                    ViskitInstantiator.FreeForm viff = (ViskitInstantiator.FreeForm) vif.getParametersList().get(0);
                    parameterInitializationValue = viff.getValue();
					parameterInitializationHint = "enter " + parameterTypeName + " initial value(s) using Java syntax (quoted strings, comma-separated values, etc.)";
                }
            }
			else if (instantiator instanceof ViskitInstantiator.FreeForm) {
				parameterInitializationValue = ((ViskitInstantiator.FreeForm) instantiator).getValue();
				parameterInitializationHint = "enter initial value";
			}
			String traceMessage = "parameter name=" + parameterName + ", type=" + parameterTypeName + ", value=" + parameterInitializationValue + ", description=" + parameterDescription;
            if (viskit.ViskitStatics.debug) {
                System.out.println(traceMessage);
            }

            entryTFArray[i].setText       (parameterInitializationValue);
            entryTFArray[i].setToolTipText(parameterInitializationHint); // user prompt
            entryTFArray[i].addCaretListener(this);

            Class<?> parameterType = ViskitStatics.getClassForInstantiatorType(instantiator.getTypeName());

            if (parameterType == null) {
                System.err.println("what to do here for " + instantiator.getTypeName());
                return;
            }

            if (parameterType != null) {
                if (!parameterType.isPrimitive() || parameterType.isArray()) {
                    JPanel tinyPanel = new JPanel();
                    tinyPanel.setLayout(new BoxLayout(tinyPanel, BoxLayout.X_AXIS));
                    tinyPanel.add(entryTFArray[i]);
                    JButton dotdotdotButton = new JButton("...");
                    dotdotdotButton.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(),
                            BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    ViskitStatics.clampSize(dotdotdotButton, entryTFArray[i], dotdotdotButton);

                    tinyPanel.add(dotdotdotButton);
                    if (showLabels) {
                        typeLabelArray[i].setLabelFor(tinyPanel);
                    }
                    panelArray[i] = tinyPanel;
                    dotdotdotButton.setToolTipText("Edit with Instantiation Wizard");
                    dotdotdotButton.addActionListener(this);
                    dotdotdotButton.setActionCommand("" + i);
                } else {
                    if (showLabels) {
                        typeLabelArray[i].setLabelFor(entryTFArray[i]);
                    }
                    panelArray[i] = entryTFArray[i];
                }
            }
        }
        if (showLabels) {
            for (int x = 0; x < typeLabelArray.length; x++) {
                add(typeLabelArray[x]);
                add(panelArray[x]);
            }

            SpringUtilities.makeCompactGrid(this, typeLabelArray.length, 2, 5, 5, 5, 5);
        } else {
            for (int x = 0; x < typeLabelArray.length; x++) {
                add(panelArray[x]);
            }
            SpringUtilities.makeCompactGrid(this, entryTFArray.length, 1, 5, 5, 5, 5);
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
        for (int i = 0; i < typeLabelArray.length; i++) {
            if (shadowInstantiatorArray[i] instanceof ViskitInstantiator.FreeForm) {
                ((ViskitInstantiator.FreeForm) shadowInstantiatorArray[i]).setValue(entryTFArray[i].getText().trim());
            } else if (shadowInstantiatorArray[i] instanceof ViskitInstantiator.Array) {
                ViskitInstantiator.Array via = (ViskitInstantiator.Array) shadowInstantiatorArray[i];
                List<Object> insts = via.getInstantiators();

                // TODO: Limit one instantiator per Array?
                if (insts.isEmpty())
                    insts.add(new ViskitInstantiator.FreeForm(via.getTypeName(), entryTFArray[i].getText().trim()));
            } else if (shadowInstantiatorArray[i] instanceof ViskitInstantiator.Factory) {
                ViskitInstantiator.Factory vif = (ViskitInstantiator.Factory) shadowInstantiatorArray[i];
                List<Object> params = vif.getParametersList();

                // TODO: Limit one parameter per Factory?
                if (params.isEmpty())
                    params.add(new ViskitInstantiator.FreeForm(vif.getTypeName(), entryTFArray[i].getText().trim()));
            }
            v.add(shadowInstantiatorArray[i]);
        }
        setData(v, true);
        return v;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int actionCommandIndex = Integer.parseInt(e.getActionCommand());

        ViskitInstantiator vInstantiator = shadowInstantiatorArray[actionCommandIndex];

        Class<?> classForInstantiatorType = ViskitStatics.getClassForInstantiatorType(vInstantiator.getTypeName());
        if (classForInstantiatorType == null) {
            System.err.println("what to do here for " + vInstantiator.getTypeName());
            return;
        }
        if (classForInstantiatorType.isArray()) {
            ArrayInspector arrayInspector = new ArrayInspector(parent);   // "this" could be locComp
            arrayInspector.setType(vInstantiator.getTypeName());

            // Special case for Object... (varargs)
            if (vInstantiator instanceof ViskitInstantiator.FreeForm) {
                List<Object> objectList = new ArrayList<>();
                objectList.add((ViskitInstantiator.FreeForm) vInstantiator);
                arrayInspector.setData(objectList);
            } else {
                arrayInspector.setData(((ViskitInstantiator.Array) vInstantiator).getInstantiators());
            }

            arrayInspector.setVisible(true); // blocks
            if (arrayInspector.modified) {
                 shadowInstantiatorArray[actionCommandIndex] = arrayInspector.getData();
                entryTFArray[actionCommandIndex].setText(shadowInstantiatorArray[actionCommandIndex].toString());
                caretUpdate(null);
            }
        } else {
            ObjectInspector objectInspector = new ObjectInspector(parent);
            objectInspector.setType(vInstantiator.getTypeName());

            try {
                objectInspector.setData(vInstantiator);
            } catch (ClassNotFoundException e1) {
                String msg = "An object type specified in this element (probably " + vInstantiator.getTypeName() + ") was not found.\n" +
                        "Add the XML or class file defining the element to the proper list at left.";
                ((AssemblyController)ViskitGlobals.instance().getAssemblyEditViewFrame().getController()).messageToUser(
                        JOptionPane.ERROR_MESSAGE,
                        e1.getMessage(),
                        msg);
                return;
            }
            objectInspector.setVisible(true); // blocks
            if (objectInspector.modified) {

                ViskitInstantiator vi = objectInspector.getData();
                if (vi == null) {return;}

                // Prevent something like RVF.getInstance(RandomVariate) from
                // being entered in the text field
                if (vi instanceof ViskitInstantiator.Factory) {
                    ViskitInstantiator.Factory viFactory = (ViskitInstantiator.Factory) vi;
                    if (!viFactory.getParametersList().isEmpty()) {
                        return;
                    }
                }
                shadowInstantiatorArray[actionCommandIndex] = vi;
                entryTFArray[actionCommandIndex].setText(vi.toString());
                caretUpdate(null);
            }
        }
    }
}
