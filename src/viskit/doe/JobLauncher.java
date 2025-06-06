/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

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
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.Log4jUtilities;
import edu.nps.util.SpringUtilities;
import edu.nps.util.TempFileManager;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import viskit.util.TitleListener;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 21, 2005
 * @since 12:29:08 PM
 * @version $Id$
 */
public class JobLauncher extends JFrame implements Runnable, DirectoryWatch.DirectoryChangeListener 
{
    static final Logger LOG = LogManager.getLogger();

    File inputFile;
    File filteredFile;
    FileReader fr;
    PrintWriter out;
    BufferedReader br;
    XmlRpcClientLite rpc;
    private JTextArea ta;
    JFrame mom;

    // TODO: single variable for all viskit
    String clusterDNS = "wipeout.hpr.nps.edu";
    int clusterPort = 4_444;
    int chosenPort;
    String clusterWebStatus1 = "http://" + clusterDNS + "/ganglia/";
    String clusterWebStatus2 = "http://" + clusterDNS + "/ganglia/?m=cpu_user&r=hour&s=descending&c=MOVES&h=&sh=1&hc=3";
    String clusterWebStatus  = "http://" + clusterDNS + "/ganglia/?r=hour&c=MOVES&h=&sh=0";
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
    private int numRuns,  designPts,  samps;
    private Container content;
    private boolean isSubComponent = false;

    public JobLauncher() {
        super();
        content = buildContent();
        doListeners();
    }

    public JobLauncher(boolean isSubComponent, String file, String title, JFrame mainFrame) {
        this();
        setTitle("Job " + title);
        this.isSubComponent = isSubComponent;
        mom = mainFrame;
        setContentPane(content);
        setFile(file, title);

        if (!isSubComponent) {
            try {
                getParams();
            } 
            catch (Exception e) 
            {
                LOG.error("runFile(" + isSubComponent + ", " + file + ", " + title + ") exception: " + e.getMessage());
//                e.printStackTrace(System.err);
            }

            pack();
            Dimension d = getSize();
            d.width += 50;
            setSize(d);
            //centerMe();
            setLocation(10, 10);
            setVisible(true);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    canButt.doClick();
                }
            });
        }
    }

    public Container getContent() {
        return content;
    }

    private Container buildContent() {
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
        JLabel dpLab = new JLabel("Design point variables");
        dps = new JTextField(6);
        JLabel sampLab = new JLabel("Hypercubes");
        runs = new JTextField(6);
        JLabel runLab = new JLabel("Replications");
        tmo = new JTextField(6);
        JLabel tmoLab = new JLabel("Replication time out (ms)");

        dps.setEditable(false);
        cluster.setEditable(false);
        //cluster.setBackground(samps.getBackground());

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
        botBar.add(Box.createHorizontalGlue());
        botBar.add(canButt);
        botBar.add(runButt);
        if (!isSubComponent) {
            botBar.add(Box.createHorizontalStrut(20));
            closeButt = new JButton("Close");
            botBar.add(closeButt);
        }
        p.add(topPan);
        p.add(jsp);
        p.add(Box.createVerticalStrut(8));
        p.add(botBar);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    public void setFile(String file, String title) {
        if (file == null) {
            inputFile = null;
            filteredFile = null;
            return;
        }
        inputFile = new File(file);
        filteredFile = inputFile;      // will be possibly changed

        try {
            filteredFile = TempFileManager.createTempFile("DoeInputFile", ".xml");
        } catch (IOException e) {
            LOG.info("couldn't make temp file");
        }

        try {
            getParams();
        } 
        catch (Exception e) 
        {
            LOG.error("setFile(" + file + ", " + title + ") exception: " + e.getMessage());
//            e.printStackTrace(System.err);
        }
        doTitle(title);
    }

    private void getParams() throws Exception {
        doc = FileHandler.unmarshallJdom(inputFile);
        Element root = doc.getRootElement();

        Element exp = root.getChild("Experiment");
        if (exp != null) {
            designPts = root.getChildren("TerminalParameter").size();
            dps.setText("" + designPts);

            String att = exp.getAttributeValue("totalSamples");
            if (att != null) {
                sampsTF.setText(att);
            }

            att = exp.getAttributeValue("runsPerDesignPoint");
            if (att != null) {
                runs.setText(att);
            }

            numRuns = Integer.parseInt(att);
            att = exp.getAttributeValue("timeout");
            tmo.setText(att);
        } else {
            exp = new Element("Experiment");

            root.addContent(exp);
            Element tp = new Element("TerminalParameter");
            root.addContent(tp);
            exp.setAttribute("totalSamples", "1");
            exp.setAttribute("runsPerDesignPoint", "1");
            exp.setAttribute("timeout", "5000");

            dps.setText("1");
            sampsTF.setText("1");
            runs.setText("1");
            numRuns = 1;
            tmo.setText("5000");
        }
        portTF.setText("" + clusterPort);
    }

    private void setParams() throws Exception {
        Element root = doc.getRootElement();
        Element exp = root.getChild("Experiment");
        samps = Integer.parseInt(sampsTF.getText());
        designPts = Integer.parseInt(dps.getText());
        numRuns = Integer.parseInt(runs.getText());
        chosenPort = Integer.parseInt(portTF.getText());

        exp.setAttribute("totalSamples", "" + samps);
        exp.setAttribute("runsPerDesignPoint", "" + numRuns);
        exp.setAttribute("timeout", tmo.getText().trim());
        //exp.setAttribute("debug","false");

        FileHandler.marshallJdom(filteredFile, doc, true);
    }

    private void doListeners() {
        canButt.setActionCommand("cancel");
        runButt.setActionCommand("run");
        ActionListener al = new ButtListener();
        canButt.addActionListener(al);
        runButt.addActionListener(al);
        if (!isSubComponent) {
            closeButt.setActionCommand("x");
            closeButt.addActionListener(al);
        }
    }

    /* Here's where we are informed of changed in the assembly file */
    @Override
    public void fileChanged(File file, int action, DirectoryWatch source) {
        // temp:
        switch (action) {
            case DirectoryWatch.DirectoryChangeListener.FILE_ADDED:
                if (viskit.ViskitStatics.debug) {
                    LOG.info("Grid JobLauncher got assembly change message: FILE_ADDED: " + " " + file.getAbsolutePath());
                }
                setFile(file.getAbsolutePath(), file.getName());
                break;
            case DirectoryWatch.DirectoryChangeListener.FILE_REMOVED:
                if (viskit.ViskitStatics.debug) {
                    LOG.info("Grid JobLauncher got assembly change message: FILE_REMOVED: " + " " + file.getAbsolutePath());
                }
                setFile(null, null);
                break;
            case DirectoryWatch.DirectoryChangeListener.FILE_CHANGED:
                if (viskit.ViskitStatics.debug) {
                    LOG.info("Grid JobLauncher got assembly change message: FILE_CHANGED: " + " " + file.getAbsolutePath());
                }
                setFile(file.getAbsolutePath(), file.getName());
                break;
            default:

        }
    }

    class ButtListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand().charAt(0)) {
                case 'r':
                    runButt.setEnabled(false);
                    canButt.setEnabled(true);
                    if (!isSubComponent) {
                        closeButt.setEnabled(false);
                    }
                    thread = new Thread(JobLauncher.this);
                    thread.setPriority(Thread.NORM_PRIORITY); // don't inherit swing event thread prior
                    thread.start();
                    break;
                case 'c':
                    stopRun();
                    break;
                case 'x':
                    runButt.setEnabled(true);  // for next time (probably not used)
                    canButt.setEnabled(false);
                    if (outputDirty) {
                        if (JOptionPane.showConfirmDialog(JobLauncher.this, "Save output?") == JOptionPane.YES_OPTION) {
                            JFileChooser jfc = new JFileChooser();
                            jfc.setSelectedFile(new File("DOEOutput.txt"));
                            jfc.showSaveDialog(JobLauncher.this);
                            if (jfc.getSelectedFile() != null) {
                                File f = jfc.getSelectedFile();
                                try {
                            try (FileWriter fw = new FileWriter(f)) {
                                fw.write(ta.getText());
                            }
                                } catch (IOException e1) {
                                    e1.printStackTrace(System.err);
                                }
                            }
                        }
                        outputDirty = false;
                    }

                    dispose();
                    break;
                default:
                    //assert false:"Program error JobLauncher.java";
                    LOG.error("Program error JobLauncher.java");
            }
        }
    }

    private void centerMe() {
        Rectangle meR = getBounds();
        if (mom != null) {
            Rectangle rectangle = mom.getBounds();
            meR.x = rectangle.x + rectangle.width / 2 - meR.width / 2;
            meR.y = rectangle.y + rectangle.height / 2 - meR.height / 2;
        }
        setBounds(meR);
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
        if (!isSubComponent) {
            closeButt.setEnabled(true);
        }
        runButt.setEnabled(true);

        if (thread == null) {
            return;
        }

        writeStatus("Stopping run.");
        hideClusterStatus();

        Thread jobKiller = new Thread(() -> {
            if (thread != null) {
                Thread t = thread;
                thread = null;
                t.interrupt();
                try {
                    t.join(1_000);
                } catch (InterruptedException e) {
                    LOG.info("join exception");
                }

            }
            try {
                Vector parms = new Vector();
                //o = rpc.execute("experiment.flushQueue",parms);
                Object o = rpc.execute("experiment.clear", parms);
                //writeStatus("flushQueue = " + o);
            } catch (XmlRpcException | IOException e) {
                e.printStackTrace(System.err);
            }
        }, "JobKiller");
        jobKiller.setPriority(Thread.NORM_PRIORITY);
        jobKiller.start();
    }

    private void writeStatus(final String s) {
        SwingUtilities.invokeLater(() -> {
            ta.append(s);
            ta.append("\n");
        });
    }
    StringWriter data;

    @Override
    public void run() {
        LOG.info("Hello Run");
    }

    public void runOrig() {
        outputDirty = true;
        outputList = new ArrayList<>();
        lp3:
        {
            try {
                createOutputDir();
                setParams(); // runs, sampls, tmo

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
                out.flush();
                out.close();

                Vector<String> parms = new Vector<>();
                parms.add(data.toString());

                writeStatus("Sending job file to " + clusterDNS);
                Object o = rpc.execute("experiment.setAssembly", parms);
                writeStatus("experiment.setAssembly returned " + o);

            } 
            catch (Exception e) {
                if (thread != null) {
                    // If normal error:
                    writeStatus("runOrig() exception connecting to server: " + e.getMessage());
                    LOG.info   ("runOrig() exception connecting to server: " + e.getMessage());
                }
                else LOG.error ("runOrig() exception connecting to server: " + e.getMessage());
                break lp3;
            }
            // Bring up the 2 other windows
            showClusterStatus(clusterWebStatus);
            chartter = new JobResults(JobLauncher.this, getTitle());

            //writeStatus("10 second wait before getting results.");
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                break lp3;
            }
            writeStatus("Getting results:");

            Vector<Integer> parms = new Vector<>();
            Object o;
            int i = 0;
            int n = designPts * samps * numRuns;
            lp:
            {
                for (int dp = 0; dp < designPts * samps; dp++) {
                    for (int nrun = 0; nrun < numRuns; nrun++, i++) {
                        try {
                            parms.clear();
                            parms.add(dp);
                            parms.add(nrun);
                            o = rpc.execute("experiment.getResult", parms);
                            if (thread == null) {
                                break lp;
                            }
                            kickOffClusterUpdate();
                            writeStatus("gotResult " + dp + "," + nrun + " (" + i + " of " + n + ")");
                            int idx = saveOutput((String) o, dp, nrun);
                            if (idx != -1) {
                                plotOutput(idx);
                            } else {
                                LOG.info("Output not saved");
                            }
                        } catch (XmlRpcException | IOException e) {
                            if (thread != null) {
                                writeStatus("Error from experiment.getResult(): " + e.getMessage());
                            }
                            break lp;
                        }
                    }
                }
            } // lp
        } // lp3
        stopRun();
    }
    java.util.List<Object[]> outputList;

    private void createOutputDir() throws Exception {
        outDir = TempFileManager.createTempFile("DoeRun", "");
        outDir = TempFileManager.createTempDir(outDir);
    }
    JobResults chartter;

    private void plotOutput(int idx) {
        if (chartter == null) {
            chartter = new JobResults(JobLauncher.this, getTitle());
        }
        synchronized (outputList) {
            Object[] oa = outputList.get(idx);
            Gresults res = getSingleResult(oa);
            chartter.addPoint(res);
            if (!res.resultsValid) {
                LOG.info("Results not retrieved for rep " + idx);
            }
        }
    }

    private Gresults getSingleResult(Object[] oa) {
        File f = new File((String) oa[2]);
        int dp = (int) oa[0];
        int nrun = (int) oa[1];
        Gresults res = new Gresults();

        Document document;
        try {
            document = FileHandler.unmarshallJdom(f);
        } catch (IOException | JDOMException e) {
            LOG.error("Error unmarshalling results: " + e.getMessage());
            return null;
        }
        Element el = document.getRootElement();
        if (!el.getName().equals("Results")) {
            LOG.info("Unknown results format, design point = " + dp + ", run = " + nrun);
            return res;
        }
        String design = attValue(el, "design");
        String index = attValue(el, "index");
        String job = attValue(el, "job");
        String run = attValue(el, "run");

        Element propCh = el.getChild("PropertyChange");
        if (propCh == null) {
            if (viskit.ViskitStatics.debug) {
                LOG.info("PropertyChange results element null, design point = " + dp + ", run = " + nrun);
            }
            return res;
        }
        String listenerName = attValue(propCh, "listenerName");
        String property = attValue(propCh, "property");
        java.util.List propertyContent = propCh.getContent();
        Text txt = (Text) propertyContent.get(0);
        String cstr = txt.getTextTrim();
        if (viskit.ViskitStatics.debug) {
            LOG.info("got back " + cstr);
        }
        String[] sa = cstr.split("\n");
        if (sa.length != 2) {
            LOG.info("PropertyChange parse error, design point = " + dp + ", run = " + nrun);
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
            LOG.error("JobLauncher.doResults");
        }

        res.dp = Integer.parseInt(design);
        //assert res.dp == dp : "JobLauncher.doResults1";
        if (res.dp != dp) {
            LOG.error("JobLauncher.doResults1");
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
            LOG.info("mischief detected!");
        }
        try {
            File f = File.createTempFile("DoeResults", ".xml", outDir);
            f.deleteOnExit();
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(o);
            }
            writeStatus("Result saved to " + f.getAbsolutePath());
            //outputs.put("" + dp + "," + nrun, f);
            int idx = outputList.size();
            outputList.add(new Object[]{dp, nrun, f.getAbsolutePath()});
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
            statusURL = new URI(surl).toURL();
            editorPane.setPage(statusURL);
        } catch (IOException | URISyntaxException e) {
            LOG.error("Error showing cluster status: " + e.getMessage());
            return;
        }

        clusterStatusFrame.pack();
        Rectangle frR = clusterStatusFrame.getBounds();

        Rectangle rectangle = this.getBounds();
        /*
        frR.x = r.x + r.width / 2 - frR.width / 2;
        frR.y = r.y + r.height / 2 - frR.height / 2;
         */
        frR.x = rectangle.x + rectangle.width;
        frR.y = rectangle.y; //chartter.getLocation().y + chartter.getSize().height;
        clusterStatusFrame.setBounds(frR);

        Runnable rn = () -> {
            clusterStatusFrame.setVisible(true);
        };
        SwingUtilities.invokeLater(rn);

        stopStatusThread(); // if running
        statusThread = new Thread(new statusUpdater());
        statusThread.start();

    }

    private void kickOffClusterUpdate() {
        if (waitToGo == true) {
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

        @Override
        public void run() {
            if (waitToGo == true) {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {}
            }

            while (statusThread != null && clusterStatusFrame != null) {
                try {
                    Thread.sleep(10_000);

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
                    SwingUtilities.invokeLater(() -> {
                        hbar.setValue(50);
                        vbar.setValue(50); //vbar.getMaximum());
                    });
                } catch (InterruptedException | IOException e) {
                    LOG.error("statusUpdater kill: " + e.getMessage());
                }
            }
        }
    }

    public static void main(final String[] args) {
        if (args.length != 1) {
            LOG.info("Give .xml file as argument");
        } else {

            Runnable r = () -> {
                new JobLauncher(false, args[0], args[0], null);
            };
            SwingUtilities.invokeLater(r);
        }
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

    public JMenuItem getQuitMenuItem() {
        JMenuBar mb = getJMenuBar();
        if (mb != null) {
            JMenu fileM = mb.getMenu(0);
            for (int i = 0; i < fileM.getMenuComponentCount(); i++) {
                JMenuItem m = fileM.getItem(i);
                if (m != null && m.getText().toLowerCase().startsWith("quit")) {
                    return m;
                }
            }
        }
        return null;
    }
}
