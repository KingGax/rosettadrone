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
    private float xWP = 25;
    private float xWI = 0;
    private float xWD = 9_000;
    private float xIntegralWipeDist = 10;

    private PIDControllerY yController = new PIDControllerY();
    private float yWP = 25;
    private float yWI = 0;
    private float yWD = 9_000;
    private float yIntegralWipeDist = 10;

    private PIDControllerZ zController = new PIDControllerZ();
    private float zWP = 20;
    private float zWI = 0;
    private float zWD = 8_000;
    private float zIntegralWipeDist = 5;

    private PIDControllerYaw yawController = new PIDControllerYaw();
    private float yawWP = 1200;
    private float yawWI = 0;
    private float yawWD = 80_000;
    private float yawIntegralWipeDist = 10;

    private float targetYaw;

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
        yawController.updateStateAndRef(yaw,System.currentTimeMillis(),targetYaw);
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
        System.out.println("sticks xyzr: " + (short)xStick + " " + (short)yStick + " " + (short)zStick + " " + (short)yawStick);
        System.out.println("Xerrors  P D I : " + xController.error * xWP + " " + xController.getDifferential() * xWD + " " + xController.Isum * xWI);
        System.out.println("Yerrors  P D I : " + yController.error * yWP + " " + yController.getDifferential() * yWD + " " + yController.Isum * yWI);
        System.out.println("Zerrors P D I : " + zController.error * zWP + " " + zController.getDifferential() * zWD + " " + zController.Isum * zWI);
        System.out.println("Yawerrors P D I : " + yawController.error * yawWP + " " + yawController.getDifferential() * yawWD + " " + yawController.Isum * yawWI);
        System.out.println("drone xyz: " + droneLoc.x + " " + droneLoc.y + " " + droneLoc.z);
        System.out.println("target xyz: " + targetCoord.x + " " + targetCoord.y + " " + targetCoord.z);
        return new short[] {(short)xStick,(short)yStick, (short)zStick, (short)yawStick};
    }

}
