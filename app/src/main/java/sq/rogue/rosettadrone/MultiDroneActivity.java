package sq.rogue.rosettadrone;

import android.os.Handler;
//import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.autonavi.amap.mapcore.Convert;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.mission.MissionControl;
import sq.rogue.rosettadrone.multidrone.UserDroneData;
import sq.rogue.rosettadrone.shared.Notification;
import sq.rogue.rosettadrone.shared.NotificationStatus;

public class MultiDroneActivity extends ListenerCallbacks {

    EditText editTextAddress, editTextPort;
    Button buttonConnect;
    TextView textViewState, textViewRx;

    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;

    private int notificationsPort = 32323;
    private String serverAddress = "localhost";

    private UserDroneData myData = new UserDroneData();
    private String username = "test";
    private ClientMessageListener clientListener = new ClientMessageListener();

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
                        sendData(myData);
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
    }

    View.OnClickListener buttonConnectOnClickListener =
            new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    /*udpClientThread = new UdpClientThread(
                            editTextAddress.getText().toString(),
                            Integer.parseInt(editTextPort.getText().toString()),
                            udpClientHandler);
                    udpClientThread.start();*/

                    serverAddress = editTextAddress.getText().toString();
                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            sq.rogue.rosettadrone.Message mes = new sq.rogue.rosettadrone.Message("hi there","me");
                            try  {
                                notifyServer(NotificationStatus.CONNECTED);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();

                    buttonConnect.setEnabled(false);
                    registerTimeoutHandler.postDelayed(registerTimeoutRunnable, 1000L);
                }
            };

    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
        System.out.println("add rxmsg "+ rxmsg);
    }

    private void clientEnd(){
        udpClientThread = null;
        textViewState.setText("clientEnd()");
        buttonConnect.setEnabled(true);
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

    public static class UdpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        private MultiDroneActivity parent;

        public UdpClientHandler(MultiDroneActivity parent) {
            super();
            this.parent = parent;
        }

        /*@Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    parent.updateState((String)msg.obj);
                    break;
                case UPDATE_MSG:
                    parent.updateRxMsg((String)msg.obj);
                    break;
                case UPDATE_END:
                    parent.clientEnd();
                    break;
                default:
                    super.handleMessage(msg);
            }

        }*/
    }

    @Override
    public void handleDataReceived(String data) {
        updateRxMsg(data);
    }
    @Override
    public void handleIdReceived(String id) {
        updateRxMsg("My id is " + id);
        registeredWithServer = true;
        myId = Integer.parseInt(id);
        myData.id = myId;
        registerTimeoutHandler.removeCallbacks(registerTimeoutRunnable);
        sendDataThread.start();
    }

    public void notifyServer(NotificationStatus type) throws Exception{
        DatagramSocket socket = new DatagramSocket();

        Notification n = new Notification(this.username, type,this.clientListener.getPort());

        String msg = n.serialize();

        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverAddress), this.notificationsPort); 																												// paketa
        socket.send(packet);
        socket.close();
    }

    public void sendData(UserDroneData data) throws Exception{
        System.out.println("attempting to send data");
        DatagramSocket socket = new DatagramSocket();
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);

        oo.writeObject(data);
        oo.close();

        byte[] buffer = bStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverAddress), this.notificationsPort); 																												// paketa
        socket.send(packet);
        socket.close();
        System.out.println("attempting to send data");
    }

    public void transmitData(Message msg,String recipientPassed) throws Exception{
        String delimiter = ";";
        String data = "M"+delimiter+this.username+delimiter+this.recipient+delimiter+msg.getData();
        DatagramSocket socket = new DatagramSocket();
        byte[] buffer = data.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverAddress), this.notificationsPort); 																												// paketa
        socket.send(packet);
        socket.close();

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
