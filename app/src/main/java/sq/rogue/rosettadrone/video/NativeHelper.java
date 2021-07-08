/*
    FFMPEG decoder thread, used by VideoService...
 */

package sq.rogue.rosettadrone.video;

import android.util.Log;

import sq.rogue.rosettadrone.KeyframeTransmitter;

/**
 * A helper class to invoke native methods
 */
public class NativeHelper {

    public static final String TAG = NativeHelper.class.getSimpleName();
    private static NativeHelper instance;
    private int framesSinceSend = 0;
    private int framesBetweenSends = 30;

    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("djivideojni");
    }

    private KeyframeTransmitter kft;

    private NativeDataListener dataListener;
    //JNI

    private NativeHelper() {
        Log.e(TAG, "NativeHelper");
    }

    public static NativeHelper getInstance() {
        if (instance == null) {
            instance = new NativeHelper();
        }
        return instance;
    }

    public void setDataListener(NativeDataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void setKft(KeyframeTransmitter kft) {
        this.kft = kft;
    }

    /**
     * Test the ffmpeg.
     *
     * @return
     */
    public native String codecinfotest();

    /**
     * Initialize the ffmpeg.
     *
     * @return
     */
    public native boolean init();

    /**
     * Framing the raw data from camera
     *
     * @param buf
     * @param size
     * @return
     */
    public native boolean parse(byte[] buf, int size, int mode);

    /**
     * Release the ffmpeg
     *
     * @return
     */
    public native boolean release();

    /**
     * Invoke by JNI
     * Callback the frame data.
     *
     * @param buf
     * @param size
     * @param frameNum
     * @param isKeyFrame
     * @param width
     * @param height
     */
    public void onFrameDataRecv(byte[] buf, int size, int frameNum, boolean isKeyFrame, int width, int height) {
        if (dataListener != null) {
            dataListener.onDataRecv(buf, size, frameNum, isKeyFrame, width, height);
            //System.out.println("frame " + frameNum + " size: " + size + " " + (isKeyFrame?"KEYFRAME":""));
        }
        if (kft != null){//define a keyframe as between 100-250MB
            if (!kft.isDataSending() && framesBetweenSends < framesSinceSend && (size > 100_000 && size < 250_000)){
                if (kft.trySetDataToSend(buf,size)){
                    framesSinceSend=0;
                    System.out.println("Started send!  size:" + size);
                }
            }
        }
        framesSinceSend++;
    }

    public interface NativeDataListener {
        /**
         * Callback method for receiving the frame data from NativeHelper.
         * Note that this method will be invoke in framing thread, which means time consuming
         * processing should not in this thread, or the framing process will be blocked.
         *
         * @param data
         * @param size
         * @param frameNum
         * @param isKeyFrame
         * @param width
         * @param height
         */
        void onDataRecv(byte[] data, int size, int frameNum, boolean isKeyFrame, int width, int height);
    }
}
