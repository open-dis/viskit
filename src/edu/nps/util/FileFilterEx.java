/*
Copyright (c) 1995-2008 held by the author(s).  All rights reserved.

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

package edu.nps.util;

/**
 * Filename      : FileFilterEx
 * Description   : Define a file filter (extension, description)
 *                 in drop-down combo-box
 *
 * Created Date  : 15 February 2004
 * Course        : Thesis Work
 * Program       : Viskit
 * Compiler      : JDK 1.4.2 onwards
 * Platform      : Windows 2000/Windows XP
 * @author Lee, Chin Siong Daryl
 * @version $Id:$
 */
public class FileFilterEx extends javax.swing.filechooser.FileFilter {

    /** file extension */
    private String[] _extensions;
    /** message to be displayed in combo-box */
    private String _msg;
    private boolean _showDirs;

    public FileFilterEx(String extension, String msg) {
        this(extension, msg, false);
    }

    public FileFilterEx(String extension, String msg, boolean showDirectories) {
        this(new String[] {extension}, msg, showDirectories);
    }

    public FileFilterEx(String[] extensions, String msg, boolean showDirectories) {
        this._extensions = extensions;
        this._msg = msg;
        this._showDirs = showDirectories;
    }
    
    /**
     * accept/refuse file
     * @param f a File
     * @return true/false
     */
    public boolean accept(java.io.File f) {
        if (f.isDirectory()) {
            return _showDirs;
        }
        for (String extension : _extensions) {
            if (f.getName().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * get description of file extension
     * @return String value
     */
    public String getDescription() {
        return _msg;
    }
} // FileFilterEx
