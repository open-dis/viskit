package viskit.view.dialog;

import edu.nps.util.LogUtilities;
import viskit.model.GraphMetadata;

import javax.swing.JFrame;
import org.apache.logging.log4j.Logger;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since May 26, 2004
 * @since 1:35:07 PM
 * @version $Id$
 */
public class AssemblyMetadataDialog extends MetadataDialog 
{
    static final Logger LOG = LogUtilities.getLogger(AssemblyMetadataDialog.class);

    private static MetadataDialog dialog;
    
    public static boolean showDialog(JFrame f, GraphMetadata graphMetadata) 
	{
        if (dialog == null) 
		{
            dialog = new AssemblyMetadataDialog(f, graphMetadata);
        } 
		else 
		{
            dialog.setGraphMetadata(f, graphMetadata);
        }
		dialog.fillWidgets();
        dialog.setVisible(true); // this call blocks
       
        return modified;
    }

    AssemblyMetadataDialog(JFrame f, GraphMetadata graphMetadata) 
	{
        super(f, graphMetadata, "Assembly Properties");
    }
}
