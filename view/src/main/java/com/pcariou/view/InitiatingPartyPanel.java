package com.pcariou.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.model.InitiatingPartyIdentifier;

import javax.swing.*;
import java.awt.*;

/**
 * Initiating party fields: the name and an optional organisation identifier
 * ({@code InitgPty/Id/OrgId/Othr}) with an optional scheme (ISO code or
 * proprietary name) and issuer.
 */
public class InitiatingPartyPanel extends AbstractSettingsPanel {

    static final String SCHEME_NONE = "None";
    static final String SCHEME_CODE = "ISO code";
    static final String SCHEME_PROPRIETARY = "Proprietary";

    private final JTextField nameField = new JTextField();
    private final JTextField identifierField = new JTextField();
    private final JComboBox<String> schemeTypeBox =
            new JComboBox<>(new String[] { SCHEME_NONE, SCHEME_CODE, SCHEME_PROPRIETARY });
    private final JTextField schemeField = new JTextField();
    private final JTextField issuerField = new JTextField();

    private final JLabel nameErrorLabel = new JLabel();
    private final JLabel identifierErrorLabel = new JLabel();

    public InitiatingPartyPanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        configureErrorLabel(nameErrorLabel);
        configureErrorLabel(identifierErrorLabel);

        GridBagConstraints label = labelConstraints();
        GridBagConstraints field = fieldConstraints();
        GridBagConstraints error = errorConstraints();

        label.gridy = 0; add(new JLabel("Name:"), label);
        field.gridy = 0; add(nameField, field);
        error.gridy = 1; add(nameErrorLabel, error);

        identifierField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Optional, e.g. SIRET or bank-assigned ID");
        identifierField.setToolTipText("Organisation identifier of the party submitting the file "
                + "(Group Header InitgPty/Id). Leave blank if your bank does not require one.");
        label.gridy = 2; add(new JLabel("Identifier:"), label);
        field.gridy = 2; add(identifierField, field);

        schemeTypeBox.setToolTipText("Optional scheme of the identifier: an ISO code "
                + "(e.g. BANK, CUST, TXID, SRET) or a proprietary scheme name.");
        schemeTypeBox.addActionListener(e -> updateSchemeField());
        JPanel schemeRow = new JPanel(new BorderLayout(6, 0));
        schemeRow.setOpaque(false);
        schemeRow.add(schemeTypeBox, BorderLayout.WEST);
        schemeRow.add(schemeField, BorderLayout.CENTER);
        label.gridy = 3; add(new JLabel("Scheme:"), label);
        field.gridy = 3; add(schemeRow, field);

        issuerField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Optional");
        issuerField.setToolTipText("Optional issuer of the identifier.");
        label.gridy = 4; add(new JLabel("Issuer:"), label);
        field.gridy = 4; add(issuerField, field);

        error.gridy = 5; add(identifierErrorLabel, error);
        updateSchemeField();
    }

    private void updateSchemeField() {
        Object type = schemeTypeBox.getSelectedItem();
        schemeField.setEnabled(!SCHEME_NONE.equals(type));
        if (SCHEME_NONE.equals(type)) {
            schemeField.setText("");
        }
        schemeField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                SCHEME_CODE.equals(type) ? "e.g. BANK, CUST, SRET"
                        : SCHEME_PROPRIETARY.equals(type) ? "Scheme name" : null);
        schemeField.repaint();
    }

    public void setNameError(String message)  { applyError(nameField, nameErrorLabel, message); }
    public void clearNameError()              { clearError(nameField, nameErrorLabel); }

    public void setIdentifierError(String message) {
        applyError(identifierField, identifierErrorLabel, message);
    }
    public void clearIdentifierError() { clearError(identifierField, identifierErrorLabel); }

    public void clearAllErrors() {
        clearNameError();
        clearIdentifierError();
    }

    public String getName()  { return nameField.getText(); }
    public void setName(String name)   { nameField.setText(name); }

    /** The entered identifier, or {@code null} when every identifier field is blank. */
    public InitiatingPartyIdentifier getIdentifier() {
        Object type = schemeTypeBox.getSelectedItem();
        String scheme = schemeField.getText();
        return InitiatingPartyIdentifier.of(identifierField.getText(),
                SCHEME_CODE.equals(type) ? scheme : null,
                SCHEME_PROPRIETARY.equals(type) ? scheme : null,
                issuerField.getText());
    }

    public void setIdentifier(InitiatingPartyIdentifier identifier) {
        identifierField.setText(identifier == null || identifier.getId() == null ? "" : identifier.getId());
        issuerField.setText(identifier == null || identifier.getIssuer() == null ? "" : identifier.getIssuer());
        if (identifier != null && identifier.getSchemeCode() != null) {
            schemeTypeBox.setSelectedItem(SCHEME_CODE);
            schemeField.setText(identifier.getSchemeCode());
        } else if (identifier != null && identifier.getSchemeProprietary() != null) {
            schemeTypeBox.setSelectedItem(SCHEME_PROPRIETARY);
            schemeField.setText(identifier.getSchemeProprietary());
        } else {
            schemeTypeBox.setSelectedItem(SCHEME_NONE);
        }
    }
}