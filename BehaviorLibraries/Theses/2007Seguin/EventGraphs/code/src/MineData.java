/*
 * MineData.java
 *
 * Created on November 10, 2006, 8:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package seadiver;

/**
 *
 * @author jmseguin
 */
public class MineData {

    private double detectionTime;
    private double undetectionTime;
    private diskit.Vec3d detLocation;
    private diskit.Vec3d undetLocation;

    /** Creates a new instance of MineData */
    public MineData(double detTime, diskit.Vec3d detLoc) {
        this.setDetectionTime(detTime);
        this.setDetLocation(detLoc);

    }

    public void setDetectionTime(double dT) {
        this.detectionTime = dT;
    }

    public void setUndetectionTime(double uT) {
        this.undetectionTime = uT;
    }

    public void setDetLocation(diskit.Vec3d dL) {
        this.detLocation = dL;
    }

    public void setUndetLocation(diskit.Vec3d uL) {
        this.undetLocation = uL;
    }

    public double getDetectionTime() {
        return detectionTime;
    }

    public double getUndetectionTime() {
        return undetectionTime;
    }

    public diskit.Vec3d getDetLocation() {
        return detLocation;
    }

    public diskit.Vec3d getUndetLocation() {
        return undetLocation;
    }
}
