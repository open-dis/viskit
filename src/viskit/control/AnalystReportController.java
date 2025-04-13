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
package viskit.control;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import static viskit.ViskitStatics.isFileReady;
import viskit.mvc.MvcAbstractController;
import viskit.model.AnalystReportModel;
import viskit.util.XsltUtility;
import viskit.view.AnalystReportViewFrame;
import static viskit.ViskitProject.VISKIT_CONFIG_DIRECTORY;

/** A controller for the analyst report panel.  All functions are to be
 * performed here vice the view.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.AnalystReportController">Terry Norbraten, NPS MOVES</a>
 * @version $Id$
 */
public class AnalystReportController extends MvcAbstractController
{
    static final Logger LOG = LogManager.getLogger();

    private static AnalystReportViewFrame analystReportViewFrame;
    private File   currentAssemblyFile;
    private AnalystReportModel analystReportModel;

    /** Constructor creates a new instance of AnalystReportController */
    public AnalystReportController() 
    {
        // initializations go here
    }

    /** Called from the InternalSimulationRunner when the temp Analyst report is
     * filled out and ready to copy from a temp to a permanent directory
     *
     * @param xmlFilePath the path to the temp Analyst Report that will be copied
     */
    public void setReportXML(String xmlFilePath) 
    {
        LOG.debug("Path of setReportXML Analyst Report: " + xmlFilePath);
        File xmlSourceFile = new File(xmlFilePath);

        File analystReportDirectory = ViskitGlobals.instance().getViskitProject().getAnalystReportsDirectory();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        String dateOutput = formatter.format(new Date()); // today, now

        String userName = System.getProperty("user.name");
        
        String assemblyName = ViskitGlobals.instance().getActiveAssemblyModel().getName();
        if ((assemblyName == null) || assemblyName.isBlank())
             assemblyName = currentAssemblyFile.getName().substring(0,currentAssemblyFile.getName().lastIndexOf(".xml"));
        if (((assemblyName == null) || assemblyName.isBlank()) && (getAnalystReportModel() != null))
            assemblyName = getAnalystReportModel().getReportName();
        if ((assemblyName == null) || assemblyName.isBlank())
        {
            assemblyName = "ERROR_AssemblyNameNotFound";
            LOG.error("setReportXML unable to find assemblyName");
        }
        
        String outputFilenameDated = (assemblyName + "_AnalystReport_" + dateOutput + ".xml");
        String outputFilename      = (assemblyName + "_AnalystReport"               + ".xml");

        File analystReportXmlFile = new File(analystReportDirectory, outputFilename);
        
        LOG.info("xmlSourceFile.getAbsolutePath()=\n      " + xmlSourceFile.getAbsolutePath());
        isFileReady(xmlSourceFile);
        try {
            Files.copy(xmlSourceFile.toPath(), analystReportXmlFile.toPath());
        } 
        catch (IOException ioe)
        {
            LOG.debug(ioe);  // typically this file already exists, authors are progressively/iteratively editing it
        }
        LOG.info("analystReportXmlFile.toPath()=\n      " + analystReportXmlFile.getAbsolutePath());
        isFileReady(xmlSourceFile);
        if (getAnalystReportModel() == null)
        {
            analystReportModel =  ViskitGlobals.instance().getAnalystReportModel();
        }
        if (getAnalystReportModel() == null)
        {
            LOG.info("setReportXML() creating analystReportModel");
            analystReportModel =  new AnalystReportModel(analystReportXmlFile);
        }

        if (analystReportViewFrame == null) {
            analystReportViewFrame = ViskitGlobals.instance().getAnalystReportViewFrame();
        }

        buildAnalystReport(analystReportXmlFile);
    }

    JTabbedPane mainTabbedPane;
    int mainTabbedPaneIdx;

    /**
     * Sets the Analyst report panel
     * @param tabbedPane our Analyst report panel parent
     * @param idx the index to retrieve the Analyst report panel
     */
    public void setMainTabbedPane(JComponent tabbedPane, int idx) {
        this.mainTabbedPane = (JTabbedPane) tabbedPane;
        mainTabbedPaneIdx = idx;
    }
    
    /** method name for reflection use */
    public static final String METHOD_openAnalystReportXML = "openAnalystReportXML";

    public void openAnalystReportXML() 
    {
        ViskitGlobals.instance().selectAnalystReportTab();
        
        if ((analystReportViewFrame != null) && analystReportViewFrame.isReportDirty()) 
        {
            int result = JOptionPane.showConfirmDialog(analystReportViewFrame,
                    "Save current simulation data and Analyst Report annotations?",
                    "Confirm",
                    JOptionPane.WARNING_MESSAGE);
            switch (result) {
                case JOptionPane.OK_OPTION:
                    saveReport();
                    break;
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.NO_OPTION:
                default:
                    break;
            }
        }
        File analystReportDirectory = ViskitGlobals.instance().getViskitProject().getAnalystReportsDirectory();
        JFileChooser openAnalystReportChooser = new JFileChooser(analystReportDirectory);
        openAnalystReportChooser.setDialogTitle("Open Analyst Report XML");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst Report data files (.xml)", "xml");
        openAnalystReportChooser.setFileFilter(filter);
        openAnalystReportChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnValue = openAnalystReportChooser.showOpenDialog(analystReportViewFrame);
        if (returnValue != JFileChooser.APPROVE_OPTION) {
            return;
        }

        openAnalystReport(openAnalystReportChooser.getSelectedFile());
    }

    public void setCurrentAssemblyFile(File newAssemblyFile) 
    {
        if (getAnalystReportModel() == null)
        {
            LOG.error("setCurrentAssemblyFile() error, analystReportModel is null");
        }
        else if (newAssemblyFile == null)
        {
            LOG.error("setCurrentAssemblyFile() error, received null file");
        }
        else if (!newAssemblyFile.exists())
        {
            LOG.error("setCurrentAssemblyFile(" + newAssemblyFile.getName() + ") error, file does not exist");
        }
        else
        {
            currentAssemblyFile = newAssemblyFile;
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_saveAnalystReportXML = "saveAnalystReportXML";

    public void saveAnalystReportXML() 
    {
        ViskitGlobals.instance().selectAnalystReportTab();
        if (getAnalystReportModel() == null)
        {
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "Analyst Report not ready",
                    "<html><p align='center'>To save XML, first load and edit an Analyst Report, or else</p><br />" + 
                          "<p align='center'>perform a simulation run in order to create a new Analyst Report.</p>");
            return;
        }
        JFileChooser saveAnalystReportXmlChooser = new JFileChooser(getAnalystReportModel().getAnalystReportXmlFile().getParent());
        saveAnalystReportXmlChooser.setDialogTitle("Save Analyst Report XML");
        saveAnalystReportXmlChooser.setSelectedFile(getAnalystReportModel().getAnalystReportXmlFile());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst report data files (.xml)", "xml");
        saveAnalystReportXmlChooser.setFileFilter(filter);
        saveAnalystReportXmlChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int response = saveAnalystReportXmlChooser.showSaveDialog(analystReportViewFrame);

        if (response != JFileChooser.APPROVE_OPTION) { // cancel
            return;
        }
        analystReportViewFrame.unFillLayout();

        // Ensure user can save a unique name for Analyst Report (Bug fix: 1260)
        getAnalystReportModel().setAnalystReportFile(saveAnalystReportXmlChooser.getSelectedFile());
        saveReport(getAnalystReportModel().getAnalystReportXmlFile()); // first ensure that current report is saved
        String outputFileName = getAnalystReportModel().getAnalystReportXmlFile().getAbsolutePath();
        int idx = outputFileName.lastIndexOf(".");

        outputFileName = outputFileName.substring(0, idx) + ".xml"; // TODO superfluous?
        XsltUtility.runXsltStylesheet(getAnalystReportModel().getAnalystReportXmlFile().getAbsolutePath(),
                outputFileName, "config/AnalystReportXMLtoHTML.xslt");
    }
    
    /** method name for reflection use */
    public static final String METHOD_generateHtmlReport = "generateHtmlReport";

    public void generateHtmlReport() throws IOException 
    {
        if (analystReportViewFrame == null)
        {
            String message1 = "Analyst Report results must first be available in order to generateHtmlReport()";
            String message2 = "<html><p align='center'>Analyst Report results must first be available</p><br />" +
                                    "<p align='center'>in order to generate the HTML Analyst Report.</p><br />" +
                                    "<p align='center'>Please load a prior Analyst Report or else perform a simulation run first.</p><br />";
            LOG.error(message1);
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                "Unable to show Analyst Report", message2);
            return;
        }
        ViskitGlobals.instance().selectAnalystReportTab();
        if (!ViskitGlobals.instance().getSimulationRunPanel().analystReportCB.isSelected()) {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Enable Analyst Reports not selected",
                    "<html><body><p align='center'>"
                    + "The checkbox for <code>Enable Analyst Reports </code>is not"
                    + " currently selected.  Please select on the <code>Viskit User Preferences</code> panel,"
                    + "then re-run the simulation to produce the Analyst Report.</p></body></html>"
            );
            return;
        }
        analystReportViewFrame.unFillLayout();
        saveReport(getAnalystReportModel().getAnalystReportXmlFile());

        String outputHtmlFilePath = getAnalystReportModel().getAnalystReportXmlFile().getAbsolutePath();
        // change .xml extension to .html
        // simplified output report name.  TODO: handle . characters in path/filename itself
        outputHtmlFilePath = outputHtmlFilePath.substring(0, 
                // outputHtmlFilePath.lastIndexOf(".")) + ".html";
                outputHtmlFilePath.indexOf(".xml")) + ".html";
        
        // all unnecessary, user does not need to frequently change paths, this code is problematic anywar
//        File analystReportDirectory = ViskitGlobals.instance().getViskitProject().getAnalystReportsDirectory();
//        JFileChooser generateHtmlReportChooser = new JFileChooser(analystReportDirectory);
//        generateHtmlReportChooser.setDialogTitle("Save as XML, then convert to HTML");
//        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst report data (.xml)", "xml");
//        generateHtmlReportChooser.setFileFilter(filter);
//        generateHtmlReportChooser.setSelectedFile(new File(outputHtmlFilePath));
//        generateHtmlReportChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        // consistently offer chooser options to rename, simply accepting that saves user a step
//        if (JOptionPane.YES_OPTION
//                == JOptionPane.showConfirmDialog(analystReportFrame,
//                        "<html><p align='center'>Change Analyst Report output file name?</p><br>" + 
//                            "<p align='center'>" + outputHtmlFilePath + "</p></html>",
//                        "Confirm", JOptionPane.YES_NO_OPTION)) {
//            generateHtmlReportChooser.showSaveDialog(analystReportFrame);
//        }

        // always generate new report before display, regardless of old or new name
        // TODO:  change XML input to temp file, rather than final file, if possible
        try {
            XsltUtility.runXsltStylesheet(getAnalystReportModel().getAnalystReportXmlFile().getCanonicalPath(), // XML  input
                outputHtmlFilePath, // HTML output
                VISKIT_CONFIG_DIRECTORY + "/" + "AnalystReportXMLtoHTML.xslt");  // XSLT stylesheet
        }
        catch (IOException ioe)
        {
            LOG.error(METHOD_generateHtmlReport + "() trouble converting XML to HTML", ioe);
        }

        // show latest report, they asked for it... also confirms operation in case window is hidden
        LOG.info("Launching HTML Analyst Report in browser\n      {}", outputHtmlFilePath);
        ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Launching HTML Analyst Report in browser",
                    "<html><body><p align='center'>Launching HTML Analyst Report in browser</p><br  />" +
                                "<p align='center'>" + outputHtmlFilePath + "</p><br /></body></html>");
        showHtmlViewer(outputHtmlFilePath);
    }

    private void saveReport()
    {
        saveReport(getAnalystReportModel().getAnalystReportXmlFile());
    }

    private void saveReport(File reportFile)
    {
        if (!isFileReady(reportFile))
            return;
        
        try {
            getAnalystReportModel().writeToXMLFile(reportFile);
            analystReportViewFrame.setReportDirty(false);
        } 
        catch (Exception e) {
            LOG.error("saveReport(" + reportFile.getAbsolutePath() + ") exception: " + e);
        }
        ViskitGlobals.instance().selectAnalystReportTab();
    }
    
    /** method name for reflection use */
    public static final String METHOD_openAnalystReport = "openAnalystReport";

    private void openAnalystReport(File selectedFile)
    {
        ViskitGlobals.instance().selectAnalystReportTab();
        
        AnalystReportModel analystReportModelLocal = new AnalystReportModel(selectedFile);
        setContent(analystReportModelLocal);
        getAnalystReportModel().setAnalystReportFile(selectedFile);
        analystReportViewFrame.setReportDirty(false);
    }

    private void buildAnalystReport(File targetFile)
    {
        LOG.debug("buildAnalystReport() targetFile is:\n      {}", targetFile);
        AnalystReportModel analystReportModelLocal;
        try {
            analystReportModelLocal = new AnalystReportModel(analystReportViewFrame, targetFile, currentAssemblyFile);
        } 
        catch (Exception e) {
            LOG.error("Error parsing Analyst Report: {}", e);
//            e.printStackTrace();
            return;
        }
        setContent(analystReportModelLocal);
        getAnalystReportModel().setAnalystReportFile(targetFile);
        analystReportViewFrame.setReportDirty(false);
        analystReportModelLocal.setReportReady(true);
    }

    private void setContent(AnalystReportModel analystReportModelLocal) {
        if (analystReportModelLocal != null && analystReportViewFrame.isReportDirty()) {
            int resp = JOptionPane.showConfirmDialog(analystReportViewFrame,
                    "<html><body><p align='center'>The experiment has completed and the report is ready to be displayed.<br>" +
                    "The current report data has not been saved. Save current report before continuing?</p></body></html>",
                    "Save Report",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (resp == JOptionPane.YES_OPTION) {
                saveReport();
            }
        }

        analystReportViewFrame.setReportDirty(false);

        this.analystReportModel = analystReportModelLocal;
        analystReportViewFrame.setReportBuilder(analystReportModelLocal);
        analystReportViewFrame.fillLayout();
    }

    public void showHtmlViewer(String htmlFilepath) // TODO problem on mac?
    {
        URI htmlFilepathURI;
        ViskitGlobals.instance().selectAnalystReportTab();
        // pop up the system html viewer, or send currently running browser to html page
        try {
            htmlFilepathURI = new URI(htmlFilepath.replaceAll("\\\\", "/"));
            // must convert slashes here when creating URI, not beforehand
            Desktop.getDesktop().browse(htmlFilepathURI);
        } 
        // see https://StackOverflow.com/questions/31367967/open-in-default-browser-exception
        // exception on macos is a JDK problem, have to fall back to Runtime.exec for browser launch
        catch (URISyntaxException ex) {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><p align='center'>URISyntaxException displaying HTML:<br>" + ex.getMessage() + "<br /></p></html>"
            );
            LOG.error(ex.getMessage());
        }
        catch (IOException ex) {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><p align='center'>IOException displaying HTML:<br>" + ex.getMessage() + "<br /></p></html>"
            );
            LOG.error("showHtmlViewer() Browser Launch Error\n      " + htmlFilepath + "\n{}",ex.getMessage());
        }
    }

    public void showHtmlViewer(File file)
    {
        ViskitGlobals.instance().selectAnalystReportTab();
        // pop up the system html viewer, or send currently running browser to html page
        try {
            // must convert slashes here when creating URI, not beforehand
            Desktop.getDesktop().browse(new URI(file.getPath().replaceAll("\\\\", "/")));
        } 
        catch (URISyntaxException ex) {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><p align='center'>URISyntaxException displaying HTML:<br>" + ex.getMessage() + "<br /></p></html>"
            );
            LOG.error(ex.getMessage());
        }
        catch (IOException ex) {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><p align='center'>IOException displaying HTML:<br>" + ex.getMessage() + "<br /></p></html>"
            );
            LOG.error(ex.getMessage());
        }
    }
    
    /** method name for reflection use */
    public static final String METHOD_viewXML = "viewXML";

//    @Override
    public void viewXML() 
    {
        ViskitGlobals.instance().selectAnalystReportTab();
        if ((getAnalystReportModel() == null) || (ViskitGlobals.instance().getAssemblyViewFrame() == null))
        {
            ViskitGlobals.instance().messageUser(JOptionPane.INFORMATION_MESSAGE, "Analyst Report not ready",
                    "<html><p align='center'>To view XML, first load an Analyst Report or else</p><br />" + 
                          "<p align='center'>perform a simulation run in order to create a new Analyst Report.</p>");
            return;
        }
        // bravely ignoring error checking here...
        ViskitGlobals.instance().getAssemblyViewFrame().displayXML(getAnalystReportModel().getAnalystReportXmlFile());
    }

    /**
     * @return the analystReportModel
     */
    public AnalystReportModel getAnalystReportModel() {
        return analystReportModel;
    }

} // end class file AnalystReportController.java
