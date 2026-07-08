package com.pcariou.view.update;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Cursor;

/**
 * Small accent "pill" notification shown in the header action area (next to
 * "Upgrade to Pro") only when an update is available. It stays hidden otherwise,
 * so the header is unchanged when the app is up to date. Clicking it opens the
 * {@link UpdateDialog} with the details of the latest release.
 *
 * <p>It never appears on its own and adds no clutter when the app is up to date,
 * in line with the "no aggressive banner" UX rule.</p>
 */
public final class UpdateIndicator extends JButton {

    private final Component dialogParent;
    private UpdateInfo info;
    private String currentVersion;

    public UpdateIndicator(Component dialogParent) {
        super("Update available");
        this.dialogParent = dialogParent;

        setVisible(false);
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("A newer version is available - click for details");
        putClientProperty(FlatClientProperties.STYLE,
                "arc: 999;"
                        + " font: -1;"
                        + " margin: 3,12,3,12;"
                        + " toolbar.margin: 3,12,3,12;"
                        + " focusWidth: 0;"
                        + " innerFocusWidth: 0;"
                        + " borderWidth: 0;"
                        + " background: $Component.accentColor;"
                        + " foreground: #ffffff;"
                        + " hoverBackground: darken($Component.accentColor,8%);"
                        + " pressedBackground: darken($Component.accentColor,15%);"
                        + " toolbar.hoverBackground: darken($Component.accentColor,8%);"
                        + " toolbar.pressedBackground: darken($Component.accentColor,15%);");

        addActionListener(e -> openDialogIfAvailable());
    }

    /** Shows the pill for the given available update. */
    public void showUpdate(UpdateInfo info, String currentVersion) {
        this.info = info;
        this.currentVersion = currentVersion;
        setVisible(true);
        revalidate();
        repaint();
    }

    /** Hides the pill (e.g. after a manual check confirms the app is up to date). */
    public void clear() {
        this.info = null;
        setVisible(false);
        revalidate();
        repaint();
    }

    /** Opens the details dialog when an update is currently known. */
    public void openDialogIfAvailable() {
        if (info != null) {
            UpdateDialog.show(dialogParent, info, currentVersion);
        }
    }
}
