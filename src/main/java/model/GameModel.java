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
    private final double timeLimitSeconds; // Time limit in seconds
    private final int maxTimeSteps; // Calculated based on time limit and FPS
    private boolean isTimeScrubbing = false; // Flag to indicate if we are manually controlling time
    private Map<Integer, NetworkModelSnapshot> history = new HashMap<>(); // To store snapshots
    // --- End Temporal Progress ---

    // --- Snapshot Execution State ---
    private boolean isExecutingSnapshots = false; // True when creating all snapshots
    private boolean snapshotsReady = false; // True when all snapshots are created
    // --- End Snapshot Execution State ---

    public GameModel(double timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
        this.maxTimeSteps = (int) Math.ceil(timeLimitSeconds * TARGET_FPS);
        this.networkModel = new NetworkModel();
        this.temporaryWireLength = 0.0;
    }

    // Backward compatibility constructor (default 10 seconds)
    public GameModel() {
        this(10.0); // Default 10 seconds
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

    public boolean isGamePaused() {
        return gamePaused;
    }

    public boolean isGameExecuting() {
        return gameRunning && !gamePaused;
    }

    public int getCurrentTimeStep() {
        return currentTimeStep;
    }

    public int getMaxTimeSteps() {
        return maxTimeSteps;
    }
    
    public double getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
    
    public double getCurrentTimeSeconds() {
        return (double) currentTimeStep / TARGET_FPS;
    }
    
    public double getRemainingTimeSeconds() {
        return timeLimitSeconds - getCurrentTimeSeconds();
    }
    
    public boolean isExecutingSnapshots() {
        return isExecutingSnapshots;
    }
    
    public boolean areSnapshotsReady() {
        return snapshotsReady;
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
     * Starts the full simulation by pre-computing all snapshots (Execute button).
     */
    public boolean startExecution() {
        if (gameRunning) return false;
        if (isNetworkModelValidForStart()) {
            gameRunning = true;
            gamePaused = true; // Start in paused state
            isExecutingSnapshots = true;
            snapshotsReady = false;
            currentTimeStep = 0;
            history.clear();
            
            // Show loading state
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
            
            // Pre-compute all snapshots in background thread
            new Thread(() -> {
                try {
                    executeAllSnapshots();
                } catch (Exception e) {
                    System.err.println("Error during snapshot execution: " + e.getMessage());
                    e.printStackTrace();
                    // Reset state on error
                    isExecutingSnapshots = false;
                    snapshotsReady = false;
                    gameRunning = false;
                    gamePaused = false;
                    if (repaintCallback != null) repaintCallback.run();
                    if (updateStatsCallback != null) updateStatsCallback.run();
                }
            }).start();
            
            return true;
        } else {
            System.out.println("GameModel: Network is invalid. Cannot start execution.");
            return false;
        }
    }
    
    /**
     * Pre-computes all snapshots for the entire simulation.
     */
    private void executeAllSnapshots() {
        System.out.println("Pre-computing " + maxTimeSteps + " snapshots...");
        
        // Reset to initial state
        networkModel.resetSimulation();
        prepareInitialPackets();
        
        // Store initial snapshot (step 0)
        history.put(0, new NetworkModelSnapshot(networkModel));
        
        // Simulate and store each step
        for (int step = 1; step <= maxTimeSteps; step++) {
            updateGameLogic(false); // Simulate one step
            history.put(step, new NetworkModelSnapshot(networkModel));
            
            // Update progress occasionally
            if (step % (maxTimeSteps / 10) == 0) {
                System.out.println("Snapshot progress: " + step + "/" + maxTimeSteps);
            }
        }
        
        // Mark snapshots as ready
        isExecutingSnapshots = false;
        snapshotsReady = true;
        currentTimeStep = 0; // Start at beginning
        
        // Restore to initial state
        loadSnapshot(0);
        
        System.out.println("All snapshots ready! Total: " + history.size());
        
        // Initialize the timer for automatic progression through snapshots
        initializeSnapshotTimer();
        
        // Update UI
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }

    /**
     * Initializes the timer for automatic progression through snapshots.
     */
    private void initializeSnapshotTimer() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        
        gameLoopTimer = new Timer(GAME_UPDATE_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gamePaused && snapshotsReady) {
                    // Advance to next snapshot
                    if (currentTimeStep < maxTimeSteps) {
                        loadSnapshot(currentTimeStep + 1);
                        
                        // Update UI
                        if (repaintCallback != null) repaintCallback.run();
                        if (updateStatsCallback != null) updateStatsCallback.run();
                    } else {
                        // End of simulation reached, pause automatically
                        pauseExecution();
                    }
                }
            }
        });
    }

    /**
     * Stops the continuous simulation.
     */
    public void stopExecution() {
        if (gameRunning) {
            gameRunning = false;
            gamePaused = false; // Reset pause state when stopping
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
        gamePaused = false; // Reset pause state
        snapshotsReady = false; // Reset snapshots ready state
        isExecutingSnapshots = false; // Reset executing snapshots state
        
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
        if (currentTimeStep < maxTimeSteps) {
            goToTimeStep(currentTimeStep + 1);
        }
    }

    /**
     * Steps the simulation backward by one step. Used for manual control.
     */
    public void timeStepBackward() {
        if (currentTimeStep > 0) {
            goToTimeStep(currentTimeStep - 1);
        }
    }

    /**
     * Jumps to a specific time step using pre-computed snapshots.
     * @param targetStep The desired time step.
     */
    public void goToTimeStep(int targetStep) {
        if (!snapshotsReady) {
            System.out.println("Snapshots not ready yet!");
            return;
        }
        
        // Pause automatic execution for manual navigation
        boolean wasExecuting = gameRunning && !gamePaused;
        if (wasExecuting) {
            pauseExecution();
        }
        isTimeScrubbing = true;

        targetStep = Math.max(0, Math.min(maxTimeSteps, targetStep));
        
        // Load the pre-computed snapshot
        loadSnapshot(targetStep);
        
        System.out.println("Loaded snapshot for time step: " + targetStep);
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }
    
    /**
     * Loads a specific snapshot and updates the current game state.
     */
    private void loadSnapshot(int step) {
        NetworkModelSnapshot snapshot = history.get(step);
        if (snapshot != null) {
            snapshot.restoreToModel(networkModel);
            currentTimeStep = step;
        } else {
            System.err.println("No snapshot found for step: " + step);
        }
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

    /**
     * Pauses the execution without stopping the timer.
     */
    public void pauseExecution() {
        if (gameRunning && !gamePaused) {
            gamePaused = true;
            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
            System.out.println("GameModel: Execution paused at step " + currentTimeStep);
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
        }
    }

    /**
     * Resumes the execution from pause.
     */
    public void resumeExecution() {
        if (gameRunning && gamePaused && snapshotsReady) {
            gamePaused = false;
            if (gameLoopTimer != null) {
                gameLoopTimer.start();
            }
            lastUpdateTimeNanos = System.nanoTime(); // Reset timing to avoid large delta
            System.out.println("GameModel: Execution resumed at step " + currentTimeStep);
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
        }
    }

    // --- Inner Class for Snapshots ---
    private static class NetworkModelSnapshot {
        private final List<Packet> activePackets;
        private final List<Packet> deliveredPackets;
        private final List<Packet> lostPackets;
        private final int playerCoins;
        
        // Store system states (for source systems: sender storage, for non-source: internal storage)
        private final Map<String, List<Packet>> systemStorages;
        private final Map<String, Long> systemLastReleaseTime;

        NetworkModelSnapshot(NetworkModel modelToCopy) {
            // Deep copy packet lists
            this.activePackets = deepCopyPacketList(modelToCopy.getPackets());
            this.deliveredPackets = deepCopyPacketList(modelToCopy.getDeliveredPackets());
            this.lostPackets = deepCopyPacketList(modelToCopy.getLostPackets());
            this.playerCoins = modelToCopy.getPlayerCoins();
            
            // Store system-specific states
            this.systemStorages = new HashMap<>();
            this.systemLastReleaseTime = new HashMap<>();
            
            for (NetworkSystem system : modelToCopy.getSystems()) {
                if (system instanceof SourceNetworkSystem) {
                    SourceNetworkSystem source = (SourceNetworkSystem) system;
                    this.systemStorages.put(system.getId(), deepCopyPacketList(new ArrayList<>(source.getSenderStorage())));
                } else if (system instanceof NonSourceNetworkSystem) {
                    NonSourceNetworkSystem nonSource = (NonSourceNetworkSystem) system;
                    this.systemStorages.put(system.getId(), deepCopyPacketList(new ArrayList<>(nonSource.getStorage())));
                }
                this.systemLastReleaseTime.put(system.getId(), system.lastPacketReleaseTimeMillis);
            }
        }
        
        private List<Packet> deepCopyPacketList(List<Packet> original) {
            List<Packet> copy = new ArrayList<>();
            for (Packet p : original) {
                copy.add(deepCopyPacket(p));
            }
            return copy;
        }
        
        private Packet deepCopyPacket(Packet original) {
            Packet copy = new Packet(new Point(original.getPosition()), original.getShape(), original.getRadius());
            copy.setState(original.getState());
            copy.setCurrentWire(original.getCurrentWire());
            copy.setTargetPort(original.getTargetPort());
            copy.setOriginPort(original.getOriginPort());
            copy.setProgressOnWire(original.getProgressOnWire());
            copy.setNetworkSystem(original.getNetworkSystem());
            return copy;
        }
        
        void restoreToModel(NetworkModel model) {
            // Clear current state
            model.getPackets().clear();
            model.getDeliveredPackets().clear();
            model.getLostPackets().clear();
            
            // Restore player coins first
            model.setPlayerCoins(this.playerCoins);
            
            // Restore packet lists
            for (Packet p : activePackets) {
                model.addPacketToActiveList(deepCopyPacket(p));
            }
            for (Packet p : deliveredPackets) {
                model.addDeliveredPacket(deepCopyPacket(p));
            }
            for (Packet p : lostPackets) {
                model.addLostPacket(deepCopyPacket(p));
            }
            
            // Restore system states
            for (NetworkSystem system : model.getSystems()) {
                if (system instanceof SourceNetworkSystem) {
                    SourceNetworkSystem source = (SourceNetworkSystem) system;
                    source.getSenderStorage().clear();
                    List<Packet> storedPackets = systemStorages.get(system.getId());
                    if (storedPackets != null) {
                        for (Packet p : storedPackets) {
                            source.getSenderStorage().offer(deepCopyPacket(p));
                        }
                    }
                } else if (system instanceof NonSourceNetworkSystem) {
                    NonSourceNetworkSystem nonSource = (NonSourceNetworkSystem) system;
                    nonSource.getStorage().clear();
                    List<Packet> storedPackets = systemStorages.get(system.getId());
                    if (storedPackets != null) {
                        for (Packet p : storedPackets) {
                            nonSource.getStorage().offer(deepCopyPacket(p));
                        }
                    }
                }
                
                Long lastReleaseTime = systemLastReleaseTime.get(system.getId());
                if (lastReleaseTime != null) {
                    system.lastPacketReleaseTimeMillis = lastReleaseTime;
                }
                
                // Reset port usage states
                for (Port port : system.getAllPorts()) {
                    port.setInUse(false);
                }
            }
        }
    }
}