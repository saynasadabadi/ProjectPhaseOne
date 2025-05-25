package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class CollisionDetector {

    public static List<List<Packet>> detectCollisions(List<Packet> packets) {
        List<List<Packet>> collidingPairs = new ArrayList<>();
        List<List<Packet>> potentialCollisions = broadPhaseDetection(packets);

        for (List<Packet> pair : potentialCollisions) {
            if (narrowPhaseDetection(pair.get(0), pair.get(1))) {
                collidingPairs.add(pair);
            }
        }
        return collidingPairs;
    }
    public static List<List<Packet>> broadPhaseDetection(List<Packet> packets) {
        List<List<Packet>> potentialPairs = new ArrayList<>();
        for (int i = 0; i < packets.size(); i++) {
            for (int j = i + 1; j < packets.size(); j++) {
                Packet p1 = packets.get(i);
                Packet p2 = packets.get(j);

                if (p1.position == null || p2.position == null) {
                    continue;
                }

                double distanceSquared = Math.pow(p1.position.getX() - p2.position.getX(), 2) +
                        Math.pow(p1.position.getY() - p2.position.getY(), 2);
                double sumRadiiSquared = Math.pow(p1.radius + p2.radius, 2);

                if (distanceSquared <= sumRadiiSquared) {
                    List<Packet> pair = new ArrayList<>();
                    pair.add(p1);
                    pair.add(p2);
                    potentialPairs.add(pair);
                }
            }
        }
        return potentialPairs;
    }

    public static boolean narrowPhaseDetection(Packet p1, Packet p2) {
        if (p1.shape == null || p2.shape == null || p1.position == null || p2.position == null) {
            return false;
        }

        List<java.awt.Point> vertices1 = p1.getVertices();
        List<java.awt.Point> vertices2 = p2.getVertices();

        if (isSeparatingAxis(vertices1, vertices2)) {
            return false;
        }

        if (isSeparatingAxis(vertices2, vertices1)) {
            return false;
        }

        return true;
    }

    private static boolean isSeparatingAxis(List<java.awt.Point> vertsA, List<java.awt.Point> vertsB) {
        for (int i = 0; i < vertsA.size(); i++) {
            java.awt.Point p1 = vertsA.get(i);
            java.awt.Point p2 = vertsA.get((i + 1) % vertsA.size());

            Vector edge = new Vector(p2.getX() - p1.getX(), p2.getY() - p1.getY());
            Vector axis = new Vector(-edge.getY(), edge.getX());

            Projection projA = project(vertsA, axis);
            Projection projB = project(vertsB, axis);

            if (!projA.overlaps(projB)) {
                return true;
            }
        }
        return false;
    }

    private static Projection project(List<java.awt.Point> vertices, Vector axis) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (Point vertex : vertices) {
            double dotProduct = vertex.getX() * axis.getX() + vertex.getY() * axis.getY();
            min = Math.min(min, dotProduct);
            max = Math.max(max, dotProduct);
        }
        return new Projection(min, max);
    }

    private static class Projection {
        double min;
        double max;

        public Projection(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public boolean overlaps(Projection other) {
            return this.max >= other.min && other.max >= this.min;
        }
    }

    /**
     * Checks if a packet is still considered to be on its wire.
     * A packet is on the wire if its center is within its radius plus a small tolerance
     * from the wire's line segment.
     * @param packet The packet to check.
     * @return True if the packet is on its wire, false otherwise.
     */
    public static boolean isPacketStillOnWire(Packet packet) {
        if (packet == null || packet.getCurrentWire() == null || packet.getState() != PacketState.ON_WIRE) {
            // If not on a wire, or no wire assigned, it can't be "on the wire".
            // Or if it's not in a state where it should be on a wire.
            return false;
        }
        Wire wire = packet.getCurrentWire();
        Point packetCenter = packet.getPosition();
        double packetRadius = packet.getRadius();

        Port sourcePort = wire.getSourcePort();
        Port destPort = wire.getDestinationPort();

        if (packetCenter == null || sourcePort == null || destPort == null) return false;

        Point wireStart = sourcePort.getAbsolutePosition();
        Point wireEnd = destPort.getAbsolutePosition();

        if (wireStart == null || wireEnd == null) return false;

        // Calculate distance from packet center to the line segment of the wire
        double dist = distanceToLineSegment(packetCenter.x, packetCenter.y, wireStart.x, wireStart.y, wireEnd.x, wireEnd.y);

        // Packet's edge can be at most a small tolerance away from the wire line.
        // Let's define tolerance as a fraction of its radius, e.g., 0.5 * radius.
        // This means the packet's main body must still significantly overlap the wire's path.
        double tolerance = packetRadius * 0.5;
        return dist <= tolerance; // If distance from center to line is less than half radius, it's "on"
    }

    // Helper method: Distance from point (px, py) to line segment (x1, y1) - (x2, y2)
    private static double distanceToLineSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1); // Squared length of the segment
        if (l2 == 0.0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1)); // Segment is a point

        // Consider the line extending the segment, parameterized as P = P1 + t (P2 - P1).
        // We find projection of point P onto the line.
        // t = [(P-P1) . (P2-P1)] / |P2-P1|^2
        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;

        // If the projection is outside the segment, clamp t to the nearest endpoint.
        t = Math.max(0, Math.min(1, t));

        // Coordinates of the closest point on the segment to P
        double closestX = x1 + t * (x2 - x1);
        double closestY = y1 + t * (y2 - y1);

        // Distance from P to this closest point
        double dx = px - closestX;
        double dy = py - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }
}