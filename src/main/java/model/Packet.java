package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Packet {
    Point position;
    double noise;
    Vector velocity;
    PacketState state;
    Wire wire;
    NetworkSystem networkSystem;
    PacketAndPortShape shape;
    int radius;

    public Packet(Point position, PacketAndPortShape shape, int radius) {
        this.position = position;
        this.shape = shape;
        this.radius = radius;
        this.state = PacketState.IN_NETWORK_SYSTEM;
    }

    public List<Point> getVertices() {
        int numberOfSides = shape.getNumberOfSides();
        Point center = this.position;
        double currentRadius = this.radius;

        List<Point> vertices = new ArrayList<>(numberOfSides);
        double angleIncrement = 2 * Math.PI / numberOfSides;
        double initialAngle = 0;

        if (numberOfSides == 4) {
            initialAngle = Math.PI / 4;
        } else if (numberOfSides == 3) {
            initialAngle = -Math.PI / 2;
        }


        for (int i = 0; i < numberOfSides; i++) {
            double angle = initialAngle + i * angleIncrement;
            int x = (int) (center.x + currentRadius * Math.cos(angle));
            int y = (int) (center.y + currentRadius * Math.sin(angle));
            vertices.add(new Point(x, y));
        }
        return vertices;
    }

    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }
    public PacketAndPortShape getShape() { return shape; }
    public java.awt.Color getColor() { return shape.getColor(); }
    public int getRadius() { return radius; }
    public PacketState getState() { return state; }
    public void setState(PacketState state) { this.state = state; }
    public Wire getWire() { return wire; }
    public void setWire(Wire wire) { this.wire = wire; }
    public NetworkSystem getNetworkSystem() { return networkSystem; }
    public void setNetworkSystem(NetworkSystem networkSystem) { this.networkSystem = networkSystem; }
}