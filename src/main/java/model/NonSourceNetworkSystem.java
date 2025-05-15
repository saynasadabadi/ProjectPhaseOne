package model;

import java.awt.Point;
import java.util.ArrayList;

public class NonSourceNetworkSystem extends NetworkSystem {
    ArrayList<Packet> storage;
    int storageLimit;

    public NonSourceNetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                                  ArrayList<Port> inputPorts, ArrayList<Port> outputPorts, int storageLimit) {
        super(indicatorState, position, width, indicator_height, body_height, inputPorts, outputPorts);
        this.storageLimit = storageLimit;
        this.storage = new ArrayList<>();
    }
    public int getStorageLimit() { return storageLimit; }
    public ArrayList<Packet> getStorage() { return storage; }
}