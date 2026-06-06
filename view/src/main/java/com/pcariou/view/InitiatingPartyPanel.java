package com.pcariou.view;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;

public class InitiatingPartyPanel extends JPanel {

    private JTextField nameField = new JTextField();
    private JTextField siretField = new JTextField();

    private JLabel nameErrorLabel = new JLabel();
    private JLabel siretErrorLabel = new JLabel();

    public InitiatingPartyPanel() {
        setBorder(BorderFactory.createTitledBorder("Initiating Party"));
        setLayout(new GridBagLayout());

        configureErrorLabel(nameErrorLabel);
        configureErrorLabel(siretErrorLabel);

        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();
        GridBagConstraints error = errorConstraints();

        label.gridy = 0; add(new JLabel("Name:"),  label);
        field.gridy = 0; add(nameField,             field);
        error.gridy = 1; add(nameErrorLabel,         error);

        label.gridy = 2; add(new JLabel("SIRET:"), label);
        field.gridy = 2; add(siretField,            field);
        error.gridy = 3; add(siretErrorLabel,        error);
    }

    // ── Layout helpers (mirrors DebtorPanel exactly) ──────────────────────

    private static GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 0;
        c.weightx = 0;
        c.fill    = GridBagConstraints.NONE;
        c.anchor  = GridBagConstraints.LINE_END;
        c.insets  = new Insets(6, 8, 0, 6);
        c.ipadx   = DebtorPanel.LABEL_WIDTH;
        return c;
    }

    private static GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 1;
        c.weightx = 1.0;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.LINE_START;
        c.insets  = new Insets(6, 0, 0, 8);
        return c;
    }

    private static GridBagConstraints errorConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 1;
        c.weightx = 1.0;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.LINE_START;
        c.insets  = new Insets(0, 0, 4, 8);
        return c;
    }

    private void configureErrorLabel(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(UIManager.getColor("Actions.Red") != null
                ? UIManager.getColor("Actions.Red")
                : new Color(0xCC0000));
        label.setVisible(false);
    }

    private void applyError(JTextField field, JLabel errorLabel, String message) {
        field.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        field.repaint();
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError(JTextField field, JLabel errorLabel) {
        field.putClientProperty(FlatClientProperties.OUTLINE, null);
        field.repaint();
        errorLabel.setVisible(false);
    }

    public void setNameError(String message)  { applyError(nameField, nameErrorLabel, message); }
    public void clearNameError()              { clearError(nameField, nameErrorLabel); }

    public void setSiretError(String message) { applyError(siretField, siretErrorLabel, message); }
    public void clearSiretError()             { clearError(siretField, siretErrorLabel); }

    public void clearAllErrors() {
        clearNameError();
        clearSiretError();
    }

    public String getName()  { return nameField.getText(); }
    public String getSiret() { return siretField.getText(); }

    public void setName(String name)   { nameField.setText(name); }
    public void setSiret(String siret) { siretField.setText(siret); }
}