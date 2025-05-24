package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.Point;

public class NetworkModel {
    private List<NetworkSystem> systems;
    private List<Wire> wires;
    private List<Packet> activePackets; // Packets currently in simulation (on wire or in non-source storage ready to move)
    private List<Packet> deliveredPackets;
    private List<Packet> lostPackets;
    private int playerCoins; // To store player's coins
    
    // Wire length limit system
    private double wireLengthLimit;
    private double currentWireLength;

    public NetworkModel() {
        this.systems = new ArrayList<>();
        this.wires = new ArrayList<>();
        this.activePackets = new CopyOnWriteArrayList<>(); // For thread-safe operations during game loop
        this.deliveredPackets = new ArrayList<>();
        this.lostPackets = new ArrayList<>();
        this.playerCoins = 0; // Initialize coins to 0
        this.wireLengthLimit = 1000.0; // Default wire length limit
        this.currentWireLength = 0.0;
    }

    public void addSystem(NetworkSystem system) {
        this.systems.add(system);
        // Initial packets for SourceNetworkSystem are managed by its own senderStorage
        // They are added to 'activePackets' when released onto a wire.
        system.updateIndicatorState();
    }

    public void addWire(Wire wire) {
        this.wires.add(wire);
        this.currentWireLength += wire.getLength(); // Track wire length
        
        // Ensure ports on the wire know about the wire and update system indicators
        Port port1 = wire.getSourcePort(); // Assuming Wire interface has these
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

    // Method to add a packet to the list of actively simulated packets
    public void addPacketToActiveList(Packet packet) {
        if (!this.activePackets.contains(packet)) {
            this.activePackets.add(packet);
        }
    }

    // This method was adding all packets from source storage prematurely
    // public void addPacket(Packet packet) { this.activePackets.add(packet); }

    public List<NetworkSystem> getSystems() { return new ArrayList<>(systems); }
    public List<Wire> getWires() { return new ArrayList<>(wires); }
    public List<Packet> getPackets() { return activePackets; } // Get active packets for drawing and updates

    public List<Packet> getDeliveredPackets() { return new ArrayList<>(deliveredPackets); }
    public List<Packet> getLostPackets() { return new ArrayList<>(lostPackets); }
    public int getDeliveredCount() { return deliveredPackets.size(); }
    public int getLostCount() { return lostPackets.size(); }
    public int getPlayerCoins() { return playerCoins; } // Getter for playerCoins
    public void setPlayerCoins(int coins) { this.playerCoins = coins; } // Setter for playerCoins

    // Wire length limit methods
    public double getWireLengthLimit() { return wireLengthLimit; }
    public void setWireLengthLimit(double limit) { this.wireLengthLimit = limit; }
    public double getCurrentWireLength() { return currentWireLength; }
    public double getRemainingWireLength() { return wireLengthLimit - currentWireLength; }
    public double getWireUsagePercentage() { return (currentWireLength / wireLengthLimit) * 100.0; }
    
    // Check if a wire of given length can be added
    public boolean canAddWire(double wireLength) {
        return (currentWireLength + wireLength) <= wireLengthLimit;
    }
    
    // Calculate wire length between two points (for temporary wire validation)
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
            // Award coins based on packet shape
            if (packet.getShape() != null) {
                this.playerCoins += packet.getShape().getCoinValue();
            }
            // System.out.println("Packet " + packet.getId() + " DELIVERED. Coins: " + playerCoins);
        } else {
            // System.out.println("Packet " + packet.getId() + " was already processed or not found for delivery.");
        }
    }

    public void addLostPacket(Packet packet) {
        if (activePackets.remove(packet)) {
            lostPackets.add(packet);
            //System.out.println("Packet " + packet.getId() + " officially LOST. Total lost: " + lostPackets.size());
        } else if (systems.stream().anyMatch(s -> s instanceof SourceNetworkSystem && ((SourceNetworkSystem)s).getSenderStorage().contains(packet))) {
            systems.forEach(s -> {
                if (s instanceof SourceNetworkSystem) {
                    ((SourceNetworkSystem)s).getSenderStorage().remove(packet);
                }
            });
            lostPackets.add(packet);
            // System.out.println("Packet " + packet.getId() + " from source storage LOST. Total lost: " + lostPackets.size());
        }
    }

    public void removeWire(Wire wire) {
        if (wire != null && this.wires.remove(wire)) {
            this.currentWireLength -= wire.getLength(); // Return wire length to available pool
            
            Port port1 = wire.getSourcePort();
            Port port2 = wire.getDestinationPort();

            if (port1 != null) {
                port1.setConnectedWire(null);
                port1.setInUse(false); // Free the port
                if (port1.getNetworkSystem() != null) port1.getNetworkSystem().updateIndicatorState();
            }
            if (port2 != null) {
                port2.setConnectedWire(null);
                port2.setInUse(false); // Free the port
                if (port2.getNetworkSystem() != null) port2.getNetworkSystem().updateIndicatorState();
            }
            // Remove packets that were on this specific wire and mark them as LOST
            activePackets.removeIf(p -> {
                if (p.getCurrentWire() == wire) {
                    p.setState(PacketState.LOST);
                    addLostPacket(p); // Use the method to correctly move it
                    return true; // Remove from active list
                }
                return false;
            });
            // System.out.println("Wire removed. Associated packets marked LOST.");
        }
    }

    public void resetSimulation() {
        activePackets.clear();
        deliveredPackets.clear();
        lostPackets.clear();
        playerCoins = 0; // Reset coins on simulation reset

        for(NetworkSystem ns : systems) {
            ns.lastPacketReleaseTimeMillis = 0;
            for(Port p : ns.getAllPorts()) {
                p.setInUse(false);
            }
            if (ns instanceof SourceNetworkSystem) {
                ((SourceNetworkSystem) ns).getSenderStorage().clear();
                // TODO: Potentially re-initialize with default packets if needed
            } else if (ns instanceof NonSourceNetworkSystem) {
                ((NonSourceNetworkSystem) ns).getStorage().clear();
            }
        }
        //System.out.println("NetworkModel: Simulation reset.");
    }
}