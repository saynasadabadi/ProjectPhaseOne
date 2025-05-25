package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.geom.Line2D;

public class StraightWire extends Wire {

    private Port inputPort;
    private Port outputPort;
    private Color color;

    public StraightWire(Port port1, Port port2, Color color) {
        super(List.of(determineSourcePort(port1, port2), determineDestinationPort(port1, port2)), color);
        for (Port port : List.of(port1, port2)) {
            if (port.getIoType() == IOType.OUTPUT) {
                outputPort = port;
            }
            else {
                inputPort = port;
            }
        }
    }

    private static Port determineSourcePort(Port p1, Port p2) {
        if (p1.getIoType() == IOType.OUTPUT) return p1;
        if (p2.getIoType() == IOType.OUTPUT) return p2;
        return p1; // Default or error, though validation should prevent this
    }

    private static Port determineDestinationPort(Port p1, Port p2) {
        if (p1.getIoType() == IOType.INPUT) return p1;
        if (p2.getIoType() == IOType.INPUT) return p2;
        return p2; // Default or error
    }

    public List<Port> getPorts() {
        return Arrays.asList(inputPort, outputPort);
    }

    public Color getColor() {
        return color;
    }

    @Override
    public double getLength() {
        Point2D.Double p1 = getSourceAbsolutePosition();
        Point2D.Double p2 = getDestinationAbsolutePosition();
        return p1.distance(p2);
    }

    @Override
    public double calculateProgress(Point2D.Double currentPacketPosition) {
        Point2D.Double wireStart = getSourceAbsolutePosition();
        Point2D.Double wireEnd = getDestinationAbsolutePosition();
        double wireLength = getLength();

        if (wireLength < 0.0001) {
            return 0.0; // Avoid division by zero for zero-length wire
        }

        // Vector from wire start to packet
        double Vwp_x = currentPacketPosition.x - wireStart.x;
        double Vwp_y = currentPacketPosition.y - wireStart.y;

        // Vector representing the wire itself
        double Vwe_x = wireEnd.x - wireStart.x;
        double Vwe_y = wireEnd.y - wireStart.y;

        // Project Vwp onto Vwe using dot product
        double dotProduct = Vwp_x * Vwe_x + Vwp_y * Vwe_y;
        double projectedLength = dotProduct / wireLength;

        // Normalize to progress (0 to 1)
        double progress = projectedLength / wireLength;
        return Math.max(0.0, Math.min(1.0, progress)); // Clamp progress
    }

    @Override
    public Point2D.Double getSourceAbsolutePosition() {
        Port sourcePort = getSourcePort();
        if (sourcePort == null || sourcePort.getAbsolutePosition() == null) return new Point2D.Double(0,0); // Should not happen with valid wire
        return new Point2D.Double(sourcePort.getAbsolutePosition().x, sourcePort.getAbsolutePosition().y);
    }

    @Override
    public Point2D.Double getDestinationAbsolutePosition() {
        Port destinationPort = getDestinationPort();
        if (destinationPort == null || destinationPort.getAbsolutePosition() == null) return new Point2D.Double(0,0); // Should not happen
        return new Point2D.Double(destinationPort.getAbsolutePosition().x, destinationPort.getAbsolutePosition().y);
    }

    @Override
    public Point2D.Double getPointAtProgress(double progress) {
        Point2D.Double p1 = getSourceAbsolutePosition();
        Point2D.Double p2 = getDestinationAbsolutePosition();

        progress = Math.max(0.0, Math.min(1.0, progress)); // Clamp progress

        double x = p1.x + (p2.x - p1.x) * progress;
        double y = p1.y + (p2.y - p1.y) * progress;
        return new Point2D.Double(x, y);
    }

    // Method to check if a point is close to the wire segment
    public boolean isPointNearWire(Point2D.Double point, double maxDistance) {
        Point2D.Double p1 = getSourceAbsolutePosition();
        Point2D.Double p2 = getDestinationAbsolutePosition();
        return Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, point.x, point.y) < maxDistance;
    }
}