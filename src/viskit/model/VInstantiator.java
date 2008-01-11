package viskit.model;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import viskit.Vstatics;
import viskit.xsd.bindings.eventgraph.Parameter;
import viskit.xsd.bindings.assembly.TerminalParameter;
import viskit.xsd.bindings.assembly.FactoryParameter;
import viskit.xsd.bindings.assembly.MultiParameter;
import viskit.xsd.bindings.assembly.ObjectFactory;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 15, 2004
 * @since 9:43:42 AM
 * @version $Id: VInstantiator.java 1662 2007-12-16 19:44:04Z tdnorbra $
 */
public abstract class VInstantiator {
    
    static Logger log = Logger.getLogger(VInstantiator.class);

    private String type;
    private String name = "";
    private String description = "";

    public String getType() {
        return type;
    }

    public VInstantiator(String typ) {
        type = typ;
    }

    public void setName(String nm) {
        name = nm;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public String getDescription() {
        return description;
    }

    abstract public VInstantiator vcopy();

    abstract public boolean isValid();

    public static List<Object> buildDummyInstantiators(Constructor con) {
        Vector<Object> v = new Vector<Object>();
        Class<?>[] cs = con.getParameterTypes();
        for (int i = 0; i < cs.length; i++) {
            if (cs[i].isArray()) {
                VInstantiator.Array va = new VInstantiator.Array(Vstatics.convertClassName(cs[i].getName()),
                        new Vector<Object>());
                v.add(va);
            } else {
                VInstantiator.FreeF vff = new VInstantiator.FreeF(Vstatics.convertClassName(cs[i].getName()),
                        "");
                v.add(vff);
            }
        }
        return v;
    }

    /***********************************************************************/
    public static class FreeF extends VInstantiator {

        private String value;

        public FreeF(String type, String value) {
            super(type);
            setValue(value);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public VInstantiator vcopy() {
            VInstantiator rv = new VInstantiator.FreeF(getType(), getValue());
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        public boolean isValid() {
            String t = getType();
            String v = getValue();
            return t != null & v != null & t.length() > 0 && v.length() > 0;
        }
    }

    /***********************************************************************/
    public static class Constr extends VInstantiator {

        private List<Object> args;

        // takes List of Assembly parameters and args for type
        public Constr(List<Object> params, String type) {
            super(type);

            if (viskit.Vstatics.debug) {
                log.info("Building Constr for " + type);
            }
            if (viskit.Vstatics.debug) {
                log.info("Required Parameters:");
            }

            for (Object o : params) {

                String s1 = "null";
                if (o instanceof TerminalParameter) { // check if caller is sending assembly param types
                    s1 = ((TerminalParameter) o).getType();
                    if (viskit.Vstatics.debug) {
                        System.out.print("\tAssembly TerminalParameter");
                    }
                } else if (o instanceof MultiParameter) {
                    s1 = ((MultiParameter) o).getType();
                    if (viskit.Vstatics.debug) {
                        System.out.print("\tAssembly MultiParameter");
                    }
                } else if (o instanceof FactoryParameter) {
                    s1 = ((FactoryParameter) o).getType();
                    if (viskit.Vstatics.debug) {
                        System.out.print("\tAssembly FactoryParameter");
                    }
                } else if (o instanceof Parameter) { // from InstantiationPanel, this could also be an eventgraph param type?
                    s1 = ((Parameter) o).getType();
                    if (viskit.Vstatics.debug) {
                        System.out.print("\tEventGraph Parameter");
                    }
                }
                if (viskit.Vstatics.debug) {
                    log.info(" " + s1);
                }
            }
            // gets lists of EventGraph parameters for type if top-level
            // or null if type is a basic class ie. java.lang.Double
            // todo use same block as LegosTree to resolve any type
            List<Object>[] eparams = Vstatics.resolveParameters(type);
            int indx = 0;

            args = buildInstantiators(params);
            // pick the EventGraph list that matches the
            // Assembly arguments
            if (eparams != null) {
                while (indx < (eparams.length - 1)) {

                    if (paramsMatch(params, eparams[indx])) {
                        break;
                    } else {
                        indx++;
                    }
                }
                if (viskit.Vstatics.debug) {
                    log.info(type + " VInstantiator using constructor #" + indx);
                }
                // bug: weird case where params came in 0 length but no 0 length constuctors
                // happens if external class used as parameter?
                if (params.size() != eparams[indx].size()) {
                    args = buildInstantiators(eparams[indx]);
                    if (viskit.Vstatics.debug) {
                        System.err.println("Warning: VInstantiator.Constr tried 0 length when it was more");
                    }
                }
                if (eparams[indx] != null) {
                    // now that the values, types, etc set, grab names from eg parameters
                    if (viskit.Vstatics.debug) {
                        log.info("args came back from buildInstantiators as: ");
                        for (int i = 0; i < args.size(); i++) {
                            log.info(args.get(i));
                        }
                    }
                    if (args != null) {
                        for (int j = 0; j < eparams[indx].size(); j++) {//for ( int j = 0; j < args.size(); j++ ) {
                            if (viskit.Vstatics.debug) {
                                log.info("setting name " + ((Parameter) eparams[indx].get(j)).getName());
                            }
                            ((VInstantiator) args.get(j)).setName(((Parameter) eparams[indx].get(j)).getName());
                            ((VInstantiator) args.get(j)).setDescription(listToString(((Parameter) eparams[indx].get(j)).getComment()));

                        }
                    }
                }
            }
        }

        public Constr(String type, List<Object> args) {
            super(type);
            setArgs(args);
            findArgNames(type, args);
        }

        public Constr(String type, List<Object> args, List<String> names) {
            this(type, args);
            for (int i = 0; i < args.size(); i++) {
                ((VInstantiator) args.get(i)).setName(names.get(i));
            }
        }

        private String listToString(List<String> lis) {
            StringBuffer sb = new StringBuffer("");
            for (String s : lis) {
                sb.append(s);
            }
            return sb.toString();
        }

        /** @return a List of VInstantiators given a List of Assembly Parameters */
        List<Object> buildInstantiators(List<Object> assemblyParameters) {

            ArrayList<Object> instr = new ArrayList<Object>();
            for (Object o : assemblyParameters) {
                if (o instanceof TerminalParameter) {
                    instr.add(buildTerminalParameter((TerminalParameter) o));
                } else if (o instanceof MultiParameter) {
                    instr.add(buildMultiParameter((MultiParameter) o));
                } else if (o instanceof FactoryParameter) {
                    instr.add(buildFactoryParameter((FactoryParameter) o));
                } else if (o instanceof Parameter) { // from InstantiationPanel Const getter
                    if (viskit.Vstatics.debug) {
                        log.info("Conversion from " + ((Parameter) o).getType());
                    } // 

                    String type = ((Parameter) o).getType();
                    String name = ((Parameter) o).getName();
                    ObjectFactory of = new ObjectFactory();

                    // TerminalParameter
                    if (Vstatics.isPrimitive(type) || type.equals("String") || type.equals("java.lang.String")) {
                        TerminalParameter tp = of.createTerminalParameter();
                        tp.setType(type);
                        tp.setName(name);
                        tp.setValue("");

                        instr.add(buildTerminalParameter(tp));

                    } else if (Vstatics.numConstructors(type) > 0) { // MultiParameter

                        MultiParameter mp = of.createMultiParameter();
                        mp.setType(type);
                        mp.setName(name);
                        // if  mp is [] then done
                        if (!type.endsWith("]")) {
                        // fill with empty parameters?
                        }
                        instr.add(buildMultiParameter(mp));

                    } else { // no constructors, should be a FactoryParameter or array of them
                        if (type.endsWith("]")) {
                            MultiParameter mp = of.createMultiParameter();
                            mp.setType(type);
                            mp.setName(name);
                            instr.add(buildMultiParameter(mp));
                        } else {
                            FactoryParameter fp = of.createFactoryParameter();
                            fp.setName(name);
                            fp.setFactory(type); // this gets handled later
                            fp.setType(type); // this should be the type returned by method
                            fp.setMethod("fill in method for factory");

                            instr.add(buildFactoryParameter(fp));
                        }
                    }
                }
            }
            return instr;
        }

        VInstantiator.FreeF buildTerminalParameter(TerminalParameter p) {
            VInstantiator.FreeF vf = new VInstantiator.FreeF(p.getType(), p.getValue());
            return vf;
        }

        VInstantiator.Array buildMultiParameter(MultiParameter p, boolean dummy) {
            VInstantiator.Array va = new VInstantiator.Array(p.getType(), buildInstantiators(p.getParameters()));
            return va;
        }

        VInstantiator buildMultiParameter(MultiParameter p) {
            VInstantiator vAorC;
            if (p.getType().endsWith("]")) {
                vAorC = buildMultiParameter(p, true);
            } else {
                if (Vstatics.debug) {
                    log.info("Trying to buildMultiParamter " + p.getType());
                }
                List tmp = p.getParameters();

                if (tmp.isEmpty()) {
                    tmp = Vstatics.resolveParameters(p.getType())[0];
                }
                Iterator li = tmp.iterator();
                if (Vstatics.debug) {
                    while (li.hasNext()) {
                        log.info(li.next());
                    }
                }

                vAorC = new VInstantiator.Constr(p.getParameters(), p.getType());
            }
            return vAorC;
        }

        VInstantiator.Factory buildFactoryParameter(FactoryParameter p) {
            return new VInstantiator.Factory(
                    p.getType(), p.getFactory(), p.getMethod(),
                    buildInstantiators(p.getParameters()));
        }

        boolean paramsMatch(List<Object> aparams, List<Object> eparams) {
            if (aparams.size() != eparams.size()) {
                if (viskit.Vstatics.debug) {
                    log.info("No match.");
                }
                return false;
            }

            for (int i = 0; i < aparams.size(); i++) {
                Object o = aparams.get(i);
                String eType = ((Parameter) (eparams.get(i))).getType();
                String aType;
                if (o instanceof TerminalParameter) { // check if caller is sending assembly param types
                    aType = ((TerminalParameter) o).getType();
                } else if (o instanceof MultiParameter) {
                    aType = ((MultiParameter) o).getType();
                } else if (o instanceof FactoryParameter) {
                    aType = ((FactoryParameter) o).getType();
                } else if (o instanceof Parameter) { // from InstantiationPanel, this could also be an eventgraph param type
                    aType = ((Parameter) o).getType();
                } else {
                    return false;
                }
                if (viskit.Vstatics.debug) {
                    System.out.print("Type match " + aType + " to " + eType);
                }
                //aType = aType.split("\\.")[ aType.split("\\.").length - 1 ];
                //eType = eType.split("\\.")[ eType.split("\\.").length - 1 ];
                //if (viskit.Vstatics.debug) log.info(" tail "+aType + " " + eType); // eg. java.lang.String vs. String?

                // check if vType was assignable from pType.

                Class<?> eClazz = Vstatics.classForName(eType);
                Class<?> aClazz = Vstatics.classForName(aType);
                Class<?>[] vInterfz = aClazz.getInterfaces();
                boolean interfz = false;
                for (int k = 0; k < vInterfz.length; k++) {
                    //interfz |= vInterfz[k].isAssignableFrom(eClazz);
                    interfz |= eClazz.isAssignableFrom(vInterfz[k]);
                }
                //boolean match = ( aClazz.isAssignableFrom(eClazz) | interfz );
                boolean match = (eClazz.isAssignableFrom(aClazz) | interfz);
                if (!match) {
                    if (viskit.Vstatics.debug) {
                        log.info("No match.");
                    }
                    return false;
                }
            }
            if (viskit.Vstatics.debug) {
                log.info("Match.");
            }
            return true;
        }

        /**
         * Find the names of the arguments
         * @param type
         * @param args List of VInstantiators
         */
        private void findArgNames(String type, List<Object> args) {
            if (args == null) {
                setArgs(getDefaultArgs(type));
                args = getArgs();
            }
            if (indexOfArgNames(type, args) < 0) {
                log.info(type + " not loaded?");
            }
        //findArgNamesFromBeanStuff(type,args);
        }

        /** @return the index into the found matching constructor **/
        public int indexOfArgNames(String type, List<Object> args) {
            List<Object>[] parameters = Vstatics.resolveParameters(type);
            if (parameters == null) {
                return -1;
            }
            int indx = 0;
            int ix = 0;
            boolean found = false;

            if (viskit.Vstatics.debug) {
                log.info("args length " + args.size());
                log.info("resolveParameters " + type + " list length is " + parameters.length);
            }
            for (List<Object> parameterLi : parameters) {
                if (viskit.Vstatics.debug) {
                    log.info("parameterLi.size() " + parameterLi.size());
                }
                if (parameterLi.size() == args.size()) {
                    boolean match = true;
                    for (int j = 0; j < args.size(); j++) {

                        if (viskit.Vstatics.debug) {
                            System.out.print("touching " + Vstatics.convertClassName(((Parameter) (parameterLi.get(j))).getType()) + " " + ((VInstantiator) args.get(j)).getType());
                        }
                        String pType = Vstatics.convertClassName(((Parameter) (parameterLi.get(j))).getType());
                        String vType = ((VInstantiator) args.get(j)).getType();
                        
                        // check if vType was assignable from pType.

                        Class<?> pClazz = Vstatics.classForName(pType);
                        Class<?> vClazz = Vstatics.classForName(vType);
                        Class<?>[] vInterfz = vClazz.getInterfaces();
                        boolean interfz = false;
                        for (Class<?> clazz : vInterfz) {
                            //interfz |= vInterfz[k].isAssignableFrom(pClazz);
                            interfz |= pClazz.isAssignableFrom(clazz);
                        }
                        //match &= ( vClazz.isAssignableFrom(pClazz) | interfz );
                        match &= (pClazz.isAssignableFrom(vClazz) | interfz);
                        // set the names, the final iteration of while cleans up

                        ((VInstantiator) (args.get(j))).setName(((Parameter) (parameterLi.get(j))).getName());
                        if (viskit.Vstatics.debug) {
                            log.info(" to " + ((Parameter) (parameterLi.get(j))).getName());
                        }                        
                    }
                    found = match;
                    log.info("found is: " + found);
                    if (found) {
                        indx = ix;
//                        ix = parameters.length;
                        break;
                    }
                }
                ix++;
            }
            if (viskit.Vstatics.debug) {
                log.info("Resolving " + type + " " + parameters[indx] + " at index " + indx);
            }
            // the class manager caches Parameter List jaxb from the SimEntity.
            // if it didn't come from XML, then a null is returned. Probably
            // better to move the findArgNamesFromBeansStuff() to the class manager
            // to resolve the bean there, then in that case, a null could
            // indicate a zero-parameter constructor.

            if (!found) {
                indx = -1;
                log.error("An index of -1 will throw an error to the caller of VInstantiator.indexOfArgNames(String, List<Object>)");
            }
            return indx;
        }

        private void findArgNamesFromBeanStuff(String type, List<Object> args) {
            // Alternate method
            // 1. The parameters to an event graph have both getters and setters.  State vars do not
            //

            try {
                Class<?> cls = Vstatics.classForName(type);
                if (cls == null) {
                    return;
                }

                Class<?>[] argArr = new Class<?>[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    argArr[i] = Vstatics.classForName((String) ((VInstantiator) args.get(i)).getType());
                }

                //Introspector.flushCaches();
                BeanInfo bi = Introspector.getBeanInfo(cls, cls.getSuperclass());
                ArrayList<PropertyDescriptor> parms = new ArrayList<PropertyDescriptor>();
                PropertyDescriptor[] pd = bi.getPropertyDescriptors();
                for (PropertyDescriptor descriptor : pd) {
                    if (descriptor.getReadMethod() != null &&
                            descriptor.getWriteMethod() != null) {
                        parms.add(descriptor);
                    }
                }
                if (unambiguousMatch(argArr, parms)) {
                    setNames(args, parms);
                }
            } catch (Throwable e) {
            //e.printStackTrace();
            }
        }

        private boolean unambiguousMatch(Class[] args, ArrayList<PropertyDescriptor> propDesc) {
            // if can find unambiguous match by type, put propDesc into proper order
            if (args.length <= 0 || propDesc.size() <= 0) {
                return false;
            }
            Vector<Integer> holder = new Vector<Integer>();
            for (int i = 0; i < args.length; i++) {
                holder.clear();
                Class<?> c = args[i];
                for (int j = 0; j < propDesc.size(); j++) {
                    PropertyDescriptor pd = propDesc.get(j);
                    if (typeMatch(c, pd)) {
                        holder.add(new Integer(j));
                    }
                }
                if (holder.size() != 1) {
                    return false;
                }
                int jj = holder.get(0).intValue();
                // put pd at j into i
                PropertyDescriptor i_ob = propDesc.get(i);
                propDesc.set(i, propDesc.get(jj));
                propDesc.set(jj, i_ob);

            }
            return true;
        }

        private boolean typeMatch(Class<?> c, PropertyDescriptor pd) {
            return pd.getPropertyType().equals(c);
        }

        private void setNames(List<Object> instanc, ArrayList<PropertyDescriptor> propDesc) {
            for (int i = 0; i < instanc.size(); i++) {
                VInstantiator vi = (VInstantiator) instanc.get(i);
                vi.setName(propDesc.get(i).getName());
            }
        }

        private List<Object> getDefaultArgs(String type) {
            Class<?> clazz = Vstatics.classForName(type);
            if (clazz != null) {
                Constructor[] construct = clazz.getConstructors();
                if (construct != null && construct.length > 0) {
                    return VInstantiator.buildDummyInstantiators(construct[0]);
                }
            }
            return new Vector<Object>(); // null
        }

        public List<Object> getArgs() {
            return args;
        }

        public void setArgs(List<Object> args) {
            this.args = args;
        }

        @Override
        public String toString() {
            String rets = "new " + getType() + "(";
            rets = rets + (args.size() > 0 ? ((VInstantiator) args.get(0)).getType() + ",..." : "");
            return rets + ")";
        }

        public VInstantiator vcopy() {
            Vector<Object> lis = new Vector<Object>();
            for (Object o : getArgs()) {
                VInstantiator vi = (VInstantiator) o;
                lis.add(vi.vcopy());
            }
            VInstantiator rv = new VInstantiator.Constr(getType(), lis);
            rv.setName(new String(this.getName()));
            rv.setDescription(new String(this.getDescription()));
            return rv;
        }

        public boolean isValid() {
            if (getType() == null || getType().length() <= 0) {
                return false;
            }
            for (Object o : getArgs()) {
                VInstantiator v = (VInstantiator) o;
                if (!v.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /***********************************************************************/
    public static class Array extends VInstantiator {

        private List<Object> instantiators; // array dimension == size()

        public Array(String typ, List<Object> inst) {
            super(typ);
            setInstantiators(inst);
        }

        public VInstantiator vcopy() {
            Vector<Object> lis = new Vector<Object>();
            for (Object vi : getInstantiators()) {
                lis.add(((VInstantiator) vi).vcopy());
            }
            VInstantiator rv = new VInstantiator.Array(getType(), getInstantiators());
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        public List<Object> getInstantiators() {
            return instantiators;
        }

        public void setInstantiators(List<Object> instantiators) {
            this.instantiators = instantiators;
        }

        @Override
        public String toString() {
            if (instantiators != null) {
                String t = getType().substring(0, getType().indexOf('['));
                return "new " + t + "[" + instantiators.size() + "]";
            } else {
                return "";
            }
        }

        public boolean isValid() {
            if (getType() == null || getType().length() <= 0) {
                return false;
            }
            for (Object vi : getInstantiators()) {
                if (!((VInstantiator) vi).isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /***********************************************************************/
    public static class Factory extends VInstantiator {

        private String factoryClass;
        private String method;
        private List<Object> params;

        public Factory(String type, String factoryClass, String method, List<Object> params) {
            super(type);
            setFactoryClass(factoryClass);
            setMethod(method);
            setParams(params);
        }

        public String getFactoryClass() {
            return factoryClass;
        }

        public String getMethod() {
            return method;
        }

        public List<Object> getParams() {
            return params;
        }

        public void setFactoryClass(String s) {
            this.factoryClass = s;
        }

        public void setMethod(String m) {
            this.method = m;
        }

        public void setParams(List<Object> p) {
            this.params = p;
        }

        @Override
        public String toString() {
            if (params.size() <= 0) {
                return "";
            }
            return factoryClass + "." + method + "(" + ((VInstantiator) params.get(0)).getType() + ",...)";
        }

        public VInstantiator vcopy() {
            Vector<Object> lis = new Vector<Object>();
            for (Object o : getParams()) {
                VInstantiator vi = (VInstantiator) o;
                lis.add(vi.vcopy());
            }
            VInstantiator rv = new VInstantiator.Factory(getType(), getFactoryClass(), getMethod(), lis);
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        public boolean isValid() {
            String t = getType(), fc = getFactoryClass(), m = getMethod();
            if (t == null || fc == null || m == null ||
                    t.length() <= 0 || fc.length() <= 0 || m.length() <= 0) {
                return false;
            }

            for (Object o : getParams()) {
                VInstantiator v = (VInstantiator) o;
                if (!v.isValid()) {
                    return false;
                }
            }
            return true;
        }}
}
