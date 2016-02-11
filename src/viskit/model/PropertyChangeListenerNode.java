package viskit.model;

import viskit.ViskitStatics;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:08:08 AM
 * @version $Id$

 An event as seen by the model (not the view)
 */
public class PropertyChangeListenerNode extends AssemblyNode {

    private boolean operation;
    private String  operationOrAssignment;
    private String  indexingExpression;
    private String  value;
    private String  comment;
    private String  description = new String();
	
    private boolean getMean  = false;
    private boolean getCount = false;

    PropertyChangeListenerNode(String name, String type) // package access on constructor
    {
        super(name, type);
        setType(type);
    }

    @Override
    public final void setType(String newType) {
        super.setType(newType);

        Class<?> myClass = ViskitStatics.classForName(newType);
        if (myClass != null) {
            Class<?> sampleStatisticsClass = ViskitStatics.classForName("simkit.stat.SampleStatistics");
            if (sampleStatisticsClass != null) {
                if (sampleStatisticsClass.isAssignableFrom(myClass)) {
                    isSampleStatistics = true;
                }
            }
        }
    }
    private boolean isSampleStatistics = false;

    public boolean isSampleStatistics() {
        return isSampleStatistics;
    }

    public void setIsSampleStatistics(boolean status) {
        isSampleStatistics = status;
    }

    private boolean clearStatisticsAfterEachRun = true; // bug 706

    public boolean isClearStatisticsAfterEachRun() {
        return clearStatisticsAfterEachRun;
    }

    public void setClearStatisticsAfterEachRun(boolean b) {
        clearStatisticsAfterEachRun = b;
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
    public String getIndexingExpression() {
        return indexingExpression;
    }

    @Override
    public String getValue() {
        return value;
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
    public String getDescription() 
	{		
		moveLegacyCommentsToDescription ();
        return description;
    }

    @Override
    public void setDescription(String newDescription) {
        this.description = newDescription;
    }
	
	/**
	 * "Comment" elements are earlier viskit constructs.
	 * If found from an earlier model, append them as part of description and then delete.
	 */
	private void moveLegacyCommentsToDescription ()
	{
		if (description == null)
			description = new String();
		if ((comment != null) && !comment.isEmpty())
		{
			description = comment.trim();
			comment     = "";
		}
	}

    @Override
	@Deprecated
    public String getComment() 
	{
		moveLegacyCommentsToDescription ();
        return description;
    }

}
