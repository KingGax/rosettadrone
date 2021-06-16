package sq.rogue.rosettadrone;

import com.MAVLink.Messages.MAVLinkMessage;

public interface MavLinkMessageCallbacks {
    void receiveMavMessage(MAVLinkMessage msg);
    void setMavLinkPort(int port);
}
