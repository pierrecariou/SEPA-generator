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
import java.awt.Cursor;
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

    /** Heading of the optional curated summary block. */
    static final String HIGHLIGHTS_HEADING = "What's new";

    /** Label of the optional link to the published release notes. */
    static final String FULL_RELEASE_NOTES = "View full release notes";

    /** Wrap width for a highlight line, keeping the dialog compact. */
    private static final int HIGHLIGHT_WIDTH_PX = 380;

    private UpdateDialog() {
    }

    /** Builds and shows the update dialog centred on {@code parent}'s window. */
    public static void show(Component parent, UpdateInfo info, String currentVersion) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        final JDialog dialog = new JDialog(owner, "Update available", JDialog.ModalityType.APPLICATION_MODAL);

        dialog.setContentPane(buildContent(dialog, owner, info, currentVersion));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Builds the dialog content. Package-visible and free of any modal call so
     * the presentation can be asserted headlessly.
     *
     * @param dialog the dialog the actions close, may be {@code null}
     */
    static JPanel buildContent(final JDialog dialog, final Window owner,
            final UpdateInfo info, String currentVersion) {
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

        // Optional curated summary. A manifest without highlights renders exactly
        // as before, so the dialog never depends on this being published.
        if (info.hasHighlights()) {
            content.add(highlights(info), "growx");
        }

        JPanel buttons = new JPanel(new MigLayout("insets 0, fillx", "[grow][][]", "[]"));
        JButton later = new JButton("Not now");
        later.putClientProperty(FlatClientProperties.STYLE,
                "hoverBorderColor: darken($Button.borderColor,22%);");
        later.addActionListener(e -> dispose(dialog));

        JButton download = new JButton("Download update");
        download.putClientProperty(FlatClientProperties.STYLE,
                "background: $Component.accentColor; foreground: #ffffff;");
        download.addActionListener(e -> {
            String url = info.downloadUrlFor(PlatformDetector.currentKey());
            dispose(dialog);
            ExternalLinks.open(url, owner);
        });

        // Only offered when the manifest actually points somewhere usable.
        if (info.hasUsableReleaseNotesUrl()) {
            buttons.add(fullNotesLink(info.getReleaseNotesUrl(), owner));
        } else {
            buttons.add(new JLabel(), "growx");
        }
        buttons.add(later);
        buttons.add(download);
        content.add(buttons, "growx");

        if (dialog != null) {
            dialog.getRootPane().setDefaultButton(download);
        }
        return content;
    }

    /** The "What's new" block: a heading plus the curated lines as plain text. */
    private static JPanel highlights(UpdateInfo info) {
        JPanel panel = new JPanel(new MigLayout("insets 0, wrap 1, fillx", "[grow]", "[]4[]"));
        panel.setOpaque(false);

        JLabel heading = new JLabel(HIGHLIGHTS_HEADING);
        heading.putClientProperty(FlatClientProperties.STYLE, "font: bold;");
        panel.add(heading, "growx");

        for (String highlight : info.getHighlights()) {
            JLabel line = new JLabel("<html>&#8226;&nbsp;" + escape(highlight) + "</html>");
            line.putClientProperty(FlatClientProperties.STYLE, "font: -1;");
            panel.add(line, "growx, w ::" + HIGHLIGHT_WIDTH_PX);
        }
        return panel;
    }

    private static JButton fullNotesLink(final String url, final Window owner) {
        JButton link = new JButton(FULL_RELEASE_NOTES);
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setOpaque(false);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1;");
        link.addActionListener(e -> ExternalLinks.open(url, owner));
        return link;
    }

    private static void dispose(JDialog dialog) {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    /** Keeps manifest-supplied text literal inside the HTML-rendered label. */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
