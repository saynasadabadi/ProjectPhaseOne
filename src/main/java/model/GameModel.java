package model;

import java.util.List; // Required for getSystems() in NetworkModel

public class GameModel {
    private NetworkModel networkModel; // Changed from networkmodel to networkModel for convention
    double MAXIMUM_TOTAL_WIRE_LENGTH = 200;
    double currentTotalWireLength;

    // Constructor to initialize NetworkModel
    public GameModel() {
        this.networkModel = new NetworkModel();
    }

    // Getter for the NetworkModel, needed by View and Controller
    public NetworkModel getNetworkModel() {
        return networkModel;
    }

    // Setter for the NetworkModel, if needed (e.g., for loading a game)
    public void setNetworkModel(NetworkModel networkModel) {
        this.networkModel = networkModel;
    }

    /**
     * Checks if the current network configuration is valid to start the game.
     * For now, it checks if all NetworkSystem indicators are ON.
     * @return true if the network model is valid, false otherwise.
     */
    public boolean isNetworkModelValid() {
        if (networkModel == null || networkModel.getSystems().isEmpty()) {
            System.out.println("GameModel: Network model is null or has no systems. Considered invalid.");
            return false; // Or handle as an error/exception
        }
        for (NetworkSystem system : networkModel.getSystems()) {
            if (system.getIndicatorState() != IndicatorState.ON) {
                System.out.println("GameModel: System '" + system.toString() + "' indicator is not ON. Validation failed.");
                return false; // If any system's indicator is not ON, it's invalid
            }
        }
        System.out.println("GameModel: All system indicators are ON. Network model is valid.");
        return true;
    }

    /**
     * Attempts to start the game.
     * Checks network validity before proceeding.
     */
    public void startTheGame() {
        System.out.println("GameModel: Attempting to start the game...");
        if (isNetworkModelValid()) {
            // --- Placeholder for actual game start logic ---
            System.out.println("GameModel: Network is valid! Starting the game simulation...");
            // Example: Initialize packet movement, start timers, change game state, etc.
            // For now, we'll just print a message.
            // You might want to notify the view or controller that the game has started.
        } else {
            System.out.println("GameModel: Network is invalid. Cannot start the game.");
            // Optionally, provide feedback to the user through the UI
            // e.g., show a dialog message.
        }
    }
}