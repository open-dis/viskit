/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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

import viskit.view.SimulationRunPanel;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.swing.*;
import org.apache.log4j.Logger;
import simkit.Schedule;
import viskit.util.TitleListener;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.assembly.BasicAssembly;
import viskit.assembly.JTextAreaOutputStream;
import viskit.model.AnalystReportModel;
import viskit.model.AssemblyModelImpl;

/** Controller for the Simulation Run panel
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

    static final Logger LOG = LogUtils.getLogger(InternalAssemblyRunner.class);

    /** The name of the assembly to run */
    String assemblyClassName;
    SimulationRunPanel runPanel;
    ActionListener closer, saveListener;
    JMenuBar myMenuBar;
    BufferedReader backChan;
    Thread simulationRunThread;
    SimulationRunThreadMonitor swingThreadMonitor;
    PipedOutputStream pos;
    PipedInputStream pis;
    BasicAssembly assembly;

    /** external runner saves a file */
    private String analystReportTempFile = null;
    FileOutputStream fos;
    FileInputStream fis;

    /** The assembly to be run from java source */
    Class<?> assemblyClass;

    /** Instance of the assembly to run from java source */
    Object assemblyInstance;
    private static int mutex = 0;
    private ClassLoader lastLoaderNoReset;
    private ClassLoader lastLoaderWithReset;

    /** Captures the original RNG seed state */
    private long[] seeds;
    private StopListener assemblyRunStopListener;

    private final String namePrefix = "Viskit Assembly Runner";
    private String currentTitle = namePrefix;

    StringBuilder npsString = new StringBuilder("<html><body><font color=black>\n" + "<p><b>Now running Replication ");

    /**
     * The internal logic for the Assembly Runner panel
     * @param analystReportPanelVisible if true, the analyst report panel will be visible
     */
    public InternalAssemblyRunner(boolean analystReportPanelVisible) {

        saveListener = new SaveListener();
		String assemblyName = "";
		if (ViskitGlobals.instance().getActiveAssemblyModel() != null)
			   assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getMetaData().name;

        // TODO NOTE:
        // Don't supply rewind or pause buttons on VCR, not hooked up, or working right.
        // false will enable all VCR buttons.  Currently, only start and stop work
        runPanel = new SimulationRunPanel(assemblyName, true, analystReportPanelVisible);
        buildMenus();
        runPanel.vcrStop.addActionListener(assemblyRunStopListener = new StopListener());
        runPanel.vcrPlay.addActionListener(new StartResumeListener());
        runPanel.vcrRewind.addActionListener(new RewindListener());
        runPanel.vcrStep.addActionListener(new StepListener());
        runPanel.vcrVerbose.addActionListener(new VerboseListener());
        runPanel.vcrStop.setEnabled(false);
        runPanel.vcrPlay.setEnabled(false);
        runPanel.vcrRewind.setEnabled(false);
        runPanel.vcrStep.setEnabled(false);
        twiddleButtons(OFF);

        // Viskit's current working ClassLoader
        lastLoaderNoReset = ViskitGlobals.instance().getWorkClassLoader();
    }

    public JComponent getRunnerPanel() {return runPanel;}

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public JMenuItem getQuitMenuItem() {
        return null;
    }

    /**
     * Pre-initialization this runner for a simulation run
     * @param params arguments to initialize the Simulation runner
     */
    public void preInitRun(String[] params) {

//        for (String s : params) {
//            LOG.info("VM argument is: " + s);
//        }

        assemblyClassName = params[AssemblyControllerImpl.EXEC_TARGET_CLASS_NAME];
        doTitle(assemblyClassName);

        runPanel.vcrSimulationTime.setText("0.0");

        // These values are from the XML file
        boolean defaultVerbose = Boolean.parseBoolean(params[AssemblyControllerImpl.EXEC_VERBOSE_SWITCH]);
        double defaultStopTime =  Double.parseDouble(params[AssemblyControllerImpl.EXEC_STOPTIME_SWITCH]);

        try {
            fillSimulationRunPanelWidgetsFromPreRunAssembly(defaultVerbose, defaultStopTime);
        } catch (Throwable throwable) {
            ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).messageToUser(
                    JOptionPane.ERROR_MESSAGE,
                    "Java Error",
                    "Error initializing Assembly:\n" + throwable.getMessage());
            twiddleButtons(OFF);
//            throwable.printStackTrace();
            return;
        }
        twiddleButtons(REWIND);
    }

    private void fillSimulationRunPanelWidgetsFromPreRunAssembly(boolean verbose, double stopTime) throws Throwable {

        assemblyClass = ViskitStatics.classForName(assemblyClassName);
        if (assemblyClass == null) {
            throw new ClassNotFoundException();
        }
        assemblyInstance = assemblyClass.newInstance();

        /* in order to resolve the assy as a BasicAssembly, it must be
         * loaded using the the same ClassLoader as the one used to compile
         * it.  Used in the VerboseListener within the working Viskit
         * ClassLoader
         */
        assembly = (BasicAssembly) assemblyInstance;

        Method getNumberReplications     = assemblyClass.getMethod("getNumberReplications");
        Method isSaveReplicationData     = assemblyClass.getMethod("isSaveReplicationData");
        Method isPrintReplicationReports = assemblyClass.getMethod("isPrintReplicationReports");
        Method isPrintSummaryReport      = assemblyClass.getMethod("isPrintSummaryReport");
        Method setVerbose                = assemblyClass.getMethod("setVerbose", boolean.class);
        Method isVerbose                 = assemblyClass.getMethod("isVerbose");
        Method setStopTime               = assemblyClass.getMethod("setStopTime", double.class);
        Method getStopTime               = assemblyClass.getMethod("getStopTime");

        runPanel.numberOfReplicationsTF.setText("" + (Integer) getNumberReplications.invoke(assemblyInstance));
        runPanel.saveRepDataCB.setSelected((Boolean) isSaveReplicationData.invoke(assemblyInstance));
        runPanel.printRepReportsCB.setSelected((Boolean) isPrintReplicationReports.invoke(assemblyInstance));
        runPanel.printSummReportsCB.setSelected((Boolean) isPrintSummaryReport.invoke(assemblyInstance));

        // Set the run panel according to what the assy XML value is
        setVerbose.invoke(assemblyInstance, verbose);
        runPanel.vcrVerbose.setSelected((Boolean) isVerbose.invoke(assemblyInstance));
        setStopTime.invoke(assemblyInstance, stopTime);
        runPanel.vcrStopTime.setText("" + (Double) getStopTime.invoke(assemblyInstance));
    }

    File tmpFile;
    RandomAccessFile rTmpFile;
    JTextAreaOutputStream textAreaOutputStream;

    protected void initRun() {

        // Prevent multiple pushes of the sim run button
        mutex++;
        if (mutex > 1) {
            return;
        }
        Runnable assemblyRunnable;

        try {

            ViskitGlobals.instance().resetFreshClassLoader();
            lastLoaderWithReset = ViskitGlobals.instance().getFreshClassLoader();

            // Test for Bug 1237
//            for (String s : ((LocalBootLoader)lastLoaderWithReset).getClassPath()) {
//                LOG.info(s);
//            }
//            LOG.info("\n");

            // Now we are in the pure classloader realm where each assy run can
            // be independent of any other
            assemblyClass = lastLoaderWithReset.loadClass(assemblyClass.getName());
            assemblyInstance = assemblyClass.newInstance();

            Method setOutputStream            = assemblyClass.getMethod("setOutputStream", OutputStream.class);
            Method setNumberReplications      = assemblyClass.getMethod("setNumberReplications", int.class);
            Method setSaveReplicationData     = assemblyClass.getMethod("setSaveReplicationData", boolean.class);
            Method setPrintReplicationReports = assemblyClass.getMethod("setPrintReplicationReports", boolean.class);
            Method setPrintSummaryReport      = assemblyClass.getMethod("setPrintSummaryReport", boolean.class);
            Method setEnableAnalystReports    = assemblyClass.getMethod("setEnableAnalystReports", boolean.class);
            Method setVerbose                 = assemblyClass.getMethod("setVerbose", boolean.class);
            Method setStopTime                = assemblyClass.getMethod("setStopTime", double.class);
            Method setVerboseReplication      = assemblyClass.getMethod("setVerboseReplication", int.class);
            Method setPclNodeCache            = assemblyClass.getMethod("setPclNodeCache", Map.class);
            Method addPropertyChangeListener  = assemblyClass.getMethod("addPropertyChangeListener", PropertyChangeListener.class);

            // As of discussion held 09 APR 2015, resetting the RNG seed state
            // is not necessary for basic Viskit operation.  Pseudo random
            // independence is guaranteed from the default RNG (normally the
            // MersenneTwister)

            // *** Resetting the RNG seed state ***
            // NOTE: This is currently disabled as the resetSeedStateCB is not
            // enabled nor visible
            if (runPanel.resetSeedStateCB.isSelected()) {

                Class<?> rVFactClass = lastLoaderWithReset.loadClass(ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS);
                Method getDefaultRandomNumber = rVFactClass.getMethod("getDefaultRandomNumber");
                Object rn = getDefaultRandomNumber.invoke(null);

                Method getSeeds = rn.getClass().getMethod("getSeeds");
                seeds = (long[]) getSeeds.invoke(rn);

                Class<?> rNClass = lastLoaderWithReset.loadClass(ViskitStatics.RANDOM_NUMBER_CLASS);
                Method setSeeds = rNClass.getMethod("setSeeds", long[].class);
                setSeeds.invoke(rn, seeds);

                // TODO: We can also call RNG.resetSeed() which recreates the
                // seed state (array) from the original seed
            }
            // *** End RNG seed state reset ***

            textAreaOutputStream = new JTextAreaOutputStream(runPanel.simulationOutputTA, 16*1024);

            setOutputStream.invoke(assemblyInstance, textAreaOutputStream);
            setNumberReplications.invoke(assemblyInstance, Integer.parseInt(runPanel.numberOfReplicationsTF.getText().trim()));
            setSaveReplicationData.invoke(assemblyInstance, runPanel.saveRepDataCB.isSelected());
            setPrintReplicationReports.invoke(assemblyInstance, runPanel.printRepReportsCB.isSelected());
            setPrintSummaryReport.invoke(assemblyInstance, runPanel.printSummReportsCB.isSelected());

            /* DIFF between OA3302 branch and trunk */
            setEnableAnalystReports.invoke(assemblyInstance, runPanel.analystReportCB.isSelected());
            /* End DIFF between OA3302 branch and trunk */

            // Allow panel values to override XML set values
            setStopTime.invoke(assemblyInstance, getStopTime());
            setVerbose.invoke(assemblyInstance, getVerbose());

            setVerboseReplication.invoke(assemblyInstance, getVerboseReplicationNumber());
            setPclNodeCache.invoke(assemblyInstance, ((AssemblyModelImpl)ViskitGlobals.instance().getActiveAssemblyModel()).getNodeCache());
            addPropertyChangeListener.invoke(assemblyInstance, this);
            assemblyRunnable = (Runnable) assemblyInstance;

            // Start the simulation run(s)
            simulationRunThread = new Thread(assemblyRunnable);
            new SimulationRunThreadMonitor(simulationRunThread).start();

            // Restore Viskit's working ClassLoader
            Thread.currentThread().setContextClassLoader(lastLoaderNoReset);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException | ClassNotFoundException ex) {
            LOG.error(ex);
        }
    }

	/**
	 * @return the simulationRunMenu
	 */
	public JMenu getSimulationRunMenu() {
		return simulationRunMenu;
	}

    /** Class to perform end of simulation run cleanup items */
    public class SimulationRunThreadMonitor extends Thread {

        Thread waitOn;

        public SimulationRunThreadMonitor(Thread toWaitOn) {
            waitOn = toWaitOn;
        }

        @Override
        public void run() {
            waitOn.start();
            try {
                waitOn.join();
            } catch (InterruptedException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            }

            end();

            // Grab the temp analyst report and signal the AnalystReportFrame
            try {
                Method getAnalystReport = assemblyClass.getMethod("getAnalystReport");
                analystReportTempFile = (String) getAnalystReport.invoke(assemblyInstance);
            } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                LOG.fatal(ex);
            }
            signalAnalystReportReady();
        }

        /** Perform simulation stop and reset calls */
        public void end() {
            System.out.println("Simulation ended");
            System.out.println("----------------");
            runPanel.npsLabel.setText("<html><body><p><b>Replications complete\n</b></p></body></html>");
            assemblyRunStopListener.actionPerformed(null);
        }
    }

    public ActionListener getAssemblyRunStopListener() {
        return assemblyRunStopListener;
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
            ret = Integer.parseInt(runPanel.verboseRepNumberTF.getText().trim());
        } catch (NumberFormatException ex) {
          //  ;
        }
        return ret;
    }
    PrintWriter pWriter;

    class StartResumeListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            runPanel.vcrSimulationTime.setText("0.0");    // because no pausing
            twiddleButtons(START);
            initRun();
        }
    }

    class StepListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            twiddleButtons(STEP);
        }
    }

    /** Restores the Viskit default ClassLoader after an Assembly compile and
     * run.  Performs a Schedule.coldReset() to clear Simkit for the next run.
     */
    public class StopListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {

                if (assemblyInstance != null) {

                    Thread.currentThread().setContextClassLoader(lastLoaderWithReset);

                    Method setStopRun = assemblyClass.getMethod("setStopRun", boolean.class);
                    setStopRun.invoke(assemblyInstance, true);

                    if (textAreaOutputStream != null)
                        textAreaOutputStream.kill();

                    mutex--;
                }

                Schedule.coldReset();

                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader != null && !loader.equals(lastLoaderNoReset))
                    Thread.currentThread().setContextClassLoader(lastLoaderNoReset);

            } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {

                // Some screwy stuff can happen here if a user jams around with
                // the initialize Assy run button and tabs back and forth
                // between the Assy editor and the Assy runner panel, but it
                // won't impede a correct Assy run.  Catch the
                // IllegalArgumentException and move on.
//                LOG.error(ex);
//                ex.printStackTrace();
            }

            twiddleButtons(STOP);
        }
    }

    class RewindListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            twiddleButtons(REWIND);
        }
    }

    /** Allow for overriding XML set value via the Run panel setting */
    class VerboseListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (assembly == null) {return;}
            assembly.setVerbose(((JCheckBox) e.getSource()).isSelected());
        }
    }

    private JFileChooser saveChooser;

    class SaveListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (saveChooser == null) {
                saveChooser = new JFileChooser(ViskitGlobals.instance().getCurrentViskitProject().getProjectRoot());
                saveChooser.setDialogTitle("Save Assembly Output");
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

            try {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(fil))) {
                    bw.write(runPanel.simulationOutputTA.getText());
                }
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage(), "I/O Error,", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    String returnedSimTime;

    private void signalAnalystReportReady() {
        if (analystReportTempFile == null) {
            // No report to print, not yet ready
            return;
        }

        AnalystReportController analystReportController = (AnalystReportController) ViskitGlobals.instance().getAnalystReportController();
        if (analystReportController != null) {
            analystReportController.setReportXML(analystReportTempFile);

            // Switch over to the analyst report tab if we have a report ready
            // for editing
            AnalystReportModel analystReportModel = (AnalystReportModel) analystReportController.getModel();
            if (analystReportModel != null && analystReportModel.isReportReady()) {
                analystReportController.mainTabbedPane.setSelectedIndex(analystReportController.mainTabbedPaneIdx);
                analystReportModel.setReportReady(false);
            }
        } else {
            JOptionPane.showMessageDialog(null, "<html><body><p align='center'>" +
                    "The Analyst Report tab has not been set to be visible.<br>To " +
                    "view on next Viskit opening, select File -> Settings -> " +
                    "Tab visibility -> Select Analyst report -> Close, then Exit" +
                    " the application.  On re-startup, it will appear.</p></body></html>",
                    "Analyst Report Panel not visible", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Allow for overriding XML set value via the Run panel setting
     * @return overridden XML set value via the Run panel setting
     */
    double getStopTime() {
        return Double.parseDouble(runPanel.vcrStopTime.getText());
    }

    /** Allow for overriding XML set value via the Run panel setting
     * @return overridden XML set value via the Run panel setting
     */
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
	
    private JMenu  simulationRunMenu;
	
    private void buildMenus() 
	{
        simulationRunMenu = new JMenu("Simulation Run");
		simulationRunMenu.setMnemonic(KeyEvent.VK_S);
        JMenuItem saveOutputMI = new JMenuItem("Save simulation output to text file");
        JMenuItem viewOutputMI = new JMenuItem("View simulation output in text editor");
		
        JMenu         editMenu = new JMenu("Edit");
        JMenuItem       copyMI = new JMenuItem("Copy");
        JMenuItem  selectAllMI = new JMenuItem("Select all");
        JMenuItem   clearAllMI = new JMenuItem("Clear all");

              saveOutputMI.addActionListener(saveListener);
        viewOutputMI.addActionListener(new ViewOutputListener());
              copyMI.addActionListener(new CopyListener());
         selectAllMI.addActionListener(new SelectAllListener());
          clearAllMI.addActionListener(new ClearListener());
		
   simulationRunMenu.setMnemonic(KeyEvent.VK_S);
	    saveOutputMI.setMnemonic(KeyEvent.VK_S); // submenu
		viewOutputMI.setMnemonic(KeyEvent.VK_V); // submenu
		
		    editMenu.setMnemonic(KeyEvent.VK_E);
		      copyMI.setMnemonic(KeyEvent.VK_C); // submenu
		 selectAllMI.setMnemonic(KeyEvent.VK_A); // submenu
		  clearAllMI.setMnemonic(KeyEvent.VK_L); // submenu

//        fileMenu.addSeparator();
//        fileMenu.add(new JMenuItem("Settings"));

        simulationRunMenu.add(saveOutputMI);
        simulationRunMenu.add(viewOutputMI);

         editMenu.add(copyMI);
         editMenu.add(selectAllMI);
         editMenu.add(clearAllMI);
		 
        myMenuBar = new JMenuBar();
        myMenuBar.add(simulationRunMenu);
        myMenuBar.add(editMenu);
    }

    class CopyListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String s = runPanel.simulationOutputTA.getSelectedText();
            StringSelection ss = new StringSelection(s);
            Clipboard clpbd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbd.setContents(ss, ss);
        }
    }

    class SelectAllListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            runPanel.simulationOutputTA.requestFocus();
            runPanel.simulationOutputTA.selectAll();
        }
    }

    class ClearListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            runPanel.simulationOutputTA.setText(null);
        }
    }

    class ViewOutputListener implements ActionListener
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

            String s = runPanel.simulationOutputTA.getText().trim();
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
                  LOG.error(ex1);
//                  ex1.printStackTrace();
              }
            }
        }
    }

    private TitleListener titleListener;
    private int titlekey;
	
    private void doTitle(String name) {
        if (name != null && name.length() > 0) {
            currentTitle = namePrefix + ": " + name;
        }

        if (titleListener != null) {
            titleListener.setTitle(currentTitle, titlekey);
        }
    }
    public void setTitleListener(TitleListener listener, int key) {
        titleListener = listener;
        titlekey = key;
//        doTitle(null); // don't blow away title while setting up listener, let it be set elsewhere
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        LOG.debug(evt.getPropertyName());

        if (evt.getPropertyName().equals("replicationNumber")) {
            int beginLength = npsString.length();
            npsString.append(evt.getNewValue());
            npsString.append(" of ");
            npsString.append(Integer.parseInt(runPanel.numberOfReplicationsTF.getText()));
            npsString.append("</b>\n");
            npsString.append("</font></p></body></html>\n");
            runPanel.npsLabel.setText(npsString.toString());

            // reset for the next replication output
            npsString.delete(beginLength, npsString.length());
        }
    }

}  // end class file InternalAssemblyRunner.java
