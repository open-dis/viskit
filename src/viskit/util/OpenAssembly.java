/*
Copyright (c) 1995-2024 held by the author(s).  All rights reserved.

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
      Modeling, Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and https://my.nps.edu/web/moves)
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.SimkitAssembly;

/**
 * This is a singleton class to coordinate opening of and modifications to
 * Assembly files.
 * <p>
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu</p>
 * @author Mike Bailey
 * @since Dec 1, 2005
 */
public class OpenAssembly
{
    static final Logger LOG = LogManager.getLogger();

    private static OpenAssembly openAssemblyInstance;
    private static final Object SYNCHER = new Object();
    private static String name = new String();

    public static OpenAssembly instance() {
        if (openAssemblyInstance != null) {
            return openAssemblyInstance;
        }

        synchronized (SYNCHER) {
            if (openAssemblyInstance == null) {
                openAssemblyInstance = new OpenAssembly();
            }
            return openAssemblyInstance;
        }
    }

    public File file;
    public SimkitAssembly jaxbRootSimkitAssembly;
    public ObjectFactory jaxbAssemblyObjectFactory;

    /** Singleton class constructor */
    private OpenAssembly() 
    {
        // empty constructor
    }

    /** @param newFile the Assembly XML file to announce to all the Assembly Listeners
     * @param jaxbSimkitAssembly the JAXB root of this XML file
     */
    public void setFile(File newFile, SimkitAssembly jaxbSimkitAssembly) 
    {
        if (newFile != null) {
            this.file = newFile;
            jaxbRootSimkitAssembly = jaxbSimkitAssembly;
            jaxbAssemblyObjectFactory = new ObjectFactory();

            // This is crucial for Viskit being able to open associated Event Graphs for
            // this Assembly
            EventGraphCache.instance().makeEntityTable(newFile);
            doFireActionNewAssembly(newFile);
            
            // this works but TODO unnecessary?  can also see graphMetadata
            name = newFile.getName();
            if (name.toLowerCase().endsWith(".xml"))
                name = name.substring(0, name.indexOf(".xml"));
        }
    }
    private final Set<AssemblyChangeListener> assemblyChangeListenerSet = new HashSet<>();

    /**
     * @param assemblyChangeListener assembly change listener
     * @return true if was not already registered
     */
    public boolean addListener(AssemblyChangeListener assemblyChangeListener) {
        return assemblyChangeListenerSet.add(assemblyChangeListener);
    }

    /**
     * @param assemblyChangeListener assembly change listener
     * @return true if it had been registered
     */
    public boolean removeListener(AssemblyChangeListener assemblyChangeListener) {
        return assemblyChangeListenerSet.remove(assemblyChangeListener);
    }

    public void doParamLocallyEdited(AssemblyChangeListener sourceAssemblyChangeListener) {
        fireAction(AssemblyChangeListener.PARAMETER_LOCALLY_EDITED, sourceAssemblyChangeListener, null);
    }

    public void doFireActionAssemblyJaxbChanged(AssemblyChangeListener sourceAssemblyChangeListener) {
        fireAction(AssemblyChangeListener.JAXB_CHANGED, sourceAssemblyChangeListener, null);
    }

    public void doFireActionNewAssembly(File file) {
        fireAction(AssemblyChangeListener.NEW_ASSEMBLY, null, file);
    }

    public void doFireActionCloseAssembly() {
        fireAction(AssemblyChangeListener.CLOSE_ASSEMBLY, null, null);
    }

    private void fireAction(int assemblyChangeListenerAction, AssemblyChangeListener sourceAssemblyChangeListener, Object parameterObject) 
    {
        for (AssemblyChangeListener assemblyChangeListener : assemblyChangeListenerSet) {
            if (assemblyChangeListener != sourceAssemblyChangeListener) {
                assemblyChangeListener.assemblyChanged(assemblyChangeListenerAction, sourceAssemblyChangeListener, parameterObject);
            }
        }
    }

    static public interface AssemblyChangeListener {

        // public final static int JDOM_CHANGED = 0;
        int JAXB_CHANGED = 1;
        int NEW_ASSEMBLY = 2;
        int CLOSE_ASSEMBLY = 3;
        int PARAMETER_LOCALLY_EDITED = 4;

        /**
         * Notify the assembly listeners of a change
         * @param action the change taking place
         * @param sourceAssemblyChangeListener the AssemblyChangeListener
         * @param parameterObject the object that changes
         */
        void assemblyChanged(int action, AssemblyChangeListener sourceAssemblyChangeListener, Object parameterObject);

        /** @return the handle for this Assembly ChangeListener */
        String getHandle();
    }

    /**
     * @return the name
     */
    public static String getName() {
        return name;
    }

    /**
     * @param newName the name to set
     */
    public static void setName(String newName) {
        name = newName;
    }
}