package multidrone.controller;

public abstract class PIDOneVarController {
    public float lastError;
    public long lastTime;
    public float wP;
    public float wI;
    public float wD;
    public float ref;
    public float state;
    public float error;
    public long stateTime;
    public float Isum;
    private boolean errorAdded = false;
    private boolean positiveProp = false;

    public abstract float getOutput();

    public void setStartTime(long _time){
        stateTime = _time;
    }

    protected void setPositiveProp(boolean prop){
        positiveProp = prop;
    }

    protected void setErrorAdded(boolean val){
        errorAdded = val;
    }

    public void setWeights(float _wP, float _wI, float _wD){
        wP = _wP;
        wI = _wI;
        wD = _wD;
    }

    public void resetPID(){
        error = 0;
        lastError = 0;
        ref = 0;
        state = 0;
        Isum = 0;

    }

    protected void wipeIntegral(){
        Isum = 0;
    }

    protected boolean getPositiveProp(){
        return positiveProp;
    }

    public void updateStateAndRef(float _state, long _time, float _ref){
        lastTime = stateTime;
        lastError = error;
        ref = _ref;
        state = _state;
        stateTime = _time;
        error = state - ref;
        errorAdded = false;
    }


    protected float getDifferential(){
        return  ((error - lastError) / (stateTime - lastTime))/1000f;
    }

    protected float getIntegral(){
        if (!errorAdded) {
            float newError = error * (stateTime - lastTime);

            Isum += newError/1000f;
            errorAdded = true;
        } else{
            System.out.println("Adding error twice?");
        }
        return  Isum;
    }


}

