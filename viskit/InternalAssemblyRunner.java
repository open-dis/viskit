/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.MovesInstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 26, 2005
 * @since 3:43:51 PM
 */

package viskit;

import edu.nps.util.DirectoryWatch;
import java.lang.reflect.Method;
import simkit.Schedule;
import viskit.xsd.assembly.ViskitAssembly;
import viskit.xsd.assembly.BasicAssembly;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.EventListener;

/** Analogous to ExternalAssemblyRunner, but puts gui in calling thread and runs
 * sim in external VM.
 */
public class InternalAssemblyRunner implements OpenAssembly.AssyChangeListener
{
  String targetClassName;
  String targetClassPath;
  List outputs;
  RunnerPanel2 runPanel;
  ActionListener closer,saver;
  static String lineSep = System.getProperty("line.separator");
  JMenuBar myMenuBar;
  BufferedReader backChan;

  private Process externalSimRunner;
  private String analystReportTempFile; // external runner saves a file

  public InternalAssemblyRunner()
  {
    saver = new saveListener();
    runPanel = new RunnerPanel2(null,true); //"Initialize using Assembly Edit tab, then Run button",true);
    doMenus();
    runPanel.vcrStop.addActionListener(new stopListener());
    runPanel.vcrPlay.addActionListener(new startResumeListener());
    runPanel.vcrRewind.addActionListener(new rewindListener());
    runPanel.vcrStep.addActionListener(new stepListener());
    runPanel.vcrVerbose.addActionListener(new verboseListener());
    //runPanel.saveParms.addActionListener(new saveParmListener());

    runPanel.vcrStop.setEnabled(false);
    runPanel.vcrPlay.setEnabled(false);
    runPanel.vcrRewind.setEnabled(false);
    runPanel.vcrStep.setEnabled(false);

    //runPanel.numRepsTF.addActionListener(new repsListener()); // this gets directly read at initRun()

    twiddleButtons(OFF);
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

  public String getHandle()
  {
    return "";
  }

  AnalystReportPanel reportPanel;
  public void setAnalystReportGUI(AnalystReportPanel pan)
  {
    reportPanel = pan;
  }

  public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param)
  {
    switch (action) {
      case JAXB_CHANGED:
        break;
      case NEW_ASSY:
        // fill out the parameters here..
        // todo
        break;

      case CLOSE_ASSY:
        break;
      case PARAM_LOCALLY_EDITTED:
        break;

      default:
        System.err.println("Program error InternalAssemblyRunner.assyChanged");
    }
  }

  private String[] myCmdLine;

  /**
   * Get param indices from AssemblyController statics
   * @param parms
   */
  public void initParams(String[]parms)
  {
    parms[AssemblyController.EXEC_RUNNER_CLASS_NAME] = "viskit.InternalAssemblyRunner$ExternalSimRunner";
    myCmdLine = parms;

    targetClassName = parms[AssemblyController.EXEC_TARGET_CLASS_NAME];
    doTitle(targetClassName);

    targetClassPath = parms[AssemblyController.EXEC_CLASSPATH];
    boolean defaultVerbose = Boolean.valueOf(parms[AssemblyController.EXEC_VERBOSE_SWITCH]).booleanValue();
    double defaultStopTime = Double.parseDouble(parms[AssemblyController.EXEC_STOPTIME_SWITCH]);

    runPanel.vcrStopTime.setText(""+defaultStopTime);
    runPanel.vcrSimTime.setText("0"); //Schedule.getSimTimeStr());
    runPanel.vcrVerbose.setSelected(defaultVerbose);

    try {
      fillRepWidgetsFromBasicAssemblyObject(targetClassName);
    }
    catch (Throwable throwable) {
      JOptionPane.showMessageDialog(runPanel,"Error initializing Assembly object:\n"+throwable.getMessage(),"Java Error",JOptionPane.ERROR_MESSAGE);
      twiddleButtons(OFF);
      throwable.printStackTrace();
      return;
    }
    twiddleButtons(InternalAssemblyRunner.REWIND);

    if (externalSimRunner != null) {
      send(ExternalSimRunner.QUIT);
      externalSimRunner = null;
    }
  }

  private void fillRepWidgetsFromBasicAssemblyObject(String clName) throws Throwable
  {
    Class targetClass;
    Object targetObject;

    // Assembly has been compile by now
    VGlobals.instance().resetWorkClassLoader();
    Thread.currentThread().setContextClassLoader(VGlobals.instance().getWorkClassLoader());
    targetClass = Vstatics.classForName(targetClassName);
    if (targetClass == null) throw new ClassNotFoundException();
    targetObject = targetClass.newInstance();
    // in order to see BasicAssembly this thread has to have
    // the same loader as the one used since they don't
    // share the same simkit or viskit.
   
    Method getNumberReplications = targetClass.getMethod("getNumberReplications");
    Method isSaveReplicationData = targetClass.getMethod("isSaveReplicationData");
    Method isPrintReplicationReports = targetClass.getMethod("isPrintReplicationReports");
    Method isPrintSummaryReport = targetClass.getMethod("isPrintSummaryReport");
    Method isVerbose = targetClass.getMethod("isVerbose");
    Method getStopTime = targetClass.getMethod("getStopTime");
    
    runPanel.numRepsTF.setText("" + (Integer) getNumberReplications.invoke(targetObject));
    runPanel.saveRepDataCB.setSelected((Boolean) isSaveReplicationData.invoke(targetObject));
    runPanel.printRepReportsCB.setSelected((Boolean) isPrintReplicationReports.invoke(targetObject));
    runPanel.printSummReportsCB.setSelected((Boolean) isPrintSummaryReport.invoke(targetObject));
    runPanel.vcrVerbose.setSelected((Boolean) isVerbose.invoke(targetObject));
    runPanel.vcrStopTime.setText("" + (Double) getStopTime.invoke(targetObject));
  }

  PrintWriter pWriter;
  private void initRun()
  {
    analystReportTempFile = null;
    if(externalSimRunner == null) {
      // disable this...it's not showing up because of the .exec below which
      // is also being done in the Swing thread...redesign
      // showLaunchingDialog(2*1000);
      try {
        externalSimRunner = Runtime.getRuntime().exec(myCmdLine);
        runPanel.setStreams(externalSimRunner.getInputStream(),externalSimRunner.getErrorStream());
        // set autoflush after every println
        pWriter = new PrintWriter(new OutputStreamWriter(externalSimRunner.getOutputStream()),true);
      }
      catch (Exception e) {
        System.out.println("Error launching virtual machine: "+e.getMessage());
        return;
      }
      // not right placesend(ExternalSimRunner.SCHEDULE_RESET);
      //pWriter.flush();
    }
    send(ExternalSimRunner.ASSY_NUM_REPS);
    send(runPanel.numRepsTF.getText().trim());
    send(ExternalSimRunner.ASSY_SAVE_REP_DATA);
    send(runPanel.saveRepDataCB.isSelected());
    send(ExternalSimRunner.ASSY_PRINT_REP_REPORTS);
    send(runPanel.printRepReportsCB.isSelected());
    send(ExternalSimRunner.ASSY_PRINT_SUMM_REPORTS);
    send(runPanel.printSummReportsCB.isSelected());

  }

  private void showLaunchingDialog(int msToShow)
  {
    JDialog dia = null;
    Frame[] frames = Frame.getFrames();
    int i;
    for(i=0;i<frames.length;i++) {
      if(frames[i].isVisible())
        break;
    }
    if(i < frames.length)
      dia = new JDialog(frames[i]);
    else
      dia = new JDialog();   // no owner
    dia.setModal(false);
    dia.setUndecorated(true);
    JPanel con = new JPanel(new BorderLayout());
    JLabel lab = new JLabel("Launching external virtual machine...");
    con.setBorder(new JTextArea().getBorder());
    lab.setBorder(new EmptyBorder(20,20,20,20));
    con.add(lab,BorderLayout.CENTER);
    dia.setContentPane(con);
    dia.pack();
    dia.setLocationRelativeTo(runPanel);
    dia.setVisible(true);

    final Dialog diaFin = dia;

    javax.swing.Timer timr = new javax.swing.Timer(msToShow,new ActionListener()
    {
      public void actionPerformed(ActionEvent actionEvent)
      {
        diaFin.setVisible(false);
      }
    });
    timr.setRepeats(false);
    timr.start();
  }
  
  class startResumeListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      runPanel.vcrSimTime.setText("0.0");    // because no pausing
      twiddleButtons(InternalAssemblyRunner.START);
      Thread t = new Thread(new Runnable()
      {
        public void run()
        {
          initRun();
          send(ExternalSimRunner.SCHEDULE_SETPAUSEAFTEREACHEVENT);
          send(false);
          send(ExternalSimRunner.SCHEDULE_RESET); // because no pausing
          _startLocalEnd();              // sends start command in different thread
        }
      }, "initRun");
      t.setPriority(Thread.NORM_PRIORITY);
      t.start();
    }
  }

  class stepListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      twiddleButtons(InternalAssemblyRunner.STEP);
      Thread t = new Thread(new Runnable()
      {
        public void run()
        {
          initRun();
          send(ExternalSimRunner.SCHEDULE_SETPAUSEAFTEREACHEVENT);
          send(true);
          _startLocalEnd();
        }
      }, "stepInitRun");
      t.setPriority(Thread.NORM_PRIORITY);
      t.start();
    }
  }

  class stopListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      // not with no pause butt: send(ExternalSimRunner.SCHEDULE_PAUSESIMULATION);   // Our stop button pauses, because we want to rewind
      send(ExternalSimRunner.SCHEDULE_STOPSIMULATION);
      twiddleButtons(InternalAssemblyRunner.STOP);
      send(ExternalSimRunner.SCHEDULE_GETSIMTIMESTR);
    }
  }
  class rewindListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      send(ExternalSimRunner.SCHEDULE_RESET);
      twiddleButtons(InternalAssemblyRunner.REWIND);
      send(ExternalSimRunner.SCHEDULE_GETSIMTIMESTR);
    }
  }
  class verboseListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if(externalSimRunner != null) {
        send(ExternalSimRunner.SCHEDULE_SETVERBOSE);
        send(((JCheckBox)e.getSource()).isSelected());
      }
    }
  }

  /* unused, currenlty this data gets set at initRun()
  * right when the play button gets toggled
   class repsListener implements ActionListener
 {
     public void actionPerformed(ActionEvent e)
     {
         System.out.println(e);
         int reps = Integer.parseInt(runPanel.numRepsTF.getText());
         send(ExternalSimRunner.ASSY_NUM_REPS);
         send(runPanel.numRepsTF.getText().trim());
        
         
     }
 }
  */
  /* to handle the saving of the execution parms to the assembly file
  class saveParmListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      //todo getexecution parms from runpanel
      //todo makesure the dirwatch listeners don't get hit
      //todo save the parmeters into the xml.
    }
  }
  */
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

      int retv = saveChooser.showSaveDialog(null); //InternalAssemblyRunner.this);
      if(retv != JFileChooser.APPROVE_OPTION)
        return;

      fil = saveChooser.getSelectedFile();
      if(fil.exists()) {
        int r = JOptionPane.showConfirmDialog(null/*InternalAssemblyRunner.this*/, "File exists.  Overwrite?","Confirm",
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
        JOptionPane.showMessageDialog(null/*InternalAssemblyRunner.this*/,e1.getMessage(),"I/O Error,",JOptionPane.ERROR_MESSAGE);
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

  private void _startLocalEnd()
  {
    if(backChan == null) {
    try {
      ServerSocket svrsocket = new ServerSocket(0);  // any port

      send(ExternalSimRunner.OPEN_BACKCHANNEL);
      send("" + svrsocket.getLocalPort());

      svrsocket.setSoTimeout(3000); // 3 secs should be plenty
      Socket sock = svrsocket.accept();
      backChan = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      initBackChanReader();
      svrsocket.close();
    }
    catch (SocketTimeoutException tmo) {
      _startLocalBail("Back channel socket timeout");
      return;
    }
    catch (IOException e) {
      _startLocalBail("Bad Back Channel build: " + e.getMessage());
      return;
    }
    }
    send(ExternalSimRunner.SCHEDULE_STOPATTIME);
    send(getStopTime());
    send(ExternalSimRunner.SCHEDULE_SETVERBOSE);
    send(getVerbose());
    send(ExternalSimRunner.DUMP_OUTPUTS);

    send(ExternalSimRunner.SCHEDULE_STARTSIMULATION);
  }

  private void _startLocalBail(String msg)
  {
    System.err.println(msg);
    if (externalSimRunner != null){
      externalSimRunner.destroy();
      externalSimRunner = null;
    }
    backChan = null;
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        //twiddleButtons(InternalAssemblyRunner.STOP);
        runPanel.vcrStop.doClick();
      }
    });
  }

  private void initBackChanReader()
  {
    Thread bkChanRdr = new Thread(new BackChannelReader(), "BackChannelReader");
    bkChanRdr.setPriority(Thread.NORM_PRIORITY);
    bkChanRdr.start();
  }

  String returnedSimTime;

  class BackChannelReader implements Runnable
  {
    public void run()
    {
      try {
        while (true) {
          String line = backChan.readLine();
          if (line.startsWith(ExternalSimRunner.RESP_BACKCHANNEL))
            ;
          else if (line.startsWith(ExternalSimRunner.RESP_TIMESTR)) {
            returnedSimTime = line.substring(ExternalSimRunner.RESP_TIMESTR.length());
            SwingUtilities.invokeLater(new Runnable()
            {
              public void run()
              {
                runPanel.vcrSimTime.setText(returnedSimTime);
              }
            });
          }
          else if (line.startsWith(ExternalSimRunner.RESP_SIMSTOPPED)) {
            send(ExternalSimRunner.DUMP_OUTPUTS);
            send(ExternalSimRunner.SCHEDULE_GETSIMTIMESTR);
            SwingUtilities.invokeLater(new Runnable()
            {
              public void run()
              {
                twiddleButtons(InternalAssemblyRunner.STOP);
                if(InternalAssemblyRunner.this.analystReportTempFile != null &&
                   InternalAssemblyRunner.this.reportPanel != null)
                  signalReportReady();
              }
            });
          }
          else if (line.startsWith(ExternalSimRunner.RESP_ANALYSTREPORTFILE)) {
            analystReportTempFile = line.substring(ExternalSimRunner.RESP_ANALYSTREPORTFILE.length());
          }
        }
      }
      catch (Exception e) {
        System.out.println("Back channel reader dead");
      }
    }
  }

  private void signalReportReady()
  {
    reportPanel.setReportXML(analystReportTempFile); 
  }

  double getStopTime()
  {
    return Double.parseDouble(runPanel.vcrStopTime.getText());
  }

  boolean getVerbose()
  {
    return runPanel.vcrVerbose.isSelected();
  }

  private String sendAndWait(int cmd) throws IOException
  {
    send(cmd);
    return backChan.readLine();
  }

  private void send(int cmd)
  {
    //System.out.println("Queing "+ExternalSimRunner.cmdNames[cmd]);
    pWriter.println(cmd);
  }

  private void send(boolean b)
  {
    //System.out.println("Queing "+b);
    pWriter.println(b);
  }

  private void send(String s)
  {
    //System.out.println("Queing "+s);
    pWriter.println(s);
  }

  private void send(double d)
  {
    //System.out.println("Queing "+d);
    pWriter.println(d);
  }

  public static final int START = 0;
  public static final int STOP = 1;
  public static final int STEP = 2;
  public static final int REWIND = 3;
  public static final int OFF = 4;

  private void twiddleButtons(int evnt)
  {
    switch(evnt) {
      case START:
        //System.out.println("twbutt start");
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrStop.setEnabled(true);
        runPanel.vcrStep.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        break;
      case STOP:
        //System.out.println("twbutt stop");
        runPanel.vcrPlay.setEnabled(true);
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrStep.setEnabled(true);
        runPanel.vcrRewind.setEnabled(true);
        break;
      case STEP:
        //System.out.println("twbutt step");
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrStop.setEnabled(true);
        runPanel.vcrStep.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        break;
      case REWIND:
        //System.out.println("twbutt rewind");
        runPanel.vcrPlay.setEnabled(true);
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrStep.setEnabled(true);
        runPanel.vcrRewind.setEnabled(false);
        break;
      case OFF:
        //System.out.println("twbutt off");
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrStep.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        break;
      default:
        System.err.println("Bad event in InternalAssemblyRunner");
        break;
    }
  }
  private void doMenus()
  {
    myMenuBar = new JMenuBar();
    JMenu file = new JMenu("File");
    JMenuItem save  = new JMenuItem("Save output streams");
    JMenu edit = new JMenu("Edit");
    JMenuItem copy = new JMenuItem("Copy");
    JMenuItem selAll = new JMenuItem("Select all");
    JMenuItem clrAll = new JMenuItem("Clear all");

    save.addActionListener(saver);
    copy.addActionListener(new copyListener());
    selAll.addActionListener(new selectAllListener());
    clrAll.addActionListener(new clearListener());

    file.add(save);

    file.addSeparator();
    file.add(new JMenuItem("Settings"));

    edit.add(copy);
    edit.add(selAll);
    edit.add(clrAll);
    myMenuBar.add(file);
    myMenuBar.add(edit);
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
      runPanel.soutTA.requestFocus();
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
  /**
   * We have typically been fired-off from Viskit.  Our classpath has been set.  We just
   * instantiate this object, and pass it the class name, whether defaultVerbose output is desired or not,
   * and the list of output objects which should be (periodically dumped).

   * @param args
   */

 /*
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
    new InternalAssemblyRunner(args[0],
        Boolean.valueOf(args[1]).booleanValue(),
        Double.valueOf(args[2]).doubleValue(), v);
  }

  private static String errMsg = "Wrong number of parameters to InternalAssemblyRunner.main().";
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

  public static InternalAssemblyRunner main2(String[] args)
  {
    Vector v = null;
    try {
      v = _mainCommon(args);
    }
    catch (Exception e) {
      return null;
    }

    return new InternalAssemblyRunner(true, args[0],
        Boolean.valueOf(args[1]).booleanValue(),
        Double.valueOf(args[2]).doubleValue(), v);
  }
 */
  /** This is the class that gets run when we launch */
  public static class ExternalSimRunner
  {
    public static final int SCHEDULE_RESET = 0;                  // Schedule.reset();
    public static final int SCHEDULE_GETSIMTIMESTR = 1;          // Schedule.getSimTimeStr());
    public static final int SCHEDULE_SETPAUSEAFTEREACHEVENT = 2; // Schedule.setPauseAfterEachEvent(false);
    public static final int SCHEDULE_STOPSIMULATION = 3;         // Schedule.stopSimulation();
    public static final int SCHEDULE_PAUSESIMULATION = 4;
    public static final int SCHEDULE_SETVERBOSE = 5;             // Schedule.setVerbose(getVerbose());
    public static final int SCHEDULE_STARTSIMULATION =6;         // Schedule.startSimulation();
    public static final int SCHEDULE_STOPATTIME = 7;             // Schedule.stopAtTime(double);

    public static final int DUMP_OUTPUTS = 8;
    public static final int OPEN_BACKCHANNEL = 9;
    public static final int QUIT = 10;

    public static final int ASSY_NUM_REPS = 11;
    public static final int ASSY_SAVE_REP_DATA = 12;
    public static final int ASSY_PRINT_REP_REPORTS = 13;
    public static final int ASSY_PRINT_SUMM_REPORTS= 14;

    public static String[] cmdNames = {"Reset","GetSimTimeString","SetStep",
                                       "StopSim","PauseSim","SetVerbose","StartSim","StopAtTime",
                                       "DumpOutputs","OpenBackchannel","Quit",
                                       "SetNumReps","SaveRepData","PrintRepReports","PrintSummReports"};

    public static final String RESP_TIMESTR           = "R1/";
    public static final String RESP_SIMSTOPPED        = "R2/";
    public static final String RESP_BACKCHANNEL       = "R3/";
    public static final String RESP_ANALYSTREPORTFILE = "R4/";

    private Vector outputs = new Vector();
    private PrintStream extBackChannel;
    private Object targetObject;
    private BasicAssembly targetAssembly;

    public static void main(String[] args)
    {
      new ExternalSimRunner(args);
    }

    private ExternalSimRunner(String[] args)
    {
      Class targetClass = null;
      String targetClassName = args[AssemblyController.APP_TARGET_CLASS_NAME];
      String errMsg = null;
      try {
        targetClass = Vstatics.classForName(targetClassName);
          if(targetClass == null) throw new ClassNotFoundException();
        targetObject = targetClass.newInstance();
        if(! (targetObject instanceof BasicAssembly))
          errMsg = "targetClassName not instanceof BasicAssembly";
      }
      // Error cases for instantiation:
      catch (ClassNotFoundException e) {
        errMsg = targetClassName+" could not be found.";
      }
      catch (InstantiationException e) {
        errMsg = targetClassName+" could not be instantiated.";
      }
      catch (IllegalAccessException e) {
        errMsg = targetClassName+" could not be accessed.";
      }

      if(errMsg != null) {
        JOptionPane.showMessageDialog(null,errMsg);
        System.out.println(errMsg);
        System.out.flush();
        System.exit(-1);
      }
      targetAssembly = (BasicAssembly)targetObject;
      targetAssembly.setVerbose(Boolean.valueOf(args[AssemblyController.APP_VERBOSE_SWITCH]).booleanValue());
      targetAssembly.setStopTime(Double.parseDouble(args[AssemblyController.APP_STOPTIME_SWITCH]));
      outputs.clear();
      if (args != null && args.length > 0) {
        for (int i = AssemblyController.APP_FIRST_ENTITY_NAME; i < args.length; i++)
          outputs.add(args[i]);
      }

      sendAnalystReportPath();  // if backchannel open at this point, else when it is

      Schedule.reset();

      execLoop();
    }

    Thread simThread;
    private void execLoop()
    {
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try {

        while(true) {
          String cmd = in.readLine();

          if(cmd == null)
            System.exit(0);

          int cmdIn  = Integer.parseInt(cmd);
          switch(cmdIn) {
            case OPEN_BACKCHANNEL:
              String port = in.readLine();
              openChannel(port);
              if(extBackChannel != null) {
                extBackChannel.println(RESP_BACKCHANNEL+"BackChannelOK");
                sendAnalystReportPath();
              }
              break;
            case SCHEDULE_RESET:
              System.err.println("Got reset (not error)");
              // todo remove the check if we always get a ViskitAssembly
              if(targetAssembly instanceof ViskitAssembly)
                ((ViskitAssembly)targetAssembly).reset();
              else
                Schedule.reset();
              break;
            case SCHEDULE_STOPSIMULATION:
              System.err.println("Got stop sim (not error)");
              // todo remove the check if we always get a ViskitAssembly
              if(targetAssembly instanceof ViskitAssembly)
                ((ViskitAssembly)targetAssembly).setStopRun(true);
              else
                Schedule.stopSimulation();
              break;
            case SCHEDULE_PAUSESIMULATION:
              System.err.println("Got pause sim (not error)");
              Schedule.pause();
              break;
            case SCHEDULE_STARTSIMULATION:
              System.err.println("Got start sim (not error)");

              if (simThread != null) {
                System.err.println("Previous run not done");
                break;
              }
              simThread = new Thread((Runnable) targetObject, "SimThread");
              Thread watcherThread = new Thread(new Runnable()
              {
                public void run()
                {
                  try {
                    simThread.start();   // This does Schedule.startSimulation()
                    simThread.join();
                  }
                  catch (InterruptedException e) {
                  }
                  simThread = null;
                  if ( extBackChannel != null ) {
                    extBackChannel.println(RESP_SIMSTOPPED + "SimStopped");
                  }
                  System.err.println("Sim stopped (not error)");
                }
              }, "simEndWatcher");
              watcherThread.setPriority(Thread.NORM_PRIORITY);
              watcherThread.start();

              break;
            case SCHEDULE_SETPAUSEAFTEREACHEVENT:
              String which = in.readLine();
              boolean bool = Boolean.valueOf(which).booleanValue();
              System.err.println("Got setPauseAfterEachEvent (not error) = "+bool);;
              Schedule.setPauseAfterEachEvent(bool);
              break;
            case SCHEDULE_SETVERBOSE:
              String verb = in.readLine();
              bool = Boolean.valueOf(verb).booleanValue();
              System.err.println("Got setVerbose (not error) = "+bool);
              targetAssembly.setVerbose(bool);//Schedule.setVerbose(bool);
              break;
            case SCHEDULE_GETSIMTIMESTR:
              System.err.println("Got getSimTimeString (not error)");
              extBackChannel.println(RESP_TIMESTR+Schedule.getSimTimeStr());
              break;
            case SCHEDULE_STOPATTIME:
              String tm = in.readLine();
              System.err.println("Got stopAtTime (not error) =" + tm);
              targetAssembly.setStopTime(Double.parseDouble(tm));//Schedule.stopAtTime(Double.parseDouble(tm));
              break;
            case ASSY_NUM_REPS:
              String n = in.readLine();
              int nint = 0;
              try {
                nint = Integer.parseInt(n);
                if(!(targetAssembly instanceof ViskitAssembly))
                    targetAssembly.setNumberReplications(1); // todo when fixed   nint);
                else {
                    ((ViskitAssembly)targetAssembly).setNumberReplications(nint);
                }

              }
              catch (NumberFormatException e) {
                System.err.println("Error parsing number of reps: "+n);
              }
              System.err.println("Got ASSY_NUM_REPS (not error) " + nint);
              break;

            case ASSY_SAVE_REP_DATA:
              String boolSt = in.readLine();
              bool = Boolean.valueOf(boolSt).booleanValue();
              targetAssembly.setSaveReplicationData(bool);
              System.err.println("Got ASSY_SAVE_REP_DATA (not error) "+bool);
              break;

            case ASSY_PRINT_REP_REPORTS:
              boolSt = in.readLine();
              bool = Boolean.valueOf(boolSt).booleanValue();
              targetAssembly.setPrintReplicationReports(bool);
              System.err.println("Got ASSY_PRINT_REP_REPORTS (not error) "+bool);
              break;

            case ASSY_PRINT_SUMM_REPORTS:
              boolSt = in.readLine();
              bool = Boolean.valueOf(boolSt).booleanValue();
              targetAssembly.setPrintSummaryReport(bool);
              System.err.println("Got ASSY_PRINT_SUMM_REPORTS (not error) "+bool);
              break;

            case DUMP_OUTPUTS:
              System.err.println("Got dumpOutputs (not error)");
              dumpOutputs();
              break;
            case QUIT:
              System.err.println("Got quit (not error)");
              System.exit(0);
              break;

            default:
              throw new RuntimeException("Program error in InternalAssemblyRunner");
          }
        }
      }
      catch (Exception e) {
        System.out.println("external assembly runner terminating. "+e.getMessage());
      }

    }

    private void openChannel(String port)
    {
      try {
        if(extBackChannel != null) {
          extBackChannel.close();
          extBackChannel = null;
        }
        Socket s = new Socket("localhost",Integer.parseInt(port));
        extBackChannel = new PrintStream(s.getOutputStream(),true);
      }
      catch (IOException e) {
        System.out.println("bad backChannel open");
        e.printStackTrace();
      }
    }

    private void sendAnalystReportPath()
    {
      String analystReportPath = targetAssembly.getAnalystReportPath();
      if (analystReportPath != null && extBackChannel != null)
        extBackChannel.println(RESP_ANALYSTREPORTFILE + analystReportPath);
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


  }

  private String namePrefix = "Viskit Assembly Runner";
  private String currentTitle = namePrefix;
  private void doTitle(String nm)
  {
    if(nm != null && nm.length()>0)
      currentTitle = namePrefix +": "+nm;

    if(titlList != null)
      titlList.setTitle(currentTitle,titlkey);
  }
  private TitleListener titlList;
  private int titlkey;
  public void setTitleListener(TitleListener lis, int key)
  {
    titlList = lis;
    titlkey = key;
    doTitle(null);
  }
}
