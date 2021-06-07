package sq.rogue.rosettadrone;

import android.os.Handler;
//import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import sq.rogue.rosettadrone.shared.Notification;
import sq.rogue.rosettadrone.shared.NotificationStatus;

public class MultiDroneActivity extends AppCompatActivity {

    EditText editTextAddress, editTextPort;
    Button buttonConnect;
    TextView textViewState, textViewRx;

    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;

    private int notificationsPort = 32323;
    private String serverAddress = "localhost";

    private String username = "test";
    private ClientMessageListener clientListener = new ClientMessageListener();

    //private LoginView loginView;
    //private ChatView chatView;

    //private static ClientController instance = null;
    //private ClientMessageListener clientListener = new ClientMessageListener();
    //private boolean isMessageListenerInitialized = false;


    //private ArrayList<String> onlineUsers = new ArrayList<>();

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

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        udpClientHandler = new UdpClientHandler(this);
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
                                //sendMessage(mes,"£reee");
                                notifyServer(NotificationStatus.CONNECTED);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();

                    buttonConnect.setEnabled(false);
                }
            };

    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
    }

    private void clientEnd(){
        udpClientThread = null;
        textViewState.setText("clientEnd()");
        buttonConnect.setEnabled(true);

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

    public void notifyServer(NotificationStatus type) throws Exception{
        DatagramSocket socket = new DatagramSocket();

        Notification n = new Notification(this.username, type,this.clientListener.getPort());

        String msg = n.serialize();

        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverAddress), this.notificationsPort); 																												// paketa
        socket.send(packet);
        socket.close();
    }

    public void sendMessage(Message msg,String recipientPassed) throws Exception{
        String delimiter = ";";
        String data = "M"+delimiter+this.username+delimiter+this.recipient+delimiter+msg.getData();
        DatagramSocket socket = new DatagramSocket();
        byte[] buffer = data.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverAddress), this.notificationsPort); 																												// paketa
        socket.send(packet);
        socket.close();
    }
}
