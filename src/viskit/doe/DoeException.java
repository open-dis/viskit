/*
 * DoeException.java
 *
 * Created on January 9, 2007, 1:24 PM
 *
 */

package viskit.doe;

/**
 *
 * @author Rick Goldberg
 */
public class DoeException extends java.lang.Exception {
    
    /**
     * Creates a new instance of <code>DoeException</code> without detail message.
     */
    public DoeException() {
    }
    
    
    /**
     * Constructs an instance of <code>DoeException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DoeException(String msg) {
        super(msg);
    }
}
