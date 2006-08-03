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
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


public class AnalystReportBuilder
{
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
  //private String xreportName = "";

  /**
   * IPTF - Classification - the classfication for this entire report
   */
  //private String xclassification = "";


  /**
   * IPTF -  Author - the name of the author as it should appear on the report
   */
  //private String xauthor = "";

  /**
   * SYSPROP - Date - The date of the report (auto but modifiable)
   */
  //private String xdateOfReport = "";

  /**
   * IPCB - Whether or not to include comments in the executive summary - default to true
   */
  //private boolean xexecutiveSummaryComments = true;

  /**
   * IPTF - Comments (wrapped of the executive summary) from input field
   */
  //private String xexecutiveSummary = "";

  //SECTION II. VARIABLES
  /**
   * IPCB - Whether or not to include comments in SimulationLocation
   */
  //private boolean xprintSimLocationComments = true;

  /**
   * IPCB - Whether or not to include an image in the SimulationLocation section
   */
  //private boolean xprintSimLocationImage = true;

  /**
   * IPTF - SimulationLocation comments
   */
  //private String xsimLocComments = "";

  /**
   * IPTF - SimulationLocation conclusions
   */
  //private String xsimLocConclusions = "";

  /**
   * IPFC - Location X3D screenshot
   */
  //private String xlocationImage = "";

  /**
   * IPFC - Location of chart image (or secondary image if no chart)
   */
  //private String xchartImage = "";

  //SECTION III. VARIABLES
  /**
   * IPTF - Simulation configuration comments
   */
  //private boolean xprintSimConfigComments = true;

  /**
   * IPCB - Whether to show assembly image
   */
  //private boolean xprintAssemblyImage = true;

  /**
   * IPCB - Whether to show entityTable
   */
  //private boolean xprintEntityTable = true;

  /**
   * UTH - Assembly file
   */
  private String assemblyFile;

  /**
   * IPTF - Simulation configuration comments
   */
  //private String xsimConfigComments = "";

  /**
   * IPTF - Simulation configuration conclusions
   */
  //private String xsimConfigConclusions = "";

  /**
   * AREND - Location of the assembly image
   */
  //private String xassemblyImageLocation = "";

  /**
   * The jdom.Document object of the assembly file
   */
  private Document assemblyDocument;

  //SECTION IV. VARIABLES
  /**
   * IPCB - Whether to print parameter comments
   */
  //private boolean xprintParameterComments = true;

  /**
   * IPCB - Whether to print the parameterTable;
   */
  //private boolean xprintParameterTable = true;

  /**
   * IPTF - Parameter comments
   */
  //private String xparameterComments = "";

  /**
   * IPTF - Parameter conclusions
   */
  //private String xparameterConclusions = "";

  /**
   * The viskit.xsd.assembly.ReportStatisticsDOM object for this report
   */
  private Document statsReport;
  private String   statsReportPath;

  //SECTION V. VALUES
  /**
   * IPCB - Whether to print behavior definition comments
   */
  //private boolean xprintBehaviorDefComments = true;

  /**
   * IPCB - Whether to print event graph images
   */
  //private boolean xprintEventGraphImages = true;

  /**
   * IPCB - Wheter to print parameter and state variable information for each event graph
   */
  //private boolean xprintEventGraphDetails = true;

  /**
   * IPTF - Behavior Definition comments
   */
  //private String xbehaviorComments = "";

  /**
   * IPTF - Behavior Definition conclusions
   */
  //private String xbehaviorConclusions = "";

  /**
   * IPCB - Whether to include behavior descriptions
   */
  //private boolean xprintBehaviorDescriptions = true;


  /**
   * The file locations of the the event graph files
   */
  private LinkedList eventGraphFiles = new LinkedList();

  /**
   * The file locations of the event graph image files
   */
  private LinkedList eventGraphImages = new LinkedList();

  /**
   * The names of the event graphs being saved
   */
  private LinkedList eventGraphNames = new LinkedList();

  //SECTION VI. VALUES
  /**
   * IPCB - Whether to include stats results comments
   */
  //private boolean xprintStatsComments = true;

  /**
   * IPCB - Whether to include replication stats
   */
  //private boolean xprintReplicationStats = true;

  /**
   * IPCB - Whether to include summary stats
   */
  //private boolean xprintSummaryStats = true;

  /**
   * IPTF - The comments for the stats results setting
   */
  //private String xstatsComments = "";

  /**
   * IPTF - The conclusion for the stats results setting
   */
  //private String xstatsConclusions = "";

  /**
   * IPCB - Whether to include Conclusions/Recommendations comments
   */
  //private boolean xprintRecommendationsConclusions = true;

  /**
   * IPTF - The conclusions for this simulation
   */
  //private String xconclusions = "";

  /**
   * IPTF - The recommendations for future work
   */
  //private String xrecommendations = "";

  /**
   * The jdom.Document object that is used to build the report
   */
  private Document reportJdomDocument;

  /**
   * The file name selected by the user from "SAVE AS" menu option
   */
  private String fileName;

  /**
   * The root element of the report xml document
   */
  private Element rootElement;
  private Element execSummary;
  private Element simulationLocation;
  private Element simConfig;
  private Element entityParameters;
  private Element behaviorDefinitions;
  private Element statisticalResults;
  private Element concRec;

  private File xoutputXMLFile;

  public static void main(String[] args)
  {
    AnalystReportBuilder ar = new AnalystReportBuilder();
  } /**
   * Build a default AnalystReport object.
   */
  public AnalystReportBuilder()
  {
    initDocument();
    setDefaultValues();
  }

  /**
   * Build an analystReport object from an existing XML file
   * @param xmlFile
   */
  public AnalystReportBuilder(File xmlFile, String assyFile) throws Exception
  {
    parseXML(xmlFile);
    if(assyFile != null)
      setAssemblyFile(assyFile);
  }


  /**
   * Build an AnalystReport object from an existing statisticsReport document
   */
  public AnalystReportBuilder(String statisticsReportPath)
  {
    SAXBuilder builder = new SAXBuilder();
    try {
      Document doc = builder.build(new File(statisticsReportPath));
      setStatsReportPath(statisticsReportPath);
      setStatsReport(doc);
    }
    catch (Exception e) {
      System.err.println("Exception reading "+statisticsReportPath + " : "+e.getMessage());
    }
    initDocument();
    setDefaultValues();
  }
/*
  public AnalystReport(Document statisticsReport)
  {
    this(statisticsReport, null);
  }

  public AnalystReport(Document statisticsReport, File outFile)
  {
    this();
    if (outFile != null)
      outputXMLFile = outFile;
    else {
      try {
        outputXMLFile = File.createTempFile("ViskitAnalystReport", ".xml");
      }
      catch (IOException e) {
        System.err.println("Error creating output file: " + e.getMessage());
        outputXMLFile = null;
      }
    }

    setReportJdomDocument(new Document());
    setRootElement(new Element("AnalystReport"));
    this.setStatsReport(statisticsReport);

    if (debug) {
      setDefaultValues();//TODO: Remove after GUI is fully wired or while testing GUI
    }
    createReportXML();

  }
*/

  private void initDocument()
  {
    reportJdomDocument = new Document();
    rootElement = new Element("AnalystReport");
    reportJdomDocument.setRootElement(rootElement);

    fillDocument();
  }

  private void fillDocument()
  {
    createHeader();
    createExecutiveSummary();
    createSimulationLocation();
    createSimulationConfiguration();
    createEntityParameters();
    createBehaviorDefinitions();
    createStatisticalResults();
    createConclusionsRecommendations();
  }

  public File writeToXMLFile(File fil) throws Exception
  {
    if(fil == null)
      return writeToXMLFile();
    _writeCommon(fil);
    return fil;
  }

  public File writeToXMLFile() throws Exception
  {
    File fil = File.createTempFile("AnalystReport",".xml");
    _writeCommon(fil);
    return fil;
  }

  private void _writeCommon(File fil) throws Exception
  {
    XMLOutputter outputter = new XMLOutputter();
    FileWriter writer = new FileWriter(fil);
    outputter.output(reportJdomDocument, writer);
    writer.close();
    return;
  }

  /**
   * File I/O that saves the report in XML format
   */
  public void xsaveAnalystReportXML()
  {
    if (xoutputXMLFile == null) {
      System.err.println("Report not saved");
      return;
    }
    File file = xoutputXMLFile;

    Date today;
    String output;
    SimpleDateFormat formatter;

    formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
    today = new Date();
    output = formatter.format(today);

    try {
      XMLOutputter outputter = new XMLOutputter();
      //Create a unique file name for each DTG/Location Pair

      //todo GUI is better place for this:

/*
            File anDir = new File("./AnalystReports");
            anDir.mkdirs();

            String usr = System.getProperty("user.name");
            String outputFile = (usr + "AnalystReport_"+output+".xml");

            File file = new File(anDir,outputFile);
*/
      FileWriter writer = new FileWriter(file);
      outputter.output(reportJdomDocument, writer);
      writer.close();
      return;

    }
    catch (java.io.IOException e) {
      System.err.println("Error writing Report XML: " + e.getMessage());
      e.printStackTrace();
    }
    return;
  }

  private void parseXML(File fil) throws Exception
  {
    SAXBuilder builder = new SAXBuilder();
    reportJdomDocument = builder.build(fil);
    rootElement = reportJdomDocument.getRootElement();
    execSummary = rootElement.getChild("ExecutiveSummary");
    simulationLocation = rootElement.getChild("SimulationLocation");
    simConfig = rootElement.getChild("SimulationConfiguration");
    entityParameters = rootElement.getChild("EntityParameters");
    behaviorDefinitions = rootElement.getChild("BehaviorDefinitions");
    statisticalResults = rootElement.getChild("StatisticalResults");
    concRec = rootElement.getChild("ConclusionsRecommendations");
  }

  /**
   * Creates the report XML and saves it in report format
   */
  public void xcreateReportXML()
  {
    createHeader();
    createExecutiveSummary();
    createSimulationLocation();
    createSimulationConfiguration();
    createEntityParameters();
    createBehaviorDefinitions();
    createStatisticalResults();
    createConclusionsRecommendations();

    xsaveAnalystReportXML();

  }

  /**
   * Creates the root element for the analyst report
   */
  public void createHeader()
  {
    rootElement.setAttribute("name", "");
    rootElement.setAttribute("classification", "");
    rootElement.setAttribute("author", "");
    rootElement.setAttribute("date", "");
  }

  /**
   * Populates the executive summary portion of the AnalystReport XML
   */
  public void createExecutiveSummary()
  {
    execSummary = new Element("ExecutiveSummary");
    execSummary.setAttribute("comments", "true");
   // execSummary.addContent(makeComments("ES", ""));
    rootElement.addContent(execSummary);
  }

  /**
   * Creates the SimulationLocation portion of the analyst report XML
   */
  public void createSimulationLocation()
  {
    simulationLocation = new Element("SimulationLocation");
    simulationLocation.setAttribute("comments", "true");
    simulationLocation.setAttribute("images", "true");
    makeComments(simulationLocation,"SL", "");
    makeConclusions(simulationLocation,"SL", "");

    //simulationLocation.addContent(makeImage("Location", xchartImage));
    //simulationLocation.addContent(makeImage("Location", xlocationImage));

    rootElement.addContent(simulationLocation);
  }


  /**
   * Creates the simulation configuration portion of the Analyst report XML
   */
  private void createSimulationConfiguration()
  {
    simConfig = new Element("SimulationConfiguration");
    simConfig.setAttribute("comments", "true");
    simConfig.setAttribute("image", "true");
    simConfig.setAttribute("entityTable", "true");
    makeComments(simConfig,"SC", "");
    makeConclusions(simConfig,"SC", "");
    //simConfig.addContent(makeImage("Assembly", xassemblyImageLocation));
    if(assemblyFile != null)
      simConfig.addContent(makeEntityTable(assemblyFile));

    rootElement.addContent(simConfig);
  }

  /**
   * Creates the entity parameter section of this analyst report
   */
  private void createEntityParameters()
  {
    entityParameters = new Element("EntityParameters");
    entityParameters.setAttribute("comments", "true");
    entityParameters.setAttribute("parameterTables", "true");
    makeComments(entityParameters,"PC", "");
    makeConclusions(entityParameters,"PC", "");
    if(assemblyFile != null)
      entityParameters.addContent(makeParameterTables());

    rootElement.addContent(entityParameters);
  }


  /**
   * Creates the behavior parameters portion of the report
   */
  private void createBehaviorDefinitions()
  {
    behaviorDefinitions = new Element("BehaviorDefinitions");
    behaviorDefinitions.setAttribute("comments", "true");
    behaviorDefinitions.setAttribute("descriptions", "true");
    behaviorDefinitions.setAttribute("image", "true");
    behaviorDefinitions.setAttribute("details", "true");
    makeComments(behaviorDefinitions,"BC", "");
    makeConclusions(behaviorDefinitions,"BC", "");

    behaviorDefinitions.addContent(processBehaviors(true,true,true));

    rootElement.removeChild("BehaviorDefinitions");
    rootElement.addContent(behaviorDefinitions);
  }

  private void createStatisticalResults()
  {
    statisticalResults = new Element("StatisticalResults");
    rootElement.addContent(statisticalResults);

    statisticalResults.setAttribute("comments", "true");
    statisticalResults.setAttribute("replicationStats", "true");
    statisticalResults.setAttribute("summaryStats", "true");
    makeComments(statisticalResults,"SR", "");
    makeConclusions(statisticalResults,"SR", "");

    if (statsReportPath != null && statsReportPath.length() > 0) {
      statisticalResults.setAttribute("file", statsReportPath);

      Element sumReport = new Element("SummaryReport");
      Iterator itr = statsReport.getRootElement().getChildren("SimEntity").iterator();
      while (itr.hasNext()) {
        Element entity = (Element) itr.next();
        Element temp = (Element) entity.clone();
        temp.removeChildren("SummaryReport");

        Element summStats = entity.getChild("SummaryReport");
        Iterator summItr = summStats.getChildren("Summary").iterator();
        while (summItr.hasNext()) {
          Element temp2 = (Element) summItr.next();
          Element summaryRecord = new Element("SummaryRecord");
          summaryRecord.setAttribute("entity", entity.getAttributeValue("name"));
          summaryRecord.setAttribute("property", temp2.getAttributeValue("property"));
          summaryRecord.setAttribute("count", temp2.getAttributeValue("count"));
          summaryRecord.setAttribute("minObs", temp2.getAttributeValue("minObs"));
          summaryRecord.setAttribute("maxObs", temp2.getAttributeValue("maxObs"));
          summaryRecord.setAttribute("mean", temp2.getAttributeValue("mean"));
          summaryRecord.setAttribute("stdDeviation", temp2.getAttributeValue("stdDeviation"));
          summaryRecord.setAttribute("variance", temp2.getAttributeValue("variance"));
          sumReport.addContent(summaryRecord);
        }
      }

      statisticalResults.addContent(makeReplicationReport());
      statisticalResults.addContent(sumReport);
    }
  }

  /**
   * Creates the conclusions/Recommendations portion of the analyst report template
   */
  private void createConclusionsRecommendations()
  {
    concRec = new Element("ConclusionsRecommendations");
    concRec.setAttribute("comments", "true");
    makeComments(concRec,"CR", "");
    makeConclusions(concRec,"CR", "");
    rootElement.addContent(concRec);
  }

  /**
   * Creates Behavior definition references in the analyst report template
   */
  private Element processBehaviors(boolean descript, boolean image, boolean details)
  {
    Element behaviorList = new Element("BehaviorList");
    if (eventGraphNames != null) {
      for (int i = 0; i < eventGraphNames.size(); i++) {
        Element behavior = new Element("Behavior");
        Element rootElement = null;
        String descriptText = "";
        behavior.setAttribute("name", (String) eventGraphNames.get(i));

        if (descript) {
          try {

            Document temp = loadXML((String) eventGraphFiles.get(i));
            rootElement = temp.getRootElement();
            descriptText = temp.getRootElement().getChild("Comment").getText();
          }
          catch (IOException e) {
            System.out.println("Unable to load event graph file");
          }
          Element description = new Element("description");
          description.setAttribute("text", descriptText);
          behavior.addContent(description);

          if (details) {
            Iterator itr = rootElement.getChildren("Parameter").iterator();
            while (itr.hasNext()) {
              Element temp = (Element) itr.next();
              Element param = new Element("parameter");
              param.setAttribute("name", temp.getAttributeValue("name"));
              param.setAttribute("type", temp.getAttributeValue("type"));
              param.setAttribute("description", temp.getChildText("Comment"));
              behavior.addContent(param);

            }
            Iterator itr2 = rootElement.getChildren("StateVariable").iterator();
            while (itr2.hasNext()) {
              Element temp = (Element) itr2.next();
              Element stvar = new Element("stateVariable");
              stvar.setAttribute("name", temp.getAttributeValue("name"));
              stvar.setAttribute("type", temp.getAttributeValue("type"));
              stvar.setAttribute("description", temp.getChildText("Comment"));
              behavior.addContent(stvar);
            }
          }
        }
        if (image) {
          Element evtGraphImage = new Element("EventGraphImage");
          evtGraphImage.setAttribute("dir", (String) eventGraphImages.get(i));
          behavior.addContent(evtGraphImage);
        }
        behaviorList.addContent(behavior);
      }
    }
    return behaviorList;
  }

  List unMakeBehaviorList(Element localRoot)
  {
    Vector v = new Vector();

    Element listEl = localRoot.getChild("BehaviorList");
    List behElms = listEl.getChildren("Behavior");
    for (Iterator itr = behElms.iterator(); itr.hasNext();) {
      Vector b = new Vector();

      Element behavior = (Element) itr.next();
      String nm = behavior.getAttributeValue("name");
      b.add(nm);

      Element desc = behavior.getChild("description");
      String desctxt = desc.getAttributeValue("text");
      b.add(desctxt);

      List parms = behavior.getChildren("parameter");
      Vector p = new Vector();
      for (Iterator pitr = parms.iterator(); pitr.hasNext();) {
        Element param = (Element) pitr.next();
        String pnm = param.getAttributeValue("name");
        String pty = param.getAttributeValue("type");
        String pdsc = param.getAttributeValue("description");
        String[] pa = new String[]{pnm, pty, pdsc};
        p.add(pa);
      }
      b.add(p);

      List stvars = behavior.getChildren("stateVariable");
      Vector s = new Vector();
      for (Iterator sitr = stvars.iterator(); sitr.hasNext();) {
        Element svar = (Element) sitr.next();
        String snm = svar.getAttributeValue("name");
        String sty = svar.getAttributeValue("type");
        String sdsc = svar.getAttributeValue("description");
        String[]sa = new String[]{snm, sty, sdsc};
        s.add(sa);
      }
      b.add(s);

      Element evtGrImg = behavior.getChild("EventGraphImage");
      b.add(evtGrImg.getAttributeValue("dir"));

      v.add(b);
    }
    return v;
  }


  Vector unMakeParameterTables(Element rootOfTabs)
  {
    Element elm = rootOfTabs.getChild("ParameterTables");
    List lis = elm.getChildren("EntityParameterTable");
    Vector v = new Vector(lis.size());   // list of entpartab elms

    for(Iterator itr_0=lis.iterator();itr_0.hasNext();) {
      Element e_0 = (Element)itr_0.next();
      List lis_0 = e_0.getChildren();       //list of parts: class/id/phys/dynam
      Vector v_0 = new Vector(lis_0.size());
      for(Iterator itr_1=lis_0.iterator();itr_1.hasNext();) {
        Element e_1 = (Element)itr_1.next();
        List lis_1 = e_1.getChildren("parameter");     // list of param elms
        Vector v_1 = new Vector(lis_1.size());
        for(Iterator itr_2=lis_1.iterator();itr_2.hasNext();) {
          Element e_2 = (Element)itr_2.next();
          String name = e_2.getAttributeValue("name");
          String val  = e_2.getAttributeValue("value");
          v_1.add(new String[]{name,val});
        }
        v_0.add(new Object[]{e_1.getName(),v_1});
      }
      v.add(new Object[]{e_0.getAttributeValue("name"),v_0});
    }
    return v;
  }

  /**
   * Creates parameter tables for all files in the assembly that have SMAL definitions.
   * TODO: extract all parameters?  How to format in the report?
   */
  private Element makeParameterTables()
  {
    Element parameterTables = new Element("ParameterTables");
    Element rootElement = assemblyDocument.getRootElement();
    List simEntityList = rootElement.getChildren("SimEntity");
    Iterator itr = simEntityList.iterator();
    Element temp;
    String entityName;
    while (itr.hasNext()) {
      temp = (Element) itr.next();
      entityName = temp.getAttributeValue("name");
      List entityParams = temp.getChildren("MultiParameter");
      Iterator itr2 = entityParams.iterator();
      while (itr2.hasNext()) {
        Element param = (Element) itr2.next();
        if (param.getAttributeValue("type").equals("diskit.SMAL.EntityDefinition")) {
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
   * @param entityName the name of the entity
   * @param entityDef  the entityDefinition for this file
   * @return table the properly formatted table entries
   */
  private Element extractSMAL(String entityName, Element entityDef)
  {
    Element table = new Element("EntityParameterTable");
    ElementFilter multiParam = new ElementFilter("MultiParameter");
    Iterator itr = entityDef.getDescendants(multiParam);
    table.setAttribute("name", entityName);
    while (itr.hasNext()) {
      Element temp = (Element) itr.next();
      String category = temp.getAttributeValue("type");
      if (category.equals("diskit.SMAL.Classification")) table.addContent(makeTableEntry("Classification", temp));
      if (category.equals("diskit.SMAL.IdentificationParameters"))
        table.addContent(makeTableEntry("Identification", temp));
      if (category.equals("diskit.SMAL.PhysicalConstraints"))
        table.addContent(makeTableEntry("PhysicalConstraints", temp));
      if (category.equals("diskit.SMAL.DynamicResponseConstraints"))
        table.addContent(makeTableEntry("DynamicResponseConstraints", temp));
      if (category.equals("diskit.SMAL.TacticalConstraints"))
        table.addContent(makeTableEntry("TacticalConstraints", temp));
    }

    return table;
  }

  /**
   * Processes parameters
   *
   * @param category the category for this table entry
   * @param data     the element that corresponds to the category
   * @return tableEntry the parameter in table format
   */
  private Element makeTableEntry(String category, Element data)
  {
    Element tableEntry = new Element(category);

    List dataList = data.getChildren("TerminalParameter");
    Iterator itr = dataList.iterator();

    while (itr.hasNext()) {
      Element temp = (Element) itr.next();
      if (!temp.getAttributeValue("value").equals("0")) {
        Element param = new Element("parameter");
        param.setAttribute("name", temp.getAttributeValue("name"));
        param.setAttribute("value", temp.getAttributeValue("value"));

        tableEntry.addContent(param);
      }
    }
    return tableEntry;
  }

  public String[][] unMakeEntityTable()
  {
    Element elm = simConfig.getChild("EntityTable");
    List lis = elm.getChildren("SimEntity");

    String[][] sa = new String[lis.size()][2];
    int i=0;
    for(Iterator itr=lis.iterator();itr.hasNext();) {
      Element e = (Element)itr.next();
      sa[i]  [0] = e.getAttributeValue("name");
      sa[i++][1] = e.getAttributeValue("behaviorDefinition");
    }
    return sa;
  }

  /**
   * Creates the entity table for this analyst xml object
   *
   * @param fileDirectory the location of the assembly file
   * @return table the entityTable for the simConfig portion of the analyst report
   */
  public Element makeEntityTable(String fileDirectory)
  {
    try {
      setAssemblyDocument(loadXML(fileDirectory));
    }
    catch (IOException e) {
      System.out.println("Unable to load: " + fileDirectory);
    }

    Element entityTable = new Element("EntityTable");
    Element rootElement = assemblyDocument.getRootElement();
    List simEntityList = rootElement.getChildren("SimEntity");
    Iterator itr = simEntityList.iterator();

    //Extract XML based simEntities for entityParameters and event graph image
/*
    setEventGraphFiles(new LinkedList());
    setEventGraphImages(new LinkedList());
    setEventGraphNames(new LinkedList());
*/
    String isJAVAfile = "diskit";//if a file is in the diskit package it is native java
    while (itr.hasNext()) {
      Element temp = (Element) itr.next();
      String javaTest = (temp.getAttributeValue("type").substring(0, 6));
      //If its not a java file process it

      if (!javaTest.equals(isJAVAfile)) {
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
   * This method re-shuffles the statistics report to a format that is handled
   * by the xslt for the analyst report.  The mis-match of formatting was discovered
   * after all classes were written. This should be cleaned up or the XML formatted
   * more uniformly.
   */
  private Element makeReplicationReport()
  {
    Element repReports = new Element("ReplicationReports");
    Iterator mainItr = statsReport.getRootElement().getChildren("SimEntity").iterator();
    while (mainItr.hasNext()) {
      Element tempEntity = (Element) mainItr.next();
      Iterator itr = tempEntity.getChildren("DataPoint").iterator();
      while (itr.hasNext()) {
        Element temp = (Element) itr.next();
        Element entity = new Element("SimEntity");
        entity.setAttribute("name", tempEntity.getAttributeValue("name"));
        entity.setAttribute("property", temp.getAttributeValue("property"));
        Iterator itr2 = temp.getChildren("ReplicationReport").iterator();
        while (itr2.hasNext()) {
          Element temp3 = (Element) itr2.next();
          Iterator itr3 = temp3.getChildren("Replication").iterator();
          while (itr3.hasNext()) {
            Element temp2 = (Element) itr3.next();
            Element repRecord = new Element("Replication");
            repRecord.setAttribute("number", temp2.getAttributeValue("number"));
            repRecord.setAttribute("count", temp2.getAttributeValue("count"));
            repRecord.setAttribute("minObs", temp2.getAttributeValue("minObs"));
            repRecord.setAttribute("maxObs", temp2.getAttributeValue("maxObs"));
            repRecord.setAttribute("mean", temp2.getAttributeValue("mean"));
            repRecord.setAttribute("stdDeviation", temp2.getAttributeValue("stdDeviation"));
            repRecord.setAttribute("variance", temp2.getAttributeValue("variance"));
            entity.addContent(repRecord);
          }
          repReports.addContent(entity);
        }

      }
    }
    return repReports;

  }

  /**
   * Processes the 'type' value from a Viskit assembly, if it is an xml file, and
   * adds it to the list of event graphs with the proper formatting of the file's
   * path
   *
   * @param fileType the type of XML file being used
   */
  private void saveEventGraphReferences(String fileType)
  {
    char letter;
    int idx = 0;
    for (int i = 0; i < fileType.length(); i++) {
      letter = fileType.charAt(i);
      if (letter == '.') idx = i;

    }
    String dir = "./BehaviorLibraries/SavageTactics/" + fileType.substring(0, idx) + "/";
    String file = fileType.substring(idx + 1, fileType.length());
    String eventGraphDirectory = (dir + file + ".xml");
    String imgDirectory = "./images/BehaviorLibraries/SavageTactics/" + fileType.substring(0, idx) + "/" + file + ".xml.png";
    if (!eventGraphFiles.contains(eventGraphDirectory)) {
      eventGraphFiles.add(eventGraphDirectory);
      eventGraphImages.add(imgDirectory);
      eventGraphNames.add(fileType.substring(idx + 1, fileType.length()));
    }
  }

  /**
   * Loads an XML document file for processing
   *
   * @param fileDir the location of the file
   * @return doc the document object of the loaded XML
   */
  private Document loadXML(String fileDir) throws IOException
  {

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(new File(fileDir));
      return doc;
    }
    catch (JDOMException e) {
      e.printStackTrace();
    }
    catch (NullPointerException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Converts boolean input into a 'true'/'false' string representation for use as
   * an attribute value in the Analyst report XML.
   *
   * @param booleanFlag the boolean variable to convert
   * @return str the string representation of the boolean variable
   */
  private String booleanToString(boolean booleanFlag)
  {
    String str = "";
    if (booleanFlag) str = "true";
    if (!booleanFlag) str = "false";
    return str;
  }

  private boolean stringToBoolean(String s)
  {
    if(s.equalsIgnoreCase("true"))
      return true;
    else
      return false;
  }

  /**
   * Creates a stand 'Image' element used by all sections of the report
   *
   * @param imageId a unique identifier for this XML Element
   * @param dir     the directory of the image
   * @return image the Image url embedded in well formed XML
   */
  private Element makeImage(String imageID, String dir)
  {
    Element image = new Element(imageID + "Image");
    image.setAttribute("dir", dir);
    return image;
  }

  private String unMakeImage(Element e, String imageID)
  {
    return _unMakeContent(e,imageID+"Image");
  }
  /**
   * Creates a standard 'Comments' element used by all sections of the report
   * to add comments
   *
   * @param commentTag  the tag used to identify unique Comments (used by XSLT)
   * @param commentText the text comments
   * @return comments the Comments embedded in well formed XML
   */
  public void makeComments(Element parent, String commentTag, String commentText)
  {
    Element e = _makeContent(commentTag,"Comments",commentText);
    parent.removeChild(e.getName());
    parent.addContent(e);
  }

  public Element xmakeComments(String commentTag,String commentText)
  {
    return _makeContent(commentTag,"Comments",commentText);
  }

  private String unMakeComments(Element e)
  {
    return _unMakeContent(e,"Comments");
  }

  /**
   * Creates a standard 'Conclusions' element used by all sections of the report
   * to add conclusions
   *
   * @param commentTag     the tag used to identify unique Comments (used by XSLT)
   * @param conclusionText the text comments
   * @return conclusions the Comments embedded in well formed XML
   */
  public Element xmakeConclusions(String commentTag, String conclusionText)
  {
    return _makeContent(commentTag,"Conclusions",conclusionText);
  }

  public void makeConclusions(Element parent, String commentTag, String conclusionText)
  {
    Element e = _makeContent(commentTag,"Conclusions",conclusionText);
    parent.removeChild(e.getName());
    parent.addContent(e);
  }

  public String unMakeConclusions(Element e)
  {
    return _unMakeContent(e,"Conclusions");
  }

  private Element _makeContent(String commentTag, String suffix, String commentText)
  {
    Element comments = new Element((commentTag + suffix));
    comments.setAttribute("text", commentText);
    return comments;
  }

  private String _unMakeContent(Element e, String suffix)
  {
    List content = e.getContent();
    for(Iterator itr=content.iterator(); itr.hasNext();) {
      Element celem = (Element)itr.next();
      if(celem.getName().endsWith(suffix))
        return celem.getAttributeValue("text");
    }
    return "";
  }

  /**
   * TODO: Change this to put in appropriate sample text
   */
  private void setDefaultValues()
  {
    //Header values
    setReportName("AnalystReportTest");
    setClassification("UNCLASSIFIED");
    //setAuthor("Patrick Sullivan");
    setDateOfReport(DateFormat.getInstance().format(new Date()));

    //Executive Summary values
    setExecutiveSummaryComments(true);
    setExecutiveSummary("The purpose of this report is to test various force protection alternatives " +
        "for Naval Station Bremerton Washington. The initial motivation for this report is to provide an exemplar template of what the end product of the ATFP tool could be to allow for discussion and " +
        "suggestions among participating developers and sponsors");

    //SimulationLocation Values
    setPrintSimLocationComments(true);
    setPrintSimLocationImage(true);
    setSimLocComments("This simulation was designed to reflect real world conditions in Bremerton harbor and to evaluate the effectiveness of a ship's patrol craft at defending against a waterborne terrorist threat. " +
        "This experiment tests the ability of one patrol boat against up to three terrorist threats. The use of ship's self defense forces (SSDF) was not included in this initial experiment. Bremerton harbor" +
        "is accessible from public waterways though a floating barrier system is in place to protect the units that are docked at it's piers.  This initial simulation test does not incorporate the floating barrier system.");
    setSimLocConclusions("While this simulation was able to demonstrate that the tool could setup a simple experiment and run the exclusion of the barrier system probably renders the results useless.  This installation relies heavily on " +
        "the barrier system which it has installed. Excluding it from the simulation gives an unrealistic and unrepresentative advantage to waterborne terrorist platforms.");
    setLocationImage("C:/CVSProjects/Viskit/images/BehaviorLibraries/SavageTactics/Locations/BremertonAerial.bmp");
    setChartImage("C:/CVSProjects/Viskit/images/BehaviorLibraries/SavageTactics/Locations/BremertonChart.bmp");

    //Simulation Configuration Values
    setPrintSimConfigComments(true);
    setPrintAssemblyImage(true);
    setPrintEntityTable(true);
    setAssemblyFile("./BehaviorLibraries/SavageTactics/Scenarios/Bremerton.xml");
    setSimConfigComments("The simulation created for this reports included multiple entities as well as the well " +
        "known harbor obstructions (e.g. Mooring buoys).  A nautical chart object was included which " +
        "provided the simulation agents with an understanding of the waterfront environment by charting " +
        "both the water perimeter and waterways that could be navigated by waterborne terrorist assets. " +
        "Further discussion about agent behavior implementation is provided in the next section of this report.");
    setSimConfigConclusions("While the mooring buoys were represented in the simulation they did not have a 3D representation " +
        "for watching post-experiment replay.  Adding  a simple model of a mooring buoy would be a good addition " +
        "to this model.");

    setAssemblyImageLocation("C:/CVSProjects/Viskit/images/BehaviorLibraries/SavageTactics/Scenarios/Bremerton.xml.png");

    //Entity Parameters values
    setPrintParameterComments(true);
    setPrintParameterTable(true);
    setParameterComments("Parameters for the entities in this simulation were derived from online, unclassified sources. In the cases " +
        "where such sources were not available generic estimations were used for the parameter values. The units of measure " +
        "for the parameters listed was meters.");
    setParameterConclusions("The parameters given to the models for this simulation generated realistic enough results. It is recommended that " +
        "detailed, and perhaps classified values be incorporated for future studies.");
    //PAT TODO: BUILD PARAMETER TABLE

    //BehaviorParameter values
    setPrintBehaviorDefComments(true);
    setPrintEventGraphImages(true);
    setPrintBehaviorDescriptions(true);
    setPrintEventGraphDetails(true);
    setBehaviorComments("The agent behaviors used for this simulation were taken from the Behavior Libraries directory. No changes to these behaviors were made.");
    setBehaviorConclusions("The behaviors used in this experiment were sufficient given the overall objective.  However, it would be worthwhile to consider incorporating " +
        "classified procedures and doctrine for a more realistic harbor representation.");

    //StatisticalResults values
    setPrintStatsComments(true);
    setPrintReplicationStats(true);
    setPrintSummaryStats(true);
    setStatsComments("Given that only two replications were conducted the results derived from this simulation are not statistically significant.  They do however " +
        "demonstrate the tools ability to generate a report.");
    setStatsConclusions("Increased replications are required for any additional analysis");

    //Recommendations/Conclusions
    setPrintRecommendationsConclusions(true);
    setConclusions("This simulation was a good initial test of the functionality of this application.  The statistics derived from the simulation runs should not be " +
        "used for any purpose other than to show that the tool could generate results.");
    setRecommendations("The next step in the project is to set up an experiment with greater level of fidelity, improved agent behaviors and the inclusion of the barrier system.");
  }

  //public boolean isExecutiveSummaryComments()        { return executiveSummaryComments; }
  //public boolean isPrintAssemblyImage()              { return printAssemblyImage; }
  //public boolean isPrintBehaviorDefComments()        { return printBehaviorDefComments; }
  //public boolean isPrintBehaviorDescriptions()       { return printBehaviorDescriptions; }
  //public boolean isPrintEntityTable()                { return printEntityTable; }
  //public boolean isPrintEventGraphImages()           { return printEventGraphImages; }
  //public boolean isPrintParameterComments()          { return printParameterComments; }
  //public boolean isPrintParameterTable()             { return printParameterTable; }
  //public boolean isPrintRecommendationsConclusions() { return printRecommendationsConclusions; }
  //public boolean isPrintReplicationStats()           { return printReplicationStats; }
  //public boolean isPrintSimConfigComments()          { return printSimConfigComments; }
  //public boolean isPrintSimLocationComments()        { return printSimLocationComments; }
  //public boolean isPrintSimLocationImage()           { return printSimLocationImage; }
  //public boolean isPrintStatsComments()              { return printStatsComments; }
  //public boolean isPrintSummaryStats()               { return printSummaryStats; }

  public boolean isDebug()                           { return debug; }

  //public String     getAssemblyImageLocation() { return assemblyImageLocation; }
  //public String     getAuthor()                { return author; }
  //public String     getBehaviorComments()      { return behaviorComments; }
  //public String     getBehaviorConclusions()   { return behaviorConclusions; }
  //public String     getChartImage              { return chartImage; }
  //public String     getClassification()        { return classification; }
  //public String     getConclusions()           { return conclusions; }
  //public String     getDateOfReport()          { return dateOfReport; }
  //public String     getExecutiveSummary()      { return executiveSummary; }
  //public String     getParameterComments()     { return parameterComments; }
  //public String     getParameterConclusions()  { return parameterConclusions; }
  //public String     getRecommendations()       { return recommendations; }
  //public String     getReportName()            { return reportName; }
  //public String     getSimConfigComments()     { return simConfigComments; }
  //public String     getSimConfigConclusions()  { return simConfigConclusions; }
  //public String     getSimLocComments()        { return simLocComments; }
  //public String     getSimLocConclusions()     { return simLocConclusions; }
  //public String     getStatsComments()         { return statsComments; }
  //public String     getStatsConclusions()      { return statsConclusions; }

  public Document   getAssemblyDocument()      { return assemblyDocument; }
  public Document   getReportJdomDocument()    { return reportJdomDocument; }
  public Document   getStatsReport()           { return statsReport; }
  public Element    getRootElement()           { return rootElement; }
  public String     getFileName()              { return fileName; }
  public LinkedList getEventGraphFiles()       { return eventGraphFiles; }
  public LinkedList getEventGraphImages()      { return eventGraphImages; }
  public LinkedList getEventGraphNames()       { return eventGraphNames; }
  public String     getAssemblyFile()          { return assemblyFile; }

  public String     getAuthor()                { return rootElement.getAttributeValue("author"); }
  public String     getClassification()        { return rootElement.getAttributeValue("classification");}
  public String     getDateOfReport()          { return rootElement.getAttributeValue("date");}
  public String     getReportName()            { return rootElement.getAttributeValue("name"); }





  // public void setPrintSimConfigComments(boolean printSimConfigComments) { this.printSimConfigComments = printSimConfigComments; }
  //public void setAssemblyImageLocation(String assemblyImageLocation) { this.assemblyImageLocation = assemblyImageLocation; }
  //public void setAuthor(String author) { this.author = author; }
  //public void setBehaviorComments(String behaviorComments) { this.behaviorComments = behaviorComments; }
  //public void setBehaviorConclusions(String behaviorConclusions) { this.behaviorConclusions = behaviorConclusions; }
  //public void setClassification(String classification) { this.classification = classification; }
  //public void setConclusions(String conclusions) { this.conclusions = conclusions; }
  //public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
  //public void setExecutiveSummaryComments(boolean executiveSummaryComments) { this.executiveSummaryComments = executiveSummaryComments; }
  //public void setLocationImage(String locationImage) { this.locationImage = locationImage; }
  //public void setParameterComments(String parameterComments) { this.parameterComments = parameterComments; }
  //public void setParameterConclusions(String parameterConclusions) { this.parameterConclusions = parameterConclusions; }
  //public void setPrintBehaviorDefComments(boolean printBehaviorDefComments) { this.printBehaviorDefComments = printBehaviorDefComments; }
  //public void setPrintBehaviorDescriptions(boolean printBehaviorDescriptions) { this.printBehaviorDescriptions = printBehaviorDescriptions; }
  //public void setPrintEntityTable(boolean printEntityTable) { this.printEntityTable = printEntityTable; }
  //public void setPrintEventGraphDetails(boolean printEventGraphDetails) { this.printEventGraphDetails = printEventGraphDetails; }
  //public void setPrintEventGraphImages(boolean printEventGraphImages) { this.printEventGraphImages = printEventGraphImages; }
  //public void setPrintParameterComments(boolean printParameterComments) { this.printParameterComments = printParameterComments; }
  //public void setPrintParameterTable(boolean printParameterTable) { this.printParameterTable = printParameterTable; }
  //public void setPrintRecommendationsConclusions(boolean printRecommendationsConclusions) { this.printRecommendationsConclusions = printRecommendationsConclusions; }
  //public void setPrintReplicationStats(boolean printReplicationStats) { this.printReplicationStats = printReplicationStats; }
  //public void setPrintSimLocationComments(boolean printSimLocationComments) { this.printSimLocationComments = printSimLocationComments; }
  //public void setPrintSimLocationImage(boolean printSimLocationImage) { this.printSimLocationImage = printSimLocationImage; }
  //public void setPrintStatsComments(boolean printStatsComments) { this.printStatsComments = printStatsComments; }
  //public void setPrintSummaryStats(boolean printSummaryStats) { this.printSummaryStats = printSummaryStats; }
  //public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
  //public void setReportName(String reportName) { this.reportName = reportName; }
  //public void setSimConfigComments(String simConfigComments) { this.simConfigComments = simConfigComments; }
  //public void setSimConfigConclusions(String simConfigConclusions) { this.simConfigConclusions = simConfigConclusions; }
  //public void setSimLocConclusions(String simLocConclusions) { this.simLocConclusions = simLocConclusions; }
  //public void setStatsComments(String statsComments) { this.statsComments = statsComments; }
  //public void setStatsConclusions(String statsConclusions) { this.statsConclusions = statsConclusions; }

  public void setAssemblyDocument  (Document assemblyDocument) { this.assemblyDocument = assemblyDocument; }
  public void setAssemblyFile      (String assemblyFile)
  {
    this.assemblyFile = assemblyFile;
    simConfig.addContent(makeEntityTable(assemblyFile));
    entityParameters.addContent(makeParameterTables());
    createBehaviorDefinitions();
  }
  public void setFileName          (String fileName)           { this.fileName = fileName; }
  //public void setReportJdomDocument(Document doc)              { this.reportJdomDocument = doc; }
  //public void setRootElement       (Element el)                { this.rootElement = el; }
  public void setStatsReport       (Document statsReport)      { this.statsReport = statsReport; }
  public void setStatsReportPath   (String filename)           { this.statsReportPath = statsReportPath; }
  public void setAuthor                   (String s) { rootElement.setAttribute("author", s); };
  public void setClassification           (String s) { rootElement.setAttribute("classification", s);}
  public void setDateOfReport             (String s) { rootElement.setAttribute("date", s);}
  public void setDebug                    (boolean bool) { this.debug = bool; }
  public void setReportName              (String s) { rootElement.setAttribute("name", s); }

  public boolean isPrintRecommendationsConclusions() { return stringToBoolean(concRec.getAttributeValue("comments")); }
  public String  getConclusions()                    { return unMakeComments(concRec);}
  public String  getRecommendations()                { return unMakeConclusions(concRec);}
  public void setPrintRecommendationsConclusions (boolean bool) { concRec.setAttribute("comments", booleanToString(bool)); }
  public void setConclusions                     (String s)     { makeComments(concRec,"CR", s); }   // watch the wording
  public void setRecommendations                 (String s)     { makeConclusions(concRec,"CR", s); }

  // exec summary:
  // good
  public boolean isExecutiveSummaryComments() { return stringToBoolean(execSummary.getAttributeValue("comments"));}
  public void   setExecutiveSummaryComments   (boolean bool) { execSummary.setAttribute("comments", booleanToString(bool));}
  public String  getExecutiveSummary() { return unMakeComments(execSummary);}
  public void    setExecutiveSummary   (String s) { makeComments(execSummary,"ES", s);}

  // sim-location:
  // good
  public boolean isPrintSimLocationComments() { return stringToBoolean(simulationLocation.getAttributeValue("comments"));}
  public void    setPrintSimLocationComments(boolean bool){ simulationLocation.setAttribute("comments", booleanToString(bool));}
  public boolean isPrintSimLocationImage()    { return stringToBoolean(simulationLocation.getAttributeValue("images"));}
  public void    setPrintSimLocationImage   (boolean bool){ simulationLocation.setAttribute("images", booleanToString(bool)) ;}

  public String  getSimLocComments()          { return unMakeComments(simulationLocation);}
  public String  getSimLocConclusions()       { return unMakeConclusions(simulationLocation);}
  public String  getLocationImage()           { return unMakeImage(simulationLocation,"LocationA");}
  public String  getChartImage()              { return unMakeImage(simulationLocation,"LocationB"); }

  public void setSimLocComments          (String s)    { makeComments(simulationLocation,"SL", s);}
  public void setSimLocConclusions       (String s)    { makeConclusions(simulationLocation,"SL", s);}
  public void setLocationImage           (String s)    { simulationLocation.addContent(makeImage("LocationA", s)); }
  public void setChartImage              (String s)    { simulationLocation.addContent(makeImage("LocationB", s)); }

  // entity-parameters
  //good
  public boolean isPrintParameterComments() { return stringToBoolean(entityParameters.getAttributeValue("comments"));}
  public boolean isPrintParameterTable()    { return stringToBoolean(entityParameters.getAttributeValue("parameterTables")); }
  public void setPrintParameterComments   (boolean bool) { entityParameters.setAttribute("comments", booleanToString(bool)); }
  public void setPrintParameterTable      (boolean bool) { entityParameters.setAttribute("parameterTables", booleanToString(bool)); }

  public String  getParameterComments()    { return unMakeComments(entityParameters);}
  public String  getParameterConclusions() { return unMakeConclusions(entityParameters);}
  public Vector  getParameterTables()      { return unMakeParameterTables(entityParameters);}
  public void setParameterComments         (String s){ makeComments(entityParameters,"PC", s); }
  public void setParameterConclusions      (String s){ makeConclusions(entityParameters,"PC", s); }

  // behavior definitions:
  //good
  public boolean isPrintBehaviorDefComments()  { return stringToBoolean(behaviorDefinitions.getAttributeValue("comments"));}
  public void setPrintBehaviorDefComments (boolean bool) { behaviorDefinitions.setAttribute("comments", booleanToString(bool)); }

  public boolean isPrintBehaviorDescriptions() { return stringToBoolean(behaviorDefinitions.getAttributeValue("descriptions"));}
  public boolean isPrintEventGraphDetails()    { return stringToBoolean(behaviorDefinitions.getAttributeValue("details"));}
  public boolean isPrintEventGraphImages()     { return stringToBoolean(behaviorDefinitions.getAttributeValue("image"));}
  public void setPrintBehaviorDescriptions (boolean bool) { behaviorDefinitions.setAttribute("descriptions", booleanToString(bool)); }
  public void setPrintEventGraphDetails    (boolean bool) { behaviorDefinitions.setAttribute("details", booleanToString(bool)); }
  public void setPrintEventGraphImages     (boolean bool) { behaviorDefinitions.setAttribute("image", booleanToString(bool)); }

  public String  getBehaviorComments()         { return unMakeComments(behaviorDefinitions); }
  public String  getBehaviorConclusions()      { return unMakeConclusions(behaviorDefinitions); }
  public void setBehaviorComments         (String s) { makeComments(behaviorDefinitions,"BC", s); }
  public void setBehaviorConclusions      (String s) { makeConclusions(behaviorDefinitions,"BC", s); }
  public List getBehaviorList()          { return unMakeBehaviorList(behaviorDefinitions); }
  // sim-config:
  //good
  public boolean isPrintSimConfigComments() { return stringToBoolean(simConfig.getAttributeValue("comments"));}
  public boolean isPrintEntityTable()       { return stringToBoolean(simConfig.getAttributeValue("entityTable"));}
  public boolean isPrintAssemblyImage()     { return stringToBoolean(simConfig.getAttributeValue("image"));}
  public void    setPrintSimConfigComments  (boolean bool) { simConfig.setAttribute("comments", booleanToString(bool));}
  public void    setPrintEntityTable        (boolean bool) { simConfig.setAttribute("entityTable", booleanToString(bool)); }
  public void    setPrintAssemblyImage      (boolean bool) { simConfig.setAttribute("image", booleanToString(bool)); }

  public String  getSimConfigComments()     { return unMakeComments(simConfig);}
  public String[][]  getSimConfigEntityTable()  { return unMakeEntityTable();}
  public String  getSimConfigConclusions()  { return unMakeConclusions(simConfig);}
  public String  getAssemblyImageLocation() { return unMakeImage(simConfig,"Assembly");}
  public void    setSimConfigComments       (String s) { makeComments(simConfig,"SC", s); }
  public void    setSimConfigEntityTable    (String s) { }; //todo
  public void    setSimConfigConclusions    (String s) { makeConclusions(simConfig,"SC", s); }
  public void    setAssemblyImageLocation   (String s) { simConfig.addContent(makeImage("Assembly", s)); }

  // stat results:
  // good
  public boolean isPrintReplicationStats() { return stringToBoolean(statisticalResults.getAttributeValue("replicationStats")); }
  public boolean isPrintStatsComments()    { return stringToBoolean(statisticalResults.getAttributeValue("comments")); }
  public boolean isPrintSummaryStats()     { return stringToBoolean(statisticalResults.getAttributeValue("summaryStats")); }
  public boolean isPrintStatsCharts()      { return stringToBoolean(statisticalResults.getAttributeValue("charts")); }
  public boolean isOverlayStatsCharts()    { return stringToBoolean(statisticalResults.getAttributeValue("overlay")); }
  public void setPrintReplicationStats   (boolean bool) { statisticalResults.setAttribute("replicationStats", booleanToString(bool)); }
  public void setPrintStatsComments      (boolean bool) { statisticalResults.setAttribute("comments", booleanToString(bool)); }
  public void setPrintSummaryStats       (boolean bool) { statisticalResults.setAttribute("summaryStats", booleanToString(bool)); }
  public void setPrintStatsCharts        (boolean bool) { statisticalResults.setAttribute("charts", booleanToString(bool)); }
  public void setOverlayStatsCharts      (boolean bool) { statisticalResults.setAttribute("overlay", booleanToString(bool)); }

  public String  getStatsComments()        { return unMakeComments(statisticalResults);}
  public String  getStatsConclusions()     { return unMakeConclusions(statisticalResults);}
  public void setStatsComments           (String s) { makeComments(statisticalResults,"SR", s); }
  public void setStatsConclusions        (String s) { makeConclusions(statisticalResults,"SR", s); }


  // misc:
  public void setEventGraphFiles(LinkedList eventGraphFiles) { this.eventGraphFiles = eventGraphFiles; }
  public void setEventGraphImages(LinkedList eventGraphImages) { this.eventGraphImages = eventGraphImages; }
  public void setEventGraphNames(LinkedList eventGraphNames) { this.eventGraphNames = eventGraphNames; }
}

