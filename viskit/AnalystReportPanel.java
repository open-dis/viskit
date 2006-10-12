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
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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
  JTextField analyNameTF = new JTextField();
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
      arb = new AnalystReportBuilder(targetFile, currentAssyFile.getAbsolutePath());
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
    fillSimLoc();
    fillSimConfig();
    fillEntityParams();
    fillBehaviors();
    fillStatsPan();
    fillConRecPan();
  }
  private void unFillLayout()
  {
    unFillHeader();
    unFillExecSumm();
    unFillSimLoc();
    unFillSimConfig();
    unFillEntityParams();
    unFillBehaviors();
    unFillStatsPan();
    unFillConRecPan();
  }

  private void fillHeader()
  {
    titleTF.setText(arb.getReportName());
    analyNameTF.setText(arb.getAuthor());
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
    arb.setAuthor(analyNameTF.getText());
    arb.setDateOfReport(dateTF.getText());
    arb.setClassification((String)classifiedTF.getSelectedItem());
  }

  private void setLayout()
  {
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    JTabbedPane tabs = new JTabbedPane();

    JPanel hdrPan = new JPanel(new SpringLayout());
    hdrPan.add(new JLabel("Report Title"));
    hdrPan.add(titleTF);
    hdrPan.add(new JLabel("Report Author"));
    hdrPan.add(analyNameTF);
    hdrPan.add(new JLabel("Report Date"));
    hdrPan.add(dateTF);
    hdrPan.add(new JLabel("Report Classification"));
    hdrPan.add(classifiedTF);
    Dimension d = new Dimension(Integer.MAX_VALUE,titleTF.getPreferredSize().height);
    titleTF.setMaximumSize(new Dimension(d));
    analyNameTF.setMaximumSize(new Dimension(d));
    dateTF.setMaximumSize(new Dimension(d));
    classifiedTF.setMaximumSize(new Dimension(d));
    SpringUtilities.makeCompactGrid(hdrPan, 4, 2, 10, 10, 5, 5);

    hdrPan.setBorder(new EmptyBorder(10,10,10,10));
    hdrPan.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    hdrPan.setMaximumSize(new Dimension(Integer.MAX_VALUE,hdrPan.getPreferredSize().height));

    tabs.add("1 Heading",hdrPan);

    tabs.add("2 Executive Summary",makeExecSummPan());
    tabs.add("3 Simulation Location",makeSimLocPan());
    tabs.add("4 Simulation Configuration",makeSimConfigPan());
    tabs.add("5 Entity Parameters",makeEntityParamsPan());
    tabs.add("6 Behavior Definitions",makeBehaviorsPan());
    tabs.add("7 Statistical Results",makeStatsPan());
    tabs.add("8 Conclusions and Recommendations",makeConRecPan());

    add(tabs);
    //setBorder(new EmptyBorder(10,10,10,10));
  }

  JCheckBox wantExecSumm;
  JTextArea execSummTA;
  private JPanel makeExecSummPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(wantExecSumm = new JCheckBox("Want executive summary", true));
    wantExecSumm.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JScrollPane jsp;
    p.add(jsp=new JScrollPane(execSummTA = new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

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
    wantExecSumm.setSelected(arb.isExecutiveSummaryComments());
    execSummTA.setText(arb.getExecutiveSummary());
    execSummTA.setEnabled(wantExecSumm.isSelected());
  }
  private void unFillExecSumm()
  {
    arb.setExecutiveSummaryComments(wantExecSumm.isSelected());
    arb.setExecutiveSummary(execSummTA.getText());
  }
  /************************/

  JCheckBox wantLocCommentsAndConclusions;
  JCheckBox wantLocImages;
  JTextArea locCommentsTA;
  JTextArea locConclusionsTA;
  JTextField simLocImgTF;
  JButton    simLocImgButt;
  JTextField simChartImgTF;
  JButton    simChartImgButt;

  private JPanel makeSimLocPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(wantLocCommentsAndConclusions = new JCheckBox("Want location comments and conclusions", true));
    wantLocCommentsAndConclusions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JScrollPane jsp;
    p.add(jsp=new JScrollPane(locCommentsTA = new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Comments"));
    p.add(jsp = new JScrollPane(locConclusionsTA = new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Post Experiment Comments"));
    p.add(wantLocImages = new JCheckBox("Want location image(s)", true));
    wantLocImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    JPanel imp = new JPanel();
    imp.setLayout(new BoxLayout(imp,BoxLayout.X_AXIS));
    imp.add(new JLabel("Chart image: "));
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
    imp.add(new JLabel("    Location image: "));
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

  private void fillSimLoc()
  {
    wantLocCommentsAndConclusions.setSelected(arb.isPrintSimLocationComments());
    locCommentsTA.setText(arb.getSimLocComments());
    locCommentsTA.setEnabled(wantLocCommentsAndConclusions.isSelected());
    locConclusionsTA.setText(arb.getSimLocConclusions());
    locConclusionsTA.setEnabled(wantLocCommentsAndConclusions.isSelected());
    wantLocImages.setSelected(arb.isPrintSimLocationImage());
    simLocImgTF.setEnabled(wantLocImages.isSelected());
    simLocImgButt.setEnabled(wantLocImages.isSelected());
    simChartImgTF.setEnabled(wantLocImages.isSelected());
    simChartImgButt.setEnabled(wantLocImages.isSelected());
    simLocImgTF.setText(arb.getLocationImage());
    simChartImgTF.setText(arb.getChartImage());
  }
  private void unFillSimLoc()
  {
    arb.setPrintSimLocationComments(wantLocCommentsAndConclusions.isSelected());
    arb.setSimLocComments(locCommentsTA.getText());
    arb.setSimLocConclusions(locConclusionsTA.getText());
    arb.setPrintSimLocationImage(wantLocImages.isSelected());
    arb.setLocationImage(simLocImgTF.getText());
    arb.setChartImage(simChartImgTF.getText());
  }

  /************************/

  JCheckBox wantSimConfigCommentsAndConclusions;
  JTextArea simConfigComments;
  JTextArea simConfigConclusions;
  JCheckBox wantSimConfigImages;
  JCheckBox wantEntityTable;
  JTable    entityTable;
  JTextField configImgPathTF;
  JButton   configImgButt;

  private JPanel makeSimConfigPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(wantSimConfigCommentsAndConclusions = new JCheckBox("Want simulation configuration comments", true));
    wantSimConfigCommentsAndConclusions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JScrollPane jsp;
    p.add(jsp = new JScrollPane(simConfigComments = new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Comments"));

    p.add(wantEntityTable = new JCheckBox("Want entity table", true));
    wantEntityTable.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JPanel pp = new JPanel();
    pp.setLayout(new BoxLayout(pp,BoxLayout.X_AXIS));
    pp.add(Box.createHorizontalGlue());
    pp.add(new JScrollPane(entityTable = new JTable()));
    pp.add(Box.createHorizontalGlue());
    pp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(pp);
    //p.add(new JScrollPane(entityTable = new JTable()));
    entityTable.setPreferredScrollableViewportSize(new Dimension(550, 120));
    p.add(jsp = new JScrollPane(simConfigConclusions = new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Post Experiment Comments"));
    p.add(wantSimConfigImages = new JCheckBox("Want simulation configuration image", true));
    wantSimConfigImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);
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

  private void fillSimConfig()
  {
    wantSimConfigCommentsAndConclusions.setSelected(arb.isPrintSimConfigComments());
    simConfigComments.setText(arb.getSimConfigComments());
    simConfigComments.setEnabled(wantSimConfigCommentsAndConclusions.isSelected());

    wantEntityTable.setSelected(arb.isPrintEntityTable());

    String[][]sa = arb.getSimConfigEntityTable();
    entityTable.setModel(new DefaultTableModel(sa,new String[]{"Entity Name","Behavior Definition"}));
    entityTable.getColumnModel().getColumn(0).setPreferredWidth(200);
    entityTable.getColumnModel().getColumn(1).setPreferredWidth(200);

    simConfigConclusions.setText(arb.getSimConfigConclusions());
    simConfigConclusions.setEnabled(arb.isPrintSimConfigComments());

    wantSimConfigImages.setSelected(arb.isPrintAssemblyImage());
    configImgButt.setEnabled(wantSimConfigImages.isSelected());
    configImgPathTF.setEnabled(wantSimConfigImages.isSelected());
    configImgPathTF.setText(arb.getAssemblyImageLocation());
  }

  private void unFillSimConfig()
  {
    arb.setPrintSimConfigComments(wantSimConfigCommentsAndConclusions.isSelected());
    arb.setSimConfigComments(simConfigComments.getText());
    arb.setSimConfigConclusions(simConfigConclusions.getText());
    arb.setPrintEntityTable(wantEntityTable.isSelected());
    arb.setPrintAssemblyImage(wantSimConfigImages.isSelected());
    arb.setAssemblyImageLocation(configImgPathTF.getText());
  }

  /***********/

  JCheckBox wantEntityParamComments;
  JCheckBox wantEntityParamTables;
  JTabbedPane entityParamTabs;
  JTextArea entityParamCommentsTA;

  private JPanel makeEntityParamsPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(wantEntityParamComments = new JCheckBox("Want entity parameter comments", true));
      wantEntityParamComments.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    p.add(new JScrollPane(entityParamCommentsTA=new WrappingTextArea()));

    p.add(wantEntityParamTables = new JCheckBox("Want entity parameter tables", true));
    wantEntityParamTables.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    entityParamTabs = new JTabbedPane(JTabbedPane.LEFT);
    entityParamTabs.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(entityParamTabs);
    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void fillEntityParams()
  {
    wantEntityParamComments.setSelected(arb.isPrintParameterComments());
    wantEntityParamTables.setSelected(arb.isPrintParameterTable());

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
    arb.setPrintParameterComments(wantEntityParamComments.isSelected());
    arb.setPrintParameterTable(wantEntityParamTables.isSelected());
  }

  JCheckBox doBehaviorComments;
  JCheckBox doBehaviorDescriptions;
  JCheckBox doBehaviorImages;
  JTextArea behaviorCommentsTA;
  JTextArea behaviorConclusionsTA;
  JTabbedPane behaviorTabs;

  private JPanel makeBehaviorsPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(doBehaviorComments = new JCheckBox("Want behavior comments", true));
    doBehaviorComments.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JScrollPane jsp = new JScrollPane(behaviorCommentsTA = new WrappingTextArea());
    jsp.setBorder(new TitledBorder("Comments"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);
    jsp = new JScrollPane(behaviorConclusionsTA = new WrappingTextArea());
    jsp.setBorder(new TitledBorder("Post Experiment Comments"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp);

    p.add(doBehaviorDescriptions = new JCheckBox("Want behavior descriptions", true));
    doBehaviorDescriptions.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    p.add(doBehaviorImages = new JCheckBox("Want behavior images", true));
    doBehaviorImages.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    behaviorTabs = new JTabbedPane(JTabbedPane.LEFT);
    behaviorTabs.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(behaviorTabs);
    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void unFillBehaviors()
  {
    arb.setPrintBehaviorDefComments(doBehaviorComments.isSelected());
    arb.setBehaviorComments(behaviorCommentsTA.getText());
    arb.setBehaviorConclusions(behaviorConclusionsTA.getText());
    arb.setPrintBehaviorDescriptions(doBehaviorDescriptions.isSelected());
    arb.setPrintEventGraphImages(doBehaviorImages.isSelected());

    // tables are uneditable
  }
  private void fillBehaviors()
  {
    doBehaviorComments.setSelected(arb.isPrintBehaviorDefComments());
    behaviorCommentsTA.setText(arb.getBehaviorComments());
    behaviorCommentsTA.setEnabled(doBehaviorComments.isSelected());
    behaviorConclusionsTA.setText(arb.getBehaviorConclusions());
    behaviorConclusionsTA.setEnabled(doBehaviorComments.isSelected());
    doBehaviorImages.setEnabled(arb.isPrintEventGraphImages());
    doBehaviorDescriptions.setSelected(arb.isPrintBehaviorDescriptions());
    behaviorTabs.setEnabled(doBehaviorDescriptions.isSelected());

    List lis = arb.getBehaviorList();

    behaviorTabs.removeAll();
    for(Iterator itr=lis.iterator(); itr.hasNext();) {
      List b = (List)itr.next();
      String nm = (String)b.get(0);
      String dsc= (String)b.get(1);
      List params = (List)b.get(2);
      List stVars = (List)b.get(3);
      String imgPath = (String) b.get(4);

      JPanel p = new JPanel();
      p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

      JLabel lab;
      p.add(lab=new JLabel("Description:  "+dsc));
      lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);

      p.add(lab = new JLabel("Image location:  "+imgPath));
      lab.setAlignmentX(JComponent.LEFT_ALIGNMENT);

      Vector cols = new Vector(3);
      cols.add("name");cols.add("type");cols.add("description");

      Vector data = new Vector(params.size());
      for(int i=0;i<params.size();i++) {
        String[] sa = (String[])params.get(i);
        Vector row = new Vector(3);
        row.add(sa[0]);row.add(sa[1]);row.add(sa[2]);
        data.add(row);
      }
      JScrollPane jsp;

      p.add(jsp=new JScrollPane(new ROTable(data,cols)));
      jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      jsp.setBorder(new TitledBorder("Parameters"));

      data = new Vector(stVars.size());
      for(int i=0;i<stVars.size();i++) {
        String[] sa = (String[])stVars.get(i);
        Vector row = new Vector(3);
        row.add(sa[0]);row.add(sa[1]);row.add(sa[2]);
        data.add(row);
      }
      p.add(jsp=new JScrollPane(new ROTable(data,cols)));
      jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      jsp.setBorder(new TitledBorder("State variables"));

      behaviorTabs.add(nm,p);
    }

  }

  JCheckBox wantStatsComments;
  JCheckBox wantStatsReplications;
  JCheckBox wantStatsSummary;
  JTextArea statsComments;
  JTextArea statsConclusions;
  JPanel    statsSummaryPanel;
  JPanel    statsRepPanel;
  JScrollPane repsJsp;
  JScrollPane summJsp;

  private JPanel makeStatsPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(wantStatsComments = new JCheckBox("Want statistical results comments", true));
      wantStatsComments.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JScrollPane jsp;
    p.add(jsp = new JScrollPane(statsComments=new WrappingTextArea()));
    jsp.setBorder(new TitledBorder("Comments"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    p.add(jsp = new JScrollPane(statsConclusions= new WrappingTextArea()));
    jsp.setBorder(new TitledBorder("Post-Experiment Comments"));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    p.add(wantStatsReplications=new JCheckBox("Want replication statistics", true));
    wantStatsReplications.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    p.add(repsJsp = new JScrollPane(statsRepPanel = new JPanel()));
    repsJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    statsRepPanel.setLayout(new BoxLayout(statsRepPanel,BoxLayout.Y_AXIS));

    p.add(wantStatsSummary = new JCheckBox("Want summary statistics", true));
    wantStatsSummary.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    p.add(summJsp = new JScrollPane(statsSummaryPanel = new JPanel()));
    summJsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    statsSummaryPanel.setLayout(new BoxLayout(statsSummaryPanel,BoxLayout.Y_AXIS));

    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void fillStatsPan()
  {
    boolean bool = arb.isPrintStatsComments();
    wantStatsComments.setSelected(bool);
    statsComments.setText(arb.getStatsComments());
    statsConclusions.setText(arb.getStatsConclusions());
    statsComments.setEnabled(bool);
    statsConclusions.setEnabled(bool);

    bool = arb.isPrintReplicationStats();
    wantStatsReplications.setSelected(bool);
    bool = arb.isPrintSummaryStats();
    wantStatsSummary.setSelected(bool);

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
    arb.setPrintStatsComments(wantStatsComments.isSelected());
    arb.setStatsComments(statsComments.getText());
    arb.setStatsConclusions(statsConclusions.getText());
    arb.setPrintReplicationStats(wantStatsReplications.isSelected());
    arb.setPrintSummaryStats(wantStatsSummary.isSelected());
  }

  JCheckBox wantTotalConRec;
  JTextArea conRecConclusionsTA;
  JTextArea conRecRecsTA;

  private JPanel makeConRecPan()
  {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    p.add(wantTotalConRec = new JCheckBox("Want overall conclusions and recommendations", true));
      wantTotalConRec.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JScrollPane jsp;
    p.add(jsp = new JScrollPane(conRecConclusionsTA=new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Conclusions"));
    p.add(jsp = new JScrollPane(conRecRecsTA=new WrappingTextArea()));
    jsp.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    jsp.setBorder(new TitledBorder("Recommendations for Future Work"));
    p.setBorder(new EmptyBorder(10,10,10,10));
    return p;
  }

  private void fillConRecPan()
  {
    boolean bool = arb.isPrintRecommendationsConclusions();
    wantTotalConRec.setSelected(bool);
    conRecConclusionsTA.setText(arb.getConclusions());
    conRecConclusionsTA.setEnabled(bool);
    conRecRecsTA.setText(arb.getRecommendations());
    conRecRecsTA.setEnabled(bool);
  }

  private void unFillConRecPan()
  {
    arb.setPrintRecommendationsConclusions(wantTotalConRec.isSelected());
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
    JMenu file = new JMenu("File");
    JMenuItem open  = new JMenuItem("Open analyst report");
    JMenuItem save  = new JMenuItem("Save analyst report");
    JMenuItem saveAs= new JMenuItem("Save analyst report as HTML...");

    file.add(open);
    file.add(save);
    file.add(saveAs);
    myMenuBar.add(file);

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
      }
    };

    saveAs.addActionListener(saveAsLis);
    save.addActionListener(saveAsLis);      //todo implement properly
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

