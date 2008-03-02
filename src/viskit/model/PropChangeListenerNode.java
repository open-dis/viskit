package viskit.model;

import java.util.ArrayList;

import viskit.Vstatics;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id: PropChangeListenerNode.java 1662 2007-12-16 19:44:04Z tdnorbra $
 *
 * An event as seen by the model (not the view)
 */
public class PropChangeListenerNode extends AssemblyNode {
    
    private ArrayList<String> descriptionArray = new ArrayList<String>();
    private boolean operation;
    private String operationOrAssignment;
    private String arrayType;
    private String indexingExpression;
    private String stateVarName;
    private String value;
    private String comment;
    private String stateVarType;
    private boolean getMean = false;
    private boolean getCount = false; 

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
    public ArrayList<String> getDescriptionArray() {
        return descriptionArray;
    }

    @Override
    public void setDescriptionArray(ArrayList<String> descriptionArray) {
        this.descriptionArray = descriptionArray;
    }

    @Override
    public String getArrayType() {
        return arrayType;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getStateVarName() {
        return stateVarName;
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

    @Override
    public String getStateVarType() {
        return stateVarType;
    }
}
