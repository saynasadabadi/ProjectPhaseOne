package model;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // For unique ID

public class Packet {
    private final String id; // Added for tracking
    Point position;
    double noise; // Not used in current requirements
    Vector velocity; // Not used in current requirements
    PacketState state;
    Wire currentWire; // Renamed from 'wire' for clarity, and to match previous logic
    NetworkSystem networkSystem; // System it's currently inside (if IN_NETWORK_SYSTEM)
    PacketAndPortShape shape;
    int radius;

    // Fields for movement and wire travel logic
    private Port targetPort;
    private Port originPort;
    private double progressOnWire; // 0.0 to 1.0

    public static final double SPEED = 2.0; // Pixels per game update (frame), adjust as needed
    public static final int DEFAULT_RADIUS = 8; // Default radius if not specified

    public Packet(Point position, PacketAndPortShape shape, int radius) {
        this.id = UUID.randomUUID().toString();
        this.position = position;
        this.shape = shape;
        this.radius = (radius > 0) ? radius : DEFAULT_RADIUS;
        this.state = PacketState.IN_NETWORK_SYSTEM; // Initial state
        this.progressOnWire = 0.0;
    }

    // Constructor with default radius
    public Packet(Point position, PacketAndPortShape shape) {
        this(position, shape, DEFAULT_RADIUS);
    }


    public List<Point> getVertices() {
        // Use the provided getVertices logic, ensure shape is not null
        if (shape == null || position == null) {
            return new ArrayList<>(); // Return empty list if no shape or position
        }
        int numberOfSides = shape.getNumberOfSides();
        Point center = this.position;
        double currentRadius = this.radius;

        List<Point> vertices = new ArrayList<>(numberOfSides);
        double angleIncrement = 2 * Math.PI / numberOfSides;
        double initialAngle = 0; // Default for circle-like or other polygons

        // Adjust initial angle for specific shapes to orient them as commonly expected
        if (numberOfSides == 4) { // Square
            initialAngle = Math.PI / 4; // Start at 45 degrees to make it flat
        } else if (numberOfSides == 3) { // Triangle
            initialAngle = -Math.PI / 2; // Start at -90 degrees to point upwards
        }


        for (int i = 0; i < numberOfSides; i++) {
            double angle = initialAngle + i * angleIncrement;
            int x = (int) (center.x + currentRadius * Math.cos(angle));
            int y = (int) (center.y + currentRadius * Math.sin(angle));
            vertices.add(new Point(x, y));
        }
        return vertices;
    }

    public String getId() { return id; }
    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }
    public PacketAndPortShape getShape() { return shape; }
    public java.awt.Color getColor() {
        if (shape != null) {
            return shape.getColor().brighter(); // Make packet color slightly different from port
        }
        return Color.WHITE; // Default color if shape is null
    }
    public int getRadius() { return radius; }
    public PacketState getState() { return state; }
    public void setState(PacketState state) { this.state = state; }

    public Wire getCurrentWire() { return currentWire; } // Renamed getter
    public void setCurrentWire(Wire wire) { this.currentWire = wire; } // Renamed setter

    public NetworkSystem getNetworkSystem() { return networkSystem; }
    public void setNetworkSystem(NetworkSystem networkSystem) { this.networkSystem = networkSystem; }

    // Getters and setters for new fields
    public Port getTargetPort() { return targetPort; }
    public void setTargetPort(Port targetPort) { this.targetPort = targetPort; }

    public Port getOriginPort() { return originPort; }
    public void setOriginPort(Port originPort) { this.originPort = originPort; }

    public double getProgressOnWire() { return progressOnWire; }
    public void setProgressOnWire(double progressOnWire) { this.progressOnWire = progressOnWire; }

    @Override
    public String toString() {
        return "Packet{" +
                "id='" + id + '\'' +
                ", shape=" + shape +
                ", state=" + state +
                ", position=" + position +
                '}';
    }
}