package com.pcariou.view.update;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.ExternalLinks;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;

/**
 * Small, professional modal dialog that presents an available update: the current
 * and latest versions, the release date when known, and two actions - "Download
 * update" (opens the platform-specific download in the browser) and "Not now".
 *
 * <p>The dialog never downloads or runs an installer itself; it only hands off to
 * the browser via {@link ExternalLinks}, keeping the user in control.</p>
 */
public final class UpdateDialog {

    private UpdateDialog() {
    }

    /** Builds and shows the update dialog centred on {@code parent}'s window. */
    public static void show(Component parent, UpdateInfo info, String currentVersion) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        final JDialog dialog = new JDialog(owner, "Update available", JDialog.ModalityType.APPLICATION_MODAL);

        JPanel content = new JPanel(new MigLayout(
                "insets 20 24 16 24, wrap 1, fillx",
                "[grow]",
                "[]12[]4[]4[]16[]"));

        JLabel title = new JLabel("Version " + safe(info.getLatestVersion()) + " is available");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +2;");
        content.add(title, "growx");

        content.add(versionRow("Current version", "v" + safe(currentVersion)));
        content.add(versionRow("Latest version", "v" + safe(info.getLatestVersion())));
        if (info.hasReleaseDate()) {
            content.add(versionRow("Release date", info.getReleaseDate()));
        }

        if (info.isCritical()) {
            JLabel critical = new JLabel("This is an important update. Updating is strongly recommended.");
            critical.putClientProperty(FlatClientProperties.STYLE,
                    "foreground: $App.errorColor; font: -1;");
            content.add(critical, "growx");
        }

        JPanel buttons = new JPanel(new MigLayout("insets 0, fillx", "[grow][][]", "[]"));
        JButton later = new JButton("Not now");
        later.putClientProperty(FlatClientProperties.STYLE,
                "hoverBorderColor: darken($Button.borderColor,22%);"
                        + " pressedBorderColor: darken($Button.borderColor,30%);");
        later.addActionListener(e -> dialog.dispose());

        JButton download = new JButton("Download update");
        download.putClientProperty(FlatClientProperties.STYLE,
                "background: $Component.accentColor; foreground: #ffffff;");
        download.addActionListener(e -> {
            String url = info.downloadUrlFor(PlatformDetector.currentKey());
            dialog.dispose();
            ExternalLinks.open(url, owner);
        });

        buttons.add(new JLabel(), "growx");
        buttons.add(later);
        buttons.add(download);
        content.add(buttons, "growx");

        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(download);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel versionRow(String label, String value) {
        JPanel row = new JPanel(new MigLayout("insets 0, fillx", "[120!][grow]", "[]"));
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground;");
        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.BOLD));
        row.add(name);
        row.add(val, "growx");
        row.setBorder(BorderFactory.createEmptyBorder());
        return row;
    }

    private static String safe(String value) {
        return value == null ? "unknown" : value;
    }
}
