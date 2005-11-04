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

import simkit.*;
import simkit.stat.*;
import java.util.*;
import java.beans.PropertyChangeListener;



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
    boolean debug = false;
    
    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly() {
        
    }  
    
    public void createObjects() {
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
    
    public void performHookups() {
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
        for ( int i = 0; i < listeners.length; i++ ) {
            String source = (String) simEventListenerConnections.get(listeners[i]);
            connectSimEventListener(listeners[i], source);
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
        System.out.println("connecting entity " + pc.source + "to " + listener + " property " + pc.property );
        getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getPropertyChangeListenerByName(listener));
    }
    
    void connectReplicationStats(String listener, PropertyConnector pc) {
        System.out.println("connecting entity " + pc.source + "to " + listener + " property " + pc.property );
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
        System.out.println("Replication stats creating");
        replicationStats = 
                (SampleStatistics[]) replicationStatistics.values().toArray(new SampleStatistics[0]);
        for ( int i = 0; debug && i < replicationStats.length; i++ ) {
            System.out.println(replicationStats[i].getName());
        }
    }
    
    public void createPropertyChangeListeners() {
        System.out.println("PropertyChangeListener creating");
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
        simEventListenerConnections.put(listener,source);
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
}
