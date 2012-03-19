package viskit;

import java.util.List;
import viskit.model.EventArgument;
import viskit.model.ViskitElement;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 8, 2004
 * @since 8:49:21 AM
 * @version $Id$
 */
public class ArgumentsPanel extends ViskitTablePanel {

    private String[] mytitles = {"name", "type", "description"};
    private static int count = 0;

    ArgumentsPanel(int wid) {
        this(wid, 0);
    }

    ArgumentsPanel(int wid, int numRows) {
        super(wid, numRows);
        init(true);                       // separate constructor from initialization
    }

    @Override
    public String[] getColumnTitles() {
        return mytitles;
    }

    @Override
    public String[] getFields(Object o, int rowNum) {
        String[] sa = new String[3];
        sa[0] = ((EventArgument) o).getName();
        sa[1] = ((EventArgument) o).getType();
        List<String> ar = ((EventArgument) o).getDescription();
        if (ar.size() > 0) {
            sa[2] = ((EventArgument) o).getDescription().get(0);
        } else {
            sa[2] = "";
        }
        return sa;
    }

    @Override
    public ViskitElement newRowObject() {
        EventArgument ea = new EventArgument();
        ea.setName("arg_" + count++);
        ea.setType("int");
        return ea;
    }

    @Override
    public int getNumVisibleRows() {
        return 3;
    }
}
