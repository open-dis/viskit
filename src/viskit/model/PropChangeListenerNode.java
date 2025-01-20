package viskit.model;

import java.util.ArrayList;
import java.util.List;
import viskit.VStatics;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$
 *
 * An event as seen by the model (not the view)
 */
public class PropChangeListenerNode extends AssemblyNode {

    private List<String> descriptionArray = new ArrayList<>();
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
    private String comment;
    private boolean getMean = false;
    private boolean getCount = false;

    PropChangeListenerNode(String name, String type) // package access on constructor
    {
        super(name, type);
        setType(type);
    }

    @Override
    public final void setType(String typ) {
        super.setType(typ);

        Class<?> myClass = VStatics.classForName(typ);
        if (myClass != null) {
            Class<?> sampstatcls = VStatics.classForName("simkit.stat.SampleStatistics");
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

    public boolean isGetMean() {
        return getMean;
    }

    public void setGetMean(boolean b) {
        getMean = b;
    }

    public boolean isGetCount() {
        return getCount;
    }

    public void setGetCount(boolean b) {
        getCount = b;
    }

    @Override
    public List<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(List<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() {
        return operation;
    }
}
