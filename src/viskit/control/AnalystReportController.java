/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

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
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.Logger;

import viskit.VGlobals;
import viskit.mvc.mvcAbstractController;
import viskit.model.AnalystReportModel;
import viskit.util.XsltUtility;
import viskit.view.AnalystReportFrame;

/** A controller for the analyst report panel.  All functions are to be
 * performed here vice the view.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.AnalystReportController">Terry Norbraten, NPS MOVES</a>
 * @version $Id$
 */
public class AnalystReportController extends mvcAbstractController {

    static final Logger LOG = LogUtils.getLogger(AnalystReportController.class);

    private AnalystReportFrame analystReportFrame;
    private File   analystReportFile;
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

        File analystReportDirectory = VGlobals.instance().getCurrentViskitProject().getAnalystReportDir();

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
            analystReportFrame = (AnalystReportFrame) getView();
        }

        analystReportFrame.showProjectName();
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

    public void openAnalystReport() {
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

        File analystReportsDir = VGlobals.instance().getCurrentViskitProject().getAnalystReportDir();
        JFileChooser openChooser = new JFileChooser(analystReportsDir);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst Report files only", "xml");
        openChooser.setFileFilter(filter);
        openChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int response = openChooser.showOpenDialog(analystReportFrame);
        if (response != JFileChooser.APPROVE_OPTION) {
            return;
        }
        openAnalystReport(openChooser.getSelectedFile());
    }

    public void setCurrentAssemblyFile(File f) {
        currentAssemblyFile = f;

        if (analystReportModel != null) {
            analystReportModel.setAssemblyFile(currentAssemblyFile);
        }
    }

    public void clearAnalystReport()
    {
        analystReportFrame.unFillLayout();
        // TODO updated display as well?
    }

    public void saveAnalystReport() {
        JFileChooser saveChooser = new JFileChooser(analystReportFile.getParent());
        saveChooser.setSelectedFile(analystReportFile);
        saveChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int response = saveChooser.showSaveDialog(analystReportFrame);

        if (response != JFileChooser.APPROVE_OPTION) {
            return;
        }
        // not clearing TODO in hopes of iterative improvements
        
        // Ensure user can save a unique name for Analyst Report (Bug fix: 1260)
        analystReportFile = saveChooser.getSelectedFile();
        saveReport(analystReportFile);
        String analystReportFilePath = analystReportFile.getAbsolutePath();
        int idx = analystReportFilePath.lastIndexOf(".");

        analystReportFilePath = analystReportFilePath.substring(0, idx) + ".html";
        XsltUtility.runXslt(analystReportFile.getAbsolutePath(),
                analystReportFilePath, "config/AnalystReportXMLtoHTML.xslt");
    }

    public void generateHtmlReport()
    {
        if (!VGlobals.instance().getSimRunnerPanel().analystReportCB.isSelected()) {
            VGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.INFORMATION_MESSAGE,
                    "Enable Analyst Reports not selected",
                    "<html><body><p align='center'>"
                    + "The checkbox for <code>Enable Analyst Reports </code>is not"
                    + " currently selected.  Please select on the <code>Assembly Run </code>panel,"
                    + " re-run the experiment and the report will then be available to "
                    + "view.</p></body></html>"
            );
            return;
        }

        analystReportFrame.unFillLayout();
        saveReport(analystReportFile);

        String analysisReportFilePath = analystReportFile.getAbsolutePath();
        int idx = analysisReportFilePath.lastIndexOf(".");

        analysisReportFilePath = analysisReportFilePath.substring(0, idx) + ".html";

        File analystReportDir = VGlobals.instance().getCurrentViskitProject().getAnalystReportDir();
        JFileChooser generateAnalystReportChooser = new JFileChooser(analystReportDir);
        generateAnalystReportChooser.setSelectedFile(new File(analysisReportFilePath));
        generateAnalystReportChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (JOptionPane.YES_OPTION
                == JOptionPane.showConfirmDialog(analystReportFrame,
                        "Rename analyst report output?",
                        "Confirm", JOptionPane.YES_NO_OPTION)) {
            generateAnalystReportChooser.showSaveDialog(analystReportFrame);
        }

        // always generate new report before display, regardless of old or new name
        // TODO:  change XML input to temp file, rather than final file, if possible
        XsltUtility.runXslt(analystReportFile.getAbsolutePath(), // XML  input
                generateAnalystReportChooser.getSelectedFile().getAbsolutePath(), // HTML output
                "config/AnalystReportXMLtoHTML.xslt");  // stylesheet

        // always show latest report, they asked for it
        showHtmlViewer(generateAnalystReportChooser.getSelectedFile());
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
        LOG.info("TargetFile is: {}", targetFile);
        AnalystReportModel analystReportModelLocal;
        try {
            analystReportModelLocal = new AnalystReportModel(analystReportFrame, targetFile, currentAssemblyFile);
        } catch (Exception e) {
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

    private void showHtmlViewer(File f) {

        // pop up the system html viewer, or send currently running browser to html page
        try {
            Desktop.getDesktop().browse(f.toURI());
        } catch (IOException ex) {
            VGlobals.instance().getAssemblyEditor().genericReport(JOptionPane.ERROR_MESSAGE,
                    "Browser Launch Error",
                    "<html><center>Error displaying HTML:<br>" + ex.getMessage()
            );
        }
    }

} // end class file AnalystReportController.java
