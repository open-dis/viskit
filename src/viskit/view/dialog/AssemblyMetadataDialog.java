package viskit.view.dialog;

import viskit.model.GraphMetadata;

import javax.swing.JFrame;

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
public class AssemblyMetadataDialog extends MetadataDialog {

    private static MetadataDialog dialog;
    
    public static boolean showDialog(JFrame f, GraphMetadata graphMetadata) {
        if (dialog == null) {
            dialog = new AssemblyMetadataDialog(f, graphMetadata);
        } else {
            dialog.setGraphMetadata(f, graphMetadata);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    AssemblyMetadataDialog(JFrame f, GraphMetadata graphMetadata) {
        super(f, graphMetadata, "Assembly Properties");
    }
}
