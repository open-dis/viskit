/*
Copyright (c) 1995-2025 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.model;

import edu.nps.util.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;

import viskit.util.EventGraphCache;
import viskit.ViskitGlobals;
import static viskit.ViskitStatics.isFileReady;
import viskit.control.AssemblyControllerImpl;
import viskit.control.EventGraphController;
import viskit.doe.FileHandler;
import viskit.mvc.MvcAbstractModel;
import viskit.reports.HistogramChart;
import viskit.reports.LinearRegressionChart;

/** This class constructs and exports an analyst report based on the parameters
 * selected by the Analyst Report panel in the Viskit UI.  This file uses the
 * assembly file and event graph files as well as customizable items (images,
 * comments) to construct a report that is saved in XML and HTML formats.
 *
 * @author Patrick Sullivan
 * @since July 18, 2006, 7:04 PM
 * @version $Id$
 */
public final class AnalystReportModel extends MvcAbstractModel
{
    static final Logger LOG = LogManager.getLogger();

    private boolean debug = false;

    /** UTH - Assembly file */
    private File   assemblyFile;
    
    private String assemblyName;

    /** The viskit.reports.ReportStatisticsDOM object for this report */
    private Document statisticsReportDocument;
    private String   statisticsReportPath;

    /** The jdom.Document object that is used to build the report */
    private Document reportJdomDocument;

    /** The file name selected by the user from "SAVE AS" menu option */
    private String  analystReportXmFilelName;
    private File    analystReportXmlFile;

    /** The root element of the report xml document */
    private Element rootElement;
    private Element executiveSummaryElement;
    private Element simulationLocationElement;
    private Element assemblyConfigurationElement;
    private Element entityParametersElement;
    private Element behaviorDescriptionsElement;
    private Element statisticalResultsElement;
    private Element conclusionsRecommendationsElement;

    private JProgressBar progressBar;
    
    // Element constants (to avoid spelling errors in source)
    public static final String ANALYST_REPORT              = "AnalystReport";
    public static final String BEHAVIOR_DESCRIPTIONS       = "BehaviorDescriptions";
    public static final String CHART                       = "Chart";
    public static final String CONCLUSIONS_RECOMMENDATIONS = "ConclusionsRecommendations";
    public static final String DIR                         = "dir";
    public static final String DESCRIPTION                 = "Description";
    public static final String ENTITY_PARAMETERS           = "EntityParameters";
    public static final String ENTITY_PARAMETERS_TABLE     = "EntityParametersTable";
    public static final String ENTITY_TABLE                = "EntityTable";
    public static final String EXECUTIVE_SUMMARY           = "ExecutiveSummary";
    public static final String HISTOGRAM_CHART             = "HistogramChart";
    public static final String IMAGE                       = "Image";
    public static final String LINEAR_REGRESSION_CHART     = "LinearRegressionChart";
    public static final String LOCATION                    = "Location";
    public static final String MULTI_PARAMETER             = "MultiParameter";
    public static final String PARAMETER                   = "Parameter";
    public static final String PARAMETER_TABLES            = "ParameterTables";
    public static final String REPLICATION                 = "Replication";
    public static final String REPLICATION_REPORT          = "ReplicationReport";
    public static final String SIM_ENTITY                  = "SimEntity";
    public static final String SIMULATION_CONFIGURATION    = "SimulationConfiguration";
    public static final String SUMMARY                     = "Summary";
    public static final String SUMMARY_REPORT              = "SummaryReport";
    
    public static final String STATISTICAL_RESULTS         = "StatisticalResults";
    public static final String SUMMARY_RECORD              = "SummaryRecord";
    
    // attribute constants (to avoid spelling errors in source)
    public static final String NAME                        = "name";
    public static final String ACCESS                      = "access";
    public static final String AUTHOR                      = "author";
    public static final String DATE                        = "date";
    public static final String DESCRIPTION_ATTRIBUTE       = "description";
    public static final String ENTITY                      = "entity";
    public static final String PROPERTY                    = "property";
    public static final String REPLICATION_STATISTICS      = "replicationStatistics";
    public static final String SUMMARY_STATISTICS          = "summaryStatistics";
    public static final String SHOW_DESCRIPTION            = "showDescription";
    public static final String SHOW_ENTITY_TABLE           = "showEntityTable";
    public static final String SHOW_IMAGE                  = "showImage";
    public static final String SHOW_IMAGES                 = "images"; // TODO
    public static final String SHOW_OVERVIEW               = "showOverview";
    public static final String SHOW_PARAMETER_TABLES       = "showParameterTables";
    public static final String TEXT                        = "text";
    
    public static final String NO_DESCRIPTION_PROVIDED     = "no description found in Event Graph";

    /** Must have the order of the PCL as input from AssemblyModel */
    private Map<String, AssemblyNode> pclNodeCache;

    /** 
     * Build an AnalystReport object from an existing statisticsReport
     * document.This is done from viskit.BasicAssembly via reflection.
     * @param assemblyName the assembly name
     * @param statisticsReportPath the path to the statistics generated report
     *        used by this Analyst Report
     * @param map the set of PCLs that have specific properties set for type statistic desired
     */
    public AnalystReportModel(String assemblyName, String statisticsReportPath, Map<String, AssemblyNode> map)
    {        
        this.assemblyName = assemblyName;
        Document newDocument = null;
        setStatisticsReportPath(statisticsReportPath);
        try {
            newDocument = EventGraphCache.instance().loadXML(statisticsReportPath);
        }
        catch (Exception e) {
            LOG.error("Constructor exception reading {}", statisticsReportPath + " : " + e.getMessage());
        }
        if (newDocument == null)
        {
            LOG.error("Constructor error, found null document\n  " +  statisticsReportPath);
            return; // statistics XML was not written to, not selected for recording
        }

        setStatisticsReportDocument(newDocument);
        setPclNodeCache(map);
        initializeDocument();
    }

    /**
     * <p>Build an analystReport object from an existing partial Analyst Report.
     * This done after the statistic report is incorporated into the basic
     * Analyst Report and further annotations are to be written by the analyst
     * to finalize the report.</p>
     * @param analystReportFrame a reference to the Analyst Report Frame
     * @param xmlFile an existing temp Analyst Report
     * @param newAssemblyFile the current assembly file to process a report from
     * @throws java.lang.Exception general catchall
     */
    public AnalystReportModel(JFrame analystReportFrame, File xmlFile, File newAssemblyFile) throws Exception
    {
        this(xmlFile); // also sets assemblyName
        
        // TODO: This doesn't seem to be doing anything correctly
        progressBar = new JProgressBar();
        analystReportFrame.add(progressBar);
        analystReportFrame.validate();

        LOG.debug("Successful parseXML");
        if (newAssemblyFile != null)
        {
            setAssemblyFile(newAssemblyFile);
            LOG.debug("Successful setting of assembly file");
            postProcessing();
            LOG.debug("Successful post processing of Analyst Report");
//
//            announceAnalystReportReadyToView();
//            reportReady = true;
        }
    }

    /** This constructor for opening a temporary XML report for further
     * annotations, or as required from the analyst/user.  Can be called from
     * the InternalSimulationRunner after a report is ready for display
     *
     * @param newAnalystReportFileXml an existing report to open
     */
    public AnalystReportModel(File newAnalystReportFileXml) 
    {
        if (!isFileReady(newAnalystReportFileXml))
        {
            LOG.error("Constructor error reading\n      {}", newAnalystReportFileXml.getAbsolutePath());
            return;
        }
        assemblyName = newAnalystReportFileXml.getName().substring(0, newAnalystReportFileXml.getName().lastIndexOf("_AnalystReport"));
        try {
            analystReportXmlFile = newAnalystReportFileXml;
            parseXML(newAnalystReportFileXml);
        } 
        catch (Exception ex) {
            LOG.error("Constructor exception reading {}", newAnalystReportFileXml.getAbsolutePath() + " : " + ex.getMessage());
        }
    }

    private void initializeDocument()
    {
        reportJdomDocument = new Document();
        rootElement = new Element(ANALYST_REPORT);
        reportJdomDocument.setRootElement(rootElement);

        fillDocument();
        setDefaultPanelValues();
    }

    private void fillDocument()
    {
        createHeader();
        createExecutiveSummary();
        createSimulationLocation();
        createSimulationConfiguration();
        createEntityParameters();
        createBehaviorDescriptions();
        createStatisticsResults();
        createConclusionsRecommendations();
    }

    /**
     * File I/O that saves the report in XML format
     * @param file the initial temp file to save for further post-processing
     * @return the initial temp file to saved for further post-processing
     * @throws java.lang.Exception general catchall
     */
    public File writeToXMLFile(File file) throws Exception
    {
        if (file == null)
            file = writeToXMLFile();
        _writeCommon(file);
        return file;
    }

    /** @return the initial temp file to be saved for further post-processing
     * @throws java.lang.Exception general catchall
     */
    public File writeToXMLFile() throws Exception {
        return TempFileManager.createTempFile(ANALYST_REPORT, ".xml");
    }

    private void _writeCommon(File file) 
    {
        try {
            FileHandler.marshallJdom(file, reportJdomDocument, false);
        } 
        catch (IOException | JDOMException e) {
            LOG.error("_writeCommon(" + file.getName() + ") Bad JDOM operation: {}: ", e);
        }
    }

    /**
     * Parse a completed out report from XML
     * @param file the XML file to parse
     * @throws Exception is a parsing error is encountered
     */
    private void parseXML(File file) throws Exception 
    {
        reportJdomDocument                = EventGraphCache.instance().loadXML(file);
        rootElement                       = reportJdomDocument.getRootElement();
        executiveSummaryElement           = rootElement.getChild(EXECUTIVE_SUMMARY);
        simulationLocationElement         = rootElement.getChild(LOCATION);
        assemblyConfigurationElement    = rootElement.getChild(SIMULATION_CONFIGURATION);
        entityParametersElement           = rootElement.getChild(ENTITY_PARAMETERS);
        behaviorDescriptionsElement       = rootElement.getChild(BEHAVIOR_DESCRIPTIONS);
        statisticalResultsElement         = rootElement.getChild(STATISTICAL_RESULTS);
        conclusionsRecommendationsElement = rootElement.getChild(CONCLUSIONS_RECOMMENDATIONS);
    }

    /**
     * Creates the root element for the analyst report
     */
    public void createHeader() 
    {
        // keeping filename out of files seemslike a good idea, but might be userful on the display as an uneditable value
        rootElement.setAttribute(NAME,   "");
        rootElement.setAttribute(ACCESS, "");
        rootElement.setAttribute(AUTHOR, "");
        rootElement.setAttribute(DATE,   "");
    }

    /**
     * Populates the executive summary portion of the AnalystReport XML
     */
    public void createExecutiveSummary()
    {
        executiveSummaryElement = new Element(EXECUTIVE_SUMMARY);
        executiveSummaryElement.setAttribute(SHOW_DESCRIPTION, "true");
        rootElement.addContent(executiveSummaryElement);
    }

    /** Creates the SimulationLocation portion of the analyst report XML */
    public void createSimulationLocation()
    {
        simulationLocationElement = new Element(LOCATION);
        simulationLocationElement.setAttribute(SHOW_DESCRIPTION, "true");
        simulationLocationElement.setAttribute(SHOW_IMAGE,   "true");
        makeCustomDescriptionElement(simulationLocationElement, "SL", ""); // TODO SL -> SimulationLocation
        makeProductionNotes(simulationLocationElement, "SL", ""); // TODO SL -> SimulationLocation
        makeConclusions(simulationLocationElement, "SL", ""); // TODO SL -> SimulationLocation
        rootElement.addContent(simulationLocationElement);
    }

    /** Creates the simulation configuration portion of the Analyst report XML */
    private void createSimulationConfiguration() 
    {
        assemblyConfigurationElement = new Element(SIMULATION_CONFIGURATION);
        assemblyConfigurationElement.setAttribute(SHOW_DESCRIPTION, "true");
        assemblyConfigurationElement.setAttribute(SHOW_IMAGE, "true");
        assemblyConfigurationElement.setAttribute(SHOW_ENTITY_TABLE, "true");
        makeCustomDescriptionElement(assemblyConfigurationElement, "SC", "");
        makeProductionNotes(assemblyConfigurationElement, "SC", ""); // TODO SC -> SimulationConfiguration
        makeConclusions(assemblyConfigurationElement, "SC", "");
        if (assemblyFile != null) {
            assemblyConfigurationElement.addContent(EventGraphCache.instance().getEntityTable());
        }

        rootElement.addContent(assemblyConfigurationElement);
    }

    /** Creates the entity parameter section of this analyst report */
    private void createEntityParameters() 
    {
        entityParametersElement = new Element(ENTITY_PARAMETERS);
        entityParametersElement.setAttribute(SHOW_DESCRIPTION, "true");
        entityParametersElement.setAttribute(SHOW_PARAMETER_TABLES, "true");
        makeCustomDescriptionElement(entityParametersElement, "EP", "");
        makeConclusions(entityParametersElement, "EP", "");
        if (assemblyFile != null) {
            entityParametersElement.addContent(makeParameterTables());
        }

        rootElement.addContent(entityParametersElement);
    }

    /** Creates the behavior descriptions portion of the report */
    private void createBehaviorDescriptions() 
    {
        behaviorDescriptionsElement = new Element(BEHAVIOR_DESCRIPTIONS);
        behaviorDescriptionsElement.setAttribute(SHOW_DESCRIPTION, "true");
        behaviorDescriptionsElement.setAttribute("showDetails", "true");
        behaviorDescriptionsElement.setAttribute("showImage", "true");
        makeCustomDescriptionElement(behaviorDescriptionsElement,"BD", "");
        makeConclusions(behaviorDescriptionsElement,"BD", "");

        behaviorDescriptionsElement.addContent(processBehaviors(true, true, true));

        rootElement.removeChild(BEHAVIOR_DESCRIPTIONS);
        rootElement.addContent(behaviorDescriptionsElement);
    }

    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private void createStatisticsResults() 
    {
        statisticalResultsElement = new Element(STATISTICAL_RESULTS);
        rootElement.addContent(statisticalResultsElement);

        statisticalResultsElement.setAttribute(SHOW_DESCRIPTION, "true");
        statisticalResultsElement.setAttribute(REPLICATION_STATISTICS, "true"); // TODO full name
        statisticalResultsElement.setAttribute(SUMMARY_STATISTICS, "true");     // TODO full name
        makeCustomDescriptionElement(statisticalResultsElement,"SR", "");
        makeConclusions(statisticalResultsElement,"SR", "");

        if (statisticsReportPath != null && statisticsReportPath.length() > 0) {
            statisticalResultsElement.setAttribute("file", statisticsReportPath);

            Element sumReport = new Element(SUMMARY_REPORT);
            List<Element> itr = statisticsReportDocument.getRootElement().getChildren(SIM_ENTITY);
            List<Element> summItr;
            Element temp, summaryRecord, summaryStatistics;
            for (Element entity : itr) {
                temp = (Element) entity.clone();
                temp.removeChildren(SUMMARY_REPORT);

                summaryStatistics = entity.getChild(SUMMARY_REPORT);
                summItr = summaryStatistics.getChildren(SUMMARY);
                for (Element temp2 : summItr) {
                    summaryRecord = new Element(SUMMARY_RECORD);
                    summaryRecord.setAttribute(ENTITY, entity.getAttributeValue(NAME));
                    summaryRecord.setAttribute(PROPERTY, temp2.getAttributeValue(PROPERTY));
                    summaryRecord.setAttribute("numRuns", temp2.getAttributeValue("numRuns"));
                    summaryRecord.setAttribute("minObs", temp2.getAttributeValue("minObs"));
                    summaryRecord.setAttribute("maxObs", temp2.getAttributeValue("maxObs"));
                    summaryRecord.setAttribute("mean", temp2.getAttributeValue("mean"));
                    summaryRecord.setAttribute("stdDeviation", temp2.getAttributeValue("stdDeviation"));
                    summaryRecord.setAttribute("variance", temp2.getAttributeValue("variance"));
                    sumReport.addContent(summaryRecord);
                }
            }

            statisticalResultsElement.addContent(makeReplicationReport());
            statisticalResultsElement.addContent(sumReport);
        }
    }

    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    public List<Object> unMakeReplicationList(Element statisticalResults) {
        Vector<Object> v = new Vector<>();
        Vector<Object> se;
        Vector<String[]> r;
        String[] sa;
        Element repReports = statisticalResults.getChild("ReplicationReports");
        List<Element> simEnts = repReports.getChildren(SIM_ENTITY);
        for (Element sEnt : simEnts) {

            se = new Vector<>(3);
            se.add(sEnt.getAttributeValue(NAME));
            se.add(sEnt.getAttributeValue(PROPERTY));

            r = new Vector<>();
            List<Element> repLis = sEnt.getChildren(REPLICATION);
            for(Element rep : repLis) {
                sa = new String[7];
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
    public List<String[]> unMakeStatisticsSummaryList(Element statisticalResults) {
        Vector<String[]> v = new Vector<>();

        Element sumReports = statisticalResults.getChild(SUMMARY_REPORT);
        List<Element> recs = sumReports.getChildren(SUMMARY_RECORD);
        String[] sa;
        for (Element rec : recs) {
            sa = new String[8];
            sa[0] = rec.getAttributeValue(ENTITY);
            sa[1] = rec.getAttributeValue(PROPERTY);
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
        conclusionsRecommendationsElement = new Element(CONCLUSIONS_RECOMMENDATIONS);
        conclusionsRecommendationsElement.setAttribute(SHOW_DESCRIPTION, "true");
        makeCustomDescriptionElement(conclusionsRecommendationsElement, "CR", "");
        makeConclusions(conclusionsRecommendationsElement, "CR", "");
        rootElement.addContent(conclusionsRecommendationsElement);
    }

    /** Creates Behavior definition references in the analyst report template
     * @param showDescription if true, show description text
     * @param showAllImages if true, show all images
     * @param showAllDetails if true, show all details text
     * @return a table of scenario Event Graph Behaviors
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element processBehaviors(boolean showDescription, boolean showAllImages, boolean showAllDetails) 
    {
        Element behaviorListElement = new Element("BehaviorList");
        Element behaviorElement, localRootElement, descriptionElement, parameterElement, stateVariable, eventGraphImageElement;
        String descriptionString, imagePath;
        Document tmpDocument;
        List<Element> localRootElementParameters, lre2;
        for (int i = 0; i < EventGraphCache.instance().getEventGraphNamesList().size(); i++) 
        {
            behaviorElement = new Element("Behavior");
            behaviorElement.setAttribute(NAME, EventGraphCache.instance().getEventGraphNamesList().get(i));

            if (showDescription)
            {
                tmpDocument = EventGraphCache.instance().loadXML(EventGraphCache.instance().getEventGraphFilesList().get(i));
                localRootElement = tmpDocument.getRootElement();

                // prevent returning a null if there was no attribute value
                descriptionString = (localRootElement.getChildText(DESCRIPTION) == null) ? NO_DESCRIPTION_PROVIDED : localRootElement.getChildText(DESCRIPTION);

                descriptionElement = new Element(DESCRIPTION);
                descriptionElement.setAttribute(TEXT, descriptionString);
                behaviorElement.addContent(descriptionElement);

                if (showAllDetails) 
                {
                    localRootElementParameters = localRootElement.getChildren(PARAMETER);
                    for (Element temp : localRootElementParameters) {
                        parameterElement = new Element(PARAMETER);
                        parameterElement.setAttribute(NAME, temp.getAttributeValue(NAME));
                        parameterElement.setAttribute("type", temp.getAttributeValue("type"));

                        // The data "null" is not legal for a JDOM attribute
                        parameterElement.setAttribute(DESCRIPTION_ATTRIBUTE, (temp.getChildText(DESCRIPTION) == null) ? NO_DESCRIPTION_PROVIDED : temp.getChildText(DESCRIPTION));
                        behaviorElement.addContent(parameterElement);
                    }
                    lre2 = localRootElement.getChildren("StateVariable");
                    for (Element temp : lre2) {
                        stateVariable = new Element("stateVariable");
                        stateVariable.setAttribute(NAME, temp.getAttributeValue(NAME));
                        stateVariable.setAttribute("type", temp.getAttributeValue("type"));

                        // The data "null" is not legal for a JDOM attribute
                        stateVariable.setAttribute(DESCRIPTION_ATTRIBUTE, (temp.getChildText(DESCRIPTION) == null) ? NO_DESCRIPTION_PROVIDED : temp.getChildText(DESCRIPTION));
                        behaviorElement.addContent(stateVariable);
                    }
                }
            }
            if (showAllImages) {
                eventGraphImageElement = new Element("EventGraphImage");

                // Set relative path only
                imagePath = EventGraphCache.instance().getEventGraphImageFilesList().get(i).getPath();
                imagePath = imagePath.substring(imagePath.indexOf("images"), imagePath.length());
                eventGraphImageElement.setAttribute(DIR, imagePath);
                behaviorElement.addContent(eventGraphImageElement);
            }
            behaviorListElement.addContent(behaviorElement);
        }

        return behaviorListElement;
    }

    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    List unMakeBehaviorList(Element localRootElement) 
    {
        ArrayList v = new ArrayList();

        Element BehaviorListElement = localRootElement.getChild("BehaviorList");
        if (BehaviorListElement != null) 
        {
            List<Element> behaviorList = BehaviorListElement.getChildren("Behavior");
            
            String name, descriptionText, parameterName, parameterType, parameterDescription, stateVariableElementName, stateVariableElementType, stateVariableElementDescriotion;
            String[] parameterStringArray, stateVariableStringArray;
            ArrayList<Object> behaviorArrayList;
            ArrayList<String[]> parameterArrayList, stateVariableArrayList;
            Element descriptionElement, eventGraphImageElement;
            List<Element> parameterElementList, stateVariableElementList;
            for (Element behavior : behaviorList) 
            {
                behaviorArrayList = new ArrayList<>();
                name = behavior.getAttributeValue(NAME);
                behaviorArrayList.add(name);

                descriptionElement = behavior.getChild(DESCRIPTION);
                descriptionText    = descriptionElement.getAttributeValue(TEXT);
                behaviorArrayList.add(descriptionText);

                parameterElementList = behavior.getChildren(PARAMETER);

                parameterArrayList = new ArrayList<>();
                for (Element parameterElement : parameterElementList) {
                    parameterName = parameterElement.getAttributeValue(NAME);
                    parameterType = parameterElement.getAttributeValue("type");
                    parameterDescription = parameterElement.getAttributeValue(DESCRIPTION_ATTRIBUTE);
                    parameterStringArray = new String[]{parameterName, parameterType, parameterDescription};
                    parameterArrayList.add(parameterStringArray);
                }
                behaviorArrayList.add(parameterArrayList);

                stateVariableElementList = behavior.getChildren("stateVariable");

                stateVariableArrayList = new ArrayList<>();
                for (Element stateVariableElement : stateVariableElementList) 
                {
                    stateVariableElementName        = stateVariableElement.getAttributeValue(NAME);
                    stateVariableElementType        = stateVariableElement.getAttributeValue("type");
                    stateVariableElementDescriotion = stateVariableElement.getAttributeValue(DESCRIPTION_ATTRIBUTE);
                    stateVariableStringArray = new String[]{stateVariableElementName, 
                                                            stateVariableElementType, 
                                                            stateVariableElementDescriotion};
                    stateVariableArrayList.add(stateVariableStringArray);
                }
                behaviorArrayList.add(stateVariableArrayList);

                eventGraphImageElement = behavior.getChild("EventGraphImage");
                behaviorArrayList.add(eventGraphImageElement.getAttributeValue(DIR));

                v.add(behaviorArrayList);
            }
        }
        return v;
    }

    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    Vector<Object[]> unMakeParameterTables(Element rootOfTabs) {
        Element element = rootOfTabs.getChild(PARAMETER_TABLES);
        List<Element> entityParameterTableList = element.getChildren(ENTITY_PARAMETERS_TABLE);
        Vector<Object[]> v = new Vector<>(entityParameterTableList.size());   // list of entpartab elms
        List<Element> elementList_0, elementList_1;
        Vector<Object[]> v_0, v_1;
        String name, value;
        for(Element e_0 : entityParameterTableList) {
            elementList_0 = e_0.getChildren();       //list of parts: class/id/phys/dynam
            v_0 = new Vector<>(elementList_0.size());
            for(Element e_1 : elementList_0) {
                elementList_1 = e_1.getChildren(PARAMETER);     // list of param elms

                v_1 = new Vector<>(elementList_1.size());
                for(Element e_2 : elementList_1) {
                    name = e_2.getAttributeValue(NAME);
                    value  = e_2.getAttributeValue("value");
                    v_1.add(new String[]{name, value});
                }
                v_0.add(new Object[]{e_1.getName(),v_1});
            }
            v.add(new Object[]{e_0.getAttributeValue(NAME),v_0});
        }
        return v;
    }

    /**
     * Creates parameter tables for all files in the assembly that have SMAL
     * definitions.
     *
     * @return a Parameter Table for a given event graph
     */
    private Element makeParameterTables() {
        return makeTablesCommon(PARAMETER_TABLES);
    }

    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element makeTablesCommon(String tableName) {
        Element table = new Element(tableName);
        Element localRootElement = EventGraphCache.instance().getAssemblyDocument().getRootElement();
        List<Element> simEntityList = localRootElement.getChildren(SIM_ENTITY);
        String entityName;
        List<Element> entityParametersElementList;
        for (Element temp : simEntityList) {
            entityName = temp.getAttributeValue(NAME);
            entityParametersElementList = temp.getChildren(MULTI_PARAMETER);
            for (Element parameterElement : entityParametersElementList) {
                if (parameterElement.getAttributeValue("type").equals("diskit.SMAL.EntityDefinition")) {
                    table.addContent(extractSMAL(entityName, parameterElement));
                }
            }
        }

        return table;
    }

    /**
     * Takes viskit.Assembly formatted SMAL.EntityDefinition data and formats it
     * for the analyst report
     *
     * @param entityName the name of the entity
     * @param entityDef  the entityDefinition for this file
     * @return table of properly formatted entries
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element extractSMAL(String entityName, Element entityDef) {
        Element table = new Element(ENTITY_PARAMETERS_TABLE);
        ElementFilter multiParam = new ElementFilter(MULTI_PARAMETER);
        Iterator<Element> itr = entityDef.getDescendants(multiParam);
        table.setAttribute(NAME, entityName);
        Element temp;
        String category;
        while (itr.hasNext()) {
            temp = itr.next();
            category = temp.getAttributeValue("type");
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
        List<Element> dataList = data.getChildren("TerminalParameter");
        Element parameterElement;
        for (Element temp : dataList) {
            if (!temp.getAttributeValue("value").equals("0")) {
                parameterElement = new Element(PARAMETER);
                parameterElement.setAttribute(NAME, temp.getAttributeValue(NAME));
                parameterElement.setAttribute("value", temp.getAttributeValue("value"));

                tableEntry.addContent(parameterElement);
            }
        }
        return tableEntry;
    }

    // TODO: Version 1.1 JDOM does not yet support generics
    @SuppressWarnings("unchecked")
    public String[][] unMakeEntityTable() 
    {
        String[][] emptyResult = new String[0][0];
        if (assemblyConfigurationElement == null)
        {
            LOG.error("AnalystReportModel unMakeEntityTable() simulationConfigurationElement is null");
            return emptyResult;
        }
        if (!assemblyConfigurationElement.getChildren().isEmpty())
        {
            Element element = assemblyConfigurationElement.getChild(ENTITY_TABLE);
            if (element == null)
            {
                LOG.error("AnalystReportModel unMakeEntityTable() simulationConfigurationElement EntityTable is null");
                return emptyResult;
            }
            List<Element> elementsList = element.getChildren(SIM_ENTITY);

            String[][] sa = new String[elementsList.size()][2];
            int i = 0;
            for(Element e : elementsList) {
                sa[i]  [0] = e.getAttributeValue(NAME);
                sa[i++][1] = e.getAttributeValue("fullyQualifiedName");
            }
            return sa;
        }
        else return emptyResult;
    }

    /**
     * This method re-shuffles the statistics report to a format that is handled
     * by the xslt for the analyst report.  The mismatch of formatting was discovered
     * after all classes were written. TODO this should be cleaned up or the XML formatted
     * more uniformly.
     * @return the replication report
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element makeReplicationReport() 
    {
        Element replicatioReportsElement = new Element("ReplicationReports");
        List<Element> simEntitiesElementList = statisticsReportDocument.getRootElement().getChildren(SIM_ENTITY);

        // variables for JFreeChart construction
        HistogramChart histogramChart = new HistogramChart();
        LinearRegressionChart linearRegressionChart = new LinearRegressionChart();
        String chartTitle, axisLabel, typeStat = "", dataPointProperty;
        List<Element> dataPointsElementList, replicationReportsElementList, replicationsElementList;
        boolean isCount;
        Object obj;
        Element entityElement, histogramChartUrlElement, linearRegressionChartUrlElement, replicationRecordElement;
        double[] dataArray;
        int index;
        for (Element simEntityElement : simEntitiesElementList) {
            dataPointsElementList = simEntityElement.getChildren("DataPoint");
            for (Element dataPoint : dataPointsElementList) {
                dataPointProperty = dataPoint.getAttributeValue(PROPERTY);
                for (Map.Entry<String, AssemblyNode> entry : getPclNodeCache().entrySet()) {
                    obj = getPclNodeCache().get(entry.getKey());
                    if (obj.getClass().toString().contains("PropertyChangeListenerNode")) {
                        try {
                            isCount = Boolean.parseBoolean(obj.getClass().getMethod("isGetCount").invoke(obj).toString());
                            typeStat = isCount ? "count" : "mean";
                            break;
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                            LOG.error(ex);
                        }
                    }
                }
                entityElement = new Element(SIM_ENTITY);
                entityElement.setAttribute(NAME, simEntityElement.getAttributeValue(NAME));
                entityElement.setAttribute(PROPERTY, dataPointProperty);
                replicationReportsElementList = dataPoint.getChildren(REPLICATION_REPORT);

                // Chart title and label
                chartTitle = simEntityElement.getAttributeValue(NAME);
                axisLabel  = dataPoint.getAttributeValue(PROPERTY) ;

                for (Element replicationReport : replicationReportsElementList) {
                    replicationsElementList = replicationReport.getChildren(REPLICATION);

                    // Create a data set instance and histogramChart for each replication report
                    dataArray = new double[replicationReport.getChildren().size()];
                    index = 0;
                    for (Element replication : replicationsElementList) {
                        replicationRecordElement = new Element(REPLICATION);
                        replicationRecordElement.setAttribute("number", replication.getAttributeValue("number"));
                        replicationRecordElement.setAttribute("count", replication.getAttributeValue("count"));
                        replicationRecordElement.setAttribute("minObs", replication.getAttributeValue("minObs"));
                        replicationRecordElement.setAttribute("maxObs", replication.getAttributeValue("maxObs"));
                        replicationRecordElement.setAttribute("mean", replication.getAttributeValue("mean"));
                        replicationRecordElement.setAttribute("stdDeviation", replication.getAttributeValue("stdDeviation"));
                        replicationRecordElement.setAttribute("variance", replication.getAttributeValue("variance"));
                        entityElement.addContent(replicationRecordElement);

                        // Add the raw count, or mean of replication data to the chart generators
                        LOG.debug(replication.getAttributeValue(typeStat));
                        dataArray[index] = Double.parseDouble(replication.getAttributeValue(typeStat));
                        index++;
                    }

                    histogramChartUrlElement        = new Element(HISTOGRAM_CHART);
                    linearRegressionChartUrlElement = new Element(LINEAR_REGRESSION_CHART);

                    if ((assemblyFile != null) && ((assemblyName == null) || assemblyName.isBlank()))  // TODO duplicative
                        if (assemblyFile.getName().contains(".xml"))
                            assemblyName = assemblyFile.getName().substring(0, assemblyFile.getName().indexOf(".xml"));
                    
                    histogramChartUrlElement.setAttribute(DIR, 
                              histogramChart.createChart(assemblyName, chartTitle, axisLabel, dataArray));
                    entityElement.addContent(histogramChartUrlElement);

                    //  dataArray must be > than length 1 for scatter regression
                    if (dataArray.length > 1) 
                    {
                        linearRegressionChartUrlElement.setAttribute(DIR, 
                       linearRegressionChart.createChart(assemblyName, chartTitle, axisLabel, dataArray));
                        entityElement.addContent(linearRegressionChartUrlElement);
                    }

                    replicatioReportsElement.addContent(entityElement);
                }
            }
        }
        return replicatioReportsElement;
    }

    /**
     * Converts boolean input into a 'true'/'false' string representation for
     * use as an attribute value in the Analyst report XML.
     *
     * @param value the boolean variable to convert
     * @return the string representation of the boolean variable
     */
    private String booleanToString(boolean value) {return value ? "true" : "false";}

    private boolean stringToBoolean(String s) {return s.equalsIgnoreCase("true");}

    /**
     * Creates a standard 'Image' element used by all sections of the report
     *
     * @param imageID a unique identifier for this XML Element
     * @param dir     the directory of the image
     * @return the Image url embedded in well formed XML
     */
    private Element makeImageElement(String imageID, String dir) {
        Element imageElement = new Element(imageID + IMAGE);

        // Set relative path only
        imageElement.setAttribute(DIR, dir.substring(dir.indexOf("images"), dir.length()));
        return imageElement;
    }

    private String unMakeImage(Element element, String imageID) {
        return _unMakeContent(element, imageID + IMAGE, DIR);
    }

    /**
     * Creates a standard 'Description' element used by all sections of the report
     * to add descriptions.  TODO rename as Description for correctness.
     *
     * @param parentElement parentElement
     * @param elementPrefix  the tag prefix used to identify unique Description elements (used by XSLT)
     * @param descriptionText the text descriptions
     */
    public void makeCustomDescriptionElement(Element parentElement, String elementPrefix, String descriptionText)
    {
        Element newElement = _makeElementContent(elementPrefix, DESCRIPTION, descriptionText);
        replaceChildren(parentElement, newElement);
    }

//    /** Unused
//     * @param descriptionTag the comment Element, which actually contains description
//     * @param descriptionText the comment text, which actually contains description
//     * @return the Comments Element
//     */
//    public Element xmakeComments(String descriptionTag, String descriptionText) 
//    {
//        return _makeElementContent(descriptionTag, DESCRIPTION, descriptionText);
//    }

    private String unMakeCustomDescriptionElements(Element element) 
    {
        return _unMakeContent(element, DESCRIPTION);
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
        return _makeElementContent(commentTag,"Conclusions",conclusionText);
    }

    public void makeConclusions(Element parent, String commentTag, String conclusionText) {
        replaceChildren(parent,_makeElementContent(commentTag,"Conclusions",conclusionText));
    }

    /** @param element the Element to extract information from
     * @return a String object of the Element's contents
     */
    public String unMakeConclusions(Element element) {
        return _unMakeContent(element, "Conclusions");
    }

    /**
     * Creates a standard 'Production Notes' element used by all sections of the report
     * to add conclusions
     *
     * @param productionNotesTag the tag used to identify unique Production Notes (used by XSLT)
     * @param productionNotesText author's text block
     * @return the ProductionNotes Element
     */
    public Element xmakeProductionNotes(String productionNotesTag, String productionNotesText) {
        return _makeElementContent(productionNotesTag, "ProductionNotes", productionNotesText);
    }

    /**
     * Creates a standard 'Production Notes' element used by all sections of the
     * report to add production notes
     *
     * @param parentElement the parent element to add content too
     * @param productionNotesTag the tag used to identify unique production notes (used by XSLT)
     * @param productionNotesText author's text block
     */
    public void makeProductionNotes(Element parentElement, String productionNotesTag, String productionNotesText) {
        replaceChildren(parentElement, _makeElementContent(productionNotesTag, "ProductionNotes", productionNotesText));
    }

    public String unMakeProductionNotes(Element e) {
        return _unMakeContent(e, "ProductionNotes");
    }

    private Element _makeElementContent(String elementPrefix, String elementSuffix, String textValue)
    {
        Element newElement = new Element((elementPrefix + elementSuffix));
        newElement.setAttribute(TEXT, textValue);
        return newElement;
    }

    private String _unMakeContent(Element element, String suffix) 
    {
        return _unMakeContent(element,suffix,TEXT);
    }

    private String _unMakeContent(Element element, String suffix, String attributeName) 
    {
        if (element == null) {return "";}
        List content = element.getContent();
        Object o;
        Element contentELement;
        for (Iterator iterator = content.iterator(); iterator.hasNext();) {
            o = iterator.next();
            if (!(o instanceof Element)) {
                continue;
            }
            contentELement = (Element) o;
            if (contentELement.getName().endsWith(suffix)) {
                return contentELement.getAttributeValue(attributeName);
            }
        }
        return "";
    }
    /** Keeps parentElement while replacing children with childElement */
    private void replaceChildren(Element parentElement, Element childElement)
    {
        if (parentElement == null)
            return;

        parentElement.removeChildren(childElement.getName());
        parentElement.addContent(childElement);
    }

    /**
     * Initialize all panel values to default settings
     */
    private void setDefaultPanelValues() 
    {
        //Header values
        setReportName         ("***ENTER REPORT TITLE HERE***");
        setDocumentAccessLabel("***ENTER ACCESS LABEL HERE***");
        setAuthor             ("***ENTER NAME OF AUTHOR HERE***");
        setDateOfReport       (DateFormat.getInstance().format(new Date()));
        setDocumentAccessLabel("");

        setShowExecutiveSummary(true);
        setExecutiveSummary   ("***ENTER EXECUTIVE SUMMARY HERE***");

        setScenarioLocationIncluded(true);
        setShowScenarioLocationImage(true);
        setScenarioLocationDescription    ("***ENTER SCENARIO LOCATION DESCRIPTION HERE***");
        setScenarioLocationProductionNotes("***ENTER SCENARIO LOCATION PRODUCTION NOTES HERE***");
        setScenarioLocationConclusions    ("***ENTER SCENARIO LOCATION CONCLUSIONS HERE***");
        //setChartImage(""); // TODO: generate nauthical chart image, set file location

        setAssemblyConfigurationIncluded(true);
        setShowAssemblyConfigurationImage(true);
        setShowAssemblyEntityDefinitionsTable(true);
        setAssemblyDesignConsiderations       ("***ENTER ASSEMBLY DESIGN CONSIDERATIONS HERE***");
        setAssemblyConfigationProductionNotes ("***ENTER ASSEMBLY DESIGN PRODUCTION NOTES HERE***");
        setAssemblyConfigurationConclusions   ("***ENTER ASSEMBLY DESIGN CONCLUSIONS HERE***");

        //Entity Parameters values
        setShowEntityParametersOverview(true);
        setShowEntityParametersTables   (true);
        setEntityParametersOverview    ("***ENTER ENTITY PARAMETER OVERVIEW HERE***");
        setEntityParametersConclusions ("***ENTER ENTITY PARAMETER CONCLUSIONS HERE***"); // TODO not shown?

        //BehaviorParameter values
        setShowBehaviorDesignAnalysisDescriptions(true);
        setShowEventGraphImages(true);
        setShowBehaviorDescriptions(true);
        setShowEventGraphDetails(true);
        setBehaviorDescription("***ENTER ENTITY BEHAVIOR DESCRIPTION HERE***");
        setBehaviorConclusions("***ENTER ENTITY BEHAVIOR CONCLUSIONS HERE***");

        //StatisticalResults values
        setShowStatisticsDescriptionAnalysis(true);
        setShowReplicationStatistics(true);
        setShowSummaryStatistics(true);
        setStatisticsDescription("***ENTER STATISTICAL RESULTS DESCRIPTION HERE***");
        setStatisticsConclusions("***ENTER STATISTICAL RESULTS CONCLUSIONS HERE***");

        //Recommendations/Conclusions
        setShowRecommendationsConclusions(true);
        setConclusions    ("***ENTER ANALYST CONCLUSIONS HERE***");
        setRecommendations("***ENTER RECOMMENDATIONS FOR FUTURE WORK HERE***");
    }

    public boolean isDebug()                         {return debug;}

    public Document   getReportJdomDocument()        {return reportJdomDocument;}
    public Document   getStatisticsReport()          {return statisticsReportDocument;}
    public Element    getRootElement()               {return rootElement;}
    public String     getAnalystReportFileXmlName()  {return analystReportXmFilelName;}
    public File       getAnalystReportXmlFile()      {return analystReportXmlFile;}
    public String     getAuthor()                    {return rootElement.getAttributeValue(AUTHOR);}
    public String     getDocumentAccessLabel()       {return rootElement.getAttributeValue(ACCESS);}
    public String     getDateOfReport()              {return rootElement.getAttributeValue(DATE);}
    public String     getReportName()                {return rootElement.getAttributeValue(NAME);}

    /**
     * Called twice.  Once for preliminary Analyst Report, then for full integration Analyst Report.
     * @param newAssemblyFile the Assembly File to parse for information
     */
    public void setAssemblyFile(File newAssemblyFile) 
    {
        assemblyFile = newAssemblyFile;

        // Subsequent calls within the same runtime require a cleared cache
        // which this does
        EventGraphCache.instance().makeEntityTable(assemblyFile);
        if (assemblyConfigurationElement == null)
            return; // stats report not set for recording

        assemblyConfigurationElement.addContent(EventGraphCache.instance().getEntityTable());
        entityParametersElement.addContent(makeParameterTables());
        createBehaviorDescriptions();
    }

    private boolean reportReady = false;

    public boolean isReportReady() {
        return reportReady;
    }

    public void setReportReady(boolean value) {
        reportReady = value;
    }

    /** Post-processing steps to take for Analyst Report preparation */
    private void postProcessing() 
    {
        progressBar.setIndeterminate(true);
        progressBar.setString("Analyst Report now generating...");
        progressBar.setStringPainted(true);

        LOG.debug("JProgressBar set");

        captureEventGraphImages();
        LOG.debug("Event Graphs captured");
        captureAssemblyImage();
        LOG.debug("Assembly captured");
        captureLocationImage();
        LOG.debug("Location Image captured");

        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(false);
    }

    /** Utility method used here to invoke the capability to capture all Event
     * Graph images of which are situated in a particular Assembly File.  These
     * PNGs will be dropped into ${viskitProject}/AnalystReports/images/EventGraphs </p>
     */
    private void captureEventGraphImages() 
    {
        EventGraphCache eventGraphCache = EventGraphCache.instance();
        ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).captureEventGraphImages(
                    eventGraphCache.getEventGraphFilesList(),
                eventGraphCache.getEventGraphImageFilesList());
    }

    /** Utility method used here to invoke the capability to capture the
     * Assembly image of the loaded Assembly File.  This PNG will be dropped
     * into ${viskitProject}/AnalystReports/images/Assemblies </p>
     */
    private void captureAssemblyImage() 
    {
        String assemblyFilePath = assemblyFile.getPath();
        assemblyFilePath = assemblyFilePath.substring(assemblyFilePath.indexOf("Assemblies"), assemblyFilePath.length());
        if      (assemblyFilePath.contains("\\"))
                 assemblyFilePath = assemblyFilePath.substring(assemblyFilePath.lastIndexOf("\\") + 1);
        else if (assemblyFilePath.contains("/"))
                 assemblyFilePath = assemblyFilePath.substring(assemblyFilePath.lastIndexOf("/")  + 1);
        assemblyFilePath = "Assemblies/" + assemblyFilePath;
        File assemblyImageFile = new File(
                ViskitGlobals.instance().getViskitProject().getAnalystReportImagesDirectory(),
                assemblyFilePath + ".png"); // ends with .xml.png

        if (!assemblyImageFile.getParentFile().exists())
             assemblyImageFile.mkdirs();

        setAssemblyImageLocation(assemblyImageFile.getPath());
        ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).captureAssemblyImage(
                assemblyImageFile);
        LOG.info("Assembly graph image saved at\n      " + assemblyImageFile.getAbsolutePath());
    }

    public void announceAnalystReportReadyToView()
    {
        // TODO consider inserting loaded assembly filename into message above as a user confirmation
        
        String assemblyName = assemblyFile.getName().substring(0, assemblyFile.getName().indexOf(".xml"));
        int numberOfReplications = ViskitGlobals.instance().getSimulationRunPanel().getNumberOfReplications();
        String popupTitle = "Simulation Run Data Collected, Analyst Report Ready";
        String message =
                "<html><body>" +
                "<p align='center'>" + numberOfReplications + " total replication";
        if (numberOfReplications > 1)
            message = message + "s";
        message = message +  " performed, with data saved.</p><br />" +
                  // Elapsed clock time: TODO
                "<p align='center'>" + assemblyName + " Analyst Report</p><br />" +
                "<p align='center'>is now loaded and ready for further analysis.</p><br /></body></html>";
                
        ViskitGlobals.instance().selectSimulationRunTab();
        ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                popupTitle, message);
        // user: OK
        
        ViskitGlobals.instance().selectAnalystReportTab();
        popupTitle = "Ready to Edit and Display HTML Analyst Report";
        
        message =
                   "<html><body>" +
                   "<p align='center'>Edit tabbed field values as needed, then select</p><br />" +
                   "<p align='center'><b>Display HTML Analyst Report</b></p><br />";
        // no joy
//        String  menuImageURL = "doc/images/AnalystReportDisplayHtmlMenu.png";
//        boolean menuImageFileExists = (new File("doc/images/AnalystReportDisplayHtmlMenu.png").exists()); // debug
//        if (imageFileExists)
//            message += // not working
//                   "<p align='center'><a href='" + imageURL + "'><img src='" + imageURL + "'/></a></p><br />";

        message += "<p align='center'>in Analyst Report menu to view results</p><br /></body></html>";
        ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE, popupTitle, message);
        
        /* better not to present another decision, had thread-completion issues anyway
        message =
                "<html><body>" +
                "<p align='center'>Do you want to view HTML Analyst Report</p><br />" +
                "<p align='center'>or simply continue your analysis?</p><br />";
        int returnValue = ViskitGlobals.instance().getMainFrame().genericAsk2Buttons(popupTitle, message, 
                "View HTML", "Continue Analysis");
        
        if  (returnValue == 0) // yes, build and show report
        {
            String htmlFilePath = getAnalystReportXmlFile().getAbsolutePath();
            // change file extension. remove timestamp for HTML file path
            htmlFilePath = htmlFilePath.substring(0,htmlFilePath.indexOf("_AnalystReport")) + "_AnalystReport.html";
            if (htmlFilePath.startsWith("."))
                htmlFilePath = htmlFilePath.substring(2); // no "hidden" files; possible problem?
            LOG.info("announceAnalystReportReadyToView() htmlFilePath=\n      ", htmlFilePath);
            
            XsltUtility.runXslt(getAnalystReportXmlFile().getAbsolutePath(),
                htmlFilePath, "config/AnalystReportXMLtoHTML.xslt");
            ViskitGlobals.instance().getAnalystReportController().showHtmlViewer(htmlFilePath); // TODO show HTML, need filename
//             ViskitGlobals.instance().getAnalystReportController().generateHtmlReport(); // too far back in workflow
        }
        // TODO fix other conversion HTML filenames to match this abbrieviated form
        */
    }

    /** If a 2D top town image was generated from SavageStudio, then point to
     *  this location
     */
    private void captureLocationImage() {
        File locationImage = new File(
                ViskitGlobals.instance().getViskitProject().getAnalystReportImagesDirectory(),
                assemblyFile.getName() + ".png");

        LOG.debug(locationImage);
        if (locationImage.exists()) {

            // Set relative path only
            setLocationImage(locationImage.getPath());
        }
        LOG.debug(getLocationImage());
    }

    public void setAnalystReportFileName      (String newAnalystReportFileName) 
                                              { this.analystReportXmFilelName = newAnalystReportFileName; }
    public void setAnalystReportFile          (File newAnalystReportFile) 
                                              { this.analystReportXmlFile     = newAnalystReportFile; }
    public void setStatisticsReportDocument   (Document newStatisticsReportDocument)      
                                              { this.statisticsReportDocument = newStatisticsReportDocument; }
    public void setStatisticsReportPath       (String filePath)           { this.statisticsReportPath = filePath; }
    public void setAuthor                     (String s) { rootElement.setAttribute(AUTHOR, s); };
    public void setDocumentAccessLabel        (String s) { rootElement.setAttribute(ACCESS, s);}
    public void setDateOfReport               (String s) { rootElement.setAttribute(DATE, s);}
    public void setDebug                      (boolean value) { this.debug = value; }
    public void setReportName                 (String s) { rootElement.setAttribute(NAME, s); }

    public boolean isShowRecommendationsConclusions() { return stringToBoolean(conclusionsRecommendationsElement.getAttributeValue(SHOW_DESCRIPTION)); }
    public String  getConclusions()                    { return unMakeCustomDescriptionElements(conclusionsRecommendationsElement);}
    public String  getRecommendations()                { return unMakeConclusions(conclusionsRecommendationsElement);}
    public void setShowRecommendationsConclusions(boolean value) { conclusionsRecommendationsElement.setAttribute(SHOW_DESCRIPTION, booleanToString(value)); }
    public void setConclusions                     (String s)     { makeCustomDescriptionElement(conclusionsRecommendationsElement,"CR", s); }   // watch the wording
    public void setRecommendations                 (String s)     { makeConclusions(conclusionsRecommendationsElement,"CR", s); }

    // exec summary:
    public boolean isShowExecutiveSummary() 
    { 
        if  (executiveSummaryElement.getAttributeValue(SHOW_DESCRIPTION) == null)
             return true; // default
        else return stringToBoolean(executiveSummaryElement.getAttributeValue(SHOW_DESCRIPTION));
    }
    public void    setShowExecutiveSummary  (boolean value) 
    {
        executiveSummaryElement.setAttribute(SHOW_DESCRIPTION, booleanToString(value));
    }
    public String  getExecutiveSummary() 
    { 
        return unMakeCustomDescriptionElements(executiveSummaryElement);
    }
    public void    setExecutiveSummary   (String s) 
    { 
        makeCustomDescriptionElement(executiveSummaryElement,EXECUTIVE_SUMMARY, s);
    }

    // sim-location:
    public boolean isShowScenarioLocationDescription() {return stringToBoolean(simulationLocationElement.getAttributeValue(SHOW_DESCRIPTION));}
    public void    setScenarioLocationIncluded  (boolean value) {simulationLocationElement.setAttribute(SHOW_DESCRIPTION, booleanToString(value));}
    public boolean isShowScenarioLocationImage()    {return stringToBoolean(simulationLocationElement.getAttributeValue(SHOW_IMAGE));}
    public void    setShowScenarioLocationImage     (boolean value) {simulationLocationElement.setAttribute(SHOW_IMAGE, booleanToString(value));}

    public String  getScenarioLocationDescription()        {return unMakeCustomDescriptionElements(simulationLocationElement);}
    public String  getScenarioLocationConclusions()     {return unMakeConclusions(simulationLocationElement);}
    public String  getScenarioLocationProductionNotes() {return unMakeProductionNotes(simulationLocationElement);}
    public String  getLocationImage()              {return unMakeImage(simulationLocationElement, LOCATION);}
    public String  getChartImage()                 {return unMakeImage(simulationLocationElement, CHART);}
    public void setScenarioLocationDescription  (String s)    {makeCustomDescriptionElement(simulationLocationElement, "SL", s);}
    public void setScenarioLocationConclusions  (String s)    {makeConclusions(simulationLocationElement, "SL", s);}
    public void setScenarioLocationProductionNotes(String s)  {makeProductionNotes(simulationLocationElement, "SL", s);}
    public void setLocationImage           (String s)    {replaceChildren(simulationLocationElement, makeImageElement(LOCATION, s)); }
    public void setChartImage              (String s)    {replaceChildren(simulationLocationElement, makeImageElement(CHART, s)); }

    public boolean isShowEntityParametersOverview() { return stringToBoolean(entityParametersElement.getAttributeValue(SHOW_OVERVIEW));}
    public boolean isShowEntityParametersTables()        { return stringToBoolean(entityParametersElement.getAttributeValue(SHOW_PARAMETER_TABLES)); }
    public void   setShowEntityParametersOverview    (boolean value) { entityParametersElement.setAttribute(SHOW_OVERVIEW, booleanToString(value)); }
    public void   setShowEntityParametersTables          (boolean value) { entityParametersElement.setAttribute("parameterTables", booleanToString(value)); }

    public String  getEntityParametersOverview()    { return unMakeCustomDescriptionElements(entityParametersElement);} // TODO check, apparent mismatch
    public String  getEntityParametersConclusions() { return unMakeConclusions(entityParametersElement);}               // TODO check, apparent mismatch
    public Vector<Object[]> getEntityParametersTables() {return unMakeParameterTables(entityParametersElement);}
    public void setEntityParametersOverview         (String s){ makeCustomDescriptionElement(entityParametersElement,"EP", s); }
    public void setEntityParametersConclusions      (String s){ makeConclusions(entityParametersElement,"EP", s); }

    public boolean isShowBehaviorDesignAnalysisDescriptions()             { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue(SHOW_DESCRIPTION));}
    public void   setShowBehaviorDesignAnalysisDescriptions(boolean value) { behaviorDescriptionsElement.setAttribute(SHOW_DESCRIPTION, booleanToString(value)); }

    public boolean isShowBehaviorDescriptions() { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue(SHOW_DESCRIPTION));}
    public boolean isShowEventGraphDetails()    { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue("showDetails"));}
    public boolean isShowEventGraphImage()     { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue("showImage"));}
    public void   setShowBehaviorDescriptions (boolean value) { behaviorDescriptionsElement.setAttribute(SHOW_DESCRIPTION, booleanToString(value)); }
    public void   setShowEventGraphDetails    (boolean value) { behaviorDescriptionsElement.setAttribute("showDtails", booleanToString(value)); }
    public void   setShowEventGraphImages     (boolean value) { behaviorDescriptionsElement.setAttribute("showImage", booleanToString(value)); }

    public String  getBehaviorDescription()             { return unMakeCustomDescriptionElements(behaviorDescriptionsElement); }
    public String  getBehaviorConclusions()          { return unMakeConclusions(behaviorDescriptionsElement); }
    public void    setBehaviorDescription (String s) { makeCustomDescriptionElement(behaviorDescriptionsElement,"BD", s); }
    public void    setBehaviorConclusions (String s) { makeConclusions(behaviorDescriptionsElement,"BD", s); }
    public List    getBehaviorList()                 { return unMakeBehaviorList(behaviorDescriptionsElement); }
    
    public boolean isShowAssemblyConfigurationDescription() { return stringToBoolean(assemblyConfigurationElement.getAttributeValue(SHOW_DESCRIPTION));}
    public boolean isShowAssemblyEntityDefinitionsTable()       { return stringToBoolean(assemblyConfigurationElement.getAttributeValue(SHOW_ENTITY_TABLE));}
    public boolean isShowAssemblyImage()             { return stringToBoolean(assemblyConfigurationElement.getAttributeValue(SHOW_IMAGE));}
    public void    setAssemblyConfigurationIncluded  (boolean value) { assemblyConfigurationElement.setAttribute(SHOW_DESCRIPTION, booleanToString(value));}
    public void    setShowAssemblyEntityDefinitionsTable        (boolean value) { assemblyConfigurationElement.setAttribute(SHOW_ENTITY_TABLE, booleanToString(value)); }
    public void    setShowAssemblyConfigurationImage      (boolean value) { assemblyConfigurationElement.setAttribute(SHOW_IMAGE, booleanToString(value)); }

    public String     getConfigurationComments()        {return unMakeCustomDescriptionElements(assemblyConfigurationElement);}
    public String[][] getAssemblyDesignEntityDefinitionsTable()     {return unMakeEntityTable();}
    public String     getAssemblyConfigurationConclusions()     {return unMakeConclusions(assemblyConfigurationElement);}
    public String     getAssemblyConfigurationProductionNotes() {return unMakeProductionNotes(assemblyConfigurationElement);}
    public String     getAssemblyImageLocation()                  {return unMakeImage(assemblyConfigurationElement, "Assembly");}

    public void    setAssemblyDesignConsiderations  (String s) { makeCustomDescriptionElement(assemblyConfigurationElement, "SC", s); }
    public void    setSimulationConfigurationEntityTable  (String s) { }; //todo//todo
    public void    setAssemblyConfigurationConclusions  (String s) { makeConclusions(assemblyConfigurationElement, "SC", s); }
    public void    setAssemblyConfigationProductionNotes(String s) {makeProductionNotes(assemblyConfigurationElement, "SC", s);}
    public void    setAssemblyImageLocation        (String s) {replaceChildren(assemblyConfigurationElement, makeImageElement("Assembly", s));}

    // stat results:
    // good
    public boolean isShowReplicationStatistics() { return stringToBoolean(statisticalResultsElement.getAttributeValue(REPLICATION_STATISTICS)); }
    public boolean isShowStatiisticalResultsDescriptionAnalysis()    { return stringToBoolean(statisticalResultsElement.getAttributeValue(SHOW_DESCRIPTION)); }
    public boolean isShowSummaryStatistics()     { return stringToBoolean(statisticalResultsElement.getAttributeValue(SUMMARY_STATISTICS)); }
    public boolean isShowStatisticsCharts()      { return stringToBoolean(statisticalResultsElement.getAttributeValue("charts")); }
    //todo later public boolean isOverlayStatsCharts()    { return stringToBoolean(statisticalResults.getAttributeValue("overlay")); }
    public void setShowReplicationStatistics   (boolean value) { statisticalResultsElement.setAttribute(REPLICATION_STATISTICS, booleanToString(value)); }
    public void setShowStatisticsDescriptionAnalysis      (boolean value) { statisticalResultsElement.setAttribute(SHOW_DESCRIPTION ,booleanToString(value)); }
    public void setShowSummaryStatistics       (boolean value) { statisticalResultsElement.setAttribute(SUMMARY_STATISTICS, booleanToString(value)); }
    public void setShowStatisticsCharts        (boolean value) { statisticalResultsElement.setAttribute("charts", booleanToString(value)); }
    //todo later public void setOverlayStatsCharts      (boolean value) { statisticalResults.setAttribute("overlay", booleanToString(bool)); }

    public String  getStatisticsDescription()        { return unMakeCustomDescriptionElements(statisticalResultsElement);}
    public String  getStatisticsConclusions()     { return unMakeConclusions(statisticalResultsElement);}
    public void setStatisticsDescription          (String s) { makeCustomDescriptionElement(statisticalResultsElement,"SR", s); }
    public void setStatisticsConclusions          (String s) { makeConclusions(statisticalResultsElement,"SR", s); }
    public String getStatisticsFilePath()         { return statisticalResultsElement.getAttributeValue("file"); }
    public List<Object>   getStatisticsReplicationsList() {return unMakeReplicationList(statisticalResultsElement);}
    public List<String[]> getStatisticsSummaryList()      {return unMakeStatisticsSummaryList(statisticalResultsElement);}

    public Map<String, AssemblyNode> getPclNodeCache() {
        return pclNodeCache;
    }

    public void setPclNodeCache(Map<String, AssemblyNode> pclNodeCache) {
        this.pclNodeCache = pclNodeCache;
    }

} // end class file AnalystReportModel.java
