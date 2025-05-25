package model;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // For unique ID

public class Packet {
    private final String id; // Added for tracking
    Point position;
    double noise; // Now functional - noise level from collisions
    Vector velocity; // Enhanced for impact wave effects - NOTE: This field is still present but not primarily used for impact displacement.
    PacketState state;
    Wire currentWire; // Renamed from 'wire' for clarity, and to match previous logic
    NetworkSystem networkSystem; // System it's currently inside (if IN_NETWORK_SYSTEM)
    PacketAndPortShape shape;
    int radius;

    // Fields for movement and wire travel logic
    private Port targetPort;
    private Port originPort;
    private double progressOnWire; // 0.0 to 1.0
    
    // New fields for enhanced collision and impact system
    private Vector displacementVelocity; // Velocity impulse from impacts, decays over time
    private double maxNoise; // Maximum noise before packet is lost
    private boolean knockedOffWire; // Flag if packet was knocked off its wire

    public static final double SPEED = 2.0; // Pixels per game update (frame), adjust as needed
    public static final int DEFAULT_RADIUS = 8; // Default radius if not specified
    public static final double DEFAULT_MAX_NOISE = 100.0; // Default maximum noise threshold
    public static final double COLLISION_NOISE_INCREMENT = 80.0; // Noise added per collision
    public static final double NOISE_DECAY_RATE = 0.5; // Noise reduction per frame

    // Constants for displacement velocity due to impact
    private static final double DISPLACEMENT_VELOCITY_DECAY = 0.85; // Decay factor per update
    public static final double MIN_DISPLACEMENT_VELOCITY_MAGNITUDE = 0.1; // Threshold to reset velocity - MADE PUBLIC
    private static final double IMPACT_FORCE_TO_VELOCITY_SCALE = 0.05; // Scales incoming force from collision/wave to velocity
    private static final double MAX_DISPLACEMENT_VELOCITY = 3.0; // Max magnitude of displacement velocity component

    public Packet(Point position, PacketAndPortShape shape, int radius) {
        this.id = UUID.randomUUID().toString();
        this.position = position;
        this.shape = shape;
        this.radius = (radius > 0) ? radius : DEFAULT_RADIUS;
        this.state = PacketState.IN_NETWORK_SYSTEM; // Initial state
        this.progressOnWire = 0.0;
        this.noise = 0.0; // Start with no noise
        this.maxNoise = DEFAULT_MAX_NOISE;
        this.displacementVelocity = new Vector(0, 0); // Initialize displacement velocity
        this.knockedOffWire = false;
        this.velocity = new Vector(0, 0); // Initialize base velocity (if used elsewhere)
    }

    // Constructor with default radius
    public Packet(Point position, PacketAndPortShape shape) {
        this(position, shape, DEFAULT_RADIUS);
    }

    // === Noise Management Methods ===
    
    /**
     * Adds noise to this packet due to collision or other factors
     */
    public void addNoise(double amount) {
        this.noise = Math.min(this.maxNoise, this.noise + amount);
    }
    
    /**
     * Reduces noise naturally over time
     */
    public void decayNoise() {
        this.noise = Math.max(0, this.noise - NOISE_DECAY_RATE);
    }
    
    /**
     * Checks if packet should be lost due to excessive noise
     */
    public boolean shouldBeLostDueToNoise() {
        return this.noise >= this.maxNoise;
    }
    
    /**
     * Gets the noise level as a percentage of maximum
     */
    public double getNoisePercentage() {
        return (this.noise / this.maxNoise) * 100.0;
    }
    
    /**
     * Resets noise to zero (for shop item effects)
     */
    public void resetNoise() {
        this.noise = 0.0;
    }

    // === Impact Force Management ===
    
    /**
     * Applies an impact force to this packet, contributing to a displacement velocity.
     * The force comes from a collision or an impact wave.
     */
    public void applyImpactForce(Vector forceFromCollisionOrWave) {
        Vector velocityImpulse = forceFromCollisionOrWave.multiply(IMPACT_FORCE_TO_VELOCITY_SCALE);
        this.displacementVelocity = this.displacementVelocity.add(velocityImpulse);

        // Cap the magnitude of the displacement velocity
        if (this.displacementVelocity.magnitude() > MAX_DISPLACEMENT_VELOCITY) {
            this.displacementVelocity = this.displacementVelocity.normalize().multiply(MAX_DISPLACEMENT_VELOCITY);
        }
    }
    
    /**
     * Checks if the packet has been knocked off its wire
     */
    public boolean isKnockedOffWire() {
        return knockedOffWire;
    }
    
    /**
     * Marks packet as knocked off wire
     */
    public void setKnockedOffWire(boolean knockedOff) {
        this.knockedOffWire = knockedOff;
    }

    // === Movement and Physics ===
    
    /**
     * Updates packet movement: applies displacement from impact velocity, decays noise.
     * The actual check for being knocked off the wire is handled in GameModel
     * using CollisionDetector.isPacketStillOnWire after all movements.
     */
    public void updateMovement() {
        // Apply displacement from accumulated impact velocity
        if (this.displacementVelocity.magnitude() > MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
            if (this.position != null) {
                this.position.translate((int)Math.round(this.displacementVelocity.getX()),
                                        (int)Math.round(this.displacementVelocity.getY()));
            }
            // Decay the velocity for the next frame
            this.displacementVelocity = this.displacementVelocity.multiply(DISPLACEMENT_VELOCITY_DECAY);
        } else if (this.displacementVelocity.magnitude() != 0) { // Avoid creating new vector if already zero
            // If force is too small, reset it to zero to avoid tiny calculations
            this.displacementVelocity = new Vector(0,0);
        }
        
        // Natural noise decay (still relevant for visual feedback or other mechanics)
        decayNoise();
        
        // Check if packet should be lost due to excessive noise (this can still happen independently)
        if (shouldBeLostDueToNoise() && state != PacketState.LOST && state != PacketState.DELIVERED) {
            setState(PacketState.LOST);
            freeOriginPort(); // Free the port when packet is lost due to noise
        }
    }
    
    /**
     * Frees the origin port when this packet is lost
     */
    public void freeOriginPort() {
        if (originPort != null && originPort.isInUse()) {
            originPort.setInUse(false);
            System.out.println("Freed origin port " + originPort.getId() + " for lost packet " + getId());
        }
    }

    // === Existing methods with some enhancements ===

    public List<Point> getVertices() {
        // Use the provided getVertices logic, ensure shape is not null
        if (shape == null || position == null) {
            return new ArrayList<>(); // Return empty list if no shape or position
        }
        int numberOfSides = shape.getNumberOfSides();
        Point center = this.position;
        double currentRadius = this.radius;

        List<Point> vertices = new ArrayList<>(numberOfSides);
        double angleIncrement = 2 * Math.PI / numberOfSides;
        double initialAngle = 0; // Default for circle-like or other polygons

        // Adjust initial angle for specific shapes to orient them as commonly expected
        if (numberOfSides == 4) { // Square
            initialAngle = Math.PI / 4; // Start at 45 degrees to make it flat
        } else if (numberOfSides == 3) { // Triangle
            initialAngle = -Math.PI / 2; // Start at -90 degrees to point upwards
        }


        for (int i = 0; i < numberOfSides; i++) {
            double angle = initialAngle + i * angleIncrement;
            int x = (int) (center.x + currentRadius * Math.cos(angle));
            int y = (int) (center.y + currentRadius * Math.sin(angle));
            vertices.add(new Point(x, y));
        }
        return vertices;
    }

    public String getId() { return id; }
    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }
    public PacketAndPortShape getShape() { return shape; }
    public java.awt.Color getColor() {
        if (shape != null) {
            Color baseColor = shape.getColor().brighter();
            
            // Tint color based on noise level - redder as noise increases
            if (noise > 0) {
                double noiseRatio = Math.min(1.0, noise / maxNoise);
                int red = Math.min(255, (int)(baseColor.getRed() + (255 - baseColor.getRed()) * noiseRatio));
                int green = (int)(baseColor.getGreen() * (1.0 - noiseRatio * 0.5));
                int blue = (int)(baseColor.getBlue() * (1.0 - noiseRatio * 0.5));
                return new Color(red, green, blue);
            }
            
            return baseColor;
        }
        return Color.WHITE; // Default color if shape is null
    }
    public int getRadius() { return radius; }
    public PacketState getState() { return state; }
    public void setState(PacketState state) { this.state = state; }

    public Wire getCurrentWire() { return currentWire; } // Renamed getter
    public void setCurrentWire(Wire wire) { this.currentWire = wire; } // Renamed setter

    public NetworkSystem getNetworkSystem() { return networkSystem; }
    public void setNetworkSystem(NetworkSystem networkSystem) { this.networkSystem = networkSystem; }

    // Getters and setters for existing fields
    public Port getTargetPort() { return targetPort; }
    public void setTargetPort(Port targetPort) { this.targetPort = targetPort; }

    public Port getOriginPort() { return originPort; }
    public void setOriginPort(Port originPort) { this.originPort = originPort; }

    public double getProgressOnWire() { return progressOnWire; }
    public void setProgressOnWire(double progressOnWire) { this.progressOnWire = progressOnWire; }

    // New getters and setters for enhanced features
    public double getNoise() { return noise; }
    public void setNoise(double noise) { this.noise = Math.max(0, Math.min(maxNoise, noise)); }
    
    public double getMaxNoise() { return maxNoise; }
    public void setMaxNoise(double maxNoise) { this.maxNoise = maxNoise; }
    
    public Vector getVelocity() { return velocity; }
    public void setVelocity(Vector velocity) { this.velocity = velocity; }

    public Vector getDisplacementVelocity() { return displacementVelocity; } // Getter for displacement velocity

    @Override
    public String toString() {
        return "Packet{" +
                "id='" + id + '\'' +
                ", shape=" + shape +
                ", state=" + state +
                ", position=" + position +
                ", noise=" + String.format("%.1f", noise) +
                ", knockedOff=" + knockedOffWire +
                '}';
    }
}