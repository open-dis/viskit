package seadiver;

import diskit.Vec3d;
import diskit.ZoneGeometry;
import java.util.LinkedList;
//import seadiver.RandomNumberGenerator;

/*
 * TargetWaypointCreator.java
 *
 * Created on December 29, 2006, 2:27 PM
 *
 * @version $Id$
 * @author John Seguin
 */

public class TargetWaypointCreator {

    private int numTargets;

    private LinkedList zoneList;

    private RandomNumberGenerator rvGen;

    private LinkedList waypointList;

    private int xDist = 0;

    private int yDist = 1;

    private Vec3d origin;

    SeadiverToAVCL[] outputFile;

    private double speed;
    
    private double depth;

    private String inputFileName;

    private String outputFileName;

    /** Creates a new instance of TargetWaypointCreator */
    public TargetWaypointCreator(
            LinkedList zoneList,
            int numberTargets,
            RandomNumberGenerator rvGen,
            double speed,
            String inputFilePathAndName,
            String outputFilePathAndName,
            double depth) 
    {
        setNumberTargets(numberTargets);
        setZoneList(zoneList);
        setSpeed(speed);
        setInputFileName(inputFilePathAndName);
        setOutputFileName(outputFilePathAndName);
        setDepth(depth);
        this.rvGen = rvGen;
        waypointList = new LinkedList();
        outputFile = new SeadiverToAVCL[numberTargets];
        
        for(int i =400; i<(getNumberTargets()+400); i++){
            outputFile[i-400] = new SeadiverToAVCL(i);
            outputFile[i-400].setInputFile(getInputFileName());
        }        
        
        generateWaypoints();

        writeAVCL();
    }

    private void setNumberTargets(int numberTargets) {
        this.numTargets = numberTargets;
    }

    private int getNumberTargets(){
        return this.numTargets;
    }

    private void setZoneList(LinkedList zoneList) {
        this.zoneList = zoneList;
    }

    private void generateWaypoints() {
        for(int i=0; i<this.numTargets; i++){

            LinkedList temp;
            temp = new LinkedList(zoneList);
            Vec3d[] waypoints = new Vec3d[zoneList.size()+2];

            ZoneGeometry zone = (ZoneGeometry) temp.getFirst();

            double a = zone.getLength()*rvGen.genRandomNumber(xDist);
            double b = zone.getWidth()*rvGen.genRandomNumber(yDist);
            double c = 0.0;

            double lowX;
            double lowY;
            double lowZ;

            lowX = zone.getCenter().getX() - zone.getLength()/2;
            lowY = zone.getCenter().getZ() - zone.getWidth()/2;
            lowZ = zone.getCenter().getY() - zone.getHeight()/2;

            //Adds the first waypoint prior to target entering area.
            origin = new Vec3d(lowX, lowY - zone.getWidth(), lowZ);
            origin.add(new Vec3d(a,b,c));
            waypoints[0] = origin;

            //Adds a waypoint for each zone.
            int j = 1;
            while (!temp.isEmpty()){

                zone = (ZoneGeometry) temp.removeFirst();

                double x = zone.getLength()*rvGen.genRandomNumber(xDist);
                double y = zone.getWidth()*rvGen.genRandomNumber(yDist);
                double z = 0.0;

                lowX = zone.getCenter().getX() - zone.getLength()/2;
                lowY = zone.getCenter().getZ() - zone.getWidth()/2;
                lowZ = zone.getCenter().getY() - zone.getHeight()/2;

//                System.out.println("centerZ is: "+ zone.getCenter().getZ());
//                System.out.println("centerY is: "+ zone.getCenter().getY());
//                System.out.println("lowY(Z) is: "+ lowY);
                
                origin = new Vec3d(lowX, lowY , lowZ);
                origin.add(new Vec3d(x,y,z));
                waypoints[j] = origin;

                j=j+1;
            }
                // Adds final wp to get target out of area.
            if(temp.isEmpty()){
                lowX = zone.getCenter().getX() - zone.getLength()/2;
                lowY = zone.getCenter().getZ() - zone.getWidth()/2;
                lowZ = zone.getCenter().getY() - zone.getHeight()/2;
                origin = new Vec3d(lowX, lowY + zone.getWidth(), lowZ);
                double x = zone.getLength()*rvGen.genRandomNumber(xDist);
                double y = zone.getWidth()*rvGen.genRandomNumber(yDist);
                double z = 0.0;
                origin.add(new Vec3d(x,y,z));
                waypoints[j] = origin;
            }
            waypointList.add(waypoints);
        }
    }
    public Vec3d[] getWaypoints(int moverID) {
        return (Vec3d[]) waypointList.get(moverID);
    }
    public Vec3d getStartPosition(int moverID) {
        Vec3d[] tempList = this.getWaypoints(moverID);
        return tempList[0];
    }
    public Vec3d getFirstWaypoint(int moverID) {
        Vec3d[] tempList = this.getWaypoints(moverID);
        return tempList[1];
    }
    public String printWaypoints(int moverID) {
        Vec3d[] temp = (Vec3d[])waypointList.get(moverID);

         String s = new String("TargetWaypointCreator waypoints are:");

         for (int i = 0; i<temp.length; i++){
            s += '\n'  + temp[i].toString() + "\t" + i; }
         return s;
    }

    private void writeAVCL() {
        for(int i =400; i<(getNumberTargets()+400); i++){
   
            Vec3d[] temp = (Vec3d[])waypointList.get(i-400);
            
            outputFile[i-400].createSetPositionElement(
               String.valueOf(temp[0].get(0)),
               String.valueOf(temp[0].get(1)),
                    getDepth());

            outputFile[i-400].createThrusterElement();

            outputFile[i-400].createMakeKnotsElement(
                    String.valueOf(getSpeed()));

            for(int j = 1; j<temp.length; j++) {
                outputFile[i-400].createWPelement(
                    String.valueOf(temp[j].get(0)),
                    String.valueOf(temp[j].get(1)));
            }
            
            outputFile[i-400].setOutputFile(getOutputFileName());
            outputFile[i-400].generateAVCL();
//            System.out.println("Created mission file for: " + i);
        }
    }

    private void setSpeed(double speed) {
        this.speed = speed;
    }

    private double getSpeed() {
        return this.speed;
    }

    private double getDepth() {
        return this.depth;
    }

    private void setDepth(double depth) {
        this.depth = depth;
    }

    private String getInputFileName() {
        return this.inputFileName;
    }

    private String getOutputFileName() {
        return this.outputFileName;
    }

    private void setOutputFileName(String fileName) {
        this.outputFileName = fileName;
    }

    private void setInputFileName(String fileName) {
        this.inputFileName = fileName;
    }


}
