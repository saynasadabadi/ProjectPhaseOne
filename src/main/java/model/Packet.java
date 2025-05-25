package model;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // For unique ID

public class Packet {
    private final String id; // Added for tracking
    Point2D.Double position;
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

    // --- Fields for Acceleration/Deceleration ---
    private double currentSpeed; // Current speed of the packet along the wire
    public static final double MAX_SPEED = 2.0; // Max speed pixels per update
    public static final double ACCELERATION = 0.1;  // Speed increment per update
    public static final double DECELERATION = 0.2; // Speed decrement per update (should be > ACCELERATION for effective stopping)
    // --- End Fields for Acceleration/Deceleration ---

    // public static final double SPEED = 2.0; // Pixels per game update (frame), adjust as needed - REPLACED by currentSpeed
    public static final int DEFAULT_RADIUS = 8; // Default radius if not specified
    public static final double DEFAULT_MAX_NOISE = 100.0; // Default maximum noise threshold
    public static final double COLLISION_NOISE_INCREMENT = 80.0; // Noise added per collision
    // public static final double NOISE_DECAY_RATE = 0.05; // Noise decay is removed

    // Constants for displacement velocity due to impact
    private static final double DISPLACEMENT_DECAY_RATE = 0.35; // Adjusted from previous 0.85 to be a decay *rate*
    public static final double MIN_DISPLACEMENT_VELOCITY_MAGNITUDE = 0.01; // Threshold to consider velocity negligible
    private static final double IMPACT_FORCE_TO_VELOCITY_SCALE = 0.05; // Scales incoming force from collision/wave to velocity
    private static final double MAX_DISPLACEMENT_VELOCITY = 3.0; // Max magnitude of displacement velocity component
    private static final double WORLD_FRICTION_COEFFICIENT = 0.20; // For slowing down when knocked off wire

    public Packet(Point2D.Double position, PacketAndPortShape shape, int radius) {
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
        this.currentSpeed = 0.0; // Initialize current speed
    }

    // Constructor with default radius
    public Packet(Point2D.Double position, PacketAndPortShape shape) {
        this(position, shape, DEFAULT_RADIUS);
    }

    // New constructor for convenience
    public Packet(double x, double y, PacketAndPortShape shape, int radius) {
        this(new Point2D.Double(x, y), shape, radius);
    }

    public Packet(double x, double y, PacketAndPortShape shape) {
        this(new Point2D.Double(x,y), shape, DEFAULT_RADIUS);
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
        // This method is now empty as noise decay is removed
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
     * Updates packet's visual position based on displacement from impact velocity, and handles noise.
     * The packet's nominal position on the wire should have already been set by GameModel
     * calling wire.getPointAtProgress(packet.getProgressOnWire()) BEFORE this.
     * This method then applies any *additional* displacement.
     */
    public void applyDisplacementAndNoiseEffects() {
        // 1. Apply and decay displacement velocity
        if (displacementVelocity.magnitude() > MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
            // Apply displacement to current position
            if (this.position != null) { // Ensure position is not null before using it
                this.position.setLocation(this.position.getX() + displacementVelocity.getX(),
                                          this.position.getY() + displacementVelocity.getY());
            }

            // Decay displacement velocity (e.g., simple linear decay or exponential)
            // Assuming DISPLACEMENT_DECAY_RATE is a factor to reduce by (e.g., 0.15 means 15% decay)
            displacementVelocity = displacementVelocity.multiply(1.0 - DISPLACEMENT_DECAY_RATE);
            if (displacementVelocity.magnitude() < MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
                displacementVelocity = new Vector(0, 0); // Stop decaying if very small
            }
        } else if (displacementVelocity.magnitude() != 0) { // If it was non-zero but became too small
            displacementVelocity = new Vector(0, 0); // Ensure it's zero if below threshold
        }

        // 2. Noise handling (Decay removed)
        // The 'noise' variable now only increases from collisions/impacts and does not decay.
        // No code needed here for decay.

        // 3. Check for packet loss due to excessive noise
        // This check remains: if noise hits maxNoise, packet is lost.
        if (this.state != PacketState.LOST && this.state != PacketState.DELIVERED && noise >= maxNoise) {
            System.out.println("Packet " + id + " lost due to exceeding max noise: " + noise + "/" + maxNoise);
            this.setState(PacketState.LOST);
            this.setKnockedOffWire(true); // Consider it 'knocked off' if lost by noise
            if (this.originPort != null) { 
                this.originPort.setInUse(false);
            }
        }
    }
    
    /**
     * Frees the origin port when this packet is lost
     */
    public void freeOriginPort() {
        if (originPort != null && originPort.isInUse()) {
            originPort.setInUse(false);
            // System.out.println("Freed origin port " + originPort.getId() + " for lost packet " + getId());
        }
    }

    // === New methods for Acceleration/Deceleration ===

    /**
     * Resets packet's speed and progress for starting on a new wire.
     * To be called when packet is set to ON_WIRE state and assigned a wire.
     */
    public void initializeForWireMovement() {
        this.currentSpeed = 0.0;
        // this.progressOnWire = 0.0; // Progress should be set by initial placement on wire
        // position should already be at the origin port
    }

    /**
     * Updates the packet's current speed along the wire based on acceleration/deceleration rules.
     * This method ONLY updates currentSpeed. It does NOT update progressOnWire.
     * GameModel is responsible for updating position and then progressOnWire based on the new speed.
     *
     * @param speedFactor General factor from game loop (e.g., delta time based) - currently 1.0 for fixed steps.
     * @param wire The wire the packet is on.
     */
    public void updateCurrentSpeedOnWire(double speedFactor, Wire wire) {
        if (wire == null || state != PacketState.ON_WIRE) {
            return; // Or set currentSpeed = 0 if appropriate default
        }

        double wireLength = wire.getLength();
        if (wireLength < 0.001) { // Effectively a zero-length wire
            this.currentSpeed = 0.0;
            return;
        }

        // Read current progressOnWire to determine if deceleration is needed.
        // progressOnWire is managed by GameModel and reflects state from start of this tick or previous.
        double distanceToStopPixels = (currentSpeed * currentSpeed) / (2 * DECELERATION);
        double remainingDistancePixels = (1.0 - progressOnWire) * wireLength;

        boolean shouldDecelerate = false;
        // Check if we need to start decelerating, assuming progress is still before the end.
        if (currentSpeed > 0 && remainingDistancePixels <= distanceToStopPixels && progressOnWire < 1.0) {
            shouldDecelerate = true;
        }
        
        // Update speed
        if (shouldDecelerate) {
            currentSpeed -= DECELERATION * speedFactor;
            if (currentSpeed < 0) currentSpeed = 0;
        } else {
            // Only accelerate if not yet at the destination (progress < 1.0)
            // GameModel will set speed to 0 upon arrival.
            if (progressOnWire < 1.0) {
                currentSpeed += ACCELERATION * speedFactor;
                if (currentSpeed > MAX_SPEED) currentSpeed = MAX_SPEED;
            }
            // If progressOnWire is >= 1.0, it means packet has arrived or overshot.
            // Speed should be 0, which GameModel/arrival logic handles.
            // Or, force it here:
            // else {
            //    currentSpeed = 0.0;
            // }
        }
        
        // Final check: if progress is 1.0 or more, packet has arrived or overshot.
        // Speed should be zero. GameModel sets packet.setCurrentSpeed(0.0) on arrival.
        // This is an additional safeguard.
        if (progressOnWire >= 1.0) {
            currentSpeed = 0.0;
        }
    }
    
    /**
     * Applies world friction to the displacement velocity.
     * Called by GameModel when the packet is knocked off wire.
     */
    public void applyWorldFriction() {
        if (displacementVelocity.magnitude() > MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
            displacementVelocity = displacementVelocity.multiply(1.0 - WORLD_FRICTION_COEFFICIENT);
            if (displacementVelocity.magnitude() < MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
                displacementVelocity = new Vector(0, 0); // Stop decaying if very small
            }
        } else if (displacementVelocity.magnitude() != 0) { // Ensure it's zero if below threshold
             displacementVelocity = new Vector(0,0);
        }
    }

    // === Existing methods with some enhancements ===

    public List<Point2D.Double> getVertices() {
        // Use the provided getVertices logic, ensure shape is not null
        if (shape == null || position == null) {
            return new ArrayList<>(); // Return empty list if no shape or position
        }
        int numberOfSides = shape.getNumberOfSides();
        Point2D.Double center = this.position;
        double currentRadius = this.radius;

        List<Point2D.Double> vertices = new ArrayList<>(numberOfSides);
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
            double x = center.getX() + currentRadius * Math.cos(angle);
            double y = center.getY() + currentRadius * Math.sin(angle);
            vertices.add(new Point2D.Double(x, y));
        }
        return vertices;
    }

    public String getId() { return id; }
    public Point2D.Double getPosition() { return position; }
    public void setPosition(Point2D.Double position) { this.position = position; }

    // Convenience setter for position
    public void setPosition(double x, double y) {
        if (this.position == null) {
            this.position = new Point2D.Double(x,y);
        } else {
            this.position.setLocation(x,y);
        }
    }

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

    public double getCurrentSpeed() { return currentSpeed; }
    public void setCurrentSpeed(double currentSpeed) { this.currentSpeed = currentSpeed; }

    @Override
    public String toString() {
        return "Packet{" +
                "id='" + id + '\'' +
                ", shape=" + shape +
                ", state=" + state +
                ", position=" + (position != null ? String.format("(%.2f, %.2f)", position.getX(), position.getY()) : "null") +
                ", noise=" + String.format("%.1f", noise) +
                ", knockedOff=" + knockedOffWire +
                '}';
    }
}