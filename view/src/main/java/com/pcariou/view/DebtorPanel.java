package com.pcariou.view;

import javax.swing.*;
import java.awt.*;

public class DebtorPanel extends AbstractSettingsPanel {

    private JTextField nameField = new JTextField();
    private JTextField ibanField = new JTextField();
    private JTextField bicField = new JTextField();

    private JLabel nameErrorLabel = new JLabel();
    private JLabel ibanErrorLabel = new JLabel();
    private JLabel bicErrorLabel = new JLabel();

    public DebtorPanel() {
        setOpaque(false);
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