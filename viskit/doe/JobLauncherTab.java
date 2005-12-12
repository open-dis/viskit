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
import viskit.xsd.bindings.assembly.Experiment;
import viskit.xsd.bindings.assembly.Schedule;
import viskit.xsd.bindings.assembly.SimkitAssembly;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.security.Key;
import java.util.ArrayList;
import java.util.Vector;

public class JobLauncherTab extends JPanel implements Runnable, OpenAssembly.AssyChangeListener
{
  String inputFileString;
  File inputFile;
  File filteredFile;
  FileReader fr;
  PrintWriter out;
  BufferedReader br;
  XmlRpcClientLite rpc;
  private JTextArea statusTextArea;
  JFrame mom;

  String clusterDNS = "cluster.moves.nps.navy.mil";
  int clusterPort = 4444;
  int chosenPort;
  String clusterWebStatus1 = "http://cluster.moves.nps.navy.mil/ganglia/";
  String clusterWebStatus2 = "http://cluster.moves.nps.navy.mil/ganglia/?m=cpu_user&r=hour&s=descending&c=MOVES&h=&sh=1&hc=3";
  String clusterWebStatus  = "http://cluster.moves.nps.navy.mil/ganglia/?r=hour&c=MOVES&h=&sh=0";
  private JButton canButt;
  private JButton runButt;
  private SimkitAssembly jaxbRoot;
  private JTextField sampsTF;
  private JTextField portTF;
  private JTextField runs;
  private JTextField dps;
  private JTextField tmo;

  private Thread thread;
  private boolean outputDirty = false;
  private int numRuns, designPts, samps;

  private String serverCfg;
  private String portCfg;
  private String unameCfg;
  private String pwordCfg;
  private String unameDec;
  private String pwordDec;
  private JTextField unameTF;
  private JPasswordField upwPF;
  private JTextField clusterTF;

  private String title;
  private DoeController cntlr;
  public JCheckBox doClusterStat;
  public JCheckBox doGraphOutput;

  public JobLauncherTab(DoeController controller, String file, String title, JFrame mainFrame)
  {
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
    unameLab.setEnabled(false);
    unameTF = new JTextField(10);
    unameTF.setText("unimplemented"); //unameDec);
    unameTF.setEnabled(false);
    JLabel upwLab = new JLabel("Password");
    upwLab.setEnabled(false);
    upwPF = new JPasswordField(10);
    upwPF.setText(pwordDec);
    upwPF.setEnabled(false);
    clusPan.add(clusLab);
    clusPan.add(clusterTF);
    clusPan.add(unameLab);
    clusPan.add(unameTF);
    clusPan.add(portLab);
    clusPan.add(portTF);
    clusPan.add(upwLab);
    clusPan.add(upwPF);


    JLabel dpLab = new JLabel("Design point variables");
    dps = new JTextField(6);

    JLabel sampLab = new JLabel("Hypercubes");
    sampsTF = new ttJTextField(20);
    runs = new JTextField(6);
    JLabel runLab = new JLabel("Replications");
    tmo = new JTextField(6);
    JLabel tmoLab = new JLabel("Replication time out (ms)");

    dps.setEditable(false);

//    topPan.add(clusLab);
//    topPan.add(clusterCB);
//    topPan.add(portLab);
//    topPan.add(portTF);
    topPan.add(dpLab);
    topPan.add(dps);
    topPan.add(sampLab);
    topPan.add(sampsTF);
    topPan.add(runLab);
    topPan.add(runs);
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
    botBar.add(doClusterStat);
    botBar.add(Box.createHorizontalStrut(5));
    botBar.add(doGraphOutput);
    botBar.add(Box.createHorizontalGlue());
    botBar.add(canButt);
    botBar.add(runButt);

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
      System.out.println("sending paramlocallyeditted from JobLauncherTab");
      OpenAssembly.inst().doParamLocallyEditted(JobLauncherTab.this);     // inform who is listening that we tweeked the params
    }
  };

  private void getParams()
  {
    Experiment exp = (Experiment)jaxbRoot.getExperiment();    // todo cast requirement jaxb error?
    if(exp != null) {
      //designPts = exp.getDesignPoint().size();
      designPts = jaxbRoot.getDesignParameters().size();        //todo  which is it?
      dps.setText("" + designPts);
      String s = exp.getTotalSamples();
      if(s != null)
        sampsTF.setText(s);
      s = exp.getRunsPerDesignPoint();
      if(s != null)
        runs.setText(s);

      numRuns = Integer.parseInt(s);
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
      sampsTF.setText("1");
      exp.setRunsPerDesignPoint("1");
      runs.setText("1");
      exp.setTimeout("5000");
      tmo.setText("5000");
      designPts = jaxbRoot.getDesignParameters().size();
      dps.setText(""+designPts);

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

  public void assyChanged(int action, OpenAssembly.AssyChangeListener source)
  {
  }

  public String getHandle()
  {
    return "Launch Cluster Job";
  }

  private void saveParamsToJaxbNoNotify()
  {
    // Put the params from the GUI into the jaxbRoot
    Experiment exp = (Experiment)jaxbRoot.getExperiment();
    exp.setTotalSamples(sampsTF.getText().trim());
    exp.setRunsPerDesignPoint(runs.getText().trim());
    exp.setTimeout(tmo.getText().trim());
    // todo resolve designpoints

    samps = Integer.parseInt(sampsTF.getText());
    designPts = Integer.parseInt(dps.getText());
    numRuns = Integer.parseInt(runs.getText());
    chosenPort = Integer.parseInt(portTF.getText());

    Schedule sch = (Schedule)jaxbRoot.getSchedule();
    sch.setNumberReplications("1");
    sch.setPrintReplicationReports("false");
    sch.setPrintSummaryReport("false");
    sch.setSaveReplicationData("false");
    sch.setStopTime("100000"); //todo
    sch.setVerbose("false");
  }


 /* private void oldgetParams() throws Exception
  {
    doc = FileHandler.unmarshallJdom(inputFile);
    Element root = doc.getRootElement();

    Element exp = root.getChild("Experiment");
    if (exp != null) {
      designPts = root.getChildren("TerminalParameter").size();
      dps.setText("" + designPts);

      String att = exp.getAttributeValue("totalSamples");
      if (att != null)
        sampsTF.setText(att);

      att = exp.getAttributeValue("runsPerDesignPoint");
      if (att != null)
        runs.setText(att);

      numRuns = Integer.parseInt(att);
      att = exp.getAttributeValue("timeout");
      tmo.setText(att);
    }
    else {
      exp = new Element("Experiment");

      root.addContent(exp);
      Element tp = new Element("TerminalParameter");
      root.addContent(tp);
      exp.setAttribute("totalSamples","1");
      exp.setAttribute("runsPerDesignPoint","1");
      exp.setAttribute("timeout","5000");

      dps.setText("1");
      sampsTF.setText("1");
      runs.setText("1");
      numRuns = 1;
      tmo.setText("5000");
    }
    portTF.setText("" + clusterPort);
  }
  */
  private void setLocalParamVars() throws Exception
  {
    samps = Integer.parseInt(sampsTF.getText());
    designPts = Integer.parseInt(dps.getText());
    numRuns = Integer.parseInt(runs.getText());
    chosenPort = Integer.parseInt(portTF.getText());

    clusterDNS = clusterTF.getText().trim();
  }

  private void doListeners()
  {
    canButt.setActionCommand("cancel");
    runButt.setActionCommand("run");
    ActionListener al = new ButtListener();
    canButt.addActionListener(al);
    runButt.addActionListener(al);

    sampsTF.addKeyListener(myEditListener);
    runs.addKeyListener(myEditListener);
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
    eRpdp = exp.getRunsPerDesignPoint();
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
    exp.setRunsPerDesignPoint(eRpdp);
    exp.setTotalSamples(eTotSamp);
  }

  private void doStartRun()
  {
    runButt.setEnabled(false);
    canButt.setEnabled(true);

    // call back to the controller to put design parms and egs in place in a temp file

    if (cntlr.prepRun() == false)
      return;

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
            //o = rpc.execute("experiment.flushQueue",parms);
            Object o = rpc.execute("experiment.clear", parms);
            //writeStatus("flushQueue = " + o);
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

  StringWriter data;

  public void run()
  {
    boolean doClustStat = this.doClusterStat.isSelected();
    boolean doGraphOut    = this.doGraphOutput.isSelected();

    outputDirty = true;
    outputList = new ArrayList();
    lp3:
    {
      try {
        createOutputDir();
        setLocalParamVars(); // runs, sampls, tmo

        writeStatus("Building XML-RPC client to " + clusterDNS + ".");
        rpc = new XmlRpcClientLite(clusterDNS, chosenPort);
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
        String[]sa = dataS.split("<\\?xml.*\\?>"); // remove the hdr if present
        if(sa.length == 2)
          dataS = sa[1].trim();

        Vector parms = new Vector();
        parms.add(dataS);

        writeStatus("Sending job file to " + clusterDNS);
        Object o = rpc.execute("experiment.setAssembly", parms);
        writeStatus("experiment.setAssembly returned " + o);

      }
      catch (Exception e) {
        if (thread != null)    // If normal error:
          writeStatus("Error connecting to server: " + e.getMessage());
        break lp3;
      }
      // Bring up the 2 other windows
      if(doClustStat)
        showClusterStatus(clusterWebStatus);
      if(doGraphOut)
        chartter = new JobResults(null, title);

      //writeStatus("10 second wait before getting results.");
      try {
        Thread.sleep(10000);
      }
      catch (InterruptedException e) {
        break lp3;
      }
      writeStatus("Getting results:");

      Vector parms = new Vector();
      Object o = null;
      int i = 0;
      int n = designPts * samps * numRuns;
      lp:
      {
        for (int dp = 0; dp < designPts * samps; dp++) {
          for (int nrun = 0; nrun < numRuns; nrun++, i++) {
            try {
              parms.clear();
              parms.add(new Integer(dp));
              parms.add(new Integer(nrun));
              o = rpc.execute("experiment.getResult", parms);
              if (thread == null)
                break lp;
              kickOffClusterUpdate();
              writeStatus("gotResult " + dp + "," + nrun + " (" + i + " of " + n + ")");
              int idx = saveOutput((String) o, dp, nrun);
              if (idx != -1 && doGraphOut)
                plotOutput(idx);
              else
                System.out.println("Output not saved");
            }
            catch (Exception e) {
              if (thread != null)
                writeStatus("Error from experiment.getResult(): " + e.getMessage());
              break lp;
            }
          }
        }
      } // lp
    } // lp3
    stopRun();
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
    java.util.List content = propCh.getContent();
    Text txt = (Text) content.get(0);
    String cstr = txt.getTextTrim();
    System.out.println("got back " + cstr);
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
    if (waitToGo == true) {
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
      if (waitToGo == true)
        try {
          statusThread.sleep(60000);
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
    unameCfg = vConfig.getString(recentClusterKey+"[@username]");
    pwordCfg = vConfig.getString(recentClusterKey+"[@password]");

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
    unameCfg = CryptoMethods.doEncryption(unameDec,cryptoKey);
    pwordCfg = CryptoMethods.doEncryption(pwordDec,cryptoKey);
    portCfg   = portTF.getText().trim();
    serverCfg = clusterTF.getText().trim();

    vConfig.setProperty(recentClusterKey+"[@server]",serverCfg);
    vConfig.setProperty(recentClusterKey+"[@port]",portCfg);
    vConfig.setProperty(recentClusterKey+"[@username]",unameCfg);
    vConfig.setProperty(recentClusterKey+"[@password]",pwordCfg);
  }



}
