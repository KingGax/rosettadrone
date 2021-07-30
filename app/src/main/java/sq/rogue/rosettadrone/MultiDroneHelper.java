package sq.rogue.rosettadrone;

import android.os.Handler;

import com.MAVLink.Messages.MAVLinkMessage;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import multidrone.sharedclasses.UserDroneData;
import sq.rogue.rosettadrone.shared.Notification;
import sq.rogue.rosettadrone.shared.NotificationStatus;

public class MultiDroneHelper implements ListenerCallbacks,MavLinkMessageCallbacks {
    private String serverAddress;
    private String username = "test";
    private final String delimeter = ";";
    private int listenerPort;
    private int notificationsPort = 32323;
    private int mavPort= 0;
    private int dataPort = 0;
    private short sysID = 0;
    int myID = -1;
    private MultiDroneCallbacks parent;
    private boolean isMessageListenerInitialized = false;
    private boolean isDataListenerInitialized = false;
    private ClientMessageListener clientListener = new ClientMessageListener();
    private ClientDataListener clientDataListener = new ClientDataListener();
    private Handler registerTimeoutHandler = new Handler();
    private Runnable registerTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            parent.onConnectTimeout();
        }
    };

    private Handler portResendHandler = new Handler();
    private Runnable portResendRunnable = new Runnable() {
        @Override
        public void run() {
            sendMavDetails(myID,mavPort,serverAddress,notificationsPort,sysID);
            portResendHandler.postDelayed(portResendRunnable,5000L);
        }
    };




    public void notifyServer(NotificationStatus type,String username, int port, String serverAddress, int notificationsPort) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    DatagramSocket socket = new DatagramSocket();

                    Notification n = new Notification(username, type,port);

                    String msg = n.serialize();

                    byte[] buffer = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverAddress), notificationsPort); 																												// paketa
                    socket.send(packet);
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }

    public void updateMavDetails(short droneSysID){
        if (myID != -1){
            //mavPort = port;
            //mavPort = clientDataListener.getPort();
            sysID = droneSysID;
            portResendHandler.post(portResendRunnable);
        }
    }
    private void sendMavDetails(int id, int port, String serverAddress, int notificationsPort, short sysID) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    DatagramSocket socket = new DatagramSocket();



                    String msg = "P" + port + delimeter + id + delimeter + sysID;

                    byte[] buffer = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverAddress), notificationsPort); 																												// paketa
                    socket.send(packet);
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }

    public MultiDroneHelper(MultiDroneCallbacks p){
        parent = p;
    }

    public void setParent(MultiDroneCallbacks parent) {
        this.parent = parent;
    }

    public void setServerAddress(String _serverAddress){
        serverAddress = _serverAddress;
    }
    public void setListenerPort(int _listenerPort)
    {
        listenerPort = _listenerPort;
    }

    public void setDataPort(int dataPort) {
        this.dataPort = dataPort;
    }

    public void setNotificationsPort(int notificationsPort) {
        this.notificationsPort = notificationsPort;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void sendData(UserDroneData data) throws Exception{
        if (dataPort != 0){
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream);

            oo.writeObject(data);
            oo.close();

            byte[] buffer = bStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverAddress), this.dataPort); 																												// paketa
            socket.send(packet);
            socket.close();
        } else{
            System.out.println("SENDING DATA WITHOUT DATA PORT - bad program");
        }

    }

    public void startRegister(String _serverAddress){
        if (_serverAddress != null){
            setServerAddress(_serverAddress);
        }

        parent.onStartConnect();
        startMessageListener();
        startDataListener();
        setListenerPort(clientListener.getPort());
        notifyServer(NotificationStatus.CONNECTED,username,listenerPort,serverAddress, notificationsPort);
        registerTimeoutHandler.postDelayed(registerTimeoutRunnable, 1000L);
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

    public void startDataListener(){
        if (isDataListenerInitialized) {
            return;
        }
        clientDataListener.setListenerCallback(this);
        Thread clientListenerThread = new Thread(this.clientDataListener);
        clientListenerThread.start(); // start thread in the background
        this.isDataListenerInitialized = true;
    }

    @Override
    public void handleDataReceived(String data) {
        parent.handleDataReceived(data);
    }

    @Override
    public void handleIdReceived(String data) {
        try{
        String[] message = data.split(";");
        System.out.println("id data:" + data);
        System.out.println(message);
        int port = Integer.parseInt(message[1]);
        int id = Integer.parseInt(message[0]);
        int imgPort = Integer.parseInt(message[2]);
        setDataPort(port);
        myID = id;
        parent.handleIdReceived(id,port,serverAddress, imgPort);
        registerTimeoutHandler.removeCallbacks(registerTimeoutRunnable);
        } catch (Exception e){
            System.out.println("INVALID ID REPLY RECEIVED");
        }
    }

    @Override
    public void handleMavPortAck() {
        portResendHandler.removeCallbacks(portResendRunnable);
    }

    @Override
    public void receiveMavMessage(MAVLinkMessage msg) {
        if (msg != null){
            parent.receiveMavMessage(msg);
            //parent.showToast("recieved mavMessage");
        } else{
            parent.showToast("message not decoded");
        }

    }

    @Override
    public void setMavLinkPort(int port) {
        mavPort = port;
        parent.showToast(Integer.toString(port));
        System.out.println("set mav port " + port);
    }
}
