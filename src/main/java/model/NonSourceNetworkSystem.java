package model;

import java.awt.Point;
import java.awt.geom.Point2D;
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
        packet.setNetworkSystem(this);
        packet.setPosition(new Point2D.Double(this.position.x + this.width / 2.0, 
                                            this.position.y + getTotalHeight() / 2.0));

        if (storage.size() >= storageLimit) {

            packet.setState(PacketState.LOST);
            networkModel.addLostPacket(packet);
        } else {

            packet.setState(PacketState.IN_NETWORK_SYSTEM);
            storage.offer(packet);
        }
    }

    @Override
    public void attemptPacketRelease(long currentTimeMillis, NetworkModel networkModel) {
        if (canReleasePacket(currentTimeMillis) && !storage.isEmpty()) {

            boolean anyPortAvailable = getOutputPorts().stream().anyMatch(p -> p.isConnected() && !p.isInUse());
            if (!anyPortAvailable) {

                return;
            }

            Packet packetToForward = storage.peek();
            if (packetToForward != null) {

                if (sendPacketToWire(packetToForward, networkModel)) {
                    storage.poll();
                    recordPacketRelease(currentTimeMillis);

                } else {

                }
            }
        }
    }
}