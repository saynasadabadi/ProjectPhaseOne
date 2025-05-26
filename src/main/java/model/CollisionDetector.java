package model;

import java.awt.Point;
import java.awt.geom.Point2D;
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

        List<Point2D.Double> vertices1 = p1.getVertices();
        List<Point2D.Double> vertices2 = p2.getVertices();

        if (isSeparatingAxis(vertices1, vertices2)) {
            return false;
        }

        if (isSeparatingAxis(vertices2, vertices1)) {
            return false;
        }

        return true;
    }

    private static boolean isSeparatingAxis(List<Point2D.Double> vertsA, List<Point2D.Double> vertsB) {
        for (int i = 0; i < vertsA.size(); i++) {
            Point2D.Double p1 = vertsA.get(i);
            Point2D.Double p2 = vertsA.get((i + 1) % vertsA.size());

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

    private static Projection project(List<Point2D.Double> vertices, Vector axis) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (Point2D.Double vertex : vertices) {
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

        public static boolean isPacketStillOnWire(Packet packet) {
        if (packet == null || packet.getCurrentWire() == null || packet.getState() != PacketState.ON_WIRE) {
            return false;
        }
        Wire wire = packet.getCurrentWire();
        Point2D.Double packetCenter = packet.getPosition();
        double packetRadius = packet.getRadius();

        Port sourcePort = wire.getSourcePort();
        Port destPort = wire.getDestinationPort();

        if (packetCenter == null || sourcePort == null || destPort == null) return false;

        Point wireStart = sourcePort.getAbsolutePosition();
        Point wireEnd = destPort.getAbsolutePosition();

        if (wireStart == null || wireEnd == null) return false;

        double dist = distanceToLineSegment(packetCenter.getX(), packetCenter.getY(), wireStart.x, wireStart.y, wireEnd.x, wireEnd.y);

        double tolerance = packetRadius;        return dist <= tolerance;    }

    private static double distanceToLineSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);        if (l2 == 0.0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;

        t = Math.max(0, Math.min(1, t));

        double closestX = x1 + t * (x2 - x1);
        double closestY = y1 + t * (y2 - y1);

        double dx = px - closestX;
        double dy = py - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

        public static boolean isPacketNearPort(Packet packet, Port targetPort, Wire wire) {
        if (packet == null || targetPort == null || wire == null || packet.getPosition() == null) {
            return false;
        }
        Point2D.Double packetPos = packet.getPosition();
        Point2D.Double portPos = targetPort.getAbsolutePositionAsPoint2D();
        double distanceToPortCenter = packetPos.distance(portPos);

        double arrivalTolerance = packet.getRadius() * 1.5; 

        if (distanceToPortCenter > arrivalTolerance) {
            return false;        }

        double toleranceForWireAlignment = packet.getRadius() * 2.0;        
        Point2D.Double wireStart = wire.getSourceAbsolutePosition();
        Point2D.Double wireEnd = wire.getDestinationAbsolutePosition();

        double distToWire = distanceToLineSegment(packetPos.x, packetPos.y, wireStart.x, wireStart.y, wireEnd.x, wireEnd.y);
        
        return distToWire <= toleranceForWireAlignment;
    }
}