package viskit.view.dialog;

import edu.nps.util.SpringUtilities;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import viskit.ViskitStatics;
import viskit.model.VInstantiator;
import viskit.view.ObjectListPanel;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 16, 2004
 * @since 3:27:42 PM
 * @version $Id$
 */
public class ArrayInspector extends JDialog {

    public boolean modified = false;
    private final JButton cancelButton,  okButton;
    private final JPanel buttonPanel,  contentPanel;
    private final JTextField typeTF,  sizeTF;
    private JPanel upPan;
    private final enableApplyButtonListener listener;

    public ArrayInspector(JDialog parent) {
        super(parent, "Array Inspector", true);
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(contentPanel);

        upPan = new JPanel(new SpringLayout());
        JLabel typeLabel = new JLabel("Array type", JLabel.TRAILING);
        typeTF = new JTextField();
        typeTF.setEditable(false);
        ViskitStatics.clampHeight(typeTF);
        typeLabel.setLabelFor(typeTF);
        JLabel countLab = new JLabel("Array length", JLabel.TRAILING);
        sizeTF = new JTextField(50); // columns to initially grow the display
        ViskitStatics.clampHeight(sizeTF);
        countLab.setLabelFor(sizeTF);

        JLabel helpLab = new JLabel("");
        JLabel helpTextLabel = new JLabel("Press return to resize list");
        helpTextLabel.setFont(sizeTF.getFont());
        helpLab.setLabelFor(helpTextLabel);

        upPan.add(typeLabel);
        upPan.add(typeTF);
        upPan.add(countLab);
        upPan.add(sizeTF);
        upPan.add(helpLab);
        upPan.add(helpTextLabel);

        SpringUtilities.makeCompactGrid(upPan, 3, 2, 5, 5, 5, 5);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            okButton = new JButton("Apply changes");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // attach listeners
        listener = new enableApplyButtonListener();
        typeTF.addCaretListener(listener);
        sizeTF.addCaretListener(listener);
        sizeTF.addActionListener(new sizeListener());
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());
        okButton.setEnabled(false);
    }
    ObjectListPanel olp;

    public void setData(List<Object> lis) // of instantiators
    {
        olp = new ObjectListPanel(listener);
        olp.setDialogInfo((JDialog) getParent());
        olp.setData(lis, false); // don't show the type

        contentPanel.removeAll();
        contentPanel.add(upPan);
        contentPanel.add(Box.createVerticalStrut(5));
        JScrollPane jsp = new JScrollPane(olp);
        jsp.getViewport().setPreferredSize(new Dimension(Integer.MAX_VALUE, 240));
        contentPanel.add(jsp);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(buttonPanel);

        sizeTF.setText("" + lis.size());
        pack();
        setLocationRelativeTo(getParent());

        // Something going on....the array size textfield doesn't become active again until we do this...
        olp.requestFocus();
        sizeTF.requestFocus();
    }
    String myTyp;

    public void setType(String typ) {
        Class<?> c = ViskitStatics.getClassForInstantiatorType(typ);

        myTyp = ViskitStatics.convertClassName(c.getComponentType().getName());

        typeTF.setText(typ);
    }

    public VInstantiator.Array getData() {
        return new VInstantiator.Array(typeTF.getText().trim(), olp.getData());
    }

    class sizeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            setSize();
        }
    }

    public void setSize() {
        String s = sizeTF.getText().trim();
        int sz;
        try {
            sz = Integer.parseInt(s);
        } catch (NumberFormatException e1) {
            return;
        }
        if (sz <= 0) {
            return;
        }

        Vector<Object> v = new Vector<>(sz);
        if (myTyp.equals(ViskitStatics.RANDOM_VARIATE_CLASS)) {
            for (int i = 0; i < sz; i++) {
                v.add(new VInstantiator.Factory(myTyp,
                        ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS,
                        ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
                        new Vector<>()
                ));
            }
        } else {
            for (int i = 0; i < sz; i++) {
                v.add(new VInstantiator.FreeF(myTyp, ""));
            }
        }
        setData(v);
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
}
