/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.MovesInstitute.org)
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

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2006
 * @since 2:47:03 PM
 */

package viskit;

import edu.nps.util.FileIO;
import viskit.xsd.assembly.AnalystReportBuilder;
import viskit.xsd.assembly.XsltUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.net.URL;

public class AnalystReportPanel extends JPanel implements OpenAssembly.AssyChangeListener
{
  private AnalystReportBuilder arb;
  private File reportFile;

  private boolean dirty=false;
  private JMenuBar myMenuBar;

  private JFileChooser locationImageFileChooser;
  public AnalystReportPanel()
  {
    setLayout();
    setBackground(new Color(251,251,229)); // yellow
    doMenus();

    locationImageFileChooser = new JFileChooser("./images/BehaviorLibraries/SavageTactics/");

  }

  JTextField titleTF = new JTextField();
  JTextField analystNameTF = new JTextField();
  JComboBox classifiedTF = new JComboBox(new String[]{"UNCLASSIFIED","FOUO","CONFIDENTIAL","SECRET","TOP SECRET"});
  JTextField dateTF = new JTextField(DateFormat.getDateInstance(DateFormat.LONG).format(new Date()));

  File currentAssyFile;
  /* to get the name of the assembly file */
  public void assyChanged(int action, OpenAssembly.AssyChangeListener source, Object param)
  {
    switch (action) {
      case NEW_ASSY:
        currentAssyFile = (File)param;
        if(arb != null)
          arb.setAssemblyFile(((File)param).getAbsolutePath());
        break;

      case CLOSE_ASSY:
      case PARAM_LOCALLY_EDITTED:
      case JAXB_CHANGED:
        break;

      default:
        System.err.println("Program error InternalAssemblyRunner.assyChanged");
    }
  }

  public String getHandle()
  {
    return "";
  }

  public JMenuBar getMenus()
  {
    return myMenuBar;
  }

  // From assy runner:

  public void setReportXML(String path)
  {
    File srcFil = new File(path);

    File anDir = new File("./AnalystReports");
    anDir.mkdirs();

    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmm");
    String output = formatter.format(new Date());// today

    String usr = System.getProperty("user.name");
    String outputFile = (usr + "AnalystReport_"+output+".xml");

    File targetFile = new File(anDir,outputFile);
    try {
      FileIO.copyFile(srcFil,targetFile,true);
      srcFil.deleteOnExit();
    }
    catch (IOException e) {
      System.err.println("Exception copying Analyst Report File: "+e.getMessage());
    }

    doTitle(targetFile.getName());
    buildArb(targetFile);
  }
  private void buildArb(File targetFile)
  {
    AnalystReportBuilder arb = null;
    try {
      String assyFile = (currentAssyFile != null) ? currentAssyFile.getAbsolutePath() : null;
      arb = new AnalystReportBuilder(targetFile, assyFile);
    }
    catch (Exception e) {
      System.err.println("Error parsing analyst report: "+e.getMessage());
      return;
    }
    setContent(arb);
    reportFile = targetFile;
    dirty=false;
  }

  public void setContent(AnalystReportBuilder arb)
  {
    if(arb != null && dirty) {
      int resp = JOptionPane.showConfirmDialog(this,"<html>The experiment has completed and the report is ready to be displayed.<br>"+
                                                    "The current report data has not been saved. Save current report before continuing?",
                                                    "Save Report",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
      if(resp == JOptionPane.YES_OPTION)
        saveReport();
    }
    dirty=false;

    this.arb = arb;
    fillLayout();
  }

  private void fillLayout()
  {
    // We don't always come in on the swing thread.
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        _fillLayout();
      }
    });
  }

  private void _fillLayout()
  {
    fillHeader();
    fillExecSumm();
    fillSimulationLocation();
    fillSimulationConfiguration();
    fillEntityParams();
    fillBehaviors();
    fillStatsPan();
    fillConclusionsRecommendationsPanel();
  }
  private void unFillLayout()
  {
    unFillHeader();
    unFillExecSumm();
    unFillSimulationLocation();
    unFillSimulationConfiguration();
    unFillEntityParams();
    unFillBehaviors();
    unFillStatsPan();
    unFillConRecPan();
  }

  private void fillHeader()
  {
    titleTF.setText(arb.getReportName());
    analystNameTF.setText(arb.getAuthor());
    String date = arb.getDateOfReport();
    if(date != null && date.length()>0)
      dateTF.setText(date);
    else
      dateTF.setText(DateFormat.getDateInstance().format(new Date())); //now
    classifiedTF.setSelectedItem(arb.getClassification());
  }

  private void unFillHeader()
  {
    arb.setReportName(titleTF.getText());
    arb.setAuthor(analystNameTF.getText());
    arb.setDateOfReport(dateTF.getText());
    arb.setClassification((String)classifiedTF.getSelectedItem());
  }

  private void setLayout()
  {
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    JTabbedPane tabs = new JTabbedPane();

    JPanel headerPanel = new JPanel(new SpringLayout());
    headerPanel.add(new JLabel("Title"));
    headerPanel.add(titleTF);
    headerPanel.add(new JLabel("Author"));
    headerPanel.add(analystNameTF);
    headerPanel.add(new JLabel("Analysis Date"));
    headerPanel.add(dateTF);
    headerPanel.add(new JLabel("Report Classification"));
    headerPanel.add(classifiedTF);
    Dimension d = new Dimension(Integer.MAX_VALUE,titleTF.getPreferredSize().height);
    titleTF.setMaximumSize(new Dimension(d));
    analystNameTF.setMaximumSize(new Dimension(d));
    dateTF.setMaximumSize(new Dimension(d));
    classifiedTF.setMaximumSize(new Dimension(d));
    SpringUtilities.makeCompactGrid(headerPanel, 4, 2, 10, 10, 5, 5);

    headerPanel.setBorder(new EmptyBorder(10,10,10,10));
    headerPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    headerPanel.setAlignmentY(JComponent.RIGHT_ALIGNMENT);
    headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,headerPanel.getPreferredSize().height));

    tabs.add("1 Header",headerPanel);
    tabs.add("2 Executive Summary",makeExecutiveSummaryPanel());
    tabs.add("3 Simulation Location",makeSimulationLocationPanel());
    tabs.add("4 Assembly Configuration",makeAssemblyDesignPanel());
    tabs.add("5 Entity Parameters",makeEntityParamsPanel());
    tabs.add("6 Behavior Descriptions",makeBehaviorsPanel());
    tabs.add("7 Statistical Results",makeStatisticsPanel());
    tabs.add("8 Conclusions, Recommendations",makeConclusionsRecommendationsPanel());

    add(tabs);
    //setBorder(new EmptyBorder(10,10,10,10));
  }

  JCheckBox wantExecutiveSummary;
  JTextArea execSummTA;
  private JPanel makeExecutiveSummaryPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    wantExecutiveSummary = new JCheckBox("Include executive summary", true);
    wantExecutiveSummary.setToolTipText ("Include entries in output report");
    wantExecutiveSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantExecutiveSummary);
    
    JScrollPane jsp = new JScrollPane(execSummTA = new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);

    /*
    execSummary = new Element("ExecutiveSummary");
    execSummary.setAttribute("comments", "true");
    execSummary.addContent(makeComments("ES", ""));

    */
    execSummTA.setLineWrap(true);
    execSummTA.setWrapStyleWord(true);
    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }
  private void fillExecSumm()
  {
    wantExecutiveSummary.setSelected(arb.isExecutiveSummaryComments());
    execSummTA.setText(arb.getExecutiveSummary());
    execSummTA.setEnabled(wantExecutiveSummary.isSelected());
  }
  private void unFillExecSumm()
  {
    arb.setExecutiveSummaryComments(wantExecutiveSummary.isSelected());
    arb.setExecutiveSummary(execSummTA.getText());
  }
  /************************/

  JCheckBox wantLocationDescriptions;
  JCheckBox wantLocationImages;
  JTextArea locCommentsTA;
  JTextArea locConclusionsTA;
  JTextField simLocImgTF;
  JButton    simLocImgButt;
  JTextField simChartImgTF;
  JButton    simChartImgButt;

  private JPanel makeSimulationLocationPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    wantLocationDescriptions = new JCheckBox("Include location features and post-experiment descriptions", true);
    wantLocationDescriptions.setToolTipText ("Include entries in output report");
    wantLocationDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantLocationDescriptions);
    
    JScrollPane jsp = new JScrollPane(locCommentsTA = new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Description of Location Features"));
    p.add(jsp);
    
    jsp = new JScrollPane(locConclusionsTA = new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Significant Location Features"));
    p.add(jsp);
    
    wantLocationImages = new JCheckBox("Include location and chart image(s)", true);
    wantLocationImages.setToolTipText ("Include entries in output report");
    wantLocationImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantLocationImages);

    JPanel imp = new JPanel();
    imp.setLayout(new BoxLayout(imp,BoxLayout.X_AXIS));
    imp.add(new JLabel("Location image "));
    imp.add(simLocImgTF=new JTextField(20));
    imp.add(simLocImgButt=new JButton("..."));
    simLocImgButt.addActionListener(new fileChoiceListener(simLocImgTF));
    Dimension ps = simLocImgTF.getPreferredSize();
    simLocImgTF.setMaximumSize(new Dimension(Integer.MAX_VALUE,ps.height));
    simLocImgTF.setEditable(false);
    imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(imp);

    imp = new JPanel();
    imp.setLayout(new BoxLayout(imp,BoxLayout.X_AXIS));
    imp.add(new JLabel("Chart image "));
    imp.add(simChartImgTF=new JTextField(20));
    imp.add(simChartImgButt=new JButton("..."));
    simChartImgButt.addActionListener(new fileChoiceListener(simChartImgTF));
    ps = simChartImgTF.getPreferredSize();
    simChartImgTF.setMaximumSize(new Dimension(Integer.MAX_VALUE,ps.height));
    simChartImgTF.setEditable(false);
    imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(imp);

    p.setBorder(new EmptyBorder(10,10,10,10));

    return p;
  }

  private void fillSimulationLocation()
  {
    wantLocationDescriptions.setSelected(arb.isPrintSimLocationComments());
    locCommentsTA.setText(arb.getSimLocationComments());
    locCommentsTA.setEnabled(wantLocationDescriptions.isSelected());
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
  private void unFillSimulationLocation()
  {
    arb.setPrintSimLocationComments(wantLocationDescriptions.isSelected());
    arb.setSimLocationDescription(locCommentsTA.getText());
    arb.setSimLocationConclusions(locConclusionsTA.getText());
    arb.setPrintSimLocationImage(wantLocationImages.isSelected());
    String s = simLocImgTF.getText().trim();
    if(s != null & s.length() > 0)
      arb.setLocationImage(s);
    s = simLocImgTF.getText().trim();
    if(s != null & s.length() > 0)
      arb.setChartImage(simChartImgTF.getText());
  }

  /************************/

  JCheckBox wantAssemblyDesignAndAnalysis;
  JTextArea assemblyDesignConsiderations;
  JTextArea simConfigConclusions;
  JCheckBox wantSimConfigImages;
  JCheckBox wantEntityTable;
  JTable    entityTable;
  JTextField configImgPathTF;
  JButton   configImgButt;

  private JPanel makeAssemblyDesignPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    wantAssemblyDesignAndAnalysis = new JCheckBox("Include assembly-design considerations and post-experiment analysis", true);
    wantAssemblyDesignAndAnalysis.setToolTipText ("Include entries in output report");
    wantAssemblyDesignAndAnalysis.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantAssemblyDesignAndAnalysis);
    
    JScrollPane jsp = new JScrollPane(assemblyDesignConsiderations = new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Assembly Design Considerations"));
    p.add(jsp);
    
    jsp = new JScrollPane(simConfigConclusions = new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Simulation Assembly Design"));
    p.add(jsp);

    wantEntityTable = new JCheckBox("Include entity definition table", true);
    wantEntityTable.setToolTipText ("Include entries in output report");
    wantEntityTable.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantEntityTable);
    
    JPanel pp = new JPanel();
    pp.setLayout(new BoxLayout(pp,BoxLayout.X_AXIS));
    pp.add(Box.createHorizontalGlue());
    pp.add(new JScrollPane(entityTable = new JTable()));
    pp.add(Box.createHorizontalGlue());
    pp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(pp);
    //p.add(new JScrollPane(entityTable = new JTable()));
    entityTable.setPreferredScrollableViewportSize(new Dimension(550, 120));
    
    wantSimConfigImages = new JCheckBox("Include simulation configuration image", true);
    wantSimConfigImages.setToolTipText ("Include entries in output report");
    wantSimConfigImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantSimConfigImages);
    
    JPanel imp = new JPanel();
    imp.setLayout(new BoxLayout(imp,BoxLayout.X_AXIS));
    imp.add(new JLabel("Configuration image: "));
    imp.add(configImgPathTF=new JTextField(20));
    imp.add(configImgButt=new JButton("..."));
    configImgButt.addActionListener(new fileChoiceListener(configImgPathTF));
    Dimension ps = configImgPathTF.getPreferredSize();
    configImgPathTF.setMaximumSize(new Dimension(Integer.MAX_VALUE,ps.height));
    configImgPathTF.setEditable(false);
    imp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(imp);

    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void fillSimulationConfiguration()
  {
    wantAssemblyDesignAndAnalysis.setSelected(arb.isPrintSimConfigComments());
    assemblyDesignConsiderations.setText(arb.getSimConfigComments());
    assemblyDesignConsiderations.setEnabled(wantAssemblyDesignAndAnalysis.isSelected());

    wantEntityTable.setSelected(arb.isPrintEntityTable());

    String[][]sa = arb.getSimConfigEntityTable();
    entityTable.setModel(new DefaultTableModel(sa,new String[]{"Entity Name","Behavior Type"}));
    entityTable.getColumnModel().getColumn(0).setPreferredWidth(200);
    entityTable.getColumnModel().getColumn(1).setPreferredWidth(200);

    simConfigConclusions.setText(arb.getSimConfigConclusions());
    simConfigConclusions.setEnabled(arb.isPrintSimConfigComments());

    wantSimConfigImages.setSelected(arb.isPrintAssemblyImage());
    configImgButt.setEnabled(wantSimConfigImages.isSelected());
    configImgPathTF.setEnabled(wantSimConfigImages.isSelected());
    configImgPathTF.setText(arb.getAssemblyImageLocation());
  }

  private void unFillSimulationConfiguration()
  {
    arb.setPrintSimConfigComments(wantAssemblyDesignAndAnalysis.isSelected());
    arb.setSimConfigurationDescription(assemblyDesignConsiderations.getText());
    arb.setSimConfigurationConclusions(simConfigConclusions.getText());
    arb.setPrintEntityTable(wantEntityTable.isSelected());
    arb.setPrintAssemblyImage(wantSimConfigImages.isSelected());
    String s = configImgPathTF.getText();
    if(s != null && s.length()>0)
      arb.setAssemblyImageLocation(s);
  }

  JCheckBox wantEntityParameterDescriptions;
  JCheckBox wantEntityParameterTables;
  JTabbedPane entityParamTabs;
  JTextArea entityParamCommentsTA;
  JScrollPane entityParamCommentsSP;

  private JPanel makeEntityParamsPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    wantEntityParameterDescriptions = new JCheckBox("Include entity parameter descriptions", true);
    wantEntityParameterDescriptions.setToolTipText ("Include entries in output report");
    wantEntityParameterDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantEntityParameterDescriptions);

    entityParamCommentsTA=new WrappingTextArea();
    entityParamCommentsSP = new JScrollPane(entityParamCommentsTA);
    entityParamCommentsSP.setBorder(new TitledBorder("Entity Parameters Overview"));
    entityParamCommentsSP.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(entityParamCommentsSP);

    wantEntityParameterTables = new JCheckBox("Include entity parameter tables", true);
    wantEntityParameterTables.setToolTipText ("Include entries in output report");
    wantEntityParameterTables.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantEntityParameterTables);
    
    // TODO: post-experiment
    
    entityParamTabs = new JTabbedPane(JTabbedPane.LEFT);
    entityParamTabs.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(entityParamTabs);
    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void fillEntityParams()
  {
    wantEntityParameterDescriptions.setSelected(arb.isPrintParameterComments());
    wantEntityParameterTables.setSelected(arb.isPrintParameterTable());

    entityParamCommentsTA.setText(arb.getParameterComments());

    Vector colNames = new Vector();
    colNames.add("Category");
    colNames.add("Name");
    colNames.add("Description");

    Vector v = arb.getParameterTables();

    for(Iterator itr=v.iterator(); itr.hasNext();) {
      Vector tableVector = new Vector();
      Object[] oa = (Object[])itr.next();
      String nm = (String)oa[0];
      Vector v0 = (Vector)oa[1];
      for(Iterator itr0=v0.iterator();itr0.hasNext();) {
        // Rows here
        Object[] oa0 = (Object[])itr0.next();
        String nm0 = (String)oa0[0];
        Vector rowVect = new Vector(3);
        rowVect.add(nm0);
        rowVect.add("");
        rowVect.add("");
        tableVector.add(rowVect);
        Vector v1 = (Vector)oa0[1];
        for(Iterator itr1=v1.iterator();itr1.hasNext();) {
          rowVect = new Vector(3);
          rowVect.add("");
          String[] sa = (String[])itr1.next();
          rowVect.add(sa[0]); // name
          rowVect.add(sa[1]); // description
          tableVector.add(rowVect);
        }
      }

      entityParamTabs.add(nm,new JScrollPane(new EntityParamTable(tableVector,colNames)));
    }
  }

  private void unFillEntityParams()
  {
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

  private JPanel makeBehaviorsPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    doBehaviorDesignAnalysisDescriptions = new JCheckBox("Include behavior design and post-experiment analysis", true);
    doBehaviorDesignAnalysisDescriptions.setToolTipText ("Include entries in output report");
    doBehaviorDesignAnalysisDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(doBehaviorDesignAnalysisDescriptions);
    
    JScrollPane jsp = new JScrollPane(behaviorDescriptionTA = new WrappingTextArea());
    jsp.setBorder(new TitledBorder("Behavior Design Description"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);
    
    jsp = new JScrollPane(behaviorConclusionsTA = new WrappingTextArea());
    jsp.setBorder(new TitledBorder("Post-Experiment Analysis of Entity Behaviors"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);
    
    doBehaviorDescriptions = new JCheckBox("Include behavior descriptions", true);
    doBehaviorDescriptions.setToolTipText ("Include entries in output report");
    doBehaviorDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(doBehaviorDescriptions);

    doBehaviorImages = new JCheckBox("Include behavior images", true);
    doBehaviorImages.setToolTipText ("Include entries in output report");
    doBehaviorImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(doBehaviorImages);

    behaviorTabs = new JTabbedPane(JTabbedPane.LEFT);
    behaviorTabs.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(behaviorTabs);
    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void unFillBehaviors()
  {
    arb.setPrintBehaviorDefComments(doBehaviorDesignAnalysisDescriptions.isSelected());
    arb.setBehaviorDescription(behaviorDescriptionTA.getText());
    arb.setBehaviorConclusions(behaviorConclusionsTA.getText());
    arb.setPrintBehaviorDescriptions(doBehaviorDescriptions.isSelected());
    arb.setPrintEventGraphImages(doBehaviorImages.isSelected());

    // tables are uneditable
  }
  private void fillBehaviors()
  {
    doBehaviorDesignAnalysisDescriptions.setSelected(arb.isPrintBehaviorDefComments());
    behaviorDescriptionTA.setText(arb.getBehaviorComments());
    behaviorDescriptionTA.setEnabled(doBehaviorDesignAnalysisDescriptions.isSelected());
    behaviorConclusionsTA.setText(arb.getBehaviorConclusions());
    behaviorConclusionsTA.setEnabled(doBehaviorDesignAnalysisDescriptions.isSelected());
    doBehaviorImages.setEnabled(arb.isPrintEventGraphImages());
    doBehaviorDescriptions.setSelected(arb.isPrintBehaviorDescriptions());
    behaviorTabs.setEnabled(doBehaviorDescriptions.isSelected());

    List behaviorList = arb.getBehaviorList();

    behaviorTabs.removeAll();
    for(Iterator itr=behaviorList.iterator(); itr.hasNext();) {
      List   nextBehavior = (List)itr.next();
      String behaviorName = (String)nextBehavior.get(0);
      String behaviorDescription= (String)nextBehavior.get(1);
      List   behaviorParameters = (List)nextBehavior.get(2);
      List   behaviorStateVariables = (List)nextBehavior.get(3);
      String behaviorImagePath = (String) nextBehavior.get(4);

      JPanel p = new JPanel();
      p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

      JLabel lab = new JLabel("Description:  "+behaviorDescription);
      lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      p.add(lab);

      lab = new JLabel("Image location:  "+behaviorImagePath);
      p.add(lab);

      Vector cols = new Vector(3);
      cols.add("name");cols.add("type");cols.add("description");

      Vector data = new Vector(behaviorParameters.size());
      for(int i=0;i<behaviorParameters.size();i++) {
        String[] sa = (String[])behaviorParameters.get(i);
        Vector row = new Vector(3);
        row.add(sa[0]);row.add(sa[1]);row.add(sa[2]);
        data.add(row);
      }
      JScrollPane jsp = new JScrollPane(new ROTable(data,cols));
      jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      jsp.setBorder(new TitledBorder("Parameters"));
      p.add(jsp);

      data = new Vector(behaviorStateVariables.size());
      for(int i=0;i<behaviorStateVariables.size();i++) {
        String[] sa = (String[])behaviorStateVariables.get(i);
        Vector row = new Vector(3);
        row.add(sa[0]);row.add(sa[1]);row.add(sa[2]);
        data.add(row);
      }
      jsp=new JScrollPane(new ROTable(data,cols));
      jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      jsp.setBorder(new TitledBorder("State variables"));
      p.add(jsp);

      behaviorTabs.add(behaviorName,p);
    }

  }

  JCheckBox wantStatisticsDescriptionAnalysis;
  JCheckBox wantStatsReplications;
  JCheckBox wantStatisticsSummary;
  JTextArea statsComments;
  JTextArea statsConclusions;
  JPanel    statsSummaryPanel;
  JPanel    statsRepPanel;
  JScrollPane repsJsp;
  JScrollPane summJsp;

  private JPanel makeStatisticsPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    wantStatisticsDescriptionAnalysis = new JCheckBox("Include statistical description and analysis", true);
    wantStatisticsDescriptionAnalysis.setToolTipText ("Include entries in output report");
    wantStatisticsDescriptionAnalysis.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantStatisticsDescriptionAnalysis);
    
    JScrollPane jsp = new JScrollPane(statsComments=new WrappingTextArea());
    jsp.setBorder(new TitledBorder("Description of Expected Results"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);
    
    jsp = new JScrollPane(statsConclusions= new WrappingTextArea());
    jsp.setBorder(new TitledBorder("Analysis of Experimental Results"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);

    wantStatsReplications=new JCheckBox("Include replication statistics", true);
    wantStatsReplications.setToolTipText ("Include entries in output report");
    wantStatsReplications.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantStatsReplications);

    repsJsp = new JScrollPane(statsRepPanel = new JPanel());
    repsJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    statsRepPanel.setLayout(new BoxLayout(statsRepPanel,BoxLayout.Y_AXIS));
    p.add(repsJsp);

    wantStatisticsSummary = new JCheckBox("Include summary statistics", true);
    wantStatisticsSummary.setToolTipText ("Include entries in output report");
    wantStatisticsSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantStatisticsSummary);

    summJsp = new JScrollPane(statsSummaryPanel = new JPanel());
    summJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    statsSummaryPanel.setLayout(new BoxLayout(statsSummaryPanel,BoxLayout.Y_AXIS));
    p.add(summJsp);

    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void fillStatsPan()
  {
    boolean bool = arb.isPrintStatsComments();
    wantStatisticsDescriptionAnalysis.setSelected(bool);
    statsComments.setText(arb.getStatsComments());
    statsConclusions.setText(arb.getStatsConclusions());
    statsComments.setEnabled(bool);
    statsConclusions.setEnabled(bool);

    bool = arb.isPrintReplicationStats();
    wantStatsReplications.setSelected(bool);
    bool = arb.isPrintSummaryStats();
    wantStatisticsSummary.setSelected(bool);

    List reps = arb.getStatsReplicationsList();
    statsRepPanel.removeAll();
    JLabel lab;
    JScrollPane jsp;
    JTable tab;

    statsRepPanel.add(lab=new JLabel("Replication Reports"));
    lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    statsRepPanel.add(Box.createVerticalStrut(10));
    String[] colNames = new String[]{"Run #","Count","Min","Max","Mean","Std Deviation","Variance"};

    for(Iterator repItr = reps.iterator(); repItr.hasNext();) {
      List r = (List)repItr.next();
      String nm = (String)r.get(0);
      String prop = (String)r.get(1);
      statsRepPanel.add(lab=new JLabel("Entity: "+nm));
      lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      statsRepPanel.add(lab=new JLabel("Property: "+prop));
      lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);

      List vals = (List)r.get(2);
      String[][] saa = new String[vals.size()][];
      int i=0;
      for(Iterator r2=vals.iterator();r2.hasNext();) {
        saa[i++]= (String[])r2.next();
      }
      statsRepPanel.add(jsp=new JScrollPane(tab=new ROTable(saa,colNames)));
      tab.setPreferredScrollableViewportSize(new Dimension(tab.getPreferredSize()));
      jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

      statsRepPanel.add(Box.createVerticalStrut(20));
    }
    List summs= arb.getStastSummaryList();

    colNames = new String[]{"Entity","Property","Count","Min","Max","Mean","Std Deviation","Variance"};
    String[][] saa = new String[summs.size()][];
    int i=0;
    for(Iterator sumItr = summs.iterator();sumItr.hasNext();) {
      saa[i++] = (String[])sumItr.next();
    }

    statsSummaryPanel.removeAll();
    statsSummaryPanel.add(lab = new JLabel("Summary Report"));
    lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    statsSummaryPanel.add(Box.createVerticalStrut(10));

    statsSummaryPanel.add(jsp = new JScrollPane(tab=new ROTable(saa,colNames)));
    tab.setPreferredScrollableViewportSize(new Dimension(tab.getPreferredSize()));
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    repsJsp.setMaximumSize(new Dimension(Integer.MAX_VALUE,150));
    summJsp.setMaximumSize(new Dimension(Integer.MAX_VALUE,150));

  }

  private void unFillStatsPan()
  {
    arb.setPrintStatsComments(wantStatisticsDescriptionAnalysis.isSelected());
    arb.setStatsDescription(statsComments.getText());
    arb.setStatsConclusions(statsConclusions.getText());
    arb.setPrintReplicationStats(wantStatsReplications.isSelected());
    arb.setPrintSummaryStats(wantStatisticsSummary.isSelected());
  }

  JCheckBox wantConclusionsRecommendations;
  JTextArea conRecConclusionsTA;
  JTextArea conRecRecsTA;

  private JPanel makeConclusionsRecommendationsPanel()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    wantConclusionsRecommendations = new JCheckBox("Include conclusions and recommendations", true);
    wantConclusionsRecommendations.setToolTipText ("Include entries in output report");
    wantConclusionsRecommendations.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(wantConclusionsRecommendations);
    
    JScrollPane jsp = new JScrollPane(conRecConclusionsTA=new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Conclusions"));
    p.add(jsp);
    
    jsp = new JScrollPane(conRecRecsTA=new WrappingTextArea());
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Recommendations for Future Work"));
    p.setBorder(new EmptyBorder(10,10,10,10));
    p.add(jsp);
    return p;
  }

  private void fillConclusionsRecommendationsPanel()
  {
    boolean bool = arb.isPrintRecommendationsConclusions();
    wantConclusionsRecommendations.setSelected(bool);
    conRecConclusionsTA.setText(arb.getConclusions());
    conRecConclusionsTA.setEnabled(bool);
    conRecRecsTA.setText(arb.getRecommendations());
    conRecRecsTA.setEnabled(bool);
  }

  private void unFillConRecPan()
  {
    arb.setPrintRecommendationsConclusions(wantConclusionsRecommendations.isSelected());
    arb.setConclusions(conRecConclusionsTA.getText());
    arb.setRecommendations(conRecRecsTA.getText());
  }

  private void saveReport()
  {
    saveReport(reportFile);
  }

  private void saveReport(File f)
  {
    try {
      arb.writeToXMLFile(f);
      dirty=false;
    }
    catch (Exception e) {
      System.err.println("Error writing XML: "+e.getMessage());
    }
  }

  private void doMenus()
  {
    myMenuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    JMenuItem open  = new JMenuItem("Open analyst report XML");
    open.setMnemonic(KeyEvent.VK_O);
    JMenuItem view  = new JMenuItem("View analyst report XML");
    view.setMnemonic(KeyEvent.VK_V);
    view.setEnabled (false); // TODO:  imolement listener and view functionality
    JMenuItem save  = new JMenuItem("Save analyst report XML");
    save.setMnemonic(KeyEvent.VK_S);
    JMenuItem generateViewHtml = new JMenuItem("View generated report HTML");
    generateViewHtml.setMnemonic(KeyEvent.VK_V);

    fileMenu.add(open);
    fileMenu.add(view);
    fileMenu.add(save);
    fileMenu.add(generateViewHtml);
    myMenuBar.add(fileMenu);

    open.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (dirty) {
          int result = JOptionPane.showConfirmDialog(AnalystReportPanel.this, "Save current analyst report data?",
              "Confirm", JOptionPane.WARNING_MESSAGE);
          switch (result) {
            case JOptionPane.YES_OPTION:
              saveReport();
              break;
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.NO_OPTION:
            default:
              break;
          }
        }

        JFileChooser openChooser = new JFileChooser("./AnalystReports");
        openChooser.setMultiSelectionEnabled(false);
        openChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resp = openChooser.showOpenDialog(AnalystReportPanel.this);
        if (resp != JFileChooser.APPROVE_OPTION)
          return;

        buildArb(openChooser.getSelectedFile());
      }
    });

    ActionListener saveAsLis = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        JFileChooser saveChooser = new JFileChooser(reportFile.getParent());
        saveChooser.setSelectedFile(reportFile);
        saveChooser.setMultiSelectionEnabled(false);
        saveChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resp = saveChooser.showSaveDialog(AnalystReportPanel.this);

        if(resp!=JFileChooser.APPROVE_OPTION)
          return;

        unFillLayout();
        saveReport(reportFile);
        String outFile = reportFile.getAbsolutePath();
        int idx = outFile.lastIndexOf(".");

        outFile = outFile.substring(0,idx) + ".html";
        XsltUtility.runXslt(reportFile.getAbsolutePath(),
            outFile, "./AnalystReports/AnalystReportXMLtoHTML.xslt");
        
        // pop up the system html viewer, or send currently running browser to html page
        try {
            viskit.util.BareBonesBrowserLaunch.openURL( (new File(outFile)).toURI().toURL().toString() );
        } catch (java.net.MalformedURLException mue) {
            System.out.println(outFile + " : malformed path error.");
        }
      }
    };
    save.addActionListener(saveAsLis);

    ActionListener generateViewHtmlListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (dirty) {
          int result = JOptionPane.showConfirmDialog(AnalystReportPanel.this, "Save current analyst report data?",
              "Confirm", JOptionPane.WARNING_MESSAGE);
          switch (result) {
            case JOptionPane.YES_OPTION:
              saveReport();
              break;
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.NO_OPTION:
            default:
              break;
          }
        }

        String outFile = reportFile.getAbsolutePath();
        int idx = outFile.lastIndexOf(".");

        outFile = outFile.substring(0,idx) + ".html";

        JFileChooser genChooser = new JFileChooser("./AnalystReports");
        genChooser.setSelectedFile(new File(outFile));
        genChooser.setMultiSelectionEnabled(false);
        genChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resp = genChooser.showSaveDialog(AnalystReportPanel.this);

        if(resp == JFileChooser.APPROVE_OPTION)
            XsltUtility.runXslt(reportFile.getAbsolutePath(),
                                genChooser.getSelectedFile().getAbsolutePath(),
                                "./AnalystReports/AnalystReportXMLtoHTML.xslt");
        showHtmlViewer(genChooser.getSelectedFile());

      }
    };
    generateViewHtml.addActionListener(generateViewHtmlListener);
  }

  private void showHtmlViewer(File f)
  {
      String errMsg = null;
      // pop up the system html viewer, or send currently running browser to html page
        try {
            viskit.util.BareBonesBrowserLaunch.openURL( f.toURI().toURL().toString() );
        } catch (java.net.MalformedURLException mue) {
            errMsg = f + " : malformed path error.";
        }
  
    if(errMsg != null)
      JOptionPane.showMessageDialog(this,"<html><center>Error displaying HTML:<br>"+errMsg,"Error",JOptionPane.ERROR_MESSAGE);
/*

    JFrame fr = new JFrame("Analyst Report -- "+f.getPath());
    fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    //fr.setSize(640,480);

    Container con = fr.getContentPane();
    con.setLayout(new BorderLayout());

    JEditorPane jtp = new JEditorPane();
    jtp.setEditable(false);
    try {
      URL url = new URL("file","localhost",f.getAbsolutePath());
      jtp.setPage(url);

      JScrollPane jsp = new JScrollPane(jtp);
      jsp.setMinimumSize(new Dimension(10,10));
      jsp.setPreferredSize(new Dimension(640,480));
      con.add(jsp,BorderLayout.CENTER);
      fr.pack();
      fr.setLocation(300,300);
      fr.setVisible(true);
    }
    catch (IOException e) {
      JOptionPane.showMessageDialog(this,"<html><center>Error displaying HTML:<br>"+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    }
*/
  }

  class fileChoiceListener implements ActionListener
  {
    JTextField tf;
    fileChoiceListener(JTextField tf)
    {
      this.tf = tf;
    }

    public void actionPerformed(ActionEvent e)
    {
      int resp = locationImageFileChooser.showOpenDialog(AnalystReportPanel.this);
      if(resp == JFileChooser.APPROVE_OPTION) {
         tf.setText(locationImageFileChooser.getSelectedFile().getAbsolutePath());
      }
    }
  }

  private TitleListener titlList;
  private int titlkey;
  public void setTitleListener(TitleListener lis, int key)
  {
    titlList = lis;
    titlkey = key;
    doTitle(null);
  }

  private String namePrefix = "Viskit Analyst Report Editor";
  private String currentTitle = namePrefix;
  private void doTitle(String nm)
  {
    if(nm != null && nm.length()>0)
      currentTitle = namePrefix +": "+nm;

    if(titlList != null)
      titlList.setTitle(currentTitle,titlkey);
  }

}

class WrappingTextArea extends JTextArea
{
  WrappingTextArea()
  {
    super(4,20);
    setLineWrap(true);
    setWrapStyleWord(true);
  }
}

class ROTable extends JTable
{
  ROTable(Vector v, Vector c)
  {
    super(v,c);
  }
  ROTable(Object[][]oa, Object[]cols)
  {
    super(oa,cols);
  }
  public boolean isCellEditable(int row, int column)
  {
    return false;
  }
}

class EntityParamTable extends ROTable implements TableCellRenderer
{
  TableCellRenderer defRenderer;
  EntityParamTable(Vector v, Vector c)
  {
    super(v,c);
    defRenderer = new DefaultTableCellRenderer();

    TableColumn tc = getColumnModel().getColumn(0);
    tc.setCellRenderer(this);
  }

  Color grey = new Color(204,204,204);
  Color origBkgd;
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
  {
    Component c = defRenderer.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
    if(origBkgd == null)
      origBkgd = c.getBackground();
    Object o0 = getValueAt(row,0);
    Object o1 = getValueAt(row,1);
    Object o2 = getValueAt(row,2);

    if(o0 != null && (o1==null || ((String)o1).length()<=0) && ((o2 == null || ((String)o2).length()<=0)))
      c.setBackground(grey);
    else
      c.setBackground(origBkgd);
    return c;
  }

}

