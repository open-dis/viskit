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

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 10:36:33 AM
 */
package viskit.doe;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.FileFilterEx;
import edu.nps.util.LogUtilities;
import org.apache.logging.log4j.Logger;
import viskit.util.OpenAssembly;
import viskit.xsd.bindings.assembly.EventGraph;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.bindings.assembly.TerminalParameter;

/**
 * Note:  The filechooser stuff is not used since the DOE panel does not expose the corresponding menu items.
 */
public class DoeController implements DoeEvents, ActionListener, OpenAssembly.AssemblyChangeListener 
{
    static final Logger LOG = LogUtilities.getLogger(DoeController.class);

    private JFileChooser openSaveFileChooser;
    private DoeMainFrame doeFrame;

    public void setDoeFrame(DoeMainFrame frame) {
        doeFrame = frame;
    }

    public DoeController() {
        openSaveFileChooser = initFileChooser();
    }

    // Event handling code;

    public void actionPerformed(char c) {
        actionPerformed(c, new Object());    // use dummy
    }

    public void actionPerformed(char c, Object src) {
        actionPerformed(new ActionEvent(src, 0, new String(new char[] {c})));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        char c = e.getActionCommand().charAt(0);

        DoeFileModel dfm;
        switch (c) {
            case OPEN_FILE:
                // Todo remove menu
                checkDirty();
                old_doOpen(new File(((String) e.getSource())));
                break;

            case OPEN_FILE_CHOOSE:
                checkDirty();
                openSaveFileChooser.setDialogTitle("Open Assembly or DOE File");
                int retv = openSaveFileChooser.showOpenDialog(doeFrame);
                if (retv != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File f = openSaveFileChooser.getSelectedFile();
                old_doOpen(f);
                break;

            case SAVE_FILE:
                dfm = doeFrame.getModel();
                if (dfm == null) {
                    return;
                }

                if (dfm.userFile.getName().endsWith(".grd")) {
                    doSave(dfm);
                } else {
                    doSaveAs(dfm);
                }
                clearDirty();
                break;

            case SAVE_FILE_AS:
                dfm = doeFrame.getModel();
                if (dfm == null) {
                    return;
                }
                doSaveAs(dfm);
                clearDirty();
                break;

            case EXIT_APP:
                if (preQuit()) {
                    postQuit();
                }
                break;

            case RUN_JOB:
                doRun();
                break;
        }
    }

    public boolean preQuit() {
        return (checkDirty() != JOptionPane.CANCEL_OPTION);
    }

    public void postQuit() {
        // TODO: provide something other than the sysExit() call.  This is done
        // elsewhere
//        VGlobals.instance().sysExit(0);
    }

    private int checkDirty() {
        DoeFileModel dfm = doeFrame.getModel();
        int response = JOptionPane.YES_OPTION;
        if (dfm != null) {
            if (((ParameterTableModel) dfm.paramTable.getModel()).dirty) {
                response = JOptionPane.showConfirmDialog(doeFrame, "Save changes?");
                if (response == JOptionPane.YES_OPTION) {
                    doSave(dfm);
                }
            }
        }
        return response;
    }

    private void clearDirty()
	
	{
        DoeFileModel doeFileModel = doeFrame.getModel();
        if (doeFileModel != null) {
            ((ParameterTableModel) doeFileModel.paramTable.getModel()).dirty = false;
        }
    }

    private void doSaveAs(DoeFileModel doeFileModel)
	{
        String fileName = doeFileModel.userFile.getName();
        if (!fileName.endsWith(".grd")) // TODO likely switch to Grid.xml or somesuch
		{
            int idx = fileName.lastIndexOf('.');
            fileName = fileName.substring(0, idx);
            fileName = fileName + ".grd";
        }

        openSaveFileChooser.setSelectedFile(new File(fileName));
        int ret = openSaveFileChooser.showSaveDialog(doeFrame);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File f = openSaveFileChooser.getSelectedFile();
        try {
            doeFileModel.marshallJaxb(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(doeFrame, "Error on file save-as: " + e.getMessage(), "File save error", JOptionPane.OK_OPTION);
        }
        doeFileModel.userFile = f;
        doeFrame.setTitle(doeFrame.titleString + " -- " + doeFileModel.userFile.getName());
    }

    private void doSave(DoeFileModel doeFileModel) {
        try {
            doeFileModel.marshallJaxb(doeFileModel.userFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(doeFrame, "Error on file save: " + e.getMessage(), "File save error", JOptionPane.OK_OPTION);
        }
    }

	@Deprecated
    private void old_doOpen(File f) // todo remove
    {
        try {
            DoeFileModel dfm = FileHandler.openFile(f);
            doeFrame.setModel(dfm);
            doeFrame.installContent();
            doeFrame.setTitle(doeFrame.titleString + " -- " + dfm.userFile.getName());
        } catch (Exception e) {
            System.out.println("bad file open: " + e.getMessage());
        }
    }

    private void doOpen(SimkitAssembly jaxbSimkitAssembly, File f) {
        DoeFileModel doeFileModel = FileHandler._openFileJaxb(jaxbSimkitAssembly, f);
        doeFrame.setModel(doeFileModel);
        doeFrame.installContent();
        doeFrame.setTitle(doeFrame.titleString + " -- " + doeFileModel.userFile.getName());
    }
    private JobLauncherTab2 jobLauncher;

    public void setJobLauncher(JobLauncherTab2 jobLauncher) {
        this.jobLauncher = jobLauncher;
    }
    Vector<TerminalParameter> savedDesignParameters;
    Vector<EventGraph> savedEvGraphs;

    public boolean prepRun() {
        DoeFileModel doeFileModel = doeFrame.getModel();

        // check for anything checked
        check:
        {
            int n = doeFileModel.paramTable.getModel().getRowCount();

            for (int r = 0; r < n; r++) {
                if (((Boolean) doeFileModel.paramTable.getModel().getValueAt(r, ParameterTableModel.FACTOR_COLUMN))) {
                    break check;
                }
            }
            JOptionPane.showMessageDialog(doeFrame, "No independent variables (factors) selected.",
                    "Sorry", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // clone the jaxbroot (we want to use currently checked widgets, but don't want to force save
    // No clone method, but save the params

        savedDesignParameters = new Vector<>(OpenAssembly.getInstance().jaxbRoot.getDesignParameters());
        saveDoeParametersNoNotify();

        // put Event graphs in place (CDATA stuff)

        savedEvGraphs = new Vector<>(OpenAssembly.getInstance().jaxbRoot.getEventGraph());
        // eventgraphs aren't inserted in gridkit xml any more ... dfm.saveEventGraphsToJaxb(loadedEventGraphs);
        return true;
    }

    public Collection<File> getLoadedEventGraphs() {
        return new Vector<>(loadedEventGraphs);
    }

    public void restorePrepRun() {
        SimkitAssembly sa = OpenAssembly.getInstance().jaxbRoot;
        sa.getDesignParameters().clear();
        sa.getDesignParameters().addAll(savedDesignParameters);
        savedDesignParameters = null;
        sa.getEventGraph().clear();
        sa.getEventGraph().addAll(savedEvGraphs);
        savedEvGraphs = null;
    }

    private void doRun() {
        prepRun();

        // marshall to a temp file
    // pass to the FileHandler.runFile

        File fil = doTempFileMarshall();

        restorePrepRun();

        DoeFileModel dfm = doeFrame.getModel();
        if (fil != null) {
            if (jobLauncher == null) {
                FileHandler.runFile(fil, dfm.userFile.getName() + " " + new Date().toString(), doeFrame);
            } else {
                FileHandler.runFile(fil, dfm.userFile.getName() + " " + new Date().toString(), jobLauncher);
            }
        } else {
            System.out.println("no marshall");
        }
    }

    public File doTempFileMarshall() {
        DoeFileModel dfm = doeFrame.getModel();
        if (dfm != null) {
            File fil;
            try {
                fil = dfm.marshallJaxb();
                return fil;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            System.out.println("no model");
            return null;
        }
    }

    private JFileChooser initFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Design of Experiment (DOE) file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        FileFilterEx[] filter = {new FileFilterEx(".grd", "Doe files (*.grd)", true),
                new FileFilterEx(".xml", "Assembly files (*.xml)", true)};
        for (FileFilterEx filter1 : filter) {
            chooser.addChoosableFileFilter(filter1);
        }

        return chooser;
    }

    /**
     * From save button;  this takes the data from the table...possibly edited and puts it into
     * the jaxb SimkitAssembly object, ready to be marshalled with the next Assembly save;
     */
    public void saveDoeParams() {
        saveDoeParametersNoNotify();
        OpenAssembly.getInstance().doSendAssyJaxbChanged(this);
    }

    private void saveDoeParametersNoNotify() {
        doeFrame.getModel().saveTableEditsToJaxb();
    }

    public OpenAssembly.AssemblyChangeListener getOpenAssemblyListener() {
        return this;
    }

    @Override
    public String getHandle() {
        return "";
    }

    @Override
    public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
        switch (action) {
            case JAXB_CHANGED:
                break;

            case NEW_ASSEMBLY:
                doOpen(OpenAssembly.getInstance().jaxbRoot, OpenAssembly.getInstance().file);

                if (jobLauncher != null) {
                    jobLauncher.setAssemblyFile(OpenAssembly.getInstance().jaxbRoot, OpenAssembly.getInstance().file); //todo fixfile);
          //todo required? remarshallEvGraphs();
                }
                break;

            case PARAM_LOCALLY_EDITED:
                break;
            case CLOSE_ASSEMBLY:
                break;
            default:
                LOG.error("Program error DoeController.assemblyChanged");
        }

    }

    public DirectoryWatch.DirectoryChangeListener getOpenEventGraphListener() {
        return myEventGraphListener;
    }
    private DirectoryWatch.DirectoryChangeListener myEventGraphListener = new EventGraphListener();
    Vector<File> loadedEventGraphs = new Vector<>();

    /* and here we hear about open event graphs */
    class EventGraphListener implements DirectoryWatch.DirectoryChangeListener {

        @Override
        public void fileChanged(File file, int action, DirectoryWatch source) {
            switch (action) {
                case DirectoryWatch.DirectoryChangeListener.FILE_ADDED:
                    //System.out.println("DoeController got event-graph change message: FILE_ADDED: "+" " + file.getAbsolutePath());
                    loadedEventGraphs.add(file);
                    break;
                case DirectoryWatch.DirectoryChangeListener.FILE_REMOVED:
                    //System.out.println("DoeController got event-graph change message: FILE_REMOVED: "+" " + file.getAbsolutePath());
                    loadedEventGraphs.remove(file);
                    break;
                case DirectoryWatch.DirectoryChangeListener.FILE_CHANGED:
                    //System.out.println("DoeController got event-graph change message: FILE_CHANGED: "+" " + file.getAbsolutePath());
                    break;
                default:
            }
        }
    }
}