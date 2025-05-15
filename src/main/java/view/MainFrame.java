package view;

import controller.NetworkController;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private NetworkPanel networkPanel;
    private NetworkModel networkModel;

    public MainFrame() {
        this.networkModel = new NetworkModel();
        setupInitialModel(this.networkModel);

        setTitle("Network System Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        networkPanel = new NetworkPanel(this.networkModel);
        add(networkPanel, BorderLayout.CENTER);

        NetworkController controller = new NetworkController(this.networkModel, networkPanel);
        networkPanel.addMouseListener(controller);
        networkPanel.addMouseMotionListener(controller);


        JButton addSystemButton = new JButton("Add Test System");
        addSystemButton.addActionListener(e -> {
            Point newPos = new Point(50 + (networkModel.getSystems().size() % 5) * 180,
                    350 + (networkModel.getSystems().size() / 5) * 150);
            ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)));
            ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)));
            NonSourceNetworkSystem newSys = new NonSourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, 5);
            controller.addNetworkSystem(newSys);
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(addSystemButton);
        add(controlPanel, BorderLayout.SOUTH);


        setPreferredSize(new Dimension(1000, 750));
        pack();
        setLocationRelativeTo(null);
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    private static void setupInitialModel(NetworkModel model) {
        ArrayList<Port> s1InPorts = new ArrayList<>(List.of(
                new Port(IOType.INPUT, PacketAndPortShape.SQUARE),
                new Port(IOType.INPUT, PacketAndPortShape.SQUARE)
        ));
        ArrayList<Port> s1OutPorts = new ArrayList<>(List.of(
                new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)
        ));
        SourceNetworkSystem sys1 = new SourceNetworkSystem(
                IndicatorState.ON, new Point(50, 50), 120, 20, 80,
                s1InPorts, s1OutPorts,
                new ArrayList<>(List.of(new Packet(new Point(0,0), PacketAndPortShape.TRIANGLE, 8)))
        );
        model.addSystem(sys1);


        ArrayList<Port> s2InPorts = new ArrayList<>(List.of(
                new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)
        ));
        ArrayList<Port> s2OutPorts = new ArrayList<>(List.of(
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE),
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)
        ));
        NonSourceNetworkSystem sys2 = new NonSourceNetworkSystem(
                IndicatorState.OFF, new Point(300, 100), 110, 18, 70,
                s2InPorts, s2OutPorts, 10
        );
        model.addSystem(sys2);

        ArrayList<Port> s3InPorts = new ArrayList<>(List.of(
                new Port(IOType.INPUT, PacketAndPortShape.TRIANGLE)
        ));
        ArrayList<Port> s3OutPorts = new ArrayList<>(List.of(
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE),
                new Port(IOType.OUTPUT, PacketAndPortShape.TRIANGLE)
        ));
        NonSourceNetworkSystem sys3 = new NonSourceNetworkSystem(
                IndicatorState.ON, new Point(100, 250), 140, 22, 90,
                s3InPorts, s3OutPorts, 5
        );
        model.addSystem(sys3);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}