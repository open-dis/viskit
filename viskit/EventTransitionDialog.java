package viskit;

import viskit.model.EventStateTransition;
import viskit.model.vStateVariable;
import viskit.model.ViskitModel;

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
 * @author DMcG, Mike Bailey
 */

public class EventTransitionDialog extends JDialog
{
  //private JTextField nameField;    // Text field that holds the parameter name
  private JTextField actionField;
  private JTextField arrayIndexField;
  private JTextField commentField;          // Text field that holds the comment
  private JComboBox  stateVarsCB;
  private JRadioButton assTo,opOn;

  private static EventTransitionDialog dialog;
  private static boolean modified = false;
  private EventStateTransition param;
  private Component locationComp;
  private JButton okButt, canButt;
  private JButton newSVButt;
  private JLabel actionLab;
  private JPanel indexPanel;

  public static String newStateVarName, newStateVarType, newIndexExpression, newAction, newComment;
  public static boolean newIsOperation;

  private EventGraphViewFrame parentFrame;
  public static boolean showDialog(EventGraphViewFrame f, Component comp, EventStateTransition parm)
  {
    if(dialog == null)
      dialog = new EventTransitionDialog(f,comp,parm);
    else
      dialog.setParams(comp,parm);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  private EventTransitionDialog(EventGraphViewFrame parent, Component comp, EventStateTransition param)
  {
    super(parent, "State Transition", true);
    this.param = param;
    this.locationComp = comp;
    this.parentFrame = parent;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    Container cont = getContentPane();
    cont.setLayout(new BoxLayout(cont,BoxLayout.Y_AXIS));

      JPanel con = new JPanel();
      con.setLayout(new BoxLayout(con,BoxLayout.Y_AXIS));
      con.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

      con.add(Box.createVerticalStrut(5));
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel,BoxLayout.Y_AXIS));

          JLabel nameLab = new JLabel("state variable");
          JLabel arrayIdxLab = new JLabel("index expression");

          assTo = new JRadioButton("assign to (\"=\")");
          opOn  = new JRadioButton("invoke on (\".\")");
          ButtonGroup bg = new ButtonGroup();
            bg.add(assTo);bg.add(opOn);

          actionLab = new JLabel("invoke");
          Dimension dx = actionLab.getPreferredSize();
          actionLab.setText("=");
          actionLab.setPreferredSize(dx);
          actionLab.setHorizontalAlignment(JLabel.TRAILING);

          JLabel commLab = new JLabel("state var. comment");
          int w = maxWidth(new JComponent[]{nameLab,assTo,opOn,actionLab,commLab});

          //nameField = new JTextField(15);   setMaxHeight(nameField);

          stateVarsCB = new JComboBox(VGlobals.instance().getStateVarsCBModel());
            setMaxHeight(stateVarsCB);
            stateVarsCB.setBackground(Color.white);
          newSVButt = new JButton("new");
          commentField       = new JTextField(25);   setMaxHeight(commentField);
            commentField.setEditable(false);
          actionField        = new JTextField(25);   setMaxHeight(actionField);
          arrayIndexField    = new JTextField(25);   setMaxHeight(arrayIndexField);

        fieldsPanel.add(new OneLinePanel(nameLab,w,stateVarsCB,newSVButt));
        fieldsPanel.add(indexPanel = new OneLinePanel(arrayIdxLab,w,arrayIndexField));
        fieldsPanel.add(new OneLinePanel(null,w,assTo));
        fieldsPanel.add(new OneLinePanel(null,w,opOn));
        fieldsPanel.add(new OneLinePanel(actionLab,w,actionField));
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(new OneLinePanel(commLab,w,commentField));

      con.add(fieldsPanel);
      con.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
          canButt = new JButton("Cancel");
          okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
      con.add(buttPan);
      con.add(Box.createVerticalGlue());    // takes up space when dialog is expanded vertically
      cont.add(con);

    fillWidgets();     // put the data into the widgets

    modified        = (param==null?true:false);     // if it's a new param, they can always accept defaults with no typing
    okButt.setEnabled((param==null?true:false));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt .addActionListener(new applyButtonListener());

    enableApplyButtonListener lis = new enableApplyButtonListener();
    //nameField.addCaretListener(lis);
    arrayIndexField.addCaretListener(lis);
    actionField.addCaretListener(lis);
    commentField.      addCaretListener(lis);
    stateVarsCB.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        JComboBox cb = (JComboBox)e.getSource();
        vStateVariable sv = (vStateVariable)cb.getSelectedItem();
        commentField.setText(sv.getComment());
        okButt.setEnabled(true);
        arrayIndexField.setEditable(false); //sv.getType().indexOf('[') != -1);
        indexPanel.setVisible(sv.getType().indexOf('[')!= -1);
        modified=true;
        pack();
      }
    });
    newSVButt.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        String nm = ((EventGraphViewFrame)parentFrame).addStateVariableDialog();
        if(nm != null) {
          stateVarsCB.setModel(VGlobals.instance().getStateVarsCBModel());
          for(int i=0;i<stateVarsCB.getItemCount();i++) {
            vStateVariable vsv = (vStateVariable)stateVarsCB.getItemAt(i);
            if(vsv.getName().indexOf(nm) != -1) {
              stateVarsCB.setSelectedIndex(i);
              break;
            }
          }
        }
      }
    });

    // to start off:
    if(stateVarsCB.getItemCount() > 0)
      commentField.setText(((vStateVariable)stateVarsCB.getItemAt(0)).getComment());
    else
      commentField.setText("");
    
    RadButtListener rbl = new RadButtListener();
    this.assTo.addActionListener(rbl);
    this.opOn.addActionListener(rbl);
  }

  private int maxWidth(JComponent[] c)
  {
    int tmpw=0,maxw=0;
    for(int j=0; j<c.length; j++) {
      tmpw = c[j].getPreferredSize().width;
      if(tmpw > maxw)
        maxw = tmpw;
    }
    return maxw;
  }
  private void setMaxHeight(JComponent c)
  {
    Dimension d = c.getPreferredSize();
    d.width = Integer.MAX_VALUE;
    c.setMaximumSize(d);
  }
  public void setParams(Component c, EventStateTransition p)
  {
    param = p;
    locationComp = c;

    fillWidgets();

    modified        = (p==null?true:false);
    okButt.setEnabled((p==null?true:false));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(param != null) {
      //nameField.setText(param.getStateVarName());//getName());
      actionField.setText(param.getOperationOrAssignment());
      String ie = param.getIndexingExpression();
      if(ie == null || ie.equals(""))
        arrayIndexField.setText(((ViskitModel)parentFrame.getModel()).generateLocalVariableName());
      else
        arrayIndexField.setText(param.getIndexingExpression());
      opOn.setSelected(param.isOperation());
      assTo.setSelected(!param.isOperation());

      setVarNameComboBox(param);
      if(stateVarsCB.getItemCount() > 0)
        commentField.setText(((vStateVariable)stateVarsCB.getItemAt(0)).getComment());
      else
        commentField.setText("");
      vStateVariable vsv = (vStateVariable)stateVarsCB.getSelectedItem();
      arrayIndexField.setEditable(false); //vsv.getType().indexOf('[') != -1);
    }
    else {
      //nameField.setText("");
      actionField.setText("");
      arrayIndexField.setText( ((ViskitModel)parentFrame.getModel()).generateLocalVariableName());
      arrayIndexField.setEditable(false);
      stateVarsCB.setSelectedIndex(0);
      commentField.setText("");
      assTo.setSelected(true);
    }
    indexPanel.setVisible(((vStateVariable)stateVarsCB.getItemAt(0)).getType().indexOf('[')!= -1);
    pack();
  }

  private void setVarNameComboBox(EventStateTransition est)
  {
    stateVarsCB.setModel(VGlobals.instance().getStateVarsCBModel());
    stateVarsCB.setSelectedIndex(0);
    for (int i = 0; i < stateVarsCB.getItemCount(); i++) {
      vStateVariable sv = (vStateVariable) stateVarsCB.getItemAt(i);
      if (est.getStateVarName().equalsIgnoreCase(sv.getName())) {
        stateVarsCB.setSelectedIndex(i);
        return;
      }
    }

    if (!est.getStateVarName().equals(""))     // for first time
      JOptionPane.showMessageDialog(this, "State variable " + est.getStateVarName() + "not found.",
          "alert", JOptionPane.ERROR_MESSAGE);
  }

  private void unloadWidgets()
  {
    if(param != null) {
      param.setStateVarName(((vStateVariable)stateVarsCB.getSelectedItem()).getName());
      param.setStateVarType(((vStateVariable)stateVarsCB.getSelectedItem()).getType());
      param.setIndexingExpression(arrayIndexField.getText().trim());
      param.setOperationOrAssignment(actionField.getText().trim());
      param.setOperation(opOn.isSelected());
      param.getComments().clear();
      String cs = commentField.getText().trim();
      if(cs.length() > 0)
        param.getComments().add(0,cs);
    }
    else {
      newStateVarName    = ((vStateVariable)stateVarsCB.getSelectedItem()).getName();
      newStateVarType    = ((vStateVariable)stateVarsCB.getSelectedItem()).getType();
      newIndexExpression = arrayIndexField.getText().trim();
      newAction          = actionField.getText().trim();
      newIsOperation     = opOn.isSelected();
      newComment         = commentField.getText().trim();
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
  class RadButtListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      modified = true;
      okButt.setEnabled(true);

      Dimension d = actionLab.getPreferredSize();
      if(assTo.isSelected())
        actionLab.setText("=");
      else {
        String ty = ((vStateVariable)stateVarsCB.getSelectedItem()).getType();
        if(VGlobals.instance().isPrimitive(ty)) {
          JOptionPane.showMessageDialog(EventTransitionDialog.this,"A method may not be invoked on a primitive type.",
                                        "Java Language Error",JOptionPane.ERROR_MESSAGE);
          assTo.setSelected(true);
        }
        else
          actionLab.setText(".");
      }
      actionLab.setPreferredSize(d);
    }
  }
  class applyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      if(modified) {
        // check for array index
        if(((vStateVariable)stateVarsCB.getSelectedItem()).getType().indexOf('[') != -1) {
          if(arrayIndexField.getText().trim().length() <= 0) {
            int ret = JOptionPane.showConfirmDialog(EventTransitionDialog.this, "Using a state variable which is an array"+
                      "\nrequires an indexing expression.\nIgnore and continue?",
                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret != JOptionPane.YES_OPTION)
              return;
          }
        }
        // check for null action
        if(actionField.getText().trim().length() <= 0) {
          int ret = JOptionPane.showConfirmDialog(EventTransitionDialog.this, "No transition (action) has been entered."+
                    "\nIgnore and continue?",
              "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
          if (ret != JOptionPane.YES_OPTION)
            return;
        }
        unloadWidgets();
      }
      setVisible(false);
    }
  }

  class enableApplyButtonListener implements CaretListener,ActionListener
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
      setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
      if(lab == null) lab = new JLabel("");
      add(Box.createHorizontalStrut(5));
      add(Box.createHorizontalStrut(w-lab.getPreferredSize().width));
      add(lab);
      add(Box.createHorizontalStrut(5));
      add(comp);

      Dimension d = getPreferredSize();
      d.width = Integer.MAX_VALUE;
      setMaximumSize(d);
    }
    OneLinePanel(JLabel lab, int w, JComponent comp1, JComponent comp2)
    {
      setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
      if(lab == null) lab = new JLabel("");
      add(Box.createHorizontalStrut(5));
      add(Box.createHorizontalStrut(w-lab.getPreferredSize().width));
      add(lab);
      add(Box.createHorizontalStrut(5));
      add(comp1);
      add(comp2);

      Dimension d = getPreferredSize();
      d.width = Integer.MAX_VALUE;
      setMaximumSize(d);
    }
  }

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if(modified == true) {
        int ret = JOptionPane.showConfirmDialog(EventTransitionDialog.this,"Apply changes?",
            "Question",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(ret == JOptionPane.YES_OPTION)
          okButt.doClick();
        else
          canButt.doClick();
        }
      else
        canButt.doClick();
    }
  }


}


