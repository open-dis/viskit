package seadiver;
import diskit.Mover3D;

/**
 * Class that tests LawnMowerWaypointCreator.
 *
 * @author John Seguin
 */
public class TestLawnMowerWaypointCreator {
    
    private static String inputFile = "G:/ThesisCode/MissionFiles/SeaDiverTemplate.xml";

    private static String outputFile = "G:/ThesisCode/MissionFiles/Target";

    public TestLawnMowerWaypointCreator() {
    }

    public static void main(String[] args) {
        diskit.DISMover3D mover;
        mover = new diskit.DISMover3D(new diskit.Vec3d(0.0,0.0,0.0), 4.0, 10);
        double depth = 50.0;
        double sensorRange = 10.0;

        diskit.ZoneGeometry zone = new diskit.ZoneGeometry(1, new diskit.X3DCoordinate(500.0,0.0,300.0), 1000.0, 600.0, 0.0, new diskit.Vec4d(0,0,-1,0));

        LawnMowerWaypointCreator tester = new LawnMowerWaypointCreator(mover, zone, sensorRange,inputFile,outputFile, depth);
        System.out.println(tester.toString());

    }

}
