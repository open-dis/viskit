package viskit.test;

import java.util.Arrays;
import simkit.Schedule;
import simkit.SimEntityBase;

/**
 * This test shows that arguments for varargs only need to be cast to
 * (Object) if the variable is an non-primitive array.
 * 
 * <p>Output:
 * <pre>
Compiling 1 source file to C:\tmp\SimkitExamples\build\classes
C:\tmp\SimkitExamples\src\test\VarArgsTest.java:60: warning: non-varargs call of varargs method with inexact argument type for last parameter;
cast to java.lang.Object for a varargs call
cast to java.lang.Object[] for a non-varargs call and to suppress this warning
        waitDelay("Bar", 2.0, new String[] { "This", "is", "a", "warning!"});
1 warning
compile-single:
run-single:
** Event List 0 -- Starting Simulation **
0.000	Run	
 ** End of Event List -- Starting Simulation **

Time: 0.0000	CurrentEvent: Run [1]
** Event List 0 --  **
1.000	Foo 	{[Ljava.lang.String;@de6f34}	
2.000	Bar 	{This, is, a, warning!}	
3.000	Baz 	{3, [I@156ee8e, [Ljava.lang.String;@47b480}	
5.000	FooBar 	{[I@19b49e6}	
 ** End of Event List --  **

[Hi, Mom!]
Time: 1.0000	CurrentEvent: Foo 	{[Ljava.lang.String;@de6f34} [1]
** Event List 0 --  **
2.000	Bar 	{This, is, a, warning!}	
3.000	Baz 	{3, [I@156ee8e, [Ljava.lang.String;@47b480}	
5.000	FooBar 	{[I@19b49e6}	
 ** End of Event List --  **

Time: 2.0000	CurrentEvent: Bar 	{This, is, a, warning!} [1]
** Event List 0 --  **
3.000	Baz 	{3, [I@156ee8e, [Ljava.lang.String;@47b480}	
5.000	FooBar 	{[I@19b49e6}	
 ** End of Event List --  **

x = 3
y = [3, 4]
y = [Hi, Again!]
Time: 3.0000	CurrentEvent: Baz 	{3, [I@156ee8e, [Ljava.lang.String;@47b480} [1]
** Event List 0 --  **
5.000	FooBar 	{[I@19b49e6}	
 ** End of Event List --  **

[3, 4, -17, 2147483647]
Time: 5.0000	CurrentEvent: FooBar 	{[I@19b49e6} [1]
** Event List 0 --  **
            << empty >>
 ** End of Event List --  **
 * 
 * @version $Id$
 * @author abuss
 */
public class TestVarArgs extends SimEntityBase  {

    public void doRun() {
        waitDelay("Foo", 1.0, (Object) new String[] { "Hi", "Mom!" });
        waitDelay("Baz", 3.0, 1 + 2, (Object) new int[] { 3, 4 }, 
                (Object) new String[] { "Hi", "Again!" });
//        This next call triggers the warning.  Worse, the "right"
//        event is not scheduled.  Uncomment to test
//        waitDelay("Bar", 2.0, new String[] { "This", "is", "a", "warning!"});
        waitDelay("FooBar", 5.0, new int[] { 3, 4, -17, Integer.MAX_VALUE} );
    }
    
    public void doFoo(String[] param) {
        System.out.println(Arrays.toString(param));
    }
    
    public void doBar(String[] param) {
        System.out.println(Arrays.toString(param));
    }
    
    public void doBaz(int x, int[] y, String[] z) {
        System.out.println("x = " + x);
        System.out.println("y = " + Arrays.toString(y));
        System.out.println("y = " + Arrays.toString(z));
    }
    
    public void doFooBar(int[] param) {
        System.out.println(Arrays.toString(param));
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TestVarArgs varArgsTest = new TestVarArgs();
        
        Schedule.setVerbose(true);
        Schedule.reset();
        Schedule.startSimulation();
    }

}
