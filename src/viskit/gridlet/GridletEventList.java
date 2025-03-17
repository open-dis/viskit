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

import edu.nps.util.Log4jUtilities;
import java.io.ByteArrayOutputStream;
import simkit.EventList;
import java.io.PrintWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Rick Goldberg
 */
public class GridletEventList extends EventList
{
    static final Logger LOG = LogManager.getLogger();
    
    private final PrintWriter printWriter;
    private final ByteArrayOutputStream buffer;
    
    public GridletEventList(int id) {
        super(id);
        buffer = new ByteArrayOutputStream();
        printWriter = new PrintWriter(buffer);
    }
    
    public ByteArrayOutputStream getOutputBuffer() {
        return buffer;
    }
    
    @Override
    public void dump(String reason) {
        printWriter.println(super.getEventListAsString(reason));
        LOG.info(getEventListAsString(reason));
    }
    
}
