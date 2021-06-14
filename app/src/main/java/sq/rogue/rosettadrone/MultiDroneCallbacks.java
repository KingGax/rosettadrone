package sq.rogue.rosettadrone;

public interface MultiDroneCallbacks {
    void onStartConnect();
    void onConnectTimeout();
    void handleDataReceived(String data);
    void handleIdReceived(int id, int port);
}
