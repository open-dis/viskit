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
package viskit.control;

import viskit.view.AssemblySimulationRunPanel;

import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.awt.Desktop;

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

import org.apache.logging.log4j.Logger;

import simkit.Schedule;

import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.assembly.BasicAssembly;
import viskit.assembly.JTextAreaOutputStream;
import viskit.model.AnalystReportModel;
import viskit.model.AssemblyModelImpl;
import viskit.util.OpenAssembly;

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
public class InternalAssemblySimulationRunner implements PropertyChangeListener {

    static final Logger LOG = LogUtils.getLogger(InternalAssemblySimulationRunner.class);

    /** The name of the basicAssembly to run */
    String assemblyClassName;
    AssemblySimulationRunPanel assemblySimulationRunPanel;
    ActionListener saveListener;
    JMenuBar myMenuBar;
    Thread simulationRunnerThread;
    BasicAssembly basicAssembly;

    /** external runner saves a file */
    private String analystReportTempFile = null;

    /** The basicAssembly to be run from java source */
    Class<?> assemblyClass;

    /** Instance of the basicAssembly to run from java source */
    Object assemblyInstance;
    private static int mutex = 0;
    private final ClassLoader lastLoaderNoReset;
    private ClassLoader lastLoaderWithReset;

    /** Captures the original RNG seed state */
    private long[] seeds;
    private final StopListener assemblySimulationRunStopListener;

    public static final String ASSEMBLY_SIMULATION_RUN_PANEL_TITLE = "Simulation Run Console";
    /**
     * The internal logic for the Assembly Runner panel
     * @param analystReportPanelVisible if true, the analyst report panel will be visible
     */
    public InternalAssemblySimulationRunner(boolean analystReportPanelVisible) 
    {
        saveListener = new SaveListener();

        // NOTE:
        // Don't supply rewind or pause buttons on VCR, not hooked up, or working right.
        // false will enable all VCR buttons. Currently, only start and stop work

        assemblySimulationRunPanel = new AssemblySimulationRunPanel(ASSEMBLY_SIMULATION_RUN_PANEL_TITLE, false, analystReportPanelVisible);
        doMenus();
        assemblySimulationRunPanel.vcrStopButton.addActionListener(assemblySimulationRunStopListener = new StopListener());
        assemblySimulationRunPanel.vcrPlayButton.addActionListener(new StartResumeListener());
        assemblySimulationRunPanel.vcrRewindButton.addActionListener(new RewindListener());
        assemblySimulationRunPanel.vcrStepButton.addActionListener(new StepListener());
        assemblySimulationRunPanel.vcrVerboseCB.addActionListener(new VerboseListener());
        assemblySimulationRunPanel.vcrStopButton.setEnabled(false);
        assemblySimulationRunPanel.vcrPlayButton.setEnabled(false);
        assemblySimulationRunPanel.vcrRewindButton.setEnabled(false);
        assemblySimulationRunPanel.vcrStepButton.setEnabled(false);

        // Viskit's current working ClassLoader
        lastLoaderNoReset = ViskitGlobals.instance().getWorkClassLoader();

        // Provide access to Enable Analyst Report checkbox
        ViskitGlobals.instance().setAssemblySimulationRunPanel(assemblySimulationRunPanel);
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return null;
    }

    /**
     * Pre-initialize this runner for a sim run
     * @param params arguments to initialize the Assembly runner
     */
    public void preInitRun(String[] params) {

//        for (String s : params) {
//            LOG.info("VM argument is: {}", s);
//        }

        assemblyClassName = params[AssemblyControllerImpl.EXEC_TARGET_CLASS_NAME];
        doTitle(assemblyClassName);

        assemblySimulationRunPanel.vcrStartTimeTF.setText("0.0");

        // These values are from the XML file
        boolean defaultVerbose = Boolean.parseBoolean(params[AssemblyControllerImpl.EXEC_VERBOSE_SWITCH]);
        boolean saveRepDataToXml = assemblySimulationRunPanel.analystReportCB.isSelected();
        double defaultStopTime = Double.parseDouble(params[AssemblyControllerImpl.EXEC_STOPTIME_SWITCH]);

        try {
            fillSimulationRunButtonsFromAssemblyInitialization(defaultVerbose, saveRepDataToXml, defaultStopTime);
        } 
        catch (Throwable throwable) 
        {
            ViskitGlobals.instance().getAssemblyEditor().genericReport(
                    JOptionPane.ERROR_MESSAGE,
                    "Java Error",
                    "Error initializing Assembly:\n" + throwable.getMessage()
            );
            vcrButtonPressDisplayUpdate(Event.OFF);
//            throwable.printStackTrace();
            return;
        }
        vcrButtonPressDisplayUpdate(Event.REWIND);
    }

    private void fillSimulationRunButtonsFromAssemblyInitialization(boolean verbose, boolean saveReplicationDataToXml, double stopTime) throws Throwable {

        assemblyClass = ViskitStatics.classForName(assemblyClassName);
        if (assemblyClass == null) {
            throw new ClassNotFoundException();
        }
        assemblyInstance = assemblyClass.getDeclaredConstructor().newInstance();

        /* in order to resolve the basicAssembly as a BasicAssembly, it must be
         * loaded using the the same ClassLoader as the one used to compile
         * it. Used in the verboseListener within the working Viskit
         * ClassLoader
         */
        basicAssembly = (BasicAssembly) assemblyInstance;

        Method getNumberReplicationsMethod = assemblyClass.getMethod("getNumberReplications");
        Method isSaveReplicationData = assemblyClass.getMethod("isSaveReplicationData"); // TODO hook this up
        Method isPrintReplicationReportsMethod = assemblyClass.getMethod("isPrintReplicationReports");
        Method isPrintSummaryReportMethod = assemblyClass.getMethod("isPrintSummaryReport");
        Method setVerboseMethod = assemblyClass.getMethod("setVerbose", boolean.class);
        Method isVerboseMethod = assemblyClass.getMethod("isVerbose");
        Method setStopTimeMethod = assemblyClass.getMethod("setStopTime", double.class);
        Method getStopTimeMethod = assemblyClass.getMethod("getStopTime");

        assemblySimulationRunPanel.numberReplicationsTF.setText("" + getNumberReplicationsMethod.invoke(assemblyInstance));
        assemblySimulationRunPanel.printReplicationReportsCB.setSelected((Boolean) isPrintReplicationReportsMethod.invoke(assemblyInstance));
        assemblySimulationRunPanel.printSummaryReportsCB.setSelected((Boolean) isPrintSummaryReportMethod.invoke(assemblyInstance));
        assemblySimulationRunPanel.saveReplicationDataCB.setSelected(saveReplicationDataToXml);

        // Set the run panel verboseness according to what the basicAssembly XML value is
        setVerboseMethod.invoke(assemblyInstance, verbose);
        assemblySimulationRunPanel.vcrVerboseCB.setSelected((Boolean) isVerboseMethod.invoke(assemblyInstance));
        setStopTimeMethod.invoke(assemblyInstance, stopTime);
        assemblySimulationRunPanel.vcrStopTimeTF.setText("" + getStopTimeMethod.invoke(assemblyInstance));
    }
    JTextAreaOutputStream textAreaOutputStream;
    Runnable assemblyRunnable;

    protected void initializeAssemblySimulationRun() {

        // Prevent multiple pushes of the sim run button
        mutex++;
        if (mutex > 1)
            return;

        try // initializeAssemblySimulationRun
        {
            ViskitGlobals.instance().resetFreshClassLoader();
            lastLoaderWithReset = ViskitGlobals.instance().getFreshClassLoader();

            // Test for Bug 1237
//            for (String s : ((LocalBootLoader)lastLoaderWithReset).getClassPath()) {
//                LOG.info(s);
//            }
//            LOG.info("\n");

            // Now we are in the pure classloader realm where each basicAssembly run can be independent of any other
            assemblyClass = lastLoaderWithReset.loadClass(assemblyClass.getName());
            assemblyInstance = assemblyClass.getDeclaredConstructor().newInstance();

            Method setOutputStreamMethod = assemblyClass.getMethod("setOutputStream", OutputStream.class);
            Method setNumberReplicationsMethod = assemblyClass.getMethod("setNumberReplications", int.class);
            Method setPrintReplicationReportsMethod = assemblyClass.getMethod("setPrintReplicationReports", boolean.class);
            Method setPrintSummaryReportMethod = assemblyClass.getMethod("setPrintSummaryReport", boolean.class);
            Method setSaveReplicationDataMethod = assemblyClass.getMethod("setSaveReplicationData", boolean.class);
            Method setEnableAnalystReports = assemblyClass.getMethod("setEnableAnalystReports", boolean.class);
            Method setVerboseMethod = assemblyClass.getMethod("setVerbose", boolean.class);
            Method setStopTimeMethod = assemblyClass.getMethod("setStopTime", double.class);
            Method setVerboseReplicationMethod = assemblyClass.getMethod("setVerboseReplication", int.class);
            Method setPclNodeCacheMethod = assemblyClass.getMethod("setPclNodeCache", Map.class);
            Method addPropertyChangeListenerMethod = assemblyClass.getMethod("addPropertyChangeListener", PropertyChangeListener.class);

            // As of discussion held 09 APR 2015, resetting the RNG seed state
            // is not necessary for basic Viskit operation.  Pseudo random
            // independence is guaranteed from the default RNG (normally the
            // MersenneTwister)

            // *** Resetting the RNG seed state ***
            // NOTE: This is currently disabled as the resetSeedStateCB is not
            // enabled nor visible
            if (assemblySimulationRunPanel.resetSeedStateCB.isSelected()) 
            {
                Class<?> rVFactClass = lastLoaderWithReset.loadClass(ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS);
                Method getDefaultRandomNumberMethod = rVFactClass.getMethod("getDefaultRandomNumber");
                Object rn = getDefaultRandomNumberMethod.invoke(null);

                Method getSeedsMethod = rn.getClass().getMethod("getSeeds");
                seeds = (long[]) getSeedsMethod.invoke(rn);

                Class<?> rNClass = lastLoaderWithReset.loadClass(ViskitStatics.RANDOM_NUMBER_CLASS);
                Method setSeedsMethod = rNClass.getMethod("setSeeds", long[].class);
                setSeedsMethod.invoke(rn, seeds);

                // TODO: We can also call RNG.resetSeed() which recreates the
                // seed state (array) from the original seed
            }
            // *** End RNG seed state reset ***

            textAreaOutputStream = new JTextAreaOutputStream(assemblySimulationRunPanel.outputStreamTA, 16*1024);

            setOutputStreamMethod.invoke(assemblyInstance, textAreaOutputStream);
            setNumberReplicationsMethod.invoke(assemblyInstance, Integer.valueOf(assemblySimulationRunPanel.numberReplicationsTF.getText().trim()));
            setPrintReplicationReportsMethod.invoke(assemblyInstance, assemblySimulationRunPanel.printReplicationReportsCB.isSelected());
            setPrintSummaryReportMethod.invoke(assemblyInstance, assemblySimulationRunPanel.printSummaryReportsCB.isSelected());

            /* DIFF between OA3302 branch and trunk */
            setSaveReplicationDataMethod.invoke(assemblyInstance, assemblySimulationRunPanel.saveReplicationDataCB.isSelected());
            setEnableAnalystReports.invoke(assemblyInstance, assemblySimulationRunPanel.analystReportCB.isSelected());
            /* End DIFF between OA3302 branch and trunk */

            // Allow panel values to override XML set values
            setStopTimeMethod.invoke(assemblyInstance, getStopTime());
            setVerboseMethod.invoke(assemblyInstance, getVerbose());

            setVerboseReplicationMethod.invoke(assemblyInstance, getVerboseReplicationNumber());
            setPclNodeCacheMethod.invoke(assemblyInstance, ((AssemblyModelImpl) ViskitGlobals.instance().getActiveAssemblyModel()).getNodeCache());
            addPropertyChangeListenerMethod.invoke(assemblyInstance, this);
            assemblyRunnable = (Runnable) assemblyInstance;

            // Start the simulation run(s)
            simulationRunnerThread = new Thread(assemblyRunnable);
            new SimulationRunMonitor(simulationRunnerThread).execute();

            // Restore Viskit's working ClassLoader
            Thread.currentThread().setContextClassLoader(lastLoaderNoReset);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | ClassNotFoundException exception) 
        {
            LOG.error(exception);
        }
    }

    /** Thread to perform simulation run and end of run cleanup items */
    public class SimulationRunMonitor extends SwingWorker<Void, Void> 
    {
        Thread waitOn;

        public SimulationRunMonitor(Thread toWaitOn) {
            waitOn = toWaitOn;
        }

        @Override
        public Void doInBackground() {
            setProgress(0);

            waitOn.start();
            try {
                waitOn.join();
            } catch (InterruptedException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            }
            return null;
        }

        // Perform simulation stop and reset calls
        @Override
        public void done() {
            setProgress(100);

            // Grab the temp analyst report and signal the AnalystReportFrame
            try {
                Method getAnalystReportMethod = assemblyClass.getMethod("getAnalystReport");
                analystReportTempFile = (String) getAnalystReportMethod.invoke(assemblyInstance);
            } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                LOG.error(ex);
                return;
            }

            signalAnalystReportReady();

            System.out.println("Simulation ended");
            System.out.println("----------------");

            assemblySimulationRunPanel.nowRunningLabel.setText("<html><body><p><b>Replications complete\n</b></p></body></html>");
            assemblySimulationRunStopListener.actionPerformed(null);
        }
    }

    public ActionListener getAssemblyRunStopListener() {
        return assemblySimulationRunStopListener;
    }

    /**
     * Retrieves the value of the verboseRepNumber text field. This number
     * starts counting at 0, the method will return -1 for blank
     * or non-integer value.
     * @return the replication instance to output verbose on
     */
    public int getVerboseReplicationNumber() {
        int ret = -1;
        try {
            ret = Integer.parseInt(assemblySimulationRunPanel.verboseReplicationNumberTF.getText().trim());
        } catch (NumberFormatException ex) {
          //  ;
        }
        return ret;
    }

    class StartResumeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) 
        {
            assemblySimulationRunPanel.vcrStartTimeTF.setText("0.0");    // because no pausing
            vcrButtonPressDisplayUpdate(Event.START);
            initializeAssemblySimulationRun();
        }
    }

    class StepListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) // TODO editing in progress, not fully tested
        {
            try // StepListener
            {
                if (assemblyInstance != null) {

                    Thread.currentThread().setContextClassLoader(lastLoaderWithReset);

                    Method setStepRunMethod = assemblyClass.getMethod("setStepRun", boolean.class);
                    setStepRunMethod.invoke(assemblyInstance, true);

//                    if (textAreaOutputStream != null)
//                        textAreaOutputStream.kill();
//
//                    mutex--;
                }

                Schedule.coldReset();

                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader != null && !loader.equals(lastLoaderNoReset))
                    Thread.currentThread().setContextClassLoader(lastLoaderNoReset);

            } 
            catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {

                // Some screwy stuff can happen here if a user jams around with
                // the initialize Assembly run button and tabs back and forth
                // between the Assembly editor and the Assembly runner panel, but it
                // won't impede a correct Assembly run.  Catch the
                // IllegalArgumentException and move on.
//                LOG.error(ex);
//                ex.printStackTrace();
            }
            vcrButtonPressDisplayUpdate(Event.STEP);
        }
    }

    /** Restores the Viskit default ClassLoader after an Assembly compile and
     * run.  Performs a Schedule.coldReset() to clear Simkit for the next run.
     */
    public class StopListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try // StopListener
            {
                if (assemblyInstance != null) {

                    Thread.currentThread().setContextClassLoader(lastLoaderWithReset);

                    Method setStopRunMethod = assemblyClass.getMethod("setStopRun", boolean.class);
                    setStopRunMethod.invoke(assemblyInstance, true);

                    if (textAreaOutputStream != null)
                        textAreaOutputStream.kill();

                    mutex--;
                }

                Schedule.coldReset();

                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader != null && !loader.equals(lastLoaderNoReset))
                    Thread.currentThread().setContextClassLoader(lastLoaderNoReset);

            } 
            catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {

                // Some screwy stuff can happen here if a user jams around with
                // the initialize Assembly run button and tabs back and forth
                // between the Assembly editor and the Assembly runner panel, but it
                // won't impede a correct Assembly run.  Catch the
                // IllegalArgumentException and move on.
//                LOG.error(ex);
//                ex.printStackTrace();
            }
            vcrButtonPressDisplayUpdate(Event.STOP);
        }
    }

    class RewindListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            vcrButtonPressDisplayUpdate(Event.REWIND);
        }
    }

    /** Allow for overriding XML set value via the Run panel setting */
    class VerboseListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (basicAssembly == null) {return;}
            basicAssembly.setVerbose(((AbstractButton) e.getSource()).isSelected());
        }
    }

    private JFileChooser saveChooser;

    class SaveListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (saveChooser == null) {
                saveChooser = new JFileChooser(ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot());
            }
            File fil = ViskitGlobals.instance().getEventGraphEditor().getUniqueName("AssemblyOutput.txt", saveChooser.getCurrentDirectory());
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

            try (Writer bw = new BufferedWriter(new FileWriter(fil))) {
                bw.write(assemblySimulationRunPanel.outputStreamTA.getText());
            } catch (IOException e1) {
                ViskitGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.ERROR_MESSAGE, "I/O Error,", e1.getMessage() );
            }
        }
    }

    private void signalAnalystReportReady() {
        if (analystReportTempFile == null) {
            // No report to print
            return;
        }

        AnalystReportController analystReportController = (AnalystReportController) ViskitGlobals.instance().getAnalystReportController();
        if (analystReportController != null) {
            analystReportController.setReportXML(analystReportTempFile);

            // Switch over to the analyst report tab if we have a report ready
            // for editing
            AnalystReportModel analystReportModel = (AnalystReportModel) analystReportController.getModel();
            if (analystReportModel != null && analystReportModel.isReportReady())
            {
                analystReportController.mainTabbedPane.setSelectedIndex(analystReportController.mainTabbedPaneIdx);
                analystReportModel.setReportReady(false);
            }
        } 
        else 
        {
            ViskitGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Analyst Report Panel not visible",
                    "<html><body><p align='center'>" +
                    "The Analyst Report tab has not been set to be visible.<br>To " +
                    "view on next Viskit opening, select File -> Viskit Settings -> " +
                    "Tab visibility -> Select Analyst report -> Close, then Exit" +
                    " the application. On re-startup, it will appear.</p></body></html>"
            );
        }
    }

    /** Allow for overriding XML set value via the Run panel setting
     * @return overridden XML set value via the Run panel setting
     */
    double getStopTime() {
        return Double.parseDouble(assemblySimulationRunPanel.vcrStopTimeTF.getText());
    }

    /** Allow for overriding XML set value via the Run panel setting
     * @return overridden XML set value via the Run panel setting
     */
    boolean getVerbose() {
        return assemblySimulationRunPanel.vcrVerboseCB.isSelected();
    }

    public enum Event {
        START, STOP, STEP, REWIND, OFF
    }

    public void vcrButtonPressDisplayUpdate(Event newEvent) {
        switch (newEvent) {
            case START:
                assemblySimulationRunPanel.vcrPlayButton.setEnabled(false);
                assemblySimulationRunPanel.vcrStepButton.setEnabled(true);
                assemblySimulationRunPanel.vcrStopButton.setEnabled(true);
                assemblySimulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            case STEP:
                assemblySimulationRunPanel.vcrPlayButton.setEnabled(true);
                assemblySimulationRunPanel.vcrStepButton.setEnabled(true);
                assemblySimulationRunPanel.vcrStopButton.setEnabled(true);
                assemblySimulationRunPanel.vcrRewindButton.setEnabled(true);
                break;
            case STOP:
                assemblySimulationRunPanel.vcrPlayButton.setEnabled(true);
                assemblySimulationRunPanel.vcrStepButton.setEnabled(true);
                assemblySimulationRunPanel.vcrStopButton.setEnabled(false);
                assemblySimulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            case REWIND:
                assemblySimulationRunPanel.vcrPlayButton.setEnabled(true);
                assemblySimulationRunPanel.vcrStepButton.setEnabled(true); 
                assemblySimulationRunPanel.vcrStopButton.setEnabled(false);
                assemblySimulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            case OFF:
                assemblySimulationRunPanel.vcrPlayButton.setEnabled(false);
                assemblySimulationRunPanel.vcrStepButton.setEnabled(false);
                assemblySimulationRunPanel.vcrStopButton.setEnabled(false);
                assemblySimulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            default:
                LOG.warn("*** Unrecognized vcrButtonListener(event=" + newEvent + ")");
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

        save.addActionListener(saveListener);
        copy.addActionListener(new CopyListener());
        selAll.addActionListener(new SelectAllListener());
        clrAll.addActionListener(new ClearListener());
        view.addActionListener(new ViewListener());

        file.add(save);
        file.add(view);

        file.addSeparator();
        file.add(new JMenuItem("Viskit Settings"));

        edit.add(copy);
        edit.add(selAll);
        edit.add(clrAll);
        myMenuBar.add(file);
        myMenuBar.add(edit);
    }

    class CopyListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String s = assemblySimulationRunPanel.outputStreamTA.getSelectedText();
            StringSelection ss = new StringSelection(s);
            Clipboard clpbd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbd.setContents(ss, ss);
        }
    }

    class SelectAllListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            assemblySimulationRunPanel.outputStreamTA.requestFocus();
            assemblySimulationRunPanel.outputStreamTA.selectAll();
        }
    }

    class ClearListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            assemblySimulationRunPanel.outputStreamTA.setText(null);
        }
    }

    class ViewListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            File f; // = tmpFile;
            String osName = ViskitStatics.OPERATING_SYSTEM;
            String filePath = "";
            String tool;
            if (osName.toLowerCase().contains("win")) {
                tool = "notepad";
            } else if (osName.toLowerCase().contains("mac")) {
                tool = "open -a";
            } else {
                tool = "gedit"; // assuming Linux here
            }

            String consoleText = assemblySimulationRunPanel.outputStreamTA.getText().trim();
            try {
                f = TempFileManager.createTempFile("ViskitOutput", ".txt");
                f.deleteOnExit();
                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f))) {
                    bufferedWriter.append(consoleText);
                }
                filePath = f.getCanonicalPath();
                Desktop.getDesktop().open(new File(filePath));
              }
            catch (IOException ex) {
            }
            catch (UnsupportedOperationException ex) {
              try {
                  Runtime.getRuntime().exec(new String[] {tool + " " + filePath});
              }
              catch (IOException ex1) {
                  LOG.error(ex1);
//                  ex1.printStackTrace();
              }
            }
        }
    }

    private final String namePrefix = "Viskit Assembly Runner";
    private StringBuilder currentTitle = new StringBuilder();

    public void doTitle(String nm) {
        currentTitle.append(namePrefix);
        if (nm != null && nm.length() > 0) {
            currentTitle = currentTitle.append(": ").append(nm);
        }

        if (titlList != null) {
            titlList.setTitle(currentTitle.toString(), titlkey);
        }
        currentTitle.setLength(0); // reset
    }
    private TitleListener titlList;
    private int titlkey;

    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlkey = key;
        doTitle(null);
    }

    StringBuilder nowRunningsString = new StringBuilder("<html><body><font color=black>\n" + "<p><b>Now Running Replication ");

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        LOG.debug(evt.getPropertyName());

        if (evt.getPropertyName().equals("replicationNumber")) {
            int beginLength = nowRunningsString.length();
            nowRunningsString.append(evt.getNewValue());
            assemblySimulationRunPanel.setNumberOfReplications(Integer.parseInt(evt.getNewValue().toString()));
            nowRunningsString.append(" of ");
            nowRunningsString.append(Integer.parseInt(assemblySimulationRunPanel.numberReplicationsTF.getText()));
            nowRunningsString.append("</b>\n");
            nowRunningsString.append("</font></p></body></html>\n");
            assemblySimulationRunPanel.nowRunningLabel.setText(nowRunningsString.toString());

            // reset display string in preparation for the next replication output
            nowRunningsString.delete(beginLength, nowRunningsString.length());
        }
    }

}  // end class file InternalAssemblySimulationRunner.java
