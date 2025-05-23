package view;

import controller.NetworkController;
import model.*; // Assuming GameModel is in model package

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List; // Required for List

public class MainFrame extends JFrame {
    private NetworkPanel networkPanel;
    private GameModel gameModel;
    private JLabel statsLabel; // For displaying delivered/lost packets
    private JProgressBar wireUsageBar; // For displaying wire usage
    private JLabel wireLimitLabel; // For displaying wire limit info
    private JLabel coinsLabel; // Label for displaying coins

    public MainFrame() {
        this.gameModel = new GameModel();
        // Pass the repaint and stats update callbacks to GameModel
        gameModel.setRepaintCallback(() -> { if (networkPanel != null) networkPanel.repaint(); });
        gameModel.setUpdateStatsCallback(this::updateStatsDisplay);

        setupInitialModel(this.gameModel.getNetworkModel());

        setTitle("Network System Simulator - 60FPS Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        networkPanel = new NetworkPanel(this.gameModel);
        add(networkPanel, BorderLayout.CENTER);

        NetworkController controller = new NetworkController(this.gameModel, networkPanel);
        networkPanel.addMouseListener(controller);
        networkPanel.addMouseMotionListener(controller);

        JButton addSourceSystemButton = new JButton("Add Source System");
        addSourceSystemButton.addActionListener(e -> {
            NetworkModel currentNetworkModel = gameModel.getNetworkModel();
            if (currentNetworkModel != null) {
                Point newPos = new Point(50 + (currentNetworkModel.getSystems().size() % 6) * 150,
                        50 + (currentNetworkModel.getSystems().size() / 6) * 150);
                ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
                ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));

                // Create some initial packets for the new source system
                ArrayList<Packet> initialPackets = new ArrayList<>();
                initialPackets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 8)); // Position will be updated
                initialPackets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 10));

                SourceNetworkSystem newSys = new SourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, initialPackets);
                controller.addNetworkSystem(newSys);
                updateStatsDisplay(); // Update wire stats when system is added
            }
        });

        JButton addNonSourceSystemButton = new JButton("Add Non-Source System");
        addNonSourceSystemButton.addActionListener(e -> {
            NetworkModel currentNetworkModel = gameModel.getNetworkModel();
            if (currentNetworkModel != null) {
                Point newPos = new Point(70 + (currentNetworkModel.getSystems().size() % 6) * 150,
                        70 + (currentNetworkModel.getSystems().size() / 6) * 150);
                ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
                ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
                NonSourceNetworkSystem newSys = new NonSourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, 5);
                controller.addNetworkSystem(newSys);
                updateStatsDisplay(); // Update wire stats when system is added
            }
        });

        JButton startGameButton = new JButton("Start Game");
        startGameButton.addActionListener(e -> {
            if (!gameModel.isGameRunning()) {
                boolean gameStarted = gameModel.startTheGame();
                if (gameStarted) {
                    startGameButton.setText("Stop Game");
                } // If gameStarted is false, button text remains "Start Game"
            } else {
                gameModel.stopTheGame();
                startGameButton.setText("Start Game");
            }
            updateStatsDisplay(); // Update stats immediately on start/stop
        });

        // Initialize UI components for HUD
        statsLabel = new JLabel("Packets: D 0 | L 0 | A 0", SwingConstants.CENTER);
        coinsLabel = new JLabel("Coins: 0", SwingConstants.CENTER); // Initialize coins label
        wireUsageBar = new JProgressBar(0, 100);
        wireUsageBar.setStringPainted(true);
        wireUsageBar.setString("Wire Usage: 0%");
        wireUsageBar.setForeground(new Color(100, 255, 100));
        wireLimitLabel = new JLabel("Limit: 1000.0", SwingConstants.LEFT);

        // --- Enhanced HUD Panel --- 
        JPanel hudPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        hudPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Padding

        // Buttons Panel (top part of HUD)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(addSourceSystemButton);
        buttonPanel.add(addNonSourceSystemButton);
        buttonPanel.add(startGameButton);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        hudPanel.add(buttonPanel, gbc);

        // Stats Label
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.33; gbc.insets = new Insets(5,0,0,5);
        gbc.anchor = GridBagConstraints.LINE_START;
        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        hudPanel.add(statsLabel, gbc);

        // Coins Label
        gbc.gridx = 1; gbc.weightx = 0.33; gbc.insets = new Insets(5,5,0,5);
        gbc.anchor = GridBagConstraints.CENTER;
        coinsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        hudPanel.add(coinsLabel, gbc);
        
        // Wire Limit Label (Part of Wire Usage Display)
        JPanel wirePanel = new JPanel(new BorderLayout(5,0));
        wireLimitLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        wirePanel.add(wireLimitLabel, BorderLayout.WEST);
        wirePanel.add(wireUsageBar, BorderLayout.CENTER);

        gbc.gridx = 2; gbc.weightx = 0.34; gbc.insets = new Insets(5,5,0,0);
        gbc.anchor = GridBagConstraints.LINE_END;
        hudPanel.add(wirePanel, gbc);
        
        add(hudPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1200, 800)); // Increased size a bit
        pack();
        setLocationRelativeTo(null);
        updateStatsDisplay(); // Initial stats display
    }

    private void updateStatsDisplay() {
        if (gameModel != null && gameModel.getNetworkModel() != null && statsLabel != null) {
            NetworkModel nm = gameModel.getNetworkModel();
            statsLabel.setText(String.format("Packets: D %d | L %d | A %d", 
                                            nm.getDeliveredCount(), nm.getLostCount(), nm.getPackets().size()));
            coinsLabel.setText("Coins: " + nm.getPlayerCoins()); // Update coins display
            
            if (wireUsageBar != null && wireLimitLabel != null) {
                double actualCommittedWireLength = nm.getCurrentWireLength();
                double temporaryWireLength = gameModel.getTemporaryWireLength();
                double totalLengthToDisplay = actualCommittedWireLength + temporaryWireLength;
                double wireLengthLimit = nm.getWireLengthLimit();
                
                double usagePercentage = 0.0;
                if (wireLengthLimit > 0) { // Avoid division by zero
                    usagePercentage = (totalLengthToDisplay / wireLengthLimit) * 100.0;
                }
                // Clamp percentage between 0 and 100 for the progress bar display
                usagePercentage = Math.max(0, Math.min(100, usagePercentage));
                
                wireUsageBar.setValue((int) Math.round(usagePercentage));
                wireUsageBar.setString(String.format("Wire Usage: %.1f%% (%.0f/%.0f)", 
                    (totalLengthToDisplay / wireLengthLimit) * 100.0, // Show true percentage in text, even if > 100%
                    totalLengthToDisplay, wireLengthLimit));
                    
                // Change color based on usage
                if (usagePercentage >= 90) {
                    wireUsageBar.setForeground(new Color(255, 100, 100)); // Red
                } else if (usagePercentage >= 75) {
                    wireUsageBar.setForeground(new Color(255, 200, 100)); // Orange
                } else {
                    wireUsageBar.setForeground(new Color(100, 255, 100)); // Green
                }
                
                wireLimitLabel.setText(String.format("Limit: %.0f", wireLengthLimit));
            }
        } else if (statsLabel != null) {
            statsLabel.setText("Packets: D 0 | L 0 | A 0");
            if (coinsLabel != null) coinsLabel.setText("Coins: 0"); // Reset coins display
            if (wireUsageBar != null) {
                wireUsageBar.setValue(0);
                wireUsageBar.setString("Wire Usage: 0%");
                wireUsageBar.setForeground(new Color(100, 255, 100));
            }
            if (wireLimitLabel != null) {
                wireLimitLabel.setText("Limit: 1000.0");
            }
        }
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    private static void setupInitialModel(NetworkModel model) {
        if (model == null) return;

        // Initial packets for sys1
        ArrayList<Packet> sys1Packets = new ArrayList<>();
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 10)); // Position will be set by SourceNetworkSystem

        ArrayList<Port> s1InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
        ArrayList<Port> s1OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        SourceNetworkSystem sys1 = new SourceNetworkSystem(
                IndicatorState.OFF, new Point(50, 50), 120, 20, 80,
                s1InPorts, s1OutPorts, sys1Packets
        );
        model.addSystem(sys1);

        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        NonSourceNetworkSystem sys2 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(350, 100), 110, 18, 70, // Adjusted position for clarity
                s2InPorts, s2OutPorts, 5 // Storage limit of 5
        );
        model.addSystem(sys2);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}