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
package edu.nps.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

/**
 * Log4j Logging utilities for Log4J2 v19+.
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.LogUtils">Terry Norbraten, NPS MOVES</a>
 */
public class LogUtils {
    
    private static final Logger LOG;

    static {
//        configureLog4j("log4j2.properties");
        LOG = getLogger(LogUtils.class);
    }

    /**<p>
     * This is a utility to configure the Log4J logger.
     * </p>
     * If requested configuration file can not be read, the default behavior
     * will be to use a DefaultConfigurator and set the debug level to INFO.
     *
     * @param configFile The file name to configure the logger with.
     * @return true if successful, false if failed to find/use the file
     */
    public static boolean configureLog4j(String configFile) {
        
        if (!configFile.isEmpty()) {

            Configurator.initialize(null, ClassLoader.getPlatformClassLoader(), configFile);

            return true;
        } else {
            // Set up a simple configuration that logs on the console
            // and set root logger level to INFO.
            Configurator.initialize(new DefaultConfiguration());
            Configurator.setRootLevel(Level.INFO);

            // The following is useful early on when developers are starting to
            // use log4j to know what is going on. We can remove this printout
            // in the future, or turn it into a log4j message!
            LOG.warn("Failed to read {}. Assuming INFO level and Console appender.", configFile);

            return false;
        }
    }

    /** Provide a synchronized method for multiple threads to use single
     * run-time logger
     * @param clazz the class type of the caller
     * @return synchronized method for multiple threads to use a single run-time logger
     */
    public static synchronized Logger getLogger(Class clazz) {
        return LogManager.getLogger(clazz);
    }

    /** @return a model to print a stack trace of calling classes and their methods */
    public static String printCallerLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(new Throwable().fillInStackTrace().getStackTrace()[4].getClassName());
        sb.append(" :");
        sb.append(new Throwable().fillInStackTrace().getStackTrace()[4].getLineNumber());
        sb.append(")");
        return sb.toString();
    }

} // end class file LogUtils.java
