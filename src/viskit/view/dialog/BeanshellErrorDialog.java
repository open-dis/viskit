package viskit.view.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import viskit.ViskitGlobals;
import viskit.ViskitConfigurationStore;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2005
 * @since 10:48:48 AM
 * @version $Id$
 */
public class BeanshellErrorDialog extends JDialog {

    private static BeanshellErrorDialog me;
    
    private final JTextArea errorMsg;
    private boolean returnBool = false;
    private final JCheckBox current;
    private final JCheckBox permanent;
    private final JCheckBox nullCB;
    private JCheckBox selectedButt;

    public static boolean showDialog(String errMsg, Component locationComp) {
        if (me == null) {
            me = new BeanshellErrorDialog();
        }
        me.errorMsg.setText(errMsg);
        me.errorMsg.setCaretPosition(0);
        me.setLocationRelativeTo(locationComp);
        me.setVisible(true);
        return me.returnBool;
    }

    private BeanshellErrorDialog() {
        ViskitConfigurationStore cfg = ViskitConfigurationStore.instance();

        setTitle(cfg.getValue(ViskitConfigurationStore.BEANSHELL_ERROR_DIALOG_TITLE_KEY)); //"Warning"
        setModal(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPan = new JPanel();
        topPan.setLayout(new BoxLayout(topPan, BoxLayout.X_AXIS));
        topPan.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        JPanel leftPan = new JPanel();
        leftPan.setLayout(new BoxLayout(leftPan, BoxLayout.Y_AXIS));

        Icon ic = UIManager.getIcon("OptionPane.warningIcon");
        JLabel lab = new JLabel(cfg.getValue(ViskitConfigurationStore.BEANSHELL_ERROR_DIALOG_LABEL_KEY), ic, JLabel.LEFT);//"Java language error:"
        lab.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        leftPan.add(lab);
        errorMsg = new JTextArea(4, 40);
        errorMsg.setLineWrap(true);
        errorMsg.setWrapStyleWord(true);
        errorMsg.setBackground(lab.getBackground());      // lose the white
        JScrollPane jsp = new JScrollPane(errorMsg);
        jsp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0),
                BorderFactory.createLineBorder(Color.black, 1)));
        jsp.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
        leftPan.add(jsp);
        leftPan.add(Box.createVerticalStrut(3));
        lab = new JLabel(cfg.getValue(ViskitConfigurationStore.BEANSHELL_ERROR_DIALOG_QUESTION_KEY)); //"Ignore and continue?"
        lab.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        leftPan.add(lab);
        topPan.add(leftPan);
        topPan.add(Box.createHorizontalStrut(15));
        p.add(topPan);

        p.add(Box.createVerticalStrut(10));

        current = new JCheckBox(cfg.getValue(ViskitConfigurationStore.BEANSHELL_ERROR_DIALOG_SESSIONCHECKBOX_KEY)); //"Hide warnings for current session"
        permanent = new JCheckBox(cfg.getValue(ViskitConfigurationStore.BEANSHELL_ERROR_DIALOG_PREFERENCESCHECKBOX_KEY)); //"Hide warnings permanently"
        nullCB = new JCheckBox("hidden");
        current.setSelected(false);
        permanent.setSelected(false);
        nullCB.setSelected(true);
        selectedButt = nullCB;
        ButtonGroup bg = new ButtonGroup();
        bg.add(current);
        bg.add(permanent);
        bg.add(nullCB);

        permanent.setToolTipText(cfg.getValue(ViskitConfigurationStore.BEANSHELL_ERROR_DIALOG_PREFERENCESTOOLTIP_KEY)); //"Set permanent options in File->Preferences"

        current.addActionListener(new cbTweeker(current));
        permanent.addActionListener(new cbTweeker(permanent));

        current.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        permanent.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        JPanel cbPan = new JPanel();
        cbPan.setLayout(new BoxLayout(cbPan, BoxLayout.Y_AXIS));
        cbPan.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray, 1),
                "if yes"));
        cbPan.add(current);
        cbPan.add(permanent);
        p.add(cbPan);

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        buttPan.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        buttPan.add(Box.createHorizontalGlue());
        JButton noButton = new JButton("No");
        JButton yesButton = new JButton("Yes");

        buttPan.add(noButton);
        buttPan.add(yesButton);

        p.add(Box.createVerticalStrut(5));
        p.add(buttPan);

        setContentPane(p);
        pack();

        noButton.addActionListener((ActionEvent e) -> {
            returnBool = false;
            dispose();
        });

        yesButton.addActionListener((ActionEvent e) -> {
            if (current.isSelected()) {
                ViskitConfigurationStore.instance().setSessionValue(ViskitConfigurationStore.BEANSHELL_WARNING_KEY, "false");
            } else if (permanent.isSelected()) {
                ViskitConfigurationStore.instance().setValue(ViskitConfigurationStore.BEANSHELL_WARNING_KEY, "false");
            }
            returnBool = true;
            dispose();
        });
    }

    class cbTweeker implements ActionListener {

        JCheckBox cb;

        cbTweeker(JCheckBox cb) {
            this.cb = cb;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedButt == cb) {// need to turn it off
                nullCB.setSelected(true);
                selectedButt = nullCB;
            } else {
                selectedButt = cb;
            }
        }
    }

    public static void main(String[] args) {
        BeanshellErrorDialog.showDialog("Mama Leone and \nblah blah", null);
        ViskitGlobals.instance().systemExit(0);
    }
}
