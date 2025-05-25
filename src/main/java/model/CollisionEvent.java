package model;

import java.awt.Point;

/**
 * Represents a collision between two packets
 */
public class CollisionEvent {
    private Packet packet1;
    private Packet packet2;
    private Point collisionPoint;
    private long timestamp;
    private double impactStrength;
    
    public CollisionEvent(Packet packet1, Packet packet2) {
        this.packet1 = packet1;
        this.packet2 = packet2;
        this.timestamp = System.currentTimeMillis();
        
        // Calculate collision point (midpoint between packet centers)
        Point p1 = packet1.getPosition();
        Point p2 = packet2.getPosition();
        this.collisionPoint = new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        
        // Calculate impact strength based on relative speeds and sizes
        this.impactStrength = calculateImpactStrength();
    }
    
    /**
     * Calculates the strength of the impact based on packet properties
     */
    private double calculateImpactStrength() {
        // Base strength on packet sizes
        double baseStrength = (packet1.getRadius() + packet2.getRadius()) * 2.0;
        
        // Add velocity component if available
        Vector v1 = packet1.getVelocity();
        Vector v2 = packet2.getVelocity();
        if (v1 != null && v2 != null) {
            double relativeSpeed = v1.subtract(v2).magnitude();
            baseStrength += relativeSpeed * 5.0; // Amplify based on relative motion
        }
        
        return Math.min(100.0, baseStrength); // Cap at reasonable maximum
    }
    
    /**
     * Processes this collision by applying effects to both packets
     */
    public void processCollision() {
        // Add noise to both packets
        double noiseAmount = Packet.COLLISION_NOISE_INCREMENT + (impactStrength * 0.1);
        packet1.addNoise(noiseAmount);
        packet2.addNoise(noiseAmount);
        
        // Apply small impact forces to push packets apart
        Point p1 = packet1.getPosition();
        Point p2 = packet2.getPosition();
        
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0.01) { // Avoid division by zero
            // Normalize direction vector
            Vector separationForce1 = new Vector(-dx / distance, -dy / distance).multiply(impactStrength * 0.5);
            Vector separationForce2 = new Vector(dx / distance, dy / distance).multiply(impactStrength * 0.5);
            
            packet1.applyImpactForce(separationForce1);
            packet2.applyImpactForce(separationForce2);
        }
    }
    
    /**
     * Creates an impact wave from this collision
     */
    public ImpactWave createImpactWave() {
        return new ImpactWave(collisionPoint, impactStrength);
    }
    
    // Getters
    public Packet getPacket1() { return packet1; }
    public Packet getPacket2() { return packet2; }
    public Point getCollisionPoint() { return new Point(collisionPoint.x, collisionPoint.y); }
    public long getTimestamp() { return timestamp; }
    public double getImpactStrength() { return impactStrength; }
    
    @Override
    public String toString() {
        return String.format("CollisionEvent{p1=%s, p2=%s, point=%s, strength=%.1f}", 
                           packet1.getId(), packet2.getId(), collisionPoint, impactStrength);
    }
} 