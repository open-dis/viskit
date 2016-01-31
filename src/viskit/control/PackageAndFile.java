package viskit.control;

import java.io.File;

/**
 * Utility class to hold java source package and file names
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.PkgAndFile">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class PackageAndFile {

    public String pkg;
    public File file;

    public PackageAndFile(String packageName, File file) {
        this.pkg = packageName;
        this.file = file;
    }

} // end class file PackageAndFile.java
