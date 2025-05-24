package model;

import java.awt.Point;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameModel {
    private NetworkModel networkModel;
    private Timer gameLoopTimer;
    private boolean gameRunning = false;
    private boolean gamePaused = false; // New field to track pause state
    private Runnable repaintCallback;
    private Runnable updateStatsCallback;
    private double temporaryWireLength = 0.0;

    public static final int TARGET_FPS = 60;
    public static final int GAME_UPDATE_DELAY = 1000 / TARGET_FPS;
    private long lastUpdateTimeNanos = 0;

    // --- Temporal Progress ---
    private int currentTimeStep = 0;
    public static final int MAX_TIME_STEPS = 500; // Max simulation duration in steps
    private boolean isTimeScrubbing = false; // Flag to indicate if we are manually controlling time
    private Map<Integer, NetworkModelSnapshot> history = new HashMap<>(); // To store snapshots
    // --- End Temporal Progress ---


    public GameModel() {
        this.networkModel = new NetworkModel();
        this.temporaryWireLength = 0.0;
    }

    // ... (Keep existing getters and setters: getNetworkModel, setNetworkModel, etc.) ...
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

    public void triggerStatsUpdate() {
        if (this.updateStatsCallback != null) {
            this.updateStatsCallback.run();
        }
    }

    public double getTemporaryWireLength() {
        return temporaryWireLength;
    }

    public void setTemporaryWireLength(double length) {
        this.temporaryWireLength = length;
        if (this.updateStatsCallback != null) {
            this.updateStatsCallback.run();
        }
    }
    public boolean isGameRunning() {
        return gameRunning;
    }

    public int getCurrentTimeStep() {
        return currentTimeStep;
    }

    public int getMaxTimeSteps() {
        return MAX_TIME_STEPS;
    }
    // --- End Getters/Setters ---

    public boolean isNetworkModelValidForStart() {
        // ... (Keep existing validation logic) ...
        if (networkModel == null || networkModel.getSystems().isEmpty()) {
            return false;
        }

        for (NetworkSystem system : networkModel.getSystems()) {
            if (system.getIndicatorState() != IndicatorState.ON) {
                System.out.println("Game cannot start: System " + system.getId() + " indicator is OFF.");
                return false;
            }
        }
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
        }
        return true;
    }

    /**
     * Starts the full, continuous simulation (Execute button).
     */
    public boolean startExecution() {
        if (gameRunning) return false;
        if (isNetworkModelValidForStart()) {
            networkModel.resetSimulation(); // Reset to base state
            prepareInitialPackets();
            currentTimeStep = 0;
            history.clear();
            isTimeScrubbing = false; // We are running live
            gameRunning = true;
            lastUpdateTimeNanos = System.nanoTime();

            ActionListener gameUpdateAction = e -> {
                if (gameRunning && currentTimeStep < MAX_TIME_STEPS) {
                    updateGameLogic(true); // Run a live step
                    if (repaintCallback != null) repaintCallback.run();
                    if (updateStatsCallback != null) updateStatsCallback.run();
                } else {
                    stopExecution(); // Stop if max steps reached or manually stopped
                }
            };
            gameLoopTimer = new Timer(GAME_UPDATE_DELAY, gameUpdateAction);
            gameLoopTimer.setInitialDelay(0);
            gameLoopTimer.start();
            return true;
        } else {
            System.out.println("GameModel: Network is invalid. Cannot start execution.");
            return false;
        }
    }

    /**
     * Stops the continuous simulation.
     */
    public void stopExecution() {
        if (gameRunning) {
            gameRunning = false;
            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
            // Keep the last state when stopping, don't reset yet.
            // Allow scrubbing from here.
            isTimeScrubbing = true;
            System.out.println("GameModel: Execution stopped at step " + currentTimeStep);
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
        }
    }

    /**
     * Stops execution and returns to design mode, clearing all snapshots.
     */
    public void reDesign() {
        if (gameRunning) {
            gameRunning = false;
            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
        }
        
        // Clear all snapshots and reset to design mode
        history.clear();
        isTimeScrubbing = false;
        currentTimeStep = 0;
        
        // Reset the network simulation to initial state
        if (networkModel != null) {
            networkModel.resetSimulation();
        }
        
        System.out.println("GameModel: Switched to re-design mode. All snapshots cleared.");
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }

    /**
     * Steps the simulation forward by one step. Used for manual control.
     */
    public void timeStepForward() {
        if (gameRunning) stopExecution(); // Stop live run if stepping manually
        isTimeScrubbing = true;
        if (currentTimeStep < MAX_TIME_STEPS) {
            goToTimeStep(currentTimeStep + 1);
        }
    }

    /**
     * Steps the simulation backward by one step. Used for manual control.
     */
    public void timeStepBackward() {
        if (gameRunning) stopExecution();
        isTimeScrubbing = true;
        if (currentTimeStep > 0) {
            goToTimeStep(currentTimeStep - 1);
        }
    }

    /**
     * Jumps to a specific time step, simulating if necessary.
     * @param targetStep The desired time step.
     */
    public void goToTimeStep(int targetStep) {
        if (gameRunning) stopExecution();
        isTimeScrubbing = true;

        targetStep = Math.max(0, Math.min(MAX_TIME_STEPS, targetStep));

        // If we have a snapshot and it's valid, load it.
        // For now, we always re-simulate for simplicity as requested,
        // but history tracking could be added here later.
        // We *must* re-simulate if the network *might* have changed.
        // Since we allow changes anytime in scrub mode, always re-sim is safest.

        System.out.println("Going to time step: " + targetStep);
        networkModel.resetSimulation();
        prepareInitialPackets();
        history.clear(); // Clear history as we're re-simulating

        for (int i = 0; i < targetStep; i++) {
            updateGameLogic(false); // Run a simulated step (no delta-time needed)
        }

        // Ensure the final state is set
        currentTimeStep = targetStep;
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }


    /**
     * Prepares the initial packets in the source systems.
     */
    private void prepareInitialPackets() {
        for (NetworkSystem ns : networkModel.getSystems()) {
            if (ns instanceof SourceNetworkSystem) {
                SourceNetworkSystem sns = (SourceNetworkSystem) ns;
                // Add default packets ONLY if storage is empty.
                // You might want a more sophisticated level definition later.
                if (sns.getSenderStorage().isEmpty()) {
                    sns.generateAndStorePacket(PacketAndPortShape.SQUARE, Packet.DEFAULT_RADIUS);
                    sns.generateAndStorePacket(PacketAndPortShape.TRIANGLE, Packet.DEFAULT_RADIUS);
                    sns.generateAndStorePacket(PacketAndPortShape.SQUARE, 12);
                }
            }
        }
    }


    /**
     * Updates the game logic by one step or based on delta-time.
     * @param isLiveRun If true, uses delta-time; otherwise, uses fixed steps.
     */
    private void updateGameLogic(boolean isLiveRun) {
        if (networkModel == null) return;

        double speedFactor = 1.0; // Default for fixed steps
        if(isLiveRun) {
            long currentTimeNanos = System.nanoTime();
            long deltaTimeNanos = currentTimeNanos - lastUpdateTimeNanos;
            long maxReasonableDeltaNanos = (long)GAME_UPDATE_DELAY * 1_000_000L * 3L;
            if (deltaTimeNanos <= 0L || deltaTimeNanos > maxReasonableDeltaNanos) {
                deltaTimeNanos = (long)GAME_UPDATE_DELAY * 1_000_000L;
            }
            this.lastUpdateTimeNanos = currentTimeNanos;
            double idealFrameDurationNanos = (double)GAME_UPDATE_DELAY * 1_000_000.0;
            speedFactor = deltaTimeNanos / idealFrameDurationNanos;
        }

        long currentWallClockMillis = System.currentTimeMillis();

        // 1. Attempt packet release
        for (NetworkSystem system : networkModel.getSystems()) {
            system.attemptPacketRelease(currentWallClockMillis, networkModel);
        }

        // 2. Update packet movement
        List<Packet> packetsToProcess = new CopyOnWriteArrayList<>(networkModel.getPackets());
        for (Packet packet : packetsToProcess) {
            if (packet.getState() == PacketState.ON_WIRE) {
                Wire wire = packet.getCurrentWire();
                Port targetPort = packet.getTargetPort();
                Port originPort = packet.getOriginPort();

                if (wire == null || targetPort == null || originPort == null) {
                    packet.setState(PacketState.LOST);
                    networkModel.addLostPacket(packet);
                    continue;
                }

                Point startPos = originPort.getAbsolutePosition();
                Point endPos = targetPort.getAbsolutePosition();
                double totalDistance = startPos.distance(endPos);

                if (totalDistance < 0.01) {
                    packet.setProgressOnWire(1.0);
                } else {
                    double distanceToCover = Packet.SPEED * speedFactor;
                    double currentDistance = packet.getProgressOnWire() * totalDistance;
                    double newDistance = currentDistance + distanceToCover;
                    packet.setProgressOnWire(Math.min(1.0, newDistance / totalDistance));
                }

                double progress = packet.getProgressOnWire();
                int newX = (int) (startPos.x + (endPos.x - startPos.x) * progress);
                int newY = (int) (startPos.y + (endPos.y - startPos.y) * progress);
                packet.setPosition(new Point(newX, newY));

                if (packet.getProgressOnWire() >= 1.0) {
                    packet.setPosition(new Point(endPos.x, endPos.y));
                    originPort.setInUse(false);
                    NetworkSystem destSystem = targetPort.getNetworkSystem();
                    if (destSystem != null) {
                        destSystem.processIncomingPacket(packet, targetPort, networkModel);
                    } else {
                        packet.setState(PacketState.LOST);
                        networkModel.addLostPacket(packet);
                    }
                }
            }
        }
        // 3. Increment time step only if running live
        if(isLiveRun) {
            currentTimeStep++;
        } else if (!isLiveRun) {
            currentTimeStep++; // Also increment when simulating step-by-step
        }

        // 4. (Optional) Save snapshot if needed (can be heavy)
        // history.put(currentTimeStep, new NetworkModelSnapshot(networkModel));
    }

    // --- Inner Class for Snapshots (Optional but recommended for full history) ---
    // For now, we are re-simulating, so this isn't strictly needed yet.
    private static class NetworkModelSnapshot {
        // Store copies of systems, wires, and especially packet states/positions
        NetworkModelSnapshot(NetworkModel modelToCopy) {
            // Deep copy logic would go here
        }
        // Method to restore a model to this snapshot
    }
}