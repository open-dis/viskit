/*
 * DISMover3DSender.java
 *
 * Created on September 7, 2004, 1:07 PM
 */

package diskit;


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
 * @author rmgold
 */
public class DISMover3DSender extends SimEntityBase {
    
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
    public DISMover3DSender(String inetAddr, int port) {
        
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
    protected int addMover3D(DISMover3DBase mover) {
        EntityStatePdu pdu = new EntityStatePdu();
        short[] ids = mover.getId();
        
        pdu.setEntityID(ids[0],ids[1],ids[2]);
        movers.add(mover);
        pdus.add(pdu);
        
        return movers.size();
    }
    
    
    // note: coordinate space is already DIS's
    /**
     * send network pings with time scaled positions and velocities
     * @return number of pings sent, should be one per mover.
     */    
    public int sendPings() {
        
        EntityStatePdu espdu;
        DISMover3DBase mover;
        float[] velocity = new float[4];
        float[] position = new float[4];
        float time = (float) Schedule.getSimTime();
        
        for ( int i = 0; i < movers.size(); i++ ) {
            
            espdu = (EntityStatePdu) pdus.get(i);
            mover = (DISMover3DBase) movers.get(i);
            
            // math here : see http://web.nps.navy.mil/~brutzman/vrtp/demo/auv/AuvPduGenerator.java
            
            (mover.getVelocity()).get(velocity);
            
            // note: Don says xzDistance is not properly named.
            
            double xzDistance = Math.sqrt(velocity[0]*velocity[0] + velocity[2]*velocity[2]);
            double pitch = Math.atan(velocity[1]/xzDistance);
            double direction = Math.atan2(velocity[2],velocity[0]);
        
            // math here: done by the mover3dbase
        
            espdu.setEntityLinearVelocityX((float)velocity[0]);
            espdu.setEntityLinearVelocityY((float)velocity[1]);
            espdu.setEntityLinearVelocityZ((float)velocity[2]);
            espdu.setEntityOrientationTheta((float)pitch);
            espdu.setEntityOrientationPsi((float)direction); 
        
            // positionAtTime(t) gives the theoretical but mostly exact position at any
            // time in particular; does not update the position of the mover.
            
            (mover.positionAtTime(time)).get(position);
            
            espdu.setEntityLocationX(position[0]);
            espdu.setEntityLocationY(position[1]);
            espdu.setEntityLocationZ(position[2]);
        
            bpUDP.write(espdu);
            System.out.println(espdu);
            System.out.println("(x,y,x,t) -> (" + position[0] + " " + position[1] + " " + position[2] + " " + position[3] + ")");
            System.out.println("(vx,vy,vz,vt) -> (" + velocity[0] + " " + velocity[1] + " " + velocity[2] + " " + velocity[3] + ")");

        }
        
        return movers.size();
    }
}
