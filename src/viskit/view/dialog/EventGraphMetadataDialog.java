package viskit.view.dialog;

import viskit.model.GraphMetadata;

import javax.swing.JFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Aug 19, 2004
 * @since 1:35:07 PM
 * @version $Id$
 */
public class EventGraphMetadataDialog extends MetadataDialog 
{
    static final Logger LOG = LogManager.getLogger();

    private static MetadataDialog dialog;
    
    public static boolean showDialog(JFrame frame, GraphMetadata graphMetadata) 
    {
        ViskitGlobals.instance().getMainFrame().selectEventGraphEditorTab();
        if (dialog == null) {
            dialog = new EventGraphMetadataDialog(frame, graphMetadata);
        } 
        else {
            dialog.setParameters(frame, graphMetadata);
        }
        dialog.setVisible(true); // this call blocks
        return modified;
    }

    EventGraphMetadataDialog(JFrame frame, GraphMetadata graphMetadata) 
    {
        super(frame, graphMetadata, "Event Graph Metadata Properties");
        remove(this.runtimePanel);  // only for assembly
        pack();
    }
}
