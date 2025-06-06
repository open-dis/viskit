package viskit.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import viskit.ViskitStatics;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 8, 2004
 * @since 2:57:37 PM
 * @version $Id$
 */
public abstract class Edge extends ViskitElement
{
    static final Logger LOG = LogManager.getLogger();

    private EventNode to;
    private EventNode from;
    private List<ViskitElement> parameters = new ArrayList<>();
    private String conditional = new String();
    private String conditionalDescription = new String();
    private String delay = new String();
    private String description = new String();

    abstract Object copyShallow();

    /**
     * @return the to
     */
    public EventNode getTo() {
        return to;
    }

    /**
     * @param to the to to set
     */
    public void setTo(EventNode to) {
        this.to = to;
    }

    /**
     * @return the from
     */
    public EventNode getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(EventNode from) {
        this.from = from;
    }

    /**
     * @return the parameters
     */
    public List<ViskitElement> getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(List<ViskitElement> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the conditional
     */
    public String getConditional() {
        return conditional;
    }

    /**
     * @param conditional the conditional to set
     */
    public void setConditional(String conditional) {
        this.conditional = conditional;
    }

    /**
     * @return the conditionalDescription
     */
    public String getConditionalDescription() {
        return conditionalDescription;
    }

    /**
     * @param conditionalDescription the conditionalDescription to set
     */
    public void setConditionalDescription(String conditionalDescription) {
        this.conditionalDescription = conditionalDescription;
    }

    /**
     * @return the delay
     */
    public String getDelay() {
        return delay;
    }

    /**
     * @param delay the delay to set
     */
    public void setDelay(String delay) {
        this.delay = delay;
    }

    /**
     * @return the description
     */
    @Override
    public String getDescription() {
        return ViskitStatics.emptyIfNull(description);
    }

    /**
     * @param newDescription the description to set
     */
    @Override
    public void setDescription(String newDescription) {
        this.description = ViskitStatics.emptyIfNull(newDescription);
    }
}