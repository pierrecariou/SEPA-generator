package com.pcariou.view;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Standardized application message dialogs.
 *
 * <p>Centralizes the semantic mapping from a {@link Kind} to a consistent
 * Lucide-style icon (tinted per theme) so success/error/info/warning dialogs
 * look the same everywhere and avoid the default Swing/FlatLaf icons:
 *
 * <ul>
 *   <li>{@link Kind#SUCCESS} - circle-check</li>
 *   <li>{@link Kind#ERROR} - circle-x</li>
 *   <li>{@link Kind#INFO} - circle-info</li>
 *   <li>{@link Kind#WARNING} - triangle-alert</li>
 * </ul>
 *
 * <p>Thin wrapper over {@link JOptionPane}: it only supplies the icon and a
 * matching message type (for accessibility), so callers keep full control of
 * titles, buttons and option flows.
 */
public final class AppDialogs {

    /** Semantic intent of a message dialog. */
    public enum Kind {
        SUCCESS(SvgIcons.CIRCLE_CHECK,   "App.successColor",       JOptionPane.INFORMATION_MESSAGE),
        ERROR  (SvgIcons.CIRCLE_X,       "App.errorColor",         JOptionPane.ERROR_MESSAGE),
        INFO   (SvgIcons.CIRCLE_INFO,    "App.infoColor",          JOptionPane.INFORMATION_MESSAGE),
        WARNING(SvgIcons.TRIANGLE_ALERT, "App.warningColor",       JOptionPane.WARNING_MESSAGE);

        private final String iconName;
        private final String colorKey;
        private final int messageType;

        Kind(String iconName, String colorKey, int messageType) {
            this.iconName = iconName;
            this.colorKey = colorKey;
            this.messageType = messageType;
        }

        Icon icon() {
            return SvgIcons.dialogIcon(iconName, colorKey);
        }

        /** The SVG resource name for this kind (package-visible for tests/reuse). */
        String iconName() {
            return iconName;
        }
    }

    /**
     * Shows a standardized confirmation dialog (e.g. Yes/No or OK/Cancel) with
     * the semantic icon for {@code kind} and returns the selected option (see
     * {@link JOptionPane#showConfirmDialog}). Keeps the standard confirm
     * buttons while replacing the default Swing/FlatLaf icon.
     */
    public static int confirm(Component parent, String title, Object message, Kind kind,
            int optionType) {
        return JOptionPane.showConfirmDialog(parent, message, title, optionType,
                kind.messageType, kind.icon());
    }

    /** Shows a standardized message dialog with the semantic icon for {@code kind}. */
    public static void show(Component parent, String title, Object message, Kind kind) {
        JOptionPane.showMessageDialog(parent, message, title, kind.messageType, kind.icon());
    }

    /**
     * Shows a standardized option dialog with the semantic icon for {@code kind}
     * and returns the selected option index (see
     * {@link JOptionPane#showOptionDialog}). Use for flows that need custom
     * buttons.
     */
    public static int showOptions(Component parent, String title, Object message, Kind kind,
            Object[] options, Object initial) {
        return JOptionPane.showOptionDialog(parent, message, title,
                JOptionPane.DEFAULT_OPTION, kind.messageType, kind.icon(), options, initial);
    }

    private AppDialogs() {
    }
}
