package viskit.jgraph;

import java.awt.event.MouseEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jgraph.plaf.basic.BasicGraphUI;
import viskit.VGlobals;
import viskit.control.EventGraphController;
import viskit.model.Edge;
import viskit.model.EventNode;
import viskit.model.SchedulingEdge;

/**
 * BasicGraphUI must be overridden to allow node and edge editing.
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
 */
public class vGraphUI extends BasicGraphUI {

    /** Default constructor */
    public vGraphUI() {
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

    /** Our own implemented dialog editor scheme
     *
     * @param cell the cell to edit
     */
    private void createEditDialog(Object cell) {

        EventGraphController cntl = (EventGraphController) VGlobals.instance().getEventGraphController();
        if (cell instanceof vEdgeCell) {
            Edge e = (Edge) ((DefaultMutableTreeNode) cell).getUserObject();
            
            if (e instanceof SchedulingEdge)
                cntl.schedulingArcEdit(e);
            else
                cntl.cancellingArcEdit(e);
            
        } else if (cell instanceof CircleCell) {
            EventNode en = (EventNode) ((DefaultMutableTreeNode) cell).getUserObject();
            cntl.nodeEdit(en);
        }
    }
}
