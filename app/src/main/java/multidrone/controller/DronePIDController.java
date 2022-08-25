package multidrone.controller;

import multidrone.coordinates.*;

import java.net.DatagramSocket;

public class DronePIDController {

    final float refreshRateHz = 2;
    long sleepTime;
    private boolean active = false;

    private GlobalRefrencePoint globalRef;
    private NEDCoordinate targetCoord;

    private PIDControllerX xController = new PIDControllerX();
    private float xWP = 20;
    private float xWI = 0;
    private float xWD = 7_000;
    private float xIntegralWipeDist = 10;

    private PIDControllerY yController = new PIDControllerY();
    private float yWP = 20;
    private float yWI = 0;
    private float yWD = 7_000;
    private float yIntegralWipeDist = 10;

    private PIDControllerZ zController = new PIDControllerZ();
    private float zWP = 20*2.5f;
    private float zWI = 0;
    private float zWD = 8_000*1.5f;
    private float zIntegralWipeDist = 5;

    private PIDControllerYaw yawController = new PIDControllerYaw();
    private float yawWP = 2400;
    private float yawWI = 0;
    private float yawWD = 120_000;
    private float yawIntegralWipeDist = 10;

    private float targetYaw;
    private boolean useRoiForYaw = false;
    private NEDCoordinate roi;

    NEDCoordinate droneLoc;

    public void initialise(){
        xController.setWeights(xWP,xWI,xWD);
        xController.integralWipeDist = xIntegralWipeDist;

        yController.setWeights(yWP,yWI,yWD);
        yController.integralWipeDist = yIntegralWipeDist;

        zController.setWeights(zWP,zWI,zWD);
        zController.integralWipeDist = zIntegralWipeDist;

        yawController.setWeights(yawWP,yawWI,yawWD);
        yawController.integralWipeDist = yawIntegralWipeDist;


        setStartTime();
    }

    public void setGlobalRef(GlobalRefrencePoint ref){
        globalRef = ref;
    }

    public void setTargetCoord(NEDCoordinate _targetCoord){
        targetCoord = _targetCoord;
    }
    public void setTargetYaw(float yaw){
        targetYaw = yaw;
    }
    public void setROI(float lat, float lng, float alt){
        GeodeticCoordinate roiGEO = new GeodeticCoordinate(lat,lng,alt);
        roi = CoordinateTranslator.GeodeticToNED(roiGEO,globalRef);
        useRoiForYaw = true;
    }

    public void removeROI(){
        useRoiForYaw = false;
    }

    public void setTargetCoord(float lat, float lng, float alt){
        GeodeticCoordinate targetGEO = new GeodeticCoordinate(lat,lng,alt);
        targetCoord = CoordinateTranslator.GeodeticToNED(targetGEO,globalRef);
    }

    public void resetPIDController() {
        xController.resetPID();
        yController.resetPID();
        zController.resetPID();
        yawController.resetPID();
    }

    public void setPos(float lat, float lng, float alt, float yaw){
        GeodeticCoordinate droneGEO = new GeodeticCoordinate(lat,lng,alt);
        BodyCoordinate droneCoord = CoordinateTranslator.GeodeticToBody(new GeodeticCoordinate(droneGEO.lat,droneGEO.lng,droneGEO.height),globalRef,yaw);
        NEDCoordinate droneLocation = CoordinateTranslator.GeodeticToNED(new GeodeticCoordinate(droneGEO.lat,droneGEO.lng,droneGEO.height),globalRef);
        droneLoc = droneLocation;
        BodyCoordinate droneTarget = CoordinateTranslator.NEDToBody(targetCoord,yaw);
        xController.updateStateAndRef((float)droneCoord.x,System.currentTimeMillis(),(float)droneTarget.x);
        yController.updateStateAndRef((float)droneCoord.y,System.currentTimeMillis(),(float)droneTarget.y);
        zController.updateStateAndRef((float)droneCoord.z,System.currentTimeMillis(),(float)droneTarget.z);
        if (useRoiForYaw){
            yawController.updateStateAndRef(yaw,System.currentTimeMillis(),getTargetYaw(droneLocation.x,droneLocation.y,roi.x,roi.y));

            System.out.println("using ROI targ yaw " + getTargetYaw(droneLocation.x,droneLocation.y,roi.x,roi.y));
        } else{
            yawController.updateStateAndRef(yaw,System.currentTimeMillis(),targetYaw);
        }
    }

    private float getTargetYaw(double x, double y, double targX, double targY){
        double yaw = Math.atan2((targX - x),(targY - y)) - Math.PI/2;
        if (yaw < -Math.PI){
            yaw += Math.PI * 2;
        }
        return (float)-yaw;
    }
    public float getCameraPitch(){
        if (useRoiForYaw){
            return (float) -Math.toDegrees(Math.atan2(roi.z-droneLoc.z,Math.sqrt(Math.pow(roi.x-droneLoc.x,2) + Math.pow(roi.y-droneLoc.y,2))));
        } else{
            return 0;
        }

    }

    public void setStartTime(){
        xController.setStartTime(System.currentTimeMillis());
        yController.setStartTime(System.currentTimeMillis());
        zController.setStartTime(System.currentTimeMillis());
        yawController.setStartTime(System.currentTimeMillis());
    }

    public short[] getStickOutputs(){
        float xStick = -xController.getOutput();
        float yStick = -yController.getOutput();
        float zStick = zController.getOutput();
        float yawStick = yawController.getOutput();

        float xnorm = (float)Math.sqrt((xStick*xStick));
        float ynorm = (float)Math.sqrt((yStick*yStick));
        float znorm = (float)Math.sqrt((zStick*zStick));
        float wnorm = (float)Math.sqrt((yawStick*yawStick));

        float maxStick = 1000.0f;

        if (xnorm > maxStick)
        {
            xStick = xStick*(xnorm/maxStick);
        }
        if (ynorm > maxStick)
        {
            xStick = yStick*(ynorm/maxStick);
        }
        if (znorm > maxStick)
        {
            xStick = zStick*(znorm/maxStick);
        }
        if (wnorm > maxStick)
        {
            yawStick = yawStick*(wnorm/maxStick);
        }

        /*
        float xynorm = (float)Math.sqrt((xStick*xStick) + (yStick*yStick));

        if (xynorm > maxStick) //max value of each stick 1000 so 1000^2
        {
            xStick = (xStick/xynorm)*maxStick;
            yStick = (yStick/xynorm)*maxStick;
        }

        float zrnorm = (float)Math.sqrt((zStick*zStick) + (yawStick*yawStick));
        if (zrnorm > maxStick){
            zStick = (zStick/zrnorm)*maxStick;
            yawStick = (yawStick/zrnorm)*maxStick;
        }
        /*
         */
        System.out.println("sticks xyzr: " + (short)xStick + " " + (short)yStick + " " + (short)zStick + " " + (short)yawStick);
        //System.out.println("Xerrors  P D I : " + xController.error * xWP + " " + xController.getDifferential() * xWD + " " + xController.Isum * xWI);
        //System.out.println("Yerrors  P D I : " + yController.error * yWP + " " + yController.getDifferential() * yWD + " " + yController.Isum * yWI);
        //System.out.println("Zerrors P D I : " + zController.error * zWP + " " + zController.getDifferential() * zWD + " " + zController.Isum * zWI);
        //System.out.println("Yawerrors P D I : " + yawController.error * yawWP + " " + yawController.getDifferential() * yawWD + " " + yawController.Isum * yawWI);
        //System.out.println("drone xyz: " + droneLoc.x + " " + droneLoc.y + " " + droneLoc.z);
        //System.out.println("target xyz: " + targetCoord.x + " " + targetCoord.y + " " + targetCoord.z);
        return new short[] {(short)xStick,(short)yStick, (short)zStick, (short)yawStick};
    }

}
