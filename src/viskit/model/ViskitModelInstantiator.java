package viskit.model;


import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

import viskit.ViskitGlobals;
import viskit.ViskitStatics;
import static viskit.ViskitStatics.emptyIfNull;
import viskit.xsd.bindings.assembly.FactoryParameter;
import viskit.xsd.bindings.assembly.MultiParameter;
import viskit.xsd.bindings.assembly.ObjectFactory;
import viskit.xsd.bindings.assembly.TerminalParameter;
import viskit.xsd.bindings.eventgraph.Parameter;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Jun 15, 2004
 * @since 9:43:42 AM
 * @version $Id$
 */
public abstract class ViskitModelInstantiator
{
    static final Logger LOG = LogManager.getLogger();
    
    private String name = "";
    private String type = "";
    private String description = "";

    public ViskitModelInstantiator(String typ) {
        type = typ;
    }

    public String getType() {
        return type;
    }

    public void setName(String nm) {
        name = nm;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return emptyIfNull(description);
    }

    public void setDescription(String newDescription) {
        description = emptyIfNull(newDescription);
    }

    abstract public ViskitModelInstantiator vcopy();

    abstract public boolean isValid();

    public static Vector<Object> buildDummyInstantiators(Executable exe) {

        Vector<Object> v = new Vector<>();
        Class<?>[] cs = exe.getParameterTypes();
        String args;
        for (Class<?> c : cs) {
            args = ViskitStatics.convertClassName(c.getName());

            // Strip out java.lang
            args = ViskitStatics.stripOutJavaDotLang(args);

            // Show varargs symbol vice []
            args = ViskitStatics.makeVarArgs(args);

            if (c.isArray())
                v.add(new ViskitModelInstantiator.Array(args, new ArrayList<>()));
            else
                v.add(new ViskitModelInstantiator.FreeF(args, ""));
        }
        return v;
    }

    /* **********************************************************************/
    public static class FreeF extends ViskitModelInstantiator {

        private String value;

        public FreeF(String type, String value) {
            super(type);
            setValue(value);
        }

        public String getValue() {
            return value;
        }

        public final void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public ViskitModelInstantiator vcopy() {
            ViskitModelInstantiator rv = new ViskitModelInstantiator.FreeF(getType(), getValue());
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        @Override
        public boolean isValid() {
            String t = getType();
            String v = getValue();
            return t != null & v != null & !t.isEmpty() & !v.isEmpty();
        }
    }

    /* **********************************************************************/
    public static class Constr extends ViskitModelInstantiator {

        private List<Object> args;

        /** Takes a List of Assembly parameters and args for type
         *
         * @param params a list of Assembly parameters
         * @param type a parameter type
         */
        public Constr(List<Object> params, String type) {
            super(type);

            if (viskit.ViskitStatics.debug) {
                LOG.info("Building Constr for: {}", type);
            }
            if (viskit.ViskitStatics.debug) {
                LOG.info("Required Parameters:");

                for (Object o : params) {

                    String s1 = "null";
                    if (o instanceof TerminalParameter) { // check if caller is sending assembly param types
                        s1 = ((TerminalParameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tAssembly TerminalParameter");
                        }
                    } else if (o instanceof MultiParameter) {
                        s1 = ((MultiParameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tAssembly MultiParameter");
                        }
                    } else if (o instanceof FactoryParameter) {
                        s1 = ((FactoryParameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tAssembly FactoryParameter");
                        }
                    } else if (o instanceof Parameter) { // from InstantiationPanel, this could also be an eventgraph param type?
                        s1 = ((Parameter) o).getType();
                        if (viskit.ViskitStatics.debug) {
                            System.out.print("\tEventGraph Parameter");
                        }
                    }
                    LOG.info("Parameter type: {}", s1);
                }
            }

            // gets lists of EventGraph parameters for type if top-level
            // or null if type is a basic class i.e., java.lang.Double
            List<Object>[] eparams = ViskitStatics.resolveParameters(ViskitStatics.classForName(type));
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
                if (viskit.ViskitStatics.debug)
                    LOG.info("{} VInstantiator using constructor #: {}", type, indx);
                
                // bug: weird case where params came in 0 length but no 0 length constuctors
                // happens if external class used as parameter?
                if (params.size() != eparams[indx].size()) {
                    args = buildInstantiators(eparams[indx]);
                    if (viskit.ViskitStatics.debug)
                        LOG.info("Warning: VInstantiator.Constr tried 0 length when it was more");
                    
                }
                if (eparams[indx] != null) {
                    // now that the values, types, etc. set, grab names from Event Graph parameters
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("args came back from buildInstantiators as: ");
                        for (Object arg : args)
                            LOG.info(arg);
                    
                    }
                    if (args != null) {
                        for (int j = 0; j < eparams[indx].size(); j++) {
                            if (viskit.ViskitStatics.debug)
                                LOG.info("setting name: {} ", ((Parameter) eparams[indx].get(j)).getName());
                            
                            ((ViskitModelInstantiator) args.get(j)).setName(((Parameter) eparams[indx].get(j)).getName());
                            ((ViskitModelInstantiator) args.get(j)).setDescription(ViskitStatics.emptyIfNull(((Parameter) eparams[indx].get(j)).getDescription()));
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
                ((ViskitModelInstantiator) args.get(i)).setName(names.get(i));
            }
        }

        private String listToString(List<String> lis) {
            StringBuilder sb = new StringBuilder("");
            for (String s : lis) {
                sb.append(s);
            }
            return sb.toString();
        }

        /**
         * @param assemblyParameters used to build the instantiators
         * @return a List of VInstantiators given a List of Assembly Parameters
         */
        final List<Object> buildInstantiators(List<Object> assemblyParameters) {

            String type, name;
            TerminalParameter tp;
            MultiParameter mp;
            FactoryParameter fp;
            List<Object> instrs = new ArrayList<>();
            ObjectFactory of = new ObjectFactory();
            for (Object o : assemblyParameters) {
                if (o instanceof TerminalParameter) {
                    instrs.add(buildTerminalParameter((TerminalParameter) o));
                } else if (o instanceof MultiParameter) {
                    instrs.add(buildMultiParameter((MultiParameter) o));
                } else if (o instanceof FactoryParameter) {
                    instrs.add(buildFactoryParameter((FactoryParameter) o));
                } else if (o instanceof Parameter) { // from InstantiationPanel Const getter
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("Conversion from " + ((Parameter) o).getType());
                    }

                    type = ((Parameter) o).getType();
                    name = ((Parameter) o).getName();

                    // TerminalParameter w/ special case for Object... (varargs)
                    if (ViskitStatics.isPrimitive(type) || type.contains("String") || type.contains("Object...")) {
                        tp = of.createTerminalParameter();
                        tp.setType(type);
                        tp.setName(name);
                        tp.setValue("");

                        instrs.add(buildTerminalParameter(tp));
                    } else if (ViskitStatics.numConstructors(type) > 0) { // MultiParameter
                        mp = of.createMultiParameter();
                        mp.setType(type);
                        mp.setName(name);

                        instrs.add(buildMultiParameter(mp));
                    } else { // no constructors, should be a FactoryParameter or array of them

                        if (ViskitGlobals.instance().isArray(type)) {
                            mp = of.createMultiParameter();
                            mp.setType(type);
                            mp.setName(name);
                            
                            instrs.add(buildMultiParameter(mp));
                        } else {
                            fp = of.createFactoryParameter();
                            fp.setName(name);
                            fp.setFactory(ViskitStatics.RANDOM_VARIATE_FACTORY_CLASS);
                            fp.setType(type); // this is the type returned by method
                            fp.setMethod(ViskitStatics.RANDOM_VARIATE_FACTORY_DEFAULT_METHOD);

                            instrs.add(buildFactoryParameter(fp));
                        }
                    }
                }
            }
            return instrs;
        }

        ViskitModelInstantiator.FreeF buildTerminalParameter(TerminalParameter p) {
            return new ViskitModelInstantiator.FreeF(p.getType(), p.getValue());
        }

        ViskitModelInstantiator.Array buildMultiParameter(MultiParameter p, boolean dummy) {
            List<Object> lis = p.getParameters();
            return new ViskitModelInstantiator.Array(p.getType(), buildInstantiators(lis));
        }

        ViskitModelInstantiator buildMultiParameter(MultiParameter p) {
            ViskitModelInstantiator vAorC;

            // Check for special case of varargs
            if (ViskitGlobals.instance().isArray(p.getType()) || p.getType().contains("..."))
                vAorC = buildMultiParameter(p, true);
            else {
                if (ViskitStatics.debug)
                    LOG.info("Trying to build MultiParameter: {}", p.getType());

                List<Object> tmpParams = p.getParameters();
                if (tmpParams.isEmpty())

                    // Likely, Diskit, or another library is not on the classpath
                    if (ViskitStatics.resolveParameters(ViskitStatics.classForName(p.getType())) == null)
                        return null;
                    else
                        tmpParams = ViskitStatics.resolveParameters(ViskitStatics.classForName(p.getType()))[0];
                
                if (ViskitStatics.debug) {
                    Iterator<Object> li = tmpParams.iterator();
                    while (li.hasNext())
                        LOG.info("Parameter: {}", li.next());
                }
                
                if (!p.getType().contains("simkit.stat")) // a PCL
                    vAorC = new ViskitModelInstantiator.Constr(tmpParams, p.getType());
                else
                    vAorC = this;
            }
            return vAorC;
        }

        ViskitModelInstantiator.Factory buildFactoryParameter(FactoryParameter p) {
            List<Object> lis = p.getParameters();
            return new ViskitModelInstantiator.Factory(
                    p.getType(), p.getFactory(), p.getMethod(),
                    buildInstantiators(lis));
        }

        final boolean paramsMatch(List<Object> aparams, List<Object> eparams) {
            if (aparams.size() != eparams.size()) {
                if (viskit.ViskitStatics.debug) {
                    LOG.info("No match.");
                }
                return false;
            }

            Object o;
            String aType, eType;
            Class<?> aClazz, eClazz;
            Class<?>[] vInterfz;
            boolean interfz, match;
            for (int i = 0; i < aparams.size(); i++) {
                o = aparams.get(i);
                eType = ((Parameter)eparams.get(i)).getType();
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
                if (viskit.ViskitStatics.debug) {
                    System.out.print("Type match " + aType + " to " + eType);
                }

                // check if vType was assignable from pType.

                eClazz = ViskitStatics.classForName(eType);
                aClazz = ViskitStatics.classForName(aType);
                vInterfz = aClazz.getInterfaces();
                interfz = false;
                for (Class<?> vInterfz1 : vInterfz) {
                    interfz |= eClazz.isAssignableFrom(vInterfz1);
                }
                match = (eClazz.isAssignableFrom(aClazz) | interfz);
                if (!match) {
                    if (viskit.ViskitStatics.debug) {
                        LOG.info("No match.");
                    }
                    return false;
                }
            }
            if (viskit.ViskitStatics.debug) {
                LOG.info("Match.");
            }
            return true;
        }

        /**
         * Find the names of the arguments
         * @param type
         * @param args List of VInstantiators
         * @return true if arg names have been found
         */
        private boolean findArgNames(String type, List<Object> args) {
            if (args == null) {
                setArgs(getDefaultArgs(type));
                args = getArgs();
            }
            return (indexOfArgNames(type, args) < 0);
        }

        /** Find a constructor match in the ClassLoader of the given Event Graph's parameters
         *
         * @param type the EventGraph to parameter check
         * @param args a list of Event Graph parameters
         * @return the index into the found matching constructor
         */
        public int indexOfArgNames(String type, List<Object> args) {
            List<Object>[] parameters = ViskitStatics.resolveParameters(ViskitStatics.classForName(type));
            int indx = -1;

            if (parameters == null) {
                return indx;
            }
            int ix = 0;

            if (viskit.ViskitStatics.debug) {
                LOG.info("args length " + args.size());
                LOG.info("resolveParameters " + type + " list length is " + parameters.length);
            }
            boolean match;
            String pType, vType;
            Class<?> pClazz, vClazz;
            Class<?>[] vInterfz;
            boolean interfz;
            for (List<Object> parameter : parameters) {
                if (viskit.ViskitStatics.debug) {
                    LOG.info("parameterLi.size() " + parameter.size());
                }
                if (parameter.size() == args.size()) {
                    match = true;
                    for (int j = 0; j < args.size(); j++) {

                        if (viskit.ViskitStatics.debug) {
                            LOG.info("touching " +
                                    ViskitStatics.convertClassName(
                                            ((Parameter)parameter.get(j)).getType())
                                    + " "
                                    + ((ViskitModelInstantiator) args.get(j)).getType());
                        }
                        pType = ViskitStatics.convertClassName(((Parameter)parameter.get(j)).getType());
                        vType = ((ViskitModelInstantiator) args.get(j)).getType();

                        // check if vType was assignable from pType.

                        pClazz = ViskitStatics.classForName(pType);

                        if (pClazz == null) {
                            ViskitGlobals.instance().getMainFrame().genericReport(JOptionPane.ERROR_MESSAGE, 
                                    "Basic Java Class Name Found",
                                    "<html><body><p align='center'>" +
                                    "Please check Event Graph <b>" + type + "</b> parameter(s) for compliance using" +
                                    " fully qualified Java class names.  " + pType + " should be a " +
                                    vType + ".</p></body></html>"
                            );
                            match = false;
                        } else {

                            vClazz = ViskitStatics.classForName(vType);
                            vInterfz = vClazz.getInterfaces();
                            interfz = false;
                            for (Class<?> clazz : vInterfz) {
                                //interfz |= vInterfz[k].isAssignableFrom(pClazz);
                                interfz |= pClazz.isAssignableFrom(clazz);
                            }

                            match &= (pClazz.isAssignableFrom(vClazz) | interfz);

                            // set the names, the final iteration of while cleans up
                            if (!((ViskitModelInstantiator) (args.get(j))).getName().equals(((Parameter)parameter.get(j)).getName()))
                                ((ViskitModelInstantiator) (args.get(j))).setName(((Parameter)parameter.get(j)).getName());
                            if (viskit.ViskitStatics.debug) {
                                LOG.info(" to " + ((Parameter)parameter.get(j)).getName());
                            }
                        }
                    }
                    if (match) {
                        indx = ix;
                        break;
                    }
                }
                ix++;
            }
            if (viskit.ViskitStatics.debug) {
                LOG.info("Resolving " + type + " " + parameters[indx] + " at index " + indx);
            }
            // the class manager caches Parameter List jaxb from the SimEntity.
            // If it didn't come from XML, then a null is returned.

            return indx;
        }

        private List<Object> getDefaultArgs(String type) {
            Class<?> clazz = ViskitStatics.classForName(type);
            if (clazz != null) {
                Constructor[] construct = clazz.getConstructors();
                if (construct != null && construct.length > 0) {

                    // TODO: May need to revisit why we are just concerned with
                    // the default zero param constructor
                    return ViskitModelInstantiator.buildDummyInstantiators(construct[0]);
                }
            }
            return new Vector<>(); // null
        }

        public List<Object> getArgs() {
            return args;
        }

        public final void setArgs(List<Object> args) {
            this.args = args;
        }

        @Override
        public String toString() {
            String rets = "new " + getType() + "(";
            rets = rets + (!args.isEmpty() ? ((ViskitModelInstantiator) args.get(0)).getType() + ",..." : "");
            return rets + ")";
        }

        @Override
        public ViskitModelInstantiator vcopy() {
            Vector<Object> lis = new Vector<>();
            ViskitModelInstantiator vi;
            for (Object o : args) {
                vi = (ViskitModelInstantiator) o;
                lis.add(vi);
            }
            ViskitModelInstantiator rv = new ViskitModelInstantiator.Constr(getType(), lis);
            rv.setName(this.getName());
            rv.setDescription(this.getDescription());
            return rv;
        }

        @Override
        public boolean isValid() {
            if (getType() == null || getType().isEmpty()) {
                return false;
            }
            ViskitModelInstantiator v;
            for (Object o : args) {
                v = (ViskitModelInstantiator) o;
                if (!v.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /* **********************************************************************/
    public static class Array extends ViskitModelInstantiator {

        private List<Object> instantiators; // array dimension == size()

        public Array(String typ, List<Object> inst) {
            super(typ);
            setInstantiators(inst);
        }

        @Override
        public ViskitModelInstantiator vcopy() {
            Vector<Object> lis = new Vector<>();
            for (Object vi : instantiators)
                lis.add((ViskitModelInstantiator) vi);
            
            ViskitModelInstantiator rv = new ViskitModelInstantiator.Array(getType(), lis);
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        public List<Object> getInstantiators() {
            return instantiators;
        }

        public final void setInstantiators(List<Object> instantiators) {
            this.instantiators = instantiators;
        }

        @Override
        public String toString() {
            if (instantiators != null) {
                if (getType().contains("Object...")) {
                    return getType();
                } else {
                    String t = getType().substring(0, getType().indexOf('['));
                    return "new " + t + "[" + instantiators.size() + "]";
                }
            } else {
                return "";
            }
        }

        @Override
        public boolean isValid() {
            if (getType() == null || getType().isEmpty()) {
                return false;
            }
            for (Object vi : instantiators) {
                if (!((ViskitModelInstantiator) vi).isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /* **********************************************************************/
    public static class Factory extends ViskitModelInstantiator {

        private String factoryClass;
        private String method;
        private List<Object> params;

        /** A factory for the ViskitModelInstantiator which carries information on what
 type of variable we need to provide for a SimEntity constructor.
         *
         * @param type Object type required by a SimEntity constructor
         * @param factoryClass the class that will return this type
         * @param method the method of the factoryClass that will return our desired type
         * @param params the parameters required to return the desired type
         */
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

        public final void setFactoryClass(String s) {
            this.factoryClass = s;
        }

        public final void setMethod(String m) {
            this.method = m;
        }

        public final void setParams(List<Object> p) {
            this.params = p;
        }

        @Override
        public String toString() {
            if (params.isEmpty()) {
                return "";
            }

            StringBuilder b = new StringBuilder();
            b.append(factoryClass);
            b.append(".");
            b.append(method);
            b.append("(");
            String args = null;
            for (Object o : params) {

                if (o instanceof ViskitModelInstantiator) {
                    args = ((ViskitModelInstantiator)o).type;
                } else if (o instanceof String) {
                    args = (String) o;
                }

                // Strip out java.lang
                args = ViskitStatics.stripOutJavaDotLang(args);

                // Show varargs symbol vice []
                if (ViskitGlobals.instance().isArray(args)) {
                    args = ViskitStatics.makeVarArgs(args);
                    b.append(args);
                } else {
                    b.append(args);
                }
                b.append(", ");
            }
            b = b.delete(b.lastIndexOf(", "), b.length());
            b.append(")");

            return b.toString();
        }

        @Override
        public ViskitModelInstantiator vcopy() {
            Vector<Object> lis = new Vector<>();
            ViskitModelInstantiator vi;
            for (Object o : params) {

                if (o instanceof ViskitModelInstantiator) {
                    vi = (ViskitModelInstantiator) o;
                    lis.add(vi);
                } else if (o instanceof String) {
                    lis.add(o);
                }
            }
            ViskitModelInstantiator rv = new ViskitModelInstantiator.Factory(getType(), getFactoryClass(), getMethod(), lis);
            rv.setName(getName());
            rv.setDescription(getDescription());
            return rv;
        }

        @Override
        public boolean isValid() {
            String t = getType(), fc = getFactoryClass(), m = getMethod();
            if (t == null || fc == null || m == null ||
                    t.isEmpty() || fc.isEmpty() || m.isEmpty()) {
                return false;
            }

            for (Object o : params) {

                if (o instanceof ViskitModelInstantiator) {
                    ViskitModelInstantiator v = (ViskitModelInstantiator) o;
                    if (!v.isValid()) {
                        return false;
                    }
                } else if (o instanceof String) {
                    if (((String) o).isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
    
} // end class ViskitModelInstantiator
