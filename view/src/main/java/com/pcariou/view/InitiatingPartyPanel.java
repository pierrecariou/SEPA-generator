package com.pcariou.view;

import javax.swing.*;
import java.awt.*;

public class InitiatingPartyPanel extends AbstractSettingsPanel {

    private JTextField nameField = new JTextField();
    private JTextField siretField = new JTextField();

    private JLabel nameErrorLabel = new JLabel();
    private JLabel siretErrorLabel = new JLabel();

    public InitiatingPartyPanel() {
        setOpaque(false);
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