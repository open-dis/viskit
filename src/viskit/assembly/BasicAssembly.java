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

import java.beans.PropertyChangeListener;

import java.io.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.text.DecimalFormat;
import java.util.*;
import javax.swing.JOptionPane;
import org.apache.logging.log4j.LogManager;
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
import viskit.ViskitUserConfiguration;
import viskit.ViskitProject;
import viskit.control.InternalAssemblyRunner.SimulationState;
import viskit.model.AnalystReportModel;
// import static viskit.ViskitGlobals.isFileReady; // while in thread, do not invoke ViskitGlobals!

import viskit.model.AssemblyNode;
import static viskit.model.ViskitElement.METHOD_getName;
import static viskit.model.ViskitElement.METHOD_getType;

import viskit.reports.ReportStatisticsConfiguration;
import viskit.view.SimulationRunPanel;
import static viskit.model.PropertyChangeListenerNode.METHOD_isStatisticTypeCount;
import static viskit.view.SimulationRunPanel.METHOD_setNumberReplications;

/**
 * Abstract base class for running assembly simulations, invoked in a thread by InternalAssemblyRunner.
 * The corresponding concrete class is the newly compiled assembly being invoked to run simulation.
 * Key characteristic: BasicAssembly is the only class running in a separate thread.
 * Modified to be BeanShellable and Viskit VCR aware - rmgoldbe, jmbailey
 *
 * @author ahbuss
 * @version $Id$
 */
public abstract class BasicAssembly extends BasicSimEntity implements Runnable 
{
    static final Logger LOG = LogManager.getLogger();
    
    protected Map<Integer, List<SavedStats>> replicationDataSavedStatisticsList;
    protected PropertyChangeListener[]       replicationStatisticsPropertyChangeListenerArray;
    protected SampleStatistics[]             designPointSimpleStatisticsTallyArray;
    protected SimEntity[]                    simEntityArray;
    protected PropertyChangeListener[]       propertyChangeListenerArray;
    
    /** external notification received for thread to stop all replications, simulation complete */
    protected boolean        stopSimulationRun;
    /** external notification received for thread to pause, either pausing replications or running a single step simulation */
    protected boolean       pauseSimulationRun;
    /** only proceed with one replication at a time */
    protected boolean  singleStepSimulationRun;
    
    /** if paused or single-step operation, next replication number of interest */
    protected int startReplicationNumber = 0;
    private int verboseReplicationNumber;
    
    protected boolean hookupsCalled;
    protected Set<ReRunnable> runEntitiesSet;
    protected long seed;

    private double  stopTime;
    private int     numberReplicationsPlanned;
    private boolean printReplicationReportsToConsole;
    private boolean printSummaryReportToConsole;
    private boolean saveReplicationData;

    /** Ordering is essential for this collection */
    private Map<String, AssemblyNode> pclNodeCache;

    /** where AnalystReport file gets written */
    private File analystReportFile;
    
    private AnalystReportModel analystReportModel;

    /** A checkbox is user enabled from the Analyst Report Panel */
    private boolean enableAnalystReports = true;
    
    /** debugThread is for developer use */
    private boolean debugThread = true; /* default */

    private int designPointID;
    private final DecimalFormat decimalFormat1, decimalFormat4;
    private List<String> entitiesWithStatisticsList;
    private PrintWriter printWriter;
    
    // private /*static*/ ClassLoader localWorkingClassLoader;       // TODO moved this out of ViskitGlobals due to thread-clobbering issues
    /** save local copies of these objects during setup, in order to avoid 
     * run-time queries while in a separate threaded context that clobbers
     * the singleton classes */
    /** must be saved prior to running in new thread (several approached did not work), or else
     * retrieved once thread has started in run() method */
    private File          projectDirectory;
    private ViskitProject viskitProject;
    private ClassLoader   localWorkingClassLoader;
    private ReportStatisticsConfiguration reportStatisticsConfiguration; // depends on ViskitProject
    private       String projectDirectoryPath = new String();
    private       String projectName          = new String();
    private       String assemblyName         = new String();
    
    private SimulationState simulationState;
    private int  pausedReplicationNumber = 0;
    private int initialReplicationNumber = 1;
        
            // Because there is no instantiated report builder in the current
            // thread context, we reflect here

    /**
     * Experimental: Constructor passes parameters to BasicAssembly when invoked as Runnable.
     * Also uses default constructor to set parameters to their
     * default values.
     * @param workingDirectory
     */
    public BasicAssembly(File workingDirectory) 
    {
        this(); // invoke default constructor
        // receiving parameters for use as Runnable
        this.projectDirectory = workingDirectory;
        

        // TODO superfluous?  actual file will be timestamped, actual directory already exists
//        analystReportFile = new File(ViskitGlobals.instance().getProjectDirectoryPath() +
//                                     "/AnalystReports/", "AnalystReport.xml");
//        LOG.info("BasicAssembly() constructor created new analystReportFile\n      " + analystReportFile.getAbsolutePath());
    }
    /**
     * Default constructor sets parameters of BasicAssembly to their
     * default values.These are:
     * <pre>
 printReplicationReportsToConsole = true
 printSummaryReport = true
 saveReplicationData = false
 numberReplicationsPlanned = 1
 </pre>
     */
    public BasicAssembly() 
    {
        decimalFormat1 = new DecimalFormat("0.0; -0.0");
        decimalFormat4 = new DecimalFormat(" 0.0000;-0.0000");
        
        // names also used by SimkitAssemblyXML2Java when generating source
        setPrintReplicationReportsToConsole(true); // TODO false
        setPrintSummaryReportToConsole(true);
        
        replicationDataSavedStatisticsList = new LinkedHashMap<>();
        simEntityArray = new SimEntity[0];
        replicationStatisticsPropertyChangeListenerArray = new PropertyChangeListener[0];
        designPointSimpleStatisticsTallyArray = new SampleStatistics[0];
        propertyChangeListenerArray = new PropertyChangeListener[0];
        hookupsCalled = false;
//      setNumberReplicationsPlanned(SimulationRunPanel.DEFAULT_NUMBER_OF_REPLICATIONS); // do not perform this, it is handled elsewhere
        
        fixThreadedName();
    }
    
    /** assembly name with &#46;1 appended while within thread.
     * @return name of implementing assembly */
    @Override
    public String getName()
    {
//      fixThreadedName(); // do not invoke while within thread
        
        if ((assemblyName == null) || assemblyName.isBlank())
        {
            assemblyName = getClass().getSimpleName();
//          assemblyName = assemblyName.substring(assemblyName.indexOf(".") + 1);
        }
        if ((assemblyName == null) || assemblyName.isBlank())
        {
            assemblyName = "ERROR_AssemblyNameNotFound";
            LOG.error("setReportXML unable to find assemblyName");
        }
        return assemblyName;
    }
    
    /** assembly name with &#46;1 removed, do not use while within thread.
     * @return name of implementing assembly */
    public String getFixedName()
    {
        fixThreadedName();
        return assemblyName;
    }
    /** when threaded, Java appends &#x2e;1 <!-- .1 --> to filename. */
    // https://stackoverflow.com/questions/18282086/how-tell-tell-javadoc-that-my-period-doesnt-end-a-sentence
    private void fixThreadedName()
    {
        assemblyName = this.getName(); // need to get rid of appended .1 when threaded
        if (assemblyName.endsWith(".1"))
        {
            // remove filename suffix that Java apparently adds when threaded
            assemblyName = assemblyName.substring(0, assemblyName.lastIndexOf(".1"));
        }
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
    protected void createObjects() 
    {
//        LOG.info("I was called?");
        createSimEntities();
        createReplicationStatistics();

        // This is implemented in this class
        createDesignPointStatistics();
        createPropertyChangeListeners();
    }

    /** Call all the hookup methods */
    protected void performHookups() 
    {
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
    protected void setStatisticsKeyValues(Map<String, PropertyChangeListener> replicationStatistics)
    {
        Set<Map.Entry<String, PropertyChangeListener>> entrySet = replicationStatistics.entrySet();
        entitiesWithStatisticsList = new LinkedList<>();
        entrySet.forEach(entry -> {
            String entryKey = entry.getKey();
            LOG.debug("Entry is: " + entry);
            entitiesWithStatisticsList.add(entryKey);
        });
    }

    /** to be called after all entities have been added as a super()
     *  note not using template version of ArrayList...
     */
    protected abstract void createSimEntities();

    protected abstract void createReplicationStatistics();

    /**
     * The default behavior is to create a <code>SimpleStatsTally</code>
     * instance for each element in <code>replicationStatisticsPropertyChangeListenerArray</code> with the
     * corresponding name + ".count," or ".mean"
     */
    protected void createDesignPointStatistics() 
    {
        /* Check for zero length.  SimplePropertyDumper may have been selected as the only PCL */
        if (BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length == 0) 
        {
            return;
        }
        designPointSimpleStatisticsTallyArray = new SampleStatistics[BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray().length];
        String statisticType, nodeType;
        int index = 0;
        boolean isCount;
        SampleStatistics sampleStatistics;
        Object obj;
        for (Map.Entry<String, AssemblyNode> entry : pclNodeCache.entrySet()) 
        {
            LOG.debug("createDesignPointStatistics(): map entry is: {}", entry);
            obj = pclNodeCache.get(entry.getKey());
            if (obj.getClass().toString().contains("PropertyChangeListenerNode")) 
            {
                LOG.debug("createDesignPointStatistics(): AssemblyNode is: {}", obj);

                try {
                    // Since the pclNodeCache was created under a previous ClassLoader
                    // we must use reflection to invoke the methods on the AssemblyNodes
                    // that it contains, otherwise we will throw ClassCastExceptions
                    nodeType = obj.getClass().getMethod(METHOD_getType).invoke(obj).toString();

                    // This is not a designPoint, so skip
                    if (nodeType.equals(ViskitStatics.SIMPLE_PROPERTY_DUMPER)) {
                        LOG.debug("createDesignPointStatistics(): createDesignPointStatistics():SimplePropertyDumper encountered");
                        continue;
                    }

                    isCount = Boolean.parseBoolean(obj.getClass().getMethod(METHOD_isStatisticTypeCount).invoke(obj).toString());
                    LOG.debug("createDesignPointStatistics(): isGetCount: " + isCount);

                    statisticType = isCount ? ".count" : ".mean";
                    LOG.debug("createDesignPointStatistics(): statisticType is: " + statisticType);

                    sampleStatistics = (SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[index];
                    
                    if (sampleStatistics.getName().equals("%unnamed%"))
                        sampleStatistics.setName(obj.getClass().getMethod(METHOD_getName).invoke(obj).toString());
                    
                    designPointSimpleStatisticsTallyArray[index] = new SimpleStatsTally(sampleStatistics.getName() + statisticType);

                    LOG.debug("createDesignPointStatistics(): Design point statistic: {}", designPointSimpleStatisticsTallyArray[index]);
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
    
    /** method name for reflection use, found in superclass */
    public static final String METHOD_addPropertyChangeListener = "addPropertyChangeListener";

    /** Set up all outer statistics propertyChangeListeners */
    protected void hookupDesignPointListeners() 
    {
        for (SampleStatistics designPointStatistics : designPointSimpleStatisticsTallyArray) 
        {
            this.addPropertyChangeListener(designPointStatistics); // add designPointStatistics listener using superclass method
        }
    }

    /**
     * This method is left concrete so subclasses don't have to worry about
     * it if no additional PropertyChangeListeners are desired.
     */
    protected void hookupPropertyChangeListeners() {}
    
    /** method name for reflection use */
    public static final String METHOD_setStopTime = "setStopTime";

    public void setStopTime(double newStopTime) 
    {
        if (isDebugThread()) logThreadStatus(METHOD_setStopTime + " newStopTime=" + newStopTime);
        
        if (newStopTime < 0.0) {
            throw new IllegalArgumentException("Stop time must be >= 0.0: " + newStopTime);
        }
        stopTime = newStopTime;
    }
    
    /** method name for reflection use */
    public static final String METHOD_getStopTime = "getStopTime";

    public double getStopTime() {
        return stopTime;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setSingleStepSimulationRun = "setSingleStepSimulationRun";

    /** Causes paused simulation runs to continue in single-step mode by receiving external signal inside the execution thread
     * @param newValue signal whether to continue in single-step mode
     */
    public void setSingleStepSimulationRun(boolean newValue)
    {
        // do not set debug breakpoint in this method or else thread is not notified in a timely manner
        
        if (isDebugThread()) logThreadStatus(METHOD_setSingleStepSimulationRun + " newValue=" + newValue + " (threaded)");
        
        singleStepSimulationRun = newValue;
    }

    public boolean isSingleStep() 
    {
        return singleStepSimulationRun;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setStopSimulationRun = "setStopSimulationRun";

    /** Causes simulation runs to stop by receiving external signal inside the execution thread
     *
     * @param newValue signal whether to stop further simulation runs
     */
    public void setStopSimulationRun(boolean newValue) 
    {
        // do not set debug breakpoint in this method or else thread is not notified in a timely manner
        
        stopSimulationRun = newValue; // save value
        if (!newValue)
            return;  // ignore if false
        
        if (isDebugThread()) logThreadStatus(METHOD_setStopSimulationRun + " newValue=" + newValue + " begun (threaded)");
        
        if (stopSimulationRun)
        {
            // reset other sentinels 
                 pauseSimulationRun = false;
            singleStepSimulationRun = false; // in case we were in single-step mode
            Schedule.stopSimulation(); // simkit
        }
        
        if (isDebugThread()) logThreadStatus(METHOD_setStopSimulationRun + "... complete");
    }
    
    /** method name for reflection use */
    public static final String METHOD_setPauseSimulationRun = "setPauseSimulationRun";

    /** Causes simulation runs to pause (or single step) by receiving external signal inside the execution thread
     *
     * @param newValue signal whether to pause further simulation runs
     */
    public void setPauseSimulationRun(boolean newValue) 
    {
        // do not set debug breakpoint in this method or else thread is not notified in a timely manner
        
        if (newValue == false)
            LOG.error("setPauseSimulationRun({}) received unexpected value, ignoring", newValue);
        
        if (pauseSimulationRun && newValue) // previous loop and current loop received PAUSE so we are in STEP mode
            singleStepSimulationRun = true;
        
        pauseSimulationRun = newValue; // save value
        if (!newValue)
            return;  // ignore if false
        
        if (isDebugThread()) logThreadStatus(METHOD_setPauseSimulationRun + "(" + newValue + ")" + " begun (threaded)");
        
        if (pauseSimulationRun)
        {
            // advance the simkit event clock when in single step mode?  no, stay at per-replication level
//            Schedule.setPauseAfterEachEvent(true); // simkit

//          Schedule.pause(); // simkit method; no, blocks console for text-based thread console
        }
        LOG.info("setPauseSimulationRun({}), pauseSimulationRun={}, singleStepSimulationRun={}", newValue, pauseSimulationRun, singleStepSimulationRun); 
    }
    
    /** method name for reflection use */
    public static final String METHOD_setRunResumeSimulation = "setRunResumeSimulation";

    /** Causes simulation runs to begin running (or resume running) by receiving external signal inside the execution thread
     */
    public void setRunResumeSimulation()
    {
        // do not set debug breakpoint in this method or else thread is not notified in a timely manner
        
        if (isDebugThread()) logThreadStatus(METHOD_setRunResumeSimulation + " begun (threaded)");
        
        // reset other sentinels 
             pauseSimulationRun = false;
        singleStepSimulationRun = false; // in case we were in single-step mode
        
        Schedule.startSimulation(); // simkit
    }

//    /** this is getting called by the Assembly Runner stopSimulationRun
// button, which may get called on startup.
//     */
//    // TODO unused??  uncalled?
//    public void stopSimulationRun()
//    {
//        stopSimulationRun = true;
//    }
    
    /** method name for reflection use */
    public static final String METHOD_setEnableAnalystReports = "setEnableAnalystReports";

    public void setEnableAnalystReports(boolean enable) {
        enableAnalystReports = enable;
    }

    public boolean isEnableAnalystReports() {return enableAnalystReports;}
    
    /** method name for reflection use */
    public static final String METHOD_setNumberReplicationsPlanned = "setNumberReplicationsPlanned";
    
    public final void setNumberReplicationsPlanned(int newNumberReplicationsPlanned) 
    {
        if (newNumberReplicationsPlanned < 1) {
            throw new IllegalArgumentException("setNumberReplicationsPlanned(): planned number of replications must be > 0: " + newNumberReplicationsPlanned);
        }
        numberReplicationsPlanned = newNumberReplicationsPlanned;
    }
    
    /** method name for reflection use */
    public static final String METHOD_getNumberReplicationsPlanned = "getNumberReplicationsPlanned";

    /** How many replications have occurred so far during this simulation
     * @return number of replications completed, so far */
    public int getNumberReplicationsPlanned()
    {
        return numberReplicationsPlanned;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setPrintReplicationReportsToConsole = "setPrintReplicationReportsToConsole";

    public final void setPrintReplicationReportsToConsole(boolean newValue) {
        printReplicationReportsToConsole = newValue;
    }
    
    /** method name for reflection use */
    public static final String METHOD_isPrintReplicationReportsToConsole = "isPrintReplicationReportsToConsole";

    public boolean isPrintReplicationReportsToConsole() {
        return printReplicationReportsToConsole;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setPrintSummaryReportToConsole = "setPrintSummaryReportToConsole";

    public final void setPrintSummaryReportToConsole(boolean newValue) {
        printSummaryReportToConsole = newValue;
    }
    
    /** method name for reflection use */
    public static final String METHOD_isPrintSummaryReportToConsole = "isPrintSummaryReportToConsole";

    public boolean isPrintSummaryReportToConsole() {
        return printSummaryReportToConsole;
    }
    
    /** method name for reflection use */
    public static final String METHOD_getAnalystReport = "getAnalystReport";

    /** @return the absolute path to the temporary analyst report if user enabled */
    public String getAnalystReport() 
    {
        if (pauseSimulationRun)
            return null; // not ready yet
        
        if (analystReportFile == null)
        {
            LOG.error("getAnalystReport() found (analystReportFile == null)"); // unexpected condition
            return "";
        }
        else return analystReportFile.getAbsolutePath();
    }

    public void setDesignPointID(int id) {
        designPointID = id;
    }

    public int getDesignPointID() {
        return designPointID;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setSaveReplicationData = "setSaveReplicationData";

    public void setSaveReplicationData(boolean newValue) {
        saveReplicationData = newValue;
    }
    
    /** method name for reflection use */
    public static final String METHOD_isSaveReplicationData = "isSaveReplicationData";

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
        return designPointSimpleStatisticsTallyArray.clone();
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

    public SampleStatistics getReplicationSampleStatistics(String name, int replicationNumber) 
    {
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
            reportStatisticsConfiguration.processReplicationStatistics((replicationNumber), clonedReplicationStatistics);
        
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
        StringBuilder outputReportStringBuilder = new StringBuilder();
        
        if ((simulationState != null) && (simulationState != SimulationState.RUN) && 
            (replicationNumber != getNumberReplicationsPlanned()))
        {
            // likely the STOP button was pressed
            outputReportStringBuilder.append(System.getProperty("line.separator"));
            outputReportStringBuilder.append("(Simulation did not complete ")
                                     .append(getNumberReplicationsPlanned()).append(" replications as originally planned)");
            outputReportStringBuilder.append(System.getProperty("line.separator"));
        }
        outputReportStringBuilder.append("\nOutput Report following Replication #");
        outputReportStringBuilder.append(replicationNumber);
        outputReportStringBuilder.append(System.getProperty("line.separator"));
        outputReportStringBuilder.append(System.getProperty("line.separator"));
        outputReportStringBuilder.append(String.format("%-" + nameCW + "s%" + countCW + "s%" 
                + minCW + "s%" + maxCW + "s%" + meanCW + "s%" + stdDevCW 
                + "s%" + varCW + "s", 
                "Name", "Count", "Minimum", 
                "Maximum", "Mean", "Standard Deviation", "Variance"));
        outputReportStringBuilder.append(System.getProperty("line.separator"));
        outputReportStringBuilder.append("-".repeat(nameCW+countCW+minCW+maxCW+meanCW+stdDevCW+varCW));

        SampleStatistics sampleStatistics;

        // Report data (statistics) in aligned columns
        for (PropertyChangeListener pcl : clonedReplicationStatistics) {
            sampleStatistics = (SampleStatistics) pcl;
            outputReportStringBuilder.append(System.getProperty("line.separator"));
            outputReportStringBuilder.append(String.format("%-" + nameCW   + "s",sampleStatistics.getName()));
            outputReportStringBuilder.append(String.format("%"  + countCW  + "d",sampleStatistics.getCount()));
            outputReportStringBuilder.append(String.format("%"  + minCW    + "s",decimalFormat1.format(sampleStatistics.getMinObs())));
            outputReportStringBuilder.append(String.format("%"  + maxCW    + "s",decimalFormat1.format(sampleStatistics.getMaxObs())));
            outputReportStringBuilder.append(String.format("%"  + meanCW   + "s",decimalFormat4.format(sampleStatistics.getMean())));
            outputReportStringBuilder.append(String.format("%"  + stdDevCW + "s",decimalFormat4.format(sampleStatistics.getStandardDeviation())));
            outputReportStringBuilder.append(String.format("%"  + varCW    + "s",decimalFormat4.format(sampleStatistics.getVariance())));

            ((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i++]).reset();
        }
        outputReportStringBuilder.append(System.getProperty("line.separator"));
        return outputReportStringBuilder.toString();
    }

    /**
     * For each of the outer statistics, print to console output name, count, min, max,
     * mean, standard deviation and variance. This can be done generically.
     * @return the summary report section of the analyst report
     */
    protected String getSummaryReport() {

        // Outputs raw summary statistics to XML report
        if (isSaveReplicationData())
            reportStatisticsConfiguration.processSummaryReport(getDesignPointSampleStatistics());

        StringBuilder summaryReportStringBuilder = new StringBuilder("Summary Output Report:");
        summaryReportStringBuilder.append(" ").append(assemblyName); // class name is mangled by thread with .1 appended: super.toString());
        summaryReportStringBuilder.append(System.getProperty("line.separator"));

        for (SampleStatistics designPointStatistics : getDesignPointSampleStatistics())
        {
            summaryReportStringBuilder.append(System.getProperty("line.separator"));
            summaryReportStringBuilder.append(designPointStatistics);
        }
        summaryReportStringBuilder.append(System.getProperty("line.separator"));
        return summaryReportStringBuilder.toString();
    }

    /**
     * These are the actual SimEnties in the array, but the array itself is
     * a copy.
     *
     * @return the SimEntities in this scenario in a copy of the array.
     */
    public SimEntity[] getSimEntities() {
        return simEntityArray.clone();
    }
    
    /** method name for reflection use */
    public static final String METHOD_setOutputStream = "setOutputStream";

    public void setOutputStream(OutputStream outputStream) {
        PrintStream outputPrintStream = new PrintStream(outputStream);
        this.printWriter = new PrintWriter(outputStream);

        // This OutputStream gets ported to the JScrollPane of the Assembly Runner
        Schedule.setOutputStream(outputPrintStream); // simkit
        // tbd, need a way to not use System.out as
        // during multi-threaded runs, some applications
        // send debug messages directy to System.out.
        // i.e., one thread sets System.out then another
        // takes it mid thread.

        // This is possibly what causes output to dump to a console
        System.setOut(outputPrintStream);
    }

    /** This method should only occur wile BasicAssembly is running inside an independent thread. */
    public void findProjectWorkingDirectoryFromWithinThread()
    {
        // this should only occur inside the simulation thread
        // TODO hacking wildly here...
//            File findingClassesDirectory = new File("./build/classes"); // hoping to find we are in project...
//            LOG.info("Experimental: findingClassesDirectory=\n      " + findingClassesDirectory.getAbsolutePath());
//            setWorkingDirectory(findingClassesDirectory); // no good, we are not in project directory, darn

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 1: 
        // First reaching into ViskitGlobals even though it breaks singleton pattern, ouch.
        // Note that Netbeans object inspection can be misleading when looking at ViskitGlobals while inside the thread.
        // In any case, the directory is no longer there.
        
        /*
        // the following test provokes a singleton-reset problem message
        if (ViskitGlobals.instance().getProjectWorkingDirectory() == null)
            LOG.error("BLOCKER: run() ViskitGlobals.instance().getProjectWorkingDirectory() is null");
        else 
            setWorkingDirectory(ViskitGlobals.instance().getProjectWorkingDirectory());
        */

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 2: default class initialization
        
        if (projectDirectory == null) // don't test getWorkingDirectory() which likely produces NPE
        {
            LOG.debug("BLOCKER: run() following initial configuration, getWorkingDirectory() is null");
        }
        else if (!getWorkingDirectory().exists())
        {
            LOG.debug("BLOCKER: run() " + getWorkingDirectory().getAbsolutePath() + " does not exist!");
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 3.  Look in ViskitUserConfiguration (and TODO possibly relaxing singleton status)
        
        // can look in user/.viskit/c_app.xml:  yes the project home directory and name are included there in 
        // ViskitConfig/app/projectHome/path@dir and name@value
        // exemplar code found in singleton ViskitUserConfiguration()
                
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 3a.  recreate Apache Commons Configuration code
        
        /*
        File VISKIT_CONFIGURATION_DIR = new File(System.getProperty("user.home"), ".viskit");
        if (!VISKIT_CONFIGURATION_DIR.exists())
            LOG.error("BLOCKER: run() VISKIT_CONFIGURATION_DIR does not exist");
        else if (!VISKIT_CONFIGURATION_DIR.isDirectory())
            LOG.error("BLOCKER: run() VISKIT_CONFIGURATION_DIR is not a directory");
        
        File C_APP_FILE               = new File(VISKIT_CONFIGURATION_DIR, "c_app.xml");
        if (!C_APP_FILE.exists())
             LOG.error("BLOCKER: run() C_APP_FILE does not exist");
        else if (C_APP_FILE.isDirectory())
             LOG.error("BLOCKER: run() C_APP_FILE is a directory, not a file as expected");
        else LOG.info  ("run() C_APP_FILE found:\n      " + C_APP_FILE.getAbsolutePath());
        // simple accessor, everything there should be good
        // https://commons.apache.org/proper/commons-configuration/userguide/quick_start.html
        Configurations configurations = new Configurations();
        
        String PROJECT_HOME_CLEAR_KEY_PREFIX = "app.projecthome";
        String PROJECT_PATH_KEY = PROJECT_HOME_CLEAR_KEY_PREFIX + ".path[@dir]";
        String PROJECT_NAME_KEY = PROJECT_HOME_CLEAR_KEY_PREFIX + ".name[@value]";
        try
        {
            Configuration config = configurations.properties(C_APP_FILE);
            projectDirectoryPath = config.getString( PROJECT_PATH_KEY); // workingDirectoryKey);
            projectName          = config.getString(PROJECT_NAME_KEY);
        }
        catch (ConfigurationException ce)
        {
            LOG.error("(incomplete implementation) run() commons configuration excerpt unable to read C_APP_FILE: " + ce.getMessage());
        }
        */
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 3b.  re-use ViskitConfiguration for Apache Commons Configuration code

        if (projectDirectory == null)
        {
            projectDirectoryPath = ViskitUserConfiguration.instance().getViskitProjectDirectoryPath();
            projectName          = ViskitUserConfiguration.instance().getViskitProjectName();
            projectDirectory     = ViskitUserConfiguration.instance().getViskitProjectDirectory();
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 4.  BLOCKER TODO: 
        // how to find working directory while inside thread, perhaps via ClassLoader context?
        
        // not yet tried
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Approach 4.  "Hardwire" local MyProjects/DefaultProject
        
        if (projectDirectory == null)
        {
            projectDirectory = new File("MyViskitProjects/DefaultProject");
            projectName      = "DefaultProject";
            LOG.info("run() findProjectWorkingDirectoryFromWithinThread() hard-wired directory:\n  " + getWorkingDirectory().getAbsolutePath());
        }
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // all done searching around, report outcome
        if ((projectDirectory != null) && projectDirectory.isDirectory())
        {
             // found it!
             LOG.info("run() findProjectWorkingDirectoryFromWithinThread() worked!\n      " + getWorkingDirectory().getAbsolutePath());
             viskitProject = new ViskitProject(projectDirectory);
        }
        else LOG.error("BLOCKER: run() findProjectWorkingDirectoryFromWithinThread() not successful, analyst reports will fail");
    }
    
    /** method name for reflection use */
    public static final String METHOD_run = "run";

    /** Execute the simulation for the desired number of replications */
    // TODO: Simkit not generisized yet
    @SuppressWarnings("unchecked")
    @Override
    public void run() // we are now in the simulation thread, commence thread complete
    {
        // https://stackoverflow.com/questions/442747/getting-the-name-of-the-currently-executing-method
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        if (!methodName.equals(METHOD_run))
            LOG.error("Reflection error: methodName=" + methodName + " does not match METHOD_run=" + METHOD_run);
        
        if (isDebugThread()) logThreadStatus(METHOD_run);
        
        fixThreadedName();
        LOG.info(assemblyName + " is now running inside BasicAssembly run() Simulation Run thread...");
        
         stopSimulationRun = false;
        pauseSimulationRun = false;
        
        if (projectDirectory == null) // don't test getWorkingDirectory() which likely produces NPE
        {
            findProjectWorkingDirectoryFromWithinThread(); // the great mouse hunt
        }
                
        if (Schedule.isRunning() && !Schedule.getCurrentEvent().getName().equalsIgnoreCase("Run")) // simkit
        {
            LOG.error("Assembly already running.");
        }

        // In case the user inputs bad parameters in the XML
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
                
                String msg = "Assembly run aborted.  <br/>"
                    + "Please navigate to " + ViskitUserConfiguration.VISKIT_ERROR_LOG.getAbsolutePath() + " <br/>"
                    + "and email the log to "
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

        if (reportStatisticsConfiguration == null) // time to initialize, using localViskitProject handed into thread
        {
            if (viskitProject == null)
                LOG.error("Incorrect initialization of BasicAssembly in thread context, localViskitProject is null");
            // Creates a ReportStatisticsConfiguration instance and names it based on the name of this Assembly.
            reportStatisticsConfiguration = new ReportStatisticsConfiguration(assemblyName, viskitProject);
        }
        // reset the document with existing parameters since it might have run before
        reportStatisticsConfiguration.reset();
        reportStatisticsConfiguration.setEntityIndex(entitiesWithStatisticsList);
        if (!hookupsCalled)
        {
            LOG.error("run() RuntimeException: performHookups() hasn't been called!");
            throw new RuntimeException("performHookups() hasn't been called!");
        }

        LOG.info("Planned simulation stop time: " + getStopTime());
        Schedule.stopAtTime(getStopTime()); // simkit
        Schedule.setEventSourceVerbose(true);   // simkit

        if (isSingleStep())
            Schedule.setSingleStep(isSingleStep()); // simkit
       
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

        runEntitiesSet = Schedule.getReruns(); // simkit

        Method setNumberReplicationsMethod;
        // Convenience for Diskit if on the classpath
        for (ReRunnable entity : runEntitiesSet)
        {
            // access the SM's numberOfReplications parameter setter for user
            // dynamic input to override the XML value
            if (entity.getName().contains("ScenarioManager")) 
            {
                scenarioManager = entity;
                try {
                    setNumberReplicationsMethod = scenarioManager.getClass().getMethod(METHOD_setNumberReplications, int.class);
                    setNumberReplicationsMethod.invoke(scenarioManager, getNumberReplicationsPlanned());
                } 
                catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | SecurityException | NoSuchMethodException ex) {
                    LOG.error("run() error during ScenarioManager checks: " + ex);
                }
            }
        }
        // TODO ensure using latest value SimulationRunPanel().getNumberReplications(), must be set outside of thread
//        setNumberReplicationsPlanned(ViskitGlobals.instance().getSimulationRunPanel().getNumberReplications());

        int runCount = runEntitiesSet.size();
        
        if (pausedReplicationNumber > 0)
        {
            initialReplicationNumber = pausedReplicationNumber;
            LOG.info("Resume running " + getName() + " simulation replication " + initialReplicationNumber +
                     " for {} planned replications total", getNumberReplicationsPlanned());
        }
        else
        {
            LOG.info("Begin running " + getName() + " simulation for {} planned replications total", getNumberReplicationsPlanned());
        }
        
        // here is the primary loop for each replication within the current simulation
        for (int replicationNumber = initialReplicationNumber; replicationNumber <= getNumberReplicationsPlanned(); replicationNumber++)
        {
            firePropertyChange("replicationNumber", (replicationNumber));
            // look ahead at next replication
            if (replicationNumber == getVerboseReplicationNumber())
            {
                Schedule.setVerbose(true);       // simkit
                Schedule.setReallyVerbose(true); // simkit
            } 
            else 
            {
                Schedule.setVerbose(isVerbose());       // simkit
                Schedule.setReallyVerbose(isVerbose()); // simkit
            }

            int nextRunCount = Schedule.getReruns().size(); // simkit
            if (nextRunCount != runCount) 
            {
                LOG.info("run() simkit.Schedule.getReruns() value changed, old: " + runCount + " new: " + nextRunCount);
                firePropertyChange("rerunCount", runCount, nextRunCount);
                runCount = nextRunCount;
            }
            
            // now look for interrupting signals into the thread
            
            if (stopSimulationRun)       // signal received within thread, we are inside replication loop
            {
                replicationNumber--; // at this point we are just prior to actually conducting the replication loop
                String stopMessage = "Threaded assembly simulation run() stopped after Replication # " + (replicationNumber);
                LOG.info(stopMessage);
                break;
            } 
            else if (pauseSimulationRun) // signal received within thread, we are inside replication loop
            {
                // the RUN has just gotten a PAUSE signal prior to starting/resuming/stepping another replication loop
                // setup for another single STEP or else resumed RUN
                pausedReplicationNumber = replicationNumber; // prepare for return by remembering how many replications were complete
                replicationNumber--; // at this point we are just prior to actually conducting the replication loop
                String pauseMessage = "Threaded assembly simulation run() paused after Replication # " + (replicationNumber);
                LOG.info(pauseMessage);
                
                return;
                
                // TODO is it important to return to regular vcrButton logic; or else rather 
                // - briefly sleep within this specific thread, 
                // - re-loop waiting for next button (STEP or RUN or STOP),
                // - perhaps a time-out popup every 100 loops to ask user if still there...
                // this avoids multiple pause/restart loop repair steps

                // TODO experimental: stop and wait here for resume, then restore loop before continuing...
//                try
//                {
//                    Thread.currentThread().wait(); // TODO sleeping likely unnecessary
//                }
//                catch (InterruptedException ie)
//                {
//                    Thread.currentThread().interrupt();
//                    LOG.error("PauseStepListener Thread.wait interruption");
//                }
            }
            else // continue running replications
            {
                // simkit execution consistency checks:
                if (Schedule.isRunning())   // simkit
                    LOG.error("run() replication #{} discrepancy: simkit.Schedule.isRunning() already, continuing anyway...",
                            replicationNumber);
                if (Schedule.isSingleStep()) // simkit
                    LOG.error("run() replication #{} discrepancy: simkit.Schedule.isSingleStep() already, continuing anyway...",
                            replicationNumber);
                
                // use pseudorandom number generator (RNG) to get next seed
                // https://en.wikipedia.org/Pseudorandom_number_generator
                seed = RandomVariateFactory.getDefaultRandomNumber().getSeed();
                String seedString = String.valueOf(seed);
                String indexSpacing = new String();
                if (replicationNumber <= 9)
                    indexSpacing = " ";
                String spacing = new String();
                if      (seedString.length() == 10)
                         spacing = " ";
                else if (seedString.length() == 9)
                         spacing = "  ";
                else if (seedString.length() == 8)
                         spacing = "   ";
                LOG.info("Simulation starting Replication #" + indexSpacing + (replicationNumber) + " with RNG seed state = " + spacing + seed
//                       + "\n     simulationState=" + simulationState // debug diagnostic, is thread seeing updates? NO
                );
                try {
                    Schedule.reset(); // simkit
                } 
                catch (ConcurrentModificationException cme) 
                {
                    LOG.error("run() error when attempting Schedule.reset(); " + cme.getMessage());
                    ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                            "Assembly Run Error",
                            cme + "\nSimulation will terminate"
                    );
                    
                    int newEventListId = Schedule.addNewEventList(); // simkit
                    Schedule.setDefaultEventList(Schedule.getEventList(newEventListId)); // simkit
                    for (SimEntity entity : simEntityArray) {
                        entity.setEventListID(newEventListId);
                    }
                    Schedule.stopSimulation(); // simkit
                    Schedule.clearRerun();     // simkit
                    runEntitiesSet.forEach(entity -> {
                        Schedule.addRerun(entity); // simkit
                    });
                } // end exception catch for Schedule.reset(); // simkit // end exception catch for Schedule.reset(); // simkit

                // now tell simkit to run the replication
                Schedule.startSimulation(); // simkit

                String typeStatistics, nodeType;
                int propertyIndex = 0;
                boolean isCountStatistic; // statistics property type, "count" or "mean"
                SampleStatistics sampleStatistics;
                Object pclObject;
                
                // Outer statistics output
                // # of PropertyChangeListenerNodes is == to replicationStatisticsPropertyChangeListenerArray.length
                if (pclNodeCache == null) // <- Headless mode
                    return;
                
                for (Map.Entry<String, AssemblyNode> entry : pclNodeCache.entrySet()) 
                {
                    LOG.debug("entry is: {}", entry);

                    pclObject = pclNodeCache.get(entry.getKey());
                    if (pclObject.getClass().toString().contains("PropertyChangeListenerNode")) {

                        try {
                            // Since the pclNodeCache was created under a previous ClassLoader
                            // we must use reflection to invoke the methods on the AssemblyNodes
                            // that it contains, otherwise we will throw ClassCastExceptions
                            nodeType = pclObject.getClass().getMethod(METHOD_getType).invoke(pclObject).toString();

                            // This is not a designPoint, so skip
                            if (nodeType.equals(ViskitStatics.SIMPLE_PROPERTY_DUMPER)) {
                                LOG.debug("SimplePropertyDumper encountered");
                                continue;
                            }
                            isCountStatistic = Boolean.parseBoolean(pclObject.getClass().getMethod(METHOD_isStatisticTypeCount).invoke(pclObject).toString());
                            typeStatistics = isCountStatistic ? ".count" : ".mean";
                            sampleStatistics = (SampleStatistics) BasicAssembly.this.getReplicationStatisticsPropertyChangeListenerArray()[propertyIndex];
                            fireIndexedPropertyChange(propertyIndex, sampleStatistics.getName(), sampleStatistics);

                            if (isCountStatistic)
                                fireIndexedPropertyChange(propertyIndex, sampleStatistics.getName() + typeStatistics, sampleStatistics.getCount());
                            else
                                fireIndexedPropertyChange(propertyIndex, sampleStatistics.getName() + typeStatistics, sampleStatistics.getMean());

                            propertyIndex++;
                        } 
                        catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
                            LOG.error("run() error during PropertyChangeListenerNode checks: " + ex);
                        }
                    }
                } // end for
                if (isPrintReplicationReportsToConsole()) {
                    printWriter.println(getReplicationReport(replicationNumber));
                    printWriter.flush();
                }
            } // continue running replications
        } // end of replication loop
        
        // pay attention locally, internal to simulation thread.  change to corresponding Viskit state occurs later.
        setSimulationState(SimulationState.DONE);
        LOG.info("All simulation replications now complete.");

        if (isPrintSummaryReportToConsole()) 
        {
            printWriter.println(getSummaryReport());
            printWriter.flush();
        }
        
        if (isEnableAnalystReports()) 
        {
            createAnalystReportFile();
            LOG.info("createAnalystReportFile()\n      " + 
                      analystReportFile.getAbsolutePath()); 
                
            analystReportModel = new AnalystReportModel(assemblyName, reportStatisticsConfiguration.saveStatisticsGetReportPath(), pclNodeCache);
                
            try {
                analystReportModel.writeToXMLFile(analystReportFile);
            }
            catch (Exception e)
            {
                LOG.error("analystReportModel.writeToXMLFile(analystReportFile) failed,\n      {}\n      {}",
                        analystReportFile.getAbsolutePath(), e);
            }
            
//            // TODO the following block appears to break ViskitGlobals singleton pattern!
//            // Because there is no instantiated report builder in the current
//            // thread context, we reflect here
//            ClassLoader localLoader = ViskitGlobals.instance().getViskitApplicationClassLoader();

// TODO better future fix, if possible?  move out of reflection land completely...

/*
            try 
            {
                
                // while in thread, do not invoke ViskitStatics!
                // isFileReady(analystReportFile);
                
                // while in thread, do not invoke ViskitStatics!
//                if (!isFileReady(analystReportFile))
//                {
//                    LOG.error("analystReportFile not ready");
//                }
                // TODO this line provokes the restart of singletons ViskitGlobals and ViskitUserConfiguration if used,
                // can we employ local reference?  moved workingClassLoaderinitialization to InternalAssemblyRunner, outside the separate thread
                // Class<?> clazz = ViskitGlobals.instance().getViskitApplicationClassLoader().loadClass("viskit.model.AnalystReportModel");// was localWorkingClassLoader
                if (localWorkingClassLoader == null) // being extra careful to report an unexpected error condition...
                {
                    LOG.error("run() error preparing for Analyst Report recovery: (clazz == null)");
                    return;
                }
                Class<?> clazz = localWorkingClassLoader.loadClass("viskit.model.AnalystReportModel");
                
                Constructor<?> arbConstructor = clazz.getConstructor(String.class, Map.class);
                
                Object arbObject = arbConstructor.newInstance(reportStatisticsConfiguration.saveStatisticsGetReportPath(), pclNodeCache);
                Method writeToXMLFileMethod = clazz.getMethod(METHOD_writeToXMLFile, File.class);
                writeToXMLFileMethod.invoke(arbObject, analystReportFile);
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                LOG.error("run() error during getWorkingClassLoader() and reflection checks: " + 
                        ex);
            }
            catch (Exception ue)
            {
                LOG.error("run() uncaught exception during getWorkingClassLoader() and reflection checks: " + ue);
            }
*/
        }
    }

    /**
     * This method gets called at the beginning of every simulation run,
     * building a tempFile that is saved on the path. That path is what
     * is used at the bottom of the run to write out the analyst report file. 
     * We report the path back to the caller immediately, and it is the 
     * caller's responsibility to dispose of the file once done with it.
     */
    private void createAnalystReportFile() //  intentionally duplicative, creating copy with timestamped filename
    {
        // TODO needs to match statistics file naming convention:
        analystReportFile = new File(viskitProject.getAnalystReportsDirectory(), 
                this.getName() + "_" + "AnalystReport" + "_" + ViskitStatics.todaysDate() + ".xml"); // 
//        LOG.info("createAnalystReportFile() new analyst report (duplicative):\n      " + analystReportFile.getAbsolutePath());
    }
    
    /** method name for reflection use, found in superclass */
    public static final String METHOD_setVerbose = "setVerbose";
    
    /** method name for reflection use, found in superclass */
    public static final String METHOD_isVerbose = "isVerbose";
    
    /** method name for reflection use */
    public static final String METHOD_setVerboseReplicationNumber = "setVerboseReplicationNumber";

    public void setVerboseReplicationNumber(int newVerboseReplicationNumber) {
        verboseReplicationNumber = newVerboseReplicationNumber;
    }
    
    /** method name for reflection use */
    public static final String METHOD_getVerboseReplicationNumber = "getVerboseReplicationNumber";

    public int getVerboseReplicationNumber() 
    {
        return verboseReplicationNumber;
    }
    
    /** method name for reflection use */
    public static final String METHOD_setPclNodeCache = "setPclNodeCache";

    public void setPclNodeCache(Map<String, AssemblyNode> pclNodeCache) 
    {
        this.pclNodeCache = pclNodeCache;
    }

    /**
     * Method which may be overridden by subclasses (e.g., ViskitAssembly) which will be called after
     * createObject() at run time.
     */
    abstract protected void printInfo();

    /**
     * @return the localWorkingClassLoader
     */
    public ClassLoader getWorkingClassLoader() {
        return localWorkingClassLoader;
    }

    /**
     * @param workingClassLoader the localWorkingClassLoader to set
     */
    public void setWorkingClassLoader(ClassLoader workingClassLoader) {
        this.localWorkingClassLoader = workingClassLoader;
    }

    /**
     * @return the localViskitProject
     */
    public final ViskitProject getViskitProject() {
        return viskitProject;
    }

    /**
     * @param localViskitProject the localViskitProject to set
     */
    public void setViskitProject(ViskitProject localViskitProject)
    {
        this.viskitProject = localViskitProject; // TODO does this value persist inside the thread context?
    }

    /**
     * @return the projectDirectory
     */
    private File getWorkingDirectory() {
        return projectDirectory;
    }

    /**
     * @param workingDirectory the projectDirectory to set
     */
    public void setWorkingDirectory(File workingDirectory) {
        this.projectDirectory = workingDirectory;
        if (workingDirectory == null)
        {
            LOG.error("setWorkingDirectory() received null value ");
        }
        else if (!workingDirectory.exists())
        {
            LOG.error("setWorkingDirectory() does not exist: " + workingDirectory.getAbsolutePath());
        }
    }

    /**
     * @return the debugThread
     */
    public boolean isDebugThread() {
        return debugThread;
    }

    /**
     * @param debugThread the debugThread to set
     */
    public void setDebugThread(boolean debugThread) {
        this.debugThread = debugThread;
    }
    private void logThreadStatus(String threadMethodInvocationMessage)
    {
        LOG.info("logThreadStatus(): {}", threadMethodInvocationMessage);
    }
    
    /**
     * @param newSimulationState the simulationState to set
     */
    public void setSimulationState(SimulationState newSimulationState) 
    {
        this.simulationState = newSimulationState;
        
        LOG.info("setSimulationState(): {}", newSimulationState); // TODO test, are we unreachable inside thread?
    }

} // end class file BasicAssembly.java