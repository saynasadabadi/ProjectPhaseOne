package controller;

import model.*; // Assuming GameModel is in model package
import view.NetworkPanel;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.List;

public class NetworkController extends MouseAdapter {
    private GameModel gameModel; // Changed from NetworkModel to GameModel
    private NetworkPanel view;
    private Port dragStartPort = null;

    public NetworkController(GameModel model, NetworkPanel view) { // Constructor now takes GameModel
        this.gameModel = model;
        this.view = view;
    }

    // Method for the button in MainFrame to call
    public void addNetworkSystem(NetworkSystem system) {
        // Access NetworkModel through GameModel
        if (gameModel != null && gameModel.getNetworkModel() != null) {
            gameModel.getNetworkModel().addSystem(system);
            view.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (dragStartPort != null) {
            dragStartPort.setSelectedForConnection(false);
            view.setFirstPortForWire(null);
            view.setCurrentMouseForWire(null);
            view.setTemporaryWireColor(Color.gray);
            dragStartPort = null;
            System.out.println("Clicked empty space, deselected drag.");
            view.repaint();
        } else {
            Point clickPoint = e.getPoint();
            Port clickedPort = findPortAtPoint(clickPoint);
            if (clickedPort != null) {
                System.out.println("Clicked port: " + clickedPort.getId() + " (" + clickedPort.getIoType() + ")");
                // Example of interacting with the system via click (e.g., toggling indicator)
                // This is just an example, you might want a different interaction
                if (clickedPort.getNetworkSystem() != null) {
                    NetworkSystem system = clickedPort.getNetworkSystem();
                    if (system.getIndicatorState() == IndicatorState.ON) {
                        system.setIndicatorState(IndicatorState.OFF);
                    } else {
                        system.setIndicatorState(IndicatorState.ON);
                    }
                    system.updateIndicatorState(); // Ensure this method exists and updates based on new state
                    System.out.println("Toggled indicator state for system of port: " + clickedPort.getId() + " to " + system.getIndicatorState());
                    view.repaint();
                }

            } else {
                System.out.println("Clicked empty space.");
            }
            // view.repaint(); // Repaint is handled by actions above or if drag was cancelled
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
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
                    System.out.println("Started drag from " + dragStartPort.getIoType() + " port: " + dragStartPort.getId());
                    view.repaint();
                }
            } else {
                if (dragStartPort != null) {
                    dragStartPort.setSelectedForConnection(false);
                    view.setFirstPortForWire(null);
                    view.setCurrentMouseForWire(null);
                    view.setTemporaryWireColor(Color.gray);
                    dragStartPort = null;
                    System.out.println("Drag cancelled on press.");
                    view.repaint();
                }
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStartPort != null) {
            view.setCurrentMouseForWire(e.getPoint());
            Point currentMousePoint = e.getPoint();
            Port hoveredPort = findPortAtPoint(currentMousePoint);

            Color colorToSet;
            if (hoveredPort != null) {
                if (isValidConnection(dragStartPort, hoveredPort)) {
                    colorToSet = Color.green;
                } else {
                    colorToSet = Color.red;
                }
            } else {
                colorToSet = Color.gray;
            }
            view.setTemporaryWireColor(colorToSet);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        NetworkModel currentNetworkModel = gameModel.getNetworkModel(); // Get current NetworkModel instance
        if (currentNetworkModel == null) {
            System.err.println("NetworkController: NetworkModel is null in mouseReleased. Cannot proceed.");
            if (dragStartPort != null) { // Still reset drag state if it was active
                dragStartPort.setSelectedForConnection(false);
                view.setFirstPortForWire(null);
                view.setCurrentMouseForWire(null);
                view.setTemporaryWireColor(Color.gray);
                dragStartPort = null;
                view.repaint();
            }
            return;
        }


        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            Point clickPoint = e.getPoint();
            Wire wireToDelete = findWireAtPoint(clickPoint);

            if (wireToDelete != null) {
                currentNetworkModel.removeWire(wireToDelete); // Use currentNetworkModel
                System.out.println("Deleted wire.");
                view.repaint();
            } else {
                System.out.println("Right-clicked, but no wire found.");
            }
            return;
        }

        if (dragStartPort != null && e.getButton() == MouseEvent.BUTTON1) {
            Point releasePoint = e.getPoint();
            Port releasePort = findPortAtPoint(releasePoint);

            boolean connected = false;
            if (releasePort != null) {
                if (isValidConnection(dragStartPort, releasePort)) {
                    Color wireColor;
                    int colorIndex = currentNetworkModel.getWires().size() % 3; // Use currentNetworkModel
                    if (colorIndex == 0) wireColor = new Color(100, 255, 100);
                    else if (colorIndex == 1) wireColor = new Color(255, 100, 200);
                    else wireColor = new Color(255, 255, 100);

                    Port sourcePort, destPort;
                    if (dragStartPort.getIoType() == IOType.OUTPUT && releasePort.getIoType() == IOType.INPUT) {
                        sourcePort = dragStartPort;
                        destPort = releasePort;
                    } else if (dragStartPort.getIoType() == IOType.INPUT && releasePort.getIoType() == IOType.OUTPUT) {
                        sourcePort = releasePort;
                        destPort = dragStartPort;
                    } else {
                        sourcePort = null; destPort = null;
                    }

                    if (sourcePort != null && destPort != null) {
                        StraightWire newWire = new StraightWire(sourcePort, destPort, wireColor);
                        currentNetworkModel.addWire(newWire); // Use currentNetworkModel
                        // sourcePort.setConnectedWire(newWire); // addWire should handle this via updateIndicatorState if ports know their systems
                        // destPort.setConnectedWire(newWire);
                        System.out.println("Wire created between " + sourcePort.getId() + " and " + destPort.getId());
                        connected = true;
                    }

                } else {
                    System.out.println("Release on invalid port or connection.");
                }
            } else {
                System.out.println("Release on empty space.");
            }

            if (dragStartPort != null) {
                dragStartPort.setSelectedForConnection(false);
            }
            view.setFirstPortForWire(null);
            view.setCurrentMouseForWire(null);
            view.setTemporaryWireColor(Color.gray);
            dragStartPort = null;

            view.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // No changes needed here for now
    }


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
            if (source != null && dest != null) {
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
        if (port1 == null || port2 == null) return false;
        if (port1.getNetworkSystem() == port2.getNetworkSystem()) {
            return false;
        }
        if (port1.isConnected() || port2.isConnected()) {
            return false;
        }

        if ((port1.getIoType() == IOType.OUTPUT && port2.getIoType() == IOType.INPUT) ||
                (port1.getIoType() == IOType.INPUT && port2.getIoType() == IOType.OUTPUT)) {
            return true;
        }
        return false;
    }
}