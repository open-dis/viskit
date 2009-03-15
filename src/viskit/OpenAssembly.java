/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

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
package viskit;


import java.io.File;
import java.util.HashSet;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.SimkitAssembly;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Dec 1, 2005
 * @since 11:18:53 AM
 *
 * This is a singleton class to coordinate opening of Assembly files
 */
public class OpenAssembly {

    private static OpenAssembly instance;
    private static final Object SYNCHER = new Object();

    public static OpenAssembly inst() {
        if (instance != null) {
            return instance;
        }

        synchronized (SYNCHER) {
            if (instance == null) {
                instance = new OpenAssembly();
            }
            return instance;
        }
    }
    
    public File file;
    public Document jdomDoc;
    public SimkitAssembly jaxbRoot;
    public ObjectFactory jaxbFactory;

    /** Singleton class */
    private OpenAssembly() {}

    /** @param f the Assembly XML file to announce to all the Assy Listeners
     * @param jaxb the JAXB root of this XML file
     * @throws Exception if the Assembly file is null
     */
    public void setFile(File f, SimkitAssembly jaxb) throws Exception {
        if (f != null) {
            file = f;
            jaxbRoot = jaxb;
            jaxbFactory = new ObjectFactory();

            SAXBuilder builder;
            try {
                builder = new SAXBuilder();
                jdomDoc = builder.build(f);
            } catch (Exception e) {
                jdomDoc = null;
                throw new Exception("Error parsing or finding XML file " + f.getAbsolutePath());
            }

            doSendNewAssy(f);
        }
    }
    private HashSet<AssyChangeListener> listeners = new HashSet<AssyChangeListener>();

    /**
     * @param lis
     * @return true if was not already registered
     */
    public boolean addListener(AssyChangeListener lis) {
        return listeners.add(lis);
    }

    /**
     * @param lis
     * @return true if it had been registered
     */
    public boolean removeListener(AssyChangeListener lis) {
        return listeners.remove(lis);
    }

    public void doParamLocallyEditted(AssyChangeListener source) {
        fireAction(AssyChangeListener.PARAM_LOCALLY_EDITTED, source, null);
    }
   
    public void doSendAssyJaxbChanged(AssyChangeListener source) {
        fireAction(AssyChangeListener.JAXB_CHANGED, source, null);
    }

    public void doSendNewAssy(File f) {
        fireAction(AssyChangeListener.NEW_ASSY, null, f);
    }

    public void doSendCloseAssy() {
        fireAction(AssyChangeListener.CLOSE_ASSY, null, null);
    }

    private void fireAction(int action, AssyChangeListener source, Object param) {
        for (AssyChangeListener lis : listeners) {
            if (lis != source) {
                lis.assyChanged(action, source, param);
            }
        }
    }

    static public interface AssyChangeListener {
        // public final static int JDOM_CHANGED = 0;
        public final static int JAXB_CHANGED = 1;
        public final static int NEW_ASSY = 2;
        public final static int CLOSE_ASSY = 3;
        public final static int PARAM_LOCALLY_EDITTED = 4;

        /**
         * Notify the assembly listeners of a change
         * @param action the change taking place
         * @param source the AssyChangeListener
         * @param param the object that changes
         */
        public void assyChanged(int action, AssyChangeListener source, Object param);

        public String getHandle();
    }
}