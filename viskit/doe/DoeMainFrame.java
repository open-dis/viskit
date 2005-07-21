/*
Copyright (c) 1995-2005 held by the author(s).  All rights reserved.

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
      (http://www.nps.edu and http://www.MovesInstitute.org)
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
 * @since Jul 20, 2005
 * @since 10:36:33 AM
 */

package viskit.doe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DoeMainFrame extends JFrame implements DoeEvents
{
  DoeController controller;
  JScrollPane leftJsp;
  JScrollPane rightJsp;
  JPanel leftP, rightP;
  JSplitPane split;

  public DoeMainFrame(DoeController controller)
  {
    setTitle("Simkit/Viskit/Gridkit  Experiment Design");
    this.controller = controller;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myWlistener());

    leftP = new JPanel(new BorderLayout());
    rightP = new JPanel(new BorderLayout());
    split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,true,leftP,rightP);
    getContentPane().setLayout(new BorderLayout());
    //getContentPane().add(split,BorderLayout.CENTER);
  }
  public void setModel(DoeFileModel dfm)
  {

    leftJsp = new JScrollPane(dfm.paramTable); //dfm.paramTree);
    leftJsp.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,10,10,10),new EtchedBorder()));
/*
    rightJsp = new JScrollPane(new JLabel("tbd"));
    leftP.removeAll();
    leftP.add(leftJsp,BorderLayout.CENTER);
    rightP.removeAll();
    rightP.add(rightJsp,BorderLayout.CENTER);
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        split.setDividerLocation(0.5d);
      }
    });
*/
    getContentPane().add(leftJsp,BorderLayout.CENTER);
    getContentPane().validate();

  }
  class myWlistener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          controller.actionPerformed(EXIT_APP);
        }
      });
    }
  }
}