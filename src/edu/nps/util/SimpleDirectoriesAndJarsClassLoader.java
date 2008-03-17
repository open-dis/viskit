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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Oct 24, 2005
 * @since 9:42:57 AM
 * 
 * Simple class loader to illustrate custom Class
 * loading with new delegation model in Java 1.2.
 * Derived from Simple1_2ClassLoader in
 * <a href="http://www.javaworld.com/javaworld/jw-03-2000/jw-03-classload.html">this</a> Javaworld article.
 */
public class SimpleDirectoriesAndJarsClassLoader extends ClassLoader {

    /**
     *  Provide delegation constructor
     *
     * @param parent
     * @param dirsJarsOrZips 
     */
    public SimpleDirectoriesAndJarsClassLoader(ClassLoader parent, String[] dirsJarsOrZips) {
        super(parent);
        init(dirsJarsOrZips);
    }

    /**
     *  Same old ClassLoader constructor
     *
     * @param dirsJarsOrZips 
     */
    public SimpleDirectoriesAndJarsClassLoader(String[] dirsJarsOrZips) {
        super();
        init(dirsJarsOrZips);
    }
    Object[] roots;
    private Manifest manifest;
    // **************************************************************************
    // Initialize the ClassLoader by loading the Manifest, if there is one
    // The Manifest contains the package versioning information
    // For simplicity, our manifest will be placed in a store directory
    // **************************************************************************
    private void init(String[] dirsJarsOrZips) {
        roots = new Object[dirsJarsOrZips.length];

        for (int i = 0; i < dirsJarsOrZips.length; i++) {
            String s = dirsJarsOrZips[i];
            if (s.toLowerCase().endsWith(".jar")) {
                roots[i] = handleJar(s);
            } else if (s.toLowerCase().endsWith(".zip")) {
                roots[i] = handleZip(s);
            } else {
                roots[i] = handleDir(s);
            }
        }

    }

    private JarFile handleJar(String path) {
        try {
            return new JarFile(path);
        } catch (IOException e) {
            System.err.println("Bad jar file load: " + e.getMessage());
            return null;
        }
    }

    private ZipFile handleZip(String path) {
        try {
            return new ZipFile(path);
        } catch (IOException e) {
            System.err.println("Bad zip file load: " + e.getMessage());
            return null;
        }
    }

    private File handleDir(String path) {
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
            return f;
        } else {
            return null;
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

        String path = name.replace('.', '/');
        path = path + ".class";
        InputStream is = null;
        for (int i = 0; i < roots.length; i++) {
            if (roots[i] == null) {
                continue;
            }
            try {
                if (roots[i] instanceof JarFile) {
                    JarFile jf = (JarFile) roots[i];
                    JarEntry je = jf.getJarEntry(path);
                    if (je == null) {
                        continue;
                    }
                    is = jf.getInputStream(je);
                } else if (roots[i] instanceof ZipFile) {
                    ZipFile zf = (ZipFile) roots[i];
                    ZipEntry ze = zf.getEntry(path);
                    if (ze == null) {
                        continue;
                    }
                    is = zf.getInputStream(ze);
                } else if (roots[i] instanceof File) {
                    File f = new File((File) roots[i], path);
                    if (!f.exists()) {
                        continue;
                    }
                    is = new FileInputStream(f);
                }

                byte[] classBytes = new byte[is.available()];
                is.read(classBytes);
                is.close();
                definePackage(name);
                return defineClass(name, classBytes, 0, classBytes.length);

            } catch (Exception e) {
                System.out.println("exc: " + e.getMessage());
            }
        }
        /*
        finally
        {
        if ( null != fi )
        {
        try
        {
        fi.close();
        }
        catch (Exception e){}
        }
        }
         */
        // We could not find the class, so indicate the problem with an exception
        throw new ClassNotFoundException(name);

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
     * @throws java.io.IOException 
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
        if (null == manifest || getPackage(pkgName) != null) {
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

    public static void main(String[] args) {
        SimpleDirectoriesAndJarsClassLoader jdjcl = new SimpleDirectoriesAndJarsClassLoader(
                new String[]{
            "/Users/Mike/Desktop/externClasses",
            "/tmp/simkit.jar"
        });

        try {
            jdjcl.findClass("simkit.util.Misc");
            jdjcl.findClass("mv4302.Customer");

            JarFile jf = new JarFile(args[0]);
            Enumeration en = jf.entries();
            while (en.hasMoreElements()) {
                Object o = en.nextElement();
                System.out.println(o.toString());
            }
            JarEntry je = jf.getJarEntry("simkit/util/Misc.class");
            System.out.println(je.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
