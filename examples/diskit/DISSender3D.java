/*
 * DISMover3DSender.java
 *
 * Created on September 7, 2004, 1:07 PM
 */

package examples.diskit;


import mil.navy.nps.math.Vec4f;
import mil.navy.nps.dis.EntityStatePdu;
import mil.navy.nps.net.BehaviorProducerUDP;
import simkit.SimEntityBase;
import simkit.Schedule;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * a multicast socket manger for 3D entities
 * @author Rick Goldberg
 */
public class DISSender3D extends SimEntityBase {
    
    BehaviorProducerUDP bpUDP;
    EntityStatePdu espdu;
    InetAddress address;
    MulticastSocket socket;
    ArrayList pdus;
    ArrayList movers;
    
    /**
     * Creates a new instance of DISSender
     * @param inetAddr the multicast group you would like to broadcast to
     * @param port port of the server
     */
    public DISSender3D(String inetAddr, int port) {
        
        try {
            socket = new MulticastSocket(port);
            address = InetAddress.getByName(inetAddr);
        } catch (java.net.UnknownHostException uhe) {
            System.out.println("Unknown host " + inetAddr);
        } catch (java.io.IOException ioe) {
            System.out.println("Can't create MulticastSocket to port " + port);
        }
        
        try {
            socket.joinGroup(address);
        } catch (java.io.IOException e) { 
            System.out.println("Unable to join socket");
            e.printStackTrace();
        }
        
        bpUDP = new BehaviorProducerUDP(socket);
        bpUDP.setDefaultDestination(address,new Integer(port));
        
        movers = new ArrayList();
        pdus = new ArrayList();
    }
    
    /**
     * used by the subclass harness to add a newly initialized DISMover3DBase, thereby avoiding array initializers from within the Assembly.
     * @param mover mover from EdgeParameter
     * @return number of movers added; a workaround for code insertions, via LocalVariable initializers that can call more or less arbitrary expressions including calling a superclass method with any state. Since state could be declared by the harness, there is no way for the superclass to know about it otherwise.
     */    
    protected int addMover3D(DISMover3D mover) {
        EntityStatePdu pdu = new EntityStatePdu();
        short[] ids = mover.getId();
        
        pdu.setEntityID(ids[0],ids[1],ids[2]);
        movers.add(mover);
        pdus.add(pdu);
        
        return movers.size();
    }
    
    protected boolean removeMover3D(DISMover3D mover) {
        pdus.remove(movers.indexOf(mover));
        return movers.remove(mover);
    }
    
    // note: coordinate space is already DIS's
    /**
     * send network pings with time scaled positions and velocities
     * @return number of pings sent, should be one per mover.
     */    
    public int sendPings() {
        
        EntityStatePdu espdu;
        DISMover3DBase mover;
        Vec4f position;
        Vec4f velocity;
        Vec4f startPosition;
        
        float time = (float) Schedule.getSimTime();
        
        for ( int i = 0; i < movers.size(); i++ ) {
            
            espdu = (EntityStatePdu) pdus.get(i);
            mover = (DISMover3DBase) movers.get(i);
            
            // math here : see http://web.nps.navy.mil/~brutzman/vrtp/demo/auv/AuvPduGenerator.java
            
            velocity = mover.getVelocity();
            startPosition = mover.getStartPosition();
            
            // note: Don says xzDistance is not properly named.
            
            double xzDistance = Math.sqrt(velocity.get(0)*velocity.get(0) + velocity.get(2)*velocity.get(2));
            double pitch = Math.atan(velocity.get(1)/xzDistance);
            double direction = Math.atan2(velocity.get(2),velocity.get(0));
        
            // math here: was mover3d
            
            System.out.println("start (" + startPosition.get(0) + " " + startPosition.get(1) + " " + startPosition.get(2) + " " + startPosition.get(3) + ")");
            System.out.println("velocity (" + velocity.get(0) + " " + velocity.get(1) + " " + velocity.get(2) + " " + velocity.get(3) + ")");
        
            position = mover.getCurrentPosition();
        
            espdu.setEntityLinearVelocityX(velocity.get(0));
            espdu.setEntityLinearVelocityY(velocity.get(1));
            espdu.setEntityLinearVelocityZ(velocity.get(2));
            espdu.setEntityOrientationTheta((float)pitch);
            espdu.setEntityOrientationPsi((float)direction); 
                        
            espdu.setEntityLocationX(position.get(0));
            espdu.setEntityLocationY(position.get(1));
            espdu.setEntityLocationZ(position.get(2));
        
            bpUDP.write(espdu);
            System.out.println(espdu);
            System.out.println("(x,y,x,t) -> (" + position.get(0) + " " + position.get(1) + " " + position.get(2) + " " + position.get(3) + ")");
            System.out.println("(vx,vy,vz,vt) -> (" + velocity.get(0) + " " + velocity.get(1) + " " + velocity.get(2) + " " + velocity.get(3) + ")");

        }
        
        return movers.size();
    }
    
    public void reset() {
        pdus.clear();
        movers.clear();
        super.reset();
    }
}
