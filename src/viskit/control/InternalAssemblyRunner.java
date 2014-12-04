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
package viskit.control;

import viskit.view.RunnerPanel2;
import viskit.view.dialog.SettingsDialog;
import viskit.view.AnalystReportPanel;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.swing.*;
import org.apache.log4j.Logger;
import simkit.Schedule;
import simkit.random.RandomVariateFactory;
import viskit.util.TitleListener;
import viskit.VGlobals;
import viskit.ViskitProject;
import viskit.Vstatics;
import viskit.assembly.BasicAssembly;
import viskit.assembly.JTextAreaOutputStream;
import viskit.doe.LocalBootLoader;
import viskit.model.AssemblyModelImpl;

/** Controller for the Assembly Run panel
 *
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @author Rick Goldberg
 * @since Sep 26, 2005
 * @since 3:43:51 PM
 * @version $Id$
 */
public class InternalAssemblyRunner implements PropertyChangeListener {

    static String lineSep = System.getProperty("line.separator");
    static Logger log = LogUtils.getLogger(InternalAssemblyRunner.class);
    String targetClassName;
    RunnerPanel2 runPanel;
    ActionListener closer, saver;
    JMenuBar myMenuBar;
    BufferedReader backChan;
    Thread simRunner;
    SimThreadMonitor swingThreadMonitor;
    PipedOutputStream pos;
    PipedInputStream pis;
    BasicAssembly assembly;

    /** external runner saves a file */
    private String analystReportTempFile = null;
    FileOutputStream fos;
    FileInputStream fis;
    Class<?> targetClass;
    Object assemblyObj, targetObject;
    private static int mutex = 0;
    private LocalBootLoader loader;
    private ClassLoader lastLoaderNoReset;
    private ClassLoader lastLoaderWithReset;
    long seed;
    private boolean inRegressionMode;
    AnalystReportPanel reportPanel;

    public InternalAssemblyRunner() {
        inRegressionMode = false;

        saver = new saveListener();

        // NOTE:
        // Don't supply rewind or pause buttons on VCR, not hooked up, or working right
        // false will enable all VCR buttons.  Currently, only start and stop work
        runPanel = new RunnerPanel2(null, true);
        doMenus();
        runPanel.vcrStop.addActionListener(new stopListener());
        runPanel.vcrPlay.addActionListener(new startResumeListener());
        runPanel.vcrRewind.addActionListener(new rewindListener());
        runPanel.vcrStep.addActionListener(new stepListener());
        runPanel.vcrVerbose.addActionListener(new verboseListener());
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        runPanel.vcrStep.setEnabled(false);
        seed = RandomVariateFactory.getDefaultRandomNumber().getSeed();
        twiddleButtons(OFF);
    }

    /**
     * Are we running in regression mode.
     *
     * @param regress Should we run in regression mode
     */
    public void setRegressionMode(boolean regress) {
        inRegressionMode = regress;

        if (inRegressionMode) {
            runPanel.analystReportCB.setSelected(true);
        }
    }

    public JComponent getRunnerPanel() {return runPanel;}

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return null;
    }

    public void setAnalystReportGUI(AnalystReportPanel pan) {
        reportPanel = pan;
    }

    /**
     * Pre-initialization this runner for a sim run
     * @param params arguments to initialize the Assembly runner
     */
    public void preInitRun(String[] params) {

//        for (String s : params) {
//            log.info("VM argument is: " + s);
//        }

        targetClassName = params[AssemblyControllerImpl.EXEC_TARGET_CLASS_NAME];
        doTitle(targetClassName);

        // TODO: should this be editable?
        runPanel.vcrSimTime.setText("0.0");

        boolean defaultVerbose = Boolean.parseBoolean(params[AssemblyControllerImpl.EXEC_VERBOSE_SWITCH]);
        double defaultStopTime = Double.parseDouble(params[AssemblyControllerImpl.EXEC_STOPTIME_SWITCH]);


        lastLoaderNoReset = VGlobals.instance().getWorkClassLoader();
//        Thread.currentThread().setContextClassLoader(lastLoaderNoReset);

        Class<?> obj;

        try {

            // Initialize an Object so that we have it's ClassLoader as the
            // parent of our about to be reset LocalBootLoader
            obj = lastLoaderNoReset.loadClass("java.lang.Object");
        } catch (ClassNotFoundException e) {
            LogUtils.getLogger(InternalAssemblyRunner.class).error(e);
            return;
        }

        // Forcing exposure of extra classpaths here.  Bugfix 1237
        loader = new LocalBootLoader(SettingsDialog.getExtraClassPathArraytoURLArray(), obj.getClassLoader(), VGlobals.instance().getWorkDirectory());
        lastLoaderWithReset = loader.init(true);
        Thread.currentThread().setContextClassLoader(lastLoaderWithReset);

            // Test for Bug 1237
//            for (String s : loader.getClassPath()) {
//                log.info(s);
//            }

        try {
            fillRepWidgetsFromBasicAssemblyObject(defaultVerbose, defaultStopTime);
        } catch (Throwable throwable) {
            JOptionPane.showMessageDialog(runPanel, "Error initializing Assembly object:\n" + throwable.getMessage(), "Java Error", JOptionPane.ERROR_MESSAGE);
            twiddleButtons(OFF);
//            throwable.printStackTrace();
            return;
        }
        twiddleButtons(InternalAssemblyRunner.REWIND);
    }

    private void fillRepWidgetsFromBasicAssemblyObject(boolean verbose, double stopTime) throws Throwable {

        targetClass = Vstatics.classForName(targetClassName);
        if (targetClass == null) {
            throw new ClassNotFoundException();
        }
        targetObject = targetClass.newInstance();

        /* in order to see BasicAssembly this thread has to have
         * the same ClassLoader as the one used to compile this entity since
         * they don't share the same simkit or viskit.  Used in the
         * verboseListener
         */
        assembly = (BasicAssembly) targetObject;

        Method getNumberReplications = targetClass.getMethod("getNumberReplications");
        Method isSaveReplicationData = targetClass.getMethod("isSaveReplicationData");
        Method isPrintReplicationReports = targetClass.getMethod("isPrintReplicationReports");
        Method isPrintSummaryReport = targetClass.getMethod("isPrintSummaryReport");
        Method setVerbose = targetClass.getMethod("setVerbose", boolean.class);
        Method isVerbose = targetClass.getMethod("isVerbose");
        Method setStopTime = targetClass.getMethod("setStopTime", double.class);
        Method getStopTime = targetClass.getMethod("getStopTime");

        runPanel.numRepsTF.setText("" + (Integer) getNumberReplications.invoke(targetObject));
        runPanel.saveRepDataCB.setSelected((Boolean) isSaveReplicationData.invoke(targetObject));
        runPanel.printRepReportsCB.setSelected((Boolean) isPrintReplicationReports.invoke(targetObject));
        runPanel.printSummReportsCB.setSelected((Boolean) isPrintSummaryReport.invoke(targetObject));
        setVerbose.invoke(targetObject, verbose);
        runPanel.vcrVerbose.setSelected((Boolean) isVerbose.invoke(targetObject));
        setStopTime.invoke(targetObject, stopTime);
        runPanel.vcrStopTime.setText("" + (Double) getStopTime.invoke(targetObject));

        Schedule.coldReset();
    }

    File tmpFile;
    RandomAccessFile rTmpFile;
    boolean resetSeeds = false;
    JTextAreaOutputStream textAreaOutputStream;

    protected void initRun() {

        // Prevent multiple pushes of the sim run button
        mutex++;
        if (mutex > 1) {
            return;
        }
        Runnable assemblyRunnable;

        try {

            targetClass = lastLoaderWithReset.loadClass(targetClass.getName());
            assemblyObj = targetClass.newInstance();

            Method setOutputStream = targetClass.getMethod("setOutputStream", OutputStream.class);
            Method setNumberReplications = targetClass.getMethod("setNumberReplications", int.class);
            Method setSaveReplicationData = targetClass.getMethod("setSaveReplicationData", boolean.class);
            Method setPrintReplicationReports = targetClass.getMethod("setPrintReplicationReports", boolean.class);
            Method setPrintSummaryReport = targetClass.getMethod("setPrintSummaryReport", boolean.class);
            Method setEnableAnalystReports = targetClass.getMethod("setEnableAnalystReports", boolean.class);
            Method setVerbose = targetClass.getMethod("setVerbose", boolean.class);
            Method setStopTime = targetClass.getMethod("setStopTime", double.class);
            Method setVerboseReplication = targetClass.getMethod("setVerboseReplication", int.class);
            Method setPclNodeCache = targetClass.getMethod("setPclNodeCache", Map.class);
            Method addPropertyChangeListener = targetClass.getMethod("addPropertyChangeListener", PropertyChangeListener.class);

            // Resetting the seed
            Class<?> RVFactClass = lastLoaderWithReset.loadClass("simkit.random.RandomVariateFactory");
            Method getDefaultRandomNumber = RVFactClass.getMethod("getDefaultRandomNumber");
            Object rn = getDefaultRandomNumber.invoke(null);

            Class<?> RNClass = lastLoaderWithReset.loadClass("simkit.random.RandomNumber");
            Method setSeed = RNClass.getMethod("setSeed", long.class);
            setSeed.invoke(rn, seed);

            textAreaOutputStream = new JTextAreaOutputStream(runPanel.soutTA,16*1024);

            setOutputStream.invoke(assemblyObj, textAreaOutputStream);
            setNumberReplications.invoke(assemblyObj, Integer.parseInt(runPanel.numRepsTF.getText().trim()));
            setSaveReplicationData.invoke(assemblyObj, runPanel.saveRepDataCB.isSelected());
            setPrintReplicationReports.invoke(assemblyObj, runPanel.printRepReportsCB.isSelected());
            setPrintSummaryReport.invoke(assemblyObj, runPanel.printSummReportsCB.isSelected());

            /* DIFF between OA3302 branch and trunk */
            setEnableAnalystReports.invoke(assemblyObj, runPanel.analystReportCB.isSelected());
            /* End DIFF between OA3302 branch and trunk */

            setStopTime.invoke(assemblyObj, getStopTime());
            setVerbose.invoke(assemblyObj, runPanel.vcrVerbose.isSelected());
            setVerboseReplication.invoke(assemblyObj, getVerboseReplicationNumber());
            setPclNodeCache.invoke(assemblyObj, ((AssemblyModelImpl)VGlobals.instance().getActiveAssemblyModel()).getNodeCache());
            addPropertyChangeListener.invoke(assemblyObj, this);
            assemblyRunnable = (Runnable) assemblyObj;

            // Start the simulation run(s)
            simRunner = new Thread(assemblyRunnable);
            new SimThreadMonitor(simRunner).start();

            // Restore Viskit's working ClassLoader
            Thread.currentThread().setContextClassLoader(lastLoaderNoReset);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | ClassNotFoundException ex) {
            log.error(ex);
        }
    }

    /** Class to perform end of simulation run cleanup items */
    public class SimThreadMonitor extends Thread {

        Thread waitOn;

        public SimThreadMonitor(Thread toWaitOn) {
            waitOn = toWaitOn;
        }

        @Override
        public void run() {
            waitOn.start();
            try {
                waitOn.join();
            } catch (InterruptedException ex) {
                log.error(ex);
//                ex.printStackTrace();
            }

            end();

            // Grab the temp analyst report and signal the AnalystReportPanel
            try {
                Method getAnalystReport = targetClass.getMethod("getAnalystReport");
                analystReportTempFile = (String) getAnalystReport.invoke(assemblyObj);
            } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                log.fatal(ex);
            }
            signalReportReady();
        }

        /** Perform simulation stop and reset calls */
        public void end() {
            System.out.println("Simulation ended");
            System.out.println("----------------");
            runPanel.npsLabel.setText("<html><body><p><b>Replications complete\n</b></p></body></html>");
            new stopListener().actionPerformed(null);
        }
    }

    /**
     * get the value of the RunnerPanel2 text field. This number
     * starts counting at 0, the method will return -1 for blank
     * or non-integer value.
     * @return the replication instance to output verbose on
     */
    public int getVerboseReplicationNumber() {
        int ret = -1;
        try {
            ret = Integer.parseInt(runPanel.verboseRepNumberTF.getText().trim());
        } catch (NumberFormatException ex) {
          //  ;
        }
        return ret;
    }
    PrintWriter pWriter;

    class startResumeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            runPanel.vcrSimTime.setText("0.0");    // because no pausing
            twiddleButtons(InternalAssemblyRunner.START);
            initRun();
        }
    }

    class stepListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            twiddleButtons(InternalAssemblyRunner.STEP);
        }
    }

    class stopListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Method setStopRun;
            try {
                Thread.currentThread().setContextClassLoader(lastLoaderWithReset);
                setStopRun = targetClass.getMethod("setStopRun", boolean.class);
                setStopRun.invoke(assemblyObj, true);
                Schedule.coldReset();
                Thread.currentThread().setContextClassLoader(lastLoaderNoReset);
            } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                log.error(ex);
            }

            twiddleButtons(InternalAssemblyRunner.STOP);
            textAreaOutputStream.kill();
            mutex--;
        }
    }

    class rewindListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            twiddleButtons(InternalAssemblyRunner.REWIND);
        }
    }

    /**
     * TODO: not sure this is required.  setVerbose gets set in initRun()
     */
    class verboseListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (assembly == null) {return;}
            assembly.setVerbose(((JCheckBox) e.getSource()).isSelected());
        }
    }

    private JFileChooser saveChooser;

    class saveListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (saveChooser == null) {
                saveChooser = new JFileChooser(ViskitProject.MY_VISKIT_PROJECTS_DIR);
            }
            File fil = getUniqueName("AssemblyOutput.txt", saveChooser.getCurrentDirectory());
            saveChooser.setSelectedFile(fil);

            int retv = saveChooser.showSaveDialog(null);
            if (retv != JFileChooser.APPROVE_OPTION) {
                return;
            }

            fil = saveChooser.getSelectedFile();
            if (fil.exists()) {
                int r = JOptionPane.showConfirmDialog(null, "File exists.  Overwrite?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(fil))) {
                    bw.write(runPanel.soutTA.getText());
                }
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage(), "I/O Error,", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    // dup of EventGraphViewFrame
    private File getUniqueName(String suggName, File parent) {
        String appnd = "";
        String suffix = "";

        int lastDot = suggName.lastIndexOf('.');
        if (lastDot != -1) {
            suffix = suggName.substring(lastDot);
            suggName = suggName.substring(0, lastDot);
        }
        int count = -1;
        File fil = null;
        do {
            fil = new File(parent, suggName + appnd + suffix);
            appnd = "" + ++count;
        } while (fil.exists());

        return fil;
    }
    String returnedSimTime;

    private void signalReportReady() {
        if (analystReportTempFile == null) {
            // No report to print
            return;
        }

        if (reportPanel != null) {
            reportPanel.setReportXML(analystReportTempFile);
        } else {
            JOptionPane.showMessageDialog(null, "<html><body><p align='center'>" +
                    "The Analyst Report tab has not been set to be visible.<br>To " +
                    "view on next Viskit opening, select File -> Settings -> " +
                    "Tab visibility -> Select Analyst report -> Close, then Exit" +
                    " the application.  On re-startup, it will appear.</p></body></html>",
                    "Analyst Report Panel not visible", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    double getStopTime() {
        return Double.parseDouble(runPanel.vcrStopTime.getText());
    }

    boolean getVerbose() {
        return runPanel.vcrVerbose.isSelected();
    }
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int STEP = 2;
    public static final int REWIND = 3;
    public static final int OFF = 4;

    private void twiddleButtons(int evnt) {
        switch (evnt) {
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

    private void doMenus() {
        myMenuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save output streams");
        JMenu edit = new JMenu("Edit");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem selAll = new JMenuItem("Select all");
        JMenuItem clrAll = new JMenuItem("Clear all");
        JMenuItem view = new JMenuItem("View output in text editor");

        save.addActionListener(saver);
        copy.addActionListener(new copyListener());
        selAll.addActionListener(new selectAllListener());
        clrAll.addActionListener(new clearListener());
        view.addActionListener(new viewListener());

        file.add(save);
        file.add(view);

        file.addSeparator();
        file.add(new JMenuItem("Settings"));

        edit.add(copy);
        edit.add(selAll);
        edit.add(clrAll);
        myMenuBar.add(file);
        myMenuBar.add(edit);
    }

    class copyListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String s = runPanel.soutTA.getSelectedText();
            StringSelection ss = new StringSelection(s);
            Clipboard clpbd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbd.setContents(ss, ss);
        }
    }

    class selectAllListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            runPanel.soutTA.requestFocus();
            runPanel.soutTA.selectAll();
        }
    }

    class clearListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            runPanel.soutTA.setText(null);
        }
    }

    class viewListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            File f; // = tmpFile;
            String osName = System.getProperty("os.name");
            String filePath = "";
            String tool;
            if (osName.toLowerCase().contains("win")) {
                tool = "notepad";
            } else if (osName.toLowerCase().contains("mac")) {
                tool = "open -a";
            } else {
                tool = "gedit"; // assuming Linux here
            }

            String s = runPanel.soutTA.getText().trim();
            try {
                f = TempFileManager.createTempFile("ViskitOutput", ".txt");
                f.deleteOnExit();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                    bw.append(s);
                }
                filePath = f.getCanonicalPath();
                java.awt.Desktop.getDesktop().open(new File(filePath));
              }
            catch (IOException ex) {
            }
            catch (UnsupportedOperationException ex) {
              try {
                  Runtime.getRuntime().exec(tool + " " + filePath);
              }
              catch (IOException ex1) {
                  log.error(ex1);
//                  ex1.printStackTrace();
              }
            }
        }
    }

    private String namePrefix = "Viskit Assembly Runner";
    private String currentTitle = namePrefix;

    private void doTitle(String nm) {
        if (nm != null && nm.length() > 0) {
            currentTitle = namePrefix + ": " + nm;
        }

        if (titlList != null) {
            titlList.setTitle(currentTitle, titlkey);
        }
    }
    private TitleListener titlList;
    private int titlkey;

    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlkey = key;
        doTitle(null);
    }

    StringBuilder npsString = new StringBuilder("<html><body><font color=black>\n" + "<p><b>Now Running Replication ");

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        log.debug(evt.getPropertyName());

        if (evt.getPropertyName().equals("replicationNumber")) {
            int beginLength = npsString.length();
            npsString.append(evt.getNewValue());
            npsString.append(" of ");
            npsString.append(Integer.parseInt(runPanel.numRepsTF.getText()));
            npsString.append("</b>\n");
            npsString.append("</font></p></body></html>\n");
            runPanel.npsLabel.setText(npsString.toString());

            // reset for the next replication output
            npsString.delete(beginLength, npsString.length());
        }
    }

}  // end class file InternalAssemblyRunner.java
