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
    Socket socket;

    public KeyframeTransmitter(String _host, int _port){
        port = _port;
        host = _host;
    }

    @Override
    public void run() {
        this.initialize();

    }

    public boolean trySetDataToSend(byte[] data, int size){
        if (!isDataSending){
            currentData = data;
            currentDataSize = size;
            setDataSending(true);
            return true;
        } else{
            return false;
        }
    }

    public synchronized void setDataSending(boolean sending){
        isDataSending = sending;
    }

    public synchronized boolean getIsDataSending(){
        return isDataSending;
    }


    boolean alive = true;

    private void initialize() {
         // sending socket
        try {
            System.out.println("waiting for host");
            socket = new Socket(host,port);
            System.out.println("created keyframe socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
        OutputStream out = null;
        InputStream socketIn = null;
        try {
            socketIn = socket.getInputStream();
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }


        byte[] sendBuffer = new byte[8 * 1024];

        System.out.println("Client listener initialized on port "+port);
        while(alive){
            if (isDataSending){
                int count;
                InputStream in = new ByteArrayInputStream(currentData);
                try {
                    out = socket.getOutputStream();
                    socketIn = socket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                }
                try {
                    byte[] size = ByteBuffer.allocate(4).putInt(currentDataSize).array();
                    //System.out.println("putting current data size " + currentDataSize);
                    out.write(size);
                    while ((count = in.read(sendBuffer)) > 0) {
                        out.write(sendBuffer, 0, count);
                    }
                    boolean ack = false;
                    while (!ack && (socketIn.read() > 0)) {
                        //out.write(sendBuffer, 0, count);
                        ack = true;
                        Thread.sleep(100L);
                    }
                } catch (SocketException e) {
                    System.out.println("Socket Error sending picture data!");
                    System.out.println(e.getMessage());
                    System.out.println("SHUTTING DOWN KFT");
                    alive = false;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Other error sending picture data!");
                }
                setDataSending(false);
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            out.close();
            socket.close();
            System.out.println("socket closed");
        } catch (Exception e) {
            System.out.println("socket maybe not closed");
            System.out.println(e.getMessage());
        }

        System.out.println("Keyframe transmitted finished");
    }

    private void catchSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        alive = false;
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
