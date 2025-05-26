package model;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Packet {
    private final String id;
    Point2D.Double position;
    double noise;
    Vector velocity;
    PacketState state;
    Wire currentWire;
    NetworkSystem networkSystem;
    PacketAndPortShape shape;
    int radius;


    private Port targetPort;
    private Port originPort;
    private double progressOnWire;
    

    private Vector displacementVelocity;
    private double maxNoise;
    private boolean knockedOffWire;


    private double currentSpeed;
    private double effectiveMaxSpeed;
    public static final double MAX_SPEED = 2.0;
    public static final double ACCELERATION = 0.1;
    public static final double DECELERATION = 0.2;



    public static final int DEFAULT_RADIUS = 8;
    public static final double DEFAULT_MAX_NOISE = 100.0;
    public static final double COLLISION_NOISE_INCREMENT = 80.0;



    private static final double DISPLACEMENT_DECAY_RATE = 0.35;
    public static final double MIN_DISPLACEMENT_VELOCITY_MAGNITUDE = 0.01;
    private static final double IMPACT_FORCE_TO_VELOCITY_SCALE = 0.05;
    private static final double MAX_DISPLACEMENT_VELOCITY = 3.0;
    private static final double WORLD_FRICTION_COEFFICIENT = 0.20;

    public Packet(Point2D.Double position, PacketAndPortShape shape, int radius) {
        this.id = UUID.randomUUID().toString();
        this.position = position;
        this.shape = shape;
        this.radius = (radius > 0) ? radius : DEFAULT_RADIUS;
        this.state = PacketState.IN_NETWORK_SYSTEM;
        this.progressOnWire = 0.0;
        this.noise = 0.0;
        this.maxNoise = DEFAULT_MAX_NOISE;
        this.displacementVelocity = new Vector(0, 0);
        this.knockedOffWire = false;
        this.velocity = new Vector(0, 0);
        this.currentSpeed = 0.0;
    }


    public Packet(Point2D.Double position, PacketAndPortShape shape) {
        this(position, shape, DEFAULT_RADIUS);
    }


    public Packet(double x, double y, PacketAndPortShape shape, int radius) {
        this(new Point2D.Double(x, y), shape, radius);
    }

    public Packet(double x, double y, PacketAndPortShape shape) {
        this(new Point2D.Double(x,y), shape, DEFAULT_RADIUS);
    }


    
    
    public void addNoise(double amount) {
        this.noise = Math.min(this.maxNoise, this.noise + amount);
    }
    
    
    public void decayNoise() {

    }
    
    
    public boolean shouldBeLostDueToNoise() {
        return this.noise >= this.maxNoise;
    }
    
    
    public double getNoisePercentage() {
        return (this.noise / this.maxNoise) * 100.0;
    }
    
    
    public void resetNoise() {
        this.noise = 0.0;
    }


    
    
    public void applyImpactForce(Vector forceFromCollisionOrWave) {
        Vector velocityImpulse = forceFromCollisionOrWave.multiply(IMPACT_FORCE_TO_VELOCITY_SCALE);
        this.displacementVelocity = this.displacementVelocity.add(velocityImpulse);


        if (this.displacementVelocity.magnitude() > MAX_DISPLACEMENT_VELOCITY) {
            this.displacementVelocity = this.displacementVelocity.normalize().multiply(MAX_DISPLACEMENT_VELOCITY);
        }
    }
    
    
    public boolean isKnockedOffWire() {
        return knockedOffWire;
    }
    
    
    public void setKnockedOffWire(boolean knockedOff) {
        this.knockedOffWire = knockedOff;
    }


    
    
    public void applyDisplacementAndNoiseEffects() {

        if (displacementVelocity.magnitude() > MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {

            if (this.position != null) {
                this.position.setLocation(this.position.getX() + displacementVelocity.getX(),
                                          this.position.getY() + displacementVelocity.getY());
            }



            displacementVelocity = displacementVelocity.multiply(1.0 - DISPLACEMENT_DECAY_RATE);
            if (displacementVelocity.magnitude() < MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
                displacementVelocity = new Vector(0, 0);
            }
        } else if (displacementVelocity.magnitude() != 0) {
            displacementVelocity = new Vector(0, 0);
        }







        if (this.state != PacketState.LOST && this.state != PacketState.DELIVERED && noise >= maxNoise) {
            System.out.println("Packet " + id + " lost due to exceeding max noise: " + noise + "/" + maxNoise);
            this.setState(PacketState.LOST);
            this.setKnockedOffWire(true);
            if (this.originPort != null) { 
                this.originPort.setInUse(false);
            }
        }
    }
    
    
    public void freeOriginPort() {
        if (originPort != null && originPort.isInUse()) {
            originPort.setInUse(false);

        }
    }



    
    public void initializeForWireMovement(Port originPort) {




        PacketAndPortShape packetShape = this.getShape();
        PacketAndPortShape portShape = originPort.getShape();
        boolean isCompatible = (packetShape == portShape);

        this.effectiveMaxSpeed = MAX_SPEED;
        this.currentSpeed = 0.0;

        if (packetShape == PacketAndPortShape.SQUARE) {
            if (isCompatible) {
                this.effectiveMaxSpeed = MAX_SPEED / 2.0;
            } else {
                this.effectiveMaxSpeed = MAX_SPEED;
            }

        } else if (packetShape == PacketAndPortShape.TRIANGLE) {
            if (isCompatible) {
                this.effectiveMaxSpeed = MAX_SPEED;
                this.currentSpeed = MAX_SPEED;
            } else {
                this.effectiveMaxSpeed = MAX_SPEED;

            }
        }

    }

    
    public void updateCurrentSpeedOnWire(double speedFactor, Wire wire) {
        if (wire == null || state != PacketState.ON_WIRE) {
            return;
        }

        double wireLength = wire.getLength();
        if (wireLength < 0.001) {
            this.currentSpeed = 0.0;
            return;
        }



        double distanceToStopPixels = (currentSpeed * currentSpeed) / (2 * DECELERATION);
        double remainingDistancePixels = (1.0 - progressOnWire) * wireLength;

        boolean shouldDecelerate = false;

        if (currentSpeed > 0 && remainingDistancePixels <= distanceToStopPixels && progressOnWire < 1.0) {
            shouldDecelerate = true;
        }
        

        if (shouldDecelerate) {
            currentSpeed -= DECELERATION * speedFactor;
            if (currentSpeed < 0) currentSpeed = 0;
        } else {


            if (progressOnWire < 1.0) {
                currentSpeed += ACCELERATION * speedFactor;
                if (currentSpeed > this.effectiveMaxSpeed) currentSpeed = this.effectiveMaxSpeed;
            }






        }
        



        if (progressOnWire >= 1.0) {
            currentSpeed = 0.0;
        }
    }
    
    
    public void applyWorldFriction() {
        if (displacementVelocity.magnitude() > MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
            displacementVelocity = displacementVelocity.multiply(1.0 - WORLD_FRICTION_COEFFICIENT);
            if (displacementVelocity.magnitude() < MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
                displacementVelocity = new Vector(0, 0);
            }
        } else if (displacementVelocity.magnitude() != 0) {
             displacementVelocity = new Vector(0,0);
        }
    }



    public List<Point2D.Double> getVertices() {

        if (shape == null || position == null) {
            return new ArrayList<>();
        }
        int numberOfSides = shape.getNumberOfSides();
        Point2D.Double center = this.position;
        double currentRadius = this.radius;

        List<Point2D.Double> vertices = new ArrayList<>(numberOfSides);
        double angleIncrement = 2 * Math.PI / numberOfSides;
        double initialAngle = 0;


        if (numberOfSides == 4) {
            initialAngle = Math.PI / 4;
        } else if (numberOfSides == 3) {
            initialAngle = -Math.PI / 2;
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
            

            if (noise > 0) {
                double noiseRatio = Math.min(1.0, noise / maxNoise);
                int red = Math.min(255, (int)(baseColor.getRed() + (255 - baseColor.getRed()) * noiseRatio));
                int green = (int)(baseColor.getGreen() * (1.0 - noiseRatio * 0.5));
                int blue = (int)(baseColor.getBlue() * (1.0 - noiseRatio * 0.5));
                return new Color(red, green, blue);
            }
            
            return baseColor;
        }
        return Color.WHITE;
    }
    public int getRadius() { return radius; }
    public PacketState getState() { return state; }
    public void setState(PacketState state) { this.state = state; }

    public Wire getCurrentWire() { return currentWire; }
    public void setCurrentWire(Wire wire) { this.currentWire = wire; }

    public NetworkSystem getNetworkSystem() { return networkSystem; }
    public void setNetworkSystem(NetworkSystem networkSystem) { this.networkSystem = networkSystem; }


    public Port getTargetPort() { return targetPort; }
    public void setTargetPort(Port targetPort) { this.targetPort = targetPort; }

    public Port getOriginPort() { return originPort; }
    public void setOriginPort(Port originPort) { this.originPort = originPort; }

    public double getProgressOnWire() { return progressOnWire; }
    public void setProgressOnWire(double progressOnWire) { this.progressOnWire = progressOnWire; }


    public double getNoise() { return noise; }
    public void setNoise(double noise) { this.noise = Math.max(0, Math.min(maxNoise, noise)); }
    
    public double getMaxNoise() { return maxNoise; }
    public void setMaxNoise(double maxNoise) { this.maxNoise = maxNoise; }
    
    public Vector getVelocity() { return velocity; }
    public void setVelocity(Vector velocity) { this.velocity = velocity; }

    public Vector getDisplacementVelocity() { return displacementVelocity; }

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