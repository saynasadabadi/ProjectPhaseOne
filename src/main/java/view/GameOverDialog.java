package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import utils.SoundManager;


public class GameOverDialog extends JDialog {

    private boolean returnToMenuClicked = false;
    private controller.NetworkController controller;
    
    public GameOverDialog(Frame parent, double packetLossPercentage, int deliveredPackets, int lostPackets, int coins, controller.NetworkController controller) {
        super(parent, "Game Over", true);
        this.controller = controller;
        
        setupDialog(packetLossPercentage, deliveredPackets, lostPackets, coins);
    }
    
    private void setupDialog(double lossPercentage, int delivered, int lost, int coins) {
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.RED.darker());
        JLabel titleLabel = new JLabel("GAME OVER", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        

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
        coinsLabel.setForeground(new Color(255, 215, 0));
        

        int score = Math.max(0, delivered * 10 - lost * 5 + coins);
        JLabel scoreLabel = new JLabel(String.format("Final Score: %d", score), JLabel.CENTER);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        scoreLabel.setForeground(Color.BLUE);
        
        statsPanel.add(reasonLabel);
        statsPanel.add(deliveredLabel);
        statsPanel.add(lostLabel);
        statsPanel.add(coinsLabel);
        statsPanel.add(scoreLabel);
        

        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton restartButton = new JButton("Try Again");
        restartButton.setPreferredSize(new Dimension(120, 35));
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

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
                dispose();
            }
        });
        
        buttonPanel.add(restartButton);
        buttonPanel.add(menuButton);
        

        add(titlePanel, BorderLayout.NORTH);
        add(statsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        

        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }
    
    public boolean isRestartRequested() {

        return false;
    }
    
    public boolean isReturnToMenuClicked() {
        return returnToMenuClicked;
    }
    
    
    public void showDialog() {

        setVisible(true);
    }
} 