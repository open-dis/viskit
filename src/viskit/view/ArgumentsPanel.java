package viskit.view;

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

    private final String[] columnTitles = {"name", "type", "description"};
    private static int count = 0;

    public ArgumentsPanel(int width) {
        this(width, 0);
    }

    public ArgumentsPanel(int width, int numRows) {
        super(width, numRows);
        init(true);                       // separate constructor from initialization
    }

    @Override
    public String[] getColumnTitles() {
        return columnTitles;
    }

    @Override
    public String[] getFields(ViskitElement viskitElemen, int rowNum) {
        String[] sa = new String[3];
        sa[0] = viskitElemen.getName();
        sa[1] = viskitElemen.getType();
        sa[2] = viskitElemen.getDescription();
        return sa;
    }

    @Override
    public ViskitElement newRowObject()
	{
        EventArgument eventArgument = new EventArgument();
        eventArgument.setName("argument_" + count++);
        eventArgument.setType("int");
        return eventArgument;
    }

    @Override
    public int getNumberVisibleRows() {
        return 3;
    }
}
