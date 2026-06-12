package com.pcariou.view;

import javax.swing.*;
import java.awt.*;

public class DebtorPanel extends AbstractSettingsPanel {

    private JTextField nameField = new JTextField();
    private JTextField ibanField = new JTextField();
    private JTextField bicField = new JTextField();

    private JTextField streetField         = new JTextField();
    private JTextField buildingNumberField = new JTextField();
    private JTextField postcodeField       = new JTextField();
    private JTextField townField           = new JTextField();
    private JTextField countryField        = new JTextField();

    private JLabel nameErrorLabel = new JLabel();
    private JLabel ibanErrorLabel = new JLabel();
    private JLabel bicErrorLabel = new JLabel();
    private JLabel addressErrorLabel = new JLabel();

    public DebtorPanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        configureErrorLabel(nameErrorLabel);
        configureErrorLabel(ibanErrorLabel);
        configureErrorLabel(bicErrorLabel);
        configureErrorLabel(addressErrorLabel);

        countryField.setToolTipText("2-letter ISO country code (e.g. FR, DE, NL)");

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

        GridBagConstraints section = sectionConstraints();
        section.gridy = 6;
        add(createAddressSectionLabel(), section);

        label.gridy = 7;  add(new JLabel("Street:"),       label);
        field.gridy = 7;  add(streetField,                  field);

        label.gridy = 8;  add(new JLabel("Building no.:"), label);
        field.gridy = 8;  add(buildingNumberField,          field);

        label.gridy = 9;  add(new JLabel("Postcode:"),     label);
        field.gridy = 9;  add(postcodeField,                field);

        label.gridy = 10; add(new JLabel("Town/City:"),    label);
        field.gridy = 10; add(townField,                    field);

        label.gridy = 11; add(new JLabel("Country:"),      label);
        field.gridy = 11; add(countryField,                 field);
        error.gridy = 12; add(addressErrorLabel,             error);
    }

    private JComponent createAddressSectionLabel() {
        JLabel section = new JLabel("Postal address — optional, used in pain.001.001.09");
        section.setFont(section.getFont().deriveFont(Font.PLAIN, 11f));
        section.setForeground(UIManager.getColor("Label.disabledForeground"));
        return section;
    }

    /** Constraints for a full-width section separator label. */
    private GridBagConstraints sectionConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx     = 0;
        c.gridwidth = 2;
        c.weightx   = 1.0;
        c.fill      = GridBagConstraints.HORIZONTAL;
        c.anchor    = GridBagConstraints.LINE_START;
        c.insets    = new Insets(14, 8, 0, 8);
        return c;
    }

    public void setNameError(String message) { applyError(nameField, nameErrorLabel, message); }
    public void clearNameError()             { clearError(nameField, nameErrorLabel); }

    public void setIbanError(String message) { applyError(ibanField, ibanErrorLabel, message); }
    public void clearIbanError()             { clearError(ibanField, ibanErrorLabel); }

    public void setBicError(String message)  { applyError(bicField, bicErrorLabel, message); }
    public void clearBicError()              { clearError(bicField, bicErrorLabel); }

    /** Marks the town and/or country fields and shows one combined address error. */
    public void setAddressError(String message, boolean markTown, boolean markCountry) {
        if (markTown)    applyError(townField, addressErrorLabel, message);
        if (markCountry) applyError(countryField, addressErrorLabel, message);
        addressErrorLabel.setText(message);
        addressErrorLabel.setVisible(true);
    }

    public void clearAddressError() {
        clearError(townField, addressErrorLabel);
        clearError(countryField, addressErrorLabel);
    }

    public void clearAllErrors() {
        clearNameError();
        clearIbanError();
        clearBicError();
        clearAddressError();
    }

    public String getName() { return nameField.getText(); }
    public String getIban() { return ibanField.getText(); }
    public String getBic()  { return bicField.getText(); }

    public void setName(String name) { nameField.setText(name); }
    public void setIban(String iban) { ibanField.setText(iban); }
    public void setBic(String bic)   { bicField.setText(bic); }

    public String getStreet()         { return streetField.getText(); }
    public String getBuildingNumber() { return buildingNumberField.getText(); }
    public String getPostcode()       { return postcodeField.getText(); }
    public String getTown()           { return townField.getText(); }
    public String getCountry()        { return countryField.getText(); }

    public void setStreet(String street)                 { streetField.setText(street); }
    public void setBuildingNumber(String buildingNumber) { buildingNumberField.setText(buildingNumber); }
    public void setPostcode(String postcode)             { postcodeField.setText(postcode); }
    public void setTown(String town)                     { townField.setText(town); }
    public void setCountry(String country)               { countryField.setText(country); }
}