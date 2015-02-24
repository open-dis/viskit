/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.nps.util;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import static java.nio.file.FileVisitResult.*;

/**
 * Code that finds files that
 * match the specified regex pattern.
 *
 * Modified for finding class files on the classpath
 *
 * The file or directories that match
 * the pattern are printed to
 * standard out when executed using the main method.
 *
 * When executing this application:
 *     java Find . -name *.java
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.FindFile">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class FindFile extends SimpleFileVisitor<Path> {

    private final PathMatcher matcher;
    private Path path;

    /**
     * A {@code FileVisitor} that finds all files that match the specified
     * pattern.
     *
     * @param pattern the regex pattern to match
     */
    public FindFile(String pattern) {
        matcher = FileSystems.getDefault()
                .getPathMatcher("regex:" + pattern);
    }

    /** Compares the glob pattern against the file or directory name
     * 
     * @param file the path to evaluate
     * @return a Path found during a match, or null if not found
     */
    Path find(Path file) {
        Path name = file.getFileName();
        if (name != null && matcher.matches(name)) {
            return file;
        }
        return null;
    }

    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {

        if (path == null)
            path = find(file);

        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir,
            BasicFileAttributes attrs) {

        if (path == null)
            path = find(dir);

        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file,
            IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }

    public Path getPath() {
        return path;
    }

    static void usage() {
        System.err.println("java Find <path>"
                + " -name <regex_pattern>");
    }

    public static void main(String[] args)
            throws IOException {

        if (args.length < 3 || !args[1].equals("-name")) {
            usage();
        }

        Path startingDir = Paths.get(args[0]);
        String pattern = args[2];

        FindFile finder = new FindFile(pattern);
        Files.walkFileTree(startingDir, finder);

        if (finder.path != null)
            System.out.println("Path found: " + finder.path);
    }

} // end class file FindFile.java
