/* Program:     Visual Discrete Event Simulation (DES) Toolkit (Viskit)
 *
 * Author(s):   Tony BenBrahim, Terry Norbraten
 *
 * Created on:  16 APR 2015
 *
 * File:        ZipUtilities.java
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Class that zips a directory that is pointed to.The  processFolder method
 reads from the input stream and writes onto the output stream until the input
 stream is exhausted.
 *
 * @author Tony BenBrahim
 * @author <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.ZipUtilities">Terry Norbraten, NPS MOVES</a>
 * @version $Id$
 */
public final class ZipUtilities
{
    static final Logger LOG = LogManager.getLogger();
    
    private static int      fileCount = 0;
    private static int directoryCount = 0;
    
    /** Initialize file and directory counts */
    public static void initializeCounts ()
    {
             fileCount = 0;
        directoryCount = 0;
    }

    public static void zipFolder(final File folder, final File zipFile) throws IOException {
        zipFolder(folder, new FileOutputStream(zipFile));
    }

    public static void zipFolder(final File folder, final OutputStream outputStream) throws IOException
    {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) 
        {
            processFolder(folder, zipOutputStream, folder.getPath().length() + 1);
        }
    }

    private static void processFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength)
            throws IOException 
    {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                Files.copy(file.toPath(), zipOutputStream);
                zipOutputStream.closeEntry();
                fileCount++;
            } 
            else if (file.isDirectory()) {

                // hard code hack here, but so far, this utility is only being
                // used to zip Viskit Projects, so, don't include the project's
                // /build directory, or a NetBeans project private directory
                if (!file.getName().equals("archive") && 
                    !file.getName().equals("build")   && 
                    !file.getName().equals("private"))
                {
                    processFolder(file, zipOutputStream, prefixLength);
                    directoryCount++;
                }
            }
        }
    }

    /**
     * @return the fileCount
     */
    public static int getFileCount() {
        return fileCount;
    }

    /**
     * @return the directoryCount
     */
    public static int getDirectoryCount() {
        return directoryCount;
    }

} // end class file ZipUtilities.java
