/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

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

import viskit.view.SimulationRunPanel;

import edu.nps.util.Log4jUtilities;
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
import viskit.model.AnalystReportModel;
import viskit.model.AssemblyModelImpl;
import viskit.view.dialog.ViskitUserPreferences;

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
public class TextAreaOutputStream implements PropertyChangeListener 
{
    static final Logger LOG = Log4jUtilities.getLogger(TextAreaOutputStream.class);

    /** The name of the basicAssembly to run */
    private String simulationRunAssemblyClassName;
    private SimulationRunPanel simulationRunPanel;
    private ActionListener saveListener;
    private JMenuBar myMenuBar;
    private JMenu simulationRunMenu;
    private Thread simulationRunThread;
    private BasicAssembly basicAssembly;

    /** external runner saves a file */
    private String analystReportTempFile = null;

    /** The basicAssembly to be run from java source */
    Class<?> simulationRunAssemblyClass;

    /** Instance of the basicAssembly to run from java source */
    Object simulationRunAssemblyInstance;
    private static int mutex = 0;
    
    private final ClassLoader lastWorkingClassLoaderNoReset;
    private       ClassLoader lastRunSimulationClassLoader;

    /** Captures the original RNG seed state */
    private long[] seeds;
    private final StopListener assemblySimulationRunStopListener;
    
    private AnalystReportModel analystReportModel;

    public static final String ASSEMBLY_SIMULATION_RUN_PANEL_TITLE = "Simulation Run Console";
    /**
     * The internal logic for the Assembly Runner panel
     * @param analystReportPanelVisible if true, the Analyst Report panel will be visible
     */
    public TextAreaOutputStream(boolean analystReportPanelVisible) 
    {
        saveListener = new SaveListener();

        // NOTE:
        // Don't supply rewind or pause buttons on VCR, not hooked up, or working right.
        // false will enable all VCR buttons. Currently, only start and stop work

        simulationRunPanel = new SimulationRunPanel(ASSEMBLY_SIMULATION_RUN_PANEL_TITLE, false, analystReportPanelVisible);
        buildMenus();
        simulationRunPanel.vcrStopButton.addActionListener(assemblySimulationRunStopListener = new StopListener());
        simulationRunPanel.vcrPlayButton.addActionListener(new StartResumeListener());
        simulationRunPanel.vcrRewindButton.addActionListener(new RewindListener());
        simulationRunPanel.vcrStepButton.addActionListener(new StepListener());
        simulationRunPanel.vcrVerboseCB.addActionListener(new VerboseListener());
        simulationRunPanel.vcrStopButton.setEnabled(false);
        simulationRunPanel.vcrPlayButton.setEnabled(false);
        simulationRunPanel.vcrRewindButton.setEnabled(false);
        simulationRunPanel.vcrStepButton.setEnabled(false);

        // Save Viskit's current working ClassLoader for later restoration
        lastWorkingClassLoaderNoReset = ViskitGlobals.instance().getWorkingClassLoader();

        // Provide access to Enable Analyst Report checkbox
        ViskitGlobals.instance().setSimulationRunPanel(simulationRunPanel);
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

        simulationRunAssemblyClassName = params[AssemblyControllerImpl.EXEC_TARGET_CLASS_NAME];
        doTitle(simulationRunAssemblyClassName);

        simulationRunPanel.vcrStartTimeTF.setText("0.0");

        // These values are from the XML file
        boolean defaultVerbose = Boolean.parseBoolean(params[AssemblyControllerImpl.EXEC_VERBOSE_SWITCH]);
        boolean saveRepDataToXml = simulationRunPanel.analystReportCB.isSelected();
        double defaultStopTime = Double.parseDouble(params[AssemblyControllerImpl.EXEC_STOPTIME_SWITCH]);

        try {
            fillSimulationRunButtonsFromAssemblyInitialization(defaultVerbose, saveRepDataToXml, defaultStopTime);
        } 
        catch (Throwable throwable) 
        {
            ViskitGlobals.instance().getMainFrame().genericReport(
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

    private void fillSimulationRunButtonsFromAssemblyInitialization(boolean verbose, boolean saveReplicationDataToXml, double stopTime) throws Throwable 
    {
        simulationRunAssemblyClass = ViskitStatics.classForName(simulationRunAssemblyClassName);
        if (simulationRunAssemblyClass == null) {
            throw new ClassNotFoundException();
        }
        simulationRunAssemblyInstance = simulationRunAssemblyClass.getDeclaredConstructor().newInstance();

        /* in order to resolve the basicAssembly as a BasicAssembly, it must be
         * loaded using the the same ClassLoader as the one used to compile
         * it. Used in the verboseListener within the working Viskit ClassLoader */
        // the follow-on initializations using ViskitGlobals and ViskitUserPreferences
        // must occur prior to threading and new RunSimulationClassLoader
        basicAssembly = (BasicAssembly) simulationRunAssemblyInstance;

        basicAssembly.setWorkingDirectory (ViskitGlobals.instance().getProjectWorkingDirectory());     
        basicAssembly.setClassPathUrlArray(ViskitUserPreferences.getExtraClassPathArraytoURLArray());

        Method getNumberReplicationsMethod = simulationRunAssemblyClass.getMethod("getNumberReplications");
        Method isSaveReplicationData = simulationRunAssemblyClass.getMethod("isSaveReplicationData"); // TODO hook this up
        Method isPrintReplicationReportsMethod = simulationRunAssemblyClass.getMethod("isPrintReplicationReports");
        Method isPrintSummaryReportMethod = simulationRunAssemblyClass.getMethod("isPrintSummaryReport");
        Method setVerboseMethod = simulationRunAssemblyClass.getMethod("setVerbose", boolean.class);
        Method isVerboseMethod = simulationRunAssemblyClass.getMethod("isVerbose");
        Method setStopTimeMethod = simulationRunAssemblyClass.getMethod("setStopTime", double.class);
        Method getStopTimeMethod = simulationRunAssemblyClass.getMethod("getStopTime");

        simulationRunPanel.numberReplicationsTF.setText("" + getNumberReplicationsMethod.invoke(simulationRunAssemblyInstance));
        simulationRunPanel.printReplicationReportsCB.setSelected((Boolean) isPrintReplicationReportsMethod.invoke(simulationRunAssemblyInstance));
        simulationRunPanel.printSummaryReportsCB.setSelected((Boolean) isPrintSummaryReportMethod.invoke(simulationRunAssemblyInstance));
        simulationRunPanel.saveReplicationDataCB.setSelected(saveReplicationDataToXml);

        // Set the run panel verboseness according to what the basicAssembly XML value is
        setVerboseMethod.invoke(simulationRunAssemblyInstance, verbose);
        simulationRunPanel.vcrVerboseCB.setSelected((Boolean) isVerboseMethod.invoke(simulationRunAssemblyInstance));
        setStopTimeMethod.invoke(simulationRunAssemblyInstance, stopTime);
        simulationRunPanel.vcrStopTimeTF.setText("" + getStopTimeMethod.invoke(simulationRunAssemblyInstance));
    }
    viskit.assembly.TextAreaOutputStream textAreaOutputStream;
    Runnable assemblyRunnable;

    protected void initializeAssemblySimulationRun() 
    {
        // Prevent multiple pushes of the sim run button
        mutex++;
        if (mutex > 1)
            return;

        try // initializeAssemblySimulationRun
        {
            // the follow-on initializations using ViskitGlobals and ViskitUserPreferences
            // must occur prior to threading and new RunSimulationClassLoader
////            basicAssembly.resetRunSimulationClassLoader(); // TODO wrong place for this, likely out of place
            basicAssembly.setWorkingDirectory(ViskitGlobals.instance().getProjectWorkingDirectory());
            basicAssembly.setClassPathUrlArray(ViskitUserPreferences.getExtraClassPathArraytoURLArray());
            lastRunSimulationClassLoader = basicAssembly.getRunSimulationClassLoader(); // TODO reset?
            
            // Now we are in the pure classloader realm where each basicAssembly run can be independent of any other
            simulationRunAssemblyClass    = lastRunSimulationClassLoader.loadClass(simulationRunAssemblyClass.getName());
            // TODO the BasicAssembly instantiation and constuctor is now causing the singleton failure
            simulationRunAssemblyInstance = simulationRunAssemblyClass.getDeclaredConstructor().newInstance();

            Method setOutputStreamMethod            = simulationRunAssemblyClass.getMethod("setOutputStream", OutputStream.class);
            Method setNumberReplicationsMethod      = simulationRunAssemblyClass.getMethod("setNumberReplications", int.class);
            Method setPrintReplicationReportsMethod = simulationRunAssemblyClass.getMethod("setPrintReplicationReports", boolean.class);
            Method setPrintSummaryReportMethod      = simulationRunAssemblyClass.getMethod("setPrintSummaryReport", boolean.class);
            Method setSaveReplicationDataMethod     = simulationRunAssemblyClass.getMethod("setSaveReplicationData", boolean.class);
            Method setEnableAnalystReports          = simulationRunAssemblyClass.getMethod("setEnableAnalystReports", boolean.class);
            Method setVerboseMethod                 = simulationRunAssemblyClass.getMethod("setVerbose", boolean.class);
            Method setStopTimeMethod                = simulationRunAssemblyClass.getMethod("setStopTime", double.class);
            Method setVerboseReplicationMethod      = simulationRunAssemblyClass.getMethod("setVerboseReplication", int.class);
            Method setPclNodeCacheMethod            = simulationRunAssemblyClass.getMethod("setPclNodeCache", Map.class);
            Method addPropertyChangeListenerMethod  = simulationRunAssemblyClass.getMethod("addPropertyChangeListener", PropertyChangeListener.class);

            // As of discussion held 09 APR 2015, resetting the RNG seed state
            // is not necessary for basic Viskit operation.  Pseudo random
            // independence is guaranteed from the default RNG (normally the
            // MersenneTwister)

            // *** Resetting the RNG seed state ***
            // NOTE: This is currently disabled as the resetSeedStateCB is not
            // enabled nor visible
            if (simulationRunPanel.resetSeedStateCB.isSelected()) 
            {
                Class<?> rVFactClass = lastRunSimulationClassLoader.loadClass(ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS);
                Method getDefaultRandomNumberMethod = rVFactClass.getMethod("getDefaultRandomNumber");
                Object rn = getDefaultRandomNumberMethod.invoke(null);

                Method getSeedsMethod = rn.getClass().getMethod("getSeeds");
                seeds = (long[]) getSeedsMethod.invoke(rn);

                Class<?> rNClass = lastRunSimulationClassLoader.loadClass(ViskitStatics.RANDOM_NUMBER_CLASS);
                Method setSeedsMethod = rNClass.getMethod("setSeeds", long[].class);
                setSeedsMethod.invoke(rn, seeds);

                // TODO: We can also call RNG.resetSeed() which recreates the
                // seed state (array) from the original seed
            }
            // *** End RNG seed state reset ***

            textAreaOutputStream = new viskit.assembly.TextAreaOutputStream(simulationRunPanel.outputStreamTA, 16*1024);

            setOutputStreamMethod.invoke(simulationRunAssemblyInstance, textAreaOutputStream); // redirect output
            setNumberReplicationsMethod.invoke(simulationRunAssemblyInstance, Integer.valueOf(simulationRunPanel.numberReplicationsTF.getText().trim()));
            setPrintReplicationReportsMethod.invoke(simulationRunAssemblyInstance, simulationRunPanel.printReplicationReportsCB.isSelected());
            setPrintSummaryReportMethod.invoke(simulationRunAssemblyInstance, simulationRunPanel.printSummaryReportsCB.isSelected());

            setSaveReplicationDataMethod.invoke(simulationRunAssemblyInstance, simulationRunPanel.saveReplicationDataCB.isSelected());
            setEnableAnalystReports.invoke(simulationRunAssemblyInstance, simulationRunPanel.analystReportCB.isSelected());
            /* End DIFF between OA3302 branch and trunk */

            // Allow panel values to override XML set values
            setStopTimeMethod.invoke(simulationRunAssemblyInstance, getStopTime());
            setVerboseMethod.invoke(simulationRunAssemblyInstance, getVerbose());

            setVerboseReplicationMethod.invoke(simulationRunAssemblyInstance, getVerboseReplicationNumber());
            setPclNodeCacheMethod.invoke(simulationRunAssemblyInstance, ((AssemblyModelImpl) ViskitGlobals.instance().getActiveAssemblyModel()).getNodeCache());
            addPropertyChangeListenerMethod.invoke(simulationRunAssemblyInstance, this);
            assemblyRunnable = (Runnable) simulationRunAssemblyInstance;

            // Simulation run start the in a separate thread
            simulationRunThread = new Thread(assemblyRunnable);
            new SimulationRunMonitor(simulationRunThread).execute();
            // Simulation Run thread is now launched and will execute separately

            // Restore thread context to Viskit's WorkingClassLoader prior to returning control
            Thread.currentThread().setContextClassLoader(lastWorkingClassLoaderNoReset);
            LOG.info("initializeAssemblySimulationRun() complete");
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | ClassNotFoundException exception) 
        {
            LOG.error("initializeAssemblySimulationRun() reflection exception: " + exception);
        }
        catch (Exception ue) // sometimes strange things happen with reflection and threading
        {
            LOG.error("initializeAssemblySimulationRun() reflection uncaught exception: " + ue);
        }
    }

    /** Thread to perform simulation run and end of run cleanup items */
    public class SimulationRunMonitor extends SwingWorker<Void, Void> 
    {
        Thread simulationRunMonitorThread;

        public SimulationRunMonitor(Thread newSimulationRunMonitorThread) {
              simulationRunMonitorThread = newSimulationRunMonitorThread;
        }

        @Override
        public Void doInBackground() {
            setProgress(0);

            simulationRunMonitorThread.start();
            try {
                simulationRunMonitorThread.join();
                LOG.info("SimulationRunMonitor now running in simulationRunThread");
            } 
            catch (InterruptedException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            }
            return null;
        }

        // Perform simulation stop and reset calls
        @Override
        public void done()
        {
            setProgress(100);
            
            // TODO perform analyst report work here, avoid reflection!!
            

            // Grab the temp Analyst Report and signal the AnalystReportFrame
            try {
                Method getAnalystReportMethod = simulationRunAssemblyClass.getMethod("getAnalystReport");
                analystReportTempFile = (String) getAnalystReportMethod.invoke(simulationRunAssemblyInstance);
            } 
            catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                LOG.error(ex);
                return;
            }

            signalAnalystReportReady(); // saves temp report

            System.out.println("+------------------+"); // output goes to console TextArea
            System.out.println("| Simulation ended |");
            System.out.println("+------------------+");

            simulationRunPanel.nowRunningLabel.setText("<html><body><p><b>Replications complete\n</b></p></body></html>");
            assemblySimulationRunStopListener.actionPerformed(null);
            
            // TODO when do we clean up this thread, or does it automatically get removed?
        }
    } // end SimulationRunMonitor class

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
            ret = Integer.parseInt(simulationRunPanel.verboseReplicationNumberTF.getText().trim());
        } catch (NumberFormatException ex) {
          //  ;
        }
        return ret;
    }

    class StartResumeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) 
        {
            simulationRunPanel.vcrStartTimeTF.setText("0.0");    // because no pausing
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
                if (simulationRunAssemblyInstance != null) {

                    Thread.currentThread().setContextClassLoader(lastRunSimulationClassLoader);

                    Method setStepRunMethod = simulationRunAssemblyClass.getMethod("setStepRun", boolean.class);
                    setStepRunMethod.invoke(simulationRunAssemblyInstance, true);

//                    if (textAreaOutputStream != null)
//                        textAreaOutputStream.kill();
//
//                    mutex--;
                }

                Schedule.coldReset();

                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if ((classLoader != null) && !classLoader.equals(lastWorkingClassLoaderNoReset))
                    Thread.currentThread().setContextClassLoader(lastWorkingClassLoaderNoReset); // rejoin regular thread

                // TODO should prior thread be removed?
            } 
            catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {

                // Some screwy stuff can happen here if a user jams around with
                // the Prepare Assembly Run button and tabs back and forth
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
                if (simulationRunAssemblyInstance != null) {

                    Thread.currentThread().setContextClassLoader(lastRunSimulationClassLoader);

                    Method setStopRunMethod = simulationRunAssemblyClass.getMethod("setStopRun", boolean.class);
                    setStopRunMethod.invoke(simulationRunAssemblyInstance, true);

                    if (textAreaOutputStream != null)
                        textAreaOutputStream.kill();

                    mutex--;
                }

                Schedule.coldReset();

                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if ((classLoader != null) && !classLoader.equals(lastWorkingClassLoaderNoReset))
                    Thread.currentThread().setContextClassLoader(lastWorkingClassLoaderNoReset); // rejoin regular thread

            } 
            catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {

                // Some screwy stuff can happen here if a user jams around with
                // the Prepare Assembly Run button and tabs back and forth
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
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().getMainFrame().selectSimulationRunTab();
            if (saveChooser == null) {
                saveChooser = new JFileChooser(ViskitGlobals.instance().getViskitProject().getProjectRoot());
            }
            File consoleFile = ViskitGlobals.instance().getEventGraphViewFrame().getUniqueName("SimulationRunOutput.txt", saveChooser.getCurrentDirectory());
            saveChooser.setSelectedFile(consoleFile);
            saveChooser.setDialogTitle("Save Console Output");

            int retv = saveChooser.showSaveDialog(null);
            if (retv != JFileChooser.APPROVE_OPTION) {
                return;
            }

            consoleFile = saveChooser.getSelectedFile();
            if (consoleFile.exists()) {
                int r = JOptionPane.showConfirmDialog(null, "File exists.  Overwrite?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try (Writer bw = new BufferedWriter(new FileWriter(consoleFile))) {
                bw.write(simulationRunPanel.outputStreamTA.getText());
            } catch (IOException e1) {
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, "I/O Error,", e1.getMessage() );
            }
        }
    }

    private void signalAnalystReportReady()
    {
        if (analystReportTempFile == null) 
        {
            // No report to print, TODO undexpected
            return;
        }
        AnalystReportController analystReportController = ViskitGlobals.instance().getAnalystReportController();
        if (analystReportController != null)
        {
            analystReportController.setReportXML(analystReportTempFile);

            // Switch over to the Analyst Report tab if we have a report ready for editing
            analystReportModel = (AnalystReportModel) analystReportController.getModel();
            if (analystReportModel != null && analystReportModel.isReportReady())
            {
                analystReportController.mainTabbedPane.setSelectedIndex(analystReportController.mainTabbedPaneIdx);
                analystReportModel.setReportReady(false); // TODO false??
            }
        } 
        else 
        {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Analyst Report Panel not visible",
                    "<html><body><p align='center'>" +
                    "The Analyst Report tab has not been set to be visible.<br>" +
                    "To view on next Viskit opening, select File -> Viskit User Preferences -> " +
                    "Tab visibility -> Select Analyst report -> Close, " +
                    "then Exit the application. On re-startup, the panel will appear.</p></body></html>"
            );
        }
    }

    /** Allow for overriding XML set value via the Run panel setting
     * @return overridden XML set value via the Run panel setting
     */
    double getStopTime() {
        return Double.parseDouble(simulationRunPanel.vcrStopTimeTF.getText());
    }

    /** Allow for overriding XML set value via the Run panel setting
     * @return overridden XML set value via the Run panel setting
     */
    boolean getVerbose() {
        return simulationRunPanel.vcrVerboseCB.isSelected();
    }

    public enum Event {
        START, STOP, STEP, REWIND, OFF
    }

    public void vcrButtonPressDisplayUpdate(Event newEvent) {
        switch (newEvent) {
            case START:
                simulationRunPanel.vcrPlayButton.setEnabled(false);
                simulationRunPanel.vcrStepButton.setEnabled(false); // TODO
                simulationRunPanel.vcrStopButton.setEnabled(true);
                simulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            case STEP:
                simulationRunPanel.vcrPlayButton.setEnabled(true);
                simulationRunPanel.vcrStepButton.setEnabled(false); // TODO
                simulationRunPanel.vcrStopButton.setEnabled(true);
                simulationRunPanel.vcrRewindButton.setEnabled(true);
                break;
            case STOP:
                simulationRunPanel.vcrPlayButton.setEnabled(true);
                simulationRunPanel.vcrStepButton.setEnabled(false); // TODO
                simulationRunPanel.vcrStopButton.setEnabled(false);
                simulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            case REWIND:
                simulationRunPanel.vcrPlayButton.setEnabled(true);
                simulationRunPanel.vcrStepButton.setEnabled(false); // TODO 
                simulationRunPanel.vcrStopButton.setEnabled(false);
                simulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            case OFF:
                simulationRunPanel.vcrPlayButton.setEnabled(false);
                simulationRunPanel.vcrStepButton.setEnabled(false);
                simulationRunPanel.vcrStopButton.setEnabled(false);
                simulationRunPanel.vcrRewindButton.setEnabled(false);
                break;
            default:
                LOG.warn("*** Unrecognized vcrButtonListener(event=" + newEvent + ")");
                break;
        }
    }

    private void buildMenus()
    {
        myMenuBar = new JMenuBar();
//      JMenu fileMenu = new JMenu("File");
        simulationRunMenu = new JMenu("Simulation Run");
        simulationRunMenu.setToolTipText("Simulation Run performs multiple replications of a compiled Assembly model");
        simulationRunMenu.setMnemonic('S');
        JMenuItem copyMI = new JMenuItem("Copy selected console text");
        copyMI.setMnemonic('C');
        copyMI.setToolTipText("Copy simulation run console output");
        JMenuItem saveMI = new JMenuItem("Save console text to file");
        saveMI.setMnemonic('S');
        saveMI.setToolTipText("Save simulation run console output to a file");
        JMenuItem selectAllMI = new JMenuItem("Select all console text");
        selectAllMI.setMnemonic('S');
        selectAllMI.setToolTipText("Select all text in the console log");
        JMenuItem clearAllMI = new JMenuItem("Clear all console text");
        clearAllMI.setMnemonic('C');
        clearAllMI.setToolTipText("Clear the console text area");
        JMenuItem viewMI = new JMenuItem("View console output in text editor");
        viewMI.setMnemonic('V');
        viewMI.setToolTipText("Directly launch console output to text editor");

        copyMI.addActionListener(new CopyListener());
        saveMI.addActionListener(saveListener);
        selectAllMI.addActionListener(new SelectAllListener());
        clearAllMI.addActionListener(new ClearListener());
        viewMI.addActionListener(new ViewListener());

        simulationRunMenu.add(copyMI);
        simulationRunMenu.add(saveMI);
        simulationRunMenu.add(selectAllMI);
        simulationRunMenu.add(clearAllMI);
        simulationRunMenu.add(viewMI);

        if (ViskitGlobals.instance().getMainFrame().hasModalMenus())
        {
        getSimulationRunMenu().addSeparator();
        getSimulationRunMenu().add(new JMenuItem("Viskit Preferences"));
        }
        myMenuBar.add(getSimulationRunMenu());

        // No edit functionality needed for SimulationRun panel
//        JMenu editMenu = new JMenu("Edit");
//        editMenu.add(copyMI);
//        editMenu.add(selectAllMI);
//        editMenu.add(clearAllMI);
//        myMenuBar.add(editMenu);
    }

    class CopyListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) 
        {
            ViskitGlobals.instance().getMainFrame().selectSimulationRunTab();
            String s = simulationRunPanel.outputStreamTA.getSelectedText();
            StringSelection ss = new StringSelection(s);
            Clipboard clpbd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbd.setContents(ss, ss);
        }
    }

    class SelectAllListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().getMainFrame().selectSimulationRunTab();
            simulationRunPanel.outputStreamTA.requestFocus();
            simulationRunPanel.outputStreamTA.selectAll();
        }
    }

    class ClearListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().getMainFrame().selectSimulationRunTab();
            simulationRunPanel.outputStreamTA.setText(null);
        }
    }

    class ViewListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().getMainFrame().selectSimulationRunTab();
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

            String consoleText = simulationRunPanel.outputStreamTA.getText().trim();
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

    private final String initialValue = "awaiting initialization by Assembly Editor..."; // TODO unscramble logic
    private StringBuilder currentTitle = new StringBuilder();

    public void doTitle(String newName)
    {
        currentTitle.setLength(0); // reset
       
        if (newName != null && newName.length() > 0)
        { 
             currentTitle = currentTitle.append(": ").append(newName);
        }
        else currentTitle.append(initialValue);

        if (titlList != null) {
            titlList.setTitle(currentTitle.toString(), titlkey);
        }
    }
    private TitleListener titlList;
    private int titlkey;

    public void setTitleListener(TitleListener lis, int key) {
        titlList = lis;
        titlkey = key;
//        doTitle(null); // TODO why is this here?
    }

    StringBuilder nowRunningsString = new StringBuilder("<html><body><font color=black>\n" + "<p><b>Now Running Replication ");

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        LOG.debug(evt.getPropertyName());

        if (evt.getPropertyName().equals("replicationNumber")) {
            int beginLength = nowRunningsString.length();
            nowRunningsString.append(evt.getNewValue());
            simulationRunPanel.setNumberOfReplications(Integer.parseInt(evt.getNewValue().toString()));
            nowRunningsString.append(" of ");
            nowRunningsString.append(Integer.parseInt(simulationRunPanel.numberReplicationsTF.getText()));
            nowRunningsString.append("</b>\n");
            nowRunningsString.append("</font></p><br></body></html>\n");
            simulationRunPanel.nowRunningLabel.setText(nowRunningsString.toString());

            // reset display string in preparation for the next replication output
            nowRunningsString.delete(beginLength, nowRunningsString.length());
        }
    }

    /**
     * @return the simulationRunMenu
     */
    public JMenu getSimulationRunMenu() {
        return simulationRunMenu;
    }

}  // end class file TextAreaOutputStream.java
