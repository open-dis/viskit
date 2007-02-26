package seadiver;

import diskit.*;

/**
 * Creates waypoints in a lawn mower pattern given a box of size length*width and a
 * starting position.
 * @author jmseguin
 * @version $Id$
 */
public class LawnMowerWaypointCreator {

    diskit.Vec3d[] waypoints;
    
    private int samples;
    
    Vec3d currentPosition;
    
    SeadiverToAVCL outputFile;

    private double depth;

    /**
     * Creates a new instance of LawnMowerWaypointCreator.
     * @param length double describing the long portion of the path.
     * @param width double describing the width along the short portion of the path.
     * @param startingPosition Vec3d of the initial starting position.
     * @param sensorRange double describing the range (radius) of the movers sensor.
     */
    public LawnMowerWaypointCreator(double length, double width, Vec3d startingPosition, double sensorRange)  {
        currentPosition = new Vec3d(startingPosition);
        generate(length, width, sensorRange);
    }

    /* Remember the X3DCoordinate in the ZoneGeometry is the center in (length, height, width)
     *
     */
    public LawnMowerWaypointCreator(
            diskit.Mover3D mover, 
            diskit.ZoneGeometry zone, 
            double sensorRange,
            String inputFilePathAndName,
            String outputFilePathAndName,
            double depth)  {
        
            double length = zone.getLength();
            double width = zone.getWidth();
            setDepth(depth);

            outputFile = new SeadiverToAVCL(mover.getMoverID());
            outputFile.setInputFile(inputFilePathAndName);
            outputFile.setOutputFile(outputFilePathAndName);

            currentPosition = new Vec3d(zone.getLowX(),zone.getLowY(),zone.getLowZ());

            outputFile.createSetPositionElement(
               String.valueOf(currentPosition.get(0)),
               String.valueOf(currentPosition.get(1)),
                    getDepth());

            outputFile.createThrusterElement();

            outputFile.createMakeKnotsElement(String.valueOf(mover.getMaximumSpeed()));

            generate(length, width, sensorRange);

            outputFile.generateAVCL();
    }

    // Coordinates in X3D valuse (X, +Y is up, Z).
    void generate(double xValue, double zValue, double sensorRange){

        Vec3d negDeltaY;
        Vec3d deltaX;
        Vec3d deltaY;
        samples = (int)(xValue/(sensorRange));
        waypoints = new diskit.Vec3d[samples];

        int it = 1;
        deltaY = new Vec3d(0.0, zValue, 0.0);
        deltaX = new Vec3d(sensorRange*2, 0.0, 0.0);

        int j = 0;
        for ( int i = 0 ; i < samples; i++ ) {

            if (i % 2 == 0)  //to get odd or even to determine which leg it is on.
            {
                if (j % 2 == 0) //again odd or even to check if going or coming
                {
                    currentPosition.add(deltaY);
                    waypoints[i] = new Vec3d(currentPosition);
                    j = j + 1;
                    outputFile.createWPelement(
                            String.valueOf(currentPosition.get(0)),
                            String.valueOf(currentPosition.get(1)));
                }
                else {
                    currentPosition.sub(deltaY);
                    waypoints[i] = new Vec3d(currentPosition);
                    j = j + 1;
                    outputFile.createWPelement(
                            String.valueOf(currentPosition.get(0)),
                            String.valueOf(currentPosition.get(1)));
                }
            }
            else {
                currentPosition.add(deltaX);
                waypoints[i] = new Vec3d(currentPosition);
                outputFile.createWPelement(
                            String.valueOf(currentPosition.get(0)),
                            String.valueOf(currentPosition.get(1)));
            }
        }
    }

    /**
     *
     * @return Array of Vec3d waypoints.
     */
    public Vec3d[] getWaypoints() {
        return waypoints;
    }

    public int length() {
        return samples;
    }

    public String toString() {
         String s = new String("LawnMowerWaypointCreator ");

         for (int i = 0; i<waypoints.length; i++){
             int j = 1;
            s += '\n'  + waypoints[i].toString() + "\t" + i; }
         return s;
    }
     private double getDepth() {
        return this.depth;
    }

    private void setDepth(double depth) {
        this.depth = depth;
    }

    /** Parses XML file and returns XML document.
      * @param fileName XML file to parse
      * @return XML document or <B>null</B> if error occured
      */

}
