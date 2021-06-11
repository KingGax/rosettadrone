package sq.rogue.rosettadrone;

import android.os.Handler;

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

public class MultiDroneHelper implements ListenerCallbacks {
    private String serverAddress;
    private String username;
    private int listenerPort;
    private int notificationsPort;
    private MultiDroneCallbacks parent;
    private Handler registerTimeoutHandler = new Handler();
    private Runnable registerTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            parent.onConnectTimeout();
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

    public void setNotificationsPort(int notificationsPort) {
        this.notificationsPort = notificationsPort;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public void startRegister(String _serverAddress){
        if (_serverAddress != null){
            setServerAddress(_serverAddress);
        }
        notifyServer(NotificationStatus.CONNECTED,username,listenerPort,serverAddress, notificationsPort);
        parent.onStartConnect();
        registerTimeoutHandler.postDelayed(registerTimeoutRunnable, 1000L);
    }

    @Override
    public void handleDataReceived(String data) {

    }

    @Override
    public void handleIdReceived(String data) {

    }
}
