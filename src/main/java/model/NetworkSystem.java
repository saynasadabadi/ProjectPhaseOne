package model;

import java.awt.Point;
import java.awt.Rectangle;
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
    protected static final long PACKET_RELEASE_COOLDOWN_MILLIS = 1000; // 0.25 seconds

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
        // Assign system to ports and calculate relative positions
        // Input ports on the left side
        int numInputPorts = inputPorts.size();
        for (int i = 0; i < numInputPorts; i++) {
            Port port = inputPorts.get(i);
            port.setNetworkSystem(this);
            // Calculate X and Y relative to the NetworkSystem's top-left corner
            int portX = 0; // Left edge
            int portY = indicator_height + (body_height * (i + 1) / (numInputPorts + 1));
            port.setRelativePosition(new Point(portX, portY));
        }

        // Output ports on the right side
        int numOutputPorts = outputPorts.size();
        for (int i = 0; i < numOutputPorts; i++) {
            Port port = outputPorts.get(i);
            port.setNetworkSystem(this);
            int portX = width; // Right edge
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

    public List<Port> getInputPorts() { return new ArrayList<>(inputPorts); } // Return copy
    public List<Port> getOutputPorts() { return new ArrayList<>(outputPorts); } // Return copy

    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<>(inputPorts);
        allPorts.addAll(outputPorts);
        return allPorts;
    }

    public Point getAbsolutePortPosition(Port port) {
        if (port.getNetworkSystem() != this) {
            // This check can be useful for debugging if ports get mixed up.
            // System.err.println("Warning: getAbsolutePortPosition called for a port not belonging to this system.");
        }
        if (port.getRelativePosition() != null && position != null) {
            return new Point(position.x + port.getRelativePosition().x,
                    position.y + port.getRelativePosition().y);
        }
        // Fallback, though relativePosition should always be set by constructor
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
            // Use port's own getBounds method which is more robust
            if (port.getBounds().contains(clickPoint)) {
                return port;
            }
        }
        return null;
    }

    public void updateIndicatorState() {
        List<Port> allPorts = getAllPorts();
        if (allPorts.isEmpty()) {
            this.indicatorState = IndicatorState.OFF; // Or some other default for systems with no ports
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

    // --- New methods for game logic ---

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
            return sameShapePorts.get(0); // Pick the first available same-shape port
        }

        // If no same-shape port is available, and there are other available ports, pick one of them
        if (!availablePorts.isEmpty()){
            return availablePorts.get(0); // Pick the first available port of any shape
        }
        return null; // No suitable port found
    }

    protected boolean sendPacketToWire(Packet packet, NetworkModel networkModel) {
        Port selectedOutputPort = selectOutputPort(packet);
        System.out.println("    - Selected output port: " + (selectedOutputPort != null ? selectedOutputPort.getId() : "null"));

        if (selectedOutputPort != null && selectedOutputPort.getConnectedWire() != null) {
            Wire connectedWire = selectedOutputPort.getConnectedWire();
            Port destinationPort = connectedWire.getDestinationPort();
            System.out.println("    - Connected wire: " + connectedWire);
            System.out.println("    - Destination port: " + (destinationPort != null ? destinationPort.getId() : "null"));

            if (destinationPort == null || destinationPort.getIoType() != IOType.INPUT) {
                System.err.println("NetworkSystem " + id + ": Error sending packet. Destination port is null or not an INPUT port.");
                return false; // Invalid connection or wire setup
            }

            packet.setOriginPort(selectedOutputPort);
            packet.setTargetPort(destinationPort);
            packet.setCurrentWire(connectedWire);
            packet.setPosition(new Point(selectedOutputPort.getAbsolutePosition().x, selectedOutputPort.getAbsolutePosition().y));
            packet.setProgressOnWire(0.0);
            packet.setState(PacketState.ON_WIRE);
            packet.setNetworkSystem(null); // No longer inside this system

            selectedOutputPort.setInUse(true); // This port is now busy sending this packet

            if (!networkModel.getPackets().contains(packet)) {
                networkModel.addPacketToActiveList(packet); // Add to active simulation list
            }

            System.out.println(this.getClass().getSimpleName() + " " + this.getId() + " sending packet " + packet.getId() + " via port " + selectedOutputPort.getId() + " towards " + destinationPort.getId());
            return true;
        }
        System.out.println(this.getClass().getSimpleName() + " " + this.getId() + " failed to find output port or wire for packet " + packet.getId());
        return false;
    }

    public abstract void processIncomingPacket(Packet packet, Port inputPort, NetworkModel networkModel);
    public abstract void attemptPacketRelease(long currentTimeMillis, NetworkModel networkModel);
}