/*
 * File:        EventGraphFileFilter.java
 *
 * Created on:  December 17, 2007, 0413Z
 *
 * Refenences:  This code adapted from Roedy Green's FileFilter suite at:
 *              <a href="http://mindprod.com/products1.html#FILTER">http://mindprod.com/products1.html#FILTER</a>
 *
 * Assumptions: Just give it the String to parse and it should filter based on
 *              that String
 */

/*
Copyright (c) 1995-2007 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package viskit.util;

// Standard library imports
import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 * Filters files whose names contain a given String, case insensitive
 *
 * @version $Id$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     17 DEC 07
 *     Time:     0130Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphFileFilter">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial, Bug 1247 fix
 *
 *     Date:     17 DEC 07
 *     Time:     1918Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphFileFilter">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Better filename filtering
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.EventGraphFileFilter">Terry Norbraten</a>
 */
public final class EventGraphFileFilter extends FileFilter {

    /** we filter to accept only files starting with the contents of this [] */
    private final String[] contents;

    /* CONSTRUCTOR(s) */

    /**
     * Creates an instance of EventGraphFileFilter
     *
     * @param contents file must not contain any of these Strings.
     *        Case Insensitive.
     */
    public EventGraphFileFilter(String[] contents) {
        this.contents = contents;
        int ix = 0;
        for (String s : contents) {
            contents[ix] = s.toLowerCase();
            ix++;
        }
    }

    /**
     * Select only files not containing our String.  Does expose directories
     * for ease of navigation
     *
     * @param f the file for naming determination
     *
     * @return false if and only if the name should be not included in the file
     *         list; true otherwise.
     */
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {return true;}
        boolean retVal = false;
        String fileName = f.getName();
        for (String s : contents) {
            retVal = !fileName.toLowerCase().contains(s);
            if (!retVal) {
                break;
            }
        }
        return retVal;
    }

    /** @return a fileview description of the filter */
    @Override
    public String getDescription() {return "EventGraph XML Files Only";}

} // end class file EventGraphFileFilter.java