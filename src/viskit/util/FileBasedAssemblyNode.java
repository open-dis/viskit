package viskit.util;

import edu.nps.util.Log4jUtilities;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility class to help identify whether an Event Graph or PCL is from XML or *.class
 * form.  Used to help populate the LEGO tree on the Assembly Editor.
 *
 * <pre>
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * </pre>
 *
 * @author Mike Bailey
 * @since Jul 21, 2004
 * @since 3:26:42 PM
 * @version $Id$
 */
public class FileBasedAssemblyNode
{
    static final Logger LOG = LogManager.getLogger();

    public static final String FBAN_DELIM = "<fbasdelim>";
    public String loadedClass;
    public File xmlSource;
    public File classFile;
    public boolean isXML;
    public String pkg;
    public long lastModified;

    public FileBasedAssemblyNode(File classFile, String loadedClass, File xml, String pkg) {
        this.classFile = classFile;
        this.loadedClass = loadedClass;
        this.xmlSource = xml;
        this.pkg = pkg;
        isXML = (xml != null);
        lastModified = (isXML) ? xml.lastModified() : classFile.lastModified();
    }

    public FileBasedAssemblyNode(File classFile, String loadedClass, String pkg) {
        this(classFile, loadedClass, null, pkg);
    }

    @Override
    public String toString() {
        if (isXML) {
            return classFile.getPath() + FBAN_DELIM + loadedClass + FBAN_DELIM + xmlSource.getPath() + FBAN_DELIM + pkg;
        } else {
            return classFile.getPath() + FBAN_DELIM + loadedClass + FBAN_DELIM + pkg;
        }
    }

    public static FileBasedAssemblyNode fromString(String s) throws FileBasedAssemblyNode.exception {
        try {
            String[] sa = s.split(FBAN_DELIM);
            if (sa.length == 3) {
                return new FileBasedAssemblyNode(new File(sa[0]), sa[1], sa[2]);
            } else if (sa.length == 4) {
                return new FileBasedAssemblyNode(new File(sa[0]), sa[1], new File(sa[2]), sa[3]);
            }
        } 
        catch (Exception e)
        {
            LOG.error("FileBasedAssemblyNode fromString() exception: " + e.getMessage());
            throw new FileBasedAssemblyNode.exception();
        }
        return null;
    }

    public static class exception extends Exception {}
}
