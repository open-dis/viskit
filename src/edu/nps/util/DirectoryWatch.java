/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 * A class to observe a directory (tree) for changes and report them to listeners
 */
public class DirectoryWatch 
{
    static final Logger LOG = LogUtilities.getLogger(DirectoryWatch.class);

    private static int sequenceNumber = 0;
    private final static int   DEFAULTSLEEPTIMEMS = 3 * 1_000; // 3 seconds
    private long sleepTimeMs = DEFAULTSLEEPTIMEMS;
    private Map<File, Long> lastFiles;
    private Thread thread;
    private File root;

    public DirectoryWatch(File root) throws FileNotFoundException 
	{
        this(root, false);
    }

    public DirectoryWatch(File root, boolean recurse) throws FileNotFoundException 
	{
        this(root, recurse, null);
    }

    public DirectoryWatch(File root, boolean recurse, DirectoryChangeListener directoryChangeListener) throws FileNotFoundException
	{
        buildInitialFileList(root, recurse);
        if (directoryChangeListener != null) 
		{
            addListener(directoryChangeListener);
        }
        this.root = root;
    }

    public void startWatcher() 
	{
        thread = new Thread(new Runner(), "DirectoryWatch-" + sequenceNumber++);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void stopWatcher()
	{
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void setLoopSleepTime(long ms) {
        sleepTimeMs = ms;
    }

    private void buildInitialFileList(File rootFile, boolean recurse) throws FileNotFoundException 
	{
        if (!rootFile.exists()) 
		{
            throw new FileNotFoundException("File or directory passed to DirectoryWatch constructor does not exist");
        }
        lastFiles = new HashMap<>();

        FileAdder fa = new FileAdder();
        if (recurse) 
		{
            recurseTree(rootFile, new FileAdder());
        } else 
		{
            fa.foundFile(rootFile);
        }
    }

    class FileAdder implements RecurseListener
	{
        @Override
        public void foundFile(File file)
		{
            lastFiles.put(file, file.lastModified());
        }
    }

    private void recurseTree(File file, RecurseListener recurseListener) 
	{
        if (file == null)
		{
            return;
        }
        if (file.isHidden())
		{
            return;
        }
        if (file.isFile()) 
		{
            recurseListener.foundFile(file);
        } 
		else 
		{
            File[] containedFileArray = file.listFiles();
            if (containedFileArray != null)
			{
                for (File containedFile : containedFileArray) 
				{
                    recurseTree(containedFile, recurseListener);
                }
            }
        }
    }
    private Set<DirectoryChangeListener> listeners = new HashSet<>();

    /**
     * @param listener listener for this directory
     * @return true if was not already registered
     */
    public final boolean addListener(DirectoryChangeListener listener) {
        return listeners.add(listener);
    }

    /**
     * @param listener listener for this directory
     * @return true if it had been registered
     */
    public boolean removeListener(DirectoryChangeListener listener) {
        return listeners.remove(listener);
    }

    private void fireAction(File file, int action)
	{
        for (DirectoryChangeListener listener : listeners) 
		{
            listener.fileChanged(file, action, this);
        }
    }

    class Runner implements Runnable, RecurseListener 
	{
        Map<File, Long> workingHashMap = new HashMap<>(50);

        @Override
        public void run()
		{
            while (true) 
			{
                workingHashMap.clear();

                added.clear();
                changed.clear();

                // Want to send out updates in this order: removed,changed,added
                recurseTree(root, this);    // this removes from lastFiles

                // Now see if any were removed...they will be the ones left
                for (File cPath : lastFiles.keySet()) 
				{
                    fireAction(cPath, DirectoryChangeListener.FILE_REMOVED);
                }
                Map<File, Long> temp = lastFiles;
                lastFiles = workingHashMap;
                workingHashMap = temp; // gets zeroed above
                for (File f : changed) 
				{
                    fireAction(f, DirectoryChangeListener.FILE_CHANGED);
                }
                for (File f : added)
				{
                    fireAction(f, DirectoryChangeListener.FILE_ADDED);
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } 
				catch (InterruptedException e) 
				{
                    LOG.error("DirectoryWatcher killed", e);
                    break;
                }
            }
        }
        Vector<File> added = new Vector<>();
        Vector<File> changed = new Vector<>();

        @Override
        public void foundFile(File file)
		{
            long modifiedDate = file.lastModified();

            Long lastFileDate = lastFiles.get(file);

            if (lastFileDate == null)
			{
                added.add(file);
            } 
			else 
			{
                lastFiles.remove(file);
                if (lastFileDate != modifiedDate) 
				{
                    changed.add(file);
                }
            }
            workingHashMap.put(file, modifiedDate);
        }
    }

    interface RecurseListener 
	{
        void foundFile(File rile);
    }

    public interface DirectoryChangeListener 
	{
        int FILE_ADDED   = 0;
        int FILE_REMOVED = 1;
        int FILE_CHANGED = 2;

        void fileChanged(File file, int action, DirectoryWatch source);
    }
}
