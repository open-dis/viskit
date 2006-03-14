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

import viskit.model.EvGraphNode;
import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EvGraphNodeInspectorDialog extends JDialog
{
  private JLabel handleLab; //,outputLab;
  private JTextField handleField;
  private JCheckBox outputCheck;
  private InstantiationPanel ip;
  private static EvGraphNodeInspectorDialog dialog;
  private static boolean modified = false;
  private EvGraphNode egNode;
  private Component locationComp;
  private JButton okButt, canButt;
  private enableApplyButtonListener lis;
  private JPanel  buttPan;

  public static String newName;
  public static VInstantiator newInstantiator;
  private JTextField descField;
  private JLabel descLab;

  public static boolean showDialog(JFrame f, Component comp, EvGraphNode parm)
  {
    try {
      if (dialog == null)
        dialog = new EvGraphNodeInspectorDialog(f, comp, parm);
      else
        dialog.setParams(comp, parm);
    }
    catch (ClassNotFoundException e) {
      String msg = "An object type specified in this element (probably "+parm.getType()+") was not found.\n" +
                   "Add the XML or class file defining the element to the proper list at left.";
      JOptionPane.showMessageDialog(f,msg,"Event Graph Definition Not Found",JOptionPane.ERROR_MESSAGE);
      dialog = null;
      return false; // unmodified
    }

    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  private EvGraphNodeInspectorDialog(JFrame parent, Component comp, EvGraphNode lv) throws ClassNotFoundException
  {
    super(parent, "Event Graph Inspector", true);
    this.egNode = lv;
    this.locationComp = comp;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    lis = new enableApplyButtonListener();

    JPanel content = new JPanel();
    setContentPane(content);
    content.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    handleField = new JTextField();
    Vstatics.clampHeight(handleField);
    handleLab = new JLabel("handle",JLabel.TRAILING);
    handleLab.setLabelFor(handleField);
    //outputLab = new JLabel("detailed output",JLabel.TRAILING);
    outputCheck = new JCheckBox("detailed output");

    descField = new JTextField();
    Vstatics.clampHeight(descField);
    descLab = new JLabel("description",JLabel.TRAILING);
    descLab.setLabelFor(descField);
    Vstatics.cloneSize(handleLab,descLab);    // make handle same size

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    buttPan.add(okButt);

    fillWidgets();     // put the data into the widgets

    modified = (lv == null ? true : false);     // if it's a new egNode, they can always accept defaults with no typing
    okButt.setEnabled((lv == null ? true : false));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());

    handleField.addCaretListener(lis);
    descField.addCaretListener(lis);
    outputCheck.addActionListener(lis);
  }

  public void setParams(Component c, EvGraphNode p) throws ClassNotFoundException
  {
    egNode = p;
    locationComp = c;

    fillWidgets();

    modified = (p == null ? true : false);
    okButt.setEnabled((p == null ? true : false));

    getRootPane().setDefaultButton(canButt);
    pack();
    this.setLocationRelativeTo(c);
  }

  private void fillWidgets() throws ClassNotFoundException
  {
    if (egNode != null) {
      handleField.setText(egNode.getName());
      outputCheck.setSelected(egNode.isOutputMarked());
      descField.setText(egNode.getDescription());
      //ArrayList alis = egNode.getComments();

      ip = new InstantiationPanel(this,lis,true);
      
      ip.setData(egNode.getInstantiator());
      ip.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                      "Object creation",TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION));

      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
      content.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

/*
      JPanel cont = new JPanel(new SpringLayout());
      cont.add(handleLab);
      cont.add(handleField);
      //cont.add(outputLab);
      cont.add(outputCheck);
      SpringUtilities.makeCompactGrid(cont, 1 , 3, 10, 10, 5, 5);
      content.add(cont);
*/

      JPanel bcont = new JPanel();
      bcont.setLayout(new BoxLayout(bcont,BoxLayout.X_AXIS));
      bcont.add(handleLab);
      bcont.add(Box.createHorizontalStrut(5));
      bcont.add(handleField);
      bcont.add(outputCheck);
      bcont.add(Box.createHorizontalGlue());
      content.add(bcont);

      JPanel dcont = new JPanel();
      dcont.setLayout(new BoxLayout(dcont,BoxLayout.X_AXIS));
      dcont.add(descLab);
      dcont.add(Box.createHorizontalStrut(5));
      dcont.add(descField);
      content.add(dcont);

      ip.setAlignmentX(Box.CENTER_ALIGNMENT);
      content.add(ip);
      content.add(Box.createVerticalStrut(5));
      content.add(buttPan);
      setContentPane(content);
    }
    else {
      handleField.setText("egNode name");
    }
  }

  private void unloadWidgets()
  {
    String nm = handleField.getText();
    nm = nm.replaceAll("\\s", "");
    if (egNode != null) {
      egNode.setName(nm);
      egNode.setDescription(descField.getText().trim());
      egNode.setInstantiator(ip.getData());
      egNode.setOutputMarked(outputCheck.isSelected());
    }
    else {
      newName = nm;
      newInstantiator = ip.getData();
    }
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
      if (modified) {
        unloadWidgets();
        if(checkBlankFields())
          return;
      }
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
  /**
   * Check for blank fields and return true if user wants to cancel close
   * @return true = cancel close
   */
  boolean checkBlankFields()
  {
    VInstantiator vi=null;

    if (egNode != null)
      vi = egNode.getInstantiator();
    else
      vi = newInstantiator;

    testLp:
    {
      if(handleField.getText().trim().length() <= 0)
        break testLp;
      if(!vi.isValid())
        break testLp;

      return false; // no blank fields , don't cancel close
    }   // testLp

    // Here if we found a problem
    int ret = JOptionPane.showConfirmDialog(EvGraphNodeInspectorDialog.this, "All fields must be completed. Close anyway?",
        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (ret == JOptionPane.YES_OPTION)
      return false;  // don't cancel
    else
      return true;  // cancel close
  }

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if (modified == true) {
        int ret = JOptionPane.showConfirmDialog(EvGraphNodeInspectorDialog.this, "Apply changes?",
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


