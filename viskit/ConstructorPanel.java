package viskit;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 8, 2004
 * Time: 8:33:17 AM
 */

import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.CaretListener;

import simkit.random.RandomVariate;
import simkit.random.RandomVariateFactory;
import viskit.model.ConstructorArgument;

/**
 * Swing panel for instantiating objects from a given Constructor.
 * The user inputs (currently) are just Strings.
 * //TODO: a more generic and robust way to pass data to constructors.
 *
 * @author Arnold Buss
 */

public class ConstructorPanel extends JPanel
{

  private Constructor constructor;
  private Class[] signature;
  private JTextField[] field;
  private JLabel[] label;
  private RandomVariateFromString rvfs;
  private JButton selectButt;

  public ConstructorPanel(Constructor construct, ActionListener selectListener, CaretListener modifiedListener)
  {
    rvfs = new RandomVariateFromString();

    constructor = construct;
    signature = constructor.getParameterTypes();
    field = new JTextField[signature.length];
    label = new JLabel[signature.length];

    JPanel innerP = null;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(Box.createVerticalGlue());
    if (signature.length > 0) {
      innerP = new JPanel(new SpringLayout());
      innerP.setBorder(BorderFactory.createEtchedBorder());
      innerP.setLayout(new SpringLayout());
      for (int i = 0; i < label.length; ++i) {
        label[i] = new JLabel(convertClassName(signature[i].getName()), JLabel.TRAILING);
        innerP.add(label[i]);
        field[i] = new JTextField(10);
        if (modifiedListener != null)
          field[i].addCaretListener(modifiedListener);
        clampHeight(field[i]);
        // Do the extended constructor game
        // Terminal Param, MultiParam, Factoryparm
        Class c = null;
        c = signature[i];

        if (c != null) {
          if (!c.isPrimitive() || c.isArray()) {
            JPanel tinyP = new JPanel();
            tinyP.setLayout(new BoxLayout(tinyP, BoxLayout.X_AXIS));
            tinyP.add(field[i]);
            JButton b = new JButton("...");
            b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
            clampSize(b, field[i], b);
            tinyP.add(b);
            innerP.add(tinyP);
            label[i].setLabelFor(tinyP);
            if (c.isArray())
              b.setToolTipText("Edit with array wizard");
            else
              b.setToolTipText("Edit with new object wizard");
          }
          else {
            innerP.add(field[i]);
            label[i].setLabelFor(field[i]);
          }
        }
      }
      SpringUtilities.makeCompactGrid(innerP, signature.length, 2, 5, 5, 5, 5);
    }
    else {
      innerP = new JPanel(new BorderLayout());
      innerP.add(new JLabel("<no parameters>", JLabel.CENTER), BorderLayout.CENTER);
    }
    add(innerP);
    add(Box.createVerticalStrut(5));
    add(Box.createVerticalGlue());

    JPanel buttPan = new JPanel();
    buttPan.setLayout(new BoxLayout(buttPan, BoxLayout.X_AXIS));
    buttPan.add(Box.createHorizontalGlue());
    selectButt = new JButton("Select this constructor");
    buttPan.add(selectButt);
    buttPan.add(Box.createHorizontalGlue());

    add(buttPan);
    add(Box.createVerticalStrut(5));

    if (selectListener != null)
      selectButt.addActionListener(selectListener);

    setSelected(false);
  }

  String convertClassName(String s)
  {
    if(s.charAt(0) != '[')
      return s;

    int dim = 0;
    StringBuffer sb = new StringBuffer();
    for(int i=0;i<s.length();i++) {
      if(s.charAt(i) == '['){
        dim ++;
        sb.append("[]");
      }
      else
        break;
    }

    String brackets = sb.toString();

    char ty = s.charAt(dim);
    s = s.substring(dim+1);
    switch(ty)
    {
      case 'Z': return "boolean" + brackets;
      case 'B': return "byte" + brackets;
      case 'C': return "char" + brackets;
      case 'L': return s.substring(0,s.length()-1) + brackets;  // lose the ;
      case 'D': return "double" + brackets;
      case 'F': return "float" + brackets;
      case 'I': return "int" + brackets;
      case 'J': return "long" + brackets;
      case 'S': return "short" + brackets;
      default:
        return "bad parse";
    }
  }
  void clampSize(JComponent c, JComponent h, JComponent w)
  {
    Dimension d = new Dimension(h.getPreferredSize().height,w.getPreferredSize().width);
    c.setMaximumSize(d);
    c.setMinimumSize(d);
  }
  void clampHeight(JComponent comp)
  {
    Dimension d = comp.getPreferredSize();
    comp.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
    comp.setMinimumSize(new Dimension(Integer.MAX_VALUE,d.height));
  }
  public void setSelected(boolean tf)
  {
    for(int i=0;i<field.length;i++) {
      field[i].setEnabled(tf);
      label[i].setEnabled(tf);
    }
  }
  public void setData(ArrayList arl)
  {
    for(int i=0;i<arl.size();i++) {
      ConstructorArgument ca = (ConstructorArgument)arl.get(i);
      field[i].setText(ca.getValue());
    }
  }
  public ArrayList getData()
  {
    ArrayList retAL = new ArrayList(field.length);
    for(int i=0;i<field.length;i++) {
      ConstructorArgument ca = new ConstructorArgument();
      ca.setType(label[i].getText());
      ca.setValue(field[i].getText().trim());
      retAL.add(i,ca);
    }
    return retAL;
  }
  /**
   * Create an instance of the given object from user input
   *
   * @return new instance of given object from input
   * @throws Throwable Whatever exceptions are thrown during instantiation
   */
  public Object instantiate() throws Throwable
  {
    Object[] vals = new Object[field.length];
    for (int i = 0; i < vals.length; ++i) {
      if (signature[i] == java.lang.String.class) {
        vals[i] = field[i].getText();
      }
      else if (signature[i] == int.class || java.lang.Integer.class.isAssignableFrom(signature[i])) {
        vals[i] = new Integer(field[i].getText());
      }
      else if (RandomVariate.class.isAssignableFrom(signature[i])) {
        vals[i] = rvfs.newInstance(field[i].getText());
      }
    }
    return constructor.newInstance(vals);
  }

  /**
   * Create a new instance - this to be used as "Action" method
   */
  public void newInstance()
  {
    try {
      Object obj = instantiate();
      firePropertyChange("newInstance", null, obj);
//            System.out.println(obj);
    }
    catch (Throwable t) {
      t.printStackTrace(System.err);
    }
  }

  /**
   * @param clazz Class[] array, tyoically from constructor signature
   * @return String identifying Class's signature
   */
  public static String getSignature(Class[] clazz)
  {
    StringBuffer buf = new StringBuffer("(");
    for (int i = 0; i < clazz.length; ++i) {
      buf.append(clazz[i].getName());
      if (i < clazz.length - 1) {
        buf.append(',');
      }
    }
    buf.append(')');
    return buf.toString();
  }

}

interface ObjectFromString
{
  public Object newInstance(String inputString) throws InstantiationException;
}

class RandomVariateFromString implements ObjectFromString
{
  public Object newInstance(String inputString) throws InstantiationException
  {
    Object newInstance = null;
    String[] parsedInput = inputString.split(" ");
    try {
      String name = parsedInput[0];
      Object[] params = new Object[parsedInput.length - 1];
      for (int i = 1; i < parsedInput.length; ++i) {
        params[i - 1] = new Double(parsedInput[i]);
      }
      newInstance = RandomVariateFactory.getInstance(name, params);
    }
    catch (Throwable t) {
      throw new IllegalArgumentException("Bad input string: " + inputString);
    }
    return newInstance;
  }
}
