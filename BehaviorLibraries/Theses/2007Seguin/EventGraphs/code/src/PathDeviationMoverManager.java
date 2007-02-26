package seadiver;

import diskit.Mover3D;
import diskit.Vec3d;
import simkit.*;
//import diskit.*;
//import diskit.util.Math3D;
/**
 * A mover manager that gets inserted if a GPS fix is needed.
 * The mover will stop and obtain a fix with a passed delay time then return to the
 * MoverMangager in use before the detection.
 * @author Rick Goldberg, John Seguin (modified AvoidanceMoverManager)
 */
public class PathDeviationMoverManager extends SimEntityBase implements MoverManager  {


    private Mover3D mover;

    /**
     * True if fix needed.
     */
    protected boolean fixNeeded = false;
    /**
     * Final destination
     */
    protected Vec3d finalDest = new Vec3d();
    /**
     * MoverManager in effect prior.
     */
    protected MoverManager priorMoverManager;
    /**
     * Location of fix (current location).
     */
    protected Vec3d fixLocation = new Vec3d();

    protected double fixDelayTime;

    /**
     * Initializes the MoverManager with the prior MoverManager to swap back in
     * upon completion of the move.
     * @param mover The prior mover in use that would have moved to next waypoint.
     */
    public PathDeviationMoverManager(Mover3D mover, double fixDelayTime) {
        setMover(mover);
        setFixDelayTime(fixDelayTime);
    }

    /**
     * super resets() and clears the avoiding bit.
     */
    public void reset() {
        super.reset();
        fixNeeded = false;
    }

    /**
     * Fires avoiding propertyChange.
     */
    public void doRun() {
        firePropertyChange("fixNeeded", isObtainingFix());
    }

    /**
     * As EndMove event is heard, send EndAvoiding event.
     * @param mover Mover that should have sent EndMove event.
     */
    public void doEndMove(Mover3D mover) {
        if (mover == getMover()) {
            fixNeeded = false;
            firePropertyChange("fixNeeded", Boolean.TRUE, Boolean.FALSE);
            mover.waitDelay("EndFix", fixDelayTime, new Object[] { this, mover });
        }
    }

    /**
     * At the end of fix, reconnect the prior MoverManager and set the final
     * destination to the intended one prior to fix.
     * @param m Probably this MoverManager.
     * @param mover The mover to manage.
     */
    public void doEndFix(MoverManager m, Mover3D mover) {
        if ( m == this) {
            priorMoverManager.setMover(mover);
            mover.setDestination(finalDest);
            setMover(null);
        }
    }

    /**
     * Starts, however empty.
     */
    public void doStart() {

    }

    /**
     * Stop event heard is passed to the Mover.
     */
    public void doStop() {
        mover.stop();
    }


    /**
     * Initiates the MoverManager swap, fires the fix property, and adjusts
     * the Mover's destination to current loaction.
     * @param activeMoverManager previously connected MoverManager
     */
    public void obtainFix(MoverManager activeMoverManager) {

        priorMoverManager = activeMoverManager;
        priorMoverManager.setMover(null);
        finalDest = mover.getDestination();
        mover.setDestination(mover.getLocation());
    }

    /**
     * Gets MovementState
     * @return the current MovementState
     */
    public simkit.smdx.MovementState getMovementState() {
        return mover.getMovementState();
    }

    /**
     * Gets the Mover being MoverManaged.
     * @return The managed mover.
     */
    public Mover3D getMover() {
        return mover;
    }

    /**
     * Always false.
     * @return Always false.
     */
    public boolean isStartOnReset() {
        return false;
    }

    /**
     * Disconnects any prior Mover's SimEventListenerConnection and connects to
     * the newMover.
     * @param newMover The new Mover to connect.
     */
    public void setMover(Mover3D newMover) {
        if (mover != null) {
            mover.removeSimEventListener(this);
        }
        this.mover = newMover;
        if (mover != null) {
            mover.addSimEventListener(this);
        }
    }

    /**
     * Unused.
     * @param startOnReset Unused, startOnReset always false.
     */
    public void setStartOnReset(boolean startOnReset) {
    }

    /**
     * Property to see if this MoverManager is currently active.
     * @return the fix bit.
     */
    public boolean isObtainingFix() { return fixNeeded; }

    /**
     * reports whether or where it is obtaining fix.
     * @return some text
     */
    public String toString() {
        if (fixNeeded) {
            return new String("PathDeviationMoverManager fix position "+ fixLocation.toString());
        } else {
            return new String("PathDeviationMoverManager: not obtaining fix.");
        }
    }

    private void setFixDelayTime(double fixDT) {
        fixDelayTime = fixDT;
    }
}
