package controller;

import model.*;
import view.NetworkPanel;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyAdapter; // Import KeyAdapter
import java.awt.event.KeyEvent;   // Import KeyEvent
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

// Implement KeyListener or extend KeyAdapter
public class NetworkController extends MouseAdapter implements java.awt.event.KeyListener {
    private GameModel gameModel;
    private NetworkPanel view;
    private Port dragStartPort = null;

    public NetworkController(GameModel model, NetworkPanel view) {
        this.gameModel = model;
        this.view = view;
    }

    public void addSourceSystemAction() {
        if (gameModel.isGameRunning()) {
            System.out.println("Cannot add systems: Game is running.");
            return;
        }
        NetworkModel currentNetworkModel = gameModel.getNetworkModel();
        if (currentNetworkModel != null) {
            Point newPos = new Point(50 + (currentNetworkModel.getSystems().size() % 6) * 150,
                    50 + (currentNetworkModel.getSystems().size() / 6) * 150);
            ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
            ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
            ArrayList<Packet> initialPackets = new ArrayList<>();
            initialPackets.add(new Packet(new Point(0,0), PacketAndPortShape.SQUARE, 8));
            SourceNetworkSystem newSys = new SourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, initialPackets);
            addNetworkSystem(newSys); // Use existing method
            view.updateStatsDisplay();
        }
    }

    public void addNonSourceSystemAction() {
        if (gameModel.isGameRunning()) {
            System.out.println("Cannot add systems: Game is running.");
            return;
        }
        NetworkModel currentNetworkModel = gameModel.getNetworkModel();
        if (currentNetworkModel != null) {
            Point newPos = new Point(70 + (currentNetworkModel.getSystems().size() % 6) * 150,
                    70 + (currentNetworkModel.getSystems().size() / 6) * 150);
            ArrayList<Port> inputs = new ArrayList<>(List.of(new Port(IOType.INPUT, PacketAndPortShape.SQUARE)));
            ArrayList<Port> outputs = new ArrayList<>(List.of(new Port(IOType.OUTPUT, PacketAndPortShape.SQUARE)));
            NonSourceNetworkSystem newSys = new NonSourceNetworkSystem(IndicatorState.OFF, newPos, 100, 15, 60, inputs, outputs, 5);
            addNetworkSystem(newSys); // Use existing method
            view.updateStatsDisplay();
        }
    }

    public void toggleExecutionAction() {
        if (!gameModel.isGameRunning()) {
            gameModel.startExecution();
        } else {
            gameModel.stopExecution();
        }
        view.updateStatsDisplay(); // Update button text & stats
    }

    public void reDesignAction() {
        gameModel.reDesign();
        view.updateStatsDisplay(); // Update button visibility & stats
    }

    public void addNetworkSystem(NetworkSystem system) {
        if (gameModel != null && gameModel.getNetworkModel() != null) {
            gameModel.getNetworkModel().addSystem(system);
            view.repaint();
        }
    }

    // ... (Keep ALL existing MouseAdapter methods: mouseClicked, mousePressed, etc.) ...
    @Override
    public void mouseClicked(MouseEvent e) {
        if (dragStartPort != null) {
            dragStartPort.setSelectedForConnection(false);
            view.setFirstPortForWire(null);
            view.setCurrentMouseForWire(null);
            view.setTemporaryWireColor(Color.gray);
            dragStartPort = null;
            gameModel.setTemporaryWireLength(0.0);
            view.repaint();
        } else {
            Point clickPoint = e.getPoint();
            Wire wireToDelete = findWireAtPoint(clickPoint);

            if (wireToDelete != null && e.getButton() == MouseEvent.BUTTON3) { // Right-click deletes
                if (gameModel.isGameRunning()) {
                    System.out.println("Cannot delete wires: Game is running.");
                    return;
                }
                gameModel.getNetworkModel().removeWire(wireToDelete);
                System.out.println("Deleted wire.");
                view.repaint();
                gameModel.triggerStatsUpdate();
            } else {
                Port clickedPort = findPortAtPoint(clickPoint);
                if (clickedPort != null) {
                    System.out.println("Clicked port: " + clickedPort.getId());
                } else {
                    System.out.println("Clicked empty space.");
                }
            }
        }
        view.getParent().requestFocusInWindow(); // Ask frame to regain focus
    }
    @Override
    public void mousePressed(MouseEvent e) {
        if (gameModel.isGameRunning()) {
            System.out.println("Cannot modify wires: Game is running.");
            return;
        }
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point pressPoint = e.getPoint();
            Port pressedPort = findPortAtPoint(pressPoint);

            if (pressedPort != null && !pressedPort.isConnected()) {
                if (pressedPort.getIoType() == IOType.OUTPUT || pressedPort.getIoType() == IOType.INPUT) {
                    dragStartPort = pressedPort;
                    view.setFirstPortForWire(dragStartPort);
                    view.setCurrentMouseForWire(pressPoint);
                    view.setTemporaryWireColor(Color.gray);
                    dragStartPort.setSelectedForConnection(true);
                    view.repaint();
                }
            } else if (dragStartPort != null) {
                dragStartPort.setSelectedForConnection(false);
                view.setFirstPortForWire(null);
                view.setCurrentMouseForWire(null);
                dragStartPort = null;
                view.repaint();
            }
        }
        view.getParent().requestFocusInWindow();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStartPort != null) {
            view.setCurrentMouseForWire(e.getPoint());
            Point currentMousePoint = e.getPoint();
            Port hoveredPort = findPortAtPoint(currentMousePoint);
            Point startPos = dragStartPort.getAbsolutePosition();
            double tempWireLength = NetworkModel.calculateWireLength(startPos, currentMousePoint);
            gameModel.setTemporaryWireLength(tempWireLength);
            NetworkModel networkModel = gameModel.getNetworkModel();
            boolean canAddWire = networkModel != null && networkModel.canAddWire(tempWireLength);
            Color colorToSet = canAddWire ? (hoveredPort != null && isValidConnection(dragStartPort, hoveredPort) ? Color.green : Color.red) : Color.red;
            if (hoveredPort == null) colorToSet = canAddWire ? Color.gray : Color.red;
            view.setTemporaryWireColor(colorToSet);
        }
        view.getParent().requestFocusInWindow();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gameModel.isGameRunning()) return;

        NetworkModel currentNetworkModel = gameModel.getNetworkModel();
        gameModel.setTemporaryWireLength(0.0);

        if (dragStartPort != null && e.getButton() == MouseEvent.BUTTON1) {
            Point releasePoint = e.getPoint();
            Port releasePort = findPortAtPoint(releasePoint);
            boolean wireAdded = false;

            if (releasePort != null && isValidConnection(dragStartPort, releasePort)) {
                Point startPos = dragStartPort.getAbsolutePosition();
                Point endPos = releasePort.getAbsolutePosition();
                double wireLength = NetworkModel.calculateWireLength(startPos, endPos);

                if (currentNetworkModel.canAddWire(wireLength)) {
                    Port sourcePort = (dragStartPort.getIoType() == IOType.OUTPUT) ? dragStartPort : releasePort;
                    Port destPort = (dragStartPort.getIoType() == IOType.INPUT) ? dragStartPort : releasePort;
                    StraightWire newWire = new StraightWire(sourcePort, destPort, Color.CYAN);
                    currentNetworkModel.addWire(newWire);
                    wireAdded = true;
                } else {
                    System.out.println("Wire length limit exceeded.");
                }
            }

            dragStartPort.setSelectedForConnection(false);
            view.setFirstPortForWire(null);
            view.setCurrentMouseForWire(null);
            dragStartPort = null;
            view.repaint();
            if(wireAdded) gameModel.triggerStatsUpdate();
        }
        view.getParent().requestFocusInWindow();
    }
    // --- KeyListener Methods ---

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used, but required by interface
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameModel.isGameRunning()) {
            // Don't allow manual time control during execution
            return;
        }
        
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT) {
            gameModel.timeStepBackward();
            view.updateStatsDisplay(); // Update HUD after step
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            gameModel.timeStepForward();
            view.updateStatsDisplay(); // Update HUD after step
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used, but required by interface
    }

    // --- Helper Methods (Keep existing) ---
    private Port findPortAtPoint(Point p) {
        if (gameModel == null || gameModel.getNetworkModel() == null) return null;
        for (NetworkSystem system : gameModel.getNetworkModel().getSystems()) {
            Port port = system.getPortAt(p);
            if (port != null) {
                return port;
            }
        }
        return null;
    }
    private Wire findWireAtPoint(Point p) {
        if (gameModel == null || gameModel.getNetworkModel() == null) return null;
        final double tolerance = 5.0;
        for (Wire wire : gameModel.getNetworkModel().getWires()) {
            Port source = wire.getSourcePort();
            Port dest = wire.getDestinationPort();
            if (source != null && dest != null && source.getAbsolutePosition() != null && dest.getAbsolutePosition() != null) {
                Point p1 = source.getAbsolutePosition();
                Point p2 = dest.getAbsolutePosition();
                Line2D line = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);

                if (line.ptSegDist(p) < tolerance) {
                    return wire;
                }
            }
        }
        return null;
    }


    private boolean isValidConnection(Port port1, Port port2) {
        // ... (Keep existing validation logic) ...
        if (port1 == null || port2 == null) return false;
        if (port1.getNetworkSystem() == port2.getNetworkSystem()) return false;
        if (port1.isConnected() || port2.isConnected()) return false;
        boolean validIo = (port1.getIoType() == IOType.OUTPUT && port2.getIoType() == IOType.INPUT) ||
                (port1.getIoType() == IOType.INPUT && port2.getIoType() == IOType.OUTPUT);
        if (!validIo) return false;
        return port1.getShape() == port2.getShape(); // Must match shapes
    }
}