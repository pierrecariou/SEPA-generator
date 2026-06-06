package com.pcariou.view.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.AppTheme;
import com.pcariou.view.SettingsFrame;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class HeaderPanel extends JPanel {
    private final JButton themeButton    = new JButton("🌙");
    private final JButton profilesButton = new JButton("👤");
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

        JLabel edition = new JLabel("  Community Edition");
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
        profilesButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        themeButton.setFocusable(false);
        profilesButton.setFocusable(false);
        themeButton.setToolTipText("Toggle Theme");
        profilesButton.setToolTipText("Profiles / Settings");

        themeButton.addActionListener(e -> AppTheme.switchMode());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem debtor = new JMenuItem("Debtor profile...");
        debtor.addActionListener(e -> {
            JFrame settingsFrame = new SettingsFrame(owner);
            settingsFrame.setVisible(true);
        });
        menu.add(debtor);

        profilesButton.addActionListener(e -> menu.show(profilesButton, 0, profilesButton.getHeight()));

        rightPanel.add(themeButton);
        rightPanel.add(profilesButton);
        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        refreshColors();
        refreshIcon();
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
        String resource = dark ? "/sepa-generator-icon-dark-theme.png" : "/sepa-generator-icon-light-theme.png";
        URL url = getClass().getResource(resource);
        if (url != null) {
            Image img = new ImageIcon(url).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(img));
        }
    }
}