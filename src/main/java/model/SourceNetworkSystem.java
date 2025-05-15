package model;

import java.awt.Point;
import java.util.ArrayList;

public class SourceNetworkSystem extends NetworkSystem {
    ArrayList<Packet> senderStorage;
    ArrayList<Packet> receiverStorage;

    public SourceNetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                               ArrayList<Port> inputPorts, ArrayList<Port> outputPorts, ArrayList<Packet> initialPackets) {
        super(indicatorState, position, width, indicator_height, body_height, inputPorts, outputPorts);
        this.senderStorage = (initialPackets == null) ? new ArrayList<>() : initialPackets;
        this.receiverStorage = new ArrayList<>();

        if (this.senderStorage != null) {
            for (Packet p : this.senderStorage) {
                p.setNetworkSystem(this);
                if (!outputPorts.isEmpty()) {
                    Point outPortAbsPos = getAbsolutePortPosition(outputPorts.get(0));
                    p.setPosition(new Point(outPortAbsPos.x + 10, outPortAbsPos.y));
                } else {
                    p.setPosition(new Point(position.x + width / 2, position.y + getTotalHeight() / 2));
                }
            }
        }
    }
    public ArrayList<Packet> getSenderStorage() { return senderStorage; }
    public ArrayList<Packet> getReceiverStorage() { return receiverStorage; }
}