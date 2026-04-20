package com.pcariou.view;

import javax.swing.*;
import java.awt.*;

public class InitiatingPartyPanel extends JPanel {

    private JTextField nameField = new JTextField();
    private JTextField siretField = new JTextField();

    public InitiatingPartyPanel() {
        setBorder(BorderFactory.createTitledBorder("Initiating Party"));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("SIRET:"), gbc);
        gbc.gridx = 1;
        add(siretField, gbc);

        designer();
    }

    private void designer() {
//        setBackground(StyleUtils.BACKGROUND_COLOR);

        // styling removed (StyleUtils is minimal now)
    }


    public String getName() { return nameField.getText(); }
    public String getSiret() { return siretField.getText(); }

    public void setName(String name) { nameField.setText(name); }
    public void setSiret(String siret) { siretField.setText(siret); }
}