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
 * Time: 12:59:43 PM
 */

package viskit.doe;

import org.jdom.Document;
import viskit.OpenAssembly;
import viskit.xsd.bindings.assembly.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DoeFileModel
{
  public File userFile;
  public ParamTable paramTable;
  public List designParms;
  private List simEntities;
  public Document jdomDocument;
  public boolean dirty=false;

  public HashMap seTerminalParamsHM;

/*
  // todo remove the jdom stuff
  public File xmarshall() throws Exception
  {
    File f = File.createTempFile("DOEtemp",".grd");
    return xmarshall(f);
  }

  // todo remove the jdom stuff 
  public File xmarshall(File f) throws Exception
  {
    // Throw away existing design points
    // Go down rows, update the nameRef and value fields (type, content aren't changed)
    // For each row that's a factor (design point) add a design point TP at top
    // If experiment tag doesn't exist add it with default values

    ParamTableModel ptm = (ParamTableModel)paramTable.getModel();
    int n = ptm.getRowCount();

    Element root = jdomDocument.getRootElement();
    root.removeChildren("TerminalParameter"); // design points at the top

    int dpCount = 0;
    for(int r=0;r<n;r++)  {
      if(!ptm.isCellEditable(r,ParamTableModel.FACTOR_COL))
        continue;

      Object[] rData = ptm.getRowData(r);
      Element el = (Element)ptm.getElementAtRow(r);

      String val = (String)rData[ParamTableModel.VALUE_COL];
      if(val != null && val.length()>0)
        el.setAttribute("value",val);

      if(((Boolean)rData[ParamTableModel.FACTOR_COL]).booleanValue() == true) {
        String nmrf = (String)rData[ParamTableModel.NAME_COL];
        if(nmrf != null && nmrf.length()>0)
          el.setAttribute("nameRef",nmrf);
        else
          el.setAttribute("nameRef","noname"+r);

        Element tp = new Element("TerminalParameter");
        tp.setAttribute("type",el.getAttribute("type").getValue());
        if(val != null && val.length()>0)
          tp.setAttribute("value",val);

        tp.setAttribute("name",el.getAttribute("nameRef").getValue());
        Content cont = new CDATA(buildTPContent(rData,el));
        tp.addContent(cont);

        root.getChildren().add(dpCount++,tp); // at top in order
      }
    }

    Element elm = root.getChild("Experiment");
    if(elm == null) {
      elm = new Element("Experiment");
      elm.setAttribute("type","latin-hypercube");
      elm.setAttribute("totalSamples","5");
      elm.setAttribute("runsPerDesignPoint","7");
      elm.setAttribute("timeout","30000");
      List lis = root.getChildren();
      lis.add(lis.size(),elm);   //at bottom
    }

    FileHandler.marshallJdom(f,jdomDocument);
    return f;
  }
*/

  public File marshallJaxb() throws Exception
  {
    File f = File.createTempFile("DOEtemp",".xml");
    return marshallJaxb(f);
  }


  public File marshallJaxb(File f) throws Exception
  {
    FileHandler.marshallJaxb(f);
    return f;
  }


/*
  private static String s0 = "Double[] ";
  private static String s1 = "() {return new Double[] { new Double(";
  private static String s2 = "), new Double(";
  private static String s3 = ") };}";
*/

/*
  private String buildTPContent(Object[] rData, Element elm)
  {
    String name = elm.getAttribute("nameRef").getValue();
    //String type = (String)rData[ParamTableModel.TYPE_COL];
    //String valu = (String)rData[ParamTableModel.VALUE_COL];
    String min  = (String)rData[ParamTableModel.MIN_COL];
    String max  = (String)rData[ParamTableModel.MAX_COL];


    StringBuffer sb = new StringBuffer();
    sb.append(s0);
    sb.append(name);
    sb.append(s1);
    sb.append(min);
    sb.append(s2);
    sb.append(max);
    sb.append(s3);

    return sb.toString();
  }
  private String buildTPContent(Object[] rData, String nmrf)
  {
    //String type = (String)rData[ParamTableModel.TYPE_COL];
    //String valu = (String)rData[ParamTableModel.VALUE_COL];
    String min  = (String)rData[ParamTableModel.MIN_COL];
    String max  = (String)rData[ParamTableModel.MAX_COL];


    StringBuffer sb = new StringBuffer();
    sb.append(s0);
    sb.append(nmrf);
    sb.append(s1);
    sb.append(min);
    sb.append(s2);
    sb.append(max);
    sb.append(s3);

   return sb.toString();
  }
*/

  public void saveEventGraphsToJaxb(Collection evGraphs)
  {
    SimkitAssembly assy = OpenAssembly.inst().jaxbRoot;
    List lis = assy.getEventGraph();
    lis.clear();

    for(Iterator itr = evGraphs.iterator(); itr.hasNext();) {
      File f = (File)itr.next();
      try {
        //SimkitXML2Java s2j = new SimkitXML2Java(f);
        //s2j.unmarshal();
        //String src = s2j.translate();

        FileReader fr = new FileReader(f);
        StringBuffer sb = new StringBuffer();
        char[] cbuf = new char[4096];
        int retc;
        while ((retc = fr.read(cbuf)) != -1) {
          sb.append(cbuf,0,retc);
        }

        EventGraph eg = OpenAssembly.inst().jaxbFactory.createEventGraph();
        eg.setFileName(f.getName());
        String s = sb.toString();
        String[]sa = s.split("<\\?xml.*\\?>"); // remove the hdr if present
        if(sa.length == 2)
          s = sa[1];
        //eg.getContent().add(0,"<![CDATA["+s.trim() + "]]>");
        eg.getContent().add(0,s.trim());

        //eg.getContent().add(0,src.trim());
        lis.add(eg);
      }
      catch (Exception e) {
        System.err.println("IOException inserting into GRID file "+f.getName()+" :"+e.getMessage());
      }
    }
  }

  public void saveTableEditsToJaxb()
  {
    SimkitAssembly assy = OpenAssembly.inst().jaxbRoot;
    ObjectFactory factory = OpenAssembly.inst().jaxbFactory;

    List designParms = assy.getDesignParameters();

    // Throw away existing design points
    designParms.clear();

    // Go down rows, update the nameRef and value fields (type, content aren't changed)
    // For each row that's a factor (design point) add a design point TP at top
    // If experiment tag doesn't exist add it with default values

    ParamTableModel ptm = (ParamTableModel)paramTable.getModel();
    int n = ptm.getRowCount();

    int dpCount = 0;
    for(int r=0;r<n;r++)  {
      if(!ptm.isCellEditable(r,ParamTableModel.FACTOR_COL))
        continue;

      Object[] rData = ptm.getRowData(r);
      Object el = ptm.getElementAtRow(r);
      TerminalParameter tp = (TerminalParameter)el;

      // 20 Mar 2006, RG's new example for setting design points does not use "setValue", only "setValueRange"
      // I'm going to retain the value bit
      String val = (String)rData[ParamTableModel.VALUE_COL];
      if(val != null && val.length()>0)
        tp.setValue(val);

      if(((Boolean)rData[ParamTableModel.FACTOR_COL]).booleanValue()) {
        String name = (String)rData[ParamTableModel.NAME_COL];
        name = name.replace('.','_');  // periods illegal in java identifiers
        tp.setName(name);
        tp.setLinkRef(name+"_DP");

        // Create a designpoint TP with a name
        TerminalParameter newTP;
        try {
          newTP = factory.createTerminalParameter();
        }
        catch (JAXBException e) {
          System.err.println("Can't create TerminalParameter.");
          designParms.clear();
          return;
        }
        //newTP.setName(name);
        newTP.setLink(name+"_DP");
        newTP.setType(tp.getType());
        newTP.setValue(tp.getValue());  //may not be required with below:

        // put range
        String lowRange = tp.getValue();     // default
        String highRange = tp.getValue();

        String lowTable = (String)rData[ParamTableModel.MIN_COL];
        String hiTable = (String)rData[ParamTableModel.MAX_COL];
        if(lowTable != null && lowTable.length()>0)
          lowRange = lowTable;
        if(hiTable != null && hiTable.length()>0)
          highRange = hiTable;

        DoubleRange dr = null;
        try {
          dr = (DoubleRange)factory.createDoubleRange();
        }
        catch (JAXBException e) {
          System.err.println("Can't create DoubleRange.");
          designParms.clear();
          return;
        }

        dr.setLowValue(lowRange);
        dr.setHighValue(highRange);
        newTP.setValueRange(dr);
        
        designParms.add(dpCount++,newTP);


        // Set the nameref of the SimEntity TP to be a ref to the design point tp
        // todo the following is permanently changing the jaxbroot, and will thusly get saved into xml.
        //  The user has not necessarily requested that at this point, so we need to be able to undo the change
        //   after marshalling.
        tp.setLinkRef(newTP);
        //tp.setName(null);

      }
    }
    /* dont do this here.  do it in the runpanel
    Element elm = root.getChild("Experiment");
    if(elm == null) {
      elm = new Element("Experiment");
      elm.setAttribute("type","latin-hypercube");
      elm.setAttribute("totalSamples","5");
      elm.setAttribute("runsPerDesignPoint","7");
      elm.setAttribute("timeout","30000");
      List lis = root.getChildren();
      lis.add(lis.size(),elm);   //at bottom
    }

    FileHandler.marshallJdom(f,jdomDocument);
    return f;
 */

  }
 /*
  public void jaxbMarshall() throws Exception
  {
    JAXBContext jaxbCtx = JAXBContext.newInstance("viskit.xsd.bindings.assembly");
    File of = File.createTempFile("DOEtoCluster",".grd");
    FileOutputStream fos  = new FileOutputStream(of);
    Marshaller m = jaxbCtx.createMarshaller();

    fillRoot();
    m.marshal(jaxbRoot,fos);
  }

  private void fillRoot()
  {
    ObjectFactory oFactory = new ObjectFactory();

    Experiment exp = null;
    try {
      exp = oFactory.createExperiment();
    }
    catch (JAXBException e) {
      System.out.println("Exception in DoeFileModel.fillRoot()");
      e.printStackTrace();
    }
    exp.setType("latin-hypercube");
    exp.setTotalSamples("100");
    exp.setRunsPerDesignPoint("5");
    jaxbRoot.setExperiment(exp);

  }

*/
/*
  private void setDesignPoints(ObjectFactory oFactory)
  {
    List dps = jaxbRoot.getDesignParameters();
    dps.clear();
    TableModel tm = paramTable.getModel();
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
*/
  public List getSimEntities()
  {
    return simEntities;
  }
  public void setSimEntities(List se)
  {
    simEntities = se;   //jdom
    seTerminalParamsHM = new HashMap(se.size());

    // switch to jaxb
/*
    se = jaxbRoot.getSimEntity();
    for(Iterator itr = se.iterator(); itr.hasNext();) {
      SimEntity sime = (SimEntity)itr.next();

      sime.
      String nm = sime.getName();
      seTerminalParamsHM.put(nm,sime);
    }
*/

  }

/*
  private void addTPs(List lis)
  {
    for(Iterator itr=lis.iterator(); itr.hasNext();) {
      Object o = itr.next();
      if(o instanceof TerminalParameter) {
        TerminalParameter tp = (TerminalParameter)o;

      }
    }
  }
*/
}