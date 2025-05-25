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

    // --- Collision and Impact Wave System ---
    private List<ImpactWave> activeImpactWaves = new ArrayList<>();
    private boolean collisionDetectionEnabled = true;
    private boolean impactWavesEnabled = true;
    private long lastCollisionCheckTime = 0;
    private static final long COLLISION_CHECK_INTERVAL = 16; // Check every ~16ms (60fps)
    
    // Game Over tracking
    private boolean gameOverTriggered = false;
    private double packetLossThreshold = 50.0; // 50% loss triggers game over
    private boolean justGotGameOver = false; // To trigger dialog only once
    // --- End Collision System ---

    public GameModel(double timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
        this.maxTimeSteps = (int) Math.ceil(timeLimitSeconds * TARGET_FPS);
        this.networkModel = new NetworkModel();
        this.temporaryWireLength = 0.0;
    }

    // Backward compatibility constructor (default 60 seconds)
    public GameModel() {
        this(25.0); // Default 60 seconds
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
    
    // --- New collision and game over methods ---
    
    public boolean isGameOverTriggered() {
        return gameOverTriggered;
    }
    
    public boolean hasJustGotGameOver() {
        return justGotGameOver;
    }

    public void acknowledgeGameOver() {
        this.justGotGameOver = false;
    }
    
    public double getPacketLossPercentage() {
        if (networkModel == null) return 0.0;
        int totalPackets = networkModel.getDeliveredCount() + networkModel.getLostCount() + networkModel.getPackets().size();
        if (totalPackets == 0) return 0.0;
        return (double) networkModel.getLostCount() / totalPackets * 100.0;
    }
    
    public void setCollisionDetectionEnabled(boolean enabled) {
        this.collisionDetectionEnabled = enabled;
    }
    
    public void setImpactWavesEnabled(boolean enabled) {
        this.impactWavesEnabled = enabled;
    }
    
    public List<ImpactWave> getActiveImpactWaves() {
        return new ArrayList<>(activeImpactWaves);
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
        // Check if any source system has packets - but don't add defaults here
        // Initial packets should be set during system creation in MainFrame
        boolean hasSourceWithPackets = networkModel.getSystems().stream()
                .filter(s -> s instanceof SourceNetworkSystem)
                .anyMatch(s -> !((SourceNetworkSystem) s).getSenderStorage().isEmpty());

        if (!hasSourceWithPackets) {
            System.out.println("Warning: No source systems have packets. Make sure to add initial packets in MainFrame.setupInitialModel()");
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
            gameOverTriggered = false; // Reset game over state
            
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
        lastCollisionCheckTime = 0; // Reset collision check time for new snapshot generation
        
        // Store initial snapshot (step 0)
        history.put(0, new NetworkModelSnapshot(networkModel));
        
        // Simulate and store each step
        for (int step = 1; step <= maxTimeSteps; step++) {
            updateGameLogic(false); // Simulate one step
            history.put(step, new NetworkModelSnapshot(networkModel));
            
            // Check for game over during simulation
            if (!gameOverTriggered && getPacketLossPercentage() > packetLossThreshold) {
                gameOverTriggered = true;
                justGotGameOver = true; // Set when game over first occurs
                System.out.println("Game Over triggered at step " + step + " - Packet loss: " + String.format("%.1f%%", getPacketLossPercentage()));
                // break; // Stop simulation early - let it complete for full history
            }
            
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
                        
                        // Check for game over after loading the new step (during playback)
                        if (!gameOverTriggered && getPacketLossPercentage() > packetLossThreshold) {
                            gameOverTriggered = true;
                            justGotGameOver = true; // Set when game over first occurs during playback
                            System.out.println("Game Over triggered during playback at step " + currentTimeStep + " - Packet loss: " + String.format("%.1f%%", getPacketLossPercentage()));
                        }

                        // Update UI
                        if (repaintCallback != null) repaintCallback.run();
                        if (updateStatsCallback != null) updateStatsCallback.run();
                    } else {
                        // End of simulation reached or game over, pause automatically
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
        gameOverTriggered = false; // Reset game over state
        justGotGameOver = false; // Reset this flag too
        activeImpactWaves.clear(); // Clear impact waves
        
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
        // This method now only adds packets if systems have no initial packets
        // All initial packets should be set during system creation in MainFrame
        for (NetworkSystem ns : networkModel.getSystems()) {
            if (ns instanceof SourceNetworkSystem) {
                SourceNetworkSystem sns = (SourceNetworkSystem) ns;
                // Debug: Check current storage state
                System.out.println("SourceSystem " + sns.getId() + " storage size: " + sns.getSenderStorage().size());
                
                // Only add fallback packets if absolutely no packets were provided
                if (sns.getSenderStorage().isEmpty()) {
                    System.out.println("Warning: No initial packets provided for SourceSystem " + sns.getId() + ", adding minimal defaults");
                    sns.generateAndStorePacket(PacketAndPortShape.SQUARE, Packet.DEFAULT_RADIUS);
                }
            }
        }
    }


    /**
     * Updates the game logic by one step or based on delta-time.
     * Now includes collision detection and impact wave processing.
     * @param isLiveRun If true, uses delta-time; otherwise, uses fixed steps.
     */
    private void updateGameLogic(boolean isLiveRun) {
        if (networkModel == null) return;

        double speedFactor = 1.0; // Default for fixed steps
        long simulatedTimeMillis;

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
            simulatedTimeMillis = System.currentTimeMillis(); // For live run, use actual time
        } else {
            // For snapshot generation, calculate simulated time based on the current step.
            // history.size() gives the next step number (e.g., if step 0 is in, size is 1, so it's for step 1's logic)
            // However, attemptPacketRelease is usually based on a 'current' time.
            // Let's use the *start* of the current step's time.
            // When updateGameLogic is called in executeAllSnapshots for 'step s', history contains 0 to s-1.
            // So, the "current time" for this update can be considered (step-1) * GAME_UPDATE_DELAY if step > 0
            // or more simply, use the step number that is *about to be* snapshotted.
            // The loop in executeAllSnapshots is: for (int step = 1; step <= maxTimeSteps; step++)
            // updateGameLogic is called, then history.put(step, ...)
            // So, when updateGameLogic is called, 'step' from the loop is the current target step.
            // Let's find a way to pass the current step or derive it.
            // The history map grows. history.size() reflects the number of snapshots *already taken*.
            // If history has snapshot 0, size is 1. If it has 0 and 1, size is 2.
            // When updateGameLogic is called for step 's', history.size() = s.
            simulatedTimeMillis = (long)history.size() * GAME_UPDATE_DELAY;
        }

        // 1. Attempt packet release
        for (NetworkSystem system : networkModel.getSystems()) {
            system.attemptPacketRelease(simulatedTimeMillis, networkModel);
        }

        // 2. Update packet movement and physics
        List<Packet> packetsToProcess = new CopyOnWriteArrayList<>(networkModel.getPackets());
        for (Packet packet : packetsToProcess) {

            if (packet.getState() == PacketState.ON_WIRE) {
                Wire wire = packet.getCurrentWire();
                Port targetPort = packet.getTargetPort();
                Port originPort = packet.getOriginPort();

                // Call updateMovement first. This applies displacement and handles noise.
                packet.updateMovement();

                // Check if packet was lost due to noise during updateMovement()
                if (packet.getState() == PacketState.LOST) {
                    if (!networkModel.getLostPackets().contains(packet)) {
                        networkModel.addLostPacket(packet); 
                    }
                    continue; // Packet is lost, skip further ON_WIRE logic
                }

                // If wire/ports are invalid, packet is lost
                if (wire == null || targetPort == null || originPort == null) {
                    packet.setState(PacketState.LOST);
                    packet.setKnockedOffWire(true);
                    if (packet.getOriginPort() != null) packet.getOriginPort().setInUse(false); 
                    if (!networkModel.getLostPackets().contains(packet)) {
                         networkModel.addLostPacket(packet);
                    }
                    continue;
                }
                
                // Get wire start/end for calculations
                Point startPos = originPort.getAbsolutePosition();
                Point endPos = targetPort.getAbsolutePosition();
                double totalWireDistance = startPos.distance(endPos);

                // Update packet's progressOnWire based on its current (possibly displaced) position
                double projectedProgress = wire.calculateProgress(packet.getPosition());
                packet.setProgressOnWire(projectedProgress);

                // Check if packet is currently being displaced significantly by an impact
                if (packet.getDisplacementVelocity().magnitude() > Packet.MIN_DISPLACEMENT_VELOCITY_MAGNITUDE) {
                    // Packet is drifting. Its visual position is already set by updateMovement().
                    // Its progressOnWire now reflects its true projected position.
                    if (!CollisionDetector.isPacketStillOnWire(packet)) {
                        System.out.println("Packet " + packet.getId() + " knocked off wire " + wire.getId() + " while drifting.");
                        packet.setState(PacketState.LOST);
                        packet.setKnockedOffWire(true);
                        originPort.setInUse(false); 
                        if (!networkModel.getLostPackets().contains(packet)) {
                            networkModel.addLostPacket(packet);
                        }
                        continue; 
                    }
                    // If drifting but still on wire, it doesn't "arrive" this frame by following wire path strictly.
                } else {
                    // Displacement has worn off. Packet should adhere to wire and move forward.
                    // Calculate a small forward step based on speed.
                    double distanceToCover = Packet.SPEED * speedFactor;
                    double progressIncrement = (totalWireDistance > 0.01) ? (distanceToCover / totalWireDistance) : 0;
                    double newNominalProgress = packet.getProgressOnWire() + progressIncrement;
                    packet.setProgressOnWire(Math.min(1.0, newNominalProgress));

                    // Set visual position strictly according to the new progress on wire.
                    int newX = (int) (startPos.x + (endPos.x - startPos.x) * packet.getProgressOnWire());
                    int newY = (int) (startPos.y + (endPos.y - startPos.y) * packet.getProgressOnWire());
                    packet.setPosition(new Point(newX, newY));

                    if (!CollisionDetector.isPacketStillOnWire(packet)) {
                        System.out.println("Packet " + packet.getId() + " found off wire " + wire.getId() + " after nominal movement.");
                        packet.setState(PacketState.LOST);
                        packet.setKnockedOffWire(true);
                        originPort.setInUse(false); 
                        if (!networkModel.getLostPackets().contains(packet)) {
                            networkModel.addLostPacket(packet);
                        }
                        continue; 
                    }

                    // Check for arrival at destination
                    if (packet.getProgressOnWire() >= 1.0) {
                        packet.setPosition(new Point(endPos.x, endPos.y)); 
                        originPort.setInUse(false);
                        NetworkSystem destSystem = targetPort.getNetworkSystem();
                        if (destSystem != null) {
                            destSystem.processIncomingPacket(packet, targetPort, networkModel);
                        } else {
                            packet.setState(PacketState.LOST);
                            packet.setKnockedOffWire(true);
                            packet.freeOriginPort();
                            if (!networkModel.getLostPackets().contains(packet)) {
                                networkModel.addLostPacket(packet);
                            }
                        }
                    }
                }
            } else if (packet.getState() != PacketState.LOST && packet.getState() != PacketState.DELIVERED) {
                 // For packets IN_NETWORK_SYSTEM, or other non-ON_WIRE states, still run updateMovement for noise decay etc.
                 packet.updateMovement();
                 if (packet.getState() == PacketState.LOST) { // Check if lost by noise
                    if (!networkModel.getLostPackets().contains(packet)) {
                        networkModel.addLostPacket(packet);
                    }
                 }
            }
        }
        
        // 3. Collision Detection and Processing
        processCollisions(isLiveRun, simulatedTimeMillis);
        
        // 4. Update Impact Waves
        updateImpactWaves();
        
        // 5. Increment time step
        if(isLiveRun) {
            currentTimeStep++;
        } else if (!isLiveRun) {
            currentTimeStep++; // Also increment when simulating step-by-step
        }
    }

    /**
     * Processes collisions between packets and creates impact waves
     */
    private void processCollisions(boolean isLiveRun, long simulatedTimeMillis) {
        if (!collisionDetectionEnabled) return;
        
        long currentTimeForCheck;
        if (isLiveRun) {
            currentTimeForCheck = System.currentTimeMillis();
            if (currentTimeForCheck - lastCollisionCheckTime < COLLISION_CHECK_INTERVAL) {
                return; // Skip collision detection this frame for live run
            }
        } else { // Snapshot generation
            currentTimeForCheck = simulatedTimeMillis;
            // Skip if simulated time hasn't advanced enough since last check,
            // but always check for the first few steps (e.g. if lastCollisionCheckTime is 0 or very small).
            if (lastCollisionCheckTime != 0 && (currentTimeForCheck - lastCollisionCheckTime < COLLISION_CHECK_INTERVAL)) {
                return; 
            }
        }
        lastCollisionCheckTime = currentTimeForCheck;
        
        // Get only packets that are actively moving (ON_WIRE)
        List<Packet> movingPackets = new ArrayList<>();
        for (Packet packet : networkModel.getPackets()) {
            if (packet.getState() == PacketState.ON_WIRE && !packet.isKnockedOffWire()) {
                movingPackets.add(packet);
            }
        }
        
        if (movingPackets.size() < 2) return;
        
        // Use existing collision detector
        List<List<Packet>> collisionPairs = CollisionDetector.detectCollisions(movingPackets);
        
        // Process each collision
        for (List<Packet> pair : collisionPairs) {
            if (pair.size() == 2) {
                CollisionEvent collision = new CollisionEvent(pair.get(0), pair.get(1));
                
                // Process immediate collision effects (noise, separation forces)
                collision.processCollision();
                
                // Create impact wave if enabled
                if (impactWavesEnabled) {
                    ImpactWave wave = collision.createImpactWave();
                    activeImpactWaves.add(wave);
                }
                
                System.out.println("Collision detected: " + collision);
            }
        }
    }
    
    /**
     * Updates all active impact waves and applies their forces to nearby packets
     */
    private void updateImpactWaves() {
        if (!impactWavesEnabled) {
            activeImpactWaves.clear();
            return;
        }
        
        // Update wave expansion and remove inactive waves
        activeImpactWaves.removeIf(wave -> {
            wave.update();
            return !wave.isActive();
        });
        
        // Apply wave forces to packets
        for (Packet packet : networkModel.getPackets()) {
            if (packet.getState() == PacketState.ON_WIRE) {
                for (ImpactWave wave : activeImpactWaves) {
                    Vector force = wave.calculateForceAt(packet.getPosition());
                    if (force.magnitude() > 0.1) {
                        packet.applyImpactForce(force);
                    }
                }
            }
        }
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
            copy.setNoise(original.getNoise()); // Copy noise level
            copy.setKnockedOffWire(original.isKnockedOffWire()); // Copy knocked off state
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