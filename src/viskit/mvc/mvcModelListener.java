package viskit.mvc;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 11:29:33 AM
 * @version $Id$
 *
 * From an article at www.jaydeetechnology.co.uk
 */
public interface mvcModelListener {

    /**
     * This is where the "master" model (simkit.viskit.model.Model) updates the
     * view. Not so much to do in this editor as in EventGraphViewFrame
     *
     * @param event the instance when our model has changed
     */
    void modelChanged(mvcModelEvent event);
}