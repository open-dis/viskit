package viskit.model;

import viskit.Vstatics;
import viskit.xsd.translator.SimkitXML2Java;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Jun 15, 2004
 * Time: 9:43:42 AM
 */

public abstract class VInstantiator
{
  private String type;
  private String name = "";
  public String getType()
  {
    return type;
  }
  public VInstantiator(String typ)
  {
    type = typ;
  }
  public void setName(String nm)
  {
    name = nm;
  }
  public String getName()
  {
    return name;
  }
  abstract public VInstantiator vcopy();
  abstract public boolean isValid();

  public static List buildDummyInstantiators(Constructor con)
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

  /***********************************************************************/
  public static class FreeF extends VInstantiator
  {
    private String value;

    public FreeF(String type, String value)
    {
      super(type);
      setValue(value);
    }
    public String getValue()            {return value;}
    public void   setValue(String value){this.value = value;}

    public String toString()
    {
      return value;
    }

    public VInstantiator vcopy()
    {
      VInstantiator rv = new VInstantiator.FreeF(getType(),getValue());
      rv.setName(this.getName());
      return rv;
    }

    public boolean isValid()
    {
      String t = getType();
      String v = getValue();
      return t != null & v != null & t.length()>0 && v.length()>0;
    }
  }

  /***********************************************************************/
  public static class Constr extends VInstantiator
  {
    private List args;

    public Constr(String type, List args)
    {
      super(type);
      setArgs(args);
      findArgNames(type,args);
    }
    public Constr(String type, List args, List names)
    {
      this(type,args);
      for(int i=0;i<args.size();i++) {
        ((VInstantiator)args.get(i)).setName((String)names.get(i));
      }
    }
    /**
     * Find the names of the arguments
     * @param type
     * @param args List of VInstantiators
     */
    private void findArgNames(String type, List args)
    {
      if(args == null) {
        setArgs(getDefaultArgs(type));
        args = getArgs();
      }
      if(!findArgNamesFromSourceMangle(type,args))
        findArgNamesFromBeanStuff(type,args);
    }

    private boolean findArgNamesFromSourceMangle(String type, List args)
    {
//      if(type.indexOf("DISPinger") != -1)
//        System.out.println("bp");
      // "Jam the names into the source at create-source-from-xml time" method
      // 1 get constructor list of this class
      // 2. get the one we want by matching against args
      // 3. compare the single vs. the list, if equal get the index within the list
      // 4. use that to index into names to set the names to the params.
      try {
        Class cls = Vstatics.classForName(type);
        Constructor[] ca = cls.getDeclaredConstructors();   // List of constructors

        Class[] argArr = new Class[args.size()];
        for(int i=0;i<args.size();i++)
          argArr[i] = Vstatics.classForName((String)((VInstantiator)args.get(i)).getType());
        Constructor c = cls.getDeclaredConstructor(argArr);  // that's the one we want
        int j;
        for(j=0;j<ca.length;j++) {
          if(ca[j].equals(c))
            break;
        }
        if(j>=ca.length)
          return false;  // found nothing

        Field fld = cls.getDeclaredField(SimkitXML2Java.constructorParmNamesID);
        String[][] nms = (String[][])fld.get(null);
        if(nms != null && nms.length>0) {
          for(int n=0;n<nms[j].length;n++)
            try {
              ((VInstantiator)args.get(n)).setName(nms[j][n]);
            }
            catch (Throwable e) {
              // if the names are manually editted, the author may have screwed up; don't crash
              if(args.size()>n) {
                ((VInstantiator)args.get(n)).setName("error, check source");
                return false;
              }
            }
        }
      }
      catch (Exception e) {
        //System.out.println("can't find constructor param names");
        return false;
      }
      return true;
    }

    private void findArgNamesFromBeanStuff(String type, List args)
    {
      // Alternate method
      // 1. The parameters to an event graph have both getters and setters.  State vars do not
      //

      try {
        Class cls = Vstatics.classForName(type);
        if(cls == null)
            return;

        Class[] argArr = new Class[args.size()];
        for(int i=0;i<args.size();i++)
          argArr[i] = Vstatics.classForName((String)((VInstantiator)args.get(i)).getType());
        Constructor c = cls.getDeclaredConstructor(argArr);  // that's the one we want

       //Introspector.flushCaches();
        BeanInfo bi = Introspector.getBeanInfo(cls,cls.getSuperclass());
        ArrayList parms = new ArrayList();
        PropertyDescriptor[] pd = bi.getPropertyDescriptors();
        for(int i=0;i<pd.length;i++) {
          if(pd[i].getReadMethod() != null &&
             pd[i].getWriteMethod() != null )
            parms.add(pd[i]);
        }
        if(unambiguousMatch(argArr,parms))
          setNames(args,parms);
      }
      catch (Throwable e) {
        //e.printStackTrace();
      }
    }

    private boolean unambiguousMatch(Class[]args, ArrayList propDesc)
    {
      // if can find unambiguous match by type, put propDesc into proper order
      if(args.length <= 0 || propDesc.size() <= 0)
        return false;
      Vector holder = new Vector();
      for(int i=0;i<args.length;i++) {
        holder.clear();
        Class c = args[i];
        for(int j=0;j<propDesc.size();j++) {
          PropertyDescriptor pd = (PropertyDescriptor)propDesc.get(j);
          if(typeMatch(c,pd))
            holder.add(new Integer(j));
        }
        if(holder.size() != 1)
          return false;
        int jj = ((Integer)holder.get(0)).intValue();
        // put pd at j into i
        Object i_ob = propDesc.get(i);
        propDesc.set(i,propDesc.get(jj));
        propDesc.set(jj,i_ob);

      }
      return true;
    }

    private boolean typeMatch(Class c, PropertyDescriptor pd)
    {
      return pd.getPropertyType().equals(c);
    }
    private void setNames(List instanc, ArrayList propDesc)
    {
      for(int i=0;i<instanc.size();i++) {
        VInstantiator vi = (VInstantiator)instanc.get(i);
        vi.setName(((PropertyDescriptor)propDesc.get(i)).getName());
      }
    }

    private List getDefaultArgs(String type)
    {
      Class clazz = Vstatics.classForName(type);
      if(clazz != null) {
        Constructor[] construct = clazz.getConstructors();
        if(construct != null && construct.length > 0)
          return VInstantiator.buildDummyInstantiators(construct[0]);
      }
      return new Vector(); // null
    }

    public List getArgs()         {return args;}
    public void setArgs(List args){this.args = args;}

    public String toString()
    {
      String rets = "new "+getType()+ "(";
      rets = rets + (args.size()>0?((VInstantiator)args.get(0)).getType()+",...":"");
      return rets+")";
    }

    public VInstantiator vcopy()
    {
      Vector lis = new Vector();
      for (Iterator itr = getArgs().iterator(); itr.hasNext();) {
        VInstantiator vi = (VInstantiator) itr.next();
        lis.add(vi.vcopy());
      }
      VInstantiator rv = new VInstantiator.Constr(getType(),lis);
      rv.setName(this.getName());
      return rv;
    }
    public boolean isValid()
    {
      if(getType()==null || getType().length() <=0)
        return false;
      for (Iterator itr = getArgs().iterator(); itr.hasNext();) {
        VInstantiator v = (VInstantiator) itr.next();
        if(!v.isValid())
          return false;
      }
      return true;
    }
  }

  /***********************************************************************/
  public static class Array extends VInstantiator
  {
    private List instantiators; // array dimension == size()
    public Array(String typ, List inst)
    {
      super(typ);
      setInstantiators(inst);
    }
    public VInstantiator vcopy()
    {
      Vector lis = new Vector();
      for (Iterator itr = getInstantiators().iterator(); itr.hasNext();) {
        VInstantiator vi = (VInstantiator) itr.next();
        lis.add(vi.vcopy());
      }
      VInstantiator rv = new VInstantiator.Array(getType(),getInstantiators());
      rv.setName(this.getName());
      return rv;
    }

    public List getInstantiators()
    {
      return instantiators;
    }

    public void setInstantiators(List instantiators)
    {
      this.instantiators = instantiators;
    }

    public String toString()
    {
      if(instantiators != null) {
        String t = getType().substring(0,getType().indexOf('['));
        return "new " + t + "[" + instantiators.size() + "]";
      }
      else
        return "";
    }
    public boolean isValid()
    {
      if(getType()==null || getType().length() <=0)
        return false;
      for (Iterator itr = getInstantiators().iterator(); itr.hasNext();) {
        VInstantiator v = (VInstantiator) itr.next();
        if(!v.isValid())
          return false;
      }
      return true;
    }
  }

  /***********************************************************************/
  public static class Factory extends VInstantiator
  {
    private String factoryClass;
    private String method;
    private List   params;

    public Factory(String type, String factoryClass, String method, List params)
    {
      super(type);
      setFactoryClass(factoryClass);
      setMethod(method);
      setParams(params);
    }

    public String getFactoryClass(){return factoryClass;}
    public String getMethod()      {return method;}
    public List   getParams()      {return params;}

    public void setFactoryClass(String s){this.factoryClass = s;}
    public void setMethod(String m)      {this.method = m;}
    public void setParams(List p)        {this.params = p;}

    public String toString()
    {
      return factoryClass+"."+method+"("+((VInstantiator)params.get(0)).getType()+",...)";
    }
    public VInstantiator vcopy()
    {
      Vector lis = new Vector();
      for (Iterator itr = lis.iterator(); itr.hasNext();) {
        VInstantiator vi = (VInstantiator) itr.next();
        lis.add(vi.vcopy());
      }
      VInstantiator rv = new VInstantiator.Factory(getType(),getFactoryClass(),getMethod(),lis);
      rv.setName(this.getName());
      return rv;
    }
    public boolean isValid()
    {
      String t=getType(),fc=getFactoryClass(),m=getMethod();
      if ( t == null || fc == null || m == null ||
           t.length()<=0 || fc.length()<= 0 || m.length()<=0)
        return false;

      for (Iterator itr = getParams().iterator(); itr.hasNext();) {
        VInstantiator v = (VInstantiator) itr.next();
        if(!v.isValid())
          return false;
      }
      return true;
    }

  }
}