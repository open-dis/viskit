package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitStatics;

/**
 * An event as seen by the model (not the view).
 * 
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$
 */
public class PropertyChangeListenerNode extends AssemblyNode
{
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    static final Logger LOG = LogManager.getLogger();
    
    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete
    private boolean statisticTypeMean  = false;
    private boolean statisticTypeCount = false;

    PropertyChangeListenerNode(String name, String type) // package access on constructor
    {
        super(name, type);
        setType(type);
    }

    @Override
    public final void setType(String newType)
    {
        super.setType(newType);

        Class<?> newTypeClass = ViskitStatics.classForName(newType);
        if (newTypeClass != null) 
        {
            Class<?> sampleStatisticsClass = ViskitStatics.classForName("simkit.stat.SampleStatistics");
            if (sampleStatisticsClass != null) {
                if (sampleStatisticsClass.isAssignableFrom(newTypeClass)) {
                    isSampleStatistics = true;
                }
            }
        }
    }
    private boolean isSampleStatistics = false;

    public boolean isSampleStatistics() {
        return isSampleStatistics;
    }

    public void setSampleStatistics(boolean newValue) {
        isSampleStatistics = newValue;
    }

    private boolean clearStatisticsAfterEachRun = true; // bug 706

    public boolean isClearStatisticsAfterEachRun() {
        return clearStatisticsAfterEachRun;
    }

    public void setClearStatisticsAfterEachRun(boolean newValue) {
        clearStatisticsAfterEachRun = newValue;
    }

    public boolean isStatisticTypeMean() {
        return statisticTypeMean;
    }

    public void setStatisticTypeMean(boolean newValue) {
        statisticTypeMean = newValue;
    }
    
    /** method name for reflection use */
    public static final String METHOD_isStatisticTypeCount  = "isStatisticTypeCount";

    public boolean isStatisticTypeCount() {
        return statisticTypeCount;
    }

    public void setStatisticTypeCount(boolean newValue) {
        statisticTypeCount = newValue;
    }

    @Override
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getValue() {
        return value;
    }

    // obsolete
//    @Override
//    public List<String> getDescriptionArray() {
//        return descriptionArray;
//    }
//
//    @Override
//    public void setDescriptionArray(List<String> descriptionArray) {
//        this.descriptionArray = descriptionArray;
//    }

    @Override
    public String getOperationOrAssignment() {
        return operationOrAssignment;
    }

    @Override
    public boolean isOperation() {
        return operation;
    }

    @Override
    public boolean isAssignment() {
        return !operation;
    }
}
