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
    // which is also a table of addition, eg,
    // ( i + j ) = A(i,j) % N
    
    // can be shown that any permution of 1..N-1 used as
    // i' = I(p(i)) and j' = J(q(j)) in A is Latin.
    
    // hence we can iterate through all I and J permutations rather quickly
    // and generate all Latins, or any random one almost instantly.
    // no memory matrix needs to be created, just indexes into virtual
    // rows and cols.
    
 */

package viskit.xsd.assembly;
import simkit.random.MersenneTwister;
import java.util.ArrayList;

/**
 *
 * @author Rick Goldberg
 */
public class LatinPermutator {
    MersenneTwister rnd;
    ArrayList set;
    int size;
    int[] row;
    int[] col;
    int rc,cc;
    int ct;
    
    //for testing stand-alone
    public static void main(String[] args) {
        LatinPermutator lp = new LatinPermutator(Integer.parseInt(args[0]));
        //output size number of randoms
        System.out.println("Output "+lp.size+" random LHS");
        for ( int j = 0; j < 10*lp.size; j++ ) {
            java.util.Date d = new java.util.Date();
            long time = d.getTime();
            lp.randomSquare();
            d = new java.util.Date();
            time -= d.getTime();
            System.out.println("Random Square:");
            lp.output();
            System.out.println("milliseconds : "+-1*time);
            System.out.println("---------------------------------------------");
        }
        
        //output series starting at base
        System.out.println("---------------------------------------------");
        System.out.println("Output bubbled LHS");
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
        //System.out.println("Row index: ");
        ////for ( int i = 0;  i < size; i++ ) {
        //System.out.print(row[i]+" ");
        //}
        //System.out.println();
        //System.out.println();
        //System.out.println("Col index: ");
        //for ( int i = 0;  i < size; i++ ) {
        //System.out.print(col[i]+" ");
        //}
        //System.out.println();
        //System.out.println();
        System.out.println();
        System.out.println("Square "+(ct++)+": ");
        for ( int i = 0;  i < size; i++ ) {
            System.out.println();
            for ( int j = 0; j < size; j++ ) {
                System.out.print(getAsubIJ(row[i],col[j])+" ");
            }
        }
        System.out.println();
    }
    
    void randomSquare() {
        ArrayList<Integer> r = new ArrayList<Integer>();
        ArrayList<Integer> c = new ArrayList<Integer>();
        
        for ( int i = 0; i < size; i ++) {
            r.add(new Integer(i));
            c.add(new Integer(i));
        }
        
        for ( int i = 0; i < size; i ++) {
            row[i] = r.remove((int) ((double) r.size() * rnd.draw())).intValue();
            col[i] = c.remove((int) ((double) c.size() * rnd.draw())).intValue();
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



