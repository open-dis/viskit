/*
Copyright (c) 1995-2015 held by the author(s).  All rights reserved.

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
package viskit.view.dialog;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Convenience class to create a one line JPanel for inclusion as a module for
 * other complex panel
 *
 * @author <a href="mailto:tdnorbra@nps.edu?subject=viskit.view.dialog.OneLinePanel">Terry Norbraten, NPS MOVES</a>
 * @version $Id:$
 */
public class OneLinePanel extends JPanel {

    public final static String OPEN_LABEL_BOLD = "<html><p><b>";
    public final static String CLOSE_LABEL_BOLD = "</b></p></html>";


    /** Default constructor for a OneLinePanel
     *
     * @param lab the label for this panel
     * @param w a parameter to set the label in the center
     * @param comp a JComponent to add to this panel
     */
    public OneLinePanel(JLabel lab, int w, JComponent comp) {
        this(lab, w, comp, null);
    }

    /** Secondary constructor for a OneLinePanel
     *
     * @param lab the label for this panel
     * @param w a parameter to set the label in the center
     * @param comp1 a JComponent to add to this panel
     * @param comp2 a second JComponent to add to this panel
     */
    public OneLinePanel(JLabel lab, int w, JComponent comp1, JComponent comp2) {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalStrut(5));

        if (lab != null) {

            if (w >= lab.getPreferredSize().width)
                add(Box.createHorizontalStrut(w - lab.getPreferredSize().width));

            add(lab);
            add(Box.createHorizontalStrut(5));
        }

        add(comp1);

        if (comp2 != null) {
            add(Box.createHorizontalStrut(5));
            add(comp2);
        }

        Dimension d = getPreferredSize();
        d.width = Integer.MAX_VALUE;
        setMaximumSize(d);
    }

    public static int maxWidth(JComponent[] c) {
        int tmpw, maxw = 0;
        for (JComponent jc : c) {
            tmpw = jc.getPreferredSize().width;
            if (tmpw > maxw) {
                maxw = tmpw;
            }
        }
        return maxw;
    }

} // end class file OneLinePanel.java
