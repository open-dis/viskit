package viskit.mvc;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004
 * @since 11:30:36 AM
 * @version $Id$
 *
 * From an article at www.jaydeetechnology.co.uk
 */
public abstract class Mvc2AbstractController implements Mvc2Controller {

    private Mvc2View view;
    private Mvc2Model model;

    @Override
    public Mvc2Model getModel() {
        return model;
    }

    @Override
    public Mvc2View getView() {
        return view;
    }

    @Override
    public void setModel(Mvc2Model model) {
        this.model = model;
    }

    @Override
    public void setView(Mvc2View view) {
        this.view = view;
    }
    
    static interface RecentFileListener {
        void listChanged();
    }
}
