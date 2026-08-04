package com.pcariou.view.help;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.pcariou.view.AppEdition;
import com.pcariou.view.AppLinks;
import com.pcariou.view.AppResources;
import com.pcariou.view.AppTheme;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Image;
import java.awt.Window;
import java.net.URL;

/**
 * Restrained "About" dialog: what the product is, which version is installed and
 * who publishes it.
 *
 * <p>It deliberately states only facts the application already knows reliably -
 * the edition product name, the version handed to the UI at startup and the
 * publisher - and performs no network access of any kind.</p>
 */
public final class AboutDialog {

    /** Window title, shared with the Help menu entry and the tests. */
    public static final String TITLE = "About " + AppEdition.PRODUCT_NAME;

    /** Publisher line. Niryosys publishes the product; it is not part of the product name. */
    public static final String PUBLISHER = "Published by Niryosys";

    private static final int ICON_SIZE_PX = 48;

    /** Publisher mark beside the publisher line. */
    private static final int COMPANY_ICON_SIZE_PX = 32;

    private AboutDialog() {
    }

    /** Builds and shows the About dialog centred on {@code parent}'s window. */
    public static void show(Component parent, String version) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, TITLE, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(buildContent(dialog, owner, version));
        HelpDialogs.closeOnEscape(dialog);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Builds the dialog content. Package-visible and free of any modal call so
     * the presentation can be asserted headlessly.
     */
    static JPanel buildContent(final JDialog dialog, final Window owner, String version) {
        JPanel content = new JPanel(new MigLayout(
                "insets 20 24 16 24, fillx",
                "[]16[grow]",
                "[]4[]4[]16[]"));

        JLabel icon = new JLabel();
        URL iconUrl = AboutDialog.class.getResource(AppResources.appIcon(64));
        if (iconUrl != null) {
            Image image = new ImageIcon(iconUrl).getImage()
                    .getScaledInstance(ICON_SIZE_PX, ICON_SIZE_PX, Image.SCALE_SMOOTH);
            icon.setIcon(new ImageIcon(image));
        }
        icon.setBorder(BorderFactory.createEmptyBorder());
        content.add(icon, "spany 3, aligny top");

        JLabel name = new JLabel(AppEdition.PRODUCT_NAME);
        name.putClientProperty(FlatClientProperties.STYLE, "font: bold +2;");
        content.add(name, "growx, wrap");

        JLabel versionLabel = new JLabel(displayVersion(version));
        content.add(versionLabel, "growx, wrap");

        // Publisher: the wording stays a statement of fact; only the line itself
        // becomes actionable so users can reach the company site from About. The
        // company mark rides on the same link, so no extra row, panel or second
        // mention of Niryosys is introduced.
        JButton publisher = HelpDialogs.linkButton(PUBLISHER, AppLinks.COMPANY, owner);
        publisher.setHorizontalAlignment(SwingConstants.LEADING);
        publisher.setIcon(companyIcon());
        publisher.setIconTextGap(8);
        publisher.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.accentColor; font: -1; margin: 0,0,0,0;");
        content.add(publisher, "growx, wrap");

        content.add(buttons(dialog, owner), "span 2, growx");
        return content;
    }

    private static JPanel buttons(final JDialog dialog, final Window owner) {
        JPanel buttons = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        buttons.setOpaque(false);

        // Deliberately the product homepage, not a releases archive or the
        // publisher site: About is about the product.
        JButton website = HelpDialogs.linkButton("Website", AppLinks.WEBSITE, owner);

        JButton close = new JButton("Close");
        close.addActionListener(e -> {
            if (dialog != null) {
                dialog.dispose();
            }
        });

        buttons.add(website);
        buttons.add(close, "gapleft push");

        if (dialog != null) {
            dialog.getRootPane().setDefaultButton(close);
        }
        return buttons;
    }

    /**
     * The Niryosys mark at the About-dialog size, in the variant that suits the
     * current theme. Rendered from the canonical SVG through the icon
     * infrastructure the UI already uses, so it stays sharp on high-DPI displays
     * and needs no raster derivatives.
     */
    private static Icon companyIcon() {
        boolean dark = AppTheme.getCurrentMode() == AppTheme.Mode.DARK;
        return new FlatSVGIcon(AppResources.companyIcon(dark).substring(1),
                COMPANY_ICON_SIZE_PX, COMPANY_ICON_SIZE_PX);
    }

    private static String displayVersion(String version) {
        return version == null || version.trim().isEmpty()
                ? "Version unknown"
                : "Version " + version.trim();
    }
}
