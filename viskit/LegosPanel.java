package viskit;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: May 14, 2004
 * Time: 9:44:55 AM
 */

public class LegosPanel extends ClassPanel
{
  JButton plus,minus;
  LegosTree tree ;
  public LegosPanel(LegosTree ltree)
  {
    super(ltree,"Event Graphs","Add event graph class file, XML file or directory root to this list",
                               "Remove event graph class file, XML file or directory from this list");
  }
}
