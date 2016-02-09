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
import viskit.ViskitStatics;
import viskit.model.EventGraphNode;
import viskit.model.SimEventListenerEdge;

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
  private final JLabel     sourceLabel, targetLabel, descriptionLabel;
  private final JTextField sourceTF, targetTF, descriptionTF;

  private static SimEventListenerConnectionInspectorDialog dialog;
  private static boolean modified = false;
  private SimEventListenerEdge simEvEdge;
  private final JButton  okButton, cancelButton;

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

  private SimEventListenerConnectionInspectorDialog(JFrame parent, SimEventListenerEdge ed)
  {
    super(parent, "SimEvent Listener Connection", true);
    simEvEdge = ed;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    lis = new enableApplyButtonListener();

    sourceLabel = new JLabel("producing event graph",JLabel.TRAILING);
    targetLabel = new JLabel("listening event graph",JLabel.TRAILING);
    descriptionLabel   = new JLabel("description",JLabel.TRAILING);

    sourceTF = new JTextField();
    targetTF = new JTextField();
    descriptionTF   = new JTextField();

    pairWidgets(sourceLabel,sourceTF,false);
    pairWidgets(targetLabel,targetTF,false);
    pairWidgets(descriptionLabel,  descriptionTF,true);

    buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        okButton = new JButton("Apply changes");
    cancelButton = new JButton("Cancel");
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

    setParams(parent, ed);
  }

  private void pairWidgets(JLabel lab, JComponent tf, boolean edit)
  {
    ViskitStatics.clampHeight(tf);
    lab.setLabelFor(tf);
    if(tf instanceof JTextField){
      ((JTextField)tf).setEditable(edit);
      if(edit)
        ((JTextField)tf).addCaretListener(lis);
    }
  }

  public final void setParams(Component c, SimEventListenerEdge ae)
  {
    simEvEdge = ae;

    fillWidgets();

    modified = (ae == null);
    okButton.setEnabled((ae == null));

    getRootPane().setDefaultButton(cancelButton);
    pack();
    setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(simEvEdge != null) {
      EventGraphNode egnS = (EventGraphNode)simEvEdge.getFrom();
      EventGraphNode egnT = (EventGraphNode)simEvEdge.getTo();
      sourceTF.setText(egnS.getName() + " (" + egnS.getType()+")");
      targetTF.setText(egnT.getName() + " (" + egnT.getType()+")");
      descriptionTF  .setText(simEvEdge.getDescription());
    }
    else {
      sourceTF.setText("");
      targetTF.setText("");
    }

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    JPanel cont = new JPanel(new SpringLayout());
    cont.add(sourceLabel);      cont.add(sourceTF);
    cont.add(targetLabel);      cont.add(targetTF);
    cont.add(descriptionLabel);        cont.add(descriptionTF);
    SpringUtilities.makeCompactGrid(cont,3,2,10,10,5,5);
    content.add(cont);
    content.add(buttonPanel);
    content.add(Box.createVerticalStrut(5));
    setContentPane(content);
  }

  private void unloadWidgets()
  {
    if(simEvEdge != null)
      simEvEdge.setDescription(descriptionTF.getText().trim());
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
