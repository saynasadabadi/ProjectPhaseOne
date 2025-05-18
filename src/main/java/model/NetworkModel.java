package model;

import java.util.ArrayList;
import java.util.List;

public class NetworkModel {
    private List<NetworkSystem> systems;
    private List<Wire> wires;
    private List<Packet> packets;

    public NetworkModel() {
        this.systems = new ArrayList<>();
        this.wires = new ArrayList<>();
        this.packets = new ArrayList<>();
    }

    public void addSystem(NetworkSystem system) {
        this.systems.add(system);
        if (system instanceof SourceNetworkSystem) {
            this.packets.addAll(((SourceNetworkSystem) system).getSenderStorage());
        }
        system.updateIndicatorState();
    }

    public void addWire(Wire wire) {
        this.wires.add(wire);
        if (wire.getPorts() != null) {
            for (Port p : wire.getPorts()) {
                if (p.getNetworkSystem() != null) {
                    p.setConnectedWire(wire);
                    p.getNetworkSystem().updateIndicatorState();
                }
            }
        }
    }

    public void addPacket(Packet packet) {
        this.packets.add(packet);
    }

    public List<NetworkSystem> getSystems() { return new ArrayList<>(systems); }
    public List<Wire> getWires() { return new ArrayList<>(wires); }
    public List<Packet> getPackets() { return new ArrayList<>(packets); }

    public void removeWire(Wire wire) {
        if (wire != null) {
            List<NetworkSystem> systemsToUpdate = new ArrayList<>();
            for (Port p : wire.getPorts()) {
                if (p.getNetworkSystem() != null) {
                    if (!systemsToUpdate.contains(p.getNetworkSystem())) {
                        systemsToUpdate.add(p.getNetworkSystem());
                    }
                }
                if (p.getConnectedWire() == wire) {
                    p.setConnectedWire(null);
                }
            }
            this.wires.remove(wire);
            for (NetworkSystem sys : systemsToUpdate) {
                sys.updateIndicatorState();
            }
        }
    }
}