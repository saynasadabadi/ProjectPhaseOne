package view;

import model.*; // Assuming GameModel is in model package

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class NetworkPanel extends JPanel {
    private GameModel gameModel;
    private transient Port firstPortForWire = null;
    private transient Point currentMouseForWire = null;
    private transient Color temporaryWireColor = Color.gray;

    public NetworkPanel(GameModel model) {
        this.gameModel = model;
        this.setBackground(new Color(20, 25, 30)); // Dark background
        // It's good practice to set a preferred size if the layout manager respects it
        this.setPreferredSize(new Dimension(800, 600));
    }

    public void setGameModel(GameModel model) {
        this.gameModel = model;
        repaint();
    }

    public void setFirstPortForWire(Port port) {
        if (this.firstPortForWire != null) {
            this.firstPortForWire.setSelectedForConnection(false); // Deselect old one
        }
        this.firstPortForWire = port;
        if (port != null) {
            port.setSelectedForConnection(true); // Select new one
        }
        repaint();
    }

    public Port getFirstPortForWire() {
        return this.firstPortForWire;
    }

    public void setCurrentMouseForWire(Point mousePoint) {
        this.currentMouseForWire = mousePoint;
        // Repaint is usually called by setTemporaryWireColor or other actions
    }

    public void setTemporaryWireColor(Color color) {
        this.temporaryWireColor = color;
        repaint(); // Repaint when color changes to give immediate feedback
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);


        drawGrid(g2d);

        if (gameModel == null || gameModel.getNetworkModel() == null) {
            g2d.setColor(Color.RED);
            g2d.drawString("Error: GameModel or NetworkModel is null!", 50, 50);
            g2d.dispose();
            return;
        }

        NetworkModel networkModel = gameModel.getNetworkModel();



        // Draw Network Systems and their Ports
        for (NetworkSystem system : networkModel.getSystems()) {
            drawNetworkSystem(g2d, system); // This will also call drawPort
        }

        for (Wire wire : networkModel.getWires()) {
            drawWire(g2d, wire);
        }


        // Draw Temporary Wire for connection dragging
        if (firstPortForWire != null && currentMouseForWire != null && firstPortForWire.getAbsolutePosition() != null) {
            drawTemporaryWire(g2d, firstPortForWire.getAbsolutePosition(), currentMouseForWire, temporaryWireColor);
        }

        // Draw Packets
        // Iterate over a copy if concurrent modification is an issue, though GameModel uses CopyOnWriteArrayList for activePackets.
        for (model.Packet packet : networkModel.getPackets()) {
            if (packet.getState() == PacketState.ON_WIRE || packet.getState() == PacketState.IN_NETWORK_SYSTEM) { // Only draw active packets
                drawPacket(g2d, packet);
            }
        }

        g2d.dispose();
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(40, 45, 50)); // Darker grid lines
        int gridSize = 25;
        for (int x = 0; x < getWidth(); x += gridSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawNetworkSystem(Graphics2D g2d, NetworkSystem system) {
        Point sysPos = system.getPosition();
        int width = system.getWidth();
        int bodyH = system.getBodyHeight();
        int indH = system.getIndicatorHeight();
        float arc = 15.0f; // Slightly less rounded for a more "blocky" feel

        Color bodyFillColor;
        Color bodyBorderColor;
        Color indicatorFillColor;

        // Base colors
        if (system instanceof SourceNetworkSystem) {
            bodyFillColor = new Color(60, 80, 60); // Darker green
            bodyBorderColor = bodyFillColor.darker();
        } else if (system instanceof NonSourceNetworkSystem) {
            bodyFillColor = new Color(70, 75, 80); // Darker grey-blue
            bodyBorderColor = bodyFillColor.darker();
        } else {
            bodyFillColor = new Color(60, 60, 60); // Generic dark
            bodyBorderColor = bodyFillColor.darker();
        }

        // Indicator color based on state
        indicatorFillColor = system.getIndicatorState().getColor();
        if (system.getIndicatorState() == IndicatorState.ON) {
            if (system instanceof SourceNetworkSystem) indicatorFillColor = new Color(150, 255, 150); // Brighter green for ON
            else indicatorFillColor = new Color(200, 200, 255); // Brighter blue/purple for ON
        }


        // Draw Body
        g2d.setColor(bodyFillColor);
        g2d.fill(new RoundRectangle2D.Float(sysPos.x, sysPos.y + indH, width, bodyH, arc, arc));
        g2d.setColor(bodyBorderColor);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new RoundRectangle2D.Float(sysPos.x, sysPos.y + indH, width, bodyH, arc, arc));

        // Draw Indicator
        g2d.setColor(indicatorFillColor);
        g2d.fill(new RoundRectangle2D.Float(sysPos.x, sysPos.y, width, indH, arc / 2f, arc / 2f)); // Top corners less rounded
        g2d.setColor(indicatorFillColor.darker());
        g2d.draw(new RoundRectangle2D.Float(sysPos.x, sysPos.y, width, indH, arc / 2f, arc / 2f));

        g2d.setStroke(new BasicStroke(1f)); // Reset stroke

        for (Port port : system.getAllPorts()) {
            if (port.getRelativePosition() != null) { // Ensure port is initialized
                drawPort(g2d, port);
            } else {
                System.err.println("Attempted to draw port " + port.getId() + " with null relativePosition.");
            }
        }
    }

    private void drawPort(Graphics2D g2d, Port port) {
        Point absPortPos = port.getAbsolutePosition();
        if (absPortPos == null) {
            System.err.println("Port " + port.getId() + " has null absolute position. Cannot draw.");
            return;
        }

        int s = NetworkSystem.PORT_VISUAL_SIZE;
        PacketAndPortShape shapeType = port.getShape();

        g2d.setColor(shapeType.getColor());

        // Define polygon based on shape
        Polygon portPolygon = new Polygon();
        if (shapeType == PacketAndPortShape.SQUARE) {
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y + s / 2);
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y + s / 2);
        } else if (shapeType == PacketAndPortShape.TRIANGLE) {
            // Pointing outwards: if output (right side), points right. if input (left side), points left.
            if (port.getIoType() == IOType.OUTPUT) {
                portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y - s / 2); // top-left
                portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y);         // mid-right
                portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y + s / 2); // bottom-left
            } else { // INPUT or default
                portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y - s / 2); // top-right
                portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y);         // mid-left
                portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y + s / 2); // bottom-right
            }
        } else { // Default to a circle or a fallback square
            g2d.fillOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
            g2d.setColor(shapeType.getColor().darker());
            g2d.drawOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
        }

        if (portPolygon.npoints > 0) {
            g2d.fillPolygon(portPolygon);
            g2d.setColor(shapeType.getColor().darker());
            g2d.drawPolygon(portPolygon);
        }

        // Highlight if selected for connection or in use
        if (port.isSelectedForConnection()) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2f));
            if (portPolygon.npoints > 0) g2d.drawPolygon(portPolygon);
            else g2d.drawOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
        } else if (port.isInUse()) {
            g2d.setColor(Color.ORANGE.darker()); // Indicate 'inUse'
            g2d.setStroke(new BasicStroke(1.5f));
            if (portPolygon.npoints > 0) g2d.drawPolygon(portPolygon);
            else g2d.drawOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
        }
        g2d.setStroke(new BasicStroke(1f)); // Reset stroke
    }

    private void drawWire(Graphics2D g2d, Wire wire) {
        Port source = wire.getSourcePort();
        Port dest = wire.getDestinationPort();

        if (source == null || dest == null || source.getAbsolutePosition() == null || dest.getAbsolutePosition() == null) {
            //System.err.println("Cannot draw wire, source or destination port (or their positions) are null.");
            return;
        }

        Point p1 = source.getAbsolutePosition();
        Point p2 = dest.getAbsolutePosition();

        g2d.setColor(wire.getColor());
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Thicker wires
        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        g2d.setStroke(new BasicStroke(1f)); // Reset stroke
    }

    private void drawTemporaryWire(Graphics2D g2d, Point start, Point end, Color color) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{5.0f, 5.0f}, 0.0f)); // Dashed line
        g2d.drawLine(start.x, start.y, end.x, end.y);
        g2d.setStroke(new BasicStroke(1f));
    }

    private void drawPacket(Graphics2D g2d, model.Packet packet) {
        if (packet.getPosition() == null || packet.getShape() == null) {
            //System.err.println("Packet has null position or shape, cannot draw: " + packet.getId());
            return;
        }

        List<Point> verticesList = packet.getVertices(); // Uses packet's radius and position
        if (verticesList.isEmpty()) {
            //System.err.println("Packet getVertices() returned empty list, cannot draw: " + packet.getId());
            return;
        }

        Polygon polygon = new Polygon();
        for (Point vertex : verticesList) {
            polygon.addPoint(vertex.x, vertex.y);
        }

        g2d.setColor(packet.getColor()); // Uses packet's shape color (brightened)
        g2d.fillPolygon(polygon);
        g2d.setColor(packet.getColor().darker().darker()); // Darker border for contrast
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawPolygon(polygon);
        g2d.setStroke(new BasicStroke(1f));
    }
}