package viskit.model;

import java.util.ArrayList;
import viskit.Vstatics;

import java.awt.*;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id: PropChangeListenerNode.java 1662 2007-12-16 19:44:04Z tdnorbra $
 *
 * An event as seen by the model (not the view)
 */
public class PropChangeListenerNode extends AssemblyNode {
    
    private ArrayList<String> descriptionArray = new ArrayList<String>();

    PropChangeListenerNode(String name, String type) // package access on constructor
    {
        super(name, type);
        setType(type);
    }

    @Override
    public void setType(String typ) {
        super.setType(typ);

        Class<?> myClass = Vstatics.classForName(typ);
        if (myClass != null) {
            Class<?> sampstatcls = Vstatics.classForName("simkit.stat.SampleStatistics");
            if (sampstatcls != null) {
                if (sampstatcls.isAssignableFrom(myClass)) {
                    isSampleStatistics = true;
                }
            }
        }
    }
    private boolean isSampleStatistics = false;

    public boolean isSampleStats() {
        return isSampleStatistics;
    }

    public void setIsSampleStats(boolean b) {
        isSampleStatistics = b;
    }
    
    private boolean clearStatsAfterEachRun = true; // bug 706

    public boolean isClearStatsAfterEachRun() {
        return clearStatsAfterEachRun;
    }

    public void setClearStatsAfterEachRun(boolean b) {
        clearStatsAfterEachRun = b;
    }

    @Override
    public ArrayList<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(ArrayList<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }
}
