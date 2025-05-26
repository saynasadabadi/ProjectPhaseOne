package model;

import utils.SoundManager;import java.awt.geom.Point2D;
public class CollisionEvent {
    private Packet packet1;
    private Packet packet2;
    private Point2D.Double collisionPoint;    private long timestamp;
    private double impactStrength;
    
    public CollisionEvent(Packet packet1, Packet packet2) {
        this.packet1 = packet1;
        this.packet2 = packet2;
        this.timestamp = System.currentTimeMillis();
        
        Point2D.Double p1Pos = packet1.getPosition();        Point2D.Double p2Pos = packet2.getPosition();
        this.collisionPoint = new Point2D.Double((p1Pos.getX() + p2Pos.getX()) / 2.0,
                                                 (p1Pos.getY() + p2Pos.getY()) / 2.0);
        
        this.impactStrength = calculateImpactStrength();
    }
    
        private double calculateImpactStrength() {
        double baseStrength = (packet1.getRadius() + packet2.getRadius()) * 2.0;
        double speedComponent = (packet1.getCurrentSpeed() + packet2.getCurrentSpeed()) * 1.5;
        baseStrength += speedComponent;
        
        return Math.min(100.0, baseStrength);    }
    
        public void processCollision() {
        double noiseAmount = Packet.COLLISION_NOISE_INCREMENT + (impactStrength * 0.1);
        packet1.addNoise(noiseAmount);
        packet2.addNoise(noiseAmount);
        
        SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);
        Point2D.Double p1Pos = packet1.getPosition();
        Point2D.Double p2Pos = packet2.getPosition();
        
        double dx = p2Pos.getX() - p1Pos.getX();
        double dy = p2Pos.getY() - p1Pos.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0.01) {            Vector separationForce1 = new Vector(-dx / distance, -dy / distance).multiply(impactStrength * 0.5);
            Vector separationForce2 = new Vector(dx / distance, dy / distance).multiply(impactStrength * 0.5);
            
            packet1.applyImpactForce(separationForce1);
            packet2.applyImpactForce(separationForce2);
        }
    }
    
        public ImpactWave createImpactWave() {
        return new ImpactWave(collisionPoint, impactStrength);
    }
    
    public Packet getPacket1() { return packet1; }
    public Packet getPacket2() { return packet2; }
    public Point2D.Double getCollisionPoint() { return new Point2D.Double(collisionPoint.getX(), collisionPoint.getY()); }    public long getTimestamp() { return timestamp; }
    public double getImpactStrength() { return impactStrength; }
    
    @Override
    public String toString() {
        return String.format("CollisionEvent{p1=%s, p2=%s, point=(%.2f, %.2f), strength=%.1f}", 
                           packet1.getId(), packet2.getId(), collisionPoint.getX(), collisionPoint.getY(), impactStrength);
    }
} 