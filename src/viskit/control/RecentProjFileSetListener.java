/*
Copyright (c) 1995-2015 held by the author(s).  All rights reserved.

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
package viskit.control;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import viskit.VGlobals;
import viskit.VStatics;
import viskit.mvc.mvcAbstractJFrameView;
import viskit.mvc.mvcController;
import viskit.mvc.mvcRecentFileListener;

/** Utility class to help facilitate menu actions for recently opened Viskit
 * projects.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.ParameterizedProjectAction">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class RecentProjFileSetListener implements mvcRecentFileListener {

    private List<JMenu> openRecentProjMenus;

    public RecentProjFileSetListener() {
        openRecentProjMenus = new ArrayList<>();
    }

    public void addMenuItem(JMenu menuItem) {
        openRecentProjMenus.add(menuItem);
    }

    @Override
    public void listChanged() {
        AssemblyController acontroller = (AssemblyController) VGlobals.instance().getAssemblyController();
        Set<File> lis = acontroller.getRecentProjFileSet();

        for (JMenu m : openRecentProjMenus) {
            m.removeAll();
        }

        for (File fullPath : lis) {

            if (!fullPath.exists()) {
                continue;
            }

            for (JMenu m : openRecentProjMenus) {
                String nameOnly = fullPath.getName();
                Action act = new ParameterizedProjAction(nameOnly);
                act.putValue(VStatics.FULL_PATH, fullPath);
                JMenuItem mi = new JMenuItem(act);
                mi.setToolTipText(fullPath.getPath());
                m.add(mi);
            }
        }
        if (!lis.isEmpty()) {

            for (JMenu m : openRecentProjMenus) {
                m.add(new JSeparator());
                Action act = new ParameterizedProjAction("clear");
                act.putValue(VStatics.FULL_PATH, VStatics.CLEAR_PATH_FLAG);  // flag
                JMenuItem mi = new JMenuItem(act);
                mi.setToolTipText("Clear this list");
                m.add(mi);
            }
        }
    }

    class ParameterizedProjAction extends AbstractAction {

        ParameterizedProjAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            AssemblyController acontroller = (AssemblyController) VGlobals.instance().getAssemblyController();

            File fullPath;
            Object obj = getValue(VStatics.FULL_PATH);
            if (obj instanceof String)
                fullPath = new File((String) obj);
            else
                fullPath = (File) obj;

            if (fullPath.getPath().equals(VStatics.CLEAR_PATH_FLAG)) {
                acontroller.clearRecentProjFileSet();
            } else {
                acontroller.doProjectCleanup();
                acontroller.openProject(fullPath);

                ((mvcAbstractJFrameView) ((mvcController) acontroller).getView()).showProjectName();
            }
        }
    }

} // end class file RecentProjFileSetListener.java
