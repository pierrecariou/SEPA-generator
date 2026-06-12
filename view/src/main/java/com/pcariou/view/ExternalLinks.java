package com.pcariou.view;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small helper for opening external web pages and {@code mailto:} links using
 * the platform's default handler.
 *
 * <p>Web links are opened with {@link Desktop#browse(URI)} and email links with
 * {@link Desktop#mail(URI)}. When the desktop integration is missing or the
 * requested action is unsupported, a clear error dialog is shown instead of
 * failing silently.
 */
public final class ExternalLinks {

    private static final String MAILTO_PREFIX = "mailto:";

    /** The kind of external target, which determines the {@link Desktop} action used. */
    public enum Kind { BROWSE, MAIL }

    private ExternalLinks() {
    }

    /** Classifies a target as a {@code mailto:} link ({@link Kind#MAIL}) or a web link ({@link Kind#BROWSE}). */
    public static Kind classify(String target) {
        if (target != null && target.regionMatches(true, 0, MAILTO_PREFIX, 0, MAILTO_PREFIX.length())) {
            return Kind.MAIL;
        }
        return Kind.BROWSE;
    }

    /**
     * Opens {@code target} (an {@code http(s)} URL or a {@code mailto:} link) with the
     * platform default handler. If the action cannot be performed, a clear error dialog
     * is shown, parented to {@code parent}.
     */
    public static void open(String target, Component parent) {
        Kind kind = classify(target);
        try {
            URI uri = new URI(target);
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            Desktop.Action action = (kind == Kind.MAIL) ? Desktop.Action.MAIL : Desktop.Action.BROWSE;

            if (desktop == null || !desktop.isSupported(action)) {
                showError(parent, kind, target);
                return;
            }

            if (kind == Kind.MAIL) {
                desktop.mail(uri);
            } else {
                desktop.browse(uri);
            }
        } catch (Exception ex) {
            Logger.getLogger(ExternalLinks.class.getName())
                    .log(Level.WARNING, "Could not open link: " + target, ex);
            showError(parent, kind, target);
        }
    }

    /**
     * Reveals {@code file} in the platform file manager, selecting it when the
     * platform supports it. Falls back to opening the containing folder, and
     * shows a clear error dialog when nothing works.
     */
    public static void showInFolder(java.io.File file, Component parent) {
        if (file == null) {
            return;
        }
        try {
            // Windows: Explorer can select the file directly.
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win") && file.exists()) {
                new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                return;
            }
            java.io.File folder = file.getParentFile();
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (folder != null && desktop != null && desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(folder);
                return;
            }
            showFolderError(parent, file);
        } catch (Exception ex) {
            Logger.getLogger(ExternalLinks.class.getName())
                    .log(Level.WARNING, "Could not show file in folder: " + file, ex);
            showFolderError(parent, file);
        }
    }

    private static void showFolderError(Component parent, java.io.File file) {
        JOptionPane.showMessageDialog(parent,
                "Could not open the containing folder.\nThe file is located at:\n" + file.getAbsolutePath(),
                "Unable to open folder", JOptionPane.WARNING_MESSAGE);
    }

    static String stripMailto(String target) {
        if (target != null && target.regionMatches(true, 0, MAILTO_PREFIX, 0, MAILTO_PREFIX.length())) {
            return target.substring(MAILTO_PREFIX.length());
        }
        return target;
    }

    private static void showError(Component parent, Kind kind, String target) {
        String message = (kind == Kind.MAIL)
                ? "Could not open your email client.\nPlease email us at: " + stripMailto(target)
                : "Could not open your web browser.\nPlease visit: " + target;
        JOptionPane.showMessageDialog(parent, message, "Unable to open link", JOptionPane.WARNING_MESSAGE);
    }
}
