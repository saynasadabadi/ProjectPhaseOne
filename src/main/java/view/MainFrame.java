package view;

import controller.NetworkController;
import model.*; // Assuming GameModel is in model package

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private NetworkPanel networkPanel;
    private GameModel gameModel; // Changed from NetworkModel to GameModel

    public MainFrame() {
        this.gameModel = new GameModel(); // Instantiate GameModel
        // The NetworkModel is now created inside GameModel's constructor
        setupInitialModel(this.gameModel.getNetworkModel()); // Pass the NetworkModel from GameModel

        setTitle("Network System Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        networkPanel = new NetworkPanel(this.gameModel); // Pass GameModel to NetworkPanel
        add(networkPanel, BorderLayout.CENTER);

        // Pass GameModel to NetworkController
        NetworkController controller = new NetworkController(this.gameModel, networkPanel);
        networkPanel.addMouseListener(controller);
        networkPanel.addMouseMotionListener(controller);


        JButton addSystemButton = new JButton("Add Test System");
        addSystemButton.addActionListener(e -> {
            // Access NetworkModel through GameModel
            NetworkModel currentNetworkModel = gameModel.getNetworkModel();
            if (currentNetworkModel != null) {
                Point newPos = new Point(50 + (currentNetworkModel.getSystems().size() % 5) * 180,
                        350 + (currentNetworkModel.getSystems().size() / 5) * 150);
                ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)));
                ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)));
                NonSourceNetworkSystem newSys = new NonSourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, 5);
                // The controller's addNetworkSystem method now uses the GameModel to get to the NetworkModel
                controller.addNetworkSystem(newSys);
            }
        });

        // New "Start the Game" button
        JButton startGameButton = new JButton("Start the Game");
        startGameButton.addActionListener(e -> {
            gameModel.startTheGame(); // Call the method on GameModel
            // You might want to update the UI or give feedback based on the result
            // For example, disable the button or change its text if the game starts.
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(addSystemButton);
        controlPanel.add(startGameButton); // Add the new button to the control panel
        add(controlPanel, BorderLayout.SOUTH);


        setPreferredSize(new Dimension(1000, 750));
        pack();
        setLocationRelativeTo(null);
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    // setupInitialModel now takes NetworkModel directly as GameModel handles its creation
    private static void setupInitialModel(NetworkModel model) {
        if (model == null) return; // Guard against null model

        ArrayList<Port> s1InPorts = new ArrayList<>(List.of(
                new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)) // Source might not need input ports for this game logic
        );
        ArrayList<Port> s1OutPorts = new ArrayList<>(List.of(
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)
        ));
        // For the game to be startable, initial indicators should be ON or systems need to be "powered"
        SourceNetworkSystem sys1 = new SourceNetworkSystem(
                IndicatorState.OFF, new Point(50, 50), 120, 20, 80, // Start as OFF, user must turn ON
                s1InPorts, s1OutPorts,
                new ArrayList<>()
        );
        model.addSystem(sys1);


        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(
                new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)
        ));
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)
        ));
        NonSourceNetworkSystem sys2 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(300, 100), 110, 18, 70, // Start as OFF
                s2InPorts, s2OutPorts, 10
        );
        model.addSystem(sys2);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}