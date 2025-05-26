package view;

import model.GameModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShopDialog extends JDialog {
    private GameModel gameModel;
    private JLabel coinsLabel;

    private static final String ATAR_NAME = "O' Atar";
    private static final int ATAR_COST = 3;
    private static final String AIRYAMAN_NAME = "O' Airyaman";
    private static final int AIRYAMAN_COST = 4;
    private static final String ANAHITA_NAME = "O' Anahita";
    private static final int ANAHITA_COST = 5;


    public ShopDialog(JFrame parent, GameModel gameModel) {
        super(parent, "فروشگاه (Shop)", true); // Modal dialog
        this.gameModel = gameModel;

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Coins display
        coinsLabel = new JLabel("Coins: " + gameModel.getCoins());
        coinsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(coinsLabel, BorderLayout.NORTH);

        // Power-ups panel
        JPanel powerUpsPanel = new JPanel(new GridLayout(0, 1, 10, 10)); // Single column, spacing
        powerUpsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton atarButton = createPowerUpButton(ATAR_NAME, ATAR_COST, "Disable Impact Waves for 10s");
        JButton airyamanButton = createPowerUpButton(AIRYAMAN_NAME, AIRYAMAN_COST, "Disable Packet Collisions for 5s");
        JButton anahitaButton = createPowerUpButton(ANAHITA_NAME, ANAHITA_COST, "Set noise of all current packets to zero");

        powerUpsPanel.add(atarButton);
        powerUpsPanel.add(airyamanButton);
        powerUpsPanel.add(anahitaButton);

        add(powerUpsPanel, BorderLayout.CENTER);

        // Close button
        JButton closeButton = new JButton("Close & Resume Game");
        closeButton.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        updateButtonStates(); // Initial check for button enabled state
    }

    private JButton createPowerUpButton(String name, int cost, String description) {
        JButton button = new JButton(String.format("<html>%s (%d Coins)<br><small>%s</small></html>", name, cost, description));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameModel.getCoins() >= cost) {
                    gameModel.purchasePowerUp(name, cost); // Pass name and cost
                    coinsLabel.setText("Coins: " + gameModel.getCoins());
                    System.out.println(name + " bought!"); // Placeholder action
                    updateButtonStates(); // Update button states after purchase
                    // For now, no actual effect is implemented.
                } else {
                    JOptionPane.showMessageDialog(ShopDialog.this,
                            "Not enough coins to purchase " + name + ".",
                            "Insufficient Coins",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        return button;
    }

    private void updateButtonStates() {
        // This method can be expanded later if power-ups have limited uses or other conditions
        // For now, it just ensures buttons are disabled if the player can't afford them.
        Component[] components = ((JPanel)getContentPane().getComponent(1)).getComponents(); // Assuming powerUpsPanel is the second component
        for (Component component : components) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                // A bit of a hack to get the cost from the button text.
                // Proper way would be to store cost with button or have specific update methods.
                if (button.getText().contains(ATAR_NAME)) {
                    button.setEnabled(gameModel.getCoins() >= ATAR_COST);
                } else if (button.getText().contains(AIRYAMAN_NAME)) {
                    button.setEnabled(gameModel.getCoins() >= AIRYAMAN_COST);
                } else if (button.getText().contains(ANAHITA_NAME)) {
                    button.setEnabled(gameModel.getCoins() >= ANAHITA_COST);
                }
            }
        }
    }
} 