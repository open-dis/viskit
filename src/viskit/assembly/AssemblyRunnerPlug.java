package viskit.assembly;

/**
 * Internal access for the Assembly Runner
 * 
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.assembly.AssemblyRunnerPlug">Terry Norbraten, NPS MOVES</a>
 */
public interface AssemblyRunnerPlug {

    /**
     * Provide execution arguments for running an assembly
     * @param execStrings the args to supply for execution
     */
    void exec(String[] execStrings);
    
    /** Resets the runner upon project change */
    void resetRunner();

} // end class file AssemblyRunnerPlug.java
