package model;

public class Vector {
    double x, y;
    
    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector subtract(Vector other) {
        return new Vector(this.x - other.x, this.y - other.y);
    }
    
    public Vector add(Vector other) {
        return new Vector(this.x + other.x, this.y + other.y);
    }
    
    public Vector multiply(double scalar) {
        return new Vector(this.x * scalar, this.y * scalar);
    }
    
    public Vector divide(double scalar) {
        if (scalar == 0) return new Vector(0, 0);
        return new Vector(this.x / scalar, this.y / scalar);
    }
    
    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }
    
    public Vector normalize() {
        double mag = magnitude();
        if (mag == 0) return new Vector(0, 0);
        return divide(mag);
    }
    
    public double distanceTo(Vector other) {
        return subtract(other).magnitude();
    }
    
    public double dotProduct(Vector other) {
        return this.x * other.x + this.y * other.y;
    }

    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    @Override
    public String toString() {
        return String.format("Vector(%.2f, %.2f)", x, y);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector vector = (Vector) obj;
        return Double.compare(vector.x, x) == 0 && Double.compare(vector.y, y) == 0;
    }
}
