package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class NonSourceNetworkSystem extends NetworkSystem {
    private Queue<Packet> storage;
    private int storageLimit;

    public NonSourceNetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                                  ArrayList<Port> inputPorts, ArrayList<Port> outputPorts, int storageLimit) {
        super(indicatorState, position, width, indicator_height, body_height, inputPorts, outputPorts);
        this.storageLimit = storageLimit;
        this.storage = new LinkedList<>();
    }

    public int getStorageLimit() { return storageLimit; }
    public Queue<Packet> getStorage() { return storage; }
    public int getCurrentStorageSize() { return storage.size(); }


    @Override
    public void processIncomingPacket(Packet packet, Port inputPort, NetworkModel networkModel) {
        packet.setCurrentWire(null);
        packet.setTargetPort(null);
        packet.setOriginPort(null);
        packet.setNetworkSystem(this); // Packet is now inside this system
        packet.setPosition(new Point(this.position.x + this.width / 2, this.position.y + getTotalHeight() / 2));

        if (storage.size() >= storageLimit) {
            //System.out.println("NonSourceSystem " + getId() + " storage full. Packet " + packet.getId() + " LOST.");
            packet.setState(PacketState.LOST);
            networkModel.addLostPacket(packet);
        } else {
            //System.out.println("Packet " + packet.getId() + " stored in NonSourceSystem " + getId() + ". Storage: " + (storage.size() + 1) + "/" + storageLimit);
            packet.setState(PacketState.IN_NETWORK_SYSTEM);
            storage.offer(packet);
        }
    }

    @Override
    public void attemptPacketRelease(long currentTimeMillis, NetworkModel networkModel) {
        if (canReleasePacket(currentTimeMillis) && !storage.isEmpty()) {
            // Check if any output port is available (not inUse) before peeking/sending
            boolean anyPortAvailable = getOutputPorts().stream().anyMatch(p -> p.isConnected() && !p.isInUse());
            if (!anyPortAvailable) {
                //System.out.println("NonSourceSystem " + getId() + ": All output ports are busy or not connected. Cannot forward packet.");
                return;
            }

            Packet packetToForward = storage.peek();
            if (packetToForward != null) {
                //System.out.println("NonSourceSystem " + getId() + " attempting to forward packet " + packetToForward.getId());
                if (sendPacketToWire(packetToForward, networkModel)) {
                    storage.poll(); // Remove from storage only if successfully sent
                    recordPacketRelease(currentTimeMillis);
                    //System.out.println("NonSourceSystem " + getId() + " forwarded packet " + packetToForward.getId() + ". Storage now: " + storage.size());
                } else {
                    // System.out.println("NonSourceSystem " + getId() + " failed to forward packet " + packetToForward.getId() + " (no suitable port).");
                }
            }
        }
    }
}