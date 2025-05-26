package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import utils.SoundManager;

/**
 * Dialog displayed when game over condition is met (packet loss > 50%)
 */
public class GameOverDialog extends JDialog {
    // private boolean restartRequested = false; // Not strictly needed if actions are direct
    private boolean returnToMenuClicked = false; // Flag for panel to check
    private controller.NetworkController controller; // Store controller reference
    
    public GameOverDialog(Frame parent, double packetLossPercentage, int deliveredPackets, int lostPackets, int coins, controller.NetworkController controller) {
        super(parent, "Game Over", true); // Make it MODAL to simplify flow
        this.controller = controller; // Store controller
        
        setupDialog(packetLossPercentage, deliveredPackets, lostPackets, coins);
    }
    
    private void setupDialog(double lossPercentage, int delivered, int lost, int coins) {
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Allow user to close with 'X'
        
        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.RED.darker());
        JLabel titleLabel = new JLabel("GAME OVER", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel reasonLabel = new JLabel(String.format("Packet Loss: %.1f%% (> 50.0%%)", lossPercentage), JLabel.CENTER);
        reasonLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        reasonLabel.setForeground(Color.RED);
        
        JLabel deliveredLabel = new JLabel(String.format("Packets Delivered: %d", delivered), JLabel.CENTER);
        deliveredLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JLabel lostLabel = new JLabel(String.format("Packets Lost: %d", lost), JLabel.CENTER);
        lostLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JLabel coinsLabel = new JLabel(String.format("Coins Earned: %d", coins), JLabel.CENTER);
        coinsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        coinsLabel.setForeground(new Color(255, 215, 0)); // Gold color
        
        // Calculate a simple score
        int score = Math.max(0, delivered * 10 - lost * 5 + coins);
        JLabel scoreLabel = new JLabel(String.format("Final Score: %d", score), JLabel.CENTER);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        scoreLabel.setForeground(Color.BLUE);
        
        statsPanel.add(reasonLabel);
        statsPanel.add(deliveredLabel);
        statsPanel.add(lostLabel);
        statsPanel.add(coinsLabel);
        statsPanel.add(scoreLabel);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton restartButton = new JButton("Try Again");
        restartButton.setPreferredSize(new Dimension(120, 35));
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Action is now handled by NetworkPanel after dialog is shown
                if (GameOverDialog.this.controller != null) {
                    GameOverDialog.this.controller.restartLevel();
                }
                dispose();
            }
        });
        
        JButton menuButton = new JButton("Return to Menu");
        menuButton.setPreferredSize(new Dimension(120, 35));
        menuButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnToMenuClicked = true;
                dispose(); // Dispose the GameOverDialog itself
            }
        });
        
        buttonPanel.add(restartButton);
        buttonPanel.add(menuButton);
        
        // Add panels to dialog
        add(titlePanel, BorderLayout.NORTH);
        add(statsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Configure dialog
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }
    
    public boolean isRestartRequested() {
        // This can be inferred by controller action if dialog is modal
        return false; // Or could be set if Try Again is clicked, if needed elsewhere
    }
    
    public boolean isReturnToMenuClicked() {
        return returnToMenuClicked;
    }
    
    /**
     * Shows the dialog (modal)
     */
    public void showDialog() { // Return type void now
        // For modal dialog, setVisible(true) will block until dialog is disposed.
        setVisible(true);
    }
} 