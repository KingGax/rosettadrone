package sq.rogue.rosettadrone;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ListenerCallbacks extends AppCompatActivity {
    abstract public void handleDataReceived(String data);
    abstract public void handleIdReceived(String data);
}
