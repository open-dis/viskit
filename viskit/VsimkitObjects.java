package viskit;

import simkit.random.*;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * OPNAV N81 - NPS World Class Modeling (WCM)  2004 Projects
 * MOVES Institute
 * Naval Postgraduate School, Monterey, CA
 * www.nps.edu
 * By:   Mike Bailey
 * Date: Apr 27, 2004
 * Time: 10:42:12 AM
 */

/**
 * A class to provide the beanshell parser in viskit with sample, throw-away simkit instantiated objects
 * on which to test conditions and transitions code within events and edges.
 */

public class VsimkitObjects
{
  /**
   * VGlobals uses this, not the indiv. methods below.
   */
  public static HashMap hashmap = new HashMap();

  static {
    try {
      Class c = Class.forName("viskit.VsimkitObjects");
      Method[] meths = c.getDeclaredMethods();
      for(int i=0;i<meths.length;i++) {
        String nm = meths[i].getName();
        nm = nm.substring(4); // lose the 'get_'
        nm = "simkit."+nm.replace('_','.');

        Object o = meths[i].invoke(null,null);
        hashmap.put(nm,o);
      }
    }
    catch (Exception e) {
      System.err.println("VsimkitObjects error");
      System.err.println(e);
    }
  }

  // Here are all the objects in the random package that could be used:
  public static Object get_random_Antithetic ()              { return new  Antithetic (); }
  public static Object get_random_AR1Variate ()              { return new  AR1Variate (); }
  public static Object get_random_BernoulliDistribution ()   { return new  BernoulliDistribution (); }
  public static Object get_random_BernoulliVariate ()        { return new  BernoulliVariate (); }
  public static Object get_random_BetaVariate ()             { return new  BetaVariate (); }
  public static Object get_random_BinomialVariate ()         { return new  BinomialVariate (); }
  public static Object get_random_BivariateNormal ()         { return new  BivariateNormal (); }
  public static Object get_random_Congruential ()            { return new  Congruential (); }
  public static Object get_random_ConstantVariate ()         { return new  ConstantVariate (); }
  public static Object get_random_ConvolutionVariate ()      { return new  ConvolutionVariate (); }
  public static Object get_random_DiscreteUniformVariate ()  { return new  DiscreteUniformVariate (); }
  public static Object get_random_DiscreteVariate ()         { return new  DiscreteVariate (); }
  public static Object get_random_ExponentialTransform ()    { return new  ExponentialTransform ((RandomVariate)get_random_RandomVariate());}
  public static Object get_random_ExponentialVariate ()      { return new  ExponentialVariate (); }
  public static Object get_random_GammaVariate ()            { return new  GammaVariate (); }
  public static Object get_random_GeometricVariate ()        { return new  GeometricVariate (); }
  public static Object get_random_InverseGaussianVariate ()  { return new  InverseGaussianVariate (); }
  public static Object get_random_LogTransform ()            { return new  LogTransform ((RandomVariate)get_random_RandomVariate()); }
  public static Object get_random_MersenneTwister ()         { return new  MersenneTwister (); }
  public static Object get_random_MixedVariate ()            { return new  MixedVariate (); }
  public static Object get_random_Mother ()                  { return new  Mother (); }
  public static Object get_random_NHPoissonProcessVariate () { return new  NHPoissonProcessVariate (); }
  public static Object get_random_Normal02Variate ()         { return new  Normal02Variate (); }
  public static Object get_random_Normal03Variate ()         { return new  Normal03Variate (); }
  public static Object get_random_NormalVariate ()           { return new  NormalVariate (); }
  public static Object get_random_NSSrng ()                  { return new  NSSrng (); }
  public static Object get_random_OscillatingExponentialVariate () { return new  OscillatingExponentialVariate (); }
  public static Object get_random_PearsonTypeVVariate ()     { return new  PearsonTypeVVariate (); }
  public static Object get_random_PoissonVariate ()          { PoissonVariate pv = new PoissonVariate(); pv.setMean(100.);return pv; }
  public static Object get_random_Pooled ()                  { return get_random_PooledXORGenerator(); }
  // depr public static Object get_random_PooledGenerator ()         { return new  PooledGenerator (); }
  public static Object get_random_PooledXORGenerator ()      { return new  PooledXORGenerator (); }
  public static Object get_random_RandomNumber ()            { return get_random_Mother (); }
  public static Object get_random_RandomPointGenerator ()    { return new  RandomPointGenerator (new double[]{0.,1.,2.,3.}); }
  public static Object get_random_RandomVariate ()           { return get_random_PoissonVariate();}
  public static Object get_random_RandomVector ()            { return get_random_BivariateNormal(); }
  public static Object get_random_RenewalProcessVariate ()   { return new  RenewalProcessVariate (); }
  public static Object get_random_ResampleVariate ()         { return new  ResampleVariate (); }
  public static Object get_random_RightWedgeVariate ()       { return new  RightWedgeVariate (); }
  public static Object get_random_RNG ()                     { return new  RNG (); }
  public static Object get_random_ScaledVariate ()           { return new  ScaledVariate (); }
  public static Object get_random_Sequential ()              { return new  Sequential (); }
  public static Object get_random_Tausworthe ()              { return new  Tausworthe (); }
  public static Object get_random_TraceVariate ()            { return new  TraceVariate (); }
  public static Object get_random_TriangleVariate ()         { return new  TriangleVariate (); }
  public static Object get_random_TwoStateMarkovVariate ()   { return new  TwoStateMarkovVariate (); }
  public static Object get_random_UniformVariate ()          { return new  UniformVariate (); }
  public static Object get_random_WeibullVariate ()          { return new  WeibullVariate (); }

  // todo All other simkit objects
}
