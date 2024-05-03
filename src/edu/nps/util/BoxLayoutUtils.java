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
package edu.nps.util;

import javax.swing.*;
import java.awt.*;

/**
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Dec 16, 2004
 * Time: 3:38:56 PM
 */

public class BoxLayoutUtils
{
  // Methods to adjust a component's size limits based on its preferred size

  static public void clampHeight(JComponent c)
  {
    clampHeight(c,c);
  }
  static public void clampWidth(JComponent c)
  {
    clampWidth(c,c);
  }

  // Methods to adjust a component's size limits base on the preferred
  // size of another component

  static public void clampHeight(JComponent c, JComponent source)
  {
    clampMinHeight(c,source);
    clampMaxHeight(c,source);
  }
  static public void clampMaxHeight(JComponent c, JComponent source)
  {
    Dimension s = source.getPreferredSize();
    Dimension mx = c.getMaximumSize();
    c.setMaximumSize(new Dimension(mx.width,s.height));
  }
  static public void clampMinHeight(JComponent c, JComponent source)
  {
    Dimension s = source.getPreferredSize();
    Dimension mn = c.getMinimumSize();
    c.setMinimumSize(new Dimension(mn.width,s.height));
  }
  static public void clampWidth(JComponent c, JComponent source)
  {
    clampMinWidth(c,source);
    clampMaxWidth(c,source);
  }
  static public void clampMaxWidth(JComponent c, JComponent source)
  {
    Dimension s = source.getPreferredSize();
    Dimension mx = c.getMaximumSize();
    c.setMaximumSize(new Dimension(s.width,mx.height));
  }
  static public void clampMinWidth(JComponent c, JComponent source)
  {
    Dimension s = source.getPreferredSize();
    Dimension mn = c.getMinimumSize();
    c.setMinimumSize(new Dimension(s.width,mn.height));
  }

  // This group clamps both dimensions, in contrast to the above methods which
  // don't touch the unspecified dimension

  static public void clampSize(JComponent c)
  {
    clampSize(c,c);
  }
  static public void clampMinSize(JComponent c)
  {
    clampMinSize(c,c);
  }
  static public void clampMaxSize(JComponent c)
  {
    clampMaxSize(c,c);
  }

  static public void clampSize(JComponent c, JComponent source)
  {
    clampMinSize(c,source);
    clampMaxSize(c,source);
  }
  static public void clampMinSize(JComponent c, JComponent source)
  {
    Dimension d = source.getPreferredSize();
    c.setMinimumSize(new Dimension(d));
  }
  static public void clampMaxSize(JComponent c, JComponent source)
  {
    Dimension d = source.getPreferredSize();
    c.setMaximumSize(new Dimension(d));
  }
}
