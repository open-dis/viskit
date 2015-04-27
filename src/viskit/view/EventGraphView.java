package viskit.view;

import viskit.model.*;

import java.io.File;
import java.util.Collection;

/**
 * The MVC design of Viskit means that the Model and the EventGraphView know about the
 * chosen view only as much as is described by this interface.
 *
 * Created by IntelliJ IDEA.
 * @author Mike Bailey
 * @since Mar 18, 2004
 * @since 12:06:11 PM
 * @version $Id$
 */
public interface EventGraphView {

    /**
     * Permits user to edit existing edges
     *
     * @param edge the canceling edge to edit
     * @return successful or not
     */
    boolean doEditCancelEdge(Edge edge);

    /**
     * Permits user to edit existing edges
     *
     * @param edge the scheduling edge to edit
     * @return successful or not
     */
    boolean doEditEdge(Edge edge);

    /**
     * Permits user to edit existing event nodes
     *
     * @param node the event node to edit
     * @return successful or not
     */
    boolean doEditNode(EventNode node);

    boolean doEditParameter(vParameter param);

    boolean doEditStateVariable(vStateVariable var);

    /**
     * Question dialog
     *
     * @param title
     * @param prompt
     * @return yes, no or cancel constants
     */
    int genericAsk(String title, String prompt);

    int genericAskYN(String title, String msg);

    /**
     * A component, e.g., vMod, wants to say something.
     *
     * @param typ the type of message, i.e. WARN, ERROR, INFO, QUESTION
     * @param title the title of the message in the dialog frame
     * @param msg the message to transmit
     */
    void genericReport(int typ, String title, String msg);

    String promptForStringOrCancel(String title, String message, String initval);

    File[] openFilesAsk();

    /** NOTE: Not used
     * @param lis a list of recently open files
     * @return a recently opened file
     */
    File openRecentFilesAsk(Collection<String> lis);

    /** Saves the current Event Graph "as" desired by the user
     *
     * @param suggName the package and file name of the EG
     * @param showUniqueName show EG name only
     * @return a File object of the saved EG
     */
    File saveFileAsk(String suggName, boolean showUniqueName);

    /**
     * Update the name of the EventGraph in the component title bar
     *
     * @param name the name of the EventGraph
     */
    void setSelectedEventGraphName(String name);

    /**
     * @param description the description to set for the EventGraph
     */
    void setSelectedEventGraphDescription(String description);

    void addTab(Model mod); // When a tab is added

    void delTab(Model mod); // When a tab is removed

    Model[] getOpenModels();

    // The following 2 may be implemented by the view in some other way than an
    // official GUI Dialog

    /**
     * run the add parameter dialog
     * @return the String representation of this parameter
     */
    String addParameterDialog();

    /**
     * run the add state variable dialog
     * @return the String representation of this state variable
     */
    String addStateVariableDialog();

}
