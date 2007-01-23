package viskit;

import edu.nps.util.BoxLayoutUtils;
import viskit.model.Edge;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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
  JTextArea condTA,jtaComments;
  Edge edge;
  private JPanel conditionalsPan,ifTextPan;
  private JScrollPane descJsp;

  public ConditionalsPanel(Edge edge)
  //=================================
  {
    this.edge = edge;
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    setAlignmentX(JComponent.CENTER_ALIGNMENT);

    conditionalsPan = new JPanel();
    conditionalsPan.setLayout(new BoxLayout(conditionalsPan,BoxLayout.Y_AXIS));
    conditionalsPan.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Conditional Expression")));

      ifTextPan = new JPanel();
      ifTextPan.setLayout(new BoxLayout(ifTextPan,BoxLayout.X_AXIS));
        ifTextPan.add(Box.createHorizontalStrut(5));
          JLabel leftParen = new JLabel("if (");
          leftParen.setFont(leftParen.getFont().deriveFont(Font.ITALIC | Font.BOLD));
          leftParen.setAlignmentX(Box.LEFT_ALIGNMENT);
         ifTextPan.add(leftParen);
         ifTextPan.add(Box.createHorizontalGlue());
       BoxLayoutUtils.clampHeight(ifTextPan);
    conditionalsPan.add(ifTextPan);

      condTA = new JTextArea(3,25);
      condTA.setText(edge.conditional);
      condTA.setEditable(true);
      JScrollPane condJsp = new JScrollPane(condTA);
    conditionalsPan.add(condJsp);

      Dimension d = condJsp.getPreferredSize();
      condJsp.setMinimumSize(d);

      JPanel thenTextPan = new JPanel();
      thenTextPan.setLayout(new BoxLayout(thenTextPan,BoxLayout.X_AXIS));
        thenTextPan.add(Box.createHorizontalStrut(10));
          JLabel rightParen = new JLabel(") then schedule/cancel target event");
          rightParen.setFont(leftParen.getFont().deriveFont(Font.ITALIC | Font.BOLD));
          rightParen.setAlignmentX(Box.LEFT_ALIGNMENT);
        thenTextPan.add(rightParen);
        thenTextPan.add(Box.createHorizontalGlue());
      BoxLayoutUtils.clampHeight(thenTextPan);
    conditionalsPan.add(thenTextPan);

    add(conditionalsPan);

    jtaComments = new JTextArea(2,25);
    descJsp = new JScrollPane(jtaComments);
    descJsp.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Description")));
    add(descJsp);

     d = descJsp.getPreferredSize();
     descJsp.setMinimumSize(d);

    jtaComments.addCaretListener(new CaretListener()
    {
      public void caretUpdate(CaretEvent e)
      {
        if(lis != null) {
          lis.stateChanged(new ChangeEvent(jtaComments));
        }
      }
    });
    condTA.addCaretListener(new CaretListener()
    {
      public void caretUpdate(CaretEvent e)
      {
        if(lis != null) {
          lis.stateChanged(new ChangeEvent(condTA));
        }
      }
    });
  }

  public void showConditions(boolean wh)
  {
    conditionalsPan.setVisible(wh);
  }

  public void showDescription(boolean wh)
  {
    descJsp.setVisible(wh);
  }

  public boolean isConditionsVisible()
  {
    return conditionalsPan.isVisible();
  }

  public boolean isDescriptionVisible()
  {
    return descJsp.isVisible();
  }

  public void setText(String s)
  {
    condTA.setText(s);
  }

  public String getText()
  {
    return condTA.getText();
  }

  public void setComment(String s)
  {
    jtaComments.setText(s);
  }

  public String getComment()
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
