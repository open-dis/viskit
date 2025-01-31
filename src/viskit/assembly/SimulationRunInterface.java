package viskit.assembly;

/**
 * Internal access for the Assembly Runner
 * 
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.assembly.SimulationRunInterface">Terry Norbraten, NPS MOVES</a>
 */
public interface SimulationRunInterface {

    /**
     * Provide execution arguments for running an assembly
     * @param execStrings the args to supply for execution
     */
    void exec(String[] execStrings);
    
    /** Resets the runner upon project change */
    void resetSimulationRunPanel();

} // end class file SimulationRunInterface.java
