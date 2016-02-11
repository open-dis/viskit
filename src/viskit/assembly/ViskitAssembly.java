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

    protected Map<String, SimEntity>               entitiesMap;
    protected Map<String, PropertyChangeListener>  propertyChangeListenersMap;
    protected Map<String, PropertyChangeListener>  designPointStatisticsMap;
    protected Map<String, PropertyChangeListener>  replicationStatisticsMap;
    protected Map<String, List<PropertyConnector>> propertyChangeListenerConnectionsMap;
    protected Map<String, List<PropertyConnector>> designPointStatisticsListenerConnectionsMap;
    protected Map<String, List<PropertyConnector>> replicationStatisticsListenerConnectionsMap;
    protected Map<String, List<String>>            simEventListenerConnectionsMap;
    protected Map<String, Adapter>                 adaptersMap;
    private static boolean debug = false;

    /** Creates a new instance of ViskitAssembly */
    public ViskitAssembly()
	{
                                        entitiesMap = new LinkedHashMap<>();
                         propertyChangeListenersMap = new LinkedHashMap<>();
                           designPointStatisticsMap = new LinkedHashMap<>();
                           replicationStatisticsMap = new LinkedHashMap<>();
               propertyChangeListenerConnectionsMap = new LinkedHashMap<>();
        designPointStatisticsListenerConnectionsMap = new LinkedHashMap<>();
        replicationStatisticsListenerConnectionsMap = new LinkedHashMap<>();
                     simEventListenerConnectionsMap = new LinkedHashMap<>();
                                        adaptersMap = new LinkedHashMap<>();
    }

    @Override
    public void createObjects() {
        super.createObjects();

        /* After all PCLs have been created pass the LHMap to the super so that the
         * keys can be extracted for data output indexing. This method is used by
         * the ReportStatisticsConfig.
         */
        setStatisticsKeyValues(replicationStatisticsMap);
    }

    @Override
    protected void createSimEntities() {
        if (entitiesMap != null) {
            if (entitiesMap.values() != null) {
                simEntity = GenericConversion.toArray(entitiesMap.values(), new SimEntity[0]);
            }
        }
    }

    @Override
    protected void createReplicationStatistics() {
        super.replicationStatisticsPropertyChangeListenerArray = GenericConversion.toArray(replicationStatisticsMap.values(), new PropertyChangeListener[0]);
        for (PropertyChangeListener sampleStatistics : super.replicationStatisticsPropertyChangeListenerArray) {
            LOG.debug(((Named) sampleStatistics).getName() + " replicationStatistics created");
        }
    }

    @Override
    protected void createDesignPointStatistics() {

        super.createDesignPointStatistics();

        // the super.
        for (SampleStatistics sampleStatistics : super.designPointStatistics) {
//            LOG.debug(sampleStatistics.getName() + " designPointStat created");
            designPointStatisticsMap.put(sampleStatistics.getName(), sampleStatistics);
        }
    }

    @Override
    protected void createPropertyChangeListeners() {
        propertyChangeListenerArray = GenericConversion.toArray(propertyChangeListenersMap.values(), new PropertyChangeListener[0]);
        for (PropertyChangeListener pcl : propertyChangeListenerArray) {
            LOG.debug(pcl + " propertyChangeListener created");
        }
    }

    @Override
    public void hookupSimEventListeners() {
        String[] listeners = GenericConversion.toArray(simEventListenerConnectionsMap.keySet(), new String[0]);
        if(debug) {
            LOG.info("hookupSimEventListeners called " + listeners.length);
        }
        for (String listener : listeners) {
            List<String> simEventListenerConnects = simEventListenerConnectionsMap.get(listener);
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
        String[] listeners = GenericConversion.toArray(replicationStatisticsListenerConnectionsMap.keySet(), new String[0]);
        for (String listener : listeners) {
            List<PropertyConnector> replicationStatisticsConnects = replicationStatisticsListenerConnectionsMap.get(listener);
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
        String[] listeners = GenericConversion.toArray(designPointStatisticsListenerConnectionsMap.keySet(), new String[0]);
        // if not the default case, need to really do this with
        // a Class to create instances selected by each ReplicationStatistics listener.
        if (listeners.length > 0) {
            for (String listener : listeners) {
                List<PropertyConnector> designPointConnects = designPointStatisticsListenerConnectionsMap.get(listener);
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
        String[] listeners = GenericConversion.toArray(propertyChangeListenerConnectionsMap.keySet(), new String[0]);
        for (String listener : listeners) {
            List<PropertyConnector> propertyConnects = propertyChangeListenerConnectionsMap.get(listener);
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
            LOG.info("Connecting entity " + pc.source + " to replicationStatistics " + listener + " property " + pc.property);
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
        if ( "".equals(pc.property) ) { // TODO ??
            pc.property = getDesignPointStatisticsByName(listener).getName();
        }

        if ( "".equals(pc.property) ) { // TODO ??
            getSimEntityByName(pc.source).addPropertyChangeListener(getDesignPointStatisticsByName(listener));
        } else {
            getSimEntityByName(pc.source).addPropertyChangeListener(pc.property,getDesignPointStatisticsByName(listener));
        }
    }

    public void addSimEntity(String name, SimEntity simEntity) {
        simEntity.setName(name);
        entitiesMap.put(name, simEntity);

        // TODO: This will throw an IllegalArgumentException?
//        LOG.debug("entity is: " + entity);
    }

    public void addDesignPointStatistics(String listenerName, PropertyChangeListener pcl) {
        LOG.debug("Adding to designPointStatistics " + listenerName + " " + pcl);
        designPointStatisticsMap.put(listenerName,pcl);
    }

    /** Called from the generated Assembly adding PCLs in order of calling
     * @param listenerName the given name of the PropertyChangeListener
     * @param pcl type of PropertyChangeListener
     */
    public void addReplicationStatistics(String listenerName, PropertyChangeListener pcl) {
        LOG.debug("Adding to replicationStatistics " + listenerName + " " + pcl);
        replicationStatisticsMap.put(listenerName, pcl);
    }

    @Override
    public void addPropertyChangeListener(String listenerName, PropertyChangeListener pcl) {
        LOG.debug("Adding to propertyChangeListeners " + listenerName + " " + pcl);
        propertyChangeListenersMap.put(listenerName, pcl);
    }

    public void addPropertyChangeListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> propertyConnects = propertyChangeListenerConnectionsMap.get(listener);
        if (propertyConnects == null ) {
            propertyConnects = new LinkedList<>();
            propertyChangeListenerConnectionsMap.put(listener, propertyConnects);
        }
        propertyConnects.add(new PropertyConnector(property, source));
    }

    public void addDesignPointStatisticsListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> designPointConnects = designPointStatisticsListenerConnectionsMap.get(listener);
        if (designPointConnects == null ) {
            designPointConnects = new LinkedList<>();
            designPointStatisticsListenerConnectionsMap.put(listener, designPointConnects);
        }
        designPointConnects.add(new PropertyConnector(property,source));
    }

    public void addReplicationStatisticsListenerConnection(String listener, String property, String source) {
        List<PropertyConnector> replicationStatisticsConnects = replicationStatisticsListenerConnectionsMap.get(listener);
        if (replicationStatisticsConnects == null ) {
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
        if (debug) {
            LOG.info("addSimEventListenerConnection source " + source + " to listener " + listener );
        }
        simEventListenerConnects.add(source);
    }

    public void addAdapter(String name, String heard, String sent, String from, String to) {
        Adapter adapter = new Adapter(heard,sent);
        adapter.connect(getSimEntityByName(from),getSimEntityByName(to));
        adaptersMap.put(name,adapter);
        entitiesMap.put(name,adapter);
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
        if (debug) {
            LOG.info("getSimEntityByName for " + name + " " + entitiesMap.get(name));
        }
        return entitiesMap.get(name);
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
