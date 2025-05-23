package view;

import controller.NetworkController;
import model.*; // Assuming GameModel is in model package

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List; // Required for List

public class MainFrame extends JFrame {
    private NetworkPanel networkPanel;
    private GameModel gameModel;
    private JLabel statsLabel; // For displaying delivered/lost packets

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
                initialPackets.add(new Packet(new Point(0,0), PacketAndPortShape.TRIANGLE, 10));


                SourceNetworkSystem newSys = new SourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, initialPackets);
                controller.addNetworkSystem(newSys);
            }
        });

        JButton addNonSourceSystemButton = new JButton("Add Non-Source System");
        addNonSourceSystemButton.addActionListener(e -> {
            NetworkModel currentNetworkModel = gameModel.getNetworkModel();
            if (currentNetworkModel != null) {
                Point newPos = new Point(70 + (currentNetworkModel.getSystems().size() % 6) * 150,
                        70 + (currentNetworkModel.getSystems().size() / 6) * 150);
                ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)));
                ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)));
                NonSourceNetworkSystem newSys = new NonSourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, 5);
                controller.addNetworkSystem(newSys);
            }
        });


        JButton startGameButton = new JButton("Start Game");
        startGameButton.addActionListener(e -> {
            if (!gameModel.isGameRunning()) {
                gameModel.startTheGame();
                startGameButton.setText("Stop Game");
            } else {
                gameModel.stopTheGame();
                startGameButton.setText("Start Game");
            }
            updateStatsDisplay(); // Update stats immediately on start/stop
        });

        statsLabel = new JLabel("Delivered: 0 | Lost: 0");
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Added some spacing
        controlPanel.add(addSourceSystemButton);
        controlPanel.add(addNonSourceSystemButton);
        controlPanel.add(startGameButton);
        controlPanel.add(statsLabel); // Add stats label to control panel

        add(controlPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1200, 800)); // Increased size a bit
        pack();
        setLocationRelativeTo(null);
        updateStatsDisplay(); // Initial stats display
    }

    private void updateStatsDisplay() {
        if (gameModel != null && gameModel.getNetworkModel() != null && statsLabel != null) {
            NetworkModel nm = gameModel.getNetworkModel();
            statsLabel.setText("Delivered: " + nm.getDeliveredCount() + " | Lost: " + nm.getLostCount() +
                    " | Active: " + nm.getPackets().size());
        } else if (statsLabel != null) {
            statsLabel.setText("Delivered: 0 | Lost: 0 | Active: 0");
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


        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)));
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)
        ));
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