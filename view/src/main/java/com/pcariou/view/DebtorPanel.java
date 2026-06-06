package com.pcariou.view;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;

public class DebtorPanel extends JPanel {

    private JTextField nameField = new JTextField();
    private JTextField ibanField = new JTextField();
    private JTextField bicField = new JTextField();

    private JLabel nameErrorLabel = new JLabel();
    private JLabel ibanErrorLabel = new JLabel();
    private JLabel bicErrorLabel = new JLabel();

    public DebtorPanel() {
        setBorder(BorderFactory.createTitledBorder("Debtor Information"));
        setLayout(new GridBagLayout());

        configureErrorLabel(nameErrorLabel);
        configureErrorLabel(ibanErrorLabel);
        configureErrorLabel(bicErrorLabel);

        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();
        GridBagConstraints error = errorConstraints();

        label.gridy = 0; add(new JLabel("Name:"),  label);
        field.gridy = 0; add(nameField,             field);
        error.gridy = 1; add(nameErrorLabel,         error);

        label.gridy = 2; add(new JLabel("IBAN:"),  label);
        field.gridy = 2; add(ibanField,             field);
        error.gridy = 3; add(ibanErrorLabel,         error);

        label.gridy = 4; add(new JLabel("BIC:"),   label);
        field.gridy = 4; add(bicField,              field);
        error.gridy = 5; add(bicErrorLabel,          error);
    }

    // ── Layout helpers ────────────────────────────────────────────────────

    static final int LABEL_WIDTH = 70; // shared constant so both panels align

    private static GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx    = 0;
        c.weightx  = 0;
        c.fill     = GridBagConstraints.NONE;
        c.anchor   = GridBagConstraints.LINE_END;
        c.insets   = new Insets(6, 8, 0, 6);
        c.ipadx    = LABEL_WIDTH; // fixed label column width
        return c;
    }

    private static GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx    = 1;
        c.weightx  = 1.0;
        c.fill     = GridBagConstraints.HORIZONTAL;
        c.anchor   = GridBagConstraints.LINE_START;
        c.insets   = new Insets(6, 0, 0, 8);
        return c;
    }

    private static GridBagConstraints errorConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx    = 1;
        c.weightx  = 1.0;
        c.fill     = GridBagConstraints.HORIZONTAL;
        c.anchor   = GridBagConstraints.LINE_START;
        c.insets   = new Insets(0, 0, 4, 8);
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

    public void setNameError(String message) { applyError(nameField, nameErrorLabel, message); }
    public void clearNameError()             { clearError(nameField, nameErrorLabel); }

    public void setIbanError(String message) { applyError(ibanField, ibanErrorLabel, message); }
    public void clearIbanError()             { clearError(ibanField, ibanErrorLabel); }

    public void setBicError(String message)  { applyError(bicField, bicErrorLabel, message); }
    public void clearBicError()              { clearError(bicField, bicErrorLabel); }

    public void clearAllErrors() {
        clearNameError();
        clearIbanError();
        clearBicError();
    }

    public String getName() { return nameField.getText(); }
    public String getIban() { return ibanField.getText(); }
    public String getBic()  { return bicField.getText(); }

    public void setName(String name) { nameField.setText(name); }
    public void setIban(String iban) { ibanField.setText(iban); }
    public void setBic(String bic)   { bicField.setText(bic); }
}