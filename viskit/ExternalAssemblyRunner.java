package viskit;

import simkit.Schedule;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 13, 2004
 * Time: 10:30:52 AM
 */

public class ExternalAssemblyRunner extends JFrame
{
  String targetClassName;
  Object targetObject;
  boolean defaultVerbose;
  double defaultStopTime;
  List outputs;
  RunnerPanel runPanel;
  ActionListener closer;

  public ExternalAssemblyRunner(String className, boolean verbose, double stopTime, List outputEntities)
  {
    this.targetClassName = className;
    this.defaultVerbose = verbose;
    this.defaultStopTime = stopTime;

    this.outputs = outputEntities;

    closer = new closeListener();
    
    doMenus();

    setTitle("Running "+className);
    runPanel = new RunnerPanel(verbose);
    this.setContentPane(runPanel);

    runPanel.vcrStop.addActionListener(new stopListener());
    runPanel.vcrPlay.addActionListener(new startResumeListener());
    runPanel.vcrRewind.addActionListener(new rewindListener());
    runPanel.vcrStep.addActionListener(new stepListener());
  //  runPanel.setVerboseListener(new verboseListener());
    // not needed.just check eachtime

    runPanel.closeButt.addActionListener(closer);

    Class targetClass = null;
    try {
      targetClass = Class.forName(targetClassName);
      targetObject = targetClass.newInstance();
    }

    // Error cases for instantiation:
    catch (ClassNotFoundException e) {
      JOptionPane.showMessageDialog(null,className+" could not be found.",
           "Internal Error",JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }
    catch (InstantiationException e) {
      JOptionPane.showMessageDialog(null,className+" could not be instantiated.",
           "Internal Error",JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }
    catch (IllegalAccessException e) {
      JOptionPane.showMessageDialog(null,className+" could not be accessed.",
           "Internal Error",JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }

    twiddleButtons(ExternalAssemblyRunner.REWIND);
    Schedule.reset();

    runPanel.vcrStopTime.setText(""+defaultStopTime);
    runPanel.vcrSimTime.setText(Schedule.getSimTimeStr());

    this.setSize(750,500);
    this.setLocation(300,300);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    this.setVisible(true);
    runPanel.splPn.setDividerLocation(0.85d);       // clumsy way
  }

  private String borderString = "******************************";

  private void dumpOutputs()
  {
    System.out.println("\n"+borderString);
    boolean first=true;
    for(Iterator itr = outputs.iterator();itr.hasNext();) {
      try {
        Field fld = targetObject.getClass().getField((String)itr.next());
        if(first)
          first=false;
        else
          System.out.println();
        System.out.println(fld.getName()+" dump:\n"+fld.get(targetObject).toString());
      }
      catch (Exception e) {
       // e.printStackTrace();
      }
    }
    System.out.println(borderString+"\n");
  }

  class startResumeListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      Schedule.setPauseAfterEachEvent(false);
      twiddleButtons(ExternalAssemblyRunner.START);

      _start();
    }
  }
  class stopListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      Schedule.stopSimulation();
      twiddleButtons(ExternalAssemblyRunner.STOP);
    }
  }
  class rewindListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      Schedule.reset();
      twiddleButtons(ExternalAssemblyRunner.REWIND);
      runPanel.vcrSimTime.setText(Schedule.getSimTimeStr());
    }
  }
  class stepListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      Schedule.setPauseAfterEachEvent(true);
      twiddleButtons(ExternalAssemblyRunner.STEP);
      _start();
    }
  }
  class closeListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      System.exit(0);
    }
  }
  private void _start()
  {
    Schedule.stopAtTime(getStopTime());
    Schedule.setVerbose(getVerbose());
    new Thread(new Runnable()
    {
      public void run()
      {
        dumpOutputs();
        Schedule.startSimulation();
        dumpOutputs();

        // update GUI stuff in GUI thread
        SwingUtilities.invokeLater(new Runnable(){
          public void run()
          {
            runPanel.vcrSimTime.setText(Schedule.getSimTimeStr());
            twiddleButtons(ExternalAssemblyRunner.STOP);
          }});
    }}).start();
  }

  double getStopTime()
  {
    return Double.parseDouble(runPanel.vcrStopTime.getText());
  }

  boolean getVerbose()
  {
    return runPanel.vcrVerbose.isSelected();
  }

  public static final int START = 0;
  public static final int STOP = 1;
  public static final int STEP = 2;
  public static final int REWIND = 3;

  private void twiddleButtons(int evnt)
  {
    switch(evnt) {
      case START:
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrStop.setEnabled(true);
        runPanel.vcrStep.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        break;
      case STOP:
        runPanel.vcrPlay.setEnabled(true);
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrStep.setEnabled(true);
        runPanel.vcrRewind.setEnabled(true);
        break;
      case STEP:
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrStop.setEnabled(true);
        runPanel.vcrStep.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        break;
      case REWIND:
        runPanel.vcrPlay.setEnabled(true);
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrStep.setEnabled(true);
        runPanel.vcrRewind.setEnabled(false);
        break;
      default:
        System.err.println("Bad event in ExternalAssemblyRunner");
        break;
    }
  }
  private void doMenus()
  {
    JMenuBar mb = new JMenuBar();
    JMenu file = new JMenu("File");
    JMenuItem close = new JMenuItem("close");
    JMenu edit = new JMenu("Edit");
    JMenuItem copy = new JMenuItem("copy");
    JMenuItem selAll = new JMenuItem("select all");

    close.addActionListener(closer);
    copy.addActionListener(new copyListener());
    selAll.addActionListener(new selectAllListener());
    
    file.add(close);
    edit.add(copy);
    edit.add(selAll);
    mb.add(file);
    mb.add(edit);
    setJMenuBar(mb);
  }

  class copyListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      String s = runPanel.soutTA.getSelectedText() + "\n" + runPanel.serrTA.getSelectedText();
      StringSelection ss = new StringSelection(s);
      Clipboard clpbd = Toolkit.getDefaultToolkit().getSystemClipboard();
      clpbd.setContents(ss,ss);
    }
  }
  class selectAllListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      runPanel.soutTA.selectAll();
      runPanel.serrTA.selectAll();
    }
  }
  class timekeeper extends simkit.SimEntityBase
  {
    public synchronized void doTick()
    {
      System.out.println("doKeepTime");
      this.waitDelay("Tick",0.0d);
    }
  }
  /**
   * We have typically been fired-off from Viskit.  Our classpath has been set.  We just
   * instantiate this object, and pass it the class name, whether defaultVerbose output is desired or not,
   * and the list of output objects which should be (periodically dumped).

   * @param args
   */

  public static void main(String[] args)
  {
     if(args.length < 2) {
      JOptionPane.showMessageDialog(null,"Wrong number of parameters to ExternalAssemblyRunner.main().",
           "Internal Error",JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }

    viskit.Main.setLandFandFonts(); // same as editor

    Vector v=null;
    if(args.length > 3) {
      v = new Vector();
      for(int i=3;i<args.length;i++)
        v.add(args[i]);
    }

    new ExternalAssemblyRunner(args[0],Boolean.valueOf(args[1]).booleanValue(),Double.valueOf(args[2]).doubleValue(), v);
  }
}
