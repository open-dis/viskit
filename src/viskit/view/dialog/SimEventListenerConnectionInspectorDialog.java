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

    private final JLabel sourceLab;
  private JLabel targetLab, descLab;
    private final JTextField sourceTextField;
  private JTextField targetTextField, descriptionTF;

  private static SimEventListenerConnectionInspectorDialog dialog;
  private static boolean modified = false;
  private SimEventListenerEdge simEvEdge;
    private final JButton okButt;
    private JButton canButt;

  private final JPanel  buttPan;
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

    sourceLab = new JLabel("producing event graph",JLabel.TRAILING);
    targetLab = new JLabel("listening event graph",JLabel.TRAILING);
    descLab   = new JLabel("description",JLabel.TRAILING);

    sourceTextField = new JTextField();
    targetTextField = new JTextField();
    descriptionTF   = new JTextField();

    pairWidgets(sourceLab,sourceTextField,false);
    pairWidgets(targetLab,targetTextField,false);
    pairWidgets(descLab,  descriptionTF,true);

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(okButt);
    buttPan.add(canButt);
    buttPan.add(Box.createHorizontalStrut(5));

    // Make the first display a minimum of 400 width
    Dimension d = getSize();
    d.width = Math.max(d.width,400);
    setSize(d);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());

    setParams(parent, ed);
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
    simEvEdge = ae;

    fillWidgets();

    modified = (ae == null);
    okButt.setEnabled((ae == null));

    getRootPane().setDefaultButton(canButt);
    pack();
    setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if(simEvEdge != null) {
      EventGraphNode eventGraphNodeSource = (EventGraphNode)simEvEdge.getFrom();
      EventGraphNode eventGraphNodeTarget = (EventGraphNode)simEvEdge.getTo();
      sourceTextField.setText(eventGraphNodeSource.getName() + " (" + eventGraphNodeSource.getType()+")");
      targetTextField.setText(eventGraphNodeTarget.getName() + " (" + eventGraphNodeTarget.getType()+")");
 descriptionTF.setText(simEvEdge.getDescriptionString());
    }
    else {
      sourceTextField.setText("");
      targetTextField.setText("");
    }

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    JPanel cont = new JPanel(new SpringLayout());
    cont.add(sourceLab);      cont.add(sourceTextField);
    cont.add(targetLab);      cont.add(targetTextField);
    cont.add(descLab);        cont.add(descriptionTF);
    SpringUtilities.makeCompactGrid(cont,3,2,10,10,5,5);
    content.add(cont);
    content.add(buttPan);
    content.add(Box.createVerticalStrut(5));
    setContentPane(content);
  }

  private void unloadWidgets()
  {
    if(simEvEdge != null)
      simEvEdge.setDescriptionString(descriptionTF.getText().trim());
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
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
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
          okButt.doClick();
        else
          canButt.doClick();
      }
      else
        canButt.doClick();
    }
  }
}
