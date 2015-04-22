/* Program:     Visual Discrete Event Simulation (DES) Toolkit (Viskit)
 *
 * Author(s):   Tony BenBrahim, Terry Norbraten
 *
 * Created on:  16 APR 2015
 *
 * File:        ZipUtils.java
 *
 * Compiler:    JDK1.8
 * O/S:         Mac OS X Yosimite (10.10.3)
 *
 * Description: Class that zips a directory that is pointed to.  The
 *              processFoler method reads from the input stream and
 *              writes onto the output stream until the input stream is
 *              exhausted.
 *
 * Information: Borrowed from stackoverflow
 *              http://stackoverflow.com/questions/15968883/how-to-zip-a-folder-itself-using-java
 */
package edu.nps.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Class that zips a directory that is pointed to.  The  processFoler method
 * reads from the input stream and writes onto the output stream until the input
 * stream is exhausted.
 *
 * @author Tony BenBrahim
 * @author <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.ZipUtils">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public final class ZipUtils {

    public static void zipFolder(final File folder, final File zipFile) throws IOException {
        zipFolder(folder, new FileOutputStream(zipFile));
    }

    public static void zipFolder(final File folder, final OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            processFolder(folder, zipOutputStream, folder.getPath().length() + 1);
        }
    }

    private static void processFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength)
            throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    StreamCopier.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                processFolder(file, zipOutputStream, prefixLength);
            }
        }
    }

} // end class file ZipUtils.java
