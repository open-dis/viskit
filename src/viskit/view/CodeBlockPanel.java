/*
Copyright (c) 1995-2016 held by the author(s).  All rights reserved.

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
package viskit.view;

import viskit.view.dialog.TextAreaDialog;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;


/** Supports literal Java code snippets to be written to an Event method
 *
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Nov 3, 2005
 * @since 4:06:07 PM
 * @version $Id$
 */
public class CodeBlockPanel extends JPanel {

    private JTextComponent codeBlockTA; // myJTextArea or myJTextField
    private Window  owner;
    private String  title;
    private JButton editButton;
    private static final String TOOLTIP = "Advanced feature to insert Java methods, import statements, etc.  Avoid using code blocks!";

    public CodeBlockPanel(Window owner, boolean multipleLines, String title) {
        this.owner = owner; // hunt for the guilty to affix blame and punishment...
        this.title = title;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        if (multipleLines) {
            codeBlockTA = new myJTextArea();
        } else {
            codeBlockTA = new myJTextField("");
        }
        codeBlockTA.setOpaque(true);
        codeBlockTA.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        codeBlockTA.setToolTipText(TOOLTIP);

        codeBlockTA.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (updateListener != null) {
                    updateListener.actionPerformed(new ActionEvent(codeBlockTA.getText(), 0, ""));
                }
            }
        });

        add(codeBlockTA);
        add(Box.createHorizontalStrut(3));
        if (!multipleLines) {
            Dimension d = getPreferredSize();
            d.width = Integer.MAX_VALUE;
            setMaximumSize(d);
        }
        editButton = new JButton(" ... ");
        editButton.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        editButton.setToolTipText("Click to edit a long code block");
        Dimension dd = new Dimension(editButton.getPreferredSize());
        if (!multipleLines) {
            dd.height = getPreferredSize().height;
        }
        editButton.setMaximumSize(dd);
        add(editButton);

        editButton.addActionListener(new buttListener());
    }

    /**
     * setVisibleLines sets the preferredSize of the codeblock panel to borders plus number of lines specified
     * @param n the number of lines desired
     */
    public void setVisibleLines(int n) {
        if (codeBlockTA instanceof JTextArea) {
            ((JTextArea) codeBlockTA).setRows(n);
            Dimension d = new Dimension(codeBlockTA.getPreferredScrollableViewportSize());
            int ph = Math.max(d.height, editButton.getPreferredSize().height);
            ph += getInsets().top + getInsets().bottom;
            setPreferredSize(new Dimension(getPreferredSize().width, ph));
            invalidate();
        }
    }
    private ActionListener updateListener;

    public void addUpdateListener(ActionListener lis) {
        updateListener = lis;
    }

    public String getData() {
        String s = codeBlockTA.getText();
        return (s == null) ? "" : s;
    }

    public void setData(String newText) {
        codeBlockTA.setText(newText);
    }

    class buttListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            StringBuffer sb = new StringBuffer(codeBlockTA.getText().trim());
            boolean modded;
            if (owner instanceof JDialog) {
                modded = TextAreaDialog.showTitledDialog(title, owner, sb);
            } else {
                modded = TextAreaDialog.showTitledDialog(title, owner, sb);
            }
            if (modded) {
                codeBlockTA.setText(sb.toString().trim());
                codeBlockTA.setCaretPosition(0);
                if (updateListener != null) {
                    updateListener.actionPerformed(new ActionEvent(codeBlockTA.getText(), 0, ""));
                }
            }
        }
    }

    class myJTextArea extends JTextArea implements DocumentListener {

        public myJTextArea() {
            super();
            setPreferredSize(new Dimension(50, 50));
            getDocument().addDocumentListener(myJTextArea.this);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return "<html><pre>" + TOOLTIP + "</pre></html>";
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            changedUpdate(documentEvent);
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            changedUpdate(documentEvent);
        }
    }

    class myJTextField extends JTextField implements DocumentListener {

        public myJTextField(String s) {
            super(s);
            getDocument().addDocumentListener(myJTextField.this);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return "<html><pre>" + TOOLTIP + "</pre></html>";
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            if (updateListener != null) {
                updateListener.actionPerformed(new ActionEvent(getText(), 0, ""));
            }
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            changedUpdate(documentEvent);
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            changedUpdate(documentEvent);
        }
    }
}