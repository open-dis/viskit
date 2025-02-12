package viskit.view.dialog;

import edu.nps.util.SpringUtilities;

import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.text.JTextComponent;
import viskit.ViskitStatics;
import viskit.model.EventGraphNode;
import viskit.model.SimEventListenerEdge;
import static viskit.ViskitStatics.DESCRIPTION_HINT;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since June 2, 2004
 * @since 9:19:41 AM
 * @version $Id$
 */
public class SimEventListenerConnectionInspectorDialog extends JDialog
{

    private final JLabel sourceLabel;
  private JLabel targetLabel, descriptionLabel;
    private final JTextField sourceTextField;
  private JTextField targetTextField, descriptionTF;

  private static SimEventListenerConnectionInspectorDialog dialog;
  private static boolean modified = false;
  private SimEventListenerEdge simEventListenerEdge;
    private final JButton okButton;
    private JButton cancelButton;

  private final JPanel  buttonPanel;
  private final enableApplyButtonListener lis;
  public static String xnewProperty;
  public static String newTarget,newTargetEvent,newSource,newSourceEvent;

  public static boolean showDialog(JFrame f, SimEventListenerEdge parm)
  {
    if (dialog == null)
      dialog = new SimEventListenerConnectionInspectorDialog(f, parm);
    else
      dialog.setParams(f, parm);

    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  private SimEventListenerConnectionInspectorDialog(JFrame parent, SimEventListenerEdge simEventListenerEdge)
  {
    super(parent, "SimEvent Listener Connection", true);
    this.simEventListenerEdge = simEventListenerEdge;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    lis = new enableApplyButtonListener();

    sourceLabel      = new JLabel("producing event graph",JLabel.TRAILING);
    targetLabel      = new JLabel("listening event graph",JLabel.TRAILING);
    descriptionLabel = new JLabel("description",JLabel.TRAILING);
    descriptionLabel.setToolTipText(DESCRIPTION_HINT);

    sourceTextField = new JTextField();
    targetTextField = new JTextField();
    descriptionTF   = new JTextField();
    descriptionTF.setToolTipText(DESCRIPTION_HINT);

    pairWidgets(sourceLabel,sourceTextField,false);
    pairWidgets(targetLabel,targetTextField,false);
    pairWidgets(descriptionLabel,  descriptionTF,true);

    buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    cancelButton = new JButton("Cancel");
    okButton = new JButton("Apply changes");
    buttonPanel.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    buttonPanel.add(Box.createHorizontalStrut(5));

    // Make the first display a minimum of 400 width
    Dimension d = getSize();
    d.width = Math.max(d.width,400);
    setSize(d);

    // attach listeners
    cancelButton.addActionListener(new cancelButtonListener());
    okButton.addActionListener(new applyButtonListener());

    setParams(parent, simEventListenerEdge);
  }

  private void pairWidgets(JLabel lab, JComponent tf, boolean edit)
  {
    ViskitStatics.clampHeight(tf);
    lab.setLabelFor(tf);
    if(tf instanceof JTextField){
      ((JTextComponent)tf).setEditable(edit);
      if(edit)
        ((JTextComponent)tf).addCaretListener(lis);
    }
  }

  public final void setParams(Component c, SimEventListenerEdge ae)
  {
    simEventListenerEdge = ae;

    fillWidgets();

    modified = (ae == null);
    okButton.setEnabled((ae == null));

    getRootPane().setDefaultButton(cancelButton);
    pack();
    setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(simEventListenerEdge != null) {
      EventGraphNode eventGraphNodeSource = (EventGraphNode)simEventListenerEdge.getFrom();
      EventGraphNode eventGraphNodeTarget = (EventGraphNode)simEventListenerEdge.getTo();
      sourceTextField.setText(eventGraphNodeSource.getName() + " (" + eventGraphNodeSource.getType()+")");
      targetTextField.setText(eventGraphNodeTarget.getName() + " (" + eventGraphNodeTarget.getType()+")");
 descriptionTF.setText(simEventListenerEdge.getDescriptionString());
    }
    else {
      sourceTextField.setText("");
      targetTextField.setText("");
    }

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    JPanel cont = new JPanel(new SpringLayout());
    cont.add(sourceLabel);      cont.add(sourceTextField);
    cont.add(targetLabel);      cont.add(targetTextField);
    cont.add(descriptionLabel);        cont.add(descriptionTF);
    SpringUtilities.makeCompactGrid(cont,3,2,10,10,5,5);
    content.add(cont);
    content.add(buttonPanel);
    content.add(Box.createVerticalStrut(5));
    setContentPane(content);
  }

  private void unloadWidgets()
  {
    if(simEventListenerEdge != null)
      simEventListenerEdge.setDescriptionString(descriptionTF.getText().trim());
  }

  class cancelButtonListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent event)
    {
      modified = false;    // for the caller
      dispose();
    }
  }

  class applyButtonListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (modified)
        unloadWidgets();
      dispose();
    }
  }


  class enableApplyButtonListener implements CaretListener, ActionListener
  {
    @Override
    public void caretUpdate(CaretEvent event)
    {
      modified = true;
      okButton.setEnabled(true);
      getRootPane().setDefaultButton(okButton);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      caretUpdate(null);
    }
  }


  class myCloseListener extends WindowAdapter
  {
    @Override
    public void windowClosing(WindowEvent e)
    {

      if (modified) {
        int ret = JOptionPane.showConfirmDialog(SimEventListenerConnectionInspectorDialog.this, "Apply changes?",
            "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION)
          okButton.doClick();
        else
          cancelButton.doClick();
      }
      else
        cancelButton.doClick();
    }
  }
}
