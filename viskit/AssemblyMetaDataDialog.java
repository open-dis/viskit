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
 * Date: May 26, 2004
 * Time: 1:35:07 PM
 */

public class AssemblyMetaDataDialog extends MetaDataDialog
{
  protected static MetaDataDialog dialog;
  protected static boolean modified = false;
  
  public static boolean showDialog(JFrame f, Component comp, GraphMetaData gmd)
  {
    if(dialog == null)
      dialog = new AssemblyMetaDataDialog(f,comp,gmd);
    else
      dialog.setParams(comp,gmd);

    dialog.setVisible(true);
      // above call blocks
    return modified;
  }

  AssemblyMetaDataDialog(JFrame f, Component c, GraphMetaData gmd)
  {
    super(f,c,gmd,"Assembly Properties");
  }
}
