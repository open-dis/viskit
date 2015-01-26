package viskit.jgraph;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import org.jgraph.plaf.basic.BasicGraphUI;
import viskit.VGlobals;
import viskit.control.EventGraphController;
import viskit.model.CancelingEdge;
import viskit.model.Edge;
import viskit.model.EventNode;
import viskit.model.SchedulingEdge;

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
public class vGraphUI extends BasicGraphUI {

    public vGraphUI() {
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

    /** Our own implemented dialog editor scheme
     *
     * @param c the cell to edit
     */
    private void createEditDialog(Object c) {
        final Object cell = c;

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                EventGraphController cntl = (EventGraphController) VGlobals.instance().getEventGraphController();     // todo fix this
                if (cell instanceof vEdgeCell) {
                    Edge e = (Edge) ((vEdgeCell) cell).getUserObject();
                    if (e instanceof SchedulingEdge) {
                        cntl.schedulingArcEdit((SchedulingEdge) e);
                    } else //if(e instanceof CancelingEdge)
                    {
                        cntl.cancellingArcEdit((CancelingEdge) e);
                    }
                } else if (cell instanceof CircleCell) {
                    EventNode en = (EventNode) ((CircleCell) cell).getUserObject();
                    cntl.nodeEdit(en);
                }
            }
        });
    }
}
