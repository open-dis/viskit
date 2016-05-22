package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import viskit.model.GraphMetadata;

import javax.swing.JFrame;
import org.apache.log4j.Logger;

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
    static final Logger LOG = LogUtilities.getLogger(EventGraphMetadataDialog.class);

    private static MetadataDialog dialog;
    
    public static boolean showDialog(JFrame f, GraphMetadata graphMetadata)
	{
        if (dialog == null)
		{
            dialog = new EventGraphMetadataDialog(f, graphMetadata);
        } 
		else 
		{
            dialog.setGraphMetadata(f, graphMetadata);
        }
        dialog.setVisible(true); // this call blocks
       
        return modified;
    }

    EventGraphMetadataDialog(JFrame f, GraphMetadata graphMetadata)
	{
        super(f, graphMetadata, "Event Graph Properties");
        remove(this.runtimePanel);  // panel is only used by assembly
        pack();
    }
}
