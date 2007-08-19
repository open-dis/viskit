/*
 * File:        ContainsFilenameFilter.java
 *
 * Created on:  August 17, 2007, 1852Z
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

package edu.nps.util;

// Standard library imports
import java.io.File;
import javax.swing.filechooser.FileFilter;

// Application specific imports
import org.apache.log4j.Logger;

/**
 * Filters files whose names contain a given String, case insensitive
 *
 * @version $Id$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     17 AUG 07
 *     Time:     1852Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.ContainsFilenameFilter">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial, Bug 1247 fix
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.ContainsFilenameFilter">Terry Norbraten</a>
 */
public final class ContainsFilenameFilter extends FileFilter {
    
    /** log4j logger instance */
    static Logger log = Logger.getLogger(ContainsFilenameFilter.class);
    
    /** we filter to accept only files starting with this string */
    private final String contains;
    
    /* CONSTRUCTOR(s) */
    
    /**
     * Creates an instance of ContainsFilenameFilter
     *
     * @param contains file must contain this String. Case Insensitive.
     */
    public ContainsFilenameFilter(String contains) {this.contains = contains.toLowerCase();}

    /**
     * Select only files containing with our String.  Does expose directories 
     * for ease of navigation
     *
     * @param f  the file for naming determination 
     *
     * @return true if and only if the name should be included in the file list;
     *         false otherwise.
     */
    public boolean accept(File f) {
        if (f.isDirectory()) {return true;}
        return f.getName().toLowerCase().contains(contains);        
    }
    
    /** @return a fileview description of the filter */
    public String getDescription() {return "Viskit Assembly XML Files Only";}

} // end class file ContainsFilenameFilter.java