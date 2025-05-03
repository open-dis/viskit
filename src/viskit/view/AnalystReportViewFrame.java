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
import viskit.ViskitStatics;
import static viskit.ViskitStatics.isFileReady;

import viskit.util.OpenAssembly;
import viskit.control.AnalystReportController;
import static viskit.control.AnalystReportController.METHOD_generateHtmlReport;
import static viskit.control.AnalystReportController.METHOD_saveAnalystReportXML;
import static viskit.control.AnalystReportController.METHOD_openAnalystReportXML;
import static viskit.control.AnalystReportController.METHOD_viewXML;
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
public class AnalystReportViewFrame extends MvcAbstractViewFrame implements OpenAssembly.AssemblyChangeListener 
{
    static final Logger LOG = LogManager.getLogger();
    
    private final static String FRAME_DEFAULT_TITLE = "Viskit Analyst Report Editor";
    private AnalystReportModel analystReportModel;
    private JTabbedPane tabbedPane;
    private JMenu analystReportMenu;
    
    JPanel headerPanel,                 executiveSummaryPanel,       scenarioLocationPanel,     
           simulationConfigurationAssemblyDesignPanel,               entityParametersTablesPanel, 
           behaviorDescriptionsPanel,   statisticalResultsPanel,     conclusionsRecommendationsPanel;

    /**
     * TODO: rewire this functionality?
     * boolean to show that raw report has not been saved to AnalystReports
     */
    private boolean dirty = false;
    private JMenuBar myMenuBar;
    private JFileChooser locationImageFileChooser;
    private static AnalystReportController analystReportController;
    
    JTextField titleTF       = new JTextField();
    JTextField analystNameTF = new JTextField();
    
    // , "CONFIDENTIAL", "SECRET", "TOP SECRET"
    JComboBox<String> documentAccessRightsLabelTF = new JComboBox<>(new String[] {"","Informational"}); // ,"CONTROLLED UNCLASSIFIED INFORMATION (CUI)"
    
    JTextField analysisDateTF = new JTextField(DateFormat.getDateInstance(DateFormat.LONG).format(new Date()));
    File currentAssemblyFile;

    public AnalystReportViewFrame()
    {
        super(FRAME_DEFAULT_TITLE); // necessary
        if (analystReportController == null)
            analystReportController = ViskitGlobals.instance().getAnalystReportController();
        initializeMVC((MvcController) analystReportController);
        initializeAnalystReportController(analystReportController); // TODO unscramble this hierarchy

        if (tabbedPane == null)
        {
            tabbedPane = new JTabbedPane();
            tabbedPane.setBackground(Color.white);
            setLayout(); // initialize panel
            setContentPane(tabbedPane); // not add!
        }
        setBackground(new Color(251, 251, 229)); // yellow
        buildMenus();

        locationImageFileChooser = new JFileChooser("./images/");
    }
    
    private void initializeMVC(MvcController mvcController) {
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
    public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) 
    {
        switch (action) {
            case NEW_ASSEMBLY:
                currentAssemblyFile = (File) param;
                analystReportController = (AnalystReportController) getController();
                if ((analystReportController != null) && (analystReportController.getAnalystReportModel() != null) && (currentAssemblyFile != null))
                    analystReportController.setCurrentAssemblyFile(currentAssemblyFile);
                break;

            case CLOSE_ASSEMBLY:
            case PARAMETER_LOCALLY_EDITED:
            case JAXB_CHANGED:
                break;

            default:
                LOG.error("Program error AnalystReportFrame.assemblyChanged(" + action + ")");
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

    /** update analystReportModel data using values from AnalystReport user interface */
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

    /** update AnalystReport user interface using analystReportModel values */
    private void fillHeader()
    {
        try {
                  titleTF.setText(analystReportModel.getReportName());
            analystNameTF.setText(analystReportModel.getAuthor());
            String date = analystReportModel.getDateOfReport();
            if (date != null && date.length() > 0) {
                analysisDateTF.setText(date);
            } 
            else 
            {
                analysisDateTF.setText(DateFormat.getDateInstance().format(new Date())); // now
            }
            documentAccessRightsLabelTF.setSelectedItem(analystReportModel.getDocumentAccessRights());
        }
        catch (Exception ex)
        {
            LOG.error("fillHeader() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
    private void unFillHeader() 
    {
        analystReportModel.setReportName(                                     titleTF.getText());
        analystReportModel.setAuthor(                                   analystNameTF.getText());
        analystReportModel.setDateOfReport(                            analysisDateTF.getText());
        analystReportModel.setDocumentAccessRights((String) documentAccessRightsLabelTF.getSelectedItem());
    }

    private void setLayout()
    {
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().setBackground(Color.white);
        
        if (tabbedPane.getTabCount() == 0)
        {
            tabbedPane.removeAll(); // reset
            tabbedPane.add("1 Header",                          makeHeaderPanel());
            tabbedPane.add("2 Executive Summary",               makeExecutiveSummaryPanel());
            tabbedPane.add("3 Scenario Location",               makeScenarioLocationPanel());
            tabbedPane.add("4 Simulation Configuration",        makeSimulationConfigurationAssemblyDesignPanel());
            tabbedPane.add("5 Entity Parameters",               makeEntityParametersTablesPanel());
            tabbedPane.add("6 Behavior Descriptions",           makeBehaviorDescriptionsPanel());
            tabbedPane.add("7 Statistical Results",             makeStatisticalResultsPanel());
            tabbedPane.add("8 Conclusions and Recommendations", makeConclusionsRecommendationsPanel());
        }
        else
        {
            tabbedPane.setComponentAt(1, makeHeaderPanel());
            tabbedPane.setComponentAt(2, makeExecutiveSummaryPanel());
            tabbedPane.setComponentAt(3, makeScenarioLocationPanel());
            tabbedPane.setComponentAt(4, makeSimulationConfigurationAssemblyDesignPanel());
            tabbedPane.setComponentAt(5, makeEntityParametersTablesPanel());
            tabbedPane.setComponentAt(6, makeBehaviorDescriptionsPanel());
            tabbedPane.setComponentAt(7, makeStatisticalResultsPanel());
            tabbedPane.setComponentAt(8, makeConclusionsRecommendationsPanel());
        }


    //setBorder(new EmptyBorder(10,10,10,10));
    }
    JCheckBox showExecutiveSummaryCB;
    JTextArea executiveSummaryTA;

    /** user interface creation */
    private JPanel makeHeaderPanel()
    {
        if (headerPanel == null)
            headerPanel = new JPanel(new SpringLayout());
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
        headerPanel.add(documentAccessRightsLabelTF);
        
        Dimension d = new Dimension(Integer.MAX_VALUE, titleTF.getPreferredSize().height);
                titleTF.setMaximumSize(new Dimension(d));
          analystNameTF.setMaximumSize(new Dimension(d));
         analysisDateTF.setMaximumSize(new Dimension(d));
        documentAccessRightsLabelTF.setMaximumSize(new Dimension(d));
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
        
        return headerPanel;
    }

    /** user interface creation */
    private JPanel makeExecutiveSummaryPanel() 
    {
        if (executiveSummaryPanel == null)
            executiveSummaryPanel = new JPanel();
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

    /** update AnalystReport user interface using analystReportModel values */
    private void fillExecutiveSummary() 
    {
        try {
            showExecutiveSummaryCB.setSelected(analystReportModel.isShowExecutiveSummary());
            executiveSummaryTA.setText(analystReportModel.getExecutiveSummary());
            executiveSummaryTA.setEnabled(showExecutiveSummaryCB.isSelected());
        }
        catch (Exception ex)
        {
            LOG.error("fillExecutiveSummary() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
    private void unFillExecutiveSummary() 
    {
        analystReportModel.setShowExecutiveSummary(showExecutiveSummaryCB.isSelected());
        analystReportModel.setExecutiveSummary(executiveSummaryTA.getText());
    }
    /************************/
    JCheckBox  showScenarioLocationDescriptionsCB;
    JCheckBox  showScenarioLocationImagesCB;
    JTextArea  scenarioLocationDesignConsiderationsTA, scenarioLocationConclusionsTA, scenarioLocationProductionNotesTA;
    JTextField scenarioLocationImageTF;
    JButton    scenarioLocationImageButton;
    JTextField simulationChartImageTF;
    JButton    simulationChartImageButton;

    /** user interface creation */
    private JPanel makeScenarioLocationPanel() 
    {
        if (scenarioLocationPanel == null)
            scenarioLocationPanel = new JPanel();
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
        jsp.setBorder(new TitledBorder("Simulation Configuration Production Notes"));
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

    /** update AnalystReport user interface using analystReportModel values */
    private void fillScenarioLocationPanel()
    {
        try
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
        catch (Exception ex)
        {
            LOG.error("fillScenarioLocationPanel() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
    private void unFillScenarioLocationPanel() 
    {
        try
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
        catch (Exception ex)
        {
            LOG.error("unFillScenarioLocationPanel() {}", ex);
        }
    }

    /************************/
    JCheckBox showSimulationConfigurationAndAnalysisCB;
    JTextArea simulationConfigurationTA, assemblyDesignConclusionsTA, assemblyDesignProductionNotesTA;
    JCheckBox showSimulationConfigurationAssemblyImageCB;
    JCheckBox showEntityDefinitionsTableCB;
    JTable entityDefinitionsTable;
    JTextField simulationConfigurationAssemblyImagePathTF;
    JButton simulationConfigurationAssemblyImageButton;

    /** user interface creation */
    private JPanel makeSimulationConfigurationAssemblyDesignPanel()
    {
        if (simulationConfigurationAssemblyDesignPanel == null)
            simulationConfigurationAssemblyDesignPanel = new JPanel();
        simulationConfigurationAssemblyDesignPanel.setLayout(new BoxLayout(simulationConfigurationAssemblyDesignPanel, BoxLayout.Y_AXIS));
        showSimulationConfigurationAndAnalysisCB = new JCheckBox("Show Simulation Configuration Considerations, Simulation Configuration Production Notes, and Post-Experiment Analysis", true);
        showSimulationConfigurationAndAnalysisCB.setToolTipText("Show entries in output report");
        showSimulationConfigurationAndAnalysisCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        simulationConfigurationAssemblyDesignPanel.add(showSimulationConfigurationAndAnalysisCB);

        JScrollPane scrollPane = new JScrollPane(simulationConfigurationTA = new WrappingTextArea());
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrollPane.setBorder(new TitledBorder("Simulation Configuration Considerations"));
        simulationConfigurationAssemblyDesignPanel.add(scrollPane);

        scrollPane = new JScrollPane(assemblyDesignProductionNotesTA = new WrappingTextArea());
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrollPane.setBorder(new TitledBorder("Simulation Configuration Production Notes"));
        simulationConfigurationAssemblyDesignPanel.add(scrollPane);

        scrollPane = new JScrollPane(assemblyDesignConclusionsTA = new WrappingTextArea());
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        scrollPane.setBorder(new TitledBorder("Post-Experiment Analysis of Simulation Configuration"));
        simulationConfigurationAssemblyDesignPanel.add(scrollPane);

        showEntityDefinitionsTableCB = new JCheckBox("Show entity definition table", true);
        showEntityDefinitionsTableCB.setToolTipText("Show entries in output report");
        showEntityDefinitionsTableCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        simulationConfigurationAssemblyDesignPanel.add(showEntityDefinitionsTableCB);

        JPanel pp = new JPanel();
        pp.setLayout(new BoxLayout(pp, BoxLayout.X_AXIS));
        pp.add(Box.createHorizontalGlue());
        pp.add(new JScrollPane(entityDefinitionsTable = new JTable()));
        pp.add(Box.createHorizontalGlue());
        pp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        simulationConfigurationAssemblyDesignPanel.add(pp);
        //p.add(new JScrollPane(entityDefinitionsTable = new JTable()));
        entityDefinitionsTable.setPreferredScrollableViewportSize(new Dimension(550, 120));

        showSimulationConfigurationAssemblyImageCB = new JCheckBox("Show Assembly Image for Simulation Configuration", true);
        showSimulationConfigurationAssemblyImageCB.setToolTipText("Show entries in output report");
        showSimulationConfigurationAssemblyImageCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        simulationConfigurationAssemblyDesignPanel.add(showSimulationConfigurationAssemblyImageCB);

        JPanel imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Configuration image: "));
        imp.add(simulationConfigurationAssemblyImagePathTF = new JTextField(20));
        imp.add(simulationConfigurationAssemblyImageButton = new JButton("..."));
        simulationConfigurationAssemblyImageButton.addActionListener(new fileChoiceListener(simulationConfigurationAssemblyImagePathTF));
        Dimension ps = simulationConfigurationAssemblyImagePathTF.getPreferredSize();
        simulationConfigurationAssemblyImagePathTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        simulationConfigurationAssemblyImagePathTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        simulationConfigurationAssemblyDesignPanel.add(imp);

        simulationConfigurationAssemblyDesignPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return simulationConfigurationAssemblyDesignPanel;
    }

    /** update AnalystReport user interface using analystReportModel values */
    private void fillSimulationConfigurationPanel() 
    {
        try
        {
            showSimulationConfigurationAndAnalysisCB.setSelected(analystReportModel.isShowAssemblyConfigurationDescription());
            simulationConfigurationTA.setText(analystReportModel.getConfigurationComments());
            simulationConfigurationTA.setEnabled(showSimulationConfigurationAndAnalysisCB.isSelected());

            showEntityDefinitionsTableCB.setSelected(analystReportModel.isShowAssemblyEntityDefinitionsTable());

            String[][] stringArrayArray = analystReportModel.getAssemblyDesignEntityDefinitionsTable();
            entityDefinitionsTable.setModel(new DefaultTableModel(stringArrayArray, new String[] {"Entity Name", "Behavior Type"}));
            entityDefinitionsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
            entityDefinitionsTable.getColumnModel().getColumn(1).setPreferredWidth(200);

            assemblyDesignProductionNotesTA.setText(analystReportModel.getAssemblyConfigurationProductionNotes());
            assemblyDesignProductionNotesTA.setEnabled(analystReportModel.isShowAssemblyConfigurationDescription());

            assemblyDesignConclusionsTA.setText(analystReportModel.getAssemblyConfigurationConclusions());
            assemblyDesignConclusionsTA.setEnabled(analystReportModel.isShowAssemblyConfigurationDescription());

            showSimulationConfigurationAssemblyImageCB.setSelected(analystReportModel.isShowAssemblyImage());
            simulationConfigurationAssemblyImageButton.setEnabled(showSimulationConfigurationAssemblyImageCB.isSelected());
            simulationConfigurationAssemblyImagePathTF.setEnabled(showSimulationConfigurationAssemblyImageCB.isSelected());
            simulationConfigurationAssemblyImagePathTF.setText(analystReportModel.getAssemblyImageLocation());
        }
        catch (Exception ex)
        {
            LOG.error("fillSimulationConfigurationPanel() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
    private void unFillSimulationConfigurationPanel() 
    {
        analystReportModel.setShowSimulationConfiguration(showSimulationConfigurationAndAnalysisCB.isSelected());
        analystReportModel.setSimulationConfigurationConsiderations(simulationConfigurationTA.getText());
        analystReportModel.setSimulationConfigurationProductionNotes(assemblyDesignProductionNotesTA.getText());
        analystReportModel.setSimulationConfigurationConclusions(assemblyDesignConclusionsTA.getText());
        analystReportModel.setShowSimulationConfigurationAssemblyEntityDefinitionsTable(showEntityDefinitionsTableCB.isSelected());
        analystReportModel.setShowSimulationConfigurationAssemblyImage(showSimulationConfigurationAssemblyImageCB.isSelected());
        String s = simulationConfigurationAssemblyImagePathTF.getText();
        if (s != null && s.length() > 0) {
            analystReportModel.setAssemblyImageLocation(s);
        }
    }

    JCheckBox showEntityParametersOverviewCB;
    JTextArea     entityParametersOverviewTA;
    JScrollPane   entityParametersOverviewSP;
    JCheckBox showEntityParametersTablesCB;
    JTabbedPane   entityParametersTablesTabbedPane;

    /** user interface creation */
    private JPanel makeEntityParametersTablesPanel()
    {
        if (entityParametersTablesPanel == null)
            entityParametersTablesPanel = new JPanel();
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

    /** update AnalystReport user interface using analystReportModel values */
    @SuppressWarnings("unchecked")
    private void fillEntityParametersTablesPanel() 
    {
        try
        {
            showEntityParametersOverviewCB.setSelected(analystReportModel.isShowEntityParametersOverview());
            showEntityParametersTablesCB.setSelected  (analystReportModel.isShowEntityParametersTables());

                entityParametersOverviewTA.setText    (analystReportModel.getEntityParametersOverview());

            Vector<String> columnNames = new Vector<>();
            columnNames.add("category");
            columnNames.add("name");
            columnNames.add("description");

            Vector<Object[]> entityParametersTablesArray = analystReportModel.getEntityParametersTables();
            if (entityParametersTablesArray == null)
            {
                LOG.info("fillEntityParametersTablesPanel() entityParametersTablesArray is null");
                return;
            }

            for (Object[] objectArray : entityParametersTablesArray) 
            {
                Vector<Vector<String>> tableVector = new Vector<>();
                String name = (String) objectArray[0];
                Vector<Object[]> v0 = (Vector) objectArray[1];
                v0.forEach(oa0 -> {
                    // Rows here
                    String nm0 = (String) oa0[0];
                    Vector<String> rowVector = new Vector<>(3);
                    rowVector.add(nm0);
                    rowVector.add("");
                    rowVector.add("");
                    tableVector.add(rowVector);
                    Vector<String[]> v1 = (Vector) oa0[1];
                    for (String[] sa : v1) {
                        rowVector = new Vector<>(3);
                        rowVector.add("");
                        rowVector.add(sa[0]); // name
                        rowVector.add(sa[1]); // description
                        tableVector.add(rowVector);
                    }
                });

                entityParametersTablesTabbedPane.add(name, new JScrollPane(new EntityParameterTable(tableVector, columnNames)));
            }
        }
        catch (Exception ex)
        {
            LOG.error("fillEntityParametersTablesPanel() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
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

    /** user interface creation */
    private JPanel makeBehaviorDescriptionsPanel() 
    {
        if (behaviorDescriptionsPanel == null)
            behaviorDescriptionsPanel = new JPanel();
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

    /** update analystReportModel data using values from AnalystReport user interface */
    private void unFillBehaviorDescriptionsPanel()
    {
        analystReportModel.setShowBehaviorDesignAnalysisDescriptions(showBehaviorDesignAnalysisDescriptions.isSelected());
        analystReportModel.setBehaviorDescription(behaviorDescriptionsTA.getText());
        analystReportModel.setBehaviorConclusions(behaviorConclusionsTA.getText());
        analystReportModel.setShowBehaviorDescriptions(showBehaviorDescriptionsCB.isSelected());
        analystReportModel.setShowEventGraphImages(showBehaviorImagesCB.isSelected());

        // tables are uneditable
    }

    /** update AnalystReport user interface using analystReportModel values */
    private void fillBehaviorDescriptionsPanel() 
    {
        try
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
                String behaviorDescription = ViskitStatics.emptyIfNull((String) nextBehavior.get(1));
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
        catch (Exception ex)
        {
            LOG.error("fillBehaviorDescriptionsPanel() {}", ex);
        }
    }
    
    JCheckBox showStatisticsDescriptionAnalysisCB;
    JCheckBox showReplicationStatisticsCB;
    JCheckBox showSummaryStatisticsCB;
    JTextArea statisticsDescriptionTextArea;
    JTextArea statisticsConclusions;
    JPanel statisticsSummaryPanel;
    JPanel statisticsReplicationReportsPanel;
    JScrollPane replicationStatisticsScrollPane;
    JScrollPane statisticsSummaryScrollPane;

    /** user interface creation */
    private JPanel makeStatisticalResultsPanel() 
    {
        if (statisticalResultsPanel == null)
            statisticalResultsPanel = new JPanel();
        statisticalResultsPanel.setLayout(new BoxLayout(statisticalResultsPanel, BoxLayout.Y_AXIS));
        showStatisticsDescriptionAnalysisCB = new JCheckBox("Show statistical description and analysis", true);
        showStatisticsDescriptionAnalysisCB.setToolTipText("Show entries in output report");
        showStatisticsDescriptionAnalysisCB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticalResultsPanel.add(showStatisticsDescriptionAnalysisCB);

        JScrollPane scrollPane = new JScrollPane(statisticsDescriptionTextArea = new WrappingTextArea());
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

    /** update AnalystReport user interface using analystReportModel values */
    private void fillStatisticalResultsPanel() 
    {
        try
        {
            boolean value = analystReportModel.isShowStatiisticalResultsDescriptionAnalysis();
            showStatisticsDescriptionAnalysisCB.setSelected(value);
            statisticsDescriptionTextArea.setText(analystReportModel.getStatisticsDescription());
            statisticsConclusions.setText(analystReportModel.getStatisticsConclusions());
            statisticsDescriptionTextArea.setEnabled(value);
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
        catch (Exception ex)
        {
            LOG.error("fillStatisticalResultsPanel() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
    private void unFillStatisticalResultsPanel()
    {
        analystReportModel.setShowStatisticsDescriptionAnalysis(showStatisticsDescriptionAnalysisCB.isSelected());
        analystReportModel.setStatisticsDescription(statisticsDescriptionTextArea.getText());
        analystReportModel.setStatisticsConclusions(statisticsConclusions.getText());
        analystReportModel.setShowReplicationStatistics(showReplicationStatisticsCB.isSelected());
        analystReportModel.setShowSummaryStatistics(showSummaryStatisticsCB.isSelected());
    }
    
    JCheckBox showConclusionsRecommendationsCB;
    JTextArea conRecConclusionsTA;
    JTextArea conRecRecommendationsTA;

    /** user interface creation */
    private JPanel makeConclusionsRecommendationsPanel() 
    {
        if (conclusionsRecommendationsPanel == null)
            conclusionsRecommendationsPanel = new JPanel();
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

    /** update AnalystReport user interface using analystReportModel values */
    private void fillConclusionsRecommendationsPanel() 
    {
        try
        {
            boolean value = analystReportModel.isShowRecommendationsConclusions();
            showConclusionsRecommendationsCB.setSelected(value);
            conRecConclusionsTA.setText(analystReportModel.getConclusions());
            conRecConclusionsTA.setEnabled(value);
            conRecRecommendationsTA.setText(analystReportModel.getRecommendations());
            conRecRecommendationsTA.setEnabled(value);
        }
        catch (Exception ex)
        {
            LOG.error("fillConclusionsRecommendationsPanel() {}", ex);
        }
    }

    /** update analystReportModel data using values from AnalystReport user interface */
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
                METHOD_generateHtmlReport,
                "Generate HTML Analyst Report",
                KeyEvent.VK_H, // use hotkey H for HTML
                KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));

        getAnalystReportMenu().add(buildMenuItem(analystReportController,
                METHOD_openAnalystReportXML,
                "Open another Analyst Report XML",
                KeyEvent.VK_O,
                null));
        
        getAnalystReportMenu().add(buildMenuItem(analystReportController,
                METHOD_saveAnalystReportXML,
                "Save Analyst Report XML",
                KeyEvent.VK_S,
                null));

        getAnalystReportMenu().add(buildMenuItem(analystReportController, 
                METHOD_viewXML, 
                "XML View of Saved Analyst Report",
                KeyEvent.VK_X, 
                null));

        myMenuBar.add(getAnalystReportMenu());
    }

    // Use the actions package
    private JMenuItem buildMenuItem(Object sourceObject, String methodName, String menuItemName, Integer mnemonic, KeyStroke acceleratorKey)
    {
        Action action = ActionIntrospector.getAction(sourceObject, methodName);
        if (action == null)
        {
            LOG.error("buildMenuItem() reflection failed for name=" + menuItemName + " method=" + methodName + " in " + sourceObject.toString());
            return new JMenuItem(menuItemName + "(not working, reflection failed) ");
        }
        Map<String, Object> map = new HashMap<>();
        if (mnemonic != null) {
            map.put(Action.MNEMONIC_KEY, mnemonic);
        }
        if (acceleratorKey != null) {
            map.put(Action.ACCELERATOR_KEY, acceleratorKey);
        }
        if (menuItemName != null) {
            map.put(Action.NAME, menuItemName);
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

/** Convenience class for JTextArea with word wrap turned on */
class WrappingTextArea extends JTextArea {

    WrappingTextArea() {
        super(4, 20);
        setLineWrap(true);
        setWrapStyleWord(true);
    }
}

/** Convenience class for right-alignment JTable
 * @see https://stackoverflow.com/questions/3467052/set-right-alignment-in-jtable-column
 */
class ROTable extends JTable 
{
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

/** Custom JTable with appropriate rendering */
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
