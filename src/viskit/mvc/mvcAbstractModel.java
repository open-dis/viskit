package viskit.mvc;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM) 2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey CA
 * www.nps.navy.mil
 * By:   Mike Bailey
 * Date: Mar 2, 2004
 * Time: 11:04:25 AM
 */
/**
 * Abstract root class of the model hierarchy.  Provides basic notification behavior.
 */
/**
 * From an article at www.jaydeetechnology.co.uk
 */
public abstract class mvcAbstractModel implements mvcModel {

    private ArrayList<mvcModelListener> listeners = new ArrayList<mvcModelListener>(4);

    // We know this to return an ArrayList<mvcModelListener>
    @SuppressWarnings("unchecked")
    public void notifyChanged(mvcModelEvent event) {
        ArrayList<mvcModelListener> list = (ArrayList<mvcModelListener>) listeners.clone();
        for (mvcModelListener ml : list) {
            ml.modelChanged(event);
        }
    }

    public void addModelListener(mvcModelListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeModelListener(mvcModelListener l) {
        listeners.remove(l);
    }
}
