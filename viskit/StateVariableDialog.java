package viskit;

import viskit.model.vStateVariable;
import viskit.model.ViskitModel;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 */

public class StateVariableDialog extends ViskitSmallDialog
{
  private JTextField stateVarNameField;    // Text field that holds the parameter name
  private JTextField commentField;          // Text field that holds the comment
  private JTextField arraySizeField;
  private JComboBox  stateVarTypeCombo;    // Editable combo box that lets us select a type
  private JLabel     arrSizeLab;

  private vStateVariable stVar;
  private Component      locationComp;
  private JButton        okButt, canButt;
  static private int     count = 0;

  public static String newName, newType, newComment;

  private myFocusListener focList;
  private Component       myTyperComponent;       // i.e., the editor of the type JComboBox
  private EventGraphViewFrame parent;             // todo fix this, this class need ref to method in model

  public static boolean showDialog(JFrame f, Component comp, vStateVariable var)
  {
    return ViskitSmallDialog.showDialog("StateVariableDialog",f,comp,var);
  }

  protected StateVariableDialog(JFrame parent, Component comp, Object param)
  {
    super(parent, "State Variable Declaration", true);
    this.parent = (EventGraphViewFrame)parent;

    focList = new myFocusListener();

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

        stateVarNameField = new JTextField(15);
        stateVarNameField.addFocusListener(focList);
        setMaxHeight(stateVarNameField);

        commentField = new JTextField(25);
        commentField.addFocusListener(focList);
        setMaxHeight(commentField);

        stateVarTypeCombo = VGlobals.instance().getTypeCB();
        stateVarTypeCombo.getEditor().getEditorComponent().addFocusListener(focList);
        setMaxHeight(stateVarTypeCombo);

        arraySizeField    = new JTextField(15);
        arraySizeField.addFocusListener(focList);

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
    if(okButt.isEnabled())
      getRootPane().setDefaultButton(okButt);
    else
      getRootPane().setDefaultButton(canButt);

    arrSizeLab.setEnabled(false);
    arraySizeField.setEnabled(false);
    arraySizeField.setEditable(false);     // grays background if false

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt .addActionListener(new StateVarApplyButtonListener());//applyButtonListener());

    enableApplyButtonListener lis = new enableApplyButtonListener(okButt);
    stateVarNameField.getDocument().addDocumentListener(lis);//addCaretListener(lis);
    commentField.     getDocument().addDocumentListener(lis);// addCaretListener(lis);
    stateVarTypeCombo.addActionListener(lis);

    myTyperComponent = stateVarTypeCombo.getEditor().getEditorComponent();

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
    if(p == null)
      getRootPane().setDefaultButton(okButt);
    else
      getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private String stripArraySize(String typ)
  {
    Pattern p = Pattern.compile("\\[.*?\\]");
    Matcher m = p.matcher(typ);
    return m.replaceAll("[]");
  }

  private String getArraySize(String typ)
  {
    Pattern p = Pattern.compile("\\[.*?\\]");
    Matcher m = p.matcher(typ);
    if(m.find()) {
      String f = m.group();
      return f.substring(1,f.length()-1);
    }
    else
      return "";

  }
  private void fillWidgets()
  {
    if(stVar != null) {
      stateVarNameField.setText(stVar.getName());
      String ty = stVar.getType();
      stateVarTypeCombo.setSelectedItem(stripArraySize(ty));
      arraySizeField.setText(getArraySize(ty));
      commentField.setText(stVar.getComment());
      boolean isArray = stVar.getType().indexOf('[') != -1;
      arraySizeField.setEditable(isArray);   // grays background if false
      arraySizeField.setEnabled(isArray);
      arrSizeLab.setEnabled(isArray);
    }
    else {
      stateVarNameField.setText(((ViskitModel)parent.getModel()).generateStateVariableName()); //"state_"+count++);
      String ty = (String)stateVarTypeCombo.getSelectedItem();
      boolean isArray = ty.indexOf('[') != -1;
      commentField.setText("");
      arraySizeField.setText("");
      arraySizeField.setEditable(isArray); // grays out background
      arraySizeField.setEnabled(isArray);
      arrSizeLab.setEnabled(isArray);
    }
    stateVarNameField.requestFocus();
    stateVarNameField.selectAll();
  }


  void unloadWidgets()
  {
    // make sure there are no spaces
    String ty = (String)stateVarTypeCombo.getSelectedItem();
    ty = VGlobals.instance().typeChosen(ty);
    if(isArray(ty)) {
      ty=ty.substring(0,ty.indexOf('[')+1) + arraySizeField.getText().trim() + "]";
    }
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

  private boolean isArray(String s)
  {
    return s.indexOf('[') == -1 ? false:true;
  }

  private boolean isGoodArray(String s)
  {
    s = s.trim();
    int brkIdx = s.indexOf('[');
    if(brkIdx == -1)
      return true;

    Pattern p = Pattern.compile(".+\\[\\s*\\](^\\[\\]){0}$");     // blah[whitsp]<eol>
    Matcher m = p.matcher(s);
    return m.matches();
  }
  // Little classes to move the focus around
  private sizeFieldFocus       sizeFocus = new sizeFieldFocus();
  private commentFieldFocus commentFocus = new commentFieldFocus();

  class sizeFieldFocus implements Runnable
  {
    public void run()
    {
      arraySizeField.requestFocus();
    }
  }
  class commentFieldFocus implements Runnable
  {
    public void run()
    {
      commentField.requestFocus();
    }
  }

  class myFocusListener extends FocusAdapter
  {
    public void focusGained(FocusEvent e)
    {
      handleSelect(e.getComponent());

      if (e.getOppositeComponent() == myTyperComponent)
        handleArrayFieldEnable();
    }

    /**
     *  Enable the array size field if the type is an array, and set the focus to the right guy.
     */
    private void handleArrayFieldEnable()
    {
      String s = (String) stateVarTypeCombo.getEditor().getItem();
      boolean isAr = isArray(s);
      arraySizeField.setEditable(isAr); // grays background if false
      arraySizeField.setEnabled(isAr);
      arrSizeLab.setEnabled(isAr);

      // Do this this way to shake out all the pending focus events before twiddling focus.
      if (isAr)
        SwingUtilities.invokeLater(sizeFocus);
      else
        SwingUtilities.invokeLater(commentFocus);
    }

    /**
     * select the text in whatever comes in
     * @param c
     */
    private void handleSelect(Component c)
    {
      if (c instanceof ComboBoxEditor)
        c = ((ComboBoxEditor) c).getEditorComponent();

      if (c instanceof JTextComponent)
        ((JTextComponent) c).selectAll();
      else if (c instanceof ComboBoxEditor)
        ((ComboBoxEditor) c).selectAll();
      else if (c instanceof JButton)
        ;
    }
  }

  class StateVarApplyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      if (modified) {
        String typ = ((String) stateVarTypeCombo.getSelectedItem()).trim();
        String nam = stateVarNameField.getText().trim();
        String arsz = arraySizeField.getText().trim();

        if (nam.length() <= 0 ||
            typ.length() <= 0 ||
            (isArray(typ) && arsz.length() <= 0)) {
          JOptionPane.showMessageDialog(StateVariableDialog.this, "Name, type and (if array) array size must be entered.",
              "Data entry error", JOptionPane.ERROR_MESSAGE);
          arrSizeLab.setEnabled(true);
          arraySizeField.setEnabled(true);
          arraySizeField.setEditable(true);
          arraySizeField.requestFocus();
          return;
        }
        else if (isArray(typ) && !isGoodArray(typ)) {
          JOptionPane.showMessageDialog(StateVariableDialog.this, "Use a single trailing pair of empty square brackets\nto signify a one-dimensional array.",
              "Data entry error", JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Do a beanshell test for array declaration
        if (!VGlobals.instance().isPrimitive(typ) && isArray(typ)) {         // isPrimitive returns false for arrays

          String s = typ + " " + nam + " = new " + typ;
          s = s.substring(0, s.lastIndexOf('[') + 1) + arsz + "]";          // stick in size

          if(ViskitConfig.instance().getVal("app.beanshell.warning").equalsIgnoreCase("true")) {
            String result = VGlobals.instance().parseCode(null, s);
            if (result != null) {
              boolean ret = BeanshellErrorDialog.showDialog(result,StateVariableDialog.this);
              if(ret == false) // don't ignore
                return;
/*
              int ret = JOptionPane.showConfirmDialog(StateVariableDialog.this, "Java language error:\n" + result + "\nIgnore and continue?",
                  "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
              if (ret != JOptionPane.YES_OPTION)
                return;
*/
            }
          }
        }
        // ok, we passed
        unloadWidgets();

      }
      setVisible(false);
    }
  }
}
