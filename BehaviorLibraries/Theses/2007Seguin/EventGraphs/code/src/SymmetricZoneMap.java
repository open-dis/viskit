package seadiver;

import diskit.Vec4d;
import diskit.X3DCoordinate;
import diskit.ZoneGeometry;
/*
 * SymmetricZoneMap.java
 *
 * Created on December 8, 2006, 12:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author John
 */
public class SymmetricZoneMap {

    private int firstMoverID;

    private int numMovers;

    private ZoneGeometry[] zone;

    private ZoneGeometry region;


    /**
     * Creates a new instance of SymmetricZoneMap
     */
    public SymmetricZoneMap(int numMovers, int firstMoverID, ZoneGeometry totalArea) {
        this.numMovers = numMovers;
        this.firstMoverID = firstMoverID;
        zone = new ZoneGeometry[numMovers];
        region = totalArea;
 //       System.out.println("totalArea:" + totalArea.getLength());

        createZones(numMovers, firstMoverID, totalArea);
    }

    public ZoneGeometry getZone(int moverID){
        return zone[moverID - firstMoverID];
    }


    private void createZones(int numMovers, int fMoverID, ZoneGeometry area) {
        int moverID = fMoverID;
        double zoneHeight;
        double zoneWidth;
        double zoneLength;
        int moversOnY;
        int moversOnX;
        double ratio;
        double width;
        double length;
        int idx = 0;

        length = area.getLength();
        width = area.getWidth();
        ratio = length/width;

        moversOnX =  (int) java.lang.Math.rint(java.lang.Math.sqrt(numMovers/ratio));
//        System.out.println(moversOnX);

        while(numMovers % moversOnX != 0){
            moversOnX += 1;
//            System.out.println(moversOnX);
        }

        moversOnY = numMovers / moversOnX;
//        System.out.println(moversOnX + "   " + moversOnY + "  " + ratio);

        zoneLength = length/moversOnX;
        zoneWidth = width/moversOnY;
        zoneHeight = 0.0;
        double halfWidth = zoneWidth/2;
        double halfLength = zoneLength/2;

        for(int i=0; i<moversOnY; i++){
            double yTranslation = 0;
            yTranslation = halfWidth + zoneWidth*i;

            for(int j=0; j<moversOnX; j++){
                zone[idx] = new ZoneGeometry(moverID,
                        new X3DCoordinate(halfLength + zoneLength*j, 0.0, yTranslation ),
                        zoneLength,
                        zoneWidth,
                        zoneHeight,
                        new Vec4d(0.0,0.0,-1,0.0));
                moverID += 1;
                idx += 1;
            }
        }
    }

    private int getFirstMoverID() {
        return firstMoverID;
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        for(int i=0; i<zone.length; i++){

            s.append("Zone").append(zone[i].getID()).append("\n");
            s.append("centerX3D").append("\t").append(zone[i].center).append("\n");
            s.append("length").append("\t\t").append(zone[i].length).append("\n");
            s.append("width").append("\t\t").append(zone[i].width).append("\n");
            s.append("startingPos").append("\t").append(zone[i].getCenter().getX()
                - zone[i].getLength()/2).append("\t").append(zone[i].getCenter().getZ()
                - zone[i].getWidth()/2).append("\t").append(zone[i].getCenter().getY()
                - zone[i].getHeight()/2).append("\t").append("\n\n");

        }
        return s.toString();
    }

     public String toString(int mID) {
        return "\nZone is {\t" + zone[mID].getID() + "\n\t" +
                zone[mID].getCenter() + "\n\t" +
                zone[mID].getLength() + "\n\t" +
                zone[mID].getWidth() + "\n\t" +
                zone[mID].getHeight() + "\n\t" +
                zone[mID].getAxisAngle() + "}";
    }
}
