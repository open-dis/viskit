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
 * @since Mar 16, 2006
 * @since 12:58:14 PM
 */

package viskit.doe;

import org.apache.xmlrpc.XmlRpcClientLite;
import viskit.xsd.assembly.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class ClusterAdminDialog extends JDialog implements ActionListener
{
  private JButton closeButt;
  private JButton addUserButt;
  private JButton delUserButt;
  private JButton chgPwButt;
  private JButton loginButt;

  private JTextField unameTF;
  private JPasswordField unamePF;
  private JTextField adminNameTF;
  private JPasswordField adminPF;

  private JTextField resultsTF;

  private XmlRpcClientLite xmlrpc;
  private String adminUsrID;

  private String clusterName;
  private int port;
  private JLabel unameLab;
  private JLabel pwLab;
  private JLabel resultsLab;
  private Color defaultResultsColor;

  public static void showDialog(String cluster, int port, JFrame parent, Component locComp)
  {
    new ClusterAdminDialog(cluster, port, parent, locComp).setVisible(true);
  }

  public static void showDialog(String cluster, int port, JDialog parent, Component locComp)
  {
    new ClusterAdminDialog(cluster, port, parent, locComp).setVisible(true);
  }

  private ClusterAdminDialog(String cluster, int port, JDialog parent, Component locComp)
  {
    super(parent,cluster+":"+port,true);
    commonConstructor(cluster,port,locComp);
  }
  private ClusterAdminDialog(String cluster, int port, JFrame parent, Component locComp)
  {
    super(parent,cluster+":"+port,true);
    commonConstructor(cluster,port,locComp);
  }

  private void commonConstructor(String cluster, int port, Component locComp)
  {
    clusterName = cluster;
    this.port = port;

    JPanel c = new JPanel();
    c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));

    JPanel loginP = new JPanel();
    loginP.setLayout(new BoxLayout(loginP,BoxLayout.X_AXIS));
    JLabel logNamLab = new JLabel("admin user name");
    loginP.add(logNamLab);
    loginP.add(Box.createHorizontalStrut(5));
    adminNameTF = new JTextField(10);
    loginP.add(adminNameTF);
    loginP.add(Box.createHorizontalStrut(5));
    JLabel logPwLab = new JLabel("password");
    loginP.add(logPwLab);
    loginP.add(Box.createHorizontalStrut(5));
    adminPF = new JPasswordField(10);
    loginP.add(adminPF);
    loginP.add(Box.createHorizontalStrut(10));
    loginButt = new JButton("login");
    loginP.add(loginButt);

    JPanel actionPanel = new JPanel();
    actionPanel.setLayout(new BoxLayout(actionPanel,BoxLayout.Y_AXIS));

      JPanel unameP = new JPanel();
        unameP.setLayout(new BoxLayout(unameP,BoxLayout.X_AXIS));
        unameLab = new JLabel("user name");
        unameP.add(unameLab);
        unameP.add(Box.createHorizontalStrut(5));
        unameTF = new JTextField(10);
        unameP.add(unameTF);
        unameP.add(Box.createHorizontalStrut(5));
        pwLab = new JLabel("password");
        unameP.add(pwLab);
        unameP.add(Box.createHorizontalStrut(5));
        unamePF = new JPasswordField(10);
      unameP.add(unamePF);

    actionPanel.add(unameP);
    actionPanel.add(Box.createVerticalStrut(5));
      JPanel actionButtPan = new JPanel();
      actionButtPan.setLayout(new BoxLayout(actionButtPan,BoxLayout.X_AXIS));
      actionButtPan.add(Box.createHorizontalGlue());
        addUserButt = new JButton("add user");
        actionButtPan.add(addUserButt);
        actionButtPan.add(Box.createHorizontalStrut(5));
        delUserButt = new JButton("remove user");
        actionButtPan.add(delUserButt);
        actionButtPan.add(Box.createHorizontalStrut(5));
        chgPwButt = new JButton("change password");
        actionButtPan.add(chgPwButt);
      actionButtPan.add(Box.createHorizontalGlue());

    actionPanel.add(actionButtPan);
    actionPanel.add(Box.createVerticalStrut(5));
      JPanel resultsPan = new JPanel();
      resultsPan.setLayout(new BoxLayout(resultsPan,BoxLayout.X_AXIS));
        resultsPan.add(Box.createHorizontalGlue());
        resultsLab = new JLabel("results");
        resultsPan.add(resultsLab);
        resultsPan.add(Box.createHorizontalStrut(5));
        resultsTF = new JTextField(10);
        defaultResultsColor = resultsTF.getForeground();
        resultsTF.setEditable(false);
        resultsPan.add(resultsTF);
        resultsPan.add(Box.createHorizontalGlue());

    actionPanel.add(resultsPan);
    actionPanel.add(Box.createVerticalStrut(5));
    actionPanel.setBorder(new TitledBorder("Actions"));
    actionPanel.setEnabled(false); //todo test

    closeButt = new JButton("Close");
    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan,BoxLayout.X_AXIS));
    buttPan.add(Box.createHorizontalGlue());
    buttPan.add(closeButt);

    c.add(loginP);
    c.add(Box.createVerticalStrut(5));
    c.add(actionPanel);
    c.add(Box.createVerticalStrut(5));
    c.add(buttPan);

    c.setBorder(new EmptyBorder(10,10,10,10));
    setContentPane(c);

    pack();
    setLocationRelativeTo(locComp);
    
    addHandlers();
    enableActions(false);
  }
  private void showResultsOrStatus(String s, boolean isError)
  {
    resultsTF.setText(s);
    resultsTF.setToolTipText(s);

    if(!isError)
      resultsTF.setForeground(defaultResultsColor);
    else
      resultsTF.setForeground(Color.red);
  }
  private void showResultsOrStatus_SWTHR(final String s, final boolean isError)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        showResultsOrStatus(s,isError);
      }
    });
  }
  private void addHandlers()
  {
    addUserButt.setActionCommand("add");
    delUserButt.setActionCommand("del");
    chgPwButt.setActionCommand("change");
    closeButt.setActionCommand("x");
    loginButt.setActionCommand("Login");

    addUserButt.addActionListener(this);
    delUserButt.addActionListener(this);
    chgPwButt.addActionListener(this);
    closeButt.addActionListener(this);
    loginButt.addActionListener(this);
  }

  private void enableActions_SWTHR(final boolean wh)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        enableActions(wh);
      }
    });
  }
  private void enableActions(boolean wh)
  {
    unameTF.setEnabled(wh);
    unamePF.setEnabled(wh);
    enableButtonActions(wh);
    unameLab.setEnabled(wh);
    pwLab.setEnabled(wh);
    resultsLab.setEnabled(wh);
  }
  private void enableButtonActions_SWTHR(final boolean wh)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        enableButtonActions(wh);
      }
    });
  }
  private void enableButtonActions(boolean wh)
  {
    addUserButt.setEnabled(wh);
    delUserButt.setEnabled(wh);
    chgPwButt.setEnabled(wh);
  }
  public void actionPerformed(ActionEvent e)
  {
    switch (e.getActionCommand().charAt(0)) {
      case 'a':
        addUser();
        break;
      case 'd':
        delUser();
        break;
      case 'c':
        chgPassword();
        break;
      case 'L':
        login();
        break;
      case 'x':
        close();
        break;
    }
  }
  private void enableButt(JButton butt,boolean wh)
  {
    butt.setEnabled(wh);
  }
  private void enableButt_SWTHR(final JButton butt, final boolean wh)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        enableButt(butt,wh);
      }
    });
  }
  private void close()
  {
    if(xmlrpc != null && adminUsrID != null) {
      Vector args = new Vector();
      args.add(adminUsrID);
      try {
        xmlrpc.execute("gridkit.logout",args);
      }
      catch (Exception e) {
        System.err.println("Error logging out from cluster.");
      }
    }
    ClusterAdminDialog.this.setVisible(false);
  }
  Thread execThread;
  private void runInThread(Runnable runner)
  {
    execThread = new Thread(runner);
    execThread.setPriority(Thread.NORM_PRIORITY);
    execThread.start();
  }

  private void login()
  {
    final String adminuname = adminNameTF.getText();
    final String pw    = new String(adminPF.getPassword());
    if(!checkUnameAndPword(adminuname,pw))
      return;

    enableButt(loginButt,false);
    showResultsOrStatus("Trying login to "+clusterName+":"+port,false);    //not error

    runInThread(new Runnable()
    {
      public void run()
      {
        Exception ex = null;
        try {
          xmlrpc = new XmlRpcClientLite(clusterName,port);
          Vector args = new Vector();
          args.add(adminuname);
          args.add(pw);
          String usrID = (String)xmlrpc.execute("gridkit.login",args);
          if(!usrID.equals(SessionManager.LOGIN_ERROR)) {
            ClusterAdminDialog.this.adminUsrID = usrID;
            showResultsOrStatus_SWTHR("Logged in.",false);
            enableActions_SWTHR(true);
            return;   // good return
          }
        }
        catch (Exception e) {
          ex = e;
        }
        //error;
        if (ex != null)
          showResultsOrStatus_SWTHR("Error ("+ex.getClass().getName()+") logging in.",true); //error
        else
          showResultsOrStatus_SWTHR("Login to "+clusterName+":"+port+" refused.",true);
        enableButt_SWTHR(loginButt,true);
        ClusterAdminDialog.this.adminUsrID = null;
      }
    });
  }

  private void addUser()
  {
    final String uname = unameTF.getText().trim();
    final String pw = new String(unamePF.getPassword());
    if(!checkUnameAndPword(uname,pw))
      return;
    showResultsOrStatus("Trying add user " + uname,false);
    enableButtonActions(false);
    runInThread(new Runnable()
    {
      public void run()
      {
        Exception ex = null;
        breakClause:
        {
          try {
            Vector args = new Vector();
            args.add(adminUsrID);
            args.add(uname);
            Boolean ret = (Boolean) xmlrpc.execute("gridkit.addUser", args);
            if (!ret.booleanValue())   // i.e., ret != true
              break breakClause;

            // We now have a user with a pw = uname; set the password to that specified
            args.clear();
            args.add(adminUsrID);
            args.add(uname);
            args.add(pw);
            ret = (Boolean) xmlrpc.execute("gridkit.changePassword", args);
            if (ret.booleanValue()) {   // i.e., ret == true
              showResultsOrStatus_SWTHR("User " + uname + " added.",false);
              enableButtonActions_SWTHR(true);
              return;   // good return
            }
          }
          catch (Exception e) {
            ex = e;
          }
        } // breakClause

        //error;
        if (ex != null)
          showResultsOrStatus_SWTHR("Error (" + ex.getMessage() + ") adding user " + uname,true);
        else
          showResultsOrStatus_SWTHR("Add user name " + uname + " denied.",true);

        enableButtonActions_SWTHR(true);
      }
    });
  }

  private void delUser()
  {
    final String uname = unameTF.getText().trim();
    final String pw = new String(unamePF.getPassword());
    if(!checkUnameAndPword(uname,pw))
      return;
    showResultsOrStatus("Trying delete user " + uname,false);
    enableButtonActions(false);

    runInThread(new Runnable()
    {
      public void run()
      {
        Exception ex = null;
        breakClause:
        {
          try {
            Vector args = new Vector();
            args.add(adminUsrID);
            args.add(uname);
            Boolean ret = (Boolean) xmlrpc.execute("gridkit.deleteUser", args);
            if (ret.booleanValue()) {   // i.e., ret == true
              showResultsOrStatus_SWTHR("User " + uname + " deleted.",false);
              enableButtonActions_SWTHR(true);
              return;   // good return
            }
          }
          catch (Exception e) {
            ex = e;
          }
        } // breakClause

        //error;
        if (ex != null)
          showResultsOrStatus_SWTHR("Error (" + ex.getMessage() + ") deleting user " + uname,true);
        else
          showResultsOrStatus_SWTHR("Delete user name " + uname + " denied.",true);

        enableButtonActions_SWTHR(true);
      }
    });

  }
  private void chgPassword()
  {
    final String uname = unameTF.getText().trim();
    final String pw = new String(unamePF.getPassword());
    if(!checkUnameAndPword(uname,pw))
      return;

    showResultsOrStatus("Trying change password for " + uname,false);
    enableButtonActions(false);

    runInThread(new Runnable()
    {
      public void run()
      {
        _chgPassword(uname,pw);
      }
    });
  }

  private void _chgPassword(String uname, String pw)
  {
    Exception ex = null;
    try {
      Vector args = new Vector();
      args.add(adminUsrID);
      args.add(uname);
      args.add(pw);
      Boolean ret = (Boolean) xmlrpc.execute("gridkit.changePassword", args);
      if (ret.booleanValue()) {   // i.e., ret == true
        showResultsOrStatus_SWTHR("User " + uname + " password changed.",false);
        enableButtonActions_SWTHR(true);
        return;   // good return
      }
    }
    catch (Exception e) {
      ex = e;
    }

    //error;
    if (ex != null)
      showResultsOrStatus_SWTHR("Error (" + ex.getMessage() + ") changing password for " + uname,true);
    else
      showResultsOrStatus_SWTHR("Change password for " + uname + " denied.",true);

    enableButtonActions_SWTHR(true);
  }

  private boolean checkUnameAndPword(String uname, String pw)
  {
    if (uname == null || uname.length() <= 0 || pw == null || pw.length() <= 0) {
      JOptionPane.showMessageDialog(this, "Name and password must be entered", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;
  }
}