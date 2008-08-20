package viskit.util;

import simkit.BasicSimEntity;
import simkit.SimEvent;

/**
 * This class will dump the Event List whenever it hears any event.
 * 
 * @version $Id$
 * @author abuss
 */
public class VerboseListener extends BasicSimEntity {

    /**
     * A no-op because this will never have any cause to schedule an event.
     * @param simEvent The event that had been scheduled
     */
    @Override
    public void handleSimEvent(SimEvent simEvent) {
    }

    /**
     * Dump the Event List for any heard event
     * @param simEvent The event that is heard
     */
    @Override
    public void processSimEvent(SimEvent simEvent) {
        getEventList().dump(simEvent.getSource().getName());
    }

}
