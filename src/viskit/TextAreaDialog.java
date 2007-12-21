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
 * @since Nov 4, 2005
 * @since 9:54:45 AM
 */


package viskit;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TextAreaDialog extends JDialog
{
  private JTextArea commentArea;          // Text field that holds the comment

  private static TextAreaDialog dialog;
  private static boolean modified = false;
  private JButton okButt, canButt;

  public static StringBuffer newComment;
  public StringBuffer param;
  private static String title = "";

  public static boolean showDialog(Window owner, Component comp, StringBuffer parm)
  {
    if (owner instanceof JFrame)
      dialog = new TextAreaDialog((JFrame) owner, comp, parm);
    else
      dialog = new TextAreaDialog((JDialog) owner, comp, parm);

    dialog.setVisible(true);
    // above call blocks
    return modified;
  }

  public static boolean showTitledDialog(String titl, Window owner, Component comp, StringBuffer parm)
  {
    if (dialog != null)
      dialog.setTitle(titl);

    title = titl;
    return showDialog(owner, comp, parm);
  }

  private TextAreaDialog(JDialog owner, Component comp, StringBuffer param)
  {
    super(owner, title, true);
    common(comp,param);
  }
  private TextAreaDialog(JFrame owner, Component comp, StringBuffer param)
  {
    super(owner, title, true);
    common(comp,param);
  }
  private void common(Component comp,StringBuffer param)
  {
    this.param = param;
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new myCloseListener());

    JPanel cont = new JPanel();
    setContentPane(cont);

    cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
    cont.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    commentArea = new JTextArea(10, 40);
    commentArea.setLineWrap(true);           // This needs to be true to ease formatting issues in tooltips
    commentArea.setWrapStyleWord(true);

    JScrollPane jsp = new JScrollPane(commentArea);
    cont.add(jsp);
    cont.add(Box.createVerticalStrut(5));

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    canButt = new JButton("Cancel");
    okButt = new JButton("Apply changes");
    buttPan.add(Box.createHorizontalGlue());     // takes up space when dialog is expanded horizontally
    buttPan.add(canButt);
    buttPan.add(okButt);
    cont.add(buttPan);

    fillWidgets();     // put the data into the widgets

    modified = (param == null);     // if it's a new myEA, they can always accept defaults with no typing
    okButt.setEnabled((param == null));

    getRootPane().setDefaultButton(canButt);

    pack();     // do this prior to next
    this.setLocationRelativeTo(comp);

    // attach listeners
    canButt.addActionListener(new cancelButtonListener());
    okButt .addActionListener(new applyButtonListener());

    enableApplyButtonListener lis = new enableApplyButtonListener();
    this.commentArea.      addCaretListener(lis);
  }

  public void setParams(Component c, StringBuffer p)
  {
    param = p;

    fillWidgets();

    modified = (p.length() == 0);
    okButt.setEnabled((p.length() == 0));

    getRootPane().setDefaultButton(canButt);

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    if (param != null)
      commentArea.setText(param.toString());
    else
      commentArea.setText("");
  }

  private void unloadWidgets()
  {
    if (param != null) {
      param.setLength(0);
      param.append(commentArea.getText().trim());
    }
    else {
      newComment = new StringBuffer(commentArea.getText().trim());
    }
  }

  class cancelButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      modified = false;    // for the caller
      setVisible(false);
    }
  }

  class applyButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      if (modified)
        unloadWidgets();
      setVisible(false);
    }
  }

  class enableApplyButtonListener implements CaretListener, ActionListener
  {
    public void caretUpdate(CaretEvent event)
    {
      modified = true;
      okButt.setEnabled(true);
      getRootPane().setDefaultButton(okButt);
    }

    public void actionPerformed(ActionEvent event)
    {
      caretUpdate(null);
    }
  }

/*
  class OneLinePanel extends JPanel
  {
    OneLinePanel(JLabel lab, int w, JComponent comp)
    {
      setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
      add(Box.createHorizontalStrut(5));
      add(Box.createHorizontalStrut(w-lab.getPreferredSize().width));
      add(lab);
      add(Box.createHorizontalStrut(5));
      add(comp);

      Dimension d = getPreferredSize();
      d.width = Integer.MAX_VALUE;
      setMaximumSize(d);
    }
  }
*/

  class myCloseListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      if (modified) {
        int ret = JOptionPane.showConfirmDialog(TextAreaDialog.this, "Apply changes?",
            "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION)
          okButt.doClick();
        else
          canButt.doClick();
      }
      else
        canButt.doClick();
    }
  }


}


