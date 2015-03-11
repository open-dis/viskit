package viskit.util;

import java.io.File;

/** Utility class to help identify whether an EG or PCL is from XML or *.class
 * form.  Used to help populate the LEGO tree on the Assy Editor.
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
public class FileBasedAssyNode {

    public static final String FBAN_DELIM = "<fbasdelim>";
    public String loadedClass;
    public File xmlSource;
    public File classFile;
    public boolean isXML;
    public String pkg;
    public long lastModified;

    public FileBasedAssyNode(File classFile, String loadedClass, File xml, String pkg) {
        this.classFile = classFile;
        this.loadedClass = loadedClass;
        this.xmlSource = xml;
        this.pkg = pkg;
        isXML = (xml != null);
        lastModified = (isXML) ? xml.lastModified() : classFile.lastModified();
    }

    public FileBasedAssyNode(File classFile, String loadedClass, String pkg) {
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

    public static FileBasedAssyNode fromString(String s) throws FileBasedAssyNode.exception {
        try {
            String[] sa = s.split(FBAN_DELIM);
            if (sa.length == 3) {
                return new FileBasedAssyNode(new File(sa[0]), sa[1], sa[2]);
            } else if (sa.length == 4) {
                return new FileBasedAssyNode(new File(sa[0]), sa[1], new File(sa[2]), sa[3]);
            }
        } catch (Exception e) {}
        throw new FileBasedAssyNode.exception();
    }

    public static class exception extends Exception {}
}
