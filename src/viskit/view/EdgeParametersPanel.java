package viskit.view;

import viskit.model.EventArgument;
import viskit.model.vEdgeParameter;
import viskit.model.ViskitElement;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 8, 2004
 * @since 8:49:21 AM
 * @version $Id: EdgeParametersPanel.java 2641 2015-03-01 03:08:08Z tdnorbra $
 */
public class EdgeParametersPanel extends ViskitTablePanel {

    private final String[] mytitles = {"event argument", "value"};
    private List<? extends ViskitElement> argList;

    public EdgeParametersPanel(int wid) {
        super(wid);            // separate constructor from initialization
        init(false);

        // Set the first column background to be the color of the panel (indicating r/o)
        TableColumn tc = tab.getColumnModel().getColumn(0);
        tc.setCellRenderer(new DifferentTableColumnBackgroundRenderer(getBackground()));

        super.addDoubleClickedListener(new myEditListener());
        argList = new ArrayList<>();
    }

    @Override
    public String[] getColumnTitles() {
        return mytitles;
    }

    public String[] xgetFields(Object o) {
        return null;
    }

    @Override
    public String[] getFields(ViskitElement e, int rowNum) {
        String[] sa = new String[2];
        ViskitElement eventArgument = argList.get(rowNum);
        sa[0] = eventArgument.getName() + " (" + eventArgument.getType() + ")";
        sa[1] = e.getValue();
        return sa;
    }

    @Override
    public ViskitElement newRowObject() {
        return new vEdgeParameter("value");
    }

    @Override
    public int getNumVisibleRows() {
        return 3;
    }

    public void setArgumentList(List<? extends ViskitElement> lis) {
        argList = lis;
    }

    /**
     * This is overridden because I've removed the plus minus buttons.  The
     * number of parameters must match the target.
     * @param data edge parameter data
     */
    @Override
    public void setData(List<? extends ViskitElement> data) {
        List<ViskitElement> myList = new ArrayList<>(data);
        int diff = argList.size() - myList.size();
        if (diff == 0) {
            super.setData(data);
        } else if (diff > 0) {

            // more arguments than we've got.
            for (int i = 0; i < diff; i++) {
                myList.add(new vEdgeParameter("0"));
            }
            super.setData(myList);
        } else {
            // fewer arguments than we've got.
            for (int i = 0; i > diff; i--) {
                myList.remove(myList.size() - 1);
            }
            super.setData(myList);
        }
    }

    class DifferentTableColumnBackgroundRenderer extends DefaultTableCellRenderer {

        public DifferentTableColumnBackgroundRenderer(Color c) {
            super();
            setBackground(c);
        }
    }
    private ActionListener alis;

    /**
     * Install external handler for row edit requests.  The row object can be retrieved by
     * ActionEvent.getSource().
     * @param edLis
     */
    @Override
    public void addDoubleClickedListener(ActionListener edLis) {
        alis = edLis;
    }

    class myEditListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (alis != null) {
                Object o = e.getSource();
                int row = findObjectRow(o);
                EventArgument ea = (EventArgument) argList.get(row);
                ((vEdgeParameter) o).bogus = ea.getType();
                ActionEvent ae = new ActionEvent(o, 0, "");
                alis.actionPerformed(ae);
            }
        }
    }
}
