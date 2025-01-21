package viskit.view;

import viskit.model.ViskitElement;
import viskit.model.Viskit2StateVariable;

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
public class StateVariablesPanel extends ViskitTablePanel {

    private final String[] mytitles = {"name", "type", "description"};
    private final String plusToolTip = "Add a state variable";
    private final String minusToolTip = "Removed the selected state variable";

    StateVariablesPanel(int wid, int height) {
        super(wid, height);            // separate constructor from initialization
        init(true);
    }

    @Override
    public String[] getColumnTitles() {
        return mytitles;
    }

    @Override
    public String[] getFields(ViskitElement e, int rowNum) {
        String[] sa = new String[3];
        sa[0] = e.getName();
        sa[1] = e.getType();
        sa[2] = e.getComment();
        return sa;
    }

    @Override
    public ViskitElement newRowObject() {
        Viskit2StateVariable ea = new Viskit2StateVariable("name", "int", "description");
        return ea;
    }

    @Override
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
