package viskit.model;

import viskit.Vstatics;
import viskit.xsd.translator.SimkitXML2Java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
      return new VInstantiator.FreeF(getType(),getValue());
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

    /**
     * Find the names of the arguments
     * @param type
     * @param args List of VInstantiators
     */
    private void findArgNames(String type, List args)
    {
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
          return;  // found nothing

        Field fld = cls.getDeclaredField(SimkitXML2Java.constructorParmNamesID);
        String[][] nms = (String[][])fld.get(null);
        if(nms != null && nms.length>0) {
          for(int n=0;n<nms[j].length;n++)
            try {
              ((VInstantiator)args.get(n)).setName(nms[j][n]);
            }
            catch (Throwable e) {
              // if the names are manually editted, the author may have screwed up; don't crash
              if(args.size()>n)
                ((VInstantiator)args.get(n)).setName("error, check source");
            }
        }
      }
      catch (Exception e) {
        //System.out.println("can't find constructor param names");
      }

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
      return new VInstantiator.Constr(getType(),lis);
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
      return new VInstantiator.Array(getType(),getInstantiators());
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
      return new VInstantiator.Factory(getType(),getFactoryClass(),getMethod(),lis);
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