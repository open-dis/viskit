package viskit;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: June 2, 2004
 * Time: 9:19:41 AM
 */

import viskit.model.PropChangeListenerNode;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;


public class PclNodeInspectorDialog extends JDialog
{
  private JLabel handleLab;
  private JLabel typeLab;
  private JTextField handleField;    // Text field that holds the parameter name
  private JTextField typeField;
  private InstantiationPanel ip;
  private Class myClass;
  private static PclNodeInspectorDialog dialog;
  private static boolean modified = false;
  private PropChangeListenerNode pclNode;
  private Component locationComp;
  private JButton okButt, canButt;
  private enableApplyButtonListener lis;
  JPanel  buttPan;

  public static String newName, newConstrValue;

  public static boolean showDialog(JFrame f, Component comp, PropChangeListenerNode parm)
  {
    if (dialog == null)
      dialog = new PclNodeInspectorDialog(f, comp, parm);
    else
      dialog.setParams(comp, parm);

    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  private PclNodeInspectorDialog(JFrame parent, Component comp, PropChangeListenerNode lv)
  {
    super(parent, "Property Change Listener", true);
    this.pclNode = lv;
    this.locationComp = comp;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    lis = new enableApplyButtonListener();

    JPanel content = new JPanel();
    setContentPane(content);
    content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    handleField = new JTextField();
    Vstatics.clampHeight(handleField);
    handleField.addCaretListener(lis);
    handleLab = new JLabel("handle",JLabel.TRAILING);
    handleLab.setLabelFor(handleField);

    typeLab = new JLabel("type",JLabel.TRAILING);
    //typeField = new JLabel("bogus",JLabel.CENTER);
    typeField = new JTextField();
    Vstatics.clampHeight(typeField);
    typeField.setEditable(false);
    typeLab.setLabelFor(typeField);

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    buttPan.add(okButt);

    fillWidgets();     // put the data into the widgets

    modified = (lv == null ? true : false);     // if it's a new pclNode, they can always accept defaults with no typing
    okButt.setEnabled((lv == null ? true : false));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());
  }

  public void setParams(Component c, PropChangeListenerNode p)
  {
    pclNode = p;
    locationComp = c;

    fillWidgets();

    modified = (p == null ? true : false);
    okButt.setEnabled((p == null ? true : false));

    getRootPane().setDefaultButton(canButt);
    pack();
    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if (pclNode != null) {

      try {
        myClass = Class.forName(pclNode.getType());
        Constructor[] cons = myClass.getConstructors();
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      handleField.setText(pclNode.getName());
      typeField.setText(pclNode.getType());

      ip = new InstantiationPanel(lis);
      setupIP();
      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
      content.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      JPanel cont = new JPanel(new SpringLayout());
      cont.add(handleLab);
      cont.add(handleField);
      cont.add(typeLab);
      cont.add(typeField);
      SpringUtilities.makeCompactGrid(cont, 2 , 2, 10, 10, 5, 5);
      content.add(cont);

      content.add(ip);
      content.add(Box.createVerticalStrut(5));
      content.add(buttPan);
      setContentPane(content);
    }
    else {
      handleField.setText("pclNode name");
      //commentField.setText("comments here");
    }
  }

  private void unloadWidgets()
  {
    String nm = handleField.getText();
    nm = nm.replaceAll("\\s", "");
    if (pclNode != null) {
      pclNode.setName(nm);
      pclNode.setInstantiator(ip.getData());
    }
    else {
      newName = nm;
   //   newConstrValue = constrParmFields[0].getText().trim();
    }
  }

  /**
   * Initialize the InstantiationsPanel with the data from the pclnode
   */
  private void setupIP()
  {
    ip.setData(pclNode.getInstantiator());
  }
  class cancelButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      modified = false;    // for the caller
      setVisible(false);
    }
  }

  class applyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      if (modified)
        unloadWidgets();
      setVisible(false);
    }
  }

  class enableApplyButtonListener implements CaretListener, ActionListener
  {
    public void caretUpdate(CaretEvent event)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }

    public void actionPerformed(ActionEvent event)
    {
      caretUpdate(null);
    }
  }

  class OneLinePanel extends JPanel
  {
    OneLinePanel(JLabel lab, int w, JComponent comp)
    {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      add(Box.createHorizontalStrut(5));
      add(Box.createHorizontalStrut(w - lab.getPreferredSize().width));
      add(lab);
      add(Box.createHorizontalStrut(5));
      add(comp);

      Dimension d = getPreferredSize();
      d.width = Integer.MAX_VALUE;
      setMaximumSize(d);
    }
  }

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if (modified == true) {
        int ret = JOptionPane.showConfirmDialog(PclNodeInspectorDialog.this, "Apply changes?",
            "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION)
          okButt.doClick();
        else
          canButt.doClick();
      }
      else
        canButt.doClick();
    }
  }
}


