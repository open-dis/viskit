package viskit.model;

import edu.nps.util.LogUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import viskit.VGlobals;
import viskit.VStatics;
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
public abstract class VInstantiator {

    static final Logger LOG = LogUtils.getLogger(VInstantiator.class);
    private String type;
    private String name = "";
    private String description = "";

    public VInstantiator(String typ) {
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

    public void setDescription(String desc) {
        description = desc;
    }

    public String getDescription() {
        return description;
    }

    abstract public VInstantiator vcopy();

    abstract public boolean isValid();

    public static Vector<Object> buildDummyInstantiators(Executable exe) {

        Vector<Object> v = new Vector<>();
        Class<?>[] cs = exe.getParameterTypes();
        String args;
        for (Class<?> c : cs) {
            args = VStatics.convertClassName(c.getName());

            // Show varargs symbol vice []
            args = VStatics.stripOutJavaDotLang(args);

            if (c.isArray()) {
                v.add(new VInstantiator.Array(args, new ArrayList<>()));

            // Special case, make a factory instantiator
            } /*else if (args.equals(VStatics.RANDOM_VARIATE)) {

                List<Object> params = new ArrayList<>();
                params.add("java.lang.String");
                params.add("java.lang.Object...");
                VInstantiator fac = new VInstantiator.Factory(args, VStatics.RANDOM_VARIATE_FACTORY, "getInstance", params);
                v.add(fac);
            } */else {
                v.add(new VInstantiator.FreeF(args, ""));
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

        public final void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public VInstantiator vcopy() {
            VInstantiator rv = new VInstantiator.FreeF(getType(), getValue());
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

    /***********************************************************************/
    public static class Constr extends VInstantiator {

        private List<Object> args;

        /** Takes a List of Assembly parameters and args for type
         *
         * @param params a list of Assembly parameters
         * @param type a parameter type
         */
        public Constr(List<Object> params, String type) {
            super(type);

            if (viskit.VStatics.debug) {
                LOG.info("Building Constr for " + type);
            }
            if (viskit.VStatics.debug) {
                LOG.info("Required Parameters:");

                for (Object o : params) {

                    String s1 = "null";
                    if (o instanceof TerminalParameter) { // check if caller is sending assembly param types
                        s1 = ((TerminalParameter) o).getType();
                        if (viskit.VStatics.debug) {
                            System.out.print("\tAssembly TerminalParameter");
                        }
                    } else if (o instanceof MultiParameter) {
                        s1 = ((MultiParameter) o).getType();
                        if (viskit.VStatics.debug) {
                            System.out.print("\tAssembly MultiParameter");
                        }
                    } else if (o instanceof FactoryParameter) {
                        s1 = ((FactoryParameter) o).getType();
                        if (viskit.VStatics.debug) {
                            System.out.print("\tAssembly FactoryParameter");
                        }
                    } else if (o instanceof Parameter) { // from InstantiationPanel, this could also be an eventgraph param type?
                        s1 = ((Parameter) o).getType();
                        if (viskit.VStatics.debug) {
                            System.out.print("\tEventGraph Parameter");
                        }
                    }
                    LOG.info(" " + s1);
                }
            }

            // gets lists of EventGraph parameters for type if top-level
            // or null if type is a basic class i.e., java.lang.Double
            // todo use same block as LegosTree to resolve any type
            List<Object>[] eparams = VStatics.resolveParameters(type);
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
                if (viskit.VStatics.debug) {
                    LOG.info(type + " VInstantiator using constructor #" + indx);
                }
                // bug: weird case where params came in 0 length but no 0 length constuctors
                // happens if external class used as parameter?
                if (params.size() != eparams[indx].size()) {
                    args = buildInstantiators(eparams[indx]);
                    if (viskit.VStatics.debug) {
                        LOG.info("Warning: VInstantiator.Constr tried 0 length when it was more");
                    }
                }
                if (eparams[indx] != null) {
                    // now that the values, types, etc set, grab names from eg parameters
                    if (viskit.VStatics.debug) {
                        LOG.info("args came back from buildInstantiators as: ");
                        for (Object arg : args) {
                            LOG.info(arg);
                        }
                    }
                    if (args != null) {
                        for (int j = 0; j < eparams[indx].size(); j++) {
                            if (viskit.VStatics.debug) {
                                LOG.info("setting name " + ((Parameter) eparams[indx].get(j)).getName());
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

            List<Object> instr = new ArrayList<>();
            for (Object o : assemblyParameters) {
                if (o instanceof TerminalParameter) {
                    instr.add(buildTerminalParameter((TerminalParameter) o));
                } else if (o instanceof MultiParameter) {
                    instr.add(buildMultiParameter((MultiParameter) o));
                } else if (o instanceof FactoryParameter) {
                    instr.add(buildFactoryParameter((FactoryParameter) o));
                } else if (o instanceof Parameter) { // from InstantiationPanel Const getter
                    if (viskit.VStatics.debug) {
                        LOG.info("Conversion from " + ((Parameter) o).getType());
                    }

                    String type = ((Parameter) o).getType();
                    String name = ((Parameter) o).getName();
                    ObjectFactory of = new ObjectFactory();

                    // TerminalParameter w/ special case for Object... (varargs)
                    if (VStatics.isPrimitive(type) || type.contains("String") || type.contains("Object...")) {
                        TerminalParameter tp = of.createTerminalParameter();
                        tp.setType(type);
                        tp.setName(name);
                        tp.setValue("");

                        instr.add(buildTerminalParameter(tp));

                    } else if (VStatics.numConstructors(type) > 0) { // MultiParameter

                        MultiParameter mp = of.createMultiParameter();
                        mp.setType(type);
                        mp.setName(name);

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
            return new VInstantiator.FreeF(p.getType(), p.getValue());
        }

        VInstantiator.Array buildMultiParameter(MultiParameter p, boolean dummy) {
            return new VInstantiator.Array(p.getType(), buildInstantiators(p.getParameters()));
        }

        VInstantiator buildMultiParameter(MultiParameter p) {
            VInstantiator vAorC;

            // Check for special case of varargs
            if (VGlobals.instance().isArray(p.getType()) || p.getType().contains("...")) {
                vAorC = buildMultiParameter(p, true);
            } else {
                if (VStatics.debug) {
                    LOG.info("Trying to buildMultiParamter " + p.getType());
                }
                List<Object> tmp = p.getParameters();

                if (tmp.isEmpty()) {

                    // Likely, Diskit, or another library is not on the classpath
                    if (VStatics.resolveParameters(p.getType()) == null) {
                        return null;
                    } else {
                        tmp = VStatics.resolveParameters(p.getType())[0];
                    }
                }
                Iterator<Object> li = tmp.iterator();
                if (VStatics.debug) {
                    while (li.hasNext()) {
                        LOG.info(li.next());
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

        final boolean paramsMatch(List<Object> aparams, List<Object> eparams) {
            if (aparams.size() != eparams.size()) {
                if (viskit.VStatics.debug) {
                    LOG.info("No match.");
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
                if (viskit.VStatics.debug) {
                    System.out.print("Type match " + aType + " to " + eType);
                }

                // check if vType was assignable from pType.

                Class<?> eClazz = VStatics.classForName(eType);
                Class<?> aClazz = VStatics.classForName(aType);
                Class<?>[] vInterfz = aClazz.getInterfaces();
                boolean interfz = false;
                for (Class<?> vInterfz1 : vInterfz) {
                    //interfz |= vInterfz[k].isAssignableFrom(eClazz);
                    interfz |= eClazz.isAssignableFrom(vInterfz1);
                }
                boolean match = (eClazz.isAssignableFrom(aClazz) | interfz);
                if (!match) {
                    if (viskit.VStatics.debug) {
                        LOG.info("No match.");
                    }
                    return false;
                }
            }
            if (viskit.VStatics.debug) {
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

        /** Find a constructor match in the ClassLoader of the given EG's parameters
         *
         * @param type the EventGraph to parameter check
         * @param args a list of EG parameters
         * @return the index into the found matching constructor
         */
        public int indexOfArgNames(String type, List<Object> args) {
            List<Object>[] parameters = VStatics.resolveParameters(type);
            int indx = -1;

            if (parameters == null) {
                return indx;
            }
            int ix = 0;

            if (viskit.VStatics.debug) {
                LOG.info("args length " + args.size());
                LOG.info("resolveParameters " + type + " list length is " + parameters.length);
            }
            for (List<Object> parameter : parameters) {
                if (viskit.VStatics.debug) {
                    LOG.info("parameterLi.size() " + parameter.size());
                }
                if (parameter.size() == args.size()) {
                    boolean match = true;
                    for (int j = 0; j < args.size(); j++) {

                        if (viskit.VStatics.debug) {
                            LOG.info("touching " +
                                    VStatics.convertClassName(
                                            ((Parameter) (parameter.get(j))).getType())
                                    + " "
                                    + ((VInstantiator) args.get(j)).getType());
                        }
                        String pType = VStatics.convertClassName(((Parameter) (parameter.get(j))).getType());
                        String vType = ((VInstantiator) args.get(j)).getType();

                        // check if vType was assignable from pType.

                        Class<?> pClazz = VStatics.classForName(pType);

                        if (pClazz == null) {
                            JOptionPane.showMessageDialog(null, "<html><body><p align='center'>" +
                                    "Please check Event Graph <b>" + type + "</b> parameter(s) for compliance using" +
                                    " fully qualified Java class names.  " + pType + " should be a " +
                                    vType + ".</p></body></html>",
                                    "Basic Java Class Name Found",
                                    JOptionPane.ERROR_MESSAGE);
                            match = false;
                        } else {

                            Class<?> vClazz = VStatics.classForName(vType);
                            Class<?>[] vInterfz = vClazz.getInterfaces();
                            boolean interfz = false;
                            for (Class<?> clazz : vInterfz) {
                                //interfz |= vInterfz[k].isAssignableFrom(pClazz);
                                interfz |= pClazz.isAssignableFrom(clazz);
                            }

                            match &= (pClazz.isAssignableFrom(vClazz) | interfz);

                            // set the names, the final iteration of while cleans up
                            if (!((VInstantiator) (args.get(j))).getName().equals(((Parameter) (parameter.get(j))).getName()))
                                ((VInstantiator) (args.get(j))).setName(((Parameter) (parameter.get(j))).getName());
                            if (viskit.VStatics.debug) {
                                LOG.info(" to " + ((Parameter) (parameter.get(j))).getName());
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
            if (viskit.VStatics.debug) {
                LOG.info("Resolving " + type + " " + parameters[indx] + " at index " + indx);
            }
            // the class manager caches Parameter List jaxb from the SimEntity.
            // If it didn't come from XML, then a null is returned.

            return indx;
        }

        private List<Object> getDefaultArgs(String type) {
            Class<?> clazz = VStatics.classForName(type);
            if (clazz != null) {
                Constructor[] construct = clazz.getConstructors();
                if (construct != null && construct.length > 0) {
                    return VInstantiator.buildDummyInstantiators(construct[0]);
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
            rets = rets + (args.size() > 0 ? ((VInstantiator) args.get(0)).getType() + ",..." : "");
            return rets + ")";
        }

        @Override
        public VInstantiator vcopy() {
            Vector<Object> lis = new Vector<>();
            for (Object o : args) {
                VInstantiator vi = (VInstantiator) o;
                lis.add(vi.vcopy());
            }
            VInstantiator rv = new VInstantiator.Constr(getType(), lis);
            rv.setName(this.getName());
            rv.setDescription(this.getDescription());
            return rv;
        }

        @Override
        public boolean isValid() {
            if (getType() == null || getType().isEmpty()) {
                return false;
            }
            for (Object o : args) {
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

        @Override
        public VInstantiator vcopy() {
            Vector<Object> lis = new Vector<>();
            for (Object vi : instantiators) {
                lis.add(((VInstantiator) vi).vcopy());
            }
            VInstantiator rv = new VInstantiator.Array(getType(), lis);
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

        /** A factory for the VInstantiator which carries information on what
         * type of variable we need to provide for a SimEntity constructor.
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

                if (o instanceof VInstantiator) {
                    args = ((VInstantiator)o).type;
                } else if (o instanceof String) {
                    args = (String) o;
                }

                // Strip out java.lang
                args = VStatics.stripOutJavaDotLang(args);

                // Show varargs symbol vice []
                if (VGlobals.instance().isArray(args)) {
                    args = VStatics.makeVarArgs(args);
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
        public VInstantiator vcopy() {
            Vector<Object> lis = new Vector<>();
            VInstantiator vi;
            for (Object o : params) {

                if (o instanceof VInstantiator) {
                    vi = (VInstantiator) o;
                    lis.add(vi.vcopy());
                } else if (o instanceof String) {
                    lis.add((String) o);
                }
            }
            VInstantiator rv = new VInstantiator.Factory(getType(), getFactoryClass(), getMethod(), lis);
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

                if (o instanceof VInstantiator) {
                    VInstantiator v = (VInstantiator) o;
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
}
