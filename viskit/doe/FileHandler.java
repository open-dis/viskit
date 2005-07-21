/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.MovesInstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * Autonomous Underwater Vehicle Workbench
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jul 20, 2005
 * Time: 11:44:06 AM
 */

package viskit.doe;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.List;

public class FileHandler
{
  public static DoeFileModel openFile(File f) throws Exception
  {
    SAXBuilder builder;
    Format form;
    XMLOutputter xmlOut;
    Document doc;
    try {
      builder = new SAXBuilder();
      doc     = builder.build(f);
      xmlOut  = new XMLOutputter();
      form    = Format.getPrettyFormat();
      xmlOut.setFormat(form);
    }
    catch (Exception e) {
      builder = null;
      doc = null;
      xmlOut=null;
      form = null;

      throw new Exception("Error parsing or finding file "+f.getAbsolutePath());
    }
    Element elm = doc.getRootElement();
    if(!elm.getName().equalsIgnoreCase("SimkitAssembly"))
      throw new Exception("Root element must be named \"SimkitAssembly\".");

    DoeFileModel dfm = new DoeFileModel();
    dfm.designParms = getDesignParams(doc);
    dfm.simEntities = getSimEntities(doc);
    //dfm.paramTree = new ParamTree(dfm.simEntities);
    dfm.paramTable = new ParamTable(dfm.simEntities, dfm.designParms);
    return dfm;
  }

  private static List getDesignParams(Document doc) throws Exception
  {
    Element elm = doc.getRootElement();
    return elm.getChildren("TerminalParameter");
  }

  private static List getSimEntities(Document doc) throws Exception
  {
    Element elm = doc.getRootElement();
    return elm.getChildren("SimEntity");
  }

  public static class FileFilterEx extends FileFilter
  {

  /** file extension */
  private String[] _extensions;

  /** message to be displayed in combo-box */
  private String _msg;
  private boolean _showDirs;

  public FileFilterEx(String extension , String msg)
  {
    this(extension,msg,false);
  }

  public FileFilterEx(String extension , String msg, boolean showDirectories)
  {
    this(new String[]{extension},msg,showDirectories);
  }

  public FileFilterEx(String[]extensions, String msg, boolean showDirectories)
  {
    this._extensions = extensions;
    this._msg = msg;
    this._showDirs = showDirectories;

  }
  //----------------------------------------------------------------------------
  /**
   * accept/refuse file
   * @param f a File
   * @return true/false
   */
  public boolean accept(java.io.File f) {
    if(f.isDirectory())
      return _showDirs==true;
    for(int i=0;i<_extensions.length;i++)
      if (f.getName().endsWith(_extensions[i]))
        return true;
    return false;
  }
  //----------------------------------------------------------------------------
  /**
   * get description of file extension
   * @return String value
   */
  public String getDescription() {
    return _msg;
  } // getDescription
} // FileFilterEx
public static class IconFileView extends javax.swing.filechooser.FileView {
  /** file extension */
  String _extension;

  Icon _icon;
  //----------------------------------------------------------------------------
  /**
   * constructor
   * @param extension file extension
   * @param icon an Icon
   */
  public IconFileView(String extension , Icon icon) {
    this._extension = extension;
    this._icon = icon;
  }
  //----------------------------------------------------------------------------
  /**
   * assign icon to file extension
   * @param extension file extension
   * @param sIcon an Icon
   */
  public IconFileView(String extension , String sIcon) {
    this._extension = extension;
    this._icon = new ImageIcon(sIcon);
  } // IconFileView
  //----------------------------------------------------------------------------
  /**
   * get icon
   * @param f a File
   * @return an Icon
   */
  public Icon getIcon(java.io.File f) {
    if (f.getName().endsWith(_extension))
      return _icon;
    else
      return null;
  }
} // IconFileView

}