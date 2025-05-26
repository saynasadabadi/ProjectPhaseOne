package model;

import java.awt.geom.Point2D;

public class ImpactWave {
    private Point2D.Double origin;
    private double currentRadius;
    private double maxRadius;
    private double strength;
    private double expansionSpeed;
    private boolean active;
    private long creationTime;
    
    public static final double DEFAULT_MAX_RADIUS = 30.0;
    public static final double DEFAULT_EXPANSION_SPEED = 10.0;
    public static final long WAVE_LIFETIME_MS = 1000;
    public ImpactWave(Point2D.Double origin, double strength) {
        this.origin = new Point2D.Double(origin.getX(), origin.getY());
        this.strength = strength;
        this.currentRadius = 0.0;
        this.maxRadius = DEFAULT_MAX_RADIUS;
        this.expansionSpeed = DEFAULT_EXPANSION_SPEED;
        this.active = true;
        this.creationTime = System.currentTimeMillis();
    }

    
        public void update() {
        if (!active) return;
        
        currentRadius += expansionSpeed;
        
        long currentTime = System.currentTimeMillis();
        if (currentRadius >= maxRadius || (currentTime - creationTime) > WAVE_LIFETIME_MS) {
            active = false;
        }
    }
    
        public Vector calculateForceAt(Point2D.Double position) {
        if (!active) {
            return new Vector(0, 0);
        }
        
        double dx = position.getX() - origin.getX();
        double dy = position.getY() - origin.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double waveThickness = 20.0;        if (distance < currentRadius - waveThickness || distance > currentRadius + waveThickness) {
            return new Vector(0, 0);
        }
        
        double distanceFactor = Math.max(0, 1.0 - (Math.abs(distance - currentRadius) / waveThickness));
        double forceMagnitude = strength * distanceFactor;
        
        if (distance < 0.01) {            return new Vector(0, 0);
        }
        
        Vector forceDirection = new Vector(dx / distance, dy / distance);
        return forceDirection.multiply(forceMagnitude);
    }
    
        public boolean affectsPosition(Point2D.Double position) {
        if (!active) return false;
        
        double dx = position.getX() - origin.getX();
        double dy = position.getY() - origin.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double waveThickness = 20.0;
        return distance >= currentRadius - waveThickness && distance <= currentRadius + waveThickness;
    }
    
    public Point2D.Double getOrigin() { return new Point2D.Double(origin.getX(), origin.getY()); }
    public double getCurrentRadius() { return currentRadius; }
    public double getMaxRadius() { return maxRadius; }
    public boolean isActive() { return active; }
    public long getAge() { return System.currentTimeMillis() - creationTime; }

    @Override
    public String toString() {
        return String.format("ImpactWave{origin=(%.2f, %.2f), radius=%.1f/%.1f, strength=%.1f, active=%s}", 
                           origin.getX(), origin.getY(), currentRadius, maxRadius, strength, active);
    }
} 