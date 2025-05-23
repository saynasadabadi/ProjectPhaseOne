package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class SourceNetworkSystem extends NetworkSystem {
    private Queue<Packet> senderStorage;

    public SourceNetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                               ArrayList<Port> inputPorts, ArrayList<Port> outputPorts,
                               ArrayList<Packet> initialPackets) { // Assuming initialPackets are provided
        super(indicatorState, position, width, indicator_height, body_height, inputPorts, outputPorts);
        this.senderStorage = new LinkedList<>();
        if (initialPackets != null) {
            for (Packet p : initialPackets) {
                addPacketToSenderStorage(p); // Use helper method
            }
        }
    }

    // Helper to correctly initialize and add packets to internal storage
    private void addPacketToSenderStorage(Packet packet){
        packet.setNetworkSystem(this);
        packet.setState(PacketState.IN_NETWORK_SYSTEM);
        packet.setPosition(new Point(this.position.x + this.width / 2, this.position.y + this.getTotalHeight() / 2));
        this.senderStorage.offer(packet);
    }


    public Queue<Packet> getSenderStorage() {
        return senderStorage;
    }

    // Method to programmatically add more packets to be sent later
    public void generateAndStorePacket(PacketAndPortShape shape, int radius) {
        Packet newPacket = new Packet(
                new Point(this.position.x + this.width / 2, this.position.y + getTotalHeight() / 2),
                shape,
                radius
        );
        addPacketToSenderStorage(newPacket);
        //System.out.println("Packet " + newPacket.getId() + " generated and added to SourceSystem " + getId() + " storage. Storage size: " + senderStorage.size());
    }
    public void generateAndStorePacket(PacketAndPortShape shape) {
        generateAndStorePacket(shape, Packet.DEFAULT_RADIUS);
    }


    @Override
    public void processIncomingPacket(Packet packet, Port inputPort, NetworkModel networkModel) {
        //System.out.println("Packet " + packet.getId() + " DELIVERED to SourceSystem " + getId() + " via port " + inputPort.getId());
        packet.setState(PacketState.DELIVERED);
        packet.setNetworkSystem(this); // Mark as 'in' this system temporarily for state
        packet.setPosition(new Point(this.position.x + this.width / 2, this.position.y + getTotalHeight() / 2));
        packet.setCurrentWire(null);
        packet.setTargetPort(null);
        packet.setOriginPort(null);
        networkModel.addDeliveredPacket(packet);
    }

    @Override
    public void attemptPacketRelease(long currentTimeMillis, NetworkModel networkModel) {
        if (canReleasePacket(currentTimeMillis) && !senderStorage.isEmpty()) {
            // Check if any output port is available (not inUse) before peeking/sending
            boolean anyPortAvailable = getOutputPorts().stream().anyMatch(p -> p.isConnected() && !p.isInUse());
            if (!anyPortAvailable) {
                //System.out.println("SourceSystem " + getId() + ": All output ports are busy or not connected. Cannot release packet.");
                return;
            }

            Packet packetToSend = senderStorage.peek();
            if (packetToSend != null) {
                //System.out.println("SourceSystem " + getId() + " attempting to release packet " + packetToSend.getId());
                if (sendPacketToWire(packetToSend, networkModel)) {
                    senderStorage.poll(); // Remove from storage only if successfully sent
                    recordPacketRelease(currentTimeMillis);
                    // System.out.println("SourceSystem " + getId() + " released packet " + packetToSend.getId() + ". Storage now: " + senderStorage.size());
                } else {
                    // System.out.println("SourceSystem " + getId() + " failed to send packet " + packetToSend.getId() + " (no suitable port, or port selection failed).");
                }
            }
        }
    }
}