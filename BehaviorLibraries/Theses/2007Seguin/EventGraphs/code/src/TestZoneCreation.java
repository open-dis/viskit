package seadiver;
/**
 *
 * @author John
 */
public class TestZoneCreation {

    private double length;
    private double width;
    double ratio;
    int numMovers;

    private int moversOnX = 0;

    private double moversOnXint = 0.0;

    private int moversOnY = 0;

    /** Creates a new instance of TestZoneCreation */
    public TestZoneCreation() {

    }

    public void createZones(double length, double width, int numMovers) {
        ratio = length/width;

        moversOnX =  (int) java.lang.Math.rint(java.lang.Math.sqrt(numMovers/ratio));
        System.out.println(moversOnX);
//        moversOnXint = (numMovers/ratio);
//        moversOnX = (int) java.lang.Math.round(moversOnXint);


        while(numMovers % moversOnX != 0){
            moversOnX += 1;
            System.out.println(moversOnX);
        }

        moversOnY = numMovers / moversOnX;

        System.out.println(moversOnX + "   " + moversOnY + "  " + ratio);

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TestZoneCreation test = new TestZoneCreation();
        test.createZones(2000, 450, 30);

    }

}
