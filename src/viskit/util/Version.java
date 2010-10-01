package viskit.util;

import edu.nps.util.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * Reads a file assumed to be in the following form (2 lines):
 * <pre>
 * <major version>.<minor version>.<subminor version>
 * $Date: 2007-12-16 11:44:04 -0800 (Sun, 16 Dec 2007) $
 * </pre>
 *
 * @version $Id$
 * @author ahbuss
 */
public class Version {
    
    static Logger log = LogUtils.getLogger(Version.class);
    
    protected String versionString;
    
    protected int majorVersion;
    
    protected int minorVersion;
    
    protected int patchVersion;
    
    protected Date lastModified;
    
    protected int svnRevisionNumber;
    
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
            String revisionString = reader.readLine();
            svnRevisionNumber = parseRevisionString(revisionString);
            versionString += "." + svnRevisionNumber;
        } catch (IOException e) {
            log.error("Problem reading " + fileName + ": " + e);
        }
    }
    
    protected static int[] parseVersionString(String versionString) {
        String[] versions = versionString.split("\\.");
//        if (versions.length != 4) {
//            log.warn("Expected w.x.y.z: " + versionString);
//            throw new IllegalArgumentException("Expected w.x.y.z: " + versionString +
//                    " length = " + versions.length);
//        }
        int[] versionNumber = new int[versions.length];
        for (int i = 0; i < versionNumber.length; ++i) {
            versionNumber[i] = Integer.parseInt(versions[i]);
        }
        return versionNumber;
    }
    
    protected static Date parseDateString(String dateString) {
        Date date = null;
        try {
            Pattern pattern = 
                    Pattern.compile("\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d \\d\\d:\\d\\d:\\d\\d");
            Matcher matcher = pattern.matcher(dateString);
            if (matcher.find()) {
                date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(matcher.group());
            }
        } catch (Throwable t) {
            log.error("Problem parsing date string " + dateString + ": " + t);
        }
        return date;
    }
    
    protected static int parseRevisionString(String revisionString) {
        String[] data = revisionString.split("\\D+");
        return Integer.parseInt(data[1]);
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
    
    public int getSVNRevisionNumber() {
        return svnRevisionNumber;
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
