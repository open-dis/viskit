package viskit;

import viskit.model.GraphMetaData;

import java.awt.Component;
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
    
    public static boolean showDialog(JFrame f, Component comp, GraphMetaData gmd) {
        if (dialog == null) {
            dialog = new AssemblyMetaDataDialog(f, comp, gmd);
        } else {
            dialog.setParams(comp, gmd);
        }

        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    AssemblyMetaDataDialog(JFrame f, Component c, GraphMetaData gmd) {
        super(f, c, gmd, "Assembly Properties");
    }
}
