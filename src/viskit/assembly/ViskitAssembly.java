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
public class ViskitAssembly extends BasicAssembly
{
    protected Map<String, SimEntity> simEntitiesMap;
    protected Map<String, PropertyChangeListener>  replicationStatisticsMap;
    protected Map<String, PropertyChangeListener>  designPointStatisticsMap;
    protected Map<String, PropertyChangeListener>  propertyChangeListenersMap;
    protected Map<String, List<PropertyConnector>> propertyChangeListenerConnectionsMap;
    protected Map<String, List<PropertyConnector>> designPointStatisticsListenerConnectionsMap;
    protected Map<String, List<PropertyConnector>> replicationStatisticsListenerConnectionsMap;
    protected Map<String, List<String>>            simEventListenerConnectionsMap;
    protected Map<String, Adapter>                 adapterMap;
    private static final boolean DEBUG = false; // TODO: tie to ViskitStatics.debug?

    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly() {
        simEntitiesMap = new LinkedHashMap<>();
        replicationStatisticsMap = new LinkedHashMap<>();
        designPointStatisticsMap = new LinkedHashMap<>();
        propertyChangeListenersMap = new LinkedHashMap<>();
        propertyChangeListenerConnectionsMap = new LinkedHashMap<>();
        designPointStatisticsListenerConnectionsMap = new LinkedHashMap<>();
        replicationStatisticsListenerConnectionsMap = new LinkedHashMap<>();
        simEventListenerConnectionsMap = new LinkedHashMap<>();
        adapterMap = new LinkedHashMap<>();
    }

    @Override
    public void createObjects() {
        super.createObjects();

        /* After all PropertyChangeListeners have been created pass the LHMap to the super so that the
         * keys can be extracted for data output indexing. This method is used by
         * the ReportStatisticsConfig.
         */
        setStatisticsKeyValues(replicationStatisticsMap);
    }

    @Override
    protected void createSimEntities() {
        if (simEntitiesMap != null) {
            if (simEntitiesMap.values() != null) {
                simEntity = GenericConversion.toArray(simEntitiesMap.values(), new SimEntity[0]);
            }
        }
    }

    @Override
    protected void createReplicationStatistics() {
        replicationStatisticsPropertyChangeListenerArray = GenericConversion.toArray(replicationStatisticsMap.values(), new PropertyChangeListener[0]);
        for (PropertyChangeListener sampleStatisticsListener : replicationStatisticsPropertyChangeListenerArray) {
            LOG.debug(((Named) sampleStatisticsListener).getName() + " sampleStatisticsListener created");
        }
    }

    @Override
    protected void createDesignPointStatistics() {
        super.createDesignPointStatistics();

        // the super.
        for (SampleStatistics sampleStatisticsListeners : super.designPointSimpleStatisticsTally) {
//            LOG.debug(sampleStatisticsListeners.getName() + " sampleStatisticsListeners created");
            designPointStatisticsMap.put(sampleStatisticsListeners.getName(), sampleStatisticsListeners);
        }
    }

    @Override
    protected void createPropertyChangeListeners() {
        propertyChangeListenerArray = GenericConversion.toArray(propertyChangeListenersMap.values(), new PropertyChangeListener[0]);
        for (PropertyChangeListener propertyChangeListener : propertyChangeListenerArray) {
            LOG.debug(propertyChangeListener + " propertyChangeListener created");
        }
    }

    @Override
    public void hookupSimEventListeners() {
        String[] listeners = GenericConversion.toArray(simEventListenerConnectionsMap.keySet(), new String[0]);
        if(DEBUG) {
            LOG.info("hookupSimEventListeners called " + listeners.length);
        }
        List<String> simEventListenerConnects;
        for (String listener : listeners) {
            simEventListenerConnects = simEventListenerConnectionsMap.get(listener);
            if (simEventListenerConnects != null) {
                for(String source : simEventListenerConnects) {
                    connectSimEventListener(listener, source);
                    if (DEBUG) {
                        LOG.info("hooking up SimEvent source " + source + " to listener " + listener);
                    }
                }
            }
        }
    }

    @Override
    public void hookupReplicationListeners() {
        String[] listeners = GenericConversion.toArray(replicationStatisticsListenerConnectionsMap.keySet(), new String[0]);
        List<PropertyConnector> replicationStatisiticsConnectorList;
        for (String listener : listeners) {
            replicationStatisiticsConnectorList = replicationStatisticsListenerConnectionsMap.get(listener);
            if (replicationStatisiticsConnectorList != null) {
                for (PropertyConnector propertyConnector : replicationStatisiticsConnectorList) {
                    connectReplicationStatistics(listener, propertyConnector);
                }
            } else if (DEBUG) {
                LOG.info("No replicationListeners");
            }
        }
    }

    @Override
    protected void hookupDesignPointListeners() {
        super.hookupDesignPointListeners();
        String[] designPointStatisticsListenerArray = GenericConversion.toArray(designPointStatisticsListenerConnectionsMap.keySet(), new String[0]);
        // if not the default case, need to really do this with
        // a Class to create instances selected by each Replicationstatistics listener.
        List<PropertyConnector> designPointConnectorsList;
        if (designPointStatisticsListenerArray.length > 0) {
            for (String designPointStatisticsListener : designPointStatisticsListenerArray) {
                designPointConnectorsList = designPointStatisticsListenerConnectionsMap.get(designPointStatisticsListener);
                if ( designPointConnectorsList != null ) {
                    for (PropertyConnector propertyConnector : designPointConnectorsList) {
                        connectDesignPointStatistics(designPointStatisticsListener, propertyConnector);
                    }
                }
            }
        } else if (DEBUG) {
            LOG.info("No external designPointListeners to add");
        }
    }

    @Override
    protected void hookupPropertyChangeListeners() {
        String[] listeners = GenericConversion.toArray(propertyChangeListenerConnectionsMap.keySet(), new String[0]);
        List<PropertyConnector> propertyConnects;
        for (String listener : listeners) {
            propertyConnects = propertyChangeListenerConnectionsMap.get(listener);
            if (propertyConnects != null) {
                for (PropertyConnector pc : propertyConnects) {
                    connectPropertyChangeListener(listener, pc);
                }
            } else if (DEBUG) {
                LOG.info("No propertyConnectors");
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
        if (DEBUG) {
            LOG.info("connecting entity " + pc.source + " to " + listener + " property " + pc.property);
        }
    }

    void connectReplicationStatistics(String listener, PropertyConnector propertyConnector) {
        if ( "null".equals(propertyConnector.property) ) {
            propertyConnector.property = "";
        }
        if (DEBUG) {
            LOG.info("Connecting entity " + propertyConnector.source + " to replicationStatistic " + listener + " property " + propertyConnector.property);
        }

        if (propertyConnector.property.isEmpty()) {
            propertyConnector.property = getReplicationStatisticsByName(listener).getName().trim();
            if (DEBUG) {
                LOG.info("Property unspecified, attempting with lookup " + propertyConnector.property);
            }
        }

        if (propertyConnector.property.isEmpty()) {
            if (DEBUG) {
                LOG.info("Null property, replicationStatistics connecting "+propertyConnector.source+" to "+listener);
            }
            getSimEntityByName(propertyConnector.source).addPropertyChangeListener(getReplicationStatisticsByName(listener));
        } else {
            if (DEBUG) {
                LOG.info("Connecting replicationStatistics from "+propertyConnector.source+" to "+listener);
            }
            getSimEntityByName(propertyConnector.source).addPropertyChangeListener(propertyConnector.property,getReplicationStatisticsByName(listener));
        }
    }

    void connectDesignPointStatistics(String listener, PropertyConnector propertyConnector) {
        if ( propertyConnector.property.equals("null") ) {
            propertyConnector.property = "";
        }
        if ( "".equals(propertyConnector.property) ) {
            propertyConnector.property = getDesignPointStatisticsByName(listener).getName();
        }

        if ( "".equals(propertyConnector.property) ) {
            getSimEntityByName(propertyConnector.source).addPropertyChangeListener(getDesignPointStatisticsByName(listener));
        } else {
            getSimEntityByName(propertyConnector.source).addPropertyChangeListener(propertyConnector.property,getDesignPointStatisticsByName(listener));
        }
    }

    public void addSimEntity(String name, SimEntity entity) {
        entity.setName(name);
        simEntitiesMap.put(name, entity);

        // TODO: This will throw an IllegalArgumentException?
//        LOG.debug("entity is: {}", entity);
    }

    public void addDesignPointStatistics(String listenerName, PropertyChangeListener propertyChangeListener) {
        designPointStatisticsMap.put(listenerName,propertyChangeListener);
    }

    /** Called from the generated Assembly adding PropertyChangeListeners in order of calling
     * @param listenerName the given name of the PropertyChangeListener
     * @param propertyChangeListener type of PropertyChangeListener
     */
    public void addReplicationStatistics(String listenerName, PropertyChangeListener propertyChangeListener) {
        LOG.debug("Adding to replicationStatistics " + listenerName + " " + propertyChangeListener);
        replicationStatisticsMap.put(listenerName, propertyChangeListener);
    }

    @Override
    public void addPropertyChangeListener(String listenerName, PropertyChangeListener propertyChangeListener) {
        propertyChangeListenersMap.put(listenerName, propertyChangeListener);
    }

    public void addPropertyChangeListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> propertyConnects = propertyChangeListenerConnectionsMap.get(listener);
        if ( propertyConnects == null ) {
            propertyConnects = new LinkedList<>();
            propertyChangeListenerConnectionsMap.put(listener, propertyConnects);
        }
        propertyConnects.add(new PropertyConnector(property, source));
    }

    public void addDesignPointStatisticsListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> designPointConnects = designPointStatisticsListenerConnectionsMap.get(listener);
        if ( designPointConnects == null ) {
            designPointConnects = new LinkedList<>();
            designPointStatisticsListenerConnectionsMap.put(listener, designPointConnects);
        }
        designPointConnects.add(new PropertyConnector(property,source));
    }

    public void addReplicationStatisticsListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> replicationStatisticsConnects = replicationStatisticsListenerConnectionsMap.get(listener);
        if ( replicationStatisticsConnects == null ) {
            replicationStatisticsConnects = new LinkedList<>();
            replicationStatisticsListenerConnectionsMap.put(listener, replicationStatisticsConnects);
        }
        replicationStatisticsConnects.add(new PropertyConnector(property,source));
    }

    public void addSimEventListenerConnection(String listener, String source) {
        List<String> simEventListenerConnects = simEventListenerConnectionsMap.get(listener);
        if ( simEventListenerConnects == null ) {
            simEventListenerConnects = new LinkedList<>();
            simEventListenerConnectionsMap.put(listener, simEventListenerConnects);
        }
        if (DEBUG) {
            LOG.info("addSimEventListenerConnection source " + source + " to listener " + listener );
        }
        simEventListenerConnects.add(source);
    }

    public void addAdapter(String name, String heard, String sent, String from, String to) {
        Adapter a = new Adapter(heard,sent);
        a.connect(getSimEntityByName(from),getSimEntityByName(to));
        adapterMap.put(name,a);
        simEntitiesMap.put(name,a);
    }

    public PropertyChangeListener getPropertyChangeListenerByName(String name) {
        return propertyChangeListenersMap.get(name);
    }

    public SampleStatistics getDesignPointStatisticsByName(String name) {
        return (SampleStatistics) designPointStatisticsMap.get(name);
    }

    public SampleStatistics getReplicationStatisticsByName(String name) {
        return (SampleStatistics) replicationStatisticsMap.get(name);
    }

    public SimEntity getSimEntityByName(String name) {
        if (DEBUG) {
            LOG.info("getSimEntityByName for {}: {}",name, simEntitiesMap.get(name));
        }
        return simEntitiesMap.get(name);
    }

    protected class PropertyConnector {
        String property;
        String source;

        PropertyConnector(String p, String s) {
            this.property = p;
            this.source = s;
        }
    }

} // end class ViskitAssembly
