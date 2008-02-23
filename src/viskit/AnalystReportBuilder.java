/*
 * AnalystReportBuilder.java
 *
 * Created on July 18, 2006, 7:04 PM
 */
package viskit;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.filter.ElementFilter;
import viskit.xsd.assembly.ChartDrawer;

/** This class constructs and exports an analyst report based on the parameters
 * selected by the Analyst Report panel in the Viskit UI.  This file uses the
 * assembly file, and event graph files as well as customizable items (images,
 * comments) to construct a report that is saved in XML and html formats.
 *
 * @author Patrick Sullivan
 * @version $Id: AnalystReportBuilder.java 1670 2007-12-20 00:23:36Z tdnorbra $
 */
public class AnalystReportBuilder {
    
    static Logger log = Logger.getLogger(AnalystReportBuilder.class);
    private boolean debug = false;
    
    /** UTH - Assembly file */
    private String assemblyFile;
    
    /** The viskit.xsd.assembly.ReportStatisticsDOM object for this report */
    private Document statsReport;
    private String   statsReportPath;
        
    /** The jdom.Document object that is used to build the report */
    private Document reportJdomDocument;
    
    /** The file name selected by the user from "SAVE AS" menu option */
    private String fileName;
    
    /** The root element of the report xml document */
    private Element rootElement;
    private Element execSummary;
    private Element simulationLocation;
    private Element simConfig;
    private Element entityParameters;
    private Element behaviorDescriptions;
    private Element statisticalResults;
    private Element concRec;

    /** Reference to the Analyst Report JPanel for JProgressBar */
    private JPanel aRPanel;
    
    /** Build a default AnalystReport object */
    public AnalystReportBuilder() {
        initDocument();
        setDefaultValues();
    }
    
    /** Build an AnalystReport object from an existing statisticsReport 
     * document.  This is done from viskit.BasicAssembly.</p>
     * @param statisticsReportPath the path to the statistics generated report
     *        used by this Analyst Report 
     */
    public AnalystReportBuilder(String statisticsReportPath) {
        try {
            Document doc = EventGraphCache.loadXML(statisticsReportPath);
            setStatsReportPath(statisticsReportPath);
            setStatsReport(doc);
        } catch (Exception e) {
            log.error("Exception reading "+statisticsReportPath + " : "+e.getMessage());
        }
        initDocument();
        setDefaultValues();
    }

    /**
     * Build an analystReport object from an existing partial Analyst Report.  
     * This done after the statistic report is incorporated into the basic 
     * Analyst Report and further annotations are to be written by the analyst
     * to finalize the report.</p>
     * @param aRPanel a reference to the Analyst Report JPanel
     * @param xmlFile an existing temp Analyst Report
     * @param assyFile the current assembly file to process a report from
     * @throws java.lang.Exception 
     */
    public AnalystReportBuilder(JPanel aRPanel, File xmlFile, String assyFile) throws Exception {
        this.aRPanel = aRPanel;
        parseXML(xmlFile);
        log.debug("Successful parseXML");
        if (assyFile != null) {
            setAssemblyFile(assyFile);
            log.debug("Successful setting of assembly file");
            postProcessing();
            log.debug("Successful post processing of Analyst Report");
        }
    }
    
    /** This constructor for opening a fully annotated report for further 
     * annotations, or as required from the analyst/user.
     * @param fullReport an exisiting fully annotated report to reopen
     */
    public AnalystReportBuilder(File fullReport) {
        try {
            parseXML(fullReport);
        } catch (Exception ex) {
            log.error(ex);
        }
    }
    
    private void initDocument() {
        reportJdomDocument = new Document();
        rootElement = new Element("AnalystReport");
        reportJdomDocument.setRootElement(rootElement);
        
        fillDocument();
    }
    
    private void fillDocument() {
        createHeader();
        createExecutiveSummary();
        createSimulationLocation();
        createSimulationConfiguration();
        createEntityParameters();
        createBehaviorDescriptions();
        createStatisticalResults();
        createConclusionsRecommendations();
    }
    
    /**
     * File I/O that saves the report in XML format
     * @param fil the initial temp file to save for further post-processing
     * @return the initial temp file to saveed for further post-processing
     * @throws java.lang.Exception 
     */
    public File writeToXMLFile(File fil) throws Exception {
        if (fil == null) {return writeToXMLFile();}
        
        _writeCommon(fil);
        return fil;
    }
    
    /** @return the initial temp file to saved for further post-processing
     * @throws java.lang.Exception 
     */
    public File writeToXMLFile() throws Exception {
        File fil = File.createTempFile("AnalystReport", ".xml");
        _writeCommon(fil);
        return fil;
    }
    
    private void _writeCommon(File fil) throws Exception {
        XMLOutputter outputter = new XMLOutputter();
        Format form = Format.getPrettyFormat();
        outputter.setFormat(form);
        
        FileWriter writer = new FileWriter(fil);
        outputter.output(reportJdomDocument, writer);
        writer.close();
    }
    
    private void parseXML(File fil) throws Exception {
        reportJdomDocument = EventGraphCache.loadXML(fil.getPath());
        rootElement = reportJdomDocument.getRootElement();
        execSummary = rootElement.getChild("ExecutiveSummary");
        simulationLocation = rootElement.getChild("Location");
        simConfig = rootElement.getChild("SimulationConfiguration");
        entityParameters = rootElement.getChild("EntityParameters");
        behaviorDescriptions = rootElement.getChild("BehaviorDescriptions");
        statisticalResults = rootElement.getChild("StatisticalResults");
        concRec = rootElement.getChild("ConclusionsRecommendations");
    }
    
    /**
     * Creates the root element for the analyst report
     */
    public void createHeader() {
        rootElement.setAttribute("name", "");
        rootElement.setAttribute("classification", "");
        rootElement.setAttribute("author", "");
        rootElement.setAttribute("date", "");
    }
    
    /**
     * Populates the executive summary portion of the AnalystReport XML
     */
    public void createExecutiveSummary() {
        execSummary = new Element("ExecutiveSummary");
        execSummary.setAttribute("comments", "true");
        // execSummary.addContent(makeComments("ES", ""));
        rootElement.addContent(execSummary);
    }
    
    /** Creates the SimulationLocation portion of the analyst report XML */
    public void createSimulationLocation() {
        simulationLocation = new Element("Location");
        simulationLocation.setAttribute("comments", "true");
        simulationLocation.setAttribute("images", "true");
        makeComments(simulationLocation, "SL", "");        
        makeProductionNotes(simulationLocation, "SL", "");
        makeConclusions(simulationLocation, "SL", "");
        rootElement.addContent(simulationLocation);
    }
    
    /** Creates the simulation configuration portion of the Analyst report XML */
    private void createSimulationConfiguration() {
        simConfig = new Element("SimulationConfiguration");
        simConfig.setAttribute("comments", "true");
        simConfig.setAttribute("image", "true");
        simConfig.setAttribute("entityTable", "true");
        makeComments(simConfig, "SC", "");
        makeProductionNotes(simConfig, "SC", "");
        makeConclusions(simConfig, "SC", "");
        if(assemblyFile != null) {
            try {
                simConfig.addContent(EventGraphCache.makeEntityTable(assemblyFile));
            } catch (Exception e) {
                log.error("Error reading assembly file: " + e.getMessage());
            }
        }
        
        rootElement.addContent(simConfig);
    }
    
    /** Creates the entity parameter section of this analyst report */
    private void createEntityParameters() {
        entityParameters = new Element("EntityParameters");
        entityParameters.setAttribute("comments", "true");
        entityParameters.setAttribute("parameterTables", "true");
        makeComments(entityParameters, "EP", "");
        makeConclusions(entityParameters, "EP", "");
        if(assemblyFile != null) {
            entityParameters.addContent(makeParameterTables());
        }
        
        rootElement.addContent(entityParameters);
    }
    
    /** Creates the behavior descriptions portion of the report */
    private void createBehaviorDescriptions() {
        behaviorDescriptions = new Element("BehaviorDescriptions");
        behaviorDescriptions.setAttribute("comments", "true");
        behaviorDescriptions.setAttribute("descriptions", "true");
        behaviorDescriptions.setAttribute("image", "true");
        behaviorDescriptions.setAttribute("details", "true");
        makeComments(behaviorDescriptions,"BD", "");
        makeConclusions(behaviorDescriptions,"BD", "");
        
        behaviorDescriptions.addContent(processBehaviors(true, true, true));       
        
        rootElement.removeChild("BehaviorDescriptions");
        rootElement.addContent(behaviorDescriptions);
    }
    
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private void createStatisticalResults() {
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
            List<Element> itr = (List<Element>) statsReport.getRootElement().getChildren("SimEntity");
            for (Element entity : itr) {
                Element temp = (Element) entity.clone();
                temp.removeChildren("SummaryReport");
                
                Element summStats = entity.getChild("SummaryReport");
                List<Element> summItr = (List<Element>) summStats.getChildren("Summary");
                for (Element temp2 : summItr) {
                    Element summaryRecord = new Element("SummaryRecord");
                    summaryRecord.setAttribute("entity", entity.getAttributeValue("name"));
                    summaryRecord.setAttribute("property", temp2.getAttributeValue("property"));
                    summaryRecord.setAttribute("numRuns", temp2.getAttributeValue("numRuns"));
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
    
    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    public List<Object> unMakeReplicationList(Element statisticalResults) {
        Vector<Object> v = new Vector<Object>();
        
        Element repReports = statisticalResults.getChild("ReplicationReports");
        List<Element> simEnts = (List<Element>) repReports.getChildren("SimEntity");
        for (Element sEnt : simEnts) {
            
            Vector<Object> se = new Vector<Object>(3);
            se.add(sEnt.getAttributeValue("name"));
            se.add(sEnt.getAttributeValue("property"));
            
            Vector<String[]> r = new Vector<String[]>();
            List<Element> repLis = (List<Element>) sEnt.getChildren("Replication");
            for(Element rep : repLis) {
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
    
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    public List<String[]> unMakeStatsSummList(Element statisticalResults) {
        Vector<String[]> v = new Vector<String[]>();
        
        Element sumReports = statisticalResults.getChild("SummaryReport");
        List<Element> recs = (List<Element>) sumReports.getChildren("SummaryRecord");
        for (Element rec : recs) {
            String[] sa = new String[8];
            sa[0] = rec.getAttributeValue("entity");
            sa[1] = rec.getAttributeValue("property");
            sa[2] = rec.getAttributeValue("numRuns");
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
    private void createConclusionsRecommendations() {
        concRec = new Element("ConclusionsRecommendations");
        concRec.setAttribute("comments", "true");
        makeComments(concRec,"CR", "");
        makeConclusions(concRec,"CR", "");
        rootElement.addContent(concRec);
    }
    
    /** Creates Behavior definition references in the analyst report template
     * @param descript if true, show description text
     * @param image if true, show all images
     * @param details if true, show all details text
     * @return a table of scenario Event Graph Behaviors
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element processBehaviors(boolean descript, boolean image, boolean details) /*throws Exception*/ {
        Element behaviorList = new Element("BehaviorList");
        if (EventGraphCache.getEventGraphNames() != null) {
            for (int i = 0; i < EventGraphCache.getEventGraphNames().size(); i++) {
                Element behavior = new Element("Behavior");
                Element localRootElement = null;
                String descriptText = "";
                behavior.setAttribute("name", EventGraphCache.getEventGraphNames().get(i));

                if (descript) {
                    Document tmp = EventGraphCache.loadXML(EventGraphCache.getEventGraphFiles().get(i));
                    localRootElement = tmp.getRootElement();

                    // prevent returning a null if there was no attribute value
                    descriptText = (localRootElement.getChildText("Comment") == null) ? "no comment provided" : localRootElement.getChildText("Comment");

                    Element description = new Element("description");
                    description.setAttribute("text", descriptText);
                    behavior.addContent(description);

                    if (details) {
                        List<Element> lre = (List<Element>) localRootElement.getChildren("Parameter");
                        for (Element temp : lre) {
                            Element param = new Element("parameter");
                            param.setAttribute("name", temp.getAttributeValue("name"));
                            param.setAttribute("type", temp.getAttributeValue("type"));

                            // The data "null" is not legal for a JDOM attribute
                            param.setAttribute("description", (temp.getChildText("Comment") == null) ? "no comment provided" : temp.getChildText("Comment"));
                            behavior.addContent(param);
                        }
                        List<Element> lre2 = (List<Element>) localRootElement.getChildren("StateVariable");
                        for (Element temp : lre2) {
                            Element stvar = new Element("stateVariable");
                            stvar.setAttribute("name", temp.getAttributeValue("name"));
                            stvar.setAttribute("type", temp.getAttributeValue("type"));

                            // The data "null" is not legal for a JDOM attribute
                            stvar.setAttribute("description", (temp.getChildText("Comment") == null) ? "no comment provided" : temp.getChildText("Comment"));
                            behavior.addContent(stvar);
                        }
                    }
                }
                if (image) {
                    Element evtGraphImage = new Element("EventGraphImage");
                    evtGraphImage.setAttribute("dir", EventGraphCache.getEventGraphImagePaths().get(i));
                    behavior.addContent(evtGraphImage);
                }
                behaviorList.addContent(behavior);
            }
        }
        return behaviorList;
    }

    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    List unMakeBehaviorList(Element localRoot) {
        Vector v = new Vector();
        
        Element listEl = localRoot.getChild("BehaviorList");
        if (listEl != null) {
            List<Element> behElms = (List<Element>) listEl.getChildren("Behavior");
            for (Element behavior : behElms) {
                
                Vector<Object> b = new Vector<Object>();
                String nm = behavior.getAttributeValue("name");
                b.add(nm);
                
                Element desc = behavior.getChild("description");
                String desctxt = desc.getAttributeValue("text");
                b.add(desctxt);
                
                List<Element> parms = (List<Element>) behavior.getChildren("parameter");
                
                Vector<String[]> p = new Vector<String[]>();
                for (Element param : parms) {
                    String pnm = param.getAttributeValue("name");
                    String pty = param.getAttributeValue("type");
                    String pdsc = param.getAttributeValue("description");
                    String[] pa = new String[]{pnm, pty, pdsc};
                    p.add(pa);
                }
                b.add(p);
                
                List<Element> stvars = (List<Element>) behavior.getChildren("stateVariable");
                
                Vector<String[]> s = new Vector<String[]>();
                for (Element svar : stvars) {
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
    
    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    Vector<Object[]> unMakeParameterTables(Element rootOfTabs) {
        Element elm = rootOfTabs.getChild("ParameterTables");
        List<Element> lis = elm.getChildren("EntityParameterTable");
        Vector<Object[]> v = new Vector<Object[]>(lis.size());   // list of entpartab elms
        
        for(Element e_0 : lis) {
            List<Element> lis_0 = e_0.getChildren();       //list of parts: class/id/phys/dynam
            Vector<Object[]> v_0 = new Vector<Object[]>(lis_0.size());
            for(Element e_1 : lis_0) {
                List<Element> lis_1 = e_1.getChildren("parameter");     // list of param elms
                
                Vector<String[]> v_1 = new Vector<String[]>(lis_1.size());
                for(Element e_2 : lis_1) {
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
     * @return a Parameter Table for a given event graph
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element makeParameterTables() {
        Element parameterTables = new Element("ParameterTables");
        Element localRootElement = EventGraphCache.getAssemblyDocument().getRootElement();
        List<Element> simEntityList = (List<Element>) localRootElement.getChildren("SimEntity");
        String entityName;
        for (Element temp : simEntityList) {
            entityName = temp.getAttributeValue("name");
            List<Element> entityParams = (List<Element>) temp.getChildren("MultiParameter");
            for (Element param : entityParams) {
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
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element extractSMAL(String entityName, Element entityDef) {
        Element table = new Element("EntityParameterTable");
        ElementFilter multiParam = new ElementFilter("MultiParameter");
        Iterator<Element> itr = entityDef.getDescendants(multiParam);
        table.setAttribute("name", entityName);
        while (itr.hasNext()) {
            Element temp = itr.next();
            String category = temp.getAttributeValue("type");
            if (category.equals("diskit.SMAL.Classification")) {
                table.addContent(makeTableEntry("Classification", temp));
            }
            if (category.equals("diskit.SMAL.IdentificationParameters")) {
                table.addContent(makeTableEntry("Identification", temp));
            }
            if (category.equals("diskit.SMAL.PhysicalConstraints")) {
                table.addContent(makeTableEntry("PhysicalConstraints", temp));
            }
            if (category.equals("diskit.SMAL.DynamicResponseConstraints")) {
                table.addContent(makeTableEntry("DynamicResponseConstraints", temp));
            }
            if (category.equals("diskit.SMAL.TacticalConstraints")) {
                table.addContent(makeTableEntry("TacticalConstraints", temp));
            }
        }        
        return table;    
    }
    
    /**
     * Processes parameters
     *
     * @param category the category for this table entry
     * @param data     the element that corresponds to the category
     * @return the parameter in table format
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element makeTableEntry(String category, Element data) {
        Element tableEntry = new Element(category);        
        List<Element> dataList = (List<Element>) data.getChildren("TerminalParameter");
        
        for (Element temp : dataList) {
            if (!temp.getAttributeValue("value").equals("0")) {
                Element param = new Element("parameter");
                param.setAttribute("name", temp.getAttributeValue("name"));
                param.setAttribute("value", temp.getAttributeValue("value"));
                
                tableEntry.addContent(param);
            }
        }
        return tableEntry;
    }
    
    // TODO: Version 1.1 JDOM does not yet support generics
    @SuppressWarnings("unchecked")
    public String[][] unMakeEntityTable() {
        Element elm = simConfig.getChild("EntityTable");
        List<Element> lis = (List<Element>) elm.getChildren("SimEntity");
        
        String[][] sa = new String[lis.size()][2];
        int i = 0;
        for(Element e : lis) {
            sa[i]  [0] = e.getAttributeValue("name");
            sa[i++][1] = e.getAttributeValue("fullyQualifiedName");
        }
        return sa;
    }

    /**
     * This method re-shuffles the statistics report to a format that is handled
     * by the xslt for the analyst report.  The mis-match of formatting was discovered
     * after all classes were written. This should be cleaned up or the XML formatted
     * more uniformly.
     * @return the replication report
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element makeReplicationReport() {
        Element repReports = new Element("ReplicationReports");
        List<Element> mainItr = (List<Element>) statsReport.getRootElement().getChildren("SimEntity");
        
        // variables for JFreeChart construction
        ChartDrawer chart = new ChartDrawer();
        String chartTitle = "";
        String axisLabel  = "";
        
        for (Element tempEntity : mainItr) {
            Iterator itr = tempEntity.getChildren("DataPoint").iterator();
            while (itr.hasNext()) {
                Element temp = (Element) itr.next();
                Element entity = new Element("SimEntity");
                entity.setAttribute("name", tempEntity.getAttributeValue("name"));
                entity.setAttribute("property", temp.getAttributeValue("property"));
                Iterator itr2 = temp.getChildren("ReplicationReport").iterator();
                
                // Chart title and label
                chartTitle = (tempEntity.getAttributeValue("name"));
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
                    chartDir.setAttribute("dir", chart.createHistogram(chartTitle, axisLabel, data, filename, idx));
                    entity.addContent(chartDir);
                    repReports.addContent(entity);
                }
            }
        }
        return repReports;
    }
   
    /**
     * Converts boolean input into a 'true'/'false' string representation for use as
     * an attribute value in the Analyst report XML.
     *
     * @param booleanFlag the boolean variable to convert
     * @return the string representation of the boolean variable
     */
    private String booleanToString(boolean booleanFlag) {return booleanFlag ? "true" : "false";}
    
    private boolean stringToBoolean(String s) {return s.equalsIgnoreCase("true");}
    
    /**
     * Creates a standard 'Image' element used by all sections of the report
     *
     * @param imageID a unique identifier for this XML Element
     * @param dir     the directory of the image
     * @return the Image url embedded in well formed XML
     */
    private Element makeImage(String imageID, String dir) {
        Element image = new Element(imageID + "Image");
        image.setAttribute("dir", dir);
        return image;
    }
    
    private String unMakeImage(Element e, String imageID) {
        return _unMakeContent(e, imageID + "Image", "dir");
    }
    
    /**
     * Creates a standard 'Comments' element used by all sections of the report
     * to add comments
     *
     * @param parent 
     * @param commentTag  the tag used to identify unique Comments (used by XSLT)
     * @param commentText the text comments
     */
    public void makeComments(Element parent, String commentTag, String commentText) {
        replaceChild(parent, _makeContent(commentTag, "Comments", commentText));
    }
    
    /**
     * 
     * @param commentTag
     * @param commentText
     * @return
     */
    public Element xmakeComments(String commentTag, String commentText) {
        return _makeContent(commentTag, "Comments", commentText);
    }
    
    private String unMakeComments(Element e) {
        return _unMakeContent(e, "Comments");
    }
        
    /**
     * Creates a standard 'Conclusions' element used by all sections of the report
     * to add conclusions
     *
     * @param commentTag     the tag used to identify unique Comments (used by XSLT)
     * @param conclusionText the text comments
     * @return conclusions the Comments embedded in well formed XML
     */
    public Element xmakeConclusions(String commentTag, String conclusionText) {
        return _makeContent(commentTag,"Conclusions",conclusionText);
    }
    
    public void makeConclusions(Element parent, String commentTag, String conclusionText) {
        replaceChild(parent,_makeContent(commentTag,"Conclusions",conclusionText));
    }
    
    /**
     * 
     * @param e
     * @return
     */
    public String unMakeConclusions(Element e) {
        return _unMakeContent(e,"Conclusions");
    }
    
    /**
     * Creates a standard 'Production Notes' element used by all sections of the report
     * to add conclusions
     *
     * @param productionNotesTag the tag used to identify unique Production Notes (used by XSLT)
     * @param productionNotesText
     * @return 
     */
    public Element xmakeProductionNotes(String productionNotesTag, String productionNotesText) {
        return _makeContent(productionNotesTag, "ProductionNotes", productionNotesText);
    }
    
    /**
     * Creates a standard 'Production Notes' element used by all sections of the
     * report to add production notes
     *
     * @param parent the parent element to add content too
     * @param productionNotesTag the tag used to identify unique production notes (used by XSLT)
     * @param productionNotesText 
     */
    public void makeProductionNotes(Element parent, String productionNotesTag, String productionNotesText) {
        replaceChild(parent, _makeContent(productionNotesTag, "ProductionNotes", productionNotesText));
    }
    
    public String unMakeProductionNotes(Element e) {
        return _unMakeContent(e, "ProductionNotes");
    }
    
    private Element _makeContent(String commentTag, String suffix, String commentText) {
        Element comments = new Element((commentTag + suffix));
        comments.setAttribute("text", commentText);
        return comments;
    }
    
    private String _unMakeContent(Element e, String suffix) {
        return _unMakeContent(e,suffix,"text");
    }
    
    private String _unMakeContent(Element e, String suffix, String attrName) {
        if (e == null) {return "";}
        List content = e.getContent();
        for (Iterator itr = content.iterator(); itr.hasNext();) {
            Object o = itr.next();
            if (!(o instanceof Element)) {
                continue;
            }
            Element celem = (Element) o;
            if (celem.getName().endsWith(suffix)) {
                return celem.getAttributeValue(attrName);
            }
        }
        return "";
    }
    
    private void replaceChild(Element parent, Element child) {
        parent.removeChildren(child.getName());
        parent.addContent(child);
    }
    
    /**
     * TODO: Change this to put in appropriate sample text
     */
    private void setDefaultValues() {
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
        setSimLocationProductionNotes("***ENTER SIMULATION PRODUCTION NOTES HERE***");
        //setChartImage(""); // TODO:  generate image, set file location
        
        //Simulation Configuration Values
        setPrintSimConfigComments(true);
        setPrintAssemblyImage(true);
        setPrintEntityTable(true);
        setSimConfigurationDescription("***ENTER ASSEMBLY CONFIGURATION DESCRIPTION HERE***");
        setSimConfigurationConclusions("***ENTER ASSEMBLY CONFIGURATION CONCLUSIONS HERE***");
        setSimConfigationProductionNotes("***ENTER ASSEMBLY CONFIGURATION PRODUCTION NOTES HERE***");
                
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
    
    public boolean isDebug()                     { return debug; }
    
    public Document   getReportJdomDocument()    { return reportJdomDocument; }
    public Document   getStatsReport()           { return statsReport; }
    public Element    getRootElement()           { return rootElement; }
    public String     getFileName()              { return fileName; }    
    public String     getAssemblyFile()          { return assemblyFile; }
    
    public String     getAuthor()                { return rootElement.getAttributeValue("author"); }
    public String     getClassification()        { return rootElement.getAttributeValue("classification");}
    public String     getDateOfReport()          { return rootElement.getAttributeValue("date");}
    public String     getReportName()            { return rootElement.getAttributeValue("name"); }
    
    public void setAssemblyFile(String assyFile) {
        assemblyFile = assyFile;
        simConfig.addContent(EventGraphCache.makeEntityTable(assyFile));
        entityParameters.addContent(makeParameterTables());
        createBehaviorDescriptions();
    } 
    
    /** Post Analyst Report processing steps to take */
    public void postProcessing() {
        JProgressBar jpb = new JProgressBar();
        jpb.setIndeterminate(true);
        jpb.setString("Analyst Report now generating...");
        jpb.setStringPainted(true);

        aRPanel.add(jpb);
        aRPanel.validate();
        
        log.debug("JProgressBar set");

        captureEventGraphImages();
        log.debug("EGs captured");
        captureAssemblyImage();
        log.debug("Assembly captured");
        captureLocationImage();
        log.debug("Location Image captured");
        
        jpb.setIndeterminate(false);
        jpb.setStringPainted(false);
        
        announceReportReadyToView();
    }
    
    /** Utility method used here to invoke the capability to capture all Event 
     * Graph images of which are situated in a particular Assembly File.  These
     * PNGs will be dropped into ./AnalystReports/images/EventGraphs </p>
     */
    private void captureEventGraphImages() {
        VGlobals.instance().getEventGraphEditor().controller.captureEventGraphImages(EventGraphCache.getEventGraphFiles(), EventGraphCache.getEventGraphImagePaths());
    }
    
    /** Utility method used here to invoke the capability to capture the 
     * Assembly image of the loaded Assembly File.  This PNG will be dropped 
     * into ./AnalystReports/images/Assemblies </p>
     */
    private void captureAssemblyImage() {
        String assemblyImageDir = System.getProperty("user.dir") + "/AnalystReports/images/Assemblies/";
        assemblyImageDir = assemblyImageDir.replaceAll("\\\\", "/");
        String assyFileName = new File(getAssemblyFile()).getName();
        setAssemblyImageLocation(assemblyImageDir + assyFileName + ".png");
        VGlobals.instance().getAssemblyController().captureAssemblyImage(getAssemblyImageLocation());
    }
    
    private void announceReportReadyToView() {
        JOptionPane.showMessageDialog(null, "<html><body><p align='center'>" +
                "Analyst Report is loaded and ready for display.</p></body></html>", 
                "Analyst Report Ready", JOptionPane.INFORMATION_MESSAGE);
        // TODO consider inserting loaded filename into message above as a user confirmation
    }
    
    /** If a 2D top town image was generated from SavageStudio, then point to 
     *  this location
     */
    private void captureLocationImage() {
        String assyFile = getAssemblyFile().replaceAll("\\\\", "/");
        String baseName = assyFile.substring(assyFile.lastIndexOf("/"));
        if (baseName.contains("_")) {
            baseName = baseName.substring(0, baseName.lastIndexOf("_"));
        }
        String locationImagePath = System.getProperty("user.dir") + "/AnalystReports/images" + baseName + ".png";
        locationImagePath = locationImagePath.replaceAll("\\\\", "/");
        log.debug(locationImagePath);
        if (new File(locationImagePath).exists()) {
            setLocationImage(locationImagePath);
        }
        log.debug(getLocationImage());
    }
    
    public void setFileName          (String fileName)           { this.fileName = fileName; }
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
    public void setPrintRecommendationsConclusions(boolean bool) { concRec.setAttribute("comments", booleanToString(bool)); }
    public void setConclusions                     (String s)     { makeComments(concRec,"CR", s); }   // watch the wording
    public void setRecommendations                 (String s)     { makeConclusions(concRec,"CR", s); }
    
    // exec summary:
    // good
    public boolean isExecutiveSummaryComments() { return stringToBoolean(execSummary.getAttributeValue("comments"));}
    public void    setExecutiveSummaryComments  (boolean bool) {execSummary.setAttribute("comments", booleanToString(bool));}
    public String  getExecutiveSummary() { return unMakeComments(execSummary);}
    public void    setExecutiveSummary   (String s) { makeComments(execSummary,"ES", s);}
    
    // sim-location:
    // good
    public boolean isPrintSimLocationComments() {return stringToBoolean(simulationLocation.getAttributeValue("comments"));}
    public void    setPrintSimLocationComments  (boolean bool) {simulationLocation.setAttribute("comments", booleanToString(bool));}
    public boolean isPrintSimLocationImage()    {return stringToBoolean(simulationLocation.getAttributeValue("images"));}
    public void    setPrintSimLocationImage     (boolean bool) {simulationLocation.setAttribute("images", booleanToString(bool));}
    
    public String  getSimLocationComments()        {return unMakeComments(simulationLocation);}
    public String  getSimLocationConclusions()     {return unMakeConclusions(simulationLocation);}
    public String  getSimLocationProductionNotes() {return unMakeProductionNotes(simulationLocation);}
    public String  getLocationImage()              {return unMakeImage(simulationLocation, "Location");}
    public String  getChartImage()                 {return unMakeImage(simulationLocation, "Chart");}
    
    public void setSimLocationDescription  (String s)    {makeComments(simulationLocation, "SL", s);}
    public void setSimLocationConclusions  (String s)    {makeConclusions(simulationLocation, "SL", s);}
    public void setSimLocationProductionNotes(String s)  {makeProductionNotes(simulationLocation, "SL", s);}
    public void setLocationImage           (String s)    {replaceChild(simulationLocation, makeImage("Location", s)); }
    public void setChartImage              (String s)    {replaceChild(simulationLocation, makeImage("Chart", s)); }
    
    // entity-parameters
    //good
    public boolean isPrintParameterComments() { return stringToBoolean(entityParameters.getAttributeValue("comments"));}
    public boolean isPrintParameterTable()    { return stringToBoolean(entityParameters.getAttributeValue("parameterTables")); }
    public void setPrintParameterComments   (boolean bool) { entityParameters.setAttribute("comments", booleanToString(bool)); }
    public void setPrintParameterTable      (boolean bool) { entityParameters.setAttribute("parameterTables", booleanToString(bool)); }
    
    public String  getParameterComments()    { return unMakeComments(entityParameters);}
    public String  getParameterConclusions() { return unMakeConclusions(entityParameters);}
    public Vector<Object[]> getParameterTables() {return unMakeParameterTables(entityParameters);}
    public void setParameterDescription         (String s){ makeComments(entityParameters,"EP", s); }
    public void setParameterConclusions      (String s){ makeConclusions(entityParameters,"EP", s); }
    
    // behavior descriptions:
    //good
    public boolean isPrintBehaviorDefComments()  { return stringToBoolean(behaviorDescriptions.getAttributeValue("comments"));}
    public void setPrintBehaviorDefComments(boolean bool) { behaviorDescriptions.setAttribute("comments", booleanToString(bool)); }
    
    public boolean isPrintBehaviorDescriptions() { return stringToBoolean(behaviorDescriptions.getAttributeValue("descriptions"));}
    public boolean isPrintEventGraphDetails()    { return stringToBoolean(behaviorDescriptions.getAttributeValue("details"));}
    public boolean isPrintEventGraphImages()     { return stringToBoolean(behaviorDescriptions.getAttributeValue("image"));}
    public void setPrintBehaviorDescriptions(boolean bool) { behaviorDescriptions.setAttribute("descriptions", booleanToString(bool)); }
    public void setPrintEventGraphDetails    (boolean bool) { behaviorDescriptions.setAttribute("details", booleanToString(bool)); }
    public void setPrintEventGraphImages     (boolean bool) { behaviorDescriptions.setAttribute("image", booleanToString(bool)); }
    
    public String  getBehaviorComments()         { return unMakeComments(behaviorDescriptions); }
    public String  getBehaviorConclusions()      { return unMakeConclusions(behaviorDescriptions); }
    public void setBehaviorDescription         (String s) { makeComments(behaviorDescriptions,"BD", s); }
    public void setBehaviorConclusions      (String s) { makeConclusions(behaviorDescriptions,"BD", s); }
    public List getBehaviorList()          { return unMakeBehaviorList(behaviorDescriptions); }
    // sim-config:
    //good
    public boolean isPrintSimConfigComments() { return stringToBoolean(simConfig.getAttributeValue("comments"));}
    public boolean isPrintEntityTable()       { return stringToBoolean(simConfig.getAttributeValue("entityTable"));}
    public boolean isPrintAssemblyImage()     { return stringToBoolean(simConfig.getAttributeValue("image"));}
    public void    setPrintSimConfigComments  (boolean bool) { simConfig.setAttribute("comments", booleanToString(bool));}
    public void    setPrintEntityTable        (boolean bool) { simConfig.setAttribute("entityTable", booleanToString(bool)); }
    public void    setPrintAssemblyImage      (boolean bool) { simConfig.setAttribute("image", booleanToString(bool)); }
    
    public String  getSimConfigComments()        {return unMakeComments(simConfig);}
    public String[][]  getSimConfigEntityTable() {return unMakeEntityTable();}
    public String  getSimConfigConclusions()     {return unMakeConclusions(simConfig);}    
    public String  getSimConfigProductionNotes() {return unMakeProductionNotes(simConfig);}
    public String  getAssemblyImageLocation()    {return unMakeImage(simConfig, "Assembly");}
    
    public void    setSimConfigurationDescription       (String s) { makeComments(simConfig, "SC", s); }
    public void    setSimConfigEntityTable    (String s) { }; //todo
    public void    setSimConfigurationConclusions       (String s) { makeConclusions(simConfig, "SC", s); }
    public void    setSimConfigationProductionNotes(String s) {makeProductionNotes(simConfig, "SC", s);}
    public void    setAssemblyImageLocation   (String s) {replaceChild(simConfig, makeImage("Assembly", s));}
    
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
    public void setStatsDescription          (String s) { makeComments(statisticalResults,"SR", s); }
    public void setStatsConclusions          (String s) { makeConclusions(statisticalResults,"SR", s); }
    public String getStatsFilePath()         { return statisticalResults.getAttributeValue("file"); }
    public List<Object> getStatsReplicationsList() {return unMakeReplicationList(statisticalResults);}
    public List<String[]> getStastSummaryList() {return unMakeStatsSummList(statisticalResults);}
    
    /** Command line entry point
     * 
     * @param args command line arguments if any (not used)
     */
    public static void main(String[] args) {
        new AnalystReportBuilder();
    }
    
} // end class file AnalystReportBuilder.java