package model;

import java.util.ArrayList;

public class Packet {
    Point position;
    double noise;
    Vector velocity;
    PacketState state;
    Wire wire;
    NetworkSystem networkSystem;
    PacketAndPortShape shape;
    int radius;


    public ArrayList<Point> getVertices() {
        int numberOfSides = shape.getNumberOfSides();
        Point center = this.position;
        double radius = this.radius;

        ArrayList<Point> vertices = new ArrayList<>(numberOfSides);
        double angleIncrement = 2 * Math.PI / numberOfSides;

        double initialAngle = 0;
        for (int i = 0; i < numberOfSides; i++) {
            double angle = initialAngle + i * angleIncrement;
            int x = (int) (center.getX() + radius * Math.cos(angle));
            int y = (int) (center.getY() + radius * Math.sin(angle));
            vertices.add(new Point(x, y));
        }

        return vertices;
    }

}

