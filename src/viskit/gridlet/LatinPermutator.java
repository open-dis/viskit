/*
 * LatinPermutator.java
 *
 * Created on January 26, 2006, 2:27 PM
 *
 * Refactored from SimkitAssemblyXML2Java
 *
    // LatinPermutator -
    // any swap of two rows or two columns in a LHS is a LHS
    // start with a base LHS, where
    // A(i,j) = [ i + (N-j) % N ] % N
    //
    // which is also a table of addition, for example,
    // ( i + j ) = A(i,j) % N

    // can be shown that any permution of 1..N-1 used as
    // i' = I(p(i)) and j' = J(q(j)) in A is Latin.

    // hence we can iterate through all I and J permutations rather quickly
    // and generate all Latins, or any random one almost instantly.
    // no memory matrix needs to be created, just indexes into virtual
    // rows and cols.

 */

package viskit.gridlet;

import edu.nps.util.Log4jUtilities;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import simkit.random.MersenneTwister;

/**
 *
 * @author Rick Goldberg
 */
public class LatinPermutator 
{
    static final Logger LOG = Log4jUtilities.getLogger(LatinPermutator.class);
    
    MersenneTwister rnd;
    int size;
    int[] row;
    int[] col;
    int rc,cc;
    int ct;

    //for testing stand-alone
    public static void main(String[] args) 
    {
        LatinPermutator lp = new LatinPermutator(Integer.parseInt(args[0]));
        //output size number of randoms
        LOG.info("Output "+lp.size+" random LHS");
        for ( int j = 0; j < 10*lp.size; j++ ) {
            java.util.Date d = new java.util.Date();
            long time = d.getTime();
            lp.randomSquare();
            d = new java.util.Date();
            time -= d.getTime();
            LOG.info("Random Square:");
            lp.output();
            LOG.info("milliseconds : "+-1*time);
            LOG.info("---------------------------------------------");
        }

        //output series starting at base
        LOG.info("---------------------------------------------");
        LOG.info("Output bubbled LHS");
        lp.ct=0;
        //bubbles not perfect, hits some squares more than once, not all squares
        //possible with only single base
        lp.bubbles();

    }

    public LatinPermutator(int size) {
        rnd = new MersenneTwister();
        this.size=size;
        row = new int[size];
        col = new int[size];
        rc = cc = size-1;
        ct=0;
    }

    int getAsubIJ(int i, int j) {
        return
                (i + ((size - j)%size))%size;
    }

    // not really used except for test as per main()
    void bubbles() {
        int i;
        for ( i = 0; i < size; i++ ) {
            row[i] = col[i] = i;
        }
        output();
        i = size;
        while ( i-- > 0 ) {
            while (bubbleRow()) {
                output();
                while (bubbleCol()) {
                    output();
                }
            }
        }
    }

    // not really used except for test as per bubbles() in main()
    boolean bubbleRow() {
        int t;
        if ( rc < 1 ) {
            rc = size-1;
            return false;
        }
        t = row[rc];
        row[rc] = row[rc-1];
        row[rc-1] = t;
        rc--;
        return true;
    }

    // not really used except for test as per bubbles() in main()
    boolean bubbleCol() {
        int t;
        if ( cc < 1 ) {
            cc = size-1;
            return false;
        }
        t = col[cc];
        col[cc] = col[cc-1];
        col[cc-1] = t;
        cc--;
        return true;
    }

    void output() {
        //LOG.info("Row index: ");
        ////for ( int i = 0;  i < size; i++ ) {
        //System.out.print(row[i]+" ");
        //}
        //LOG.info();
        //LOG.info();
        //LOG.info("Col index: ");
        //for ( int i = 0;  i < size; i++ ) {
        //System.out.print(col[i]+" ");
        //}
        LOG.info("Square "+(ct++)+": ");
        for ( int i = 0;  i < size; i++ ) {
            for ( int j = 0; j < size; j++ ) {
                System.out.print(getAsubIJ(row[i],col[j])+" ");
            }
        }
    }

    void randomSquare() {
        List<Integer> r = new ArrayList<>();
        List<Integer> c = new ArrayList<>();

        for ( int i = 0; i < size; i ++) {
            r.add(i);
            c.add(i);
        }

        for ( int i = 0; i < size; i ++) {
            row[i] = r.remove((int) (r.size() * rnd.draw()));
            col[i] = c.remove((int) (c.size() * rnd.draw()));
        }
    }

    int[][] getRandomLatinSquare() {
        int[][] square = new int[size][size];
        randomSquare();
        for ( int i = 0;  i < size; i++ ) {
            for ( int j = 0; j < size; j++ ) {
                square[i][j]=getAsubIJ(row[i],col[j]);
            }
        }

        output();
        return square;
    }

    void setSeed(long seed) {
        rnd.setSeed(seed);
    }

}



