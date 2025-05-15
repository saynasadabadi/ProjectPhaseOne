package controller;

import model.*;
import view.NetworkPanel;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NetworkController extends MouseAdapter {
    private NetworkModel model;
    private NetworkPanel view;

    public NetworkController(NetworkModel model, NetworkPanel view) {
        this.model = model;
        this.view = view;
    }

    // Method for the button in MainFrame to call
    public void addNetworkSystem(NetworkSystem system) {
        model.addSystem(system);
        view.repaint();
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        Point clickPoint = e.getPoint();
        Port clickedPort = findPortAtPoint(clickPoint);

        Port firstSelected = view.getFirstPortForWire();

        if (clickedPort != null) {
            if (firstSelected == null) {
                if (!clickedPort.isConnected()) {
                    view.setFirstPortForWire(clickedPort);
                    System.out.println("Selected first port: " + clickedPort.getId() + " (" + clickedPort.getIoType() + ")");
                } else {
                    System.out.println("Port " + clickedPort.getId() + " is already connected.");
                }
            } else {
                if (clickedPort == firstSelected) {
                    firstSelected.setSelectedForConnection(false);
                    view.setFirstPortForWire(null);
                    view.setCurrentMouseForWire(null);
                    System.out.println("Deselected port: " + clickedPort.getId());
                } else if (isValidConnection(firstSelected, clickedPort)) {
                    Color wireColor;
                    int colorIndex = model.getWires().size() % 3;
                    if (colorIndex == 0) wireColor = new Color(100, 255, 100); // Greenish
                    else if (colorIndex == 1) wireColor = new Color(255, 100, 200); // Pinkish
                    else wireColor = new Color(255, 255, 100); // Yellowish

                    StraightWire newWire = new StraightWire(firstSelected, clickedPort, wireColor);
                    model.addWire(newWire);
                    firstSelected.setConnectedWire(newWire);
                    clickedPort.setConnectedWire(newWire);

                    System.out.println("Wire created between " + firstSelected.getId() + " and " + clickedPort.getId());

                    firstSelected.setSelectedForConnection(false); // Deselect after connection
                    view.setFirstPortForWire(null);
                    view.setCurrentMouseForWire(null);
                } else {
                    System.out.println("Invalid connection attempt.");
                    firstSelected.setSelectedForConnection(false);
                    view.setFirstPortForWire(null);
                    view.setCurrentMouseForWire(null);
                }
            }
        } else {
            if (firstSelected != null) {
                firstSelected.setSelectedForConnection(false);
                view.setFirstPortForWire(null);
                view.setCurrentMouseForWire(null);
                System.out.println("Clicked empty space, deselected.");
            }
        }
        view.repaint();
    }

    private Port findPortAtPoint(Point p) {
        for (NetworkSystem system : model.getSystems()) {
            Port port = system.getPortAt(p);
            if (port != null) {
                return port;
            }
        }
        return null;
    }

    private boolean isValidConnection(Port port1, Port port2) {
        if (port1 == null || port2 == null) return false;
        if (port1.getNetworkSystem() == port2.getNetworkSystem()) {
            System.out.println("Error: Cannot connect ports on the same system.");
            return false; // Cannot connect ports on the same system
        }
        if (port1.isConnected() || port2.isConnected()) {
            System.out.println("Error: One or both ports already connected.");
            return false; // One or both ports already have a wire
        }
        return (port1.getIoType() != port2.getIoType());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (view.getFirstPortForWire() != null) {
            view.setCurrentMouseForWire(e.getPoint());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {}
}