package com.pcariou.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsFrame extends JFrame {

    private final DebtorPanel debtorPanel = new DebtorPanel();
    private final InitiatingPartyPanel initiatingPartyPanel = new InitiatingPartyPanel();
    private final PrefillPanel prefillPanel = new PrefillPanel();

    private final File configFile = new File(System.getProperty("user.home"), ".sepa-generator-config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SettingsFrame(JFrame parent) {
        super("Settings");
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
        mainPanel.add(prefillPanel);

        add(mainPanel, BorderLayout.CENTER);
        add(saveButtonPanel, BorderLayout.SOUTH);

        loadConfig();

        setVisible(true);
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ConfigData config = gson.fromJson(reader, ConfigData.class);

                if (isValid(config)) {
                    debtorPanel.setName(config.debtor.name);
                    debtorPanel.setIban(config.debtor.iban);
                    debtorPanel.setBic(config.debtor.bic);

                    initiatingPartyPanel.setName(config.initiatingParty.name);
                    initiatingPartyPanel.setSiret(config.initiatingParty.siret);

                    prefillPanel.setInputPath(config.fileSettings.defaultInputPath);
                    prefillPanel.setOutputPath(config.fileSettings.defaultOutputPath);
                } else {
                    JOptionPane.showMessageDialog(this, "Config file is invalid and did not load properly: " + configFile.getAbsolutePath(),
                            "Invalid Config", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load config: " + e.getMessage());
            }
        }
    }

    private boolean isValid(ConfigData config) {
        return config != null &&
               config.debtor != null && config.debtor.name != null && !config.debtor.name.isEmpty() &&
               config.debtor.iban != null && !config.debtor.iban.isEmpty() &&
               config.debtor.bic != null && !config.debtor.bic.isEmpty() &&
               config.initiatingParty != null && config.initiatingParty.name != null && !config.initiatingParty.name.isEmpty() &&
               config.initiatingParty.siret != null && !config.initiatingParty.siret.isEmpty() &&
               config.fileSettings != null &&
               config.fileSettings.defaultInputPath != null && !config.fileSettings.defaultInputPath.isEmpty() &&
               config.fileSettings.defaultOutputPath != null && !config.fileSettings.defaultOutputPath.isEmpty();
    }

    private void saveConfig() {
        ConfigData config = new ConfigData();
        config.debtor = new Debtor();
        config.debtor.name = debtorPanel.getName();
        config.debtor.iban = debtorPanel.getIban();
        config.debtor.bic = debtorPanel.getBic();

        config.initiatingParty = new InitiatingParty();
        config.initiatingParty.name = initiatingPartyPanel.getName();
        config.initiatingParty.siret = initiatingPartyPanel.getSiret();

        config.fileSettings = new FileSettings();
        config.fileSettings.defaultInputPath = prefillPanel.getInputPath();
        config.fileSettings.defaultOutputPath = prefillPanel.getOutputPath();

        if (!isValid(config)) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields before saving.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            JOptionPane.showMessageDialog(this, "Settings saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save config: " + e.getMessage());
        }
    }

    // --- Data Models ---
    static class ConfigData {
        Debtor debtor;
        InitiatingParty initiatingParty;
        FileSettings fileSettings;
    }

    static class Debtor {
        String name;
        String iban;
        String bic;
    }

    static class InitiatingParty {
        String name;
        String siret;
    }

    static class FileSettings {
        String defaultInputPath;
        String defaultOutputPath;
    }
}
