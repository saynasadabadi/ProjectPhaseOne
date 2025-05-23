package model;

import java.awt.Point;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GameModel {
    private NetworkModel networkModel;
    private Timer gameLoopTimer;
    private boolean gameRunning = false;
    private Runnable repaintCallback; // To trigger repaint on the panel
    private Runnable updateStatsCallback; // To update stats display

    public static final int TARGET_FPS = 60;
    public static final int GAME_UPDATE_DELAY = 1000 / TARGET_FPS; // Milliseconds

    private long lastUpdateTimeNanos = 0; // For delta-time calculation

    public GameModel() {
        this.networkModel = new NetworkModel();
    }

    public NetworkModel getNetworkModel() {
        return networkModel;
    }

    public void setNetworkModel(NetworkModel networkModel) {
        this.networkModel = networkModel;
    }

    public void setRepaintCallback(Runnable callback) {
        this.repaintCallback = callback;
    }

    public void setUpdateStatsCallback(Runnable callback) {
        this.updateStatsCallback = callback;
    }

    public boolean isNetworkModelValidForStart() {
        if (networkModel == null || networkModel.getSystems().isEmpty()) {
            return false;
        }
        // boolean allSystemsConnected = true; // Original check, can be reinstated if strictness is needed
        // for (NetworkSystem system : networkModel.getSystems()) {
        //     if (system.getIndicatorState() != IndicatorState.ON) {
        //         allSystemsConnected = false;
        //         break;
        //     }
        // }
        // if (!allSystemsConnected) {
        //     return false;
        // }

        boolean hasSourceWithPackets = networkModel.getSystems().stream()
                .filter(s -> s instanceof SourceNetworkSystem)
                .anyMatch(s -> !((SourceNetworkSystem) s).getSenderStorage().isEmpty());

        if (!hasSourceWithPackets) {
            networkModel.getSystems().stream()
                    .filter(s -> s instanceof SourceNetworkSystem)
                    .findFirst()
                    .ifPresent(s -> {
                        ((SourceNetworkSystem) s).generateAndStorePacket(PacketAndPortShape.SQUARE, 10);
                        ((SourceNetworkSystem) s).generateAndStorePacket(PacketAndPortShape.TRIANGLE, 8);
                    });
            hasSourceWithPackets = networkModel.getSystems().stream()
                    .filter(s -> s instanceof SourceNetworkSystem)
                    .anyMatch(s -> !((SourceNetworkSystem) s).getSenderStorage().isEmpty());
            // if (!hasSourceWithPackets) {
            //     return false; // Could be strict: if no packets even after trying to add, fail.
            // }
        }
        return true; // Lenient: allows starting even if sources are initially empty (they get auto-populated)
    }

    public void startTheGame() {
        if (gameRunning) {
            return;
        }
        if (isNetworkModelValidForStart()) {
            networkModel.resetSimulation();

            // Ensure Source Systems have some initial packets if their storage is empty
            // (This is also partially handled in isNetworkModelValidForStart's auto-population)
            for (NetworkSystem ns : networkModel.getSystems()) {
                if (ns instanceof SourceNetworkSystem) {
                    SourceNetworkSystem sns = (SourceNetworkSystem) ns;
                    if (sns.getSenderStorage().isEmpty()) {
                        sns.generateAndStorePacket(PacketAndPortShape.SQUARE, Packet.DEFAULT_RADIUS);
                        sns.generateAndStorePacket(PacketAndPortShape.TRIANGLE, Packet.DEFAULT_RADIUS);
                    }
                }
            }

            gameRunning = true;
            lastUpdateTimeNanos = System.nanoTime(); // Initialize last update time

            ActionListener gameUpdateAction = e -> {
                if (gameRunning) {
                    updateGameLogic(); // Changed from update(System.currentTimeMillis())
                    if (repaintCallback != null) {
                        repaintCallback.run();
                    }
                    if (updateStatsCallback != null) {
                        updateStatsCallback.run();
                    }
                } else {
                    if (gameLoopTimer != null) gameLoopTimer.stop();
                }
            };
            gameLoopTimer = new Timer(GAME_UPDATE_DELAY, gameUpdateAction);
            gameLoopTimer.setInitialDelay(0);
            gameLoopTimer.start();
        } else {
            // System.out.println("GameModel: Network is invalid or not ready. Cannot start the game.");
        }
    }

    public void stopTheGame() {
        if (gameRunning) {
            gameRunning = false;
            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
            networkModel.resetSimulation();
            if (repaintCallback != null) {
                repaintCallback.run();
            }
            if (updateStatsCallback != null) {
                updateStatsCallback.run();
            }
        }
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    // Renamed from update(long currentTimeMillis)
    private void updateGameLogic() {
        if (!gameRunning || networkModel == null) return;

        long currentTimeNanos = System.nanoTime();
        long deltaTimeNanos = currentTimeNanos - lastUpdateTimeNanos;

        // Prevent spiral of death or huge jumps if game was paused or lagging severely.
        // Clamp delta time to a max of, e.g., 3x the target frame delay.
        long maxReasonableDeltaNanos = (long)GAME_UPDATE_DELAY * 1_000_000L * 3L;
        if (deltaTimeNanos <= 0L || deltaTimeNanos > maxReasonableDeltaNanos) {
            deltaTimeNanos = (long)GAME_UPDATE_DELAY * 1_000_000L; // Default to one ideal frame's duration in nanos
        }
        this.lastUpdateTimeNanos = currentTimeNanos;

        // This factor scales Packet.SPEED based on actual elapsed time vs. ideal elapsed time per frame.
        // If deltaTimeNanos is exactly (GAME_UPDATE_DELAY * 1_000_000), speedFactor is 1.0.
        double idealFrameDurationNanos = (double)GAME_UPDATE_DELAY * 1_000_000.0;
        double speedFactor = deltaTimeNanos / idealFrameDurationNanos;

        long currentWallClockMillis = System.currentTimeMillis(); // For cooldowns that use wall clock time

        // 1. Attempt to release/forward packets from network systems
        for (NetworkSystem system : networkModel.getSystems()) {
            // Pass currentWallClockMillis if system cooldowns are based on it
            system.attemptPacketRelease(currentWallClockMillis, networkModel);
        }

        // 2. Update packet movement for packets on wires
        for (Packet packet : networkModel.getPackets()) { // Iterates over activePackets
            if (packet.getState() == PacketState.ON_WIRE) {
                Wire wire = packet.getCurrentWire();
                Port targetPort = packet.getTargetPort();
                Port originPort = packet.getOriginPort();

                if (wire == null || targetPort == null || originPort == null) {
                    System.err.println("Packet " + packet.getId() + " ON_WIRE with null essential references. Setting LOST.");
                    packet.setState(PacketState.LOST);
                    networkModel.addLostPacket(packet); // Ensure this correctly removes from activePackets
                    continue;
                }

                Point startPos = originPort.getAbsolutePosition();
                Point endPos = targetPort.getAbsolutePosition();
                double totalDistance = startPos.distance(endPos);

                if (totalDistance < 0.01) { // Effectively at target or invalid wire
                    packet.setProgressOnWire(1.0);
                } else {
                    // Packet.SPEED is defined as "pixels per game update (ideal frame)".
                    // Adjust this by the speedFactor to get distance for the current actual frame duration.
                    double distanceToCoverThisUpdate = Packet.SPEED * speedFactor;

                    double currentDistanceCovered = packet.getProgressOnWire() * totalDistance;
                    double newDistanceCovered = currentDistanceCovered + distanceToCoverThisUpdate;
                    packet.setProgressOnWire(Math.min(1.0, newDistanceCovered / totalDistance));
                }

                double progress = packet.getProgressOnWire();
                int newX = (int) (startPos.x + (endPos.x - startPos.x) * progress);
                int newY = (int) (startPos.y + (endPos.y - startPos.y) * progress);
                packet.setPosition(new Point(newX, newY));

                if (packet.getProgressOnWire() >= 1.0) {
                    packet.setPosition(new Point(endPos.x, endPos.y)); // Snap to target
                    originPort.setInUse(false); // Free the port the packet departed from

                    NetworkSystem destinationSystem = targetPort.getNetworkSystem();
                    if (destinationSystem != null) {
                        destinationSystem.processIncomingPacket(packet, targetPort, networkModel);
                    } else {
                        System.err.println("Packet " + packet.getId() + " arrived at port " + targetPort.getId() + " with no attached system. LOST.");
                        packet.setState(PacketState.LOST);
                        networkModel.addLostPacket(packet);
                    }
                }
            }
        }
        // Note: Iterating over networkModel.getPackets() (which is activePackets, a CopyOnWriteArrayList)
        // while potentially removing elements via addLostPacket/addDeliveredPacket is safe with CopyOnWriteArrayList.
    }
}