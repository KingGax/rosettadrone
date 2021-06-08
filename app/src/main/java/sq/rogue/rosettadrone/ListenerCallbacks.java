package sq.rogue.rosettadrone;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ListenerCallbacks extends AppCompatActivity {
    abstract public void handleReceived(String data);
}
