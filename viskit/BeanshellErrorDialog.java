package viskit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Mar 8, 2005
 * Time: 10:48:48 AM
 */

public class BeanshellErrorDialog extends JDialog
{
  private JTextArea errorMsg;
  private boolean returnBool = false;

  private static BeanshellErrorDialog me;
  private JCheckBox current, permanent, nullCB;
  private JCheckBox selectedButt;

  public static boolean showDialog(String errMsg, Component locationComp)
  {
    if (me == null)
      me = new BeanshellErrorDialog();
    me.errorMsg.setText(errMsg);
    me.errorMsg.setCaretPosition(0);
    me.setLocationRelativeTo(locationComp);
    me.setVisible(true);
    return me.returnBool;
  }

  private BeanshellErrorDialog()
  {
    ViskitConfig cfg = ViskitConfig.instance();

    setTitle(cfg.getVal("gui.beanshellerrordialog.title")); //"Warning"
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
    JLabel lab = new JLabel(cfg.getVal("gui.beanshellerrordialog.label"), ic, JLabel.LEFT);//"Java language error:"
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
    lab = new JLabel(cfg.getVal("gui.beanshellerrordialog.question")); //"Ignore and continue?"
    lab.setAlignmentX(JLabel.LEFT_ALIGNMENT);
    leftPan.add(lab);
    topPan.add(leftPan);
    topPan.add(Box.createHorizontalStrut(15));
    p.add(topPan);

    p.add(Box.createVerticalStrut(10));

    current = new JCheckBox(cfg.getVal("gui.beanshellerrordialog.sessioncheckbox")); //"Hide warnings for current session"
    permanent = new JCheckBox(cfg.getVal("gui.beanshellerrordialog.preferencescheckbox")); //"Hide warnings permanently"
    nullCB = new JCheckBox("hidden");
    current.setSelected(false);
    permanent.setSelected(false);
    nullCB.setSelected(true);
    selectedButt = nullCB;
    ButtonGroup bg = new ButtonGroup();
    bg.add(current);
    bg.add(permanent);
    bg.add(nullCB);

    permanent.setToolTipText(cfg.getVal("gui.beanshellerrordialog.preferencestooltip")); //"Set permanent options in File->Preferences"

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

    noButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        returnBool = false;
        setVisible(false);
      }
    });

    yesButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (current.isSelected())
          ViskitConfig.instance().setSessionVal("app.beanshell.warning", "false");
        else if (permanent.isSelected()) {
          ViskitConfig.instance().setVal("app.beanshell.warning", "false");
        }
        returnBool = true;
        setVisible(false);
      }
    });
  }

  class cbTweeker implements ActionListener
  {
    JCheckBox cb;

    cbTweeker(JCheckBox cb)
    {
      this.cb = cb;
    }

    public void actionPerformed(ActionEvent e)
    {
      if (selectedButt == cb) {// need to turn it off
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            nullCB.setSelected(true);
          }
        });
        selectedButt = nullCB;
      }
      else {
        selectedButt = cb;
      }
    }
  }

  public static void main(String[] args)
  {
    //setLAF();
    BeanshellErrorDialog.showDialog("Mama Leone and \nblah blah", null);
    System.exit(0);
  }
/*
  private static void setLAF()
  {
  String laf = "javax.swing.plaf.metal.MetalLookAndFeel";          //default

  String os = System.getProperty("os.name").toLowerCase();
  if (os.indexOf("windows") != -1)
    laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
  else if (os.indexOf("mac") != -1) {
    //laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
    //laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
    //laf = "apple.laf.AquaLookAndFeel";
    laf = "javax.swing.plaf.metal.MetalLookAndFeel";

    //com.jgoodies.plaf.plastic.Plastic3DLookAndFeel.setMyCurrentTheme(new com.jgoodies.plaf.plastic.theme.DesertGreen());
    //laf = "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel";

    //laf = QuaquaManager.getLookAndFeelClassName();
    //System.setProperty("apple.awt.brushMetalLook","true");
    System.setProperty("apple.awt.antialiasing", "true");
    //System.setProperty("apple.awt.showGrowBox","true");     // only for aqua
  }

  try {
    UIManager.setLookAndFeel(laf);
    UIDefaults def = UIManager.getDefaults();
    def.put("Tree.font", new Font("Verdana", Font.PLAIN, 12));
  }
  catch (Exception e) {
    System.err.println("Could not enable " + laf);
  }
}

private static void setAllFonts()
{
  try {

    UIManager.put("ToolTip.background",new ColorUIResource(204,204,255));


    FontUIResource fontPlain12 = new FontUIResource("Verdana", Font.PLAIN, 12);
    FontUIResource fontBold12 = new FontUIResource("Verdana", Font.BOLD, 12);
    FontUIResource fontPlain10 = new FontUIResource("Verdana", Font.PLAIN, 10);

    UIManager.put("InternalFrame.titleFont", fontBold12);

    UIManager.put("ToolTip.font", fontPlain12);

    //UIManager.put("Button.font", fontBold12);
    UIManager.put("CheckBox.font", fontBold12);
    UIManager.put("ComboBox.font", fontPlain12); //fontBold12);
    UIManager.put("DesktopIcon.font", fontBold12);
    UIManager.put("Label.font", fontBold12);
    UIManager.put("List.font", fontPlain12); //fontBold12);
    UIManager.put("ProgressBar.font", fontBold12);
    UIManager.put("RadioButton.font", fontBold12);
    UIManager.put("Spinner.font", fontBold12);
    UIManager.put("TabbedPane.font", fontBold12);
    UIManager.put("TitledBorder.font", fontBold12);
    UIManager.put("ToggleButton.font", fontBold12);

    UIManager.put("EditorPane.font", fontPlain12);
    UIManager.put("FormattedTextField.font", fontPlain12);
    UIManager.put("PasswordField.font", fontPlain12);
    UIManager.put("Table.font", fontPlain12);        // no effect
    UIManager.put("TableHeader.font", fontPlain12);  // no effect
    UIManager.put("TextArea.font", fontPlain12);
    UIManager.put("TextField.font", fontPlain12);
    UIManager.put("TextPane.font", fontPlain12);
    UIManager.put("Tree.font", fontPlain12);

    UIManager.put("CheckBoxMenuItem.acceleratorFont", fontPlain10);
    UIManager.put("Menu.acceleratorFont", fontPlain10);
    UIManager.put("MenuItem.acceleratorFont", fontPlain10);
    UIManager.put("RadioButtonMenuItem.acceleratorFont", fontPlain10);

    UIManager.put("CheckBoxMenuItem.font", fontBold12);
    UIManager.put("Menu.font", fontBold12);
    UIManager.put("MenuBar.font", fontBold12);
    UIManager.put("MenuItem.font", fontBold12);
    UIManager.put("PopupMenu.font", fontBold12);
    UIManager.put("RadioButtonMenuItem.font", fontBold12);
    UIManager.put("ToolBar.font", fontBold12);

    //SwingUtilities.updateComponentTreeUI(f);
  }
  catch (Exception e) {
    System.err.println("error setting UI fonts " + e.getMessage());
  }

}
*/

}
