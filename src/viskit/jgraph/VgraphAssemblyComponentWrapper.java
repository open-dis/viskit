/*
 * Program:      Visual Discrete Event Simulation (DES) Toolkit (Viskit)
 *
 * Author(s):    Terry Norbraten
 *               http://www.nps.edu and http://www.movesinstitute.org
 *
 * Created:      22 AUG 2008
 *
 * Filename:     vGraphAssemblyComponentWrapper.java
 *
 * Compiler:     JDK1.6
 * O/S:          Windows XP Home Ed. (SP2)
 *
 * Description:  A class to serve as the jgraph object, while carrying other
 *               objects needed for the gui
 *
 * References:   see viskit.VgraphComponentWrapper
 *
 * URL:
 *
 * Requirements: 1)
 *
 * Assumptions:  1)
 *
 * TODO:
 *
 * Copyright (c) 1995-2008 held by the author(s).  All rights reserved.
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
package viskit.jgraph;

import javax.swing.JSplitPane;
import viskit.model.AssemblyModel;
import viskit.view.AssemblyViewFrame;

/**
 * A class to serve as the jgraph object, while carrying other objects needed
 * for the gui
 *
 * @version $Id:$
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     22 AUG 2008
 *     Time:     1758Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=viskit.VgraphAssemblyComponentWrapper">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 *
 *   </b></pre>
 *
 * @author <a href="mailto:tdnorbra@nps.edu">Terry Norbraten</a>
 */
public class VgraphAssemblyComponentWrapper extends vGraphAssemblyComponent {

    public AssemblyModel assyModel;
    public JSplitPane drawingSplitPane;
    public JSplitPane trees;
    public boolean isActive = true;

    public VgraphAssemblyComponentWrapper(vGraphAssemblyModel model, AssemblyViewFrame frame) {
        super(model, frame);
    }

} // end class file VgraphAssemblyComponentWrapper.java
