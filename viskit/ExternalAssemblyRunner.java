package viskit;

import simkit.Schedule;

import javax.swing.*;
import java.util.Vector;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;

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
  
  public ExternalAssemblyRunner(String className, boolean verbose, double stopTime, List outputEntities)
  {
    this.targetClassName = className;
    this.defaultVerbose = verbose;
    this.defaultStopTime = stopTime;

    this.outputs = outputEntities;

    setTitle("Running "+className);
    runPanel = new RunnerPanel(verbose);
    this.setContentPane(runPanel);

    runPanel.vcrStop.addActionListener(new stopListener());
    runPanel.vcrPlay.addActionListener(new startResumeListener());
    runPanel.vcrRewind.addActionListener(new rewindListener());
    runPanel.vcrStep.addActionListener(new stepListener());
  //  runPanel.setVerboseListener(new verboseListener());
    // not needed.just check eachtime
    runPanel.closeButt.addActionListener(new closeListener());

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
        Schedule.startSimulation();
        //System.out.println("********************");
        System.out.flush();
        System.err.flush();

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
    if(args.length > 2) {
      v = new Vector();
      for(int i=2;i<args.length;i++)
        v.add(args[i]);
    }

    new ExternalAssemblyRunner(args[0],Boolean.valueOf(args[1]).booleanValue(),Double.valueOf(args[2]).doubleValue(), v);
  }
  public static void testmain(String[] args)
  {
    testmain(new String[]{"simkit.examples.ServerWithRenegesAssembly1_mikeTest","true","29.3"});
  }
}
