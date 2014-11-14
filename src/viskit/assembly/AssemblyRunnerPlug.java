package viskit.assembly;

/**
 * Run an assembly in a separate JVM
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.assembly.AssemblyRunnerPlug">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public interface AssemblyRunnerPlug {

    /**
     * Execute and assembly with the give args
     * @param execStrings the args to supply for execution
     */
    void exec(String[] execStrings);

} // end class file AssemblyRunnerPlug.java
