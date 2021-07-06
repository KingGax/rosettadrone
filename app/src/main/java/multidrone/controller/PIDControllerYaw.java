package multidrone.controller;

public class PIDControllerYaw extends PIDOneVarController{
    public float integralWipeDist;
    private boolean rotatePositive = false;
    public PIDControllerYaw(){

    }

    @Override
    public float getOutput() {
        float integral = getIntegral();
        if (integralWipeDist < Math.abs(error)){
            integral = 0;
            Isum = 0;
        }
        if (getPositiveProp() ^ (error >= 0)){ //triggers if proportional sign does not match boolean using xor
            setPositiveProp((error >= 0));
            wipeIntegral();
        }
        float sum = getDifferential() * wD + integral * wI + error * wP;

        return (sum > 1000 ? 1000 : (sum < -1000 ? -1000 : sum));
    }

    @Override
    public void updateStateAndRef(float _state, long _time, float _ref) {
        lastTime = stateTime;
        lastError = error;
        ref = _ref;
        state = _state;
        stateTime = _time;
        error = getRotationalDistance(_state,_ref);
        setErrorAdded(false);
    }


    float getRotationalDistance(float yaw, float targetYaw){ //positive is clockwise
        return (float) (((targetYaw - yaw + 3*Math.PI) % (2*Math.PI))-Math.PI);
    }

}
