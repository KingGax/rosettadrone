package multidrone.controller;

public class PIDControllerZ extends PIDOneVarController{
    public float integralWipeDist;
    public PIDControllerZ(){

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
}