package view;

import controller.NetworkController;import model.*;
import utils.SoundManager;import java.awt.geom.Point2D;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;import java.awt.geom.RoundRectangle2D;

public class NetworkPanel extends JPanel {
    private GameModel gameModel;
    private NetworkController controller;
    private transient Port firstPortForWire = null;
    private transient Point currentMouseForWire = null;
    private transient Color temporaryWireColor = Color.gray;

    private JLabel statsLabel;
    private JProgressBar wireUsageBar;
    private JLabel wireLimitLabel;
    private JLabel coinsLabel;
    private JSlider timeSlider;
    private JButton stepBackButton;
    private JButton stepForwardButton;
    private JButton executeButton;
    private JButton tryAgainButton;
    private JLabel timeStepLabel;
    private JLabel levelDurationInfoLabel;
    private boolean isSliderBeingAdjusted = false;
    

    public NetworkPanel(GameModel model) {
        this.gameModel = model;
        this.setLayout(new BorderLayout());        this.setBackground(new Color(20, 25, 30));

        initializeHud();
        this.setFocusable(true);
        this.requestFocusInWindow();
    }

    public void setController(NetworkController controller) {
        this.controller = controller;
        this.addMouseListener(controller);
        this.addMouseMotionListener(controller);
        this.addKeyListener(controller);
        executeButton.addActionListener(e -> {
            controller.toggleExecutionAction();
            this.requestFocusInWindow();
        });
        tryAgainButton.addActionListener(e -> {
            controller.restartLevel();
            this.requestFocusInWindow();
        });
        stepBackButton.addActionListener(e -> {
            gameModel.timeStepBackward();
            updateStatsDisplay();
            this.requestFocusInWindow();
        });
        stepForwardButton.addActionListener(e -> {
            gameModel.timeStepForward();
            updateStatsDisplay();
            this.requestFocusInWindow();
        });
    }


    private void initializeHud() {
        statsLabel = new JLabel("Packets: D 0 | L 0 | A 0", SwingConstants.CENTER);
        coinsLabel = new JLabel("Coins: 0", SwingConstants.CENTER);
        wireUsageBar = new JProgressBar(0, 100);
        wireUsageBar.setStringPainted(true);
        wireUsageBar.setString("Wire Usage: 0%");
        wireUsageBar.setForeground(new Color(100, 255, 100));
        wireLimitLabel = new JLabel("Limit: 1000.0", SwingConstants.LEFT);
        timeStepLabel = new JLabel("Time: 0.0s / 0.0s", SwingConstants.CENTER);
        levelDurationInfoLabel = new JLabel("Level Duration: 0.0s");
        levelDurationInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        levelDurationInfoLabel.setForeground(Color.black);

        timeSlider = new JSlider(0, 100, 0);        timeSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (source.getValueIsAdjusting()) {
                isSliderBeingAdjusted = true;
                double currentTime = (double) source.getValue() / gameModel.getMaxTimeSteps() * gameModel.getTimeLimitSeconds();
                timeStepLabel.setText(String.format("Time: %.2fs / %.1fs", currentTime, gameModel.getTimeLimitSeconds()));
            } else {
                if (isSliderBeingAdjusted) {
                    int newStep = source.getValue();
                    if (newStep != gameModel.getCurrentTimeStep()) {
                        gameModel.goToTimeStep(newStep);
                    }
                }
                isSliderBeingAdjusted = false;
                updateStatsDisplay();
            }
        });

        stepBackButton = new JButton("<");
        stepForwardButton = new JButton(">");
        executeButton = new JButton("Execute");
        tryAgainButton = new JButton("Try Again");
        tryAgainButton.setVisible(false);
        
        stepBackButton.setVisible(false);
        stepForwardButton.setVisible(false);
        timeSlider.setVisible(false);
        timeStepLabel.setVisible(false);

        JPanel hudPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        hudPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0,0,5,0);
        hudPanel.add(levelDurationInfoLabel, gbc);

        JPanel timeControlPanel = new JPanel(new BorderLayout(5, 0));
        timeControlPanel.add(stepBackButton, BorderLayout.WEST);
        timeControlPanel.add(timeSlider, BorderLayout.CENTER);
        timeControlPanel.add(stepForwardButton, BorderLayout.EAST);

        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.add(timeStepLabel, BorderLayout.NORTH);
        timePanel.add(timeControlPanel, BorderLayout.CENTER);

        gbc.gridy = 2;
        hudPanel.add(timePanel, gbc);

        JPanel statsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcStats = new GridBagConstraints();
        gbcStats.gridx = 0; gbcStats.gridy = 0; gbcStats.weightx = 0.30; gbcStats.anchor = GridBagConstraints.LINE_START;
        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statsPanel.add(statsLabel, gbcStats);
        gbcStats.gridx = 1; gbcStats.weightx = 0.20; gbcStats.anchor = GridBagConstraints.CENTER;
        coinsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statsPanel.add(coinsLabel, gbcStats);
        gbcStats.gridx = 2; gbcStats.weightx = 0.20; gbcStats.anchor = GridBagConstraints.CENTER;
        
        JPanel executePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        executePanel.add(executeButton);
        executePanel.add(tryAgainButton);
        statsPanel.add(executePanel, gbcStats);
        
        JPanel wirePanel = new JPanel(new BorderLayout(5,0));
        wireLimitLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        wirePanel.add(wireLimitLabel, BorderLayout.WEST);
        wirePanel.add(wireUsageBar, BorderLayout.CENTER);
        gbcStats.gridx = 3; gbcStats.weightx = 0.30; gbcStats.anchor = GridBagConstraints.LINE_END;
        statsPanel.add(wirePanel, gbcStats);
        gbc.gridy = 3; gbc.insets = new Insets(5,0,0,0);
        hudPanel.add(statsPanel, gbc);

        this.add(hudPanel, BorderLayout.SOUTH);
    }

        public void updateStatsDisplay() {
        if (gameModel == null || gameModel.getNetworkModel() == null) return;

        NetworkModel nm = gameModel.getNetworkModel();
        
        int notReleasedCount = 0;
        for (NetworkSystem system : nm.getSystems()) {
            if (system instanceof SourceNetworkSystem) {
                notReleasedCount += ((SourceNetworkSystem) system).getSenderStorage().size();
            }
        }
        int activeCount = nm.getPackets().size();
        int deliveredCount = nm.getDeliveredCount();
        int lostCount = nm.getLostCount();
        int totalPackets = nm.getPackets().size() + notReleasedCount;

        double lossPercentage = gameModel.getPacketLossPercentage();
        String lossText = String.format("%.1f%%", lossPercentage);
        
        if (lossPercentage > 40.0) {
            statsLabel.setForeground(Color.RED);
        } else if (lossPercentage > 25.0) {
            statsLabel.setForeground(Color.ORANGE);
        } else {
            statsLabel.setForeground(Color.RED);
        }
        
        statsLabel.setText(String.format("Packets: Total %d | Pending %d | Active %d | Delivered %d | Lost %d (%s)",
                totalPackets, notReleasedCount, activeCount, deliveredCount, lostCount, lossText));
        
        coinsLabel.setText("Coins: " + nm.getPlayerCoins());

        double actualCommittedWireLength = nm.getCurrentWireLength();
        double temporaryWireLength = gameModel.getTemporaryWireLength();
        double totalLengthToDisplay = actualCommittedWireLength + temporaryWireLength;
        double wireLengthLimit = nm.getWireLengthLimit();
        double usagePercentage = (wireLengthLimit > 0) ? (totalLengthToDisplay / wireLengthLimit) * 100.0 : 0.0;
        int barValue = (int) Math.round(Math.max(0, Math.min(100, usagePercentage)));
        wireUsageBar.setValue(barValue);
        wireUsageBar.setString(String.format("Wire: %.0f / %.0f", totalLengthToDisplay, wireLengthLimit));
        if (usagePercentage >= 90) wireUsageBar.setForeground(new Color(255, 100, 100));
        else if (usagePercentage >= 75) wireUsageBar.setForeground(new Color(255, 200, 100));
        else wireUsageBar.setForeground(new Color(100, 255, 100));
        wireLimitLabel.setText(String.format("Limit: %.0f", wireLengthLimit));

        int currentStep = gameModel.getCurrentTimeStep();
        int maxSteps = gameModel.getMaxTimeSteps();
        double currentTime = gameModel.getCurrentTimeSeconds();
        double maxTime = gameModel.getTimeLimitSeconds();
        
        if (timeSlider.getMaximum() != maxSteps) {
            timeSlider.setMaximum(maxSteps);
        }
        
        timeStepLabel.setText(String.format("Elapsed: %.3fs / Total: %.1fs", currentTime, maxTime));
        timeStepLabel.setForeground(Color.WHITE);

        if (gameModel.isGameOverTriggered() && gameModel.hasJustGotGameOver()) {
            gameModel.acknowledgeGameOver(); 
            SwingUtilities.invokeLater(() -> showGameOverDialog(lossPercentage, nm));
        }
        
        if (!isSliderBeingAdjusted && timeSlider.getValue() != currentStep) {
            timeSlider.setValue(currentStep);
        }

        if (gameModel.isExecutingSnapshots()) {
            executeButton.setText("Executing...");
            executeButton.setEnabled(false);
        } else if (!gameModel.isGameRunning()) {
            executeButton.setText("Execute");
            executeButton.setEnabled(true);
        } else if (gameModel.isGamePaused()) {
            executeButton.setText("Resume");
            executeButton.setEnabled(true);
        } else {
            executeButton.setText("Pause");
            executeButton.setEnabled(true);
        }
        
        tryAgainButton.setVisible(gameModel.isGameRunning());
        
        stepBackButton.setEnabled(!gameModel.isGameExecuting());
        stepForwardButton.setEnabled(!gameModel.isGameExecuting());
        timeSlider.setEnabled(!gameModel.isGameExecuting());
        
        stepBackButton.setVisible(gameModel.isGameRunning());
        stepForwardButton.setVisible(gameModel.isGameRunning());
        timeSlider.setVisible(gameModel.isGameRunning());
        timeStepLabel.setVisible(gameModel.isGameRunning());

        levelDurationInfoLabel.setText(String.format("Level Duration: %.1fs", gameModel.getTimeLimitSeconds()));
    }

    public void setFirstPortForWire(Port port) {
        if (this.firstPortForWire != null) {
            this.firstPortForWire.setSelectedForConnection(false);
        }
        this.firstPortForWire = port;
        if (port != null) {
            port.setSelectedForConnection(true);
        }
        repaint();
    }

    public Port getFirstPortForWire() { return this.firstPortForWire; }
    public void setCurrentMouseForWire(Point mousePoint) { this.currentMouseForWire = mousePoint; }
    public void setTemporaryWireColor(Color color) {
        this.temporaryWireColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);        Graphics2D g2d = (Graphics2D) g.create();


        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.setColor(new Color(20, 25, 30));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        drawGrid(g2d);

        if (gameModel == null || gameModel.getNetworkModel() == null) {
            g2d.setColor(Color.RED);
            g2d.drawString("Error: GameModel or NetworkModel is null!", 50, 50);
            g2d.dispose();
            return;
        }

        if (gameModel.isExecutingSnapshots()) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            String loadingText = "Executing the level...";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(loadingText);
            int textHeight = fm.getHeight();
            int x = (getWidth() - textWidth) / 2;
            int y = (getHeight() - textHeight) / 2 + fm.getAscent();
            g2d.drawString(loadingText, x, y);
            g2d.dispose();
            return;
        }

        NetworkModel networkModel = gameModel.getNetworkModel();

        for (NetworkSystem system : networkModel.getSystems()) {
            drawNetworkSystem(g2d, system);
        }
        for (Wire wire : networkModel.getWires()) {
            drawWire(g2d, wire);
        }
        if (firstPortForWire != null && currentMouseForWire != null && firstPortForWire.getAbsolutePosition() != null) {
            drawTemporaryWire(g2d, firstPortForWire.getAbsolutePosition(), currentMouseForWire, temporaryWireColor);
        }
        for (model.Packet packet : networkModel.getPackets()) {
            if (packet.getState() == PacketState.ON_WIRE || packet.getState() == PacketState.IN_NETWORK_SYSTEM) {
                drawPacket(g2d, packet);
            }
        }
        
        for (ImpactWave wave : gameModel.getActiveImpactWaves()) {
            drawImpactWave(g2d, wave);
        }

        g2d.dispose();

    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(40, 45, 50));
        int gridSize = 25;
        for (int x = 0; x < getWidth(); x += gridSize) g2d.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += gridSize) g2d.drawLine(0, y, getWidth(), y);
    }
    private void drawNetworkSystem(Graphics2D g2d, NetworkSystem system) {
        Point sysPos = system.getPosition();
        int width = system.getWidth();
        int bodyH = system.getBodyHeight();
        int indH = system.getIndicatorHeight();
        float arc = 15.0f;
        Color bodyFillColor = (system instanceof SourceNetworkSystem) ? new Color(60, 80, 60) : new Color(70, 75, 80);
        Color bodyBorderColor = bodyFillColor.darker();
        Color indicatorFillColor = system.getIndicatorState().getColor();
        g2d.setColor(bodyFillColor);
        g2d.fill(new RoundRectangle2D.Float(sysPos.x, sysPos.y + indH, width, bodyH, arc, arc));
        g2d.setColor(bodyBorderColor);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new RoundRectangle2D.Float(sysPos.x, sysPos.y + indH, width, bodyH, arc, arc));
        g2d.setColor(indicatorFillColor);
        g2d.fill(new RoundRectangle2D.Float(sysPos.x, sysPos.y, width, indH, arc / 2f, arc / 2f));
        g2d.setColor(indicatorFillColor.darker());
        g2d.draw(new RoundRectangle2D.Float(sysPos.x, sysPos.y, width, indH, arc / 2f, arc / 2f));
        g2d.setStroke(new BasicStroke(1f));
        for (Port port : system.getAllPorts()) {
            if (port.getRelativePosition() != null) drawPort(g2d, port);
        }
    }
    private void drawPort(Graphics2D g2d, Port port) {
        Point absPortPos = port.getAbsolutePosition();
        if (absPortPos == null) return;
        int s = NetworkSystem.PORT_VISUAL_SIZE;
        PacketAndPortShape shapeType = port.getShape();
        g2d.setColor(shapeType.getColor());
        Polygon portPolygon = new Polygon();
        if (shapeType == PacketAndPortShape.SQUARE) {
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y + s / 2);
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y + s / 2);
        } else if (shapeType == PacketAndPortShape.TRIANGLE) {
            if (port.getIoType() == IOType.OUTPUT) {
                portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y - s / 2);
                portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y);
                portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y + s / 2);
            } else {
                portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y - s / 2);
                portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y);
                portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y + s / 2);
            }
        } else { g2d.fillOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s); }
        if (portPolygon.npoints > 0) { g2d.fillPolygon(portPolygon); g2d.setColor(shapeType.getColor().darker()); g2d.drawPolygon(portPolygon); }
        if (port.isSelectedForConnection()) g2d.setColor(Color.YELLOW);
        else if (port.isInUse()) g2d.setColor(Color.ORANGE.darker());
        if (port.isSelectedForConnection() || port.isInUse()) {
            g2d.setStroke(new BasicStroke(2f));
            if (portPolygon.npoints > 0) g2d.drawPolygon(portPolygon); else g2d.drawOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
            g2d.setStroke(new BasicStroke(1f));
        }
    }
    private void drawWire(Graphics2D g2d, Wire wire) {
        Port source = wire.getSourcePort(); Port dest = wire.getDestinationPort();
        if (source == null || dest == null || source.getAbsolutePosition() == null || dest.getAbsolutePosition() == null) return;
        Point p1 = source.getAbsolutePosition(); Point p2 = dest.getAbsolutePosition();
        g2d.setColor(wire.getColor());
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        g2d.setStroke(new BasicStroke(1f));
    }
    private void drawTemporaryWire(Graphics2D g2d, Point start, Point end, Color color) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f, 5.0f}, 0.0f));
        g2d.drawLine(start.x, start.y, end.x, end.y);
        g2d.setStroke(new BasicStroke(1f));
    }
    private void drawPacket(Graphics2D g2d, model.Packet packet) {
        if (packet.getPosition() == null || packet.getShape() == null) return;
        java.util.List<Point2D.Double> verticesList = packet.getVertices();
        if (verticesList.isEmpty()) return;
        
        Polygon polygon = new Polygon();
        for (Point2D.Double vertex : verticesList) {
            polygon.addPoint((int)Math.round(vertex.getX()), (int)Math.round(vertex.getY()));
        }
        
        g2d.setColor(packet.getColor());
        g2d.fillPolygon(polygon);
        g2d.setColor(packet.getColor().darker().darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawPolygon(polygon);
        g2d.setStroke(new BasicStroke(1f));
    }
    
    private void drawImpactWave(Graphics2D g2d, ImpactWave wave) {
        if (!wave.isActive()) return;
        
        Point2D.Double origin = wave.getOrigin();
        double radius = wave.getCurrentRadius();
        
        double fadeRatio = Math.max(0.0, 1.0 - (radius / wave.getMaxRadius()));
        int alpha = (int) (255 * fadeRatio * 0.6);        
        if (alpha <= 0) return;
        
        Color waveColor = new Color(255, 255, 100, alpha);        g2d.setColor(waveColor);
        
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int waveRadius = (int) radius;
        g2d.drawOval((int)Math.round(origin.getX()) - waveRadius, 
                     (int)Math.round(origin.getY()) - waveRadius, 
                     waveRadius * 2, waveRadius * 2);
        
        int innerAlpha = (int) (alpha * 0.5);
        if (innerAlpha > 0 && radius > 10) {
            Color innerWaveColor = new Color(255, 200, 50, innerAlpha);
            g2d.setColor(innerWaveColor);
            int innerRadius = (int) (radius * 0.7);
            g2d.drawOval((int)Math.round(origin.getX()) - innerRadius, 
                         (int)Math.round(origin.getY()) - innerRadius, 
                         innerRadius * 2, innerRadius * 2);
        }
        
        g2d.setStroke(new BasicStroke(1f));    }

        private void showGameOverDialog(double lossPercentage, NetworkModel nm) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        
        SoundManager.playSound(SoundManager.SoundEffect.GAME_OVER);
        GameOverDialog dialog = new GameOverDialog(
            parentFrame, 
            lossPercentage, 
            nm.getDeliveredCount(), 
            nm.getLostCount(), 
            nm.getPlayerCoins(),
            this.controller        );
        
        dialog.showDialog();        
        if (dialog.isReturnToMenuClicked()) {
            if (parentFrame instanceof Level1Frame) {
                ((Level1Frame) parentFrame).returnToMainMenu();
            } else if (parentFrame instanceof Level2Frame) {
                ((Level2Frame) parentFrame).returnToMainMenu();
            }
        }
    }

}