package com.pcariou.view;

import javax.swing.*;
import java.awt.*;

public class PrefillPanel extends JPanel {

    private JTextField inputPathField = new JTextField();
    private JTextField outputPathField = new JTextField();

    public PrefillPanel() {
        setBorder(BorderFactory.createTitledBorder("Default File Paths"));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Default Input Path:"), gbc);
        gbc.gridx = 1;
        add(inputPathField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Default Output Path:"), gbc);
        gbc.gridx = 1;
        add(outputPathField, gbc);

        designer();
    }

    private void designer() {
//        setBackground(StyleUtils.BACKGROUND_COLOR);

        // styling removed (StyleUtils is minimal now)
    }

    public String getInputPath() { return inputPathField.getText(); }
    public String getOutputPath() { return outputPathField.getText(); }

    public void setInputPath(String inputPath) { inputPathField.setText(inputPath); }
    public void setOutputPath(String outputPath) { outputPathField.setText(outputPath); }
}