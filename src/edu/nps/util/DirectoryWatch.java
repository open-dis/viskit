/*
Copyright (c) 1995-2007 held by the author(s).  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.movesinstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Sep 22, 2005
 * @since 10:04:25 AM
 */
package edu.nps.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * A class to observe a directory (tree) for changes and report them to listeners
 */
public class DirectoryWatch {

    private static int sequenceNum = 0;
    private final static int DEFAULTSLEEPTIMEMS = 3 * 1_000; // 3 seconds
    private long sleepTimeMs = DEFAULTSLEEPTIMEMS;
    private Map<String, Long> lastFiles;
    private Thread thread;
    private File root;

    public DirectoryWatch(File root) throws FileNotFoundException {
        this(root, false);
    }

    public DirectoryWatch(File root, boolean recurse) throws FileNotFoundException {
        this(root, recurse, null);
    }

    public DirectoryWatch(File root, boolean recurse, DirectoryChangeListener lis) throws FileNotFoundException {
        buildInitialList(root, recurse);
        if (lis != null) {
            addListener(lis);
        }
        this.root = root;
    }

    public void startWatcher() {
        thread = new Thread(new Runner(), "DirectoryWatch-" + sequenceNum++);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void stopWatcher() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void setLoopSleepTime(long ms) {
        sleepTimeMs = ms;
    }

    private void buildInitialList(File root, boolean recurse) throws FileNotFoundException {
        if (!root.exists()) {
            throw new FileNotFoundException("File or directory passed to DirectoryWatch constructor does not exist");
        }
        lastFiles = new HashMap<>(50);

        fileAdder fa = new fileAdder();
        if (recurse) {
            recurseTree(root, new fileAdder());
        } else {
            fa.foundFile(root);
        }
    }

    class fileAdder implements RecurseListener {

        @Override
        public void foundFile(File f) {
            try {
                lastFiles.put(f.getCanonicalPath(), f.lastModified());
            } catch (IOException e) {
                System.err.println("error in getCanonicalPath() of " + f.toString());
            }
        }
    }

    private void recurseTree(File f, RecurseListener lis) {
        if (f == null) {
            return;
        }
        if (f.isHidden()) {
            return;
        }
        if (f.isFile()) {
            lis.foundFile(f);
        } else {
            File[] fa = f.listFiles();
            if (fa != null) {
                for (File fa1 : fa) {
                    recurseTree(fa1, lis);
                }
            }
        }
    }
    private Set<DirectoryChangeListener> listeners = new HashSet<>();

    /**
     * @param lis
     * @return true if was not already registered
     */
    public final boolean addListener(DirectoryChangeListener lis) {
        return listeners.add(lis);
    }

    /**
     * @param lis
     * @return true if it had been registered
     */
    public boolean removeListener(DirectoryChangeListener lis) {
        return listeners.remove(lis);
    }

    private void fireAction(File f, int action) {
        for (DirectoryChangeListener lis : listeners) {
            lis.fileChanged(f, action, this);
        }
    }

    class Runner implements Runnable, RecurseListener {

        Map<String, Long> workingHM = new HashMap<>(50);

        @Override
        public void run() {
            while (true) {
                workingHM.clear();

                added.clear();
                changed.clear();

                // Want to send out updates in this order: removed,changed,added
                recurseTree(root, this);    // this removes from lastFiles

                // Now see if any were removed...they will be the ones left
                for (String cPath : lastFiles.keySet()) {
                    fireAction(new File(cPath), DirectoryChangeListener.FILE_REMOVED);
                }
                Map<String, Long> temp = lastFiles;
                lastFiles = workingHM;
                workingHM = temp; // gets zeroed above
                for (File f : changed) {
                    fireAction(f, DirectoryChangeListener.FILE_CHANGED);
                }
                for (File f : added) {
                    fireAction(f, DirectoryChangeListener.FILE_ADDED);
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e) {
                    System.err.println("DirWatcher killed");
                    break;
                }
            }
        }
        Vector<File> added = new Vector<>();
        Vector<File> changed = new Vector<>();

        @Override
        public void foundFile(File f) {
            String canonP;
            try {
                canonP = f.getCanonicalPath();
            } catch (IOException e) {
                System.out.println("error in getCanonicalPath() of " + f.toString());
                return;
            }
            long moddate = f.lastModified();

            Long lastdate = lastFiles.get(canonP);

            if (lastdate == null) {
                added.add(f);//fireAction(f,DirectoryChangeListener.FILE_ADDED);
            } else {
                lastFiles.remove(canonP);
                if (lastdate != moddate) {
                    changed.add(f); //fireAction(f,DirectoryChangeListener.FILE_CHANGED);
                }
            }
            workingHM.put(canonP, moddate);
        }
    }

    interface RecurseListener {
        void foundFile(File f);
    }

    public interface DirectoryChangeListener {

        int FILE_ADDED = 0;
        int FILE_REMOVED = 1;
        int FILE_CHANGED = 2;

        void fileChanged(File file, int action, DirectoryWatch source);
    }
}
