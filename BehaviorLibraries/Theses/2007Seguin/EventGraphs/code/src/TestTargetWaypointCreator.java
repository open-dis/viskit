package seadiver;

import diskit.Vec4d;
import diskit.X3DCoordinate;
import diskit.ZoneGeometry;
import java.util.LinkedList;
//import seadiver.RandomNumberGenerator;

import simkit.random.RandomVariate;
import simkit.random.RandomVariateFactory;
/*
 * TestTargetWaypointCreator.java
 *
 * Created on December 29, 2006, 3:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author John
 */
public class TestTargetWaypointCreator {

    private static TargetWaypointCreator test;

    private static String inputFile = "G:/ThesisCode/MissionFiles/SeaDiverTemplate.xml";

    private static String outputFile = "G:/ThesisCode/MissionFiles/Target.xml";

    /** Creates a new instance of TestTargetWaypointCreator */
    public TestTargetWaypointCreator() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        double speed = 10.0;
        double depth = 100.0;
        RandomNumberGenerator rvGen;
        int nTar = 2;
        LinkedList zList;
        RandomVariate[] rv = new RandomVariate[3];
        rv[0] = RandomVariateFactory.getInstance(
        "Uniform", new Object[] { new Double(0.0), new Double(1.0) });
        rv[1] = RandomVariateFactory.getInstance(
        "Uniform", new Object[] { new Double(0.0), new Double(1.0) });
        rv[2] = RandomVariateFactory.getInstance("Exponential", new Object[] {3.2});
        rvGen = new RandomNumberGenerator(rv);
        zList = new LinkedList();
        zList.add(new ZoneGeometry(1,new X3DCoordinate(5000,0,1500),10000,3000,0,new Vec4d(0,0,-1,0)));
        zList.add(new ZoneGeometry(2,new X3DCoordinate(5000,0,4500),10000,3000,0,new Vec4d(0,0,-1,0)));

        test = new TargetWaypointCreator(zList, nTar, rvGen, speed, inputFile, outputFile,depth);
        System.out.println(test.printWaypoints(0));
        System.out.println(test.printWaypoints(1));
    }

}
