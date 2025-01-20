package viskit.util;

import edu.nps.util.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import viskit.ViskitGlobals;

/**
 * Reads a file assumed to be in the following form (2 lines):
 * <pre>
 * &lt;major version&gt;.&lt;minor version&gt;.&lt;subminor version&gt;
 * $Date: 2007-12-16 11:44:04 -0800 (Sun, 16 Dec 2007) $
 * </pre>
 *
 * @author ahbuss
 */
public class Version {

    static Logger LOG = LogUtils.getLogger(Version.class);

    protected String versionString;

    protected int majorVersion;

    protected int minorVersion;

    protected int patchVersion;
    
    protected LocalDate lastModified;
    
    private Properties versionProperties;

    public Version(String fileName) {
        
        InputStream stream;
        
        versionProperties = new Properties();
        
        // version.txt is in the same package as ViskitGlobals
        URL versionURL = ViskitGlobals.class.getResource(fileName);
        
        if (versionURL != null)
            if (versionURL.getProtocol().equals("jar"))
                stream = ViskitGlobals.class.getResourceAsStream(fileName);
            else {
                try {
                    stream = versionURL.openStream();
                } catch (IOException ex) {
                    LOG.error("Problem reading {} due to {}: ", fileName, ex);
                    return;
                }
            }
        else {
            LOG.error("Problem reading {} due to {}: ", fileName, versionURL);
            return;
        }
        try {
            if (stream != null)
                versionProperties.load(stream);
            else {
                LOG.error("Problem reading {} due to {}: ", fileName, stream);
                return;
            }
        } catch (IOException ex) {
            LOG.error(ex);
            return;
        }
        
        Version.this.parseInputFile(versionURL);
        
        majorVersion = Version.this.parseVersionString(versionProperties.getProperty("major"));
        minorVersion = Version.this.parseVersionString(versionProperties.getProperty("minor"));
        patchVersion = Version.this.parseVersionString(versionProperties.getProperty("patch"));
        versionString = String.format("%s.%s.%s", majorVersion, minorVersion, patchVersion);
    }

    protected int parseVersionString(String input) {
        return Integer.parseInt(input);
    }

    protected void parseInputFile(URL versionURL) {
        
        Path versionPath;
        
        try {
            
            // Just use the viskit.jar lastModified file property
            if (versionURL.getProtocol().equals("jar"))
                versionPath = Paths.get(versionURL.toURI().toString().substring(versionURL.toURI().toString().lastIndexOf(":") + 1, versionURL.toURI().toString().indexOf("!")));
            else
                versionPath = Paths.get(versionURL.toURI());
            BasicFileAttributes attributes
                    = Files.readAttributes(versionPath, BasicFileAttributes.class);
            Date date = new Date(attributes.lastModifiedTime().toMillis());
            Instant instant = date.toInstant();
            ZonedDateTime zoneDateTime = instant.atZone(ZoneId.systemDefault());
            lastModified = zoneDateTime.toLocalDate();
        } catch (IOException | URISyntaxException ex) {
            LOG.error(ex);
        }
    }

    public String getVersionString() {
        return versionString;
    }

    public LocalDate getLastModified() {
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

    @Override
    public String toString() {
        return "Version " + +getMajorVersion() + "."
                + getMinorVersion() + "." + getPatchVersion()
                + System.getProperty("line.separator")
                + "Last Modified: " + getLastModified();
    }
}
