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
import javax.swing.table.TableModel;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import viskit.xsd.assembly.SimkitAssemblyXML2Java;
import viskit.xsd.bindings.assembly.*;

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
      doc = builder.build(f);
      xmlOut = new XMLOutputter();
      form = Format.getPrettyFormat();
      xmlOut.setFormat(form);
    }
    catch (Exception e) {
      builder = null;
      doc = null;
      xmlOut = null;
      form = null;

      throw new Exception("Error parsing or finding file " + f.getAbsolutePath());
    }
    Element elm = doc.getRootElement();
    if (!elm.getName().equalsIgnoreCase("SimkitAssembly"))
      throw new Exception("Root element must be named \"SimkitAssembly\".");

    DoeFileModel dfm = new DoeFileModel();
    dfm.userFile = f;
    dfm.designParms = getDesignParams(doc);
    dfm.simEntities = getSimEntities(doc);
    //dfm.paramTree = new ParamTree(dfm.simEntities);
    dfm.paramTable = new ParamTable(dfm.simEntities, dfm.designParms);

    JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
    FileInputStream fis  = new FileInputStream(f);
    Unmarshaller u = jaxbCtx.createUnmarshaller();
    dfm.jaxbRoot = (SimkitAssemblyType) u.unmarshal(fis);

    return dfm;
  }
  public static Document unmarshallJdom(File f) throws Exception
  {
    SAXBuilder builder;

    builder = new SAXBuilder();
    return builder.build(f);
  }

  public static void marshallJdom(File of, Document doc) throws Exception
  {
    XMLOutputter xmlOut = new XMLOutputter();
    Format form = Format.getPrettyFormat();
    form.setOmitDeclaration(true); // lose the <?xml at the top
    xmlOut.setFormat(form);

    FileOutputStream fow = new FileOutputStream(of);
    xmlOut.output(doc,fow);
  }

  public static void runFile(DoeFileModel dfm, JFrame mainFrame)
  {
    try {
/*
      ObjectFactory oFactory = new ObjectFactory();
      Experiment exp = oFactory.createExperiment();
      exp.setType("latin-hypercube");
      exp.setTotalSamples("100");
      exp.setRunsPerDesignPoint("5");
      dfm.jaxbRoot.setExperiment(exp);

      //setDesignPoints(dfm,oFactory);

      File tf = File.createTempFile("DOEtemp",".grd");
      tf.deleteOnExit();
      JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
      FileOutputStream fos  = new FileOutputStream(tf);
      Marshaller m = jaxbCtx.createMarshaller();
      m.marshal(dfm.jaxbRoot,fos);

*/

      new JobLauncher(dfm.userFile.getAbsolutePath(),mainFrame);

      //ExperimentType exp = dfm.jaxbRoot.getExperiment();
      //String runsPerDP = exp.getRunsPerDesignPoint();
      //int dps = dfm.jaxbRoot.getDesignParameters().size();
      //String totS = exp.getTotalSamples();
      //int totSint = Integer.parseInt(totS);
      //new JobLauncher(dfm.userFile.getAbsolutePath(),mainFrame,dps*totSint,Integer.parseInt(runsPerDP));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  private static String s0 = "Double[] ";
  private static String s1 = "() {return new Double[] { new Double(";
  private static String s2 = "), new Double(";
  private static String s3 = ") };";

  private static void setDesignPoints(DoeFileModel dfm, ObjectFactory oFactory)
  {
    List dps = dfm.jaxbRoot.getDesignParameters();
    dps.clear();
    TableModel tm = dfm.paramTable.getModel();
    int rows = tm.getRowCount();
    for(int r=0;r<rows;r++) {
      Boolean b = (Boolean)tm.getValueAt(r,ParamTableModel.FACTOR_COL);
      if(b.booleanValue()==true) {
        String name = (String)tm.getValueAt(r,ParamTableModel.NAME_COL);
        String type = (String)tm.getValueAt(r,ParamTableModel.TYPE_COL);
        String valu = (String)tm.getValueAt(r,ParamTableModel.VALUE_COL);
        String min  = (String)tm.getValueAt(r,ParamTableModel.MIN_COL);
        String max  = (String)tm.getValueAt(r,ParamTableModel.MAX_COL);

        TerminalParameter tp = null;
        try {
          tp = oFactory.createTerminalParameter();
        }
        catch (JAXBException e) {
          e.printStackTrace();
        }
        tp.setValue("5.0"); //test valu);
        tp.setName(name);
        tp.setNameRef(null);
        tp.setType(type);
        List content = tp.getContent();
        content.clear();

        StringBuffer sb = new StringBuffer();
        sb.append(s0);
        sb.append(name);
        sb.append(s1);
        sb.append(min);
        sb.append(s2);
        sb.append(max);
        sb.append(s3);

        content.add(sb.toString());
        dps.add(tp);
      }
    }

  }
  /*
  public void init()
  {
    try {
      jc = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
      oFactory = new ObjectFactory();
      jaxbRoot = oFactory.createSimkitAssembly(); // to start with empty graph
    }
    catch (JAXBException e) {
      JOptionPane.showMessageDialog(null,"Exception on JAXBContext instantiation" +
                                 "\n"+ e.getMessage(),
                                 "XML Error",JOptionPane.ERROR_MESSAGE);
    }
  }

  */
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
    private String[] _extensions;

    private String _msg;
    private boolean _showDirs;

    public FileFilterEx(String extension, String msg)
    {
      this(extension, msg, false);
    }

    public FileFilterEx(String extension, String msg, boolean showDirectories)
    {
      this(new String[]{extension}, msg, showDirectories);
    }

    public FileFilterEx(String[]extensions, String msg, boolean showDirectories)
    {
      this._extensions = extensions;
      this._msg = msg;
      this._showDirs = showDirectories;

    }

    public boolean accept(java.io.File f)
    {
      if (f.isDirectory())
        return _showDirs == true;
      for (int i = 0; i < _extensions.length; i++)
        if (f.getName().endsWith(_extensions[i]))
          return true;
      return false;
    }

    public String getDescription()
    {
      return _msg;
    }
  }

  public static class IconFileView extends javax.swing.filechooser.FileView
  {
    String _extension;

    Icon _icon;

    public IconFileView(String extension, Icon icon)
    {
      this._extension = extension;
      this._icon = icon;
    }

    public IconFileView(String extension, String sIcon)
    {
      this._extension = extension;
      this._icon = new ImageIcon(sIcon);
    }

    public Icon getIcon(java.io.File f)
    {
      if (f.getName().endsWith(_extension))
        return _icon;
      else
        return null;
    }
  }

}