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
import simkit.Schedule;

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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/** Analogous to ExternalAssemblyRunner, but puts gui in calling thread and runs
 * sim in external VM.
 */
public class InternalAssemblyRunner implements edu.nps.util.DirectoryWatch.DirectoryChangeListener
{
  String targetClassName;
  String targetClassPath;
  List outputs;
  RunnerPanel runPanel;
  ActionListener closer,saver;
  static String lineSep = System.getProperty("line.separator");
  JMenuBar myMenuBar;

  private Process externalSimRunner;

  public InternalAssemblyRunner()
  {
    saver = new saveListener();
    runPanel = new RunnerPanel(null,true); //"Initialize using Assembly Edit tab, then Run button",true);
    doMenus();
    runPanel.vcrStop.addActionListener(new stopListener());
    runPanel.vcrPlay.addActionListener(new startResumeListener());
    runPanel.vcrRewind.addActionListener(new rewindListener());
    runPanel.vcrStep.addActionListener(new stepListener());

    runPanel.vcrStop.setEnabled(false);
    runPanel.vcrPlay.setEnabled(false);
    runPanel.vcrRewind.setEnabled(false);
    runPanel.vcrStep.setEnabled(false);

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

  /**
   * Here's where we get notified that the assembly file has changed
   * @param file
   * @param action
   * @param source
   */
  public void fileChanged(File file, int action, DirectoryWatch source)
  {
    // temp:
        System.out.println("InternalAssemblyRunner got assembly change message: "+action+
                                  " " + file.getAbsolutePath());
  }

  private String[] myCmdLine;
  private static final int RUNNER_ARG_CLASSNAME = 0; //  when it gets to the external vm
  private static final int RUNNER_ARG_VERBOSE = 1;
  private static final int RUNNER_ARG_STOPTIME = 2;
  private static final int RUNNER_ARG_OUTPUTS = 3;


  public void initParams(String[]parms, int runnerClassIndex)
  {
    /*
    0 javacmd
    1 "-cp"
    2 classPath
    3 "viskit.ExternalAssemblyRunner" main class
    4 className to run (first program argument)
    5 verbose
    6 stoptime
    7+ outputentitites
    */

    parms[runnerClassIndex] = "viskit.InternalAssemblyRunner$ExternalSimRunner";
    myCmdLine = parms;

    targetClassName = parms[4];
    doTitle(parms[4]);

    targetClassPath = parms[2];
    boolean defaultVerbose = Boolean.valueOf(parms[5]).booleanValue();
    double defaultStopTime = Double.parseDouble(parms[6]);

    //outputs = outputEntities;

    //setTitle("Assembly Run Panel -- "+targetClassName);

    twiddleButtons(InternalAssemblyRunner.REWIND);

    runPanel.vcrStopTime.setText(""+defaultStopTime);
    runPanel.vcrSimTime.setText("0"); //Schedule.getSimTimeStr());
    runPanel.vcrVerbose.setSelected(defaultVerbose);

    if (externalSimRunner != null) {
      send(ExternalSimRunner.QUIT);
      externalSimRunner = null;
    }
  }

  PrintWriter pWriter;
  private void initRun()
  {
    if(externalSimRunner == null) {
      showLaunchingDialog(2*1000);
      try {
        externalSimRunner = Runtime.getRuntime().exec(myCmdLine);
        runPanel.setStreams(externalSimRunner.getInputStream(),externalSimRunner.getErrorStream());
        // set autoflush after every println
        pWriter = new PrintWriter(new OutputStreamWriter(externalSimRunner.getOutputStream()),true);
      }
      catch (Exception e) {
        System.out.println("Error launching virtual machine: "+e.getMessage());
      }
    }

    send(ExternalSimRunner.SCHEDULE_RESET);
    pWriter.flush();
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
      initRun();
      send(ExternalSimRunner.SCHEDULE_SETPAUSEAFTEREACHEVENT);
      send(false);
      twiddleButtons(InternalAssemblyRunner.START);
      _start();
    }
  }
  class stopListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      send(ExternalSimRunner.SCHEDULE_STOPSIMULATION);
      twiddleButtons(InternalAssemblyRunner.STOP);
    }
  }
  class rewindListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      send(ExternalSimRunner.SCHEDULE_RESET);
      twiddleButtons(InternalAssemblyRunner.REWIND);

      String timeString = null;
      try {
        timeString = sendAndWait(ExternalSimRunner.SCHEDULE_GETSIMTIMESTR);
      }
      catch (IOException e1) {
        timeString = e1.getMessage();
      }
      runPanel.vcrSimTime.setText(timeString);
    }
  }
  class stepListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      send(ExternalSimRunner.SCHEDULE_SETPAUSEAFTEREACHEVENT);
      send(true);

      twiddleButtons(InternalAssemblyRunner.STEP);
      _start();
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

  BufferedReader backChan;

  private void _start()
  {
    Thread t = new Thread(new Runnable()
    {
      String response;
      public void run()
      {
        Thread.yield(); // let dialog show up
        try {
          ServerSocket svrsocket = new ServerSocket(0);  // any port

          send(ExternalSimRunner.OPEN_BACKCHANNEL);
          send("" + svrsocket.getLocalPort());

          Socket sock = svrsocket.accept();

          backChan = new BufferedReader(new InputStreamReader(sock.getInputStream()));
          response = backChan.readLine();
          //System.out.println("Back channel opened, response: " + response);
        }
        catch (IOException e) {
          e.printStackTrace();
        }

        send(ExternalSimRunner.SCHEDULE_STOPATTIME);
        send(getStopTime());
        send(ExternalSimRunner.SCHEDULE_SETVERBOSE);
        send(getVerbose());

        send(ExternalSimRunner.DUMP_OUTPUTS);
        // The following one blocks in the running VM.  We're just queing up commands here.
        send(ExternalSimRunner.SCHEDULE_STARTSIMULATION);

        try {
          System.out.println("waiting for sim to complete");
          response = backChan.readLine();  // wait for reply
          System.out.println("sim completed");
        }
        catch (IOException e) {
          System.out.println("error in InternalAssemblyRunner");
        }

        send(ExternalSimRunner.DUMP_OUTPUTS);

        System.out.println("waiting to get time");
        String tm;
        try {
          tm = sendAndWait(ExternalSimRunner.SCHEDULE_GETSIMTIMESTR);
        }
        catch (IOException e) {
          tm = "Error";
        }
        System.out.println("got time " + tm);
        final String time = tm;
        // get answer and update GUI stuff in GUI thread
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            twiddleButtons(InternalAssemblyRunner.STOP);
            runPanel.vcrSimTime.setText(time);
          }
        }
        );

      }
    }, "asyRnr");
    t.setPriority(Thread.NORM_PRIORITY);
    t.start();
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
      case OFF:
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
    public static final int SCHEDULE_SETVERBOSE = 4;             // Schedule.setVerbose(getVerbose());
    public static final int SCHEDULE_STARTSIMULATION =5;         // Schedule.startSimulation();
    public static final int SCHEDULE_STOPATTIME = 6;             // Schedule.stopAtTime(double);

    public static final int DUMP_OUTPUTS = 7;
    public static final int OPEN_BACKCHANNEL = 8;
    public static final int QUIT = 9;

    public static String[] cmdNames = {"Reset","GetSimTimeString","SetPause",
                                       "StopSim","SetVerbose","StartSim","StopAtTime",
                                       "DumpOutputs","OpenBackchannel","Quit"};

    private Vector outputs = new Vector();
    private PrintStream backChannel;
    private Object targetObject;

    public static void main(String[] args)
    {
      new ExternalSimRunner(args);
    }

    private ExternalSimRunner(String[] args)
    {
      Class targetClass = null;
      String targetClassName = args[RUNNER_ARG_CLASSNAME];
      String errMsg = null;
      try {
        targetClass = Vstatics.classForName(targetClassName);
          if(targetClass == null) throw new ClassNotFoundException();
        targetObject = targetClass.newInstance();
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

      outputs.clear();
      if (args != null && args.length > 0) {
        for (int i = 3; i < args.length; i++)
          outputs.add(args[i]);
      }
      Schedule.reset();

      execLoop();
    }

    private void execLoop()
    {
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      try {
        // Setup the sim by spawning an assembly thread
        Thread t = new Thread((Runnable)targetObject,"SimThread");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();

        while(true) {
          String cmd = in.readLine();

          if(cmd == null)
            System.exit(0);

          int cmdIn  = Integer.parseInt(cmd);
          switch(cmdIn) {
            case OPEN_BACKCHANNEL:
              String port = in.readLine();
              openChannel(port);
              if(backChannel != null)
                backChannel.println("BackChannelOK");
              break;
            case SCHEDULE_RESET:
              Schedule.reset();
              break;
            case SCHEDULE_STOPSIMULATION:
              System.err.println("Got stop sim.");
              Schedule.stopSimulation();
              break;
            case SCHEDULE_STARTSIMULATION:
              Thread tt = new Thread(new Runnable()
              {
                public void run()
                {
                  Schedule.startSimulation();
                  if(backChannel != null)
                    backChannel.println("SimDone");
                  else
                    System.out.println("SimDone");
                }
              },"startSimThread");
              tt.setPriority(Thread.NORM_PRIORITY);
              tt.start();
             break;
            case SCHEDULE_SETPAUSEAFTEREACHEVENT:
              String which = in.readLine();
              Schedule.setPauseAfterEachEvent(Boolean.valueOf(which).booleanValue());
              break;
            case SCHEDULE_SETVERBOSE:
              String verb = in.readLine();
              Schedule.setVerbose(Boolean.valueOf(verb).booleanValue());
              break;
            case SCHEDULE_GETSIMTIMESTR:
              backChannel.println(Schedule.getSimTimeStr());
              break;
            case SCHEDULE_STOPATTIME:
              String tm = in.readLine();
              Schedule.stopAtTime(Double.parseDouble(tm));
              break;

            case DUMP_OUTPUTS:
              dumpOutputs();
              break;
            case QUIT:
              System.exit(0);
              break;

            default:
              throw new RuntimeException("Program error in InternalAssemblyRunner");
          }
        }
      }
      catch (Exception e) {
        System.out.println("external assembly runner terminating.");
      }

    }

    private void openChannel(String port)
    {
      try {
        if(backChannel != null) {
          backChannel.close();
          backChannel = null;
        }
        Socket s = new Socket("localhost",Integer.parseInt(port));
        backChannel = new PrintStream(s.getOutputStream(),true);
      }
      catch (IOException e) {
        System.out.println("bad backChannel open");
        e.printStackTrace();
      }
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
