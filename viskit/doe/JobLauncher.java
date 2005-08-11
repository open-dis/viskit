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

import org.apache.xmlrpc.XmlRpcClientLite;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import viskit.SpringUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Vector;
import java.util.ArrayList;
import java.net.URL;

public class JobLauncher extends JFrame implements Runnable
{
  String inputFileString;
  File inputFile;
  File filteredFile;
  FileReader fr;
  PrintWriter out;
  BufferedReader br;
  XmlRpcClientLite rpc;
  private JTextArea ta;
  JFrame mom;

  String clusterDNS = "cluster.moves.nps.navy.mil";
  int    clusterPort = 4445;
  int    chosenPort;
  String clusterWebStatus1 = "http://cluster.moves.nps.navy.mil/ganglia/";
  String clusterWebStatus2 = "http://cluster.moves.nps.navy.mil/ganglia/?m=cpu_user&r=hour&s=descending&c=MOVES&h=&sh=1&hc=3";
  String clusterWebStatus  = "http://cluster.moves.nps.navy.mil/ganglia/?r=hour&c=MOVES&h=&sh=0";
  private JButton canButt;
  private JButton runButt;
  private JButton closeButt;
  private Document doc;
  private JTextField sampsTF;
  private JTextField portTF;
  private JTextField runs;
  private JTextField dps;
  private JTextField tmo;

  private Thread thread;
  private boolean outputDirty = false;
  private int numRuns,designPts,samps;

  public JobLauncher(String file, String title, JFrame mainFrame)
  {
    super("Job " + title);
    inputFileString = file;
    inputFile = new File(file);
    filteredFile = inputFile;      // will be possibly changed

    try {
      filteredFile = File.createTempFile("DoeInputFile",".xml");
    }
    catch (IOException e) {
      System.out.println("couldn't make temp file");
    }

    mom = mainFrame;

    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

    JPanel topPan = new JPanel(new SpringLayout());
    ta = new JTextArea(20, 30);
    JScrollPane jsp = new JScrollPane(ta);
    JPanel botBar = new JPanel();
    botBar.setLayout(new BoxLayout(botBar, BoxLayout.X_AXIS));

    JTextField cluster = new JTextField(clusterDNS);
    JLabel clusLab = new JLabel("Target grid engine");
    sampsTF = new JTextField(6);
    JLabel portLab = new JLabel("RPC port");
    portTF = new JTextField(6);
    JLabel dpLab = new JLabel("Design points");
    dps = new JTextField(6);
    JLabel sampLab = new JLabel("Hypercubes");
    runs = new JTextField(6);
    JLabel runLab = new JLabel("Replications");
    tmo = new JTextField(6);
    JLabel tmoLab = new JLabel("Replication time out (ms)");

    dps.setEditable(false);
    cluster.setEditable(false);
    //cluster.setBackground(samps.getBackground());

    try {
      getParams();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    topPan.add(clusLab);
    topPan.add(cluster);
    topPan.add(portLab);
    topPan.add(portTF);
    topPan.add(dpLab);
    topPan.add(dps);
    topPan.add(sampLab);
    topPan.add(sampsTF);
    topPan.add(runLab);
    topPan.add(runs);
    topPan.add(tmoLab);
    topPan.add(tmo);

    SpringUtilities.makeCompactGrid(topPan, 6, 2, 10, 10, 5, 5);
    topPan.setMaximumSize(topPan.getPreferredSize());

    canButt = new JButton("Cancel job");
    canButt.setEnabled(false);
    runButt = new JButton("Run job");
    closeButt = new JButton("Close");
    botBar.add(Box.createHorizontalGlue());
    botBar.add(canButt);
    botBar.add(runButt);
    botBar.add(Box.createHorizontalStrut(20));
    botBar.add(closeButt);

    p.add(topPan);
    p.add(jsp);
    p.add(Box.createVerticalStrut(8));
    p.add(botBar);
    p.setBorder(new EmptyBorder(10, 10, 10, 10));
    setContentPane(p);

    pack();
    Dimension d = getSize();
    d.width+=50;
    setSize(d);
    centerMe();

    setVisible(true);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        canButt.doClick();
      }
    });
    doListeners();
  }

  private void getParams() throws Exception
  {
    doc = FileHandler.unmarshallJdom(inputFile);
    Element root = doc.getRootElement();

    Element exp = root.getChild("Experiment");

    designPts = root.getChildren("TerminalParameter").size();
    dps.setText(""+designPts);

    String att = exp.getAttributeValue("totalSamples");
    if(att != null)
      sampsTF.setText(att);

    att = exp.getAttributeValue("runsPerDesignPoint");
    if(att != null)
      runs.setText(att);

    numRuns = Integer.parseInt(att);
    att = exp.getAttributeValue("timeout");
    tmo.setText(att);

    portTF.setText(""+clusterPort);
  }
  private void setParams() throws Exception
  {
    Element root = doc.getRootElement();
    Element exp = root.getChild("Experiment");
    samps = Integer.parseInt(sampsTF.getText());
    designPts = Integer.parseInt(dps.getText());
    numRuns = Integer.parseInt(runs.getText());
    chosenPort = Integer.parseInt(portTF.getText());

    exp.setAttribute("totalSamples",""+samps);
    exp.setAttribute("runsPerDesignPoint",""+numRuns);
    exp.setAttribute("timeout",tmo.getText().trim());
    //exp.setAttribute("debug","false");
    FileHandler.marshallJdom(filteredFile,doc);
  }

  private void doListeners()
  {
    canButt.setActionCommand("cancel");
    runButt.setActionCommand("run");
    closeButt.setActionCommand("x");
    ActionListener al = new ButtListener();
    canButt.addActionListener(al);
    runButt.addActionListener(al);
    closeButt.addActionListener(al);
  }

  class ButtListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      switch (e.getActionCommand().charAt(0)) {
        case 'r':
          runButt.setEnabled(false);
          canButt.setEnabled(true);
          closeButt.setEnabled(false);
          thread = new Thread(JobLauncher.this);
          thread.start();
          break;
        case 'c':
          stopRun();
          break;
        case 'x':
          runButt.setEnabled(true);  // for next time (probably not used)
          canButt.setEnabled(false);
          if(outputDirty ) {
            if (JOptionPane.showConfirmDialog(JobLauncher.this, "Save output?") == JOptionPane.YES_OPTION) {
              JFileChooser jfc = new JFileChooser();
              jfc.setSelectedFile(new File("DOEOutput.txt"));
              jfc.showSaveDialog(JobLauncher.this);
              if (jfc.getSelectedFile() != null) {
                File f = jfc.getSelectedFile();
                try {
                  FileWriter fw = new FileWriter(f);
                   fw.write(ta.getText());
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
          assert false:"Program error JobLauncher.java";
      }
    }
  }

  private void centerMe()
  {
    Rectangle meR = getBounds();
    if(mom != null) {
      Rectangle r = mom.getBounds();
      meR.x = r.x + r.width / 2 - meR.width / 2;
      meR.y = r.y + r.height / 2 - meR.height / 2;
    }
    setBounds(meR);
  }

  private void stopRun()
  {
    outputList.clear();

    canButt.setEnabled(false);
    closeButt.setEnabled(true);
    runButt.setEnabled(true);

    writeStatus("Stopping run.");
    try {
      Vector parms = new Vector();
      //o = rpc.execute("experiment.flushQueue",parms);
      Object o = rpc.execute("experiment.clear", parms);
      writeStatus("flushQueue = " + o);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    if(thread != null) {
      Thread t = thread;
      thread = null;
      t.interrupt();
    }

    hideClusterStatus();
  }

  private void writeStatus(final String s)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        ta.append(s);
        ta.append("\n");
      }
    });
  }

  StringWriter data;

  public void run()
  {
    outputDirty = true;
    outputList = new ArrayList();
    lp3:
    {
    try {
      createOutputDir();
      setParams(); // runs, sampls, tmo

      writeStatus("Building XML-RPC client to " + clusterDNS + ".");
      rpc = new XmlRpcClientLite(clusterDNS, chosenPort);
      fr = new FileReader(filteredFile);
      //fr = new FileReader("/users/mike/Desktop/rickBrem.grd"); //Bremerton_1.grd");
      br = new BufferedReader(fr);
      data = new StringWriter();
      out = new PrintWriter(data);
      String line;
      while ((line = br.readLine()) != null) {
        out.println('\t' + line);
      }
      out.close();

      Vector parms = new Vector();
      parms.add(data.toString());

      writeStatus("Sending job file to " + clusterDNS);
      Object o = rpc.execute("experiment.setAssembly", parms);
      writeStatus("experiment.setAssembly returned " + o);

    }
    catch (Exception e) {
      writeStatus("Error connecting to server: " + e.getMessage());
      thread = null;
      break lp3;
    }
    // Bring up the 2 other windows
    showClusterStatus(clusterWebStatus);
    chartter = new JobResults(JobLauncher.this);

    writeStatus("10 second wait before getting results.");
    try {
      Thread.sleep(10000);
    }
    catch (InterruptedException e) {
      thread=null;
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
            writeStatus("gotResult " + dp + "," + nrun + " (" + i + " of " + n + ")");
            int idx = saveOutput((String) o, dp, nrun);
            if(idx != -1)
              plotOutput(idx);
            else
              System.out.println("Output not saved");
          }
          catch (Exception e) {
            writeStatus("Error from experiment.getResult(): " + e.getMessage());
            if (thread == null)
              break lp;
          }

        }
      }
    } // lp
    } // lp3
 //   doResults();
    stopRun();
  }


  //HashMap outputs = new HashMap();
  ArrayList outputList;
  private void createOutputDir() throws Exception
  {
    outDir = File.createTempFile("DoeRun","");
    outDir.delete();
    outDir.mkdir();
  }

  JobResults chartter;
  private void plotOutput(int idx)
  {
    if(chartter == null)
      chartter = new JobResults(JobLauncher.this);
    synchronized(outputList) {
      Object[] oa = (Object[])outputList.get(idx);
      Gresults res = getSingleResult(oa);
      if(res != null)
        chartter.addPoint(res);
      else
        System.out.println("Results not retrieved for rep "+idx);
    }
  }

  private Gresults getSingleResult(Object[] oa)
  {
    File f = new File((String)oa[2]);
    int dp = ((Integer)oa[0]).intValue();
    int nrun = ((Integer)oa[1]).intValue();

    Document doc =  null;
    try {
      doc = FileHandler.unmarshallJdom(f);
    }
    catch (Exception e) {
      System.out.println("Error unmarshalling results: "+e.getMessage());
      return null;
    }
    Element el = doc.getRootElement();
    if(!el.getName().equals("Results")) {
      System.out.println("Unknown results format, design point = "+dp+", run = "+nrun);
      return null;
    }
    String design = attValue(el,"design");
    String index = attValue(el,"index");
    String job = attValue(el,"job");
    String run = attValue(el,"run");

    Element propCh = el.getChild("PropertyChange");
    if(propCh == null) {
      System.out.println("PropertyChange results element null, design point = "+dp+", run = "+nrun);
      return null;
    }
    String listenerName = attValue(propCh,"listenerName");
    String property = attValue(propCh,"property");
    java.util.List content = propCh.getContent();
    Text txt = (Text)content.get(0);
    String cstr = txt.getTextTrim();
    System.out.println("got back "+cstr);
    String[] sa = cstr.split("\n");
    if(sa.length != 2) {
      System.out.println("PropertyChange parse error, design point = "+dp+", run = "+nrun);
      return null;
    }
    sa[1] = sa[1].trim();
    String[] nums = sa[1].split("\\s+");
    Gresults res = new Gresults();
    res.listener = listenerName;
    res.property = property;
    res.run = Integer.parseInt(run);
    assert res.run == nrun :"JobLauncher.doResults";

    res.dp = Integer.parseInt(design);
    assert res.dp == dp : "JobLauncher.doResults1";
    boolean[] ba = new boolean[nums.length];
    for(int j=0;j<ba.length;j++) {
      double d = Double.parseDouble(nums[j]);
      ba[j] = d != 0.0d;
    }
    res.results = ba;
    return res;
  }
/*
  private void doResults()
  {
    Vector v = new Vector();
      synchronized(outputList) {
      for(int i=0;i<outputList.size();i++) {
        try {
        Object[] oa = (Object[])outputList.get(i);
        File f = new File((String)oa[2]);
        int dp = ((Integer)oa[0]).intValue();
        int nrun = ((Integer)oa[1]).intValue();


        Document doc =  FileHandler.unmarshallJdom(f);
        System.out.println("bp");
        Element el = doc.getRootElement();
        if(!el.getName().equals("Results")) {
          System.out.println("Unknown results format, design point = "+dp+", run = "+nrun);
          continue;
        }
        String design = attValue(el,"design");
        String index = attValue(el,"index");
        String job = attValue(el,"job");
        String run = attValue(el,"run");

        Element propCh = el.getChild("PropertyChange");
        if(propCh == null) {
          System.out.println("PropertyChange results element null, design point = "+dp+", run = "+nrun);
          continue;
        }
        String listenerName = attValue(propCh,"listenerName");
        String property = attValue(propCh,"property");
        java.util.List content = propCh.getContent();
        Text txt = (Text)content.get(0);
        String cstr = txt.getTextTrim();
        String[] sa = cstr.split("\n1");
        if(sa.length != 2) {
          System.out.println("PropertyChange parse error, design point = "+dp+", run = "+nrun);
          continue;
        }
        sa[1] = sa[1].trim();
        String[] nums = sa[1].split("\\s+");
        Gresults res = new Gresults();
        res.listener = listenerName;
        res.property = property;
        res.run = Integer.parseInt(run);
        assert res.run == nrun :"JobLauncher.doResults";

        res.dp = Integer.parseInt(design);
        assert res.dp == dp : "JobLauncher.doResults1";
        boolean[] ba = new boolean[nums.length];
        for(int j=0;j<ba.length;j++) {
          double d = Double.parseDouble(nums[j]);
          ba[j] = d != 0.0d;
        }
        res.results = ba;
        v.add(res);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
    }
      }

  // to test  if(v.size() > 0)
      new JobResults(JobLauncher.this,v);
  }
 */
  String attValue(Element e, String att)
  {
    Attribute at = e.getAttribute(att);
    return (at!=null?at.getValue():null);
  }
  File outDir;
  private int  saveOutput(String o, int dp, int nrun)
  {
    if(o == null)
      System.out.println("mischief detected!");
    try {
      File f = File.createTempFile("DoeResults", ".xml", outDir);
      f.deleteOnExit();
      FileWriter fw = new FileWriter(f);
      fw.write(o);
      fw.close();
      writeStatus("Result saved to "+f.getAbsolutePath());
      //outputs.put("" + dp + "," + nrun, f);
      int idx = outputList.size();
      outputList.add(new Object[]{new Integer(dp),new Integer(nrun),f.getAbsolutePath()});
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
    if(clusterStatusFrame == null) {
      clusterStatusFrame = new JFrame("Cluster Status");
      editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorScrollPane = new JScrollPane(editorPane);
      editorScrollPane.setVerticalScrollBarPolicy(
                      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      editorScrollPane.setPreferredSize(new Dimension(680,800)); //640,480));
      editorScrollPane.setMinimumSize(new Dimension(10, 10));

      clusterStatusFrame.getContentPane().setLayout(new BorderLayout());
      clusterStatusFrame.getContentPane().add(editorScrollPane);
    }

    try {
      statusURL = new URL(surl);
      editorPane.setPage(statusURL);
    }
    catch (Exception e) {
      System.out.println("Error showing cluster status: "+e.getMessage());
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

    stopStatusThread(); // if running
    statusThread = new Thread(new statusUpdater());
    statusThread.start();

  }

  private void hideClusterStatus()
  {
    if(clusterStatusFrame != null) {
      clusterStatusFrame.setVisible(false);
    }
    stopStatusThread();
  }

  private void stopStatusThread()
  {
    if(statusThread != null) {
      Thread t = statusThread;
      statusThread = null;
      t.interrupt();
      Thread.yield();
    }
  }

  class statusUpdater implements Runnable
  {
    public void run()
    {
      while (statusThread != null && clusterStatusFrame != null) {
        try{
          Thread.sleep(5000);
          System.out.println("updating cluster status");

          // to refresh
          javax.swing.text.Document doc = editorPane.getDocument();
          doc.putProperty(javax.swing.text.Document.StreamDescriptionProperty, null);

          final JScrollBar hbar = editorScrollPane.getHorizontalScrollBar();
          System.out.println("h value = "+hbar.getValue());
          final JScrollBar vbar = editorScrollPane.getHorizontalScrollBar();
          editorPane.setPage(statusURL); // same page
          editorPane.setCaretPosition(editorPane.getDocument().getLength());
          //editorPane.revalidate();
          int hm = hbar.getMaximum();
          int vm = vbar.getMaximum();
          SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
              hbar.setValue(50);
              vbar.setValue(50); //vbar.getMaximum());
            }
          });
       }
        catch(Exception e) {
          System.out.println("statusUpdater kill: "+e.getMessage());
        }
      }
    }
  }
  public static void main(String[] args)
  {
    if(args.length != 1)
      System.out.println("Give .grd file as argument");
    else
      new JobLauncher(args[0],args[0],null);
  }

  public static class Gresults
  {
    String listener;
    String property;
    int run;
    int dp;
    boolean[] results;
  }

}
