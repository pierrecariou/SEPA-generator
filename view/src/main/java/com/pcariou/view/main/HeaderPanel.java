package com.pcariou.view.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.AppEdition;
import com.pcariou.view.AppLinks;
import com.pcariou.view.AppResources;
import com.pcariou.view.AppTheme;
import com.pcariou.view.ExternalLinks;
import com.pcariou.view.SettingsFrame;
import com.pcariou.view.SvgIcons;
import com.pcariou.view.help.AboutDialog;
import com.pcariou.view.help.ReleaseNotesDialog;
import com.pcariou.view.update.UpdateIndicator;
import com.pcariou.view.update.UpdateUi;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class HeaderPanel extends JPanel {
    /** Fixed square size for the small toolbar icon buttons so icons never shift on hover. */
    private static final int ICON_BUTTON_SIZE = 30;

    /** Help menu labels, shared with the tests. */
    public static final String HELP_ONLINE_GUIDES     = "Online guides";
    public static final String HELP_RELEASE_NOTES     = "What\u2019s new\u2026";
    public static final String HELP_CHECK_FOR_UPDATES = "Check for updates";
    public static final String HELP_CONTACT           = "Contact\u2026";
    public static final String HELP_ABOUT             = AboutDialog.TITLE + "\u2026";

    private final JButton themeButton    = new JButton();
    private final JButton settingsButton = new JButton();
    private final JButton helpButton     = new JButton();
    private JLabel logoLabel;

    public HeaderPanel(MainFrame owner, UpdateUi updateUi, String version) {
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

        standardizeIconButton(themeButton);
        standardizeIconButton(settingsButton);
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

        // Actionable product-level notification: hidden until an update is
        // available, then shown just before "Upgrade to Pro". The trailing spacer
        // is a zero-height (invisible) strut, so it costs nothing while hidden.
        UpdateIndicator updateIndicator = updateUi.getIndicator();
        rightPanel.add(updateIndicator);
        rightPanel.addSeparator(new Dimension(8, 0));

        // Help: product information rather than actions on the user's data.
        // Sits immediately before the Upgrade pill so the informational and
        // commercial entry points read as one group.
        buildHelpMenu(owner, updateUi, version);
        rightPanel.add(helpButton);
        rightPanel.addSeparator(new Dimension(8, 0));

        if (upgradeButton != null) {
            rightPanel.add(upgradeButton);
            rightPanel.addSeparator(new Dimension(8, 0));
        }
        rightPanel.add(themeButton);
        rightPanel.add(settingsButton);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Builds the header "Help" control: a restrained named menu grouping what
     * users look for when they need information about the product rather than an
     * action on their data.
     *
     * <p>"Online guides" opens the public guides page in the default browser
     * through the shared {@link ExternalLinks} helper. "What's new" always works
     * offline (the release notes are bundled with
     * the application); "Check for updates" delegates to the existing
     * {@link UpdateUi#checkManually()} flow rather than duplicating any update
     * logic, and never installs anything by itself.</p>
     */
    private void buildHelpMenu(MainFrame owner, final UpdateUi updateUi, final String version) {
        final JPopupMenu menu = buildHelpMenuItems(owner, updateUi, version, this);

        // Reused off-screen popup: refresh it on theme changes so it never keeps
        // stale colors after a live switch.
        AppTheme.registerThemedComponent(menu);

        helpButton.setText("Help");
        helpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        helpButton.setFocusable(false);
        helpButton.setToolTipText("What\u2019s new, updates and product information");
        helpButton.putClientProperty(FlatClientProperties.STYLE, "margin: 4,10,4,10;");
        helpButton.setHorizontalTextPosition(SwingConstants.LEFT);
        helpButton.setIconTextGap(6);
        helpButton.setIcon(caretIcon());
        helpButton.addActionListener(e -> menu.show(helpButton, 0, helpButton.getHeight()));
    }

    /**
     * Populates the Help menu. Package-visible and free of any window-showing
     * call so the entries and their order can be asserted headlessly; the header
     * itself needs a full application context and cannot be built in a test.
     *
     * <p>"Online guides" leads the menu because it is what a user reaches for
     * when they need to understand a SEPA format or workflow. It opens the
     * public guides page through the shared {@link ExternalLinks} helper, so it
     * uses the platform default browser and the same error dialog as every other
     * external link, and performs no network access until it is chosen.</p>
     */
    static JPopupMenu buildHelpMenuItems(final MainFrame owner, final UpdateUi updateUi,
                                         final String version, final Component linkParent) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem guides = new JMenuItem(HELP_ONLINE_GUIDES);
        guides.setToolTipText("Open the SEPA Generator guides in your browser");
        guides.addActionListener(e -> ExternalLinks.open(AppLinks.GUIDES, linkParent));
        menu.add(guides);

        JMenuItem releaseNotes = new JMenuItem(HELP_RELEASE_NOTES);
        releaseNotes.setToolTipText("What is new in this version");
        releaseNotes.addActionListener(e -> ReleaseNotesDialog.show(owner, version));
        menu.add(releaseNotes);

        JMenuItem checkUpdates = new JMenuItem(HELP_CHECK_FOR_UPDATES);
        checkUpdates.setToolTipText("Check whether a newer version is available");
        checkUpdates.addActionListener(e -> {
            if (updateUi != null) {
                updateUi.checkManually();
            }
        });
        menu.add(checkUpdates);

        menu.addSeparator();

        JMenuItem contact = new JMenuItem(HELP_CONTACT);
        contact.setToolTipText("Contact us by email");
        contact.addActionListener(e -> ExternalLinks.open(AppLinks.CONTACT, linkParent));
        menu.add(contact);

        JMenuItem about = new JMenuItem(HELP_ABOUT);
        about.addActionListener(e -> AboutDialog.show(owner, version));
        menu.add(about);

        return menu;
    }

    /** A small downward caret signalling the Help dropdown, in a calm tone. */
    private Icon caretIcon() {
        final Color color = UIManager.getColor("Label.disabledForeground");
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : Color.GRAY);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 1, y + 3, x + 4, y + 6);
                g2.drawLine(x + 4, y + 6, x + 7, y + 3);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 9;
            }

            @Override
            public int getIconHeight() {
                return 9;
            }
        };
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
                        + " toolbar.margin: 3,12,3,12;"
                        + " focusWidth: 0;"
                        + " innerFocusWidth: 0;"
                        + " borderWidth: 1;"
                        + " background: null;"
                        + " borderColor: $Component.accentColor;"
                        + " foreground: $Component.accentColor;"
                        + " hoverBorderColor: $Component.accentColor;"
                        + " hoverBackground: fade($Component.accentColor,22%);"
                        + " pressedBackground: fade($Component.accentColor,34%);");
        upgrade.addActionListener(e -> ExternalLinks.open(AppEdition.upgradeUrl(), this));
        return upgrade;
    }

    /**
     * Standardizes a small action icon button: toolbar styling, a fixed square
     * container so the icon stays centred (and never moves on hover) and a hand
     * cursor for consistent clickable feedback.
     */
    private void standardizeIconButton(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        Dimension size = new Dimension(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
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
        if (helpButton != null) {
            helpButton.setIcon(caretIcon());
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