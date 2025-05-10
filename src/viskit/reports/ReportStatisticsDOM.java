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
    public Document reportStatisticsDocument;
    /**
     * The root element of the document
     */
    private final Element rootElement;
    /**
     * The collection of SimEntityRecords sorted by entityName
     */
    private final Map<String, SimEntityRecord> simEntityRecordsMap;
    /**
     *The names that correspond to the order of the data being sent
     */
    private String[] simEntityNamesArray;
    /**
     * The properties in order of the data being sent
     */
    private String[] propertiesArray;

    /** Constructor creates a new instance of ReportStatisticsDOM */
    public ReportStatisticsDOM()
    {
        reportStatisticsDocument = new Document();
        rootElement = new Element("ReportStatistics");
        simEntityRecordsMap = new HashMap<>();
        reportStatisticsDocument.setRootElement(rootElement);
    }

    /**
     * Initializes all of the entities and properties in this object.
     * This step is currently necessary because Viskit has no notion of entities
     * and therefore they cannot organize output.
     *
     * @param simEntityNamesArray the names of the simEntityRecordsMap for the simulation
     * @param propertiesArray the name of the properties in the same order as the simEntityRecordsMap
     */
    public void initializeEntities(String[] simEntityNamesArray, String[] propertiesArray) 
    {
        SimEntityRecord simEntityRecord;
        this.simEntityNamesArray = simEntityNamesArray;
        this.propertiesArray     = propertiesArray;

        // Create SimEntityRecords
        for (int i = 0; i < simEntityNamesArray.length; i++) 
        {
            if (!simEntityRecordsMap.containsKey(simEntityNamesArray[i])) 
            {
                simEntityRecord = new SimEntityRecord(simEntityNamesArray[i]); // TODO missing name...
                simEntityRecordsMap.put(simEntityNamesArray[i], simEntityRecord);
            }
            SimEntityRecord rec = simEntityRecordsMap.get(simEntityNamesArray[i]);
            rec.addDataPoint(propertiesArray[i]);
        }
    }

    /**
     * Stores the replication data as it is passed to this object
     *
     * @param replicationDataElementArray the replication information in jdom.Element form
     */
    public void storeReplicationData(Element[] replicationDataElementArray) 
    {
        SimEntityRecord tempSimEntityRecord;
        for (int i = 0; i < replicationDataElementArray.length; i++) 
        {
            tempSimEntityRecord = simEntityRecordsMap.get(simEntityNamesArray[i]);
            tempSimEntityRecord.addReplicationRecord(propertiesArray[i], replicationDataElementArray[i]);
        }
    }

    /**
     * Stores the summary data for the simulation
     *
     * @param summaryDataElementArray the summary data for this simulation in jdom.Element form
     */
    public void storeSummaryData(Element[] summaryDataElementArray)
    {
        SimEntityRecord tempSimEntityRecord;
        for (int i = 0; i < summaryDataElementArray.length; i++) {
            tempSimEntityRecord = simEntityRecordsMap.get(simEntityNamesArray[i]);
            tempSimEntityRecord.addSummaryRecord(summaryDataElementArray[i]);
        }
    }

    /**
     * Returns the statistics report object created by this class
     *
     * @return reportStatisticsDocument the statistics from this simulation in jdom.Document
        form
     */
    public Document getReport() 
    {
        for (SimEntityRecord simEntityRecord : simEntityRecordsMap.values())
            rootElement.addContent(simEntityRecord.getEntityRecord());
        
        reportStatisticsDocument.setRootElement(rootElement);
        return reportStatisticsDocument;
    }

    /**
     * Protected inner class SimEntityRecord is used to create a single Entity
     * record which can contain multiple statistical data points
     */
    protected class SimEntityRecord 
    {
        String entityName;
        Element simEntityElement, summaryReportElement;
        Map<String, Element> dataPointElementMap = new HashMap<>();

        SimEntityRecord(String entityName) 
        {
            //Initialize the default layout
            simEntityElement = new Element("SimEntity");
            simEntityElement.setAttribute("name", entityName);
            simEntityElement.setAttribute("name", entityName);
            summaryReportElement = new Element("SummaryReport");
        }

        /**
         * Adds a data point to this SimEntityRecord which is another property change
         * listener and statistic. This will be updated after each replication.
         *
         * @param property the name of the property for this data point
         */
        protected void addDataPoint(String property) 
        {
            Element dataPoint = new Element("DataPoint");
            Element repReport = new Element("ReplicationReport");
            dataPoint.setAttribute("property", property);
            dataPoint.addContent(repReport);
            dataPointElementMap.put(property, dataPoint);
        }

        /**
         * Returns this entity record object which is properly formatted
         *
         * @return simEntityElement returns this entity in jdom.Element form
         */
        protected Element getEntityRecord() 
        {
            for (Element tempElement : dataPointElementMap.values())
                simEntityElement.addContent(tempElement);
            
            simEntityElement.addContent(summaryReportElement);
            return simEntityElement;
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
         * @param property the property to update
         * @param replicationDataElement the replication data in jdom.Element form
         */
        protected void addReplicationRecord(String property, Element replicationDataElement)
        {
            Element dataPointElement;
            for (String propertyKey : dataPointElementMap.keySet()) 
            {
                if (propertyKey.equals(property)) {
                    dataPointElement = dataPointElementMap.get(propertyKey);
                    dataPointElement.getChild("ReplicationReport").addContent(replicationDataElement);
                }
            }
        }

        /**
         * Adds the summary report to this SimEntity record
         *
         * @param summaryDataElement
         */
        protected void addSummaryRecord(Element summaryDataElement) 
        {
            summaryReportElement.addContent(summaryDataElement);
        }
    }
}
