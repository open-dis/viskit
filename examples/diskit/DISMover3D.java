/*
 * DISMover3DBase.java
 *
 * Created on September 16, 2004, 11:58 AM
 */

package examples.diskit;

import mil.navy.nps.math.Vec4f;
import mil.navy.nps.math.Vec3f;
import simkit.SimEntityBase;
import simkit.Schedule;
import simkit.smdx.MovementState;

/**
 * mainly to pull out a position at a time
 * DISMover3D uses a 4D convention on space and velocity
 * where p[3] -> moment in time
 * and   v[3] -> duration of move
 * this is nice since then you just have v -> p1 - p0
 * @author Rick Goldberg
 */
public class DISMover3D extends SimEntityBase implements DISEntity, Mover3D {
    // DISMover3D class uses a 4D convention on space and velocity
    // where p[3] -> moment in time
    // and   v[3] -> duration of move
    // this is nice since v[] -> p1 - p0
    protected Vec4f startPosition;
    protected Vec4f velocity;
    protected Vec4f destination;
    protected MovementState movementState;
    private Vec3f tp;
    float maxSpeed;
    float duration;
    float cruisingSpeed; // using the wayPoint + cruisingSpeed model
    short[] id;
    protected Vec4f currentPosition; // this gets used a lot, rather than create a new instance,
                                     // or try to get into a possibly uninstantiated vec, keep
                                     // a ref here, and reuse it to avoid lots of gc. Really the
                                     // only one using this would be the DIS sender which is a 
                                     // single thread.
    private Vec4f originalStart; // for reset()
    private float scale;
    private float dt;
    private double eps = 0.01; // change when converted to double precision vecs 
    
    /**
     * Creates a new instance of DISMover3DBase, a linear 3D momver. 
     * @param start initial positiono
     * @param speedLimit mover's limit of speed
     * @param id DIS entity id for this mover
     * @param tol roundoff error
     */
    public DISMover3D(Vec4f start, float speedLimit, short[] id) {
        startPosition = new Vec4f(start);
        originalStart = new Vec4f(start);
        startPosition.set(3,(float)Schedule.getSimTime());
        velocity = new Vec4f();
        destination = new Vec4f();
        maxSpeed = speedLimit;
        cruisingSpeed = 0.0f;
        tp = new Vec3f(); // temporary gc free 
        currentPosition = new Vec4f(); // now the sender knows simtime, may be useful for clock pdu
        movementState = MovementState.STOPPED;
        
        if (id.length < 1 ) {
            System.out.println("Setting dis-entity Id to (1,0,0)");
            this.id = new short[]{1,0,0};
        } else {
            this.id = new short[id.length];
            for ( int i = 0; i< id.length; i++ ) {
                this.id[i] = id[i];
            }
        }

    }
    
    
    /**
     * gets a copy of the velocity
     * @return snapshot of velocity for the mover
     */    
    public Vec4f getVelocity() { 
        return new Vec4f(velocity); 
    }
    
    /**
     * assumes Vec4f in 4D notation
     * @param v velocity vector with delta t
     */    
    public void setVelocity(Vec4f v) {
        velocity.set(v);
        setEndMove();
    }
    
    private void setEndMove() {
        setMovementState(MovementState.CRUISING);
        interrupt("EndMove",new Object[]{this});
        waitDelay("EndMove",velocity.get(3),new Object[]{this});
    }
        
    public void doEndMove(Mover3D mover) {
        if (mover == this) {
            stopHere();
            setMovementState(MovementState.PAUSED);
        }
    }
    
     public void stop() {
        stopHere();
        setMovementState(MovementState.STOPPED);
    }
     
   
    public void pause() {
        stopHere();
        setMovementState(MovementState.PAUSED);
    }
    
    public boolean isMoving() {
        // if checking movementState wasn't enough:
        if( Math.sqrt(Math.pow(velocity.get(0),2)+Math.pow(velocity.get(1),2)+Math.pow(velocity.get(2),2)) - eps > 0.0 )
            return true;
        else 
            return false;
    }
     
    // updates next StartPosition to whatever happens to be CurrentPosition
    protected void stopHere() {
        setStartPosition(getCurrentPosition());
        velocity.sub(velocity);
    }
    
    /**
     * set desired cruisingSpeed, gets compared to maxSpeed and clipped
     * @param speed desired cruising speed, checked against maxSpeed
     */    
    public void setCruisingSpeed(float speed) {
        float oldSpeed = cruisingSpeed;
        if ( speed > maxSpeed ) {
            cruisingSpeed = maxSpeed;
        } else {
            cruisingSpeed = speed;
        }
        firePropertyChange("cruisingSpeed",oldSpeed,cruisingSpeed);
    }
    
    /**
     * schedule an EndMove with velocity calculated
     * @param wp next way point
     * @param cruisingSpeed desired cruising speed, checked against maxSpeed
     */    
    public void setNextWaypoint(Vec3f wp, float cruisingSpeed) {        
        // no wp[3]
        tp.set(startPosition.get(0),startPosition.get(1),startPosition.get(2));
        tp.sub(wp);
        tp.negate();
        setCruisingSpeed(cruisingSpeed); // checks if over limit
        destination.set(wp.get(0),wp.get(1),wp.get(2),tp.length()/cruisingSpeed); // arrive time now correctly set
        velocity.set(tp.get(0),tp.get(1),tp.get(2),destination.get(3)-(float)Schedule.getSimTime());
        setEndMove(); // schedule the event
        setMovementState(MovementState.CRUISING);
        firePropertyChange("destination",null,destination);
        firePropertyChange("velocity",null,velocity);
        waitDelay("StartMove",0.0,new Object[]{this});
    }
    
    /**
     * schedule EndMove based on startPostition, nextWayPoint, maxSpeed, desired time of EndMove, sets velocity and destination
     * @param wp destination with desired end time, gets compared to maxSpeed and adjusted
     */    
    public void setNextWaypoint(Vec4f wp) {
        dt = wp.get(3) - (float)Schedule.getSimTime();
        if ( dt > 0.0f ) {
            destination.set(wp);
            // determine velocity from wp[3]
            tp.set(wp.get(0)-startPosition.get(0),wp.get(1)-startPosition.get(1),wp.get(2)-startPosition.get(2));
            setCruisingSpeed(tp.length()/dt);
            velocity.set(tp.get(0),tp.get(1),tp.get(2),dt);
            setEndMove(); // schedule the event
            setMovementState(MovementState.CRUISING);
            firePropertyChange("velocity",null,velocity);
            firePropertyChange("destination",null,startPosition);
            waitDelay("StartMove",0.0,new Object[]{this});
        }
    }
    
    public void setDestination(Vec4f d) {
        setNextWaypoint(d);
    }
    
    public void setDestination(Vec3f d, float s) {
        setNextWaypoint(d,s);
    }
    
    public Vec4f getDestination() {
        return new Vec4f(destination);
    }
    
    /**
     * DIS entity id for the mover in DISEntity
     * @return a copy of this mover's id should be short[3]
     */    
    public short[] getId() { 
        short[] rid = new short[id.length];
        System.arraycopy(id,0,rid,0,id.length);
        return rid; 
    }

    /**
     * current start position for this mover's move segment
     * @return x,y,z and time to be there
     */    
    public Vec4f getStartPosition() { 
        return new Vec4f(startPosition); 
    }
    
    public Vec3f getLocation() {
        Vec4f t = getCurrentPosition();
        return new Vec3f(t.get(0),t.get(1),t.get(2));
    }
    
    /**
     * sets startPostition of a move, which should be set at the beginning of a move, time component of position vector is set to current simtime, which would be at the start of a move.
     * @param sp startposition of a move, set usually at an EndMove event to the destination which is usually set by nextWayPoint.
     */    
    public void setStartPosition(Vec4f sp) {
        startPosition.set(sp);
        startPosition.set(3,(float)(Schedule.getSimTime()));
        firePropertyChange("startPosition",null,startPosition);
    }
    
    public void setStartPosition(Vec3f sp) {
        startPosition.set(0,sp.get(0));
        startPosition.set(1,sp.get(1));
        startPosition.set(2,sp.get(2));
        startPosition.set(3,(float)Schedule.getSimTime());
        firePropertyChange("startPosition",null,startPosition);
    }
    
    /**
     * get the current location at the current simtime. there is no setCurrentLocation. returns an mt unsafe handle to a gc friendly vector
     * @return a handle to a calculated Vec4f, product of state, that gets reused, so do something with the values promptly.
     */    
    public Vec4f getCurrentPosition() { 
        tp.set(velocity.get(0),velocity.get(1),velocity.get(2)); 
        dt = (float)Schedule.getSimTime() - startPosition.get(3);
        scale = tp.length()*(((float)Schedule.getSimTime()-startPosition.get(3))/velocity.get(3));
        currentPosition.set(startPosition.get(0) + tp.get(0)*scale, 
                startPosition.get(1) + tp.get(1)*scale, 
                startPosition.get(2) + tp.get(2)*scale, 
                (float)Schedule.getSimTime());
        
        return currentPosition;
    }
    
    // right from UniformLinearMover
    protected void setMovementState(MovementState state) {
        MovementState oldState = getMovementState();
        movementState = state;
        firePropertyChange("movementState", oldState, movementState);
    }
    
    
    public MovementState getMovementState() {
        return movementState;
    }
    
    public void reset() {
        super.reset();
        setMovementState(MovementState.STOPPED);
        startPosition = new Vec4f(originalStart);
        startPosition.set(3,(float)Schedule.getSimTime());
        destination = new Vec4f();
      
    }
    
    public float getCruisingSpeed() {
        return cruisingSpeed;
    }
    
}
