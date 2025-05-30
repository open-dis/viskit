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

import edu.nps.util.Log4jUtilities;

import java.awt.Dialog;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.ViskitProject;
import viskit.view.dialog.ViskitProjectGenerationDialog3;
import viskit.mvc.MvcController;

/**
 * Utility to help guide the user on Viskit start up options, or when a new
 * project need to be created during runtime
 *
 * @author Mike Bailey
 * @since Aug 2008
 * @version $Id$
 */
public class ViskitProjectSelectionPanel extends javax.swing.JPanel
{
    static final Logger LOG = LogManager.getLogger();
    
    private static JDialog dialog;

    public void showDialog()
    {
        dialog = new JDialog((Dialog) null, true);  // modal
        dialog.setTitle("Viskit Project Selection");
        dialog.setContentPane(this); // viskitProjectSelectionPanel
        dialog.pack();

        // We don't want a JVM exit when the user closes this dialog and merely
        // disposing doesn't kill the JVM.  Will need to force user the use the
        // Exit button, or chose another project open or creation option
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Dialog will appear in center screen
        dialog.setLocationRelativeTo(null);
        
        if (!SwingUtilities.isEventDispatchThread())
        {
            try {
                Runnable r = () -> {
                    dialog.setVisible(true);
                };
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException | InvocationTargetException ex) {
                LOG.error(ex);
            }
        }
        else dialog.setVisible(true);
    }

    /** Creates new form ViskitProjectButtonPanel */
    public ViskitProjectSelectionPanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        openExistingProjectButton = new javax.swing.JButton();
        createNewProjectButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();

        openExistingProjectButton.setText("Open existing Viskit project");
        openExistingProjectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openExistingProjectButtonActionPerformed(evt);
            }
        });

        createNewProjectButton.setText("Create new Viskit project");
        createNewProjectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNewProjectButtonActionPerformed(evt);
            }
        });

        exitButton.setText("Exit Viskit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(createNewProjectButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(openExistingProjectButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(exitButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(openExistingProjectButton)
                .addGap(18, 18, 18)
                .addComponent(createNewProjectButton)
                .addGap(18, 18, 18)
                .addComponent(exitButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    static boolean firstTime = true;

    /** I'm not happy about one minor aspect and that is if the user selects
     * this option, then cancels, Viskit will automatically create and open a
     * ${user.home}/MyViskitProjects/DefaultProject space
     *
     * @param evt the open an existing project event action
     */
private void openExistingProjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExistingProjectButtonActionPerformed
    File file;
    if (!firstTime) 
    {
        MvcController mvcController = ViskitGlobals.instance().getActiveAssemblyController();
        if (mvcController != null) {

            AssemblyView assemblyView = (AssemblyView) mvcController.getView();

            if (assemblyView != null)
                assemblyView.openProject();
        }
    } 
    else 
    {
        firstTime = !firstTime;
        file = ViskitProject.openProjectDirectory(this, ViskitProject.VISKIT_PROJECTS_DIRECTORY);
        ViskitGlobals.instance().setProjectFile(file);

        // NOTE: We have no way of setting the first opened project here as the
        // controller hasn't been created yet to store that info when Viskit
        // first starts up
    }
    // Since this dialog is modal, need to dispose() before we can move along in the startup
    dialog.dispose();

}//GEN-LAST:event_openExistingProjectButtonActionPerformed

private void createNewProjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNewProjectButtonActionPerformed
    File newProjectFile;

    // What we wish to do here is force the user to create a new project space
    // before letting them move on, or, open and existing project, or the only
    // other option is to exit
    do {
        ViskitProjectGenerationDialog3.showDialog();
        if (ViskitProjectGenerationDialog3.cancelled)
            return;
        
        String newProjectPath = ViskitProjectGenerationDialog3.projectPath;
        newProjectFile = new File(newProjectPath);
        if (newProjectFile.exists() && (newProjectFile.isFile() || newProjectFile.list().length > 0))
            JOptionPane.showMessageDialog(this, "Chosen project name already exists, please create a new project name.");
        else
            break; // out of do
        
    } while (true);

    ViskitGlobals.instance().setProjectFile(newProjectFile);

    // NOTE: We have no way of setting the first opened project here as the
    // controller hasn't been created yet to store that info when Viskit first
    // starts up

    // Since this dialog is modal, need to dispose() before we can move along in the startup
    dialog.dispose();

    // The work directory will have already been created by default as ViskitGlobals.init
    // was already called which creates the directory ${user.home}/.viskit
    // during constructor init
}//GEN-LAST:event_createNewProjectButtonActionPerformed

private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed

    dialog.dispose();

    // I don't like the idea of a SystemExit call right here, but the way each
    // frame component needs to develop while starting Viskit; each has to
    // finish before the ViskitGlobals.instance().systemExit(0) call will work
    // properly, so, reluctantly...
    System.exit(0);
}//GEN-LAST:event_exitButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton createNewProjectButton;
    private javax.swing.JButton exitButton;
    private javax.swing.JButton openExistingProjectButton;
    // End of variables declaration//GEN-END:variables
}
