package view;

import utils.SoundManager;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {

    private JSlider volumeSlider;

    public SettingsDialog(JFrame parent) {
        super(parent, "Settings", true); // true for modal
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Volume Label
        JLabel volumeLabel = new JLabel("Volume:");
        volumeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Volume Slider
        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, SoundManager.getVolume());
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);

        volumeSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) { // Process final value
                int volume = source.getValue();
                SoundManager.setVolume(volume);
            }
        });

        JPanel contentPanel = new JPanel(new BorderLayout(5,5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        contentPanel.add(volumeLabel, BorderLayout.NORTH);
        contentPanel.add(volumeSlider, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
} 