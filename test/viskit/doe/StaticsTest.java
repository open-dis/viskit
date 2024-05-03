/*
Copyright (c) 1995-2023 held by the author(s).  All rights reserved.

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
      (https://www.nps.edu and https://my.nps.edu/web/moves)
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
package viskit.doe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import viskit.VGlobals;

/** Test of static variables isolated by unique class loaders. The static "debug"
 * variable of the VStatics class is initialized to false.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.doe.StaticsTest">Terry D. Norbraten</a>
 */
public class StaticsTest extends TestCase {
    
    LocalBootLoader loaderNoReset;
    Class<?> statics;
    Object rstatics;
    Object fdebug;
    Field debug; 
    
    public StaticsTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        loaderNoReset = (LocalBootLoader) VGlobals.instance().getWorkClassLoader();
        statics = loaderNoReset.loadClass("viskit.VStatics");
        Constructor sconstr = statics.getConstructor();
        rstatics = sconstr.newInstance();
        debug = statics.getDeclaredField("debug");
        debug.setAccessible(true);
        fdebug = debug.get(rstatics); // should be false
        System.out.println(fdebug);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        loaderNoReset = null;
        statics = null;
        debug = null;
        rstatics = null;
    }

    public void testStatics() throws Exception {
        
        LocalBootLoader loaderWithReset = (LocalBootLoader) VGlobals.instance().getFreshClassLoader();
        Class<?> staticz = loaderWithReset.loadClass("viskit.VStatics");
        Constructor sconstr = staticz.getConstructor();
        Object rstaticz = sconstr.newInstance();
        Field debugz = staticz.getDeclaredField("debug");
        debugz.setAccessible(true);
        Object fdebugz = debugz.get(rstaticz); // should be false
        System.out.println(fdebugz);
        
        Assert.assertTrue(fdebug == fdebugz);
        
        debug.set(rstaticz, true);
        fdebug = debug.get(rstatics);
        fdebugz = debugz.get(rstaticz);
        
        Assert.assertFalse(fdebug == fdebugz);
    }
    
    /**
     * Fetch the suite of tests for this test class to perform.
     *
     * @return A collection of all the tests to be run
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new StaticsTest("testStatics"));
        return suite;
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
