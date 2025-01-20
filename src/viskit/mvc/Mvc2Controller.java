package viskit.mvc;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 10:55:20 AM
 */

/**
 * From an article at www.jaydeetechnology.co.uk
 */

/**
 * Primary role of controller is to determine what should happen in response to user input.
 */
public interface Mvc2Controller
{
    /** Set the model for this controller
     *
     * @param model the model for this controller
     */
    void setModel(Mvc2Model model);

    /** Set the view for this controller
     *
     * @param view the view for this controller
     */
    void setView (Mvc2View  view);

    /** Retrieve the model of this mvc
     *
     * @return the model of this mvc
     */
    Mvc2Model getModel();

    /** Retrieve the view of this mvc
     *
     * @return the view of this mvc
     */
    Mvc2View  getView();
}
