package model;

import utils.Calculations;

import java.util.ArrayList;

public class StraightWire extends Wire{
    private Port inputPort;
    private Port outputPort;
    public StraightWire(ArrayList<Port> ports, int width, Color color) {
        super(ports, width, color);
        initializeInputAndOutputPorts();
    }
    @Override
    public double getLength() {
        return Calculations.calculateDistance(outputPort.getPosition(),inputPort.getPosition());
    }
    private void initializeInputAndOutputPorts(){
        for (Port port : ports){
            if (port.getIoType().equals(IOType.INPUT)){
                inputPort = port;
            }
            else{
                outputPort = port;
            }
        }
    }

}
