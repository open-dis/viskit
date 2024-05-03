/*
 * ReportStatisticsDOM.java
 *
 * Created on July 15, 2006, 8:21 PM
 */
package viskit.reports;

import java.util.HashMap;
import java.util.Map;
import org.jdom.Document;
import org.jdom.Element;

/**
 *
 * This class is used by viskit.reports.ReportStatsConfig to construct an XML
 * document out of the replication and summary stats objects that are passed to it.
 *
 * @author Patrick Sullivan
 * @version $Id$
 */
public class ReportStatisticsDOM {

    /**
     * The DOM object which is created and saved for use by the analyst report
     */
    public Document reportStatistics;
    /**
     * The root element of the document
     */
    private Element rootElement;
    /**
     * The collection of SimEntityRecords sorted by entityName
     */
    private Map<String, SimEntityRecord> entities;
    /**
     *The names that correspond to the order of the data being sent
     */
    private String[] entityNames;
    /**
     * The properties in order of the data being sent
     */
    private String[] properties;

    /** Creates a new instance of ReportStatisticsDOM */
    public ReportStatisticsDOM() {
        reportStatistics = new Document();
        rootElement = new Element("ReportStatistics");
        entities = new HashMap<>();
        reportStatistics.setRootElement(rootElement);
    }

    /**
     * Initializes all of the entities and properties in this object.  This step
     * is currently necessary because Viskit has no notion of entities and therefore
     * they cannot organize output.
     *
     * @param simEntities the names of the entities for the simulation
     * @param properties the name of the properties in the same order as the entities
     */
    public void initializeEntities(String[] simEntities, String[] properties) {

        SimEntityRecord record;
        this.entityNames = simEntities;
        this.properties = properties;

        // Create SimEntityRecords
        for (int i = 0; i < simEntities.length; i++) {
            if (!entities.containsKey(simEntities[i])) {
                record = new SimEntityRecord(simEntities[i]);
                entities.put(simEntities[i], record);
            }
            SimEntityRecord rec = entities.get(simEntities[i]);
            rec.addDataPoint(properties[i]);
        }
    }

    /**
     * Stores the replication data as it is passed to this object
     *
     * @param repData the replication information in jdom.Element form
     */
    public void storeReplicationData(Element[] repData) {

        for (int i = 0; i < repData.length; i++) {
            SimEntityRecord temp = entities.get(entityNames[i]);
            temp.addReplicationRecord(properties[i], repData[i]);
        }
    }

    /**
     * Stores the summary data for the simulation
     *
     * @param summaryData the summary data for this simulation in jdom.Element form
     */
    public void storeSummaryData(Element[] summaryData) {

        for (int i = 0; i < summaryData.length; i++) {
            SimEntityRecord temp = entities.get(entityNames[i]);
            temp.addSummaryRecord(summaryData[i]);
        }
    }

    /**
     * Returns the statistics report object created by this class
     *
     * @return reportStatistics the statistics from this simulation in jdom.Document
     *        form
     */
    public Document getReport() {
        for (SimEntityRecord record : entities.values()) {
            rootElement.addContent(record.getEntityRecord());
        }
        reportStatistics.setRootElement(rootElement);
        return reportStatistics;
    }

    /**
     * Protected inner class SimEntityRecord is used to create a single Entity
     * record which can contain multiple statistical data points
     */
    protected class SimEntityRecord {

        String entityName;
        String property;
        Element simEntity, sumReport;
        Map<String, Element> dataPointMap = new HashMap<>();

        SimEntityRecord(String entityName) {
            //Initialize the default layout
            simEntity = new Element("SimEntity");
            simEntity.setAttribute("name", entityName);
            sumReport = new Element("SummaryReport");
        }

        /**
         * Adds a data point to this SimEntityRecord which is another property change
         * listener and statistic. This will be updated after each replication.
         *
         * @param property the name of the property for this data point
         */
        protected void addDataPoint(String property) {
            Element dataPoint = new Element("DataPoint");
            Element repReport = new Element("ReplicationReport");
            dataPoint.setAttribute("property", property);
            dataPoint.addContent(repReport);
            dataPointMap.put(property, dataPoint);
        }

        /**
         * Returns this entity record object which is properly formatted
         *
         * @return simEntity returns this entity in jdom.Element form
         */
        protected Element getEntityRecord() {
            for (Element temp : dataPointMap.values()) {
                simEntity.addContent(temp);
            }
            simEntity.addContent(sumReport);
            return simEntity;
        }

        /**
         * Returns the name for this entity
         *
         * @return entityName the name for this entity
         */
        protected String getEntityName() {
            return entityName;
        }

        /**
         * Adds a properly formatted replication record as it is added to this
         * SimEntities record.
         *
         * @param property the property to udpate
         * @param repData the replication data in jdom.Element form
         */
        protected void addReplicationRecord(String property, Element repData) {
            for (String propertyKey : dataPointMap.keySet()) {
                if (propertyKey.equals(property)) {
                    Element dataPoint = dataPointMap.get(propertyKey);
                    dataPoint.getChild("ReplicationReport").addContent(repData);
                }
            }
        }

        /**
         * Adds the summary report to this SimEntity record
         *
         * @param summaryData
         */
        protected void addSummaryRecord(Element summaryData) {
            sumReport.addContent(summaryData);
        }
    }
}
