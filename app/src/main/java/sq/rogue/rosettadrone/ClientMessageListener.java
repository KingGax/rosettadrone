package sq.rogue.rosettadrone;

import androidx.annotation.UiThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

public class ClientMessageListener implements Runnable {

    private int port = 2019;
    private String onlineStatusUpdateStartPattern = "---START STATUS UPDATE---";
    private String onlineStatusUpdateEndPattern = "---END STATUS UPDATE---";

    private String onlineUsersStartPattern = "---START ONLINE U UPDATE---";
    private String onlineUsersEndPattern = "---END ONLINE U UPDATE---";

    private final char ID_HEADER = 'i';
    private final char MSG_HEADER = 'm';
    private final char ACK_HEADER = 'a';

    private String delimiter = ";";

    private ListenerCallbacks callback;


    @Override
    public void run() {
        this.initialize();

    }

    public void setListenerCallback(ListenerCallbacks c){
        callback = c;
    }

    private void initialize() {
        DatagramSocket socket; // listener socket

        while(true) {
            try {
                socket = new DatagramSocket(this.port);
                break;
            } catch (SocketException e) {
                this.port++;
            }
        }

        System.out.println("Client listener initialized on port "+port);
        while(true){

            byte[] buffer = new byte[1500]; // MTU = 1500 bytes
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // primanje paketa
            try {
                socket.receive(packet);
                System.out.println("I got somethinig");
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();

                String receivedText = new String(buffer).trim();

                System.out.println("NEW DATA: "+receivedText);
                passMessage(receivedText);



            } catch (Exception e) {
                System.out.println("Fail");
                e.printStackTrace();
                continue;
            }


        }
    }

    public void passMessage(String msg) {
        switch (msg.charAt(0)){
            case ID_HEADER:
                callback.handleIdReceived(msg.substring(1));
                break;
            case MSG_HEADER:
                callback.handleDataReceived(msg.substring(1));
                break;
            case ACK_HEADER:
                callback.handleMavPortAck();
                if (msg == (ACK_HEADER+"MAVPORT")){
                    callback.handleMavPortAck();
                }
                break;

            default:
                System.out.println("BAD MESSAGE HEADER");
                System.out.println("BAD MESSAGE HEADER");
                System.out.println("BAD MESSAGE HEADER");
                System.out.println("BAD MESSAGE HEADER");
                System.out.println("BAD MESSAGE HEADER");
                System.out.println("BAD MESSAGE HEADER");
                break;
        }
        /*callback.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });*/
    }


    public int getPort() {
        return port;
    }



}