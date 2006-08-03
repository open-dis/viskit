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
 * @since Jul 21, 2005
 * @since 12:29:08 PM
 */

package viskit.doe;

import edu.nps.util.CryptoMethods; 
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import viskit.OpenAssembly;
import viskit.SpringUtilities;
import viskit.TitleListener;
import viskit.VGlobals;
import viskit.xsd.assembly.SessionManager;
import viskit.xsd.bindings.assembly.Experiment;
import viskit.xsd.bindings.assembly.SampleStatisticsType;
import viskit.xsd.bindings.assembly.Schedule;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.security.Key;
import java.util.*;
import java.util.List;

public class JobLauncherTab extends JPanel implements Runnable, OpenAssembly.AssyChangeListener
{
  private XmlRpcClientLite rpc;
  private String userID;
  Hashtable statsGraphs;
  String inputFileString;
  File inputFile;
  File filteredFile;
  FileReader fr;
  PrintWriter out;
  BufferedReader br;

  JFrame mom;

  String clusterDNS = "cluster.moves.nps.navy.mil";
  int defaultClusterPort = 4444;

  String clusterWebStatus1 = "http://cluster.moves.nps.navy.mil/ganglia/";
  String clusterWebStatus2 = "http://cluster.moves.nps.navy.mil/ganglia/?m=cpu_user&r=hour&s=descending&c=MOVES&h=&sh=1&hc=3";
  String clusterWebStatus  = "http://cluster.moves.nps.navy.mil/ganglia/?r=hour&c=MOVES&h=&sh=0";
  private JButton canButt;
  private JButton runButt;
  private SimkitAssembly jaxbRoot;
  private JTextField numCubesTF;
  private JTextField portTF;
  private JTextField numRepsTF;
  private JTextField numDPsTF;
  private JTextField tmo;
  private JTextArea statusTextArea;
  private QstatConsole qstatConsole;
  private StatsGraph statsGraph;
  private Thread thread;
  private boolean outputDirty = false;

  private String serverCfg;
  private String portCfg;
  private String unameDec="";
  private String pwordDec="";
  private JTextField unameTF;
  private JPasswordField upwPF;
  private JTextField clusterTF;

  private String title;
  private DoeController cntlr;
  public  JCheckBox doClusterStat;
  public  JCheckBox doGraphOutput;
  public  JButton doQstatConsole;
  private JButton adminButt;
  
  private JAXBContext jaxbCtx;
  private Unmarshaller unmarshaller;

  public JobLauncherTab(DoeController controller, String file, String title, JFrame mainFrame)
  {
      try {
    jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
    unmarshaller = jaxbCtx.createUnmarshaller();
      } catch (JAXBException je) {
          je.printStackTrace();
      }
    this.title = title;
    cntlr = controller;
    mom = mainFrame;
    buildContent();

    setFile(file,title);
    doListeners();
  }

  public Container getContent()
  {
    return this;
  }

  private Container buildContent()
  {
    initConfig();
    JPanel p = this;
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

    JPanel topPan = new JPanel(new SpringLayout());

    statusTextArea = new JTextArea(20, 30);
    JScrollPane jsp = new JScrollPane(statusTextArea);
    JPanel botBar = new JPanel();
    botBar.setLayout(new BoxLayout(botBar, BoxLayout.X_AXIS));

    JPanel clusPan = new JPanel(new SpringLayout());
    clusPan.setBorder(new EtchedBorder());

    JLabel clusLab = new JLabel("Target grid engine");
    clusterTF = new ttJTextField(20);
    clusterTF.setText(serverCfg);//clusterDNS);
    clusterTF.setToolTipText("dummy"); // overridden
    JLabel portLab = new JLabel("RPC port");
    portTF = new ttJTextField(10);
    portTF.setToolTipText("dummy"); // overridden
    portTF.setText(portCfg);
    JLabel unameLab = new JLabel("User name");
    unameTF = new JTextField(10);
    unameTF.setText(unameDec);
    JLabel upwLab = new JLabel("Password");
    upwPF = new JPasswordField(10);
    upwPF.setText(pwordDec);

    JPanel adminPan = new JPanel();
    adminButt = new JButton("admin");

    adminPan.setLayout(new BoxLayout(adminPan,BoxLayout.X_AXIS));
    adminPan.add(clusterTF);
    adminPan.add(adminButt);

    clusPan.add(clusLab);
    clusPan.add(adminPan); //clusterTF);
    clusPan.add(unameLab);
    clusPan.add(unameTF);
    clusPan.add(portLab);
    clusPan.add(portTF);
    clusPan.add(upwLab);
    clusPan.add(upwPF);


    JLabel dpLab = new JLabel("Design point variables");
    numDPsTF = new JTextField(6);

    JLabel sampLab = new JLabel("Hypercubes");
    numCubesTF = new ttJTextField(20);

    numRepsTF = new JTextField(6);
    JLabel repLab = new JLabel("Replications");
    tmo = new JTextField(6);
    JLabel tmoLab = new JLabel("Replication time out (ms)");

    numDPsTF.setEditable(false);

//    topPan.add(clusLab);
//    topPan.add(clusterCB);
//    topPan.add(portLab);
//    topPan.add(portTF);
    topPan.add(dpLab);
    topPan.add(numDPsTF);
    topPan.add(sampLab);
    topPan.add(numCubesTF);
    topPan.add(repLab);
    topPan.add(numRepsTF);
    topPan.add(tmoLab);
    topPan.add(tmo);

    SpringUtilities.makeCompactGrid(clusPan,2, 4, 10, 10, 10, 5);
    clusPan.setMaximumSize(clusPan.getPreferredSize());
    SpringUtilities.makeCompactGrid(topPan, 4, 2, 10, 10, 5, 5);
    topPan.setMaximumSize(topPan.getPreferredSize());

    canButt = new JButton("Cancel job");
    canButt.setEnabled(false);
    runButt = new JButton("Run job");
    doClusterStat = new JCheckBox("Display cluster status in browser",false);
    doGraphOutput = new JCheckBox("Graph job output",false);
    doQstatConsole = new JButton("Qstat Console");
    doQstatConsole.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
            if (qstatConsole == null) {
                qstatConsole = new QstatConsole(unameTF.getText(),new String(upwPF.getPassword()),clusterTF.getText().trim(),portTF.getText().trim());
            }
            qstatConsole.show();
        }
    });
    botBar.add(doClusterStat);
    botBar.add(Box.createHorizontalStrut(5));
    botBar.add(doGraphOutput);
    botBar.add(Box.createHorizontalGlue());
    botBar.add(canButt);
    botBar.add(runButt);
    botBar.add(doQstatConsole);
    p.add(clusPan);
    //p.add(topPan);
      JPanel saveButtCenterPan = new JPanel();
      saveButtCenterPan.setLayout(new BoxLayout(saveButtCenterPan,BoxLayout.X_AXIS));
      saveButtCenterPan.add(Box.createHorizontalGlue());
      saveButtCenterPan.add(topPan);
      saveButtCenterPan.add(Box.createHorizontalGlue());

      JPanel saveButtPan = new JPanel();
      saveButtPan.setLayout(new BoxLayout(saveButtPan,BoxLayout.X_AXIS));
    saveButtCenterPan.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
      saveButtPan.add(saveButtCenterPan);
      JButton sv = new JButton("Save");
      sv.addActionListener(new svLister());
    sv.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
    sv.setToolTipText("<html><center>Save cluster run parameters<br>to assembly file<br>"+
                      "(not required to run job)");
      sv.setBorder(new CompoundBorder(new EmptyBorder(0,0,5,0),sv.getBorder()));
      saveButtPan.add(sv);
      p.add(saveButtPan);
    p.add(jsp);
    p.add(Box.createVerticalStrut(8));
    p.add(botBar);
    p.setBorder(new EmptyBorder(10, 10, 10, 10));
    return p;
  }

  public void setFile(String file, String title)
  {
    if(file==null) {
      inputFileString = null;
      inputFile=null;
      filteredFile=null;
      return;
    }
    inputFileString = file;
    inputFile = new File(file);
    filteredFile = inputFile;      // will be possibly changed

    try {
      filteredFile = File.createTempFile("DoeInputFile", ".xml");
    }
    catch (IOException e) {
      System.out.println("couldn't make temp file");
    }

    try {
      getParams();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    doTitle(title);
  }

  /**
   * This is where an open assembly gets mentioned here
   * @param jaxbRoot
   * @param file
   */
  public void setAssemblyFile(SimkitAssembly jaxbRoot, File file)
  {
    this.jaxbRoot = jaxbRoot;
    setFile(file.getAbsolutePath(),file.getName());
  }
  public void closeAssemblyFile(File file)
  {
    //todo do something here to put up a "no-file" banner or equivalent
  }
  public void refreshAssemblyFile(File file)
  {
    // nothing here
  }

  private KeyListener myEditListener = new KeyAdapter()
  {
    public void keyTyped(KeyEvent e)
    {
      //System.out.println("sending paramlocallyeditted from JobLauncherTab");
      OpenAssembly.inst().doParamLocallyEditted(JobLauncherTab.this);     // inform who is listening that we tweeked the params
    }
  };

  private void getParams()
  {
    Experiment exp = (Experiment)jaxbRoot.getExperiment();    // todo cast requirement jaxb error?
    if(exp != null) {
      //designPts = exp.getDesignPoint().size();
      int numDesignPts = jaxbRoot.getDesignParameters().size();
      numDPsTF.setText("" + numDesignPts);
      String s = exp.getTotalSamples();
      if(s != null)
        numCubesTF.setText(s);
      s = exp.getReplicationsPerDesignPoint();
      if(s != null)
        numRepsTF.setText(s);

      s = exp.getTimeout();
      if(s != null)
        tmo.setText(s);
    }
    else {
      try {
        exp = OpenAssembly.inst().jaxbFactory.createExperiment();
      }
      catch (JAXBException e) {
        e.printStackTrace();
        return;
      }
      jaxbRoot.setExperiment(exp);

      exp.setTotalSamples("1");
      numCubesTF.setText("1");
      exp.setReplicationsPerDesignPoint("1");
      numRepsTF.setText("1");
      exp.setTimeout("5000");
      tmo.setText("5000");
      int numDesignPts = jaxbRoot.getDesignParameters().size();
      numDPsTF.setText(""+numDesignPts);
    }
  }

  class svLister implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      // The user has hit the save button;
      saveParamsToJaxbNoNotify();
      OpenAssembly.inst().doSendAssyJaxbChanged(JobLauncherTab.this);
    }
  }

  public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param)
  {
  }

  public String getHandle()
  {
    return "Launch Cluster Job";
  }

  private void saveParamsToJaxbNoNotify()
  {
    // Put the params from the GUI into the jaxbRoot
    Experiment exp = null;
    Schedule sch = null;
    try {
      exp = (Experiment) OpenAssembly.inst().jaxbFactory.createExperiment();
      sch = OpenAssembly.inst().jaxbFactory.createSchedule();
    }
    catch (JAXBException e) {
      System.err.println("jaxb error: "+e.getMessage());
    }

    String reps = numRepsTF.getText().trim();
    try {Integer.parseInt(reps);}
    catch (NumberFormatException e) {
      reps="1";
      System.err.println("Bad number of replications...use 1");
    }
    sch.setNumberReplications(reps);                            // rg: 2
    exp.setReplicationsPerDesignPoint(reps);

    String samps = numCubesTF.getText().trim();
    try {Integer.parseInt(samps);}
    catch (NumberFormatException e) {
      samps="1";
      System.err.println("Bad number of samples...use 1");
    }
    exp.setTotalSamples(samps);                                // rg: 5

    exp.setJitter("true");
    exp.setType("latin-hypercube");

    String stopTime = tmo.getText().trim();
    try {Double.parseDouble(stopTime);}
    catch (NumberFormatException e) {
      stopTime="1000.0";
      System.err.println("Bad stop time...use 1000");
    }
    sch.setStopTime(stopTime);

    sch.setVerbose("true");

    jaxbRoot.setSchedule(sch);
    jaxbRoot.setExperiment(exp);
  }


  private void doListeners()
  {
    canButt.setActionCommand("cancel");
    runButt.setActionCommand("run");
    adminButt.setActionCommand("admin");
    ActionListener al = new ButtListener();
    canButt.addActionListener(al);
    runButt.addActionListener(al);
    adminButt.addActionListener(al);

    numCubesTF.addKeyListener(myEditListener);
    numRepsTF.addKeyListener(myEditListener);
    tmo.addKeyListener(myEditListener);
  }

  /**
   * Save off the exp stuff for a moment; to be restored
   * @param exp
   */
  private void saveExp(Experiment exp)
  {
    //eType = exp.getType();
    eTmo = exp.getTimeout();
    //eJitter = exp.getJitter();
    eRpdp = exp.getReplicationsPerDesignPoint();
    eTotSamp = exp.getTotalSamples();
    //eBatchID = exp.getBatchID();
    //eDbg = exp.getDebug();
    //eScript = exp.getScript();
    //java.util.List getDesignPoint();


  }

  //private String eType;
  private String eTmo;
  //private String eJitter;
  private String eRpdp;
  private String eTotSamp;
  //private String eBatchID;
  //private String eDbg;
  //private String eScript;
  //private List eDPs;

  private void restoreExp(Experiment exp)
  {
    exp.setTotalSamples(eTmo);
    exp.setReplicationsPerDesignPoint(eRpdp);
    exp.setTotalSamples(eTotSamp);
  }

  private void doStartRun()
  {
    runButt.setEnabled(false);
    canButt.setEnabled(true);

    // call back to the controller to put design parms and egs in place in a temp file
    // prepRun() access the DOE tab and puts the dps into the
    if (!cntlr.prepRun()) {
      runButt.setEnabled(true);
      canButt.setEnabled(false);
      return;
    }
    // put the local stuff in place
    saveExp((Experiment) jaxbRoot.getExperiment());
    saveParamsToJaxbNoNotify();

    filteredFile = cntlr.doTempFileMarshall();

    restoreExp((Experiment) jaxbRoot.getExperiment());
    cntlr.restorePrepRun();

    thread = new Thread(JobLauncherTab.this);
    thread.setPriority(Thread.NORM_PRIORITY); // don't inherit swing event thread prior
    thread.start();

    writeConfig(); // save parameters

  }

  class ButtListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      switch (e.getActionCommand().charAt(0)) {
        case 'r':
          doStartRun();
          break;
        case 'c':
          stopRun();
          break;
        case 'a':
          int port = 0;
          try {
            port = Integer.parseInt(portTF.getText().trim());
          }
          catch (NumberFormatException e1) {
            System.err.println("Bad number parse: "+e1.getMessage()+"; using "+defaultClusterPort);
            port = defaultClusterPort;
          }
          ClusterAdminDialog.showDialog(clusterTF.getText(),port,mom,mom);
          break;
        case 'x':
          runButt.setEnabled(true);  // for next time (probably not used)
          canButt.setEnabled(false);
          if (outputDirty) {
            if (JOptionPane.showConfirmDialog(JobLauncherTab.this, "Save output?") == JOptionPane.YES_OPTION) {
              JFileChooser jfc = new JFileChooser();
              jfc.setSelectedFile(new File("DOEOutput.txt"));
              jfc.showSaveDialog(JobLauncherTab.this);
              if (jfc.getSelectedFile() != null) {
                File f = jfc.getSelectedFile();
                try {
                  FileWriter fw = new FileWriter(f);
                  fw.write(statusTextArea.getText());
                  fw.close();
                }
                catch (IOException e1) {
                  e1.printStackTrace();
                }
              }
            }
            outputDirty = false;
          }

          setVisible(false);
          break;
        default:
          //assert false:"Program error JobLauncher.java";
          System.err.println("Program error JobLauncher.java");
      }
    }
  }

  /**
   * There seems to be a bug in the XMLRpc code.  If you try to
   * attach to a server which never answers, you can't kill the thread.
   * (This has been reported.)  The solution is to make the best effort,
   * but do it in a separate thread to not stop gui.  The thread may not die
   * but shouldn't be a problem.
   */
  private void stopRun()
  {
    outputList.clear();

    canButt.setEnabled(false);
    runButt.setEnabled(true);

    if (thread == null)
      return;

    writeStatus("Stopping run.");
    hideClusterStatus();

    Thread jobKiller = new Thread(new Runnable()
    {
      public void run()
      {
        if (thread != null) {
          Thread t = thread;
          thread = null;
          t.interrupt();
          try {
            t.join(1000);
          }
          catch (InterruptedException e) {
            System.err.println("join exception");
          }
        }
        try {
          if(rpc != null) {
            Vector parms = new Vector();
            rpc.execute("gridkit.clear", parms);
          }
        }
        catch (Exception e) {
          System.err.println("RPC exception: "+e.getMessage());
        }

      }
    }, "JobKiller");
    jobKiller.setPriority(Thread.NORM_PRIORITY);
    jobKiller.start();
  }

  private void writeStatus(final String s)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        statusTextArea.append(s);
        statusTextArea.append("\n");
      }
    });
  }

  private boolean sendEGToServer(File file) throws Exception
  {
    FileInputStream fis = new FileInputStream(file);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int rdRet;
    while((rdRet = fis.read(buf)) > 0)
      baos.write(buf,0,rdRet);
    fis.close();
    baos.close();
    String egText = new String(baos.toByteArray());

    Vector args = new Vector();
    args.add(userID);
    args.add(egText);

    Boolean ret = (Boolean)rpc.execute("gridkit.addEventGraph",args);
    return ret.booleanValue();
  }

  StringWriter data;

  public void run()
  {
    Vector args = new Vector(5);

    boolean doClustStat = this.doClusterStat.isSelected();
    boolean doGraphOut  = this.doGraphOutput.isSelected();

    outputDirty = true;
    outputList = new ArrayList();
    lp3:
    {
      try {
        createOutputDir();
        int chosenPort = Integer.parseInt(portTF.getText());
        String clusterDNS = clusterTF.getText().trim();

        writeStatus("Building XML-RPC client to " + clusterDNS + ".");
        rpc = new XmlRpcClientLite(clusterDNS, chosenPort);
        // login
        args.add(unameTF.getText());
        args.add(new String(upwPF.getPassword()));
        userID = (String)rpc.execute("gridkit.login",args);
        if(userID.equalsIgnoreCase(SessionManager.LOGIN_ERROR)) {
          userID=null;
          throw new Exception("Login refused.");
        }

        // Send dependencies
        Collection egs = cntlr.getLoadedEventGraphs();

        for(Iterator itr=egs.iterator(); itr.hasNext();) {
          sendEGToServer((File)itr.next());
        }

        // Construct assembly
        args.clear();
        args.add(userID);

        fr = new FileReader(filteredFile);
        br = new BufferedReader(fr);
        data = new StringWriter();
        out = new PrintWriter(data);
        String line;
        while ((line = br.readLine()) != null) {
          out.println('\t' + line);
        }
        out.close();

        String dataS = data.toString().trim();
        //String[]sa = dataS.split("<\\?xml.*\\?>"); // remove the hdr if present
        //if(sa.length == 2)
        //  dataS = sa[1].trim();
        args.add(dataS);
        if (viskit.Vstatics.debug) System.out.println(dataS);
        writeStatus("Sending job file to " + clusterDNS);
        Boolean retBool = (Boolean)rpc.execute("gridkit.setAssembly",args);
        writeStatus("gridkit.setAssembly returned " + retBool.booleanValue());
        if(retBool.booleanValue() == false) {
          throw new Exception("Set Assembly returned false.");
        }
        // Run it!
        args.clear();
        args.add(userID);
        writeStatus("Executing job");
        retBool = (Boolean)rpc.execute("gridkit.run",args);
        //retBool = (Boolean)gr.run();                         //todo
        writeStatus("gridkit.run returned " + retBool.booleanValue());
        if(retBool.booleanValue() == false) {
          throw new Exception("Set Assembly returned false.");
        }

      }
      catch (Exception e) {

     //   if (thread != null)    // If normal error:
        if(rpc == null) {
          writeStatus("Error connecting to server: " + e.getMessage());
          break lp3;
        }
        if(userID == null) {
          writeStatus("Error authenticating to server: "+ e.getMessage());
          break lp3;
        }

        writeStatus("Error: "+ e.getMessage());
        args.clear();
        args.add(userID);
        try {rpc.execute("gridkit.logout",args);}catch (Exception e1) {}
        break lp3;
      }


      // Bring up the 2 other windows
      if(doClustStat)
        showClusterStatus(clusterWebStatus);
      //if(doGraphOut)
        //chartter = new JobResults(null, title);

      writeStatus("Getting results:");

      processResultsNew();

    }

    args.clear();
    args.add(userID);
    try {rpc.execute("gridkit.logout",args);}catch (Exception e) {}

    stopRun();
  }

  private void processResultsNew()
  {
    Vector args = new Vector(5);
    Object ret;
    Experiment exp = (Experiment)jaxbRoot.getExperiment();
    int samples = Integer.parseInt(exp.getTotalSamples());
    int designPoints = jaxbRoot.getDesignParameters().size();
    // synchronous single threaded results, uses
    // a status buffer that locks until results are
    // in, at which point a select can be performed.
    // this saves server thread resources
    
    
    Vector lastQueue;

    try {
      // this shouldn't block on the very first call, the queue
      // is born dirty.
      args.clear();
      args.add(userID);
      lastQueue = (Vector) rpc.execute("gridkit.getTaskQueue", args);

      // initial number of tasks ( can also query getRemainingTasks )
      args.clear();
      args.add(userID);
      int tasksRemaining = ((Integer)rpc.execute("gridkit.getRemainingTasks",args)).intValue(); //5 * 3;
      writeStatus("Total tasks: "+tasksRemaining);
      
      while (tasksRemaining > 0) {
        // this will block until a task ends which could be
        // because it died, or because it completed, either way
        // check the logs returned by getResults will tell.
        args.clear();
        args.add(userID);
        Vector queue = (Vector) rpc.execute("gridkit.getTaskQueue", args);
        for (int i = 0; i < queue.size(); i ++) {
          // trick: any change between queries indicates a transition at
          // taskID = i (well i+1 really, taskID's in SGE start at 1)
          if (!((Boolean) lastQueue.get(i)).equals(((Boolean) queue.get(i)))) {
            int sampleIndex   = i / designPoints; // 3; // number of designPoints chosed in this experiemnt was 3
            int designPtIndex = i % designPoints; // 3; // can also just use getResultByTaskID(int)
            args.clear();
            args.add(userID);
            args.add(new Integer(sampleIndex));
            args.add(new Integer(designPtIndex));
            // this is reallllly verbose, you may wish to consider
            // not getting the output logs unless there is a problem
            // even so at that point the design points can be run
            // as a regular assembly to reproduce. the complete
            // logs are so far stored on the server.

            if (/*verbose*/false) {
              ret = rpc.execute("gridkit.getResult", args);
              writeStatus("Result returned from task " + (i + 1));
              writeStatus(ret.toString());
            }
            writeStatus("DesignPointStats from task " + (i + 1)+ " is sampleIndex "+sampleIndex+" at designPtIndex "+designPtIndex);
            ret = rpc.execute("gridkit.getDesignPointStats", args);
            if (statsGraph == null) {
                final String[] properties = (String[])((Hashtable)ret).keySet().toArray(new String[0]);
                statsGraph = new StatsGraph(jaxbRoot.getName(),properties,designPoints, samples);
                statsGraph.show();
            }
            addDesignPointStatsToGraphs((Hashtable)ret,designPtIndex,sampleIndex);
            //writeStatus(ret.toString());
            writeStatus("Replications per designPt "+exp.getReplicationsPerDesignPoint());
            for (int j = 0; j < Integer.parseInt(exp.getReplicationsPerDesignPoint()); j++) {
              writeStatus("ReplicationStats from task " + (i + 1) + " replication " + j);
              args.clear();
              args.add(userID);
              args.add(new Integer(sampleIndex));
              args.add(new Integer(designPtIndex));
              args.add(new Integer(j));
              ret = rpc.execute("gridkit.getReplicationStats", args);
              //writeStatus(ret.toString());
            }
            --tasksRemaining;
            // could also call to get tasksRemaining:
            // ((Integer)xmlrpc.execute("gridkit.getRemainingTasks",args)).intValue();
          }
        }
        lastQueue = queue;

      }
      statsGraph = null; // allow to gc after done
    }
    catch (Exception e) {
        e.printStackTrace();
      writeStatus("Error in cluster execution: " + e.getMessage());
    }

  }


  ArrayList outputList;

  private void createOutputDir() throws Exception
  {
    outDir = File.createTempFile("DoeRun", "");
    outDir.delete();
    outDir.mkdir();
  }

  JobResults chartter;

  private void plotOutput(int idx)
  {
    if (chartter == null)
      chartter = new JobResults(null, title);
    synchronized (outputList) {
      Object[] oa = (Object[]) outputList.get(idx);
      Gresults res = getSingleResult(oa);
      chartter.addPoint(res);
      if (!res.resultsValid)
        System.out.println("Results not retrieved for rep " + idx);
    }
  }

  private Gresults getSingleResult(Object[] oa)
  {
    File f = new File((String) oa[2]);
    int dp = ((Integer) oa[0]).intValue();
    int nrun = ((Integer) oa[1]).intValue();
    Gresults res = new Gresults();

    Document doc = null;
    try {
      doc = FileHandler.unmarshallJdom(f);
    }
    catch (Exception e) {
      System.out.println("Error unmarshalling results: " + e.getMessage());
      return null;
    }
    Element el = doc.getRootElement();
    if (!el.getName().equals("Results")) {
      System.out.println("Unknown results format, design point = " + dp + ", run = " + nrun);
      return res;
    }
    String design = attValue(el, "design");
    String index = attValue(el, "index");
    String job = attValue(el, "job");
    String run = attValue(el, "run");

    Element propCh = el.getChild("PropertyChange");
    if (propCh == null) {
      System.out.println("PropertyChange results element null, design point = " + dp + ", run = " + nrun);
      return res;
    }
    String listenerName = attValue(propCh, "listenerName");
    String property = attValue(propCh, "property");
    List content = propCh.getContent();
    Text txt = (Text) content.get(0);
    String cstr = txt.getTextTrim();
    if (viskit.Vstatics.debug) System.out.println("got back " + cstr);
    String[] sa = cstr.split("\n");
    if (sa.length != 2) {
      System.out.println("PropertyChange parse error, design point = " + dp + ", run = " + nrun);
      return res;
    }
    sa[1] = sa[1].trim();
    String[] nums = sa[1].split("\\s+");
    // format: 0: int, count
    //         1: float, minObs
    //         2: float, maxObs
    //         3: float, mean -- if < 1.0, a terrorist succeeded
    //         4: float, variance
    //         5: float, std dev
    res.listener = listenerName;
    res.property = property;
    res.run = Integer.parseInt(run);
    //assert res.run == nrun :"JobLauncher.doResults";
    if(res.run != nrun)
      System.err.println("JobLauncher.doResults");

    res.dp = Integer.parseInt(design);
    //assert res.dp == dp : "JobLauncher.doResults1";
    if(res.dp != dp)
      System.err.println("JobLauncher.doResults1");

    res.resultsCount = Integer.parseInt(nums[Gresults.COUNT]);
    res.resultsMinObs = Double.parseDouble(nums[Gresults.MINOBS]);
    res.resultsMaxObs = Double.parseDouble(nums[Gresults.MAXOBS]);
    res.resultsMean = Double.parseDouble(nums[Gresults.MEAN]);
    res.resultsVariance = Double.parseDouble(nums[Gresults.VARIANCE]);
    res.resultsStdDev = Double.parseDouble(nums[Gresults.STDDEV]);

    res.resultsValid = true;
    return res;
  }

  String attValue(Element e, String att)
  {
    Attribute at = e.getAttribute(att);
    return (at != null ? at.getValue() : null);
  }

  File outDir;

  private int saveOutput(String o, int dp, int nrun)
  {
    if (o == null)
      System.out.println("mischief detected!");
    try {
      File f = File.createTempFile("DoeResults", ".xml", outDir);
      f.deleteOnExit();
      FileWriter fw = new FileWriter(f);
      fw.write(o);
      fw.close();
      writeStatus("Result saved to " + f.getAbsolutePath());
      //outputs.put("" + dp + "," + nrun, f);
      int idx = outputList.size();
      outputList.add(new Object[]{new Integer(dp), new Integer(nrun), f.getAbsolutePath()});
      return idx;
    }
    catch (IOException e) {
      writeStatus("error saving output for run " + dp + ", " + nrun + ": " + e.getMessage());
    }
    return -1;
  }

  JFrame clusterStatusFrame;
  JEditorPane editorPane;
  JScrollPane editorScrollPane;
  URL statusURL;
  Thread statusThread;

  private void showClusterStatus(String surl)
  {
    if (clusterStatusFrame == null) {
      clusterStatusFrame = new JFrame("Cluster Status");
      editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorScrollPane = new JScrollPane(editorPane);
      editorScrollPane.setVerticalScrollBarPolicy(
          JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      editorScrollPane.setPreferredSize(new Dimension(680, 800)); //640,480));
      editorScrollPane.setMinimumSize(new Dimension(10, 10));

      clusterStatusFrame.getContentPane().setLayout(new BorderLayout());
      clusterStatusFrame.getContentPane().add(editorScrollPane);
    }

    try {
      statusURL = new URL(surl);
      editorPane.setPage(statusURL);
    }
    catch (Exception e) {
      System.out.println("Error showing cluster status: " + e.getMessage());
      return;
    }

    clusterStatusFrame.pack();
    Rectangle frR = clusterStatusFrame.getBounds();

    Rectangle r = this.getBounds();
/*
    frR.x = r.x + r.width / 2 - frR.width / 2;
    frR.y = r.y + r.height / 2 - frR.height / 2;
*/
    frR.x = r.x + r.width;
    frR.y = r.y; //chartter.getLocation().y + chartter.getSize().height;
    clusterStatusFrame.setBounds(frR);

    clusterStatusFrame.setVisible(true);

    // uncomment the following if you want continuous updates
    /*
    stopStatusThread(); // if running
    statusThread = new Thread(new statusUpdater());
    statusThread.start();
    */

  }

  private void kickOffClusterUpdate()
  {
    if (waitToGo) {
      waitToGo = false;
      statusThread.interrupt();
    }
  }

  private void hideClusterStatus()
  {
    if (clusterStatusFrame != null) {
      clusterStatusFrame.setVisible(false);
    }
    stopStatusThread();
  }

  private void stopStatusThread()
  {
    if (statusThread != null) {
      Thread t = statusThread;
      statusThread = null;
      int pr = Thread.currentThread().getPriority();
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      t.interrupt();
      t.interrupt();
      t.interrupt();
      t.interrupt();
      Thread.currentThread().setPriority(pr);
      Thread.yield();

    }
  }

  boolean waitToGo = true;

  class statusUpdater implements Runnable
  {
    public void run()
    {
      if (waitToGo)
        try {
          Thread.sleep(60000);
        }
        catch (InterruptedException e) {
        }

      while (statusThread != null && clusterStatusFrame != null) {
        try {
          Thread.sleep(10000);

          // to refresh
          javax.swing.text.Document doc = editorPane.getDocument();
          doc.putProperty(javax.swing.text.Document.StreamDescriptionProperty, null);
          // I'm trying to control the scroll bar position after loading, but it doesn't
          // seem to work (somewhat confirmed by reading the forums) when HTML is being rendered.
          final JScrollBar hbar = editorScrollPane.getHorizontalScrollBar();
          final JScrollBar vbar = editorScrollPane.getHorizontalScrollBar();
          editorPane.setPage(statusURL); // same page
          editorPane.setCaretPosition(editorPane.getDocument().getLength());
          int hm = hbar.getMaximum();
          int vm = vbar.getMaximum();
          SwingUtilities.invokeLater(new Runnable()
          {
            public void run()
            {
              hbar.setValue(50);
              vbar.setValue(50); //vbar.getMaximum());
            }
          });
        }
        catch (Exception e) {
          System.out.println("statusUpdater kill: " + e.getMessage());
        }
      }
    }
  }


  private String namePrefix = "Viskit Cluster Job Controller";
  private String currentTitle = namePrefix;
  private void doTitle(String nm)
  {
    if(nm != null && nm.length()>0)
      currentTitle = namePrefix +": "+nm;

    if(titlLis != null)
      titlLis.setTitle(currentTitle,titlIdx);
  }


  TitleListener titlLis;
  int titlIdx;
  public void setTitleListener(TitleListener tLis, int idx)
  {
    titlLis = tLis;
    titlIdx = idx;
    doTitle(null);
  }

  public static class Gresults
  {
    String listener = "";
    String property = "";
    int run = -1;
    int dp = -1;

    public static final int COUNT = 0;
    public static final int MINOBS = 1;
    public static final int MAXOBS = 2;
    public static final int MEAN = 3;
    public static final int VARIANCE = 4;
    public static final int STDDEV = 5;

    boolean resultsValid = false;

    int resultsCount;
    double resultsMinObs;
    double resultsMaxObs;
    double resultsMean;    //if < 1.0, a terrorist succeeded
    double resultsVariance;
    double resultsStdDev;
  }

  /**
   *  a subclass to make the tooltip text for a JTextField = to the content
   */
  class ttJTextField extends JTextField
  {
    ttJTextField(int wid)
    {
      super(wid);
    }

    public String getToolTipText(MouseEvent event)
    {
      String s = getText();
      if(s != null && s.length()>0)
        return s;
      return null;
    }
  }

  // This is code to manage server, port, user and password from CommonsConfig.
  private static XMLConfiguration vConfig;
  private static String recentClusterKey = "history.Cluster.Account(0)";
  private Key cryptoKey;

  private void initConfig()
  {
    try {
      vConfig = VGlobals.instance().getHistoryConfig();
    }
    catch (Exception e) {
      System.out.println("Error loading config file: "+e.getMessage());
      vConfig = null;
    }
    serverCfg = vConfig.getString(recentClusterKey+"[@server]");
    if(serverCfg == null || serverCfg.length()<=0)
      serverCfg = "127.0.0.1";
    portCfg = vConfig.getString(recentClusterKey+"[@port]");
    if(portCfg == null || portCfg.length()<=0)
      portCfg = "4444";
    String unameCfg = vConfig.getString(recentClusterKey+"[@username]");
    String pwordCfg = vConfig.getString(recentClusterKey+"[@password]");

    cryptoKey = CryptoMethods.getTheKey();

    if(unameCfg != null && unameCfg.length()>0)
      unameDec = CryptoMethods.doDecryption(unameCfg,cryptoKey);
    else
      unameDec = "username";

    if(pwordCfg != null && pwordCfg.length()>0)
      pwordDec = CryptoMethods.doDecryption(pwordCfg,cryptoKey);
    else {
      pwordDec = "password";
      pwordCfg = "password"; // just to put asterisks in the tf
    }
  }

  private void writeConfig()
  {
    unameDec = unameTF.getText().trim();
    pwordDec = new String(upwPF.getPassword());
    String unameCfg = CryptoMethods.doEncryption(unameDec,cryptoKey);
    String pwordCfg = CryptoMethods.doEncryption(pwordDec,cryptoKey);
    portCfg   = portTF.getText().trim();
    serverCfg = clusterTF.getText().trim();

    vConfig.setProperty(recentClusterKey+"[@server]",serverCfg);
    vConfig.setProperty(recentClusterKey+"[@port]",portCfg);
    vConfig.setProperty(recentClusterKey+"[@username]",unameCfg);
    vConfig.setProperty(recentClusterKey+"[@password]",pwordCfg);
  }

  private void addDesignPointStatsToGraphs(Hashtable ret, int d, int s) {
      if (viskit.Vstatics.debug) System.out.println("StatsGraph: addDesignPointStatsToGraphs at designPoint "+d+" sample "+s);
      if (viskit.Vstatics.debug) System.out.println(ret);
      java.util.Enumeration stats = ret.elements();
      while ( stats.hasMoreElements() ) {
          String data = (String)stats.nextElement();
          try {
              if (viskit.Vstatics.debug) System.out.println("\tAdding data "+data);
              SampleStatisticsType sst = (SampleStatisticsType)unmarshaller.unmarshal(new ByteArrayInputStream(data.getBytes()));
              
              statsGraph.addSampleStatistic(sst,d,s);
          } catch (JAXBException ex) {
              ex.printStackTrace();
          }
      }
      
  }



}
