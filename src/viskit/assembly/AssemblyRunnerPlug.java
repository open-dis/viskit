package viskit.assembly;

/**
 * Handles a pre-run init for an assembly
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.assembly.AssemblyRunnerPlug">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public interface AssemblyRunnerPlug {

    /**
     * Execute an assembly with the given args
     * @param execStrings the args to supply for execution
     */
    void exec(String[] execStrings);

} // end class file AssemblyRunnerPlug.java
