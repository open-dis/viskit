package viskit;

import edu.nps.util.BoxLayoutUtils;
import simkit.Priority;
import viskit.model.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Vector;
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

public class EdgeInspectorDialog extends JDialog
{
  private static EdgeInspectorDialog dialog;
  private Edge edge;
  private static boolean modified = false;
  private JButton canButt,okButt;
  private JLabel srcEvent,targEvent;
  private JTextField delay;
  private EdgeParametersPanel parameters;
  private ConditionalsPanel conditionals;
  private JPanel delayPan;
  private Border delayPanBorder,delayPanDisabledBorder;

  private JPanel priorityPan;
  private JComboBox priorityCB;
  private ArrayList<Priority> priorityList;  // matches combo box
  private Vector<String> priorityNames;
  private int priorityDefaultIndex = 3;  // set properly below
  
  private JPanel myParmPanel;

  private JLabel schLab;
  private JLabel canLab;

  /**
   * Set up and show the dialog.  The first Component argument
   * determines which frame the dialog depends on; it should be
   * a component in the dialog's controlling frame. The second
   * Component argument should be null if you want the dialog
   * to come up with its left corner in the center of the screen;
   * otherwise, it should be the component on top of which the
   * dialog should appear.
   */
  public static boolean showDialog(JFrame f, Component comp, Edge edge)
  {
    if(dialog == null)
      dialog = new EdgeInspectorDialog(f,comp,edge);
    else
      dialog.setParams(comp,edge);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  Vector nodeList;
  Model mod; //todo fix
  private EdgeInspectorDialog(JFrame frame,
                              Component locationComp,
                              Edge edge)
  {
    super(frame, "Edge Inspector", true);
    this.edge = edge;

    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    mod = (Model)(VGlobals.instance().getEventGraphEditor().getModel());
   // nodeList = mod.getAllNodes();

    //Collections.sort(nodeList);             // todo get working

    Container cont = getContentPane();
    cont.setLayout(new BoxLayout(cont,BoxLayout.Y_AXIS));
    JPanel con = new JPanel();
    con.setLayout(new BoxLayout(con,BoxLayout.Y_AXIS));
    con.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    con.add(Box.createVerticalStrut(5));
      // edge type
      JPanel typeP = new JPanel();
      typeP.setLayout(new BoxLayout(typeP,BoxLayout.X_AXIS));
        typeP.add(Box.createHorizontalGlue());
        JLabel lab = new JLabel("Type: ");
        BoxLayoutUtils.clampWidth(lab);
        typeP.add(lab);
        typeP.add(Box.createHorizontalStrut(5));
        schLab = new JLabel("<html><b>Scheduling");
        BoxLayoutUtils.clampWidth(schLab);
        canLab = new JLabel("<html><b>Cancelling");
        BoxLayoutUtils.clampWidth(canLab);
        typeP.add(schLab);
        typeP.add(canLab);
        typeP.add(Box.createHorizontalGlue());

      BoxLayoutUtils.clampHeight(typeP);
    con.add(typeP);
    con.add(Box.createVerticalStrut(5));

      JPanel srcTargP = new JPanel();
      srcTargP.setLayout(new BoxLayout(srcTargP,BoxLayout.X_AXIS));
      srcTargP.add(Box.createHorizontalGlue());
        JPanel stNamesP = new JPanel();
        stNamesP.setLayout(new BoxLayout(stNamesP,BoxLayout.Y_AXIS));
          JLabel srcLab = new JLabel("Source event:");
          srcLab.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        stNamesP.add(srcLab);
          JLabel tarLab = new JLabel("Target event:");
          tarLab.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        stNamesP.add(tarLab);
      srcTargP.add(stNamesP);
      srcTargP.add(Box.createHorizontalStrut(25));
        JPanel stValuesP = new JPanel();
        stValuesP.setLayout(new BoxLayout(stValuesP,BoxLayout.Y_AXIS));
          srcEvent = new JLabel("srcEvent");
          //srcEvent.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
          srcEvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        stValuesP.add(srcEvent);
          targEvent = new JLabel("targEvent");
          //targEvent.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
          targEvent.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        stValuesP.add(targEvent);
        stValuesP.setBorder(BorderFactory.createTitledBorder(""));
        keepSameSize(srcEvent,targEvent);
      srcTargP.add(stValuesP);
      srcTargP.add(Box.createHorizontalGlue());
      BoxLayoutUtils.clampHeight(srcTargP);
    con.add(srcTargP);

    con.add(Box.createVerticalStrut(5));

    priorityPan = new JPanel();
      priorityPan.setLayout(new BoxLayout(priorityPan,BoxLayout.X_AXIS));
      priorityPan.setOpaque(false);
      priorityPan.setBorder(BorderFactory.createTitledBorder("Priority"));
        priorityCB = buildPriorityComboBox();
        priorityPan.add(Box.createHorizontalGlue());
        priorityPan.add(priorityCB);
        priorityPan.add(Box.createHorizontalGlue());
    BoxLayoutUtils.clampHeight(priorityPan);
    con.add(priorityPan);
    con.add(Box.createVerticalStrut(5));

    delayPan = new JPanel();
      delayPan.setLayout(new BoxLayout(delayPan,BoxLayout.X_AXIS));
      delayPan.setOpaque(false);
      delayPan.setBorder(BorderFactory.createTitledBorder("Time Delay"));
        delay = new JTextField();
        delay.setOpaque(true);
        delay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        Dimension d = delay.getPreferredSize();      // only expand in horiz direction
        d.width = Integer.MAX_VALUE;
        delay.setMaximumSize(d);
      delayPan.add(delay);
      this.delayPanBorder = delayPan.getBorder();
      this.delayPanDisabledBorder = BorderFactory.createTitledBorder(new LineBorder(Color.gray),"Time Delay",
                                    TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,
                                    null,Color.gray);
    con.add(delayPan);
    con.add(Box.createVerticalStrut(5));
      conditionals = new ConditionalsPanel(edge);
    con.add(conditionals);
    con.add(Box.createVerticalStrut(5));

    myParmPanel = new JPanel();
     myParmPanel.setLayout(new BoxLayout(myParmPanel,BoxLayout.Y_AXIS));
     myParmPanel.setBorder(BorderFactory.createTitledBorder("Edge Parameters"));

       parameters = new EdgeParametersPanel(300);
       JScrollPane paramSp = new JScrollPane(parameters);
       paramSp.setBorder(null);
       paramSp.setOpaque(false);

     myParmPanel.add(paramSp);

    con.add(myParmPanel);
    con.add(Box.createVerticalStrut(5));


      JPanel buttPan = new JPanel();
      buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
      canButt = new JButton("Cancel");
      okButt = new JButton("Apply changes");
      buttPan.add(Box.createHorizontalGlue());
      buttPan.add(canButt);
      buttPan.add(okButt);
    con.add(buttPan);
    cont.add(con);

    fillWidgets();     // put the data into the widgets

    modified = false;
    okButt.setEnabled(false);
    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(locationComp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt.addActionListener(new applyButtonListener());

    myChangeListener chlis = new myChangeListener();
    conditionals.addChangeListener(chlis);
    priorityCB.addActionListener(chlis);
    delay.addCaretListener(chlis);
    priorityCB.getEditor().getEditorComponent().addKeyListener(chlis);
    parameters.addDoubleClickedListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        vEdgeParameter ep = (vEdgeParameter)event.getSource();

        boolean wasModified = EdgeParameterDialog.showDialog(EdgeInspectorDialog.this,EdgeInspectorDialog.this,ep);
        if(wasModified) {
          parameters.updateRow(ep);
          okButt.setEnabled(true);
          modified=true;
        }
      }
    });
  }
  public void setParams(Component c, Edge e)
  {
    edge = e;

    fillWidgets();
    modified = false;
    okButt.setEnabled(false);
    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }
  private void keepSameSize(JComponent a, JComponent b)
  {
    Dimension ad = a.getPreferredSize();
    Dimension bd = b.getPreferredSize();
    Dimension d = new Dimension(Math.max(ad.width,bd.width),Math.max(ad.height,bd.height));
    a.setMinimumSize(d);
    b.setMinimumSize(d);
  }

  private JComboBox buildPriorityComboBox()
  {
    priorityNames = new Vector<String>(10);
    priorityList = new ArrayList<Priority>(10);
    try {
      Class c = Class.forName("simkit.Priority");
      Field[] fa = c.getDeclaredFields();
      for(Field f : fa) {
        if(Modifier.isStatic(f.getModifiers()) && f.getType().equals(c)) {
          priorityNames.add(f.getName());
          priorityList.add((Priority)f.get(null)); // static objects
          if(f.getName().equalsIgnoreCase("default"))
            priorityDefaultIndex = priorityNames.size()-1;  // save the default one
        }
      }
      JComboBox jcb = new JComboBox(priorityNames);
      jcb.setEditable(true); // this allows anything to be intered
      return jcb;
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      return new JComboBox(new String[]{"simkit package not in class path"});
    }
  }

  private void setPriorityCBValue(String pr)
  {
    try {
      // Assume numeric comes in
      double prd= Double.parseDouble(pr);
      for(Priority p : priorityList) {
        int cmp = Double.compare(p.getPriority(), prd);
        if(cmp == 0) {
          priorityCB.setSelectedIndex(priorityList.indexOf(p));
          return;
        }
      }
      // Must have been an odd one, but we know it's a good double
      priorityCB.setSelectedItem(pr);
    }
    catch (NumberFormatException e) {
      // First try to find it in the list
      int i=0;
      for(String s : priorityNames) {
        if(s.equalsIgnoreCase(pr)) {
          priorityCB.setSelectedIndex(i);
          return;
        }
        i++;
      }

      System.err.println("Unknown edge priority: "+pr+" -- setting to DEFAULT)");
      priorityCB.setSelectedIndex(priorityDefaultIndex);
    }
  }

  private void fillWidgets()
  {
    nodeList = mod.getAllNodes();                  // todo fix

    srcEvent.setText(edge.from.getName()); //setSelectedItem(edge.from);
    targEvent.setText(edge.to.getName());  //setSelectedItem(edge.to);
    myParmPanel.setBorder(BorderFactory.createTitledBorder("Edge Parameters -- to "+targEvent.getText()));

    parameters.setArgumentList(edge.to.getArguments());  //
    parameters.setData(edge.parameters);

    if(edge instanceof SchedulingEdge) {
      if(edge.conditional == null || edge.conditional.trim().length() <= 0)
        conditionals.setText("true");
      else
        conditionals.setText(edge.conditional);
      conditionals.setComment(edge.conditionalsComment);
      if(edge.delay == null || edge.delay.trim().length() <= 0)
        delay.setText("0.0");
      else
        delay.setText(""+edge.delay);
      delay.setEnabled(true);
      delayPan.setBorder(delayPanBorder);

      setPriorityCBValue(((SchedulingEdge)edge).priority);

  }
    else {
      if(edge.conditional == null || edge.conditional.trim().length() <= 0)
        conditionals.setText("true");
      else
       conditionals.setText(edge.conditional);
      conditionals.setComment(edge.conditionalsComment);

      delay.setText("n/a");
      delay.setEnabled(false);
      delayPan.setBorder(delayPanDisabledBorder);
    }

    schedTypeSelected(edge instanceof SchedulingEdge);
  }

  private void unloadWidgets()
  {
    if(edge instanceof SchedulingEdge) {
      int idx = priorityCB.getSelectedIndex();
      if(idx < 0) {
        String s = (String)priorityCB.getSelectedItem();
        if(s.length()<=0) {
          Priority p = priorityList.get(priorityDefaultIndex);
          ((SchedulingEdge)edge).priority = ""+p.getPriority();
        }
        else
          ((SchedulingEdge)edge).priority = s;
      }
      else {
        Priority p = priorityList.get(priorityCB.getSelectedIndex());
        ((SchedulingEdge)edge).priority = ""+p.getPriority();
      }
    }
    String delaySt = delay.getText();
    if(delaySt == null || delaySt.trim().length() <= 0)
      edge.delay = "0.0";
    else
      edge.delay = delay.getText();
    String condSt = conditionals.getText();
    if(condSt == null || condSt.trim().length() <= 0)
      edge.conditional = "true";
    else
      edge.conditional = conditionals.getText();
    edge.conditionalsComment = conditionals.getComment();
    edge.parameters.clear();
    for(Iterator itr = parameters.getData().iterator(); itr.hasNext(); ) {
      edge.parameters.add(itr.next());
    }
  }

  class reverseButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      EventNode en = edge.from;
      edge.from = edge.to;
      edge.to = en;
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }
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
      if(modified) {
      // todo fix beanshell syntax checking.  I don't know if this was ever complete enough.  The example to test it
      // against is the edge from Run to Arrival in examples/ArrivalProcess.  The time delay is "interarrivalTime.generate()",
      // which is good syntax if the state variable "RandomVariate interarrivalTime" is already in the beanshell context,
      // which I don't think it is.  Must make the beanshell error checking smarter. 19 Jan 2007

      /*
        StringBuffer sb = new StringBuffer();
        if(edge instanceof SchedulingEdge) {
          sb.append("double delay = ");
          sb.append(delay.getText());
          sb.append(";\n");
        }
        sb.append("if(");
        sb.append(conditionals.getText());
        sb.append("){;}");

        if(ViskitConfig.instance().getVal("app.beanshell.warning").equalsIgnoreCase("true")) {
          String parseResults = VGlobals.instance().parseCode(edge.from,sb.toString()); //pre+conditionals.getText()+post);
          if(parseResults != null) {
            boolean ret = BeanshellErrorDialog.showDialog(parseResults,EdgeInspectorDialog.this);
            if(!ret) // don't ignore
              return;


          //  int ret = JOptionPane.showConfirmDialog(EdgeInspectorDialog.this,"Java language error:\n"+parseResults+"\nIgnore and continue?",
          //                                "Warning",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
          //  if(ret != JOptionPane.YES_OPTION)
          //    return;
          }
        }
      */
        unloadWidgets();
      }
      setVisible(false);
    }
  }

  class myChangeListener extends KeyAdapter implements ChangeListener,ActionListener,CaretListener
  {
    public void stateChanged(ChangeEvent event)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }

    public void actionPerformed(ActionEvent e)
    {
      stateChanged(null);
    }

    public void caretUpdate(CaretEvent e)
    {
      stateChanged(null);
    }

    public void keyTyped(KeyEvent e)
    {
      stateChanged(null);
    }
  }

  private void schedTypeSelected(boolean wh)
  {
    priorityPan.setVisible(wh);
    delayPan.setVisible(wh);
    schLab.setVisible(wh);
    canLab.setVisible(!wh);
  }

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if(modified) {
        int ret = JOptionPane.showConfirmDialog(EdgeInspectorDialog.this,"Apply changes?",
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
}
