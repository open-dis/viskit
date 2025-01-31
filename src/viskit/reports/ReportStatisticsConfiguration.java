/*
 * ReportStatisticsConfiguration.java
 *
 * Created on July 15, 2006, 3:38 PM
 */
package viskit.reports;

import edu.nps.util.LogUtils;

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
import viskit.ViskitGlobals;
import viskit.ViskitProject;
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

    static final Logger LOG = LogUtils.getLogger(ReportStatisticsConfiguration.class);

    /**
     * The ordered list of Entities in the simulation that have property change
     * listeners
     */
    private String[] entityIndex;

    /**
     * The name of the property, as typed in the assembly
     */
    private String[] propertyIndex;

    /**
     * Used to truncate the precision of statistical results
     */
    private final DecimalFormat df1, df3;

    /**
     * The DOM object this class uses to create an XML record of the simulation
     * statistics
     */
    private ReportStatisticsDOM reportStats;

    /**
     * Report author (system username)
     */
    private final String author = System.getProperty("user.name");

    /**
     * Assembly name
     */
    private final String assemblyName;

    /** Creates a new instance of ReportStatisticsConfig
     * @param assemblyName name of assembly
     */
    public ReportStatisticsConfiguration(String assemblyName) {
        this.assemblyName = assemblyName;
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setInfinity("inf");  // xml chokes on default
        df1 = new DecimalFormat("0.", dfs);
        df3 = new DecimalFormat("0.000", dfs);
        reportStats = new ReportStatisticsDOM();
    }

    public void reset() {
        reportStats = new ReportStatisticsDOM();
    }

    /**
     * Parses the key value of the replicationStatistics LHMap to create a local
     * index of entities and properties
     * @param keyValues list of keyValues
     */
    public void setEntityIndex(List<String> keyValues) {
        entityIndex = new String[keyValues.size()];
        propertyIndex = new String[keyValues.size()];

        if (!keyValues.isEmpty()) {
            System.out.println("Replication Statistic(s) created");
            System.out.println("--------------------------------");
            int seperator;
            int idx = 0;
            for (String key : keyValues) {
                seperator = findUnderscore(key);

                // TODO: verify this logic works with/without underscores present
                entityIndex[idx] = key.substring(0, seperator);

                if (seperator > 0)
                    propertyIndex[idx] = key.substring(seperator + 1, key.length());
                else
                    propertyIndex[idx] = key.substring(seperator, key.length());

                System.out.println(entityIndex[idx] + " " + propertyIndex[idx]);
                idx++;
            }
        }
        reportStats.initializeEntities(entityIndex, propertyIndex);
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
    public void processReplicationStatistics(int repNumber, PropertyChangeListener[] replicationStatisticsPropertyChangeListenerArray) {
        LogUtils.getLogger(ReportStatisticsConfiguration.class).debug("\n\nprocessReplicationReport in ReportStatisticsConfig");

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
        reportStats.storeReplicationData(replicationUpdate);
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

            summary.setAttribute("property", propertyIndex[i]);
            summary.setAttribute("numRuns", new DecimalFormat("0").format(sum[i].getCount()));
            summary.setAttribute("minObs", df1.format(sum[i].getMinObs()));
            summary.setAttribute("maxObs", df1.format(sum[i].getMaxObs()));
            summary.setAttribute("mean", df3.format(sum[i].getMean()));
            summary.setAttribute("stdDeviation", df3.format(sum[i].getStandardDeviation()));
            summary.setAttribute("variance", df3.format(sum[i].getVariance()));

            summaryUpdate[i] = summary;
        }
        reportStats.storeSummaryData(summaryUpdate);
    }

    /**
     * @return a stats report in jdom.Document format; Naw...filename
     */
    public String getReport() {
        Document report = reportStats.getReport();
        return saveData(report);
    }

    /**
     * File I/O that saves the report in XML format
     * @param report a data report to save
     * @return the String representation of this report
     */
    public String saveData(Document report) {
        SimpleDateFormat formatter;
        String dateFormat;

        formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        Date today = new Date();
        dateFormat = formatter.format(today);

        // Create a unique file name for each DTG/Location Pair
        ViskitProject vkp = ViskitGlobals.instance().getViskitProject();
        File anRptStatDir = vkp.getAnalystReportStatisticsDir();

        String outputFile = (author + assemblyName + "_" + dateFormat + ".xml");
        File f = new File(anRptStatDir, outputFile);

        try {
            FileHandler.marshallJdom(f, report, false);
        } catch (JDOMException | IOException ex) {
            LOG.error( ex);
            ex.printStackTrace(System.err);
            return null;
        }

        return f.getAbsolutePath();
    }

} // end class ReportStatisticsConfiguration