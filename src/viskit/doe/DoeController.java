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
package viskit.doe;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import edu.nps.util.DirectoryWatch;
import edu.nps.util.FileFilterExtension;
import edu.nps.util.Log4jUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.util.OpenAssembly;
import viskit.xsd.bindings.assembly.EventGraph;
import viskit.xsd.bindings.assembly.SimkitAssembly;
import viskit.xsd.bindings.assembly.TerminalParameter;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 10:36:33 AM
 *
 * Note:  The filechooser stuff is not used since the DOE panel does not expose the corresponding menu items.
 */
public class DoeController implements DoeEvents, ActionListener, OpenAssembly.AssemblyChangeListener 
{
    static final Logger LOG = LogManager.getLogger();

    private final JFileChooser openSaveFileChooser;
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
                olddoOpen(new File(((String) e.getSource())));
                break;

            case OPEN_FILE_CHOOSE:
                checkDirty();
                openSaveFileChooser.setDialogTitle("Open Assembly or DOE File");
                int retv = openSaveFileChooser.showOpenDialog(doeFrame);
                if (retv != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File f = openSaveFileChooser.getSelectedFile();
                olddoOpen(f);
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
        // TODO: provide something other than the systemExit() call.  This is done
        // elsewhere
//        VGlobals.instance().systemExit(0);
    }

    private int checkDirty() {
        DoeFileModel dfm = doeFrame.getModel();
        int reti = JOptionPane.YES_OPTION;
        if (dfm != null) {
            if (((ParameterTableModel) dfm.parameterTable.getModel()).dirty) {
                reti = JOptionPane.showConfirmDialog(doeFrame, "Save changes?");
                if (reti == JOptionPane.YES_OPTION) {
                    doSave(dfm);
                }
            }
        }
        return reti;
    }

    private void clearDirty() {
        DoeFileModel dfm = doeFrame.getModel();
        if (dfm != null) {
            ((ParameterTableModel) dfm.parameterTable.getModel()).dirty = false;
        }
    }

    private void doSaveAs(DoeFileModel dfm) {
        String nm = dfm.userFile.getName();
        if (!nm.endsWith(".grd")) {
            int idx = nm.lastIndexOf('.');
            nm = nm.substring(0, idx);
            nm = nm + ".grd";
        }

        openSaveFileChooser.setSelectedFile(new File(nm));
        int ret = openSaveFileChooser.showSaveDialog(doeFrame);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File f = openSaveFileChooser.getSelectedFile();
        try {
            dfm.marshallJaxb(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(doeFrame, "Error on file save-as: " + e.getMessage(), "File save error", JOptionPane.OK_OPTION);
        }
        dfm.userFile = f;
        doeFrame.setTitle(doeFrame.titleString + " -- " + dfm.userFile.getName());
    }

    private void doSave(DoeFileModel dfm) {
        try {
            dfm.marshallJaxb(dfm.userFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(doeFrame, "Error on file save: " + e.getMessage(), "File save error", JOptionPane.OK_OPTION);
        }
    }

    private void olddoOpen(File f) // todo remove
    {
        try {
            DoeFileModel dfm = FileHandler.openFile(f);
            doeFrame.setModel(dfm);
            doeFrame.installContent();
            doeFrame.setTitle(doeFrame.titleString + " -- " + dfm.userFile.getName());
        } catch (Exception e) {
            LOG.info("bad file open: " + e.getMessage());
        }
    }

    private void doOpen(SimkitAssembly jaxbRoot, File f) {
        DoeFileModel dfm = FileHandler._openFileJaxb(jaxbRoot, f);
        doeFrame.setModel(dfm);
        doeFrame.installContent();
        doeFrame.setTitle(doeFrame.titleString + " -- " + dfm.userFile.getName());
    }
    private JobLauncherTab2 jobLauncher;

    public void setJobLauncher(JobLauncherTab2 jobL) {
        jobLauncher = jobL;
    }
    Vector<TerminalParameter> savedDesignParms;
    Vector<EventGraph> savedEvGraphs;

    public boolean prepRun() {
        DoeFileModel dfm = doeFrame.getModel();

        // check for anything checked
        check:
        {
            int n = dfm.parameterTable.getModel().getRowCount();

            for (int r = 0; r < n; r++) {
                if (((Boolean) dfm.parameterTable.getModel().getValueAt(r, ParameterTableModel.FACTOR_COL))) {
                    break check;
                }
            }
            JOptionPane.showMessageDialog(doeFrame, "No independent variables (factors) selected.",
                    "Sorry", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // clone the jaxbroot (we want to use currently checked widgets, but don't want to force save
    // No clone method, but save the params

        savedDesignParms = new Vector<>(OpenAssembly.inst().jaxbRoot.getDesignParameters());
        saveDoeParmsNoNotify();

        // put Event graphs in place (CDATA stuff)

        savedEvGraphs = new Vector<>(OpenAssembly.inst().jaxbRoot.getEventGraph());
        // event graphs aren't inserted in gridkit xml any more ... dfm.saveEventGraphsToJaxb(loadedEventGraphs);
        return true;
    }

    public Collection getLoadedEventGraphs() {
        return new Vector<>(loadedEventGraphs);
    }

    public void restorePrepRun() {
        SimkitAssembly sa = OpenAssembly.inst().jaxbRoot;
        sa.getDesignParameters().clear();
        sa.getDesignParameters().addAll(savedDesignParms);
        savedDesignParms = null;
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
            LOG.info("no marshall");
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
            LOG.info("no model");
            return null;
        }
    }

    private JFileChooser initFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        FileFilterExtension[] filter = {new FileFilterExtension(".grd", "Doe files (*.grd)", true),
                new FileFilterExtension(".xml", "Assembly files (*.xml)", true)};
        for (FileFilterExtension filter1 : filter) {
            chooser.addChoosableFileFilter(filter1);
        }

        return chooser;
    }

    /**
     * From save button;  this takes the data from the table...possibly edited and puts it into
     * the jaxb SimkitAssembly object, ready to be marshalled with the next Assembly save;
     */
    public void saveDoeParams() {
        saveDoeParmsNoNotify();
        OpenAssembly.inst().doFireActionAssemblyJaxbChanged(this);
    }

    private void saveDoeParmsNoNotify() {
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
                doOpen(OpenAssembly.inst().jaxbRoot, OpenAssembly.inst().file);

                if (jobLauncher != null) {
                    jobLauncher.setAssemblyFile(OpenAssembly.inst().jaxbRoot, OpenAssembly.inst().file); //todo fixfile);
          //todo required? remarshallEvGraphs();
                }
                break;

            case PARAMETER_LOCALLY_EDITED:
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
    private final DirectoryWatch.DirectoryChangeListener myEventGraphListener = new EventGraphListener();
    Vector<File> loadedEventGraphs = new Vector<>();

    /* and here we hear about open event graphs */
    class EventGraphListener implements DirectoryWatch.DirectoryChangeListener {

        @Override
        public void fileChanged(File file, int action, DirectoryWatch source) {
            switch (action) {
                case DirectoryWatch.DirectoryChangeListener.FILE_ADDED:
                    //LOG.info("DoeController got Event Graph change message: FILE_ADDED: "+" " + file.getAbsolutePath());
                    loadedEventGraphs.add(file);
                    break;
                case DirectoryWatch.DirectoryChangeListener.FILE_REMOVED:
                    //LOG.info("DoeController got Event Graph change message: FILE_REMOVED: "+" " + file.getAbsolutePath());
                    loadedEventGraphs.remove(file);
                    break;
                case DirectoryWatch.DirectoryChangeListener.FILE_CHANGED:
                    //LOG.info("DoeController got Event Graph change message: FILE_CHANGED: "+" " + file.getAbsolutePath());
                    break;
                default:
            }
        }
    }
}