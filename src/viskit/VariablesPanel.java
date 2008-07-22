package viskit;

import viskit.model.ViskitElement;
import viskit.model.vStateVariable;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Apr 8, 2004
 * @since 8:49:21 AM
 * @version $Id$
 */
public class VariablesPanel extends ViskitTablePanel {

    private String[] mytitles = {"name", "type", "description"};
    private String plusToolTip = "Add a state variable";
    private String minusToolTip = "Removed the selected state variable";

    VariablesPanel(int wid, int height) {
        super(wid, height);            // separate constructor from initialization
        init(true);
    }

    public String[] getColumnTitles() {
        return mytitles;
    }

    public String[] getFields(Object o, int rowNum) {
        String[] sa = new String[3];
        sa[0] = ((vStateVariable) o).getName();
        sa[1] = ((vStateVariable) o).getType();
        sa[2] = ((vStateVariable) o).getComment();
        return sa;
    }

    public ViskitElement newRowObject() {
        vStateVariable ea = new vStateVariable("name", "int", "description");
        return ea;
    }

    public int getNumVisibleRows() {
        return 3;  // not used if we init super with a height
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
