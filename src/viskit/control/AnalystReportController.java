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

import edu.nps.util.LogUtils;

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
import viskit.mvc.MvcAbstractController;
import viskit.model.AnalystReportModel;
import viskit.model.Model;
import viskit.util.XsltUtility;
import viskit.view.AnalystReportViewFrame;
import viskit.view.AssemblyViewFrame;

/** A controller for the analyst report panel.  All functions are to be
 * performed here vice the view.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.AnalystReportController">Terry Norbraten, NPS MOVES</a>
 * @version $Id$
 */
public class AnalystReportController extends MvcAbstractController {

    static final Logger LOG = LogUtils.getLogger(AnalystReportController.class);

    private AnalystReportViewFrame analystReportFrame;
    private File analystReportFile;
    private File currentAssemblyFile;
    private AnalystReportModel analystReportModel;

    /** Creates a new instance of AnalystReportController */
    public AnalystReportController() {}

    /** Called from the InternalAssemblyRunner when the temp Analyst report is
     * filled out and ready to copy from a temp to a permanent directory
     *
     * @param path the path to the temp Analyst Report that will be copied
     */
    public void setReportXML(String path) {

        LOG.debug("Path of temp Analyst Report: " + path);
        File srcFil = new File(path);

        File analystReportDirectory = ViskitGlobals.instance().getCurrentViskitProject().getAnalystReportsDirectory();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        String output = formatter.format(new Date()); // today

        String usr = System.getProperty("user.name");
        String outputFile = (usr + "AnalystReport_" + output + ".xml");

        File targetFile = new File(analystReportDirectory, outputFile);
        try {
            Files.copy(srcFil.toPath(), targetFile.toPath());
        } catch (IOException ioe) {
            LOG.warn(ioe);
        }

        if (analystReportFrame == null) {
            analystReportFrame = (AnalystReportViewFrame) getView();
        }

        analystReportFrame.setTitleApplicationProjectName();
        buildAnalystReport(targetFile);
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
        if (analystReportFrame.isReportDirty()) {
            int result = JOptionPane.showConfirmDialog(analystReportFrame,
                    "Save current simulation data and analyst report annotations?",
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
        File analystReportDirectory = ViskitGlobals.instance().getCurrentViskitProject().getAnalystReportsDirectory();
        JFileChooser openAnalystReportChooser = new JFileChooser(analystReportDirectory);
        openAnalystReportChooser.setDialogTitle("Open Analyst Report XML");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst Report data files (.xml)", "xml");
        openAnalystReportChooser.setFileFilter(filter);
        openAnalystReportChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resp = openAnalystReportChooser.showOpenDialog(analystReportFrame);
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
        JFileChooser saveAnalystReportXmlChooser = new JFileChooser(analystReportFile.getParent());
        saveAnalystReportXmlChooser.setDialogTitle("Save Analyst Report XML");
        saveAnalystReportXmlChooser.setSelectedFile(analystReportFile);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst report data files (.xml)", "xml");
        saveAnalystReportXmlChooser.setFileFilter(filter);
        saveAnalystReportXmlChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int response = saveAnalystReportXmlChooser.showSaveDialog(analystReportFrame);

        if (response != JFileChooser.APPROVE_OPTION) { // cancel
            return;
        }
        analystReportFrame.unFillLayout();

        // Ensure user can save a unique name for Analyst Report (Bug fix: 1260)
        analystReportFile = saveAnalystReportXmlChooser.getSelectedFile();
        saveReport(analystReportFile);
        String outputFile = analystReportFile.getAbsolutePath();
        int idx = outputFile.lastIndexOf(".");

        outputFile = outputFile.substring(0, idx) + ".xml";
        XsltUtility.runXslt(analystReportFile.getAbsolutePath(),
                outputFile, "config/AnalystReportXMLtoHTML.xslt");
    }

    public void generateHtmlReport() 
    {
        if (!ViskitGlobals.instance().getAssemblySimulationRunPanel().analystReportCB.isSelected()) {
            ViskitGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Enable Analyst Reports not selected",
                    "<html><body><p align='center'>"
                    + "The checkbox for <code>Enable Analyst Reports </code>is not"
                    + " currently selected.  Please select on the <code>Assembly Simulation Run </code>panel,"
                    + " re-run the experiment and the report will then be available to "
                    + "view.</p></body></html>"
            );
            return;
        }

        analystReportFrame.unFillLayout();
        saveReport(analystReportFile);

        String outputHtmlFilePath = analystReportFile.getAbsolutePath();
        // change .xml extension to .html
        outputHtmlFilePath = outputHtmlFilePath.substring(0, outputHtmlFilePath.lastIndexOf(".")) + ".html";
        
        // all unnecessary, user does not need to frequently change paths, this code is problematic anywar
//        File analystReportDirectory = ViskitGlobals.instance().getCurrentViskitProject().getAnalystReportsDirectory();
//        JFileChooser generateHtmlReportChooser = new JFileChooser(analystReportDirectory);
//        generateHtmlReportChooser.setDialogTitle("Save as XML, then convert to HTML");
//        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst report data (.xml)", "xml");
//        generateHtmlReportChooser.setFileFilter(filter);
//        generateHtmlReportChooser.setSelectedFile(new File(outputHtmlFilePath));
//        generateHtmlReportChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        // consistently offer chooser options to rename, simply accepting that saves user a step
//        if (JOptionPane.YES_OPTION
//                == JOptionPane.showConfirmDialog(analystReportFrame,
//                        "<html><p align='center'>Change analyst report output file name?</p><br>" + 
//                            "<p align='center'>" + outputHtmlFilePath + "</p></html>",
//                        "Confirm", JOptionPane.YES_NO_OPTION)) {
//            generateHtmlReportChooser.showSaveDialog(analystReportFrame);
//        }

        // always generate new report before display, regardless of old or new name
        // TODO:  change XML input to temp file, rather than final file, if possible
        XsltUtility.runXslt(analystReportFile.getAbsolutePath(), // XML  input
                outputHtmlFilePath, // HTML output
                "config/AnalystReportXMLtoHTML.xslt");  // stylesheet

        // always show latest report, they asked for it
        showHtmlViewer(outputHtmlFilePath);
    }

    private void saveReport() {
        saveReport(analystReportFile);
    }

    private void saveReport(File f) {
        try {
            analystReportModel.writeToXMLFile(f);
            analystReportFrame.setReportDirty(false);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private void openAnalystReport(File selectedFile) {
        AnalystReportModel analystReportModelLocal = new AnalystReportModel(selectedFile);
        setContent(analystReportModelLocal);
        analystReportFile = selectedFile;
        analystReportFrame.setReportDirty(false);
    }

    private void buildAnalystReport(File targetFile) {
        LOG.debug("TargetFile is: {}", targetFile);
        AnalystReportModel analystReportModelLocal;
        try {
            analystReportModelLocal = new AnalystReportModel(analystReportFrame, targetFile, currentAssemblyFile);
        } 
        catch (Exception e) {
            LOG.error("Error parsing analyst report: {}", e);
//            e.printStackTrace();
            return;
        }
        setContent(analystReportModelLocal);
        analystReportFile = targetFile;
        analystReportFrame.setReportDirty(false);
    }

    private void setContent(AnalystReportModel analystReportModelLocal) {
        if (analystReportModelLocal != null && analystReportFrame.isReportDirty()) {
            int resp = JOptionPane.showConfirmDialog(analystReportFrame,
                    "<html><body><p align='center'>The experiment has completed and the report is ready to be displayed.<br>" +
                    "The current report data has not been saved. Save current report before continuing?</p></body></html>",
                    "Save Report",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (resp == JOptionPane.YES_OPTION) {
                saveReport();
            }
        }

        analystReportFrame.setReportDirty(false);

        this.analystReportModel = analystReportModelLocal;
        analystReportFrame.setReportBuilder(analystReportModelLocal);
        analystReportFrame.fillLayout();
    }

    private void showHtmlViewer(String htmlFilename) 
    {
        showHtmlViewer(new File(htmlFilename));
    }

    private void showHtmlViewer(File file) {

        // pop up the system html viewer, or send currently running browser to html page
        try {
            // must convert slashes here when creating URI, not beforehand
            Desktop.getDesktop().browse(new URI(file.getPath().replaceAll("\\\\", "/")));
        } 
        catch (URISyntaxException ex) {
            ViskitGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><center>URISyntaxException displaying HTML:<br>" + ex.getMessage()
            );
            System.err.println(ex.getMessage());
        }
        catch (IOException ex) {
            ViskitGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><center>IOException displaying HTML:<br>" + ex.getMessage()
            );
            System.err.println(ex.getMessage());
        }
    }

//    @Override
    public void viewXML() 
    {
        // bravely ignoring error checking here...
        ViskitGlobals.instance().getAssemblyEditor().displayXML(analystReportFile);
    }

} // end class file AnalystReportController.java
