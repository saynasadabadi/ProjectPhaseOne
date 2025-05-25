package model;

import java.awt.Point;
import java.awt.geom.Point2D;
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

    @Override
    public double calculateProgress(Point2D.Double currentPacketPosition) {
        if (ports.size() != 2 || currentPacketPosition == null) {
            return 0.0; // Or throw exception, or return -1 to indicate error
        }

        Port sourcePort = getSourcePort();
        Port destPort = getDestinationPort();

        if (sourcePort == null || destPort == null) {
            return 0.0;
        }

        Point p1 = sourcePort.getAbsolutePosition();
        Point p2 = destPort.getAbsolutePosition();

        if (p1 == null || p2 == null) {
            return 0.0;
        }

        double lineDx = p2.x - p1.x;
        double lineDy = p2.y - p1.y;

        double totalLengthSquared = lineDx * lineDx + lineDy * lineDy;

        if (totalLengthSquared < 0.0001) { // Wire is essentially a point
            // If packet is at p1 (source), progress is 0, otherwise 1 (or based on distance to p1)
            return (Math.abs(currentPacketPosition.getX() - p1.x) < 0.001 && Math.abs(currentPacketPosition.getY() - p1.y) < 0.001) ? 0.0 : 1.0;
        }

        // Project packet position onto the line defined by the wire
        // t = [(packetPos - p1) . (p2 - p1)] / |p2 - p1|^2
        double t = ((currentPacketPosition.getX() - p1.x) * lineDx +
                      (currentPacketPosition.getY() - p1.y) * lineDy) / totalLengthSquared;

        // Clamp t to be between 0 and 1 for projection onto the segment
        t = Math.max(0, Math.min(1, t));

        // 't' now represents the progress along the wire (0.0 at p1, 1.0 at p2)
        return t;
    }
}