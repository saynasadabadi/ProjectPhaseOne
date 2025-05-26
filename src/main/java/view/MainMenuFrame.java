package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import utils.SoundManager;

public class MainMenuFrame extends JFrame implements ActionListener {

    private JButton level1Button;
    private JButton level2Button;
    private JButton exitButton;

    public MainMenuFrame() {
        setTitle("Main Menu - Network Simulator");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 1, 10, 10)); // 3 rows, 1 col, with gaps

        level1Button = new JButton("Level 1");
        level2Button = new JButton("Level 2");
        exitButton = new JButton("Exit");

        level1Button.addActionListener(this);
        level2Button.addActionListener(this);
        exitButton.addActionListener(this);
        
        // Add some padding around the buttons
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel1.add(level1Button);
        add(panel1);

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel2.add(level2Button);
        add(panel2);

        JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel3.add(exitButton);
        add(panel3);

        SoundManager.loopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == level1Button) {
            SoundManager.stopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
            // Launch Level 1 Frame
            SwingUtilities.invokeLater(() -> {
                Level1Frame level1 = new Level1Frame();
                level1.setVisible(true);
            });
        } else if (e.getSource() == level2Button) {
            SoundManager.stopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
            // Launch Level 2 Frame
            SwingUtilities.invokeLater(() -> {
                Level2Frame level2 = new Level2Frame();
                level2.setVisible(true);
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