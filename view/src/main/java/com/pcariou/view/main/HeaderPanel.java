package com.pcariou.view.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.AppEdition;
import com.pcariou.view.AppResources;
import com.pcariou.view.AppTheme;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.SettingsFrame;
import com.pcariou.view.SvgIcons;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class HeaderPanel extends JPanel {
    private final JButton themeButton    = new JButton();
    private final JButton settingsButton = new JButton();
    private JLabel logoLabel;

    public HeaderPanel(MainFrame owner) {
        super(new BorderLayout());
        setOpaque(true);
        refreshColors();

        // ── Left: logo + app name ─────────────────────────────────────────
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        logoLabel = new JLabel();
        logoLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        refreshIcon();
        left.add(logoLabel);

        JLabel appName = new JLabel("SEPA Generator");
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 13f));
        appName.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel edition = new JLabel("  " + AppEdition.LABEL);
        edition.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Label.disabledForeground;");
        edition.setAlignmentY(Component.CENTER_ALIGNMENT);

        left.add(appName);
        left.add(edition);
        add(left, BorderLayout.WEST);

        // ── Right: toolbar buttons ────────────────────────────────────────
        JToolBar rightPanel = new JToolBar();
        rightPanel.setFloatable(false);
        rightPanel.setOpaque(false);

        themeButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        settingsButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        themeButton.setFocusable(false);
        settingsButton.setFocusable(false);
        refreshThemeButton();
        settingsButton.setIcon(SvgIcons.toolbarIcon(SvgIcons.SETTINGS));
        settingsButton.setToolTipText("Settings");

        themeButton.addActionListener(e -> {
            AppTheme.switchMode();
            refreshThemeButton();
        });

        settingsButton.addActionListener(e -> {
            SettingsFrame settings = new SettingsFrame(owner);
            settings.setVisible(true);
        });

        JButton upgradeButton = createUpgradeButton();

        if (upgradeButton != null) {
            rightPanel.add(upgradeButton);
            rightPanel.addSeparator(new Dimension(8, 0));
        }
        rightPanel.add(themeButton);
        rightPanel.add(settingsButton);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * A discreet outlined "pill" link to the Pro upgrade page, or {@code null}
     * when the current edition does not expose the Upgrade to Pro action.
     */
    private JButton createUpgradeButton() {
        if (!AppEdition.showUpgradeToPro()) {
            return null;
        }
        JButton upgrade = new JButton(AppEdition.UPGRADE_TEXT);
        upgrade.setFocusable(false);
        upgrade.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        upgrade.setToolTipText(AppEdition.UPGRADE_TOOLTIP);
        upgrade.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + " font: -1;"
                        + " margin: 3,12,3,12;"
                        + " focusWidth: 0;"
                        + " innerFocusWidth: 0;"
                        + " borderWidth: 1;"
                        + " background: null;"
                        + " borderColor: $Component.accentColor;"
                        + " foreground: $Component.accentColor;"
                        + " hoverBorderColor: $Component.accentColor;"
                        + " hoverBackground: lighten($Component.accentColor,40%,relative);");
        upgrade.addActionListener(e -> ExternalLinks.open(AppEdition.upgradeUrl(), this));
        return upgrade;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        refreshColors();
        refreshIcon();
        refreshThemeButton();
        if (settingsButton != null) {
            settingsButton.setIcon(SvgIcons.toolbarIcon(SvgIcons.SETTINGS));
        }
    }

    /** Shows the mode the button will switch to, with a matching tooltip. */
    private void refreshThemeButton() {
        if (themeButton == null) return;
        boolean dark = AppTheme.getCurrentMode() == AppTheme.Mode.DARK;
        themeButton.setIcon(SvgIcons.toolbarIcon(dark ? SvgIcons.SUN : SvgIcons.MOON));
        themeButton.setToolTipText(dark ? "Switch to light theme" : "Switch to dark theme");
    }

    private void refreshColors() {
        Color bg  = UIManager.getColor("App.cardBackground");
        if (bg != null) setBackground(bg);
        Color sep = UIManager.getColor("App.borderColor");
        if (sep != null) setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, sep));
    }

    private void refreshIcon() {
        if (logoLabel == null) return;
        boolean dark = AppTheme.getCurrentMode() == AppTheme.Mode.DARK;
        String resource = AppResources.headerIcon(dark);
        URL url = getClass().getResource(resource);
        if (url != null) {
            Image img = new ImageIcon(url).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(img));
        }
    }
}