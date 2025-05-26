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
        super(parent, " (Shop)", true);        this.gameModel = gameModel;

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        coinsLabel = new JLabel("Coins: " + gameModel.getCoins());
        coinsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(coinsLabel, BorderLayout.NORTH);

        JPanel powerUpsPanel = new JPanel(new GridLayout(0, 1, 10, 10));        powerUpsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton atarButton = createPowerUpButton(ATAR_NAME, ATAR_COST, "Disable Impact Waves for 10s");
        JButton airyamanButton = createPowerUpButton(AIRYAMAN_NAME, AIRYAMAN_COST, "Disable Packet Collisions for 5s");
        JButton anahitaButton = createPowerUpButton(ANAHITA_NAME, ANAHITA_COST, "Set noise of all current packets to zero");

        powerUpsPanel.add(atarButton);
        powerUpsPanel.add(airyamanButton);
        powerUpsPanel.add(anahitaButton);

        add(powerUpsPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close & Resume Game");
        closeButton.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        updateButtonStates();    }

    private JButton createPowerUpButton(String name, int cost, String description) {
        JButton button = new JButton(String.format("<html>%s (%d Coins)<br><small>%s</small></html>", name, cost, description));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameModel.getCoins() >= cost) {
                    gameModel.purchasePowerUp(name, cost);                    coinsLabel.setText("Coins: " + gameModel.getCoins());
                    System.out.println(name + " bought!");                    updateButtonStates();                } else {
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
        Component[] components = ((JPanel)getContentPane().getComponent(1)).getComponents();        for (Component component : components) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
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