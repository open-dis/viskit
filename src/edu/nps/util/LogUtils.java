package edu.nps.util;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Log4j Logging utilities.
 * <br>
 * For the time being, this class has log4j utilities but it may be changed
 * to java.log.util in the future!
 *
 * @author suleyman <br>
 * @version $Id: LogUtils.java 4921 2008-11-19 00:44:23Z tnorbraten $
 */
public class LogUtils {
    private static final Logger LOG = Logger.getLogger(LogUtils.class);

    static {
        configureLog4j("configuration/log4j.properties");
    }

    /**
     * This is a utility to configure the Log4j logger.
     * <p/>
     * If requested configuration file can not be read, the default bahavior
     * will be to use BasicConfigurator and set the debug lefvl to INFO.
     *
     * @param configFileFname The file name to configure the logger with.
     * @return true if successful, false if failed to find/use the file
     */
    public static boolean configureLog4j(String configFileFname) {
        return configureLog4j(configFileFname, false);
    }

    /**
     * This is a utility to configure the Log4J logger.
     * <p/>
     * If requested configuration file can not be read, the default bahavior
     * will be to use BasicConfigurator and set the debug level to INFO.
     *
     * @param configFileFname The file name to configure the logger with.
     * @param watch           not used
     * @return true if successful, false if failed to find/use the file
     */
    public static boolean configureLog4j(String configFileFname, boolean watch) {
        InputStream inStream = LogUtils.class.getClassLoader().getResourceAsStream(configFileFname);
        Properties props = new Properties();
        if (inStream != null) {
            try {
                props.load(inStream);
                inStream.close();
            } catch (IOException ioe) {
                LogLog.error("" + ioe);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException ioe) {}
                }
            }

            PropertyConfigurator.configure(props);

            // The following is useful early on when developers are starting to
            // use log4j to know what is going on.  We can remove this printout
            // in the future, or turn it into a log4j message!
            LOG.debug(configFileFname + " is used to configure log4j.");
            return true;
        } else {
            // Set up a simple configuration that logs on the console
            // and set root logger level to INFO.
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.INFO);

            // The following is useful early on when developers are starting to
            // use log4j to know what is going on. We can remove this printout
            // in the future, or turn it into a log4j message!
            LOG.warn("Failed to read " + configFileFname + ". " +
                    "Assuming INFO level and Console appender.");

            return false;
        }
    }

    /** @return synchronized method for multiple threads to use single run-time logger */
    public static synchronized Logger getLogger() {return LOG;}

    /** @return a model to print a stack trace of calling classes and their methods */
    public static String printCallerLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("(" + new Throwable().fillInStackTrace().getStackTrace()[4].getClassName());
        sb.append(" :" + new Throwable().fillInStackTrace().getStackTrace()[4].getLineNumber() + ")");
        return sb.toString();
    }

} // end class file LogUtils.java