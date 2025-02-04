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
public abstract class MvcAbstractController implements MvcController {

    private MvcView view;
    private MvcModel model;

    public MvcModel getModel() {
        return model;
    }

    @Override
    public MvcView getView() {
        return view;
    }

    @Override
    public void setModel(MvcModel model) {
        this.model = model;
    }

    @Override
    public void setView(MvcView view) {
        this.view = view;
    }
    
    static interface RecentFileListener {
        void listChanged();
    }
}
