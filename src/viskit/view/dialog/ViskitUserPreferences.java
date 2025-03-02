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
package viskit.view.dialog;

import static edu.nps.util.BoxLayoutUtils.clampWidth;
import edu.nps.util.Log4jUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.logging.log4j.Logger;

import viskit.control.EventGraphController;
import viskit.ViskitGlobals;
import viskit.ViskitConfigurationStore;
import static viskit.ViskitConfigurationStore.tabVisibilityUserPreferenceKeyList;
import viskit.ViskitProject;
import viskit.ViskitStatics;
import viskit.control.AssemblyControllerImpl;

/**
 * <p>MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Nov 2, 2005
 * @since 11:24:06 AM
 * @version $Id$
 */
public class ViskitUserPreferences extends JDialog
{
    static final Logger LOG = Log4jUtilities.getLogger(ViskitUserPreferences.class);

    private static ViskitUserPreferences settingsDialog;
    private static boolean modified = false;
    private final JButton cancelButton;
    private final JButton okButton;
    private final JTabbedPane tabbedPane;
    private JList<String> classpathJList;
    private JCheckBox eventGraphCB;
    private JCheckBox assemblyCB;
    private JCheckBox simulationRunCB;
    private JCheckBox analystReportCB;
    private JCheckBox designOfExperimentsCB;
    private JCheckBox cloudSimulationRunCB;
    private JCheckBox dverboseDebugMessagesCB;

    private JRadioButton defaultLafRB;
    private JRadioButton platformLafRB;
    private JRadioButton otherLafRB;
    private JTextField otherTF;

    /** 
     * Display preferences panel
     * @param parentFrame so that window can popup
     * @return whether modified
     */
    public static boolean showDialog(JFrame parentFrame) 
    {
        if (settingsDialog == null) {
            settingsDialog = new ViskitUserPreferences(parentFrame);
        } 
        else {
            settingsDialog.setParams();
        }
        settingsDialog.setVisible(true);
        // above call blocks
        return modified;
    }

    private ViskitUserPreferences(JFrame parentFrame)
    {
        super(parentFrame, "Viskit Preferences", true);

        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.addWindowListener(new myCloseListener());
        getXMLConfigurations();

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(content);

        tabbedPane = new JTabbedPane();
        buildWidgets();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        cancelButton = new JButton("Cancel");
        okButton = new JButton("Close");
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);

        content.add(tabbedPane);
        content.add(Box.createVerticalStrut(5));
        content.add(buttonPanel);

        // attach listeners
        cancelButton.addActionListener(new cancelButtonListener());
        okButton.addActionListener(new applyButtonListener());
        VisibilityHandler visibilityHandler = new VisibilityHandler();
        eventGraphCB.addActionListener(visibilityHandler);
        assemblyCB.addActionListener(visibilityHandler);
        simulationRunCB.addActionListener(visibilityHandler);
        analystReportCB.addActionListener(visibilityHandler);
        designOfExperimentsCB.addActionListener(visibilityHandler);
        cloudSimulationRunCB.addActionListener(visibilityHandler);
        dverboseDebugMessagesCB.addActionListener(visibilityHandler);

        setParams();
    }

    private void setParams() {
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
        JPanel authorPanel = new JPanel(); // TODO
        
        // name, affilation, email; where does this information get saved? .viskit somewhere?
        
//        tabbedPane.addTab("Author", authorPanel);
              
        
        JPanel additionalClasspathPanel = new JPanel();
        additionalClasspathPanel.setLayout(new BoxLayout(additionalClasspathPanel, BoxLayout.Y_AXIS));
        JLabel additionalClasspathLabel = new JLabel ("Additional classpath entries");
        additionalClasspathLabel.setToolTipText("Add classpath entries if other jars or classes are needed to compile and run");
        additionalClasspathPanel.add(Box.createVerticalStrut(10));
        additionalClasspathPanel.add(additionalClasspathLabel);
        additionalClasspathPanel.add(Box.createVerticalStrut(10));
        
        classpathJList = new JList<>(new DefaultListModel<>());
        JScrollPane jScrollPane = new JScrollPane(classpathJList);
        jScrollPane.setPreferredSize(new Dimension(70, 70)); // don't want it to control size of dialog
        additionalClasspathPanel.add(jScrollPane);
        
        JPanel classpathButtonPanel = new JPanel();
        classpathButtonPanel.setLayout(new BoxLayout(classpathButtonPanel, BoxLayout.X_AXIS));
        classpathButtonPanel.add(Box.createHorizontalGlue());
        JButton upClasspathButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/upArrow.png")));
        upClasspathButton.setBorder(null);
        upClasspathButton.addActionListener(new upClasspathHandler());
        JButton addClasspathButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/plus.png")));
        addClasspathButton.addActionListener(new addClasspathHandler());
        JButton removeClasspathButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/minus.png")));
        removeClasspathButton.addActionListener(new deleteClasspathHandler());
        JButton downClasspathButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("viskit/images/downArrow.png")));
        downClasspathButton.setBorder(null);
        downClasspathButton.addActionListener(new downClasspathHandler());
        classpathButtonPanel.add(upClasspathButton);
        classpathButtonPanel.add(addClasspathButton);
        classpathButtonPanel.add(removeClasspathButton);
        classpathButtonPanel.add(downClasspathButton);
        classpathButtonPanel.add(Box.createHorizontalGlue());
        additionalClasspathPanel.add(classpathButtonPanel);

        tabbedPane.addTab("Classpath", additionalClasspathPanel);

        JPanel recentListsPanel = new JPanel();
        recentListsPanel.setLayout(new BoxLayout(recentListsPanel, BoxLayout.Y_AXIS));

        JButton clearRecentEventGraphListsButton = new JButton("Clear recent event graphs list");
        clearRecentEventGraphListsButton.addActionListener(new ClearEventGraphListHandler());
        clearRecentEventGraphListsButton.setAlignmentX(Box.CENTER_ALIGNMENT);
        
        recentListsPanel.add(Box.createVerticalStrut(10));
        
        JButton clearRecentAssembliesListButton = new JButton(" Clear recent assemblies list ");
        clearRecentAssembliesListButton.addActionListener(new ClearAssemblyListHandler());
        clearRecentAssembliesListButton.setAlignmentX(Box.CENTER_ALIGNMENT);
        recentListsPanel.add(Box.createVerticalGlue());
        recentListsPanel.add(clearRecentEventGraphListsButton);
        recentListsPanel.add(Box.createVerticalStrut(20));
        recentListsPanel.add(clearRecentAssembliesListButton);
        recentListsPanel.add(Box.createVerticalGlue());
        clampWidth(clearRecentAssembliesListButton, clearRecentEventGraphListsButton); // TODO working?

        tabbedPane.addTab("Clear recent-file lists", recentListsPanel);

        JPanel lookAndFeelPanel = new JPanel();
        lookAndFeelPanel.setLayout(new BoxLayout(lookAndFeelPanel, BoxLayout.Y_AXIS));
        lookAndFeelPanel.add(Box.createVerticalGlue());
        JPanel lookAndFeelInnerPanel = new JPanel();
        lookAndFeelInnerPanel.setLayout(new BoxLayout(lookAndFeelInnerPanel, BoxLayout.Y_AXIS));
        lookAndFeelInnerPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        defaultLafRB = new JRadioButton("Default Look and Feel (LAF)");
        defaultLafRB.setToolTipText("Best for WIN to render Event Graph editor status color");
        defaultLafRB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lookAndFeelInnerPanel.add(defaultLafRB);
        platformLafRB = new JRadioButton("Platform Look and Feel (LAF)");
        platformLafRB.setToolTipText("Best for macOS to render Event Graph editor status color");
        platformLafRB.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lookAndFeelInnerPanel.add(platformLafRB);
        otherLafRB = new JRadioButton("Other");
        otherLafRB.setToolTipText("Set to a supported Look and Feel for your platform");
        JPanel otherPan = new JPanel();
        otherPan.setLayout(new BoxLayout(otherPan,BoxLayout.X_AXIS));
        otherPan.add(otherLafRB);
        otherPan.add(Box.createHorizontalStrut(5));
        otherTF = new JTextField();
        ViskitStatics.clampHeight(otherTF);
        otherPan.add(otherTF);
        otherPan.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lookAndFeelInnerPanel.add(otherPan);
        lookAndFeelInnerPanel.setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(3,3,3,3)));
        ViskitStatics.clampHeight(lookAndFeelInnerPanel);
        lookAndFeelPanel.add(lookAndFeelInnerPanel);
        lookAndFeelPanel.add(Box.createVerticalStrut(3));
        
        // TODO fix:
        JLabel whiningLabel = new JLabel("Changes are in effect at next Viskit launch.", JLabel.CENTER);
        whiningLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        lookAndFeelPanel.add(whiningLabel);
        lookAndFeelPanel.add(Box.createVerticalGlue());

        tabbedPane.addTab("Look and Feel",lookAndFeelPanel);

        JPanel tabVisibilityPanel = new JPanel();
        tabVisibilityPanel.setLayout(new BoxLayout(tabVisibilityPanel, BoxLayout.Y_AXIS));
        tabVisibilityPanel.add(Box.createVerticalGlue());
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        eventGraphCB = new JCheckBox("Event Graph Editor");
        innerPanel.add(eventGraphCB);
        assemblyCB = new JCheckBox("Assembly Editor");
        innerPanel.add(assemblyCB);
        simulationRunCB = new JCheckBox("Simulation Run");
        innerPanel.add(simulationRunCB);
        analystReportCB = new JCheckBox("Analyst Report");
        innerPanel.add(analystReportCB);
        designOfExperimentsCB = new JCheckBox("Design Of Experiments (DOE)");
        designOfExperimentsCB.setEnabled(false);
        innerPanel.add(designOfExperimentsCB);
        cloudSimulationRunCB = new JCheckBox("Cloud Simulation Run");
        cloudSimulationRunCB.setEnabled(false);
        innerPanel.add(cloudSimulationRunCB);
        dverboseDebugMessagesCB = new JCheckBox("Verbose debug messages");
        innerPanel.add(dverboseDebugMessagesCB);
        innerPanel.setBorder(new CompoundBorder(new LineBorder(Color.black), new EmptyBorder(3, 3, 3, 3)));

        tabVisibilityPanel.add(innerPanel, BorderLayout.CENTER);
        tabVisibilityPanel.add(Box.createVerticalStrut(3));
        whiningLabel = new JLabel("Changes are in effect at next Viskit launch.", JLabel.CENTER);
        whiningLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        tabVisibilityPanel.add(whiningLabel);
        tabVisibilityPanel.add(Box.createVerticalGlue());

        tabbedPane.addTab("Tab visibility", tabVisibilityPanel);

        ButtonGroup bg = new ButtonGroup();
        defaultLafRB.setSelected(true);
        otherTF.setEnabled(false);
        bg.add(defaultLafRB);
        bg.add(platformLafRB);
        bg.add(otherLafRB);
        ActionListener lis = new lafListener();
        platformLafRB.addActionListener(lis);
        defaultLafRB.addActionListener(lis);
        otherLafRB.addActionListener(lis);
        otherTF.addActionListener(lis);
        
        // TODO when implemented, prefer author pane if not yet filled out
        tabbedPane.setSelectedIndex(3); // Tab visibility pane
     }

    class lafListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == otherTF) {
                guiXMLConfiguration.setProperty(ViskitConfigurationStore.LOOK_AND_FEEL_KEY, otherTF.getText().trim());
            } 
            else {
                if (defaultLafRB.isSelected()) {
                    guiXMLConfiguration.setProperty(ViskitConfigurationStore.LOOK_AND_FEEL_KEY, ViskitConfigurationStore.LOOK_AND_FEEL_DEFAULT);
                    otherTF.setEnabled(false);
                } else if (platformLafRB.isSelected()) {
                    guiXMLConfiguration.setProperty(ViskitConfigurationStore.LOOK_AND_FEEL_KEY, ViskitConfigurationStore.LOOK_AND_FEEL_PLATFORM);
                    otherTF.setEnabled(false);
                } else if (otherLafRB.isSelected()) {
                    guiXMLConfiguration.setProperty(ViskitConfigurationStore.LOOK_AND_FEEL_KEY, otherTF.getText().trim());
                    otherTF.setEnabled(true);
                }
            }
        }
    }
    private static XMLConfiguration     appXMLConfiguration;
    private static XMLConfiguration projectXMLConfiguration;
    private static XMLConfiguration     guiXMLConfiguration;

    private static void getXMLConfigurations() 
    {
            appXMLConfiguration = ViskitConfigurationStore.instance().getViskitAppConfiguration();
        projectXMLConfiguration = ViskitConfigurationStore.instance().getProjectXMLConfiguration();
            guiXMLConfiguration = ViskitConfigurationStore.instance().getViskitGuiConfiguration();
    }

    class VisibilityHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox src = (JCheckBox) e.getSource();
            if (src == eventGraphCB) {
                appXMLConfiguration.setProperty(ViskitConfigurationStore.EVENTGRAPH_EDITOR_VISIBLE_KEY, eventGraphCB.isSelected());
            } else if (src == assemblyCB) {
                appXMLConfiguration.setProperty(ViskitConfigurationStore.ASSEMBLY_EDITOR_VISIBLE_KEY, assemblyCB.isSelected());
            } else if (src == simulationRunCB) {
                if (simulationRunCB.isSelected()) {
                    // if we turn on the assembly runner, we also need the assembly editor
                    if (!assemblyCB.isSelected()) {
                        assemblyCB.doClick();
                    } // reenter here
                }
                appXMLConfiguration.setProperty(ViskitConfigurationStore.SIMULATION_RUN_VISIBLE_KEY, simulationRunCB.isSelected());
            } else if (src == dverboseDebugMessagesCB) {
                appXMLConfiguration.setProperty(ViskitConfigurationStore.VERBOSE_DEBUG_MESSAGES_KEY, dverboseDebugMessagesCB.isSelected());
                ViskitStatics.debug = dverboseDebugMessagesCB.isSelected();
            } else if (src == analystReportCB) {
                appXMLConfiguration.setProperty(ViskitConfigurationStore.ANALYST_REPORT_VISIBLE_KEY, analystReportCB.isSelected());
            } else if (src == designOfExperimentsCB) {
                appXMLConfiguration.setProperty(ViskitConfigurationStore.DESIGNOFEXPERIMENTS_DOE_EDITOR_VISIBLE_KEY, designOfExperimentsCB.isSelected());
            } else if (src == cloudSimulationRunCB) {
                appXMLConfiguration.setProperty(ViskitConfigurationStore.CLOUD_SIMULATION_RUN_VISIBLE_KEY, cloudSimulationRunCB.isSelected());
            }
        }
    }

    class ClearEventGraphListHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            EventGraphController ctrlr = (EventGraphController) ViskitGlobals.instance().getEventGraphController();
            ctrlr.clearRecentEventGraphFileSet();
        }
    }

    class ClearAssemblyListHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            AssemblyControllerImpl assemblyController = ViskitGlobals.instance().getAssemblyController();
            assemblyController.clearRecentAssemblyFileList();
        }
    }

    private static void clearClassPathEntries() {
        // Always reinitialize the prop config. We may have changed projects
        getXMLConfigurations();
        projectXMLConfiguration.clearTree(ViskitConfigurationStore.X_CLASSPATHS_CLEAR_KEY);
    }

    /** Method to facilitate putting project/lib entries on the classpath
     * @param extraClassPathEntries a list of (jar/.class) entries to include in extraClassPaths.path[@value]
     */
    public static void saveExtraClasspathEntries(String[] extraClassPathEntries) {
        String[] extraClassPathArray = getExtraClassPathArray();
        if (Arrays.equals(extraClassPathEntries, extraClassPathArray))
            return; // no need to rebuild the LEGO tree

        clearClassPathEntries();

        int ix = 0;
        for (String s : extraClassPathEntries) {
            s = s.replaceAll("\\\\", "/");
            LOG.debug("lis[" + ix + "]: {}", s);
            projectXMLConfiguration.setProperty(ViskitConfigurationStore.X_CLASSPATHS_PATH_KEY + "(" + ix + ")[@value]", s);
            ix++;
        }
        RebuildLEGOTreePanelTask rebuildLEGOTreePanelTask = new RebuildLEGOTreePanelTask();
        rebuildLEGOTreePanelTask.execute();
    }

    public static class RebuildLEGOTreePanelTask extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            setProgress(0);

            // Incase we have custom jars, need to add these to the ClassLoader
            ViskitGlobals.instance().resetWorkingClassLoader();

            Runnable r = () -> {
                ViskitGlobals.instance().rebuildLEGOTreePanels();
            };
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException | InvocationTargetException ex) {
                LOG.error(ex);
            }
            return null;
        }

        @Override
        public void done() {
            setProgress(100);
        }
    }

    private void fillWidgets() {
        DefaultListModel<String> mod = (DefaultListModel<String>) classpathJList.getModel();
        mod.clear();
        if (getExtraClassPathArray() != null) {
            String[] sa = getExtraClassPathArray();
            for (String s : sa) {
                if (!mod.contains(s))
                    mod.addElement(s);
            }
            classpathJList.setModel(mod);
        }

        eventGraphCB.setSelected(isEventGraphEditorVisible());
        assemblyCB.setSelected(isAssemblyEditorVisible());
        simulationRunCB.setSelected(isAssemblySimulationRunVisible());
        designOfExperimentsCB.setSelected(isDesignOfExperimentsVisible());
        cloudSimulationRunCB.setSelected(isCloudSimulationRunVisible());
        analystReportCB.setSelected(isAnalystReportVisible());
        dverboseDebugMessagesCB.setSelected(ViskitStatics.debug = isVerboseDebug());

        String laf = getLookAndFeel();
        if(null == laf) {
            platformLafRB.setSelected(true);
        } else switch (laf) {
            case ViskitConfigurationStore.LOOK_AND_FEEL_PLATFORM:
                platformLafRB.setSelected(true);
                break;
            case ViskitConfigurationStore.LOOK_AND_FEEL_DEFAULT:
                defaultLafRB.setSelected(true);
                break;
            default:
                otherLafRB.setSelected(true);
                otherTF.setEnabled(true);
                otherTF.setText(laf);
                break;
        }
    }

    private void unloadWidgets() {
      // most everything gets instantly updated;  check for pending text entry
      if(otherLafRB.isSelected()) {
          guiXMLConfiguration.setProperty(ViskitConfigurationStore.LOOK_AND_FEEL_KEY, otherTF.getText().trim());
      }
    }

    class cancelButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            modified = false;    // for the caller
            dispose();
        }
    }

    class applyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            unloadWidgets();
            dispose();
        }
    }

    class myCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (modified) {
                int ret = JOptionPane.showConfirmDialog(ViskitUserPreferences.this, "Apply changes?",
                        "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ret == JOptionPane.YES_OPTION) {
                    okButton.doClick();
                } else {
                    cancelButton.doClick();
                }
            } else {
                cancelButton.doClick();
            }
        }
    }
    JFileChooser addChooser;

    class addClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (addChooser == null) {
                addChooser = new JFileChooser(ViskitProject.VISKIT_PROJECTS_DIRECTORY);
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

            int retv = addChooser.showOpenDialog(ViskitUserPreferences.this);
            if (retv == JFileChooser.APPROVE_OPTION) {
                File selFile = addChooser.getSelectedFile();
                String absPath = selFile.getAbsolutePath();
                ((DefaultListModel<String>) classpathJList.getModel()).addElement(absPath.replaceAll("\\\\", "/"));
                installExtraClasspathIntoConfig();
            }
        }
    }

    private void installExtraClasspathIntoConfig() {
        Object[] oa = ((DefaultListModel) classpathJList.getModel()).toArray();
        String[] sa = new String[oa.length];

        System.arraycopy(oa, 0, sa, 0, oa.length);

        saveExtraClasspathEntries(sa);
    }

    class deleteClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = classpathJList.getSelectedIndices();
            if (selected == null || selected.length <= 0) {
                return;
            }
            for (int i = selected.length - 1; i >= 0; i--) {
                ((DefaultListModel) classpathJList.getModel()).remove(selected[i]);
            }
            installExtraClasspathIntoConfig();
        }
    }

    class upClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = classpathJList.getSelectedIndices();
            if (selected == null || selected.length <= 0 || selected[0] <= 0) {
                return;
            }
            moveLine(selected[0], -1);
        }
    }

    private void moveLine(int idx, int polarity) {
        classpathJList.clearSelection();
        DefaultListModel<String> mod = (DefaultListModel<String>) classpathJList.getModel();
        Object o = mod.get(idx);
        mod.remove(idx);
        mod.add(idx + polarity, (String) o);
        installExtraClasspathIntoConfig();
        classpathJList.setSelectedIndex(idx + polarity);
    }

    class downClasspathHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selected = classpathJList.getSelectedIndices();
            int listLen = classpathJList.getModel().getSize();

            if (selected == null || selected.length <= 0 || selected[0] >= (listLen - 1)) {
                return;
            }
            moveLine(selected[0], +1);
        }
    }

    /** @return a String array containing the user's extra classpaths to consider, with default being empty. */
    public static String[] getExtraClassPathArray() 
    {
        // Always update the property configurations, since we may have changed projects
        getXMLConfigurations();
        String[] extraClassPathArray =  new String[0]; // safety first, initialize with a valid value
        try {
            if (projectXMLConfiguration.getStringArray(ViskitConfigurationStore.X_CLASSPATHS_KEY) != null)
                extraClassPathArray = projectXMLConfiguration.getStringArray(ViskitConfigurationStore.X_CLASSPATHS_KEY);
            else LOG.error("getExtraClassPathArray() recieved null value from ViskitConfigurationStore");
        }
        catch (Exception e)
        {
            LOG.error("getExtraClassPathArray() exception: " + e.getMessage());
        }
        return extraClassPathArray;
    }

    /** @return a URL[] of the extra classpaths, to include a path to event graphs */
    public static URL[] getExtraClassPathArraytoURLArray() 
    {
        String[] extraClassPaths = getExtraClassPathArray();
        if (extraClassPaths == null) 
        {
            LOG.error("getExtraClassPathArraytoURLArray() got null String[] from getExtraClassPathArray()");
            return new URL[0];
        }
        URL[] extraClassPathsUrlArray = new URL[extraClassPaths.length];
        int i = 0;
        File file;
        Iterator<Path> itr;
        for (String path : extraClassPaths) 
        {
            file = new File(path);
            // Allow relative paths
            if (path.contains("..")) {
                itr = Path.of(file.toURI()).iterator();
                file = ViskitGlobals.instance().getViskitProject().getProjectRoot();
                while (itr.hasNext() && path.contains("..")) {
                    file = file.getParentFile();
                    path = path.replaceFirst("../", "");
                }
                file = new File(file, path);
            }
            if (file.exists()) {
                try {
                    extraClassPathsUrlArray[i++] = file.toURI().toURL();
                } catch (MalformedURLException ex) {
                    LOG.error(ex);
                }
            }
        }
        for (URL url : extraClassPathsUrlArray)
            LOG.debug("ExtraClassPath URL: {}", url);
        return extraClassPathsUrlArray;
    }

    /**
     * Return the value for the platform look and feel
     * @return the value for the platform look and feel
     */
    public static String getLookAndFeel()
    {
        try // troubleshooting for threading issues, perhaps unneeded now
        {
            if ((ViskitConfigurationStore.instance() != null))
                return ViskitConfigurationStore.instance().getValue(ViskitConfigurationStore.LOOK_AND_FEEL_KEY);
            else 
            {
                LOG.error("getLookAndFeel() received null singleton ViskitConfigurationStore.instance()");
                return "";
            }
        }
        catch (Exception e)
        {
            LOG.error("getLookAndFeel() exception: " + e.getMessage());
            return ""; // if reached, then possibly invoked too soon
        }
    }

    /**
     * Return the value for user preference enabling visibility of tab indicated by property
     * @param property the tab of interest
     * @return the value for tab visibility
     */
    public static boolean isUserPreferenceEnabled(String property)
    {
        if (!tabVisibilityUserPreferenceKeyList.contains(property))
        {
            LOG.error("isUserPreferenceEnabled(" + property + ") is not a legal user preferences key" );
            return false;
        }
        boolean isEnabled;
        try {
            isEnabled = appXMLConfiguration.getBoolean(property);
            return isEnabled;
        }
        catch (Exception ex)
        {
            LOG.error("isUserPreferenceEnabled(" + property + ") exception: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Return if the Event Graph Editor is to be visible
     * @return if the Event Graph Editor is to be visible
     */
    public static boolean isEventGraphEditorVisible() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.EVENTGRAPH_EDITOR_VISIBLE_KEY);
    }

    /**
     * Return if the Assembly Editor is to be visible
     * @return if the Assembly Editor is to be visible
     */
    public static boolean isAssemblyEditorVisible() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.ASSEMBLY_EDITOR_VISIBLE_KEY);
    }

    /**
     * Return if the Assembly Runner is to be visible
     * @return if the Assembly Runner is to be visible
     */
    public static boolean isAssemblySimulationRunVisible() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.SIMULATION_RUN_VISIBLE_KEY);
    }

    /**
     * Return if the Analyst Report Editor is to be visible
     * @return if the Analyst Report Editor is to be visible
     */
    public static boolean isAnalystReportVisible() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.ANALYST_REPORT_VISIBLE_KEY);
    }

    /**
     * Return if verbose debug message are to be printed
     * @return if verbose debug message are to be printed
     */
    public static boolean isVerboseDebug() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.VERBOSE_DEBUG_MESSAGES_KEY);
    }

    /**
     * Return if the Design of Experiments Editor is to be visible
     * @return if the Design of Experiments Editor is to be visible
     */
    public static boolean isDesignOfExperimentsVisible() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.DESIGNOFEXPERIMENTS_DOE_EDITOR_VISIBLE_KEY);
    }

    /**
     * Return if the Cluster Runner is to be visible
     * @return if the Cluster Runner is to be visible
     */
    public static boolean isCloudSimulationRunVisible() {
        return isUserPreferenceEnabled(ViskitConfigurationStore.CLOUD_SIMULATION_RUN_VISIBLE_KEY);
    }
}
