package viskit;

import viskit.model.Edge;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 15, 2004
 * Time: 11:53:36 AM
 */

public class ConditionalsPanel extends JPanel
/*******************************************/
{
  JTextArea jta,jtaComments;
  Edge edge;

  public ConditionalsPanel(Edge edge)
  //=================================
  {
    this.edge = edge;
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    setBorder(BorderFactory.createTitledBorder("Conditional Expression"));

    JPanel tp = new JPanel();
     tp.setLayout(new BoxLayout(tp,BoxLayout.X_AXIS));
     tp.add(Box.createHorizontalStrut(5));
      JLabel leftParen = new JLabel("if (");
      leftParen.setFont(leftParen.getFont().deriveFont(Font.ITALIC | Font.BOLD));
      leftParen.setAlignmentX(Box.LEFT_ALIGNMENT);
     tp.add(leftParen);
     tp.add(Box.createHorizontalGlue());
    add(tp);

     jta = new JTextArea(3,25);
     jta.setText(edge.conditional);
     jta.setEditable(true);
     JScrollPane jsp = new JScrollPane(jta);
    add(jsp);
     Dimension d = jsp.getPreferredSize();
     jsp.setMinimumSize(d);

     JPanel bp = new JPanel();
     bp.setLayout(new BoxLayout(bp,BoxLayout.X_AXIS));
     bp.add(Box.createHorizontalStrut(10));
      JLabel rightParen = new JLabel(") then schedule/cancel target event");
      rightParen.setFont(leftParen.getFont().deriveFont(Font.ITALIC | Font.BOLD));
      rightParen.setAlignmentX(Box.LEFT_ALIGNMENT);
     bp.add(rightParen);
     bp.add(Box.createHorizontalGlue());
    add(bp);

    add(Box.createVerticalStrut(5));

     jtaComments = new JTextArea(2,25);
     jsp = new JScrollPane(jtaComments);
     jsp.setBorder(BorderFactory.createTitledBorder("Description"));
    add(jsp);
     d = jsp.getPreferredSize();
     jsp.setMinimumSize(d);

    add(Box.createVerticalStrut(5));

    // This whole panel expands horizontally, but not vertically
    d = getPreferredSize();
    d.width = Integer.MAX_VALUE;
    setMinimumSize(d);
    setMaximumSize(d);

    jtaComments.addCaretListener(new CaretListener()
    {
      public void caretUpdate(CaretEvent e)
      {
        if(lis != null) {
          lis.stateChanged(new ChangeEvent(jtaComments));
        }
      }
    });
    jta.addCaretListener(new CaretListener()
    {
      public void caretUpdate(CaretEvent e)
      {
        if(lis != null) {
          lis.stateChanged(new ChangeEvent(jta));
        }
      }
    });
  }

  public void setText(String s)
  //---------------------------
  {
    jta.setText(s);
  }

  public String getText()
  //---------------------
  {
    return jta.getText();
  }

  public void setComment(String s)
  //------------------------------
  {
    jtaComments.setText(s);
  }

  public String getComment()
  //------------------------
  {
    return jtaComments.getText().trim();
  }

  private ChangeListener lis;
  public void addChangeListener(ChangeListener lis)
  //-----------------------------------------------
  {
    this.lis = lis;
  }
}
