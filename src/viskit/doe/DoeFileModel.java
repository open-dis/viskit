/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

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
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
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
package viskit.doe;

import edu.nps.util.Log4jUtilities;
import edu.nps.util.TempFileManager;
import org.jdom.Document;
import viskit.util.OpenAssembly;
import viskit.xsd.bindings.assembly.*;
import viskit.xsd.bindings.assembly.ValueRange;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 12:59:43 PM
 * @version $Id$
 */
public class DoeFileModel
{
    static final Logger LOG = LogManager.getLogger();

    public File userFile;
    public ParameterTable parameterTable;
    public List<TerminalParameter> designParameters;
    public Document jdomDocument;
    public boolean dirty = false;
    public HashMap<SimEntity, TerminalParameter> seTerminalParamsHM;

    private List<SimEntity> simEntityList;
    private final Map<String, Integer> nameSpace = new HashMap<>();

    public File marshallJaxb() throws Exception {
        File tempFile = TempFileManager.createTempFile("DOEtemp", ".xml");
        return marshallJaxb(tempFile);
    }

    public File marshallJaxb(File f) throws Exception {
        FileHandler.marshallJaxb(f);
        return f;
    }

    public void saveEventGraphsToJaxb(Collection<File> evGraphs) {
        SimkitAssembly simkitAssembly = OpenAssembly.inst().jaxbRoot;
        List<EventGraph> lis = simkitAssembly.getEventGraph();
        lis.clear();

        for (File f : evGraphs) {
            try {

                FileReader fr = new FileReader(f);
                StringBuilder sb = new StringBuilder();
                char[] cbuf = new char[4096];
                int retc;
                while ((retc = fr.read(cbuf)) != -1) {
                    sb.append(cbuf, 0, retc);
                }

                EventGraph eventGraph = OpenAssembly.inst().jaxbAssemblyObjectFactory.createEventGraph();
                eventGraph.setFileName(f.getName());
                String s = sb.toString();
                String[] sa = s.split("<\\?xml.*\\?>"); // remove the hdr if present
                if (sa.length == 2) {
                    s = sa[1];
                }
                //eventGraph.getContent().add(0,"<![CDATA["+s.trim() + "]]>");
                eventGraph.setContent(s.trim());

                //eventGraph.getContent().add(0,src.trim());
                lis.add(eventGraph);
            } catch (IOException e) {
                LOG.error("IOException inserting into GRID file " + f.getName() + " :" + e.getMessage());
            }
        }
    }

    public void saveTableEditsToJaxb() {
        SimkitAssembly simkitAssembly = OpenAssembly.inst().jaxbRoot;
        ObjectFactory factory = OpenAssembly.inst().jaxbAssemblyObjectFactory;

        List<TerminalParameter> designParameters = simkitAssembly.getDesignParameters();

        // Throw away existing design points
        designParameters.clear();

        // Go down rows, update the nameRef and value fields (type, content aren't changed)
        // For each row that's a factor (design point) add a design point TP at top
        // If experiment tag doesn't exist add it with default values

        ParameterTableModel ptm = (ParameterTableModel) parameterTable.getModel();
        int n = ptm.getRowCount();

        int dpCount = 0;
        for (int r = 0; r < n; r++) {
            if (!ptm.isCellEditable(r, ParameterTableModel.FACTOR_COL)) {
                continue;
            }

            Object[] rData = ptm.getRowData(r);
            Object el = ptm.getElementAtRow(r);
            TerminalParameter tp = (TerminalParameter) el;

            // 20 Mar 2006, RG's new example for setting design points does not use "setValue", only "setValueRange"
            // I'm going to retain the value bit
            String val = (String) rData[ParameterTableModel.VALUE_COL];
            if (val != null && val.length() > 0) {
                tp.setValue(val);
            }

            if (((Boolean) rData[ParameterTableModel.FACTOR_COL])) {
                String name = (String) rData[ParameterTableModel.NAME_COL];
                name = name.replace('.', '_');  // periods illegal in java identifiers
                tp.setName(name);
                Integer cnt = nameSpace.get(name);
                if (cnt == null) {
                    cnt = 0;

                } else {
                    cnt++;
                }
                nameSpace.put(name, cnt);
                //tp.setLinkRef(name+"_"+cnt.toString()+"_DP");
                tp.setName(name + "_" + cnt);
                // Create a designpoint TP with a name
                TerminalParameter newTP = factory.createTerminalParameter();

                newTP.setName(name + "_" + cnt.toString());
                newTP.setLink(name + "_" + cnt.toString());
                newTP.setType(tp.getType());
                newTP.setValue(tp.getValue());  //may not be required with below:

                // put range
                String lowRange = tp.getValue();     // default
                String highRange = tp.getValue();

                String lowTable = (String) rData[ParameterTableModel.MIN_COL];
                String hiTable = (String) rData[ParameterTableModel.MAX_COL];
                if (lowTable != null && lowTable.length() > 0) {
                    lowRange = lowTable;
                }
                if (hiTable != null && hiTable.length() > 0) {
                    highRange = hiTable;
                }

                ValueRange dr = new ValueRange();
                JAXBElement<ValueRange> jevr = factory.createDoubleRange(dr);

                dr.setLowValue(lowRange);
                dr.setHighValue(highRange);
                newTP.setValueRange(jevr);

                designParameters.add(dpCount++, newTP);

                tp.setLinkRef(newTP);
            }
        }
    }

    /** @return a List of SimEntities for this experiment */
    public List<SimEntity> getSimEntityList() {
        return simEntityList;
    }

    public void setSimEntityList(List<SimEntity> simEntityList) {
        simEntityList = simEntityList;   //jdom
        seTerminalParamsHM = new HashMap<>(simEntityList.size());
    }
}
