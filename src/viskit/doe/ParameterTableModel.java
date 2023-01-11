/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.movesinstitute.org)
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

import bsh.Interpreter;
import bsh.NameSpace;
import edu.nps.util.LogUtilities;
import viskit.util.OpenAssembly;
import viskit.xsd.bindings.assembly.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.*;
import javax.xml.bind.JAXBElement;
import org.apache.logging.log4j.Logger;
import viskit.model.EventGraphModelImpl;

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
    static final Logger LOG = LogUtilities.getLogger(ParameterTableModel.class);

    Interpreter interpreter;
    Map<String, Integer> terminalParameterHashMap = new HashMap<>();
    List<Object> elementsByRow = new ArrayList<>();

    public static final int NUM_COLS = 6;
    public static String[] columnNames = {
        "SimEntity/Parameter name", // TODO clarify
        "type",
        "value",
        "Is factor?",
        "min",
        "max"
    };
    public static final int   NAME_COLUMN = 0;
    public static final int   TYPE_COLUMN = 1;
    public static final int  VALUE_COLUMN = 2;
    public static final int FACTOR_COLUMN = 3;
    public static final int    MIN_COLUMN = 4;
    public static final int    MAX_COLUMN = 5;
    Object[][] mydata = new Object[0][0];
    Vector<Object[]> rows;
    public Set<Integer> noEditRows = new HashSet<>();
    public Set<Integer> multiRows = new HashSet<>();
    public boolean dirty = false;

    /**
     *
     * @param simEntitiesJaxb
     * @param terminalParametersJaxb
     */
    public ParameterTableModel(List<SimEntity> simEntitiesJaxb, List<TerminalParameter> terminalParametersJaxb) 
	{
        super(0, 0);

        initBeanShell();
        rows = new Vector<>();

        int i = 0;
        for (SimEntity jaxbSimEntity : simEntitiesJaxb) 
		{
            if (!(jaxbSimEntity instanceof SimEntity)) 
			{
                LOG.error("Error ParameterTableModel(), element not SimEntity");
            }
            processRow(jaxbSimEntity, "SimEntity_" + i++);
        }
        mydata = rows.toArray(mydata);
        if (terminalParametersJaxb != null) {
            for (TerminalParameter terminalParameter : terminalParametersJaxb)
			{
                if (!(terminalParameter instanceof TerminalParameter)) {
                    LOG.error("Error ParameterTableModel(), element not TerminalParameter");
                }
                processDesignParam(terminalParameter);
            }
        }
        dirty = false;
        this.addTableModelListener(ParameterTableModel.this);
    }

    @Override
    public void tableChanged(TableModelEvent e) 
	{
        if (viskit.ViskitStatics.debug)
		{
            LOG.info("Sending parameter locally edited from ParameterTableModel");
        }
        OpenAssembly.getInstance().doParamLocallyEdited(dummyListener);
    }

    private void processDesignParam(TerminalParameter terminalParameter) {

        String name = terminalParameter.getName();
        if (name.isEmpty())
		{
            LOG.error("TerminalParameter without name!");
        }

        if (terminalParameterHashMap.get(name) == null) {return;}
        
        int row = terminalParameterHashMap.get(name);

        String value = terminalParameter.getValue();
        value = (value == null) ? "" : value;
        setValueAt(value, row, VALUE_COLUMN);

        JAXBElement<ValueRange> valueRange = terminalParameter.getValueRange();

        setValueAt(valueRange.getValue().getLowValue(),  row, MIN_COLUMN);
        setValueAt(valueRange.getValue().getHighValue(), row, MAX_COLUMN);

        setValueAt(true, row, FACTOR_COLUMN); //cb
    }

    private void initBeanShell() {
        interpreter = new Interpreter();
        interpreter.setStrictJava(true);       // no loose typeing
        NameSpace ns = interpreter.getNameSpace();
        ns.importPackage("simkit.*");
        ns.importPackage("simkit.random.*");
        ns.importPackage("simkit.smdx.*");
        ns.importPackage("simkit.stat.*");
        ns.importPackage("simkit.util.*");
        ns.importPackage("diskit.*");         // 17 Nov 2004
    }

    private void processRow(Object object, String defaultName) 
	{
        Object[] objectArray = new Object[6];

        if (object instanceof SimEntity) 
		{
            SimEntity simEntity = (SimEntity) object;
            String SimEntityName = simEntity.getName();
            if (SimEntityName == null || SimEntityName.length() <= 0) {
                SimEntityName = defaultName;
            }

            objectArray[NAME_COLUMN] = "<html><b>" + SimEntityName;

            String typeName = simEntity.getType();
            typeName = (typeName == null ? "" : typeName);
            objectArray[TYPE_COLUMN] = "<html><b>" + loseDots(typeName);
            objectArray[VALUE_COLUMN] = objectArray[MIN_COLUMN] = objectArray[MAX_COLUMN] = "";
            objectArray[FACTOR_COLUMN] = false;
            rows.add(objectArray);
            elementsByRow.add(object);

            noEditRows.add(rows.size() - 1);

            List<Object> simEntityParameters = simEntity.getParameters();
            int i = 1;
            for (Object o : simEntityParameters)
			{
                processRow(o, SimEntityName + "_" + i++);
            }
        } 
		else if (object instanceof TerminalParameter) 
		{
            TerminalParameter terminalParameter = (TerminalParameter) object;
            Object objectReferenceName = terminalParameter.getLinkRef();
            String terminalParameterName = defaultName;
            if (objectReferenceName != null) 
			{
                if (objectReferenceName instanceof String) 
				{
                    terminalParameterName = (String) objectReferenceName;
                } 
				else if (objectReferenceName instanceof TerminalParameter) 
				{
                    terminalParameterName = ((TerminalParameter) objectReferenceName).getName();
                }
            } 
			else if (terminalParameter.getName() != null && terminalParameter.getName().length() > 0) 
			{
                terminalParameterName = terminalParameter.getName();
            }
            if (terminalParameterName == null) 
			{
                terminalParameterName = defaultName;
            }

            objectArray[NAME_COLUMN]   = terminalParameterName;
            String typeName = terminalParameter.getType();
            typeName = (typeName == null ? "" : typeName);
            objectArray[TYPE_COLUMN]   = loseDots(typeName);
            String value = terminalParameter.getValue();
            objectArray[VALUE_COLUMN]  = (value == null ? "" : value);
            objectArray[MIN_COLUMN]    = objectArray[MAX_COLUMN] = ""; // will be edited or filled in from existing file
            objectArray[FACTOR_COLUMN] = false;
            rows.add(objectArray);
            elementsByRow.add(object);

            terminalParameterHashMap.put(terminalParameterName, rows.size() - 1);
        } 
		else if (object instanceof MultiParameter) 
		{
            MultiParameter multiParameter = (MultiParameter) object;
            String multiParameterName = multiParameter.getName();
            if (multiParameterName == null || multiParameterName.length() <= 0) 
			{
                multiParameterName = defaultName;
            }
            objectArray[NAME_COLUMN] = multiParameterName;
            String typeName = multiParameter.getType();
            objectArray[TYPE_COLUMN]  = loseDots(typeName == null ? "" : typeName);
            objectArray[VALUE_COLUMN]  = objectArray[MIN_COLUMN] = objectArray[MAX_COLUMN] = "";
            objectArray[FACTOR_COLUMN] = false;
            rows.add(objectArray);
            elementsByRow.add(object);

            multiRows.add(rows.size() - 1);
            List<Object> children = multiParameter.getParameters();
            int i = 1;
            for (Object o : children) 
			{
                processRow(o, multiParameterName + "_" + i++);
            }
        } 
		else if (object instanceof FactoryParameter)
		{
            FactoryParameter factoryParameter = (FactoryParameter) object;
            String factoryParameterName = factoryParameter.getName();
            if (factoryParameterName == null || factoryParameterName.length() <= 0)
			{
                factoryParameterName  = defaultName;
            }

            objectArray[NAME_COLUMN]   = factoryParameterName;
            String typeName            = factoryParameter.getType();
            objectArray[TYPE_COLUMN]   = typeName; //loseDots(typ)
            objectArray[VALUE_COLUMN]  = objectArray[MIN_COLUMN] = objectArray[MAX_COLUMN] = "";
            objectArray[FACTOR_COLUMN] = false;
            rows.add(objectArray);
            elementsByRow.add(object);

            multiRows.add(rows.size() - 1);

            List<Object> children = factoryParameter.getParameters();
            int i = 1;
            for (Object o : children) 
			{
                processRow(o, factoryParameterName + "_" + i++);
            }
        } 
		else if (object instanceof Coordinate) 
		{
            // Do nothing
        } 
		else 
		{
            LOG.error("Error ParameterTableModel.processRow, unknown type: " + object);
        }
    }

    public Object getElementAtRow(int r) 
	{
        return elementsByRow.get(r);
    }

    public Object[] getRowData(int r) 
	{
        return mydata[r];
    }

    private String loseDots(String typeName) 
	{
        int dotIndex = typeName.lastIndexOf('.');
        if (dotIndex != -1) 
		{
            typeName = typeName.substring(dotIndex + 1);
        }
        return typeName;
    }

    @Override
    public int getColumnCount() 
	{
        return columnNames.length;
    }

    @Override
    public int getRowCount() 
	{
        return mydata == null ? 0 : mydata.length;
    }

    @Override
    public String getColumnName(int col) 
	{
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) 
	{
        return mydata[row][col];
    }

    @Override
    public Class getColumnClass(int classConstant)
	{
        //return getValueAt(0, c).getClass();
        switch (classConstant)
		{
            case NAME_COLUMN:
            case TYPE_COLUMN:
            case VALUE_COLUMN:
            case MIN_COLUMN:
            case MAX_COLUMN:
                return String.class;
            case FACTOR_COLUMN:
                return Boolean.class;
            default:
                //assert false:"Column error in ParameterTableModel";
                LOG.error("Column error in ParameterTableModel");
        }
        return null;
    }

    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    @Override
    public boolean isCellEditable(int row, int column)
	{
        if (column == TYPE_COLUMN)
		{
            return false;
        }
        Integer rowKey = row;
        if (noEditRows.contains(rowKey))
		{
            return false;
        }
        return column <= TYPE_COLUMN || !multiRows.contains(rowKey);
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    @Override
    public void setValueAt(Object value, int row, int col)
	{
        mydata[row][col] = value;
        dirty = true;

        fireTableCellUpdated(row, col);
    }
    OpenAssembly.AssemblyChangeListener dummyListener = new OpenAssembly.AssemblyChangeListener() 
	{

        @Override
        public void assemblyChanged(int action, OpenAssembly.AssemblyChangeListener source, Object param) 
		{
        }

        @Override
        public String getHandle() 
		{
            return "Design of Experiments"; // TODO seems incorrect
        }
    };
}
