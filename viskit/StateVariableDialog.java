package viskit;

import viskit.model.vStateVariable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A dialog class that lets the user add a new state variable to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */
 
public class StateVariableDialog extends ViskitSmallDialog
{
  private JTextField stateVarNameField;    // Text field that holds the parameter name
  private JTextField commentField;          // Text field that holds the comment
  private JTextField arraySizeField;
  private JComboBox  stateVarTypeCombo;    // Editable combo box that lets us select a type
  private JLabel     arrSizeLab;

  private vStateVariable stVar;
  private Component locationComp;
  private JButton okButt, canButt;
  static private int count = 0;

  public static String newName, newType, newComment;

  public static boolean showDialog(JFrame f, Component comp, vStateVariable var)
  {
    return ViskitSmallDialog.showDialog("StateVariableDialog",f,comp,var);
  }

  protected StateVariableDialog(JFrame parent, Component comp, Object param)
  {
    super(parent, "State Variable Inspector", true);
    this.stVar = (vStateVariable)param;
    this.locationComp = comp;

    Container cont = getContentPane();
    cont.setLayout(new BoxLayout(cont,BoxLayout.Y_AXIS));

     JPanel con = new JPanel();
     con.setLayout(new BoxLayout(con,BoxLayout.Y_AXIS));
     con.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

      con.add(Box.createVerticalStrut(5));
      JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel,BoxLayout.Y_AXIS));

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel commLab = new JLabel("comment");
            arrSizeLab = new JLabel("array size");

        int w = maxWidth(new JComponent[]{nameLab,initLab,typeLab,commLab});

        stateVarNameField = new JTextField(15);   setMaxHeight(stateVarNameField);
        commentField       = new JTextField(25);   setMaxHeight(commentField);
        stateVarTypeCombo = VGlobals.instance().getTypeCB(); setMaxHeight(stateVarTypeCombo);
        arraySizeField    = new JTextField(15);

        fieldsPanel.add(new OneLinePanel(nameLab,w,stateVarNameField));
        fieldsPanel.add(new OneLinePanel(typeLab,w,stateVarTypeCombo));
        fieldsPanel.add(new OneLinePanel(arrSizeLab,w,arraySizeField));
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

    modified        = (param==null?true:false);     // if it's a new stVar, they can always accept defaults with no typing
    okButt.setEnabled((param==null?true:false));
    arrSizeLab.setEnabled(false);
    arraySizeField.setEditable(false);
    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt .addActionListener(new applyButtonListener());

    enableApplyButtonListener lis = new enableApplyButtonListener(okButt);
    stateVarNameField.addCaretListener(lis);
    commentField.      addCaretListener(lis);
    stateVarTypeCombo.addActionListener(lis);
    // also, check for array
    stateVarTypeCombo.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        boolean isArray = ((String)stateVarTypeCombo.getSelectedItem()).indexOf('[') != -1;
        arraySizeField.setEditable(isArray);
        arrSizeLab.setEnabled(isArray);
      }
    });

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowClosingListener(this,okButt,canButt));
  }

  void setParams(Component c, Object p)
  {
    stVar = (vStateVariable)p;
    locationComp = c;

    fillWidgets();

    modified        = (p==null?true:false);
    okButt.setEnabled((p==null?true:false));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(stVar != null) {
      stateVarNameField.setText(stVar.getName());
      stateVarTypeCombo.setSelectedItem(stVar.getType());
      //setType(stVar.getType(),stateVarTypeCombo);
      commentField.setText(stVar.getComment());
      boolean isArray = stVar.getType().indexOf('[') != -1;
      arraySizeField.setEditable(isArray);
      arrSizeLab.setEnabled(isArray);
    }
    else {
      stateVarNameField.setText("state_"+count++);
      commentField.setText("");
      arraySizeField.setEditable(false);
      arrSizeLab.setEnabled(false);
    }
    stateVarNameField.requestFocus();
    stateVarNameField.selectAll();
  }


  void unloadWidgets()
  {
    // make sure there are no spaces
    String ty = (String)stateVarTypeCombo.getSelectedItem();
    ty = VGlobals.instance().typeChosen(ty);
    String nm = stateVarNameField.getText();
    nm = nm.replaceAll("\\s","");

    if(stVar != null) {
      stVar.setName(nm);
      stVar.setType(ty);
      stVar.setComment(this.commentField.getText().trim());
    }
    else {
      newName = nm;
      newType = ty;
      newComment = commentField.getText().trim();
    }
  }
}
