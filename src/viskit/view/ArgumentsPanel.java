package viskit.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class ArgumentsPanel extends ViskitTablePanel
{
    static final Logger LOG = LogManager.getLogger();
    
    private final String[] mytitles = {"name", "type", "description"};
    private static int count = 0;

    public ArgumentsPanel(int wid) {
        this(wid, 0);
    }

    public ArgumentsPanel(int wid, int numRows) {
        super(wid, numRows);
        initialize(true);                       // separate constructor from initialization
    }

    @Override
    public String[] getColumnTitles() {
        return mytitles;
    }

    @Override
    public String[] getFields(ViskitElement viskitElement, int rowNum) 
    {
        String[] sa = new String[3];
        sa[0] = viskitElement.getName();
        sa[1] = viskitElement.getType();
//        List<String> ar = ((EventArgument) e).getDescriptionArray();
//        if (!ar.isEmpty()) {
//            sa[2] = ((EventArgument) e).getDescriptionArray().get(0);
        if (viskitElement.getDescription() == null)
        {
            sa[2] = viskitElement.getDescription();
        } 
        else {
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
    public int getNumberVisibleRows() {
        return 3;
    }
}
