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


public class AnalystReportBuilder {
    
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
    private String reportName;
    
    /**
     *IPTF - Classification - the classfication for this entire report
     */
    private String classification;
    
    /**
     *IPTF -  Author - the name of the author as it should appear on the report
     */
    private String author;
    
    /**
     *SYSPROP - Date - The date of the report (auto but modifiable)
     */
    private String dateOfReport;
    
    /**
     *IPCB - Whether or not to include comments in the executive summary - default to true
     */
    private boolean executiveSummaryComments;
    
    /**
     *IPTF - Comments (wrapped of the executive summary) from input field
     */
    private String executiveSummary;
    
    
    //SECTION II. VARIABLES
    /**
     *IPCB - Whether or not to include comments in SimulationLocation 
     */
    private boolean printSimLocationComments;
    
    /**
     *IPCB - Whether or not to include an image in the SimulationLocation section
     */
    private boolean printSimLocationImage;
    
    /**
     *IPTF - SimulationLocation comments 
     */
    private String simLocComments;
    
    /**
     *IPTF - SimulationLocation conclusions
     */
    private String simLocConclusions;
    
    /**
     *IPFC - Location X3D screenshot 
     */
    private String locationImage;
    

    //SECTION III. VARIABLES
    /**
     *IPTF - Simulation configuration comments
     */
    private boolean printSimConfigComments;
    
    /**
     *IPCB - Whether to show assembly image
     */
    private boolean printAssemblyImage;
    
    /**
     *IPCB - Whether to show entityTable
     */
    private boolean printEntityTable;
    
    /**
     *UTH - Assembly file
     */
    private String assemblyFile;
    
    /**
     *IPTF - Simulation configuration comments
     */
    private String simConfigComments;
    
    /**
     *IPTF - Simulation configuration conclusions
     */
    private String simConfigConclusions;
    
    /**
     *AREND - Location of the assembly image
     */
    private String assemblyImageLocation;
    
    /**
     *The jdom.Document object of the assembly file
     */
    private Document assemblyDocument;
    
    
    //SECTION IV. VARIABLES
    /**
     *IPCB - Whether to print parameter comments
     */
    private boolean printParameterComments;
    
    /**
     *IPCB - Whether to print the parameterTable;
     */
    private boolean printParameterTable;
    
    /**
     *IPTF - Parameter comments
     */
    private String parameterComments;
    
    /**
     *IPTF - Parameter conclusions
     */
    private String parameterConclusions;
    
    /**
     *The viskit.xsd.assembly.ReportStatisticsDOM object for this report
     */
    private Document statsReport;
    
    //SECTION V. VALUES
    /**
     *IPCB - Whether to print behavior definition comments
     */
    private boolean printBehaviorDefComments;
    
    /**
     *IPCB - Whether to print event graph images
     */
    private boolean printEventGraphImages;
    
    /**
     *IPTF - Behavior Definition comments
     */
    private String behaviorComments;
    
    /**
     *IPTF - Behavior Definition conclusions
     */
    private String behaviorConclusions;
    
    /**
     *UTH - The file locations of the the event graph files
     */
    private String[] eventGraphFiles;
    
    /**
     *AREND - The file locations of the event graph image files
     */
    private String[] eventGraphImages;
    
    //SECTION VI. VALUES
    /**
     *IPCB - Whether to include stats results comments
     */
    private boolean printStatsComments;
    
    /**
     *IPCB - Whether to include replication stats
     */
    private boolean printReplicationStats;
    
    /**
     *IPCB - Whether to include summary stats
     */
    private boolean printSummaryStats;
    
    /**
     *IPTF - The comments for the stats results setting
     */
    private String statsComments;
    
    /**
     *IPTF - The conclusion for the stats results setting
     */
    private String statsConclusions;
    
    /**
     *IPCB - Whether to include Conclusions/Recommendations comments
     */
    private boolean printRecommendationsConclusions;
    
    /**
     *IPTF - The conclusions for this simulation
     */
    private String conclusions;
    
    /**
     *IPTF - The recommendations for future work
     */
    private String recommendations;
    
    
    /** Creates a new instance of AnalystReportBuilder */
    public AnalystReportBuilder(Document statisticsReport) {
     /***********************************************************************************
      *TODO: MIKE - remove after all of the wiring up is done this just tests dummy values
      ***********************************************************************************/
      setTestValues();
     
      this.statsReport = statisticsReport;
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
        assemblyFile                    ="C:/CVSProjects/Viskit/BehaviorLibraries/SavageTactics/Scenarios/BremertonNoPing.xml";       
        simConfigComments               ="The simulation created for this reports included multiple entities as well as the well " +
                                         "known harbor obstructions (e.g. Mooring buoys).  A nautical chart object was included which " +
                                         "provided the simulation agents with an understanding of the waterfront environment by charting " +
                                         "both the water perimeter and waterways that could be navigated by waterborne terrorist assets. " +
                                         "Further discussion about agent behavior implementation is provided in the next section of this report.";
        simConfigConclusions            ="While the mooring buoys were represented in the simulation they did not have a 3D representation " +
                                         "for watching post-experiment replay.  Adding  a simple model of a mooring buoy would be a good addition " +
                                         "to this model.";
        assemblyImageLocation           ="C:/CVSProjects/Viskit/BehaviorLibraries/SavageTactics/Scenarios/Bremerton.png";
        //PAT TODO: FILE IO to read in assembly file for Entity Table
        
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

