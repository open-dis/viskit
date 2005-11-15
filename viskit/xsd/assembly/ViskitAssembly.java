/*
 * ViskitAssembly.java
 *
 * Created on September 25, 2005, 1:44 PM
 *
 * BasicAssembly doesn't provide Viskit users with ability to 
 * reference a known SimEntity within a constructor. This class 
 * provides hooks into the BasicAssembly and enables named references
 * to instances within the design tool. 
 */

package viskit.xsd.assembly;

import simkit.Adapter;
import simkit.BasicAssembly;
import simkit.Schedule;
import simkit.SimEntity;
import simkit.stat.SampleStatistics;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;



/**
 * @version $Id$
 * @author Rick Goldberg
 */
public class ViskitAssembly extends BasicAssembly { 
    
    public LinkedHashMap entities;
    public LinkedHashMap replicationStatistics;
    public LinkedHashMap designPointStatistics;
    public LinkedHashMap propertyChangeListeners;
    public LinkedHashMap propertyChangeListenerConnections;
    public LinkedHashMap designPointStatsListenerConnections;
    public LinkedHashMap replicationStatsListenerConnections;
    public LinkedHashMap simEventListenerConnections;
    public LinkedHashMap adapters;
    private static boolean debug = false;
    
    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly() {
        
    }  
    
    public synchronized void createObjects() {
        entities = new LinkedHashMap();
        replicationStatistics = new LinkedHashMap();
        designPointStatistics = new LinkedHashMap();
        propertyChangeListeners = new LinkedHashMap();
        propertyChangeListenerConnections = new LinkedHashMap();
        designPointStatsListenerConnections = new LinkedHashMap();
        replicationStatsListenerConnections = new LinkedHashMap();
        simEventListenerConnections = new LinkedHashMap();
        adapters = new LinkedHashMap();
        super.createObjects();
    }
    
    public synchronized void performHookups() {
        super.performHookups();
    }
    
    public void hookupReplicationListeners() {
        String[] listeners = (String[]) replicationStatsListenerConnections.keySet().toArray(new String[0]);
        for ( int i = 0; i < listeners.length; i++ ) {
            LinkedList repStatsConnects = (LinkedList) replicationStatsListenerConnections.get(listeners[i]);
            if ( repStatsConnects != null ) {
                ListIterator li = repStatsConnects.listIterator();
                while ( li.hasNext() ) {
                    PropertyConnector pc = (PropertyConnector) li.next();
                    connectReplicationStats(listeners[i], pc);
                }
            }
 
        }
        
    }
    
    public void hookupSimEventListeners() {
        String[] listeners = (String[]) simEventListenerConnections.keySet().toArray(new String[0]);
        if(debug) {
            System.out.println("hookupSimEventListeners called " + listeners.length);
        }
        for ( int i = 0; i < listeners.length; i++ ) {
            LinkedList simEventListenerConnects = (LinkedList)simEventListenerConnections.get(listeners[i]);
            if ( simEventListenerConnects != null ) {
                ListIterator li = simEventListenerConnects.listIterator();
                while( li.hasNext() ) {
                    String source = (String)li.next();
                    connectSimEventListener(listeners[i],source);
                    if(debug) {
                        System.out.println("hooking up SimEvent source " + source + " to listener " + listeners[i]);
                    }
                }
            }
            
        }

    }
    
    public void hookupPropertyChangeListeners() {
        String[] listeners = (String[]) propertyChangeListenerConnections.keySet().toArray(new String[0]);
        for ( int i = 0; i < listeners.length; i++ ) {
            LinkedList propertyConnects = (LinkedList) propertyChangeListenerConnections.get(listeners[i]);
            if ( propertyConnects != null ) {
                ListIterator li = propertyConnects.listIterator();
                while ( li.hasNext() ) {
                    PropertyConnector pc = (PropertyConnector) li.next();
                    connectPropertyChangeListener(listeners[i], pc);
                }
            }
        }
    }
    
    public void hookupDesignPointListeners() {
        super.hookupDesignPointListeners();
        String[] listeners = (String[]) designPointStatsListenerConnections.keySet().toArray(new String[0]);
        // if not the default case, need to really do this with
        // a Class to create instances selected by each ReplicationStats listener.
        if (listeners.length > 0) {
            for ( int i = 0; i < listeners.length; i++ ) {
                LinkedList designPointConnects = (LinkedList) designPointStatsListenerConnections.get(listeners[i]);
                if ( designPointConnects != null ) {
                    ListIterator li = designPointConnects.listIterator();
                    while ( li.hasNext() ) {
                        PropertyConnector pc = (PropertyConnector) li.next();
                        connectDesignPointStats(listeners[i], pc);
                    }
                }
            }
        }
    }
    
    void connectSimEventListener(String listener, String source) {
        getSimEntityByName(source).addSimEventListener(getSimEntityByName(listener));
    } 
    
    void connectPropertyChangeListener(String listener, PropertyConnector pc) {
        if ( debug ) System.out.println("connecting entity " + pc.source + "to " + listener + " property " + pc.property );
        getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getPropertyChangeListenerByName(listener));
    }
    
    void connectReplicationStats(String listener, PropertyConnector pc) {
        if ( debug ) System.out.println("connecting entity " + pc.source + "to " + listener + " property " + pc.property );
        if ( pc.property.equals(null) ) {
            pc.property = ((SampleStatistics) (getReplicationStatsByName(listener))).getName();
        }
        
        if ( pc.property.equals(null) ) {
            getSimEntityByName(pc.source).addPropertyChangeListener(getReplicationStatsByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getReplicationStatsByName(listener));
        }
    }
    
    void connectDesignPointStats(String listener, PropertyConnector pc) {
        if ( pc.property.equals(null) ) {
            pc.property = ((SampleStatistics) (getDesignPointStatsByName(listener))).getName();
        }
        
        if ( pc.property.equals(null) ) {
            getSimEntityByName(pc.source).addPropertyChangeListener(getDesignPointStatsByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getDesignPointStatsByName(listener));
        }
        
    }
    

    /** to be called after all entities have been added as a super() */
    /*  note not using template version of ArrayList... */
    public void createSimEntities() {
        simEntity = 
                (SimEntity[]) entities.values().toArray(new SimEntity[0]);
    }
    
    public void createDesignPointStats() {
        super.createDesignPointStats();
        // to be consistent; should be getting the designPointStats from 
        // the super. 
        
        for ( int i = 0 ; i < designPointStats.length; i ++ ) {
            designPointStatistics.put(designPointStats[i].getName(),designPointStats[i]);
        }
    }
    
    public void createReplicationStats() {
        replicationStats = 
                (SampleStatistics[]) replicationStatistics.values().toArray(new SampleStatistics[0]);
        for ( int i = 0; debug && i < replicationStats.length; i++ ) {
            System.out.println(replicationStats[i].getName());
        }
    }
    
    public void createPropertyChangeListeners() {
        propertyChangeListener = 
                (PropertyChangeListener[]) propertyChangeListeners.values().toArray(new PropertyChangeListener[0]);
        for ( int i = 0; debug && i < propertyChangeListener.length; i++ ) {
            System.out.println(propertyChangeListener[i]);
        }
    }
    
    public void addSimEntity(String name, SimEntity entity) {
        entity.setName(name);
        entities.put(name,entity);
    }
    
    public void addDesignPointStats(String listenerName, PropertyChangeListener pcl) {
        designPointStatistics.put(listenerName,pcl);
    }
   
    public void addReplicationStats(String listenerName, PropertyChangeListener pcl) {
        replicationStatistics.put(listenerName,pcl);
    }
    
    public void addPropertyChangeListener(String listenerName, PropertyChangeListener pcl) {
        propertyChangeListeners.put(listenerName,pcl);
    }
    
    public void addPropertyChangeListenerConnection(String listener, String property, String source) {
        LinkedList propertyConnects = (LinkedList)(propertyChangeListenerConnections.get(listener));
        if ( propertyConnects == null ) {
            propertyConnects = new LinkedList();
            propertyChangeListenerConnections.put(listener,propertyConnects);
        }
        propertyConnects.add(new PropertyConnector(property,source));
    }
    
    public void addDesignPointStatsListenerConnection(String listener, String property, String source) {
        LinkedList designPointConnects = (LinkedList)(designPointStatsListenerConnections.get(listener));
        if ( designPointConnects == null ) {
            designPointConnects = new LinkedList();
            designPointStatsListenerConnections.put(listener,designPointConnects);
        }
        designPointConnects.add(new PropertyConnector(property,source));
    }
        
    public void addReplicationStatsListenerConnection(String listener, String property, String source) {
        LinkedList repStatsConnects = (LinkedList)(replicationStatsListenerConnections.get(listener));
        if ( repStatsConnects == null ) {
            repStatsConnects = new LinkedList();
            replicationStatsListenerConnections.put(listener,repStatsConnects);
        }
        repStatsConnects.add(new PropertyConnector(property,source));
    } 
    
    public void addSimEventListenerConnection(String listener, String source) {
        LinkedList simEventListenerConnects = (LinkedList)(simEventListenerConnections.get(listener));
        if ( simEventListenerConnects == null ) {
            simEventListenerConnects = new LinkedList();
            simEventListenerConnections.put(listener, simEventListenerConnects);
        }
        if ( debug ) {
            System.out.println("addSimEventListenerConnection source " + source + " to listener " + listener );
        }
        simEventListenerConnects.add(source);
    }
    
    public void addAdapter(String name, String heard, String sent, String from, String to) {
        Adapter a = new Adapter(heard,sent);
        a.connect(getSimEntityByName(from),getSimEntityByName(to));
        adapters.put(name,a);
        entities.put(name,a);
    }
    
    public PropertyChangeListener getPropertyChangeListenerByName(String name) {
        return (PropertyChangeListener) propertyChangeListeners.get(name);
    }
    
    public SampleStatistics getDesignPointStatsByName(String name) {
        return (SampleStatistics) designPointStatistics.get(name);
    }
    
    public SampleStatistics getReplicationStatsByName(String name) {
        return (SampleStatistics) replicationStatistics.get(name);
    }
    
    public SimEntity getSimEntityByName(String name) {
        return (SimEntity) entities.get(name);
    }
    
    /* not exactly sure if this is needed when a property is set as in the constructor
     * of a SampleStatisics type 
     */
    class PropertyConnector {
        String property;
        String source;
        
        PropertyConnector(String p, String s) {
            this.property = p;
            this.source = s;
        }
    }

  /*
   * 14 NOV 05, the run method is lifted straight from the parent class, BasicAssembly.
   * The reason for the duplication is to allow the gui a bit of control over
   * starting and stopping the run.
   * A few booleans have been added to remember run state.
  */

    protected boolean stopRun;

    public void setStopRun(boolean wh)
    {
        stopRun = wh;
        if(stopRun == true)
          Schedule.stopSimulation();
    }

    protected int startRepNumber = 0;

    /**
     * Execute the simulation for the desired number of replications.
     */
    public void run() {
        stopRun = false;

        if (!hookupsCalled) {
            throw new RuntimeException("performHookups() hasn't been called!");
        }

        Schedule.stopAtTime(getStopTime());
        if (isVerbose()) {
            Schedule.setVerbose(isVerbose());
        }
        if (isSingleStep()) {
            Schedule.setSingleStep(isSingleStep());
        }

        if (isSaveReplicationData()) {
            replicationData.clear();
            for (int i = 0; i < replicationStats.length; ++i) {
                replicationData.put(new Integer(i), new ArrayList());
            }
        }
        int replication;
        for (replication = startRepNumber;
             !stopRun && (replication < getNumberReplications());
             ++replication) {

            maybeReset();
            Schedule.startSimulation();

            for (int i = 0; i < replicationStats.length; ++i) {
                fireIndexedPropertyChange(i, replicationStats[i].getName(), replicationStats[i]);
                fireIndexedPropertyChange(i, replicationStats[i].getName() + ".mean", replicationStats[i].getMean());
            }
            if (isPrintReplicationReports()) {
                System.out.println(getReplicationReport(replication));
            }
            if (isSaveReplicationData()) {
                saveReplicationStats();
            }
        }

        if (isPrintSummaryReport()) {
            System.out.println(getSummaryReport());
        }

        saveState(replication);
    }

    private void saveState(int lastRepNum) {
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
     */
    private void maybeReset() {
        // We reset if we're not in the middle of a run
        if (Schedule.getDefaultEventList().isFinished())
            Schedule.reset();
    }

    /**
     * Called by GUI instead of Schedule.reset(); resets rep loop, too
     */
    public void reset()
    {
      super.reset();
      startRepNumber = 0;
    }
}
