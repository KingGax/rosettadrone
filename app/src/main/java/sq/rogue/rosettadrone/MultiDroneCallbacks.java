package sq.rogue.rosettadrone;

import com.MAVLink.Messages.MAVLinkMessage;

public interface MultiDroneCallbacks {
    void onStartConnect();
    void onConnectTimeout();
    void handleDataReceived(String data);
    void handleIdReceived(int id, int port, String serverAddress, int imgPort);
    void receiveMavMessage(MAVLinkMessage msg);
    void showToast(final String msg);
}
