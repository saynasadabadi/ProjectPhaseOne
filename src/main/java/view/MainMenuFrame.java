package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import utils.SoundManager;

public class MainMenuFrame extends JFrame implements ActionListener {

    private JButton playGameButton;
    private JButton settingsButton;
    private JButton exitButton;

    public MainMenuFrame() {
        setTitle("Main Menu - Network Simulator");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 1, 10, 10)); // 3 rows, 1 col, with gaps

        playGameButton = new JButton("Play Game");
        settingsButton = new JButton("Settings");
        exitButton = new JButton("Exit");

        playGameButton.addActionListener(this);
        settingsButton.addActionListener(this);
        exitButton.addActionListener(this);
        
        // Add some padding around the buttons
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel1.add(playGameButton);
        add(panel1);

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel2.add(settingsButton);
        add(panel2);

        JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel3.add(exitButton);
        add(panel3);

        SoundManager.loopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playGameButton) {
            // Open LevelSelectionFrame
            SwingUtilities.invokeLater(() -> {
                LevelSelectionFrame levelSelectionFrame = new LevelSelectionFrame(this);
                levelSelectionFrame.setVisible(true);
                this.setVisible(false);
            });
        } else if (e.getSource() == settingsButton) {
            // Open SettingsFrame
            SwingUtilities.invokeLater(() -> {
                SettingsDialog settingsDialog = new SettingsDialog(this);
                settingsDialog.setVisible(true);
            });
        } else if (e.getSource() == exitButton) {
            SoundManager.stopAllSounds();
            System.exit(0);
        }
    }

    // Main method for the entire application will be here
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            MainMenuFrame menu = new MainMenuFrame();
            menu.setVisible(true);
        });
    }
} 