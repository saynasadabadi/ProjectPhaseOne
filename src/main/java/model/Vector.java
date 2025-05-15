package model;

public class Vector {
    double x,y;
    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector subtract(Vector other) {
        return new Vector(this.x - other.x, this.y - other.y);
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
}
