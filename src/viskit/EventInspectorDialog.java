package viskit;

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

import viskit.model.EventArgument;
import viskit.model.EventLocalVariable;
import viskit.model.EventNode;
import viskit.model.EventStateTransition;
import viskit.model.ViskitElement;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:56:21 PM
 * @version $Id: EventInspectorDialog.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */

public class EventInspectorDialog extends JDialog
{
  private static EventInspectorDialog dialog;
  private Component locationComponent;
  private JFrame fr;
  private EventNode node;
  private static boolean modified = false;
  private JTextField name;
  private JTextField description;

  private JPanel descriptionPanel;
  private TransitionsPanel transitions;
  private ArgumentsPanel arguments;
  private LocalVariablesPanel localVariables;
  private CodeBlockPanel codeBlock;

  private JButton cancelButton, okButton;
  private JButton addDescriptionButton,addArgumentsButton,addLocalsButton,addCodeBlockButton,addStateTransitionsButton;

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

    JPanel panel = new JPanel();
    setContentPane(panel);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(15,10,10,10));

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
    panel.add(namePan);

    descriptionPanel = new JPanel();
    descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));
    descriptionPanel.setOpaque(false);
    descriptionPanel.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Description")));
    description = new JTextField("");
    description.setOpaque(true);
    description.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    descriptionPanel.add(description);
    d = descriptionPanel.getPreferredSize();
    d.width = Integer.MAX_VALUE;
    descriptionPanel.setMaximumSize(new Dimension(d));

    JButton editDescriptionButton = new JButton(" ... ");
    editDescriptionButton.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    editDescriptionButton.setToolTipText("Click to edit a long description");
    Dimension dd = editDescriptionButton.getPreferredSize();
    dd.height = d.height;
    editDescriptionButton.setMaximumSize(new Dimension(dd));
    descriptionPanel.add(editDescriptionButton);
    panel.add(descriptionPanel);

    // Event arguments
    arguments = new ArgumentsPanel(300,2);
    arguments.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Event arguments")));
    panel.add(arguments);

    // local vars
    localVariables = new LocalVariablesPanel(300,2);
    localVariables.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Local variables")));
    panel.add(localVariables);

    // code block
    codeBlock = new CodeBlockPanel(this,true, "Event Code Block");
    codeBlock.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("Code block")));
    panel.add(codeBlock);

    // state transitions
    transitions = new TransitionsPanel();
    transitions.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),BorderFactory.createTitledBorder("State transitions")));
    panel.add(transitions);

    // buttons
    JPanel twoRowButtonPanel = new JPanel();
    twoRowButtonPanel.setLayout(new BoxLayout(twoRowButtonPanel,BoxLayout.Y_AXIS));

    JPanel tinyButtonPanel = new JPanel();
    tinyButtonPanel.setLayout(new BoxLayout(tinyButtonPanel,BoxLayout.X_AXIS));

    addDescriptionButton = new JButton("add description");
    addArgumentsButton = new JButton("add arguments");
    addLocalsButton = new JButton("add locals");
    addCodeBlockButton = new JButton("add code block");
    addStateTransitionsButton= new JButton("add state transitions");

    Font defButtFont = addDescriptionButton.getFont();
      int defButtFontSize = defButtFont.getSize();
      addDescriptionButton.setFont(defButtFont.deriveFont((float)(defButtFontSize-4)));
      addArgumentsButton.setFont(addDescriptionButton.getFont());
      addLocalsButton.setFont(addDescriptionButton.getFont());
      addCodeBlockButton.setFont(addDescriptionButton.getFont());
      addStateTransitionsButton.setFont(addDescriptionButton.getFont());

      tinyButtonPanel.add(Box.createHorizontalGlue());
      tinyButtonPanel.add(addDescriptionButton);
      tinyButtonPanel.add(addArgumentsButton);
      tinyButtonPanel.add(addLocalsButton);
      tinyButtonPanel.add(addCodeBlockButton);
      tinyButtonPanel.add(addStateTransitionsButton);
      tinyButtonPanel.add(Box.createHorizontalGlue());
    twoRowButtonPanel.add(tinyButtonPanel);
    twoRowButtonPanel.add(Box.createVerticalStrut(5));

      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
      cancelButton = new JButton("Cancel");
      okButton = new JButton("Apply changes");
      buttonPanel.add(Box.createHorizontalGlue());
      buttonPanel.add(cancelButton);
      buttonPanel.add(okButton);
    twoRowButtonPanel.add(buttonPanel);

    panel.add(twoRowButtonPanel);

    fillWidgets();     // put the data into the widgets
    sizeAndPosition();

    // attach listeners
    cancelButton.addActionListener(new cancelButtonListener());
    okButton.addActionListener(new applyButtonListener());


    addHideButtListener hideList = new addHideButtListener();
    addDescriptionButton.addActionListener(hideList);
    addArgumentsButton.addActionListener(hideList);
    addLocalsButton.addActionListener(hideList);
    addCodeBlockButton.addActionListener(hideList);
    addStateTransitionsButton.addActionListener(hideList);

    myChangeActionListener myChangeListener = new myChangeActionListener();
    //name.addActionListener(chlis);
    KeyListener klis = new myKeyListener();
    name.addKeyListener(klis);
    description.addKeyListener(klis);
    codeBlock.addUpdateListener(myChangeListener);
    editDescriptionButton.addActionListener(new commentListener());
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
      @Override
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
          okButton.setEnabled(true);
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
          okButton.setEnabled(true);
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
    nmSt = nmSt.replace(' ','_');
    setTitle("Event Inspector: " + nmSt);
    name.setText(nmSt);

    Dimension d = description.getPreferredSize();
    String s = fillString(node.getComments());
    description.setText(s);
    description.setCaretPosition(0);
    description.setPreferredSize(d);
    
//    hideShowDescription(true); // always show
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
    okButton.setEnabled(false);
    getRootPane().setDefaultButton(cancelButton);
  }

  private void unloadWidgets(EventNode en)
  {
    if (modified) {
      en.setName(name.getText().trim().replace(' ','_'));
            
      en.setTransitions(transitions.getTransitions());
      
      en.setArguments(arguments.getData());
      en.setLocalVariables(new Vector<ViskitElement>(localVariables.getData()));
      en.getComments().clear();
      en.getComments().add(description.getText().trim());
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
            if(!ret) // don't ignore
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
      Vector<ViskitElement> locVars = n.getLocalVariables();
      locVars.add(new EventLocalVariable(lvName,"int","0"));
    }
  }

  // begin show/hide support for unused fields
  private void hideShowDescription(boolean show)
  {
    descriptionPanel.setVisible(show);
    addDescriptionButton.setVisible(!show);
    pack();
  }

  private void hideShowArguments(boolean show)
  {
    arguments.setVisible(show);
    addArgumentsButton.setVisible(!show);
    pack();
  }

  private void hideShowLocals(boolean show)
  {
    localVariables.setVisible(show);
    addLocalsButton.setVisible(!show);
    pack();
  }

  private void hideShowCodeBlock(boolean show)
  {
    codeBlock.setVisible(show);
    addCodeBlockButton.setVisible(!show);
    pack();
  }

  private void hideShowStateTransitions(boolean show)
  {
    transitions.setVisible(show);
    addStateTransitionsButton.setVisible(!show);
    pack();
  }

  class addHideButtListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if(e.getSource().equals(addDescriptionButton))
        hideShowDescription(true);
      else if(e.getSource().equals(addArgumentsButton))
        hideShowArguments(true);
      else if(e.getSource().equals(addLocalsButton))
        hideShowLocals(true);
      else if(e.getSource().equals(addCodeBlockButton))
        hideShowCodeBlock(true);
      else if(e.getSource().equals(addStateTransitionsButton))
        hideShowStateTransitions(true);
    }
  }

  // end show/hide support for unused fields
  
  class myKeyListener extends KeyAdapter
  {
    @Override
    public void keyTyped(KeyEvent e)
    {
      modified = true;
      okButton.setEnabled(true);
      getRootPane().setDefaultButton(okButton);
    }
  }

  class myChangeActionListener implements ChangeListener, ActionListener
  {
    public void stateChanged(ChangeEvent event)
    {
      modified = true;
      okButton.setEnabled(true);
      getRootPane().setDefaultButton(okButton);
    }

    public void actionPerformed(ActionEvent event)
    {
      stateChanged(null);
    }
  }
  class myCloseListener extends WindowAdapter
  {
    @Override
    public void windowClosing(WindowEvent e)
    {
      if(modified) {
        int ret = JOptionPane.showConfirmDialog(EventInspectorDialog.this,"Apply changes?",
            "Question",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(ret == JOptionPane.YES_OPTION)
          okButton.doClick();
        else
          cancelButton.doClick();
        }
      else
        cancelButton.doClick();
    }
  }
  class commentListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      StringBuffer sb = new StringBuffer(EventInspectorDialog.this.description.getText().trim());
      boolean modded = TextAreaDialog.showTitledDialog("Event Description",EventInspectorDialog.this,
                                                       EventInspectorDialog.this,sb);
      if(modded) {
        EventInspectorDialog.this.description.setText(sb.toString().trim());
        EventInspectorDialog.this.description.setCaretPosition(0);
        modified = true;
        okButton.setEnabled(true);
      }
    }
  }
}
