/*
 * ReportStatisticsConfig.java
 *
 * Created on July 15, 2006, 3:38 PM
 *
 * This classes serves as the intermediate step between viskit.xsd.BasicAssembly and
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
 * TODO: Remove the naming convention requiriment and index the statistics object in either the
 *       BasicAssembly or ViskitAssembly classes.
 *
 *@author Patrick Sullivan
 *@version $Id$
 */

package viskit.xsd.assembly;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import simkit.stat.SampleStatistics;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class ReportStatisticsConfig {
    
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


    /** Creates a new instance of ReportStatisticsConfig */
    public ReportStatisticsConfig(String assemblyName) {
         this.assemblyName = assemblyName;
         DecimalFormatSymbols dfs = new DecimalFormatSymbols();
         dfs.setInfinity("inf");  // xml chokes on default
         form = new DecimalFormat("0.000",dfs);
         reportStats = new ReportStatisticsDOM();
    }
    
    /**
     * Parses the key value of the replicationStatistics LHMap to create a local
     * index of entities and properties
     */
    public void setEntityIndex(LinkedList keyValues){
        System.out.println("\n\nAnalyst Report Selected for the following:");
        System.out.printf("%-20s %s",new Object[]{"Entity Name", "Data Point\n"});
        System.out.println("-----------------------------------------");    
        Iterator itr  = keyValues.iterator();
        entityIndex   = new String[keyValues.size()];
        propertyIndex = new String[keyValues.size()];
        int seperator = 0;
        int idx = 0;
        while(itr.hasNext()){
            String key = (String)itr.next();
            seperator  = findUnderscore(key);
            entityIndex[idx]   = key.substring(0, seperator);
            propertyIndex[idx] = key.substring(seperator, key.length());
            System.out.printf("%-20s %s",new Object[]{ entityIndex[idx], (propertyIndex[idx] +"\n")});
            idx++;
        }
        reportStats.initializeEntities(entityIndex,propertyIndex);
    }
    /**
     * Performs simple string parsing to find the underscore separating the 
     * EntityName and the Property Name
     *
     *@param str the string entry for the name of a property change listener
     */
    private int findUnderscore(String str){
         char letter;
         int  idx = 0; 
         for(int i = 0; i< str.length(); i++){
            letter = str.charAt(i);
            if(letter == '_')idx = i;
         }
         return idx;
        }
    /**
     * Creates a replication record for each SampleStatistics object after each
     * run.
     */
    public void processReplicationReport(int repNumber, SampleStatistics[] rep){
        //System.out.println("\n\nProcessRepReports in RepStatsConfig");
        for(int j = 0; j < rep.length; j++){
            System.out.println(rep[j].getName());
        }
        Element[] replicationUpdate = new Element[rep.length];
       
        for(int i = 0; i < rep.length; i++){
           
            Element replication = new Element("Replication");

            replication.setAttribute("number", Integer.toString(repNumber));
            replication.setAttribute("count",form.format(rep[i].getCount()));
            replication.setAttribute("minObs",form.format(rep[i].getMinObs()));
            replication.setAttribute("maxObs",form.format(rep[i].getMaxObs()));
            replication.setAttribute("mean",form.format(rep[i].getMean()));
            replication.setAttribute("stdDeviation",form.format(rep[i].getStandardDeviation()));
            replication.setAttribute("variance",form.format(rep[i].getVariance()));
             
            //replication.addContent(statRecord);
            replicationUpdate[i] = replication;
            
           
            
        }
         reportStats.storeReplicationData(replicationUpdate);
    }
    /**
     * Processes summary reports. The format of this array is the default
     * output from Viskit (statistics output ordered in the order that the PCLs were added
     * to the Assembly.
     *
     *@param sum the summary statistics provided from Viskit
     */
    public void processSummaryReport(SampleStatistics[] sum){
        
        
        Element[] summaryUpdate = new Element[sum.length];
       
        for(int i = 0; i < sum.length; i++){
            
            Element summary = new Element("Summary");

            summary.setAttribute("property", propertyIndex[i]);            
            summary.setAttribute("count",form.format(sum[i].getCount()));
            summary.setAttribute("minObs",form.format(sum[i].getMinObs()));
            summary.setAttribute("maxObs",form.format(sum[i].getMaxObs()));
            summary.setAttribute("mean",form.format(sum[i].getMean()));
            summary.setAttribute("stdDeviation",form.format(sum[i].getStandardDeviation()));
            summary.setAttribute("variance",form.format(sum[i].getVariance()));
             
            //replication.addContent(statRecord);
            summaryUpdate[i] = summary;
            
           
            
        }
         reportStats.storeSummaryData(summaryUpdate);
    }
    /**
     *Returns stats report in jdom.Document format; Naw...filename
     */
    public String getReport(){
        Document report = reportStats.getReport();
        return saveData(report);
    }
    /**
     * File I/O that saves the report in XML format
     */
    public String saveData(Document report){
        
        Date today;
        String output;
        SimpleDateFormat formatter;
        
        formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        today = new Date();
        output = formatter.format(today);
        
          try {
            XMLOutputter outputter = new XMLOutputter();
            //Create a unique file name for each DTG/Location Pair
            File anStatDir = new File("./AnalystReports/statistics");
            anStatDir.mkdirs();

            String outputFile = (author+assemblyName+"_"+ output+".xml");
            File f = new File(anStatDir,outputFile);
            FileWriter writer = new FileWriter(f);
            
            outputter.output(report, writer);
            
            writer.close();
            return f.getAbsolutePath();
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }

     }
    }

