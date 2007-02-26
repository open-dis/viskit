/*
 * DISMover3D.java
 *
 * Created on September 16, 2004, 11:58 AM
 *
 *Modified on 12/13/06 to include setter for TacticalMode.
 */
package seadiver;

import diskit.EntityType;
import diskit.ForceID;
import diskit.Mover3D;
import diskit.TacticalMode;
import diskit.Vec3d;
import diskit.Vec4d;
import simkit.SimEntityBase;
import simkit.Schedule;
import simkit.smdx.MovementState;
import diskit.util.Transform;

/**
 * DISMover3D base DIS enabled 3D position Mover3D
 * @author Rick Goldberg
 *
 *Modified on 12/13/06 to include setter for TacticalMode.
 *by John Seguin
 */
public class DISMover3D extends SimEntityBase implements Mover3D {

    /**
     * The initial position of the Mover3D, independent of any MoverManager. This
     * should be anything but the first waypoint to enable automated initial velocity
     * calculation.
     */
    protected Vec3d startPosition;
    /**
     * Velocity.
     */

    protected Vec3d velocity;
    /**
     * Current destination.
     */

    protected Vec3d destination;
    /**
     * Movement state.
     */

    protected MovementState movementState;

    protected TacticalMode tacticalMode;
    /**
     * Maximum speed this Mover3D can take on.
     */

    protected double maximumSpeed;
    /**
     * Duration of move between startPostion and destination may be calcualted.
     */

    protected double duration;
    /**
     * Current speed.
     */

    protected double cruiseSpeed; // using the wayPoint + cruisingSpeed model

    protected int  moverID;

    protected String color ="0 0 0";
    /**
     * First startPosition since instance.
     */

    protected Vec3d originalStart; // for reset()

    protected Transform transform;

    private double scale;
    private double dt;
    private double stms;
    private double eps = 0.00001;
    private Vec3d tp;
    private Vec3d lastVelocity;
    private EntityType type;
    private String name;

    //The forceID of this entity
    private static final int NOFORCEID = -1;
    private ForceID forceID = new diskit.ForceID(NOFORCEID);


    /**
     * A bounding box object for this mover which exposes SMAL dimensions to
     * this mover and those interacting with it
     */
    private diskit.util.BoundingBox boundingBox;
    /**
     * Creates a new instance of DISMover3D, a linear 3D momver.
     * @param start initial positiono
     * @param speedLimit mover's limit of speed
     * @param id DIS entity id for this mover
     */
    public DISMover3D(Vec3d start, double speedLimit, int id) {
        super();
        startPosition = new Vec3d(start);
        originalStart = new Vec3d(startPosition);
        velocity = new Vec3d();
        destination = new Vec3d(startPosition);
        maximumSpeed = speedLimit;
        cruiseSpeed = 0.0;
		  moverID = id;
        tp = new Vec3d(); // temporary gc free
        lastVelocity = new Vec3d();
        setMovementState(MovementState.STOPPED);
        this.setBoundingBox(new diskit.util.BoundingBox(this));
        transform = new Transform(new Vec4d(0.0,0.0,1.0,0.0),1.0,originalStart);
	 }

    /**
     * Sends Init event to ScenarioManger to register this Mover3D.
     */
    public void doRun() {
	waitDelay("Init",0.0,new Object[]{this});
        //System.out.println("Do Init on "+this);
    }

    public void reset() {
        super.reset();
        setStartPosition(originalStart);
        setDestination(startPosition);
        setCruiseSpeed(0.0f);
        setVelocity(calcVelocity(startPosition,destination,maximumSpeed*20));
        setMovementState(MovementState.STOPPED);
    }

    /**
     * gets a copy of the velocity
     * @return snapshot of velocity for the mover
     */
    public Vec3d getVelocity() {
        return new Vec3d(velocity);
    }


    /**
     * set the velocity
     * @param v no time component
     */

    public void setVelocity(Vec3d v) {
        lastVelocity.set(getVelocity());
        velocity.set(v);
        firePropertyChange("velocity",null,velocity);
    }

    public void setVelocity(Vec4d v) {
        lastVelocity.set(getVelocity());
        velocity.set(v.get(0),v.get(1),v.get(2));
        duration = v.get(3);
        firePropertyChange("velocity",null,velocity);
        firePropertyChange("duration",duration);
    }

    private void setEndMove() {
        if( duration == Double.POSITIVE_INFINITY || duration == Double.NEGATIVE_INFINITY || duration == Double.NaN ) {
            duration=0.0;
        }
        dt = Schedule.getSimTime();
        setMovementState(MovementState.CRUISING);
        interrupt("EndMove",new Object[]{this});
        waitDelay("EndMove",duration,new Object[]{this});
    }

    /**
     *
     * @param mover
     */
    public void doEndMove(Mover3D mover) {
        if (mover == this) {
            pause();
        }
		  String tm = mover.getTacticalMode().toString();
        waitDelay("RecordWaypoint", 0.0, new Object[]{new Double(Schedule.getSimTime()), new Vec3d(mover.getLocation()), new String(tm)});
        setStartPosition(destination);
    }
    public void doRecordWaypoint(int moverID, double timeStamp, Vec3d location, String state){
        /*This method does nothing in this class but is acted on by
         *the TrackHistoryRecorder class*/

    }
	  public void setColor(String clr){
	  	this.color = clr;
	  }

	  public String getColor(){
	  	return color;
	  }

     public void stop() {
        stopHere();
        setMovementState(MovementState.STOPPED);
    }


    public void pause() {
        stopHere();
        setMovementState(MovementState.PAUSED);

    }

    public void start() {
        waitDelay("StartMove",0.0,new Object[]{this});
    }
    public TacticalMode getTacticalMode(){
	 	return tacticalMode;
	 }
    /**
     *
     * @param mover
     */
    public void doStartMove(Mover3D mover) {
        setVelocity(calcVelocity(startPosition,destination,cruiseSpeed));
        if ( cruiseSpeed > 0.0 ) {
            duration = Vec3d.distance(startPosition,destination)/cruiseSpeed;
        } else {
            duration = Double.NaN;
        }


        setEndMove();
    }

    /**
     *
     *
     */
    public boolean isMoving() {
        if( Math.sqrt(Math.pow(velocity.get(0),2)+Math.pow(velocity.get(1),2)+Math.pow(velocity.get(2),2)) - eps > 0.0 )
            return true;
        else
            return false;
    }


    protected void stopHere() {
        setVelocity(new Vec3d(0.0,0.0,0.0));
    }

    /**
     * set desired cruisingSpeed, gets compared to maxSpeed and clipped
     * @param speed desired cruising speed, checked against maxSpeed
     */
    public void setCruiseSpeed(double speed) {
        double oldSpeed = cruiseSpeed;
        if ( speed > maximumSpeed ) {
            cruiseSpeed = maximumSpeed;
        } else {
            cruiseSpeed = speed;
        }
        firePropertyChange("cruisingSpeed",oldSpeed,cruiseSpeed);
    }

    /**
     * schedule an EndMove with velocity calculated
     * @param wp next way point
     * @param cs cruisingSpeed desired cruising speed, checked against maxSpeed
     */
    public void setNextWaypoint(Vec3d wp, double cs) {
        if( getLocation().get(0) == Double.NaN )
            tp.set(getDestination());
        else
            tp.set(getLocation());
        tp.sub(wp);
        tp.negate();
        setCruiseSpeed(cs); // checks if over limit
        if ( cruiseSpeed > 0.0 ) {
            double oldDuration=duration;
            lastVelocity.set(getVelocity());
            destination.set(wp);
            velocity.set(tp);
            duration = velocity.length()/cruiseSpeed;
            velocity.scale(1.0/duration);
            setEndMove(); // schedule the event, set movementState
            firePropertyChange("destination",null,destination);
            firePropertyChange("velocity",null,velocity);
            firePropertyChange("duration",oldDuration,duration);
            waitDelay("StartMove",0.0,new Object[]{this});

        }
    }

    /**
     *
     * @param s
     * @param d
     * @param cs
     *
     */
    protected Vec4d calcVelocity(Vec3d s, Vec3d d, double cs) {
        Vec3d v = new Vec3d(d);
        Vec4d r = new Vec4d();
        double dur = 0.0;
        v.sub(s);
        if ( v.length() != 0.0 && cs != 0.0) {
            dur = v.length()/cs;
            v.scale(1.0/dur);
        }
        r.set(v.get(0),v.get(1),v.get(2),dur);
        return r;
    }

    /**
     *
     * @param d
     * @param cs
     */
    public void setDestination(Vec3d d, double cs) {
        setNextWaypoint(d,cs);
    }

    /**
     *
     * @param d
     */
    public void setDestination(Vec3d d) {
        setNextWaypoint(d,cruiseSpeed);
    }

    /**
     *
     *
     */
    public Vec3d getDestination() {
        return new Vec3d(destination);
    }

    /**
     * current start position for this mover's move segment
     * @return x,y,z and time to be there
     */
    public Vec3d getStartPosition() {
        return new Vec3d(startPosition);
    }


    /**
     *
     *
     */
    public Vec3d getLocation() {
        tp.set(velocity);

        tp.scale((Schedule.getSimTime()-dt));

        tp.add(startPosition);

        return new Vec3d(tp);
    }


    /**
     *
     * @param sp
     */
    public void setStartPosition(Vec3d sp) {
        startPosition.set(sp);
        firePropertyChange("startPosition",null,startPosition);
    }

    /**
     *
     * @param state
     */
    protected void setMovementState(MovementState state) {
        MovementState oldState = getMovementState();
        movementState = state;
        firePropertyChange("movementState", oldState, movementState);
    }


    /**
     *
     *
     */
    public simkit.smdx.MovementState getMovementState() {
        return movementState;
    }


    /**
     *
     *
     */
    public double getCruiseSpeed() {
        return cruiseSpeed;
    }

    /**
     *
     *
     */
    public double getMaximumSpeed() {
        return maximumSpeed;
    }

    /**
     *
     * @param speed
     */
    public void setMaximumSpeed(double speed) {
        if (speed >= 0.0) {
            maximumSpeed = speed;
        } else {
            throw new IllegalArgumentException ("maxSpeed must be >= 0.0: " + speed);
        }
    }

    /**
     *
     *
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Get the DIS Entity ID.
     * @return The DIS Entity ID.
     */
    public int getMoverID() {
        return moverID;
    }

    /**
     * Set DIS Entity ID.
     * @param moverID DIS Entity ID.
     */
    public void setMoverID(int moverID) {
        this.moverID = moverID;
    }
    public void setEntityType(EntityType type){
 	this.type = type;
     }
    public String getEntityType(){
        String type = this.getClass().toString();
        type = type.substring(13, type.length());
        return type;
    }

     public void setEntityName(String name){
            this.name = name;
     }
     public String getEntityName(){
            return name;
     }
     public void setForceID(ForceID intID){
        this.forceID = intID;
     }
     public int getForceID(){
         return forceID.idValue();
     }

    /**
     *
     *
     */
    public Vec3d getOriginalStartPosition() {
        return new Vec3d(originalStart);
    }

    /**
     *
     * @param mover
     * @deprecated Should be moved down to subclasses that need respawning.
     */
    public void doRespawn(Mover3D mover) {
        if(mover==this) {
            System.out.println("RESPAWN: "+this);
            reset();
            waitDelay("Run",0.0);
        }
    }
       /**
     *
     */
    public diskit.SMAL.EntityDefinition getEntityDefinition(){
        return null;
        }

    /**
     *
     *
     */
    public String toString() {
        StringBuffer s = new StringBuffer("\n\tDISMover3D: Entity ID# "+getMoverID()+" at "+getLocation());
        s.append('\n');
        s.append('\t');
        s.append("startPosition "+startPosition);
        s.append('\n');
        s.append('\t');
        s.append("destination "+destination);
        s.append('\n');
        s.append('\t');
        s.append("velocity "+velocity+" cruisingSpeed "+cruiseSpeed);
        s.append('\n');
        s.append('\t');
        s.append('\n');
        return s.toString();
    }

    public diskit.util.BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(diskit.util.BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Transform getTransform() {

        // Z axis rotation
        double x = velocity.get(0);
        double y = velocity.get(1);
        double theta = 0.0;
        if ( x == 0 ) {
            if ( y > 0 ) {
                theta = Math.PI/2.0;
            } else if ( y < 0 ) {
                theta = - Math.PI/2.0;
            }
        } else {
            theta = Math.atan(y/x);
        }

        transform.setTranslation(getLocation());
        transform.setRotation(new Vec4d(0.0,0.0,1.0,theta));

        return transform;
    }

    public void setTacticalMode(diskit.TacticalMode mode) {
        tacticalMode = mode;
    }


}
