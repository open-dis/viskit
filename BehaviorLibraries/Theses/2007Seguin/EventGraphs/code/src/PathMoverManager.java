package seadiver;

//import diskit.*;
import diskit.Mover3D;
import diskit.Vec3d;
import simkit.SimEntityBase;
import simkit.smdx.MovementState;

/**
 * Modified from original file in diskit to allow for continuing moving from
 * first waypoint when last waypoint reached.
 *
 *
 * @author  ahbuss
 */

public class PathMoverManager extends SimEntityBase implements MoverManager {

    private boolean startOnReset;
    private Mover3D mover;

    protected Vec3d[] waypoints;

    protected int nextWaypointIndex;

    private boolean resetWaypoints;

    public PathMoverManager(Mover3D mover, Vec3d[] waypoints, boolean startOnReset, boolean resetWaypoints) {
         super();
         setMover(mover);
         setWaypoints(waypoints);
         setStartOnReset(startOnReset);
         setResetWaypoints(resetWaypoints);
    }

       public void reset() {
         super.reset();
         nextWaypointIndex = 0;
      }

       public void doRun() {
         if (isStartOnReset() && mover != null) {
            waitDelay("Start", 0.0);
         }
      }

       public void doEndMove(Mover3D mover) {
         if (mover == getMover()) {
            nextWaypointIndex = nextWaypointIndex + 1;
            if (nextWaypointIndex < waypoints.length) {
               mover.setStartPosition(mover.getDestination());
               mover.setDestination(waypoints[nextWaypointIndex]);
               //System.out.println("Normal Progression: "+ waypoints[nextWaypointIndex]+ " from: "+ mover.getDestination()+ " for: "+ mover.getMoverID());
            }
//            else if(mover.getDestination() != mover.getLocation()){
//               mover.setStartPosition(mover.getLocation());
//               mover.setDestination(mover.getDestination());
//               System.out.println("Dest!=Loc Progression: setDest "+ mover.getDestination() + "  setStart "+ mover.getLocation()+ " for: "+ mover.getMoverID());
//            }
            else {
                if(resetWaypoints==false){
                    waitDelay("Stop", 0.0);
                    //System.out.println("Stop Line Progression: nextWaypointIndex is: "+ nextWaypointIndex + "  Waypoint.length is: "+ waypoints.length);
                }
                else {
                    mover.setStartPosition(mover.getDestination());
                    mover.setDestination(waypoints[0]);
                    //System.out.println("Modified Progression: setStart to: "+ mover.getDestination() + "  setDest to: "+ waypoints[0]+ " for: "+ mover.getMoverID());
                    nextWaypointIndex = 0;
                }
            }

         }
      }

       public void doStart() {
         if (nextWaypointIndex < waypoints.length) {
            mover.setDestination(waypoints[nextWaypointIndex]);
         }
      }

       public void doStop() {
         mover.stop();
      }

       public boolean isStartOnReset() {
         return startOnReset;
      }

       public void setStartOnReset(boolean startOnReset) {
         this.startOnReset = startOnReset;
      }

       public Mover3D getMover() {
         return mover;
      }

       public void setMover(Mover3D mover) {
         if (this.mover != null) {
            this.mover.removeSimEventListener(this);
         }
         this.mover = mover;
         if (mover != null) {
            mover.addSimEventListener(this);
         }
      }

       public MovementState getMovementState() {
         return mover.getMovementState();
      }

       public void setWaypoints(Vec3d[] wp) {
         this.waypoints = new Vec3d[wp.length];
         for (int i = 0; i < waypoints.length; ++i) {
            waypoints[i] = new Vec3d(wp[i]);
         }
         nextWaypointIndex = 0;
      }

       public void setWaypoints(Vec3d point, int index) {
         waypoints[index] = new Vec3d(point);
      }

       public Vec3d[] getWaypoints() {
         Vec3d[] copy = new Vec3d[waypoints.length];
         for (int i = 0; i < waypoints.length; ++i) {
            copy[i] = new Vec3d(waypoints[i]);
         }
         return copy;
      }

       public Vec3d getWaypoints(int index) {
         return waypoints[index];
      }

       public int numberWaypoints() {
         return waypoints.length;
      }

       public Vec3d getLastWaypoint() {
         return waypoints[waypoints.length-1];
      }

       public String toString() {
         String s = new String("MoverManager ");

         for (int i = 0; i<waypoints.length; i++)
            s += '\n' + waypoints[i].toString();
         return s;

      }

    private void setResetWaypoints(boolean resetWPs) {
        this.resetWaypoints = resetWPs;
    }
   }
