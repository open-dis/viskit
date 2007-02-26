package seadiver;

import diskit.Mover3D;
import simkit.SimEntity;
import simkit.smdx.MovementState;

/**
 *
 * @author  ahbuss
 *
 * Modified to extend SimEntity on 1/10 by John Seguin
 */
public interface MoverManager extends SimEntity {
    
    public void setStartOnReset(boolean startOnReset);
    
    public boolean isStartOnReset();
    
    public void doStart();
    
    public void doStop();
    
    public void doEndMove(Mover3D mover);
    
    public void setMover(Mover3D mover);
    
    public Mover3D getMover();
    
    public MovementState getMovementState();
    
}
