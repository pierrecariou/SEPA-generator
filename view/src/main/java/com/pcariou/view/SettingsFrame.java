package com.pcariou.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.pcariou.model.BicValidator;
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
        int minWidth = Math.max(packed.width, 480);
        // Baseline height with no validation errors visible; the dialog may grow
        // taller than this when error labels appear, but never narrower.
        setMinimumSize(new Dimension(minWidth, packed.height));
        setSize(minWidth, packed.height);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Re-sizes the dialog to fit its current content. Error labels are hidden
     * (zero height) when the dialog is first packed, so showing them would
     * otherwise overflow the fixed window and clip the Save button. Packing
     * again grows the dialog to fit, and shrinks it back once errors clear,
     * while the minimum size keeps the width stable.
     */
    private void resizeToFitContent() {
        int currentWidth = getWidth();
        pack();
        // Keep height from pack() (grows for new errors, shrinks when cleared),
        // but never let the dialog become narrower than it already was so the
        // layout doesn't jitter horizontally.
        int width = Math.max(getWidth(), currentWidth);
        if (width != getWidth()) {
            setSize(width, getHeight());
        }
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
            populateAddress(config.debtor.address);
        }

        if (config.initiatingParty != null) {
            initiatingPartyPanel.setName(defaultValue(config.initiatingParty.name));
            initiatingPartyPanel.setSiret(defaultValue(config.initiatingParty.siret));
        }
        // A config without these sections simply shows empty fields.
    }

    private void populateAddress(AppConfig.Address address) {
        if (address == null) {
            return;
        }
        debtorPanel.setStreet(defaultValue(address.street));
        debtorPanel.setBuildingNumber(defaultValue(address.buildingNumber));
        debtorPanel.setPostcode(defaultValue(address.postcode));
        debtorPanel.setTown(defaultValue(address.town));
        debtorPanel.setCountry(defaultValue(address.country));
    }

    private AppConfig collectFormData() {
        AppConfig config = new AppConfig();
        config.debtor = new AppConfig.Debtor();
        config.debtor.name = debtorPanel.getName();
        config.debtor.iban = debtorPanel.getIban();
        config.debtor.bic = debtorPanel.getBic();
        config.debtor.address = collectAddress();

        config.initiatingParty = new AppConfig.InitiatingParty();
        config.initiatingParty.name = initiatingPartyPanel.getName();
        config.initiatingParty.siret = initiatingPartyPanel.getSiret();
        return config;
    }

    /** Returns the entered address, or null when every address field is blank. */
    private AppConfig.Address collectAddress() {
        AppConfig.Address address = new AppConfig.Address();
        address.street         = trimToNull(debtorPanel.getStreet());
        address.buildingNumber = trimToNull(debtorPanel.getBuildingNumber());
        address.postcode       = trimToNull(debtorPanel.getPostcode());
        address.town           = trimToNull(debtorPanel.getTown());
        address.country        = trimToNull(debtorPanel.getCountry());

        boolean empty = address.street == null && address.buildingNumber == null
                && address.postcode == null && address.town == null && address.country == null;
        return empty ? null : address;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private static final String IBAN_FORMAT_PATTERN = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$";
    private static final String BIC_PATTERN         = BicValidator.BIC_PATTERN;
    private static final String SIRET_PATTERN       = "^[0-9]{14}$";
    private static final String COUNTRY_PATTERN     = "^[A-Z]{2}$";

    private void saveConfig() {
        AppConfig existingConfig = configStore.read();
        AppConfig updatedSettings = collectFormData();

        if (!validateAndMarkFields(updatedSettings)) {
            // Newly shown error labels add height; grow the dialog so the
            // cards and Save button stay fully visible.
            resizeToFitContent();
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

        // Optional postal address: if any field is filled, town and country are required
        debtorPanel.clearAddressError();
        if (config.debtor.address != null) {
            AppConfig.Address address = config.debtor.address;
            boolean townMissing = isBlank(address.town);
            boolean countryMissing = isBlank(address.country);
            if (townMissing || countryMissing) {
                debtorPanel.setAddressError(
                        "Town/City and country are required when an address is provided",
                        townMissing, countryMissing);
                valid = false;
            } else if (!address.country.trim().toUpperCase().matches(COUNTRY_PATTERN)) {
                debtorPanel.setAddressError(
                        "Country must be a 2-letter ISO code (e.g. FR, DE, NL)",
                        false, true);
                valid = false;
            }
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
