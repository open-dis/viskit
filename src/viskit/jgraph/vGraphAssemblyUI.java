package viskit.jgraph;

import org.jgraph.graph.GraphCellEditor;
import org.jgraph.graph.GraphConstants;
import org.jgraph.plaf.basic.BasicGraphUI;
import viskit.ViskitAssemblyController;
import viskit.model.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Map;

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
public class vGraphAssemblyUI extends BasicGraphUI {

    private JDialog editDialog;
    private vGraphAssemblyComponent parent;

    public vGraphAssemblyUI(vGraphAssemblyComponent parent) {
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

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ViskitAssemblyController cntl = (ViskitAssemblyController) vGraphAssemblyUI.this.parent.parent.getController();     // todo fix this
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
    protected void completeEditing(boolean messageStop, boolean messageCancel, boolean messageGraph) {
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
                Map map = GraphConstants.createMap();
                GraphConstants.setValue(map, newValue);
                Map<Object, Map> nested = new Hashtable<Object, Map>();
                nested.put(oldCell, map);
                graphLayoutCache.edit(nested, null, null, null);
            }
            updateSize();
            // Remove Editor Listener
            if (oldEditor != null && cellEditorListener != null) {
                oldEditor.removeCellEditorListener(cellEditorListener);
            }
            cellEditor = null;
            editDialog = null;
        }
    }
}
