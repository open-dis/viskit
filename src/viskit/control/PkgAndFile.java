package viskit.control;

import java.io.File;

/**
 * Utility class to hold java source package and file names
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.control.PkgAndFile">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class PkgAndFile {

    public String pkg;
    public File f;

    public PkgAndFile(String pkg, File f) {
        this.pkg = pkg;
        this.f = f;
    }

} // end class file PkgAndFile.java
