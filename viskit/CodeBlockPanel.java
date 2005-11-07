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
 * @since Nov 3, 2005
 * @since 4:06:07 PM
 */

package viskit;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

public class CodeBlockPanel extends JPanel
{
  private JTextComponent jtf;
  private Window owner;
  private String title;

  public CodeBlockPanel(Window owner, boolean multilined, String title)
  {
    this.owner = owner;
    this.title = title;
    setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
    setOpaque(false);
    if(multilined)
      jtf = new myJTextArea();
    else
      jtf = new myJTextField("");

    jtf.setOpaque(true);
    jtf.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    jtf.setToolTipText("bogus");

    jtf.addKeyListener(new KeyListener()
    {
      public void keyTyped(KeyEvent e)
      {
        if(updateListener != null)
          updateListener.actionPerformed(new ActionEvent(jtf.getText(),0,""));
      }

      public void keyPressed(KeyEvent e)
      {
      }

      public void keyReleased(KeyEvent e)
      {
      }
    });


    add(jtf);
    add(Box.createHorizontalStrut(3));
    if(!multilined) {
      Dimension d = getPreferredSize();
      d.width = Integer.MAX_VALUE;
      setMaximumSize(d);
    }
    JButton editButt = new JButton(" ... ");
    editButt.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    editButt.setToolTipText("Click to edit a long code block");
    Dimension dd = new Dimension(editButt.getPreferredSize());
    if(!multilined)
      dd.height = getPreferredSize().height;
    editButt.setMaximumSize(dd);
    add(editButt);

    editButt.addActionListener(new buttListener());
  }
  private ActionListener updateListener;
  public void addUpdateListener(ActionListener lis)
  {
    updateListener = lis;
  }

  public String getData()
  {
    String s = jtf.getText();
    if(s == null)
      s = "";
    return s;
  }

  public void setData(String s)
  {
    jtf.setText(s);
  }

  class buttListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      StringBuffer sb = new StringBuffer(jtf.getText().trim());
      boolean modded;
      if(owner instanceof JDialog)
        modded = TextAreaDialog.showTitledDialog(title,(JDialog)owner,owner,sb);
      else
        modded = TextAreaDialog.showTitledDialog(title,(JFrame)owner,(JFrame)owner,sb);

      if(modded) {
        jtf.setText(sb.toString().trim());
        jtf.setCaretPosition(0);
        if(updateListener != null)
          updateListener.actionPerformed(new ActionEvent(jtf.getText(),0,""));
      }
    }
  }
  class myJTextArea extends JTextArea
  {
    public myJTextArea()
    {
      super();
      setPreferredSize(new Dimension(50,50));
    }
    public String getToolTipText(MouseEvent event)
    {
      if (myMultiLineTextString == null || myMultiLineTextString.trim().length() <= 0)
        return null;
      return "<html><pre>"+myMultiLineTextString;
    }

    String myMultiLineTextString;
    public void setText(String t)
    {
      super.setText(t);
      myMultiLineTextString = t;
    }

  }
  class myJTextField extends JTextField
  {
    public myJTextField(String s)
    {
      super(s);
    }

    public String getToolTipText(MouseEvent event)
    {
      if (myMultiLineTextString == null || myMultiLineTextString.trim().length() <= 0)
        return null;
      return "<html><pre>"+myMultiLineTextString;
    }

    String myMultiLineTextString;
    public void setText(String t)
    {
      super.setText(t);
      myMultiLineTextString = t;
    }
  }

}