package viskit;

import viskit.model.VInstantiator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 8, 2004
 * Time: 8:31:41 AM
 */

public class InstantiationPanel extends JPanel implements ActionListener, CaretListener
{
  private JLabel typeLab, methodLab;
  private JTextField typeTF;
  private JComboBox methodCB;

  private static final int FF=0,CONSTR=1,FACT=2,ARR=10;

  private JPanel instPane;
  private CardLayout instPaneLayMgr;
  //private ArrayPanel arrPan;
  private FFPanel ffPan;
  private ConstrPanel conPan;
  private FactoryPanel factPan;

  private ActionListener modifiedListener;

  public InstantiationPanel(ActionListener changedListener)
  {
    modifiedListener = changedListener;
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

    JPanel topP = new JPanel(new SpringLayout());
    typeLab = new JLabel("type",JLabel.TRAILING);
    typeTF = new JTextField();
    typeTF.setEditable(false);
    typeLab.setLabelFor(typeTF);

    methodLab = new JLabel("method",JLabel.TRAILING);
    methodCB = new JComboBox(new String[]{"free form","constructor","factory"});
    methodLab.setLabelFor(methodCB);

    topP.add(typeLab);
    topP.add(typeTF);
    topP.add(methodLab);
    topP.add(methodCB);

    SpringUtilities.makeCompactGrid(topP,2,2,10,10,5,5);
    add(topP);

    instPane = new JPanel();
    instPaneLayMgr = new CardLayout();
    instPane.setLayout(instPaneLayMgr);

    //instPane.setPreferredSize(new Dimension(150,100));
    instPane.setBorder(BorderFactory.createEtchedBorder());
    instPane.setAlignmentX(Box.CENTER_ALIGNMENT);

    //arrPan   = new ArrayPanel(this);
    ffPan    = new FFPanel(this);
    conPan   = new ConstrPanel(this);
    factPan  = new FactoryPanel(this);

/*
    instPaneLayMgr.addLayoutComponent(arrPan,"arrPan");
    instPaneLayMgr.addLayoutComponent(ffPan,"ffPan");
    instPaneLayMgr.addLayoutComponent(conPan,"conPan");
    instPaneLayMgr.addLayoutComponent(factPan,"factPan");
    //false advertising
*/
    //instPane.add(arrPan,"arrPan");
    instPane.add(ffPan,"ffPan");
    instPane.add(conPan,"conPan");
    instPane.add(factPan,"factPan");

    add(Box.createVerticalStrut(5));
    add(instPane);

    methodCB.addActionListener(new ActionListener()
    {
      int lastIdx = 0;
      public void actionPerformed(ActionEvent e)
      {
        int idx = methodCB.getSelectedIndex();
        if(lastIdx != idx)
          if(modifiedListener != null)
            modifiedListener.actionPerformed(new ActionEvent(methodCB,0,"modified"));
        switch(idx)
        {
        case FF:
            instPaneLayMgr.show(instPane,"ffPan");
            ffPan.value.requestFocus();
            ffPan.value.selectAll();
            break;
        case CONSTR:
            instPaneLayMgr.show(instPane,"conPan");
            break;
        case FACT:
            instPaneLayMgr.show(instPane,"factPan");
            factPan.factClassTF.requestFocus();
            factPan.factClassTF.selectAll();
            break;
        default:
            System.err.println("bad data Instantiation panel");
        }
      }
    });
  }

  VInstantiator getData()
  {
    //VInstantiator vi = null;
    switch(methodCB.getSelectedIndex())
    {
    case FF:
      return ffPan.getData();
    case CONSTR:
      return conPan.getData();
    case FACT:
      return factPan.getData();
    default:
      System.err.println("bad data Inst. panel getData()");
      return null;
    }
/*
    myVcon.getInstantiators().clear();
    myVcon.getInstantiators().add(vi);
    return myVcon;
*/
  }
  VInstantiator myVi;
  public void setData(VInstantiator vi)
  {
    myVi = vi.vcopy();
    //myVcon = vc.vcopy();
    //String typ = myVcon.getType();
    String typ = vi.getType();
    typeTF.setText(typ);

/*   We don't get here...arrays are not handled by the IP
    // Array is special
    Class c = Vstatics.ClassForName(typ);
    if(c != null && c.isArray()) {
      arrPan.setType(typ);
      arrPan.setData(myVi); //myVcon);
      instPaneLayMgr.show(instPane,"arrPan");

    }
*/
    // inform all panels of the type of the object
    conPan.setType(typ);
    factPan.setType(typ);
    ffPan.setType(typ);

    // pass the real data to the panel which reflects the current data
    //List lis = myVcon.getInstantiators();
    //List lis = vi.getInstantiators();


/*
    switch(lis.size())
    {
    case 1:
      Object o = lis.get(0);
      if(o instanceof VInstantiator.Constr) {
        conPan.setData((VInstantiator.Constr)o);
        methodCB.setSelectedIndex(CONSTR);
      }
      else if(o instanceof VInstantiator.Factory) {
        factPan.setData((VInstantiator.Factory)o);
        methodCB.setSelectedIndex(FACT);
      }
      else if(o instanceof VInstantiator.FreeF) {
        ffPan.setData((VInstantiator.FreeF)o);
        methodCB.setSelectedIndex(FF);
      }
      break;

    case 0:
      methodCB.setSelectedIndex(FF);
      break;
    default:  // > 0
      methodLab.setVisible(false);
      methodCB.setVisible(false);
      break;
    }
*/

    // do differently
    if(vi instanceof VInstantiator.Constr) {
      conPan.setData((VInstantiator.Constr)vi);
      methodCB.setSelectedIndex(CONSTR);
    }
    else if(vi instanceof VInstantiator.Factory) {
      factPan.setData((VInstantiator.Factory)vi);
      methodCB.setSelectedIndex(FACT);
    }
    else if(vi instanceof VInstantiator.FreeF) {
      ffPan.setData((VInstantiator.FreeF)vi);
      methodCB.setSelectedIndex(FF);
    }
    else {
      //assert false: "Internal error InstantianPanel.setData()"
      System.err.println("Internal error InstantiationPanel.setData()");
    }

  }
  public void actionPerformed(ActionEvent e)
  {
    if(modifiedListener != null)
      modifiedListener.actionPerformed(null);
  }

  public void caretUpdate(CaretEvent e)
  {
    actionPerformed(null);
  }

  class FFPanel extends JPanel
  {
    private JTextField value;
    public FFPanel(InstantiationPanel ip)
    {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      value = new JTextField("");
      value.setAlignmentX(Box.CENTER_ALIGNMENT);
      Vstatics.clampHeight(value);

      add(value);
      add(Box.createVerticalGlue());
    }
    public void setData(VInstantiator.FreeF viff)
    {
      if(viff == null)
        return;
      value.setText(viff.getValue());
    }
    String typ;
    public void setType(String typ)
    {
      this.typ = typ;
    }
    public VInstantiator getData()
    {
      return new VInstantiator.FreeF(typ,value.getText().trim());
    }
  }

  class ConstrPanel extends JPanel implements ActionListener,CaretListener
  {
    private Class clazz;
    private JTabbedPane tp;
    private Constructor[] construct;
    private ConstructorPanel[] constructorPanels;
    //private ObjListPanel[] constructPanels;     // *

    private String noParamString = "(no parameters)";
    private ImageIcon checkMark;
    private InstantiationPanel ip;

    public ConstrPanel(InstantiationPanel ip)
    {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      this.ip = ip;
      tp = new JTabbedPane();
      checkMark = new ImageIcon(ClassLoader.getSystemResource("viskit/images/checkMark.png"));

      //setup(className);         don't init in constructor
    }
    String typ;
    public void setType(String clName)
    {
      typ = clName;
      removeAll();
      tp.removeAll();
      //modifiedListener = changedListener;
      try {
        clazz = Class.forName(clName);
        construct = clazz.getConstructors();
        constructorPanels = new ConstructorPanel[construct.length];
       // constructPanels   = new ObjListPanel[construct.length];     // *
      }
      catch (ClassNotFoundException e) {
      }

      if (construct == null || construct.length <= 0) {
        // here if their is no way to directly build an object of this class.
        tp.addTab("0", null, new JLabel("Abstract class of some kind.  Use free-form construction."));
      }
      else {
        for (int i = 0; i < construct.length; ++i) {
          //constructorPanels[i] = new ConstructorPanel(construct[i], this, this, construct.length != 1);
          constructorPanels[i] = new ConstructorPanel(this,construct.length != 1,this);
          constructorPanels[i].setData(buildDummyInstantiators(construct[i]));
          String sign = ConstructorPanel.getSignature(construct[i].getParameterTypes());

          if (construct[i].getParameterTypes().length == 0)
            sign = noParamString;

          tp.addTab("" + i, null, constructorPanels[i], sign);
        }
      }
      add(tp);

      actionPerformed(null);    // set icon for initially selected pane
    }
    private List buildDummyInstantiators(Constructor con)
    {
      Vector v = new Vector();
      Class[] cs = con.getParameterTypes();
      for(int i=0;i<cs.length;i++) {
        if(cs[i].isArray()) {
          VInstantiator.Array va = new VInstantiator.Array(Vstatics.convertClassName(cs[i].getName()),
                                                           new Vector());
          v.add(va);
        }
        else {
          VInstantiator.FreeF vff = new VInstantiator.FreeF(Vstatics.convertClassName(cs[i].getName()),
                                                            "");
          v.add(vff);
        }
      }
      return v;
    }

    public void caretUpdate(CaretEvent e)
    {
      ip.caretUpdate(e);
    }

    public void actionPerformed(ActionEvent e)
    {
      int idx = tp.getSelectedIndex();
      if(construct == null ||construct.length <= 0)               // some classes have no constructors
        return;

      for(int i=0;i<tp.getTabCount();i++) {
        if(i == idx) {
          tp.setIconAt(i,checkMark);
          constructorPanels[i].setBorder(BorderFactory.createLineBorder(Color.red));
          constructorPanels[i].setSelected(true);
        }
        else {
          tp.setIconAt(i,null);
          constructorPanels[i].setBorder(null);
          constructorPanels[i].setSelected(false);
        }
      }
      // tell mommy
      ip.actionPerformed(e);
    }
    public void setData(VInstantiator.Constr vi)
    {
      if(vi == null)
        return;
      List args = vi.getArgs();
      for(int i = 0;i<constructorPanels.length;i++) {    //give to everyone...they must cho
        Constructor con = construct[i];
        Class[] params = con.getParameterTypes();
        noMatch:
        {
          for(int j=0;j<params.length;j++) {
            if(j >= args.size())
              break noMatch;
            VInstantiator argVi = (VInstantiator)args.get(j);
            if(! Vstatics.convertClassName(params[j].getName()).equals(argVi.getType())) {
              break noMatch;
            }
          }
          // here if we matched all the arguments to the constructor
          constructorPanels[i].setData(args);
          tp.setSelectedIndex(i);
          actionPerformed(null);
          return;

        } // nomatch
      } // next Constructor
      //assert false: "Internal error ConstrPanel.setData";
      System.err.println("assert false: \"Internal error ConstrPanel.setData\"");
    }

    public VInstantiator getData()
    {
      ConstructorPanel cp = (ConstructorPanel)tp.getSelectedComponent();
      return new VInstantiator.Constr(typ,cp.getData());
    }
  }


  class FactoryPanel extends JPanel
  {
    private InstantiationPanel ip;
    private JLabel factClassLab, factMethodLab;
    private JTextField factClassTF, factMethodTF;
    private JButton factMethodButt;
    private JPanel topP;
    private ObjListPanel olp;

    public FactoryPanel(InstantiationPanel ip)
    {
      this.ip = ip;
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      topP = new JPanel(new SpringLayout());
      factClassLab = new JLabel("Factory class",JLabel.TRAILING);
      factClassTF = new JTextField();
      Vstatics.clampHeight(factClassTF);
      factClassLab.setLabelFor(factClassTF);

      JLabel dummy = new JLabel("");
      JLabel classHelp = new JLabel("(Press return after entering class name)");
      classHelp.setFont(factClassTF.getFont());
      dummy.setLabelFor(classHelp);

      factMethodLab = new JLabel("Class method",JLabel.TRAILING);
      factMethodTF  = new JTextField();
      Vstatics.clampHeight(factMethodTF);
      factMethodLab.setLabelFor(factMethodTF);
        JPanel tinyP = new JPanel();
        tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
        tinyP.add(factMethodTF);
        factMethodButt = new JButton("...");
        factMethodButt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        Vstatics.clampSize(factMethodButt, factMethodTF, factMethodButt);
        tinyP.add(factMethodButt);
      topP.add(factClassLab);
      topP.add(factClassTF);
      topP.add(dummy);
      topP.add(classHelp);
      topP.add(factMethodLab);
      topP.add(tinyP);
      SpringUtilities.makeCompactGrid(topP,3,2,5,5,5,5);

      add(topP);

      factClassTF.addActionListener(new MyClassListener());
    }
    class MyClassListener implements ActionListener
    {
      public void actionPerformed(ActionEvent e)
      {
        Class c = null;
        String cName = factClassTF.getText().trim();
        try {
          c = Class.forName(cName);
        }
        catch (ClassNotFoundException e1) {
          JOptionPane.showMessageDialog(ip,cName + " class not found");
          factClassTF.requestFocus();
          factClassTF.selectAll();
          return;
        }

        Method[] statMeths = c.getMethods();
        if(statMeths == null || statMeths.length <= 0) {
          JOptionPane.showMessageDialog(ip,cName + " contains no methods");
          factClassTF.requestFocus();
          factClassTF.selectAll();
          return;
        }
        //Vector v = new Vector();
        Vector vn = new Vector();
        HashMap hm = new HashMap();
// test

/*
try {
myObjClass = Class.forName("simkit.random.RandomVariate");
}
catch (ClassNotFoundException e1) {
e1.printStackTrace();
}
*/

        for(int i=0;i<statMeths.length;i++) {
          int mods = statMeths[i].getModifiers();
          Class retCl = statMeths[i].getReturnType();
          if(Modifier.isStatic(mods))
            if(retCl == myObjClass){
              hm.put(statMeths[i].toString(),statMeths[i]);
              vn.add(statMeths[i].toString());
            }
        }
        if(vn.size() <= 0) {
          JOptionPane.showMessageDialog(ip,"<html><center>"+cName + " contains no static methods<br>returning "+typ+".");
          factClassTF.requestFocus();
          factClassTF.selectAll();
          return;
        }
        String[] ms = new String[0];
        ms =(String[])vn.toArray(ms);
        Object ret = JOptionPane.showInputDialog(ip,"Choose method","Factory methods",JOptionPane.PLAIN_MESSAGE,null,
              ms,ms[0]);
        if(ret == null) {
          factClassTF.requestFocus();
          factClassTF.selectAll();
          return;
        }

        Method m = (Method)hm.get(ret);
        factMethodTF.setText(m.getName());
        factMethodTF.setEnabled(true);
        factMethodLab.setEnabled(true);
        factMethodButt.setEnabled(true);
        Class[] pc = m.getParameterTypes();
        Vector vc = new Vector();
        for(int i=0;i<pc.length;i++) {
          if(pc[i].isArray())
            vc.add(new VInstantiator.Array(Vstatics.convertClassName(pc[i].getName()),new ArrayList()));
          else
            vc.add(new VInstantiator.FreeF(Vstatics.convertClassName(pc[i].getName()),""));
        }

        olp = new ObjListPanel(ip);
        olp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                        "Method arguments",TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION));

        olp.setData(vc,true);
        add(olp);

        add(Box.createVerticalGlue());

        olp.revalidate();   // required?
      }
    }

    String typ;
    Class myObjClass;
    public void setType(String clName)
    {
      typ = clName;
      myObjClass = Vstatics.ClassForName(typ);
      if (myObjClass == null) {
        System.err.println("can find class for " + typ);
        return;
      }
      factMethodLab.setEnabled(false);
      factMethodTF.setEnabled(false);
      factMethodButt.setEnabled(false);
    }

    public void setData(VInstantiator.Factory vi)
    {
      if(vi == null)
        return;

      removeAll();
      factClassTF.setText(vi.getFactoryClass());
      factMethodTF.setText(vi.getMethod());
      add(topP);

      if(vi.getParams().size() <= 0) {
        JLabel tempLab = new JLabel("no arguments to chosen factory method",JLabel.CENTER);
        add(tempLab);
        add(Box.createVerticalGlue());
        return;
      }
      olp = new ObjListPanel(ip);
      olp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                      "Method arguments",TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION));

      olp.setData(vi.getParams(),true);
      add(olp);

      add(Box.createVerticalGlue());
      revalidate();
    }

    public VInstantiator getData()
    {
      return new VInstantiator.Factory(typ,factClassTF.getText().trim(),
                                         factMethodTF.getText().trim(),
                                         olp.getData());
    }
  }

/*
  class ArrayPanel extends JPanel
  {
    private JLabel[] label;
    private JTextField[] field;
    private InstantiationPanel ip;
    private JLabel typeLab;
    private JTextField typeTF;
    private JPanel typePan;

    public ArrayPanel(InstantiationPanel ip)
    {
      this.ip = ip;
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      typePan = new JPanel(new SpringLayout());
      typeLab = new JLabel("Array type",JLabel.TRAILING);
      typeTF = new JTextField();
      typeLab.setLabelFor(typeTF);
      typePan.add(typeLab);
      typePan.add(typeTF);
      SpringUtilities.makeCompactGrid(typePan,1,2,5,5,5,5);
    }
    public void setType(String clName)
    {
      //doLayout();
    }
    public void setData(VConstructor vc)
    {
      if(vc == null)
        return;

      removeAll();
      ip.methodCB.setVisible(false);      // whoa!
      ip.methodLab.setVisible(false);
      typeTF.setText(vc.getType());
      add(typePan);
      add(Box.createVerticalStrut(5));

      ObjListPanel olp = new ObjListPanel(ip);
      olp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                      "Array elements",TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION));

      olp.setData(vc.getInstantiators(),false);

      add(olp);

      add(Box.createVerticalGlue());
      revalidate();
    }
  }
*/

}