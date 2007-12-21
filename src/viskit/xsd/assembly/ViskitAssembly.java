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
import simkit.SimEntity;
import simkit.stat.SampleStatistics;
import static edu.nps.util.GenericConversion.toArray;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * @version $Id: ViskitAssembly.java 1666 2007-12-17 05:24:41Z tdnorbra $
 * @author Rick Goldberg
 */
public class ViskitAssembly extends BasicAssembly { 
    
    protected LinkedHashMap<String, SimEntity> entities;
    protected LinkedHashMap replicationStatistics;
    protected LinkedHashMap<String, PropertyChangeListener> designPointStatistics;
    protected LinkedHashMap<String, PropertyChangeListener> propertyChangeListeners;
    protected LinkedHashMap<String, LinkedList<PropertyConnector>> propertyChangeListenerConnections;
    protected LinkedHashMap<String, LinkedList<PropertyConnector>> designPointStatsListenerConnections;
    protected LinkedHashMap<String, LinkedList<PropertyConnector>> replicationStatsListenerConnections;
    protected LinkedHashMap<String, LinkedList<String>> simEventListenerConnections;
    protected LinkedHashMap<String, Adapter> adapters;
    private static boolean debug = false;
    
    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly() {
        
    }  
    
    @Override
    public void createObjects() {
        entities = new LinkedHashMap<String, SimEntity>();
        replicationStatistics = new LinkedHashMap();
        designPointStatistics = new LinkedHashMap<String, PropertyChangeListener>();
        propertyChangeListeners = new LinkedHashMap<String, PropertyChangeListener>();
        propertyChangeListenerConnections = new LinkedHashMap<String, LinkedList<PropertyConnector>>();
        designPointStatsListenerConnections = new LinkedHashMap<String, LinkedList<PropertyConnector>>();
        replicationStatsListenerConnections = new LinkedHashMap<String, LinkedList<PropertyConnector>>();
        simEventListenerConnections = new LinkedHashMap<String, LinkedList<String>>();
        adapters = new LinkedHashMap<String, Adapter>();
        //super.createObjects();
        createSimEntities();
        createReplicationStats();
        createDesignPointStats();
        createPropertyChangeListeners();       
        
        /**
         * After all PCLs have been created pass the LHMap to the super so that the
         * keys can be extracted for data output indexing. This method is used by 
         * the ReportStatisticsConfig.
         */
        setStatisticsKeyValues(replicationStatistics);
    }
    
    @Override
    public void performHookups() {
        super.performHookups();
    }
    
    public void hookupReplicationListeners() {        
        String[] listeners = toArray(replicationStatsListenerConnections.keySet(), new String[0]);
        for ( int i = 0; i < listeners.length; i++ ) {
            LinkedList<PropertyConnector> repStatsConnects = replicationStatsListenerConnections.get(listeners[i]);
            if ( repStatsConnects != null ) {
                ListIterator li = repStatsConnects.listIterator();
                while ( li.hasNext() ) {
                    PropertyConnector pc = (PropertyConnector) li.next();
                    connectReplicationStats(listeners[i], pc);
                }
            } else if (debug) {
                System.out.println("No replicationListeners");
            }
 
        }
        
    }
    
    public void hookupSimEventListeners() {
        String[] listeners = toArray(simEventListenerConnections.keySet(), new String[0]);
        if(debug) {
            System.out.println("hookupSimEventListeners called " + listeners.length);
        }
        for ( int i = 0; i < listeners.length; i++ ) {
            LinkedList<String> simEventListenerConnects = simEventListenerConnections.get(listeners[i]);
            if ( simEventListenerConnects != null ) {
                ListIterator li = simEventListenerConnects.listIterator();
                while( li.hasNext() ) {
                    String source = (String)li.next();
                    connectSimEventListener(listeners[i],source);
                    if (debug) {
                        System.out.println("hooking up SimEvent source " + source + " to listener " + listeners[i]);
                    }
                }
            }
            
        }

    }
    
    @Override
    protected void hookupPropertyChangeListeners() {
        String[] listeners = toArray(propertyChangeListenerConnections.keySet(), new String[0]);
        for ( int i = 0; i < listeners.length; i++ ) {
            LinkedList<PropertyConnector> propertyConnects = propertyChangeListenerConnections.get(listeners[i]);
            if ( propertyConnects != null ) {
                ListIterator li = propertyConnects.listIterator();
                while ( li.hasNext() ) {
                    PropertyConnector pc = (PropertyConnector) li.next();
                    connectPropertyChangeListener(listeners[i], pc);
                }
            } else if (debug) {
                System.out.println("No propertyConnectors");
            }
        }
    }
    
    @Override
    protected void hookupDesignPointListeners() {
        super.hookupDesignPointListeners();
        String[] listeners = toArray(designPointStatsListenerConnections.keySet(), new String[0]);
        // if not the default case, need to really do this with
        // a Class to create instances selected by each ReplicationStats listener.
        if (listeners.length > 0) {
            for ( int i = 0; i < listeners.length; i++ ) {
                LinkedList<PropertyConnector> designPointConnects = designPointStatsListenerConnections.get(listeners[i]);
                if ( designPointConnects != null ) {
                    ListIterator li = designPointConnects.listIterator();
                    while ( li.hasNext() ) {
                        PropertyConnector pc = (PropertyConnector) li.next();
                        connectDesignPointStats(listeners[i], pc);
                    }
                }
            }
        } else if ( debug ) {
            System.out.println("No external designPointListeners to add");
        }
    }
    
    void connectSimEventListener(String listener, String source) {
        getSimEntityByName(source).addSimEventListener(getSimEntityByName(listener));
    } 
    
    void connectPropertyChangeListener(String listener, PropertyConnector pc) {
        if ( pc.property.equals("null") ) {
            pc.property = "";
            getSimEntityByName(pc.source).addPropertyChangeListener(getPropertyChangeListenerByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getPropertyChangeListenerByName(listener));
        }
        if ( debug ) System.out.println("connecting entity " + pc.source + "to " + listener + " property " + pc.property );
        
    }
    
    void connectReplicationStats(String listener, PropertyConnector pc) {
        if ( "null".equals(pc.property) ) {
            pc.property = "";
        }
        if ( debug ) System.out.println("Connecting entity " + pc.source + "to replicationStat " + listener + " property " + pc.property );
        if ( "".equals(pc.property) ) {
            pc.property = ((SampleStatistics) (getReplicationStatsByName(listener))).getName().trim();
            if ( debug ) {
                System.out.println("Property unspecified, attempting with lookup " + pc.property);
            }
        }
        
        if ( "".equals(pc.property) ) {
            if ( debug ) {
                System.out.println("Null property, replicationStats connecting "+pc.source+" to "+listener);
            }
            getSimEntityByName(pc.source).addPropertyChangeListener(getReplicationStatsByName(listener));
        } else {
            if ( debug ) {
                System.out.println("Connecting replicationStats from "+pc.source+" to "+listener);
            }
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getReplicationStatsByName(listener));
        }
    }
    
    void connectDesignPointStats(String listener, PropertyConnector pc) {
        if ( pc.property.equals("null") ) {
            pc.property = "";
        }
        if ( "".equals(pc.property) ) {
            pc.property = ((SampleStatistics) (getDesignPointStatsByName(listener))).getName();
        }
        
        if ( "".equals(pc.property) ) {
            getSimEntityByName(pc.source).addPropertyChangeListener(getDesignPointStatsByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getDesignPointStatsByName(listener));
        }
        
    }    

    /** to be called after all entities have been added as a super() */
    /*  note not using template version of ArrayList... */
    protected void createSimEntities() {
        if (entities != null) if (entities.values() != null)
        simEntity = toArray(entities.values(), new SimEntity[0]);
    }
    
    @Override
    protected void createDesignPointStats() {
        super.createDesignPointStats();
        // to be consistent; should be getting the designPointStats from 
        // the super. 
        
        for ( int i = 0 ; i < designPointStats.length; i ++ ) {
            if ( debug ) {
                System.out.println(designPointStats[i].getName() + " designPointStat created");
            }
            designPointStatistics.put(designPointStats[i].getName(), designPointStats[i]);
        }
    }
    
    @Override
    // TODO: fix generics: SampleStatistics vs PropertyChangeListener
    @SuppressWarnings("unchecked")
    protected void createReplicationStats() {
        replicationStats = toArray((Collection<SampleStatistics>) replicationStatistics.values(), new SampleStatistics[0]);
        for ( int i = 0; debug && i < replicationStats.length; i++ ) {
            System.out.println(replicationStats[i].getName() + " replicationStat created");
        }
    }
    
    @Override
    protected void createPropertyChangeListeners() {
        propertyChangeListener = toArray(propertyChangeListeners.values(), new PropertyChangeListener[0]);
        for ( int i = 0; debug && i < propertyChangeListener.length; i++ ) {
            System.out.println(propertyChangeListener[i] + " propertyChangeListener created");
        }
    }
    
    public void addSimEntity(String name, SimEntity entity) {
        entity.setName(name);
        entities.put(name,entity);
        if (debug) {
            System.out.println("ViskitAssembly addSimEntity "+entity);
        }
    }
    
    public void addDesignPointStats(String listenerName, PropertyChangeListener pcl) {
        designPointStatistics.put(listenerName,pcl);
    }
   
    // TODO: fix generics: SampleStatistics vs PropertyChangeListener
    @SuppressWarnings("unchecked")
    public void addReplicationStats(String listenerName, PropertyChangeListener pcl) {        
        replicationStatistics.put(listenerName, pcl);
    }
    
    @Override
    public void addPropertyChangeListener(String listenerName, PropertyChangeListener pcl) {        
        propertyChangeListeners.put(listenerName,pcl);
    }
    
    public void addPropertyChangeListenerConnection(String listener, String property, String source) {
        LinkedList<PropertyConnector> propertyConnects = (propertyChangeListenerConnections.get(listener));
        if ( propertyConnects == null ) {
            propertyConnects = new LinkedList<PropertyConnector>();            
            propertyChangeListenerConnections.put(listener, propertyConnects);
        }
        propertyConnects.add(new PropertyConnector(property, source));
    }
    
    public void addDesignPointStatsListenerConnection(String listener, String property, String source) {
        LinkedList<PropertyConnector> designPointConnects = (designPointStatsListenerConnections.get(listener));
        if ( designPointConnects == null ) {
            designPointConnects = new LinkedList<PropertyConnector>();
            designPointStatsListenerConnections.put(listener, designPointConnects);
        }
        designPointConnects.add(new PropertyConnector(property,source));
    }
        
    public void addReplicationStatsListenerConnection(String listener, String property, String source) {
        LinkedList<PropertyConnector> repStatsConnects = replicationStatsListenerConnections.get(listener);
        if ( repStatsConnects == null ) {
            repStatsConnects = new LinkedList<PropertyConnector>();
            replicationStatsListenerConnections.put(listener, repStatsConnects);
        }
        repStatsConnects.add(new PropertyConnector(property,source));
    } 
    
    public void addSimEventListenerConnection(String listener, String source) {
        LinkedList<String> simEventListenerConnects = simEventListenerConnections.get(listener);
        if ( simEventListenerConnects == null ) {
            simEventListenerConnects = new LinkedList<String>();
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
        if (debug) System.out.println("getSimEntityByName for "+name+" "+entities.get(name));
        return (SimEntity) entities.get(name);
    }
    
    protected class PropertyConnector {
        String property;
        String source;
        
        PropertyConnector(String p, String s) {
            this.property = p;
            this.source = s;
        }
    }

}
