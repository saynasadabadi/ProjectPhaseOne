package model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class NetworkSystem {
    private final String id;
    protected IndicatorState indicatorState;
    protected Point position;
    protected int width;
    protected int indicator_height;
    protected int body_height;
    protected ArrayList<Port> inputPorts;
    protected ArrayList<Port> outputPorts;

    public static final int PORT_VISUAL_SIZE = 12;
    public static final int PORT_CLICK_PADDING = 2;

    public NetworkSystem(IndicatorState indicatorState, Point position, int width, int indicator_height, int body_height,
                         ArrayList<Port> inputPorts, ArrayList<Port> outputPorts) {
        this.id = UUID.randomUUID().toString();
        this.indicatorState = indicatorState;
        this.position = position;
        this.width = width;
        this.indicator_height = indicator_height;
        this.body_height = body_height;
        this.inputPorts = (inputPorts == null) ? new ArrayList<>() : inputPorts;
        this.outputPorts = (outputPorts == null) ? new ArrayList<>() : outputPorts;
        assignPortsToSystemAndPosition();
    }

    private void assignPortsToSystemAndPosition() {
        int numInputPorts = inputPorts.size();
        for (int i = 0; i < numInputPorts; i++) {
            Port port = inputPorts.get(i);
            port.setNetworkSystem(this);
            int portX = 0;
            int portY = indicator_height + (body_height * (i + 1) / (numInputPorts + 1));
            port.setRelativePosition(new Point(portX, portY));
        }

        int numOutputPorts = outputPorts.size();
        for (int i = 0; i < numOutputPorts; i++) {
            Port port = outputPorts.get(i);
            port.setNetworkSystem(this);
            int portX = width;
            int portY = indicator_height + (body_height * (i + 1) / (numOutputPorts + 1));
            port.setRelativePosition(new Point(portX, portY));
        }
    }
    public String getId() { return id; }
    public IndicatorState getIndicatorState() { return indicatorState; }
    public Point getPosition() { return position; }
    public int getWidth() { return width; }
    public int getIndicatorHeight() { return indicator_height; }
    public int getBodyHeight() { return body_height; }
    public int getTotalHeight() { return indicator_height + body_height; }
    public List<Port> getInputPorts() { return inputPorts; }
    public List<Port> getOutputPorts() { return outputPorts; }

    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<>(inputPorts);
        allPorts.addAll(outputPorts);
        return allPorts;
    }

    public Point getAbsolutePortPosition(Port port) {
        if (port.getRelativePosition() != null && position != null) {
            return new Point(position.x + port.getRelativePosition().x,
                    position.y + port.getRelativePosition().y);
        }
        return new Point(position.x, position.y);
    }

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, width, getTotalHeight());
    }

    public Rectangle getIndicatorBounds() {
        return new Rectangle(position.x, position.y, width, indicator_height);
    }

    public Rectangle getBodyBounds() {
        return new Rectangle(position.x, position.y + indicator_height, width, body_height);
    }

    public void setPosition(Point position) { this.position = position; }
    public void setIndicatorState(IndicatorState state) { this.indicatorState = state; }

    public Port getPortAt(Point clickPoint) {
        for (Port port : getAllPorts()) {
            Point portAbsPos = getAbsolutePortPosition(port);
            int s = PORT_VISUAL_SIZE + PORT_CLICK_PADDING;
            Rectangle portClickBounds = new Rectangle(portAbsPos.x - s / 2, portAbsPos.y - s / 2, s, s);
            if (portClickBounds.contains(clickPoint)) {
                return port;
            }
        }
        return null;
    }
}