package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class SourceNetworkSystem extends NetworkSystem {
    private Queue<Packet> senderStorage;

    public SourceNetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                               ArrayList<Port> inputPorts, ArrayList<Port> outputPorts,
                               ArrayList<Packet> initialPackets) {
        super(indicatorState, position, width, indicator_height, body_height, inputPorts, outputPorts);
        this.senderStorage = new LinkedList<>();
        if (initialPackets != null) {
            for (Packet p : initialPackets) {
                addPacketToSenderStorage(p);
            }
        }
    }


    private void addPacketToSenderStorage(Packet packet){
        packet.setNetworkSystem(this);
        packet.setState(PacketState.IN_NETWORK_SYSTEM);
        packet.setPosition(new Point2D.Double(this.position.x + this.width / 2.0,
                                            this.position.y + this.getTotalHeight() / 2.0));
        this.senderStorage.offer(packet);
    }


    public Queue<Packet> getSenderStorage() {
        return senderStorage;
    }


    public void generateAndStorePacket(PacketAndPortShape shape, int radius) {
        Packet newPacket = new Packet(
                new Point2D.Double(this.position.x + this.width / 2.0, this.position.y + getTotalHeight() / 2.0),
                shape,
                radius
        );
        addPacketToSenderStorage(newPacket);

    }
    public void generateAndStorePacket(PacketAndPortShape shape) {
        generateAndStorePacket(shape, Packet.DEFAULT_RADIUS);
    }


    @Override
    public void processIncomingPacket(Packet packet, Port inputPort, NetworkModel networkModel) {

        packet.setState(PacketState.DELIVERED);
        packet.setNetworkSystem(this);
        packet.setPosition(new Point2D.Double(this.position.x + this.width / 2.0, 
                                            this.position.y + getTotalHeight() / 2.0));
        packet.setCurrentWire(null);
        packet.setTargetPort(null);
        packet.setOriginPort(null);
        networkModel.addDeliveredPacket(packet);
    }

    @Override
    public void attemptPacketRelease(long currentTimeMillis, NetworkModel networkModel) {

        System.out.println("SourceSystem " + getId() + " attempting packet release...");
        System.out.println("  - Storage size: " + senderStorage.size());
        System.out.println("  - Can release packet: " + canReleasePacket(currentTimeMillis));
        System.out.println("  - Cooldown remaining: " + Math.max(0, PACKET_RELEASE_COOLDOWN_MILLIS - (currentTimeMillis - lastPacketReleaseTimeMillis)) + "ms");
        
        if (canReleasePacket(currentTimeMillis) && !senderStorage.isEmpty()) {

            boolean anyPortAvailable = getOutputPorts().stream().anyMatch(p -> p.isConnected() && !p.isInUse());
            System.out.println("  - Any port available: " + anyPortAvailable);
            

            for (Port port : getOutputPorts()) {
                System.out.println("    Port " + port.getId() + ": connected=" + port.isConnected() + ", inUse=" + port.isInUse());
            }
            
            if (!anyPortAvailable) {
                System.out.println("SourceSystem " + getId() + ": All output ports are busy or not connected. Cannot release packet.");
                return;
            }

            Packet packetToSend = senderStorage.peek();
            if (packetToSend != null) {
                System.out.println("SourceSystem " + getId() + " attempting to release packet " + packetToSend.getId());
                if (sendPacketToWire(packetToSend, networkModel)) {
                    senderStorage.poll();
                    recordPacketRelease(currentTimeMillis);
                    System.out.println("SourceSystem " + getId() + " released packet " + packetToSend.getId() + ". Storage now: " + senderStorage.size());
                } else {
                    System.out.println("SourceSystem " + getId() + " failed to send packet " + packetToSend.getId() + " (no suitable port, or port selection failed).");
                }
            }
        } else {
            if (senderStorage.isEmpty()) {
                System.out.println("  - Storage is empty");
            }
            if (!canReleasePacket(currentTimeMillis)) {
                System.out.println("  - Still in cooldown period");
            }
        }
    }
}