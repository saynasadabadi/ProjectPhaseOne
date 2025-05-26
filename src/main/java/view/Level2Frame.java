package view;

import controller.NetworkController;
import model.*;
import utils.SoundManager;
import java.awt.geom.Point2D;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Level2Frame extends JFrame {
    private NetworkPanel networkPanel; // Only holds the panel
    private GameModel gameModel;
    private JFrame mainMenuFrameRef; // Store reference to MainMenuFrame

    public Level2Frame(JFrame mainMenuFrame) { // Accept MainMenuFrame reference
        this.mainMenuFrameRef = mainMenuFrame;
        this.gameModel = new GameModel(30);

        setUndecorated(true); // Make the frame undecorated (must be called before visible)

        // Create the Panel
        networkPanel = new NetworkPanel(this.gameModel);

        // --- Set Callbacks to NetworkPanel ---
        gameModel.setRepaintCallback(() -> { if (networkPanel != null) networkPanel.repaint(); });
        gameModel.setUpdateStatsCallback(() -> { if (networkPanel != null) networkPanel.updateStatsDisplay(); });

        // Setup the initial level/model
        setupInitialModel_Level2(this.gameModel.getNetworkModel());

        setTitle("Level 2 - Network System Simulator"); // Title won't be visible on undecorated frame
        setResizable(false); // Make frame not resizable
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Disable 'X' button, use in-app button
        setLayout(new BorderLayout());

        // Add the NetworkPanel - it now contains everything (drawing + HUD)
        add(networkPanel, BorderLayout.CENTER);

        // Create Controller and link it to Panel
        NetworkController controller = new NetworkController(this.gameModel, networkPanel);
        networkPanel.setController(controller); // Pass controller to panel

        // Add Back to Menu button
        JButton backToMenuButton = new JButton("Main Menu");
        backToMenuButton.addActionListener(e -> {
            returnToMainMenu();
        });
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(backToMenuButton);
        add(topPanel, BorderLayout.NORTH); // Add to the top

        setPreferredSize(new Dimension(1200, 800));
        pack();
        setLocationRelativeTo(null);

        networkPanel.updateStatsDisplay(); // Initial stats display
        networkPanel.requestFocusInWindow(); // Request focus for the panel
    }

    public void returnToMainMenu() {
        SoundManager.stopAllSounds(); // Stop any game sounds
        SoundManager.loopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC); // Start menu music
        this.dispose(); // Close this level frame
        if (this.mainMenuFrameRef != null) {
            this.mainMenuFrameRef.setVisible(true); // Show the original main menu
        }
    }

    // --- Renamed setupInitialModel for clarity ---
    private static void setupInitialModel_Level2(NetworkModel model) {
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



        System.out.println("Level2Frame: Creating SourceSystem with " + sys1Packets.size() + " initial packets");

        ArrayList<Port> s1InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE),
        new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
        ArrayList<Port> s1OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE),
        new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
        SourceNetworkSystem sys1 = new SourceNetworkSystem(
                IndicatorState.OFF, new Point(50, 50), 120, 20, 80,
                s1InPorts, s1OutPorts, sys1Packets
        );
        model.addSystem(sys1);
        
        System.out.println("Level2Frame: SourceSystem created with storage size: " + sys1.getSenderStorage().size());

        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE),
                new Port(IOType.INPUT, PacketAndPortShape.SQUARE))); // Changed shape for variety
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE),
                new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE))); // Changed shape for variety
        NonSourceNetworkSystem sys2 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(350, 150), 110, 18, 70,
                s2InPorts, s2OutPorts, 5
        );
        model.addSystem(sys2);

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
            Level2Frame frame = new Level2Frame(); // Updated constructor call
            frame.setVisible(true);
        });
    }
    */
}