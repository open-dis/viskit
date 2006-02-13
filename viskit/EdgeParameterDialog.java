package viskit;

import viskit.model.vEdgeParameter;

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

public class EdgeParameterDialog extends JDialog
{
  private JTextField valueField;       // Text field that holds the expression
  private JLabel     typeLabel;    // static type value passed in
  //private JTextField commentField;          // Text field that holds the comment

  private static EdgeParameterDialog dialog;
  private static boolean modified = false;
  private vEdgeParameter param;
  private String type;
  private JButton okButt, canButt;

  public static String newValue; //, newType; //, newComment;

  public static boolean showDialog(JDialog d, Component comp, vEdgeParameter parm)
  {
    if(dialog == null)
      dialog = new EdgeParameterDialog(d,comp,parm);
    else
      dialog.setParams(comp,parm);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  private EdgeParameterDialog(JDialog parent, Component comp, vEdgeParameter param)
  {
    super(parent, "Edge Parameter", true);
    this.param = param;
    this.type = param.bogus!=null?param.bogus:"";

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

        JLabel valueLab = new JLabel("value");
        JLabel typeLab = new JLabel("type");
        //JLabel commLab = new JLabel("description");
        int w = maxWidth(new JComponent[]{valueLab,typeLab}); //commLab});

        valueField         = new JTextField(25);   setMaxHeight(valueField);
       // commentField       = new JTextField(25);   setMaxHeight(commentField);
        typeLabel = new JLabel("argument type"); //new JComboBox(VGlobals.instance().getTypeCBModel());
                                               //setMaxHeight(typeLabel);

        fieldsPanel.add(new OneLinePanel(typeLab,w,typeLabel));
        fieldsPanel.add(new OneLinePanel(valueLab,w,valueField));
        //fieldsPanel.add(new OneLinePanel(commLab,w,commentField));
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

    modified        = (param==null);     // if it's a new param, they can always accept defaults with no typing
    okButt.setEnabled((param==null));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(comp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt .addActionListener(new applyButtonListener());

    enableApplyButtonListener lis = new enableApplyButtonListener();
    //this.commentField.      addCaretListener(lis);
    this.valueField.   addCaretListener(lis);
    //this.typeLabel.addActionListener(lis);
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
  public void setParams(Component c, vEdgeParameter p)
  {
    param = p;
    type = p.bogus!=null?p.bogus:"";

    fillWidgets();

    modified        = (p==null);
    okButt.setEnabled((p==null));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    valueField.setText(param.getValue());
    typeLabel.setText(type);
    //this.commentField.setText(param.getComment());
  }

  private void unloadWidgets()
  {
    if(param != null) {
      param.setValue(valueField.getText().trim());
      //param.setComment(this.commentField.getText());
    }
    else {
      newValue = valueField.getText().trim();
      //newComment = commentField.getText().trim();
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
      if(modified) {
        int ret = JOptionPane.showConfirmDialog(EdgeParameterDialog.this,"Apply changes?",
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


