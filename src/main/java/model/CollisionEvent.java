package model;

// import java.awt.Point;
import java.awt.geom.Point2D; // Import Point2D

/**
 * Represents a collision between two packets
 */
public class CollisionEvent {
    private Packet packet1;
    private Packet packet2;
    private Point2D.Double collisionPoint; // Changed to Point2D.Double
    private long timestamp;
    private double impactStrength;
    
    public CollisionEvent(Packet packet1, Packet packet2) {
        this.packet1 = packet1;
        this.packet2 = packet2;
        this.timestamp = System.currentTimeMillis();
        
        // Calculate collision point (midpoint between packet centers)
        Point2D.Double p1Pos = packet1.getPosition(); // Packet position is Point2D.Double
        Point2D.Double p2Pos = packet2.getPosition();
        this.collisionPoint = new Point2D.Double((p1Pos.getX() + p2Pos.getX()) / 2.0,
                                                 (p1Pos.getY() + p2Pos.getY()) / 2.0);
        
        // Calculate impact strength based on relative speeds and sizes
        this.impactStrength = calculateImpactStrength();
    }
    
    /**
     * Calculates the strength of the impact based on packet properties
     */
    private double calculateImpactStrength() {
        // Base strength on packet sizes
        double baseStrength = (packet1.getRadius() + packet2.getRadius()) * 2.0; // Example: (8+8)*2 = 32

        // Add component based on their current speeds from nominal wire movement
        double speedComponent = (packet1.getCurrentSpeed() + packet2.getCurrentSpeed()) * 1.5; // Reduced multiplier from 3.5 to 1.5

        baseStrength += speedComponent;
        
        // System.out.println("Impact Strength: Base=" + ((packet1.getRadius() + packet2.getRadius()) * 2.0) + ", SpeedComp=" + speedComponent + ", TotalRaw=" + baseStrength);
        return Math.min(100.0, baseStrength); // Reduced cap from 150.0 to 100.0 as overall strength is lower
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
        Point2D.Double p1Pos = packet1.getPosition();
        Point2D.Double p2Pos = packet2.getPosition();
        
        double dx = p2Pos.getX() - p1Pos.getX();
        double dy = p2Pos.getY() - p1Pos.getY();
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
    public Point2D.Double getCollisionPoint() { return new Point2D.Double(collisionPoint.getX(), collisionPoint.getY()); } // Return Point2D.Double
    public long getTimestamp() { return timestamp; }
    public double getImpactStrength() { return impactStrength; }
    
    @Override
    public String toString() {
        return String.format("CollisionEvent{p1=%s, p2=%s, point=(%.2f, %.2f), strength=%.1f}", 
                           packet1.getId(), packet2.getId(), collisionPoint.getX(), collisionPoint.getY(), impactStrength);
    }
} 