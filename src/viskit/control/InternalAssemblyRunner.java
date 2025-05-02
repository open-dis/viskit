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
import java.net.URL;
import java.util.Map;
import javax.swing.*;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import simkit.Schedule;

import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitUserConfiguration;
import viskit.assembly.BasicAssembly;
import static viskit.assembly.BasicAssembly.METHOD_addPropertyChangeListener;
import static viskit.assembly.BasicAssembly.METHOD_getAnalystReport;
import static viskit.assembly.BasicAssembly.METHOD_getStopTime;
import static viskit.assembly.BasicAssembly.METHOD_isPrintReplicationReports;
import static viskit.assembly.BasicAssembly.METHOD_isPrintSummaryReport;
import static viskit.assembly.BasicAssembly.METHOD_isSaveReplicationData;
import static viskit.assembly.BasicAssembly.METHOD_isVerbose;
import static viskit.assembly.BasicAssembly.METHOD_setEnableAnalystReports;
import static viskit.assembly.BasicAssembly.METHOD_setOutputStream;
import static viskit.assembly.BasicAssembly.METHOD_setPclNodeCache;
import static viskit.assembly.BasicAssembly.METHOD_setPrintReplicationReports;
import static viskit.assembly.BasicAssembly.METHOD_setPrintSummaryReport;
import static viskit.assembly.BasicAssembly.METHOD_setSaveReplicationData;
import static viskit.assembly.BasicAssembly.METHOD_setSingleStep;
import static viskit.assembly.BasicAssembly.METHOD_setStopSimulationRun;
import static viskit.assembly.BasicAssembly.METHOD_setStopTime;
import static viskit.assembly.BasicAssembly.METHOD_setVerbose;
import viskit.doe.LocalBootLoader;
import viskit.model.AnalystReportModel;
import viskit.model.AssemblyModelImpl;
import static viskit.view.SimulationRunPanel.INITIAL_SIMULATION_RUN_HINT;
import static viskit.view.SimulationRunPanel.SIMULATION_RUN_PANEL_TITLE;
import static viskit.view.SimulationRunPanel.VERBOSE_REPLICATION_NUMBER_DEFAULT_HINT;
import viskit.view.dialog.ViskitUserPreferencesDialog;
import static viskit.assembly.BasicAssembly.METHOD_setVerboseReplicationNumber;
import static viskit.assembly.BasicAssembly.METHOD_getNumberPlannedReplications;
import static viskit.assembly.BasicAssembly.METHOD_setNumberPlannedReplications;

/** Controller for the Assembly RunSimulation panel, which
 * spawns the BasicAssembly thread
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
public class InternalAssemblyRunner implements PropertyChangeListener 
{
    static final Logger LOG = LogManager.getLogger();

    /** The name of the basicAssembly to run */
    private String simulationRunAssemblyClassName;
    private SimulationRunPanel simulationRunPanel;
    private JMenuBar myMenuBar;
    private JMenu  simulationRunMenu;
    private JMenu  simulationButtonsMenu;
    private  JMenuItem rewindButtonMI, runButtonMI, stepButtonMI, stopButtonMI, clearAllConsoleTextMI;
    private Thread simulationRunThread;
    private BasicAssembly basicAssembly;

    /** external runner saves a file */
    private String analystReportTempFile = null;

    /** The basicAssembly to be run from java source */
    Class<?> simulationRunAssemblyClass;

    /** Instance of the basicAssembly to run from java source */
    Object simulationRunAssemblyInstance;
    private static int mutex = 0;
    
    private final ClassLoader priorWorkingClassLoaderNoReset;
    private       ClassLoader priorRunSimulationClassLoader;  // TODO needed?
    
    private /*static*/ ClassLoader runSimulationClassLoader; // this was moved out of ViskitGlobals due to thread-clobbering issues,
    
    private URL[] classPathUrlArray = new URL[0]; // legal default, actual values are set externally

    /** Captures the original RNG seed state */
    private long[] seedsArray;
    private final StopListener assemblySimulationRunStopListener;
    
    viskit.assembly.TextAreaOutputStream textAreaOutputStream;
    Runnable assemblyRunnable;
    
    private AnalystReportModel analystReportModel;
    /**
     * The internal logic for the Assembly Runner panel
     * @param analystReportPanelVisible if true, the Analyst Report panel will be visible
     */
    
    private SimulationState simulationState;
    
    public InternalAssemblyRunner(boolean analystReportPanelVisible) 
    {
        simulationRunPanel = new SimulationRunPanel(SIMULATION_RUN_PANEL_TITLE, true, analystReportPanelVisible);
        buildMenus();
        simulationRunPanel.vcrRewindButton   .addActionListener(new RewindListener());
        simulationRunPanel.vcrRunResumeButton.addActionListener(new RunResumeListener());
        simulationRunPanel.vcrPauseStepButton.addActionListener(new PauseStepListener());
        simulationRunPanel.vcrStopButton     .addActionListener(assemblySimulationRunStopListener = new StopListener());
        simulationRunPanel.vcrVerboseCB      .addActionListener(new VerboseListener());
        
        simulationRunPanel.vcrRewindButton   .setEnabled (false);
        simulationRunPanel.vcrRunResumeButton.setEnabled (false);
        simulationRunPanel.vcrPauseStepButton.setEnabled (false);
        simulationRunPanel.vcrStopButton     .setEnabled (false);
        simulationRunPanel.vcrVerboseCB      .setSelected(false);

        // Save Viskit's current working ClassLoader for later restoration
        priorWorkingClassLoaderNoReset = ViskitGlobals.instance().getViskitApplicationClassLoader();

        // Provide access to Enable Analyst Report checkbox
        ViskitGlobals.instance().setSimulationRunPanel(simulationRunPanel);
        
        vcrButtonPressDisplayUpdate(SimulationState.INACTIVE);
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return null;
    }

    /**
     * Initialize this assembly runner prior to a simulation run
     * @param params arguments to initialize the Assembly runner
     */
    public void preRunInitialization(String[] params) {

//        for (String s : params) {
//            LOG.info("VM argument is: {}", s);
//        }

        simulationRunAssemblyClassName = params[AssemblyControllerImpl.EXEC_TARGET_CLASS_NAME];
        doTitle(simulationRunAssemblyClassName);

        simulationRunPanel.vcrStartTimeTF.setText("0.0");

        // These values are from the XML file
        boolean defaultVerbose   = Boolean.parseBoolean(params[AssemblyControllerImpl.EXEC_VERBOSE_SWITCH]);
        boolean saveRepDataToXml = simulationRunPanel.analystReportCB.isSelected();
        double  defaultStopTime  = Double.parseDouble(params[AssemblyControllerImpl.EXEC_STOPTIME_SWITCH]);

        try {
            simulationRunAssemblyClass = ViskitStatics.classForName(simulationRunAssemblyClassName);
            if (simulationRunAssemblyClass == null) {
                LOG.error("fillSimulationRunButtonsFromAssemblyInitialization() found (simulationRunAssemblyClass == null)");
                throw new ClassNotFoundException();
            }

//            LOG.info("attempting fillSimulationRunButtonsFromAssemblyInitialization() simulationRunAssemblyClass.getDeclaredConstructor(parameterTypes)");
//            // https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/Class.html#getDeclaredConstructor(java.lang.Class...)
//            Class<?>[] parameterTypes = new Class[] { // TODO duplicative
//                File.class, // workingDirectory
//            };
//            // https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/reflect/Constructor.html#newInstance(java.lang.Object...)
//            File workingDirectory = ViskitGlobals.instance().getProjectWorkingDirectory();
//            // note that parameter list of getDeclaredConstructor type must match newInstance being created
//            // BLOCKER: autogenerated code (e.g. ServerAssembly3.java does not include the additional constructor of interest
//            simulationRunAssemblyInstance = simulationRunAssemblyClass.getDeclaredConstructor(parameterTypes).newInstance(workingDirectory);
            simulationRunAssemblyInstance = simulationRunAssemblyClass.getDeclaredConstructor().newInstance(); // empty constructor, TODO omit

            /* in order to resolve the basicAssembly as a BasicAssembly, it must be
             * loaded using the the same ClassLoader as the one used to compile
             * it. Used in the verboseListener within the working Viskit ClassLoader */
            // the follow-on initializations using ViskitGlobals and ViskitUserPreferencesDialog
            // must occur prior to threading and new RunSimulationClassLoader
            basicAssembly = (BasicAssembly) simulationRunAssemblyInstance;
            basicAssembly.setWorkingClassLoader(priorWorkingClassLoaderNoReset);

            basicAssembly.setViskitProject(ViskitGlobals.instance().getViskitProject());

            basicAssembly.setWorkingDirectory(ViskitGlobals.instance().getProjectWorkingDirectory()); // TODO duplicate invocation?
            // trace::  basicAssembly.getWorkingDirectory();
        
            fillSimulationRunButtonsFromAssemblyInitialization(defaultVerbose, saveRepDataToXml, defaultStopTime);
        } 
        catch (Throwable throwable) 
        {
            ViskitGlobals.instance().getMainFrame().genericReport(
                    JOptionPane.ERROR_MESSAGE,
                    "Java Error",
                    "Error initializing Assembly:\n" + throwable.getMessage()
            );
            vcrButtonPressDisplayUpdate(SimulationState.INACTIVE);
            simulationRunPanel.outputStreamTA.setText(INITIAL_SIMULATION_RUN_HINT); // duplicative since handling exception
//            throwable.printStackTrace();
            return;
        }
        // reset state machine
        vcrButtonPressDisplayUpdate(SimulationState.REWIND);
        vcrButtonPressDisplayUpdate(SimulationState.READY);
        
//        try {
//            SwingUtilities.invokeLater(() -> {
                 simulationRunPanel.outputStreamTA.setText("");
                 simulationRunPanel.outputStreamTA.setText(INITIAL_SIMULATION_RUN_HINT);
                 vcrButtonPressDisplayUpdate(SimulationState.INACTIVE);
//            });
//        } 
//        catch (Exception ex) {
//            LOG.error("preRunInitialization() console exception: " + ex.getMessage());
//        }
//       
    }

    private void fillSimulationRunButtonsFromAssemblyInitialization(boolean verbose, boolean saveReplicationDataToXml, double stopTime) throws Throwable 
    {
        setClassPathUrlArray(ViskitUserPreferencesDialog.getExtraClassPathArraytoURLArray());

        Method getNumberReplicationsMethod     = simulationRunAssemblyClass.getMethod(METHOD_getNumberPlannedReplications);
        Method isSaveReplicationData           = simulationRunAssemblyClass.getMethod(METHOD_isSaveReplicationData); // TODO hook this up
        Method isPrintReplicationReportsMethod = simulationRunAssemblyClass.getMethod(METHOD_isPrintReplicationReports);
        Method isPrintSummaryReportMethod      = simulationRunAssemblyClass.getMethod(METHOD_isPrintSummaryReport);
        Method setVerboseMethod                = simulationRunAssemblyClass.getMethod(METHOD_setVerbose, boolean.class);
        Method isVerboseMethod                 = simulationRunAssemblyClass.getMethod(METHOD_isVerbose);
        Method setStopTimeMethod               = simulationRunAssemblyClass.getMethod(METHOD_setStopTime, double.class);
        Method getStopTimeMethod               = simulationRunAssemblyClass.getMethod(METHOD_getStopTime);

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

    protected void prepareAndStartAssemblySimulationRun() // formerly initRun
    {
        // Ignore multiple pushes of the sim run button
        mutex++;
        if (mutex > 1)
        {
            LOG.error("prepareAndStartAssemblySimulationRun() unable to commence because mututal-exclusion count mutex={}", mutex);
            return;
        }

        try // prepareAndStartAssemblySimulationRun()
        {
            // the follow-on initializations using ViskitGlobals and ViskitUserPreferencesDialog
            // must occur prior to threading and new RunSimulationClassLoader
////            basicAssembly.resetRunSimulationClassLoader(); // TODO wrong place for this, likely out of place
            
            basicAssembly.setWorkingDirectory(ViskitGlobals.instance().getProjectWorkingDirectory()); // TODO duplicate invocation?
            setClassPathUrlArray(ViskitUserPreferencesDialog.getExtraClassPathArraytoURLArray());
            
            // originally VGlobals().instance().getFreshClassLoader(), then moved into this class
            priorRunSimulationClassLoader = getRunSimulationClassLoader(); 
            
            // Now we are in the pure classloader realm where each basicAssembly run can be independent of any other
            simulationRunAssemblyClass    = priorRunSimulationClassLoader.loadClass(simulationRunAssemblyClass.getName());

//            // we want the constructor that passes in parameters as a Runnable
//            LOG.info("attempting prepareAndStartAssemblySimulationRun() simulationRunAssemblyClass.getDeclaredConstructor(parameterTypes)");
//            // https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/Class.html#getDeclaredConstructor(java.lang.Class...)
//            Class<?>[] parameterTypes = new Class[] { // TODO duplicative
//                File.class, // workingDirectory
//            };
//            // https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/lang/reflect/Constructor.html#newInstance(java.lang.Object...)
//            File workingDirectory = ViskitGlobals.instance().getProjectWorkingDirectory();
//            // note that parameter list of getDeclaredConstructor type must match newInstance being created
//            simulationRunAssemblyInstance = simulationRunAssemblyClass.getDeclaredConstructor(parameterTypes).newInstance(workingDirectory);
            simulationRunAssemblyInstance = simulationRunAssemblyClass.getDeclaredConstructor().newInstance(); // TODO needed?

            Method setOutputStreamMethod             = simulationRunAssemblyClass.getMethod(METHOD_setOutputStream, OutputStream.class);
            Method setNumberReplicationsMethod       = simulationRunAssemblyClass.getMethod(METHOD_setNumberPlannedReplications, int.class);
            Method setPrintReplicationReportsMethod  = simulationRunAssemblyClass.getMethod(METHOD_setPrintReplicationReports, boolean.class);
            Method setPrintSummaryReportMethod       = simulationRunAssemblyClass.getMethod(METHOD_setPrintSummaryReport, boolean.class);
            Method setSaveReplicationDataMethod      = simulationRunAssemblyClass.getMethod(METHOD_setSaveReplicationData, boolean.class);
            Method setEnableAnalystReports           = simulationRunAssemblyClass.getMethod(METHOD_setEnableAnalystReports, boolean.class);
            Method setVerboseMethod                  = simulationRunAssemblyClass.getMethod(METHOD_setVerbose, boolean.class);
            Method setStopTimeMethod                 = simulationRunAssemblyClass.getMethod(METHOD_setStopTime, double.class);
            Method setVerboseReplicationNumberMethod = simulationRunAssemblyClass.getMethod(METHOD_setVerboseReplicationNumber, int.class);
            Method setPclNodeCacheMethod             = simulationRunAssemblyClass.getMethod(METHOD_setPclNodeCache, Map.class);
            Method addPropertyChangeListenerMethod   = simulationRunAssemblyClass.getMethod(METHOD_addPropertyChangeListener, PropertyChangeListener.class);

            // As of discussion held 09 APR 2015, resetting the RNG seed state
            // is not necessary for basic Viskit operation.  Pseudo random
            // independence is guaranteed from the default RNG (normally the
            // MersenneTwister)

            // *** Resetting the RNG seed state ***
            // NOTE: This is currently disabled as the resetSeedStateCB is not
            // enabled nor visible
            if (simulationRunPanel.resetSeedStateCB.isSelected()) 
            {
                Class<?> randomVariateFactoryClass  = priorRunSimulationClassLoader.loadClass(ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS);
                Method getDefaultRandomNumberMethod = randomVariateFactoryClass.getMethod(""); // simkit randomVariateFactoryClass semantics for empty string
                Object defaultRandomNumberMethod = getDefaultRandomNumberMethod.invoke(null);

                Method getSeedsMethod = defaultRandomNumberMethod.getClass().getMethod("getSeeds"); // simkit method
                seedsArray = (long[]) getSeedsMethod.invoke(defaultRandomNumberMethod);

                Class<?> randomNumberClass = priorRunSimulationClassLoader.loadClass(ViskitStatics.RANDOM_NUMBER_CLASS);
                Method setSeedsMethod = randomNumberClass.getMethod("setSeeds", long[].class); // simkit method
                setSeedsMethod.invoke(defaultRandomNumberMethod, seedsArray);

                // TODO: We can also call RNG.resetSeed() which recreates the
                // seed state (array) from the original seed
            }
            // *** End RNG seed state reset ***

            textAreaOutputStream = new viskit.assembly.TextAreaOutputStream(simulationRunPanel.outputStreamTA, 16*1024);

            // TODO update panel values for Simulation Run

            setOutputStreamMethod.invoke(simulationRunAssemblyInstance, textAreaOutputStream); // redirect output
            setNumberReplicationsMethod.invoke(simulationRunAssemblyInstance, Integer.valueOf(simulationRunPanel.numberReplicationsTF.getText().trim()));
            setPrintReplicationReportsMethod.invoke(simulationRunAssemblyInstance, simulationRunPanel.printReplicationReportsCB.isSelected());
            setPrintSummaryReportMethod.invoke(simulationRunAssemblyInstance, simulationRunPanel.printSummaryReportsCB.isSelected());

            setSaveReplicationDataMethod.invoke(simulationRunAssemblyInstance, simulationRunPanel.saveReplicationDataCB.isSelected());
            setEnableAnalystReports.invoke(simulationRunAssemblyInstance, simulationRunPanel.analystReportCB.isSelected());

            // Allow panel values to override XML set values
            setStopTimeMethod.invoke(simulationRunAssemblyInstance, getStopTime());
            setVerboseMethod.invoke (simulationRunAssemblyInstance, getVerbose());

            setVerboseReplicationNumberMethod.invoke(simulationRunAssemblyInstance, getVerboseReplicationNumber());
            setPclNodeCacheMethod.invoke(simulationRunAssemblyInstance, ((AssemblyModelImpl) ViskitGlobals.instance().getActiveAssemblyModel()).getNodeCache());
            addPropertyChangeListenerMethod.invoke(simulationRunAssemblyInstance, this);
            
            assemblyRunnable = (Runnable) simulationRunAssemblyInstance;

            // Simulation run starts the runnable assembly in a separate thread
            simulationRunThread = new Thread(assemblyRunnable);
            new SimulationRunMonitor(simulationRunThread).execute(); // commence thread
            // Simulation Run thread is now launched and will execute separately

            // Restore current thread context to Viskit's WorkingClassLoader prior to returning control
            if  (priorWorkingClassLoaderNoReset != null)
            {
                Thread.currentThread().setContextClassLoader(priorWorkingClassLoaderNoReset);
                LOG.info ("prepareAndStartAssemblySimulationRun() complete for " + basicAssembly.getFixedName());
                LOG.debug("restored currentThread contextClassLoader='" + priorWorkingClassLoaderNoReset.getName() + "'");
            }
            else LOG.error("prepareAssemblySimulationRun() complete, but priorWorkingClassLoaderNoReset is unexpectedly null");
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | ClassNotFoundException exception) 
        {
            LOG.error("prepareAssemblySimulationRun() reflection exception: " + exception.getMessage());
        }
        // the following redundant "catch (Exception ue)" is correct for handling reflection exceptions
        catch (Exception ue) // any other unexpected exceptions?? sometimes strange things happen with reflection and threading
        {
            LOG.error("prepareAssemblySimulationRun() reflection uncaught exception: " + ue);
        }
    }

    /** Thread to perform simulation run and end of run cleanup items */
    public class SimulationRunMonitor extends SwingWorker<Void, Void> 
    {
        Thread simulationRunMonitorThread;

        public SimulationRunMonitor(Thread newSimulationRunMonitorThread) 
        {
               simulationRunMonitorThread = newSimulationRunMonitorThread;
        }

        @Override
        public Void doInBackground() 
        {
            setProgress(0);

            simulationRunMonitorThread.start(); // commence thread
            try {
                LOG.info("SimulationRunMonitor.doInBackground() now commencing simulationRunMonitorThread.join()");
                simulationRunMonitorThread.join();
            } 
            catch (InterruptedException ex) {
                LOG.error("SimulationRunMonitor.doInBackground() exception: {}", ex);
//                ex.printStackTrace();
            }
            return null;
        }

        /** Perform simulation stop and reset calls.
         *  Java javadoc: "Executed on the Event Dispatch Thread after the doInBackground method is finished."
         */
        @Override
        public void done()
        {
            setProgress(100); // TODO what happens here, exactly? SwingWorker method
            mutex--; // this thread is complete, decrement mutual exclusion (mutex) safety-net counter
            
            // TODO perform analyst report work here, avoid reflection!!
            

            // Grab the temp Analyst Report and signal the AnalystReportFrame
            try {
                Method getAnalystReportMethod = simulationRunAssemblyClass.getMethod(METHOD_getAnalystReport);
                analystReportTempFile = (String) getAnalystReportMethod.invoke(simulationRunAssemblyInstance);
            } 
            catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                LOG.error("SimulationRunMonitor.done(): {}", ex);
                return;
            }
            
            vcrButtonPressDisplayUpdate(SimulationState.DONE); // also sets simulationState

            System.out.println("+------------------------------+"); // output goes to console TextArea
            System.out.println("| Simulation replications DONE |");
            System.out.println("+------------------------------+");

            notifyUserAnalystReportReady(); // saves temp report

// TODO old/contrary? mistakenly resets status label
//            simulationRunPanel.stateMachineMessageLabel.setText("<html><body><p><b>Replications all complete.</b>\n</p></body></html>");
//            assemblySimulationRunStopListener.actionPerformed(null);
                
            LOG.info("SimulationRunMonitor.done() is complete");
            
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
    public int getVerboseReplicationNumber()
    {
        int replicationNumber = -1;
        try {
            replicationNumber = Integer.parseInt(simulationRunPanel.verboseReplicationNumberTF.getText().trim());
        } 
        catch (NumberFormatException ex) 
        {
            // don't report problem with default TF value
            if (!ex.getMessage().contains(VERBOSE_REPLICATION_NUMBER_DEFAULT_HINT))
                LOG.error("getVerboseReplicationNumber() exception: " + ex.getMessage());
        }
        return replicationNumber;
    }

    class RunResumeListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent) 
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            if (!ViskitGlobals.instance().getSimulationRunPanel().vcrRunResumeButton.isEnabled())
            {
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Simulation controller button selection ignored", "Run/resume button not currently enabled");
                return;
            }
            
//          simulationRunPanel.vcrStartTimeTF.setText("0.0");        // because no pausing ?? TODO check
            
            vcrButtonPressDisplayUpdate(SimulationState.RUN_RESUME); // resolves RUN or RESUME
            
            Schedule.setSingleStep(false);        // simkit ensure no longer in single-step mode
            Schedule.setPauseAfterEachEvent(false); // simkit ensure no longer in single-step mode
            
            if (simulationRunPanel.outputStreamTA.getText().isBlank() ||
                simulationRunPanel.outputStreamTA.getText().trim().equals(INITIAL_SIMULATION_RUN_HINT)) // TODO fix
            {
                simulationRunPanel.outputStreamTA.setText(SIMULATION_RUN_PANEL_TITLE);
            }
            
            prepareAndStartAssemblySimulationRun(); // keep this last, launches thread
        }
    }

    class PauseStepListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent) // TODO development in progress, not fully tested
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            if (!ViskitGlobals.instance().getSimulationRunPanel().vcrPauseStepButton.isEnabled())
            {
                
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Simulation controller button selection ignored", "Pause/step button not currently enabled");
                return;
            }
            
            // the following method threaded basicAssembly includes simkit.Schedule.stopSimulation();
            basicAssembly.setPauseSimulationRun(true);
            
            try // PauseStepListener
            {
                if (simulationRunAssemblyInstance != null) 
                {
                    if (getSimulationState() != SimulationState.PAUSE)
                    {
                        vcrButtonPressDisplayUpdate(SimulationState.PAUSE);
                        
                        // TODO duplicative?
                        
                        // Pause (from Run mode)
                        Schedule.setSingleStep(true);
                        Schedule.setPauseAfterEachEvent(true);
                        Schedule.pause();
                        
                        // TODO runaway thread; is any action needed at this point?
                        // Likely problem:  pause event is not being recieved in threaded event loop, rather in between replications
//                        Schedule.startSimulation();
                    }
                    else
                    {
                        vcrButtonPressDisplayUpdate(SimulationState.STEP);
                        
                        // Step (while in single-step mode)
                        // TODO run one step and return
                        Schedule.startSimulation(); // TODO is this correct method for single stepping?
                    }
                    
                    if (priorRunSimulationClassLoader == null)
                    {
                        priorRunSimulationClassLoader = getRunSimulationClassLoader(); // can occur if stepping prior to running
                        
                        LOG.debug("StepListener actionPerformed() currentThread contextClassLoader=" + priorRunSimulationClassLoader.getName());
                    }
                    
                    if (priorRunSimulationClassLoader != null) // TODO necessary?
                        Thread.currentThread().setContextClassLoader(priorRunSimulationClassLoader);

                    // TODO where is this method? "setStepRun" ... not certain it turned into METHOD_setSingleStep
                    Method setStepRunMethod = simulationRunAssemblyClass.getMethod(METHOD_setSingleStep, boolean.class);
                    setStepRunMethod.invoke(simulationRunAssemblyInstance, true);

//                    if (textAreaOutputStream != null)
//                        textAreaOutputStream.stop();
//
//                    mutex--;
                }
//              Schedule.coldReset(); // simkit

                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if ((classLoader != null) && !classLoader.equals(priorWorkingClassLoaderNoReset))
                {
                    Thread.currentThread().setContextClassLoader(priorWorkingClassLoaderNoReset);
                    LOG.info("StepListener actionPerformed(), rejoin regular thread");
                    LOG.info("      currentThread contextClassLoader=" + priorWorkingClassLoaderNoReset.getName());
                } // rejoin regular thread

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
        }
    }

    /** Restores the Viskit default ClassLoader after an Assembly compile and run.
     *  Performs a Schedule.coldReset() to clear Simkit for the next run.
     */
    public class StopListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            if (!ViskitGlobals.instance().getSimulationRunPanel().vcrStopButton.isEnabled())
            {
                
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Simulation controller button selection ignored", "Stop button not currently enabled");
                return;
            }
            
            if (mutex > 0)
                mutex--;
            
            // the following method threaded basicAssembly includes simkit.Schedule.stopSimulation();
            basicAssembly.setStopSimulationRun(true);
            
            try // StopListener
            {
                if (simulationRunAssemblyInstance != null) 
                {
                    Thread.currentThread().setContextClassLoader(priorRunSimulationClassLoader);
                    LOG.info("StopListener actionPerformed() currentThread contextClassLoader=" + priorRunSimulationClassLoader.getName());
                    
                    // TODO where is this method? "setStopRun" is likely METHOD_setStopSimulationRun
                    Method setStopRunMethod = simulationRunAssemblyClass.getMethod(METHOD_setStopSimulationRun, boolean.class);
                    setStopRunMethod.invoke(simulationRunAssemblyInstance, true);

//////                    if (textAreaOutputStream != null)
//////                    {
//////                        textAreaOutputStream.stop(); // TODO misleading?  loses output if thread continues
////////                        textAreaOutputStream.
//////                    }
                }
                else
                {
                    LOG.error("StopListener.actionPerformed(" + actionEvent + ") unable to find simulationRunAssemblyInstance");
                }
                vcrButtonPressDisplayUpdate(SimulationState.STOP);
                Schedule.coldReset(); // simkit

                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if ((classLoader != null) && !classLoader.equals(priorWorkingClassLoaderNoReset))
                {
                    Thread.currentThread().setContextClassLoader(priorWorkingClassLoaderNoReset);
                    LOG.info("StopListener actionPerformed(), rejoin regular thread");
                    LOG.info("      currentThread contextClassLoader=" + priorWorkingClassLoaderNoReset.getName());
                } // rejoin regular thread
                else
                {
                    LOG.error("StopListener.actionPerformed(" + actionEvent + ") classLoader restoration problem");
                }
            } 
            catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) 
            {
                // Some screwy stuff can happen here if a user jams around with
                // the Prepare Assembly Run button and tabs back and forth
                // between the Assembly editor and the Assembly runner panel, but it
                // won't impede a correct Assembly run.  Catch the
                // IllegalArgumentException and move on.
//                LOG.error(ex);
//                ex.printStackTrace();

                 LOG.error("StopListener.actionPerformed(" + actionEvent + ") exception: {}", exception);
            }
        }
    }

    class RewindListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent) 
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            if (!ViskitGlobals.instance().getSimulationRunPanel().vcrRewindButton.isEnabled())
            {
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Simulation controller button selection ignored", "Rewind button not currently enabled");
                return;
            }
            
            String title, message;
            title = "Rewind, clear console, and reset simulation?";
            message = "<html><p align='center'>Are you sure that you want to reset this simulation?</p><br/>";
            int returnValue = ViskitGlobals.instance().getMainFrame().genericAskYesNo(title, message);
            if (returnValue == JOptionPane.YES_OPTION)
            {
                vcrButtonPressDisplayUpdate(SimulationState.REWIND);
                
                Schedule.reset(); // simkit reset event list
            
                AssemblyController assemblyController = ViskitGlobals.instance().getAssemblyController();
                assemblyController.prepareSimulationRunner();

                // TODO reset simulation clock
                try {
                    SwingUtilities.invokeLater(() -> {
                        vcrButtonPressDisplayUpdate(SimulationState.READY);
                    });
                }
                catch (Exception ex) {
                    LOG.error("RewindListener.actionPerformed(" + actionEvent.toString() + ") exception: " + ex.getMessage());
                }
            }
        }
    }

    /** Allow for overriding XML set value via the Run panel setting */
    class VerboseListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent) 
        {
            if (basicAssembly == null) {return;}
            basicAssembly.setVerbose(((AbstractButton) actionEvent.getSource()).isSelected());
        }
    }

    private JFileChooser saveChooser;

    class SaveListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            if (saveChooser == null) {
                saveChooser = new JFileChooser(ViskitGlobals.instance().getViskitProject().getProjectDirectory());
            }
            File consoleFile = ViskitGlobals.instance().getEventGraphViewFrame().getUniqueName("SimulationRunOutput.txt", saveChooser.getCurrentDirectory());
            saveChooser.setSelectedFile(consoleFile);
            saveChooser.setDialogTitle("Save Console Output");

            int returnValue = saveChooser.showSaveDialog(null);
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                return;
            }

            consoleFile = saveChooser.getSelectedFile();
            if (consoleFile.exists())
            {
                returnValue = JOptionPane.showConfirmDialog(null, "File exists.  Overwrite?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try (Writer bufferedWriter = new BufferedWriter(new FileWriter(consoleFile))) {
                bufferedWriter.write(simulationRunPanel.outputStreamTA.getText());
            } catch (IOException e1) {
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, "I/O Error,", e1.getMessage() );
            }
        }
    }

    private void notifyUserAnalystReportReady()
    {
        if (analystReportTempFile == null) 
        {
            // No report to print, TODO unexpected
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

    /** Simulation state machine: states plus transitions,
     *  see Viskit Assembly Simulation State Machine diagram */
    public enum SimulationState {
        READY, RUN_RESUME, RUN, RESUME, PAUSE_STEP, PAUSE, STEP, STOP, REWIND, DONE, INACTIVE
    }

    /** Simulation State Machine transition logic, initiated by user button selection.
     * @param newEvent triggered by button push
     */
    public final void vcrButtonPressDisplayUpdate(SimulationState newEvent) 
    {
        SimulationState newState = newEvent; // save original value
        
        // disambiguate RUN_RESUME modes
        if (newEvent.equals(SimulationState.RUN_RESUME))
        {
            if  (getSimulationState() == SimulationState.PAUSE) // check prior state to determine transition mode
                 newState =  SimulationState.RESUME;  // previously PAUSE, now resume running
            else newState =  SimulationState.RUN;     // previously READY, now begin running
        }
        // disambiguate PAUSE_STEP modes
        if (newEvent.equals(SimulationState.PAUSE_STEP))
        {
            if  (getSimulationState() == SimulationState.PAUSE) // check prior state to determine transition mode
                 newState =  SimulationState.STEP;  // previously PAUSE,  now perform single step
            else newState =  SimulationState.PAUSE; // previously RUN,    now initial pause
        }
                
        setSimulationState(newState); // update overall state
        simulationRunPanel.vcrButtonStatusLabel.setText(" " + newState.name());
        
        // debug
//        simulationRunPanel.outputStreamTA.setText(
//                simulationRunPanel.outputStreamTA.getText() + "\n" +
//                "User button pressed: " + vcrState          + "\n");

        switch (getSimulationState()) 
        {
            case READY: // initialization
                 simulationRunPanel.vcrRewindButton      .setEnabled(false);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(true);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(true);
                 simulationRunPanel.vcrStopButton        .setEnabled(false);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(true);
                 LOG.info("vcrButtonPressDisplayUpdate({})", newEvent);
                 break;
                
            case REWIND:
                 simulationRunPanel.vcrRewindButton      .setEnabled(false);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(true);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(false); 
                 simulationRunPanel.vcrStopButton        .setEnabled(false);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(true);
                 LOG.info("vcrButtonPressDisplayUpdate({})", newEvent);
                 break;
                
            case RUN_RESUME:
            case RUN:
            case RESUME:
                 simulationRunPanel.vcrRewindButton      .setEnabled(false);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(false);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(true);
                 simulationRunPanel.vcrStopButton        .setEnabled(true);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(false);   // avoid clearing console accidentally while running
                 LOG.info("vcrButtonPressDisplayUpdate({})", newState); // newEvent);
                 break;
                
            case PAUSE:
            case STEP:
                 simulationRunPanel.vcrRewindButton      .setEnabled(true);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(true);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(true);
                 simulationRunPanel.vcrStopButton        .setEnabled(true);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(true);
                 LOG.info("vcrButtonPressDisplayUpdate({})", newState); // newEvent);
                 break;
                
            case STOP:
                 simulationRunPanel.vcrRewindButton      .setEnabled(false);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(false);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(false);
                 simulationRunPanel.vcrStopButton        .setEnabled(false);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(true);
                 LOG.info("vcrButtonPressDisplayUpdate({})", newEvent);
                 break;
                
            case DONE:
                 simulationRunPanel.vcrRewindButton      .setEnabled(true);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(false);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(false);
                 simulationRunPanel.vcrStopButton        .setEnabled(false);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(true);
                 LOG.info("vcrButtonPressDisplayUpdate({})", newEvent);
                 break;
                 
            case INACTIVE: // no assembly is ready
                 simulationRunPanel.vcrRewindButton      .setEnabled(false);
                 simulationRunPanel.vcrRunResumeButton   .setEnabled(false);
                 simulationRunPanel.vcrPauseStepButton   .setEnabled(false);
                 simulationRunPanel.vcrStopButton        .setEnabled(false);
                 simulationRunPanel.vcrClearConsoleButton.setEnabled(false);
                 LOG.info("vcrButtonPressDisplayUpdate({})", newEvent);
                 break;
                
            default:
                 LOG.warn("*** Unrecognized vcrButtonListener(event=" + newEvent + ")");
                 break;
        }
        // now that buttons enabled/disabled are all up to date, can update corresponding menu items to match
        updateSimulationControllerButtonsMenu();
        
        logSimulationRunState(); // development diagnostics
    }
    /** diagnostic utility
     * @param booleanValue value of interest
     * @return true "on" or false "off"
     */
    private String isOnOff(boolean booleanValue)
    {
        if  (booleanValue)
             return "on ";
        else return "off";
    }

    /** LOG simulationState and simkitState diagnostics */
    public void logSimulationRunState()
    {
        String simkitState;
        if      (Schedule.isRunning())
                 simkitState = "Running    simTime=" + Schedule.getSimTime();
        else if (Schedule.isSingleStep())
                 simkitState = "SingleStep simTime=" + Schedule.getSimTime();
        else if (Schedule.getSimTime() == 0.0)
                 simkitState = "(inactive)"; // likely between replications
        else
                 simkitState = "(unknown)  simTime=" + Schedule.getSimTime();
        LOG.info("logSimulationRunState()" +
                   " play=" + isOnOff(simulationRunPanel.vcrRunResumeButton.isEnabled()) +
                   " step=" + isOnOff(simulationRunPanel.vcrPauseStepButton.isEnabled()) +
                   " stop=" + isOnOff(simulationRunPanel.vcrStopButton.isEnabled()) +
                 " rewind=" + isOnOff(simulationRunPanel.vcrRewindButton.isEnabled()) +
                  " clear=" + isOnOff(simulationRunPanel.vcrClearConsoleButton.isEnabled()) +
                  "\n     " + // log readability
        " simulationState=" + getSimulationState().name() +
            " simkitState=" + simkitState
        );
    }

    private void buildMenus() // Simulation Run
    {
        myMenuBar = new JMenuBar();
//      JMenu fileMenu = new JMenu("File");
        simulationRunMenu = new JMenu("Simulation Run");
        simulationRunMenu.setToolTipText("Simulation Run performs multiple replications of a compiled Assembly model");
        simulationRunMenu.setMnemonic('S');
        
        simulationButtonsMenu = new JMenu("Simulation controller buttons");
        simulationButtonsMenu.setEnabled(false); // initial condition while SimulationState is INACTIVE
        rewindButtonMI = new JMenuItem("Rewind");
        rewindButtonMI.setMnemonic('R');
        rewindButtonMI.setToolTipText("Reset the simulation run");
       runButtonMI = new JMenuItem("Run/Resume");
        runButtonMI.setMnemonic('R');
        runButtonMI.setToolTipText("Run or resume the simulation replications");
       stepButtonMI = new JMenuItem("Pause/Step");
        stepButtonMI.setMnemonic('S');
        stepButtonMI.setToolTipText("Pause current replication, or single step the next replication");
       stopButtonMI = new JMenuItem("Stop");
        stopButtonMI.setMnemonic('S');
        stopButtonMI.setToolTipText("Stop the simulation replications");
        
        rewindButtonMI.addActionListener(new RewindListener());
           runButtonMI.addActionListener(new RunResumeListener());
          stepButtonMI.addActionListener(new PauseStepListener());
          stopButtonMI.addActionListener(assemblySimulationRunStopListener); // = new StopListener());
        
        simulationButtonsMenu.add(rewindButtonMI);
        simulationButtonsMenu.add(   runButtonMI);
        simulationButtonsMenu.add(  stepButtonMI);
        simulationButtonsMenu.add(  stopButtonMI);
        simulationRunMenu.add(simulationButtonsMenu);
                
        clearAllConsoleTextMI = new JMenuItem("Clear all console text");
        clearAllConsoleTextMI.setMnemonic('C');
        clearAllConsoleTextMI.setToolTipText("Clear the console text area");
        JMenuItem copyMI = new JMenuItem("Copy console text selection");
        copyMI.setMnemonic('C');
        copyMI.setToolTipText("Copy simulation run console output");
        JMenuItem saveMI = new JMenuItem("Save console text to file");
        saveMI.setMnemonic('S');
        saveMI.setToolTipText("Save simulation run console output to a file");
        JMenuItem selectAllMI = new JMenuItem("Select all console text");
        selectAllMI.setMnemonic('S');
        selectAllMI.setToolTipText("Select all text in the console log");
        JMenuItem viewConsoleOutputMI = new JMenuItem("View console output in text editor");
        viewConsoleOutputMI.setMnemonic('V');
        viewConsoleOutputMI.setToolTipText("Directly launch console output to text editor");
        JMenuItem viewLogsDirectoryMI = new JMenuItem("View simulation logs directory");
        viewLogsDirectoryMI.setMnemonic('V');
        viewLogsDirectoryMI.setToolTipText("View simulation logs directory in system");

        clearAllConsoleTextMI.addActionListener(new ClearConsoleListener());
                       copyMI.addActionListener(new CopyConsoleTextListener());
                       saveMI.addActionListener(new SaveListener());
                  selectAllMI.addActionListener(new SelectAllConsoleTextListener());
          viewConsoleOutputMI.addActionListener(new ViewConsoleListener());
          viewLogsDirectoryMI.addActionListener(new ViewLogsDirectoryListener());

        simulationRunMenu.add(clearAllConsoleTextMI);
        simulationRunMenu.add(copyMI);
        simulationRunMenu.add(saveMI);
        simulationRunMenu.add(selectAllMI);
        simulationRunMenu.add(viewConsoleOutputMI);
        simulationRunMenu.add(viewLogsDirectoryMI);

        if (ViskitGlobals.instance().getMainFrame().hasOriginalModalMenus())
        {
        getSimulationRunMenu().addSeparator();
        getSimulationRunMenu().add(new JMenuItem("Viskit User Preferences"));
        }
        myMenuBar.add(getSimulationRunMenu());

        // No edit functionality needed for SimulationRun panel
//        JMenu editMenu = new JMenu("Edit");
//        editMenu.add(copyMI);
//        editMenu.add(selectAllMI);
//        editMenu.add(clearAllMI);
//        myMenuBar.add(editMenu);
    }

    class CopyConsoleTextListener implements ActionListener 
    {
        @Override
        public void actionPerformed(ActionEvent e) 
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            String selectedText = simulationRunPanel.outputStreamTA.getSelectedText();
            StringSelection stringSelection = new StringSelection(selectedText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);
        }
    }

    class SelectAllConsoleTextListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            simulationRunPanel.outputStreamTA.requestFocus();
            simulationRunPanel.outputStreamTA.selectAll();
        }
    }

    public class ClearConsoleListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            // TODO vcrButtonPressDisplayUpdate(SimulationState._); ... not appropriate, ClearConsole is not a state
            
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            if (!ViskitGlobals.instance().getSimulationRunPanel().vcrClearConsoleButton.isEnabled())
            {
                ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Simulation controller button selection ignored", "Clear console button not currently enabled");
                return;
            }
                
            int returnValue = JOptionPane.showConfirmDialog(ViskitGlobals.instance().getSimulationRunPanel(), "Are you sure?", "Confirm clearing all console information", JOptionPane.YES_NO_OPTION);
            if (returnValue == JOptionPane.YES_OPTION) 
            {
                simulationRunPanel.outputStreamTA.setText("");
                LOG.info("ClearConsoleListener: clear console");
            }
        }
    }

    class ViewConsoleListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ViskitGlobals.instance().selectSimulationRunTab(); // ensure correct tab selected if invoked by menu item
            
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

    class ViewLogsDirectoryListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            File logsDirectory =ViskitUserConfiguration.VISKIT_LOGS_DIR;
            try {
                Desktop.getDesktop().open(logsDirectory);
            }
            catch (IOException ioe)
            {
                LOG.error("error opening logs directory\n      {}", logsDirectory);
            }
        }
    }

    private final String  initialValue = "awaiting initialization by Assembly Editor..."; // TODO unscramble logic
    private StringBuilder currentTitle = new StringBuilder();

    public void doTitle(String newName)
    {
        currentTitle.setLength(0); // reset
       
        if (newName != null && newName.length() > 0)
        { 
             currentTitle = currentTitle.append(": ").append(newName);
        }
        else currentTitle.append(initialValue);

        if (titleListener != null) {
            titleListener.setTitle(currentTitle.toString(), titleKey);
        }
    }
    private TitleListener titleListener;
    private int titleKey;

    public void setTitleListener(TitleListener listener, int key) {
        titleListener = listener;
        titleKey = key;
    }

    StringBuilder nowRunningMessageBuilder = new StringBuilder("<html><body><font color=black>\n" + "<p><b>Now Running Replication ");

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        LOG.debug(event.getPropertyName());

        if (event.getPropertyName().equals("replicationNumber")) 
        {
            int beginLength = nowRunningMessageBuilder.length();
            nowRunningMessageBuilder.append(event.getNewValue());
            simulationRunPanel.setNumberOfReplications(Integer.parseInt(event.getNewValue().toString()));
            nowRunningMessageBuilder.append(" of ");
            nowRunningMessageBuilder.append(Integer.parseInt(simulationRunPanel.numberReplicationsTF.getText()));
            nowRunningMessageBuilder.append("...</b>\n");
            nowRunningMessageBuilder.append("</font></p><br></body></html>\n");
            simulationRunPanel.stateMachineMessageLabel.setText(nowRunningMessageBuilder.toString());

            // reset display string in preparation for the next replication output
            nowRunningMessageBuilder.delete(beginLength, nowRunningMessageBuilder.length());
        }
    }

    /**
     * @return the simulationRunMenu
     */
    public JMenu getSimulationRunMenu() {
        return simulationRunMenu;
    }

    /** 
     * The RunSimulationClassLoader is specific to Assembly running in that it is
     * pristine from the ViskitApplicationClassLoader in use for normal Viskit operations.
     * TODO out of place? Warning: note that this method also resets ContextClassLoader for the current thread.
     * @see ViskitGlobals.getWorkingClassLoader()
     * @return a pristine class loader for Assembly runs
     */
    public ClassLoader getRunSimulationClassLoader() // formerly "Fresh" boot loader
    {
        try
        {
            if (runSimulationClassLoader == null)
            {
                /* Not sure if this breaks the "RunSimulation" classloader for assembly
                running, but in post JDK8 land, the Java Platform Module System
                (JPMS) rules and as such we need to retain certain modules, i.e.
                java.sql. With retaining the boot class loader, not sure if that
                causes sibling classloader static variables to be retained. In
                any case we must retain the original bootloader as it has
                references to the loaded module system.
                 */
                LocalBootLoader localBootLoader = new LocalBootLoader(classPathUrlArray, 
                    // the parent of the platform loader should be the internal boot loader
                    ClassLoader.getPlatformClassLoader(), 
                    ViskitGlobals.instance().getProjectWorkingDirectory(), // workingDirectory, // do not use singleton ?? referencing by ViskitGlobals here!
                    "RunSimulationClassLoader"); 
                // Allow Assembly files in the ClassLoader
                runSimulationClassLoader = localBootLoader.initialize(true);
                // Set a RunSimulation ClassLoader for this thread to be free of any static
                // state set from the Viskit WorkingClassLoader
                Thread.currentThread().setContextClassLoader(runSimulationClassLoader);
                // TODO ensure no threading and singleton issues while inside ViskitGlobals?
                LOG.info("getRunSimulationClassLoader() currentThread\n      " + 
                         "contextClassLoader=" + runSimulationClassLoader.getName() + " and created new ClassLoader for\n      " +
                         ViskitGlobals.instance().getProjectRootDirectoryPath());
            }
        }
        catch (Exception e)
        {
            LOG.error("getRunSimulationClassLoader() exception, returning null: " + e);
        }
        if (runSimulationClassLoader == null)
        {
            LOG.error("getRunSimulationClassLoader() ran without exception but returned null");
        }
        return runSimulationClassLoader;
    }
    
    public void resetRunSimulationClassLoader() {
        runSimulationClassLoader = null;
        LOG.info("resetRunSimulationClassLoader() complete"); // TODO threading issue?
    }

    /**
     * @return the classPathUrlArray
     */
    private URL[] getClassPathUrlArray() {
        return classPathUrlArray;
    }

    /**
     * @param classPathUrlArray the classPathUrlArray to set
     */
    public void setClassPathUrlArray(URL[] classPathUrlArray) {
        this.classPathUrlArray = classPathUrlArray;
    }

    /**
     * @return the simulationState
     */
    public SimulationState getSimulationState() {
        return simulationState;
    }

    /**
     * @param newSimulationState the simulationState to set
     */
    public void setSimulationState(SimulationState newSimulationState) 
    {
        this.simulationState = newSimulationState;
        if (basicAssembly != null) // also update superclass
            basicAssembly.setSimulationState(newSimulationState);
    }

    /**
     * @return whether simulationState is active
     */
    public boolean isAssemblySimulationEnabled() {
        return !(simulationState == SimulationState.INACTIVE);
    }
    
    private void updateSimulationControllerButtonsMenu()
    {
        simulationButtonsMenu.setEnabled(isAssemblySimulationEnabled());
        rewindButtonMI.setEnabled(simulationRunPanel.vcrRewindButton.isEnabled());
           runButtonMI.setEnabled(simulationRunPanel.vcrRunResumeButton.isEnabled());
          stepButtonMI.setEnabled(simulationRunPanel.vcrPauseStepButton.isEnabled());
          stopButtonMI.setEnabled(simulationRunPanel.vcrStopButton.isEnabled());
 clearAllConsoleTextMI.setEnabled(simulationRunPanel.vcrClearConsoleButton.isEnabled());
    }

}  // end class file InternalAssemblyRunner.java
