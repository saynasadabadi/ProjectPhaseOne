// FILE: model/StraightWire.java
package model;

import java.awt.Point;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


public class StraightWire extends Wire {

    public StraightWire(Port port1, Port port2, Color color) {
        super(new ArrayList<>(List.of(port1, port2)), color);
        if (!((port1.getIoType() == IOType.OUTPUT && port2.getIoType() == IOType.INPUT) ||
                (port1.getIoType() == IOType.INPUT && port2.getIoType() == IOType.OUTPUT))) {
        }
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