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

import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.mvc.MvcAbstractViewFrame;
import viskit.mvc.MvcController;
import viskit.mvc.MvcRecentFileListener;

/** Utility class to help facilitate menu actions for recently opened Viskit
 * projects.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.ParameterizedProjectAction">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class RecentProjectFileSetListener implements MvcRecentFileListener {

    private List<JMenu> openRecentProjectMenus;

    public RecentProjectFileSetListener() {
        openRecentProjectMenus = new ArrayList<>();
    }

    public void addMenuItem(JMenu menuItem) {
        openRecentProjectMenus.add(menuItem);
    }

    @Override
    public void listChanged()
    {
        AssemblyControllerImpl assemblyController = ViskitGlobals.instance().getAssemblyController();
        Set<String> recentProjectFileSet = assemblyController.getRecentProjectFileSet();
        if (openRecentProjectMenus == null)
            openRecentProjectMenus = new ArrayList<>();

        for (JMenu menu : openRecentProjectMenus)
        {
            if  (menu == null)
                 menu = new JMenu(); // TODO why did this happen?
            menu.removeAll();
        }

        String nameOnly;
        Action act;
        JMenuItem mi;
        File f;
        for (String fullPath : recentProjectFileSet) {
            f = new File(fullPath);
            if (!f.exists()) {
                continue;
            }

            for (JMenu menu : openRecentProjectMenus)
            {
                if  (menu == null)
                     menu = new JMenu(); // TODO why did this happen?
                nameOnly = f.getName();
                act = new ParameterizedProjectAction(nameOnly);
                act.putValue(ViskitStatics.FULL_PATH, fullPath);
                mi = new JMenuItem(act);
                mi.setToolTipText(fullPath);
                menu.add(mi);
            }
        }
        if (!recentProjectFileSet.isEmpty()) {

            for (JMenu menu : openRecentProjectMenus) 
            {
                if  (menu == null)
                     menu = new JMenu(); // TODO why did this happen?
                menu.add(new JSeparator());
                act = new ParameterizedProjectAction("clear history");
                act.putValue(ViskitStatics.FULL_PATH, ViskitStatics.CLEAR_PATH_FLAG);  // flag
                mi = new JMenuItem(act);
                mi.setToolTipText("Clear this list");
                menu.add(mi);
            }
        }
    }

    class ParameterizedProjectAction extends AbstractAction {

        ParameterizedProjectAction(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            AssemblyControllerImpl assemblyController = ViskitGlobals.instance().getAssemblyController();

            File fullPath;
            Object obj = getValue(ViskitStatics.FULL_PATH);
            if (obj instanceof String)
                fullPath = new File((String) obj);
            else
                fullPath = (File) obj;

            if (fullPath != null && fullPath.getPath().equals(ViskitStatics.CLEAR_PATH_FLAG)) {
                assemblyController.clearRecentProjectFileSet();
            } else {
                assemblyController.doProjectCleanup();
                assemblyController.openProject(fullPath);

//              ((MvcAbstractViewFrame) ((MvcController) assemblyController).getView()).setTitleProjectName(); // unneeded
            }
        }
    }

} // end class RecentProjectFileSetListener
