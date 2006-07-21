/*
 * AnalystReportBuilder.java
 *
 * Created on July 18, 2006, 7:04 PM
 *
 * This class constructs and exports an analyst report based on the parameters selected
 * by the Analyst Report panel in the Viskit UI.  This file uses the assembly file, and
 * event graph files as well as customizable items (images, comments) to construct a report
 * that is saved in XML and html formats.
 *
 *@author Patrick Sullivan
 *@version $Id$
 */

package viskit.xsd.assembly;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.filter.ElementFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Date;
import java.util.Formatter;
import java.text.SimpleDateFormat;


public class AnalystReportBuilder {
    
    private boolean debug = true;
    //MIKE: IPTF = input that should be received from the GUI - text field likely
    //MIKE: IPCB = input that should be received from the GUI - Check box likely
    //MIKE: IPFC = input that should be received from the GUI - FileChooser likely
    //MIKE: SYSPROP = value from system, some overwritable (i.e. date of report)
    //MIKE: UTH = under the hood, get it without user involvement
    //MIKE: AREND = auto render and assign absolute path for assembly and event graph images
    //              see NOTE 1.
    
    /**
     *NOTE 1: For assembly and event graph images it would be better if they were auto
     *        generated and dropped into the report. They would probably not get included
     *        if the user had to hand generate them everytime
     *
     *NOTE 2: Variables that do not have a key are local variables that I have taken care of (e.g. statsReport)
     */
    
    //SECTION I. VARIABLES
    /**
     * IPTF - User defined name for this analyst report
     */
    private String reportName="";
    
    /**
     *IPTF - Classification - the classfication for this entire report
     */
    private String classification="";
    
    /**
     *IPTF -  Author - the name of the author as it should appear on the report
     */
    private String author="";
    
    /**
     *SYSPROP - Date - The date of the report (auto but modifiable)
     */
    private String dateOfReport="";
    
    /**
     *IPCB - Whether or not to include comments in the executive summary - default to true
     */
    private boolean executiveSummaryComments=true;
    
    /**
     *IPTF - Comments (wrapped of the executive summary) from input field
     */
    private String executiveSummary="";
    
    
    //SECTION II. VARIABLES
    /**
     *IPCB - Whether or not to include comments in SimulationLocation 
     */
    private boolean printSimLocationComments=true;
    
    /**
     *IPCB - Whether or not to include an image in the SimulationLocation section
     */
    private boolean printSimLocationImage=true;
    
    /**
     *IPTF - SimulationLocation comments 
     */
    private String simLocComments="";
    
    /**
     *IPTF - SimulationLocation conclusions
     */
    private String simLocConclusions="";
    
    /**
     *IPFC - Location X3D screenshot 
     */
    private String locationImage="";
    

    //SECTION III. VARIABLES
    /**
     *IPTF - Simulation configuration comments
     */
    private boolean printSimConfigComments=true;
    
    /**
     *IPCB - Whether to show assembly image
     */
    private boolean printAssemblyImage=true;
    
    /**
     *IPCB - Whether to show entityTable
     */
    private boolean printEntityTable=true;
    
    /**
     *UTH - Assembly file
     */
    private String assemblyFile="";
    
    /**
     *IPTF - Simulation configuration comments
     */
    private String simConfigComments="";
    
    /**
     *IPTF - Simulation configuration conclusions
     */
    private String simConfigConclusions="";
    
    /**
     *AREND - Location of the assembly image
     */
    private String assemblyImageLocation="";
    
    /**
     *The jdom.Document object of the assembly file
     */
    private Document assemblyDocument;
    
    
    //SECTION IV. VARIABLES
    /**
     *IPCB - Whether to print parameter comments
     */
    private boolean printParameterComments=true;
    
    /**
     *IPCB - Whether to print the parameterTable;
     */
    private boolean printParameterTable=true;
    
    /**
     *IPTF - Parameter comments
     */
    private String parameterComments="";
    
    /**
     *IPTF - Parameter conclusions
     */
    private String parameterConclusions="";
    
    /**
     *The viskit.xsd.assembly.ReportStatisticsDOM object for this report
     */
    private Document statsReport;
    
    //SECTION V. VALUES
    /**
     *IPCB - Whether to print behavior definition comments
     */
    private boolean printBehaviorDefComments=true;
    
    /**
     *IPCB - Whether to print event graph images
     */
    private boolean printEventGraphImages=true;
    
    /**
     *IPTF - Behavior Definition comments
     */
    private String behaviorComments="";
    
    /**
     *IPTF - Behavior Definition conclusions
     */
    private String behaviorConclusions="";
    
    /**
     *IPCB - Whether to include behavior descriptions
     */
    private boolean printBehaviorDescriptions=true;
    
    
    /**
     *The file locations of the the event graph files
     */
    private LinkedList eventGraphFiles;
    
    /**
     *The file locations of the event graph image files
     */
    private LinkedList eventGraphImages;
    
    /**
     *The names of the event graphs being saved
     */
    private LinkedList eventGraphNames;
    
    //SECTION VI. VALUES
    /**
     *IPCB - Whether to include stats results comments
     */
    private boolean printStatsComments=true;
    
    /**
     *IPCB - Whether to include replication stats
     */
    private boolean printReplicationStats=true;
    
    /**
     *IPCB - Whether to include summary stats
     */
    private boolean printSummaryStats=true;
    
    /**
     *IPTF - The comments for the stats results setting
     */
    private String statsComments="";
    
    /**
     *IPTF - The conclusion for the stats results setting
     */
    private String statsConclusions="";
    
    /**
     *IPCB - Whether to include Conclusions/Recommendations comments
     */
    private boolean printRecommendationsConclusions=true;
    
    /**
     *IPTF - The conclusions for this simulation
     */
    private String conclusions="";
    
    /**
     *IPTF - The recommendations for future work
     */
    private String recommendations="";
    
    /**
     * The jdom.Document object that is used to build the report
     */
    private Document reportXML;
    
    /**
     * The file name selected by the user from "SAVE AS" menu option
     */
    private String fileName;
    
    /**
     * The root element of the report xml
     */
    private Element rootElement;
    
    /** Creates a new instance of AnalystReportBuilder */
    public AnalystReportBuilder(Document statisticsReport) {
     
      reportXML   = new Document();
      rootElement = new Element("AnalystReport");
      this.statsReport = statisticsReport;
      
      if(debug){
      setTestValues();//TODO: Remove after GUI is fully wired or while testing GUI
      }
      createReportXML();
          
    }  
      /**
       *Creates the report XML and saves it in report format
       */
      public void createReportXML(){
      createHeader();
      createExecutiveSummary();
      createSimulationLocation();
      createSimulationConfiguration();
      createEntityParameters();
      createBehaviorDefinitions();
      createStatisticalResults();
      createConclusionsRecommendations();
      
       saveAnalystReportXML();
      
    }
    /**
     * Creates the root element for the analyst report
     */
    public void createHeader(){
        reportXML.setRootElement(rootElement);
        rootElement.setAttribute("name", reportName);
        rootElement.setAttribute("classification", classification);
        rootElement.setAttribute("author", author);
        rootElement.setAttribute("date", dateOfReport);
        
    }
    /**
     * Populates the executive summary portion of the AnalystReport XML
     */
    public void createExecutiveSummary(){
        Element execSummary = new Element("ExecutiveSummary");
        execSummary.setAttribute("comments", booleanToString(executiveSummaryComments));
        if(executiveSummaryComments){
            execSummary.addContent(makeComments(executiveSummary));
        }
        rootElement.addContent(execSummary);
    }
          
    /**
     * Creates the SimulationLocation portion of the analyst report XML
     */
    public void createSimulationLocation(){
        Element simulationLocation = new Element("SimulationLocation");
        simulationLocation.setAttribute("comments", booleanToString(printSimLocationComments));
        simulationLocation.setAttribute("images", booleanToString(printSimLocationImage));
        if(printSimLocationComments){
            simulationLocation.addContent(makeComments(simLocComments));
            simulationLocation.addContent(makeConclusions(simLocConclusions));
        }
        if(printSimLocationImage)simulationLocation.addContent(makeImage(locationImage));
        
        rootElement.addContent(simulationLocation);
    }
          
    
    /**
     * Creates the simulation configuration portion of the Analyst report XML
     */
    private void createSimulationConfiguration(){
        Element simConfig = new Element("SimulationConfiguration");
        simConfig.setAttribute("comments", booleanToString(printSimConfigComments));
        simConfig.setAttribute("image", booleanToString(printAssemblyImage));
        simConfig.setAttribute("entityTable", booleanToString(printEntityTable));
        if(printSimConfigComments)simConfig.addContent(makeComments(simConfigComments));
        if(printSimConfigComments)simConfig.addContent(makeConclusions(simConfigConclusions));
        if(printAssemblyImage)simConfig.addContent(makeImage(assemblyImageLocation));
        if(printEntityTable)simConfig.addContent(makeEntityTable(assemblyFile));
        rootElement.addContent(simConfig);   
    }
        
    /**
     * Creates the entity parameter section of this analyst report
     */
    private void createEntityParameters(){
        Element entityParameters = new Element("EntityParameters");
        entityParameters.setAttribute("comments", booleanToString(printParameterComments));
        entityParameters.setAttribute("parameterTables", booleanToString(printParameterTable));
        if(printParameterComments)entityParameters.addContent(makeComments(parameterComments));
        if(printParameterComments)entityParameters.addContent(makeConclusions(parameterConclusions));
        if(printParameterTable)entityParameters.addContent(makeParameterTables());
        
        rootElement.addContent(entityParameters);
            
        }
          
    
    /**
     * Creates the behavior parameters portion of the report
     */
    private void createBehaviorDefinitions(){
        Element behaviorDefinitions = new Element("BehaviorDefinitions");
        behaviorDefinitions.setAttribute("comments", booleanToString(printBehaviorDefComments));
        behaviorDefinitions.setAttribute("descriptions", booleanToString(printBehaviorDescriptions));
        behaviorDefinitions.setAttribute("image", booleanToString(printEventGraphImages));
        if(printBehaviorDefComments)behaviorDefinitions.addContent(makeComments(behaviorComments));
        if(printBehaviorDefComments)behaviorDefinitions.addContent(makeConclusions(behaviorConclusions));
        behaviorDefinitions.addContent(processBehaviors(printBehaviorDescriptions, printEventGraphImages));
        
        rootElement.addContent(behaviorDefinitions);
    }
     
    private void createStatisticalResults(){
        Element statisticalResults = new Element("StatisticalResults");
        statisticalResults.setAttribute("comments",booleanToString(printStatsComments));
        statisticalResults.setAttribute("replicationStats", booleanToString(printReplicationStats));
        statisticalResults.setAttribute("summaryStats", booleanToString(printSummaryStats));
        Element statsFile = new Element("ReportStatistics");
        statsFile.setAttribute("file","TODO need to get file name of statsReport file");
        statisticalResults.addContent(statsFile);
        rootElement.addContent(statisticalResults);
        //TODO: need to get file location/path from the local copy of stats report
    }
    
    /**
     * Creates the conclusions/Recommendations portion of the analyst report template
     */
    private void createConclusionsRecommendations(){
        Element concRec = new Element("ConclusionsRecommendations");
        concRec.setAttribute("comments", booleanToString(printRecommendationsConclusions));
        if(printRecommendationsConclusions){
            concRec.addContent(makeComments(conclusions));
            concRec.addContent(makeConclusions(recommendations));
        }
        rootElement.addContent(concRec);
    }
    /**
     *Creates Behavior definition references in the analyst report template
     */
    private Element processBehaviors(boolean descript, boolean image){
        Element behaviorList = new Element("BehaviorList");
        for(int i = 0; i<eventGraphNames.size(); i++){
            Element behavior = new Element("Behavior");
            String descriptText="";
            behavior.setAttribute("name", (String)eventGraphNames.get(i));
           
            if(descript){
                  try{
                    Document temp = loadXML((String)eventGraphFiles.get(i)); 
                    descriptText = temp.getRootElement().getChild("Comment").getText();
                }catch(IOException e){
                    System.out.println("Unable to load event graph file");
                }
                Element description = new Element("description");
                description.setAttribute("text", descriptText);
                behavior.addContent(description);
            }
            if(image){
                Element evtGraphImage = new Element("EventGraphImage");
                evtGraphImage.setAttribute("dir",(String)eventGraphImages.get(i));
                behavior.addContent(evtGraphImage);
            }
            behaviorList.addContent(behavior);
        }
        return behaviorList;
    }
    /**
     *Creates parameter tables for all files in the assembly that have SMAL definitions.
     *TODO: extract all parameters?  How to format in the report?
     *
     */
     private Element makeParameterTables(){
         Element parameterTables = new Element("ParameterTables");
         Element rootElement = assemblyDocument.getRootElement();
         List simEntityList  = rootElement.getChildren("SimEntity");
         Iterator itr = simEntityList.iterator();
         Element temp;
         String entityName;
         while(itr.hasNext()){
             temp = (Element)itr.next();
             entityName = temp.getAttributeValue("name");
             List entityParams = temp.getChildren("MultiParameter");
             Iterator itr2 = entityParams.iterator();
             while(itr2.hasNext()){
                 Element param = (Element)itr2.next();
                 if(param.getAttributeValue("type").equals("diskit.SMAL.EntityDefinition")){
                 parameterTables.addContent(extractSMAL(entityName, param));
                 }
             }
         }
         return parameterTables;
     }
     /**
      * Takes viskit.Assembly formatted SMAL.EntityDefinition data and formats it for the
      * analyst report
      *
      *@param entityName the name of the entity
      *@param entityDef the entityDefinition for this file
      *@return table the properly formatted table entries
      */
     private Element extractSMAL(String entityName, Element entityDef){
        Element table     = new Element("EntityParameterTable");
        ElementFilter  multiParam = new ElementFilter("MultiParameter");
        Iterator itr = entityDef.getDescendants(multiParam);
        table.setAttribute("name", entityName);
        while(itr.hasNext()){
            Element temp = (Element)itr.next();
            String category = temp.getAttributeValue("type");
            if(category.equals("diskit.SMAL.Classification"))table.addContent(makeTableEntry("Classification", temp));
            if(category.equals("diskit.SMAL.IdentificationParameters"))table.addContent(makeTableEntry("Identification", temp));
            if(category.equals("diskit.SMAL.PhysicalConstraints"))table.addContent(makeTableEntry("PhysicalConstraints",temp));
            if(category.equals("diskit.SMAL.DynamicResponseConstraints"))table.addContent(makeTableEntry("DynamicResponseConstraints", temp));
            if(category.equals("diskit.SMAL.TacticalConstraints"))table.addContent(makeTableEntry("TacticalConstraints",temp));
        }
        
        return table;
     }
     /**
      * Processes parameters
      *
      *@param category the category for this table entry
      *@param data the element that corresponds to the category
      *@return tableEntry the parameter in table format
      */
      private Element makeTableEntry(String category, Element data){
        Element tableEntry = new Element(category);
       
        List dataList = data.getChildren("TerminalParameter");
        Iterator itr = dataList.iterator();
        
        while(itr.hasNext()){
            Element temp = (Element)itr.next();
            if(!temp.getAttributeValue("value").equals("0")){
                Element param = new Element("parameter");
                param.setAttribute("name", temp.getAttributeValue("name"));
                param.setAttribute("value", temp.getAttributeValue("value"));
        
                tableEntry.addContent(param);
            }
        }
        return tableEntry;
      }
    /**
     * Creates the entity table for this analyst xml object
     *
     *@param fileDirectory the location of the assembly file
     *@return table the entityTable for the simConfig portion of the analyst report
     */
    public Element makeEntityTable(String fileDirectory){
        try{
        assemblyDocument = loadXML(fileDirectory);
        }catch(IOException e){
            System.out.println("Unable to load: " + fileDirectory);
        }
        
        Element  entityTable = new Element("EntityTable");
        Element  rootElement = assemblyDocument.getRootElement();
        List     simEntityList = rootElement.getChildren("SimEntity");
        Iterator itr = simEntityList.iterator();
        
        //Extract XML based simEntities for entityParameters and event graph image
        eventGraphFiles = new LinkedList();
        eventGraphImages = new LinkedList();
        eventGraphNames  = new LinkedList();
        String isJAVAfile = "diskit";//if a file is in the diskit package it is native java
        
        while(itr.hasNext()){
            Element temp = (Element)itr.next();
            String javaTest = (temp.getAttributeValue("type").substring(0,6));
            //If its not a java file process it
           
            if(!javaTest.equals(isJAVAfile)){
            Element tableEntry = new Element("SimEntity");
            tableEntry.setAttribute("name", temp.getAttributeValue("name"));
            tableEntry.setAttribute("behaviorDefinition", temp.getAttributeValue("type"));
            saveEventGraphReferences(temp.getAttributeValue("type"));
            entityTable.addContent(tableEntry);
            }
        }
        
        
        return entityTable;
    }
    /**
     * Processes the 'type' value from a Viskit assembly, if it is an xml file, and
     * adds it to the list of event graphs with the proper formatting of the file's
     * path
     *
     *@param fileType the type of XML file being used
     */
    private void saveEventGraphReferences(String fileType){
        
         char letter;
         int  idx = 0; 
         for(int i = 0; i< fileType.length(); i++){
            letter = fileType.charAt(i);
            if(letter == '.')idx = i;
            
         }
         String dir = "./BehaviorLibraries/SavageTactics/"+fileType.substring(0,idx)+"/";
         String file = fileType.substring(idx+1, fileType.length());
         String eventGraphDirectory = (dir + file +".xml");
         String imgDirectory = "./images/BehaviorLibraries/SavageTactics/"+fileType.substring(0,idx)+"/"+ file +".png";
         if(!eventGraphFiles.contains(eventGraphDirectory)){
             eventGraphFiles.add(eventGraphDirectory);
             eventGraphImages.add(imgDirectory);
             eventGraphNames.add(fileType.substring(idx+1,fileType.length()));
         }
        }
    
    /**
     * Loads an XML document file for processing
     *
     * @param fileDir the location of the file
     * @return doc the document object of the loaded XML
     */
    private Document loadXML(String fileDir)throws IOException{
        
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File(fileDir));
            return doc;
        } catch(JDOMException e) {
            e.printStackTrace();
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     *Converts boolean input into a 'true'/'false' string representation for use as
     *an attribute value in the Analyst report XML.
     *
     *@param booleanFlag the boolean variable to convert
     *@return str the string representation of the boolean variable
     */
    private String booleanToString(boolean booleanFlag){
        String str = "";
        if(booleanFlag)str = "true";
        if(!booleanFlag)str = "false";
        return str;
    }
    /**
     * Creates a stand 'Image' element used by all sections of the report
     *
     *@param dir the directory of the image
     *@return image the Image url embedded in well formed XML
     */
    private Element makeImage(String dir){
        Element image = new Element("Image");
        image.setAttribute("dir", dir);
        return image;
    }
    /**
     * Creates a standard 'Comments' element used by all sections of the report
     * to add comments
     *
     *@param commentText the text comments
     *@return comments the Comments embedded in well formed XML
     */          
    public Element makeComments(String commentText){
        Element comments = new Element("Comments");
        comments.setAttribute("text", commentText);
        return comments;
    }
    /**
     * Creates a standard 'Conclusions' element used by all sections of the report
     * to add conclusions
     *
     *@param conclusionText the text comments
     *@return conclusions the Comments embedded in well formed XML
     */ 
    public Element makeConclusions(String conclusionText){
        Element conclusions = new Element("Conclusions");
        conclusions.setAttribute("text", conclusionText);
        return conclusions;
    }
     /**
     * File I/O that saves the report in XML format
     */
    public void saveAnalystReportXML(){
        Date today;
        String output;
        SimpleDateFormat formatter;
        
        formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        today = new Date();
        output = formatter.format(today);
          
          try {
            XMLOutputter outputter = new XMLOutputter();
            //Create a unique file name for each DTG/Location Pair
            String usr = System.getProperty("user.name");
           
            String outputFile = ("./AnalystReports/" + usr + "AnalystReport_"+output+".xml");
            FileWriter writer = new FileWriter(outputFile);
            outputter.output(reportXML, writer);
            writer.close();
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
     }
    
    /**
     * TODO: Remove this method and all values. This was just setup to test XML and html output
     */
    private void setTestValues(){
        //Header values
        reportName               = "AnalystReportTest";
        classification           = "UNCLASSIFIED";
        author                   = "Patrick Sullivan";
        dateOfReport             = "July 19, 2006";
        
        //Executive Summary values
        executiveSummaryComments = true;
        executiveSummary         ="The purpose of this report is to test various force protection alternatives " +
                                  "for Naval Station Bremerton Washington. The initial motivation for this report is to provide an exemplar template of what the end product of the ATFP tool could be to allow for discussion and " +
                                  "suggestions among participating developers and sponsors";
        
        //SimulationLocation Values
        printSimLocationComments = true;
        printSimLocationImage    = true;
        simLocComments           ="This simulation was designed to reflect real world conditions in Bremerton harbor and to evaluate the effectiveness of a ship's patrol craft at defending against a waterborne terrorist threat. " +
                                     "This experiment tests the ability of one patrol boat against up to three terrorist threats. The use of ship's self defense forces (SSDF) was not included in this initial experiment. Bremerton harbor" +
                                     "is accessible from public waterways though a floating barrier system is in place to protect the units that are docked at it's piers.  This initial simulation test does not incorporate the floating barrier system.";
        simLocConclusions        ="While this simulation was able to demonstrate that the tool could setup a simple experiment and run the exclusion of the barrier system probably renders the results useless.  This installation relies heavily on " +
                                     "the barrier system which it has installed. Excluding it from the simulation gives an unrealistic and unrepresentative advantage to waterborne terrorist platforms.";
        locationImage            ="C:/www.web3D.org/x3d/content/examples/SavageDefense/Locations/Naval-Station-Bremerton-WA/X3D.bmp";
        
        //Simulation Configuration Values
        printSimConfigComments          = true;
        printAssemblyImage              = true;
        printEntityTable                = true;
        assemblyFile                    ="./BehaviorLibraries/SavageTactics/Scenarios/Bremerton.xml";       
        simConfigComments               ="The simulation created for this reports included multiple entities as well as the well " +
                                         "known harbor obstructions (e.g. Mooring buoys).  A nautical chart object was included which " +
                                         "provided the simulation agents with an understanding of the waterfront environment by charting " +
                                         "both the water perimeter and waterways that could be navigated by waterborne terrorist assets. " +
                                         "Further discussion about agent behavior implementation is provided in the next section of this report.";
        simConfigConclusions            ="While the mooring buoys were represented in the simulation they did not have a 3D representation " +
                                         "for watching post-experiment replay.  Adding  a simple model of a mooring buoy would be a good addition " +
                                         "to this model.";
        assemblyImageLocation           ="C:/CVSProjects/Viskit/BehaviorLibraries/SavageTactics/Scenarios/Bremerton.png";
        
        
        //Entity Parameters values
        printParameterComments      = true;
        printParameterTable         = true;
        parameterComments           ="Parameters for the entities in this simulation were derived from online, unclassified sources. In the cases " +
                                     "where such sources were not available generic estimations were used for the parameter values. The units of measure " +
                                     "for the parameters listed was meters.";
        parameterConclusions        ="The parameters given to the models for this simulation generated realistic enough results. It is recommended that " +
                                     "detailed, and perhaps classified values be incorporated for future studies.";
        //PAT TODO: BUILD PARAMETER TABLE
        
        //BehaviorParameter values
        printBehaviorDefComments   = true;
        printEventGraphImages      = true;
        printBehaviorDescriptions  = true;
        behaviorComments           ="The agent behaviors used for this simulation were taken from the Behavior Libraries directory. No changes to these behaviors were made.";
        behaviorConclusions        ="The behaviors used in this experiment were sufficient given the overall objective.  However, it would be worthwhile to consider incorporating " +
                                    "classified procedures and doctrine for a more realistic harbor representation.";
        
        //StatisticalResults values
        printStatsComments         = true;
        printReplicationStats      = true;
        printSummaryStats          = true;
        statsComments              ="Given that only two replications were conducted the results derived from this simulation are not statistically significant.  They do however " +
                                    "demonstrate the tools ability to generate a report.";
        statsConclusions           ="Increased replications are required for any additional analysis";
        
        //Recommendations/Conclusions
        printRecommendationsConclusions = true;
        conclusions                ="This simulation was a good initial test of the functionality of this application.  The statistics derived from the simulation runs should not be " +
                                    "used for any purpose other than to show that the tool could generate results.";
        recommendations            ="The next step in the project is to set up an experiment with greater level of fidelity, improved agent behaviors and the inclusion of the barrier system.";
    }
}

