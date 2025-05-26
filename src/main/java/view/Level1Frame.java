package view;

import controller.NetworkController;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Level1Frame extends JFrame {
    private NetworkPanel networkPanel; // Only holds the panel
    private GameModel gameModel;

    public Level1Frame() {
        this.gameModel = new GameModel(60); // Level 1 uses a 60s GameModel

        // Create the Panel
        networkPanel = new NetworkPanel(this.gameModel);

        // --- Set Callbacks to NetworkPanel ---
        gameModel.setRepaintCallback(() -> { if (networkPanel != null) networkPanel.repaint(); });
        gameModel.setUpdateStatsCallback(() -> { if (networkPanel != null) networkPanel.updateStatsDisplay(); });

        // Setup the initial level/model
        setupInitialModel_Level1(this.gameModel.getNetworkModel());

        setTitle("Level 1 - Network System Simulator");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose instead of exit
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

    // --- Renamed setupInitialModel for clarity ---
    private static void setupInitialModel_Level1(NetworkModel model) {
        if (model == null) return;

        // Create custom packets for sys1
        ArrayList<Packet> sys1Packets = new ArrayList<>();
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.TRIANGLE, 8));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.TRIANGLE, 8));

        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.SQUARE, 12));
        sys1Packets.add(new Packet(new Point2D.Double(0,0), PacketAndPortShape.TRIANGLE, 8));

        System.out.println("Level1Frame: Creating SourceSystem with " + sys1Packets.size() + " initial packets");

        ArrayList<Port> s1InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)
        ));
        ArrayList<Port> s1OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        SourceNetworkSystem sys1 = new SourceNetworkSystem(
                IndicatorState.OFF, new Point(50, 50), 120, 20, 80,
                s1InPorts, s1OutPorts, sys1Packets
        );
        model.addSystem(sys1);
        
        System.out.println("Level1Frame: SourceSystem created with storage size: " + sys1.getSenderStorage().size());

        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        NonSourceNetworkSystem sys2 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(350, 50), 110, 18, 70,
                s2InPorts, s2OutPorts, 5
        );
        model.addSystem(sys2);

        ArrayList<Port> s3InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
        ArrayList<Port> s3OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        NonSourceNetworkSystem sys3 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(350, 150), 110, 18, 70,
                s3InPorts, s3OutPorts, 5
        );
        model.addSystem(sys3);

    }

    // --- main method removed, new entry point is MainMenuFrame.java ---
    /*
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Level1Frame frame = new Level1Frame(); // Updated constructor call
            frame.setVisible(true);
        });
    }
    */
}