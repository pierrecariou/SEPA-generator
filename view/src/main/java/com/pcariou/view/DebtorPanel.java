package com.pcariou.view;

import javax.swing.*;
import java.awt.*;

public class DebtorPanel extends JPanel {

    private JTextField nameField = new JTextField();
    private JTextField ibanField = new JTextField();
    private JTextField bicField = new JTextField();

    public DebtorPanel() {
        setBorder(BorderFactory.createTitledBorder("Debtor Information"));
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
        add(new JLabel("IBAN:"), gbc);
        gbc.gridx = 1;
        add(ibanField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("BIC:"), gbc);
        gbc.gridx = 1;
        add(bicField, gbc);

        designer();
    }

    private void designer() {
//        setBackground(StyleUtils.BACKGROUND_COLOR);

        // styling removed (StyleUtils is minimal now)
    }

    public String getName() { return nameField.getText(); }
    public String getIban() { return ibanField.getText(); }
    public String getBic() { return bicField.getText(); }

    public void setName(String name) { nameField.setText(name); }
    public void setIban(String iban) { ibanField.setText(iban); }
    public void setBic(String bic) { bicField.setText(bic); }
}