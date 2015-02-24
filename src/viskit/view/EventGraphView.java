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
     * Permit user to edit existing entities *
     *
     * @param edge the canceling edge to edit
     * @return successful or not
     */
    boolean doEditCancelEdge(CancelingEdge edge);

    boolean doEditEdge(SchedulingEdge edge);

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

    File openRecentFilesAsk(Collection<String> lis);

    /** Saves the current Event Graph "as" desired by the user
     *
     * @param suggName the package and file name of the EG
     * @param showUniqueName show EG name only
     * @return a File object of the saved EG
     */
    File saveFileAsk(String suggName, boolean showUniqueName);

    /**
     * Show the project name in the main frame title bar
     */
    void showProjectName();

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

    /**
     * Called by the controller after source has been generated. Show to the
     * user and provide him with the option to save.
     *
     * @param className the name of the source file to show
     * @param s Java source
     * @param filename the source file's name
     */
    void showAndSaveSource(String className, String s, String filename);

    /**
     * Shows the XML representation of this EG
     * @param f the EG file to display
     */
    void displayXML(File f);

    // The following 2 may be implemented by the view in some other way that an official GUI Dialog

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
