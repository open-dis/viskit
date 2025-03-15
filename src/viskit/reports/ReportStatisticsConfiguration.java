/*
 * ReportStatisticsConfiguration.java
 *
 * Created on July 15, 2006, 3:38 PM
 */
package viskit.reports;

import edu.nps.util.Log4jUtilities;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import simkit.stat.SampleStatistics;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import viskit.doe.FileHandler;

/**
 * This class serves as the intermediate step between viskit.xsd.BasicAssembly and
 * the AnalystReport.  As of (July 2006) Viskit does not have the ability
 * to export statistical reports indexed by SimEntity.  This is a requirement for
 * the analyst report.
 *
 * To accomplish indexing ViskitAssembly.java contains a LinkedHashMap
 * (replicationStatistics). After construction this object is passed to the BasicAssembly.java
 * object. The BasicAssembly strips the keyValues from the passed object and provides those
 * values to this class.  Using an underscore '_' as a deliberate separator this class extracts
 * the name of each SimEntity for each PropChangeListener.  These names are used to index output
 * from the simulation.
 *
 * TODO: Remove the naming convention requirement and index the statistics object in either the
 *       BasicAssembly or ViskitAssembly classes.
 *
 * @author Patrick Sullivan
 * @version $Id$
 */
public class ReportStatisticsConfiguration {

    static final Logger LOG = Log4jUtilities.getLogger(ReportStatisticsConfiguration.class);
    
    static ViskitProject localViskitProject; // avoid ViskitGlobals while in separate thread context

    /**
     * The ordered list of Entities in the simulation that have property change
     * listeners
     */
    private String[] entityIndexArray;

    /**
     * The name of the property, as typed in the assembly
     */
    private String[] propertyIndexArray;

    /**
     * Used to truncate the precision of statistical results
     */
    private final DecimalFormat df1, df3;

    /**
     * The DOM object this class uses to create an XML record of the simulation
     * statistics
     */
    private ReportStatisticsDOM reportStatisticsDOM;

    /**
     * Report author (system username)
     */
    private final String author = System.getProperty("user.name"); // TODO need persistent viskt property

    /**
     * Assembly name
     */
    private final String assemblyName;

    /** Creates a new instance of ReportStatisticsConfig
     * @param assemblyName name of assembly
     * @param viskitProject must pass in viskitProject since ViskitGlobals not usable while in separate thread context
     */
    public ReportStatisticsConfiguration(String assemblyName, ViskitProject viskitProject) 
    {
        this.assemblyName  = assemblyName;
        localViskitProject = viskitProject;
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setInfinity("inf");  // xml chokes on default
        df1 = new DecimalFormat("0.", dfs);
        df3 = new DecimalFormat("0.000", dfs);
        reportStatisticsDOM = new ReportStatisticsDOM();
    }

    public void reset() {
        reportStatisticsDOM = new ReportStatisticsDOM();
    }

    /**
     * Parses the key value of the replicationStatistics LHMap to create a local
     * index of entities and properties
     * @param keyValues list of keyValues
     */
    public void setEntityIndex(List<String> keyValues) 
    {
          entityIndexArray = new String[keyValues.size()];
        propertyIndexArray = new String[keyValues.size()];

        if (!keyValues.isEmpty()) 
        {
            LOG.info(      "Replication Statistics created");
            System.out.println("Replication Statistics created"); // System output goes to console TextArea
            System.out.println("------------------------------");
            int separatorColumn;
            int entityIndex = 0;
            for (String currentKeyValue : keyValues) 
            {
                separatorColumn = findUnderscore(currentKeyValue);

                // TODO: verify this logic works with/without underscores present
                if (separatorColumn > 0)
                {
                      entityIndexArray[entityIndex] = currentKeyValue.substring(0, separatorColumn);
                    propertyIndexArray[entityIndex] = currentKeyValue.substring(separatorColumn + 1, currentKeyValue.length());
                }
                else
                {
                      entityIndexArray[entityIndex] = currentKeyValue.substring(0, separatorColumn);
                    propertyIndexArray[entityIndex] = currentKeyValue.substring(separatorColumn, currentKeyValue.length());
                }

                System.out.println("entity='" + entityIndexArray[entityIndex] + "', property='" + propertyIndexArray[entityIndex] + "'");
                entityIndex++;
            }
            System.out.println();
        }
        reportStatisticsDOM.initializeEntities(entityIndexArray, propertyIndexArray);
    }

    /**
     * Performs simple string parsing to find the underscore separating the
     * EntityName and the Property Name
     *
     * @param str the string entry for the name of a property change listener
     * @return the index of the underscore
     */
    private int findUnderscore(String str) {
        char letter;
        int idx = 0;
        for (int i = 0; i < str.length(); i++) {
            letter = str.charAt(i);
            if (letter == '_') {
                idx = i;
            }
        }
        return idx;
    }

    /**
     * Creates a replication record for each SampleStatistics object after each
     * run.
     * @param repNumber replication number
     * @param replicationStatisticsPropertyChangeListenerArray  replication statistics
     */
    public void processReplicationStatistics(int repNumber, PropertyChangeListener[] replicationStatisticsPropertyChangeListenerArray) 
    {
        LOG.debug("\n\nprocessReplicationReport in ReportStatisticsConfig");

        Element[] replicationUpdate = new Element[replicationStatisticsPropertyChangeListenerArray.length];
        Element replication;
        for (int i = 0; i < replicationStatisticsPropertyChangeListenerArray.length; i++) {

            replication = new Element("Replication");

            replication.setAttribute("number", Integer.toString(repNumber));
            replication.setAttribute("count", new DecimalFormat("0").format(((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i]).getCount()));
            replication.setAttribute("minObs", df1.format(((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i]).getMinObs()));
            replication.setAttribute("maxObs", df1.format(((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i]).getMaxObs()));
            replication.setAttribute("mean", df3.format(((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i]).getMean()));
            replication.setAttribute("stdDeviation", df3.format(((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i]).getStandardDeviation()));
            replication.setAttribute("variance", df3.format(((SampleStatistics) replicationStatisticsPropertyChangeListenerArray[i]).getVariance()));

            replicationUpdate[i] = replication;
        }
        reportStatisticsDOM.storeReplicationData(replicationUpdate);
    }

    /**
     * Processes summary reports. The format of this array is the default
     * output from Viskit (statistics output ordered in the order that the PCLs were added
     * to the Assembly.
     *
     * @param sum the summary statistics provided from Viskit
     */
    public void processSummaryReport(SampleStatistics[] sum) {

        Element[] summaryUpdate = new Element[sum.length];
        Element summary;
        for (int i = 0; i < sum.length; i++) {

            summary = new Element("Summary");

            summary.setAttribute("property", propertyIndexArray[i]);
            summary.setAttribute("numRuns", new DecimalFormat("0").format(sum[i].getCount()));
            summary.setAttribute("minObs", df1.format(sum[i].getMinObs()));
            summary.setAttribute("maxObs", df1.format(sum[i].getMaxObs()));
            summary.setAttribute("mean", df3.format(sum[i].getMean()));
            summary.setAttribute("stdDeviation", df3.format(sum[i].getStandardDeviation()));
            summary.setAttribute("variance", df3.format(sum[i].getVariance()));

            summaryUpdate[i] = summary;
        }
        reportStatisticsDOM.storeSummaryData(summaryUpdate);
    }

    /**
     * Save statistics, provide path to report
     * @return path to statistics XML file
     */
    public String saveStatisticsGetReportPath() {
        Document reportDocument = reportStatisticsDOM.getReport();
        return saveStatisticsData(reportDocument);
    }

    /**
     * File I/O that saves the report in XML format
     * @param reportDocument a data report to save
     * @return the String representation of this report
     */
    public String saveStatisticsData(Document reportDocument) 
    {
        // Create a unique file name for each DTG/Location Pair
        File analystReportStatisticsDirectory = localViskitProject.getAnalystReportStatisticsDirectory();

        String statisticsOutputFileName = (assemblyName + "_" + "Statistics" + "_" + ViskitStatics.todaysDate() + ".xml"); // superfluous: author + "_" + 
        File analystReportStatisticsDirectoryFile = new File(analystReportStatisticsDirectory, statisticsOutputFileName);

        try {
            FileHandler.marshallJdom(analystReportStatisticsDirectoryFile, reportDocument, false);
        } 
        catch (JDOMException | IOException ex) {
            if (reportDocument == null)
                LOG.error( "saveData(" + null + "} reportDocument is null, exception: "    + ex.getMessage());
            else 
            {
                LOG.error( "saveData(" + reportDocument.getRootElement() + "} exception: " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
            return null;
        }
        LOG.info("saveData(" + reportDocument.getRootElement().getName() + ") analystReportStatisticsDirectoryFile at\n      " + analystReportStatisticsDirectoryFile.getAbsolutePath());

        return analystReportStatisticsDirectoryFile.getAbsolutePath();
    }

} // end class ReportStatisticsConfiguration