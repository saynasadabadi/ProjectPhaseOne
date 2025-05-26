package model;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.UUID;

public class Port {
    private final String id;
    private PacketAndPortShape shape;
    private IOType ioType;
    private NetworkSystem networkSystem;
    private Point relativePosition;
    private Wire connectedWire;
    private boolean selectedForConnection;
    private boolean inUse = false;

    public Port(IOType ioType, PacketAndPortShape shape) {
        this.id = UUID.randomUUID().toString();
        this.ioType = ioType;
        this.shape = shape;
        this.selectedForConnection = false;
    }

    public String getId() { return id; }
    public PacketAndPortShape getShape() { return shape; }
    public IOType getIoType() { return ioType; }
    public NetworkSystem getNetworkSystem() { return networkSystem; }
    public Wire getConnectedWire() { return connectedWire; }
    public Point getRelativePosition() { return relativePosition; }
    public boolean isSelectedForConnection() { return selectedForConnection; }


    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public Point getAbsolutePosition() {
        if (networkSystem != null && relativePosition != null) {
            return new Point(networkSystem.getPosition().x + relativePosition.x,
                    networkSystem.getPosition().y + relativePosition.y);
        }

        System.err.println("Port " + id + " getAbsolutePosition: networkSystem or relativePosition is null.");
        return new Point(0,0);
    }

    public Point2D.Double getAbsolutePositionAsPoint2D() {
        if (networkSystem != null && relativePosition != null && networkSystem.getPosition() != null) {
            return new Point2D.Double(networkSystem.getPosition().x + relativePosition.x,
                                      networkSystem.getPosition().y + relativePosition.y);
        }
        System.err.println("Port " + id + " getAbsolutePositionAsPoint2D: networkSystem, its position, or relativePosition is null.");
        return new Point2D.Double(0,0);
    }

    public Rectangle getBounds() {
        Point absPos = getAbsolutePosition();
        int s = NetworkSystem.PORT_VISUAL_SIZE;
        return new Rectangle(absPos.x - s / 2, absPos.y - s / 2, s, s);
    }

    public void setNetworkSystem(NetworkSystem networkSystem) { this.networkSystem = networkSystem; }
    public void setRelativePosition(Point relativePosition) { this.relativePosition = relativePosition; }
    public void setConnectedWire(Wire connectedWire) { this.connectedWire = connectedWire; }
    public void setSelectedForConnection(boolean selected) { this.selectedForConnection = selected; }

    public boolean isConnected() {
        return connectedWire != null;
    }
}