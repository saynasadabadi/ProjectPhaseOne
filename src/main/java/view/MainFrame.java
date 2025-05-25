package view;

import controller.NetworkController;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private NetworkPanel networkPanel; // Only holds the panel
    private GameModel gameModel;

    public MainFrame() {
        this.gameModel = new GameModel();

        // Create the Panel
        networkPanel = new NetworkPanel(this.gameModel);

        // --- Set Callbacks to NetworkPanel ---
        gameModel.setRepaintCallback(() -> { if (networkPanel != null) networkPanel.repaint(); });
        gameModel.setUpdateStatsCallback(() -> { if (networkPanel != null) networkPanel.updateStatsDisplay(); });

        // Setup the initial level/model
        setupInitialModel(this.gameModel.getNetworkModel());

        setTitle("Network System Simulator V2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Add the NetworkPanel - it now contains everything (drawing + HUD)
        add(networkPanel, BorderLayout.CENTER);

        // Create Controller and link it to Panel
        NetworkController controller = new NetworkController(this.gameModel, networkPanel);
        networkPanel.setController(controller); // Pass controller to panel

        setPreferredSize(new Dimension(1200, 800));
        pack();
        setLocationRelativeTo(null);

        networkPanel.updateStatsDisplay(); // Initial stats display
        networkPanel.requestFocusInWindow(); // Request focus for the panel
    }

    // --- Keep setupInitialModel ---
    private static void setupInitialModel(NetworkModel model) {
        if (model == null) return;

        // Create custom packets for sys1
        ArrayList<Packet> sys1Packets = new ArrayList<>();
//        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 10));
        // // Add more packets if you want
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.TRIANGLE, 8));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.TRIANGLE, 8));

        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point(0,0), PacketAndPortShape.TRIANGLE, 8));



        System.out.println("MainFrame: Creating SourceSystem with " + sys1Packets.size() + " initial packets");

        ArrayList<Port> s1InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE),
        new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
        ArrayList<Port> s1OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE),
        new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        SourceNetworkSystem sys1 = new SourceNetworkSystem(
                IndicatorState.OFF, new Point(50, 50), 120, 20, 80,
                s1InPorts, s1OutPorts, sys1Packets
        );
        model.addSystem(sys1);
        
        System.out.println("MainFrame: SourceSystem created with storage size: " + sys1.getSenderStorage().size());

        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE),
                new Port(IOType.INPUT, PacketAndPortShape.SQUARE))); // Changed shape for variety
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE),
                new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE))); // Changed shape for variety
        NonSourceNetworkSystem sys2 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(350, 100), 110, 18, 70,
                s2InPorts, s2OutPorts, 5
        );
        model.addSystem(sys2);

    }

    // --- Keep main method ---
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