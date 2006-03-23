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
 * @since Sep 19, 2005
 * @since 2:00:51 PM
 */
package viskit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * A dialog class that lets the user add a new parameter to the document.
 * After the user clicks "OK", "Cancel", or closes the dialog via the
 * close box, the caller retrieves the "buttonChosen" variable from
 * the object to determine the choice. If the user clicked "OK", the
 * caller can retrieve various choices from the object.
 *
 * @author DMcG
 */

public class RecentFilesDialog extends JDialog
{
  private static RecentFilesDialog dialog;

  private final Collection lis;
  private JList jlist;
  private JButton closeButt;
  private Color defaultColor;
  private MouseListener myRollOverHandler = new mHandler();
  
  public static String showDialog(JFrame f, Component comp, Collection lis)
  {
    if(dialog == null)
      dialog = new RecentFilesDialog(f,comp,lis);
    else
      dialog.setParams(comp,lis);

    dialog.selection=null;
    dialog.setVisible(true); // blocks here
    return dialog.selection;
  }

  String selection;
  private RecentFilesDialog(JFrame parent, Component comp, Collection lis)
  {
    super(parent, "Recent files", true);
    this.lis = lis;
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setUndecorated(true);

    JPanel cont = new JPanel();
    setContentPane(cont);
    cont.setLayout(new BoxLayout(cont,BoxLayout.Y_AXIS));
    closeButt = new JButton("X");
    defaultColor = closeButt.getForeground();
    closeButt.setFocusable(false);
    closeButt.setBorder(new EmptyBorder(2,2,2,2)); //null); //new LineBorder(Color.gray,1));
    closeButt.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        RecentFilesDialog.this.setVisible(false);
      }
    });
    closeButt.addMouseListener(myRollOverHandler);
    JButton clearButt = new JButton("clear");
    clearButt.setFocusable(false);
    clearButt.setBorder(new EmptyBorder(2,2,2,2));
    clearButt.addActionListener(myClearer=new clearAction(lis));
    clearButt.addMouseListener(myRollOverHandler);
    
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
    p.add(new JLabel("Recent Eventgraphs"));
    p.add(Box.createHorizontalGlue());
    p.add(clearButt);
    p.add(Box.createHorizontalStrut(5));
    p.add(closeButt);
    cont.add(p);

    jlist = new JList();
    jlist.setBorder(new EmptyBorder(0,3,0,3));
    jlist.addMouseListener(new myMM());
    cont.add(jlist);
    cont.setBorder(new LineBorder(Color.black,1));
    setParams(comp,lis);
  }
  
  clearAction myClearer;
  class clearAction implements ActionListener
  {
    Collection nlis;
    clearAction(Collection lis)
    {
      this.nlis = lis;
    }
    public void actionPerformed(ActionEvent e)
    {
      nlis.clear();
      closeButt.doClick();
    }    
  }
  public void setParams(Component c, Collection lis)
  {
    myClearer.nlis = lis;

    fillWidgets();

    this.setLocationRelativeTo(c);
  }

  private void fillWidgets()
  {
    jlist.removeAll();
    jlist.setListData(lis.toArray());
    jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jlist.clearSelection();
    jlist.requestFocus();
    pack();
  }

  private void unloadWidgets()
  {
  }

  class myMM extends MouseAdapter
  {
    public void mouseClicked(MouseEvent e)
    {

      if(jlist.getSelectedIndex() != -1)
        selection = (String)jlist.getSelectedValue();
      else
        selection = null;
      if(e.getClickCount() > 1) {
        RecentFilesDialog.this.setVisible(false);
      }
    }
  }
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

  class mHandler extends MouseAdapter
  {
    public void mouseExited(MouseEvent e) {
      JButton butt = (JButton)e.getSource();
      butt.setForeground(defaultColor);
    }

    public void mouseEntered(MouseEvent e) {
     JButton butt = (JButton)e.getSource();
      butt.setForeground(Color.red);
 
    }
    
  }
}


