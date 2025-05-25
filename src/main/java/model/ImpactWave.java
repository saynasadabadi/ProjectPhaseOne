package model;

import java.awt.geom.Point2D;

/**
 * Represents an impact wave generated from packet collisions.
 * The wave emanates from a collision point and affects nearby packets.
 */
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
    public static final double DEFAULT_STRENGTH = 50.0;
    public static final long WAVE_LIFETIME_MS = 1000; // 2 seconds
    
    public ImpactWave(Point2D.Double origin, double strength) {
        this.origin = new Point2D.Double(origin.getX(), origin.getY());
        this.strength = strength;
        this.currentRadius = 0.0;
        this.maxRadius = DEFAULT_MAX_RADIUS;
        this.expansionSpeed = DEFAULT_EXPANSION_SPEED;
        this.active = true;
        this.creationTime = System.currentTimeMillis();
    }
    
    public ImpactWave(Point2D.Double origin) {
        this(origin, DEFAULT_STRENGTH);
    }
    
    /**
     * Updates the wave expansion and checks if it should be deactivated
     */
    public void update() {
        if (!active) return;
        
        // Expand the wave
        currentRadius += expansionSpeed;
        
        // Deactivate if reached max radius or lifetime exceeded
        long currentTime = System.currentTimeMillis();
        if (currentRadius >= maxRadius || (currentTime - creationTime) > WAVE_LIFETIME_MS) {
            active = false;
        }
    }
    
    /**
     * Calculates the force this wave applies to a packet at the given position
     */
    public Vector calculateForceAt(Point2D.Double position) {
        if (!active) {
            return new Vector(0, 0);
        }
        
        // Calculate distance from wave origin to packet
        double dx = position.getX() - origin.getX();
        double dy = position.getY() - origin.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Check if packet is within the current wave radius (with some tolerance)
        double waveThickness = 20.0; // Wave has some thickness
        if (distance < currentRadius - waveThickness || distance > currentRadius + waveThickness) {
            return new Vector(0, 0);
        }
        
        // Calculate force magnitude based on distance and wave strength
        double distanceFactor = Math.max(0, 1.0 - (Math.abs(distance - currentRadius) / waveThickness));
        double forceMagnitude = strength * distanceFactor;
        
        // Force direction is away from the origin
        if (distance < 0.01) { // Avoid division by zero
            return new Vector(0, 0);
        }
        
        Vector forceDirection = new Vector(dx / distance, dy / distance);
        return forceDirection.multiply(forceMagnitude);
    }
    
    /**
     * Checks if this wave can affect a packet at the given position
     */
    public boolean affectsPosition(Point2D.Double position) {
        if (!active) return false;
        
        double dx = position.getX() - origin.getX();
        double dy = position.getY() - origin.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double waveThickness = 20.0;
        return distance >= currentRadius - waveThickness && distance <= currentRadius + waveThickness;
    }
    
    // Getters and setters
    public Point2D.Double getOrigin() { return new Point2D.Double(origin.getX(), origin.getY()); }
    public double getCurrentRadius() { return currentRadius; }
    public double getMaxRadius() { return maxRadius; }
    public double getStrength() { return strength; }
    public boolean isActive() { return active; }
    public long getAge() { return System.currentTimeMillis() - creationTime; }
    
    public void setMaxRadius(double maxRadius) { this.maxRadius = maxRadius; }
    public void setExpansionSpeed(double expansionSpeed) { this.expansionSpeed = expansionSpeed; }
    public void setStrength(double strength) { this.strength = strength; }
    public void deactivate() { this.active = false; }
    
    @Override
    public String toString() {
        return String.format("ImpactWave{origin=(%.2f, %.2f), radius=%.1f/%.1f, strength=%.1f, active=%s}", 
                           origin.getX(), origin.getY(), currentRadius, maxRadius, strength, active);
    }
} 