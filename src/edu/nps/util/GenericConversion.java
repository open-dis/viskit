/*
 * Program:      Viskit Discrete Event Simulation (DES) Tool
 *
 * Author(s):    Terry Norbraten
 *               http://www.nps.edu and http://www.movesinstitute.org
 *
 * Created:      07 DEC 07
 *
 * Filename:     GenericConversion.java
 *
 * Compiler:     JDK1.6
 * O/S:          Windows XP Home Ed. (SP2)
 *
 * Description:  Converting A Collection To An Array
 *
 * References:   http://javasymposium.techtarget.com/images/TSSJS_E_Presentations/Naftalin_Maurice_EffectiveGenerics.pdf
 *
 * URL:
 *
 * Requirements: 1) JDK1.5+
 *
 * Assumptions:  1)
 *
 * TODO:
 *
 * Copyright (c) 1995-2011 held by the author(s).  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer
 *       in the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the names of the Naval Postgraduate School (NPS)
 *       Modeling Virtual Environments and Simulation (MOVES) Institute
 *       (http://www.nps.edu and http://www.movesinstitute.org)
 *       nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.nps.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import viskit.xsd.bindings.eventgraph.Parameter;

/**
 * Converter for a Collection of Type to an Array of Type using reification
 * @version $Id$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     07 DEC 07
 *     Time:     0701Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.GenericConversion">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 *
 *     Date:     12 DEC 07
 *     Time:     0106Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=edu.nps.util.GenericConversion">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Created method to take a Collection of Type and fill a
 *                  Collection Array of Type
 *   </b></pre>
 *
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class GenericConversion {

    /** Method to convert a "Collection" to an array.  Will warn of an
     * "unchecked cast," but this is a known cast to us, therefore permissible.
     * @param <T> the type to cast this array
     * @param c the Collection of Type to convert
     * @param a the Array Type to convert the Collection into
     * @return an Array Type with contents from a given Collection
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<T> c, T[] a) {
        if (a.length < c.size()) {

            // known unchecked cast
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), c.size());
        }
        int i = 0;
        for (T t : c) {
            a[i++] = t;
        }
        if (i < a.length) {
            a[i] = null;
        }
        return a;
    }

    /** Method to return a generic List&lt;Object&gt; array initialized based
     * on runtime type information.  Will warn of an unchecked cast which, in
     * this case, is permissible
     * @param <T> the type to cast this array
     * @param type a Class&lt;? extends T&gt; type
     * @param length the desired length of the type array
     * @throws IllegalArgumentException if a primitive class type is given
     * @return a initialized List&lt;Object&gt;[] of length length
     */
    public static <T> List<Object>[] newListObjectTypeArray(Class<? extends T> type, int length) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Argument cannot be primitive: " + type);
        }
        @SuppressWarnings("unchecked")
        // known unchecked cast
        List<Object>[] lis = (List<Object>[]) Array.newInstance(type, length);
        for (int ix = 0; ix < lis.length; ix++) {
            lis[ix] = new ArrayList<>();
        }
        return lis;
    }

    /** Method to return a generic List&lt;Parameter&gt; array initialized based
     * on runtime type information.  Will warn of an unchecked cast which, in
     * this case, is permissible
     * @param <T> the type to cast this array
     * @param type a Class&lt;? extends T&gt; type
     * @param length the desired length of the type array
     * @throws IllegalArgumentException if a primitive class type is given
     * @return a initialized List&lt;Object&gt;[] of length length
     */
    public static <T> List<Parameter>[] newListParameterTypeArray(Class<? extends T> type, int length) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Argument cannot be primitive: " + type);
        }
        @SuppressWarnings("unchecked")
        // known unchecked cast
        List<Parameter>[] lis = (List<Parameter>[]) Array.newInstance(type, length);
        for (int ix = 0; ix < lis.length; ix++) {
            lis[ix] = new ArrayList<>();
        }
        return lis;
    }

} // end class file GenericConversion.java
