package viskit;

import simkit.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
  ActionListener closer,saver;
  static String lineSep = System.getProperty("line.separator");
  JMenuBar myMenuBar;

  public ExternalAssemblyRunner(String className, boolean verbose, double stopTime, List outputEntities)
  {
    this(false,className,verbose,stopTime,outputEntities);
  }

  public ExternalAssemblyRunner(boolean contentOnly, String className, boolean verbose, double stopTime, List outputEntities)
  {
    this.targetClassName = className;
    this.defaultVerbose = verbose;
    this.defaultStopTime = stopTime;

    this.outputs = outputEntities;

    saver = new saveListener();
    runPanel = new RunnerPanel(verbose,false);
    doMenus(contentOnly);

    if(!contentOnly) {
      closer = new closeListener();
      setTitle("Assembly Run Panel -- "+className);
      setContentPane(runPanel);
    }

    runPanel.vcrStop.addActionListener(new stopListener());
    runPanel.vcrPlay.addActionListener(new startResumeListener());
    runPanel.vcrRewind.addActionListener(new rewindListener());
    runPanel.vcrStep.addActionListener(new stepListener());
  //  runPanel.setVerboseListener(new verboseListener());
    // not needed.just check eachtime

    runPanel.closeButt.addActionListener(closer);

    Class targetClass = null;
    try {
      targetClass = Vstatics.classForName(targetClassName);
        if(targetClass == null) throw new ClassNotFoundException();
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

    if(!contentOnly) {
      setSize(750,500);
      setLocation(300,300);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setVisible(true);
    }
  }

  public JComponent getContent()
  {
    return runPanel;
  }
  public JMenuBar getMenus()
  {
    return myMenuBar;
  }
  public JMenuItem getQuitMenuItem()
  {
    return null;
  }

  private String borderString = "******************************";

  private void dumpOutputs()
  {
    if(outputs == null)
      return;

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
  private JFileChooser saveChooser;

  class saveListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if(saveChooser == null) {
        saveChooser = new JFileChooser(System.getProperty("user.dir"));
      }
      File fil = getUniqueName("AssemblyOutput.txt",saveChooser.getCurrentDirectory());
      saveChooser.setSelectedFile(fil);

      int retv = saveChooser.showSaveDialog(ExternalAssemblyRunner.this);
      if(retv != JFileChooser.APPROVE_OPTION)
        return;

      fil = saveChooser.getSelectedFile();
      if(fil.exists()) {
        int r = JOptionPane.showConfirmDialog(ExternalAssemblyRunner.this, "File exists.  Overwrite?","Confirm",
                                              JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(r != JOptionPane.YES_OPTION)
          return;
      }

      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(fil));

        bw.write(runPanel.soutTA.getText());
        bw.write(lineSep);
        bw.write(runPanel.serrTA.getText());
        bw.flush();
        bw.close();
      }
      catch (IOException e1) {
        JOptionPane.showMessageDialog(ExternalAssemblyRunner.this,e1.getMessage(),"I/O Error,",JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  // dup of EventGraphViewFrame
  private File getUniqueName(String suggName, File parent)
  {
    String appnd = "";
    String suffix = "";

    int lastDot = suggName.lastIndexOf('.');
    if(lastDot != -1) {
      suffix = suggName.substring(lastDot);
      suggName = suggName.substring(0,lastDot);
    }
    int count = -1;
    File fil = null;
    do {
      fil = new File(parent,suggName + appnd + suffix);
      appnd = "" + ++count;
    }
    while (fil.exists());

    return fil;
  }

  private void _start()
  {
    Schedule.stopAtTime(getStopTime());
    Schedule.setVerbose(getVerbose());
    // testing Schedule.setReallyVerbose(true);
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
  private void doMenus(boolean contentOnly)
  {
    myMenuBar = new JMenuBar();
    JMenu file = new JMenu("File");
    JMenuItem save  = new JMenuItem("Save output streams");
    JMenuItem close = new JMenuItem("Close");
    JMenu edit = new JMenu("Edit");
    JMenuItem copy = new JMenuItem("Copy");
    JMenuItem selAll = new JMenuItem("Select all");
    JMenuItem clrAll = new JMenuItem("Clear all");

    save.addActionListener(saver);
    close.addActionListener(closer);
    copy.addActionListener(new copyListener());
    selAll.addActionListener(new selectAllListener());
    clrAll.addActionListener(new clearListener());

    file.add(save);
    file.add(close);
    edit.add(copy);
    edit.add(selAll);
    edit.add(clrAll);
    myMenuBar.add(file);
    myMenuBar.add(edit);

    if(!contentOnly)
      setJMenuBar(myMenuBar);
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
  class clearListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      runPanel.soutTA.setText(null);
      runPanel.serrTA.setText(null);
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
    Vector v = null;
    try {
      v = _mainCommon(args);
    }
    catch (Exception e) {
      System.exit(-1);
    }

    viskit.Main.setLandFandFonts(); // same as editor
    new ExternalAssemblyRunner(args[0],
        Boolean.valueOf(args[1]).booleanValue(),
        Double.valueOf(args[2]).doubleValue(), v);
  }

  private static String errMsg = "Wrong number of parameters to ExternalAssemblyRunner.main().";
  private static Vector _mainCommon(String[] args) throws Exception
  {
    if (args.length < 2) {
      JOptionPane.showMessageDialog(null, errMsg,
          "Internal Error", JOptionPane.ERROR_MESSAGE);
      throw new Exception(errMsg);
    }

    Vector v = null;
    if (args.length > 3) {
      v = new Vector();
      for (int i = 3; i < args.length; i++)
        v.add(args[i]);
    }

    return v;
  }

  /** An alternate entry point which doesn't assume we're to run in a different vm. */
  public static ExternalAssemblyRunner main2(String[] args)
  {
    Vector v = null;
    try {
      v = _mainCommon(args);
    }
    catch (Exception e) {
      return null;
    }

    return new ExternalAssemblyRunner(true, args[0],
        Boolean.valueOf(args[1]).booleanValue(),
        Double.valueOf(args[2]).doubleValue(), v);
  }
}
