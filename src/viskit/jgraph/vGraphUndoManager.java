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
package viskit.jgraph;

import java.util.Vector;
import javax.swing.event.UndoableEditEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.GraphUndoManager;
import viskit.control.AssemblyController;
import viskit.control.AssemblyControllerImpl;
import viskit.control.EventGraphController;
import viskit.control.EventGraphControllerImpl;
import viskit.mvc.mvcController;

/**
 * This class informs the controller that the selected set has changed. Since
 * we're only using this to (dis)able the cut and copy menu items, it could be
 * argued that this functionality should be internal to the View, and the
 * controller needn't be involved. Nevertheless, the round trip through the
 * controller remains in place. We also implement undo/redo.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.jgraph.vUndoableEditListener">Terry Norbraten, NPS MOVES</a>
 */
public class vGraphUndoManager extends GraphUndoManager implements GraphSelectionListener {

    private Vector<Object> selected;
    private mvcController controller;

    public vGraphUndoManager(mvcController controller) {
        this.controller = controller;
        selected = new Vector<>();
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {

        // Remember the update the menus.  Edit is handled by the super()
        super.undoableEditHappened(e);

        if (controller instanceof EventGraphControllerImpl)
            ((EventGraphControllerImpl) controller).updateUndoRedoStatus();
        else
            ((AssemblyControllerImpl) controller).updateUndoRedoStatus();
    }

    @Override
    public void valueChanged(GraphSelectionEvent e) {

        Object[] oa = e.getCells();
        if (oa == null || oa.length <= 0) {
            return;
        }
        for (Object o : oa) {
            if (e.isAddedCell(o)) {

                // Prevent dups
                if (!selected.contains(((DefaultMutableTreeNode) o).getUserObject()))
                    selected.add(((DefaultMutableTreeNode) o).getUserObject());
            } else {
                selected.remove(((DefaultMutableTreeNode) o).getUserObject());
            }
        }

        if (controller instanceof EventGraphControllerImpl)
            ((EventGraphController) controller).selectNodeOrEdge(selected);
        else
            ((AssemblyController) controller).selectNodeOrEdge(selected);
    }

} // end class file vGraphUndoManager.java
