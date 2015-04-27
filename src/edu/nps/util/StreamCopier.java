/* Program:     Visual Discrete Event Simulation (DES) Toolkit (Viskit)
 *
 * Author(s):   Elliotte Rusty Harold, Terry Norbraten
 *
 * Created on:  Unknown, 1999
 *
 * File:        StreamCopier.java
 *
 * Compiler:    JDK1.8
 * O/S:         Mac OS X Yosimite (10.10.3)
 *
 * Description: Class that copies data between two streams as quickly as
 *              possible.  The copy method reads from the input stream and
 *              writes onto the output stream until the input stream is
 *              exhausted.
 *
 * Information: Borrowed from Java I/O by Elliotte Rusty Harold, Copyright 1999
 *              O'Reilly & Associates, Inc.
 */
package edu.nps.util;

// Standard library imports
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class that copies data between two streams as quickly as possible.  The copy
 * method reads from the input stream and writes onto the output stream until
 * the input stream is exhausted.
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     June 23, 2005
 *     Time:     1055:27
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.StreamCopier">Terry D. Norbraten</a>
 *     Comments: Initial
 *   </b></pre>
 * </p>
 *
 * @author Elliotte Rusty Harold
 * @version $Id: StreamCopier.java 232 2011-12-26 20:44:41Z tdnorbra $
 */
public class StreamCopier {

    /** A 256 byte buffer is used to try to make the reads efficient
     * @param in the input stream to read
     * @param out the output stream to write to
     * @throws java.io.IOException if something goes wrong
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {

        /* Do not allow other threads to read from the input or write to the
         * output while copying is taking place.
         */
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[256];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

} // end class file StreamCopier.java
