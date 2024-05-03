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
import viskit.VStatics;
import viskit.model.VInstantiator;
import viskit.view.ObjListPanel;

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
    private JButton canButt,  okButt;
    private JPanel buttPan,  contentP;
    private JTextField typeTF,  sizeTF;
    private JPanel upPan;
    private enableApplyButtonListener listnr;

    public ArrayInspector(JDialog parent) {
        super(parent, "Array Inspector", true);
        contentP = new JPanel();
        contentP.setLayout(new BoxLayout(contentP, BoxLayout.Y_AXIS));
        contentP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(contentP);

        upPan = new JPanel(new SpringLayout());
        JLabel typeLab = new JLabel("Array type", JLabel.TRAILING);
        typeTF = new JTextField();
        typeTF.setEditable(false);
        VStatics.clampHeight(typeTF);
        typeLab.setLabelFor(typeTF);
        JLabel countLab = new JLabel("Array length", JLabel.TRAILING);
        sizeTF = new JTextField();
        VStatics.clampHeight(sizeTF);
        countLab.setLabelFor(sizeTF);

        JLabel helpLab = new JLabel("");
        JLabel helpTextLabel = new JLabel("Press return to resize list");
        helpTextLabel.setFont(sizeTF.getFont());
        helpLab.setLabelFor(helpTextLabel);

        upPan.add(typeLab);
        upPan.add(typeTF);
        upPan.add(countLab);
        upPan.add(sizeTF);
        upPan.add(helpLab);
        upPan.add(helpTextLabel);

        SpringUtilities.makeCompactGrid(upPan, 3, 2, 5, 5, 5, 5);

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
        sizeTF.addCaretListener(listnr);
        sizeTF.addActionListener(new sizeListener());
        canButt.addActionListener(new cancelButtonListener());
        okButt.addActionListener(new applyButtonListener());
        okButt.setEnabled(false);
    }
    ObjListPanel olp;

    public void setData(List<Object> lis) // of instantiators
    {
        olp = new ObjListPanel(listnr);
        olp.setDialogInfo((JDialog) getParent());
        olp.setData(lis, false); // don't show the type

        contentP.removeAll();
        contentP.add(upPan);
        contentP.add(Box.createVerticalStrut(5));
        JScrollPane jsp = new JScrollPane(olp);
        jsp.getViewport().setPreferredSize(new Dimension(Integer.MAX_VALUE, 240));
        contentP.add(jsp);
        contentP.add(Box.createVerticalStrut(5));
        contentP.add(buttPan);

        sizeTF.setText("" + lis.size());
        pack();
        setLocationRelativeTo(getParent());

        // Something going on....the array size textfield doesn't become active again until we do this...
        olp.requestFocus();
        sizeTF.requestFocus();
    }
    String myTyp;

    public void setType(String typ) {
        Class<?> c = VStatics.getClassForInstantiatorType(typ);

        myTyp = VStatics.convertClassName(c.getComponentType().getName());

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
        if (myTyp.equals(VStatics.RANDOM_VARIATE_CLASS)) {
            for (int i = 0; i < sz; i++) {
                v.add(new VInstantiator.Factory(myTyp,
                        VStatics.RANDOM_VARIATE_FACTORY_CLASS,
                        VStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD,
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
            okButt.setEnabled(true);
            getRootPane().setDefaultButton(okButt);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            caretUpdate(null);
        }
    }
}
