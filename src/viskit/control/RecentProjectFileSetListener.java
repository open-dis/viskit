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

import edu.nps.util.LogUtilities;
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
import org.apache.log4j.Logger;
import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import viskit.mvc.mvcRecentFileListener;

/** Utility class to help facilitate menu actions for recently opened Viskit
 * projects.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.ParameterizedProjectAction">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class RecentProjectFileSetListener implements mvcRecentFileListener 
{
    static final Logger LOG = LogUtilities.getLogger(RecentProjectFileSetListener.class);

    private List<JMenu> recentProjectMenuList;

    public RecentProjectFileSetListener() {
        recentProjectMenuList = new ArrayList<>(4);
    }

    public void addMenuItem(JMenu menuItem) {
        recentProjectMenuList.add(menuItem);
    }

    @Override
    public void recentFileListChanged()
	{
        AssemblyController assemblyController = (AssemblyController) ViskitGlobals.instance().getAssemblyController();
        Set<File> recentProjectFileSet = assemblyController.getRecentProjectFileSet();

        for (JMenu m : recentProjectMenuList)
		{
            if (m != null)
				m.removeAll();
        }

        for (File fullPath : recentProjectFileSet) {

            if (!fullPath.exists())
			{
                continue;
            }
            for (JMenu m : recentProjectMenuList) // TODO why down another level?
			{
                if (m != null) // safety net
				{
					String nameOnly = fullPath.getName();
					Action parameterizedProjectAction = new ParameterizedProjectAction(nameOnly);
					parameterizedProjectAction.putValue(ViskitStatics.FULL_PATH, fullPath);
					JMenuItem mi = new JMenuItem(parameterizedProjectAction);
					mi.setToolTipText(fullPath.getPath());
					m.add(mi);	
				}
            }
        }
        if (!recentProjectFileSet.isEmpty())
		{
            for (JMenu m : recentProjectMenuList)
			{
                if (m != null) // safety net
				{
					m.add(new JSeparator());
					Action clearAction = new ParameterizedProjectAction("clear");
					clearAction.putValue(ViskitStatics.FULL_PATH, ViskitStatics.CLEAR_PATH_FLAG);  // flag
					JMenuItem mi = new JMenuItem(clearAction);
					mi.setToolTipText("Clear this list");
					m.add(mi);
				}
            }
        }
    }

    class ParameterizedProjectAction extends AbstractAction
	{
        ParameterizedProjectAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent ev)
		{
            AssemblyControllerImpl assemblyController = ViskitGlobals.instance().getAssemblyController();

            File projectDirectory;
            Object   projectPathObject = getValue(ViskitStatics.FULL_PATH);
            if      (projectPathObject instanceof String)
                     projectDirectory = new File((String) projectPathObject);
			else if (projectPathObject instanceof File)
                     projectDirectory = (File) projectPathObject;
			else
			{
				LOG.error ("Erroneous projectPathObject=" + projectPathObject);
				return;
			}

            if ( projectDirectory.getPath().equals(ViskitStatics.CLEAR_PATH_FLAG) || (getValue(NAME) == "clear"))
			{
                assemblyController.clearRecentProjectFileSet();
            } 
			else
			{
                assemblyController.doProjectCleanup();
                assemblyController.openProjectDirectory(projectDirectory);
				assemblyController.reportProjectOpenResult (projectDirectory.getName()); // pass original name in case of failure
            }
        }
    }

} // end class file RecentProjectFileSetListener.java
