package viskit;

import viskit.model.GraphMetaData;

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
public class AssemblyMetaDataDialog extends MetaDataDialog {

    private static MetaDataDialog dialog;
    
    public static boolean showDialog(JFrame f, GraphMetaData gmd) {
        gmd.description = "NOTE: The description field for this Assembly is not" +
                " currently implemented.  Any text typed in this area will not" +
                " be saved to XML";
        if (dialog == null) {
            dialog = new AssemblyMetaDataDialog(f, gmd);
        } else {
            dialog.setParams(f, gmd);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    AssemblyMetaDataDialog(JFrame f, GraphMetaData gmd) {
        super(f, gmd, "Assembly Properties");
    }
}
