package viskit.xsd.assembly;


import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JOptionPane;
import simkit.BasicSimEntity;
import simkit.Priority;
import simkit.Schedule;
import simkit.SimEntity;
import simkit.SimEvent;
import simkit.random.RandomVariateFactory;
import simkit.stat.SampleStatistics;
import simkit.stat.SavedStats;
import simkit.stat.SimpleStatsTally;

import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import viskit.xsd.assembly.GridletEventList;
import viskit.xsd.bindings.assembly.SimkitAssembly;

/**
 * Base class for creating Simkit scenarios.
 * Modified to be BeanShellable and Viskit VCR aware - rmgoldbe, jmbailey
 *
 * @author ahbuss
 * @version $Id$
 */
public abstract class BasicAssembly extends BasicSimEntity implements Runnable
{

  protected LinkedHashMap replicationData;
  protected SampleStatistics[] replicationStats;
  protected SampleStatistics[] designPointStats;
  protected SimEntity[] simEntity;
  protected PropertyChangeListener[] propertyChangeListener;
  protected boolean hookupsCalled;
  protected boolean stopRun;
  protected int startRepNumber = 0;
  protected Set<SimEntity> runEntities;

  private double stopTime;
  private boolean verbose;
  private boolean singleStep;
  private int numberReplications;

  private boolean printReplicationReports;
  private boolean printSummaryReport;
  private boolean saveReplicationData;

  private File analystReportFile;     // where file gets written

  /**
   * ************************************************
   * TODO MIKE: Wire boolean filters to AnalystReport GUI
   * *************************************************
   */
  private boolean enableAnalystReports = true;
  private boolean analystReplicationData = true;
  private boolean analystSummaryData = true;
  private boolean generateAnalystReport = true;
  private AnalystReportBuilder reportBuilder;
  /**
   * ***********************************************
   */


  private ReportStatisticsConfig statsConfig;

  private int designPointID;

  private DecimalFormat form;

  private LinkedList entitiesWithStats;
  
  private ByteArrayOutputStream outputBuffer;

  private PrintWriter println;

  private int verboseReplicationNumber;

  /**
   * Default constructor sets paameters of BasicAssembly to their
   * default values.  These are:
   * <pre>
   * printReplicationReports = false
   * printSummaryReport = true
   * saveReplicationData = false
   * numberReplications = 1
   * </pre>
   */
  public BasicAssembly()
  {
    form = new DecimalFormat("0.0000");
    setPrintReplicationReports(false);
    setPrintSummaryReport(true);
    replicationData = new LinkedHashMap();
    simEntity = new SimEntity[0];
    replicationStats = new SampleStatistics[0];
    designPointStats = new SampleStatistics[0];
    propertyChangeListener = new PropertyChangeListener[0];
    setNumberReplications(1);
    hookupsCalled = false;

    initReportFile();    // Creates the temp file
    //Creates a report stats config object and names it based on the name of this
    //Assembly.
    //TODO MIKE: instead of this.getName() We may not need to worry about the name of
    //the stats report file. Should discuss though.
    statsConfig = new ReportStatisticsConfig(this.getName());
    outputBuffer = new ByteArrayOutputStream();
    println = new PrintWriter(System.out);
    //moved to run() to avoid beanshell upcall error
    //createObjects();
    //performHookups();
  }

  /**
   * <p>Resets all inner stats.  State resetting for SimEntities is their
   * responsibility.  Outer stats are not reset.
   */
  public void reset()
  {
    super.reset();
    for (int i = 0; i < replicationStats.length; ++i) {
      replicationStats[i].reset();
    }
    startRepNumber = 0;
  }

  // mask the Thread run() 
  public void doRun() {
      setPersistant(false);
  }
  /**
   * Create all the objects used.  This is called from the constructor.
   * The <code>createSimEntities()</code> method is abstract and will
   * be implemented in the concrete subclass.  The others are empty by
   * default.  The <code>createReplicationStats()</code> method must be
   * overridden if any replications stats are needed.
   */
  protected void createObjects()
  {
    createSimEntities();
    createReplicationStats();
    createDesignPointStats();
    createPropertyChangeListeners();
  }

  /**
   * Call all the hookup methods.
   */
  protected void performHookups()
  {
    hookupSimEventListeners();
    hookupReplicationListeners();
    hookupDesignPointListeners();
    hookupPropertyChangeListeners();
    hookupsCalled = true;
  }

  /**
   * Received the replicationStatistics Linked Hash Map from ViskitAssembly. This
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
   * TODO: Remove the naming convention requirement and has the SimEntityName be
   * an automated key value
   */
  protected void setStatisticsKeyValues(LinkedHashMap repStatistics)
  {
    Iterator itr = repStatistics.entrySet().iterator();
    entitiesWithStats = new LinkedList();
    while (itr.hasNext()) {
      Map.Entry entry = (Map.Entry) itr.next();
      entitiesWithStats.add(entry.getKey().toString());
      println.println(entry.getKey().toString());
      println.flush();
    }
    statsConfig.setEntityIndex(entitiesWithStats);

  }

  protected abstract void createSimEntities();

  protected abstract void hookupSimEventListeners();

  protected abstract void hookupReplicationListeners();

  /**
   * This method is left concrete so subclasses don't have to worry about
   * it if no additional PropertyChangeListeners are desired.
   */
  protected void hookupPropertyChangeListeners()
  {
  }

  /**
   * The default behavior is to create a <code>SimplStatsTally</code>
   * instance for each element in <code>replicationStats</code> with the
   * corresponding name + "mean".
   */
  protected void createDesignPointStats()
  {
    designPointStats = new SampleStatistics[replicationStats.length];
    for (int i = 0; i < designPointStats.length; ++i) {
      designPointStats[i] = new SimpleStatsTally(replicationStats[i].getName() + ".mean");
    }
  }

  protected void createReplicationStats()
  {
  }

  protected void createPropertyChangeListeners()
  {
  }

  /**
   * Set up all outer stats propertyChangeListeners
   */
  protected void hookupDesignPointListeners()
  {
    for (int i = 0; i < designPointStats.length; ++i) {
      this.addPropertyChangeListener(designPointStats[i]);
    }
  }


  public void setStopTime(double time)
  {
    if (time < 0.0) {
      throw new IllegalArgumentException("Stop time must be >= 0.0: " + time);
    }
    stopTime = time;
    System.out.println("stopTime: "+time);
  }

  public double getStopTime()
  {
    return stopTime;
  }

  public void setVerbose(boolean b)
  {
    verbose = b;
  }

  public boolean isVerbose()
  {
    return verbose;
  }

  public void setSingleStep(boolean b)
  {
    singleStep = b;
  }

  public boolean isSingleStep()
  {
    return singleStep;
  }

  public void setStopRun(boolean wh)
  {
    stopRun = wh; //?
    if (stopRun == true)
      Schedule.stopSimulation();
  }

  private void saveState(int lastRepNum)
  {
    boolean midRun = !Schedule.getDefaultEventList().isFinished();
    boolean midReps = lastRepNum < getNumberReplications();

    if (midReps) {
      // middle of some rep, fell out because of GUI stop
      startRepNumber = lastRepNum;
    }
    else if (!midReps && !midRun) {
      // done with all reps
      startRepNumber = 0;
    }
    else if (!midReps && midRun) {
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
  private void maybeReset()
  {
    // We reset if we're not in the middle of a run

    // but, isFinished didn't happen for the 0th
    // replication
    if (Schedule.getDefaultEventList().isFinished())
      
            try {
                Schedule.reset();
            } catch (java.util.ConcurrentModificationException cme) {
                System.out.println("Maybe not finished in Event List "+Schedule.getDefaultEventList().getID());
                
            }
  }

  public void setNumberReplications(int num)
  {
    if (num < 1) {
      throw new IllegalArgumentException("Number replications must be > 0: " + num);
    }
    numberReplications = num;
  }

  public int getNumberReplications()
  {
    return numberReplications;
  }

  public void setPrintReplicationReports(boolean b)
  {
    printReplicationReports = b;
  }

  public boolean isPrintReplicationReports()
  {
    return printReplicationReports;
  }

  public void setPrintSummaryReport(boolean b)
  {
    printSummaryReport = b;
  }

  public boolean isPrintSummaryReport()
  {
    return printSummaryReport;
  }

  public String getAnalystReportPath()
  {
    return analystReportFile.getAbsolutePath();
  }

  public void setDesignPointID(int id)
  {
    designPointID = id;
  }

  public int getDesignPointID()
  {
    return designPointID;
  }

  public void setSaveReplicationData(boolean b)
  {
    saveReplicationData = b;
  }

  public boolean isSaveReplicationData()
  {
    return saveReplicationData;
  }

  /**
   * Empty, needed to implement SimEntity
   */
  public void handleSimEvent(SimEvent simEvent)
  {
  }

  /**
   * Empty, needed to implement SimEntity
   */
  public void processSimEvent(SimEvent simEvent)
  {
  }

  public SampleStatistics[] getDesignPointStats()
  {
    return (SampleStatistics[]) designPointStats.clone();
  }

  public SampleStatistics[] getReplicationStats(int id)
  {
    SampleStatistics[] stats = null;
    ArrayList reps = (ArrayList) replicationData.get(new Integer(id));
    if (reps != null) {
      stats = (SampleStatistics[]) reps.toArray(new SampleStatistics[0]);
    }
    return stats;
  }

  public SampleStatistics getReplicationStat(String name, int replication)
  {
    SampleStatistics stats = null;
    int id = getIDforReplicationStateName(name);
    if (id >= 0) {
      stats = getReplicationStats(id)[replication];
    }
    return stats;
  }

  public int getIDforReplicationStateName(String state)
  {
    int id = -1;
    for (int i = 0; i < replicationStats.length; ++i) {
      if (replicationStats[i].getName().equals(state)) {
        id = i;
        break;
      }
    }
    return id;
  }

  public Map getReplicationData()
  {
    return new LinkedHashMap(replicationData);
  }

  /**
   * Save all replicationStats for a given iteration.  This assumes that the
   * replicationData map has been instntaied and initialized.
   */
  protected void saveReplicationStats()
  {
    for (int i = 0; i < replicationStats.length; ++i) {
      ArrayList reps = (ArrayList) replicationData.get(new Integer(i));
      reps.add(new SavedStats(replicationStats[i]));
    }
  }

  /**
   * For each inner stats, print name, count, min, max, mean, variance, and
   * standard deviation.  This can be done generically.
   *
   * @param rep The replication number for this report
   */
  protected String getReplicationReport(int rep)
  {
    StringBuffer buf = new StringBuffer("Output Report for Replication #");
    buf.append(rep + 1);

    //TODO MIKE: outputs replication data on the fly not best location but it works
    if (analystReplicationData) statsConfig.processReplicationReport(rep + 1, replicationStats);

    for (int i = 0; i < replicationStats.length; ++i) {
      buf.append(System.getProperty("line.separator"));
      buf.append(replicationStats[i].getName());
      buf.append('[');
      buf.append(i);
      buf.append(']');
      buf.append('\t');
      buf.append(replicationStats[i].getCount());
      buf.append('\t');
      buf.append(form.format(replicationStats[i].getMinObs()));
      buf.append('\t');
      buf.append(form.format(replicationStats[i].getMaxObs()));
      buf.append('\t');
      buf.append(form.format(replicationStats[i].getMean()));
      buf.append('\t');
      buf.append(form.format(replicationStats[i].getVariance()));
      buf.append('\t');
      buf.append(form.format(replicationStats[i].getStandardDeviation()));
      replicationStats[i].reset();
    }
    return buf.toString();
  }

  /**
   * For each outer stats, print name, count, min, max, mean, variance, and
   * standard deviation.  This can be done generically.
   */
  protected String getSummaryReport()
  {
    StringBuffer buf = new StringBuffer("Summary Output Report:");
    buf.append(System.getProperty("line.separator"));
    buf.append(super.toString());
    for (int i = 0; i < designPointStats.length; ++i) {
      buf.append(System.getProperty("line.separator"));
      buf.append(designPointStats[i]);
    }
    return buf.toString();
  }

  /**
   * These are the actual SimEnties in the array, but the array itself is
   * a copy.
   *
   * @return the SimEntities in this scenario in a copy of the array.
   */
  public SimEntity[] getSimEntities()
  {
    return (SimEntity[]) simEntity.clone();
  }

  public void setOutputStream(OutputStream os) {
      PrintStream out = new PrintStream(os);
      this.println = new PrintWriter(os);
      Schedule.setOutputStream(out);
      // tbd, need a way to not use System.out as
      // during multi-threaded runs, some applications
      // send debug message directy to System.out.
      // ie, one thread sets System.out then another
      // takes it mid thread.
      System.setOut(out);
  }
  
  /**
   * Execute the simulation for the desired number of replications.
   */
  public void run()
  {
    stopRun = false;
    if (Schedule.isRunning() && !Schedule.getCurrentEvent().getName().equals("Run")) {
        System.out.println("Already running.");
        //Schedule.stopSimulation();
    }
    System.out.println("stopTime set at "+getStopTime());
    createObjects();
    performHookups();
    // reset the document with
    // existing parameters
    // might have run before
    statsConfig.reset();
    statsConfig.setEntityIndex(entitiesWithStats);
    if (!hookupsCalled) {
      throw new RuntimeException("performHookups() hasn't been called!");
    }
    System.out.println("Stopping at "+getStopTime());
    Schedule.stopAtTime(getStopTime());
    Schedule.setEventSourceVerbose(true);
    Schedule.setVerbose(isVerbose());
    if (isSingleStep()) {
      Schedule.setSingleStep(isSingleStep());
    }
    if (isSaveReplicationData()) {
      replicationData.clear();
      for (int i = 0; i < replicationStats.length; ++i) {
        replicationData.put(new Integer(i), new ArrayList());
      }
    }
    
    SimEntity timer = null;
    runEntities = Schedule.getReruns();
    for ( SimEntity entity:runEntities ) {
        if ( entity.getName().equals("Clock") || entity.getName().equals("DISPinger") ) {
            timer = entity;
        }
    }
    int runCount = runEntities.size();
    boolean clockChecker = false; // tbd hook this up and fix properly
    for (int replication = 0; replication < getNumberReplications(); replication++) {
      if (clockChecker && getVerboseReplication() >= 0) {
          timer.waitDelay("Stop",0.0);
      }
      if (replication == getVerboseReplication()) {
          Schedule.setVerbose(true);
          Schedule.setReallyVerbose(true);
          if (clockChecker) timer.waitDelay("Ping",0.0);
      } else {
          Schedule.setVerbose(isVerbose());
          Schedule.setReallyVerbose(isVerbose());
      }
      int nextRunCount = Schedule.getReruns().size();
      if (nextRunCount != runCount) {
          System.out.println("Reruns changed old: "+runCount+" new: "+nextRunCount);
          firePropertyChange("rerunCount",runCount,nextRunCount);
          runCount = nextRunCount;
          // print out new reRuns
          System.out.println("ReRun entities added since startup: ");
          for (SimEntity entity:Schedule.getDefaultEventList().getRerun()) {
              if ( !runEntities.contains(entity) ) {
                  System.out.print(entity.getName()+" ");
              }
          }
          
      }
      if(stopRun) {
          System.out.println("Stopped in Replication# "+replication+1);
          
        break;
      } else {
          firePropertyChange("seed",new Long(simkit.random.RandomVariateFactory.getDefaultRandomNumber().getSeed()));
          if (Schedule.isRunning()) {
              System.out.println("Already running.");
          }
          System.out.println("Starting Replication #"+(replication+1));
          try {
              Schedule.reset();
          } catch (java.util.ConcurrentModificationException cme) {
              JOptionPane.showMessageDialog(null,"Viskit has detected a possible error condition in the simulation entities. \nIt is possible that one of the entities is instancing a SimEntity type unsafely, \nplease check that any internally created entities are handled appropriately. \nYou'll probably have to restart Viskit, however Viskit will now try to swap in\n a new EventList for debugging purposes only.");
              int newEventListId = Schedule.addNewEventList();
              Schedule.setDefaultEventList(Schedule.getEventList(newEventListId));
              for (SimEntity entity:simEntity) {
                  entity.setEventListID(newEventListId);
              }
              Schedule.setReallyVerbose(true);
              Schedule.stopSimulation();
              Schedule.clearRerun();
              for (SimEntity entity:runEntities) {
                  Schedule.addRerun(entity);
              }
              //Schedule.reset();
              
          }
          
          Schedule.startSimulation();
          
          
          for (int i = 0; i < replicationStats.length; ++i) {
              fireIndexedPropertyChange(i, replicationStats[i].getName(), replicationStats[i]);
              fireIndexedPropertyChange(i, replicationStats[i].getName() + ".mean", replicationStats[i].getMean());
          }
          if (isPrintReplicationReports()) {
              println.println(getReplicationReport(replication));
              println.flush();
          }
          if (isSaveReplicationData()) {
              saveReplicationStats();
          }

          //Schedule.stopSimulation();
          System.runFinalization();
          System.gc();
      }
    } // for

    if (isPrintSummaryReport()) {
      println.println(getSummaryReport());
      println.flush();
    }
    

    //TODO MIKE: Wire the following to the analyst report GUI somehow
    if (enableAnalystReports && analystSummaryData)
      statsConfig.processSummaryReport(designPointStats);
    if (enableAnalystReports && generateAnalystReport) {
      //statsConfig.saveData();
      reportBuilder = new AnalystReportBuilder(statsConfig.getReport());
      try {
        reportBuilder.writeToXMLFile(analystReportFile);
      }
      catch (Exception e) {
        System.err.println("BasicAssembly can't write analyst report XML to " + analystReportFile.getAbsolutePath());
        System.err.println(e.getMessage());
      }
    }
    System.gc();
    System.runFinalization();
    //saveState(replication);
    
    //}
  }
  
  public void pause() {
      Schedule.pause();
  }
  
  public void resume() {
      Schedule.startSimulation();
  }
  
  // this is getting called by the Assembly Runner stop
  // button, which may get called on startup.
  public void stop() {
      //Schedule.stopSimulation();
      //Schedule.reset(); //?
      stopRun=true;
  }
  
  public void setEnableAnalystReports(boolean enable) {      
           enableAnalystReports = enable;
  }

  /**
   * This gets called at the top of every run.  It builds a tempFile and saves the path.  That path is what
   * is used at the bottom of run to write out the analyst report file.  We report the path back to the caller
   * immediately, and it is the caller's responsibility to dispose of the file once he is done with it.
   */
  private void initReportFile()
  {
    try {
      analystReportFile = File.createTempFile("ViskitAnalystReport", ".xml");
    }
    catch (IOException e) {
      analystReportFile = null;
      System.err.println("Error creating AnalystReport file: " + e.getMessage());
    }
  }

    public void setVerboseReplication(int i) {
       this.verboseReplicationNumber = i;
    }

    public int getVerboseReplication() {
        return verboseReplicationNumber;
    }
}
