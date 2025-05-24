package view;

import controller.NetworkController; // Will need controller for buttons
import model.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener; // For button actions
import java.awt.geom.RoundRectangle2D;

public class NetworkPanel extends JPanel {
    private GameModel gameModel;
    private NetworkController controller; // Keep a reference to the controller

    // --- Drawing State ---
    private transient Port firstPortForWire = null;
    private transient Point currentMouseForWire = null;
    private transient Color temporaryWireColor = Color.gray;

    // --- HUD Components ---
    private JLabel statsLabel;
    private JProgressBar wireUsageBar;
    private JLabel wireLimitLabel;
    private JLabel coinsLabel;
    private JSlider timeSlider;
    private JButton stepBackButton;
    private JButton stepForwardButton;
    private JButton executeButton;
    private JLabel timeStepLabel;
    private JButton addSourceSystemButton;
    private JButton addNonSourceSystemButton;
    private boolean isSliderBeingAdjusted = false;

    public NetworkPanel(GameModel model) {
        this.gameModel = model;
        this.setLayout(new BorderLayout()); // Use BorderLayout for drawing area + HUD
        this.setBackground(new Color(20, 25, 30));

        initializeHud(); // Create and add the HUD

        // --- Important for KeyListener ---
        this.setFocusable(true);
        this.requestFocusInWindow();
    }

    // Method to set the controller after both are created
    public void setController(NetworkController controller) {
        this.controller = controller;
        // Add listeners *here* now that controller exists
        this.addMouseListener(controller);
        this.addMouseMotionListener(controller);
        this.addKeyListener(controller); // Add key listener to the panel itself

        // Add action listeners for buttons
        addSourceSystemButton.addActionListener(e -> {
            controller.addSourceSystemAction();
            this.requestFocusInWindow(); // Regain focus
        });
        addNonSourceSystemButton.addActionListener(e -> {
            controller.addNonSourceSystemAction();
            this.requestFocusInWindow();
        });
        executeButton.addActionListener(e -> {
            controller.toggleExecutionAction();
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
        // --- Initialize UI components for HUD ---
        statsLabel = new JLabel("Packets: D 0 | L 0 | A 0", SwingConstants.CENTER);
        coinsLabel = new JLabel("Coins: 0", SwingConstants.CENTER);
        wireUsageBar = new JProgressBar(0, 100);
        wireUsageBar.setStringPainted(true);
        wireUsageBar.setString("Wire Usage: 0%");
        wireUsageBar.setForeground(new Color(100, 255, 100));
        wireLimitLabel = new JLabel("Limit: 1000.0", SwingConstants.LEFT);
        timeStepLabel = new JLabel("Time: 0 / " + GameModel.MAX_TIME_STEPS, SwingConstants.CENTER);

        timeSlider = new JSlider(0, GameModel.MAX_TIME_STEPS, 0);
        timeSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (source.getValueIsAdjusting()) {
                isSliderBeingAdjusted = true;
                timeStepLabel.setText("Time: " + source.getValue() + " / " + GameModel.MAX_TIME_STEPS);
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
        addSourceSystemButton = new JButton("Add Source");
        addNonSourceSystemButton = new JButton("Add Non-Source");

        // --- Enhanced HUD Panel ---
        JPanel hudPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        hudPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Row 0: Add Buttons
        JPanel addButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        addButtonsPanel.add(addSourceSystemButton);
        addButtonsPanel.add(addNonSourceSystemButton);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0,0,5,0);
        hudPanel.add(addButtonsPanel, gbc);

        // Row 1: Time Control
        JPanel timeControlPanel = new JPanel(new BorderLayout(5, 0));
        timeControlPanel.add(stepBackButton, BorderLayout.WEST);
        timeControlPanel.add(timeSlider, BorderLayout.CENTER);
        timeControlPanel.add(stepForwardButton, BorderLayout.EAST);
        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.add(timeControlPanel, BorderLayout.CENTER);
        timePanel.add(timeStepLabel, BorderLayout.SOUTH);
        gbc.gridy = 1;
        hudPanel.add(timePanel, gbc);

        // Row 2: Stats and Execute
        JPanel statsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcStats = new GridBagConstraints();
        gbcStats.gridx = 0; gbcStats.gridy = 0; gbcStats.weightx = 0.30; gbcStats.anchor = GridBagConstraints.LINE_START;
        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statsPanel.add(statsLabel, gbcStats);
        gbcStats.gridx = 1; gbcStats.weightx = 0.20; gbcStats.anchor = GridBagConstraints.CENTER;
        coinsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statsPanel.add(coinsLabel, gbcStats);
        gbcStats.gridx = 2; gbcStats.weightx = 0.20; gbcStats.anchor = GridBagConstraints.CENTER;
        statsPanel.add(executeButton, gbcStats);
        JPanel wirePanel = new JPanel(new BorderLayout(5,0));
        wireLimitLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        wirePanel.add(wireLimitLabel, BorderLayout.WEST);
        wirePanel.add(wireUsageBar, BorderLayout.CENTER);
        gbcStats.gridx = 3; gbcStats.weightx = 0.30; gbcStats.anchor = GridBagConstraints.LINE_END;
        statsPanel.add(wirePanel, gbcStats);
        gbc.gridy = 2; gbc.insets = new Insets(5,0,0,0);
        hudPanel.add(statsPanel, gbc);

        // Add the HUD to the SOUTH region of the NetworkPanel
        this.add(hudPanel, BorderLayout.SOUTH);
    }

    /**
     * Updates all HUD components based on the current GameModel state.
     */
    public void updateStatsDisplay() {
        if (gameModel == null || gameModel.getNetworkModel() == null) return;

        NetworkModel nm = gameModel.getNetworkModel();
        statsLabel.setText(String.format("Packets: D %d | L %d | A %d",
                nm.getDeliveredCount(), nm.getLostCount(), nm.getPackets().size()));
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
        timeStepLabel.setText("Time: " + currentStep + " / " + gameModel.getMaxTimeSteps());
        if (!isSliderBeingAdjusted && timeSlider.getValue() != currentStep) {
            timeSlider.setValue(currentStep);
        }

        executeButton.setText(gameModel.isGameRunning() ? "Stop" : "Execute");
    }

    // --- Drawing methods (Keep these, but they now draw in the CENTER) ---
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
        super.paintComponent(g); // This now paints the background and HUD
        Graphics2D g2d = (Graphics2D) g.create();

        // --- IMPORTANT: Translate g2d if HUD is present ---
        // We only want to draw the network in the 'center' area.
        // However, since we're overriding paintComponent for the whole panel,
        // we'll draw over the HUD unless we are careful.
        // A simpler approach: Let the default paintComponent draw the background.
        // We will draw *only* the network elements. They should appear in the center.
        // We need to ensure the HUD is drawn *on top* or handled by layout manager.
        // Since we added HUD to SOUTH, it *should* work, and paintComponent
        // here will draw *before* the HUD, but in the CENTER area if we don't
        // clear everything. Let's try drawing normally and see if layout handles it.

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Fill background *again* to ensure network area is dark
        g2d.setColor(new Color(20, 25, 30));
        g2d.fillRect(0, 0, getWidth(), getHeight()); // Fill the whole panel

        drawGrid(g2d);

        if (gameModel == null || gameModel.getNetworkModel() == null) {
            g2d.setColor(Color.RED);
            g2d.drawString("Error: GameModel or NetworkModel is null!", 50, 50);
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

        g2d.dispose();

        // We DO NOT call super.paintComponent() at the end here,
        // because the layout manager handles drawing children (like the HUD).
        // We *do* call it at the start to clear and setup.
    }

    // --- Keep all existing draw* methods (drawGrid, drawNetworkSystem, drawPort, etc.) ---
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
        java.util.List<Point> verticesList = packet.getVertices();
        if (verticesList.isEmpty()) return;
        Polygon polygon = new Polygon();
        for (Point vertex : verticesList) polygon.addPoint(vertex.x, vertex.y);
        g2d.setColor(packet.getColor());
        g2d.fillPolygon(polygon);
        g2d.setColor(packet.getColor().darker().darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawPolygon(polygon);
        g2d.setStroke(new BasicStroke(1f));
    }

}