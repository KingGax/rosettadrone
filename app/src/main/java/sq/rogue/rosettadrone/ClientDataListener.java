package sq.rogue.rosettadrone;

import androidx.annotation.UiThread;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Parser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import dji.midware.data.model.P3.Pa;

public class ClientDataListener implements Runnable {

    private int port = 0;
    private String onlineStatusUpdateStartPattern = "---START STATUS UPDATE---";
    private String onlineStatusUpdateEndPattern = "---END STATUS UPDATE---";

    private String onlineUsersStartPattern = "---START ONLINE U UPDATE---";
    private String onlineUsersEndPattern = "---END ONLINE U UPDATE---";

    private final char ID_HEADER = 'i';
    private final char MSG_HEADER = 'm';
    private final char ACK_HEADER = 'a';
    private MavLinkMessageCallbacks parent;
    Parser mMavlinkParser;

    private String delimiter = ";";



    @Override
    public void run() {
        this.initialize();

    }

    public void setListenerCallback(MavLinkMessageCallbacks p){
        parent = p;
    }

    private void initialize() {
        DatagramSocket socket; // listener socket

        while(true) {
            try {
                socket = new DatagramSocket();
                port = socket.getLocalPort();
                parent.setMavLinkPort(port);
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
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();
                mMavlinkParser = new Parser();
                byte[] bytes = packet.getData();
                int[] ints = new int[bytes.length];
                for (int i = 0; i < bytes.length; i++)
                    ints[i] = bytes[i] & 0xff;

                /*for (int i = 0; i < bytes.length; i++) {
                    MAVLinkPacket mavPack = mMavlinkParser.mavlink_parse_char(ints[i]);



                    if (packet != null) {
                        MAVLinkMessage msg = mavPack.unpack();
                        //if (mainActivityWeakReference.get().prefs.getBoolean("pref_log_mavlink", false))
                        //    mainActivityWeakReference.get().logMessageFromGCS(msg.toString());
                        parent.receiveMavMessage(msg);
                    }
                }*/
                MAVLinkPacket mavPacket = decodePacket(bytes,bytes[1] & 0xff);
                MAVLinkMessage msg = mavPacket.unpack();
                parent.receiveMavMessage(msg);




            } catch (Exception e) {
                System.out.println("Fail");
                e.printStackTrace();
                parent.receiveMavMessage(null);
                continue;
            }


        }
    }

    private MAVLinkPacket decodePacket(byte[] buffer, int payloadSize) {
        MAVLinkPacket pack = new MAVLinkPacket(payloadSize);
        pack.seq = buffer[2] & 0xff;
        pack.sysid = buffer[3] & 0xff;
        pack.compid = buffer[4] & 0xff;
        pack.msgid = buffer[5] & 0xff;

        //buffer[0] = (byte) MAVLINK_STX;
        //buffer[1] = (byte) len;
        //buffer[2] = (byte) seq;
        //buffer[3] = (byte) sysid;
        //buffer[4] = (byte) compid;
        //buffer[5] = (byte) msgid;

        for (int j = 0; j < payloadSize; j++) {
            pack.payload.payload.put(buffer[6 + j]);
        }
        return pack;
        //generateCRC();
        //buffer[i++] = (byte) (crc.getLSB());
        //buffer[i++] = (byte) (crc.getMSB());
        //return buffer;
    }



    public int getPort() {
        return port;
    }



}