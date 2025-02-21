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
package viskit.assembly;

import edu.nps.util.GenericConversion;
import edu.nps.util.Log4jUtilities;
import edu.nps.util.TempFileManager;

import java.beans.PropertyChangeListener;

import java.io.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.text.DecimalFormat;

import java.util.*;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.Logger;

import simkit.BasicSimEntity;
import simkit.Named;
import simkit.ReRunnable;
import simkit.Schedule;
import simkit.SimEntity;
import simkit.SimEvent;
import simkit.random.RandomVariateFactory;
import simkit.stat.SampleStatistics;
import simkit.stat.SavedStats;
import simkit.stat.SimpleStatsTally;

import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitConfigurationStore;
import viskit.doe.LocalBootLoader;
import viskit.model.AnalystReportModel;

import viskit.model.AssemblyNode;

import viskit.reports.ReportStatisticsConfiguration;
import viskit.view.SimulationRunPanel;

/**
 * Base class for creating Simkit scenarios.
 * Modified to be BeanShellable and Viskit VCR aware - rmgoldbe, jmbailey
 *
 * @author ahbuss
 * @version $Id$
 */
public abstract class BasicAssembly extends BasicSimEntity implements Runnable {

    static final Logger LOG = Log4jUtilities.getLogger(BasicAssembly.class);
    
    protected Map<Integer, List<SavedStats>> replicationDataSavedStatisticsList;
    protected PropertyChangeListener[] replicationStatisticsPropertyChangeListenerArray;
    protected SampleStatistics[] designPointSimpleStatisticsTally;
    protected SimEntity[] simEntity;
    protected PropertyChangeListener[] propertyChangeListenerArray;
    protected boolean hookupsCalled;
    protected boolean stopRun;
    protected int startReplicationNumber = 0;
    protected Set<ReRunnable> runEntitiesSet;
    protected long seed;

    private double stopTime;
    private boolean singleStep;
    private int numberReplications;
    private boolean printReplicationReports;
    private boolean printSummaryReport;
    private boolean saveReplicationData;

    /** Ordering is essential for this collection */
    private Map<String, AssemblyNode> pclNodeCache;

    /** where file gets written */
    private File analystReportFile;
    
    private AnalystReportModel analystReportModel;

    /** A checkbox is user enabled from the Analyst Report Panel */
    private boolean enableAnalystReports = true;

    private final ReportStatisticsConfiguration reportStatisticsConfiguration;
    private int designPointID;
    private final DecimalFormat decimalFormat1, decimalFormat4;
    private List<String> entitiesWithStatisticsList;
    private PrintWriter printWriter;
    private int verboseReplicationNumber;
    private static ClassLoader runSimulationClassLoader; // TODO move this out of ViskitGlobals due to thread-clobbering issues
    private static ClassLoader workingClassLoader; // TODO move this out of ViskitGlobals due to thread-clobbering issues
    
    /** must be saved prior to running in new thread */
    private File  workingDirectory;
    private URL[] classPathUrlArray = new URL[0]; // legal default, actual values are set externally

            // Because there is no instantiated report builder in the current
            // thread context, we reflect here

    /**
     * Default constructor sets parameters of BasicAssembly to their
     * default values.  These are:
     * <pre>
     * printReplicationReports = true
     * printSummaryReport = true
     * saveReplicationData = false
     * numberReplications = 1
     * </pre>
     */
    public BasicAssembly() 
    {
        decimalFormat1 = new DecimalFormat("0.0; -0.0");
        decimalFormat4 = new DecimalFormat("0.0000; -0.000");
        setPrintReplicationReports(false);
        setPrintSummaryReport(true);
        replicationDataSavedStatisticsList = new LinkedHashMap<>();
        simEntity = new SimEntity[0];
        replicationStatisticsPropertyChangeListenerArray = new PropertyChangeListener[0];
        designPointSimpleStatisticsTally = new SampleStatistics[0];
        propertyChangeListenerArray = new PropertyChangeListener[0];
        setNumberReplications(SimulationRunPanel.DEFAULT_NUMBER_OF_REPLICATIONS);
        hookupsCalled = false;

//        analystReportFile = new File(ViskitGlobals.instance().getProjectRootDirectoryPath() + "/AnalystReports/", "AnalystReport.xml");
        
        // Creates a report statistics config object and names it based on the name of this Assembly.
        reportStatisticsConfiguration = new ReportStatisticsConfiguration(this.getName());
    }

    /**
     * Resets all inner statistics.  State resetting for SimEntities is their
     * responsibility.  Outer statistics are not reset.
     */
    @Override
    public void reset() {
        super.reset();
        for (PropertyChangeListener sampleStatisticsPropertyChangeListener : replicationStatisticsPropertyChangeListenerArray) {
            ((SampleStatistics) sampleStatisticsPropertyChangeListener).reset();
        }
        startReplicationNumber = 0;
    }

    // mask the Thread run()
    public void doRun() {
        setPersistant(false); // simkit spelling typo
    }

    /**
     * Create all the objects used.The <code>createSimEntities()</code> method
 is abstract and will be implemented in the concrete subclass.  The others
 are empty by default.  The <code>createReplicationStatistics()</code> method
     * must be overridden if any replications statistics are needed.
     */
    protected void createObjects() {
//        LOG.info("I was called?");
        createSimEntities();
        createReplicationStatistics();

        // This is implemented in this class
        createDesignPointStatistics();
        createPropertyChangeListeners();
    }

    /** Call all the hookup methods */
    protected void performHookups() {
        hookupSimEventListeners();
        hookupReplicationListeners();

        // This is implemented in this class
        hookupDesignPointListeners();
        hookupPropertyChangeListeners();
        hookupsCalled = true;
    }

    /** <p>
     * Receives the replicationStatistics LinkedHashMap from ViskitAssembly. This
     * method extracts the key values and passes them to ReportStatisticsConfig. The
     * key set is in the order of the replication statistics object in this class.
     * The goal of this and related methods is to aid ReportStatisticsConfig in
     * exporting statistical results sorted by SimEntity
     * </p>
     * <p>
     * NOTE: Requires that the Listeners in the assembly use the following naming
     * convention SimEntityName_PropertyName (e.g. RHIB_reportedContacts).
     * ReportStatistics config uses the underscore to extract the entity name
     * from the key values of the LinkedHashMap.
     * </p>
     * TODO: Remove the naming convention requirement and have the SimEntity name be
     * an automated key value
     * @param replicationStatistics a map containing collected statistics on a SimEntity's state variables
     */
    protected void setStatisticsKeyValues(Map<String, PropertyChangeListener> replicationStatistics) {
        Set<Map.Entry<String, PropertyChangeListener>> entrySet = replicationStatistics.entrySet();
        entitiesWithStatisticsList = new LinkedList<>();
        entrySet.forEach(entry -> {
            String ent = entry.getKey();
            LOG.debug("Entry is: " + entry);
            entitiesWithStatisticsList.add(ent);
        });
    }

    /** to be called after all entities have been added as a super()
     *  note not using template version of ArrayList...
     */
    protected abstract void createSimEntities();

    protected abstract void createReplicationStatistics();

    /**
     * The default behavior is to create a <code>SimplStatsTally</code>
     * instance for each element in <code>replicationStatisticsPropertyChangeListenerArray</code> with the
     * corresponding name + ".count," or ".mean"
     */
    protected void createDesignPointStatistics() {

        /* Check for zero length.  SimplePropertyDumper may have been selected
         * as the only PCL
         */
        if (BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length == 0) {return;}
        designPointSimpleStatisticsTally = new SampleStatistics[BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length];
        String statisticType, nodeType;
        int index = 0;
        boolean isCount;
        SampleStatistics sampleStatistics;
        Object obj;
        for (Map.Entry<String, AssemblyNode> entry : pclNodeCache.entrySet()) {
            LOG.debug("entry is: {}", entry);
            obj = pclNodeCache.get(entry.getKey());
            if (obj.getClass().toString().contains("PropertyChangeListenerNode")) {
                LOG.debug("AssemblyNode is: {}", obj);

                try {
                    // Since the pclNodeCache was created under a previous ClassLoader
                    // we must use reflection to invoke the methods on the AssemblyNodes
                    // that it contains, otherwise we will throw ClassCastExceptions
                    nodeType = obj.getClass().getMethod("getType").invoke(obj).toString();

                    // This is not a designPoint, so skip
                    if (nodeType.equals(ViskitStatics.SIMPLE_PROPERTY_DUMPER)) {
                        LOG.debug("SimplePropertyDumper encountered");
                        continue;
                    }

                    isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                    LOG.debug("isGetCount: " + isCount);

                    statisticType = isCount ? ".count" : ".mean";
                    LOG.debug("statisticType is: " + statisticType);

                    sampleStatistics = (SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[index];
                    
                    if (sampleStatistics.getName().equals("%unnamed%"))
                        sampleStatistics.setName(obj.getClass().getMethod("getName").invoke(obj).toString());
                    
                    designPointSimpleStatisticsTally[index] = new SimpleStatsTally(sampleStatistics.getName() + statisticType);

                    LOG.debug("Design point statistic: {}", designPointSimpleStatisticsTally[index]);
                    index++;
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
                    LOG.error("createDesignPointStatistics() exception: " + ex);
                }
            }
        }
    }

    protected abstract void createPropertyChangeListeners();

    protected abstract void hookupSimEventListeners();

    protected abstract void hookupReplicationListeners();

    /** Set up all outer statistics propertyChangeListeners */
    protected void hookupDesignPointListeners() {
        for (SampleStatistics designPointStatistics : designPointSimpleStatisticsTally) {
            this.addPropertyChangeListener(designPointStatistics);
        }
    }

    /**
     * This method is left concrete so subclasses don't have to worry about
     * it if no additional PropertyChangeListeners are desired.
     */
    protected void hookupPropertyChangeListeners() {}

    public void setStopTime(double time) {
        if (time < 0.0) {
            throw new IllegalArgumentException("Stop time must be >= 0.0: " + time);
        }
        stopTime = time;
    }

    public double getStopTime() {
        return stopTime;
    }

    public void setSingleStep(boolean b) {
        singleStep = b;
    }

    public boolean isSingleStep() {
        return singleStep;
    }

    /** Causes simulation runs to halt
     *
     * @param b if true, stops further simulation runs
     */
    public void setStopSimulationRun(boolean b) {
        stopRun = b;
        if (stopRun)
        {
            Schedule.stopSimulation();
        }
    }

    public void pauseSimulation()
    {
        Schedule.pause();
    }

    public void resumeSimulation()
    {
        Schedule.startSimulation();
    }

    /** this is getting called by the Assembly Runner stopSimulationRun
 button, which may get called on startup.
     */
    public void stopSimulationRun() {
        stopRun = true;
    }

    public void setEnableAnalystReports(boolean enable) {
        enableAnalystReports = enable;
    }

    public boolean isEnableAnalystReports() {return enableAnalystReports;}

    public final void setNumberReplications(int newNumberReplications) 
    {
        if (newNumberReplications < 1) {
            throw new IllegalArgumentException("Number replications must be > 0: " + newNumberReplications);
        }
        numberReplications = newNumberReplications;
    }

    /** How many replications have occurred so far during this simulation
     * @return number of replications completed, so far */
    public int getNumberReplications()
    {
        return numberReplications;
    }

    public final void setPrintReplicationReports(boolean b) {
        printReplicationReports = b;
    }

    public boolean isPrintReplicationReports() {
        return printReplicationReports;
    }

    public final void setPrintSummaryReport(boolean b) {
        printSummaryReport = b;
    }

    public boolean isPrintSummaryReport() {
        return printSummaryReport;
    }

    /** @return the absolute path to the temporary analyst report if user enabled */
    public String getAnalystReport() {
        return (analystReportFile == null) ? null : analystReportFile.getAbsolutePath();
    }

    public void setDesignPointID(int id) {
        designPointID = id;
    }

    public int getDesignPointID() {
        return designPointID;
    }

    public void setSaveReplicationData(boolean b) {
        saveReplicationData = b;
    }

    public boolean isSaveReplicationData() {
        return saveReplicationData;
    }

    /**
     * Empty, needed to implement SimEntity
     * @param simEvent the sim event to handle
     */
    @Override
    public void handleSimEvent(SimEvent simEvent) {
    }

    /**
     * Empty, needed to implement SimEntity
     * @param simEvent the sim event to process
     */
    @Override
    public void processSimEvent(SimEvent simEvent) {}

    /** @return an array of design point statistics for this Assembly */
    public SampleStatistics[] getDesignPointSampleStatistics() {
        return designPointSimpleStatisticsTally.clone();
    }

    /** @return an array of ProperChangeListeners for this Assembly  */
    public PropertyChangeListener[] getReplicationStatisticsPropertyChangeListenerArray() {
        return replicationStatisticsPropertyChangeListenerArray.clone();
    }

    /** @param id the ID of this replication statistic
     * @return an array of SampleStatistics for this Assembly
     */
    public SampleStatistics[] getReplicationSampleStatistics(int id) {
        SampleStatistics[] sampleStatisticsArray = null;

        List<SavedStats> currentReplicationDataSavedStatisticsList = replicationDataSavedStatisticsList.get(id);
        if (currentReplicationDataSavedStatisticsList != null)
            sampleStatisticsArray = GenericConversion.toArray(currentReplicationDataSavedStatisticsList, new SavedStats[0]);
        
        return sampleStatisticsArray;
    }

    public SampleStatistics getReplicationSampleStatistics(String name, int replicationNumber) {
        SampleStatistics sampleStatistics = null;
        int id = getIDforReplicationStateName(name);
        if (id >= 0) {
            sampleStatistics = getReplicationSampleStatistics(id)[replicationNumber];
        }
        return sampleStatistics;
    }

    public int getIDforReplicationStateName(String stateName) {
        int id = -1;
        int replicationStatisticsLength = BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length;
        for (int i = 0; i < replicationStatisticsLength; i++) {
            if (((Named) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[i]).getName().equals(stateName)) {
                id = i;
                break;
            }
        }
        return id;
    }

//    private void saveState(int lastReplicationNum) {
//        boolean midRun = !Schedule.getDefaultEventList().isFinished();
//        boolean midReplications = lastReplicationNum < getNumberReplicationlications();
//
//        if (midReplications) {
//            // middle of some rep, fell out because of GUI stopSimulationRun
//            startReplicationNumber = lastReplicationNum;
//        } else if (!midReplications && !midRun) {
//            // done with all replications
//            startReplicationlicationlicationNumber = 0;
//        } else if (!midReplications && midRun) {
//            // n/a can't be out of replications but in a run
//            throw new RuntimeException("Bad state in ViskitAssembly");
//        }
//    }

//    /**
//     * Called at top of rep loop;  This will support "pauseSimulation", but the GUI
//     * is not taking advantage of it presently.
//     * <p/>
//     * rg - try using Schedule.pauseSimulation() directly from GUI?
//     */
//    private void maybeReset() {
//        // We reset if we're not in the middle of a run
//
//        // but, isFinished didn't happen for the 0th
//        // replication
//        if (Schedule.getDefaultEventList().isFinished()) {
//            try {
//                Schedule.reset();
//            } catch (java.util.ConcurrentModificationException cme) {
//                LOG.error("Maybe not finished in Event List " + Schedule.getDefaultEventList().getID());
//            }
//        }
//    }

    /**
     * For each inner statistics, print to console name, count, min, max, mean,
     * standard deviation and variance. This can be done generically.
     *
     * @param replicationNumber The replication number (one off) for this report
     * @return a replication report section for the analyst report
     */
    protected String getReplicationReport(int replicationNumber) {

        PropertyChangeListener[] clonedReplicationStatistics = BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray();
        int i = 0;

        // Outputs raw replication statistics to XML report
        if (isSaveReplicationData())
            reportStatisticsConfiguration.processReplicationStatistics((replicationNumber + 1), clonedReplicationStatistics);
        
        // Borrowed from https://howtodoinjava.com/java/string/align-text-in-columns/#:~:text=One%20of%20the%20most%20straightforward,column%20within%20the%20format%20string.
        // Define column widths (CW). These may have to be slightly adjusted for
        // long property names and large counts/means
        int nameCW   = 26;
        int countCW  =  6;
        int minCW    =  7;
        int maxCW    =  9;
        int meanCW   = 12;
        int stdDevCW = 12;
        int varCW    = 16;

        // Report header
        StringBuilder buf = new StringBuilder("\nOutput Report for Replication #");
        buf.append(replicationNumber + 1);
        buf.append(System.getProperty("line.separator"));
        buf.append(String.format("%-" + nameCW + "s%" + countCW + "s%" 
                + minCW + "s%" + maxCW + "s%" + meanCW + "s%" + stdDevCW 
                + "s%" + varCW + "s", 
                "Name", "Count", "Minimum", 
                "Maximum", "Mean", "Standard Deviation", "Variance"));
        buf.append(System.getProperty("line.separator"));
        buf.append("-".repeat(nameCW+countCW+minCW+maxCW+meanCW+stdDevCW+varCW));

        SampleStatistics sampleStatistics;

        // Report data (statistics) in aligned columns
        for (PropertyChangeListener pcl : clonedReplicationStatistics) {
            sampleStatistics = (SampleStatistics) pcl;
            buf.append(System.getProperty("line.separator"));
            buf.append(String.format("%-" + nameCW   + "s",sampleStatistics.getName()));
            buf.append(String.format("%"  + countCW  + "d",sampleStatistics.getCount()));
            buf.append(String.format("%"  + minCW    + "s",decimalFormat1.format(sampleStatistics.getMinObs())));
            buf.append(String.format("%"  + maxCW    + "s",decimalFormat1.format(sampleStatistics.getMaxObs())));
            buf.append(String.format("%"  + meanCW   + "s",decimalFormat4.format(sampleStatistics.getMean())));
            buf.append(String.format("%"  + stdDevCW + "s",decimalFormat4.format(sampleStatistics.getStandardDeviation())));
            buf.append(String.format("%"  + varCW    + "s",decimalFormat4.format(sampleStatistics.getVariance())));

            ((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i++]).reset();
        }
        buf.append(System.getProperty("line.separator"));
        return buf.toString();
    }

    /**
     * For each outer statistics, print to console output name, count, min, max,
     * mean, standard deviation and variance. This can be done generically.
     * @return the summary report section of the analyst report
     */
    protected String getSummaryReport() {

        // Outputs raw summary statistics to XML report
        if (isSaveReplicationData())
            reportStatisticsConfiguration.processSummaryReport(getDesignPointSampleStatistics());

        StringBuilder buf = new StringBuilder("Summary Output Report:");
        buf.append(System.getProperty("line.separator"));
        buf.append(super.toString());
        buf.append(System.getProperty("line.separator"));

        for (SampleStatistics designPointStatistics : getDesignPointSampleStatistics()) {
            buf.append(System.getProperty("line.separator"));
            buf.append(designPointStatistics);
        }
        buf.append(System.getProperty("line.separator"));
        return buf.toString();
    }

    /**
     * These are the actual SimEnties in the array, but the array itself is
     * a copy.
     *
     * @return the SimEntities in this scenario in a copy of the array.
     */
    public SimEntity[] getSimEntities() {
        return simEntity.clone();
    }

    public void setOutputStream(OutputStream outputStream) {
        PrintStream outputPrintStream = new PrintStream(outputStream);
        this.printWriter = new PrintWriter(outputStream);

        // This OutputStream gets ported to the JScrollPane of the Assembly Runner
        Schedule.setOutputStream(outputPrintStream);
        // tbd, need a way to not use System.out as
        // during multi-threaded runs, some applications
        // send debug messages directy to System.out.
        // i.e., one thread sets System.out then another
        // takes it mid thread.

        // This is possibly what causes output to dump to a console
        System.setOut(outputPrintStream);
    }

    /** Execute the simulation for the desired number of replications */
    // TODO: Simkit not generisized yet
    @SuppressWarnings("unchecked")
    @Override
    public void run() 
    {
        stopRun = false;
        if (Schedule.isRunning() && !Schedule.getCurrentEvent().getName().equals("Run")) {
            LOG.error("Assemby already running.");
        }

        // Incase the user input bad parameters in the XML
        try {
            createObjects();
            performHookups();
        } 
        catch (Throwable t) {
            LOG.error("Comment in stack trace and recompile to drill down into: {}", t);
            // Comment in to see what the matter is
//            t.printStackTrace();

            try {
                URL url = new URI("mailto:" + ViskitStatics.VISKIT_MAILING_LIST
                        + "?subject=Assembly%20Run%20Error&body=log%20output:").toURL();
                
                String msg = "Assembly run aborted.  <br/>Please "
                    + "navigate to " + ViskitConfigurationStore.VISKIT_ERROR_LOG.getPath() + " and "
                    + "email the log to "
                    + "<b><a href=\"" + url.toString() + "\">" + ViskitStatics.VISKIT_MAILING_LIST + "</a></b>"
                    + "<br/><br/>Click the link to open up an email form, then attach the log. Would "
                    + "be good to have your project attached as well to replicate: File -> Zip/Mail Viskit Project";

                ViskitStatics.showHyperlinkedDialog(null, t.getMessage(), url, msg, true);
            } 
            catch (MalformedURLException | URISyntaxException ex) {
                LOG.error("error preparing mailto URI: " + ex);
            }
            return;
        }

        printInfo(); // subclasses may display what they wish at the top of the run.

        // reset the document with existing parameters since it might have run before
        reportStatisticsConfiguration.reset();
        reportStatisticsConfiguration.setEntityIndex(entitiesWithStatisticsList);
        if (!hookupsCalled)
            throw new RuntimeException("performHookups() hasn't been called!");

        LOG.info("Planned simulation stop time: " + getStopTime());
        Schedule.stopAtTime(getStopTime());
        Schedule.setEventSourceVerbose(true);

        if (isSingleStep())
            Schedule.setSingleStep(isSingleStep());
       
        // Used by the Gridlet(s)
        replicationDataSavedStatisticsList.clear();
        int replicationStatisticsLength = BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length;
        for (int i = 0; i < replicationStatisticsLength; i++)
            replicationDataSavedStatisticsList.put(i, new ArrayList<>());

        // TBD: there should be a pluggable way to have Viskit
        // directly modify entities. One possible way is to enforce
        // packages that wish to take advantage of exposed controls
        // all agree to be dependent on, i.e. viskit.simulation.Interface
        ReRunnable scenarioManager;

        runEntitiesSet = Schedule.getReruns();

        Method setNumberOfReplicationsMethod;
        // Convenience for Diskit if on the classpath
        for (ReRunnable entity : runEntitiesSet) {

            // access the SM's numberOfReplications parameter setter for user
            // dynamic input to override the XML value
            if (entity.getName().contains("ScenarioManager")) {
                scenarioManager = entity;
                try {
                    setNumberOfReplicationsMethod = scenarioManager.getClass().getMethod("setNumberOfReplications", int.class);
                    setNumberOfReplicationsMethod.invoke(scenarioManager, getNumberReplications());
                } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | SecurityException | NoSuchMethodException ex) {
                    LOG.error("run() error during ScenarioManager checks: " + ex);
                }
            }
        }

        int runCount = runEntitiesSet.size();

        for (int replication = 0; replication < getNumberReplications(); replication++) {
            firePropertyChange("replicationNumber", (replication + 1));
            if ((replication + 1) == getVerboseReplication()) {
                Schedule.setVerbose(true);
                Schedule.setReallyVerbose(true);
            } else {
                Schedule.setVerbose(isVerbose());
                Schedule.setReallyVerbose(isVerbose());
            }

            int nextRunCount = Schedule.getReruns().size();
            if (nextRunCount != runCount) {
                LOG.info("Reruns changed old: " + runCount + " new: " + nextRunCount);
                firePropertyChange("rerunCount", runCount, nextRunCount);
                runCount = nextRunCount;

                // print out new reRuns
                // Note: too many Sysouts for multiple replications.  Comment
                // in for debugging only.
//                LOG.info("ReRun entities added since startup: ");
//                Set<SimEntity> entitiesWithRunEvents = Schedule.getDefaultEventList().getRerun();
//                for (SimEntity entity : entitiesWithRunEvents) {
//                    if (!runEntitiesSet.contains(entity)) {
//                        System.out.print(entity.getName() + " ");
//                    }
//                }
//                LOG.info();
            }
            if (stopRun) 
            {
                LOG.info("Stopped in Replication # " + (replication + 1));
                break;
            } 
            else // continue running replications
            {
                if (Schedule.isRunning())
                    LOG.info("Already running.");
                
                seed = RandomVariateFactory.getDefaultRandomNumber().getSeed();
                LOG.info("Starting Replication #" + (replication + 1) + " with RNG seed state = " + seed);
                try {
                    Schedule.reset();
                } 
                catch (ConcurrentModificationException cme) 
                {
                    LOG.error("run() error when attempting Schedule.reset(); " + cme.getMessage());
                    ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                            "Assembly Run Error",
                            cme + "\nSimulation will terminate"
                    );
                    int newEventListId = Schedule.addNewEventList();
                    Schedule.setDefaultEventList(Schedule.getEventList(newEventListId));
                    for (SimEntity entity : simEntity) {
                        entity.setEventListID(newEventListId);
                    }

                    Schedule.stopSimulation();
                    Schedule.clearRerun();
                    runEntitiesSet.forEach(entity -> {
                        Schedule.addRerun(entity);
                    });
                }

                Schedule.startSimulation();

                String typeStatistics, nodeType;
                int ix = 0;
                boolean isCount;
                SampleStatistics sampleStatistics;
                Object obj;
                
                // Outer statistics output
                // # of PropertyChangeListenerNodes is == to replicationStatisticsPropertyChangeListenerArray.length
                if (pclNodeCache == null) // <- Headless mode
                    return;
                
                for (Map.Entry<String, AssemblyNode> entry : pclNodeCache.entrySet()) {
                    LOG.debug("entry is: {}", entry);

                    obj = pclNodeCache.get(entry.getKey());
                    if (obj.getClass().toString().contains("PropertyChangeListenerNode")) {

                        try {
                            // Since the pclNodeCache was created under a previous ClassLoader
                            // we must use reflection to invoke the methods on the AssemblyNodes
                            // that it contains, otherwise we will throw ClassCastExceptions
                            nodeType = obj.getClass().getMethod("getType").invoke(obj).toString();

                            // This is not a designPoint, so skip
                            if (nodeType.equals(ViskitStatics.SIMPLE_PROPERTY_DUMPER)) {
                                LOG.debug("SimplePropertyDumper encountered");
                                continue;
                            }
                            isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                            typeStatistics = isCount ? ".count" : ".mean";
                            sampleStatistics = (SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[ix];
                            fireIndexedPropertyChange(ix, sampleStatistics.getName(), sampleStatistics);

                            if (isCount)
                                fireIndexedPropertyChange(ix, sampleStatistics.getName() + typeStatistics, sampleStatistics.getCount());
                            else
                                fireIndexedPropertyChange(ix, sampleStatistics.getName() + typeStatistics, sampleStatistics.getMean());

                            ix++;
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
                            LOG.error("run() error during PropertyChangeListenerNode checks: " + ex);
                        }
                    }
                }
                if (isPrintReplicationReports()) {
                    printWriter.println(getReplicationReport(replication));
                    printWriter.flush();
                }
            }
        } // end of replication loop
        LOG.info("All simulation replications now complete.");

        if (isPrintSummaryReport()) 
        {
            printWriter.println(getSummaryReport());
            printWriter.flush();
        }
        
        if (isEnableAnalystReports()) 
        {
            // Creates the temp file only when user required
            initializeTemporaryAnalystReportFile();
            LOG.info("Temporary analyst report at " + analystReportFile.getAbsolutePath());
            isFileReady(analystReportFile);
            
//            // TODO the following block appears to break ViskitGlobals singleton pattern!
//            // Because there is no instantiated report builder in the current
//            // thread context, we reflect here
//            ClassLoader localLoader = ViskitGlobals.instance().getWorkingClassLoader();

// TODO better future fix: move out of reflection land completely...

            try 
            {
                if (getWorkingDirectory() == null)
                {
                    // this should only occur for analyst reports, following (and outside of) the simulation thread
                    // TODO hacking wildly here...
                    setWorkingDirectory(new File("./build/classes")); // no longer looking for project classes
//                  setWorkingDirectory(ViskitGlobals.instance().getProjectWorkingDirectory());
                }
                if (false)  // debugging to replace reflection code
                {
                analystReportModel = new AnalystReportModel(reportStatisticsConfiguration.getReport(), pclNodeCache);
                analystReportModel.writeToXMLFile(analystReportFile);
                if (!isFileReady(analystReportFile))
                {
                    LOG.error("analystReportFile not ready");
                }
                }
                
                if (workingClassLoader == null)
                {
                    LOG.error("run() error preparing for Analyst Report recovery: (workingClassLoader == null)");
                    return;
                }
                // TODO this line provokes the restart of singletons ViskitGlobals and ViskitConfigurationStore if used,
                // can we employ local reference?
                Class<?> clazz = workingClassLoader.loadClass("viskit.model.AnalystReportModel");
                
                Constructor<?> arbConstructor = clazz.getConstructor(String.class, Map.class);
                
                Object arbObject = arbConstructor.newInstance(reportStatisticsConfiguration.getReport(), pclNodeCache);
                Method writeToXMLFileMethod = clazz.getMethod("writeToXMLFile", File.class);
                writeToXMLFileMethod.invoke(arbObject, analystReportFile);
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                LOG.error("run() error during getWorkingClassLoader() and reflection checks: " + ex);
            }
            catch (Exception ue)
            {
                LOG.error("run() uncaught exception during getWorkingClassLoader() and reflection checks: " + ue);
            }
        }
    }

    /**
     * This gets called at the top of every run. It builds a tempFile and saves the path. That path is what
     * is used at the bottom of the run to write out the analyst report file. We report the path back to the caller
     * immediately, and it is the caller's responsibility to dispose of the file once they are done with it.
     */
    private void initializeTemporaryAnalystReportFile() {
        try {
            analystReportFile = TempFileManager.createTempFile("ViskitAnalystReport", ".xml");
        } 
        catch (IOException ioe) {
            analystReportFile = null;
            LOG.error("initializeAnalystReportFile() exception: " + ioe);
        }
    }

    public void setVerboseReplication(int i) {
        verboseReplicationNumber = i;
    }

    public int getVerboseReplication() {
        return verboseReplicationNumber;
    }

    public void setPclNodeCache(Map<String, AssemblyNode> pclNodeCache) {
        this.pclNodeCache = pclNodeCache;
    }

    /**
     * Method which may be overridden by subclasses (e.g., ViskitAssembly) which will be called after
     * createObject() at run time.
     */
    protected void printInfo() {}

    /** This class loader is specific to Assembly running in that it is
     * pristine from the working class loader in use for normal Viskit operations
     *
     * @return a pristine class loader for Assembly runs
     */
    public ClassLoader getRunSimulationClassLoader()
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
                    // do not use singleton referencing by ViskitGlobals here!
                    workingDirectory); 
                // Allow Assembly files in the ClassLoader
                runSimulationClassLoader = localBootLoader.initialize(true);
                // Set a RunSimulation ClassLoader for this thread to be free of any static
                // state set from the Viskit WorkingClassLoader
                Thread.currentThread().setContextClassLoader(runSimulationClassLoader);
                // TODO threading and singleton issues while inside ViskitGlobals?
                LOG.info("getRunSimulationClassLoader() created new ClassLoader for " + getWorkingDirectory());
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
        LOG.debug("resetRunSimulationClassLoader() complete"); // TODO threading issue?
    }

    /**
     * @return the workingDirectory
     */
    private File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @param workingDirectory the workingDirectory to set
     */
    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
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
    /** Check whether file and contents exist, ready for further work, copied from ViskitGlobals to avoid threading/singleton issues
     * @param file to check
     * @return whether ready
     */
    public static boolean isFileReady (File file)
    {
        if (file == null)
        {
            LOG.error("isFileReady() file reference is null");
            return false;
        }
        else if (!file.exists())
        {
            LOG.error("isFileReady() file does not exist: " + file.getPath());
            return false;
        }
        else if (file.length() == 0)
        {
            LOG.error("isFileReady() file is empty: " + file.getPath());
            return false;
        }
        return true;
    }

} // end class file BasicAssembly.java