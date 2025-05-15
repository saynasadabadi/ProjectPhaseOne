package model;

import java.util.ArrayList;

public class SourceNetworkSystem extends NetworkSystem {
    ArrayList<Packet> SenderStorage;
    ArrayList<Packet> ReceiverStorage;
    public SourceNetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,ArrayList<Port> inputPorts, ArrayList<Port> outputPorts,ArrayList<Packet> initialPackets) {
        super(indicatorState, position, width, indicator_height, body_height,inputPorts,outputPorts);
        this.SenderStorage=initialPackets;
    }
}
