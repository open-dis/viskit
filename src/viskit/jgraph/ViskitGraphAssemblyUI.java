package viskit.jgraph;

import java.awt.event.MouseEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jgraph.plaf.basic.BasicGraphUI;
import viskit.ViskitGlobals;
import viskit.control.AssemblyController;
import viskit.model.*;

/**
 * BasicGraphUI must be overridden to allow in node and edge editing.
 * This code is a copy of the appropriate parts of EditorGraph.java, which is
 * part of JGraph examples.
 *
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 3:17:59 PM
 * @version $Id$
 */
public class ViskitGraphAssemblyUI extends BasicGraphUI {

    public ViskitGraphAssemblyUI() {
        super();
    }

    @Override
    protected boolean startEditing(Object cell, MouseEvent event) {

        // We're not concerned with the MouseEvent here, but we can be assured
        // we're here on the EDT

        completeEditing();

        // We'll use our own editors here
        if (graph.isCellEditable(cell)) {
            createEditDialog(cell);
        }

        return false; // any returned boolean does nothing in JGraph v.5.14.0
    }

    private void createEditDialog(Object cell) {

        AssemblyController assemblyController = (AssemblyController) ViskitGlobals.instance().getAssemblyController();
        Object obj = ((DefaultMutableTreeNode) cell).getUserObject();
        if (cell instanceof ViskitAssemblyEdgeCell) {
            if (obj instanceof AdapterEdge) {
                assemblyController.adapterEdgeEdit((AdapterEdge) obj);
            } else if (obj instanceof PropertyChangeEdge) {
                assemblyController.pcListenerEdgeEdit((PropertyChangeEdge) obj);
            } else {
                assemblyController.simEvListenerEdgeEdit((SimEvListenerEdge) obj);
            }
        } else if (cell instanceof ViskitAssemblyCircleCell) {
            assemblyController.evGraphEdit((EvGraphNode) obj);
        } else if (cell instanceof ViskitAssemblyPropertyListCell) {
            assemblyController.pcListenerEdit((PropChangeListenerNode) obj);
        }
    }
}
