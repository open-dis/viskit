package viskit.assembly;

import edu.nps.util.GenericConversion;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import simkit.Adapter;
import simkit.Named;
import simkit.SimEntity;
import simkit.stat.SampleStatistics;

/** BasicAssembly doesn't provide Viskit users with ability to
 * reference a known SimEntity within a constructor. This class
 * provides hooks into the BasicAssembly and enables named references
 * to instances within the design tool.
 * @version $Id$
 * @author Rick Goldberg
 * @since September 25, 2005, 1:44 PM
 */
public class ViskitAssembly extends BasicAssembly {

    protected Map<String, SimEntity> entities;
    protected Map<String, PropertyChangeListener> replicationStatistics;
    protected Map<String, PropertyChangeListener> designPointStatistics;
    protected Map<String, PropertyChangeListener> propertyChangeListeners;
    protected Map<String, List<PropertyConnector>> propertyChangeListenerConnections;
    protected Map<String, List<PropertyConnector>> designPointStatisticsListenerConnections;
    protected Map<String, List<PropertyConnector>> replicationStatisticsListenerConnections;
    protected Map<String, List<String>> simEventListenerConnections;
    protected Map<String, Adapter> adapters;
    private static boolean debug = false;

    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly() {
        entities = new LinkedHashMap<>();
        replicationStatistics = new LinkedHashMap<>();
        designPointStatistics = new LinkedHashMap<>();
        propertyChangeListeners = new LinkedHashMap<>();
        propertyChangeListenerConnections = new LinkedHashMap<>();
        designPointStatisticsListenerConnections = new LinkedHashMap<>();
        replicationStatisticsListenerConnections = new LinkedHashMap<>();
        simEventListenerConnections = new LinkedHashMap<>();
        adapters = new LinkedHashMap<>();
    }

    @Override
    public void createObjects() {
        super.createObjects();

        /* After all PCLs have been created pass the LHMap to the super so that the
         * keys can be extracted for data output indexing. This method is used by
         * the ReportStatisticsConfig.
         */
        setStatisticsKeyValues(replicationStatistics);
    }

    @Override
    protected void createSimEntities() {
        if (entities != null) {
            if (entities.values() != null) {
                simEntity = GenericConversion.toArray(entities.values(), new SimEntity[0]);
            }
        }
    }

    @Override
    protected void createReplicationStatistics() {
        super.replicationStatistics = GenericConversion.toArray(replicationStatistics.values(), new PropertyChangeListener[0]);
        for (PropertyChangeListener sampleStatistics : super.replicationStatistics) {
            LOG.debug(((Named) sampleStatistics).getName() + " replicationStat created");
        }
    }

    @Override
    protected void createDesignPointStatistics() {

        super.createDesignPointStatistics();

        // the super.
        for (SampleStatistics sampleStatistics : super.designPointStatistics) {
//            LOG.debug(sampleStatistics.getName() + " designPointStat created");
            designPointStatistics.put(sampleStatistics.getName(), sampleStatistics);
        }
    }

    @Override
    protected void createPropertyChangeListeners() {
        propertyChangeListener = GenericConversion.toArray(propertyChangeListeners.values(), new PropertyChangeListener[0]);
        for (PropertyChangeListener pcl : propertyChangeListener) {
            LOG.debug(pcl + " propertyChangeListener created");
        }
    }

    @Override
    public void hookupSimEventListeners() {
        String[] listeners = GenericConversion.toArray(simEventListenerConnections.keySet(), new String[0]);
        if(debug) {
            LOG.info("hookupSimEventListeners called " + listeners.length);
        }
        for (String listener : listeners) {
            List<String> simEventListenerConnects = simEventListenerConnections.get(listener);
            if (simEventListenerConnects != null) {
                for(String source : simEventListenerConnects) {
                    connectSimEventListener(listener, source);
                    if (debug) {
                        LOG.info("hooking up SimEvent source " + source + " to listener " + listener);
                    }
                }
            }
        }
    }

    @Override
    public void hookupReplicationListeners() {
        String[] listeners = GenericConversion.toArray(replicationStatisticsListenerConnections.keySet(), new String[0]);
        for (String listener : listeners) {
            List<PropertyConnector> replicationStatisticsConnects = replicationStatisticsListenerConnections.get(listener);
            if (replicationStatisticsConnects != null) {
                for (PropertyConnector pc : replicationStatisticsConnects) {
                    connectReplicationStatistics(listener, pc);
                }
            } else if (debug) {
                LOG.info("No replicationListeners");
            }
        }
    }

    @Override
    protected void hookupDesignPointListeners() {
        super.hookupDesignPointListeners();
        String[] listeners = GenericConversion.toArray(designPointStatisticsListenerConnections.keySet(), new String[0]);
        // if not the default case, need to really do this with
        // a Class to create instances selected by each ReplicationStatistics listener.
        if (listeners.length > 0) {
            for (String listener : listeners) {
                List<PropertyConnector> designPointConnects = designPointStatisticsListenerConnections.get(listener);
                if ( designPointConnects != null ) {
                    for (PropertyConnector pc : designPointConnects) {
                        connectDesignPointStatistics(listener, pc);
                    }
                }
            }
        } else if (debug) {
            log.info("No external designPointListeners to add");
        }
    }

    @Override
    protected void hookupPropertyChangeListeners() {
        String[] listeners = GenericConversion.toArray(propertyChangeListenerConnections.keySet(), new String[0]);
        for (String listener : listeners) {
            List<PropertyConnector> propertyConnects = propertyChangeListenerConnections.get(listener);
            if (propertyConnects != null) {
                for (PropertyConnector pc : propertyConnects) {
                    connectPropertyChangeListener(listener, pc);
                }
            } else if (debug) {
                log.info("No propertyConnectors");
            }
        }
    }

    void connectSimEventListener(String listener, String source) {
        getSimEntityByName(source).addSimEventListener(getSimEntityByName(listener));
    }

    void connectPropertyChangeListener(String listener, PropertyConnector pc) {
        if ( "null".equals(pc.property) ) {
            pc.property = "";
        }
        if (pc.property.isEmpty()) {
            getSimEntityByName(pc.source).addPropertyChangeListener(getPropertyChangeListenerByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getPropertyChangeListenerByName(listener));
        }
        if (debug) {
            LOG.info("connecting entity " + pc.source + " to " + listener + " property " + pc.property);
        }
    }

    void connectReplicationStatistics(String listener, PropertyConnector pc) {
        if ( "null".equals(pc.property) ) {
            pc.property = "";
        }
        if (debug) {
            LOG.info("Connecting entity " + pc.source + " to replicationStat " + listener + " property " + pc.property);
        }

        if (pc.property.isEmpty()) {
            pc.property = getReplicationStatisticsByName(listener).getName().trim();
            if (debug) {
                LOG.info("Property unspecified, attempting with lookup " + pc.property);
            }
        }

        if (pc.property.isEmpty()) {
            if (debug) {
                LOG.info("Null property, replicationStatistics connecting "+pc.source+" to "+listener);
            }
            getSimEntityByName(pc.source).addPropertyChangeListener(getReplicationStatisticsByName(listener));
        } else {
            if (debug) {
                LOG.info("Connecting replicationStatistics from "+pc.source+" to "+listener);
            }
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getReplicationStatisticsByName(listener));
        }
    }

    void connectDesignPointStatistics(String listener, PropertyConnector pc) {
        if ( pc.property.equals("null") ) {
            pc.property = "";
        }
        if ( "".equals(pc.property) ) {
            pc.property = getDesignPointStatisticsByName(listener).getName();
        }

        if ( "".equals(pc.property) ) {
            getSimEntityByName(pc.source).addPropertyChangeListener(getDesignPointStatisticsByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getDesignPointStatisticsByName(listener));
        }
    }

    public void addSimEntity(String name, SimEntity entity) {
        entity.setName(name);
        entities.put(name, entity);

        // TODO: This will throw an IllegalArgumentException?
//        LOG.debug("entity is: " + entity);
    }

    public void addDesignPointStatistics(String listenerName, PropertyChangeListener pcl) {
        designPointStatistics.put(listenerName,pcl);
    }

    /** Called from the generated Assembly adding PCLs in order of calling
     * @param listenerName the given name of the PropertyChangeListener
     * @param pcl type of PropertyChangeListener
     */
    public void addReplicationStatistics(String listenerName, PropertyChangeListener pcl) {
        LOG.debug("Adding to replicationStatistics " + listenerName + " " + pcl);
        replicationStatistics.put(listenerName, pcl);
    }

    @Override
    public void addPropertyChangeListener(String listenerName, PropertyChangeListener pcl) {
        propertyChangeListeners.put(listenerName, pcl);
    }

    public void addPropertyChangeListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> propertyConnects = propertyChangeListenerConnections.get(listener);
        if ( propertyConnects == null ) {
            propertyConnects = new LinkedList<>();
            propertyChangeListenerConnections.put(listener, propertyConnects);
        }
        propertyConnects.add(new PropertyConnector(property, source));
    }

    public void addDesignPointStatisticsListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> designPointConnects = designPointStatisticsListenerConnections.get(listener);
        if ( designPointConnects == null ) {
            designPointConnects = new LinkedList<>();
            designPointStatisticsListenerConnections.put(listener, designPointConnects);
        }
        designPointConnects.add(new PropertyConnector(property,source));
    }

    public void addReplicationStatisticsListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> repStatisticsConnects = replicationStatisticsListenerConnections.get(listener);
        if ( repStatisticsConnects == null ) {
            repStatisticsConnects = new LinkedList<>();
            replicationStatisticsListenerConnections.put(listener, repStatisticsConnects);
        }
        repStatisticsConnects.add(new PropertyConnector(property,source));
    }

    public void addSimEventListenerConnection(String listener, String source) {
        List<String> simEventListenerConnects = simEventListenerConnections.get(listener);
        if ( simEventListenerConnects == null ) {
            simEventListenerConnects = new LinkedList<>();
            simEventListenerConnections.put(listener, simEventListenerConnects);
        }
        if (debug) {
            LOG.info("addSimEventListenerConnection source " + source + " to listener " + listener );
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
        return propertyChangeListeners.get(name);
    }

    public SampleStatistics getDesignPointStatisticsByName(String name) {
        return (SampleStatistics) designPointStatistics.get(name);
    }

    public SampleStatistics getReplicationStatisticsByName(String name) {
        return (SampleStatistics) replicationStatistics.get(name);
    }

    public SimEntity getSimEntityByName(String name) {
        if (debug) {
            LOG.info("getSimEntityByName for " + name + " " + entities.get(name));
        }
        return entities.get(name);
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
