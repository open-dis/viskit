package viskit;

import viskit.model.GraphMetaData;

import javax.swing.*;
import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Aug 19, 2004
 * Time: 1:35:07 PM
 */

public class EventGraphMetaDataDialog extends MetaDataDialog
{
  public static boolean showDialog(JFrame f, Component comp, GraphMetaData gmd)
  {
    if(dialog == null)
      dialog = new EventGraphMetaDataDialog(f,comp,gmd);
    else
      dialog.setParams(comp,gmd);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  EventGraphMetaDataDialog(JFrame f, Component c, GraphMetaData gmd)
  {
    super(f,c,gmd,"Event Graph Properties");
    remove(this.runtimePanel);  // only for assembly
    pack();
  }
}
