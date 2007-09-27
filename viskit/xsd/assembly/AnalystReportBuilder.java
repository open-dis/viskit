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
import org.jdom.output.Format;
import org.jdom.filter.ElementFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.text.DateFormat;


public class AnalystReportBuilder
{
  private boolean debug = false;

  /**
   * UTH - Assembly file
   */
  private String assemblyFile;

  /**
   * The jdom.Document object of the assembly file
   */
  private Document assemblyDocument;

  /**
   * The viskit.xsd.assembly.ReportStatisticsDOM object for this report
   */
  private Document statsReport;
  private String   statsReportPath;

  /**
   * The names and file locations of the the event graph files and image files being linked
   */
  private LinkedList eventGraphNames  = new LinkedList();
  private LinkedList eventGraphFiles  = new LinkedList();
  private LinkedList eventGraphImages = new LinkedList();

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

  public static void main(String[] args)
  {
    AnalystReportBuilder ar = new AnalystReportBuilder();
  } 
  
  /**
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

  /**
    * File I/O that saves the report in XML format
    */
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
    Format form = Format.getPrettyFormat();
    outputter.setFormat(form);

    FileWriter writer = new FileWriter(fil);
    outputter.output(reportJdomDocument, writer);
    writer.close();
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
      try {
        simConfig.addContent(makeEntityTable(assemblyFile));
      }
      catch (Exception e) {
        System.err.println("Error reading assembly file: "+e.getMessage());
      }

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
    makeComments(entityParameters,"EP", "");
    makeConclusions(entityParameters,"EP", "");
    if(assemblyFile != null)
      entityParameters.addContent(makeParameterTables());

    rootElement.addContent(entityParameters);
  }

  /** Creates the behavior parameters portion of the report */
  private void createBehaviorDefinitions() {
    behaviorDefinitions = new Element("BehaviorDefinitions");
    behaviorDefinitions.setAttribute("comments", "true");
    behaviorDefinitions.setAttribute("descriptions", "true");
    behaviorDefinitions.setAttribute("image", "true");
    behaviorDefinitions.setAttribute("details", "true");
    makeComments(behaviorDefinitions,"BC", "");
    makeConclusions(behaviorDefinitions,"BC", "");

    try {
      behaviorDefinitions.addContent(processBehaviors(true, true, true));
    }
    catch (Exception e) {
      System.err.println("Error processing assembly file: " + e.getMessage());
    }

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
  public List  unMakeReplicationList(Element statisticalResults)
  {
    Vector v = new Vector();

    Element repReports = statisticalResults.getChild("ReplicationReports");
    List simEnts = repReports.getChildren("SimEntity");
    for (Iterator itr = simEnts.iterator(); itr.hasNext();) {
      Vector se = new Vector(3);
      Element sEnt = (Element)itr.next();
      se.add(sEnt.getAttributeValue("name"));
      se.add(sEnt.getAttributeValue("property"));
      Vector r = new Vector();
      List repLis = sEnt.getChildren("Replication");
      for(Iterator i2 = repLis.iterator(); i2.hasNext();) {
        Element rep = (Element)i2.next();
        String[] sa = new String[7];
        sa[0] = rep.getAttributeValue("number");
        sa[1] = rep.getAttributeValue("count");
        sa[2] = rep.getAttributeValue("minObs");
        sa[3] = rep.getAttributeValue("maxObs");
        sa[4] = rep.getAttributeValue("mean");
        sa[5] = rep.getAttributeValue("stdDeviation");
        sa[6] = rep.getAttributeValue("variance");
        r.add(sa);
      }
      se.add(r);
      v.add(se);
    }

    return v;
  }

  public List unMakeStatsSummList(Element statisticalResults)
  {
    Vector v = new Vector();

    Element sumReports = statisticalResults.getChild("SummaryReport");
    List recs = sumReports.getChildren("SummaryRecord");
    for (Iterator itr = recs.iterator(); itr.hasNext();) {
      Element rec = (Element) itr.next();
      String[] sa = new String[8];
      sa[0] = rec.getAttributeValue("entity");
      sa[1] = rec.getAttributeValue("property");
      sa[2] = rec.getAttributeValue("count");
      sa[3] = rec.getAttributeValue("minObs");
      sa[4] = rec.getAttributeValue("maxObs");
      sa[5] = rec.getAttributeValue("mean");
      sa[6] = rec.getAttributeValue("stdDeviation");
      sa[7] = rec.getAttributeValue("variance");

      v.add(sa);
    }
    return v;
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

  /** Creates Behavior definition references in the analyst report template */
  private Element processBehaviors(boolean descript, boolean image, boolean details) throws Exception {
    Element behaviorList = new Element("BehaviorList");
    if (eventGraphNames != null) {
      for (int i = 0; i < eventGraphNames.size(); i++) {
        Element behavior = new Element("Behavior");
        Element rootElement = null;
        String descriptText = "";
        behavior.setAttribute("name", (String) eventGraphNames.get(i));

        if (descript) {
          Document tmp = loadXML((String) eventGraphFiles.get(i));
          rootElement = tmp.getRootElement();
          descriptText = rootElement.getChild("Comment").getText();

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
    if (listEl != null) {
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
      if (category.equals("diskit.SMAL.Classification"))
        table.addContent(makeTableEntry("Classification", temp));
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
  public Element makeEntityTable(String fileDirectory) throws Exception
  {
    setAssemblyDocument(loadXML(fileDirectory));

    Element entityTable = new Element("EntityTable");
    Element rootElement = assemblyDocument.getRootElement();
    List simEntityList = rootElement.getChildren("SimEntity");
    Iterator itr = simEntityList.iterator();

    //Extract XML based simEntities for entityParameters and event graph image

    setEventGraphFiles(new LinkedList());
    setEventGraphImages(new LinkedList());
    setEventGraphNames(new LinkedList());

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

    //variables for JFreeChart construction
    ChartDrawer chart = new ChartDrawer();
    String chartTitle = "";
    String axisLabel  = "";

    while (mainItr.hasNext()) {
      Element tempEntity = (Element) mainItr.next();
      Iterator itr = tempEntity.getChildren("DataPoint").iterator();
      while (itr.hasNext()) {
        Element temp = (Element) itr.next();
        Element entity = new Element("SimEntity");
        entity.setAttribute("name", tempEntity.getAttributeValue("name"));
        entity.setAttribute("property", temp.getAttributeValue("property"));
        Iterator itr2 = temp.getChildren("ReplicationReport").iterator();

        //Chart title and label
        chartTitle = ( // "Entity Name: " +   // TODO: fix missing @name in AnalystReports/statistics/someAssembly.xml/<ReportStatistics>/<SimEntity>
                tempEntity.getAttributeValue("name"));
        axisLabel  = (temp.getAttributeValue("property")) ;

        while (itr2.hasNext()) {
          Element temp3 = (Element) itr2.next();
          Iterator itr3 = temp3.getChildren("Replication").iterator();

         //Create a data set instance and chart for each replication report
          double[] data = new double[temp3.getChildren().size()];
          int idx = 0;
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

            //Add the mean of this replication to the chart
            data[idx] = Double.parseDouble(temp2.getAttributeValue("mean"));
            idx++;

          }
          Element chartDir = new Element("chartURL");
          String filename = axisLabel;
          chartDir.setAttribute("dir",chart.createHistogram(chartTitle, axisLabel, data, filename));
          entity.addContent(chartDir);
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
  private void saveEventGraphReferences(String fileType) {
    char letter;
    int idx = 0;
    for (int i = 0; i < fileType.length(); i++) {
      letter = fileType.charAt(i);
      if (letter == '.') idx = i;
    }
    String dir = System.getProperty("user.dir") + "/" + fileType.substring(0, idx) + "/";
    dir = dir.replaceAll("\\\\", "/");
    String file = fileType.substring(idx + 1, fileType.length());
    String eventGraphFile = (dir + file + ".xml");
    String img = System.getProperty("user.dir") + "/images/" + fileType.substring(0, idx) + "/" + file + ".xml.png";
    img = img.replaceAll("\\\\", "/");
    
    // Get the absolute path to resolve the broken url problem
    
    if (!eventGraphFiles.contains(eventGraphFile)) {
      eventGraphFiles.add(eventGraphFile);
      eventGraphImages.add(img);
      eventGraphNames.add(fileType.substring(idx + 1, fileType.length()));
    }
  }

  /**
   * Loads an XML document file for processing
   *
   * @param fileDir the location of the file
   * @return doc the document object of the loaded XML
   */
  private Document loadXML(String fileDir) throws Exception
  {
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(fileDir));
    return doc;
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
   * @param imageID a unique identifier for this XML Element
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
    return _unMakeContent(e,imageID+"Image","dir");
  }
  /**
   * Creates a standard 'Comments' element used by all sections of the report
   * to add comments
   *
   * @param commentTag  the tag used to identify unique Comments (used by XSLT)
   * @param commentText the text comments
   */
  public void makeComments(Element parent, String commentTag, String commentText)
  {
    replaceChild(parent, _makeContent(commentTag,"Comments",commentText));
  }

  public Element xmakeComments(String commentTag,String commentText)
  {
    return _makeContent(commentTag,"Comments",commentText);
  }

  private String unMakeComments(Element e)
  {
    return _unMakeContent(e,"Comments");
  }

  private void replaceChild(Element parent, Element child)
  {
    parent.removeChildren(child.getName());
    parent.addContent(child);
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
    replaceChild(parent,_makeContent(commentTag,"Conclusions",conclusionText));
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
    return _unMakeContent(e,suffix,"text");
  }

  private String _unMakeContent(Element e, String suffix, String attrName)
  {
    List content = e.getContent();
    for(Iterator itr=content.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(!(o instanceof Element))
        continue;
      Element celem = (Element)o;
      if(celem.getName().endsWith(suffix))
        return celem.getAttributeValue(attrName);
    }
    return "";
  }

  /**
   * TODO: Change this to put in appropriate sample text
   */
  private void setDefaultValues()
  {
    //Header values
    setReportName("***ENTER REPORT TITLE HERE***");
    setClassification("***ENTER CLASSIFICATION HERE***");
    setAuthor("***ENTER THE NAME OF THE AUTHOR HERE***");
    setDateOfReport(DateFormat.getInstance().format(new Date()));

    //Executive Summary values
    setExecutiveSummaryComments(true);
    setExecutiveSummary("***ENTER EXECUTIVE SUMMARY HERE***");

    //SimulationLocation Values
    setPrintSimLocationComments(true);
    setPrintSimLocationImage(true);
    setSimLocationDescription("***ENTER SIMULATION LOCATION DESCRIPTION HERE***");
    setSimLocationConclusions("***ENTER SIMULATION LOCATION CONCLUSIONS HERE***");
    //setLocationImage(""); // TODO:  generate image, set file location
    //setChartImage(""); // TODO:  generate image, set file location

    //Simulation Configuration Values
    setPrintSimConfigComments(true);
    setPrintAssemblyImage(true);
    setPrintEntityTable(true);
    setSimConfigurationDescription("***ENTER ASSEMBLY CONFIGURATION DESCRIPTION HERE***");
    setSimConfigurationConclusions("***ENTER ASSEMBLY CONFIGURATION CONCLUSIONS HERE***");

    //setAssemblyImageLocation(""); // TODO:  generate image, set file location

    //Entity Parameters values
    setPrintParameterComments(true);
    setPrintParameterTable(true);
    setParameterDescription("***ENTER ENTITY PARAMETER DESCRIPTION HERE***");
    setParameterConclusions("***ENTER ENTITY PARAMETER CONCLUSIONS HERE***");
    

    //BehaviorParameter values
    setPrintBehaviorDefComments(true);
    setPrintEventGraphImages(true);
    setPrintBehaviorDescriptions(true);
    setPrintEventGraphDetails(true);
    setBehaviorDescription("***ENTER ENTITY BEHAVIOR DESCRIPTION HERE***");
    setBehaviorConclusions("***ENTER ENTITY BEHAVIOR CONCLUSIONS HERE***");

    //StatisticalResults values
    setPrintStatsComments(true);
    setPrintReplicationStats(true);
    setPrintSummaryStats(true);
    setStatsDescription("***ENTER STATISTICAL RESULTS DESCRIPTION HERE***");
    setStatsConclusions("***ENTER STATISTICAL RESULTS CONCLUSIONS HERE***");

    //Recommendations/Conclusions
    setPrintRecommendationsConclusions(true);
    setConclusions    ("***ENTER ANALYST CONCLUSIONS HERE***");
    setRecommendations("***ENTER RECOMMENDATIONS FOR FUTURE WORK HERE***");
  }


  public boolean isDebug()                           { return debug; }

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

  public void setAssemblyDocument  (Document assemblyDocument) { this.assemblyDocument = assemblyDocument; }
  public void setAssemblyFile      (String assyFile)
  {
    assemblyFile = assyFile;
    try {
      simConfig.addContent(makeEntityTable(assyFile));
    }
    catch (Exception e) {
      System.err.println("Error processing assemblyFile");
    }
    entityParameters.addContent(makeParameterTables());
    createBehaviorDefinitions();
  }
  public void setFileName          (String fileName)           { this.fileName = fileName; }
  //public void setReportJdomDocument(Document doc)              { this.reportJdomDocument = doc; }
  //public void setRootElement       (Element el)                { this.rootElement = el; }
  public void setStatsReport       (Document statsReport)      { this.statsReport = statsReport; }
  public void setStatsReportPath   (String filename)           { this.statsReportPath = filename; }
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

  public String  getSimLocationComments()          { return unMakeComments(simulationLocation);}
  public String  getSimLocationConclusions()       { return unMakeConclusions(simulationLocation);}
  public String  getLocationImage()           { return unMakeImage(simulationLocation,"Location");}
  public String  getChartImage()              { return unMakeImage(simulationLocation,"Chart"); }

  public void setSimLocationDescription          (String s)    { makeComments(simulationLocation,"SL", s);}
  public void setSimLocationConclusions       (String s)    { makeConclusions(simulationLocation,"SL", s);}
  public void setLocationImage           (String s)    { replaceChild(simulationLocation,makeImage("Location", s)); }
  public void setChartImage              (String s)    { replaceChild(simulationLocation,makeImage("Chart", s)); }

  // entity-parameters
  //good
  public boolean isPrintParameterComments() { return stringToBoolean(entityParameters.getAttributeValue("comments"));}
  public boolean isPrintParameterTable()    { return stringToBoolean(entityParameters.getAttributeValue("parameterTables")); }
  public void setPrintParameterComments   (boolean bool) { entityParameters.setAttribute("comments", booleanToString(bool)); }
  public void setPrintParameterTable      (boolean bool) { entityParameters.setAttribute("parameterTables", booleanToString(bool)); }

  public String  getParameterComments()    { return unMakeComments(entityParameters);}
  public String  getParameterConclusions() { return unMakeConclusions(entityParameters);}
  public Vector  getParameterTables()      { return unMakeParameterTables(entityParameters);}
  public void setParameterDescription         (String s){ makeComments(entityParameters,"PC", s); }
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
  public void setBehaviorDescription         (String s) { makeComments(behaviorDefinitions,"BC", s); }
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
  public void    setSimConfigurationDescription       (String s) { makeComments(simConfig,"SC", s); }
  public void    setSimConfigEntityTable    (String s) { }; //todo
  public void    setSimConfigurationConclusions    (String s) { makeConclusions(simConfig,"SC", s); }
  public void    setAssemblyImageLocation   (String s) { replaceChild(simConfig,makeImage("Assembly", s)); }

  // stat results:
  // good
  public boolean isPrintReplicationStats() { return stringToBoolean(statisticalResults.getAttributeValue("replicationStats")); }
  public boolean isPrintStatsComments()    { return stringToBoolean(statisticalResults.getAttributeValue("comments")); }
  public boolean isPrintSummaryStats()     { return stringToBoolean(statisticalResults.getAttributeValue("summaryStats")); }
  public boolean isPrintStatsCharts()      { return stringToBoolean(statisticalResults.getAttributeValue("charts")); }
  //todo later public boolean isOverlayStatsCharts()    { return stringToBoolean(statisticalResults.getAttributeValue("overlay")); }
  public void setPrintReplicationStats   (boolean bool) { statisticalResults.setAttribute("replicationStats", booleanToString(bool)); }
  public void setPrintStatsComments      (boolean bool) { statisticalResults.setAttribute("comments", booleanToString(bool)); }
  public void setPrintSummaryStats       (boolean bool) { statisticalResults.setAttribute("summaryStats", booleanToString(bool)); }
  public void setPrintStatsCharts        (boolean bool) { statisticalResults.setAttribute("charts", booleanToString(bool)); }
  //todo later public void setOverlayStatsCharts      (boolean bool) { statisticalResults.setAttribute("overlay", booleanToString(bool)); }

  public String  getStatsComments()        { return unMakeComments(statisticalResults);}
  public String  getStatsConclusions()     { return unMakeConclusions(statisticalResults);}
  public void setStatsDescription           (String s) { makeComments(statisticalResults,"SR", s); }
  public void setStatsConclusions        (String s) { makeConclusions(statisticalResults,"SR", s); }
  public String getStatsFilePath()         { return statisticalResults.getAttributeValue("file"); }
  public List   getStatsReplicationsList() { return unMakeReplicationList(statisticalResults);}
  public List   getStastSummaryList()      { return unMakeStatsSummList(statisticalResults); }

  // misc:
  public void setEventGraphFiles(LinkedList eventGraphFiles) { this.eventGraphFiles = eventGraphFiles; }
  public void setEventGraphImages(LinkedList eventGraphImages) { this.eventGraphImages = eventGraphImages; }
  public void setEventGraphNames(LinkedList eventGraphNames) { this.eventGraphNames = eventGraphNames; }
}

