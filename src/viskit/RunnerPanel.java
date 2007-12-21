package viskit;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import java.awt.*;
import java.io.*;
import javax.swing.text.PlainDocument;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 13, 2004
 * @since 10:51:00 AM
 * @version $Id: RunnerPanel.java 1662 2007-12-16 19:44:04Z tdnorbra $ 
 * A VCR-controls and TextArea panel.  Hijacks System.out and System.err and displays them on its TextAreas.
 */
public class RunnerPanel extends JPanel {

    String lineEnd = System.getProperty("line.separator");
    public JTextArea soutTA,  serrTA;
    public JSplitPane splPn;
    public JButton vcrStop,  vcrPlay,  vcrRewind,  vcrStep,  closeButt;
    //public JButton saveParms;
    public JCheckBox vcrVerbose;
    public JTextField vcrSimTime,  vcrStopTime;
    private InputStream outInpStr,  errInpStr;
    public JCheckBox saveRepDataCB;
    public JCheckBox printRepReportsCB;
    public JCheckBox printSummReportsCB;
    public JTextField numRepsTF;
    public static int MAX_LINES = 2048;

    public RunnerPanel(boolean verbose, boolean skipCloseButt) {
        this(null, skipCloseButt);
        vcrVerbose.setSelected(verbose);
        piping = true;
        setupPipes();
    }

    public RunnerPanel(String title, boolean skipCloseButt) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); //BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        if (title != null) {
            JLabel titl = new JLabel(title);
            titl.setHorizontalAlignment(JLabel.CENTER);
            add(titl); //,BorderLayout.NORTH);
        }
        soutTA = new JTextArea();
        soutTA.setDocument(new PlainDocument());

        soutTA.setEditable(false); //false);
        soutTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        soutTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        soutTA.append("Assembly output stream:" + lineEnd +
                "----------------------" + lineEnd);
        JScrollPane jsp = new JScrollPane(soutTA);
        jsp.setPreferredSize(new Dimension(10, 350));   // give it some height for the initial split

        serrTA = new JTextArea("Assembly error stream:" + lineEnd +
                "---------------------" + lineEnd);
        serrTA.setForeground(Color.red);
        serrTA.setEditable(true); //false);
        serrTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        serrTA.setBackground(new Color(0xFB, 0xFB, 0xE5));
        JScrollPane jspErr = new JScrollPane(serrTA);

        splPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, jsp, jspErr);
        splPn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        add(splPn); //, BorderLayout.CENTER);
        //add(makeVCRPanel(skipCloseButt), BorderLayout.SOUTH);

        JComponent vcrPanel = makeVCRPanel(skipCloseButt);
        vcrPanel.setBorder(new EtchedBorder());
        Dimension d = vcrPanel.getPreferredSize();
        vcrPanel.setMaximumSize(new Dimension(d));
        vcrPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);


        JPanel buttAndSave = new JPanel();
        buttAndSave.setLayout(new BoxLayout(buttAndSave, BoxLayout.X_AXIS));
        vcrPanel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        buttAndSave.add(Box.createHorizontalGlue());
        buttAndSave.add(vcrPanel);
        //saveParms = new JButton("Save");
        //saveParms.setToolTipText("<html><center>Save execution parameters<br>to assembly file<br>"+
        //                        "(not required to run job)");
        //saveParms.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        //buttAndSave.add(Box.createHorizontalStrut(5));
        //buttAndSave.add(saveParms);
        buttAndSave.add(Box.createHorizontalGlue());
        add(Box.createVerticalStrut(5));
        add(buttAndSave);
    //add(vcrPanel); //,BorderLayout.SOUTH);
    }

    public void setStreams(InputStream out, InputStream err) {
        outInpStr = out;
        errInpStr = err;
        setupPipes();
    }

    JPanel makeVCRPanel(boolean skipCloseButt) {
        JPanel doubleVcrToolBar = new JPanel();
        doubleVcrToolBar.setLayout(new BoxLayout(doubleVcrToolBar, BoxLayout.Y_AXIS));

        JPanel vcrToolBar = new JPanel();
        vcrToolBar.setLayout(new BoxLayout(vcrToolBar, BoxLayout.X_AXIS));
        vcrToolBar.add(Box.createHorizontalGlue());

        vcrStop = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Stop24.gif")));
        vcrStop.setToolTipText("Stop the simulation run");
        vcrStop.setEnabled(false);
        vcrStop.setBorder(BorderFactory.createEtchedBorder());
        vcrStop.setText(null);
        vcrToolBar.add(vcrStop);

        vcrRewind = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Rewind24.gif")));
        vcrRewind.setToolTipText("Reset the simulation run");
        vcrRewind.setEnabled(false);
        vcrRewind.setBorder(BorderFactory.createEtchedBorder());
        vcrRewind.setText(null);
        if (!skipCloseButt) {
            vcrToolBar.add(vcrRewind);
        }

        vcrPlay = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/Play24.gif")));
        vcrPlay.setToolTipText("Begin or resume the simulation run");
        if (skipCloseButt) {
            vcrPlay.setToolTipText("Begin the simulation run");
        }
        vcrPlay.setBorder(BorderFactory.createEtchedBorder());
        vcrPlay.setText(null);
        vcrToolBar.add(vcrPlay);

        vcrStep = new JButton(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("viskit/images/StepForward24.gif")));
        vcrStep.setToolTipText("Step the simulation");
        vcrStep.setBorder(BorderFactory.createEtchedBorder());
        vcrStep.setText(null);
        if (!skipCloseButt) {
            vcrToolBar.add(vcrStep);
        }

        vcrToolBar.add(Box.createHorizontalStrut(20));

        JLabel vcrSimTimeLab = new JLabel("Sim start time:");
        // TODO:  is this start time or current time of sim?
        // TODO:  is this used elsewhere, or else can it simply be removed?
        vcrSimTime = new JTextField(10);
        vcrSimTime.setEditable(false);
        Vstatics.clampSize(vcrSimTime, vcrSimTime, vcrSimTime);

        vcrToolBar.add(vcrSimTimeLab);
        vcrToolBar.add(vcrSimTime);
        vcrToolBar.add(Box.createHorizontalStrut(10));

        JLabel vcrStopTimeLabel = new JLabel("Sim stop time:");
        vcrStopTimeLabel.setToolTipText("Stop current replication once simulation stop time reached");
        vcrStopTime = new JTextField(10);
        Vstatics.clampSize(vcrStopTime, vcrStopTime, vcrStopTime);

        vcrToolBar.add(vcrStopTimeLabel);
        vcrToolBar.add(vcrStopTime);
        vcrToolBar.add(Box.createHorizontalStrut(10));

        vcrVerbose = new JCheckBox("Verbose output", false);
        vcrVerbose.setToolTipText("Enable or disable verbose simulation output");
        vcrToolBar.add(vcrVerbose);
        vcrToolBar.add(Box.createHorizontalStrut(5));

        closeButt = new JButton("Close");
        closeButt.setToolTipText("Close this window");
        if (!skipCloseButt) {
            vcrToolBar.add(closeButt);
        }
        vcrToolBar.add(Box.createHorizontalGlue());
        vcrToolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel lowerToolBar = new JPanel();
        lowerToolBar.setLayout(new BoxLayout(lowerToolBar, BoxLayout.X_AXIS));

        numRepsTF = new JTextField(8);
        Vstatics.clampSize(numRepsTF, numRepsTF, numRepsTF);
        JLabel numRepsLab = new JLabel("Number of replications");
        saveRepDataCB = new JCheckBox("Save replication data");
        printRepReportsCB = new JCheckBox("Print replication reports");
        printSummReportsCB = new JCheckBox("Print summary reports");
        lowerToolBar.add(Box.createHorizontalGlue());
        lowerToolBar.add(numRepsLab);
        lowerToolBar.add(numRepsTF);
        lowerToolBar.add(Box.createHorizontalStrut(10));
        lowerToolBar.add(saveRepDataCB);
        lowerToolBar.add(Box.createHorizontalStrut(10));
        lowerToolBar.add(printRepReportsCB);
        lowerToolBar.add(Box.createHorizontalStrut(10));
        lowerToolBar.add(printSummReportsCB);
        lowerToolBar.add(Box.createHorizontalGlue());
        doubleVcrToolBar.add(vcrToolBar);
        doubleVcrToolBar.add(lowerToolBar);
        //return vcrToolBar;
        return doubleVcrToolBar;
    }
    private boolean piping = false;

    private void setupPipes() {
        if ((outInpStr != null) && (errInpStr != null)) {
            new ReaderThread(outInpStr, soutTA, true).start();
            new ReaderThread(errInpStr, serrTA, false).start();
        } else {
            try {
                PipedInputStream piOut = new PipedInputStream();
                PipedOutputStream poOut = new PipedOutputStream(piOut);
                System.setOut(new PrintStream(poOut, true));
                PipedInputStream piErr = new PipedInputStream();
                PipedOutputStream poErr = new PipedOutputStream(piErr);
                System.setErr(new PrintStream(poErr, true));

                new ReaderThread(piOut, soutTA, true).start();
                new ReaderThread(piErr, serrTA, false).start();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "exep in setupPipes " + e.getMessage());
            }
        }
    }

    class ReaderThread extends Thread {

        InputStream pi;
        JTextArea myTa;
        boolean clampSize;

        ReaderThread(InputStream pi, JTextArea ta, boolean clampSize) {
            super("rdrThr");
            this.pi = pi;
            this.myTa = ta;
            this.clampSize = clampSize;
        }

        @Override
        public void run() {
            final byte[] buf = new byte[256];
            PipedInputStream pis = new PipedInputStream();
            PipedOutputStream pos = null;
            try {
                pos = new PipedOutputStream(pis);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] ba = new byte[1];

            while (true) {
                int len = -1;
                try {
                    len = pi.read(buf);
                } catch (IOException e) {
                    // this happens when the thread that was the writer is dead.  Seems screwy when you
                    // compare it to System.out, which doesn't care if anybody lives or dies.  The thread in
                    // ExternalAssemblyRunner dies each time we exit Schedule, whether stepping or otherwise.
                    // It's fine to just set up the I/O again and wait for the next time.

                    if (piping) {
                        setupPipes();
                    }
                    return;
                }
                if (len == -1) {
                    if (piping) {
                        setupPipes();
                    }
                    return;
                }

                // Write to the swing widget
                new inSwingThread(myTa, new String(buf, 0, len), pos);

            }
        }
        StringBuffer stsb = new StringBuffer(); // we're single threaded

        /**
         * Class to encapsulate a packet containing a JTextArea reference and a string, and append the
         * string to the TextArea in the GUI thread.
         */
        class inSwingThread implements Runnable {

            JTextArea ta;
            String s;
            Document doc;
            PipedOutputStream pos;

            inSwingThread(JTextArea ta, String s, PipedOutputStream pos) {
                this.ta = ta;
                this.s = s;
                this.pos = pos;
                doc = ta.getDocument();
                SwingUtilities.invokeLater(this);
            }

            public void run() {

                ta.append(s);
                // tbd, these have to be backed by file storage
                // somehow. save every MAX_LINES
                // set up a scroll listener, if scroll tab
                // goes to 0, load previous lines in text-area
                // if goes to max-scroll, load next availble
                // block of lines.
                if (ta.getLineCount() > MAX_LINES) {
                    ta.setText("");
                }

                ta.setCaretPosition(doc.getLength());

            }
        }
    }
}
