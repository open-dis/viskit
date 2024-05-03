package viskit.view;

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
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 15, 2004
 * @since 11:53:36 AM
 * @version $Id$
 */
public class ConditionalExpressionPanel extends JPanel {

    JTextArea conditionalTA;
    Edge edge;
    private JPanel conditionalPanel, ifTextPan;

    public ConditionalExpressionPanel(Edge edge, boolean schedulingType) {
        this.edge = edge;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(JComponent.CENTER_ALIGNMENT);

        conditionalPanel = new JPanel();
        conditionalPanel.setLayout(new BoxLayout(conditionalPanel, BoxLayout.Y_AXIS));
        conditionalPanel.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("Conditional Expression")));

        ifTextPan = new JPanel();
        ifTextPan.setLayout(new BoxLayout(ifTextPan, BoxLayout.X_AXIS));
        ifTextPan.add(Box.createHorizontalStrut(5));
        JLabel leftParen = new JLabel("if (");
        leftParen.setFont(leftParen.getFont().deriveFont(Font.ITALIC | Font.BOLD));
        leftParen.setAlignmentX(Box.LEFT_ALIGNMENT);
        ifTextPan.add(leftParen);
        ifTextPan.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(ifTextPan);
        conditionalPanel.add(ifTextPan);

        conditionalTA = new JTextArea(3, 25);
        conditionalTA.setText(edge.conditional);
        conditionalTA.setEditable(true);
        JScrollPane conditionalJsp = new JScrollPane(conditionalTA);
        conditionalPanel.add(conditionalJsp);

        Dimension conditionalJspDimension = conditionalJsp.getPreferredSize();
        conditionalJsp.setMinimumSize(conditionalJspDimension);

        JPanel thenTextPan = new JPanel();
        thenTextPan.setLayout(new BoxLayout(thenTextPan, BoxLayout.X_AXIS));
        thenTextPan.add(Box.createHorizontalStrut(10));
        JLabel rightParen;

        if (schedulingType) {
            rightParen = new JLabel(") then schedule target event");
        } else {
            rightParen = new JLabel(") then cancel target event");
        }
        rightParen.setFont(leftParen.getFont().deriveFont(Font.ITALIC | Font.BOLD));
        rightParen.setAlignmentX(Box.LEFT_ALIGNMENT);
        thenTextPan.add(rightParen);
        thenTextPan.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(thenTextPan);
        conditionalPanel.add(thenTextPan);

        add(conditionalPanel);

        conditionalTA.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                if (changeListener != null) {
                    changeListener.stateChanged(new ChangeEvent(conditionalTA));
                }
            }
        });
    }

    public void showConditions(boolean wh) {
        conditionalPanel.setVisible(wh);
    }

    public boolean isConditionsVisible() {
        return conditionalPanel.isVisible();
    }

    public void setText(String s) {
        conditionalTA.setText(s);
    }

    public String getText() {
        return conditionalTA.getText();
    }
    private ChangeListener changeListener;

    public void addChangeListener(ChangeListener listener) {
        this.changeListener = listener;
    }
}
