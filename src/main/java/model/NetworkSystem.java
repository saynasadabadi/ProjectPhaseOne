package model;

import java.util.ArrayList;

public abstract class NetworkSystem {
    IndicatorState indicatorState;
    Point position;
    int width;
    int indicator_height;
    int body_height;
    ArrayList<Port> inputPorts;
    ArrayList<Port> outputPorts;
    public NetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,ArrayList<Port> inputPorts, ArrayList<Port> outputPorts) {
        this.indicatorState = indicatorState;
        this.position = position;
        this.width = width;
        this.indicator_height = indicator_height;
        this.body_height = body_height;
        this.inputPorts = inputPorts;
        this.outputPorts = outputPorts;
    }
}
