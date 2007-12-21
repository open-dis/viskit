package viskit;

import viskit.model.ViskitElement;
import viskit.model.vParameter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 8, 2004
 * @since 8:49:21 AM
 * @version $Id;$
 */
public class ParametersPanel extends ViskitTablePanel {

    private String[] mytitles = {"name", "type", "description"};
    private String plusToolTip = "Add a simulation parameter";
    private String minusToolTip = "Removed the selected parameter";

    ParametersPanel(int wid) {
        this(wid, 0);
    }

    ParametersPanel(int wid, int numRows) {
        super(wid, numRows);             // separate constructor from initialization
        init(true);
    }

    public String[] getColumnTitles() {
        return mytitles;
    }

    public String[] getFields(Object o, int rowNum) {
        String[] sa = new String[3];
        sa[0] = ((vParameter) o).getName();
        sa[1] = ((vParameter) o).getType();
        sa[2] = ((vParameter) o).getComment();
        return sa;
    }

    @Override
    public ViskitElement newRowObject() {
        vParameter ea = new vParameter("name", "int", "description");
        return ea;
    }

    public int getNumVisibleRows() {
        return 3;
    }

    // Custom tooltips
    @Override
    protected String getMinusToolTip() {
        return minusToolTip;
    }

    // Protected methods
    @Override
    protected String getPlusToolTip() {
        return plusToolTip;
    }
}
