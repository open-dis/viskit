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

import edu.nps.util.Log4jUtilities;
import edu.nps.util.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.apache.logging.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;

import viskit.util.EventGraphCache;
import viskit.ViskitGlobals;
import static viskit.ViskitGlobals.isFileReady;
import viskit.control.AssemblyControllerImpl;
import viskit.control.EventGraphController;
import viskit.doe.FileHandler;
import viskit.mvc.MvcAbstractModel;
import viskit.reports.HistogramChart;
import viskit.reports.LinearRegressionChart;
import viskit.util.XsltUtility;

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
    static final Logger LOG = Log4jUtilities.getLogger(AnalystReportModel.class);

    private boolean debug = false;

    /** UTH - Assembly file */
    private File assemblyFile;

    /** The viskit.reports.ReportStatisticsDOM object for this report */
    private Document statisticsReportDocument;
    private String   statisticsReportPath;

    /** The jdom.Document object that is used to build the report */
    private Document reportJdomDocument;

    /** The file name selected by the user from "SAVE AS" menu option */
    private String  analystReportFileXmlName;
    private File    analystReportFileXml;

    /** The root element of the report xml document */
    private Element rootElement;
    private Element exececutiveSummaryElement;
    private Element simulationLocationElement;
    private Element simulationConfigurationElement;
    private Element entityParametersElement;
    private Element behaviorDescriptionsElement;
    private Element statisticalResultsElement;
    private Element conclusionsRecommendationsElement;

    private JProgressBar progressBar;

    /** Must have the order of the PCL as input from AssemblyModel */
    private Map<String, AssemblyNode> pclNodeCache;

    /** <p>Build an AnalystReport object from an existing statisticsReport
     * document.  This is done from viskit.BasicAssembly via reflection.</p>
     * @param statisticsReportPath the path to the statistics generated report
     *        used by this Analyst Report
     * @param map the set of PCLs that have specific properties set for type statistic desired
     */
    public AnalystReportModel(String statisticsReportPath, Map<String, AssemblyNode> map)
    {        
        Document newDocument = null;
        setStatisticsReportPath(statisticsReportPath);
        try {
            newDocument = EventGraphCache.instance().loadXML(statisticsReportPath);
        }
        catch (Exception e) {
            LOG.error("Constructor exception reading {}", statisticsReportPath + " : " + e.getMessage());
        }
        if (newDocument == null)
            return; // statistics XML was not written to, not selected for recording

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
        this(xmlFile);

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
            LOG.error("Constructor error reading {}", newAnalystReportFileXml.getPath());
            return;
        }
        try {
            analystReportFileXml = newAnalystReportFileXml;
            parseXML(newAnalystReportFileXml);
        } 
        catch (Exception ex) {
            LOG.error("Constructor exception reading {}", newAnalystReportFileXml.getPath() + " : " + ex.getMessage());
        }
    }

    private void initializeDocument() {
        reportJdomDocument = new Document();
        rootElement = new Element("AnalystReport");
        reportJdomDocument.setRootElement(rootElement);

        fillDocument();
        setDefaultValues();
    }

    private void fillDocument() {
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
    public File writeToXMLFile(File file) throws Exception {
        if (file == null)
            file = writeToXMLFile();
        _writeCommon(file);
        return file;
    }

    /** @return the initial temp file to be saved for further post-processing
     * @throws java.lang.Exception general catchall
     */
    public File writeToXMLFile() throws Exception {
        return TempFileManager.createTempFile("AnalystReport", ".xml");
    }

    private void _writeCommon(File fil) {
        try {
            FileHandler.marshallJdom(fil, reportJdomDocument, false);
        } catch (IOException | JDOMException e) {
            LOG.error("Bad JDOM op {}: ", e);
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
        exececutiveSummaryElement         = rootElement.getChild("ExecutiveSummary");
        simulationLocationElement         = rootElement.getChild("Location");
        simulationConfigurationElement    = rootElement.getChild("SimulationConfiguration");
        entityParametersElement           = rootElement.getChild("EntityParameters");
        behaviorDescriptionsElement       = rootElement.getChild("BehaviorDescriptions");
        statisticalResultsElement         = rootElement.getChild("StatisticalResults");
        conclusionsRecommendationsElement = rootElement.getChild("ConclusionsRecommendations");
    }

    /**
     * Creates the root element for the analyst report
     */
    public void createHeader() {
        rootElement.setAttribute("name", "");
        rootElement.setAttribute("access", "");
        rootElement.setAttribute("author", "");
        rootElement.setAttribute("date", "");
    }

    /**
     * Populates the executive summary portion of the AnalystReport XML
     */
    public void createExecutiveSummary() {
        exececutiveSummaryElement = new Element("ExecutiveSummary");
        exececutiveSummaryElement.setAttribute("comments", "true"); // TODO description
        rootElement.addContent(exececutiveSummaryElement);
    }

    /** Creates the SimulationLocation portion of the analyst report XML */
    public void createSimulationLocation() {
        simulationLocationElement = new Element("Location");
        simulationLocationElement.setAttribute("comments", "true"); // TODO description
        simulationLocationElement.setAttribute("images", "true");
        makeComments(simulationLocationElement, "SL", "");
        makeProductionNotes(simulationLocationElement, "SL", "");
        makeConclusions(simulationLocationElement, "SL", "");
        rootElement.addContent(simulationLocationElement);
    }

    /** Creates the simulation configuration portion of the Analyst report XML */
    private void createSimulationConfiguration() {
        simulationConfigurationElement = new Element("SimulationConfiguration");
        simulationConfigurationElement.setAttribute("comments", "true"); // TODO description
        simulationConfigurationElement.setAttribute("image", "true");
        simulationConfigurationElement.setAttribute("entityTable", "true");
        makeComments(simulationConfigurationElement, "SC", ""); // TODO description
        makeProductionNotes(simulationConfigurationElement, "SC", "");
        makeConclusions(simulationConfigurationElement, "SC", "");
        if (assemblyFile != null) {
            simulationConfigurationElement.addContent(EventGraphCache.instance().getEntityTable());
        }

        rootElement.addContent(simulationConfigurationElement);
    }

    /** Creates the entity parameter section of this analyst report */
    private void createEntityParameters() {
        entityParametersElement = new Element("EntityParameters");
        entityParametersElement.setAttribute("comments", "true"); // TODO description
        entityParametersElement.setAttribute("parameterTables", "true");
        makeComments(entityParametersElement, "EP", "");
        makeConclusions(entityParametersElement, "EP", "");
        if (assemblyFile != null) {
            entityParametersElement.addContent(makeParameterTables());
        }

        rootElement.addContent(entityParametersElement);
    }

    /** Creates the behavior descriptions portion of the report */
    private void createBehaviorDescriptions() {
        behaviorDescriptionsElement = new Element("BehaviorDescriptions");
        behaviorDescriptionsElement.setAttribute("comments", "true"); // TODO description
        behaviorDescriptionsElement.setAttribute("descriptions", "true");
        behaviorDescriptionsElement.setAttribute("image", "true");
        behaviorDescriptionsElement.setAttribute("details", "true");
        makeComments(behaviorDescriptionsElement,"BD", "");
        makeConclusions(behaviorDescriptionsElement,"BD", "");

        behaviorDescriptionsElement.addContent(processBehaviors(true, true, true));

        rootElement.removeChild("BehaviorDescriptions");
        rootElement.addContent(behaviorDescriptionsElement);
    }

    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private void createStatisticsResults() 
    {
        statisticalResultsElement = new Element("StatisticalResults");
        rootElement.addContent(statisticalResultsElement);

        statisticalResultsElement.setAttribute("comments", "true"); // TODO description
        statisticalResultsElement.setAttribute("replicationStats", "true"); // TODO full name
        statisticalResultsElement.setAttribute("summaryStats", "true");     // TODO full name
        makeComments(statisticalResultsElement,"SR", "");
        makeConclusions(statisticalResultsElement,"SR", "");

        if (statisticsReportPath != null && statisticsReportPath.length() > 0) {
            statisticalResultsElement.setAttribute("file", statisticsReportPath);

            Element sumReport = new Element("SummaryReport");
            List<Element> itr = statisticsReportDocument.getRootElement().getChildren("SimEntity");
            List<Element> summItr;
            Element temp, summaryRecord, summStats;
            for (Element entity : itr) {
                temp = (Element) entity.clone();
                temp.removeChildren("SummaryReport");

                summStats = entity.getChild("SummaryReport");
                summItr = summStats.getChildren("Summary");
                for (Element temp2 : summItr) {
                    summaryRecord = new Element("SummaryRecord");
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
        List<Element> simEnts = repReports.getChildren("SimEntity");
        for (Element sEnt : simEnts) {

            se = new Vector<>(3);
            se.add(sEnt.getAttributeValue("name"));
            se.add(sEnt.getAttributeValue("property"));

            r = new Vector<>();
            List<Element> repLis = sEnt.getChildren("Replication");
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
    public List<String[]> unMakeStatsSummList(Element statisticalResults) {
        Vector<String[]> v = new Vector<>();

        Element sumReports = statisticalResults.getChild("SummaryReport");
        List<Element> recs = sumReports.getChildren("SummaryRecord");
        String[] sa;
        for (Element rec : recs) {
            sa = new String[8];
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
        conclusionsRecommendationsElement = new Element("ConclusionsRecommendations");
        conclusionsRecommendationsElement.setAttribute("comments", "true"); // TODO description
        makeComments(conclusionsRecommendationsElement, "CR", "");
        makeConclusions(conclusionsRecommendationsElement, "CR", "");
        rootElement.addContent(conclusionsRecommendationsElement);
    }

    /** Creates Behavior definition references in the analyst report template
     * @param descript if true, show description text
     * @param image if true, show all images
     * @param details if true, show all details text
     * @return a table of scenario Event Graph Behaviors
     */
    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element processBehaviors(boolean descript, boolean image, boolean details) {
        Element behaviorList = new Element("BehaviorList");
        Element behavior, localRootElement, description, param, stvar, evtGraphImage;
        String descriptText, imgPath;
        Document tmp;
        List<Element> lre, lre2;
        for (int i = 0; i < EventGraphCache.instance().getEventGraphNamesList().size(); i++) {
            behavior = new Element("Behavior");
            behavior.setAttribute("name", EventGraphCache.instance().getEventGraphNamesList().get(i));

            if (descript) {
                tmp = EventGraphCache.instance().loadXML(EventGraphCache.instance().getEventGraphFilesList().get(i));
                localRootElement = tmp.getRootElement();

                // prevent returning a null if there was no attribute value
                descriptText = (localRootElement.getChildText("Comment") == null) ? "no comment provided" : localRootElement.getChildText("Comment");

                description = new Element("description");
                description.setAttribute("text", descriptText);
                behavior.addContent(description);

                if (details) {
                    lre = localRootElement.getChildren("Parameter");
                    for (Element temp : lre) {
                        param = new Element("parameter");
                        param.setAttribute("name", temp.getAttributeValue("name"));
                        param.setAttribute("type", temp.getAttributeValue("type"));

                        // The data "null" is not legal for a JDOM attribute
                        param.setAttribute("description", (temp.getChildText("Comment") == null) ? "no comment provided" : temp.getChildText("Comment"));
                        behavior.addContent(param);
                    }
                    lre2 = localRootElement.getChildren("StateVariable");
                    for (Element temp : lre2) {
                        stvar = new Element("stateVariable");
                        stvar.setAttribute("name", temp.getAttributeValue("name"));
                        stvar.setAttribute("type", temp.getAttributeValue("type"));

                        // The data "null" is not legal for a JDOM attribute
                        stvar.setAttribute("description", (temp.getChildText("Comment") == null) ? "no comment provided" : temp.getChildText("Comment"));
                        behavior.addContent(stvar);
                    }
                }
            }
            if (image) {
                evtGraphImage = new Element("EventGraphImage");

                // Set relative path only
                imgPath = EventGraphCache.instance().getEventGraphImageFilesList().get(i).getPath();
                imgPath = imgPath.substring(imgPath.indexOf("images"), imgPath.length());
                evtGraphImage.setAttribute("dir", imgPath);
                behavior.addContent(evtGraphImage);
            }
            behaviorList.addContent(behavior);
        }

        return behaviorList;
    }

    // TODO: Fix generics: version of JDOM does not support generics
    @SuppressWarnings("unchecked")
    List unMakeBehaviorList(Element localRoot) {
        Vector v = new Vector();

        Element listEl = localRoot.getChild("BehaviorList");
        if (listEl != null) {
            List<Element> behElms = listEl.getChildren("Behavior");
            String nm, desctxt, pnm, pty, pdsc, snm, sty, sdsc;
            String[] pa, sa;
            Vector<Object> b;
            Vector<String[]> p, s;
            Element desc, evtGrImg;
            List<Element> parms, stvars;
            for (Element behavior : behElms) {

                b = new Vector<>();
                nm = behavior.getAttributeValue("name");
                b.add(nm);

                desc = behavior.getChild("description");
                desctxt = desc.getAttributeValue("text");
                b.add(desctxt);

                parms = behavior.getChildren("parameter");

                p = new Vector<>();
                for (Element param : parms) {
                    pnm = param.getAttributeValue("name");
                    pty = param.getAttributeValue("type");
                    pdsc = param.getAttributeValue("description");
                    pa = new String[]{pnm, pty, pdsc};
                    p.add(pa);
                }
                b.add(p);

                stvars = behavior.getChildren("stateVariable");

                s = new Vector<>();
                for (Element svar : stvars) {
                    snm = svar.getAttributeValue("name");
                    sty = svar.getAttributeValue("type");
                    sdsc = svar.getAttributeValue("description");
                    sa = new String[]{snm, sty, sdsc};
                    s.add(sa);
                }
                b.add(s);

                evtGrImg = behavior.getChild("EventGraphImage");
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
        Vector<Object[]> v = new Vector<>(lis.size());   // list of entpartab elms
        List<Element> lis_0, lis_1;
        Vector<Object[]> v_0, v_1;
        String name, val;
        for(Element e_0 : lis) {
            lis_0 = e_0.getChildren();       //list of parts: class/id/phys/dynam
            v_0 = new Vector<>(lis_0.size());
            for(Element e_1 : lis_0) {
                lis_1 = e_1.getChildren("parameter");     // list of param elms

                v_1 = new Vector<>(lis_1.size());
                for(Element e_2 : lis_1) {
                    name = e_2.getAttributeValue("name");
                    val  = e_2.getAttributeValue("value");
                    v_1.add(new String[]{name, val});
                }
                v_0.add(new Object[]{e_1.getName(),v_1});
            }
            v.add(new Object[]{e_0.getAttributeValue("name"),v_0});
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
        return makeTablesCommon("ParameterTables");
    }

    // TODO: This version JDOM does not support generics
    @SuppressWarnings("unchecked")
    private Element makeTablesCommon(String tableName) {
        Element table = new Element(tableName);
        Element localRootElement = EventGraphCache.instance().getAssemblyDocument().getRootElement();
        List<Element> simEntityList = localRootElement.getChildren("SimEntity");
        String entityName;
        List<Element> entityParams;
        for (Element temp : simEntityList) {
            entityName = temp.getAttributeValue("name");
            entityParams = temp.getChildren("MultiParameter");
            for (Element param : entityParams) {
                if (param.getAttributeValue("type").equals("diskit.SMAL.EntityDefinition")) {
                    table.addContent(extractSMAL(entityName, param));
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
        Element table = new Element("EntityParameterTable");
        ElementFilter multiParam = new ElementFilter("MultiParameter");
        Iterator<Element> itr = entityDef.getDescendants(multiParam);
        table.setAttribute("name", entityName);
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
        Element param;
        for (Element temp : dataList) {
            if (!temp.getAttributeValue("value").equals("0")) {
                param = new Element("parameter");
                param.setAttribute("name", temp.getAttributeValue("name"));
                param.setAttribute("value", temp.getAttributeValue("value"));

                tableEntry.addContent(param);
            }
        }
        return tableEntry;
    }

    // TODO: Version 1.1 JDOM does not yet support generics
    @SuppressWarnings("unchecked")
    public String[][] unMakeEntityTable() 
    {
        String[][] emptyResult = new String[0][0];
        if (simulationConfigurationElement == null)
        {
            LOG.error("AnalystReportModel unMakeEntityTable() simulationConfigurationElement is null");
            return emptyResult;
        }
        if (!simulationConfigurationElement.getChildren().isEmpty())
        {
            Element element = simulationConfigurationElement.getChild("EntityTable");
            if (element == null)
            {
                LOG.error("AnalystReportModel unMakeEntityTable() simulationConfigurationElement EntityTable is null");
                return emptyResult;
            }
            List<Element> elementsList = element.getChildren("SimEntity");

            String[][] sa = new String[elementsList.size()][2];
            int i = 0;
            for(Element e : elementsList) {
                sa[i]  [0] = e.getAttributeValue("name");
                sa[i++][1] = e.getAttributeValue("fullyQualifiedName");
            }
            return sa;
        }
        else return emptyResult;
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
    private Element makeReplicationReport() 
    {
        Element repReports = new Element("ReplicationReports");
        List<Element> simEntities = statisticsReportDocument.getRootElement().getChildren("SimEntity");

        // variables for JFreeChart construction
        HistogramChart histogramChart = new HistogramChart();
        LinearRegressionChart linearRegressionChart = new LinearRegressionChart();
        String chartTitle, axisLabel, typeStat = "", dataPointProperty;
        List<Element> dataPoints, replicationReports, replications;
        boolean isCount;
        Object obj;
        Element entity, histogramChartURL, linearRegressionChartURL, repRecord;
        double[] data;
        int idx;
        for (Element simEntity : simEntities) {
            dataPoints = simEntity.getChildren("DataPoint");
            for (Element dataPoint : dataPoints) {
                dataPointProperty = dataPoint.getAttributeValue("property");
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
                entity = new Element("SimEntity");
                entity.setAttribute("name", simEntity.getAttributeValue("name"));
                entity.setAttribute("property", dataPointProperty);
                replicationReports = dataPoint.getChildren("ReplicationReport");

                // Chart title and label
                chartTitle = simEntity.getAttributeValue("name");
                axisLabel  = dataPoint.getAttributeValue("property") ;

                for (Element replicationReport : replicationReports) {
                    replications = replicationReport.getChildren("Replication");

                    // Create a data set instance and histogramChart for each replication report
                    data = new double[replicationReport.getChildren().size()];
                    idx = 0;
                    for (Element replication : replications) {
                        repRecord = new Element("Replication");
                        repRecord.setAttribute("number", replication.getAttributeValue("number"));
                        repRecord.setAttribute("count", replication.getAttributeValue("count"));
                        repRecord.setAttribute("minObs", replication.getAttributeValue("minObs"));
                        repRecord.setAttribute("maxObs", replication.getAttributeValue("maxObs"));
                        repRecord.setAttribute("mean", replication.getAttributeValue("mean"));
                        repRecord.setAttribute("stdDeviation", replication.getAttributeValue("stdDeviation"));
                        repRecord.setAttribute("variance", replication.getAttributeValue("variance"));
                        entity.addContent(repRecord);

                        // Add the raw count, or mean of replication data to the chart generators
                        LOG.debug(replication.getAttributeValue(typeStat));
                        data[idx] = Double.parseDouble(replication.getAttributeValue(typeStat));
                        idx++;
                    }

                    histogramChartURL = new Element("HistogramChart");
                    linearRegressionChartURL = new Element("LinearRegressionChart");

                    histogramChartURL.setAttribute("dir", histogramChart.createChart(chartTitle, axisLabel, data));
                    entity.addContent(histogramChartURL);

                    // data[] must be > than length 1 for scatter regression
                    if (data.length > 1) {
                        linearRegressionChartURL.setAttribute("dir", linearRegressionChart.createChart(chartTitle, axisLabel, data));
                        entity.addContent(linearRegressionChartURL);
                    }

                    repReports.addContent(entity);
                }
            }
        }
        return repReports;
    }

    /**
     * Converts boolean input into a 'true'/'false' string representation for
     * use as an attribute value in the Analyst report XML.
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

        // Set relative path only
        image.setAttribute("dir", dir.substring(dir.indexOf("images"), dir.length()));
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

    /** @param commentTag the comment Element, which actually contains description
     * @param commentText the comment text, which actually contains description
     * @return the Comments Element
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

    /** @param e the Element to extract information from
     * @return a String object of the Element's contents
     */
    public String unMakeConclusions(Element e) {
        return _unMakeContent(e, "Conclusions");
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
        return _makeContent(productionNotesTag, "ProductionNotes", productionNotesText);
    }

    /**
     * Creates a standard 'Production Notes' element used by all sections of the
     * report to add production notes
     *
     * @param parent the parent element to add content too
     * @param productionNotesTag the tag used to identify unique production notes (used by XSLT)
     * @param productionNotesText author's text block
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
        Object o;
        Element celem;
        for (Iterator itr = content.iterator(); itr.hasNext();) {
            o = itr.next();
            if (!(o instanceof Element)) {
                continue;
            }
            celem = (Element) o;
            if (celem.getName().endsWith(suffix)) {
                return celem.getAttributeValue(attrName);
            }
        }
        return "";
    }

    private void replaceChild(Element parent, Element child) {
        if (parent == null)
            return;

        parent.removeChildren(child.getName());
        parent.addContent(child);
    }

    /**
     * TODO: Change this to put in appropriate sample text
     */
    private void setDefaultValues() {
        //Header values
        setReportName("***ENTER REPORT TITLE HERE***");
        setAccessLabel("***ENTER ACCESS LABEL HERE***");
        setAuthor("***ENTER NAME OF AUTHOR HERE***");
        setDateOfReport(DateFormat.getInstance().format(new Date()));

        //Executive Summary values
        setExecutiveSummaryComments(true);
        setExecutiveSummary("***ENTER EXECUTIVE SUMMARY HERE***");

        //SimulationLocation Values
        setPrintSimulationLocationComments(true);
        setPrintSimulationLocationImage(true);
        setSimulationLocationDescription("***ENTER SIMULATION LOCATION DESCRIPTION HERE***");
        setSimulationLocationConclusions("***ENTER SIMULATION LOCATION CONCLUSIONS HERE***");
        setSimulationLocationProductionNotes("***ENTER SIMULATION PRODUCTION NOTES HERE***");
        //setChartImage(""); // TODO:  generate nauthical chart image, set file location

        //Simulation Configuration Values
        setPrintSimulationConfigurationComments(true);
        setPrintAssemblyImage(true);
        setPrintEntityTable(true);
        setSimulationConfigurationDescription("***ENTER ASSEMBLY CONFIGURATION DESCRIPTION HERE***");
        setSimulationConfigurationConclusions("***ENTER ASSEMBLY CONFIGURATION CONCLUSIONS HERE***");
        setSimulationConfigationProductionNotes("***ENTER ASSEMBLY CONFIGURATION PRODUCTION NOTES HERE***");

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
        setPrintStatisticsComments(true);
        setPrintReplicationStatistics(true);
        setPrintSummaryStatistics(true);
        setStatisticsDescription("***ENTER STATISTICAL RESULTS DESCRIPTION HERE***");
        setStatisticsConclusions("***ENTER STATISTICAL RESULTS CONCLUSIONS HERE***");

        //Recommendations/Conclusions
        setPrintRecommendationsConclusions(true);
        setConclusions    ("***ENTER ANALYST CONCLUSIONS HERE***");
        setRecommendations("***ENTER RECOMMENDATIONS FOR FUTURE WORK HERE***");
    }

    public boolean isDebug()                     {return debug;}

    public Document   getReportJdomDocument()    {return reportJdomDocument;}
    public Document   getStatisticsReport()      {return statisticsReportDocument;}
    public Element    getRootElement()           {return rootElement;}
    public String     getAnalystReportFileXmlName() {return analystReportFileXmlName;}
    public File       getAnalystReportXmlFile()     {return analystReportFileXml;}
    public String     getAuthor()                {return rootElement.getAttributeValue("author");}
    public String     getAccess()                {return rootElement.getAttributeValue("access");}
    public String     getDateOfReport()          {return rootElement.getAttributeValue("date");}
    public String     getReportName()            {return rootElement.getAttributeValue("name");}

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
        if (simulationConfigurationElement == null)
            return; // stats report not set for recording

        simulationConfigurationElement.addContent(EventGraphCache.instance().getEntityTable());
        entityParametersElement.addContent(makeParameterTables());
        createBehaviorDescriptions();
    }

    private boolean reportReady = false;

    public boolean isReportReady() {
        return reportReady;
    }

    public void setReportReady(boolean b) {
        reportReady = b;
    }

    /** Post Analyst Report processing steps to take */
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

        announceAnalystReportReadyToView();
        reportReady = true;
    }

    /** Utility method used here to invoke the capability to capture all Event
     * Graph images of which are situated in a particular Assembly File.  These
     * PNGs will be dropped into ${viskitProject}/AnalystReports/images/EventGraphs </p>
     */
    private void captureEventGraphImages() {
        EventGraphCache evc = EventGraphCache.instance();
        ((EventGraphController)ViskitGlobals.instance().getEventGraphController()).captureEventGraphImages(
                evc.getEventGraphFilesList(),
                evc.getEventGraphImageFilesList());
    }

    /** Utility method used here to invoke the capability to capture the
     * Assembly image of the loaded Assembly File.  This PNG will be dropped
     * into ${viskitProject}/AnalystReports/images/Assemblies </p>
     */
    private void captureAssemblyImage() {
        String assemblyFilePath = assemblyFile.getPath();
        assemblyFilePath = assemblyFilePath.substring(assemblyFilePath.indexOf("Assemblies"), assemblyFilePath.length());
        File assemblyImage = new File(
                ViskitGlobals.instance().getViskitProject().getAnalystReportImagesDirectory(),
                assemblyFilePath + ".png");

        if (!assemblyImage.getParentFile().exists())
             assemblyImage.mkdirs();

        setAssemblyImageLocation(assemblyImage.getPath());
        ((AssemblyControllerImpl)ViskitGlobals.instance().getAssemblyController()).captureAssemblyImage(
                assemblyImage);
    }

    private void announceAnalystReportReadyToView()
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
        
        ViskitGlobals.instance().selectAnalystReportTab();
        popupTitle = "View HTML Analyst Report?";
        message =
                "<html><body>" +
                "<p align='center'>View HTML Analyst Report</p><br />" +
                "<p align='center'>or simply continue your analysis?</p><br />";
        int returnValue = ViskitGlobals.instance().getMainFrame().genericAsk2Buttons(popupTitle, message, 
                "View HTML", "Continue Analysis");
        
        if  (returnValue == 0) // yes, build and show report
        {
            String htmlFilePath = new String();
        
            try
            {                
                htmlFilePath = getAnalystReportXmlFile().getCanonicalPath();
            }
            catch (IOException ioe)
            {
                LOG.error("Trying to view HTML report from XML upon first arrival", ioe);
            }
            // change file extension. remove timestamp for HTML file path
            htmlFilePath = htmlFilePath.substring(0,htmlFilePath.indexOf("_AnalystReport")) + "_AnalystReport.html";
            if (htmlFilePath.startsWith("."))
                htmlFilePath = htmlFilePath.substring(2); // possible problem
            XsltUtility.runXslt(getAnalystReportXmlFile().getAbsolutePath(),
                htmlFilePath, "config/AnalystReportXMLtoHTML.xslt");
            ViskitGlobals.instance().getAnalystReportController().showHtmlViewer(htmlFilePath); // TODO show HTML, need filename
//             ViskitGlobals.instance().getAnalystReportController().generateHtmlReport(); // too far back in workflow
        }
        // TODO fix other conversion HTML filenames to match this abbrieviated form
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
                                              { this.analystReportFileXmlName = newAnalystReportFileName; }
    public void setAnalystReportFile          (File newAnalystReportFile) 
                                              { this.analystReportFileXml     = newAnalystReportFile; }
    public void setStatisticsReportDocument   (Document newStatisticsReportDocument)      
                                              { this.statisticsReportDocument = newStatisticsReportDocument; }
    public void setStatisticsReportPath       (String filename)           { this.statisticsReportPath = filename; }
    public void setAuthor                     (String s) { rootElement.setAttribute("author", s); };
    public void setAccessLabel                (String s) { rootElement.setAttribute("access", s);}
    public void setDateOfReport               (String s) { rootElement.setAttribute("date", s);}
    public void setDebug                      (boolean bool) { this.debug = bool; }
    public void setReportName                 (String s) { rootElement.setAttribute("name", s); }

    public boolean isPrintRecommendationsConclusions() { return stringToBoolean(conclusionsRecommendationsElement.getAttributeValue("comments")); } // TODO description
    public String  getConclusions()                    { return unMakeComments(conclusionsRecommendationsElement);}
    public String  getRecommendations()                { return unMakeConclusions(conclusionsRecommendationsElement);}
    public void setPrintRecommendationsConclusions(boolean bool) { conclusionsRecommendationsElement.setAttribute("comments", booleanToString(bool)); } // TODO description
    public void setConclusions                     (String s)     { makeComments(conclusionsRecommendationsElement,"CR", s); }   // watch the wording
    public void setRecommendations                 (String s)     { makeConclusions(conclusionsRecommendationsElement,"CR", s); }

    // exec summary:
    // good
    public boolean isExecutiveSummaryComments() { return stringToBoolean(exececutiveSummaryElement.getAttributeValue("comments"));} // TODO description
    public void    setExecutiveSummaryComments  (boolean bool) {exececutiveSummaryElement.setAttribute("comments", booleanToString(bool));} // TODO description
    public String  getExecutiveSummary() { return unMakeComments(exececutiveSummaryElement);}
    public void    setExecutiveSummary   (String s) { makeComments(exececutiveSummaryElement,"ES", s);} // TODO description

    // sim-location:
    // good
    public boolean isPrintSimulationLocationComments() {return stringToBoolean(simulationLocationElement.getAttributeValue("comments"));} // TODO description
    public void    setPrintSimulationLocationComments  (boolean bool) {simulationLocationElement.setAttribute("comments", booleanToString(bool));} // TODO description
    public boolean isPrintSimulationLocationImage()    {return stringToBoolean(simulationLocationElement.getAttributeValue("images"));}
    public void    setPrintSimulationLocationImage     (boolean bool) {simulationLocationElement.setAttribute("images", booleanToString(bool));}

    public String  getSimulationLocationComments()        {return unMakeComments(simulationLocationElement);}
    public String  getSimulationLocationConclusions()     {return unMakeConclusions(simulationLocationElement);}
    public String  getSimulationLocationProductionNotes() {return unMakeProductionNotes(simulationLocationElement);}
    public String  getLocationImage()              {return unMakeImage(simulationLocationElement, "Location");}
    public String  getChartImage()                 {return unMakeImage(simulationLocationElement, "Chart");}

    public void setSimulationLocationDescription  (String s)    {makeComments(simulationLocationElement, "SL", s);}
    public void setSimulationLocationConclusions  (String s)    {makeConclusions(simulationLocationElement, "SL", s);}
    public void setSimulationLocationProductionNotes(String s)  {makeProductionNotes(simulationLocationElement, "SL", s);}
    public void setLocationImage           (String s)    {replaceChild(simulationLocationElement, makeImage("Location", s)); }
    public void setChartImage              (String s)    {replaceChild(simulationLocationElement, makeImage("Chart", s)); }

    // entity-parameters
    //good
    public boolean isPrintParameterComments() { return stringToBoolean(entityParametersElement.getAttributeValue("comments"));} // TODO description
    public boolean isPrintParameterTable()    { return stringToBoolean(entityParametersElement.getAttributeValue("parameterTables")); }
    public void setPrintParameterComments   (boolean bool) { entityParametersElement.setAttribute("comments", booleanToString(bool)); } // TODO description
    public void setPrintParameterTable      (boolean bool) { entityParametersElement.setAttribute("parameterTables", booleanToString(bool)); }

    public String  getParameterComments()    { return unMakeComments(entityParametersElement);} // TODO description
    public String  getParameterConclusions() { return unMakeConclusions(entityParametersElement);}
    public Vector<Object[]> getParameterTables() {return unMakeParameterTables(entityParametersElement);}
    public void setParameterDescription         (String s){ makeComments(entityParametersElement,"EP", s); }
    public void setParameterConclusions      (String s){ makeConclusions(entityParametersElement,"EP", s); }

    // behavior descriptions:
    //good
    public boolean isPrintBehaviorDefComments()  { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue("comments"));} // TODO description
    public void setPrintBehaviorDefComments(boolean bool) { behaviorDescriptionsElement.setAttribute("comments", booleanToString(bool)); } // TODO description

    public boolean isPrintBehaviorDescriptions() { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue("descriptions"));}
    public boolean isPrintEventGraphDetails()    { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue("details"));}
    public boolean isPrintEventGraphImages()     { return stringToBoolean(behaviorDescriptionsElement.getAttributeValue("image"));}
    public void setPrintBehaviorDescriptions(boolean bool) { behaviorDescriptionsElement.setAttribute("descriptions", booleanToString(bool)); }
    public void setPrintEventGraphDetails    (boolean bool) { behaviorDescriptionsElement.setAttribute("details", booleanToString(bool)); }
    public void setPrintEventGraphImages     (boolean bool) { behaviorDescriptionsElement.setAttribute("image", booleanToString(bool)); }

    public String  getBehaviorComments()         { return unMakeComments(behaviorDescriptionsElement); }
    public String  getBehaviorConclusions()      { return unMakeConclusions(behaviorDescriptionsElement); }
    public void setBehaviorDescription         (String s) { makeComments(behaviorDescriptionsElement,"BD", s); }
    public void setBehaviorConclusions      (String s) { makeConclusions(behaviorDescriptionsElement,"BD", s); }
    public List getBehaviorList()          { return unMakeBehaviorList(behaviorDescriptionsElement); }
    // sim-config:
    //good
    public boolean isPrintSimulationConfigurationComments() { return stringToBoolean(simulationConfigurationElement.getAttributeValue("comments"));} // TODO description
    public boolean isPrintEntityTable()       { return stringToBoolean(simulationConfigurationElement.getAttributeValue("entityTable"));}
    public boolean isPrintAssemblyImage()     { return stringToBoolean(simulationConfigurationElement.getAttributeValue("image"));}
    public void    setPrintSimulationConfigurationComments  (boolean bool) { simulationConfigurationElement.setAttribute("comments", booleanToString(bool));} // TODO description
    public void    setPrintEntityTable        (boolean bool) { simulationConfigurationElement.setAttribute("entityTable", booleanToString(bool)); }
    public void    setPrintAssemblyImage      (boolean bool) { simulationConfigurationElement.setAttribute("image", booleanToString(bool)); }

    public String  getSimulationConfigurationComments()        {return unMakeComments(simulationConfigurationElement);}
    public String[][]  getSimulationConfigurationEntityTable() {return unMakeEntityTable();}
    public String  getSimulationConfigurationConclusions()     {return unMakeConclusions(simulationConfigurationElement);}
    public String  getSimulationConfigurationProductionNotes() {return unMakeProductionNotes(simulationConfigurationElement);}
    public String  getAssemblyImageLocation()    {return unMakeImage(simulationConfigurationElement, "Assembly");}

    public void    setSimulationConfigurationDescription  (String s) { makeComments(simulationConfigurationElement, "SC", s); }
    public void    setSimConfigurationEntityTable         (String s) { }; //todo//todo
    public void    setSimulationConfigurationConclusions  (String s) { makeConclusions(simulationConfigurationElement, "SC", s); }
    public void    setSimulationConfigationProductionNotes(String s) {makeProductionNotes(simulationConfigurationElement, "SC", s);}
    public void    setAssemblyImageLocation        (String s) {replaceChild(simulationConfigurationElement, makeImage("Assembly", s));}

    // stat results:
    // good
    public boolean isPrintReplicationStatistics() { return stringToBoolean(statisticalResultsElement.getAttributeValue("replicationStats")); }
    public boolean isPrintStatisticsComments()    { return stringToBoolean(statisticalResultsElement.getAttributeValue("comments")); } // TODO description
    public boolean isPrintSummaryStatistics()     { return stringToBoolean(statisticalResultsElement.getAttributeValue("summaryStats")); }
    public boolean isPrintStatisticsCharts()      { return stringToBoolean(statisticalResultsElement.getAttributeValue("charts")); }
    //todo later public boolean isOverlayStatsCharts()    { return stringToBoolean(statisticalResults.getAttributeValue("overlay")); }
    public void setPrintReplicationStatistics   (boolean bool) { statisticalResultsElement.setAttribute("replicationStats", booleanToString(bool)); }
    public void setPrintStatisticsComments      (boolean bool) { statisticalResultsElement.setAttribute("comments", booleanToString(bool)); } // TODO description
    public void setPrintSummaryStatistics       (boolean bool) { statisticalResultsElement.setAttribute("summaryStats", booleanToString(bool)); }
    public void setPrintStatsCharts        (boolean bool) { statisticalResultsElement.setAttribute("charts", booleanToString(bool)); }
    //todo later public void setOverlayStatsCharts      (boolean bool) { statisticalResults.setAttribute("overlay", booleanToString(bool)); }

    public String  getStatsComments()        { return unMakeComments(statisticalResultsElement);}
    public String  getStatsConclusions()     { return unMakeConclusions(statisticalResultsElement);}
    public void setStatisticsDescription          (String s) { makeComments(statisticalResultsElement,"SR", s); }
    public void setStatisticsConclusions          (String s) { makeConclusions(statisticalResultsElement,"SR", s); }
    public String getStatsFilePath()         { return statisticalResultsElement.getAttributeValue("file"); }
    public List<Object> getStatisticsReplicationsList() {return unMakeReplicationList(statisticalResultsElement);}
    public List<String[]> getStastSummaryList() {return unMakeStatsSummList(statisticalResultsElement);}

    public Map<String, AssemblyNode> getPclNodeCache() {
        return pclNodeCache;
    }

    public void setPclNodeCache(Map<String, AssemblyNode> pclNodeCache) {
        this.pclNodeCache = pclNodeCache;
    }

} // end class file AnalystReportModel.java
