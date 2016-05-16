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
package viskit.assembly;

import viskit.reports.ReportStatisticsConfig;
import edu.nps.util.GenericConversion;
import edu.nps.util.LogUtilities;
import edu.nps.util.TempFileManager;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import simkit.BasicSimEntity;
import simkit.Schedule;
import simkit.SimEntity;
import simkit.SimEvent;
import simkit.random.RandomVariateFactory;
import simkit.stat.SampleStatistics;
import simkit.stat.SavedStats;
import simkit.stat.SimpleStatsTally;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.ViskitConfiguration;
import viskit.control.InternalAssemblyRunner;
import viskit.model.AssemblyNode;

/**
 * Base class for creating Simkit scenarios.
 * Modified to be BeanShellable and Viskit VCR aware - rmgoldbe, jmbailey
 *
 * @author ahbuss
 * @version $Id$
 */
public abstract class BasicAssembly extends BasicSimEntity implements Runnable {

    static final Logger LOG = LogUtilities.getLogger(BasicAssembly.class);
    protected Map<Integer, List<SavedStats>> replicationData;
    protected PropertyChangeListener[] replicationStatisticsPropertyChangeListenerArray;
    protected SampleStatistics[] designPointStatistics;
    protected SimEntity[] simEntity;
    protected PropertyChangeListener[] propertyChangeListenerArray;
    protected boolean hookupsCalled;
    protected boolean stopRun;
    protected int startReplicationNumber = 0;
    protected Set<SimEntity> runEntities;
    protected long seed;

    private double stopTime;
    private boolean singleStep;
    private int numberOfReplications;
    private boolean printReplicationReports;
    private boolean printSummaryReport;
    private boolean saveReplicationData;

    /** Ordering is essential for this collection */
    private Map<String, AssemblyNode> pclNodeCache;

    /** where file gets written */
    private File analystReportFile;

    /** A checkbox is user enabled from the Analyst Report Panel */
    private boolean enableAnalystReports = true;

    private ReportStatisticsConfig statisticsConfig;
    private int designPointID;
    private DecimalFormat decimalFormat = new DecimalFormat("0.0000");
    private List<String> entitiesWithStatisticsList;
    private PrintWriter printWriter;
	/** State variable */
    private int verboseReplicationNumber;

    /**
     * Default constructor sets parameters of BasicAssembly to their
     * default values.  These are:
     * <pre>
 printReplicationReports = false
 printSummaryReport = true
 saveReplicationData = false
 numberOfReplications = 1
 </pre>
     */
    public BasicAssembly() {
        setPrintReplicationReports(false);
        setPrintSummaryReport(true);
        replicationData = new LinkedHashMap<>();
        simEntity = new SimEntity[0];
        replicationStatisticsPropertyChangeListenerArray = new PropertyChangeListener[0];
        designPointStatistics = new SampleStatistics[0];
        propertyChangeListenerArray = new PropertyChangeListener[0];
        setNumberOfReplications(1);
        hookupsCalled = false;

        // Creates a report statistics config object and names it based on the name
        // of this Assembly.
        statisticsConfig = new ReportStatisticsConfig(this.getName());
    }

    /**
     * <p>Resets all inner statistics.  State resetting for SimEntities is their
     * responsibility.  Outer statistics are not reset.
     */
    @Override
    public void reset() {
        super.reset();
        for (PropertyChangeListener sampleStatistics : replicationStatisticsPropertyChangeListenerArray) {
            ((SampleStatistics) sampleStatistics).reset();
        }
        startReplicationNumber = 0;
    }

    // mask the Thread run()
    public void doRun() {
        setPersistant(false);
    }

    /**
     * Create all the objects used.  The <code>createSimEntities()</code> method
     * is abstract and will be implemented in the concrete subclass.  The others
     * are empty by default.  The <code>createReplicationStatistics()</code> method
     * must be overridden if any replication statistics are needed.
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

    /**
     * Receives the replicationStatisticsPropertyChangeListenerArray LinkedHashMap from BasicAssembly. This
     * method extracts the key values and passes them to ReportStatisticsConfig. The
     * key set is in the order of the replication statistics object in this class.
     * The goal of this and related methods is to aid ReportStatisticsConfig in
     * exporting statistical results sorted by SimEntity
     * <p/>
     * NOTE: Requires that the Listeners in the assembly use the following naming
     * convention SimEntityName_PropertyName (e.g. RHIB_reportedContacts).
     * ReportStatistics config uses the underscore to extract the entity name
     * from the key values of the LinkedHashMap.
     * <p/>
     * TODO: Remove the naming convention requirement and have the SimEntity name be
     * an automated key value
     * @param replicationStatisticsMap a map containing collected statistics on a SimEntity's state variables
     */
    protected void setStatisticsKeyValues(Map<String, PropertyChangeListener> replicationStatisticsMap) {
        Set<Map.Entry<String, PropertyChangeListener>> entrySet = replicationStatisticsMap.entrySet();
        entitiesWithStatisticsList = new LinkedList<>();
        for (Map.Entry<String, PropertyChangeListener> entry : entrySet) {
            String entryKey = entry.getKey();
            LOG.debug("Entry key is: " + entry);
            entitiesWithStatisticsList.add(entryKey);
        }
    }

    /** to be called after all entities have been added as a super()
     *  note not using template version of ArrayList...
     */
    protected abstract void createSimEntities();

    protected abstract void createReplicationStatistics();

    /**
     * The default behavior is to create a <code>SimplStatisticsTally</code>
     * instance for each element in <code>replicationStatisticsPropertyChangeListenerArray</code> with the
     * corresponding name + ".count," or ".mean"
     */
    protected void createDesignPointStatistics() {

        /* Check for zero length.  SimplePropertyDumper may have been selected
         * as the only PCL
         */
        if (BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length == 0) {return;}
        designPointStatistics = new SampleStatistics[BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length];
        String typeStat, nodeType;
        int ix = 0;
        boolean isCount;
        for (Map.Entry<String, AssemblyNode> entry : pclNodeCache.entrySet()) {

            LOG.debug("entry is: " + entry);
            Object obj;

            if (entry.toString().contains("PropertyChangeListenerNode")) {

                // Since the pclNodeCache was created under a previous ClassLoader
                // we must use reflection to invoke the methods on the AssemblyNodes
                // that it contains, otherwise we will throw ClassCastExceptions
                try {
                    obj = pclNodeCache.get(entry.getKey());
                    LOG.debug("AssemblyNode key: " + obj);
                    nodeType = obj.getClass().getMethod("getType").invoke(obj).toString();

                    // This is not a designPoint, so skip
                    if (nodeType.equals(ViskitStatics.SIMPLE_PROPERTY_DUMPER)) {
                        LOG.debug("SimplePropertyDumper encountered");
                        continue;
                    }

                    isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                    LOG.debug("isGetCount: " + isCount);

                    typeStat = isCount ? ".count" : ".mean";
                    LOG.debug("typeStat is: " + typeStat);

                    SampleStatistics stat = (SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[ix];
                    if (stat.getName().equals("%unnamed%")) {
                        stat.setName(obj.getClass().getMethod("getName").invoke(obj).toString());
                    }

                    designPointStatistics[ix] = new SimpleStatsTally(stat.getName() + typeStat);

                    LOG.debug(designPointStatistics[ix]);
                    ix++;
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOG.error(ex);
                }
            }
        }
    }

    protected abstract void createPropertyChangeListeners();

    protected abstract void hookupSimEventListeners();

    protected abstract void hookupReplicationListeners();

    /** Set up all outer statistics propertyChangeListeners */
    protected void hookupDesignPointListeners() {
        for (SampleStatistics designPointStatistic : designPointStatistics) {
            this.addPropertyChangeListener(designPointStatistic);
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
    public void setStopRun(boolean b) {
        stopRun = b;
        if (stopRun) {
            Schedule.stopSimulation();
        }
    }

    public void pause() {
        Schedule.pause();
    }

    public void resume() {
        Schedule.startSimulation();
    }

    /** this is getting called by the Simulation Run stop
     * button, which may get called on startup.
     */
    public void stop() {
        stopRun = true;
    }

    public void setEnableAnalystReports(boolean enable) {
        enableAnalystReports = enable;
    }

    public boolean isEnableAnalystReports() {return enableAnalystReports;}

    public final void setNumberOfReplications(int numberOfReplications) {
        if (numberOfReplications < 1) {
            throw new IllegalArgumentException("Number of replications must be > 0: " + numberOfReplications);
        }
        this.numberOfReplications = numberOfReplications;
    }

    public int getNumberOfReplications() {
        return numberOfReplications;
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
    public SampleStatistics[] getDesignPointStatistics() {
        return designPointStatistics.clone();
    }

    /** @return an array of ProperChangeListeners for this Assembly  */
    public PropertyChangeListener[] getReplicationStatisticsPropertyChangeListenerArray() {
        return replicationStatisticsPropertyChangeListenerArray.clone();
    }

    /** @param id the ID of this replication statistic
     * @return an array of SampleStatistics for this Assembly
     */
    public SampleStatistics[] getReplicationStatistics(int id) {
        SampleStatistics[] statistics = null;

        List<SavedStats> reps = replicationData.get(id);
        if (reps != null) {
            statistics = GenericConversion.toArray(reps, new SavedStats[0]);
        }
        return statistics;
    }

    public SampleStatistics getReplicationStat(String name, int replication) {
        SampleStatistics statistics = null;
        int id = getIDforReplicationStateName(name);
        if (id >= 0) {
            statistics = getReplicationStatistics(id)[replication];
        }
        return statistics;
    }

    public int getIDforReplicationStateName(String state) {
        int id = -1;
        int replicationStatisticsLength = BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length;
        for (int i = 0; i < replicationStatisticsLength; i++) {
            if (((SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[i]).getName().equals(state)) {
                id = i;
                break;
            }
        }
        return id;
    }

    private void saveState(int lastReplicationNumber) // TODO why not used?
	{
        boolean middleOfRun          = !Schedule.getDefaultEventList().isFinished();
        boolean middleOfReplications = lastReplicationNumber < getNumberOfReplications();

        if (middleOfReplications) {
            // middle of some replication, fell out because of GUI stop
            startReplicationNumber = lastReplicationNumber;
        } else if (!middleOfReplications && !middleOfRun) {
            // done with all repeplications
            startReplicationNumber = 0;
        } else if (!middleOfReplications && middleOfRun) {
            // n/a can't be out of replications but within a run
            throw new RuntimeException("Bad state in BasicAssembly");
        }
    }

    /**
     * Called at top of rep loop;  This will support "pause", but the GUI
     * is not taking advantage of it presently.
     * <p/>
     * rg - try using Schedule.pause() directly from GUI?
     */
    private void maybeReset() {
        // We reset if we're not in the middle of a run

        // but, isFinished didn't happen for the 0th
        // replication
        if (Schedule.getDefaultEventList().isFinished()) {
            try {
                Schedule.reset();
            } catch (java.util.ConcurrentModificationException cme) {
                System.err.println("Maybe not finished in Event List " + Schedule.getDefaultEventList().getID());
            }
        }
    }

    /**
     * For each inner statistics, print to console name, count, min, max, mean,
     * standard deviation and variance.  This can be done generically.
     *
     * @param rep The replication number (one off) for this report
     * @return a replication report section for the analyst report
     */
    protected String getReplicationReport(int rep) {

        PropertyChangeListener[] clonedReplicationStatistics = BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray();
        int i = 0;

        // Outputs raw replication statistics to XML report
        if (isSaveReplicationData()) {
            statisticsConfig.processReplicationReport((rep + 1), clonedReplicationStatistics);
        }

        // Report header
        StringBuilder buf = new StringBuilder("\nOutput Report for Replication #");
        buf.append(rep + 1);
        buf.append(System.getProperty("line.separator"));
        buf.append("name");
        buf.append('\t');
        buf.append('\t');
        buf.append("count");
        buf.append('\t');
        buf.append("min");
        buf.append('\t');
        buf.append("max");
        buf.append('\t');
        buf.append("mean");
        buf.append('\t');
        buf.append("std dev");
        buf.append('\t');
        buf.append("var");

        SampleStatistics sampleStatistic;

        // Report data
        for (PropertyChangeListener pcl : clonedReplicationStatistics) {
            sampleStatistic = (SampleStatistics) pcl;
            buf.append(System.getProperty("line.separator"));
            buf.append(sampleStatistic.getName());
            if (!(sampleStatistic.getName().length() > 20)) {
                buf.append('\t');
            }
            buf.append('\t');
            buf.append(sampleStatistic.getCount());
            buf.append('\t');
            buf.append(decimalFormat.format(sampleStatistic.getMinObs()));
            buf.append('\t');
            buf.append(decimalFormat.format(sampleStatistic.getMaxObs()));
            buf.append('\t');
            buf.append(decimalFormat.format(sampleStatistic.getMean()));
            buf.append('\t');
            buf.append(decimalFormat.format(sampleStatistic.getStandardDeviation()));
            buf.append('\t');
            buf.append(decimalFormat.format(sampleStatistic.getVariance()));

           ((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i++]).reset();
        }
        buf.append(System.getProperty("line.separator"));
        return buf.toString();
    }

    /**
     * For each outer statistics, print to console output name, count, min, max,
     * mean, standard deviation and variance.  This can be done generically.
     * @return the summary report section of the analyst report
     */
    protected String getSummaryReport() {

        // Outputs raw summary statistics to XML report
        if (isSaveReplicationData()) {
            statisticsConfig.processSummaryReport(getDesignPointStatistics());
        }

		System.out.println(InternalAssemblyRunner.horizontalRuleDashes);
		System.out.println();
        StringBuilder buf = new StringBuilder("Summary Output Report:");
        buf.append(System.getProperty("line.separator"));
        buf.append(super.toString());
        buf.append(System.getProperty("line.separator"));

        for (SampleStatistics designPointStat : getDesignPointStatistics()) {
            buf.append(System.getProperty("line.separator"));
            buf.append(designPointStat);
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

    public void setOutputStream(OutputStream os) {
        PrintStream out = new PrintStream(os);
        this.printWriter = new PrintWriter(os);

        // This OutputStream gets ported to the JScrollPane of the Simulation Run
        Schedule.setOutputStream(out);
        // tbd, need a way to not use System.out as
        // during multi-threaded runs, some applications
        // send debug message directy to System.out.
        // ie, one thread sets System.out then another
        // takes it mid thread.

        // This is possibly what causes output to dump to a console
        System.setOut(out);
    }

    /** Execute the simulation for the desired number of replications */
    // TODO: Simkit not generisized yet
	
    @Override
	@SuppressWarnings("unchecked") // TODO Schedule<SimEntity> generics not yet supported by simkit
    public void run()
	{
        stopRun = false;
        if (Schedule.isRunning() && !Schedule.getCurrentEvent().getName().equals("Run")) {
            System.err.println("Assembly is already running.");
			return;
        }

        // Incase the user input bad parameters in the XML
        try {
            createObjects();
            performHookups();
        } 
		catch (Throwable t)
		{
            LOG.error(t);

            URL url = null;
			String path = "mailto:"; // avoid warning
            try {
                url = new URL("mailto:" + ViskitConfiguration.VISKIT_MAILING_LIST
                        + "?subject=Assembly%20Run%20Error&body=log%20output:");
				path = url.getPath();
            } catch (MalformedURLException ex) {
                LOG.error(ex);
            }
            String msg = "Assembly run aborted.  <br/>Please "
                    + "navigate to " + ViskitConfiguration.VISKIT_DEBUG_LOG.getPath() + " and "
                    + "email the log to "
                    + "<b><a href=\"" + path + "\">" + ViskitConfiguration.VISKIT_MAILING_LIST + "</a></b>"
                    + "<br/><br/>Click the link to open up an email form, then copy and paste the log's contents";

            ViskitStatics.showHyperlinkedDialog(null, t.toString(), url, msg, true);

            // Comment in to see what the matter is
            t.printStackTrace();
            return;
        }

        printInfo();    // subclasses may display what they wish at the top of the run.

        // reset the document with
        // existing parameters.
        // might have run before
        statisticsConfig.reset();
        statisticsConfig.setEntityIndex(entitiesWithStatisticsList);
        if (!hookupsCalled) {
            throw new RuntimeException("performHookups() hasn't been called!");
        }

        System.out.println("\nStopping at time: " + getStopTime() + "\n");
        Schedule.stopAtTime(getStopTime());
        Schedule.setEventSourceVerbose(true);

        if (isSingleStep()) {
            Schedule.setSingleStep(isSingleStep());
        }

        // This should be unchecked if only listening with a SimplePropertyDumper
        if (isSaveReplicationData()) {
            replicationData.clear();
            int replicationStatisticsLength = BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length;
            for (int i = 0; i < replicationStatisticsLength; i++) {
                replicationData.put(i, new ArrayList<SavedStats>());
            }
        }

        // TBD: there should be a pluggable way to have Viskit
        // directly modify entities. One possible way is to enforce
        // packages that wish to take advantage of exposed controls
        // all agree to be dependent on, i.e. viskit.simulation.Interface
        SimEntity scenarioManager;

        runEntities = Schedule.getReruns();

        // Convenience for Diskit if on the classpath; this is a SavageStudio hack...
        for (SimEntity entity : runEntities) {

            // access the ScenarioManager's numberOfReplications parameter setter for user
            // dynamic input to override the XML value
            if (entity.getName().contains("ScenarioManager")) {
                scenarioManager = entity;
                try {
                    Method setNumberOfReplications = scenarioManager.getClass().getMethod("setNumberOfReplications", int.class);
                    setNumberOfReplications.invoke(scenarioManager, getNumberOfReplications());
                } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | SecurityException | NoSuchMethodException ex) {
                    LOG.error(ex);
                }
            }
        }

        int runEntitiesCount = runEntities.size(); // number of SimEntity objects with Run methods

		// Replication loop
        for (int replication = 0; replication < getNumberOfReplications(); replication++)
		{
            firePropertyChange("replicationNumber", (replication + 1));
            if ((replication + 1) == getVerboseReplication()) // check whether one replication is marked as being verbose
			{
                Schedule.setVerbose(true);
                Schedule.setReallyVerbose(true);
            } else {
                Schedule.setVerbose(isVerbose());
                Schedule.setReallyVerbose(isVerbose());
            }
			// Initiate all Run events for existing event graphs
            int nextRunCount = Schedule.getReruns().size();
            if (nextRunCount != runEntitiesCount) { // TODO confirm logic
                LOG.debug("Reruns changed old: " + runEntitiesCount + " new: " + nextRunCount);
                firePropertyChange("rerunCount", runEntitiesCount, nextRunCount);
                runEntitiesCount = nextRunCount;

                // print out new reRuns
                // Note: too many Sysouts for multiple replications.  Comment
                // in for debugging only.
//                System.out.println("ReRun entities added since startup: ");
//                Set<SimEntity> entitiesWithRunEvents = Schedule.getDefaultEventList().getRerun();
//                for (SimEntity entity : entitiesWithRunEvents) {
//                    if (!runEntities.contains(entity)) {
//                        System.out.print(entity.getName() + " ");
//                    }
//                }
//                System.out.println();
            }
            if (stopRun) {
                LOG.info("Stopped in Replication # " + (replication + 1));
                break;
            } else {
                if (Schedule.isRunning()) {
                    System.out.println("Already running.");
                }
				// TODO keep track of seed RNG value for each replication in order to support readability
                seed = RandomVariateFactory.getDefaultRandomNumber().getSeed();
				System.out.println(InternalAssemblyRunner.horizontalRuleDashes);
				System.out.println();
                System.out.println("Starting Replication #" + (replication + 1) + " with RNG seed state of: " + seed);
                try {
                    Schedule.reset(); // master reset: each event graph and event list should be ready to go
                } catch (java.util.ConcurrentModificationException cme) {
                    JOptionPane.showMessageDialog(null,
                            cme + "\nSimulation will terminate",
                            "Simulation Run Error",
                            JOptionPane.ERROR_MESSAGE);
                    int newEventListId = Schedule.addNewEventList();
                    Schedule.setDefaultEventList(Schedule.getEventList(newEventListId));
                    for (SimEntity entity : simEntity) {
                        entity.setEventListID(newEventListId);
                    }

                    Schedule.stopSimulation();
                    Schedule.clearRerun();
                    for (SimEntity entity : runEntities) {
                        Schedule.addRerun(entity);
                    }
                }

                Schedule.startSimulation(); // simulation time set to 0, simulation proceeds

                String typeStat;
                int ix = 0;
                boolean isCount;

                // This should be unchecked if only listening with a SimplePropertyDumper
                if (isSaveReplicationData()) {
                    // # of PropertyChangeListenerNodes is == to replicationStatisticsPropertyChangeListenerArray.length
                    for (Map.Entry<String, AssemblyNode> entry : pclNodeCache.entrySet()) {
                        Object obj;
                        if (entry.toString().contains("PropertyChangeListenerNode")) {

                            // Since the pclNodeCache was created under a previous ClassLoader
                            // we must use reflection to invoke the methods on the AssemblyNodes
                            // that it contains, otherwise we will throw ClassCastExceptions
                            try {
                                obj = pclNodeCache.get(entry.getKey());
                                String nodeType = obj.getClass().getMethod("getType").invoke(obj).toString();

                                // This is not a designPoint, so skip
                                if (nodeType.equals(ViskitStatics.SIMPLE_PROPERTY_DUMPER)) {
                                    LOG.debug("SimplePropertyDumper encountered");
                                    continue;
                                }
                                isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                                typeStat = isCount ? ".count" : ".mean";
                                SampleStatistics ss = (SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[ix];
                                fireIndexedPropertyChange(ix, ss.getName(), ss);
                                if (isCount) {
                                    fireIndexedPropertyChange(ix, ss.getName() + typeStat, ss.getCount());
                                } else {
                                    fireIndexedPropertyChange(ix, ss.getName() + typeStat, ss.getMean());
                                }
                                ix++;
                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                LOG.error(ex);
                            }
                        }
                    }
                }
                if (isPrintReplicationReports()) {
                    printWriter.println(getReplicationReport(replication));
                    printWriter.flush();
                }
            }
			System.out.println ("Replication " + (replication + 1) + " complete.\n");
        }

        if (isPrintSummaryReport()) {
            printWriter.println(getSummaryReport());
            printWriter.flush();
        }

        if (isEnableAnalystReports()) {

            // Creates the temp file only when user required
            initReportFile();

            // Because there is no instantiated report builder in the current
            // thread context, we reflect here
            ClassLoader localLoader = ViskitGlobals.instance().getWorkClassLoader();
            try {
                Class<?> clazz = localLoader.loadClass("viskit.model.AnalystReportModel");
                Constructor<?> arbConstructor = clazz.getConstructor(String.class, Map.class);
                Object arbObject = arbConstructor.newInstance(statisticsConfig.getReport(), pclNodeCache);
                Method writeToXMLFile = clazz.getMethod("writeToXMLFile", File.class);
                writeToXMLFile.invoke(arbObject, analystReportFile);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            }
        }
    }

    /**
     * This gets called at the top of every run.  It builds a tempFile and saves the path.  That path is what
     * is used at the bottom of run to write out the analyst report file.  We report the path back to the caller
     * immediately, and it is the caller's responsibility to dispose of the file once he is done with it.
     */
    private void initReportFile() {
        try {
            analystReportFile = TempFileManager.createTempFile("ViskitAnalystReport", ".xml");
        } catch (IOException e) {
            analystReportFile = null;
            LOG.error(e);
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
     * Method which may be overridden by subclasses (e.g., BasicAssembly) which will be called after
     * createObject() at run time.
     */
    public void printInfo() {}

} // end class file BasicAssembly.java