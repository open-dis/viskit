/*
 * DISMover3DBase.java
 *
 * Created on September 16, 2004, 11:58 AM
 */

package examples.diskit;

import mil.navy.nps.math.Vec4f;
import simkit.SimEntityBase;

/**
 * mainly to pull out a position at a time
 * @author Rick Goldberg
 */
public class DISMover3DBase extends SimEntityBase {
    // these are required to harness an event graph; other base
    // classes have no way of knowing otherwise.
    // some are for internal diskit use from the harness, see getters
    // while the others are for external event graph useage
    protected Vec4f startPosition;
    protected Vec4f velocity;
    float maxSpeed;
    float duration;
    float tol;
    short[] id;
    float[] stp;
    float[] v;
    
    /**
     * Creates a new instance of DISMover3DBase, a linear 3D momver. 
     * @param start initial positiono
     * @param speedLimit mover's limit of speed
     * @param id DIS entity id for this mover
     * @param tol roundoff error
     */
    public DISMover3DBase(Vec4f start, float speedLimit, short[] id, float tol) {
        startPosition = new Vec4f(start);
        velocity = new Vec4f();
        maxSpeed = speedLimit;
        
        if (id.length < 1 ) {
            System.out.println("Setting dis-entity Id to (1,0,0)");
            this.id = new short[]{1,0,0};
        } else {
            this.id = new short[id.length];
            for ( int i = 0; i< id.length; i++ ) {
                this.id[i] = id[i];
            }
        }
        this.tol = tol;  // tol = .01 seems o.k.
        
        stp = new float[4];
        v = new float[4];
    }
    
    
    /**
     * gets the velocity
     * @return current velocity for the mover
     */    
    public Vec4f getVelocity() { return velocity; }
    
    /**
     * DIS entity id for the mover
     * @return this mover's id should be short[3]
     */    
    public short[] getId() { return id; }

    /**
     * current start position for this mover's move segment
     * @return x,y,z and time to be there
     */    
    public Vec4f getStartPosition() { return startPosition; }
    
  
}
