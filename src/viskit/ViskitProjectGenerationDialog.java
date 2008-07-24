/*
 * Program:      Viskit Discrete Event Simulation (DES) Tool
 *
 * Author(s):    Terry Norbraten
 *               http://www.nps.edu and http://www.movesinstitute.org
 *
 * Created:      24 MAR 2008
 *
 * Filename:     ViskitProjectGenerationDialog.java
 *
 * Compiler:     JDK1.6
 * O/S:          Windows XP Home Ed. (SP2)
 *
 * Description:  Dialog for generating a user home for viskit projects
 *
 * References:   Adapted from AUV Workbench's workbench.main.FileConversionsDialog.java
 *
 * URL:          
 *
 * Requirements: 1) 
 *
 * Assumptions:  1) 
 *
 * TODO:         
 *
 * Copyright (c) 1995-2008 held by the author(s).  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer
 *       in the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the names of the Naval Postgraduate School (NPS)
 *       Modeling Virtual Environments and Simulation (MOVES) Institute
 *       (http://www.nps.edu and http://www.movesinstitute.org)
 *       nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package viskit;

import actions.ActionUtilities;
import javax.swing.*;

import edu.nps.util.BoxLayoutUtils;
import edu.nps.util.SpringUtilities;
import edu.nps.util.FileFilterEx;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * Dialog for generating a user home for viskit projects
 * @version $Id:$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     03 MAR 2008
 *     Time:     2341Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.ViskitProjectGenerationDialog">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 * 
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class ViskitProjectGenerationDialog extends JDialog {

    static Logger log = Logger.getLogger(ViskitProjectGenerationDialog.class);
    static ViskitConfig viskitConfig = ViskitConfig.instance();
    protected static ViskitProjectGenerationDialog dialog;
    private JTextField projectNameTF;
    private JTextField projectLocationTF;
    private JButton generateProjectBtn;
    private JButton helpButton;
    private JButton exitButton;
    private JButton projectLocationChooserBtn;
    private String projectNameResult, projectLocationResult;
    private String locationFileChooserTitle = viskitConfig.getVal("gui.projecttitles.locationfilechoosertitle");

    public static ViskitProjectGenerationDialog instance() {
        if (dialog == null) {
            buildIt();
        }
        return dialog;
    }

    private static void buildIt() {
        dialog = new ViskitProjectGenerationDialog();
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Dialog will appear in center screen
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private ViskitProjectGenerationDialog() {
        setTitle(viskitConfig.getVal("gui.title.projecthome.generation"));
        setModal(true);

        JPanel projHomePanel = new JPanel();
        projHomePanel.setLayout(new BoxLayout(projHomePanel, BoxLayout.Y_AXIS));
        projHomePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        projHomePanel.setToolTipText(viskitConfig.getVal("gui.tooltip.projecthome"));

        JPanel gridPan = new JPanel(new SpringLayout());

        JPanel fileSelector = new JPanel();
        fileSelector.setLayout(new BoxLayout(fileSelector, BoxLayout.X_AXIS));

        projectNameTF = new JTextField(ViskitProject.DEFAULT_PROJECT);
        projectNameTF.addKeyListener(new MyProjectNameListener());
        projectNameTF.setEditable(true);
        projectNameTF.selectAll();
        BoxLayoutUtils.clampHeight(projectNameTF);

        fileSelector.add(projectNameTF);
        fileSelector.add(Box.createHorizontalStrut(5));
        gridPan.add(fileSelector);

        SpringUtilities.makeCompactGrid(gridPan,
                1, 1,  // rows, cols
                0, 0,  // initX, initY
                5, 5); // xPad, yPad

        JPanel lowerPan = new JPanel(new SpringLayout());
        lowerPan.setBorder(BorderFactory.createTitledBorder("Viskit Project Parent Directory"));

        JPanel ofileSelector = new JPanel();
        ofileSelector.setLayout(new BoxLayout(ofileSelector, BoxLayout.X_AXIS));

        // Load a default diretory starting point
        setProjectLocationResult(ViskitProject.MY_VISKIT_PROJECTS_DIR);
        projectLocationTF = new JTextField(getProjectLocationResult());
        projectLocationTF.setEditable(false);
        projectLocationTF.scrollRectToVisible(projectLocationTF.getBounds());
        projectLocationChooserBtn = ActionUtilities.createButton(this, "selectProjectLocation");
        projectLocationChooserBtn.setText("Browse...");

        BoxLayoutUtils.clampWidth(projectLocationChooserBtn);
        BoxLayoutUtils.clampHeight(projectLocationTF, projectLocationChooserBtn);
        ofileSelector.add(projectLocationTF);
        ofileSelector.add(Box.createHorizontalStrut(5));
        ofileSelector.add(projectLocationChooserBtn);

        lowerPan.add(ofileSelector);

        SpringUtilities.makeCompactGrid(lowerPan,
                1, 1, // rows, cols
                0, 0, // initX, initY
                5, 5); // xPad, yPad

        projHomePanel.add(gridPan);
        projHomePanel.add(lowerPan);

        projHomePanel.add(Box.createVerticalStrut(5));

        JPanel buttPan = new JPanel();
        buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
        generateProjectBtn = ActionUtilities.createButton(this, "generateProject");
        generateProjectBtn.setText(viskitConfig.getVal("gui.button.projectgeneration.text"));
        generateProjectBtn.setIcon(
                new ImageIcon(
                Thread.currentThread().getContextClassLoader().getResource(
                "viskit/images/" + viskitConfig.getVal("gui.button.projectgeneration.image"))));

        generateProjectBtn.setToolTipText(viskitConfig.getVal("gui.button.projectgeneration.tt"));
        generateProjectBtn.setAlignmentX(JButton.CENTER_ALIGNMENT);

        buttPan.add(generateProjectBtn);
        buttPan.add(Box.createHorizontalGlue());

        helpButton = ActionUtilities.createButton(this, "help");
        helpButton.setText("Help");
        helpButton.setToolTipText("Please create a Viskit project");
        buttPan.add(helpButton);
        
        exitButton = ActionUtilities.createButton(this, "exit");
        exitButton.setText("Exit");
        exitButton.setToolTipText("Exit Viskit");
        buttPan.add(exitButton);
        
        projHomePanel.add(buttPan);

        JPanel jp = new JPanel();
        jp.add(projHomePanel);
        jp.setOpaque(true);
        setContentPane(jp);
        pack();
    }

    public void generateProject() {
        setProjectNameResult(projectNameTF.getText());
        ViskitConfig.instance().setVal(ViskitConfig.PROJECT_HOME_KEY, ViskitProject.MY_VISKIT_PROJECTS_DIR);
        dialog.setVisible(false);
        dialog.dispose();
        
        // Only way to get this dialog to show up each time it's called
        dialog = null;
    }
    
    public void help() {
        JOptionPane.showMessageDialog(this,
                "Start by creating a Viskit project\n\n" +
                "If you are not sure what to do, simply\n" +
                "click 'Generate Project' and a project will\n" +
                "be created in the default location"
                );
    }

    public void exit() {
        int result = JOptionPane.showConfirmDialog(this,
                "Do you wish to exit Viskit without creating a project?",
                "Cancel and Exit Viskit",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            System.exit(0);
        }
    }
    
    public String getProjectNameResult() {
        return projectNameResult;
    }

    public void setProjectNameResult(String result) {
        this.projectNameResult = result;
        ViskitProject.DEFAULT_PROJECT = getProjectNameResult();
    }

    public String getProjectLocationResult() {
        return projectLocationResult;
    }

    public void setProjectLocationResult(String projectLocationResult) {
        this.projectLocationResult = projectLocationResult;
        ViskitProject.MY_VISKIT_PROJECTS_DIR = getProjectLocationResult();
    }

    class MyProjectNameListener extends KeyAdapter {
        StringBuilder sb;
        Pattern pat;
        Matcher mat;
        boolean fnd;

        MyProjectNameListener() {
            sb = new StringBuilder();
            pat = Pattern.compile("\\w");
        }

        @Override
        public void keyTyped(KeyEvent e) {
            char result = e.getKeyChar();
            if ((result == KeyEvent.VK_BACK_SPACE) && (sb.length() > 0)) {
                sb.deleteCharAt(sb.length() - 1);
            } else if ((result == KeyEvent.VK_DELETE) && (sb.length() > 0)) {
                sb.deleteCharAt(sb.length() - 1);
            } else {
                mat = pat.matcher(new String(new char[]{result}));
                fnd = mat.find();
                if (!fnd) {
                    return;
                }
                sb.append(result);
            }
            projectLocationTF.setText(getProjectLocationResult() + Vstatics.getFileSeparator() + sb.toString());
        }
    }

    public void selectProjectLocation() {
        String location = getFileViaChooser(getProjectLocationResult(), locationFileChooserTitle);
        if (location == null) {
            return;
        }
        projectLocationTF.setText(location);
        setProjectLocationResult(location);
    }

    /** One file chooser for selection of viskit project home directories 
     * 
     * @param root the directory to start looking
     * @param header the title of the file chooser
     * @return the absolute path chosen
     */
    private String getFileViaChooser(String root, String header) {
        JFileChooser chooser = new JFileChooser(root);
        chooser.setApproveButtonText("Select");

        FileFilterEx filter = new FileFilterEx(
                new String[]{""},
                "Directories only", true);
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle(header);

        int result = chooser.showOpenDialog(this);

        return (result == JFileChooser.APPROVE_OPTION) ? chooser.getSelectedFile().getAbsolutePath() : null;
    }
    
} // end class file ViskitProjectGenerationDialog.java

