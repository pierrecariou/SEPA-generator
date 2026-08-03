package com.pcariou.view.help;

import com.formdev.flatlaf.FlatClientProperties;
import com.pcariou.view.ExternalLinks;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

/**
 * Shows the curated release notes of the <em>installed</em> version, loaded from
 * the bundled {@link ReleaseNotes} resource.
 *
 * <p>The dialog is always reachable from Help and never depends on the network:
 * the notes ship with the application. When no notes are bundled for the running
 * version (an unreleased development build, for example) it says so calmly and
 * points at the product website instead of showing an error.</p>
 */
public final class ReleaseNotesDialog {

    /** Window title, shared with the Help menu entry and the tests. */
    public static final String TITLE = "Release notes";

    /** Shown instead of the notes when none are bundled for the running version. */
    static final String UNAVAILABLE =
            "Release notes are not available in this build.\n"
                    + "You can read them on the product website.";

    /** Label of the action opening the release page of the installed version. */
    static final String VIEW_ONLINE = "View online";

    private static final int CONTENT_WIDTH_PX = 520;
    private static final int CONTENT_HEIGHT_PX = 420;

    private ReleaseNotesDialog() {
    }

    /** Builds and shows the release-notes dialog centred on {@code parent}'s window. */
    public static void show(Component parent, String version) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, TITLE, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(buildContent(dialog, owner, version));
        HelpDialogs.closeOnEscape(dialog);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Builds the dialog content. Package-visible and free of any modal call so
     * the presentation can be asserted headlessly.
     *
     * @param dialog the dialog to close from the Close action, may be {@code null}
     */
    static JPanel buildContent(final JDialog dialog, final Window owner, String version) {
        // The bundled notes carry their own versioned heading, so the dialog adds
        // none of its own; the window title already says what this is.
        String notes = ReleaseNotes.forVersion(version);
        JPanel content = new JPanel(new MigLayout(
                "insets 20 24 16 24, wrap 1, fill",
                "[grow]",
                notes.isEmpty() ? "[]16[]" : "[grow]16[]"));

        if (notes.isEmpty()) {
            JLabel unavailable = new JLabel("<html>" + UNAVAILABLE.replace("\n", "<br>") + "</html>");
            unavailable.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground;");
            content.add(unavailable, "growx");
        } else {
            content.add(notesScrollPane(notes, owner), "grow, push");
        }

        content.add(buttons(dialog, owner, version), "growx");
        return content;
    }

    private static JScrollPane notesScrollPane(String notes, final Window owner) {
        JEditorPane pane = new JEditorPane("text/html", notes);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setCaret(readOnlyCaret());
        pane.setCaretPosition(0);
        // Follow the current theme instead of the Swing HTML defaults.
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        Color foreground = UIManager.getColor("Label.foreground");
        if (foreground != null) {
            pane.setForeground(foreground);
        }
        // Any link in the notes opens in the browser through the shared helper.
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null) {
                ExternalLinks.open(event.getURL().toString(), owner);
            }
        });

        JScrollPane scroll = new JScrollPane(pane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(CONTENT_WIDTH_PX, CONTENT_HEIGHT_PX));
        return scroll;
    }

    /**
     * A caret that is never painted, for a document the user may read and copy
     * but not edit.
     *
     * <p>Suppressing only the painted caret - rather than disabling the caret or
     * the component's focusability - keeps everything that makes the notes
     * usable: mouse selection, {@code Ctrl+C}, {@code Shift}+arrow selection,
     * {@code Tab} focus and keyboard scrolling all still work, because they are
     * driven by the caret's position and by the component being focusable. Only
     * the blinking insertion bar, which would suggest the text can be typed
     * into, is hidden. The blink timer is switched off as well, so an invisible
     * caret costs nothing.</p>
     */
    static Caret readOnlyCaret() {
        DefaultCaret caret = new DefaultCaret() {
            @Override
            public void setVisible(boolean visible) {
                // Focus gain would otherwise show the insertion bar.
                super.setVisible(false);
            }
        };
        caret.setBlinkRate(0);
        return caret;
    }

    private static JPanel buttons(final JDialog dialog, final Window owner, String version) {
        JPanel buttons = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        buttons.setOpaque(false);

        JButton viewOnline = HelpDialogs.linkButton(VIEW_ONLINE, ReleaseNotes.onlineUrl(version), owner);

        JButton close = new JButton("Close");
        close.addActionListener(e -> {
            if (dialog != null) {
                dialog.dispose();
            }
        });

        buttons.add(viewOnline);
        buttons.add(close, "gapleft push");

        if (dialog != null) {
            dialog.getRootPane().setDefaultButton(close);
        }
        return buttons;
    }
}
