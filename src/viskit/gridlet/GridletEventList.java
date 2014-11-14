/*
 * GridletEventList.java
 *
 * Created on March 9, 2007, 4:41 PM
 *
 * Simkit's verbose mode can only go to System.out, 
 * synchronizing on that would defeat a lot of benefit for multiprocessors. 
 * This isn't an issue in grid mode as those Gridlets run on a separate JVM. 
 * 
 * The GridletEventList overrides the defaultEventList in simkit.Schedule allowing
 * for selected ouput streams.
 *
 */

package viskit.gridlet;
import java.io.ByteArrayOutputStream;
import simkit.EventList;
import java.io.PrintWriter;

/**
 *
 * @author Rick Goldberg
 */
public class GridletEventList extends EventList {
    
    private PrintWriter printWriter;
    private ByteArrayOutputStream buffer;
    
    public GridletEventList(int id) {
        super(id);
        buffer = new ByteArrayOutputStream();
        printWriter = new PrintWriter(buffer);
    }
    
    public ByteArrayOutputStream getOutputBuffer() {
        return buffer;
    }
    
    public void dump(String reason) {
        printWriter.println(super.getEventListAsString(reason));
        System.out.println(getEventListAsString(reason));
    }
    
}
