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

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */

public class PclNodeInspectorDialog extends JDialog
{
  private JLabel handleLab;
  private JLabel typeLab;
  private JTextField handleField;    // Text field that holds the parameter name
  //private JLabel typeField;
  private JTextField typeField;
  private JLabel[] constrParmLabels;
  private JTextField[] constrParmFields;
  private int numParms;

  private static PclNodeInspectorDialog dialog;
  private static boolean modified = false;
  private PropChangeListenerNode pclNode;
  private Component locationComp;
  private JButton okButt, canButt;

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

    enableApplyButtonListener lis = new enableApplyButtonListener();

    JPanel content = new JPanel();
    setContentPane(content);
    content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    handleField = new JTextField();
    clampHeight(handleField);
    handleField.addCaretListener(lis);
    handleLab = new JLabel("handle",JLabel.TRAILING);
    handleLab.setLabelFor(handleField);

    typeLab = new JLabel("type",JLabel.TRAILING);
    //typeField = new JLabel("bogus",JLabel.CENTER);
    typeField = new JTextField();
    clampHeight(typeField);
    typeField.setEditable(false);
    typeLab.setLabelFor(typeField);

    constrParmFields = new JTextField[10];
    constrParmLabels = new JLabel[10];
    for (int i = 0; i < 10; i++) {
      constrParmFields[i] = new JTextField();  clampHeight(constrParmFields[i]);
      constrParmFields[i].addCaretListener(lis);
      constrParmLabels[i] = new JLabel(/*"constr. param " + */i + " (",JLabel.TRAILING);
      constrParmLabels[i].setLabelFor(constrParmFields[i]);
    }


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

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    Class[] params = null;
    if (pclNode != null) {

      try {
        Class c = Class.forName(pclNode.getType());
        Constructor[] cons = c.getConstructors();
        params = cons[0].getParameterTypes();
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      handleField.setText(pclNode.getName());
      typeField.setText(pclNode.getType());
      numParms = params.length;

      for(int i=0;i<numParms;i++) {
        String paramLab = constrParmLabels[i].getText();
        int paren = paramLab.lastIndexOf('(');
        String parmTyp = params[i].getName();
        parmTyp = parmTyp.substring(parmTyp.lastIndexOf('.')+1);
        constrParmLabels[i].setText(paramLab.substring(0, paren+1) + parmTyp + ")");
      }
      //todo resolve ... currently only one param in XML def.
      constrParmFields[0].setText(pclNode.getParamValue());

      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));

      JPanel cont = new JPanel(new SpringLayout());
      cont.add(handleLab);
      cont.add(handleField);
      cont.add(typeLab);
      cont.add(typeField);
      SpringUtilities.makeCompactGrid(cont, 2 , 2, 10, 10, 5, 5);
      content.add(cont);

      cont = new JPanel(new SpringLayout());
      cont.setBorder(BorderFactory.createTitledBorder("Constructor parameters"));
      for (int i = 0; i < numParms; i++) {
        cont.add(this.constrParmLabels[i]);
        cont.add(this.constrParmFields[i]);
      }
      // This potentially puts more
      SpringUtilities.makeCompactGrid(cont, numParms , 2, 10, 10, 5, 5);
      content.add(cont);
      //content.add(Box.createVerticalStrut(5));
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
      for (int i = 0; i < numParms; i++) {
        pclNode.setParamValue(constrParmFields[i].getText().trim());
      }
    }
    else {
      newName = nm;
      newConstrValue = constrParmFields[0].getText().trim();
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

  void clampHeight(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
    comp.setMinimumSize(new Dimension(Integer.MAX_VALUE,d.height));
  }
}


