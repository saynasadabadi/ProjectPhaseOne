package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.Point;

public class NetworkModel {
    private List<NetworkSystem> systems;
    private List<Wire> wires;
    private List<Packet> activePackets;
    private List<Packet> deliveredPackets;
    private List<Packet> lostPackets;
    private int playerCoins;
    

    private double wireLengthLimit;
    private double currentWireLength;

    public NetworkModel() {
        this.systems = new ArrayList<>();
        this.wires = new ArrayList<>();
        this.activePackets = new CopyOnWriteArrayList<>();
        this.deliveredPackets = new ArrayList<>();
        this.lostPackets = new ArrayList<>();
        this.playerCoins = 0;
        this.wireLengthLimit = 2000.0;
        this.currentWireLength = 0.0;
    }

    public void addSystem(NetworkSystem system) {
        this.systems.add(system);


        system.updateIndicatorState();
    }

    public void addWire(Wire wire) {
        this.wires.add(wire);
        this.currentWireLength += wire.getLength();
        

        Port port1 = wire.getSourcePort();
        Port port2 = wire.getDestinationPort();

        if (port1 != null) {
            port1.setConnectedWire(wire);
            if (port1.getNetworkSystem() != null) {
                port1.getNetworkSystem().updateIndicatorState();
            }
        }
        if (port2 != null) {
            port2.setConnectedWire(wire);
            if (port2.getNetworkSystem() != null) {
                port2.getNetworkSystem().updateIndicatorState();
            }
        }
    }


    public void addPacketToActiveList(Packet packet) {
        if (!this.activePackets.contains(packet)) {
            this.activePackets.add(packet);
        }
    }




    public List<NetworkSystem> getSystems() { return new ArrayList<>(systems); }
    public List<Wire> getWires() { return new ArrayList<>(wires); }
    public List<Packet> getPackets() { return activePackets; }

    public List<Packet> getDeliveredPackets() { return new ArrayList<>(deliveredPackets); }
    public List<Packet> getLostPackets() { return new ArrayList<>(lostPackets); }
    public int getDeliveredCount() { return deliveredPackets.size(); }
    public int getLostCount() { return lostPackets.size(); }
    public int getPlayerCoins() { return playerCoins; }
    public void setPlayerCoins(int coins) { this.playerCoins = coins; }


    public double getWireLengthLimit() { return wireLengthLimit; }
    public void setWireLengthLimit(double limit) { this.wireLengthLimit = limit; }
    public double getCurrentWireLength() { return currentWireLength; }
    public double getRemainingWireLength() { return wireLengthLimit - currentWireLength; }
    public double getWireUsagePercentage() { return (currentWireLength / wireLengthLimit) * 100.0; }
    

    public boolean canAddWire(double wireLength) {
        return (currentWireLength + wireLength) <= wireLengthLimit;
    }
    

    public static double calculateWireLength(Point p1, Point p2) {
        return p1.distance(p2);
    }

    public void addDeliveredPacket(Packet packet) {
        boolean removedFromActive = activePackets.remove(packet);
        boolean removedFromSourceStorage = false;

        if (!removedFromActive) {
            for (NetworkSystem s : systems) {
                if (s instanceof SourceNetworkSystem && ((SourceNetworkSystem)s).getSenderStorage().remove(packet)) {
                    removedFromSourceStorage = true;
                    break;
                }
            }
        }

        if (removedFromActive || removedFromSourceStorage) {
            deliveredPackets.add(packet);

            if (packet.getShape() != null) {
                this.playerCoins += packet.getShape().getCoinValue();
            }

        } else {

        }
    }

    public void addLostPacket(Packet packet) {
        if (activePackets.remove(packet)) {
            lostPackets.add(packet);

        } else if (systems.stream().anyMatch(s -> s instanceof SourceNetworkSystem && ((SourceNetworkSystem)s).getSenderStorage().contains(packet))) {
            systems.forEach(s -> {
                if (s instanceof SourceNetworkSystem) {
                    ((SourceNetworkSystem)s).getSenderStorage().remove(packet);
                }
            });
            lostPackets.add(packet);

        }
    }

    public void removeWire(Wire wire) {
        if (wire != null && this.wires.remove(wire)) {
            this.currentWireLength -= wire.getLength();
            
            Port port1 = wire.getSourcePort();
            Port port2 = wire.getDestinationPort();

            if (port1 != null) {
                port1.setConnectedWire(null);
                port1.setInUse(false);
                if (port1.getNetworkSystem() != null) port1.getNetworkSystem().updateIndicatorState();
            }
            if (port2 != null) {
                port2.setConnectedWire(null);
                port2.setInUse(false);
                if (port2.getNetworkSystem() != null) port2.getNetworkSystem().updateIndicatorState();
            }

            activePackets.removeIf(p -> {
                if (p.getCurrentWire() == wire) {
                    p.setState(PacketState.LOST);
                    addLostPacket(p);
                    return true;
                }
                return false;
            });

        }
    }

    public void resetSimulation() {
        activePackets.clear();
        deliveredPackets.clear();
        lostPackets.clear();
        playerCoins = 0;

        for(NetworkSystem ns : systems) {
            ns.lastPacketReleaseTimeMillis = 0;
            for(Port p : ns.getAllPorts()) {
                p.setInUse(false);
            }


            if (ns instanceof NonSourceNetworkSystem) {
                ((NonSourceNetworkSystem) ns).getStorage().clear();
            }
        }
        System.out.println("NetworkModel: Simulation reset (preserving initial packets).");
    }
}