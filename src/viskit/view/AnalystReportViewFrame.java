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
package viskit.view;

import actions.ActionIntrospector;
import actions.ActionUtilities;

import edu.nps.util.SpringUtilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.DateFormat;
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
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;
import static viskit.ViskitStatics.isFileReady;

import viskit.util.OpenAssembly;
import viskit.control.AnalystReportController;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.mvc.MvcModelEvent;
import viskit.model.AnalystReportModel;
import viskit.mvc.MvcController;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2006
 * @since 2:47:03 PM
 * @version $Id$
 */
public class AnalystReportViewFrame extends MvcAbstractViewFrame implements OpenAssembly.AssemblyChangeListener {

    static final Logger LOG = LogManager.getLogger();
    private final static String FRAME_DEFAULT_TITLE = "Viskit Analyst Report Editor";
    private AnalystReportModel analystReportModel;
    private JMenu analystReportMenu;

    /**
     * TODO: rewire this functionality?
     * boolean to show that raw report has not been saved to AnalystReports
     */
    private boolean dirty = false;
    private JMenuBar myMenuBar;
    private JFileChooser locationImageFileChooser;
    private static AnalystReportController analystReportController;
    
    JTextField titleTF = new JTextField();
    JTextField analystNameTF = new JTextField();
    // , "CONFIDENTIAL", "SECRET", "TOP SECRET"
    JComboBox<String> documentAccessLabelTF = new JComboBox<>(new String[] {"","Informational"}); // ,"CONTROLLED UNCLASSIFIED INFORMATION (CUI)"
    JTextField analysisDateTF = new JTextField(DateFormat.getDateInstance(DateFormat.LONG).format(new Date()));
    File currentAssemblyFile;

    public AnalystReportViewFrame()
    {
        super(FRAME_DEFAULT_TITLE); // necessary
        analystReportController = ViskitGlobals.instance().getAnalystReportController();
        initMVC((MvcController) analystReportController);
        initializeAnalystReportController(analystReportController); // TODO unscramble this hierarchy
                
        setLayout();
        setBackground(new Color(251, 251, 229)); // yellow
        buildMenus();

        locationImageFileChooser = new JFileChooser("./images/");
        
    }
    
    private void initMVC(MvcController mvcController) {
        setController(mvcController);
    }
    
    private void initializeAnalystReportController(AnalystReportController analystReportController) {
        setController(analystReportController);
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
                cntlr.setCurrentAssemblyFile(currentAssemblyFile);
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

    public boolean isReportDirty() {
        return dirty;
    }

    public void setReportDirty(boolean b) {
        dirty = b;
    }

    public void setReportBuilder(AnalystReportModel newAnalystReportModel) 
    {
        analystReportModel = newAnalystReportModel;
        setModel(analystReportModel); // hold on locally
        getController().setModel(getModel()); // tell controller
    }

    /** invokes _fillLayout() when Swing interface is ready */
    public void fillLayout() 
    {

        // We don't always come in on the swing thread.thread
        // TODO need to ensure that panel update occurs before Analyst Report is generated
        // Use invokeLater() , cannot call invokeAndWait from the event dispatcher 
        // https://stackoverflow.com/questions/5499921/invokeandwait-method-in-swingutilities
        // "Basically, everything that affects the GUI in any way musthappen on a single thread,
        //  This is because experience shows that a multi-threaded GUI is impossible to get rignt."
        
        SwingUtilities.invokeLater(() -> {
            _fillLayout();
            repaint();    // making sure
            revalidate();
            analystReportModel.announceAnalystReportReadyToView();
        });
    }

    private void _fillLayout() 
    {
        if (!isFileReady(analystReportModel.getAnalystReportXmlFile()))
        {
            LOG.error ("_fillLayout() problem, analystReportModel.getAnalystReportXmlFile() not ready");
            return;
        }
        try {
            fillHeader();
            fillExecutiveSummary();
            fillScenarioLocationPanel();
            fillSimulationConfigurationPanel();
            fillEntityParametersTablesPanel();
            fillBehaviorDescriptionsPanel();
            fillStatisticalResultsPanel();
            fillConclusionsRecommendationsPanel();
        }
        catch (Exception ex)
        {
            LOG.error (" _fillLayout() exception: " + ex.getMessage());
        }
    }

    public void unFillLayout() 
    {
        unFillHeader();
        unFillExecutiveSummary();
        unFillScenarioLocationPanel();
        unFillSimulationConfigurationPanel();
        unFillEntityParameterTablesPanel();
        unFillBehaviorDescriptionsPanel();
        unFillStatisticalResultsPanel();
        unFillConclusionsRecommendationsPanel();
    }

    private void fillHeader()
    {
              titleTF.setText(analystReportModel.getReportName());
        analystNameTF.setText(analystReportModel.getAuthor());
        String date = analystReportModel.getDateOfReport();
        if (date != null && date.length() > 0) {
            analysisDateTF.setText(date);
        } else {
            analysisDateTF.setText(DateFormat.getDateInstance().format(new Date()));
        } //now
        documentAccessLabelTF.setSelectedItem(analystReportModel.getDocumentAccessLabel());
    }

    private void unFillHeader() 
    {
        analystReportModel.setReportName(titleTF.getText());
        analystReportModel.setAuthor(analystNameTF.getText());
        analystReportModel.setDateOfReport(analysisDateTF.getText());
        analystReportModel.setDocumentAccessLabel((String) documentAccessLabelTF.getSelectedItem());
    }

    private void setLayout()
    {
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().setBackground(Color.white);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Color.white);

        JPanel headerPanel = new JPanel(new SpringLayout());
        headerPanel.add(new JLabel("Title"));
        headerPanel.add(titleTF);
        headerPanel.add(new JLabel("Author"));
        headerPanel.add(analystNameTF);
        JLabel analysisDateLabel = new JLabel("Analysis Date");
        analysisDateLabel.setToolTipText("Analysis Date");
        headerPanel.add(analysisDateLabel);
        headerPanel.add(analysisDateTF);
        JLabel documentAccessLabel = new JLabel("Document Access ");
        documentAccessLabel.setToolTipText("Document Access Label");
        headerPanel.add(documentAccessLabel);
        headerPanel.add(documentAccessLabelTF);
        
        Dimension d = new Dimension(Integer.MAX_VALUE, titleTF.getPreferredSize().height);
                titleTF.setMaximumSize(new Dimension(d));
          analystNameTF.setMaximumSize(new Dimension(d));
         analysisDateTF.setMaximumSize(new Dimension(d));
        documentAccessLabelTF.setMaximumSize(new Dimension(d));
        SpringUtilities.makeCompactGrid(headerPanel, 4, 2, 10, 10, 5, 5);
        
        /* TODO not yet working, overlapping :(
        // add help image to blank space on pane
        String  menuImageURL        = "doc/images/AnalystReportDisplayHtmlMenu.png"; // 985x376
        File    menuImageFile       = new File(menuImageURL);
        boolean menuImageFileExists = menuImageFile.exists(); // being careful
        if (menuImageFileExists)
        {
            // woof, an awful lot of work just to show an image
            // https://stackoverflow.com/questions/6714045/how-to-resize-jlabel-imageicon
            ImageIcon   menuImageIcon    = new ImageIcon(menuImageURL);
            Image           menuImage    = menuImageIcon.getImage();
            Image     scaledMenuImage    = menuImage.getScaledInstance(492, 188, java.awt.Image.SCALE_SMOOTH);
            ImageIcon scaledenuImageIcon = new ImageIcon(scaledMenuImage);
            JLabel menuImageLabel     = new JLabel(scaledenuImageIcon);
            menuImageLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            menuImageLabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
            JPanel menuImagePanel = new JPanel();
            menuImagePanel.add(menuImageLabel);
            headerPanel.add(Box.createVerticalStrut(10));
            headerPanel.add(menuImagePanel);
        }
        */

        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headerPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        headerPanel.setAlignmentY(JComponent.RIGHT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height));

        tabs.add("1 Header", headerPanel);
        tabs.add("2 Executive Summary",               makeExecutiveSummaryPanel());
        tabs.add("3 Scenario Location",               makeScenarioLocationPanel());
        tabs.add("4 Assembly Design",                 makeAssemblyDesignPanel());
        tabs.add("5 Entity Parameters",               makeEntityParametersTablesPanel());
        tabs.add("6 Behavior Descriptions",           makeBehaviorDescriptionsPanel());
        tabs.add("7 Statistical Results",             makeStatisticalResultsPanel());
        tabs.add("8 Conclusions and Recommendations", makeConclusionsRecommendationsPanel());

        add(tabs);
    //setBorder(new EmptyBorder(10,10,10,10));
    }
    JCheckBox showExecutiveSummaryCB;
    JTextArea executiveSummaryTA;

    private JPanel makeExecutiveSummaryPanel() 
    {
        JPanel executiveSummaryPanel = new JPanel();
        executiveSummaryPanel.setLayout(new BoxLayout(executiveSummaryPanel, BoxLayout.Y_AXIS));
        showExecutiveSummaryCB = new JCheckBox("Show Executive Summary", true);
        showExecutiveSummaryCB.setToolTipText("Show entries in output report");
        showExecutiveSummaryCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        executiveSummaryPanel.add(showExecutiveSummaryCB);

        JScrollPane jsp = new JScrollPane(executiveSummaryTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        executiveSummaryPanel.add(jsp);

        executiveSummaryTA.setLineWrap(true);
        executiveSummaryTA.setWrapStyleWord(true);
        executiveSummaryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return executiveSummaryPanel;
    }

    private void fillExecutiveSummary() 
    {
        showExecutiveSummaryCB.setSelected(analystReportModel.isShowExecutiveSummary());
        executiveSummaryTA.setText(analystReportModel.getExecutiveSummary());
        executiveSummaryTA.setEnabled(showExecutiveSummaryCB.isSelected());
    }

    private void unFillExecutiveSummary() 
    {
        analystReportModel.setShowExecutiveSummary(showExecutiveSummaryCB.isSelected());
        analystReportModel.setExecutiveSummary(executiveSummaryTA.getText());
    }
    /************************/
    JCheckBox showScenarioLocationDescriptionsCB;
    JCheckBox showScenarioLocationImagesCB;
    JTextArea scenarioLocationDesignConsiderationsTA, scenarioLocationConclusionsTA, scenarioLocationProductionNotesTA;
    JTextField scenarioLocationImageTF;
    JButton scenarioLocationImageButton;
    JTextField simulationChartImageTF;
    JButton simulationChartImageButton;

    private JPanel makeScenarioLocationPanel() 
    {
        JPanel scenarioLocationPanel = new JPanel();
        scenarioLocationPanel.setLayout(new BoxLayout(scenarioLocationPanel, BoxLayout.Y_AXIS));
        showScenarioLocationDescriptionsCB = new JCheckBox("Show location features and post-experiment descriptions", true);
        showScenarioLocationDescriptionsCB.setToolTipText("Show entries in output report");
        showScenarioLocationDescriptionsCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scenarioLocationPanel.add(showScenarioLocationDescriptionsCB);

        JScrollPane jsp = new JScrollPane(scenarioLocationDesignConsiderationsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Description of Location Features"));
        scenarioLocationPanel.add(jsp);

        jsp = new JScrollPane(scenarioLocationProductionNotesTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Production Notes"));
        scenarioLocationPanel.add(jsp);

        jsp = new JScrollPane(scenarioLocationConclusionsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Significant Location Features"));
        scenarioLocationPanel.add(jsp);

        showScenarioLocationImagesCB = new JCheckBox("Show location and chart image(s)", true);
        showScenarioLocationImagesCB.setToolTipText("Show entries in output report");
        showScenarioLocationImagesCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scenarioLocationPanel.add(showScenarioLocationImagesCB);

        JPanel imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Location image "));
        imp.add(scenarioLocationImageTF = new JTextField(20));
        imp.add(scenarioLocationImageButton = new JButton("..."));
        scenarioLocationImageButton.addActionListener(new fileChoiceListener(scenarioLocationImageTF));
        Dimension ps = scenarioLocationImageTF.getPreferredSize();
        scenarioLocationImageTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        scenarioLocationImageTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scenarioLocationPanel.add(imp);

        imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Chart image "));
        imp.add(simulationChartImageTF = new JTextField(20));
        imp.add(simulationChartImageButton = new JButton("..."));
        simulationChartImageButton.addActionListener(new fileChoiceListener(simulationChartImageTF));
        ps = simulationChartImageTF.getPreferredSize();
        simulationChartImageTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        simulationChartImageTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scenarioLocationPanel.add(imp);

        scenarioLocationPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        return scenarioLocationPanel;
    }

    private void fillScenarioLocationPanel()
    {
        showScenarioLocationDescriptionsCB.setSelected(analystReportModel.isShowScenarioLocationDescription());
        scenarioLocationDesignConsiderationsTA.setText(analystReportModel.getScenarioLocationDescription());
        scenarioLocationDesignConsiderationsTA.setEnabled(showScenarioLocationDescriptionsCB.isSelected());
        scenarioLocationProductionNotesTA.setText(analystReportModel.getScenarioLocationProductionNotes());
        scenarioLocationProductionNotesTA.setEnabled(showScenarioLocationDescriptionsCB.isSelected());
        scenarioLocationConclusionsTA.setText(analystReportModel.getScenarioLocationConclusions());
        scenarioLocationConclusionsTA.setEnabled(showScenarioLocationDescriptionsCB.isSelected());
        showScenarioLocationImagesCB.setSelected(analystReportModel.isShowScenarioLocationImage());
        scenarioLocationImageTF.setEnabled(showScenarioLocationImagesCB.isSelected());
        scenarioLocationImageButton.setEnabled(showScenarioLocationImagesCB.isSelected());
        simulationChartImageTF.setEnabled(showScenarioLocationImagesCB.isSelected());
        simulationChartImageButton.setEnabled(showScenarioLocationImagesCB.isSelected());
        scenarioLocationImageTF.setText(analystReportModel.getLocationImage());
        simulationChartImageTF.setText(analystReportModel.getChartImage());
    }

    private void unFillScenarioLocationPanel() 
    {
        analystReportModel.setScenarioLocationIncluded(showScenarioLocationDescriptionsCB.isSelected());
        analystReportModel.setScenarioLocationDescription(scenarioLocationDesignConsiderationsTA.getText());
        analystReportModel.setScenarioLocationProductionNotes(scenarioLocationProductionNotesTA.getText());
        analystReportModel.setScenarioLocationConclusions(scenarioLocationConclusionsTA.getText());
        analystReportModel.setShowScenarioLocationImage(showScenarioLocationImagesCB.isSelected());

        String s = scenarioLocationImageTF.getText().trim();
        if (s != null && !s.isEmpty())
            analystReportModel.setLocationImage(s);

        s = simulationChartImageTF.getText();
        if (s != null && !s.isEmpty())
            analystReportModel.setChartImage(s);
    }

    /************************/
    JCheckBox showAssemblyDesignConsiderationsAndAnalysisCB;
    JTextArea assemblyDesignConsiderationsTA, assemblyDesignConclusionsTA, assemblyDesignProductionNotesTA;
    JCheckBox showAssemblyConfigurationImageCB;
    JCheckBox showEntityDefinitionsTableCB;
    JTable entityDefinitionsTable;
    JTextField configurationImagePathTF;
    JButton configurationImageButton;

    private JPanel makeAssemblyDesignPanel()
    {
        JPanel assemblyDesignPanel = new JPanel();
        assemblyDesignPanel.setLayout(new BoxLayout(assemblyDesignPanel, BoxLayout.Y_AXIS));
        showAssemblyDesignConsiderationsAndAnalysisCB = new JCheckBox("Show Assembly Design Considerations, Production Notes, and Post-Experiment Analysis", true);
        showAssemblyDesignConsiderationsAndAnalysisCB.setToolTipText("Show entries in output report");
        showAssemblyDesignConsiderationsAndAnalysisCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        assemblyDesignPanel.add(showAssemblyDesignConsiderationsAndAnalysisCB);

        JScrollPane jsp = new JScrollPane(assemblyDesignConsiderationsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Assembly Design Considerations"));
        assemblyDesignPanel.add(jsp);

        jsp = new JScrollPane(assemblyDesignProductionNotesTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Production Notes"));
        assemblyDesignPanel.add(jsp);

        jsp = new JScrollPane(assemblyDesignConclusionsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Assembly Design"));
        assemblyDesignPanel.add(jsp);

        showEntityDefinitionsTableCB = new JCheckBox("Show entity definition table", true);
        showEntityDefinitionsTableCB.setToolTipText("Show entries in output report");
        showEntityDefinitionsTableCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        assemblyDesignPanel.add(showEntityDefinitionsTableCB);

        JPanel pp = new JPanel();
        pp.setLayout(new BoxLayout(pp, BoxLayout.X_AXIS));
        pp.add(Box.createHorizontalGlue());
        pp.add(new JScrollPane(entityDefinitionsTable = new JTable()));
        pp.add(Box.createHorizontalGlue());
        pp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        assemblyDesignPanel.add(pp);
        //p.add(new JScrollPane(entityDefinitionsTable = new JTable()));
        entityDefinitionsTable.setPreferredScrollableViewportSize(new Dimension(550, 120));

        showAssemblyConfigurationImageCB = new JCheckBox("Show Assembly Configuration Image", true);
        showAssemblyConfigurationImageCB.setToolTipText("Show entries in output report");
        showAssemblyConfigurationImageCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        assemblyDesignPanel.add(showAssemblyConfigurationImageCB);

        JPanel imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Configuration image: "));
        imp.add(configurationImagePathTF = new JTextField(20));
        imp.add(configurationImageButton = new JButton("..."));
        configurationImageButton.addActionListener(new fileChoiceListener(configurationImagePathTF));
        Dimension ps = configurationImagePathTF.getPreferredSize();
        configurationImagePathTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        configurationImagePathTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        assemblyDesignPanel.add(imp);

        assemblyDesignPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return assemblyDesignPanel;
    }

    private void fillSimulationConfigurationPanel() 
    {
        showAssemblyDesignConsiderationsAndAnalysisCB.setSelected(analystReportModel.isShowAssemblyConfigurationDescription());
        assemblyDesignConsiderationsTA.setText(analystReportModel.getConfigurationComments());
        assemblyDesignConsiderationsTA.setEnabled(showAssemblyDesignConsiderationsAndAnalysisCB.isSelected());

        showEntityDefinitionsTableCB.setSelected(analystReportModel.isShowAssemblyEntityDefinitionsTable());

        String[][] stringArrayArray = analystReportModel.getAssemblyDesignEntityDefinitionsTable();
        entityDefinitionsTable.setModel(new DefaultTableModel(stringArrayArray, new String[] {"Entity Name", "Behavior Type"}));
        entityDefinitionsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        entityDefinitionsTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        assemblyDesignProductionNotesTA.setText(analystReportModel.getAssemblyConfigurationProductionNotes());
        assemblyDesignProductionNotesTA.setEnabled(analystReportModel.isShowAssemblyConfigurationDescription());

        assemblyDesignConclusionsTA.setText(analystReportModel.getAssemblyConfigurationConclusions());
        assemblyDesignConclusionsTA.setEnabled(analystReportModel.isShowAssemblyConfigurationDescription());

        showAssemblyConfigurationImageCB.setSelected(analystReportModel.isShowAssemblyImage());
        configurationImageButton.setEnabled(showAssemblyConfigurationImageCB.isSelected());
        configurationImagePathTF.setEnabled(showAssemblyConfigurationImageCB.isSelected());
        configurationImagePathTF.setText(analystReportModel.getAssemblyImageLocation());
    }

    private void unFillSimulationConfigurationPanel() 
    {
        analystReportModel.setAssemblyConfigurationIncluded(showAssemblyDesignConsiderationsAndAnalysisCB.isSelected());
        analystReportModel.setAssemblyDesignConsiderations(assemblyDesignConsiderationsTA.getText());
        analystReportModel.setAssemblyConfigationProductionNotes(assemblyDesignProductionNotesTA.getText());
        analystReportModel.setAssemblyConfigurationConclusions(assemblyDesignConclusionsTA.getText());
        analystReportModel.setShowAssemblyEntityDefinitionsTable(showEntityDefinitionsTableCB.isSelected());
        analystReportModel.setShowAssemblyConfigurationImage(showAssemblyConfigurationImageCB.isSelected());
        String s = configurationImagePathTF.getText();
        if (s != null && s.length() > 0) {
            analystReportModel.setAssemblyImageLocation(s);
        }
    }

    JCheckBox showEntityParametersOverviewCB;
    JTextArea     entityParametersOverviewTA;
    JScrollPane   entityParametersOverviewSP;
    JCheckBox showEntityParametersTablesCB;
    JTabbedPane   entityParametersTablesTabbedPane;

    private JPanel makeEntityParametersTablesPanel()
    {
        JPanel entityParametersTablesPanel = new JPanel();
        entityParametersTablesPanel.setLayout(new BoxLayout(entityParametersTablesPanel, BoxLayout.Y_AXIS));
        showEntityParametersOverviewCB = new JCheckBox("Show Entity Parameters Overview", true);
        showEntityParametersOverviewCB.setToolTipText("Show overview in output report");
        showEntityParametersOverviewCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        entityParametersTablesPanel.add(showEntityParametersOverviewCB);

        entityParametersOverviewTA = new WrappingTextArea();
        entityParametersOverviewSP = new JScrollPane(entityParametersOverviewTA);
        entityParametersOverviewSP.setBorder(new TitledBorder("Entity Parameters Overview"));
        entityParametersOverviewSP.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        entityParametersTablesPanel.add(entityParametersOverviewSP);

        showEntityParametersTablesCB = new JCheckBox("Show Entity Parameter Tables", true);
        showEntityParametersTablesCB.setToolTipText("Show entries in output report");
        showEntityParametersTablesCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        entityParametersTablesPanel.add(showEntityParametersTablesCB);

        // TODO: post-experiment

        entityParametersTablesTabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        entityParametersTablesTabbedPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        entityParametersTablesPanel.add(entityParametersTablesTabbedPane);
        entityParametersTablesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return entityParametersTablesPanel;
    }

    @SuppressWarnings("unchecked")
    private void fillEntityParametersTablesPanel() 
    {
        showEntityParametersOverviewCB.setSelected(analystReportModel.isShowEntityParametersOverview());
        showEntityParametersTablesCB.setSelected  (analystReportModel.isShowEntityParametersTables());

            entityParametersOverviewTA.setText    (analystReportModel.getEntityParametersOverview());

        Vector<String> colNames = new Vector<>();
        colNames.add("category");
        colNames.add("name");
        colNames.add("description");

        Vector<Object[]> v = analystReportModel.getEntityParametersTables();

        for (Object[] oa : v) {
            Vector<Vector<String>> tableVector = new Vector<>();
            String nm = (String) oa[0];
            Vector<Object[]> v0 = (Vector) oa[1];
            v0.forEach(oa0 -> {
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
            });

            entityParametersTablesTabbedPane.add(nm, new JScrollPane(new EntityParameterTable(tableVector, colNames)));
        }
    }

    private void unFillEntityParameterTablesPanel() 
    {
        analystReportModel.setShowEntityParametersOverview(showEntityParametersOverviewCB.isSelected());
        analystReportModel.setShowEntityParametersTables(showEntityParametersTablesCB.isSelected());
        analystReportModel.setEntityParametersOverview(entityParametersOverviewTA.getText());
    }
    JCheckBox   showBehaviorDesignAnalysisDescriptions;
    JCheckBox   showBehaviorDescriptionsCB;
    JCheckBox   showBehaviorImagesCB;
    JTextArea   behaviorDescriptionsTA;
    JTextArea   behaviorConclusionsTA;
    JTabbedPane behaviorTabbedPane;

    private JPanel makeBehaviorDescriptionsPanel() 
    {
        JPanel behaviorDescriptionsPanel = new JPanel();
        behaviorDescriptionsPanel.setLayout(new BoxLayout(behaviorDescriptionsPanel, BoxLayout.Y_AXIS));
        showBehaviorDesignAnalysisDescriptions = new JCheckBox("Show Event Graph Behavior Considerations and Post-Experiment Analysis", true);
        showBehaviorDesignAnalysisDescriptions.setToolTipText("Show entries in output report");
        showBehaviorDesignAnalysisDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        behaviorDescriptionsPanel.add(showBehaviorDesignAnalysisDescriptions);

        JScrollPane scrollPane = new JScrollPane(behaviorDescriptionsTA = new WrappingTextArea());
        scrollPane.setBorder(new TitledBorder("Event Graph Behavior Design Descriptions"));
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        behaviorDescriptionsPanel.add(scrollPane);

        scrollPane = new JScrollPane(behaviorConclusionsTA = new WrappingTextArea());
        scrollPane.setBorder(new TitledBorder("Post-Experiment Analysis of Event Graph Entity Behaviors"));
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        behaviorDescriptionsPanel.add(scrollPane);

        showBehaviorDescriptionsCB = new JCheckBox("Show Event Graph Behavior Descriptions", true);
        showBehaviorDescriptionsCB.setToolTipText("Show entries in output report");
        showBehaviorDescriptionsCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        behaviorDescriptionsPanel.add(showBehaviorDescriptionsCB);

        showBehaviorImagesCB = new JCheckBox("Show Event Graph Behavior Images", true);
        showBehaviorImagesCB.setToolTipText("Show entries in output report");
        showBehaviorImagesCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        behaviorDescriptionsPanel.add(showBehaviorImagesCB);

        behaviorTabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        behaviorTabbedPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        behaviorDescriptionsPanel.add(behaviorTabbedPane);
        behaviorDescriptionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        return behaviorDescriptionsPanel;
    }

    private void unFillBehaviorDescriptionsPanel()
    {
        analystReportModel.setShowBehaviorDesignAnalysisDescriptions(showBehaviorDesignAnalysisDescriptions.isSelected());
        analystReportModel.setBehaviorDescription(behaviorDescriptionsTA.getText());
        analystReportModel.setBehaviorConclusions(behaviorConclusionsTA.getText());
        analystReportModel.setShowBehaviorDescriptions(showBehaviorDescriptionsCB.isSelected());
        analystReportModel.setShowEventGraphImages(showBehaviorImagesCB.isSelected());

        // tables are uneditable
    }

    private void fillBehaviorDescriptionsPanel() 
    {
        showBehaviorDesignAnalysisDescriptions.setSelected(analystReportModel.isShowBehaviorDesignAnalysisDescriptions());
        behaviorDescriptionsTA.setText(analystReportModel.getBehaviorDescription());
        behaviorDescriptionsTA.setEnabled(showBehaviorDesignAnalysisDescriptions.isSelected());
        behaviorConclusionsTA.setText(analystReportModel.getBehaviorConclusions());
        behaviorConclusionsTA.setEnabled(showBehaviorDesignAnalysisDescriptions.isSelected());
        showBehaviorImagesCB.setEnabled(analystReportModel.isShowEventGraphImage());
        showBehaviorDescriptionsCB.setSelected(analystReportModel.isShowBehaviorDescriptions());
        behaviorTabbedPane.setEnabled(showBehaviorDescriptionsCB.isSelected());

        // TODO: JDOM v1.1 does not yet support generics
        List behaviorList = analystReportModel.getBehaviorList();

        behaviorTabbedPane.removeAll();
        for (Iterator itr = behaviorList.iterator(); itr.hasNext();) 
        {
            List nextBehavior = (List) itr.next();
            String behaviorName = (String) nextBehavior.get(0);
            String behaviorDescription = (String) nextBehavior.get(1);
            List behaviorParameters = (List) nextBehavior.get(2);
            List behaviorStateVariables = (List) nextBehavior.get(3);
            String behaviorImagePath = (String) nextBehavior.get(4);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel label = new JLabel("Description:  " + behaviorDescription);
            label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            panel.add(label);

            label = new JLabel("Image location:  " + behaviorImagePath);
            panel.add(label);

            Vector<String> columns = new Vector<>(3);
            columns.add("name");
            columns.add("type");
            columns.add("description");

            Vector<Vector<String>> data = new Vector<>(behaviorParameters.size());
            for (Object behaviorParameter : behaviorParameters) 
            {
                String[] stringArray = (String[]) behaviorParameter;
                Vector<String> row = new Vector<>(3);
                row.add(stringArray[0]);
                row.add(stringArray[1]);
                row.add(stringArray[2]);
                data.add(row);
            }
            JScrollPane scrollPane = new JScrollPane(new ROTable(data, columns));
            scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            scrollPane.setBorder(new TitledBorder("Parameters"));
            panel.add(scrollPane);

            data = new Vector<>(behaviorStateVariables.size());
            for (Object behaviorStateVariable : behaviorStateVariables) 
            {
                String[] stringArray = (String[]) behaviorStateVariable;
                Vector<String> row = new Vector<>(3);
                row.add(stringArray[0]);
                row.add(stringArray[1]);
                row.add(stringArray[2]);
                data.add(row);
            }
            scrollPane = new JScrollPane(new ROTable(data, columns));
            scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            scrollPane.setBorder(new TitledBorder("State variables"));
            panel.add(scrollPane);

            behaviorTabbedPane.add(behaviorName, panel);
        }
    }
    JCheckBox showStatisticsDescriptionAnalysisCB;
    JCheckBox showReplicationStatisticsCB;
    JCheckBox showSummaryStatisticsCB;
    JTextArea statisticsDescription;
    JTextArea statisticsConclusions;
    JPanel statisticsSummaryPanel;
    JPanel statisticsReplicationReportsPanel;
    JScrollPane replicationStatisticsScrollPane;
    JScrollPane statisticsSummaryScrollPane;

    private JPanel makeStatisticalResultsPanel() 
    {
        JPanel statisticalResultsPanel = new JPanel();
        statisticalResultsPanel.setLayout(new BoxLayout(statisticalResultsPanel, BoxLayout.Y_AXIS));
        showStatisticsDescriptionAnalysisCB = new JCheckBox("Show statistical description and analysis", true);
        showStatisticsDescriptionAnalysisCB.setToolTipText("Show entries in output report");
        showStatisticsDescriptionAnalysisCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticalResultsPanel.add(showStatisticsDescriptionAnalysisCB);

        JScrollPane scrollPane = new JScrollPane(statisticsDescription = new WrappingTextArea());
        scrollPane.setBorder(new TitledBorder("Description of Expected Results"));
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticalResultsPanel.add(scrollPane);

        scrollPane = new JScrollPane(statisticsConclusions = new WrappingTextArea());
        scrollPane.setBorder(new TitledBorder("Analysis of Experimental Results and Conclusions"));
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticalResultsPanel.add(scrollPane);

        showReplicationStatisticsCB = new JCheckBox("Show replication statistics", true);
        showReplicationStatisticsCB.setToolTipText("Show entries in output report");
        showReplicationStatisticsCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticalResultsPanel.add(showReplicationStatisticsCB);

        replicationStatisticsScrollPane = new JScrollPane(statisticsReplicationReportsPanel = new JPanel());
        // TODO limit initial height replicationStatisticsScrollPane, perhaps multi-part split pane?
        replicationStatisticsScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsReplicationReportsPanel.setLayout(new BoxLayout(statisticsReplicationReportsPanel, BoxLayout.Y_AXIS));
        statisticalResultsPanel.add(replicationStatisticsScrollPane);

        showSummaryStatisticsCB = new JCheckBox("Show summary statistics", true);
        showSummaryStatisticsCB.setToolTipText("Show entries in output report");
        showSummaryStatisticsCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticalResultsPanel.add(showSummaryStatisticsCB);

        statisticsSummaryScrollPane = new JScrollPane(statisticsSummaryPanel = new JPanel());
        statisticsSummaryScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsSummaryPanel.setLayout(new BoxLayout(statisticsSummaryPanel, BoxLayout.Y_AXIS));
        statisticalResultsPanel.add(statisticsSummaryScrollPane);

        statisticalResultsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return statisticalResultsPanel;
    }

    private void fillStatisticalResultsPanel() 
    {
        boolean value = analystReportModel.isShowStatiisticalResultsDescriptionAnalysis();
        showStatisticsDescriptionAnalysisCB.setSelected(value);
        statisticsDescription.setText(analystReportModel.getStatisticsDescription());
        statisticsConclusions.setText(analystReportModel.getStatisticsConclusions());
        statisticsDescription.setEnabled(value);
        statisticsConclusions.setEnabled(value);

        value = analystReportModel.isShowReplicationStatistics();
        showReplicationStatisticsCB.setSelected(value);
        value = analystReportModel.isShowSummaryStatistics();
        showSummaryStatisticsCB.setSelected(value);

        List statisticsReplicationsList = analystReportModel.getStatisticsReplicationsList();
        statisticsReplicationReportsPanel.removeAll();
        JLabel label;
        JScrollPane scrollPane;
        JTable table;

        statisticsReplicationReportsPanel.add(label = new JLabel("Replication Reports"));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsReplicationReportsPanel.add(Box.createVerticalStrut(10));
        String[] columnNames = new String[] {"Replication #", "Count", "Minimum", "Maximum", "Mean", "Standard Deviation", "Variance"};

        for (Iterator replicationsListIterator = statisticsReplicationsList.iterator(); replicationsListIterator.hasNext();) 
        {
            List replication = (List) replicationsListIterator.next();
            String entityName = (String) replication.get(0); // TODO not getting set...
            String property = (String) replication.get(1);
            statisticsReplicationReportsPanel.add(label = new JLabel("Entity: " + entityName));
            label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            statisticsReplicationReportsPanel.add(label = new JLabel("Property: " + property));
            label.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            List vals = (List) replication.get(2);
            String[][] saa = new String[vals.size()][];
            int i = 0;
            for (Iterator r2 = vals.iterator(); r2.hasNext();) {
                saa[i++] = (String[]) r2.next();
            }
            statisticsReplicationReportsPanel.add(scrollPane = new JScrollPane(table = new ROTable(saa, columnNames)));
            table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredSize()));
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            statisticsReplicationReportsPanel.add(Box.createVerticalStrut(20));
        }
        List statisticsSummaryList = analystReportModel.getStatisticsSummaryList();

        columnNames = new String[] {"Entity", "Property", "# Replications", "Minimum", "Maximum", "Mean", "Standard Deviation", "Variance"};
        String[][] saa = new String[statisticsSummaryList.size()][];
        int i = 0;
        for (Iterator statisticsSummaryIterator = statisticsSummaryList.iterator(); statisticsSummaryIterator.hasNext();) {
            saa[i++] = (String[]) statisticsSummaryIterator.next();
        }

        statisticsSummaryPanel.removeAll();
        statisticsSummaryPanel.add(label = new JLabel("Summary Report"));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsSummaryPanel.add(Box.createVerticalStrut(10));

        statisticsSummaryPanel.add(scrollPane = new JScrollPane(table = new ROTable(saa, columnNames)));
        table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredSize()));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        replicationStatisticsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        statisticsSummaryScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
    }

    private void unFillStatisticalResultsPanel()
    {
        analystReportModel.setShowStatisticsDescriptionAnalysis(showStatisticsDescriptionAnalysisCB.isSelected());
        analystReportModel.setStatisticsDescription(statisticsDescription.getText());
        analystReportModel.setStatisticsConclusions(statisticsConclusions.getText());
        analystReportModel.setShowReplicationStatistics(showReplicationStatisticsCB.isSelected());
        analystReportModel.setShowSummaryStatistics(showSummaryStatisticsCB.isSelected());
    }
    
    JCheckBox showConclusionsRecommendationsCB;
    JTextArea conRecConclusionsTA;
    JTextArea conRecRecommendationsTA;

    private JPanel makeConclusionsRecommendationsPanel() 
    {
        JPanel conclusionsRecommendationsPanel = new JPanel();
        conclusionsRecommendationsPanel.setLayout(new BoxLayout(conclusionsRecommendationsPanel, BoxLayout.Y_AXIS));
        showConclusionsRecommendationsCB = new JCheckBox("Show Conclusions and Recommendations for Future Work", true);
        showConclusionsRecommendationsCB.setToolTipText("Show entries in output report");
        showConclusionsRecommendationsCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        conclusionsRecommendationsPanel.add(showConclusionsRecommendationsCB);

        JScrollPane scrollPane = new JScrollPane(conRecConclusionsTA = new WrappingTextArea());
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrollPane.setBorder(new TitledBorder("Conclusions"));
        conclusionsRecommendationsPanel.add(scrollPane);

        scrollPane = new JScrollPane(conRecRecommendationsTA = new WrappingTextArea());
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrollPane.setBorder(new TitledBorder("Recommendations for Future Work"));
        conclusionsRecommendationsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        conclusionsRecommendationsPanel.add(scrollPane);
        return conclusionsRecommendationsPanel;
    }

    private void fillConclusionsRecommendationsPanel() 
    {
        boolean bool = analystReportModel.isShowRecommendationsConclusions();
        showConclusionsRecommendationsCB.setSelected(bool);
        conRecConclusionsTA.setText(analystReportModel.getConclusions());
        conRecConclusionsTA.setEnabled(bool);
        conRecRecommendationsTA.setText(analystReportModel.getRecommendations());
        conRecRecommendationsTA.setEnabled(bool);
    }

    private void unFillConclusionsRecommendationsPanel() {
        analystReportModel.setShowRecommendationsConclusions(showConclusionsRecommendationsCB.isSelected());
        analystReportModel.setConclusions(conRecConclusionsTA.getText());
        analystReportModel.setRecommendations(conRecRecommendationsTA.getText());
    }

    private void buildMenus()
    {
        if (analystReportController == null)
            analystReportController = (AnalystReportController) getController();

        int accelMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Setup the Analyst Report Menu
        myMenuBar = new JMenuBar(); // TODO omit completely when refactoring complete
//      JMenu fileMenu = new JMenu("File");
        analystReportMenu = new JMenu("Analyst Report");
        getAnalystReportMenu().setMnemonic(KeyEvent.VK_R);

        getAnalystReportMenu().add(buildMenuItem(analystReportController,
                "generateHtmlReport",
                "Display HTML Analyst Report",
                KeyEvent.VK_H, // use H for consistency with hotkey
                KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));

        getAnalystReportMenu().add(buildMenuItem(analystReportController,
                "openAnalystReportXML",
                "Open another Analyst Report XML",
                KeyEvent.VK_O,
                null));
        
        getAnalystReportMenu().add(buildMenuItem(analystReportController,
                "saveAnalystReportXML",
                "Save Analyst Report XML",
                KeyEvent.VK_S,
                null));

        getAnalystReportMenu().add(buildMenuItem(analystReportController, 
                "viewXML", 
                "XML View of Saved Analyst Report",
                KeyEvent.VK_X, 
                null));

        myMenuBar.add(getAnalystReportMenu());
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel)
    {
        Action action = ActionIntrospector.getAction(source, method);
        if (action == null)
        {
            LOG.error("buildMenuItem reflection failed for name=" + name + " method=" + method + " in " + source.toString());
            return new JMenuItem(name + "(not working, reflection failed) ");
        }
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
            ActionUtilities.decorateAction(action, map);
        }

        return ActionUtilities.createMenuItem(action);
    }

    @Override
    public void modelChanged(MvcModelEvent event) {}

    class fileChoiceListener implements ActionListener {

        JTextField tf;

        fileChoiceListener(JTextField tf) {
            this.tf = tf;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int resp = locationImageFileChooser.showOpenDialog(AnalystReportViewFrame.this);
            if (resp == JFileChooser.APPROVE_OPTION) {
                tf.setText(locationImageFileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    /**
     * @return the analystReportMenu
     */
    public JMenu getAnalystReportMenu() {
        return analystReportMenu;
    }

    /**
     * @return the analystReportController
     */
    public AnalystReportController getAnalystReportController() {
        return analystReportController;
    }

    /**
     * @param analystReportController the analystReportController to set
     */
    public void setAnalystReportController(AnalystReportController analystReportController) {
        AnalystReportViewFrame.analystReportController = analystReportController;
    }
}

class WrappingTextArea extends JTextArea {

    WrappingTextArea() {
        super(4, 20);
        setLineWrap(true);
        setWrapStyleWord(true);
    }
}

class ROTable extends JTable 
{
    // https://stackoverflow.com/questions/3467052/set-right-alignment-in-jtable-column
    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();

    ROTable(Vector<? extends Vector> v, Vector<?> c) 
    {
        // initializer
        super(v, c);
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
    }
    
    @Override
    public TableCellRenderer getCellRenderer (int arg0, int arg1) {
        return rightRenderer;
    }

    ROTable(Object[][] oa, Object[] cols)
    {
        // initializer
        super(oa, cols);
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}

class EntityParameterTable extends ROTable implements TableCellRenderer {

    TableCellRenderer defRenderer;

    EntityParameterTable(Vector<? extends Vector> v, Vector<?> c) {
        super(v, c);
        defRenderer = new DefaultTableCellRenderer();

        TableColumn tc = getColumnModel().getColumn(0);
        EntityParameterTable instance = this;
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
