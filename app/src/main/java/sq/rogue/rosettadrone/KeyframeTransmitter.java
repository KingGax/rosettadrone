package sq.rogue.rosettadrone;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class KeyframeTransmitter implements Runnable{

    private int port;
    private String host;
    private byte[] currentData;
    private int currentDataSize;
    private boolean isDataSending = false;
    private boolean isDataToSend = false;
    Socket socket;

    public KeyframeTransmitter(String _host, int _port){
        port = _port;
        host = _host;
    }

    @Override
    public void run() {
        this.initialize();

    }

    public boolean isDataSending(){
        return isDataSending;
    }

    public boolean trySetDataToSend(byte[] data, int size){
        if (!isDataSending){
            currentData = data;
            currentDataSize = size;
            isDataSending = true;
            return true;
        } else{
            return false;
        }
    }



    private void initialize() {
         // sending socket
        try {
            socket = new Socket(host,port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        OutputStream out = null;
        InputStream socketIn;
        try {
            socketIn = socket.getInputStream();
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }


        byte[] sendBuffer = new byte[8 * 1024];

        System.out.println("Client listener initialized on port "+port);
        while(true){
            if (isDataSending){
                int count;
                InputStream in = new ByteArrayInputStream(currentData);
                try {
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    byte[] size = ByteBuffer.allocate(4).putInt(currentDataSize).array();
                    System.out.println("putting current data size " + currentDataSize);
                    out.write(size);
                    while ((count = in.read(sendBuffer)) > 0) {
                        out.write(sendBuffer, 0, count);
                    }
                    System.out.println("Finished sending!");
                    out.close();
                    while (!(in.read(sendBuffer) > 0)) {
                        //out.write(sendBuffer, 0, count);
                        System.out.println("Waiting for ack");
                        Thread.sleep(100L);
                    }
                    System.out.println("Ack recieved!");
                    //out.close();
                    //in.close();
                } catch (SocketException e) {
                    System.out.println("Socket Error sending picture data!");
                    if (socket.isClosed()){
                        socket = reOpenSocket();
                    }
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("Other error sending picture data!");
                }

                isDataSending = false;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /*byte[] buffer = new byte[1500]; // MTU = 1500 bytes
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
            }*/


        }
    }

    private void catchSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Socket reOpenSocket(){
        Socket sock;
        try {
            sock = new Socket(host,port);
        } catch (IOException e) {
            e.printStackTrace();
            sock = null;
        }
        return sock;
    }
}
