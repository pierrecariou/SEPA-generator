package com.pcariou.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.pcariou.view.config.AppConfig;
import com.pcariou.view.config.ConfigStore;
import com.pcariou.view.custom.Cards;
import com.pcariou.view.main.MainFrame;

import net.miginfocom.swing.MigLayout;

/**
 * Modal settings dialog: debtor information and initiating party.
 * Uses the same card styling as the main window so both themes match.
 */
public class SettingsFrame extends JDialog {

    private final DebtorPanel debtorPanel = new DebtorPanel();
    private final InitiatingPartyPanel initiatingPartyPanel = new InitiatingPartyPanel();

    private final ConfigStore configStore = new ConfigStore();
    private final MainFrame owner;

    public SettingsFrame(MainFrame parent) {
        super(parent, "Settings", true);
        this.owner = parent;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new MigLayout(
                "insets 20, fillx, wrap 1, gapy 14",
                "[grow,fill]",
                "[]"));

        content.add(createSectionCard("Debtor information",
                "Account that the payments are taken from.", debtorPanel), "growx");
        content.add(createSectionCard("Initiating party",
                "Organisation submitting the payment file.", initiatingPartyPanel), "growx");
        content.add(createButtonsRow(), "growx");

        setContentPane(content);

        // Esc closes the dialog without saving
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        loadConfig();

        pack();
        Dimension packed = getSize();
        setMinimumSize(new Dimension(Math.max(packed.width, 480), packed.height));
        setSize(Math.max(packed.width, 480), packed.height);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private JComponent createSectionCard(String title, String subtitle, JComponent fields) {
        JPanel card = Cards.createCard(new MigLayout(
                "fillx, insets 16, wrap 1",
                "[grow,fill]",
                "[]2[]10[]"));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        card.add(titleLabel);
        card.add(subtitleLabel);
        card.add(fields, "growx");
        return card;
    }

    private JComponent createButtonsRow() {
        JPanel row = new JPanel(new MigLayout("insets 0", "[grow][][]", "[]"));
        row.setOpaque(false);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener((ActionEvent e) -> dispose());

        JButton save = new JButton("Save");
        save.addActionListener(e -> saveConfig());

        row.add(new JLabel(), "growx, pushx");
        row.add(cancel, "gapright 8");
        row.add(save);

        getRootPane().setDefaultButton(save);
        return row;
    }

    private void loadConfig() {
        AppConfig config = configStore.read();
        if (config != null) {
            populateForm(config);
        }
    }

    private void populateForm(AppConfig config) {
        if (config.debtor != null) {
            debtorPanel.setName(defaultValue(config.debtor.name));
            debtorPanel.setIban(defaultValue(config.debtor.iban));
            debtorPanel.setBic(defaultValue(config.debtor.bic));
        }

        if (config.initiatingParty != null) {
            initiatingPartyPanel.setName(defaultValue(config.initiatingParty.name));
            initiatingPartyPanel.setSiret(defaultValue(config.initiatingParty.siret));
        }
        // A config without these sections simply shows empty fields.
    }

    private AppConfig collectFormData() {
        AppConfig config = new AppConfig();
        config.debtor = new AppConfig.Debtor();
        config.debtor.name = debtorPanel.getName();
        config.debtor.iban = debtorPanel.getIban();
        config.debtor.bic = debtorPanel.getBic();

        config.initiatingParty = new AppConfig.InitiatingParty();
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
        AppConfig existingConfig = configStore.read();
        AppConfig updatedSettings = collectFormData();

        if (!validateAndMarkFields(updatedSettings)) {
            return;
        }

        debtorPanel.clearAllErrors();
        initiatingPartyPanel.clearAllErrors();

        AppConfig configToSave = existingConfig != null ? existingConfig : new AppConfig();
        configToSave.debtor = updatedSettings.debtor;
        configToSave.initiatingParty = updatedSettings.initiatingParty;

        if (!configStore.write(configToSave)) {
            JOptionPane.showMessageDialog(this,
                    "Could not save settings.\nPlease check that this file is writable:\n"
                            + configStore.file().getAbsolutePath(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        owner.refreshStatus();
        dispose();
    }

    private boolean validateAndMarkFields(AppConfig config) {
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
            debtorPanel.setBicError("BIC format is invalid (e.g. BNPAFRPP or BNPAFRPPXXX)");
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
}
