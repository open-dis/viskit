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
 * @since Oct 24, 2005
 * @since 9:42:57 AM
 */

/*
 * Simple1.2ClassLoader.java - simple Java 1.2 class loader
 *
 * Copyright (c) 1999 Ken McCrary, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * KEN MCCRARY MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. KEN MCCRARY
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package edu.nps.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *  Simple class loader to illustrate custom Class
 *  loading with new delegation model in Java 1.2.
 *  Derived from Simple1_2ClassLoader in
 *  <a href="http://www.javaworld.com/javaworld/jw-03-2000/jw-03-classload.html">this</a> Javaworld article.
 */
public class SimpleDirectoryClassLoader extends ClassLoader {

    private File root;

    /**
     *  Provide delegation constructor
     *
     * @param parent
     * @param directoryRoot 
     */
    public SimpleDirectoryClassLoader(ClassLoader parent, File directoryRoot) {
        super(parent);
        init(directoryRoot);
    }

    /**
     *  Same old ClassLoader constructor
     *
     * @param directoryRoot 
     */
    public SimpleDirectoryClassLoader(File directoryRoot) {
        super();
        init(directoryRoot);
    }

    // **************************************************************************
    // Initialize the ClassLoader by loading the Manifest, if there is one
    // The Manifest contains the package versioning information
    // For simplicity, our manifest will be placed in a store directory
    // **************************************************************************
    private void init(File directoryRoot) {
        root = directoryRoot;
        FileInputStream fi = null;

        // Check for a manifest in the store directory
        try {
            fi = new FileInputStream("store\\MANIFEST.MF");    // unused and untested
            manifest = new Manifest(fi);
        } catch (Exception e) {
        // No manifest
        } finally {
            if (null != fi) {
                try {
                    fi.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     *  This is the method where the task of class loading
     *  is delegated to our custom loader.
     *
     * @param  name the name of the class
     * @return the resulting <code>Class</code> object
     * @exception ClassNotFoundException if the class could not be found
     */
    @Override
    protected Class findClass(String name) throws ClassNotFoundException {
        FileInputStream fi = null;

        try {
            String path = name.replace('.', '/');
            // fi = new FileInputStream("store/" + path + ".impl");
            fi = new FileInputStream(root.getAbsolutePath() + "/" + path + ".class");
            byte[] classBytes = new byte[fi.available()];
            fi.read(classBytes);
            definePackage(name);
            return defineClass(name, classBytes, 0, classBytes.length);

        } catch (Exception e) {
            // We could not find the class, so indicate the problem with an exception
            throw new ClassNotFoundException(name);
        } finally {
            if (null != fi) {
                try {
                    fi.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     *  Identify where to load a resource from, resources for
     *  this simple ClassLoader are in a directory name "store"
     *
     *  @param name the resource name
     *  @return URL for resource or null if not found
     */
    @Override
    protected URL findResource(String name) {
        File searchResource = new File("store\\" + name);
        URL result = null;

        if (searchResource.exists()) {
            try {
                return searchResource.toURI().toURL();
            } catch (MalformedURLException mfe) {
            }
        }

        return result;
    }

    /**
     *  Used for identifying resources from multiple URLS
     *  Since our simple Classloader only has one repository
     *  the returned Enumeration contains 0 to 1 items
     *
     *  @param name the resource name
     *  @return Enumeration of one URL
     * @throws IOException 
     */
    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        // Since we only have a single repository we will only have one
        // resource of a particular name, the Enumeration will just return
        // this single URL

        return new Enumeration<URL>() {

            URL resource = findResource(name);

            public boolean hasMoreElements() {
                return (resource != null ? true : false);
            }

            public URL nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                } else {
                    URL result = resource;
                    resource = null;
                    return result;
                }
            }
        };
    }

    /**
     *  Minimal package definition
     *
     * @param className 
     */
    private void definePackage(String className) {
        // Extract the package name from the class name,
        String pkgName = className;
        int index = className.lastIndexOf('.');
        if (-1 != index) {
            pkgName = className.substring(0, index);
        }

        // Pre-conditions - need a manifest and the package
        // is not previously defined
        if (null == manifest ||
                getPackage(pkgName) != null) {
            return;
        }

        String specTitle,
                specVersion,
                specVendor,
                implTitle,
                implVersion,
                implVendor;

        // Look up the versioning information
        // This should really look for a named attribute
        Attributes attr = manifest.getMainAttributes();

        if (null != attr) {
            specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);

            definePackage(pkgName,
                    specTitle,
                    specVersion,
                    specVendor,
                    implTitle,
                    implVersion,
                    implVendor,
                    null); // no sealing for simplicity
        }
    }
    private Manifest manifest;
}
