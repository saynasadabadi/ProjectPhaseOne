package model;

import java.awt.Point;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class StraightWire extends Wire {

    private Port inputPort;
    private Port outputPort;
    private Color color;


    public StraightWire(Port port1, Port port2, Color color) {
        super(new ArrayList<>(List.of(port1, port2)), color);
        for (Port port : List.of(port1, port2)) {
            if (port.getIoType() == IOType.OUTPUT) {
                outputPort = port;
            }
            else {
                inputPort = port;
            }

        }
    }

    public List<Port> getPorts() {
        return Arrays.asList(inputPort, outputPort);
    }

    public Color getColor() {
        return color;
    }

    @Override
    public double getLength() {
        if (ports.size() == 2) {
            Point p1 = getSourcePort().getAbsolutePosition();
            Point p2 = getDestinationPort().getAbsolutePosition();
            return p1.distance(p2);
        }
        return 0;
    }
}