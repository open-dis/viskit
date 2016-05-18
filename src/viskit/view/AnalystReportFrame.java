/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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
package viskit.view;

import actions.ActionIntrospector;
import actions.ActionUtilities;
import edu.nps.util.LogUtilities;
import edu.nps.util.SpringUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.util.OpenAssembly;
import viskit.control.AnalystReportController;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcController;
import viskit.mvc.mvcModelEvent;
import viskit.model.AnalystReportModel;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2006
 * @since 2:47:03 PM
 * @version $Id$
 */
public class AnalystReportFrame extends mvcAbstractJFrameView implements OpenAssembly.AssemblyChangeListener
{
    static final Logger LOG = LogUtilities.getLogger(AnalystReportFrame.class);
	
    private final static String FRAME_DEFAULT_TITLE = " Viskit Analyst Report Editor";
    private AnalystReportModel arb;

    /**
     * TODO: rewire this functionality?
     * boolean to show that raw report has not been saved to AnalystReports
     */
    private boolean reportFileDirty = false;
    private JMenuBar myMenuBar;
    private JMenu    analystReportMenu = new JMenu("Analyst Report");
    private JFileChooser locationImageFileChooser;
	
    JTextField                titleTF = new JTextField();
    JTextField          analystNameTF = new JTextField();
    JComboBox<String> accessControlTF = new JComboBox<>(new String[]{"UNCLASSIFIED", "FOUO", "CONFIDENTIAL", "SECRET"});
    JTextField                 dateTF = new JTextField();
    File currentAssemblyFile;

    public AnalystReportFrame(mvcController controller)
	{
        super(FRAME_DEFAULT_TITLE);
        initializeMVC(controller);
        setLayout();
        setBackground(new Color(251, 251, 229)); // yellow
        buildMenus();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ViskitGlobals.getDateFormat());
		dateTF.setText(simpleDateFormat.format(new Date()));

        locationImageFileChooser = new JFileChooser("./images/");
		locationImageFileChooser.setDialogTitle("Open Image");
    }

    private void initializeMVC(mvcController controller) {
        setController(controller);
    }

    /** Captures the name of the assembly file
     * @param action the action that led us here
     * @param source the listener source
     * @param param the object to act upon
     */
    @Override
    public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
        switch (action) {
            case NEW_ASSEMBLY:
                currentAssemblyFile = (File) param;
                AnalystReportController cntlr = (AnalystReportController) getController();
                cntlr.setCurrentAssyFile(currentAssemblyFile);
                break;

            case CLOSE_ASSEMBLY:
            case PARAM_LOCALLY_EDITED:
            case JAXB_CHANGED:
                break;

            default:
                LOG.error("Program error AnalystReportFrame.assemblyChanged");
        }
    }

    @Override
    public String getHandle() {
        return "";
    }

    public JMenuBar getMenus() {
        return myMenuBar;
    }

    public boolean isReportFileDirty() {
        return reportFileDirty;
    }

    public void setReportFileDirty(boolean b) {
        reportFileDirty = b;
    }

    public void setReportBuilder(AnalystReportModel b) {
        arb = b;
        setModel(arb); // hold on locally
        getController().setModel(getModel()); // tell controller
    }

    public void fillLayout() {

        // We don't always come in on the swing thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                _fillLayout();
            }
         });
    }

    private void _fillLayout() {
        fillHeader();
        fillExecSumm();
        fillSimulationLocation();
        fillSimulationConfiguration();
        fillEntityParams();
        fillBehaviors();
        fillStatisticsPanel();
        fillConclusionsRecommendationsPanel();
    }

    public void unFillLayout() {
        unFillHeader();
        unFillExecSumm();
        unFillSimulationLocation();
        unFillSimulationConfiguration();
        unFillEntityParams();
        unFillBehaviors();
        unFillStatisticsPanel();
        unFillConRecPan();
    }

    private void fillHeader() {
        titleTF.setText(arb.getReportName());
        analystNameTF.setText(arb.getAuthor());
        String date = arb.getDateOfReport();
        if (date != null && date.length() > 0) {
            dateTF.setText(date);
        } else {
            dateTF.setText(DateFormat.getDateInstance().format(new Date()));
        } //now
        accessControlTF.setSelectedItem(arb.getClassification());
    }

    private void unFillHeader() {
        arb.setReportName(titleTF.getText());
        arb.setAuthor(analystNameTF.getText());
        arb.setDateOfReport(dateTF.getText());
        arb.setClassification((String) accessControlTF.getSelectedItem());
    }

    private void setLayout() {

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JTabbedPane tabs = new JTabbedPane();

//      tabs.add("1. Document Header",     headerPanel);
        tabs.add("1. Executive Summary",   makeExecutiveSummaryPanel());
        tabs.add("2. Scenario Location",   makeSimulationLocationPanel());
        tabs.add("3. Simulation Assembly", makeAssemblyDesignPanel());
        tabs.add("4. Entity Parameters",   makeEntityParamsPanel());
        tabs.add("5. Model Behaviors",     makeBehaviorsPanel());
        tabs.add("6. Statistical Results", makeStatisticsPanel());
        tabs.add("7. Conclusions and Recommendations", makeConclusionsRecommendationsPanel());

        add(tabs);
    //setBorder(new EmptyBorder(10,10,10,10));
    }
    JCheckBox wantExecutiveSummary;
    JTextArea execSummTA;

    private JPanel makeExecutiveSummaryPanel() 
	{
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel headerPanel = new JPanel(new SpringLayout());
        headerPanel.add(new JLabel("Title"));
        headerPanel.add(titleTF); // TODO setting defaults
        headerPanel.add(new JLabel("Author"));
        headerPanel.add(analystNameTF); // TODO setting defaults
        headerPanel.add(new JLabel("Analysis Date"));
        headerPanel.add(dateTF);
        headerPanel.add(new JLabel("Report Classification"));
        headerPanel.add(accessControlTF);
        Dimension d = new Dimension(Integer.MAX_VALUE, titleTF.getPreferredSize().height);
        titleTF.setMaximumSize(new Dimension(d));
        analystNameTF.setMaximumSize(new Dimension(d));
        dateTF.setMaximumSize(new Dimension(d));
        accessControlTF.setMaximumSize(new Dimension(d));
        SpringUtilities.makeCompactGrid(headerPanel, 4, 2, 10, 10, 5, 5);

        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        headerPanel.setAlignmentY(JComponent.RIGHT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height));
		
		p.add(headerPanel);
		
        wantExecutiveSummary = new JCheckBox("include Executive Summary", true);
        wantExecutiveSummary.setToolTipText("include in output report");
        wantExecutiveSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantExecutiveSummary);

        JScrollPane jsp = new JScrollPane(execSummTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Description of Location Features"));
        p.add(jsp);

        execSummTA.setLineWrap(true);
        execSummTA.setWrapStyleWord(true);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private void fillExecSumm() {
        wantExecutiveSummary.setSelected(arb.isExecutiveSummaryComments());
        execSummTA.setText(arb.getExecutiveSummary());
        execSummTA.setEnabled(wantExecutiveSummary.isSelected());
    }

    private void unFillExecSumm() {
        arb.setExecutiveSummaryComments(wantExecutiveSummary.isSelected());
        arb.setExecutiveSummary(execSummTA.getText());
    }
    /************************/
    JCheckBox wantLocationDescriptions;
    JCheckBox wantLocationImages;
    JTextArea locCommentsTA, locConclusionsTA, locProductionNotesTA;
    JTextField simLocImgTF;
    JButton simLocImgButt;
    JTextField simChartImgTF;
    JButton simChartImgButt;

    private JPanel makeSimulationLocationPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantLocationDescriptions = new JCheckBox("include Location Features and notes", true);
        wantLocationDescriptions.setToolTipText("include in output report");
        wantLocationDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantLocationDescriptions);

        JScrollPane jsp = new JScrollPane(locCommentsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Description of Location Features"));
        p.add(jsp);

        jsp = new JScrollPane(locProductionNotesTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Production Notes"));
        p.add(jsp);

        jsp = new JScrollPane(locConclusionsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Significant Location Features"));
        p.add(jsp);

        wantLocationImages = new JCheckBox("Include location and chart image(s)", true);
        wantLocationImages.setToolTipText("Include entries in output report");
        wantLocationImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantLocationImages);

        JPanel imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Location image "));
        imp.add(simLocImgTF = new JTextField(20));
        imp.add(simLocImgButt = new JButton("..."));
        simLocImgButt.addActionListener(new fileChoiceListener(simLocImgTF));
        Dimension ps = simLocImgTF.getPreferredSize();
        simLocImgTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        simLocImgTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(imp);

        imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Chart image "));
        imp.add(simChartImgTF = new JTextField(20));
        imp.add(simChartImgButt = new JButton("..."));
        simChartImgButt.addActionListener(new fileChoiceListener(simChartImgTF));
        ps = simChartImgTF.getPreferredSize();
        simChartImgTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        simChartImgTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(imp);

        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        return p;
    }

    private void fillSimulationLocation() {
        wantLocationDescriptions.setSelected(arb.isPrintSimLocationComments());
        locCommentsTA.setText(arb.getSimLocationComments());
        locCommentsTA.setEnabled(wantLocationDescriptions.isSelected());
        locProductionNotesTA.setText(arb.getSimLocationProductionNotes());
        locProductionNotesTA.setEnabled(wantLocationDescriptions.isSelected());
        locConclusionsTA.setText(arb.getSimLocationConclusions());
        locConclusionsTA.setEnabled(wantLocationDescriptions.isSelected());
        wantLocationImages.setSelected(arb.isPrintSimLocationImage());
        simLocImgTF.setEnabled(wantLocationImages.isSelected());
        simLocImgButt.setEnabled(wantLocationImages.isSelected());
        simChartImgTF.setEnabled(wantLocationImages.isSelected());
        simChartImgButt.setEnabled(wantLocationImages.isSelected());
        simLocImgTF.setText(arb.getLocationImage());
        simChartImgTF.setText(arb.getChartImage());
    }

    private void unFillSimulationLocation() {
        arb.setPrintSimLocationComments(wantLocationDescriptions.isSelected());
        arb.setSimLocationDescription(locCommentsTA.getText());
        arb.setSimLocationProductionNotes(locProductionNotesTA.getText());
        arb.setSimLocationConclusions(locConclusionsTA.getText());
        arb.setPrintSimLocationImage(wantLocationImages.isSelected());
        
        String s = simLocImgTF.getText().trim();
        if (s != null && !s.isEmpty())
            arb.setLocationImage(s);

        s = simChartImgTF.getText();
        if (s != null && !s.isEmpty())
            arb.setChartImage(s);
    }

    /************************/
    JCheckBox wantAssemblyDesignAndAnalysis;
    JTextArea assemblyDesignConsiderations, simConfigConclusions, simProductionNotes;
    JCheckBox wantSimConfigImages;
    JCheckBox wantEntityTable;
    JTable entityTable;
    JTextField configImgPathTF;
    JButton configImgButt;

    private JPanel makeAssemblyDesignPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantAssemblyDesignAndAnalysis = new JCheckBox("Include assembly-design considerations and post-experiment analysis", true);
        wantAssemblyDesignAndAnalysis.setToolTipText("Include entries in output report");
        wantAssemblyDesignAndAnalysis.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantAssemblyDesignAndAnalysis);

        JScrollPane jsp = new JScrollPane(assemblyDesignConsiderations = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Assembly Design Considerations"));
        p.add(jsp);

        jsp = new JScrollPane(simProductionNotes = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Production Notes"));
        p.add(jsp);

        jsp = new JScrollPane(simConfigConclusions = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Simulation Assembly Design"));
        p.add(jsp);

        wantEntityTable = new JCheckBox("Include entity definition table", true);
        wantEntityTable.setToolTipText("Include entries in output report");
        wantEntityTable.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantEntityTable);

        JPanel pp = new JPanel();
        pp.setLayout(new BoxLayout(pp, BoxLayout.X_AXIS));
        pp.add(Box.createHorizontalGlue());
        pp.add(new JScrollPane(entityTable = new JTable()));
        pp.add(Box.createHorizontalGlue());
        pp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(pp);
        //p.add(new JScrollPane(entityTable = new JTable()));
        entityTable.setPreferredScrollableViewportSize(new Dimension(550, 120));

        wantSimConfigImages = new JCheckBox("Include simulation configuration image", true);
        wantSimConfigImages.setToolTipText("Include entries in output report");
        wantSimConfigImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantSimConfigImages);

        JPanel imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Configuration image: "));
        imp.add(configImgPathTF = new JTextField(20));
        imp.add(configImgButt = new JButton("..."));
        configImgButt.addActionListener(new fileChoiceListener(configImgPathTF));
        Dimension ps = configImgPathTF.getPreferredSize();
        configImgPathTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        configImgPathTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(imp);

        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private void fillSimulationConfiguration() {
        wantAssemblyDesignAndAnalysis.setSelected(arb.isPrintSimConfigComments());
        assemblyDesignConsiderations.setText(arb.getSimConfigComments());
        assemblyDesignConsiderations.setEnabled(wantAssemblyDesignAndAnalysis.isSelected());

        wantEntityTable.setSelected(arb.isPrintEntityTable());

        String[][] sa = arb.getSimConfigEntityTable();
        entityTable.setModel(new DefaultTableModel(sa, new String[] {"Entity Name", "Behavior Type"}));
        entityTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        entityTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        simProductionNotes.setText(arb.getSimConfigProductionNotes());
        simProductionNotes.setEnabled(arb.isPrintSimConfigComments());

        simConfigConclusions.setText(arb.getSimConfigConclusions());
        simConfigConclusions.setEnabled(arb.isPrintSimConfigComments());

        wantSimConfigImages.setSelected(arb.isPrintAssemblyImage());
        configImgButt.setEnabled(wantSimConfigImages.isSelected());
        configImgPathTF.setEnabled(wantSimConfigImages.isSelected());
        configImgPathTF.setText(arb.getAssemblyImageLocation());
    }

    private void unFillSimulationConfiguration() {
        arb.setPrintSimConfigComments(wantAssemblyDesignAndAnalysis.isSelected());
        arb.setSimConfigurationDescription(assemblyDesignConsiderations.getText());
        arb.setSimConfigationProductionNotes(simProductionNotes.getText());
        arb.setSimConfigurationConclusions(simConfigConclusions.getText());
        arb.setPrintEntityTable(wantEntityTable.isSelected());
        arb.setPrintAssemblyImage(wantSimConfigImages.isSelected());
        String s = configImgPathTF.getText();
        if (s != null && s.length() > 0) {
            arb.setAssemblyImageLocation(s);
        }
    }

    JCheckBox wantEntityParameterDescriptions;
    JCheckBox wantEntityParameterTables;
    JTabbedPane entityParamTabs;
    JTextArea entityParamCommentsTA;
    JScrollPane entityParamCommentsSP;

    private JPanel makeEntityParamsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantEntityParameterDescriptions = new JCheckBox("Include entity parameter descriptions", true);
        wantEntityParameterDescriptions.setToolTipText("Include entries in output report");
        wantEntityParameterDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantEntityParameterDescriptions);

        entityParamCommentsTA = new WrappingTextArea();
        entityParamCommentsSP = new JScrollPane(entityParamCommentsTA);
        entityParamCommentsSP.setBorder(new TitledBorder("Entity Parameters Overview"));
        entityParamCommentsSP.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(entityParamCommentsSP);

        wantEntityParameterTables = new JCheckBox("Include entity parameter tables", true);
        wantEntityParameterTables.setToolTipText("Include entries in output report");
        wantEntityParameterTables.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantEntityParameterTables);

        // TODO: post-experiment

        entityParamTabs = new JTabbedPane(JTabbedPane.LEFT);
        entityParamTabs.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(entityParamTabs);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    @SuppressWarnings("unchecked")
    private void fillEntityParams() {
        wantEntityParameterDescriptions.setSelected(arb.isPrintParameterComments());
        wantEntityParameterTables.setSelected(arb.isPrintParameterTable());

        entityParamCommentsTA.setText(arb.getParameterComments());

        Vector<String> colNames = new Vector<>();
        colNames.add("Category");
        colNames.add("Name");
        colNames.add("Description");

        Vector<Object[]> v = arb.getParameterTables();

        for (Object[] oa : v) {
            Vector<Vector<String>> tableVector = new Vector<>();
            String nm = (String) oa[0];
            Vector<Object[]> v0 = (Vector) oa[1];
            for (Object[] oa0 : v0) {
                // Rows here
                String nm0 = (String) oa0[0];
                Vector<String> rowVect = new Vector<>(3);
                rowVect.add(nm0);
                rowVect.add("");
                rowVect.add("");
                tableVector.add(rowVect);
                Vector<String[]> v1 = (Vector) oa0[1];
                for (String[] sa : v1) {
                    rowVect = new Vector<>(3);
                    rowVect.add("");
                    rowVect.add(sa[0]); // name
                    rowVect.add(sa[1]); // description
                    tableVector.add(rowVect);
                }
            }

            entityParamTabs.add(nm, new JScrollPane(new EntityParamTable(tableVector, colNames)));
        }
    }

    private void unFillEntityParams() {
        arb.setPrintParameterComments(wantEntityParameterDescriptions.isSelected());
        arb.setPrintParameterTable(wantEntityParameterTables.isSelected());
        arb.setParameterDescription(entityParamCommentsTA.getText());
    }
    JCheckBox doBehaviorDesignAnalysisDescriptions;
    JCheckBox doBehaviorDescriptions;
    JCheckBox doBehaviorImages;
    JTextArea behaviorDescriptionTA;
    JTextArea behaviorConclusionsTA;
    JTabbedPane behaviorTabs;

    private JPanel makeBehaviorsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        doBehaviorDesignAnalysisDescriptions = new JCheckBox("Include behavior design and post-experiment analysis", true);
        doBehaviorDesignAnalysisDescriptions.setToolTipText("Include entries in output report");
        doBehaviorDesignAnalysisDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(doBehaviorDesignAnalysisDescriptions);

        JScrollPane jsp = new JScrollPane(behaviorDescriptionTA = new WrappingTextArea());
        jsp.setBorder(new TitledBorder("Behavior Design Descriptions"));
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(jsp);

        jsp = new JScrollPane(behaviorConclusionsTA = new WrappingTextArea());
        jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Entity Behaviors"));
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(jsp);

        doBehaviorDescriptions = new JCheckBox("Include behavior descriptions", true);
        doBehaviorDescriptions.setToolTipText("Include entries in output report");
        doBehaviorDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(doBehaviorDescriptions);

        doBehaviorImages = new JCheckBox("Include behavior images", true);
        doBehaviorImages.setToolTipText("Include entries in output report");
        doBehaviorImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(doBehaviorImages);

        behaviorTabs = new JTabbedPane(JTabbedPane.LEFT);
        behaviorTabs.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(behaviorTabs);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private void unFillBehaviors() {
        arb.setPrintBehaviorDefComments(doBehaviorDesignAnalysisDescriptions.isSelected());
        arb.setBehaviorDescription(behaviorDescriptionTA.getText());
        arb.setBehaviorConclusions(behaviorConclusionsTA.getText());
        arb.setPrintBehaviorDescriptions(doBehaviorDescriptions.isSelected());
        arb.setPrintEventGraphImages(doBehaviorImages.isSelected());

        // tables are uneditable
    }

    private void fillBehaviors() {
        doBehaviorDesignAnalysisDescriptions.setSelected(arb.isPrintBehaviorDefComments());
        behaviorDescriptionTA.setText(arb.getBehaviorComments());
        behaviorDescriptionTA.setEnabled(doBehaviorDesignAnalysisDescriptions.isSelected());
        behaviorConclusionsTA.setText(arb.getBehaviorConclusions());
        behaviorConclusionsTA.setEnabled(doBehaviorDesignAnalysisDescriptions.isSelected());
        doBehaviorImages.setEnabled(arb.isPrintEventGraphImages());
        doBehaviorDescriptions.setSelected(arb.isPrintBehaviorDescriptions());
        behaviorTabs.setEnabled(doBehaviorDescriptions.isSelected());

        // TODO: JDOM v1.1 does not yet support generics
        List behaviorList = arb.getBehaviorList();

        behaviorTabs.removeAll();
        for (Iterator iterator = behaviorList.iterator(); iterator.hasNext();)
		{
            List nextBehavior = (List) iterator.next();
            String behaviorName = (String) nextBehavior.get(0);
            String behaviorDescription = (String) nextBehavior.get(1);
            List behaviorParameters = (List) nextBehavior.get(2);
            List behaviorStateVariables = (List) nextBehavior.get(3);
            String behaviorImagePath = (String) nextBehavior.get(4);

            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

            JLabel lab = new JLabel("Description:  " + behaviorDescription);
            lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            p.add(lab);

            lab = new JLabel("Image location:  " + behaviorImagePath);
            p.add(lab);

            Vector<String> cols = new Vector<>(3);
            cols.add("name");
            cols.add("type");
            cols.add("description");

            Vector<Vector<String>> data = new Vector<>(behaviorParameters.size());
            for (Object behaviorParameter : behaviorParameters) {
                String[] sa = (String[]) behaviorParameter;
                Vector<String> row = new Vector<>(3);
                row.add(sa[0]);
                row.add(sa[1]);
                row.add(sa[2]);
                data.add(row);
            }
            JScrollPane jsp = new JScrollPane(new ROTable(data, cols));
            jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            jsp.setBorder(new TitledBorder("Parameters"));
            p.add(jsp);

            data = new Vector<>(behaviorStateVariables.size());
            for (Object behaviorStateVariable : behaviorStateVariables) {
                String[] sa = (String[]) behaviorStateVariable;
                Vector<String> row = new Vector<>(3);
                row.add(sa[0]);
                row.add(sa[1]);
                row.add(sa[2]);
                data.add(row);
            }
            jsp = new JScrollPane(new ROTable(data, cols));
            jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            jsp.setBorder(new TitledBorder("State variables"));
            p.add(jsp);

            behaviorTabs.add(behaviorName, p);
        }
    }
    JCheckBox wantStatisticsDescriptionAnalysis;
    JCheckBox wantStatisticsReplications;
    JCheckBox wantStatisticsSummary;
    JTextArea statisticsComments;
    JTextArea statisticsConclusions;
    JPanel statisticsSummaryPanel;
    JPanel statisticsRepPanel;
    JScrollPane repsJsp;
    JScrollPane summJsp;

    private JPanel makeStatisticsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantStatisticsDescriptionAnalysis = new JCheckBox("Include statistical description and analysis", true);
        wantStatisticsDescriptionAnalysis.setToolTipText("Include entries in output report");
        wantStatisticsDescriptionAnalysis.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantStatisticsDescriptionAnalysis);

        JScrollPane jsp = new JScrollPane(statisticsComments = new WrappingTextArea());
        jsp.setBorder(new TitledBorder("Description of Expected Results"));
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(jsp);

        jsp = new JScrollPane(statisticsConclusions = new WrappingTextArea());
        jsp.setBorder(new TitledBorder("Analysis of Experimental Results"));
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(jsp);

        wantStatisticsReplications = new JCheckBox("Include replication statistics", true);
        wantStatisticsReplications.setToolTipText("Include entries in output report");
        wantStatisticsReplications.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantStatisticsReplications);

        repsJsp = new JScrollPane(statisticsRepPanel = new JPanel());
        repsJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsRepPanel.setLayout(new BoxLayout(statisticsRepPanel, BoxLayout.Y_AXIS));
        p.add(repsJsp);

        wantStatisticsSummary = new JCheckBox("Include summary statistics", true);
        wantStatisticsSummary.setToolTipText("Include entries in output report");
        wantStatisticsSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantStatisticsSummary);

        summJsp = new JScrollPane(statisticsSummaryPanel = new JPanel());
        summJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsSummaryPanel.setLayout(new BoxLayout(statisticsSummaryPanel, BoxLayout.Y_AXIS));
        p.add(summJsp);

        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private void fillStatisticsPanel() {
        boolean bool = arb.isPrintStatisticsComments();
        wantStatisticsDescriptionAnalysis.setSelected(bool);
        statisticsComments.setText(arb.getStatisticsComments());
        statisticsConclusions.setText(arb.getStatisticsConclusions());
        statisticsComments.setEnabled(bool);
        statisticsConclusions.setEnabled(bool);

        bool = arb.isPrintReplicationStatistics();
        wantStatisticsReplications.setSelected(bool);
        bool = arb.isPrintSummaryStatistics();
        wantStatisticsSummary.setSelected(bool);

        List reps = arb.getStatisticsReplicationsList();
        statisticsRepPanel.removeAll();
        JLabel lab;
        JScrollPane jsp;
        JTable tab;

        statisticsRepPanel.add(lab = new JLabel("Replication Reports"));
        lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsRepPanel.add(Box.createVerticalStrut(10));
        String[] colNames = new String[] {"Run #", "Count", "Min", "Max", "Mean", "Std Deviation", "Variance"};

        for (Iterator repItr = reps.iterator(); repItr.hasNext();) {
            List r = (List) repItr.next();
            String nm = (String) r.get(0);
            String prop = (String) r.get(1);
            statisticsRepPanel.add(lab = new JLabel("Entity: " + nm));
            lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            statisticsRepPanel.add(lab = new JLabel("Property: " + prop));
            lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            List vals = (List) r.get(2);
            String[][] saa = new String[vals.size()][];
            int i = 0;
            for (Iterator r2 = vals.iterator(); r2.hasNext();) {
                saa[i++] = (String[]) r2.next();
            }
            statisticsRepPanel.add(jsp = new JScrollPane(tab = new ROTable(saa, colNames)));
            tab.setPreferredScrollableViewportSize(new Dimension(tab.getPreferredSize()));
            jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            statisticsRepPanel.add(Box.createVerticalStrut(20));
        }
        List summs = arb.getStastSummaryList();

        colNames = new String[] {"Entity", "Property", "# Runs", "Min", "Max", "Mean", "Std Deviation", "Variance"};
        String[][] saa = new String[summs.size()][];
        int i = 0;
        for (Iterator sumItr = summs.iterator(); sumItr.hasNext();) {
            saa[i++] = (String[]) sumItr.next();
        }

        statisticsSummaryPanel.removeAll();
        statisticsSummaryPanel.add(lab = new JLabel("Summary Report"));
        lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsSummaryPanel.add(Box.createVerticalStrut(10));

        statisticsSummaryPanel.add(jsp = new JScrollPane(tab = new ROTable(saa, colNames)));
        tab.setPreferredScrollableViewportSize(new Dimension(tab.getPreferredSize()));
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        repsJsp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        summJsp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
    }

    private void unFillStatisticsPanel() {
        arb.setPrintStatisticsComments(wantStatisticsDescriptionAnalysis.isSelected());
        arb.setStatisticsDescription(statisticsComments.getText());
        arb.setStatisticsConclusions(statisticsConclusions.getText());
        arb.setPrintReplicationStatistics(wantStatisticsReplications.isSelected());
        arb.setPrintSummaryStatistics(wantStatisticsSummary.isSelected());
    }
    JCheckBox wantConclusionsRecommendations;
    JTextArea conRecConclusionsTA;
    JTextArea conRecRecsTA;

    private JPanel makeConclusionsRecommendationsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantConclusionsRecommendations = new JCheckBox("Include conclusions and recommendations", true);
        wantConclusionsRecommendations.setToolTipText("Include entries in output report");
        wantConclusionsRecommendations.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantConclusionsRecommendations);

        JScrollPane jsp = new JScrollPane(conRecConclusionsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Conclusions"));
        p.add(jsp);

        jsp = new JScrollPane(conRecRecsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Recommendations for Future Work"));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(jsp);
        return p;
    }

    private void fillConclusionsRecommendationsPanel() {
        boolean bool = arb.isPrintRecommendationsConclusions();
        wantConclusionsRecommendations.setSelected(bool);
        conRecConclusionsTA.setText(arb.getConclusions());
        conRecConclusionsTA.setEnabled(bool);
        conRecRecsTA.setText(arb.getRecommendations());
        conRecRecsTA.setEnabled(bool);
    }

    private void unFillConRecPan() {
        arb.setPrintRecommendationsConclusions(wantConclusionsRecommendations.isSelected());
        arb.setConclusions(conRecConclusionsTA.getText());
        arb.setRecommendations(conRecRecsTA.getText());
    }

    private void buildMenus()
	{
        AnalystReportController analystReportController = (AnalystReportController) getController();

        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // Setup the File Menu
        myMenuBar = new JMenuBar();
        analystReportMenu.setMnemonic(KeyEvent.VK_N);

        analystReportMenu.add(buildMenuItem(analystReportController,
                AnalystReportController.OPEN_ANALYST_REPORT_METHOD,
                "Open Previous Analyst Report",
                KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask)));

        JMenuItem saveMI = buildMenuItem(analystReportController,
                AnalystReportController.SAVE_ANALYST_REPORT_METHOD,
                "Save Analyst Report",
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask));
        saveMI.setToolTipText("Save Analyst Report as XML file");
        analystReportMenu.add(saveMI);

		
        JMenuItem generateMI = buildMenuItem(analystReportController,
                AnalystReportController.GENERATE_ANALYST_REPORT_METHOD,
                "Display Analyst Report",
                KeyEvent.VK_D,
                KeyStroke.getKeyStroke(KeyEvent.VK_D, menuShortcutKeyMask));
        saveMI.setToolTipText("Save Analyst Report as HTML page");
        analystReportMenu.add(generateMI);

        JMenuItem viewAnalystReportXmlMI = buildMenuItem(analystReportController,
                AnalystReportController.VIEW_ANALYST_REPORT_XML_METHOD,
				"View analyst report XML",
                KeyEvent.VK_X,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask));
        viewAnalystReportXmlMI.setEnabled(false); // TODO:  implement listener and view functionality
        viewAnalystReportXmlMI.setToolTipText("TODO future capability");
        analystReportMenu.add(viewAnalystReportXmlMI);

        myMenuBar.add(analystReportMenu);
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel)
	{
        Action a = ActionIntrospector.getAction(source, method);
        Map<String, Object> map = new HashMap<>();
        if (mn != null) {
            map.put(Action.MNEMONIC_KEY, mn);
        }
        if (accel != null) {
            map.put(Action.ACCELERATOR_KEY, accel);
        }
        if (name != null) {
            map.put(Action.NAME, name);
        }
        if (!map.isEmpty()) {
            ActionUtilities.decorateAction(a, map);
        }

        return ActionUtilities.createMenuItem(a);
    }

    @Override
    public void modelChanged(mvcModelEvent event) {}

	/**
	 * @return the analystReportMenu
	 */
	public JMenu getFileMenu() {
		return analystReportMenu;
	}

	/**
	 * @param fileMenu the analystReportMenu to set
	 */
	public void setFileMenu(JMenu fileMenu) {
		this.analystReportMenu = fileMenu;
	}

    class fileChoiceListener implements ActionListener {

        JTextField tf;

        fileChoiceListener(JTextField tf) {
            this.tf = tf;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int resp = locationImageFileChooser.showOpenDialog(AnalystReportFrame.this);
            if (resp == JFileChooser.APPROVE_OPTION) {
                tf.setText(locationImageFileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }
}

class WrappingTextArea extends JTextArea {

    WrappingTextArea() {
        super(4, 20);
        setLineWrap(true);
        setWrapStyleWord(true);
    }
}

class ROTable extends JTable {

    ROTable(Vector v, Vector c) {
        super(v, c);
    }

    ROTable(Object[][] oa, Object[] cols) {
        super(oa, cols);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}

class EntityParamTable extends ROTable implements TableCellRenderer {

    TableCellRenderer defRenderer;

    EntityParamTable(Vector v, Vector c) {
        super(v, c);
        defRenderer = new DefaultTableCellRenderer();

        TableColumn tc = getColumnModel().getColumn(0);
        EntityParamTable instance = this;
        tc.setCellRenderer(instance);
    }
    Color grey = new Color(204, 204, 204);
    Color origBkgd;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = defRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (origBkgd == null) {
            origBkgd = c.getBackground();
        }
        Object o0 = getValueAt(row, 0);
        Object o1 = getValueAt(row, 1);
        Object o2 = getValueAt(row, 2);

        if (o0 != null && (o1 == null || ((CharSequence) o1).length() <= 0) && ((o2 == null || ((CharSequence) o2).length() <= 0))) {
            c.setBackground(grey);
        } else {
            c.setBackground(origBkgd);
        }
        return c;
    }
}
