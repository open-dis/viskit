/*
Copyright (c) 1995-2015 held by the author(s).  All rights reserved.

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
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
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

import edu.nps.util.FileIO;
import edu.nps.util.LogUtils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.log4j.Logger;
import viskit.VGlobals;
import viskit.mvc.mvcAbstractController;
import viskit.reports.AnalystReportBuilder;
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

    private AnalystReportFrame frame;
    private File reportFile;
    private File currentAssyFile;
    private AnalystReportBuilder arb;

    /** Creates a new instance of AnalystReportController */
    public AnalystReportController() {}

    /** Called from the InternalAssemblyRunner when the temp Analyst report is
     * filled out and ready to copy
     *
     * @param path the path to the temp Analyst Report that will be copied
     */
    public void setReportXML(String path) {

        LOG.debug("Path of temp Analyst Report: " + path);
        File srcFil = new File(path);

        File aRDir = VGlobals.instance().getCurrentViskitProject().getAnalystReportsDir();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
        String output = formatter.format(new Date()); // today

        String usr = System.getProperty("user.name");
        String outputFile = (usr + "AnalystReport_" + output + ".xml");

        File targetFile = new File(aRDir, outputFile);
        try {
            FileIO.copyFile(srcFil, targetFile, true);
            srcFil.deleteOnExit();
        } catch (IOException ioe) {
            LOG.fatal(ioe);
        }

        if (frame == null) {
            frame = (AnalystReportFrame) getView();
        }

        frame.showProjectName();
        buildArb(targetFile);
    }

    public void openAnalystReport() {
        if (frame.isReportDirty()) {
            int result = JOptionPane.showConfirmDialog(frame,
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

        File aRDir = VGlobals.instance().getCurrentViskitProject().getAnalystReportsDir();
        JFileChooser openChooser = new JFileChooser(aRDir);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Analyst Report files only", "xml");
        openChooser.setFileFilter(filter);
        openChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resp = openChooser.showOpenDialog(frame);
        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }

        openAnalystReport(openChooser.getSelectedFile());
    }

    public void setCurrentAssyFile(File f) {
        currentAssyFile = f;

        if (arb != null) {
            arb.setAssemblyFile(currentAssyFile);
        }
    }

    public void saveAnalystReport() {
        JFileChooser saveChooser = new JFileChooser(reportFile.getParent());
        saveChooser.setSelectedFile(reportFile);
        saveChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resp = saveChooser.showSaveDialog(frame);

        if (resp != JFileChooser.APPROVE_OPTION) {
            return;
        }

        frame.unFillLayout();

        // Ensure user can save a unique name for Analyst Report (Bug fix: 1260)
        reportFile = saveChooser.getSelectedFile();
        saveReport(reportFile);
        String outFile = reportFile.getAbsolutePath();
        int idx = outFile.lastIndexOf(".");

        outFile = outFile.substring(0, idx) + ".html";
        XsltUtility.runXslt(reportFile.getAbsolutePath(),
                outFile, "configuration/AnalystReportXMLtoHTML.xslt");
    }

    public void generateHtmlReport() {
        if (!VGlobals.instance().getRunPanel().analystReportCB.isSelected()) {
            JOptionPane.showMessageDialog(null, "<html><body><p align='center'>"
                    + "The checkbox for <code>Enable Analyst Reports </code>is not"
                    + " currently selected.  Please select on the <code>Assembly Run </code>panel,"
                    + " re-run the experiment and the report will then be available to "
                    + "view.</p></body></html>", "Enable Analyst Reports not selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        frame.unFillLayout();
        saveReport(reportFile);

        String outFile = reportFile.getAbsolutePath();
        int idx = outFile.lastIndexOf(".");

        outFile = outFile.substring(0, idx) + ".html";

        File aRDir = VGlobals.instance().getCurrentViskitProject().getAnalystReportsDir();
        JFileChooser genChooser = new JFileChooser(aRDir);
        genChooser.setSelectedFile(new File(outFile));
        genChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (JOptionPane.YES_OPTION
                == JOptionPane.showConfirmDialog(frame,
                        "Rename analyst report output?",
                        "Confirm", JOptionPane.YES_NO_OPTION)) {
            genChooser.showSaveDialog(frame);
        }

        // always generate new report before display, regardless of old or new name
        // TODO:  change XML input to temp file, rather than final file, if possible
        XsltUtility.runXslt(reportFile.getAbsolutePath(), // XML  input
                genChooser.getSelectedFile().getAbsolutePath(), // HTML output
                "configuration/AnalystReportXMLtoHTML.xslt");  // stylesheet

        // always show latest report, they asked for it
        showHtmlViewer(genChooser.getSelectedFile());
    }

    private void saveReport() {
        saveReport(reportFile);
    }

    private void saveReport(File f) {
        try {
            arb.writeToXMLFile(f);
            frame.setReportDirty(false);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private void openAnalystReport(File selectedFile) {
        AnalystReportBuilder arbLocal = new AnalystReportBuilder(selectedFile);
        setContent(arbLocal);
        reportFile = selectedFile;
        frame.setReportDirty(false);
    }

    private void buildArb(File targetFile) {
        LOG.debug("TargetFile is: " + targetFile);
        AnalystReportBuilder arbLocal;
        try {
            arbLocal = new AnalystReportBuilder(frame, targetFile, currentAssyFile);
        } catch (Exception e) {
            LOG.error("Error parsing analyst report: " + e.getMessage());
//            e.printStackTrace();
            return;
        }
        setContent(arbLocal);
        reportFile = targetFile;
        frame.setReportDirty(false);
    }

    private void setContent(AnalystReportBuilder arb) {
        if (arb != null && frame.isReportDirty()) {
            int resp = JOptionPane.showConfirmDialog(frame,
                    "<html><body><p align='center'>The experiment has completed and the report is ready to be displayed.<br>" +
                    "The current report data has not been saved. Save current report before continuing?</p></body></html>",
                    "Save Report",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (resp == JOptionPane.YES_OPTION) {
                saveReport();
            }
        }

        frame.setReportDirty(false);

        this.arb = arb;
        frame.setReportBuilder(arb);
        frame.fillLayout();
    }

    private void showHtmlViewer(File f) {
        String errMsg = null;
        // pop up the system html viewer, or send currently running browser to html page
        try {
            viskit.util.BareBonesBrowserLaunch.openURL(f.toURI().toURL().toString());
        } catch (java.net.MalformedURLException mue) {
            errMsg = f + " : malformed path error.";
        }

        if (errMsg != null) {
            JOptionPane.showMessageDialog(frame,
                    "<html><center>Error displaying HTML:<br>" + errMsg,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

} // end class file AnalystReportController.java
