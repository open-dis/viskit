package viskit;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 8, 2004
 * Time: 8:31:41 AM
 */
/*
 * From A. Buss
 */

import java.lang.reflect.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;

import actions.*;
import simkit.util.*;
import viskit.model.ConstructorArgument;

/**
 * Create a tabbed pane for instantiating objects of a given
 * class.  Each tab will contain a panel based on one
 * of the class's constructors.
 *
 * @author Arnold Buss
 */
public class InstantiationPanel extends JTabbedPane implements ActionListener,CaretListener
{

  private Class clazz;
  private Constructor[] construct;
  private ConstructorPanel[] constructorPanels;

  private String noParamString = "(no parameters)";
  private ImageIcon checkMark;
  /**
   * Create an instance of the Panel for a given class
   */
  private ActionListener modifiedListener;
  public InstantiationPanel(Class theClass, ActionListener changedListener)
  {
    modifiedListener = changedListener;
    checkMark = new ImageIcon(ClassLoader.getSystemResource("viskit/images/checkMark.png"));
    clazz = theClass;
    construct = clazz.getConstructors();
    constructorPanels = new ConstructorPanel[construct.length];

    for (int i = 0; i < construct.length; ++i) {
      constructorPanels[i] = new ConstructorPanel(construct[i],this,this);
      //cp.addPropertyChangeListener(new SimplePropertyDumper());
      String sign = ConstructorPanel.getSignature(construct[i].getParameterTypes());

      if(sign.equals("()"))
        sign = noParamString;

      addTab(""+i,null,constructorPanels[i],sign);
    }
    actionPerformed(null);    // set icon for initially selected pane
  }

  public void actionPerformed(ActionEvent e)
  {
    int idx = getSelectedIndex();
    for(int i=0;i<this.getTabCount();i++) {
      if(i == idx) {
        setIconAt(i,checkMark);
        constructorPanels[i].setBorder(BorderFactory.createLineBorder(Color.red));
      }
      else {
       setIconAt(i,null);
        constructorPanels[i].setBorder(null);
      }
    }
    if(modifiedListener != null)
      modifiedListener.actionPerformed(null);
  }

  public void caretUpdate(CaretEvent e)
  {
    if(modifiedListener != null)
      modifiedListener.actionPerformed(null);
  }

  /**
   * Idea here is to find the tab which matches the signature of the incoming data
   * @param data
   */
  public void setData(ArrayList data)
  {
    int datasz = data.size();
    if(datasz == 0)
      return;     // new node
    lp:
    for (int i=0;i<construct.length;i++) {
      Constructor cn = construct[i];
      if(cn.getParameterTypes().length == datasz) {   // check param types
        for(int j=0;j<datasz;j++) {
          ConstructorArgument arg = (ConstructorArgument)data.get(j);
          if(!arg.getType().equals(cn.getParameterTypes()[j].getName()))
            continue lp;
        }
        // here if we got a match
        constructorPanels[i].setData(data);
        setSelectedIndex(i);
        return;
      }
    }
    //assert false : "Should not get this error, InstantiationPanel";
    System.err.println("Should not get this error, InstantiationPanel");
  }
  public ArrayList getData()
  {
    return constructorPanels[getSelectedIndex()].getData();
  }
  /**
   * Simple test of InstantiationPanel.  Create one and
   * put it in a JFrame (MyFrame actually).
   *
   * @param args Fully-qualified name oif class.  If empty, default
   *             class is used
   * @throws Throwable Quick & dirty - cheesy, but effective
   */
  public static void main(String[] args) throws Throwable
  {
    try {
      UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
    }
    catch (Exception e) {
      System.err.println("Could not enable laf");
    }

    String className = args.length > 0 ? args[0] : "javax.swing.JTable"; //"simkit.stat.SimpleStatsTally";
    Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
    MyFrame frame = new MyFrame("Constructor Test: " + clazz.getName());

    JTabbedPane panel = new InstantiationPanel(clazz,null);

    frame.getContentPane().add(panel);
    frame.pack();
    frame.setVisible(true);
  }

}