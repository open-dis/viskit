package viskit.view;

import edu.nps.util.LogUtilities;
import org.apache.log4j.Logger;
import viskit.ViskitStatics;
import viskit.model.ViskitElement;
import viskit.model.ViskitStateVariable;

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
public class StateVariablesPanel extends ViskitTablePanel
{
    static final Logger LOG = LogUtilities.getLogger(StateVariablesPanel.class);

    private final String[] columnTitles = {"type", "name", "initial value", "description"};
    private final String    plusToolTip = "Add a state variable";
    private final String   minusToolTip = "Removed the selected state variable";

    StateVariablesPanel(int width, int height)
	{
        super(width, height); // separate constructor from initialization
        init(true);
    }

    @Override
    public String[] getColumnTitles()
	{
        return columnTitles;
    }

    @Override
    public String[] getFields(ViskitElement viskitElement, int rowNum)
	{
		ViskitStateVariable stateVariable = (ViskitStateVariable)viskitElement;
        String[] sa = new String[4];
        sa[0] = stateVariable.getType();
        sa[1] = stateVariable.getName();
        sa[2] = stateVariable.getValue();
        sa[3] = stateVariable.getDescription();
		
		// ensure non-empty
		for (int index = 0; index < sa.length; index++)
		{
			if ((sa[index] == null) || sa[index].isEmpty())
			{
				sa[index] = "TODO";
				switch (index)
				{
					case 0:
						stateVariable.setType(sa[index]);
						break;
					case 1:
						stateVariable.setName(sa[index]);
						break;
					case 2:
						stateVariable.setValue(sa[index]);
						break;
					case 3:
						sa[index] = ViskitStatics.DEFAULT_DESCRIPTION;
						stateVariable.setDescription(sa[index]);
						break;
				}
			}
		}
        return sa;
    }

    @Override
    public ViskitElement newRowObject()
	{
        ViskitStateVariable viskitStateVariable = new ViskitStateVariable("name", "int", "0", ViskitStatics.DEFAULT_DESCRIPTION);
        return viskitStateVariable;
    }

    @Override
    public int getNumberVisibleRows() {
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
