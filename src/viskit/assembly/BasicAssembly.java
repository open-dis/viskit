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
package viskit.assembly;

import viskit.reports.ReportStatisticsConfig;
import static edu.nps.util.GenericConversion.toArray;
import edu.nps.util.LogUtils;
import edu.nps.util.TempFileManager;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import simkit.BasicSimEntity;
import simkit.Schedule;
import simkit.SimEntity;
import simkit.SimEvent;
import simkit.stat.SampleStatistics;
import simkit.stat.SavedStats;
import simkit.stat.SimpleStatsTally;
import viskit.VGlobals;
import viskit.model.AssemblyNode;

/**
 * Base class for creating Simkit scenarios.
 * Modified to be BeanShellable and Viskit VCR aware - rmgoldbe, jmbailey
 *
 * @author ahbuss
 * @version $Id$
 */
public abstract class BasicAssembly extends BasicSimEntity implements Runnable {

    static final Logger LOG = LogUtils.getLogger(BasicAssembly.class);
    protected Map<Integer, List<SavedStats>> replicationData;
    protected PropertyChangeListener[] replicationStats;
    protected SampleStatistics[] designPointStats;
    protected SimEntity[] simEntity;
    protected PropertyChangeListener[] propertyChangeListener;
    protected boolean hookupsCalled;
    protected boolean stopRun;
    protected int startRepNumber = 0;
    protected Set<SimEntity> runEntities;
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

    /** A checkbox is user enabled from the Analyst Report Panel */
    private boolean enableAnalystReports = false;

    /**
     * ***********************************************
     */
    private ReportStatisticsConfig statsConfig;
    private int designPointID;
    private DecimalFormat form;
    private LinkedList<String> entitiesWithStats;
    private PrintWriter printWriter;
    private int verboseReplicationNumber;

    /**
     * Default constructor sets parameters of BasicAssembly to their
     * default values.  These are:
     * <pre>
     * printReplicationReports = false
     * printSummaryReport = true
     * saveReplicationData = false
     * numberReplications = 1
     * </pre>
     */
    public BasicAssembly() {
        form = new DecimalFormat("0.0000");
        setPrintReplicationReports(false);
        setPrintSummaryReport(true);
        replicationData = new LinkedHashMap<>();
        simEntity = new SimEntity[0];
        replicationStats = new PropertyChangeListener[0];
        designPointStats = new SampleStatistics[0];
        propertyChangeListener = new PropertyChangeListener[0];
        setNumberReplications(1);
        hookupsCalled = false;

        //Creates a report stats config object and names it based on the name of this
        //Assembly.
        //TODO MIKE: instead of this.getName() We may not need to worry about the name of
        //the stats report file. Should discuss though.
        statsConfig = new ReportStatisticsConfig(this.getName());
    }

    /**
     * <p>Resets all inner stats.  State resetting for SimEntities is their
     * responsibility.  Outer stats are not reset.
     */
    @Override
    public void reset() {
        super.reset();
        for (PropertyChangeListener sampleStats : replicationStats) {
            ((SampleStatistics) sampleStats).reset();
        }
        startRepNumber = 0;
    }

    // mask the Thread run()
    public void doRun() {
        setPersistant(false);
    }

    /**
     * Create all the objects used.  The <code>createSimEntities()</code> method
     * is abstract and will be implemented in the concrete subclass.  The others
     * are empty by default.  The <code>createReplicationStats()</code> method
     * must be overridden if any replications stats are needed.
     */
    protected void createObjects() {
//        LOG.info("I was called?");
        createSimEntities();
        createReplicationStats();

        // This is implemented in this class
        createDesignPointStats();
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
     * Received the replicationStatistics LinkedHashMap from ViskitAssembly. This
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
     * @param repStatistics
     */
    protected void setStatisticsKeyValues(Map<String, PropertyChangeListener> repStatistics) {
        Set<Map.Entry<String, PropertyChangeListener>> entrySet = repStatistics.entrySet();
        entitiesWithStats = new LinkedList<>();
        for (Map.Entry<String, PropertyChangeListener> entry : entrySet) {
            String ent = entry.getKey();
            LOG.debug("Entry is: " + entry);
            entitiesWithStats.add(ent);
        }
    }

    protected abstract void createSimEntities();

    protected abstract void createReplicationStats();

    /**
     * The default behavior is to create a <code>SimplStatsTally</code>
     * instance for each element in <code>replicationStats</code> with the
     * corresponding name + ".count," or ".mean"
     */
    protected void createDesignPointStats() {

        /* Check for zero length.  SimplePropertyDumper may have been selected
         * as the only PCL
         */
        if (getReplicationStats().length == 0) {return;}
        designPointStats = new SampleStatistics[getReplicationStats().length];
        String typeStat;
        int ix = 0;
        boolean isCount;
        for (Map.Entry<String, AssemblyNode> entry : getPclNodeCache().entrySet()) {
            LOG.debug("entry is: " + entry);
            Object obj;
            if (entry.toString().contains("PropChangeListenerNode")) {

                // Since the pclNodeCache was created under a previous ClassLoader
                // we must use reflection to invoke the methods on the AssemblyNodes
                // that it contains, otherwise we will throw ClassCastExceptions
                try {
                    obj = getPclNodeCache().get(entry.getKey());
                    String nodeType = obj.getClass().getMethod("getType").invoke(obj).toString();

                    // This is not a designPoint, so skip
                    if (nodeType.equals("simkit.util.SimplePropertyDumper")) {
                        LOG.debug("SimplePropertyDumper encountered");
                        continue;
                    }
                    isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                    LOG.debug("isGetCount: " + isCount);
                    typeStat = isCount ? ".count" : ".mean";
                    LOG.debug("AssemblyNode key: " + entry.getKey());
                    LOG.debug("typeStat is: " + typeStat);
                    designPointStats[ix] = new SimpleStatsTally(((SampleStatistics) getReplicationStats()[ix]).getName() + typeStat);
                    LOG.debug(designPointStats[ix]);
                    ix++;
                } catch (NoSuchMethodException ex) {
                    LOG.error(ex);
                } catch (SecurityException ex) {
                    LOG.error(ex);
                } catch (IllegalAccessException ex) {
                    LOG.error(ex);
                } catch (IllegalArgumentException ex) {
                    LOG.error(ex);
                } catch (InvocationTargetException ex) {
                    LOG.error(ex);
                }
            }
        }
    }

    protected abstract void createPropertyChangeListeners();

    protected abstract void hookupSimEventListeners();

    protected abstract void hookupReplicationListeners();

    /** Set up all outer stats propertyChangeListeners */
    protected void hookupDesignPointListeners() {
        for (SampleStatistics designPointStat : designPointStats) {
            this.addPropertyChangeListener(designPointStat);
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

    /** this is getting called by the Assembly Runner stop
     * button, which may get called on startup.
     */
    public void stop() {
        stopRun = true;
    }

    public void setEnableAnalystReports(boolean enable) {
        enableAnalystReports = enable;
    }

    public boolean isEnableAnalystReports() {return enableAnalystReports;}

    public final void setNumberReplications(int num) {
        if (num < 1) {
            throw new IllegalArgumentException("Number replications must be > 0: " + num);
        }
        numberReplications = num;
    }

    public int getNumberReplications() {
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
    public SampleStatistics[] getDesignPointStats() {
        return designPointStats.clone();
    }

    /** @return an array of ProperChangeListeners for this Assembly  */
    public PropertyChangeListener[] getReplicationStats() {
        return replicationStats.clone();
    }

    /** @param id the ID of this replication statistic
     * @return an array of SampleStatistic for this Assembly
     */
    public SampleStatistics[] getReplicationStats(int id) {
        SampleStatistics[] stats = null;

        List<SavedStats> reps = replicationData.get(id);
        if (reps != null) {
            stats = toArray(reps, new SavedStats[0]);
        }
        return stats;
    }

    public SampleStatistics getReplicationStat(String name, int replication) {
        SampleStatistics stats = null;
        int id = getIDforReplicationStateName(name);
        if (id >= 0) {
            stats = getReplicationStats(id)[replication];
        }
        return stats;
    }

    public int getIDforReplicationStateName(String state) {
        int id = -1;
        int repStatsLength = getReplicationStats().length;
        for (int i = 0; i < repStatsLength; i++) {
            if (((SampleStatistics) getReplicationStats()[i]).getName().equals(state)) {
                id = i;
                break;
            }
        }
        return id;
    }

    public Map<Integer, List<SavedStats>> getReplicationData() {
        return new LinkedHashMap<>(replicationData);
    }

    private void saveState(int lastRepNum) {
        boolean midRun = !Schedule.getDefaultEventList().isFinished();
        boolean midReps = lastRepNum < getNumberReplications();

        if (midReps) {
            // middle of some rep, fell out because of GUI stop
            startRepNumber = lastRepNum;
        } else if (!midReps && !midRun) {
            // done with all reps
            startRepNumber = 0;
        } else if (!midReps && midRun) {
            // n/a can't be out of reps but in a run
            throw new RuntimeException("Bad state in ViskitAssembly");
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
     * For each inner stats, print to console name, count, min, max, mean,
     * standard deviation and variance.  This can be done generically.
     *
     * @param rep The replication number for this report
     * @return a replication report section of the analyst report
     */
    protected String getReplicationReport(int rep) {

        PropertyChangeListener[] clonedReplicationStats = getReplicationStats();

        // Outputs raw replication statistics to XML report
        if (isSaveReplicationData()) {
            statsConfig.processReplicationReport((rep + 1), clonedReplicationStats);
        }

        StringBuilder buf = new StringBuilder("Output Report for Replication #");
        buf.append(rep + 1);

        for (int i = 0; i < clonedReplicationStats.length; i++) {
            buf.append(System.getProperty("line.separator"));
            buf.append(((SampleStatistics) clonedReplicationStats[i]).getName());
//            buf.append('[');
//            buf.append(i);
//            buf.append(']');
            if (!(((SampleStatistics) clonedReplicationStats[i]).getName().length() > 20)) {
                buf.append('\t');
            }
            buf.append('\t');
            buf.append(((SampleStatistics) clonedReplicationStats[i]).getCount());
            buf.append('\t');
            buf.append(form.format(((SampleStatistics) clonedReplicationStats[i]).getMinObs()));
            buf.append('\t');
            buf.append(form.format(((SampleStatistics) clonedReplicationStats[i]).getMaxObs()));
            buf.append('\t');
            buf.append(form.format(((SampleStatistics) clonedReplicationStats[i]).getMean()));
            buf.append('\t');
            buf.append(form.format(((SampleStatistics) clonedReplicationStats[i]).getStandardDeviation()));
            buf.append('\t');
            buf.append(form.format(((SampleStatistics) clonedReplicationStats[i]).getVariance()));
            ((SampleStatistics) replicationStats[i]).reset();
        }
        return buf.toString();
    }

    /**
     * For each outer stats, print to console output name, count, min, max,
     * mean, standard deviation and variance.  This can be done generically.
     * @return the summary report section of the analyst report
     */
    protected String getSummaryReport() {

        // Outputs raw summary statistics to XML report
        if (isSaveReplicationData()) {
            statsConfig.processSummaryReport(getDesignPointStats());
        }

        StringBuilder buf = new StringBuilder("Summary Output Report:");
        buf.append(System.getProperty("line.separator"));
        buf.append(super.toString());
        for (SampleStatistics designPointStat : getDesignPointStats()) {
            buf.append(System.getProperty("line.separator"));
            buf.append(designPointStat);
        }
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

        // This OutputStream gets ported to the JScrollPane of the Assy Runner
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
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        stopRun = false;
        if (Schedule.isRunning() && !Schedule.getCurrentEvent().getName().equals("Run")) {
            System.out.println("Already running.");
        }
        createObjects();
        performHookups();

        printInfo();    // subclasses may display what they wish at the top of the run.

        // reset the document with
        // existing parameters
        // might have run before
        statsConfig.reset();
        statsConfig.setEntityIndex(entitiesWithStats);
        if (!hookupsCalled) {
            throw new RuntimeException("performHookups() hasn't been called!");
        }

        System.out.println("\nStopping at time: " + getStopTime());
        Schedule.stopAtTime(getStopTime());
        Schedule.setEventSourceVerbose(true);
        Schedule.setVerbose(isVerbose());
        if (isSingleStep()) {
            Schedule.setSingleStep(isSingleStep());
        }

        // This should be unchecked if only listening with a SimplePropertyDumper
        if (isSaveReplicationData()) {
            replicationData.clear();
            int repStatsLength = getReplicationStats().length;
            for (int i = 0; i < repStatsLength; i++) {
                replicationData.put(i, new ArrayList<SavedStats>());
            }
        }

        // TBD: there should be a pluggable way to have Viskit
        // directly modify entities. One possible way is to enforce
        // packages that wish to take advantage of exposed controls
        // all agree to be dependent on, i.e. viskit.simulation.Interface
        SimEntity timer = null;
        SimEntity scenarioManager = null;

        runEntities = Schedule.getReruns();
        for (SimEntity entity : runEntities) {
            if (entity.getName().equals("Clock") || entity.getName().equals("DISPinger")) {
                timer = entity;

            // Convenience for Diskit
            } else if (entity.getName().contains("ScenarioManager")) {
                scenarioManager = entity;
                // access the SM's numberOfReplications parameter setter
                try {
                    Method setNumberOfReplications = scenarioManager.getClass().getMethod("setNumberOfReplications", int.class);
                    setNumberOfReplications.invoke(scenarioManager, getNumberReplications());
                } catch (IllegalArgumentException ex) {
                    //ex.printStackTrace(); // nop, this is the default case
                } catch (InvocationTargetException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                } catch (IllegalAccessException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                } catch (SecurityException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                } catch (NoSuchMethodException ex) {
                    //ex.printStackTrace(); // nop, this is the default case
                }
            }
        }
        int runCount = runEntities.size();
        boolean clockChecker = false; // tbd hook this up and fix properly
        for (int replication = 0; replication < getNumberReplications(); replication++) {
            firePropertyChange("replicationNumber", (replication + 1));
            if (clockChecker && getVerboseReplication() >= 0) {
                timer.waitDelay("Stop", 0.0);
            }
            if (replication == getVerboseReplication()) {
                Schedule.setVerbose(true);
                Schedule.setReallyVerbose(true);
                if (clockChecker) {
                    timer.waitDelay("Ping", 0.0);
                }
            } else {
                Schedule.setVerbose(isVerbose());
                Schedule.setReallyVerbose(isVerbose());
            }

            int nextRunCount = Schedule.getReruns().size();
            if (nextRunCount != runCount) {
                System.out.println("Reruns changed old: " + runCount + " new: " + nextRunCount);
                firePropertyChange("rerunCount", runCount, nextRunCount);
                runCount = nextRunCount;

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
                Long seed = simkit.random.RandomVariateFactory.getDefaultRandomNumber().getSeed();
                firePropertyChange("seed", seed);
                if (Schedule.isRunning()) {
                    System.out.println("Already running.");
                }
                System.out.println("Starting Replication #" + (replication + 1) + " with random seed " + seed + " for:");
                try {
                    Schedule.reset();
                } catch (java.util.ConcurrentModificationException cme) {
                    JOptionPane.showMessageDialog(null, "Viskit has detected " +
                            "a possible error condition in the simulation " +
                            "entities. \nIt is possible that one of the " +
                            "entities is instancing a SimEntity type unsafely." +
                            " \nPlease check that any internally created " +
                            "entities are handled appropriately. \nYou'll " +
                            "probably have to restart Viskit, however, Viskit " +
                            "will now try to swap in\n a new EventList for " +
                            "debugging purposes only.");
                    int newEventListId = Schedule.addNewEventList();
                    Schedule.setDefaultEventList(Schedule.getEventList(newEventListId));
                    for (SimEntity entity : simEntity) {
                        entity.setEventListID(newEventListId);
                    }
                    Schedule.setReallyVerbose(true);
                    Schedule.stopSimulation();
                    Schedule.clearRerun();
                    for (SimEntity entity : runEntities) {
                        Schedule.addRerun(entity);
                    }
                }

                Schedule.startSimulation();

                String typeStat;
                int ix = 0;
                boolean isCount;

                // This should be unchecked if only listening with a SimplePropertyDumper
                if (isSaveReplicationData()) {
                    // # of PropertyChangeListenerNodes is == to replicationStats.length
                    for (Map.Entry<String, AssemblyNode> entry : getPclNodeCache().entrySet()) {
                        Object obj;
                        if (entry.toString().contains("PropChangeListenerNode")) {

                            // Since the pclNodeCache was created under a previous ClassLoader
                            // we must use reflection to invoke the methods on the AssemblyNodes
                            // that it contains, otherwise we will throw ClassCastExceptions
                            try {
                                obj = getPclNodeCache().get(entry.getKey());
                                String nodeType = obj.getClass().getMethod("getType").invoke(obj).toString();

                                // This is not a designPoint, so skip
                                if (nodeType.equals("simkit.util.SimplePropertyDumper")) {
                                    LOG.debug("SimplePropertyDumper encountered");
                                    continue;
                                }
                                isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                                typeStat = isCount ? ".count" : ".mean";
                                SampleStatistics ss = (SampleStatistics) getReplicationStats()[ix];
                                fireIndexedPropertyChange(ix, ss.getName(), ss);
                                if (isCount) {
                                    fireIndexedPropertyChange(ix, ss.getName() + typeStat, ss.getCount());
                                } else {
                                    fireIndexedPropertyChange(ix, ss.getName() + typeStat, ss.getMean());
                                }
                                ix++;
                            } catch (NoSuchMethodException ex) {
                                LOG.error(ex);
                            } catch (SecurityException ex) {
                                LOG.error(ex);
                            } catch (IllegalAccessException ex) {
                                LOG.error(ex);
                            } catch (IllegalArgumentException ex) {
                                LOG.error(ex);
                            } catch (InvocationTargetException ex) {
                                LOG.error(ex);
                            }
                        }
                    }
                }
                if (isPrintReplicationReports()) {
                    printWriter.println(getReplicationReport(replication));
                    printWriter.flush();
                }

                System.runFinalization();
                System.gc();
            }

            if (scenarioManager != null) {
                try {
                    Method doStop = scenarioManager.getClass().getMethod("doStopSimulation");
                    doStop.invoke(scenarioManager, getNumberReplications());
                } catch (IllegalArgumentException ex) {
                    //ex.printStackTrace();
                } catch (InvocationTargetException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                } catch (IllegalAccessException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                } catch (SecurityException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                } catch (NoSuchMethodException ex) {
//                    ex.printStackTrace();
                    LOG.error(ex);
                }
            }
        }

        if (isPrintSummaryReport()) {
            printWriter.println(getSummaryReport());
            printWriter.flush();
        }

        if (isEnableAnalystReports()) {

            // Creates the temp file only when user required
            initReportFile();

            ClassLoader localLoader = VGlobals.instance().getWorkClassLoader();
            try {
                Class<?> clazz = localLoader.loadClass("viskit.reports.AnalystReportBuilder");
                Constructor<?> arbConstructor = clazz.getConstructor(String.class, Map.class);
                Object arbObject = arbConstructor.newInstance(statsConfig.getReport(), getPclNodeCache());
                Method writeToXMLFile = clazz.getMethod("writeToXMLFile", File.class);
                writeToXMLFile.invoke(arbObject, analystReportFile);
            } catch (ClassNotFoundException ex) {
                LOG.error(ex);
            } catch (InstantiationException ex) {
                LOG.error(ex);
            } catch (IllegalAccessException ex) {
                LOG.error(ex);
            } catch (SecurityException ex) {
                LOG.error(ex);
            } catch (NoSuchMethodException ex) {
                LOG.error(ex);
            } catch (IllegalArgumentException ex) {
                LOG.error(ex);
            } catch (InvocationTargetException ex) {
                LOG.error(ex);
//                ex.printStackTrace();
            }
        }

        // TODO: Determine if these actually work they way are intended too?
        System.runFinalization();
        System.gc();
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

    public Map<String, AssemblyNode> getPclNodeCache() {
        return pclNodeCache;
    }

    public void setPclNodeCache(Map<String, AssemblyNode> pclNodeCache) {
        this.pclNodeCache = pclNodeCache;
    }

    /**
     * Method which may be overridden by subclasses (e.g., ViskitAssembly) which will be called after
     * createObject() at run time.
     */
    public void printInfo() {
    }

} // end class file BasicAssembly.java