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
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THEs
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.doe;

import bsh.Interpreter;
import bsh.NameSpace;
import viskit.util.OpenAssembly;
import viskit.xsd.bindings.assembly.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.*;
import javax.xml.bind.JAXBElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 4:13:25 PM
 * @version $Id$
 */
public class ParameterTableModel extends DefaultTableModel implements TableModelListener
{
    static final Logger LOG = LogManager.getLogger();

    public static final int NUM_COLS = 6;
    public static String[] columnNames = {
        "SimEntity/Parameter name",
        "Type",
        "Value",
        "Is factor?",
        "Minimum",
        "Maximum"
    };
    public static final int NAME_COL = 0;
    public static final int TYPE_COL = 1;
    public static final int VALUE_COL = 2;
    public static final int FACTOR_COL = 3;
    public static final int MIN_COL = 4;
    public static final int MAX_COL = 5;
    Object[][] mydata = new Object[0][0];
    ArrayList<Object[]> rows;
    public Set<Integer> noEditRows = new HashSet<>();
    public Set<Integer> multiRows = new HashSet<>();
    public boolean modified = false;

    /**
     *
     * @param simEntitiesJaxb
     * @param designParametersJaxb
     */
    public ParameterTableModel(List<SimEntity> simEntitiesJaxb, List<TerminalParameter> designParametersJaxb) {
        super(0, 0);

        initializeBeanShell();
        rows = new ArrayList<>();

        int i = 0;
        for (SimEntity simEntity : simEntitiesJaxb) {
            if (!(simEntity instanceof SimEntity)) {
                LOG.error("Error ParameterTableModel() constructor, element not SimEntity");
            }
            processRow(simEntity, "SimEntity_" + i++);
        }
        mydata = rows.toArray(mydata);
        if (designParametersJaxb != null) {
            for (TerminalParameter terminalParameter : designParametersJaxb) {
                if (!(terminalParameter instanceof TerminalParameter)) {
                    LOG.error("Error ParamTableModel(), element not TerminalParameter");
                }
                processDesignParameter(terminalParameter);
            }
        }
        modified = false;
        this.addTableModelListener(ParameterTableModel.this);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (viskit.ViskitStatics.debug) {
            LOG.info("Sending paramlocally editted from ParamTableModel");
        }
        OpenAssembly.instance().doParamLocallyEdited(dummyListener);
    }

    private void processDesignParameter(TerminalParameter terminalParameter) {

        String name = terminalParameter.getName();
        if (name.isEmpty()) {
            LOG.error("Terminal param without name ref!");
        }

        if (terminalParameterHashMap.get(name) == null) {return;}
        
        int row = terminalParameterHashMap.get(name);

        String valueString = terminalParameter.getValue();
        valueString = (valueString == null) ? "" : valueString;
        setValueAt(valueString, row, VALUE_COL);

        JAXBElement<ValueRange> valueRange = terminalParameter.getValueRange();

        setValueAt(valueRange.getValue().getLowValue(), row, MIN_COL);
        setValueAt(valueRange.getValue().getHighValue(), row, MAX_COL);

        setValueAt(true, row, FACTOR_COL); //cb
    }

    Interpreter interpreter;

    private void initializeBeanShell() {
        interpreter = new Interpreter();
        interpreter.setStrictJava(true);       // no loose typing
        NameSpace nameSpace = interpreter.getNameSpace();
        nameSpace.importPackage("simkit.*");
        nameSpace.importPackage("simkit.random.*");
        nameSpace.importPackage("simkit.smdx.*");
        nameSpace.importPackage("simkit.stat.*");
        nameSpace.importPackage("simkit.util.*");
        nameSpace.importPackage("diskit.*");         // 17 Nov 2004
    }
    Map<String, Integer> terminalParameterHashMap = new HashMap<>();
    List<Object> elementsByRow = new ArrayList<>();

    private void processRow(Object obj, String defaultName) {
        Object[] oa = new Object[6];

        if (obj instanceof SimEntity) {
            SimEntity se = (SimEntity) obj;
            String SEname = se.getName();
            if (SEname == null || SEname.length() <= 0) {
                SEname = defaultName;
            }

            oa[NAME_COL] = "<html><b>" + SEname;

            String typ = se.getType();
            typ = (typ == null ? "" : typ);
            oa[TYPE_COL] = "<html><b>" + loseDots(typ);
            oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
            oa[FACTOR_COL] = false;
            rows.add(oa);
            elementsByRow.add(obj);

            noEditRows.add(rows.size() - 1);

            List<Object> children = se.getParameters();
            int i = 1;
            for (Object o : children) {
                processRow(o, SEname + "_" + i++);
            }
        } else if (obj instanceof TerminalParameter) {
            TerminalParameter tp = (TerminalParameter) obj;
            Object nameRefObj = tp.getLinkRef();
            String tpname = defaultName;
            if (nameRefObj != null) {
                if (nameRefObj instanceof String) {
                    tpname = (String) nameRefObj;
                } else if (nameRefObj instanceof TerminalParameter) {
                    tpname = ((TerminalParameter) nameRefObj).getName();
                }
            } else if (tp.getName() != null && tp.getName().length() > 0) {
                tpname = tp.getName();
            }
            if (tpname == null) {
                tpname = defaultName;
            }

            oa[NAME_COL] = tpname;
            String typ = tp.getType();
            typ = (typ == null ? "" : typ);
            oa[TYPE_COL] = loseDots(typ);
            String value = tp.getValue();
            oa[VALUE_COL] = (value == null ? "" : value);
            oa[MIN_COL] = oa[MAX_COL] = ""; // will be editted or filled in from existing file
            oa[FACTOR_COL] = false;
            rows.add(oa);
            elementsByRow.add(obj);

            terminalParameterHashMap.put(tpname, rows.size() - 1);
        } else if (obj instanceof MultiParameter) {
            MultiParameter mp = (MultiParameter) obj;
            String MPname = mp.getName();
            if (MPname == null || MPname.length() <= 0) {
                MPname = defaultName;
            }
            oa[NAME_COL] = MPname;
            String typ = mp.getType();
            oa[TYPE_COL] = loseDots(typ == null ? "" : typ);
            oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
            oa[FACTOR_COL] = false;
            rows.add(oa);
            elementsByRow.add(obj);

            multiRows.add(rows.size() - 1);
            List<Object> children = mp.getParameters();
            int i = 1;
            for (Object o : children) {
                processRow(o, MPname + "_" + i++);
            }
        } else if (obj instanceof FactoryParameter) {
            FactoryParameter fp = (FactoryParameter) obj;
            String FPname = fp.getName();
            if (FPname == null || FPname.length() <= 0) {
                FPname = defaultName;
            }

            oa[NAME_COL] = FPname;
            String typ = fp.getType();
            oa[TYPE_COL] = typ; //loseDots(typ)
            oa[VALUE_COL] = oa[MIN_COL] = oa[MAX_COL] = "";
            oa[FACTOR_COL] = false;
            rows.add(oa);
            elementsByRow.add(obj);

            multiRows.add(rows.size() - 1);

            List<Object> children = fp.getParameters();
            int i = 1;
            for (Object o : children) {
                processRow(o, FPname + "_" + i++);
            }
        } else if (obj instanceof Coordinate) {
            // Do nothing
        } else {
            LOG.error("Error ParamTableModel.processRow, unknown type: " + obj);
        }
    }

    public Object getElementAtRow(int r) {
        return elementsByRow.get(r);
    }

    public Object[] getRowData(int r) {
        return mydata[r];
    }

    private String loseDots(String typ) {
        int dot = typ.lastIndexOf('.');
        if (dot != -1) {
            typ = typ.substring(dot + 1);
        }
        return typ;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return mydata == null ? 0 : mydata.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return mydata[row][col];
    }

    @Override
    public Class getColumnClass(int c) // class name miscapitalization by Java superclass
    {
        //return getValueAt(0, c).getClass();
        switch (c) {
            case NAME_COL:
            case TYPE_COL:
            case VALUE_COL:
            case MIN_COL:
            case MAX_COL:
                return String.class;
            case FACTOR_COL:
                return Boolean.class;
            default:
                //assert false:"Column error in ParameterTableModel";
                LOG.error("Column error in ParamTableModel");
        }
        return null;
    }

    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == TYPE_COL) {
            return false;
        }
        Integer rowKey = row;
        if (noEditRows.contains(rowKey)) {
            return false;
        }
        return col <= TYPE_COL || !multiRows.contains(rowKey);
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    @Override
    public void setValueAt(Object value, int row, int col) {
        mydata[row][col] = value;
        modified = true;

        fireTableCellUpdated(row, col);
    }
    OpenAssembly.AssemblyChangeListener dummyListener = new OpenAssembly.AssemblyChangeListener() {

        @Override
        public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) {
        }

        @Override
        public String getHandle() {
            return "Design of Experiments";
        }
    };
}
