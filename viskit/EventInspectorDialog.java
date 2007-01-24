package viskit;

import viskit.model.EventArgument;
import viskit.model.EventLocalVariable;
import viskit.model.EventNode;
import viskit.model.EventStateTransition;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

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
  private JFrame fr;
  private EventNode node;
  private static boolean modified = false;
  private JTextField name;
  private JTextField comment;

  private JPanel commentPan;
  private TransitionsPanel transitions;
  private ArgumentsPanel arguments;
  private LocalVariablesPanel localVariables;
  private CodeBlockPanel codeBlock;

  private JButton canButt, okButt;
  private JButton addDescButt,addArgsButt,addLocsButt,addCodeButt,addTransButt;

  /**
   * Set up and show the dialog.  The first Component argument
   * determines which frame the dialog depends on; it should be
   * a component in the dialog's controlling frame. The second
   * Component argument should be null if you want the dialog
   * to come up with its left corner in the center of the screen;
   * otherwise, it should be the component on top of which the
   * dialog should appear.
   * @return whether data modified
   * @param f parent frame
   * @param comp location component
   * @param node EventNode to edit
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
    cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
    cont.setBorder(BorderFactory.createEmptyBorder(15,10,10,10));

    // name
    JPanel namePan = new JPanel();
    namePan.setLayout(new BoxLayout(namePan, BoxLayout.X_AXIS));
    namePan.setOpaque(false);
    namePan.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Event name")));
    name = new JTextField(30); // This sets the "preferred width" when this dialog is packed
    name.setOpaque(true);
    name.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    namePan.add(name);
    // make the field expand only horiz.
    Dimension d = namePan.getPreferredSize();
    d.width = Integer.MAX_VALUE;
    namePan.setMaximumSize(new Dimension(d));
    cont.add(namePan);


    // comment
    commentPan = new JPanel();
    commentPan.setLayout(new BoxLayout(commentPan, BoxLayout.X_AXIS));
    commentPan.setOpaque(false);
    commentPan.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Description")));
    comment = new JTextField("");
    comment.setOpaque(true);
    comment.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    commentPan.add(comment);
    d = commentPan.getPreferredSize();
    d.width = Integer.MAX_VALUE;
    commentPan.setMaximumSize(new Dimension(d));

    JButton edComment = new JButton(" ... ");
    edComment.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    edComment.setToolTipText("Click to edit a long description");
    Dimension dd = edComment.getPreferredSize();
    dd.height = d.height;
    edComment.setMaximumSize(new Dimension(dd));
    commentPan.add(edComment);
    cont.add(commentPan);

    // Event arguments
    arguments = new ArgumentsPanel(300,2);
    arguments.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Event arguments")));
    cont.add(arguments);

    // local vars
    localVariables = new LocalVariablesPanel(300,2);
    localVariables.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Local variables")));
    cont.add(localVariables);

    // code block
    codeBlock = new CodeBlockPanel(this,true, "Event Code Block");
    codeBlock.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Code block")));
    cont.add(codeBlock);

    // state transitions
    transitions = new TransitionsPanel();
    transitions.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("State transitions")));
    cont.add(transitions);

    // buttons
    JPanel twoRowButtPan = new JPanel();
    twoRowButtPan.setLayout(new BoxLayout(twoRowButtPan,BoxLayout.Y_AXIS));

    JPanel tinyButtPan = new JPanel();
    tinyButtPan.setLayout(new BoxLayout(tinyButtPan,BoxLayout.X_AXIS));

    addDescButt = new JButton("add description");
    addArgsButt = new JButton("add arguments");
    addLocsButt = new JButton("add locals");
    addCodeButt = new JButton("add code block");
    addTransButt= new JButton("add state transitions");

    Font defButtFont = addDescButt.getFont();
      int defButtFontSize = defButtFont.getSize();
      addDescButt.setFont(defButtFont.deriveFont((float)(defButtFontSize-4)));
      addArgsButt.setFont(addDescButt.getFont());
      addLocsButt.setFont(addDescButt.getFont());
      addCodeButt.setFont(addDescButt.getFont());
      addTransButt.setFont(addDescButt.getFont());

      tinyButtPan.add(Box.createHorizontalGlue());
      tinyButtPan.add(addDescButt);
      tinyButtPan.add(addArgsButt);
      tinyButtPan.add(addLocsButt);
      tinyButtPan.add(addCodeButt);
      tinyButtPan.add(addTransButt);
      tinyButtPan.add(Box.createHorizontalGlue());
    twoRowButtPan.add(tinyButtPan);
    twoRowButtPan.add(Box.createVerticalStrut(5));

      JPanel buttPan = new JPanel();
      buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
      canButt = new JButton("Cancel");
      okButt = new JButton("Apply changes");
      buttPan.add(Box.createHorizontalGlue());
      buttPan.add(canButt);
      buttPan.add(okButt);
    twoRowButtPan.add(buttPan);

    cont.add(twoRowButtPan);

    fillWidgets();     // put the data into the widgets
    sizeAndPosition();

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());


    addHideButtListener hideList = new addHideButtListener();
    addDescButt.addActionListener(hideList);
    addArgsButt.addActionListener(hideList);
    addLocsButt.addActionListener(hideList);
    addCodeButt.addActionListener(hideList);
    addTransButt.addActionListener(hideList);

    myChangeActionListener myChangeListener = new myChangeActionListener();
    //name.addActionListener(chlis);
    KeyListener klis = new myKeyListener();
    name.addKeyListener(klis);
    comment.addKeyListener(klis);
    codeBlock.addUpdateListener(myChangeListener);
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
        boolean modified = EventTransitionDialog.showDialog(EventInspectorDialog.this, locationComponent, est);
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
    setTitle("Event Inspector: " + nmSt);
    name.setText(nmSt);

    Dimension d = comment.getPreferredSize();
    String s = fillString(node.getComments());
    comment.setText(s);
    comment.setCaretPosition(0);
    comment.setPreferredSize(d);
    hideShowDescription(s != null && s.length()>0);

    s = node.getCodeBlock();
    codeBlock.setData(s);
    codeBlock.setVisibleLines(1);
    hideShowCodeBlock(s != null && s.length()>0);

    transitions.setTransitions(node.getTransitions());
    s = transitions.getString();
    hideShowStateTransitions(s != null && s.length()>0);

    arguments.setData(node.getArguments());
    hideShowArguments(!arguments.isEmpty());

    localVariables.setData(node.getLocalVariables());
    hideShowLocals(!localVariables.isEmpty());

    modified = false;
    okButt.setEnabled(false);
    getRootPane().setDefaultButton(canButt);
  }

  private void unloadWidgets(EventNode en)
  {
    if (modified) {
      en.setName(name.getText().trim().replace(' ','_'));
      en.setTransitions(transitions.getTransitions());
      en.setArguments(arguments.getData());
      en.setLocalVariables(new Vector(localVariables.getData()));
      en.getComments().clear();
      en.getComments().add(comment.getText().trim());
      en.setCodeBLock(codeBlock.getData());
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
        String ps = parseThis.toString().trim();
        if(ps.length()>0 && ViskitConfig.instance().getVal("app.beanshell.warning").equalsIgnoreCase("true")) {
          String parseResults = VGlobals.instance().parseCode(evn, ps);
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

  // begin show/hide support for unused fields
  private void hideShowDescription(boolean show)
  {
    commentPan.setVisible(show);
    addDescButt.setVisible(!show);
    pack();
  }

  private void hideShowArguments(boolean show)
  {
    arguments.setVisible(show);
    addArgsButt.setVisible(!show);
    pack();
  }

  private void hideShowLocals(boolean show)
  {
    localVariables.setVisible(show);
    addLocsButt.setVisible(!show);
    pack();
  }

  private void hideShowCodeBlock(boolean show)
  {
    codeBlock.setVisible(show);
    addCodeButt.setVisible(!show);
    pack();
  }

  private void hideShowStateTransitions(boolean show)
  {
    transitions.setVisible(show);
    addTransButt.setVisible(!show);
    pack();
  }

  class addHideButtListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if(e.getSource().equals(addDescButt))
        hideShowDescription(true);
      else if(e.getSource().equals(addArgsButt))
        hideShowArguments(true);
      else if(e.getSource().equals(addLocsButt))
        hideShowLocals(true);
      else if(e.getSource().equals(addCodeButt))
        hideShowCodeBlock(true);
      else if(e.getSource().equals(addTransButt))
        hideShowStateTransitions(true);
    }
  }

  // end show/hide support for unused fields
  
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
      boolean modded = TextAreaDialog.showTitledDialog("Event Description",EventInspectorDialog.this,
                                                       EventInspectorDialog.this,sb);
      if(modded) {
        EventInspectorDialog.this.comment.setText(sb.toString().trim());
        EventInspectorDialog.this.comment.setCaretPosition(0);
        modified = true;
        okButt.setEnabled(true);
      }
    }
  }
}
