package viskit;

import viskit.model.EventArgument;

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
 * @author DMcG
 */

public class EventArgumentDialog extends JDialog
{
  private JTextField nameField;    // Text field that holds the parameter name
  private JTextField commentField;          // Text field that holds the comment
  private JComboBox  parameterTypeCombo;    // Editable combo box that lets us select a type

  private static EventArgumentDialog dialog;
  private static boolean modified = false;
  private EventArgument myEA;
  private Component locationComp;
  private JButton okButt, canButt;

  public static String newName, newType, newComment;

  public static boolean showDialog(JFrame f, Component comp, EventArgument parm)
  {
    if(dialog == null)
      dialog = new EventArgumentDialog(f,comp,parm);
    else
      dialog.setParams(comp,parm);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  private EventArgumentDialog(JFrame parent, Component comp, EventArgument param)
  {
    super(parent, "Event Argument", true);
    this.myEA = param;
    this.locationComp = comp;
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

        JLabel nameLab = new JLabel("name");
        JLabel initLab = new JLabel("initial value");
        JLabel typeLab = new JLabel("type");
        JLabel commLab = new JLabel("comment");
        int w = maxWidth(new JComponent[]{nameLab,initLab,typeLab,commLab});

        nameField = new JTextField(15);   setMaxHeight(nameField);
        commentField       = new JTextField(25);   setMaxHeight(commentField);
        //parameterTypeCombo = new JComboBox();
        //parameterTypeCombo.setModel(VGlobals.instance().getTypeCBModel(parameterTypeCombo));
        //                                       setMaxHeight(parameterTypeCombo);
        //parameterTypeCombo.setBackground(Color.white);
       // parameterTypeCombo.setEditable(true);
        parameterTypeCombo = VGlobals.instance().getTypeCB(); setMaxHeight(parameterTypeCombo);


        fieldsPanel.add(new OneLinePanel(nameLab,w,nameField));
        fieldsPanel.add(new OneLinePanel(typeLab,w,parameterTypeCombo));
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

    modified        = (param==null?true:false);     // if it's a new myEA, they can always accept defaults with no typing
    okButt.setEnabled((param==null?true:false));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt .addActionListener(new applyButtonListener());

    enableApplyButtonListener lis = new enableApplyButtonListener();
    this.nameField.addCaretListener(lis);
    this.commentField.      addCaretListener(lis);
    this.parameterTypeCombo.addActionListener(lis);
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
  public void setParams(Component c, EventArgument p)
  {
    myEA = p;
    locationComp = c;

    fillWidgets();

    modified        = (p==null?true:false);
    okButt.setEnabled((p==null?true:false));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(myEA != null) {
      nameField.setText(myEA.getName());
      parameterTypeCombo.setSelectedItem(myEA.getType());
      if(myEA.getComments().size() > 0)
        commentField.setText((String)myEA.getComments().get(0));
    }
    else {
      nameField.setText("");
      commentField.setText("");
    }
  }

  private void unloadWidgets()
  {
    String ty = (String)parameterTypeCombo.getSelectedItem();
    ty = VGlobals.instance().typeChosen(ty);
    String nm = nameField.getText();
    nm = nm.replaceAll("\\s","");

    if(myEA != null) {
      myEA.setName(nm);
      myEA.setType(ty);
      myEA.getComments().clear();
      String cs = commentField.getText().trim();
      if(cs.length() > 0)
        myEA.getComments().add(0,cs);
    }
    else {
      newName    = nm;
      newType    = ty;
      newComment = commentField.getText().trim();
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
      if(modified)
        unloadWidgets();
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
      add(Box.createHorizontalStrut(5));
      add(Box.createHorizontalStrut(w-lab.getPreferredSize().width));
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
      if(modified == true) {
        int ret = JOptionPane.showConfirmDialog(EventArgumentDialog.this,"Apply changes?",
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


