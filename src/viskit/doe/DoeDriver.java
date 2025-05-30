/*
 * DoeDriver.java
 *
 * Created on January 8, 2007, 10:31 AM
 *
 * Viskit communicates to the Experiment operations, either
 * Local or Grid runs, via Drivers that implement this interface.
 * 
 */

package viskit.doe;

/**
 * Abstract interface for RemoteDriverImpl
 * @author Rick Goldberg
 *
 */
public interface DoeDriver extends DoeSessionDriver, DoeRunDriver {}