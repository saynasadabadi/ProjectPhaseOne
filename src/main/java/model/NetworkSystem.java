package model;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class NetworkSystem {
    private final String id;
    protected IndicatorState indicatorState;
    protected Point position;
    protected int width;
    protected int indicator_height;
    protected int body_height;
    protected ArrayList<Port> inputPorts;
    protected ArrayList<Port> outputPorts;

    public static final int PORT_VISUAL_SIZE = 12;
    public static final int PORT_CLICK_PADDING = 2;

    protected long lastPacketReleaseTimeMillis = 0;
    protected static final long PACKET_RELEASE_COOLDOWN_MILLIS = 1000;

    public NetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                         ArrayList<Port> inputPorts, ArrayList<Port> outputPorts) {
        this.id = UUID.randomUUID().toString();
        this.indicatorState = indicatorState;
        this.position = position;
        this.width = width;
        this.indicator_height = indicator_height;
        this.body_height = body_height;
        this.inputPorts = (inputPorts == null) ? new ArrayList<>() : inputPorts;
        this.outputPorts = (outputPorts == null) ? new ArrayList<>() : outputPorts;
        assignPortsToSystemAndPosition();
        updateIndicatorState();
    }

    private void assignPortsToSystemAndPosition() {


        int numInputPorts = inputPorts.size();
        for (int i = 0; i < numInputPorts; i++) {
            Port port = inputPorts.get(i);
            port.setNetworkSystem(this);

            int portX = 0;
            int portY = indicator_height + (body_height * (i + 1) / (numInputPorts + 1));
            port.setRelativePosition(new Point(portX, portY));
        }


        int numOutputPorts = outputPorts.size();
        for (int i = 0; i < numOutputPorts; i++) {
            Port port = outputPorts.get(i);
            port.setNetworkSystem(this);
            int portX = width;
            int portY = indicator_height + (body_height * (i + 1) / (numOutputPorts + 1));
            port.setRelativePosition(new Point(portX, portY));
        }
    }
    public String getId() { return id; }
    public IndicatorState getIndicatorState() { return indicatorState; }
    public Point getPosition() { return position; }
    public int getWidth() { return width; }
    public int getIndicatorHeight() { return indicator_height; }
    public int getBodyHeight() { return body_height; }
    public int getTotalHeight() { return indicator_height + body_height; }

    public List<Port> getInputPorts() { return new ArrayList<>(inputPorts); }
    public List<Port> getOutputPorts() { return new ArrayList<>(outputPorts); }

    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<>(inputPorts);
        allPorts.addAll(outputPorts);
        return allPorts;
    }

    public Point getAbsolutePortPosition(Port port) {
        if (port.getNetworkSystem() != this) {


        }
        if (port.getRelativePosition() != null && position != null) {
            return new Point(position.x + port.getRelativePosition().x,
                    position.y + port.getRelativePosition().y);
        }

        System.err.println("Port " + port.getId() + " or system " + id + " has null position/relativePosition.");
        return new Point(position != null ? position.x : 0, position != null ? position.y : 0);
    }

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, width, getTotalHeight());
    }

    public Rectangle getIndicatorBounds() {
        return new Rectangle(position.x, position.y, width, indicator_height);
    }

    public Rectangle getBodyBounds() {
        return new Rectangle(position.x, position.y + indicator_height, width, body_height);
    }

    public void setPosition(Point position) { this.position = position; }
    public void setIndicatorState(IndicatorState state) { this.indicatorState = state; }

    public Port getPortAt(Point clickPoint) {
        for (Port port : getAllPorts()) {

            if (port.getBounds().contains(clickPoint)) {
                return port;
            }
        }
        return null;
    }

    public void updateIndicatorState() {
        List<Port> allPorts = getAllPorts();
        if (allPorts.isEmpty()) {
            this.indicatorState = IndicatorState.OFF;
            return;
        }
        boolean allConnected = true;
        for (Port port : allPorts) {
            if (!port.isConnected()) {
                allConnected = false;
                break;
            }
        }
        this.indicatorState = allConnected ? IndicatorState.ON : IndicatorState.OFF;
    }



    public boolean canReleasePacket(long currentTimeMillis) {
        return (currentTimeMillis - lastPacketReleaseTimeMillis) >= PACKET_RELEASE_COOLDOWN_MILLIS;
    }

    public void recordPacketRelease(long currentTimeMillis) {
        this.lastPacketReleaseTimeMillis = currentTimeMillis;
    }

    protected Port selectOutputPort(Packet packet) {
        List<Port> availablePorts = outputPorts.stream()
                .filter(p -> p.isConnected() && !p.isInUse() && p.getIoType() == IOType.OUTPUT)
                .collect(Collectors.toList());

        if (availablePorts.isEmpty()) {
            return null;
        }

        List<Port> sameShapePorts = availablePorts.stream()
                .filter(p -> p.getShape() == packet.getShape())
                .collect(Collectors.toList());

        if (!sameShapePorts.isEmpty()) {
            return sameShapePorts.get(0);
        }


        if (!availablePorts.isEmpty()){
            return availablePorts.get(0);
        }
        return null;
    }

    protected boolean sendPacketToWire(Packet packet, NetworkModel networkModel) {
        Port selectedOutputPort = selectOutputPort(packet);


        if (selectedOutputPort != null && selectedOutputPort.getConnectedWire() != null) {
            Wire connectedWire = selectedOutputPort.getConnectedWire();
            Port destinationPort = connectedWire.getDestinationPort();



            if (destinationPort == null || destinationPort.getIoType() != IOType.INPUT) {
                System.err.println("NetworkSystem " + id + ": Error sending packet. Destination port is null or not an INPUT port.");
                return false;
            }

            packet.setOriginPort(selectedOutputPort);
            packet.setTargetPort(destinationPort);
            packet.setCurrentWire(connectedWire);
            packet.setPosition(new Point2D.Double(selectedOutputPort.getAbsolutePosition().x, selectedOutputPort.getAbsolutePosition().y));
            packet.setProgressOnWire(0.0);
            packet.setState(PacketState.ON_WIRE);
            packet.setNetworkSystem(null);
            packet.initializeForWireMovement(selectedOutputPort);

            selectedOutputPort.setInUse(true);

            if (!networkModel.getPackets().contains(packet)) {
                networkModel.addPacketToActiveList(packet);
            }

            System.out.println(this.getClass().getSimpleName() + " " + this.getId() + " sending packet " + packet.getId() + " via port " + selectedOutputPort.getId() + " towards " + destinationPort.getId());
            return true;
        }
        System.out.println(this.getClass().getSimpleName() + " " + this.getId() + " failed to find output port or wire for packet " + packet.getId());
        return false;
    }

    public abstract void processIncomingPacket(Packet packet, Port inputPort, NetworkModel networkModel);
    public abstract void attemptPacketRelease(long currentTimeMillis, NetworkModel networkModel);

    public boolean hasAvailableOutputPort() {
        for (Port port : getAllPorts()) {
            if (port.getIoType() == IOType.OUTPUT && !port.isConnected()) {
                return true;
            }
        }
        return false;
    }
}