package sq.rogue.rosettadrone;

import androidx.appcompat.app.AppCompatActivity;

public interface ListenerCallbacks {
    void handleDataReceived(String data);
    void handleIdReceived(String data);
    void handleMavPortAck();
}
