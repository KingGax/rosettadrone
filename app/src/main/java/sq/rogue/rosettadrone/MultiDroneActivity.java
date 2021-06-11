package sq.rogue.rosettadrone;

import android.os.Handler;
//import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import multidrone.sharedclasses.UserDroneData;
import sq.rogue.rosettadrone.shared.Notification;
import sq.rogue.rosettadrone.shared.NotificationStatus;

public class MultiDroneActivity extends AppCompatActivity implements ListenerCallbacks,MultiDroneCallbacks {

    EditText editTextAddress, editTextPort;
    Button buttonConnect;
    TextView textViewState, textViewRx;


    private int notificationsPort = 32323;
    private String serverAddress = "localhost";

    private UserDroneData myData = new UserDroneData();
    private String username = "test";
    private ClientMessageListener clientListener = new ClientMessageListener();

    private MultiDroneHelper helper = new MultiDroneHelper(this);

    //private LoginView loginView;
    //private ChatView chatView;

    //private static ClientController instance = null;
    //private ClientMessageListener clientListener = new ClientMessageListener();
    private boolean isMessageListenerInitialized = false;
    private boolean registeredWithServer = false;
    private int myId = -1;



    private Handler registerTimeoutHandler = new Handler();
    private Runnable registerTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!registeredWithServer){
                buttonConnect.setEnabled(true);
                showToast("Did not receive user ID from server");
            }
        }
    };

    private Thread sendDataThread = new Thread() {
        @Override
        public void run() {
            try {
                while(true) {
                    sleep(1000);
                    try  {
                        myData.height+=1;
                        myData.batteryPercent-=1;
                        helper.sendData(myData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("BAD THINGS");
                e.printStackTrace();
            }
        }
    };

    private String recipient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multidrone);
        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        buttonConnect = (Button) findViewById(R.id.connect);
        textViewState = (TextView)findViewById(R.id.state);
        textViewRx = (TextView)findViewById(R.id.received);

        myData.batteryPercent = 100;
        myData.height = 0;
        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        startMessageListener();
        helper.setListenerPort(clientListener.getPort());
        helper.setUsername(username);
        helper.setNotificationsPort(notificationsPort);
    }

    View.OnClickListener buttonConnectOnClickListener =
            new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    helper.startRegister(editTextAddress.getText().toString());



                    //registerTimeoutHandler.postDelayed(registerTimeoutRunnable, 1000L);
                }
            };

    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
        System.out.println("add rxmsg "+ rxmsg);
    }


    public void startMessageListener(){
        if (isMessageListenerInitialized) {
            return;
        }
        clientListener.setListenerCallback(this);
        Thread clientListenerThread = new Thread(this.clientListener);
        clientListenerThread.start(); // start thread in the background
        this.isMessageListenerInitialized = true;
    }

    @Override
    public void onStartConnect() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonConnect.setEnabled(false);
            }
        });

    }

    @Override
    public void onConnectTimeout() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!registeredWithServer){
                    buttonConnect.setEnabled(true);
                    showToast("Did not receive user ID from server");
                }
            }
        });
    }

    @Override
    public void handleDataReceived(String data) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateRxMsg(data);
            }
        });
    }
    @Override
    public void handleIdReceived(String id) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateRxMsg("My id is " + id);
                registeredWithServer = true;
                myId = Integer.parseInt(id);
                myData.id = myId;
                registerTimeoutHandler.removeCallbacks(registerTimeoutRunnable);
                sendDataThread.start();
            }
        });

    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MultiDroneActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
