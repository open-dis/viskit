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
 * @since Sep 22, 2005
 * @since 3:25:11 PM
 */

package viskit;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class EventGraphAssemblyComboMainFrame extends JFrame
{
  JTabbedPane tabbedPane;
  EventGraphViewFrame egFrame;
  AssemblyViewFrame asyFrame;
  InternalAssemblyRunner asyRunComponent;
  Action myQuitAction;

  public EventGraphAssemblyComboMainFrame(String[] args)
  {
    super("Viskit");

    initUI(args);

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation((d.width - 800) / 2, (d.height - 600) / 2);
    this.setSize(800, 600);

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        myQuitAction.actionPerformed(null);
      }
    });
    ImageIcon icon = new ImageIcon(ClassLoader.getSystemResource("viskit/images/ViskitSplash2.png"));
    this.setIconImage(icon.getImage());

  }

  ArrayList menus = new ArrayList();

  private void initUI(String[] args)
  {
    VGlobals.instance().setAssemblyQuitHandler(null);
    VGlobals.instance().setEventGraphQuitHandler(null);
    JMenu helpMenu;
    JMenuBar menuBar;

    tabbedPane = new JTabbedPane();
    myQuitAction = new QuitAction("Quit");

    egFrame = VGlobals.instance().initEventGraphViewFrame(true);
    tabbedPane.add("Event Graphs",egFrame.getContent());   // 0
    menuBar = egFrame.getMenus();
    menus.add(menuBar);
    doCommonHelp(menuBar);
    egFrame.setTitleListener(myTitleListener,0);
    setJMenuBar(menuBar);
    jamQuitHandler(egFrame.getQuitMenuItem(),myQuitAction,egFrame.getMenus());

    asyFrame = VGlobals.instance().initAssemblyViewFrame(true);
    tabbedPane.add("Assembly",asyFrame.getContent());  //1
    menuBar = asyFrame.getMenus();
    menus.add(menuBar);
    doCommonHelp(menuBar);
    asyFrame.setTitleListener(myTitleListener,1);
    jamQuitHandler(asyFrame.getQuitMenuItem(),myQuitAction,asyFrame.getMenus());

    asyRunComponent = new InternalAssemblyRunner();
    tabbedPane.add("Run Assembly",asyRunComponent.getContent());   // 2
    menuBar = asyRunComponent.getMenus();
    menus.add(menuBar);
    doCommonHelp(menuBar);
    asyRunComponent.setTitleListener(myTitleListener,2);
    jamQuitHandler(asyRunComponent.getQuitMenuItem(),myQuitAction,asyRunComponent.getMenus());
    ((AssemblyController)asyFrame.getController()).setAssemblyRunner( new ThisAssemblyRunnerPlug());

/*
    //DoeMain doeMain = DoeMain.main2();
    //DoeMainFrame doeFrame = doeMain.getMainFrame();
    //tabbedPane.add("Design of Experiments",doeFrame.getContent());
    //menus.add(doeMain.getMenus());

    JLabel junk =  new JLabel("Design of Experiments placeholder");
    tabbedPane.add("Design of Experiments",junk);
    menus.add(makeDummyMbar("File"));

    junk =  new JLabel("Cluster Controller placeholder");
    tabbedPane.add("Cluster Controller",junk);
    menus.add(makeDummyMbar("File"));
*/

    // Now setup the assembly file change listeners
    ViskitAssemblyController asyCntlr = (ViskitAssemblyController)asyFrame.getController();

    asyCntlr.addAssemblyFileListener(asyRunComponent);
    //todo asyCntlr.addAssemblyFileListener(doeFrame.getController());
    //todo asyCntlr.addAssemblyFileListener(runGridComponent.getController());

    // Now setup the open-event graph listener(s)
    ViskitController cntl = (ViskitController)egFrame.getController();
    cntl.addOpenEventGraphListener(asyCntlr.getOpenEventGraphListener());

    // Start the controllers
    ((AssemblyController)asyFrame.getController()).begin();
    ((ViskitController)egFrame.getController()).begin();

    // Swing:
    getContentPane().add(tabbedPane);
    tabbedPane.addChangeListener(new myTabChangeListener());
  }
  private JMenuBar makeDummyMbar(String s)
  {
    JMenuBar mb = new JMenuBar();
    mb.add(new JMenu(s));
    return mb;
  }

  class myTabChangeListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e)
    {
      getJMenuBar().remove(hmen);
      int i = tabbedPane.getSelectedIndex();
      JMenuBar newMb = (JMenuBar)menus.get(i);
      newMb.add(hmen);
      setJMenuBar(newMb); //(JMenuBar)menus.get(i));
      setTitle((String)titles[i]);
    }
  }

  private JMenu hmen;
  /**
   * Stick the first Help menu we see into all the following ones.
   * @param mb
   */
  private void doCommonHelp(JMenuBar mb)
  {
    for(int i=0;i<mb.getMenuCount();i++) {
      JMenu men = mb.getMenu(i);
      if(men.getText().equalsIgnoreCase("Help")) {
        if(hmen == null)
          hmen = men;
        else
          mb.remove(i);
        return;
      }
    }
  }

  private void jamQuitHandler(JMenuItem mi, Action qa, JMenuBar mb)
  {
    if(mi==null) {
      JMenu m = mb.getMenu(0); // first menu
      if(m == null) {
        m = new JMenu("File");
        mb.add(m);
      }
      m.addSeparator();
      mi = new JMenuItem("Quit");
      m.add(mi);
    }

    ActionListener[] al = mi.getActionListeners();
    for(int i=0; i<al.length;i++)
      mi.removeActionListener(al[i]);

    mi.setAction(qa);
  }
  class QuitAction extends AbstractAction
  {
    public QuitAction(String s)
    {
      super(s);
    }

    public void actionPerformed(ActionEvent e)
    {
      tabbedPane.setSelectedIndex(0); // eg
      ((Controller)egFrame.getController()).quit();

      tabbedPane.setSelectedIndex(1); // assy ed
      ((AssemblyController)asyFrame.getController()).quit();

      //todo others
      System.exit(0);
    }
  }
  class ThisAssemblyRunnerPlug implements AssemblyRunnerPlug
  {
    public void exec(String[] execStrings, int runnerClassIndex)
    {
      /** The default version of this does a RuntimeExex("java"....) to spawn a new
       * VM.  We want to run the assembly in a new VM, but not the GUI.
       */
      tabbedPane.setSelectedIndex(2);
      asyRunComponent.initParams(execStrings,runnerClassIndex);
    }
  }

  String[] titles = new String[]{"","","","","","","","","",""};
  TitleListener myTitleListener = new myTitleListener();
  class myTitleListener implements TitleListener
  {
    public void setTitle(String title, int key)
    {
      titles[key] = title;
      if(tabbedPane.getSelectedIndex() == key)
        EventGraphAssemblyComboMainFrame.this.setTitle(title);
    }
  }
}