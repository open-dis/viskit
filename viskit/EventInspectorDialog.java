package viskit;

import viskit.model.EventArgument;
import viskit.model.EventLocalVariable;
import viskit.model.EventNode;
import viskit.model.EventStateTransition;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 8, 2004
 * Time: 2:56:21 PM
 */

public class EventInspectorDialog extends JDialog
{
  private static EventInspectorDialog dialog;
  private Component locationComponent;
  private EventNode node;
  private static boolean modified = false;
  private JButton canButt, okButt;
  private JTextField name;
  private JTextField comment;
  private TransitionsPanel transitions;
  private ArgumentsPanel arguments;
  private LocalVariablesPanel localVariables;
  private JFrame fr;
  private myChangeActionListener myChangeListener;

  /**
   * Set up and show the dialog.  The first Component argument
   * determines which frame the dialog depends on; it should be
   * a component in the dialog's controlling frame. The second
   * Component argument should be null if you want the dialog
   * to come up with its left corner in the center of the screen;
   * otherwise, it should be the component on top of which the
   * dialog should appear.
   */
  public static boolean showDialog(JFrame f, Component comp, EventNode node)
  {
    if (dialog == null)
      dialog = new EventInspectorDialog(f, comp, node);
    else
      dialog.setParams(comp, node);

    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  private EventInspectorDialog(JFrame frame,
                               Component locationComp,
                               EventNode node)
  {
    super(frame, "Event Inspector: " + node.getName(), true);
    this.fr = frame;
    this.node = node;
    this.locationComponent = locationComp;

    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    JPanel cont = new JPanel();
    setContentPane(cont);
    cont.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));

    JPanel threePanels = new JPanel();
    threePanels.setLayout(new BoxLayout(threePanels, BoxLayout.Y_AXIS));
    threePanels.add(Box.createVerticalStrut(5));

    // name
    JPanel namePan = new JPanel();
    namePan.setLayout(new BoxLayout(namePan, BoxLayout.X_AXIS));
    namePan.setOpaque(false);
    namePan.setBorder(BorderFactory.createTitledBorder("Event name"));
    name = new JTextField("Junk");
    name.setOpaque(true);
    name.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    namePan.add(name);
    // make the field expand only horiz.
    Dimension d = namePan.getPreferredSize();
    d.width = Integer.MAX_VALUE;
    namePan.setMaximumSize(d);

    threePanels.add(namePan);
    threePanels.add(Box.createVerticalStrut(5));

    // comment
    JPanel commentPan = new JPanel();
    commentPan.setLayout(new BoxLayout(commentPan, BoxLayout.X_AXIS));
    commentPan.setOpaque(false);
    commentPan.setBorder(BorderFactory.createTitledBorder("Description"));
    comment = new JTextField("");
    comment.setOpaque(true);
    comment.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    commentPan.add(comment);
    d = commentPan.getPreferredSize();
    d.width = Integer.MAX_VALUE;
    commentPan.setMaximumSize(d);

    JButton edComment = new JButton(" ... ");
    edComment.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    edComment.setToolTipText("Click to edit a long description");
    Dimension dd = edComment.getPreferredSize();
    dd.height = d.height;
    edComment.setMaximumSize(dd);
    commentPan.add(edComment);

    threePanels.add(commentPan);
    threePanels.add(Box.createVerticalStrut(5));
/*
      // delay
      JPanel delayPan = new JPanel();
      delayPan.setLayout(new BoxLayout(delayPan,BoxLayout.X_AXIS));
      delayPan.setOpaque(false);
      delayPan.setBorder(BorderFactory.createTitledBorder("Time delay"));
        delay = new JTextField();
        delay.setOpaque(true);
        delay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
      delayPan.add(delay);
    threePanels.add(delayPan);
    threePanels.add(Box.createVerticalStrut(5));
*/
    // Event arguments
    arguments = new ArgumentsPanel(300,2);
    arguments.setBorder(BorderFactory.createTitledBorder("Event arguments"));
    threePanels.add(arguments);
    threePanels.add(Box.createVerticalStrut(5));

    // local vars
    localVariables = new LocalVariablesPanel(300,2);
    localVariables.setBorder(BorderFactory.createTitledBorder("Local variables"));
    threePanels.add(localVariables);
    threePanels.add(Box.createVerticalStrut(5));

    // state transitions
    transitions = new TransitionsPanel();
    transitions.setBorder(BorderFactory.createTitledBorder("State transitions"));
    threePanels.add(transitions);
    threePanels.add(Box.createVerticalStrut(5));

    // buttons
    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());
    buttPan.add(canButt);
    buttPan.add(okButt);
    threePanels.add(buttPan);

    cont.add(threePanels);

    fillWidgets();     // put the data into the widgets
    sizeAndPosition();

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());
    myChangeListener = new myChangeActionListener();
    //name.addActionListener(chlis);
    KeyListener klis = new myKeyListener();
    name.addKeyListener(klis);
    comment.addKeyListener(klis);
    edComment.addActionListener(new commentListener());
    arguments.addPlusListener(myChangeListener);
    arguments.addMinusListener(myChangeListener);
    arguments.addDoubleClickedListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        EventArgument ea = (EventArgument) e.getSource();
        boolean modified = EventArgumentDialog.showDialog(fr, locationComponent, ea);
        if (modified) {
          arguments.updateRow(ea);
        }
      }
    });

    transitions.addPlusListener(myChangeListener);
    transitions.addMinusListener(myChangeListener);
    transitions.addDoubleClickedListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        EventStateTransition est = (EventStateTransition) e.getSource();
        boolean modified = EventTransitionDialog.showDialog((EventGraphViewFrame)fr, locationComponent, est);
        if (modified) {
          /*
          // experiment
          EventLocalVariable nuts = new EventLocalVariable("__01","int",est.getIndexingExpression());
          localVariables.addRow(nuts);
          est.setIndexingExpression(nuts.getName());
          // works...
          // //todo resolve how to sync
          */

          transitions.updateTransition(est);
          okButt.setEnabled(true);
          EventInspectorDialog.modified = true;
        }
      }
    });

    localVariables.addPlusListener(myChangeListener);
    localVariables.addMinusListener(myChangeListener);
    localVariables.addDoubleClickedListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        EventLocalVariable elv = (EventLocalVariable) e.getSource();
        boolean modified = LocalVariableDialog.showDialog(fr, locationComponent, elv);
        if (modified) {
          localVariables.updateRow(elv);
          okButt.setEnabled(true);
          EventInspectorDialog.modified = true;
        }
      }
    });
  }

  private void sizeAndPosition()
  {
    pack();     // do this prior to next
    // little check to add some extra space to always include the node name in title bar w/out dotdotdots
    if (getWidth() < 350)
      setSize(350, getHeight());
    this.setLocationRelativeTo(locationComponent);
  }

  public void setParams(Component c, EventNode en)
  {
    node = en;
    locationComponent = c;

    fillWidgets();
    sizeAndPosition();
  }

  private void fillWidgets()
  {
    String nmSt = node.getName();
    nmSt.replace(' ','_');
    setTitle("Event Inspector: " + nmSt); //node.getName());
    name.setText(nmSt); //node.getName());
    Dimension d = comment.getPreferredSize();
    comment.setText(fillString(node.getComments()));
    comment.setCaretPosition(0);
    comment.setPreferredSize(d);
    transitions.setTransitions(node.getTransitions());
    arguments.setData(node.getArguments());
    localVariables.setData(node.getLocalVariables());
    modified = false;
    okButt.setEnabled(false);
    getRootPane().setDefaultButton(canButt);
  }

  private void unloadWidgets(EventNode en)
  {
    if (modified) {
      en.setName(name.getText().trim().replace(' ','_'));
      //en.setName(name.getText());
      en.setTransitions(transitions.getTransitions());
      en.setArguments(arguments.getData());
      en.setLocalVariables(new Vector(localVariables.getData()));
      en.getComments().clear();
      en.getComments().add(comment.getText().trim());
    }
  }
  private String fillString (ArrayList lis)
  {
    if(lis == null) return "";
    StringBuffer sb = new StringBuffer();
    for(Iterator itr = lis.iterator(); itr.hasNext();) {
      sb.append((String)itr.next());
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  class cancelButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      modified = false;
      VGlobals.instance().getActiveEventGraphModel().resetLVNameGenerator();  // To start numbering over next time
      setVisible(false);
    }
  }

  class applyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      if (modified) {

        // Our node object hasn't been updated yet (see unloadWidgets) and won't if
        // we cancel out below.  But to do the beanshell parse test, a node needs to be supplied
        // so the context can be set up properly.
        // Build a temp one;
        EventNode evn = node.shallowCopy();   // temp copy
        unloadWidgets(evn);  // put our pending edits in place

        // Parse the state transitions
        StringBuffer parseThis = new StringBuffer();
        for (Iterator itr = transitions.getTransitions().iterator(); itr.hasNext();) {
          EventStateTransition est = (EventStateTransition)itr.next();
          parseThis.append(est.toString());
          parseThis.append(";");
          String idxv = est.getIndexingExpression();
          if(idxv != null && idxv.length()>0)
            addPotentialLocalIndexVariable(evn,est.getIndexingExpression());
        }

        if(ViskitConfig.instance().getVal("app.beanshell.warning").equalsIgnoreCase("true")) {
          String parseResults = VGlobals.instance().parseCode(evn, parseThis.toString().trim());
          if (parseResults != null) {
            boolean ret = BeanshellErrorDialog.showDialog(parseResults,EventInspectorDialog.this);
            if(ret == false) // don't ignore
              return;
/*
            int ret = JOptionPane.showConfirmDialog(EventInspectorDialog.this, "Java language error:\n" + parseResults + "\nIgnore and continue?",
                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret != JOptionPane.YES_OPTION)
              return;
*/
          }
        }
        unloadWidgets(node);
      }
      setVisible(false);
    }
    private void addPotentialLocalIndexVariable(EventNode n, String lvName)
    {
      Vector locVars = n.getLocalVariables();
      locVars.add(new EventLocalVariable(lvName,"int","0"));
    }
  }

  class myKeyListener extends KeyAdapter
  {
    public void keyTyped(KeyEvent e)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }
  }

  class myChangeActionListener implements ChangeListener, ActionListener
  {
    public void stateChanged(ChangeEvent event)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }

    public void actionPerformed(ActionEvent event)
    {
      stateChanged(null);
    }
  }
  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if(modified == true) {
        int ret = JOptionPane.showConfirmDialog(EventInspectorDialog.this,"Apply changes?",
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
  class commentListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      StringBuffer sb = new StringBuffer(EventInspectorDialog.this.comment.getText().trim());
      boolean modded = EventCommentDialog.showDialog(fr,EventInspectorDialog.this,sb);
      if(modded) {
        EventInspectorDialog.this.comment.setText(sb.toString().trim());
        EventInspectorDialog.this.comment.setCaretPosition(0);
        modified = true;
        okButt.setEnabled(true);
      }
    }
  }
}
