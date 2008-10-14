/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.movesinstitute.org)
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

package viskit.doe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.security.Key;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import edu.nps.util.CryptoMethods;
import edu.nps.util.TempFileManager;
import static edu.nps.util.GenericConversion.toArray;
import org.apache.commons.configuration.XMLConfiguration;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import viskit.*;
import viskit.xsd.bindings.assembly.Experiment;
import viskit.xsd.bindings.assembly.SampleStatistics;
import viskit.xsd.bindings.assembly.Schedule;
import viskit.xsd.bindings.assembly.SimkitAssembly;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 18, 2006
 * @since 2:50:08 PM
 * @version $Id$
 */
public class JobLauncherTab2 extends JPanel implements Runnable, OpenAssembly.AssyChangeListener {

    DoeRunDriver doe;
    Hashtable statsGraphs;
    BlockingQueue<DesignPointStatsWrapper> bQ;
    String inputFileString;
    File inputFile;
    File filteredFile;
    FileReader fr;
    PrintWriter out;
    BufferedReader br;
    JFrame mom;
    String lineEnd = System.getProperty("line.separator");
    int defaultClusterPort = 4444;

    // TODO: single variable for all viskit
    String clusterDNS = "wipeout.hpr.nps.edu";
    String clusterName = clusterDNS;
    String clusterWebStatus1 = "http://" + clusterDNS + "/ganglia/";
    String clusterWebStatus2 = "http://" + clusterDNS + "/ganglia/?m=cpu_user&r=hour&s=descending&c=MOVES&h=&sh=1&hc=3";
    String clusterWebStatus = "http://" + clusterDNS + "/ganglia/?r=hour&c=MOVES&h=&sh=0";

    // Configuration file data
    private String serverCfg;
    private String portCfg;
    private String unameDecrCfg = "";
    private String pwordDecrCfg = "";
    private JButton canButt;
    private JButton runButt;
    private JButton adminButt;
    private JButton dotDotButt;
    private JPasswordField upwPF;
    private JTextArea statusTextArea;
    private JTextField numCubesTF;
    private JTextField clusterTF;
    private JTextField clusNameReadOnlyTF;
    private JTextField portTF;
    private JTextField numRepsTF;
    private JTextField numDPsTF;
    private JTextField tmo;
    private JTextField unameTF;
    private JCheckBox doAnalystReports;
    private JCheckBox doGraphOutput;
    private JCheckBox doLocalRun;
    private GraphUpdater graphUpdater;
    private QstatConsole qstatConsole;
    private StatsGraph statsGraph;
    private Thread thread;
    private boolean outputDirty = false;
    private String title;
    private DoeController cntlr;
    private SimkitAssembly jaxbRoot;
    private Unmarshaller unmarshaller;
    private JPanel clusterConfigPanel;
    private Boolean clusterConfigReturn = null;
    private JDialog configDialog;
    private JAXBContext jaxbCtx;

    public JobLauncherTab2(DoeController controller, String file, String title, JFrame mainFrame) {
        try {
            jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly", this.getClass().getClassLoader());
            unmarshaller = jaxbCtx.createUnmarshaller();
        } catch (JAXBException je) {
            je.printStackTrace();
        }
        this.title = title;
        cntlr = controller;
        mom = mainFrame;
        buildContent();
        doListeners();

        setFile(file, title);

        setGridMode();
    }

    public Container getContent() {
        return this;
    }

    private JPanel buildClusterConfigPanel() {
        JPanel clusPan = new JPanel(new SpringLayout());
        //clusPan.setBorder(new EtchedBorder());

        JLabel clusLab = new JLabel("Target grid engine");
        Vstatics.clampHeight(clusLab);
        clusterTF = new ttJTextField(15);
        clusterTF.setText(serverCfg);//clusterDNS);
        clusterTF.setToolTipText("dummy"); // overridden
        Vstatics.clampHeight(clusterTF);
        JLabel portLab = new JLabel("RPC port");
        Vstatics.clampHeight(portLab);
        portTF = new ttJTextField(10);
        portTF.setToolTipText("dummy"); // overridden
        portTF.setText(portCfg);
        Vstatics.clampHeight(portTF);
        JLabel unameLab = new JLabel("User name");
        Vstatics.clampHeight(unameLab);
        Vstatics.clampMaxSize(unameLab);
        unameTF = new JTextField(10);
        unameTF.setText(unameDecrCfg);
        Vstatics.clampHeight(unameTF);
        JLabel upwLab = new JLabel("Password");
        upwPF = new JPasswordField(10);
        upwPF.setText(pwordDecrCfg);
        Vstatics.clampHeight(upwPF);
        JPanel adminPan = new JPanel();
        adminButt = new JButton("admin");
        adminPan.setLayout(new BoxLayout(adminPan, BoxLayout.X_AXIS));
        adminPan.add(clusterTF);
        adminPan.add(adminButt);
        JLabel localRunLab = new JLabel("Run Locally");
        doLocalRun = new JCheckBox();


        clusPan.add(clusLab);
        clusPan.add(adminPan); //clusterTF);
        clusPan.add(unameLab);
        clusPan.add(unameTF);
        clusPan.add(portLab);
        clusPan.add(portTF);
        clusPan.add(upwLab);
        clusPan.add(upwPF);
        clusPan.add(localRunLab);
        clusPan.add(doLocalRun);
        clusPan.add(new JLabel());
        clusPan.add(new JLabel());
        clusPan.add(new JLabel());
        clusPan.add(new JLabel());
        SpringUtilities.makeCompactGrid(clusPan, 3, 4, 10, 10, 10, 5);
        Dimension d = clusPan.getPreferredSize();
        clusPan.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        buttPan.setBorder(new EmptyBorder(5, 10, 10, 10));
        buttPan.add(Box.createHorizontalGlue());
        JButton cancelButt = new JButton("Cancel");
        JButton okButt = new JButton("Apply changes");
        buttPan.add(cancelButt);
        buttPan.add(okButt);

        JPanel allPan = new JPanel();
        allPan.setLayout(new BoxLayout(allPan, BoxLayout.Y_AXIS));
        allPan.add(clusPan);
        allPan.add(buttPan);

        cancelButt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clusterConfigReturn = new Boolean(false);
                configDialog.dispose();
                configDialog = null;
            }
        });
        okButt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clusterConfigReturn = new Boolean(true);
                configDialog.dispose();
                configDialog = null;
            }
        });

        doLocalRun.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (doLocalRun.getModel().isSelected()) {
                    clusterTF.setText("localhost");
                    unameTF.setEnabled(false);
                    portTF.setEnabled(false);
                    upwPF.setEnabled(false);
                    adminButt.setEnabled(false);
                    clusterTF.setEnabled(false);
                    gridMode = false;
                } else {
                    clusterTF.setText(clusterName);
                    unameTF.setEnabled(true);
                    portTF.setEnabled(true);
                    upwPF.setEnabled(true);
                    adminButt.setEnabled(true);
                    clusterTF.setEnabled(true);
                    gridMode = true;
                }
            }
        });

        return allPan;
    }

    private JPanel buildClusterPanel() {
        JPanel clusNameP = new JPanel();
        clusNameP.setLayout(new BoxLayout(clusNameP, BoxLayout.X_AXIS));
        clusNameP.add(Box.createHorizontalStrut(5));
        clusNameP.add(new JLabel("Grid machine    "));
        clusNameReadOnlyTF = new JTextField(10);
        clusNameReadOnlyTF.setText(serverCfg);
        clusNameReadOnlyTF.setEditable(false);
        clusNameP.add(clusNameReadOnlyTF);
        dotDotButt = new JButton("...");
        clusNameP.add(dotDotButt);
        clusNameP.add(Box.createHorizontalStrut(5));
        Dimension d = clusNameP.getPreferredSize();
        clusNameP.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        return clusNameP;
    }

    private JPanel buildExpPanel() {
        JPanel topPan = new JPanel(new SpringLayout());

        JLabel dpLab = new JLabel("Design point variables");
        numDPsTF = new JTextField(6);

        JLabel sampLab = new JLabel("Hypercubes");
        numCubesTF = new ttJTextField(20);

        numRepsTF = new JTextField(6);
        JLabel repLab = new JLabel("Replications");
        tmo = new JTextField(6);
        JLabel tmoLab = new JLabel("Replication time out (ms)");

        JLabel analystReportLab = new JLabel("Analyst report each run");
        doAnalystReports = new JCheckBox((String) null, false);
        //JLabel doGraphLab = new JLabel("Graph job output");
        //JLabel doGraphLab = new JLabel("AnalystReports each run");
        doGraphOutput = new JCheckBox((String) null, false);

        numDPsTF.setEditable(false);

        topPan.add(dpLab);
        topPan.add(numDPsTF);
        topPan.add(sampLab);
        topPan.add(numCubesTF);
        topPan.add(repLab);
        topPan.add(numRepsTF);
        topPan.add(tmoLab);
        topPan.add(tmo);
        topPan.add(analystReportLab); // tooltip this with warning 
        topPan.add(doAnalystReports);
        //topPan.add(doGraphLab);
        //topPan.add(doGraphOutput);

        SpringUtilities.makeCompactGrid(topPan, 5, 2, 10, 10, 5, 5);
        topPan.setMaximumSize(new Dimension(topPan.getPreferredSize()));
        topPan.setMinimumSize(new Dimension(20, 20));
        topPan.setBorder(new EtchedBorder());
        return topPan;
    }
    JSplitPane leftSplit;
    JSplitPane rightSplit;

    private Container buildContent() {
        initConfig();
        clusterConfigPanel = buildClusterConfigPanel();

        setLayout(new BorderLayout());

        JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        JPanel controlP = new JPanel();
        controlP.setLayout(new BoxLayout(controlP, BoxLayout.Y_AXIS));

        controlP.add(buildClusterPanel());
        controlP.add(Box.createVerticalStrut(5));
        controlP.add(buildExpPanel());
        controlP.add(Box.createVerticalStrut(5));

        JPanel vPan = new JPanel();
        vPan.setLayout(new BoxLayout(vPan, BoxLayout.X_AXIS));
        canButt = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Stop24.gif")));
        canButt.setToolTipText("Stop the Grid run");
        canButt.setEnabled(false);
        canButt.setBorder(BorderFactory.createEtchedBorder());
        canButt.setText(null);
        vPan.add(canButt);

        runButt = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Play24.gif")));
        runButt.setToolTipText("Begin the Grid run");
        runButt.setBorder(BorderFactory.createEtchedBorder());
        runButt.setText(null);
        vPan.add(runButt);
        vPan.add(Box.createHorizontalGlue());
        controlP.add(vPan);

        controlP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        qstatConsole = new QstatConsole();

        leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controlP, qstatConsole.getContent());
        leftSplit.setDividerLocation(180);

        statusTextArea = new JTextArea("Grid system console:" + lineEnd +
                "--------------------" + lineEnd);
        //statusTextArea.setBackground(new Color(0xFB, 0xFB, 0xE5));
        statusTextArea.setBackground(Color.BLACK);
        statusTextArea.setForeground(Color.GREEN);
        statusTextArea.setEditable(false);
        statusTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statusJsp = new JScrollPane(statusTextArea);

        JTextArea serrTA = new JTextArea("Grid error console:" + lineEnd +
                "-------------------" + lineEnd);
        serrTA.setForeground(Color.red);
        serrTA.setEditable(false);
        serrTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        serrTA.setBackground(new Color(0xFB, 0xFB, 0xE5));

        statsGraph = new StatsGraph();
        JScrollPane stgSp = new JScrollPane(statsGraph);
        rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusJsp, stgSp);
        leftRightSplit.setLeftComponent(leftSplit);
        leftRightSplit.setRightComponent(rightSplit);
        rightSplit.setDividerLocation(180);
        leftRightSplit.setDividerLocation(300);
        add(leftRightSplit, BorderLayout.CENTER);

        return this; //p;
    }

    public void setFile(String file, String title) {
        if (file == null) {
            inputFileString = null;
            inputFile = null;
            filteredFile = null;
            return;
        }
        inputFileString = file;
        inputFile = new File(file);
        filteredFile = inputFile;      // will be possibly changed

        try {
            filteredFile = TempFileManager.createTempFile("DoeInputFile", ".xml");
        } catch (IOException e) {
            System.out.println("couldn't make temp file");
        }

        try {
            getParams();
        } catch (Exception e) {
            e.printStackTrace();
        }
        doTitle(title);
    }

    /**
     * This is where an open assembly gets mentioned here
     *
     * @param jaxbRoot
     * @param file
     */
    public void setAssemblyFile(SimkitAssembly jaxbRoot, File file) {
        this.jaxbRoot = jaxbRoot;
        numDPsTF.setText("" + jaxbRoot.getDesignParameters().size());
        setFile(file.getAbsolutePath(), file.getName());
    }

    public void closeAssemblyFile(File file) {
    //todo do something here to put up a "no-file" banner or equivalent
    }

    public void refreshAssemblyFile(File file) {
    // nothing here
    }
    private KeyListener myEditListener = new KeyAdapter() {

        @Override
        public void keyTyped(KeyEvent e) {
            //System.out.println("sending paramlocallyeditted from JobLauncherTab");
            OpenAssembly.inst().doParamLocallyEditted(JobLauncherTab2.this);     // inform who is listening that we tweeked the params
        }
    };

    private void getParams() {
        Experiment exp = jaxbRoot.getExperiment();    // todo cast requirement jaxb error?
        if (exp != null) {
            //designPts = exp.getDesignPoint().size();
            int numDesignPts = jaxbRoot.getDesignParameters().size();
            numDPsTF.setText("" + numDesignPts);
            String s = exp.getTotalSamples();
            if (s != null) {
                numCubesTF.setText(s);
            }
            s = exp.getReplicationsPerDesignPoint();
            if (s != null) {
                numRepsTF.setText(s);
            }

            s = exp.getTimeout();
            if (s != null) {
                tmo.setText(s);
            }
        } else {
            exp = OpenAssembly.inst().jaxbFactory.createExperiment();

            jaxbRoot.setExperiment(exp);

            exp.setTotalSamples("1");
            numCubesTF.setText("1");
            exp.setReplicationsPerDesignPoint("1");
            numRepsTF.setText("1");
            exp.setTimeout("5000");
            tmo.setText("5000");
            int numDesignPts = jaxbRoot.getDesignParameters().size();
            numDPsTF.setText("" + numDesignPts);
        }
    }

    class svLister implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            // The user has hit the save button;
            saveParamsToJaxbNoNotify();
            OpenAssembly.inst().doSendAssyJaxbChanged(JobLauncherTab2.this);
        }
    }

    public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param) {
    }

    public String getHandle() {
        return "Launch Cluster Job";
    }

    private void saveParamsToJaxbNoNotify() {
        // Put the params from the GUI into the jaxbRoot
        int numDesignPts = jaxbRoot.getDesignParameters().size();
        numDPsTF.setText("" + numDesignPts);

        Experiment exp = jaxbRoot.getExperiment();
        Schedule sch = jaxbRoot.getSchedule();
        if (exp == null) {
            exp = OpenAssembly.inst().jaxbFactory.createExperiment();
        }
        if (sch == null) {
            sch = OpenAssembly.inst().jaxbFactory.createSchedule();
        }

        String reps = numRepsTF.getText().trim();
        try {
            Integer.parseInt(reps);
        } catch (NumberFormatException e) {
            reps = "1";
            System.err.println("Bad number of replications...use 1");
        }
        sch.setNumberReplications(reps);                            // rg: 2
        exp.setReplicationsPerDesignPoint(reps);

        String samps = numCubesTF.getText().trim();
        try {
            Integer.parseInt(samps);
        } catch (NumberFormatException e) {
            samps = "1";
            System.err.println("Bad number of samples...use 1");
        }
        exp.setTotalSamples(samps);                                // rg: 5

        exp.setJitter("true");
        exp.setType("latin-hypercube");

        String stopTime = tmo.getText().trim();
        try {
            Double.parseDouble(stopTime);
        } catch (NumberFormatException e) {
            stopTime = "1000.0";
            System.err.println("Bad stop time...use 1000");
        }
        sch.setStopTime(stopTime);

        sch.setVerbose("true");

        jaxbRoot.setSchedule(sch);
        jaxbRoot.setExperiment(exp);
    }

    private void doListeners() {
        canButt.setActionCommand("cancel");
        runButt.setActionCommand("run");
        adminButt.setActionCommand("admin");
        dotDotButt.setActionCommand("dotdot");

        ActionListener al = new ButtListener();
        canButt.addActionListener(al);
        runButt.addActionListener(al);
        dotDotButt.addActionListener(al);
        adminButt.addActionListener(al);

        numCubesTF.addKeyListener(myEditListener);
        numRepsTF.addKeyListener(myEditListener);
        tmo.addKeyListener(myEditListener);
    }

    /**
     * Save off the exp stuff for a moment; to be restored
     *
     * @param exp
     */
    private void saveExp(Experiment exp) {
        //eType = exp.getType();
        eTmo = exp.getTimeout();
        //eJitter = exp.getJitter();
        eRpdp = exp.getReplicationsPerDesignPoint();
        eTotSamp = exp.getTotalSamples();
    //eBatchID = exp.getBatchID();
    //eDbg = exp.getDebug();
    //eScript = exp.getScript();
    //List getDesignPoint();


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
    private void restoreExp(Experiment exp) {
        exp.setTotalSamples(eTmo);
        exp.setReplicationsPerDesignPoint(eRpdp);
        exp.setTotalSamples(eTotSamp);
    }

    private void doStartRun() {
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
        saveExp(jaxbRoot.getExperiment());
        saveParamsToJaxbNoNotify();

        filteredFile = cntlr.doTempFileMarshall();
        cntlr.restorePrepRun();

        thread = new Thread(JobLauncherTab2.this);
        thread.setPriority(Thread.NORM_PRIORITY); // don't inherit swing event thread prior
        thread.start();

        statusTextArea.setText("");
    }

    class ButtListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
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
                    } catch (NumberFormatException e1) {
                        System.err.println("Bad number parse: " + e1.getMessage() + "; using " + defaultClusterPort);
                        port = defaultClusterPort;
                    }
                    ClusterAdminDialog.showDialog(clusterTF.getText(), port, configDialog, mom);
                    break;
                case 'd': // dot dot
                    configDialog = new JDialog(mom, "Cluster Configuration", true);
                    configDialog.setContentPane(clusterConfigPanel);
                    configDialog.pack();
                    configDialog.setLocationRelativeTo(mom);
                    clusterConfigReturn = null;
                    configDialog.setVisible(true);

                    if (clusterConfigReturn.booleanValue()) {// true means apply
                        unloadServerWidgets();
                        writeConfig();
                    }

                    break;
                case 'x':
                    runButt.setEnabled(true);  // for next time (probably not used)
                    canButt.setEnabled(false);
                    if (outputDirty) {
                        if (JOptionPane.showConfirmDialog(JobLauncherTab2.this, "Save output?") == JOptionPane.YES_OPTION) {
                            JFileChooser jfc = new JFileChooser();
                            jfc.setSelectedFile(new File("DOEOutput.txt"));
                            jfc.showSaveDialog(JobLauncherTab2.this);
                            if (jfc.getSelectedFile() != null) {
                                File f = jfc.getSelectedFile();
                                try {
                                    FileWriter fw = new FileWriter(f);
                                    fw.write(statusTextArea.getText());
                                    fw.close();
                                } catch (IOException e1) {
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
    private void stopRun() {
        outputList.clear();

        canButt.setEnabled(false);
        runButt.setEnabled(true);

        if (thread == null) {
            return;
        }

        writeStatus("Stopping run.");
        hideClusterStatus();

        Thread jobKiller = new Thread(new Runnable() {

            public void run() {
                if (thread != null) {
                    Thread t = thread;
                    thread = null;
                    t.interrupt();
                    try {
                        t.join(1000);
                    } catch (InterruptedException e) {
                        System.err.println("join exception");
                    }
                }
                try {
                    doe.clear();
                    doe = null; // will cause doe to logout() on GC if it's a grid run
                } catch (Exception e) {
                    System.err.println("DoeException: " + e.getMessage());
                }

            }
        }, "JobKiller");
        jobKiller.setPriority(Thread.NORM_PRIORITY);
        jobKiller.start();
    }

    void writeStatus(final String s) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                statusTextArea.append(s);
                statusTextArea.append("\n");
            }
        });
    }

    private void addEventGraphFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int rdRet;
        while ((rdRet = fis.read(buf)) > 0) {
            baos.write(buf, 0, rdRet);
        }
        fis.close();
        baos.flush();
        baos.close();
        String egText = new String(baos.toByteArray());

        doe.addEventGraph(egText);
    }
    StringWriter data;

    // remove, for testing some loader stuff while writing it
    public void runTesting() {
        writeStatus("JobLauncherTab2.run()");
        LocalBootLoader loader = new LocalBootLoader(new URL[]{}, Thread.currentThread().getContextClassLoader(), viskit.VGlobals.instance().getWorkDirectory());
        //loader.setTab(this);

        // loader gets own copy of Viskit's libs, init method here
        // will return it properly with any eventgraph's in a jar
        // already loaded in its classpath
        loader = loader.init();


        for (URL line : loader.getURLs()) {
            writeStatus("URL: " + line.toString());
        }

    }
    // tbd gui: set gridMode true if logging into a remote service, or false if
    // it should run locally
    private boolean gridMode = true;

    public void run() {
        try {
            if (gridMode) {
                doe = new RemoteDriverImpl(clusterTF.getText().trim(), Integer.parseInt(portTF.getText().trim()), unameTF.getText().trim(), new String(upwPF.getPassword()));
            } else {
                doe = new LocalDriverImpl(SettingsDialog.getExtraClassPathArraytoURLArray(), viskit.VGlobals.instance().getWorkDirectory());
            }
            System.gc();
            qstatConsole.setDoe(doe);

            statsGraph = new StatsGraph();
            rightSplit.setBottomComponent(new JScrollPane(statsGraph));
            rightSplit.setDividerLocation(180);

            //boolean doClustStat = this.doClusterStat.isSelected();
            //boolean doGraphOut = this.doGraphOutput.isSelected();


            outputDirty = true;
            outputList = new ArrayList<Object[]>();
            lp3:
            {
                try {
                    createOutputDir();

                    // Send EventGraphs
                    Collection egs = cntlr.getLoadedEventGraphs();

                    for (Iterator itr = egs.iterator(); itr.hasNext();) {
                        addEventGraphFile((File) itr.next());
                    }

                    // Construct assembly
                    fr = new FileReader(filteredFile);
                    br = new BufferedReader(fr);
                    data = new StringWriter();
                    out = new PrintWriter(data);
                    String line;
                    while ((line = br.readLine()) != null) {
                        out.println('\t' + line);
                    }
                    out.flush();
                    out.close();

                    String dataS = data.toString().trim();

                    doe.setAssembly(dataS);

                    if (viskit.Vstatics.debug) {
                        writeStatus(dataS);
                    }

                    writeStatus("Executing job");


                } catch (Exception e) {
                    e.printStackTrace();
                    writeStatus("Error: " + e.getMessage());


                    doe = null; // will cause GC to hit finally() which in grid will logout()

                }

                // Bring up the 2 other windows
                //if (doClustStat)
                //showClusterStatus(clusterWebStatus);
                //if(doGraphOut)
                //chartter = new JobResults(null, title);
                statsGraphSet = false;
                writeStatus("Getting results:");
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                //processResults();
                ProcessResults processResults = new ProcessResults(doe, jaxbRoot, statsGraph);
                processResults.execute();

            }


        } catch (DoeException de) {
            writeStatus(de.toString());
        }
    }
    private boolean statsGraphSet = false;

    class ProcessResults extends SwingWorker<Void, Void> {

        SimkitAssembly jaxbRoot;
        DoeRunDriver doe;
        StatsGraph statsGraph;

        public ProcessResults(DoeRunDriver doe, SimkitAssembly jaxbRoot, StatsGraph statsGraph) {
            this.doe = doe;
            this.jaxbRoot = jaxbRoot;
            this.statsGraph = statsGraph;
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        }

        protected Void doInBackground() throws Exception {
            processResults();
            return (Void) null;
        }

        // TODO: XML RPC v2.0 not updated to generics
        @SuppressWarnings("unchecked")
        private void processResults() {
            Hashtable ret;
            Experiment exp = jaxbRoot.getExperiment();
            int samples = Integer.parseInt(exp.getTotalSamples());
            int designPoints = jaxbRoot.getDesignParameters().size();

            ArrayList<Object> lastQueue;

            try {

                lastQueue = doe.getTaskQueue();
                int totalTasks = lastQueue.size();
                int tasksRemaining = totalTasks;
                writeStatus("Total tasks: " + totalTasks);
                //writeStatus("Started tasks: " + totalTasks - tasksRemaining);
                bQ = new ArrayBlockingQueue<DesignPointStatsWrapper>(totalTasks);
                graphUpdater = new GraphUpdater(bQ, statsGraph);
                doe.run();
                graphUpdater.execute();
                while (tasksRemaining > 0) {
                    try {
                        // this will block until a task ends which could be
                        // because it died, or because it completed, either way
                        // check the logs returned by getResults will tell.
                        ArrayList<Object> queue = doe.getTaskQueue();
                        List<Object> sQueue = Collections.synchronizedList(queue);
                        synchronized (sQueue) {
                            ListIterator li = sQueue.listIterator();
                            int i = 0;
                            while (li.hasNext()) {
                                //for (int i = 0; i < totalTasks; i ++) {
                                // trick: any change between queries indicates a transition at
                                // taskID = i (well i+1 really, taskID's in SGE start at 1)
                                //if (!((Boolean) lastQueue.get(i)).equals(((Boolean) queue.get(i)))) {

                                boolean state = ((Boolean) li.next()).booleanValue();

                                if (((Boolean) lastQueue.get(i)).booleanValue() != state) {
                                    int sampleIndex = i / designPoints;
                                    int designPtIndex = i % designPoints;

                                    if (/*verbose*/true) {
                                        //ret = doe.getResult(sampleIndex,designPtIndex);
                                        writeStatus("Result returned from task " + (i + 1) + " leaving " + tasksRemaining + " to go");
                                    //writeStatus(ret.toString());
                                    }

                                    ret = doe.getDesignPointStats(sampleIndex, designPtIndex);

                                    if (statsGraphSet == false) {
                                        String[] properties = toArray(ret.keySet(), new String[0]);
                                        statsGraph.setProperties(properties, designPoints, samples);
                                        statsGraphSet = true;
                                    }
                                    addDesignPointStatsToGraphs(ret, designPtIndex, sampleIndex);
                                    writeStatus("DesignPointStats from task " + (i + 1) + " at sampleIndex " + sampleIndex + " at designPtIndex " + designPtIndex);

                                    writeStatus("Replications per designPt " + exp.getReplicationsPerDesignPoint());
                                    for (int j = 0; j < Integer.parseInt(exp.getReplicationsPerDesignPoint()); j++) {
                                        writeStatus("ReplicationStats from task " + (i + 1) + " replication " + j);
                                        ret = doe.getReplicationStats(sampleIndex, designPtIndex, j);
                                    }
                                    --tasksRemaining;
                                    lastQueue.set(i, new Boolean(state));
                                }
                                i++;
                                System.gc();
                                System.runFinalization();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                writeStatus("Error in cluster execution: " + e.getMessage());
            }
            stopRun();
        }
    }
    ArrayList<Object[]> outputList;

    private void createOutputDir() throws Exception {
        outDir = File.createTempFile("DoeRun", "");
        outDir.delete();
        outDir.mkdir();
    }
    JobResults chartter;

    private void plotOutput(int idx) {
        if (chartter == null) {
            chartter = new JobResults(null, title);
        }
        synchronized (outputList) {
            Object[] oa = outputList.get(idx);
            Gresults res = getSingleResult(oa);
            chartter.addPoint(res);
            if (!res.resultsValid) {
                System.out.println("Results not retrieved for rep " + idx);
            }
        }
    }

    private Gresults getSingleResult(Object[] oa) {
        File f = new File((String) oa[2]);
        int dp = ((Integer) oa[0]).intValue();
        int nrun = ((Integer) oa[1]).intValue();
        Gresults res = new Gresults();

        Document doc = null;
        try {
            doc = FileHandler.unmarshallJdom(f);
        } catch (Exception e) {
            System.out.println("Error unmarshalling results: " + e.getMessage());
            return null;
        }
        Element el = doc.getRootElement();
        if (!el.getName().equals("Results")) {
            System.out.println("Unknown results format, design point = " + dp + ", run = " + nrun);
            return res;
        }
        String design = attValue(el, "design");
        //String index = attValue(el, "index");
        //String job = attValue(el, "job");
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
        if (viskit.Vstatics.debug) {
            System.out.println("got back " + cstr);
        }
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
        if (res.run != nrun) {
            System.err.println("JobLauncher.doResults");
        }

        res.dp = Integer.parseInt(design);
        //assert res.dp == dp : "JobLauncher.doResults1";
        if (res.dp != dp) {
            System.err.println("JobLauncher.doResults1");
        }

        res.resultsCount = Integer.parseInt(nums[Gresults.COUNT]);
        res.resultsMinObs = Double.parseDouble(nums[Gresults.MINOBS]);
        res.resultsMaxObs = Double.parseDouble(nums[Gresults.MAXOBS]);
        res.resultsMean = Double.parseDouble(nums[Gresults.MEAN]);
        res.resultsVariance = Double.parseDouble(nums[Gresults.VARIANCE]);
        res.resultsStdDev = Double.parseDouble(nums[Gresults.STDDEV]);

        res.resultsValid = true;
        return res;
    }

    String attValue(Element e, String att) {
        Attribute at = e.getAttribute(att);
        return (at != null ? at.getValue() : null);
    }
    File outDir;

    private int saveOutput(String o, int dp, int nrun) {
        if (o == null) {
            System.out.println("mischief detected!");
        }
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
        } catch (IOException e) {
            writeStatus("error saving output for run " + dp + ", " + nrun + ": " + e.getMessage());
        }
        return -1;
    }
    JFrame clusterStatusFrame;
    JEditorPane editorPane;
    JScrollPane editorScrollPane;
    URL statusURL;
    Thread statusThread;

    private void showClusterStatus(String surl) {
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
        } catch (Exception e) {
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

    private void kickOffClusterUpdate() {
        if (waitToGo) {
            waitToGo = false;
            statusThread.interrupt();
        }
    }

    private void hideClusterStatus() {
        if (clusterStatusFrame != null) {
            clusterStatusFrame.dispose();
        }
        stopStatusThread();
    }

    private void stopStatusThread() {
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

    class statusUpdater implements Runnable {

        public void run() {
            if (waitToGo) {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                }
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
                    //int hm = hbar.getMaximum();
                    //int vm = vbar.getMaximum();
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            hbar.setValue(50);
                            vbar.setValue(50); //vbar.getMaximum());
                        }
                    });
                } catch (Exception e) {
                    System.out.println("statusUpdater kill: " + e.getMessage());
                }
            }
        }
    }
    private String namePrefix = "Viskit Cluster Job Controller";
    private String currentTitle = namePrefix;

    private void doTitle(String nm) {
        if (nm != null && nm.length() > 0) {
            currentTitle = namePrefix + ": " + nm;
        }

        if (titlLis != null) {
            titlLis.setTitle(currentTitle, titlIdx);
        }
    }
    TitleListener titlLis;
    int titlIdx;

    public void setTitleListener(TitleListener tLis, int idx) {
        titlLis = tLis;
        titlIdx = idx;
        doTitle(null);
    }

    public static class Gresults {

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
     * a subclass to make the tooltip text for a JTextField = to the content
     */
    class ttJTextField extends JTextField {

        ttJTextField(int wid) {
            super(wid);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            String s = getText();
            if (s != null && s.length() > 0) {
                return s;
            }
            return null;
        }
    }

    // This is code to manage server, port, user and password from CommonsConfig.
    private static XMLConfiguration vConfig;
    private static String recentClusterKey = "history.Cluster.Account(0)";
    private Key cryptoKey;

    private void initConfig() {
        try {
            vConfig = ViskitConfig.instance().getViskitConfig();
        } catch (Exception e) {
            System.out.println("Error loading config file: " + e.getMessage());
            vConfig = null;
        }
        serverCfg = vConfig.getString(recentClusterKey + "[@server]");
        if (serverCfg == null || serverCfg.length() <= 0) {
            serverCfg = "localhost";
        }

        if (serverCfg.equals("localhost")) {

            gridMode = false;
        }

        portCfg = vConfig.getString(recentClusterKey + "[@port]");
        if (portCfg == null || portCfg.length() <= 0) {
            portCfg = "4444";
        }
        String unameEncrCfg = vConfig.getString(recentClusterKey + "[@username]");
        String pwordEncrCfg = vConfig.getString(recentClusterKey + "[@password]");

        cryptoKey = CryptoMethods.getTheKey();

        if (unameEncrCfg != null && unameEncrCfg.length() > 0) {
            unameDecrCfg = CryptoMethods.doDecryption(unameEncrCfg, cryptoKey);
        } else {
            unameDecrCfg = "username";
        }

        if (pwordEncrCfg != null && pwordEncrCfg.length() > 0) {
            pwordDecrCfg = CryptoMethods.doDecryption(pwordEncrCfg, cryptoKey);
        } else {
            pwordDecrCfg = "password";
        }
    }

    private void writeConfig() {
        vConfig.setProperty(recentClusterKey + "[@server]", serverCfg);
        vConfig.setProperty(recentClusterKey + "[@port]", portCfg);
        vConfig.setProperty(recentClusterKey + "[@username]", CryptoMethods.doEncryption(unameDecrCfg, cryptoKey));
        vConfig.setProperty(recentClusterKey + "[@password]", CryptoMethods.doEncryption(pwordDecrCfg, cryptoKey));
    }

    private void unloadServerWidgets() {
        unameDecrCfg = unameTF.getText().trim();
        pwordDecrCfg = new String(upwPF.getPassword());
        portCfg = portTF.getText().trim();
        serverCfg = clusterTF.getText().trim();
        clusNameReadOnlyTF.setText(serverCfg);
    }

    private void addDesignPointStatsToGraphs(Hashtable ret, int d, int s) {

        // for the SwingWorker to publish a single object 
        bQ.add(new DesignPointStatsWrapper(ret, d, s));

    }

    class GraphUpdater extends SwingWorker<Void, DesignPointStatsWrapper> {

        BlockingQueue<DesignPointStatsWrapper> bQ;
        StatsGraph statsGraph;

        GraphUpdater(BlockingQueue<DesignPointStatsWrapper> bQ, StatsGraph statsGraph) {
            this.bQ = bQ; // bbq
            this.statsGraph = statsGraph;
        }

        @Override
        public Void doInBackground() {
            DesignPointStatsWrapper dp;

            try {
                while ((dp = bQ.take()) != null) {
                    publish(dp);
                }
            } catch (InterruptedException ie) {
                ;
            }

            return null;
        }

        @Override
        protected void process(List<DesignPointStatsWrapper> chunks) {
            for (DesignPointStatsWrapper dp : chunks) {
                java.util.Enumeration stats = dp.statsReturned.elements();
                while (stats.hasMoreElements()) {
                    String data = (String) stats.nextElement();
                    try {
                        // creating an unmarshaller each time here is supposed to be more thread safe
                        // if slower, however, this only gets hit by the swing thread
                        // unmarshaller = jaxbCtx.createUnmarshaller();
                        if (viskit.Vstatics.debug) {
                            System.out.println("\tAdding data " + data);
                        }
                        SampleStatistics sst = (SampleStatistics) unmarshaller.unmarshal(new ByteArrayInputStream(data.getBytes()));

                        statsGraph.addSampleStatistic(sst, dp.designPtIndex, dp.sampleIndex);
                    } catch (JAXBException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    class DesignPointStatsWrapper {

        public Hashtable statsReturned;
        public int designPtIndex;
        public int sampleIndex;

        DesignPointStatsWrapper(Hashtable statsReturned, int designPtIndex, int sampleIndex) {
            this.statsReturned = statsReturned;
            this.designPtIndex = designPtIndex;
            this.sampleIndex = sampleIndex;
        }
    }

    private void setGridMode() {
        if (!gridMode) {
            doLocalRun.getModel().setSelected(true);
            clusterTF.setText("localhost");
            unameTF.setEnabled(false);
            portTF.setEnabled(false);
            upwPF.setEnabled(false);
            adminButt.setEnabled(false);
            clusterTF.setEnabled(false);
        }
    }
}
