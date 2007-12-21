package viskit.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Reads a file assumed to be in the following form (2 lines):
 * <pre>
 * <major version>.<minor version>.<subminor version>
 * $Date: 2007-12-16 11:44:04 -0800 (Sun, 16 Dec 2007) $
 * </pre>
 *
 *
 * @version $Id: Version.java 1662 2007-12-16 19:44:04Z tdnorbra $
 * @author ahbuss
 */
public class Version {
    
    public static Logger log = Logger.getLogger("viskit.util");
    
    protected String versionString;
    
    protected int majorVersion;
    
    protected int minorVersion;
    
    protected int patchVersion;
    
    protected Date lastModified;
    
    public Version(String versionString, String dateString) {
        int[] version = parseVersionString(versionString);
        majorVersion = version[0];
        minorVersion = version[1];
        patchVersion = version[2];
        lastModified = parseDateString(dateString);
    }
    
    public Version(String fileName) {
        InputStream versionStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(versionStream));
        try {
            versionString = reader.readLine();
            int[] version = parseVersionString(versionString);
            majorVersion = version[0];
            minorVersion = version[1];
            patchVersion = version[2];
            String dateString = reader.readLine();
            lastModified = parseDateString(dateString);
        } catch (IOException e) {
            log.fine("Problem reading " + fileName + ": " + e);
        }
    }
    
    protected static int[] parseVersionString(String versionString) {
        String[] versions = versionString.split("\\.");
        if (versions.length != 3) {
            log.fine("Expected x.y.x: " + versionString);
            throw new IllegalArgumentException("Expected x.y.x: " + versionString +
                    " length = " + versions.length);
        }
        int[] versionNumber = new int[versions.length];
        for (int i = 0; i < versionNumber.length; ++i) {
            versionNumber[i] = Integer.parseInt(versions[i]);
        }
        return versionNumber;
    }
    
    protected static Date parseDateString(String dateString) {
        GregorianCalendar calendar = null;
        try {
            String[] data = dateString.split("[: $-]");
            int year = Integer.parseInt(data[3]);
            int month = Integer.parseInt(data[4]);
            int day = Integer.parseInt(data[5]);
            int hour = Integer.parseInt(data[6]);
            int minute = Integer.parseInt(data[7]);
            int second = Integer.parseInt(data[8]);
            
            calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
            calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        } catch (Throwable t) {
            System.err.println("Problem parsing date string " + dateString + ": " + t);
        }
        return calendar.getTime();
    }
    
    public String getVersionString() {
        return versionString;
    }
    
    public Date getLastModified() {
        return lastModified;
    }
    
    public int getMajorVersion() {
        return majorVersion;
    }
    
    public int getMinorVersion() {
        return minorVersion;
    }
    
    public int getPatchVersion() {
        return patchVersion;
    }
    
    public boolean isSameVersionAs(String otherVersionString) {
        return getVersionString().equals(otherVersionString);
    }
    
    public boolean isHigherVersionThan(String otherVersionString) {
        int[] otherVersion = parseVersionString(otherVersionString);
        return getMajorVersion() > otherVersion[0] ||
                getMinorVersion() > otherVersion[1] ||
                getPatchVersion() > otherVersion[2];
    }
    
    @Override
    public String toString() {
        return "Version " +  + getMajorVersion() + "." +
                getMinorVersion() + "." + getPatchVersion() +
                System.getProperty("line.separator") +
                "Last Modified: " + getLastModified();
    }
}
