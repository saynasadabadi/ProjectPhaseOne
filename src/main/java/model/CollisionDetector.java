package model;

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

        ArrayList<Point> vertices1 = p1.getVertices();
        ArrayList<Point> vertices2 = p2.getVertices();

        if (isSeparatingAxis(vertices1, vertices2)) {
            return false;
        }

        if (isSeparatingAxis(vertices2, vertices1)) {
            return false;
        }

        return true;
    }

    private static boolean isSeparatingAxis(ArrayList<Point> vertsA, ArrayList<Point> vertsB) {
        for (int i = 0; i < vertsA.size(); i++) {
            Point p1 = vertsA.get(i);
            Point p2 = vertsA.get((i + 1) % vertsA.size());

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

    private static Projection project(ArrayList<Point> vertices, Vector axis) {
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

}