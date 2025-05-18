package controller;

import model.*;
import view.NetworkPanel;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.List;

public class NetworkController extends MouseAdapter {
    private NetworkModel model;
    private NetworkPanel view;

    private Port dragStartPort = null;
    // temporaryWireColor is now managed by the view, removed from controller

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
        // If a drag was in progress and the user clicked elsewhere, cancel the drag state.
        if (dragStartPort != null) {
            dragStartPort.setSelectedForConnection(false);
            view.setFirstPortForWire(null);
            view.setCurrentMouseForWire(null);
            view.setTemporaryWireColor(Color.gray); // Reset temporary wire color in view
            dragStartPort = null;
            System.out.println("Clicked empty space, deselected drag.");
            view.repaint();
        } else {
            // This block is now mainly for potential future features not related to wire creation/deletion.
            // For this update, we've removed the click-to-connect logic.
            Point clickPoint = e.getPoint();
            Port clickedPort = findPortAtPoint(clickPoint);
            if (clickedPort != null) {
                System.out.println("Clicked port: " + clickedPort.getId() + " (" + clickedPort.getIoType() + ")");
            } else {
                System.out.println("Clicked empty space.");
            }
            view.repaint(); // Repaint might be needed if clicking affects selection states
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Check if it's a left-click to start a drag
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point pressPoint = e.getPoint();
            Port pressedPort = findPortAtPoint(pressPoint);

            // Start drag from an unconnected output OR an unconnected input port
            if (pressedPort != null && !pressedPort.isConnected()) {
                if (pressedPort.getIoType() == IOType.OUTPUT || pressedPort.getIoType() == IOType.INPUT) {
                    dragStartPort = pressedPort;
                    view.setFirstPortForWire(dragStartPort); // Set the start port for the view to draw from
                    view.setCurrentMouseForWire(pressPoint); // Set the current mouse position
                    view.setTemporaryWireColor(Color.gray); // Set initial color to gray
                    dragStartPort.setSelectedForConnection(true); // Indicate the port is selected for connection
                    System.out.println("Started drag from " + dragStartPort.getIoType() + " port: " + dragStartPort.getId());
                    view.repaint(); // Repaint to show the selected port and initial gray wire
                }
            } else {
                // If we press on a port that is not a valid start, or on empty space,
                // and a drag was previously started, cancel it.
                if (dragStartPort != null) {
                    dragStartPort.setSelectedForConnection(false);
                    view.setFirstPortForWire(null);
                    view.setCurrentMouseForWire(null);
                    view.setTemporaryWireColor(Color.gray); // Reset temporary wire color in view
                    dragStartPort = null;
                    System.out.println("Drag cancelled on press.");
                    view.repaint();
                }
            }
        }
        // Right-click handling for deletion is in mouseReleased to ensure the click is complete
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStartPort != null) {
            view.setCurrentMouseForWire(e.getPoint()); // Update the end point of the temporary wire
            Point currentMousePoint = e.getPoint();
            Port hoveredPort = findPortAtPoint(currentMousePoint);

            Color colorToSet;
            if (hoveredPort != null) {
                // Check if it's a valid connection target based on the drag start port type
                if (isValidConnection(dragStartPort, hoveredPort)) {
                    colorToSet = Color.green; // Valid connection target
                } else {
                    colorToSet = Color.red; // Invalid port
                }
            } else {
                colorToSet = Color.gray; // Not over a port
            }
            view.setTemporaryWireColor(colorToSet); // Pass the determined color to the view
            // Repaint is now handled by view.setTemporaryWireColor()
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Check if it's a right-click (popup trigger) for deletion
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) { // BUTTON3 is a common check for right-click
            Point clickPoint = e.getPoint();
            Wire wireToDelete = findWireAtPoint(clickPoint);

            if (wireToDelete != null) {
                model.removeWire(wireToDelete);
                System.out.println("Deleted wire.");
                view.repaint(); // Repaint after deleting the wire
            } else {
                System.out.println("Right-clicked, but no wire found.");
            }
            // Do not proceed with drag release logic if it was a right-click for deletion
            return;
        }

        // Existing logic for handling the release of a drag for wire creation (left-click release)
        if (dragStartPort != null && e.getButton() == MouseEvent.BUTTON1) { // Ensure it's a left-click release
            Point releasePoint = e.getPoint();
            Port releasePort = findPortAtPoint(releasePoint);

            boolean connected = false;
            if (releasePort != null) {
                // Check if the release port is a valid target based on the drag start port type
                if (isValidConnection(dragStartPort, releasePort)) {
                    Color wireColor;
                    int colorIndex = model.getWires().size() % 3;
                    if (colorIndex == 0) wireColor = new Color(100, 255, 100); // Greenish
                    else if (colorIndex == 1) wireColor = new Color(255, 100, 200); // Pinkish
                    else wireColor = new Color(255, 255, 100); // Yellowish

                    // Determine source and destination based on which port was the drag start
                    Port sourcePort, destPort;
                    if (dragStartPort.getIoType() == IOType.OUTPUT && releasePort.getIoType() == IOType.INPUT) {
                        sourcePort = dragStartPort;
                        destPort = releasePort;
                    } else if (dragStartPort.getIoType() == IOType.INPUT && releasePort.getIoType() == IOType.OUTPUT) {
                        sourcePort = releasePort; // Output is the source
                        destPort = dragStartPort; // Input is the destination
                    } else {
                        // Should not happen if isValidConnection passed, but as a fallback
                        System.out.println("Invalid connection type on release.");
                        sourcePort = null; destPort = null; // Prevent wire creation
                    }

                    if (sourcePort != null && destPort != null) {
                        StraightWire newWire = new StraightWire(sourcePort, destPort, wireColor);
                        model.addWire(newWire);
                        sourcePort.setConnectedWire(newWire);
                        destPort.setConnectedWire(newWire);
                        System.out.println("Wire created between " + sourcePort.getId() + " and " + destPort.getId());
                        connected = true;
                    }

                } else {
                    System.out.println("Release on invalid port or connection.");
                }
            } else {
                System.out.println("Release on empty space.");
            }

            // Reset drag state
            if (dragStartPort != null) { // Ensure dragStartPort is not null before accessing
                dragStartPort.setSelectedForConnection(false);
            }
            view.setFirstPortForWire(null);
            view.setCurrentMouseForWire(null);
            view.setTemporaryWireColor(Color.gray); // Reset temporary wire color in view
            dragStartPort = null;

            view.repaint(); // Repaint to show the new wire or clear the temporary one
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // This method's primary role in drawing a temporary wire when a first port was clicked
        // is now handled by mouseDragged. It's kept as is but will have less visual impact
        // during a drag operation.
        if (view.getFirstPortForWire() != null && dragStartPort != null) {
            // During a drag, mouseDragged is active.
            // This block might not be reached or have visual effect.
        }
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

    // New method to find a wire at or near a given point
    private Wire findWireAtPoint(Point p) {
        final double tolerance = 5.0; // Pixels tolerance for clicking near a wire
        for (Wire wire : model.getWires()) {
            Port source = wire.getSourcePort();
            Port dest = wire.getDestinationPort();
            if (source != null && dest != null) {
                Point p1 = source.getAbsolutePosition();
                Point p2 = dest.getAbsolutePosition();
                Line2D line = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);

                // Check distance from the point to the line segment
                if (line.ptSegDist(p) < tolerance) {
                    return wire; // Found a wire near the click point
                }
            }
        }
        return null; // No wire found near the click point
    }


    private boolean isValidConnection(Port port1, Port port2) {
        if (port1 == null || port2 == null) return false;
        if (port1.getNetworkSystem() == port2.getNetworkSystem()) {
            // System.out.println("Error: Cannot connect ports on the same system."); // Keep console logs for debugging
            return false; // Cannot connect ports on the same system
        }
        if (port1.isConnected() || port2.isConnected()) {
            // System.out.println("Error: One or both ports already connected."); // Keep console logs for debugging
            return false; // One or both ports already have a wire
        }

        // Allow connection if dragging from Output to Input OR from Input to Output
        if ((port1.getIoType() == IOType.OUTPUT && port2.getIoType() == IOType.INPUT) ||
                (port1.getIoType() == IOType.INPUT && port2.getIoType() == IOType.OUTPUT)) {
            return true;
        }

        // System.out.println("Error: Invalid port type combination for connection."); // Keep console logs for debugging
        return false; // Invalid combination (e.g., Output to Output, Input to Input)
    }
}