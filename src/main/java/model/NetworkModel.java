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
    }

    public void addWire(Wire wire) {
        this.wires.add(wire);
    }

    public void addPacket(Packet packet) {
        this.packets.add(packet);
    }

    public List<NetworkSystem> getSystems() { return new ArrayList<>(systems); }
    public List<Wire> getWires() { return new ArrayList<>(wires); }
    public List<Packet> getPackets() { return new ArrayList<>(packets); }

    public void removeWire(Wire wire) {
        if (wire != null) {
            for (Port p : wire.getPorts()) {
                if (p.getConnectedWire() == wire) {
                    p.setConnectedWire(null);
                }
            }
            this.wires.remove(wire);
        }
    }

    public void removeSystem(NetworkSystem system) {
        if (system != null) {
            List<Wire> wiresToRemove = new ArrayList<>();
            for (Wire w : this.wires) {
                for (Port p : w.getPorts()) {
                    if (p.getNetworkSystem() == system) {
                        wiresToRemove.add(w);
                        break;
                    }
                }
            }
            for (Wire wtr : wiresToRemove) {
                removeWire(wtr);
            }

            List<Packet> packetsToRemove = new ArrayList<>();
            for(Packet p : this.packets){
                if(p.getNetworkSystem() == system || (p.getWire() != null && wiresToRemove.contains(p.getWire()))){
                    packetsToRemove.add(p);
                }
            }
            this.packets.removeAll(packetsToRemove);
            this.systems.remove(system);
        }
    }
}