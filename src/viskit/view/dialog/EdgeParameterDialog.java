package viskit.view.dialog;

import viskit.model.ViskitEdgeParameter;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */
public class EdgeParameterDialog extends JDialog {

    private final JTextField valueField;       // Text field that holds the expression
    private final JLabel typeLabel;            // static type value passed in
    private static EdgeParameterDialog dialog;
    private static boolean modified = false;
    private ViskitEdgeParameter param;
    private String type;
    private final JButton okButton;
    private JButton canButt;
    public static String newValue;

    public static boolean showDialog(JDialog d, ViskitEdgeParameter parm) {
        if (dialog == null) {
            dialog = new EdgeParameterDialog(d, parm);
        } else {
            dialog.setParams(d, parm);
        }
        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private EdgeParameterDialog(JDialog parent, ViskitEdgeParameter param) {
        super(parent, "Edge Parameter", true);
        this.param = param;
        this.type = param.bogus != null ? param.bogus : "";

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new myCloseListener());

        Container cont = getContentPane();
        cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

        JPanel con = new JPanel();
        con.setLayout(new BoxLayout(con, BoxLayout.Y_AXIS));
        con.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        JLabel valueLab = new JLabel("value");
        JLabel typeLab = new JLabel("type");
        int w = OneLinePanel.maxWidth(new JComponent[]{valueLab, typeLab});

        valueField = new JTextField(25);
        setMaxHeight(valueField);
        typeLabel = new JLabel("argument type");

        fieldsPanel.add(new OneLinePanel(typeLab, w, typeLabel));
        fieldsPanel.add(new OneLinePanel(valueLab, w, valueField));
        con.add(fieldsPanel);
        con.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButton = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(okButton);
        buttPan.add(canButt);
        con.add(buttPan);
        con.add(Box.createVerticalGlue());           // takes up space when dialog is expanded vertically
        cont.add(con);

        // attach listeners
        canButt.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());

        enableApplyButtonListener lis = new enableApplyButtonListener();
        this.valueField.addCaretListener(lis);

        setParams(parent, param);
    }

    private void setMaxHeight(JComponent c) {
        Dimension d = c.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        c.setMaximumSize(d);
    }

    public final void setParams(Component c, ViskitEdgeParameter p) {
        param = p;
        type = p.bogus != null ? p.bogus : "";

        fillWidgets();

        modified = (p == null);
        okButton.setEnabled((p == null));

        getRootPane().setDefaultButton(canButt);
        pack();
        setLocationRelativeTo(c);
    }

    private void fillWidgets() {
        valueField.setText(param.getValue());
        typeLabel.setText(type);
    }

    private void unloadWidgets() {
        if (param != null) {
            param.setValue(valueField.getText().trim());
        } else {
            newValue = valueField.getText().trim();
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
                int ret = JOptionPane.showConfirmDialog(EdgeParameterDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    canButt.doClick();
                }
            } else {
                canButt.doClick();
            }
        }
    }
}
