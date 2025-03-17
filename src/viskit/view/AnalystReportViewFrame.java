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

import edu.nps.util.Log4jUtilities;
import edu.nps.util.SpringUtilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
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
public class AnalystReportViewFrame extends MvcAbstractViewFrame implements OpenAssembly.AssembyChangeListener {

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
    JComboBox<String> documentLabelTF = new JComboBox<>(new String[] {"","Informational", "CONTROLLED UNCLASSIFIED INFORMATION (CUI)"});
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
    public void assemblyChanged(int action, OpenAssembly.AssembyChangeListener source, Object param) {
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

    public void setReportBuilder(AnalystReportModel newAnalystReportModel) {
        analystReportModel = newAnalystReportModel;
        setModel(analystReportModel); // hold on locally
        getController().setModel(getModel()); // tell controller
    }

    /** invokes _fillLayout() when Swing interface is ready */
    public void fillLayout() {

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
            fillSimulationLocation();
            fillSimulationConfiguration();
            fillEntityParams();
            fillBehaviors();
            fillStatisticsPanel();
            fillConclusionsRecommendationsPanel();
        }
        catch (Exception ex)
        {
            LOG.error (" _fillLayout() exception: " + ex.getMessage());
        }
    }

    public void unFillLayout() {
        unFillHeader();
        unFillExecutiveSummary();
        unFillSimulationLocation();
        unFillSimulationConfiguration();
        unFillEntityParams();
        unFillBehaviors();
        unFillStatisticsPanel();
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
        documentLabelTF.setSelectedItem(analystReportModel.getAccess());
    }

    private void unFillHeader() 
    {
        analystReportModel.setReportName(titleTF.getText());
        analystReportModel.setAuthor(analystNameTF.getText());
        analystReportModel.setDateOfReport(analysisDateTF.getText());
        analystReportModel.setAccessLabel((String) documentLabelTF.getSelectedItem());
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
        JLabel documentLabel = new JLabel("Document Label ");
        documentLabel.setToolTipText("Document Label");
        headerPanel.add(documentLabel);
        headerPanel.add(documentLabelTF);
        
        Dimension d = new Dimension(Integer.MAX_VALUE, titleTF.getPreferredSize().height);
                titleTF.setMaximumSize(new Dimension(d));
          analystNameTF.setMaximumSize(new Dimension(d));
         analysisDateTF.setMaximumSize(new Dimension(d));
        documentLabelTF.setMaximumSize(new Dimension(d));
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
        tabs.add("2 Executive Summary", makeExecutiveSummaryPanel());
        tabs.add("3 Simulation Location", makeSimulationLocationPanel());
        tabs.add("4 Assembly Configuration", makeAssemblyDesignPanel());
        tabs.add("5 Entity Parameters", makeEntityParamsPanel());
        tabs.add("6 Behavior Descriptions", makeBehaviorsPanel());
        tabs.add("7 Statistical Results", makeStatisticsPanel());
        tabs.add("8 Conclusions, Recommendations", makeConclusionsRecommendationsPanel());

        add(tabs);
    //setBorder(new EmptyBorder(10,10,10,10));
    }
    JCheckBox wantExecutiveSummary;
    JTextArea execSummTA;

    private JPanel makeExecutiveSummaryPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantExecutiveSummary = new JCheckBox("Include executive summary", true);
        wantExecutiveSummary.setToolTipText("Include entries in output report");
        wantExecutiveSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantExecutiveSummary);

        JScrollPane jsp = new JScrollPane(execSummTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(jsp);

        execSummTA.setLineWrap(true);
        execSummTA.setWrapStyleWord(true);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private void fillExecutiveSummary() {
        wantExecutiveSummary.setSelected(analystReportModel.isExecutiveSummaryComments());
        execSummTA.setText(analystReportModel.getExecutiveSummary());
        execSummTA.setEnabled(wantExecutiveSummary.isSelected());
    }

    private void unFillExecutiveSummary() {
        analystReportModel.setExecutiveSummaryComments(wantExecutiveSummary.isSelected());
        analystReportModel.setExecutiveSummary(execSummTA.getText());
    }
    /************************/
    JCheckBox wantLocationDescriptions;
    JCheckBox wantLocationImages;
    JTextArea locationCommentsTA, locationConclusionsTA, locationProductionNotesTA;
    JTextField simLocationImageTF;
    JButton simLocationImageButton;
    JTextField simChartImageTF;
    JButton simChartImageButton;

    private JPanel makeSimulationLocationPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantLocationDescriptions = new JCheckBox("Include location features and post-experiment descriptions", true);
        wantLocationDescriptions.setToolTipText("Include entries in output report");
        wantLocationDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantLocationDescriptions);

        JScrollPane jsp = new JScrollPane(locationCommentsTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Description of Location Features"));
        p.add(jsp);

        jsp = new JScrollPane(locationProductionNotesTA = new WrappingTextArea());
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        jsp.setBorder(new TitledBorder("Production Notes"));
        p.add(jsp);

        jsp = new JScrollPane(locationConclusionsTA = new WrappingTextArea());
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
        imp.add(simLocationImageTF = new JTextField(20));
        imp.add(simLocationImageButton = new JButton("..."));
        simLocationImageButton.addActionListener(new fileChoiceListener(simLocationImageTF));
        Dimension ps = simLocationImageTF.getPreferredSize();
        simLocationImageTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        simLocationImageTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(imp);

        imp = new JPanel();
        imp.setLayout(new BoxLayout(imp, BoxLayout.X_AXIS));
        imp.add(new JLabel("Chart image "));
        imp.add(simChartImageTF = new JTextField(20));
        imp.add(simChartImageButton = new JButton("..."));
        simChartImageButton.addActionListener(new fileChoiceListener(simChartImageTF));
        ps = simChartImageTF.getPreferredSize();
        simChartImageTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, ps.height));
        simChartImageTF.setEditable(false);
        imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(imp);

        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        return p;
    }

    private void fillSimulationLocation()
    {
        wantLocationDescriptions.setSelected(analystReportModel.isPrintSimulationLocationComments());
        locationCommentsTA.setText(analystReportModel.getSimulationLocationComments());
        locationCommentsTA.setEnabled(wantLocationDescriptions.isSelected());
        locationProductionNotesTA.setText(analystReportModel.getSimulationLocationProductionNotes());
        locationProductionNotesTA.setEnabled(wantLocationDescriptions.isSelected());
        locationConclusionsTA.setText(analystReportModel.getSimulationLocationConclusions());
        locationConclusionsTA.setEnabled(wantLocationDescriptions.isSelected());
        wantLocationImages.setSelected(analystReportModel.isPrintSimulationLocationImage());
        simLocationImageTF.setEnabled(wantLocationImages.isSelected());
        simLocationImageButton.setEnabled(wantLocationImages.isSelected());
        simChartImageTF.setEnabled(wantLocationImages.isSelected());
        simChartImageButton.setEnabled(wantLocationImages.isSelected());
        simLocationImageTF.setText(analystReportModel.getLocationImage());
        simChartImageTF.setText(analystReportModel.getChartImage());
    }

    private void unFillSimulationLocation() {
        analystReportModel.setPrintSimulationLocationComments(wantLocationDescriptions.isSelected());
        analystReportModel.setSimulationLocationDescription(locationCommentsTA.getText());
        analystReportModel.setSimulationLocationProductionNotes(locationProductionNotesTA.getText());
        analystReportModel.setSimulationLocationConclusions(locationConclusionsTA.getText());
        analystReportModel.setPrintSimulationLocationImage(wantLocationImages.isSelected());

        String s = simLocationImageTF.getText().trim();
        if (s != null && !s.isEmpty())
            analystReportModel.setLocationImage(s);

        s = simChartImageTF.getText();
        if (s != null && !s.isEmpty())
            analystReportModel.setChartImage(s);
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
        wantAssemblyDesignAndAnalysis.setSelected(analystReportModel.isPrintSimulationConfigurationComments());
        assemblyDesignConsiderations.setText(analystReportModel.getSimulationConfigurationComments());
        assemblyDesignConsiderations.setEnabled(wantAssemblyDesignAndAnalysis.isSelected());

        wantEntityTable.setSelected(analystReportModel.isPrintEntityTable());

        String[][] sa = analystReportModel.getSimulationConfigurationEntityTable();
        entityTable.setModel(new DefaultTableModel(sa, new String[] {"Entity Name", "Behavior Type"}));
        entityTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        entityTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        simProductionNotes.setText(analystReportModel.getSimulationConfigurationProductionNotes());
        simProductionNotes.setEnabled(analystReportModel.isPrintSimulationConfigurationComments());

        simConfigConclusions.setText(analystReportModel.getSimulationConfigurationConclusions());
        simConfigConclusions.setEnabled(analystReportModel.isPrintSimulationConfigurationComments());

        wantSimConfigImages.setSelected(analystReportModel.isPrintAssemblyImage());
        configImgButt.setEnabled(wantSimConfigImages.isSelected());
        configImgPathTF.setEnabled(wantSimConfigImages.isSelected());
        configImgPathTF.setText(analystReportModel.getAssemblyImageLocation());
    }

    private void unFillSimulationConfiguration() {
        analystReportModel.setPrintSimulationConfigurationComments(wantAssemblyDesignAndAnalysis.isSelected());
        analystReportModel.setSimulationConfigurationDescription(assemblyDesignConsiderations.getText());
        analystReportModel.setSimulationConfigationProductionNotes(simProductionNotes.getText());
        analystReportModel.setSimulationConfigurationConclusions(simConfigConclusions.getText());
        analystReportModel.setPrintEntityTable(wantEntityTable.isSelected());
        analystReportModel.setPrintAssemblyImage(wantSimConfigImages.isSelected());
        String s = configImgPathTF.getText();
        if (s != null && s.length() > 0) {
            analystReportModel.setAssemblyImageLocation(s);
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
        wantEntityParameterDescriptions.setSelected(analystReportModel.isPrintParameterComments());
        wantEntityParameterTables.setSelected(analystReportModel.isPrintParameterTable());

        entityParamCommentsTA.setText(analystReportModel.getParameterComments());

        Vector<String> colNames = new Vector<>();
        colNames.add("category");
        colNames.add("name");
        colNames.add("description");

        Vector<Object[]> v = analystReportModel.getParameterTables();

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

            entityParamTabs.add(nm, new JScrollPane(new EntityParamTable(tableVector, colNames)));
        }
    }

    private void unFillEntityParams() {
        analystReportModel.setPrintParameterComments(wantEntityParameterDescriptions.isSelected());
        analystReportModel.setPrintParameterTable(wantEntityParameterTables.isSelected());
        analystReportModel.setParameterDescription(entityParamCommentsTA.getText());
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
        analystReportModel.setPrintBehaviorDefComments(doBehaviorDesignAnalysisDescriptions.isSelected());
        analystReportModel.setBehaviorDescription(behaviorDescriptionTA.getText());
        analystReportModel.setBehaviorConclusions(behaviorConclusionsTA.getText());
        analystReportModel.setPrintBehaviorDescriptions(doBehaviorDescriptions.isSelected());
        analystReportModel.setPrintEventGraphImages(doBehaviorImages.isSelected());

        // tables are uneditable
    }

    private void fillBehaviors() {
        doBehaviorDesignAnalysisDescriptions.setSelected(analystReportModel.isPrintBehaviorDefComments());
        behaviorDescriptionTA.setText(analystReportModel.getBehaviorComments());
        behaviorDescriptionTA.setEnabled(doBehaviorDesignAnalysisDescriptions.isSelected());
        behaviorConclusionsTA.setText(analystReportModel.getBehaviorConclusions());
        behaviorConclusionsTA.setEnabled(doBehaviorDesignAnalysisDescriptions.isSelected());
        doBehaviorImages.setEnabled(analystReportModel.isPrintEventGraphImages());
        doBehaviorDescriptions.setSelected(analystReportModel.isPrintBehaviorDescriptions());
        behaviorTabs.setEnabled(doBehaviorDescriptions.isSelected());

        // TODO: JDOM v1.1 does not yet support generics
        List behaviorList = analystReportModel.getBehaviorList();

        behaviorTabs.removeAll();
        for (Iterator itr = behaviorList.iterator(); itr.hasNext();) {
            List nextBehavior = (List) itr.next();
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
    JPanel statisticsReplicationReportsPanel;
    JScrollPane repsJsp;
    JScrollPane statisticsSummaryScrollPane;

    private JPanel makeStatisticsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        wantStatisticsDescriptionAnalysis = new JCheckBox("Include statistical description and analysis", true);
        wantStatisticsDescriptionAnalysis.setToolTipText("Include entries in output report");
        wantStatisticsDescriptionAnalysis.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantStatisticsDescriptionAnalysis);

        JScrollPane scrollPane = new JScrollPane(statisticsComments = new WrappingTextArea());
        scrollPane.setBorder(new TitledBorder("Description of Expected Results"));
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(scrollPane);

        scrollPane = new JScrollPane(statisticsConclusions = new WrappingTextArea());
        scrollPane.setBorder(new TitledBorder("Analysis of Experimental Results"));
        scrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(scrollPane);

        wantStatisticsReplications = new JCheckBox("Include replication statistics", true);
        wantStatisticsReplications.setToolTipText("Include entries in output report");
        wantStatisticsReplications.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantStatisticsReplications);

        repsJsp = new JScrollPane(statisticsReplicationReportsPanel = new JPanel());
        repsJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsReplicationReportsPanel.setLayout(new BoxLayout(statisticsReplicationReportsPanel, BoxLayout.Y_AXIS));
        p.add(repsJsp);

        wantStatisticsSummary = new JCheckBox("Include summary statistics", true);
        wantStatisticsSummary.setToolTipText("Include entries in output report");
        wantStatisticsSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(wantStatisticsSummary);

        statisticsSummaryScrollPane = new JScrollPane(statisticsSummaryPanel = new JPanel());
        statisticsSummaryScrollPane.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsSummaryPanel.setLayout(new BoxLayout(statisticsSummaryPanel, BoxLayout.Y_AXIS));
        p.add(statisticsSummaryScrollPane);

        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private void fillStatisticsPanel() {
        boolean bool = analystReportModel.isPrintStatisticsComments();
        wantStatisticsDescriptionAnalysis.setSelected(bool);
        statisticsComments.setText(analystReportModel.getStatsComments());
        statisticsConclusions.setText(analystReportModel.getStatsConclusions());
        statisticsComments.setEnabled(bool);
        statisticsConclusions.setEnabled(bool);

        bool = analystReportModel.isPrintReplicationStatistics();
        wantStatisticsReplications.setSelected(bool);
        bool = analystReportModel.isPrintSummaryStatistics();
        wantStatisticsSummary.setSelected(bool);

        List replicationsList = analystReportModel.getStatisticsReplicationsList();
        statisticsReplicationReportsPanel.removeAll();
        JLabel label;
        JScrollPane jsp;
        JTable tab;

        statisticsReplicationReportsPanel.add(label = new JLabel("Replication Reports"));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsReplicationReportsPanel.add(Box.createVerticalStrut(10));
        String[] columnNames = new String[] {"Replication #", "Count", "Minimum", "Maximum", "Mean", "Standard Deviation", "Variance"};

        for (Iterator replicationsListIterator = replicationsList.iterator(); replicationsListIterator.hasNext();) {
            List r = (List) replicationsListIterator.next();
            String entityName = (String) r.get(0); // TODO not getting set...
            String property = (String) r.get(1);
            statisticsReplicationReportsPanel.add(label = new JLabel("Entity: " + entityName));
            label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            statisticsReplicationReportsPanel.add(label = new JLabel("Property: " + property));
            label.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            List vals = (List) r.get(2);
            String[][] saa = new String[vals.size()][];
            int i = 0;
            for (Iterator r2 = vals.iterator(); r2.hasNext();) {
                saa[i++] = (String[]) r2.next();
            }
            statisticsReplicationReportsPanel.add(jsp = new JScrollPane(tab = new ROTable(saa, columnNames)));
            tab.setPreferredScrollableViewportSize(new Dimension(tab.getPreferredSize()));
            jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            statisticsReplicationReportsPanel.add(Box.createVerticalStrut(20));
        }
        List summs = analystReportModel.getStastSummaryList();

        columnNames = new String[] {"Entity", "Property", "# Replications", "Minimum", "Maximum", "Mean", "Standard Deviation", "Variance"};
        String[][] saa = new String[summs.size()][];
        int i = 0;
        for (Iterator sumItr = summs.iterator(); sumItr.hasNext();) {
            saa[i++] = (String[]) sumItr.next();
        }

        statisticsSummaryPanel.removeAll();
        statisticsSummaryPanel.add(label = new JLabel("Summary Report"));
        label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        statisticsSummaryPanel.add(Box.createVerticalStrut(10));

        statisticsSummaryPanel.add(jsp = new JScrollPane(tab = new ROTable(saa, columnNames)));
        tab.setPreferredScrollableViewportSize(new Dimension(tab.getPreferredSize()));
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        repsJsp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        statisticsSummaryScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
    }

    private void unFillStatisticsPanel() {
        analystReportModel.setPrintStatisticsComments(wantStatisticsDescriptionAnalysis.isSelected());
        analystReportModel.setStatisticsDescription(statisticsComments.getText());
        analystReportModel.setStatisticsConclusions(statisticsConclusions.getText());
        analystReportModel.setPrintReplicationStatistics(wantStatisticsReplications.isSelected());
        analystReportModel.setPrintSummaryStatistics(wantStatisticsSummary.isSelected());
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
        boolean bool = analystReportModel.isPrintRecommendationsConclusions();
        wantConclusionsRecommendations.setSelected(bool);
        conRecConclusionsTA.setText(analystReportModel.getConclusions());
        conRecConclusionsTA.setEnabled(bool);
        conRecRecsTA.setText(analystReportModel.getRecommendations());
        conRecRecsTA.setEnabled(bool);
    }

    private void unFillConclusionsRecommendationsPanel() {
        analystReportModel.setPrintRecommendationsConclusions(wantConclusionsRecommendations.isSelected());
        analystReportModel.setConclusions(conRecConclusionsTA.getText());
        analystReportModel.setRecommendations(conRecRecsTA.getText());
    }

    private void buildMenus()
    {
        AnalystReportController analystReportController = (AnalystReportController) getController();

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
    private JMenuItem buildMenuItem(Object source, String method, String name, Integer mn, KeyStroke accel) {
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
        this.analystReportController = analystReportController;
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

class EntityParamTable extends ROTable implements TableCellRenderer {

    TableCellRenderer defRenderer;

    EntityParamTable(Vector<? extends Vector> v, Vector<?> c) {
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
