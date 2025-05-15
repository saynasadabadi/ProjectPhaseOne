package model;

public class Port {
    private PacketAndPortShape shape;
    private IOType ioType;
    private NetworkSystem networkSystem;
    private Point position;
    private Wire ConnectedWire;

    public Point getPosition() {
        return position;
    }
    public IOType getIoType() {
        return ioType;
    }
}
