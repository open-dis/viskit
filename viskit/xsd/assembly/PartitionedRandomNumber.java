/*
 * PartitionedRandomNumber.java
 *
 * Created on July 10, 2006, 10:54 AM
 *
 * Wraps a RandomNumber to partition the outcome within
 * N equally lengthed intervals.
 * 
 * This is used for instance in an LHS, where
 * a distribution is to be sampled within each partition.
 * Setting the partition number in effect scales by
 * 1/N and shifts by n/N the draw() outcome.
 *
 * Use in grid-mode:
 *
 * 1. set RandomVariateFactory's default RandomNumber class
 * by RandomVariateFactory.setDefaultClass("viskit.xsd.assembly.PartitionedRandomNumber");
 *
 * 2. before each series of Replications in a DesignPoint as given by row of LHS, 
 * set the partition for each respective RandomVariate in the row by the row value.
 * ie, for some RV, rv[i].getRandomNumber().setCurrentPartition(row[i]);
 *
 * repeat for each DesignPoint in the LHS. 
 *
 * This would be done in Gridlet's run method once for each RV in the Assembly, once
 * because a Gridlet only runs a single DesignPoint (through so many replications).
 */

package viskit.xsd.assembly;
import simkit.random.RandomNumber;

/**
 *
 * @author Rick Goldberg
 *
 */

public class PartitionedRandomNumber implements simkit.random.RandomNumber {
    RandomNumber base;
    int partitions;
    int currentPartition;
    
    public PartitionedRandomNumber() {
        this(new simkit.random.MersenneTwister(),1);
    }
    /** Creates a new instance of PartitionedRandomNumber */
    public PartitionedRandomNumber(RandomNumber base, int partitions) {
        setBase(base);
        setPartitions(partitions);
        setCurrentPartition(0);
    }
    
    public void setBase(RandomNumber base) {
        this.base=base;
    }
    
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }
    
    public void setCurrentPartition(int partition) {
        this.currentPartition = partition;
    }

    public void setSeed(long l) {
        base.setSeed(l);
    }

    public long getSeed() {
        return base.getSeed();
    }

    public void resetSeed() {
        base.resetSeed();
    }

    public void setSeeds(long[] l) {
        base.setSeeds(l);
    }

    public long[] getSeeds() {
        return base.getSeeds();
    }

    public double draw() {
        double rn = base.draw();
        rn = rn/(double)partitions;
        rn += (double)currentPartition/(double)partitions;
        return rn;
    }

    public long drawLong() {
        long rn = base.drawLong();
        rn = rn/(long)partitions;
        rn += (long)currentPartition/(long)partitions;
        return rn;
    }

    public double getMultiplier() {
        return base.getMultiplier();
    }

}
