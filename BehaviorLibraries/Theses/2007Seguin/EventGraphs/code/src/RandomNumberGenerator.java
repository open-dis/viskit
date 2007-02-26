package seadiver;

import simkit.random.RandomVariate;
/*
 * RandomNumberGenerator.java
 *
 * Created on December 29, 2006, 1:47 PM
 *
 * @author John Seguin
 */


public class RandomNumberGenerator {

    private RandomVariate[] rv;

    /**
     * Creates a new instance of RandomNumberGenerator
     */
    public RandomNumberGenerator(RandomVariate[] rv) {
        this.rv = rv;

    }

    public double genRandomNumber(int distribution) {
        if(distribution == 0) return rv[0].generate();
        else if(distribution == 1) return rv[1].generate();
        else return rv[2].generate();
    }

}
