package model;

import utils.SoundManager;

import java.awt.Point;
import java.awt.geom.Point2D;
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
    private boolean gamePaused = false;    private Runnable repaintCallback;
    private Runnable updateStatsCallback;
    private double temporaryWireLength = 0.0;

    public static final int TARGET_FPS = 60;
    public static final int GAME_UPDATE_DELAY = 1000 / TARGET_FPS;
    private long lastUpdateTimeNanos = 0;

    private int currentTimeStep = 0;
    private final double timeLimitSeconds;    private final int maxTimeSteps;    private boolean isTimeScrubbing = false;    private Map<Integer, NetworkModelSnapshot> history = new HashMap<>();
    private boolean isExecutingSnapshots = false;    private boolean snapshotsReady = false;
    private List<ImpactWave> activeImpactWaves = new ArrayList<>();
    private boolean collisionDetectionEnabled = true;
    private boolean impactWavesEnabled = true;
    private long lastCollisionCheckTime = 0;
    private static final long COLLISION_CHECK_INTERVAL = 16;    
    private boolean gameOverTriggered = false;
    private double packetLossThreshold = 50.0;    private boolean justGotGameOver = false;
    private boolean shopOpen = false;
    private boolean wasPlayingBeforeShop = false;
    public GameModel(double timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
        this.maxTimeSteps = (int) Math.ceil(timeLimitSeconds * TARGET_FPS);
        this.networkModel = new NetworkModel();
        this.temporaryWireLength = 0.0;
    }

    public GameModel() {
        this(10.0);    }

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
    
    public boolean isShopOpen() {        return shopOpen;
    }

    public int getCoins() {
        if (networkModel != null) {
            return networkModel.getPlayerCoins();
        }
        return 0;
    }

    public void purchasePowerUp(String powerUpName, int cost) {
        if (networkModel != null && networkModel.getPlayerCoins() >= cost) {
            networkModel.setPlayerCoins(networkModel.getPlayerCoins() - cost);
            System.out.println("GameModel: Purchased " + powerUpName + " for " + cost + " coins. Remaining: " + networkModel.getPlayerCoins());
            if (updateStatsCallback != null) {
                updateStatsCallback.run();
            }
        } else {
            System.out.println("GameModel: Not enough coins or network model not available for purchase.");
        }
    }

    public void openShop() {
        if (gameRunning && snapshotsReady && !shopOpen && !gameOverTriggered) {
            this.wasPlayingBeforeShop = !this.gamePaused;
            if (this.wasPlayingBeforeShop) {                if (gameLoopTimer != null) {
                    gameLoopTimer.stop();
                }
                this.gamePaused = true;                System.out.println("GameModel: Game paused for shop.");
            }

            this.shopOpen = true;
            System.out.println("GameModel: Shop opened.");

            if (updateStatsCallback != null) {
                updateStatsCallback.run();
            }
        } else {
            System.out.println("GameModel: Shop cannot be opened. Conditions: gameRunning=" + gameRunning +
                               ", snapshotsReady=" + snapshotsReady + ", shopOpen=" + shopOpen + ", gameOverTriggered=" + gameOverTriggered);
        }
    }

    public void closeShop() {
        if (this.shopOpen) {
            this.shopOpen = false;
            if (this.wasPlayingBeforeShop) {                this.gamePaused = false;                if (gameLoopTimer != null) {
                    gameLoopTimer.start();
                }
                lastUpdateTimeNanos = System.nanoTime();                System.out.println("GameModel: Game resumed after shop.");
            }

            System.out.println("GameModel: Shop closed.");
            this.wasPlayingBeforeShop = false;
            if (updateStatsCallback != null) {
                updateStatsCallback.run();
            }
        }
    }


    public boolean isNetworkModelValidForStart() {
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
            System.out.println("Warning: No source systems have packets. Make sure to add initial packets in MainFrame.setupInitialModel()");
        }
        return true;
    }

        public boolean startExecution() {
        if (gameRunning) return false;
        if (isNetworkModelValidForStart()) {
            gameRunning = true;
            gamePaused = true;            isExecutingSnapshots = true;
            snapshotsReady = false;
            currentTimeStep = 0;
            history.clear();
            gameOverTriggered = false;            
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
            
            new Thread(() -> {
                try {
                    executeAllSnapshots();
                } catch (Exception e) {
                    System.err.println("Error during snapshot execution: " + e.getMessage());
                    e.printStackTrace();
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
    
        private void executeAllSnapshots() {
        System.out.println("Pre-computing " + maxTimeSteps + " snapshots...");
        
        networkModel.resetSimulation();
        prepareInitialPackets();
        lastCollisionCheckTime = 0;        
        history.put(0, new NetworkModelSnapshot(networkModel));
        
        for (int step = 1; step <= maxTimeSteps; step++) {
            updateGameLogic(false);            history.put(step, new NetworkModelSnapshot(networkModel));
            
            if (!gameOverTriggered && getPacketLossPercentage() > packetLossThreshold) {
                gameOverTriggered = true;
                justGotGameOver = true;                System.out.println("Game Over triggered at step " + step + " - Packet loss: " + String.format("%.1f%%", getPacketLossPercentage()));
            }
            
            if (step % (maxTimeSteps / 10) == 0) {
                System.out.println("Snapshot progress: " + step + "/" + maxTimeSteps);
            }
        }
        
        System.out.println("Time limit reached at step " + currentTimeStep + ". Marking remaining undelivered packets as LOST.");

        List<Packet> activePacketsCopy = new ArrayList<>(networkModel.getPackets());
        for (Packet packet : activePacketsCopy) {
            if (packet.getState() != PacketState.DELIVERED) {                packet.setState(PacketState.LOST);
                packet.freeOriginPort();                networkModel.addLostPacket(packet);                SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);            }
        }

        for (NetworkSystem system : networkModel.getSystems()) {
            if (system instanceof SourceNetworkSystem) {
                SourceNetworkSystem sourceSystem = (SourceNetworkSystem) system;
                while (!sourceSystem.getSenderStorage().isEmpty()) {
                    Packet packet = sourceSystem.getSenderStorage().poll();                    if (packet != null) {
                        packet.setState(PacketState.LOST);
                        networkModel.addLostPacket(packet);                        SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);                    }
                }
            }
        }

        history.put(maxTimeSteps, new NetworkModelSnapshot(networkModel));
        
        isExecutingSnapshots = false;
        snapshotsReady = true;
        currentTimeStep = 0;        
        loadSnapshot(0);
        
        System.out.println("All snapshots ready! Total: " + history.size());
        
        initializeSnapshotTimer();
        
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }

        private void initializeSnapshotTimer() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        
        gameLoopTimer = new Timer(GAME_UPDATE_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameRunning && !gamePaused && snapshotsReady) {
                    if (currentTimeStep < maxTimeSteps) { 
                        loadSnapshot(currentTimeStep + 1);
                        
                        if (!gameOverTriggered && getPacketLossPercentage() > packetLossThreshold) {
                            gameOverTriggered = true;
                            justGotGameOver = true;                            System.out.println("Game Over triggered during playback at step " + currentTimeStep + " - Packet loss: " + String.format("%.1f%%", getPacketLossPercentage()));
                        }

                        if (repaintCallback != null) repaintCallback.run();
                        if (updateStatsCallback != null) updateStatsCallback.run();
                    } else {
                        pauseExecution();
                    }
                }
            }
        });
    }

        public void stopExecution() {
        if (gameRunning) {
            gameRunning = false;
            gamePaused = false;            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
            isTimeScrubbing = true;
            System.out.println("GameModel: Execution stopped at step " + currentTimeStep);
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
        }
    }

        public void reDesign() {
        if (gameRunning) {
            gameRunning = false;
            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
        }
        
        history.clear();
        isTimeScrubbing = false;
        currentTimeStep = 0;
        gamePaused = false; 
        snapshotsReady = false; 
        isExecutingSnapshots = false; 
        gameOverTriggered = false; 
        justGotGameOver = false; 
        activeImpactWaves.clear(); 
        
        if (networkModel != null) {
            networkModel.resetSimulation();        }
        
        System.out.println("GameModel: Switched to re-design mode. All snapshots cleared.");
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }

        public void restartFromInitialSnapshot() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        gameRunning = false;        gamePaused = false;        isTimeScrubbing = false;        gameOverTriggered = false;
        justGotGameOver = false;
        activeImpactWaves.clear(); 

        if (history.containsKey(0)) {
            loadSnapshot(0);            this.currentTimeStep = 0; 
            if (this.networkModel != null) {
                this.networkModel.resetSimulation();
            }
            snapshotsReady = true; 
        } else {
            System.err.println("Error: Cannot restart from initial snapshot. Snapshot 0 not found. Performing full redesign.");
            reDesign();            return;
        }

        System.out.println("GameModel: Level restarted from initial snapshot (Time Step 0).");
        if (repaintCallback != null) repaintCallback.run();
        if (updateStatsCallback != null) updateStatsCallback.run();
    }

        public void timeStepForward() {
        if (gameRunning && gamePaused && snapshotsReady && !isTimeScrubbing && !shopOpen) {
            goToTimeStep(Math.min(maxTimeSteps, currentTimeStep + 1));
        }
    }

        public void timeStepBackward() {
        if (gameRunning && gamePaused && snapshotsReady && !isTimeScrubbing && !shopOpen) {
            goToTimeStep(Math.max(0, currentTimeStep - 1));
        }
    }

        public void goToTimeStep(int targetStep) {
        if (gameRunning && gamePaused && snapshotsReady && !shopOpen) {
            if (targetStep >= 0 && targetStep <= maxTimeSteps) {
                if (!snapshotsReady) {
                    System.out.println("Snapshots not ready yet!");
                    return;
                }
                
                boolean wasExecuting = gameRunning && !gamePaused;
                if (wasExecuting) {
                    pauseExecution();
                }
                isTimeScrubbing = true;

                targetStep = Math.max(0, Math.min(maxTimeSteps, targetStep));
                
                loadSnapshot(targetStep);
                
                System.out.println("Loaded snapshot for time step: " + targetStep);
                if (repaintCallback != null) repaintCallback.run();
                if (updateStatsCallback != null) updateStatsCallback.run();
            }
        }
    }
    
        private void loadSnapshot(int step) {
        NetworkModelSnapshot snapshot = history.get(step);
        if (snapshot != null) {
            snapshot.restoreToModel(networkModel);
            currentTimeStep = step;
        } else {
            System.err.println("No snapshot found for step: " + step);
        }
    }

        private void prepareInitialPackets() {
        for (NetworkSystem ns : networkModel.getSystems()) {
            if (ns instanceof SourceNetworkSystem) {
                SourceNetworkSystem sns = (SourceNetworkSystem) ns;
                System.out.println("SourceSystem " + sns.getId() + " storage size: " + sns.getSenderStorage().size());
                
                if (sns.getSenderStorage().isEmpty()) {
                    System.out.println("Warning: No initial packets provided for SourceSystem " + sns.getId() + ", adding minimal defaults");
                    sns.generateAndStorePacket(PacketAndPortShape.SQUARE, Packet.DEFAULT_RADIUS);
                }
            }
        }
    }


        private void updateGameLogic(boolean isLiveRun) {
        if (networkModel == null) return;

        double speedFactor = 1.0;        long simulatedTimeMillis;

        if(isLiveRun) {
            long currentTimeNanos = System.nanoTime();
            if (lastUpdateTimeNanos == 0) lastUpdateTimeNanos = currentTimeNanos - (long)(GAME_UPDATE_DELAY * 1_000_000L);            long deltaTimeNanos = currentTimeNanos - lastUpdateTimeNanos;
            long maxReasonableDeltaNanos = (long)GAME_UPDATE_DELAY * 1_000_000L * 5L;            if (deltaTimeNanos <= 0L ) deltaTimeNanos = (long)GAME_UPDATE_DELAY * 1_000_000L;            if (deltaTimeNanos > maxReasonableDeltaNanos) deltaTimeNanos = maxReasonableDeltaNanos;            
            this.lastUpdateTimeNanos = currentTimeNanos;
            double idealFrameDurationNanos = (double)GAME_UPDATE_DELAY * 1_000_000.0;
            speedFactor = deltaTimeNanos / idealFrameDurationNanos;
            simulatedTimeMillis = System.currentTimeMillis(); 
        } else {
            simulatedTimeMillis = (long)history.size() * GAME_UPDATE_DELAY;
        }

        for (NetworkSystem system : networkModel.getSystems()) {
            system.attemptPacketRelease(simulatedTimeMillis, networkModel);
        }

        List<Packet> packetsToProcess = new CopyOnWriteArrayList<>(networkModel.getPackets());
        for (Packet packet : packetsToProcess) {

            if (packet.getState() == PacketState.ON_WIRE) {
                Wire wire = packet.getCurrentWire();
                Port targetPort = packet.getTargetPort();
                Port originPort = packet.getOriginPort();

                if (wire == null || targetPort == null || originPort == null) {
                    packet.setState(PacketState.LOST);
                    packet.setKnockedOffWire(true);
                    if (packet.getOriginPort() != null) packet.getOriginPort().setInUse(false); 
                    if (!networkModel.getLostPackets().contains(packet)) {
                         networkModel.addLostPacket(packet);
                         SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);
                    }
                    continue;
                }

                packet.updateCurrentSpeedOnWire(speedFactor, wire);

                Point2D.Double wireDirStart = wire.getSourceAbsolutePosition();
                Point2D.Double wireDirEnd = wire.getDestinationAbsolutePosition();
                double dx = wireDirEnd.x - wireDirStart.x;
                double dy = wireDirEnd.y - wireDirStart.y;
                double wireActualLength = wire.getLength();                
                Vector nominalMovementVector = new Vector(0,0);
                if (wireActualLength > 0.001) {
                    double normalizedDx = dx / wireActualLength;
                    double normalizedDy = dy / wireActualLength;
                    double nominalDistanceThisFrame = packet.getCurrentSpeed() * speedFactor;
                    nominalMovementVector = new Vector(normalizedDx * nominalDistanceThisFrame, 
                                                       normalizedDy * nominalDistanceThisFrame);
                }

                Point2D.Double currentPos = packet.getPosition();
                packet.setPosition(currentPos.getX() + nominalMovementVector.getX(), 
                                   currentPos.getY() + nominalMovementVector.getY());

                packet.applyDisplacementAndNoiseEffects(); 

                if (packet.getState() == PacketState.LOST) {
                    if (!networkModel.getLostPackets().contains(packet)) {
                        networkModel.addLostPacket(packet); 
                        SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);
                    }
                    continue; 
                }

                double newProgress = wire.calculateProgress(packet.getPosition());
                packet.setProgressOnWire(newProgress);
                
                if (packet.getProgressOnWire() >= 1.0) {
                    if (CollisionDetector.isPacketNearPort(packet, targetPort, wire)) {                        Point2D.Double destPortAbsPos = wire.getDestinationAbsolutePosition();
                        packet.setPosition(destPortAbsPos.x, destPortAbsPos.y);
                        packet.setProgressOnWire(1.0); 
                        packet.setState(PacketState.ARRIVED_AT_PORT); 
                        packet.setCurrentSpeed(0.0);                        
                        if (originPort != null) originPort.setInUse(false);
                        NetworkSystem destSystem = targetPort.getNetworkSystem();
                        if (destSystem != null) {
                            destSystem.processIncomingPacket(packet, targetPort, networkModel);
                        } else {
                            packet.setState(PacketState.LOST);
                            packet.setKnockedOffWire(true); 
                            if (!networkModel.getLostPackets().contains(packet)) {
                                networkModel.addLostPacket(packet);
                                SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);
                            }
                        }
                        continue; 
                    } else {
                    }
                }

                if (!CollisionDetector.isPacketStillOnWire(packet)) {
                    packet.setKnockedOffWire(true);                    packet.applyWorldFriction();                    
                    packet.setState(PacketState.LOST);
                    packet.freeOriginPort(); 
                    if (!networkModel.getLostPackets().contains(packet)) {
                        networkModel.addLostPacket(packet);
                        SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);
                    }
                    continue; 
                }

            } else if (packet.getState() != PacketState.LOST && packet.getState() != PacketState.DELIVERED) {
                 packet.applyDisplacementAndNoiseEffects();                 if (packet.getState() == PacketState.LOST) { 
                    if (!networkModel.getLostPackets().contains(packet)) {
                        networkModel.addLostPacket(packet);
                        SoundManager.playSound(SoundManager.SoundEffect.PACKET_DAMAGE);
                    }
                 }
            }
        }
        
        processCollisions(isLiveRun, simulatedTimeMillis);
        
        updateImpactWaves();
        
        if(isLiveRun) {
            currentTimeStep++;
        } else if (!isLiveRun) {
            currentTimeStep++;        }
    }

        private void processCollisions(boolean isLiveRun, long simulatedTimeMillis) {
        if (!collisionDetectionEnabled) return;
        
        long currentTimeForCheck;
        if (isLiveRun) {
            currentTimeForCheck = System.currentTimeMillis();
            if (currentTimeForCheck - lastCollisionCheckTime < COLLISION_CHECK_INTERVAL) {
                return;            }
        } else {            currentTimeForCheck = simulatedTimeMillis;
            if (lastCollisionCheckTime != 0 && (currentTimeForCheck - lastCollisionCheckTime < COLLISION_CHECK_INTERVAL)) {
                return; 
            }
        }
        lastCollisionCheckTime = currentTimeForCheck;
        
        List<Packet> movingPackets = new ArrayList<>();
        for (Packet packet : networkModel.getPackets()) {
            if (packet.getState() == PacketState.ON_WIRE && !packet.isKnockedOffWire()) {
                movingPackets.add(packet);
            }
        }
        
        if (movingPackets.size() < 2) return;
        
        List<List<Packet>> collisionPairs = CollisionDetector.detectCollisions(movingPackets);
        
        for (List<Packet> pair : collisionPairs) {
            if (pair.size() == 2) {
                CollisionEvent collision = new CollisionEvent(pair.get(0), pair.get(1));
                
                collision.processCollision();
                
                if (impactWavesEnabled) {
                    ImpactWave wave = collision.createImpactWave();
                    activeImpactWaves.add(wave);
                }
                
                System.out.println("Collision detected: " + collision);
            }
        }
    }
    
        private void updateImpactWaves() {
        if (!impactWavesEnabled) {
            activeImpactWaves.clear();
            return;
        }
        
        activeImpactWaves.removeIf(wave -> {
            wave.update();
            return !wave.isActive();
        });
        
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

        public void pauseExecution() {
        if (!snapshotsReady || !gameRunning || shopOpen) return;
        if (!gamePaused) {
            gamePaused = true;
            if (gameLoopTimer != null) {
                gameLoopTimer.stop();
            }
            System.out.println("GameModel: Execution paused at step " + currentTimeStep);
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
        }
    }

        public void resumeExecution() {
        if (!snapshotsReady || !gameRunning || shopOpen) return;
        if (gamePaused) {
            gamePaused = false;
            if (gameLoopTimer != null) {
                gameLoopTimer.start();
            }
            lastUpdateTimeNanos = System.nanoTime();            System.out.println("GameModel: Execution resumed at step " + currentTimeStep);
            if (repaintCallback != null) repaintCallback.run();
            if (updateStatsCallback != null) updateStatsCallback.run();
        }
    }

    private static class NetworkModelSnapshot {
        private final List<Packet> activePackets;
        private final List<Packet> deliveredPackets;
        private final List<Packet> lostPackets;
        private final int playerCoins;
        
        private final Map<String, List<Packet>> systemStorages;
        private final Map<String, Long> systemLastReleaseTime;

        NetworkModelSnapshot(NetworkModel modelToCopy) {
            this.activePackets = deepCopyPacketList(modelToCopy.getPackets());
            this.deliveredPackets = deepCopyPacketList(modelToCopy.getDeliveredPackets());
            this.lostPackets = deepCopyPacketList(modelToCopy.getLostPackets());
            this.playerCoins = modelToCopy.getPlayerCoins();
            
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
            Packet copy = new Packet(new Point2D.Double(original.getPosition().getX(), original.getPosition().getY()), 
                                   original.getShape(), 
                                   original.getRadius());
            copy.setState(original.getState());
            copy.setCurrentWire(original.getCurrentWire());
            copy.setTargetPort(original.getTargetPort());
            copy.setOriginPort(original.getOriginPort());
            copy.setProgressOnWire(original.getProgressOnWire());
            copy.setNetworkSystem(original.getNetworkSystem());
            copy.setNoise(original.getNoise());            copy.setKnockedOffWire(original.isKnockedOffWire());            return copy;
        }
        
        void restoreToModel(NetworkModel model) {
            model.getPackets().clear();
            model.getDeliveredPackets().clear();
            model.getLostPackets().clear();
            
            model.setPlayerCoins(this.playerCoins);
            
            for (Packet p : activePackets) {
                model.addPacketToActiveList(deepCopyPacket(p));
            }
            for (Packet p : deliveredPackets) {
                model.addDeliveredPacket(deepCopyPacket(p));
            }
            for (Packet p : lostPackets) {
                model.addLostPacket(deepCopyPacket(p));
            }
            
            for (NetworkSystem system : model.getSystems()) {
                if (system instanceof SourceNetworkSystem) {
                    SourceNetworkSystem source = (SourceNetworkSystem) system;
                    source.getSenderStorage().clear();
                    List<Packet> storedPackets = systemStorages.get(system.getId());
                    if (storedPackets != null) {
                        for (Packet p : storedPackets) {
                            Packet freshPacketCopy = deepCopyPacket(p);
                            freshPacketCopy.setNetworkSystem(source); 
                            freshPacketCopy.setState(PacketState.IN_NETWORK_SYSTEM);
                            freshPacketCopy.setPosition(new Point2D.Double(
                                source.getPosition().x + source.getWidth() / 2.0,
                                source.getPosition().y + source.getTotalHeight() / 2.0
                            ));
                            freshPacketCopy.setCurrentWire(null);
                            freshPacketCopy.setTargetPort(null);
                            freshPacketCopy.setOriginPort(null);
                            freshPacketCopy.setProgressOnWire(0.0);
                            freshPacketCopy.setKnockedOffWire(false);
                            source.getSenderStorage().offer(freshPacketCopy);
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
                
                for (Port port : system.getAllPorts()) {
                    port.setInUse(false);
                }
            }
        }
    }
}