/*
 * Mover3D.java
 *
 * Created on October 6, 2004, 9:04 AM
 */

package examples.diskit;

import mil.navy.nps.math.Vec4f;
import mil.navy.nps.math.Vec3f;
import simkit.smdx.MovementState;

/**
 *
 * @author  Rick Goldberg
 */
public interface Mover3D {
    
    public Vec4f getVelocity(); // dx,dy,dz,dt
    public void setVelocity(Vec4f v); // most apps should not be using these.
    
    public float getCruisingSpeed();
    public void setCruisingSpeed(float s);
    
    public void setStartPosition(Vec3f sp); // start at xyz
    public void setStartPosition(Vec4f sp); // start at xyzt
    
    public void setNextWaypoint(Vec3f p, float cs);
    public void setNextWaypoint(Vec4f wp);
    // these two do basically the above two
    public void setDestination(Vec3f d, float cs); // get there this fast
    public void setDestination(Vec4f d); // meet at 7-11 at 10pm., v[3] is point in time
    public Vec4f getDestination();
    // gets the location from the currentPosition, 
    public Vec3f getLocation();
    public Vec4f getCurrentPosition();
    
    public MovementState getMovementState();
    
}
