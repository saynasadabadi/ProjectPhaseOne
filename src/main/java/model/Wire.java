package model;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.UUID;

public abstract class Wire {
    protected final String id;
    protected List<Port> ports;
    protected Color color;

    Wire(List<Port> ports, Color color) {
        this.id = UUID.randomUUID().toString();
        this.ports = ports;
        this.color = color;
    }

    public String getId() { return id; }
    public List<Port> getPorts() { return ports; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public abstract double getLength();
    public abstract Point2D.Double getSourceAbsolutePosition();
    public abstract Point2D.Double getDestinationAbsolutePosition();
    public abstract Point2D.Double getPointAtProgress(double progress);

    public Port getSourcePort() {
        if (ports == null || ports.isEmpty()) return null;
        for (Port p : ports) {
            if (p.getIoType() == IOType.OUTPUT) return p;
        }
        return ports.get(0);
    }

    public Port getDestinationPort() {
        if (ports == null || ports.size() < 2) return null;
        for (Port p : ports) {
            if (p.getIoType() == IOType.INPUT) return p;
        }
        return ports.get(ports.size() -1);
    }

    public abstract double calculateProgress(Point2D.Double currentPacketPosition);
}