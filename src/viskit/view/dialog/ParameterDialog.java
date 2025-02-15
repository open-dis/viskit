package viskit.view.dialog;

import viskit.model.ViskitParameter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import viskit.ViskitGlobals;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */
public class ParameterDialog extends JDialog {

    private static ParameterDialog dialog;
    private static boolean modified = false;
    public static String newName,  newType,  newComment;
    private static int count = 0;

    private final JTextField parameterNameField;    // Text field that holds the parameter name
    private final JTextField expressionField;       // Text field that holds the expression
    private final JTextField descriptionField;      // Text field that holds the description
    private final JComboBox parameterTypeCombo;     // Editable combo box that lets us select a type
    private ViskitParameter param;
    private final JButton okButton;
    private JButton cancelButton;

    public static boolean showDialog(JFrame f, ViskitParameter parm) {
        if (dialog == null) {
            dialog = new ParameterDialog(f, parm);
        } else {
            dialog.setParams(f, parm);
        }

        dialog.setVisible(true);
        // above call blocks

        return modified;
    }

    private ParameterDialog(JFrame parent, ViskitParameter param) {
        super(parent, "Simulation Parameter Inspector", true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new myCloseListener());

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel con = new JPanel();
        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));

        con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel descriptionLabel = new JLabel("description");
        descriptionLabel.setToolTipText(DESCRIPTION_HINT);
        int w = OneLinePanel.maxWidth(new JComponent[]{nameLab, initLab, typeLab, descriptionLabel});

        parameterNameField = new JTextField(15);
        setMaxHeight(parameterNameField);
        expressionField = new JTextField(25);
        setMaxHeight(expressionField);
        descriptionField = new JTextField(25);
        descriptionField.setToolTipText(DESCRIPTION_HINT);
        setMaxHeight(descriptionField);

        parameterTypeCombo = ViskitGlobals.instance().getViskitTypeComboBox();
        setMaxHeight(parameterTypeCombo);

        fieldsPanel.add(new OneLinePanel(nameLab, w, parameterNameField));
        fieldsPanel.add(new OneLinePanel(typeLab, w, parameterTypeCombo));
        fieldsPanel.add(new OneLinePanel(descriptionLabel, w, descriptionField));
        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(okButton);
        buttPan.add(cancelButton);
        con.add(buttPan);
        con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
        cont.add(con);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        parameterNameField.addCaretListener(lis);
        descriptionField.addCaretListener(lis);
        expressionField.addCaretListener(lis);
        parameterTypeCombo.addActionListener(lis);

        setParams(parent, param);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParams(Component c, ViskitParameter p) {
        param = p;

        fillWidgets();

        okButton.setEnabled((p == null));

        getRootPane().setDefaultButton(cancelButton);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        if (param != null) {
            parameterNameField.setText(param.getName());
            parameterTypeCombo.setSelectedItem(param.getType());
            descriptionField.setText(param.getComment());
        } else {
            parameterNameField.setText("param_" + count++);
            descriptionField.setText("");
        }
    }

    private void unloadWidgets() {
        String ty = (String) parameterTypeCombo.getSelectedItem();
        ty = ViskitGlobals.instance().typeChosen(ty);
        String nm = parameterNameField.getText();
        nm = nm.replaceAll("\\s", "");
        if (param != null) {
            param.setName(nm);
            //
            if (ty.equals("String") || ty.equals("Double") || ty.equals("Integer")) {
                ty = "java.lang." + ty;
            }
            param.setType(ty);
            param.setComment(descriptionField.getText());
        } else {
            newName = nm;
            newType = ty;
            newComment = descriptionField.getText().trim();
        }
    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (modified) {
                unloadWidgets();
            }
            dispose();
        }
    }

    class enableApplyButtonListener implements CaretListener, ActionListener {

        @Override
        public void caretUpdate(CaretEvent event) {
            modified = true;
            okButton.setEnabled(true);
            getRootPane().setDefaultButton(okButton);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(ParameterDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    cancelButton.doClick();
                }
            } else {
                cancelButton.doClick();
            }
        }
    }
}
