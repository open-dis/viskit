package viskit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class SchedulingEdge extends Edge 
{
    static final Logger LOG = LogManager.getLogger();

    public String priority;

    /** Regex expression for simkit.Priority floating point values */
    public static final String DIGITS = "(\\p{Digit}+)";

    /** Regex expression for simkit.Priority exponential values */
    public static final String EXP = "[eE][+-]?";

    /** Adapted from JDK 1.6 Javadoc on java.lang.Double.valueOf(String s) */
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
             // in addition to strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions from the Java Language Specification, 2nd
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
//    private String comment; // obsolete
//    private List<String> descriptionArray = new ArrayList<>(); // obsolete

    /** package-limited constructor */
    SchedulingEdge() 
    {
    }

    @Override
    Object copyShallow() 
    {
        SchedulingEdge newSchedulingEdge = new SchedulingEdge();
        newSchedulingEdge.opaqueViewObject = opaqueViewObject;
        newSchedulingEdge.setTo(getTo());
        newSchedulingEdge.setFrom(getFrom());
        newSchedulingEdge.setParameters(getParameters());
        newSchedulingEdge.setDelay(getDelay());
        newSchedulingEdge.setDescription(getDescription());
        newSchedulingEdge.setConditional(getConditional());
        newSchedulingEdge.setConditionalDescription(getConditionalDescription());
        newSchedulingEdge.priority = priority;
        return newSchedulingEdge;
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
//    public String getDescription() {
//        return description;
//    }    @Override
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
