package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import utils.SoundManager;

public class LevelSelectionFrame extends JFrame implements ActionListener {

    private JButton level1Button;
    private JButton level2Button;
    private JButton backButton;
    private JFrame parentMenu;

    public LevelSelectionFrame(JFrame parentMenu) {
        this.parentMenu = parentMenu;
        setTitle("Select Level");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parentMenu);
        setLayout(new GridLayout(3, 1, 10, 10));

        level1Button = new JButton("Level 1");
        level2Button = new JButton("Level 2");
        backButton = new JButton("Back to Main Menu");

        level1Button.addActionListener(this);
        level2Button.addActionListener(this);
        backButton.addActionListener(this);

        add(createButtonPanel(level1Button));
        add(createButtonPanel(level2Button));
        add(createButtonPanel(backButton));


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (parentMenu != null && !parentMenu.isVisible()) {
                    parentMenu.setVisible(true);
                }
            }
        });
    }

    private JPanel createButtonPanel(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        button.setPreferredSize(new Dimension(200, 40));
        panel.add(button);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == level1Button) {
            SoundManager.stopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
            JFrame mainMenuRef = this.parentMenu;
            SwingUtilities.invokeLater(() -> {
                Level1Frame level1 = new Level1Frame(mainMenuRef);
                level1.setVisible(true);
            });
            this.dispose();
        } else if (e.getSource() == level2Button) {
            SoundManager.stopSound(SoundManager.SoundEffect.BACKGROUND_MUSIC);
            JFrame mainMenuRef = this.parentMenu;
            SwingUtilities.invokeLater(() -> {
                Level2Frame level2 = new Level2Frame(mainMenuRef);
                level2.setVisible(true);
            });
            this.dispose();
        } else if (e.getSource() == backButton) {
            this.dispose();
            if (parentMenu != null) {
                parentMenu.setVisible(true);
            }
        }
    }
} 