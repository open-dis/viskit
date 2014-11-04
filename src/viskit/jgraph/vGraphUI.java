package viskit.jgraph;

import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.jgraph.graph.GraphCellEditor;
import org.jgraph.graph.GraphConstants;
import org.jgraph.plaf.basic.BasicGraphUI;
import viskit.EventGraphController;
import viskit.model.CancellingEdge;
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
 * This code is a copy of the appropriate part of EditorGraph.java, which is
 * part of JGraph examples.
 */
public class vGraphUI extends BasicGraphUI {

    private JDialog editDialog;
    private vGraphComponent parent;

    // This will force snap-to-grid, but it won't make the lines straight: that's
    // due to the bezier line drawing code.
    // protected boolean snapSelectedView = true;
    public vGraphUI(vGraphComponent parent) {
        this.parent = parent;
    }

    @Override
    protected boolean startEditing(Object cell, MouseEvent event) {
        completeEditing();
        if (graph.isCellEditable(cell) && editDialog == null) {
            createEditDialog(cell, event);
        }

        return false;
    }

    protected void createEditDialog(Object c, MouseEvent event) {
        final Object cell = c;
        //MouseEvent ev = event;

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                EventGraphController cntl = (EventGraphController) vGraphUI.this.parent.parent.getController();     // todo fix this
                if (cell instanceof vEdgeCell) {
                    Edge e = (Edge) ((vEdgeCell) cell).getUserObject();
                    if (e instanceof SchedulingEdge) {
                        cntl.arcEdit((SchedulingEdge) e);
                    } else //if(e instanceof CancellingEdge)
                    {
                        cntl.canArcEdit((CancellingEdge) e);
                    }
                } else if (cell instanceof CircleCell) {
                    EventNode en = (EventNode) ((CircleCell) cell).getUserObject();
                    cntl.nodeEdit(en);
                }
            }
        });
    }

    /**
     * Stops the editing session. If messageStop is true the editor
     * is messaged with stopEditing, if messageCancel is true the
     * editor is messaged with cancelEditing. If messageGraph is true
     * the graphModel is messaged with valueForCellChanged.
     * @param messageStop
     * @param messageCancel
     * @param messageGraph
     */
    @Override
    protected void completeEditing(boolean messageStop,
            boolean messageCancel,
            boolean messageGraph) {
        if (stopEditingInCompleteEditing && editingComponent != null && editDialog != null) {
            Object oldCell = editingCell;
            GraphCellEditor oldEditor = cellEditor;
            Object newValue = oldEditor.getCellEditorValue();
            boolean requestFocus =
                    (graph != null && (graph.hasFocus() || editingComponent.hasFocus()));
            editingCell = null;
            editingComponent = null;
            if (messageStop) {
                oldEditor.stopCellEditing();
            } else if (messageCancel) {
                oldEditor.cancelCellEditing();
            }
            editDialog.dispose();
            if (requestFocus) {
                graph.requestFocus();
            }
            if (messageGraph) {
                Map map = graphLayoutCache.createNestedMap();
                GraphConstants.setValue(map, newValue);
                Map<Object, Map> nested = new Hashtable<Object, Map>();
                nested.put(oldCell, map);
                graphLayoutCache.edit(nested, null, null, null);
            }
            updateSize();
            // Remove Editor Listener
            if (cellEditorListener != null) {
                oldEditor.removeCellEditorListener(cellEditorListener);
            }
            cellEditor = null;
            editDialog = null;
        }
    }
}
