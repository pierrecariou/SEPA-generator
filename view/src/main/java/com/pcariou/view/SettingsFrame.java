package com.pcariou.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pcariou.view.main.MainFrame;

public class SettingsFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(SettingsFrame.class.getName());

    private final DebtorPanel debtorPanel = new DebtorPanel();
    private final InitiatingPartyPanel initiatingPartyPanel = new InitiatingPartyPanel();

    private final File configFile = new File(System.getProperty("user.home"), ".sepa-generator-config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final MainFrame owner;

    public SettingsFrame(MainFrame parent) {
        super("Settings");
        this.owner = parent;
        setSize(450, 500);
        setLocationRelativeTo(parent);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // --- Save Button ---
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveConfig());
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel saveButtonPanel = new JPanel();
        saveButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        saveButtonPanel.add(saveButton);

        // -- Main Panel ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(debtorPanel);
        mainPanel.add(initiatingPartyPanel);

        add(mainPanel, BorderLayout.CENTER);
        add(saveButtonPanel, BorderLayout.SOUTH);

        loadConfig();

        setVisible(true);
    }

    private void loadConfig() {
        ConfigData config = readConfig();
        if (config != null) {
            populateForm(config);
        }
    }

    private ConfigData readConfig() {
        if (!configFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(configFile)) {
            return gson.fromJson(reader, ConfigData.class);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load config from " + configFile.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this, "Failed to load config: " + e.getMessage());
            return null;
        }
    }

    private void populateForm(ConfigData config) {
        boolean hasManagedSettings = false;

        if (config.debtor != null) {
            debtorPanel.setName(defaultValue(config.debtor.name));
            debtorPanel.setIban(defaultValue(config.debtor.iban));
            debtorPanel.setBic(defaultValue(config.debtor.bic));
            hasManagedSettings = true;
        }

        if (config.initiatingParty != null) {
            initiatingPartyPanel.setName(defaultValue(config.initiatingParty.name));
            initiatingPartyPanel.setSiret(defaultValue(config.initiatingParty.siret));
            hasManagedSettings = true;
        }

        if (!hasManagedSettings) {
            JOptionPane.showMessageDialog(this,
                    "Config file does not contain settings managed by this panel: " + configFile.getAbsolutePath(),
                    "Invalid Config", JOptionPane.WARNING_MESSAGE);
        }
    }

    private ConfigData collectFormData() {
        ConfigData config = new ConfigData();
        config.debtor = new Debtor();
        config.debtor.name = debtorPanel.getName();
        config.debtor.iban = debtorPanel.getIban();
        config.debtor.bic = debtorPanel.getBic();

        config.initiatingParty = new InitiatingParty();
        config.initiatingParty.name = initiatingPartyPanel.getName();
        config.initiatingParty.siret = initiatingPartyPanel.getSiret();
        return config;
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private static final String IBAN_FORMAT_PATTERN = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$";
    private static final String BIC_PATTERN         = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$";
    private static final String SIRET_PATTERN       = "^[0-9]{14}$";

    private void saveConfig() {
        ConfigData existingConfig = readConfig();
        ConfigData updatedSettings = collectFormData();

        if (!validateAndMarkFields(updatedSettings)) {
            return;
        }

        debtorPanel.clearAllErrors();
        initiatingPartyPanel.clearAllErrors();

        ConfigData configToSave = existingConfig != null ? existingConfig : new ConfigData();
        configToSave.debtor = updatedSettings.debtor;
        configToSave.initiatingParty = updatedSettings.initiatingParty;

        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(configToSave, writer);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save config to " + configFile.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this, "Failed to save config: " + e.getMessage());
            return;
        }

        owner.refreshStatus();
        JOptionPane.showMessageDialog(this, "Settings saved successfully!");
    }

    private boolean validateAndMarkFields(ConfigData config) {
        boolean valid = true;

        // Debtor name
        if (isBlank(config.debtor.name)) {
            debtorPanel.setNameError("The debtor's name is mandatory");
            valid = false;
        } else {
            debtorPanel.clearNameError();
        }

        // Debtor IBAN — format check + MOD97
        String iban = config.debtor.iban == null ? "" : config.debtor.iban.replaceAll("\\s", "").toUpperCase();
        if (isBlank(iban)) {
            debtorPanel.setIbanError("The IBAN for the debtor is mandatory");
            valid = false;
        } else if (!iban.matches(IBAN_FORMAT_PATTERN)) {
            debtorPanel.setIbanError("IBAN format is invalid (e.g. FR7630006000011234567890189)");
            valid = false;
        } else if (!ibanMod97(iban)) {
            debtorPanel.setIbanError("IBAN checksum is invalid (MOD 97 failed)");
            valid = false;
        } else {
            debtorPanel.clearIbanError();
        }

        // Debtor BIC
        String bic = config.debtor.bic == null ? "" : config.debtor.bic.trim().toUpperCase();
        if (isBlank(bic)) {
            debtorPanel.setBicError("The BIC for the debtor is mandatory");
            valid = false;
        } else if (!bic.matches(BIC_PATTERN)) {
            debtorPanel.setBicError("BIC format is invalid (e.g. BNPAFRPP or BNPAFRPPPARIS)");
            valid = false;
        } else {
            debtorPanel.clearBicError();
        }

        // Initiating party name
        if (isBlank(config.initiatingParty.name)) {
            initiatingPartyPanel.setNameError("The initiating party name is mandatory");
            valid = false;
        } else {
            initiatingPartyPanel.clearNameError();
        }

        // Initiating party SIRET — 14 digits
        String siret = config.initiatingParty.siret == null ? "" : config.initiatingParty.siret.trim();
        if (isBlank(siret)) {
            initiatingPartyPanel.setSiretError("The SIRET is mandatory");
            valid = false;
        } else if (!siret.matches(SIRET_PATTERN)) {
            initiatingPartyPanel.setSiretError("SIRET must be exactly 14 digits");
            valid = false;
        } else {
            initiatingPartyPanel.clearSiretError();
        }

        return valid;
    }

    /** MOD-97 IBAN checksum (RFC 3166 / ISO 13616). Input must be stripped and upper-cased. */
    private boolean ibanMod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) numeric.append(c - 'A' + 10);
            else numeric.append(c);
        }
        java.math.BigInteger value = new java.math.BigInteger(numeric.toString());
        return value.mod(java.math.BigInteger.valueOf(97)).intValue() == 1;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // --- Data Models ---
    public static class ConfigData {
        public Debtor debtor;
        public InitiatingParty initiatingParty;
        public FileSettings fileSettings;
    }

    public static class Debtor {
        public String name;
        public String iban;
        public String bic;
    }

    public static class InitiatingParty {
        public String name;
        public String siret;
    }

    public static class FileSettings {
        public String defaultInputPath;
        public String defaultOutputPath;
    }
}
