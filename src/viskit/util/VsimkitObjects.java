package viskit.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import edu.nps.util.LogUtils;
import simkit.random.*;
import viskit.VStatics;

/**
 * A class to provide the beanshell parser in viskit with sample, throw-away
 * simkit instantiated objects on which to test conditions and transitions code
 * within events and edges.
 *
 * TODO: This needs work if we are to use it.  Currently, not used.
 *
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * @author Mike Bailey
 * @since Apr 27, 2004
 * @since 10:42:12 AM
 * @version $Id$
 */
public class VsimkitObjects {

    /**
     * VGlobals uses this field, which combines all the methods below.
     * It does not use the individual methods.
     */
    private static final Map<String, Object> HASH_MAP = new HashMap<>();

    static {
        try {
            Class<?> c = VStatics.classForName("viskit.VsimkitObjects");
            Method[] meths = c.getDeclaredMethods();
            for (Method method : meths) {
                String name = method.getName();

                // we can skip these
                if (name.equals(VStatics.RANDOM_VARIATE_FACTORY_METHOD) || name.equals("getFullName")) {
                    continue;
                }

                // lose the 'get_'
                name = name.replaceAll("get_", "");
                name = "simkit." + name.replace('_', '.');

                // with no package
                String noPackageName = name.substring(name.lastIndexOf('.') + 1);

                // TODO: this breaks on AR1Variate instance "getting"
                Object m = method.invoke(null, (Object[]) null);
                Object o = new FullNameAndInstance(name, m);
                LogUtils.getLogger(VsimkitObjects.class).debug("name is: " + name + " noPackageName is: " + noPackageName);
                HASH_MAP.put(name, o);
                HASH_MAP.put(noPackageName, o);
            }
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LogUtils.getLogger(VsimkitObjects.class).error(e);
//            e.printStackTrace();
        }
    }

    public static Object getInstance(String nm) throws Exception {
        Object o = HASH_MAP.get(nm);
        if (o == null) {
            throw new Exception("Class not found: " + nm);
        }
        return ((FullNameAndInstance) o).instance;
    }

    public static Object getFullName(String nm) {
        FullNameAndInstance fnai = (FullNameAndInstance) HASH_MAP.get(nm);
        if (fnai == null) {
            return null;
        }
        return fnai.fullname;
    }

    // Here are all the objects in the random package that could be used:
    // Each must start with "get_"
    public static Object get_random_Antithetic() {
        return new Antithetic();
    }

    public static Object get_random_AR1Variate() {
        return new AR1Variate();
    }

    public static Object get_random_BernoulliDistribution() {
        return new BernoulliDistribution();
    }

    public static Object get_random_BernoulliVariate() {
        return new BernoulliVariate();
    }

    public static Object get_random_BetaVariate() {
        return new BetaVariate();
    }

    public static Object get_random_BinomialVariate() {
        return new BinomialVariate();
    }

    public static Object get_random_BivariateNormal() {
        return new BivariateNormal();
    }

    public static Object get_random_Congruential() {
        return new Congruential();
    }

    public static Object get_random_ConstantVariate() {
        return new ConstantVariate();
    }

    public static Object get_random_ConvolutionVariate() {
        return new ConvolutionVariate();
    }

    public static Object get_random_DiscreteUniformVariate() {
        return new DiscreteUniformVariate();
    }

    public static Object get_random_DiscreteVariate() {
        return new DiscreteVariate();
    }

    public static Object get_random_ExponentialTransform() {
        return new ExponentialTransform();
    }

    public static Object get_random_ExponentialVariate() {
        return new ExponentialVariate();
    }

    public static Object get_random_FrequencyRandomObjectVariate() {
        return new FrequencyRandomObjectVariate();
    }

    public static Object get_random_GammaVariate() {
        return new GammaVariate();
    }

    public static Object get_random_GeometricVariate() {
        return new GeometricVariate();
    }

    public static Object get_random_InverseGaussianVariate() {
        return new InverseGaussianVariate();
    }

    public static Object get_random_LogTransform() {
        return new LogTransform();
    }

    public static Object get_random_MersenneTwister() {
        return new MersenneTwister();
    }

    public static Object get_random_MersenneTwisterDC() {
        return new MersenneTwisterDC();
    }

    public static Object get_random_MixedVariate() {
        return new MixedVariate();
    }

    public static Object get_random_Mother() {
        return new Mother();
    }

    public static Object get_random_NHPoissonProcessVariate() {
        return new NHPoissonProcessVariate();
    }

    public static Object get_random_Normal02Variate() {
        return new Normal02Variate();
    }

    public static Object get_random_Normal03Variate() {
        return new Normal03Variate();
    }

    public static Object get_random_NormalVariate() {
        return new NormalVariate();
    }

    public static Object get_random_NPPoissonProcessThinned() {
        return new NPPoissonProcessThinnedVariate();
    }

    public static Object get_random_NSSrng() {
        return new NSSrng();
    }

    public static Object get_random_OscillatingExponentialVariate() {
        return new OscillatingExponentialVariate();
    }

    public static Object get_random_PearsonTypeVVariate() {
        return new PearsonTypeVVariate();
    }

    public static Object get_random_PoissonVariate() {
        PoissonVariate pv = new PoissonVariate();
        pv.setMean(100.);
        return pv;
    }

    public static Object get_random_Pooled() {
        return get_random_PooledXORGenerator();
    }

    public static Object get_random_PooledXORGenerator() {
        return new PooledXORGenerator();
    }

    public static Object get_random_RandomNumber() {
        return get_random_Mother();
    }

    public static Object get_random_RandomPointGenerator() {
        return new RandomPointGenerator(new double[] {0.0d, 1.0d, 2.0d, 3.0d});
    }

    public static Object get_random_RandomVariate() {
        return get_random_PoissonVariate();
    }

    public static Object get_random_RandomVector() {
        return get_random_BivariateNormal();
    }

    public static Object get_random_RenewalProcessVariate() {
        return new RenewalProcessVariate();
    }

    public static Object get_random_ResampleVariate() {
        return new ResampleVariate();
    }

    public static Object get_random_RightWedgeVariate() {
        return new RightWedgeVariate();
    }

    public static Object get_random_RNG() {
        return new RNG();
    }

    public static Object get_random_ScaledVariate() {
        return new ScaledVariate();
    }

    public static Object get_random_Sequential() {
        return new Sequential();
    }

    public static Object get_random_Tausworthe() {
        return new Tausworthe();
    }

    public static Object get_random_TraceVariate() {
        return new TraceVariate();
    }

    public static Object get_random_TriangleVariate() {
        return new TriangleVariate();
    }

    public static Object get_random_TwoStateMarkovVariate() {
        return new TwoStateMarkovVariate();
    }

    public static Object get_random_UniformVariate() {
        return new UniformVariate();
    }

    public static Object get_random_WeibullVariate() {
        return new WeibullVariate();
    }
}

// TODO: All other simkit objects
class FullNameAndInstance {

    public String fullname;
    public Object instance;

    public FullNameAndInstance(String nm, Object o) {
        fullname = nm;
        instance = o;
    }
}
