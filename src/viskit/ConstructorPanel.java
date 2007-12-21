package viskit;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 8, 2004
 * @since 8:33:17 AM
 * @version $Id: ConstructorPanel.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public class ConstructorPanel extends JPanel {

    private JButton selectButt;
    private ActionListener modLis;
    private JPanel selectButtPan;
    private boolean showButt;
    private ActionListener selectButtListener;
    private ObjListPanel olp;
    private JDialog parent;

    public ConstructorPanel(ActionListener modifiedListener, boolean showSelectButt, ActionListener selectListener, JDialog parent) {
        modLis = modifiedListener;
        showButt = showSelectButt;
        selectButtListener = selectListener;
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        selectButtPan = new JPanel();
        selectButtPan.setLayout(new BoxLayout(selectButtPan, BoxLayout.X_AXIS));
        selectButtPan.add(Box.createHorizontalGlue());
        selectButt = new JButton("Select this constructor");
        selectButtPan.add(selectButt);
        selectButtPan.add(Box.createHorizontalGlue());
    }

    public void setData(List args) // of VInstantiators
    {
        this.removeAll();
        add(Box.createVerticalGlue());

        olp = new ObjListPanel(modLis); // may have to intercept
        olp.setDialogInfo(parent, parent);
        olp.setData(args, true);
        if (args.size() > 0) {
            add(olp);
        } else {
            JLabel lab = new JLabel("zero argument constructor");
            lab.setAlignmentX(Box.CENTER_ALIGNMENT);
            add(lab);
        }
        if (showButt) {
            add(Box.createVerticalStrut(5));
            add(Box.createVerticalGlue());

            add(selectButtPan);
            add(Box.createVerticalStrut(5));

            if (selectButtListener != null) {
                selectButt.addActionListener(selectButtListener);
            }

            setSelected(false);
        } else {
            add(Box.createVerticalGlue());
            setSelected(true);
        }
        validate();
    }
    /*
    public ConstructorPanel(Constructor construct, ActionListener selectListener, CaretListener modifiedListener)
    {
    this(construct,selectListener,modifiedListener,true);
    }
    public ConstructorPanel(Constructor construct, ActionListener selectListener, CaretListener modifiedListener,
    boolean showSelectButt)
    {
    rvfs = new RandomVariateFromString();
    constructor = construct;
    signature = constructor.getParameterTypes();
    field = new JTextField[signature.length];
    label = new JLabel[signature.length];
    JPanel innerP = null;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(Box.createVerticalGlue());
    if (signature.length > 0) {
    innerP = new JPanel(new SpringLayout());
    innerP.setBorder(BorderFactory.createEtchedBorder());
    innerP.setLayout(new SpringLayout());
    for (int i = 0; i < label.length; ++i) {
    label[i] = new JLabel(Vstatics.convertClassName(signature[i].getName()), JLabel.TRAILING);
    innerP.add(label[i]);
    field[i] = new JTextField(10);
    if (modifiedListener != null)
    field[i].addCaretListener(modifiedListener);
    Vstatics.clampHeight(field[i]);
    // Do the extended constructor game
    // Terminal Param, MultiParam, Factoryparm
    Class c = null;
    c = signature[i];
    if (c != null) {
    if (!c.isPrimitive() || c.isArray()) {
    JPanel tinyP = new JPanel();
    tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
    tinyP.add(field[i]);
    JButton b = new JButton("...");
    b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    Vstatics.clampSize(b, field[i], b);
    tinyP.add(b);
    innerP.add(tinyP);
    label[i].setLabelFor(tinyP);
    if (c.isArray()) {
    b.setToolTipText("Edit with array wizard");
    }
    else {
    b.setToolTipText("Edit with new object wizard");
    b.addActionListener(new FactoryArgListener(i));
    }
    }
    else {
    innerP.add(field[i]);
    label[i].setLabelFor(field[i]);
    }
    }
    }
    SpringUtilities.makeCompactGrid(innerP, signature.length, 2, 5, 5, 5, 5);
    }
    else {
    innerP = new JPanel(new BorderLayout());
    innerP.add(new JLabel("<no parameters>", JLabel.CENTER), BorderLayout.CENTER);
    }
    add(innerP);
    if(showSelectButt) {
    add(Box.createVerticalStrut(5));
    add(Box.createVerticalGlue());
    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    buttPan.add(Box.createHorizontalGlue());
    selectButt = new JButton("Select this constructor");
    buttPan.add(selectButt);
    buttPan.add(Box.createHorizontalGlue());
    add(buttPan);
    add(Box.createVerticalStrut(5));
    if (selectListener != null)
    selectButt.addActionListener(selectListener);
    setSelected(false);
    }
    else
    setSelected(true);
    }
     */

    public List<Object> getData() {
        return olp.getData(); // of VInstantiators
    }

    public void setSelected(boolean tf) {
        if (olp != null) {
            olp.setEnabled(tf);
        }  // todo...make this work  maybe olp should be built in constructor
/*
    for(int i=0;i<field.length;i++) {
    field[i].setEnabled(tf);
    label[i].setEnabled(tf);
    }
     */
    }

    /**
     * @param clazz Class[] array, tyoically from constructor signature
     * @return String identifying Class's signature
     */
    public static String getSignature(Class[] clazz) {
        StringBuffer buf = new StringBuffer("(");
        for (int i = 0; i < clazz.length; ++i) {
            buf.append(clazz[i].getName());
            if (i < clazz.length - 1) {
                buf.append(',');
            }
        }
        buf.append(')');
        return buf.toString();
    }
}

