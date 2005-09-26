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
import java.lang.reflect.Constructor;

/**
 *
 * @author Rick Goldberg
 */
public class ViskitAssembly extends BasicAssembly { 
    
    protected LinkedHashMap entities;
    protected LinkedHashMap replicationStatistics;
    protected LinkedHashMap designPointStatistics;
    protected LinkedHashMap propertyChangeListeners;
    protected LinkedHashMap propertyChangeListenerConnections;
    protected LinkedHashMap designPointStatsListenerConnections;
    protected LinkedHashMap replicationStatsListenerConnections;
    protected LinkedHashMap simEventListenerConnections;
    
    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly() {
        entities = new LinkedHashMap();
        replicationStatistics = new LinkedHashMap();
        designPointStatistics = new LinkedHashMap();
        propertyChangeListeners = new LinkedHashMap();
        simEventListenerConnections = new LinkedHashMap();
        designPointStatsListenerConnections = new LinkedHashMap();
        replicationStatsListenerConnections = new LinkedHashMap();
        
    }
    
    public void hookupReplicationListeners() {
        String[] listeners = (String[])(new ArrayList(replicationStatsListenerConnections.keySet())).toArray();
        for ( int i = 0; i < listeners.length; i++ ) {
            PropertyConnector pc = (PropertyConnector)replicationStatsListenerConnections.get(listeners[i]);
            connectReplicationStat(listeners[i], pc.source);
        }
        
    }
    
    public void hookupSimEventListeners() {
        String[] listeners = (String[])(new ArrayList(simEventListenerConnections.keySet())).toArray();
        for ( int i = 0; i < listeners.length; i++ ) {
            String source = (String) simEventListenerConnections.get(listeners[i]);
            connectSimEventListener(listeners[i], source);
        }
        
    }
    
    public void hookupPropertyChangeListeners() {
        String[] listeners = (String[])(new ArrayList(propertyChangeListenerConnections.keySet())).toArray();
        for ( int i = 0; i < listeners.length; i++ ) {
            PropertyConnector pc = (PropertyConnector)propertyChangeListenerConnections.get(listeners[i]);
            connectPropertyChangeListener(listeners[i], pc.source);
        }
    }
    
    public void hookupDesignPointListeners() {
        String[] listeners = (String[])(new ArrayList(designPointStatsListenerConnections.keySet())).toArray();
        for ( int i = 0; i < listeners.length; i++ ) {
            PropertyConnector pc = (PropertyConnector)designPointStatsListenerConnections.get(listeners[i]);
            connectDesignPointStat(listeners[i], pc.source);
        }
        
    }
    
    void connectSimEventListener(String listener, String source) {
        getEntityByName(source).addSimEventListener(getEntityByName(listener));
    } 
    
    void connectPropertyChangeListener(String listener, String source) {
        getEntityByName(source).addPropertyChangeListener(getPropertyChangeListenerByName(listener));
    }
    
    void connectReplicationStat(String listener, String source) {
        getEntityByName(source).addPropertyChangeListener(getReplicationStatByName(listener));
    }
    
    void connectDesignPointStat(String listener, String source) {
        getEntityByName(source).addPropertyChangeListener(getDesignPointStatByName(listener));
    }
    

    /** to be called after all entities have been added as a super() */
    /*  note not using template version of ArrayList... */
    public void createSimEntities() {
        simEntity = (SimEntity[])(new ArrayList(entities.values())).toArray();
    }
    
    public void createDesignPointStats() {
        designPointStats = (SampleStatistics[]) (new ArrayList(designPointStatistics.values())).toArray();
    }
    
    public void createReplicationStats() {
        replicationStats = (SampleStatistics[]) (new ArrayList(replicationStatistics.values())).toArray();
    }
    
    public void createPropertyChangeListeners() {
        propertyChangeListener = (PropertyChangeListener[]) (new ArrayList(propertyChangeListeners.values())).toArray();
    }
    
    void addSimEntity(String name, SimEntity entity) {
        entity.setName(name);
        entities.put(name,entity);
    }
    
    /** 
     * listenerName comes from the PropertyChangeListener tag, while 
     * property comes from the connector
     * however Simkit's take the property name in the constructor. 
     */
    void addDesignPointStat(String listenerName, String propertyName, String type) {
        try {
            Class statClass = Class.forName(type);
            Constructor c = statClass.getConstructor(new Class[]{ String.class });
            PropertyChangeListener pcl;
            if ("".equals(propertyName)) {
                pcl = (PropertyChangeListener) statClass.newInstance();
            } else {
                pcl = (PropertyChangeListener) c.newInstance(new Object[]{propertyName});
            }
            designPointStatistics.put(listenerName,pcl);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    void addReplicationStat(String listenerName, String propertyName, String type) {
        try {
            Class statClass = Class.forName(type);
            Constructor c = statClass.getConstructor(new Class[]{ String.class });
            PropertyChangeListener pcl;
            if ("".equals(propertyName)) {
                pcl = (PropertyChangeListener) statClass.newInstance();
            } else {
                pcl = (PropertyChangeListener) c.newInstance(new Object[]{propertyName});
            }
            replicationStatistics.put(listenerName,pcl);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    void addPropertyChangeListener(String listenerName, String propertyName, String type) {
        try {
            Class statClass = Class.forName(type);
            Constructor c = statClass.getConstructor(new Class[]{ String.class });
            PropertyChangeListener pcl;
            if ("".equals(propertyName)) {
                pcl = (PropertyChangeListener) statClass.newInstance();
            } else {
                pcl = (PropertyChangeListener) c.newInstance(new Object[]{propertyName});
            }
            propertyChangeListeners.put(listenerName,pcl);
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    void addPropertyChangeListenerConnection(String listener, String property, String source) {
        propertyChangeListenerConnections.put(listener,new PropertyConnector(property,source));
    }
    
    void addDesignPointStatListenerConnection(String listener, String property, String source) {
        designPointStatsListenerConnections.put(listener,new PropertyConnector(property,source));
    }
        
    void addReplicationStatsListenerConnection(String listener, String property, String source) {
        propertyChangeListenerConnections.put(listener,new PropertyConnector(property,source));
    } 
    
    void addSimEventListenerConnection(String listener, String source) {
        simEventListenerConnections.put(listener,source);
    }
    
    public PropertyChangeListener getPropertyChangeListenerByName(String name) {
        return (PropertyChangeListener) propertyChangeListeners.get(name);
    }
    
    public SampleStatistics getDesignPointStatByName(String name) {
        return (SampleStatistics) designPointStatistics.get(name);
    }
    
    public SampleStatistics getReplicationStatByName(String name) {
        return (SampleStatistics) designPointStatistics.get(name);
    }
    
    public SimEntity getEntityByName(String name) {
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
