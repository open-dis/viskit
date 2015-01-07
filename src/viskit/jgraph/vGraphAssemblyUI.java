package viskit.jgraph;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import org.jgraph.plaf.basic.BasicGraphUI;
import viskit.VGlobals;
import viskit.control.AssemblyController;
import viskit.model.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 3:17:59 PM
 * @version $Id$
 *
 * BasicGraphUI must be overridden to allow in node and edge editing.
 * This code is a copy of the appropriate parts of EditorGraph.java, which is
 * part of JGraph examples.
 */
public class vGraphAssemblyUI extends BasicGraphUI {

    public vGraphAssemblyUI() {
        super();
    }

    @Override
    protected boolean startEditing(Object cell, MouseEvent event) {

        // We're not concerned with the MouseEvent here

        completeEditing();

        // We'll use our own editors here
        if (graph.isCellEditable(cell)) {
            createEditDialog(cell);
        }

        return false; // any returned boolean does nothing in JGraph v.5.14.0
    }

    private void createEditDialog(Object c) {
        final Object cell = c;

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                AssemblyController cntl = (AssemblyController) VGlobals.instance().getAssemblyController();
                if (cell instanceof vAssemblyEdgeCell) {
                    Object edgeObj = ((vAssemblyEdgeCell) cell).getUserObject();
                    if (edgeObj instanceof AdapterEdge) {
                        cntl.adapterEdgeEdit((AdapterEdge) edgeObj);
                    } else if (edgeObj instanceof PropChangeEdge) {
                        cntl.pcListenerEdgeEdit((PropChangeEdge) edgeObj);
                    } else {
                        cntl.simEvListenerEdgeEdit((SimEvListenerEdge) edgeObj);
                    }
                } else if (cell instanceof AssemblyCircleCell) {
                    Object nodeObj = ((AssemblyCircleCell) cell).getUserObject();
                    cntl.evGraphEdit((EvGraphNode) nodeObj);
                } else if (cell instanceof AssemblyPropListCell) {
                    Object nodeObj = ((AssemblyPropListCell) cell).getUserObject();
                    cntl.pcListenerEdit((PropChangeListenerNode) nodeObj);
                }
            }
        });
    }
}
