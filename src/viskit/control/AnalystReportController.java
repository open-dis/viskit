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

import edu.nps.util.Log4jUtilities;

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

import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import static viskit.ViskitStatics.isFileReady;
import viskit.mvc.MvcAbstractController;
import viskit.model.AnalystReportModel;
import viskit.util.XsltUtility;
import viskit.view.AnalystReportViewFrame;

/** A controller for the analyst report panel.  All functions are to be
 * performed here vice the view.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.AnalystReportController">Terry Norbraten, NPS MOVES</a>
 * @version $Id$
 */
public class AnalystReportController extends MvcAbstractController {

    static final Logger LOG = Log4jUtilities.getLogger(AnalystReportController.class);

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
        String outputFilenameDated = (assemblyName + "_AnalystReport_" + dateOutput + ".xml");
        String outputFilename      = (assemblyName + "_AnalystReport"               + ".xml");

        File analystReportXmlFile = new File(analystReportDirectory, outputFilenameDated);
        
        LOG.info("xmlSourceFile.getAbsolutePath()=\n   " + xmlSourceFile.getAbsolutePath());
        isFileReady(xmlSourceFile);
        try {
            Files.copy(xmlSourceFile.toPath(), analystReportXmlFile.toPath());
        } 
        catch (IOException ioe)
        {
            LOG.warn(ioe);
        }
        LOG.info("analystReportXmlFile.toPath()=\n   " + analystReportXmlFile.getAbsolutePath());
        isFileReady(xmlSourceFile);
        if (analystReportModel == null)
            analystReportModel =  new AnalystReportModel(analystReportXmlFile);

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

    public void openAnalystReportXML() 
    {
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
        if (analystReportViewFrame.isReportDirty()) {
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

        int resp = openAnalystReportChooser.showOpenDialog(analystReportViewFrame);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }

        openAnalystReport(openAnalystReportChooser.getSelectedFile());
    }

    public void setCurrentAssemblyFile(File f) {
        currentAssemblyFile = f;

        if (analystReportModel != null) {
            analystReportModel.setAssemblyFile(currentAssemblyFile);
        }
    }

    public void saveAnalystReportXML() 
    {
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
        JFileChooser saveAnalystReportXmlChooser = new JFileChooser(analystReportModel.getAnalystReportXmlFile().getParent());
        saveAnalystReportXmlChooser.setDialogTitle("Save Analyst Report XML");
        saveAnalystReportXmlChooser.setSelectedFile(analystReportModel.getAnalystReportXmlFile());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst report data files (.xml)", "xml");
        saveAnalystReportXmlChooser.setFileFilter(filter);
        saveAnalystReportXmlChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int response = saveAnalystReportXmlChooser.showSaveDialog(analystReportViewFrame);

        if (response != JFileChooser.APPROVE_OPTION) { // cancel
            return;
        }
        analystReportViewFrame.unFillLayout();

        // Ensure user can save a unique name for Analyst Report (Bug fix: 1260)
        analystReportModel.setAnalystReportFile(saveAnalystReportXmlChooser.getSelectedFile());
        saveReport(analystReportModel.getAnalystReportXmlFile()); // first ensure that current report is saved
        String outputFileName = analystReportModel.getAnalystReportXmlFile().getAbsolutePath();
        int idx = outputFileName.lastIndexOf(".");

        outputFileName = outputFileName.substring(0, idx) + ".xml"; // TODO superfluous?
        XsltUtility.runXslt(analystReportModel.getAnalystReportXmlFile().getAbsolutePath(),
                outputFileName, "config/AnalystReportXMLtoHTML.xslt");
    }

    public void generateHtmlReport() throws IOException 
    {
        if (!ViskitGlobals.instance().getSimulationRunPanel().analystReportCB.isSelected()) {
            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Enable Analyst Reports not selected",
                    "<html><body><p align='center'>"
                    + "The checkbox for <code>Enable Analyst Reports </code>is not"
                    + " currently selected.  Please select on the <code>Assembly Simulation Run </code>panel,"
                    + " re-run the experiment and the report will then be available to "
                    + "view.</p></body></html>"
            );
            return;
        }

        analystReportViewFrame.unFillLayout();
        saveReport(analystReportModel.getAnalystReportXmlFile());

        String outputHtmlFilePath = analystReportModel.getAnalystReportXmlFile().getAbsolutePath();
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
            XsltUtility.runXslt(analystReportModel.getAnalystReportXmlFile().getCanonicalPath(), // XML  input
                outputHtmlFilePath, // HTML output
                "config/AnalystReportXMLtoHTML.xslt");  // stylesheet
        }
        catch (IOException ioe)
        {
            LOG.error("trouble converting XML to HTML", ioe);
        }

        // always show latest report, they asked for it
        showHtmlViewer(outputHtmlFilePath);
        
        // TODO popup
    }

    private void saveReport()
    {
        saveReport(analystReportModel.getAnalystReportXmlFile());
    }

    private void saveReport(File reportFile)
    {
        if (!isFileReady(reportFile))
            return;
        
        try {
            analystReportModel.writeToXMLFile(reportFile);
            analystReportViewFrame.setReportDirty(false);
        } 
        catch (Exception e) {
            LOG.error("saveReport(" + reportFile.getAbsolutePath() + ") exception: " + e);
        }
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
    }

    private void openAnalystReport(File selectedFile)
    {
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
        
        AnalystReportModel analystReportModelLocal = new AnalystReportModel(selectedFile);
        setContent(analystReportModelLocal);
        analystReportModel.setAnalystReportFile(selectedFile);
        analystReportViewFrame.setReportDirty(false);
    }

    private void buildAnalystReport(File targetFile)
    {
        LOG.debug("TargetFile is: {}", targetFile);
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
        analystReportModel.setAnalystReportFile(targetFile);
        analystReportViewFrame.setReportDirty(false);
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

    public void showHtmlViewer(String htmlFilepath) 
    {
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
        // pop up the system html viewer, or send currently running browser to html page
        try {
            // must convert slashes here when creating URI, not beforehand
            Desktop.getDesktop().browse(new URI(htmlFilepath.replaceAll("\\\\", "/")));
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

    public void showHtmlViewer(File file)
    {
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
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

//    @Override
    public void viewXML() 
    {
        ViskitGlobals.instance().getMainFrame().selectAnalystReportTab();
        
        // bravely ignoring error checking here...
        ViskitGlobals.instance().getAssemblyViewFrame().displayXML(analystReportModel.getAnalystReportXmlFile());
    }

} // end class file AnalystReportController.java
