package seadiver;
/**
 *
 * @author John
 */
public class TestSymetricZoneMap {

    static int numMovers;
    static int firstMoverID;
    static diskit.ZoneGeometry area;

    /** Creates a new instance of TestSymetricZoneMap */
    public TestSymetricZoneMap() {
    }

     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        numMovers = 20;
        firstMoverID = 100;
        area = new diskit.ZoneGeometry(1, new diskit.X3DCoordinate(500,0,300), 1000, 600, 0, new diskit.Vec4d(0,0,-1,0));

        SymmetricZoneMap map = new SymmetricZoneMap(numMovers,firstMoverID,area);

        System.out.println( map.toString());
//        System.out.println("Mover 2 toString is:" + map.toString(1));
//        System.out.println("Mover 3 toString is" + map.toString(2) + "/n");
//        System.out.println("Mover 4 toString is" + map.toString(3) + "/n");
 //       System.out.println("Mover 5 toString is" + map.toString(4) + "/n");
    }

}
