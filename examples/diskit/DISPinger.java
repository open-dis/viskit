package diskit;

import simkit.*;
import java.lang.Thread;

/**
 * DISPinger simply holds the main running thread for an interval, to simulate simulated time. Extensions call the <CODE>pause()</CODE> method, typically via a self-scheduled queue delay of the same virtual duration. Note the clock and the queue may be out of phase by a small number of milliseconds each pause(), and events that transpire upon termination of the self-scheduling may be as much as <i>interval - epsilon</i> out of phase with the real time clock.
 */
public class DISPinger extends SimEntityBase {

    long interval;

    /**
     * Create a DISPinger with initial interval.
     * @param duration in sim time
     * @param stms 1.0 sim time per stms milliseconds clock time. 
     */    
    public DISPinger(double duration, long stms) {
	super("DISPinger",SimEntityBase.DEFAULT_PRIORITY);
	setInterval(duration,stms);
    }

    /**
     *
     * hold the main thread steady for interval millis.
     * typically, application repeats this call until end of move
     * @return if everything went ok, that is it timed out, true
     */
    protected boolean pause() {
        boolean full = true;
	
        try {
	    Thread.sleep(interval);
	} 
        
        catch (InterruptedException ie) { full = true; }
        
        catch (Exception e) { full = false; } // uh-oh?
        
        return full;
    }
    
    private void setInterval(double duration, long stms) {
        this.interval = (long)(duration*(double)stms);
    }

}
