package viskit;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: June 2, 2004
 * Time: 9:19:41 AM
 */

import viskit.model.*;
import viskit.xsd.bindings.assembly.SimEntity;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Vector;

public class SimEventListenerConnectionInspectorDialog extends JDialog
{
  private JLabel sourceLab, targetLab;
  private JTextField sourceTF, targetTF;

  private static SimEventListenerConnectionInspectorDialog dialog;
  private static boolean modified = false;
  private SimEvListenerEdge simEvEdge;
  private Component locationComp;
  private JButton /* okButt, */canButt;

  private JPanel  buttPan;
  //private enableApplyButtonListener lis;
  public static String xnewProperty;
  public static String newTarget,newTargetEvent,newSource,newSourceEvent;

  public static boolean showDialog(JFrame f, Component comp, SimEvListenerEdge parm)
  {
    if (dialog == null)
      dialog = new SimEventListenerConnectionInspectorDialog(f, comp, parm);
    else
      dialog.setParams(comp, parm);

    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  private SimEventListenerConnectionInspectorDialog(JFrame parent, Component comp, SimEvListenerEdge ed)
  {
    super(parent, "Adapter Connection", true);
    simEvEdge = ed;
    this.locationComp = comp;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

   // lis = new enableApplyButtonListener();


    sourceLab = new JLabel("source event graph",JLabel.TRAILING);
    targetLab = new JLabel("target event graph",JLabel.TRAILING);

    sourceTF = new JTextField();
    targetTF = new JTextField();
    pairWidgets(sourceLab,sourceTF,false);
    pairWidgets(targetLab,targetTF,false);

    buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Close"); //"Cancel");
    //okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    //buttPan.add(okButt);
    buttPan.add(Box.createHorizontalStrut(5));

    fillWidgets();     // put the data into the widgets

    modified = (ed == null ? true : false);     // if it's a new pclNode, they can always accept defaults with no typing
  //  okButt.setEnabled((ed == null ? true : false));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next

    // Make the first display a minimum of 400 width
    Dimension d = getSize();
    d.width = Math.max(d.width,400);
    setSize(d);

    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    //okButt.addActionListener(new applyButtonListener());
  }
  private void pairWidgets(JLabel lab, JComponent tf, boolean edit)
  {
    clampHeight(tf);
    lab.setLabelFor(tf);
    if(tf instanceof JTextField){
     // ((JTextField)tf).addCaretListener(lis);
      ((JTextField)tf).setEditable(edit);
    }
  }
  public void setParams(Component c, SimEvListenerEdge ae)
  {
    simEvEdge = ae;
    locationComp = c;

    fillWidgets();

    modified = (ae == null ? true : false);
//    okButt.setEnabled((ae == null ? true : false));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }
  private void fillWidgets()
  {
    if(simEvEdge != null) {
      EvGraphNode egnS = (EvGraphNode)simEvEdge.getFrom();
      EvGraphNode egnT = (EvGraphNode)simEvEdge.getTo();
      sourceTF.setText(egnS.getName() + " (" + egnS.getType()+")");
      targetTF.setText(egnT.getName() + " (" + egnT.getType()+")");
    }
    else {
      sourceTF.setText("");
      targetTF.setText("");
    }

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    JPanel cont = new JPanel(new SpringLayout());
    cont.add(sourceLab);      cont.add(sourceTF);
    cont.add(targetLab);      cont.add(targetTF);

    SpringUtilities.makeCompactGrid(cont,2,2,10,10,5,5);
    content.add(cont);
    content.add(buttPan);
    content.add(Box.createVerticalStrut(5));
    setContentPane(content);
  }

  private void unloadWidgets()
  {
    // nil to do
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
      if (modified)
        unloadWidgets();
      setVisible(false);
    }
  }

/*
  class enableApplyButtonListener implements CaretListener, ActionListener
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
*/

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
/*
      if (modified == true) {
        int ret = JOptionPane.showConfirmDialog(SimEventListenerConnectionInspectorDialog.this, "Apply changes?",
            "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION)
          okButt.doClick();
        else
          canButt.doClick();
      }
      else
*/
        canButt.doClick();
    }
  }

  void clampHeight(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
    comp.setMinimumSize(new Dimension(Integer.MAX_VALUE,d.height));
  }
}


