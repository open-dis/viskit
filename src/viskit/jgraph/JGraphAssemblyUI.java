package viskit.jgraph;

import java.awt.event.MouseEvent;
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
public class JGraphAssemblyUI extends BasicGraphUI 
{

    public JGraphAssemblyUI()
	{
        super();
    }

    @Override
    protected boolean startEditing(Object cell, MouseEvent event)
	{
        // We're not concerned with the MouseEvent here

        completeEditing();

        // We'll use our own editors here
        if (graph.isCellEditable(cell))
		{
            createEditDialog(cell);
        }
        return false; // any returned boolean does nothing in JGraph v.5.14.0
    }

    private void createEditDialog(Object jGraphCell)
	{
        AssemblyController assemblyController = (AssemblyController) ViskitGlobals.instance().getAssemblyController();
        if (jGraphCell instanceof vAssemblyEdgeCell) 
		{
            Object assemblyEdgeCell = ((vAssemblyEdgeCell) jGraphCell).getUserObject();
            if (assemblyEdgeCell instanceof AdapterEdge) 
			{
                assemblyController.adapterEdgeEdit((AdapterEdge) assemblyEdgeCell);
            } 
			else if (assemblyEdgeCell instanceof PropertyChangeListenerEdge) 
			{
                assemblyController.propertyChangeListenerEdgeEdit((PropertyChangeListenerEdge) assemblyEdgeCell);
            } 
			else 
			{
                assemblyController.simEventListenerEdgeEdit((SimEventListenerEdge) assemblyEdgeCell);
            }
        } 
		else if (jGraphCell instanceof AssemblyCircleCell) 
		{
            Object nodeObj = ((AssemblyCircleCell) jGraphCell).getUserObject();
            assemblyController.eventGraphEdit((EventGraphNode) nodeObj);
        } 
		else if (jGraphCell instanceof AssemblyPropertyListCell) 
		{
            Object nodeObj = ((AssemblyPropertyListCell) jGraphCell).getUserObject();
            assemblyController.propertyChangeListenerEdit((PropertyChangeListenerNode) nodeObj);
        }
    }
}
