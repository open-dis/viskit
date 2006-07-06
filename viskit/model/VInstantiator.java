package viskit.model;

import viskit.Vstatics;
import viskit.xsd.translator.SimkitXML2Java;
import viskit.xsd.bindings.eventgraph.ParameterType;
import viskit.xsd.bindings.assembly.TerminalParameterType;
import viskit.xsd.bindings.assembly.FactoryParameterType;
import viskit.xsd.bindings.assembly.MultiParameterType;
import viskit.xsd.bindings.assembly.ObjectFactory;
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

    // takes List of Assembly parameters and args for type

    public Constr(List params, String type) {
        super(type);
        
        if (viskit.Vstatics.debug) System.out.println("Building Constr for "+type);
        if (viskit.Vstatics.debug) System.out.println("Required Parameters:");
        java.util.ListIterator li = params.listIterator();
        while(li.hasNext()) {
            Object o = li.next();
           
            String s1="null";
            if ( o instanceof TerminalParameterType ) { // check if caller is sending assembly param types
                s1 = ((TerminalParameterType)o).getType();  if(viskit.Vstatics.debug)System.out.print("\tAssembly TerminalParameterType");
            } else if ( o instanceof MultiParameterType ) {
                s1 = ((MultiParameterType)o).getType(); if(viskit.Vstatics.debug)System.out.print("\tAssembly MultiParameterType");
            } else if ( o instanceof FactoryParameterType ) {
                s1 = ((FactoryParameterType)o).getType();   if(viskit.Vstatics.debug)System.out.print("\tAssembly FactoryParameterType");              
            } else if (o instanceof ParameterType) { // from InstantiationPanel, this could also be an eventgraph param type?
                s1 = ((ParameterType)o).getType(); if(viskit.Vstatics.debug)System.out.print("\tEventGraph ParameterType");

            }  
            if (viskit.Vstatics.debug) System.out.println(" "+s1);
        }
        // gets lists of EventGraph parameters for type if top-level
        // or null if type is a basic class ie. java.lang.Double
        // todo use same block as LegosTree to resolve any type
        List[] eparams = Vstatics.resolveParameters(type);
        int indx = 0;
        
        args = buildInstantiators(params);
        // pick the EventGraph list that matches the
        // Assembly arguments
        if (eparams != null) {
            while ( indx < eparams.length-1 ) {
                
                if (paramsMatch(params,eparams[indx])) break;
                else indx++;
            }
            if (viskit.Vstatics.debug) System.out.println("Using constructor #"+indx);
            
            if (eparams[indx] != null) { 
                // now that the values, types, etc set, grab names from eg parameters
                if ( args != null ) for ( int j = 0; j < args.size(); j++ ) {
                    if (viskit.Vstatics.debug) System.out.println("setting name "+((ParameterType)eparams[indx].get(j)).getName());
                    ((VInstantiator)args.get(j)).setName(((ParameterType)eparams[indx].get(j)).getName());
                    
                }
            }
        }
    }
    
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
    
    // return a List of VInstantiators given a List of Assembly Parameters
    List buildInstantiators(List assemblyParameters) {
        ArrayList instr = new ArrayList();
        Iterator it = assemblyParameters.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof TerminalParameterType) {
                instr.add(buildTerminalParameter((TerminalParameterType)o));
            } else if (o instanceof MultiParameterType) { 
                instr.add(buildMultiParameter((MultiParameterType)o));
            } else if (o instanceof FactoryParameterType) {
                instr.add(buildFactoryParameter((FactoryParameterType)o));
            } else if (o instanceof ParameterType) { // from InstantiationPanel Const getter
                if (viskit.Vstatics.debug) System.out.println("Conversion from "+((ParameterType)o).getType()); // 
                
                List args = new ArrayList();
                String type = ((ParameterType)o).getType();
                String name = ((ParameterType)o).getName();
                ObjectFactory of = new ObjectFactory();
                
                // TerminalParameter
                if ( Vstatics.isPrimitive(type) || type.equals("String") || type.equals("java.lang.String")) {
                    try {
                        TerminalParameterType tp = of.createTerminalParameterType();
                        tp.setType(type);
                        tp.setName(name);
                        tp.setValue("");
                        instr.add(buildTerminalParameter(tp));
                    } catch (javax.xml.bind.JAXBException jaxbe) {
                        jaxbe.printStackTrace();
                    }
                } else if (Vstatics.numConstructors(type) > 0 ){ // MultiParameter
                    
                    try {
                        MultiParameterType mp = of.createMultiParameterType();
                        mp.setType(type);
                        mp.setName(name);
                        // if  mp is [] then done
                        if (!type.endsWith("]")) {
                            // fill with empty parameters?
                        }
                        instr.add(buildMultiParameter(mp));
                    } catch (javax.xml.bind.JAXBException ex) {
                        ex.printStackTrace();
                    }
                    
                } else { // no constructors, should be a FactoryParameter or array of them
                    try {
                        if (type.endsWith("]")) {
                            MultiParameterType mp = of.createMultiParameterType();
                            mp.setType(type);
                            mp.setName(name);
                            instr.add(buildMultiParameter(mp));
                        } else {
                            FactoryParameterType fp = of.createFactoryParameterType();
                            fp.setName(name);
                            fp.setFactory(type); // this gets handled later
                            fp.setType(type); // this should be the type returned by method
                            fp.setMethod("fill in method for factory");
                            instr.add(buildFactoryParameter(fp));
                        }
                    } catch (javax.xml.bind.JAXBException ex) {
                        ex.printStackTrace();
                    }
                    
                }
                
            }
        }
        return instr;
    }
    
    VInstantiator.FreeF buildTerminalParameter(TerminalParameterType p) {
        VInstantiator.FreeF vf = new VInstantiator.FreeF(p.getType(),p.getValue());
        return vf;
    }
    
    VInstantiator.Array buildMultiParameter(MultiParameterType p, boolean dummy) {
        VInstantiator.Array va = new VInstantiator.Array(p.getType(),buildInstantiators(p.getParameters()));
        return va;
    }
    
    VInstantiator buildMultiParameter(MultiParameterType p) {
        VInstantiator vAorC;
        if (p.getType().endsWith("]")) {
            vAorC = buildMultiParameter(p,true);
        } else {
            vAorC = new VInstantiator.Constr(p.getParameters(),p.getType());
        }
        return vAorC;
    }
    
    VInstantiator.Factory buildFactoryParameter(FactoryParameterType p) {
        VInstantiator.Factory vf = 
                new VInstantiator.Factory(
                    p.getType(),p.getFactory(),p.getMethod(),
                    buildInstantiators(p.getParameters())
                );
        return vf;
    }
    
    boolean paramsMatch(List aparams,List eparams) {
        if ( aparams.size() != eparams.size() ) {
            if (viskit.Vstatics.debug) System.out.println("No match.");
            return false;
        }
        
        for (int i = 0; i < aparams.size(); i++) {
            Object o = aparams.get(i);
            String s2 = ((ParameterType)(eparams.get(i))).getType();
            String s1;
            if ( o instanceof TerminalParameterType ) { // check if caller is sending assembly param types
                s1 = ((TerminalParameterType)o).getType();
            } else if ( o instanceof MultiParameterType ) {
                s1 = ((MultiParameterType)o).getType();
            } else if ( o instanceof FactoryParameterType ) {
                s1 = ((FactoryParameterType)o).getType();
            } else if (o instanceof ParameterType) { // from InstantiationPanel, this could also be an eventgraph param type
                s1 = ((ParameterType)o).getType();
            }   else return false;
            System.out.print("Type match "+s1 + " to "+ s2);
            s1 = s1.split("\\.")[ s1.split("\\.").length - 1 ];
            s2 = s2.split("\\.")[ s2.split("\\.").length - 1 ];
            if (viskit.Vstatics.debug) System.out.println(" tail "+s1 + " " + s2); // eg. java.lang.String vs. String?
            if (!s1.equals(s2)) {
                if (viskit.Vstatics.debug) System.out.println("No match.");
                return false;
            }
        }
        if (viskit.Vstatics.debug) System.out.println("Match.");
        return true;
        
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
      if(indexOfArgNames(type,args)<0) {System.out.println(type + " not loaded?");}
        //findArgNamesFromBeanStuff(type,args);
    }

    // also returns the index into the found matching constructor
    public int indexOfArgNames(String type, List args)
    {  
      List[] parameters = Vstatics.resolveParameters(type);
      if (parameters == null) return -1; 
      int indx = 0;
      boolean found = false;
      
      if (viskit.Vstatics.debug) {
          System.out.println("args length "+args.size());
          System.out.println("resolveParameters "+type+" list length is "+parameters.length);
          
      }
      for (int i = 0; i < parameters.length; i++) {
      //while ( indx <= parameters.length - 1 && !found ) {
          if (viskit.Vstatics.debug) System.out.println("parameters["+i+"].size() "+parameters[i].size());
          if ( parameters[i].size() == args.size() ) {
              boolean match = true;
              for ( int j = 0; j < args.size(); j++ ) {
                  
                  if (viskit.Vstatics.debug) System.out.print("touching "+Vstatics.convertClassName(((ParameterType)(parameters[i].get(j))).getType())+" "+((VInstantiator)(args.get(j))).getType());
                  String pType = Vstatics.convertClassName(((ParameterType)(parameters[i].get(j))).getType());
                  String vType = ((VInstantiator)(args.get(j))).getType();
                  match &= pType.equals(vType);                
                  // set the names, the final iteration of while cleans up any
                  // foo
                  
                  ((VInstantiator)(args.get(j))).setName(
                        ((ParameterType)(parameters[i].get(j))).getName()
                  );
                  if (viskit.Vstatics.debug) System.out.println(" to "+((ParameterType)(parameters[i].get(j))).getName());
                          
              } 
              found = match;
              if (found) {
                  indx = i;
                  i = parameters.length;
              }
              
          } 
          
          
      }
      if (viskit.Vstatics.debug) System.out.println("Resolving "+type+" "+parameters[indx]+" at index "+indx);
      // the class manager caches Parameter List jaxb from the SimEntity.
      // if it didn't come from XML, then a null is returned. Probably
      // better to move the findArgNamesFromBeansStuff() to the class manager
      // to resolve the bean there, then in that case, a null could
      // indicate a zero-parameter constructor.
      
      if (!found) indx = -1;
      return indx;
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
      rv.setName(new String(this.getName()));
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
      if(params.size()<=0) {
        return "";
      }
      return factoryClass+"."+method+"("+((VInstantiator)params.get(0)).getType()+",...)";
    }
    public VInstantiator vcopy()
    {
      Vector lis = new Vector();
      for (Iterator itr = getParams().iterator(); itr.hasNext();) {
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