/*
 * DISServer3DBase.java
 *
 * Created on September 22, 2004, 9:55 AM
 */

package diskit;

import java.net.MulticastSocket;
import java.net.InetAddress;
import mil.navy.nps.net.BehaviorProducerUDP;
import mil.navy.nps.dis.BehaviorConsumerIF;
import mil.navy.nps.dis.BehaviorProducerIF;
import mil.navy.nps.dis.ProtocolDataUnit;
import mil.navy.nps.dis.EntityStatePdu;
import simkit.SimEntityBase;

/**
 * Base "server" class which listens on a multicast address and port, creating a reading thread. Viskit will keep the class instance active even if the simulation is not running, so no need for a run/wait. Tries to correlate sim-real-time of packet arrival.
 * @author Rick Goldberg
 */
public class DISServer3DBase extends SimEntityBase implements BehaviorConsumerIF {
    
    MulticastSocket socket;
    InetAddress address;
    BehaviorProducerUDP bpUDP;
    Thread reader;
    long time;
    double millisecsToSimTime; // eg. 1000 ms => 1.0 sim time -> msst eq. .001
    
    /**
     * Creates a new instance of DISServer3DBase
     * @param inetAddr the multicast address
     * @param port port to listen to
     * @param msst milliseconds to sim-time correalation factor. Eg. .001 means 1.0 sim-time is 1000 milliseconds.
     */
    public DISServer3DBase(String inetAddr, int port, double msst) {
       
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
        
        millisecsToSimTime = msst;
        time = (new java.util.Date()).getTime();
        bpUDP = new BehaviorProducerUDP(socket);
        bpUDP.setDefaultDestination(address,new Integer(port));
        bpUDP.addListener(this);
        reader = new Thread(bpUDP);
        reader.start();
        
    }
    
    /**
     * implements consumer
     * @param pdu in pdu
     * @param producer from producer
     */    
    public void receivePdu(ProtocolDataUnit pdu, BehaviorProducerIF producer) {
        if ( pdu instanceof EntityStatePdu ) {
            long dt = (new java.util.Date()).getTime() - time; 
            EntityStatePdu espdu = (EntityStatePdu)pdu;
       
            // the main thread should be pinging along at a rate proportionate to
            // inverse of msst.  this is only to keep the clock alive for the expected
            // duration, mostly it does nothing except fill the queue with evenly
            // spaced demarkations and sleep, which should allow the following from
            // the reader thread.
            
            System.out.println("Got EntityStatePdu from " + producer + " at time " + (double)dt*millisecsToSimTime);
            System.out.println("ID: " + espdu.getEntityID());
            System.out.println("(x,y,z): ("+espdu.getEntityLocationX() + ", " + espdu.getEntityLocationY() + ", " + espdu.getEntityLocationZ() + ")" );
            System.out.println("(vx,vy,vz): ("+espdu.getEntityLinearVelocityX() + ", " + espdu.getEntityLinearVelocityY() + ", " + espdu.getEntityLinearVelocityZ() + ")" );
            System.out.println("Orientation theta: "+espdu.getEntityOrientationTheta());
            System.out.println("Orientation psi: "+espdu.getEntityOrientationPsi()); 
            //not really workable without a ping, or really needed.
            //waitDelay("ESPDU_in",(double)dt*millisecsToSimTime,new Object[]{espdu},0.0);
            
        } else {
            System.out.println("Recieved unknown Pdu " + pdu + " from " + producer );
        }
    }
    
    /**
     * implements consumer interface for pdu's
     * @param pdu in pdu
     * @param producer from producer
     * @param data with Object data
     */    
    public void receivePdu(ProtocolDataUnit pdu, BehaviorProducerIF producer, Object data) {
        receivePdu(pdu,producer);
        System.out.println("Also got some data: " + data );
    }
    
    /**
     * clear the time reference. In Viskit this would be when you hit the rewind button, useful for matching sender's data, if done at the right time can correlate closely to sender's sim-time.
     */    
    public void reset() {
        time = (new java.util.Date()).getTime();
        super.reset();
    }
    
}
