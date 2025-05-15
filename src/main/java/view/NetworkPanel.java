package view;

import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class NetworkPanel extends JPanel {
    private NetworkModel networkModel;
    private transient Port firstPortForWire = null;
    private transient Point currentMouseForWire = null;

    public NetworkPanel(NetworkModel model) {
        this.networkModel = model;
        this.setBackground(new Color(20, 25, 30));
    }

    public void setNetworkModel(NetworkModel model) {
        this.networkModel = model;
        repaint();
    }

    public void setFirstPortForWire(Port port) {
        this.firstPortForWire = port;
        if (port != null) {
            port.setSelectedForConnection(true);
        }
        repaint();
    }

    public Port getFirstPortForWire() {
        return this.firstPortForWire;
    }

    public void setCurrentMouseForWire(Point mousePoint) {
        this.currentMouseForWire = mousePoint;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2d);

        if (networkModel == null) {
            g2d.dispose();
            return;
        }

        for (NetworkSystem system : networkModel.getSystems()) {
            drawNetworkSystem(g2d, system);
        }

        for (Wire wire : networkModel.getWires()) {
            drawWire(g2d, wire);
        }

        if (firstPortForWire != null && currentMouseForWire != null) {
            drawTemporaryWire(g2d, firstPortForWire.getAbsolutePosition(), currentMouseForWire);
        }


        for (model.Packet packet : networkModel.getPackets()) {
            drawPacket(g2d, packet);
        }

        g2d.dispose();
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(40, 45, 50));
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
        float arc = 20.0f;

        g2d.setColor(new Color(80, 85, 90));
        g2d.fill(new RoundRectangle2D.Float(sysPos.x, sysPos.y + indH, width, bodyH, arc, arc));
        g2d.setColor(new Color(60,65,70));
        g2d.draw(new RoundRectangle2D.Float(sysPos.x, sysPos.y + indH, width, bodyH, arc, arc));


        g2d.setColor(system.getIndicatorState().getColor());
        g2d.fill(new RoundRectangle2D.Float(sysPos.x, sysPos.y, width, indH, arc / 1.5f, arc / 1.5f));
        g2d.setColor(system.getIndicatorState().getColor().darker());
        g2d.draw(new RoundRectangle2D.Float(sysPos.x, sysPos.y, width, indH, arc / 1.5f, arc / 1.5f));


        for (Port port : system.getAllPorts()) {
            drawPort(g2d, port);
        }
    }

    private void drawPort(Graphics2D g2d, Port port) {
        Point absPortPos = port.getAbsolutePosition();
        int s = NetworkSystem.PORT_VISUAL_SIZE;
        PacketAndPortShape shapeType = port.getShape();

        g2d.setColor(shapeType.getColor());

        Polygon portPolygon = new Polygon();
        if (shapeType == PacketAndPortShape.SQUARE) {
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y + s / 2);
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y + s / 2);
        } else if (shapeType == PacketAndPortShape.TRIANGLE) {
            portPolygon.addPoint(absPortPos.x, absPortPos.y - s / 2);
            portPolygon.addPoint(absPortPos.x + s / 2, absPortPos.y);
            portPolygon.addPoint(absPortPos.x, absPortPos.y + s / 2);
            portPolygon.addPoint(absPortPos.x - s / 2, absPortPos.y);
        } else {
            g2d.fillOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
            g2d.setColor(shapeType.getColor().darker());
            g2d.drawOval(absPortPos.x - s/2, absPortPos.y - s/2, s,s);
            return;
        }
        g2d.fillPolygon(portPolygon);
        g2d.setColor(shapeType.getColor().darker());
        g2d.drawPolygon(portPolygon);

        if (port.isSelectedForConnection()) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawPolygon(portPolygon);
            g2d.setStroke(new BasicStroke(1));
        }
    }

    private void drawWire(Graphics2D g2d, Wire wire) {
        if (wire.getPorts().size() < 2) return;

        Port source = wire.getSourcePort();
        Port dest = wire.getDestinationPort();
        if(source == null || dest == null) return;

        Point p1 = source.getAbsolutePosition();
        Point p2 = dest.getAbsolutePosition();

        g2d.setColor(wire.getColor());
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        g2d.setStroke(new BasicStroke(1f));
    }

    private void drawTemporaryWire(Graphics2D g2d, Point start, Point end) {
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{6.0f, 4.0f}, 0.0f));
        g2d.drawLine(start.x, start.y, end.x, end.y);
        g2d.setStroke(new BasicStroke(1f));
    }

    private void drawPacket(Graphics2D g2d, model.Packet packet) {
        if (packet.getPosition() == null || packet.getShape() == null) return;

        List<Point> verticesList = packet.getVertices();
        if (verticesList.isEmpty()) return;

        Polygon polygon = new Polygon();
        for (Point vertex : verticesList) {
            polygon.addPoint(vertex.x, vertex.y);
        }

        g2d.setColor(packet.getColor());
        g2d.fillPolygon(polygon);
        g2d.setColor(packet.getColor().darker());
        g2d.drawPolygon(polygon);
    }
}