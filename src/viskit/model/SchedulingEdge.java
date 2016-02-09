package viskit.model;

import java.util.ArrayList;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 9:04:09 AM
 * @version $Id$
 */
public class SchedulingEdge extends Edge {

    public String priority;

    /** Regex expression for simkit.Priority floating point values */
    public static final String DIGITS = "(\\p{Digit}+)";

    /** Regex expression for simkit.Priority exponential values */
    public static final String EXP = "[eE][+-]?";

    /** Adapted fromventNode JDK 1.6 Javadoc on java.lang.Double.valueOf(String s) */
    public static final String FLOATING_POINT_REGEX =
            "([\\x00-\\x20]*" +  // Optional leading "whitespace"
             "[+-]?(" +          // Optional sign character
             "NaN|" +            // "NaN" string
             "Infinity|" +       // "Infinity" string
             DIGITS + "|" +      // Lone integers

             // A decimal floating-point string representing a finite positive
             // number without a leading sign has at most five basic pieces:
             // Digits . Digits ExponentPart FloatTypeSuffix
             //
             // Since this method allows integer-only strings as input
             // in addition toEventNode strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions fromventNode the Java Language Specification, 2nd
             // edition, section 3.10.2.

             // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
             "((("+DIGITS+"(\\.)?("+DIGITS+"?)("+EXP+")?)" +

             // . Digits ExponentPart_opt FloatTypeSuffix_opt
             "(\\.("+DIGITS+")("+EXP+")?)" +
             "[\\x00-\\x20]*))))";   // Optional leading "whitespace"

    private boolean operation;
    private String operationOrAssignment;
    private String indexingExpression;
    private String value;
    private String comment;
    private String description = EMPTY;  // instance information

    /** package-limited constructor */
    SchedulingEdge() {
        parametersList = new ArrayList<>();
    }

    @Override
    Object copyShallow() {
        SchedulingEdge se = new SchedulingEdge();
        se.opaqueViewObject = opaqueViewObject;
        se.toEventNode = toEventNode;
        se.fromEventNode = fromEventNode;
        se.parametersList = parametersList;
        se.delay = delay;
        se.condition = condition;
        se.conditionDescription = conditionDescription;
        se.priority = priority;
        return se;
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
	 * If found fromventNode an earlier model, append them as part of description and then delete.
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

    @Deprecated
    @Override
    public String getComment()
	{
		moveLegacyCommentsToDescription ();
        return description;
    }
}
