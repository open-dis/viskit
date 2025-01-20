package viskit.mvc;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract root class of the model hierarchy. Provides basic notification behavior.
 *
 * From an article at www.jaydeetechnology.co.uk
 *
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Mar 2, 2004 : 11:04:25 AM
 * @version $Id$
 */
public abstract class MvcAbstractModel implements MvcModel {

    private final List<MvcModelListener> listeners = new ArrayList<>(4);

    @Override
    public void notifyChanged(MvcModelEvent event) {
        for (MvcModelListener ml : listeners) {
            ml.modelChanged(event);
        }
    }

    public void addModelListener(MvcModelListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeModelListener(MvcModelListener l) {
        listeners.remove(l);
    }
}
