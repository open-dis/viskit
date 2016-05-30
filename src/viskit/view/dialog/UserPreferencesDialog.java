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
package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import viskit.control.EventGraphController;
import viskit.ViskitGlobals;
import viskit.ViskitConfiguration;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import viskit.control.AssemblyController;
import viskit.view.ViskitApplicationFrame;
import viskit.view.ViskitProjectButtonPanel;

/**
 * <p>MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Nov 2, 2005
 * @since 11:24:06 AM
 * @version $Id$
 */
public class UserPreferencesDialog extends JDialog 
{
    static final Logger LOG = LogUtilities.getLogger(UserPreferencesDialog.class);

    private static UserPreferencesDialog projectSettingsDialog;
    private static boolean modified = false;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JButton cancelButton   = new JButton("Cancel");
    private final JButton     okButton   = new JButton("Close");
    private JList<String> classpathAdditionsJlist;
    private JCheckBox     eventGraphEditorPreferenceCheckBox;
    private JCheckBox       assemblyEditorPreferenceCheckBox;
    private JCheckBox          runAssemblyPreferenceCheckBox;
    private JCheckBox  designOfExperimentsPreferenceCheckBox;
    private JCheckBox           clusterRunPreferenceCheckBox;
    private JCheckBox        analystReportPreferenceCheckBox;
    private JCheckBox verboseDebugMessagesPreferenceCheckBox;

    private JRadioButton defaultLookAndFeelRB;
    private JRadioButton platformLookAndFeelRB;
    private JRadioButton otherLookAndFeelRB;
    private JTextField otherTF;
	
	public final String PREFERENCE_CHANGE_MESSAGE = "Panel visibility changes are applied when Visual Simkit launches";

    public static boolean showDialog(JFrame mother) 
	{
        if (projectSettingsDialog == null) 
		{
            projectSettingsDialog = new UserPreferencesDialog(mother);
        } 
		else 
		{
            projectSettingsDialog.setParameters();
        }
        projectSettingsDialog.setVisible(true); // this call blocks for user interaction
       
        return modified;
    }

    private UserPreferencesDialog(JFrame parent) 
	{
        super(parent, "User Preferences", true);
		
		initialize ();
	}
	public void initialize ()
	{
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.addWindowListener(new MyCloseListener());
        initializeConfigurations();

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);
		
//		JPanel     headerPanel      = new JPanel ();
//		JLabel     projectNameLabel = new JLabel ("Project name");
//		JTextField projectNameTF    = new JTextField ();
//		projectNameTF.setText(ViskitGlobals.instance().getCurrentViskitProject().getProjectName());
//		projectNameTF.setEditable(false);
//		headerPanel.add(projectNameLabel);
//        headerPanel.add(Box.createVerticalStrut(5));
//		headerPanel.add(projectNameTF);
//        contentPane.add(headerPanel);
				

        
        buildWidgets();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        //buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        //buttonPanel.add(Box.createHorizontalGlue());

        contentPane.add(tabbedPane);
        contentPane.add(Box.createVerticalStrut(5));
        contentPane.add(buttonPanel);

        // attach listeners
        cancelButton.addActionListener(new CancelButtonListener());
            okButton.addActionListener(new ApplyButtonListener());
			
        CapabilitiesHandler capabilitiesHandler = new CapabilitiesHandler();
             eventGraphEditorPreferenceCheckBox.addActionListener(capabilitiesHandler);
               assemblyEditorPreferenceCheckBox.addActionListener(capabilitiesHandler);
                  runAssemblyPreferenceCheckBox.addActionListener(capabilitiesHandler);
          designOfExperimentsPreferenceCheckBox.addActionListener(capabilitiesHandler);
                   clusterRunPreferenceCheckBox.addActionListener(capabilitiesHandler);
                analystReportPreferenceCheckBox.addActionListener(capabilitiesHandler);
         verboseDebugMessagesPreferenceCheckBox.addActionListener(capabilitiesHandler);

        setParameters();
    }

    private void setParameters() 
	{
        fillWidgets();
        getRootPane().setDefaultButton(cancelButton);

        modified = false;

        pack();
        Dimension d = getSize();
        d.width = Math.max(d.width, 500);
        setSize(d);
        setLocationRelativeTo(getParent());
    }

    private void buildWidgets()
	{
        JPanel visibilityPreferencesPanel = new JPanel();
        visibilityPreferencesPanel.setLayout(new BoxLayout(visibilityPreferencesPanel, BoxLayout.Y_AXIS));
        visibilityPreferencesPanel.add(Box.createVerticalStrut(5));
        visibilityPreferencesPanel.add(Box.createVerticalGlue());
		
		// ================================================================
		// Capabilities
		
        JPanel innerCheckBoxPanel = new JPanel();
        innerCheckBoxPanel.setLayout(new BoxLayout(innerCheckBoxPanel, BoxLayout.Y_AXIS));
        innerCheckBoxPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        innerCheckBoxPanel.setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(3, 3, 3, 3)));
		
        eventGraphEditorPreferenceCheckBox = new JCheckBox("Event Graph Editor");
        innerCheckBoxPanel.add(eventGraphEditorPreferenceCheckBox);
        assemblyEditorPreferenceCheckBox = new JCheckBox("Assembly Editor");
        innerCheckBoxPanel.add(assemblyEditorPreferenceCheckBox);
        runAssemblyPreferenceCheckBox = new JCheckBox("Simulation Run");
        innerCheckBoxPanel.add(runAssemblyPreferenceCheckBox);
        analystReportPreferenceCheckBox = new JCheckBox("Analyst Report");
        innerCheckBoxPanel.add(analystReportPreferenceCheckBox);
		
        designOfExperimentsPreferenceCheckBox = new JCheckBox("Design Of Experiments (DOE)");
		designOfExperimentsPreferenceCheckBox.setEnabled(false); // TODO implement
		designOfExperimentsPreferenceCheckBox.setToolTipText("Future restoration planned"); // TODO implement
        innerCheckBoxPanel.add(designOfExperimentsPreferenceCheckBox);
		
        clusterRunPreferenceCheckBox = new JCheckBox("Cluster Computation");
		clusterRunPreferenceCheckBox.setEnabled(false); // TODO implement
		clusterRunPreferenceCheckBox.setToolTipText("Future restoration planned"); // TODO implement
        innerCheckBoxPanel.add(clusterRunPreferenceCheckBox);
		
        verboseDebugMessagesPreferenceCheckBox = new JCheckBox("Verbose debug messages");
		verboseDebugMessagesPreferenceCheckBox.setSelected(ViskitStatics.debug); // start with source code initialization; TODO remember preference
        innerCheckBoxPanel.add(verboseDebugMessagesPreferenceCheckBox);

        visibilityPreferencesPanel.add(innerCheckBoxPanel, BorderLayout.CENTER);
		
        visibilityPreferencesPanel.add(Box.createVerticalStrut(8));
        visibilityPreferencesPanel.add(Box.createVerticalGlue());
        JLabel preferenceChangeLabel = new JLabel(PREFERENCE_CHANGE_MESSAGE, JLabel.CENTER);
        preferenceChangeLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        visibilityPreferencesPanel.add(preferenceChangeLabel);
        visibilityPreferencesPanel.add(Box.createVerticalGlue());
        visibilityPreferencesPanel.add(Box.createVerticalStrut(8));

        tabbedPane.addTab("Capabilities", visibilityPreferencesPanel);
		
		// ================================================================
		// CLASSPATH Additions
		
        JPanel classpathAdditionsPanel = new JPanel();
        classpathAdditionsPanel.setLayout(new BoxLayout(classpathAdditionsPanel, BoxLayout.Y_AXIS));
		
        classpathAdditionsJlist = new JList<>(new DefaultListModel<String>());
        JScrollPane jsp = new JScrollPane(classpathAdditionsJlist);
        jsp.setPreferredSize(new Dimension(70, 70));  // don't want it to control size of dialog
        classpathAdditionsPanel.add(jsp);
		
        JPanel classpathButtonPanel = new JPanel();
        classpathButtonPanel.setLayout(new BoxLayout(classpathButtonPanel, BoxLayout.X_AXIS));
        classpathButtonPanel.add(Box.createHorizontalGlue());
		
        JButton upClasspathButton     = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/upArrow.png")));
        upClasspathButton.setBorder(null);
        upClasspathButton.addActionListener(new UpClasspathHandler());
        JButton addClasspathButton    = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/plus.png")));
        addClasspathButton.addActionListener(new AddClasspathHandler());
        JButton removeClasspathButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minus.png")));
        removeClasspathButton.addActionListener(new DeleteClasspathHandler());
        JButton downClasspathButton   = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/downArrow.png")));
        downClasspathButton.setBorder(null);
        downClasspathButton.addActionListener(new downCPhandler());
		
        classpathButtonPanel.add(upClasspathButton);
        classpathButtonPanel.add(addClasspathButton);
        classpathButtonPanel.add(removeClasspathButton);
        classpathButtonPanel.add(downClasspathButton);
        classpathButtonPanel.add(Box.createHorizontalGlue());
        classpathAdditionsPanel.add(classpathButtonPanel);

        tabbedPane.addTab("CLASSPATH Additions", classpathAdditionsPanel);
		
		// ================================================================
		// Look and Feel

        JPanel lookAndFeelPanel = new JPanel();
        lookAndFeelPanel.setLayout(new BoxLayout(lookAndFeelPanel, BoxLayout.Y_AXIS));
        lookAndFeelPanel.add(Box.createVerticalGlue());
        JPanel lAndFeelInnerP = new JPanel();
        lAndFeelInnerP.setLayout(new BoxLayout(lAndFeelInnerP, BoxLayout.Y_AXIS));
        lAndFeelInnerP.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        defaultLookAndFeelRB = new JRadioButton("Default");
        defaultLookAndFeelRB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lAndFeelInnerP.add(defaultLookAndFeelRB);
        platformLookAndFeelRB = new JRadioButton("Platform");
        platformLookAndFeelRB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lAndFeelInnerP.add(platformLookAndFeelRB);
        otherLookAndFeelRB = new JRadioButton("Other");
        JPanel otherPan = new JPanel();
        otherPan.setLayout(new BoxLayout(otherPan,BoxLayout.X_AXIS));
        otherPan.add(otherLookAndFeelRB);
        otherPan.add(Box.createHorizontalStrut(5));
        otherTF = new JTextField();
        ViskitStatics.clampHeight(otherTF);
        otherPan.add(otherTF);
        otherPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lAndFeelInnerP.add(otherPan);
        lAndFeelInnerP.setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(3,3,3,3)));
        ViskitStatics.clampHeight(lAndFeelInnerP);
        lookAndFeelPanel.add(lAndFeelInnerP);
        lookAndFeelPanel.add(Box.createVerticalStrut(3));
        preferenceChangeLabel = new JLabel(PREFERENCE_CHANGE_MESSAGE, JLabel.CENTER);
        preferenceChangeLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        lookAndFeelPanel.add(preferenceChangeLabel);
        lookAndFeelPanel.add(Box.createVerticalGlue());

        ButtonGroup buttonGroup = new ButtonGroup();
        defaultLookAndFeelRB.setSelected(true);
        otherTF.setEnabled(false);
        buttonGroup.add (defaultLookAndFeelRB);
        buttonGroup.add(platformLookAndFeelRB);
        buttonGroup.add(   otherLookAndFeelRB);
        ActionListener listener = new LookAndFeelListener();
        platformLookAndFeelRB.addActionListener(listener);
         defaultLookAndFeelRB.addActionListener(listener);
           otherLookAndFeelRB.addActionListener(listener);
                      otherTF.addActionListener(listener);

        tabbedPane.addTab("Look and Feel",lookAndFeelPanel);
		
		// ================================================================
		// Recent File Lists

        JPanel recentP = new JPanel();
        recentP.setLayout(new BoxLayout(recentP, BoxLayout.Y_AXIS));

        JButton clearRecentEventGraphsButton = new JButton("Clear List: Recent Event Graphs");
        clearRecentEventGraphsButton.addActionListener(new ClearRecentEventGraphsHandler());
        clearRecentEventGraphsButton.setAlignmentX(Box.CENTER_ALIGNMENT);
		clearRecentEventGraphsButton.setToolTipText("Clears user list of recently opened Event Graphs");
		
        JButton clearRecentAssembliesButton = new JButton("Clear List:  Recent Assemblies");
        clearRecentAssembliesButton.addActionListener(new ClearRecentAssembliesHandler());
        clearRecentAssembliesButton.setAlignmentX(Box.CENTER_ALIGNMENT);
		clearRecentAssembliesButton.setSize(clearRecentEventGraphsButton.getSize());
		clearRecentAssembliesButton.setToolTipText("Clears user list of recently opened Assemblies");
		
        JButton clearDotViskitConfigurationThenExitButton = new JButton("Clear All Preferences and Exit");
        clearDotViskitConfigurationThenExitButton.addActionListener(new ClearDotViskitConfigurationThenExitHandler());
        clearDotViskitConfigurationThenExitButton.setAlignmentX(Box.CENTER_ALIGNMENT);
		clearDotViskitConfigurationThenExitButton.setSize(clearRecentEventGraphsButton.getSize());
		clearDotViskitConfigurationThenExitButton.setToolTipText("Cleans user .viskit directory");
		
        recentP.add(Box.createVerticalGlue());
        recentP.add(clearRecentEventGraphsButton);
        recentP.add(Box.createVerticalGlue());
        recentP.add(clearRecentAssembliesButton);
        recentP.add(Box.createVerticalGlue());
        recentP.add(clearDotViskitConfigurationThenExitButton);
        recentP.add(Box.createVerticalGlue());

        tabbedPane.addTab("Recent Files", recentP);
     }

    class LookAndFeelListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == otherTF) {
                guiConfiguration.setProperty(ViskitConfiguration.LOOK_AND_FEEL_KEY, otherTF.getText().trim());
            } else {
                if (defaultLookAndFeelRB.isSelected()) {
                    guiConfiguration.setProperty(ViskitConfiguration.LOOK_AND_FEEL_KEY, ViskitConfiguration.LOOK_AND_FEEL_DEFAULT);
                    otherTF.setEnabled(false);
                } else if (platformLookAndFeelRB.isSelected()) {
                    guiConfiguration.setProperty(ViskitConfiguration.LOOK_AND_FEEL_KEY, ViskitConfiguration.LOOK_AND_FEEL_PLATFORM);
                    otherTF.setEnabled(false);
                } else if (otherLookAndFeelRB.isSelected()) {
                    guiConfiguration.setProperty(ViskitConfiguration.LOOK_AND_FEEL_KEY, otherTF.getText().trim());
                    otherTF.setEnabled(true);
                }
            }
        }
    }
    private static XMLConfiguration applicationConfiguration;
    private static XMLConfiguration     projectConfiguration;
    private static XMLConfiguration         guiConfiguration;

    private static void initializeConfigurations()
	{
      applicationConfiguration = ViskitConfiguration.instance().getViskitApplicationXMLConfiguration();
          projectConfiguration = ViskitConfiguration.instance().getProjectXMLConfiguration();
              guiConfiguration = ViskitConfiguration.instance().getViskitGuiXMLConfiguration();
    }

    class CapabilitiesHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) 
		{
            JCheckBox originatingCheckBox = (JCheckBox) e.getSource();
            if      (originatingCheckBox == eventGraphEditorPreferenceCheckBox) 
			{
                applicationConfiguration.setProperty(ViskitConfiguration.EVENTGRAPH_EDIT_VISIBLE_KEY, eventGraphEditorPreferenceCheckBox.isSelected());
            } 
			else if (originatingCheckBox == assemblyEditorPreferenceCheckBox) 
			{
                applicationConfiguration.setProperty(ViskitConfiguration.ASSEMBLY_EDIT_VISIBLE_KEY, assemblyEditorPreferenceCheckBox.isSelected());
            }
			else if (originatingCheckBox == runAssemblyPreferenceCheckBox)
			{
                if (runAssemblyPreferenceCheckBox.isSelected()) 
				{
                    // if we turn on the Simulation Run, we also need the Assembly editor
                    if (!assemblyEditorPreferenceCheckBox.isSelected()) 
					{
                        assemblyEditorPreferenceCheckBox.doClick();
                    } // reenter here
                }
                applicationConfiguration.setProperty(ViskitConfiguration.SIMULATION_RUN_VISIBLE_KEY, runAssemblyPreferenceCheckBox.isSelected());
            } 
			else if (originatingCheckBox == verboseDebugMessagesPreferenceCheckBox) 
			{
                applicationConfiguration.setProperty(ViskitConfiguration.DEBUG_MESSAGES_KEY, verboseDebugMessagesPreferenceCheckBox.isSelected());
                ViskitStatics.debug = verboseDebugMessagesPreferenceCheckBox.isSelected();
            } 
			else if (originatingCheckBox == analystReportPreferenceCheckBox)
			{
                applicationConfiguration.setProperty(ViskitConfiguration.ANALYST_REPORT_VISIBLE_KEY, analystReportPreferenceCheckBox.isSelected());
            }
			else if (originatingCheckBox == designOfExperimentsPreferenceCheckBox)
			{
                applicationConfiguration.setProperty(ViskitConfiguration.DOE_EDIT_VISIBLE_KEY, designOfExperimentsPreferenceCheckBox.isSelected());
            } 
			else if (originatingCheckBox == clusterRunPreferenceCheckBox)
			{
                applicationConfiguration.setProperty(ViskitConfiguration.CLUSTER_RUN_VISIBLE_KEY, clusterRunPreferenceCheckBox.isSelected());
            }
        }
    }

    class ClearRecentEventGraphsHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            EventGraphController eventGraphController = (EventGraphController) ViskitGlobals.instance().getEventGraphController();
            eventGraphController.clearRecentEventGraphFileSet();
        }
    }

    class ClearRecentAssembliesHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            AssemblyController assemblyController = (AssemblyController) ViskitGlobals.instance().getAssemblyController();
            assemblyController.clearRecentAssemblyFileList();
        }
    }

    class ClearDotViskitConfigurationThenExitHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
                int ret = JOptionPane.showConfirmDialog(UserPreferencesDialog.this, "Are you sure you want to clear your user preferences?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION)
				{
					ViskitApplicationFrame.nukeDotViskit(); // clear out .viskit configuration files
					
					JOptionPane.showMessageDialog(UserPreferencesDialog.this, "All user preferences cleared, Visual Simkit (Viskit) will now exit.",
                        "Visual Simkit (Viskit) preferences cleared", JOptionPane.INFORMATION_MESSAGE);
					System.exit(0);
                }
        }
    }

    private static void clearClassPathEntries() {
        // Always reinitialize the config instances.  We may have changed projects
        initializeConfigurations();
        projectConfiguration.clearTree(ViskitConfiguration.EXTRA_CLASSPATHS_CLEAR_KEY);
    }

    static JDialog progressDialog;
    static JProgressBar progress = new JProgressBar(0, 100);

    /** Method to facilitate putting project/lib entries on the classpath
     * @param lis a list of classpath (jar) entries to include on the classpath
     */
    public static void saveExtraClassPathEntries(String[] lis) {
        clearClassPathEntries();

        int ix = 0;
        if (lis != null) {
            for (String s : lis) {
                s = s.replaceAll("\\\\", "/");
                LogUtilities.getLogger(UserPreferencesDialog.class).debug("lis[" + ix + "]: " + s);
                projectConfiguration.setProperty(ViskitConfiguration.EXTRA_CLASSPATHS_PATH_KEY + "(" + ix + ")[@value]", s);
                ix++;
            }
        }

        if (projectSettingsDialog != null) {
            progressDialog = new JDialog(projectSettingsDialog);
            progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            progress.setIndeterminate(true);
            progress.setString("Loading Libraries");
            progress.setStringPainted(true);
            progressDialog.add(progress);
            progressDialog.pack();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            progressDialog.setLocation((d.width - progressDialog.getWidth()) / 2, (d.height - progressDialog.getHeight()) / 2);
            progressDialog.setVisible(true);
            progressDialog.setResizable(false);
        }
        Task t = new Task();
        t.execute();
    }

    static class Task extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            if (projectSettingsDialog != null) {
                progressDialog.setVisible(true);
                progressDialog.toFront();
            }

            // Incase we have custom jars, need to add these to the ClassLoader
            ViskitGlobals.instance().resetWorkClassLoader();
            ViskitGlobals.instance().rebuildLEGOTreePanels();
            return null;
        }

        @Override
        public void done() {
            if (projectSettingsDialog != null && progressDialog != null) {
                progress.setIndeterminate(false);
                progress.setValue(100);
                progressDialog.dispose();
            }
        }
    }

    private void fillWidgets() 
	{
        DefaultListModel<String> mod = (DefaultListModel<String>) classpathAdditionsJlist.getModel();
        mod.clear();
        if (getExtraClassPath() != null) {
            String[] sa = getExtraClassPath();
            for (String s : sa) {
                s = s.replaceAll("\\\\", "/");
                if (!mod.contains(s)) {
                    mod.addElement(s);
                }
            }
            classpathAdditionsJlist.setModel(mod);
        }

        eventGraphEditorPreferenceCheckBox.setSelected(isEventGraphEditorVisible());
        assemblyEditorPreferenceCheckBox.setSelected(isAssemblyEditorVisible());
        runAssemblyPreferenceCheckBox.setSelected(isSimulationRunVisible());
        designOfExperimentsPreferenceCheckBox.setSelected(isDOEVisible());
        clusterRunPreferenceCheckBox.setSelected(isClusterRunVisible());
        analystReportPreferenceCheckBox.setSelected(isAnalystReportVisible());
        verboseDebugMessagesPreferenceCheckBox.setSelected(ViskitStatics.debug = isVerboseDebug());

        String lookAndFeelName = getLookAndFeel();
        if(lookAndFeelName == null || lookAndFeelName.equals(ViskitConfiguration.LOOK_AND_FEEL_PLATFORM)) 
		{
            platformLookAndFeelRB.setSelected(true);
        } 
		else if(lookAndFeelName.equals(ViskitConfiguration.LOOK_AND_FEEL_DEFAULT)) 
		{
            defaultLookAndFeelRB.setSelected(true);
        } 
		else 
		{
			otherLookAndFeelRB.setSelected(true);
			otherTF.setEnabled(true);
			otherTF.setText(lookAndFeelName);
        }
    }

    private void unloadWidgets() {
      // most everything gets instantly updated;  check for pending text entry
      if(otherLookAndFeelRB.isSelected()) {
          guiConfiguration.setProperty(ViskitConfiguration.LOOK_AND_FEEL_KEY, otherTF.getText().trim());
      }
    }

    class CancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class ApplyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) 
		{
            unloadWidgets();
            dispose();
        }
    }

    class MyCloseListener extends WindowAdapter 
	{
        @Override
        public void windowClosing(WindowEvent e) 
		{
            if (modified) 
			{
                int returnValue = JOptionPane.showConfirmDialog(UserPreferencesDialog.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (returnValue == JOptionPane.YES_OPTION) 
				{
                    okButton.doClick();
                }
				else 
				{
                    cancelButton.doClick();
                }
            } 
			else 
			{
                cancelButton.doClick();
            }
        }
    }
    JFileChooser addChooser;

    class AddClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) 
		{
            if (addChooser == null) {
                addChooser = new JFileChooser(ViskitProject.MY_VISKIT_PROJECTS_DIR);
				addChooser.setDialogTitle("Open Project Settings");
                addChooser.setMultiSelectionEnabled(false);
                addChooser.setAcceptAllFileFilterUsed(false);
                addChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                addChooser.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        String nm = f.getName();
                        int idx = nm.lastIndexOf('.');
                        if (idx != -1) {
                            String extension = nm.substring(idx).toLowerCase();
                            if (extension != null && (extension.equals(".jar") ||
                                    extension.equals(".zip"))) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Directories, jars and zips";
                    }
                });
            }

            int retv = addChooser.showOpenDialog(UserPreferencesDialog.this);
            if (retv == JFileChooser.APPROVE_OPTION) {
                File selFile = addChooser.getSelectedFile();
                String absPath = selFile.getAbsolutePath();
                ((DefaultListModel<String>) classpathAdditionsJlist.getModel()).addElement(absPath.replaceAll("\\\\", "/"));
                installExtraClassPathIntoConfig();
            }
        }
    }

    private void installExtraClassPathIntoConfig() {
        Object[] oa = ((DefaultListModel) classpathAdditionsJlist.getModel()).toArray();
        String[] sa = new String[oa.length];

        System.arraycopy(oa, 0, sa, 0, oa.length);

        saveExtraClassPathEntries(sa);
    }

    class DeleteClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = classpathAdditionsJlist.getSelectedIndices();
            if (selected == null || selected.length <= 0) {
                return;
            }
            for (int i = selected.length - 1; i >= 0; i--) {
                ((DefaultListModel) classpathAdditionsJlist.getModel()).remove(selected[i]);
            }
            installExtraClassPathIntoConfig();
        }
    }

    class UpClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = classpathAdditionsJlist.getSelectedIndices();
            if (selected == null || selected.length <= 0 || selected[0] <= 0) {
                return;
            }
            moveLine(selected[0], -1);
        }
    }

    private void moveLine(int idx, int polarity) {
        classpathAdditionsJlist.clearSelection();
        DefaultListModel<String> mod = (DefaultListModel<String>) classpathAdditionsJlist.getModel();
        Object o = mod.get(idx);
        mod.remove(idx);
        mod.add(idx + polarity, (String) o);
        installExtraClassPathIntoConfig();
        classpathAdditionsJlist.setSelectedIndex(idx + polarity);
    }

    class downCPhandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = classpathAdditionsJlist.getSelectedIndices();
            int listLen = classpathAdditionsJlist.getModel().getSize();

            if (selected == null || selected.length <= 0 || selected[0] >= (listLen - 1)) {
                return;
            }
            moveLine(selected[0], +1);
        }
    }

    /** @return a String array containing the extra classpaths to consider */
    public static String[] getExtraClassPath()
	{
        if ((applicationConfiguration == null) || (projectConfiguration == null) || (guiConfiguration == null))
		{
            initializeConfigurations();
        }
		if (projectConfiguration == null) // no project during startup
			 return null;
		else return projectConfiguration.getStringArray(ViskitConfiguration.EXTRA_CLASSPATHS_KEY);
    }

    /** @return a URL[] of the extra classpaths, to include a path to event graphs */
    public static URL[] getExtraClassPathArraytoURLArray() {
        String[] extClassPaths = getExtraClassPath();
        if (extClassPaths == null) {return null;}
        URL[] extClassPathsUrls = new URL[extClassPaths.length];
        int i = 0;
        File file;
        for (String path : extClassPaths) {
            file = new File(path);
            if (!file.exists()) {

                // Allow a relative path for Diskit-Test (Diskit)
                if (path.contains("..") && (ViskitGlobals.instance().getCurrentViskitProject() != null))
				{
                    file = new File(ViskitGlobals.instance().getCurrentViskitProject().getProjectRootDirectory().getParent() + "/" + path.replaceFirst("../", ""));
                }
            }
            if (file.exists()) {
                try {
                    extClassPathsUrls[i++] = file.toURI().toURL();
                } catch (MalformedURLException ex) {
                    LogUtilities.getLogger(UserPreferencesDialog.class).error(ex);
                }
            }
        }
        return extClassPathsUrls;
    }

    /**
     * Return the value for the platform look and feel
     * @return the value for the platform look and feel
     */
    public static String getLookAndFeel() 
	{
        return ViskitConfiguration.instance().getValue(ViskitConfiguration.LOOK_AND_FEEL_KEY);
    }

    /**
     * Return the value for tab visibility
     * @param tabProperty the tab of interest
     * @return the value for tab visibility
     */
    public static boolean getVisibilitySense(String tabProperty)
	{
        return applicationConfiguration.getBoolean(tabProperty);
    }

    /**
     * Return if the EVENT GRAPH Editor is to be visible
     * @return if the EVENT GRAPH Editor is to be visible
     */
    public static boolean isEventGraphEditorVisible() 
	{
        return getVisibilitySense(ViskitConfiguration.EVENTGRAPH_EDIT_VISIBLE_KEY);
    }

    /**
     * Return if the Assembly Editor is to be visible
     * @return if the Assembly Editor is to be visible
     */
    public static boolean isAssemblyEditorVisible()
	{
        return getVisibilitySense(ViskitConfiguration.ASSEMBLY_EDIT_VISIBLE_KEY);
    }

    /**
     * Return if the Simulation Run is to be visible
     * @return if the Simulation Run is to be visible
     */
    public static boolean isSimulationRunVisible()
	{
		boolean visible  = true; // default
		try {
			visible = getVisibilitySense(ViskitConfiguration.SIMULATION_RUN_VISIBLE_KEY);
		}
		catch (Exception e)
		{
			// visible = getVisibilitySense(ViskitConfiguration.ASSEMBLY_RUN_VISIBLE_KEY); // deprecated, use default
		}
        return visible;
    }

    /**
     * Return if the Analyst Report Editor is to be visible
     * @return if the Analyst Report Editor is to be visible
     */
    public static boolean isAnalystReportVisible() 
	{
        return getVisibilitySense(ViskitConfiguration.ANALYST_REPORT_VISIBLE_KEY);
    }

    /**
     * Return if verbose debug message are to be printed
     * @return if verbose debug message are to be printed
     */
    public static boolean isVerboseDebug() 
	{
        return getVisibilitySense(ViskitConfiguration.DEBUG_MESSAGES_KEY);
    }

    /**
     * Return if the Design of Experiments Editor is to be visible
     * @return if the Design of Experiments Editor is to be visible
     */
    public static boolean isDOEVisible() 
	{
        return getVisibilitySense(ViskitConfiguration.DOE_EDIT_VISIBLE_KEY);
    }

    /**
     * Return if the Cluster Runner is to be visible
     * @return if the Cluster Runner is to be visible
     */
    public static boolean isClusterRunVisible() 
	{
        return getVisibilitySense(ViskitConfiguration.CLUSTER_RUN_VISIBLE_KEY);
    }
}
