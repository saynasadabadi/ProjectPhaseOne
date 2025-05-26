package view;

import controller.NetworkController;
import model.*;
import utils.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Level1Frame extends JFrame {
    private NetworkPanel networkPanel;
    private GameModel gameModel;
    private JFrame mainMenuFrameRef;
    private JButton shopButton;

    public Level1Frame(JFrame mainMenuFrame) {
        this.mainMenuFrameRef = mainMenuFrame;
        this.gameModel = new GameModel(60);

        setUndecorated(true);


        networkPanel = new NetworkPanel(this.gameModel);



        gameModel.setRepaintCallback(() -> {
            if (networkPanel != null) networkPanel.repaint();
            updateButtonStates(); 
        });
        gameModel.setUpdateStatsCallback(() -> {
            if (networkPanel != null) networkPanel.updateStatsDisplay();
            updateButtonStates(); 
        });


        setupInitialModel_Level1(this.gameModel.getNetworkModel());

        setTitle("Level 1 - Network System Simulator");
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());


        add(networkPanel, BorderLayout.CENTER);


        NetworkController controller = new NetworkController(this.gameModel, networkPanel);
        networkPanel.setController(controller);


        JButton backToMenuButton = new JButton("Main Menu");
        backToMenuButton.addActionListener(e -> returnToMainMenu());

        shopButton = new JButton("Shop (فروشگاه)");
        shopButton.addActionListener(e -> {
            gameModel.openShop();
            if (gameModel.isShopOpen()) {
                ShopDialog shopDialog = new ShopDialog(this, gameModel);
                shopDialog.setVisible(true);
                gameModel.closeShop();
            }
            networkPanel.requestFocusInWindow();
        });
        shopButton.setVisible(false);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(backToMenuButton, BorderLayout.WEST);
        topPanel.add(shopButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        setPreferredSize(new Dimension(1200, 800));
        pack();
        setLocationRelativeTo(null);

        updateButtonStates();
        networkPanel.updateStatsDisplay();
        networkPanel.requestFocusInWindow();
    }

    private void updateButtonStates() {
        if (gameModel != null && shopButton != null) {
            boolean visible = gameModel.isGameRunning() && gameModel.areSnapshotsReady() && !gameModel.isGameOverTriggered();
            shopButton.setVisible(visible);
        }
    }

    public void returnToMainMenu() {
        SoundManager.stopAllSounds();
        SoundManager.loopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
        this.dispose();
        if (this.mainMenuFrameRef != null) {
            this.mainMenuFrameRef.setVisible(true);
        }
    }


    private static void setupInitialModel_Level1(NetworkModel model) {
        if (model == null) return;


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


    
}