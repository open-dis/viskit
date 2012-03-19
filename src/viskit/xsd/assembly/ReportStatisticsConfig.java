/*
 * ReportStatisticsConfig.java
 *
 * Created on July 15, 2006, 3:38 PM
 */
package viskit.xsd.assembly;

import edu.nps.util.LogUtils;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import simkit.stat.SampleStatistics;
import viskit.VGlobals;
import viskit.ViskitProject;

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
public class ReportStatisticsConfig {

    static final Logger LOG = LogUtils.getLogger(ReportStatisticsConfig.class);

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
    private DecimalFormat form;

    /**
     * The DOM object this class uses to create an XML record of the simulation
     * statistics
     */
    private ReportStatisticsDOM reportStats;

    /**
     * Report author (system username)
     */
    private String author = System.getProperty("user.name");

    /**
     * Assembly name
     */
    private String assemblyName;
    
    /** Creates a new instance of ReportStatisticsConfig
     * @param assemblyName 
     */
    public ReportStatisticsConfig(String assemblyName) {
        this.assemblyName = assemblyName;
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setInfinity("inf");  // xml chokes on default
        form = new DecimalFormat("0.000", dfs);
        reportStats = new ReportStatisticsDOM();
    }

    public void reset() {
        reportStats = new ReportStatisticsDOM();
    }

    /**
     * Parses the key value of the replicationStatistics LHMap to create a local
     * index of entities and properties
     * @param keyValues 
     */
    public void setEntityIndex(LinkedList<String> keyValues) {
        entityIndex = new String[keyValues.size()];
        propertyIndex = new String[keyValues.size()];
      
        if (keyValues.size() > 0) {
            System.out.println("Replication Statistic(s) created");
            System.out.println("--------------------------------");
            int seperator;
            int idx = 0;
            for (String key : keyValues) {
                seperator = findUnderscore(key);

                // TODO: verify this logic works with/without underscores present
                entityIndex[idx] = key.substring(0, seperator);
                if (seperator > 0) {
                    propertyIndex[idx] = key.substring(seperator + 1, key.length());
                }
                else {
                    propertyIndex[idx] = key.substring(seperator, key.length());
                }
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
     * @param repNumber
     * @param repStats 
     */
    public void processReplicationReport(int repNumber, PropertyChangeListener[] repStats) {
        LogUtils.getLogger(ReportStatisticsConfig.class).debug("\n\nprocessReplicationReport in ReportStatisticsConfig");

        Element[] replicationUpdate = new Element[repStats.length];

        for (int i = 0; i < repStats.length; i++) {

            Element replication = new Element("Replication");

            replication.setAttribute("number", Integer.toString(repNumber));
            replication.setAttribute("count", new DecimalFormat("0").format(((SampleStatistics) repStats[i]).getCount()));
            replication.setAttribute("minObs", form.format(((SampleStatistics) repStats[i]).getMinObs()));
            replication.setAttribute("maxObs", form.format(((SampleStatistics) repStats[i]).getMaxObs()));
            replication.setAttribute("mean", form.format(((SampleStatistics) repStats[i]).getMean()));
            replication.setAttribute("stdDeviation", form.format(((SampleStatistics) repStats[i]).getStandardDeviation()));
            replication.setAttribute("variance", form.format(((SampleStatistics) repStats[i]).getVariance()));

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

        for (int i = 0; i < sum.length; i++) {

            Element summary = new Element("Summary");

            summary.setAttribute("property", propertyIndex[i]);
            summary.setAttribute("numRuns", new DecimalFormat("0").format(sum[i].getCount()));
            summary.setAttribute("minObs", form.format(sum[i].getMinObs()));
            summary.setAttribute("maxObs", form.format(sum[i].getMaxObs()));
            summary.setAttribute("mean", form.format(sum[i].getMean()));
            summary.setAttribute("stdDeviation", form.format(sum[i].getStandardDeviation()));
            summary.setAttribute("variance", form.format(sum[i].getVariance()));

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

        Date today;
        String output;
        SimpleDateFormat formatter;

        formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        today = new Date();
        output = formatter.format(today);

        FileWriter writer = null;
        try {
            XMLOutputter outputter = new XMLOutputter();
            Format fmt = Format.getPrettyFormat();
            outputter.setFormat(fmt);
            
            // Create a unique file name for each DTG/Location Pair
            ViskitProject vkp = VGlobals.instance().getCurrentViskitProject();
            File anStatDir = vkp.getAnalystReportStatisticsDir();

            String outputFile = (author + assemblyName + "_" + output + ".xml");
            File f = new File(anStatDir, outputFile);
            writer = new FileWriter(f);

            outputter.output(report, writer);

            return f.getAbsolutePath();

        } catch (IOException ioe) {
            LOG.error(ioe);
//            ioe.printStackTrace();
            return null;
        } finally {
            try {
                writer.close();
            } catch (IOException ioe) {}
        }
    }
}