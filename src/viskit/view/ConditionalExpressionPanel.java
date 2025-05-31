package viskit.view;

import edu.nps.util.BoxLayoutUtils;
import viskit.model.Edge;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
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
public class ConditionalExpressionPanel extends JPanel 
{
               JTextArea conditionalExpressionTA;
    private final JPanel conditionalExpressionPanel;
    private final JPanel ifConditionalExpressionTextPanel;
    private final String CONDITIONAL_EXPRESSION_HINT = "boolean expression, which can include dstate variables from source event node";

    public ConditionalExpressionPanel(Edge edge, boolean schedulingType)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(JComponent.CENTER_ALIGNMENT);

        conditionalExpressionPanel = new JPanel();
        conditionalExpressionPanel.setLayout(new BoxLayout(conditionalExpressionPanel, BoxLayout.Y_AXIS));
        conditionalExpressionPanel.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, 5, 0),
                BorderFactory.createTitledBorder("Conditional Expression")));
        conditionalExpressionPanel.setToolTipText("Conditional expression is optional");

        ifConditionalExpressionTextPanel = new JPanel();
        ifConditionalExpressionTextPanel.setLayout(new BoxLayout(ifConditionalExpressionTextPanel, BoxLayout.X_AXIS));
        ifConditionalExpressionTextPanel.add(Box.createHorizontalStrut(5));
        JLabel leftParenLabel = new JLabel("if (");
        leftParenLabel.setFont(leftParenLabel.getFont().deriveFont(Font.ITALIC | Font.BOLD));
        leftParenLabel.setAlignmentX(Box.LEFT_ALIGNMENT);
        ifConditionalExpressionTextPanel.add(leftParenLabel);
        ifConditionalExpressionTextPanel.setToolTipText(CONDITIONAL_EXPRESSION_HINT);
        ifConditionalExpressionTextPanel.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(ifConditionalExpressionTextPanel);
        conditionalExpressionPanel.add(ifConditionalExpressionTextPanel);

        conditionalExpressionTA = new JTextArea(3, 25);
        conditionalExpressionTA.setText(edge.getConditional());
        conditionalExpressionTA.setEditable(true);
        conditionalExpressionTA.setToolTipText(CONDITIONAL_EXPRESSION_HINT);
        JScrollPane conditionalScrollPane = new JScrollPane(conditionalExpressionTA);
        conditionalExpressionPanel.add(conditionalScrollPane);

        Dimension conditionalJspDimension = conditionalScrollPane.getPreferredSize();
        conditionalScrollPane.setMinimumSize(conditionalJspDimension);

        JPanel thenTextPanel = new JPanel();
        thenTextPanel.setLayout(new BoxLayout(thenTextPanel, BoxLayout.X_AXIS));
        thenTextPanel.add(Box.createHorizontalStrut(10));
        JLabel rightParen;

        if (schedulingType) {
            rightParen = new JLabel(") then schedule target event");
        } else {
            rightParen = new JLabel(") then cancel target event");
        }
        rightParen.setFont(leftParenLabel.getFont().deriveFont(Font.ITALIC | Font.BOLD));
        rightParen.setAlignmentX(Box.LEFT_ALIGNMENT);
        rightParen.setToolTipText("conditional expression must be met prior to sending this event");
        thenTextPanel.add(rightParen);
        thenTextPanel.add(Box.createHorizontalGlue());
        BoxLayoutUtils.clampHeight(thenTextPanel);
        conditionalExpressionPanel.add(thenTextPanel);

        add(conditionalExpressionPanel);

        conditionalExpressionTA.addCaretListener((CaretEvent e) -> {
            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(conditionalExpressionTA));
            }
        });
    }

    public void setPanelVisible(boolean displayVisible) {
        conditionalExpressionPanel.setVisible(displayVisible);
    }

    public boolean isPanelVisible() {
        return conditionalExpressionPanel.isVisible();
    }

    public void setText(String conditionalExpressionSourceText) {
        conditionalExpressionTA.setText(conditionalExpressionSourceText);
    }

    public String getText() {
        return conditionalExpressionTA.getText();
    }
    private ChangeListener changeListener;

    public void addChangeListener(ChangeListener listener) {
        this.changeListener = listener;
    }
}
