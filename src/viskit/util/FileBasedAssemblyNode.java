package viskit.util;

import edu.nps.util.LogUtilities;
import java.io.File;
import org.apache.logging.log4j.Logger;

/** Utility class to help identify whether an EG or PCL is from XML or *.class
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
    static final Logger LOG = LogUtilities.getLogger(FileBasedAssemblyNode.class);

    public static final String FBAS_DELIM = "<fbasdelim>";
    public String loadedClass;
    public File xmlSource;
    public File classFile;
    public boolean isXML;
    public String packageName;
    public long lastModified;

    public FileBasedAssemblyNode(File classFile, String loadedClass, File xml, String packageName) 
	{
        this.classFile   = classFile;
        this.loadedClass = loadedClass;
        this.xmlSource   = xml;
        this.packageName = packageName;
        isXML = (xml != null);
        lastModified = (isXML) ? xml.lastModified() : classFile.lastModified();
    }

    public FileBasedAssemblyNode(File classFile, String loadedClass, String packageName) {
        this(classFile, loadedClass, null, packageName);
    }

    @Override
    public String toString() 
	{
        if (isXML) 
		{
            return classFile.getPath() + FBAS_DELIM + loadedClass + FBAS_DELIM + xmlSource.getPath() + FBAS_DELIM + packageName;
        } 
		else 
		{
            return classFile.getPath() + FBAS_DELIM + loadedClass + FBAS_DELIM + packageName;
        }
    }

    public static FileBasedAssemblyNode fromString(String s) throws FileBasedAssemblyNode.exception 
	{
        try {
            String[] sa = s.split(FBAS_DELIM);
            if (sa.length == 3) {
                return new FileBasedAssemblyNode(new File(sa[0]), sa[1], sa[2]);
            } else if (sa.length == 4) {
                return new FileBasedAssemblyNode(new File(sa[0]), sa[1], new File(sa[2]), sa[3]);
            }
        } catch (Exception e) {}
        throw new FileBasedAssemblyNode.exception();
    }

    public static class exception extends Exception {}
}
