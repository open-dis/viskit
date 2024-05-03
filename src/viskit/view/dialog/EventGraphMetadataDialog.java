package viskit.view.dialog;

import viskit.model.GraphMetaData;

import javax.swing.JFrame;

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
public class EventGraphMetaDataDialog extends MetaDataDialog {

    private static MetaDataDialog dialog;
    
    public static boolean showDialog(JFrame f, GraphMetaData gmd) {
        if (dialog == null) {
            dialog = new EventGraphMetaDataDialog(f, gmd);
        } else {
            dialog.setParams(f, gmd);
        }
        dialog.setVisible(true);
        // above call blocks
        return modified;
    }

    EventGraphMetaDataDialog(JFrame f, GraphMetaData gmd) {
        super(f, gmd, "Event Graph Properties");
        remove(this.runtimePanel);  // only for assembly
        pack();
    }
}
