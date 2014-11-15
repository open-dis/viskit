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
package viskit.doe;

import viskit.util.TitleListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jul 20, 2005
 * @since 10:36:33 AM
 * @version $Id$
 */
public class DoeMainFrame extends JFrame implements DoeEvents {

    DoeController controller;
    JScrollPane leftJsp;
    JScrollPane rightJsp;
    JPanel leftP, rightP;
    JSplitPane split;
    JComponent content;
    boolean contentOnly;
    public String titleString = "Simkit/Viskit/Gridkit Experiment Design";

    public DoeMainFrame(boolean contentOnly, DoeController controller) {
        setTitle(titleString);
        this.contentOnly = contentOnly;
        this.controller = controller;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new myWlistener());

        leftP = new JPanel(new BorderLayout());

        content = new JPanel();
        content.setLayout(new BorderLayout());

        JLabel lab = new JLabel("This is initialized when an assembly is opened.");
        lab.setHorizontalAlignment(JLabel.CENTER);
        content.add(lab, BorderLayout.NORTH);
        getContentPane().setLayout(new BorderLayout());
    }
    private DoeFileModel dfm;

    public void setModel(DoeFileModel dfm) {
        this.dfm = dfm;
        if (dfm == null) {
            doTitle("");
        } else {
            dfm.paramTable.setBorder(new EtchedBorder()); // double etched with below
            leftJsp = new JScrollPane(dfm.paramTable); //dfm.paramTree);
            leftJsp.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 5, 10), new EtchedBorder()));
            doTitle(dfm.userFile.getName());
        }
    }

    public void removeContent() {
        content.removeAll();
    }

    public void installContent() {
        removeContent();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        if (leftJsp != null) {
            content.add(leftJsp); //, BorderLayout.CENTER);
            JButton sv = new JButton("Save");
            sv.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.saveDoeParams();
                }
            });
            sv.setToolTipText("<html><center>Save experiment parameters<br>to assembly file<br>" +
                    "(not required to run job)");
            sv.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 0, 10, 10), sv.getBorder()));
            JPanel moveRight = new JPanel();
            moveRight.setLayout(new BoxLayout(moveRight, BoxLayout.X_AXIS));
            moveRight.add(Box.createHorizontalGlue());
            moveRight.add(sv);
            content.add(moveRight);
            content.validate();
        }
        if (!contentOnly) {
            getContentPane().add(content, BorderLayout.CENTER);
            getContentPane().validate();
        }
    }

    public DoeFileModel getModel() {
        return dfm;
    }

    public DoeController getController() {
        return controller;
    }

    public JComponent getContent() {
        return content;
    }

    class myWlistener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    controller.actionPerformed(EXIT_APP);
                }
            });
        }
    }
    private String namePrefix = "Viskit Design of Experiments";
    private String currentTitle = namePrefix;

    private void doTitle(String nm) {
        if (nm != null && nm.length() > 0) {
            currentTitle = namePrefix + ": " + nm;
        }

        if (titlLis != null) {
            titlLis.setTitle(currentTitle, titlLisIdx);
        }
    }
    TitleListener titlLis;
    int titlLisIdx;

    public void setTitleListener(TitleListener tLis, int idx) {
        titlLis = tLis;
        titlLisIdx = idx;
        doTitle(null);
    }
}
