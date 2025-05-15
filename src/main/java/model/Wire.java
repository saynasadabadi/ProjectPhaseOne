package model;

import java.util.ArrayList;

public abstract class Wire {
    ArrayList<Port> ports;
    Color color;
    Wire(ArrayList<Port> ports, Color color) {
        this.ports = ports;
        this.color = color;
    }
    abstract double getLength();
}
