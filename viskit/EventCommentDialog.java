package viskit;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 20, 2004
 * Time: 2:51:29 PM
 */

public class EventCommentDialog extends JDialog
{
  private JTextArea commentArea;          // Text field that holds the comment

  private static EventCommentDialog dialog;
  private static boolean modified = false;
  private Component locationComp;
  private JButton okButt, canButt;

  public static StringBuffer newComment;
  public StringBuffer param;

  public static boolean showDialog(JFrame f, Component comp, StringBuffer parm)
  {
    if(dialog == null)
      dialog = new EventCommentDialog(f,comp,parm);
    else
      dialog.setParams(comp,parm);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  private EventCommentDialog(JFrame parent, Component comp, StringBuffer param)
  {
    super(parent, "Event Description", true);
    this.locationComp = comp;
    this.param = param;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    JPanel cont = new JPanel();
    setContentPane(cont);

    cont.setLayout(new BoxLayout(cont,BoxLayout.Y_AXIS));
    cont.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    commentArea = new JTextArea(10,40);
    commentArea.setLineWrap(true);
    commentArea.setWrapStyleWord(true);
    
    JScrollPane jsp = new JScrollPane(commentArea);
    cont.add(jsp);
       cont.add(Box.createVerticalStrut(5));

       JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
        canButt = new JButton("Cancel");
        okButt = new JButton("Apply changes");
        buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
        buttPan.add(canButt);
        buttPan.add(okButt);
       cont.add(buttPan);

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
    this.commentArea.      addCaretListener(lis);
  }

  public void setParams(Component c, StringBuffer p)
  {
    param = p;
    locationComp = c;

    fillWidgets();

    modified        = (p.length()==0?true:false);
    okButt.setEnabled((p.length()==0?true:false));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(param != null)
      commentArea.setText(param.toString());
    else
      commentArea.setText("");
  }

  private void unloadWidgets()
  {
    if(param != null) {
      param.setLength(0);
      param.append(commentArea.getText().trim());
    }
    else {
      newComment = new StringBuffer(commentArea.getText().trim());
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

/*
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
*/

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if(modified == true) {
        int ret = JOptionPane.showConfirmDialog(EventCommentDialog.this,"Apply changes?",
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


